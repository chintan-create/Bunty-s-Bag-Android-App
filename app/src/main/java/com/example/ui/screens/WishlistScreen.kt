package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.catalog.KutchiCatalog
import com.example.data.model.KutchiProduct
import com.example.data.model.WishlistItem
import com.example.ui.components.ArtisanOriginBadge
import com.example.ui.components.EmptyBazaarState
import com.example.ui.components.KutchiDiamondBorder
import com.example.ui.components.ProductArtwork
import com.example.ui.state.UiState

@Composable
fun WishlistScreen(
    wishlistState: UiState<List<WishlistItem>>,
    onProductClick: (KutchiProduct) -> Unit,
    onMoveToBag: (KutchiProduct) -> Unit,
    onRemoveFromWishlist: (KutchiProduct) -> Unit,
    onBrowseShop: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "Saved Crafts",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your handpicked Kutchi treasures",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            KutchiDiamondBorder()
        }

        when (wishlistState) {
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
                    title = "Couldn't load saved crafts",
                    subtitle = wishlistState.message,
                    icon = Icons.Outlined.FavoriteBorder,
                    actionLabel = "Explore Bazaar",
                    onAction = onBrowseShop
                )
            }
            is UiState.Success -> {
                val items = wishlistState.data
                if (items.isEmpty()) {
                    EmptyBazaarState(
                        title = "No saved treasures yet",
                        subtitle = "Tap the heart on any handmade bag, potli, or stole to save it here for later.",
                        icon = Icons.Outlined.FavoriteBorder,
                        actionLabel = "Browse Kutchi Crafts",
                        onAction = onBrowseShop
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = contentPadding.calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items, key = { it.productId }) { savedItem ->
                            val catalogProduct = KutchiCatalog.findById(savedItem.productId)
                            WishlistCard(
                                savedItem = savedItem,
                                catalogProduct = catalogProduct,
                                onCardClick = { if (catalogProduct != null) onProductClick(catalogProduct) },
                                onMoveToBag = { if (catalogProduct != null) onMoveToBag(catalogProduct) },
                                onRemove = { if (catalogProduct != null) onRemoveFromWishlist(catalogProduct) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistCard(
    savedItem: WishlistItem,
    catalogProduct: KutchiProduct?,
    onCardClick: () -> Unit,
    onMoveToBag: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = catalogProduct != null, onClick = onCardClick)
            .testTag("wishlist_card_${savedItem.productId}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            ProductArtwork(
                drawableKey = savedItem.drawableKey,
                contentDescription = savedItem.productName,
                modifier = Modifier.size(94.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (catalogProduct != null) {
                    ArtisanOriginBadge(village = catalogProduct.artisanVillage)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = savedItem.productName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = KutchiCatalog.formatInr(savedItem.unitPriceInr),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilledTonalButton(
                        onClick = onMoveToBag,
                        enabled = catalogProduct != null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("move_to_bag_${savedItem.productId}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add to Bag", style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(
                        onClick = onRemove,
                        enabled = catalogProduct != null,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("remove_wishlist_${savedItem.productId}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove ${savedItem.productName} from saved",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
