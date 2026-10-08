package com.toparla.spike

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.EmbeddingEngine
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.EmbeddingOptions
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Spike 10b: gerçek RAG. `assets/rag-set.json` içindeki 24 bilgi kartı ve 24 soru (4'ünün yanıtı
 * kartlarda yok) için: (1) gömme modeliyle kart bulma, dört yapılandırmada; (2) en iyi yapılandırmanın
 * getirdiği ilk 3 kartla Gemma 4 E4B'nin yanıtı. Çıktı `rag-out.jsonl`; puanlama `scripts/rag-puanla.mjs`.
 * Çalıştırma: am start -n com.toparla.spike/.SpikeActivity --es rag 1
 */
object RagSpike {
    private const val TAG = "TOPARLA_RAG"
    private const val EMBED_MODEL = "embeddinggemma-2-text-270m.litertlm"
    private const val LLM_MODEL = "gemma-4-E4B-it.litertlm"
    private const val TOP_K = 3
    private const val TIMEOUT_SEC = 240L

    private const val SYSTEM =
        "Sen Güneş adlı sakin bir yardımcısın. Türkçe yaz ve kullanıcıya \"sen\" diye hitap et. Kısa ve somut ol. " +
            "İlaç, doz ve tedavi konusunda öneri verme; doktora ya da eczacıya yönlendir."

    /** Yanıt talimatı çeşitleri: "kati" yalnız karttaki ifadeye, "esnek" eş anlamlı eşleştirmeye ve doğrudan çıkarıma izin verir. */
    private val PROMPT_VARIANTS = listOf(
        "kati" to "Yalnızca bu kartlara dayanarak yanıtla. Kartlarda yoksa \"Kartlarda bu bilgi yok\" de.",
        "esnek" to "Soru kartlardaki sözcükleri birebir kullanmayabilir: eş anlamlıları ve gündelik ifadeleri kartlardaki " +
            "terimlerle eşleştir, karttaki bilgiden doğrudan çıkan sonucu söyle. Kartlardaki bilgiyle yanıtlanamıyorsa " +
            "tahmin etme, \"Kartlarda bu bilgi yok\" de.",
    )

    private class Config(val name: String, val prefix: Boolean, val outputSize: Int?)

    private val configs = listOf(
        Config("duz-tam", prefix = false, outputSize = null),
        Config("duz-256", prefix = false, outputSize = 256),
        Config("onek-tam", prefix = true, outputSize = null),
        Config("onek-256", prefix = true, outputSize = 256),
    )

    fun run(context: Context) {
        val app = context.applicationContext
        thread(name = "rag-spike") {
            try {
                runAll(app)
            } catch (e: Throwable) {
                Log.e(TAG, "hata", e)
                AlarmSpike.log(app, "RAG_ERROR", "rag", 0, "${e.javaClass.simpleName}: ${e.message?.take(160)}")
            }
        }
    }

    private fun runAll(context: Context) {
        val models = context.getExternalFilesDir("models")
        val set = JSONObject(context.assets.open("rag-set.json").bufferedReader().use { it.readText() })
        val cards = set.getJSONArray("cards")
        val questions = set.getJSONArray("questions")
        val out = File(context.getExternalFilesDir(null), "rag-out.jsonl")
        out.delete()

        // 1) Kart bulma
        var best: Config = configs.first()
        var bestHits = -1
        val topByConfig = HashMap<String, List<List<Int>>>()
        val t0 = System.currentTimeMillis()
        EmbeddingEngine(EmbeddingEngineConfig(File(models, EMBED_MODEL).absolutePath, Backend.CPU())).use { engine ->
            engine.initialize()
            AlarmSpike.log(context, "RAG_EMBED_INIT", "rag", 0, "initMs=${System.currentTimeMillis() - t0}")
            for (config in configs) {
                val options = EmbeddingOptions(true, null, config.outputSize)
                val start = System.currentTimeMillis()
                val cardVectors = (0 until cards.length()).map { i ->
                    val text = cards.getJSONObject(i).getString("text")
                    embed(engine, if (config.prefix) "title: none | text: $text" else text, options)
                }
                val cardMs = System.currentTimeMillis() - start
                val startQ = System.currentTimeMillis()
                var hits = 0
                val tops = ArrayList<List<Int>>()
                for (q in 0 until questions.length()) {
                    val question = questions.getJSONObject(q)
                    val text = question.getString("text")
                    val vector = embed(engine, if (config.prefix) "task: search result | query: $text" else text, options)
                    val ranked = cardVectors.indices.sortedByDescending { dot(vector, cardVectors[it]) }
                    val top = ranked.take(TOP_K)
                    tops.add(top)
                    val expected = if (question.isNull("card")) null else question.getString("card")
                    if (expected != null && cards.getJSONObject(top[0]).getString("id") == expected) hits++
                    out.appendText(
                        JSONObject()
                            .put("phase", "retrieve")
                            .put("config", config.name)
                            .put("q", question.getString("id"))
                            .put("top", JSONArray(top.map { cards.getJSONObject(it).getString("id") }))
                            .put("rank", JSONArray(ranked.map { cards.getJSONObject(it).getString("id") }))
                            .put("scores", JSONArray(top.map { "%.3f".format(dot(vector, cardVectors[it])) }))
                            .toString() + "\n",
                    )
                }
                topByConfig[config.name] = tops
                AlarmSpike.log(
                    context, "RAG_RETRIEVE", config.name, 0,
                    "dim=${cardVectors[0].size} hit1=$hits cardMsEach=${cardMs / cards.length()} " +
                        "queryMsEach=${(System.currentTimeMillis() - startQ) / questions.length()}",
                )
                // Eşitlikte küçük vektör tercih edilir (listede sonra gelen 256'lıklar kazanır).
                if (hits > bestHits || (hits == bestHits && config.outputSize != null && best.outputSize == null && config.prefix == best.prefix)) {
                    bestHits = hits
                    best = config
                }
            }
        }

        // 1b) Zenginleştirilmiş kartlar: her karta "diğer ifadeler" satırı eklenir (öğretmen modelin kart
        // yazarken ekleyeceği eş anlamlılar). Aynı yapılandırmayla (önekli, 256 boyut) yeniden aranır.
        val alias = JSONObject(context.assets.open("rag-alias.json").bufferedReader().use { it.readText() })
        val richText = (0 until cards.length()).map { i ->
            val card = cards.getJSONObject(i)
            "${card.getString("text")} Diğer ifadeler: ${alias.getString(card.getString("id"))}."
        }
        val richTops = ArrayList<List<Int>>()
        EmbeddingEngine(EmbeddingEngineConfig(File(models, EMBED_MODEL).absolutePath, Backend.CPU())).use { engine ->
            engine.initialize()
            val options = EmbeddingOptions(true, null, 256)
            val vectors = richText.map { embed(engine, "title: none | text: $it", options) }
            for (q in 0 until questions.length()) {
                val question = questions.getJSONObject(q)
                val vector = embed(engine, "task: search result | query: ${question.getString("text")}", options)
                val ranked = vectors.indices.sortedByDescending { dot(vector, vectors[it]) }
                richTops.add(ranked.take(TOP_K))
                out.appendText(
                    JSONObject()
                        .put("phase", "retrieve")
                        .put("config", "onek-256-zengin")
                        .put("q", question.getString("id"))
                        .put("top", JSONArray(ranked.take(TOP_K).map { cards.getJSONObject(it).getString("id") }))
                        .put("rank", JSONArray(ranked.map { cards.getJSONObject(it).getString("id") }))
                        .toString() + "\n",
                )
            }
        }

        // 2) Bulunan kartlarla yanıt
        val tops = topByConfig.getValue(best.name)
        val t1 = System.currentTimeMillis()
        Engine(EngineConfig(File(models, LLM_MODEL).absolutePath, Backend.GPU())).use { engine ->
            engine.initialize()
            AlarmSpike.log(context, "RAG_LLM_INIT", best.name, 0, "initMs=${System.currentTimeMillis() - t1}")
            for (variant in PROMPT_VARIANTS) for (q in 0 until questions.length()) {
                val question = questions.getJSONObject(q)
                val context3 = tops[q].mapIndexed { i, card -> "[${i + 1}] ${cards.getJSONObject(card).getString("text")}" }.joinToString("\n")
                val prompt = "BİLGİ KARTLARI:\n$context3\n\n${variant.second}\nSoru: ${question.getString("text")}"
                val start = System.currentTimeMillis()
                val answer = generate(engine, prompt)
                out.appendText(
                    JSONObject()
                        .put("phase", "answer")
                        .put("variant", variant.first)
                        .put("config", best.name)
                        .put("q", question.getString("id"))
                        .put("totalMs", System.currentTimeMillis() - start)
                        .put("text", answer)
                        .toString() + "\n",
                )
            }
            // Zenginleştirilmiş kartlarla, katı talimatla yanıt.
            for (q in 0 until questions.length()) {
                val question = questions.getJSONObject(q)
                val context3 = richTops[q].mapIndexed { i, card -> "[${i + 1}] ${richText[card]}" }.joinToString("\n")
                val prompt = "BİLGİ KARTLARI:\n$context3\n\n${PROMPT_VARIANTS[0].second}\nSoru: ${question.getString("text")}"
                val start = System.currentTimeMillis()
                val answer = generate(engine, prompt)
                out.appendText(
                    JSONObject()
                        .put("phase", "answer")
                        .put("variant", "kati-zengin")
                        .put("config", "onek-256-zengin")
                        .put("q", question.getString("id"))
                        .put("totalMs", System.currentTimeMillis() - start)
                        .put("text", answer)
                        .toString() + "\n",
                )
            }
        }
        AlarmSpike.log(context, "RAG_DONE", best.name, 0, "wallMs=${System.currentTimeMillis() - t0}")
    }

    private fun embed(engine: EmbeddingEngine, text: String, options: EmbeddingOptions): FloatArray =
        engine.computeEmbedding(listOf(InputData.Text(text)), options).embedding

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }

    private fun generate(engine: Engine, prompt: String): String {
        engine.createConversation(ConversationConfig(Contents.of(SYSTEM))).use { conversation ->
            val text = StringBuilder()
            val done = CountDownLatch(1)
            conversation.sendMessageAsync(
                prompt,
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        message.contents.contents.filterIsInstance<Content.Text>().forEach { text.append(it.text) }
                    }

                    override fun onDone() = done.countDown()

                    override fun onError(throwable: Throwable) {
                        text.append("[HATA ${throwable.javaClass.simpleName}: ${throwable.message}]")
                        done.countDown()
                    }
                },
            )
            done.await(TIMEOUT_SEC, TimeUnit.SECONDS)
            return text.toString()
        }
    }
}
