package com.toparla.domain.reminder

import com.toparla.domain.Defaults
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Aynı dakikada teslim edilecek hatırlatmaların tek kartı. Kart, içindeki en önemli sınıfın kanalından
 * ve merdiveninden gider; öğeler önem sırasına, sonra zamana göre dizilir.
 */
data class DeliveryGroup(val key: String, val minute: Instant, val klass: ReminderClass, val items: List<PlannedAlarm>)

/** Teslim anı kuralları: birleşik kart ve geç teslim (blueprint Bölüm I; karar 0003 "tek birleşik bildirim"). */
object DeliveryGrouping {
    val LATE_TOLERANCE: Duration = Duration.ofSeconds(Defaults.LATE_DELIVERY_TOLERANCE_SEC)

    /** Aynı dakikaya düşen teslimleri tek kartta toplar. Çıktı girdi sırasından bağımsızdır (idempotans). */
    fun group(due: List<PlannedAlarm>): List<DeliveryGroup> = due
        .distinctBy { it.key }
        .groupBy { it.fireAt.truncatedTo(ChronoUnit.MINUTES) }
        .toSortedMap()
        .map { (minute, alarms) ->
            val items = alarms.sortedWith(compareBy<PlannedAlarm> { it.klass }.thenBy { it.fireAt }.thenBy { it.key })
            DeliveryGroup("g@${minute.toEpochMilli()}", minute, items.first().klass, items)
        }

    /** Gecikme süresi; tolerans (±1 dk sözü) içindeyse ve erken ateşlemede sıfır. */
    fun lateBy(plannedAt: Instant, deliveredAt: Instant): Duration {
        val delay = Duration.between(plannedAt, deliveredAt)
        return if (delay > LATE_TOLERANCE) delay else Duration.ZERO
    }
}

/** Günlük teslim denetçisi (v3 §10.6-2): vakti geçmiş, ateşlenme kaydı olmayan teslimleri bulur. */
object DeliveryAuditor {
    /**
     * @param firedKeys `DeliveryLog`'da FIRED kaydı olan anahtarlar
     * @return eskiden yeniye sıralı; çağıran her biri için geç teslim yapar ve Sağlık uyarısı yazar
     */
    fun findUnfired(
        now: Instant,
        scheduled: List<ScheduledAlarm>,
        firedKeys: Set<String>,
        grace: Duration = DeliveryGrouping.LATE_TOLERANCE,
    ): List<ScheduledAlarm> = scheduled
        .filter { it.key !in firedKeys && it.fireAt.plus(grace).isBefore(now) }
        .sortedBy { it.fireAt }
}

/** Kritik bekçi (v3 §10.6-1): yakındaki kritik olayın sistem alarmı gerçekten kurulu mu? */
object CriticalWatchdog {
    val LOOKAHEAD: Duration = Duration.ofMinutes(Defaults.CRITICAL_WATCHDOG_LOOKAHEAD_MIN)

    /** Önümüzdeki [lookahead] içindeki kritik olaylardan alarmı olmayan ya da yanlış zamana kurulmuş olanlar. */
    fun missing(
        now: Instant,
        definitions: List<ReminderDef>,
        scheduled: List<ScheduledAlarm>,
        lookahead: Duration = LOOKAHEAD,
    ): List<PlannedAlarm> {
        val critical = definitions.filter { it.klass == ReminderClass.CRITICAL }
        return ReminderPlanner.plan(now, critical, scheduled, horizon = lookahead).toSchedule
    }
}

/** Bakım zamanlaması (v3 §10.2-5): pencere en geç 12 saatte bir yeniden doldurulur. */
object MaintenancePolicy {
    val INTERVAL: Duration = Duration.ofHours(Defaults.MAINTENANCE_INTERVAL_HOURS)

    fun nextMaintenanceAt(now: Instant): Instant = now.plus(INTERVAL)
}
