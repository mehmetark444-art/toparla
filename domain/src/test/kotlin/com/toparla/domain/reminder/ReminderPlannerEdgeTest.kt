package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlannerEdgeTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun `tabloda ayni anahtar yanlis zamanla duruyorsa alarm yeniden kurulur`() {
        val now = at("2026-10-07T12:00")
        val defs = listOf(ReminderDef("r1", ReminderClass.CRITICAL, LocalDateTime.parse("2026-10-07T15:00"), zone))
        val planned = ReminderPlanner.plan(now, defs, emptyList()).toSchedule.single()
        val drifted = listOf(ScheduledAlarm(planned.key, planned.fireAt.plusSeconds(600)))

        val result = ReminderPlanner.plan(now, defs, drifted)

        assertEquals(listOf(planned), result.toSchedule)
        assertTrue(result.toCancel.isEmpty())
    }

    @Test
    fun `vakti gecmis kayit planlayici tarafindan iptal edilmez denetciye kalir`() {
        // Telefon kapalıyken vakti geçen alarmın kaydı silinirse denetçi onu bulamaz ve teslim sessizce kaybolur.
        val now = at("2026-10-07T12:00")
        val defs = listOf(ReminderDef("r1", ReminderClass.CRITICAL, LocalDateTime.parse("2026-10-07T11:00"), zone))
        val overdue = ScheduledAlarm(ReminderPlanner.keyOf("r1", at("2026-10-07T11:00")), at("2026-10-07T11:00"))
        val staleFuture = ScheduledAlarm("silinmis@1", at("2026-10-07T13:00"))

        val result = ReminderPlanner.plan(now, defs, listOf(overdue, staleFuture))

        assertEquals(listOf("silinmis@1"), result.toCancel)
        assertEquals(listOf(overdue), DeliveryAuditor.findUnfired(now, listOf(overdue, staleFuture), firedKeys = emptySet()))
    }

    @Test
    fun `anahtar hatirlatma kimligi ve planlanan andan olusur`() {
        assertEquals("r1@1000", ReminderPlanner.keyOf("r1", Instant.ofEpochMilli(1000)))
    }

    @Test
    fun `gecersiz tekrar kurallari reddedilir`() {
        val nine = LocalTime.of(9, 0)
        val five = LocalTime.of(17, 0)
        assertThrows(IllegalArgumentException::class.java) { Recurrence.Weekly(emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { Recurrence.MonthlyOnDay(0) }
        assertThrows(IllegalArgumentException::class.java) { Recurrence.MonthlyOnDay(32) }
        assertThrows(IllegalArgumentException::class.java) { Recurrence.EveryHours(0, nine, five) }
        assertThrows(IllegalArgumentException::class.java) { Recurrence.EveryHours(24, nine, five) }
        assertThrows(IllegalArgumentException::class.java) { Recurrence.EveryHours(2, five, nine) }
    }

    @Test
    fun `tek noktali pencere gunde bir teslim uretir`() {
        val nine = LocalTime.of(9, 0)
        val defs = listOf(
            ReminderDef("r1", ReminderClass.NORMAL, LocalDateTime.parse("2026-10-01T09:00"), zone, Recurrence.EveryHours(3, nine, nine)),
        )
        val result = ReminderPlanner.plan(at("2026-10-07T12:00"), defs, emptyList())
        assertEquals(listOf(at("2026-10-08T09:00"), at("2026-10-09T09:00")), result.toSchedule.map { it.fireAt })
    }
}
