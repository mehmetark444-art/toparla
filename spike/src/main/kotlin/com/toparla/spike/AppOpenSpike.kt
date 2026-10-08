package com.toparla.spike

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Spike 5: izlenen uygulamanın ön plana gelişini erişilebilirlikle algılama ve müdahale ekranını
 * açma gecikmesi. Yalnız paket adı okunur (K17).
 */
class AppOpenAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply { packageNames = WATCHED }
        AlarmSpike.log(this, "A11Y_CONNECTED", "service", 0, "watched=${WATCHED.size}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val now = SystemClock.uptimeMillis()
        val dispatchMs = now - event.eventTime
        if (now - lastInterceptAt < COOLDOWN_MS) {
            // İçerik olayları çok sık gelir; bekleme süresindeyken yalnız pencere olayı kayda yazılır.
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                AlarmSpike.log(this, "APP_WINDOW_IGNORED", pkg, 0, "dispatchMs=$dispatchMs")
            }
            return
        }
        lastInterceptAt = now
        AlarmSpike.log(this, "APP_OPEN", pkg, 0, "dispatchMs=$dispatchMs type=${AccessibilityEvent.eventTypeToString(event.eventType)}")
        try {
            startActivity(
                Intent(this, InterceptActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(EXTRA_PKG, pkg)
                    .putExtra(EXTRA_EVENT_UPTIME, event.eventTime),
            )
        } catch (e: RuntimeException) {
            AlarmSpike.log(this, "INTERCEPT_START_EXCEPTION", pkg, 0, e.javaClass.simpleName)
        }
    }

    override fun onInterrupt() {
        AlarmSpike.log(this, "A11Y_INTERRUPT", "service", 0)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AlarmSpike.log(this, "A11Y_UNBIND", "service", 0)
        return super.onUnbind(intent)
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_EVENT_UPTIME = "eventUptime"

        // Kart kapanınca izlenen uygulamanın penceresi yeniden olay üretir; döngüyü önler.
        const val COOLDOWN_MS = 15_000L
        val WATCHED = arrayOf("com.google.android.youtube", "com.miui.calculator")

        @Volatile
        var lastInterceptAt = 0L
    }
}

/** Müdahale kartı denemesi: açıldığı ve ilk çizildiği an, olay anına göre kayda düşer. */
class InterceptActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(AppOpenAccessibilityService.EXTRA_PKG).orEmpty()
        val eventUptime = intent.getLongExtra(AppOpenAccessibilityService.EXTRA_EVENT_UPTIME, 0)
        AlarmSpike.log(this, "INTERCEPT_CREATED", pkg, 0, "sinceEventMs=${SystemClock.uptimeMillis() - eventUptime}")

        val pad = (24 * resources.displayMetrics.density).toInt()
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad * 3)
            setBackgroundColor(0xFF242220.toInt())
        }
        card.addView(TextView(this).apply {
            setText(R.string.intercept_question)
            textSize = 22f
            setTextColor(0xFFECE7DF.toInt())
            gravity = Gravity.CENTER
        })
        card.addView(Button(this).apply {
            setText(R.string.intercept_skip)
            setOnClickListener {
                AlarmSpike.log(this@InterceptActivity, "INTERCEPT_SKIPPED", pkg, 0)
                AppOpenAccessibilityService.lastInterceptAt = SystemClock.uptimeMillis()
                finish()
            }
        })
        val root = LinearLayout(this).apply {
            gravity = Gravity.BOTTOM
            addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        setContentView(root)
        root.post {
            AlarmSpike.log(this, "INTERCEPT_DRAWN", pkg, 0, "sinceEventMs=${SystemClock.uptimeMillis() - eventUptime}")
        }
    }
}
