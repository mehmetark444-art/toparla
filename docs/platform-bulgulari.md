# Platform bulguları

Cihaz: Xiaomi 17T Pro · Android 16 · HyperOS 3. Her `[DOĞRULA]` / `[Spike]` maddesi
burada kapanır: **ne denendi · nasıl · sonuç · karar**.

## Geliştirme ortamı (7 Ekim 2026)

| Öğe | Bulgu |
|---|---|
| İşletim sistemi | Windows 11 Home |
| Android Studio | 2025.2.2, `C:\Program Files\Android\Android Studio` |
| JDK | Android Studio JBR 21.0.8 (ayrı JDK yok; `JAVA_HOME` tanımsız) |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk`; platformlar 33, 34, **36**; build-tools 35.0.0, 36.1.0; NDK 28.2 |
| adb | 36.0.2, PATH'te değil (`…\Sdk\platform-tools\adb.exe`) |
| Gradle | Sarmalayıcı 9.6.0 (AGP 9.4.1 gereği); JBR 21 ile koşar |
| Derleme | `JAVA_HOME` = Android Studio `jbr` verilerek `./gradlew :domain:test :app:assembleDebug` başarılı (ilk derleme 2 dk 36 sn) |
| Not | Kurulu Android Studio 2025.2.2, AGP 9.4.1 projesini açmak için eski olabilir `[DOĞRULA]`; komut satırı derlemesi etkilenmez |
| Git | 2.52 |
| Telefon | `adb devices` listesi boş: henüz bağlı değil |

## S0 spike listesi

Sıra: önce ürünü taşıyan ve en kırılgan varsayımlar.

| # | Spike | Blueprint | Durum |
|---|---|---|---|
| 1 | **Alarm teslimi:** `setAlarmClock` ↔ `setExactAndAllowWhileIdle`; 2 dk / 1 sa / gece / Doze / uygulama kapalı / yeniden başlatma / kilitli yeniden başlatma / bekleme kovası rare | G1, J2 | Devam ediyor: ilk tur yapıldı, bulgular aşağıda |
| 2 | Kesin alarmdan FGS başlatma muafiyeti; `specialUse` FGS; her başlatma yolu | G1, G2 | Bekliyor |
| 3 | Tam ekran bildirim + HyperOS "Kilit ekranında göster", "Arka planda açılır pencere" | G1 | Bekliyor |
| 4 | HyperOS ayar derin bağlantıları (otomatik başlatma, pil, kısıtlı ayarlar) → `hyperos-baglantilar.md` | G3 | Bekliyor |
| 5 | Erişilebilirlik: uygulama açılışı algılama gecikmesi (hedef ≤ 400 ms), servis ömrü, servisten Activity başlatma; olmazsa overlay yedeği | G3, G4, G10, D11 | Bekliyor |
| 6 | Bildirim erişimi: yan yüklemede kısıtlı ayarlar, `allow_listener`, kopma sonrası `requestRebind` | G4 | Bekliyor |
| 7 | Bildirim "cooldown" davranışı ve alarm ses akışı; DND aşımı | G1 | Bekliyor |
| 8 | Tile: kilitliyken `LockCaptureActivity`, Tile → mikrofon ≤ 1 sn | G9 | Bekliyor |
| 9 | Türkçe cihaz içi STT: 30 cümlelik set (sayı, tarih, özel isim) WER; yetersizse yerel Whisper | G7 | Bekliyor |
| 10 | Cihaz içi model: LiteRT-LM + Gemma sürümü `[DOĞRULA: güncel adlar]`, 16 KB sayfa uyumu, GPU/NPU, token/sn, ilk token, 10 dk sıcaklık; 50 örnek Türkçe kalite | G8, F2 | Bekliyor |
| 11 | **Gemini API:** anahtarın uç noktası (Developer API ↔ Vertex AI), akış, işlev çağrısı, şemalı çıktı, bağlam önbellekleme, görsel girdi, Google Arama temellendirmesi (atıf alanları, maliyet), model kimlikleri ve fiyatlar | Karar 0001, F5.1, M24.2 | Bekliyor |
| 12 | Konu bütçesi ölçümü: 1 konu × günde 2 tarama gerçek maliyeti → varsayılan sıklık | F11, M24.7 | Bekliyor |
| 13 | Room + BundledSQLiteDriver ile FTS5 | B1 | Bekliyor |
| 14 | Health Connect: Mi Band → Mi Fitness → uyku/adım akışı | M19.6 | Bekliyor |
| 15 | Geofence: Play Hizmetleri varlığı, arka plan olay gecikmesi | G6 | Bekliyor |
| 16 | Canlı güncelleme: `ProgressStyle`, "promoted ongoing", HyperOS odak bildirimi | G9 | Bekliyor |
| 17 | Arama durumu: `AudioManager.getMode` ile izinsiz algılama | Karar 0005-11 | Bekliyor |
| 18 | ALO 171 hattının güncelliği | M25.2 T1 | Bekliyor |
| 19 | `kur.sh` komutlarının HyperOS'te davranışı | Ek B | Bekliyor |

## Bulgular

_Henüz cihaz bulgusu yok._

### 7 Ekim 2026 — Cihaz kimliği ve ilk kurulum denemesi

- **Cihaz:** Xiaomi 17T Pro (`2602EPTC0G`, `warhol_global`), Android 16 (API 36), HyperOS `OS3.0.310.0.WPSMIXM`, güvenlik yaması 2026-08-01, `arm64-v8a`.
- **Sayfa boyutu:** `getconf PAGE_SIZE` = **4096**. Cihaz 16 KB sayfa kullanmıyor; yerel kütüphanelerde 16 KB uyumu yine de korunur (ileriye dönük), ama bu cihazda engel değil.
- **`adb install` engeli:** İlk deneme `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` ile reddedildi. HyperOS, Geliştirici seçenekleri → **USB ile yükle** açık değilse ya da telefondaki onay penceresi 10 sn içinde onaylanmazsa kurulumu reddeder. `kur.sh` bu hatayı yakalayıp Türkçe yönerge göstermeli. Durum: Kullanıcı'nın ayarı açması bekleniyor.

### 7 Ekim 2026 — Spike 1: alarm teslimi (ilk tur)

**Yöntem:** `:spike` uygulaması `setAlarmClock` ve `setExactAndAllowWhileIdle` alarmlarını aynı ana kurar; alıcı sapmayı (ms) cihaz korumalı depolamadaki `log.csv`'ye yazar. Telefon USB'ye bağlıydı. 9 çift ölçüldü.

| Koşul | `setAlarmClock` | `setExactAndAllowWhileIdle` |
|---|---|---|
| Ekran açık, uygulama arka planda | 13–36 ms | 7–18 ms |
| Ekran kapalı | 12–36 ms | 18–41 ms, iki kez ~510 ms |
| Ekran kapalı + bekleme kovası `rare` komutu verilmişken kurulan çift | **12 ms** | **211 246 ms (3 dk 31 sn geç)** |

**Sonuçlar:**
1. **`setAlarmClock` her koşulda ±40 ms içinde.** Kritik sınıf için blueprint kararı (G1) doğrulandı.
2. **`setExactAndAllowWhileIdle` bir kez 3,5 dakika gecikti.** `dumpsys alarm` alarmı geçmiş zamanına rağmen beklemede gösteriyordu; aynı ana kurulan `setAlarmClock` zamanında çaldı. Olası neden bekleme kovası / pil politikası ertelemesi; kök neden henüz ayrıştırılmadı. **Karar adayı:** ±1 dk sözü verilen her şey (kritik sınıf ve karar 0003'teki ısrarlı takip) `setAlarmClock` kullanmalı; `setExactAndAllowWhileIdle` yalnız birkaç dakikalık kaymanın önemsiz olduğu yerde. Tekrar testiyle doğrulanınca karar kaydı yazılır.
3. **Zorla durdurma sonrası:** `am force-stop` uygulamanın alarmlarını siler. Uygulama yeniden açıldığında sistem `LOCKED_BOOT_COMPLETED` ve `BOOT_COMPLETED` yayınlarını gönderdi ve alıcı bekleyen alarmları yeniden kurdu (`REARMED`). Yani yeniden planlama alıcısı, zorla durdurmadan çıkışta da çalışıyor; ama uygulama açılana kadar alarm yok.
4. **Bildirim:** `POST_NOTIFICATIONS` verildiğinde alıcıdan bildirim gönderimi her seferinde başarılı.
5. **Bekleme kovası:** `am set-standby-bucket … rare` kalıcı olmadı (okununca 20 = WORKING_SET, sonra 10 = ACTIVE). Kısıtlı kova testi için başka yöntem gerek.
6. **Doze zorlanamadı:** `dumpsys deviceidle force-idle` → "Unable to go deep idle; stopped at INACTIVE". `dumpsys battery unplug`'a rağmen `mCharging=true` görünüyor (USB bağlı). Doze testi kablosuz hata ayıklamayla ya da kablo çekilip gerçek bekleme süresiyle yapılacak.
7. **Araç notu:** Git Bash, `adb shell` argümanlarındaki `/data/...` yollarını Windows yoluna çeviriyor; betiklerde `MSYS_NO_PATHCONV=1` şart. `kur.sh` ve diğer betikler buna göre yazılır.

**Açık kalanlar:** gerçek Doze · gece (uzun bekleme) · 1 saat · son uygulamalardan kaydırarak kapatma · yeniden başlatma · kilitli yeniden başlatma · kısıtlı kova · madde 2'nin tekrar testi.
