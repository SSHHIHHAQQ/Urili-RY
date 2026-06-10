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
    ]) {
      expect(script).toContain(envName);
    }
    expect(script).toContain('It does not read .env.local');
    expect(script).not.toContain('readFile');
    expect(script).not.toContain("from 'fs'");
    expect(script).not.toContain('require("fs")');
    expect(script).not.toContain("require('fs')");
    expect(script).not.toContain('dotenv');
    expect(script).not.toContain('RUOYI_DB_');
    expect(script).not.toContain('RUOYI_REDIS_');
    expect(script).toContain("process.env.PORTAL_LIVE_API_PREFIX || ''");
    expect(script).toContain('The default base URL is the backend service on 8080');
    expect(script).toContain('set PORTAL_LIVE_API_PREFIX=/api only when targeting the React dev proxy');
    expect(script).toContain('apiPath(path)');
    expect(script).not.toContain('requestJson(`/api/');
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

  it('checks exact self-management permissions and frozen business surfaces', () => {
    expect(new Set(extractTerminalPermissions(script, 'seller'))).toEqual(
      new Set(EXPECTED_SELF_MANAGEMENT_PERMISSIONS.seller),
    );
    expect(new Set(extractTerminalPermissions(script, 'buyer'))).toEqual(
      new Set(EXPECTED_SELF_MANAGEMENT_PERMISSIONS.buyer),
    );
    for (const frozenPart of [
      ':product:',
      ':order:',
      ':inventory:',
      ':logistics:',
      ':finance:',
      ':fulfillment:',
      ':integration:',
    ]) {
      expect(script).toContain(`'${frozenPart}'`);
    }
    expect(script).toContain('assertSelfManagementPermissions(terminal, info.permissions)');
    expect(script).toContain("assertNoFrozenBusinessSurface(terminal, 'getInfo', info)");
    expect(script).toContain("assertNoFrozenBusinessSurface(terminal, 'getRouters', routers)");
    expect(script).toContain('assertNoFrozenBusinessSurface(terminal, endpoint, data)');
  });

  it('checks that seller and buyer tokens are rejected by the opposite terminal', () => {
    expect(script).toContain("assertCrossTerminalTokenRejected('seller', 'buyer', tokens.seller)");
    expect(script).toContain("assertCrossTerminalTokenRejected('buyer', 'seller', tokens.buyer)");
    expect(script).toContain('throw new Error(`${sourceTerminal} token was accepted by ${targetTerminal} getInfo`)');
  });

  it('checks that anonymous portal getInfo requests are rejected before login', () => {
    expect(script).toContain('assertAnonymousPortalRequestRejected');
    expect(script).toContain('anonymous getInfo request was accepted');
    expect(script).toContain('await assertAnonymousPortalRequestRejected(terminal)');
  });
});
