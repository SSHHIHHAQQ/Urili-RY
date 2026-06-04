export const connectionStatusText: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用',
};

export const systemKindText: Record<string, string> = {
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
  { label: '上游应付', value: 'UPSTREAM_PAYABLE' },
  { label: '平台垫付', value: 'PLATFORM_ADVANCE' },
];

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
