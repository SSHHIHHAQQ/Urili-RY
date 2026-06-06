export const salesStatusOptions = [
    { label: '草稿', value: 'DRAFT' },
    { label: '待上架', value: 'READY' },
    { label: '已上架', value: 'ON_SALE' },
    { label: '已下架', value: 'OFF_SALE' },
];
export const salesStatusTabOptions = [
    { label: '待上架', value: 'READY' },
    { label: '已上架', value: 'ON_SALE' },
    { label: '已下架', value: 'OFF_SALE' },
    { label: '停用', value: 'DISABLED' },
    { label: '草稿', value: 'DRAFT' },
    { label: '全部', value: 'ALL' },
];
export const salesStatusValueEnum = {
    DRAFT: { text: '草稿', status: 'Default' },
    READY: { text: '待上架', status: 'Warning' },
    ON_SALE: { text: '已上架', status: 'Success' },
    OFF_SALE: { text: '已下架', status: 'Processing' },
};
export const controlStatusOptions = [
    { label: '正常', value: 'NORMAL' },
    { label: '停用', value: 'DISABLED' },
];
export const controlStatusValueEnum = {
    NORMAL: { text: '正常', status: 'Success' },
    DISABLED: { text: '停用', status: 'Error' },
};
export const productOperationTypeText = {
    SALES_STATUS_CHANGE: '销售状态调整',
    CONTROL_DISABLE: '停用',
    CONTROL_RECOVER: '恢复',
    SALE_PRICE_ADJUST: '调价',
};
export const productOperationTypeColor = {
    SALES_STATUS_CHANGE: 'blue',
    CONTROL_DISABLE: 'orange',
    CONTROL_RECOVER: 'green',
    SALE_PRICE_ADJUST: 'purple',
};
export const sourceTypeValueEnum = {
    ADMIN_MANUAL: { text: '管理端手工创建', status: 'Processing' },
    SELLER_SUBMIT: { text: '卖家提交', status: 'Success' },
    SOURCE_PRODUCT: { text: '来源商品库生成', status: 'Default' },
};
export const warehouseKindText = {
    official: '官方仓',
    third_party: '三方仓',
    MIXED: '混合',
};
export const skuSpecFields = [
    { label: '颜色', value: 'color' },
    { label: '尺寸', value: 'size' },
    { label: '材质', value: 'material' },
    { label: '风格', value: 'style' },
    { label: '型号', value: 'model' },
    { label: '商品数量', value: 'packageQuantity' },
    { label: '容量', value: 'capacity' },
];
export function getSalesStatusText(status) {
    return salesStatusOptions.find((item) => item.value === status)?.label || status || '--';
}
export function getControlStatusText(status) {
    return controlStatusOptions.find((item) => item.value === status)?.label || status || '--';
}
function readSkuSpecValue(record, field) {
    return String(record[field] || '').trim();
}
function formatSkuSpecValue(record, field) {
    const value = readSkuSpecValue(record, field);
    if (!value)
        return '';
    const label = skuSpecFields.find((item) => item.value === field)?.label || String(field);
    return `${label}：${value}`;
}
export function buildSkuSpecText(record, siblingRows) {
    const allFields = skuSpecFields.map((item) => item.value);
    const differingFields = siblingRows && siblingRows.length > 1
        ? allFields.filter((field) => new Set(siblingRows.map((row) => readSkuSpecValue(row, field)).filter(Boolean)).size > 1)
        : [];
    const fallbackFields = ['color', 'size'];
    const fields = differingFields.length ? differingFields : fallbackFields;
    const text = fields
        .map((field) => formatSkuSpecValue(record, field))
        .filter(Boolean)
        .join(' / ');
    if (text)
        return text;
    return allFields
        .map((field) => formatSkuSpecValue(record, field))
        .filter(Boolean)
        .join(' / ');
}
function parseMeasurementText(value) {
    const raw = value?.trim();
    if (!raw)
        return undefined;
    const match = raw.match(/^(-?\d+(?:\.\d+)?)\s*(\D.*)?$/);
    if (!match)
        return { raw };
    return {
        raw,
        numberText: match[1],
        unit: match[2]?.trim(),
    };
}
function formatDimensionValue(item, includeUnit) {
    if (!item.numberText)
        return item.raw;
    const numericValue = Number(item.numberText);
    const formattedValue = Number.isFinite(numericValue) ? numericValue.toFixed(2) : item.numberText;
    if (!includeUnit || !item.unit)
        return formattedValue;
    return `${formattedValue} ${item.unit}`;
}
function stripTrailingZero(value) {
    return value.replace(/(\.\d*?[1-9])0+$/, '$1').replace(/\.0+$/, '');
}
function formatWeightText(value) {
    const item = parseMeasurementText(value);
    if (!item)
        return '';
    if (!item.numberText)
        return item.raw;
    const numericText = stripTrailingZero(item.numberText);
    return item.unit ? `${numericText} ${item.unit}` : numericText;
}
export function buildSkuDimensionText(record) {
    const dimensionValues = [record.lengthValue, record.widthValue, record.heightValue]
        .map(parseMeasurementText)
        .filter(Boolean);
    const dimensionUnits = Array.from(new Set(dimensionValues.map((item) => item.unit).filter(Boolean)));
    const commonDimensionUnit = dimensionUnits.length === 1 ? dimensionUnits[0] : undefined;
    const dimensionText = dimensionValues.length
        ? `${dimensionValues
            .map((item) => formatDimensionValue(item, !commonDimensionUnit))
            .join(' x ')}${commonDimensionUnit ? ` ${commonDimensionUnit}` : ''}`
        : '';
    const weightText = formatWeightText(record.weight);
    return [dimensionText, weightText].filter(Boolean).join(' \u00a0\u00a0 ');
}
export function formatPriceRange(min, max) {
    if (min === undefined || min === null)
        return '--';
    if (max === undefined || max === null || min === max)
        return `${min}`;
    return `${min} - ${max}`;
}
export function resolveResourceUrl(url) {
    if (!url)
        return '';
    if (/^https?:\/\//i.test(url))
        return url;
    return `/api${url.startsWith('/') ? url : `/${url}`}`;
}
