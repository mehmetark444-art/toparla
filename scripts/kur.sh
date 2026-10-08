#!/usr/bin/env bash
# Kurulum betiği (blueprint Ek B): APK'yı kurar, izinleri ve özel erişimleri adb ile verir, her adımın
# gerçekten tuttuğunu geri okuyarak doğrular. Sonuç tablosu ekrana ve build/kur-sonuc.txt'ye yazılır.
# Kullanım (proje kökünden):  bash scripts/kur.sh [paket] [apk yolu | -]
#   bash scripts/kur.sh                       asıl uygulama (com.toparla.app), release APK
#   bash scripts/kur.sh com.toparla.spike -   deneme uygulaması, kurulum atlanır (yalnız izinler)
set -u
cd "$(dirname "$0")/.." || exit 1
export MSYS_NO_PATHCONV=1 # Git Bash /data/... yollarını çevirmesin (proje-beyni H2)

PKG="${1:-com.toparla.app}"
APK="${2:-app/build/outputs/apk/release/app-release.apk}"
if [ "$PKG" = "com.toparla.spike" ]; then
  NLS="$PKG/$PKG.SpikeNotificationListener"
  A11Y="$PKG/$PKG.AppOpenAccessibilityService"
else
  NLS="$PKG/com.toparla.sensors.NotificationSensorService"
  A11Y="$PKG/com.toparla.sensors.AppOpenAccessibilityService"
fi

OUT=build/kur-sonuc.txt
mkdir -p build
: >"$OUT"
FAIL=0
adb() { ./scripts/adb "$@" 2>&1 | tr -d '\r'; }
row() { printf '%-4s %-26s %s\n' "$1" "$2" "$3" | tee -a "$OUT"; }
ok() { row "OK" "$1" "${2:-}"; }
skip() { row "-" "$1" "$2"; }
no() {
  row "HATA" "$1" "$2"
  FAIL=$((FAIL + 1))
}

./scripts/adb wait-for-device # adb yeni başladıysa ilk komutlar cihazı görmez (H21)
[ "$(adb get-state)" = "device" ] || { echo "Telefon bağlı değil."; exit 1; }
echo "Paket: $PKG · $(adb shell getprop ro.mi.os.version.incremental) · $(date '+%d.%m.%Y %H:%M')" | tee -a "$OUT"

if [ "$APK" != "-" ]; then
  [ -f "$APK" ] || { echo "APK bulunamadı: $APK"; exit 1; }
  echo "Telefonda 'USB ile yüklensin mi?' penceresi çıkacak; 10 saniye içinde 'Yükle'ye bas."
  R=$(adb install -r "$APK" | tail -1)
  case "$R" in
    Success*) ok "APK kurulumu" ;;
    *USER_RESTRICTED*) no "APK kurulumu" "onaylanmadı ya da 'USB ile yükle' kapalı (H9)"; exit 1 ;;
    *) no "APK kurulumu" "$R"; exit 1 ;;
  esac
fi
adb shell pm path "$PKG" | grep -q '^package:' || { echo "$PKG telefonda kurulu değil."; exit 1; }

REQUESTED=$(adb shell dumpsys package "$PKG")

# Çalışma zamanı izni: uygulama manifestte istemiyorsa atlanır, hata sayılmaz.
grant() {
  local label="$1" perm="android.permission.$2"
  if ! printf '%s' "$REQUESTED" | grep -q "$perm"; then
    skip "$label" "uygulama istemiyor"
    return
  fi
  local r
  r=$(adb shell pm grant "$PKG" "$perm")
  if adb shell dumpsys package "$PKG" | grep -q "$perm: granted=true"; then ok "$label"; else no "$label" "${r:-verilmedi}"; fi
}

# Özel erişim (appops): komuttan sonra değer geri okunur.
appop() {
  local label="$1" op="$2" r
  r=$(adb shell cmd appops set "$PKG" "$op" allow)
  if adb shell cmd appops get "$PKG" "$op" | grep -q "allow"; then ok "$label"; else no "$label" "${r:-allow görünmüyor}"; fi
}

# Komut + geri okuma: üçüncü argüman doğrulama komutu, dördüncü beklenen metin.
check() {
  local label="$1" cmd="$2" verify="$3" expect="$4" r
  r=$(adb shell "$cmd")
  if adb shell "$verify" | grep -q -- "$expect"; then ok "$label"; else no "$label" "${r:-doğrulanamadı}"; fi
}

grant "Bildirim izni" POST_NOTIFICATIONS
grant "Mikrofon" RECORD_AUDIO
grant "Kamera" CAMERA
grant "Takvim oku" READ_CALENDAR
grant "Takvim yaz" WRITE_CALENDAR
grant "Konum (ince)" ACCESS_FINE_LOCATION
grant "Konum (arka plan)" ACCESS_BACKGROUND_LOCATION
grant "Görseller" READ_MEDIA_IMAGES
grant "Bluetooth" BLUETOOTH_CONNECT
grant "Aktivite" ACTIVITY_RECOGNITION
appop "Kullanım istatistikleri" GET_USAGE_STATS
appop "Tam ekran bildirim" USE_FULL_SCREEN_INTENT
appop "Üstte gösterme" SYSTEM_ALERT_WINDOW
check "Pil muafiyeti" "dumpsys deviceidle whitelist +$PKG" "dumpsys deviceidle whitelist" "$PKG"
check "Bekleme kovası active" "am set-standby-bucket $PKG active" "am get-standby-bucket $PKG" "^10$"
check "Bildirim erişimi" "cmd notification allow_listener $NLS" "settings get secure enabled_notification_listeners" "$NLS"
check "Rahatsız Etme erişimi" "cmd notification allow_dnd $PKG" "settings get secure enabled_notification_policy_access_packages" "$PKG"

# Erişilebilirlik: mevcut listeye eklenir, üzerine yazılmaz.
CUR=$(adb shell settings get secure enabled_accessibility_services)
if printf '%s' "$CUR" | grep -q "$A11Y"; then
  ok "Erişilebilirlik" "zaten açık"
else
  if [ "$CUR" = "null" ] || [ -z "$CUR" ]; then NEW="$A11Y"; else NEW="$CUR:$A11Y"; fi
  adb shell settings put secure enabled_accessibility_services "$NEW" >/dev/null
  adb shell settings put secure accessibility_enabled 1 >/dev/null
  if adb shell settings get secure enabled_accessibility_services | grep -q "$A11Y"; then ok "Erişilebilirlik"; else no "Erişilebilirlik" "listeye yazılamadı"; fi
fi

{
  echo
  echo "Başarısız adım: $FAIL"
  echo "Elle yapılacaklar (HyperOS; betikle yapılamaz):"
  echo " - Otomatik başlatma: açık"
  echo " - Pil: Kısıtlama yok"
  echo " - Son uygulamalarda Toparla'yı kilitle"
  echo " - Kilit ekranında göster, Arka planda açılır pencere"
} | tee -a "$OUT"
[ "$FAIL" = 0 ]
