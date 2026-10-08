package com.toparla.spike

import android.app.Activity
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Spike 3: kilit ekranının üstünde açılması beklenen kart. Açıldığı an kayda düşer. */
class FullScreenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val key = intent.getStringExtra(AlarmSpike.EXTRA_KEY).orEmpty()
        val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        AlarmSpike.log(this, "FSI_SHOWN", key, AlarmSpike.plannedAt(key), "keyguardLocked=$locked")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        root.addView(TextView(this).apply {
            setText(R.string.fsi_title)
            textSize = 28f
            gravity = Gravity.CENTER
        })
        root.addView(Button(this).apply {
            setText(R.string.fsi_ok)
            setOnClickListener {
                AlarmSpike.log(this@FullScreenActivity, "FSI_TAPPED", key, AlarmSpike.plannedAt(key))
                getSystemService(NotificationManager::class.java).cancel(key.hashCode())
                finish()
            }
        })
        setContentView(root)
    }
}

/** Spike 2: `specialUse` foreground service; başlatılabildiği an kayda düşer ve kendini durdurur. */
class SpikeService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val key = intent?.getStringExtra(AlarmSpike.EXTRA_KEY).orEmpty()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_fgs), NotificationManager.IMPORTANCE_LOW),
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.fgs_title))
            .build()
        try {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            AlarmSpike.log(this, "FGS_STARTED", key, AlarmSpike.plannedAt(key))
        } catch (e: RuntimeException) {
            AlarmSpike.log(this, "FGS_DENIED", key, AlarmSpike.plannedAt(key), e.javaClass.simpleName)
        }
        // Spike 5: "hold" kipinde servis açık kalır; HyperOS'in arka plan sürecini boşa alıp almadığı ölçülür.
        if (intent?.getBooleanExtra(EXTRA_HOLD, false) == true) return START_STICKY
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        const val EXTRA_HOLD = "hold"
        private const val CHANNEL = "spike_fgs"
        private const val NOTIFICATION_ID = 42
    }
}
