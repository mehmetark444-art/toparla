# 0001 — Bulut katmanı Gemini API

Tarih: 7 Ekim 2026  Durum: Kabul

**Bağlam:** Blueprint K4, F2, F5.1 ve M24 bulut katmanını Anthropic Messages API (Claude)
olarak tanımlıyor. Kullanıcı netleştirme turunda "Claude API değil Gemini API
kullanılacaktır" dedi ve bir Google Cloud projesine bağlı anahtar verdi.

**Karar:** Katman 2 sağlayıcısı Google Gemini API'dir. `LlmClient` soyutlaması aynen
kalır; `ClaudeClient` yerine `GeminiClient` yazılır. Blueprint'teki kavramlar şöyle eşlenir:

| Blueprint | Gemini karşılığı | Durum |
|---|---|---|
| Haiku (hızlı/ucuz) · Sonnet (günlük) · Opus (derin) | Gemini'nin hızlı / günlük / derin model kademeleri | `[DOĞRULA]` S0: güncel model kimlikleri ve fiyatlar |
| Sunucu tarafı web arama aracı | Google Arama ile temellendirme (grounding) | `[DOĞRULA]` S0: araç adı, kaynak/atıf alanları, maliyet |
| Zorlanmış `tool_choice` ile yapılandırılmış çıktı | Yanıt şeması (JSON şemalı çıktı) / işlev çağrısı | `[DOĞRULA]` |
| Prompt önbellekleme (`cache_control`) | Bağlam önbellekleme | `[DOĞRULA]`: asgari boyut, ücret |
| SSE akışı, görsel girdi | Akışlı üretim, görsel girdi | `[DOĞRULA]` |
| Anthropic Console harcama limiti | Google Cloud bütçe uyarısı / kota | Onboarding metni buna göre |
| `./gradlew :ai:evalCloud` anahtarı | Ortam değişkeni `GEMINI_API_KEY` | |

Verilen anahtarın hangi uç noktayla çalıştığı (Gemini Developer API mi, Vertex AI mı)
S0'ın ilk bulut spike'ında denenerek belirlenir ve `docs/platform-bulgulari.md`'ye yazılır.

Aylık bütçe varsayılanı (25 $), %80 uyarı, %100'de Katman 1'e düşüş, "Buluta ne gitti?"
günlüğü, gizlilik renkleri ve enjeksiyon savunması değişmez. `res/raw/pricing.json`
Gemini fiyatlarıyla doldurulur.

**Anahtar güvenliği:** Anahtar bu depoya, belgelere, günlüklere yazılmaz. Uygulamada
Kullanıcı Ayarlar → AI ekranından yapıştırır (`SecretStore`, Keystore AES-GCM).
Geliştirmede ortam değişkeni ya da gitignore'daki `secrets.properties`. Anahtar sohbet
içinde açık paylaşıldığı için, depo GitHub'a taşınmadan önce yenilenmesi ve yalnız
Gemini API'sine kısıtlanması önerilir.

**Alternatifler:** Claude'da kalmak (Kullanıcı reddetti); iki sağlayıcıyı birden
desteklemek (tek kullanıcı için gereksiz karmaşa; soyutlama ileride eklemeye izin verir).

**Sonuçlar ve riskler:** Cihaz içi Gemma ile aynı aile olması prompt ve değerlendirme
işini sadeleştirir. Arama temellendirmesinin maliyeti ve atıf biçimi Konu Motoru
bütçesini (aylık bütçenin %40'ı) doğrudan etkiler; S0'da ölçülür. Altın setler ve
güvenlik setleri Gemini modelleriyle koşar.

**İlgili blueprint bölümü:** K4, K21, B1, D1-5, F2, F3, F5.1, F11, M24.2
