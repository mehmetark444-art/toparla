// Depo tutarlılık denetimi: belgeler birbirini ve git geçmişini tutuyor mu?
// Çalıştırma: node .claude/skills/dogrula/kontrol.mjs [--faz F2]
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const read = (p) => (fs.existsSync(path.join(root, p)) ? fs.readFileSync(path.join(root, p), 'utf8') : '');
const git = (args) => {
  try {
    return execFileSync('git', args, { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
  } catch {
    return null;
  }
};

let failed = 0;
const ok = (m) => console.log(`GECTI  ${m}`);
const bad = (m) => {
  failed++;
  console.log(`KALDI  ${m}`);
};
const check = (cond, m, detail = '') => (cond ? ok(m) : bad(detail ? `${m} — ${detail}` : m));

const roadmap = read('docs/yol-haritasi.md');
const brain = read('docs/proje-beyni.md');
const claude = read('CLAUDE.md');

// 1) Gizli değer izlenen dosyalarda yok
const secretRe = /AQ\.[A-Za-z0-9_-]{30,}|AIza[0-9A-Za-z_-]{30,}|-----BEGIN [A-Z ]*PRIVATE KEY-----/;
const tracked = (git(['ls-files']) || '').split(/\r?\n/).filter(Boolean);
const leaks = tracked.filter((f) => {
  try {
    const p = path.join(root, f);
    return fs.statSync(p).size < 4 * 1024 * 1024 && secretRe.test(fs.readFileSync(p, 'utf8'));
  } catch {
    return false;
  }
});
check(leaks.length === 0, 'izlenen dosyalarda gizli değer yok', leaks.join(', '));
check(!tracked.some((f) => /(^|\/)(secrets|keystore)\.properties$|\.(jks|keystore)$/.test(f)), 'gizli dosyalar depoda izlenmiyor');

// 2) Belgelerde kanıt gösterilen commit'ler gerçekten var
for (const [name, text] of [['yol-haritasi', roadmap], ['proje-beyni', brain]]) {
  const hashes = [...new Set([...text.matchAll(/`([0-9a-f]{7,40})`/g)].map((m) => m[1]))];
  const missing = hashes.filter((h) => git(['cat-file', '-e', `${h}^{commit}`]) === null);
  check(missing.length === 0, `${name}: gösterilen ${hashes.length} commit depoda var`, missing.join(', '));
}

// 3) Yol haritası kapsamı: M1–M30
const missingM = Array.from({ length: 30 }, (_, i) => `M${i + 1}`).filter((m) => !new RegExp(`\\b${m}\\b`).test(roadmap));
check(missingM.length === 0, 'yol haritası M1–M30 hepsine değiniyor', missingM.join(', '));

// 4) Her fazda K1, K2, K3 var
const phases = [...roadmap.matchAll(/^## (F\d+) —/gm)].map((m) => m[1]);
for (const k of ['K1', 'K2', 'K3']) {
  const n = (roadmap.match(new RegExp(`^- [☑☐◐⛔] ${k}:`, 'gm')) || []).length;
  check(n === phases.length, `her fazda ${k} satırı var (${n}/${phases.length})`);
}

// 5) Faz kapısı: ☑ işaretli her fazın proje beyninde kapanış kaydı var; K1–K3'ü de ☑
const closed = [...roadmap.matchAll(/^\|\s*(F\d+)\s*\|[^\n]*\|\s*☑\s*\|\s*$/gm)].map((m) => m[1]);
const recorded = new Set([...(brain.split(/^## 13\./m)[1] || '').matchAll(/^\|\s*(F\d+)\s*\|/gm)].map((m) => m[1]));
check(closed.every((p) => recorded.has(p)), 'kapanan her fazın proje beyninde kapanış kaydı var', closed.filter((p) => !recorded.has(p)).join(', '));
for (const p of closed) {
  const body = (roadmap.split(new RegExp(`^## ${p} —`, 'm'))[1] || '').split(/^## F\d+ —/m)[0];
  const open = (body.match(/^\s*- [☐◐⛔] /gm) || []).length;
  check(open === 0, `${p} kapalı ve içinde açık madde yok`, `${open} açık madde`);
}

// 6) Proje beyni güncel mi: "Kapsadığı son commit" HEAD'in atası ve arada kod değişikliği yok
const covered = (brain.match(/Kapsadığı son commit:\*\*\s*`([0-9a-f]{7,40})`/) || [])[1];
if (!covered) {
  bad('proje beyni: "Kapsadığı son commit" okunamadı');
} else {
  const changed = (git(['diff', '--name-only', `${covered}..HEAD`]) || '').split(/\r?\n/).filter(Boolean);
  const code = changed.filter((f) => /\.(kt|kts|xml|toml)$/.test(f) && !f.startsWith('spike/'));
  if (code.length) console.log(`UYARI  proje beyni ${covered} commit'ini kapsıyor; sonrasında ${code.length} kod dosyası değişti (faz kapanışında güncellenmeli)`);
  else ok('proje beyni son kod değişikliklerini kapsıyor');
}

// 7) Karar kayıtları CLAUDE.md özetinde geçiyor
const decisions = fs.existsSync(path.join(root, 'docs/decisions')) ? fs.readdirSync(path.join(root, 'docs/decisions')).filter((f) => /^\d{4}-/.test(f)) : [];
const notListed = decisions.map((f) => f.slice(0, 4)).filter((n) => !claude.includes(n));
check(notListed.length === 0, `${decisions.length} karar kaydının hepsi CLAUDE.md özetinde`, notListed.join(', '));

// 8) İsteğe bağlı: belirli bir fazın kapanmaya hazır olup olmadığı
const fazArg = process.argv.indexOf('--faz');
if (fazArg > -1) {
  const p = process.argv[fazArg + 1];
  const body = (roadmap.split(new RegExp(`^## ${p} —`, 'm'))[1] || '').split(/^## F\d+ —/m)[0];
  if (!body) bad(`${p} yol haritasında bulunamadı`);
  else {
    const openItems = (body.match(/^\s*- [☐◐⛔] F\d+\.\d+[^\n]*/gm) || []);
    check(openItems.length === 0, `${p}: tüm maddeler ☑`, `${openItems.length} açık: ${openItems.slice(0, 5).map((s) => s.trim().split(' ')[2]).join(', ')}`);
    for (const k of ['K1', 'K2', 'K3']) check(new RegExp(`^- ☑ ${k}:`, 'm').test(body), `${p}: ${k} işaretli`);
    check(recorded.has(p), `${p}: proje beyni Bölüm 13'te kapanış kaydı var`);
  }
}

// 9) Gece kontrolü: temizlik ve bayatlık (yalnız --gece ile)
if (process.argv.includes('--gece')) {
  const warn = (m) => console.log(`UYARI  ${m}`);
  const status = (git(['status', '--porcelain']) || '').split(/\r?\n/).filter(Boolean);
  check(status.length === 0, 'çalışma ağacı temiz (commit bekleyen değişiklik yok)', status.slice(0, 6).join(' · '));

  const leftovers = tracked.filter((f) => /(Gecici|Deneme|sizinti|Scratch)[^/]*$|\.(tmp|bak|orig|rej)$|~$/i.test(f));
  check(leftovers.length === 0, 'geçici ya da artık dosya yok', leftovers.join(', '));

  const sources = tracked.filter((f) => /\.(kt|kts|mjs|sh|xml|toml)$/.test(f) && f !== '.claude/skills/dogrula/kontrol.mjs');
  const todos = [];
  for (const f of sources) {
    read(f).split(/\r?\n/).forEach((line, i) => {
      if (/\b(TODO|FIXME|XXX|HACK)\b/.test(line)) todos.push(`${f}:${i + 1}`);
    });
  }
  check(todos.length === 0, 'kodda sahipsiz TODO / FIXME yok', todos.slice(0, 8).join(', '));

  const claudeLines = claude.split(/\r?\n/).length;
  check(claudeLines < 200, `CLAUDE.md kısa (${claudeLines} satır < 200)`);

  const noFront = tracked.filter((f) => /^\.claude\/(skills\/[^/]+\/SKILL|agents\/[^/]+|rules\/[^/]+)\.md$/.test(f) && !read(f).startsWith('---'));
  check(noFront.length === 0, 'yetenek, alt ajan ve kural dosyalarının ön maddesi var', noFront.join(', '));

  // Yol haritasının tarihi, son kod/belge commit'inden eski olmamalı.
  const months = { Ocak: 0, Şubat: 1, Mart: 2, Nisan: 3, Mayıs: 4, Haziran: 5, Temmuz: 6, Ağustos: 7, Eylül: 8, Ekim: 9, Kasım: 10, Aralık: 11 };
  const parseTr = (text) => {
    const m = /\*\*Son güncelleme:\*\*\s*(\d{1,2}) (\S+) (\d{4})/.exec(text);
    return m && months[m[2]] !== undefined ? new Date(Number(m[3]), months[m[2]], Number(m[1])) : null;
  };
  const lastCommit = new Date((git(['log', '-1', '--format=%cI']) || '').trim());
  const lastDay = new Date(lastCommit.getFullYear(), lastCommit.getMonth(), lastCommit.getDate());
  for (const [name, text] of [['yol haritası', roadmap], ['proje beyni', brain]]) {
    const d = parseTr(text);
    check(d !== null && d >= lastDay, `${name} "Son güncelleme" tarihi son commit gününü kapsıyor`, d ? d.toLocaleDateString('tr-TR') : 'tarih okunamadı');
  }

  // Bilgi: açık engeller ve teyit bekleyenler (hata değil, hatırlatma)
  const blockers = (roadmap.match(/^- ⛔ .*$/gm) || []).filter((l) => !/ F\d+\.\d+/.test(l));
  blockers.forEach((b) => warn(`açık engel: ${b.replace(/^- ⛔ /, '')}`));
  for (const f of decisions) {
    const head = read(`docs/decisions/${f}`).split(/\r?\n/).slice(0, 4).join(' ');
    if (/teyit ettirilecek|Aday/.test(head)) warn(`karar ${f.slice(0, 4)} hâlâ teyit ya da ölçüm bekliyor`);
  }
}

console.log(failed ? `SONUC: ${failed} sorun` : 'SONUC: tutarlı');
process.exit(failed ? 1 : 0);
