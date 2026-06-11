#!/usr/bin/env node

const TERMINALS = {
  seller: {
    subjectEnv: 'SELLER_DIRECT_LOGIN_SUBJECT_ID',
    listPath: '/seller/admin/sellers/list?pageNum=1&pageSize=200',
    idField: 'sellerId',
    statusField: 'status',
    accountsPath: (subjectId) => `/seller/admin/sellers/${subjectId}/accounts`,
    adminDirectLoginPath: (subjectId) => `/seller/admin/sellers/${subjectId}/directLogin`,
  },
  buyer: {
    subjectEnv: 'BUYER_DIRECT_LOGIN_SUBJECT_ID',
    listPath: '/buyer/admin/buyers/list?pageNum=1&pageSize=200',
    idField: 'buyerId',
    statusField: 'status',
    accountsPath: (subjectId) => `/buyer/admin/buyers/${subjectId}/accounts`,
    adminDirectLoginPath: (subjectId) => `/buyer/admin/buyers/${subjectId}/directLogin`,
  },
};

const DIRECT_LOGIN_CONFIRM_ENV = 'PORTAL_DIRECT_LOGIN_LIVE_CONFIRM';
const DIRECT_LOGIN_CONFIRM_VALUE = 'APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY';

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

const ALLOWED_ADMIN_DIRECT_LOGIN_RESULT_FIELDS = [
  'token',
  'ticketId',
  'loginUrl',
  'expireMinutes',
  'expireTime',
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

function printHelp() {
  console.log(`Usage:
  PORTAL_LIVE_BASE_URL=http://127.0.0.1:8080 \\
  PORTAL_LIVE_API_PREFIX= \\
  ADMIN_AUTH_TOKEN=... \\
  # or ADMIN_USERNAME=... ADMIN_PASSWORD=... \\
  # optional: SELLER_DIRECT_LOGIN_SUBJECT_ID=... BUYER_DIRECT_LOGIN_SUBJECT_ID=... \\
  ${DIRECT_LOGIN_CONFIRM_ENV}=${DIRECT_LOGIN_CONFIRM_VALUE} \\
  node scripts/verify-portal-direct-login-live.mjs

This script verifies the admin-to-OWNER direct-login chain for seller and buyer
portals. It does not read .env.local, does not execute SQL, and does not touch
product, order, inventory, logistics, finance, fulfillment, or integration
business APIs. It creates and consumes one short-lived direct-login ticket per
terminal, verifies that the ticket response does not expose internal ids or token
hashes, verifies that the opposite terminal rejects the ticket, verifies that the
same ticket cannot be reused, checks consumed-token getInfo/getRouters
self-management isolation, verifies that the opposite terminal rejects the
consumed portal token, then logs out the consumed portal token on a best-effort
basis. If the terminal subject ids are not provided, it uses the admin auth
context to read the admin seller/buyer lists and select the first active subject
with an active OWNER account. If ADMIN_AUTH_TOKEN is not provided, it can obtain
one from ADMIN_USERNAME and ADMIN_PASSWORD without printing the token. The
default base URL is the backend service on 8080; set
PORTAL_LIVE_API_PREFIX=/api only when targeting the React dev proxy.
`);
}

function otherTerminal(terminal) {
  return terminal === 'seller' ? 'buyer' : 'seller';
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

let cachedAdminBearerToken = null;

function normalizeBearerToken(token) {
  return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
}

async function bearerToken() {
  if (cachedAdminBearerToken) {
    return cachedAdminBearerToken;
  }
  const token = process.env.ADMIN_AUTH_TOKEN || '';
  if (token) {
    cachedAdminBearerToken = normalizeBearerToken(token);
    return cachedAdminBearerToken;
  }
  const username = process.env.ADMIN_USERNAME || '';
  const password = process.env.ADMIN_PASSWORD || '';
  if (!username || !password) {
    throw new Error('ADMIN_AUTH_TOKEN or ADMIN_USERNAME/ADMIN_PASSWORD is required for admin auth.');
  }
  const payload = { username, password };
  if (process.env.ADMIN_LOGIN_UUID || process.env.ADMIN_LOGIN_CODE) {
    payload.uuid = process.env.ADMIN_LOGIN_UUID || '';
    payload.code = process.env.ADMIN_LOGIN_CODE || '';
  }
  const { response, body } = await requestJson('/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  const data = assertSuccess('admin login', response, body);
  const issuedToken = data.token || body.token;
  if (typeof issuedToken !== 'string' || issuedToken.length === 0) {
    throw new Error('admin login did not return a token.');
  }
  cachedAdminBearerToken = normalizeBearerToken(issuedToken);
  return cachedAdminBearerToken;
}

function requireLiveEnv() {
  const missing = [];
  if (!process.env.ADMIN_AUTH_TOKEN && (!process.env.ADMIN_USERNAME || !process.env.ADMIN_PASSWORD)) {
    missing.push('ADMIN_AUTH_TOKEN or ADMIN_USERNAME/ADMIN_PASSWORD');
  }
  if (process.env[DIRECT_LOGIN_CONFIRM_ENV] !== DIRECT_LOGIN_CONFIRM_VALUE) {
    missing.push(`${DIRECT_LOGIN_CONFIRM_ENV}=${DIRECT_LOGIN_CONFIRM_VALUE}`);
  }
  if (missing.length > 0) {
    throw new Error(`Missing required direct-login live env vars: ${missing.join(', ')}`);
  }
}

async function adminGet(path) {
  const { response, body } = await requestJson(path, {
    method: 'GET',
    headers: { Authorization: await bearerToken() },
  });
  return assertSuccess(`admin GET ${path}`, response, body);
}

function isActive(value) {
  return value === undefined || value === null || String(value) === '0';
}

async function resolveDirectLoginSubjectId(terminal, config) {
  const explicitSubjectId = process.env[config.subjectEnv];
  if (explicitSubjectId) {
    return explicitSubjectId;
  }

  const list = await adminGet(config.listPath);
  const subjects = Array.isArray(list.rows) ? list.rows : Array.isArray(list) ? list : [];
  for (const subject of subjects) {
    const subjectId = subject?.[config.idField];
    if (subjectId === undefined || subjectId === null || !isActive(subject?.[config.statusField])) {
      continue;
    }
    const accounts = await adminGet(config.accountsPath(subjectId));
    const rows = Array.isArray(accounts) ? accounts : Array.isArray(accounts?.rows) ? accounts.rows : [];
    const owner = rows.find((account) =>
      String(account?.accountRole || '').toUpperCase() === 'OWNER'
      && isActive(account?.status)
      && isActive(account?.lockStatus)
    );
    if (owner) {
      console.log(`${terminal} direct-login auto-selected subject ${subjectId} with active OWNER account.`);
      return String(subjectId);
    }
  }

  throw new Error(`${terminal} direct-login could not auto-select an active subject with active OWNER account.`);
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
  return body.data ?? body;
}

function assertNoUnexpectedFields(label, data, allowedFields) {
  const unexpected = Object.keys(data || {}).filter((field) => !allowedFields.includes(field));
  if (unexpected.length > 0) {
    throw new Error(`${label} exposed unexpected response fields: ${unexpected.join(', ')}`);
  }
}

function assertNoFrozenBusinessSurface(terminal, label, value) {
  const serialized = JSON.stringify(value).toLowerCase();
  for (const term of FROZEN_BUSINESS_TERMS) {
    if (serialized.includes(`${terminal}:${term}:`) || serialized.includes(`/${term}/`)) {
      throw new Error(`${terminal} direct-login ${label} exposes frozen business surface ${term}`);
    }
  }
}

function assertExactSelfManagementPermissions(terminal, permissions) {
  if (!Array.isArray(permissions)) {
    throw new Error(`${terminal} getInfo returned non-array permissions`);
  }
  const expected = new Set(SELF_MANAGEMENT_PERMISSIONS[terminal]);
  const actual = new Set(permissions);
  const missing = [...expected].filter((permission) => !actual.has(permission));
  const unexpected = [...actual].filter((permission) => !expected.has(permission));
  if (missing.length > 0 || unexpected.length > 0) {
    throw new Error(
      `${terminal} direct-login permissions are not exactly self-management; missing=${
        missing.join(',') || '-'
      } unexpected=${unexpected.join(',') || '-'}`,
    );
  }
}

function assertDirectLoginResultContract(terminal, data) {
  assertNoUnexpectedFields(
    `${terminal} admin direct-login`,
    data,
    ALLOWED_ADMIN_DIRECT_LOGIN_RESULT_FIELDS,
  );
  for (const forbidden of ['accountId', 'username', 'tokenHash']) {
    if (Object.prototype.hasOwnProperty.call(data, forbidden)) {
      throw new Error(`${terminal} admin direct-login leaked internal field ${forbidden}`);
    }
  }
  if (!Number.isInteger(data.expireMinutes) || data.expireMinutes <= 0 || data.expireMinutes > 30) {
    throw new Error(`${terminal} admin direct-login did not return a short-lived expireMinutes`);
  }
  if (typeof data.expireTime !== 'string' || data.expireTime.trim().length === 0) {
    throw new Error(`${terminal} admin direct-login did not return expireTime`);
  }
  if (data.loginUrl.includes('directLoginToken=') || data.loginUrl.includes('token=')) {
    throw new Error(`${terminal} admin direct-login loginUrl leaked token material`);
  }
}

function assertPortalLoginResultContract(terminal, data) {
  assertNoUnexpectedFields(`${terminal} portal direct-login`, data, ALLOWED_PORTAL_LOGIN_RESULT_FIELDS);
  if (!data || data.terminal !== terminal || typeof data.token !== 'string' || data.token.length === 0) {
    throw new Error(`${terminal} portal direct-login returned invalid login payload`);
  }
  if (data.expireMinutes !== undefined && (!Number.isInteger(data.expireMinutes) || data.expireMinutes <= 0)) {
    throw new Error(`${terminal} portal direct-login returned invalid expireMinutes`);
  }
  if (data.expireTime !== undefined && (typeof data.expireTime !== 'string' || data.expireTime.trim().length === 0)) {
    throw new Error(`${terminal} portal direct-login returned invalid expireTime`);
  }
}

function assertPortalGetInfoContract(terminal, data) {
  assertNoUnexpectedFields(`${terminal} direct-login getInfo`, data, ALLOWED_PORTAL_GET_INFO_FIELDS);
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
    throw new Error(`${terminal} direct-login getRouters did not include ${homePerm}`);
  }
  if (homeRoute.path !== `/${terminal}/portal`) {
    throw new Error(`${terminal} direct-login portal home route path is not terminal-scoped`);
  }
  const expectedComponent = `${terminal === 'seller' ? 'Seller' : 'Buyer'}/Portal/index`;
  if (homeRoute.component !== expectedComponent) {
    throw new Error(`${terminal} direct-login portal home route component is not terminal-scoped`);
  }
  for (const route of flatRouters) {
    const perms = typeof route?.perms === 'string' ? route.perms.trim() : '';
    if (!perms) {
      continue;
    }
    if (perms.includes('*')) {
      throw new Error(`${terminal} direct-login getRouters exposes wildcard permission ${perms}`);
    }
    if (!perms.startsWith(`${terminal}:`)) {
      throw new Error(`${terminal} direct-login getRouters exposes cross-terminal permission ${perms}`);
    }
    if (perms.startsWith(`${terminal}:admin:`)) {
      throw new Error(`${terminal} direct-login getRouters exposes admin permission ${perms}`);
    }
    const isSelfManagement = SELF_MANAGEMENT_PERMISSIONS[terminal].includes(perms);
    if (!isSelfManagement) {
      throw new Error(`${terminal} direct-login getRouters exposes non self-management permission ${perms}`);
    }
  }
}

async function createDirectLoginTicket(terminal, config) {
  const subjectId = await resolveDirectLoginSubjectId(terminal, config);
  const { response, body } = await requestJson(config.adminDirectLoginPath(subjectId), {
    method: 'POST',
    headers: { Authorization: await bearerToken() },
    body: JSON.stringify({ reason: `portal direct-login live verification ${terminal}` }),
  });
  const data = assertSuccess(`${terminal} admin direct-login ticket`, response, body);
  if (!data || typeof data.token !== 'string' || data.token.length === 0) {
    throw new Error(`${terminal} admin direct-login did not return a one-time token`);
  }
  if (!Number.isInteger(data.ticketId) || data.ticketId <= 0) {
    throw new Error(`${terminal} admin direct-login did not return a valid ticketId`);
  }
  if (typeof data.loginUrl !== 'string' || !data.loginUrl.includes(`/${terminal}/direct-login`)) {
    throw new Error(`${terminal} admin direct-login did not return the current terminal direct-login URL`);
  }
  assertDirectLoginResultContract(terminal, data);
  return data;
}

async function assertDirectLoginTicketRejectedByOtherTerminal(terminal, ticket) {
  const targetTerminal = otherTerminal(terminal);
  const { response, body } = await requestJson(`/${targetTerminal}/direct-login`, {
    method: 'POST',
    body: JSON.stringify({ directLoginToken: ticket.token }),
  });
  if (response.ok && body.code === 200) {
    throw new Error(`${terminal} direct-login ticket was accepted by ${targetTerminal}`);
  }
}

async function consumeDirectLoginTicket(terminal, ticket) {
  const { response, body } = await requestJson(`/${terminal}/direct-login`, {
    method: 'POST',
    body: JSON.stringify({ directLoginToken: ticket.token }),
  });
  const data = assertSuccess(`${terminal} portal direct-login consume`, response, body);
  assertPortalLoginResultContract(terminal, data);
  return data.token;
}

async function assertDirectLoginTicketCannotBeReused(terminal, ticket) {
  const { response, body } = await requestJson(`/${terminal}/direct-login`, {
    method: 'POST',
    body: JSON.stringify({ directLoginToken: ticket.token }),
  });
  if (response.ok && body.code === 200) {
    throw new Error(`${terminal} one-time direct-login token was accepted twice`);
  }
}

async function verifyDirectLoginToken(terminal, token) {
  const { response, body } = await requestJson(`/${terminal}/getInfo`, {
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  const info = assertSuccess(`${terminal} direct-login getInfo`, response, body);
  assertPortalGetInfoContract(terminal, info);
  assertExactSelfManagementPermissions(terminal, info.permissions);
  assertNoFrozenBusinessSurface(terminal, 'getInfo', info);

  const routersResponse = await requestJson(`/${terminal}/getRouters`, {
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  const routers = assertSuccess(
    `${terminal} direct-login getRouters`,
    routersResponse.response,
    routersResponse.body,
  );
  assertSelfManagementRouters(terminal, routers);
  assertNoFrozenBusinessSurface(terminal, 'getRouters', routers);
}

async function assertDirectLoginTokenRejectedByOtherTerminal(terminal, token) {
  const targetTerminal = otherTerminal(terminal);
  for (const endpoint of ['getInfo', 'getRouters']) {
    const { response, body } = await requestJson(`/${targetTerminal}/${endpoint}`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.ok && body.code === 200) {
      throw new Error(`${terminal} direct-login token was accepted by ${targetTerminal} ${endpoint}`);
    }
  }
}

async function logoutPortalToken(terminal, token) {
  try {
    await requestJson(`/${terminal}/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    });
  } catch (error) {
    console.warn(`${terminal} direct-login logout cleanup failed: ${error.message}`);
  }
}

async function verifyTerminal(terminal, config) {
  const ticket = await createDirectLoginTicket(terminal, config);
  await assertDirectLoginTicketRejectedByOtherTerminal(terminal, ticket);
  const token = await consumeDirectLoginTicket(terminal, ticket);
  try {
    await assertDirectLoginTicketCannotBeReused(terminal, ticket);
    await verifyDirectLoginToken(terminal, token);
    await assertDirectLoginTokenRejectedByOtherTerminal(terminal, token);
  } finally {
    await logoutPortalToken(terminal, token);
  }
  return `${terminal}#${ticket.ticketId}`;
}

async function main() {
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    printHelp();
    return;
  }

  requireLiveEnv();
  const tickets = [];
  for (const [terminal, config] of Object.entries(TERMINALS)) {
    tickets.push(await verifyTerminal(terminal, config));
  }
  console.log(`portal direct-login live verification passed. Tickets: ${tickets.join(', ')}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
