package com.toparla.domain.reminder

import java.time.Duration

/** Merdiven basamağında yapılacak iş (blueprint Bölüm I, Merdiven). */
enum class LadderAction {
    /** Bildirim + ses (kritikte alarm sesi). */
    NOTIFY,

    /** Titreşim + ses tekrarı. */
    VIBRATE_REPEAT,

    /** Kilit ekranı üstünde tam ekran kart. */
    FULL_SCREEN,

    /** Güvenilir kişiye SMS; yalnız M15 açıksa ve Kullanıcı bu basamağı açtıysa. */
    TRUSTED_CONTACT_SMS,

    /** Tek sade tekrar bildirimi. */
    REPEAT,
}

data class LadderStep(val offset: Duration, val action: LadderAction)

/**
 * Sınıfa göre yükselme merdiveni. Basamaklar yanıt gelene kadar sırayla işler; yanıt gelince kalan
 * basamaklar iptal edilir. Israrlı takip (karar 0003) merdiven değildir: [PersistentFollowUp].
 */
object Ladder {
    private fun min(minutes: Long) = Duration.ofMinutes(minutes)

    private val critical = listOf(
        LadderStep(Duration.ZERO, LadderAction.NOTIFY),
        LadderStep(min(2), LadderAction.VIBRATE_REPEAT),
        LadderStep(min(5), LadderAction.FULL_SCREEN),
        LadderStep(min(10), LadderAction.FULL_SCREEN),
    )
    private val trustedContactStep = LadderStep(min(15), LadderAction.TRUSTED_CONTACT_SMS)
    private val singleRepeat = listOf(
        LadderStep(Duration.ZERO, LadderAction.NOTIFY),
        LadderStep(min(30), LadderAction.REPEAT),
    )
    private val single = listOf(LadderStep(Duration.ZERO, LadderAction.NOTIFY))

    fun stepsFor(klass: ReminderClass, trustedContactEnabled: Boolean = false): List<LadderStep> = when (klass) {
        ReminderClass.CRITICAL -> if (trustedContactEnabled) critical + trustedContactStep else critical
        ReminderClass.IMPORTANT, ReminderClass.NORMAL -> singleRepeat
        ReminderClass.INFO -> single
    }
}
