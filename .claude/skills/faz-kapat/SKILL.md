---
name: faz-kapat
description: Bir fazı kapatma töreni - K1 makine kontrolü, K2 Kullanıcı onayı, K3 proje beyni güncellemesi, yol haritası işaretleme ve kapanış commit'i. "Fazı kapat, F2 bitti mi, faz kapanışı" denince kullan.
argument-hint: "<faz, ör. F2>"
---

# Faz kapanışı: $ARGUMENTS

Bir faz üç kontrol de geçmeden kapanmaz. Sırayı bozma; bir adım kalırsa dur ve neyin eksik olduğunu söyle.

## 1. Maddeler
`docs/yol-haritasi.md` içinde fazın bölümünü oku. ☐ / ◐ / ⛔ kalan madde varsa faz kapanamaz:
kalanları listele ve dur. Her ☑ maddenin satır sonunda kanıtı olmalı.

## 2. K1 — Makine kontrolü
`/dogrula $ARGUMENTS` koş. Fazın "Çift kontrol" bölümündeki K1 satırında yazan her ölçütü tek tek
kanıtla (test adı, ölçüm, komut çıktısı). Kanıtlanamayan ölçüt = K1 kaldı.

## 3. K2 — Gerçek dünya kontrolü
K2 satırındaki ölçütleri Kullanıcı'ya sade Türkçeyle, tek tek sor. Kayıttan doğrulanabilen her şeyi
(teslim günlüğü, kullanım günleri) önce kendin doğrula; beyanı kanıt sayma.
**K2'yi yalnız Kullanıcı'nın bu oturumdaki açık onayı işaretler.** Onay yoksa faz ◐ kalır.

## 4. K3 — Proje beyni
`docs/proje-beyni.md` Bölüm 12'deki kontrol listesini uygula:
1. Üst satır: son güncelleme, kapsadığı son commit, kapanan son faz.
2. Bölüm 3 (şu anki durum) yeniden yaz.
3. Bölüm 4: yeni modüller, sınıflar, sürümler.
4. Bölüm 5: fazda alınan kararlar; kapanan karar adayları.
5. Bölüm 6: fazın zaman çizelgesi girişi ve commit'leri.
6. Bölüm 7–9: yeni bulgular, **fazda yapılan her hata**, yeni tuzaklar.
7. Bölüm 10: kapanan sorular "kapandı: …" diye işaretlenir; yenileri eklenir.
8. Bölüm 13: faz kapanış kaydı satırı.

## 5. İşaretle ve commit'le
Sıra önemli (faz kapısı kancası proje beyninde kayıt arar):
1. Önce proje beynini kaydet.
2. Sonra yol haritasında K1, K2, K3 satırlarını ve "Genel durum" tablosunda fazı ☑ yap;
   "Şu an / Sıradaki tek adım / Açık engeller" ve "Son güncelleme"yi güncelle; değişiklik günlüğüne satır ekle.
3. `docs/progress.md`'ye kapanış girişi.
4. `node .claude/skills/dogrula/kontrol.mjs --faz $ARGUMENTS` → "tutarlı" olmalı.
5. Tek commit: `Faz $ARGUMENTS kapanisi: <özet>`.

## 6. Kullanıcı'ya rapor
Üç başlık: **Ne bitti** · **Telefonda neyi dene** (3 madde) · **Sıradaki faz**.
