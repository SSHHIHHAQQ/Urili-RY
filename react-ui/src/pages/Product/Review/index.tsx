import { PageContainer, type ActionType, type ProColumns, ProTable } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  Button,
  Descriptions,
  Drawer,
  Form,
  Image,
  Input,
  Modal,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { type CSSProperties, type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getDictValueEnum } from '@/services/system/dict';
import {
  approveProductReview,
  getProductReview,
  getProductReviewList,
  getProductReviewLogs,
  getProductReviewPendingCounts,
  rejectProductReview,
} from '@/services/product/productReview';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTablePagination, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  getControlStatusText,
  getSalesStatusText,
  inventoryStatusText,
  resolveResourceUrl,
  warehouseKindText,
} from '../Distribution/constants';
import ProductReviewBusinessPreview from './components/ProductReviewBusinessPreview';

type ActionKind = 'APPROVE' | 'REJECT';
type ActionState = {
  open: boolean;
  kind: ActionKind;
  review?: API.ProductReview.Review;
};

type ValueEnumItem = {
  text: string;
  label?: string;
  value?: string;
  status?: string;
  listClass?: string;
};

type SnapshotCompareRow = {
  key: string;
  item?: API.ProductReview.Item;
  payloadType?: string;
  before?: API.ProductReview.Snapshot;
  after?: API.ProductReview.Snapshot;
};

type ReviewListColumnContext = {
  canQueryProductReview: boolean;
  canApproveProductReview: boolean;
  canRejectProductReview: boolean;
  reviewStatusValueEnum: Record<string, ValueEnumItem>;
  reviewTypeValueEnum: Record<string, ValueEnumItem>;
  openDetail: (record: API.ProductReview.Review) => void;
  openAction: (kind: ActionKind, record: API.ProductReview.Review) => void;
};

const REVIEW_TYPE_ORDER = [
  'NEW_PRODUCT',
  'ADD_SKU',
  'EDIT_PRODUCT_INFO',
  'EDIT_SKU_INFO',
  'EDIT_PRICE',
  'EDIT_MIXED',
];
const DEFAULT_PRODUCT_REVIEW_LIST_STATUS = 'PENDING';

const listLineStackStyle: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 4,
};

const listLineStyle: CSSProperties = {
  minHeight: 24,
  lineHeight: '20px',
  whiteSpace: 'normal',
  overflowWrap: 'anywhere',
  wordBreak: 'break-word',
};

const listTagLineStyle: CSSProperties = {
  minHeight: 24,
  display: 'flex',
  alignItems: 'center',
  gap: 4,
  flexWrap: 'wrap',
};

const DEFAULT_REVIEW_TYPE_ENUM: Record<string, ValueEnumItem> = {
  NEW_PRODUCT: { text: '新增商品', status: 'processing' },
  ADD_SKU: { text: '新增SKU', status: 'processing' },
  EDIT_PRODUCT_INFO: { text: '商品资料变更', status: 'warning' },
  EDIT_SKU_INFO: { text: 'SKU资料变更', status: 'warning' },
  EDIT_PRICE: { text: '供货价变更', status: 'error' },
  EDIT_MIXED: { text: '综合变更', status: 'warning' },
};

const DEFAULT_REVIEW_STATUS_ENUM: Record<string, ValueEnumItem> = {
  PENDING: { text: '待审核', status: 'warning' },
  APPROVED: { text: '已通过', status: 'success' },
  REJECTED: { text: '已驳回', status: 'error' },
  WITHDRAWN: { text: '已撤回', status: 'default' },
};

const TERMINAL_VALUE_ENUM: Record<string, ValueEnumItem> = {
  ADMIN: { text: '管理端' },
  SELLER: { text: '卖家端' },
  BUYER: { text: '买家端' },
};

const ITEM_TYPE_TEXT: Record<string, string> = {
  SPU: '商品',
  SKU: 'SKU',
};

const CHANGE_TYPE_TEXT: Record<string, string> = {
  CREATE: '新增',
  UPDATE: '资料变更',
  PRICE_UPDATE: '供货价变更',
};

const OPERATION_TYPE_TEXT: Record<string, string> = {
  SUBMIT: '提交',
  APPROVE: '通过',
  REJECT: '驳回',
  WITHDRAW: '撤回',
};

const SNAPSHOT_ROLE_TEXT: Record<string, string> = {
  BEFORE: '修改前',
  AFTER: '修改后',
};

const PAYLOAD_TYPE_TEXT: Record<string, string> = {
  SPU: '商品',
  SKU: 'SKU',
  PRICE: '供货价',
  OBJECT: '对象',
  UNKNOWN: '未知',
};

const ATTRIBUTE_TYPE_TEXT: Record<string, string> = {
  TEXT: '文本',
  NUMBER: '数字',
  DATE: '日期',
  SINGLE_SELECT: '单选',
  MULTI_SELECT: '多选',
  BOOLEAN: '是/否',
};

const IMAGE_ROLE_TEXT: Record<string, string> = {
  MAIN: '主图',
  GALLERY: '图库',
};

const LOCK_STATUS_TEXT: Record<string, string> = {
  LOCKED: '已锁定',
  UNLOCKED: '未锁定',
};

const BINDING_STATUS_TEXT: Record<string, string> = {
  BOUND: '已绑定',
  UNBOUND: '未绑定',
};

const TABLE_SCROLL_X = 2300;

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function valueEnumText(valueEnum: Record<string, ValueEnumItem>, value?: string, unknownText = '未知') {
  if (!value) {
    return '--';
  }
  return valueEnum[value]?.text || unknownText;
}

function normalizeValueEnum(data: Record<string, ValueEnumItem>, fallback: Record<string, ValueEnumItem>) {
  return Object.keys(data || {}).length > 0 ? data : fallback;
}

function normalizeReviewTypeValueEnum(data: Record<string, ValueEnumItem>) {
  const valueEnum = normalizeValueEnum(data, DEFAULT_REVIEW_TYPE_ENUM);
  return {
    ...valueEnum,
    EDIT_PRICE: {
      ...(valueEnum.EDIT_PRICE || DEFAULT_REVIEW_TYPE_ENUM.EDIT_PRICE),
      text: '供货价变更',
    },
    EDIT_MIXED: {
      ...(valueEnum.EDIT_MIXED || DEFAULT_REVIEW_TYPE_ENUM.EDIT_MIXED),
      text: '综合变更',
    },
  };
}

function formatPriceRangeValues(min?: number, max?: number, currencySummary?: string) {
  if (min == null && max == null) {
    return '--';
  }
  const currency = currencySummary ? ` ${currencySummary}` : '';
  if (min != null && max != null && min !== max) {
    return `${min} - ${max}${currency}`;
  }
  return `${min ?? max}${currency}`;
}

function formatPriceRange(record: API.ProductReview.Review, role: 'before' | 'after' = 'after') {
  return role === 'before'
    ? formatPriceRangeValues(record.priceBeforeMin, record.priceBeforeMax, record.currencySummary)
    : formatPriceRangeValues(record.priceAfterMin, record.priceAfterMax, record.currencySummary);
}

function formatPriceTransition(record: API.ProductReview.Review) {
  const before = formatPriceRange(record, 'before');
  const after = formatPriceRange(record, 'after');
  if (before !== '--' && after !== '--') {
    return `${before} -> ${after}`;
  }
  return after !== '--' ? after : before;
}

function formatCount(value?: number, unit = '个') {
  return value == null ? '--' : `${value}${unit}`;
}

function normalizeWarehouseKind(value?: string) {
  if (!value) {
    return '';
  }
  if (warehouseKindText[value]) {
    return value;
  }
  if (value === '官方仓') {
    return 'official';
  }
  if (value === '三方仓' || value === '混合') {
    return value === '混合' ? 'MIXED' : 'third_party';
  }
  return 'third_party';
}

function formatWarehouseKindLabel(value?: string) {
  const normalized = normalizeWarehouseKind(value);
  if (!normalized) {
    return '--';
  }
  return warehouseKindText[normalized] || normalized;
}

function formatSalesStatusLabel(value?: string) {
  if (!value) {
    return '--';
  }
  if (value === 'DISABLED') {
    return '停用';
  }
  const label = getSalesStatusText(value);
  return label === value ? '未知状态' : label;
}

function formatControlStatusLabel(value?: string) {
  if (!value) {
    return '--';
  }
  const label = getControlStatusText(value);
  return label === value ? '未知状态' : label;
}

function formatSnapshotRoleLabel(value?: string) {
  return value ? SNAPSHOT_ROLE_TEXT[value] || '未知快照' : '--';
}

function formatPayloadTypeLabel(value?: string) {
  return value ? PAYLOAD_TYPE_TEXT[value] || '未知载荷' : '--';
}

function formatItemTypeLabel(value?: string) {
  return value ? ITEM_TYPE_TEXT[value] || '未知对象' : '--';
}

function formatChangeTypeLabel(value?: string) {
  return value ? CHANGE_TYPE_TEXT[value] || '未知变化' : '--';
}

function formatOperationTypeLabel(value?: string) {
  return value ? OPERATION_TYPE_TEXT[value] || '未知操作' : '--';
}

function formatReviewOrSalesStatusLabel(value?: string) {
  if (!value) {
    return '--';
  }
  const reviewStatus = valueEnumText(DEFAULT_REVIEW_STATUS_ENUM, value, '');
  if (reviewStatus) {
    return reviewStatus;
  }
  return formatSalesStatusLabel(value);
}

function isOfficialWarehouseKind(value?: string) {
  return normalizeWarehouseKind(value) === 'official';
}

function resolveReviewSkuCount(record: API.ProductReview.Review) {
  return record.skuCount ?? record.items?.filter((item) => item.itemType === 'SKU').length;
}

function formatTitleTransition(record: API.ProductReview.Review) {
  const before = record.productNameBefore || '--';
  const after = record.productNameAfter || '--';
  if (before !== '--' && after !== '--' && before !== after) {
    return `${before} -> ${after}`;
  }
  return after !== '--' ? after : before;
}

function formatMainImageChange(record: API.ProductReview.Review) {
  const before = record.mainImageUrlBefore || '';
  const after = record.mainImageUrlAfter || '';
  if (before && after && before !== after) {
    return '已变更';
  }
  if (!before && after) {
    return '新增主图';
  }
  if (before && !after) {
    return '移除主图';
  }
  return before || after ? '无变化' : '--';
}

function getListDisplayItems(record: API.ProductReview.Review) {
  return record.listDisplayItems || [];
}

function getChangedModules(record: API.ProductReview.Review) {
  return record.listChangedModules?.length ? record.listChangedModules : [];
}

function formatListMoney(value?: number, currency?: string) {
  if (value == null) {
    return '--';
  }
  return `${value}${currency ? ` ${currency}` : ''}`;
}

function renderListLines<T>(
  rows: T[] | undefined,
  renderLine: (row: T, index: number) => ReactNode,
  emptyText = '--',
) {
  const visibleRows = rows || [];
  if (!visibleRows.length) {
    return emptyText;
  }
  return (
    <div style={listLineStackStyle}>
      {visibleRows.map((row, index) => (
        <div key={index} style={listLineStyle}>
          {renderLine(row, index)}
        </div>
      ))}
    </div>
  );
}

function renderSummaryTextLines(value?: string) {
  const lines = String(value || '')
    .split(/[；;\n]+/)
    .map((part) => part.trim())
    .filter(Boolean);
  if (!lines.length) {
    return '--';
  }
  return (
    <div style={listLineStackStyle}>
      {lines.map((line) => (
        <span key={line} style={listLineStyle}>
          {line}
        </span>
      ))}
    </div>
  );
}

function renderTagLines<T>(
  rows: T[] | undefined,
  renderTags: (row: T, index: number) => ReactNode,
  emptyText = '--',
) {
  const visibleRows = rows || [];
  if (!visibleRows.length) {
    return emptyText;
  }
  return (
    <div style={listLineStackStyle}>
      {visibleRows.map((row, index) => (
        <div key={index} style={listTagLineStyle}>
          {renderTags(row, index)}
        </div>
      ))}
    </div>
  );
}

function renderSkuCodeLine(item: API.ProductReview.ListDisplayItem) {
  return item.skuCode || item.sellerSkuCode || item.systemSkuCode || '--';
}

function renderChangedFieldTags(fields?: string[]) {
  if (!fields?.length) {
    return '--';
  }
  return fields.map((field) => (
    <Tag key={field} color={field === '供货价' ? 'red' : 'blue'}>
      {field}
    </Tag>
  ));
}

function renderPriceDirectionTag(direction?: string) {
  if (direction === 'UP') {
    return <Tag color="red">涨价</Tag>;
  }
  if (direction === 'DOWN') {
    return <Tag color="green">降价</Tag>;
  }
  if (direction === 'SAME') {
    return <Tag>无变化</Tag>;
  }
  return null;
}

function renderAfterSupplyPrice(item: API.ProductReview.ListDisplayItem) {
  const color = item.priceDirection === 'UP' ? '#cf1322' : item.priceDirection === 'DOWN' ? '#389e0d' : undefined;
  return (
    <Space size={4}>
      <Typography.Text style={{ color }}>
        {formatListMoney(item.afterSupplyPrice, item.currencyCode)}
      </Typography.Text>
      {renderPriceDirectionTag(item.priceDirection)}
    </Space>
  );
}

function renderSupplyPriceTransition(item: API.ProductReview.ListDisplayItem) {
  if (item.beforeSupplyPrice == null && item.afterSupplyPrice == null) {
    return '--';
  }
  return (
    <Space size={4}>
      <Typography.Text type="secondary">
        {formatListMoney(item.beforeSupplyPrice, item.currencyCode)}
      </Typography.Text>
      <span>→</span>
      {renderAfterSupplyPrice(item)}
    </Space>
  );
}

function renderModuleTags(record: API.ProductReview.Review) {
  const modules = getChangedModules(record);
  if (!modules.length) {
    return record.diffSummary || '--';
  }
  return (
    <Space wrap size={[4, 4]}>
      {modules.map((moduleName) => (
        <Tag key={moduleName} color={moduleName === '供货价' ? 'red' : 'blue'}>
          {moduleName}
        </Tag>
      ))}
    </Space>
  );
}

function renderCompactProduct(record: API.ProductReview.Review) {
  return (
    <Space orientation="vertical" size={2}>
      <Typography.Text strong>{record.productNameAfter || '--'}</Typography.Text>
      <Typography.Text type="secondary">{record.categoryName || '--'}</Typography.Text>
    </Space>
  );
}

function renderSeller(record: API.ProductReview.Review) {
  return (
    <Space orientation="vertical" size={2}>
      <span>{record.sellerName || '--'}</span>
      <Typography.Text type="secondary">{record.sellerId ?? '--'}</Typography.Text>
    </Space>
  );
}

function getDisplayWarehouse(record: API.ProductReview.Review) {
  if (isOfficialWarehouseKind(record.warehouseSummary)) {
    return '--';
  }
  const warehouses = Array.from(new Set(
    getListDisplayItems(record)
      .map((item) => item.afterWarehouseSummary || item.beforeWarehouseSummary)
      .filter(Boolean),
  ));
  return warehouses.length ? warehouses.join('、') : formatWarehouseKindLabel(record.warehouseSummary);
}

function shouldShowReviewTypeColumn(reviewTypeTab: string) {
  return reviewTypeTab === 'ALL';
}

function safeParseJson(value?: string) {
  if (!value) {
    return undefined;
  }
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return value;
  }
}

function localizeSnapshotScalar(key: string, value: unknown) {
  if (typeof value !== 'string') {
    return value;
  }
  switch (key) {
    case 'spuStatus':
    case 'skuStatus':
      return formatSalesStatusLabel(value);
    case 'controlStatus':
    case 'spuControlStatus':
      return formatControlStatusLabel(value);
    case 'warehouseKind':
    case 'warehouseKindSummary':
      return formatWarehouseKindLabel(value);
    case 'attributeType':
      return ATTRIBUTE_TYPE_TEXT[value] || '未知属性类型';
    case 'ownerType':
      return formatItemTypeLabel(value);
    case 'imageRole':
      return IMAGE_ROLE_TEXT[value] || '未知图片类型';
    case 'inventoryStatus':
      return inventoryStatusText[value] || '未知库存状态';
    case 'lockStatus':
      return LOCK_STATUS_TEXT[value] || '未知锁定状态';
    case 'bindingStatus':
      return BINDING_STATUS_TEXT[value] || '未知绑定状态';
    case 'valueCode':
      if (value === 'Y') return '是';
      if (value === 'N') return '否';
      return value;
    case 'snapshotRole':
      return formatSnapshotRoleLabel(value);
    case 'payloadType':
      return formatPayloadTypeLabel(value);
    case 'itemType':
      return formatItemTypeLabel(value);
    case 'changeType':
      return formatChangeTypeLabel(value);
    case 'reviewStatus':
    case 'itemStatus':
      return valueEnumText(DEFAULT_REVIEW_STATUS_ENUM, value, '未知状态');
    case 'reviewType':
      return valueEnumText(DEFAULT_REVIEW_TYPE_ENUM, value, '未知类型');
    case 'submitTerminal':
    case 'operatorTerminal':
      return valueEnumText(TERMINAL_VALUE_ENUM, value, '未知端');
    default:
      return value;
  }
}

function localizeSnapshotPayload(payload: unknown, key = ''): unknown {
  if (Array.isArray(payload)) {
    return payload.map((item) => localizeSnapshotPayload(item));
  }
  if (payload && typeof payload === 'object') {
    return Object.fromEntries(
      Object.entries(payload as Record<string, unknown>).map(([entryKey, entryValue]) => [
        entryKey,
        localizeSnapshotPayload(entryValue, entryKey),
      ]),
    );
  }
  return localizeSnapshotScalar(key, payload);
}

function stringifyPayload(payload: unknown) {
  if (payload == null || payload === '') {
    return '--';
  }
  if (typeof payload === 'string') {
    return payload;
  }
  try {
    return JSON.stringify(localizeSnapshotPayload(payload), null, 2);
  } catch {
    return String(payload);
  }
}

function renderPayloadPreview(snapshot?: API.ProductReview.Snapshot) {
  const payload = safeParseJson(snapshot?.payloadJson);
  return (
    <Typography.Paragraph style={{ marginBottom: 0 }} ellipsis={{ rows: 5, expandable: true }}>
      {stringifyPayload(payload)}
    </Typography.Paragraph>
  );
}

function renderReviewTypeTabLabel(label: string, count?: number) {
  return `${label}(${count ?? 0})`;
}

function normalizeReviewTypePendingCounts(data?: Record<string, number>) {
  const result: Record<string, number> = {};
  ['ALL', ...REVIEW_TYPE_ORDER].forEach((key) => {
    result[key] = Number(data?.[key] || 0);
  });
  return result;
}

function renderReviewImage(url?: string) {
  return url ? (
    <Image width={64} height={64} src={resolveResourceUrl(url)} style={{ objectFit: 'cover' }} />
  ) : (
    '--'
  );
}

function buildSnapshotCompareRows(
  review: API.ProductReview.Review,
  payloadTypes?: string[],
): SnapshotCompareRow[] {
  const itemById = new Map<number, API.ProductReview.Item>();
  (review.items || []).forEach((item) => {
    if (item.itemId != null) {
      itemById.set(item.itemId, item);
    }
  });

  const rows = new Map<string, SnapshotCompareRow>();
  (review.snapshots || [])
    .filter((snapshot) => !payloadTypes?.length || payloadTypes.includes(String(snapshot.payloadType || '')))
    .forEach((snapshot) => {
      const itemId = snapshot.itemId ?? -1;
      const payloadType = String(snapshot.payloadType || 'UNKNOWN');
      const key = `${itemId}:${payloadType}`;
      const row = rows.get(key) || {
        key,
        item: itemById.get(itemId),
        payloadType,
      };
      if (snapshot.snapshotRole === 'BEFORE') {
        row.before = snapshot;
      } else if (snapshot.snapshotRole === 'AFTER') {
        row.after = snapshot;
      }
      rows.set(key, row);
    });

  (review.items || []).forEach((item, index) => {
    const keyPrefix = `${item.itemId ?? `item-${index}`}:`;
    const hasSnapshotRow = Array.from(rows.keys()).some((key) => key.startsWith(keyPrefix));
    if (!hasSnapshotRow) {
      const payloadType = String(item.itemType || 'OBJECT');
      rows.set(`${keyPrefix}${payloadType}`, {
        key: `${keyPrefix}${payloadType}`,
        item,
        payloadType,
      });
    }
  });

  return Array.from(rows.values());
}

function renderSnapshotObject(row: SnapshotCompareRow) {
  const item = row.item;
  return (
    <Space orientation="vertical" size={2}>
      <span>{item?.itemType ? formatItemTypeLabel(item.itemType) : formatPayloadTypeLabel(row.payloadType)}</span>
      <Typography.Text type="secondary">{item?.systemSkuCode || '--'}</Typography.Text>
      {item?.sellerSkuCode ? <Typography.Text type="secondary">{item.sellerSkuCode}</Typography.Text> : null}
      {item?.diffSummary ? <Typography.Text type="secondary">{item.diffSummary}</Typography.Text> : null}
    </Space>
  );
}

function renderSnapshotCompareTable(
  review: API.ProductReview.Review,
  title: string,
  payloadTypes?: string[],
) {
  const rows = buildSnapshotCompareRows(review, payloadTypes);
  const compareColumns: ColumnsType<SnapshotCompareRow> = [
    {
      title: '对象',
      dataIndex: 'item',
      width: 220,
      render: (_, row) => renderSnapshotObject(row),
    },
    {
      title: '载荷',
      dataIndex: 'payloadType',
      width: 100,
      render: (value) => formatPayloadTypeLabel(String(value || '')),
    },
    {
      title: '变更前',
      dataIndex: 'before',
      render: (_, row) => renderPayloadPreview(row.before),
    },
    {
      title: '变更后',
      dataIndex: 'after',
      render: (_, row) => renderPayloadPreview(row.after),
    },
  ];

  return (
    <Space orientation="vertical" size={8} style={{ width: '100%' }}>
      <Typography.Text strong>{title}</Typography.Text>
      <Table
        rowKey="key"
        size="small"
        columns={compareColumns}
        dataSource={rows}
        pagination={false}
        scroll={{ x: 1100 }}
      />
    </Space>
  );
}

function renderTypeDetailPanel(review: API.ProductReview.Review) {
  const commonSummary = (
    <Descriptions size="small" bordered column={3}>
      <Descriptions.Item label="对象数">{formatCount(review.itemCount)}</Descriptions.Item>
      <Descriptions.Item label="SKU数">{formatCount(resolveReviewSkuCount(review), '个SKU')}</Descriptions.Item>
      <Descriptions.Item label="仓库类型">{formatWarehouseKindLabel(review.warehouseSummary)}</Descriptions.Item>
      <Descriptions.Item label="供货价区间">{formatPriceRange(review)}</Descriptions.Item>
      <Descriptions.Item label="供货价变化">{formatPriceTransition(review)}</Descriptions.Item>
      <Descriptions.Item label="币种">{review.currencySummary || '--'}</Descriptions.Item>
    </Descriptions>
  );

  switch (review.reviewType) {
    case 'NEW_PRODUCT':
      return (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions size="small" bordered column={3}>
            <Descriptions.Item label="新增SKU">{formatCount(resolveReviewSkuCount(review), '个SKU')}</Descriptions.Item>
            <Descriptions.Item label="供货价区间">{formatPriceRange(review)}</Descriptions.Item>
            <Descriptions.Item label="类目">{review.categoryName || '--'}</Descriptions.Item>
            <Descriptions.Item label="仓库类型">{formatWarehouseKindLabel(review.warehouseSummary)}</Descriptions.Item>
            <Descriptions.Item label="主图">{renderReviewImage(review.mainImageUrlAfter)}</Descriptions.Item>
          </Descriptions>
          {renderSnapshotCompareTable(review, '新增内容快照', ['SPU', 'SKU'])}
        </Space>
      );
    case 'ADD_SKU':
      return (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions size="small" bordered column={3}>
            <Descriptions.Item label="新增SKU">{formatCount(resolveReviewSkuCount(review), '个SKU')}</Descriptions.Item>
            <Descriptions.Item label="供货价区间">{formatPriceRange(review)}</Descriptions.Item>
            <Descriptions.Item label="系统SPU">{review.systemSpuCode || '--'}</Descriptions.Item>
            <Descriptions.Item label="商品标题">{review.productNameAfter || '--'}</Descriptions.Item>
            <Descriptions.Item label="仓库类型">{formatWarehouseKindLabel(review.warehouseSummary)}</Descriptions.Item>
          </Descriptions>
          {renderSnapshotCompareTable(review, '新增 SKU 快照', ['SKU'])}
        </Space>
      );
    case 'EDIT_PRODUCT_INFO':
      return (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions size="small" bordered column={2}>
            <Descriptions.Item label="原标题">{review.productNameBefore || '--'}</Descriptions.Item>
            <Descriptions.Item label="新标题">{review.productNameAfter || '--'}</Descriptions.Item>
            <Descriptions.Item label="原主图">{renderReviewImage(review.mainImageUrlBefore)}</Descriptions.Item>
            <Descriptions.Item label="新主图">{renderReviewImage(review.mainImageUrlAfter)}</Descriptions.Item>
            <Descriptions.Item label="主图变化">{formatMainImageChange(review)}</Descriptions.Item>
            <Descriptions.Item label="类目">{review.categoryName || '--'}</Descriptions.Item>
          </Descriptions>
          {renderSnapshotCompareTable(review, 'SPU 资料变更对比', ['SPU'])}
        </Space>
      );
    case 'EDIT_SKU_INFO':
      return (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions size="small" bordered column={3}>
            <Descriptions.Item label="影响SKU">{formatCount(resolveReviewSkuCount(review), '个SKU')}</Descriptions.Item>
            <Descriptions.Item label="供货价区间">{formatPriceRange(review)}</Descriptions.Item>
          </Descriptions>
          {renderSnapshotCompareTable(review, 'SKU 资料变更对比', ['SKU'])}
        </Space>
      );
    case 'EDIT_PRICE':
      return (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions size="small" bordered column={3}>
            <Descriptions.Item label="原供货价区间">{formatPriceRange(review, 'before')}</Descriptions.Item>
            <Descriptions.Item label="新供货价区间">{formatPriceRange(review, 'after')}</Descriptions.Item>
            <Descriptions.Item label="影响SKU">{formatCount(resolveReviewSkuCount(review), '个SKU')}</Descriptions.Item>
            <Descriptions.Item label="币种">{review.currencySummary || '--'}</Descriptions.Item>
          </Descriptions>
          {renderSnapshotCompareTable(review, '供货价变更对比', ['SKU', 'PRICE'])}
        </Space>
      );
    case 'EDIT_MIXED':
      return (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions size="small" bordered column={3}>
            <Descriptions.Item label="影响SKU">{formatCount(resolveReviewSkuCount(review), '个SKU')}</Descriptions.Item>
            <Descriptions.Item label="供货价变化">{formatPriceTransition(review)}</Descriptions.Item>
            <Descriptions.Item label="仓库类型">{formatWarehouseKindLabel(review.warehouseSummary)}</Descriptions.Item>
            <Descriptions.Item label="币种">{review.currencySummary || '--'}</Descriptions.Item>
          </Descriptions>
          {renderSnapshotCompareTable(review, '综合变更快照对比', ['SPU', 'SKU'])}
        </Space>
      );
    default:
      return (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          {commonSummary}
          {renderSnapshotCompareTable(review, '审核快照对比')}
        </Space>
      );
  }
}

function renderReviewBasicInfo(
  review: API.ProductReview.Review,
  reviewStatusValueEnum: Record<string, ValueEnumItem>,
  reviewTypeValueEnum: Record<string, ValueEnumItem>,
) {
  return (
    <Descriptions size="small" bordered column={2}>
      <Descriptions.Item label="审核单号">{review.reviewNo || '--'}</Descriptions.Item>
      <Descriptions.Item label="审核状态">
        {renderEnumTag(reviewStatusValueEnum, review.reviewStatus)}
      </Descriptions.Item>
      <Descriptions.Item label="审核类型">
        {renderEnumTag(reviewTypeValueEnum, review.reviewType)}
      </Descriptions.Item>
      <Descriptions.Item label="商品标题">{review.productNameAfter || '--'}</Descriptions.Item>
      <Descriptions.Item label="系统SPU">{review.systemSpuCode || '--'}</Descriptions.Item>
      <Descriptions.Item label="卖家">{review.sellerName || '--'}</Descriptions.Item>
      <Descriptions.Item label="类目">{review.categoryName || '--'}</Descriptions.Item>
      <Descriptions.Item label="提交人">{review.submitUserName || '--'}</Descriptions.Item>
      <Descriptions.Item label="提交时间">{review.submitTime || '--'}</Descriptions.Item>
      <Descriptions.Item label="审核人">{review.reviewerName || '--'}</Descriptions.Item>
      <Descriptions.Item label="审核时间">{review.reviewTime || '--'}</Descriptions.Item>
      <Descriptions.Item label="供货价区间">{formatPriceRange(review)}</Descriptions.Item>
      <Descriptions.Item label="仓库类型">{formatWarehouseKindLabel(review.warehouseSummary)}</Descriptions.Item>
      <Descriptions.Item label="审核原因" span={2}>
        {review.reviewReason || '--'}
      </Descriptions.Item>
    </Descriptions>
  );
}

function renderEnumTag(valueEnum: Record<string, ValueEnumItem>, value?: string) {
  const item = value ? valueEnum[value] : undefined;
  return <Tag color={item?.status}>{item?.text || (value ? '未知' : '--')}</Tag>;
}

function buildReviewListColumns(
  reviewTypeTab: string,
  context: ReviewListColumnContext,
): ProColumns<API.ProductReview.Review>[] {
  const {
    canQueryProductReview,
    canApproveProductReview,
    canRejectProductReview,
    reviewStatusValueEnum,
    reviewTypeValueEnum,
    openDetail,
    openAction,
  } = context;

  const searchColumns: ProColumns<API.ProductReview.Review>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '审核单号 / 系统SPU / 标题 / 卖家' },
    },
    {
      title: '提交时间',
      dataIndex: 'submitTimeRange',
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value: string[]) => ({
          'params[beginTime]': value?.[0],
          'params[endTime]': value?.[1],
        }),
      },
    },
  ];

  const productImageColumn: ProColumns<API.ProductReview.Review> = {
    title: '商品图',
    dataIndex: 'mainImageUrlAfter',
    search: false,
    width: 72,
    render: (_, record) =>
      record.mainImageUrlAfter ? (
        <Image
          width={44}
          height={44}
          src={resolveResourceUrl(record.mainImageUrlAfter)}
          style={{ objectFit: 'cover' }}
        />
      ) : (
        '--'
      ),
  };
  const systemSpuColumn: ProColumns<API.ProductReview.Review> = {
    title: '系统SPU',
    dataIndex: 'systemSpuCode',
    search: false,
    width: 170,
    render: (value) => value || '--',
  };
  const reviewTypeColumn: ProColumns<API.ProductReview.Review> = {
    title: '审核类型',
    dataIndex: 'reviewType',
    search: false,
    width: 120,
    render: (_, record) => renderEnumTag(reviewTypeValueEnum, record.reviewType),
  };
  const productColumn: ProColumns<API.ProductReview.Review> = {
    title: '商品',
    dataIndex: 'productNameAfter',
    width: 280,
    search: false,
    render: (_, record) => renderCompactProduct(record),
  };
  const categoryColumn: ProColumns<API.ProductReview.Review> = {
    title: '类目',
    dataIndex: 'categoryName',
    search: false,
    width: 140,
    render: (_, record) => record.categoryName || '--',
  };
  const skuCountColumn: ProColumns<API.ProductReview.Review> = {
    title: 'SKU数量',
    dataIndex: 'skuCount',
    search: false,
    width: 90,
    render: (_, record) => formatCount(resolveReviewSkuCount(record), '个'),
  };
  const supplyPriceRangeColumn: ProColumns<API.ProductReview.Review> = {
    title: '供货价区间',
    dataIndex: 'priceAfterMin',
    search: false,
    width: 150,
    render: (_, record) => formatPriceRange(record),
  };
  const warehouseKindColumn: ProColumns<API.ProductReview.Review> = {
    title: '仓库类型',
    dataIndex: 'warehouseSummary',
    search: false,
    width: 110,
    render: (_, record) => <Tag>{formatWarehouseKindLabel(record.warehouseSummary)}</Tag>,
  };
  const deliveryWarehouseColumn: ProColumns<API.ProductReview.Review> = {
    title: '发货仓库',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 180,
    render: (_, record) => getDisplayWarehouse(record),
  };
  const changedSkuColumn: ProColumns<API.ProductReview.Review> = {
    title: reviewTypeTab === 'ADD_SKU' ? '新增SKU' : '变更SKU',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 170,
    render: (_, record) => renderListLines(
      getListDisplayItems(record),
      renderSkuCodeLine,
      record.reviewType === 'EDIT_PRICE' ? '未识别供货价变更' : '--',
    ),
  };
  const specColumn: ProColumns<API.ProductReview.Review> = {
    title: '规格',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 300,
    render: (_, record) => renderListLines(getListDisplayItems(record), (item) => (
      renderSummaryTextLines(item.afterSpecSummary || item.beforeSpecSummary)
    )),
  };
  const skuSupplyPriceColumn: ProColumns<API.ProductReview.Review> = {
    title: '供货价',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 140,
    render: (_, record) => renderListLines(getListDisplayItems(record), (item) => (
      formatListMoney(item.afterSupplyPrice, item.currencyCode || record.currencySummary)
    )),
  };
  const beforeSupplyPriceColumn: ProColumns<API.ProductReview.Review> = {
    title: '原供货价',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 140,
    render: (_, record) => renderListLines(getListDisplayItems(record), (item) => (
      formatListMoney(item.beforeSupplyPrice, item.currencyCode || record.currencySummary)
    )),
  };
  const afterSupplyPriceColumn: ProColumns<API.ProductReview.Review> = {
    title: '新供货价',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 170,
    render: (_, record) => renderListLines(getListDisplayItems(record), (item) => renderAfterSupplyPrice({
      ...item,
      currencyCode: item.currencyCode || record.currencySummary,
    })),
  };
  const currencyColumn: ProColumns<API.ProductReview.Review> = {
    title: '币种',
    dataIndex: 'currencySummary',
    search: false,
    width: 90,
    render: (_, record) => renderListLines(getListDisplayItems(record), (item) => (
      item.currencyCode || record.currencySummary || '--'
    )),
  };
  const skuWarehouseColumn: ProColumns<API.ProductReview.Review> = {
    title: '仓库',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 180,
    render: (_, record) => renderListLines(getListDisplayItems(record), (item) => (
      item.afterWarehouseSummary || item.beforeWarehouseSummary || formatWarehouseKindLabel(record.warehouseSummary)
    )),
  };
  const changedFieldsColumn: ProColumns<API.ProductReview.Review> = {
    title: '变更字段',
    dataIndex: 'listChangedModules',
    search: false,
    width: 220,
    render: (_, record) => (
      record.reviewType === 'EDIT_PRODUCT_INFO'
        ? renderModuleTags(record)
        : renderTagLines(getListDisplayItems(record), (item) => renderChangedFieldTags(item.changedFieldNames))
    ),
  };
  const changedModulesColumn: ProColumns<API.ProductReview.Review> = {
    title: '变更模块',
    dataIndex: 'listChangedModules',
    search: false,
    width: 180,
    render: (_, record) => renderModuleTags(record),
  };
  const affectedSkuColumn: ProColumns<API.ProductReview.Review> = {
    title: '影响SKU',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 170,
    render: (_, record) => renderListLines(getListDisplayItems(record), renderSkuCodeLine),
  };
  const supplyPriceChangeColumn: ProColumns<API.ProductReview.Review> = {
    title: '供货价变化',
    dataIndex: 'listDisplayItems',
    search: false,
    width: 280,
    render: (_, record) => {
      const priceItems = getListDisplayItems(record).filter((item) => item.changedFieldNames?.includes('供货价'));
      return renderListLines(priceItems, (item) => renderSupplyPriceTransition({
        ...item,
        currencyCode: item.currencyCode || record.currencySummary,
      }));
    },
  };
  const sellerColumn: ProColumns<API.ProductReview.Review> = {
    title: '卖家',
    dataIndex: 'sellerName',
    width: 160,
    search: false,
    render: (_, record) => renderSeller(record),
  };
  const reviewStatusColumn: ProColumns<API.ProductReview.Review> = {
    title: '审核状态',
    dataIndex: 'reviewStatus',
    valueType: 'select',
    valueEnum: reviewStatusValueEnum,
    fieldProps: SEARCHABLE_SELECT_PROPS,
    width: 100,
    render: (_, record) => renderEnumTag(reviewStatusValueEnum, record.reviewStatus),
  };
  const submitUserColumn: ProColumns<API.ProductReview.Review> = {
    title: '提交人',
    dataIndex: 'submitUserName',
    search: false,
    width: 110,
  };
  const submitTimeColumn: ProColumns<API.ProductReview.Review> = {
    title: '提交时间',
    dataIndex: 'submitTime',
    search: false,
    width: 170,
  };
  const reviewerColumn: ProColumns<API.ProductReview.Review> = {
    title: '审核人',
    dataIndex: 'reviewerName',
    search: false,
    width: 110,
    render: (_, record) => record.reviewerName || '--',
  };
  const reviewTimeColumn: ProColumns<API.ProductReview.Review> = {
    title: '审核时间',
    dataIndex: 'reviewTime',
    search: false,
    width: 170,
    render: (_, record) => record.reviewTime || '--',
  };
  const actionColumn: ProColumns<API.ProductReview.Review> = {
    title: '操作',
    valueType: 'option',
    fixed: 'right',
    width: 170,
    render: (_, record) => {
      const pending = record.reviewStatus === 'PENDING';
      return [
        <Button
          key="detail"
          type="link"
          size="small"
          hidden={!canQueryProductReview}
          onClick={() => openDetail(record)}
        >
          详情
        </Button>,
        <Button
          key="approve"
          type="link"
          size="small"
          hidden={!pending || !canApproveProductReview}
          onClick={() => openAction('APPROVE', record)}
        >
          通过
        </Button>,
        <Button
          key="reject"
          type="link"
          danger
          size="small"
          hidden={!pending || !canRejectProductReview}
          onClick={() => openAction('REJECT', record)}
        >
          驳回
        </Button>,
      ];
    },
  };

  const baseColumns = [
    productImageColumn,
    systemSpuColumn,
    ...(shouldShowReviewTypeColumn(reviewTypeTab) ? [reviewTypeColumn] : []),
    productColumn,
  ];
  const commonTailColumns = [sellerColumn, reviewStatusColumn, submitUserColumn, submitTimeColumn, actionColumn];

  if (reviewTypeTab === 'NEW_PRODUCT') {
    return [
      ...searchColumns,
      ...baseColumns,
      categoryColumn,
      skuCountColumn,
      supplyPriceRangeColumn,
      warehouseKindColumn,
      deliveryWarehouseColumn,
      ...commonTailColumns,
    ];
  }
  if (reviewTypeTab === 'ADD_SKU') {
    return [
      ...searchColumns,
      ...baseColumns,
      changedSkuColumn,
      specColumn,
      skuSupplyPriceColumn,
      skuWarehouseColumn,
      ...commonTailColumns,
    ];
  }
  if (reviewTypeTab === 'EDIT_PRODUCT_INFO') {
    return [
      ...searchColumns,
      ...baseColumns,
      changedFieldsColumn,
      warehouseKindColumn,
      ...commonTailColumns,
    ];
  }
  if (reviewTypeTab === 'EDIT_SKU_INFO') {
    return [
      ...searchColumns,
      ...baseColumns,
      changedSkuColumn,
      changedFieldsColumn,
      ...commonTailColumns,
    ];
  }
  if (reviewTypeTab === 'EDIT_PRICE') {
    return [
      ...searchColumns,
      ...baseColumns,
      changedSkuColumn,
      beforeSupplyPriceColumn,
      afterSupplyPriceColumn,
      currencyColumn,
      ...commonTailColumns,
    ];
  }
  if (reviewTypeTab === 'EDIT_MIXED') {
    return [
      ...searchColumns,
      ...baseColumns,
      changedModulesColumn,
      affectedSkuColumn,
      supplyPriceChangeColumn,
      ...commonTailColumns,
    ];
  }

  return [
    ...searchColumns,
    ...baseColumns,
    sellerColumn,
    warehouseKindColumn,
    reviewStatusColumn,
    submitUserColumn,
    submitTimeColumn,
    reviewerColumn,
    reviewTimeColumn,
    actionColumn,
  ];
}

const ProductReviewPage = () => {
  const access = useAccess();
  const actionRef = useRef<ActionType>(null);
  const [reviewTypeTab, setReviewTypeTab] = useState('ALL');
  const [currentReview, setCurrentReview] = useState<API.ProductReview.Review>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [actionState, setActionState] = useState<ActionState>({ open: false, kind: 'APPROVE' });
  const [actionLoading, setActionLoading] = useState(false);
  const [actionForm] = Form.useForm<{ reason?: string }>();
  const [reviewTypeValueEnum, setReviewTypeValueEnum] =
    useState<Record<string, ValueEnumItem>>(DEFAULT_REVIEW_TYPE_ENUM);
  const [reviewStatusValueEnum, setReviewStatusValueEnum] =
    useState<Record<string, ValueEnumItem>>(DEFAULT_REVIEW_STATUS_ENUM);
  const [reviewTypePendingCounts, setReviewTypePendingCounts] = useState<Record<string, number>>({});

  const canListProductReview = access.hasPerms('review:productDistribution:list');
  const canQueryProductReview = access.hasPerms('review:productDistribution:query');
  const canApproveProductReview = access.hasPerms('review:productDistribution:approve');
  const canRejectProductReview = access.hasPerms('review:productDistribution:reject');
  const canViewProductReviewLog = access.hasPerms('review:productDistribution:log');
  const canPreviewCategorySchema = access.hasPerms('product:categoryAttribute:preview');

  const refreshReviewTypePendingCounts = useCallback(async () => {
    if (!canListProductReview) {
      setReviewTypePendingCounts({});
      return;
    }

    try {
      const resp = await getProductReviewPendingCounts();
      setReviewTypePendingCounts(resp.code === 200 ? normalizeReviewTypePendingCounts(resp.data) : {});
    } catch {
      setReviewTypePendingCounts({});
    }
  }, [canListProductReview]);

  useEffect(() => {
    getDictValueEnum('product_review_type').then((data) => {
      setReviewTypeValueEnum(normalizeReviewTypeValueEnum(data as Record<string, ValueEnumItem>));
    });
    getDictValueEnum('product_review_status').then((data) => {
      setReviewStatusValueEnum(normalizeValueEnum(data as Record<string, ValueEnumItem>, DEFAULT_REVIEW_STATUS_ENUM));
    });
  }, []);

  useEffect(() => {
    refreshReviewTypePendingCounts();
  }, [refreshReviewTypePendingCounts]);

  const reviewTypeTabs = useMemo(
    () => [
      { key: 'ALL', label: renderReviewTypeTabLabel('全部', reviewTypePendingCounts.ALL) },
      ...REVIEW_TYPE_ORDER.map((key) => ({
        key,
        label: renderReviewTypeTabLabel(valueEnumText(reviewTypeValueEnum, key), reviewTypePendingCounts[key]),
      })),
    ],
    [reviewTypePendingCounts, reviewTypeValueEnum],
  );

  const reload = () => actionRef.current?.reload();

  const openDetail = async (record: API.ProductReview.Review) => {
    if (!canQueryProductReview || record.reviewId == null) {
      message.warning('缺少商品审核详情权限');
      return;
    }
    const resp = await getProductReview(record.reviewId);
    if (resp.code === 200) {
      const review: API.ProductReview.Review = { ...resp.data, logs: [] };
      if (canViewProductReviewLog) {
        const logsResp = await getProductReviewLogs(record.reviewId);
        if (logsResp.code === 200) {
          review.logs = logsResp.data || [];
        } else {
          message.error(logsResp.msg || '审核日志加载失败');
        }
      }
      setCurrentReview(review);
      setDetailOpen(true);
    } else {
      message.error(resp.msg || '审核详情加载失败');
    }
  };

  const canHandleReviewAction = (kind: ActionKind, record?: API.ProductReview.Review) => {
    if (!record || record.reviewId == null) {
      message.warning('缺少商品审核单');
      return false;
    }
    if (record.reviewStatus !== 'PENDING') {
      message.warning('当前审核单已处理');
      return false;
    }
    if (kind === 'APPROVE' && !canApproveProductReview) {
      message.warning('缺少商品审核通过权限');
      return false;
    }
    if (kind === 'REJECT' && !canRejectProductReview) {
      message.warning('缺少商品审核驳回权限');
      return false;
    }
    return true;
  };

  const openAction = (kind: ActionKind, record: API.ProductReview.Review) => {
    if (!canHandleReviewAction(kind, record)) {
      return;
    }
    setActionState({ open: true, kind, review: record });
    actionForm.resetFields();
  };

  const closeAction = () => {
    setActionState((prev) => ({ ...prev, open: false }));
    setActionLoading(false);
  };

  const submitAction = async () => {
    if (!canHandleReviewAction(actionState.kind, actionState.review)) {
      return;
    }
    const reviewId = actionState.review?.reviewId;
    if (reviewId == null) {
      return;
    }
    const values = await actionForm.validateFields();
    setActionLoading(true);
    try {
      const ok = resultOk(
        actionState.kind === 'APPROVE'
          ? await approveProductReview(reviewId, values.reason)
          : await rejectProductReview(reviewId, values.reason || ''),
        actionState.kind === 'APPROVE' ? '审核已通过' : '审核已驳回',
      );
      if (ok) {
        closeAction();
        setDetailOpen(false);
        setCurrentReview(undefined);
        reload();
        refreshReviewTypePendingCounts();
      }
    } finally {
      setActionLoading(false);
    }
  };

  const columns = buildReviewListColumns(reviewTypeTab, {
    canQueryProductReview,
    canApproveProductReview,
    canRejectProductReview,
    reviewStatusValueEnum,
    reviewTypeValueEnum,
    openDetail,
    openAction,
  });

  const itemColumns: ColumnsType<API.ProductReview.Item> = [
    {
      title: '对象',
      dataIndex: 'itemType',
      width: 90,
      render: (value) => formatItemTypeLabel(String(value || '')),
    },
    { title: '系统SKU', dataIndex: 'systemSkuCode', width: 160, render: (value) => value || '--' },
    { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 160, render: (value) => value || '--' },
    {
      title: '变化',
      dataIndex: 'changeType',
      width: 120,
      render: (value) => formatChangeTypeLabel(String(value || '')),
    },
    {
      title: '状态',
      dataIndex: 'itemStatus',
      width: 100,
      render: (value) => valueEnumText(reviewStatusValueEnum, String(value || '')),
    },
    { title: '摘要', dataIndex: 'diffSummary', ellipsis: true },
  ];

  const snapshotColumns: ColumnsType<API.ProductReview.Snapshot> = [
    { title: '快照', dataIndex: 'snapshotRole', width: 90, render: (value) => formatSnapshotRoleLabel(String(value || '')) },
    { title: '载荷', dataIndex: 'payloadType', width: 120, render: (value) => formatPayloadTypeLabel(String(value || '')) },
    { title: 'Hash', dataIndex: 'payloadHash', width: 220, ellipsis: true },
    {
      title: '内容',
      dataIndex: 'payloadJson',
      render: (_, record) => renderPayloadPreview(record),
    },
  ];

  const logColumns: ColumnsType<API.ProductReview.OperationLog> = [
    {
      title: '操作',
      dataIndex: 'operationType',
      width: 100,
      render: (value) => formatOperationTypeLabel(String(value || '')),
    },
    { title: '前状态', dataIndex: 'beforeStatus', width: 110, render: (value) => formatReviewOrSalesStatusLabel(String(value || '')) },
    { title: '后状态', dataIndex: 'afterStatus', width: 110, render: (value) => formatReviewOrSalesStatusLabel(String(value || '')) },
    { title: '操作人', dataIndex: 'operatorName', width: 120 },
    { title: '操作时间', dataIndex: 'operationTime', width: 170 },
    { title: '原因', dataIndex: 'reason', ellipsis: true, render: (value) => value || '--' },
  ];

  const detailTabs = [
    {
      key: 'focus',
      label: '变更预览',
      children: currentReview
        ? (
            <ProductReviewBusinessPreview
              review={currentReview}
              canPreviewCategorySchema={canPreviewCategorySchema}
            />
          )
        : null,
    },
    {
      key: 'basic',
      label: '审核基础信息',
      children: currentReview
        ? renderReviewBasicInfo(
            currentReview,
            reviewStatusValueEnum,
            reviewTypeValueEnum,
          )
        : null,
    },
    {
      key: 'items',
      label: '审计对象',
      children: (
        <Table
          rowKey="itemId"
          size="small"
          columns={itemColumns}
          dataSource={currentReview?.items || []}
          pagination={false}
          scroll={{ x: 900 }}
        />
      ),
    },
    {
      key: 'snapshots',
      label: '审计快照',
      children: (
        <Table
          rowKey="snapshotId"
          size="small"
          columns={snapshotColumns}
          dataSource={currentReview?.snapshots || []}
          pagination={false}
          scroll={{ x: 1100 }}
        />
      ),
    },
    ...(canViewProductReviewLog
      ? [
          {
            key: 'logs',
            label: '操作日志',
            children: (
              <Table
                rowKey="logId"
                size="small"
                columns={logColumns}
                dataSource={currentReview?.logs || []}
                pagination={false}
                scroll={{ x: 900 }}
              />
            ),
          },
        ]
      : []),
  ];

  return (
    <PageContainer title={false}>
      <Tabs
        activeKey={reviewTypeTab}
        items={reviewTypeTabs}
        style={{ flex: 'none' }}
        tabBarStyle={{ marginBottom: 8 }}
        onChange={setReviewTypeTab}
      />
      <ProTable<API.ProductReview.Review>
        className="urili-fill-table"
        actionRef={actionRef}
        rowKey="reviewId"
        columns={columns}
        scroll={getProTableScroll(TABLE_SCROLL_X)}
        tableLayout="fixed"
        search={getPersistedProTableSearch({ labelWidth: 90, fieldCount: 4 }, 'product-review')}
        form={{
          initialValues: { reviewStatus: DEFAULT_PRODUCT_REVIEW_LIST_STATUS },
        }}
        pagination={getProTablePagination(20)}
        params={{ reviewTypeTab }}
        beforeSearchSubmit={(params) => ({
          ...params,
          reviewStatus: params.reviewStatus || DEFAULT_PRODUCT_REVIEW_LIST_STATUS,
        })}
        request={async ({ current, pageSize, ...params }) => {
          if (!canListProductReview) {
            return { data: [], total: 0, success: false };
          }
          const { reviewTypeTab: activeReviewTypeTab, ...queryParams } = params;
          delete (queryParams as Record<string, unknown>).reviewType;
          const resp = await getProductReviewList({
            ...queryParams,
            reviewStatus:
              (queryParams as Record<string, unknown>).reviewStatus || DEFAULT_PRODUCT_REVIEW_LIST_STATUS,
            ...(activeReviewTypeTab && activeReviewTypeTab !== 'ALL'
              ? { reviewType: activeReviewTypeTab }
              : {}),
            pageNum: current,
            pageSize,
          });
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
        toolBarRender={false}
      />

      <Drawer
        title="商品审核详情"
        open={detailOpen}
        size={1280}
        onClose={() => setDetailOpen(false)}
        footer={currentReview?.reviewStatus === 'PENDING' ? (
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={() => setDetailOpen(false)}>关闭</Button>
            <Button
              hidden={!canRejectProductReview}
              danger
              onClick={() => openAction('REJECT', currentReview)}
            >
              驳回
            </Button>
            <Button
              hidden={!canApproveProductReview}
              type="primary"
              onClick={() => openAction('APPROVE', currentReview)}
            >
              通过审核
            </Button>
          </Space>
        ) : null}
      >
        {currentReview ? (
          <Tabs items={detailTabs} />
        ) : null}
      </Drawer>

      <Modal
        title={actionState.kind === 'APPROVE' ? '审核通过' : '审核驳回'}
        open={actionState.open}
        confirmLoading={actionLoading}
        okText={actionState.kind === 'APPROVE' ? '通过' : '驳回'}
        okButtonProps={{ danger: actionState.kind === 'REJECT' }}
        onOk={submitAction}
        onCancel={closeAction}
        destroyOnHidden
      >
        <Form form={actionForm} layout="vertical">
          <Form.Item
            name="reason"
            label={actionState.kind === 'APPROVE' ? '审核说明' : '驳回原因'}
            rules={actionState.kind === 'REJECT' ? [{ required: true, message: '请填写驳回原因' }] : []}
          >
            <Input.TextArea rows={4} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default ProductReviewPage;
