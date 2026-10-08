# Toparla · Güneş — Proje Beyni

Bu dosya projenin hafızasıdır. Bağlamı sıfırlanmış bir oturum, başka bir AI ya da yeni bir
geliştirici **yalnız bu dosyayı okuyarak** projenin ne olduğunu, bugüne nasıl geldiğini,
nelerin denenip neden değiştiğini ve hangi hataların bir daha yapılmaması gerektiğini anlamalıdır.

**Son güncelleme:** 8 Ekim 2026, akşam · **Kapsadığı son commit:** `caa9a8a` · **Kapanan son faz:** F0

> **Zorunlu güncelleme kuralı:** Her faz kapanışında (ve fazı beklemeden: her karar kaydında,
> her yapılan hatada, her cihaz bulgusunda) bu dosya güncellenir. Bu dosya güncellenmeden
> hiçbir faz `yol-haritasi.md`'de ☑ işaretlenemez. Ayrıntılı kural: Bölüm 12.

## 0. Bu dosya ile diğerleri arasındaki iş bölümü

| Dosya | Sorusu | Not |
|---|---|---|
| `docs/BLUEPRINT.md` | Ne inşa ediyoruz? | Ürün tanımı (v4). **Değiştirilmez.** |
| `docs/decisions/NNNN-*.md` | Blueprint'ten nerede, neden saptık? | Karar kaydı blueprint'i ezer. |
| `docs/yol-haritasi.md` | Neredeyiz, sırada ne var? | Tek durum kaynağı; madde madde işaretlenir. |
| `docs/proje-beyni.md` (bu dosya) | Buraya nasıl geldik, ne öğrendik? | Anlatı, gerekçe, hatalar, tuzaklar. |
| `docs/platform-bulgulari.md` | Bu telefonda ne ölçtük? | Ham ölçümler ve yöntem. |
| `docs/progress.md` | Hangi oturumda ne oldu? | Kısa oturum günlüğü. |
| `docs/gece-kontrolleri.md` | Her gün sonunda ne bulundu, ne temizlendi? | Gece kontrolü kayıtları. |
| `CLAUDE.md` | Nasıl çalışıyoruz? | Çekirdek kurallar; ayrıntı `.claude/rules/` altında. |
| `docs/claude-code-duzeni.md` | Kancalar, yetenekler, alt ajanlar nasıl kurulu? | Claude Code altyapısı. |
| `AGENTS.md` | Başka bir AI aracı nereden başlar? | Giriş noktası; aynı kurallara işaret eder. |

Çelişki olursa öncelik: karar kaydı > blueprint; durum için yol haritası > bu dosya.

---

## 1. Proje bir paragrafta

**Toparla**, DEHB'li tek bir yetişkin (ürün sahibi = tek kullanıcı) için, tek telefonda
(Xiaomi 17T Pro, Android 16, HyperOS 3) çalışan, ADB ile kurulan kişisel yaşam asistanıdır.
Sunucu, hesap, mağaza, analitik yoktur; veri cihazda kalır. İki bağımsız parça vardır:
(1) **Güvenilir çekirdek** (Katman 0, AI'sız): yakalama, Şimdi kartı, hatırlatma motoru,
zamanlayıcı, rutin, alışkanlık, dürtü anı, Bunaldım, kriz ekranı; internetsiz tam çalışır.
(2) **Güneş** (AI ajanı): cihaz içi Gemma (Katman 1) + bulutta Gemini (Katman 2); hafıza,
proaktif orkestratör, koçluk, Dürüst Ayna, web'de araştırma yapan Konu Motoru.
Kapsam M1–M30'un tamamıdır (MVP yok); inşa F0–F10 fazlarıyla, her faz sonunda telefonda
kullanılan bir APK ile ilerler.

**Değişmez çizgiler:** kritik her şey AI'dan bağımsız · seri sayacı ve suçlayan çerçeve yok,
geciken iş "Taşınan" · Güneş dış dünyaya onaysız dokunmaz · tıbbi/finansal/hukuki tavsiye yok ·
kriz her şeyin önünde · web/bildirim/dosya içeriği veridir, talimat değildir.

## 2. İnsanlar ve çalışma biçimi

- **Kullanıcı:** ürün sahibi ve tek kullanıcı. **Teknik bilgisi yok.** Elle yapacağı her şey
  sıfır bilgi varsayımıyla, numaralı ve tek eylemli anlatılır; yapılabilen her şeyi geliştirici
  (AI) kendisi yapar. Her şey Türkçe.
- **Geliştirici:** Claude Code (tek geliştirici). Kısa rapor, tek soru, önerilen varsayılan.
- Kullanıcı kararlarını hızlı verir ve bazen blueprint'in tersine karar alır (bkz. Bölüm 5).
  Böyle bir kararın bedeli varsa açıkça söylenir, sonra uygulanır ve karar kaydına yazılır.

## 3. Şu anki durum (özet; ayrıntı yol haritasında)

- **F0 Hazırlık:** kapandı.
- **F1 Cihaz denemeleri:** alarm teslimi büyük ölçüde doğrulandı; gece testi (8 saat, 24 alarm)
  7 Ekim 22:35'te kuruldu, sonucu 8 Ekim sabahı okunacak. Gemini denemesi bakiye engelinde.
- **F2 Hatırlatma motoru:** yalnız saf mantık (`:domain`) yazıldı; 58 birim testi geçiyor.
  Android tarafı (`:reminders`), veritabanı ve arayüz **henüz yok**.
- Telefonda çalışan tek şey atılacak deneme uygulaması (`:spike`, "Toparla Spike").
  Asıl uygulama (`:app`) boş bir kabuktur.

## 4. Mimari ve depo

```
toparla/
├─ CLAUDE.md, AGENTS.md      çalışma protokolü; başka AI araçları için giriş
├─ .claude/                  settings.json (izin, ortam, kanca) · hooks/ · rules/ · skills/ · agents/
├─ scripts/adb               adb sarmalayıcısı (PATH ve yol çevirme sorununu çözer)
├─ docs/                     BLUEPRINT, decisions/, yol-haritasi, proje-beyni, platform-bulgulari, …
├─ gradle/libs.versions.toml sürümler (resmi kaynaktan doğrulanıp kilitlenir)
├─ domain/    saf Kotlin/JVM: iş kuralları, Android sınıfı YOK      ← şu an kod burada
├─ data/      Room, DataStore, repository                            (boş)
├─ reminders/ AlarmManager, bildirim, FGS, alıcılar                  (boş)
├─ ai/        LlmClient, Gemini ve yerel istemci, yönlendirici, ajan (boş)
├─ sensors/   duyargalar, ContextHub, erişilebilirlik                (boş)
├─ ui/        tasarım sistemi                                        (boş)
├─ app/       tek Activity, feature/*, manifest                      (boş kabuk)
└─ spike/     S0 cihaz denemeleri; ürüne bağımlı değil, F10'da silinir
```

Bağımlılık tek yönlü: `app → {ui, sensors, ai, reminders, data} → domain`.
`applicationId`: `com.toparla.app` (debug: `.dev`), spike: `com.toparla.spike`.

**Sürümler (7 Ekim 2026'da resmi depolardan doğrulandı):** AGP 9.4.1 · Gradle 9.6.0 ·
Kotlin 2.4.20 · JUnit Jupiter 6.1.3 · `compileSdk = minSdk = targetSdk = 36`.

**`:domain` içindekiler (`com.toparla.domain`):**
- `Defaults` — adlandırılmış varsayılanlar (bildirim bütçesi 10, ısrarlı takip 30 dk, …).
- `core/` — `Clock`, `RandomSource`, `IdGenerator`, `AppError`.
- `reminder/ReminderPlanner` — saf fonksiyon `plan(now, tanımlar, mevcut) → kurulacaklar + iptaller`;
  5 tekrar kuralı, 48 sa pencere, 200 alarm sınırı (kritik düşmez), yaz saati kuralları.
- `reminder/Ladder` — sınıfa göre yükselme basamakları.
- `reminder/PersistentFollowUp` — ısrarlı takipte "bir sonraki soru ne zaman?".
- `reminder/OccurrenceStateMachine`, `SnoozePolicy` — teslim durumu ve erteleme.

## 5. Kararlar ve gerekçeleri

Blueprint'in kilitli kararları (K1–K23) geçerlidir; aşağıdakiler onları **değiştirenler**.

| # | Karar | Neden | Bedeli / dikkat |
|---|---|---|---|
| 0001 | Bulut katmanı Claude değil **Gemini API** | Kullanıcı kararı; anahtarı Gemini | Blueprint'teki Claude/Anthropic/Sonnet/Haiku/Opus ifadeleri Gemini karşılığıyla okunur. Web arama → Google Arama temellendirmesi. Kademe modelleri henüz kilitlenmedi. |
| 0002 | **Yasak kelime listesi yok** | Kullanıcı: "yasak kelime olmasın". Liste zaten blueprint'in kendi onaylı metinleriyle çakışıyordu ("Bugün hâlâ senin günün") | LLM'in ürettiği kırıcı ifade artık otomatik yakalanmıyor; koruma prompt yönergesi + "Bu beni kırdı" + valf. Seri sayacı yok / "Taşınan" kuralları **duruyor**. |
| 0003 | Bildirim bütçesi **10** (aralık 10–20); **ısrarlı takip**: Kullanıcı'nın üstlendiği iş "Yaptım" denene dek 30 dk'da bir sorulur | Kullanıcı: "en az 10 olsun… peşimi bırakmasın" | Blueprint'in bildirim yorgunluğu kaygısının tersine. Sınırlar Kullanıcı onaylı: uyku/sessiz saat, kriz/Bunaldım sonrası 3 sa, odak oturumu ve "Bugün sessiz"de susar, sabah sürer. Bütçeden muaf. |
| 0004 | İlk odak alışkanlıklar: **Sigara (tam bırakma) + Uyku Ritmi** | Kullanıcı seçimi bana bıraktı; sigara en net hedef, uyku diğer her şeyin temeli | Diğer dört alışkanlık "izleniyor"; dürtme almaz. |
| 0008 | Müdahale gecikmesi hedefi ≤ 3,5 sn (**kabul**). Ekran okuma (**aday**): Güneş ekranı okuyup kaydedilmemiş şeyleri önersin; yalnız cihaz içi, asla bulut | Kullanıcı 3 sn'yi kabul etti ve ekran okumayı kendisi istedi ("şifreleri görmesi sorun değil, yalnız yerel model") | Blueprint K17'nin tersi ve kapsam dışı yeni yetenek; projenin en geniş izni. Cihaz içi model henüz denenmedi; uygulanabilirlik bilinmiyor. Parola/banka sınırları Kullanıcı onayı bekliyor. |
| 0007 | Gemini kademeleri: hızlı `gemini-3.5-flash-lite`, günlük `gemini-3.8-flash` (etkileşimde düşünme `low`), derin `gemini-3.1-pro-preview`; takma ad yok | Ölçüm + resmi fiyat sayfası | Derin kademe önizleme modeli; günlük kademe fiyatı 1 Ocak 2027'de iki katı; Türkçe kalite henüz ölçülmedi. |
| 0006 | Alarm yolu: Kritik `setAlarmClock`; Önemli, **Normal** ve ısrarlı takip `setExactAndAllowWhileIdle`; esnek yol yalnız Bilgi | Gece testi: esnek yol 2–5 saat geç çaldı, ekran açıkken bile; kesin yollar ≤ 28 sn | Blueprint'ten sapma (Normal esnekti). Kesin alarm sayısı artar; pil F10'da ölçülür. Derin Doze henüz sınanmadı. |
| 0005 | Dil Türkçe (tanımlayıcılar İngilizce) · yedek parolayla şifreli · bekleme günlerinde sonraki faz flag arkasında · CI şimdilik yerel · v3'te eksik şemaları ben tasarlarım · JBR 21 ile derleme | Netleştirme turu 2 | — |

**Bekleyen karar adayları**
- ~~**Sınıf → alarm yolu.**~~ → kapandı: karar 0006 (8 Ekim 2026). Eski not: Bulgu: `setAlarmClock` her koşulda ±0,2 sn; `setExactAndAllowWhileIdle`
  bir kez 3,5 dk gecikti. Aday: ±1 dk sözü verilen her şey (kritik + ısrarlı takip)
  `setAlarmClock`. Karar 0003 şu an ısrarlı takip için `setExactAndAllowWhileIdle` diyor;
  gece testi sonucuna göre güncellenecek. `ReminderPlanner.apiFor` tek değişim noktası.
- ~~**Gemini kademe modelleri ve fiyat tablosu**~~ → kapandı: karar 0007 (8 Ekim 2026).
- **Konu Motoru varsayılan sıklığı** (maliyet ölçümünden sonra; aylık 25 $'ın %40'ı yetmeyebilir).

## 6. Zaman çizelgesi (ne yapıldı, hangi sırayla)

### 7 Ekim 2026 — Oturum 1
1. **Okuma ve boşluk analizi.** Blueprint v4 (2087 satır) ve v3 (2003 satır) baştan sona okundu.
   Bulunanlar: v3 dosyası §11.5'te kesik; yasak kelime listesi kendi metinleriyle çakışıyor;
   gözlem modunda bütçe çelişkisi; hava durumu çelişkisi; eksik izinler, tablolar, ekranlar.
2. **Netleştirme turu 2.** Kullanıcı yanıtları → kararlar 0001–0005.
3. **Ortam.** Android Studio 2025.2.2, SDK 36, adb kurulu bulundu; ek kurulum gerekmedi.
4. **Depo.** `git init`, belgeler `docs/`'a taşındı, `CLAUDE.md`, belge iskeleti. `4ca872c`
5. **Gradle iskeleti.** 7 modül, sürüm kataloğu; boş APK derlendi. `65c129f`
6. **Spike 1 (alarm teslimi).** `:spike` uygulaması yazıldı (`c38080e`); ölçümler:
   ekran açık/kapalı, kaydırıp kapatma, yeniden başlatma, kilitli yeniden başlatma geçti
   (`3963fc6`, `2e76340`, `26b59eb`, `56fb79d`); gece testi kuruldu (`4773d52`).
7. **Spike 11 (Gemini).** Anahtar Developer API ile çalışıyor; üretim çağrıları 402. `d9c7dd9`
8. **Hatırlatma saf mantığı.** Planlayıcı (`05ebe61`), merdiven + ısrarlı takip (`8a970bf`),
   durum makinesi + erteleme + çekirdek arayüzler (`0d39466`).
9. **Yol haritası** (`0cf287b`, `8a0d633`) ve bu dosya (`81f1c19`).

### 8 Ekim 2026 — Oturum 1 (gece yarısından sonra)
10. **Claude Code altyapısı.** Resmi belgeler okunup eksikler çıkarıldı; o ana dek yalnız `CLAUDE.md` vardı.
    Kurulanlar: `.claude/settings.json` (ortam, izinler), 4 kanca (oturum başlangıcı, dosya koruma +
    faz kapısı, komut koruma, kod kuralları) ve 17 senaryoluk sınaması, 5 yola göre kural dosyası,
    7 yetenek, 3 salt okunur denetçi alt ajan, `AGENTS.md`, `scripts/adb`. `CLAUDE.md` kısaltıldı;
    kod kuralları ve tamamlama tanımı kural dosyalarına taşındı. Ayrıntı: `docs/claude-code-duzeni.md`.
    Önemli sonuç: "blueprint değişmez", "gizli değer depoya girmez", "push sorulur" ve "faz, proje
    beyni güncellenmeden kapanmaz" kuralları artık talimat değil, **kancayla zorunlu**. (`38ff16d`)
11. **İlk gece kontrolü ve gece kontrolü usulü.** Projenin tamamı satır satır yeniden okundu; 9 bulgu
    düzeltildi (yakalanmayan bozma, eksik kurucu denetimleri, kullanılmayan sabitler, gereksiz Gradle
    satırları, iki kanca açığı, bayat belge satırları). Bundan sonra çalışılan her günün son işi
    `/gece-kontrolu`; sonuçlar `docs/gece-kontrolleri.md`'de. Birim testi 58, kanca sınaması 21. (`2ac3000`)
12. **GitHub'a ilk push.** Depo herkese açık çıktı; Kullanıcı gizli yaptı. Bilgisayardaki varsayılan
    GitHub girişi başka hesap olduğu için uzak adres kullanıcı adıyla tanımlandı; 18 commit gönderildi,
    yerel ve uzak eşit (`6bc3304`), kimliksiz erişim 404.
13. **Gece testi okundu, karar 0006.** 24/24 alarm çaldı; esnek yol saatlerce geç kaldığı için Normal
    sınıf kesin yola alındı (`ReminderPlanner.apiFor`). Yeni oturumda başlangıç kancasının bağlama
    girdiği, 8 yeteneğin ve 3 alt ajanın listelendiği de görüldü (gece kontrolünün iki açık maddesi kapandı). (`f43db56`)
14. **Gemini ölçümleri, karar 0007.** Bakiye geldi (çağrıyla doğrulandı). Üç kademe, düşünme ayarı,
    şemalı çıktı, işlev çağrısı, arama temellendirmesi ve akış tek çağrılarla ölçüldü; kademeler ve
    fiyatlar kilitlendi. Görsel girdi, önbellekleme ve gerçek konu taraması henüz denenmedi. (`0d2f7ab`)
15. **Spike 2 ve 3.** Deneme uygulamasına servis ve tam ekran kartı eklendi; alarmdan servis başlatma
    ve kilitliyken tam ekran bildirim tek denemede çalıştı. Kalan koşullar yol haritasında açık. (`b203ef6`)
16. **Spike 5 (erişilebilirlik).** İki olumsuz bulgu: kaydırınca servis ölüp geri gelmiyor (`caa9a8a`);
    uygulama açılışı ~2,9 sn geç algılanıyor. Olumlu: servisten kart açma hızlı ve engelsiz.
    Üç yapılandırma değişikliği gecikmeyi düzeltmedi; ölçüm betiği `scripts/spike-a11y-olc.sh`.

## 7. Bu telefonda öğrenilenler (özet; ham veri `platform-bulgulari.md`)

- **Cihaz:** `2602EPTC0G` / `warhol_global`, Android 16 (API 36), HyperOS `OS3.0.310.0.WPSMIXM`,
  `arm64-v8a`, sayfa boyutu 4096 (16 KB değil).
- **`setAlarmClock` güvenilir:** ekran açık/kapalı, kaydırıp kapatma, yeniden başlatma ve
  kilitli yeniden başlatmada 12–153 ms sapma.
- **`setExactAndAllowWhileIdle` bir kez 211 sn gecikti** (ekran kapalı, kova zorlanırken). Tek ölçüm;
  gece testindeki 8 ölçümde tekrarlanmadı (en çok 28 sn). Kök neden bilinmiyor.
- **Gece testi (8 saat, 24 alarm, özel izin yok):** hepsi çaldı. `setAlarmClock` ≤ 1,4 sn,
  `setExactAndAllowWhileIdle` ≤ 28 sn. **`setAndAllowWhileIdle` 2–5 saat geç**, ekran açıkken bile;
  zamanlı hiçbir işte kullanılmaz (karar 0006). Derin Doze alarm anında hiç gözlenmedi: o koşul açık.
- **Kaydırıp kapatma zorla durdurma değildir:** HyperOS 3'te alarmlar korunur (`stopped=false`).
  Bu, otomatik başlatma / pil muafiyeti **verilmeden** gözlendi (tek deneme, 2 dk ufuk).
- **Zorla durdurma** alarmları siler; uygulama yeniden açılınca sistem `BOOT_COMPLETED`
  yayınlarını gönderir ve alıcı her şeyi yeniden kurar. Açılana kadar alarm yoktur.
- **Direct Boot çalışıyor:** kilit açılmadan (`RUNNING_LOCKED`) `LOCKED_BOOT_COMPLETED` gelir,
  `directBootAware` alıcı cihaz korumalı depolamadan alarmları kurar, alarm kilitliyken çalar.
  Yeniden başlatmadan yeniden kuruluma ~30–45 sn.
- **Kurulum:** `adb install` için Geliştirici seçenekleri → "USB ile yükle" açık olmalı ve
  telefondaki onay penceresi ~10 sn içinde onaylanmalı; yoksa `INSTALL_FAILED_USER_RESTRICTED`.
- **USB bağlıyken Doze zorlanamıyor** (`force-idle` → "stopped at INACTIVE"; cihaz şarjda sayılıyor).
  `am set-standby-bucket … rare` kalıcı olmuyor.
- **Erişilebilirlik servisi kaydırmayla ölüyor (8 Ekim):** uygulama son uygulamalardan kaldırılınca
  HyperOS süreci öldürüyor (`SwipeUpClean`); servis "Crashed" durumuna düşüyor, ayarda açık görünüyor
  ama olay almıyor ve uygulama yeniden başlasa da geri bağlanmıyor. Müdahale ekranının (M25-I)
  en büyük riski bu; önlemler sınanmadan o özellik tasarlanmaz.
- **Uygulama açılışı ~2,9 sn geç algılanıyor (8 Ekim, 21 ölçüm):** olay servise ulaştıktan sonra kart
  ~70 ms'de çiziliyor, ama açılışta olayın kendisi ~2,9 sn geç geliyor (açılış olmayan geçişlerde
  ~100 ms). Sürekli servis, pencere bayrağı ve ek olay türleri değiştirmedi; neden bilinmiyor.
  Blueprint'in ≤ 400 ms hedefi bu yapılandırmada tutmuyor. Servis paket güncellemesinden sonra
  kendiliğinden bağlanıyor; paket süzgeci bağlanma anında sızdırıyor (kodda da denetle).
- **Alarmdan servis ve tam ekran (8 Ekim, tek denemeler):** `setAlarmClock` alıcısından `specialUse`
  foreground service başlatılabiliyor (29 ms). Kilitliyken tam ekran bildirim, HyperOS'e özgü hiçbir
  izin elle verilmeden kilit ekranının üstünde açılıyor ve ekranı uyandırıyor (270 ms). Blueprint'in
  kritik teslim hattı (alarm → servis → tam ekran) bu telefonda kurulabilir görünüyor.
- **Gemini ölçümleri (8 Ekim, tek çağrılar):** flash-lite ~1 sn; 3.8-flash varsayılan düşünmeyle
  ~8 sn, `thinkingLevel:"low"` ile ~2 sn; pro ~11 sn. Düşünme tokenleri çıktı fiyatından ücretlenir.
  Şemalı çıktı, işlev çağrısı, akış ve Google Arama temellendirmesi çalışıyor. Arama atıflarındaki
  `uri` yönlendirme adresidir (gerçek URL değil); alan adı `title`'da. Arama ayda 5 000 ücretsiz:
  Konu Motoru'nun arama maliyeti kaygısı (Bölüm 10, madde 5) büyük ölçüde kalktı.
- **Gemini:** uç nokta `generativelanguage.googleapis.com/v1beta`, başlık `x-goog-api-key`.
  Vertex AI projede kapalı (gerek yok). Bakiye **faturalandırma hesabına** bağlı; bakiye bitince
  her çağrı HTTP 402 döner, ücretsiz model yok. Ürün 402'yi `AiUnavailable` saymalı.

## 8. Yapılan hatalar ve çıkarılan dersler

Her satır gerçekten yaşandı. Aynı hatayı tekrarlamadan önce burayı oku.

| # | Ne oldu | Kök neden | Ders / kural |
|---|---|---|---|
| H1 | `local.properties`'e `C\:\Users\…` yazıldı; SDK yolu bozuldu | `.properties` biçiminde `\` kaçış karakteridir | Windows yollarını `.properties` dosyasına **ileri eğik çizgiyle** yaz (`C:/Users/…`). |
| H2 | Test betiği telefondaki kaydı okuyamadı, boşuna bekledi | Git Bash, `adb shell` argümanındaki `/data/…` yolunu `C:/Program Files/Git/data/…`'ya çeviriyor | `adb shell` içeren her betikte `export MSYS_NO_PATHCONV=1`. `kur.sh` dahil. |
| H3 | Çok satırlı bash komutları "unexpected EOF" ile hiç çalışmadı (2 kez) | Araç kabuğu, heredoc içindeki kesme işaretini (`Kullanıcı'nın`) tırnak sanıyor | Türkçe metin ya da kesme işareti içeren dosyaları heredoc ile değil **dosya yazma aracıyla** oluştur. |
| H4 | Arka plan betiği durdurulamadı | Git Bash'te `pkill` yok | `ps -ef` ile PID bulup `kill`; ya da arka plan görevini araçla durdur. |
| H5 | Betik yanlış alarmları "beklenen alarm" saydı, test adımları birbirine girdi | Bekleme koşulu toplam `FIRED` sayısına bakıyordu; önceki turdan kalan alarmlar da sayıldı | Bekleme koşulunu **belirli anahtara** bağla; teste başlamadan bekleyen alarm olmadığını doğrula. |
| H6 | Ölçüm kaydı kirlendi: benim kurmadığım alarmlar, silinmiş günlük | Test sırasında Kullanıcı deneme uygulamasının düğmelerine bastı; ben önceden "dokunma" demedim | Cihaz testinden **önce** Kullanıcı'ya neye dokunmayacağını ve ne kadar süreceğini yaz. Kirli veriyi sonuç sayma. |
| H7 | "Kilitli yeniden başlatma" testi geçti sanıldı; aslında kilitli durum hiç oluşmamıştı | Telefonda ekran kilidi yoktu (`CredentialType: NONE`); ön koşul doğrulanmadan teste girildi | Her testte **ön koşulu komutla doğrula** ve sonucu yorumlamadan önce kanıta bak (`BOOT_COMPLETED`'in hemen gelmesi ipucuydu). Kullanıcı "geldi" dese de kaydı oku. |
| H8 | Kilitliyken kayıt okunamadı | `run-as`, kimlik korumalı dizin kilitliyken çalışmaz | Kilitli dönemde `logcat -s ETIKET` kullan; ürün tanılaması kilitli dönem kayıtlarını cihaz korumalı depolamaya yazmalı. |
| H9 | İlk kurulum denemeleri reddedildi | HyperOS onay penceresi; bir kez de Kullanıcı yanlışlıkla reddetti | Kurulumdan önce Kullanıcı'yı uyar; `Success` görmeden devam etme; betikte bu hatayı yakala. |
| H10 | Gemini bakiyesi yüklendi denildi, çağrılar yine 402 | Bakiye başka Google hesabına yüklenmişti | "Yaptım" bildirimini çağrıyla doğrula; bakiyenin **hangi projeye/hesaba** bağlı olduğunu adımda açıkça yaz. |
| H11 | API anahtarı sohbete açık yazıldı | Kullanıcı güvenlik sonucunu bilmiyordu | Anahtar yalnız gitignore'daki `secrets.properties`'te; depoya girmediği her commit öncesi `git grep` ile doğrulanır. **Depo GitHub'a taşınmadan anahtar yenilenecek** (yol haritası F2.18). |
| H12 | Karar 0003'te ısrarlı takip için alarm yolu ölçümden önce seçildi | Varsayım, spike'tan önce yazıldı | Platforma bağlı seçimi karar kaydına **ölçümden sonra** yaz; öncesinde "aday" de. |
| H13 | Yol haritasının ilk hâlinde M16, M23, M24 etiketi yoktu | İçerik vardı, izlenebilirlik etiketi unutuldu | Kapsam belgesi yazınca M1–M30'u komutla tara; artık `dogrula/kontrol.mjs` her koşuda denetliyor. |
| H14 | Kancaları sınayan komut, kancanın kendisi tarafından engellendi | Sınama girdisi ("gizli dosyayı ekrana bas" örneği) komut metninin içindeydi; kanca yeni yazılır yazılmaz devreye girdi | Kanca sınama girdilerini komut satırına değil **betik dosyasına** koy (`.claude/hooks/sinama.sh`). Kancalar aynı oturumda hemen etkin olur. |
| H16 | Betik dosyası `/tmp/…` yoluyla Node'a verildi, "modül bulunamadı" hatası alındı | `MSYS_NO_PATHCONV=1` artık oturum genelinde açık; Git Bash yolları Windows programlarına **çevrilmeden** gidiyor (H2'nin çözümünün yan etkisi) | Windows programına (node, java, adb.exe) yol verirken göreli yol kullan ya da `cygpath -w` ile çevir. |
| H17 | Planlayıcıdaki bir karşılaştırma hiçbir testle korunmuyordu; ilk gece kontrolünde kasıtlı bozmayla ortaya çıktı | Testler "geçiyor" diye yeterli sayıldı; yakalayıp yakalamadıkları sınanmamıştı | İş kuralı değişen her gün gece kontrolünde kasıtlı bozma yapılır; yakalanmayan bozma = eksik test ya da gereksiz kod. |
| H18 | Kullanılmayan sabitler ve geçersiz girdiyi kabul eden veri sınıfları ilk yazımda fark edilmedi | "İleride lazım olur" eklemesi; kurucu denetimi düşünülmedi | Kullanılmayan şey eklenmez (ait olduğu fazda gelir); dışarıdan değer alan her model geçersiz değeri kurucuda reddeder. |
| H19 | Kullanıcı "GitHub'a pushla" dedi; depo **herkese açıktı** ve belgelerde sağlıkla ilgili kişisel bilgiler vardı | Depo açılırken görünürlük seçilmemiş; Kullanıcı sonucunu bilmiyordu | Dışarıya yayın öncesi hedefin görünürlüğünü komutla doğrula (kimliksiz istek 200 = açık, 404 = gizli) ve neyin görüneceğini Kullanıcı'ya söyle. Push'tan önce Kullanıcı depoyu gizli yaptı. |
| H20 | Gizli yapılan depoya push "Repository not found" verdi | Bilgisayarda kayıtlı GitHub girişi başka hesaptı (Emire221); gizli depoyu göremiyordu. Depo açıkken `ls-remote` çalıştığı için fark edilmemişti | Push öncesi kayıtlı hesabı kontrol et (`git credential-manager github list`). Çözüm: uzak adrese kullanıcı adı eklendi; Kullanıcı doğru hesapla giriş yaptı. |
| H21 | Cihaz betiğinin ilk komutları "no devices" verdi; kayıt başlangıç satırı 0 okundu ve bütün günlük ekrana döküldü | adb arka plan süreci yeniden başlarken ilk birkaç komut cihazı görmüyor; betik bağlantıyı beklemeden ölçüm başlangıcını aldı | Cihaz betiğinin ilk satırı `./scripts/adb wait-for-device`; başlangıç değeri okunamazsa (boş ya da 0) betik dursun. Ölçüm bu kez etkilenmedi. |
| H15 | Yol haritası birkaç kez `sed`/`awk` ile değiştirildi | Alışkanlık; o sırada kanca yoktu | Kabuk komutuyla yapılan değişiklik düzenleme kancalarından (faz kapısı, gizli değer) geçmez. Yol haritası ve proje beyni **yalnız düzenleme aracıyla** değiştirilir. |

## 9. Tuzaklar ve "bunu bilmeden başlama" notları

- **Derleme:** sistemde `JAVA_HOME` tanımsız; Claude Code oturumlarında `.claude/settings.json`
  verir. Başka ortamda: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
  `adb` PATH'te değil: `./scripts/adb …` kullan.
- **Kancalar etkin:** blueprint ve arşiv düzenlenemez; gizli değer kalıbı içeren içerik yazılamaz
  ve commit'lenemez; `secrets.properties` ekrana basılamaz (değişkene alınır); `git push` sorulur;
  proje beyninde kapanış kaydı olmayan faz ☑ yapılamaz; Kotlin kural ihlali düzenleme sonrası
  bildirilir. Bir işlem "hook" hatasıyla durursa bu bir arıza değil, kuraldır: nedenini oku.
- **Kanca ve ayar değişikliği** sonrası `bash .claude/hooks/sinama.sh`; depo tutarlılığı için
  `node .claude/skills/dogrula/kontrol.mjs`.
- **AGP 9:** Kotlin yerleşiktir; Android modüllerine `kotlin-android` eklentisi **eklenmez**.
  Yalnız saf JVM modülü (`:domain`) `kotlin("jvm")` kullanır. AGP 9.4, Gradle 9.6.0 ister.
- **Android Studio 2025.2.2**, AGP 9.4 projesini açmak için eski olabilir (doğrulanmadı).
  Derleme komut satırından yapılır; Kullanıcı Studio kullanmıyor.
- **Blueprint'te geçen ama geçersiz olanlar:** Claude/Anthropic (→ Gemini, karar 0001);
  yasak ifade listesi ve `forbidden_tr.json` (→ yok, karar 0002); bildirim ≤ 8 / gözlem ≤ 4
  (→ 10, karar 0003); D16'daki "hava açık" (hava verisi yok); "v3 §11'deki şemalar korunur"
  (v3'te o şemalar yok; biz tasarlayacağız).
- **`:domain` kuralları:** Android sınıfı yok; `System.currentTimeMillis()` / `Instant.now()` yok
  (zaman parametre ya da `Clock` ile gelir); `!!` yok. `:spike` bu kurallardan bilerek muaftır.
- **Git:** ~~her şey yerelde, hiç push yapılmadı~~ → 8 Ekim 2026'da ilk push yapıldı. Uzak depo
  `mehmetark444-art/toparla`, **gizli**. Bu bilgisayarın varsayılan GitHub girişi başka bir hesap
  (Emire221); bu yüzden uzak adres kullanıcı adıyla tanımlı
  (`https://mehmetark444-art@github.com/…`). Adresi sadeleştirme: push "depo bulunamadı" verir.
  Push yalnız Kullanıcı isteyince (kanca her seferinde sorar). GitHub Actions henüz kurulmadı;
  API anahtarı henüz yenilenmedi (depoda anahtar yok, geçmiş tarandı).
- **Depo gizli kalmalı:** belgeler Kullanıcı'nın sağlıkla ilgili kişisel bilgilerini içeriyor.
  Görünürlüğü değiştirmeden ya da başka bir yere yayınlamadan önce Kullanıcı'ya açıkça sor.
- **Telefonun ekran kilidi** 7 Ekim 22:08'de kaldırılmış, sonra PIN olarak geri kondu.
  Direct Boot'a bağlı her test öncesi `dumpsys lock_settings | grep CredentialType` ile doğrula.
- **Kullanıcı'nın "oldu / geldi / yükledim" demesi kanıt değildir;** her seferinde kayıttan doğrula
  (H7 ve H10 bu yüzden yakalandı).

## 10. Açık sorular ve riskler

1. `setExactAndAllowWhileIdle` gecikmesinin kök nedeni (kova mı, pil politikası mı, HyperOS mu)?
2. Gerçek Doze'da ve uzun ufukta (1–8 sa) üç alarm yolunun davranışı → gece testi.
3. Erişilebilirlikle uygulama açılışı algılama ≤ 400 ms tutacak mı; HyperOS servisi öldürüyor mu?
4. Cihaz içi model: LiteRT-LM + Gemma'nın güncel adları, Türkçe kalitesi, model dosyasının
   nasıl edinileceği (sunucumuz yok; Kullanıcı elle indirecek).
5. Konu Motoru maliyeti: 5 konu × günde 2 tarama aylık konu payına sığacak mı?
6. Israrlı takip + bütçe 10, blueprint'in "bildirim yorgunluğu → uygulamayı bırakma" riskini
   büyütüyor. Haftalık gözden geçirmede "Yaptım ile bitme oranı" izlenecek.
7. KSP / Room / Hilt'in Kotlin 2.4.20 + AGP 9.4.1 ile uyumu henüz denenmedi.
8. Kriz sözlüğü Kullanıcı incelemesi olmadan sürüme giremez.

## 11. Yeni oturum için hızlı başlangıç

1. Bu dosyayı oku (10 dk), sonra `docs/yol-haritasi.md`'nin en üstünü ("Şu an", "Sıradaki tek adım").
2. `git log --oneline -15` ve `docs/progress.md`'nin son girişi.
3. İlgili blueprint bölümü + `docs/decisions/`.
4. Doğrula: `./gradlew :domain:test` (58 test yeşil olmalı) ve `node .claude/skills/dogrula/kontrol.mjs`.
5. Telefon gerekiyorsa: `adb devices` → `device` görünmeli; kilit durumu ve "USB ile yükle" açık.

## 12. Güncelleme kuralı (zorunlu)

**Ne zaman:**
- **Her faz kapanışında** (zorunlu kapı): faz ☑ işaretlenmeden önce.
- Her yeni karar kaydında → Bölüm 5.
- Her yapılan hatada, yanlış varsayımda, geri alınan işte → Bölüm 8 (aynı gün).
- Her cihaz bulgusunda → Bölüm 7 (özet; ham veri `platform-bulgulari.md`'ye).
- Mimari, modül, sürüm ya da çalışma biçimi değiştiğinde → Bölüm 4 ve 9.

**Faz kapanışında yapılacaklar (kontrol listesi):**
1. Üstteki "Son güncelleme", "Kapsadığı son commit", "Kapanan son faz".
2. Bölüm 3 (şu anki durum) yeniden yazılır.
3. Bölüm 4: yeni modüller, sınıflar, sürümler.
4. Bölüm 5: fazda alınan kararlar; kapanan "bekleyen karar adayları".
5. Bölüm 6: fazın zaman çizelgesi girişi (ne yapıldı, hangi commit'ler).
6. Bölüm 7–9: yeni bulgular, **fazda yapılan her hata**, yeni tuzaklar.
7. Bölüm 10: kapanan sorular silinmez, "kapandı: …" diye işaretlenir; yenileri eklenir.
8. Bölüm 13'e faz kapanış kaydı eklenir.

**Yazım kuralı:** Geçmiş silinmez, üstüne yazılmaz; yanlış çıkan bilgi "~~eski~~ → yeni (tarih)"
biçiminde düzeltilir. Hatalar kimseyi suçlamak için değil, tekrarlanmasın diye yazılır.
API anahtarı, parola ve kişisel sağlık verisi bu dosyaya **asla** girmez.

## 13. Faz kapanış kayıtları

| Faz | Kapanış | Son commit | Beyin güncellendi | Not |
|---|---|---|---|---|
| F0 | 7 Ekim 2026 | `8a0d633` | ☑ (ilk sürüm) | K1 ve K2 geçti. |
