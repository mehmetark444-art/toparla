package com.toparla.domain.reminder

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Tekrar kuralının veritabanındaki metin karşılığı. Biçim sabittir ve okunaklıdır:
 * `ONCE` · `DAILY` · `WEEKLY:MONDAY,FRIDAY` · `MONTHLY:31` · `HOURS:4:08:00:20:30`.
 */
object RecurrenceCodec {
    private const val ONCE = "ONCE"
    private const val DAILY = "DAILY"
    private const val WEEKLY = "WEEKLY"
    private const val MONTHLY = "MONTHLY"
    private const val HOURS = "HOURS"

    /** `HOURS:` sonrası: aralık, pencere başı, pencere sonu (`4:08:00:20:30`). */
    private val HOURS_PATTERN = Regex("""(\d+):(\d{2}:\d{2}):(\d{2}:\d{2})""")

    fun encode(rule: Recurrence): String = when (rule) {
        Recurrence.Once -> ONCE
        Recurrence.Daily -> DAILY
        is Recurrence.Weekly -> "$WEEKLY:${rule.days.sorted().joinToString(",")}"
        is Recurrence.MonthlyOnDay -> "$MONTHLY:${rule.day}"
        is Recurrence.EveryHours -> "$HOURS:${rule.hours}:${rule.windowStart}:${rule.windowEnd}"
    }

    /** Bozuk metinde null döner; çağıran kaydı geçersiz sayar (hatırlatma sessizce yanlış kurulmaz). */
    @Suppress("SwallowedException") // Bozuk metnin karşılığı bilerek null'dur; çağıran atlar ve günlüğe yazar.
    fun decode(text: String): Recurrence? = try {
        val head = text.substringBefore(':')
        val rest = text.substringAfter(':', "")
        when (head) {
            ONCE -> Recurrence.Once.takeIf { rest.isEmpty() }
            DAILY -> Recurrence.Daily.takeIf { rest.isEmpty() }
            WEEKLY -> Recurrence.Weekly(rest.split(',').map { DayOfWeek.valueOf(it) }.toSet())
            MONTHLY -> Recurrence.MonthlyOnDay(rest.toInt())
            HOURS -> HOURS_PATTERN.matchEntire(rest)?.destructured?.let { (hours, start, end) ->
                Recurrence.EveryHours(hours.toInt(), LocalTime.parse(start), LocalTime.parse(end))
            }
            else -> null
        }
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: DateTimeParseException) {
        null
    }
}
