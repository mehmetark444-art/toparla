package com.toparla.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Dokunsal geri bildirim (blueprint C5). Bileşenler titreşimi bu arayüzden ister; Android karşılığı
 * `AndroidHaptics.kt`'de. Ayarlar'daki tek anahtar kapatınca `NoHaptics` sağlanır (kritik alarm bundan bağımsız).
 */
interface ToparlaHaptics {
    /** Yakalama, tamamlama, birincil eylem. */
    fun confirm()

    /** Chip seçimi. */
    fun tick()
}

object NoHaptics : ToparlaHaptics {
    override fun confirm() = Unit

    override fun tick() = Unit
}

val LocalToparlaHaptics = staticCompositionLocalOf<ToparlaHaptics> { NoHaptics }
