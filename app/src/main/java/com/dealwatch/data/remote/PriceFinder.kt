package com.dealwatch.data.remote

/** Outcome of a price lookup: the offers found plus a human-readable status. */
data class PriceLookup(
    val results: List<PriceResult>,
    /** Short message shown to the user (e.g. "Found 7 offers", "Search blocked"). */
    val status: String,
)

/**
 * Strategy for turning a product query into priced offers.
 *
 * Two implementations ship:
 *  - [SerpApiPriceFinder] — reliable, uses a free SerpApi key (Google Shopping).
 *  - [WebScrapePriceFinder] — keyless best-effort scraping; works with no setup
 *    but can be blocked or incomplete because the good price sources block bots.
 *
 * [RoutingPriceFinder] picks between them based on whether a key is configured.
 * To add another backend (a retailer feed, a different API), implement this
 * interface and wire it into [RoutingPriceFinder] — nothing else changes.
 */
interface PriceFinder {
    /** Returns offers for [query], best-effort. Should not throw; report problems via status. */
    suspend fun findPrices(query: String): PriceLookup
}
