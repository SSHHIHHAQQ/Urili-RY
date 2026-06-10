#!/usr/bin/env node

const TERMINALS = ['seller', 'buyer'];

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

const FROZEN_PERMISSION_PARTS = [
  ':product:',
  ':order:',
  ':inventory:',
  ':logistics:',
  ':finance:',
  ':fulfillment:',
  ':integration:',
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

function printHelp() {
  console.log(`Usage:
  PORTAL_LIVE_BASE_URL=http://127.0.0.1:8080 \\
  PORTAL_LIVE_API_PREFIX= \\
  SELLER_PORTAL_USERNAME=... SELLER_PORTAL_PASSWORD=... \\
  BUYER_PORTAL_USERNAME=... BUYER_PORTAL_PASSWORD=... \\
  node scripts/verify-portal-self-management-live.mjs

This script performs read-only live checks for the minimal seller/buyer portal
self-management framework. It does not read .env.local and does not execute SQL
or portal write operations. The default base URL is the backend service on 8080;
set PORTAL_LIVE_API_PREFIX=/api only when targeting the React dev proxy.
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
  return body.data;
}

async function login(terminal) {
  const username = process.env[envName(terminal, 'USERNAME')];
  const password = process.env[envName(terminal, 'PASSWORD')];
  const { response, body } = await requestJson(`/${terminal}/login`, {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  const data = assertSuccess(`${terminal} login`, response, body);
  if (!data || data.terminal !== terminal || typeof data.token !== 'string' || data.token.length === 0) {
    throw new Error(`${terminal} login returned invalid token payload`);
  }
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

function assertNoFrozenBusinessSurface(terminal, label, value) {
  const serialized = JSON.stringify(value);
  const frozen = FROZEN_PERMISSION_PARTS.filter((part) => serialized.includes(`${terminal}${part}`));
  if (frozen.length > 0) {
    throw new Error(`${terminal} ${label} exposes frozen business permissions: ${frozen.join(', ')}`);
  }
}

async function assertCrossTerminalTokenRejected(sourceTerminal, targetTerminal, token) {
  const { response, body } = await requestJson(`/${targetTerminal}/getInfo`, {
    method: 'GET',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (response.ok && body.code === 200) {
    throw new Error(`${sourceTerminal} token was accepted by ${targetTerminal} getInfo`);
  }
}

async function assertAnonymousPortalRequestRejected(terminal) {
  const { response, body } = await requestJson(`/${terminal}/getInfo`, {
    method: 'GET',
  });
  if (response.ok && body.code === 200) {
    throw new Error(`${terminal} anonymous getInfo request was accepted`);
  }
}

async function verifyTerminal(terminal) {
  const token = await login(terminal);
  const info = await authorizedGet(terminal, token, '/getInfo');
  assertSelfManagementPermissions(terminal, info.permissions);
  assertNoFrozenBusinessSurface(terminal, 'getInfo', info);

  const routers = await authorizedGet(terminal, token, '/getRouters');
  assertNoFrozenBusinessSurface(terminal, 'getRouters', routers);

  for (const endpoint of READ_ENDPOINTS) {
    const data = await authorizedGet(terminal, token, endpoint);
    assertNoFrozenBusinessSurface(terminal, endpoint, data);
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
