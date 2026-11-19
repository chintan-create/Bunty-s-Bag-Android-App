package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.KutchiCatalog
import com.example.data.model.CartItem
import com.example.data.model.KutchiProduct
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.model.ShippingAddress
import com.example.data.model.SortOption
import com.example.data.model.WishlistItem
import com.example.data.repository.StoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class StoreViewModel(
    private val repository: StoreRepository,
    val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ProductCategory.ALL)
    val selectedCategory: StateFlow<ProductCategory> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.FEATURED)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    private val _orderItemsCache = MutableStateFlow<Map<String, List<OrderLineItem>>>(emptyMap())
    val orderItemsCache: StateFlow<Map<String, List<OrderLineItem>>> = _orderItemsCache.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val filteredProducts: StateFlow<List<KutchiProduct>> = combine(
        _searchQuery,
        _selectedCategory,
        _sortOption
    ) { query, category, sort ->
        val trimmed = query.trim().lowercase()
        val matching = KutchiCatalog.products.filter { product ->
            val categoryMatch = category == ProductCategory.ALL || product.category == category
            val queryMatch = trimmed.isEmpty() ||
                product.name.lowercase().contains(trimmed) ||
                product.craftType.lowercase().contains(trimmed) ||
                product.artisanVillage.lowercase().contains(trimmed) ||
                product.subtitle.lowercase().contains(trimmed)
            categoryMatch && queryMatch
        }
        when (sort) {
            SortOption.FEATURED -> matching
            SortOption.PRICE_LOW_HIGH -> matching.sortedBy { it.priceInr }
            SortOption.PRICE_HIGH_LOW -> matching.sortedByDescending { it.priceInr }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = KutchiCatalog.products
    )

    val cartState: StateFlow<UiState<List<CartItem>>> = repository.observeCart(currentUserId)
        .map<List<CartItem>, UiState<List<CartItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing cart", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Unable to load shopping bag"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val wishlistState: StateFlow<UiState<List<WishlistItem>>> = repository.observeWishlist(currentUserId)
        .map<List<WishlistItem>, UiState<List<WishlistItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing wishlist", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Unable to load saved crafts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val addressesState: StateFlow<UiState<List<ShippingAddress>>> = repository.observeAddresses(currentUserId)
        .map<List<ShippingAddress>, UiState<List<ShippingAddress>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing addresses", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Unable to load addresses"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val ordersState: StateFlow<UiState<List<Order>>> = repository.observeOrders(currentUserId)
        .map<List<Order>, UiState<List<Order>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing orders", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Unable to load orders"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ProductCategory) {
        _selectedCategory.value = category
    }

    fun selectSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun addToCart(product: KutchiProduct, variant: String, quantity: Int = 1) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.addToCart(currentUserId, product, variant, quantity)
            _isActionBusy.value = false
            result.fold(
                onSuccess = { _events.tryEmit("Added ${product.name} to your bag") },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Could not add to bag") }
            )
        }
    }

    fun updateCartQuantity(cartItemId: String, newQuantity: Long) {
        viewModelScope.launch {
            val result = repository.updateCartQuantity(currentUserId, cartItemId, newQuantity)
            result.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not update quantity")
            }
        }
    }

    fun removeFromCart(cartItemId: String) {
        viewModelScope.launch {
            val result = repository.removeFromCart(currentUserId, cartItemId)
            result.fold(
                onSuccess = { _events.tryEmit("Removed item from your bag") },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Could not remove item") }
            )
        }
    }

    fun toggleWishlist(product: KutchiProduct, isCurrentlySaved: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleWishlist(currentUserId, product, isCurrentlySaved)
            result.fold(
                onSuccess = { saved ->
                    val message = if (saved) "Saved ${product.name} to wishlist" else "Removed from wishlist"
                    _events.tryEmit(message)
                },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Could not update wishlist") }
            )
        }
    }

    fun saveAddress(
        recipientName: String,
        phone: String,
        streetLine: String,
        city: String,
        state: String,
        postalCode: String,
        label: String,
        preferred: Boolean,
        onSaved: (ShippingAddress) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.saveAddress(
                userId = currentUserId,
                recipientName = recipientName,
                phone = phone,
                streetLine = streetLine,
                city = city,
                state = state,
                postalCode = postalCode,
                label = label,
                preferred = preferred
            )
            _isActionBusy.value = false
            result.fold(
                onSuccess = { address ->
                    _events.tryEmit("Saved delivery address")
                    onSaved(address)
                },
                onFailure = { err ->
                    _events.tryEmit(err.localizedMessage ?: "Please check address fields")
                }
            )
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            val result = repository.deleteAddress(currentUserId, addressId)
            result.fold(
                onSuccess = { _events.tryEmit("Address removed") },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Could not delete address") }
            )
        }
    }

    fun placeOrder(
        cartItems: List<CartItem>,
        address: ShippingAddress,
        paymentMethod: PaymentMethod,
        giftNote: String,
        paymentReference: String = "",
        onOrderPlaced: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.placeOrder(
                userId = currentUserId,
                cartItems = cartItems,
                address = address,
                paymentMethod = paymentMethod,
                giftNote = giftNote,
                paymentReference = paymentReference
            )
            _isActionBusy.value = false
            result.fold(
                onSuccess = { orderNumber ->
                    onOrderPlaced(orderNumber)
                },
                onFailure = { err ->
                    _events.tryEmit(err.localizedMessage ?: "Order could not be placed")
                }
            )
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val result = repository.cancelOrder(currentUserId, orderId)
            _isActionBusy.value = false
            result.fold(
                onSuccess = { _events.tryEmit("Order cancelled") },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Unable to cancel order") }
            )
        }
    }

    fun loadOrderItemsIfNeeded(orderId: String) {
        if (_orderItemsCache.value.containsKey(orderId)) return
        viewModelScope.launch {
            val result = repository.getOrderItems(currentUserId, orderId)
            result.onSuccess { items ->
                _orderItemsCache.value = _orderItemsCache.value + (orderId to items)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Unable to load order items")
            }
        }
    }

    suspend fun deleteAllUserData(): Result<Unit> {
        _isActionBusy.value = true
        val result = repository.deleteAllUserData(currentUserId)
        _isActionBusy.value = false
        return result
    }

    fun showMessage(message: String) {
        _events.tryEmit(message)
    }

    private companion object {
        const val TAG = "StoreViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
