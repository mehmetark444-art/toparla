---
name: karar-kaydi
description: Yeni karar kaydı (ADR) yazar ve bağlı belgeleri günceller. Blueprint'ten sapan, yeni izin ekleyen, teknoloji ya da platform seçimi yapan, Kullanıcı'nın verdiği ürün kararını kayda geçiren her durumda kullan.
argument-hint: "<kısa başlık>"
---

# Karar kaydı: $ARGUMENTS

1. `docs/decisions/` içindeki en büyük numarayı bul; yeni dosya `docs/decisions/NNNN-kisa-baslik.md`
   (ASCII, küçük harf, tire).
2. Şablon:

```markdown
# NNNN — Başlık

Tarih: <gün ay yıl>  Durum: Kabul | Aday (ölçüm bekliyor) | Değişti → NNNN | Geri alındı

**Bağlam:** Blueprint ne diyordu, ne oldu, kim ne istedi.

**Karar:** Ne yapılacak; tek anlamlı, uygulanabilir.

**Alternatifler:** Değerlendirilip seçilmeyenler ve nedeni.

**Sonuçlar ve riskler:** Bedeli, neyi zorlaştırdığı, nasıl izleneceği.

**İlgili blueprint bölümü:** K…, M…, D…
```

3. Kurallar:
   - Platforma bağlı seçim **ölçümden sonra** "Kabul" olur; öncesinde "Aday" (proje-beyni H12).
   - Kullanıcı'nın sözünü yorumladıysan yorumun olduğunu yaz ve teyit iste.
   - Bedeli gizleme: blueprint'in kaygısıyla çelişiyorsa açıkça yaz.
   - Eski karar değişiyorsa eskisini silme; durumunu "Değişti → NNNN" yap.
4. Bağlı güncellemeler (aynı commit):
   - `CLAUDE.md` → "Blueprint'i ezen kararlar" özetine tek satır.
   - `docs/proje-beyni.md` → Bölüm 5 tablosuna satır; karar adayıysa "Bekleyen karar adayları".
   - Karar yeni iş doğuruyorsa `docs/yol-haritasi.md`'ye madde.
5. `node .claude/skills/dogrula/kontrol.mjs` → karar özetinin CLAUDE.md'de geçtiğini doğrular.
