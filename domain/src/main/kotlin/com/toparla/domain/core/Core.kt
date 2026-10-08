package com.toparla.domain.core

import kotlinx.coroutines.CoroutineDispatcher
import java.time.Instant
import java.time.ZoneId

/** Tek zaman kaynağı (K14). Üretimde sistem saati, testte sahte saat. */
interface Clock {
    fun now(): Instant

    fun zone(): ZoneId
}

/** Tohumlanabilir rastgelelik; aynı tohumla aynı karar (blueprint F8). */
interface RandomSource {
    fun nextDouble(): Double

    fun nextInt(untilExclusive: Int): Int
}

/** Kayıt anahtarı üretici (UUID metin; blueprint H). */
fun interface IdGenerator {
    fun newId(): String
}

/** Eşzamanlılık bağlamları (blueprint B1, B4). Testte tek bir test dağıtıcısıyla değiştirilir. */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

/** Hata modeli (blueprint B4). [AiUnavailable] kullanıcıya gösterilmez; kural tabanlı sonuç gelir. */
sealed interface AppError {
    data class Storage(val detail: String) : AppError

    data class Permission(val permission: String) : AppError

    data class Network(val detail: String) : AppError

    data class AiUnavailable(val reason: String) : AppError

    data class Validation(val detail: String) : AppError

    data class Unknown(val detail: String) : AppError
}
