import fs from 'fs';
import path from 'path';

const uiRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(uiRoot, '..');

function readUiSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function readRepoSource(relativePath: string) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8');
}

const EXPECTED_SELF_MANAGEMENT_PERMISSIONS = {
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

function extractTerminalPermissions(script: string, terminal: 'seller' | 'buyer') {
  return [...script.matchAll(new RegExp(`'${terminal}:[^']+'`, 'g'))].map((match) =>
    match[0].slice(1, -1),
  );
}

describe('portal self-management live verifier contract', () => {
  const script = readUiSource('scripts/verify-portal-self-management-live.mjs');
  const packageJson = JSON.parse(readUiSource('package.json'));
  const manifest = JSON.parse(readUiSource('tests/three-terminal.manifest.json'));

  it('keeps the live verifier exposed and covered by the three-terminal gate', () => {
    expect(packageJson.scripts['verify:portal-self-management-live']).toBe(
      'node scripts/verify-portal-self-management-live.mjs',
    );
    expect(manifest.frontendTestPaths).toContain(
      'tests/portal-self-management-live-contract.test.ts',
    );
    expect(manifest.criticalFrontendExplicitTestPaths).toContain(
      'tests/portal-self-management-live-contract.test.ts',
    );
  });

  it('stays env-driven and does not read local secret files or runtime data source env', () => {
    for (const envName of [
      'PORTAL_LIVE_BASE_URL',
      'PORTAL_LIVE_API_PREFIX',
      'SELLER_PORTAL_USERNAME',
      'SELLER_PORTAL_PASSWORD',
      'BUYER_PORTAL_USERNAME',
      'BUYER_PORTAL_PASSWORD',
      'PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM',
      'APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY',
    ]) {
      expect(script).toContain(envName);
    }
    expect(script).toContain("const LIVE_CONFIRM_ENV = 'PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM'");
    expect(script).toContain(
      "const LIVE_CONFIRM_VALUE = 'APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY'",
    );
    expect(script).toContain('requires explicit live');
    expect(script).toContain('create login logs and sessions');
    expect(script).toContain('It does not read .env.local');
    expect(script).not.toContain('readFile');
    expect(script).not.toContain("from 'fs'");
    expect(script).not.toContain('require("fs")');
    expect(script).not.toContain("require('fs')");
    expect(script).not.toContain('dotenv');
    expect(script).not.toContain('RUOYI_DB_');
    expect(script).not.toContain('RUOYI_REDIS_');
    expect(script).toContain("process.env.PORTAL_LIVE_API_PREFIX || ''");
    expect(script).toContain('The default base URL is');
    expect(script).toContain('the backend service on 8080');
    expect(script).toContain('set PORTAL_LIVE_API_PREFIX=/api only when targeting');
    expect(script).toContain('the React dev proxy');
    expect(script).toContain('opposite terminal getInfo/getRouters endpoints');
    expect(script).toContain('apiPath(path)');
    expect(script).not.toContain('requestJson(`/api/');
  });

  it('keeps the runbook aligned with the explicit live confirmation gate', () => {
    const runbook = readRepoSource(
      'docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md',
    );
    expect(runbook).toContain(
      "$env:PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM = 'APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY'",
    );
    expect(runbook).toContain('如果缺少确认变量或任一账号密码环境变量');
    expect(runbook).toContain('npm run verify:portal-self-management-live');
  });

  it('checks explicit live confirmation before any portal login can run', () => {
    const mainIndex = script.indexOf('async function main()');
    expect(mainIndex).toBeGreaterThanOrEqual(0);
    const mainBody = script.slice(mainIndex);
    const requireIndex = mainBody.indexOf('requireLiveEnv();');
    const verifyIndex = mainBody.indexOf('verifyTerminal(terminal)');
    expect(requireIndex).toBeGreaterThanOrEqual(0);
    expect(verifyIndex).toBeGreaterThan(requireIndex);
    expect(mainBody.slice(0, requireIndex)).not.toContain('await ');
    expect(script).toContain('process.env[LIVE_CONFIRM_ENV] !== LIVE_CONFIRM_VALUE');
    expect(script).toContain('missing.push(`${LIVE_CONFIRM_ENV}=${LIVE_CONFIRM_VALUE}`)');
  });

  it('keeps the live verifier read-only after the terminal login request', () => {
    const methods = [...script.matchAll(/method:\s*'([A-Z]+)'/g)].map((match) => match[1]);
    expect(methods).toEqual(['POST', 'GET', 'GET', 'GET']);
    expect(script).toContain("requestJson(`/${terminal}/login`");
    expect(script).toContain('READ_ENDPOINTS');
    expect(script).toContain("'/accounts'");
    expect(script).toContain("'/depts'");
    expect(script).toContain("'/roles'");
    expect(script).toContain("'/account/sessions?pageNum=1&pageSize=5'");
    expect(script).not.toContain("method: 'PUT'");
    expect(script).not.toContain("method: 'DELETE'");
    expect(script).not.toContain("method: 'PATCH'");
    expect(script).not.toContain('/account/password');
    expect(script).not.toContain('createAccount');
    expect(script).not.toContain('updateAccount');
    expect(script).not.toContain('deleteDept');
  });

  it('keeps the portal login live response contract whitelisted', () => {
    for (const allowedField of [
      'token',
      'terminal',
      'subjectNo',
      'username',
      'nickName',
      'expireMinutes',
      'expireTime',
    ]) {
      expect(script).toContain(`'${allowedField}'`);
    }
    expect(script).toContain('ALLOWED_PORTAL_LOGIN_RESULT_FIELDS');
    expect(script).toContain('assertNoUnexpectedFields');
    expect(script).toContain('assertPortalLoginResultContract(terminal, data)');
    expect(script).toContain('exposed unexpected response fields');
    expect(script).toContain('login returned invalid expireMinutes');
    expect(script).toContain('login returned invalid expireTime');
  });

  it('keeps the portal getInfo live response contract whitelisted', () => {
    for (const allowedField of ['subjectNo', 'userName', 'nickName', 'roles', 'permissions']) {
      expect(script).toContain(`'${allowedField}'`);
    }
    expect(script).toContain('ALLOWED_PORTAL_GET_INFO_FIELDS');
    expect(script).toContain('assertPortalGetInfoContract(terminal, info)');
    expect(script).toContain('assertNoUnexpectedFields(`${terminal} getInfo`, data');
  });

  it('checks exact self-management permissions and frozen business surfaces', () => {
    expect(new Set(extractTerminalPermissions(script, 'seller'))).toEqual(
      new Set(EXPECTED_SELF_MANAGEMENT_PERMISSIONS.seller),
    );
    expect(new Set(extractTerminalPermissions(script, 'buyer'))).toEqual(
      new Set(EXPECTED_SELF_MANAGEMENT_PERMISSIONS.buyer),
    );
    for (const frozenTerm of [
      'product',
      'order',
      'inventory',
      'logistics',
      'finance',
      'fulfillment',
      'integration',
    ]) {
      expect(script).toContain(`'${frozenTerm}'`);
    }
    expect(script).toContain('serialized.includes(`${terminal}:${term}:`)');
    expect(script).toContain("serialized.includes(`/${term}/`)");
    expect(script).toContain('assertSelfManagementPermissions(terminal, info.permissions)');
    expect(script).toContain("assertNoFrozenBusinessSurface(terminal, 'getInfo', info)");
    expect(script).toContain('assertSelfManagementRouters(terminal, routers)');
    expect(script).toContain("assertNoFrozenBusinessSurface(terminal, 'getRouters', routers)");
    expect(script).toContain('assertNoFrozenBusinessSurface(terminal, endpoint, data)');
  });

  it('checks self-audit read DTOs do not expose internal audit fields', () => {
    for (const endpointPrefix of [
      '/account/login-logs',
      '/account/oper-logs',
      '/account/sessions',
    ]) {
      expect(script).toContain(`'${endpointPrefix}'`);
    }
    for (const field of [
      'subjectId',
      'accountId',
      'directLoginTicketId',
      'actingAdminId',
      'actingAdminName',
      'directLoginReason',
      'operParam',
      'jsonResult',
      'tokenId',
    ]) {
      expect(script).toContain(`'${field}'`);
    }
    expect(script).toContain('function assertSelfAuditDto');
    expect(script).toContain('FORBIDDEN_SELF_AUDIT_FIELDS.filter');
    expect(script).toContain('isSelfAuditEndpoint(endpoint)');
    expect(script).toContain('assertSelfAuditDto(`${terminal} ${endpoint}`, data)');
  });

  it('checks live getRouters contains only terminal self-management route permissions', () => {
    expect(script).toContain('flattenRouters');
    expect(script).toContain("const homePerm = `${terminal}:portal:home`");
    expect(script).toContain('getRouters did not include ${homePerm}');
    expect(script).toContain('portal home route path is not terminal-scoped');
    expect(script).toContain('portal home route component is not terminal-scoped');
    expect(script).toContain('getRouters exposes wildcard permission');
    expect(script).toContain('getRouters exposes cross-terminal permission');
    expect(script).toContain('getRouters exposes admin permission');
    expect(script).toContain('getRouters exposes non self-management permission');
  });

  it('checks live role menu templates stay in the terminal id range and self-management size', () => {
    expect(script).toContain("if (endpoint === '/roles/menus')");
    expect(script).toContain('assertTerminalRoleMenuTemplate(terminal, data)');
    expect(script).toContain('function collectTreeIds');
    expect(script).toContain("terminal === 'seller' ? [100000, 200000] : [200000, 300000]");
    expect(script).toContain('role menu template is empty');
    expect(script).toContain('role menu template contains duplicate ids');
    expect(script).toContain('role menu template has cross-terminal ids');
    expect(script).toContain('role menu template is not exactly self-management');
    expect(script).toContain('SELF_MANAGEMENT_PERMISSIONS[terminal].length');
  });

  it('checks that seller and buyer tokens are rejected by the opposite terminal', () => {
    expect(script).toContain("assertCrossTerminalTokenRejected('seller', 'buyer', tokens.seller)");
    expect(script).toContain("assertCrossTerminalTokenRejected('buyer', 'seller', tokens.buyer)");
    expect(script).toContain("for (const endpoint of ['getInfo', 'getRouters'])");
    expect(script).toContain('throw new Error(`${sourceTerminal} token was accepted by ${targetTerminal} ${endpoint}`)');
  });

  it('checks that anonymous portal getInfo requests are rejected before login', () => {
    expect(script).toContain('assertAnonymousPortalRequestRejected');
    expect(script).toContain("for (const endpoint of ['getInfo', 'getRouters'])");
    expect(script).toContain('anonymous ${endpoint} request was accepted');
    expect(script).toContain('await assertAnonymousPortalRequestRejected(terminal)');
  });
});
