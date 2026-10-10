package com.toparla.reminders

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.toparla.data.settings.SettingsStore
import com.toparla.domain.reminder.AlarmApi
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.HealthReport
import com.toparla.domain.reminder.HealthSnapshot
import com.toparla.domain.reminder.PlannedAlarm
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderEngine
import com.toparla.domain.reminder.StandbyBucket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import java.time.Instant

/** Sistem alarmları ve cihaz korumalı kopya (F2.25, F2.32). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmManagerSchedulerTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val scheduler = AlarmManagerScheduler(context, LauncherIntents(context))
    private val mirror = BootMirror(context)

    private fun alarm(key: String, atMs: Long, api: AlarmApi) = PlannedAlarm(key, key.substringBefore('@'), Instant.ofEpochMilli(atMs), ReminderClass.CRITICAL, api)

    @Test
    fun `kritik alarm saat alarmi olarak kurulur ve basligiyla kopyaya yazilir`() {
        scheduler.schedule(alarm("disci@5000", 5000, AlarmApi.ALARM_CLOCK), "Dişçi")

        val scheduled = alarms.scheduledAlarms.single()
        assertEquals(5000, scheduled.triggerAtMs)
        assertNotNull(scheduled.alarmClockInfo)
        assertEquals(BootMirror.Entry("disci@5000", 5000, AlarmApi.ALARM_CLOCK, "Dişçi"), mirror.find("disci@5000"))
    }

    @Test
    fun `ayni anahtar yeniden kurulunca tek alarm kalir iptal sistemden ve kopyadan siler`() {
        scheduler.schedule(alarm("fatura@5000", 5000, AlarmApi.EXACT_IDLE), "Fatura")
        scheduler.schedule(alarm("fatura@5000", 5000, AlarmApi.EXACT_IDLE), "Fatura")
        assertEquals(1, alarms.scheduledAlarms.size)
        assertTrue(alarms.scheduledAlarms.single().isAllowWhileIdle)

        scheduler.cancel("fatura@5000")

        assertTrue(alarms.scheduledAlarms.isEmpty())
        assertNull(mirror.find("fatura@5000"))
    }

    @Test
    fun `kilitli acilista vakti gelmemis alarmlar kopyadan kurulur vakti gecenler bildirilir`() {
        mirror.put("gecmis@1000", 1000, AlarmApi.ALARM_CLOCK, "Geçmiş")
        mirror.put("gelecek@9000", 9000, AlarmApi.EXACT_IDLE, "Gelecek")

        val overdue = scheduler.rearmFromMirror(nowMs = 5000)

        assertEquals(listOf("gecmis@1000"), overdue)
        assertEquals(9000, alarms.scheduledAlarms.single().triggerAtMs)
    }

    @Test
    fun `kopya ayirici iceren basligi ve basliksiz eski kaydi dogru okur vakti geceni budar`() {
        mirror.put("a@9000", 9000, AlarmApi.EXACT_IDLE, "Kira | elektrik")
        mirror.put("b@9000", 9000, AlarmApi.INEXACT)
        // Başlık alanı eklenmeden önce yazılmış kayıt biçimi.
        context.createDeviceProtectedStorageContext().getSharedPreferences("alarm_mirror", 0).edit().putString("eski@1000", "1000|ALARM_CLOCK").commit()

        assertEquals("Kira | elektrik", mirror.find("a@9000")?.title)
        assertNull(mirror.find("b@9000")?.title)
        assertEquals(BootMirror.Entry("eski@1000", 1000, AlarmApi.ALARM_CLOCK, null), mirror.find("eski@1000"))

        mirror.markAwaitingDetail("a@9000")
        mirror.prune(nowMs = 5000)

        // Ayrıntı bekleyenler listesi alarm kaydı sayılmaz ve budamadan etkilenmez.
        assertEquals(setOf("a@9000", "b@9000"), mirror.all().mapTo(HashSet()) { it.key })
        assertEquals(setOf("a@9000"), mirror.takeAwaitingDetail())
    }
}

/** Nabız uyarısı (F2.33): karar `:domain/Heartbeat`'te; burada bildirimin gösterilmesi, yinelenmemesi ve kalkması. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HealthWatchTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
    private val settings = SettingsStore(PreferenceDataStoreFactory.create { File(context.cacheDir, "nabiz-${System.nanoTime()}.preferences_pb") })
    private var snapshot = HealthSnapshot(
        notifications = true, exactAlarms = true, fullScreen = true, batteryExempt = true,
        dndAccess = true, autoStartConfirmed = true, weakenedChannels = emptySet(), standbyBucket = StandbyBucket.ACTIVE,
        upcomingReminders = 0, lastDelivery = null, recentDeliveries = emptyList(), lastSelfTest = null,
    )
    private val watch = HealthWatch(context, settings, LauncherIntents(context)) { HealthReport.of(snapshot, Instant.EPOCH) }

    @Test
    fun `eksik yokken uyari gosterilmez`() = runBlocking {
        watch.beat()
        assertNull(manager.getNotification(HealthWatch.NOTIFICATION_ID))
    }

    @Test
    fun `yeni eksikte sistem kanalinda tek uyari gosterilir ayni eksikte yinelenmez duzelince kalkar`() = runBlocking {
        snapshot = snapshot.copy(fullScreen = false)
        watch.beat()
        val first = manager.getNotification(HealthWatch.NOTIFICATION_ID)
        assertEquals(AndroidReminderNotifier.CHANNEL_SYSTEM, first.channelId)
        assertEquals(setOf(HealthCheck.FULL_SCREEN.name), settings.heartbeatWarned.first())

        // Kullanıcı uyarıyı kaydırıp sildi; eksik sürse de nabız yeniden dürtmez (ekranda görünmeye devam eder).
        context.getSystemService(NotificationManager::class.java).cancel(HealthWatch.NOTIFICATION_ID)
        watch.beat()
        assertNull(manager.getNotification(HealthWatch.NOTIFICATION_ID))

        snapshot = snapshot.copy(batteryExempt = false)
        watch.beat()
        assertNotNull(manager.getNotification(HealthWatch.NOTIFICATION_ID))

        snapshot = snapshot.copy(fullScreen = true, batteryExempt = true)
        watch.beat()
        assertNull(manager.getNotification(HealthWatch.NOTIFICATION_ID))
        assertTrue(settings.heartbeatWarned.first().isEmpty())
    }
}

/**
 * Hatırlatma Sağlığı'nın sistemden okunan satırları (F2.35): kanal önemi ve bekleme kovası; "kapalı uygulamaya
 * teslim" kaydı (F2.36). Kararlar `:domain`'dedir (`StandbyBucket`, `ColdDelivery`); burada Android'e değen kısım.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SystemHealthTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun `kanallar kuruldugu gibiyken kisilan sinif yoktur`() {
        AndroidReminderNotifier.ensureChannels(context)

        assertTrue(AndroidReminderNotifier.weakenedChannels(context).isEmpty())
    }

    @Test
    fun `onemi dusurulen ya da kapatilan kanalin sinifi kisilmis sayilir`() {
        AndroidReminderNotifier.ensureChannels(context)
        manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_CRITICAL).importance = NotificationManager.IMPORTANCE_NONE
        manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_IMPORTANT).importance = NotificationManager.IMPORTANCE_LOW
        // Yükseltilen kanalın sayılmaması `:domain/ChannelHealth` testindedir: sahte sistem, kanal yeniden
        // kurulurken yükseltilmiş önemi geri indirdiği için burada sınanamıyor (kasıtlı bozma bunu gösterdi).

        assertEquals(setOf(ReminderClass.CRITICAL, ReminderClass.IMPORTANT), AndroidReminderNotifier.weakenedChannels(context))
    }

    @Test
    fun `bekleme kovasi sistemden okunur`() {
        val usage = shadowOf(context.getSystemService(UsageStatsManager::class.java))

        usage.setCurrentAppStandbyBucket(UsageStatsManager.STANDBY_BUCKET_ACTIVE)
        assertEquals(StandbyBucket.ACTIVE, HealthProbe.standbyBucket(context))
        usage.setCurrentAppStandbyBucket(UsageStatsManager.STANDBY_BUCKET_RARE)
        assertEquals(StandbyBucket.RARE, HealthProbe.standbyBucket(context))
        usage.setCurrentAppStandbyBucket(UsageStatsManager.STANDBY_BUCKET_RESTRICTED)
        assertEquals(StandbyBucket.RESTRICTED, HealthProbe.standbyBucket(context))
    }

    @Test
    fun `alarm kapali uygulamayi uyandirdiysa teslim gunlugune yazilir acik uygulamada ve bakim alarminda yazilmaz`() = runBlocking {
        val logged = ArrayList<Triple<String, String, String?>>()
        val log: suspend (String, Instant, String, String?) -> Unit = { key, _, event, detail -> logged += Triple(key, event, detail) }
        val at = Instant.ofEpochMilli(5000)

        WakeLog.record("deneme@5000", Duration.ofMillis(312), at, log)
        WakeLog.record("acik@5000", Duration.ofMinutes(3), at, log)
        WakeLog.record(AlarmManagerScheduler.MAINTENANCE_KEY, Duration.ofMillis(200), at, log)

        assertEquals(listOf(Triple("deneme@5000", ReminderEngine.EVENT_WOKE_APP, "312 ms")), logged)
    }

    @Test
    fun `surec yasi okunur ve negatif degildir`() {
        assertTrue(!WakeLog.processAge().isNegative)
    }
}
