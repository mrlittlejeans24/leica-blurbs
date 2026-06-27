package com.dealwatch.data.remote

import android.content.Context
import com.dealwatch.data.SettingsStore

/**
 * Picks the price backend per check: the reliable [SerpApiPriceFinder] when the
 * user has configured an API key, otherwise the keyless [WebScrapePriceFinder].
 * Resolved each call so toggling the key in Settings takes effect immediately.
 */
class RoutingPriceFinder(private val appContext: Context) : PriceFinder {
    override suspend fun findPrices(query: String): PriceLookup {
        val key = SettingsStore.getSerpApiKey(appContext)
        val finder: PriceFinder = if (key != null) {
            SerpApiPriceFinder(key)
        } else {
            WebScrapePriceFinder()
        }
        return finder.findPrices(query)
    }
}
