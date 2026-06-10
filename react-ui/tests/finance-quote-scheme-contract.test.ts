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

describe('finance quote scheme admin contract', () => {
  it('keeps quote scheme mirrors as pure re-exports', () => {
    expectPureNamedReExport('src/services/finance/quoteScheme.js', './quoteScheme.ts');
    expectPureDefaultReExport('src/pages/Finance/QuoteScheme/index.js', './index.tsx');
    expectPureDefaultReExport('src/pages/Billing/QuoteScheme/index.js', './index.tsx');
    expect(readSource('src/pages/Billing/QuoteScheme/index.tsx').trim()).toBe(
      "export { default } from '../../Finance/QuoteScheme';",
    );
  });

  it('keeps quote scheme services on the finance admin namespace', () => {
    const service = readSource('src/services/finance/quoteScheme.ts');

    expect(service).toContain("const baseUrl = '/api/finance/admin/quote-schemes';");
    for (const method of [
      'getQuoteSchemeList',
      'getQuoteScheme',
      'addQuoteScheme',
      'updateQuoteScheme',
      'updateQuoteSchemeStatus',
      'getQuoteSchemeWarehouses',
      'saveQuoteSchemeWarehouses',
      'getQuoteSchemeChannels',
      'addQuoteSchemeChannel',
      'updateQuoteSchemeChannel',
      'deleteQuoteSchemeChannel',
      'getQuoteSchemeValueFees',
      'addQuoteSchemeValueFee',
      'updateQuoteSchemeValueFee',
      'deleteQuoteSchemeValueFee',
      'getQuoteSchemeBuyerOptions',
      'getQuoteSchemeWarehouseOptions',
      'getQuoteSchemeCustomerChannelOptions',
      'getQuoteSchemeSystemChannelOptions',
      'getQuoteSchemeFeePlaceholderOptions',
    ]) {
      expect(service).toContain(method);
    }
    expect(service).toContain('options/system-channels');
    expect(service).not.toContain('/api/seller/');
    expect(service).not.toContain('/api/buyer/');
    expect(service).not.toContain('/api/logistics/admin/quote');
  });

  it('keeps page permissions, pagination, and searchable selects aligned with backend', () => {
    const page = readSource('src/pages/Finance/QuoteScheme/index.tsx');

    for (const permission of [
      'finance:quoteScheme:add',
      'finance:quoteScheme:edit',
      'finance:quoteScheme:status',
      'finance:quoteScheme:channel',
      'finance:quoteScheme:valueFee',
    ]) {
      expect(page).toContain(permission);
    }
    expect(page).toContain("getPersistedProTableSearch({ fieldCount: 6 }, 'finance-quote-scheme')");
    expect(page).toContain('getProTablePagination()');
    expect(page).toContain('getProTableScroll(1600)');
    expect(page).toContain('SEARCHABLE_SELECT_PROPS');
    expect(page).toContain('getQuoteSchemeFeePlaceholderOptions');
    expect(page).not.toContain('label="方案编码"');
    expect(page).toContain('保存并下一步');
    expect(page).toContain('shouldShowSchemeDetails');
    expect(page).not.toContain('<Alert');
    expect(page).toContain('<Switch');
    expect(page).toContain('<ProFormSwitch');
    expect(page).toContain('checkedChildren="启用"');
    expect(page).toContain('placeholder: \'数字越大越优先\'');
    expect(page).toContain('buildChannelFormValues');
    expect(page).toContain('buildValueFeeFormValues');
    expect(page).toContain('QuoteSchemeValueFeeRule');
    expect(page).toContain("label: '增值费'");
    expect(page).toContain('getQuoteSchemeValueFees');
    expect(page).toContain('quote_scheme_value_fee_trigger');
    expect(page).toContain('quote_scheme_value_fee_calc_method');
    expect(page).toContain('quote_scheme_value_fee_direction');
    expect(page).toContain('ORDER_CANCELLED');
    expect(page).toContain('FIXED_AMOUNT');
    expect(page).toContain('systemChannelOptions');
    expect(page).toContain("channelScheme?.schemeType === 'COST' ? systemChannelOptions : customerChannelOptions");
    expect(page).not.toContain('费用配置');
    expect(page).not.toContain('disabled={isCostScheme}');
    expect(page).not.toContain('成本方案的系统物流渠道将在下一阶段接入');
    expect(page).not.toContain('name="status"');
  });

  it('keeps phase1 SQL guarded and finance-owned', () => {
    const sql = readRepoSource('RuoYi-Vue/sql/20260610_quote_scheme_phase1.sql');

    expect(sql).toContain('set @confirm_quote_scheme_phase1');
    expect(sql).toContain('APPLY_QUOTE_SCHEME_PHASE1');
    expect(sql).toContain("signal sqlstate '45000'");
    expect(sql).toContain('create table if not exists quote_scheme');
    expect(sql).toContain('create table if not exists quote_scheme_scope');
    expect(sql).toContain('create table if not exists quote_scheme_warehouse');
    expect(sql).toContain('create table if not exists quote_scheme_channel');
    expect(sql).toContain('scheme_type');
    expect(sql).toContain('effective_priority');
    expect(sql).toContain('warehouse_scope_mode');
    expect(sql).toContain('Finance/QuoteScheme/index');
    expect(sql).toContain('finance:quoteScheme:list');
    expect(sql).toContain("(2545, 2053, 'F', 'finance:quoteScheme:channel')");
  });

  it('keeps value fee SQL guarded and finance-owned', () => {
    const sql = readRepoSource('RuoYi-Vue/sql/20260611_quote_scheme_value_fee_rule.sql');

    expect(sql).toContain('set @confirm_quote_scheme_value_fee_rule');
    expect(sql).toContain('APPLY_QUOTE_SCHEME_VALUE_FEE_RULE');
    expect(sql).toContain("signal sqlstate '45000'");
    expect(sql).toContain('create table if not exists quote_scheme_value_fee_rule');
    expect(sql).toContain('quote_scheme_value_fee_trigger');
    expect(sql).toContain('quote_scheme_value_fee_calc_method');
    expect(sql).toContain('quote_scheme_value_fee_direction');
    expect(sql).toContain('ORDER_CANCELLED');
    expect(sql).toContain('PERCENT');
    expect(sql).toContain('FIXED_AMOUNT');
    expect(sql).toContain('INCREASE');
    expect(sql).toContain('DECREASE');
    expect(sql).toContain('finance:quoteScheme:valueFee');
    expect(sql).toContain("(2546, 2053, 'F', 'finance:quoteScheme:valueFee')");
  });

  it('is manifest-owned for the three-terminal gate', () => {
    const manifest = readSource('tests/three-terminal.manifest.json');

    expect(manifest).toContain('"tests/finance-quote-scheme-contract.test.ts"');
    expect(manifest).toContain('"FinanceAdminRouteContractTest"');
  });
});
