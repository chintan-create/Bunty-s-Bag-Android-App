package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.catalog.KutchiCatalog
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ShippingAddress
import com.example.ui.components.EmptyBazaarState
import com.example.ui.components.KutchiDiamondBorder
import com.example.ui.components.ProductArtwork
import com.example.ui.state.UiState
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun OrdersProfileScreen(
    userDisplayName: String,
    userEmail: String,
    ordersState: UiState<List<Order>>,
    addressesState: UiState<List<ShippingAddress>>,
    orderItemsCache: Map<String, List<OrderLineItem>>,
    isBusy: Boolean,
    onExpandOrder: (String) -> Unit,
    onCancelOrder: (String) -> Unit,
    onSaveAddress: (
        recipientName: String,
        phone: String,
        streetLine: String,
        city: String,
        state: String,
        postalCode: String,
        label: String,
        preferred: Boolean
    ) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onBrowseShop: () -> Unit,
    onSignOut: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var showAddAddressDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 10.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Profile Header
        item(key = "profile_card") {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    val initials = userDisplayName.trim()
                        .split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                        .ifEmpty { "BB" }

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userDisplayName.ifBlank { "Kutchi Patron" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (userEmail.isNotBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Saved Addresses Section
        item(key = "addresses_section") {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Saved Delivery Addresses",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = { showAddAddressDialog = true },
                        modifier = Modifier.testTag("profile_add_address_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddLocationAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                when (addressesState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }
                    is UiState.Error -> {
                        Text(
                            text = addressesState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is UiState.Success -> {
                        if (addressesState.data.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "No saved delivery addresses yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                addressesState.data.forEach { addr ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${addr.recipientName} · ${addr.label}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = addr.formattedSummary(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = addr.phone,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteAddress(addr.id) },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("delete_address_${addr.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Delete address",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
        }

        item(key = "divider_orders") {
            Column {
                KutchiDiamondBorder()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Order History",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        when (ordersState) {
            is UiState.Loading -> {
                item(key = "orders_loading") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item(key = "orders_error") {
                    EmptyBazaarState(
                        title = "Couldn't load orders",
                        subtitle = ordersState.message,
                        icon = Icons.Outlined.Inventory2,
                        actionLabel = "Back to Shop",
                        onAction = onBrowseShop
                    )
                }
            }
            is UiState.Success -> {
                val orders = ordersState.data
                if (orders.isEmpty()) {
                    item(key = "orders_empty") {
                        EmptyBazaarState(
                            title = "No Kutchi orders yet",
                            subtitle = "When you place an order, you can track packaging from Bhuj and view receipts here.",
                            icon = Icons.Outlined.Inventory2,
                            actionLabel = "Shop Handmade Crafts",
                            onAction = onBrowseShop
                        )
                    }
                } else {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            lineItems = orderItemsCache[order.id],
                            isBusy = isBusy,
                            onExpand = { onExpandOrder(order.id) },
                            onCancel = { onCancelOrder(order.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddAddressDialog) {
        AddAddressDialog(
            defaultName = userDisplayName,
            isBusy = isBusy,
            onDismiss = { showAddAddressDialog = false },
            onSave = { name, phone, street, city, state, postal, label ->
                onSaveAddress(name, phone, street, city, state, postal, label, false)
                showAddAddressDialog = false
            }
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    lineItems: List<OrderLineItem>?,
    isBusy: Boolean,
    onExpand: () -> Unit,
    onCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val status = OrderStatus.fromRaw(order.status)
    val payment = PaymentMethod.fromRaw(order.paymentMethod)
    val displayTs = order.updatedAt ?: order.createdAt ?: Timestamp.now()
    val dateStr = remember(displayTs) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(displayTs.toDate())
    }

    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                if (expanded) onExpand()
            }
            .testTag("order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val (bgColor, fgColor) = when (status) {
                    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    OrderStatus.DELIVERED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                }
                Surface(
                    color = bgColor,
                    contentColor = fgColor,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = status.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductArtwork(
                    drawableKey = order.previewDrawableKey,
                    contentDescription = order.previewTitle,
                    modifier = Modifier.size(58.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.previewTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${order.itemCount} item(s) · ${payment.displayLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = KutchiCatalog.formatInr(order.totalInr),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse details" else "Expand details"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Deliver to ${order.recipientName} (${order.phone})",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = order.shippingSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (order.giftNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gift Note: ${order.giftNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Handcrafted Items",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (lineItems == null) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        lineItems.forEach { line ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${line.quantity}× ${line.productName} (${line.variant})",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = KutchiCatalog.formatInr(line.unitPriceInr * line.quantity),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (status == OrderStatus.PLACED) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !isBusy,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cancel_order_${order.id}")
                        ) {
                            Text(
                                text = "Cancel Order",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAddressDialog(
    defaultName: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postal by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("Home") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Delivery Address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AddressEntryCard(
                    recipientName = name,
                    onRecipientNameChange = { name = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    streetLine = street,
                    onStreetLineChange = { street = it },
                    city = city,
                    onCityChange = { city = it },
                    stateName = state,
                    onStateNameChange = { state = it },
                    postalCode = postal,
                    onPostalCodeChange = { postal = it },
                    label = label,
                    onLabelChange = { label = it },
                    formError = error,
                    isBusy = isBusy,
                    onSave = {
                        val validation = validateAddressFields(name, phone, street, city, state, postal)
                        if (validation != null) {
                            error = validation
                        } else {
                            onSave(name, phone, street, city, state, postal, label)
                        }
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
