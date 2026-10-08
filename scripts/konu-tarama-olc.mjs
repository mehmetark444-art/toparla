// F1.17: bir Konu Motoru taramasının gerçek token ve arama tüketimi. Anahtar secrets.properties'ten okunur.
// Tarama = (1) aramalı çağrı (kaynaklar buradan gelir) + (2) bulguları şemalı özete döken ayrı çağrı
// (tek çağrıda kaynak listesi kayboluyor: platform-bulgulari § Spike 11 kalanlar). Konular uydurmadır.
// Kullanım: node scripts/konu-tarama-olc.mjs
import fs from 'node:fs';

const KEY = fs.readFileSync('secrets.properties', 'utf8').match(/^GEMINI_API_KEY=(.+)$/m)?.[1]?.trim();
if (!KEY) throw new Error('secrets.properties içinde GEMINI_API_KEY yok');
const BASE = 'https://generativelanguage.googleapis.com/v1beta';
// Fiyatlar: karar 0007 (resmi sayfa, 7 Ekim 2026), $ / 1M token.
const TIERS = {
  'gemini-3.5-flash-lite': { in: 0.30, out: 2.50 },
  'gemini-3.8-flash': { in: 0.75, out: 3.75 },
};
const SEARCH_FREE_PER_MONTH = 5000;
const SEARCH_PRICE_PER_1000 = 14;
const SCANS_PER_DAY = 2;
const DAYS = 30;
const TOPICS = [
  'Xiaomi HyperOS 3 güncellemeleri ve yeni özellikler',
  'yetişkinlerde DEHB üzerine yeni yayımlanan bilimsel araştırmalar',
  'Türkiye elektrikli otomobil pazarı fiyat ve model haberleri',
];

async function gen(model, body) {
  const t0 = Date.now();
  const res = await fetch(`${BASE}/models/${model}:generateContent`, {
    method: 'POST', headers: { 'x-goog-api-key': KEY, 'content-type': 'application/json' }, body: JSON.stringify(body),
  });
  const json = await res.json();
  if (res.status !== 200) throw new Error(`${model} HTTP ${res.status}: ${json?.error?.message?.slice(0, 200)}`);
  const u = json.usageMetadata ?? {};
  return {
    json, ms: Date.now() - t0,
    text: json.candidates?.[0]?.content?.parts?.map((p) => p.text ?? '').join('') ?? '',
    tin: u.promptTokenCount ?? 0, tout: (u.candidatesTokenCount ?? 0) + (u.thoughtsTokenCount ?? 0),
  };
}
const cost = (model, r) => (r.tin * TIERS[model].in + r.tout * TIERS[model].out) / 1e6;

for (const model of Object.keys(TIERS)) {
  const thinking = model === 'gemini-3.8-flash' ? { thinkingConfig: { thinkingLevel: 'low' } } : {};
  let total = 0;
  let queries = 0;
  for (const topic of TOPICS) {
    const search = await gen(model, {
      contents: [{ parts: [{ text: `Konu: ${topic}\nSon 3 günde bu konuda çıkan en önemli en fazla 5 gelişmeyi madde madde yaz; her maddede ne olduğu ve tarihi olsun. Yeni bir şey yoksa "yeni gelişme yok" yaz.` }] }],
      tools: [{ google_search: {} }],
      generationConfig: thinking,
    });
    const md = search.json.candidates?.[0]?.groundingMetadata ?? {};
    const sources = (md.groundingChunks ?? []).map((c, i) => `[${i}] ${c.web?.title}`).join('\n');
    const digest = await gen(model, {
      contents: [{ parts: [{ text: `Aşağıdaki bulguları kısa bir özete dök. Yalnız bulgularda olanı yaz; kaynak numarası bulgunun dayandığı kaynaktır.\nBULGULAR:\n${search.text}\nKAYNAKLAR:\n${sources}` }] }],
      generationConfig: {
        ...thinking, responseMimeType: 'application/json',
        responseSchema: { type: 'OBJECT', required: ['nothingNew', 'items'], properties: {
          nothingNew: { type: 'BOOLEAN' },
          items: { type: 'ARRAY', items: { type: 'OBJECT', required: ['title', 'summary', 'source'], properties: { title: { type: 'STRING' }, summary: { type: 'STRING' }, source: { type: 'INTEGER' } } } },
        } },
      },
    });
    let items = -1;
    try { items = JSON.parse(digest.text).items.length; } catch { /* geçersiz JSON: -1 olarak raporlanır */ }
    const c = cost(model, search) + cost(model, digest);
    total += c;
    queries += (md.webSearchQueries ?? []).length;
    console.log(`${model} · ${topic.slice(0, 34)}… arama ${search.tin}/${search.tout} tok ${search.ms} ms, sorgu=${(md.webSearchQueries ?? []).length} kaynak=${(md.groundingChunks ?? []).length} · özet ${digest.tin}/${digest.tout} tok ${digest.ms} ms, madde=${items} · ${(c * 100).toFixed(3)} sent`);
  }
  const perScan = total / TOPICS.length;
  const perMonth = perScan * SCANS_PER_DAY * DAYS;
  const q = queries / TOPICS.length;
  console.log(`== ${model}: tarama başına ${(perScan * 100).toFixed(3)} sent · konu başına ayda (günde ${SCANS_PER_DAY}) ${perMonth.toFixed(3)} $ · tarama başına ${q.toFixed(1)} arama sorgusu → konu başına ayda ${(q * SCANS_PER_DAY * DAYS).toFixed(0)} sorgu (ücretsiz pay ${SEARCH_FREE_PER_MONTH}; aşımda 1000'i ${SEARCH_PRICE_PER_1000} $)\n`);
}
