package com.dealwatch.data.remote

/**
 * Strategy for turning a product query into a list of priced offers.
 *
 * The shipped implementation ([WebScrapePriceFinder]) scrapes public search
 * results with no API key. Scraping is inherently fragile — if a provider
 * changes its markup, results may dry up. Because everything is behind this
 * interface, you can swap in a paid, reliable price API (e.g. SerpApi, a
 * retailer affiliate feed) by writing another implementation and wiring it in
 * [com.dealwatch.data.ProductRepository] — nothing else has to change.
 */
interface PriceFinder {
    /** Returns offers for [query], best-effort. Never throws for "no results"; returns empty. */
    suspend fun findPrices(query: String): List<PriceResult>
}
