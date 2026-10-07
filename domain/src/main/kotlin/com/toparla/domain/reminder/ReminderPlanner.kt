package com.toparla.domain.reminder

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Hatırlatma planlayıcısı: saf fonksiyon (blueprint Bölüm I). Girdiler yalnız veridir; şimdiki
 * zaman dışarıdan gelir. Android'e ve saate dokunmaz.
 *
 * Bu ilk kesit yalnız ana teslimleri planlar; merdiven basamakları ve ısrarlı takip (karar 0003)
 * ayrı kesitlerde eklenir.
 */
object ReminderPlanner {
    val DEFAULT_HORIZON: Duration = Duration.ofHours(48)

    /** Uygulama başına bekleyen alarm üst sınırı (blueprint Bölüm I, pencere doldurma 4). */
    const val DEFAULT_MAX_PENDING = 200

    fun plan(
        now: Instant,
        definitions: List<ReminderDef>,
        existing: List<ScheduledAlarm>,
        horizon: Duration = DEFAULT_HORIZON,
        maxPending: Int = DEFAULT_MAX_PENDING,
    ): PlanResult {
        val windowEnd = now.plus(horizon)
        val planned = definitions
            .filter { it.active }
            .flatMap { def -> occurrences(def, now, windowEnd) }
            .let { capToLimit(it, maxPending) }

        val existingByKey = existing.associateBy { it.key }
        val plannedKeys = planned.mapTo(HashSet()) { it.key }
        return PlanResult(
            toSchedule = planned.filter { existingByKey[it.key]?.fireAt != it.fireAt },
            toCancel = existing.map { it.key }.filterNot { it in plannedKeys },
        )
    }

    fun keyOf(reminderId: String, plannedAt: Instant): String = "$reminderId@${plannedAt.toEpochMilli()}"

    /** Sınıf → alarm yolu. Kritik her zaman `setAlarmClock` (S0 spike 1 bulgusu). */
    fun apiFor(klass: ReminderClass): AlarmApi = when (klass) {
        ReminderClass.CRITICAL -> AlarmApi.ALARM_CLOCK
        ReminderClass.IMPORTANT -> AlarmApi.EXACT_IDLE
        ReminderClass.NORMAL, ReminderClass.INFO -> AlarmApi.INEXACT
    }

    /** (now, windowEnd] aralığındaki teslimler. Geçmiş an kurulmaz. */
    private fun occurrences(def: ReminderDef, now: Instant, windowEnd: Instant): List<PlannedAlarm> {
        val firstDay = maxOf(def.startLocal.toLocalDate(), now.atZone(def.zoneId).toLocalDate().minusDays(1))
        val lastDay = windowEnd.atZone(def.zoneId).toLocalDate().plusDays(1)
        return localTimes(def, firstDay, lastDay)
            .filter { !it.isBefore(def.startLocal) }
            .map { toInstant(it, def.zoneId) }
            .filter { it.isAfter(now) && !it.isAfter(windowEnd) }
            .distinct()
            .map { PlannedAlarm(keyOf(def.id, it), def.id, it, def.klass, apiFor(def.klass)) }
            .toList()
    }

    private fun localTimes(def: ReminderDef, firstDay: LocalDate, lastDay: LocalDate): Sequence<LocalDateTime> {
        val time = def.startLocal.toLocalTime()
        val days = generateSequence(firstDay) { it.plusDays(1) }.takeWhile { !it.isAfter(lastDay) }
        return when (val rule = def.recurrence) {
            Recurrence.Once -> sequenceOf(def.startLocal)
            Recurrence.Daily -> days.map { it.atTime(time) }
            is Recurrence.Weekly -> days.filter { it.dayOfWeek in rule.days }.map { it.atTime(time) }
            is Recurrence.MonthlyOnDay ->
                days.filter { it.dayOfMonth == minOf(rule.day, it.lengthOfMonth()) }.map { it.atTime(time) }
            is Recurrence.EveryHours -> days.flatMap { day ->
                generateSequence(rule.windowStart) { t ->
                    // Gece yarısını aşan adım pencereyi bitirir.
                    t.plusHours(rule.hours.toLong()).takeIf { it.isAfter(t) }
                }.takeWhile { !it.isAfter(rule.windowEnd) }.map { day.atTime(it) }
            }
        }
    }

    /**
     * Yerel saati anlık zamana çevirir. Atlanan yerel saat (ileri alma boşluğu) ilk geçerli ana
     * kaydırılır; tekrarlanan yerel saatte (geri alma) ilk geçiş kullanılır.
     */
    private fun toInstant(local: LocalDateTime, zone: ZoneId): Instant {
        val transition = zone.rules.getTransition(local)
        return if (transition != null && transition.isGap) {
            transition.instant
        } else {
            ZonedDateTime.ofLocal(local, zone, null).toInstant()
        }
    }

    /**
     * Sınır aşılırsa pencere önce Bilgi, sonra Normal, sonra Önemli sınıf için en uzak teslimlerden
     * başlayarak daralır. Kritik asla düşürülmez.
     */
    private fun capToLimit(planned: List<PlannedAlarm>, maxPending: Int): List<PlannedAlarm> {
        if (planned.size <= maxPending) return planned
        val (critical, rest) = planned.partition { it.klass == ReminderClass.CRITICAL }
        val room = (maxPending - critical.size).coerceAtLeast(0)
        val kept = rest.sortedWith(compareBy<PlannedAlarm> { it.klass }.thenBy { it.fireAt }).take(room)
        return critical + kept
    }
}
