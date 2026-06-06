export const WAREHOUSE_STATUS_OPTIONS = [
    { label: '正常', value: '0' },
    { label: '停用', value: '1' },
];
export const WAREHOUSE_STATUS_VALUE_ENUM = {
    '0': { text: '正常', status: 'Success' },
    '1': { text: '停用', status: 'Default' },
};
export function statusText(status) {
    return status === '1' ? '停用' : '正常';
}
export function statusColor(status) {
    return status === '1' ? 'default' : 'green';
}
