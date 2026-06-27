package com.dealwatch.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Reliable [PriceFinder] backed by SerpApi's Google Shopping engine.
 *
 * Requires a (free-tier) API key. The free plan allows ~100 searches/month,
 * which comfortably covers a handful of products checked once a day. Returns
 * clean structured offers (price, store, link) as JSON — no HTML scraping.
 */
class SerpApiPriceFinder(
    private val apiKey: String,
    private val client: OkHttpClient = HttpClients.shared,
) : PriceFinder {

    override suspend fun findPrices(query: String): PriceLookup = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://serpapi.com/search.json?engine=google_shopping" +
            "&q=$encoded&gl=us&hl=en&api_key=$apiKey"
        val request = Request.Builder().url(url).build()

        val body = try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = runCatching { JSONObject(text).optString("error") }.getOrNull()
                    return@withContext PriceLookup(
                        emptyList(),
                        "API error: ${msg?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"}",
                    )
                }
                text
            }
        } catch (t: Throwable) {
            return@withContext PriceLookup(emptyList(), "Network error during check")
        }

        parse(body)
    }

    private fun parse(body: String): PriceLookup {
        val json = runCatching { JSONObject(body) }.getOrNull()
            ?: return PriceLookup(emptyList(), "Could not read API response")

        json.optString("error").takeIf { it.isNotBlank() }?.let {
            return PriceLookup(emptyList(), "API error: $it")
        }

        val array = json.optJSONArray("shopping_results")
            ?: return PriceLookup(emptyList(), "No offers found")

        val results = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val price = item.optDouble("extracted_price", Double.NaN)
                if (price.isNaN() || price <= 0.0) continue
                add(
                    PriceResult(
                        title = item.optString("title").ifBlank { "Offer" },
                        price = price,
                        currency = currencyFrom(item.optString("price")),
                        store = item.optString("source").ifBlank { null },
                        url = item.optString("product_link")
                            .ifBlank { item.optString("link") }
                            .ifBlank { null },
                    ),
                )
            }
        }.sortedBy { it.price }

        return if (results.isEmpty()) {
            PriceLookup(results, "No offers found")
        } else {
            PriceLookup(results, "Found ${results.size} offers")
        }
    }

    private fun currencyFrom(display: String): String = when {
        display.contains('£') -> "£"
        display.contains('€') -> "€"
        display.contains('¥') -> "¥"
        else -> "$"
    }
}
