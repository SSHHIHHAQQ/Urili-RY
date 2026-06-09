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

describe('finance currency admin contract', () => {
  it('keeps finance js mirrors as pure TS re-exports', () => {
    expectPureDefaultReExport('src/pages/Finance/Currency/index.js', './index.tsx');
    expectPureNamedReExport('src/pages/Finance/Currency/constants.js', './constants.ts');
    expectPureDefaultReExport(
      'src/pages/Finance/Currency/components/SyncSettingsPanel.js',
      './SyncSettingsPanel.tsx',
    );
    expectPureNamedReExport('src/services/finance/currency.js', './currency.ts');
  });

  it('keeps finance services on the admin namespace', () => {
    const service = readSource('src/services/finance/currency.ts');

    expect(service).toContain("const baseUrl = '/api/finance/admin';");
    expect(service).toContain('getCurrencyList');
    expect(service).toContain('getRateHistoryList');
    expect(service).toContain('getSyncConfig');
    expect(service).toContain('saveSyncConfig');
    expect(service).toContain('testSyncConfig');
    expect(service).toContain('syncRates');
    expect(service).toContain('getSyncLogList');
    expect(service).not.toContain('/api/seller/');
    expect(service).not.toContain('/api/buyer/');
  });

  it('keeps finance page and sync panel permission gates aligned with backend permissions', () => {
    const page = readSource('src/pages/Finance/Currency/index.tsx');
    const syncPanel = readSource('src/pages/Finance/Currency/components/SyncSettingsPanel.tsx');

    for (const permission of [
      'finance:currency:add',
      'finance:currency:edit',
      'finance:currency:remove',
      'finance:currency:query',
      'finance:currency:syncConfig',
      'finance:currency:sync',
      'finance:currency:log',
    ]) {
      expect(`${page}\n${syncPanel}`).toContain(permission);
    }

    expect(page).toContain("const canViewRateHistory = access.hasPerms('finance:currency:query')");
    expect(page).toContain("const canViewSyncTab = access.hasPerms('finance:currency:syncConfig')");
    expect(syncPanel).toContain("const canViewSyncConfig = access.hasPerms('finance:currency:syncConfig')");
    expect(syncPanel).toContain("const canViewSyncLog = access.hasPerms('finance:currency:log')");
    expect(syncPanel).toContain("const canSyncCurrency = access.hasPerms('finance:currency:sync')");
    expect(syncPanel).toContain('if (!canViewSyncConfig)');
    expect(syncPanel).toContain('{canViewSyncLog ? (');
  });

  it('maps all finance ProTable pagination to RuoYi page parameters', () => {
    const page = readSource('src/pages/Finance/Currency/index.tsx');
    const syncPanel = readSource('src/pages/Finance/Currency/components/SyncSettingsPanel.tsx');

    expect(page).toMatch(/getCurrencyList\(\{[\s\S]*?\.\.\.rest[\s\S]*?pageNum:\s*current[\s\S]*?pageSize[\s\S]*?\}\)/);
    expect(page).toMatch(/getRateHistoryList\([\s\S]*?historyCurrency\.currencyCode,[\s\S]*?\{[\s\S]*?\.\.\.rest[\s\S]*?pageNum:\s*current[\s\S]*?pageSize[\s\S]*?\}/);
    expect(syncPanel).toMatch(/getSyncLogList\(\{[\s\S]*?\.\.\.rest[\s\S]*?pageNum:\s*current[\s\S]*?pageSize[\s\S]*?\}\)/);
    expect(page).not.toContain('getCurrencyList(params)');
    expect(page).not.toContain('getRateHistoryList(\n                historyCurrency.currencyCode,\n                params');
    expect(syncPanel).not.toContain('getSyncLogList(params)');
  });

  it('keeps finance tests manifest-owned and critical to the three-terminal gate', () => {
    const manifest = readSource('tests/three-terminal.manifest.json');
    const verifier = readSource('scripts/verify-three-terminal.mjs');

    expect(manifest).toContain('"FinanceCurrencyServiceImplTest"');
    expect(manifest).toContain('"CurrencyRateSyncSchedulePolicyTest"');
    expect(manifest).toContain('"tests/finance-currency-contract.test.ts"');
    expect(verifier).toContain('finance[\\\\/]src[\\\\/]test[\\\\/]java');
    expect(verifier).toMatch(/Finance\|Currency/);
    expect(verifier).toMatch(/finance\|currency/);
  });
});
