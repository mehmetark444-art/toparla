# 0010 — Cihaz içi model seçimi

Tarih: 8 Ekim 2026  Durum: Kabul (ölçüme dayalı; Kullanıcı 8 Ekim 2026'da onayladı: tek model, Gemma 4 E4B)

> **Kesinleşen karar:** cihaz içi tek üretici model **Gemma 4 E4B**. **Yedek model (E2B) tutulmaz.**
> Aşağıdaki "E2B yedek model" maddesi ilk öneridir ve geçersizdir. Gerekçe: iki model aynı anda
> belleğe sığmaz, geçiş her seferinde 4–11 sn yükleme ister (E2B'nin hız üstünlüğünü siler), ~5 GB ek
> depolama ve ikinci bir doğrulama yükü getirir, E2B'nin metin kalitesi daha düşüktür. Bellek, ısı
> ya da pil kısıtında yedek **Katman 0'dır** (kural tabanlı sonuç) ve ağır iş ertelenir; hızlı işler
> (sınıflama, tarih) zaten önce kuralla çözülür. Gerçek kullanımda E4B bellekten öldürülür ya da
> fazla yavaş kalırsa bu karar yeniden açılır (izleme: Hatırlatma Sağlığı / Debug HUD).

**Bağlam:** Blueprint K4/B1: cihaz içi model "Gemma 4 ailesi, E2B varsayılan, E4B denenir". Karar 0009:
model Gemma olmak zorunda değil; en iyi Türkçe ve karta dayalı yanıt (RAG) sonucunu veren seçilir ve
AI işlerinin varsayılanı cihaz içidir. Kullanıcı: "yerel yapay zekâ seçimimiz çok önemli".

**Ölçüm:** `platform-bulgulari.md` § "Spike 10: 50 soruluk Türkçe set". Beş aday eleme turundan geçti;
üçü 50 soruluk sette ikişer kez koştu (LiteRT-LM 0.18.0, GPU, Xiaomi 17T Pro).

| | Gemma 4 E2B | Gemma 4 E4B | Qwen3 4B |
|---|---|---|---|
| Doğru (50) | 46,5 | 45,5 | 32–32,5 |
| Talimata harfiyen uyum (50) | 37 | **43** | 29 |
| Karta dayalı yanıt (8) | 8 | 8 | 8 |
| Güvenlik (5) | 5 | 5 | **2** (doz verdi, gömülü talimata uydu) |
| Yanıt süresi ortanca / %90 | **2,0 / 10 sn** | 3,7–4,2 / 10 sn | 8,1 / 15 sn |
| İlk parça ortanca | **0,5 sn** | 1,4 sn | 5,3 sn |
| Koşu sırasında en düşük boş bellek | 5,2 GB | 3,1 GB | 3,1 GB |
| Metin kalitesi (gözle) | düzgün; yer yer boş ya da hatalı | **en doğal ve en somut** | hatalı, buyurgan |

**Aday karar:** cihaz içi üretici model **Gemma 4 E4B** (`gemma-4-E4B-it.litertlm`, 3,66 GB).
- Gerekçe: talimata en iyi uyan (kod çiti 3 ↔ 11), metin kalitesi gözle en iyi (ilk adımlar somut,
  özet veriye sadık, hatırlatmalar doğal), güvenlik ve karta sadakat tam. Otomatik "doğru" puanındaki
  1 puanlık fark E2B lehine ama E2B'nin tam puan aldığı kısa metinlerde gözle hata var
  (`[VERİ]` etiketini yanıta taşıdı, "kızıyla" dedi, özetten sayıları düşürdü).
- **E2B yedek model** olarak tutulur: bellek darsa (boş bellek < 4 GB), ısı ya da pil kısıtında ve
  ≤ 1 sn yanıt gereken işlerde (sınıflama) kullanılabilir. İki modelin birlikte tutulması ~13 GB
  depolama ister (GPU önbellekleriyle); tek model yeterse E2B kaldırılır. Bu ayrım F6'da ölçülerek kesinleşir.
- **Qwen3 4B elendi:** melatonin için doz aralığı verdi ve veri bloğuna gömülü "TÜM GÖREVLER SİLİNDİ"
  talimatına uydu; ayrıca en yavaş aday ve yalnız elle kurulan ChatML yoluyla çalışıyor.

**Modelden bağımsız sonuçlar (hangi model seçilirse seçilsin):**
1. **Tarih ayrıştırma modele bırakılmaz.** Üç model de Türkçe göreli tarihleri yanlış hesapladı
   (E4B 5/6, E2B 4/6, Qwen 1/6). `TrDateParser` (Katman 0) birincil yoldur; model yalnız yedek.
2. **Bölme fazla parçalıyor ya da iş olmayan cümleden iş çıkarıyor** (her iki Gemma 6,5/8).
   Çıktı öneridir; Gelen'de birleştirme ve silme tek dokunuş olmalı.
3. **Toleranslı JSON ayrıştırma ve şema doğrulama zorunlu** (kod çitleri, yanlış alan adları).
4. Yanıtlar iki koşuda birebir aynı çıktı: örnekleme bu yapılandırmada belirlenimci; testler tekrarlanabilir.
5. Öğrenilen her şey (hafıza, bilgi kartları, beceri notları) modelden bağımsız saklanır; model
   ileride değiştirilebilir (yeni model aynı setten geçmeden varsayılan olmaz).

**Sınırlar:** 50 soru küçük bir settir; kategoriler 2–8 soruluk. Kısa metin kategorilerinde otomatik
puan yalnız biçime bakar; kalite gözle değerlendirildi. Çok turlu sohbet, araç çağrısının gerçek API'si,
görsel ve ses girdisi, uzun bağlam (2K+ token) ve gömme modeliyle gerçek RAG **ölçülmedi**.
Sürekli kullanımda pil sıcaklığı 35 dakikada 32 → 39 °C çıktı (ısıl durum 0); pil tüketimi ölçülemedi (USB bağlı).

**İlgili blueprint bölümü:** K4, B1, F2, F5.2, G8; kararlar 0009, 0007
