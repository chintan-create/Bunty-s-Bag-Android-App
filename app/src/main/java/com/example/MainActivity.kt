package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.deleteAccountAndData
import com.example.auth.signOutUser
import com.example.data.catalog.KutchiCatalog
import com.example.data.payment.RazorpayCheckoutBridge
import com.example.data.repository.StoreRepository
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BagScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.OrdersProfileScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.state.StoreViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.BuntysBagTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object StoreRoutes {
    const val ROUTE_SHOP = "shop"
    const val ROUTE_WISHLIST = "wishlist"
    const val ROUTE_BAG = "bag"
    const val ROUTE_ORDERS = "orders"
    const val ROUTE_CHECKOUT = "checkout"
    const val ROUTE_PRIVACY_POLICY = "privacy_policy"
    const val ROUTE_PRODUCT_DETAIL = "product/{productId}"

    fun productDetail(productId: String): String = "product/$productId"
}

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelTabs = listOf(
    BottomTab(
        route = StoreRoutes.ROUTE_SHOP,
        labelRes = R.string.nav_shop,
        selectedIcon = Icons.Filled.Storefront,
        unselectedIcon = Icons.Outlined.Storefront,
        testTag = "nav_tab_shop"
    ),
    BottomTab(
        route = StoreRoutes.ROUTE_WISHLIST,
        labelRes = R.string.nav_wishlist,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_tab_wishlist"
    ),
    BottomTab(
        route = StoreRoutes.ROUTE_BAG,
        labelRes = R.string.nav_bag,
        selectedIcon = Icons.Filled.ShoppingBag,
        unselectedIcon = Icons.Outlined.ShoppingBag,
        testTag = "nav_tab_bag"
    ),
    BottomTab(
        route = StoreRoutes.ROUTE_ORDERS,
        labelRes = R.string.nav_orders,
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_tab_orders"
    )
)

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuntysBagTheme {
                AppNavigation()
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        RazorpayCheckoutBridge.emitSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        RazorpayCheckoutBridge.emitError(code, response)
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: StoreViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        StoreViewModel(
                            repository = StoreRepository(db, auth),
                            currentUserId = user.uid
                        )
                    }
                }
            )
            MainStoreScaffold(
                viewModel = viewModel,
                userDisplayName = user.displayName ?: "Kutchi Patron",
                userEmail = user.email ?: "",
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                },
                onDeleteAccount = {
                    deleteAccountAndData(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        deleteUserData = { viewModel.deleteAllUserData() },
                        onAccountDeleted = {},
                        onError = { msg -> viewModel.showMessage(msg) }
                    )
                }
            )
        }
    }
}

@Composable
private fun MainStoreScaffold(
    viewModel: StoreViewModel,
    userDisplayName: String,
    userEmail: String,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val wishlistState by viewModel.wishlistState.collectAsStateWithLifecycle()
    val addressesState by viewModel.addressesState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val orderItemsCache by viewModel.orderItemsCache.collectAsStateWithLifecycle()
    val isActionBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()

    var giftNote by rememberSaveable { mutableStateOf("") }

    val wishlistIds = remember(wishlistState) {
        (wishlistState as? UiState.Success)?.data?.map { it.productId }?.toSet() ?: emptySet()
    }
    val bagCount = remember(cartState) {
        (cartState as? UiState.Success)?.data?.sumOf { it.quantity }?.toInt() ?: 0
    }

    val isTopLevelRoute = TopLevelTabs.any { it.route == currentRoute }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevelRoute && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        bagCount = bagCount
                                    )
                                },
                                label = { Text(stringResource(id = tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                if (isTopLevelRoute && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TopLevelTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        bagCount = bagCount
                                    )
                                },
                                label = { Text(stringResource(id = tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = StoreRoutes.ROUTE_SHOP,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(StoreRoutes.ROUTE_SHOP) {
                        ShopScreen(
                            products = products,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            sortOption = sortOption,
                            wishlistProductIds = wishlistIds,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onCategorySelected = viewModel::selectCategory,
                            onSortOptionSelected = viewModel::selectSortOption,
                            onProductClick = { product ->
                                navController.navigate(StoreRoutes.productDetail(product.id))
                            },
                            onToggleWishlist = viewModel::toggleWishlist,
                            onQuickAddToCart = { product ->
                                val defaultVariant = product.variants.firstOrNull() ?: "Artisan Original"
                                viewModel.addToCart(product, defaultVariant, 1)
                            },
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                        )
                    }

                    composable(StoreRoutes.ROUTE_WISHLIST) {
                        WishlistScreen(
                            wishlistState = wishlistState,
                            onProductClick = { product ->
                                navController.navigate(StoreRoutes.productDetail(product.id))
                            },
                            onMoveToBag = { product ->
                                val defaultVariant = product.variants.firstOrNull() ?: "Artisan Original"
                                viewModel.addToCart(product, defaultVariant, 1)
                            },
                            onRemoveFromWishlist = { product ->
                                viewModel.toggleWishlist(product, isCurrentlySaved = true)
                            },
                            onBrowseShop = { navigateToTab(StoreRoutes.ROUTE_SHOP) },
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                        )
                    }

                    composable(StoreRoutes.ROUTE_BAG) {
                        BagScreen(
                            cartState = cartState,
                            giftNote = giftNote,
                            onGiftNoteChange = { giftNote = it },
                            onProductClick = { product ->
                                navController.navigate(StoreRoutes.productDetail(product.id))
                            },
                            onUpdateQuantity = viewModel::updateCartQuantity,
                            onRemoveItem = viewModel::removeFromCart,
                            onProceedToCheckout = {
                                navController.navigate(StoreRoutes.ROUTE_CHECKOUT)
                            },
                            onBrowseShop = { navigateToTab(StoreRoutes.ROUTE_SHOP) },
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                        )
                    }

                    composable(StoreRoutes.ROUTE_ORDERS) {
                        OrdersProfileScreen(
                            userDisplayName = userDisplayName,
                            userEmail = userEmail,
                            ordersState = ordersState,
                            addressesState = addressesState,
                            orderItemsCache = orderItemsCache,
                            isBusy = isActionBusy,
                            onExpandOrder = viewModel::loadOrderItemsIfNeeded,
                            onCancelOrder = viewModel::cancelOrder,
                            onSaveAddress = { name, phone, street, city, state, postal, label, preferred ->
                                viewModel.saveAddress(name, phone, street, city, state, postal, label, preferred)
                            },
                            onDeleteAddress = viewModel::deleteAddress,
                            onBrowseShop = { navigateToTab(StoreRoutes.ROUTE_SHOP) },
                            onSignOut = onSignOut,
                            onDeleteAccount = onDeleteAccount,
                            onOpenPrivacyPolicy = {
                                navController.navigate(StoreRoutes.ROUTE_PRIVACY_POLICY)
                            },
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                        )
                    }

                    composable(
                        route = StoreRoutes.ROUTE_PRODUCT_DETAIL,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { entry ->
                        val productId = entry.arguments?.getString("productId") ?: ""
                        val product = KutchiCatalog.findById(productId) ?: KutchiCatalog.products.first()
                        val isSaved = wishlistIds.contains(product.id)
                        ProductDetailScreen(
                            product = product,
                            isSaved = isSaved,
                            isBusy = isActionBusy,
                            onBack = { navController.popBackStack() },
                            onToggleWishlist = { viewModel.toggleWishlist(product, isSaved) },
                            onAddToBag = { variant, qty ->
                                viewModel.addToCart(product, variant, qty)
                            }
                        )
                    }

                    composable(StoreRoutes.ROUTE_CHECKOUT) {
                        CheckoutScreen(
                            cartState = cartState,
                            addressesState = addressesState,
                            giftNote = giftNote,
                            isBusy = isActionBusy,
                            defaultRecipientName = userDisplayName,
                            userEmail = userEmail,
                            onBack = { navController.popBackStack() },
                            onSaveAddress = { name, phone, street, city, state, postal, label, preferred, onSaved ->
                                viewModel.saveAddress(
                                    name, phone, street, city, state, postal, label, preferred, onSaved
                                )
                            },
                            onPlaceOrder = { items, address, payment, note, paymentRef, onOrderPlaced ->
                                viewModel.placeOrder(items, address, payment, note, paymentRef) { orderNum ->
                                    giftNote = ""
                                    onOrderPlaced(orderNum)
                                }
                            },
                            onViewOrders = {
                                navController.popBackStack(StoreRoutes.ROUTE_SHOP, inclusive = false)
                                navigateToTab(StoreRoutes.ROUTE_ORDERS)
                            },
                            onContinueShopping = {
                                navController.popBackStack(StoreRoutes.ROUTE_SHOP, inclusive = false)
                                navigateToTab(StoreRoutes.ROUTE_SHOP)
                            }
                        )
                    }

                    composable(StoreRoutes.ROUTE_PRIVACY_POLICY) {
                        PrivacyPolicyScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: BottomTab,
    selected: Boolean,
    bagCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    val label = stringResource(id = tab.labelRes)
    if (tab.route == StoreRoutes.ROUTE_BAG && bagCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (bagCount > 99) "99+" else bagCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = label)
    }
}
