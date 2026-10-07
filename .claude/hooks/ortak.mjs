// Kancaların ortak yardımcıları. Bağımlılık yok; yalnız Node.
import fs from 'node:fs';
import path from 'node:path';

/** Kanca girdisini (stdin JSON) okur. Bozuk girdi kancayı düşürmesin diye boş nesne döner. */
export function readInput() {
  try {
    return JSON.parse(fs.readFileSync(0, 'utf8') || '{}');
  } catch {
    return {};
  }
}

/** Git Bash biçimindeki yolu (/c/Users/…) Windows biçimine (C:/Users/…) çevirir. */
export function normalize(p) {
  if (!p) return '';
  const m = /^\/([a-zA-Z])\/(.*)$/.exec(p);
  return (m ? `${m[1].toUpperCase()}:/${m[2]}` : p).replace(/\\/g, '/');
}

export function projectRoot(input) {
  return normalize(process.env.CLAUDE_PROJECT_DIR || input.cwd || process.cwd());
}

/** Proje köküne göre göreli yol (ileri eğik çizgili); kök dışındaysa null. */
export function relativeTo(root, filePath) {
  const abs = normalize(path.resolve(root, normalize(filePath)));
  const r = root.replace(/\/$/, '');
  if (abs.toLowerCase() === r.toLowerCase()) return '';
  if (!abs.toLowerCase().startsWith(r.toLowerCase() + '/')) return null;
  return abs.slice(r.length + 1);
}

/** Depoya ve belgelere asla girmemesi gereken gizli değer kalıpları. */
export const SECRET_PATTERNS = [
  { name: 'Google API anahtarı (AQ.)', re: /AQ\.[A-Za-z0-9_-]{30,}/ },
  { name: 'Google API anahtarı (AIza)', re: /AIza[0-9A-Za-z_-]{30,}/ },
  { name: 'özel anahtar bloğu', re: /-----BEGIN [A-Z ]*PRIVATE KEY-----/ },
  { name: 'keystore parolası', re: /^(storePassword|keyPassword)\s*=\s*\S+/m },
];

export function findSecret(text) {
  if (!text) return null;
  return SECRET_PATTERNS.find((p) => p.re.test(text))?.name ?? null;
}

/** Gizli değer taşımasına izin verilen (gitignore'daki) dosyalar. */
export const SECRET_FILES = /(^|\/)(secrets\.properties|keystore\.properties|local\.properties)$|\.(jks|keystore)$/i;

export function denyPreToolUse(reason) {
  console.log(
    JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PreToolUse',
        permissionDecision: 'deny',
        permissionDecisionReason: reason,
      },
    }),
  );
  process.exit(0);
}

export function askPreToolUse(reason) {
  console.log(
    JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PreToolUse',
        permissionDecision: 'ask',
        permissionDecisionReason: reason,
      },
    }),
  );
  process.exit(0);
}
