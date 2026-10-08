package com.toparla.spike

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.widget.TextView

/** S0 "A grubu" denemeleri: bildirim erişimi, Tile, alarm sesi/DND, ayar bağlantıları, arama, canlı bildirim. */
object AGroup {
    private const val PKG = "com.toparla.spike"

    /** Spike 4: HyperOS ve Android ayar sayfalarına derin bağlantı adayları. */
    private fun links(context: Context): Map<String, Intent> = mapOf(
        "otomatik-baslatma" to Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        ),
        "pil-kisiti" to Intent().setComponent(
            ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"),
        ).putExtra("package_name", PKG).putExtra("package_label", "Toparla Spike"),
        "diger-izinler" to Intent("miui.intent.action.APP_PERM_EDITOR").setClassName(
            "com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity",
        ).putExtra("extra_pkgname", PKG),
        "uygulama-bilgisi" to Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$PKG")),
        "tam-ekran" to Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$PKG")),
        "pil-muafiyeti" to Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$PKG")),
        "bildirim-erisimi" to Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        "erisilebilirlik" to Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
        "dnd-erisimi" to Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
        "bildirim-ayarlari" to Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, PKG),
        "kullanim-erisimi" to Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
    )

    fun openLink(activity: Activity, name: String) {
        val intent = links(activity)[name]
        if (intent == null) {
            AlarmSpike.log(activity, "LINK_UNKNOWN", name, 0)
            return
        }
        val resolved = activity.packageManager.resolveActivity(intent, 0)?.activityInfo
        try {
            activity.startActivity(intent)
            AlarmSpike.log(activity, "LINK_OK", name, 0, "resolved=${resolved?.name} exported=${resolved?.exported}")
        } catch (e: RuntimeException) {
            AlarmSpike.log(activity, "LINK_FAIL", name, 0, "${e.javaClass.simpleName} resolved=${resolved?.name}")
        }
    }

    /** Spike 7: alarm ses akışı ve Rahatsız Etme aşımı olan kritik kanal bildirimi. */
    fun alarmTest(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel("spike_critical", context.getString(R.string.channel_critical), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
            setBypassDnd(true)
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
        val created = nm.getNotificationChannel("spike_critical")
        nm.notify(
            7001,
            Notification.Builder(context, "spike_critical")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(context.getString(R.string.critical_title))
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build(),
        )
        AlarmSpike.log(
            context, "ALARM_SOUND_POSTED", "critical", 0,
            "policyAccess=${nm.isNotificationPolicyAccessGranted} channelBypassDnd=${created.canBypassDnd()} " +
                "interruptionFilter=${nm.currentInterruptionFilter} importance=${created.importance}",
        )
    }

    /** Spike 16: Android 16 canlı güncelleme (ProgressStyle + "promoted ongoing"). */
    fun progressTest(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("spike_ongoing", context.getString(R.string.channel_ongoing), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val style = Notification.ProgressStyle()
            .setProgressSegments(listOf(Notification.ProgressStyle.Segment(100)))
            .setProgress(40)
        val notification = Notification.Builder(context, "spike_ongoing")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.ongoing_title))
            .setContentText(context.getString(R.string.ongoing_text))
            .setOngoing(true)
            .setStyle(style)
            // API 36 platform SDK'sında istek için genel bir yöntem yok; anahtar adı AndroidX'in kullandığı
            // değerdir (doğrulanmadı: sistemin bunu tanıyıp tanımadığı ölçümle görülecek).
            .addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) })
            .setShortCriticalText("15 dk")
            .build()
        nm.notify(7002, notification)
        AlarmSpike.log(
            context, "PROGRESS_POSTED", "ongoing", 0,
            "canPostPromoted=${nm.canPostPromotedNotifications()} promotable=${notification.hasPromotableCharacteristics()}",
        )
    }

    /** Spike 17: arama durumunu izin gerektirmeden okuma. */
    fun audioMode(context: Context) {
        val am = context.getSystemService(AudioManager::class.java)
        AlarmSpike.log(context, "AUDIO_MODE", "mode", 0, "mode=${am.mode} (0=normal 1=ringtone 2=in_call 3=in_communication)")
    }

    /** Spike 8: sistemden "Yakala" kutucuğunu hızlı ayarlara eklemesini iste (Kullanıcı onaylar). */
    fun requestTile(activity: Activity) {
        activity.getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(activity, CaptureTile::class.java),
            activity.getString(R.string.tile_label),
            Icon.createWithResource(activity, android.R.drawable.ic_btn_speak_now),
            activity.mainExecutor,
        ) { result -> AlarmSpike.log(activity, "TILE_ADD_RESULT", "tile", 0, "result=$result (1=zaten ekli 2=eklendi)") }
    }
}

/** Spike 6: bildirim erişimi. İçerik saklanmaz; yalnız paket, tür ve uzunluklar kayda yazılır. */
class SpikeNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        AlarmSpike.log(this, "NLS_CONNECTED", "listener", 0, "active=${activeNotifications?.size}")
    }

    override fun onListenerDisconnected() {
        AlarmSpike.log(this, "NLS_DISCONNECTED", "listener", 0)
        requestRebind(ComponentName(this, SpikeNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val otp = OTP.containsMatchIn(text)
        AlarmSpike.log(
            this, "NLS_POSTED", sbn.packageName, 0,
            "category=${sbn.notification.category} ongoing=${sbn.isOngoing} titleLen=${title.length} textLen=${text.length} otpPattern=$otp",
        )
    }

    private companion object {
        /** 4–8 haneli kod + "kod/şifre" sözcüğü: blueprint M19.2 süzgeci. */
        val OTP = Regex("(?i)(kod|şifre|sifre|parola)[^0-9]{0,40}[0-9]{4,8}|[0-9]{4,8}[^0-9]{0,40}(kod|şifre|sifre|parola)")
    }
}

/** Spike 8: "Yakala" kutucuğu. Dokunma anı kilit ekranı yakalama Activity'sine taşınır. */
class CaptureTile : TileService() {
    override fun onClick() {
        AlarmSpike.log(this, "TILE_CLICK", "tile", 0, "locked=$isLocked secure=$isSecure")
        if (FinalSpikes.consumeTileService(this)) {
            FinalSpikes.startService(this, "fgs-tile")
            return
        }
        if (isLocked) {
            // Kilitliyken startActivityAndCollapse bu telefonda PIN istiyor (ölçüldü): tanıma doğrudan servisten.
            listenFromService(SystemClock.uptimeMillis())
            return
        }
        val intent = Intent(this, LockCaptureActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(LockCaptureActivity.EXTRA_CLICK_UPTIME, SystemClock.uptimeMillis())
        startActivityAndCollapse(
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
        )
    }

    private fun tileFeedback(state: Int, subtitle: String?) {
        val tile = qsTile ?: return
        tile.state = state
        tile.subtitle = subtitle
        tile.updateTile()
    }

    private fun listenFromService(clickUptime: Long) {
        // Kutucuk diyaloğu kilit ekranında görünmedi (ölçüldü); geri bildirim kutucuğun kendisinden verilir.
        tileFeedback(Tile.STATE_ACTIVE, getString(R.string.stt_listening))
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) return
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        fun since() = "sinceClickMs=${SystemClock.uptimeMillis() - clickUptime}"
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = AlarmSpike.log(this@CaptureTile, "CAPTURE_MIC_READY", "tile-locked", 0, since())

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                AlarmSpike.log(this@CaptureTile, "CAPTURE_RESULT", "tile-locked", 0, "chars=${text.length}")
                tileFeedback(Tile.STATE_INACTIVE, getString(R.string.capture_saved))
                recognizer.destroy()
            }

            override fun onError(error: Int) {
                AlarmSpike.log(this@CaptureTile, "CAPTURE_ERROR", "tile-locked", 0, "error=$error")
                tileFeedback(Tile.STATE_INACTIVE, null)
                recognizer.destroy()
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true),
        )
    }
}

/** Kilit ekranının üstünde yalnız yeni kayıt: açılır açılmaz dinlemeye başlar, mevcut veri göstermez. */
class LockCaptureActivity : Activity(), RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private var clickUptime = 0L
    private lateinit var label: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        clickUptime = intent.getLongExtra(EXTRA_CLICK_UPTIME, SystemClock.uptimeMillis())
        label = TextView(this).apply {
            setText(R.string.stt_listening)
            textSize = 28f
            gravity = Gravity.CENTER
        }
        setContentView(label)
        AlarmSpike.log(this, "CAPTURE_CREATED", "tile", 0, "sinceClickMs=${SystemClock.uptimeMillis() - clickUptime}")
        if (SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this).also {
                it.setRecognitionListener(this)
                it.startListening(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true),
                )
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        AlarmSpike.log(this, "CAPTURE_MIC_READY", "tile", 0, "sinceClickMs=${SystemClock.uptimeMillis() - clickUptime}")
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        AlarmSpike.log(this, "CAPTURE_RESULT", "tile", 0, "chars=${text.length}")
        label.text = getString(R.string.capture_saved)
        label.postDelayed({ finish() }, 900)
    }

    override fun onError(error: Int) {
        AlarmSpike.log(this, "CAPTURE_ERROR", "tile", 0, "error=$error")
        finish()
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() = Unit

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    override fun onDestroy() {
        recognizer?.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CLICK_UPTIME = "clickUptime"
    }
}
