package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** F2.7 (birleşik kart, geç teslim) ve F2.8 (teslim denetçisi, kritik bekçi, bakım) kuralları. */
class DeliveryRulesTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun alarm(id: String, klass: ReminderClass, fireAt: String) =
        PlannedAlarm(ReminderPlanner.keyOf(id, at(fireAt)), id, at(fireAt), klass, ReminderPlanner.apiFor(klass))

    @Test
    fun `ayni dakikadaki bes teslim tek kartta birlesir ve kart en onemli sinifi tasir`() {
        val due = listOf(
            alarm("a", ReminderClass.NORMAL, "2026-10-08T09:00:05"),
            alarm("b", ReminderClass.INFO, "2026-10-08T09:00:10"),
            alarm("c", ReminderClass.CRITICAL, "2026-10-08T09:00:40"),
            alarm("d", ReminderClass.IMPORTANT, "2026-10-08T09:00:00"),
            alarm("e", ReminderClass.NORMAL, "2026-10-08T09:00:59"),
        )

        val group = DeliveryGrouping.group(due).single()

        assertEquals(ReminderClass.CRITICAL, group.klass)
        assertEquals(listOf("c", "d", "a", "e", "b"), group.items.map { it.reminderId })
        assertEquals(at("2026-10-08T09:00"), group.minute)
    }

    @Test
    fun `farkli dakikalardaki teslimler ayri kartlardir ve zaman sirasiyla doner`() {
        val due = listOf(
            alarm("b", ReminderClass.NORMAL, "2026-10-08T09:01"),
            alarm("a", ReminderClass.NORMAL, "2026-10-08T09:00"),
        )

        val groups = DeliveryGrouping.group(due)

        assertEquals(listOf(listOf("a"), listOf("b")), groups.map { g -> g.items.map { it.reminderId } })
    }

    @Test
    fun `kart anahtari dakikadan uretilir ve girdi sirasindan bagimsizdir`() {
        val x = alarm("x", ReminderClass.NORMAL, "2026-10-08T09:00:10")
        val y = alarm("y", ReminderClass.IMPORTANT, "2026-10-08T09:00:20")

        val first = DeliveryGrouping.group(listOf(x, y)).single()
        val second = DeliveryGrouping.group(listOf(y, x)).single()

        assertEquals(first, second)
        assertEquals("g@${at("2026-10-08T09:00").toEpochMilli()}", first.key)
    }

    @Test
    fun `ayni anahtar iki kez gelirse kartta bir kez yer alir`() {
        val x = alarm("x", ReminderClass.NORMAL, "2026-10-08T09:00")

        assertEquals(1, DeliveryGrouping.group(listOf(x, x)).single().items.size)
    }

    @Test
    fun `bos girdi kart uretmez`() {
        assertTrue(DeliveryGrouping.group(emptyList()).isEmpty())
    }

    @Test
    fun `gec teslim toleransi bir dakikadir`() {
        val planned = at("2026-10-08T09:00")

        assertEquals(Duration.ZERO, DeliveryGrouping.lateBy(planned, at("2026-10-08T09:00:59")))
        assertEquals(Duration.ZERO, DeliveryGrouping.lateBy(planned, at("2026-10-08T09:01")))
        assertEquals(Duration.ofSeconds(61), DeliveryGrouping.lateBy(planned, at("2026-10-08T09:01:01")))
        // Erken ateşleme (saat geri alındı) gecikme değildir.
        assertEquals(Duration.ZERO, DeliveryGrouping.lateBy(planned, at("2026-10-08T08:59")))
    }

    @Test
    fun `denetci vakti gecmis ve ateslenme kaydi olmayan teslimleri bulur`() {
        val now = at("2026-10-08T12:00")
        val scheduled = listOf(
            ScheduledAlarm("fired", at("2026-10-08T09:00")),
            ScheduledAlarm("unfired-old", at("2026-10-08T10:00")),
            ScheduledAlarm("unfired-older", at("2026-10-08T08:00")),
            ScheduledAlarm("within-grace", at("2026-10-08T11:59:30")),
            ScheduledAlarm("future", at("2026-10-08T13:00")),
        )

        val found = DeliveryAuditor.findUnfired(now, scheduled, firedKeys = setOf("fired"))

        assertEquals(listOf("unfired-older", "unfired-old"), found.map { it.key })
    }

    @Test
    fun `bekci onumuzdeki yirmi dakikadaki kritik olayin alarmi yoksa kurdurur`() {
        val now = at("2026-10-08T12:00")
        val defs = listOf(
            ReminderDef("soon", ReminderClass.CRITICAL, LocalDateTime.parse("2026-10-08T12:15"), zone),
            ReminderDef("armed", ReminderClass.CRITICAL, LocalDateTime.parse("2026-10-08T12:10"), zone),
            ReminderDef("later", ReminderClass.CRITICAL, LocalDateTime.parse("2026-10-08T12:30"), zone),
            ReminderDef("normal", ReminderClass.NORMAL, LocalDateTime.parse("2026-10-08T12:05"), zone),
        )
        val armedKey = ReminderPlanner.keyOf("armed", at("2026-10-08T12:10"))

        val missing = CriticalWatchdog.missing(now, defs, listOf(ScheduledAlarm(armedKey, at("2026-10-08T12:10"))))

        assertEquals(listOf("soon"), missing.map { it.reminderId })
        assertEquals(AlarmApi.ALARM_CLOCK, missing.single().api)
    }

    @Test
    fun `bekci alarmi yanlis zamana kurulmus kritik olayi da bulur`() {
        val now = at("2026-10-08T12:00")
        val defs = listOf(ReminderDef("r", ReminderClass.CRITICAL, LocalDateTime.parse("2026-10-08T12:10"), zone))
        val drifted = listOf(ScheduledAlarm(ReminderPlanner.keyOf("r", at("2026-10-08T12:10")), at("2026-10-08T12:40")))

        assertEquals(1, CriticalWatchdog.missing(now, defs, drifted).size)
    }

    @Test
    fun `sonraki bakim en gec on iki saat sonradir`() {
        val now = at("2026-10-08T12:00")

        assertEquals(at("2026-10-09T00:00"), MaintenancePolicy.nextMaintenanceAt(now))
    }
}
