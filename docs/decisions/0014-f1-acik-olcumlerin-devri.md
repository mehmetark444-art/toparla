# 0014 — F1'de açık kalan ölçümlerin sonraki fazlara devri

Tarih: 8 Ekim 2026  Durum: Kabul (Kullanıcı kararı: "onaylıyorum devret")

**Bağlam:** Yol haritası F1'in kapanış şartı (K1) her spike satırının ölçülmüş olmasını istiyor. 8 Ekim gecesi
itibarıyla F1'in büyük kısmı ölçüldü; kalanların bir bölümü şu an ölçülemiyor: Kullanıcı evden çıkamıyor
(konum), Mi Band siparişte, bir ölçüm günlerce bekleme istiyor, bazı küçük koşullar da ancak gerçek kullanımda
doğal olarak oluşuyor. F1 bu yüzden açık kalırsa F2'nin Android tarafı (F2-D) başlayamıyor.

**Karar:** Aşağıdaki ölçümler F1'den çıkarılır ve yazılı olarak ilgili faza **devredilir**. Devredilen madde
"ölçüldü" sayılmaz; devredildiği fazın çift kontrolünde ölçülür ve o faz bu ölçüm yapılmadan kapanmaz.

| Devredilen ölçüm | F1 maddesi | Devredildiği yer |
|---|---|---|
| Konum / geofence: Play Hizmetleri, arka plan olay gecikmesi | F1.12 | F7.1 başlamadan önce (F7 ön koşulu) |
| Health Connect: Mi Band → Mi Fitness → uyku/adım akışı | F1.13 | F7.1 başlamadan önce (F7 ön koşulu) |
| Kısıtlı bekleme kovası (uygulama günlerce açılmadan) altında alarm teslimi | F1.1 | F2 K2 (7 günlük gerçek kullanım) |
| `setExactAndAllowWhileIdle` 3,5 dk gecikmesinin kök nedeni | F1.1 | F2 K2: teslim günlüğünde tekrar ederse araştırılır |
| Kilitsiz uygulamada Güvenlik temizliği sonrası teslim; saat dilimi değişimi | F1.1 | F2 K1 cihaz matrisi |
| Rahatsız Etme erişimi verilmemişken ve "tam sessizlik" kipinde kritik ses | F1.4 | F2.34 |
| Ayar sayfalarının doğru uygulamayı gösterdiğinin göz doğrulaması; gri tonlama bağlantısı | F1.5 | F2.37 (kurulum sihirbazı) |
| `kur.sh`: APK kurulum adımı, temiz kurulumda tam koşu, iki `appops` adımının izin istendiğinde davranışı | F1.6 | F2.17 |
| Erişilebilirlik: kilidin yeniden başlatma sonrası kalması, uzun süre ömrü, ~2,9 sn gecikmenin nedeni | F1.7 | F5.7 (müdahale ekranı) |
| Bildirim erişimi: `allow_listener` komutunun sıfırdan etkisi | F1.8 | F2.17 |
| Arama: gelen aramanın çalma anı | F1.11 | F2.30 (ısrarlı takibi susturan durumlar) |
| Konuşma tanıma: kulaklık mikrofonu, 2–3 dk kesintisiz konuşma | F1.14 | F3.6 |
| Cihaz içi model: düşünme kipiyle karta dayalı yanıt, çok turlu sohbet, görsel/ses, kablosuz pil ve ısı | F1.15 | F6.3 ve F6.13 |
| Gemini: 429 / hız sınırı gövdesi | F1.16 | F6.2 |
| Ekran okuma: kablosuz pil ve ısı, metinden modelin öneri çıkarması | F1.24 | Karar 0008'in sınır onayı verilirse açılacak iş; verilmezse düşer |

**F1'de kalan ve kapanıştan önce yapılacaklar:** kablosuz gece testinin okunması (derin Doze, 9 Ekim sabahı) ve
gürültülü ortamda ses tanıma (F1.14, 9 Ekim, iş yeri). Ardından K1, K2 (Kullanıcı onayı), K3.

**Alternatifler:** F1'i her madde ölçülene kadar açık tutmak: Mi Band ve günler isteyen ölçüm yüzünden F2-D en
az bir hafta bekler; bu sürede ölçülecek şeylerin çoğu zaten F2'nin gerçek kullanım sınamasında ortaya çıkar.

**Sonuçlar ve riskler:** Birkaç platform varsayımı kod yazılırken hâlâ ölçülmemiş olacak (en önemlisi kısıtlı
kova altında teslim). Azaltma: bu varsayımlara dayanan kod arayüz arkasında yazılır; F2 K2'nin teslim günlüğü
ölçümü zaten bu koşulu gerçek kullanımda sınar; devredilen her satır yol haritasında adıyla durur. Blueprint'in
"Tüm `[DOĞRULA]` kapandı" kabul ölçütünden sapmadır.

**İlgili blueprint bölümü:** A (S0 kabul ölçütü), G1, G3, G4, G6, M19.6
