---
name: oturum-basla
description: Yeni oturumu ya da sıkıştırılmış bağlamı projeye yeniden bağlar - hafızayı ve durumu okur, bu oturumun tek maddesini seçer, hedefi yazar. Oturum başında, "kaldığımız yerden devam", "neredeydik" denince ya da bağlam kaybolduğunda kullan.
---

# Oturum başlangıcı

## Güncel durum (otomatik)
```!
git log --oneline -8 2>/dev/null || true
git status --short 2>/dev/null || true
```

## Yapılacaklar
1. `docs/proje-beyni.md` oku: Bölüm 3 (durum), 5 (kararlar), 8 (hatalar), 9 (tuzaklar), 10 (açık sorular).
2. `docs/yol-haritasi.md` en üstü: "Şu an", "Sıradaki tek adım", "Açık engeller".
3. `docs/progress.md` son girişi.
4. Açık engeller Kullanıcı'da mı? Öyleyse önce onların durumunu **kayıttan** doğrula
   (ör. telefon bağlı mı: `./scripts/adb devices`).
5. Bu oturumun **tek** maddesini seç (ör. F2.11). Faz kapısı şartı sağlanmamış maddeye başlama.
6. Hedefi iki cümleyle `docs/progress.md`'ye yaz.
7. Maddeye göre: yeni özellik → `/yeni-modul`; telefon işi → `/cihaz-testi`.

## Kullanıcı'ya ilk mesaj
En çok 5 madde: nerede kaldık · bu oturumda ne yapacağım · senden gereken tek şey (varsa, numaralı adımla).
"Şunu yapıyorum; itiraz etmezsen devam."
