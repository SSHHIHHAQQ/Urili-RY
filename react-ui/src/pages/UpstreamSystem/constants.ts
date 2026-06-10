export {
  inventoryPairingStatusOptions,
  inventoryScopeSearchOptions as inventoryScopeOptions,
  inventoryScopeText,
  pairingRoleTagColor,
  pairingRoleText,
  pairingStatusText,
  requestOperationText,
  requestResultText,
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
  PENDING: '待校验',
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
  SELF_OPERATED_RECEIVABLE: '自营仓（应收）',
};

export const normalizeSystemKindValue = (value?: string) =>
  value === 'LINGXING_WMS' ? 'lingxing-wms' : value || 'lingxing-wms';

export const normalizeSettlementTypeValue = (value?: string) => {
  const rawValue = value?.trim();
  if (!rawValue) {
    return 'upstream-payable';
  }
  const normalizedValue = rawValue.toLowerCase();
  if (normalizedValue === 'upstream-payable') {
    return 'upstream-payable';
  }
  if (normalizedValue === 'self-operated-receivable') {
    return 'self-operated-receivable';
  }
  const legacyValue = rawValue.toUpperCase();
  if (legacyValue === 'UPSTREAM_PAYABLE') {
    return 'upstream-payable';
  }
  if (
    legacyValue === 'PLATFORM_ADVANCE' ||
    legacyValue === 'SELF_OPERATED_RECEIVABLE'
  ) {
    return 'self-operated-receivable';
  }
  return rawValue;
};

export const settlementTypeDisplayText = (value?: string) => {
  const normalizedValue = normalizeSettlementTypeValue(value);
  return (
    settlementTypeText[normalizedValue] ||
    settlementTypeText[value || ''] ||
    value ||
    '-'
  );
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
