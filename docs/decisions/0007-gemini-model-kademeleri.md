# 0007 — Gemini model kademeleri ve fiyat tablosu

Tarih: 8 Ekim 2026  Durum: Kabul (ölçüme ve resmi fiyat sayfasına dayalı)

**Bağlam:** Karar 0001 bulut katmanını Gemini yaptı; kademe modelleri "doğrulanacak" bırakılmıştı.
8 Ekim'de bakiye geldi, çağrılar ölçüldü (`platform-bulgulari.md`, "Spike 11: Gemini API ölçümleri").
Fiyatlar ai.google.dev/gemini-api/docs/pricing sayfasından (son güncelleme 7 Ekim 2026).

**Karar:**

| Kademe (blueprint adı) | Model kimliği | Girdi / çıktı (1M token, USD) | Kullanım |
|---|---|---|---|
| Hızlı (Haiku) | `gemini-3.5-flash-lite` | 0,30 / 2,50 | Yakalama ayrıştırma yedeği, konu niyet ayrıştırma, rutin konu taraması |
| Günlük (Sonnet) | `gemini-3.8-flash` | 0,75 / 3,75 (1 Ocak 2027'de iki katı) | Sohbet, planlama, görsel, ajan araçları |
| Derin (Opus) | `gemini-3.1-pro-preview` | 2,00 / 12,00 | Haftalık Ayna, müfredat, kalite sınavı üretimi |

- Kimlikler açık yazılır; `-latest` takma adları kullanılmaz (davranış habersiz değişmesin).
  Model kimliği Ayarlar → AI'dan değiştirilebilir; yeni model altın seti geçmeden varsayılan olmaz.
- **Düşünme:** çıktı fiyatına dahildir ve gecikmeyi belirler. Etkileşimli işlerde günlük kademe
  `thinkingLevel: "low"` ile çağrılır (ölçüm: 7,9 sn → 2,3 sn). Derin kademe varsayılan düşünmeyle.
- **Arama temellendirmesi:** ayda 5 000 ücretsiz, sonrası 1 000'i 14 $; arama sorgusu başına sayılır.
  Blueprint'teki arama tavanlarıyla (konu başına 3/5/10) 5 konu × günde 2 tarama en çok ayda ~1 500
  sorgu eder: ücretsiz payın içinde. Konu Motoru'nun varsayılan sıklığı blueprint'teki gibi kalır.
- Fiyatlar F6'da `ai/src/main/res/raw/pricing.json` dosyasına bu tablodan yazılır.

**Alternatifler:** Hızlı kademede `gemini-3.1-flash-lite` (0,25 / 1,50; daha ucuz, ölçülmedi; altın
sette denenir). Derin kademede Flash + yüksek düşünme (Pro'nun önizleme sürümü kaldırılırsa yedek).

**Sonuçlar ve riskler:** Derin kademe "preview" modeldir; kaldırılabilir. Günlük kademenin fiyatı
2027 başında iki katına çıkıyor: bütçe ekranı fiyatı dosyadan okur, o tarihte güncellenir.
Hızlı kademe tek denemede "kedi maması bitmiş"i yanlış sınıfladı (alışveriş yerine endişe): kalite
altın setle ölçülmeden hiçbir kademeye güvenilmez. Aylık 25 $ bütçe varsayılanı değişmedi.

**İlgili blueprint bölümü:** F2, F3, F5.1, F11, M24.2, M24.7; karar 0001
