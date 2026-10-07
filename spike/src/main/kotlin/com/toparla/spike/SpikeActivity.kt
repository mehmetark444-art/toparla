package com.toparla.spike

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Elle: düğmeler. Otomasyon:
 * am start -n com.toparla.spike/.SpikeActivity --es api both --ei delaySec 120
 */
class SpikeActivity : Activity() {
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 3, pad, pad)
        }
        fun button(textRes: Int, onClick: () -> Unit) {
            val button = Button(this)
            button.setText(textRes)
            button.setOnClickListener {
                onClick()
                refresh()
            }
            root.addView(button)
        }

        button(R.string.btn_test_2min) { scheduleBoth(120) }
        button(R.string.btn_test_60min) { scheduleBoth(3600) }
        button(R.string.btn_refresh) {}
        button(R.string.btn_clear) { AlarmSpike.logFile(this).delete() }
        logView = TextView(this).apply {
            textSize = 11f
            setTextIsSelectable(true)
        }
        root.addView(ScrollView(this).apply { addView(logView) })
        setContentView(root)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun handle(intent: Intent) {
        val delaySec = intent.getIntExtra("delaySec", 0)
        if (delaySec <= 0) return
        val api = intent.getStringExtra("api") ?: "both"
        if (api == "both") {
            scheduleBoth(delaySec)
        } else {
            AlarmSpike.schedule(this, api, System.currentTimeMillis() + delaySec * 1000L)
        }
        intent.removeExtra("delaySec")
    }

    /** İki API aynı ana kurulur; sapmalar doğrudan karşılaştırılır. */
    private fun scheduleBoth(delaySec: Int) {
        val at = System.currentTimeMillis() + delaySec * 1000L
        AlarmSpike.schedule(this, AlarmSpike.API_CLOCK, at)
        AlarmSpike.schedule(this, AlarmSpike.API_IDLE, at)
    }

    private fun refresh() {
        val file = AlarmSpike.logFile(this)
        logView.text = if (file.exists()) {
            file.readLines().takeLast(60).reversed().joinToString("\n")
        } else {
            getString(R.string.log_empty)
        }
    }
}
