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

describe('system logistics channel admin contract', () => {
  it('keeps system channel services on the admin logistics namespace', () => {
    const service = readUiSource('src/services/logistics/systemChannel.ts');

    expect(service).toContain("const baseUrl = '/api/logistics/admin/system-channels';");
    for (const method of [
      'getSystemChannelList',
      'addSystemChannel',
      'updateSystemChannelStatus',
      'getCarrierMappings',
      'getWarehouseBindings',
      'saveOrderSetting',
      'getCarrierAccountOptions',
    ]) {
      expect(service).toContain(method);
    }
    expect(service).not.toContain('buyer-scope');
    expect(service).not.toContain('platform-mappings');
    expect(service).not.toContain('options/buyers');
    expect(service).not.toContain('/api/seller/');
    expect(service).not.toContain('/api/buyer/');
  });

  it('keeps system channel page permissions aligned with backend protected endpoints', () => {
    const page = readUiSource('src/pages/Channel/System/index.tsx');

    for (const permission of [
      'logistics:systemChannel:add',
      'logistics:systemChannel:edit',
      'logistics:systemChannel:status',
      'logistics:systemChannel:binding',
      'logistics:systemChannel:rule',
    ]) {
      expect(page).toContain(permission);
    }

    expect(page).toContain("const canBind = access.hasPerms('logistics:systemChannel:binding');");
    expect(page).toContain("const canRule = access.hasPerms('logistics:systemChannel:rule');");
    expect(page).not.toContain('logistics:systemChannel:platformMapping');
  });

  it('keeps system channel editor scoped to carrier mappings, main warehouse channel, warehouses, and order rule placeholder', () => {
    const page = readUiSource('src/pages/Channel/System/index.tsx');
    const mainFormStart = page.indexOf('<ProForm<SystemChannel>');
    const mainFormEnd = page.indexOf('<ProFormTextArea name="remark"', mainFormStart);
    const mainChannelForm = page.slice(mainFormStart, mainFormEnd);

    expect(page).toContain('shipperAddressMode');
    expect(page).toContain('externalShipperCode');
    expect(page).toContain('hasShipperAddressConfig');
    expect(page).not.toContain('getWarehousePairings');
    expect(page).toContain('getLogisticsChannelSyncList');
    expect(page).toContain('getLogisticsChannelPairings');
    expect(page).toContain('addLogisticsChannelPairing');
    expect(page).toContain('deleteLogisticsChannelPairing');
    expect(page).toContain('UPSTREAM_PAIRING_ROLE_FULFILLMENT');
    expect(page).toContain('loadUpstreamConnectionOptions');
    expect(page).not.toContain('loadFulfillmentWarehousePairingContext');
    expect(page).not.toContain('pairing.systemWarehouseCode === item.warehouseCode');
    expect(page).not.toContain('<ProFormText name="systemWarehouseCode" hidden />');
    expect(page).not.toContain('<ProFormText name="upstreamWarehouseCode" hidden />');
    expect(page).not.toContain("fulfillmentChannelForm.getFieldValue('upstreamWarehouseCode')");
    expect(page).toContain('label: connection.masterWarehouseName || connection.connectionCode');
    expect(page).toContain('searchText: connection.masterWarehouseName || connection.connectionCode');
    expect(page).not.toContain('label: `${connection.connectionCode} / ${connection.masterWarehouseName}`');
    expect(page).toContain("label: '主仓渠道映射'");
    expect(page).toContain('title="配置主仓渠道"');
    expect(page).toContain('name="upstreamChannelCode"');
    expect(page).toContain('label="主仓渠道"');
    expect(page).not.toContain('ProFormText name="upstreamChannelCode"');
    expect(page).not.toContain('主仓渠道手工录入');
    expect(page).toContain('<Row gutter={24} style={{ marginLeft: 0, marginRight: 0 }}>');
    expect(page).toContain('<Switch');
    expect(page).toContain("checked={record.status === 'ENABLED'}");
    expect(page).toContain('onClick={() => toggleStatus(record)}');
    expect(page).not.toContain('size="small"\n          checked={record.status === \'ENABLED\'}');
    expect(page).toContain('配置发货地址');
    expect(page).toContain("payload.shipperAddressMode = hasShipperAddressConfig(payload) ? 'OVERRIDE' : 'WAREHOUSE';");
    expect(page).toContain('shipperAddressLine1');
    expect(page).toContain('signatureServices');
    expect(page).toContain('ProFormCheckbox.Group');
    expect(page).toContain('fulfillmentMode');
    expect(page).toContain('logistics_system_channel_fulfillment_mode');
    expect(page).toContain('FULFILLMENT_MODE_CARRIER_LABELING');
    expect(page).toContain('FULFILLMENT_MODE_DIRECT_WAREHOUSE');
    expect(page).toContain("label=\"渠道履约模式\"");
    expect(page).toContain('shouldShowCarrierMappings');
    expect(page).toContain("...(shouldShowCarrierMappings ? [");
    expect(page).toContain('saveOrderSetting');
    expect(page).toContain('下单规则先做预留');
    expect(page).toContain("title: '绑定仓库数量'");
    expect(page).toContain("const isCreateBaseStep = channelEditorMode === 'create' && !currentChannel?.systemChannelCode;");
    expect(page).toContain("const shouldShowChannelDetails = channelEditorMode === 'edit' || !!currentChannel?.systemChannelCode;");
    expect(page).toContain('保存并下一步');
    expect(page).toContain('{shouldShowChannelDetails ? (');
    expect(page).toContain('width="94vw"');
    expect(page).toContain('maxWidth: 1680');
    expect(page).toContain("overflowX: 'hidden'");
    expect(page).not.toContain('width="88vw"');
    expect(page).not.toContain('maxWidth: 1440');
    expect(page).not.toContain('key="config"');
    expect(mainChannelForm).not.toContain('name="status"');
    expect(mainChannelForm).toContain('name="signatureServices"');
    expect(page).not.toContain('DownOutlined');
    expect(page).not.toContain('<Dropdown');
    expect(page).not.toContain('更多 <');
    expect(page).not.toContain('serviceLevel');
    expect(page).not.toContain('服务等级');
    expect(page).not.toContain('ProFormDigit');
    expect(page).not.toContain('displayOrder');
    expect(page).not.toContain('label="排序"');
    expect(page).not.toContain('shipperOverrideCount');
    expect(page).not.toContain("title: '覆写地址'");
    expect(page).not.toContain("title: '买家范围'");
    expect(page).not.toContain("title: '平台映射'");
    expect(page).not.toContain("key: 'buyerScope'");
    expect(page).not.toContain("key: 'platformMappings'");
    expect(page).not.toContain('platformChannelCode');
    expect(page).not.toContain('ProFormDigit name="maxLength"');
    expect(page).not.toContain('ProFormDependency');
    expect(page).not.toContain('label="发货地址模式"');
    expect(page).not.toContain('请先点击确定保存基础信息');
  });

  it('keeps system channel menu sql aligned with the dynamic page component and backend permissions', () => {
    const sql = readRepoSource('RuoYi-Vue/sql/20260610_system_logistics_channel_management.sql');

    expect(sql).toContain('@confirm_system_logistics_channel_management');
    expect(sql).toContain("signal sqlstate '45000'");
    expect(sql).toContain("component = 'Channel/System/index'");
    expect(sql).toContain("perms = 'logistics:systemChannel:list'");
    expect(sql).toContain('logistics_system_channel_warehouse');
    expect(sql).toContain('fulfillment_mode');
    expect(sql).toContain('logistics_system_channel_fulfillment_mode');
    expect(sql).toContain('CARRIER_LABELING');
    expect(sql).toContain('DIRECT_FULFILLMENT_WAREHOUSE');
    expect(sql).toContain('signature_services');
    expect(sql).not.toContain('service_level');
    expect(sql).not.toContain('logistics_system_channel_buyer_scope');
    expect(sql).not.toContain('logistics_platform_channel_mapping');
    expect(sql).not.toContain('logistics_channel_buyer_scope_mode');
    expect(sql).not.toContain('logistics_platform_kind');

    for (const permission of [
      'logistics:systemChannel:query',
      'logistics:systemChannel:add',
      'logistics:systemChannel:edit',
      'logistics:systemChannel:status',
      'logistics:systemChannel:binding',
      'logistics:systemChannel:rule',
    ]) {
      expect(sql).toContain(permission);
    }
    expect(sql).not.toContain('logistics:systemChannel:platformMapping');
  });

  it('keeps logistics system channel contract in the three-terminal manifest', () => {
    const manifest = readUiSource('tests/three-terminal.manifest.json');

    expect(manifest).toContain('"LogisticsAdminRouteContractTest"');
    expect(manifest).toContain('"tests/logistics-system-channel-contract.test.ts"');
  });
});
