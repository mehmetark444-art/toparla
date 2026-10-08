// Spike 9 puanlayıcısı: build/llm/stt-<kip>.jsonl dosyalarında beklenen ve duyulan cümleleri
// karşılaştırır; sözcük hata oranını (WER) hesaplar ve farklı satırları gösterir.
// Kullanım: node scripts/stt-puanla.mjs
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'build/llm');
const norm = (s) => s.toLocaleLowerCase('tr-TR').replace(/[’']/g, '').replace(/[^a-zçğıöşü0-9 ]/g, ' ').split(/\s+/).filter(Boolean);

/** Sözcük düzeyinde düzenleme uzaklığı. */
function distance(a, b) {
  const d = Array.from({ length: a.length + 1 }, (_, i) => [i, ...Array(b.length).fill(0)]);
  for (let j = 1; j <= b.length; j++) d[0][j] = j;
  for (let i = 1; i <= a.length; i++) {
    for (let j = 1; j <= b.length; j++) {
      d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
    }
  }
  return d[a.length][b.length];
}

for (const file of fs.readdirSync(dir).filter((f) => /^stt-.*\.jsonl$/.test(f)).sort()) {
  const rows = fs.readFileSync(path.join(dir, file), 'utf8').split(/\r?\n/).filter(Boolean).map((l) => JSON.parse(l));
  // Aynı cümle birden çok kez kaydedildiyse sonuncusu geçerlidir.
  const last = new Map(rows.map((r) => [r.i, r]));
  let errors = 0;
  let words = 0;
  let exact = 0;
  const after = [];
  const ready = [];
  console.log(`\n=== ${file} (${last.size} cümle) ===`);
  for (const r of [...last.values()].sort((a, b) => a.i - b.i)) {
    const e = norm(r.expected);
    const h = norm(r.heard);
    const dist = distance(e, h);
    errors += dist;
    words += e.length;
    if (dist === 0) exact++;
    if (r.afterSpeechMs > 0) after.push(r.afterSpeechMs);
    if (r.readyMs > 0) ready.push(r.readyMs);
    if (dist > 0 || r.error) console.log(`  ${String(r.i + 1).padStart(2)} [${dist}] beklenen: ${r.expected}\n         duyulan : ${r.heard || '(' + r.error + ')'}`);
  }
  const med = (a) => (a.length ? [...a].sort((x, y) => x - y)[Math.floor(a.length / 2)] : -1);
  console.log(`birebir aynı cümle: ${exact}/${last.size} · ham WER: %${((100 * errors) / words).toFixed(1)} (${errors}/${words} sözcük)`);
  console.log(`mikrofon hazır ortanca ${med(ready)} ms · konuşma bitişinden sonuca ortanca ${med(after)} ms`);
}
