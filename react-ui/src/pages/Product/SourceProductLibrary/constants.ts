export const approveStatusText: Record<string, string> = {
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

export const dangerousCargoText: Record<number, string> = {
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

export const productTypeText: Record<number, string> = {
  0: '自有产品',
  1: '分销商品',
};

export function displayText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

export function displayNumber(value?: number | null, suffix = '') {
  if (value === undefined || value === null) {
    return '-';
  }
  return `${Number(value).toFixed(2)}${suffix}`;
}

export function displayPrice(value?: number | null, currencyCode?: string) {
  if (value === undefined || value === null) {
    return '-';
  }
  return `${Number(value).toFixed(2)}${currencyCode ? ` ${currencyCode}` : ''}`;
}

export function joinText(values: Array<string | undefined>) {
  const text = values.filter(Boolean).join(' / ');
  return text || '-';
}

export function dimensionText(record?: API.Integration.SourceProductItem) {
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

export function weightText(record?: API.Integration.SourceProductItem) {
  if (!record) {
    return '-';
  }
  return displayNumber(record.weight, ' kg');
}

export function wmsDimensionText(record?: API.Integration.SourceProductItem) {
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

export function wmsWeightText(record?: API.Integration.SourceProductItem) {
  if (!record) {
    return '-';
  }
  return displayNumber(record.wmsWeight, ' kg');
}

export function jsonArrayCount(value?: string) {
  if (!value) {
    return 0;
  }
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.length : 0;
  } catch {
    return 0;
  }
}
