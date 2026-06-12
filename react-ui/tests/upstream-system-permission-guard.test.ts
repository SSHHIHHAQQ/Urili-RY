import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function expectPureReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export { default } from '${target}';`);
}

function expectPureNamedReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export * from '${target}';`);
}

describe('upstream system permission guard', () => {
  it('keeps the upstream system page js mirror as a pure re-export', () => {
    expectPureReExport('src/pages/UpstreamSystem/index.js', './index.tsx');
    expectPureNamedReExport('src/services/integration/upstreamSystem.js', './upstreamSystem.ts');
  });

  it('keeps component js mirrors as pure re-exports', () => {
    expectPureNamedReExport('src/pages/UpstreamSystem/constants.js', './constants.ts');
    expectPureNamedReExport('src/pages/UpstreamSystem/helpers.js', './helpers.tsx');
    expectPureNamedReExport('src/pages/UpstreamSystem/types.js', './types.ts');
    expectPureReExport('src/pages/UpstreamSystem/components/ConnectionModal.js', './ConnectionModal.tsx');
    expectPureReExport('src/pages/UpstreamSystem/components/ConnectionSidebar.js', './ConnectionSidebar.tsx');
    expectPureReExport('src/pages/UpstreamSystem/components/ConnectionSummary.js', './ConnectionSummary.tsx');
    expectPureReExport('src/pages/UpstreamSystem/components/PairingModal.js', './PairingModal.tsx');
    expectPureReExport('src/pages/UpstreamSystem/components/SkuDimensionPanel.js', './SkuDimensionPanel.tsx');
    expectPureReExport('src/pages/UpstreamSystem/components/SkuInventoryPanel.js', './SkuInventoryPanel.tsx');
    expectPureReExport('src/pages/UpstreamSystem/components/SyncTabs.js', './SyncTabs.tsx');
    expectPureReExport('src/pages/UpstreamSystem/components/SkuSyncPanel.js', './SkuSyncPanel.tsx');
  });

  it('keeps manual sync and warehouse pairing permission gates in the upstream page', () => {
    const source = readSource('src/pages/UpstreamSystem/index.tsx');
    const summarySource = readSource('src/pages/UpstreamSystem/components/ConnectionSummary.tsx');

    expect(source).toContain('syncTypeOptions');
    expect(source).toMatch(/permission:\s*['"]integration:upstream:sync['"]/);
    expect(source).toMatch(/permission:\s*['"]integration:upstream:dimensionSync['"]/);
    expect(source).toMatch(/permission:\s*['"]integration:upstream:inventorySync['"]/);
    expect(source).toMatch(/access\.hasPerms\(option\.permission\)/);
    expect(source).toMatch(/syncTypes:\s*allowedDefaults/);
    expect(source).toMatch(/syncTypes:\s*syncModal\.syncTypes/);
    expect(source).toContain('dimensionActionRef');
    expect(source).toContain('inventoryActionRef');
    expect(source).toContain("const canListUpstreamConnections = access.hasPerms('integration:upstream:list')");
    expect(source).toMatch(/if \(!canListUpstreamConnections\)[\s\S]*?setConnections\(\[\]\)[\s\S]*?setSelectedConnection\(undefined\)[\s\S]*?return \[\]/);
    expect(source).toContain('}, [canListUpstreamConnections]);');
    expect(source).toContain("const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list')");
    expect(source).toMatch(/if \(!canQueryOfficialWarehouses\)[\s\S]*?setWarehouseOptions\(\[\]\)[\s\S]*?return/);
    expect(source).toContain('getOfficialWarehouseList');
    expect(source).not.toMatch(/await syncUpstreamConnection\(record\.connectionCode\)/);

    expect(summarySource).toContain('manualSyncEntryPermissions');
    expect(summarySource).toContain("'integration:upstream:sync'");
    expect(summarySource).toContain("'integration:upstream:dimensionSync'");
    expect(summarySource).toContain("'integration:upstream:inventorySync'");
    expect(summarySource).toMatch(/manualSyncEntryPermissions\.some\([\s\S]{0,120}access\.hasPerms\(permission\)/);
    expect(summarySource).toContain('hidden={!canOpenSync}');
  });

  it('keeps pairing actions gated by their dependent permissions', () => {
    const source = readSource('src/pages/UpstreamSystem/components/SyncTabs.tsx');

    expect(source).toContain("const canPairUpstream = access.hasPerms('integration:upstream:pair')");
    expect(source).toContain("const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list')");
    expect(source).toContain("const canViewSyncTasks = access.hasPerms('integration:upstream:task:list')");
    expect(source).toContain("const canRetrySyncTask = access.hasPerms('integration:upstream:task:retry')");
    expect(source).toContain("const canCancelSyncTask = access.hasPerms('integration:upstream:task:cancel')");
    expect(source).toContain('hidden={!canPairUpstream || !canQueryOfficialWarehouses}');
    expect(source).toMatch(/return canPairUpstream \? \([\s\S]*?<Popconfirm[\s\S]*?deleteLogisticsChannelPairing[\s\S]*?\) : \(/);
    expect(source).toContain("key: 'tasks'");
    expect(source).toContain('disabled: !canViewSyncTasks');
    expect(source).toContain('getSyncTaskList(requestCode,');
    expect(source).toContain('retrySyncTask(selectedCode, record.taskId)');
    expect(source).toContain('cancelSyncTask(selectedCode, record.taskId)');
  });

  it('maps request-log ProTable pagination to RuoYi page parameters', () => {
    const source = readSource('src/pages/UpstreamSystem/components/SyncTabs.tsx');

    expect(source).toContain('const { current, pageSize, ...rest } = params;');
    expect(source).toMatch(/getRequestLogList\(requestCode,\s*\{[\s\S]*?\.\.\.rest[\s\S]*?pageNum:\s*current[\s\S]*?pageSize[\s\S]*?\}\)/);
    expect(source).not.toContain('getRequestLogList(requestCode, params)');
    expect(source).toMatch(/getSyncTaskList\(requestCode,\s*\{[\s\S]*?\.\.\.rest[\s\S]*?pageNum:\s*current[\s\S]*?pageSize[\s\S]*?\}\)/);
    expect(source).not.toContain('getSyncTaskList(requestCode, params)');
  });

  it('maps upstream SKU and inventory panels to RuoYi page parameters', () => {
    const skuSyncSource = readSource('src/pages/UpstreamSystem/components/SkuSyncPanel.tsx');
    const skuDimensionSource = readSource('src/pages/UpstreamSystem/components/SkuDimensionPanel.tsx');
    const skuInventorySource = readSource('src/pages/UpstreamSystem/components/SkuInventoryPanel.tsx');

    expect(skuSyncSource).toMatch(/getSkuSyncList\(selectedCode,\s*\{[\s\S]*?pageNum:\s*params\.current[\s\S]*?pageSize:\s*params\.pageSize[\s\S]*?\}\)/);
    expect(skuSyncSource).not.toContain('getSkuSyncList(selectedCode, params)');
    expect(skuDimensionSource).toMatch(/getSkuSyncList\(selectedCode,\s*\{[\s\S]*?pageNum:\s*params\.current[\s\S]*?pageSize:\s*params\.pageSize[\s\S]*?\}\)/);
    expect(skuDimensionSource).not.toContain('getSkuSyncList(selectedCode, params)');
    expect(skuInventorySource).toMatch(/getUpstreamInventoryList\(selectedCode,\s*\{[\s\S]*?pageNum:\s*params\.current[\s\S]*?pageSize:\s*params\.pageSize[\s\S]*?\}\)/);
    expect(skuInventorySource).not.toContain('getUpstreamInventoryList(selectedCode, params)');
  });
});
