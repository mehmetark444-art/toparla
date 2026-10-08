package com.toparla.spike

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(AlarmSpike.EXTRA_KEY) ?: return
        val plannedAt = AlarmSpike.plannedAt(key)
        AlarmSpike.log(context, "FIRED", key, plannedAt)
        AlarmSpike.resolve(context, key)

        when (key.substringBefore('-')) {
            AlarmSpike.API_FGS, AlarmSpike.API_FGS_IDLE -> startService(context, key, plannedAt)
            AlarmSpike.API_CRIT -> AGroup.alarmTest(context)
            AlarmSpike.API_FSI -> postFullScreen(context, key, plannedAt)
            else -> postPlain(context, key, plannedAt)
        }
    }

    /** Spike 2: kesin alarm tetikleyicisinden foreground service başlatılabiliyor mu? */
    private fun startService(context: Context, key: String, plannedAt: Long) {
        try {
            context.startForegroundService(Intent(context, SpikeService::class.java).putExtra(AlarmSpike.EXTRA_KEY, key))
            AlarmSpike.log(context, "FGS_REQUESTED", key, plannedAt)
        } catch (e: RuntimeException) {
            AlarmSpike.log(context, "FGS_START_EXCEPTION", key, plannedAt, e.javaClass.simpleName)
        }
    }

    /** Spike 3: tam ekran bildirim kilit ekranının üstünde açılıyor mu? */
    private fun postFullScreen(context: Context, key: String, plannedAt: Long) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_FSI, context.getString(R.string.channel_fsi), NotificationManager.IMPORTANCE_HIGH),
        )
        val full = PendingIntent.getActivity(
            context,
            key.hashCode(),
            Intent(context, FullScreenActivity::class.java).putExtra(AlarmSpike.EXTRA_KEY, key),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_FSI)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.fsi_title))
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(full, true)
            .build()
        nm.notify(key.hashCode(), notification)
        AlarmSpike.log(context, "FSI_POSTED", key, plannedAt, "canUseFsi=${nm.canUseFullScreenIntent()}")
    }

    private fun postPlain(context: Context, key: String, plannedAt: Long) {
        val delta = System.currentTimeMillis() - plannedAt
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
        const val CHANNEL_FSI = "spike_fsi"
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmSpike.rescheduleAll(context, intent.action?.substringAfterLast('.') ?: "UNKNOWN")
    }
}
