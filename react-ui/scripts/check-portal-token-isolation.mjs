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
const portalServiceFiles = [
  path.join(root, 'src', 'services', 'portal', 'session.ts'),
  path.join(root, 'src', 'services', 'portal', 'session.js'),
];
const appFile = path.join(root, 'src', 'app.tsx');
const appJsFile = path.join(root, 'src', 'app.js');
const accessFile = path.join(root, 'src', 'access.ts');
const accessJsFile = path.join(root, 'src', 'access.js');
const requestErrorConfigFile = path.join(root, 'src', 'requestErrorConfig.ts');
const requestErrorConfigJsFile = path.join(root, 'src', 'requestErrorConfig.js');
const proxyConfigFile = path.join(root, 'config', 'proxy.ts');
const proxyConfigJsFile = path.join(root, 'config', 'proxy.js');
const routeConfigFiles = [
  path.join(root, 'config', 'routes.ts'),
  path.join(root, 'config', 'routes.js'),
];
const portalRequestFile = path.join(root, 'src', 'utils', 'portalRequest.ts');
const portalRequestJsFile = path.join(root, 'src', 'utils', 'portalRequest.js');
const portalPathsFiles = [
  path.join(root, 'src', 'utils', 'portalPaths.ts'),
  path.join(root, 'src', 'utils', 'portalPaths.js'),
];
const remoteMenuRouteGuardFiles = [
  path.join(root, 'src', 'wrappers', 'RemoteMenuRouteGuard.tsx'),
  path.join(root, 'src', 'wrappers', 'RemoteMenuRouteGuard.js'),
];
const adminSessionFile = path.join(root, 'src', 'services', 'session.ts');
const adminSessionJsFile = path.join(root, 'src', 'services', 'session.js');
const remoteMenuStorageFiles = [
  path.join(root, 'src', 'utils', 'remoteMenuStorage.ts'),
  path.join(root, 'src', 'utils', 'remoteMenuStorage.js'),
];
const portalTerminalFile = path.join(root, 'src', 'pages', 'Portal', 'terminal.ts');
const portalTerminalJsFile = path.join(root, 'src', 'pages', 'Portal', 'terminal.js');
const portalLoginPageFile = path.join(root, 'src', 'pages', 'Portal', 'Login', 'index.tsx');
const portalLoginPageJsFile = path.join(root, 'src', 'pages', 'Portal', 'Login', 'index.js');
const portalHomePageFile = path.join(root, 'src', 'pages', 'Portal', 'Home', 'index.tsx');
const portalHomePageJsFile = path.join(root, 'src', 'pages', 'Portal', 'Home', 'index.js');
const sellerPageFile = path.join(root, 'src', 'pages', 'Seller', 'index.tsx');
const buyerPageFile = path.join(root, 'src', 'pages', 'Buyer', 'index.tsx');
const partnerManagementPageFile = path.join(
  root,
  'src',
  'components',
  'PartnerManagement',
  'PartnerManagementPage.tsx',
);
const partnerAccountModalFile = path.join(
  root,
  'src',
  'components',
  'PartnerManagement',
  'PartnerAccountModal.tsx',
);
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

function assertFileExists(file) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    violations.push(`${relativePath} is missing`);
    return null;
  }
  return {
    relativePath,
    source: read(file),
  };
}

function assertIncludes(source, relativePath, expected, description) {
  if (!source.includes(expected)) {
    violations.push(`${relativePath} must ${description} with ${expected}`);
  }
}

function assertMatches(source, relativePath, pattern, description) {
  if (!pattern.test(source)) {
    violations.push(`${relativePath} must ${description}`);
  }
}

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

for (const portalServiceFile of portalServiceFiles) {
  const relativePath = path.relative(root, portalServiceFile).replaceAll(path.sep, '/');
  if (!fs.existsSync(portalServiceFile)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(portalServiceFile);
  const requestCount = [...source.matchAll(/\brequest(?:<[^;]+?>)?\s*\(/g)]
    .length;
  const isTokenFalseCount = [...source.matchAll(/\bisToken\s*:\s*false\b/g)]
    .length;
  if (requestCount !== isTokenFalseCount) {
    violations.push(
      `${relativePath} must set isToken:false on every portal request; request=${requestCount}, isToken:false=${isTokenFalseCount}`,
    );
  }
  if (!/\bfunction\s+sanitizePortalQueryParams\b/.test(source)) {
    violations.push(
      `${relativePath} must define sanitizePortalQueryParams for portal query params`,
    );
  }
  if (!/PORTAL_SCOPE_PARAM_KEYS[\s\S]*['"`]terminal['"`]/.test(source)) {
    violations.push(
      `${relativePath} must sanitize terminal from portal query params`,
    );
  }
  if (/\n\s*params\s*,/.test(source)) {
    violations.push(
      `${relativePath} must not pass raw params into portal requests`,
    );
  }
  for (const fnName of portalQueryFunctions) {
    const functionPattern = new RegExp(
      `function\\s+${fnName}[\\s\\S]*?params\\s*:\\s*sanitizePortalQueryParams\\(params\\)`,
    );
    if (!functionPattern.test(source)) {
      violations.push(
        `${relativePath} ${fnName} must sanitize portal query params before request`,
      );
    }
  }
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
    'PORTAL_DIRECT_LOGIN_RESULT_MESSAGE',
    'PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE',
    'postConsumeResult',
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

for (const portalPathsFile of portalPathsFiles) {
  const relativePath = path.relative(root, portalPathsFile).replaceAll(path.sep, '/');
  if (!fs.existsSync(portalPathsFile)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(portalPathsFile);
  for (const expected of [
    '/seller/login',
    '/buyer/login',
    'getPortalLoginPath',
    'isPortalRoute',
  ]) {
    if (!source.includes(expected)) {
      violations.push(`${relativePath} must define portal login routing with ${expected}`);
    }
  }
}

for (const remoteMenuRouteGuardFile of remoteMenuRouteGuardFiles) {
  const file = assertFileExists(remoteMenuRouteGuardFile);
  if (!file) {
    continue;
  }
  for (const expected of [
    'STATIC_ROUTE_AUTHORITIES',
    "'/seller': ['seller:admin:list']",
    "'/buyer': ['buyer:admin:list']",
    'PUBLIC_PORTAL_ROUTE_PATHS',
    '/seller/login',
    '/buyer/login',
    '/seller/direct-login',
    '/buyer/direct-login',
    '/seller/portal',
    '/buyer/portal',
    'return undefined;',
    'getStaticRouteAuthority(location.pathname)',
    'route?.authority ?? getStaticRouteAuthority(location.pathname) ?? []',
  ]) {
    assertIncludes(
      file.source,
      file.relativePath,
      expected,
      'keep terminal route fallback and public portal route exemption',
    );
  }
}

const adminSession = assertFileExists(adminSessionFile);
if (adminSession) {
  for (const expected of [
    'getRemoteMenuStorageKey(scope)',
    'window.sessionStorage.getItem(storageKey)',
    'window.sessionStorage.setItem(storageKey, JSON.stringify(data))',
    'window.sessionStorage.removeItem(storageKey)',
    'permissions.length > 0 && permissions.some',
    "status: '403'",
    'createGuardedMenuElement(pagePath, menuItem.authority)',
  ]) {
    assertIncludes(
      adminSession.source,
      adminSession.relativePath,
      expected,
      'keep remote menu scope storage and fail-closed route guard',
    );
  }
}

const adminSessionJs = assertFileExists(adminSessionJsFile);
if (adminSessionJs) {
  assertIncludes(
    adminSessionJs.source,
    adminSessionJs.relativePath,
    "export * from './session.ts';",
    'delegate to the guarded TS session implementation',
  );
}

for (const remoteMenuStorageFile of remoteMenuStorageFiles) {
  const file = assertFileExists(remoteMenuStorageFile);
  if (!file) {
    continue;
  }
  for (const expected of [
    'REMOTE_MENU_SCOPES',
    "['admin', 'seller', 'buyer']",
    'admin_remote_menu:${scope}',
  ]) {
    assertIncludes(
      file.source,
      file.relativePath,
      expected,
      'keep remote menu cache keys scoped by terminal',
    );
  }
}

const portalLoginPage = assertFileExists(portalLoginPageFile);
if (portalLoginPage) {
  for (const expected of [
    'getPortalTerminal(redirect) !== terminal',
    'redirect === PORTAL_META[terminal].loginPath',
    'PORTAL_META[terminal].homePath',
    'persistPortalLogin(response.data, terminal)',
    'history.replace(resolveRedirect(location.search, terminal))',
  ]) {
    assertIncludes(
      portalLoginPage.source,
      portalLoginPage.relativePath,
      expected,
      'keep portal login redirect and terminal match checks',
    );
  }
}

const portalLoginPageJs = assertFileExists(portalLoginPageJsFile);
if (portalLoginPageJs) {
  assertIncludes(
    portalLoginPageJs.source,
    portalLoginPageJs.relativePath,
    "export { default } from './index.tsx';",
    'delegate to the guarded TSX login page implementation',
  );
}

const portalHomePage = assertFileExists(portalHomePageFile);
if (portalHomePage) {
  for (const expected of [
    'getTerminalAccessToken(terminal)',
    'history.replace(PORTAL_META[terminal].loginPath)',
    'loadData(terminal);',
    'loadSessions(terminal);',
    'clearPortalLogin(currentTerminal);',
  ]) {
    assertIncludes(
      portalHomePage.source,
      portalHomePage.relativePath,
      expected,
      'keep portal home token gate and terminal-scoped recovery',
    );
  }
}

const portalHomePageJs = assertFileExists(portalHomePageJsFile);
if (portalHomePageJs) {
  assertIncludes(
    portalHomePageJs.source,
    portalHomePageJs.relativePath,
    "export { default } from './index.tsx';",
    'delegate to the guarded TSX home page implementation',
  );
}

const sellerPage = assertFileExists(sellerPageFile);
if (sellerPage) {
  for (const expected of [
    "moduleKey: 'seller'",
    'directLogin: createAdminSellerDirectLogin',
    'directLoginAccount: createAdminSellerAccountDirectLogin',
  ]) {
    assertIncludes(
      sellerPage.source,
      sellerPage.relativePath,
      expected,
      'keep seller direct-login terminal binding',
    );
  }
}

const buyerPage = assertFileExists(buyerPageFile);
if (buyerPage) {
  for (const expected of [
    "moduleKey: 'buyer'",
    'directLogin: createAdminBuyerDirectLogin',
    'directLoginAccount: createAdminBuyerAccountDirectLogin',
  ]) {
    assertIncludes(
      buyerPage.source,
      buyerPage.relativePath,
      expected,
      'keep buyer direct-login terminal binding',
    );
  }
}

for (const filePath of [partnerManagementPageFile, partnerAccountModalFile]) {
  const file = assertFileExists(filePath);
  if (!file) {
    continue;
  }
  assertIncludes(
    file.source,
    file.relativePath,
    'await openPortalDirectLoginWindow(resp.data, config.moduleKey)',
    'wait for portal direct-login consume ack before reporting success',
  );
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
    'PORTAL_DIRECT_LOGIN_RESULT_MESSAGE',
    'resolveTargetOrigin',
    'isResultMessage',
    'const targetOrigin = resolveTargetOrigin(result.loginUrl)',
    'event.origin !== targetOrigin',
    'popup.postMessage(payload, targetOrigin)',
    'DIRECT_LOGIN_CONSUME_TIMEOUT',
    'rejectBridge',
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
  if (!/redirectToPortalLogin\s*\(\s*portalTerminal\s*\)/.test(source)) {
    violations.push(
      `${relativePath} must redirect portal 401 through redirectToPortalLogin after clearing the matched portal token`,
    );
  }
  if (!/history\.replace\s*\(\s*`\$\{loginPath\}\?redirect=\$\{encodeURIComponent\(redirect\)\}`\s*\)/.test(source)) {
    violations.push(
      `${relativePath} must preserve current route in portal 401 redirect query`,
    );
  }
  if (source.includes('?token=123')) {
    violations.push(`${relativePath} must not append debug token query params`);
  }
}

for (const file of [appFile, appJsFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    continue;
  }
  const source = read(file);
  if (
    !/if\s*\(\s*isUnauthorizedCode\s*\(\s*getResponseCode\s*\(\s*response\?\.data\s*\)\s*\)\s*\)\s*\{[\s\S]*?handleUnauthorizedResponse\s*\(\s*response\?\.config\?\.url\s*\)\s*;[\s\S]*?return\s+Promise\.reject\s*\(\s*response\s*\)\s*;[\s\S]*?\}/.test(source)
  ) {
    violations.push(
      `${relativePath} must reject body-level 401 responses after portal/admin redirect handling`,
    );
  }
}

for (const proxyFile of [proxyConfigFile, proxyConfigJsFile]) {
  const relativePath = path.relative(root, proxyFile).replaceAll(path.sep, '/');
  if (!fs.existsSync(proxyFile)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(proxyFile);
  if (source.includes('http://localhost:8080')) {
    violations.push(`${relativePath} must not hardcode localhost:8080`);
  }
  if (!source.includes('API_PROXY_TARGET')) {
    violations.push(`${relativePath} must allow API_PROXY_TARGET override`);
  }
}

for (const accessTokenFile of [accessFile, accessJsFile]) {
  const relativePath = path.relative(root, accessTokenFile).replaceAll(path.sep, '/');
  if (!fs.existsSync(accessTokenFile)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(accessTokenFile);
  for (const expected of [
    'SESSION_TOKEN_KEYS',
    'seller_access_token',
    'buyer_access_token',
    'getTerminalSessionTokenKeys',
    'setTerminalSessionToken',
    'getTerminalAccessToken',
    'clearTerminalSessionToken',
    "setTerminalSessionToken('admin'",
    "clearTerminalSessionToken('admin'",
  ]) {
    if (!source.includes(expected)) {
      violations.push(`${relativePath} must keep terminal token isolation support for ${expected}`);
    }
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
