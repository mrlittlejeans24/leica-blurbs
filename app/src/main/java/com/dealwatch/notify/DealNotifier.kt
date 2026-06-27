package com.dealwatch.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dealwatch.MainActivity
import com.dealwatch.R
import com.dealwatch.data.DealAlert
import java.text.NumberFormat

object DealNotifier {
    const val CHANNEL_ID = "deal_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.deal_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.deal_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    // Permission is verified by hasPermission() right below before any notify() call.
    @SuppressLint("MissingPermission")
    fun notifyDeal(context: Context, alert: DealAlert) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val money = NumberFormat.getNumberInstance()
        val cur = alert.product.currency
        val title = "Price drop: ${alert.product.query}"
        val storeSuffix = alert.store?.let { " at $it" } ?: ""
        val text = "$cur${money.format(alert.newPrice)}$storeSuffix — new low " +
            "(was $cur${money.format(alert.previousLowest)})"

        val intent = buildTapIntent(context, alert.url)
        val pending = PendingIntent.getActivity(
            context,
            alert.product.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context)
            .notify(alert.product.id.toInt(), notification)
    }

    private fun buildTapIntent(context: Context, url: String?): Intent =
        if (!url.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } else {
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}
