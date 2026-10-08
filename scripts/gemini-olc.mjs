// Spike 11 (kalanlar): görsel girdi, bağlam önbellekleme, arama + şemalı çıktı, yönlendirme adresini çözme,
// hız sınırı. Anahtar secrets.properties'ten okunur ve hiçbir yere yazdırılmaz.
// Kullanım (proje kökünden): node scripts/gemini-olc.mjs <görsel.png> [gorsel|onbellek|arama|hiz ...]
import { readFileSync } from 'node:fs';

const KEY = readFileSync('secrets.properties', 'utf8').match(/^GEMINI_API_KEY=(.+)$/m)?.[1]?.trim();
if (!KEY) throw new Error('secrets.properties içinde GEMINI_API_KEY yok');
const BASE = 'https://generativelanguage.googleapis.com/v1beta';
const LITE = 'gemini-3.5-flash-lite';
const DAILY = 'gemini-3.8-flash';
const [image, ...only] = process.argv.slice(2);
const want = (name) => only.length === 0 || only.includes(name);

async function post(path, body) {
  const t0 = Date.now();
  const res = await fetch(`${BASE}/${path}`, {
    method: 'POST',
    headers: { 'x-goog-api-key': KEY, 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* gövde JSON değil */ }
  return { status: res.status, ms: Date.now() - t0, json, text, headers: res.headers };
}
const gen = (model, body) => post(`models/${model}:generateContent`, body);
const out = (r) => r.json?.candidates?.[0]?.content?.parts?.map((p) => p.text ?? '').join('') ?? '';
const usage = (r) => JSON.stringify(r.json?.usageMetadata ?? {});
const err = (r) => (r.status === 200 ? '' : ` HATA: ${r.json?.error?.status} ${r.json?.error?.message?.slice(0, 200)}`);

if (want('gorsel')) {
  console.log('\n== 1. Görsel girdi (belgeden tarih ve tutar çıkarma, şemalı)');
  for (const model of [LITE, DAILY]) {
    const r = await gen(model, {
      contents: [{ parts: [
        { inline_data: { mime_type: 'image/png', data: readFileSync(image).toString('base64') } },
        { text: 'Bu belgeden bilgileri çıkar.' },
      ] }],
      generationConfig: {
        thinkingConfig: model === DAILY ? { thinkingLevel: 'low' } : undefined,
        responseMimeType: 'application/json',
        responseSchema: { type: 'OBJECT', required: ['kurum', 'sonOdeme', 'tutarTl'], properties: {
          kurum: { type: 'STRING' }, sonOdeme: { type: 'STRING', description: 'YYYY-AA-GG' },
          tutarTl: { type: 'NUMBER' }, aboneNo: { type: 'STRING' },
        } },
      },
    });
    console.log(`${model}: ${r.status} ${r.ms} ms ${out(r).replace(/\s+/g, ' ')} ${usage(r)}${err(r)}`);
  }
}

if (want('onbellek')) {
  console.log('\n== 2. Bağlam önbellekleme');
  const filler = Array.from({ length: 260 }, (_, i) =>
    `Kural ${i + 1}: Güneş kısa, sıcak ve yargısız konuşur; tıbbi tavsiye vermez; her yanıtta en fazla bir soru sorar; örnek ${i + 1} için geçerlidir.`).join('\n');
  const body = () => ({
    systemInstruction: { parts: [{ text: filler }] },
    contents: [{ role: 'user', parts: [{ text: 'Kural 7 ne diyor? Tek cümle.' }] }],
  });
  for (const model of [LITE, DAILY]) {
    for (const n of [1, 2, 3]) {
      const r = await gen(model, { ...body(), generationConfig: model === DAILY ? { thinkingConfig: { thinkingLevel: 'low' } } : {} });
      const u = r.json?.usageMetadata ?? {};
      console.log(`örtük ${model} #${n}: ${r.status} ${r.ms} ms girdi=${u.promptTokenCount} önbellekten=${u.cachedContentTokenCount ?? 0}${err(r)}`);
    }
  }
  const created = await post('cachedContents', {
    model: `models/${LITE}`, ttl: '300s',
    systemInstruction: { parts: [{ text: filler }] },
  });
  console.log(`açık önbellek oluşturma: ${created.status} ${created.ms} ms ad=${created.json?.name ? 'var' : 'yok'} ${JSON.stringify(created.json?.usageMetadata ?? {})}${err(created)}`);
  if (created.json?.name) {
    const r = await gen(LITE, { cachedContent: created.json.name, contents: [{ role: 'user', parts: [{ text: 'Kural 7 ne diyor? Tek cümle.' }] }] });
    console.log(`açık önbellekle çağrı: ${r.status} ${r.ms} ms ${usage(r)}${err(r)}`);
    const del = await fetch(`${BASE}/${created.json.name}`, { method: 'DELETE', headers: { 'x-goog-api-key': KEY } });
    console.log(`önbellek silme: ${del.status}`);
  }
}

if (want('arama')) {
  console.log('\n== 3. Arama + şemalı çıktı birlikte; yönlendirme adresini çözme');
  const q = 'Android 17 kararlı sürümü hakkında bu haftanın en yeni haberi nedir? Kaynak göster.';
  const both = await gen(LITE, {
    contents: [{ parts: [{ text: q }] }],
    tools: [{ google_search: {} }],
    generationConfig: { responseMimeType: 'application/json', responseSchema: { type: 'OBJECT', properties: { ozet: { type: 'STRING' }, tarih: { type: 'STRING' } } } },
  });
  const md = both.json?.candidates?.[0]?.groundingMetadata;
  console.log(`arama+şema: ${both.status} ${both.ms} ms kaynak=${md?.groundingChunks?.length ?? 0} çıktı=${out(both).slice(0, 160).replace(/\s+/g, ' ')}${err(both)}`);
  const plain = both.status === 200 && md?.groundingChunks?.length ? both : await gen(LITE, { contents: [{ parts: [{ text: q }] }], tools: [{ google_search: {} }] });
  const pmd = plain.json?.candidates?.[0]?.groundingMetadata;
  console.log(`yalnız arama: ${plain.status} ${plain.ms} ms kaynak=${pmd?.groundingChunks?.length ?? 0} ${usage(plain)}${err(plain)}`);
  for (const chunk of (pmd?.groundingChunks ?? []).slice(0, 4)) {
    const t0 = Date.now();
    const head = await fetch(chunk.web.uri, { method: 'HEAD', redirect: 'manual' });
    const loc = head.headers.get('location') ?? '';
    let date = '';
    if (loc) {
      try {
        const page = await fetch(loc, { signal: AbortSignal.timeout(8000), headers: { 'user-agent': 'Mozilla/5.0' } });
        const html = (await page.text()).slice(0, 200000);
        date = html.match(/(?:article:published_time|datePublished)["'\s:=]+(?:content=)?["']?([0-9T:+.Z-]{10,30})/i)?.[1] ?? 'tarih bulunamadı';
      } catch (e) { date = `sayfa okunamadı (${e.name})`; }
    }
    console.log(`  ${chunk.web.title} → ${head.status} ${loc ? new URL(loc).origin + new URL(loc).pathname.slice(0, 60) : 'yönlendirme yok'} · ${date} · ${Date.now() - t0} ms`);
  }
}

if (want('hiz')) {
  console.log('\n== 4. Hız sınırı: 40 eşzamanlı kısa çağrı');
  const results = await Promise.all(Array.from({ length: 40 }, () =>
    gen(LITE, { contents: [{ parts: [{ text: 'Tek sözcükle yanıtla: merhaba' }] }], generationConfig: { maxOutputTokens: 8 } })));
  const counts = {};
  for (const r of results) counts[r.status] = (counts[r.status] ?? 0) + 1;
  console.log(`durum kodları: ${JSON.stringify(counts)} · en uzun ${Math.max(...results.map((r) => r.ms))} ms`);
  const limited = results.find((r) => r.status === 429);
  if (limited) console.log(`429 gövdesi: ${limited.json?.error?.status} ${limited.json?.error?.message?.slice(0, 200)} retry-after=${limited.headers.get('retry-after')} ayrıntı=${JSON.stringify(limited.json?.error?.details?.map((d) => d['@type']?.split('.').pop() + (d.retryDelay ? ':' + d.retryDelay : '')))}`);
}
