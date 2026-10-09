# Görsel dil taslağı (F2.47) — 9 Ekim 2026

**Durum:** Onaylandı — Seçenek B (9 Ekim 2026; karar 0016).
**Görüntüle:** https://claude.ai/artifact/NWvorVLq9amKHye9LXwWvu (tuval; bu klasördeki dosyalar onun kopyasıdır).
**Yöntem:** `mobile-app-ui-design` yeteneğinin 5 adımı + `.claude/rules/tasarim.md` sınırları. Örnek içerik uydurmadır.

## Panolar
Her pano dört telefon gösterir: Seçenek A (blueprint C) koyu + açık · Seçenek B (yetenek önerisi) koyu + açık.
1. `Main.dc.html` — Şimdi ekranı (D2): Şimdi kartı, 3 öncelik (biri Taşınan), 2 odak alışkanlık (son 7 gün şeridi), Yakala düğmesi, alt çubuk.
2. `Hatirlatma.dc.html` — Kritik hatırlatma tam ekranı (D25): kırmızının kullanıldığı tek yer.
3. `Saglik.dc.html` — Hatırlatma Sağlığı (D23): kontrol satırları, "Düzelt", "Hatırlatmaları sına".
4. `Kontrast.dc.html` — Okunaklılık karnesi (açık tema).

Telefon çerçevesi 412 × 915 dp **varsayımdır**; Xiaomi 17T Pro'nun gerçek dp ölçüsü F2.24'te ekran görüntüsüyle karşılaştırılacak.

## A ile B'nin farkı
| | A · Blueprint C | B · Yetenek önerisi |
|---|---|---|
| Renk (koyu) | Blueprint | Aynı (zaten her çiftte ≥ 5,1:1) |
| Renk (açık) | Blueprint; 5 çift 4,5:1 altında | Adaçayı `#47705F`, Taşınan yazısı `#7C5B12`, tamamlama `#3E7553`, lavanta `#675E9C`, ikincil metin `#625B52`, kritik `#B3362A` — hepsi ≥ 4,5:1 |
| Kart | 1 dp çizgi, gölge yok | Çok hafif, sıcak tonlu yumuşak gölge; çizgi neredeyse görünmez |
| Yazı | 8 boyut, 3 ağırlık; ekran başlığı 24 | Ekranda 4 boyut (32/20/16/13), 2 ağırlık; ekran başlığı 20 (kart öne çıkar); sayılar eşit genişlikte |
| Boşluk | Kenar 20, kart içi 20 | Kenar 24, kart içi 24 (8'in katları) |
| İkincil düğmeler | Düz metin | Adaçayının %8–10'u zemin; 48 dp kutu görünür |
| Güneş avatarı | İnce güneş halkası | Halka + çok hafif sıcak ışık (yanıp sönmez) |

Kontrast karnesi (açık tema, A → B): Başla yazısı 3,8 → 5,6 · yeşil metin 3,6 → 5,2 · Taşınan 2,1 → 5,3 ·
tamamlandı işareti 2,9 → 5,4 · lavanta 3,9 → 5,7. Blueprint'in açık paleti kendi 4,5:1 kuralını (C2, C8) geçemiyor;
F2.20'deki otomatik test bunu yakalardı.

Bu taslak yalnız **görsel dili** (renk, yazı, boşluk, kart, düğme) seçmek içindir. Ekranların boş, yükleniyor,
hata, çevrimdışı, AI kapalı ve izin yok durumları her ekranın kendi taslağında (F2.35–F2.38 ve sonrası) çizilir.

## Onay
- Seçilen: **B · Yetenek önerisi** (Kullanıcı: "B'yi beğendim")
- Onay tarihi: 9 Ekim 2026
- İstenen değişiklikler: yok
- Kayıt: karar 0016
