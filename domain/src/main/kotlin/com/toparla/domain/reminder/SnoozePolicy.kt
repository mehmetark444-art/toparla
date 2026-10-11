package com.toparla.domain.reminder

import com.toparla.domain.Defaults
import java.time.Duration

sealed interface SnoozeDecision {
    data class Snooze(val delay: Duration) : SnoozeDecision

    /** Art arda erteleme sınırı doldu: "Yarına taşıyayım mı, atlayayım mı?" sorulur. */
    data object AskCarryOrSkip : SnoozeDecision
}

/** Erteleme kuralları (blueprint Bölüm I, Erteleme). */
object SnoozePolicy {
    val DEFAULT_DELAY: Duration = Duration.ofMinutes(10)
    val MEDICATION_DEFAULT_DELAY: Duration = Duration.ofMinutes(Defaults.MEDICATION_SNOOZE_MIN)
    val MEDICATION_MAX_DELAY: Duration = Duration.ofMinutes(Defaults.MEDICATION_SNOOZE_MAX_MIN)
    const val MAX_CONSECUTIVE_SNOOZES = 3

    /**
     * @param previousSnoozes bu olay için daha önce yapılmış art arda erteleme sayısı
     * @param requested seçilen süre; yoksa varsayılan (ilaçta "15 dk sonra")
     */
    fun decide(previousSnoozes: Int, requested: Duration? = null, isMedication: Boolean = false): SnoozeDecision {
        require(previousSnoozes >= 0) { "previousSnoozes negatif olamaz" }
        if (previousSnoozes >= MAX_CONSECUTIVE_SNOOZES) return SnoozeDecision.AskCarryOrSkip
        val fallback = if (isMedication) MEDICATION_DEFAULT_DELAY else DEFAULT_DELAY
        val delay = (requested ?: fallback).let { if (it.isNegative || it.isZero) fallback else it }
        return SnoozeDecision.Snooze(if (isMedication) minOf(delay, MEDICATION_MAX_DELAY) else delay)
    }

    /** Ertelenen teslimin anahtarı: aynı olay + erteleme indeksi (yeni teslim, yeni key). */
    fun snoozedKey(originalKey: String, snoozeIndex: Int): String = "$originalKey#s$snoozeIndex"
}
