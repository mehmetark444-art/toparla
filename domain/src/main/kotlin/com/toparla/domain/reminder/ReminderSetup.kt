package com.toparla.domain.reminder

import com.toparla.domain.Defaults
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Hatırlatma eklerken sunulan hazır zamanlar (taslak onayı 9 Ekim 2026). */
enum class QuickTime {
    IN_10_MIN,
    IN_1_HOUR,
    TONIGHT,
    TOMORROW_MORNING,
    ;

    fun resolve(now: Instant, zone: ZoneId): LocalDateTime {
        val local = LocalDateTime.ofInstant(now, zone)
        val evening = LocalTime.of(Defaults.QUICK_TIME_EVENING_HOUR, 0)
        return when (this) {
            IN_10_MIN -> local.plusMinutes(Defaults.QUICK_TIME_SOON_MIN)
            IN_1_HOUR -> local.plusHours(1)
            // Akşam saati geldiyse ya da geçtiyse "bu akşam" artık yarın akşamdır.
            TONIGHT -> if (local.toLocalTime().isBefore(evening)) local.toLocalDate().atTime(evening) else local.toLocalDate().plusDays(1).atTime(evening)
            TOMORROW_MORNING -> local.toLocalDate().plusDays(1).atTime(Defaults.QUICK_TIME_MORNING_HOUR, 0)
        }
    }
}

/** Kaydedilmeye hazır, doğrulanmış hatırlatma taslağı. */
data class ReminderDraft(val title: String, val startLocal: LocalDateTime) {
    companion object {
        const val MAX_TITLE = 80

        /** Başlık kırpılır ve sınırlanır; boş başlık ya da geçmiş an taslak üretmez (null). */
        fun validate(title: String, startLocal: LocalDateTime, now: Instant, zone: ZoneId): ReminderDraft? {
            val clean = title.trim().take(MAX_TITLE)
            val inFuture = startLocal.atZone(zone).toInstant().isAfter(now)
            return if (clean.isNotEmpty() && inFuture) ReminderDraft(clean, startLocal) else null
        }
    }
}
