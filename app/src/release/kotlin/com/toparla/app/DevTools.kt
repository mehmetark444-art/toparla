package com.toparla.app

import android.app.Application

/** `release` varyantında geliştirme aracı yoktur; aynı ad `debug` kaynak kümesinde doludur. */
object DevTools {
    @Suppress("UNUSED_PARAMETER")
    fun install(app: Application) = Unit
}
