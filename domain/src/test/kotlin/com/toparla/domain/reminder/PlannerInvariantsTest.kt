package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Random

/**
 * F2.9: Bölüm I invaryantları, tohumlu rastgele girdilerle (özellik tabanlı). Her deneme aynı tohumla
 * yeniden üretilebilir; hata mesajı tohumu taşır.
 */
class PlannerInvariantsTest {
    private val zones = listOf("Europe/Istanbul", "Europe/Berlin", "America/New_York", "Asia/Tokyo").map(ZoneId::of)
    private val trials = 300

    private fun randomDef(r: Random, index: Int, base: LocalDateTime): ReminderDef {
        val start = base.plusMinutes(r.nextInt(60 * 24 * 5).toLong() - 60L * 24 * 2)
        val recurrence = when (r.nextInt(5)) {
            0 -> Recurrence.Once
            1 -> Recurrence.Daily
            2 -> Recurrence.Weekly(DayOfWeek.entries.filter { r.nextBoolean() }.toSet().ifEmpty { setOf(DayOfWeek.MONDAY) })
            3 -> Recurrence.MonthlyOnDay(1 + r.nextInt(31))
            else -> Recurrence.EveryHours(1 + r.nextInt(6), LocalTime.of(8, 0), LocalTime.of(22, 0))
        }
        return ReminderDef(
            id = "r$index",
            klass = ReminderClass.entries[r.nextInt(ReminderClass.entries.size)],
            startLocal = start,
            zoneId = zones[r.nextInt(zones.size)],
            recurrence = recurrence,
            active = r.nextInt(10) != 0,
        )
    }

    private fun forEachTrial(body: (seed: Long, r: Random, now: Instant, defs: List<ReminderDef>) -> Unit) {
        for (seed in 1L..trials) {
            val r = Random(seed)
            val base = LocalDateTime.of(2026, 1 + r.nextInt(12), 1 + r.nextInt(28), r.nextInt(24), r.nextInt(60))
            val now = base.atZone(zones[0]).toInstant()
            val defs = List(1 + r.nextInt(40)) { randomDef(r, it, base) }
            body(seed, r, now, defs)
        }
    }

    @Test
    fun `ayni anahtar icin iki alarm yoktur`() = forEachTrial { seed, _, now, defs ->
        val keys = ReminderPlanner.plan(now, defs, emptyList()).toSchedule.map { it.key }
        assertEquals(keys.size, keys.toSet().size, "tohum $seed")
    }

    @Test
    fun `gecmis an kurulmaz ve pencere disi kurulmaz`() = forEachTrial { seed, _, now, defs ->
        val windowEnd = now.plus(ReminderPlanner.DEFAULT_HORIZON)
        ReminderPlanner.plan(now, defs, emptyList()).toSchedule.forEach {
            assertTrue(it.fireAt.isAfter(now), "tohum $seed: geçmiş ${it.key}")
            assertTrue(!it.fireAt.isAfter(windowEnd), "tohum $seed: pencere dışı ${it.key}")
        }
    }

    @Test
    fun `sinir ne kadar dar olursa olsun penceredeki her kritik olay kurulur`() = forEachTrial { seed, r, now, defs ->
        val unlimited = ReminderPlanner.plan(now, defs, emptyList(), maxPending = Int.MAX_VALUE).toSchedule
        val capped = ReminderPlanner.plan(now, defs, emptyList(), maxPending = r.nextInt(10)).toSchedule.mapTo(HashSet()) { it.key }

        unlimited.filter { it.klass == ReminderClass.CRITICAL }.forEach {
            assertTrue(it.key in capped, "tohum $seed: kritik düştü ${it.key}")
        }
    }

    @Test
    fun `sinir asilinca once en onemsiz sinif duser`() = forEachTrial { seed, r, now, defs ->
        val all = ReminderPlanner.plan(now, defs, emptyList(), maxPending = Int.MAX_VALUE).toSchedule
        val limit = r.nextInt(all.size + 1)
        val kept = ReminderPlanner.plan(now, defs, emptyList(), maxPending = limit).toSchedule
        val keptKeys = kept.mapTo(HashSet()) { it.key }
        val leastImportantKept = kept.maxOfOrNull { it.klass } ?: return@forEachTrial

        // Düşen hiçbir teslim, tutulanların en önemsizinden daha önemli olamaz.
        all.filter { it.key !in keptKeys }.forEach {
            assertTrue(it.klass >= leastImportantKept, "tohum $seed: ${it.klass} düştü, $leastImportantKept tutuldu")
        }
    }

    @Test
    fun `plan uygulaninca ikinci planlama bos doner`() = forEachTrial { seed, _, now, defs ->
        val first = ReminderPlanner.plan(now, defs, emptyList())
        val applied = first.toSchedule.map { ScheduledAlarm(it.key, it.fireAt) }

        assertTrue(ReminderPlanner.plan(now, defs, applied).isEmpty, "tohum $seed")
    }

    @Test
    fun `tanim silinince o tanimin butun alarmlari iptal edilir`() = forEachTrial { seed, r, now, defs ->
        val applied = ReminderPlanner.plan(now, defs, emptyList()).toSchedule
        val removed = defs[r.nextInt(defs.size)].id
        val expected = applied.filter { it.reminderId == removed }.mapTo(HashSet()) { it.key }

        val result = ReminderPlanner.plan(now, defs.filterNot { it.id == removed }, applied.map { ScheduledAlarm(it.key, it.fireAt) })

        assertEquals(expected, result.toCancel.toSet(), "tohum $seed")
        assertTrue(result.toSchedule.none { it.reminderId == removed }, "tohum $seed")
    }

    @Test
    fun `yanit bekleyen teslimlerin ek alarmlari da gecmise ve pencere disina kurulmaz`() = forEachTrial { seed, r, now, defs ->
        val config = FollowUpConfig(Duration.ofMinutes(30), zones[0], LocalTime.of(23, 30), LocalTime.of(7, 30))
        val inFlight = defs.filter { it.active }.take(5).mapIndexed { i, d ->
            val fired = now.minus(Duration.ofMinutes(r.nextInt(600).toLong()))
            InFlightOccurrence("${d.id}@x$i", d.id, d.klass, fired, 1 + r.nextInt(4), r.nextBoolean(), fired, r.nextInt(5))
        }
        val result = ReminderPlanner.plan(now, defs, emptyList(), inFlight = inFlight, followUp = config)
        val windowEnd = now.plus(ReminderPlanner.DEFAULT_HORIZON)

        result.toSchedule.forEach { assertTrue(it.fireAt.isAfter(now) && !it.fireAt.isAfter(windowEnd), "tohum $seed: ${it.key}") }
        result.dueNow.forEach { assertTrue(!it.fireAt.isAfter(now), "tohum $seed: ${it.key}") }
        val keys = (result.toSchedule + result.dueNow).map { it.key }
        assertEquals(keys.size, keys.toSet().size, "tohum $seed")
    }

    /** Bir yıl boyunca 12 saatlik bakım adımlarıyla planlayıp kurulan tekil teslimleri sayar. */
    private fun simulateYear(def: ReminderDef): Int {
        var now = LocalDateTime.of(2026, 1, 1, 0, 0).atZone(def.zoneId).toInstant().minusSeconds(1)
        val end = LocalDateTime.of(2027, 1, 1, 0, 0).atZone(def.zoneId).toInstant().minusSeconds(1)
        val seen = HashSet<String>()
        while (now.isBefore(end)) {
            ReminderPlanner.plan(now, listOf(def), emptyList()).toSchedule.filter { !it.fireAt.isAfter(end) }.forEach { seen += it.key }
            now = now.plus(Duration.ofHours(12))
        }
        return seen.size
    }

    @Test
    fun `bir yillik simulasyon beklenen sayida olay uretir`() {
        val berlin = ZoneId.of("Europe/Berlin") // yaz saati geçişi olan dilim
        val start = LocalDateTime.of(2026, 1, 1, 2, 30) // 29 Mart'ta atlanan, 25 Ekim'de tekrarlanan yerel saat
        fun def(recurrence: Recurrence) = ReminderDef("y", ReminderClass.CRITICAL, start, berlin, recurrence)

        assertEquals(365, simulateYear(def(Recurrence.Daily)))
        assertEquals(52, simulateYear(def(Recurrence.Weekly(setOf(DayOfWeek.MONDAY))))) // 2026'da 52 pazartesi
        assertEquals(12, simulateYear(def(Recurrence.MonthlyOnDay(31)))) // kısa aylarda ay sonuna çekilir
        assertEquals(1, simulateYear(def(Recurrence.Once)))
        // 08:00–20:00 arası 4 saatte bir: 08, 12, 16, 20 → günde 4
        assertEquals(365 * 4, simulateYear(def(Recurrence.EveryHours(4, LocalTime.of(8, 0), LocalTime.of(20, 0)))))
    }
}
