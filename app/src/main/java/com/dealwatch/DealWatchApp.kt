package com.dealwatch

import android.app.Application
import com.dealwatch.notify.DealNotifier
import com.dealwatch.work.PriceCheckScheduler

class DealWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DealNotifier.ensureChannel(this)
        PriceCheckScheduler.scheduleDaily(this)
    }
}
