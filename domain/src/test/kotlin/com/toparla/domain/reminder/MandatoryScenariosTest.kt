package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * F2.9: Bölüm I'nın 18 zorunlu sahte saatli senaryosundan **saf mantıkla ifade edilebilenler**.
 *
 * Bu dosyada: aynı dakikada 5 olay · yeniden başlatma ortasında teslim · güncelleme ortasında teslim ·
 * saat elle ileri/geri · saat dilimi değişimi · 500 tanım · kritik + odak çakışması · bildirimi kaydırarak
 * silme · teslim edilmiş ama kaydedilmemiş.
 * Başka dosyada: gece yarısı ve ay/yıl sonu (`ReminderPlannerTest`), 3 ardışık erteleme (`OccurrenceAndSnoozeTest`).
 * Android tarafında (`:reminders`, F2-D ve F2-F) sınanacaklar: DND açık · bildirim izni kapalı · tam ekran izni
 * kapalı · ağ yok · depolama dolu · çift doz denemesi.
 */
class MandatoryScenariosTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val followUp = FollowUpConfig(Duration.ofMinutes(30), zone, LocalTime.of(23, 30), LocalTime.of(7, 30))

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    private fun def(id: String, klass: ReminderClass, start: String, recurrence: Recurrence = Recurrence.Once) =
        ReminderDef(id, klass, LocalDateTime.parse(start), zone, recurrence)

    private fun applied(result: PlanResult) = result.toSchedule.map { ScheduledAlarm(it.key, it.fireAt) }

    @Test
    fun `ayni dakikada bes olay bes ayri alarm ama tek kart`() {
        val defs = (1..5).map { def("r$it", ReminderClass.entries[it % 4], "2026-10-08T09:00") }

        val planned = ReminderPlanner.plan(at("2026-10-08T08:00"), defs, emptyList()).toSchedule

        assertEquals(5, planned.map { it.key }.toSet().size)
        val card = DeliveryGrouping.group(planned).single()
        assertEquals(5, card.items.size)
        assertEquals(ReminderClass.CRITICAL, card.klass)
    }

    @Test
    fun `yeniden baslatma ortasinda vakti gecen teslim denetciyle bulunur ve gec teslim edilir`() {
        val defs = listOf(def("r", ReminderClass.CRITICAL, "2026-10-08T09:00"))
        val scheduled = applied(ReminderPlanner.plan(at("2026-10-08T08:00"), defs, emptyList()))

        // Telefon 08:50–09:20 arası kapalıydı; açılışta yeniden planlama geçmiş anı kurmaz…
        val afterBoot = ReminderPlanner.plan(at("2026-10-08T09:20"), defs, scheduled)
        assertTrue(afterBoot.toSchedule.isEmpty())
        // …ama denetçi ateşlenme kaydı olmayan teslimi bulur; durum makinesi geç teslime izin verir.
        val unfired = DeliveryAuditor.findUnfired(at("2026-10-08T09:20"), scheduled, firedKeys = emptySet())
        assertEquals(scheduled, unfired)
        val state = OccurrenceStateMachine.apply(OccurrenceState.PLANNED, OccurrenceEvent.AUDIT_FOUND_UNFIRED)
        assertEquals(OccurrenceState.DELIVERED, OccurrenceStateMachine.apply(state, OccurrenceEvent.FIRED))
        assertEquals(Duration.ofMinutes(20), DeliveryGrouping.lateBy(scheduled.single().fireAt, at("2026-10-08T09:20")))
    }

    @Test
    fun `guncelleme ortasinda alarmlar silinse de yeniden planlama aynisini kurar`() {
        val defs = listOf(def("a", ReminderClass.CRITICAL, "2026-10-08T12:00"), def("b", ReminderClass.NORMAL, "2026-10-08T13:00"))
        val before = ReminderPlanner.plan(at("2026-10-08T10:00"), defs, emptyList()).toSchedule

        // Paket güncellemesi sistem alarmlarını sildi: mevcut liste boş, saat biraz ilerledi.
        val after = ReminderPlanner.plan(at("2026-10-08T10:05"), defs, emptyList()).toSchedule

        assertEquals(before, after)
    }

    @Test
    fun `saat ileri alininca araya dusen teslim kurulmaz denetciye kalir geri alininca eksik tamamlanir`() {
        val defs = listOf(def("r", ReminderClass.IMPORTANT, "2026-10-08T10:00", Recurrence.Daily))
        val scheduled = applied(ReminderPlanner.plan(at("2026-10-08T09:00"), defs, emptyList()))

        // Saat 09:00'dan 11:00'e alındı: 10:00 teslimi artık geçmişte.
        val forward = ReminderPlanner.plan(at("2026-10-08T11:00"), defs, scheduled)
        assertTrue(forward.toSchedule.none { it.fireAt == at("2026-10-08T10:00") })
        assertEquals(1, DeliveryAuditor.findUnfired(at("2026-10-08T11:00"), scheduled, emptySet()).size)

        // Saat 11:00'den 08:00'e geri alındı: hiçbir teslim iki kez kurulmaz, pencereden çıkan iptal edilir.
        val afterForward = scheduled.filter { it.fireAt.isAfter(at("2026-10-08T11:00")) } + applied(forward)
        val back = ReminderPlanner.plan(at("2026-10-08T08:00"), defs, afterForward)
        val finalKeys = afterForward.map { it.key }.filterNot { it in back.toCancel } + back.toSchedule.map { it.key }
        assertEquals(finalKeys.size, finalKeys.toSet().size)
        assertTrue(ReminderPlanner.keyOf("r", at("2026-10-08T10:00")) in finalKeys)
    }

    @Test
    fun `saat dilimi degisince teslim hatirlatmanin kendi diliminde kalir`() {
        // Tanım İstanbul saatine göre; telefon Berlin'e taşındı. Anlık zaman değişmez.
        val defs = listOf(def("r", ReminderClass.CRITICAL, "2026-10-09T09:00"))
        val now = at("2026-10-08T20:00")

        val inIstanbul = ReminderPlanner.plan(now, defs, emptyList()).toSchedule.single()
        val afterTravel = ReminderPlanner.plan(now, defs, listOf(ScheduledAlarm(inIstanbul.key, inIstanbul.fireAt)))

        assertEquals(at("2026-10-09T09:00"), inIstanbul.fireAt)
        assertTrue(afterTravel.isEmpty)
    }

    @Test
    fun `bes yuz tanimda sinir korunur kritikler eksiksiz kalir`() {
        val defs = (0 until 500).map {
            val klass = if (it % 5 == 0) ReminderClass.CRITICAL else ReminderClass.NORMAL
            def("r$it", klass, "2026-10-08T${"%02d".format(8 + it % 12)}:${"%02d".format(it % 60)}", Recurrence.Daily)
        }

        val planned = ReminderPlanner.plan(at("2026-10-08T07:00"), defs, emptyList()).toSchedule
        val unlimited = ReminderPlanner.plan(at("2026-10-08T07:00"), defs, emptyList(), maxPending = Int.MAX_VALUE).toSchedule

        // 100 kritik tanım × 48 saatte 2 teslim = 200: sınırın tamamı kritiklere gider.
        assertEquals(ReminderPlanner.DEFAULT_MAX_PENDING, planned.size)
        assertEquals(unlimited.count { it.klass == ReminderClass.CRITICAL }, planned.count { it.klass == ReminderClass.CRITICAL })
    }

    @Test
    fun `kritik olay odak oturumuyla cakisinca merdiven surer israrli takip odak bitene dek susar`() {
        val defs = listOf(def("r", ReminderClass.CRITICAL, "2026-10-08T10:00"))
        val flight = InFlightOccurrence("r@1", "r", ReminderClass.CRITICAL, at("2026-10-08T10:00"), persistent = true)
        val focus = followUp.copy(silentUntil = at("2026-10-08T10:50"))

        val result = ReminderPlanner.plan(at("2026-10-08T10:01"), defs, emptyList(), inFlight = listOf(flight), followUp = focus)

        val byKey = result.toSchedule.associateBy { it.key }
        assertEquals(at("2026-10-08T10:02"), byKey.getValue(ReminderPlanner.ladderKey("r@1", 1)).fireAt)
        assertEquals(at("2026-10-08T10:50"), byKey.getValue(ReminderPlanner.followUpKey("r@1", 0)).fireAt)
    }

    @Test
    fun `bildirim kaydirilarak silinince is cozulmus sayilmaz merdiven ve takip surer`() {
        // Kaydırma bir yanıt değildir: durum TESLİM'de kalır, teslim "yanıt bekleyen" olarak planlayıcıya gelir.
        val state = OccurrenceState.DELIVERED
        assertTrue(!state.isTerminal)
        val defs = listOf(def("r", ReminderClass.NORMAL, "2026-10-08T10:00"))
        val flight = InFlightOccurrence("r@1", "r", ReminderClass.NORMAL, at("2026-10-08T10:00"), persistent = true)

        val result = ReminderPlanner.plan(at("2026-10-08T10:10"), defs, emptyList(), inFlight = listOf(flight), followUp = followUp)

        assertEquals(at("2026-10-08T10:30"), result.toSchedule.single().fireAt)
    }

    @Test
    fun `teslim edilmis ama kaydedilmemis teslim ikinci kez gosterilse de durum bozulmaz`() {
        // Bildirim gösterildi, FIRED kaydı yazılamadan telefon kapandı: denetçi teslimi "ateşlenmemiş" bulur
        // ve geç teslim eder (çift gösterim, sessiz kayıptan iyidir). Çift FIRED durumu değiştirmez.
        val scheduled = listOf(ScheduledAlarm("r@1", at("2026-10-08T09:00")))
        assertEquals(1, DeliveryAuditor.findUnfired(at("2026-10-08T09:30"), scheduled, firedKeys = emptySet()).size)

        val delivered = OccurrenceStateMachine.apply(OccurrenceState.PLANNED, OccurrenceEvent.FIRED)
        assertEquals(OccurrenceState.DELIVERED, OccurrenceStateMachine.apply(delivered, OccurrenceEvent.FIRED))
        // Kayıt yazıldıktan sonra denetçi aynı teslimi bir daha bulmaz.
        assertTrue(DeliveryAuditor.findUnfired(at("2026-10-08T09:30"), scheduled, firedKeys = setOf("r@1")).isEmpty())
    }
}
