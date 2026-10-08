# 0013 — Konuşma tanıma yolu

Tarih: 8 Ekim 2026  Durum: Kabul (ölçüme dayalı; gürültülü ortam ölçümü bekliyor)

**Bağlam:** Blueprint B1/G7: cihaz içi `SpeechRecognizer` (Türkçe); yetersizse Gemma ses girişi ya da
yerel Whisper. Ölçüm: `platform-bulgulari.md` § "Spike 9: Türkçe konuşma tanıma".

**Karar:**
1. Konuşma tanıma **Android'in cihaz içi tanıyıcısıdır** (`createOnDeviceSpeechRecognizer`, `tr-TR`).
   Yerel Whisper ya da modelin ses girişi **eklenmez** (ihtiyaç ölçülmedi; gürültü turunda yetersiz
   çıkarsa bu karar yeniden açılır).
2. Tanıyıcı sayıları, saatleri ve tutarları **rakamla ve biçimlendirerek** verir ("9.00'da", "15.30'da",
   "5'inde", "3.500", "%20", "₺1.250") ve küçük harfle, noktalamasız yazar. Bu yüzden:
   - `TrDateParser` ve tutar ayrıştırıcı bu yazımları **birincil girdi** olarak destekler
     (altın sete bu biçimler eklenir);
   - ham WER değil, **anlam hatası oranı** izlenir (rakam/yazı farkı hata sayılmaz).
3. Özel adlar (kişi adı) ve hâl ekleri en sık hata kaynağıdır: yakalanan metin her zaman düzeltilebilir
   kalır; profildeki kişi adları sonradan düzeltme önerisi için kullanılır (Katman 0 benzerlik eşleştirmesi).

**Alternatifler:** Yerel Whisper (sherpa-onnx): ek 100+ MB model, pil ve gecikme; mevcut sonuç yeterli.

**Sonuçlar ve riskler:** Tek konuşmacı, sessiz oda, tek tur. Sokak gürültüsü, kulaklık mikrofonu ve
uzun (2–3 dk) beyin boşaltma konuşması ölçülmedi. Tanıyıcı Google'ın sistem bileşenine bağlı
(`com.google.android.as` / Türkçe paket kurulu bulundu); paket kaldırılırsa klavye girişine düşülür.

**İlgili blueprint bölümü:** B1, G7, M1, M21-1, M21-4, F5.4
