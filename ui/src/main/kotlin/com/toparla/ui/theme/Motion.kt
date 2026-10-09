package com.toparla.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween

/**
 * Hareket (blueprint C5). Süre ≤ 250 ms, kutlama ≤ 600 ms; hiçbir şey yanıp sönmez.
 * "Animasyonları azalt" açıkken yalnız opaklık geçişi kalır: konum/ölçek animasyonları anında biter.
 */
object Motion {
    const val STANDARD_MS = 250
    const val CARD_TRANSITION_MS = 220
    const val CELEBRATION_MS = 600
    const val FADE_MS = 150

    /** Güneş "düşünüyor" ışıltısının nefes periyodu (C5: yavaş, 2 sn). */
    const val GUNES_BREATH_MS = 2000

    /** Kart tamamlanınca küçüldüğü ölçek ve soluklaştığı opaklık (C5). */
    const val COMPLETE_SCALE = 0.96f
    const val COMPLETE_ALPHA = 0.5f

    fun <T> enter(reduceMotion: Boolean, durationMs: Int = STANDARD_MS): AnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis = durationMs, easing = FastOutSlowInEasing)

    fun <T> exit(reduceMotion: Boolean, durationMs: Int = STANDARD_MS): AnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis = durationMs, easing = LinearOutSlowInEasing)

    /** Opaklık geçişi "Animasyonları azalt" açıkken de kalır, yalnız kısalır. */
    fun <T> fade(reduceMotion: Boolean): AnimationSpec<T> =
        tween(durationMillis = if (reduceMotion) FADE_MS else STANDARD_MS, easing = LinearOutSlowInEasing)
}
