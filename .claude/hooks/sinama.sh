#!/usr/bin/env bash
# Kancaların sınaması: her kanca hem engellemeli hem zararsız işe izin vermeli.
# Çalıştırma: bash .claude/hooks/sinama.sh   (proje kökünden)
cd "$(dirname "$0")/../.." || exit 1
R="$(pwd -W 2>/dev/null || pwd)"
export CLAUDE_PROJECT_DIR="$R"
H=.claude/hooks
FAIL=0
FAKE="AQ.$(printf 'x%.0s' $(seq 1 40))"

# t <ad> <kanca> <beklenen: deny|ask|izin|cikis2> <json>
t() {
  local out code karar sonuc
  out=$(printf '%s' "$4" | node "$H/$2" 2>&1); code=$?
  karar=$(printf '%s' "$out" | grep -oE '"permissionDecision":"[a-z]+"' | cut -d'"' -f4)
  if [ "$code" = 2 ]; then sonuc=cikis2; elif [ -n "$karar" ]; then sonuc=$karar; else sonuc=izin; fi
  if [ "$sonuc" = "$3" ]; then printf 'GECTI  %s\n' "$1"; else printf 'KALDI  %s (beklenen %s, gelen %s)\n' "$1" "$3" "$sonuc"; FAIL=1; fi
}

t "blueprint duzenlenemez" dosya-koruma.mjs deny "{\"tool_input\":{\"file_path\":\"$R/docs/BLUEPRINT.md\",\"old_string\":\"a\",\"new_string\":\"b\"}}"
t "git bash yoluyla da duzenlenemez" dosya-koruma.mjs deny "{\"tool_input\":{\"file_path\":\"$(pwd)/docs/BLUEPRINT.md\",\"content\":\"x\"}}"
t "arsiv duzenlenemez" dosya-koruma.mjs deny "{\"tool_input\":{\"file_path\":\"$R/docs/arsiv/TOPARLA_v3.md\",\"content\":\"x\"}}"
t "siradan dosya yazilir" dosya-koruma.mjs izin "{\"tool_input\":{\"file_path\":\"$R/docs/ideas.md\",\"content\":\"merhaba\"}}"
t "belgeye anahtar yazilamaz" dosya-koruma.mjs deny "{\"tool_input\":{\"file_path\":\"$R/docs/ideas.md\",\"content\":\"k=$FAKE\"}}"
t "kapanis kaydi olmayan faz isaretlenemez" dosya-koruma.mjs deny "{\"tool_input\":{\"file_path\":\"$R/docs/yol-haritasi.md\",\"old_string\":\"| F1 | S0 | Cihaz denemeleri (spike) | ◐ |\",\"new_string\":\"| F1 | S0 | Cihaz denemeleri (spike) | ☑ |\"}}"
t "yol haritasi siradan duzenleme" dosya-koruma.mjs izin "{\"tool_input\":{\"file_path\":\"$R/docs/yol-haritasi.md\",\"old_string\":\"olmayan-metin\",\"new_string\":\"y\"}}"

t "git push sorulur" komut-koruma.mjs ask '{"tool_input":{"command":"git push origin main"}}'
t "gizli dosya ekrana basilamaz" komut-koruma.mjs deny '{"tool_input":{"command":"cat secrets.properties"}}'
t "gizli deger degiskene alinabilir" komut-koruma.mjs izin '{"tool_input":{"command":"K=$(grep ^GEMINI secrets.properties | cut -d= -f2-)"}}'
t "grep ile ekrana basma da engellenir" komut-koruma.mjs deny '{"tool_input":{"command":"grep GEMINI secrets.properties"}}'
t "gizli dosyanin varligina bakilabilir" komut-koruma.mjs izin '{"tool_input":{"command":"git check-ignore -q secrets.properties && ls -la secrets.properties"}}'
t "git stash push sorulmaz" komut-koruma.mjs izin '{"tool_input":{"command":"git stash push -m gecici"}}'
t "git -C ile push sorulur" komut-koruma.mjs ask '{"tool_input":{"command":"git -C . push"}}'
t "siradan komut" komut-koruma.mjs izin '{"tool_input":{"command":"ls"}}'
t "temiz agacta commit" komut-koruma.mjs izin '{"tool_input":{"command":"git add -A && git commit -m x"}}'
printf 'k=%s\n' "$FAKE" > sizinti-deneme.txt
t "anahtar iceren dosyayla commit engellenir" komut-koruma.mjs deny '{"tool_input":{"command":"git add -A && git commit -m x"}}'
rm -f sizinti-deneme.txt

G=domain/src/main/kotlin/com/toparla/domain/GeciciSinama.kt
printf 'package com.toparla.domain\nimport java.time.Instant\nfun kotu(x: String?) = x!!.length + Instant.now().nano\n' > "$G"
t "domain kural ihlali bildirilir" kod-kurallari.mjs cikis2 "{\"tool_input\":{\"file_path\":\"$R/$G\"}}"
rm -f "$G"
t "spike muaf" kod-kurallari.mjs izin "{\"tool_input\":{\"file_path\":\"$R/spike/src/main/kotlin/com/toparla/spike/AlarmSpike.kt\"}}"
for f in $(git ls-files '*.kt' | grep -v '^spike/'); do
  printf '%s' "{\"tool_input\":{\"file_path\":\"$R/$f\"}}" | node "$H/kod-kurallari.mjs" >/dev/null 2>&1 || { echo "KALDI  mevcut kodda ihlal: $f"; FAIL=1; }
done
echo "GECTI  mevcut Kotlin dosyalari kurallara uygun (ihlal satiri yoksa)"

OUT=$(printf '{"source":"compact"}' | node "$H/oturum-baslangici.mjs")
printf '%s' "$OUT" | grep -q 'Sıradaki tek adım' && echo "GECTI  oturum baslangici yol haritasi durumunu veriyor" || { echo "KALDI  oturum baslangici"; FAIL=1; }

[ "$FAIL" = 0 ] && echo "SONUC: hepsi gecti" || echo "SONUC: KALAN VAR"
exit $FAIL
