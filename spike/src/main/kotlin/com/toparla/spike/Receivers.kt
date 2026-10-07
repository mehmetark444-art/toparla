package com.toparla.spike

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(AlarmSpike.EXTRA_KEY) ?: return
        val plannedAt = AlarmSpike.plannedAt(key)
        val delta = System.currentTimeMillis() - plannedAt
        AlarmSpike.log(context, "FIRED", key, plannedAt)
        AlarmSpike.resolve(context, key)

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW),
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_body, key.substringBefore('-'), delta))
            .setCategory(Notification.CATEGORY_ALARM)
            .build()
        nm.notify(key.hashCode(), notification)
        AlarmSpike.log(context, "POSTED", key, plannedAt, "notifEnabled=${nm.areNotificationsEnabled()}")
    }

    private companion object {
        // Gece testi uyandırmasın diye sessiz kanal; ölçüm kayıttan okunur.
        const val CHANNEL = "spike_alarm_silent"
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmSpike.rescheduleAll(context, intent.action?.substringAfterLast('.') ?: "UNKNOWN")
    }
}
