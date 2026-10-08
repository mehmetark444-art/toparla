package com.toparla.spike

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** F1 kapanış denemeleri: servis başlatmanın kalan yolları ve ekran okuma ön ölçümü. */
object FinalSpikes {
    /** Bir sonraki kutucuk dokunuşu yakalama yerine servis başlatmayı dener (spike 2, kutucuk yolu). */
    @Volatile
    var tileStartsService = false

    /** Spike 2: bildirim eyleminden (uygulama açılmadan) foreground service başlatma. */
    fun postActionNotification(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("spike_action", context.getString(R.string.channel_action), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val action = PendingIntent.getBroadcast(
            context, 0, Intent(context, ActionReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        nm.notify(
            7003,
            Notification.Builder(context, "spike_action")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(context.getString(R.string.action_title))
                .addAction(Notification.Action.Builder(null, context.getString(R.string.action_button), action).build())
                .build(),
        )
        AlarmSpike.log(context, "ACTION_NOTIFICATION_POSTED", "fgs-action", 0)
    }

    fun startService(context: Context, path: String) {
        try {
            context.startForegroundService(Intent(context, SpikeService::class.java).putExtra(AlarmSpike.EXTRA_KEY, path))
            AlarmSpike.log(context, "FGS_REQUESTED", path, 0)
        } catch (e: RuntimeException) {
            AlarmSpike.log(context, "FGS_START_EXCEPTION", path, 0, e.javaClass.simpleName)
        }
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = FinalSpikes.startService(context, "fgs-action")
}

/**
 * F1.24 ekran okuma ön ölçümü (karar 0008, aday). Yalnız ölçer: paket adı, düğüm sayısı, metin uzunluğu,
 * parola alanı sayısı ve toplama süresi kayda yazılır. **Ekrandaki hiçbir metin saklanmaz ya da günlüğe yazılmaz.**
 */
class ScreenReadSpikeService : AccessibilityService() {
    private var lastReadAt = 0L
    private var reads = 0

    override fun onServiceConnected() {
        AlarmSpike.log(this, "SCREEN_READ_CONNECTED", "screen", 0)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val now = SystemClock.uptimeMillis()
        if (now - lastReadAt < MIN_INTERVAL_MS) return
        lastReadAt = now
        val root = rootInActiveWindow ?: return
        val stats = Stats()
        val t0 = System.nanoTime()
        walk(root, stats, 0)
        val micros = (System.nanoTime() - t0) / 1000
        reads++
        AlarmSpike.log(
            this, "SCREEN_READ", root.packageName?.toString().orEmpty(), 0,
            "n=$reads nodes=${stats.nodes} textNodes=${stats.textNodes} chars=${stats.chars} passwordNodes=${stats.passwords} " +
                "maxDepth=${stats.maxDepth} us=$micros event=${AccessibilityEvent.eventTypeToString(event.eventType)}",
        )
    }

    private fun walk(node: AccessibilityNodeInfo, stats: Stats, depth: Int) {
        stats.nodes++
        if (depth > stats.maxDepth) stats.maxDepth = depth
        if (node.isPassword) stats.passwords++
        val length = (node.text?.length ?: 0) + (node.contentDescription?.length ?: 0)
        if (length > 0) {
            stats.textNodes++
            stats.chars += length
        }
        if (stats.nodes >= MAX_NODES) return
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            walk(child, stats, depth + 1)
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        AlarmSpike.log(this, "SCREEN_READ_UNBIND", "screen", 0, "reads=$reads")
        return super.onUnbind(intent)
    }

    private class Stats(var nodes: Int = 0, var textNodes: Int = 0, var chars: Int = 0, var passwords: Int = 0, var maxDepth: Int = 0)

    private companion object {
        const val MIN_INTERVAL_MS = 1000L
        const val MAX_NODES = 5000
    }
}
