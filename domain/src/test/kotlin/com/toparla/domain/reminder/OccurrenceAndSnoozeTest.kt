package com.toparla.domain.reminder

import com.toparla.domain.reminder.OccurrenceEvent.AUDIT_FOUND_UNFIRED
import com.toparla.domain.reminder.OccurrenceEvent.CARRY
import com.toparla.domain.reminder.OccurrenceEvent.DEFINITION_CHANGED
import com.toparla.domain.reminder.OccurrenceEvent.FIRED
import com.toparla.domain.reminder.OccurrenceEvent.LADDER_EXHAUSTED
import com.toparla.domain.reminder.OccurrenceEvent.MARKED_DONE
import com.toparla.domain.reminder.OccurrenceEvent.OPENED
import com.toparla.domain.reminder.OccurrenceEvent.SKIP
import com.toparla.domain.reminder.OccurrenceEvent.SNOOZE
import com.toparla.domain.reminder.OccurrenceState.CANCELLED
import com.toparla.domain.reminder.OccurrenceState.CARRIED
import com.toparla.domain.reminder.OccurrenceState.DELIVERED
import com.toparla.domain.reminder.OccurrenceState.DONE
import com.toparla.domain.reminder.OccurrenceState.EXPIRED
import com.toparla.domain.reminder.OccurrenceState.MISSED_DETECTED
import com.toparla.domain.reminder.OccurrenceState.PLANNED
import com.toparla.domain.reminder.OccurrenceState.SEEN
import com.toparla.domain.reminder.OccurrenceState.SKIPPED
import com.toparla.domain.reminder.OccurrenceState.SNOOZED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class OccurrenceAndSnoozeTest {
    private fun run(vararg events: OccurrenceEvent) = events.fold(PLANNED, OccurrenceStateMachine::apply)

    @Test
    fun `olagan yol planlandi teslim goruldu yapildi`() {
        assertEquals(DONE, run(FIRED, OPENED, MARKED_DONE))
    }

    @Test
    fun `bildirim eylemi gorulmeden de sonuclandirir`() {
        assertEquals(DONE, run(FIRED, MARKED_DONE))
        assertEquals(SNOOZED, run(FIRED, SNOOZE))
        assertEquals(SKIPPED, run(FIRED, SKIP))
    }

    @Test
    fun `yanitsiz merdiven suresi doldu sonra tasinan`() {
        assertEquals(EXPIRED, run(FIRED, LADDER_EXHAUSTED))
        assertEquals(CARRIED, run(FIRED, OPENED, LADDER_EXHAUSTED, CARRY))
    }

    @Test
    fun `suresi dolan is gec de olsa tamamlanabilir`() {
        assertEquals(DONE, run(FIRED, LADDER_EXHAUSTED, MARKED_DONE))
    }

    @Test
    fun `tanim degisince planlanan iptal olur`() {
        assertEquals(CANCELLED, run(DEFINITION_CHANGED))
    }

    @Test
    fun `denetcinin buldugu teslim gec teslim edilebilir`() {
        assertEquals(MISSED_DETECTED, run(AUDIT_FOUND_UNFIRED))
        assertEquals(DELIVERED, run(AUDIT_FOUND_UNFIRED, FIRED))
    }

    @Test
    fun `teslim edilmis is icin denetci kacan kaydi acmaz`() {
        assertNull(OccurrenceStateMachine.next(DELIVERED, AUDIT_FOUND_UNFIRED))
    }

    @Test
    fun `cift teslim ve cift dokunus durumu bozmaz`() {
        assertEquals(DELIVERED, run(FIRED, FIRED))
        assertEquals(DONE, run(FIRED, MARKED_DONE, MARKED_DONE))
        assertEquals(DONE, run(FIRED, MARKED_DONE, SNOOZE))
    }

    @Test
    fun `son durumlardan hicbir gecis yok`() {
        OccurrenceState.entries.filter { it.isTerminal }.forEach { state ->
            OccurrenceEvent.entries.forEach { event -> assertNull(OccurrenceStateMachine.next(state, event), "$state + $event") }
        }
    }

    @Test
    fun `son olmayan her durumun en az bir cikisi var`() {
        OccurrenceState.entries.filterNot { it.isTerminal }.forEach { state ->
            assertTrue(OccurrenceEvent.entries.any { OccurrenceStateMachine.next(state, it) != null }, "$state")
        }
    }

    @Test
    fun `planlanmis is teslim edilmeden yapildi sayilmaz`() {
        assertNull(OccurrenceStateMachine.next(PLANNED, MARKED_DONE))
        assertNull(OccurrenceStateMachine.next(SEEN, OPENED))
    }

    @Test
    fun `varsayilan erteleme 10 dakika`() {
        assertEquals(SnoozeDecision.Snooze(Duration.ofMinutes(10)), SnoozePolicy.decide(previousSnoozes = 0))
    }

    @Test
    fun `secilen sure kullanilir gecersiz sure varsayilana doner`() {
        assertEquals(SnoozeDecision.Snooze(Duration.ofHours(2)), SnoozePolicy.decide(1, Duration.ofHours(2)))
        assertEquals(SnoozeDecision.Snooze(Duration.ofMinutes(10)), SnoozePolicy.decide(1, Duration.ZERO))
    }

    @Test
    fun `uc ertelemeden sonra tasi ya da atla sorulur`() {
        assertEquals(SnoozeDecision.Snooze(Duration.ofMinutes(10)), SnoozePolicy.decide(2))
        assertEquals(SnoozeDecision.AskCarryOrSkip, SnoozePolicy.decide(3))
        assertEquals(SnoozeDecision.AskCarryOrSkip, SnoozePolicy.decide(7))
    }

    @Test
    fun `ilacta erteleme en cok 30 dakika`() {
        assertEquals(SnoozeDecision.Snooze(Duration.ofMinutes(30)), SnoozePolicy.decide(0, Duration.ofHours(1), isMedication = true))
        assertEquals(SnoozeDecision.Snooze(Duration.ofMinutes(15)), SnoozePolicy.decide(0, Duration.ofMinutes(15), isMedication = true))
    }

    @Test
    fun `ertelenen teslim yeni anahtar alir`() {
        assertEquals("r1@1000#s1", SnoozePolicy.snoozedKey("r1@1000", 1))
        assertTrue(SnoozePolicy.snoozedKey("r1@1000", 1) != SnoozePolicy.snoozedKey("r1@1000", 2))
    }

    @Test
    fun `negatif erteleme sayisi reddedilir`() {
        assertThrows(IllegalArgumentException::class.java) { SnoozePolicy.decide(-1) }
    }
}
