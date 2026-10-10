package com.toparla.app

import android.app.Application
import android.content.Intent
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

    /**
     * Cihaz denemesinde ekranı dokunmadan açmak için (bu telefonda adb ile dokunma yasak):
     * `adb shell am start --activity-clear-top -a toparla.dev.EKRAN -n com.toparla.app.dev/com.toparla.app.MainActivity --es ekran saglik`
     * (eylem ve bayrak olmadan, uygulama açıkken istek teslim edilmez: görev yalnız öne gelir; 10 Ekim cihaz ölçümü).
     * Adlar `nav.Screens` içindedir: `plan`, `ekle`, `saglik`, `kurulum`, `sina`, `adim:AUTO_START`.
     */
    fun screenOf(intent: Intent): String? = intent.getStringExtra(EXTRA_SCREEN)

    private const val EXTRA_SCREEN = "ekran"
}
