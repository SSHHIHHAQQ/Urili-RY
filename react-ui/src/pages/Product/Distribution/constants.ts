import type { ProSchemaValueEnumObj } from '@ant-design/pro-components';

export const salesStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待上架', value: 'READY' },
  { label: '已上架', value: 'ON_SALE' },
  { label: '已下架', value: 'OFF_SALE' },
  { label: '停用', value: 'DISABLED' },
];

export const salesStatusValueEnum: ProSchemaValueEnumObj = {
  DRAFT: { text: '草稿', status: 'Default' },
  READY: { text: '待上架', status: 'Warning' },
  ON_SALE: { text: '已上架', status: 'Success' },
  OFF_SALE: { text: '已下架', status: 'Processing' },
  DISABLED: { text: '停用', status: 'Error' },
};

export const sourceTypeValueEnum: ProSchemaValueEnumObj = {
  ADMIN_MANUAL: { text: '管理端手工创建', status: 'Processing' },
  SELLER_SUBMIT: { text: '卖家提交', status: 'Success' },
  SOURCE_PRODUCT: { text: '来源商品库生成', status: 'Default' },
};

export const skuSpecFields: { label: string; value: keyof API.ProductDistribution.Sku }[] = [
  { label: '颜色', value: 'color' },
  { label: '尺寸', value: 'size' },
  { label: '材质', value: 'material' },
  { label: '风格', value: 'style' },
  { label: '型号', value: 'model' },
  { label: '商品数量', value: 'packageQuantity' },
  { label: '容量', value: 'capacity' },
];

export function getSalesStatusText(status?: string) {
  return salesStatusOptions.find((item) => item.value === status)?.label || status || '--';
}

export function buildSkuSpecText(record: API.ProductDistribution.Sku) {
  return [
    record.color,
    record.size,
    record.material,
    record.style,
    record.model,
    record.packageQuantity,
    record.capacity,
  ]
    .filter(Boolean)
    .join(' / ');
}

type MeasurementText = {
  raw: string;
  numberText?: string;
  unit?: string;
};

function parseMeasurementText(value?: string): MeasurementText | undefined {
  const raw = value?.trim();
  if (!raw) return undefined;
  const match = raw.match(/^(-?\d+(?:\.\d+)?)\s*(\D.*)?$/);
  if (!match) return { raw };
  return {
    raw,
    numberText: match[1],
    unit: match[2]?.trim(),
  };
}

function formatDimensionValue(item: MeasurementText, includeUnit: boolean) {
  if (!item.numberText) return item.raw;
  const numericValue = Number(item.numberText);
  const formattedValue = Number.isFinite(numericValue) ? numericValue.toFixed(2) : item.numberText;
  if (!includeUnit || !item.unit) return formattedValue;
  return `${formattedValue} ${item.unit}`;
}

function stripTrailingZero(value: string) {
  return value.replace(/(\.\d*?[1-9])0+$/, '$1').replace(/\.0+$/, '');
}

function formatWeightText(value?: string) {
  const item = parseMeasurementText(value);
  if (!item) return '';
  if (!item.numberText) return item.raw;
  const numericText = stripTrailingZero(item.numberText);
  return item.unit ? `${numericText} ${item.unit}` : numericText;
}

export function buildSkuDimensionText(record: API.ProductDistribution.Sku) {
  const dimensionValues = [record.lengthValue, record.widthValue, record.heightValue]
    .map(parseMeasurementText)
    .filter(Boolean) as MeasurementText[];
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

export function formatPriceRange(min?: number, max?: number) {
  if (min === undefined || min === null) return '--';
  if (max === undefined || max === null || min === max) return `${min}`;
  return `${min} - ${max}`;
}

export function resolveResourceUrl(url?: string) {
  if (!url) return '';
  if (/^https?:\/\//i.test(url)) return url;
  return `/api${url.startsWith('/') ? url : `/${url}`}`;
}
