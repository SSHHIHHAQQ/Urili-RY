import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function expectPureDefaultReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export { default } from '${target}';`);
}

function expectPureNamedReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export * from '${target}';`);
}

describe('warehouse admin permission guard', () => {
  it('keeps warehouse js mirrors as pure TS re-exports', () => {
    expectPureDefaultReExport('src/pages/Warehouse/WarehouseManagementPage.js', './WarehouseManagementPage.tsx');
    expectPureNamedReExport('src/pages/Warehouse/constants.js', './constants.ts');
    expectPureDefaultReExport('src/pages/Warehouse/Official/index.js', './index.tsx');
    expectPureDefaultReExport('src/pages/Warehouse/ThirdParty/index.js', './index.tsx');
    expectPureDefaultReExport(
      'src/pages/Warehouse/components/OfficialSyncModal.js',
      './OfficialSyncModal.tsx',
    );
    expectPureDefaultReExport('src/pages/Warehouse/components/WarehouseFields.js', './WarehouseFields.tsx');
    expectPureDefaultReExport('src/pages/Warehouse/components/WarehouseFormModal.js', './WarehouseFormModal.tsx');
    expectPureDefaultReExport(
      'src/pages/Warehouse/components/WarehousePairingModal.js',
      './WarehousePairingModal.tsx',
    );
    expectPureNamedReExport('src/services/warehouse/warehouse.js', './warehouse.ts');
  });

  it('keeps warehouse services on the admin namespace', () => {
    const service = readSource('src/services/warehouse/warehouse.ts');

    expect(service).toContain("const baseUrl = '/api/warehouse/admin';");
    expect(service).toContain('getOfficialWarehouseList');
    expect(service).toContain('getThirdPartyWarehouseList');
    expect(service).toContain('getWarehouseCurrencyOptions');
    expect(service).toContain('getWarehouseSellerOptions');
    expect(service).toContain('syncOfficialWarehouse');
    expect(service).toContain('pairOfficialWarehouse');
    expect(service).not.toContain('/api/seller/');
    expect(service).not.toContain('/api/buyer/');
  });

  it('keeps warehouse page permissions aligned with backend permissions', () => {
    const page = readSource('src/pages/Warehouse/WarehouseManagementPage.tsx');

    for (const permission of [
      'warehouse:official:list',
      'warehouse:official:add',
      'warehouse:official:edit',
      'warehouse:official:status',
      'warehouse:official:sync',
      'warehouse:thirdParty:list',
      'warehouse:thirdParty:add',
      'warehouse:thirdParty:edit',
      'warehouse:thirdParty:status',
    ]) {
      expect(page).toContain(permission);
    }

    expect(page).toContain('const canList = access.hasPerms(permissions.list);');
    expect(page).toContain('if (!canList)');
    expect(page).toContain('getDictSelectOption');
    expect(page).toContain('getWarehouseCurrencyOptions');
    expect(page).toContain('getWarehouseSellerOptions');
    expect(page).toContain('data: [],');
    expect(page).toContain('total: 0,');
    expect(page).toContain('access.hasPerms(permissions.add)');
    expect(page).toContain('access.hasPerms(permissions.edit)');
    expect(page).toContain('access.hasPerms(permissions.status)');
    expect(page).toContain('access.hasPerms(permissionMap.official.sync)');
  });

  it('keeps warehouse tests manifest-owned and critical to the three-terminal gate', () => {
    const manifest = readSource('tests/three-terminal.manifest.json');
    const verifier = readSource('scripts/verify-three-terminal.mjs');

    expect(manifest).toContain('"WarehouseAdminRouteContractTest"');
    expect(manifest).toContain('"tests/warehouse-permission-guard.test.ts"');
    expect(verifier).toContain('warehouse[\\\\/]src[\\\\/]test[\\\\/]java');
    expect(verifier).toMatch(/source-warehouse\|warehouse\|finance/);
  });
});
