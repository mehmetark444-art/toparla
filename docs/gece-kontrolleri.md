# Gece kontrolleri

Çalışılan her günün son işi: o gün yazılan her şeyin yeniden okunması, temizlik ve çift kontrol
(makine + göz). Usul: `/gece-kontrolu`. Her kontrol buraya bir satır ve gerekirse ayrıntı olarak yazılır.

| Tarih | İncelenen | Bulunan ve düzeltilen | Açık kalan | Testler | Sonuç |
|---|---|---|---|---|---|
| 8 Ekim 2026 (7 Ekim gecesi) | `4ca872c`…`38ff16d` (projenin tamamı) | 9 bulgu, hepsi düzeltildi (aşağıda) | 3 (aşağıda) | 58 birim · 21 kanca | Temiz |
| 9 Ekim 2026 (bulut oturumu) | `3fc8c7f`…`16e0885` (23 commit, 74 dosya) | 3 bulgu + bayat belge satırları, hepsi düzeltildi (aşağıda) | 5 (aşağıda) | 114 JVM · kontrast 3 · ekran görüntüsü 16 (CI) · kanca sınaması | Temiz (CI yeşil olunca) |

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

## 9 Ekim 2026 — ara denetim (F0 → F2-B; Kullanıcı isteğiyle)

Kapsam: yol haritasında F0, F1, F2-A ve F2-B'de işaretli her madde; 8–9 Ekim'de yazılan kod, betik ve belgeler.
Bu, günün kapanış kontrolü değil, Kullanıcı'nın istediği ara denetimdir.

**Makine kontrolleri**
- Yol haritası kanıt taraması (her işaretli satırdaki commit, bulgu başlığı, sınıf ve dosya adı): 52 ☑ maddenin
  hepsinin kanıtı yerinde. Açık: 19 ◐, 5 ☐ (hepsi satırında gerekçeli; liste aşağıda).
- Kalite kapısı: 114 JVM testi, ktlint, detekt, Android Lint, debug ve imzalı release derlemesi, `:spike`
  derlemesi: geçti. Cihaz testleri: 15/15 (düzeltmelerden sonra yeniden koşuldu).
- Kanca sınaması: hepsi geçti. İzlenen dosyalarda gizli değer, imza anahtarı ya da model dosyası yok.
- Kasıtlı bozma: F2-A için 16/16, F2-B'nin saf kodu için 8/8 (ilk turda biri kaçtı; testi eklendi).

**Bulunan ve düzeltilen**
1. **Mantık hatası (H28):** planlayıcı vakti geçmiş alarm kayıtlarını iptal listesine koyuyordu; kayıt silinince
   teslim denetçisi çalmamış teslimi bulamazdı. Planlayıcı artık yalnız gelecekteki kayıtları iptal eder; testi yazıldı.
2. **Gizlilik eksiği:** uygulama verisi Android'in bulut yedeği ve "yeni telefona aktar" akışından yalnız eski
   `allowBackup` ile korunuyordu (Android 12+ için yetersiz). `data_extraction_rules.xml` ile her alan dışlandı.
3. **Dayanıklılık:** günlük dosyası çağıran (çoğunlukla ana) iş parçacığında yazılıyordu → tek arka plan sırası.
   Gizli değer anahtarı eşzamanlı ilk kullanımda iki kez üretilebilirdi → kilitlendi.
4. **Gereksiz kod (H29):** kullanılmayan ve testsiz 4 DAO sorgusu, 1 özellik anahtarı (`DAILY_DRIVER`), 1 yardımcı
   fonksiyon silindi; ait oldukları fazda eklenecek.
5. **Eksik test:** tekrar kuralı çözücüsü `ONCE:1` gibi artık eki olan metni kabul ediyordu → reddediliyor, testli.
6. **Bayat belge:** yol haritası güncelleme tarihi; proje beyninde sırası bozuk zaman çizelgesi, "58 test"
   yazan hızlı başlangıç, kapanmış açık sorular; F0.10'daki eski sayılar.

**Doğrulanan (sorun çıkmayan)**
- Karar kayıtları 0001–0014'ün hepsi `CLAUDE.md` özetinde ve proje beyninde; kodla çelişen karar yok
  (alarm yolları 0006, ısrarlı takip 0003, tek model 0010 belgelerde ve `:domain`'de tutarlı).
- F1'de "ölçüldü" yazan her maddenin `platform-bulgulari.md`'de yöntemi, ölçüm sayısı ve sınırları var;
  tek denemeler "tek deneme" diye işaretli.
- Devredilen ölçümlerin (karar 0014) her biri yol haritasında bir F2/F5/F6/F7 maddesine bağlı.

**Açık kalan (bilerek)**
1. F1 kapanmadı: kablosuz gece testi okunmadı (telefon bütün gece kabloya bağlı kaldıysa derin Doze yine
   ölçülmemiş olacak), gürültüde ses tanıma yapılmadı, K1–K3 işaretsiz.
2. Kullanıcı'nın ertelediği üç iş: imza anahtarının iki yerde yedeği (**tek kopya bu bilgisayarda**), API
   anahtarının yenilenmesi, push ve Actions'ın ilk koşusu.
3. `release` sürümü cihazda hiç çalıştırılmadı (ekran yok); `MissingApplicationIcon` Lint uyarısı F2.24'te kapanır.
4. İlk gerçek migration testi şema v2'ye çıkınca yazılabilir; şu an yalnız düzen ve v1 kurulumu testli.
5. GitHub Actions iş akışı hiç koşmadı (push yapılmadı): Linux'ta ilk koşuda sorun çıkabilir.
6. Karar 0002 (yasak kelime listesi yok) için açık "evet" ve karar 0008 (ekran okuma) için sınır onayı bekliyor.
7. 25 commit yalnız bu bilgisayarda.

## 9 Ekim 2026 — gece kontrolü (8 Ekim sabahından bu yana: `2ac3000..3fc8c7f`, 48 commit, 93 dosya)

8 Ekim gecesi kontrol yapılmamıştı (Kullanıcı günün bitmediğini söyledi); bu kontrol iki günü birlikte kapatır.
Aynı gece yapılan "ara denetim" (yukarıdaki kayıt) göz kontrolünün `:domain`, `:data`, `:app` ve yol haritası
kısmını kapsadı; burada kalanlar okundu: betikler, `:spike`, yapılandırma, yetenek dosyaları.

**Göz kontrolünde bulunan ve düzeltilen**
1. `:spike` `CaptureTile` açıklaması eski davranışı anlatıyordu (kilitliyken Activity açılır) → güncellendi.
2. `cihaz-testi` yeteneği bu iki günün derslerini taşımıyordu (H24, H25, H27, kısa bekleme, süreç ölümü sınaması) → eklendi.
3. (Ara denetimde) H28 mantık hatası, yedek dışlama kuralları, arka plan günlüğü, kasa kilidi, kullanılmayan kod,
   bayat belge satırları.

**Makine kontrolü**
- 114 JVM testi + 15 cihaz testi yeşil; ktlint, detekt, Android Lint, debug + imzalı release + `:spike` derlemesi geçti.
- Kanca sınaması: hepsi geçti. Tutarlılık denetimi: tutarlı. İzlenen dosyalarda gizli değer yok.
- Kasıtlı bozma (bugün değişen kurallar): 24/24 yakalanıyor (ilk turlarda 3'ü kaçtı; testleri yazıldı).
- Derleme uyarısı: 1 (Gradle 10'da kalkacak `ReportingExtension.file`; kalite eklentilerinden geliyor, bizim kodumuzdan değil).

**Açık kalan**
1. F1 kapanışı: kablosuz gece testinin okunması, gürültüde ses tanıma, K1–K3.
2. Kullanıcı'nın ertelediği: imza anahtarının iki yerde yedeği, API anahtarının yenilenmesi.
3. GitHub Actions'ın ilk koşusu (bu kontrolün ardından push ile başlayacak; sonucu doğrulanacak).
4. `release` sürümü cihazda çalıştırılmadı (ekran yok); `MissingApplicationIcon` uyarısı F2.24'te kapanır.
5. Karar 0002 için açık onay, karar 0008 için sınır onayı bekliyor.
6. Telefonda `:spike` içinde ekran okuma **ölçüm** servisi kurulu duruyor (kapalı); `:spike` F10.11'de silinir.

## 9 Ekim 2026 — gece kontrolü (bulut oturumu; F2-C: `3fc8c7f`…`16e0885`, 23 commit, 74 dosya)

Kullanıcı telefondan, bulut oturumundan çalıştı. Android derlemesi burada yapılamadığı için makine kontrolünün
derleme ve test kısmı GitHub Actions'ta koştu; saf kısım (renk ve kontrast) bulutta ayrı bir Gradle projesinde sınandı.

**Göz kontrolü:** `:ui` (tema 10 dosya, bileşen 13 dosya, ikonlar, galeriler, testler), `:app` (`MainActivity`,
gezinme, kaynaklar, manifest), `SettingsStore`, yapılandırma ve sürüm kataloğu, `check.yml` satır satır okundu.
Tasarım taslağı kopyaları (`docs/tasarim/**.dc.html`) ve üçüncü taraf yetenek dosyaları içerik olarak değil,
onay/kaynak kaydı olarak kontrol edildi.

**Bulunan ve düzeltilen**
1. **Kontrast:** "Geri al" şeridinde metin eylemi açık temada 4,19–4,30:1'di (sınır 4,5). Yarı saydam zemin
   `surfaceVariant` üstüne biniyordu; `ContrastTest` yalnız jeton çiftlerini ölçtüğü için kaçmıştı. Şeridin zemini
   `surface` yapıldı; test artık metin eyleminin bindirilmiş zeminini ölçüyor. Kasıtlı bozma: eski çift teste
   eklenince 6 vurgunun 6'sında da yakalandı, kaldırılınca geçti. 6 eylem görüntüsü gözle denetlenip yenilendi (H33).
2. **Durum çubuğu:** `enableEdgeToEdge()` simge rengini telefonun temasına göre seçiyordu; uygulama varsayılan koyu
   açıldığı için açık temalı telefonda saat ve pil görünmez olurdu. Artık uygulamanın temasından veriliyor (H33).
   Telefonda denenmedi.
3. **Gereksiz bağımlılık:** önizleme kütüphanesi (`ui-tooling-preview`) release'e de giriyordu; yalnız debug'a alındı.
4. **Bayat belge:** yol haritasında "Sıradaki tek adım" (ölçülmüş ve devredilmiş işleri bekliyordu), "Son güncelleme",
   F2.18 Actions satırı; proje beyninde "Şu anki durum" (uygulama "boş kabuk" diyordu). Hepsi güncellendi.
5. `versionCode` 26100901: ilk ekranlı sürüm telefona kurulurken eskisinin üstüne yazılabilsin diye.

**Bilerek bırakılan:** tasarım jetonlarının bir kısmı (`Motion` işlevleri, `Sizes.avatarLarge`, `Shapes.sheetTop`,
`Spacing.betweenCards` vb.) henüz kullanılmıyor. F2.19–F2.21'in teslimi blueprint C'nin jeton setinin kendisidir;
renkler `ContrastTest` ile sınanıyor. İlk kullanan ekran gelene dek silinmez.

**Makine kontrolü**
- CI (GitHub Actions): 114 JVM testi, ktlint, detekt, Android Lint, debug derlemesi geçti; görüntü karşılaştırması
  yalnız bilerek değişen 6 eylem görüntüsünde kırmızıydı (koşu 20); yeni temel görüntülerle sonraki koşu sonucu
  bu commit'in ardından okunur.
- Bulutta: `ContrastTest` 3/3 (saf Kotlin), ktlint ve detekt temiz, `kontrol.mjs --gece` tutarlı (yalnız iki bilinen
  uyarı: karar 0002 ve 0008 teyit bekliyor), kanca sınaması hepsi geçti.
- Gradle'ın `--warning-mode all` derlemesi ve cihaz testleri (15) bulutta koşulamadı.

**Açık kalan**
1. F1 kapanışı: F1.14 gürültüde ses tanıma, K1, K3 (telefon gerekir).
2. Kullanıcı'nın ertelediği: imza anahtarının iki yerde yedeği, API anahtarının yenilenmesi.
3. F2-C hiç telefonda çalıştırılmadı (iskelet, ikon, durum çubuğu, `release` + R8); bu dal ana dala alınmadı.
4. Cihaz testleri ve uyarılı tam derleme bilgisayar bağlanınca.
5. Karar 0002 için açık onay, karar 0008 için sınır onayı.
