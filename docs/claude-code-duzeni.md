# Claude Code düzeni

Projenin Claude Code ile çalışma altyapısı: ne kuruldu, ne işe yarıyor, neden.
Biçimler 8 Ekim 2026'da resmi belgelerden (code.claude.com/docs) doğrulanarak yazıldı.

## Özet

| Parça | Yer | Ne yapar |
|---|---|---|
| Proje talimatı | `CLAUDE.md` | Her oturumda yüklenen çekirdek kurallar (kısa tutulur: < 200 satır) |
| Başka araçlar için giriş | `AGENTS.md` | Claude Code dışındaki AI araçlarına ve yeni geliştiriciye yol gösterir |
| Ayarlar | `.claude/settings.json` | Ortam değişkenleri, izinler, kancalar |
| Kancalar | `.claude/hooks/*.mjs` | Kuralları **zorunlu** kılar (talimat unutulabilir, kanca unutmaz) |
| Yola göre kurallar | `.claude/rules/*.md` | İlgili dosyaya dokunulunca yüklenen ayrıntılı kurallar |
| Yetenekler | `.claude/skills/*/SKILL.md` | Tekrarlanan iş usulleri (`/ad` ile ya da kendiliğinden) |
| Alt ajanlar | `.claude/agents/*.md` | Ayrı bağlamda çalışan salt okunur denetçiler |
| Yardımcı betik | `scripts/adb` | adb sarmalayıcısı |

## Kancalar

Node ile yazıldı (bağımlılıksız). Sınama: `bash .claude/hooks/sinama.sh` (21 senaryo).

| Kanca | Olay | Davranış |
|---|---|---|
| `oturum-baslangici.mjs` | SessionStart (başlangıç, devam, temizleme, **sıkıştırma**) | Yol haritasının genel durumunu, proje beyninin son güncellemesini ve son commit'leri bağlama ekler. Bağlam sıkışınca proje kaybolmaz. |
| `dosya-koruma.mjs` | PreToolUse: düzenleme araçları | (1) `docs/BLUEPRINT.md` ve `docs/arsiv/` değiştirilemez. (2) Gizli değer kalıbı içeren içerik yazılamaz. (3) **Faz kapısı:** proje beyninde kapanış kaydı olmayan faz yol haritasında ☑ yapılamaz. |
| `komut-koruma.mjs` | PreToolUse: Bash / PowerShell | (1) `git push` her seferinde Kullanıcı'ya sorulur. (2) Commit'e girecek değişikliklerde gizli değer ya da gizli dosya varsa commit engellenir. (3) Gizli dosyanın içeriği ekrana basılamaz. |
| `kod-kurallari.mjs` | PostToolUse: düzenleme araçları | Kotlin dosyasında `!!`, `GlobalScope`, test dışı `runBlocking`, `:domain`'de Android içe aktarımı, `now()` / `currentTimeMillis` / tohumsuz `Random` bulursa Claude'a bildirir. `:spike` muaf. |

Kancaların göremedikleri: `sed` gibi kabuk komutlarıyla yapılan dosya değişiklikleri düzenleme
kancalarından geçmez. Bu yüzden yol haritası ve proje beyni düzenleme aracıyla değiştirilir.

## İzinler (`.claude/settings.json`)

- **Sorulmadan:** `./gradlew`, salt okunur git, `git add` / `git commit`, salt okunur adb sorguları, resmi belge siteleri.
- **Her seferinde sorulur:** `git push`, telefonu yeniden başlatma, uygulama kaldırma, veri silme.
- **Yasak:** `secrets.properties` / keystore okuma, blueprint ve arşivi düzenleme, zorla push, `git reset --hard`, `git clean`.
- **Ortam:** `JAVA_HOME`, `ANDROID_HOME`, `MSYS_NO_PATHCONV=1`.

Kişisel tercihler için `.claude/settings.local.json` ve `CLAUDE.local.md` gitignore'dadır.

## Yola göre kurallar

| Dosya | Yüklendiği yer |
|---|---|
| `kod.md` | `*.kt`, `*.kts`, kaynak XML'leri |
| `domain.md` | `domain/**` |
| `android.md` | `app`, `reminders`, `sensors`, `ui`, `data`, `ai` |
| `belgeler.md` | `docs/**`, `CLAUDE.md`, `AGENTS.md` |
| `betikler-ve-cihaz.md` | `scripts/**`, `spike/**`, `.claude/hooks/**` |

## Yetenekler

| Yetenek | Ne zaman |
|---|---|
| `/oturum-basla` | Oturum başı, bağlam kaybı, "neredeydik" |
| `/yeni-modul <M ya da F>` | Yeni özellik ya da modüle başlarken |
| `/cihaz-testi` | Telefon, adb, spike, ölçüm içeren her iş |
| `/dogrula [faz]` | Commit ve faz kapanışı öncesi makine kontrolü |
| `/karar-kaydi <başlık>` | Blueprint'ten sapma, yeni izin, teknoloji seçimi |
| `/hata-kaydi` | Hata, yanlış varsayım, geri alınan iş (aynı gün) |
| `/faz-kapat <faz>` | Faz kapanış töreni: K1 + K2 + K3 |
| `/gece-kontrolu` | Çalışılan her günün son işi: satır satır yeniden okuma, temizlik, kasıtlı bozmayla test sınaması, belge eşitleme; sonuç `docs/gece-kontrolleri.md` |

`dogrula/kontrol.mjs` depo tutarlılığını denetler: gizli değer yok · belgelerdeki commit'ler gerçek ·
M1–M30 kapsamı · her fazda K1/K2/K3 · kapanan fazın kapanış kaydı · karar kayıtları `CLAUDE.md`'de.
`--gece` ile ek olarak: çalışma ağacı temiz · artık dosya yok · sahipsiz TODO yok · `CLAUDE.md` kısa ·
ön maddeler yerinde · yol haritası ve proje beyni tarihleri güncel · açık engel ve teyit bekleyen karar hatırlatması.

## Alt ajanlar

Hepsi salt okunur; ana oturumun bağlamını doldurmadan ayrı bağlamda çalışır ve rapor döner.

| Ajan | İş |
|---|---|
| `blueprint-denetci` | Kodu blueprint kabul kriterleriyle satır satır eşler; eksik ve testsiz olanı bulur |
| `kod-denetci` | Değişen kodu proje kurallarına ve hatırlatma invaryantlarına karşı inceler |
| `guvenlik-denetci` | Gizli değer, izin, veri akışı, enjeksiyon savunması, kriz ve tıbbi sınır denetimi |

## Değerlendirilip kurulmayanlar

| Özellik | Neden kurulmadı |
|---|---|
| MCP sunucuları | Sunucusuz, tek cihazlı proje; bağlanacak dış sistem yok. GitHub'a geçince yeniden bakılır. |
| Eklenti (plugin) paketi | Tek depo, tek kullanıcı; `.claude/` yeterli. |
| Özel çıktı stili / durum satırı | İletişim kuralları `CLAUDE.md`'de; ek katman gereksiz. |
| Durdurma (Stop) kancası | Yanlış tetiklenirse oturumu döngüye sokar; faz kapısı düzenleme kancasıyla sağlandı. |
| Zamanlanmış görevler | Henüz tekrarlayan iş yok. 7/14/30 günlük K2 beklemelerinde yeniden değerlendirilir. |
| Kullanıcı düzeyi `~/.claude` ayarları | Kişisel alan; proje dokunmaz. |

## Bakım

- Kanca değişirse: `bash .claude/hooks/sinama.sh` geçmeden commit yok.
- Yeni tekrarlanan usul → yetenek. Yeni "her zaman" kuralı → önce kural dosyası; denetlenebiliyorsa kanca.
- `CLAUDE.md` 200 satırın altında kalır; ayrıntı kural dosyalarına taşınır.
- Ayar ve kanca değişikliği yeni oturumda kesin geçerlidir; yeni eklenen yetenek ve alt ajan dizinleri için oturumu yeniden başlat.
