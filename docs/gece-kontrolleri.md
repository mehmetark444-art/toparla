# Gece kontrolleri

Çalışılan her günün son işi: o gün yazılan her şeyin yeniden okunması, temizlik ve çift kontrol
(makine + göz). Usul: `/gece-kontrolu`. Her kontrol buraya bir satır ve gerekirse ayrıntı olarak yazılır.

| Tarih | İncelenen | Bulunan ve düzeltilen | Açık kalan | Testler | Sonuç |
|---|---|---|---|---|---|
| 8 Ekim 2026 (7 Ekim gecesi) | `4ca872c`…`38ff16d` (projenin tamamı) | 9 bulgu, hepsi düzeltildi (aşağıda) | 3 (aşağıda) | 58 birim · 21 kanca | Temiz |

## 8 Ekim 2026 — ilk kontrol (projenin tamamı)

**Yöntem:** izlenen 61 dosyanın tamamı okundu; tüm modüller uyarılar açıkken baştan derlendi;
iş kurallarına 12 kasıtlı bozma uygulanıp testlerin yakalayıp yakalamadığına bakıldı; kod kuralları
kancası gerçek bir düzenlemeyle denendi.

**Bulunan ve düzeltilen**
1. **Yakalanmayan bozma:** planlayıcıdaki "aynı anahtar, farklı zaman" karşılaştırması hiçbir testle
   korunmuyordu. Kendini onarma davranışı olarak belgelendi ve testi yazıldı.
2. **Doğrulama eksiği:** tekrar kuralları geçersiz değer kabul ediyordu (boş gün kümesi, ayın 0. ya da
   32. günü, 0 ya da 24+ saat aralık, ters pencere). Kurucu denetimleri ve testleri eklendi.
3. **Gereksiz kod:** `Defaults` içinde hiçbir yerde kullanılmayan iki sabit (odak alışkanlık sınırı,
   aylık AI bütçesi) silindi; ait oldukları fazda eklenecek.
4. **Gereksiz yapılandırma:** `gradle.properties` içinde AGP 9'da zaten varsayılan olan iki satır silindi.
5. **Kanca açığı:** gizli dosya `grep` ile ekrana basılabiliyordu; okuma komutları listesi genişletildi,
   değişkene alma biçimi serbest bırakıldı.
6. **Kanca yanlış alarmı:** `git stash push` de "push" sayılıp soruluyordu; yalnız `git push` alt komutu soruluyor.
7. **Taşınamaz sınama:** kanca sınamasında makineye özgü sabit yol vardı; çalışma dizininden türetiliyor.
8. **Bayat belge:** `platform-bulgulari.md`'de "henüz cihaz bulgusu yok", "telefon bağlı değil",
   "ayarı açması bekleniyor" satırları ve 18 satırlık eskimiş durum sütunu düzeltildi; durum için tek
   kaynak yol haritası.
9. **Eksik açıklama:** önemli sınıfın "akşam özetine taşı" adımının neden merdivende olmadığı koda not edildi.

**Doğrulanan (sorun çıkmayan)**
- Tüm modüller uyarısız derleniyor. 12 bozmanın 11'i ilk hâliyle, 12'ncisi yeni testle yakalanıyor.
- Kod kuralları kancası gerçek düzenlemede tetikleniyor ve ihlali bildiriyor.
- Ortam değişkenleri (`JAVA_HOME`, `MSYS_NO_PATHCONV`) ayar dosyasından geliyor.
- İzlenen dosyalarda gizli değer yok; belgelerde gösterilen bütün commit'ler gerçek.

**Açık kalan**
1. Oturum başlangıç kancası yalnız elle çalıştırılarak denendi; gerçek bir yeni oturumda bağlama
   girdiği henüz görülmedi (ilk yeni oturumda doğrulanacak).
2. Yeni yetenek ve alt ajanların oturumda listelendiği henüz görülmedi (aynı).
3. Karar 0002'deki yorum (yasak kelime listesinin tümden kaldırılması) Kullanıcı'ya iki kez
   bildirildi, itiraz gelmedi; açık "evet" henüz yok.

**8 Ekim sabahı güncelleme:** 1 ve 2 kapandı. Devam eden oturumda başlangıç kancası durumu bağlama
ekledi; 8 yetenek ve 3 alt ajan listelendi.
