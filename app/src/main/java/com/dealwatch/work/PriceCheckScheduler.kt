package com.dealwatch.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PriceCheckScheduler {
    private const val DAILY_WORK = "dealwatch_daily_price_check"
    private const val ONE_SHOT_WORK = "dealwatch_manual_price_check"

    /** Idempotent: safe to call on every app start. Keeps the existing schedule. */
    fun scheduleDaily(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 24h period; WorkManager batches within a flex window to save battery.
        val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Triggers an immediate check (used by the "Check now" button). */
    fun checkNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<PriceCheckWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_SHOT_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
