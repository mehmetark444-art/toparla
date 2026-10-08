#!/usr/bin/env bash
# Spike 10: bir cihaz içi modeli telefona atar (yoksa), Türkçe seti koşar, ölçümü ve çıktıyı yazdırır.
# Kullanım: bash scripts/spike-llm-olc.sh <model-adı> [gpu|cpu]   (modeller/<model-adı>.litertlm)
cd "$(dirname "$0")/.." || exit 1
M="$1"; B="${2:-gpu}"
P=com.toparla.spike
D=/sdcard/Android/data/$P/files
L=/data/user_de/0/$P/files/log.csv
[ -f "modeller/$M.litertlm" ] || { echo "modeller/$M.litertlm yok"; exit 1; }
./scripts/adb wait-for-device
LOCAL=$(ls -la "modeller/$M.litertlm" | awk '{print $5}')
REMOTE=$(./scripts/adb shell "ls -la $D/models/$M.litertlm 2>/dev/null" | tr -d '\r' | awk '{print $5}')
if [ "$LOCAL" != "$REMOTE" ]; then
  ./scripts/adb shell mkdir -p $D/models
  ./scripts/adb push "modeller/$M.litertlm" "$D/models/$M.litertlm" 2>&1 | tail -1
fi
./scripts/adb shell input keyevent KEYCODE_WAKEUP
T0=$(./scripts/adb shell date +%s%3N | tr -d '\r')
[ -n "$T0" ] || { echo "Başlangıç zamanı okunamadı."; exit 1; }
./scripts/adb shell rm -f "$D/llm-$M.txt"
./scripts/adb shell am start -n $P/.SpikeActivity --es llm "$B" --es model "$M" >/dev/null 2>&1
n=0
until ./scripts/adb shell run-as $P cat $L | tr -d '\r' | awk -F, -v t="$T0" '$1>t' | grep -qE "LLM_(DONE|ERROR|NO_MODEL),$M/"; do
  [ $n -ge 1500 ] && { echo "ZAMAN AŞIMI"; break; }
  ./scripts/adb shell "sleep 10"; n=$((n + 10))
  # Süreç öldüyse (ör. bellek yetersizliği) boşuna bekleme (proje-beyni H22).
  if [ $n -ge 30 ] && [ -z "$(./scripts/adb shell pidof $P | tr -d '\r')" ]; then
    echo "SÜREÇ ÖLDÜ: $(./scripts/adb logcat -d | tr -d '\r' | grep -E "lowmemorykiller.*$P|Fatal signal" | tail -1 | cut -c1-200)"
    break
  fi
done
echo "##### $M ($B) — ${n}s"
./scripts/adb shell run-as $P cat $L | tr -d '\r' | awk -F, -v t="$T0" '$1>t' | grep LLM | cut -d, -f2,3,11- | sed "s#$M/$B/##"
echo "----- çıktı"
./scripts/adb shell cat "$D/llm-$M.txt" 2>/dev/null | tr -d '\r'
