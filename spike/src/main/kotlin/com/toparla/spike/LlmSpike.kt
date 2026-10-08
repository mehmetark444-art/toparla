package com.toparla.spike

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Spike 10: cihaz içi model (LiteRT-LM + Gemma). Yükleme süresi, ilk parça süresi, hız ve Türkçe
 * çıktı ölçülür. Çıktılar `llm-out.txt` dosyasına, ölçümler `log.csv`'ye yazılır.
 * Çalıştırma: am start -n com.toparla.spike/.SpikeActivity --es llm gpu   (ya da cpu)
 */
object LlmSpike {
    private const val TAG = "TOPARLA_LLM"
    private const val MODEL_FILE = "model.litertlm"
    private const val TIMEOUT_SEC = 180L

    private const val SYSTEM =
        "Sen Güneş adlı sakin bir yardımcısın. Türkçe yaz. Kısa ve somut ol. İstenirse yalnızca JSON üret."

    private val PROMPTS = listOf(
        "bolme" to "Şu cümledeki işleri ayır ve yalnızca JSON üret: {\"items\":[{\"type\":\"TASK|SHOPPING|EVENT\",\"text\":\"\"}]}\n" +
            "Cümle: kedi maması bitmiş, bir de Selin'e doğum günü hediyesi alayım, yarın akşam da dişçiyi arayayım",
        "mikro-adim" to "Görev: 3 gündür ertelenen e-postayı yazmak. En fazla 12 kelimelik, fiille başlayan, " +
            "2 dakikada yapılabilecek tek bir ilk adım yaz.",
        "siniflama" to "Şu notun türünü tek kelimeyle yaz (GOREV, RANDEVU, ALISVERIS, FIKIR, ENDISE): " +
            "\"acaba bu işi bırakmalı mıyım diye düşünüp duruyorum\"",
        "bildirim" to "Kullanıcı faturayı ödemeyi iki kez erteledi. En fazla 12 kelimelik, suçlamayan bir hatırlatma yaz.",
    )

    fun modelFile(context: Context): File = File(context.getExternalFilesDir("models"), MODEL_FILE)

    fun run(context: Context, backendName: String) {
        val app = context.applicationContext
        thread(name = "llm-spike") {
            try {
                runBlocking(app, backendName)
            } catch (e: Throwable) {
                Log.e(TAG, "hata", e)
                AlarmSpike.log(app, "LLM_ERROR", backendName, 0, "${e.javaClass.simpleName}: ${e.message?.take(120)}")
            }
        }
    }

    private fun runBlocking(context: Context, backendName: String) {
        val model = modelFile(context)
        if (!model.exists()) {
            AlarmSpike.log(context, "LLM_NO_MODEL", backendName, 0, model.absolutePath)
            return
        }
        val out = File(context.getExternalFilesDir(null), "llm-out.txt")
        val backend = if (backendName == "cpu") Backend.CPU() else Backend.GPU()
        val t0 = System.currentTimeMillis()
        Engine(EngineConfig(model.absolutePath, backend)).use { engine ->
            engine.initialize()
            val initMs = System.currentTimeMillis() - t0
            AlarmSpike.log(context, "LLM_INIT", backendName, 0, "initMs=$initMs sizeMb=${model.length() / 1_000_000}")
            out.appendText("\n===== $backendName initMs=$initMs =====\n")

            for ((name, prompt) in PROMPTS) {
                engine.createConversation(ConversationConfig(Contents.of(SYSTEM))).use { conversation ->
                    val text = StringBuilder()
                    val done = CountDownLatch(1)
                    var firstMs = -1L
                    val start = System.currentTimeMillis()
                    conversation.sendMessageAsync(
                        prompt,
                        object : MessageCallback {
                            override fun onMessage(message: Message) {
                                if (firstMs < 0) firstMs = System.currentTimeMillis() - start
                                message.contents.contents.filterIsInstance<Content.Text>().forEach { text.append(it.text) }
                            }

                            override fun onDone() = done.countDown()

                            override fun onError(throwable: Throwable) {
                                text.append("[HATA ${throwable.javaClass.simpleName}: ${throwable.message}]")
                                done.countDown()
                            }
                        },
                    )
                    val finished = done.await(TIMEOUT_SEC, TimeUnit.SECONDS)
                    val totalMs = System.currentTimeMillis() - start
                    // Kütüphanenin ölçüm bilgisi Kotlin'den erişilebilir değil; hız karakter/sn olarak hesaplanır.
                    val thermal = context.getSystemService(PowerManager::class.java).currentThermalStatus
                    val genMs = (totalMs - firstMs).coerceAtLeast(1)
                    AlarmSpike.log(
                        context, "LLM_GEN", "$backendName/$name", 0,
                        "firstMs=$firstMs totalMs=$totalMs finished=$finished chars=${text.length} " +
                            "charsPerSec=${text.length * 1000L / genMs} thermal=$thermal",
                    )
                    out.appendText("--- $name (${totalMs} ms)\n$text\n")
                }
            }
        }
        AlarmSpike.log(context, "LLM_DONE", backendName, 0)
    }
}
