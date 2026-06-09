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
];
const portalServiceJsFile = path.join(root, 'src', 'services', 'portal', 'session.js');
const appFile = path.join(root, 'src', 'app.tsx');
const appJsFile = path.join(root, 'src', 'app.js');
const accessFile = path.join(root, 'src', 'access.ts');
const accessJsFile = path.join(root, 'src', 'access.js');
const requestErrorConfigFile = path.join(root, 'src', 'requestErrorConfig.ts');
const requestErrorConfigJsFile = path.join(root, 'src', 'requestErrorConfig.js');
const proxyConfigFile = path.join(root, 'config', 'proxy.ts');
const proxyConfigJsFile = path.join(root, 'config', 'proxy.js');
const routeConfigFile = path.join(root, 'config', 'routes.ts');
const routeConfigJsFile = path.join(root, 'config', 'routes.js');
const portalRequestFile = path.join(root, 'src', 'utils', 'portalRequest.ts');
const portalRequestJsFile = path.join(root, 'src', 'utils', 'portalRequest.js');
const portalPathsFiles = [
  path.join(root, 'src', 'utils', 'portalPaths.ts'),
];
const portalPathsJsFile = path.join(root, 'src', 'utils', 'portalPaths.js');
const remoteMenuRouteGuardFiles = [
  path.join(root, 'src', 'wrappers', 'RemoteMenuRouteGuard.tsx'),
];
const remoteMenuRouteGuardJsFile = path.join(root, 'src', 'wrappers', 'RemoteMenuRouteGuard.js');
const adminSessionFile = path.join(root, 'src', 'services', 'session.ts');
const adminSessionJsFile = path.join(root, 'src', 'services', 'session.js');
const remoteMenuStorageFiles = [
  path.join(root, 'src', 'utils', 'remoteMenuStorage.ts'),
];
const remoteMenuStorageJsFile = path.join(root, 'src', 'utils', 'remoteMenuStorage.js');
const portalTerminalFile = path.join(root, 'src', 'pages', 'Portal', 'terminal.ts');
const portalTerminalJsFile = path.join(root, 'src', 'pages', 'Portal', 'terminal.js');
const portalLoginPageFile = path.join(root, 'src', 'pages', 'Portal', 'Login', 'index.tsx');
const portalLoginPageJsFile = path.join(root, 'src', 'pages', 'Portal', 'Login', 'index.js');
const portalHomePageFile = path.join(root, 'src', 'pages', 'Portal', 'Home', 'index.tsx');
const portalHomePageJsFile = path.join(root, 'src', 'pages', 'Portal', 'Home', 'index.js');
const sellerPageFile = path.join(root, 'src', 'pages', 'Seller', 'index.tsx');
const sellerPageJsFile = path.join(root, 'src', 'pages', 'Seller', 'index.js');
const buyerPageFile = path.join(root, 'src', 'pages', 'Buyer', 'index.tsx');
const buyerPageJsFile = path.join(root, 'src', 'pages', 'Buyer', 'index.js');
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

function assertExactSource(source, relativePath, expected, description) {
  const normalize = (value) => value.replace(/\r\n/g, '\n').trim();
  if (normalize(source) !== normalize(expected)) {
    violations.push(`${relativePath} must ${description}; expected exact source: ${expected}`);
  }
}

function assertDoesNotInclude(source, relativePath, forbidden, description) {
  if (source.includes(forbidden)) {
    violations.push(`${relativePath} must ${description}; found ${forbidden}`);
  }
}

function assertDoesNotMatch(source, relativePath, pattern, description) {
  if (pattern.test(source)) {
    violations.push(`${relativePath} must ${description}; found ${pattern}`);
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

const portalServiceJs = assertFileExists(portalServiceJsFile);
if (portalServiceJs) {
  assertExactSource(
    portalServiceJs.source,
    portalServiceJs.relativePath,
    "export * from './session.ts';",
    'be a pure re-export to the guarded TS portal session service implementation',
  );
}

for (const file of [directLoginPageFile]) {
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
    'resolvePortalDirectLoginOpenerOrigin(location.search)',
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
    'location.hash',
    'directLoginToken=',
    'Back to admin login',
    "history.replace('/user/login')",
  ]) {
    if (source.includes(forbidden)) {
      violations.push(
        `${relativePath} must not read direct-login token from URL via ${forbidden}`,
      );
    }
  }
  assertDoesNotInclude(
    source,
    relativePath,
    'clearPortalLogin',
    'not clear existing portal token before direct-login succeeds',
  );
}

const directLoginPageJs = assertFileExists(directLoginPageJsFile);
if (directLoginPageJs) {
  assertExactSource(
    directLoginPageJs.source,
    directLoginPageJs.relativePath,
    "export { default } from './index.tsx';",
    'be a pure re-export to the guarded TSX direct-login page implementation',
  );
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
    'isPortalTerminalPath',
    'isPortalRoute',
  ]) {
    if (!source.includes(expected)) {
      violations.push(`${relativePath} must define portal login routing with ${expected}`);
    }
  }
}

const portalPathsJs = assertFileExists(portalPathsJsFile);
if (portalPathsJs) {
  assertExactSource(
    portalPathsJs.source,
    portalPathsJs.relativePath,
    "export * from './portalPaths.ts';",
    'be a pure re-export to the guarded TS portal path implementation',
  );
}

for (const remoteMenuRouteGuardFile of remoteMenuRouteGuardFiles) {
  const file = assertFileExists(remoteMenuRouteGuardFile);
  if (!file) {
    continue;
  }
  for (const expected of [
    'STATIC_ROUTE_REQUIREMENTS',
    "'/seller': { authority: ['seller:admin:list'] }",
    "'/buyer': { authority: ['buyer:admin:list'] }",
    "authorityMode: 'all'",
    'PUBLIC_PORTAL_ROUTE_PATHS',
    '/seller/login',
    '/buyer/login',
    '/seller/direct-login',
    '/buyer/direct-login',
    '/seller/portal',
    '/buyer/portal',
    'return undefined;',
    'getStaticRouteAuthority(location.pathname)',
    'getStaticRouteAuthorityMode(location.pathname)',
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

const remoteMenuRouteGuardJs = assertFileExists(remoteMenuRouteGuardJsFile);
if (remoteMenuRouteGuardJs) {
  assertExactSource(
    remoteMenuRouteGuardJs.source,
    remoteMenuRouteGuardJs.relativePath,
    "export { default } from './RemoteMenuRouteGuard.tsx';\nexport * from './RemoteMenuRouteGuard.tsx';",
    'be a pure re-export to the guarded TSX remote menu route guard implementation',
  );
}

const adminSession = assertFileExists(adminSessionFile);
if (adminSession) {
  for (const expected of [
    'getRemoteMenuStorageKey(scope)',
    'window.sessionStorage.getItem(storageKey)',
    'window.sessionStorage.setItem(storageKey, JSON.stringify(data))',
    'window.sessionStorage.removeItem(storageKey)',
    "authorityMode === 'all'",
    'permissions.every((permission) => access.hasPerms(permission))',
    'permissions.some((permission) => access.hasPerms(permission))',
    "status: '403'",
    'createGuardedMenuElement(pagePath: string, authority: unknown, authorityMode?: RouteAuthorityMode)',
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
  assertExactSource(
    adminSessionJs.source,
    adminSessionJs.relativePath,
    "export * from './session.ts';",
    'be a pure re-export to the guarded TS session implementation',
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

const remoteMenuStorageJs = assertFileExists(remoteMenuStorageJsFile);
if (remoteMenuStorageJs) {
  assertExactSource(
    remoteMenuStorageJs.source,
    remoteMenuStorageJs.relativePath,
    "export * from './remoteMenuStorage.ts';",
    'be a pure re-export to the guarded TS remote menu storage implementation',
  );
}

const portalLoginPage = assertFileExists(portalLoginPageFile);
if (portalLoginPage) {
  for (const expected of [
    '!isPortalTerminalPath(redirect, terminal)',
    'redirectPath === PORTAL_META[terminal].loginPath',
    "redirectPath === `/${terminal}/direct-login`",
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
  assertDoesNotInclude(
    portalLoginPage.source,
    portalLoginPage.relativePath,
    'clearPortalLogin',
    'not clear existing portal token before login succeeds',
  );
}

const portalLoginPageJs = assertFileExists(portalLoginPageJsFile);
if (portalLoginPageJs) {
  assertExactSource(
    portalLoginPageJs.source,
    portalLoginPageJs.relativePath,
    "export { default } from './index.tsx';",
    'be a pure re-export to the guarded TSX login page implementation',
  );
  assertDoesNotInclude(
    portalLoginPageJs.source,
    portalLoginPageJs.relativePath,
    'clearPortalLogin',
    'not clear existing portal token before login succeeds',
  );
}

const portalHomePage = assertFileExists(portalHomePageFile);
if (portalHomePage) {
  for (const expected of [
    'getTerminalAccessToken(terminal)',
    'history.replace(PORTAL_META[terminal].loginPath)',
    'loadData(terminal);',
    'loadSessions(terminal);',
    "message.error('门户数据加载失败，请稍后重试')",
  ]) {
    assertIncludes(
      portalHomePage.source,
      portalHomePage.relativePath,
      expected,
      'keep portal home token gate and terminal-scoped recovery',
    );
  }
  assertDoesNotInclude(
    portalHomePage.source,
    portalHomePage.relativePath,
    'clearPortalLogin(currentTerminal);',
    'leave 401 redirect and token clearing to the request layer',
  );
  assertDoesNotInclude(
    portalHomePage.source,
    portalHomePage.relativePath,
    'history.replace(PORTAL_META[currentTerminal].loginPath)',
    'not overwrite request-layer portal redirect from loadData failures',
  );
}

const portalHomePageJs = assertFileExists(portalHomePageJsFile);
if (portalHomePageJs) {
  assertExactSource(
    portalHomePageJs.source,
    portalHomePageJs.relativePath,
    "export { default } from './index.tsx';",
    'be a pure re-export to the guarded TSX home page implementation',
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

const sellerPageJs = assertFileExists(sellerPageJsFile);
if (sellerPageJs) {
  assertExactSource(
    sellerPageJs.source,
    sellerPageJs.relativePath,
    "export { default } from './index.tsx';",
    'be a pure re-export to the guarded TSX seller management page implementation',
  );
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

const buyerPageJs = assertFileExists(buyerPageJsFile);
if (buyerPageJs) {
  assertExactSource(
    buyerPageJs.source,
    buyerPageJs.relativePath,
    "export { default } from './index.tsx';",
    'be a pure re-export to the guarded TSX buyer management page implementation',
  );
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

const routeConfig = assertFileExists(routeConfigFile);
if (routeConfig) {
  for (const expected of [
    "'/seller/login'",
    "'/buyer/login'",
    "'./Portal/Login'",
    "'/seller/direct-login'",
    "'/buyer/direct-login'",
    "'/seller/portal'",
    "'/buyer/portal'",
  ]) {
    if (!routeConfig.source.includes(expected)) {
      violations.push(`${routeConfig.relativePath} must keep portal route with ${expected}`);
    }
  }
}

const routeConfigJs = assertFileExists(routeConfigJsFile);
if (routeConfigJs) {
  assertExactSource(
    routeConfigJs.source,
    routeConfigJs.relativePath,
    "export { default } from './routes.ts';",
    'be a pure re-export to the TypeScript route source',
  );
}

const directLoginMessageFiles = [
  path.join(root, 'src', 'utils', 'portalDirectLoginMessage.ts'),
];
const directLoginMessageJsFile = path.join(root, 'src', 'utils', 'portalDirectLoginMessage.js');
for (const directLoginMessageFile of directLoginMessageFiles) {
  const relativePath = path.relative(root, directLoginMessageFile).replaceAll(path.sep, '/');
  if (!fs.existsSync(directLoginMessageFile)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(directLoginMessageFile);
  for (const expected of [
    'PORTAL_DIRECT_LOGIN_RESULT_MESSAGE',
    'PORTAL_DIRECT_LOGIN_OPENER_ORIGIN_PARAM',
    'buildPortalDirectLoginWindowUrl',
    'resolveTargetOrigin',
    'isResultMessage',
    'const loginUrl = buildPortalDirectLoginWindowUrl(result.loginUrl)',
    'const targetOrigin = resolveTargetOrigin(loginUrl)',
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

const directLoginMessageJs = assertFileExists(directLoginMessageJsFile);
if (directLoginMessageJs) {
  assertExactSource(
    directLoginMessageJs.source,
    directLoginMessageJs.relativePath,
    "export * from './portalDirectLoginMessage.ts';",
    'be a pure re-export to the guarded TS direct-login message bridge implementation',
  );
}

for (const file of [portalRequestFile]) {
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

const portalRequestJs = assertFileExists(portalRequestJsFile);
if (portalRequestJs) {
  assertExactSource(
    portalRequestJs.source,
    portalRequestJs.relativePath,
    "export * from './portalRequest.ts';",
    'be a pure re-export to the guarded TS portal request classifier implementation',
  );
}

for (const file of [portalTerminalFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    violations.push(`${relativePath} is missing`);
    continue;
  }
  const source = read(file);
  for (const expected of [
    'persistPortalLogin',
    'result.terminal !== expectedTerminal',
    'setTerminalSessionToken(terminal, result.token',
  ]) {
    if (!source.includes(expected)) {
      violations.push(`${relativePath} must keep terminal-matching login persistence with ${expected}`);
    }
  }
  assertDoesNotMatch(
    source,
    relativePath,
    /if\s*\(\s*!result\?\.token\s*\|\|\s*result\.terminal\s*!==\s*expectedTerminal\s*\)\s*\{[^}]*clearPortalLogin\(/,
    'not clear any existing portal token when a login or direct-login response is invalid',
  );
  assertDoesNotMatch(
    source,
    relativePath,
    /clearPortalLogin\(\s*result\??\.terminal\s*\)/,
    'not clear another portal terminal on login terminal mismatch',
  );
}

const portalTerminalJs = assertFileExists(portalTerminalJsFile);
if (portalTerminalJs) {
  assertExactSource(
    portalTerminalJs.source,
    portalTerminalJs.relativePath,
    "export * from './terminal.ts';",
    'be a pure re-export to the guarded TS portal terminal implementation',
  );
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

const appJs = assertFileExists(appJsFile);
if (appJs) {
  assertExactSource(
    appJs.source,
    appJs.relativePath,
    "export {\n"
      + "  getInitialState,\n"
      + "  layout,\n"
      + "  rootContainer,\n"
      + "  onRouteChange,\n"
      + "  patchClientRoutes,\n"
      + "  render,\n"
      + "  request,\n"
      + "} from './app.tsx';",
    'explicitly bridge guarded TSX app runtime exports',
  );
}

const requestErrorConfigJs = assertFileExists(requestErrorConfigJsFile);
if (requestErrorConfigJs) {
  assertExactSource(
    requestErrorConfigJs.source,
    requestErrorConfigJs.relativePath,
    "export * from './requestErrorConfig.ts';",
    'be a pure re-export to the guarded TS request error implementation',
  );
}

for (const file of [requestErrorConfigFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    continue;
  }
  const source = read(file);
  if (
    /case\s+ErrorShowType\.REDIRECT\s*:\s*handleUnauthorized\s*\(\s*requestUrl\s*\)\s*;/.test(source)
  ) {
    violations.push(
      `${relativePath} must not clear tokens or redirect login for non-401 BizError REDIRECT responses`,
    );
  }
  if (
    !/if\s*\(\s*isUnauthorizedCode\s*\(\s*errorCode\s*\)\s*\)\s*\{[\s\S]*?handleUnauthorized\s*\(\s*requestUrl\s*\)\s*;[\s\S]*?throw\s+error\s*;[\s\S]*?\}/.test(source)
  ) {
    violations.push(
      `${relativePath} must keep token cleanup and login redirect scoped to 401 BizError responses`,
    );
  }
}

if (fs.existsSync(adminSessionFile)) {
  const source = read(adminSessionFile);
  if (
    !/export\s+async\s+function\s+getRoutersInfo\s*\(\s*\)[^{]*\{[\s\S]*?throw\s+error\s*;/.test(source)
  ) {
    violations.push(
      'src/services/session.ts getRoutersInfo must throw on non-success responses instead of caching an empty menu',
    );
  }
  if (
    /export\s+async\s+function\s+getRoutersInfo\s*\(\s*\)[\s\S]*?else\s*\{\s*return\s+\[\]\s*;?\s*\}/.test(source)
  ) {
    violations.push(
      'src/services/session.ts getRoutersInfo must not silently return [] for failed remote menu responses',
    );
  }
} else {
  violations.push('src/services/session.ts is missing');
}

for (const file of [appFile]) {
  const relativePath = path.relative(root, file).replaceAll(path.sep, '/');
  if (!fs.existsSync(file)) {
    continue;
  }
  const source = read(file);
  if (!source.includes('function isUnauthorizedError') || !source.includes('function handleUnauthorizedError')) {
    violations.push(
      `${relativePath} must distinguish 401 failures before clearing admin session during bootstrap/menu loading`,
    );
  }
  if (
    /catch\s*\(\s*error\s*\)\s*\{[\s\S]{0,160}?clearAdminSession\s*\(\s*\)\s*;[\s\S]{0,80}?redirectToLogin\s*\(\s*\)\s*;/.test(source)
  ) {
    violations.push(
      `${relativePath} must not clear admin session and redirect login from generic catch(error) blocks`,
    );
  }
  if (
    /getRoutersInfo\s*\(\s*\)[\s\S]{0,260}?catch\s*\(\s*error\s*\)\s*=>\s*\{[\s\S]{0,160}?clearAdminSession\s*\(\s*\)\s*;/.test(source)
  ) {
    violations.push(
      `${relativePath} must not clear admin session for non-401 getRoutersInfo failures`,
    );
  }
  if (
    !/if\s*\(\s*isUnauthorizedCode\s*\(\s*getResponseCode\s*\(\s*response\?\.data\s*\)\s*\)\s*\)\s*\{[\s\S]*?handleUnauthorizedResponse\s*\(\s*response\?\.config\?\.url\s*\)\s*;[\s\S]*?return\s+Promise\.reject\s*\(\s*response\s*\)\s*;[\s\S]*?\}/.test(source)
  ) {
    violations.push(
      `${relativePath} must reject body-level 401 responses after portal/admin redirect handling`,
    );
  }
}

for (const proxyFile of [proxyConfigFile]) {
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
  assertMatches(
    source,
    relativePath,
    /dev\s*:\s*\{[\s\S]*['"`]\/api\/['"`]\s*:\s*\{[\s\S]*target\s*:\s*apiProxyTarget[\s\S]*changeOrigin\s*:\s*true[\s\S]*pathRewrite\s*:\s*\{\s*['"`]\^\/api['"`]\s*:\s*['"`]['"`]\s*,?\s*\}/,
    'keep dev /api/ proxy target, changeOrigin, and ^/api pathRewrite contract',
  );
}

const proxyConfigJs = assertFileExists(proxyConfigJsFile);
if (proxyConfigJs) {
  assertExactSource(
    proxyConfigJs.source,
    proxyConfigJs.relativePath,
    "export { default } from './proxy.ts';",
    'be a pure re-export to the guarded TS proxy configuration',
  );
}

for (const accessTokenFile of [accessFile]) {
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

const accessJs = assertFileExists(accessJsFile);
if (accessJs) {
  assertExactSource(
    accessJs.source,
    accessJs.relativePath,
    "export { default } from './access.ts';\nexport * from './access.ts';",
    'be a pure re-export to the guarded TS access implementation',
  );
}

if (violations.length > 0) {
  console.error('Portal token isolation guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Portal token isolation guard passed.');
