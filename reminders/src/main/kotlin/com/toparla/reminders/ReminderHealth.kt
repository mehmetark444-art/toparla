package com.toparla.reminders

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.toparla.data.db.ReminderStore
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.HealthReport
import com.toparla.domain.reminder.HealthSnapshot
import com.toparla.domain.reminder.Heartbeat
import com.toparla.domain.reminder.HeartbeatDecision
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Telefonun o anki hatırlatma sağlığını okur (blueprint G3 "Hatırlatma Sağlığı kontrolleri"). */
@Singleton
class HealthProbe @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: ReminderStore,
    private val settings: SettingsStore,
) {
    suspend fun report(): HealthReport {
        val notifications = context.getSystemService(NotificationManager::class.java)
        val stats = store.deliveryStats()
        return HealthReport.of(
            HealthSnapshot(
                notifications = notifications.areNotificationsEnabled(),
                exactAlarms = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
                fullScreen = notifications.canUseFullScreenIntent(),
                batteryExempt = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
                dndAccess = notifications.isNotificationPolicyAccessGranted,
                autoStartConfirmed = settings.autoStartConfirmed.first(),
                pendingAlarms = stats.pendingAlarms,
                lastDeliveredAt = stats.lastDeliveredAt,
            ),
        )
    }
}

/**
 * Nabız (blueprint Bölüm I "Heartbeat"): sağlık raporuna bakar; hatırlatmayı kaçırabilecek yeni bir eksik varsa
 * Sistem kanalında sakin tek bir uyarı gösterir, eksikler giderilince uyarıyı kaldırır. Karar [Heartbeat]'tedir.
 */
class HealthWatch(
    private val context: Context,
    private val settings: SettingsStore,
    private val intents: ReminderIntents,
    private val report: suspend () -> HealthReport,
) {
    suspend fun beat(): HeartbeatDecision {
        val warned = settings.heartbeatWarned.first().mapNotNullTo(HashSet()) { name -> HealthCheck.entries.firstOrNull { it.name == name } }
        val decision = Heartbeat.decide(report(), warned)
        val manager = context.getSystemService(NotificationManager::class.java)
        when {
            decision.problems.isEmpty() -> manager.cancel(NOTIFICATION_ID)
            decision.warn -> manager.notify(NOTIFICATION_ID, warning())
        }
        settings.setHeartbeatWarned(decision.problems.mapTo(HashSet()) { it.name })
        Timber.i("Nabız: eksik %s, yeni uyarı %s", decision.problems, decision.warn)
        return decision
    }

    private fun warning(): Notification {
        AndroidReminderNotifier.ensureChannels(context)
        return Notification.Builder(context, AndroidReminderNotifier.CHANNEL_SYSTEM)
            .setSmallIcon(R.drawable.ic_stat_toparla)
            .setContentTitle(context.getString(R.string.heartbeat_title))
            .setContentText(context.getString(R.string.heartbeat_text))
            .setStyle(Notification.BigTextStyle().bigText(context.getString(R.string.heartbeat_text)))
            .setContentIntent(intents.openHealth())
            .setAutoCancel(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 3
    }
}

/** Günde bir nabız. Uygulama açılışında da aynı denetim yapılır (v3 §8.3: "günde ≥ 1 kez ve sabah ilk açılışta"). */
class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ReminderEntryPoint.of(applicationContext).healthWatch().beat()
        Timber.i("Nabız işi koştu")
        return Result.success()
    }
}

/**
 * Bildirim izni, kanal, Rahatsız Etme erişimi ya da Rahatsız Etme kipi değişti (blueprint Bölüm I: yeniden
 * planlama tetikleri "izin/DND değişimi"). Kanallar yenilenir (Rahatsız Etme erişimi sonradan verildiyse kritik
 * kanalın aşma ayarı ancak o zaman yazılabilir), nabız yeniden bakar, pencere yeniden kurulur; bildirimler
 * yeniden açıldıysa kapalıyken gösterilemeyen teslimler geri getirilir.
 */
class HealthChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!context.isUserUnlocked()) return
        val unblocked = intent.action in BLOCK_ACTIONS && !intent.getBooleanExtra(NotificationManager.EXTRA_BLOCKED_STATE, true)
        Timber.i("Bildirim durumu değişti: %s (yeniden açıldı: %s)", intent.action, unblocked)
        runAsync(context) { entry ->
            AndroidReminderNotifier.ensureChannels(context)
            entry.healthWatch().beat()
            val now = entry.clock().now()
            entry.engine().replan(now, rearm = true)
            if (unblocked) entry.engine().reshowOpen(now)
        }
    }

    private companion object {
        val BLOCK_ACTIONS = setOf(
            NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED,
            NotificationManager.ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED,
        )
    }
}
