# 0016 — Görsel dil: Seçenek B (açık palet koyulaştırıldı, yumuşak gölge, dört yazı boyutu)

Tarih: 9 Ekim 2026  Durum: Kabul (Kullanıcı kararı: "B'yi beğendim")

**Bağlam:** F2.47'de blueprint Bölüm C (Seçenek A) ile `mobile-app-ui-design` yeteneğinin önerisi (Seçenek B)
Şimdi, kritik hatırlatma ve Hatırlatma Sağlığı ekranlarında, koyu ve açık temada yan yana gösterildi
(`docs/tasarim/2026-10-09-gorsel-dil/`). Taslak hazırlanırken kontrast hesaplandı: blueprint'in **açık**
paletinde beş metin/zemin çifti kendi 4,5:1 kuralının (C2, C8) altında kalıyor: `onPrimary`/`primary` 3,81 ·
`primary`/`background` 3,56 · `carried`/`carriedContainer` 2,06 · `success`/`surface` 2,90 ·
`tertiary`/`surface` 3,94. Koyu ve AMOLED paletleri her çiftte geçiyor (en düşük 5,12).

**Karar:**
1. **Açık tema** jetonları değişir (koyu ve AMOLED değişmez):
   `primary #47705F` · `onSurfaceVariant #625B52` · `success #3E7553` · `tertiary #675E9C` · `critical #B3362A`.
   `carried #C9A04A` yalnız dolgu/çizgi rengi olarak kalır; "Taşınan" **yazısı ve ikonu** yeni jeton
   `onCarriedContainer` ile çizilir: açık `#7C5B12`, koyu `#E0BC6A`.
2. Vurgu ailesinin açık tondaki ana rengi otomatik kontrast testini geçecek kadar koyulaştırılır (F2.19);
   blueprint'teki vurgu tonları başlangıçtır, test geçmezse ton koyulaşır.
   **Ek (aynı gün, F2.19 yazılırken):** taslakta görünmeyen üç açık ton da aynı kuralla koyulaştırıldı:
   `info #506B88` (5,53), `urge #845D7E` (5,46), `secondary #876148`. Altı vurgunun açık ana tonları:
   Adaçayı `#47705F` · Gökyüzü `#466D8E` · Kil `#876148` · Lavanta `#6B6393` · Gül kurusu `#895C6A` ·
   Kum `#776846`. Türevler (`container`, `on`) önceden hesaplanıp sabit yazıldı (`ui/…/theme/Palette.kt`);
   `ContrastTest` 3 tema × 6 vurgu × 26 çifti sınar.
3. **Kart:** 1 dp `outline` sınır yerine sıcak tonlu, çok hafif yumuşak gölge + neredeyse görünmez sınır
   (koyu: beyazın %5'i; açık: adaçayının %7'si). Derin ya da gri gölge yok.
4. **Yazı:** bir ekranda en çok dört boyut ve iki ağırlık: 32 (Şimdi kartı başlığı, büyük sayı) · 20 (ekran ve
   kart başlığı) · 16 (gövde, düğme, chip) · 13 (açıklama, zaman, rozet). Ağırlıklar Regular ve SemiBold.
   Ekran başlığı 24 değil 20: ekranın kahramanı kart olur. Sayılar eşit genişlikli rakamla (`tnum`).
5. **Boşluk:** ekran kenarı ve kart içi 24 dp; bölümler arası 32 dp (8'in katları). 4 dp ızgara korunur.
6. **İkincil düğme ve metin eylemi:** `primary`'nin %8–10'u zemin; 48 dp kutu görünür.
7. **Güneş avatarı:** `sun` halkasına çok hafif sıcak ışık (bulanık, düşük opaklık); yanıp sönmez,
   "Animasyonları azalt" açıkken de durağandır.

Blueprint C'nin geri kalanı (renk aileleri, köşe ölçüleri, hareket, bileşen tanımları, DEHB ve erişilebilirlik
kuralları) aynen geçerlidir.

**Alternatifler:** Seçenek A (blueprint C aynen): açık temada beş çift okunaklılık sınırının altında; otomatik
kontrast testi (F2.20) zaten geçmezdi. Yalnız renkleri düzeltip gölge/yazı önerilerini almamak: Kullanıcı B'nin
bütününü seçti.

**Sonuçlar ve riskler:**
- `mono` ve `bodyL` gibi blueprint stilleri jetonda kalır ama ekranlarda dört boyut sınırı gözetilir;
  Geliştirici menüsü bu sınırdan muaftır.
- Gölge, AMOLED temada görünmez; orada sınır çizgisi biraz daha belirgin tutulur (F2.19).
- Telefonda gerçek görünüm F2.24 sonrasında ekran görüntüsüyle taslakla karşılaştırılır.

**İlgili blueprint bölümü:** C2, C3, C4, C6, C8
