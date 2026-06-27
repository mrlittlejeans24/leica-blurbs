@file:OptIn(ExperimentalMaterial3Api::class)

package com.dealwatch.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dealwatch.data.PricePoint
import com.dealwatch.viewmodel.ProductViewModel

@Composable
fun ProductDetailScreen(
    vm: ProductViewModel,
    productId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val product by vm.product(productId).collectAsStateWithLifecycle(initialValue = null)
    val history by vm.priceHistory(productId).collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val p = product

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(p?.query ?: "Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        vm.deleteProduct(productId)
                        onDeleted()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        if (p == null) return@Scaffold
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    StatRow("Best price now", formatPrice(p.currency, p.currentPrice), p.currentStore)
                    StatRow("Lowest ever", formatPrice(p.currency, p.lowestEverPrice), null)
                    p.targetPrice?.let {
                        StatRow("Your target", formatPrice(p.currency, it), null)
                    }
                    Text(
                        "Last checked ${relativeTime(p.lastCheckedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (!p.currentUrl.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.currentUrl)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open best offer" + (p.currentStore?.let { " at $it" } ?: ""))
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Deal alerts", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = p.notificationsEnabled,
                    onCheckedChange = { vm.setNotifications(p, it) },
                )
            }

            Text("Price history", style = MaterialTheme.typography.titleMedium)
            if (history.size < 2) {
                Text(
                    "Not enough data yet — the chart fills in as daily checks run.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Sparkline(
                    history = history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, subtitle: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Sparkline(history: List<PricePoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val prices = history.map { it.price }
    val minPrice = prices.min()
    val maxPrice = prices.max()
    val range = (maxPrice - minPrice).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stepX = if (history.size > 1) w / (history.size - 1) else w
        val path = Path()
        history.forEachIndexed { index, point ->
            val x = stepX * index
            // Invert Y so lower prices sit lower on screen.
            val y = h - ((point.price - minPrice) / range * h).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
        )
        // Mark the most recent point.
        val lastX = stepX * (history.size - 1)
        val lastY = h - ((history.last().price - minPrice) / range * h).toFloat()
        drawCircle(color = lineColor, radius = 7f, center = Offset(lastX, lastY))
    }
}
