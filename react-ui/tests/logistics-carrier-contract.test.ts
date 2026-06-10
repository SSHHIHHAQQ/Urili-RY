import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(uiRoot, '..');

function readUiSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function readRepoSource(relativePath: string) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8');
}

describe('logistics carrier admin contract', () => {
  it('keeps logistics carrier services on the admin namespace', () => {
    const service = readUiSource('src/services/logistics/carrier.ts');

    expect(service).toContain("const baseUrl = '/api/logistics/admin/carriers';");
    expect(service).toContain('getCarrierList');
    expect(service).toContain('saveAgg56Credentials');
    expect(service).toContain('authorizeCarrier');
    expect(service).toContain('syncCarrierChannels');
    expect(service).toContain('getRequestLogs');
    expect(service).not.toContain('/api/seller/');
    expect(service).not.toContain('/api/buyer/');
  });

  it('keeps logistics page permission gates aligned with backend protected endpoints', () => {
    const page = readUiSource('src/pages/Logistics/Carrier/index.tsx');

    for (const permission of [
      'logistics:carrier:query',
      'logistics:carrier:add',
      'logistics:carrier:edit',
      'logistics:carrier:credential',
      'logistics:carrier:sync',
      'logistics:carrier:channel',
      'logistics:carrier:log',
    ]) {
      expect(page).toContain(permission);
    }

    expect(page).toContain("const canQueryCarrier = access.hasPerms('logistics:carrier:query');");
    expect(page).toContain("const canManageCarrierChannel = access.hasPerms('logistics:carrier:channel');");
    expect(page).toContain("const canViewCarrierLog = access.hasPerms('logistics:carrier:log');");
    expect(page).toContain("{ key: 'detail', label: '渠道', disabled: !canQueryCarrier }");
    expect(page).toContain("hidden={!canManageCarrierChannel} onClick={() => setSystemChannelDrawerOpen(true)}");
    expect(page).toContain('disabled: !canManageCarrierChannel');
    expect(page).toContain('disabled: !canViewCarrierLog');
  });

  it('keeps carrier account fields operator-owned and hides internal connection code', () => {
    const page = readUiSource('src/pages/Logistics/Carrier/index.tsx');
    const service = readUiSource('src/services/logistics/carrier.ts');

    expect(page).toContain("dataIndex: 'carrierName'");
    expect(page).toContain('label="物流商名称"');
    expect(page).toContain('label="APP Token"');
    expect(page).toContain('label="APP Key"');
    expect(page).not.toContain('接入编号');
    expect(page).not.toContain('接入名称');
    expect(page).not.toContain('connectionCode');
    expect(page).not.toContain('connectionName');
    expect(service).not.toContain('connectionCode');
    expect(service).not.toContain('connectionName');
  });

  it('uses logistics carrier channel wording in channel tables', () => {
    const page = readUiSource('src/pages/Logistics/Carrier/index.tsx');

    expect(page).toContain("title: '物流商渠道代码'");
    expect(page).toContain("title: '物流商渠道名称'");
    expect(page).not.toContain("title: '外部渠道代码'");
    expect(page).not.toContain("title: '外部渠道名称'");
    expect(page).not.toContain("title: '外部渠道'");
    expect(page).not.toContain("title: '外部代码'");
    expect(page).not.toContain("title: '原始承运商文本'");
  });

  it('keeps logistics menu sql aligned with the dynamic page component and backend permissions', () => {
    const sql = readRepoSource('RuoYi-Vue/sql/20260610_logistics_carrier_management.sql');

    expect(sql).toContain("@confirm_logistics_carrier_management");
    expect(sql).toContain("signal sqlstate '45000'");
    expect(sql).toContain("component = 'Logistics/Carrier/index'");
    expect(sql).toContain("perms = 'logistics:carrier:list'");

    for (const permission of [
      'logistics:carrier:query',
      'logistics:carrier:add',
      'logistics:carrier:edit',
      'logistics:carrier:credential',
      'logistics:carrier:sync',
      'logistics:carrier:channel',
      'logistics:carrier:label',
      'logistics:carrier:log',
    ]) {
      expect(sql).toContain(permission);
    }
  });

  it('keeps logistics contracts manifest-owned and critical to the three-terminal gate', () => {
    const manifest = readUiSource('tests/three-terminal.manifest.json');
    const verifier = readUiSource('scripts/verify-three-terminal.mjs');

    expect(manifest).toContain('"LogisticsAdminRouteContractTest"');
    expect(manifest).toContain('"tests/logistics-carrier-contract.test.ts"');
    expect(verifier).toContain('logistics[\\\\/]src[\\\\/]test[\\\\/]java');
    expect(verifier).toMatch(/logistics\|carrier/);
  });
});
