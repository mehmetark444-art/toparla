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

**Denenmedi:** içerik okuma yetkisi açık sürüm (K17'ye aykırı; Kullanıcı kararı gerekir) · pil
muafiyeti / otomatik başlatma / son uygulamalarda kilit · kullanım istatistiklerini sık sorgulama ·
release yapısı · uzun süre sonra servis ömrü.
