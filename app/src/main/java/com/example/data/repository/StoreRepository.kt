package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.KutchiProduct
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ShippingAddress
import com.example.data.model.WishlistItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class StoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireActiveUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Please sign in with Google to continue.")
        check(uid == expectedUserId) { "Active account changed; refusing cross-account access." }
        return uid
    }

    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        val uid = requireActiveUserId(userId)
        val path = "users/$uid/cart"
        emitAll(
            db.collection("users").document(uid).collection("cart")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CartItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> = flow {
        val uid = requireActiveUserId(userId)
        val path = "users/$uid/wishlist"
        emitAll(
            db.collection("users").document(uid).collection("wishlist")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WishlistItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAddresses(userId: String): Flow<List<ShippingAddress>> = flow {
        val uid = requireActiveUserId(userId)
        val path = "users/$uid/addresses"
        emitAll(
            db.collection("users").document(uid).collection("addresses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ShippingAddress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeOrders(userId: String): Flow<List<Order>> = flow {
        val uid = requireActiveUserId(userId)
        val path = "users/$uid/orders"
        emitAll(
            db.collection("users").document(uid).collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getOrderItems(userId: String, orderId: String): Result<List<OrderLineItem>> {
        val path = "users/$userId/orders/$orderId/items"
        return try {
            val uid = requireActiveUserId(userId)
            require(orderId.isNotBlank()) { "Order ID must not be blank" }
            val snapshot = db.collection("users").document(uid)
                .collection("orders").document(orderId)
                .collection("items")
                .get()
                .await()
            val items = snapshot.toObjects(
                OrderLineItem::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun addToCart(
        userId: String,
        product: KutchiProduct,
        variant: String,
        quantity: Int = 1
    ): Result<Unit> {
        val cartItemId = buildCartDocId(product.id, variant)
        val path = "users/$userId/cart/$cartItemId"
        return try {
            val uid = requireActiveUserId(userId)
            val safeVariant = variant.trim().take(60)
            require(safeVariant.isNotEmpty()) { "Variant must not be empty" }
            require(quantity in 1..20) { "Quantity must be between 1 and 20" }

            val docRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val currentQty = existingSnap.getLong("quantity") ?: 1L
                val updatedQty = (currentQty + quantity).coerceIn(1L, 20L)
                docRef.update(
                    mapOf(
                        "quantity" to updatedQty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "productId" to product.id.take(64),
                    "productName" to product.name.take(120),
                    "artisanOrigin" to product.artisanVillage.take(80),
                    "craftType" to product.craftType.take(80),
                    "variant" to safeVariant,
                    "unitPriceInr" to product.priceInr,
                    "quantity" to quantity.toLong().coerceIn(1L, 20L),
                    "drawableKey" to product.drawableKey.take(64),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(
        userId: String,
        cartItemId: String,
        newQuantity: Long
    ): Result<Unit> {
        val path = "users/$userId/cart/$cartItemId"
        return try {
            val uid = requireActiveUserId(userId)
            require(cartItemId.isNotBlank()) { "Cart item ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
            if (newQuantity <= 0L) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQuantity.coerceIn(1L, 20L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(userId: String, cartItemId: String): Result<Unit> {
        val path = "users/$userId/cart/$cartItemId"
        return try {
            val uid = requireActiveUserId(userId)
            require(cartItemId.isNotBlank()) { "Cart item ID must not be blank" }
            db.collection("users").document(uid)
                .collection("cart").document(cartItemId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleWishlist(
        userId: String,
        product: KutchiProduct,
        isCurrentlySaved: Boolean
    ): Result<Boolean> {
        val path = "users/$userId/wishlist/${product.id}"
        return try {
            val uid = requireActiveUserId(userId)
            val docRef = db.collection("users").document(uid)
                .collection("wishlist").document(product.id)
            if (isCurrentlySaved) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "productId" to product.id.take(64),
                    "productName" to product.name.take(120),
                    "unitPriceInr" to product.priceInr,
                    "drawableKey" to product.drawableKey.take(64),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAddress(
        userId: String,
        recipientName: String,
        phone: String,
        streetLine: String,
        city: String,
        state: String,
        postalCode: String,
        label: String,
        preferred: Boolean
    ): Result<ShippingAddress> {
        val path = "users/$userId/addresses"
        return try {
            val uid = requireActiveUserId(userId)
            val cleanName = recipientName.trim()
            val cleanPhone = phone.trim()
            val cleanStreet = streetLine.trim()
            val cleanCity = city.trim()
            val cleanState = state.trim()
            val cleanPostal = postalCode.trim()
            val cleanLabel = label.trim().ifEmpty { "Home" }

            require(cleanName.length in 2..80) { "Name must be 2 to 80 characters" }
            require(cleanPhone.length in 7..20) { "Enter a valid phone number (7–20 digits)" }
            require(cleanStreet.length in 4..160) { "Street address must be at least 4 characters" }
            require(cleanCity.length in 2..60) { "City must be at least 2 characters" }
            require(cleanState.length in 2..60) { "State must be at least 2 characters" }
            require(cleanPostal.length in 4..12) { "Postal code must be 4 to 12 characters" }
            require(cleanLabel.length in 1..30) { "Label must be 1 to 30 characters" }

            val docRef = db.collection("users").document(uid).collection("addresses").document()
            val payload = mapOf(
                "recipientName" to cleanName,
                "phone" to cleanPhone,
                "streetLine" to cleanStreet,
                "city" to cleanCity,
                "state" to cleanState,
                "postalCode" to cleanPostal,
                "label" to cleanLabel,
                "preferred" to preferred,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(
                ShippingAddress(
                    id = docRef.id,
                    recipientName = cleanName,
                    phone = cleanPhone,
                    streetLine = cleanStreet,
                    city = cleanCity,
                    state = cleanState,
                    postalCode = cleanPostal,
                    label = cleanLabel,
                    preferred = preferred
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(userId: String, addressId: String): Result<Unit> {
        val path = "users/$userId/addresses/$addressId"
        return try {
            val uid = requireActiveUserId(userId)
            require(addressId.isNotBlank()) { "Address ID must not be blank" }
            db.collection("users").document(uid)
                .collection("addresses").document(addressId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        userId: String,
        cartItems: List<CartItem>,
        address: ShippingAddress,
        paymentMethod: PaymentMethod,
        giftNote: String
    ): Result<String> {
        val path = "users/$userId/orders"
        return try {
            val uid = requireActiveUserId(userId)
            require(cartItems.isNotEmpty()) { "Your bag is empty" }
            val subtotal = cartItems.sumOf { it.unitPriceInr * it.quantity }
            val shipping = if (subtotal >= FREE_SHIPPING_THRESHOLD_INR) 0L else STANDARD_SHIPPING_INR
            val total = subtotal + shipping
            val totalItems = cartItems.sumOf { it.quantity }.coerceIn(1L, 200L)
            val cleanGiftNote = giftNote.trim().take(200)
            val summary = address.formattedSummary().take(240)
            require(summary.length >= 5) { "Delivery address is incomplete" }

            val orderNumber = "BB-${Random.nextInt(100000, 999999)}"
            val orderRef = db.collection("users").document(uid).collection("orders").document()
            val primaryItem = cartItems.first()

            val orderPayload = mapOf(
                "orderNumber" to orderNumber,
                "status" to OrderStatus.PLACED.raw,
                "paymentMethod" to paymentMethod.raw,
                "recipientName" to address.recipientName.take(80),
                "phone" to address.phone.take(20),
                "shippingSummary" to summary,
                "giftNote" to cleanGiftNote,
                "subtotalInr" to subtotal,
                "shippingInr" to shipping,
                "totalInr" to total,
                "itemCount" to totalItems,
                "previewTitle" to primaryItem.productName.take(120),
                "previewDrawableKey" to primaryItem.drawableKey.take(64),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(orderRef, orderPayload)

            cartItems.forEach { cartItem ->
                val itemId = cartItem.id.ifBlank { buildCartDocId(cartItem.productId, cartItem.variant) }
                val orderItemRef = orderRef.collection("items").document(itemId)
                batch.set(
                    orderItemRef,
                    mapOf(
                        "productId" to cartItem.productId.take(64),
                        "productName" to cartItem.productName.take(120),
                        "variant" to cartItem.variant.take(60),
                        "unitPriceInr" to cartItem.unitPriceInr,
                        "quantity" to cartItem.quantity.coerceIn(1L, 20L),
                        "drawableKey" to cartItem.drawableKey.take(64),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                val cartRef = db.collection("users").document(uid).collection("cart").document(itemId)
                batch.delete(cartRef)
            }

            batch.commit().await()
            Result.success(orderNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(userId: String, orderId: String): Result<Unit> {
        val path = "users/$userId/orders/$orderId"
        return try {
            val uid = requireActiveUserId(userId)
            require(orderId.isNotBlank()) { "Order ID must not be blank" }
            db.collection("users").document(uid)
                .collection("orders").document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.CANCELLED.raw,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val FREE_SHIPPING_THRESHOLD_INR = 1999L
        const val STANDARD_SHIPPING_INR = 120L

        fun buildCartDocId(productId: String, variant: String): String {
            val cleanProduct = productId.lowercase().replace(Regex("[^a-z0-9_\\-]"), "_")
            val cleanVariant = variant.lowercase().replace(Regex("[^a-z0-9_\\-]"), "_")
            return "${cleanProduct}__${cleanVariant}".take(120)
        }
    }
}
