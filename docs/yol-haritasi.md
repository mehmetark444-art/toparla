# Toparla · Güneş — Yol Haritası

Projenin sıfırdan bitişe tek durum kaynağı. Her oturumda buradan okunur, burada işaretlenir.
Kapsam `docs/BLUEPRINT.md` (M1–M30) + `docs/decisions/` kararlarıdır; bu dosya kapsam eklemez.

**Son güncelleme:** 8 Ekim 2026, gece (A grubu: ayar bağlantıları, `kur.sh`)

## Nasıl okunur

| İşaret | Anlam |
|---|---|
| ☑ | Bitti: kanıtı var (test, ölçüm ya da commit) |
| ◐ | Sürüyor: bir kısmı yapıldı |
| ☐ | Bekliyor |
| ⛔ | Engelli: yanında neyi beklediği yazar |

**İşaretleme kuralı:** Bir madde ancak kanıtı gösterilebiliyorsa ☑ olur. Kanıt madde
satırının sonuna yazılır (commit, test adı ya da `platform-bulgulari.md` başlığı).

**Çift kontrol:** Her fazın sonunda iki ayrı doğrulama vardır ve ikisi de geçmeden faz kapanmaz.
- **K1 — Makine kontrolü:** komutla tekrarlanabilir kanıt (test, derleme, ölçüm). Ben koşarım.
- **K2 — Gerçek dünya kontrolü:** telefonda, gerçek kullanımda gözlenen sonuç. Sen onaylarsın.

Biri geçip diğeri kalırsa faz "◐" kalır. K1 geçti diye K2 varsayılmaz; tersi de geçerli.

**Kapanış şartı (K3 — Proje beyni):** K1 ve K2 geçse bile, `docs/proje-beyni.md` o faz için
güncellenmeden (kararlar, bulgular, **yapılan hatalar**, zaman çizelgesi, faz kapanış kaydı)
faz ☑ işaretlenemez. Bu şart her fazın çift kontrol listesinde ayrı satır olarak durur.

## Genel durum

| Faz | Blueprint dilimi | Konu | Durum |
|---|---|---|---|
| F0 | — | Hazırlık ve temel | ☑ |
| F1 | S0 | Cihaz denemeleri (spike) | ◐ |
| F2 | S1 | İskelet + Hatırlatma motoru | ◐ |
| F3 | S2 | Günlük sürücü | ☐ |
| F4 | S3 | Ritim | ☐ |
| F5 | S4 | Alışkanlık ve bırakma | ☐ |
| F6 | S5 | Güneş çekirdeği (AI) | ☐ |
| F7 | S6 | Bağlam ve orkestratör | ☐ |
| F8 | S7 | Koçluk ve Dürüst Ayna | ☐ |
| F9 | S8 | Konu Motoru | ☐ |
| F10 | S9 | Dayanıklılık ve kapanış | ☐ |

**Şu an:** F1 (alarm yolu ve Gemini kademeleri kararlaştırıldı) ve F2-A (saf mantık) paralel.
**Sıradaki tek adım:** F1 "B grubu": F1.12 konum (geofence) → F1.13 Mi Band / Health Connect → F1.14 gürültüde ses tanıma. Ardından C grubu (F1.19 ALO 171, F1.16 kalanlar, F1.24, F1.25; F1.18 Room/KSP ☑). "A grubu" (F1.4–F1.6, F1.8–F1.11) 8 Ekim gecesi ölçüldü; her maddenin açık kalan koşulu kendi satırında.

**Açık engeller**
- Yok. (Gemini bakiyesi 8 Ekim'de geldi; çağrıyla doğrulandı.)
---

## F0 — Hazırlık ve temel ☑

**Amaç:** Ne yapılacağını netleştirmek, depoyu ve çalışma düzenini kurmak.

- ☑ F0.1 Blueprint v4 ve v3 baştan sona okundu, boşluk ve çelişkiler çıkarıldı
- ☑ F0.2 Netleştirme turu 2: Kullanıcı kararları alındı (kararlar 0001–0005)
- ☑ F0.3 Ortam kontrolü: Android Studio, SDK 36, adb, JBR 21 — `platform-bulgulari.md` § Geliştirme ortamı
- ☑ F0.4 Depo: `git init`, uzak adres, `.gitignore`, `.gitattributes` — `4ca872c`
- ☑ F0.5 `CLAUDE.md` (A6 + blueprint'i ezen kararlar) — `4ca872c`
- ☑ F0.6 Belge iskeleti: `progress`, `platform-bulgulari`, `hyperos-baglantilar`, `ideas`, `decisions/` — `4ca872c`
- ☑ F0.7 Gradle çok modüllü iskelet (7 modül), sürüm kataloğu resmi kaynaktan doğrulandı — `65c129f`
- ☑ F0.11 Gece kontrolü usulü (`/gece-kontrolu`, `kontrol.mjs --gece`) ve ilk tam kontrol — `docs/gece-kontrolleri.md`
- ☑ F0.8 Yol haritası (bu dosya) — `0cf287b`
- ☑ F0.9 Proje beyni (`docs/proje-beyni.md`) ve zorunlu güncelleme kuralı (K3) — `81f1c19`
- ☑ F0.10 Claude Code altyapısı: ayarlar ve izinler, 4 kanca (17 sınama), 5 kural dosyası, 7 yetenek, 3 denetçi alt ajan, `AGENTS.md` — `docs/claude-code-duzeni.md`

**Çift kontrol**
- ☑ K1: `./gradlew :domain:test :app:assembleDebug` başarılı; depoda API anahtarı yok (`git grep`).
- ☑ K2: Kullanıcı kararları yazılı onayladı (ısrarlı takip sınırları dahil).
- ☑ K3: `proje-beyni.md` bu faz için güncellendi (Bölüm 13 kapanış kaydı var).

---

## F1 — Cihaz denemeleri (S0 spike) ◐

**Amaç:** Blueprint'teki her `[DOĞRULA]` / `[Spike]` varsayımını bu telefonda ölçmek. Hiçbir
platform davranışı varsayılmaz. Bulgular `platform-bulgulari.md`'ye yazılır.

### F1-A Alarm ve teslim (ürünü taşıyan varsayımlar)
- ◐ F1.1 Alarm teslimi: `setAlarmClock` ↔ `setExactAndAllowWhileIdle` ↔ `setAndAllowWhileIdle`
  - ☑ Ekran açık / kapalı (2 dk)
  - ☑ Son uygulamalardan kaydırıp kapatma (tek deneme, 2 dk)
  - ☑ Yeniden başlatma
  - ☑ Kilitli yeniden başlatma (Direct Boot)
  - ☑ Gece: 1–8 saat, 3 yöntem, 24 alarm; 24/24 çaldı — `platform-bulgulari.md` § gece testi sonucu
  - ☐ Derin Doze (`idle=true`) altında teslim: gece testinde alarm anında hiç gözlenmedi
  - ◐ `setExactAndAllowWhileIdle` 3,5 dk gecikmesi: 8 gece ölçümünde tekrarlanmadı (en çok 28 sn); kök neden bilinmiyor
  - ☐ Kısıtlı bekleme kovası (uygulama günlerce açılmadan)
  - ☐ Güvenlik uygulaması "Bellek temizleme" sonrası teslim
  - ☐ Saat elle değişti / saat dilimi değişti → yeniden planlama
- ◐ F1.2 Kesin alarmdan foreground service başlatma; `specialUse` FGS; her başlatma yolu — `setAlarmClock` yolu ☑ (tek deneme, `platform-bulgulari.md` § Spike 2 ve 3); diğer yollar ☐
- ◐ F1.3 Tam ekran bildirim + HyperOS "Kilit ekranında göster", "Arka planda açılır pencere" — kilitliyken ☑ (tek deneme, özel izin verilmeden); ekran açıkken davranış ☐
- ◐ F1.4 Bildirim "cooldown" davranışı, alarm ses akışı, DND aşımı (erişim verilmişken ☑: art arda 3 bildirim kısılmadı, Rahatsız Etme açıkken 3/3 çaldı — `platform-bulgulari.md` § Spike 7; erişim verilmemişken, tam sessizlik ve ekran kapalıyken ☐)
- ◐ F1.5 HyperOS ayar derin bağlantıları → `hyperos-baglantilar.md` (11 bağlantıdan 10'u açıldı, eski pil kısıtı sayfası yok — `platform-bulgulari.md` § Spike 4; gri tonlama açılışı ve sayfaların doğru uygulamayı gösterdiğinin göz doğrulaması ☐)
- ◐ F1.6 `kur.sh`: her komutun HyperOS'teki davranışı; `USER_RESTRICTED` ve yol çevirme hatası ele alınmış (betik her adımı geri okuyarak doğruluyor, telefonda bir kez koşuldu — `platform-bulgulari.md` § Spike 19: izinler ve tam ekran tuttu, kullanım istatistikleri ve üstte gösterme `appops` ile **tutmadı**; APK kurulum adımı ve temiz kurulumda tam koşu ☐ → F2.17)

### F1-B Algılama ve sistem yüzeyleri
- ◐ F1.7 (`platform-bulgulari.md` § Spike 5: kaydırınca servis ölüyor ve geri gelmiyor; açılışta olay ~2,9 sn geç geliyor, hedef tutmuyor; servisten Activity başlatma ☑; son uygulamalarda kilit "tümünü temizle"den koruyor ☑ (tek deneme); yeniden başlatma sonrası servis kendiliğinden bağlanıyor ☑ (tek deneme); kilidin yeniden başlatma ve güncelleme sonrası kalması, uzun süre ömrü, gecikmenin nedeni ☐) Erişilebilirlik: açılış algılama gecikmesi (hedef ≤ 400 ms), servis ömrü, servisten Activity başlatma; olmazsa overlay yedeği
- ◐ F1.8 Bildirim erişimi: kısıtlı ayarlar, `allow_listener`, kopma sonrası `requestRebind` (elle açma engelsiz ☑, bildirim okuma ve kod süzgeci ☑ — `platform-bulgulari.md` § Spike 6; `allow_listener` ve kopma sonrası yeniden bağlanma ☐)
- ☑ F1.9 Tile: kilitliyken yakalama, Tile → mikrofon ≤ 1 sn — `platform-bulgulari.md` § Spike 8 (kilit açık 287 ms, kilitli 245 ms; kilitliyken ekran açılmıyor, yakalama servisten ekransız yapılıyor; koşul başına 1–2 ölçüm)
- ☑ F1.10 Canlı güncelleme: `ProgressStyle`, "promoted ongoing", HyperOS odak bildirimi — `platform-bulgulari.md` § Spike 16 (tek deneme; hap, panel ve kilit ekranında Kullanıcı gördü)
- ◐ F1.11 Arama durumu: `AudioManager.getMode` ile izinsiz algılama (giden hücresel arama ☑ — `platform-bulgulari.md` § Spike 17; gelen arama çalma anı, internet araması, arka plandan okuma ☐)
- ☐ F1.12 Geofence: Play Hizmetleri varlığı, arka plan olay gecikmesi
- ☐ F1.13 Health Connect: Mi Band → Mi Fitness → uyku/adım akışı

### F1-C Ses ve yapay zekâ
- ◐ F1.14 (sessiz ortam ☑: gerçek hata ~%6, karar 0013 — `platform-bulgulari.md` § Spike 9; gürültü, kulaklık, uzun konuşma ☐) Türkçe cihaz içi konuşma tanıma: 30 cümlelik set, WER; yetersizse yerel Whisper
- ◐ F1.15 (çalışma zamanı ve model edinme yolu ☑; beş adayın eleme turu ☑: Gemma 4 E2B, E4B ve Qwen3 4B kaldı; Phi-4 mini ve Ministral elendi — `platform-bulgulari.md` § Spike 10; 50 soruluk Türkçe set ☑ (E2B 46,5 · E4B 45,5 · Qwen3 32; karar adayı 0010); gömme modeli ve gerçek RAG ☑ (karar 0011); düşünme kipiyle karta dayalı yanıt, kablosuz pil, çok turlu sohbet, görsel/ses ☐) Cihaz içi model: LiteRT-LM + Gemma güncel adları, GPU/NPU, token/sn, ilk token, 10 dk sıcaklık, 50 örnek Türkçe kalite; model dosyasını edinme yolu
- ◐ F1.16 Gemini API — `platform-bulgulari.md` § Spike 11: Gemini API ölçümleri
  - ☑ Uç nokta: Developer API; model listesi alındı
  - ☑ Kademe modelleri (hızlı / günlük / derin) ve fiyatlar resmi sayfadan kilitlendi — karar 0007 (`pricing.json` F6.4'te yazılır)
  - ☑ Akış (SSE), şemalı çıktı, işlev çağrısı, düşünme ayarı (tek çağrı ölçümleri)
  - ☐ Görsel girdi, bağlam önbellekleme, 429 / hız sınırı davranışı
  - ◐ Google Arama temellendirmesi: atıf alanları ve fiyat ☑; yönlendirme adresinden gerçek URL ve tarih çözme, arama + şemalı çıktı birlikte ☐
- ◐ F1.17 Konu bütçesi ölçümü: 1 konu × günde 2 tarama gerçek maliyeti → varsayılan sıklık (fiyattan hesap: ücretsiz arama payına sığıyor, karar 0007; gerçek tarama ölçümü ☐)
- ☑ F1.18 Room + BundledSQLiteDriver ile FTS5; KSP'nin Kotlin 2.4 / AGP 9.4 ile uyumu — `platform-bulgulari.md` § Spike 13 (Room 3.0.3 + KSP 2.3.12 derlendi ve cihazda çalıştı; FTS5 var; noktasız ı için kendi Türkçe katlamamız gerekiyor; tek koşu)
- ☐ F1.19 ALO 171 hattının güncelliği
- ☑ F1.20 16 KB sayfa: cihaz 4096 kullanıyor; engel değil — `platform-bulgulari.md`

- ☐ F1.24 Ekran okuma ön ölçümü (karar 0008, aday): içerik yetkisiyle gecikme; ekran metnini toplama hızı, pil ve ısı; parola alanı ve hassas uygulama ayrımı. F1.15'e (cihaz içi model) bağlı.

- ☐ F1.25 Çıraklık ön ölçümü (karar 0012): Gemini'nin ürettiği örnekler isteme eklenince Gemma 4 E4B'nin 50 soruluk setteki puanı ve süresi değişiyor mu; ret → Gemini → kart → yerel yanıt zinciri uçtan uca.

### F1-D Kararlar
- ☑ F1.21 Karar kaydı: sınıf → alarm yolu (ısrarlı takip dahil) — karar 0006
- ☑ F1.22 Karar kaydı: Gemini model kademeleri ve aylık bütçe dağılımı — karar 0007
- ☑ F1.23 Karar kaydı: STT yolu (karar 0013) ve cihaz içi model seçimi (karar 0010: Gemma 4 E4B, tek model)

**Çift kontrol**
- ☐ K1: `platform-bulgulari.md`'deki 19 spike satırının hiçbiri "Bekliyor" değil; her birinde ölçüm sayısı ve yöntem yazılı; `[DOĞRULA]` araması açık madde bırakmıyor.
- ☐ K2: Gece testi sabah kaydı Kullanıcı'nın gördüğü bildirimlerle tutarlı; Kullanıcı tam ekran kartı kilit ekranında, müdahale ekranını gerçek bir uygulama açılışında kendi gözüyle gördü.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

**Faz kapısı:** F1-A tamamen ☑ olmadan F2-D (Android hatırlatma) yazılmaz.

---

## F2 — İskelet + Hatırlatma motoru (S1) ◐

**Amaç:** Ürünün omurgası. "Bu modül yanlışsa ürün çalışmaz." İlk yazılan, en çok test edilen parça.

### F2-A Hatırlatma mantığı (`:domain`, saf Kotlin)
- ☑ F2.1 `ReminderPlanner`: ana teslimler, 5 tekrar kuralı, 48 sa pencere, fark alma, 200 sınırı, yaz saati — `05ebe61`, 20 test
- ☑ F2.2 `Ladder`: sınıfa göre basamaklar; güvenilir kişi basamağı yalnız açıkken — `8a970bf`
- ☑ F2.3 `PersistentFollowUp`: ısrarlı takip zamanlaması (karar 0003) — `8a970bf`, 14 test
- ☑ F2.4 `OccurrenceStateMachine` + `SnoozePolicy` — `0d39466`, 17 test
- ☑ F2.5 Çekirdek arayüzler: `Clock`, `RandomSource`, `IdGenerator`, `AppError` — `0d39466`
- ☐ F2.6 Planlayıcıya merdiven ve ısrarlı takip teslimlerinin eklenmesi (alarm yolu F1.21'e bağlı)
- ☐ F2.7 Aynı dakikadaki olayların birleşik kartı; geç teslim kuralı (geçmiş `fireAt`)
- ☐ F2.8 Teslim denetçisi ve `CriticalWatchdog` kuralları (saf mantık)
- ☐ F2.9 Özellik tabanlı testler: Bölüm I invaryantlarının tamamı; 18 zorunlu sahte saatli senaryo

### F2-B Altyapı
- ☐ F2.10 Hilt, Coroutines, `DispatcherProvider`; üretim `Clock` / `IdGenerator` / `RandomSource`
- ☐ F2.11 Room + KSP: H2 tabloları (`Reminder`, `ReminderOccurrence`, `ScheduledAlarm`, `DeliveryLog`), şema dışa aktarımı, DAO testleri
- ☐ F2.12 Migration düzeni: `MigrationTestHelper`, migration öncesi DB kopyası ve geri dönüş
- ☐ F2.13 DataStore ayarları, `FeatureFlags`, `Defaults` / `AppConfig`
- ☐ F2.14 `SecretStore` (Keystore AES-GCM)
- ☐ F2.15 Kalite araçları: ktlint, detekt, Android Lint, StrictMode ve LeakCanary (dev), Timber döner günlük
- ☐ F2.16 Yapı varyantları: `debug` (.dev) / `release` (R8, imzalı); imza anahtarı üretimi ve **iki yerde yedeği** (Kullanıcı ile)
- ☐ F2.17 `scripts/kur.sh`, `log-cek.sh`; `versionCode = yyMMddNN`
- ◐ F2.18 Depo GitHub'a taşındı, Actions ile `check` koşuyor; API anahtarı yenilendi (karar 0005-1, 0001)
  - ☑ Depo gizli yapıldı ve ilk push tamamlandı (8 Ekim 2026; yerel = uzak `6bc3304`)
  - ☐ GitHub Actions ile `check`
  - ☐ API anahtarı yenilendi ve yalnız Gemini API'sine kısıtlandı

### F2-C Tasarım sistemi (`:ui`)
- ☐ F2.19 Renk jetonları: Koyu / Açık / AMOLED + 6 vurgu; `ExtendedColors`
- ☐ F2.20 Otomatik kontrast testi (her tema × vurgu × metin/zemin ≥ 4,5:1)
- ☐ F2.21 Tipografi, boşluk, şekil, hareket jetonları; "Animasyonları azalt"
- ☐ F2.22 Ortak bileşenler (ilk parti): `PrimaryButton`, `SecondaryButton`, `TextAction`, `Chip`, `UndoBar`, `EmptyState`, `CalmDialog`, `SettingRow`, `PermissionRow`, `SectionHeader`, `ProgressRing`
- ☐ F2.23 Roborazzi ekran görüntüsü testi düzeni (3 tema × 2 yazı ölçeği)
- ☐ F2.24 Uygulama ikonu (adaptif + monokrom), tek Activity, gezinme iskeleti, edge-to-edge, predictive back

### F2-D Hatırlatma motoru Android tarafı (`:reminders`, M6)
- ☐ F2.25 `ReminderScheduler` uygulaması: üç alarm yolu, idempotent `PendingIntent`
- ☐ F2.26 Bildirim kanalları (G5: 8 kanal) ve `Notifier`
- ☐ F2.27 `AlarmReceiver` → `ReminderService` (FGS) teslim hattı; çift teslim engeli
- ☐ F2.28 Bildirim eylemleri uygulama açılmadan: Yaptım · 10 dk sonra · Yarın · Bugün olmayacak
- ☐ F2.29 Merdiven yürütme; `ReminderFullScreenActivity` (kilit ekranı üstü)
- ☐ F2.30 Israrlı takip yürütme: 30 dk tekrar, birleşik bildirim, değişen metin, susturan durumlar
- ☐ F2.31 Yeniden planlama alıcıları: boot, locked boot, paket güncelleme, saat, saat dilimi, izin/DND
- ☐ F2.32 Direct Boot kopyası (cihaz korumalı depolama)
- ☐ F2.33 Güvenlik ağları: `DailyMaintenanceWorker`, `CriticalWatchdogWorker`, teslim denetçisi, `HeartbeatWorker`, 12 sa bakım alarmı
- ☐ F2.34 DND aşımı (izinliyse), alarm ses akışı

### F2-E Hatırlatma Sağlığı (D23)
- ☐ F2.35 Kontrol satırları (izin, kanal, tam ekran, pil, DND, kova, son teslim, bekleyen alarm, kaçan)
- ☐ F2.36 "Hatırlatmaları sına" (2 dk kendi kendini sınama) ve başarısızlık rehberi
- ☐ F2.37 HyperOS kurulum sihirbazı (derin bağlantılar, görsel açıklama)
- ☐ F2.38 Geçici hatırlatma ekleme ekranı (S2'ye kadar motoru gerçek kullanımla sınamak için)

### F2-F İlaç (M9, kapalı flag)
- ☐ F2.39 Tanım, kritik hatırlatma, Aldım / 15 dk sonra / Atlıyorum, çift doz koruması
- ☐ F2.40 Kaçan doz kaydı, stok, gizli bildirim metni, biyometrik kilit, CSV/PDF
- ☐ F2.41 Flag kapalıyken hiçbir ilaç yüzeyi görünmüyor, izin istenmiyor (test)

**Çift kontrol**
- ☐ K1: `:domain` ve `:reminders` testleri yeşil; 18 zorunlu senaryo ve invaryantlar geçti; cihaz matrisi (açık / arka plan / kapalı / Doze / yeniden başlatma / kilitli yeniden başlatma) her hücrede kritik teslim ±1 dk; lint temiz; migration testi var.
- ☐ K2: 7 gün gerçek kullanım: Kullanıcı'nın kurduğu hatırlatmalarda kaçan kritik 0, ±1 dk teslim ≥ %99 (teslim günlüğünden); ısrarlı takip en az 3 gerçek işte "Yaptım"a kadar sürdü; Kullanıcı "güveniyorum" dedi.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

**Faz kapısı:** K2'nin 7 günü beklenirken F3 flag arkasında yazılabilir (karar 0005-4).

---

## F3 — Günlük sürücü (S2) ☐

**Amaç:** Günü bu uygulamayla yürütmek (v0.1).

### F3-A Veri ve mantık
- ☐ F3.1 H1 tabloları: `Capture`, `Task`, `MicroStep`, `PostponeLog`, `WorryNote`, `RewardLog`, `DopamineItem`
- ☐ F3.2 `TrDateParser` (Türkçe doğal dil tarih) — altın set ≥ %95
- ☐ F3.3 Kural tabanlı sınıflayıcı (Görev / Randevu / Alışveriş / Fikir / Not / Hatırlatma / Endişe)
- ☐ F3.4 `NowSelector` (deterministik, ≥ 40 senaryo); 3 öncelik sınırı
- ☐ F3.5 Olay günlüğü ve genel "Geri al" altyapısı

### F3-B Yakala (M1, D3)
- ☐ F3.6 `SpeechInput` (cihaz içi Türkçe), sessizlikte bitiş, 500 ms taslak yazımı
- ☐ F3.7 `CaptureFab` (kısa = ses, uzun = yazı), ses ve yazı sheet'leri
- ☐ F3.8 Tile "Yakala" + `LockCaptureActivity` (kilitliyken yalnız yeni kayıt)
- ☐ F3.9 Widget Yakala 1×1; uzun basma kısayolları
- ☐ F3.10 Paylaş hedefi (metin, link, görüntü)

### F3-C Gelen (M2, D4)
- ☐ F3.11 Liste, işleme kartı, `SwipeCard` + düğme eşdeğerleri, chip'ler
- ☐ F3.12 5 dk ayıklama; Eski Çekmece; birleştirme önerisi; 48 sa nazik hatırlatma
- ☐ F3.13 Düşünme Defteri (endişe asla görev olmaz)

### F3-D Şimdi ve Plan (M3, D2, D5)
- ☐ F3.14 `NowCard`, 3 öncelik, durum şeridi, boş ve dinlenme durumları
- ☐ F3.15 Başla / Ertele (nedenli) / Değiştir / Bitti; uzun basma menüsü
- ☐ F3.16 Plan: Bugün şeridi · Hafta · Bir gün · Taşınan; görev detayı
- ☐ F3.17 Widget'lar: Şimdi 4×2, 3 Öncelik 4×2

### F3-E Zaman (M5)
- ☐ F3.18 Görsel zamanlayıcı, `FocusService` temeli, canlı güncelleme / sürekli bildirim
- ☐ F3.19 Geçiş uyarıları −30 / −15 / −5 / 0; takvim okuma (M19.1 temel)

### F3-F Ödül, Bunaldım, Kriz
- ☐ F3.20 M12: mikro-kutlama havuzları (30 gün tekrar yok), değişken sürpriz, dopamin menüsü, "Son 7 günde X"
- ☐ F3.21 M13: Bunaldım akışı (`BreathGuide`, tek soru, taşıma), Tile ve kısayol
- ☐ F3.22 Kriz ekranı (D13) + deterministik kriz sözlüğü (**Kullanıcı incelemesi zorunlu**), destek hatları
- ☐ F3.23 "Bugün sessiz" / "2 saat sessiz" (Tile dahil)
- ☐ F3.24 Alt çubuk (5 sekme), üst çubuk, Ayarlar iskeleti (Görünüm, Veri, Hatırlatma Sağlığı)

**Çift kontrol**
- ☐ K1: Tile → mikrofon ≤ 1 sn (ölçüm); 100 ardışık yakalamada 0 kayıp (test); `NowSelector` ≥ 40 senaryo; tarih altın seti ≥ %95; soğuk açılış ≤ 800 ms; her ekranda Compose testi ve 3 tema ekran görüntüsü; kriz sözlüğü testleri.
- ☐ K2: Senaryolar S2, S6, S9, S10 telefonda geçti; Kullanıcı 3 gün üst üste gününü yalnız bununla yürüttü; kriz sözlüğünü ve hat listesini okuyup onayladı.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

---

## F4 — Ritim (S3) ☐

**Amaç:** Sabah–akşam döngüsünün tamamı.

- ☐ F4.1 M4: mikro-adım şablonları (10 kategori × 3), engel diyaloğu, 2 dakika sözleşmesi, "Daha küçük yap"
- ☐ F4.2 M7: Odak oturumu, park, Yoldaş seviyeleri 0–3 (kural tabanlı), oturum sonu değerlendirme
- ☐ F4.3 Hiperfokus kesici (M5 ile); kritik + odak çakışması
- ☐ F4.4 M8: Rutin oynatıcı, kısa versiyon, tetikler (saat, şarj); hazır rutinler
- ☐ F4.5 M10: Çıkış kontrolü, listeler, takvim ve manuel tetik (konum/NFC F7'de)
- ☐ F4.6 M11: Check-in (enerji, ruh hali, istek düzeyi), enerji eğrisi
- ☐ F4.7 M21-8: Zaman Kalibratörü (kural)
- ☐ F4.8 M14: Gün Kapanışı (≤ 5 dokunuş), Haftalık Gözden Geçirme iskeleti
- ☐ F4.9 D16 Sabah Planı (kural tabanlı aday seçimi; AI F8'de eklenir)
- ☐ F4.10 Tasarımı blueprint'te olmayan ekranlar: Check-in, Çıkış Kontrolü (karar 0005-14)

**Çift kontrol**
- ☐ K1: Zamanlayıcı ekran kapalıyken ±1 sn; yarıda kapanan rutin ve odak kaldığı yerden; kalibratör birim testleri; kapanış ≤ 5 dokunuş (UI testi).
- ☐ K2: Senaryolar S1, S3, S4, S5, S8 geçti; Kullanıcı bir tam günü sabah planından gün kapanışına bununla yaşadı.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

---

## F5 — Alışkanlık ve bırakma (S4) ☐

**Amaç:** İlk odaklar: Sigara (bırakma) + Uyku Ritmi (karar 0004).

- ☐ F5.1 H3 ve H4 tabloları
- ☐ F5.2 `HabitWindowCalc` (esneme günü, MINI = DONE, UNKNOWN ≠ SLIP), `UrgeEngine`, `TriggerMapper`, `CostLedger`, `InterceptPolicy`
- ☐ F5.3 Şablonlar T1–T7 ve kurulum sihirbazları
- ☐ F5.4 Alışkanlık listesi ve detayı (Bugün · Harita · Plan · Bedel), `HabitStrip`
- ☐ F5.5 Dürtü Anı akışı (`UrgeWave`, şablon cümle havuzu ≥ 60/alışkanlık), Tile "Dürtü"
- ☐ F5.6 Kayma kaydı (D10), ardışık 3 gün → yeniden tasarım önerisi
- ☐ F5.7 M25-I: `AppOpenAccessibilityService`, `InterceptActivity`, göstermeme kuralları, süre uyarısı
- ☐ F5.8 Uyku Ritmi: yatış/kalkış hedefi, kademeli geçiş, Uyku hazırlığı rutini, gece müdahale sorusu
- ☐ F5.9 Bekleme listesi (`WishItem`), alışveriş müdahalesi
- ☐ F5.10 Widget Alışkanlıklar 4×1; Şimdi ekranında odak satırları
- ☐ F5.11 M27: Karar Defteri (söz, ölçüt, vade, "gazla verilen söz" koruması, Karar Kapısı)
- ☐ F5.12 M28: Gerçek Ben panosu (değerler, vizyon, mektuplar, nedenler, kanıtlar)

**Çift kontrol**
- ☐ K1: Göstermeme kurallarının her biri birim testli; müdahale algılama → ekran ≤ 400 ms (10 ölçüm); seri sayısı ve kırmızı kayma rengi hiçbir ekranda yok (UI testi); dürtü akışı uçak modunda tam çalışıyor.
- ☐ K2: Senaryolar S14, S15, S21, S23 geçti; Sigara ve Uyku Ritmi 7 gün gerçek kullanımla izlendi; Kullanıcı müdahalenin "rahatsız etmeden işe yaradığını" doğruladı.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

---

## F6 — Güneş çekirdeği (S5) ☐

**Amaç:** AI altyapısı. AI kapalıyken her şey çalışmaya devam eder.

- ☐ F6.1 M16: `LlmClient`, `TaskSpec`, `Router` (8 kural), `RouterDecision`
- ☐ F6.2 `GeminiClient`: akış, şemalı çıktı, işlev çağrısı, görsel, önbellekleme, 402/429/5xx ele alma
- ☐ F6.3 `LocalLlmClient` (LiteRT-LM + Gemma): yükleme/boşaltma, kuyruk önceliği, termal koruma
- ☐ F6.4 Bütçe: `AiCallLog`, `AiBudgetDay`, `pricing.json`, %80 uyarı, %100 Katman 1; harcama ekranı
- ☐ F6.5 Gizlilik renkleri, `RedDataMasker`, "Buluta ne gitti?" (D22)
- ☐ F6.6 Prompt dosyaları v1 + `CHANGELOG`; doğrulayıcılar (uzunluk, tıbbi sınır, tarih, Kırmızı veri, tekrar)
- ☐ F6.7 M17: ajan döngüsü, araç kaydı, onay seviyeleri, `ToolCall`, "Güneş'in yaptıkları"
- ☐ F6.8 Enjeksiyon savunması (`[VERİ]` sarmalama, veri kaynaklı yazmada onay)
- ☐ F6.9 M18: hafıza katmanları, gömme, hibrit arama (FTS5 + kosinüs + RRF), "Beni ne biliyorsun?"
- ☐ F6.10 Sohbet ekranı (D7): `VoiceTextBar`, `SpeechOutput`, PTT, Sesli Mod, `VoiceService`
- ☐ F6.11 `PersonaSelector` (3 mod + AUTO), kişilik ekleri
- ☐ F6.12 Kriz sınıflayıcı (Katman 1) + deterministik sözlük önceliği
- ☐ F6.13 Altın setler ve koşucular (`evalCloud`, cihaz içi); güvenlik setleri (Ek C)
- ☐ F6.14 [AI] yükseltmeleri: yakalama bölme, belirsiz sınıflama, tarih yedeği, mikro-adım
- ☐ F6.15 Ayarlar → AI ve Güneş sayfaları; cihaz içi model edinme akışı
- ☐ F6.16 Çıraklık dönemi (karar 0012): `TeacherExample` tablosu, örnekli istem, ret → Gemini → kart önerisi, gece gölge koşusu, devir kuralı ve haftalık "artık kendi yaptıklarım" raporu, Ayarlar anahtarı ve harcama satırı

**Çift kontrol**
- ☐ K1: Bölme F1 ≥ 0,90; sınıflama ≥ %85; kriz yönlendirme %100; tıbbi ihlal 0; başarılı enjeksiyon 0; AI kapalıyken F3–F5 testlerinin tamamı yeşil; ağ kesilince ≤ 2 sn'de Katman 1; bütçe tavanında bulut çağrısı 0 (simülasyon).
- ☐ K2: Kullanıcı uçak modunda günü yürütebildi; "Buluta ne gitti?" kaydını okudu ve içinde beklemediği veri görmedi; sesle ve yazıyla sohbet etti, her yazma işlemini geri alabildi.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

---

## F7 — Bağlam ve orkestratör (S6) ☐

**Amaç:** Güneş bağlamı algılar ve yalnız işe yarayacağı an konuşur.

- ☐ F7.1 M19 duyargaları: takvim (tam), bildirim okuma, kullanım istatistikleri, konum/geofence, Health Connect, ekran görüntüsü, kamera, NFC, cihaz durumu, zaman bağlamı (tatil verisi 2026–2030)
- ☐ F7.2 `ContextHub` → `ContextSnapshot`; Ayarlar → Duyargalar
- ☐ F7.3 M20 Orkestratör: sert elemeler, aday üretimi, bandit (tohumlu), mesaj üretimi, ödül güncelleme
- ☐ F7.4 Bildirim bütçesi 10, saatlik sınır, özet slotları; gözlem modu (karar 0003)
- ☐ F7.5 "Neden?" sheet'i (D21), "Faydalı / Faydasız"
- ☐ F7.6 Beni Tanı görüşmesi (Ek D) ve profil ekranı; hipotez ve düzeltme defteri
- ☐ F7.7 Gece konsolidasyonu (9 adım, ≤ 10 dk, kaldığı yerden)
- ☐ F7.8 M23: Onboarding tam akış (D1, 9 adım) ve Ayarlar alt sayfalarının tamamı (D20)
- ☐ F7.9 Konum ve NFC tetikleri: çıkış kontrolü, rutin, yol süresi öğrenme
- ☐ F7.10 M22 tamamlayıcılar: dinamik kısayollar, derin bağlantılar, Mi Band yansıması

**Çift kontrol**
- ☐ K1: 30 günlük sahte bağlam simülasyonunda bütçe hiç aşılmıyor; aynı tohumla aynı karar; OTP kalıbı hiç işlenmiyor (test); konsolidasyon ≤ 10 dk, pil ≤ günlük %1.
- ☐ K2: 14 gün gözlem modu gerçek kullanım: bütçe içinde, "Faydalı" oranı ≥ %50; senaryolar S7 (kural kısmı), S12 geçti; Kullanıcı "Neleri öğrendim" özetini doğru buldu.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

---

## F8 — Koçluk ve Dürüst Ayna (S7) ☐

- ☐ F8.1 M21-1 Beyin Boşaltma
- ☐ F8.2 M21-2 Başlatma Koçu (yalnız ilk adım)
- ☐ F8.3 M21-3 Günün Mimarı (yük hesabı + aday öncelik)
- ☐ F8.4 M21-4 Sesli Yoldaş
- ☐ F8.5 M21-5 Bak ve Yardım Et (Oda · Belge · Ekran görüntüsü · Serbest · Ürün)
- ☐ F8.6 M21-6 Mesaj Yazarı ve Yanıt bekleyenler
- ☐ F8.7 M21-7 Karar Daraltıcı
- ☐ F8.8 M21-9 Bunaldım Eşlikçisi
- ☐ F8.9 M21-10 Haftalık Ayna (önce deterministik istatistik, sonra dil)
- ☐ F8.10 M21-11 İkinci Beyin ("Sor")
- ☐ F8.11 M21-12 Kendini Ayarlayan Sistem (eş zamanlı ≤ 1 deney)
- ☐ F8.12 M21-13 Ödül Üreticisi
- ☐ F8.13 M26 Dürüst Ayna: 4 kademe, `MirrorValve` (kapatılamaz), tetikler, yapısal doğrulayıcı, `MirrorCard`, "Bu beni kırdı"
- ☐ F8.14 Rutin Mühendisi; `SkillNote` (beceri kanalı)
- ☐ F8.15 v3'te eksik çıktı şemalarının tasarımı (karar 0005-8)

**Çift kontrol**
- ☐ K1: Belge son tarih doğruluğu ≥ %90 (50 örnek); gömülü örüntülü sentetik veride bulgu yakalanıyor, rastgele veride 0 bulgu; valf kurallarının her biri testli; kriz sonrası 72 sa Ayna kartı 0; kaynaksız "Sor" yanıtı 0.
- ☐ K2: Senaryolar S7, S11, S13, S19, S20 geçti; Haftalık Ayna gerçek veride ilk gerçek örüntüyü buldu ve Kullanıcı doğru buldu; "Bu uygulama beni utandırdı" anı 0.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

---

## F9 — Konu Motoru (S8) ☐

- ☐ F9.1 H5 tabloları
- ☐ F9.2 M24: Niyet ayrıştırma, Niyet kartı, hassas konu tespiti, özel kişi takibi reddi
- ☐ F9.3 `WebResearchClient` (Google Arama temellendirmesi), `TopicRunWorker`, Katman 0 doğrulama (yineleme, kaynak, telif örtüşmesi, URL eşleme)
- ☐ F9.4 `TopicDigestWorker`: günde 2 birleşik özet, öğrenilen saat, yenilik yoksa atla
- ☐ F9.5 Akış ekranı (D6), konu detayı (Özetler · Bilgi · Ayarlar), `TopicDigestCard`
- ☐ F9.6 Bilgi kartları, bayatlama (`TopicDecayWorker`), `search_knowledge`, uzman kitabı
- ☐ F9.7 Uzmanlaş / Öğren: kapsam görüşmesi, müfredat, mikro-ders, aralıklı tekrar, saha görevi
- ☐ F9.8 Kalite sınavı (yetkinlik) ve yönlendiriciye etkisi
- ☐ F9.9 Karar araştırması; Olay bekle (anında uyarı, günde ≤ 3)
- ☐ F9.10 Yaşam döngüsü: ilgi sönümü, eylemsizlik, süre kutusu, konu bütçesi

**Çift kontrol**
- ☐ K1: Niyet ayrıştırma ≥ %90 (30 cümle); uydurma URL 0; yineleme ayıklama %100; `nothingNew` → bildirim yok (test); bütçe tavanı simülasyonda aşılmıyor; anahtarsız konu oluşturulabiliyor.
- ☐ K2: Senaryolar S16, S17, S18 geçti; 3 gerçek konu 7 gün çalıştı, okunma ≥ %50, aylık konu payı aşılmadı; Kullanıcı rastgele 5 maddenin kaynağını açıp doğruladı.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

---

## F10 — Dayanıklılık ve kapanış (S9) ☐

- ☐ F10.1 M29 Geri Dönüş protokolü (D24)
- ☐ F10.2 M30 Klinik Özet (PDF/CSV, yorum içermez)
- ☐ F10.3 M15 Güvenilir Kişi (kapalı flag): yardım iste, merdiven son basamağı
- ☐ F10.4 Yedek: parola ile şifreli haftalık dışa aktarım, tek tuşla geri yükleme (karar 0005-5)
- ☐ F10.5 Performans: Baseline Profile, Macrobenchmark, RAM ≤ 300 MB, APK ≤ 60 MB
- ☐ F10.6 Pil: arka plan ≤ %2 (AI hariç) / ≤ %5 (dahil)
- ☐ F10.7 Tanılama dışa aktarımı, çökme kartı, Debug HUD, Geliştirici menüsü
- ☐ F10.8 İnce ayar verisi dışa aktarımı (K10, ağırlık kanalı)
- ☐ F10.9 Erişilebilirlik turu: TalkBack, %200 yazı, kontrast, tek el
- ☐ F10.10 Tüm ekranlarda durum tablosu denetimi (boş / yükleniyor / hata / çevrimdışı / AI kapalı / izin yok)
- ☐ F10.11 `:spike` modülü kaldırıldı; `ideas.md` ve karar kayıtları gözden geçirildi
- ☐ F10.12 Blueprint ↔ uygulama izlenebilirlik denetimi: M1–M30'un her kabul maddesi bir teste ya da cihaz kaydına bağlı

**Çift kontrol**
- ☐ K1: Sıfırlanmış uygulamaya yedekten geri yükleme gidiş-dönüş testi; soğuk açılış ≤ 800 ms; tüm sürüm kapısı (J4) maddeleri yeşil; izlenebilirlik tablosunda boş hücre yok.
- ☐ K2: 30 gün gerçek kullanım: ANR ve çökme 0, kaçan kritik 0; senaryo S22 geçti; Kullanıcı sıfır telefona geri yüklemeyi bir kez kendi yaptı.
- ☐ K3: `proje-beyni.md` bu faz için güncellendi: kararlar, bulgular, yapılan hatalar, zaman çizelgesi, Bölüm 13 kapanış kaydı.

---

## Proje başarı ölçütleri (blueprint A2; F10 sonrası izlenir)

| Ölçüt | Hedef | Durum |
|---|---|---|
| 30. günde kullanım | Haftada ≥ 6 gün | ☐ |
| 90. günde kullanım | Haftada ≥ 5 gün | ☐ |
| Kritik hatırlatma ±1 dk teslim | ≥ %99 | ☐ |
| Yakalama → işleme (48 sa) | ≥ %70 | ☐ |
| "Beni utandırdı" anı | Ayda 0 | ☐ |
| Günlük proaktif bildirim | Bütçe içinde (≤ 10; karar 0003) | ☐ |
| Odak alışkanlıkta "son 7 günde ≥ 4" (90. gün) | Haftaların ≥ %60'ı | ☐ |
| Konu özeti okunma | ≥ %50 | ☐ |
| AI çıktısını düzeltme / geri alma | ≤ %15 | ☐ |

## Değişiklik günlüğü

- **7 Ekim 2026:** İlk sürüm. F0 kapalı; F1 ve F2-A sürüyor.
- **8 Ekim 2026:** Her faza K3 (proje beyni güncellemesi) kapanış şartı eklendi; F0.9.
- **8 Ekim 2026:** F0.11 gece kontrolü usulü ve ilk kontrol (9 bulgu düzeltildi; 58 birim testi).
- **8 Ekim 2026:** F0.10 Claude Code altyapısı. Faz kapısı artık kancayla zorunlu; tutarlılık `/dogrula` ile denetlenir.
