// Spike 10b puanlayıcısı: build/llm/rag-out.jsonl dosyasını spike/src/main/assets/rag-set.json
// beklentilerine göre puanlar. Kart bulma (ilk 1 / ilk 3): sözcük örtüşmesi tabanı, dört gömme
// yapılandırması ve ikisinin RRF birleşimi (blueprint F5.3). Yanıt doğruluğu anahtar sözcükle,
// talimat çeşidi başına.
// Kullanım: node scripts/rag-puanla.mjs [--yanitlar] [--dosya build/llm/rag-out.jsonl]
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const fileArg = process.argv.indexOf('--dosya');
const file = fileArg > -1 ? process.argv[fileArg + 1] : 'build/llm/rag-out.jsonl';
const set = JSON.parse(fs.readFileSync(path.join(root, 'spike/src/main/assets/rag-set.json'), 'utf8'));
const lines = fs.readFileSync(path.join(root, file), 'utf8').split(/\r?\n/).filter(Boolean).map((l) => JSON.parse(l));
const lower = (s) => s.toLocaleLowerCase('tr-TR');
const qById = new Map(set.questions.map((q) => [q.id, q]));
const answerable = set.questions.filter((q) => q.card);
const RRF_K = 60;

// Sözcük örtüşmesi tabanı: ortak sözcük (ilk 5 harf, kök yaklaşımı) sayısına göre sıralama.
const stems = (s) => new Set(lower(s).replace(/[^a-zçğıöşü0-9 ]/g, ' ').split(/\s+/).filter((w) => w.length > 2).map((w) => w.slice(0, 5)));
const cardStems = set.cards.map((c) => stems(c.text));
const lexRank = new Map();
for (const q of set.questions) {
  const qs = stems(q.text);
  const scored = set.cards.map((c, i) => [c.id, [...qs].filter((w) => cardStems[i].has(w)).length]).sort((a, b) => b[1] - a[1]);
  lexRank.set(q.id, scored);
}
const hits = (rankOf) => {
  let h1 = 0;
  let h3 = 0;
  const miss = [];
  for (const q of answerable) {
    const r = rankOf(q);
    if (r[0] === q.card) h1++;
    else miss.push(`${q.id}→${r[0]}`);
    if (r.slice(0, 3).includes(q.card)) h3++;
  }
  return { h1, h3, miss };
};
const show = (name, r) => console.log(`  ${name.padEnd(26)} ilk1 ${String(r.h1).padStart(2)}  ilk3 ${String(r.h3).padStart(2)}   kaçan: ${r.miss.join(' ') || '—'}`);

console.log(`kart bulma (${answerable.length} yanıtlanabilir soru, ${set.cards.length} kart)`);
show('sözcük örtüşmesi', hits((q) => lexRank.get(q.id).filter((x) => x[1] > 0).map((x) => x[0]).concat(['—', '—', '—'])));
const configs = [...new Set(lines.filter((l) => l.phase === 'retrieve').map((l) => l.config))];
for (const c of configs) {
  const byQ = new Map(lines.filter((x) => x.phase === 'retrieve' && x.config === c).map((l) => [l.q, l]));
  show(`gömme ${c}`, hits((q) => byQ.get(q.id).rank || byQ.get(q.id).top));
  if (byQ.values().next().value.rank) {
    show(`gömme ${c} + sözcük (RRF)`, hits((q) => {
      const emb = byQ.get(q.id).rank;
      const lex = lexRank.get(q.id);
      const score = new Map();
      emb.forEach((id, i) => score.set(id, (score.get(id) || 0) + 1 / (RRF_K + i + 1)));
      lex.forEach(([id, n], i) => {
        if (n > 0) score.set(id, (score.get(id) || 0) + 1 / (RRF_K + i + 1));
      });
      return [...score.entries()].sort((a, b) => b[1] - a[1]).map((x) => x[0]);
    }));
  }
}

const answers = lines.filter((l) => l.phase === 'answer');
for (const variant of [...new Set(answers.map((a) => a.variant || 'kati'))]) {
  const group = answers.filter((a) => (a.variant || 'kati') === variant);
  const cfg = group[0].config;
  const tops = new Map(lines.filter((l) => l.phase === 'retrieve' && l.config === cfg).map((l) => [l.q, l.top]));
  let right = 0;
  let refuseOk = 0;
  let falseRefuse = 0;
  let hallucinated = 0;
  const wrong = [];
  const times = [];
  for (const a of group) {
    const q = qById.get(a.q);
    const t = lower(a.text);
    const refused = t.includes('kartlarda bu bilgi yok');
    times.push(a.totalMs);
    if (!q.card) {
      if (refused) refuseOk++;
      else {
        hallucinated++;
        wrong.push(`${a.q}(yok denmeliydi)`);
      }
    } else if (q.keys.some((k) => t.includes(lower(k))) && !refused) {
      right++;
    } else {
      if (refused) falseRefuse++;
      wrong.push(`${a.q}(${refused ? 'yanlış ret' : 'anahtar yok'}${tops.get(a.q).includes(q.card) ? '' : ', kart gelmedi'})`);
    }
    if (process.argv.includes('--yanitlar')) console.log(`  [${variant}] ${a.q} ${a.totalMs}ms :: ${a.text.replace(/\s+/g, ' ').slice(0, 230)}`);
  }
  times.sort((x, y) => x - y);
  console.log(`\nyanıt — talimat "${variant}" (gömme ${cfg}, ilk 3 kart bağlamda)`);
  console.log(`  yanıtlanabilir: ${right}/${answerable.length} doğru · yanlış ret ${falseRefuse}`);
  console.log(`  kartlarda olmayan: ${refuseOk}/${set.questions.length - answerable.length} "yok" dedi · uydurma ${hallucinated}`);
  console.log(`  süre ortanca ${times[Math.floor(times.length / 2)]} ms · en uzun ${times[times.length - 1]} ms`);
  console.log(`  hatalı: ${wrong.join(' ') || '—'}`);
}
