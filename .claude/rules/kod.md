---
paths:
  - "**/*.kt"
  - "**/*.kts"
  - "**/src/**/*.xml"
---

# Kod kuralları (Kotlin / Android)

Kaynak: blueprint A6 ve B4. `:spike` atılacak koddur ve bu kurallardan muaftır.
İşaretli (⚙) kuralları `.claude/hooks/kod-kurallari.mjs` her düzenlemede denetler.

## Yasaklar
- ⚙ `!!` yasak. ⚙ `GlobalScope` yasak. ⚙ `runBlocking` yalnız testte.
- ⚙ `System.currentTimeMillis()`, `Instant.now()`, `LocalDate.now()` ve benzerleri, tohumsuz
  `Random()` yasak. Zaman parametreyle ya da enjekte `Clock` ile; rastgelelik `RandomSource` ile.
- Sihirli sabit yok: eşik, süre, bütçe `Defaults.kt` / `AppConfig` içinde adlandırılır;
  kullanıcı ayarı olanlar DataStore'da.
- Kod içinde Türkçe kullanıcı metni yok: `strings.xml`; mikro-metin havuzları `res/raw/microcopy/*.json`.
- Sessiz `catch` yok: ya işle ya `AppError`'a çevir.
- Yıkıcı migration yok; her migration `MigrationTestHelper` ile testli; şema dışa aktarılır.
- `kapt` yok (KSP). `security-crypto` yok (Keystore + `SecretStore`). `dataSync` FGS tipi yok.

## Katmanlar
- Akış: UI → ViewModel → UseCase (`:domain`) → Repository (`:data`) → DAO.
- UI yalnız `StateFlow<UiState>` okur; olaylar `Event`, tek seferlik yan etkiler `Effect` (Channel).
- Ekran = veritabanının işlevi: process death sonrası Room'dan yeniden kurulur.
- Yan etkiler arayüz arkasında: `ReminderScheduler`, `Notifier`, `SpeechInput`, `SpeechOutput`,
  `LlmClient`, `WebResearchClient`, `ContextSource`, `AppOpenSource`, `HealthSource`, `CalendarSource`.
- Idempotans: alarm kurma, bildirim gösterme, kayıt yazma aynı anahtarla tekrarlanırsa aynı sonuç.
- Zaman damgaları UTC epoch ms; yerel tekrarlar `zoneId` ile.
- Bitmemiş özellik `FeatureFlags` arkasında kapalıdır.

## AGP 9 notları
- Android modüllerinde Kotlin yerleşiktir; `kotlin-android` eklentisi **eklenmez**.
  Yalnız saf JVM modülü (`:domain`) `kotlin("jvm")` kullanır.
- Yeni bağımlılık: sürümü resmi depodan doğrula, `gradle/libs.versions.toml`'a kilitle. Sürüm uydurma.
- `compileSdk = minSdk = targetSdk = 36`. Geriye uyum kodu yazma.

## Dil
Tanımlayıcılar İngilizce (`NowSelector`); yorumlar ve KDoc Türkçe. Test adları Türkçe ve
ASCII (ters tırnaklı fonksiyon adında Türkçe karakter kullanma).

## Yeni izin
Neden gerekli, hangi ekranda istenir, reddedilirse ne olur → karar kaydı (`/karar-kaydi`).
