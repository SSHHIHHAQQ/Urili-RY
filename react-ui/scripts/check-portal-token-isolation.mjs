import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const sourceRoots = [
  path.join(root, 'src', 'pages', 'Portal'),
  path.join(root, 'src', 'services', 'portal'),
];
const portalPagesRoot = path.join(root, 'src', 'pages', 'Portal');
const portalServiceFile = path.join(
  root,
  'src',
  'services',
  'portal',
  'session.ts',
);
const forbiddenPatterns = [
  /\bgetAccessToken\b/,
  /\bsetSessionToken\b/,
  /\bclearSessionToken\b/,
  /\bportal_login_token\b/,
  /\baccess_token\b/,
];
const forbiddenScopeObjectKeyPattern =
  /\b(?:sellerId|buyerId|subjectId|accountId|sellerAccountId|buyerAccountId)\s*:/;
const portalQueryFunctions = [
  'getPortalLoginLogs',
  'getPortalOperLogs',
  'getPortalSessions',
  'getSellerPortalDistributionProducts',
  'getBuyerPortalDistributionProducts',
];

function walk(dir) {
  if (!fs.existsSync(dir)) {
    return [];
  }
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...walk(fullPath));
    } else if (/\.(ts|tsx)$/.test(entry.name)) {
      files.push(fullPath);
    }
  }
  return files;
}

function read(file) {
  return fs.readFileSync(file, 'utf8');
}

const violations = [];

for (const file of sourceRoots.flatMap(walk)) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  const source = read(file);
  for (const pattern of forbiddenPatterns) {
    if (pattern.test(source)) {
      violations.push(`${relativePath} must not use ${pattern}`);
    }
  }
  if (forbiddenScopeObjectKeyPattern.test(source)) {
    violations.push(
      `${relativePath} must not send portal identity scope object keys`,
    );
  }
  if (file.startsWith(portalPagesRoot)) {
    if (/\brequest\s*(?:<[^;]+?>)?\s*\(/.test(source)) {
      violations.push(
        `${relativePath} must use portal session services instead of direct request(...) calls`,
      );
    }
    if (/['"`]\/api\/(?:seller|buyer)\b/.test(source)) {
      violations.push(
        `${relativePath} must not hardcode seller/buyer API paths`,
      );
    }
  }
}

if (fs.existsSync(portalServiceFile)) {
  const source = read(portalServiceFile);
  const requestCount = [...source.matchAll(/\brequest(?:<[^;]+?>)?\s*\(/g)]
    .length;
  const isTokenFalseCount = [...source.matchAll(/\bisToken\s*:\s*false\b/g)]
    .length;
  if (requestCount !== isTokenFalseCount) {
    violations.push(
      `src/services/portal/session.ts must set isToken:false on every portal request; request=${requestCount}, isToken:false=${isTokenFalseCount}`,
    );
  }
  if (!/\bfunction\s+sanitizePortalQueryParams\b/.test(source)) {
    violations.push(
      'src/services/portal/session.ts must define sanitizePortalQueryParams for portal query params',
    );
  }
  if (!/PORTAL_SCOPE_PARAM_KEYS[\s\S]*['"`]terminal['"`]/.test(source)) {
    violations.push(
      'src/services/portal/session.ts must sanitize terminal from portal query params',
    );
  }
  if (/\n\s*params\s*,/.test(source)) {
    violations.push(
      'src/services/portal/session.ts must not pass raw params into portal requests',
    );
  }
  for (const fnName of portalQueryFunctions) {
    const functionPattern = new RegExp(
      `function\\s+${fnName}[\\s\\S]*?params\\s*:\\s*sanitizePortalQueryParams\\(params\\)`,
    );
    if (!functionPattern.test(source)) {
      violations.push(
        `${fnName} must sanitize portal query params before request`,
      );
    }
  }
} else {
  violations.push('src/services/portal/session.ts is missing');
}

if (violations.length > 0) {
  console.error('Portal token isolation guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Portal token isolation guard passed.');
