package com.dealwatch.ui

import java.text.NumberFormat
import java.util.concurrent.TimeUnit

internal fun formatPrice(currency: String, value: Double?): String =
    if (value == null) "—" else "$currency${NumberFormat.getNumberInstance().format(value)}"

internal fun relativeTime(timestamp: Long?): String {
    if (timestamp == null) return "not checked yet"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 30 -> "${days}d ago"
        else -> "a while ago"
    }
}
