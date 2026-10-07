# AGENTS.md — Toparla · Güneş

Bu depoda çalışan her AI kodlama aracı ve yeni geliştirici için giriş noktası.
(Claude Code bu dosyayı değil `CLAUDE.md`'yi yükler; ikisi aynı kurallara işaret eder.)

## Önce bunları oku (bu sırayla)
1. `docs/proje-beyni.md` — projenin hafızası: ne, neden, nasıl geldik, yapılan hatalar, tuzaklar.
2. `docs/yol-haritasi.md` — tek durum kaynağı: neredeyiz, sırada ne var, faz kapıları.
3. `CLAUDE.md` — çalışma protokolü ve blueprint'i ezen kararların özeti.
4. `docs/decisions/` — karar kayıtları (blueprint'i ezer).
5. `docs/BLUEPRINT.md` — ürün tanımı; **değiştirilmez**, yalnız ilgili bölümü oku (2000+ satır).

## Proje bir cümlede
DEHB'li tek bir kullanıcı için, tek Android telefonda (Xiaomi 17T Pro, Android 16, HyperOS 3)
çalışan, sunucusuz kişisel yaşam asistanı: AI'sız güvenilir çekirdek + "Güneş" adlı AI ajanı.

## Değişmez kurallar
- Her şey Türkçe (belge, commit, yorum, kullanıcı metni); kod tanımlayıcıları İngilizce.
- Kullanıcı teknik bilgi sahibi değil: elle adımlar numaralı ve tek eylemli; yapabildiğini kendin yap.
- `:domain` saf Kotlin; Android sınıfı, `now()`, `!!`, `GlobalScope` yok. Önce test.
- Platform davranışı varsayılmaz: ölç, `docs/platform-bulgulari.md`'ye yaz.
- Gizli değerler yalnız gitignore'daki `secrets.properties` / `keystore.properties`; depoya ve belgelere girmez.
- `git push` yalnız Kullanıcı açıkça isteyince.
- Kapsam `BLUEPRINT.md` + karar kayıtlarıdır; yeni fikir `docs/ideas.md`'ye.
- Bir iş ancak kanıtıyla "bitti" sayılır; faz K1 (makine) + K2 (Kullanıcı onayı) + K3 (proje beyni) ile kapanır.
- Karar, hata ve cihaz bulgusu aynı gün `docs/proje-beyni.md`'ye yazılır.

## Komutlar (Git Bash, proje kökü)
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew --console=plain -q :domain:test :app:assembleDebug   # test + derleme
node .claude/skills/dogrula/kontrol.mjs                         # depo ve belge tutarlılığı
./scripts/adb devices                                           # telefon (adb PATH'te değil)
```

## Claude Code'a özgü düzen
`.claude/` altında: `settings.json` (izinler, ortam, kancalar), `hooks/` (koruma ve denetim),
`rules/` (yola göre kurallar), `skills/` (iş usulleri), `agents/` (denetçi alt ajanlar).
Ayrıntı: `docs/claude-code-duzeni.md`. Başka bir araç kullanıyorsan aynı kuralları elle uygula;
özellikle `.claude/rules/*.md` dosyalarını oku.
