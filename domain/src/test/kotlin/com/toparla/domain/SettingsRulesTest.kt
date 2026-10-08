package com.toparla.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalTime

class SettingsRulesTest {
    @Test
    fun `bildirim butcesi izin verilen araliga cekilir`() {
        assertEquals(10, SettingsRules.notificationBudget(null))
        assertEquals(10, SettingsRules.notificationBudget(3))
        assertEquals(15, SettingsRules.notificationBudget(15))
        assertEquals(20, SettingsRules.notificationBudget(99))
    }

    @Test
    fun `israrli takip araligi yalniz sunulan seceneklerden biri olabilir`() {
        assertEquals(30, SettingsRules.persistentIntervalMin(null))
        assertEquals(15, SettingsRules.persistentIntervalMin(15))
        assertEquals(60, SettingsRules.persistentIntervalMin(60))
        assertEquals(30, SettingsRules.persistentIntervalMin(45))
    }

    @Test
    fun `gunun dakikasi saate cevrilir gecersiz deger varsayilana doner`() {
        assertEquals(LocalTime.of(23, 30), SettingsRules.timeOfDay(null, Defaults.SLEEP_START_MINUTE_OF_DAY))
        assertEquals(LocalTime.of(7, 15), SettingsRules.timeOfDay(7 * 60 + 15, Defaults.SLEEP_END_MINUTE_OF_DAY))
        assertEquals(LocalTime.of(7, 30), SettingsRules.timeOfDay(24 * 60, Defaults.SLEEP_END_MINUTE_OF_DAY))
        assertEquals(LocalTime.of(7, 30), SettingsRules.timeOfDay(-1, Defaults.SLEEP_END_MINUTE_OF_DAY))
    }

    @Test
    fun `bitmemis ozelliklerin anahtari varsayilan olarak kapalidir`() {
        FeatureFlag.entries.forEach { assertFalse(it.defaultOn, it.name) }
    }
}
