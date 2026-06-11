import fs from 'fs';
import path from 'path';

const uiRoot = path.resolve(__dirname, '..');

function readUiSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

describe('portal self-management live write verifier contract', () => {
  const script = readUiSource('scripts/verify-portal-self-management-live-write.mjs');
  const packageJson = JSON.parse(readUiSource('package.json'));
  const manifest = JSON.parse(readUiSource('tests/three-terminal.manifest.json'));

  it('keeps the live write verifier exposed and covered by the three-terminal gate', () => {
    expect(packageJson.scripts['verify:portal-self-management-live-write']).toBe(
      'node scripts/verify-portal-self-management-live-write.mjs',
    );
    expect(manifest.frontendTestPaths).toContain(
      'tests/portal-self-management-live-write-contract.test.ts',
    );
    expect(manifest.criticalFrontendExplicitTestPaths).toContain(
      'tests/portal-self-management-live-write-contract.test.ts',
    );
  });

  it('requires an explicit write confirmation and does not read local secrets or data source env', () => {
    expect(script).toContain("const WRITE_CONFIRM_ENV = 'PORTAL_LIVE_WRITE_CONFIRM'");
    expect(script).toContain(
      "const WRITE_CONFIRM_VALUE = 'APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY'",
    );
    expect(script).toContain('process.env[WRITE_CONFIRM_ENV] !== WRITE_CONFIRM_VALUE');
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
    expect(script).toContain('does not read .env.local');
    expect(script).not.toContain('readFile');
    expect(script).not.toContain("from 'fs'");
    expect(script).not.toContain('dotenv');
    expect(script).not.toContain('RUOYI_DB_');
    expect(script).not.toContain('RUOYI_REDIS_');
    expect(script).toContain("process.env.PORTAL_LIVE_API_PREFIX || ''");
    expect(script).toContain('The default base URL is the backend service on 8080');
    expect(script).toContain('PORTAL_LIVE_API_PREFIX=/api only when targeting the React dev proxy');
    expect(script).toContain('apiPath(path)');
    expect(script).not.toContain('requestJson(`/api/');
  });

  it('gates live writes before any asynchronous network side effect', () => {
    const mainMatch = script.match(/async function main\(\) \{([\s\S]*?)\n\}\n\nmain\(\)\.catch/);
    expect(mainMatch).not.toBeNull();
    const mainBody = mainMatch?.[1] || '';
    const requireIndex = mainBody.indexOf('requireLiveEnv();');
    const verifyIndex = mainBody.indexOf('verifyTerminalWrites(terminal)');
    expect(requireIndex).toBeGreaterThan(-1);
    expect(verifyIndex).toBeGreaterThan(requireIndex);
    expect(mainBody.slice(0, requireIndex)).not.toContain('await ');
    expect(mainBody.slice(0, requireIndex)).not.toContain('requestJson(');
    expect(mainBody.slice(0, requireIndex)).not.toContain('login(');
    expect(mainBody.slice(0, requireIndex)).not.toContain('authorizedRequest(');
    expect(mainBody.slice(0, requireIndex)).not.toContain('cleanupTerminalWrites(');

    const requireLiveEnvMatch = script.match(
      /function requireLiveEnv\(\) \{([\s\S]*?)\n\}\n\nasync function requestJson/,
    );
    expect(requireLiveEnvMatch).not.toBeNull();
    const requireLiveEnvBody = requireLiveEnvMatch?.[1] || '';
    expect(requireLiveEnvBody).toContain('process.env[WRITE_CONFIRM_ENV] !== WRITE_CONFIRM_VALUE');
    expect(requireLiveEnvBody).toContain('missing.push(`${WRITE_CONFIRM_ENV}=${WRITE_CONFIRM_VALUE}`)');
  });

  it('limits writes to the current portal self-management surface', () => {
    for (const pathFragment of [
      '/accounts',
      '/accounts/${state.accountId}/roles',
      '/depts',
      '/roles',
      '/roles/menus',
      '/account/login-logs?pageNum=1&pageSize=5',
      '/account/oper-logs?pageNum=1&pageSize=5',
      '/account/sessions?pageNum=1&pageSize=5',
    ]) {
      expect(script).toContain(pathFragment);
    }
    expect(script).not.toContain('/admin/');
    expect(script).not.toContain('/direct-login');
    expect(script).not.toContain('/product/');
    expect(script).not.toContain('/order/');
    expect(script).not.toContain('/inventory/');
    expect(script).not.toContain('/logistics/');
    expect(script).not.toContain('/finance/');
    expect(script).not.toContain('/fulfillment/');
    expect(script).not.toContain('/integration/');
  });

  it('creates only disabled STAFF evidence accounts and cleans removable test data', () => {
    expect(script).toContain("accountRole: 'STAFF'");
    expect(script).toContain("status: '1'");
    expect(script).toContain('because the portal self-management surface intentionally has no account delete');
    expect(script).toContain('cleanupTerminalWrites');
    expect(script).toContain('clear account roles');
    expect(script).toContain('disable and detach account');
    expect(script).toContain('delete role');
    expect(script).toContain('delete dept');
    expect(script).not.toContain("DELETE', `/accounts/");
  });

  it('verifies terminal-scoped menus, exact permissions, role assignment, and self-audit DTO redaction', () => {
    expect(script).toContain('assertPortalLoginResultContract(terminal, data)');
    expect(script).toContain('ALLOWED_PORTAL_LOGIN_RESULT_FIELDS');
    expect(script).toContain('assertPortalGetInfoContract(terminal, info)');
    expect(script).toContain('ALLOWED_PORTAL_GET_INFO_FIELDS');
    expect(script).toContain('assertExactSelfManagementPermissions(terminal, info.permissions)');
    expect(script).toContain('assertTerminalRoleMenuTemplate(terminal, menuIds)');
    expect(script).toContain('role menu template is not exactly self-management');
    expect(script).toContain('SELF_MANAGEMENT_PERMISSIONS[terminal].length');
    expect(script).toContain('crossTerminalMenuIdFor(terminal)');
    expect(script).toContain('assertRoleCreateRejectsInvalidMenuIds(terminal, token, marker, menuIds, crossTerminalMenuId)');
    expect(script).toContain('assertRoleUpdateRejectsInvalidMenuIds(');
    expect(script).toContain('role create accepted cross-terminal menu ids');
    expect(script).toContain('role update accepted cross-terminal menu ids');
    expect(script).toContain('rejected role write still persisted');
    expect(script).toContain('role update rejection mutated checkedKeys');
    expect(script).toContain("roleIds: [state.roleId]");
    expect(script).toContain('account role assignment was not persisted');
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
    expect(script).toContain('assertSelfAuditDto(`${terminal} login logs`');
    expect(script).toContain('assertSelfAuditDto(`${terminal} operation logs`');
    expect(script).toContain('assertSelfAuditDto(`${terminal} sessions`');
  });

  it('requires created account records to expose accountId without leaking subject scope', () => {
    expect(script).toContain('assertPortalAccountRecord(terminal, createdAccount)');
    expect(script).toContain('created account did not return a usable accountId');
    expect(script).toContain('created account leaked internal subject scope');
    expect(script).toContain('FORBIDDEN_PORTAL_ACCOUNT_FIELDS');
    expect(script).not.toContain('info.subjectId');
  });
});
