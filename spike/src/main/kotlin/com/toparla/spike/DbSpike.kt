package com.toparla.spike

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

/** Spike 13: Room 3 + KSP + BundledSQLiteDriver derleniyor ve cihazda çalışıyor mu; FTS5 Türkçe metinde ne yapıyor. */
@Entity
data class SpikeNote(@PrimaryKey(autoGenerate = true) val id: Long = 0, val body: String, val createdAt: Long)

@Dao
interface SpikeNoteDao {
    @Insert
    suspend fun insert(note: SpikeNote): Long

    @Query("SELECT * FROM SpikeNote ORDER BY id DESC LIMIT 1")
    suspend fun last(): SpikeNote?

    @Query("SELECT count(*) FROM SpikeNote")
    suspend fun count(): Int
}

@Database(entities = [SpikeNote::class], version = 1, exportSchema = false)
abstract class SpikeDb : RoomDatabase() {
    abstract fun notes(): SpikeNoteDao
}

object DbSpike {
    private val DOCS = listOf(
        "Dişçi randevusu yarın saat dokuzda",
        "Işık faturasını ödemeyi unutma",
        "İstanbul'a gidiş bileti alındı",
        "Çöpü akşam çıkar",
        "Öğle yemeğinden sonra ilacı iç",
    )

    /** Sorgu → beklenen eşleşme sayısı. Büyük/küçük İ-ı ve aksan davranışını gösterir. */
    private val QUERIES = listOf("dişçi", "disci", "DİŞÇİ", "ışık", "isik", "istanbul", "İSTANBUL", "çöp*", "ilac*", "fatura*")

    fun run(context: Context) {
        thread(name = "db-spike") {
            try {
                room(context)
                fts5(context)
            } catch (e: Exception) {
                AlarmSpike.log(context, "DB_FAIL", "db", 0, "${e.javaClass.simpleName}: ${e.message?.take(160)}")
            }
        }
    }

    private fun room(context: Context) = runBlocking {
        val t0 = System.nanoTime()
        val db = Room.databaseBuilder<SpikeDb>(context, context.getDatabasePath("spike-room.db").path)
            .setDriver(BundledSQLiteDriver())
            .build()
        val id = db.notes().insert(SpikeNote(body = DOCS[0], createdAt = System.currentTimeMillis()))
        val last = db.notes().last()
        val count = db.notes().count()
        db.close()
        AlarmSpike.log(
            context, "DB_ROOM_OK", "db", 0,
            "id=$id roundTrip=${last?.body == DOCS[0]} count=$count ms=${(System.nanoTime() - t0) / 1_000_000}",
        )
    }

    private fun fts5(context: Context) {
        val file = context.getDatabasePath("spike-fts.db")
        file.parentFile?.mkdirs()
        file.delete()
        val conn = BundledSQLiteDriver().open(file.path)
        val version = conn.prepare("SELECT sqlite_version()").use { it.step(); it.getText(0) }
        for (tokenizer in listOf("unicode61", "unicode61 remove_diacritics 2", "trigram")) {
            conn.execSQL("DROP TABLE IF EXISTS doc")
            conn.execSQL("CREATE VIRTUAL TABLE doc USING fts5(body, tokenize='$tokenizer')")
            for (body in DOCS) {
                conn.prepare("INSERT INTO doc(body) VALUES (?)").use {
                    it.bindText(1, body)
                    it.step()
                }
            }
            val hits = QUERIES.joinToString(" ") { query ->
                val n = try {
                    conn.prepare("SELECT count(*) FROM doc WHERE doc MATCH ?").use {
                        it.bindText(1, query)
                        it.step()
                        it.getLong(0).toString()
                    }
                } catch (e: Exception) {
                    "hata"
                }
                "$query=$n"
            }
            AlarmSpike.log(context, "DB_FTS5", tokenizer.replace(' ', '_'), 0, "sqlite=$version $hits")
        }
        conn.close()
    }
}
