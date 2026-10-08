---
paths:
  - "ui/**"
  - "app/**"
  - "docs/tasarim/**"
---

# Tasarım kuralları (karar 0015)

Kullanıcı'nın göreceği **her** yüzey (ekran, bileşen, bildirim görünümü, widget, kutucuk, boş/hata durumu,
onboarding) `mobile-app-ui-design` yeteneğiyle tasarlanır. Kod yazmadan önce yeteneği yükle ve 5 adımını izle:
bağlam → yapı (UX) → görsel (UI) → duygu (zirve ve bitiş) → cila.

## Akış: önce göster, sonra kodla
1. **Taslak:** ekran ya da ekran grubu önce tarayıcıda açılan bir görsel taslak olarak hazırlanır
   (telefon çerçevesinde, koyu + açık tema, gerçekçi Türkçe örnek içerikle; gerçek kişisel veri kullanılmaz).
2. **Kullanıcı onayı:** Kullanıcı taslağı görür; "olmuş / şurası değişsin" der. Onaysız ekran kodlanmaz.
3. **Uygulama:** onaylanan taslak Jetpack Compose ile `:ui` jetonları ve bileşenleri üzerinden yazılır.
   Yetenekteki React / Tailwind / Lucide notları yalnız **taslak** içindir; ürün kodu Compose + Material 3'tür.
4. **Karşılaştırma:** telefondaki ekran görüntüsü taslakla yan yana konur; fark varsa düzeltilir.

Taslaklar `docs/tasarim/` altında saklanır (ekran adı + tarih); onay tarihi dosyada yazar.

## Yetenek ile Toparla kuralları çelişirse
Aşağıdakiler ürünün güvenlik ve erişilebilirlik sınırlarıdır; yetenek bunları **gevşetemez**:

| Yetenek der ki | Toparla'da |
|---|---|
| Seri (streak), seviye, tamamlama yüzdesiyle "momentum" | **Yok.** Seri sayacı ve yüzde gösterilmez; "son 7 gün" şeridi kullanılır (K12, C7 `HabitStrip`) |
| Hata/yanlışta kırmızı; güçlü renkle vurgu | Kırmızı (`critical`) **yalnız** kriz ekranı ve kritik hatırlatma teslimi. Geciken iş "Taşınan" ve amber (`carried`) |
| Kutlama: rozet, kıvılcım, konfeti, karakter tepkisi | Sakin mikro-kutlama (kıvılcım, yaprak, dalga, ışık halkası; ≤ 600 ms); konfeti, maskot, yüzlü karakter yok. "Animasyonları azalt" açıkken yalnız metin + titreşim |
| Dokunma hedefi ≥ 44 pt | ≥ **48 dp**; birincil eylem 56 dp |
| Düşük kontrastlı "ambient" arayüz; metinde %60–70 opaklık | Metin/zemin kontrastı her temada ≥ **4,5:1** (büyük metin ≥ 3:1); opaklık bunu bozamaz |
| Emoji ve illüstrasyonla zenginleştir | Emoji yalnız ruh hali check-in'inde ve Kullanıcı açarsa Güneş mesajlarında |
| "Kimlik aynası" (paylaşım), "konfor tuzağı" (geçiş maliyeti) | Uygulanmaz: paylaşım ve bağımlılık artırıcı kalıp yok; tek kullanıcı, kendi iyiliği için |
| Parlama, cam efekti, neon, belirgin gölge | Güneş yüzeylerinde yalnız hafif, sıcak "ışık" hissi; parlak/neon yok; hiçbir şey yanıp sönmez |
| 375 px iPhone taban genişliği | Hedef cihaz Xiaomi 17T Pro; %200 yazı ölçeğinde bozulmayan düzen |

Ayrıca her zaman: her ekranda **tek** baskın eylem ve alt yarıda; durum yalnız renkle anlatılmaz (ikon + metin);
dil "Şimdi, Sonra, Bir gün, Taşınan" (asla "gecikmiş, kaçırılan, başarısız"); utandıran ya da suçlayan ifade yok.

## Görsel dil (renk, yazı, köşe, gölge)
Başlangıç noktası blueprint Bölüm C'dir (`ToparlaColors` jetonları, tipografi ölçeği, 4 dp ızgara). Yeteneğin
önerisi blueprint'in **görsel** bir ayrıntısından daha iyi sonuç veriyorsa iki seçenek taslakta yan yana
gösterilir ve **Kullanıcı seçer**; seçim blueprint'ten sapıyorsa karar kaydına yazılır. Bileşenler ham hex
kullanmaz; değişiklik jetonda yapılır.

## Tamamlama (ekran başına; `.claude/rules/android.md` listesine ek)
- Taslak Kullanıcı tarafından onaylandı ve `docs/tasarim/`'de.
- Yeteneğin kaçınılacaklar listesi tarandı (rastgele boşluk, eşit ağırlıklı bilgi, etiketin değerden büyük
  olması, başparmak bölgesi dışında birincil eylem, yönlendirmesiz boş durum).
- Boş, yükleniyor, hata, çevrimdışı, AI kapalı ve izin yok durumları da tasarlandı.
