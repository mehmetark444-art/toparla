# Toparla · Güneş — Proje Beyni

Bu dosya projenin hafızasıdır. Bağlamı sıfırlanmış bir oturum, başka bir AI ya da yeni bir
geliştirici **yalnız bu dosyayı okuyarak** projenin ne olduğunu, bugüne nasıl geldiğini,
nelerin denenip neden değiştiğini ve hangi hataların bir daha yapılmaması gerektiğini anlamalıdır.

**Son güncelleme:** 9 Ekim 2026, oturum 6 (F1 kapanışı; F2-D hatırlatma motoru telefonda) · **Kapsadığı son commit:** `1df5756` · **Kapanan son faz:** F1

> **Zorunlu güncelleme kuralı:** Her faz kapanışında (ve fazı beklemeden: her karar kaydında,
> her yapılan hatada, her cihaz bulgusunda) bu dosya güncellenir. Bu dosya güncellenmeden
> hiçbir faz `yol-haritasi.md`'de ☑ işaretlenemez. Ayrıntılı kural: Bölüm 12.

## 0. Bu dosya ile diğerleri arasındaki iş bölümü

| Dosya | Sorusu | Not |
|---|---|---|
| `docs/BLUEPRINT.md` | Ne inşa ediyoruz? | Ürün tanımı (v4). **Değiştirilmez.** |
| `docs/decisions/NNNN-*.md` | Blueprint'ten nerede, neden saptık? | Karar kaydı blueprint'i ezer. |
| `docs/yol-haritasi.md` | Neredeyiz, sırada ne var? | Tek durum kaynağı; madde madde işaretlenir. |
| `docs/proje-beyni.md` (bu dosya) | Buraya nasıl geldik, ne öğrendik? | Anlatı, gerekçe, hatalar, tuzaklar. |
| `docs/platform-bulgulari.md` | Bu telefonda ne ölçtük? | Ham ölçümler ve yöntem. |
| `docs/progress.md` | Hangi oturumda ne oldu? | Kısa oturum günlüğü. |
| `docs/gece-kontrolleri.md` | Her gün sonunda ne bulundu, ne temizlendi? | Gece kontrolü kayıtları. |
| `CLAUDE.md` | Nasıl çalışıyoruz? | Çekirdek kurallar; ayrıntı `.claude/rules/` altında. |
| `docs/claude-code-duzeni.md` | Kancalar, yetenekler, alt ajanlar nasıl kurulu? | Claude Code altyapısı. |
| `AGENTS.md` | Başka bir AI aracı nereden başlar? | Giriş noktası; aynı kurallara işaret eder. |

Çelişki olursa öncelik: karar kaydı > blueprint; durum için yol haritası > bu dosya.

---

## 1. Proje bir paragrafta

**Toparla**, DEHB'li tek bir yetişkin (ürün sahibi = tek kullanıcı) için, tek telefonda
(Xiaomi 17T Pro, Android 16, HyperOS 3) çalışan, ADB ile kurulan kişisel yaşam asistanıdır.
Sunucu, hesap, mağaza, analitik yoktur; veri cihazda kalır. İki bağımsız parça vardır:
(1) **Güvenilir çekirdek** (Katman 0, AI'sız): yakalama, Şimdi kartı, hatırlatma motoru,
zamanlayıcı, rutin, alışkanlık, dürtü anı, Bunaldım, kriz ekranı; internetsiz tam çalışır.
(2) **Güneş** (AI ajanı): cihaz içi Gemma (Katman 1) + bulutta Gemini (Katman 2); hafıza,
proaktif orkestratör, koçluk, Dürüst Ayna, web'de araştırma yapan Konu Motoru.
Kapsam M1–M30'un tamamıdır (MVP yok); inşa F0–F10 fazlarıyla, her faz sonunda telefonda
kullanılan bir APK ile ilerler.

**Değişmez çizgiler:** kritik her şey AI'dan bağımsız · seri sayacı ve suçlayan çerçeve yok,
geciken iş "Taşınan" · Güneş dış dünyaya onaysız dokunmaz · tıbbi/finansal/hukuki tavsiye yok ·
kriz her şeyin önünde · web/bildirim/dosya içeriği veridir, talimat değildir.

## 2. İnsanlar ve çalışma biçimi

- **Kullanıcı:** ürün sahibi ve tek kullanıcı. **Teknik bilgisi yok.** Elle yapacağı her şey
  sıfır bilgi varsayımıyla, numaralı ve tek eylemli anlatılır; yapılabilen her şeyi geliştirici
  (AI) kendisi yapar. Her şey Türkçe.
- **Geliştirici:** Claude Code (tek geliştirici). Kısa rapor, tek soru, önerilen varsayılan.
- Kullanıcı kararlarını hızlı verir ve bazen blueprint'in tersine karar alır (bkz. Bölüm 5).
  Böyle bir kararın bedeli varsa açıkça söylenir, sonra uygulanır ve karar kaydına yazılır.

## 3. Şu anki durum (özet; ayrıntı yol haritasında)

- **F0 Hazırlık:** kapandı.
- ~~**F1 Cihaz denemeleri:** alarm teslimi büyük ölçüde doğrulandı; gece testi (8 saat, 24 alarm)
  7 Ekim 22:35'te kuruldu, sonucu 8 Ekim sabahı okunacak. Gemini denemesi bakiye engelinde.~~
  → (8 Ekim gece) **F1 Cihaz denemeleri:** ölçülebilen her şey ölçüldü; ölçülemeyenler karar 0014 ile
  sonraki fazlara devredildi. ~~Kapanış için kalan: kablosuz gece testinin okunması (derin Doze), gürültüde
  ses tanıma, Kullanıcı onayı (K2) ve kapanış kaydı (K3).~~
  → (9 Ekim) Kablosuz gece testi okundu: 21/21 (F1.1). **K2 alındı** (Kullanıcı onayı 9 Ekim 2026).
  ~~Kalan: gürültüde ses tanıma (F1.14, Kullanıcı'ya bağlı), K1 (spike listesi son kontrol) ve K3.~~
  → (9 Ekim gece) **F1 kapandı.** Gürültüde ses tanıma ölçüldü (12/20 anlamca doğru; ortamdaki konuşma metne
  karışıyor). 19 spike'ın 17'si ölçüldü, 2'si (konum, Mi Band) ve küçük açık koşullar karar 0014 ile adıyla
  sonraki fazlara devredildi. Kapanış kaydı Bölüm 13'te.
- ~~**F2 Hatırlatma motoru:** yalnız saf mantık (`:domain`) yazıldı; 58 birim testi geçiyor.~~
  → (8 Ekim gece) **F2 Hatırlatma motoru:** saf mantığın tamamı (F2-A) bitti; 103 birim testi geçiyor.
  ~~Android tarafı (`:reminders`), veritabanı ve arayüz **henüz yok**.~~
  → (9 Ekim) Altyapı (F2-B) da yazıldı: veritabanı, ayarlar, gizli değer kasası, Hilt, kalite araçları,
  imzalı sürüm. Hatırlatmanın Android tarafı (`:reminders`) ve arayüz **henüz yok**; asıl uygulama telefonda
  kurulu ama ekransız.
  → (9 Ekim akşamı, bulut) **F2-C tasarım sistemi bitti:** görsel dil B (karar 0016), `:ui` teması ve 14 bileşen,
  kontrast ve ekran görüntüsü testleri, uygulama ikonu, 5 sekmeli gezinme iskeleti (her sekme şimdilik boş
  durum gösterir). Bu iş `claude/faz-2-tasarim-yweqr9` dalında; ana dala alınmadı, telefona kurulmadı, yalnız
  GitHub Actions'ta derlenip sınandı. Hatırlatmanın Android tarafı (F2-D) ve Hatırlatma Sağlığı (F2-E) **henüz yok**.
- ~~Telefonda çalışan tek şey atılacak deneme uygulaması (`:spike`, "Toparla Spike").
  Asıl uygulama (`:app`) boş bir kabuktur.~~ → (9 Ekim) `:app` artık tema ve gezinme iskeletine sahip, ama
  ~~telefondaki kopyası eski (ekransız) sürüm; telefonda işe yarayan tek şey hâlâ deneme uygulaması (`:spike`).~~
  → (9 Ekim gece) Bulut dalı ana dala alındı; iskeletli sürüm telefona kuruldu ve `release` açıldı (çökme yok).
  Telefonda iki uygulama: asıl uygulama (iskelet; henüz hatırlatma yapmıyor) ve deneme uygulaması (`:spike`).

## 4. Mimari ve depo

```
toparla/
├─ CLAUDE.md, AGENTS.md      çalışma protokolü; başka AI araçları için giriş
├─ .claude/                  settings.json (izin, ortam, kanca) · hooks/ · rules/ · skills/ · agents/
├─ scripts/adb               adb sarmalayıcısı (PATH ve yol çevirme sorununu çözer)
├─ docs/                     BLUEPRINT, decisions/, yol-haritasi, proje-beyni, platform-bulgulari, …
├─ gradle/libs.versions.toml sürümler (resmi kaynaktan doğrulanıp kilitlenir)
├─ domain/    saf Kotlin/JVM: iş kuralları, Android sınıfı YOK      ← şu an kod burada
├─ data/      Room, DataStore, repository                            (boş)
├─ reminders/ AlarmManager, bildirim, FGS, alıcılar                  (boş)
├─ ai/        LlmClient, Gemini ve yerel istemci, yönlendirici, ajan (boş)
├─ sensors/   duyargalar, ContextHub, erişilebilirlik                (boş)
├─ ui/        tasarım sistemi                                        (boş)
├─ app/       tek Activity, feature/*, manifest                      (boş kabuk)
└─ spike/     S0 cihaz denemeleri; ürüne bağımlı değil, F10'da silinir
```

Bağımlılık tek yönlü: `app → {ui, sensors, ai, reminders, data} → domain`.
`applicationId`: `com.toparla.app` (debug: `.dev`), spike: `com.toparla.spike`.

**Sürümler (7 Ekim 2026'da resmi depolardan doğrulandı):** AGP 9.4.1 · Gradle 9.6.0 ·
Kotlin 2.4.20 · JUnit Jupiter 6.1.3 · `compileSdk = minSdk = targetSdk = 36`.
**Eklenenler (8–9 Ekim, aynı yolla doğrulandı; hepsi birlikte derleniyor):** KSP 2.3.12 · Room 3.0.3
(`androidx.room3`) · sqlite-bundled 2.7.1 · Hilt 2.60.1 · coroutines 1.11.0 · DataStore 1.2.1 · Timber 5.0.1 ·
LeakCanary 2.14 · detekt 1.23.8 · ktlint-gradle 14.2.0 · AndroidX Test runner 1.7.0 / ext-junit 1.3.0.

**`:domain` içindekiler (`com.toparla.domain`):**
- `Defaults` — adlandırılmış varsayılanlar (bildirim bütçesi 10, ısrarlı takip 30 dk, …).
- `core/` — `Clock`, `RandomSource`, `IdGenerator`, `AppError`.
- `reminder/ReminderPlanner` — saf fonksiyon `plan(now, tanımlar, mevcut) → kurulacaklar + iptaller`;
  5 tekrar kuralı, 48 sa pencere, 200 alarm sınırı (kritik düşmez), yaz saati kuralları.
- `reminder/Ladder` — sınıfa göre yükselme basamakları.
- `reminder/PersistentFollowUp` — ısrarlı takipte "bir sonraki soru ne zaman?".
- `reminder/OccurrenceStateMachine`, `SnoozePolicy` — teslim durumu ve erteleme.
- (8 Ekim gece, F2.6–F2.9) `ReminderPlanner.plan` artık yanıt bekleyen teslimleri (`InFlightOccurrence`),
  ertelemeleri (`SnoozedDelivery`) ve takip ayarını (`FollowUpConfig`) da alır: sıradaki merdiven basamağını
  (`…#lN`), ısrarlı takip sorusunu (`…#fN`, her zaman kesin yol) ve ertelemeyi (`…#sN`) kurar. Vakti geçmiş
  olan kurulmaz, `PlanResult.dueNow` ile "hemen teslim et" diye döner. Kritik olmayan ısrarlı işte merdivenin
  tek tekrarının yerini ısrarlı takip alır; sessizlik ısrarlı takibi erteler, kritik merdiveni ertelemez.
- `reminder/DeliveryRules` — `DeliveryGrouping` (aynı dakikadakiler tek kart; geç teslim toleransı 1 dk),
  `DeliveryAuditor` (vakti geçmiş, ateşlenme kaydı olmayan teslimler), `CriticalWatchdog` (20 dk içindeki
  kritik olayın alarmı var mı), `MaintenancePolicy` (12 saatte bir bakım).
- (9 Ekim, F2-B) `:domain`'e eklenenler: `core/DispatcherProvider`, `core/RotatingLogFile`, `reminder/RecurrenceCodec`
  (tekrar kuralının veritabanı metni), `Settings.kt` (`FeatureFlag`, `SettingsRules`, `SecretStore` arayüzü).
  `:domain` artık `kotlinx-coroutines-core`'a bağlı (saf Kotlin; Android değil).
- **`:data` (9 Ekim, F2-B):** `db/` Room 3 tabloları (`Reminder`, `ReminderOccurrence`, `ScheduledAlarm`,
  `DeliveryLog`), DAO'lar, `ToparlaDatabase` (şema `data/schemas/`'a dışa aktarılır), `PreMigrationBackup`,
  `ReminderStore` (tabloları planlayıcı modellerine çevirir; bozuk satırı atlar; çift teslimi `dedupeKey` ile
  engeller) · `settings/SettingsStore` (DataStore) · `secret/KeystoreSecretStore` (AES-256-GCM) ·
  `core/SystemSources` (sistem saati, UUID, rastgelelik, dağıtıcılar: sisteme **yalnız buradan** dokunulur).
  Cihaz testleri `data/src/androidTest` (15 test, telefonda koşar: `./gradlew :data:connectedDebugAndroidTest`).
- **`:app` (9 Ekim, F2-B):** `ToparlaApp` (Hilt kökü, dönen günlük), `di/AppModule`, `debug`/`release` kaynak
  kümelerinde `DevTools` (StrictMode yalnız debug). Henüz ekran yok (F2.24).
- **Kalite kapısı:** `./gradlew :domain:test ktlintCheck detekt :app:lintDebug :app:assembleDebug`; aynısı
  `.github/workflows/check.yml`'de (push'tan sonra koşacak).
- Testler: ~~103~~ → ~~113~~ → 114 JVM + 15 cihaz. İnvaryantlar tohumlu rastgele girdiyle (300 deneme) sınanır; 18 zorunlu senaryonun saf
  mantıkla ifade edilebilen 12'si `:domain`'de, kalan 6'sı Android tarafında (yol haritası F2.46).

## 5. Kararlar ve gerekçeleri

Blueprint'in kilitli kararları (K1–K23) geçerlidir; aşağıdakiler onları **değiştirenler**.

| # | Karar | Neden | Bedeli / dikkat |
|---|---|---|---|
| 0001 | Bulut katmanı Claude değil **Gemini API** | Kullanıcı kararı; anahtarı Gemini | Blueprint'teki Claude/Anthropic/Sonnet/Haiku/Opus ifadeleri Gemini karşılığıyla okunur. Web arama → Google Arama temellendirmesi. Kademe modelleri henüz kilitlenmedi. |
| 0002 | **Yasak kelime listesi yok** | Kullanıcı: "yasak kelime olmasın". Liste zaten blueprint'in kendi onaylı metinleriyle çakışıyordu ("Bugün hâlâ senin günün") | LLM'in ürettiği kırıcı ifade artık otomatik yakalanmıyor; koruma prompt yönergesi + "Bu beni kırdı" + valf. Seri sayacı yok / "Taşınan" kuralları **duruyor**. |
| 0003 | Bildirim bütçesi **10** (aralık 10–20); **ısrarlı takip**: Kullanıcı'nın üstlendiği iş "Yaptım" denene dek 30 dk'da bir sorulur | Kullanıcı: "en az 10 olsun… peşimi bırakmasın" | Blueprint'in bildirim yorgunluğu kaygısının tersine. Sınırlar Kullanıcı onaylı: uyku/sessiz saat, kriz/Bunaldım sonrası 3 sa, odak oturumu ve "Bugün sessiz"de susar, sabah sürer. Bütçeden muaf. |
| 0004 | İlk odak alışkanlıklar: **Sigara (tam bırakma) + Uyku Ritmi** | Kullanıcı seçimi bana bıraktı; sigara en net hedef, uyku diğer her şeyin temeli | Diğer dört alışkanlık "izleniyor"; dürtme almaz. |
| 0015 | Kullanıcı'nın göreceği her tasarım **`mobile-app-ui-design` yeteneğiyle** yapılır: önce görsel taslak → Kullanıcı onayı → Compose → telefonda karşılaştırma. Yetenek `.claude/skills/` altına eklendi (üçüncü taraf; eklenmeden önce tamamı okundu) | Kullanıcı: "benim göreceğim tüm UI/UX tasarımlar bu skill ile yapılacaktır… tüm tasarım çok iyi olmalıdır" | Ekran işlerine onay adımı eklenir. Yeteneğin seri, kırmızı, parlak kutlama, düşük kontrast, 44 pt gibi önerileri Toparla kurallarıyla çelişir: çizelge `.claude/rules/tasarim.md`, çelişkide Toparla kuralı kazanır. Görsel ayrıntılar (palet, yazı) Kullanıcı seçimiyle blueprint C'den sapabilir. Google Stitch bağlayıcısı bu ortamda yok. |
| 0016 | Görsel dil **Seçenek B**: açık palet koyulaştırıldı (`primary #47705F`, Taşınan yazısı `#7C5B12` vb.), kartta sınır yerine sıcak yumuşak gölge, ekranda 4 yazı boyutu (32/20/16/13) ve 2 ağırlık, kenar ve kart içi 24 dp, ikincil düğmeler hafif zeminli | Taslakta kontrast hesaplandı: blueprint'in açık paletinde 5 çift 4,5:1'in altında (en düşük 2,06). Kullanıcı taslağı görüp "B'yi beğendim" dedi | Koyu ve AMOLED renkleri değişmedi. Blueprint C'nin görsel ayrıntılarından sapma; DEHB ve erişilebilirlik kuralları aynen. Gerçek görünüm telefonda henüz karşılaştırılmadı. |
| 0014 | F1'de ölçülemeyen maddeler ilgili fazlara **devredildi** (konum ve Mi Band → F7 ön koşulu; kısıtlı kova ve küçük açık koşullar → F2; model kalanları → F6; liste karar kaydında) | Kullanıcı evden çıkamıyor, Mi Band siparişte, bir ölçüm günler istiyor; F1 açık kalırsa F2-D başlayamıyor. Kullanıcı: "onaylıyorum devret" | Blueprint'in "tüm `[DOĞRULA]` kapandı" ölçütünden sapma: bazı varsayımlar kod yazılırken ölçülmemiş olacak. Devredilen ölçüm yapılmadan o faz kapanmaz. |
| 0013 | Konuşma tanıma: Android cihaz içi tanıyıcı (`tr-TR`); Whisper eklenmez | Ölçüm: 30 cümlede gerçek sözcük hatası ~%6, 21/30 anlamca tam doğru; Türkçe paket kurulu | Sayılar rakamla ve biçimli gelir: `TrDateParser` buna göre yazılır. Gürültü, kulaklık, uzun konuşma ölçülmedi. Özel adlar zayıf. |
| 0012 | **Çıraklık dönemi** (3 hafta): Yeşil her AI işi önce Gemini'ye; yanıtlar bilgi kartı ve örnek olarak saklanır, gece gölge koşuyla karşılaştırılır, ≥ 30 koşuda ≥ %90 uyumlu görev yerel modele devredilir. Kişisel veri cihazda kalır (seçenek A). Yerel "yok" derse Yeşil soru Gemini'ye gider ve karta dönüşür | Kullanıcı istedi: yerel model zamanla Gemini'den öğrensin | Blueprint'te olmayan mekanizma; F6–F7'ye iş ekler. İlk haftalar internete bağlı ve daha maliyetli. "Örnek" kanalının etkisi ölçülmedi (F1.25). |
| 0011 | RAG: EmbeddingGemma 2 (önekli, 256 boyut) + sözcük araması RRF; bilgi kartında "diğer ifadeler" alanı; model reddederse bulunan kart gösterilir | Ölçüm: hibrit 17/20, zengin kart 19/20 isabet; üretimde 0 uydurma | E4B karta sadık ama harfiyen: yanıtlanabilir soruların ~%65–70'ini yanıtlıyor, gerisinde "yok" diyor. Küçük set; düşünme kipi denenmedi. Karar yetkisi Kullanıcı tarafından devredildi. |
| 0010 | Cihaz içi **tek** model **Gemma 4 E4B** (Kullanıcı onayladı; yedek model yok, kısıtta yedek Katman 0); Qwen3 elendi. İlk öneri: | 50 soruluk Türkçe set: E4B talimata en iyi uyan ve metni en doğal olan; E2B iki kat hızlı; Qwen3 doz verdi ve gömülü talimata uydu | Küçük set; çok turlu sohbet, görsel, gerçek RAG ölçülmedi. Tarih ayrıştırma modele bırakılmaz. |
| 0009 | **Önce yerel model:** AI görevlerinin varsayılanı cihaz içi; bulut yalnız gerektiğinde (web araştırması, doğrulayıcıdan geçemeyen çıktı, Kullanıcı isteği, ölçülmüş kalite açığı). Model Gemma olmak zorunda değil. APK eşiği 150 MB | Kullanıcı: "Gemini'yi en az kullanalım, yerel modelin yapabildiği her şeyden faydalanalım" | Blueprint F3 tablosunun tersi. Küçük model sohbet/planlama/Ayna'da zayıf kalabilir; pil ve ısı artar. "Yerel yeterli" yalnız altın set ölçümüyle söylenir. |
| 0008 | Müdahale gecikmesi hedefi ≤ 3,5 sn (**kabul**). Ekran okuma (**aday**): Güneş ekranı okuyup kaydedilmemiş şeyleri önersin; yalnız cihaz içi, asla bulut | Kullanıcı 3 sn'yi kabul etti ve ekran okumayı kendisi istedi ("şifreleri görmesi sorun değil, yalnız yerel model") | Blueprint K17'nin tersi ve kapsam dışı yeni yetenek; projenin en geniş izni. Cihaz içi model henüz denenmedi; uygulanabilirlik bilinmiyor. Parola/banka/gizli sekme dışlaması önerildi; Kullanıcı riski duyup "hepsini okusun" dedi: dışlama yok. Ham metin saklanmaz, saklanan öneride Kırmızı kalıplar maskelenir. |
| 0007 | Gemini kademeleri: hızlı `gemini-3.5-flash-lite`, günlük `gemini-3.8-flash` (etkileşimde düşünme `low`), derin `gemini-3.1-pro-preview`; takma ad yok | Ölçüm + resmi fiyat sayfası | Derin kademe önizleme modeli; günlük kademe fiyatı 1 Ocak 2027'de iki katı; Türkçe kalite henüz ölçülmedi. |
| 0006 | Alarm yolu: Kritik `setAlarmClock`; Önemli, **Normal** ve ısrarlı takip `setExactAndAllowWhileIdle`; esnek yol yalnız Bilgi | Gece testi: esnek yol 2–5 saat geç çaldı, ekran açıkken bile; kesin yollar ≤ 28 sn | Blueprint'ten sapma (Normal esnekti). Kesin alarm sayısı artar; pil F10'da ölçülür. Derin Doze henüz sınanmadı. |
| 0005 | Dil Türkçe (tanımlayıcılar İngilizce) · yedek parolayla şifreli · bekleme günlerinde sonraki faz flag arkasında · CI şimdilik yerel · v3'te eksik şemaları ben tasarlarım · JBR 21 ile derleme | Netleştirme turu 2 | — |

**Bekleyen karar adayları**
- ~~**Sınıf → alarm yolu.**~~ → kapandı: karar 0006 (8 Ekim 2026). Eski not: Bulgu: `setAlarmClock` her koşulda ±0,2 sn; `setExactAndAllowWhileIdle`
  bir kez 3,5 dk gecikti. Aday: ±1 dk sözü verilen her şey (kritik + ısrarlı takip)
  `setAlarmClock`. Karar 0003 şu an ısrarlı takip için `setExactAndAllowWhileIdle` diyor;
  gece testi sonucuna göre güncellenecek. `ReminderPlanner.apiFor` tek değişim noktası.
- ~~**Gemini kademe modelleri ve fiyat tablosu**~~ → kapandı: karar 0007 (8 Ekim 2026).
- **Konu Motoru varsayılan sıklığı** (maliyet ölçümünden sonra; aylık 25 $'ın %40'ı yetmeyebilir).

## 6. Zaman çizelgesi (ne yapıldı, hangi sırayla)

### 7 Ekim 2026 — Oturum 1
1. **Okuma ve boşluk analizi.** Blueprint v4 (2087 satır) ve v3 (2003 satır) baştan sona okundu.
   Bulunanlar: v3 dosyası §11.5'te kesik; yasak kelime listesi kendi metinleriyle çakışıyor;
   gözlem modunda bütçe çelişkisi; hava durumu çelişkisi; eksik izinler, tablolar, ekranlar.
2. **Netleştirme turu 2.** Kullanıcı yanıtları → kararlar 0001–0005.
3. **Ortam.** Android Studio 2025.2.2, SDK 36, adb kurulu bulundu; ek kurulum gerekmedi.
4. **Depo.** `git init`, belgeler `docs/`'a taşındı, `CLAUDE.md`, belge iskeleti. `4ca872c`
5. **Gradle iskeleti.** 7 modül, sürüm kataloğu; boş APK derlendi. `65c129f`
6. **Spike 1 (alarm teslimi).** `:spike` uygulaması yazıldı (`c38080e`); ölçümler:
   ekran açık/kapalı, kaydırıp kapatma, yeniden başlatma, kilitli yeniden başlatma geçti
   (`3963fc6`, `2e76340`, `26b59eb`, `56fb79d`); gece testi kuruldu (`4773d52`).
7. **Spike 11 (Gemini).** Anahtar Developer API ile çalışıyor; üretim çağrıları 402. `d9c7dd9`
8. **Hatırlatma saf mantığı.** Planlayıcı (`05ebe61`), merdiven + ısrarlı takip (`8a970bf`),
   durum makinesi + erteleme + çekirdek arayüzler (`0d39466`).
9. **Yol haritası** (`0cf287b`, `8a0d633`) ve bu dosya (`81f1c19`).

### 8 Ekim 2026 — Oturum 1 (gece yarısından sonra)
10. **Claude Code altyapısı.** Resmi belgeler okunup eksikler çıkarıldı; o ana dek yalnız `CLAUDE.md` vardı.
    Kurulanlar: `.claude/settings.json` (ortam, izinler), 4 kanca (oturum başlangıcı, dosya koruma +
    faz kapısı, komut koruma, kod kuralları) ve 17 senaryoluk sınaması, 5 yola göre kural dosyası,
    7 yetenek, 3 salt okunur denetçi alt ajan, `AGENTS.md`, `scripts/adb`. `CLAUDE.md` kısaltıldı;
    kod kuralları ve tamamlama tanımı kural dosyalarına taşındı. Ayrıntı: `docs/claude-code-duzeni.md`.
    Önemli sonuç: "blueprint değişmez", "gizli değer depoya girmez", "push sorulur" ve "faz, proje
    beyni güncellenmeden kapanmaz" kuralları artık talimat değil, **kancayla zorunlu**. (`38ff16d`)
11. **İlk gece kontrolü ve gece kontrolü usulü.** Projenin tamamı satır satır yeniden okundu; 9 bulgu
    düzeltildi (yakalanmayan bozma, eksik kurucu denetimleri, kullanılmayan sabitler, gereksiz Gradle
    satırları, iki kanca açığı, bayat belge satırları). Bundan sonra çalışılan her günün son işi
    `/gece-kontrolu`; sonuçlar `docs/gece-kontrolleri.md`'de. Birim testi 58, kanca sınaması 21. (`2ac3000`)
12. **GitHub'a ilk push.** Depo herkese açık çıktı; Kullanıcı gizli yaptı. Bilgisayardaki varsayılan
    GitHub girişi başka hesap olduğu için uzak adres kullanıcı adıyla tanımlandı; 18 commit gönderildi,
    yerel ve uzak eşit (`6bc3304`), kimliksiz erişim 404.
13. **Gece testi okundu, karar 0006.** 24/24 alarm çaldı; esnek yol saatlerce geç kaldığı için Normal
    sınıf kesin yola alındı (`ReminderPlanner.apiFor`). Yeni oturumda başlangıç kancasının bağlama
    girdiği, 8 yeteneğin ve 3 alt ajanın listelendiği de görüldü (gece kontrolünün iki açık maddesi kapandı). (`f43db56`)
14. **Gemini ölçümleri, karar 0007.** Bakiye geldi (çağrıyla doğrulandı). Üç kademe, düşünme ayarı,
    şemalı çıktı, işlev çağrısı, arama temellendirmesi ve akış tek çağrılarla ölçüldü; kademeler ve
    fiyatlar kilitlendi. Görsel girdi, önbellekleme ve gerçek konu taraması henüz denenmedi. (`0d2f7ab`)
15. **Spike 2 ve 3.** Deneme uygulamasına servis ve tam ekran kartı eklendi; alarmdan servis başlatma
    ve kilitliyken tam ekran bildirim tek denemede çalıştı. Kalan koşullar yol haritasında açık. (`b203ef6`)
16. **Spike 5 (erişilebilirlik).** İki olumsuz bulgu: kaydırınca servis ölüp geri gelmiyor (`caa9a8a`);
    uygulama açılışı ~2,9 sn geç algılanıyor. Olumlu: servisten kart açma hızlı ve engelsiz.
    Üç yapılandırma değişikliği gecikmeyi düzeltmedi; ölçüm betiği `scripts/spike-a11y-olc.sh`.
    Sonra: son uygulamalarda kilit servisi koruyor; yeniden başlatmada servis kendiliğinden bağlanıyor.
17. **Kullanıcı kararları 0008 ve 0009.** Ekran okuma (yalnız cihaz içi, dışlama yok; aday) ve
    "önce yerel model" (bulut yalnız gerektiğinde; model Gemma olmak zorunda değil; APK eşiği 150 MB).
18. **Spike 10 (cihaz içi model).** LiteRT-LM 0.18.0 `:spike`'a eklendi; Gemma 4 E2B telefonda çalıştı
    ve ölçüldü. Dört aday model ve gömme modeli indiriliyor; karşılaştırma sırada.
19. **Model seçimi ve RAG.** Beş adayın elemesi, üç adayın 50 soruluk Türkçe seti → karar 0010
    (tek model Gemma 4 E4B; Kullanıcı onayladı; diğer dosyalar Kullanıcı onayıyla silindi).
    Ardından 24 kart / 24 soruyla gerçek RAG → karar 0011. Ölçüm araçları: `scripts/spike-llm-olc.sh`,
    `llm-puanla.mjs`, `rag-puanla.mjs`; setler `spike/src/main/assets/`.
20. **Çıraklık ve ses tanıma.** Karar 0012 (çıraklık dönemi, seçenek A) (`2262553`); 30 cümlelik Türkçe
    konuşma tanıma ölçümü → karar 0013 (`721dd5e`).
21. **"A grubu" (kısa telefon ölçümleri).** Ayar bağlantıları, Yakala kutucuğu, bildirim erişimi, kritik ses
    ve Rahatsız Etme, canlı bildirim, arama algılama ölçüldü; `scripts/kur.sh` yazıldı (koşusu sırada).
    Kutucukta blueprint'ten farklı bir bulgu: kilit ekranında ekran açılamıyor, yakalama ekransız yapılacak.
    (`80cb939`, `11297b5`, `51226d3`, `ae10fe3`, `649cb02`)
22. **Spike 13 (veritabanı yığını).** Room 3 + KSP + paketli SQLite `:spike`'ta derlendi ve telefonda koşuldu;
    FTS5'in Türkçe davranışı ölçüldü.
23. **F1 kapanış ölçümleri ve devir.** Servis yolları, tam ekran, süreç ölümü / otomatik başlatma, temizlik,
    saat değişimi, arka plandan arama, ekran okuma, Gemini kalanları, konu maliyeti, ALO 171, çıraklık ön ölçümü;
    kablosuz gece testi kuruldu. Ölçülemeyenler karar 0014 ile devredildi (`718cc1b`).
24. **F2-A bitti.** Planlayıcıya merdiven, ısrarlı takip ve erteleme; birleşik kart, geç teslim, denetçi,
    kritik bekçi; invaryant ve senaryo testleri (`33e1472`). 58 → 103 test.
25. **F2-B altyapı (8 Ekim gece → 9 Ekim).** Room 3 tabloları ve `ReminderStore`, migration öncesi kopya,
    DataStore ayarları ve özellik anahtarları, Keystore gizli değer kasası (`29b5948`); Hilt kökü, dönen
    günlük, debug/release, R8, imza anahtarı ve imzalı sürüm (`54be010`); ktlint, detekt, Lint, `log-cek.sh`,
    GitHub Actions iş akışı. İki sürüm telefona yan yana kuruldu. Açık: anahtar yedeği, API anahtarı
    yenileme, Actions'ın ilk koşusu (üçü de Kullanıcı'ya ya da push'a bağlı).
26. **Ara denetim (9 Ekim, Kullanıcı isteğiyle).** F0'dan F2-B'ye her şey denetlendi; 1 mantık hatası (H28),
    1 gizlilik eksiği, 2 dayanıklılık eksiği, kullanılmayan kod ve bayat belge satırları düzeltildi.
    Kayıt: `docs/gece-kontrolleri.md`.
27. **Gece kontrolü ve push (9 Ekim).** İki günün kapanışı (`0441671`); 26 commit GitHub'a gönderildi.
28. **Tasarım yeteneği (9 Ekim).** Kullanıcı isteğiyle `mobile-app-ui-design` eklendi; karar 0015 ve
    `.claude/rules/tasarim.md`. F2-C'den itibaren her ekran önce taslak olarak Kullanıcı'ya gösterilir. (`3e74b61`)
29. **Kablosuz gece testi okundu (9 Ekim).** F1.1 derin Doze koşulu: 21/21 alarm çaldı; kesin yollar ≤ 2,1 sn;
    esnek yol ~1 dk (ilk gece USB bağlıyken 2–5 saat gecikmişti). Sınır: `idle=true` yakalanmadı, pil muafiyeti
    açıktı. Karar 0006 değişmez. Bulgu `platform-bulgulari.md`'ye yazıldı. (`2e60b3c`)
30. **F1 K2 alındı; GitHub Actions ilk koşusu yeşil (9 Ekim).** Kullanıcı onayı: "hepsini gördüm" (alarm
    bildirimleri, kilit ekranında tam ekran kart, müdahale ekranı). GitHub Actions push sonrası koştu ve yeşil
    çıktı (Kullanıcı gözlemi). (`2cf4623`)
31. **F2-C başladı: görsel dil taslağı ve onayı (9 Ekim, bulut oturumu).** Kullanıcı telefondan, bulut
    oturumundan çalıştı (bilgisayar ve telefon bağlı değil). Şimdi, kritik hatırlatma ve Hatırlatma Sağlığı
    A/B olarak tuvalde çizildi; B onaylandı (karar 0016). Bulut ortamında Google'ın indirme sunucusu
    (`dl.google.com`) kapalı: Android derlemesi orada yapılamıyor, doğrulama GitHub Actions'a bırakıldı. (`6fabe9f`)
32. **F2.19–F2.23 bitti (9 Ekim, bulut oturumu).** `:ui` Compose teması (karar 0016 paleti, 3 tema × 6 vurgu,
    yazı/boşluk/hareket jetonları), 11 ortak bileşen + `ToparlaCard`, `ChipRow`, `ActionRow`; kontrast testi; Roborazzi
    ile 12 ekran görüntüsü, temel görüntüler depoda ve CI'da karşılaştırılıyor. CI `claude/**` dallarında da koşuyor;
    bulut oturumu görüntüleri günlükten okuyor. Göz kontrolü 3 gerçek kusur buldu (koyu temada siyah metin, %200'de
    bölünen kelimeler, taşan halka metni); hepsi düzeltildi. 13 CI koşusu; yanlışlar H30–H31. (`57ec7c1`)
33. **F2.24 ve F2-C bitti (9 Ekim, bulut oturumu).** Uygulama ikonu ve gezinme iskeleti önce tuvalde çizildi;
    Kullanıcı ikon A'yı (toparlanmış ip) ve iskeleti onayladı. Tek Activity, 5 sekmeli tür güvenli gezinme
    (Navigation 2.9.8; 2.10 compileSdk 37 istiyor), kenardan kenara, öngörülü geri, görünüm ayarları DataStore'dan.
    İşlevi olmayan eylemler (yakalama düğmesi, Ben, Güneş yazışması) yarım özellik kuralı gereği gizli. Göz kontrolü
    Güneş sekme ikonunun Şimdi'ye benzediğini yakaladı; düzeltildi. Telefonda henüz denenmedi. (`4890bcb`)
34. **Bulut dalı ana dalda, iskelet telefonda; F1 kapandı (9 Ekim gece).** `claude/faz-2-tasarim-yweqr9` ana
    dala alındı, yerelde derlendi, iki sürüm telefona kuruldu, `release` açıldı (`739bd3f`). Gürültüde ses
    tanıma ölçüldü; F1'in yarım maddeleri "ölçülen kısım ☑, kalan → karar 0014" biçiminde kapatıldı; K1, K2, K3.
35. **F2-D: hatırlatma motoru telefonda (9 Ekim gece).** Teslim hattının mantığı `:domain/ReminderEngine`'de
    (planlama, teslim, merdiven, ısrarlı takip, eylemler, geç teslim, bekçi; sahte depoyla 19 test). `:reminders`:
    `AlarmManagerScheduler` + `BootMirror`, `AndroidReminderNotifier` (8 kanal), alıcılar, `ReminderService`,
    güvenlik ağı işleri. `:app`: `ReminderFullScreenActivity`, açılışta yeniden planlama, debug tetikleyici.
    Cihazda uçtan uca: kritik teslim saniyesinde, merdiven, Yaptım, erteleme, ısrarlı takip, tam ekran kart.
    Üç kusur denemede bulundu ve düzeltildi (H34, H35; silinen hatırlatmanın bildirimi). (`3802e02` … `1df5756`)

## 7. Bu telefonda öğrenilenler (özet; ham veri `platform-bulgulari.md`)

- **Cihaz:** `2602EPTC0G` / `warhol_global`, Android 16 (API 36), HyperOS `OS3.0.310.0.WPSMIXM`,
  `arm64-v8a`, sayfa boyutu 4096 (16 KB değil).
- **`setAlarmClock` güvenilir:** ekran açık/kapalı, kaydırıp kapatma, yeniden başlatma ve
  kilitli yeniden başlatmada 12–153 ms sapma.
- **`setExactAndAllowWhileIdle` bir kez 211 sn gecikti** (ekran kapalı, kova zorlanırken). Tek ölçüm;
  gece testindeki 8 ölçümde tekrarlanmadı (en çok 28 sn). Kök neden bilinmiyor.
- **Gece testi (8 saat, 24 alarm, özel izin yok):** hepsi çaldı. `setAlarmClock` ≤ 1,4 sn,
  `setExactAndAllowWhileIdle` ≤ 28 sn. **`setAndAllowWhileIdle` 2–5 saat geç**, ekran açıkken bile;
  zamanlı hiçbir işte kullanılmaz (karar 0006). Derin Doze alarm anında hiç gözlenmedi: o koşul açık.
- **Kablosuz gece testi (9 Ekim; 7 saat, şarjsız, pil muafiyeti ve otomatik başlatma açık):** 21/21 çaldı;
  kesin yollar ≤ 2,1 sn; esnek yol bu kez yalnız ~1 dk gecikti (ilk gece 2–5 saatti: o gece USB bağlıydı ve
  muafiyet yoktu). Karar 0006 değişmez. Teslim anında `idle=true` yakalanmadı; hafif uyku 4 kez görüldü.
- **Hatırlatma motoru cihazda (9 Ekim gece, debug sürümü, tek denemeler):** kritik teslim planlanan saniyede;
  merdiven +2 / +5 dk saniyesinde; bildirim düğmeleri uygulama açılmadan çalışıyor. **Tam ekran kart yalnız
  ekran kapalıyken açılır** (ekran açıksa, kilit ekranında bile, şerit gelir) ve **yalnız yeni eklenen
  bildirimde** başlar (güncellemede başlamaz). Yeniden başlatma, Rahatsız Etme ve arka plan işleri asıl
  uygulamada henüz denenmedi.
- **Gürültüde konuşma tanıma (9 Ekim gece; 20 cümle, konuşmalı ev gürültüsü):** 12/20 anlamca doğru (sessizde
  21/30). Yeni risk: tanıyıcı **ortamdaki başka konuşmayı da yazıyor** (4 cümlede cümle başına yabancı söz
  eklendi). Sayı ve saatler gürültüde de doğru. Yakalanan metin her zaman gösterilip düzeltilebilir olmalı.
- **Kaydırıp kapatma zorla durdurma değildir:** HyperOS 3'te alarmlar korunur (`stopped=false`).
  Bu, otomatik başlatma / pil muafiyeti **verilmeden** gözlendi (tek deneme, 2 dk ufuk).
- **Zorla durdurma** alarmları siler; uygulama yeniden açılınca sistem `BOOT_COMPLETED`
  yayınlarını gönderir ve alıcı her şeyi yeniden kurar. Açılana kadar alarm yoktur.
- **Direct Boot çalışıyor:** kilit açılmadan (`RUNNING_LOCKED`) `LOCKED_BOOT_COMPLETED` gelir,
  `directBootAware` alıcı cihaz korumalı depolamadan alarmları kurar, alarm kilitliyken çalar.
  Yeniden başlatmadan yeniden kuruluma ~30–45 sn.
- **Kurulum:** `adb install` için Geliştirici seçenekleri → "USB ile yükle" açık olmalı ve
  telefondaki onay penceresi ~10 sn içinde onaylanmalı; yoksa `INSTALL_FAILED_USER_RESTRICTED`.
- **USB bağlıyken Doze zorlanamıyor** (`force-idle` → "stopped at INACTIVE"; cihaz şarjda sayılıyor).
  `am set-standby-bucket … rare` kalıcı olmuyor.
- **Cihaz içi model çalışıyor (8 Ekim):** LiteRT-LM 0.18.0 + `.litertlm` dosyaları; modeller
  `huggingface.co/litert-community`'den girişsiz iniyor, `adb push` ile telefona atılıyor. Gemma 4 E2B:
  ilk yükleme 34 sn, sonrakiler ~2 sn; kısa işler 0,4–2,7 sn; ısınma yok (kısa koşu). Türkçesi düzgün
  ama istenen JSON şemasına kendiliğinden uymuyor: doğrulayıcı ve onarım şart. Model seçimi açık
  (karar 0009: Gemma şart değil). **Eleme turu (11 istem, tek koşu):** Gemma 4 E4B en isabetli,
  E2B yaklaşık iki kat hızlı; Phi-4 mini bellekten öldürüldü, Ministral'in Türkçesi bozuk ve tıbbi
  sınırı aştı, ~~Qwen3 bu paketle çalışmıyor~~ → Qwen3 sohbet şablonuyla çalışmıyor ama elle kurulan
  ChatML istemiyle (`Session` arayüzü) çalışıyor ve temiz JSON üretiyor: aday. Phi-4 mini CPU'da
  tamamlıyor ama Türkçesi bozuk ve çok yavaş: elendi. Ayrıntılı sete girenler: Gemma 4 E4B, E2B, Qwen3 4B.
  GPU önbelleği model başına 0,8–3,8 GB ek yer tutuyor.
- **Erişilebilirlik servisi kaydırmayla ölüyor (8 Ekim):** uygulama son uygulamalardan kaldırılınca
  HyperOS süreci öldürüyor (`SwipeUpClean`); servis "Crashed" durumuna düşüyor, ayarda açık görünüyor
  ama olay almıyor ve uygulama yeniden başlasa da geri bağlanmıyor. Müdahale ekranının (M25-I)
  en büyük riski bu; önlemler sınanmadan o özellik tasarlanmaz. **Önlem (tek deneme):** uygulama son
  uygulamalarda kilitliyken "tümünü temizle" (`OneKeyClean`) süreci öldürmedi, servis bağlı kaldı.
  Yeniden başlatmadan sonra servis kilit açılınca kendiliğinden bağlandı (tek deneme). Kilidin
  yeniden başlatma ve güncelleme sonrası kalıp kalmadığı henüz bilinmiyor.
- **Uygulama açılışı ~2,9 sn geç algılanıyor (8 Ekim, 21 ölçüm):** olay servise ulaştıktan sonra kart
  ~70 ms'de çiziliyor, ama açılışta olayın kendisi ~2,9 sn geç geliyor (açılış olmayan geçişlerde
  ~100 ms). Sürekli servis, pencere bayrağı ve ek olay türleri değiştirmedi; neden bilinmiyor.
  Blueprint'in ≤ 400 ms hedefi bu yapılandırmada tutmuyor. Servis paket güncellemesinden sonra
  kendiliğinden bağlanıyor; paket süzgeci bağlanma anında sızdırıyor (kodda da denetle).
- **Alarmdan servis ve tam ekran (8 Ekim, tek denemeler):** `setAlarmClock` alıcısından `specialUse`
  foreground service başlatılabiliyor (29 ms). Kilitliyken tam ekran bildirim, HyperOS'e özgü hiçbir
  izin elle verilmeden kilit ekranının üstünde açılıyor ve ekranı uyandırıyor (270 ms). Blueprint'in
  kritik teslim hattı (alarm → servis → tam ekran) bu telefonda kurulabilir görünüyor.
- **"A grubu" yüzey ölçümleri (8 Ekim gece; çoğu tek deneme):**
  - *Ayar bağlantıları:* 11 adaydan 10'u uygulama içinden açılıyor; eski `HiddenAppsConfigActivity` yok.
    Tablo `hyperos-baglantilar.md`.
  - *Yakala kutucuğu:* kilit açıkken dokunuş → mikrofon 0,29 sn. **Kilitliyken `showWhenLocked` Activity
    açılmıyor, sistem PIN istiyor;** kutucuk diyaloğu da görünmüyor. Çalışan yol: tanıyıcıyı doğrudan
    `TileService` içinde başlatmak (0,25 sn, ekransız); geri bildirim kutucuğun yanık/sönük durumu.
    F3.8 (`LockCaptureActivity`) buna göre tasarlanır.
  - *Bildirim erişimi:* adb ile kurulan uygulamada anahtar engelsiz açıldı ("kısıtlı ayar" çıkmadı);
    bildirimler ve doğrulama kodu süzgeci çalışıyor. Kopma sonrası yeniden bağlanma ölçülmedi.
  - *Kritik ses:* alarm ses akışlı kanal art arda bildirimde kısılmıyor ve (erişim verilmişken)
    Rahatsız Etme'yi aşıyor: 3/3 ve 3/3.
  - *Canlı bildirim:* `ProgressStyle` + ek alan `android.requestPromotedOngoing` sistemce
    `PROMOTED_ONGOING` sayılıyor; durum çubuğunda hap, panelde çubuk, kilit ekranında görünüyor.
  - *Arama:* `AudioManager.getMode()` giden hücresel aramada izinsiz `2` veriyor.
  - *`kur.sh`:* `pm grant` ve tam ekran `appops` tutuyor; kullanım istatistikleri ve üstte gösterme
    `appops` komutları hata vermeden **tutmadı** (olası neden: `:spike` o izinleri istemiyor; doğrulanmadı).
    Kova, pil muafiyeti varken `5` (muaf) kalıyor. Bildirim erişimi olan uygulama Rahatsız Etme erişimini de alıyor.
- **Süreç ölümü ve otomatik başlatma (8 Ekim gece, tek denemeler) — tasarımı etkiler:** süreç çökünce
  HyperOS, "Otomatik başlatma" izni yoksa uygulamayı ve servislerini geri başlatmıyor (sistem kaydı:
  `AutoStartManagerService … Reject service`). İzin açıkken süreç kendiliğinden kalkıyor ve bildirim
  dinleyicisi ~18 sn'de bağlanıyor. Dinleyici için `requestRebind` bu telefonda etkisiz; bileşeni kapat-aç
  1 sn'de bağlıyor. **Erişilebilirlik servisi hiçbir durumda kendiliğinden dönmüyor** (yalnız güncelleme,
  yeniden başlatma, elle kapat-aç). Sonuç: otomatik başlatma sihirbazda zorunlu; müdahale ekranı "kopabilir"
  varsayımıyla ve Sağlık uyarısıyla tasarlanır. Alarm teslimi bundan bağımsız (süreç ölse de çalıyor).
- **Diğer kapanış ölçümleri (8 Ekim gece):** servis dört yoldan da başlatılabiliyor (iki alarm türü, bildirim
  eylemi, kutucuk). Ekran açık ve kilitsizken tam ekran kart açılmıyor, şerit geliyor. Kritik ses ekran
  kapalıyken de çalıyor. Kilitli uygulama Güvenlik temizliğinden etkilenmedi. Saat değişince alarmlar
  yeniden kuruluyor. Arama durumu arka plandan da okunuyor (hücresel `2`, WhatsApp `3`).
- **Ekran okuma ön ölçümü (8 Ekim gece):** ekran metnini toplamak ortanca 49 ms (%90 276 ms, en çok 2,2 sn),
  işlemci ~%2; parola alanları sistemce işaretli. Toplama darboğaz değil; açık olan, metni modele işletmek.
- **Maliyet ve hat (8 Ekim gece):** bir konu taraması 0,1–0,2 sent (konu başına ayda ~0,07–0,11 $). Model
  bazen **hiç arama yapmadan** "yenilik yok" diyor: sorgu sayısı 0 olan tarama başarısız sayılmalı.
  ALO 171 etkin; cep telefonundan ücretli.
- **Çıraklık ön ölçümü (8 Ekim gece):** öğretmen (Gemini günlük) 50 soruda 49,5. Yerel E4B örneksiz 45,5;
  aynı kategoriden iki öğretmen örneği eklenince **48,5** (harfiyen 43 → 48), süre ~%30–40 uzun. Karta dayalı
  yanıtta örnek reddi artırabiliyor (1 soru). Ret → Gemini kartı → yerel yanıt zinciri 6/6 işledi. Karar
  0012'nin "örnek kanalının etkisi ölçülmedi" notu kapandı (küçük set; örnekler sorulara çok benzer).
- **Veritabanı yığını (8 Ekim, tek koşu):** Room'un güncel ailesi `androidx.room3` (3.0.3). Room 3 + KSP 2.3.12 +
  `sqlite-bundled` 2.7.1, Kotlin 2.4.20 / AGP 9.4.1 ile derleniyor ve telefonda çalışıyor; FTS5 var
  (SQLite 3.50.1). **FTS5 noktasız ı ile I'yı eşleştirmiyor:** dizine ve sorguya kendi Türkçe katlamamız
  uygulanacak (ı, I, İ → i). Açık soru 7'nin Room/KSP kısmı kapandı; Hilt hâlâ denenmedi.
- **Gemini ölçümleri (8 Ekim, tek çağrılar):** flash-lite ~1 sn; 3.8-flash varsayılan düşünmeyle
  ~8 sn, `thinkingLevel:"low"` ile ~2 sn; pro ~11 sn. Düşünme tokenleri çıktı fiyatından ücretlenir.
  Şemalı çıktı, işlev çağrısı, akış ve Google Arama temellendirmesi çalışıyor. Arama atıflarındaki
  `uri` yönlendirme adresidir (gerçek URL değil); alan adı `title`'da. Arama ayda 5 000 ücretsiz:
  Konu Motoru'nun arama maliyeti kaygısı (Bölüm 10, madde 5) büyük ölçüde kalktı.
- **Gemini kalanları (8 Ekim gece, tek koşular; `scripts/gemini-olc.mjs`):** basılı belge görselinden tarih
  ve tutar çıkarma hızlı kademede doğru (~3 sn, görsel ~1 100 token). Örtük önbellek kendiliğinden çalışıyor.
  **Arama ile şemalı çıktı aynı çağrıda kullanılınca kaynak listesi boş geliyor:** Konu Motoru iki çağrı yapar.
  Yönlendirme adresi tek `HEAD` ile gerçek adrese çözülüyor; yayın tarihi kaynakların yarısında okunabildi.
  40 eşzamanlı çağrıda 429 görülmedi.
- **Gemini:** uç nokta `generativelanguage.googleapis.com/v1beta`, başlık `x-goog-api-key`.
  Vertex AI projede kapalı (gerek yok). Bakiye **faturalandırma hesabına** bağlı; bakiye bitince
  her çağrı HTTP 402 döner, ücretsiz model yok. Ürün 402'yi `AiUnavailable` saymalı.

## 8. Yapılan hatalar ve çıkarılan dersler

Her satır gerçekten yaşandı. Aynı hatayı tekrarlamadan önce burayı oku.

| # | Ne oldu | Kök neden | Ders / kural |
|---|---|---|---|
| H1 | `local.properties`'e `C\:\Users\…` yazıldı; SDK yolu bozuldu | `.properties` biçiminde `\` kaçış karakteridir | Windows yollarını `.properties` dosyasına **ileri eğik çizgiyle** yaz (`C:/Users/…`). |
| H2 | Test betiği telefondaki kaydı okuyamadı, boşuna bekledi | Git Bash, `adb shell` argümanındaki `/data/…` yolunu `C:/Program Files/Git/data/…`'ya çeviriyor | `adb shell` içeren her betikte `export MSYS_NO_PATHCONV=1`. `kur.sh` dahil. |
| H3 | Çok satırlı bash komutları "unexpected EOF" ile hiç çalışmadı (2 kez) | Araç kabuğu, heredoc içindeki kesme işaretini (`Kullanıcı'nın`) tırnak sanıyor | Türkçe metin ya da kesme işareti içeren dosyaları heredoc ile değil **dosya yazma aracıyla** oluştur. |
| H4 | Arka plan betiği durdurulamadı | Git Bash'te `pkill` yok | `ps -ef` ile PID bulup `kill`; ya da arka plan görevini araçla durdur. |
| H5 | Betik yanlış alarmları "beklenen alarm" saydı, test adımları birbirine girdi | Bekleme koşulu toplam `FIRED` sayısına bakıyordu; önceki turdan kalan alarmlar da sayıldı | Bekleme koşulunu **belirli anahtara** bağla; teste başlamadan bekleyen alarm olmadığını doğrula. |
| H6 | Ölçüm kaydı kirlendi: benim kurmadığım alarmlar, silinmiş günlük | Test sırasında Kullanıcı deneme uygulamasının düğmelerine bastı; ben önceden "dokunma" demedim | Cihaz testinden **önce** Kullanıcı'ya neye dokunmayacağını ve ne kadar süreceğini yaz. Kirli veriyi sonuç sayma. |
| H7 | "Kilitli yeniden başlatma" testi geçti sanıldı; aslında kilitli durum hiç oluşmamıştı | Telefonda ekran kilidi yoktu (`CredentialType: NONE`); ön koşul doğrulanmadan teste girildi | Her testte **ön koşulu komutla doğrula** ve sonucu yorumlamadan önce kanıta bak (`BOOT_COMPLETED`'in hemen gelmesi ipucuydu). Kullanıcı "geldi" dese de kaydı oku. |
| H8 | Kilitliyken kayıt okunamadı | `run-as`, kimlik korumalı dizin kilitliyken çalışmaz | Kilitli dönemde `logcat -s ETIKET` kullan; ürün tanılaması kilitli dönem kayıtlarını cihaz korumalı depolamaya yazmalı. |
| H9 | İlk kurulum denemeleri reddedildi | HyperOS onay penceresi; bir kez de Kullanıcı yanlışlıkla reddetti | Kurulumdan önce Kullanıcı'yı uyar; `Success` görmeden devam etme; betikte bu hatayı yakala. |
| H10 | Gemini bakiyesi yüklendi denildi, çağrılar yine 402 | Bakiye başka Google hesabına yüklenmişti | "Yaptım" bildirimini çağrıyla doğrula; bakiyenin **hangi projeye/hesaba** bağlı olduğunu adımda açıkça yaz. |
| H11 | API anahtarı sohbete açık yazıldı | Kullanıcı güvenlik sonucunu bilmiyordu | Anahtar yalnız gitignore'daki `secrets.properties`'te; depoya girmediği her commit öncesi `git grep` ile doğrulanır. **Depo GitHub'a taşınmadan anahtar yenilenecek** (yol haritası F2.18). |
| H12 | Karar 0003'te ısrarlı takip için alarm yolu ölçümden önce seçildi | Varsayım, spike'tan önce yazıldı | Platforma bağlı seçimi karar kaydına **ölçümden sonra** yaz; öncesinde "aday" de. |
| H13 | Yol haritasının ilk hâlinde M16, M23, M24 etiketi yoktu | İçerik vardı, izlenebilirlik etiketi unutuldu | Kapsam belgesi yazınca M1–M30'u komutla tara; artık `dogrula/kontrol.mjs` her koşuda denetliyor. |
| H14 | Kancaları sınayan komut, kancanın kendisi tarafından engellendi | Sınama girdisi ("gizli dosyayı ekrana bas" örneği) komut metninin içindeydi; kanca yeni yazılır yazılmaz devreye girdi | Kanca sınama girdilerini komut satırına değil **betik dosyasına** koy (`.claude/hooks/sinama.sh`). Kancalar aynı oturumda hemen etkin olur. |
| H16 | Betik dosyası `/tmp/…` yoluyla Node'a verildi, "modül bulunamadı" hatası alındı | `MSYS_NO_PATHCONV=1` artık oturum genelinde açık; Git Bash yolları Windows programlarına **çevrilmeden** gidiyor (H2'nin çözümünün yan etkisi) | Windows programına (node, java, adb.exe) yol verirken göreli yol kullan ya da `cygpath -w` ile çevir. |
| H17 | Planlayıcıdaki bir karşılaştırma hiçbir testle korunmuyordu; ilk gece kontrolünde kasıtlı bozmayla ortaya çıktı | Testler "geçiyor" diye yeterli sayıldı; yakalayıp yakalamadıkları sınanmamıştı | İş kuralı değişen her gün gece kontrolünde kasıtlı bozma yapılır; yakalanmayan bozma = eksik test ya da gereksiz kod. |
| H18 | Kullanılmayan sabitler ve geçersiz girdiyi kabul eden veri sınıfları ilk yazımda fark edilmedi | "İleride lazım olur" eklemesi; kurucu denetimi düşünülmedi | Kullanılmayan şey eklenmez (ait olduğu fazda gelir); dışarıdan değer alan her model geçersiz değeri kurucuda reddeder. |
| H19 | Kullanıcı "GitHub'a pushla" dedi; depo **herkese açıktı** ve belgelerde sağlıkla ilgili kişisel bilgiler vardı | Depo açılırken görünürlük seçilmemiş; Kullanıcı sonucunu bilmiyordu | Dışarıya yayın öncesi hedefin görünürlüğünü komutla doğrula (kimliksiz istek 200 = açık, 404 = gizli) ve neyin görüneceğini Kullanıcı'ya söyle. Push'tan önce Kullanıcı depoyu gizli yaptı. |
| H20 | Gizli yapılan depoya push "Repository not found" verdi | Bilgisayarda kayıtlı GitHub girişi başka hesaptı (Emire221); gizli depoyu göremiyordu. Depo açıkken `ls-remote` çalıştığı için fark edilmemişti | Push öncesi kayıtlı hesabı kontrol et (`git credential-manager github list`). Çözüm: uzak adrese kullanıcı adı eklendi; Kullanıcı doğru hesapla giriş yaptı. |
| H21 | Cihaz betiğinin ilk komutları "no devices" verdi; kayıt başlangıç satırı 0 okundu ve bütün günlük ekrana döküldü | adb arka plan süreci yeniden başlarken ilk birkaç komut cihazı görmüyor; betik bağlantıyı beklemeden ölçüm başlangıcını aldı | Cihaz betiğinin ilk satırı `./scripts/adb wait-for-device`; başlangıç değeri okunamazsa (boş ya da 0) betik dursun. Ölçüm bu kez etkilenmedi. |
| H22 | Model ölçüm betiği 15 dakika boşuna bekledi; Kullanıcı "neden bitmedi" diye sordu | Telefonda süreç bellek yetersizliğinden öldürülmüştü; betik yalnız "bitti" satırını bekliyor, sürecin yaşadığına bakmıyordu | Cihazda uzun iş bekleyen her betik sürecin yaşadığını da denetler (`pidof`); öldüyse nedeni günlükten okuyup durur. `spike-llm-olc.sh` düzeltildi. |
| H23 | İki model (Qwen3, Phi-4 mini) yetersiz kanıtla "elendi" yazıldı ve dosyaları silindi; Kullanıcı itiraz edince yeniden indirilip başka yolla koşuldu. Qwen3 aslında çalışıyor ve aday kaldı | "Çalışmadı" ile "bu yapılandırmada çalışmadı" ayrılmadı; Phi'nin çıktıları okunmadan hüküm verildi; silme Kullanıcı'ya sorulmadı | Bir seçeneği elemeden önce: en az bir alternatif yol dene, çıktıyı gerçekten oku, eleme gerekçesini kanıtıyla yaz. İndirilen/üretilen şeyi Kullanıcı kararı gelmeden silme. |
| H24 | Kullanıcı'ya "telefon üç kez çalacak" denildi, hiç çalmadı; kayıt okununca fark edildi | Önceki adımda açılan ayar sayfası deneme uygulamasının görevinin üstünde kalmıştı; `am start` "görev öne getirildi" deyip intent'i **teslim etmedi**. Komutun uyarı satırı okunmadan Kullanıcı'ya haber verildi | Cihaza komut gönderip Kullanıcı'dan gözlem istemeden önce kaydın düştüğünü doğrula. Spike'ı tetiklerken `am start --activity-clear-top` kullan; "Warning: Activity not started" satırını hata say. |
| H25 | Kullanıcı'ya verilen `bash scripts/kur.sh …` komutu iki kez "Telefon bağlı değil" deyip durdu | Kullanıcı'nın PowerShell'inde `bash`, Git Bash değil Windows'un kendi bash'i (WSL); `ANDROID_HOME` ve `LOCALAPPDATA` orada boş. Betik yalnız benim oturumumun ortamında denenmişti | Kullanıcı'nın çalıştıracağı komutu **onun ortamında** düşün: Git Bash'i tam yoluyla çağır (`& "C:\Program Files\Git\bin\bash.exe" …`) ve vermeden önce `powershell -Command` ile sına. `scripts/adb` artık SDK'yı birkaç yerde arıyor. |
| H26 | `kur.sh`'ın ilk hâlinde üç doğrulama yanlış beklentiyle yazıldı (kova `10`, Rahatsız Etme için ayar anahtarı, manifestte olmayan `appops`) | Geri okuma komutlarının bu telefonda ne döndürdüğüne bakılmadan beklenti yazıldı | Doğrulama adımı yazmadan önce geri okuma komutunu bir kez elle koş ve gerçek çıktıyı gör. |
| H27 | Çıraklık ölçümü telefonda "koşuyor" sanıldı, Kullanıcı'ya öyle söylendi; 5 dakika sonra kayıtta başlangıç satırı olmadığı görüldü | Telefon kilitliydi: `am start` ile gönderilen intent, Activity kilit ekranının arkasında durduğu için teslim edilmedi. Betik başlangıcı doğrulamadan bekliyordu | Uzun cihaz işini başlattıktan sonra ilk 30 saniyede başlangıç kaydını (`LLM_INIT` vb.) doğrula; betik kilit ekranı açıksa başlamadan dursun (`spike-llm-olc.sh` düzeltildi). H24 ile aynı aile: "komut gönderildi" ≠ "iş başladı". |
| H28 | Planlayıcı, vakti geçmiş alarm kayıtlarını "planda yok" diye iptal listesine koyuyordu; kayıt silinince teslim denetçisi çalmamış teslimi bulamayacaktı (sessiz kayıp). Ara denetimde, iki parçanın birlikte nasıl çalışacağı düşünülürken bulundu | Planlayıcı ve denetçi ayrı ayrı test edilmişti; ikisinin aynı tabloyu paylaştığı akış test edilmemişti | Aynı veriyi paylaşan iki kuralın **birlikte** senaryosu da yazılır. Planlayıcı artık vakti geçmiş kayda dokunmaz (`toCancel` yalnız geleceği kapsar); kayıt teslim hattında (F2.27) ateşlenince silinir. |
| H29 | F2-B'nin ilk yazımında kullanılmayan DAO sorguları, bir özellik anahtarı ve bir yardımcı fonksiyon eklendi; uygulama verisi Android'in bulut yedeği ve cihaz aktarımına karşı yalnız eski `allowBackup` ile korunuyordu; günlük dosyası çağıran iş parçacığında yazılıyordu; gizli değer anahtarı eşzamanlı ilk kullanımda iki kez üretilebilirdi | H18'in tekrarı ("ileride lazım olur"); Lint uyarıları okunmadan geçildi | Kod yazıldıktan sonra Lint raporunun uyarılarını tek tek oku; kullanılmayan her genel üyeyi sil. Dördü de düzeltildi (`data_extraction_rules.xml`, arka plan günlük sırası, `@Synchronized`). |
| H30 | F2.19–F2.23'ün ilk CI koşusu kırmızı: `:ui:checkDebugAarMetadata`, Compose 1.12 (BOM 2026.09.00) compileSdk 37 istiyor | En yeni BOM sürümü sayfadan doğrulandı ama **gereksinimi** (compileSdk) okunmadı; bulut oturumunda Android derlemesi yapılamadığı için ilk sınama CI oldu | Yeni androidx sürümü seçerken sürümün yanında en düşük `compileSdk` gereksinimini de oku. Proje SDK 36'da kaldıkça Compose 1.11.x (BOM 2026.06.01). SDK 37'ye geçiş ayrı karar ister (bilgisayara SDK 37 kurulumu gerekir). |
| H31 | Ekran görüntüsü testleri önce Robolectric'in iç hatasıyla düştü, sonra 14+ dk takıldı, sonra derleme betiği bir yazım hatasıyla kırıldı (CI koşu 5–7) | (1) Robolectric'in JDK 17+ `--add-opens` gereksinimi okunmadan kuruldu; (2) galeride bitmeyen animasyon (yükleniyor çubuğu) vardı, Compose hiç durulmadı ve çekim sonsuza dek bekledi; (3) `build.gradle.kts`'te `java.time` adı Android'in `java {}` uzantısıyla çakıştı. Üçü de bulut oturumunda yerel derleme olmadığı için ancak CI'da görüldü | Robolectric kurulumunda resmi `--add-opens` listesi baştan eklenir. Ekran görüntüsü testine bitmeyen animasyon girmez; test görevine süre sınırı konur (10 dk). Gradle betiğinde JDK sınıfları tam adla değil `import` ile kullanılır. |
| H32 | Güneş sekme ikonu düzeltmesi ktlint hatasıyla commit'lendi; CI koşu 17 kırmızı | ktlint ile commit aynı komutta `;` ile zincirlenmişti; ktlint düşse de commit atıldı | Denetim ile commit'i `&&` ile bağla ya da çıkış kodunu okumadan commit atma. |
| H33 | Gece kontrolünde iki erişilebilirlik kusuru bulundu: (1) "Geri al" şeridindeki metin eylemi açık temada 4,19–4,30:1 (sınır 4,5); (2) durum çubuğu simgeleri telefonun temasına göre çiziliyordu, uygulama varsayılan koyu açıldığı için açık temalı telefonda saat ve pil görünmez olurdu | (1) Kontrast testi yalnız jeton çiftlerini ölçüyordu; bileşenin gerçekten çizdiği çift (yarı saydam zemin bindirilmiş hâlde, `surfaceVariant` üstünde) testte yoktu. Ekran görüntüsünde gözle fark edilmedi. (2) `enableEdgeToEdge()` varsayılanının neye göre karar verdiği okunmadı | Kontrast testi **bileşenin çizdiği** çifti ölçer: saydam renk bindirilmiş hâliyle. Yeni bileşen yazılınca metin/zemin çiftleri `ContrastTest`'e eklenir. Uygulama kendi temasını seçiyorsa sistem çubuğu stili de açıkça ondan verilir. |
| H34 | Tam ekran kart kilit ekranında açılmadı, iki denemede şerit geldi | İki ayrı neden üst üste: (1) aynı kimlikli bildirimi tam ekran niyetiyle **güncelliyordum**; sistem niyeti yalnız bildirim ilk eklenirken başlatır. (2) Kullanıcı telefona bakıyordu; ekran açıkken sistem zaten şerit gösterir. F1 spike'ında bildirim her seferinde yeniydi ve ekran kapalıydı, bu yüzden fark edilmemişti | Spike'ta ölçülen yüzey, üründe **aynı çağrı sırasıyla** kullanılmıyorsa yeniden ölçülür. Nedeni tahmin etmek yerine Kullanıcı'yı durdurup sistem kaydından okumak (üçüncü denemede yapıldı) ilk denemede yapılmalıydı. |
| H35 | Cihazda ilk hatırlatma "kritik" yerine "normal" kuruldu; Kullanıcı yanlış sesi duydu | `adb shell am broadcast … --es body "Yola çıkma vakti."`: boşluklu değer uzak kabukta bölündü, sonraki `--es klass` okunmadı. Komutun sonucu doğrulanmadan Kullanıcı'ya "kritik gelecek" dendi (H24 ailesi) | `adb shell`'e boşluklu değer verirken komutun tamamını tek tırnak içinde gönder (`adb shell "am … --es body 'iki kelime'"`). Kullanıcı'dan gözlem istemeden önce kurulan şeyi kayıttan oku. |
| H36 | Kullanıcı'ya merdivenin 2. ve 5. dakikası beklettirildi; Kullanıcı açıkça "bir daha asla" dedi | Önceki "15–20 sn" uyarısı yalnız başlangıç gecikmesine uygulanmıştı; ürünün kendi dakikalık adımları için beklemesiz yol hazırlanmamıştı | Kullanıcı'lı denemede süre saniyeyle. Dakikalık ürün davranışı debug tetikleyicisinin `advance` komutuyla simüle edilir; gerçek zamanlama Kullanıcı'sız, kayıttan ölçülür. |
| H37 | Kilitli yeniden başlatmadan sonra bir hatırlatma kilit açıldığında gelmedi; 86 sn sonra, tesadüfen başka bir eylemle geldi. O eylem olmasa 12 saat bekleyecekti | Denetçinin 60 sn'lik toleransı ("sistem teslim ediyordur") kilit açılma anına denk geldi ve motor tolerans dolunca yeniden bakmıyordu. Birim testlerinde hep "20 dakika geç" gibi açık örnekler vardı; sınırın hemen altı sınanmamıştı | Eşik içeren her kuralda eşiğin **hemen altı** da test edilir ve "şimdi karar veremiyorum" durumunun ardından kimin, ne zaman yeniden bakacağı yazılır. Düzeltme: kilitliyken çalanlar not edilip kilit açılınca hemen gösterilir; tolerans içindeki kayıt için bakım tolerans sonuna kurulur. |
| H15 | Yol haritası birkaç kez `sed`/`awk` ile değiştirildi | Alışkanlık; o sırada kanca yoktu | Kabuk komutuyla yapılan değişiklik düzenleme kancalarından (faz kapısı, gizli değer) geçmez. Yol haritası ve proje beyni **yalnız düzenleme aracıyla** değiştirilir. |

## 9. Tuzaklar ve "bunu bilmeden başlama" notları

- **Derleme:** sistemde `JAVA_HOME` tanımsız; Claude Code oturumlarında `.claude/settings.json`
  verir. Başka ortamda: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
  `adb` PATH'te değil: `./scripts/adb …` kullan.
- **Kancalar etkin:** blueprint ve arşiv düzenlenemez; gizli değer kalıbı içeren içerik yazılamaz
  ve commit'lenemez; `secrets.properties` ekrana basılamaz (değişkene alınır); `git push` sorulur;
  proje beyninde kapanış kaydı olmayan faz ☑ yapılamaz; Kotlin kural ihlali düzenleme sonrası
  bildirilir. Bir işlem "hook" hatasıyla durursa bu bir arıza değil, kuraldır: nedenini oku.
- **Kanca ve ayar değişikliği** sonrası `bash .claude/hooks/sinama.sh`; depo tutarlılığı için
  `node .claude/skills/dogrula/kontrol.mjs`.
- **AGP 9:** Kotlin yerleşiktir; Android modüllerine `kotlin-android` eklentisi **eklenmez**.
  Yalnız saf JVM modülü (`:domain`) `kotlin("jvm")` kullanır. AGP 9.4, Gradle 9.6.0 ister.
- **AGP 9 kaynak kümesi tuzağı:** `sourceSets["androidTest"].assets.srcDir(…)` ve `getByName(…) { }` AGP 9.4'te
  sınıf dönüşüm hatası veriyor. Doğrusu: `androidComponents { onVariants { it.androidTest?.sources?.assets?.addStaticSourceDirectory(…) } }`.
- **Room'u kullanan modül** `room3-runtime`'ı `api` ile açmalı; yoksa Hilt'in ürettiği kod `:app`'te derlenmez.
- **Biçim:** `ktlintFormat` varsayılan ayarla sondaki virgülleri siler ve içe aktarma sırasını değiştirir;
  `.editorconfig` mevcut biçimi koruyacak şekilde ayarlı. Biçim aracını koşmadan önce commit'le.
- **İmza anahtarı** `keystore/toparla-release.jks`, parolası `keystore.properties` içinde; ikisi de depoya girmez.
  **Bu iki dosya kaybolursa uygulama verisi korunarak güncellenemez.** Kullanıcı'nın iki ayrı yere yedeklemesi
  gerekiyor (9 Ekim itibarıyla yapılmadı; yol haritası F2.16).
- **Cihaz testleri** telefonda onay penceresi açar (test paketi kurulur); Kullanıcı başında olmalı.
- **Android Studio 2025.2.2**, AGP 9.4 projesini açmak için eski olabilir (doğrulanmadı).
  Derleme komut satırından yapılır; Kullanıcı Studio kullanmıyor.
- **Blueprint'te geçen ama geçersiz olanlar:** Claude/Anthropic (→ Gemini, karar 0001);
  yasak ifade listesi ve `forbidden_tr.json` (→ yok, karar 0002); bildirim ≤ 8 / gözlem ≤ 4
  (→ 10, karar 0003); D16'daki "hava açık" (hava verisi yok); "v3 §11'deki şemalar korunur"
  (v3'te o şemalar yok; biz tasarlayacağız).
- **`:domain` kuralları:** Android sınıfı yok; `System.currentTimeMillis()` / `Instant.now()` yok
  (zaman parametre ya da `Clock` ile gelir); `!!` yok. `:spike` bu kurallardan bilerek muaftır.
- **Git:** ~~her şey yerelde, hiç push yapılmadı~~ → 8 Ekim 2026'da ilk push yapıldı. Uzak depo
  `mehmetark444-art/toparla`, **gizli**. Bu bilgisayarın varsayılan GitHub girişi başka bir hesap
  (Emire221); bu yüzden uzak adres kullanıcı adıyla tanımlı
  (`https://mehmetark444-art@github.com/…`). Adresi sadeleştirme: push "depo bulunamadı" verir.
  Push yalnız Kullanıcı isteyince (kanca her seferinde sorar). ~~GitHub Actions henüz kurulmadı~~ →
  9 Ekim'de `check.yml` iş akışı kuruldu ve push sonrası ilk koşu yeşil çıktı (Kullanıcı gözlemi).
  API anahtarı henüz yenilenmedi (depoda anahtar yok, geçmiş tarandı).
- **Depo gizli kalmalı:** belgeler Kullanıcı'nın sağlıkla ilgili kişisel bilgilerini içeriyor.
  Görünürlüğü değiştirmeden ya da başka bir yere yayınlamadan önce Kullanıcı'ya açıkça sor.
- **Telefonun ekran kilidi** 7 Ekim 22:08'de kaldırılmış, sonra PIN olarak geri kondu.
  Direct Boot'a bağlı her test öncesi `dumpsys lock_settings | grep CredentialType` ile doğrula.
- **Kullanıcı'nın "oldu / geldi / yükledim" demesi kanıt değildir;** her seferinde kayıttan doğrula
  (H7 ve H10 bu yüzden yakalandı).

## 10. Açık sorular ve riskler

1. `setExactAndAllowWhileIdle` gecikmesinin kök nedeni (kova mı, pil politikası mı, HyperOS mu)?
2. Gerçek Doze'da ve uzun ufukta (1–8 sa) üç alarm yolunun davranışı → gece testi.
   (kısmen kapandı, 8 Ekim: 24/24 çaldı, karar 0006; ~~**derin Doze hâlâ açık**: kablosuz gece testi kuruldu,
   sonucu okunmadı.~~ → kapandı 9 Ekim: kablosuz gece 21/21; kesin yollar ≤ 2,1 sn. Sınır: `idle=true`
   teslim anında yakalanmadı; hafif uyku 4 kez görüldü. Pil muafiyetsiz kablosuz gece ölçülmedi → F2.44.)
3. ~~Erişilebilirlikle uygulama açılışı algılama ≤ 400 ms tutacak mı; HyperOS servisi öldürüyor mu?~~
   kapandı (8 Ekim): ~2,9 sn, hedef tutmuyor, karar 0008 ile ≤ 3,5 sn kabul edildi; servis süreç ölünce
   kendiliğinden dönmüyor. Gecikmenin nedeni bilinmiyor (karar 0014 ile F5.7'ye devredildi).
4. ~~Cihaz içi model: LiteRT-LM + Gemma'nın güncel adları, Türkçe kalitesi, model dosyasının
   nasıl edinileceği (sunucumuz yok; Kullanıcı elle indirecek).~~ kapandı (8 Ekim): kararlar 0010 ve 0011.
   Kullanıcı'nın modeli telefona nasıl alacağı (3,7 GB) ürün akışı olarak hâlâ tasarlanmadı (F6.15).
5. ~~Konu Motoru maliyeti: 5 konu × günde 2 tarama aylık konu payına sığacak mı?~~ kapandı (8 Ekim):
   konu başına ayda ~0,07–0,11 $.
6. Israrlı takip + bütçe 10, blueprint'in "bildirim yorgunluğu → uygulamayı bırakma" riskini
   büyütüyor. Haftalık gözden geçirmede "Yaptım ile bitme oranı" izlenecek.
7. ~~KSP / Room / Hilt'in Kotlin 2.4.20 + AGP 9.4.1 ile uyumu henüz denenmedi.~~ kapandı (9 Ekim): üçü de
   derleniyor ve cihazda çalışıyor.
8. Kriz sözlüğü Kullanıcı incelemesi olmadan sürüme giremez.
9. (9 Ekim) **İmza anahtarının yedeği yok**: tek kopya bu bilgisayarda. Kaybolursa veriyi koruyarak güncelleme
   imkânı biter. Kullanıcı erteledi; gerçek veriyle kullanım (F2 K2) başlamadan önce yapılmalı.
10. (9 Ekim) API anahtarı hâlâ sohbete açık yazılmış eski anahtar; depo gizli olduğu için acil değil ama
    Güneş'in bulut katmanı (F6) açılmadan yenilenmeli.
11. (9 Ekim) `release` sürümü cihazda hiç çalıştırılmadı (ekran yok): R8'in Hilt/Room üretilmiş kodunu
    bozmadığı ilk ekranla (F2.24) doğrulanacak. (9 Ekim akşamı: ilk ekran yazıldı ama bulutta yazıldığı için
    telefona kurulmadı; doğrulama telefon bağlanınca.) → kapandı (9 Ekim gece): bulut dalı ana dala alındı,
    bu bilgisayarda derlendi (114 JVM testi, ktlint, detekt, Lint, debug + imzalı release), iki sürüm telefona
    kuruldu; `release` açıldı, çökmedi, 5 sekmeli iskelet ve koyu tema ekranda (ekran görüntüsüyle görüldü).
13. (9 Ekim) Bulut oturumunda yazılan işin tek doğrulaması GitHub Actions: Robolectric ekran görüntüsü gerçek
    telefondaki çizimin aynısı değildir; F2-C'nin telefonda gözle denenmesi (yazı ölçeği, HyperOS gezinme
    çubuğu, öngörülü geri) hâlâ yapılmadı.
12. (9 Ekim) Süreç çökünce erişilebilirlik servisi kendiliğinden dönmüyor: müdahale ekranının (M25-I)
    güvenilirliği Sağlık uyarısına ve Kullanıcı'nın elle kapat-açmasına bağlı.

## 11. Yeni oturum için hızlı başlangıç

1. Bu dosyayı oku (10 dk), sonra `docs/yol-haritasi.md`'nin en üstünü ("Şu an", "Sıradaki tek adım").
2. `git log --oneline -15` ve `docs/progress.md`'nin son girişi.
3. İlgili blueprint bölümü + `docs/decisions/`.
4. Doğrula: `./gradlew :domain:test ktlintCheck detekt :app:lintDebug :app:assembleDebug` (9 Ekim itibarıyla
   114 JVM testi yeşil) ve `node .claude/skills/dogrula/kontrol.mjs`. Cihaz testleri (15):
   `./gradlew :data:connectedDebugAndroidTest` (telefonda onay ister). Arayüz: `./gradlew :ui:verifyRoborazziDebug`
   (temel görüntüler `ui/src/test/screenshots/`). Bulut oturumunda Android derlenemez: doğrulama GitHub Actions'ta.
5. Telefon gerekiyorsa: `adb devices` → `device` görünmeli; kilit durumu ve "USB ile yükle" açık.

## 12. Güncelleme kuralı (zorunlu)

**Ne zaman:**
- **Her faz kapanışında** (zorunlu kapı): faz ☑ işaretlenmeden önce.
- Her yeni karar kaydında → Bölüm 5.
- Her yapılan hatada, yanlış varsayımda, geri alınan işte → Bölüm 8 (aynı gün).
- Her cihaz bulgusunda → Bölüm 7 (özet; ham veri `platform-bulgulari.md`'ye).
- Mimari, modül, sürüm ya da çalışma biçimi değiştiğinde → Bölüm 4 ve 9.

**Faz kapanışında yapılacaklar (kontrol listesi):**
1. Üstteki "Son güncelleme", "Kapsadığı son commit", "Kapanan son faz".
2. Bölüm 3 (şu anki durum) yeniden yazılır.
3. Bölüm 4: yeni modüller, sınıflar, sürümler.
4. Bölüm 5: fazda alınan kararlar; kapanan "bekleyen karar adayları".
5. Bölüm 6: fazın zaman çizelgesi girişi (ne yapıldı, hangi commit'ler).
6. Bölüm 7–9: yeni bulgular, **fazda yapılan her hata**, yeni tuzaklar.
7. Bölüm 10: kapanan sorular silinmez, "kapandı: …" diye işaretlenir; yenileri eklenir.
8. Bölüm 13'e faz kapanış kaydı eklenir.

**Yazım kuralı:** Geçmiş silinmez, üstüne yazılmaz; yanlış çıkan bilgi "~~eski~~ → yeni (tarih)"
biçiminde düzeltilir. Hatalar kimseyi suçlamak için değil, tekrarlanmasın diye yazılır.
API anahtarı, parola ve kişisel sağlık verisi bu dosyaya **asla** girmez.

## 13. Faz kapanış kayıtları

| Faz | Kapanış | Son commit | Beyin güncellendi | Not |
|---|---|---|---|---|
| F0 | 7 Ekim 2026 | `8a0d633` | ☑ (ilk sürüm) | K1 ve K2 geçti. |
| F1 | 9 Ekim 2026 | `739bd3f` (kapanış commit'i bunun ardından) | ☑ | K1: 19 spike'ın 17'si ölçüldü, her bulguda yöntem, ölçüm sayısı ve sınırlar yazılı; 2 spike (konum, Mi Band) ve küçük açık koşullar karar 0014 ile adıyla F2/F3/F5/F6/F7 maddelerine devredildi. K2: Kullanıcı onayı 9 Ekim ("hepsini gördüm"). **Blueprint'in "tüm `[DOĞRULA]` kapandı" ölçütü tam karşılanmadı** (bilerek; karar 0014). Fazın kararları: 0006–0014. Fazın hataları: H1–H12 (alarm ve Gemini), H16, H19–H27. En önemli bulgular: kesin alarm yolları güvenilir (3 gece, 45 alarm, kaçan 0); esnek yol güvenilmez; otomatik başlatma izni zorunlu; erişilebilirlik servisi çökünce kendiliğinden dönmüyor; kilit ekranında yakalama ekransız; cihaz içi model Gemma 4 E4B. |
