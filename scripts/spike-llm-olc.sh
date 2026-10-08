#!/usr/bin/env bash
# Spike 10: bir cihaz içi modeli telefona atar (yoksa), 50 soruluk Türkçe seti koşar, yanıtları
# build/llm/ altına çeker. Puanlama: node scripts/llm-puanla.mjs
# Kullanım: bash scripts/spike-llm-olc.sh <model-adı> [gpu|cpu|gpu-raw|cpu-raw] [koşu no] [set adı]
# Set adı verilirse build/llm-setler/<ad>.json telefona atılır ve o koşulur; çıktı build/llm/<model>-<ad>-<koşu>.jsonl
cd "$(dirname "$0")/.." || exit 1
M="$1"; B="${2:-gpu}"; R="${3:-1}"; S="${4:-}"
P=com.toparla.spike
D=/sdcard/Android/data/$P/files
L=/data/user_de/0/$P/files/log.csv
[ -f "modeller/$M.litertlm" ] || { echo "modeller/$M.litertlm yok"; exit 1; }
mkdir -p build/llm
./scripts/adb wait-for-device
LOCAL=$(ls -la "modeller/$M.litertlm" | awk '{print $5}')
REMOTE=$(./scripts/adb shell "ls -la $D/models/$M.litertlm 2>/dev/null" | tr -d '\r' | awk '{print $5}')
if [ "$LOCAL" != "$REMOTE" ]; then
  ./scripts/adb shell mkdir -p $D/models
  ./scripts/adb push "modeller/$M.litertlm" "$D/models/$M.litertlm" 2>&1 | tail -1
fi
SETARG=""; INNAME="llm-set-$M-$R"; OUTNAME="$M-$R"
if [ -n "$S" ]; then
  [ -f "build/llm-setler/$S.json" ] || { echo "build/llm-setler/$S.json yok"; exit 1; }
  ./scripts/adb shell mkdir -p $D/sets
  ./scripts/adb push "build/llm-setler/$S.json" "$D/sets/$S.json" >/dev/null 2>&1
  SETARG="--es set $S"; INNAME="llm-$S-$M-$R"; OUTNAME="$M-$S-$R"
fi
./scripts/adb shell input keyevent KEYCODE_WAKEUP
T0=$(./scripts/adb shell date +%s%3N | tr -d '\r')
[ -n "$T0" ] || { echo "Başlangıç zamanı okunamadı."; exit 1; }
# --activity-clear-top: üstte başka sayfa kalmışsa intent teslim edilmiyor (proje-beyni H24).
./scripts/adb shell am start --activity-clear-top -n $P/.SpikeActivity --es llm "$B" --es model "$M" --ei run "$R" $SETARG >/dev/null 2>&1
n=0
until ./scripts/adb shell run-as $P cat $L | tr -d '\r' | awk -F, -v t="$T0" '$1>t' | grep -qE "LLM_(DONE|ERROR|NO_MODEL),$M/"; do
  [ $n -ge 2400 ] && { echo "ZAMAN AŞIMI"; break; }
  ./scripts/adb shell "sleep 10"; n=$((n + 10))
  # Süreç öldüyse (ör. bellek yetersizliği) boşuna bekleme (proje-beyni H22).
  if [ $n -ge 30 ] && [ -z "$(./scripts/adb shell pidof $P | tr -d '\r')" ]; then
    echo "SÜREÇ ÖLDÜ: $(./scripts/adb logcat -d | tr -d '\r' | grep -E "lowmemorykiller.*$P|Fatal signal" | tail -1 | cut -c1-200)"
    break
  fi
done
echo "##### $M ($B) koşu $R — ${n}s"
./scripts/adb shell run-as $P cat $L | tr -d '\r' | awk -F, -v t="$T0" '$1>t' | grep LLM | cut -d, -f2,3,11-
./scripts/adb shell cat "$D/$INNAME.jsonl" 2>/dev/null | tr -d '\r' > "build/llm/$OUTNAME.jsonl"
echo "yanıt satırı: $(grep -c . "build/llm/$OUTNAME.jsonl")"
./scripts/adb shell run-as $P cat $L | tr -d '\r' | awk -F, -v t="$T0" '$1>t' | grep -E 'LLM_(INIT|DONE)' | cut -d, -f2,3,11- > "build/llm/$OUTNAME.meta"
