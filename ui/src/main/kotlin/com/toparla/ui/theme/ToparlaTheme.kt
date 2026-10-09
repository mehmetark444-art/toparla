package com.toparla.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Ayarlar → Yazı boyutu çarpanı (blueprint C3); sistem yazı ölçeğinin üstüne uygulanır. */
enum class TextSizeChoice(val multiplier: Float) {
    SMALL(0.9f),
    NORMAL(1.0f),
    LARGE(1.15f),
    LARGEST(1.3f),
}

val LocalToparlaTextStyles = staticCompositionLocalOf { ToparlaTypeScale }
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Bileşenlerin Toparla'ya özgü jetonlara erişimi (`MaterialTheme` gibi). */
object ToparlaTheme {
    val extended: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val type: ToparlaTextStyles
        @Composable @ReadOnlyComposable
        get() = LocalToparlaTextStyles.current

    /** "Animasyonları azalt" (blueprint C5): açıkken yalnız opaklık geçişi. */
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current

    val haptics: ToparlaHaptics
        @Composable @ReadOnlyComposable
        get() = LocalToparlaHaptics.current
}

/**
 * Uygulamanın teması (blueprint C1–C5, karar 0016). Ayar değerleri çağırandan gelir (DataStore → ViewModel);
 * tema kendisi hiçbir şey okumaz, bu yüzden ekran görüntüsü testinde her birleşim doğrudan kurulabilir.
 */
@Composable
fun ToparlaTheme(
    mode: ThemeMode = ThemeMode.DARK,
    accent: Accent = Accent.SAGE,
    textSize: TextSizeChoice = TextSizeChoice.NORMAL,
    reduceMotion: Boolean = false,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = remember(mode, accent) { Palettes.of(mode, accent) }
    val colorScheme = remember(palette, mode) { palette.toColorScheme(mode) }
    val extended = remember(palette, mode) { palette.toExtendedColors(mode) }
    val systemDensity = LocalDensity.current
    val density = remember(systemDensity, textSize) {
        Density(density = systemDensity.density, fontScale = systemDensity.fontScale * textSize.multiplier)
    }
    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalToparlaTextStyles provides ToparlaTypeScale,
        LocalReduceMotion provides reduceMotion,
        LocalToparlaHaptics provides rememberViewHaptics(hapticsEnabled),
        LocalDensity provides density,
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = ToparlaTypeScale.toTypography(), content = content)
    }
}
