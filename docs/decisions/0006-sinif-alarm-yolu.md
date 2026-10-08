# 0006 — Sınıf → alarm yolu

Tarih: 8 Ekim 2026  Durum: Kabul (ölçüme dayalı)

**Bağlam:** Blueprint G1: Kritik `setAlarmClock`, Önemli `setExactAndAllowWhileIdle`, Normal
`setAndAllowWhileIdle` ya da WorkManager. Karar 0003 ısrarlı takip için `setExactAndAllowWhileIdle`
demişti (ölçümden önce yazılmıştı). 7–8 Ekim gece testi üç yolu 8 saat boyunca ölçtü
(`platform-bulgulari.md`, "Spike 1: gece testi sonucu").

**Ölçüm özeti (bu telefon, hiçbir özel izin verilmeden):**
- `setAlarmClock`: 8/8, en çok 1,4 sn.
- `setExactAndAllowWhileIdle`: 8/8, en çok 28 sn. Önceki tek 211 sn'lik gecikme tekrarlanmadı.
- `setAndAllowWhileIdle`: 8/8 çaldı ama **1 sa 54 dk – 4 sa 54 dk geç**; ekran açıkken bile.

**Karar:**

| Sınıf | Alarm yolu |
|---|---|
| Kritik | `setAlarmClock` |
| Önemli | `setExactAndAllowWhileIdle` |
| Normal (rutin, alışkanlık penceresi) | `setExactAndAllowWhileIdle` (blueprint'ten sapma) |
| Israrlı takip (karar 0003) | `setExactAndAllowWhileIdle` (0003'teki seçim ölçümle doğrulandı) |
| Bilgi | `setAndAllowWhileIdle` (saatlerce kayabilir; zamanı önemli olan hiçbir şey bu yolla kurulmaz) |

Kodda tek değişim noktası `ReminderPlanner.apiFor`.

**Alternatifler:** Normal sınıfı blueprint'teki gibi esnek bırakmak (bir rutin hatırlatmasının
saatlerce geç gelmesi kabul edilemez). Israrlı takibi `setAlarmClock`'a taşımak (her 30 dakikada
sistemin "sonraki alarm" göstergesini kirletir; 28 sn'lik sapma bu iş için yeterli).

**Sonuçlar ve riskler:** Kesin alarm sayısı artar; pil etkisi F10'da ölçülür. Derin Doze
(`idle=true`) gece testinde alarm anında hiç gözlenmedi; o koşul ayrıca sınanacak (yol haritası F1.1).
Esnek yolun neden ekran açıkken de saatlerce beklediği bilinmiyor.

**İlgili blueprint bölümü:** G1, M6, Bölüm I; karar 0003
