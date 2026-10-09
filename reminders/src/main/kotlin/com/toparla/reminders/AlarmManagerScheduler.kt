package com.toparla.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.toparla.domain.reminder.AlarmApi
import com.toparla.domain.reminder.PlannedAlarm
import com.toparla.domain.reminder.ReminderScheduler
import java.time.Instant

/**
 * Sistem alarmları (karar 0006): Kritik `setAlarmClock`; Önemli, Normal ve ısrarlı takip
 * `setExactAndAllowWhileIdle`; yalnız Bilgi sınıfı esnek yol. Aynı anahtar aynı `PendingIntent`'i üretir:
 * yeniden kurmak eskisinin üstüne yazar (idempotans).
 *
 * Kurulan her alarmın en küçük kopyası cihaz korumalı depolamaya da yazılır ([BootMirror]): telefon kilidi
 * açılmadan yeniden başlarsa alarmlar oradan kurulur (Direct Boot, blueprint G1).
 */
class AlarmManagerScheduler(
    private val context: Context,
    private val intents: ReminderIntents,
) : ReminderScheduler {
    private val alarmManager: AlarmManager get() = context.getSystemService(AlarmManager::class.java)
    private val mirror = BootMirror(context)

    override fun schedule(alarm: PlannedAlarm) {
        arm(alarm.key, alarm.fireAt.toEpochMilli(), alarm.api)
        mirror.put(alarm.key, alarm.fireAt.toEpochMilli(), alarm.api)
    }

    override fun cancel(key: String) {
        alarmManager.cancel(firePendingIntent(context, key, AlarmApi.EXACT_IDLE))
        mirror.remove(key)
    }

    override fun scheduleMaintenance(at: Instant) {
        // Bakım saati kaçarsa pencere dolmaz; esnek yol bu telefonda saatlerce kayabildiği için kesin yol.
        arm(MAINTENANCE_KEY, at.toEpochMilli(), AlarmApi.EXACT_IDLE)
    }

    /** Kilitli açılışta: kopyadaki, vakti gelmemiş alarmları yeniden kurar. Vakti geçenleri döndürür. */
    fun rearmFromMirror(nowMs: Long): List<String> {
        val overdue = ArrayList<String>()
        for (entry in mirror.all()) {
            if (entry.fireAtMs > nowMs) arm(entry.key, entry.fireAtMs, entry.api) else overdue += entry.key
        }
        return overdue
    }

    private fun arm(key: String, fireAtMs: Long, api: AlarmApi) {
        val fire = firePendingIntent(context, key, api)
        when (api) {
            AlarmApi.ALARM_CLOCK -> alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(fireAtMs, intents.openApp()), fire)
            AlarmApi.EXACT_IDLE -> alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAtMs, fire)
            AlarmApi.INEXACT -> alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAtMs, fire)
        }
    }

    companion object {
        const val MAINTENANCE_KEY = "bakim"
        const val EXTRA_KEY = "key"
        const val EXTRA_CRITICAL = "critical"

        /** `PendingIntent` eşitliği eylem + bileşen + istek koduna bakar; ekler sayılmaz. İptal aynı üçlüyle bulur. */
        fun firePendingIntent(context: Context, key: String, api: AlarmApi): PendingIntent = PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_FIRE)
                .putExtra(EXTRA_KEY, key)
                .putExtra(EXTRA_CRITICAL, api == AlarmApi.ALARM_CLOCK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        const val ACTION_FIRE = "com.toparla.reminders.FIRE"
    }
}

/** Kilitli açılışta alarm kurmaya yetecek en küçük kopya: anahtar, an, alarm yolu. Başlık ve içerik yok. */
class BootMirror(context: Context) {
    data class Entry(val key: String, val fireAtMs: Long, val api: AlarmApi)

    private val prefs: SharedPreferences =
        context.createDeviceProtectedStorageContext().getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun put(key: String, fireAtMs: Long, api: AlarmApi) {
        prefs.edit().putString(key, "$fireAtMs$SEPARATOR${api.name}").apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun all(): List<Entry> = prefs.all.mapNotNull { (key, value) ->
        val parts = (value as? String)?.split(SEPARATOR) ?: return@mapNotNull null
        val fireAt = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val api = AlarmApi.entries.firstOrNull { it.name == parts.getOrNull(1) } ?: return@mapNotNull null
        Entry(key, fireAt, api)
    }

    /** Vakti geçmiş kayıtları atar; kopya sınırsız büyümesin. */
    fun prune(nowMs: Long) {
        val editor = prefs.edit()
        all().filter { it.fireAtMs <= nowMs }.forEach { editor.remove(it.key) }
        editor.apply()
    }

    private companion object {
        const val FILE = "alarm_mirror"
        const val SEPARATOR = "|"
    }
}

/** Uygulamanın ekranlarına giden `PendingIntent`'ler; `:app` sağlar (ekranlar orada). */
interface ReminderIntents {
    /** Uygulamayı açar (bildirim gövdesine dokunma, "sonraki alarm" göstergesi). */
    fun openApp(): PendingIntent

    /** Kilit ekranı üstünde kritik hatırlatma kartını açar. */
    fun fullScreen(occurrenceKey: String): PendingIntent
}
