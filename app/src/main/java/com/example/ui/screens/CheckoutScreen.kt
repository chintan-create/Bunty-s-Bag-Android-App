package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.catalog.KutchiCatalog
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.ShippingAddress
import com.example.data.payment.RazorpayCheckoutBridge
import com.example.data.payment.RazorpayOutcome
import com.example.data.payment.RazorpayPaymentGateway
import com.example.data.repository.StoreRepository
import com.example.ui.components.KutchiDiamondBorder
import com.example.ui.state.UiState
import kotlinx.coroutines.launch

private data class PendingCheckoutSnapshot(
    val cartItems: List<CartItem>,
    val address: ShippingAddress,
    val giftNote: String
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartState: UiState<List<CartItem>>,
    addressesState: UiState<List<ShippingAddress>>,
    giftNote: String,
    isBusy: Boolean,
    defaultRecipientName: String,
    userEmail: String,
    onBack: () -> Unit,
    onSaveAddress: (
        recipientName: String,
        phone: String,
        streetLine: String,
        city: String,
        state: String,
        postalCode: String,
        label: String,
        preferred: Boolean,
        onSaved: (ShippingAddress) -> Unit
    ) -> Unit,
    onPlaceOrder: (
        cartItems: List<CartItem>,
        address: ShippingAddress,
        paymentMethod: PaymentMethod,
        giftNote: String,
        paymentReference: String,
        onOrderPlaced: (String) -> Unit
    ) -> Unit,
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val razorpayReady = remember { RazorpayPaymentGateway.isOnlineCheckoutReady() }
    val backendVerifyEnabled = remember { RazorpayPaymentGateway.isBackendConfigured() }

    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var showAddressForm by remember { mutableStateOf(false) }
    var selectedPayment by remember {
        mutableStateOf(
            if (razorpayReady) PaymentMethod.RAZORPAY_ONLINE else PaymentMethod.UPI_COLLECT
        )
    }
    var placedOrderNumber by remember { mutableStateOf<String?>(null) }
    var isGatewayBusy by remember { mutableStateOf(false) }
    var gatewayError by remember { mutableStateOf<String?>(null) }
    var pendingRazorpayOrderId by remember { mutableStateOf<String?>(null) }
    var pendingSnapshot by remember { mutableStateOf<PendingCheckoutSnapshot?>(null) }

    // Form fields
    var recipientName by remember(defaultRecipientName) { mutableStateOf(defaultRecipientName) }
    var phone by remember { mutableStateOf("") }
    var streetLine by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var addressLabel by remember { mutableStateOf("Home") }
    var formError by remember { mutableStateOf<String?>(null) }

    val addresses = (addressesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(addresses) {
        if (selectedAddressId == null && addresses.isNotEmpty()) {
            selectedAddressId = (addresses.firstOrNull { it.preferred } ?: addresses.first()).id
        }
        if (addresses.isEmpty() && addressesState is UiState.Success) {
            showAddressForm = true
        }
    }

    LaunchedEffect(Unit) {
        RazorpayCheckoutBridge.outcomes.collect { outcome ->
            when (outcome) {
                is RazorpayOutcome.Success -> {
                    val snapshot = pendingSnapshot
                    if (snapshot == null) {
                        isGatewayBusy = false
                    } else if (backendVerifyEnabled) {
                        val orderId = outcome.orderId ?: pendingRazorpayOrderId ?: ""
                        val signature = outcome.signature ?: ""
                        if (orderId.isBlank() || signature.isBlank()) {
                            isGatewayBusy = false
                            gatewayError = "Payment confirmation was incomplete. Please try again."
                        } else {
                            val verifyResult = RazorpayPaymentGateway.verifyPaymentSignature(
                                orderId = orderId,
                                paymentId = outcome.paymentId,
                                signature = signature
                            )
                            isGatewayBusy = false
                            verifyResult.onSuccess { verified ->
                                if (verified) {
                                    gatewayError = null
                                    onPlaceOrder(
                                        snapshot.cartItems,
                                        snapshot.address,
                                        PaymentMethod.RAZORPAY_ONLINE,
                                        snapshot.giftNote,
                                        outcome.paymentId
                                    ) { orderNum ->
                                        pendingSnapshot = null
                                        pendingRazorpayOrderId = null
                                        placedOrderNumber = orderNum
                                    }
                                } else {
                                    gatewayError = "Payment could not be verified. Please retry."
                                }
                            }.onFailure { err ->
                                gatewayError = err.message ?: "Unable to verify payment."
                            }
                        }
                    } else {
                        isGatewayBusy = false
                        gatewayError = null
                        onPlaceOrder(
                            snapshot.cartItems,
                            snapshot.address,
                            PaymentMethod.RAZORPAY_ONLINE,
                            snapshot.giftNote,
                            outcome.paymentId
                        ) { orderNum ->
                            pendingSnapshot = null
                            pendingRazorpayOrderId = null
                            placedOrderNumber = orderNum
                        }
                    }
                }
                is RazorpayOutcome.Cancelled -> {
                    isGatewayBusy = false
                    pendingSnapshot = null
                    pendingRazorpayOrderId = null
                    gatewayError = null
                }
                is RazorpayOutcome.Error -> {
                    isGatewayBusy = false
                    gatewayError = outcome.message
                }
            }
        }
    }

    if (placedOrderNumber != null) {
        OrderConfirmedView(
            orderNumber = placedOrderNumber ?: "",
            onViewOrders = onViewOrders,
            onContinueShopping = onContinueShopping
        )
        return
    }

    val cartItems = (cartState as? UiState.Success)?.data.orEmpty()
    val subtotal = cartItems.sumOf { it.unitPriceInr * it.quantity }
    val shipping = if (subtotal >= StoreRepository.FREE_SHIPPING_THRESHOLD_INR || subtotal == 0L) {
        0L
    } else {
        StoreRepository.STANDARD_SHIPPING_INR
    }
    val total = subtotal + shipping
    val selectedAddress = addresses.firstOrNull { it.id == selectedAddressId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Checkout",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to bag"
                        )
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    val totalBusy = isBusy || isGatewayBusy
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                        ) {
                        Button(
                            onClick = {
                                if (selectedAddress == null) return@Button
                                gatewayError = null
                                if (selectedPayment == PaymentMethod.RAZORPAY_ONLINE) {
                                    val activity = context.findActivity()
                                    if (activity == null) {
                                        gatewayError = "Unable to access host activity for Razorpay checkout."
                                        return@Button
                                    }
                                    isGatewayBusy = true
                                    val snapshot = PendingCheckoutSnapshot(
                                        cartItems = cartItems,
                                        address = selectedAddress,
                                        giftNote = giftNote
                                    )
                                    pendingSnapshot = snapshot
                                    if (backendVerifyEnabled) {
                                        coroutineScope.launch {
                                            val receipt = "bb_${System.currentTimeMillis()}"
                                            val orderResult = RazorpayPaymentGateway.createServerOrderId(total, receipt)
                                            orderResult.onSuccess { serverOrderId ->
                                                pendingRazorpayOrderId = serverOrderId
                                                val launchResult = RazorpayPaymentGateway.launchCheckout(
                                                    activity = activity,
                                                    amountInr = total,
                                                    customerName = selectedAddress.recipientName,
                                                    customerEmail = userEmail,
                                                    customerPhone = selectedAddress.phone,
                                                    razorpayOrderId = serverOrderId
                                                )
                                                launchResult.onFailure { err ->
                                                    isGatewayBusy = false
                                                    gatewayError = err.message ?: "Failed to open Razorpay checkout."
                                                }
                                            }.onFailure { err ->
                                                isGatewayBusy = false
                                                gatewayError = err.message ?: "Could not create Razorpay order on backend."
                                            }
                                        }
                                    } else {
                                        val launchResult = RazorpayPaymentGateway.launchCheckout(
                                            activity = activity,
                                            amountInr = total,
                                            customerName = selectedAddress.recipientName,
                                            customerEmail = userEmail,
                                            customerPhone = selectedAddress.phone,
                                            razorpayOrderId = null
                                        )
                                        launchResult.onFailure { err ->
                                            isGatewayBusy = false
                                            gatewayError = err.message ?: "Failed to open Razorpay checkout."
                                        }
                                    }
                                } else {
                                    onPlaceOrder(
                                        cartItems,
                                        selectedAddress,
                                        selectedPayment,
                                        giftNote,
                                        ""
                                    ) { orderNum ->
                                        placedOrderNumber = orderNum
                                    }
                                }
                            },
                            enabled = selectedAddress != null && !totalBusy && !showAddressForm &&
                                (selectedPayment != PaymentMethod.RAZORPAY_ONLINE || razorpayReady),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("place_order_button")
                        ) {
                            if (totalBusy) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = when {
                                        selectedAddress == null -> "Save Delivery Address to Continue"
                                        selectedPayment == PaymentMethod.RAZORPAY_ONLINE ->
                                            "Pay & Place Order · ${KutchiCatalog.formatInr(total)}"
                                        else -> "Place Order · ${KutchiCatalog.formatInr(total)}"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                // Delivery Address Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "1. Delivery Address",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (addresses.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { showAddressForm = !showAddressForm },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_new_address_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showAddressForm) "Cancel" else "Add New")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showAddressForm) {
                    AddressEntryCard(
                        recipientName = recipientName,
                        onRecipientNameChange = { recipientName = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        streetLine = streetLine,
                        onStreetLineChange = { streetLine = it },
                        city = city,
                        onCityChange = { city = it },
                        stateName = stateName,
                        onStateNameChange = { stateName = it },
                        postalCode = postalCode,
                        onPostalCodeChange = { postalCode = it },
                        label = addressLabel,
                        onLabelChange = { addressLabel = it },
                        formError = formError,
                        isBusy = isBusy,
                        onSave = {
                            val validation = validateAddressFields(
                                recipientName, phone, streetLine, city, stateName, postalCode
                            )
                            if (validation != null) {
                                formError = validation
                            } else {
                                formError = null
                                onSaveAddress(
                                    recipientName,
                                    phone,
                                    streetLine,
                                    city,
                                    stateName,
                                    postalCode,
                                    addressLabel,
                                    addresses.isEmpty()
                                ) { saved ->
                                    selectedAddressId = saved.id
                                    showAddressForm = false
                                }
                            }
                        }
                    )
                } else {
                    addresses.forEach { addr ->
                        val selected = addr.id == selectedAddressId
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable { selectedAddressId = addr.id }
                                .testTag("checkout_address_${addr.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { selectedAddressId = addr.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = addr.recipientName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = addr.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
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
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Payment Method
                Text(
                    text = "2. Payment Option",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!gatewayError.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = gatewayError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                PaymentMethod.entries.forEach { method ->
                    val isRazorpay = method == PaymentMethod.RAZORPAY_ONLINE
                    val optionEnabled = !isRazorpay || razorpayReady
                    val selected = method == selectedPayment
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .alpha(if (optionEnabled) 1f else 0.65f)
                            .then(
                                if (optionEnabled) {
                                    Modifier.clickable {
                                        gatewayError = null
                                        selectedPayment = method
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("payment_${method.raw}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            RadioButton(
                                selected = selected,
                                enabled = optionEnabled,
                                onClick = if (optionEnabled) {
                                    {
                                        gatewayError = null
                                        selectedPayment = method
                                    }
                                } else null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = method.displayLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isRazorpay && !razorpayReady) {
                                        "Add RAZORPAY_KEY_ID in Secrets to enable UPI, Cards & Netbanking"
                                    } else {
                                        method.subtitle
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Order Review Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "3. Bag Review (${cartItems.sumOf { it.quantity }} items)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        cartItems.forEach { item ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${item.quantity}× ${item.productName} (${item.variant})",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = KutchiCatalog.formatInr(item.unitPriceInr * item.quantity),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (giftNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gift note: \"$giftNote\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Shipping", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (shipping == 0L) "Free" else KutchiCatalog.formatInr(shipping),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Order Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = KutchiCatalog.formatInr(total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AddressEntryCard(
    recipientName: String,
    onRecipientNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    streetLine: String,
    onStreetLineChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    stateName: String,
    onStateNameChange: (String) -> Unit,
    postalCode: String,
    onPostalCodeChange: (String) -> Unit,
    label: String,
    onLabelChange: (String) -> Unit,
    formError: String?,
    isBusy: Boolean,
    onSave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "New Delivery Address",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            OutlinedTextField(
                value = recipientName,
                onValueChange = { if (it.length <= 80) onRecipientNameChange(it) },
                label = { Text("Recipient Full Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("address_name_input")
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 20) onPhoneChange(it) },
                label = { Text("Phone Number") },
                placeholder = { Text("+91 98250 00000") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("address_phone_input")
            )
            OutlinedTextField(
                value = streetLine,
                onValueChange = { if (it.length <= 160) onStreetLineChange(it) },
                label = { Text("House, Street & Locality") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("address_street_input")
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackPair = maxWidth < 300.dp
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (stackPair) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { if (it.length <= 60) onCityChange(it) },
                            label = { Text("City") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_city_input")
                        )
                        OutlinedTextField(
                            value = stateName,
                            onValueChange = { if (it.length <= 60) onStateNameChange(it) },
                            label = { Text("State") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_state_input")
                        )
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = { if (it.length <= 12) onPostalCodeChange(it) },
                            label = { Text("PIN / Postal Code") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_postal_input")
                        )
                        OutlinedTextField(
                            value = label,
                            onValueChange = { if (it.length <= 30) onLabelChange(it) },
                            label = { Text("Label (Home/Work)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("address_label_input")
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { if (it.length <= 60) onCityChange(it) },
                                label = { Text("City", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("address_city_input")
                            )
                            OutlinedTextField(
                                value = stateName,
                                onValueChange = { if (it.length <= 60) onStateNameChange(it) },
                                label = { Text("State", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("address_state_input")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = postalCode,
                                onValueChange = { if (it.length <= 12) onPostalCodeChange(it) },
                                label = { Text("PIN Code", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("address_postal_input")
                            )
                            OutlinedTextField(
                                value = label,
                                onValueChange = { if (it.length <= 30) onLabelChange(it) },
                                label = { Text("Label", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("address_label_input")
                            )
                        }
                    }
                }
            }

            if (!formError.isNullOrBlank()) {
                Text(
                    text = formError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onSave,
                enabled = !isBusy,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_address_button")
            ) {
                Text("Save Address")
            }
        }
    }
}

fun validateAddressFields(
    recipientName: String,
    phone: String,
    streetLine: String,
    city: String,
    state: String,
    postalCode: String
): String? {
    if (recipientName.trim().length < 2) return "Enter recipient name (at least 2 characters)."
    if (phone.trim().length < 7) return "Enter a valid contact phone number (at least 7 characters)."
    if (streetLine.trim().length < 4) return "Enter full street address (at least 4 characters)."
    if (city.trim().length < 2) return "Enter a valid city name."
    if (state.trim().length < 2) return "Enter a valid state or province."
    if (postalCode.trim().length < 4) return "Enter a valid PIN / postal code."
    return null
}

@Composable
private fun OrderConfirmedView(
    orderNumber: String,
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Order confirmed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Aabhar! Order Confirmed",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Order #$orderNumber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Our Bhuj artisans are preparing your handcrafted parcel in reusable cotton cloth.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            KutchiDiamondBorder()
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onViewOrders,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("view_orders_after_checkout")
            ) {
                Text("Track My Order")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onContinueShopping,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("continue_shopping_button")
            ) {
                Text("Continue Browsing Bazaar")
            }
        }
    }
}
