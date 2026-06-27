package com.dealwatch.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Keyless, best-effort [PriceFinder]. It:
 *  1. searches DuckDuckGo's HTML endpoint (no API key, no JS required),
 *  2. takes the top retailer result links,
 *  3. fetches each page and reads the price from structured metadata
 *     (schema.org JSON-LD `offers.price`, or Open Graph / itemprop price tags).
 *
 * Reading the price from a page's own structured data is far more accurate than
 * scraping search snippets. It's still best-effort: some retailers block bots or
 * omit structured data, in which case fewer/no offers come back. For reliable
 * results, set a SerpApi key in Settings (see [SerpApiPriceFinder]).
 */
class WebScrapePriceFinder(
    private val client: OkHttpClient = HttpClients.shared,
) : PriceFinder {

    override suspend fun findPrices(query: String): PriceLookup = withContext(Dispatchers.IO) {
        val searchHtml = runCatching { searchDuckDuckGo(query) }.getOrNull()
            ?: return@withContext PriceLookup(emptyList(), "Network error during search")

        val links = extractResultLinks(searchHtml)
        if (links.isEmpty()) {
            // DDG occasionally returns a bot-challenge page instead of results.
            return@withContext PriceLookup(
                emptyList(),
                "Search blocked or no results — add a free API key in Settings for reliable prices",
            )
        }

        // Fetch the top few product pages concurrently and pull a structured price.
        val offers = coroutineScope {
            links.take(MAX_PAGES).map { link ->
                async { runCatching { offerFromPage(link) }.getOrNull() }
            }.awaitAll()
        }.filterNotNull().sortedBy { it.price }

        if (offers.isEmpty()) {
            PriceLookup(
                emptyList(),
                "Found pages but no readable prices — add a free API key in Settings for reliable prices",
            )
        } else {
            PriceLookup(offers, "Found ${offers.size} offers")
        }
    }

    private fun searchDuckDuckGo(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://html.duckduckgo.com/html/?q=$encoded")
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    /** Pull and decode the real destination URLs from DDG's result anchors. */
    private fun extractResultLinks(html: String): List<String> {
        val doc = Jsoup.parse(html)
        val out = LinkedHashSet<String>()
        for (a in doc.select("a.result__a[href]")) {
            val href = a.attr("href")
            val real = decodeDuckLink(href) ?: continue
            val host = hostOf(real) ?: continue
            // One result per host so we compare across distinct stores.
            if (out.none { hostOf(it) == host }) out.add(real)
        }
        return out.toList()
    }

    private fun offerFromPage(url: String): PriceResult? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string().orEmpty()
        }
        val doc = Jsoup.parse(html)
        val price = priceFromJsonLd(doc) ?: priceFromMeta(doc) ?: return null
        if (price <= 0.0 || price > MAX_REASONABLE_PRICE) return null

        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?.ifBlank { null } ?: doc.title().ifBlank { "Offer" }
        return PriceResult(
            title = title.take(140),
            price = price,
            currency = "$",
            store = hostOf(url),
            url = url,
        )
    }

    /** schema.org Product/Offer embedded as JSON-LD — the most reliable signal. */
    private fun priceFromJsonLd(doc: org.jsoup.nodes.Document): Double? {
        for (script in doc.select("script[type=application/ld+json]")) {
            val raw = script.data().trim()
            if (raw.isEmpty()) continue
            val price = runCatching { lowestPriceInJson(raw) }.getOrNull()
            if (price != null) return price
        }
        return null
    }

    private fun lowestPriceInJson(raw: String): Double? {
        val found = mutableListOf<Double>()
        fun walk(value: Any?) {
            when (value) {
                is JSONObject -> {
                    for (key in value.keys()) {
                        if (key == "price" || key == "lowPrice") {
                            asPrice(value.opt(key))?.let { found.add(it) }
                        }
                        walk(value.opt(key))
                    }
                }
                is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i))
            }
        }
        // A document can hold a single object or an array of them.
        val trimmed = raw.trimStart()
        if (trimmed.startsWith("[")) walk(JSONArray(raw)) else walk(JSONObject(raw))
        return found.filter { it > 0.0 }.minOrNull()
    }

    private fun priceFromMeta(doc: org.jsoup.nodes.Document): Double? {
        val selectors = listOf(
            "meta[property=product:price:amount]",
            "meta[property=og:price:amount]",
            "meta[itemprop=price]",
        )
        for (sel in selectors) {
            val content = doc.selectFirst(sel)?.attr("content")
            asPrice(content)?.let { return it }
        }
        return null
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val MAX_REASONABLE_PRICE = 1_000_000.0
        private const val MAX_PAGES = 6

        internal fun asPrice(value: Any?): Double? {
            val s = value?.toString()?.trim() ?: return null
            return parsePrice(s)
        }

        internal fun parsePrice(raw: String): Double? {
            val digits = raw.replace(Regex("""[^\d.,]"""), "").trim()
            if (digits.isEmpty()) return null
            val lastComma = digits.lastIndexOf(',')
            val lastDot = digits.lastIndexOf('.')
            // Normalize grouping/decimal separators to a plain "1234.56".
            val normalized = when {
                lastComma == -1 && lastDot == -1 -> digits
                // Both present: the rightmost separator is the decimal point.
                lastComma != -1 && lastDot != -1 ->
                    if (lastComma > lastDot) {
                        digits.replace(".", "").replace(',', '.') // EU: 1.234,56
                    } else {
                        digits.replace(",", "") // US: 1,234.56
                    }
                // Only one kind present: 3 trailing digits => grouping, else decimal.
                else -> {
                    val sep = if (lastComma != -1) ',' else '.'
                    val trailing = digits.length - digits.lastIndexOf(sep) - 1
                    if (trailing == 3) {
                        digits.replace(sep.toString(), "") // 1,234 / 1.234 => thousands
                    } else {
                        digits.replace(sep, '.') // 1,50 / 12.99 => decimal
                    }
                }
            }
            return normalized.toDoubleOrNull()
        }

        internal fun hostOf(url: String): String? = runCatching {
            java.net.URI(url).host?.removePrefix("www.")
        }.getOrNull()

        /** DDG wraps links as //duckduckgo.com/l/?uddg=<encoded real url>. */
        internal fun decodeDuckLink(href: String): String? {
            val q = Regex("""[?&]uddg=([^&]+)""").find(href)?.groupValues?.getOrNull(1)
            val decoded = if (q != null) {
                runCatching { URLDecoder.decode(q, "UTF-8") }.getOrNull()
            } else {
                href.takeIf { it.startsWith("http") }
            } ?: return null
            // Skip ad/redirect domains that aren't real product pages.
            if (decoded.contains("duckduckgo.com")) return null
            return decoded
        }
    }
}
