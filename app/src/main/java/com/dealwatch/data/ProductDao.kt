package com.dealwatch.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM tracked_products ORDER BY createdAt DESC")
    fun observeProducts(): Flow<List<TrackedProduct>>

    @Query("SELECT * FROM tracked_products WHERE id = :id")
    fun observeProduct(id: Long): Flow<TrackedProduct?>

    @Query("SELECT * FROM tracked_products")
    suspend fun getAllProducts(): List<TrackedProduct>

    @Query("SELECT * FROM tracked_products WHERE id = :id")
    suspend fun getProduct(id: Long): TrackedProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: TrackedProduct): Long

    @Update
    suspend fun update(product: TrackedProduct)

    @Query("DELETE FROM tracked_products WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insertPricePoint(point: PricePoint)

    @Query("SELECT * FROM price_points WHERE productId = :productId ORDER BY timestamp ASC")
    fun observePriceHistory(productId: Long): Flow<List<PricePoint>>
}
