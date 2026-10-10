package com.toparla.app.reminder

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.toparla.app.R
import com.toparla.domain.reminder.HealthCheck
import com.toparla.domain.reminder.ReminderClass
import com.toparla.domain.reminder.StandbyBucket
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/* Hatırlatma Sağlığı, sınama ve kurulum ekranlarının ortak metinleri: ad, neden, değer ve zaman yazımı. */

@StringRes
internal fun checkName(check: HealthCheck): Int = when (check) {
    HealthCheck.NOTIFICATIONS -> R.string.check_notifications
    HealthCheck.CHANNELS -> R.string.check_channels
    HealthCheck.EXACT_ALARMS -> R.string.check_exact_alarms
    HealthCheck.AUTO_START -> R.string.check_auto_start
    HealthCheck.BATTERY -> R.string.check_battery
    HealthCheck.STANDBY_BUCKET -> R.string.check_bucket
    HealthCheck.FULL_SCREEN -> R.string.check_full_screen
    HealthCheck.DND_ACCESS -> R.string.check_dnd
}

/** Eksik ayarın tek cümlelik nedeni. Kanal satırı hangi sınıfın kısıldığını adıyla söyler. */
@Composable
internal fun checkWhy(check: HealthCheck, weakened: Set<ReminderClass>): String = when (check) {
    HealthCheck.NOTIFICATIONS -> stringResource(R.string.check_notifications_why)
    HealthCheck.CHANNELS -> stringResource(R.string.check_channels_why, classList(weakened))
    HealthCheck.EXACT_ALARMS -> stringResource(R.string.check_exact_alarms_why)
    HealthCheck.AUTO_START -> stringResource(R.string.check_auto_start_why)
    HealthCheck.BATTERY -> stringResource(R.string.check_battery_why)
    HealthCheck.STANDBY_BUCKET -> stringResource(R.string.check_bucket_why)
    HealthCheck.FULL_SCREEN -> stringResource(R.string.check_full_screen_why)
    HealthCheck.DND_ACCESS -> stringResource(R.string.check_dnd_why)
}

/** "Kritik", "Kritik ve Önemli". Sıra önem sırasıdır. */
@Composable
internal fun classList(classes: Set<ReminderClass>): String {
    val names = classes.sorted().map { classLabel(it) }
    return when (names.size) {
        0 -> ""
        1 -> names.single()
        else -> stringResource(R.string.list_and, names.dropLast(1).joinToString(", "), names.last())
    }
}

@Composable
internal fun bucketLabel(bucket: StandbyBucket): String = stringResource(
    when (bucket) {
        StandbyBucket.EXEMPTED -> R.string.bucket_exempted
        StandbyBucket.ACTIVE -> R.string.bucket_active
        StandbyBucket.WORKING_SET -> R.string.bucket_working_set
        StandbyBucket.FREQUENT -> R.string.bucket_frequent
        StandbyBucket.RARE -> R.string.bucket_rare
        StandbyBucket.RESTRICTED -> R.string.bucket_restricted
    },
)

/** "Bugün 21:47", "Yarın 09:00" ya da "9 Eki 21:47". */
@Composable
internal fun whenLabel(at: Instant, zone: ZoneId, today: LocalDate): String {
    val local = at.atZone(zone)
    return stringResource(R.string.health_when, dayLabel(local.toLocalDate(), today), TIME.format(local))
}

/** "35 sn" ya da "1 dk 35 sn". */
@Composable
internal fun durationLabel(duration: Duration): String {
    val minutes = duration.toMinutes()
    val seconds = duration.seconds - minutes * SECONDS_PER_MINUTE
    return if (minutes > 0) stringResource(R.string.duration_min_sec, minutes, seconds) else stringResource(R.string.duration_sec, seconds)
}

private const val SECONDS_PER_MINUTE = 60
