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

describe('customer logistics channel admin contract', () => {
  it('keeps customer channel services on the admin logistics namespace', () => {
    const service = readUiSource('src/services/logistics/customerChannel.ts');

    expect(service).toContain("const baseUrl = '/api/logistics/admin/customer-channels';");
    for (const method of [
      'getCustomerChannelList',
      'addCustomerChannel',
      'updateCustomerChannelStatus',
      'getSystemMappings',
      'saveBuyerScope',
      'getSystemChannelOptions',
      'getBuyerOptions',
    ]) {
      expect(service).toContain(method);
    }
    expect(service).toContain('system-mappings');
    expect(service).toContain('buyer-scope');
    expect(service).toContain('options/system-channels');
    expect(service).toContain('options/buyers');
    expect(service).not.toContain('/api/seller/');
    expect(service).not.toContain('/api/buyer/');
  });

  it('keeps customer channel page permissions aligned with backend protected endpoints', () => {
    const page = readUiSource('src/pages/Channel/Customer/index.tsx');

    for (const permission of [
      'logistics:customerChannel:add',
      'logistics:customerChannel:edit',
      'logistics:customerChannel:status',
      'logistics:customerChannel:binding',
      'logistics:customerChannel:buyer',
    ]) {
      expect(page).toContain(permission);
    }

    expect(page).toContain("const canBind = access.hasPerms('logistics:customerChannel:binding');");
    expect(page).toContain("const canBuyer = access.hasPerms('logistics:customerChannel:buyer');");
    expect(page).not.toContain('logistics:systemChannel:');
  });

  it('keeps customer channel editor dynamic by channel type and label upload requirement', () => {
    const page = readUiSource('src/pages/Channel/Customer/index.tsx');
    const mainFormStart = page.indexOf('<ProForm<CustomerChannel>');
    const mainFormEnd = page.indexOf('<ProFormTextArea name="remark"', mainFormStart);
    const mainChannelForm = page.slice(mainFormStart, mainFormEnd);
    const buyerModalStart = page.indexOf('title="绑定买家"');
    const buyerModalEnd = page.indexOf('</Modal>', buyerModalStart);
    const buyerModal = page.slice(buyerModalStart, buyerModalEnd);

    expect(page).toContain("channelType: 'WAREHOUSE_LABEL'");
    expect(page).toContain("labelUploadRequired: 'NOT_REQUIRED'");
    expect(page).toContain("platformLabelFetch: 'NOT_FETCH'");
    expect(page).toContain("customerLabelUploadSupported: 'UNSUPPORTED'");
    expect(page).toContain("payload.channelType === 'WAREHOUSE_LABEL'");
    expect(page).toContain("payload.labelUploadRequired === 'REQUIRED'");
    expect(page).toContain('需要上传物流面单时，平台面单获取和客户上传面单至少开启一个');
    expect(page).toContain('<Row gutter={24}>');
    expect(page).toContain('<Col xs={24} lg={8}>');
    expect(page).toContain('<Switch');
    expect(page).toContain("checked={record.status === 'ENABLED'}");
    expect(page).toContain('onChange={() => toggleStatus(record)}');
    expect(page).toContain("const isCreateBaseStep = channelEditorMode === 'create' && !currentChannel?.customerChannelCode;");
    expect(page).toContain("const shouldShowChannelDetails = channelEditorMode === 'edit' || !!currentChannel?.customerChannelCode;");
    expect(page).toContain('保存并下一步');
    expect(page).toContain("key: 'systemMappings'");
    expect(page).toContain("key: 'buyerScope'");
    expect(page).toContain('绑定系统渠道');
    expect(page).toContain('绑定买家');
    expect(page).toContain('买家代码');
    expect(page).toContain('买家名称');
    expect(page).toContain('买家简称');
    expect(buyerModal).toContain("overflow: 'hidden'");
    expect(buyerModal).toContain('scroll={getProTableScroll(680, { y: 300 })}');
    expect(buyerModal).not.toContain("overflowY: 'auto'");
    expect(buyerModal).not.toContain('scroll={getProTableScroll(680, { y: 420 })}');
    expect(page).not.toContain('key="config"');
    expect(mainChannelForm).not.toContain('name="status"');
    expect(mainChannelForm).toContain('name="signatureServices"');
    expect(page).not.toContain('DownOutlined');
    expect(page).not.toContain('<Dropdown');
    expect(page).not.toContain('更多 <');
    expect(page).not.toContain('ProFormDigit');
    expect(page).not.toContain('displayOrder');
    expect(page).not.toContain("title: '排序'");
    expect(page).not.toContain('label="排序"');
    expect(page).not.toContain('保险');
    expect(page).not.toContain('平台渠道映射');
  });

  it('keeps customer channel menu sql aligned with the dynamic page component and backend permissions', () => {
    const sql = readRepoSource('RuoYi-Vue/sql/20260610_customer_logistics_channel_management.sql');

    expect(sql).toContain('@confirm_customer_logistics_channel_management');
    expect(sql).toContain("signal sqlstate '45000'");
    expect(sql).toContain("component = 'Channel/Customer/index'");
    expect(sql).toContain("perms = 'logistics:customerChannel:list'");
    expect(sql).toContain('logistics_customer_channel');
    expect(sql).toContain('logistics_customer_channel_system_mapping');
    expect(sql).toContain('logistics_customer_channel_buyer_scope');
    expect(sql).toContain('channel:customer:query');
    expect(sql).toContain('channel:customer:add');

    for (const permission of [
      'logistics:customerChannel:query',
      'logistics:customerChannel:add',
      'logistics:customerChannel:edit',
      'logistics:customerChannel:status',
      'logistics:customerChannel:binding',
      'logistics:customerChannel:buyer',
    ]) {
      expect(sql).toContain(permission);
    }
    expect(sql).not.toContain('logistics_platform_channel_mapping');
    expect(sql).not.toContain('logistics_system_channel_buyer_scope');
  });

  it('keeps logistics customer channel contract in the three-terminal manifest', () => {
    const manifest = readUiSource('tests/three-terminal.manifest.json');

    expect(manifest).toContain('"LogisticsAdminRouteContractTest"');
    expect(manifest).toContain('"tests/logistics-customer-channel-contract.test.ts"');
  });
});
