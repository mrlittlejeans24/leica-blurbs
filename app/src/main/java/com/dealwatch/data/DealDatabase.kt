package com.dealwatch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackedProduct::class, PricePoint::class],
    version = 1,
    exportSchema = false,
)
abstract class DealDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var instance: DealDatabase? = null

        fun get(context: Context): DealDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DealDatabase::class.java,
                    "dealwatch.db",
                ).build().also { instance = it }
            }
    }
}
