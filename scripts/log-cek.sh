#!/usr/bin/env bash
# Telefondan tanılama kayıtlarını çeker: uygulamanın sistem günlüğü (logcat) ve, geliştirme sürümünde,
# uygulamanın kendi dönen günlük dosyaları. Çıktı: build/logs/<tarih-saat>/
# Kullanım (proje kökünden):  bash scripts/log-cek.sh [paket]
#   bash scripts/log-cek.sh                    geliştirme sürümü (com.toparla.app.dev)
#   bash scripts/log-cek.sh com.toparla.app    gerçek sürüm (yalnız sistem günlüğü; dosyalara erişilemez)
# Not: günlüklerde gizli değer, bildirim/ekran içeriği ve sağlık verisi bulunmaz (uygulama yazmaz).
set -u
cd "$(dirname "$0")/.." || exit 1
export MSYS_NO_PATHCONV=1
PKG="${1:-com.toparla.app.dev}"
OUT="build/logs/$(date '+%Y%m%d-%H%M%S')"
adb() { ./scripts/adb "$@" 2>&1 | tr -d '\r'; }

./scripts/adb wait-for-device
[ "$(adb get-state)" = "device" ] || { echo "Telefon bağlı değil."; exit 1; }
adb shell pm path "$PKG" | grep -q '^package:' || { echo "$PKG telefonda kurulu değil."; exit 1; }
mkdir -p "$OUT"

PID=$(adb shell pidof "$PKG")
if [ -n "$PID" ]; then
  adb logcat -d --pid="$PID" >"$OUT/logcat.txt"
else
  echo "Uygulama şu an çalışmıyor; sistem günlüğünün son satırları paket adıyla süzüldü." >"$OUT/NOT.txt"
  adb logcat -d -t 5000 | grep -F "$PKG" >"$OUT/logcat.txt"
fi
adb logcat -d -b crash | grep -F -A 30 "$PKG" >"$OUT/cokme.txt"

# Uygulamanın kendi günlük dosyaları yalnız hata ayıklanabilir (debug) sürümde okunabilir.
FILES=$(adb shell run-as "$PKG" ls files/logs 2>/dev/null | grep '\.log$')
if [ -n "$FILES" ]; then
  for f in $FILES; do adb shell run-as "$PKG" cat "files/logs/$f" >"$OUT/$f"; done
else
  echo "Uygulama günlük dosyaları okunamadı (gerçek sürüm ya da henüz günlük yok)." >>"$OUT/NOT.txt"
fi

echo "Kayıtlar: $OUT"
ls -la "$OUT" | awk 'NR>1 {print $5, $9}'
