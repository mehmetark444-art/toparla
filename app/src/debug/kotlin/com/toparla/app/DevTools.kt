package com.toparla.app

import android.app.Application
import android.os.StrictMode
import timber.log.Timber

/**
 * Geliştirme araçları (blueprint B7): yalnız `debug` varyantında. StrictMode ana iş parçacığındaki disk/ağ
 * erişimini ve sızıntıları günlüğe yazar; LeakCanary bağımlılık olarak kendiliğinden kurulur.
 */
object DevTools {
    fun install(app: Application) {
        Timber.plant(Timber.DebugTree())
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        Timber.i("Geliştirme araçları açık: %s", app.packageName)
    }
}
