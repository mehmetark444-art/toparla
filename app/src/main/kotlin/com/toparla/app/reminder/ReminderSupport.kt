package com.toparla.app.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.toparla.data.db.CreatedBy
import com.toparla.data.db.OwnerType
import com.toparla.data.db.ReminderEntity
import com.toparla.data.db.ReminderStore
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.core.Clock
import com.toparla.domain.core.IdGenerator
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.HealthReport
import com.toparla.domain.reminder.HealthSnapshot
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderDraft
import com.toparla.domain.reminder.ReminderEngine
import com.toparla.domain.reminder.ReminderPlanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
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
 * Ayar sayfalarına derin bağlantılar (F1 ölçümü: `docs/hyperos-baglantilar.md`). Hedef bu telefonda yoksa
 * uygulama bilgisi sayfasına düşülür; o da açılmazsa hiçbir şey olmaz ve günlüğe yazılır.
 */
object SettingsLinks {
    fun open(context: Context, check: HealthCheck) {
        val pkg = Uri.parse("package:${context.packageName}")
        val intent = when (check) {
            HealthCheck.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            HealthCheck.EXACT_ALARMS -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
            HealthCheck.AUTO_START -> Intent().setComponent(
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            )
            HealthCheck.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
            HealthCheck.FULL_SCREEN -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
            HealthCheck.DND_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
        if (!start(context, intent)) start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg))
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
