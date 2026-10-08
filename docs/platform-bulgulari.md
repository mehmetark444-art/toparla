# Platform bulguları

Cihaz: Xiaomi 17T Pro · Android 16 · HyperOS 3. Her `[DOĞRULA]` / `[Spike]` maddesi
burada kapanır: **ne denendi · nasıl · sonuç · karar**.

## Geliştirme ortamı (7 Ekim 2026)

| Öğe | Bulgu |
|---|---|
| İşletim sistemi | Windows 11 Home |
| Android Studio | 2025.2.2, `C:\Program Files\Android\Android Studio` |
| JDK | Android Studio JBR 21.0.8 (ayrı JDK yok; sistemde `JAVA_HOME` tanımsız, Claude Code oturumlarında `.claude/settings.json` verir) |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk`; platformlar 33, 34, **36**; build-tools 35.0.0, 36.1.0; NDK 28.2 |
| adb | 36.0.2, PATH'te değil (`…\Sdk\platform-tools\adb.exe`) |
| Gradle | Sarmalayıcı 9.6.0 (AGP 9.4.1 gereği); JBR 21 ile koşar |
| Derleme | `JAVA_HOME` = Android Studio `jbr` verilerek `./gradlew :domain:test :app:assembleDebug` başarılı (ilk derleme 2 dk 36 sn) |
| Not | Kurulu Android Studio 2025.2.2, AGP 9.4.1 projesini açmak için eski olabilir `[DOĞRULA]`; komut satırı derlemesi etkilenmez |
| Git | 2.52 |
| Telefon | 7 Ekim 21:55'te USB ile bağlandı ve yetkilendirildi; kimliği aşağıda. `adb` için `./scripts/adb` |

## S0 spike listesi

Sıra: önce ürünü taşıyan ve en kırılgan varsayımlar. Bu tablo spike **tanımlarını** tutar;
güncel durum ve madde işaretleri yalnız `docs/yol-haritasi.md` F1 bölümündedir.

| # | Spike | Blueprint | Güncel durum |
|---|---|---|---|
| 1 | **Alarm teslimi:** `setAlarmClock` ↔ `setExactAndAllowWhileIdle`; 2 dk / 1 sa / gece / Doze / uygulama kapalı / yeniden başlatma / kilitli yeniden başlatma / bekleme kovası rare | G1, J2 | yol haritası F1.1 |
| 2 | Kesin alarmdan FGS başlatma muafiyeti; `specialUse` FGS; her başlatma yolu | G1, G2 | yol haritası F1 |
| 3 | Tam ekran bildirim + HyperOS "Kilit ekranında göster", "Arka planda açılır pencere" | G1 | yol haritası F1 |
| 4 | HyperOS ayar derin bağlantıları (otomatik başlatma, pil, kısıtlı ayarlar) → `hyperos-baglantilar.md` | G3 | yol haritası F1 |
| 5 | Erişilebilirlik: uygulama açılışı algılama gecikmesi (hedef ≤ 400 ms), servis ömrü, servisten Activity başlatma; olmazsa overlay yedeği | G3, G4, G10, D11 | yol haritası F1 |
| 6 | Bildirim erişimi: yan yüklemede kısıtlı ayarlar, `allow_listener`, kopma sonrası `requestRebind` | G4 | yol haritası F1 |
| 7 | Bildirim "cooldown" davranışı ve alarm ses akışı; DND aşımı | G1 | yol haritası F1 |
| 8 | Tile: kilitliyken `LockCaptureActivity`, Tile → mikrofon ≤ 1 sn | G9 | yol haritası F1 |
| 9 | Türkçe cihaz içi STT: 30 cümlelik set (sayı, tarih, özel isim) WER; yetersizse yerel Whisper | G7 | yol haritası F1 |
| 10 | Cihaz içi model: LiteRT-LM + Gemma sürümü `[DOĞRULA: güncel adlar]`, 16 KB sayfa uyumu, GPU/NPU, token/sn, ilk token, 10 dk sıcaklık; 50 örnek Türkçe kalite | G8, F2 | yol haritası F1 |
| 11 | **Gemini API:** anahtarın uç noktası (Developer API ↔ Vertex AI), akış, işlev çağrısı, şemalı çıktı, bağlam önbellekleme, görsel girdi, Google Arama temellendirmesi (atıf alanları, maliyet), model kimlikleri ve fiyatlar | Karar 0001, F5.1, M24.2 | yol haritası F1 |
| 12 | Konu bütçesi ölçümü: 1 konu × günde 2 tarama gerçek maliyeti → varsayılan sıklık | F11, M24.7 | yol haritası F1 |
| 13 | Room + BundledSQLiteDriver ile FTS5 | B1 | yol haritası F1 |
| 14 | Health Connect: Mi Band → Mi Fitness → uyku/adım akışı | M19.6 | yol haritası F1 |
| 15 | Geofence: Play Hizmetleri varlığı, arka plan olay gecikmesi | G6 | yol haritası F1 |
| 16 | Canlı güncelleme: `ProgressStyle`, "promoted ongoing", HyperOS odak bildirimi | G9 | yol haritası F1 |
| 17 | Arama durumu: `AudioManager.getMode` ile izinsiz algılama | Karar 0005-11 | yol haritası F1 |
| 18 | ALO 171 hattının güncelliği | M25.2 T1 | yol haritası F1 |
| 19 | `kur.sh` komutlarının HyperOS'te davranışı | Ek B | yol haritası F1 |

## Bulgular

### 7 Ekim 2026 — Cihaz kimliği ve ilk kurulum denemesi

- **Cihaz:** Xiaomi 17T Pro (`2602EPTC0G`, `warhol_global`), Android 16 (API 36), HyperOS `OS3.0.310.0.WPSMIXM`, güvenlik yaması 2026-08-01, `arm64-v8a`.
- **Sayfa boyutu:** `getconf PAGE_SIZE` = **4096**. Cihaz 16 KB sayfa kullanmıyor; yerel kütüphanelerde 16 KB uyumu yine de korunur (ileriye dönük), ama bu cihazda engel değil.
- **`adb install` engeli:** İlk deneme `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user` ile reddedildi. HyperOS, Geliştirici seçenekleri → **USB ile yükle** açık değilse ya da telefondaki onay penceresi 10 sn içinde onaylanmazsa kurulumu reddeder. `kur.sh` bu hatayı yakalayıp Türkçe yönerge göstermeli. Sonuç: ayar zaten açıkmış (`persist.security.adbinstall=1`); retlerin nedeni telefondaki onay penceresinin onaylanmaması, bir kez de yanlışlıkla reddedilmesiydi. Sonraki denemede `Success`.

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

### 7 Ekim 2026 — Spike 1: son uygulamalardan kaydırarak kapatma

Kullanıcı uygulamada 2 dk'lık çifti kurdu, uygulamayı son uygulamalardan kaydırıp kapattı, telefona dokunmadı. HyperOS ayarları varsayılan (otomatik başlatma verilmedi, pil kısıtı değiştirilmedi, kilit rozeti yok).

- İki alarm da çaldı: `setExactAndAllowWhileIdle` +130 ms, `setAlarmClock` +152 ms; bildirim gönderildi. Sapmanın önceki ~15–40 ms'den yüksek olması sürecin soğuk başlatıldığını (yani kaydırmanın süreci gerçekten öldürdüğünü) düşündürüyor.
- Paket durumu `stopped=false`: HyperOS 3'te kaydırarak kapatma **zorla durdurma değil**; alarmlar korunuyor.
- Sınır: tek deneme, 2 dk ufuk, ekran kapalı ama USB bağlı. Uzun ufuk (1 sa, gece) ve Güvenlik uygulamasının "Bellek temizleme"si ayrıca denenecek.

### 7 Ekim 2026 — Spike 1: yeniden başlatma

5 dk'lık çift kuruldu (plan 22:23:19), 22:18:27'de `adb reboot`. Cihaz 22:19:01'de adb'ye döndü.

- Açılışta `LOCKED_BOOT_COMPLETED` (22:19:12) ve 30 ms sonra `BOOT_COMPLETED` geldi; alıcı her ikisinde bekleyen 2 alarmı yeniden kurdu (çift kurulum idempotent: aynı `PendingIntent`, tek teslim).
- İki alarm da zamanında: `setExactAndAllowWhileIdle` +26 ms, `setAlarmClock` +51 ms; bildirim gönderildi. **Yeniden başlatma sonrası yeniden planlama çalışıyor**; yeniden başlatmadan alarmların geri kurulmasına ~45 sn (hedef ≤ 60 sn, S10).
- **Kilitli (Direct Boot) senaryosu DOĞRULANMADI.** `dumpsys lock_settings` → `CredentialType: NONE`: telefonda güvenli ekran kilidi yok (22:08:31'de kaldırılmış). Kilit olmayınca depolama açılışta kendiliğinden açılır; `BOOT_COMPLETED`'in hemen gelmesi bunu gösteriyor. Cihaz korumalı depolamadan okuma kodu çalıştı ama kilit açılmadan önceki pencere hiç oluşmadı. Ekran kilidi geri konunca tekrarlanacak.

### 7 Ekim 2026 — Spike 1: kilitli yeniden başlatma (Direct Boot) — DOĞRULANDI

Ekran kilidi PIN olarak kuruldu (`CredentialType: PIN`). 5,5 dk'lık çift kuruldu (plan 22:31:27), 22:26:10'da `adb reboot`. Kullanıcı ne SIM PIN'i ne ekran PIN'i girdi.

- Alarm anında ve sonrasında `dumpsys user` → `RUNNING_LOCKED`. Yalnız `LOCKED_BOOT_COMPLETED` geldi (22:26:40, yeniden başlatmadan ~30 sn sonra); `BOOT_COMPLETED` gelmedi.
- `directBootAware` alıcı, cihaz korumalı depolamadaki bekleyen listeden 2 alarmı yeniden kurdu.
- İkisi de kilitliyken çaldı: `setExactAndAllowWhileIdle` +132 ms, `setAlarmClock` +153 ms; bildirim gönderildi (`notifEnabled=true`), Kullanıcı kilit ekranında gördü.
- **Sonuç:** G1'deki Direct Boot tasarımı (alarm kurmaya yetecek en küçük veri cihaz korumalı depolamada + `directBootAware` alıcılar) bu cihazda çalışıyor.
- Araç notu: kilitliyken `run-as` çalışmıyor (kimlik korumalı dizin yok); kayıt `logcat -s TOPARLA_SPIKE` ile okundu. Ürün tanılamasında da kilitli dönem kayıtları cihaz korumalı depolamaya yazılmalı.

### 7–8 Ekim 2026 — Spike 1: gece testi (KURULDU, sonuç bekleniyor)

**Amaç:** gerçek Doze, uzun ufuk (1–8 sa), HyperOS'in gece süreç/alarm temizliği, üç yöntemin karşılaştırması, ikinci yöntemdeki 3,5 dk gecikmenin tekrarı.

**Kurulum (22:35):** saat başı 8 üçlü, toplam 24 alarm: 23:35 · 00:35 · 01:35 · 02:35 · 03:35 · 04:35 · 05:35 · 06:35. Her üçlüde `setAlarmClock` (kritik adayı), `setExactAndAllowWhileIdle` (önemli adayı), `setAndAllowWhileIdle` (normal adayı; esnek). Sistemde 24 alarm kayıtlı olduğu `dumpsys alarm` ile doğrulandı.

**Koşullar:** USB çekili, ekran kapalı, dokunulmadan. HyperOS ayarları varsayılan: otomatik başlatma yok, pil muafiyeti yok, son uygulamalarda kilit yok. Ekran kilidi PIN. Pil başlangıç %66. Bildirim kanalı sessiz (`IMPORTANCE_LOW`).

**Kayıt:** her teslimde sapma (ms), Doze (`idle`), hafif Doze (`light`), ekran, pil yüzdesi, bekleme kovası.

**Okunacaklar:** yöntem başına sapma dağılımı · çalmayan alarm var mı · `MISSED_DETECTED` / `RESCHEDULE` satırı var mı (süreç/alarm temizliği izi) · Doze'a girildi mi · gece pil tüketimi.

### 7 Ekim 2026 — Spike 11: Gemini API (ilk bakış, ücret engeline takıldı)

- **Uç nokta:** Anahtar **Gemini Developer API** ile çalışıyor (`https://generativelanguage.googleapis.com/v1beta`, başlık `x-goog-api-key`). Model listesi HTTP 200. Vertex AI uç noktası (`aiplatform.googleapis.com`) 403 `SERVICE_DISABLED`: projede açık değil, gerek de yok. → `GeminiClient` Developer API'yi hedefler.
- **Listelenen modeller (seçme):** `gemini-3.8-flash`, `gemini-3.7-flash`, `gemini-3.6-flash`, `gemini-3.5-flash`, `gemini-3.5-flash-lite`, `gemini-3.1-flash-lite`, `gemini-3.1-pro-preview`, takma adlar `gemini-flash-latest` / `gemini-flash-lite-latest` / `gemini-pro-latest`, gömme `gemini-embedding-2`, barındırılan `gemma-4-26b-a4b-it` ve `gemma-4-31b-it`. Hepsi 1 048 576 giriş / 65 536 çıkış token sınırı ve "thinking" destekli. `gemini-2.5-flash-lite` yeni kullanıcılara kapalı (404).
- **Kademe adayları (doğrulanmadı):** hızlı = Flash-Lite ailesi · günlük = Flash ailesi · derin = Pro ailesi. Kesin kimlik ve fiyat, üretim çağrıları açılınca resmi fiyat sayfasıyla birlikte kilitlenecek.
- **ENGEL:** Her üretim ve gömme çağrısı **HTTP 402** döndü: "Your prepayment credits are depleted" (ön ödemeli bakiye bitmiş). Ücretsiz çalışan model yok (Gemma dahil). Akış, şemalı çıktı, işlev çağrısı, Google Arama temellendirmesi, önbellekleme ve maliyet ölçümü bakiye yüklenene kadar denenemiyor.
- **Ürün notu:** 402 "bakiye bitti" durumu `AiUnavailable` olarak ele alınmalı (Katman 1'e düş) ve Ayarlar → AI'da açık bir satırla gösterilmeli; kullanıcıya hata olarak yansımaz.
- Anahtar yalnız gitignore'daki `secrets.properties` dosyasında; depoya girmedi.

### 7 Ekim 2026 (23:10) — Spike 11: bakiye yüklendi denildi, çağrılar hâlâ 402

Kullanıcı bakiye sorununu çözdüğünü bildirdi. 23:09–23:14 arasında 6 üretim çağrısı (üç model) yine HTTP 402 "prepayment credits are depleted" döndü. Resmi belgeye göre (ai.google.dev/gemini-api/docs/billing): bakiye **faturalandırma hesabına** bağlıdır, kart ödemesi çoğunlukla anında, güncelleme gecikebilir; anahtar yenilemek gerekmez. Olası nedenler: ödeme henüz yansımadı · bakiye bu anahtarın projesinin (662439627265) bağlı olmadığı başka bir faturalandırma hesabına yüklendi · ödeme tamamlanmadı. Kullanıcı'dan AI Studio'daki durum istenecek. Sonuç: bakiye başka hesaba yüklenmiş; doğru hesaba yüklenmesi bekleniyor.

### 8 Ekim 2026 — Spike 1: gece testi sonucu

**Koşul:** 7 Ekim 22:35'te kurulan 8 üçlü (23:35 … 06:35), 24 alarm. Kablo çekili, HyperOS ayarları
varsayılan (otomatik başlatma, pil muafiyeti, kilit rozeti yok). Kayıt 8 Ekim sabahı USB ile okundu.
24 alarmın 24'ü çaldı; `MISSED_DETECTED` ve `RESCHEDULE` satırı yok (süreç/alarm temizliği izi yok).

| Saat | Ekran / hafif Doze | `setAlarmClock` | `setExactAndAllowWhileIdle` | `setAndAllowWhileIdle` |
|---|---|---|---|---|
| 23:35 | açık / hayır | 162 ms | 118 ms | **4 sa 0 dk geç** (03:35'te) |
| 00:35 | açık / hayır | 166 ms | 216 ms | **3 sa 0 dk geç** (03:35'te) |
| 01:35 | açık / hayır | 152 ms | 120 ms | **2 sa 0 dk geç** (03:35'te) |
| 02:35 | kapalı / evet | 157 ms | 124 ms | **1 sa 0 dk geç** (03:35'te) |
| 03:35 | kapalı / evet | 18 ms | 561 ms | **4 sa 54 dk geç** (08:30'da) |
| 04:35 | kapalı / hayır | 1 427 ms | 19 410 ms | **3 sa 54 dk geç** (08:30'da) |
| 05:35 | kapalı / hayır | 1 282 ms | 1 751 ms | **2 sa 54 dk geç** (08:30'da) |
| 06:35 | kapalı / evet | 1 124 ms | 28 111 ms | **1 sa 54 dk geç** (08:30'da) |

**Sonuçlar:**
1. `setAlarmClock`: 8/8, en çok 1,4 sn. Kritik sınıf için doğrulandı.
2. `setExactAndAllowWhileIdle`: 8/8, en çok 28 sn; ±1 dk içinde. 7 Ekim'deki 211 sn'lik gecikme
   8 ölçümde tekrarlanmadı; kök nedeni hâlâ bilinmiyor.
3. `setAndAllowWhileIdle`: zamanlı hiçbir iş için kullanılamaz. İlk üçü **ekran açıkken** bile
   çalmadı; dördü 03:35'te, kalan dördü 08:30'da ekran açılınca toplu çaldı. Neden bilinmiyor.
4. Uzun bekleme (ekran kapalı, 04:35'ten sonra) kesin alarmlarda sapmayı ~0,15 sn'den 1–28 sn'ye çıkardı.

**Sınırlar:** tek gece. Alarm anında `isDeviceIdleMode` hiç `true` görülmedi (yalnız hafif Doze
3 kez); **derin Doze altında teslim doğrulanmadı.** İlk üç saatte ekran açıktı (telefon kullanılıyordu).
Pil 66 → 45; arada şarj görüldüğü (66 → 69) ve telefon kullanıldığı için pil ölçümü sayılmaz.

**Karar:** `docs/decisions/0006-sinif-alarm-yolu.md`.

### 8 Ekim 2026 — Spike 11: Gemini API ölçümleri

Bakiye doğru hesaba yüklendi; üretim çağrıları HTTP 200. Uç nokta
`generativelanguage.googleapis.com/v1beta`, başlık `x-goog-api-key`. Her satır **tek çağrıdır**.

| Deneme | Model | Sonuç |
|---|---|---|
| Düz metin (Türkçe, sistem talimatlı) | `gemini-3.5-flash-lite` | 1,1 sn; 47 girdi + 34 çıktı token; düşünme yok |
| Düz metin | `gemini-3.8-flash` | 7,9 sn; 33 çıktı + **632 düşünme** token |
| Düz metin | `gemini-pro-latest` (= `gemini-3.1-pro-preview`) | 11,2 sn; 30 çıktı + **1 013 düşünme** token |
| Düşünme ayarı | `gemini-3.8-flash` | `thinkingLevel:"low"` ve `thinkingBudget:0` çalışıyor: 2,3 sn, düşünme tokeni yok. `"minimal"` bu modelde 400 |
| Şemalı çıktı (`responseMimeType` + `responseSchema`) | `gemini-3.5-flash-lite` | 1,0 sn; geçerli JSON, 3 öğe. Sınıflama hatası: "kedi maması bitmiş" → WORRY |
| İşlev çağrısı (`functionDeclarations`) | `gemini-3.8-flash` | Tek yanıtta iki çağrı (`create_reminder`, `create_task`), argümanlar şemaya uygun. `low` ayarına rağmen 465 düşünme tokeni, 7,1 sn |
| Google Arama temellendirmesi (`tools:[{google_search:{}}]`) | `gemini-3.5-flash-lite` | 2,9 sn; `groundingChunks` (uri + title), `groundingSupports`, `webSearchQueries`, `searchEntryPoint` |
| Akış (`streamGenerateContent?alt=sse`) | `gemini-3.5-flash-lite` | İlk bayt 1,0 sn; 4 `data:` olayı |

**Ürün için sonuçlar:**
1. Düşünme tokenleri çıktı fiyatından ücretlenir ve gecikmeyi 3–5 kat artırır; etkileşimli işte `low`.
   Araç tanımı varken `low` yine de düşünebiliyor: sesli yanıt hedefi (≤ 5 sn) günlük kademede riskli.
2. Arama atıflarında `uri` gerçek adres değil, `vertexaisearch.cloud.google.com/grounding-api-redirect/…`
   yönlendirmesidir; alan adı `title` alanındadır. Konu Motoru'nun "URL yalnız arama sonuçlarından"
   kuralı `groundingChunks` üzerinden uygulanır; gerçek adres ve yayın tarihi için yönlendirme izlenmelidir.
3. Fiyat (resmi sayfa, 7 Ekim 2026): flash-lite 0,30/2,50 · 3.8-flash 0,75/3,75 · 3.1-pro 2,00/12,00 $
   (1M token). Arama: ayda 5 000 ücretsiz, sonra 1 000'i 14 $.

**Denenmedi:** görsel girdi · bağlam önbellekleme · arama + şemalı çıktı birlikte · 429 / hız sınırı
davranışı · gerçek bir konu taramasının uçtan uca maliyeti · Türkçe kalite (altın set).

**Karar:** `docs/decisions/0007-gemini-model-kademeleri.md`.

### 8 Ekim 2026 — Spike 2 ve 3: alarmdan servis başlatma, kilitliyken tam ekran bildirim

**Koşul:** USB bağlı, ekran kapalı ve kilitli (`isKeyguardShowing=true`, `Dozing`), kilit türü PIN.
HyperOS ayarlarına elle dokunulmadı; pil muafiyeti yok. Her biri **tek deneme**.

- **Servis (spike 2):** `setAlarmClock` alarmının alıcısından `startForegroundService` çağrısı kabul
  edildi; `specialUse` tipli servis `startForeground`'u alarmdan 29 ms sonra başarıyla çağırdı
  (`FGS_REQUESTED` → `FGS_STARTED`, istisna yok).
- **Tam ekran (spike 3):** `canUseFullScreenIntent()` = `true` (appop `USE_FULL_SCREEN_INTENT: allow`,
  kendiliğinden). `setFullScreenIntent` taşıyan bildirim gönderildikten 245 ms sonra (alarmdan 270 ms)
  `showWhenLocked` + `turnScreenOn` Activity açıldı: `keyguardLocked=true`, ekran uyandı,
  en üstteki Activity `FullScreenActivity`.
- HyperOS'e özgü "Kilit ekranında göster" / "Arka planda açılır pencere" izinleri elle verilmeden
  çalıştı. Ham appop dökümü: `MIUIOP(10020): allow`, `MIUIOP(10008): ignore`, `MIUIOP(10017): ask`,
  `MIUIOP(10053): ignore`, `SYSTEM_ALERT_WINDOW: default` (reddedildi). Bu numaraların hangi izne
  karşılık geldiği doğrulanmadı.

**Sınırlar / açık:** ekran açık ve kilitsizken davranış (beklenen: tam ekran yerine üstten bildirim) ·
`setExactAndAllowWhileIdle` alarmından servis başlatma · bildirim eylemi, Tile ve widget'tan servis
başlatma · uzun süre arka planda kaldıktan sonra tekrar · release yapısında tekrar ·
Kullanıcı'nın gördüğünün teyidi.

Kullanici teyidi (8 Ekim): tam ekran karti kilit ekraninda kendi gozuyle gordu ve Tamam dugmesine basti (kayit: FSI_TAPPED).

### 8 Ekim 2026 — Spike 5: erişilebilirlik servisi (ilk tur: servis öldü, geri gelmedi)

**Kurulum:** `:spike` içinde `AppOpenAccessibilityService` (yalnız `typeWindowStateChanged`,
`canRetrieveWindowContent=false`, izlenen paketler YouTube ve Hesap Makinesi). Kullanıcı servisi
Ayarlar → Erişilebilirlik'ten elle açtı; "kısıtlı ayar" engeli çıkmadı (yan yüklenen debug APK).

**Olanlar (sistem günlüğünden):**
- 17:19:12 servis bağlandı (`A11Y_CONNECTED`), 17:19:25'te çözülüp 17:19:28'de yeniden bağlandı.
- 17:19:38 uygulamanın görevi son uygulamalardan kaldırıldı; HyperOS süreci öldürdü:
  `ProcessSceneCleaner: SwipeUpClean: kill procName=com.toparla.spike` → `Killing … SwipeUpClean`.
- Bundan sonra `dumpsys accessibility`: servis **Enabled** listesinde ama **Bound services: {}** ve
  **Crashed services** içinde. Kullanıcı YouTube ve Hesap Makinesi'ni açtı: hiç olay gelmedi, kart çıkmadı.
- Uygulama yeniden başlatıldı (`am start`): servis **yine bağlanmadı**; Hesap Makinesi açıldı, olay yok.

**Sonuç:** HyperOS 3'te uygulama son uygulamalardan kaydırılınca erişilebilirlik servisi ölüyor ve
kendiliğinden geri gelmiyor; ayar ekranında "açık" görünmeye devam ediyor. (Aynı kaydırma alarmları
silmiyordu.) Blueprint G3 bunu "öldürülebilir" diye öngörmüştü; ölçüm, bunun olağan kullanımda her
kaydırmada olabileceğini gösteriyor. Algılama gecikmesi henüz ölçülemedi.

**Sınanacak önlemler:** son uygulamalarda kilit · otomatik başlatma izni · pil "kısıtlama yok" ·
ana Activity'yi son uygulamalardan gizleme · servisi ayrı süreçte çalıştırma · yeniden başlatma
sonrası servis geri geliyor mu · sağlık denetimi (Enabled ama bağlı değil → uyarı). Yedek: kullanım
istatistikleriyle gecikmeli algılama.

### 8 Ekim 2026 — Spike 5: algılama gecikmesi (ikinci tur)

Kullanıcı servisi kapatıp açtı; servis bağlandı. Ölçüm: `scripts/spike-a11y-olc.sh` iki uygulamayı
17 sn arayla açtırır; `dispatchMs` = olayın oluşması → servise ulaşması, `sinceEventMs` = olay → kartın
ilk çizimi. Kullanıcı kartı iki kez kendi gözüyle gördü.

| Koşul | Ölçüm | Olay → servis | Olay → kart çizildi |
|---|---|---|---|
| Uygulama **açılışı** (soğuk/ılık), yalın servis | 6 | 2 837–2 941 ms (biri 101 ms) | 2 910–3 028 ms |
| Açılış + sürekli foreground service açık | 5 | 2 906–2 928 ms | 2 974–2 997 ms |
| Açılış + `flagRetrieveInteractiveWindows` (içerik yetkisi yok; `capabilities=0`, etkisiz) | 4 | 2 911–2 926 ms | 2 972–3 017 ms |
| Açılış + ek olay türleri (içerik değişti, odak) | 6 | 2 915–2 979 ms | 2 962–3 066 ms |
| Açılış olmayan geçiş (ayarlardan dönüş, geri/ana ekran) | 2 | 101–106 ms | 141–153 ms |

**Sonuçlar:**
1. Olay servise ulaştıktan sonra kart ~50–90 ms'de çiziliyor: servisten Activity başlatma çalışıyor
   ve hızlı; `SYSTEM_ALERT_WINDOW` yedeğine gerek görünmüyor.
2. **Uygulama açılışında olay servise ~2,9 sn geç ulaşıyor** (21 ölçümün 20'si). Hedef ≤ 400 ms
   tutmuyor. Gecikme sabit; sürekli servis, pencere bayrağı ve ek olay türleri değiştirmedi.
   **Neden bilinmiyor** (sistem tarafında bekletme ya da HyperOS'in açılış sırasındaki davranışı olabilir).
3. İzlenen paket süzgeci bağlanma anında sızdırdı: ilk olay `com.android.settings`'ten geldi. Üründe
   paket denetimi kodda da yapılmalı.
4. Paket güncellemesinden (`adb install -r`) sonra servis kendiliğinden yeniden bağlandı (3 kez).
5. Yan etki: erişilebilirlik servisi açıkken YouTube, oynatıcı için erişilebilirlik denetimlerini
   açmayı öneren kendi penceresini gösterdi. Başka uygulamalar servisin açık olduğunu görebiliyor.

**Denenmedi:** içerik okuma yetkisi açık sürüm (karar 0008, F1.24) · pil muafiyeti / otomatik
başlatma · kullanım istatistiklerini sık sorgulama · release yapısı · uzun süre sonra servis ömrü.

### 8 Ekim 2026 — Spike 5: son uygulamalarda kilit servisi "tümünü temizle"den koruyor

Kullanıcı Toparla Spike kartını son uygulamalarda kilitledi, sonra "tümünü temizle"ye bastı
(sistem günlüğü sıfırlandıktan sonra, tek kontrollü deneme).

- Günlük: `ProcessSceneCleaner: OneKeyClean` YouTube, kamera ve diğer süreçleri öldürdü;
  `com.toparla.spike` için öldürme satırı yok.
- Süreç kimliği değişmedi (6121), görev son uygulamalarda kaldı, `dumpsys accessibility`:
  **Bound services** içinde, **Crashed** boş. Öncesindeki ölçümde servis iki açılışı da algıladı.
- Kilitsizken aynı işlem (7 Ekim: `SwipeUpClean`) süreci öldürmüş ve servisi düşürmüştü.

**Sonuç:** kilit, "tümünü temizle"ye karşı koruyor (tek deneme). Kurulum sihirbazı bu adımı zorunlu
göstermeli; sağlık denetimi "servis açık ama bağlı değil" durumunu yakalamalı.
**Açık:** kilit yeniden başlatmadan ve uygulama güncellemesinden sonra kalıyor mu · Güvenlik
uygulamasının derin temizliği · saatler sonra.

### 8 Ekim 2026 — Spike 5: yeniden başlatma sonrası servis

17:47:02 `adb reboot`; Kullanıcı PIN girdi, 17:47:53'te `RUNNING_UNLOCKED`. Tek deneme.

- Servis kendiliğinden bağlandı: `LOCKED_BOOT_COMPLETED`'den 19 sn sonra `A11Y_CONNECTED`
  (kilit açılmadan önce bağlanmıyor; servis `directBootAware` değil). `Bound services` içinde, `Crashed` boş.
- Ardından iki açılış algılandı (olay → kart 2 785 ve 2 939 ms; gecikme aynı).
- Son uygulamalardaki kilidin yeniden başlatmadan sonra durup durmadığı komutla okunamadı.
  Kullanıcı gözle baktı: "kilit duruyor" (beyan; komutla doğrulanmadı).

### 8 Ekim 2026 — Spike 10: cihaz içi model, kaynak araştırması

Kaynaklar: developers.google.com/edge/litert-lm/android, Google Maven, Hugging Face API (8 Ekim 2026).

- **Çalışma zamanı:** LiteRT-LM, `com.google.ai.edge.litertlm:litertlm-android:0.18.0`. Kotlin API:
  `Engine(EngineConfig(modelPath, backend))` → `initialize()` → `createConversation(ConversationConfig(…))`
  → `sendMessageAsync(…)`. Arka uçlar `Backend.CPU()`, `Backend.GPU()`, `Backend.NPU(…)`. Araç çağrısı
  (`ToolSet`, `@Tool`), düşünme ayarı, görsel ve ses girdisi, ayrıca `EmbeddingEngine` var.
  Kütüphane APK'yı 2,7 MB'tan 56 MB'a çıkardı (blueprint APK bütçesi ≤ 60 MB: sınırda).
- **Model biçimi:** `.litertlm`; kaynak `huggingface.co/litert-community`. Denenen bütün adaylar
  **girişsiz ve açık lisanslı** indiriliyor (blueprint'in "lisans onayı gerekebilir" kaygısı geçersiz;
  Kullanıcı'nın elle indirmesine gerek yok).
- **Cihaz:** SoC MediaTek MT6993, RAM ~11 GB, 421 GB boş. Hazır NPU derlemesi yalnız gömme modelinde
  var (`…_MediaTek_MT6993.litertlm`); üretici modellerde CPU/GPU kullanılacak.
- **Adaylar (Kullanıcı kararı: Gemma şart değil; en iyi Türkçe ve RAG sonucu veren seçilir):**

| Model | Dosya | Lisans |
|---|---|---|
| Gemma 4 E2B | 2,59 GB (GPU sürümü 2,01 GB) | Apache 2.0 |
| Gemma 4 E4B | 3,66 GB (GPU sürümü 2,97 GB) | Apache 2.0 |
| Qwen3 4B Instruct 2507 | 2,66 GB | Apache 2.0 |
| Phi-4 mini instruct | 3,91 GB | MIT |
| Ministral 3 3B Instruct | 2,34 GB | Apache 2.0 |
| LFM2.5 2.6B | 1,67 / 2,87 GB | "other" (lisans okunacak) |
| Gömme: EmbeddingGemma 2 text 270m | 0,16 GB (MT6993 NPU sürümü 0,39 GB) | Apache 2.0 |

Ölçüm düzeneği `:spike/LlmSpike` (yükleme süresi, ilk parça, hız, ısı, Türkçe çıktı).

### 8 Ekim 2026 — Spike 10: Gemma 4 E2B ilk ölçüm (cihazda çalışıyor)

Dosya `gemma-4-E2B-it.litertlm` (2 588 147 712 bayt; boyut kaynakla eşit), bilgisayara indirilip
`adb push` ile uygulamanın harici dosya alanına atıldı (60 sn, 41 MB/sn). 4 Türkçe istem, her koşuda
bir kez; 3 koşu (GPU, GPU, CPU). USB bağlı, ekran açık.

| Ölçüm | GPU 1. koşu | GPU 2. koşu | CPU |
|---|---|---|---|
| Yükleme (`initialize`) | **34,4 sn** (ilk kez) | 2,2 sn | 2,1 sn |
| İlk parça | 0,27–2,0 sn | 0,37–0,95 sn | 0,57–0,72 sn |
| Bölme (JSON, ~270 karakter) | 10,2 sn | 11,5 sn | 9,2 sn |
| Mikro-adım | 0,8 sn | 1,1 sn | 1,9 sn |
| Sınıflama (tek kelime) | 0,4 sn | 0,5 sn | 0,9 sn |
| Bildirim metni | 1,8 sn | 2,4 sn | 2,7 sn |
| Isıl durum | 0 (yok) | 0 | 0 |

**Hız:** kısa işler 0,4–2,7 sn (blueprint hedefi: cihaz içi adım ≤ 4 sn ✓, ilk parça ≤ 2 sn ✓).
Uzun JSON ~25–33 karakter/sn. İlk yükleme 34 sn (hedef ≤ 6 sn ✗); sonraki yüklemeler ~2 sn ✓ —
ilk kullanımda bir kez ısındırma gerekir. GPU ile CPU arasında bu kısa işlerde belirgin fark yok.

**Türkçe kalite (4 örnek; ölçüm değil izlenim):**
- Bölme: üç iş doğru ayrıldı, Türkçesi düzgün. **Şemaya uymadı:** `items` yerine `tasks`, izinli
  olmayan `APPOINTMENT` türü, kod çitleri. Şema doğrulayıcısı ve toleranslı ayrıştırma şart.
- Sınıflama: "bırakmalı mıyım" → `ENDISE` (doğru).
- Mikro-adım: "Yazmaya şimdi başla." — fiil var, nesne yok; blueprint kuralını karşılamıyor.
- Bildirim: "Faturanızın ödemesini hatırlatmak isterim. Lütfen ödeme planınızı kontrol edin." —
  suçlamıyor ama iki cümle ve "siz" diliyle; Güneş "sen" der.

### 8 Ekim 2026 — Spike 10: beş adayın karşılaştırması

**Yöntem:** `scripts/spike-llm-olc.sh <model> gpu`; aynı sistem talimatı, 11 Türkçe istem (2 bölme,
mikro-adım, 2 sınıflama, tarih, bildirim, ayna, karta dayalı yanıt [kartta var / yok], tıbbi sınır).
Her model **bir kez** koştu; GPU; USB bağlı. Bu bir eleme turudur, kalite ölçümü değildir.
Dosyalar indirildikten sonra boyutları kaynakla eşleştirilerek doğrulandı (ağ değişiminde bir indirme
koptu, `curl -C -` ile sürdürüldü).

| | Gemma 4 E2B | Gemma 4 E4B | Ministral 3 3B | Phi-4 mini | Qwen3 4B |
|---|---|---|---|---|---|
| Dosya | 2,59 GB | 3,66 GB | 2,34 GB | 3,91 GB | 2,66 GB |
| İlk yükleme | 33 sn | 38 sn | 27 sn | 79 sn | 20–41 sn |
| İlk parça | 0,3–1,4 sn | 0,7–2,2 sn | 1,6–3,8 sn | 3,0–6,9 sn | — |
| Kısa işler (toplam süre) | 0,5–3,4 sn | 1,1–5,9 sn | 2,0–10,6 sn | 5,9–26 sn | — |
| Uzun bölme (JSON) | 14,4 sn | 25,2 sn | 15,0 sn | 25,3 sn | — |
| Tarih ("haftaya salı akşam 7") | ✗ 14 Ekim, JSON'a sardı | ✓ 2026-10-13 19:00 | ✗ 5 Ekim | (okunmadı) | — |
| "kedi maması bitmiş" | ✗ GOREV | ✓ ALISVERIS | ✓ | (okunmadı) | — |
| "bırakmalı mıyım" | ✓ ENDISE | ✗ FİKİR | ✗ FİKİR | 469 karakterlik açıklama | — |
| Mikro-adım | "Yazmak için taslağı aç." | "E-postayı açıp taslağı oluştur." | anlamsız | — | — |
| Karta dayalı yanıt (var) | ✓ kısa | ✓ tam | ✗ önce "yok" dedi, sonra uydurdu | dağınık, yanlış ilişki kurdu | — |
| Karta dayalı yanıt (yok) | ✓ | ✓ | ✓ | süreç öldü | — |
| Tıbbi sınır (doz sorusu) | ✓ doktora yönlendirdi | ✓ | ✗ kendi önerilerini sıraladı | süreç öldü | — |
| Türkçe | düzgün | düzgün | bozuk | — | — |
| Sonuç | **aday (hızlı)** | **aday (isabetli)** | elendi | elendi | elendi |

**Elenenler:**
- **Phi-4 mini:** `lowmemorykiller` süreci öldürdü ("memavailable critical lower", 2 GB RSS + 0,5 GB
  takas) — 11 GB RAM'li bu telefonda 3,9 GB'lık q8 dosya GPU'da sığmıyor; ayrıca en yavaş aday.
- **Ministral 3 3B:** Türkçe hataları ("ödemi", "istiyorsun mu"), uydurma ve tıbbi sınır ihlali.
- **Qwen3 4B Instruct:** bu paket LiteRT-LM 0.18.0 ile kullanılamıyor. Sistem talimatıyla
  `Failed to apply template: … + operator on unsupported types string and sequence`; talimat isteme
  gömülünce her isteme aynı "mesajınız eksik" yanıtı (istem şablondan geçmiyor).

**Ortak gözlemler:**
- GPU arka ucu ilk yüklemede model başına 0,8–3,8 GB önbellek dosyası yazıyor (`…mldrift_weight_cache.bin`);
  sonraki yüklemeler bu sayede ~2 sn. Depolama hesabında model boyutunun yaklaşık iki katı ayrılmalı.
- Gemma modelleri "kod çiti ekleme" talimatına rağmen JSON'u çoğu kez ```` ```json ```` ile sardı; E2B bir
  kez düz metin yerine JSON döndürdü. Toleranslı ayrıştırma + şema doğrulama şart (blueprint F5.2).
- Hiçbir koşuda ısıl durum 0'dan çıkmadı (kısa koşular).

**Düzeltme (aynı gün, Kullanıcı isteğiyle yeniden deneme):** yukarıdaki tabloda Qwen3 "çalışmıyor",
Phi-4 mini "okunmadı / süreç öldü" görünüyor; ikisi de başka yolla yeniden koşuldu ve 11 istemin
hepsini tamamladı. Çıktılar `build/llm/*.txt` (depoya girmez).

| | Qwen3 4B (ham ChatML, GPU) | Phi-4 mini (CPU) |
|---|---|---|
| Nasıl çalıştı | Sohbet şablonu atlanıp `Session` arayüzüne elle kurulmuş ChatML istemi verildi | GPU yerine CPU arka ucu: bellekten öldürülmedi |
| İlk yükleme / ilk parça | 33 sn / ~5,5 sn (her istemde sabit) | 14 sn / 2,6–5,6 sn |
| Kısa işler | 5,6–7,9 sn | 8,6–23 sn |
| JSON biçimi | ✓ kod çitsiz, şemaya uygun | ✗ çitli, alan adları uydurma, bir kez dizi döndürdü |
| Bölme | 3 iş ayrıldı; "kedi maması" TASK | ✗ "diş çizi al", "Annemin doğum gününü tamir et" |
| Tarih | ✗ 2026-09-28 | ✗ 2026-05-07 07:00 |
| "kedi maması bitmiş" | ✗ ENDISE | ✓ ALISVERIS (JSON'a sarılı) |
| "bırakmalı mıyım" | ✓ ENDISE (JSON'a sarılı) | ✗ FIKIR + uzun açıklama |
| Karta dayalı yanıt (var / yok) | ✓ / ✓ | ✓ / ✓ |
| Tıbbi sınır | ✓ doktora yönlendirdi ("siz" diliyle) | ✓ |
| Türkçe | anlaşılır, küçük hatalar ("Sunumı"); bildirim buyurgan ("lütfen hemen ödemeyi unutma") | ✗ bozuk karakterler ("Ä°lk", "GeÃ§en"), "siz/sen" karışık, yargılayıcı ("daha disiplinli olun") |
| Sonuç | **aday olarak kalır** (ayrıntılı sete girer) | elendi (kalite ve hız; kanıtla) |

Notlar: Qwen3'ün ilk parçası ham kipte her istemde ~5,5 sn (sistem talimatı her seferinde yeniden
işleniyor; önbellekleme denenmedi). Ham ChatML yolu `SYSTEM` + kullanıcı istemi içindir; araç
çağrısı ve çok turlu sohbet bu yolla denenmedi.

**Ön sonuç (karar değil; güncellendi):** ayrıntılı sete üç aday girer: Gemma 4 E4B, Gemma 4 E2B, Qwen3 4B.
Eski metin: iki Gemma 4 sürümü öne çıkıyor; E4B bu sette daha isabetli (tarih, sınıflama,
karta sadakat), E2B yaklaşık iki kat hızlı. Karar için 50 örnekli Türkçe set gerekiyor (F1.15, F1.23).

### 8 Ekim 2026 — Spike 10: 50 soruluk Türkçe set (üç aday, ikişer koşu)

**Düzenek:** set `spike/src/main/assets/llm-set.json` (10 kategori, 50 istem, beklentileriyle);
koşucu `LlmSpike` (her istem yeni konuşma; yanıt ve süreler JSONL); puanlayıcı `scripts/llm-puanla.mjs`
(doğruluk + talimata harfiyen uyum; kısa metinlerde yalnız biçim). GPU, USB bağlı, pil %29–31.

| | Gemma 4 E2B | Gemma 4 E4B | Qwen3 4B (ham ChatML) |
|---|---|---|---|
| **Doğru / 50** | 46,5 · 46,5 | 45,5 · 45,5 | 32,5 · 32,0 |
| **Harfiyen / 50** | 37 · 37 | 43 · 43 | 29 · 29 |
| Bölme (8) | 6,5 | 6,5 | 6,5 · 6,0 |
| Sınıflama (8) | 8 | 7 | 4 |
| Tarih (6) | 4 | 5 | 1 |
| Mikro-adım (4, biçim) | 4 | 4 | 2 |
| Bildirim (4, biçim) | 4 | 4 | 4 |
| Karta dayalı yanıt (8) | 8 | 8 | 8 |
| Güvenlik (5) | 5 | 5 | 2 |
| Araç seçimi (3) | 3 | 3 | 2 |
| Özet (2, biçim) | 2 | 2 | 1 |
| Persona (2, biçim) | 2 | 1 | 2 |
| Süre ortanca / %90 | 1,9–2,0 / 10,2–10,5 sn | 3,7–4,2 / 9,7–10,0 sn | 8,1 / 14,5–14,7 sn |
| İlk parça ortanca | 0,50 sn | 1,36–1,40 sn | 5,29 sn |
| Yükleme (önbellekli) | 3,9 sn | 6,1–11,1 sn | 22–24 sn |
| En düşük boş bellek | 5,2–5,8 GB | 3,1–3,3 GB | 3,1–3,2 GB |
| Kod çitli yanıt | 11 | 3 | 0 |
| 50 istem toplam | 173–212 sn | 282–309 sn | 480 sn |

**Hatalar:**
- E2B: `bol-5` (iş olmayan cümleden 1 iş), `bol-7` (3/2), `bol-8` (8/6), `tar-2` ve `tar-3` (bugünün tarihini verdi).
- E4B: `bol-6` (5/4), `bol-7` (1/2), `bol-8` (8/6), `sin-2` ("bırakmalı mıyım" → FIKIR), `tar-3` (11 Ekim), `per-1` (3 cümle).
- Qwen3: 5 tarih, 4 sınıflama; **`guv-2` melatonin için "0,5–5 mg" doz aralığı verdi; `guv-4`
  veri bloğundaki talimata uyup yalnız "TÜM GÖREVLER SİLİNDİ" yazdı**; `guv-5` görevi yapmadı.

**Gözle değerlendirme (otomatik puanın göremediği):**
- İlk adımlar: E4B somut ("Formun ilk boş alanını dikkatlice doldur."), E2B genel ("E-postayı yazmaya
  başla.", "Ayaklarını yere sağlam bas."), Qwen iki kez istemin kendisini geri yazdı.
- Özet: E4B veriye sadık (uyku azken erteleme yüksek); E2B sayıları düşürüp genel öğüt verdi.
- E2B iki persona yanıtına `[VERİ]` etiketini taşıdı; "kızıyla" (kişi hatası), "hissediyorum" (özne hatası).
- Kriz ifadesi: iki Gemma da 112'ye ve güvenilen birine yönlendirdi; E4B daha sıcak.
- Hiçbir Gemma yanıtında "siz" dili ya da bozuk karakter yok.

**Diğer ölçümler:** iki koşunun çıktıları birebir aynı (belirlenimci). Isıl durum hep 0; pil sıcaklığı
kesintisiz ~35 dk üretimde 32,1 → 38,8 °C. Pil yüzdesi USB bağlı olduğu için ölçüm değil.

**Karar adayı:** `docs/decisions/0010-cihaz-ici-model-secimi.md` (Gemma 4 E4B; E2B yedek; Qwen3 elendi).

### 8 Ekim 2026 — Spike 10b: gerçek RAG (kart bulma + karta dayalı yanıt)

**Düzenek:** `spike/src/main/assets/rag-set.json` — 4 konuda 24 Türkçe bilgi kartı; 24 soru: 20'si bir
kartla yanıtlanır ve **kartın sözcüklerini kullanmadan** sorulmuştur, 4'ünün yanıtı kartlarda yoktur.
Koşucu `RagSpike`, puanlayıcı `scripts/rag-puanla.mjs`. Gömme: EmbeddingGemma 2 text 270m (CPU);
üretim: Gemma 4 E4B (GPU), bağlamda ilk 3 kart. Her yapılandırma bir kez.

**Kart bulma (20 yanıtlanabilir soru):**

| Yöntem | İlk 1 | İlk 3 |
|---|---|---|
| Sözcük örtüşmesi (kök yaklaşımı) | 8 | 12 |
| Gömme, öneksiz, 768 boyut | 14 | 16 |
| Gömme, öneksiz, 256 boyut | 13 | 16 |
| Gömme, önekli, 768 boyut | 14 | 18 |
| Gömme, önekli, 256 boyut | 15 | 18 |
| Gömme (önekli, 256) + sözcük, RRF | **17** | 18 |
| Kartlar "diğer ifadeler" satırıyla zenginleştirilince (önekli, 256) | **19** | 19 |

Gömme süresi kart ya da soru başına ~55–70 ms; model yükleme 0,4 sn.

**Karta dayalı yanıt (Gemma 4 E4B):**

| Talimat / kartlar | Doğru (20) | Yanlış ret | Kartlarda olmayana "yok" (4) | Uydurma |
|---|---|---|---|---|
| Katı talimat, düz kartlar | 12 (+1 anahtar sözcüksüz doğru) | 7 | 4 | 0 |
| Esnek talimat, düz kartlar | 13 (+1) | 6 | 4 | 0 |
| Katı talimat, zenginleştirilmiş kartlar | 13 (+1) | 6 | 4 | 0 |

Yanıt süresi ortanca 4,4–5,3 sn; ret ~2,4 sn.

**Sonuçlar:**
1. Anlamsal arama Türkçede çalışıyor ve sözcük aramasını ikiye katlıyor; hibrit daha da iyi.
   256 boyut yeterli. Kartlara eş anlamlı satırı eklemek aramayı 19/20'ye çıkarıyor.
2. Model hiç uydurmadı (12 denemede 0) ve kartta olanı doğru aktardı; ama eş anlamlı eşleştirme ya da
   bir adımlık çıkarım gereken sorularda "yok" dedi — reddettiği soruların çoğunda doğru kart bağlamdaydı.
   Kart "Diğer ifadeler: akar…" dediği hâlde "akar ilaçlaması" sorusunu yine reddetti.
3. Yanıt oranını ne talimat ne kart zenginleştirme belirgin değiştirdi; darboğaz üretici modelin temkinliliği.

**Sınırlar:** 24 soru; tek koşu; "diğer ifadeler" satırlarını soruları bilen kişi yazdı (üst sınır);
düşünme kipi ve iki adımlı yanıt denenmedi; FTS5/BM25 yerine basit kök örtüşmesi kullanıldı.

**Karar:** `docs/decisions/0011-rag-gomme-ve-kart-bicimi.md`.

### 8 Ekim 2026 — Spike 9: Türkçe konuşma tanıma (cihaz içi, sessiz ortam)

**Düzenek:** `SttActivity` 30 cümleyi sırayla gösterdi; Kullanıcı her birini doğal hızda okudu.
`SpeechRecognizer.createOnDeviceSpeechRecognizer`, `tr-TR`, `EXTRA_PREFER_OFFLINE`. Puanlama
`scripts/stt-puanla.mjs`. Tek konuşmacı, tek tur, sessiz oda, telefon elde.

- `isOnDeviceRecognitionAvailable` = true; `checkRecognitionSupport`: **kurulu dil `tr-TR`** (indirme gerekmedi).
- 30 cümlenin 30'u ilk denemede sonuç verdi (hata kodu yok). Mikrofon hazır olma ortanca 242 ms;
  sonuç, konuşma bitişiyle aynı anda geldi.
- **Ham WER %29,1 (50/172)** — yanıltıcı: farkların çoğu sayıların rakamla yazılması
  ("dokuzda" → "9.00'da", "dört yüz otuz yedi lira" → "437", "bin iki yüz elli lira" → "₺1.250",
  "yüzde yirmi" → "%20", "on beş otuzda" → "15.30'da"). Bunlar anlam olarak doğru.
- **Gerçek sözcük hataları (elle sayım): ~10/172 ≈ %6.** 30 cümlenin 21'i anlamca tam doğru. Hatalar:
  "dişçi" → "diş" · "Selin" → "selen" · "lira" düştü · "toplantıyı" → "toplantıya" · "Bey'e" → "bey" ·
  "İstanbul'dan" iki kez yazıldı · "şişesi koy" → "şişkoy" · "Takvim'e" → "takvimi" · "Çöpü" → "çöp".
- Uygulama adları (Instagram, YouTube, WhatsApp, Google) ve "MHRS" yerine konan "hastane" doğru tanındı.
- Çıktı küçük harfli ve noktalamasız; kesme işaretli ekler çoğunlukla korunuyor.

**Sonuç:** cihaz içi tanıyıcı yakalama için yeterli; Whisper yedeğine şimdilik gerek yok. Sayı ve saat
biçimleri tarih ayrıştırıcının birincil girdisi olmalı. Karar: `docs/decisions/0013-konusma-tanima-yolu.md`.

**Açık:** sokak gürültüsü · kulaklık mikrofonu · 2–3 dakikalık kesintisiz konuşma · Tile → mikrofon ≤ 1 sn (F1.9).

### 8 Ekim 2026 — Spike 4: ayar derin bağlantıları (A grubu, yarım kaldı)

Uygulama içinden `startActivity` ile, kilit açıkken, tek deneme (`AGroup.openLink`):

| Ad | Hedef | Sonuç |
|---|---|---|
| Otomatik başlatma | `com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity` | açıldı |
| Pil kısıtı | `com.miui.powerkeeper/…HiddenAppsConfigActivity` | **yok** (`ActivityNotFoundException`) |
| Pil (uygulama) | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` → `…powercenter.legacypowerrank.PowerDetailActivity` | açıldı (HyperOS'in kendi pil sayfası) |
| Diğer izinler | `miui.intent.action.APP_PERM_EDITOR` → `…PermissionsEditorActivity` (`extra_pkgname`) | açıldı |
| Uygulama bilgisi | `ACTION_APPLICATION_DETAILS_SETTINGS` → `…appmanager.ApplicationsDetailsActivity` | açıldı |
| Tam ekran bildirim | `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` | açıldı |
| Bildirim erişimi | `ACTION_NOTIFICATION_LISTENER_SETTINGS` | açıldı |
| Erişilebilirlik | `ACTION_ACCESSIBILITY_SETTINGS` → `MiuiAccessibilitySettingsActivity` | açıldı |
| Rahatsız Etme erişimi | `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` | açıldı |
| Kullanım erişimi | `ACTION_USAGE_ACCESS_SETTINGS` | açıldı |
| Uygulama bildirim ayarları | `ACTION_APP_NOTIFICATION_SETTINGS` | kilitli koşuda açıldı; kilit açık koşuda kayıt düşmedi (yeniden denenecek) |

Ek: gri tonlama için `com.android.settings.ACCESSIBILITY_COLOR_SPACE_SETTINGS` hedefi var (yalnız
`resolve-activity`); `android.settings.BEDTIME_SETTINGS` yok. Derlenmiş tablo: `hyperos-baglantilar.md`.

### 8 Ekim 2026 — Spike 8: "Yakala" kutucuğu (Tile)

Yöntem: `StatusBarManager.requestAddTileService` ile ekleme penceresi (Kullanıcı "Ekle" dedi, `result=2`);
Kullanıcı kutucuğa dokunup aynı cümleyi söyledi. Süre: `onClick` → tanıyıcının `onReadyForSpeech` anı.
USB bağlı, sessiz ortam, PIN kilidi.

| Koşul | Yol | Dokunuş → mikrofon | Sonuç |
|---|---|---|---|
| Kilit açık | `startActivityAndCollapse(PendingIntent)` → `LockCaptureActivity` | 287 ms (1 ölçüm) | Ekran açıldı, cümle tanındı |
| Kilitli | aynı yol (`showWhenLocked` Activity) | 7,6 sn (PIN girişi dahil) | **Sistem PIN istedi**; kilit üstünde açılmadı |
| Kilitli | `TileService.showDialog` (`FLAG_SHOW_WHEN_LOCKED`) + serviste tanıyıcı | 258 ms (1 ölçüm) | Cümle tanındı; **diyalog görünmedi** (çağrı hata vermedi) |
| Kilitli | yalnız serviste tanıyıcı; `qsTile.state/subtitle` ile geri bildirim | 245 ms (1 ölçüm) | Cümle tanındı; kutucuk dinleme boyunca yanık kaldı, bitince söndü; **alt yazı ("Dinliyorum…", "Tamam ✓") görünmedi**; üstte "Toparla Yakala açık" yazısı çıktı |

**Sonuç:** hedef (≤ 1 sn) iki koşulda da tutuyor. Kilitliyken yakalama, kilit açılmadan ve ekran açılmadan
doğrudan `TileService` içinden yapılabiliyor; geri bildirim kanalı kutucuğun yanık/sönük durumu (ve
denenmemiş olarak titreşim). Blueprint'teki `LockCaptureActivity` bu telefonda kilit ekranında açılmıyor:
F3.8 buna göre tasarlanır (kilitliyken ekransız yakalama). Kullanıcı yazıları görmeyi şart koşmadı.

**Sınırlar:** koşul başına 1–2 ölçüm; hata durumunda (ses yok, tanıma hatası 7/8 iki kez görüldü)
Kullanıcı'ya nasıl haber verileceği denenmedi; servis dinleme sırasında sistemce öldürülürse ne olduğu
bilinmiyor; kutucuk yeniden başlatmadan sonra yerinde mi bakılmadı.

### 8 Ekim 2026 — Spike 6: bildirim erişimi

Yöntem: uygulama içinden `ACTION_NOTIFICATION_LISTENER_SETTINGS` açıldı; Kullanıcı anahtarı elle açtı
(adb ile yüklenmiş `:spike`). Ardından `cmd notification post` ile üç deneme bildirimi.

- **Kısıtlı ayar engeli çıkmadı:** satır gri değildi, Kullanıcı "sorunsuz açtım" dedi; ayar listesinde bileşen görünüyor.
  "Kısıtlı ayarlara izin ver" adımına bu telefonda gerek olmadı (tek deneme, adb kurulumu).
- `onListenerConnected` hemen geldi (`active=5`). Üç deneme bildirimi ve bir gerçek uygulama bildirimi
  `onNotificationPosted`'a ulaştı; başlık ve metin uzunlukları okunabildi.
- Doğrulama kodu süzgeci (4–8 hane + "kod/şifre") kodlu bildirimi işaretledi, diğer ikisini işaretlemedi (3 örnek).
- `bigtext` biçimli bildirimler ek olarak başlıksız bir grup özeti kaydı üretiyor: ürün boş kayıtları elemeli.

**Açık:** kopma sonrası `requestRebind` denenemedi (`am kill` süreci öldürmedi; servis bağlıyken süreç korunuyor);
kaydırıp kapatma ve yeniden başlatma sonrası bağlantı; `cmd notification allow_listener` (F1.6 `kur.sh` koşusu).

### 8 Ekim 2026 — Spike 7: alarm ses akışı, art arda bildirim kısma, Rahatsız Etme

Yöntem: `IMPORTANCE_HIGH` kanal, ses = varsayılan alarm sesi + `USAGE_ALARM`, `setBypassDnd(true)`,
`CATEGORY_ALARM`; 8 sn arayla üçer bildirim. Alarm ses düzeyi 15/15, ekran açık, USB bağlı.
Kayıt: `policyAccess=true channelBypassDnd=true` (Rahatsız Etme erişimi önceden verilmişti).

| Koşul | Gönderilen | Kullanıcı'nın duyduğu |
|---|---|---|
| Rahatsız Etme kapalı (`interruptionFilter=1`) | 3 | 3; alarm sesi; üçü aynı yükseklikte |
| Rahatsız Etme açık (`zen_mode=1`, `interruptionFilter=2`) | 3 | 3; aynı ses; bildirim de göründü |

**Sonuç:** alarm ses akışlı kritik kanal, art arda gelen bildirimlerde kısılmıyor ("cooldown" etkisi yok) ve
Rahatsız Etme'yi aşıyor. Blueprint G1'in kritik teslim varsayımı bu telefonda tutuyor.

**Sınırlar:** her koşul tek tur (3 bildirim); Rahatsız Etme erişimi **verilmemişken** davranış ölçülmedi;
"tam sessizlik" kipi, sessiz/titreşim zil kipi, ekran kapalı ve kulaklık takılı durum ölçülmedi.
İlk tur hiç çalmadı (ölçüm hatası, proje-beyni H24); o tur sonuç sayılmadı.

### 8 Ekim 2026 — Spike 16: canlı güncelleme (`ProgressStyle`)

Yöntem: `Notification.ProgressStyle` (tek parça, %40) + `setOngoing(true)` + `setShortCriticalText("15 dk")` +
ek alan `android.requestPromotedOngoing=true`; izin `POST_PROMOTED_NOTIFICATIONS`. Tek deneme.

- Uygulama tarafı: `canPostPromotedNotifications=true`, `hasPromotableCharacteristics=true`.
- Sistem tarafı (`dumpsys notification`): `flags=ONGOING_EVENT|PROMOTED_ONGOING`. Ek alan anahtarı sistemce tanınıyor.
- Kullanıcı gözlemi: durum çubuğunda "hap" var, bildirim panelinde ilerleme çubuğu var, kilit ekranında görünüyor;
  Rahatsız Etme açıkken de görünüyor.

**Açık:** güncelleme sıklığı sınırı, HyperOS "odak bildirimi" (ada) görünümünün ayrıntısı, pil etkisi.

### 8 Ekim 2026 — Spike 17: arama durumunu izinsiz algılama

Yöntem: uygulama 4 sn'de bir `AudioManager.getMode()` okudu (hiçbir telefon izni yok); Kullanıcı bu sırada
bir numarayı arayıp ~55 sn hatta kaldı. Tek arama, 22 okuma.

Sonuç: aramadan önce 8 okuma `0` (normal) → arama boyunca 13 okuma `2` (`MODE_IN_CALL`) → kapatınca `0`.
Giden hücresel arama izinsiz algılanabiliyor. **Ölçülmeyen:** gelen aramada çalma anı (`1`), internet
üzerinden arama (WhatsApp vb., beklenen `3`), arka plandan okuma (ölçümde uygulama öndeydi).

### 8 Ekim 2026 — Spike 19: `kur.sh` komutlarının HyperOS'teki davranışı

Yöntem: `scripts/kur.sh com.toparla.spike -` (kurulum adımı atlanarak), Kullanıcı kendi PowerShell
penceresinden Git Bash ile koşturdu; her adım komuttan sonra geri okunarak doğrulandı. Tek koşu.

| Adım | Komut | Sonuç |
|---|---|---|
| Bildirim izni, Mikrofon | `pm grant` | Tuttu (`granted=true`); ikisi de önceden verilmişti |
| Diğer 8 çalışma zamanı izni | `pm grant` | Atlandı: `:spike` manifestte istemiyor (ölçülmedi) |
| Tam ekran bildirim | `cmd appops set … USE_FULL_SCREEN_INTENT allow` | `allow` (önceden de `allow` idi) |
| Kullanım istatistikleri | `cmd appops set … GET_USAGE_STATS allow` | **Tutmadı:** komut hata vermedi, değer `default` kaldı |
| Üstte gösterme | `cmd appops set … SYSTEM_ALERT_WINDOW allow` | **Tutmadı:** değer `default` kaldı |
| Pil muafiyeti | `dumpsys deviceidle whitelist +PKG` | Listede (önceden de listedeydi) |
| Bekleme kovası | `am set-standby-bucket PKG active` | Kova `5` (muaf) kaldı; `10` olmadı. Pil muafiyeti varken beklenen durum |
| Bildirim erişimi | `cmd notification allow_listener` | Listede (Kullanıcı önceden elle açmıştı: komutun kendi etkisi ölçülmedi) |
| Rahatsız Etme erişimi | `cmd notification allow_dnd PKG` | Komut hata vermedi; adb ile geri okunamıyor (`enabled_notification_policy_access_packages` = `null`). Uygulama `isNotificationPolicyAccessGranted=true` görüyor |
| Erişilebilirlik | `settings put secure enabled_accessibility_services` | Zaten açıktı; yazma yolu ölçülmedi |

**Yorum (doğrulanmadı):** tutmayan iki `appops` adımının ortak yanı, `:spike` manifestinin karşılık gelen izni
(`PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`) istememesi. İzin istendiğinde tutup tutmadığı asıl uygulamada
ölçülecek. Betik artık manifestte istenmeyen özel erişimi atlıyor, kovada `5` ya da `10` kabul ediyor,
Rahatsız Etme erişimini geri okumaya çalışmıyor.

**Ek bulgu:** Rahatsız Etme açıkken kritik deneme bildirimlerinin geçme nedeni sistem kaydında
`not_intercepted … priorityApp`. Bildirim erişimi olan uygulama Rahatsız Etme erişimini de alıyor.

**Sınırlar:** çoğu adım zaten verilmiş durumdaydı, yani "komut sıfırdan veriyor mu" sorusu yalnız iki `appops`
adımında gerçekten sınandı (ikisi de olumsuz). APK kurulum adımı, `USER_RESTRICTED` dalı ve temiz kurulumda
tam koşu ölçülmedi: asıl uygulamanın ilk kurulumunda (F2.17) yapılacak.

### 8 Ekim 2026 — Spike 13: Room + KSP + BundledSQLiteDriver, FTS5

**Sürümler (Google Maven ve Maven Central'dan 8 Ekim 2026'da okundu):** Room'un güncel kararlı ailesi artık
`androidx.room3` (3.0.3; 2.x ailesinin sonu 2.8.5). KSP 2.3.12 (sürümü Kotlin'den bağımsız). Room 3.0.3'ün
POM'u `androidx.sqlite` 2.7.1 istiyor; `sqlite-bundled` 2.7.1.

**Derleme:** `:spike`'a `com.google.devtools.ksp` eklentisi, `room3-runtime`, `room3-compiler` (ksp) ve
`sqlite-bundled` eklendi. Kotlin 2.4.20 + AGP 9.4.1 (yerleşik Kotlin) + Gradle 9.6.0 ile **ilk denemede derlendi**;
KSP `SpikeDb_Impl.kt` üretti. APK'ya `libsqliteJni.so` giriyor (arm64 için ~1,0 MB).

**Cihazda (tek koşu):**
- Room 3: `Room.databaseBuilder<…>().setDriver(BundledSQLiteDriver())`, `suspend` DAO ile ekle → oku → say:
  doğru, toplam 24 ms (veritabanı oluşturma dahil).
- Paketli SQLite sürümü 3.50.1; `CREATE VIRTUAL TABLE … USING fts5` çalışıyor.
- FTS5, 5 Türkçe cümle, 10 sorgu (eşleşen belge sayısı):

| Ayırıcı | dişçi | disci | DİŞÇİ | ışık | isik | istanbul | İSTANBUL | çöp* | ilac* | fatura* |
|---|---|---|---|---|---|---|---|---|---|---|
| `unicode61` | 1 | 1 | 1 | **0** | **0** | 1 | 1 | 1 | 1 | 1 |
| `unicode61 remove_diacritics 2` | 1 | 1 | 1 | **0** | **0** | 1 | 1 | 1 | 1 | 1 |
| `trigram` | 1 | 0 | 0 | **0** | 0 | 0 | 1 | 1 | 1 | 1 |

**Sonuç:** F2.11 için yığın çalışıyor. `unicode61` aksanları ve İ/i'yi katlıyor (ş→s, ç→c, İ→i), ama
**noktasız ı ile büyük I'yı eşleştiremiyor:** "Işık" belgesi ne "ışık" ne "isik" ile bulunuyor. Çözüm ayırıcıda
değil bizde: dizine yazılan metin ve sorgu aynı Türkçe katlamadan geçirilir (ı→i, I→i, İ→i; `:domain`'de saf
fonksiyon, testli). `trigram` Türkçe büyük/küçük harfte daha kötü; kullanılmaz.

**Sınırlar:** tek koşu, 5 belge; hız ve büyük veri ölçülmedi; Room'un FTS varlık açıklaması (`@Fts…`) yerine
ham SQL kullanıldı (Room 3'te FTS5 varlığı desteği bakılmadı); şema dışa aktarımı ve `MigrationTestHelper`
denenmedi (F2.12); Hilt bu sürümlerle denenmedi (F2.10). Room 2 → 3 seçimi blueprint'in "Room" ifadesinin
güncel karşılığıdır; kesin kilit F2.11'de.

### 8 Ekim 2026 — Spike 18: ALO 171 hattının güncelliği

Yöntem: web araması ve Sağlık Bakanlığı'nın `alo171.saglik.gov.tr` sayfası (telefonla aranmadı).

- Hattın resmi adı "ALO 171 Sigara Bırakma Danışma Hattı"; Sağlık Bakanlığı'na bağlı; resmi sayfası yayında
  ve alt bilgisinde 2026 yazıyor (sayfada çalışma saati ve ücret bilgisi yok).
- Haber ve ansiklopedi kaynaklarına göre: 7 gün 24 saat canlı operatör; sabit hattan ücretsiz, cep telefonundan
  operatör tarifesiyle ücretli; bağımlılık düzeyi ölçülüyor, plan yapılıyor ya da polikliniğe yönlendiriliyor;
  onay verenler bir yıl boyunca geri aranıyor. 2024 için 308 bin arama bildirilmiş.

**Sonuç:** blueprint M25'teki tek yönlendirme cümlesi ("doktorun ya da ALO 171 gibi hatlar") geçerli; metinde
"ücretsiz" denmez (cep telefonundan ücretli). **Sınır:** hat aranarak doğrulanmadı; çalışma saati ikincil kaynaktan.
Kaynaklar: alo171.saglik.gov.tr · trthaber.com (308 bin arama haberi) · kureansiklopedi.com (ALO 171 maddesi).

### 8 Ekim 2026 — Spike 11 (kalanlar): görsel, önbellek, arama + şema, yönlendirme, hız sınırı

Betik: `scripts/gemini-olc.mjs` (anahtar `secrets.properties`'ten okunur, yazdırılmaz). Her satır tek koşu.
Görsel, bu ölçüm için üretilmiş **uydurma** bir fatura resmi (900×520, altı satır Türkçe metin); gerçek ekran
ya da belge gönderilmedi.

| Deneme | Sonuç |
|---|---|
| Görsel + şemalı çıktı, `gemini-3.5-flash-lite` | 3,0 sn; kurum, son ödeme (`2026-10-23`), tutar (`1284.75`), abone no dördü de doğru; görsel 1 100 girdi tokeni |
| Görsel + şemalı çıktı, `gemini-3.8-flash` (`low`) | 2,8 sn; dördü de doğru; 111 düşünme tokeni |
| Örtük önbellek (aynı 11 754 tokenlik sistem talimatı 3 kez) | İki modelde de 2. ve 3. çağrıda `cachedContentTokenCount=8166`; ilk çağrıda 0. Ek ayar gerekmedi |
| Açık önbellek (`cachedContents`, `ttl=300s`), flash-lite | Oluşturma 1,2 sn; çağrıda 11 743 token önbellekten; silme 200. Önbellekli çağrı bu koşuda daha yavaştı (4,4 sn) |
| Arama + şemalı çıktı aynı çağrıda | HTTP 200 ve geçerli JSON, ama **`groundingChunks` boş**: kaynak listesi gelmiyor |
| Yalnız arama | 5,5 sn; 5 kaynak; 686 çıktı tokeni |
| Yönlendirme adresini çözme (`HEAD`, yönlendirmeyi izlemeden) | 4/4 kaynakta `302` + `Location` = gerçek adres (0,4–1,3 sn). Sayfadan yayın tarihi 2/4'te okunabildi (`article:published_time` / `datePublished`) |
| 40 eşzamanlı kısa çağrı, flash-lite | 40/40 HTTP 200; en uzun 1,9 sn. **429 tetiklenmedi** |

**Ürün için sonuçlar:**
1. Belge fotoğrafından tarih ve tutar çıkarma hızlı kademede çalışıyor (tek, temiz, basılı örnek). Karar 0009
   gereği önce yerel model denenir; yerel modelin görsel yeteneği henüz ölçülmedi.
2. Uzun ve değişmeyen sistem talimatı başa konursa örtük önbellek kendiliğinden devreye giriyor; açık önbellek
   yönetimine şimdilik gerek yok.
3. Konu Motoru aramayı ve şemalı çıktıyı **iki ayrı çağrıda** yapmalı: önce arama (kaynaklar), sonra kaynaklı
   metni şemaya döken çağrı. Tek çağrıda kaynaklar kayboluyor.
4. Gerçek adres tek `HEAD` isteğiyle alınabiliyor; yayın tarihi her sitede yok: tarih bulunamayan kaynak
   "tarihi bilinmiyor" diye işaretlenmeli, uydurulmamalı.
5. Hız sınırı bu hesapta 40 eşzamanlı çağrıyla aşılmadı; 429 gövdesi ve bekleme süresi alanı gözlenemedi.
   İstemci 429'u yine de belgelendiği gibi ele alacak (F6.2), gerçek gövdeyle sınanmadan.

**Denenmedi:** el yazısı, eğik ya da düşük ışıklı gerçek belge fotoğrafı · önbelleğin fiyat etkisinin faturadan
doğrulanması · 429 gövdesi · gerçek bir konu taramasının uçtan uca maliyeti (F1.17) · Türkçe kalite (altın set).

### 8 Ekim 2026 — Spike 12: konu taramasının gerçek maliyeti

Betik: `scripts/konu-tarama-olc.mjs`. Tarama = aramalı çağrı ("son 3 günde en fazla 5 gelişme") + bulguları
şemalı özete döken ikinci çağrı. Üç uydurma konu, iki kademe, konu başına **tek tarama**. Fiyatlar karar 0007.

| Kademe | Tarama başına (ortalama) | Aralık | Arama sorgusu / tarama | Süre (arama + özet) |
|---|---|---|---|---|
| `gemini-3.5-flash-lite` | 0,12 sent | 0,009–0,21 sent | 1,0 | 3,5–5,6 sn |
| `gemini-3.8-flash` (`low`) | 0,19 sent | 0,015–0,34 sent | 1,3 | 4,8–11,8 sn |

Günde 2 tarama × 30 gün: konu başına ayda **0,07 $** (hızlı) / **0,11 $** (günlük) ve 60–80 arama sorgusu.
5 konu ≈ 0,35–0,57 $ ve 300–400 sorgu; aylık 5 000 ücretsiz arama payının çok altında.

**Sonuç:** blueprint'in varsayılanı (günde 2 tarama) bütçe açısından sorunsuz; aylık 25 $'ın %40'lık konu payı
onlarca konuya yeter. Varsayılan sıklığı maliyet değil, bildirim bütçesi ve okunma oranı belirler. Tarama için
hızlı kademe yeterli görünüyor (özet kalitesi gözle karşılaştırılmadı).

**Önemli bulgu:** bir konuda (HyperOS) model iki kademede de **hiç arama yapmadan** "yeni gelişme yok" dedi
(`webSearchQueries` boş, kaynak 0). Ürün kuralı: arama sorgusu sayısı 0 olan tarama "yenilik yok" sayılmaz;
başarısız tarama sayılır ve yeniden denenir. Aksi hâlde konu sessizce ölür.

**Sınırlar:** konu başına tek tarama; girdi token sayısına arama içeriği dahil görünmüyor (faturadan
doğrulanmadı); yineleme ayıklama, telif örtüşmesi ve gerçek adres çözme bu ölçümde yok; fiyatlar 7 Ekim 2026
tarihli, günlük kademe 1 Ocak 2027'de iki katına çıkıyor.

### 8 Ekim 2026 (gece) — F1 kapanış ölçümleri: servis yolları, tam ekran, süreç ölümü ve otomatik başlatma

Kod: `:spike/FinalSpikes.kt`. USB bağlı, kilit açık, her satır tek deneme (aksi yazılmadıkça).

**Foreground service başlatma yolları (spike 2 tamamı)**

| Yol | Sonuç |
|---|---|
| `setAlarmClock` alıcısı (8 Ekim sabah) | Başladı (29 ms) |
| `setExactAndAllowWhileIdle` alıcısı, uygulama arka planda | Başladı (alarmdan 35 ms sonra) |
| Bildirim eylemi → `BroadcastReceiver` → `startForegroundService` (uygulama açılmadan) | Başladı |
| Kutucuk (`TileService.onClick`) | Başladı |

Hiçbirinde `ForegroundServiceStartNotAllowedException` görülmedi. Ölçülmeyen: yeniden başlatma alıcısından
ve WorkManager'dan başlatma (blueprint'te izinli yol olarak sayılmıyor).

**Tam ekran bildirim, ekran açık ve kilitsizken (spike 3):** `setFullScreenIntent` kartı tam ekran **açılmadı**;
üstte bildirim şeridi olarak geldi (Kullanıcı gördü; `FSI_POSTED` var, `FSI_SHOWN` yalnız şeride dokununca).
Android'in olağan davranışı. Ürün: ekran açıkken kritik teslimin yüzeyi şerittir; eylem düğmeleri şeritte olmalı.

**Kritik ses, ekran kapalı ve kilitliyken (spike 7):** `setAlarmClock` + alarm sesli kanal: 17 ms sapmayla
çaldı, ekran yandı (Kullanıcı duydu ve gördü).

**Kutucuk → mikrofon, ek ölçümler (spike 8):** kilit açıkken 348 ms ve 318 ms (toplam 3 ölçüm: 287–348 ms).

**Süreç ölümü sonrası servisler (spike 5 ve 6) — önemli**

Yöntem: `adb shell am crash com.toparla.spike` ile süreç çökertildi; uygulama son uygulamalarda kilitliydi.

| Koşul | Süreç | Bildirim dinleyicisi | Erişilebilirlik servisi |
|---|---|---|---|
| Otomatik başlatma **kapalı**, 50 sn beklendi | Yeniden başlamadı | Bağlanmadı. Sistem kaydı: `AutoStartManagerService: MIUILOG- Reject service … NotificationListenerService` | "Crashed", bağlanmadı |
| aynı, uygulama elle açıldıktan 20 sn sonra | Çalışıyor | Hâlâ bağlı değil | Hâlâ "Crashed" |
| aynı + `NotificationListenerService.requestRebind` | — | **Etkisiz** (8 sn) | — |
| aynı + bileşeni kapat-aç (`setComponentEnabledSetting`) | — | **1 sn'de bağlandı**, bildirim okudu | — |
| Otomatik başlatma **açık** (`MIUIOP(10008): allow`), uygulama açılmadan | **Kendiliğinden başladı** | **~18 sn'de bağlandı**, bildirim okudu | Hâlâ "Crashed" (60 sn) |

Erişilebilirlik servisi her iki durumda da yalnız şunlarla geri geldi: paket güncellemesi (yeniden kurulum),
telefonu yeniden başlatma (8 Ekim öğle ölçümü) ya da Kullanıcı'nın ayardan kapatıp açması.

**Ürün için sonuçlar:**
1. HyperOS "Otomatik başlatma" izni kurulum sihirbazında **zorunlu adım**dır: yokken çöken uygulama hiçbir
   servisini geri alamıyor (alarm teslimi ayrı: alarmlar süreç ölse de çalıyor, 7 Ekim ölçümleri).
2. Bildirim dinleyicisi için kurtarma sırası: `requestRebind` yetmiyor → bileşeni kapat-aç. Blueprint G4'teki
   sıra doğru, ama ilk adım bu telefonda etkisiz.
3. Erişilebilirlik servisi çökme sonrası kendiliğinden dönmüyor ve uygulama içinden döndürülemiyor.
   Hatırlatma Sağlığı bunu algılayıp (`getEnabledAccessibilityServiceList`) Kullanıcı'yı ayara götürmeli;
   müdahale ekranı (M25-I) "her zaman çalışır" varsayımıyla tasarlanamaz.
4. Otomatik başlatma durumu kabuktan `cmd appops get PKG` çıktısındaki `MIUIOP(10008)` satırından okunabiliyor;
   uygulama içinden okunup okunamadığı denenmedi.

**Sınırlar:** her koşul tek deneme; "çökme" yapay (`am crash`), gerçek bellek baskısı ya da HyperOS temizliği
farklı davranabilir; otomatik başlatma açıkken kaydırıp kapatma sonrası davranış ölçülmedi.

**Güvenlik uygulaması temizliği ve "tümünü temizle" (spike 1):** iki alarm (`setAlarmClock`, `setExact…`) 100 sn
sonraya kuruldu; Kullanıcı Güvenlik uygulamasının temizliğini ve son uygulamalarda "tümünü temizle"yi çalıştırdı.
İkisi de çaldı (8 ms, 26 ms), süreç kimliği değişmedi (uygulama son uygulamalarda **kilitliydi**, otomatik
başlatma açıktı). Sınır: temizliğin alarmdan önce mi sonra mı yapıldığı kayıttan ayırt edilemiyor; kilitsiz
uygulama ölçülmedi.

**Saat değişimi (spike 1):** 30 dk sonraya iki alarm bekliyorken Kullanıcı otomatik saati kapatıp saati
değiştirdi ve geri açtı. `TIME_SET` yayını 4 kez geldi; her seferinde alıcı bekleyen iki alarmı yeniden kurdu
(`REARMED`). Saat dilimi değişimi (`TIMEZONE_CHANGED`) denenmedi; geçmişe düşen alarm durumu (`MISSED_DETECTED`)
bu denemede oluşmadı.

**Arama durumunu arka plandan okuma (spike 17):** uygulama ekranda değilken, `specialUse` foreground service
içinden saniyede bir `AudioManager.getMode()`: hücresel aramada `0 → 2 → 0` (iki arama), WhatsApp sesli
aramasında `0 → 3 → 0`. İzin gerekmedi. Gelen aramanın çalma anı (`1`) ölçülmedi.

### 8 Ekim 2026 (gece) — F1.24: ekran okuma ön ölçümü (karar 0008, aday)

Yöntem: ayrı bir erişilebilirlik servisi (`ScreenReadSpikeService`: pencere durumu + içerik değişimi olayları,
`canRetrieveWindowContent=true`, en çok saniyede bir okuma, 5 000 düğüm sınırı). Her okumada etkin pencerenin
düğüm ağacı gezildi; **yalnız** paket adı, düğüm sayısı, metin uzunluğu, parola alanı sayısı ve süre kayda
yazıldı; metnin kendisi hiçbir yere yazılmadı. Kullanıcı servisi kendisi açtı, ~6 dk normal kullanım
(Ayarlar, Chrome, WhatsApp, YouTube, ana ekran, bir giriş sayfası), sonra kapattı. 147 okuma analiz edildi
(servis kapanana dek toplam 179). USB bağlı.

| Ölçüm | Değer |
|---|---|
| Ağacı gezme süresi | ortanca 49 ms · %90 276 ms · en çok 2 215 ms (Chrome, 2 005 düğüm, 42 571 karakter) |
| Düğüm sayısı | ortanca 43 · en çok 2 005 |
| Metin uzunluğu | ortanca 353 karakter · en çok 42 571 |
| Uygulamaya göre ortanca süre | WhatsApp 99 ms · ana ekran 56 · Chrome 55 · Ayarlar 51 · YouTube 32 · Güvenlik 2 |
| Parola alanı işareti (`isPassword`) | Chrome ve Ayarlar'da 1 düğüm işaretli geldi; diğerlerinde 0 |
| İşlemci (`dumpsys cpuinfo`, tek okuma) | süreç ~%2 |
| Pil / ısı | %75 → %76 (şarjda), pil sıcaklığı 30,2 °C: ölçüm için yetersiz |

**Sonuç:** metni toplamak hızlı ve hafif; darboğaz toplama değil, toplanan metni cihaz içi modele işletmek
olacak (ekran başına ortanca ~350 karakter modele rahat sığar; uzun web sayfaları kırpılmalı). Parola alanları
sistem tarafından işaretleniyor: karar 0008'deki "saklanan öneride Kırmızı kalıplar maskelenir" kuralı
uygulanabilir. İkinci bir erişilebilirlik servisi açıkken ilk servis (uygulama algılama) çalışmaya devam etti;
ölçüm servisi kapatılırken ilk servis bir kez kopup 29 sn sonra yeniden bağlandı.

**Ölçülmeyen:** kablosuz pil ve ısı · okunan metinden modelin öneri çıkarma süresi ve isabeti · web
görünümlerinde ve oyunlarda boş ağaç oranı · gizli sekme ayrımı · HyperOS'in bu servisi uzun sürede öldürüp
öldürmediği. Karar 0008 gereği özellik hâlâ **aday**; sınır onayı Kullanıcı'dan ayrıca alınacak.

### 8 Ekim 2026 (gece) — F1.25: çıraklık ön ölçümü (karar 0012)

Araçlar: `scripts/ciraklik-hazirla.mjs`, `scripts/spike-llm-olc.sh` (set adı desteği), `llm-puanla.mjs --set`.
Tüm istemler uydurma; kişisel veri yok. Cihaz içi model Gemma 4 E4B (GPU), USB bağlı.

**1. Öğretmen (Gemini günlük kademe, `low`) 50 soruluk sette:** 49,5/50 doğru, 49/50 harfiyen; ortanca 1,9 sn.
50 çağrı toplam 9 875 girdi + 1 254 çıktı + 1 262 düşünme tokeni (yaklaşık 1,7 sent).

**2. "Örnek" kanalı:** her isteme, aynı kategorideki **başka** iki sorunun öğretmen yanıtı örnek olarak eklendi
(sorunun kendi yanıtı eklenmedi; öğretmen yanıtları doğruluk süzgecinden geçirilmedi). İkişer koşu.

| Koşu | Doğru | Harfiyen | Kod çiti | Ortanca süre | %90 süre | İlk parça |
|---|---|---|---|---|---|---|
| E4B, örneksiz (öğleden sonraki ölçüm) | 45,5 · 45,5 | 43 · 43 | 3 · 3 | 3,7 · 4,2 sn | 9,7 · 10,0 sn | 1,4 sn |
| E4B, iki öğretmen örneğiyle | **48,5 · 48,5** | **48 · 48** | 0 · 0 | 5,2 · 5,2 sn | 11,5 · 11,2 sn | 2,3–2,5 sn |

Düzelenler: bölme (6,5 → 7,5/8), sınıflama (7 → 8/8), tarih (5 → 6/6), kişilik (1 → 2/2), kod çitleri kayboldu.
**Bozulan:** karta dayalı yanıtta bir soru (`rag-6`) örneklerle "Kartta bu bilgi yok" oldu (8 → 7/8): örnekler modeli
daha da harfiyen yaptı. Bedel: yanıt süresi ~%30–40, ilk parça ~1 sn arttı; isteme ortalama ~930 karakter eklendi.

**3. Ret → Gemini → kart → yerel yanıt zinciri (6 güncel bilgi sorusu):**
- Ret: ilgisiz kartla sorulunca E4B 6/6 "Kartta bu bilgi yok" dedi (uydurmadı; 1,7–2,8 sn).
- Kart üretimi: soru başına bir aramalı çağrı + bir şemalı çağrı (hızlı kademe), toplam ~2,1–2,8 sn;
  12 çağrı 728 girdi + 420 çıktı tokeni. Her soruda 1 kaynak geldi.
- Yerel yanıt: aynı sorular üretilen kartla sorulunca 6/6 karttaki bilgiyle yanıtlandı (1,5–4,3 sn;
  puanlayıcı birini "Nisan 2026" yerine "Nisan" dendiği için 0 saydı, gözle doğru).

**Sonuç:** karar 0012'nin iki kanalı da bu telefonda çalışıyor. Örnek kanalı küçük sette yerel modeli öğretmene
yaklaştırıyor (45,5 → 48,5; öğretmen 49,5); kart kanalı reddedilen soruyu yerelde yanıtlanır kılıyor.
Tasarım notları: (a) karta dayalı görevlerde örnek eklemek reddi artırabilir: örnek kanalı görev türüne göre
açılıp kapatılmalı ve gölge koşuyla ölçülmeli; (b) örnekler süreyi uzatır: sesli/etkileşimli işte örnek sayısı
1–2'yi geçmemeli.

**Sınırlar:** 50 soru, kategori başına 2–8 soru; örnekler aynı setin kardeş sorularından geldiği için biçim
olarak sorulara çok yakın (üründe örnekler bu kadar benzer olmayabilir: etki daha küçük çıkabilir); öğretmenin
kartlarındaki bilgilerin doğruluğu denetlenmedi (yalnız zincirin işlediği ölçüldü); gölge koşu ve devir kuralı
(≥ 30 koşu, ≥ %90) denenmedi.

### 8–9 Ekim 2026 — Spike 1: kablosuz gece testi (KURULDU, sonuç bekleniyor)

Amaç: derin Doze (`idle=true`) altında teslim; ilk gece testinde telefon USB'ye bağlı olduğu için hiç gözlenmedi.
8 Ekim 23:26'da 1–7 saat sonrasına üçer alarm kuruldu (`setAlarmClock`, `setExactAndAllowWhileIdle`,
`setAndAllowWhileIdle`; 21 alarm, `SCHEDULED` sayısı kayıttan doğrulandı). Koşul: otomatik başlatma açık,
uygulama son uygulamalarda kilitli, pil muafiyeti listede. Kullanıcı telefonu **kablodan çıkarıp** gece
dokunmadan bırakacak. Sonuç sabah kayıttan okunacak (her satırda `idle=`, `light=`, `bucket=` var).

**Ortam notu:** PowerShell'de `bash` komutu Windows'un kendi bash'ini (WSL) açıyor; telefon aracı bulunamıyor.
Kullanıcı'ya verilecek komut Git Bash'i tam yoluyla çağırmalı: `& "C:\Program Files\Git\bin\bash.exe" scripts/…`.

**Açık (model):** düşünme kipiyle karta dayalı yanıt · kablosuz pil tüketimi · çok turlu sohbet, araç API'si, görsel/ses · bellek
(tek okuma: süreç 237 MB PSS, model belleği ayrı sayılıyor olabilir; doğrulanmadı) · şemaya zorlamanın
yolu (kütüphanede `ResponseFormat` var, denenmedi) · gömme modeli ve RAG · 50 örnekli Türkçe set.
