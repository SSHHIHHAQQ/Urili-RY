export const statusValueEnum = {
    '0': { text: '正常', status: 'Success' },
    '1': { text: '停用', status: 'Default' },
};
export const yesNoValueEnum = {
    Y: { text: '是', status: 'Success' },
    N: { text: '否', status: 'Default' },
};
export const adjustmentModeOptions = [
    { label: '不调整', value: 'NONE' },
    { label: '人工维护', value: 'MANUAL' },
    { label: '上浮百分比', value: 'PERCENT_UP' },
    { label: '下调百分比', value: 'PERCENT_DOWN' },
    { label: '固定加减值', value: 'FIXED_DELTA' },
];
export const roundingModeOptions = [
    { label: '四舍五入', value: 'HALF_UP' },
    { label: '向下取整', value: 'DOWN' },
    { label: '向上取整', value: 'UP' },
];
export const syncStatusValueEnum = {
    SUCCESS: { text: '成功', status: 'Success' },
    FAILED: { text: '失败', status: 'Error' },
    PARTIAL: { text: '部分成功', status: 'Warning' },
};
