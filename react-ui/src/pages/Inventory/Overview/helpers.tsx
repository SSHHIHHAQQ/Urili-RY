import { Tag } from 'antd';

export type ViewMode = 'SPU' | 'SKU';

export const TABLE_SCROLL_X = 1480;
export const SEARCH_FIELD_COUNT = 3;

export const viewModeOptions = [
  { value: 'SPU', label: 'SPU视图' },
  { value: 'SKU', label: 'SKU视图' },
] as const;

const warehouseKindText: Record<string, string> = {
  official: '官方仓',
  third_party: '三方仓',
  unconfigured: '未配置',
  MIXED: '混合',
};

const warehouseKindColor: Record<string, string> = {
  official: 'blue',
  third_party: 'purple',
  unconfigured: 'red',
  MIXED: 'gold',
};

const inventoryStatusText: Record<string, string> = {
  IN_STOCK: '有货',
  OUT_OF_STOCK: '缺货',
  NO_WAREHOUSE: '仓库未配置',
  SOURCE_UNBOUND: '来源SKU未绑定',
  NO_SOURCE: '无来源库存',
  SOURCE_ONLY_IN_TRANSIT: '仅来源在途',
  DISABLED: '停用',
};

const inventoryStatusColor: Record<string, string> = {
  IN_STOCK: 'green',
  OUT_OF_STOCK: 'default',
  NO_WAREHOUSE: 'red',
  SOURCE_UNBOUND: 'orange',
  NO_SOURCE: 'orange',
  SOURCE_ONLY_IN_TRANSIT: 'blue',
  DISABLED: 'red',
};

export const inventoryStatusValueEnum = Object.fromEntries(
  Object.entries(inventoryStatusText).map(([key, text]) => [key, { text }]),
);

export const warehouseKindValueEnum = {
  official: { text: '官方仓' },
  third_party: { text: '三方仓' },
  unconfigured: { text: '未配置' },
  MIXED: { text: '混合' },
};

export function formatQuantity(value?: number | null) {
  if (value === undefined || value === null) {
    return '-';
  }
  return new Intl.NumberFormat('zh-CN').format(Number(value));
}

export function formatDateTime(value?: string | null) {
  return value || '-';
}

export function renderStatus(value?: string) {
  const text = inventoryStatusText[value || ''] || value || '-';
  return <Tag color={inventoryStatusColor[value || ''] || 'default'}>{text}</Tag>;
}

export function renderWarehouseKind(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={warehouseKindColor[value] || 'gold'}>{warehouseKindText[value] || value}</Tag>;
}
