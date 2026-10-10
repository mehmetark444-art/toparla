package com.toparla.reminders

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Duration

/**
 * Kritik teslimin sahibi (blueprint G2): alarm alıcısından başlatılır, teslimi yapar ve hemen durur.
 * Ağır iş yapmaz; yalnız motorun teslim adımını süreç öldürülmeden bitirmek için öne alınır.
 */
class ReminderService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val key = intent?.getStringExtra(AlarmManagerScheduler.EXTRA_KEY)
        AndroidReminderNotifier.ensureChannels(this)
        val notification = Notification.Builder(this, AndroidReminderNotifier.CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_toparla)
            .setContentTitle(getString(R.string.delivering_title))
            .build()
        try {
            startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } catch (e: IllegalStateException) {
            Timber.w(e, "Teslim servisi öne alınamadı; teslim yine de yapılıyor")
        }
        val entry = ReminderEntryPoint.of(this)
        entry.scope().launch {
            try {
                if (key != null) {
                    deliverSafely(this@ReminderService, key, critical = true) {
                        val now = entry.clock().now()
                        entry.engine().onAlarmFired(key, now)
                        // Yaş alıcıda ölçülür (servis başlayana dek geçen süre karışmasın); yoksa kanıt sayılmaz.
                        val age = Duration.ofMillis(intent.getLongExtra(EXTRA_PROCESS_AGE_MS, UNKNOWN_AGE_MS))
                        WakeLog.record(key, age, now, entry.repository()::log)
                    }
                }
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        const val EXTRA_PROCESS_AGE_MS = "processAgeMs"
        private const val UNKNOWN_AGE_MS = -1L
        private const val FOREGROUND_ID = 2
    }
}

/**
 * Ekranı olmayan durumlar için (kilitli açılış): uygulamanın açılış ekranına giden niyetler.
 * `:app` kendi ekranlarını bilen uygulamayı sağlar.
 */
class LauncherIntents(private val context: Context) : ReminderIntents {
    override fun openApp(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun fullScreen(occurrenceKey: String): PendingIntent = openApp()

    override fun openHealth(): PendingIntent = openApp()
}
