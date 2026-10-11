package com.toparla.app.medication

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.CancellationSignal
import com.toparla.app.R
import com.toparla.data.db.MedicationStore
import com.toparla.domain.core.Clock
import com.toparla.domain.core.DispatcherProvider
import com.toparla.domain.medication.Csv
import com.toparla.domain.medication.DoseHistory
import com.toparla.domain.medication.DoseState
import com.toparla.domain.medication.ExportLabels
import com.toparla.domain.medication.ExportTable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * İlaç ekranlarının isteğe bağlı kilidi (blueprint M9, karar 0018). Çerçevenin `BiometricPrompt`'u: parmak izi, yüz ya
 * da ekran kilidi. Telefonda ekran kilidi yoksa kilit uygulanmaz (Kullanıcı kendi kaydına erişemez hâle gelmesin).
 */
@Singleton
class MedicationLock @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun available(): Boolean =
        context.getSystemService(BiometricManager::class.java).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    /** Sistem penceresini açar; başarılıysa [onUnlocked]. Vazgeçilirse ya da hata olursa ekran kilitli kalır. */
    fun prompt(activity: Activity, onUnlocked: () -> Unit) {
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(activity.getString(R.string.med_lock_prompt))
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlocked()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Timber.i("İlaç kilidi açılmadı: %d", errorCode)
                }
            },
        )
    }

    private companion object {
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

enum class ExportRange(val days: Long) { DAYS_7(7), DAYS_30(30), DAYS_90(90) }

enum class ExportFormat(val mimeType: String, val extension: String) {
    PDF("application/pdf", "pdf"),
    CSV("text/csv", "csv"),
}

/**
 * Dozların kaydını dışa aktarır (blueprint M9, F2.40): tarih aralığı, CSV ya da PDF. Dosyanın yerini Kullanıcı sistemin
 * "dosya oluştur" ekranında seçer; uygulama dosyayı kimseye göndermez (karar 0018). Metin deterministiktir.
 */
@Singleton
class MedicationExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: MedicationStore,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) {
    /** @return dosya yazıldıysa true */
    suspend fun write(range: ExportRange, format: ExportFormat, target: Uri): Boolean = withContext(dispatchers.io) {
        val table = table(range)
        try {
            val stream = context.contentResolver.openOutputStream(target) ?: return@withContext false
            stream.use { if (format == ExportFormat.CSV) writeCsv(it, table) else writePdf(it, table, range) }
            true
        } catch (e: IOException) {
            Timber.w(e, "Dışa aktarım yazılamadı")
            false
        } catch (e: SecurityException) {
            Timber.w(e, "Dışa aktarım hedefine yazma izni yok")
            false
        }
    }

    /** Önerilen dosya adı (Kullanıcı değiştirebilir): `ilac-kaydi-2026-10-11.csv`. */
    fun suggestedName(format: ExportFormat): String =
        "${context.getString(R.string.med_export_file)}-${clock.now().atZone(clock.zone()).toLocalDate()}.${format.extension}"

    private suspend fun table(range: ExportRange): List<List<String>> {
        val now = clock.now()
        val from = now.atZone(clock.zone()).toLocalDate().minusDays(range.days - 1).atStartOfDay(clock.zone()).toInstant()
        val rows = DoseHistory.rows(store.plannedDoses(from), store.logsBetween(from, now.plusMillis(1)), now)
        val names = rows.map { it.medicationId }.distinct().associateWith { id ->
            val med = store.medication(id)
            (med?.name ?: store.nameOf(id).orEmpty()) to med?.doseText.orEmpty()
        }
        return ExportTable.build(rows, names, clock.zone(), labels())
    }

    private fun labels() = ExportLabels(
        header = listOf(
            R.string.med_export_col_date, R.string.med_export_col_time, R.string.med_export_col_name,
            R.string.med_export_col_dose, R.string.med_export_col_state, R.string.med_export_col_logged,
        ).map(context::getString),
        states = mapOf(
            DoseState.TAKEN to R.string.med_export_state_taken,
            DoseState.LATE to R.string.med_export_state_late,
            DoseState.SKIPPED to R.string.med_state_skipped,
            DoseState.NOT_LOGGED to R.string.med_state_not_logged,
            DoseState.PENDING to R.string.med_state_pending,
            DoseState.UPCOMING to R.string.med_state_upcoming,
            DoseState.SNOOZED to R.string.med_state_snoozed,
        ).mapValues { context.getString(it.value) },
    )

    private fun writeCsv(out: OutputStream, table: List<List<String>>) {
        // UTF-8 BOM: hesap tabloları Türkçe harfleri doğru açsın.
        out.write(BOM)
        out.write(Csv.format(table).toByteArray(Charsets.UTF_8))
    }

    /** Sade A4 sayfalar: başlık, tarih aralığı, tablo ve her sayfada aynı alt not (blueprint klinik özet biçimi: sade, sayfa başına tarih). */
    private fun writePdf(out: OutputStream, table: List<List<String>>, range: ExportRange) {
        val document = PdfDocument()
        val text = Paint().apply { textSize = PDF_TEXT }
        val bold = Paint(text).apply { isFakeBoldText = true }
        val today = clock.now().atZone(clock.zone()).toLocalDate()
        val title = context.getString(R.string.med_export_pdf_title)
        val span = context.getString(R.string.med_export_pdf_range, PDF_DATE.format(today.minusDays(range.days - 1)), PDF_DATE.format(today))
        val footer = context.getString(R.string.med_export_pdf_footer)
        val header = table.first()
        val body = table.drop(1)
        val pages = body.chunked(PDF_ROWS_PER_PAGE).ifEmpty { listOf(emptyList()) }
        try {
            pages.forEachIndexed { index, rows ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(PDF_WIDTH, PDF_HEIGHT, index + 1).create())
                val canvas = page.canvas
                var y = PDF_MARGIN
                canvas.drawText(title, PDF_MARGIN, y, bold)
                y += PDF_LINE
                canvas.drawText(span, PDF_MARGIN, y, text)
                y += PDF_LINE * 2
                (listOf(header) + rows).forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { column, cell ->
                        canvas.drawText(cell.take(PDF_CELL_CHARS[column]), PDF_MARGIN + PDF_COLUMNS[column], y, if (rowIndex == 0) bold else text)
                    }
                    y += PDF_LINE
                }
                canvas.drawText(footer, PDF_MARGIN, PDF_HEIGHT - PDF_MARGIN, text)
                document.finishPage(page)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private companion object {
        val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val PDF_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        // A4, PostScript noktası (1/72 inç).
        const val PDF_WIDTH = 595
        const val PDF_HEIGHT = 842
        const val PDF_MARGIN = 40f
        const val PDF_LINE = 16f
        const val PDF_TEXT = 10f
        const val PDF_ROWS_PER_PAGE = 42

        /** Sütunların sol kenardan uzaklığı ve en çok karakteri: tarih, saat, ilaç, doz, durum, kayıt. */
        val PDF_COLUMNS = floatArrayOf(0f, 70f, 140f, 280f, 370f, 450f)
        val PDF_CELL_CHARS = intArrayOf(12, 8, 24, 16, 14, 18)
    }
}
