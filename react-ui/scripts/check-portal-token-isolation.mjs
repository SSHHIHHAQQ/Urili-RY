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
const appFile = path.join(root, 'src', 'app.tsx');
const requestErrorConfigFile = path.join(root, 'src', 'requestErrorConfig.ts');
const portalRequestFile = path.join(root, 'src', 'utils', 'portalRequest.ts');
const portalTypeFile = path.join(
  root,
  'src',
  'types',
  'seller-buyer',
  'party.d.ts',
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

function extractInterfaceBlock(source, interfaceName) {
  const start = source.indexOf(`interface ${interfaceName}`);
  if (start < 0) {
    return '';
  }
  const bodyStart = source.indexOf('{', start);
  if (bodyStart < 0) {
    return '';
  }
  let depth = 0;
  for (let index = bodyStart; index < source.length; index += 1) {
    const char = source[index];
    if (char === '{') {
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0) {
        return source.slice(start, index + 1);
      }
    }
  }
  return '';
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

if (fs.existsSync(portalRequestFile)) {
  const source = read(portalRequestFile);
  if (!/getPortalTerminalFromApiUrl/.test(source)) {
    violations.push(
      'src/utils/portalRequest.ts must export getPortalTerminalFromApiUrl',
    );
  }
  for (const adminPrefix of ['/api/seller/admin', '/api/buyer/admin']) {
    if (!source.includes(adminPrefix)) {
      violations.push(
        `src/utils/portalRequest.ts must exclude management API prefix ${adminPrefix}`,
      );
    }
  }
} else {
  violations.push('src/utils/portalRequest.ts is missing');
}

if (fs.existsSync(portalTypeFile)) {
  const source = read(portalTypeFile);
  const internalHomeResponseTypes = [
    'PortalPermissionInfo',
    'PortalSubjectProfile',
    'PortalAccountProfile',
    'PortalDeptProfile',
    'PortalRoleProfile',
    'PortalOwnSessionProfile',
  ];
  for (const interfaceName of internalHomeResponseTypes) {
    const block = extractInterfaceBlock(source, interfaceName);
    if (!block) {
      violations.push(
        `src/types/seller-buyer/party.d.ts must define ${interfaceName}`,
      );
      continue;
    }
    for (const fieldName of ['terminal', 'subjectId', 'accountId']) {
      const fieldPattern = new RegExp(`\\b${fieldName}\\s*\\?\\s*:`);
      if (fieldPattern.test(block)) {
        violations.push(
          `${interfaceName} must not expose internal portal identity field ${fieldName}`,
        );
      }
    }
  }
  const loginResultBlock = extractInterfaceBlock(source, 'PortalLoginResultData');
  if (!loginResultBlock) {
    violations.push(
      'src/types/seller-buyer/party.d.ts must define PortalLoginResultData',
    );
  } else {
    for (const fieldName of ['subjectId', 'accountId']) {
      const fieldPattern = new RegExp(`\\b${fieldName}\\s*\\?*\\s*:`);
      if (fieldPattern.test(loginResultBlock)) {
        violations.push(
          `PortalLoginResultData must not expose internal portal identity field ${fieldName}`,
        );
      }
    }
  }
} else {
  violations.push('src/types/seller-buyer/party.d.ts is missing');
}

for (const file of [appFile, requestErrorConfigFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(file);
  for (const expected of [
    'getPortalTerminalFromApiUrl',
    'clearTerminalSessionToken',
  ]) {
    if (!source.includes(expected)) {
      violations.push(
        `${relativePath} must handle portal 401 with ${expected}`,
      );
    }
  }
  if (!/clearTerminalSessionToken\s*\(\s*portalTerminal\s*\)/.test(source)) {
    violations.push(
      `${relativePath} must clear only the matched portal terminal token on portal 401`,
    );
  }
}

if (violations.length > 0) {
  console.error('Portal token isolation guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Portal token isolation guard passed.');
