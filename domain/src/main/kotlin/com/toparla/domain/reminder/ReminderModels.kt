package com.toparla.domain.reminder

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Hatırlatma sınıfı (blueprint M6). Sıra önem sırasıdır: en önemli en başta. */
enum class ReminderClass { CRITICAL, IMPORTANT, NORMAL, INFO }

/** Bir teslimin kurulacağı sistem alarm yolu (blueprint G1). */
enum class AlarmApi { ALARM_CLOCK, EXACT_IDLE, INEXACT }

/** Tekrar kuralı (blueprint Bölüm I). Saatler yereldir; anlık zamana `zoneId` ile çevrilir. */
sealed interface Recurrence {
    data object Once : Recurrence

    data object Daily : Recurrence

    data class Weekly(val days: Set<DayOfWeek>) : Recurrence {
        init {
            require(days.isNotEmpty()) { "Weekly en az bir gün ister" }
        }
    }

    /** Ayın günü. Kısa aylarda ayın son gününe çekilir (31 → Şubat'ta 28/29). */
    data class MonthlyOnDay(val day: Int) : Recurrence {
        init {
            require(day in 1..31) { "MonthlyOnDay günü 1..31 olmalı: $day" }
        }
    }

    /** Her gün [windowStart]'tan başlayıp [windowEnd]'i geçmeyecek şekilde her [hours] saatte bir. */
    data class EveryHours(val hours: Int, val windowStart: LocalTime, val windowEnd: LocalTime) : Recurrence {
        init {
            require(hours in 1..23) { "EveryHours aralığı 1..23 saat olmalı: $hours" }
            require(!windowEnd.isBefore(windowStart)) { "EveryHours penceresi gün içinde olmalı" }
        }
    }
}

/** Hatırlatma tanımı: planlayıcının girdisi. */
data class ReminderDef(
    val id: String,
    val klass: ReminderClass,
    val startLocal: LocalDateTime,
    val zoneId: ZoneId,
    val recurrence: Recurrence = Recurrence.Once,
    val active: Boolean = true,
)

/** Planlanan tek bir teslim. [key] idempotans anahtarıdır: reminderId + plannedAt. */
data class PlannedAlarm(
    val key: String,
    val reminderId: String,
    val fireAt: Instant,
    val klass: ReminderClass,
    val api: AlarmApi,
)

/** Sistemde hâlihazırda kurulu alarmın kaydı (`ScheduledAlarm` tablosunun planlayıcıya bakan yüzü). */
data class ScheduledAlarm(val key: String, val fireAt: Instant)

/** Planlayıcı çıktısı: kurulacaklar ve iptal edilecek anahtarlar. */
data class PlanResult(val toSchedule: List<PlannedAlarm>, val toCancel: List<String>) {
    val isEmpty: Boolean get() = toSchedule.isEmpty() && toCancel.isEmpty()
}
