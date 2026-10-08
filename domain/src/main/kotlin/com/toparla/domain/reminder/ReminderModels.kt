package com.toparla.domain.reminder

import java.time.DayOfWeek
import java.time.Duration
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

/**
 * Teslim edilmiş ama henüz çözülmemiş (yanıt bekleyen) teslim. Planlayıcı buna bakarak sıradaki merdiven
 * basamağını ve ısrarlı takip sorusunu kurar; böylece yeniden başlatma sonrası da kaldığı yerden sürer.
 *
 * @param key ana teslimin anahtarı
 * @param firedAt ilk teslim anı (merdivenin t0'ı)
 * @param ladderStepsDone t0 dahil işlenmiş basamak sayısı
 * @param persistent ısrarlı takip açık mı (karar 0003)
 * @param lastAskedAt son bildirimin anı (ısrarlı takibin sayacı buradan işler)
 * @param asksDone bugüne dek sorulmuş ısrarlı takip sorusu sayısı (anahtar indeksi)
 */
data class InFlightOccurrence(
    val key: String,
    val reminderId: String,
    val klass: ReminderClass,
    val firedAt: Instant,
    val ladderStepsDone: Int = 1,
    val persistent: Boolean = false,
    val lastAskedAt: Instant = firedAt,
    val asksDone: Int = 0,
) {
    init {
        require(ladderStepsDone >= 1) { "ladderStepsDone en az 1 olmalı (t0 işlenmiş): $ladderStepsDone" }
        require(asksDone >= 0) { "asksDone negatif olamaz: $asksDone" }
    }
}

/** Ertelenmiş teslim: yeni anahtarla ([SnoozePolicy.snoozedKey]) yeni teslim. */
data class SnoozedDelivery(val key: String, val reminderId: String, val klass: ReminderClass, val fireAt: Instant)

/** Israrlı takip ve merdiven için çalışma zamanı ayarları (Kullanıcı ayarlarından ve bağlamdan gelir). */
data class FollowUpConfig(
    val interval: Duration,
    val zone: ZoneId,
    val sleepStart: LocalTime,
    val sleepEnd: LocalTime,
    /** Bu andan önce ısrarlı takip sormaz (kriz/Bunaldım sonrası, odak oturumu, "Bugün sessiz"). Kritik merdiveni etkilemez. */
    val silentUntil: Instant? = null,
    val trustedContactEnabled: Boolean = false,
)

/**
 * Planlayıcı çıktısı: kurulacaklar, iptal edilecek anahtarlar ve vakti geçtiği için kurulmayıp
 * **hemen teslim edilecek** olanlar (geç teslim; Bölüm I invaryantı: geçmiş `fireAt` kurulmaz).
 */
data class PlanResult(
    val toSchedule: List<PlannedAlarm>,
    val toCancel: List<String>,
    val dueNow: List<PlannedAlarm> = emptyList(),
) {
    val isEmpty: Boolean get() = toSchedule.isEmpty() && toCancel.isEmpty() && dueNow.isEmpty()
}
