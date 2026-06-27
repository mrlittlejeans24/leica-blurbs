package com.dealwatch.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dealwatch.data.ProductRepository
import com.dealwatch.notify.DealNotifier

/**
 * Runs on WorkManager's schedule (roughly daily, when network is available) and
 * also on demand from the "Check now" action. Checks every tracked product and
 * fires a notification for any new all-time low.
 */
class PriceCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = ProductRepository.get(applicationContext)
        return try {
            val alerts = repo.checkAllProducts()
            alerts.forEach { DealNotifier.notifyDeal(applicationContext, it) }
            Result.success()
        } catch (t: Throwable) {
            // Transient failure (e.g. no connectivity mid-run) — let WorkManager retry.
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
    }
}
