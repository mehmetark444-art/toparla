# Hatırlatma ekranları taslağı (F2.35–F2.38) — 9 Ekim 2026

**Durum:** Onaylandı (10 Ekim 2026) ve kodlandı.
**Görüntüle:** https://claude.ai/artifact/BZX4b7PgcEGrt5kTJt3WoQ (tuval; bu klasördeki dosyalar onun kopyasıdır).
**Yöntem:** `mobile-app-ui-design` yeteneğinin 5 adımı + `.claude/rules/tasarim.md` sınırları; görsel dil karar 0016
(Seçenek B). Örnek içerik uydurmadır.

## Panolar
1. `Main.dc.html` / `EkleAcik.dc.html` — **Hatırlatma ekle** (F2.38), koyu ve açık. Tek ekranda dört soru: ne, ne zaman
   (hazır seçenekler + "Başka zaman…"), ne kadar önemli (Kritik / Önemli / Normal, her birinin ne yapacağı yazılı),
   "Yaptım diyene kadar sor" anahtarı. Altta özet satırı ve tek birincil eylem **Hatırlat**.
2. `Saglik.dc.html` / `SaglikAcik.dc.html` — **Hatırlatma Sağlığı** (F2.35), koyu ve açık. Üstte tek cümlelik durum
   ("2 ayar eksik"; amber, kırmızı değil), "Düzeltilecekler" (her satırda neden + **Düzelt**), "Yerinde olanlar"
   (ikon + metin + değer), altta **Hatırlatmaları sına**.
3. `Sihirbaz.dc.html` — **Kurulum sihirbazı**nın bir adımı (F2.37): otomatik başlatma. Neden gerekli, açılacak
   sayfada yapılacak üç şey, **Ayar sayfasını aç** / **Şimdi değil**.
4. `Sina.dc.html` / `SinaSonuc.dc.html` — **Hatırlatmaları sına** (F2.36): 20 saniyelik bekleme ve "ulaşmadı"
   sonucu (olası üç neden, sırayla **Düzelt**, **Yeniden dene**).
5. `Liste.dc.html` — hatırlatmaların göründüğü yer: **Plan** sekmesi. "Seni bekleyenler" (ısrarlı iş kartı: Yaptım /
   Bugün olmayacak), "Sıradakiler" (saat, başlık, sınıf; taşınan amber), sağ altta **+**, sağ üstte **Sağlık**.

## Tasarım kararları (onayla birlikte kesinleşir)
- Hatırlatma ekleme, Plan sekmesindeki **+** düğmesinden açılır; Hatırlatma Sağlığı'na Plan'ın sağ üstünden gidilir.
- Saat seçimi önce hazır seçeneklerle; serbest tarih/saat "Başka zaman…" arkasında (sistem seçicisi).
- Tekrar (her gün, hafta içi…) bu taslakta yok: ilk sürümde yalnız tek seferlik ve "her gün"; ayrıntılı tekrar F3'te.
- Eksik ayar **amber** ile gösterilir; kırmızı yalnız kritik kartta ve kriz ekranında.

## Çizilmeyen durumlar (kodlanırken aynı dille yapılır)
Hatırlatma ekle: başlık boşken **Hatırlat** pasif. Plan: hiç hatırlatma yokken boş durum ("Henüz hatırlatman yok" +
"İlkini ekle"). Sağlık: her şey yerindeyken yeşil tek cümle. Sınama: "Geldi" sonucu (süreyle birlikte).

## Onay
- Seçilen: sekiz panonun tamamı, olduğu gibi ("taslakları beğendim").
- Onay tarihi: 10 Ekim 2026
- İstenen değişiklikler: yok.
- Koddaki farklar: tekrar seçimi yok (yalnız tek seferlik; "her gün" eklenmedi). Dört hazır zaman iki satıra
  bölündü (%200 yazı ölçeğinde taşmasın). ~~Telefon ekran görüntüsüyle yan yana karşılaştırma henüz yapılmadı.~~
  → (10 Ekim 2026 gece) Karşılaştırma yapıldı: Sağlık, sınama ve sihirbaz adımı ilk kodlamada taslaktan sapmıştı
  (proje beyni H42) ve taslağa çekildi; kalan farkların tam listesi devam taslağının README'sinde
  (`../2026-10-10-kurulum-sihirbazi/README.md` § Koddaki farklar).
