export const systemKindOptions = [{ label: '领星WMS', value: 'lingxing-wms' }];

export const systemKindText: Record<string, string> = {
  'lingxing-wms': '领星WMS',
  LINGXING_WMS: '领星WMS',
};

export const syncItemStatusText: Record<string, string> = {
  ACTIVE: '正常',
  DISABLED: '停用',
  MISSING: '上游缺失',
  MIXED: '多状态',
};

export const pairingStatusText: Record<string, string> = {
  PAIRED: '已配对',
  PARTIAL: '部分配对',
  UNASSIGNED: '未配对',
};

export const pairingRoleText: Record<string, string> = {
  FULFILLMENT: '履约',
  QUOTE: '报价',
};

export const pairingRoleTagColor: Record<string, string> = {
  FULFILLMENT: 'blue',
  QUOTE: 'orange',
};

export const skuPairingStatusOptions = [
  { label: '已配对', value: 'PAIRED' },
  { label: '未配对', value: 'UNASSIGNED' },
];

export const skuPairingStatusSearchOptions = [
  { label: '全部配对状态', value: '' },
  ...skuPairingStatusOptions,
];

export const skuSyncItemStatusOptions = [
  { label: '正常', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' },
];

export const skuSyncItemStatusSearchOptions = [
  { label: '全部同步状态', value: '' },
  ...skuSyncItemStatusOptions,
];

export const inventoryScopeText: Record<string, string> = {
  PRODUCT: '产品库存',
  BOX: '箱库存',
  RETURN: '退货库存',
  COMPREHENSIVE: '综合库存',
};

export const inventoryScopeOptions = Object.entries(inventoryScopeText).map(([value, label]) => ({
  label,
  value,
}));

export const inventoryScopeSearchOptions = [
  { label: '全部库存口径', value: '' },
  ...inventoryScopeOptions,
];

export const inventoryAttributeText: Record<string, string> = {
  '0': '正品',
  '1': '次品',
};

export const inventoryAttributeOptions = Object.entries(inventoryAttributeText).map(([value, label]) => ({
  label,
  value,
}));

export const inventoryAttributeSearchOptions = [
  { label: '全部库存属性', value: '' },
  ...inventoryAttributeOptions,
];

export const inventoryPairingStatusOptions = skuPairingStatusSearchOptions;

export const syncTypeText: Record<string, string> = {
  WAREHOUSE: '仓库',
  LOGISTICS_CHANNEL: '物流渠道',
  SKU: 'SKU信息',
  SKU_DIMENSION: 'SKU仓库尺寸重量',
  INVENTORY: 'SKU库存',
};

export const requestOperationText: Record<string, string> = {
  AUTH_CHECK: '授权校验',
  WAREHOUSE_SYNC: '仓库同步',
  LOGISTICS_CHANNEL_SYNC: '物流渠道同步',
  SKU_SYNC: 'SKU信息同步',
  SKU_DIMENSION_SYNC: 'SKU仓库尺寸重量',
  SKU_DIMENSION_FULL_SYNC: 'SKU仓库尺寸重量',
  SKU_DIMENSION_SELECTED_SYNC: '指定SKU仓库尺寸重量',
  INVENTORY_SYNC: 'SKU库存同步',
  TASK_WAREHOUSE_SYNC: '仓库同步任务',
  TASK_LOGISTICS_CHANNEL_SYNC: '物流渠道同步任务',
  TASK_SKU_SYNC: 'SKU信息同步任务',
  TASK_SKU_DIMENSION_SYNC: 'SKU仓库尺寸重量同步任务',
  TASK_INVENTORY_SYNC: 'SKU库存同步任务',
};
