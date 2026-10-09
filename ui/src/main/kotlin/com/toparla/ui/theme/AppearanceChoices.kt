package com.toparla.ui.theme

/**
 * Ayarlar'daki görünüm seçimleri (blueprint C1–C5). DataStore'da enum adı olarak saklanır; bozuk, eski ya da boş
 * değer sessizce varsayılana döner (blueprint C2: varsayılan koyu, adaçayı; C3: normal yazı).
 */
data class AppearanceChoices(
    val mode: ThemeMode = ThemeMode.DARK,
    val accent: Accent = Accent.SAGE,
    val textSize: TextSizeChoice = TextSizeChoice.NORMAL,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
) {
    companion object {
        fun parse(
            mode: String?,
            accent: String?,
            textSize: String?,
            reduceMotion: Boolean,
            haptics: Boolean,
        ): AppearanceChoices = AppearanceChoices(
            mode = ThemeMode.entries.firstOrNull { it.name == mode } ?: ThemeMode.DARK,
            accent = Accent.entries.firstOrNull { it.name == accent } ?: Accent.SAGE,
            textSize = TextSizeChoice.entries.firstOrNull { it.name == textSize } ?: TextSizeChoice.NORMAL,
            reduceMotion = reduceMotion,
            haptics = haptics,
        )
    }
}
