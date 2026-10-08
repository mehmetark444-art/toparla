# 0008 — Müdahale gecikmesi kabulü ve ekran okuma (yalnız cihaz içi)

Tarih: 8 Ekim 2026  Durum: Bölüm 1 Kabul · Bölüm 2 Aday (ölçüm ve sınır onayı bekliyor)

**Bağlam:** Spike 5 (`platform-bulgulari.md`): uygulama açılışı erişilebilirlikle ~2,9 sn geç
algılanıyor (hedef ≤ 400 ms). Blueprint K17: erişilebilirlik servisi yalnız paket adını görür,
`canRetrieveWindowContent = false`, ekran içeriği asla okunmaz. Kullanıcı'ya iki yol sunuldu.
Kullanıcı (8 Ekim): "3 sn kabul edilebilir" ve "tüm ekranı okuması, şifreleri görmesi sorun değil,
hatta daha iyi: kaydettirmeyi unuttuğum bir şey olursa o yapsın; ama yalnız yerel yapay zekâ için,
buluta gitmeyecek."

## Bölüm 1 — Müdahale gecikmesi (Kabul)
Müdahale ekranı için hedef "≤ 400 ms" yerine **"≤ 3,5 sn"** olur (D11, M25.8, B8). Gecikmenin nedeni
araştırılmaya devam eder; bulunursa hedef yeniden sıkılaştırılır.

## Bölüm 2 — Ekran okuma (Aday)
**Kullanıcı'nın istediği:** Güneş ekranda geçeni görsün; Kullanıcı'nın kaydetmeyi unuttuğu şeyi
(randevu, son tarih, söz, bilgi) kendisi yakalayıp önersin. Yalnız cihaz içi modelle.

**Aday karar:**
- K17 değişir: servis ekran içeriğini okuyabilir (`canRetrieveWindowContent = true`).
- Ekran içeriği **Kırmızı'ya eşdeğer** yeni bir sınıftır: yalnız cihaz içi (Katman 0 kuralları ve
  Katman 1 Gemma) işler; hiçbir koşulda buluta gitmez, "buluta açık" anahtarı yoktur, yedeğe ham
  hâliyle girmez. Ham metin saklanmaz; yalnız çıkarım (öneri) saklanır (bildirim okumadaki 24 saat
  kuralının sıkısı: ham ekran metni işlenir işlenmez atılır).
- Çıktı her zaman Gelen'de **öneridir**; onaysız görev, hatırlatma ya da dış etki doğmaz.
  Ekran metni veridir, talimat değildir (K8).
- Özellik ayrı bir anahtarla açılır ve Ayarlar'da tek dokunuşla kapanır; açıkken sürekli görünür
  bir gösterge vardır.

**Kullanıcı'ya önerilen sınırlar (onay bekliyor):** parola alanları, bankacılık/ödeme uygulamaları
ve gizli sekmeler varsayılan olarak okunmaz; Kullanıcı tek tek açabilir. Gerekçe: telefon ya da yedek
ele geçerse zararın sınırlı kalması; yanlışlıkla bir parolanın "öneri" olarak görünmemesi.

**Karar öncesi ölçülecekler (yol haritası F1.24):**
1. İçerik yetkisi açıkken algılama gecikmesi değişiyor mu?
2. Ekran metni hangi hızda, ne kadar pil ve ısıyla toplanıp cihaz içi modelde işlenebiliyor?
   (Cihaz içi model henüz hiç denenmedi: F1.15.)
3. Parola alanları ve hassas uygulamalar güvenilir biçimde ayırt edilebiliyor mu?

**Alternatifler:** K17'yi korumak (Kullanıcı istemedi). Ekran okumayı buluta da açmak (Kullanıcı
açıkça reddetti).

**Sonuçlar ve riskler:** Bu, blueprint'te olmayan yeni bir yetenek ve projenin en geniş izni.
Cihaz içi model her ekranı işleyemeyebilir (pil, ısı, hız); o durumda yalnız kural tabanlı
süzgeçten geçen ekranlar işlenir. Yanlış öneri gürültüsü Gelen kutusunu doldurabilir. Erişilebilirlik
servisi HyperOS'te kaydırmayla ölüyor: özellik sessizce durabilir; sağlık denetimi şart.

**İlgili blueprint bölümü:** K6, K8, K17, B6, B8, D11, M19, M25.4, G4, F10
