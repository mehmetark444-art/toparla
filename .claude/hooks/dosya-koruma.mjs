// PreToolUse (Edit | Write | MultiEdit | NotebookEdit)
// 1) Değiştirilmez dosyaları korur (blueprint, arşiv).
// 2) Gizli değerin depoya girecek bir dosyaya yazılmasını engeller.
// 3) Faz kapısı (K3): proje beyninde kapanış kaydı olmayan faz, yol haritasında ☑ yapılamaz.
import fs from 'node:fs';
import path from 'node:path';
import {
  readInput, projectRoot, relativeTo, findSecret, SECRET_FILES, denyPreToolUse,
} from './ortak.mjs';

const input = readInput();
const root = projectRoot(input);
const ti = input.tool_input || {};
const rel = relativeTo(root, ti.file_path || ti.notebook_path || '');
if (rel === null || rel === '') process.exit(0);

// 1) Değiştirilmez dosyalar
if (/^docs\/BLUEPRINT\.md$/i.test(rel) || /^docs\/arsiv\//i.test(rel)) {
  denyPreToolUse(
    `${rel} değiştirilmez (CLAUDE.md). Blueprint'ten sapma docs/decisions/ altına karar kaydı olarak yazılır.`,
  );
}

// Yazılacak yeni metin parçaları
const pieces = [];
if (typeof ti.content === 'string') pieces.push(ti.content);
if (typeof ti.new_string === 'string') pieces.push(ti.new_string);
if (typeof ti.new_source === 'string') pieces.push(ti.new_source);
if (Array.isArray(ti.edits)) for (const e of ti.edits) if (typeof e?.new_string === 'string') pieces.push(e.new_string);

// 2) Gizli değer
if (!SECRET_FILES.test(rel)) {
  const hit = findSecret(pieces.join('\n'));
  if (hit) {
    denyPreToolUse(
      `Yazılmak istenen içerikte ${hit} var. Gizli değerler yalnız gitignore'daki secrets.properties / keystore.properties dosyasında durur; depoya ve belgelere girmez.`,
    );
  }
}

// 3) Faz kapısı
if (/^docs\/yol-haritasi\.md$/i.test(rel)) {
  const file = path.join(root, rel);
  let result = '';
  if (typeof ti.content === 'string') {
    result = ti.content;
  } else {
    result = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';
    const edits = Array.isArray(ti.edits) ? ti.edits : [ti];
    for (const e of edits) {
      if (typeof e?.old_string !== 'string' || typeof e?.new_string !== 'string') continue;
      result = e.replace_all ? result.split(e.old_string).join(e.new_string) : result.replace(e.old_string, () => e.new_string);
    }
  }
  const closedPhases = [...result.matchAll(/^\|\s*(F\d+)\s*\|[^\n]*\|\s*☑\s*\|\s*$/gm)].map((m) => m[1]);
  const brainFile = path.join(root, 'docs/proje-beyni.md');
  const brain = fs.existsSync(brainFile) ? fs.readFileSync(brainFile, 'utf8') : '';
  const section = brain.split(/^## 13\./m)[1] || '';
  const recorded = new Set([...section.matchAll(/^\|\s*(F\d+)\s*\|/gm)].map((m) => m[1]));
  const missing = closedPhases.filter((p) => !recorded.has(p));
  if (missing.length) {
    denyPreToolUse(
      `Faz kapısı (K3): ${missing.join(', ')} yol haritasında ☑ yapılmak isteniyor ama docs/proje-beyni.md Bölüm 13'te kapanış kaydı yok. Önce proje beynini güncelle (12. bölümdeki kontrol listesi), sonra fazı işaretle.`,
    );
  }
}

process.exit(0);
