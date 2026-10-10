package com.toparla.reminders

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.database.SQLException
import com.toparla.domain.reminder.AlarmKey
import timber.log.Timber
import java.io.IOException

/**
 * Veritabanına ulaşılamayan anların bildirimi: kilit açılmadan çalan alarm (Direct Boot) ve kayıt yazılamayan
 * teslim (depolama dolu). İçerik cihaz korumalı kopyadaki başlıktan gelir ([BootMirror]); asıl bildirim
 * veritabanı açılınca aynı kimlikle bunun yerini alır.
 */
internal object FallbackNotice {
    fun show(context: Context, alarmKey: String, critical: Boolean, textRes: Int) {
        AndroidReminderNotifier.ensureChannels(context)
        val channel = if (critical) AndroidReminderNotifier.CHANNEL_CRITICAL else AndroidReminderNotifier.CHANNEL_IMPORTANT
        val title = BootMirror(context).find(alarmKey)?.title
        val builder = base(context, channel, critical)
            .setContentTitle(title ?: context.getString(R.string.locked_boot_title))
            .setContentText(context.getString(textRes))
            // Kritik başlık kilit ekranında da okunur (blueprint D25); ötekilerde kilit ekranı başlıksız hâli gösterir.
            .setVisibility(if (critical) Notification.VISIBILITY_PUBLIC else Notification.VISIBILITY_PRIVATE)
        if (!critical) {
            builder.setPublicVersion(base(context, channel, critical).setContentTitle(context.getString(R.string.locked_boot_title)).build())
        }
        // Kimlik teslimin kendi kimliği: merdiven basamağı da ait olduğu teslimin bildirimini günceller.
        val id = AndroidReminderNotifier.idOf(AlarmKey.parse(alarmKey).occurrenceKey)
        context.getSystemService(NotificationManager::class.java).notify(id, builder.build())
    }

    private fun base(context: Context, channel: String, critical: Boolean): Notification.Builder =
        Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_toparla)
            .setContentIntent(LauncherIntents(context).openApp())
            .setCategory(if (critical) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
}

/**
 * Teslimi yapar; kayıt açılamaz ya da yazılamazsa (depolama dolu, bozuk dosya) hatırlatma yine de gösterilir:
 * Kullanıcı'nın işi, kaydın tutulmasından önce gelir (blueprint Bölüm I "depolama dolu"). Teslim, kayıt
 * açılınca ayrıntısıyla yeniden yapılmak üzere not edilir.
 */
internal suspend fun deliverSafely(context: Context, alarmKey: String, critical: Boolean, deliver: suspend () -> Unit) {
    try {
        deliver()
    } catch (e: SQLException) {
        fallBack(context, alarmKey, critical, e)
    } catch (e: IOException) {
        fallBack(context, alarmKey, critical, e)
    }
}

private fun fallBack(context: Context, alarmKey: String, critical: Boolean, cause: Exception) {
    Timber.e(cause, "Teslim kaydı yazılamadı; hatırlatma kopyadan gösteriliyor")
    if (alarmKey == AlarmManagerScheduler.MAINTENANCE_KEY) return
    FallbackNotice.show(context, alarmKey, critical, R.string.storage_fallback_text)
    BootMirror(context).markAwaitingDetail(alarmKey)
}
