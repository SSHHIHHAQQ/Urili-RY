export const connectionStatusText: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用',
};

export const systemKindOptions = [{ label: '领星WMS', value: 'lingxing-wms' }];

export const systemKindText: Record<string, string> = {
  'lingxing-wms': '领星WMS',
  LINGXING_WMS: '领星WMS',
};

export const credentialStatusText: Record<string, string> = {
  CONFIGURED: '已授权',
  INVALID: '授权异常',
};

export const syncItemStatusText: Record<string, string> = {
  ACTIVE: '正常',
  MISSING: '上游缺失',
};

export const skuSyncStateText: Record<string, string> = {
  NEVER: '未同步',
  SYNCING: '同步中',
  FRESH: '已同步',
  FAILED: '同步失败',
};

export const pairingStatusText: Record<string, string> = {
  PAIRED: '已配对',
  UNASSIGNED: '未配对',
};

export const settlementOptions = [
  { label: '上游仓（应付）', value: 'upstream-payable' },
  { label: '自营仓（应收）', value: 'self-operated-receivable' },
];

export const settlementTypeText: Record<string, string> = {
  'upstream-payable': '上游仓（应付）',
  'self-operated-receivable': '自营仓（应收）',
  UPSTREAM_PAYABLE: '上游仓（应付）',
  PLATFORM_ADVANCE: '自营仓（应收）',
};

export const normalizeSystemKindValue = (value?: string) =>
  value === 'LINGXING_WMS' ? 'lingxing-wms' : value || 'lingxing-wms';

export const normalizeSettlementTypeValue = (value?: string) => {
  if (value === 'UPSTREAM_PAYABLE') {
    return 'upstream-payable';
  }
  if (value === 'PLATFORM_ADVANCE') {
    return 'self-operated-receivable';
  }
  return value || 'upstream-payable';
};

export const skuSearchFieldOptions = [
  { label: '全部字段', value: 'all' },
  { label: '领星 masterSku', value: 'masterSku' },
  { label: '领星产品名', value: 'masterProductName' },
  { label: '系统SKU', value: 'systemSku' },
  { label: '客户名称', value: 'customerName' },
];

export const skuPairingStatusOptions = [
  { label: '全部配对状态', value: '' },
  { label: '已配对', value: 'PAIRED' },
  { label: '未配对', value: 'UNASSIGNED' },
];

export const skuSyncItemStatusOptions = [
  { label: '全部同步状态', value: '' },
  { label: '正常', value: 'ACTIVE' },
  { label: '上游缺失', value: 'MISSING' },
];
