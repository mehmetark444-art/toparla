package com.toparla.domain.medication

import com.toparla.domain.Defaults
import java.time.Duration
import java.time.Instant

/** İlaç verisi (blueprint H2 `Medication`, `MedicationLog`). Uygulaması `:data` içindedir. */
interface MedicationRepository {
    suspend fun medication(id: String): Medication?

    /** Son "alındı" (zamanında ya da geç) kaydının anı; yoksa null. */
    suspend fun lastTakenAt(medicationId: String): Instant?

    suspend fun log(medicationId: String, plannedAt: Instant): MedicationLog?

    /** Aynı doz (ilaç, planlanan an) için var olan kaydın yerine yazar. */
    suspend fun saveLog(log: MedicationLog)

    suspend fun updateStock(medicationId: String, stock: Int, at: Instant)

    /** Tek seferlik, Önemli sınıf "Eczaneye uğra" hatırlatması kurar ([MedicationSchedule.restockReminderId]). */
    suspend fun requestRestock(medicationId: String, at: Instant)
}

/**
 * Dozun kaydı, çift doz denetimi ve stok (Katman 0; AI yok). Hatırlatma motoru ilaç dozunun eylemlerinde bunu
 * çağırır; ekranlar da aynı motordan geçer, böylece kayıt tek yoldan yazılır.
 */
class MedicationRecorder(private val repo: MedicationRepository) {
    /** "Aldım" denmeden önce: asgari aralık doldu mu? İlaç silinmişse soru sorulmaz. */
    suspend fun check(medicationId: String, now: Instant): DoubleDoseCheck {
        val med = repo.medication(medicationId) ?: return DoubleDoseCheck.Clear
        return DoubleDoseGuard.check(repo.lastTakenAt(medicationId), med.minGap, now)
    }

    /** Alındı: planlanan ana göre zamanında ya da geç. */
    suspend fun taken(medicationId: String, plannedAt: Instant, now: Instant) =
        record(medicationId, plannedAt, now, DoseRules.statusForTaken(plannedAt, now))

    suspend fun skipped(medicationId: String, plannedAt: Instant, now: Instant) = record(medicationId, plannedAt, now, DoseStatus.SKIPPED)

    /**
     * Kaydı yazar. Stok yalnız doz **ilk kez** "alındı" sayıldığında düşer: aynı dozun kaydı düzeltilirse ya da
     * ikinci kez gelirse (çift dokunuş) stok yeniden düşmez.
     */
    private suspend fun record(medicationId: String, plannedAt: Instant, now: Instant, status: DoseStatus) {
        val previous = repo.log(medicationId, plannedAt)
        repo.saveLog(MedicationLog(medicationId, plannedAt, now, status))
        if (!status.counted || previous?.status?.counted == true) return
        val med = repo.medication(medicationId) ?: return
        val change = StockRule.afterDose(med.stock, med.unitsPerDose, med.lowStockAt)
        change.stock?.let { repo.updateStock(medicationId, it, now) }
        if (change.restock) repo.requestRestock(medicationId, now.plus(RESTOCK_DELAY))
    }

    companion object {
        val RESTOCK_DELAY: Duration = Duration.ofSeconds(Defaults.MEDICATION_RESTOCK_DELAY_SEC)
    }
}

/**
 * CSV (RFC 4180): virgül, tırnak ve satır sonu içeren hücre tırnaklanır. Hesap tablosunda formül gibi çalışabilecek
 * hücre (`=`, `+`, `-`, `@` ile başlayan) başına kesme işareti alır: dışa aktarılan not bir formül olarak koşmasın.
 */
object Csv {
    private const val LINE_END = "\r\n"
    private val FORMULA_START = setOf('=', '+', '-', '@')
    private val NEEDS_QUOTE = charArrayOf(',', '"', '\n', '\r')

    fun format(rows: List<List<String>>): String = buildString {
        rows.forEach { row ->
            append(row.joinToString(",") { cell(it) })
            append(LINE_END)
        }
    }

    private fun cell(raw: String): String {
        val safe = if (raw.firstOrNull() in FORMULA_START) "'$raw" else raw
        return if (safe.any { it in NEEDS_QUOTE }) "\"${safe.replace("\"", "\"\"")}\"" else safe
    }
}
