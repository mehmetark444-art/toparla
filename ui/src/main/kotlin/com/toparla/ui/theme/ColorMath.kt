package com.toparla.ui.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.1 kontrast hesabı (blueprint C8: metin ≥ 4,5:1, büyük metin ≥ 3:1).
 * Renkler `0xAARRGGBB` biçiminde; saydamlık hesaba katılmaz, yalnız opak renk çiftleri ölçülür.
 * Saf Kotlin: Android'e bağlı değildir, JVM testinde koşar.
 */
object ColorMath {
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BLUE_SHIFT = 0
    private const val CHANNEL_MASK = 0xFF
    private const val CHANNEL_MAX = 255.0
    private const val LINEAR_THRESHOLD = 0.03928
    private const val LINEAR_DIVISOR = 12.92
    private const val GAMMA_OFFSET = 0.055
    private const val GAMMA_DIVISOR = 1.055
    private const val GAMMA = 2.4
    private const val RED_WEIGHT = 0.2126
    private const val GREEN_WEIGHT = 0.7152
    private const val BLUE_WEIGHT = 0.0722
    private const val FLARE = 0.05

    /** Göreli parlaklık, 0 (siyah) ile 1 (beyaz) arası. */
    fun relativeLuminance(argb: Long): Double =
        RED_WEIGHT * linear(channel(argb, RED_SHIFT)) +
            GREEN_WEIGHT * linear(channel(argb, GREEN_SHIFT)) +
            BLUE_WEIGHT * linear(channel(argb, BLUE_SHIFT))

    /** İki rengin kontrast oranı, 1 ile 21 arası; sıra önemsiz. */
    fun contrastRatio(first: Long, second: Long): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)
        return (max(a, b) + FLARE) / (min(a, b) + FLARE)
    }

    private fun channel(argb: Long, shift: Int): Double = ((argb shr shift).toInt() and CHANNEL_MASK) / CHANNEL_MAX

    private fun linear(value: Double): Double =
        if (value <= LINEAR_THRESHOLD) value / LINEAR_DIVISOR else ((value + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA)
}
