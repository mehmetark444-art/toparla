package com.toparla.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F2.20: her tema × her vurgu × her metin/zemin çifti ≥ 4,5:1 (blueprint C2, C8; karar 0016).
 * Yeni bir renk metin ya da ikon olarak kullanılmaya başlarsa çifti buraya eklenir.
 */
class ContrastTest {

    private fun textPairs(mode: ThemeMode, p: ToparlaPalette): List<Triple<String, Long, Long>> = listOf(
        Triple("onSurface/background", p.onSurface, p.background),
        Triple("onSurface/surface", p.onSurface, p.surface),
        Triple("onSurface/surfaceVariant", p.onSurface, p.surfaceVariant),
        Triple("onSurface/surfaceDim", p.onSurface, p.surfaceDim),
        Triple("onSurfaceVariant/background", p.onSurfaceVariant, p.background),
        Triple("onSurfaceVariant/surface", p.onSurfaceVariant, p.surface),
        Triple("onSurfaceVariant/surfaceVariant", p.onSurfaceVariant, p.surfaceVariant),
        Triple("onSurfaceVariant/surfaceDim", p.onSurfaceVariant, p.surfaceDim),
        Triple("primary/background", p.accent.primary, p.background),
        Triple("primary/surface", p.accent.primary, p.surface),
        Triple("primary/surfaceVariant", p.accent.primary, p.surfaceVariant),
        Triple("onPrimary/primary", p.accent.onPrimary, p.accent.primary),
        Triple("onPrimaryContainer/primaryContainer", p.accent.onPrimaryContainer, p.accent.primaryContainer),
        Triple("secondary/surface", p.secondary, p.surface),
        Triple("onSurface/secondaryContainer", p.onSurface, p.secondaryContainer),
        Triple("tertiary/surface", p.tertiary, p.surface),
        Triple("onSurface/tertiaryContainer", p.onSurface, p.tertiaryContainer),
        Triple("onCarriedContainer/carriedContainer", p.onCarriedContainer, p.carriedContainer),
        Triple("onCarriedContainer/surface", p.onCarriedContainer, p.surface),
        // Hatırlatma Sağlığı ve sınamanın amber kartı: başlık amber, gövde metni ana metin rengi.
        Triple("onSurface/carriedContainer", p.onSurface, p.carriedContainer),
        // Metin eylemi (TextAction): vurgu rengi, kendi yarı saydam zemini yüzeye bindirilmiş hâlde.
        Triple("primary/primarySoft@surface", p.accent.primary, primarySoftOver(mode, p, p.surface)),
        Triple("primary/primarySoft@background", p.accent.primary, primarySoftOver(mode, p, p.background)),
        Triple("success/surface", p.success, p.surface),
        Triple("success/background", p.success, p.background),
        Triple("onCarriedContainer/background", p.onCarriedContainer, p.background),
        Triple("info/surface", p.info, p.surface),
        Triple("onSurface/infoContainer", p.onSurface, p.infoContainer),
        Triple("urge/surface", p.urge, p.surface),
        Triple("onSurface/urgeContainer", p.onSurface, p.urgeContainer),
        Triple("onCritical/critical", p.onCritical, p.critical),
        Triple("critical/background", p.critical, p.background),
    )

    @Test
    fun her_tema_ve_vurguda_metin_cifti_en_az_dort_bucuk() {
        val failures = ThemeMode.entries.flatMap { mode ->
            Accent.entries.flatMap { accent ->
                textPairs(mode, Palettes.of(mode, accent))
                    .map { (name, fg, bg) -> Triple("$mode/$accent/$name", ColorMath.contrastRatio(fg, bg), MIN_TEXT) }
                    .filter { (_, ratio, min) -> ratio < min }
            }
        }
        assertTrue(failures.joinToString("\n") { (name, ratio, _) -> "$name = %.2f".format(ratio) }, failures.isEmpty())
    }

    @Test
    fun hesap_bilinen_degerleri_verir() {
        assertEquals(21.0, ColorMath.contrastRatio(0xFF000000, 0xFFFFFFFF), DELTA)
        assertEquals(1.0, ColorMath.contrastRatio(0xFF777777, 0xFF777777), DELTA)
        // Blueprint'in eski açık adaçayısı beyaz yazıyla sınırın altındaydı (karar 0016): test bunu yakalamalı.
        assertEquals(3.81, ColorMath.contrastRatio(0xFFFFFFFF, 0xFF5E8C7B), ROUNDED_DELTA)
    }

    @Test
    fun kritik_renk_yalniz_kritik_jetonda() {
        // Kırmızı ailesi başka bir jetona sızmasın (blueprint C2 renk kuralları): critical yalnız bir alanda.
        ThemeMode.entries.forEach { mode ->
            val p = Palettes.of(mode)
            val others = listOf(p.carried, p.carriedContainer, p.onCarriedContainer, p.urge, p.secondary, p.sun)
            assertTrue("$mode", others.none { it == p.critical })
        }
    }

    private fun primarySoftOver(mode: ThemeMode, p: ToparlaPalette, base: Long): Long {
        val alpha = if (mode == ThemeMode.LIGHT) PRIMARY_SOFT_ALPHA_LIGHT else PRIMARY_SOFT_ALPHA_DARK
        return composite(p.accent.primary, alpha, base)
    }

    private fun composite(fg: Long, alpha: Float, bg: Long): Long =
        listOf(RED_SHIFT, GREEN_SHIFT, 0).fold(OPAQUE) { acc, shift ->
            val f = (fg shr shift) and CHANNEL
            val b = (bg shr shift) and CHANNEL
            acc or (Math.round(f * alpha + b * (1 - alpha)).toLong() shl shift)
        }

    private companion object {
        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val CHANNEL = 0xFFL
        const val OPAQUE = 0xFF000000L
        const val MIN_TEXT = 4.5
        const val DELTA = 1e-9
        const val ROUNDED_DELTA = 0.01
    }
}
