package com.toparla.app.reminder

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.toparla.data.db.CreatedBy
import com.toparla.data.db.OwnerType
import com.toparla.data.db.ReminderEntity
import com.toparla.data.db.ReminderStore
import com.toparla.domain.core.Clock
import com.toparla.domain.core.IdGenerator
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderDraft
import com.toparla.domain.reminder.ReminderEngine
import com.toparla.domain.reminder.ReminderPlanner
import com.toparla.reminders.AndroidReminderNotifier
import timber.log.Timber
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Hatırlatma tanımı oluşturur ve hemen planlar. Ekleme ekranı ve kendi kendini sınama bunu kullanır. */
@Singleton
class ReminderCreator @Inject constructor(
    private val store: ReminderStore,
    private val engine: ReminderEngine,
    private val clock: Clock,
    private val ids: IdGenerator,
) {
    /** @return kurulan ilk teslimin anahtarı (teslim kaydı bu anahtarla izlenir) */
    suspend fun create(
        draft: ReminderDraft,
        klass: ReminderClass,
        persistent: Boolean,
        body: String = "",
        createdBy: CreatedBy = CreatedBy.USER,
    ): String {
        val now = clock.now()
        val id = ids.newId()
        // Saniye hassasiyeti: anahtar planlanan anı milisaniyeyle taşır; kırpmak okunaklı ve kararlı kılar.
        val start = draft.startLocal.truncatedTo(ChronoUnit.SECONDS)
        store.saveReminder(
            ReminderEntity(
                id = id, ownerType = OwnerType.CUSTOM, ownerId = null, klass = klass, title = draft.title, body = body,
                startLocal = start.toString(), zoneId = clock.zone().id, recurrence = "ONCE", active = true,
                persistent = persistent, createdBy = createdBy, createdAt = now.toEpochMilli(), updatedAt = now.toEpochMilli(),
            ),
        )
        engine.replan(now)
        return ReminderPlanner.keyOf(id, start.atZone(clock.zone()).toInstant())
    }

    suspend fun delete(reminderId: String) {
        store.deleteReminder(reminderId, clock.now())
        engine.replan(clock.now())
    }
}

/**
 * Ayar sayfalarına derin bağlantılar (F1 ölçümü: `docs/hyperos-baglantilar.md`). Hedef bu telefonda yoksa
 * uygulama bilgisi sayfasına düşülür; o da açılmazsa hiçbir şey olmaz ve günlüğe yazılır.
 */
object SettingsLinks {
    /**
     * @param weakened kanal denetiminde sesi kısılmış sınıflar: ilkinin kanal sayfası açılır
     * Hedefler sırayla denenir; ilk açılan kazanır, hiçbiri açılmazsa uygulama bilgisi sayfası.
     */
    fun open(context: Context, check: HealthCheck, weakened: Set<ReminderClass> = emptySet()) {
        val pkg = Uri.parse("package:${context.packageName}")
        val appNotifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        val battery = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
        val targets = when (check) {
            HealthCheck.NOTIFICATIONS -> listOf(appNotifications)
            HealthCheck.CHANNELS -> listOfNotNull(
                weakened.minOrNull()?.let { klass ->
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, AndroidReminderNotifier.channelFor(klass))
                },
                appNotifications,
            )
            HealthCheck.EXACT_ALARMS -> listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
            HealthCheck.AUTO_START -> listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            )
            // Bekleme kovasının çaresi pil muafiyetidir: muaf uygulama kovaya girmez.
            HealthCheck.BATTERY, HealthCheck.STANDBY_BUCKET -> listOf(battery)
            HealthCheck.FULL_SCREEN -> listOf(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
            HealthCheck.DND_ACCESS -> listOf(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        (targets + fallback).firstOrNull { start(context, it) }
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "Ayar sayfası bu telefonda yok: %s", intent.component ?: intent.action)
        false
    } catch (e: SecurityException) {
        Timber.w(e, "Ayar sayfası açılamadı: %s", intent.component ?: intent.action)
        false
    }
}
