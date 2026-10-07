package com.toparla.domain.reminder

/**
 * Tek bir teslimin (ReminderOccurrence) durumu (blueprint Bölüm I, Durumlar).
 * Kullanıcıya dönük dilde EXPIRED/CARRIED "Taşınan"dır (K12).
 */
enum class OccurrenceState {
    PLANNED,
    DELIVERED,
    SEEN,
    DONE,
    SNOOZED,
    SKIPPED,
    EXPIRED,
    CARRIED,
    CANCELLED,
    MISSED_DETECTED,
    ;

    /** Bu teslim için başka geçiş yok. Ertelenen teslim yeni anahtarla yeni teslim doğurur. */
    val isTerminal: Boolean get() = this in setOf(DONE, SNOOZED, SKIPPED, CARRIED, CANCELLED)
}

enum class OccurrenceEvent {
    /** Alarm ateşlendi, bildirim gönderildi. */
    FIRED,

    /** Bildirim açıldı ya da dokunuldu. */
    OPENED,
    MARKED_DONE,
    SNOOZE,
    SKIP,

    /** Merdiven yanıtsız bitti. */
    LADDER_EXHAUSTED,

    /** Süresi dolan iş ileri taşındı. */
    CARRY,

    /** Tanım silindi ya da değişti. */
    DEFINITION_CHANGED,

    /** Denetçi, vakti geçtiği hâlde ateşlenme kaydı olmayan teslimi buldu. */
    AUDIT_FOUND_UNFIRED,
}

object OccurrenceStateMachine {
    private val transitions: Map<OccurrenceState, Map<OccurrenceEvent, OccurrenceState>> = mapOf(
        OccurrenceState.PLANNED to mapOf(
            OccurrenceEvent.FIRED to OccurrenceState.DELIVERED,
            OccurrenceEvent.DEFINITION_CHANGED to OccurrenceState.CANCELLED,
            OccurrenceEvent.AUDIT_FOUND_UNFIRED to OccurrenceState.MISSED_DETECTED,
        ),
        // Geç teslim: denetçinin bulduğu teslim yine de gösterilir.
        OccurrenceState.MISSED_DETECTED to mapOf(
            OccurrenceEvent.FIRED to OccurrenceState.DELIVERED,
            OccurrenceEvent.DEFINITION_CHANGED to OccurrenceState.CANCELLED,
        ),
        // Bildirim eylemleri uygulama açılmadan çalışır; "görüldü" adımı atlanabilir.
        OccurrenceState.DELIVERED to mapOf(
            OccurrenceEvent.OPENED to OccurrenceState.SEEN,
            OccurrenceEvent.MARKED_DONE to OccurrenceState.DONE,
            OccurrenceEvent.SNOOZE to OccurrenceState.SNOOZED,
            OccurrenceEvent.SKIP to OccurrenceState.SKIPPED,
            OccurrenceEvent.LADDER_EXHAUSTED to OccurrenceState.EXPIRED,
        ),
        OccurrenceState.SEEN to mapOf(
            OccurrenceEvent.MARKED_DONE to OccurrenceState.DONE,
            OccurrenceEvent.SNOOZE to OccurrenceState.SNOOZED,
            OccurrenceEvent.SKIP to OccurrenceState.SKIPPED,
            OccurrenceEvent.LADDER_EXHAUSTED to OccurrenceState.EXPIRED,
        ),
        // Süresi dolan iş geç de olsa tamamlanabilir ya da taşınır.
        OccurrenceState.EXPIRED to mapOf(
            OccurrenceEvent.CARRY to OccurrenceState.CARRIED,
            OccurrenceEvent.MARKED_DONE to OccurrenceState.DONE,
            OccurrenceEvent.SKIP to OccurrenceState.SKIPPED,
        ),
    )

    /** Geçerli geçişte yeni durum; geçersizse null (çağıran yok sayar ve günlüğe yazar). */
    fun next(state: OccurrenceState, event: OccurrenceEvent): OccurrenceState? = transitions[state]?.get(event)

    /** Idempotent uygulama: aynı olay ikinci kez gelirse (çift dokunuş, çift yayın) durum değişmez. */
    fun apply(state: OccurrenceState, event: OccurrenceEvent): OccurrenceState = next(state, event) ?: state
}
