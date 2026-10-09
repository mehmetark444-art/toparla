package com.toparla.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceChoicesTest {

    @Test
    fun bos_ya_da_bozuk_deger_varsayilana_doner() {
        val parsed = AppearanceChoices.parse(mode = null, accent = "MOR", textSize = "", reduceMotion = false, haptics = true)
        assertEquals(AppearanceChoices(), parsed)
    }

    @Test
    fun gecerli_degerler_okunur() {
        val parsed = AppearanceChoices.parse(mode = "AMOLED", accent = "SKY", textSize = "LARGEST", reduceMotion = true, haptics = false)
        assertEquals(ThemeMode.AMOLED, parsed.mode)
        assertEquals(Accent.SKY, parsed.accent)
        assertEquals(TextSizeChoice.LARGEST, parsed.textSize)
        assertEquals(true, parsed.reduceMotion)
        assertEquals(false, parsed.haptics)
    }

    @Test
    fun kucuk_harf_ad_kabul_edilmez_varsayilana_doner() {
        // Saklama her zaman enum adıyla yapılır; elle girilmiş başka biçim güvenilmez.
        assertEquals(ThemeMode.DARK, AppearanceChoices.parse("light", null, null, reduceMotion = false, haptics = true).mode)
    }
}
