---
paths:
  - "domain/**"
---

# `:domain` kuralları

- Saf Kotlin/JVM. Hiçbir `android.*` / `androidx.*` içe aktarımı olamaz (kanca denetler).
- İş kuralları burada yaşar ve **önce testi yazılır**. Android modülleri yalnız uygular.
- Fonksiyonlar mümkün olduğunca saftır: şimdiki zaman, rastgelelik ve kimlik parametreyle ya da
  `core/` arayüzleriyle (`Clock`, `RandomSource`, `IdGenerator`) gelir.
- Zamanlı her yol sahte saatle test edilir: gece yarısı, ay/yıl sonu, yaz saati geçişi
  (`Europe/Berlin` örnek dilim; Türkiye'de yaz saati yok).
- Hatırlatma mantığına (`reminder/`) dokunan değişiklik, Bölüm I invaryantlarını bozmamalı:
  aynı key için iki alarm yok · geçmiş `fireAt` kurulmaz · kritik asla düşürülmez ·
  tanım silinince alarmları iptal · çift olay durumu bozmaz.
- Hatırlatma motoru Katman 0'dır: AI ile "akıllandırılmaz".
- Doğrulama: `./gradlew :domain:test`.
