#!/usr/bin/env node

const TERMINALS = ['seller', 'buyer'];

const LIVE_CONFIRM_ENV = 'PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM';
const LIVE_CONFIRM_VALUE = 'APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY';

const SELF_MANAGEMENT_PERMISSIONS = {
  seller: [
    'seller:portal:home',
    'seller:account:list',
    'seller:account:add',
    'seller:account:edit',
    'seller:account:role:query',
    'seller:account:role:edit',
    'seller:account:loginLog:list',
    'seller:account:operLog:list',
    'seller:account:session:list',
    'seller:dept:list',
    'seller:dept:query',
    'seller:dept:add',
    'seller:dept:edit',
    'seller:dept:remove',
    'seller:role:list',
    'seller:role:query',
    'seller:role:add',
    'seller:role:edit',
    'seller:role:remove',
  ],
  buyer: [
    'buyer:portal:home',
    'buyer:account:list',
    'buyer:account:add',
    'buyer:account:edit',
    'buyer:account:role:query',
    'buyer:account:role:edit',
    'buyer:account:loginLog:list',
    'buyer:account:operLog:list',
    'buyer:account:session:list',
    'buyer:dept:list',
    'buyer:dept:query',
    'buyer:dept:add',
    'buyer:dept:edit',
    'buyer:dept:remove',
    'buyer:role:list',
    'buyer:role:query',
    'buyer:role:add',
    'buyer:role:edit',
    'buyer:role:remove',
    'buyer:product:center:list',
    'buyer:product:center:query',
  ],
};

const FROZEN_BUSINESS_TERMS = [
  'product',
  'order',
  'inventory',
  'logistics',
  'finance',
  'fulfillment',
  'integration',
];

const ALLOWED_BUSINESS_PERMISSION_PREFIXES = {
  seller: [],
  buyer: ["buyer:product:center:"],
};

const FORBIDDEN_SELF_AUDIT_FIELDS = [
  'subjectId',
  'accountId',
  'directLoginTicketId',
  'actingAdminId',
  'actingAdminName',
  'directLoginReason',
  'operParam',
  'jsonResult',
  'tokenId',
];

const ALLOWED_PORTAL_LOGIN_RESULT_FIELDS = [
  'token',
  'terminal',
  'subjectNo',
  'username',
  'nickName',
  'expireMinutes',
  'expireTime',
];

const ALLOWED_PORTAL_GET_INFO_FIELDS = [
  'subjectNo',
  'userName',
  'nickName',
  'roles',
  'permissions',
];

const READ_ENDPOINTS = [
  '/accounts',
  '/depts',
  '/depts/treeselect',
  '/roles',
  '/roles/menus',
  '/account/login-logs?pageNum=1&pageSize=5',
  '/account/oper-logs?pageNum=1&pageSize=5',
  '/account/sessions?pageNum=1&pageSize=5',
];

const SELF_AUDIT_ENDPOINT_PREFIXES = [
  '/account/login-logs',
  '/account/oper-logs',
  '/account/sessions',
];

function printHelp() {
  console.log(`Usage:
  PORTAL_LIVE_BASE_URL=http://127.0.0.1:8080 \\
  PORTAL_LIVE_API_PREFIX= \\
  SELLER_PORTAL_USERNAME=... SELLER_PORTAL_PASSWORD=... \\
  BUYER_PORTAL_USERNAME=... BUYER_PORTAL_PASSWORD=... \\
  ${LIVE_CONFIRM_ENV}=${LIVE_CONFIRM_VALUE} \\
  node scripts/verify-portal-self-management-live.mjs

This script performs read-only live checks for the minimal seller/buyer portal
self-management framework. It does not read .env.local and does not execute SQL
or portal write operations after login. It does perform real seller/buyer portal
logins, which can create login logs and sessions, so it requires explicit live
confirmation. It also verifies that each terminal token is rejected by the
opposite terminal getInfo/getRouters endpoints. The default base URL is the backend service on 8080; set PORTAL_LIVE_API_PREFIX=/api only when targeting the React dev proxy.
`);
}

function envName(terminal, field) {
  return `${terminal.toUpperCase()}_PORTAL_${field}`;
}

function baseUrl() {
  return (process.env.PORTAL_LIVE_BASE_URL || 'http://127.0.0.1:8080').replace(/\/+$/, '');
}

function apiPrefix() {
  const raw = process.env.PORTAL_LIVE_API_PREFIX || '';
  if (!raw.trim()) {
    return '';
  }
  return `/${raw.trim().replace(/^\/+|\/+$/g, '')}`;
}

function apiPath(path) {
  return `${apiPrefix()}${path.startsWith('/') ? path : `/${path}`}`;
}

function requireLiveEnv() {
  const missing = [];
  if (process.env[LIVE_CONFIRM_ENV] !== LIVE_CONFIRM_VALUE) {
    missing.push(`${LIVE_CONFIRM_ENV}=${LIVE_CONFIRM_VALUE}`);
  }
  for (const terminal of TERMINALS) {
    for (const field of ['USERNAME', 'PASSWORD']) {
      const key = envName(terminal, field);
      if (!process.env[key]) {
        missing.push(key);
      }
    }
  }
  if (missing.length > 0) {
    throw new Error(`Missing required live env vars: ${missing.join(', ')}`);
  }
}

async function requestJson(path, options = {}) {
  const response = await fetch(`${baseUrl()}${apiPath(path)}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json;charset=UTF-8' } : {}),
      ...(options.headers || {}),
    },
  });
  const text = await response.text();
  let body;
  try {
    body = text ? JSON.parse(text) : {};
  } catch (error) {
    throw new Error(`Non-JSON response from ${path}: HTTP ${response.status} ${text.slice(0, 200)}`);
  }
  return { response, body };
}

function assertSuccess(label, response, body) {
  if (!response.ok || body.code !== 200) {
    throw new Error(`${label} failed: HTTP ${response.status}, code=${body.code}, msg=${body.msg || ''}`);
  }
  return Object.prototype.hasOwnProperty.call(body, 'data') ? body.data : body;
}

function assertNoUnexpectedFields(label, data, allowedFields) {
  const unexpected = Object.keys(data || {}).filter((field) => !allowedFields.includes(field));
  if (unexpected.length > 0) {
    throw new Error(`${label} exposed unexpected response fields: ${unexpected.join(', ')}`);
  }
}

function assertPortalLoginResultContract(terminal, data) {
  assertNoUnexpectedFields(`${terminal} login`, data, ALLOWED_PORTAL_LOGIN_RESULT_FIELDS);
  if (!data || data.terminal !== terminal || typeof data.token !== 'string' || data.token.length === 0) {
    throw new Error(`${terminal} login returned invalid token payload`);
  }
  if (data.expireMinutes !== undefined && (!Number.isInteger(data.expireMinutes) || data.expireMinutes <= 0)) {
    throw new Error(`${terminal} login returned invalid expireMinutes`);
  }
  if (data.expireTime !== undefined && (typeof data.expireTime !== 'string' || data.expireTime.trim().length === 0)) {
    throw new Error(`${terminal} login returned invalid expireTime`);
  }
}

function assertPortalGetInfoContract(terminal, data) {
  assertNoUnexpectedFields(`${terminal} getInfo`, data, ALLOWED_PORTAL_GET_INFO_FIELDS);
}

async function login(terminal) {
  const username = process.env[envName(terminal, 'USERNAME')];
  const password = process.env[envName(terminal, 'PASSWORD')];
  const { response, body } = await requestJson(`/${terminal}/login`, {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  const data = assertSuccess(`${terminal} login`, response, body);
  assertPortalLoginResultContract(terminal, data);
  return data.token;
}

async function authorizedGet(terminal, token, path) {
  const { response, body } = await requestJson(`/${terminal}${path}`, {
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  return assertSuccess(`${terminal} ${path}`, response, body);
}

function assertSelfManagementPermissions(terminal, permissions) {
  if (!Array.isArray(permissions)) {
    throw new Error(`${terminal} getInfo returned non-array permissions`);
  }
  const expected = new Set(SELF_MANAGEMENT_PERMISSIONS[terminal]);
  const actual = new Set(permissions);
  const missing = [...expected].filter((permission) => !actual.has(permission));
  const unexpected = [...actual].filter((permission) => !expected.has(permission));
  if (missing.length > 0 || unexpected.length > 0) {
    throw new Error(
      `${terminal} permissions are not exactly self-management; missing=${missing.join(',') || '-'} unexpected=${
        unexpected.join(',') || '-'
      }`,
    );
  }
}

function collectTreeIds(nodes, target = []) {
  for (const node of nodes || []) {
    if (Number.isInteger(node.id) && node.id > 0) {
      target.push(node.id);
    }
    collectTreeIds(node.children || [], target);
  }
  return target;
}

function assertTerminalRoleMenuTemplate(terminal, value) {
  const [min, max] = terminal === 'seller' ? [100000, 200000] : [200000, 300000];
  const menuIds = collectTreeIds(value?.menus || []);
  const uniqueMenuIds = new Set(menuIds);
  const invalid = menuIds.filter((menuId) => menuId < min || menuId >= max);
  if (menuIds.length === 0) {
    throw new Error(`${terminal} role menu template is empty`);
  }
  if (uniqueMenuIds.size !== menuIds.length) {
    throw new Error(`${terminal} role menu template contains duplicate ids`);
  }
  if (invalid.length > 0) {
    throw new Error(`${terminal} role menu template has cross-terminal ids: ${invalid.join(',')}`);
  }
  if (menuIds.length !== SELF_MANAGEMENT_PERMISSIONS[terminal].length) {
    throw new Error(
      `${terminal} role menu template is not exactly self-management; expected=${
        SELF_MANAGEMENT_PERMISSIONS[terminal].length
      } actual=${menuIds.length}`,
    );
  }
}

function assertNoFrozenBusinessSurface(terminal, label, value) {
  let serialized = JSON.stringify(value ?? {}).toLowerCase();
  for (const prefix of ALLOWED_BUSINESS_PERMISSION_PREFIXES[terminal] || []) {
    serialized = serialized.replaceAll(prefix, '');
  }
  const frozen = FROZEN_BUSINESS_TERMS.filter(
    (term) => serialized.includes(`${terminal}:${term}:`) || serialized.includes(`/${term}/`),
  );
  if (frozen.length > 0) {
    throw new Error(`${terminal} ${label} exposes frozen business permissions: ${frozen.join(', ')}`);
  }
}

function rowsOf(value) {
  if (Array.isArray(value)) {
    return value;
  }
  if (Array.isArray(value?.data)) {
    return value.data;
  }
  if (Array.isArray(value?.rows)) {
    return value.rows;
  }
  return [];
}

function assertSelfAuditDto(label, body) {
  const serialized = JSON.stringify(rowsOf(body));
  const leaked = FORBIDDEN_SELF_AUDIT_FIELDS.filter((field) => serialized.includes(`"${field}"`));
  if (leaked.length > 0) {
    throw new Error(`${label} leaked internal audit fields: ${leaked.join(', ')}`);
  }
}

function isSelfAuditEndpoint(endpoint) {
  return SELF_AUDIT_ENDPOINT_PREFIXES.some((prefix) => endpoint.startsWith(prefix));
}

function flattenRouters(nodes, target = []) {
  for (const node of nodes || []) {
    target.push(node);
    flattenRouters(node.children || [], target);
  }
  return target;
}

function assertSelfManagementRouters(terminal, routers) {
  const flatRouters = flattenRouters(Array.isArray(routers) ? routers : []);
  const homePerm = `${terminal}:portal:home`;
  const homeRoute = flatRouters.find((route) => route?.perms === homePerm);
  if (!homeRoute) {
    throw new Error(`${terminal} getRouters did not include ${homePerm}`);
  }
  if (homeRoute.path !== `/${terminal}/portal`) {
    throw new Error(`${terminal} portal home route path is not terminal-scoped`);
  }
  const expectedComponent = `${terminal === 'seller' ? 'Seller' : 'Buyer'}/Portal/index`;
  if (homeRoute.component !== expectedComponent) {
    throw new Error(`${terminal} portal home route component is not terminal-scoped`);
  }
  for (const route of flatRouters) {
    const perms = typeof route?.perms === 'string' ? route.perms.trim() : '';
    if (!perms) {
      continue;
    }
    if (perms.includes('*')) {
      throw new Error(`${terminal} getRouters exposes wildcard permission ${perms}`);
    }
    if (!perms.startsWith(`${terminal}:`)) {
      throw new Error(`${terminal} getRouters exposes cross-terminal permission ${perms}`);
    }
    if (perms.startsWith(`${terminal}:admin:`)) {
      throw new Error(`${terminal} getRouters exposes admin permission ${perms}`);
    }
    const isSelfManagement = SELF_MANAGEMENT_PERMISSIONS[terminal].includes(perms);
    if (!isSelfManagement) {
      throw new Error(`${terminal} getRouters exposes non self-management permission ${perms}`);
    }
  }
}

async function assertCrossTerminalTokenRejected(sourceTerminal, targetTerminal, token) {
  for (const endpoint of ['getInfo', 'getRouters']) {
    const { response, body } = await requestJson(`/${targetTerminal}/${endpoint}`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.ok && body.code === 200) {
      throw new Error(`${sourceTerminal} token was accepted by ${targetTerminal} ${endpoint}`);
    }
  }
}

async function assertAnonymousPortalRequestRejected(terminal) {
  for (const endpoint of ['getInfo', 'getRouters']) {
    const { response, body } = await requestJson(`/${terminal}/${endpoint}`, {
      method: 'GET',
    });
    if (response.ok && body.code === 200) {
      throw new Error(`${terminal} anonymous ${endpoint} request was accepted`);
    }
  }
}

async function verifyTerminal(terminal) {
  const token = await login(terminal);
  const info = await authorizedGet(terminal, token, '/getInfo');
  assertPortalGetInfoContract(terminal, info);
  assertSelfManagementPermissions(terminal, info.permissions);
  assertNoFrozenBusinessSurface(terminal, 'getInfo', info);

  const routers = await authorizedGet(terminal, token, '/getRouters');
  assertSelfManagementRouters(terminal, routers);
  assertNoFrozenBusinessSurface(terminal, 'getRouters', routers);

  for (const endpoint of READ_ENDPOINTS) {
    const data = await authorizedGet(terminal, token, endpoint);
    assertNoFrozenBusinessSurface(terminal, endpoint, data);
    if (endpoint === '/roles/menus') {
      assertTerminalRoleMenuTemplate(terminal, data);
    }
    if (isSelfAuditEndpoint(endpoint)) {
      assertSelfAuditDto(`${terminal} ${endpoint}`, data);
    }
  }

  return token;
}

async function main() {
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    printHelp();
    return;
  }

  requireLiveEnv();
  const tokens = {};
  for (const terminal of TERMINALS) {
    await assertAnonymousPortalRequestRejected(terminal);
    tokens[terminal] = await verifyTerminal(terminal);
  }
  await assertCrossTerminalTokenRejected('seller', 'buyer', tokens.seller);
  await assertCrossTerminalTokenRejected('buyer', 'seller', tokens.buyer);
  console.log('portal self-management live read-only verification passed.');
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
