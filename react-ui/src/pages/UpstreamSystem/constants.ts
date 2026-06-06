export {
  inventoryPairingStatusOptions,
  pairingStatusText,
  inventoryScopeSearchOptions as inventoryScopeOptions,
  inventoryScopeText,
  requestOperationText,
  skuPairingStatusSearchOptions as skuPairingStatusOptions,
  skuSyncItemStatusSearchOptions as skuSyncItemStatusOptions,
  syncItemStatusText,
  syncTypeText,
  systemKindOptions,
  systemKindText,
} from '@/services/integration/constants';

export const connectionStatusText: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用',
};

export const credentialStatusText: Record<string, string> = {
  CONFIGURED: '已授权',
  INVALID: '授权异常',
};

export const skuSyncStateText: Record<string, string> = {
  NEVER: '未同步',
  SYNCING: '同步中',
  FRESH: '已同步',
  FAILED: '同步失败',
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

export const dimensionStatusText: Record<string, string> = {
  COMPLETE: '已完整',
  PARTIAL: '部分缺失',
  MISSING: '未获取',
};

export const dimensionStatusOptions = [
  { label: '全部尺寸状态', value: '' },
  { label: '已完整', value: 'COMPLETE' },
  { label: '部分缺失', value: 'PARTIAL' },
  { label: '未获取', value: 'MISSING' },
];
