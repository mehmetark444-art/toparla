---
name: dogrula
description: Projenin makine doğrulamasını (K1) koşar - birim testleri, derleme, depo ve belge tutarlılığı, gizli değer taraması, kanca sınaması. Commit öncesi, faz kapanışı öncesi ya da "her şey yerinde mi, kontrol et, doğrula" denince kullan.
argument-hint: "[faz, ör. F2]"
allowed-tools: Bash(./gradlew *) Bash(node .claude/skills/dogrula/kontrol.mjs*) Bash(bash .claude/hooks/sinama.sh)
---

# Doğrulama (K1)

Aşağıdakileri sırayla koş. Birinin kalması diğerlerini koşmana engel değil; hepsini koş, sonra raporla.

1. **Testler ve derleme**
   ```bash
   ./gradlew --console=plain -q :domain:test :reminders:testDebugUnitTest :app:assembleDebug
   ```
   Test sayısını `domain/build/test-results/test/*.xml` ve `reminders/build/test-results/testDebugUnitTest/*.xml`
   dosyalarından oku (`tests=`, `failures=`, `errors=`).
   Android modüllerinde test varsa `./gradlew check` de koş.

2. **Depo ve belge tutarlılığı**
   ```bash
   node .claude/skills/dogrula/kontrol.mjs
   ```
   Faz verildiyse (`$ARGUMENTS`): `node .claude/skills/dogrula/kontrol.mjs --faz $ARGUMENTS`

3. **Kancalar** (yalnız `.claude/hooks/` değiştiyse)
   ```bash
   bash .claude/hooks/sinama.sh
   ```

## Raporlama
- Geçeni "geçti", kalanı "kaldı" diye çıktısıyla yaz; kalanı yumuşatma.
- Koşmadığın adımı "koşulmadı" diye belirt.
- K1 yalnız makine kontrolüdür. K2 (gerçek dünya) için Kullanıcı'nın açık onayı gerekir; K1 geçti diye fazı kapatma.
