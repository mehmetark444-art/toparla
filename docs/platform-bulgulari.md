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
| Gradle | Sistemde yok; sarmalayıcı önbelleğinde 8.12 ve 8.14 |
| Git | 2.52 |
| Telefon | `adb devices` listesi boş: henüz bağlı değil |

## S0 spike listesi

Sıra: önce ürünü taşıyan ve en kırılgan varsayımlar.

| # | Spike | Blueprint | Durum |
|---|---|---|---|
| 1 | **Alarm teslimi:** `setAlarmClock` ↔ `setExactAndAllowWhileIdle`; 2 dk / 1 sa / gece / Doze / uygulama kapalı / yeniden başlatma / kilitli yeniden başlatma / bekleme kovası rare | G1, J2 | Sırada (ilk) |
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
