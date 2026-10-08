package com.toparla.spike

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import java.io.File

/**
 * S0 spike 1: alarm teslim ölçümü. Atılacak koddur; ürün kuralları (Clock enjeksiyonu vb.)
 * burada bilerek uygulanmaz. Veriler cihaz korumalı depolamada durur ki kilitli yeniden
 * başlatmada (Direct Boot) da okunabilsin.
 */
object AlarmSpike {
    const val TAG = "TOPARLA_SPIKE"
    const val API_CLOCK = "clock" // setAlarmClock
    const val API_IDLE = "idle" // setExactAndAllowWhileIdle
    const val API_INEXACT = "inexact" // setAndAllowWhileIdle (Normal sınıf adayı)
    const val API_FSI = "fsi" // setAlarmClock + tam ekran bildirim (spike 3)
    const val API_FGS = "fgs" // setAlarmClock + foreground service başlatma (spike 2)
    const val API_FGS_IDLE = "fgsidle" // setExactAndAllowWhileIdle + foreground service başlatma
    const val API_CRIT = "crit" // setAlarmClock + alarm sesli kritik kanal bildirimi (ekran kapalıyken ses)
    const val EXTRA_KEY = "key"

    private const val PREFS = "pending"
    private const val LOG_FILE = "log.csv"

    private fun dps(context: Context): Context = context.createDeviceProtectedStorageContext()

    fun logFile(context: Context): File = File(dps(context).filesDir, LOG_FILE)

    @Synchronized
    fun log(context: Context, event: String, key: String, plannedAt: Long, detail: String = "") {
        val now = System.currentTimeMillis()
        val pm = context.getSystemService(PowerManager::class.java)
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val line = listOf(
            now, event, key, plannedAt, if (plannedAt > 0) now - plannedAt else 0,
            "idle=${pm.isDeviceIdleMode}", "light=${pm.isDeviceLightIdleMode}", "screen=${pm.isInteractive}",
            "batt=${context.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}",
            "bucket=${usm.appStandbyBucket}", detail,
        ).joinToString(",")
        Log.i(TAG, line)
        logFile(context).appendText(line + "\n")
    }

    fun schedule(context: Context, api: String, plannedAt: Long) {
        val key = "$api-$plannedAt"
        dps(context).getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(key, plannedAt).apply()
        arm(context, key, api, plannedAt)
        log(context, "SCHEDULED", key, plannedAt)
    }

    /** Yeniden başlatma, paket güncelleme, saat değişimi sonrası bekleyenleri yeniden kurar. */
    fun rescheduleAll(context: Context, reason: String) {
        val prefs = dps(context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        log(context, "RESCHEDULE", reason, 0, "pending=${prefs.all.size}")
        prefs.all.forEach { (key, value) ->
            val plannedAt = value as Long
            if (plannedAt <= now) {
                log(context, "MISSED_DETECTED", key, plannedAt, reason)
                prefs.edit().remove(key).apply()
            } else {
                arm(context, key, key.substringBefore('-'), plannedAt)
                log(context, "REARMED", key, plannedAt, reason)
            }
        }
    }

    fun resolve(context: Context, key: String) {
        dps(context).getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key).apply()
    }

    fun plannedAt(key: String): Long = key.substringAfter('-').toLongOrNull() ?: 0

    private fun arm(context: Context, key: String, api: String, plannedAt: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val fire = PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_KEY, key),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (api == API_CLOCK || api == API_FSI || api == API_FGS || api == API_CRIT) {
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, SpikeActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(plannedAt, show), fire)
        } else if (api == API_INEXACT) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plannedAt, fire)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plannedAt, fire)
        }
    }
}
