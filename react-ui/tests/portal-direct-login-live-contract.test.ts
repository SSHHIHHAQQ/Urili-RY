import fs from 'fs';
import path from 'path';

const uiRoot = path.resolve(__dirname, '..');

function readUiSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
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
      'SELLER_DIRECT_LOGIN_SUBJECT_ID',
      'BUYER_DIRECT_LOGIN_SUBJECT_ID',
    ]) {
      expect(script).toContain(envName);
    }
    expect(script).toContain('base URL is the backend');
    expect(script).toContain('PORTAL_LIVE_API_PREFIX=/api');
  });

  it('does not read local secrets, datasource env, or admin passwords', () => {
    expect(script).toContain('It does not read .env.local');
    expect(script).not.toContain('readFile');
    expect(script).not.toContain("from 'fs'");
    expect(script).not.toContain('dotenv');
    expect(script).not.toContain('RUOYI_DB_');
    expect(script).not.toContain('RUOYI_REDIS_');
    expect(script).not.toContain('ADMIN_PASSWORD');
    expect(script).not.toContain('ADMIN_USERNAME');
  });

  it('touches only admin direct-login, portal direct-login, getInfo, and logout paths', () => {
    for (const pathFragment of [
      '/seller/admin/sellers/${subjectId}/directLogin',
      '/buyer/admin/buyers/${subjectId}/directLogin',
      '/${terminal}/direct-login',
      '/${terminal}/getInfo',
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
    expect(script).not.toContain('/accounts/');
    expect(script).not.toContain('/roles/');
    expect(script).not.toContain('/depts/');
  });

  it('consumes one-time tickets, rejects ticket reuse, and verifies exact portal self-management permissions', () => {
    expect(script).toContain('createDirectLoginTicket');
    expect(script).toContain('consumeDirectLoginTicket');
    expect(script).toContain('assertDirectLoginTicketCannotBeReused');
    expect(script).toContain('directLoginToken: ticket.token');
    expect(script).toContain('one-time direct-login token was accepted twice');
    expect(script).toContain('assertExactSelfManagementPermissions(terminal, info.permissions)');
    expect(script).toContain("data.loginUrl.includes(`/${terminal}/direct-login`)");
    expect(new Set(extractTerminalPermissions(script, 'seller'))).toEqual(
      new Set(EXPECTED_SELF_MANAGEMENT_PERMISSIONS.seller),
    );
    expect(new Set(extractTerminalPermissions(script, 'buyer'))).toEqual(
      new Set(EXPECTED_SELF_MANAGEMENT_PERMISSIONS.buyer),
    );
  });
});
