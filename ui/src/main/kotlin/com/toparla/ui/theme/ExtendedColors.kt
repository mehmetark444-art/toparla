package com.toparla.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material 3'te karşılığı olmayan anlamsal renkler (blueprint C2 "ek semantik renkler").
 * Eşleme: Güneş → `tertiary` + `sun` halka · Konu/Akış → `info` · Taşınan → `carried` · Dürtü anı → `urge` ·
 * Tamamlama → `success`. `critical` **yalnız** kriz ekranı ve kritik hatırlatma teslimi içindir.
 */
@Immutable
data class ExtendedColors(
    val sun: Color,
    val carried: Color,
    val carriedContainer: Color,
    val onCarriedContainer: Color,
    val success: Color,
    val info: Color,
    val infoContainer: Color,
    val urge: Color,
    val urgeContainer: Color,
    val critical: Color,
    val onCritical: Color,
    val cardBorder: Color,
    val cardShadow: Color,
    /** `primary`'nin hafif zemini: ikincil düğme ve metin eylemi (karar 0016). */
    val primarySoft: Color,
)

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> {
    error("ExtendedColors yalnız ToparlaTheme içinde okunur")
}

/** Paletin Material 3 karşılığı. Dinamik renk (Material You) kullanılmaz (blueprint C1). */
fun ToparlaPalette.toColorScheme(mode: ThemeMode): ColorScheme {
    val base = if (mode == ThemeMode.LIGHT) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary = Color(accent.primary),
        onPrimary = Color(accent.onPrimary),
        primaryContainer = Color(accent.primaryContainer),
        onPrimaryContainer = Color(accent.onPrimaryContainer),
        secondary = Color(secondary),
        onSecondary = Color(background),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSurface),
        tertiary = Color(tertiary),
        onTertiary = Color(background),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onSurface),
        background = Color(background),
        onBackground = Color(onSurface),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceDim = Color(surfaceDim),
        surfaceBright = Color(surface),
        surfaceContainerLowest = Color(background),
        surfaceContainerLow = Color(surface),
        surfaceContainer = Color(surface),
        surfaceContainerHigh = Color(surfaceVariant),
        surfaceContainerHighest = Color(surfaceVariant),
        inverseSurface = Color(onSurface),
        inverseOnSurface = Color(background),
        inversePrimary = Color(accent.primaryContainer),
        outline = Color(outline),
        outlineVariant = Color(outline),
        // M3'ün "error" rolü hata metni içindir; Toparla'da hata kırmızıyla gösterilmez (C2), nötr kalır.
        error = Color(onSurfaceVariant),
        onError = Color(background),
        errorContainer = Color(surfaceVariant),
        onErrorContainer = Color(onSurface),
        scrim = Color.Black,
    )
}

fun ToparlaPalette.toExtendedColors(mode: ThemeMode): ExtendedColors = ExtendedColors(
    sun = Color(sun),
    carried = Color(carried),
    carriedContainer = Color(carriedContainer),
    onCarriedContainer = Color(onCarriedContainer),
    success = Color(success),
    info = Color(info),
    infoContainer = Color(infoContainer),
    urge = Color(urge),
    urgeContainer = Color(urgeContainer),
    critical = Color(critical),
    onCritical = Color(onCritical),
    cardBorder = Color(cardBorder),
    cardShadow = Color(cardShadow),
    primarySoft = Color(accent.primary).copy(alpha = if (mode == ThemeMode.LIGHT) PRIMARY_SOFT_ALPHA_LIGHT else PRIMARY_SOFT_ALPHA_DARK),
)
