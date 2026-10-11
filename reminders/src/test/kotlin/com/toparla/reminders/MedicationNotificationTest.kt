package com.toparla.reminders

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import com.toparla.domain.reminder.DeliveryNotice
import com.toparla.domain.reminder.LadderAction
import com.toparla.domain.reminder.MedicationRole
import com.toparla.domain.reminder.MedicationTag
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

/**
 * İlaç bildirimleri (blueprint M9, D25; F2.39–F2.40): Aldım · 15 dk sonra · Atlıyorum; kilit ekranında her zaman
 * "İlaç zamanı"; "adı gizle" açıksa bildirimde de ad yok; çift doz sorusu; eczane hatırlatması.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MedicationNotificationTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifier = AndroidReminderNotifier(context, LauncherIntents(context))
    private val key = "med:m1:0800@1000"

    private fun dose(hideName: Boolean = false) =
        ReminderInfo("med:m1:0800", ReminderClass.CRITICAL, "Örnek", "1 tablet", persistent = false, MedicationTag("m1", MedicationRole.DOSE, hideName))

    private fun notice(info: ReminderInfo) = DeliveryNotice(key, info, LadderAction.NOTIFY, Instant.ofEpochMilli(1000), Duration.ZERO, "g@0")

    private fun posted(): Notification = shadowOf(manager).allNotifications.single()

    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    private fun Notification.actionLabels() = actions.map { it.title.toString() }

    @Test
    fun `ilac dozu kritik kanaldan uc ilac eylemiyle gider`() {
        notifier.show(notice(dose()))

        val n = posted()
        assertEquals(AndroidReminderNotifier.CHANNEL_CRITICAL, n.channelId)
        assertEquals(listOf("Aldım", "15 dk sonra", "Atlıyorum"), n.actionLabels())
        assertEquals("Örnek", n.title())
        assertEquals("1 tablet", n.text())
    }

    @Test
    fun `kilit ekraninda ilac adi gorunmez genel metin gorunur`() {
        notifier.show(notice(dose()))

        val n = posted()
        assertEquals(Notification.VISIBILITY_PRIVATE, n.visibility)
        val public = n.publicVersion
        assertNotNull(public)
        assertEquals("İlaç zamanı", public.title())
        assertFalse(public.extras.toString().contains("Örnek"))
    }

    @Test
    fun `adi gizle aciksa bildirimde de ad ve doz yok`() {
        notifier.show(notice(dose(hideName = true)))

        val n = posted()
        assertEquals("İlaç zamanı", n.title())
        assertTrue(n.text().isNullOrEmpty())
    }

    @Test
    fun `cift doz sorusu sureyi soyler ve evet vazgec eylemleri sunar`() {
        notifier.askDoubleDose(key, dose(), Duration.ofMinutes(150))

        val n = posted()
        assertEquals("Son kayıt 2 saat 30 dakika önce. Emin misin?", n.text())
        assertEquals(listOf("Evet, aldım", "Vazgeç"), n.actionLabels())
    }

    @Test
    fun `sure metni saat dakika ve bir dakikadan kisa durumlari ayirir`() {
        assertEquals("Son kayıt 3 saat önce. Emin misin?", AndroidReminderNotifier.doubleDoseText(context, Duration.ofHours(3)))
        assertEquals("Son kayıt 45 dakika önce. Emin misin?", AndroidReminderNotifier.doubleDoseText(context, Duration.ofMinutes(45)))
        assertEquals(
            "Bir dakikadan kısa süre önce bir doz kaydedildi. Emin misin?",
            AndroidReminderNotifier.doubleDoseText(context, Duration.ofSeconds(20)),
        )
    }

    @Test
    fun `uc ertelemeden sonra ilac dozu yarina tasinmaz aldim ya da atliyorum sorulur`() {
        notifier.askCarryOrSkip(key, dose())

        assertEquals(listOf("Aldım", "Atlıyorum"), posted().actionLabels())
    }

    @Test
    fun `eczane hatirlatmasi onemli kanaldan ilacin adiyla gelir adi gizliyse adsiz`() {
        val restock = ReminderInfo("med-stock:m1:1", ReminderClass.IMPORTANT, "Örnek", "", persistent = false, MedicationTag("m1", MedicationRole.RESTOCK))
        notifier.show(notice(restock))
        val n = posted()
        assertEquals(AndroidReminderNotifier.CHANNEL_IMPORTANT, n.channelId)
        assertEquals("Eczaneye uğra", n.title())
        assertEquals("Örnek azalıyor.", n.text())
        assertEquals("Hatırlatman var", n.publicVersion.title())

        manager.cancelAll()
        notifier.show(notice(restock.copy(medication = MedicationTag("m1", MedicationRole.RESTOCK, hideName = true))))
        assertEquals("Bir ilacın azalıyor.", posted().text())
    }

    @Test
    fun `ilac olmayan kritik hatirlatma eskisi gibi kilit ekraninda okunur`() {
        notifier.show(notice(ReminderInfo("disci", ReminderClass.CRITICAL, "Dişçi", "", persistent = false)))

        val n = posted()
        assertEquals(Notification.VISIBILITY_PUBLIC, n.visibility)
        assertEquals(listOf("Yaptım", "10 dk sonra"), n.actionLabels())
    }
}
