import { spawnSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const uiRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(uiRoot, '..');
const runnerPath = path.join(repoRoot, 'scripts', 'portal-self-management-sql-runner.mjs');

function readRepoSource(relativePath: string) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8');
}

describe('portal self-management SQL runner contract', () => {
  const script = readRepoSource('scripts/portal-self-management-sql-runner.mjs');
  const manifest = JSON.parse(readRepoSource('react-ui/tests/three-terminal.manifest.json'));

  it('keeps the SQL runner explicit and covered by the three-terminal gate', () => {
    expect(fs.existsSync(runnerPath)).toBe(true);
    expect(manifest.frontendTestPaths).toContain(
      'tests/portal-self-management-sql-runner-contract.test.ts',
    );
    expect(manifest.criticalFrontendExplicitTestPaths).toContain(
      'tests/portal-self-management-sql-runner-contract.test.ts',
    );
  });

  it('defaults to read-only precheck and requires explicit apply confirmation', () => {
    expect(script).toContain("const confirmEnv = 'PORTAL_SELF_MANAGEMENT_SQL_CONFIRM'");
    expect(script).toContain(
      "const confirmValue = 'APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED'",
    );
    expect(script).toContain("const apply = args.has('--apply')");
    expect(script).toContain("const precheck = args.has('--precheck') || !apply");
    expect(script).toContain('Apply mode requires ${confirmEnv}=${confirmValue}');
    expect(script).toContain('connection.setReadOnly(!apply)');
    expect(script).toContain('connection.rollback()');
    expect(script).toContain('set @confirm_portal_self_management_permission_seed = ?');
    expect(script).toContain('confirm.setString(1, CONFIRM_VALUE)');
    expect(script).toContain('assertPostcheck(counts)');
  });

  it('uses the current local datasource without printing secrets or adding a mysql npm dependency', () => {
    expect(script).toContain("'.env.local'");
    expect(script).toContain('RUOYI_DB_URL');
    expect(script).toContain('RUOYI_DB_USERNAME');
    expect(script).toContain('RUOYI_DB_PASSWORD');
    expect(script).toContain('findMysqlDriverJar');
    expect(script).toContain('mysql-connector-j');
    expect(script).toContain('mysql-connector-java');
    expect(script).not.toContain("import mysql");
    expect(script).not.toContain('mysql2');
    expect(script).not.toContain('console.log(env.RUOYI_DB_URL');
    expect(script).not.toContain('console.log(env.RUOYI_DB_PASSWORD');
  });

  it('parses MySQL delimiter blocks and runs the self-management seed path only', () => {
    expect(script).toContain('20260610_portal_self_management_permission_seed.sql');
    expect(script).toContain('splitSqlStatements');
    expect(script).toContain('delimiter ');
    expect(script).toContain('isExecutableStatement');
    expect(script).not.toContain('20260610_terminal_portal_home_menu_seed.sql');
    expect(script).not.toContain('product');
    expect(script).not.toContain('order');
    expect(script).not.toContain('inventory');
    expect(script).not.toContain('logistics');
    expect(script).not.toContain('finance');
    expect(script).not.toContain('fulfillment');
    expect(script).not.toContain('integration');
  });

  it('fails apply mode when the postcheck is not the exact self-management state', () => {
    for (const requiredSnippet of [
      'seller_required_self_menu_entries", 19',
      'buyer_required_self_menu_entries", 19',
      'seller_owner_self_grants", sellerOwnerRoles * 19',
      'buyer_owner_self_grants", buyerOwnerRoles * 19',
      'seller_owner_non_self_grants", 0',
      'buyer_owner_non_self_grants", 0',
      'seller_invalid_menu_perms", 0',
      'buyer_invalid_menu_perms", 0',
      'postcheck exact self-management permission state verified.',
    ]) {
      expect(script).toContain(requiredSnippet);
    }
    expect(script).toContain('postcheck did not return count for');
    expect(script).toContain('after seed apply, but was');
  });

  it('fails closed before connecting when apply confirmation is absent', () => {
    const result = spawnSync('node', [runnerPath, '--apply'], {
      cwd: repoRoot,
      encoding: 'utf8',
      env: {
        ...process.env,
        PORTAL_SELF_MANAGEMENT_SQL_CONFIRM: '',
      },
    });

    expect(result.status).not.toBe(0);
    expect(`${result.stdout}${result.stderr}`).toContain(
      'Apply mode requires PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED',
    );
  });
});
