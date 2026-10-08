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
    /** `HOURS:4:08:00:20:30` iki nokta ile bölününce 6 parça eder. */
    private const val HOURS_PARTS = 6

    fun encode(rule: Recurrence): String = when (rule) {
        Recurrence.Once -> ONCE
        Recurrence.Daily -> DAILY
        is Recurrence.Weekly -> "$WEEKLY:${rule.days.sorted().joinToString(",")}"
        is Recurrence.MonthlyOnDay -> "$MONTHLY:${rule.day}"
        is Recurrence.EveryHours -> "$HOURS:${rule.hours}:${rule.windowStart}:${rule.windowEnd}"
    }

    /** Bozuk metinde null döner; çağıran kaydı geçersiz sayar (hatırlatma sessizce yanlış kurulmaz). */
    fun decode(text: String): Recurrence? = try {
        val head = text.substringBefore(':')
        val rest = text.substringAfter(':', "")
        when (head) {
            ONCE -> Recurrence.Once.takeIf { rest.isEmpty() }
            DAILY -> Recurrence.Daily.takeIf { rest.isEmpty() }
            WEEKLY -> Recurrence.Weekly(rest.split(',').map { DayOfWeek.valueOf(it) }.toSet())
            MONTHLY -> Recurrence.MonthlyOnDay(rest.toInt())
            HOURS -> text.split(':').takeIf { it.size == HOURS_PARTS }?.let {
                Recurrence.EveryHours(it[1].toInt(), LocalTime.parse("${it[2]}:${it[3]}"), LocalTime.parse("${it[4]}:${it[5]}"))
            }
            else -> null
        }
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: DateTimeParseException) {
        null
    }
}
