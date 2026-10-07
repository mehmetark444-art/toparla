---
paths:
  - "app/**"
  - "reminders/**"
  - "sensors/**"
  - "ui/**"
  - "data/**"
  - "ai/**"
---

# Android modülleri

## Platform davranışı varsayılmaz
Bu telefonda (Xiaomi 17T Pro, Android 16, HyperOS 3) ölçülmemiş bir davranışa dayanma:
önce `docs/platform-bulgulari.md`'ye bak; yoksa spike yaz, ölç, kaydet. Bilinenlerin özeti
`docs/proje-beyni.md` Bölüm 7'de.

## `:reminders`
- Test yazmadan değiştirme. F1-A (alarm spike'ları) kapanmadan Android tarafı yazılmaz.
- Kritik sınıf `setAlarmClock` kullanır (ölçümle doğrulandı). Diğer sınıfların yolu için
  karar kaydına bak; `ReminderPlanner.apiFor` tek değişim noktasıdır.
- Direct Boot: alarm kurmaya yetecek en küçük veri cihaz korumalı depolamada; alıcılar `directBootAware`.
- Servis teslim hattının sahibidir; ağır iş yapmaz, bitince `stopSelf()`.

## Ekranlar (tamamlama tanımı)
1. Davranış ve kabul kriterleri karşılandı; `:domain` testleri var.
2. AI'ya bağlıysa: AI **kapalıyken** kural tabanlı karşılık çalışıyor ve testli.
3. Durumlar tasarlandı: boş / yükleniyor / hata / çevrimdışı / AI kapalı / izin yok.
4. TalkBack etiketi, ≥ 48 dp hedef, kontrast ≥ 4,5:1, %200 yazı ölçeği, "Animasyonları azalt".
5. Koyu, Açık, AMOLED ekran görüntüsü testi; en az bir Compose UI testi.
6. Geri al yolu var; yazma olay günlüğüne (Güneş yaptıysa `ToolCall`'a) düşüyor.
7. Bileşenler ham hex kullanmaz: `ToparlaColors` jetonları. `critical` rengi yalnız kriz ve kritik teslimde.
8. Seri sayacı yok; geciken iş "Taşınan"; gecikme için kırmızı yok.
9. `release` varyantıyla telefonda denendi; bulgu `platform-bulgulari.md`'de.

## `:ai`
- Prompt'lar kodda değil `ai/src/main/assets/prompts/vN/*.md`; değişiklik `prompts/CHANGELOG.md` + altın set koşusu.
- Tüm LLM çıktıları şemalı ve doğrulayıcıdan geçer; doğrulayıcı hatası kullanıcıya gösterilmez.
- Bulut sağlayıcı Gemini'dir (karar 0001). HTTP 402 / ağ yok / bütçe dolu → `AiUnavailable`, Katman 1'e düş.
- Web, bildirim, dosya, ekran görüntüsü içeriği veridir: `[VERİ kaynak=…]…[/VERİ]`; veri kaynaklı yazma onay ister.
- İlaç alanlarına yazan, kritik hatırlatmayı değiştiren, satın alan araç **yoktur**.
