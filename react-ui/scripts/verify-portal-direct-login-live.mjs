#!/usr/bin/env node

const TERMINALS = {
  seller: {
    subjectEnv: 'SELLER_DIRECT_LOGIN_SUBJECT_ID',
    adminDirectLoginPath: (subjectId) => `/seller/admin/sellers/${subjectId}/directLogin`,
  },
  buyer: {
    subjectEnv: 'BUYER_DIRECT_LOGIN_SUBJECT_ID',
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

function printHelp() {
  console.log(`Usage:
  PORTAL_LIVE_BASE_URL=http://127.0.0.1:8080 \\
  PORTAL_LIVE_API_PREFIX= \\
  ADMIN_AUTH_TOKEN=... \\
  SELLER_DIRECT_LOGIN_SUBJECT_ID=... \\
  BUYER_DIRECT_LOGIN_SUBJECT_ID=... \\
  ${DIRECT_LOGIN_CONFIRM_ENV}=${DIRECT_LOGIN_CONFIRM_VALUE} \\
  node scripts/verify-portal-direct-login-live.mjs

This script verifies the admin-to-OWNER direct-login chain for seller and buyer
portals. It does not read .env.local, does not execute SQL, and does not touch
product, order, inventory, logistics, finance, fulfillment, or integration
business APIs. It creates and consumes one short-lived direct-login ticket per
terminal, verifies that the same ticket cannot be reused, then logs out the
consumed portal token on a best-effort basis. The default base URL is the backend
service on 8080; set PORTAL_LIVE_API_PREFIX=/api only when targeting the React
dev proxy.
`);
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

function bearerToken() {
  const token = process.env.ADMIN_AUTH_TOKEN || '';
  return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
}

function requireLiveEnv() {
  const missing = [];
  if (!process.env.ADMIN_AUTH_TOKEN) {
    missing.push('ADMIN_AUTH_TOKEN');
  }
  if (process.env[DIRECT_LOGIN_CONFIRM_ENV] !== DIRECT_LOGIN_CONFIRM_VALUE) {
    missing.push(`${DIRECT_LOGIN_CONFIRM_ENV}=${DIRECT_LOGIN_CONFIRM_VALUE}`);
  }
  for (const config of Object.values(TERMINALS)) {
    if (!process.env[config.subjectEnv]) {
      missing.push(config.subjectEnv);
    }
  }
  if (missing.length > 0) {
    throw new Error(`Missing required direct-login live env vars: ${missing.join(', ')}`);
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
  return body.data ?? body;
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

async function createDirectLoginTicket(terminal, config) {
  const subjectId = process.env[config.subjectEnv];
  const { response, body } = await requestJson(config.adminDirectLoginPath(subjectId), {
    method: 'POST',
    headers: { Authorization: bearerToken() },
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
  return data;
}

async function consumeDirectLoginTicket(terminal, ticket) {
  const { response, body } = await requestJson(`/${terminal}/direct-login`, {
    method: 'POST',
    body: JSON.stringify({ directLoginToken: ticket.token }),
  });
  const data = assertSuccess(`${terminal} portal direct-login consume`, response, body);
  if (!data || data.terminal !== terminal || typeof data.token !== 'string' || data.token.length === 0) {
    throw new Error(`${terminal} portal direct-login returned invalid login payload`);
  }
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
  assertExactSelfManagementPermissions(terminal, info.permissions);
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
  const token = await consumeDirectLoginTicket(terminal, ticket);
  try {
    await assertDirectLoginTicketCannotBeReused(terminal, ticket);
    await verifyDirectLoginToken(terminal, token);
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
