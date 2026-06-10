import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function expectPureSource(relativePath: string, expected: string) {
  expect(readSource(relativePath).trim()).toBe(expected);
}

describe('inventory overview contract', () => {
  it('keeps inventory overview JS mirrors as pure TS/TSX re-exports', () => {
    expectPureSource('src/services/inventory/overview.js', "export * from './overview.ts';");
    expectPureSource('src/pages/Inventory/Overview/index.js', "export { default } from './index.tsx';");
    expectPureSource('src/pages/Inventory/Overview/helpers.js', "export * from './helpers.tsx';");
    expectPureSource(
      'src/pages/Inventory/Overview/components/QuantityCell.js',
      "export { default } from './QuantityCell.tsx';",
    );
    expectPureSource(
      'src/pages/Inventory/Overview/components/InventoryAdjustButton.js',
      "export { default } from './InventoryAdjustButton.tsx';",
    );
    expectPureSource(
      'src/components/InventoryAdjust/InventoryAdjustButton.js',
      "export { default, InventoryAdjustModal } from './InventoryAdjustButton.tsx';",
    );
    expectPureSource(
      'src/components/InventorySyncPolicy/InventorySyncPolicyButton.js',
      "export { default, InventorySyncPolicyModal } from './InventorySyncPolicyButton.tsx';",
    );
    expectPureSource(
      'src/components/InventorySyncPolicy/InventorySyncPolicyTargetPicker.js',
      "export { default } from './InventorySyncPolicyTargetPicker.tsx';",
    );
    expectPureSource(
      'src/pages/Inventory/Overview/components/SkuWarehouseTable.js',
      "export { default, WarehouseStockTable } from './SkuWarehouseTable.tsx';",
    );
    expectPureSource(
      'src/pages/Inventory/Overview/components/SpuSkuWarehouseTable.js',
      "export { default } from './SpuSkuWarehouseTable.tsx';",
    );
    expectPureSource(
      'src/pages/Inventory/Overview/components/WarehouseViewTable.js',
      "export { default } from './WarehouseViewTable.tsx';",
    );
  });

  it('keeps inventory overview on admin APIs and admin permissions', () => {
    const baseUrlToken = ['$', '{baseUrl}'].join('');
    const apiUrl = (pathName: string) => ['`', baseUrlToken, pathName, '`'].join('');
    const service = readSource('src/services/inventory/overview.ts');
    const page = readSource('src/pages/Inventory/Overview/index.tsx');
    const warehouseView = readSource('src/pages/Inventory/Overview/components/WarehouseViewTable.tsx');
    const skuWarehouse = readSource('src/pages/Inventory/Overview/components/SkuWarehouseTable.tsx');
    const spuSkuWarehouse = readSource('src/pages/Inventory/Overview/components/SpuSkuWarehouseTable.tsx');
    const quantityCell = readSource('src/pages/Inventory/Overview/components/QuantityCell.tsx');
    const adjustButtonWrapper = readSource('src/pages/Inventory/Overview/components/InventoryAdjustButton.tsx');
    const adjustButton = readSource('src/components/InventoryAdjust/InventoryAdjustButton.tsx');
    const syncPolicyButton = readSource('src/components/InventorySyncPolicy/InventorySyncPolicyButton.tsx');
    const syncPolicyTargetPicker = readSource('src/components/InventorySyncPolicy/InventorySyncPolicyTargetPicker.tsx');

    expect(service).toContain("const baseUrl = '/api/inventory/admin/overview';");
    expect(service).toContain(apiUrl('/spu/list'));
    expect(service).toContain(apiUrl('/sku/list'));
    expect(service).toContain(apiUrl('/warehouse/list'));
    expect(service).toContain(apiUrl('/seller/options'));
    expect(service).toContain(apiUrl('/official-warehouse/options'));
    expect(service).toContain(apiUrl('/adjust/preview'));
    expect(service).toContain(apiUrl('/adjust/confirm'));
    expect(service).toContain('function previewInventoryOverviewBatchAdjust');
    expect(service).toContain('function confirmInventoryOverviewBatchAdjust');
    expect(service).toContain(apiUrl('/adjust/batch-preview'));
    expect(service).toContain(apiUrl('/adjust/batch-confirm'));
    expect(service).toContain('function previewInventoryOverviewSyncPolicy');
    expect(service).toContain('function confirmInventoryOverviewSyncPolicy');
    expect(service).toContain(apiUrl('/sync-policy/preview'));
    expect(service).toContain(apiUrl('/sync-policy/confirm'));
    expect(page).toContain("const canListInventoryOverview = access.hasPerms('inventory:overview:list')");
    expect(page).toContain("const canQueryInventoryOverview = access.hasPerms('inventory:overview:query')");
    expect(page).toContain(
      "const canAdjustInventoryOverview = access.hasPerms('inventory:overview:adjust') && canQueryInventoryOverview",
    );
    expect(page).toContain(
      "const canSyncInventoryOverview = access.hasPerms('inventory:overview:syncPolicy') && canQueryInventoryOverview",
    );
    expect(page).toContain('canAdjust={canAdjustInventoryOverview}');
    expect(page).toContain('canSync={canSyncInventoryOverview}');
    expect(page).toContain('canQuery={canQueryInventoryOverview}');
    expect(page).toContain('getInventoryOverviewSellerOptions');
    expect(page).toContain('getInventoryOverviewOfficialWarehouseOptions');
    expect(page).toContain("dataIndex: 'sellerId'");
    expect(page).toContain("dataIndex: 'syncModeSummary'");
    expect(page).toContain('InventorySyncPolicyButton');
    expect(warehouseView).toContain('getInventoryOverviewWarehouseList');
    expect(warehouseView).toContain('canQuery: boolean;');
    expect(warehouseView).toContain("dataIndex: 'sellerId'");
    expect(warehouseView).toContain("dataIndex: 'syncMode'");
    expect(warehouseView).toContain('InventorySyncPolicyButton');
    expect(warehouseView).toMatch(/if \(!canQuery\) \{[\s\S]*?return \{ data: \[\], total: 0, success: true \};[\s\S]*?\}/);
    expect(skuWarehouse).toContain('getInventoryOverviewWarehouses(skuId)');
    expect(skuWarehouse).toContain('InventoryAdjustButton');
    expect(skuWarehouse).toContain('InventorySyncPolicyButton');
    expect(spuSkuWarehouse).toContain('flattenSkuWarehouseGroups');
    expect(spuSkuWarehouse).toContain('showSkuColumn');
    expect(spuSkuWarehouse).toContain('showSkuAdjust');
    expect(spuSkuWarehouse).not.toContain('spuSkuWarehouseList');
    expect(quantityCell).toContain('previewInventoryOverviewAdjust({');
    expect(quantityCell).toContain('confirmInventoryOverviewAdjust({');
    expect(quantityCell).not.toContain("message.warning('请填写库存调整原因')");
    expect(quantityCell).toContain("placeholder=\"调整原因（选填）\"");
    expect(quantityCell).not.toContain('reason: preview?.message');
    expect(adjustButtonWrapper.trim()).toBe("export { default } from '@/components/InventoryAdjust/InventoryAdjustButton';");
    expect(adjustButton).toContain('previewInventoryOverviewBatchAdjust');
    expect(adjustButton).toContain('confirmInventoryOverviewBatchAdjust');
    expect(adjustButton).toContain("scope: AdjustScope");
    expect(adjustButton).toContain('effectiveSourceAvailable(record)');
    expect(adjustButton).toContain("record.syncMode !== 'AUTO_SOURCE_AVAILABLE'");
    expect(adjustButton).not.toContain("message.warning('请填写库存调整原因')");
    expect(adjustButton).toContain("placeholder=\"调整原因（选填）\"");
    expect(syncPolicyButton).toContain('previewInventoryOverviewSyncPolicy');
    expect(syncPolicyButton).toContain('confirmInventoryOverviewSyncPolicy');
    expect(syncPolicyButton).toContain('自动同步WMS库存设置');
    expect(syncPolicyButton).toContain('卖家维度');
    expect(syncPolicyButton).toContain('buttonStyle="solid"');
    expect(syncPolicyButton).toContain('mode="multiple"');
    expect(syncPolicyButton).toContain('InventorySyncPolicyTargetPicker');
    expect(syncPolicyTargetPicker).toContain('ProTable');
    expect(syncPolicyTargetPicker).toContain("type: 'radio'");
    expect(syncPolicyTargetPicker).toContain('getInventoryOverviewSpuList');
    expect(syncPolicyTargetPicker).toContain('getInventoryOverviewSkuList');
    expect(syncPolicyTargetPicker).toContain('getInventoryOverviewWarehouseList');
    expect(syncPolicyButton).not.toContain('InputNumber');
  });

  it('keeps inventory overview tests in the three-terminal gate', () => {
    const manifest = JSON.parse(readSource('tests/three-terminal.manifest.json'));
    const verifier = readSource('scripts/verify-three-terminal.mjs');

    expect(manifest.backendTestClasses).toContain('InventoryOverviewRefreshContractTest');
    expect(manifest.criticalBackendExplicitTestClasses).toContain('InventoryOverviewRefreshContractTest');
    expect(manifest.frontendTestPaths).toContain('tests/inventory-overview-contract.test.ts');
    expect(verifier).toContain('inventory[\\\\/]src[\\\\/]test[\\\\/]java');
    expect(verifier).toContain('inventory-overview');
  });

  it('keeps inventory overview backend manual adjustment reason optional', () => {
    const service = fs.readFileSync(
      path.resolve(uiRoot, '..', 'RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java'),
      'utf8',
    );

    expect(service).not.toContain('throw new ServiceException("库存调整原因不能为空")');
    expect(service).toContain('request.setReason(trimReason(request.getReason()))');
    expect(service).toContain('return trimmed.isEmpty() ? null : trimmed');
    expect(service).toContain('ledger.setReason(request.getReason())');
  });
});
