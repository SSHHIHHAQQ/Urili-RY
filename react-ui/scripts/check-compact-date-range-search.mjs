import fs from 'node:fs';
import path from 'node:path';

const uiRoot = process.cwd();
const sourceRoot = path.join(uiRoot, 'src');
const ignoredDirs = new Set([
  '.umi',
  '.umi-production',
  '.umi-test',
  '.umi-test-production',
  'services',
  'node_modules',
]);
const sourceExtensions = new Set(['.js', '.jsx', '.ts', '.tsx']);
const compactDateRangeViolationPattern =
  /(?:colSize\s*:\s*2[\s\S]{0,300}valueType\s*:\s*['"]dateRange['"]|valueType\s*:\s*['"]dateRange['"][\s\S]{0,300}colSize\s*:\s*2)/g;

function walkFiles(dir, files = []) {
  if (!fs.existsSync(dir)) {
    return files;
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!ignoredDirs.has(entry.name)) {
        walkFiles(target, files);
      }
      continue;
    }
    if (sourceExtensions.has(path.extname(entry.name))) {
      files.push(target);
    }
  }
  return files;
}

function lineNumberAt(source, index) {
  return source.slice(0, index).split(/\r?\n/).length;
}

const violations = [];

for (const file of walkFiles(sourceRoot)) {
  const source = fs.readFileSync(file, 'utf8');
  for (const match of source.matchAll(compactDateRangeViolationPattern)) {
    violations.push({
      file: path.relative(uiRoot, file),
      line: lineNumberAt(source, match.index ?? 0),
    });
  }
}

if (violations.length > 0) {
  const details = violations
    .map((item) => `- ${item.file}:${item.line}`)
    .join('\n');
  throw new Error(
    `dateRange search fields must stay compact; remove colSize: 2 from these Ant Pro dateRange columns:\n${details}`,
  );
}

console.log('Compact dateRange search check passed.');
