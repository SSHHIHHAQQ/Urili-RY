#!/usr/bin/env node

const TERMINALS = ['seller', 'buyer'];

const WRITE_CONFIRM_ENV = 'PORTAL_LIVE_WRITE_CONFIRM';
const WRITE_CONFIRM_VALUE = 'APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY';

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

const FORBIDDEN_PORTAL_ACCOUNT_FIELDS = [
  'subjectId',
  'terminal',
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
  SELLER_PORTAL_USERNAME=... SELLER_PORTAL_PASSWORD=... \\
  BUYER_PORTAL_USERNAME=... BUYER_PORTAL_PASSWORD=... \\
  ${WRITE_CONFIRM_ENV}=${WRITE_CONFIRM_VALUE} \\
  node scripts/verify-portal-self-management-live-write.mjs

This script performs live write checks for the minimal seller/buyer portal
self-management framework. It does not read .env.local, does not execute SQL,
and does not touch product, order, inventory, logistics, finance, fulfillment,
or integration business APIs. It leaves a disabled STAFF account as evidence
because the portal self-management surface intentionally has no account delete
endpoint. The default base URL is the backend service on 8080; set
PORTAL_LIVE_API_PREFIX=/api only when targeting the React dev proxy.
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
  if (process.env[WRITE_CONFIRM_ENV] !== WRITE_CONFIRM_VALUE) {
    missing.push(`${WRITE_CONFIRM_ENV}=${WRITE_CONFIRM_VALUE}`);
  }
  if (missing.length > 0) {
    throw new Error(`Missing required live write env vars: ${missing.join(', ')}`);
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

async function authorizedRequest(terminal, token, method, path, payload) {
  const { response, body } = await requestJson(`/${terminal}${path}`, {
    method,
    headers: { Authorization: `Bearer ${token}` },
    ...(payload === undefined ? {} : { body: JSON.stringify(payload) }),
  });
  return assertSuccess(`${terminal} ${method} ${path}`, response, body);
}

async function authorizedGet(terminal, token, path) {
  return authorizedRequest(terminal, token, 'GET', path);
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

function assertTerminalRoleMenuTemplate(terminal, menuIds) {
  const [min, max] = terminal === 'seller' ? [100000, 200000] : [200000, 300000];
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

function crossTerminalMenuIdFor(terminal) {
  return terminal === 'seller' ? 200003 : 100008;
}

function assertPortalAccountRecord(terminal, account) {
  if (!Number.isInteger(account?.accountId) || account.accountId <= 0) {
    throw new Error(`${terminal} created account did not return a usable accountId`);
  }
  const leaked = FORBIDDEN_PORTAL_ACCOUNT_FIELDS.filter((field) => Object.prototype.hasOwnProperty.call(account, field));
  if (leaked.length > 0) {
    throw new Error(`${terminal} created account leaked internal subject scope: ${leaked.join(', ')}`);
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

function requireRecord(label, records, predicate) {
  const record = rowsOf(records).find(predicate);
  if (!record) {
    throw new Error(`${label} was not found after write operation`);
  }
  return record;
}

function assertSelfAuditDto(label, body) {
  const serialized = JSON.stringify(rowsOf(body));
  const leaked = FORBIDDEN_SELF_AUDIT_FIELDS.filter((field) => serialized.includes(`"${field}"`));
  if (leaked.length > 0) {
    throw new Error(`${label} leaked internal audit fields: ${leaked.join(', ')}`);
  }
}

function markerFor(terminal) {
  const random = Math.random().toString(36).slice(2, 6);
  return `${terminal}_${Date.now().toString(36)}_${random}`;
}

function isSuccessResponse(response, body) {
  return response.ok && body?.code === 200;
}

async function cleanupUnexpectedRole(terminal, token, roleKey) {
  const roles = rowsOf(await authorizedGet(terminal, token, '/roles'));
  const role = roles.find((item) => item.roleKey === roleKey);
  if (Number.isInteger(role?.roleId) && role.roleId > 0) {
    await authorizedRequest(terminal, token, 'DELETE', `/roles/${role.roleId}`);
  }
  return role;
}

async function assertRoleCreateRejectsInvalidMenuIds(terminal, token, marker, menuIds, invalidMenuId) {
  const roleKey = `verify_reject_${marker}`;
  const { response, body } = await requestJson(`/${terminal}/roles`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      roleName: `Verify Reject ${marker}`,
      roleKey,
      roleSort: 98,
      status: '0',
      menuIds: [...menuIds, invalidMenuId],
      remark: 'portal self-management invalid role menu verification',
    }),
  });
  if (isSuccessResponse(response, body)) {
    await cleanupUnexpectedRole(terminal, token, roleKey);
    throw new Error(`${terminal} role create accepted cross-terminal menu ids`);
  }
  const leakedRole = await cleanupUnexpectedRole(terminal, token, roleKey);
  if (leakedRole) {
    throw new Error(`${terminal} rejected role write still persisted`);
  }
}

async function assertRoleUpdateRejectsInvalidMenuIds(terminal, token, roleId, roleKey, roleName, menuIds, invalidMenuId) {
  const { response, body } = await requestJson(`/${terminal}/roles/${roleId}`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      roleName: `${roleName} Invalid`,
      roleKey,
      roleSort: 99,
      status: '0',
      menuIds: [...menuIds, invalidMenuId],
      remark: 'portal self-management invalid role menu update verification',
    }),
  });
  if (isSuccessResponse(response, body)) {
    throw new Error(`${terminal} role update accepted cross-terminal menu ids`);
  }
  const roleMenuSnapshot = await authorizedGet(terminal, token, `/roles/${roleId}/menus`);
  const checkedKeys = Array.isArray(roleMenuSnapshot.checkedKeys) ? roleMenuSnapshot.checkedKeys : [];
  const expected = new Set(menuIds);
  const changed = checkedKeys.length !== expected.size
    || checkedKeys.some((menuId) => !expected.has(menuId))
    || checkedKeys.includes(invalidMenuId);
  if (changed) {
    throw new Error(`${terminal} role update rejection mutated checkedKeys`);
  }
}

async function verifyTerminalWrites(terminal) {
  const token = await login(terminal);
  const marker = markerFor(terminal);
  const state = {
    token,
    marker,
    accountId: undefined,
    deptId: undefined,
    roleId: undefined,
    roleAssigned: false,
  };

  try {
    const info = await authorizedGet(terminal, token, '/getInfo');
    assertPortalGetInfoContract(terminal, info);
    assertExactSelfManagementPermissions(terminal, info.permissions);

    const roleMenus = await authorizedGet(terminal, token, '/roles/menus');
    const menuIds = collectTreeIds(roleMenus.menus || []);
    assertTerminalRoleMenuTemplate(terminal, menuIds);
    const crossTerminalMenuId = crossTerminalMenuIdFor(terminal);

    const deptName = `Verify ${marker}`;
    await authorizedRequest(terminal, token, 'POST', '/depts', {
      deptName,
      parentId: 0,
      orderNum: 90,
      status: '0',
      leader: 'Portal Verify',
    });
    const createdDept = requireRecord(`${terminal} dept ${deptName}`, await authorizedGet(terminal, token, '/depts'), (
      dept,
    ) => dept.deptName === deptName);
    state.deptId = createdDept.deptId;

    const editedDeptName = `${deptName} Edited`;
    await authorizedRequest(terminal, token, 'PUT', `/depts/${state.deptId}`, {
      deptName: editedDeptName,
      parentId: 0,
      orderNum: 91,
      status: '0',
      leader: 'Portal Verify',
    });
    requireRecord(`${terminal} edited dept ${editedDeptName}`, await authorizedGet(terminal, token, '/depts'), (dept) =>
      dept.deptId === state.deptId && dept.deptName === editedDeptName);

    const roleKey = `verify_${marker}`;
    const roleName = `Verify ${marker}`;
    await assertRoleCreateRejectsInvalidMenuIds(terminal, token, marker, menuIds, crossTerminalMenuId);
    await authorizedRequest(terminal, token, 'POST', '/roles', {
      roleName,
      roleKey,
      roleSort: 90,
      status: '0',
      menuIds,
      remark: 'portal self-management live write verification',
    });
    const createdRole = requireRecord(`${terminal} role ${roleKey}`, await authorizedGet(terminal, token, '/roles'), (
      role,
    ) => role.roleKey === roleKey);
    state.roleId = createdRole.roleId;

    const roleMenuSnapshot = await authorizedGet(terminal, token, `/roles/${state.roleId}/menus`);
    const checkedKeys = Array.isArray(roleMenuSnapshot.checkedKeys) ? roleMenuSnapshot.checkedKeys : [];
    if (checkedKeys.length === 0 || checkedKeys.some((menuId) => !menuIds.includes(menuId))) {
      throw new Error(`${terminal} role menu checkedKeys are not limited to the self-management template`);
    }

    await assertRoleUpdateRejectsInvalidMenuIds(
      terminal,
      token,
      state.roleId,
      roleKey,
      roleName,
      menuIds,
      crossTerminalMenuId,
    );

    await authorizedRequest(terminal, token, 'PUT', `/roles/${state.roleId}`, {
      roleName: `${roleName} Edited`,
      roleKey,
      roleSort: 91,
      status: '0',
      menuIds,
      remark: 'portal self-management live write verification edited',
    });

    const userName = `verify_${marker}`;
    await authorizedRequest(terminal, token, 'POST', '/accounts', {
      userName,
      nickName: `Verify ${terminal}`,
      password: 'U12346',
      accountRole: 'STAFF',
      status: '1',
      deptId: state.deptId,
    });
    const createdAccount = requireRecord(
      `${terminal} account ${userName}`,
      await authorizedGet(terminal, token, '/accounts'),
      (account) => account.userName === userName,
    );
    assertPortalAccountRecord(terminal, createdAccount);
    state.accountId = createdAccount.accountId;

    await authorizedRequest(terminal, token, 'PUT', `/accounts/${state.accountId}/roles`, {
      roleIds: [state.roleId],
    });
    state.roleAssigned = true;
    const accountRoles = await authorizedGet(terminal, token, `/accounts/${state.accountId}/roles`);
    const assignedRoleIds = Array.isArray(accountRoles.checkedKeys) ? accountRoles.checkedKeys : [];
    if (!assignedRoleIds.includes(state.roleId)) {
      throw new Error(`${terminal} account role assignment was not persisted`);
    }

    await authorizedRequest(terminal, token, 'PUT', `/accounts/${state.accountId}`, {
      nickName: `Verify ${terminal} Disabled`,
      status: '1',
      deptId: null,
    });

    assertSelfAuditDto(`${terminal} login logs`, await authorizedGet(terminal, token, '/account/login-logs?pageNum=1&pageSize=5'));
    assertSelfAuditDto(`${terminal} operation logs`, await authorizedGet(terminal, token, '/account/oper-logs?pageNum=1&pageSize=5'));
    assertSelfAuditDto(`${terminal} sessions`, await authorizedGet(terminal, token, '/account/sessions?pageNum=1&pageSize=5'));

    return { terminal, marker, disabledAccountId: state.accountId };
  } finally {
    await cleanupTerminalWrites(terminal, state);
  }
}

async function cleanupTerminalWrites(terminal, state) {
  const errors = [];
  async function cleanup(label, task) {
    try {
      await task();
    } catch (error) {
      errors.push(`${label}: ${error.message}`);
    }
  }

  if (state.token && state.accountId && state.roleAssigned) {
    await cleanup('clear account roles', () =>
      authorizedRequest(terminal, state.token, 'PUT', `/accounts/${state.accountId}/roles`, { roleIds: [] }),
    );
  }
  if (state.token && state.accountId) {
    await cleanup('disable and detach account', () =>
      authorizedRequest(terminal, state.token, 'PUT', `/accounts/${state.accountId}`, {
        nickName: `Verify ${terminal} Disabled`,
        status: '1',
        deptId: null,
      }),
    );
  }
  if (state.token && state.roleId) {
    await cleanup('delete role', () => authorizedRequest(terminal, state.token, 'DELETE', `/roles/${state.roleId}`));
  }
  if (state.token && state.deptId) {
    await cleanup('delete dept', () => authorizedRequest(terminal, state.token, 'DELETE', `/depts/${state.deptId}`));
  }
  if (errors.length > 0) {
    throw new Error(`${terminal} live write cleanup failed: ${errors.join('; ')}`);
  }
}

async function main() {
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    printHelp();
    return;
  }

  requireLiveEnv();
  const results = [];
  for (const terminal of TERMINALS) {
    results.push(await verifyTerminalWrites(terminal));
  }
  console.log(
    `portal self-management live write verification passed. Disabled evidence accounts: ${results
      .map((item) => `${item.terminal}#${item.disabledAccountId}`)
      .join(', ')}`,
  );
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
