package com.toparla.data.core

import com.toparla.domain.core.Clock
import com.toparla.domain.core.DispatcherProvider
import com.toparla.domain.core.IdGenerator
import com.toparla.domain.core.RandomSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * `:domain` çekirdek arayüzlerinin üretim karşılıkları. Sistem saatine, rastgeleliğe ve kimlik üretimine
 * projede **yalnız bu dosyadan** dokunulur; başka her yer arayüzü enjekte alır (K14).
 */
class SystemClock : Clock {
    override fun now(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}

class UuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}

class SecureRandomSource : RandomSource {
    private val random = SecureRandom()

    override fun nextDouble(): Double = random.nextDouble()

    override fun nextInt(untilExclusive: Int): Int = random.nextInt(untilExclusive)
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val default: CoroutineDispatcher get() = Dispatchers.Default
}
