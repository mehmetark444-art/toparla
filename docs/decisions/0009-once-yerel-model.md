# 0009 — Önce yerel model; APK boyut sınırı

Tarih: 8 Ekim 2026  Durum: Kabul (yönlendirme tablosunun ayrıntısı model ölçümünden sonra kesinleşir)

**Bağlam:** Blueprint F3 görev → katman tablosunda sohbet, planlama, görsel, Günün Mimarı, Mesaj
Yazarı gibi işlerin varsayılanı bulut (Katman 2), cihaz içi model yedekti. B8: APK ≤ 60 MB.
Kullanıcı (8 Ekim): "Gemini'yi en az kullanalım; gerçekten ihtiyaç olunca. Yerel modelin
yapabildiği her şeyden faydalanalım." ve "60 MB sınırını gevşetebilirsin, çok önemli değil."
Ayrıca: cihaz içi model Gemma olmak zorunda değil; en iyi Türkçe ve RAG sonucunu veren seçilir.

**Karar:**
1. **Yönlendirme tersine döner:** her AI görevinin varsayılanı Katman 1'dir (cihaz içi). Bulut yalnız
   şu durumlarda kullanılır:
   - iş cihazda yapılamaz: web araştırması (Konu Motoru), müfredat ve kalite sınavı üretimi;
   - cihaz içi çıktı doğrulayıcıdan iki kez geçemedi (şema, uzunluk, tıbbi sınır) ve veri Yeşil;
   - Kullanıcı açıkça istedi ("derin düşün", "daha iyi böl", "Derin ayrıştır");
   - görev için ölçülmüş kalite eşiği cihaz içi modelle tutmuyor (altın set sonucu; görev bazında
     `TaskSpec.minTier = 2` yalnız ölçümle verilir, varsayımla değil).
   Buluta her çıkış "Buluta ne gitti?" kaydında nedeniyle görünür.
2. Gizlilik renkleri, bütçe, Katman 0 önceliği ve "AI kapalıyken her şey çalışır" kuralı değişmez.
   **Kişisel (Sarı) veri içeren işte cihaz içi model yetersiz kalırsa iş cihazda kalır**; kendiliğinden
   buluta çıkılmaz. Kullanıcı o soruya özel "Gemini'ye sor" eylemiyle, neyin gideceğini görerek
   yönlendirir (Kullanıcı kararı, 8 Ekim 2026: "ben yönlendireyim"). Kategori bazlı kalıcı
   "buluta açık" anahtarları (K6) Ayarlar'da durmaya devam eder.
   **Rol dağılımı:** Gemini "öğretmen" (web araştırması, bilgi kartı yazımı, müfredat, yetkinlik
   sınavı hazırlama); cihaz içi model "günlük asistan" (kişisel veri, kısa ve sık işler, bilgi
   kartlarından yanıt). Bir konuda yetkinlik sınavı geçilince o konu cihaz içi modele devrolur.
3. Cihaz içi model seçimi karşılaştırmalı ölçümle yapılır (yol haritası F1.15, F1.23); aday listesi
   `platform-bulgulari.md`'de.
4. **APK sınırı** (model hariç) 60 MB'tan **150 MB**'a çıkar; sınır değil izleme eşiğidir.

**Alternatifler:** Blueprint'teki bulut-öncelikli tablo (Kullanıcı istemedi).

**Sonuçlar ve riskler:** Maliyet ve buluta giden veri azalır; çevrimdışı davranış ile çevrimiçi
davranış birbirine yaklaşır. Bedeli: 2–4 milyar parametrelik bir model sohbet, planlama ve Haftalık
Ayna'da bulut modelinden belirgin biçimde zayıf olabilir; yanıt daha yavaş gelebilir; pil ve ısı
artar (blueprint: pil < %15 ya da ısı yüksekken ağır işler ertelenir). Kalite eşikleri altın setle
ölçülmeden "yerel yeterli" denmez. Aylık 25 $ bütçe varsayılanı büyük olasılıkla fazla kalır.

**İlgili blueprint bölümü:** K4, B8, F2, F3, F11, M16; kararlar 0001, 0007
