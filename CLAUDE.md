# CLAUDE.md — Toparla · Güneş çalışma protokolü

Kaynak: `docs/BLUEPRINT.md` Bölüm A6. Blueprint değiştirilmez; ondan sapan her karar
`docs/decisions/` altındadır ve **karar kaydı blueprint'i ezer**.

## Proje
DEHB'li tek bir kullanıcı için, tek telefonda (Xiaomi 17T Pro, Android 16, HyperOS 3) çalışan,
sunucusuz kişisel yaşam asistanı. İki bağımsız parça: AI'sız güvenilir çekirdek (Katman 0) ve
AI ajanı **Güneş** (cihaz içi Gemma + bulutta Gemini). Kapsam M1–M30'un tamamı; fazlar F0–F10.

## Belgeler (hangisi neyi yanıtlar)
| Dosya | Soru |
|---|---|
| `docs/proje-beyni.md` | Buraya nasıl geldik, ne öğrendik, hangi hatalar yapıldı? (hafıza) |
| `docs/yol-haritasi.md` | Neredeyiz, sırada ne var? (tek durum kaynağı) |
| `docs/decisions/` | Blueprint'ten nerede, neden saptık? |
| `docs/BLUEPRINT.md` | Ne inşa ediyoruz? (değiştirilmez; yalnız ilgili bölümü oku) |
| `docs/platform-bulgulari.md` | Bu telefonda ne ölçtük? |
| `docs/claude-code-duzeni.md` | Kancalar, yetenekler, alt ajanlar, kurallar nasıl kurulu? |

## Blueprint'i ezen kararlar (özet)
- **0001:** Bulut katmanı Anthropic Claude değil **Google Gemini API**. Blueprint'te
  "Claude / Anthropic / Sonnet / Haiku / Opus" geçen her yer buna göre okunur.
- **0002:** Yasak kelime listesi ve kelime tabanlı ton doğrulayıcısı **yok**.
- **0003:** Günlük proaktif bildirim bütçesi **10** (alt sınır 10). **Israrlı takip:** Kullanıcı'nın
  üstlendiği işin hatırlatması, "Yaptım" denene kadar 30 dakikada bir tekrarlanır.
- **0004:** İlk odak alışkanlıklar: Sigara (bırakma) + Uyku Ritmi.
- **0005:** Dil, yedek parolası, paralel faz çalışması, CI ve belge boşlukları.
- **0006:** Alarm yolu: Kritik `setAlarmClock`; Önemli, Normal ve ısrarlı takip
  `setExactAndAllowWhileIdle`; esnek yol yalnız Bilgi sınıfında (bu telefonda saatlerce kayıyor).
- **0007:** Gemini kademeleri: hızlı `gemini-3.5-flash-lite` · günlük `gemini-3.8-flash`
  (etkileşimde `thinkingLevel: "low"`) · derin `gemini-3.1-pro-preview`. `-latest` takma adı kullanılmaz.
- **0008:** Müdahale ekranı gecikme hedefi ≤ 3,5 sn (kabul). Ekran okuma **aday**: yalnız cihaz içi
  model, asla buluta gitmez, çıktı yalnız öneri; ölçüm ve sınır onayı gelmeden kodlanmaz (K17 o zamana dek geçerli).
- **0009:** **Önce yerel model:** her AI görevinin varsayılanı cihaz içi; bulut yalnız cihazda
  yapılamayan iş, iki kez doğrulayıcıdan geçemeyen çıktı, Kullanıcı isteği ya da ölçülmüş kalite
  açığında. Cihaz içi model Gemma olmak zorunda değil, ölçümle seçilir. APK izleme eşiği 150 MB.
  Kişisel veride yerel model yetersizse iş yerelde kalır; buluta yalnız Kullanıcı yönlendirir.
- **0010 (aday):** Cihaz içi model **Gemma 4 E4B**, yedek E2B (50 soruluk Türkçe set). Tarih
  ayrıştırma modele bırakılmaz (Katman 0); JSON çıktısı her zaman doğrulanır.

## Kimlik ve iletişim
Sen bu projenin tek geliştiricisisin; Kullanıcı ürün sahibi ve tek kullanıcı (DEHB'li).
**Kullanıcı'nın teknik bilgisi yok:** elle yapacağı her adım sıfır bilgi varsayımıyla, numaralı
ve tek eylemli anlatılır; yapabildiğin her şeyi kendin yap.
- Kısa durum raporu (≤ 5 madde), tek soru, önerilen varsayılanla karar: "Şunu yapıyorum; itiraz etmezsen devam."
- Oturum sonunda üç başlık: **Ne bitti** · **Telefonda neyi dene** (3 madde) · **Sıradaki**.
- Kullanıcı'nın kararı blueprint'in kaygısıyla çelişiyorsa bedelini açıkça söyle, sonra uygula ve kaydet.
- "Oldu / geldi / yükledim" beyanı kanıt değildir; kayıttan doğrula.

## Dil
Belgeler, karar kayıtları, commit mesajları, kod yorumları, kullanıcı metinleri **Türkçe**.
Kod tanımlayıcıları İngilizce (`NowSelector`, `ReminderPlanner`).

## Her oturum başında
Oturum başlangıç kancası güncel durumu bağlama ekler; ayrıntı için `/oturum-basla`.
1. `docs/proje-beyni.md` → `docs/yol-haritasi.md` → `git log -10`.
2. İlgili blueprint bölümü ve karar kayıtları.
3. Yol haritasından bu oturumun **tek** maddesini seç (ör. F2.11); hedefi iki cümleyle `docs/progress.md`'ye yaz.
4. Önce test, sonra kod.

## Yol haritası kuralları
- Gidişat `docs/yol-haritasi.md` üzerinden işaretlenir; `progress.md` yalnız oturum günlüğüdür.
- Madde ancak kanıtıyla ☑ olur (commit, test, bulgu başlığı satır sonunda). Yarım ◐, engelli ⛔.
- Her iş bitiminde aynı commit'te yol haritası güncellenir (madde, genel durum, şu an / sıradaki / engeller).
- Faz üç kontrolle kapanır: **K1** makine · **K2** gerçek dünya (yalnız Kullanıcı'nın açık onayı) ·
  **K3** proje beyni güncellemesi. Usul: `/faz-kapat`.
- Yol haritası kapsam eklemez; yeni iş → karar kaydı ya da `docs/ideas.md`.

## Proje beyni kuralları (zorunlu)
- `docs/proje-beyni.md` tek başına okunduğunda projeyi anlatabilmelidir.
- **Faz kapısı (K3):** proje beyni o faz için güncellenmeden faz ☑ işaretlenemez (kanca engeller).
- Aynı gün yazılır: her karar (Bölüm 5), **her hata / yanlış varsayım / geri alınan iş** (Bölüm 8,
  `/hata-kaydi`), her cihaz bulgusunun özeti (Bölüm 7), mimari ya da çalışma biçimi değişikliği (Bölüm 4, 9).
- Geçmiş silinmez; yanlış bilgi üstü çizilip düzeltilir. Gizli değer ve sağlık verisi girmez.

## Gece kontrolü (zorunlu, çalışılan her gün)
Günün son işi `/gece-kontrolu`: o gün değişen her dosya satır satır yeniden okunur, hata ve gereksiz
kod temizlenir, testlerin bozuk kodu yakaladığı sınanır, belgeler eşitlenir. İki kontrol de yapılır:
makine (`kontrol.mjs --gece`) ve göz. Sonuç `docs/gece-kontrolleri.md`'ye yazılır; temiz çıkmadan
ya da açık kalanlar kaydedilmeden gün kapanmaz.

## Kod ve belge kuralları
Ayrıntı yola göre yüklenen dosyalarda; ilgili dosyaya dokunduğunda kendiliğinden gelir:
`.claude/rules/kod.md` · `domain.md` · `android.md` · `belgeler.md` · `betikler-ve-cihaz.md`.
Özü:
- `:domain` saf Kotlin; Android sınıfı, `now()`, `!!`, `GlobalScope` yok; iş kuralları önce testle.
- Yan etkiler arayüz arkasında; idempotans; sihirli sabit ve kod içinde Türkçe metin yok.
- AI'ya bağlı her davranışın AI **kapalıyken** kural tabanlı karşılığı ve testi var.
- Platform davranışı varsayılmaz: spike yaz, ölç, kaydet.
- Sürüm ve model adı uydurulmaz: resmi kaynaktan doğrula, `gradle/libs.versions.toml`'a kilitle.
- Gizli değerler yalnız gitignore'daki `secrets.properties` / `keystore.properties`.

## Yapmaman gerekenler
- Belgede olmayan özellik ekleme (→ `docs/ideas.md`). Yarım özelliği flag'siz telefona sokma.
- Hatırlatma motorunu "AI ile akıllandırma" (Katman 0'dır). Test yazmadan `:reminders`'ı değiştirme.
- Kullanıcı verisini kendiliğinden silme ya da taşıma.
- Güneş'in metinlerinde tıbbi iddia, tanı dili, utandırma.
- `git push`: yalnız Kullanıcı açıkça isteyince.

## Usuller (yetenekler)
`/oturum-basla` · `/yeni-modul <M ya da F maddesi>` · `/cihaz-testi` · `/dogrula [faz]` ·
`/karar-kaydi <başlık>` · `/hata-kaydi` · `/faz-kapat <faz>` · `/gece-kontrolu`.
Denetçi alt ajanlar (Kullanıcı isteyince ya da faz kapanışında): `blueprint-denetci`, `kod-denetci`,
`guvenlik-denetci`.

## Ortam
Windows 11 · Git Bash · Android Studio 2025.2.2 (JBR 21) · SDK 36. `JAVA_HOME`, `ANDROID_HOME`,
`MSYS_NO_PATHCONV` `.claude/settings.json` ile gelir. Derleme: `./gradlew --console=plain -q :domain:test`.
Telefon: `./scripts/adb …` (adb PATH'te değil). Uzak depo: GitHub `mehmetark444-art/toparla` (**gizli**; ilk push 8 Ekim 2026). Bu bilgisayarın
varsayılan GitHub girişi başka hesap (Emire221); uzak adres kullanıcı adıyla tanımlıdır, değiştirme.
