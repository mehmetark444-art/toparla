#!/usr/bin/env bash
# Spike 5 ölçümü: izlenen iki uygulamayı sırayla açtırır, olay → kart gecikmesini kayıttan okur.
# Kullanım: bash scripts/spike-a11y-olc.sh [tur sayısı]   (proje kökünden; servis telefonda açık olmalı)
cd "$(dirname "$0")/.." || exit 1
P=com.toparla.spike
L=/data/user_de/0/$P/files/log.csv
N="${1:-4}"
./scripts/adb wait-for-device
BOUND=$(./scripts/adb shell dumpsys accessibility | tr -d '\r' | grep -c 'Bound services:{Service')
[ "$BOUND" = 1 ] || { echo "Servis bağlı değil; ölçüm yapılmadı."; exit 1; }
./scripts/adb shell input keyevent KEYCODE_WAKEUP
./scripts/adb shell input keyevent KEYCODE_HOME
T0=$(./scripts/adb shell date +%s%3N | tr -d '\r')
[ -n "$T0" ] || { echo "Başlangıç zamanı okunamadı."; exit 1; }
for i in $(seq 1 "$N"); do
  if [ $((i % 2)) = 1 ]; then app=com.miui.calculator; else app=com.google.android.youtube; fi
  ./scripts/adb shell "sleep 17" # servisteki 15 sn'lik bekleme süresini aş
  ./scripts/adb shell monkey -p "$app" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  ./scripts/adb shell "sleep 4"
  ./scripts/adb shell input keyevent KEYCODE_BACK
  ./scripts/adb shell "sleep 1"
  ./scripts/adb shell input keyevent KEYCODE_HOME
done
./scripts/adb shell run-as $P cat $L | tr -d '\r' | awk -F, -v t="$T0" '$1>t' | grep -E 'APP_OPEN|INTERCEPT_DRAWN|INTERCEPT_START|A11Y' | cut -d, -f2,3,11-
