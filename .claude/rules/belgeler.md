---
paths:
  - "docs/**"
  - "CLAUDE.md"
  - "AGENTS.md"
---

# Belge kuralları

| Dosya | Kural |
|---|---|
| `docs/BLUEPRINT.md`, `docs/arsiv/**` | **Değiştirilmez** (kanca engeller). Sapma → karar kaydı. |
| `docs/decisions/NNNN-*.md` | Sıradaki numara; şablon `/karar-kaydi`. Kabul edilen karar silinmez; değişirse yeni kayıt + eskisinde "Değişti → NNNN". |
| `docs/yol-haritasi.md` | Tek durum kaynağı. ☑ yalnız kanıtla (commit, test, bulgu başlığı satır sonunda). Faz, K1 + K2 + K3 geçmeden kapanmaz. |
| `docs/proje-beyni.md` | Hafıza. Karar, hata, bulgu **aynı gün** yazılır. Geçmiş silinmez; yanlış bilgi üstü çizilip düzeltilir. |
| `docs/platform-bulgulari.md` | Her bulgu: ne denendi · nasıl · kaç ölçüm · sonuç · sınırlar. Kirli veri sonuç sayılmaz. |
| `docs/progress.md` | Oturum günlüğü; kısa. |
| `docs/ideas.md` | Kapsam dışı fikirler; koda girmez. |

- Yol haritasını ve proje beynini **düzenleme aracıyla** değiştir (sed ile değil): faz kapısı kancası
  yalnız düzenleme aracını görür.
- K2'yi (gerçek dünya kontrolü) yalnız Kullanıcı'nın açık onayı işaretler. "Geldi / oldu / yükledim"
  beyanı kanıt değildir; kayıttan doğrula.
- Belgelere API anahtarı, parola, kişisel sağlık verisi yazılmaz (kanca engeller).
- Her şey Türkçe. Ölçülmemiş şeyi ölçülmüş gibi yazma; "tek deneme", "doğrulanmadı" de.
