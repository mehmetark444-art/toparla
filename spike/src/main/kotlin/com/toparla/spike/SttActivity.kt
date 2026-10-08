package com.toparla.spike

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Spike 9: Türkçe konuşma tanıma ölçümü. 30 cümle sırayla gösterilir; Kullanıcı "Dinle"ye basıp okur.
 * Beklenen ve duyulan metin süreleriyle `stt-<kip>.jsonl` dosyasına yazılır; WER bilgisayarda hesaplanır.
 * Kip: cihaz içi tanıyıcı (varsayılan) ya da `--es mode online` ile sistemin varsayılan tanıyıcısı.
 */
class SttActivity : Activity(), RecognitionListener {
    private lateinit var sentences: List<String>
    private lateinit var out: File
    private lateinit var prompt: TextView
    private lateinit var heard: TextView
    private lateinit var listen: Button
    private var recognizer: SpeechRecognizer? = null
    private var index = 0
    private var startedAt = 0L
    private var readyMs = -1L
    private var speechEndMs = -1L
    private var mode = "ondevice"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra("mode") ?: "ondevice"
        val array = JSONArray(assets.open("stt-set.json").bufferedReader().use { it.readText() })
        sentences = (0 until array.length()).map { array.getString(it) }
        out = File(getExternalFilesDir(null), "stt-$mode.jsonl")
        out.delete()

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad * 2, pad, pad)
        }
        prompt = TextView(this).apply {
            textSize = 26f
            gravity = Gravity.CENTER
        }
        heard = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad)
        }
        listen = Button(this).apply {
            setText(R.string.stt_listen)
            textSize = 22f
            setOnClickListener { startListening() }
        }
        val skip = Button(this).apply {
            setText(R.string.stt_skip)
            setOnClickListener {
                record("", "SKIPPED")
                next()
            }
        }
        root.addView(prompt)
        root.addView(heard)
        root.addView(listen, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, pad * 4))
        root.addView(skip)
        setContentView(root)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        val onDeviceAvailable = SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        AlarmSpike.log(
            this, "STT_START", mode, 0,
            "onDeviceAvailable=$onDeviceAvailable recognitionAvailable=${SpeechRecognizer.isRecognitionAvailable(this)}",
        )
        recognizer = if (mode == "online") {
            SpeechRecognizer.createSpeechRecognizer(this)
        } else if (onDeviceAvailable) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            null
        }
        recognizer?.setRecognitionListener(this)
        if (mode != "online") checkSupport()
        show()
    }

    /** Cihaz içi tanıyıcıda Türkçe paketin kurulu olup olmadığını kayda yazar. */
    private fun checkSupport() {
        val r = recognizer ?: return
        try {
            r.checkRecognitionSupport(
                recognizerIntent(),
                mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(support: RecognitionSupport) {
                        AlarmSpike.log(
                            this@SttActivity, "STT_SUPPORT", mode, 0,
                            "installed=${support.installedOnDeviceLanguages.joinToString("|")} " +
                                "pending=${support.pendingOnDeviceLanguages.joinToString("|")} " +
                                "supportedHasTr=${support.supportedOnDeviceLanguages.any { it.startsWith("tr") }}",
                        )
                    }

                    override fun onError(error: Int) {
                        AlarmSpike.log(this@SttActivity, "STT_SUPPORT_ERROR", mode, 0, "error=$error")
                    }
                },
            )
        } catch (e: RuntimeException) {
            AlarmSpike.log(this, "STT_SUPPORT_EXCEPTION", mode, 0, e.javaClass.simpleName)
        }
    }

    private fun recognizerIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, mode != "online")

    private fun show() {
        if (index >= sentences.size) {
            prompt.setText(R.string.stt_done)
            heard.text = ""
            listen.isEnabled = false
            AlarmSpike.log(this, "STT_DONE", mode, 0, "count=${sentences.size}")
            return
        }
        prompt.text = getString(R.string.stt_progress, index + 1, sentences.size, sentences[index])
        listen.isEnabled = recognizer != null
        if (recognizer == null) heard.setText(R.string.stt_unavailable)
    }

    private fun startListening() {
        val r = recognizer ?: return
        listen.isEnabled = false
        heard.setText(R.string.stt_listening)
        startedAt = System.currentTimeMillis()
        readyMs = -1
        speechEndMs = -1
        r.startListening(recognizerIntent())
    }

    private fun record(text: String, error: String) {
        val now = System.currentTimeMillis()
        out.appendText(
            JSONObject()
                .put("i", index)
                .put("expected", sentences[index])
                .put("heard", text)
                .put("error", error)
                .put("readyMs", readyMs)
                .put("afterSpeechMs", if (speechEndMs > 0) now - speechEndMs else -1)
                .put("totalMs", now - startedAt)
                .toString() + "\n",
        )
    }

    private fun next() {
        index++
        show()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        readyMs = System.currentTimeMillis() - startedAt
    }

    override fun onEndOfSpeech() {
        speechEndMs = System.currentTimeMillis()
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        record(text, "")
        heard.text = text
        next()
    }

    override fun onError(error: Int) {
        // Hata: aynı cümle yeniden denenebilsin diye ilerlenmez; hata kayda yazılır.
        AlarmSpike.log(this, "STT_ERROR", mode, 0, "i=$index error=$error")
        heard.text = getString(R.string.stt_error, error)
        listen.isEnabled = true
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    override fun onDestroy() {
        recognizer?.destroy()
        super.onDestroy()
    }
}
