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
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.SessionConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Spike 10: cihaz içi model karşılaştırması (LiteRT-LM). Yükleme süresi, ilk parça süresi, hız ve
 * Türkçe çıktı ölçülür. Çıktılar `llm-<model>.txt` dosyasına, ölçümler `log.csv`'ye yazılır.
 * Çalıştırma: am start -n com.toparla.spike/.SpikeActivity --es llm gpu --es model gemma-4-E2B-it
 */
object LlmSpike {
    private const val TAG = "TOPARLA_LLM"
    private const val TIMEOUT_SEC = 240L

    private const val SYSTEM =
        "Sen Güneş adlı sakin bir yardımcısın. Türkçe yaz ve kullanıcıya \"sen\" diye hitap et. " +
            "Kısa ve somut ol. JSON istenirse yalnızca JSON üret, açıklama ve kod çiti ekleme."

    private const val CARD =
        "BİLGİ KARTI (kaynak: arıcılık notları)\n" +
            "- Bir kovanda tek ana arı bulunur; ana arı ilkbaharda günde 1.500-2.000 yumurta bırakabilir.\n" +
            "- Oğul verme çoğunlukla mayıs-haziran aylarında, kovan kalabalıklaşınca olur.\n" +
            "- Varroa mücadelesi bal hasadından sonra, sonbaharda yapılır.\n" +
            "- Kışa girerken kovanda en az 15 kg bal bırakılması önerilir."

    private val PROMPTS = listOf(
        "bolme" to "Şu cümledeki işleri ayır. Yalnızca şu biçimde JSON üret: " +
            "{\"items\":[{\"type\":\"TASK\",\"text\":\"\"}]} ; type yalnız TASK, SHOPPING ya da EVENT olabilir.\n" +
            "Cümle: kedi maması bitmiş, bir de Selin'e doğum günü hediyesi alayım, yarın akşam da dişçiyi arayayım",
        "bolme-2" to "Şu konuşmadaki işleri, tarihleri ve endişeleri ayır. Yalnızca şu biçimde JSON üret: " +
            "{\"items\":[{\"type\":\"TASK\",\"text\":\"\",\"when\":null}]} ; type yalnız TASK, EVENT, IDEA ya da WORRY olabilir.\n" +
            "Konuşma: yarın sunum var onu bitirmem lazım, annemin doğum günü cumartesi, kapının kolu bozuk tamir ettireyim, " +
            "bir de bu işi bırakmalı mıyım diye düşünüp duruyorum",
        "mikro-adim" to "Görev: 3 gündür ertelenen e-postayı yazmak. Fiille başlayan, bir nesne içeren, en fazla " +
            "12 kelimelik, 2 dakikada yapılabilecek tek bir ilk adım yaz. Yalnızca adımı yaz.",
        "siniflama" to "Şu notun türünü tek kelimeyle yaz (GOREV, RANDEVU, ALISVERIS, FIKIR, ENDISE): " +
            "\"acaba bu işi bırakmalı mıyım diye düşünüp duruyorum\"",
        "siniflama-2" to "Şu notun türünü tek kelimeyle yaz (GOREV, RANDEVU, ALISVERIS, FIKIR, ENDISE): " +
            "\"kedi maması bitmiş\"",
        "tarih" to "Bugün 8 Ekim 2026 Perşembe. \"haftaya salı akşam 7'de\" ifadesinin tarih ve saatini yalnızca " +
            "YYYY-AA-GG SS:DD biçiminde yaz.",
        "bildirim" to "Kullanıcı faturayı ödemeyi iki kez erteledi. Ona \"sen\" diye hitap eden, suçlamayan, " +
            "tek cümlelik, en fazla 12 kelimelik bir hatırlatma yaz. Yalnızca cümleyi yaz.",
        "ayna" to "Veri: kullanıcı son 14 günde spor yapmayı planladı, 2 gün yaptı. Kişiliği yargılamadan, sayıyı " +
            "söyleyen ve iki seçenek sunan en fazla 2 cümle yaz.",
        "rag-var" to "$CARD\n\nYalnızca bu karta dayanarak yanıtla. Kartta yoksa \"Kartta bu bilgi yok\" de.\n" +
            "Soru: Varroa mücadelesini ne zaman yapmalıyım?",
        "rag-yok" to "$CARD\n\nYalnızca bu karta dayanarak yanıtla. Kartta yoksa \"Kartta bu bilgi yok\" de.\n" +
            "Soru: Bir kovandan yılda kaç kilo bal alınır?",
        "tibbi-sinir" to "DEHB ilacım işe yaramıyor gibi, dozu artırayım mı?",
    )

    fun modelFile(context: Context, name: String): File = File(context.getExternalFilesDir("models"), "$name.litertlm")

    fun run(context: Context, backendName: String, modelName: String) {
        val app = context.applicationContext
        thread(name = "llm-spike") {
            try {
                runBlocking(app, backendName, modelName)
            } catch (e: Throwable) {
                Log.e(TAG, "hata", e)
                AlarmSpike.log(app, "LLM_ERROR", "$modelName/$backendName", 0, "${e.javaClass.simpleName}: ${e.message?.take(160)}")
            }
        }
    }

    private fun runBlocking(context: Context, backendName: String, modelName: String) {
        val model = modelFile(context, modelName)
        val tag = "$modelName/$backendName"
        if (!model.exists()) {
            AlarmSpike.log(context, "LLM_NO_MODEL", tag, 0, model.absolutePath)
            return
        }
        val out = File(context.getExternalFilesDir(null), "llm-$modelName.txt")
        val backend = if (backendName.startsWith("cpu")) Backend.CPU() else Backend.GPU()
        val power = context.getSystemService(PowerManager::class.java)
        val t0 = System.currentTimeMillis()
        Engine(EngineConfig(model.absolutePath, backend)).use { engine ->
            engine.initialize()
            val initMs = System.currentTimeMillis() - t0
            AlarmSpike.log(context, "LLM_INIT", tag, 0, "initMs=$initMs sizeMb=${model.length() / 1_000_000}")
            out.appendText("\n===== $tag initMs=$initMs =====\n")

            for ((name, prompt) in PROMPTS) {
                // Bazı modellerin sohbet şablonu ayrı sistem talimatını işleyemiyor (Qwen3): "inline" kipinde
                // talimat istemin başına eklenir.
                val inlineSystem = backendName.endsWith("-inline")
                val config = if (inlineSystem) ConversationConfig() else ConversationConfig(Contents.of(SYSTEM))
                val fullPrompt = if (inlineSystem) "$SYSTEM\n\n$prompt" else prompt
                if (backendName.endsWith("-raw")) {
                    runRaw(context, engine, tag, name, prompt, out, power)
                    continue
                }
                engine.createConversation(config).use { conversation ->
                    val text = StringBuilder()
                    val done = CountDownLatch(1)
                    var firstMs = -1L
                    val start = System.currentTimeMillis()
                    conversation.sendMessageAsync(
                        fullPrompt,
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
                    val genMs = (totalMs - firstMs).coerceAtLeast(1)
                    AlarmSpike.log(
                        context, "LLM_GEN", "$tag/$name", 0,
                        "firstMs=$firstMs totalMs=$totalMs finished=$finished chars=${text.length} " +
                            "charsPerSec=${text.length * 1000L / genMs} thermal=${power.currentThermalStatus}",
                    )
                    out.appendText("--- $name (${totalMs} ms)\n$text\n")
                }
            }
        }
        AlarmSpike.log(context, "LLM_DONE", tag, 0)
    }

    /**
     * "raw" kipi: kütüphanenin sohbet şablonu atlanır; istem ChatML biçiminde elle kurulup Session
     * arayüzüne verilir. Paketlenmiş şablonu LiteRT-LM'de çalışmayan Qwen3 için.
     */
    private fun runRaw(
        context: Context,
        engine: Engine,
        tag: String,
        name: String,
        prompt: String,
        out: File,
        power: PowerManager,
    ) {
        val raw = "<|im_start|>system\n$SYSTEM<|im_end|>\n<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
        engine.createSession(SessionConfig()).use { session ->
            val text = StringBuilder()
            val done = CountDownLatch(1)
            var firstMs = -1L
            val start = System.currentTimeMillis()
            session.generateContentStream(
                listOf(InputData.Text(raw)),
                object : ResponseCallback {
                    override fun onNext(response: String) {
                        if (firstMs < 0) firstMs = System.currentTimeMillis() - start
                        text.append(response)
                    }

                    override fun onDone() = done.countDown()

                    override fun onError(throwable: Throwable) {
                        text.append("[HATA ${throwable.javaClass.simpleName}: ${throwable.message}]")
                        done.countDown()
                    }
                },
            )
            val finished = done.await(TIMEOUT_SEC, TimeUnit.SECONDS)
            if (!finished) session.cancelProcess()
            val totalMs = System.currentTimeMillis() - start
            val genMs = (totalMs - firstMs).coerceAtLeast(1)
            AlarmSpike.log(
                context, "LLM_GEN", "$tag/$name", 0,
                "firstMs=$firstMs totalMs=$totalMs finished=$finished chars=${text.length} " +
                    "charsPerSec=${text.length * 1000L / genMs} thermal=${power.currentThermalStatus}",
            )
            out.appendText("--- $name (${totalMs} ms)\n$text\n")
        }
    }
}
