# Kurulum sihirbazı ve Hatırlatma Sağlığı eklemeleri taslağı (F2.35–F2.37 kalanları) — 10 Ekim 2026

**Durum:** Onaylandı (10 Ekim 2026) ve kodlandı.
**Görüntüle:** https://claude.ai/artifact/1HV7MSVcCG1qSiXGRWTV2Y (tuval; bu klasördeki dosyalar onun kopyasıdır).
**Yöntem:** `mobile-app-ui-design` yeteneğinin 5 adımı + `.claude/rules/tasarim.md` sınırları; görsel dil karar 0016
(Seçenek B). 9 Ekim taslağının (`../2026-10-09-hatirlatma-ekranlari/`) devamıdır: o taslağın satır, kart ve adım
kalıpları aynen kullanıldı; burada yalnız yeni yüzeyler var. Örnek içerik uydurmadır.

## Panolar
1. `Main.dc.html` / `SaglikAcik.dc.html` — **Hatırlatma Sağlığı**, yeni satırlarla (koyu ve açık). "Düzeltilecekler"de
   bildirim sesleri (kanal), "Yerinde olanlar"da arka plan önceliği (bekleme kovası), yeni **Son durum** bölümü:
   son hatırlatma (vaktinde / kaç dk geç), bekleyen hatırlatma, vaktinde ulaşmayan (son 7 gün), son deneme.
2. `SihirbazGiris.dc.html` — **Kurulum: başlangıç ve kalanlar.** Ne kadar süreceği ve adımların listesi (tamam olan
   işaretli, sıradaki belirgin). İlk açılışta bir kez kendiliğinden gösterilir; sonra Şimdi ekranındaki karttan açılır.
3. `SihirbazKilit.dc.html` — **Uygulamayı kilitle** adımı: üç yönerge ve son uygulamalar ekranının küçük resmi
   (blueprint G3 "görsel açıklama"). Ayar sayfası açılmaz; Kullanıcı "Kilitledim" der.
4. `SihirbazBitis.dc.html` — **Bitiş:** sakin onay işareti, tek cümle, yapılanların listesi, **Bitir**. Konfeti yok.
5. `SimdiKart.dc.html` — **Şimdi ekranında kurulum kartı** (blueprint D1-4: "Atlanırsa Şimdi ekranında sakin bir kart
   kalır"). Nötr renk, kalan adım sayısı, tek eylem **Sürdür**.
6. `SinaAcikti.dc.html` — **Sınama: ulaştı ama uygulama açıktı.** Deneme kapalı uygulamayı sınamadığını söyler;
   baskın eylem **Yeniden dene**.
7. `SinaGec.dc.html` — **Sınama: geç ulaştı.** Amber özet ve sıralı nedenler; başarı gibi gösterilmez.

## Tasarım kararları
- Sihirbazda yalnız bu telefonda gereken adımlar: bildirim izni, otomatik başlatma, pil kısıtlaması, uygulama kilidi,
  deneme. Kanal, tam vaktinde alarm ve tam ekran kart yalnız eksikse adım olur. Rahatsız Etme erişimi isteğe bağlıdır,
  sihirbaza girmez (Sağlık ekranında durur).
- Blueprint'in saydığı "Kilit ekranında göster" ve "Arka planda açılır pencere" adımları **yok**: F1 ölçümünde tam
  ekran kart bu izinler elle verilmeden kilit ekranında açıldı (`platform-bulgulari.md` § Spike 2 ve 3; tek deneme).
  Kart bir gün açılmazsa adım o zaman eklenir.
- Okunabilen ayar, ayar sayfasından dönünce kendiliğinden tamam sayılır. Okunamayan iki ayarı (otomatik başlatma,
  uygulama kilidi) yalnız Kullanıcı onaylar: otomatik başlatma adımında sayfa açıldıktan sonra eylem **Açtım** olur.
- "Bekleme kovası" Kullanıcı'ya "Arka plan önceliği" diye, değerleri "Kısıtsız / Yüksek / İyi / Orta / Düşük / Kısıtlı"
  diye gösterilir.
- "Vaktinde ulaşmayan" teslim hattının kaydıdır, Kullanıcı'nın değil; sıfırdan büyükse amber, asla kırmızı.

## Onay
- Seçilen: taslak olduğu gibi ("evet kabul ediyorum"). Onay anında ilk dört pano çizilmişti; son dördü (bitiş, Şimdi
  kartı, iki sınama sonucu) aynı dille hemen ardından eklendi ve Kullanıcı'ya bildirildi.
- Onay tarihi: 10 Ekim 2026
- İstenen değişiklikler: yok.

## Koddaki farklar (taslak ↔ uygulama; ekran görüntüsü testleri `app/src/test/screenshots/`)
- Sınama beklemesinde "saniye" yazısı halkanın içinde değil altında (ortak halka bileşeni tek metin taşır).
- Bitiş ekranındaki liste, kurulumda gösterilen bütün adımları sayar (taslakta dört satır; bildirim izni de listede).
- Sağlık özet kartının metni her eksikte aynı genel cümledir (taslaktaki örnek cümle o duruma özeldi).
- Sınamanın "ulaşmadı" ve "geç ulaştı" rehberinde yalnız gerçekten eksik olan ayarlar ve okunamayan otomatik
  başlatma listelenir; pil kısıtlaması yerindeyse satırı çıkmaz (9 Ekim taslağında üç sabit satır vardı).
- 9 Ekim taslağındaki "Aslında gelmişti" eylemi kodlanmadı: sonuç teslim kaydından okunur; Kullanıcı'nın beyanıyla
  "ulaştı" saymak kanıtsız bir sonucu doğru göstermek olurdu (proje beyni H38). Yerinde "Vazgeç" var.
- 9 Ekim taslağındaki "Bildirim gelince **Geldi** düğmesine bas" cümlesi yerine "Bildirim gelince uygulamayı yeniden
  aç; sonucu burada göstereceğim": bildirimde öyle bir düğme yok, sonuç kayıttan okunuyor.
- Plan listesinde "Seni bekleyenler" kartının iki düğmesi yan yana değil alt alta (iki kat yazıda "Bugün olmayacak"
  sığmıyor) ve "Sıradakiler" satırında **Sil** eylemi var (görev ayrıntısı ekranı F3'te gelene dek tek silme yolu).
