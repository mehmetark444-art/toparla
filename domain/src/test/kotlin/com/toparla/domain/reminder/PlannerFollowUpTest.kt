package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** F2.6: planlayıcıya merdiven, ısrarlı takip ve erteleme teslimlerinin eklenmesi. */
class PlannerFollowUpTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val followUp = FollowUpConfig(
        interval = Duration.ofMinutes(30),
        zone = zone,
        sleepStart = LocalTime.of(23, 30),
        sleepEnd = LocalTime.of(7, 30),
    )

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun def(id: String, klass: ReminderClass, start: String) =
        ReminderDef(id, klass, LocalDateTime.parse(start), zone)

    private fun inFlight(
        klass: ReminderClass,
        firedAt: String,
        ladderStepsDone: Int = 1,
        persistent: Boolean = false,
        lastAskedAt: String = firedAt,
        asksDone: Int = 0,
    ) = InFlightOccurrence("r1@1", "r1", klass, at(firedAt), ladderStepsDone, persistent, at(lastAskedAt), asksDone)

    private fun plan(now: String, klass: ReminderClass, flight: InFlightOccurrence, config: FollowUpConfig? = followUp) =
        ReminderPlanner.plan(at(now), listOf(def("r1", klass, "2026-10-01T10:00")), emptyList(), inFlight = listOf(flight), followUp = config)

    @Test
    fun `kritik teslimden sonra siradaki merdiven basamagi alarm saatiyle kurulur`() {
        val result = plan("2026-10-08T10:00:30", ReminderClass.CRITICAL, inFlight(ReminderClass.CRITICAL, "2026-10-08T10:00"))

        val step = result.toSchedule.single()
        assertEquals("r1@1#l1", step.key)
        assertEquals(at("2026-10-08T10:02"), step.fireAt)
        assertEquals(AlarmApi.ALARM_CLOCK, step.api)
        assertTrue(result.dueNow.isEmpty())
    }

    @Test
    fun `merdiven bittiyse yeni basamak kurulmaz`() {
        val done = Ladder.stepsFor(ReminderClass.CRITICAL).size
        val result = plan("2026-10-08T10:11", ReminderClass.CRITICAL, inFlight(ReminderClass.CRITICAL, "2026-10-08T10:00", ladderStepsDone = done))

        assertTrue(result.toSchedule.isEmpty())
    }

    @Test
    fun `normal sinifta yarim saat sonraki tek tekrar kesin yolla kurulur`() {
        val result = plan("2026-10-08T10:05", ReminderClass.NORMAL, inFlight(ReminderClass.NORMAL, "2026-10-08T10:00"))

        val step = result.toSchedule.single()
        assertEquals(at("2026-10-08T10:30"), step.fireAt)
        assertEquals(AlarmApi.EXACT_IDLE, step.api)
    }

    @Test
    fun `vakti gecmis merdiven basamagi kurulmaz hemen teslim listesine girer`() {
        // Yeniden başlatma ortasında teslim: +2 dk basamağı telefon kapalıyken geçti.
        val result = plan("2026-10-08T10:04", ReminderClass.CRITICAL, inFlight(ReminderClass.CRITICAL, "2026-10-08T10:00"))

        assertTrue(result.toSchedule.isEmpty())
        assertEquals(listOf("r1@1#l1"), result.dueNow.map { it.key })
    }

    @Test
    fun `israrli takipte bir sonraki soru son sorudan yarim saat sonraya kesin yolla kurulur`() {
        val flight = inFlight(ReminderClass.NORMAL, "2026-10-08T10:00", persistent = true, lastAskedAt = "2026-10-08T11:00", asksDone = 2)

        val result = plan("2026-10-08T11:10", ReminderClass.NORMAL, flight)

        val ask = result.toSchedule.single()
        assertEquals("r1@1#f2", ask.key)
        assertEquals(at("2026-10-08T11:30"), ask.fireAt)
        assertEquals(AlarmApi.EXACT_IDLE, ask.api)
    }

    @Test
    fun `israrli takip kritik olmayan sinifta merdivenin tek tekrarinin yerini alir`() {
        val result = plan("2026-10-08T10:05", ReminderClass.NORMAL, inFlight(ReminderClass.NORMAL, "2026-10-08T10:00", persistent = true))

        assertEquals(listOf("r1@1#f0"), result.toSchedule.map { it.key })
    }

    @Test
    fun `israrli kritik iste merdiven de takip de kurulur ve takip kesin yoldadir`() {
        val result = plan("2026-10-08T10:00:30", ReminderClass.CRITICAL, inFlight(ReminderClass.CRITICAL, "2026-10-08T10:00", persistent = true))

        val byKey = result.toSchedule.associateBy { it.key }
        assertEquals(setOf("r1@1#l1", "r1@1#f0"), byKey.keys)
        assertEquals(AlarmApi.ALARM_CLOCK, byKey.getValue("r1@1#l1").api)
        assertEquals(AlarmApi.EXACT_IDLE, byKey.getValue("r1@1#f0").api)
    }

    @Test
    fun `uyku penceresine denk gelen soru sabah uyanisa kurulur`() {
        val flight = inFlight(ReminderClass.NORMAL, "2026-10-08T20:00", persistent = true, lastAskedAt = "2026-10-08T23:15", asksDone = 6)

        val result = plan("2026-10-08T23:20", ReminderClass.NORMAL, flight)

        assertEquals(at("2026-10-09T07:30"), result.toSchedule.single().fireAt)
    }

    @Test
    fun `vakti gecmis soru uyanikken hemen sorulur`() {
        val flight = inFlight(ReminderClass.NORMAL, "2026-10-08T10:00", persistent = true, lastAskedAt = "2026-10-08T10:30", asksDone = 1)

        val result = plan("2026-10-08T14:00", ReminderClass.NORMAL, flight)

        assertTrue(result.toSchedule.isEmpty())
        assertEquals(listOf("r1@1#f1"), result.dueNow.map { it.key })
    }

    @Test
    fun `vakti gecmis soru uykudayken hemen sorulmaz sabaha kurulur`() {
        val flight = inFlight(ReminderClass.NORMAL, "2026-10-08T20:00", persistent = true, lastAskedAt = "2026-10-08T22:00", asksDone = 4)

        val result = plan("2026-10-09T02:00", ReminderClass.NORMAL, flight)

        assertTrue(result.dueNow.isEmpty())
        assertEquals(at("2026-10-09T07:30"), result.toSchedule.single().fireAt)
    }

    @Test
    fun `sessizlik suresi takibi erteler ama kritik merdiveni ertelemez`() {
        // Kritik + odak oturumu çakışması: odak 11:00'e kadar susturur.
        val config = followUp.copy(silentUntil = at("2026-10-08T11:00"))
        val flight = inFlight(ReminderClass.CRITICAL, "2026-10-08T10:00", persistent = true)

        val result = plan("2026-10-08T10:00:30", ReminderClass.CRITICAL, flight, config)

        val byKey = result.toSchedule.associateBy { it.key }
        assertEquals(at("2026-10-08T10:02"), byKey.getValue("r1@1#l1").fireAt)
        assertEquals(at("2026-10-08T11:00"), byKey.getValue("r1@1#f0").fireAt)
    }

    @Test
    fun `takip ayari verilmezse israrli is icin soru kurulmaz`() {
        val result = plan("2026-10-08T10:05", ReminderClass.NORMAL, inFlight(ReminderClass.NORMAL, "2026-10-08T10:00", persistent = true), config = null)

        assertTrue(result.toSchedule.isEmpty())
    }

    @Test
    fun `son soru zamani degisince ayni anahtarli alarm yeni zamana guncellenir`() {
        val flight = inFlight(ReminderClass.NORMAL, "2026-10-08T10:00", persistent = true, lastAskedAt = "2026-10-08T10:05")
        val existing = listOf(ScheduledAlarm("r1@1#f0", at("2026-10-08T10:30")))

        val result = ReminderPlanner.plan(
            at("2026-10-08T10:06"), listOf(def("r1", ReminderClass.NORMAL, "2026-10-01T10:00")), existing,
            inFlight = listOf(flight), followUp = followUp,
        )

        assertEquals(at("2026-10-08T10:35"), result.toSchedule.single().fireAt)
        assertTrue(result.toCancel.isEmpty())
    }

    @Test
    fun `tanimi silinen isin merdiveni ve takibi kurulmaz mevcutlari iptal edilir`() {
        val flight = inFlight(ReminderClass.CRITICAL, "2026-10-08T10:00", persistent = true)
        val existing = listOf(ScheduledAlarm("r1@1#l1", at("2026-10-08T10:02")), ScheduledAlarm("r1@1#f0", at("2026-10-08T10:30")))

        val result = ReminderPlanner.plan(at("2026-10-08T10:01"), emptyList(), existing, inFlight = listOf(flight), followUp = followUp)

        assertTrue(result.toSchedule.isEmpty())
        assertEquals(setOf("r1@1#l1", "r1@1#f0"), result.toCancel.toSet())
    }

    @Test
    fun `ertelenen teslim kendi anahtariyla kurulur vakti gectiyse hemen teslim edilir`() {
        val defs = listOf(def("r1", ReminderClass.IMPORTANT, "2026-10-01T10:00"))
        val snooze = SnoozedDelivery(SnoozePolicy.snoozedKey("r1@1", 1), "r1", ReminderClass.IMPORTANT, at("2026-10-08T10:10"))

        val before = ReminderPlanner.plan(at("2026-10-08T10:01"), defs, emptyList(), snoozes = listOf(snooze))
        val after = ReminderPlanner.plan(at("2026-10-08T10:12"), defs, emptyList(), snoozes = listOf(snooze))

        assertEquals(listOf("r1@1#s1"), before.toSchedule.map { it.key })
        assertEquals(AlarmApi.EXACT_IDLE, before.toSchedule.single().api)
        assertTrue(after.toSchedule.isEmpty())
        assertEquals(listOf("r1@1#s1"), after.dueNow.map { it.key })
        assertFalse(after.isEmpty)
    }

    @Test
    fun `tanimi silinen isin ertelemesi kurulmaz ve hemen teslim de edilmez`() {
        val future = SnoozedDelivery("r1@1#s1", "r1", ReminderClass.NORMAL, at("2026-10-08T10:10"))
        val past = SnoozedDelivery("r1@1#s2", "r1", ReminderClass.NORMAL, at("2026-10-08T09:00"))

        val result = ReminderPlanner.plan(at("2026-10-08T10:01"), emptyList(), emptyList(), snoozes = listOf(future, past))

        assertTrue(result.isEmpty)
    }

    @Test
    fun `pencerenin otesine dusen takip sorusu ve erteleme kurulmaz`() {
        // Uzun sessizlik (3 gün) takibi 48 saatlik pencerenin dışına iter; sonraki bakımda kurulur.
        val config = followUp.copy(silentUntil = at("2026-10-11T12:00"))
        val flight = inFlight(ReminderClass.NORMAL, "2026-10-08T10:00", persistent = true)
        val farSnooze = SnoozedDelivery("r1@1#s1", "r1", ReminderClass.NORMAL, at("2026-10-12T10:00"))

        val result = ReminderPlanner.plan(
            at("2026-10-08T10:05"), listOf(def("r1", ReminderClass.NORMAL, "2026-10-01T10:00")), emptyList(),
            inFlight = listOf(flight), snoozes = listOf(farSnooze), followUp = config,
        )

        assertTrue(result.toSchedule.isEmpty())
        assertTrue(result.dueNow.isEmpty())
    }

    @Test
    fun `islenmis basamak sayisi en az bir olmalidir`() {
        assertThrows(IllegalArgumentException::class.java) {
            inFlight(ReminderClass.CRITICAL, "2026-10-08T10:00", ladderStepsDone = 0)
        }
    }
}
