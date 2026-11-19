package com.example.data.payment

import android.app.Activity
import android.util.Log
import com.example.BuildConfig
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class CreateRazorpayOrderRequest(
    @Json(name = "amountPaise") val amountPaise: Long,
    @Json(name = "currency") val currency: String = "INR",
    @Json(name = "receipt") val receipt: String
)

data class CreateRazorpayOrderResponse(
    @Json(name = "orderId") val orderId: String,
    @Json(name = "amountPaise") val amountPaise: Long,
    @Json(name = "currency") val currency: String = "INR"
)

data class VerifyRazorpayPaymentRequest(
    @Json(name = "razorpayOrderId") val razorpayOrderId: String,
    @Json(name = "razorpayPaymentId") val razorpayPaymentId: String,
    @Json(name = "razorpaySignature") val razorpaySignature: String
)

data class VerifyRazorpayPaymentResponse(
    @Json(name = "verified") val verified: Boolean
)

interface PaymentApiService {
    @POST("create-razorpay-order")
    suspend fun createOrder(@Body request: CreateRazorpayOrderRequest): CreateRazorpayOrderResponse

    @POST("verify-razorpay-payment")
    suspend fun verifyPayment(@Body request: VerifyRazorpayPaymentRequest): VerifyRazorpayPaymentResponse
}

sealed interface RazorpayOutcome {
    data class Success(
        val paymentId: String,
        val orderId: String?,
        val signature: String?
    ) : RazorpayOutcome

    data object Cancelled : RazorpayOutcome

    data class Error(
        val code: Int,
        val message: String
    ) : RazorpayOutcome
}

object RazorpayCheckoutBridge {
    private val _outcomes = MutableSharedFlow<RazorpayOutcome>(extraBufferCapacity = 2)
    val outcomes: SharedFlow<RazorpayOutcome> = _outcomes.asSharedFlow()

    fun emitSuccess(paymentId: String?, paymentData: PaymentData?) {
        val safePaymentId = paymentId ?: paymentData?.paymentId ?: ""
        if (safePaymentId.isBlank()) {
            _outcomes.tryEmit(RazorpayOutcome.Error(-1, "Missing payment reference from Razorpay."))
            return
        }
        _outcomes.tryEmit(
            RazorpayOutcome.Success(
                paymentId = safePaymentId,
                orderId = paymentData?.orderId,
                signature = paymentData?.signature
            )
        )
    }

    fun emitError(code: Int, response: String?) {
        val cleanMessage = parseErrorMessage(response)
        val isUserCancellation = code == Checkout.PAYMENT_CANCELED ||
            code == 0 ||
            cleanMessage.contains("cancel", ignoreCase = true)
        if (isUserCancellation) {
            _outcomes.tryEmit(RazorpayOutcome.Cancelled)
        } else {
            _outcomes.tryEmit(
                RazorpayOutcome.Error(
                    code = code,
                    message = cleanMessage.ifBlank { "Payment could not be completed. Please try again." }
                )
            )
        }
    }

    private fun parseErrorMessage(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank() || trimmed.equals("undefined", ignoreCase = true)) return ""
        if (trimmed.startsWith("{")) {
            return try {
                val root = JSONObject(trimmed)
                val errorObj = root.optJSONObject("error")
                val desc = errorObj?.optString("description").orEmpty()
                val reason = errorObj?.optString("reason").orEmpty()
                desc.ifBlank { reason }.ifBlank { root.optString("description") }
            } catch (_: Exception) {
                ""
            }
        }
        return trimmed
    }
}

object RazorpayPaymentGateway {
    private const val TAG = "RazorpayGateway"

    val keyId: String
        get() = BuildConfig.RAZORPAY_KEY_ID.trim()

    val backendBaseUrl: String
        get() = BuildConfig.PAYMENT_BACKEND_URL.trim()

    fun isKeyConfigured(): Boolean =
        keyId.isNotBlank() &&
            !keyId.contains("PLACEHOLDER", ignoreCase = true) &&
            keyId.startsWith("rzp_")

    fun isBackendConfigured(): Boolean =
        backendBaseUrl.startsWith("https://", ignoreCase = true) &&
            !backendBaseUrl.contains("PLACEHOLDER", ignoreCase = true)

    fun isOnlineCheckoutReady(): Boolean = isKeyConfigured()

    private fun createApiService(): PaymentApiService {
        val normalizedBase = if (backendBaseUrl.endsWith("/")) backendBaseUrl else "$backendBaseUrl/"
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedBase)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PaymentApiService::class.java)
    }

    suspend fun createServerOrderId(amountInr: Long, receipt: String): Result<String> {
        if (!isBackendConfigured()) {
            return Result.failure(IllegalStateException("Payment backend URL is not configured."))
        }
        return try {
            val response = createApiService().createOrder(
                CreateRazorpayOrderRequest(
                    amountPaise = amountInr * 100L,
                    currency = "INR",
                    receipt = receipt
                )
            )
            Result.success(response.orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Razorpay order on backend", e)
            Result.failure(e)
        }
    }

    suspend fun verifyPaymentSignature(
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<Boolean> {
        if (!isBackendConfigured()) {
            return Result.failure(IllegalStateException("Payment backend URL is not configured."))
        }
        return try {
            val response = createApiService().verifyPayment(
                VerifyRazorpayPaymentRequest(
                    razorpayOrderId = orderId,
                    razorpayPaymentId = paymentId,
                    razorpaySignature = signature
                )
            )
            Result.success(response.verified)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify Razorpay payment signature", e)
            Result.failure(e)
        }
    }

    fun launchCheckout(
        activity: Activity,
        amountInr: Long,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        razorpayOrderId: String? = null
    ): Result<Unit> {
        if (!isKeyConfigured()) {
            return Result.failure(IllegalStateException("Online payment key is not configured yet."))
        }
        return try {
            val checkout = Checkout()
            checkout.setKeyID(keyId)
            val options = JSONObject().apply {
                put("name", "Bunty's Bag")
                put("description", "Traditional Handmade Kutchi Crafts")
                put("theme.color", "#8B2635")
                put("currency", "INR")
                put("amount", amountInr * 100L)
                if (!razorpayOrderId.isNullOrBlank()) {
                    put("order_id", razorpayOrderId)
                }
                put(
                    "retry",
                    JSONObject().apply {
                        put("enabled", true)
                        put("max_count", 3)
                    }
                )
                put(
                    "prefill",
                    JSONObject().apply {
                        if (customerName.isNotBlank()) put("name", customerName)
                        if (customerEmail.isNotBlank()) put("email", customerEmail)
                        if (customerPhone.isNotBlank()) put("contact", customerPhone)
                    }
                )
            }
            checkout.open(activity, options)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Razorpay Checkout", e)
            Result.failure(e)
        }
    }
}
