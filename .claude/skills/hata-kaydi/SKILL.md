---
name: hata-kaydi
description: Yapılan bir hatayı, yanlış varsayımı ya da geri alınan işi proje beynine aynı gün kaydeder. Bir test yanlış yorumlandığında, komut beklenmedik biçimde başarısız olduğunda, bir iş geri alındığında, Kullanıcı bir yanlışı düzelttiğinde kullan.
argument-hint: "[kısa tanım]"
---

# Hata kaydı

`docs/proje-beyni.md` Bölüm 8 tablosuna yeni satır ekle (sıradaki H numarası):

| Sütun | Ne yazılır |
|---|---|
| Ne oldu | Gözlenen olay; somut (komut, hata metni, yanlış sonuç). |
| Kök neden | Belirti değil neden. Bilinmiyorsa "bilinmiyor" yaz, uydurma. |
| Ders / kural | Bir dahaki sefere farklı yapılacak **somut** şey. |

Kurallar:
- Aynı gün yaz; faz kapanışını bekleme.
- Hata kimin olursa olsun (geliştirici, araç, Kullanıcı adımı) suçlayıcı dil yok; amaç tekrarlanmaması.
- Hatayı küçültme ya da gizleme. "Geçti" sanılıp sonradan geçmediği anlaşılan şey özellikle yazılır.
- Ders bir kurala dönüşüyorsa uygun yere de işle:
  kod kuralı → `.claude/rules/kod.md` (denetlenebiliyorsa `.claude/hooks/kod-kurallari.mjs`);
  cihaz usulü → `/cihaz-testi`; betik tuzağı → `.claude/rules/betikler-ve-cihaz.md`.
- Tekrarlanan hata (aynı kök neden ikinci kez): mevcut satıra "tekrar: <tarih>" ekle ve neden
  kuralın işlemediğini yaz; gerekiyorsa kancayla zorunlu kıl.
- Üst satırdaki "Son güncelleme"yi güncelle.

Konu: $ARGUMENTS
