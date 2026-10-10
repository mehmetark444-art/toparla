package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** F2.35–F2.38'in saf kuralları: hazır zaman seçenekleri, taslak doğrulama, sağlık raporu, kendi kendini sınama. */
class ReminderSetupTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun `hazir zaman secenekleri simdiye gore cozulur`() {
        val now = at("2026-10-09T15:07:42")

        assertEquals(LocalDateTime.parse("2026-10-09T15:17:42"), QuickTime.IN_10_MIN.resolve(now, zone))
        assertEquals(LocalDateTime.parse("2026-10-09T16:07:42"), QuickTime.IN_1_HOUR.resolve(now, zone))
        assertEquals(LocalDateTime.parse("2026-10-09T20:00"), QuickTime.TONIGHT.resolve(now, zone))
        assertEquals(LocalDateTime.parse("2026-10-10T09:00"), QuickTime.TOMORROW_MORNING.resolve(now, zone))
    }

    @Test
    fun `aksam saati gectiyse bu aksam secenegi yarin aksama kayar`() {
        assertEquals(LocalDateTime.parse("2026-10-10T20:00"), QuickTime.TONIGHT.resolve(at("2026-10-09T20:00"), zone))
        assertEquals(LocalDateTime.parse("2026-10-10T20:00"), QuickTime.TONIGHT.resolve(at("2026-10-09T23:30"), zone))
    }

    @Test
    fun `taslak basligi kirpilir bos baslik ve gecmis zaman reddedilir`() {
        val now = at("2026-10-09T15:00")
        val future = LocalDateTime.parse("2026-10-09T16:00")

        assertEquals("Faturayı öde", ReminderDraft.validate("  Faturayı öde  ", future, now, zone)?.title)
        assertNull(ReminderDraft.validate("   ", future, now, zone))
        assertNull(ReminderDraft.validate("Geçmiş", LocalDateTime.parse("2026-10-09T14:59"), now, zone))
        assertEquals(ReminderDraft.MAX_TITLE, ReminderDraft.validate("x".repeat(200), future, now, zone)?.title?.length)
    }

    private val healthy = HealthSnapshot(
        notifications = true, exactAlarms = true, fullScreen = true, batteryExempt = true,
        dndAccess = true, autoStartConfirmed = true, pendingAlarms = 3, lastDeliveredAt = at("2026-10-09T10:00"),
    )

    @Test
    fun `her sey yerindeyse duzeltilecek bir sey yoktur`() {
        val report = HealthReport.of(healthy)

        assertTrue(report.toFix.isEmpty())
        assertEquals(HealthCheck.entries.toSet(), report.ok.toSet())
    }

    @Test
    fun `eksik ayarlar onem sirasiyla duzeltilecekler listesine girer`() {
        val report = HealthReport.of(healthy.copy(batteryExempt = false, notifications = false, autoStartConfirmed = false))

        assertEquals(listOf(HealthCheck.NOTIFICATIONS, HealthCheck.AUTO_START, HealthCheck.BATTERY), report.toFix)
        assertFalse(HealthCheck.NOTIFICATIONS in report.ok)
    }

    @Test
    fun `kendi kendini sinama vaktinde gelen gec gelen ve gelmeyeni ayirir`() {
        val planned = at("2026-10-09T15:00:20")
        val wait = SelfTest.TIMEOUT

        assertEquals(SelfTestResult.Waiting, SelfTest.evaluate(planned, null, at("2026-10-09T15:00:30")))
        assertEquals(SelfTestResult.Arrived(Duration.ofSeconds(1)), SelfTest.evaluate(planned, at("2026-10-09T15:00:21"), at("2026-10-09T15:00:25")))
        assertEquals(SelfTestResult.NotArrived, SelfTest.evaluate(planned, null, planned.plus(wait).plusSeconds(1)))
        // Süre dolduktan sonra gelse de sonuç "geldi"dir; gecikme gösterilir.
        assertEquals(
            SelfTestResult.Arrived(Duration.ofSeconds(90)),
            SelfTest.evaluate(planned, planned.plusSeconds(90), planned.plusSeconds(100)),
        )
    }
}
