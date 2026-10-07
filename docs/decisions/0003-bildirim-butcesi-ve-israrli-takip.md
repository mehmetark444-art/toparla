# 0003 — Bildirim bütçesi 10 ve ısrarlı takip

Tarih: 7 Ekim 2026  Durum: Kabul (susturan durumlar Kullanıcı tarafından aynı gün onaylandı)

**Bağlam:** Blueprint A5-3 proaktif bildirimi günde ≤ 8 (gözlem modunda ≤ 4) ile
sınırlıyor; Normal sınıf hatırlatma tek tekrar yapıyor, 3 ertelemeden sonra "yarına
taşıyayım mı?" soruyor. Kullanıcı: "günde bildirim sayısı en az 10 olsun ve bir şey
yapacağım dedim ve bana hatırlatıyorsa gün boyu peşimi bırakmasın, ben yaptım diyene
dek her yarım saatte bir sorsun."

## Karar 1 — Bütçe

- Günlük proaktif bildirim bütçesi varsayılan **10**; ayar aralığı 10–20.
- Gözlem modunda da bütçe 10'dur; gözlem modunun "yalnız sor biçimi" kuralı kalır.
- Konu özetinin 2 rezerve slotu bu 10'un içindedir.
- Saatlik üst sınır (≤ 2) ve bildirimler arası 30 dk kuralı kalır.
- Bu bir tavandır; Güneş söyleyecek bir şeyi yoksa sayıyı doldurmak için bildirim üretmez.

## Karar 2 — Israrlı takip (yeni hatırlatma davranışı, Katman 0)

**Kapsam:** Kullanıcı'nın açıkça üstlendiği işler:
1. Karar Defteri sözleri (M27) — bugüne denk gelenler.
2. Kullanıcı'nın kendi kurduğu ya da Güneş'in önerisini onayladığı görev hatırlatmaları.
3. Bugünün 3 önceliğinden hatırlatması olanlar.

Güneş'in kendiliğinden yaptığı dürtmeler, konu özetleri ve Ayna kartları kapsam dışıdır.

**Davranış:**
- `Reminder.persistent = true` (kapsamdakilerde varsayılan açık; hatırlatma başına kapatılabilir).
- İlk teslimden sonra **30 dakikada bir** tekrar (ayar: 15 / 30 / 60 dk), "Yaptım" denene kadar.
- Bildirim eylemleri: **Yaptım** · **Başla** (mikro-adım/zamanlayıcı açar; tekrar sürer) ·
  **Bugün olmayacak** (açık seçimle yarına taşır; sessizce kendiliğinden bitmez).
- Gün içinde tamamlanmazsa "Taşınan" olur ve ertesi gün uyanış saatinden itibaren devam eder.
- Günlük bütçeden, saatlik sınırdan ve 30 dk aralık kuralından **muaftır**.
- Metin her tekrarda havuzdan değişir (aynı cümle art arda gelmez); suçlayıcı olmaz.
- Aynı anda birden çok ısrarlı iş varsa tek birleşik bildirimde listelenir.

**Susturan durumlar (önerilen varsayılan):** uyku penceresi ve sessiz saatler (sabah
kaldığı yerden sürer) · kriz ve Bunaldım sonrası 3 saat · odak oturumu (bitince tek
bildirim) · Kullanıcı'nın kendi bastığı "Bugün sessiz" / "2 saat sessiz".

**Teknik:** `:domain/ReminderPlanner` içinde yeni merdiven türü `PERSISTENT`
(t0 → +30 dk → … → gün sonu; ertesi gün yeniden). Teslim hattı, idempotans, DeliveryLog
ve güvenlik ağları M6 ile aynıdır. `setExactAndAllowWhileIdle` (Önemli sınıf yolu) kullanılır.

**Alternatifler:** Blueprint'teki tek tekrar + akşam özeti (Kullanıcı reddetti).

**Sonuçlar ve riskler:** Bildirim yorgunluğu ve uygulamayı bırakma riski artar
(blueprint'in asıl kaygısı). Karşı önlemler: kapsam yalnız Kullanıcı'nın üstlendiği
işler, tek birleşik bildirim, değişen metin, "Bugün olmayacak" çıkışı, aralık ayarı.
Haftalık gözden geçirmede ısrarlı takibin "Yaptım" ile bitme oranı gösterilir.

**İlgili blueprint bölümü:** A2 (ölçüt tablosu), A5-3, K12, M6, M20/F8, M27, Bölüm I, D25
