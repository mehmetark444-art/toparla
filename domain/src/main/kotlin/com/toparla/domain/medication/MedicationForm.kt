package com.toparla.domain.medication

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** İlaç formunun ham hâli (ekranda yazılan ve seçilen; sayılar metin olarak gelir). */
data class MedicationInput(
    val name: String,
    val doseText: String,
    val times: List<LocalTime>,
    val days: Set<DayOfWeek>,
    val minGapHours: Int,
    val stock: String,
    val perDose: String,
    val lowStock: String,
    val notes: String,
    val hideName: Boolean,
) {
    companion object {
        /** Var olan ilacın formdaki hâli (düzenleme ekranı). */
        fun of(med: Medication) = MedicationInput(
            name = med.name,
            doseText = med.doseText,
            times = med.times,
            days = med.days,
            minGapHours = med.minGap.toHours().toInt(),
            stock = med.stock?.toString().orEmpty(),
            perDose = med.unitsPerDose.toString(),
            lowStock = med.lowStockAt?.toString().orEmpty(),
            notes = med.notes,
            hideName = med.hideName,
        )
    }
}

/**
 * İlaç formunun doğrulaması (F2.39). Uygulama hiçbir değeri önermez ya da hesaplamaz; yalnız Kullanıcı'nın yazdığını
 * temizler (boşluk, saat yuvarlama) ve eksik alanı adıyla söyler.
 */
object MedicationForm {
    const val MAX_NAME = 60
    const val MAX_DOSE = 60
    const val MAX_NOTES = 200
    const val MAX_COUNT = 9_999

    /** Çift doz koruması seçenekleri, saat; 0 = kapalı (karar 0018). */
    val GAP_OPTIONS_HOURS = listOf(0, 2, 4, 6, 8, 12)

    enum class Problem { NAME, TIMES, DAYS, STOCK, GAP }

    sealed interface Result {
        data class Ok(val medication: Medication) : Result

        data class Invalid(val problems: Set<Problem>) : Result
    }

    /**
     * @param today yeni ilacın başlangıç günü; düzenlemede [existing]'in günü korunur
     */
    fun build(id: String, input: MedicationInput, today: LocalDate, zone: ZoneId, existing: Medication? = null): Result {
        val problems = mutableSetOf<Problem>()
        val name = input.name.trim().take(MAX_NAME)
        if (name.isEmpty()) problems += Problem.NAME
        val times = Medication.normalizeTimes(input.times)
        if (times.isEmpty()) problems += Problem.TIMES
        if (input.days.isEmpty()) problems += Problem.DAYS
        if (input.minGapHours !in GAP_OPTIONS_HOURS) problems += Problem.GAP
        val stock = count(input.stock, min = 0)
        val perDose = if (input.perDose.isBlank()) Count.Value(1) else count(input.perDose, min = 1)
        val lowStock = count(input.lowStock, min = 0)
        if (listOf(stock, perDose, lowStock).any { it == Count.Bad }) problems += Problem.STOCK
        if (problems.isNotEmpty()) return Result.Invalid(problems)
        return Result.Ok(
            Medication(
                id = id,
                name = name,
                doseText = input.doseText.trim().take(MAX_DOSE),
                times = times,
                days = input.days,
                startDate = existing?.startDate ?: today,
                zoneId = existing?.zoneId ?: zone,
                minGap = Duration.ofHours(input.minGapHours.toLong()),
                stock = (stock as? Count.Value)?.value,
                unitsPerDose = (perDose as? Count.Value)?.value ?: 1,
                lowStockAt = (lowStock as? Count.Value)?.value,
                notes = input.notes.trim().take(MAX_NOTES),
                hideName = input.hideName,
                active = existing?.active ?: true,
                routineId = existing?.routineId,
            ),
        )
    }

    private sealed interface Count {
        data object Empty : Count

        data object Bad : Count

        data class Value(val value: Int) : Count
    }

    private fun count(text: String, min: Int): Count {
        if (text.isBlank()) return Count.Empty
        val value = text.trim().toIntOrNull() ?: return Count.Bad
        return if (value in min..MAX_COUNT) Count.Value(value) else Count.Bad
    }
}

/** Dışa aktarımın başlıkları ve durum adları (metinler kaynaklardan gelir; `:domain`'de Türkçe metin yok). */
data class ExportLabels(val header: List<String>, val states: Map<DoseState, String>)

/**
 * Dışa aktarım tablosu (F2.40; CSV ve PDF aynı tabloyu yazar): tarih, planlanan saat, ilaç, doz, durum, kayıt saati.
 * Eskiden yeniye sıralanır. Yorum ya da tanı cümlesi yoktur; yalnız kayıt.
 */
object ExportTable {
    private val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** @param medications ilaç kimliği → (ad, doz metni); silinmiş ilacın adı da verilebilir */
    fun build(rows: List<DoseRow>, medications: Map<String, Pair<String, String>>, zone: ZoneId, labels: ExportLabels): List<List<String>> =
        listOf(labels.header) + rows.sortedBy { it.plannedAt }.map { row ->
            val planned = row.plannedAt.atZone(zone)
            val logged = row.loggedAt?.atZone(zone)
            val (name, dose) = medications[row.medicationId] ?: ("" to "")
            listOf(
                DATE.format(planned),
                TIME.format(planned),
                name,
                dose,
                labels.states[row.state].orEmpty(),
                when {
                    logged == null -> ""
                    logged.toLocalDate() == planned.toLocalDate() -> TIME.format(logged)
                    else -> DATE_TIME.format(logged)
                },
            )
        }
}
