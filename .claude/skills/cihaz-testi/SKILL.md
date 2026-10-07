---
name: cihaz-testi
description: Telefonda (Xiaomi 17T Pro, HyperOS 3) ölçüm ya da spike koşma usulü - adb kullanımı, ön koşul doğrulama, Kullanıcı'yı yönlendirme, kaydı okuma, bulguyu yazma. adb, telefon, cihaz, spike, alarm testi, kurulum, logcat, Doze, yeniden başlatma içeren her işte kullan.
---

# Cihaz testi usulü

Bu projede cihaz testlerinde yapılan hataların hepsi `docs/proje-beyni.md` Bölüm 8'de (H2, H5–H10).
Aşağıdaki sıra o hataları önlemek için var.

## 0. Araç
- Her zaman `./scripts/adb …` (PATH ve yol çevirme sorununu çözer).
- Spike uygulaması: `com.toparla.spike`, günlük etiketi `TOPARLA_SPIKE`,
  kayıt `/data/user_de/0/com.toparla.spike/files/log.csv`.

## 1. Bağlantı ve ön koşullar (komutla doğrula, varsayma)
```bash
./scripts/adb devices                      # "device" görünmeli
./scripts/adb shell dumpsys user | grep -m1 "State:"            # RUNNING_UNLOCKED / RUNNING_LOCKED
./scripts/adb shell dumpsys lock_settings | grep CredentialType # NONE ise kilitli senaryo sınanamaz
./scripts/adb shell dumpsys alarm | grep -c "walarm.:com.toparla.spike"   # bekleyen alarm; teste temiz başla
./scripts/adb shell dumpsys battery | grep -E "level|status"
```
Testin dayandığı her koşul için böyle bir kontrol yaz. Koşul sağlanmıyorsa testi koşma.

## 2. Kullanıcı'ya önceden söyle
Teste başlamadan **önce** yaz: ne olacak, ne kadar sürecek, neye dokunmayacak, ne zaman ne yapacak.
Adımlar numaralı ve tek eylemli; menü adları telefondaki Türkçe hâliyle.
Örnek: "Yaklaşık 6 dakika telefona dokunma. Ekran kendiliğinden kapanacak, bu normal."

## 3. Kurulum
```bash
./gradlew --console=plain -q :spike:assembleDebug
./scripts/adb install -r spike/build/outputs/apk/debug/spike-debug.apk
```
Telefonda "USB ile yüklensin mi?" penceresi çıkar ve ~10 sn içinde onay ister; Kullanıcı'yı önceden uyar.
`Success` görmeden devam etme. `INSTALL_FAILED_USER_RESTRICTED` = onaylanmadı ya da
Geliştirici seçenekleri → "USB ile yükle" kapalı.

## 4. Koşma
- Bekleme koşulunu **belirli alarm anahtarına** bağla (toplam `FIRED` sayısına değil).
- Uzun bekleme: komutu arka planda koş; yerel `sleep` yerine `./scripts/adb shell "sleep N"`.
- Yeniden başlatma, kaldırma, veri silme Kullanıcı'ya sorulur.
- USB bağlıyken Doze zorlanamaz ve `set-standby-bucket rare` kalıcı olmaz: bunları kablo çekili
  gerçek beklemeyle ölç.

## 5. Kaydı oku (beyanı değil)
```bash
./scripts/adb shell run-as com.toparla.spike cat /data/user_de/0/com.toparla.spike/files/log.csv
./scripts/adb logcat -d -s TOPARLA_SPIKE:I      # telefon kilitliyken run-as çalışmaz
```
Kullanıcı "geldi" dese de kaydı oku ve kayıtla beyanı karşılaştır. Beklenmedik satır (benim kurmadığım
alarm, silinmiş günlük, hemen gelen `BOOT_COMPLETED`) = veri kirli ya da ön koşul yanlış; sonuç sayma.

## 6. Bulguyu yaz (aynı oturumda)
- `docs/platform-bulgulari.md`: tarih · ne denendi · yöntem · kaç ölçüm · sonuç · **sınırlar** (tek deneme mi, USB bağlı mıydı).
- `docs/proje-beyni.md` Bölüm 7'ye özet; hata yapıldıysa Bölüm 8'e (`/hata-kaydi`).
- `docs/yol-haritasi.md`: ilgili F1.x maddesi (kanıtıyla).
- Telefonu normal duruma döndür: `deviceidle unforce`, `battery reset`, kova `active`, ekranı uyandır.
