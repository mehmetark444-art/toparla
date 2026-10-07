# CLAUDE.md — Toparla · Güneş çalışma protokolü

Kaynak: `docs/BLUEPRINT.md` Bölüm A6. Blueprint değiştirilmez; ondan sapan her karar
`docs/decisions/` altındadır ve **karar kaydı blueprint'i ezer**. Kod yazmadan önce
ilgili karar kayıtlarını oku.

## Blueprint'i ezen kararlar (özet)

- **0001:** Bulut katmanı (Katman 2) Anthropic Claude değil **Google Gemini API**'dir.
  Blueprint'te "Claude / Anthropic / Sonnet / Haiku / Opus" geçen her yer buna göre okunur.
- **0002:** Yasak kelime listesi ve kelime tabanlı ton doğrulayıcısı **yoktur**.
- **0003:** Günlük proaktif bildirim bütçesi varsayılan **10** (alt sınır 10).
  **Israrlı takip:** Kullanıcı'nın "yapacağım" dediği işin hatırlatması, "Yaptım"
  denene kadar 30 dakikada bir tekrarlanır.
- **0004:** İlk odak alışkanlıklar: Sigara (bırakma) + Uyku Ritmi.
- **0005:** Dil, yedek parolası, paralel dilim çalışması, CI ve belge boşlukları.

## Kimlik ve amaç

Sen bu projenin tek geliştiricisisin; Kullanıcı ürün sahibi ve tek kullanıcı. Amaç
Kullanıcı'nın gerçekten her gün kullanacağı, güvenilir, sakin ve kişisel bir uygulama.
Kullanıcı'nın teknik bilgisi yoktur: elle yapacağı her adım sıfır bilgi varsayımıyla,
numaralı ve tek eylemli anlatılır. Yapabildiğin her şeyi kendin yap.

## Dil

Belgeler, karar kayıtları, commit mesajları, kod yorumları ve kullanıcıya görünen
metinler **Türkçe**. Kod tanımlayıcıları (sınıf, fonksiyon, değişken, tablo adları)
blueprint'teki gibi İngilizce kalır (`NowSelector`, `ReminderPlanner`).

## Her oturum başında

1. `docs/progress.md` ve `git log -10` oku.
2. `docs/BLUEPRINT.md`'nin ilgili bölümünü ve `docs/decisions/` kayıtlarını oku.
3. Bu oturumda **tek** dikey dilim ya da dilim içi tek madde seç.
4. Hedefi iki cümleyle `docs/progress.md`'ye yaz.
5. Önce testleri, sonra kodu yaz.

## Kod kuralları

- `:domain` saf Kotlin/JVM; hiçbir Android sınıfı içermez. İş kuralları orada ve birim testlidir.
- Yan etkiler arayüz arkasında: `ReminderScheduler`, `Notifier`, `SpeechInput`,
  `SpeechOutput`, `LlmClient`, `WebResearchClient`, `ContextSource`, `AppOpenSource`,
  `HealthSource`, `CalendarSource`. Testte sahteleri kullan.
- `System.currentTimeMillis()`, `Instant.now()`, `LocalDate.now()`, tohumsuz `Random()`
  **yasak**; enjekte `Clock` ve `RandomSource`.
- Sihirli sabit yok: eşikler, süreler, bütçeler `Defaults.kt` ve `AppConfig` içinde;
  kullanıcı ayarı olanlar DataStore'da.
- Kullanıcıya görünen her metin `strings.xml` (Türkçe). Mikro-metin havuzları
  `res/raw/microcopy/*.json`. Kod içinde Türkçe kullanıcı metni yok.
- `!!` yasak. `GlobalScope` yasak. `runBlocking` yalnızca testte.
- Her `catch` ya işler ya `AppError`'a çevirir; sessiz yutma yasak.
- Idempotans: alarm kurma, bildirim gösterme, kayıt yazma aynı anahtarla tekrarlanırsa aynı sonuç.
- Yıkıcı migration yasak; her migration test edilir; şema dışa aktarılır.
- API anahtarı Keystore ile şifreli; günlüklere, çökme kaydına, tanılama zip'ine, depoya
  ve belgelere **asla** girmez. Geliştirme sırasında yalnız ortam değişkeni
  (`GEMINI_API_KEY`) ya da gitignore'daki `secrets.properties`.
- Zaman damgaları UTC epoch ms; yerel tekrarlar `zoneId` ile.
- Yeni izin eklerken: neden gerekli, hangi ekranda istenir, reddedilirse ne olur → karar kaydı.
- Prompt'lar kodda değil `:ai/src/main/assets/prompts/vN/*.md` dosyalarında, sürümlü,
  `prompts/CHANGELOG.md` ile.
- Tüm LLM çıktıları yapılandırılmış (JSON şeması) ve doğrulayıcıdan geçer.

## Tamamlama tanımı (her özellik)

1. Davranış ve kabul kriterleri karşılandı.
2. `:domain` birim testleri; zamanlı yollar için sahte saatli test; her ekran için en az bir Compose UI testi.
3. AI'ya bağlı özellik: AI **kapalıyken** kural tabanlı karşılık çalışıyor ve testli.
4. Boş / yükleniyor / hata / çevrimdışı / AI kapalı / izin yok durumları tasarlandı.
5. TalkBack etiketi, ≥ 48 dp hedef, kontrast ≥ 4,5:1, %200 yazı ölçeği.
6. "Animasyonları azalt" açıkken çalışıyor.
7. Koyu, Açık ve AMOLED temada ekran görüntüsü testi.
8. Geri al yolu var; yazma işlemi olay günlüğüne (ve Güneş yaptıysa `ToolCall`'a) düşüyor.
9. Mikro-metinler Ek A ilkelerine (A.1) uygun. (Yasak kelime taraması yok: karar 0002.)
10. `ktlint`, `detekt`, Android Lint temiz.
11. `release` varyantıyla telefonda denendi; bulgu `docs/platform-bulgulari.md`'de.

## Yapmaman gerekenler

- Belgede olmayan özellik ekleme; öneri `docs/ideas.md`'ye.
- Yarım özelliği flag'siz telefona sokma.
- Hatırlatma motorunu "AI ile akıllandırma" (Katman 0'dır).
- Test yazmadan `:reminders`'ı değiştirme.
- Kullanıcı verisini kendiliğinden silme/taşıma.
- Belirsiz platform davranışını varsayma: spike yaz, ölç, kaydet.
- Sürüm numarası ya da model adı uydurma: resmi kaynaktan doğrula, `gradle/libs.versions.toml`'a kilitle.
- Güneş'in metinlerinde tıbbi iddia, tanı dili, utandırma.

## Kullanıcı ile iletişim (DEHB'ye uygun)

Kısa durum raporu (≤ 5 madde), tek soru, önerilen varsayılanla karar sun ("Şunu
yapıyorum; itiraz etmezsen devam"). Oturum sonunda üç başlık: **Ne bitti**,
**Telefonda neyi dene** (3 madde), **Sıradaki dilim**.

## Dilim çalışma düzeni

Dilim sırası `docs/progress.md`'de. Gerçek kullanım günleri isteyen ölçütler (S1: 7 gün,
S6: 14 gün, S9: 30 gün) beklenirken sonraki dilim **feature flag arkasında** yazılır
(karar 0005). Bir dilimin "bitti" işareti yine de ölçüt karşılanınca konur.

## Ortam

Windows 11 · Android Studio 2025.2.2 (`C:\Program Files\Android\Android Studio`, JBR 21)
· SDK `%LOCALAPPDATA%\Android\Sdk` (platform 36, build-tools 36.1.0) · `adb` PATH'te
değil: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`. Uzak depo:
`https://github.com/mehmetark444-art/toparla.git` (push yalnız Kullanıcı isteyince).
