# 0017 — Uzak depo herkese açık

Tarih: 10 Ekim 2026  Durum: Kabul (Kullanıcı kararı: "gönder, herkese açık olsun, sorun değil")

**Bağlam:** 8 Ekim'de depo, belgelerde sağlıkla ilgili kişisel bağlam bulunduğu için gizli yapılmıştı (proje beyni
H19) ve "depo gizli kalır; push öncesi gizlilik denetlenir" kuralı konmuştu. 10 Ekim'de push öncesi denetim deponun
yeniden herkese açık olduğunu gösterdi (GitHub API: `visibility: public`). Push durduruldu, durum Kullanıcı'ya
bildirildi; Kullanıcı deponun açık kalmasını seçti.

**Karar:**
1. `mehmetark444-art/toparla` herkese açık kalır. "Depo gizli kalmalı" kuralı ve push öncesi 404 denetimi kalkar.
2. Değişmeyenler: `git push` yalnız Kullanıcı açıkça isteyince; API anahtarı, imza anahtarı ve parolası depoya
   girmez; push öncesi gizli değer taraması (izlenen dosyalar) sürer.
3. Depo açık olduğu için belgelere yazılanlar herkesçe okunur: sağlık verisi, ilaç adı, kişi adı, adres, telefon
   seri numarası ve ekran/bildirim içeriği belgelere, günlüklere ve taslaklara **girmez** (zaten kuraldı; artık
   tek koruma budur). Ölçümlerde ve taslaklarda yalnız uydurma örnek içerik kullanılır.

**Bedeli (Kullanıcı'ya söylendi):** proje beynindeki ve blueprint'teki kişisel bağlam (DEHB, odak alışkanlıklar,
telefon modeli, günlük düzen ayrıntıları) herkese açıktır ve git geçmişinde kalıcıdır; sonradan gizliye çevirmek
kopyalanmış olanı geri almaz.

**Denetim (10 Ekim, push öncesi):** izlenen dosyalarda ve tüm git geçmişinde API anahtarı deseni, parola satırı ve
gizli dosya yok (0 / 0 / 0); telefon seri numarası ve e-posta adresi depoda geçmiyor.

**Açık iş:** sohbete bir kez açık yazılmış eski Gemini anahtarının yenilenmesi (F2.18) depodan bağımsızdır ama
önceliği artmıştır.
