// PostToolUse (Edit | Write | MultiEdit)
// Kotlin dosyası değişince proje kod kurallarını denetler. İhlal varsa çıkış kodu 2 ile
// Claude'a bildirilir (dosya zaten yazılmıştır; düzeltmesi istenir).
import fs from 'node:fs';
import path from 'node:path';
import { readInput, projectRoot, relativeTo } from './ortak.mjs';

const input = readInput();
const root = projectRoot(input);
const rel = relativeTo(root, input.tool_input?.file_path || '');
if (!rel || !rel.endsWith('.kt')) process.exit(0);
// :spike atılacak deneme kodudur; kurallardan bilerek muaftır.
if (rel.startsWith('spike/')) process.exit(0);

const file = path.join(root, rel);
if (!fs.existsSync(file)) process.exit(0);

// Yorumları ve metin sabitlerini at; kural yalnız koda bakar.
const code = fs.readFileSync(file, 'utf8')
  .replace(/\/\*[\s\S]*?\*\//g, '')
  .replace(/"""[\s\S]*?"""/g, '""')
  .replace(/"(?:\\.|[^"\\\n])*"/g, '""')
  .replace(/`[^`\n]*`/g, 'x')
  .replace(/\/\/[^\n]*/g, '');

const isTest = /\/src\/(test|androidTest)\//.test(rel);
const isDomain = rel.startsWith('domain/');
const problems = [];
const check = (re, message) => {
  const m = re.exec(code);
  if (m) problems.push(`${message} (bulunan: ${m[0].trim().slice(0, 40)})`);
};

check(/[\w)\]]\s*!!/, '`!!` yasak; null durumunu açıkça ele al');
check(/\bGlobalScope\b/, '`GlobalScope` yasak; sahibi olan bir CoroutineScope kullan');
if (!isTest) check(/\brunBlocking\b/, '`runBlocking` yalnız testte');
if (isDomain) check(/^\s*import\s+(android|androidx)\./m, ':domain saf Kotlin/JVM; Android sınıfı içeremez');

// Zaman ve rastgelelik: yalnız enjekte Clock / RandomSource. Üretim uygulaması dosyaları muaf.
const isProvider = /\b(class|object)\s+\w*(SystemClock|SystemRandom|UuidIdGenerator)\b/.test(code);
if (!isTest && !isProvider) {
  check(/\bSystem\.currentTimeMillis\s*\(/, '`System.currentTimeMillis()` yasak; enjekte `Clock` kullan');
  check(/\b(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime)\.now\s*\(/, '`now()` yasak; zamanı parametre ya da `Clock` ile al');
  check(/\bRandom\s*\(\s*\)/, 'tohumsuz `Random()` yasak; `RandomSource` kullan');
}

if (problems.length) {
  console.error(`Proje kod kuralı ihlali: ${rel}\n- ${problems.join('\n- ')}\nDüzelt (CLAUDE.md ve .claude/rules/kod.md).`);
  process.exit(2);
}
process.exit(0);
