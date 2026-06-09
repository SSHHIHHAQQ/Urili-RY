import fs from 'node:fs';
import path from 'node:path';

const uiRoot = process.cwd();
const mirrorScopes = [
  'src/components/InventoryAdjust',
  'src/components/InventorySyncPolicy',
  'src/components/ProductCenter',
  'src/pages/Finance',
  'src/pages/Inventory',
  'src/pages/Product',
  'src/pages/UpstreamSystem',
  'src/pages/Warehouse',
  'src/services/finance',
  'src/services/inventory',
  'src/services/product',
  'src/services/integration',
  'src/services/warehouse',
];

function walkFiles(dir, files = []) {
  if (!fs.existsSync(dir)) {
    return files;
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walkFiles(target, files);
    } else {
      files.push(target);
    }
  }
  return files;
}

function resolveMirrorSource(jsFile) {
  const base = jsFile.slice(0, -'.js'.length);
  for (const extension of ['.ts', '.tsx']) {
    const source = `${base}${extension}`;
    if (fs.existsSync(source)) {
      return source;
    }
  }
  return null;
}

function readText(file) {
  return fs.readFileSync(file, 'utf8');
}

function hasDefaultExport(sourceFile) {
  if (!sourceFile.endsWith('.tsx')) {
    return false;
  }
  const source = readText(sourceFile);
  return /\bexport\s+default\b/.test(source)
    || /\bexport\s*\{[^}]*\bdefault\b[^}]*\}\s*from\b/.test(source);
}

function namedExports(sourceFile) {
  const names = new Set();
  const source = readText(sourceFile);
  for (const regex of [
    /\bexport\s+(?:async\s+)?function\s+([A-Za-z_$][\w$]*)/g,
    /\bexport\s+(?:const|let|var|class|enum)\s+([A-Za-z_$][\w$]*)/g,
  ]) {
    for (const match of source.matchAll(regex)) {
      names.add(match[1]);
    }
  }
  return [...names].sort();
}

function expectedMirrorSource(sourceFile) {
  const sourceName = path.basename(sourceFile);
  if (hasDefaultExport(sourceFile)) {
    const exports = ['default', ...namedExports(sourceFile)].join(', ');
    return `export { ${exports} } from './${sourceName}';\n`;
  }
  return `export * from './${sourceName}';\n`;
}

const violations = [];

for (const scope of mirrorScopes) {
  const scopeDir = path.join(uiRoot, scope);
  for (const jsFile of walkFiles(scopeDir).filter((file) => file.endsWith('.js'))) {
    const sourceFile = resolveMirrorSource(jsFile);
    if (!sourceFile) {
      continue;
    }
    const actual = fs.readFileSync(jsFile, 'utf8').replace(/\r\n/g, '\n');
    const expected = expectedMirrorSource(sourceFile);
    if (actual !== expected) {
      violations.push(
        `${path.relative(uiRoot, jsFile).replaceAll(path.sep, '/')} must be a pure re-export of `
          + `${path.relative(uiRoot, sourceFile).replaceAll(path.sep, '/')}`,
      );
    }
  }
}

if (violations.length > 0) {
  throw new Error(`Product/Upstream JS mirrors must be pure TS/TSX re-exports:\n${violations.join('\n')}`);
}

console.log('product/upstream JS mirror guard passed.');
