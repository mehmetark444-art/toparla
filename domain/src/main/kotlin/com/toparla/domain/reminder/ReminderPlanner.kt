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
 * Ana teslimlerin yanında, yanıt bekleyen teslimlerin sıradaki merdiven basamağını, ısrarlı takip
 * sorusunu (karar 0003) ve ertelenmiş teslimleri de planlar. Vakti geçmiş olan kurulmaz;
 * [PlanResult.dueNow] ile "hemen teslim et" diye döner.
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
        inFlight: List<InFlightOccurrence> = emptyList(),
        snoozes: List<SnoozedDelivery> = emptyList(),
        followUp: FollowUpConfig? = null,
    ): PlanResult {
        val windowEnd = now.plus(horizon)
        val active = definitions.filter { it.active }
        val activeIds = active.mapTo(HashSet()) { it.id }
        // Tanımı silinen ya da kapatılan işin merdiveni, takibi ve ertelemesi de düşer (invaryant).
        val extras = inFlight.filter { it.reminderId in activeIds }.flatMap { followUps(it, now, followUp) } +
            snoozes.filter { it.reminderId in activeIds }
                .map { PlannedAlarm(it.key, it.reminderId, it.fireAt, it.klass, apiFor(it.klass)) }
        val (due, upcoming) = extras.distinctBy { it.key }.partition { !it.fireAt.isAfter(now) }
        val planned = (active.flatMap { def -> occurrences(def, now, windowEnd) } + upcoming.filter { !it.fireAt.isAfter(windowEnd) })
            .distinctBy { it.key }
            .let { capToLimit(it, maxPending) }

        val existingByKey = existing.associateBy { it.key }
        val plannedKeys = planned.mapTo(HashSet()) { it.key }
        return PlanResult(
            // Anahtar zamanı içerdiği için normalde key eşitse fireAt de eşittir; karşılaştırma,
            // ScheduledAlarm tablosu sistemle tutarsız kalmışsa kendini onarmak içindir.
            toSchedule = planned.filter { existingByKey[it.key]?.fireAt != it.fireAt },
            // Vakti geçmiş kayda dokunulmaz: sistem alarmı zaten yoktur, kayıt ise teslim denetçisinin
            // kanıtıdır ([DeliveryAuditor]). Burada silinirse çalmamış teslim sessizce kaybolur.
            toCancel = existing.filter { it.fireAt.isAfter(now) }.map { it.key }.filterNot { it in plannedKeys },
            dueNow = due,
        )
    }

    fun ladderKey(occurrenceKey: String, stepIndex: Int): String = "$occurrenceKey#l$stepIndex"

    fun followUpKey(occurrenceKey: String, askIndex: Int): String = "$occurrenceKey#f$askIndex"

    /** Yanıt bekleyen teslim için sıradaki merdiven basamağı ve (ısrarlıysa) sıradaki soru. */
    private fun followUps(o: InFlightOccurrence, now: Instant, config: FollowUpConfig?): List<PlannedAlarm> {
        val out = ArrayList<PlannedAlarm>(2)
        val steps = Ladder.stepsFor(o.klass, config?.trustedContactEnabled ?: false)
        // Kritik olmayan ısrarlı işte merdivenin tek tekrarının yerini ısrarlı takip alır.
        val ladderApplies = o.klass == ReminderClass.CRITICAL || !o.persistent
        if (ladderApplies && o.ladderStepsDone < steps.size) {
            val fireAt = o.firedAt.plus(steps[o.ladderStepsDone].offset)
            out += PlannedAlarm(ladderKey(o.key, o.ladderStepsDone), o.reminderId, fireAt, o.klass, apiFor(o.klass))
        }
        if (o.persistent && config != null) {
            // Israrlı takip sınıftan bağımsız kesin yolla kurulur (karar 0006).
            out += PlannedAlarm(followUpKey(o.key, o.asksDone), o.reminderId, nextAsk(o, now, config), o.klass, AlarmApi.EXACT_IDLE)
        }
        return out
    }

    /**
     * Sıradaki sorunun anı. Vakti geçmişse (uygulama ölüydü, telefon kapalıydı) "şimdi sorulabilir mi"
     * diye yeniden bakılır: uyku ya da sessizlik sürüyorsa ilk uygun ana, değilse şimdiye döner.
     */
    private fun nextAsk(o: InFlightOccurrence, now: Instant, c: FollowUpConfig): Instant {
        val planned = PersistentFollowUp.nextAskAt(o.lastAskedAt, c.interval, c.zone, c.sleepStart, c.sleepEnd, c.silentUntil)
        if (planned.isAfter(now)) return planned
        return PersistentFollowUp.nextAskAt(now.minus(c.interval), c.interval, c.zone, c.sleepStart, c.sleepEnd, c.silentUntil)
    }

    fun keyOf(reminderId: String, plannedAt: Instant): String = "$reminderId@${plannedAt.toEpochMilli()}"

    /** Sınıf → alarm yolu. Kritik her zaman `setAlarmClock` (S0 spike 1 bulgusu). */
    fun apiFor(klass: ReminderClass): AlarmApi = when (klass) {
        ReminderClass.CRITICAL -> AlarmApi.ALARM_CLOCK
        // Esnek yol bu telefonda saatlerce kayıyor (karar 0006); yalnız zamanı önemsiz Bilgi sınıfında.
        ReminderClass.IMPORTANT, ReminderClass.NORMAL -> AlarmApi.EXACT_IDLE
        ReminderClass.INFO -> AlarmApi.INEXACT
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
