import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const sourceRoots = [
  path.join(root, 'src', 'pages', 'Portal'),
  path.join(root, 'src', 'services', 'portal'),
];
const portalPagesRoot = path.join(root, 'src', 'pages', 'Portal');
const directLoginPageFile = path.join(
  portalPagesRoot,
  'DirectLogin',
  'index.tsx',
);
const directLoginPageJsFile = path.join(
  portalPagesRoot,
  'DirectLogin',
  'index.js',
);
const portalServiceFile = path.join(
  root,
  'src',
  'services',
  'portal',
  'session.ts',
);
const appFile = path.join(root, 'src', 'app.tsx');
const appJsFile = path.join(root, 'src', 'app.js');
const requestErrorConfigFile = path.join(root, 'src', 'requestErrorConfig.ts');
const requestErrorConfigJsFile = path.join(root, 'src', 'requestErrorConfig.js');
const proxyConfigFile = path.join(root, 'config', 'proxy.ts');
const routeConfigFiles = [
  path.join(root, 'config', 'routes.ts'),
  path.join(root, 'config', 'routes.js'),
];
const portalRequestFile = path.join(root, 'src', 'utils', 'portalRequest.ts');
const portalRequestJsFile = path.join(root, 'src', 'utils', 'portalRequest.js');
const portalPathsFile = path.join(root, 'src', 'utils', 'portalPaths.ts');
const portalTerminalFile = path.join(root, 'src', 'pages', 'Portal', 'terminal.ts');
const portalTerminalJsFile = path.join(root, 'src', 'pages', 'Portal', 'terminal.js');
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
    } else if (/\.(ts|tsx|js)$/.test(entry.name)) {
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

function extractInterfaceFieldNames(block) {
  return [...block.matchAll(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\??\s*:/gm)].map(
    (match) => match[1],
  );
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

for (const file of [directLoginPageFile, directLoginPageJsFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(file);
  for (const expected of [
    'PORTAL_DIRECT_LOGIN_READY_MESSAGE',
    'PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE',
    "window.addEventListener('message'",
    'window.opener?.postMessage',
    'event.source !== window.opener',
    'event.origin !== openerOrigin',
    'resolveOpenerOrigin',
  ]) {
    if (!source.includes(expected)) {
      violations.push(
        `${relativePath} must receive direct-login token by postMessage with ${expected}`,
      );
    }
  }
  if (!/window\.opener\?\.postMessage\([\s\S]*,\s*openerOrigin\)/.test(source)) {
    violations.push(
      `${relativePath} must send ready message to openerOrigin, not wildcard origin`,
    );
  }
  if (source.includes("postMessage({ type: PORTAL_DIRECT_LOGIN_READY_MESSAGE, terminal }, '*')")) {
    violations.push(
      `${relativePath} must not send direct-login ready message with wildcard origin`,
    );
  }
  for (const forbidden of [
    'new URLSearchParams',
    'location.search',
    'location.hash',
    'Back to admin login',
    "history.replace('/user/login')",
  ]) {
    if (source.includes(forbidden)) {
      violations.push(
        `${relativePath} must not read direct-login token from URL via ${forbidden}`,
      );
    }
  }
}

if (fs.existsSync(portalPathsFile)) {
  const source = read(portalPathsFile);
  for (const expected of [
    '/seller/login',
    '/buyer/login',
    'getPortalLoginPath',
    'isPortalRoute',
  ]) {
    if (!source.includes(expected)) {
      violations.push(`src/utils/portalPaths.ts must define portal login routing with ${expected}`);
    }
  }
} else {
  violations.push('src/utils/portalPaths.ts is missing');
}

for (const routeConfigFile of routeConfigFiles) {
  const relativePath = path.relative(root, routeConfigFile).replaceAll(path.sep, '/');
  if (!fs.existsSync(routeConfigFile)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(routeConfigFile);
  for (const expected of [
    "'/seller/login'",
    "'/buyer/login'",
    "'./Portal/Login'",
    "'/seller/direct-login'",
    "'/buyer/direct-login'",
    "'/seller/portal'",
    "'/buyer/portal'",
  ]) {
    if (!source.includes(expected)) {
      violations.push(`${relativePath} must keep portal route with ${expected}`);
    }
  }
}

const directLoginMessageFiles = [
  path.join(root, 'src', 'utils', 'portalDirectLoginMessage.ts'),
  path.join(root, 'src', 'utils', 'portalDirectLoginMessage.js'),
];
for (const directLoginMessageFile of directLoginMessageFiles) {
  const relativePath = path.relative(root, directLoginMessageFile).replaceAll(path.sep, '/');
  if (!fs.existsSync(directLoginMessageFile)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(directLoginMessageFile);
  for (const expected of [
    'resolveTargetOrigin',
    'const targetOrigin = resolveTargetOrigin(result.loginUrl)',
    'event.origin !== targetOrigin',
    'popup.postMessage(payload, targetOrigin)',
  ]) {
    if (!source.includes(expected)) {
      violations.push(
        `${relativePath} must constrain direct-login postMessage origin with ${expected}`,
      );
    }
  }
  for (const forbidden of [
    'setInterval(postToken',
    'setTimeout(postToken',
  ]) {
    if (source.includes(forbidden)) {
      violations.push(
        `${relativePath} must not post direct-login token before a verified READY message (${forbidden})`,
      );
    }
  }
}

for (const file of [portalRequestFile, portalRequestJsFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (fs.existsSync(file)) {
    const source = read(file);
    if (!/getPortalTerminalFromApiUrl/.test(source)) {
      violations.push(
        `${relativePath} must export getPortalTerminalFromApiUrl`,
      );
    }
    for (const adminPrefix of ['/api/seller/admin', '/api/buyer/admin']) {
      if (!source.includes(adminPrefix)) {
        violations.push(
          `${relativePath} must exclude management API prefix ${adminPrefix}`,
        );
      }
    }
  } else {
    violations.push(`${relativePath} is missing`);
  }
}

for (const file of [portalTerminalFile, portalTerminalJsFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(file);
  for (const expected of [
    'persistPortalLogin',
    'result.terminal !== expectedTerminal',
    'clearPortalLogin(expectedTerminal)',
    'clearPortalLogin(result.terminal)',
    'setTerminalSessionToken(terminal, result.token',
  ]) {
    if (!source.includes(expected)) {
      violations.push(`${relativePath} must keep terminal-matching login persistence with ${expected}`);
    }
  }
}

if (fs.existsSync(portalTypeFile)) {
  const source = read(portalTypeFile);
  const internalIdentityFields = [
    'terminal',
    'subjectId',
    'accountId',
    'sellerId',
    'buyerId',
    'sellerAccountId',
    'buyerAccountId',
  ];
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
    for (const fieldName of internalIdentityFields) {
      const fieldPattern = new RegExp(`\\b${fieldName}\\s*\\?\\s*:`);
      if (fieldPattern.test(block)) {
        violations.push(
          `${interfaceName} must not expose internal portal identity field ${fieldName}`,
        );
      }
    }
  }
  const loginResultBlock = extractInterfaceBlock(source, 'PortalLoginResultData');
  const directLoginResultBlock = extractInterfaceBlock(source, 'DirectLoginResult');
  if (!directLoginResultBlock) {
    violations.push(
      'src/types/seller-buyer/party.d.ts must define DirectLoginResult',
    );
  } else if (!/\btoken\s*:\s*string\b/.test(directLoginResultBlock)) {
    violations.push(
      'DirectLoginResult must expose token for postMessage delivery while loginUrl stays clean',
    );
  } else {
    const allowedDirectLoginFields = new Set([
      'token',
      'ticketId',
      'loginUrl',
      'expireMinutes',
      'expireTime',
    ]);
    for (const fieldName of extractInterfaceFieldNames(directLoginResultBlock)) {
      if (!allowedDirectLoginFields.has(fieldName)) {
        violations.push(
          `DirectLoginResult must keep a whitelist response contract; unexpected field ${fieldName}`,
        );
      }
    }
  }
  if (!loginResultBlock) {
    violations.push(
      'src/types/seller-buyer/party.d.ts must define PortalLoginResultData',
    );
  } else {
    for (const fieldName of internalIdentityFields.filter((fieldName) => fieldName !== 'terminal')) {
      const fieldPattern = new RegExp(`\\b${fieldName}\\s*\\?*\\s*:`);
      if (fieldPattern.test(loginResultBlock)) {
        violations.push(
          `PortalLoginResultData must not expose internal portal identity field ${fieldName}`,
        );
      }
    }
    const allowedLoginFields = new Set([
      'token',
      'terminal',
      'subjectNo',
      'username',
      'nickName',
      'expireMinutes',
      'expireTime',
    ]);
    for (const fieldName of extractInterfaceFieldNames(loginResultBlock)) {
      if (!allowedLoginFields.has(fieldName)) {
        violations.push(
          `PortalLoginResultData must keep a whitelist response contract; unexpected field ${fieldName}`,
        );
      }
    }
  }
} else {
  violations.push('src/types/seller-buyer/party.d.ts is missing');
}

for (const file of [appFile, appJsFile, requestErrorConfigFile, requestErrorConfigJsFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(file);
  for (const expected of [
    'getPortalTerminalFromApiUrl',
    'clearTerminalSessionToken',
    'getPortalLoginPath',
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
  if (!/history\.replace\s*\(\s*getPortalLoginPath\s*\(\s*portalTerminal\s*\)\s*\)/.test(source)) {
    violations.push(
      `${relativePath} must redirect portal 401 to the matched terminal login after clearing the matched portal token`,
    );
  }
  if (source.includes('?token=123')) {
    violations.push(`${relativePath} must not append debug token query params`);
  }
}

if (fs.existsSync(proxyConfigFile)) {
  const source = read(proxyConfigFile);
  if (source.includes('http://localhost:8080')) {
    violations.push('config/proxy.ts must not hardcode localhost:8080');
  }
  if (!source.includes('API_PROXY_TARGET')) {
    violations.push('config/proxy.ts must allow API_PROXY_TARGET override');
  }
} else {
  violations.push('config/proxy.ts is missing');
}

if (violations.length > 0) {
  console.error('Portal token isolation guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Portal token isolation guard passed.');
