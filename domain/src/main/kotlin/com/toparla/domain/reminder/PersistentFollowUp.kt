package com.toparla.domain.reminder

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Israrlı takip (karar 0003): Kullanıcı'nın üstlendiği iş "Yaptım" denene kadar belirli aralıkla
 * yeniden sorulur. Bu nesne yalnız "bir sonraki soru ne zaman?" sorusunu yanıtlar; saf fonksiyondur.
 *
 * Susturan durumlar: uyku penceresi (sabah uyanışta sürer) ve [silentUntil] (kriz/Bunaldım sonrası,
 * odak oturumu, Kullanıcı'nın bastığı "Bugün sessiz" / "2 saat sessiz" — çağıran birleştirip verir).
 */
object PersistentFollowUp {
    /**
     * @param lastAskedAt son sorunun (ya da ilk teslimin) anı
     * @param sleepStart uyku penceresinin başı (yerel); [sleepEnd] ile aynıysa pencere yoktur
     * @param silentUntil bu andan önce sorulmaz; yoksa null
     */
    fun nextAskAt(
        lastAskedAt: Instant,
        interval: Duration,
        zone: ZoneId,
        sleepStart: LocalTime,
        sleepEnd: LocalTime,
        silentUntil: Instant? = null,
    ): Instant {
        require(!interval.isNegative && !interval.isZero) { "interval pozitif olmalı" }
        var candidate = lastAskedAt.plus(interval)
        if (silentUntil != null && candidate.isBefore(silentUntil)) candidate = silentUntil
        return wakeIfAsleep(candidate, zone, sleepStart, sleepEnd)
    }

    /** An uyku penceresindeyse pencerenin bittiği ilk ana taşır. Pencere gece yarısını aşabilir. */
    private fun wakeIfAsleep(at: Instant, zone: ZoneId, sleepStart: LocalTime, sleepEnd: LocalTime): Instant {
        if (sleepStart == sleepEnd) return at
        val local = at.atZone(zone)
        val time = local.toLocalTime()
        val crossesMidnight = sleepStart.isAfter(sleepEnd)
        val asleep = if (crossesMidnight) {
            !time.isBefore(sleepStart) || time.isBefore(sleepEnd)
        } else {
            !time.isBefore(sleepStart) && time.isBefore(sleepEnd)
        }
        if (!asleep) return at
        val wakeDate = if (crossesMidnight && !time.isBefore(sleepStart)) local.toLocalDate().plusDays(1) else local.toLocalDate()
        return ZonedDateTime.of(wakeDate, sleepEnd, zone).toInstant()
    }
}
