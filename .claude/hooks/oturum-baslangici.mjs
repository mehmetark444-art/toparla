// SessionStart (startup | resume | clear | compact)
// Oturum başında ve bağlam sıkıştırmasından sonra projenin güncel durumunu bağlama ekler.
// Düz stdout Claude'un bağlamına girer.
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { readInput, projectRoot } from './ortak.mjs';

const input = readInput();
const root = projectRoot(input);
const read = (p) => {
  try {
    return fs.readFileSync(path.join(root, p), 'utf8');
  } catch {
    return '';
  }
};

const roadmap = read('docs/yol-haritasi.md');
// "Genel durum" başlığından ilk yatay çizgiye kadar: faz tablosu, şu an, sıradaki adım, engeller.
const status = (roadmap.split(/^## Genel durum\s*$/m)[1] || '').split(/^---\s*$/m)[0].trim();
const brainHeader = (read('docs/proje-beyni.md').match(/^\*\*Son güncelleme:\*\*.*$/m) || [''])[0];

let log = '';
try {
  log = execFileSync('git', ['log', '--oneline', '-6'], { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
} catch {
  log = '(git günlüğü okunamadı)';
}
let dirty = '';
try {
  dirty = execFileSync('git', ['status', '--short'], { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
} catch {
  dirty = '';
}

const source = input.source || 'startup';
const lines = [
  `# Toparla · Güneş — oturum durumu (${source})`,
  '',
  source === 'compact'
    ? 'Bağlam sıkıştırıldı. Ayrıntı gerekiyorsa docs/proje-beyni.md ve docs/yol-haritasi.md dosyalarını yeniden oku.'
    : 'Başlamadan önce: docs/proje-beyni.md (hafıza) → docs/yol-haritasi.md (durum) → ilgili docs/decisions/ kayıtları.',
  '',
  '## Yol haritası: genel durum',
  status || '(docs/yol-haritasi.md okunamadı)',
  '',
  `## Proje beyni\n${brainHeader || '(başlık okunamadı)'}`,
  '',
  `## Son commit'ler\n${log}`,
  '',
  dirty ? `## Commit'lenmemiş değişiklikler\n${dirty}` : "## Çalışma ağacı temiz",
  '',
  'Hatırlatma: Kullanıcı teknik bilgi sahibi değil; elle adımlar numaralı ve tek eylemli. Her şey Türkçe. Push yalnız Kullanıcı isteyince.',
];
console.log(lines.join('\n').slice(0, 9000));
process.exit(0);
