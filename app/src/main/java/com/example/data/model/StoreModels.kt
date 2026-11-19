package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ProductCategory(val displayName: String) {
    ALL("All Crafts"),
    TOTES("Totes & Shoulder"),
    SLINGS("Slings & Crossbody"),
    POTLIS("Potlis & Clutches"),
    CRAFTS("Artisan Crafts")
}

enum class SortOption(val label: String) {
    FEATURED("Curated"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low")
}

data class KutchiProduct(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: ProductCategory,
    val artisanVillage: String,
    val artisanName: String,
    val craftType: String,
    val priceInr: Long,
    val originalPriceInr: Long,
    val rating: Double,
    val reviewCount: Int,
    val dimensions: String,
    val materials: String,
    val careInstructions: String,
    val story: String,
    val variants: List<String>,
    val drawableKey: String,
    val featuredBadge: String? = null
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val artisanOrigin: String = "",
    val craftType: String = "",
    val variant: String = "",
    val unitPriceInr: Long = 0L,
    val quantity: Long = 1L,
    val drawableKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val unitPriceInr: Long = 0L,
    val drawableKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShippingAddress(
    @DocumentId val id: String = "",
    val recipientName: String = "",
    val phone: String = "",
    val streetLine: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val label: String = "Home",
    val preferred: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun formattedSummary(): String = "$streetLine, $city, $state $postalCode"
}

enum class OrderStatus(val raw: String, val displayLabel: String) {
    PLACED("placed", "Order Placed"),
    PACKING("packing", "Hand-Packing in Bhuj"),
    SHIPPED("shipped", "On the Way"),
    DELIVERED("delivered", "Delivered"),
    CANCELLED("cancelled", "Cancelled");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.raw == raw } ?: PLACED
    }
}

enum class PaymentMethod(val raw: String, val displayLabel: String, val subtitle: String) {
    UPI_COLLECT("upi_collect", "UPI on Dispatch", "Pay via any UPI app once artisans confirm"),
    COD("cod", "Cash on Delivery", "Pay in cash upon doorstep arrival"),
    CARD_ON_DELIVERY("card_on_delivery", "Card on Delivery", "Tap or swipe card with our courier");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.raw == raw } ?: UPI_COLLECT
    }
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val status: String = OrderStatus.PLACED.raw,
    val paymentMethod: String = PaymentMethod.UPI_COLLECT.raw,
    val recipientName: String = "",
    val phone: String = "",
    val shippingSummary: String = "",
    val giftNote: String = "",
    val subtotalInr: Long = 0L,
    val shippingInr: Long = 0L,
    val totalInr: Long = 0L,
    val itemCount: Long = 1L,
    val previewTitle: String = "",
    val previewDrawableKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val variant: String = "",
    val unitPriceInr: Long = 0L,
    val quantity: Long = 1L,
    val drawableKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
