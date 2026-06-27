package com.dealwatch.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Default [PriceFinder]: fetches Google Shopping search HTML with a desktop
 * User-Agent and extracts priced offers with Jsoup. No API key required.
 *
 * This is deliberately defensive rather than tied to one set of CSS classes
 * (which search providers rotate constantly): it scans for price-shaped tokens
 * and pairs each with the nearest container that has a title and a link. It
 * won't be perfect, but it degrades to "fewer/no results" instead of crashing.
 */
class WebScrapePriceFinder(
    private val client: OkHttpClient = defaultClient(),
) : PriceFinder {

    override suspend fun findPrices(query: String): List<PriceResult> = withContext(Dispatchers.IO) {
        val html = runCatching { fetch(query) }.getOrNull() ?: return@withContext emptyList()
        parse(html)
    }

    private fun fetch(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        // tbm=shop -> shopping results; hl/gl pin language & region for stable parsing.
        val url = "https://www.google.com/search?tbm=shop&hl=en&gl=us&num=20&q=$encoded"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private fun parse(html: String): List<PriceResult> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<PriceResult>()
        val seen = HashSet<String>()

        // Any element whose own text contains a price token is a candidate anchor.
        for (el in doc.allElements) {
            val ownText = el.ownText()
            val match = PRICE_REGEX.find(ownText) ?: continue
            val price = parsePrice(match.value) ?: continue
            if (price <= 0.0 || price > MAX_REASONABLE_PRICE) continue

            val container = nearestResultContainer(el)
            val title = extractTitle(container, el)
            val link = extractLink(container)
            val store = link?.let { hostOf(it) }

            // Dedup on (price, title) so the same offer rendered twice is counted once.
            val key = "$price|${title.lowercase().take(40)}"
            if (!seen.add(key)) continue

            results += PriceResult(
                title = title,
                price = price,
                currency = currencyOf(match.value),
                store = store,
                url = link,
            )
        }
        return results.sortedBy { it.price }
    }

    /** Walk up a few levels to a container that holds both a link and a title. */
    private fun nearestResultContainer(el: Element): Element {
        var node: Element? = el
        repeat(4) {
            val parent = node?.parent() ?: return node ?: el
            if (parent.selectFirst("a[href]") != null && parent.text().length in 8..400) {
                return parent
            }
            node = parent
        }
        return node ?: el
    }

    private fun extractTitle(container: Element, priceEl: Element): String {
        // Prefer the longest anchor/heading text that isn't itself the price.
        val candidates = container.select("a[href], h3, h4, [role=heading]")
            .map { it.text().trim() }
            .filter { it.length in 6..160 && PRICE_REGEX.find(it) == null }
        val best = candidates.maxByOrNull { it.length }
        if (best != null) return best
        // Fall back to the container text with the price stripped out.
        return container.text().replace(PRICE_REGEX, "").trim().take(120).ifBlank { "Offer" }
    }

    private fun extractLink(container: Element): String? {
        val href = container.selectFirst("a[href]")?.attr("href") ?: return null
        return normalizeGoogleLink(href)
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val MAX_REASONABLE_PRICE = 1_000_000.0

        // Matches $1,299.99 / £49 / €1.234,56 etc. Currency symbol then digits/grouping.
        private val PRICE_REGEX =
            Regex("""[$£€¥]\s?\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?""")

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

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

        internal fun currencyOf(raw: String): String = when {
            raw.contains('£') -> "£"
            raw.contains('€') -> "€"
            raw.contains('¥') -> "¥"
            else -> "$"
        }

        internal fun hostOf(url: String): String? = runCatching {
            java.net.URI(url).host?.removePrefix("www.")
        }.getOrNull()

        /** Google wraps outbound links as /url?q=<real>&...; unwrap when present. */
        internal fun normalizeGoogleLink(href: String): String {
            val abs = when {
                href.startsWith("http") -> href
                href.startsWith("/url?") -> "https://www.google.com$href"
                href.startsWith("/") -> "https://www.google.com$href"
                else -> href
            }
            val q = Regex("""[?&]q=([^&]+)""").find(abs)?.groupValues?.getOrNull(1)
            return if (q != null) {
                runCatching { java.net.URLDecoder.decode(q, "UTF-8") }.getOrDefault(abs)
            } else {
                abs.substring(0, min(abs.length, 2000))
            }
        }
    }
}
