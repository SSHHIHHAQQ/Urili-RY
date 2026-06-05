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
  { label: '重量', value: 'weight' },
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
    record.weight,
    record.material,
    record.style,
    record.model,
    record.packageQuantity,
    record.capacity,
  ]
    .filter(Boolean)
    .join(' / ');
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
