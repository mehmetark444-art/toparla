package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")
    private val berlin = ZoneId.of("Europe/Berlin")

    private fun at(local: String, zone: ZoneId = istanbul): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun def(
        id: String = "r1",
        start: String,
        recurrence: Recurrence = Recurrence.Once,
        klass: ReminderClass = ReminderClass.CRITICAL,
        zone: ZoneId = istanbul,
        active: Boolean = true,
    ) = ReminderDef(id, klass, LocalDateTime.parse(start), zone, recurrence, active)

    private fun plan(now: Instant, defs: List<ReminderDef>, existing: List<ScheduledAlarm> = emptyList(), max: Int = 200) =
        ReminderPlanner.plan(now, defs, existing, maxPending = max)

    private fun PlanResult.asExisting() = toSchedule.map { ScheduledAlarm(it.key, it.fireAt) }

    @Test
    fun `gelecekteki tek seferlik hatirlatma kurulur`() {
        val result = plan(at("2026-10-07T12:00"), listOf(def(start = "2026-10-07T15:00")))
        assertEquals(listOf(at("2026-10-07T15:00")), result.toSchedule.map { it.fireAt })
        assertEquals(AlarmApi.ALARM_CLOCK, result.toSchedule.single().api)
    }

    @Test
    fun `gecmis an kurulmaz`() {
        val result = plan(at("2026-10-07T12:00"), listOf(def(start = "2026-10-07T11:59")))
        assertTrue(result.toSchedule.isEmpty())
    }

    @Test
    fun `pencere disi kurulmaz`() {
        val result = plan(at("2026-10-07T12:00"), listOf(def(start = "2026-10-09T12:01")))
        assertTrue(result.toSchedule.isEmpty())
    }

    @Test
    fun `gunluk tekrar 48 saatte iki teslim uretir`() {
        val result = plan(at("2026-10-07T12:00"), listOf(def(start = "2026-10-01T08:00", recurrence = Recurrence.Daily)))
        assertEquals(listOf(at("2026-10-08T08:00"), at("2026-10-09T08:00")), result.toSchedule.map { it.fireAt })
    }

    @Test
    fun `tekrar baslangic tarihinden once teslim uretmez`() {
        val result = plan(at("2026-10-07T12:00"), listOf(def(start = "2026-10-09T08:00", recurrence = Recurrence.Daily)))
        assertEquals(listOf(at("2026-10-09T08:00")), result.toSchedule.map { it.fireAt })
    }

    @Test
    fun `ayni girdiyle ikinci planlama bos doner`() {
        val now = at("2026-10-07T12:00")
        val defs = listOf(def(start = "2026-10-01T08:00", recurrence = Recurrence.Daily))
        val first = plan(now, defs)
        assertTrue(plan(now, defs, first.asExisting()).isEmpty)
    }

    @Test
    fun `ayni key icin iki alarm uretilmez`() {
        val defs = listOf(
            def(start = "2026-10-01T08:00", recurrence = Recurrence.EveryHours(1, LocalTime.of(8, 0), LocalTime.of(22, 0))),
        )
        val keys = plan(at("2026-10-07T12:00"), defs).toSchedule.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `tanim silinince alarmlari iptal edilir`() {
        val now = at("2026-10-07T12:00")
        val first = plan(now, listOf(def(start = "2026-10-01T08:00", recurrence = Recurrence.Daily)))
        val second = plan(now, emptyList(), first.asExisting())
        assertEquals(first.toSchedule.map { it.key }, second.toCancel)
        assertTrue(second.toSchedule.isEmpty())
    }

    @Test
    fun `pasif tanim alarmlarini iptal eder`() {
        val now = at("2026-10-07T12:00")
        val active = def(start = "2026-10-07T15:00")
        val first = plan(now, listOf(active))
        val second = plan(now, listOf(active.copy(active = false)), first.asExisting())
        assertEquals(1, second.toCancel.size)
    }

    @Test
    fun `saati degisen tanim eskiyi iptal eder yeniyi kurar`() {
        val now = at("2026-10-07T12:00")
        val first = plan(now, listOf(def(start = "2026-10-07T15:00")))
        val second = plan(now, listOf(def(start = "2026-10-07T16:00")), first.asExisting())
        assertEquals(listOf(at("2026-10-07T16:00")), second.toSchedule.map { it.fireAt })
        assertEquals(first.toSchedule.map { it.key }, second.toCancel)
    }

    @Test
    fun `haftalik tekrar yalniz secili gunlerde`() {
        // 7 Ekim 2026 Çarşamba; pencere Cuma 12:00'ye kadar.
        val days = setOf(DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
        val result = plan(at("2026-10-07T12:00"), listOf(def(start = "2026-10-01T09:00", recurrence = Recurrence.Weekly(days))))
        assertEquals(listOf(at("2026-10-08T09:00")), result.toSchedule.map { it.fireAt })
    }

    @Test
    fun `ayin 31i kisa ayda son gune cekilir`() {
        val result = plan(at("2027-02-27T12:00"), listOf(def(start = "2026-10-31T09:00", recurrence = Recurrence.MonthlyOnDay(31))))
        assertEquals(listOf(at("2027-02-28T09:00")), result.toSchedule.map { it.fireAt })
    }

    @Test
    fun `her x saatte bir pencere icinde kalir`() {
        val rule = Recurrence.EveryHours(4, LocalTime.of(9, 0), LocalTime.of(21, 0))
        val result = plan(at("2026-10-07T23:00"), listOf(def(start = "2026-10-01T09:00", recurrence = rule)))
        val expected = listOf(
            "2026-10-08T09:00", "2026-10-08T13:00", "2026-10-08T17:00", "2026-10-08T21:00",
            "2026-10-09T09:00", "2026-10-09T13:00", "2026-10-09T17:00", "2026-10-09T21:00",
        ).map { at(it) }
        assertEquals(expected, result.toSchedule.map { it.fireAt })
    }

    @Test
    fun `gece yarisi ve yil sonu gecisi`() {
        val result = plan(at("2026-12-31T23:30"), listOf(def(start = "2026-12-01T00:15", recurrence = Recurrence.Daily)))
        assertEquals(listOf(at("2027-01-01T00:15"), at("2027-01-02T00:15")), result.toSchedule.map { it.fireAt })
    }

    @Test
    fun `atlanan yerel saat ilk gecerli ana kayar`() {
        // Berlin 28 Mart 2027: 02:00 → 03:00. 02:30 yoktur; teslim 03:00 yerel (01:00 UTC).
        val result = plan(at("2027-03-27T12:00", berlin), listOf(def(start = "2027-03-28T02:30", zone = berlin)))
        assertEquals(listOf(Instant.parse("2027-03-28T01:00:00Z")), result.toSchedule.map { it.fireAt })
    }

    @Test
    fun `tekrarlanan yerel saatte ilk gecis kullanilir`() {
        // Berlin 31 Ekim 2027: 03:00 → 02:00. 02:30 iki kez yaşanır; ilki yaz saatiyle (00:30 UTC).
        val result = plan(at("2027-10-30T12:00", berlin), listOf(def(start = "2027-10-31T02:30", zone = berlin)))
        assertEquals(listOf(Instant.parse("2027-10-31T00:30:00Z")), result.toSchedule.map { it.fireAt })
    }

    @Test
    fun `sinir asilinca kritik dusmez once bilgi ve en uzak olan duser`() {
        val hourly = Recurrence.EveryHours(1, LocalTime.of(0, 0), LocalTime.of(23, 0))
        val defs = listOf(
            def(id = "kritik", start = "2026-10-01T00:00", recurrence = hourly, klass = ReminderClass.CRITICAL),
            def(id = "normal", start = "2026-10-01T00:30", recurrence = Recurrence.Daily, klass = ReminderClass.NORMAL),
            def(id = "bilgi", start = "2026-10-01T00:45", recurrence = Recurrence.Daily, klass = ReminderClass.INFO),
        )
        val now = at("2026-10-07T12:00")
        val full = plan(now, defs)
        val criticalCount = full.toSchedule.count { it.klass == ReminderClass.CRITICAL }
        assertEquals(48, criticalCount)

        val capped = plan(now, defs, max = criticalCount + 3).toSchedule
        assertEquals(criticalCount, capped.count { it.klass == ReminderClass.CRITICAL })
        assertEquals(2, capped.count { it.klass == ReminderClass.NORMAL })
        assertEquals(listOf(at("2026-10-08T00:45")), capped.filter { it.klass == ReminderClass.INFO }.map { it.fireAt })
    }

    @Test
    fun `sinir kritik sayisindan kucukse bile kritiklerin hepsi kalir`() {
        val hourly = Recurrence.EveryHours(1, LocalTime.of(0, 0), LocalTime.of(23, 0))
        val defs = listOf(def(start = "2026-10-01T00:00", recurrence = hourly))
        assertEquals(48, plan(at("2026-10-07T12:00"), defs, max = 10).toSchedule.size)
    }

    @Test
    fun `sinif alarm yoluna eslenir`() {
        assertEquals(AlarmApi.ALARM_CLOCK, ReminderPlanner.apiFor(ReminderClass.CRITICAL))
        assertEquals(AlarmApi.EXACT_IDLE, ReminderPlanner.apiFor(ReminderClass.IMPORTANT))
        assertEquals(AlarmApi.EXACT_IDLE, ReminderPlanner.apiFor(ReminderClass.NORMAL))
        assertEquals(AlarmApi.INEXACT, ReminderPlanner.apiFor(ReminderClass.INFO))
    }

    @Test
    fun `bir yillik gunluk bakim simulasyonu her gune tam bir teslim uretir`() {
        // Günlük bakım: her gün yeniden planla; yaz saati olan dilimde, atlanan saate denk gelen tanımla.
        val definition = listOf(def(start = "2027-01-01T02:30", recurrence = Recurrence.Daily, zone = berlin))
        var now = at("2026-12-31T12:00", berlin)
        val seenKeys = HashSet<String>()
        val days = ArrayList<LocalDate>()
        repeat(365) {
            ReminderPlanner.plan(now, definition, emptyList()).toSchedule.forEach {
                if (seenKeys.add(it.key)) days.add(it.fireAt.atZone(berlin).toLocalDate())
            }
            now = now.plus(Duration.ofHours(24))
        }
        val year2027 = generateSequence(LocalDate.of(2027, 1, 1)) { it.plusDays(1) }.take(365).toList()
        assertEquals(year2027, days.take(365))
        assertEquals(days.size, days.toSet().size)
    }
}
