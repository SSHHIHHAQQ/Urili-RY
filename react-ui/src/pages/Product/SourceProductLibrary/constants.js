export const approveStatusText = {
    '0': '草稿',
    '1': '审核中',
    '2': '审核通过',
    '3': '审核不通过',
    '4': '废弃',
};
export const approveStatusOptions = Object.entries(approveStatusText).map(([value, label]) => ({
    label,
    value,
}));
export const dangerousCargoText = {
    [-1]: '普货',
    1: '普货',
    2: '内置电池',
    3: '配套电池',
    4: '纯电池',
    5: '液体',
    6: '膏体',
    7: '粉末',
    8: '带磁',
};
export const dangerousCargoOptions = [1, 2, 3, 4, 5, 6, 7, 8].map((value) => ({
    label: dangerousCargoText[value],
    value,
}));
export const productTypeText = {
    0: '自有产品',
    1: '分销商品',
};
export function displayText(value) {
    if (value === undefined || value === null || value === '') {
        return '-';
    }
    return String(value);
}
export function displayNumber(value, suffix = '') {
    if (value === undefined || value === null) {
        return '-';
    }
    return `${Number(value).toFixed(2)}${suffix}`;
}
export function displayPrice(value, currencyCode) {
    if (value === undefined || value === null) {
        return '-';
    }
    return `${Number(value).toFixed(2)}${currencyCode ? ` ${currencyCode}` : ''}`;
}
export function joinText(values) {
    const text = values.filter(Boolean).join(' / ');
    return text || '-';
}
export function dimensionText(record) {
    if (!record) {
        return '-';
    }
    const length = displayNumber(record.length);
    const width = displayNumber(record.width);
    const height = displayNumber(record.height);
    if (length === '-' && width === '-' && height === '-') {
        return '-';
    }
    return `${length} x ${width} x ${height} cm`;
}
export function weightText(record) {
    if (!record) {
        return '-';
    }
    return displayNumber(record.weight, ' kg');
}
export function wmsDimensionText(record) {
    if (!record) {
        return '-';
    }
    const length = displayNumber(record.wmsLength);
    const width = displayNumber(record.wmsWidth);
    const height = displayNumber(record.wmsHeight);
    if (length === '-' && width === '-' && height === '-') {
        return '-';
    }
    return `${length} x ${width} x ${height} cm`;
}
export function wmsWeightText(record) {
    if (!record) {
        return '-';
    }
    return displayNumber(record.wmsWeight, ' kg');
}
export function jsonArrayCount(value) {
    if (!value) {
        return 0;
    }
    try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed.length : 0;
    }
    catch {
        return 0;
    }
}
