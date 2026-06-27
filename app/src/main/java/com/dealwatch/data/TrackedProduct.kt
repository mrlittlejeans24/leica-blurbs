package com.dealwatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A product the user wants to watch. [query] is the free-text search the price
 * finder runs on the web. [targetPrice] is an optional ceiling; the lowest-ever
 * price is what actually drives "good deal" alerts.
 */
@Entity(tableName = "tracked_products")
data class TrackedProduct(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val targetPrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastCheckedAt: Long? = null,
    /** Best price seen on the most recent check. */
    val currentPrice: Double? = null,
    val currentStore: String? = null,
    val currentUrl: String? = null,
    /** Lowest price ever recorded since tracking began; drives all-time-low alerts. */
    val lowestEverPrice: Double? = null,
    val currency: String = "$",
    val notificationsEnabled: Boolean = true,
)
