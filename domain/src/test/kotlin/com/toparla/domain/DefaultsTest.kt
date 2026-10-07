package com.toparla.domain

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultsTest {
    @Test
    fun `bildirim butcesi varsayilani ayar araliginin icinde`() {
        assertTrue(
            Defaults.DAILY_NOTIFICATION_BUDGET in
                Defaults.DAILY_NOTIFICATION_BUDGET_MIN..Defaults.DAILY_NOTIFICATION_BUDGET_MAX,
        )
    }

    @Test
    fun `konu ozeti slotlari butceyi tek basina doldurmaz`() {
        assertTrue(Defaults.TOPIC_DIGEST_SLOTS_PER_DAY < Defaults.DAILY_NOTIFICATION_BUDGET_MIN)
    }

    @Test
    fun `israrli takip varsayilan araligi secenekler arasinda`() {
        assertTrue(
            Defaults.PERSISTENT_REMINDER_INTERVAL_MIN in Defaults.PERSISTENT_REMINDER_INTERVAL_OPTIONS_MIN,
        )
    }
}
