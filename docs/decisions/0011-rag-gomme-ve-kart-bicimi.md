# 0011 — RAG: gömme modeli, hibrit arama, kart biçimi ve yanıt politikası

Tarih: 8 Ekim 2026  Durum: Kabul (ölçüme dayalı; Kullanıcı kararı geliştiriciye devretti: "kararları benim yerime verebilirsin")

**Bağlam:** Kullanıcı, Güneş'in uzmanlaşmak istediği konularda telefonda "uzman" olmasını istiyor
(kararlar 0009, 0010). Blueprint F5.3: yerel gömme (EmbeddingGemma benzeri, 256 boyut), FTS5 BM25 +
kosinüs, RRF. Ölçüm: `platform-bulgulari.md` § "Spike 10b: gerçek RAG" (24 Türkçe kart, 24 soru).

**Karar:**
1. **Gömme modeli:** EmbeddingGemma 2 text 270m (`embeddinggemma-2-text-270m.litertlm`, 165 MB, CPU).
   Görev önekleriyle kullanılır: kart `title: none | text: …`, soru `task: search result | query: …`.
   Vektör **256 boyut** (768'e göre kayıp yok, bu sette bir soru daha iyi). Kart başına ~60 ms.
2. **Hibrit arama:** gömme sıralaması + sözcük sıralaması, RRF ile birleştirilir (ilk-1 isabet 15 → 17/20).
   Blueprint F5.3 doğrulandı; yalnız sözcük araması yetersiz (8/20).
3. **Kart biçimi:** her bilgi kartı gövdenin yanında bir **"diğer ifadeler"** alanı taşır (eş anlamlılar,
   gündelik söyleyişler, sorulabilecek biçimler). Kartı yazan öğretmen model (Gemini) doldurur;
   gömmeye ve bağlama gövdeyle birlikte girer. Ölçümde ilk-1 isabet 15 → 19/20.
   `KnowledgeCard` tablosuna `aliases` alanı eklenir (blueprint H5'e ek).
4. **Yanıt politikası:** yanıt talimatı "yalnız kartlara dayan; yoksa 'Kartlarda bu bilgi yok' de" (katı).
   - Bağlama ilk 3 kart girer.
   - **Model "yok" dediğinde ama arama güvenle kart bulduysa** çıplak ret gösterilmez: bulunan
     kart(lar) "Bulduğum en yakın bilgi" olarak kaynaklarıyla gösterilir. (Ölçümde modelin 6–7 reddinin
     5'inde doğru kart zaten bağlamdaydı.)
   - Konu Yeşil ise (bilgi kartları kişisel değildir) aynı soru karar 0009 uyarınca buluta
     yükseltilebilir; kişisel veri içeriyorsa Kullanıcı yönlendirir.
   - Kartlarda olmayan bilgi asla uydurulmaz: bu sette 12 denemede 0 uydurma.

**Bilinen sınır:** Gemma 4 E4B karta sadık ama **harfiyen**: yanıt için eş anlamlı eşleştirme ya da
bir adımlık çıkarım gerekince çoğu kez "yok" diyor (akar ↔ varroa, "gösterge 0,5'e düştü" ↔ basınç,
"sıcak ekmeği dilimlemek" ↔ soğutma). Yanıtlanabilir 20 sorunun 13'ünü anahtar sözcüğüyle doğru yanıtladı
(biri de anahtar sözcüksüz doğru: fiilen 14). Daha gevşek talimat +1, kart zenginleştirme yanıt oranını
değiştirmedi. Düşünme kipi (`ThinkingConfig`) ve iki adımlı yanıt (önce ilgili kartı seç) **denenmedi**.

**Alternatifler:** 768 boyut (kazanç yok, 3 kat yer). Yalnız gömme (hibritten 2 soru geride).
Gevşek talimat (kazanç 1 soru; uydurma riski bu küçük sette görülmedi ama ölçülemeyecek kadar az örnek).

**Sonuçlar ve riskler:** "Telefonda uzman" hedefi için arama kısmı güçlü, üretim kısmı temkinli:
kullanıcı yanlış bilgi almaz ama her üç sorudan birinde "kartı gösteriyorum" ya da buluta yükseltme
görür. Set küçük (24 soru) ve "diğer ifadeler" satırlarını test sorularını bilen kişi yazdı: 19/20
bir üst sınırdır. Gerçek ölçüm, Konu Motoru'nun ürettiği kartlarla F9'da yapılır.

**İlgili blueprint bölümü:** F5.3, F7, M21-11, M24.4, M24.5, H5; kararlar 0009, 0010
