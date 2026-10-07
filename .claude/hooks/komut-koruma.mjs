// PreToolUse (Bash | PowerShell)
// 1) git push: her seferinde Kullanıcı'ya sorulur (CLAUDE.md: push yalnız Kullanıcı isteyince).
// 2) git commit / git add: commit'e girecek içerikte gizli değer ya da gizli dosya varsa engellenir.
// 3) Gizli dosyanın içeriğini ekrana basan komutlar engellenir.
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import {
  readInput, projectRoot, findSecret, SECRET_FILES, denyPreToolUse, askPreToolUse,
} from './ortak.mjs';

const input = readInput();
const root = projectRoot(input);
const cmd = String(input.tool_input?.command || '');
if (!cmd) process.exit(0);

function git(args) {
  try {
    return execFileSync('git', args, { cwd: root, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024, stdio: ['ignore', 'pipe', 'ignore'] });
  } catch {
    return '';
  }
}

// 3) Gizli dosyayı ekrana basma
if (/\b(cat|type|head|tail|less|more|bat|Get-Content|gc)\b[^|;&\n]*\b(secrets|keystore)\.properties\b/i.test(cmd)) {
  denyPreToolUse(
    'Gizli dosyanın içeriği ekrana basılmaz. Değeri değişkene al ve yazdırmadan kullan: K=$(grep "^GEMINI_API_KEY=" secrets.properties | cut -d= -f2-)',
  );
}

// 2) Commit'e girecek içerik
if (/\bgit\b[^\n]*\b(commit|add)\b/.test(cmd)) {
  const names = (git(['diff', '--cached', '--name-only']) + '\n' + git(['ls-files', '--others', '--exclude-standard']) + '\n' + git(['diff', '--name-only']))
    .split(/\r?\n/).filter(Boolean);
  const badFile = names.find((n) => SECRET_FILES.test(n) && !/local\.properties$/.test(n));
  if (badFile) {
    denyPreToolUse(`${badFile} depoya girmemeli (gizli dosya). .gitignore'u kontrol et; dosyayı commit'e ekleme.`);
  }
  // Eklenen satırlar: sahnelenmiş + sahnelenmemiş + izlenmeyen dosyalar
  let added = (git(['diff', '--cached', '-U0']) + '\n' + git(['diff', '-U0']))
    .split(/\r?\n/).filter((l) => l.startsWith('+') && !l.startsWith('+++')).join('\n');
  for (const n of git(['ls-files', '--others', '--exclude-standard']).split(/\r?\n/).filter(Boolean)) {
    try {
      const f = path.join(root, n);
      if (fs.statSync(f).size < 2 * 1024 * 1024) added += '\n' + fs.readFileSync(f, 'utf8');
    } catch {
      // okunamayan dosya (ikili, kilitli) taranmaz
    }
  }
  const hit = findSecret(added);
  if (hit) {
    denyPreToolUse(`Commit'e girecek değişikliklerde ${hit} bulundu. Değeri dosyadan çıkar; gizli değerler yalnız secrets.properties'te durur.`);
  }
}

// 1) Push
if (/\bgit\b[^\n]*\bpush\b/.test(cmd)) {
  askPreToolUse('git push dışarıya yayındır. Kural: push yalnız Kullanıcı açıkça isteyince; ilk push öncesi API anahtarı yenilenmiş olmalı (yol haritası F2.18).');
}

process.exit(0);
