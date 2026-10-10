package com.toparla.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.toparla.domain.reminder.DeliveryNotice
import com.toparla.domain.reminder.LadderAction
import com.toparla.domain.reminder.PersistentItem
import com.toparla.domain.reminder.ReminderAction
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderInfo
import com.toparla.domain.reminder.ReminderNotifier
import java.time.Duration
import java.time.Instant

/**
 * Bildirim yüzeyi (blueprint D25, G5). Eylemler uygulama açılmadan [NotificationActionReceiver] ile işlenir.
 * Kritik kanal alarm ses akışını kullanır ve izin varsa Rahatsız Etme'yi aşar (F1 ölçümü: kısılmıyor, aşıyor).
 */
class AndroidReminderNotifier(
    private val context: Context,
    private val intents: ReminderIntents,
) : ReminderNotifier {
    private val manager: NotificationManager get() = context.getSystemService(NotificationManager::class.java)

    init {
        ensureChannels(context)
    }

    override fun show(notice: DeliveryNotice) {
        val reminder = notice.reminder
        val text = if (notice.lateBy > Duration.ZERO) context.getString(R.string.late_delivery_text) else reminder.body
        val builder = base(notice.occurrenceKey, reminder, channelFor(reminder.klass))
            .setContentText(text.ifBlank { null })
            .setWhen(notice.plannedAt.toEpochMilli())
            .setShowWhen(true)
            .setGroup(notice.groupKey)
        actionsFor(reminder).forEach { (label, kind) -> builder.addAction(action(notice.occurrenceKey, label, kind)) }
        val id = idOf(notice.occurrenceKey)
        if (notice.action == LadderAction.FULL_SCREEN) {
            // Kilitliyken kilit ekranı üstünde tam ekran; ekran açık ve kilitsizken sistem şerit gösterir (F1 ölçümü).
            // Sistem tam ekran niyetini yalnız bildirim **ilk kez eklenirken** başlatır, güncellemede başlatmaz
            // (9 Ekim cihaz denemesi: aynı kimliği güncelleyince yalnız şerit geldi). Bu yüzden basamak ayrı
            // etiketle yeni bildirim olarak eklenir ve önceki kaldırılır.
            builder.setFullScreenIntent(intents.fullScreen(notice.occurrenceKey), true)
            manager.cancel(id)
            manager.cancel(TAG_FULL_SCREEN, id)
            manager.notify(TAG_FULL_SCREEN, id, builder.build())
        } else {
            manager.cancel(TAG_FULL_SCREEN, id)
            manager.notify(id, builder.build())
        }
    }

    override fun isBlocked(klass: ReminderClass): Boolean =
        !manager.areNotificationsEnabled() || manager.getNotificationChannel(channelFor(klass))?.importance == NotificationManager.IMPORTANCE_NONE

    override fun cancel(occurrenceKey: String) {
        manager.cancel(idOf(occurrenceKey))
        manager.cancel(TAG_FULL_SCREEN, idOf(occurrenceKey))
    }

    override fun showPersistent(items: List<PersistentItem>, askedAt: Instant) {
        if (items.isEmpty()) {
            manager.cancel(PERSISTENT_SUMMARY_ID)
            return
        }
        val pool = context.resources.getStringArray(R.array.persistent_ask_pool)
        for (item in items) {
            val builder = base(item.occurrenceKey, item.reminder, channelFor(item.reminder.klass))
                // Aynı cümle art arda gelmez: soru sayısı havuzda sıradaki cümleyi seçer.
                .setContentText(pool[item.asksDone % pool.size])
                .setWhen(askedAt.toEpochMilli())
                .setGroup(PERSISTENT_GROUP)
                .setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY)
            builder.addAction(action(item.occurrenceKey, R.string.action_done, ReminderAction.DONE))
            builder.addAction(action(item.occurrenceKey, R.string.action_not_today, ReminderAction.NOT_TODAY))
            manager.notify(idOf(item.occurrenceKey), builder.build())
        }
        val summary = Notification.Builder(context, CHANNEL_NORMAL)
            .setSmallIcon(R.drawable.ic_stat_toparla)
            .setContentTitle(context.getString(R.string.persistent_summary_title))
            .setContentText(context.resources.getQuantityString(R.plurals.persistent_summary_text, items.size, items.size))
            .setContentIntent(intents.openApp())
            .setWhen(askedAt.toEpochMilli())
            .setGroup(PERSISTENT_GROUP)
            .setGroupSummary(true)
            .build()
        manager.notify(PERSISTENT_SUMMARY_ID, summary)
    }

    override fun askCarryOrSkip(occurrenceKey: String, reminder: ReminderInfo) {
        val builder = base(occurrenceKey, reminder, channelFor(reminder.klass))
            .setContentText(context.getString(R.string.carry_or_skip_text))
        builder.addAction(action(occurrenceKey, R.string.action_carry_tomorrow, ReminderAction.TOMORROW))
        builder.addAction(action(occurrenceKey, R.string.action_skip, ReminderAction.NOT_TODAY))
        manager.notify(idOf(occurrenceKey), builder.build())
    }

    private fun base(occurrenceKey: String, reminder: ReminderInfo, channel: String): Notification.Builder =
        Notification.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_toparla)
            .setContentTitle(reminder.title)
            .setContentIntent(intents.openApp())
            .setCategory(if (reminder.klass == ReminderClass.CRITICAL) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
            // Kritik kart kilit ekranında da okunur (blueprint D25); kaydırıp silmek yanıt sayılmaz, merdiven sürer.
            .setVisibility(if (reminder.klass == ReminderClass.CRITICAL) Notification.VISIBILITY_PUBLIC else Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .also { it.extras.putString(EXTRA_OCCURRENCE_KEY, occurrenceKey) }

    /** Android en çok üç eylem gösterir; sınıfa göre en işe yarar üçü (blueprint D25, F2.28). */
    private fun actionsFor(reminder: ReminderInfo): List<Pair<Int, ReminderAction>> = when (reminder.klass) {
        ReminderClass.CRITICAL -> listOf(R.string.action_done to ReminderAction.DONE, R.string.action_snooze to ReminderAction.SNOOZE)
        ReminderClass.IMPORTANT -> listOf(
            R.string.action_done to ReminderAction.DONE,
            R.string.action_snooze to ReminderAction.SNOOZE,
            R.string.action_tomorrow to ReminderAction.TOMORROW,
        )
        ReminderClass.NORMAL, ReminderClass.INFO -> listOf(
            R.string.action_done to ReminderAction.DONE,
            R.string.action_snooze to ReminderAction.SNOOZE,
            R.string.action_not_today to ReminderAction.NOT_TODAY,
        )
    }

    private fun action(occurrenceKey: String, labelRes: Int, action: ReminderAction): Notification.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction("${NotificationActionReceiver.ACTION_PREFIX}${action.name}")
            .putExtra(NotificationActionReceiver.EXTRA_OCCURRENCE_KEY, occurrenceKey)
        // İstek kodu anahtar + eylemden: farklı bildirimlerin aynı eylemi birbirinin üstüne yazmaz.
        val pending = PendingIntent.getBroadcast(
            context, "$occurrenceKey|${action.name}".hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(null, context.getString(labelRes), pending).build()
    }

    companion object {
        const val CHANNEL_CRITICAL = "critical"
        const val CHANNEL_IMPORTANT = "important"
        const val CHANNEL_NORMAL = "normal"
        const val CHANNEL_GUNES = "gunes"
        const val CHANNEL_FLOW = "flow"
        const val CHANNEL_FLOW_EVENT = "flow_event"
        const val CHANNEL_ONGOING = "ongoing"
        const val CHANNEL_SYSTEM = "system"
        const val EXTRA_OCCURRENCE_KEY = "toparla.occurrenceKey"
        private const val PERSISTENT_GROUP = "persistent"
        private const val TAG_FULL_SCREEN = "tam-ekran"
        private const val PERSISTENT_SUMMARY_ID = 1

        fun idOf(occurrenceKey: String): Int = occurrenceKey.hashCode()

        fun channelFor(klass: ReminderClass): String = when (klass) {
            ReminderClass.CRITICAL -> CHANNEL_CRITICAL
            ReminderClass.IMPORTANT -> CHANNEL_IMPORTANT
            ReminderClass.NORMAL -> CHANNEL_NORMAL
            ReminderClass.INFO -> CHANNEL_GUNES
        }

        /** Sekiz kanal (blueprint G5). Yeniden çağırmak zararsızdır; Kullanıcı'nın kanal ayarını ezmez. */
        fun ensureChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            fun channel(id: String, name: Int, desc: Int, importance: Int, configure: NotificationChannel.() -> Unit = {}) =
                NotificationChannel(id, context.getString(name), importance).apply {
                    description = context.getString(desc)
                    configure()
                }
            val alarmSound = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannels(
                listOf(
                    channel(CHANNEL_CRITICAL, R.string.channel_critical, R.string.channel_critical_desc, NotificationManager.IMPORTANCE_HIGH) {
                        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmSound)
                        enableVibration(true)
                        setBypassDnd(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    },
                    channel(CHANNEL_IMPORTANT, R.string.channel_important, R.string.channel_important_desc, NotificationManager.IMPORTANCE_HIGH) {
                        enableVibration(true)
                    },
                    channel(CHANNEL_NORMAL, R.string.channel_normal, R.string.channel_normal_desc, NotificationManager.IMPORTANCE_DEFAULT),
                    channel(CHANNEL_GUNES, R.string.channel_gunes, R.string.channel_gunes_desc, NotificationManager.IMPORTANCE_LOW),
                    channel(CHANNEL_FLOW, R.string.channel_flow, R.string.channel_flow_desc, NotificationManager.IMPORTANCE_LOW),
                    channel(CHANNEL_FLOW_EVENT, R.string.channel_flow_event, R.string.channel_flow_event_desc, NotificationManager.IMPORTANCE_DEFAULT),
                    channel(CHANNEL_ONGOING, R.string.channel_ongoing, R.string.channel_ongoing_desc, NotificationManager.IMPORTANCE_LOW),
                    channel(CHANNEL_SYSTEM, R.string.channel_system, R.string.channel_system_desc, NotificationManager.IMPORTANCE_LOW),
                ),
            )
        }
    }
}
