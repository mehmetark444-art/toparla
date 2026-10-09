package com.toparla.domain.reminder

import java.time.Duration
import java.time.Instant

/** Alarm anahtarının türü: ana teslim ya da ona bağlı merdiven basamağı, ısrarlı takip sorusu, erteleme. */
enum class AlarmKeyKind { MAIN, LADDER, FOLLOW_UP, SNOOZE }

/**
 * Anahtar çözümü. Biçimler: `id@ms` (ana) · `…#sN` (erteleme; kendi başına bir teslimdir) ·
 * `…#lN` (merdiven) · `…#fN` (ısrarlı takip). Merdiven ve takip, önündeki teslime (`occurrenceKey`) aittir.
 */
data class AlarmKey(val occurrenceKey: String, val reminderId: String, val kind: AlarmKeyKind, val index: Int) {
    companion object {
        private val TAIL = Regex("""#([lfs])(\d+)$""")

        fun parse(key: String): AlarmKey {
            val reminderId = key.substringBefore('@')
            val match = TAIL.find(key) ?: return AlarmKey(key, reminderId, AlarmKeyKind.MAIN, 0)
            val index = match.groupValues[2].toInt()
            return when (match.groupValues[1]) {
                "l" -> AlarmKey(key.removeRange(match.range), reminderId, AlarmKeyKind.LADDER, index)
                "f" -> AlarmKey(key.removeRange(match.range), reminderId, AlarmKeyKind.FOLLOW_UP, index)
                else -> AlarmKey(key, reminderId, AlarmKeyKind.SNOOZE, index)
            }
        }
    }
}

/** Bildirimde gösterilecek kadarıyla hatırlatma. */
data class ReminderInfo(val id: String, val klass: ReminderClass, val title: String, val body: String, val persistent: Boolean)

/** Tek bir teslimin kalıcı kaydı (`ReminderOccurrence` tablosunun motora bakan yüzü). */
data class OccurrenceRecord(
    val key: String,
    val reminderId: String,
    val plannedAt: Instant,
    val state: OccurrenceState,
    val deliveredAt: Instant? = null,
    val resolvedAt: Instant? = null,
    val ladderStepsDone: Int = 0,
    val snoozeCount: Int = 0,
    val lastAskedAt: Instant? = null,
    val asksDone: Int = 0,
)

/** Kullanıcı'ya gösterilecek teslim. [lateBy] sıfır değilse geç teslimdir (suçlayıcı olmayan metinle gösterilir). */
data class DeliveryNotice(
    val occurrenceKey: String,
    val reminder: ReminderInfo,
    val action: LadderAction,
    val plannedAt: Instant,
    val lateBy: Duration,
    /** Aynı dakikadaki teslimler aynı gruba düşer ([DeliveryGrouping]). */
    val groupKey: String,
)

/** Israrlı takipte yanıt bekleyen bir iş (birleşik bildirimin bir satırı). */
data class PersistentItem(val occurrenceKey: String, val reminder: ReminderInfo, val asksDone: Int)

/** Bildirim eylemleri (blueprint G1, karar 0003). */
enum class ReminderAction { DONE, SNOOZE, TOMORROW, NOT_TODAY, OPENED }

/** Hatırlatma verisi. Uygulaması `:data` içindedir; testte bellek içi sahtesi kullanılır. */
interface ReminderRepository {
    suspend fun activeDefinitions(): List<ReminderDef>

    suspend fun info(reminderId: String): ReminderInfo?

    suspend fun scheduled(): List<ScheduledAlarm>

    suspend fun applyPlan(scheduled: List<PlannedAlarm>, cancelledKeys: List<String>)

    suspend fun removeScheduled(key: String)

    /** İlk kayıtsa true; aynı teslimin ikinci ateşlenmesinde false (çift teslim engeli). */
    suspend fun recordFired(key: String, at: Instant): Boolean

    suspend fun firedKeys(keys: List<String>): Set<String>

    suspend fun log(key: String, at: Instant, event: String, detail: String? = null)

    suspend fun occurrence(key: String): OccurrenceRecord?

    suspend fun saveOccurrence(record: OccurrenceRecord)

    /** Teslim edilmiş, yanıt bekleyen teslimler. */
    suspend fun openOccurrences(): List<OccurrenceRecord>

    /** Ertelenmiş, henüz çalmamış teslimler. */
    suspend fun pendingSnoozes(): List<OccurrenceRecord>
}

/** Sistem alarmları. Aynı anahtarla yeniden kurmak aynı sonucu verir (idempotans). */
interface ReminderScheduler {
    fun schedule(alarm: PlannedAlarm)

    fun cancel(key: String)

    /** Kendini besleyen bakım alarmı (pencere en geç 12 saatte bir yeniden doldurulur). */
    fun scheduleMaintenance(at: Instant)
}

/** Bildirim yüzeyi. */
interface ReminderNotifier {
    fun show(notice: DeliveryNotice)

    fun cancel(occurrenceKey: String)

    /** Israrlı işlerin tek birleşik bildirimi; liste boşsa bildirim kaldırılır. */
    fun showPersistent(items: List<PersistentItem>, askedAt: Instant)

    /** Üç ertelemeden sonra: "Yarına taşıyayım mı, atlayayım mı?" */
    fun askCarryOrSkip(occurrenceKey: String, reminder: ReminderInfo)
}
