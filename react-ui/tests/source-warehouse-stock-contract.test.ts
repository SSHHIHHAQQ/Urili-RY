import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function expectPureSource(relativePath: string, expected: string) {
  expect(readSource(relativePath).trim()).toBe(expected);
}

describe('source warehouse stock contract', () => {
  it('keeps source warehouse stock JS mirrors as pure TS/TSX re-exports', () => {
    expectPureSource('src/services/integration/sourceWarehouseStock.js', "export * from './sourceWarehouseStock.ts';");
    expectPureSource('src/pages/Inventory/SourceWarehouseStock/index.js', "export { default } from './index.tsx';");
  });

  it('keeps source warehouse stock on admin API and fail-closed frontend permission', () => {
    const service = readSource('src/services/integration/sourceWarehouseStock.ts');
    const page = readSource('src/pages/Inventory/SourceWarehouseStock/index.tsx');

    expect(service).toContain("const baseUrl = '/api/integration/admin/source-warehouse-stocks';");
    expect(service).toContain('`${baseUrl}/groups/list`');
    expect(service).toContain('`${baseUrl}/groups/detail`');
    expect(service).toContain('`${baseUrl}/options/master-warehouses`');
    expect(service).toContain('`${baseUrl}/options/source-warehouses`');
    expect(page).toContain("const access = useAccess();");
    expect(page).toContain("const canListSourceWarehouseStock = access.hasPerms('inventory:sourceWarehouse:list');");
    expect(page).toContain('if (!canListSourceWarehouseStock) {');
    expect(page).toContain('return { data: [], total: 0, success: true };');
    expect(page).toContain('canListSourceWarehouseStock={canListSourceWarehouseStock}');
  });

  it('keeps source warehouse stock tests in the three-terminal gate', () => {
    const manifest = JSON.parse(readSource('tests/three-terminal.manifest.json'));
    const verifier = readSource('scripts/verify-three-terminal.mjs');

    expect(manifest.frontendTestPaths).toContain('tests/source-warehouse-stock-contract.test.ts');
    expect(verifier).toContain('source-warehouse');
  });
});
