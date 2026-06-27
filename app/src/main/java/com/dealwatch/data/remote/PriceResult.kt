package com.dealwatch.data.remote

/** A single priced offer found on the web for a query. */
data class PriceResult(
    val title: String,
    val price: Double,
    val currency: String,
    val store: String?,
    val url: String?,
)
