export const systemKindOptions = [{ label: '领星WMS', value: 'lingxing-wms' }];
export const systemKindText = {
    'lingxing-wms': '领星WMS',
    LINGXING_WMS: '领星WMS',
};
export const syncItemStatusText = {
    ACTIVE: '正常',
    MISSING: '上游缺失',
    MIXED: '多状态',
};
export const pairingStatusText = {
    PAIRED: '已配对',
    PARTIAL: '部分配对',
    UNASSIGNED: '未配对',
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
    { label: '上游缺失', value: 'MISSING' },
];
export const skuSyncItemStatusSearchOptions = [
    { label: '全部同步状态', value: '' },
    ...skuSyncItemStatusOptions,
];
export const inventoryScopeText = {
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
export const inventoryPairingStatusOptions = skuPairingStatusSearchOptions;
