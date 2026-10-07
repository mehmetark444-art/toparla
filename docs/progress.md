# İlerleme

## Dilim durumu

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
