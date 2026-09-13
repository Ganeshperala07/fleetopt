package com.fleetopt.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fleetopt.app.MainActivity

class StockoutNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "cng_stockout_alerts"
        const val CHANNEL_NAME = "CNG Critical Stockout Alerts"
        const val NOTIFICATION_ID_BASE = 2000
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority warnings for stations reaching critical cascade pressure (<60 bar) or near dryout"
                enableLights(true)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun postStockoutAlert(stationId: String, stationName: String, pressureBar: Double, ttdHours: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "recommendation")
            putExtra("target_station_id", stationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            stationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("⚠️ Critical CNG Stockout Risk: $stationName")
            .setContentText("Pressure dropped to ${"%.1f".format(pressureBar)} bar (~${"%.1f".format(ttdHours)}h cover left). Dispatch HCV immediately.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Urgent replenishment needed at $stationName. Cascade pressure is at ${"%.1f".format(pressureBar)} bar (cutoff is 50 bar). Estimated Time-To-Dryout is ${"%.1f".format(ttdHours)} hours.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                "⚡ Dispatch HCV",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + stationId.hashCode() % 500,
                notification
            )
        } catch (_: SecurityException) {
            // Permission not yet granted on Android 13+
        }
    }
}
