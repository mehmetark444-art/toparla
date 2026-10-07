package com.toparla.domain.reminder

import com.toparla.domain.Defaults
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class LadderAndFollowUpTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val interval = Duration.ofMinutes(Defaults.PERSISTENT_REMINDER_INTERVAL_MIN.toLong())
    private val sleepStart = LocalTime.of(23, 30)
    private val sleepEnd = LocalTime.of(7, 30)

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun next(last: String, silentUntil: String? = null) =
        PersistentFollowUp.nextAskAt(at(last), interval, zone, sleepStart, sleepEnd, silentUntil?.let(::at))

    @Test
    fun `kritik merdiven 0 2 5 10 dakika`() {
        val steps = Ladder.stepsFor(ReminderClass.CRITICAL)
        assertEquals(listOf(0L, 2L, 5L, 10L), steps.map { it.offset.toMinutes() })
        assertEquals(
            listOf(LadderAction.NOTIFY, LadderAction.VIBRATE_REPEAT, LadderAction.FULL_SCREEN, LadderAction.FULL_SCREEN),
            steps.map { it.action },
        )
    }

    @Test
    fun `guvenilir kisi basamagi yalniz acikken ve 15 dakikada`() {
        val last = Ladder.stepsFor(ReminderClass.CRITICAL, trustedContactEnabled = true).last()
        assertEquals(LadderStep(Duration.ofMinutes(15), LadderAction.TRUSTED_CONTACT_SMS), last)
        assertEquals(4, Ladder.stepsFor(ReminderClass.CRITICAL).size)
    }

    @Test
    fun `onemli ve normal tek tekrar bilgi tek bildirim`() {
        assertEquals(listOf(0L, 30L), Ladder.stepsFor(ReminderClass.IMPORTANT).map { it.offset.toMinutes() })
        assertEquals(listOf(0L, 30L), Ladder.stepsFor(ReminderClass.NORMAL).map { it.offset.toMinutes() })
        assertEquals(1, Ladder.stepsFor(ReminderClass.INFO).size)
    }

    @Test
    fun `gun icinde yarim saat sonra sorar`() {
        assertEquals(at("2026-10-07T15:00"), next("2026-10-07T14:30"))
    }

    @Test
    fun `uyku penceresine denk gelen soru sabah uyanisa kayar`() {
        assertEquals(at("2026-10-08T07:30"), next("2026-10-07T23:10"))
    }

    @Test
    fun `gece yarisindan sonra da uyanisa kayar`() {
        assertEquals(at("2026-10-08T07:30"), next("2026-10-08T02:00"))
    }

    @Test
    fun `uyku baslangic ani uykudur bitis ani degildir`() {
        assertEquals(at("2026-10-08T07:30"), next("2026-10-07T23:00"))
        assertEquals(at("2026-10-08T07:30"), next("2026-10-08T07:00"))
        assertEquals(at("2026-10-08T08:00"), next("2026-10-08T07:30"))
    }

    @Test
    fun `sessizlik bitene kadar sormaz`() {
        assertEquals(at("2026-10-07T17:00"), next("2026-10-07T14:30", silentUntil = "2026-10-07T17:00"))
    }

    @Test
    fun `gecmis sessizlik etkisizdir`() {
        assertEquals(at("2026-10-07T15:00"), next("2026-10-07T14:30", silentUntil = "2026-10-07T14:40"))
    }

    @Test
    fun `sessizlik uykuya biterse uyanisa kayar`() {
        assertEquals(at("2026-10-08T07:30"), next("2026-10-07T21:00", silentUntil = "2026-10-08T00:15"))
    }

    @Test
    fun `gun icine dusen uyku penceresi de calisir`() {
        val result = PersistentFollowUp.nextAskAt(
            at("2026-10-07T13:45"), interval, zone, LocalTime.of(14, 0), LocalTime.of(15, 0),
        )
        assertEquals(at("2026-10-07T15:00"), result)
    }

    @Test
    fun `uyku penceresi tanimsizsa her saatte sorar`() {
        val result = PersistentFollowUp.nextAskAt(at("2026-10-07T23:50"), interval, zone, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT)
        assertEquals(at("2026-10-08T00:20"), result)
    }

    @Test
    fun `gun boyu yapilmayan is icin soru zinciri uykuyu atlar ve ertesi gun surer`() {
        var last = at("2026-10-07T22:00")
        val asked = (1..5).map { next(last.atZone(zone).toLocalDateTime().toString()).also { last = it } }
        assertEquals(
            listOf("2026-10-07T22:30", "2026-10-07T23:00", "2026-10-08T07:30", "2026-10-08T08:00", "2026-10-08T08:30").map(::at),
            asked,
        )
    }

    @Test
    fun `aralik pozitif olmali`() {
        assertThrows(IllegalArgumentException::class.java) {
            PersistentFollowUp.nextAskAt(at("2026-10-07T14:30"), Duration.ZERO, zone, sleepStart, sleepEnd)
        }
    }
}
