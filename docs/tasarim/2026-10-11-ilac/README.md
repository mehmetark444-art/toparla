# İlaç takibi (M9) ekranları taslağı (F2.39–F2.41) — 11 Ekim 2026

**Durum:** Onaylandı (11 Ekim 2026).
**Görüntüle:** https://claude.ai/artifact/6ARGdrnW68LuiKsjo7fnW2 (tuval; bu klasördeki dosyalar onun kopyasıdır).
**Yöntem:** `mobile-app-ui-design` yeteneğinin 5 adımı + `.claude/rules/tasarim.md` sınırları; görsel dil karar 0016
(Seçenek B). Satır, kart ve düğme kalıpları önceki taslaklarla aynıdır (`../2026-10-09-hatirlatma-ekranlari/`,
`../2026-10-10-kurulum-sihirbazi/`). Örnek içerik uydurmadır (ilaç adları gerçek değil).

## Panolar
1. `Main.dc.html` / `IlaclarAcik.dc.html` — **İlaçlar** (koyu, açık). "Bugün": günün dozları saatiyle; durum ikon +
   metinle (Alındı · Kaydedilmedi · Sırada). "Kaydedilmedi" amber, asla kırmızı; altında **Geç aldım** / **Atladım**.
   "İlaçların": tanımlar (günler, saatler, kalan stok). Alt not: Toparla doz ya da saat önermez. Baskın eylem **İlaç ekle**.
2. `IlacEkle.dc.html` / `IlacEkleAcik.dc.html` — **İlaç ekle / düzenle.** Ad, doz (serbest metin, "hiçbir hesaba
   girmez"), saatler (çip + "Saat ekle"), günler (Her gün / Seçili günler), çift doz koruması (Kapalı · 2 · 4 · 6 · 8 ·
   12 sa; Kullanıcı seçer), stok (kalan, her dozda, eşik; isteğe bağlı), not, "Bildirimde adını gizle". Düzenlerken
   **İlacı sil** (kayıtlar silinmez). Baskın eylem **Kaydet**.
3. `Gecmis.dc.html` — **Geçmiş.** Gün gün dozlar; kaydedilmeyen dozda yalnız kayıt (Geç aldım / Atladım), telafi
   önerisi yok; bilgi kartı: "Talimat için doktorunu ya da eczacını izle." Üstte **Dışa aktar**.
4. `DisaAktar.dc.html` — **Dışa aktar** alt sayfası: aralık (7 / 30 / 90 gün), biçim (PDF / CSV), "Dosyayı nereye
   kaydedeceğini sen seçersin; Toparla kimseye göndermez." **Dosyayı kaydet** (sistemin dosya kaydetme ekranı açılır).
5. `Ayarlar.dc.html` — **Ayarlar → Modüller** (blueprint D20; K18): İlaç takibi anahtarı (varsayılan kapalı), açıkken
   "İlaçlar" satırı ve "İlaç ekranlarını kilitle" anahtarı; "Hatırlatmalar" altında Hatırlatma Sağlığı. Ayarlar'a üst
   çubuktaki dişli simgesinden girilir.
6. `Kilit.dc.html` — **Kilitli.** Kilit açıksa ilaç ekranlarına girişte sistemin parmak izi / yüz / ekran kilidi
   penceresi açılır; bu ekran onun arkasında ya da vazgeçilince görünür.
7. `KartIlac.dc.html` — **Tam ekran kart, ilaç:** saat (kritik renk), ad, doz, not; **Aldım** (baskın) · **15 dk sonra**
   · **Atlıyorum**.
8. `KartKilitli.dc.html` — **Aynı kart, telefon kilitliyken:** ad yerine "İlaç zamanı" (blueprint D25).
9. `CiftDoz.dc.html` — **Çift doz sorusu** ("Emin misin?", son kaydın ne kadar önce olduğu ve seçilen aralık;
   **Evet, aldım** / **Vazgeç**) ve bildirimin iki yüzü (telefon açık: ad, doz, üç eylem; kilit ekranı: "İlaç zamanı").
10. `PlanGiris.dc.html` — **Plan sekmesinde giriş kartı** (modül açıkken): sıradaki doz ve kaydedilmeyen doz sayısı.
    İlaç dozları Plan listelerinde görünmez; kendi ekranları vardır.

## Tasarım kararları
- Kırmızı yalnız tam ekran kartta (kritik teslim). "Kaydedilmedi" amber; hiçbir yerde "kaçırdın" yok.
- Dozun zamanı hiçbir düğmeyle değişmez: ilaçta "Yarın" ya da "Yarına taşı" yok (zaman değişikliği bir öneri olurdu).
- Kilit ekranında ad hiçbir zaman görünmez; "Bildirimde adını gizle" bildirimin kendisini de gizler.
- İlaç takibi kapalıyken: Plan kartı, Ayarlar'daki alt satırlar, bildirimler ve tam ekran kart görünmez; veri silinmez.

## Onay
- Seçilen: taslak olduğu gibi ("Onaylıyorum").
- Onay tarihi: 11 Ekim 2026
- İstenen değişiklikler: yok.

## Koddaki farklar (taslak ↔ uygulama; ekran görüntüsü testleri `app/src/test/screenshots/`)
- (Kodlanınca doldurulur.)
