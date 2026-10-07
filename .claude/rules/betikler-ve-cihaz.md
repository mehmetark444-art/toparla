---
paths:
  - "scripts/**"
  - "spike/**"
  - ".claude/hooks/**"
---

# Betikler, spike ve cihaz işleri

- `adb` için her zaman `./scripts/adb …` kullan (PATH'te değil; yol çevirme sorununu da çözer).
- `adb shell` içeren her betikte `export MSYS_NO_PATHCONV=1` (proje-beyni H2).
- Türkçe metin ya da kesme işareti içeren dosyayı bash heredoc ile oluşturma; dosya yazma aracını
  kullan (H3). Git Bash'te `pkill`, `jq` yok; `node` ve `python` var.
- `.properties` dosyasına Windows yolu ileri eğik çizgiyle yazılır (H1).
- Bekleme koşulunu belirli alarm anahtarına bağla, toplam sayıya değil (H5).
- Cihaz testinden önce: ön koşulu komutla doğrula (kilit türü, bekleyen alarm, izin) ve Kullanıcı'ya
  neye dokunmayacağını, ne kadar süreceğini yaz (H6, H7). Ayrıntı: `/cihaz-testi`.
- Kilitliyken `run-as` çalışmaz; `./scripts/adb logcat -d -s ETIKET` kullan (H8).
- `adb install` telefonda ~10 sn içinde onay ister; `Success` görmeden devam etme (H9).
- Kancalar Node ile yazılır (`.mjs`), bağımlılıksızdır; değişiklikten sonra `bash .claude/hooks/sinama.sh`.
