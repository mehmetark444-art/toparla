package com.toparla.reminders

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteFullException
import android.media.AudioAttributes
import android.os.UserManager
import com.toparla.domain.reminder.AlarmApi
import com.toparla.domain.reminder.DeliveryNotice
import com.toparla.domain.reminder.LadderAction
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Duration
import java.time.Instant

/**
 * Zorunlu senaryoların Android tarafı (blueprint Bölüm I, yol haritası F2.46): Rahatsız Etme açık · bildirim izni
 * kapalı · tam ekran izni kapalı · ağ yok · depolama dolu. Saf mantık karşılıkları `:domain`'dedir
 * (`MandatoryScenariosTest`, `ReminderEngineTest`); burada yalnız Android'e değen kısım sınanır.
 * Gerçek telefondaki davranış ayrıca ölçülür (`docs/platform-bulgulari.md`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AndroidScenariosTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = AndroidReminderNotifier(context, LauncherIntents(context))
    private val key = "disci@1000"

    private fun notice(klass: ReminderClass, action: LadderAction = LadderAction.NOTIFY) =
        DeliveryNotice(key, ReminderInfo("disci", klass, "Dişçi", "", persistent = false), action, Instant.ofEpochMilli(1000), Duration.ZERO, "g@0")

    private fun posted(): List<Notification> = shadowOf(manager).allNotifications

    @Test
    fun `rahatsiz etme acikken kritik bildirim alarm sesli ve asan kanaldan alarm kategorisiyle gider`() {
        manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)

        notifier.show(notice(ReminderClass.CRITICAL))

        val notification = posted().single()
        assertEquals(AndroidReminderNotifier.CHANNEL_CRITICAL, notification.channelId)
        assertEquals(Notification.CATEGORY_ALARM, notification.category)
        val channel = manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_CRITICAL)
        assertTrue(channel.canBypassDnd())
        assertEquals(AudioAttributes.USAGE_ALARM, channel.audioAttributes.usage)
        // Aşma yalnız kritik kanaldadır (blueprint G5).
        assertFalse(manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_IMPORTANT).canBypassDnd())
        assertFalse(manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_NORMAL).canBypassDnd())
    }

    @Test
    fun `bildirim izni kapaliyken teslim cokmez ve engelli diye bildirilir izin donunce engel kalkar`() {
        shadowOf(manager).setNotificationsEnabled(false)
        assertTrue(notifier.isBlocked(ReminderClass.CRITICAL))

        notifier.show(notice(ReminderClass.CRITICAL))

        shadowOf(manager).setNotificationsEnabled(true)
        assertFalse(notifier.isBlocked(ReminderClass.CRITICAL))
    }

    @Test
    fun `kanali kapatilan sinif engelli sayilir oteki siniflar etkilenmez`() {
        manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_NORMAL).importance = NotificationManager.IMPORTANCE_NONE

        assertTrue(notifier.isBlocked(ReminderClass.NORMAL))
        assertFalse(notifier.isBlocked(ReminderClass.CRITICAL))
    }

    @Test
    fun `tam ekran basamagi izin olmasa da bildirim olarak gider ve onceki bildirimin yerini alir`() {
        notifier.show(notice(ReminderClass.CRITICAL))
        notifier.show(notice(ReminderClass.CRITICAL, LadderAction.FULL_SCREEN))

        // İzin yoksa sistem tam ekran niyetini yok sayar ve şerit gösterir; bildirim her durumda tektir.
        val notification = posted().single()
        assertNotNull(notification.fullScreenIntent)
        assertEquals(AndroidReminderNotifier.CHANNEL_CRITICAL, notification.channelId)
        assertEquals(2, notification.actions.size)
    }

    @Test
    fun `ag yok - hatirlatma modulu internet izni istemez teslim aga bagli degildir`() {
        val requested = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue("alarm izni bekleniyordu: ${requested.toList()}", "android.permission.USE_EXACT_ALARM" in requested)
        assertFalse("android.permission.INTERNET" in requested)
    }

    @Test
    fun `depolama doluyken kayit yazilamasa da hatirlatma kopyadaki baslikla gosterilir ve ayrinti icin not edilir`() = runBlocking {
        BootMirror(context).put(key, 1000, AlarmApi.ALARM_CLOCK, "Dişçi")

        deliverSafely(context, key, critical = true) { throw SQLiteFullException("database or disk is full") }

        val notification = posted().single()
        assertEquals("Dişçi", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(AndroidReminderNotifier.CHANNEL_CRITICAL, notification.channelId)
        assertEquals(setOf(key), BootMirror(context).takeAwaitingDetail())
    }

    @Test
    fun `ayar dosyasi okunamazsa da hatirlatma gosterilir baska hatalar gizlenmez`() {
        runBlocking { deliverSafely(context, key, critical = false) { throw IOException("ayar dosyası okunamadı") } }
        assertEquals(AndroidReminderNotifier.CHANNEL_IMPORTANT, posted().single().channelId)

        // Depolama dışındaki hata yutulmaz: programlama hatası görünür kalmalı.
        assertThrows(IllegalStateException::class.java) {
            runBlocking { deliverSafely(context, key, critical = false) { error("beklenmeyen") } }
        }
    }

    @Test
    fun `bakim alarmi depolama doluyken bildirim uretmez`() = runBlocking {
        deliverSafely(context, AlarmManagerScheduler.MAINTENANCE_KEY, critical = false) { throw SQLiteFullException("dolu") }
        assertTrue(posted().isEmpty())
        assertTrue(BootMirror(context).takeAwaitingDetail().isEmpty())
    }

    @Test
    fun `kilit acilmadan calan alarm kopyadaki baslikla gosterilir`() {
        shadowOf(context.getSystemService(UserManager::class.java)).setUserUnlocked(false)
        BootMirror(context).put(key, 1000, AlarmApi.ALARM_CLOCK, "Dişçi")

        AlarmReceiver().onReceive(context, fireIntent(key, critical = true))

        val notification = posted().single()
        assertEquals("Dişçi", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(context.getString(R.string.locked_boot_text), notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(Notification.VISIBILITY_PUBLIC, notification.visibility)
        assertEquals(setOf(key), BootMirror(context).takeAwaitingDetail())
    }

    @Test
    fun `kilitliyken kritik olmayan hatirlatmanin basligi kilit ekraninda gizli kalir`() {
        shadowOf(context.getSystemService(UserManager::class.java)).setUserUnlocked(false)
        BootMirror(context).put(key, 1000, AlarmApi.EXACT_IDLE, "Fatura")

        AlarmReceiver().onReceive(context, fireIntent(key, critical = false))

        val notification = posted().single()
        assertEquals("Fatura", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals(context.getString(R.string.locked_boot_title), notification.publicVersion.extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun `kopyada basligi olmayan alarm genel baslikla gosterilir merdiven basamagi teslimin bildirimini kullanir`() {
        shadowOf(context.getSystemService(UserManager::class.java)).setUserUnlocked(false)

        AlarmReceiver().onReceive(context, fireIntent("$key#l1", critical = true))

        // Basamak anahtarı (`…#l1`) teslimin kendi kimliğine yazılır: kilit açılınca gelen asıl bildirim yerini alır.
        val notification = shadowOf(manager).getNotification(AndroidReminderNotifier.idOf(key))
        assertEquals(context.getString(R.string.locked_boot_title), notification.extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun `kilitliyken calan bakim alarmi bildirim uretmez`() {
        shadowOf(context.getSystemService(UserManager::class.java)).setUserUnlocked(false)

        AlarmReceiver().onReceive(context, fireIntent(AlarmManagerScheduler.MAINTENANCE_KEY, critical = false))

        assertTrue(posted().isEmpty())
    }

    private fun fireIntent(alarmKey: String, critical: Boolean) = Intent(context, AlarmReceiver::class.java)
        .setAction(AlarmManagerScheduler.ACTION_FIRE)
        .putExtra(AlarmManagerScheduler.EXTRA_KEY, alarmKey)
        .putExtra(AlarmManagerScheduler.EXTRA_CRITICAL, critical)
}
