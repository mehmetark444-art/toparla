# 0018 — İlaç modülünün (M9) uygulama kararları ve biyometrik kilit izni

Tarih: 11 Ekim 2026  Durum: Kabul (izin ve ekranlar); cihazda doğrulama F2.39–F2.41 K2'sinde

**Bağlam:** Blueprint M9 ilacı "tam kodlanır, kapalı başlar" der (K18): Kritik sınıf hatırlatma, Aldım / 15 dk sonra /
Atlıyorum, çift doz koruması, kaçan dozda yalnız kayıt, stok, gizlenebilir bildirim metni, biyometrik kilit, CSV/PDF.
Nasıl yapılacağı ve birkaç uç durum açık kalmıştı. Kullanıcı F2-F'nin tamamlanmasını istedi ve ekran taslağını
onayladı (11 Ekim, `docs/tasarim/2026-10-11-ilac/`).

**Karar:**
1. **İlaç dozu motorun sıradan Kritik hatırlatmasıdır.** Her saat bir `Reminder` (`ownerType = MED`, kimlik
   `med:<ilaç>:<SSdd>`); planlayıcı, merdiven, bekçi, Direct Boot hepsi aynı. Motor yalnız eylemleri ilaca göre
   yorumlar: "Aldım" = DONE (çift doz denetimi), "15 dk sonra" = SNOOZE (en çok 30 dk), "Atlıyorum" = SKIP.
2. **Doz zamanı hiçbir düğmeyle değişmez:** ilaçta "Yarın" / "Yarına taşı" yok; üç ertelemeden sonra yalnız "Aldım" ya
   da "Atlıyorum" sorulur (taşımak bir zaman önerisi olurdu; M9 güvenlik çerçevesi).
3. **Çift doz koruması uyarır, engellemez:** asgari aralık (Kullanıcı seçer; Kapalı · 2 · 4 · 6 · 8 · 12 sa, yeni
   ilaçta 4 sa) dolmadan "Aldım" denirse "Son kayıt X önce. Emin misin?" (Evet, aldım / Vazgeç); soru yanıt sayılmaz,
   merdiven sürer.
4. **"Kaydedilmedi" +60 dk'da** (Bölüm I). Sonradan "Geç aldım" (durumu LATE) ya da "Atladım" kaydedilir; telafi dozu
   önerilmez. Kayıt, ertelense de dozun ilk planlanan anına yazılır; bir doz için tek kayıt.
5. **Stok:** "Aldım"da Kullanıcı'nın verdiği "her dozda" adedi düşülür (hesap değil, çıkarma); eşiğin geçildiği dozda bir
   kez Önemli sınıf "Eczaneye uğra" hatırlatması (görev tablosu F3'te geldiğinde göreve dönüşebilir).
6. **Gizlilik:** kilit ekranında ilaç adı hiç görünmez ("İlaç zamanı"); cihaz korumalı kopyaya (Direct Boot) ilaç
   başlığı yazılmaz; "Bildirimde adını gizle" bildirimin kendisini de genel metne çevirir.
7. **Modül kapalıyken** ilaç hatırlatmaları motora hiç görünmez (kurulmaz, gösterilmez, açık doz kapanır), Plan kartı
   ve ilaç ekranları görünmez; veri silinmez, açılınca kaldığı yerden sürer.
8. **Ayarlar → Modüller** (blueprint D20) şimdilik yalnız çalışan satırlarla açılır (İlaç takibi, İlaç ekranlarını
   kilitle, Hatırlatma Sağlığı); üst çubuktaki Ben/Ayarlar simgesinden girilir.
9. **Yeni izin `USE_BIOMETRIC`** (kurulumda verilen normal izin; çalışma zamanında hiçbir pencere açmaz). Neden: ilaç
   ekranlarına girişte isteğe bağlı kilit (M9). Nerede: yalnız "İlaç ekranlarını kilitle" açıksa, ilaç ekranlarına
   girerken sistemin parmak izi / yüz / ekran kilidi penceresi. Reddedilirse ya da vazgeçilirse: ilaç ekranları kilitli
   kalır, bildirim ve hatırlatmalar etkilenmez. Androidx kitaplığı eklenmez; çerçevenin `BiometricPrompt`'u kullanılır
   (minSdk 36).
10. **Dışa aktarım** cihazda üretilir; dosyanın yerini sistemin "dosya oluştur" ekranında Kullanıcı seçer (izin yok,
    uygulama dosyayı kimseye göndermez). CSV'de formül gibi başlayan hücre metne çevrilir.

**Alternatifler:** Ayrı bir ilaç alarm hattı (motorun güvenlik ağlarını kopyalamak gerekirdi; reddedildi) ·
`androidx.biometric` (sürüm doğrulaması bu oturumda yapılamıyor ve gereksiz; çerçeve API'si yeterli) · Çift dozu
engellemek (Kullanıcı'nın kendi planına karışmak olurdu; uyarı yeterli) · Dozun "Yarın"a taşınması (M9 çerçevesi dışı).

**Sonuçlar ve riskler:** İlaç dozları her kritik teslimde olduğu gibi `setAlarmClock` kullanır: sistemin "sıradaki
alarm" göstergesinde görünür. Biyometrik pencere ve dosya kaydetme ekranı yalnız telefonda denenebilir (bulut
oturumunda yazıldı; K2'de doğrulanacak). Şema sürümü 2'ye çıktı (otomatik geçiş; ilk gerçek migration testi
`MedicationDatabaseTest`, telefonda koşar). "Eczaneye uğra" şimdilik hatırlatmadır, görev değildir.

**İlgili blueprint bölümü:** K5, K7, K18, M9, D20, D25, G5, Bölüm I (Merdiven, Erteleme), H2, J1.
