package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalTime

class RecurrenceCodecTest {
    private val samples = listOf(
        Recurrence.Once to "ONCE",
        Recurrence.Daily to "DAILY",
        Recurrence.Weekly(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)) to "WEEKLY:MONDAY,FRIDAY",
        Recurrence.MonthlyOnDay(31) to "MONTHLY:31",
        Recurrence.EveryHours(4, LocalTime.of(8, 0), LocalTime.of(20, 30)) to "HOURS:4:08:00:20:30",
    )

    @Test
    fun `her tekrar kurali metne ve metinden geri ayni sekilde doner`() {
        for ((rule, text) in samples) {
            assertEquals(text, RecurrenceCodec.encode(rule))
            assertEquals(rule, RecurrenceCodec.decode(text))
        }
    }

    @Test
    fun `bozuk ya da gecersiz metin null doner cokmeye yol acmaz`() {
        for (bad in listOf("", "YEARLY", "ONCE:1", "DAILY:x", "WEEKLY:", "WEEKLY:FUNDAY", "MONTHLY:0", "MONTHLY:x", "HOURS:0:08:00:20:00", "HOURS:4:20:00:08:00", "HOURS:4")) {
            assertNull(RecurrenceCodec.decode(bad), bad)
        }
    }
}
