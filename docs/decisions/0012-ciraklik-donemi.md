# 0012 — Çıraklık dönemi: yerel model Gemini'den öğrenir

Tarih: 8 Ekim 2026  Durum: Kabul (Kullanıcı kararı; mekanizmaların etkisi henüz ölçülmedi)

**Bağlam:** Karar 0009 "önce yerel", 0010 tek model Gemma 4 E4B, 0011 RAG. Ölçümler yerel modelin
güvenilir ama sınırlı olduğunu gösterdi (karta dayalı yanıtta ~%65–70; bölme ve ilk adımda hatalar).
Kullanıcı (8 Ekim): "Model yok dediğinde Gemini'ye sorsun; ilk birkaç hafta her soru, her iletişim
Güneş tarafından Gemini'ye yönlendirilip Gemini'den öğrenme gibi bir şey yapabilir miyiz?"
Kişisel veri için iki seçenek sunuldu; Kullanıcı **A**'yı seçti.

**Karar:**
1. **Ret → Gemini → kart.** Bilgi sorusunda cihaz içi model "yok" derse (ya da doğrulayıcıdan
   geçemezse) ve içerik Yeşil ise soru kendiliğinden Gemini'ye gider; yanıt kaynaklarıyla gösterilir ve
   **bilgi kartı önerisi** olarak kaydedilir ("diğer ifadeler" alanıyla, karar 0011). Aynı soru bir
   dahaki sefer cihazda yanıtlanır.
2. **Çıraklık dönemi** (varsayılan 3 hafta; Ayarlar'dan uzatılır, kısaltılır, kapatılır): bu sürede
   **kişisel olmayan (Yeşil)** her AI işi önce Gemini'ye gider ve yanıt oradan gelir. Öğrenme dört kanaldan:
   - **Bilgi:** Gemini'nin bilgi yanıtları karta dönüşür (madde 1).
   - **Örnek:** Gemini'nin görev çıktıları (bölme, mikro-adım, bildirim/özet metni…) girdi–çıktı
     çifti olarak saklanır (`TeacherExample`); cihaz içi model benzer işte en yakın örnekleri
     (gömmeyle bulunur) istemine örnek olarak alır.
   - **Karşılaştırma (gölge koşu):** saklanan girdiler gece konsolidasyonunda (şarjda) cihaz içi modelle
     de koşulur; iki çıktı görev başına kural tabanlı ölçütle karşılaştırılır (şema, etiket eşitliği,
     öğe sayısı; serbest metinde yalnız doğrulayıcı geçişi).
   - **Ağırlık (isteğe bağlı, sonra):** biriken çiftler K10'daki dışa aktarımla PC'de ince ayara girebilir.
3. **Devir kuralı:** bir görev türü, en az 30 gölge koşuda uyum ≥ %90 ise (eşikler `Defaults`'ta,
   ölçülerek ayarlanır) cihaz içi modele devredilir; süre dolması tek başına devir sebebi değildir,
   süre dolduğunda devredilmemiş görevler için karar 0009 kuralları geçerli olur. Devirler ve geri
   alınanlar haftalık gözden geçirmede "Güneş'in artık kendi yaptıkları" olarak gösterilir.
4. **Kişisel veri (Kullanıcı seçimi A):** Sarı veri içeren işler çıraklıkta da **cihazda kalır**;
   Kullanıcı istediği kategoriyi çıraklık süresince Ayarlar'dan buluta açabilir. Kırmızı veri ve ekran
   içeriği (karar 0008) hiçbir koşulda buluta gitmez. Her bulut çıkışı "Buluta ne gitti?" kaydında.
5. İnternet ya da bütçe yoksa çıraklık o iş için atlanır; cihaz içi model yanıtlar (hata gösterilmez).
   Çıraklık çağrıları aylık bütçeye dahildir; harcama ekranında ayrı satırdır.

**Alternatifler:** B — kişisel veri dahil her şeyin buluta gitmesi (Kullanıcı seçmedi).
Çıraklıksız "önce yerel" (öğrenme yalnız kullanımla; daha yavaş).

**Sonuçlar ve riskler:** Blueprint'te olmayan yeni bir mekanizma (yeni tablo, gölge koşu, devir
mantığı); F6–F7'ye iş ekler. İlk haftalarda yanıtlar internete bağlı ve maliyet daha yüksek (ölçülmedi).
Örneklerle istem büyür: yanıt süresi artabilir. **"Örnek" kanalının cihaz içi modelin kalitesini
gerçekten artırdığı henüz ölçülmedi**; F1.25'te sınanacak, etkisizse o kanal çıkarılır.
Model ağırlıkları değişmediği için "öğrenme" depolanan bilgi ve örneklerdedir; model değişse de kalır.

**İlgili blueprint bölümü:** K10, F3, F7, F9, F11, M18, M24; kararlar 0001, 0007, 0008, 0009, 0010, 0011
