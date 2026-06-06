export const systemKindOptions = [{ label: '领星WMS', value: 'lingxing-wms' }];
export const systemKindText = {
    'lingxing-wms': '领星WMS',
    LINGXING_WMS: '领星WMS',
};
export const syncItemStatusText = {
    ACTIVE: '正常',
    MISSING: '上游缺失',
};
export const pairingStatusText = {
    PAIRED: '已配对',
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
