import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

describe('inventory adjustment review contract', () => {
  it('keeps js mirrors as pure TS/TSX re-exports', () => {
    expect(readSource('src/services/inventory/adjustmentReview.js').trim()).toBe(
      "export * from './adjustmentReview.ts';",
    );
    expect(readSource('src/pages/Inventory/AdjustmentReview/index.js').trim()).toBe(
      "export { default } from './index.tsx';",
    );
  });

  it('uses admin review APIs and review permissions', () => {
    const service = readSource('src/services/inventory/adjustmentReview.ts');
    const page = readSource('src/pages/Inventory/AdjustmentReview/index.tsx');

    expect(service).toContain("const baseUrl = '/api/inventory/admin/adjustment-reviews';");
    for (const route of [
      '/list',
      '/logs',
      '/effect-now',
      '/effective-time',
      '/reject',
      '/policies/list',
      '/policy-bindings/list',
    ]) {
      expect(service).toContain(route);
    }

    for (const permission of [
      'review:inventoryAdjustment:list',
      'review:inventoryAdjustment:query',
      'review:inventoryAdjustment:effect',
      'review:inventoryAdjustment:edit',
      'review:inventoryAdjustment:reject',
      'review:inventoryAdjustment:log',
      'review:inventoryAdjustment:config',
    ]) {
      expect(page).toContain(`access.hasPerms('${permission}')`);
    }
  });

  it('keeps the UI vocabulary on requested return quantity', () => {
    const page = readSource('src/pages/Inventory/AdjustmentReview/index.tsx');
    const overviewQuantityCell = readSource('src/pages/Inventory/Overview/components/QuantityCell.tsx');
    const inventoryAdjustButton = readSource('src/components/InventoryAdjust/InventoryAdjustButton.tsx');

    expect(page).toContain('申请退回/调整');
    expect(page).toContain('可立即退回');
    expect(page).toContain('保护保留');
    expect(page).toContain('实际生效');
    expect(overviewQuantityCell).toContain('已生成库存调整审核单');
    expect(overviewQuantityCell).toContain('申请退回');
    expect(inventoryAdjustButton).toContain('reviewRequiredCount');
  });

  it('keeps all policy and seller-binding config fields wired to validation', () => {
    const page = readSource('src/pages/Inventory/AdjustmentReview/index.tsx');

    for (const field of [
      'policyStatus',
      'reviewMode',
      'directionScope',
      'salesWindowDays',
      'reserveDays',
      'cooldownHours',
      'minReturnQtyToReview',
      'minReturnRatioToReview',
      'autoEffectEnabled',
      'manualEffectAllowed',
      'policyId',
      'bindingType',
      'bindingIdValue',
      'priority',
      'status',
    ]) {
      expect(page).toContain(`name="${field}"`);
    }

    expect(page).toContain('validateSalesWindowText');
    expect(page).toContain('销量窗口必须至少包含一个正整数天数');
    expect(page).toContain("bindingType === 'SELLER'");
    expect(page).toContain('请输入卖家ID');
    expect(page).toContain("disabled={bindingType === 'GLOBAL'}");
  });
});
