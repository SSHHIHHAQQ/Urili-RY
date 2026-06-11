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

describe('portal direct-login live verifier contract', () => {
  const script = readUiSource('scripts/verify-portal-direct-login-live.mjs');
  const packageJson = JSON.parse(readUiSource('package.json'));
  const manifest = JSON.parse(readUiSource('tests/three-terminal.manifest.json'));

  it('keeps the direct-login live verifier exposed and covered by the three-terminal gate', () => {
    expect(packageJson.scripts['verify:portal-direct-login-live']).toBe(
      'node scripts/verify-portal-direct-login-live.mjs',
    );
    expect(manifest.frontendTestPaths).toContain('tests/portal-direct-login-live-contract.test.ts');
    expect(manifest.criticalFrontendExplicitTestPaths).toContain(
      'tests/portal-direct-login-live-contract.test.ts',
    );
  });

  it('requires explicit live confirmation and caller-provided admin auth context', () => {
    expect(script).toContain("const DIRECT_LOGIN_CONFIRM_ENV = 'PORTAL_DIRECT_LOGIN_LIVE_CONFIRM'");
    expect(script).toContain(
      "const DIRECT_LOGIN_CONFIRM_VALUE = 'APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY'",
    );
    expect(script).toContain(
      'process.env[DIRECT_LOGIN_CONFIRM_ENV] !== DIRECT_LOGIN_CONFIRM_VALUE',
    );
    for (const envName of [
      'PORTAL_LIVE_BASE_URL',
      'PORTAL_LIVE_API_PREFIX',
      'ADMIN_AUTH_TOKEN',
      'ADMIN_USERNAME',
      'ADMIN_PASSWORD',
      'SELLER_DIRECT_LOGIN_SUBJECT_ID',
      'BUYER_DIRECT_LOGIN_SUBJECT_ID',
    ]) {
      expect(script).toContain(envName);
    }
    expect(script).toContain('ADMIN_AUTH_TOKEN or ADMIN_USERNAME/ADMIN_PASSWORD');
    expect(script).toContain('base URL is the backend');
    expect(script).toContain('PORTAL_LIVE_API_PREFIX=/api');
    expect(script).toContain('uses the admin auth');
    expect(script).toContain('select the first active subject');
    expect(script).toContain('opposite terminal rejects the ticket');
    expect(script).toContain('opposite terminal rejects the');
    expect(script).toContain('consumed portal token');
    expect(script).toContain('getInfo/getRouters');
    expect(script).toContain('short-lived direct-login ticket');
    expect(script).toContain('does not expose internal ids or token');
  });

  it('keeps the runbook aligned with admin auth fallback and subject auto-discovery', () => {
    const runbook = readRepoSource(
      'docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md',
    );
    expect(runbook).toContain('管理端认证可用 `ADMIN_AUTH_TOKEN`');
    expect(runbook).toContain('`ADMIN_USERNAME` / `ADMIN_PASSWORD`');
    expect(runbook).toContain('目标主体 ID 可选');
    expect(runbook).toContain('自动发现 active OWNER 主体');
    expect(runbook).toContain("$env:ADMIN_AUTH_TOKEN = '<current-admin-token>'");
    expect(runbook).toContain("$env:ADMIN_USERNAME = '<admin username>'");
    expect(runbook).toContain("$env:ADMIN_PASSWORD = '<admin password>'");
    expect(runbook).toContain(
      "$env:PORTAL_DIRECT_LOGIN_LIVE_CONFIRM = 'APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY'",
    );
  });

  it('checks the live confirmation gate before any admin login or subject discovery can run', () => {
    const mainIndex = script.indexOf('async function main()');
    expect(mainIndex).toBeGreaterThanOrEqual(0);
    const mainBody = script.slice(mainIndex);
    const requireIndex = mainBody.indexOf('requireLiveEnv();');
    const verifyIndex = mainBody.indexOf('verifyTerminal(terminal, config)');
    expect(requireIndex).toBeGreaterThanOrEqual(0);
    expect(verifyIndex).toBeGreaterThan(requireIndex);
    expect(mainBody.slice(0, requireIndex)).not.toContain('await ');
    expect(script).toContain(
      'if (process.env[DIRECT_LOGIN_CONFIRM_ENV] !== DIRECT_LOGIN_CONFIRM_VALUE)',
    );
  });

  it('does not read local secrets or datasource env and does not print token material', () => {
    expect(script).toContain('It does not read .env.local');
    expect(script).not.toContain('readFile');
    expect(script).not.toContain("from 'fs'");
    expect(script).not.toContain('dotenv');
    expect(script).not.toContain('RUOYI_DB_');
    expect(script).not.toContain('RUOYI_REDIS_');
    expect(script).toContain('without printing the token');
    expect(script).not.toContain('console.log(issuedToken');
    expect(script).not.toContain('console.log(token');
  });

  it('touches only admin direct-login discovery, portal direct-login, getInfo, routers, and logout paths', () => {
    for (const pathFragment of [
      '/seller/admin/sellers/list?pageNum=1&pageSize=200',
      '/buyer/admin/buyers/list?pageNum=1&pageSize=200',
      '/seller/admin/sellers/${subjectId}/accounts',
      '/buyer/admin/buyers/${subjectId}/accounts',
      '/seller/admin/sellers/${subjectId}/directLogin',
      '/buyer/admin/buyers/${subjectId}/directLogin',
      '/${terminal}/direct-login',
      '/${terminal}/getInfo',
      '/${terminal}/getRouters',
      '/${terminal}/logout',
    ]) {
      expect(script).toContain(pathFragment);
    }
    expect(script).not.toContain('/product/');
    expect(script).not.toContain('/order/');
    expect(script).not.toContain('/inventory/');
    expect(script).not.toContain('/logistics/');
    expect(script).not.toContain('/finance/');
    expect(script).not.toContain('/fulfillment/');
    expect(script).not.toContain('/integration/');
    expect(script).not.toContain('/menus/');
    expect(script).not.toContain('/roles/');
    expect(script).not.toContain('/depts/');
  });

  it('can auto-discover active OWNER subjects through admin read APIs when subject ids are omitted', () => {
    expect(script).toContain('async function resolveDirectLoginSubjectId');
    expect(script).toContain('const explicitSubjectId = process.env[config.subjectEnv]');
    expect(script).toContain('return explicitSubjectId');
    expect(script).toContain('const list = await adminGet(config.listPath)');
    expect(script).toContain('const accounts = await adminGet(config.accountsPath(subjectId))');
    expect(script).toContain("String(account?.accountRole || '').toUpperCase() === 'OWNER'");
    expect(script).toContain('isActive(account?.status)');
    expect(script).toContain('isActive(account?.lockStatus)');
    expect(script).toContain('direct-login auto-selected subject');
    expect(script).toContain('could not auto-select an active subject with active OWNER account');
  });

  it('consumes one-time tickets, rejects ticket reuse, and verifies exact portal self-management permissions', () => {
    expect(script).toContain('createDirectLoginTicket');
    expect(script).toContain('assertDirectLoginResultContract(terminal, data)');
    expect(script).toContain('assertDirectLoginTicketRejectedByOtherTerminal');
    expect(script).toContain('consumeDirectLoginTicket');
    expect(script).toContain('assertDirectLoginTicketCannotBeReused');
    expect(script).toContain('assertDirectLoginTokenRejectedByOtherTerminal');
    expect(script).toContain('directLoginToken: ticket.token');
    expect(script).toContain('direct-login ticket was accepted by ${targetTerminal}');
    expect(script).toContain('direct-login token was accepted by ${targetTerminal} ${endpoint}');
    expect(script).toContain('one-time direct-login token was accepted twice');
    expect(script).toContain('assertExactSelfManagementPermissions(terminal, info.permissions)');
    expect(script).toContain("assertNoFrozenBusinessSurface(terminal, 'getInfo', info)");
    expect(script).toContain("assertNoFrozenBusinessSurface(terminal, 'getRouters', routers)");
    expect(script).toContain("data.loginUrl.includes(`/${terminal}/direct-login`)");
    expect(new Set(extractTerminalPermissions(script, 'seller'))).toEqual(
      new Set(EXPECTED_SELF_MANAGEMENT_PERMISSIONS.seller),
    );
    expect(new Set(extractTerminalPermissions(script, 'buyer'))).toEqual(
      new Set(EXPECTED_SELF_MANAGEMENT_PERMISSIONS.buyer),
    );
  });

  it('keeps admin direct-login live response short-lived and free of internal fields', () => {
    for (const allowedField of [
      'token',
      'ticketId',
      'loginUrl',
      'expireMinutes',
      'expireTime',
    ]) {
      expect(script).toContain(`'${allowedField}'`);
    }
    expect(script).toContain('ALLOWED_ADMIN_DIRECT_LOGIN_RESULT_FIELDS');
    expect(script).toContain('assertNoUnexpectedFields');
    expect(script).toContain('exposed unexpected response fields');
    expect(script).toContain("for (const forbidden of ['accountId', 'username', 'tokenHash'])");
    expect(script).toContain('admin direct-login leaked internal field');
    expect(script).toContain('data.expireMinutes <= 0 || data.expireMinutes > 30');
    expect(script).toContain('admin direct-login did not return expireTime');
    expect(script).toContain("data.loginUrl.includes('directLoginToken=')");
    expect(script).toContain("data.loginUrl.includes('token=')");
    expect(script).toContain('admin direct-login loginUrl leaked token material');
  });

  it('keeps consumed portal direct-login response whitelisted', () => {
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
    expect(script).toContain('assertPortalLoginResultContract(terminal, data)');
    expect(script).toContain('portal direct-login returned invalid expireMinutes');
    expect(script).toContain('portal direct-login returned invalid expireTime');
  });

  it('keeps consumed direct-login getInfo response whitelisted', () => {
    for (const allowedField of ['subjectNo', 'userName', 'nickName', 'roles', 'permissions']) {
      expect(script).toContain(`'${allowedField}'`);
    }
    expect(script).toContain('ALLOWED_PORTAL_GET_INFO_FIELDS');
    expect(script).toContain('assertPortalGetInfoContract(terminal, info)');
    expect(script).toContain('assertNoUnexpectedFields(`${terminal} direct-login getInfo`, data');
  });

  it('checks direct-login getRouters contains only terminal self-management route permissions', () => {
    expect(script).toContain('assertSelfManagementRouters(terminal, routers)');
    expect(script).toContain('flattenRouters');
    expect(script).toContain("const homePerm = `${terminal}:portal:home`");
    expect(script).toContain('direct-login getRouters did not include ${homePerm}');
    expect(script).toContain('direct-login portal home route path is not terminal-scoped');
    expect(script).toContain('direct-login portal home route component is not terminal-scoped');
    expect(script).toContain('direct-login getRouters exposes wildcard permission');
    expect(script).toContain('direct-login getRouters exposes cross-terminal permission');
    expect(script).toContain('direct-login getRouters exposes admin permission');
    expect(script).toContain('direct-login getRouters exposes non self-management permission');
  });

  it('checks consumed direct-login tokens are rejected by the opposite terminal', () => {
    expect(script).toContain('async function assertDirectLoginTokenRejectedByOtherTerminal');
    expect(script).toContain("for (const endpoint of ['getInfo', 'getRouters'])");
    expect(script).toContain('headers: { Authorization: `Bearer ${token}` }');
  });
});
