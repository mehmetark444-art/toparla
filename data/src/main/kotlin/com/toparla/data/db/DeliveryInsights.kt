package com.toparla.data.db

import com.toparla.domain.reminder.DeliveryTiming
import java.time.Instant

/** Hatırlatma Sağlığı ekranının veritabanından gelen bilgileri. */
data class HealthFacts(val upcomingReminders: Int, val lastDelivery: DeliveryTiming?, val recentDeliveries: List<DeliveryTiming>)

/**
 * Teslim kayıtlarının Hatırlatma Sağlığı ve sınama ekranlarına bakan yüzü (yalnız okur). Motorun kullandığı
 * depo [ReminderStore]'dur; ekranların soruları oraya karışmasın diye ayrıdır.
 */
class DeliveryInsights(private val db: ToparlaDatabase) {
    /**
     * Hatırlatma Sağlığı için: sıradaki hatırlatma sayısı, en son çalan teslim ve Kullanıcı'nın hatırlatmalarının
     * [since] anından beri çalmış teslimleri (bildirimi gösterilemeyenler işaretli).
     */
    suspend fun healthFacts(since: Instant): HealthFacts {
        val recent = db.occurrences().deliveredSince(since.toEpochMilli())
        val blocked = if (recent.isEmpty()) emptySet() else db.deliveryLog().keysWithEvent(recent.map { it.key }, DeliveryEvent.BLOCKED).toSet()
        return HealthFacts(
            upcomingReminders = db.reminders().countUpcoming(),
            lastDelivery = db.occurrences().lastDelivered()?.toTiming(blocked = false),
            recentDeliveries = recent.map { it.toTiming(blocked = it.key in blocked) },
        )
    }

    /** Bu teslimde alarm kapalı uygulamayı mı uyandırdı (teslim hattının `WOKE_APP` kaydı)? */
    suspend fun wokeApp(key: String): Boolean = db.deliveryLog().keysWithEvent(listOf(key), DeliveryEvent.WOKE_APP).isNotEmpty()

    private fun DeliveredRow.toTiming(blocked: Boolean) = DeliveryTiming(Instant.ofEpochMilli(plannedAt), Instant.ofEpochMilli(deliveredAt), blocked)
}
