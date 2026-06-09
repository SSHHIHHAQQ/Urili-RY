import type { ProColumns } from '@ant-design/pro-components';
import { Tag } from 'antd';
import { skuPairingStatusOptions } from '@/services/integration/constants';

export type ViewMode = 'SPU' | 'SKU' | 'WAREHOUSE';

export const TABLE_SCROLL_X = 1900;
export const SEARCH_FIELD_COUNT = 15;

export const viewModeOptions = [
  { value: 'SPU', label: 'SPU视图' },
  { value: 'SKU', label: 'SKU视图' },
  { value: 'WAREHOUSE', label: '仓库视图' },
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

const syncModeText: Record<string, string> = {
  MANUAL: '手动设置平台库存',
  AUTO_SOURCE_AVAILABLE: '自动同步WMS库存',
  MIXED: '混合',
};

const syncModeColor: Record<string, string> = {
  MANUAL: 'default',
  AUTO_SOURCE_AVAILABLE: 'processing',
  MIXED: 'gold',
};

const syncPolicyScopeText: Record<string, string> = {
  SYSTEM: '系统默认',
  SELLER: '卖家维度',
  WAREHOUSE: '仓库设置',
  SPU: 'SPU设置',
  SKU: 'SKU设置',
  SKU_WAREHOUSE: '明细行设置',
  MIXED: '混合',
};

const syncStatusText: Record<string, string> = {
  NORMAL: '正常',
  UNSUPPORTED: '不支持',
  SOURCE_INSUFFICIENT: '来源不足',
};

const syncStatusColor: Record<string, string> = {
  NORMAL: 'green',
  UNSUPPORTED: 'red',
  SOURCE_INSUFFICIENT: 'orange',
};

export const inventoryStatusValueEnum = Object.fromEntries(
  Object.entries(inventoryStatusText).map(([key, text]) => [key, { text }]),
);

export const syncModeValueEnum = Object.fromEntries(
  Object.entries(syncModeText).map(([key, text]) => [key, { text }]),
);

export const warehouseKindValueEnum = {
  official: { text: '官方仓' },
  third_party: { text: '三方仓' },
  unconfigured: { text: '未配置' },
  MIXED: { text: '混合' },
};

export const warehouseStockKindValueEnum = {
  official: { text: '官方仓' },
  third_party: { text: '三方仓' },
  unconfigured: { text: '未配置' },
};

export const pairingStatusValueEnum = Object.fromEntries(
  skuPairingStatusOptions.map(({ label, value }) => [value, { text: label }]),
);

const quantityRangeFields = [
  {
    key: 'platformTotalQtyRange',
    label: '平台总库存',
    minParam: 'platformTotalQtyMin',
    maxParam: 'platformTotalQtyMax',
  },
  {
    key: 'platformAvailableQtyRange',
    label: '平台可售库存',
    minParam: 'platformAvailableQtyMin',
    maxParam: 'platformAvailableQtyMax',
  },
  {
    key: 'platformReservedQtyRange',
    label: '平台锁定库存',
    minParam: 'platformReservedQtyMin',
    maxParam: 'platformReservedQtyMax',
  },
  {
    key: 'platformInTransitQtyRange',
    label: '平台在途库存',
    minParam: 'platformInTransitQtyMin',
    maxParam: 'platformInTransitQtyMax',
  },
  {
    key: 'sourceAvailableQtyRange',
    label: '来源可用库存',
    minParam: 'sourceAvailableQtyMin',
    maxParam: 'sourceAvailableQtyMax',
  },
  {
    key: 'sourceInTransitQtyRange',
    label: '来源在途库存',
    minParam: 'sourceInTransitQtyMin',
    maxParam: 'sourceInTransitQtyMax',
  },
] as const;

const dateRangeFields = [
  {
    key: 'latestStockUpdateTimeRange',
    label: '更新时间',
    startParam: 'latestStockUpdateTimeStart',
    endParam: 'latestStockUpdateTimeEnd',
  },
  {
    key: 'latestSourceSnapshotTimeRange',
    label: '来源同步时间',
    startParam: 'latestSourceSnapshotTimeStart',
    endParam: 'latestSourceSnapshotTimeEnd',
  },
] as const;

function cleanParams(params: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

function normalizeQuantityRangeParamValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function normalizeDateRangeParamValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  if (typeof value === 'string') {
    return value;
  }
  const maybeDateValue = value as { format?: (template: string) => string };
  if (typeof maybeDateValue.format === 'function') {
    return maybeDateValue.format('YYYY-MM-DD HH:mm:ss');
  }
  return String(value);
}

export function buildInventoryOverviewListParams(params: Record<string, any>, current?: number, pageSize?: number) {
  const next = { ...params };
  const rangeParams: Record<string, number | string> = {};

  quantityRangeFields.forEach((field) => {
    const rangeValue = next[field.key];
    delete next[field.key];
    const [minValue, maxValue] = Array.isArray(rangeValue) ? rangeValue : [];
    const normalizedMinValue = normalizeQuantityRangeParamValue(minValue);
    const normalizedMaxValue = normalizeQuantityRangeParamValue(maxValue);
    if (normalizedMinValue !== undefined) {
      rangeParams[field.minParam] = normalizedMinValue;
    }
    if (normalizedMaxValue !== undefined) {
      rangeParams[field.maxParam] = normalizedMaxValue;
    }
  });

  dateRangeFields.forEach((field) => {
    const rangeValue = next[field.key];
    delete next[field.key];
    const [startValue, endValue] = Array.isArray(rangeValue) ? rangeValue : [];
    const normalizedStartValue = normalizeDateRangeParamValue(startValue);
    const normalizedEndValue = normalizeDateRangeParamValue(endValue);
    if (normalizedStartValue !== undefined) {
      rangeParams[field.startParam] = normalizedStartValue;
    }
    if (normalizedEndValue !== undefined) {
      rangeParams[field.endParam] = normalizedEndValue;
    }
  });

  return cleanParams({
    pageNum: current,
    pageSize,
    ...next,
    ...rangeParams,
  });
}

export function buildQuantityRangeSearchColumns<T>() {
  return quantityRangeFields.map(
    (field) => ({
      title: field.label,
      dataIndex: field.key,
      colSize: 2,
      valueType: 'formSet' as any,
      hideInTable: true,
      fieldProps: {
        type: 'group',
        space: { block: true, style: { width: '100%' } },
      },
      columns: [
        {
          valueType: 'digit',
          fieldProps: {
            min: 0,
            precision: 0,
            placeholder: '最小',
            style: { width: '50%' },
          },
        },
        {
          valueType: 'digit',
          fieldProps: {
            min: 0,
            precision: 0,
            placeholder: '最大',
            style: { width: '50%' },
          },
        },
      ] as any,
    }) as ProColumns<T>,
  );
}

export function buildDateRangeSearchColumns<T>() {
  return dateRangeFields.map(
    (field) => ({
      title: field.label,
      dataIndex: field.key,
      valueType: 'dateTimeRange',
      hideInTable: true,
    }) as ProColumns<T>,
  );
}

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

export function renderSyncMode(value?: string, scope?: string, status?: string) {
  const mode = value || 'MANUAL';
  const text = syncModeText[mode] || mode || '-';
  const scopeText = scope ? syncPolicyScopeText[scope] || scope : '';
  const statusText = status && status !== 'NORMAL' ? syncStatusText[status] || status : '';
  return (
    <Tag color={status && status !== 'NORMAL' ? syncStatusColor[status] || 'default' : syncModeColor[mode] || 'default'}>
      {[text, scopeText, statusText].filter(Boolean).join(' / ')}
    </Tag>
  );
}

export function renderWarehouseKind(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={warehouseKindColor[value] || 'gold'}>{warehouseKindText[value] || value}</Tag>;
}
