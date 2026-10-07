# 0002 — Yasak kelime listesi yok

Tarih: 7 Ekim 2026  Durum: Kabul (yorum Kullanıcı'ya teyit ettirilecek)

**Bağlam:** Blueprint K12, A5-4, M26.4, Ek A.2 ve J1 bir yasak ifade listesi
(`res/raw/forbidden_tr.json`), bunu uygulayan ton doğrulayıcısı ve tüm metinlerde yasak
metin taraması tanımlıyor. Liste, blueprint'in kendi onaylı metinleriyle çakışıyordu
("Bugün hâlâ senin günün", "Yine de…"). Kullanıcı netleştirme turunda "yasak kelime
olmasın" dedi.

**Karar (benim yorumum):** Kelime listesine dayalı her mekanizma kaldırılır:

- `forbidden_tr.json` ve `ToneValidator`'ın kelime listesi kısmı yazılmaz.
- `strings.xml` / mikro-metin havuzları için yasak metin taraması testi yazılmaz.
- Prompt'lardaki `{{YASAK_IFADELER}}` değişkeni ve profildeki "yasak kelimeler" grubu çıkar.
- Dürüst Ayna doğrulayıcısının kelime kısmı çıkar; yapısal kuralları kalır
  (≤ 3 cümle, somut bir sonraki adım ya da seçimle biter, veri cümlesi kural katmanından gelir).

**Değişmeyenler** (kelime listesi değil, davranış ve güvenlik kuralı oldukları için):

- Seri sayacı yok; "Son 7 günde X" dili; gecikme için kırmızı yok; "Taşınan" terimi.
- Ek A.1 yazım ilkeleri (davranışı konuş, sayı dürüsttür, fiil + nesne) prompt'larda
  ve elle yazılan metinlerde yönergedir; makine denetimi yoktur.
- Tıbbi sınır doğrulayıcısı, kriz sözlüğü, Kırmızı veri maskeleme, uzunluk doğrulayıcısı,
  tekrar denetimi, yumuşama valfi.

**Alternatifler:** Kalıp tabanlı liste + izin listesi (ilk önerim; Kullanıcı tümden
kaldırmayı seçti).

**Sonuçlar ve riskler:** Güneş'in LLM ile ürettiği metinlerde kırıcı bir ifade artık
otomatik yakalanmaz; koruma prompt yönergesine, "Bu beni kırdı" geri bildirimine ve
valfe kalır. Ton altın seti hakem modelle ilke bazında değerlendirilir.

**İlgili blueprint bölümü:** K12, A5-4, M4, M26.4, F4.1, F12, J1, Ek A.2, Ek C
