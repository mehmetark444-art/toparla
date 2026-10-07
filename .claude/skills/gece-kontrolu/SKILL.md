---
name: gece-kontrolu
description: Günün son işi olan gece temizliği ve çift kontrol - o gün yazılan her şeyi yeniden okur, hata ve gereksiz kodu temizler, testlerin gerçekten yakaladığını sınar, belgeleri eşitler, sonucu günlüğe yazar. "Gece kontrolü, günü kapat, proje temizliği, son kontrol, her şeyi baştan incele" denince ve çalışılan her günün sonunda kullan.
allowed-tools: Bash(./gradlew *) Bash(node .claude/skills/dogrula/kontrol.mjs*) Bash(bash .claude/hooks/sinama.sh) Bash(git log*) Bash(git diff*) Bash(git status*) Bash(git show*)
---

# Gece kontrolü

Çalışılan her günün son işidir. İki ayrı kontrol vardır ve ikisi de yapılır:
**makine** (komutla) ve **göz** (o gün değişen her dosyanın satır satır okunması).
Biri diğerinin yerine geçmez.

## 1. Kapsam: bugün ne değişti?
```bash
git log --since="midnight" --stat --format="%h %s"
git status --short
```
Gün içinde commit yoksa son gece kontrolünden bugüne bak (`docs/gece-kontrolleri.md` son satırı).

## 2. Göz kontrolü (satır satır)
Bugün değişen **her** dosyayı baştan sona oku. Ararken:
- **Hata:** sınır durumları, yanlış koşul, ters mantık, yanlış birim, kopyala-yapıştır artığı.
- **Gereksiz kod:** kullanılmayan sabit, fonksiyon, içe aktarım, parametre; henüz ihtiyaç olmayan
  "ileride lazım olur" eklemeleri; ölü dal; tekrar eden blok.
- **Yarım iş:** yorumda kalmış niyet, flag'siz yarım özellik, geçici deneme dosyası.
- **Kural ihlali:** `CLAUDE.md` ve `.claude/rules/`; özellikle kancanın göremediği kurallar
  (sihirli sabit, kod içinde Türkçe metin, sessiz `catch`, AI kapalı karşılığı).
- **Bayat belge:** kodla ya da gerçek durumla çelişen cümle; "bekleniyor" diye kalmış bitmiş iş;
  ölçülmemiş şeyin ölçülmüş gibi yazılması.
Bulduğunu düzelt; düzeltemiyorsan yol haritasına madde ya da proje beynine açık soru olarak yaz.

## 3. Makine kontrolü
```bash
./gradlew --console=plain --warning-mode all :domain:test :app:assembleDebug
node .claude/skills/dogrula/kontrol.mjs --gece
bash .claude/hooks/sinama.sh
```
Derleme uyarısı da bulgudur. Test sayısını sonuç dosyalarından oku; eski sonuca güvenme.

## 4. Testler gerçekten yakalıyor mu? (bugün iş kuralı değiştiyse)
Bugün değişen her kural için kodu bilerek boz (koşulu ters çevir, sabiti değiştir, dalı sil),
testi koş, **kaldığını gör**, `git checkout` ile geri al. Yakalanmayan bozma = eksik test ya da
gereksiz kod: testini yaz ya da kodu sil. Sonda `git status` temiz ve testler yeşil olmalı.

## 5. Belgeleri eşitle
- `docs/yol-haritasi.md`: bugünün maddeleri kanıtıyla işaretli; "Şu an / Sıradaki tek adım /
  Açık engeller" ve "Son güncelleme" güncel.
- `docs/proje-beyni.md`: bugünün kararları (Bölüm 5), **hataları** (Bölüm 8), bulguları (Bölüm 7),
  zaman çizelgesi (Bölüm 6), "Son güncelleme" ve "Kapsadığı son commit".
- `docs/platform-bulgulari.md` ve `docs/progress.md`.
Yol haritası ve proje beyni düzenleme aracıyla değiştirilir (kabuk komutuyla değil).

## 6. Kaydet
1. `docs/gece-kontrolleri.md` tablosuna bir satır: tarih · incelenen commit aralığı · bulunan ve
   düzeltilen · açık kalan · test sayısı · sonuç.
2. Tek commit: `Gece kontrolu <tarih>: <özet>`.
3. `node .claude/skills/dogrula/kontrol.mjs --gece` → "tutarlı" ve çalışma ağacı temiz.

## 7. Kullanıcı'ya rapor (kısa)
Ne bulundu ve düzeltildi · ne açık kaldı · yarın ilk iş. Temiz çıktıysa öyle söyle; bulgu uydurma.
Sorun bulunduysa küçültme.
