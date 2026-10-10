package com.toparla.app

import android.app.Application
import android.content.Intent

/** `release` varyantında geliştirme aracı yoktur; aynı ad `debug` kaynak kümesinde doludur. */
object DevTools {
    @Suppress("UNUSED_PARAMETER")
    fun install(app: Application) = Unit

    /** Release sürümünde dışarıdan ekran istenemez. */
    @Suppress("UNUSED_PARAMETER")
    fun screenOf(intent: Intent): String? = null
}
