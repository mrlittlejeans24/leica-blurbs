package com.dealwatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One observed price for a product at a point in time — builds the price history. */
@Entity(
    tableName = "price_points",
    foreignKeys = [
        ForeignKey(
            entity = TrackedProduct::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("productId")],
)
data class PricePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val price: Double,
    val store: String?,
    val url: String?,
    val timestamp: Long = System.currentTimeMillis(),
)
