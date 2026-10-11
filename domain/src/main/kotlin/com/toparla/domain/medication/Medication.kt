package com.toparla.domain.medication

import com.toparla.domain.Defaults
import com.toparla.domain.reminder.MedicationRole
import com.toparla.domain.reminder.Recurrence
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.ReminderDef
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * İlaç tanımı (blueprint M9, H2 `Medication`; v3 §9.2). **Uygulama ilaç önermez, doz hesaplamaz, doz ya da zaman
 * değişikliği önermez:** her alan Kullanıcı'nın girdiğidir; doz serbest metindir ve hiçbir hesaba girmez.
 * Stokta yalnız Kullanıcı'nın verdiği "doz başına adet" çıkarılır.
 *
 * @param times günün saatleri (yerel); sıralı ve tekil saklanır
 * @param days hangi günler; yedisi birden "her gün"
 * @param minGap çift doz koruması: önceki kayıttan bu süre geçmeden "Aldım" denirse sorulur; sıfır = kapalı
 * @param stock kalan adet; null = izlenmiyor
 * @param lowStockAt kalan bu sayıya inince "Eczaneye uğra"; null = uyarı yok
 * @param hideName bildirimde de ad yerine genel metin (kilit ekranında ad zaten hiç görünmez)
 * @param routineId ilişkili rutin (M8; rutinler F4.4'te gelir, o zamana dek boş)
 */
data class Medication(
    val id: String,
    val name: String,
    val doseText: String,
    val times: List<LocalTime>,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val startDate: LocalDate,
    val zoneId: ZoneId,
    val minGap: Duration = Duration.ofHours(Defaults.MEDICATION_MIN_GAP_HOURS),
    val stock: Int? = null,
    val unitsPerDose: Int = 1,
    val lowStockAt: Int? = null,
    val notes: String = "",
    val hideName: Boolean = false,
    val active: Boolean = true,
    val routineId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "İlaç kimliği boş olamaz" }
        require(name.isNotBlank()) { "İlaç adı boş olamaz" }
        require(times.isNotEmpty()) { "İlacın en az bir saati olmalı" }
        require(days.isNotEmpty()) { "İlacın en az bir günü olmalı" }
        require(!minGap.isNegative) { "Asgari aralık negatif olamaz" }
        require(stock == null || stock >= 0) { "Stok negatif olamaz: $stock" }
        require(unitsPerDose >= 1) { "Doz başına adet en az 1: $unitsPerDose" }
        require(lowStockAt == null || lowStockAt >= 0) { "Stok eşiği negatif olamaz: $lowStockAt" }
        // Aynı saat iki hatırlatma doğurmasın: saatler dakikaya yuvarlı, sıralı ve tekil gelir ([normalizeTimes]).
        require(times == normalizeTimes(times)) { "Saatler dakikalık, sıralı ve tekil olmalı: $times" }
    }

    val everyDay: Boolean get() = days.size == DayOfWeek.entries.size

    companion object {
        fun normalizeTimes(times: List<LocalTime>): List<LocalTime> = times.map { it.withSecond(0).withNano(0) }.distinct().sorted()
    }
}

/**
 * İlacın hatırlatma motoruna dönüşümü: her saat bir **Kritik** hatırlatmadır (blueprint M9, D25). Motorun kendisi
 * değişmez; ilaç dozları da aynı planlayıcı, merdiven ve güvenlik ağlarından geçer.
 */
object MedicationSchedule {
    private const val DOSE_PREFIX = "med:"
    private const val RESTOCK_PREFIX = "med-stock:"
    private const val SEPARATOR = ':'
    private val SLOT: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")

    data class Parsed(val medicationId: String, val role: MedicationRole)

    fun doseReminderId(medicationId: String, time: LocalTime): String = "$DOSE_PREFIX$medicationId$SEPARATOR${SLOT.format(time)}"

    /** Stok eşiği her geçildiğinde yeni bir "Eczaneye uğra" (anahtar o anı taşır; aynı an iki kez istenmez). */
    fun restockReminderId(medicationId: String, at: Instant): String = "$RESTOCK_PREFIX$medicationId$SEPARATOR${at.toEpochMilli()}"

    fun definitions(med: Medication): List<ReminderDef> = med.times.map { time ->
        ReminderDef(
            id = doseReminderId(med.id, time),
            klass = ReminderClass.CRITICAL,
            startLocal = med.startDate.atTime(time),
            zoneId = med.zoneId,
            recurrence = if (med.everyDay) Recurrence.Daily else Recurrence.Weekly(med.days),
            active = med.active,
        )
    }

    fun isMedicationReminder(reminderId: String): Boolean = parse(reminderId) != null

    /** Hatırlatma kimliğinden ilaç ve rol; ilaca ait değilse null. */
    fun parse(reminderId: String): Parsed? {
        val (prefix, role) = when {
            reminderId.startsWith(RESTOCK_PREFIX) -> RESTOCK_PREFIX to MedicationRole.RESTOCK
            reminderId.startsWith(DOSE_PREFIX) -> DOSE_PREFIX to MedicationRole.DOSE
            else -> return null
        }
        val medicationId = reminderId.removePrefix(prefix).substringBeforeLast(SEPARATOR, "")
        return if (medicationId.isBlank()) null else Parsed(medicationId, role)
    }
}

/** Bir dozun Kullanıcı'nın verdiği kaydı (v3 `MedicationLog.status`). */
enum class DoseStatus {
    TAKEN,
    LATE,
    SKIPPED,
    ;

    /** Alındı sayılır mı (çift doz koruması ve stok için). */
    val counted: Boolean get() = this != SKIPPED
}

/**
 * Dozun kaydı. Bir doz için tek kayıt vardır: anahtar (ilaç, planlanan an). Ertelenen dozun kaydı da ilk planlanan
 * ana yazılır.
 */
data class MedicationLog(
    val medicationId: String,
    val plannedAt: Instant,
    val loggedAt: Instant,
    val status: DoseStatus,
    val note: String? = null,
)

sealed interface DoubleDoseCheck {
    data object Clear : DoubleDoseCheck

    /** Son "alındı" kaydından bu yana geçen süre asgari aralıktan kısa. */
    data class TooSoon(val since: Duration) : DoubleDoseCheck
}

/** Çift doz koruması (blueprint M9; v3: "Son kayıt X saat önce. Emin misin?"). Uyarır, engellemez. */
object DoubleDoseGuard {
    fun check(lastTakenAt: Instant?, minGap: Duration, now: Instant): DoubleDoseCheck {
        if (lastTakenAt == null || minGap.isZero) return DoubleDoseCheck.Clear
        val since = Duration.between(lastTakenAt, now)
        // Saat geri alındıysa son kayıt gelecekte görünür: emin olunamaz, sorulur.
        if (since.isNegative) return DoubleDoseCheck.TooSoon(Duration.ZERO)
        return if (since < minGap) DoubleDoseCheck.TooSoon(since) else DoubleDoseCheck.Clear
    }
}

object DoseRules {
    /** Kaydı olmayan doz bu süreden sonra "Kaydedilmedi" (blueprint Bölüm I: "+60 dk (ilaç)"). */
    val NOT_LOGGED_AFTER: Duration = Duration.ofMinutes(Defaults.MEDICATION_NOT_LOGGED_AFTER_MIN)

    /** "Aldım" bu sınırın içindeyse zamanında, sonrasındaysa geç alındı. */
    fun statusForTaken(plannedAt: Instant, takenAt: Instant): DoseStatus =
        if (takenAt.isAfter(plannedAt.plus(NOT_LOGGED_AFTER))) DoseStatus.LATE else DoseStatus.TAKEN
}

data class StockChange(val stock: Int?, val restock: Boolean)

/** Stok (blueprint M9: "stok azalınca Eczaneye uğra"). Uyarı yalnız eşiğin **geçildiği** dozda bir kez verilir. */
object StockRule {
    fun afterDose(stock: Int?, unitsPerDose: Int, lowStockAt: Int?): StockChange {
        if (stock == null) return StockChange(null, restock = false)
        val next = (stock - unitsPerDose).coerceAtLeast(0)
        val crossed = lowStockAt != null && stock > lowStockAt && next <= lowStockAt
        return StockChange(next, crossed)
    }
}

/** Ekranın ve dışa aktarımın gördüğü doz durumu. */
enum class DoseState {
    TAKEN,
    LATE,
    SKIPPED,

    /** Kayıt yok ve süre doldu ("Kaydedilmedi"). Hiçbir yerde "kaçırdın" denmez. */
    NOT_LOGGED,

    /** Kayıt yok, süre dolmadı. */
    PENDING,
    ;

    /** "Unuttum" akışı: yalnız kaydı olmayan doz sonradan kaydedilir (telafi dozu önerilmez). */
    val canLogLater: Boolean get() = this == NOT_LOGGED || this == PENDING
}

/** Teslim edilmiş bir doz: [occurrenceKey] o dozun şu anki (son ertelenmiş) teslimi. */
data class PlannedDose(val medicationId: String, val plannedAt: Instant, val occurrenceKey: String)

data class DoseRow(
    val medicationId: String,
    val plannedAt: Instant,
    val occurrenceKey: String,
    val state: DoseState,
    val loggedAt: Instant? = null,
)

/** Kaçan doz yalnız kayıttır (blueprint M9): geçmiş, teslim edilmiş dozlar ile Kullanıcı'nın kayıtlarından kurulur. */
object DoseHistory {
    fun rows(planned: List<PlannedDose>, logs: List<MedicationLog>, now: Instant): List<DoseRow> {
        val byDose = logs.associateBy { it.medicationId to it.plannedAt }
        return planned.map { dose ->
            val log = byDose[dose.medicationId to dose.plannedAt]
            val state = when (log?.status) {
                DoseStatus.TAKEN -> DoseState.TAKEN
                DoseStatus.LATE -> DoseState.LATE
                DoseStatus.SKIPPED -> DoseState.SKIPPED
                null -> if (now.isBefore(dose.plannedAt.plus(DoseRules.NOT_LOGGED_AFTER))) DoseState.PENDING else DoseState.NOT_LOGGED
            }
            DoseRow(dose.medicationId, dose.plannedAt, dose.occurrenceKey, state, log?.loggedAt)
        }.sortedByDescending { it.plannedAt }
    }
}
