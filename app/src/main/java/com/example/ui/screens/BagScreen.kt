package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.catalog.KutchiCatalog
import com.example.data.model.CartItem
import com.example.data.model.KutchiProduct
import com.example.data.repository.StoreRepository
import com.example.ui.components.EmptyBazaarState
import com.example.ui.components.KutchiDiamondBorder
import com.example.ui.components.ProductArtwork
import com.example.ui.state.UiState

@Composable
fun BagScreen(
    cartState: UiState<List<CartItem>>,
    giftNote: String,
    onGiftNoteChange: (String) -> Unit,
    onProductClick: (KutchiProduct) -> Unit,
    onUpdateQuantity: (cartItemId: String, newQty: Long) -> Unit,
    onRemoveItem: (cartItemId: String) -> Unit,
    onProceedToCheckout: () -> Unit,
    onBrowseShop: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val startPad = contentPadding.calculateStartPadding(layoutDirection)
    val endPad = contentPadding.calculateEndPadding(layoutDirection)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = contentPadding.calculateTopPadding(),
                start = startPad,
                end = endPad
            )
            .consumeWindowInsets(contentPadding)
            .imePadding()
    ) {
        val useTwoPane = maxWidth >= 840.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1120.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Your Bag",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Packed in reusable handloom cloth in Bhuj",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                KutchiDiamondBorder()
            }

            when (cartState) {
                is UiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = contentPadding.calculateBottomPadding())
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    EmptyBazaarState(
                        title = "Couldn't load your bag",
                        subtitle = cartState.message,
                        icon = Icons.Outlined.ShoppingBag,
                        actionLabel = "Browse Crafts",
                        onAction = onBrowseShop,
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .padding(bottom = contentPadding.calculateBottomPadding())
                    )
                }
                is UiState.Success -> {
                    val items = cartState.data
                    if (items.isEmpty()) {
                        EmptyBazaarState(
                            title = "Your Bunty's Bag is empty",
                            subtitle = "Discover mirror-work totes, Ajrakh slings, and handwoven stoles straight from Kutch.",
                            icon = Icons.Outlined.ShoppingBag,
                            actionLabel = "Explore Kutchi Bazaar",
                            onAction = onBrowseShop,
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .padding(bottom = contentPadding.calculateBottomPadding())
                        )
                    } else {
                        val subtotal = items.sumOf { it.unitPriceInr * it.quantity }
                        val freeThreshold = StoreRepository.FREE_SHIPPING_THRESHOLD_INR
                        val shippingFee = if (subtotal >= freeThreshold) 0L else StoreRepository.STANDARD_SHIPPING_INR
                        val total = subtotal + shippingFee

                        if (useTwoPane) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier
                                    .widthIn(max = 1120.dp)
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                            ) {
                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        top = 6.dp,
                                        bottom = contentPadding.calculateBottomPadding() + 28.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .fillMaxHeight()
                                ) {
                                    items(items, key = { it.id.ifBlank { "${it.productId}_${it.variant}" } }) { item ->
                                        val product = KutchiCatalog.findById(item.productId)
                                        BagLineItemCard(
                                            item = item,
                                            onClick = { if (product != null) onProductClick(product) },
                                            onDecrease = { onUpdateQuantity(item.id, item.quantity - 1L) },
                                            onIncrease = {
                                                if (item.quantity < 20L) {
                                                    onUpdateQuantity(item.id, item.quantity + 1L)
                                                }
                                            },
                                            onRemove = { onRemoveItem(item.id) }
                                        )
                                    }
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .weight(0.95f)
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState())
                                        .padding(
                                            top = 6.dp,
                                            bottom = contentPadding.calculateBottomPadding() + 28.dp
                                        )
                                ) {
                                    ShippingProgressBanner(subtotal = subtotal, threshold = freeThreshold)
                                    GiftNoteCard(giftNote = giftNote, onGiftNoteChange = onGiftNoteChange)
                                    OrderSummaryCard(
                                        itemCount = items.sumOf { it.quantity },
                                        subtotal = subtotal,
                                        shippingFee = shippingFee,
                                        total = total,
                                        onProceedToCheckout = onProceedToCheckout
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 6.dp,
                                    bottom = contentPadding.calculateBottomPadding() + 28.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(key = "shipping_progress") {
                                    Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                                        ShippingProgressBanner(
                                            subtotal = subtotal,
                                            threshold = freeThreshold
                                        )
                                    }
                                }

                                items(items, key = { it.id.ifBlank { "${it.productId}_${it.variant}" } }) { item ->
                                    val product = KutchiCatalog.findById(item.productId)
                                    BagLineItemCard(
                                        item = item,
                                        onClick = { if (product != null) onProductClick(product) },
                                        onDecrease = { onUpdateQuantity(item.id, item.quantity - 1L) },
                                        onIncrease = {
                                            if (item.quantity < 20L) {
                                                onUpdateQuantity(item.id, item.quantity + 1L)
                                            }
                                        },
                                        onRemove = { onRemoveItem(item.id) },
                                        modifier = Modifier.widthIn(max = 680.dp)
                                    )
                                }

                                item(key = "gift_note") {
                                    GiftNoteCard(
                                        giftNote = giftNote,
                                        onGiftNoteChange = onGiftNoteChange,
                                        modifier = Modifier.widthIn(max = 680.dp)
                                    )
                                }

                                item(key = "summary") {
                                    OrderSummaryCard(
                                        itemCount = items.sumOf { it.quantity },
                                        subtotal = subtotal,
                                        shippingFee = shippingFee,
                                        total = total,
                                        onProceedToCheckout = onProceedToCheckout,
                                        modifier = Modifier.widthIn(max = 680.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GiftNoteCard(
    giftNote: String,
    onGiftNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Artisan Gift Note (Optional)",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = giftNote,
                onValueChange = { if (it.length <= 200) onGiftNoteChange(it) },
                placeholder = { Text("Add a handwritten message for our Bhuj studio to tuck inside…") },
                maxLines = 3,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bag_gift_note_input")
            )
        }
    }
}

@Composable
private fun OrderSummaryCard(
    itemCount: Long,
    subtotal: Long,
    shippingFee: Long,
    total: Long,
    onProceedToCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Order Summary",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummaryLine(
                label = "Crafts Subtotal ($itemCount items)",
                value = KutchiCatalog.formatInr(subtotal)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SummaryLine(
                label = "Handloom Cloth Packaging",
                value = "Complimentary"
            )
            Spacer(modifier = Modifier.height(6.dp))
            SummaryLine(
                label = "Insured Artisan Shipping",
                value = if (shippingFee == 0L) "Free" else KutchiCatalog.formatInr(shippingFee)
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Total Payable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = KutchiCatalog.formatInr(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onProceedToCheckout,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("proceed_to_checkout_button")
            ) {
                Text(
                    text = "Proceed to Checkout · ${KutchiCatalog.formatInr(total)}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ShippingProgressBanner(
    subtotal: Long,
    threshold: Long
) {
    val progress = (subtotal.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)
    val remaining = (threshold - subtotal).coerceAtLeast(0L)

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (remaining == 0L) {
                        "You unlocked free insured shipping from Bhuj!"
                    } else {
                        "Add ${KutchiCatalog.formatInr(remaining)} more for free shipping"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun BagLineItemCard(
    item: CartItem,
    onClick: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("bag_item_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            ProductArtwork(
                drawableKey = item.drawableKey,
                contentDescription = item.productName,
                modifier = Modifier.size(88.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.variant} · ${item.artisanOrigin}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("remove_bag_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove ${item.productName}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = KutchiCatalog.formatInr(item.unitPriceInr * item.quantity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDecrease,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(40.dp)
                                    .testTag("bag_minus_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Decrease quantity",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = item.quantity.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = onIncrease,
                                enabled = item.quantity < 20L,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(40.dp)
                                    .testTag("bag_plus_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Increase quantity",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
