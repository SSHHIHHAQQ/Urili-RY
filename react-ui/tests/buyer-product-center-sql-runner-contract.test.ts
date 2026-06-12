import { spawnSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const uiRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(uiRoot, '..');
const runnerPath = path.join(repoRoot, 'scripts', 'buyer-product-center-sql-runner.mjs');

function readRepoSource(relativePath: string) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8');
}

describe('buyer product center SQL runner contract', () => {
  const script = readRepoSource('scripts/buyer-product-center-sql-runner.mjs');
  const manifest = JSON.parse(readRepoSource('react-ui/tests/three-terminal.manifest.json'));

  it('keeps the buyer product center runner explicit and covered by the three-terminal gate', () => {
    expect(fs.existsSync(runnerPath)).toBe(true);
    expect(manifest.frontendTestPaths).toContain(
      'tests/buyer-product-center-sql-runner-contract.test.ts',
    );
    expect(manifest.criticalFrontendExplicitTestPaths).toContain(
      'tests/buyer-product-center-sql-runner-contract.test.ts',
    );
  });

  it('defaults to read-only precheck and requires explicit apply confirmation', () => {
    expect(script).toContain("const confirmEnv = 'BUYER_PRODUCT_CENTER_SQL_CONFIRM'");
    expect(script).toContain("const confirmValue = 'APPLY_BUYER_PRODUCT_CENTER_MENU_SEED'");
    expect(script).toContain("const apply = args.has('--apply')");
    expect(script).toContain("const precheck = args.has('--precheck') || !apply");
    expect(script).toContain('Apply mode requires ${confirmEnv}=${confirmValue}');
    expect(script).toContain('connection.setReadOnly(!apply)');
    expect(script).toContain('connection.rollback()');
    expect(script).toContain('set @confirm_buyer_product_center_menu_seed = ?');
    expect(script).toContain('confirm.setString(1, CONFIRM_VALUE)');
    expect(script).toContain('assertPostcheck(counts)');
  });

  it('runs only the guarded buyer product center seed and verifies exact postcheck state', () => {
    expect(script).toContain('20260612_buyer_product_center_menu_seed.sql');
    expect(script).toContain('splitSqlStatements');
    expect(script).toContain('delimiter ');
    expect(script).toContain('buyer_product_center_page_menu", 1');
    expect(script).toContain('buyer_product_center_query_permission", 1');
    expect(script).toContain('buyer_owner_product_center_grants", buyerOwnerRoles * 2');
    expect(script).toContain('buyer_invalid_menu_perms", 0');
    expect(script).toContain('buyer_menu_id_range_violations", 0');
    expect(script).toContain('buyer_invalid_page_components", 0');
    expect(script).toContain('buyer_duplicate_menu_perms", 0');
    expect(script).toContain('postcheck exact buyer product center permission state verified.');
    expect(script).not.toContain('20260610_portal_self_management_permission_seed.sql');
    expect(script).not.toContain('20260612_fee_estimate_menu_seed.sql');
    expect(script).not.toContain('20260612_upstream_sync_task_lifecycle.sql');
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

  it('fails closed before connecting when apply confirmation is absent', () => {
    const result = spawnSync('node', [runnerPath, '--apply'], {
      cwd: repoRoot,
      encoding: 'utf8',
      env: {
        ...process.env,
        BUYER_PRODUCT_CENTER_SQL_CONFIRM: '',
      },
    });

    expect(result.status).not.toBe(0);
    expect(`${result.stdout}${result.stderr}`).toContain(
      'Apply mode requires BUYER_PRODUCT_CENTER_SQL_CONFIRM=APPLY_BUYER_PRODUCT_CENTER_MENU_SEED',
    );
  });
});
