---
name: yeni-modul
description: Bir blueprint modülüne (M1-M30) ya da yol haritası maddesine başlama usulü - ilgili blueprint bölümlerini okuma, kabul kriterlerini teste çevirme, önce domain sonra veri sonra arayüz. Yeni bir özellik, ekran ya da modül yazmaya başlarken kullan.
argument-hint: "<modül ya da madde, ör. M3 ya da F3.4>"
---

# Yeni modül / madde: $ARGUMENTS

## 1. Oku
- `docs/yol-haritasi.md`: maddenin fazı ve faz kapısı (önceki fazın şartı sağlanmış mı?).
- `docs/BLUEPRINT.md`: modül bölümü (E), ekranı (D…), tabloları (H…), varsa AI kısmı (F…), platform (G…).
  Dosya büyük; başlıkla ara, yalnız ilgili aralığı oku.
- `docs/decisions/`: modüle dokunan kararlar (0001 Gemini, 0002 yasak kelime yok, 0003 bütçe ve ısrarlı takip…).
- `docs/proje-beyni.md` Bölüm 7–9: bu telefona dair bilinenler ve tuzaklar.

## 2. Kabul kriterlerini test listesine çevir
Modülün "Kabul" satırlarını ve ilgili senaryoları (S1–S23) tek tek test adına dönüştür.
Ölçülebilir olmayan kriter varsa ölçülebilir hâlini yaz. Listeyi koda geçmeden önce çıkar.

## 3. Sıra
1. `:domain` — modeller, kurallar, saf fonksiyonlar. **Önce test.**
2. `:data` — tablo, DAO, repository, migration (+ testleri).
3. Yan etki arayüzü ve Android uygulaması (`:reminders` / `:sensors` / `:ai`).
4. `:ui` bileşenleri → `:app/feature/<ad>` ekranı (ViewModel: State / Event / Effect).

## 4. İlk günden
- **AI kapalı durumu:** `[AI]` işaretli her davranışın kural tabanlı karşılığı önce yazılır ve testlidir.
- Bitmemiş özellik `FeatureFlags` arkasında kapalı.
- Durumlar: boş / yükleniyor / hata / çevrimdışı / AI kapalı / izin yok.
- Geri al yolu ve olay günlüğü kaydı.
- Metinler `strings.xml` ve mikro-metin havuzlarında; seri sayacı yok, geciken iş "Taşınan".

## 5. Kapsam
Blueprint'te olmayan fikir koda girmez → `docs/ideas.md`. Belirsizlikte en az sürprizli, en az izinli,
en geri alınabilir seçeneği seç ve `/karar-kaydi` yaz. Kullanıcı'ya en fazla bir soru.

## 6. Bitir
`/dogrula` → yol haritasında maddeyi kanıtıyla işaretle → commit. Tek oturumda tek madde.
