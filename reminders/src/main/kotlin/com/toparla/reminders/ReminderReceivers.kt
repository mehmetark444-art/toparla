package com.toparla.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.toparla.data.core.SystemClock
import com.toparla.domain.core.Clock
import com.toparla.domain.reminder.ReminderAction
import com.toparla.domain.reminder.ReminderEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** Alıcılar ve servis, uygulamanın tek motoruna ve arka plan kapsamına buradan ulaşır. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun engine(): ReminderEngine

    fun clock(): Clock

    @ReminderScope
    fun scope(): CoroutineScope

    fun scheduler(): AlarmManagerScheduler

    fun healthWatch(): HealthWatch

    companion object {
        fun of(context: Context): ReminderEntryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, ReminderEntryPoint::class.java)
    }
}

internal fun Context.isUserUnlocked(): Boolean = getSystemService(UserManager::class.java).isUserUnlocked

/** Alıcının 10 saniyelik süresi içinde işi arka plan kapsamında bitirir. */
internal fun BroadcastReceiver.runAsync(context: Context, block: suspend (ReminderEntryPoint) -> Unit) {
    val pending = goAsync()
    val entry = ReminderEntryPoint.of(context)
    entry.scope().launch {
        try {
            block(entry)
        } finally {
            pending.finish()
        }
    }
}

/**
 * Sistem alarmı ateşlendi. Kritik teslim foreground service'e devredilir (blueprint G1, F1 ölçümü: 29–35 ms);
 * servis başlatılamazsa ve diğer sınıflarda iş alıcının kendi süresinde yapılır.
 * Kilit açılmadan çalarsa veritabanına erişilemez: bildirim cihaz korumalı kopyadaki başlıkla gösterilir
 * ([FallbackNotice]), ayrıntı kilit açılınca gelir. Kayıt yazılamazsa da aynı yola düşülür ([deliverSafely]).
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(AlarmManagerScheduler.EXTRA_KEY) ?: return
        val critical = intent.getBooleanExtra(AlarmManagerScheduler.EXTRA_CRITICAL, false)
        if (!context.isUserUnlocked()) {
            if (key != AlarmManagerScheduler.MAINTENANCE_KEY) {
                FallbackNotice.show(context, key, critical, R.string.locked_boot_text)
                BootMirror(context).markAwaitingDetail(key)
            }
            return
        }
        if (critical && startService(context, key)) return
        runAsync(context) { entry ->
            deliverSafely(context, key, critical) {
                if (key == AlarmManagerScheduler.MAINTENANCE_KEY) {
                    entry.engine().replan(entry.clock().now())
                } else {
                    entry.engine().onAlarmFired(key, entry.clock().now())
                }
            }
        }
    }

    private fun startService(context: Context, key: String): Boolean = try {
        context.startForegroundService(Intent(context, ReminderService::class.java).putExtra(AlarmManagerScheduler.EXTRA_KEY, key))
        true
    } catch (e: IllegalStateException) {
        // ForegroundServiceStartNotAllowedException dahil: bildirim yoluna düşülür (blueprint G2).
        Timber.w(e, "Teslim servisi başlatılamadı; alıcıda devam ediliyor")
        false
    }
}

/** Bildirim düğmeleri: uygulama açılmadan çalışır (blueprint G1). */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_OCCURRENCE_KEY) ?: return
        val name = intent.action?.removePrefix(ACTION_PREFIX) ?: return
        val action = ReminderAction.entries.firstOrNull { it.name == name } ?: return
        runAsync(context) { entry -> entry.engine().onAction(key, action, entry.clock().now()) }
    }

    companion object {
        const val ACTION_PREFIX = "com.toparla.reminders.ACTION_"
        const val EXTRA_OCCURRENCE_KEY = "occurrenceKey"
    }
}

/**
 * Sistem alarmlarının silinmiş ya da kaymış olabileceği anlar: açılış, paket güncelleme, saat ve saat dilimi
 * değişimi (F1 ölçümü: `TIME_SET` 4/4 geldi). Kilit açılmadan gelen açılışta veritabanı kapalıdır; alarmlar
 * cihaz korumalı kopyadan kurulur, tam planlama kilit açılınca yapılır.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!context.isUserUnlocked()) {
            // Hilt grafiği veritabanına uzanır; kilitliyken yalnız kopyadan kurulum yapılır.
            val overdue = AlarmManagerScheduler(context, LauncherIntents(context)).rearmFromMirror(SystemClock().now().toEpochMilli())
            Timber.i("Kilitli açılış: alarmlar kopyadan kuruldu; vakti geçmiş %d", overdue.size)
            return
        }
        Timber.i("Yeniden planlama tetiklendi: %s", intent.action)
        runAsync(context) { entry -> ReminderStartup.recover(context, entry) }
    }
}

/** Kilit açıldıktan sonraki toparlanma: açılış yayını ve uygulama açılışı aynı yolu kullanır. */
object ReminderStartup {
    /**
     * Önce veritabanına ulaşılamadan çalmış teslimler ayrıntısıyla gösterilir (gecikme toleransı beklenmez),
     * sonra pencere yeniden doldurulur ve sistem alarmları yeniden kurulur. Son adım nabızdır.
     */
    suspend fun recover(context: Context, entry: ReminderEntryPoint) {
        val now = entry.clock().now()
        BootMirror(context).takeAwaitingDetail().forEach { entry.engine().onAlarmFired(it, now) }
        entry.engine().replan(now, rearm = true)
        entry.healthWatch().beat()
    }
}
