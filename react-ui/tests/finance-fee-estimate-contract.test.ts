import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(uiRoot, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function readRepoSource(relativePath: string) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8');
}

function expectPureDefaultReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export { default } from '${target}';`);
}

function expectPureNamedReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export * from '${target}';`);
}

describe('finance fee estimate admin contract', () => {
  it('keeps fee estimate mirrors as pure re-exports', () => {
    expectPureNamedReExport('src/services/finance/feeEstimate.js', './feeEstimate.ts');
    expectPureDefaultReExport('src/pages/Finance/FeeEstimate/index.js', './index.tsx');
  });

  it('keeps fee estimate services on the finance admin namespace', () => {
    const service = readSource('src/services/finance/feeEstimate.ts');

    expect(service).toContain("const baseUrl = '/api/finance/admin/fee-estimate';");
    expect(service).toContain('getFeeEstimateOptions');
    expect(service).toContain('getFeeEstimateSkus');
    expect(service).toContain('calculateFeeEstimate');
    expect(service).toContain('/skus/list');
    expect(service).toContain('/calculate');
    expect(service).not.toContain('/api/seller/');
    expect(service).not.toContain('/api/buyer/');
    expect(service).not.toContain('/api/product/');
  });

  it('keeps the adaptive panels, permissions, and mutual input modes', () => {
    const page = readSource('src/pages/Finance/FeeEstimate/index.tsx');
    const styles = readSource('src/pages/Finance/FeeEstimate/style.module.css');

    expect(page).toContain("access.hasPerms('finance:feeEstimate:query')");
    expect(page).toContain("access.hasPerms('finance:feeEstimate:calculate')");
    expect(page).toContain("effectivePackageInputMode === 'SKU'");
    expect(page).toContain("packageInputMode === 'MANUAL'");
    expect(page).toContain("selectionMode === 'AUTO_BEST'");
    expect(page).toContain('VIEW_BUYER_SIMULATION');
    expect(page).toContain('viewTabs');
    expect(page).toContain('selectionModeOptions');
    expect(page).toContain('buyerId: isBuyerSimulation ? values.buyerId : undefined');
    expect(page).toContain('warehouseCodes: isBuyerAuto ? values.warehouseCodes || [] : undefined');
    expect(page).toContain('customerChannelCode: isBuyerManual ? values.customerChannelCode : undefined');
    expect(page).toContain('inputModeOptions');
    expect(page).toContain('options={inputModeOptions}');
    expect(page).toContain('label="包裹方式"');
    expect(page).toContain('label="仓库/渠道选择方式"');
    expect(page).toContain('label="买家"');
    expect(page).toContain('label="商品/SKU"');
    expect(page).toContain('renderBuyerSkuPicker');
    expect(page).toContain('const openSkuSelector = () =>');
    expect(page).toContain('title="选择商品 SKU"');
    expect(page).toContain('okText={`确认选择（${selectedSkuItems.length}）`}');
    expect(page).toContain('skuSelectorColumns');
    expect(page).toContain('rowSelection={{');
    expect(page).toContain('preserveSelectedRowKeys: true');
    expect(page).toContain('selectedRowKeys: selectedSkuKeys');
    expect(page).toContain("dataIndex: 'sourceWarehouseCode'");
    expect(page).toContain("dataIndex: 'skuCode'");
    expect(page).toContain("dataIndex: 'productName'");
    expect(page).toContain('sourceWarehouseCode: params.sourceWarehouseCode');
    expect(page).toContain('skuCode: params.skuCode');
    expect(page).toContain('productName: params.productName');
    expect(page).not.toContain("dataIndex: 'keyword'");
    expect(page).not.toContain('keyword: params.keyword');
    expect(page).not.toContain('关键词');
    expect(page).toContain('getCommonSourceWarehouseCodes');
    expect(page).toContain('canMergeSkuBySourceWarehouse');
    expect(page).toContain('sourceWarehouseCodes');
    expect(page).toContain("title: '可用库存'");
    expect(page).toContain("dataIndex: 'availableStock'");
    expect(page).toContain('共同来源仓');
    expect(page).toContain('所选 SKU 必须至少有一个共同来源仓');
    expect(page).toContain('所选 SKU 没有共同来源仓，不能放在同一个包裹试算');
    expect(page).toContain('getCheckboxProps: (record) =>');
    expect(page).toContain('disabled: !selected && !canMergeSkuBySourceWarehouse(selectedSkuItems, record)');
    expect(page).toContain('okButtonProps={{ disabled: !selectedSkuItems.length || !selectedCommonWarehouseCodes.length }}');
    expect(page).toContain('label="限制仓库"');
    expect(page).toContain('label="选择客户渠道"');
    expect(page).toContain('候选解析');
    expect(page).toContain('name="destinationAddress2"');
    expect(page).toContain('destinationAddress2: values.destinationAddress2');
    expect(page).toContain('getFeeEstimateSkus');
    expect(page).toContain('calculateFeeEstimate');
    expect(page).toContain('mode="multiple"');
    expect(page).toContain('placeholder="不选则展示全部渠道"');
    expect(page).toContain('placeholder="不选则全部可用仓库参与计算"');
    expect(page).toContain('RequestID:');
    expect(page).toContain("title: '包裹尺寸'");
    expect(page).toContain("title: '实重'");
    expect(page).toContain("title: '体积重'");
    expect(page).toContain("title: '计费重'");
    expect(page).not.toContain("title: '包裹数量'");
    expect(page).toContain('styles.conditionPanel');
    expect(page).toContain('styles.packagePanel');
    expect(page).toContain('styles.resolvePanel');
    expect(page).toContain('styles.resultPanel');
    expect(page).toContain('styles.manualWorkspace');
    expect(page).toContain('styles.buyerWorkspace');
    expect(page).toContain('styles.manualFields');
    expect(page).toContain('measureLengthCm');
    expect(page).toContain('measureWeightKg');
    expect(page).toContain('chargeableWeightKg');
    expect(page).not.toContain('是否住宅地址');
    expect(page).not.toContain('申报价值');
    expect(page).not.toContain('签名服务');

    expect(page.indexOf('name="originWarehouseCode"')).toBeLessThan(page.indexOf('name="quoteSchemeId"'));
    expect(page.indexOf('name="buyerId"')).toBeLessThan(page.indexOf('label="商品/SKU"'));
    expect(page.indexOf('label="商品/SKU"')).toBeLessThan(page.indexOf('name="selectionMode"'));
    expect(page.indexOf('name="quoteSchemeId"')).toBeLessThan(page.indexOf('name="channelCodes"'));
    expect(page.indexOf('name="channelCodes"')).toBeLessThan(page.indexOf('options={inputModeOptions}'));
    expect(page.indexOf('options={inputModeOptions}')).toBeLessThan(page.indexOf('name="destinationCountryCode"'));
    expect(page.indexOf('name="destinationAddress1"')).toBeLessThan(page.indexOf('name="destinationAddress2"'));

    expect(styles).toContain('grid-template-columns: minmax(320px, 360px) minmax(0, 1fr)');
    expect(styles).toContain('grid-template-rows: minmax(260px, 34vh) minmax(360px, 1fr)');
    expect(styles).toContain('.manualWorkspace');
    expect(styles).toContain('grid-template-rows: minmax(0, 1fr)');
    expect(styles).toContain('.buyerWorkspace');
    expect(styles).toContain('.conditionPanel');
    expect(styles).toContain('.packagePanel');
    expect(styles).toContain('.resolvePanel');
    expect(styles).toContain('.resultPanel');
    expect(styles).toContain('.manualFields');
    expect(styles).toContain('.selectedSkuList');
    expect(styles).toContain('.skuSelectionBoard');
    expect(styles).toContain('.skuSelectionTag');
    expect(styles).toContain('.skuCommonWarehouseLine');
    expect(styles).toContain('.resolveGrid');
  });

  it('keeps menu SQL guarded and table-free', () => {
    const sql = readRepoSource('RuoYi-Vue/sql/20260612_fee_estimate_menu_seed.sql');

    expect(sql).toContain('set @confirm_fee_estimate_menu_seed');
    expect(sql).toContain('APPLY_FEE_ESTIMATE_MENU_SEED');
    expect(sql).toContain("signal sqlstate '45000'");
    expect(sql).toContain("(2550, 2050, 'C', 'fee-estimate', 'Finance/FeeEstimate/index'");
    expect(sql).toContain('and order_num = 1');
    expect(sql).toContain('finance:feeEstimate:list');
    expect(sql).toContain('finance:feeEstimate:query');
    expect(sql).toContain('finance:feeEstimate:calculate');
    expect(sql).not.toContain('create table if not exists fee_estimate');
    expect(sql).not.toContain('create table if not exists finance_fee_estimate');
  });

  it('is manifest-owned for the three-terminal gate', () => {
    const manifest = readSource('tests/three-terminal.manifest.json');

    expect(manifest).toContain('"tests/finance-fee-estimate-contract.test.ts"');
    expect(manifest).toContain('"FinanceAdminRouteContractTest"');
  });
});
