@file:OptIn(ExperimentalMaterial3Api::class)

package com.dealwatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dealwatch.data.TrackedProduct

@Composable
fun ProductListScreen(
    products: List<TrackedProduct>,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onCheckNow: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DealWatch") },
                actions = {
                    IconButton(onClick = onCheckNow) {
                        Icon(Icons.Default.Refresh, contentDescription = "Check prices now")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Track product") },
            )
        },
    ) { padding ->
        if (products.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                    start = 12.dp,
                    end = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(products, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onOpen(product.id) })
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: TrackedProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = product.query,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Best now: ${formatPrice(product.currency, product.currentPrice)}" +
                    (product.currentStore?.let { "  ·  $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Lowest ever: ${formatPrice(product.currency, product.lowestEverPrice)}" +
                    "  ·  checked ${relativeTime(product.lastCheckedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No products tracked yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Tap “Track product” to add something you want.\n" +
                    "DealWatch checks the price daily and pings you on a new low.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}
