# İlerleme — oturum günlüğü

> Güncel durum ve madde madde işaretleme için **`docs/yol-haritasi.md`** dosyasına bak.
> Bu dosya yalnız oturum günlüğüdür; aşağıdaki dilim tablosu ilk günkü hâliyle tarihçe olarak durur.

## Dilim durumu (7 Ekim 2026 itibarıyla; güncel hâli yol haritasında)

| Dilim | İçerik | "Bitti" ölçütü | Durum |
|---|---|---|---|
| **S0 Spike** | ADB + `kur.sh`; HyperOS ayarları; alarm testleri; tam ekran; FGS yolları; Tile; Erişilebilirlik gecikmesi; bildirim erişimi; geofence; Türkçe STT; Gemma kalite/hız/ısı; Gemini API akış + araç + arama temellendirme; 16 KB sayfa; FTS5; Health Connect; canlı güncelleme | Tüm `[DOĞRULA]` kapandı, bulgular yazıldı | **Devam ediyor** |
| S1 İskelet + Hatırlatma | Modül iskeleti, tasarım sistemi, `:reminders` (M6 + ısrarlı takip), Hatırlatma Sağlığı, ilaç (M9, kapalı flag) | Kritik teslim testleri geçti; 7 gün gerçek test | Bekliyor |
| S2 Günlük sürücü | M1, M2, M3, M5, M12, M13, kriz ekranı | Günü bununla yürüt (v0.1) | Bekliyor |
| S3 Ritim | M4, M7, M8, M10, M11, M14 | Sabah–akşam döngüsü tam | Bekliyor |
| S4 Alışkanlık | M25, M27, M28 | 1 edinme + 1 bırakma 7 gün izlendi | Bekliyor |
| S5 Güneş çekirdeği | M16, M17, M18, altın set koşucusu, sohbet ekranı | AI kapalıyken her şey çalışıyor; bölme F1 ≥ 0,90 | Bekliyor |
| S6 Bağlam | M19, M20 + gözlem modu, Beni Tanı görüşmesi, gece konsolidasyonu | 14 gün bütçe içinde, "faydalı" ≥ %50 | Bekliyor |
| S7 Koçluk | M21-1…13, M26 | Haftalık Ayna ilk gerçek örüntüyü buldu | Bekliyor |
| S8 Konu Motoru | M24 tam | 3 konu 7 gün, bütçe aşılmadı, okunma ≥ %50 | Bekliyor |
| S9 Dayanıklılık | M29, M30, yedek/geri yükleme, performans, pil, 30 gün stabilite | 30 gün ANR/çökme 0 | Bekliyor |

## Oturum günlüğü

### 7 Ekim 2026 — Oturum 1
**Hedef:** Blueprint'i ve v3'ü baştan sona okuyup boşlukları netleştirmek. Depoyu,
belge iskeletini ve ilk karar kayıtlarını kurmak.

**Biten:** Ortam kontrolü (Android Studio, SDK 36, adb mevcut); `git init`; belgeler
`docs/` altına taşındı; `CLAUDE.md`; karar kayıtları 0001–0005; S0 spike listesi.

**Sıradaki:** Gradle çok modüllü iskelet (sürümler resmi kaynaktan doğrulanıp
`libs.versions.toml`'a kilitlenecek) → ilk spike: alarm teslim testi.

**Ek (aynı oturum):** Gradle çok modüllü iskelet kuruldu (`:domain`, `:data`, `:reminders`,
`:ai`, `:sensors`, `:ui`, `:app`). Sürümler resmi depolardan doğrulandı: AGP 9.4.1,
Kotlin 2.4.20, Gradle 9.6.0, JUnit Jupiter 6.1.3. `./gradlew :domain:test :app:assembleDebug`
başarılı (3 test, boş debug APK). Sıradaki: ilk spike — alarm teslim testi (telefon bağlanınca).

### 7 Ekim 2026 — Oturum 1 (devam): S0 spike'ları ve S1 ön çalışması
**Biten:**
- Spike 1 (alarm teslimi): ekran açık/kapalı, kaydırıp kapatma, yeniden başlatma ve kilitli yeniden başlatma (Direct Boot) geçti. Gece testi (8 saat, 3 yöntem, 24 alarm) kuruldu; sonuç 8 Ekim sabahı okunacak.
- Spike 11 (Gemini API): anahtar Developer API ile çalışıyor, modeller listelendi; üretim çağrıları **402 bakiye bitti** engelinde.
- S1 ön çalışması (flag gerekmez, saf `:domain`): `ReminderPlanner` ilk kesit — ana teslimler, tekrar kuralları, 48 sa pencere, fark alma, 200 alarm sınırı, yaz saati kuralları; 20 birim testi.

**Açık:** gece testi sonucu · Gemini bakiyesi (Kullanıcı) · `setExactAndAllowWhileIdle` gecikmesinin tekrar testi → ısrarlı takibin alarm yolu kararı.

**Sıradaki:** gece kaydını oku → `ReminderPlanner`'a merdiven ve ısrarlı takip (karar 0003) → spike 3 (tam ekran bildirim) ve spike 5 (erişilebilirlik).

**Ek (23:15):** Gemini çağrıları bakiye yüklemesinden sonra da 402. Beklerken `:domain`'e merdiven (`Ladder`) ve ısrarlı takip zamanlaması (`PersistentFollowUp`, karar 0003: 30 dk aralık, uyku penceresi ve sessizlikte susar, sabah sürer) eklendi; toplam 37 birim testi geçiyor.

**Ek (23:40):** Gemini bakiyesi yanlis hesaba yuklenmis; Kullanici 8 Ekim'de dogru hesaba yukleyecek, spike 11 yarina kaldi. `:domain`'e cekirdek arayuzler (`Clock`, `RandomSource`, `IdGenerator`, `AppError`), teslim durum makinesi (`OccurrenceStateMachine`) ve erteleme kurallari (`SnoozePolicy`) eklendi; toplam 54 birim testi geciyor.

**Yarin (8 Ekim) sirasi:** gece kaydini oku -> israrli takibin alarm yolu karari -> Gemini spike (bakiye gelince) -> spike 3 (tam ekran) ve 5 (erisilebilirlik).

### 8 Ekim 2026 — Oturum 2 (akşam–gece)
**Hedef:** F1'in "A grubu" kısa telefon ölçümlerini bitirmek; ardından F1'de yapılabilecek her şeyi ölçmek.

**Biten:** A grubu (ayar bağlantıları, kutucuk, bildirim erişimi, kritik ses, canlı bildirim, arama, `kur.sh`);
C grubu (Room 3 + KSP, ALO 171, Gemini kalanları, konu maliyeti, çıraklık ön ölçümü, ekran okuma ön ölçümü);
kapanış ölçümleri (servis yolları, süreç ölümü ve otomatik başlatma, temizlik, saat değişimi). Karar 0014
(açık ölçümlerin devri). F2-A: F2.6–F2.9 (103 birim testi).

**Açık:** kablosuz gece testinin okunması · gürültüde ses tanıma · F1 kapanışı (K1–K3) · gece kontrolü ve push
(Kullanıcı işaretini bekliyor).

**Sıradaki:** F1 kapanışı → F2-B altyapı (Hilt, Room 3 tabloları, DataStore, `SecretStore`).

### 9 Ekim 2026 — Oturum 2 (gece, devam)
**Biten:** F2-B altyapı (F2.10–F2.15, F2.17 tam; F2.16 ve F2.18 Kullanıcı'ya bağlı kısımlar hariç); F0 → F2-B ara
denetimi (1 mantık hatası, 1 gizlilik eksiği düzeltildi; kayıt `gece-kontrolleri.md`). 114 JVM + 15 cihaz testi.

**Sıradaki:** F1 kapanışı (gece testi, gürültü, K1–K3) → F2-C tasarım sistemi.
