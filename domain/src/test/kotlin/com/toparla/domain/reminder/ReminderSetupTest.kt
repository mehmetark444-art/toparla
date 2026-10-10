package com.toparla.domain.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** F2.35–F2.38'in saf kuralları: hazır zaman seçenekleri, taslak doğrulama, sağlık raporu, kendi kendini sınama. */
class ReminderSetupTest {
    private val zone = ZoneId.of("Europe/Istanbul")

    private fun at(local: String): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun `hazir zaman secenekleri simdiye gore cozulur`() {
        val now = at("2026-10-09T15:07:42")

        assertEquals(LocalDateTime.parse("2026-10-09T15:17:42"), QuickTime.IN_10_MIN.resolve(now, zone))
        assertEquals(LocalDateTime.parse("2026-10-09T16:07:42"), QuickTime.IN_1_HOUR.resolve(now, zone))
        assertEquals(LocalDateTime.parse("2026-10-09T20:00"), QuickTime.TONIGHT.resolve(now, zone))
        assertEquals(LocalDateTime.parse("2026-10-10T09:00"), QuickTime.TOMORROW_MORNING.resolve(now, zone))
    }

    @Test
    fun `aksam saati gectiyse bu aksam secenegi yarin aksama kayar`() {
        assertEquals(LocalDateTime.parse("2026-10-10T20:00"), QuickTime.TONIGHT.resolve(at("2026-10-09T20:00"), zone))
        assertEquals(LocalDateTime.parse("2026-10-10T20:00"), QuickTime.TONIGHT.resolve(at("2026-10-09T23:30"), zone))
    }

    @Test
    fun `taslak basligi kirpilir bos baslik ve gecmis zaman reddedilir`() {
        val now = at("2026-10-09T15:00")
        val future = LocalDateTime.parse("2026-10-09T16:00")

        assertEquals("Faturayı öde", ReminderDraft.validate("  Faturayı öde  ", future, now, zone)?.title)
        assertNull(ReminderDraft.validate("   ", future, now, zone))
        assertNull(ReminderDraft.validate("Geçmiş", LocalDateTime.parse("2026-10-09T14:59"), now, zone))
        assertEquals(ReminderDraft.MAX_TITLE, ReminderDraft.validate("x".repeat(200), future, now, zone)?.title?.length)
    }

    private val healthy = HealthSnapshot(
        notifications = true, exactAlarms = true, fullScreen = true, batteryExempt = true,
        dndAccess = true, autoStartConfirmed = true, weakenedChannels = emptySet(), standbyBucket = StandbyBucket.ACTIVE,
        upcomingReminders = 3, lastDelivery = DeliveryTiming(at("2026-10-09T10:00"), at("2026-10-09T10:00")),
        recentDeliveries = emptyList(), lastSelfTest = null,
    )
    private val now = at("2026-10-09T15:00")

    private fun report(snapshot: HealthSnapshot = healthy) = HealthReport.of(snapshot, now)

    @Test
    fun `her sey yerindeyse duzeltilecek bir sey yoktur`() {
        val report = report()

        assertTrue(report.toFix.isEmpty())
        assertEquals(HealthCheck.entries.toSet(), report.ok.toSet())
    }

    @Test
    fun `eksik ayarlar onem sirasiyla duzeltilecekler listesine girer`() {
        val report = report(healthy.copy(batteryExempt = false, notifications = false, autoStartConfirmed = false))

        assertEquals(listOf(HealthCheck.NOTIFICATIONS, HealthCheck.AUTO_START, HealthCheck.BATTERY), report.toFix)
        assertFalse(HealthCheck.NOTIFICATIONS in report.ok)
    }

    @Test
    fun `kendi kendini sinama vaktinde geleni ve gelmeyeni ayirir`() {
        val planned = at("2026-10-09T15:00:20")
        val wait = SelfTest.TIMEOUT

        assertEquals(SelfTestResult.Waiting, SelfTest.evaluate(planned, null, at("2026-10-09T15:00:30")))
        assertEquals(
            SelfTestResult.Arrived(Duration.ofSeconds(1), appWasClosed = false),
            SelfTest.evaluate(planned, at("2026-10-09T15:00:21"), at("2026-10-09T15:00:25")),
        )
        assertEquals(SelfTestResult.NotArrived, SelfTest.evaluate(planned, null, planned.plus(wait).plusSeconds(1)))
    }

    @Test
    fun `tolerans disinda gelen deneme ulasti sayilmaz - uygulama acilinca yapilan gec teslim basari degildir`() {
        val planned = at("2026-10-09T15:00:20")
        val tolerance = DeliveryGrouping.LATE_TOLERANCE

        // Sınırın kendisi vaktinde sayılır; bir saniye sonrası geçtir (proje beyni H37: eşiğin iki yanı da sınanır).
        assertEquals(
            SelfTestResult.Arrived(tolerance, appWasClosed = true),
            SelfTest.evaluate(planned, planned.plus(tolerance), planned.plusSeconds(100), wokeApp = true),
        )
        assertEquals(
            SelfTestResult.Late(tolerance.plusSeconds(1)),
            SelfTest.evaluate(planned, planned.plus(tolerance).plusSeconds(1), planned.plusSeconds(100), wokeApp = true),
        )
        assertEquals(SelfTestResult.Late(Duration.ofSeconds(95)), SelfTest.evaluate(planned, planned.plusSeconds(95), planned.plusSeconds(100)))
    }

    @Test
    fun `deneme ancak alarm kapali uygulamayi uyandirdiysa kapaliyken ulasti sayilir`() {
        val planned = at("2026-10-09T15:00:20")

        assertEquals(SelfTestOutcome.ARRIVED_CLOSED, SelfTest.evaluate(planned, planned, planned, wokeApp = true).outcome())
        assertEquals(SelfTestOutcome.ARRIVED_OPEN, SelfTest.evaluate(planned, planned, planned, wokeApp = false).outcome())
        assertEquals(SelfTestOutcome.LATE, SelfTest.evaluate(planned, planned.plusSeconds(200), planned.plusSeconds(200)).outcome())
        assertEquals(SelfTestOutcome.NOT_ARRIVED, SelfTest.evaluate(planned, null, planned.plusSeconds(200)).outcome())
        assertNull(SelfTest.evaluate(planned, null, planned).outcome())
        assertTrue(SelfTestOutcome.ARRIVED_CLOSED.passed && SelfTestOutcome.ARRIVED_OPEN.passed)
        assertFalse(SelfTestOutcome.LATE.passed || SelfTestOutcome.NOT_ARRIVED.passed)
    }

    @Test
    fun `surec alarmdan hemen once dogduysa alarm uygulamayi uyandirmistir`() {
        assertTrue(ColdDelivery.wokeApp(Duration.ofMillis(400)))
        assertTrue(ColdDelivery.wokeApp(ColdDelivery.MAX_PROCESS_AGE))
        assertFalse(ColdDelivery.wokeApp(ColdDelivery.MAX_PROCESS_AGE.plusMillis(1)))
        assertFalse(ColdDelivery.wokeApp(Duration.ofMinutes(10)))
        // Okunamayan (negatif) yaş kanıt sayılmaz.
        assertFalse(ColdDelivery.wokeApp(Duration.ofMillis(-1)))
    }

    @Test
    fun `sinama sonucu metne yazilip geri okunur bozuk kayit yok sayilir`() {
        val record = SelfTestRecord(at("2026-10-09T08:51:35"), SelfTestOutcome.ARRIVED_CLOSED)

        assertEquals(record, SelfTestRecord.decode(record.encode()))
        assertNull(SelfTestRecord.decode(null))
        assertNull(SelfTestRecord.decode("bozuk"))
        assertNull(SelfTestRecord.decode("12|YOK_BOYLE_SONUC"))
        assertNull(SelfTestRecord.decode("abc|LATE"))
    }

    @Test
    fun `basarisiz sinamada bakilacaklar - teslimi etkileyen eksikler ve okunamayan otomatik baslatma`() {
        // Her şey yerinde görünse de otomatik başlatma okunamaz: Kullanıcı onaylamış olsa bile yeniden bakılır.
        assertEquals(listOf(HealthCheck.AUTO_START), SelfTest.likelyCauses(report()))

        val missing = report(healthy.copy(batteryExempt = false, fullScreen = false, dndAccess = false, notifications = false))
        // Tam ekran ve Rahatsız Etme erişimi deneme hatırlatmasının ulaşmasını etkilemez; listeye girmez.
        assertEquals(listOf(HealthCheck.NOTIFICATIONS, HealthCheck.AUTO_START, HealthCheck.BATTERY), SelfTest.likelyCauses(missing))
    }

    @Test
    fun `kisilan bildirim kanali duzeltilecekler listesine girer ve hangi sinif oldugu rapora tasinir`() {
        val result = report(healthy.copy(weakenedChannels = setOf(ReminderClass.IMPORTANT)))

        assertEquals(listOf(HealthCheck.CHANNELS), result.toFix)
        assertEquals(setOf(ReminderClass.IMPORTANT), result.weakenedChannels)
        assertTrue(HealthCheck.CHANNELS in report().ok)
    }

    @Test
    fun `kanal onemi dusuruldugunde ya da kapatildiginda kisilmis sayilir yukseltilince sayilmaz`() {
        val high = 4
        val default = 3

        assertTrue(ChannelHealth.weakened(designedImportance = high, actualImportance = 2))
        assertTrue(ChannelHealth.weakened(designedImportance = default, actualImportance = 0))
        assertFalse(ChannelHealth.weakened(designedImportance = default, actualImportance = default))
        // Kullanıcı kanalı daha belirgin yaptıysa bu bir eksik değildir.
        assertFalse(ChannelHealth.weakened(designedImportance = default, actualImportance = high))
    }

    @Test
    fun `bekleme kovasi android degerinden cozulur - hedef etkin ya da calisma kumesi`() {
        assertEquals(
            listOf(StandbyBucket.EXEMPTED, StandbyBucket.ACTIVE, StandbyBucket.WORKING_SET, StandbyBucket.FREQUENT, StandbyBucket.RARE, StandbyBucket.RESTRICTED),
            listOf(5, 10, 20, 30, 40, 45).map(StandbyBucket::of),
        )
        // Tanınmayan değer bir üst kovaya düşer; "hiç kullanılmamış" (50) en kötü kova sayılır.
        assertEquals(StandbyBucket.WORKING_SET, StandbyBucket.of(15))
        assertEquals(StandbyBucket.RESTRICTED, StandbyBucket.of(50))

        assertTrue(listOf(StandbyBucket.EXEMPTED, StandbyBucket.ACTIVE, StandbyBucket.WORKING_SET).all { it.healthy })
        assertFalse(listOf(StandbyBucket.FREQUENT, StandbyBucket.RARE, StandbyBucket.RESTRICTED).any { it.healthy })
        assertEquals(listOf(HealthCheck.STANDBY_BUCKET), report(healthy.copy(standbyBucket = StandbyBucket.RARE)).toFix)
        assertEquals(StandbyBucket.RARE, report(healthy.copy(standbyBucket = StandbyBucket.RARE)).standbyBucket)
    }

    @Test
    fun `vaktinde ulasmayanlar - son yedi gunde tolerans disinda gelen ya da gosterilemeyen teslimler`() {
        fun delivery(planned: String, lateSec: Long, blocked: Boolean = false) = DeliveryTiming(at(planned), at(planned).plusSeconds(lateSec), blocked)
        val deliveries = listOf(
            delivery("2026-10-09T09:00", lateSec = 0),
            // Toleransın kendisi (60 sn) vaktindedir; bir saniye fazlası değildir.
            delivery("2026-10-08T09:00", lateSec = 60),
            delivery("2026-10-08T12:00", lateSec = 61),
            delivery("2026-10-07T09:00", lateSec = 240),
            // Vaktinde çaldı ama bildirim kapalı olduğu için gösterilemedi.
            delivery("2026-10-06T09:00", lateSec = 0, blocked = true),
            // Pencerenin tam sınırı sayılır; bir saniye eskisi sayılmaz.
            delivery("2026-10-02T15:00", lateSec = 300),
            delivery("2026-10-02T14:59:59", lateSec = 300),
        )

        assertEquals(4, DeliveryHealth.missed(deliveries, now))
        assertEquals(4, report(healthy.copy(recentDeliveries = deliveries)).missedLastWeek)
        assertEquals(0, report().missedLastWeek)
        assertEquals(Duration.ofSeconds(240), deliveries[3].lateBy)
        assertTrue(deliveries[1].onTime)
        assertFalse(deliveries[4].onTime)
    }

    @Test
    fun `nabiz kisilan kanal icin uyarir bekleme kovasi icin ayrica uyarmaz`() {
        // Kısılan kanal hatırlatmayı sessizleştirir: uyarı doğurur.
        assertTrue(Heartbeat.decide(report(healthy.copy(weakenedChannels = setOf(ReminderClass.CRITICAL))), emptySet()).warn)
        // Kovanın çaresi pil muafiyetidir ve o zaten izlenir; kova tek başına ikinci bir uyarı doğurmaz.
        val bucketOnly = Heartbeat.decide(report(healthy.copy(standbyBucket = StandbyBucket.RESTRICTED)), emptySet())
        assertTrue(bucketOnly.problems.isEmpty())
        assertFalse(bucketOnly.warn)
    }

    @Test
    fun `nabiz yalniz hatirlatmayi kacirabilecek eksikler icin uyarir`() {
        val report = report(healthy.copy(fullScreen = false, dndAccess = false, autoStartConfirmed = false))
        val decision = Heartbeat.decide(report, alreadyWarned = emptySet())
        // Rahatsız Etme erişimi ve otomatik başlatma uyarı doğurmaz; tam ekran doğurur.
        assertEquals(setOf(HealthCheck.FULL_SCREEN), decision.problems)
        assertTrue(decision.warn)
    }

    @Test
    fun `nabiz ayni eksik icin yeniden uyarmaz yeni eksik cikinca uyarir hepsi duzelince susar`() {
        val oneMissing = report(healthy.copy(batteryExempt = false))
        assertFalse(Heartbeat.decide(oneMissing, alreadyWarned = setOf(HealthCheck.BATTERY)).warn)

        val twoMissing = report(healthy.copy(batteryExempt = false, notifications = false))
        val second = Heartbeat.decide(twoMissing, alreadyWarned = setOf(HealthCheck.BATTERY))
        assertTrue(second.warn)
        assertEquals(setOf(HealthCheck.NOTIFICATIONS, HealthCheck.BATTERY), second.problems)

        val allGood = Heartbeat.decide(report(), alreadyWarned = second.problems)
        assertTrue(allGood.problems.isEmpty())
        assertFalse(allGood.warn)
    }

    @Test
    fun `kurulum sihirbazi her zaman bes temel adimi listeler - yerinde olan tamam gorunur`() {
        val fresh = report(healthy.copy(autoStartConfirmed = false, batteryExempt = false))
        val progress = SetupWizard.progress(fresh, recentsLockConfirmed = false)

        assertEquals(
            listOf(SetupStep.NOTIFICATIONS, SetupStep.AUTO_START, SetupStep.BATTERY, SetupStep.RECENTS_LOCK, SetupStep.SELF_TEST),
            progress.steps.map { it.step },
        )
        assertEquals(listOf(true, false, false, false, false), progress.steps.map { it.done })
        assertEquals(4, progress.remaining)
        assertEquals(SetupStep.AUTO_START, progress.next)
        assertFalse(progress.done)
    }

    @Test
    fun `kurulumda bir adimdan sonra siradaki eksik adima gecilir - tamam olan atlanir sonda null doner`() {
        val progress = SetupWizard.progress(report(healthy.copy(autoStartConfirmed = false)), recentsLockConfirmed = false)

        // Pil yerinde olduğu için otomatik başlatmadan sonra doğrudan kilide geçilir.
        assertEquals(SetupStep.RECENTS_LOCK, progress.after(SetupStep.AUTO_START))
        assertEquals(SetupStep.SELF_TEST, progress.after(SetupStep.RECENTS_LOCK))
        assertNull(progress.after(SetupStep.SELF_TEST))
        // Atlanan adım geride eksik kalır; ileri gidiş ona dönmez.
        assertEquals(SetupStep.AUTO_START, progress.next)
        // Listede olmayan adım için sıradaki yoktur.
        assertNull(progress.after(SetupStep.CHANNELS))
    }

    @Test
    fun `kurulumda kosullu adimlar yalniz eksikse gorunur ve onem sirasina girer`() {
        val missing = report(healthy.copy(exactAlarms = false, fullScreen = false, weakenedChannels = setOf(ReminderClass.CRITICAL), dndAccess = false))
        val steps = SetupWizard.progress(missing, recentsLockConfirmed = true).steps.map { it.step }

        // Rahatsız Etme erişimi isteğe bağlıdır ve bekleme kovasının çaresi pil adımıdır: ikisi de adım olmaz.
        assertEquals(
            listOf(
                SetupStep.NOTIFICATIONS, SetupStep.CHANNELS, SetupStep.EXACT_ALARMS, SetupStep.AUTO_START,
                SetupStep.BATTERY, SetupStep.FULL_SCREEN, SetupStep.RECENTS_LOCK, SetupStep.SELF_TEST,
            ),
            steps,
        )
    }

    @Test
    fun `kurulum ancak deneme gecince biter - gec gelen deneme adimi kapatmaz`() {
        fun progressWith(outcome: SelfTestOutcome?) = SetupWizard.progress(
            report(healthy.copy(lastSelfTest = outcome?.let { SelfTestRecord(now, it) })),
            recentsLockConfirmed = true,
        )

        assertEquals(SetupStep.SELF_TEST, progressWith(null).next)
        assertEquals(1, progressWith(SelfTestOutcome.LATE).remaining)
        assertEquals(1, progressWith(SelfTestOutcome.NOT_ARRIVED).remaining)
        assertTrue(progressWith(SelfTestOutcome.ARRIVED_OPEN).done)
        val finished = progressWith(SelfTestOutcome.ARRIVED_CLOSED)
        assertTrue(finished.done)
        assertNull(finished.next)
        assertEquals(0, finished.remaining)
    }
}
