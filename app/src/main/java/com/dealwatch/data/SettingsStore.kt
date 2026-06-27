package com.dealwatch.data

import android.content.Context

/**
 * Tiny wrapper over SharedPreferences for app settings. Currently just the
 * optional SerpApi key that upgrades price checks from best-effort scraping to
 * reliable structured results.
 */
object SettingsStore {
    private const val PREFS = "dealwatch_settings"
    private const val KEY_SERPAPI = "serpapi_key"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSerpApiKey(context: Context): String? =
        prefs(context).getString(KEY_SERPAPI, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun setSerpApiKey(context: Context, key: String?) {
        prefs(context).edit().putString(KEY_SERPAPI, key?.trim().orEmpty()).apply()
    }
}
