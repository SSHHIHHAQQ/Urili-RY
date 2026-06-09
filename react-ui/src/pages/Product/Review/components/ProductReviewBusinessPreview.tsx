import {
  Collapse,
  Descriptions,
  Divider,
  Empty,
  Image,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { type ReactNode, useEffect, useMemo, useState } from 'react';
import { getCategorySchema } from '@/services/product/product';
import {
  buildAttributeLabelMap,
  buildOptionLabelMap,
  formatAttributeValue,
  resolveAttributeLabel,
} from '@/pages/Product/utils/attributeDisplay';
import DetailContentPreview from '../../Distribution/components/DetailContentPreview';
import { getSalesStatusText, resolveResourceUrl, warehouseKindText } from '../../Distribution/constants';

type SnapshotPayload<T> = {
  key: string;
  item?: API.ProductReview.Item;
  snapshot?: API.ProductReview.Snapshot;
  payload?: T;
};

type SkuPair = {
  key: string;
  item?: API.ProductReview.Item;
  before?: API.ProductDistribution.Sku;
  after?: API.ProductDistribution.Sku;
};

type ProductReviewBusinessPreviewProps = {
  review: API.ProductReview.Review;
};

type ChangeRow = {
  key: string;
  label: string;
  before?: ReactNode;
  after?: ReactNode;
  changed: boolean;
};

type SkuFieldChange = {
  key: string;
  label: string;
  beforeRaw?: unknown;
  afterRaw?: unknown;
  before: ReactNode;
  after: ReactNode;
  changed: boolean;
};

type AttributeDisplayMaps = {
  attributeLabelMap: Map<string, string>;
  optionLabelMap: Map<string, string>;
};

type CompareTone = 'same' | 'before' | 'after' | 'empty';

type CompareField = {
  key: string;
  before: ReactNode;
  after: ReactNode;
};

type EditChangeScope = 'PRODUCT_INFO' | 'ADD_SKU' | 'SKU_INFO' | 'SUPPLY_PRICE';

const sectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: 12,
} as const;

const stackStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: 16,
} as const;

const inlineGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
  gap: 12,
} as const;

const compactTagWrapStyle = {
  display: 'flex',
  flexWrap: 'wrap',
  gap: 6,
} as const;

const compareTableStyle = {
  border: '1px solid #f0f0f0',
  borderRadius: 6,
  background: '#fff',
  overflow: 'hidden',
} as const;

const compareHeaderRowStyle = {
  display: 'grid',
  gridTemplateColumns: 'minmax(0, 1fr) minmax(0, 1fr)',
  background: '#fafafa',
  borderBottom: '1px solid #f0f0f0',
} as const;

const compareHeaderCellStyle = {
  padding: '8px 12px',
  fontWeight: 600,
} as const;

const compareRowStyle = {
  display: 'grid',
  gridTemplateColumns: 'minmax(0, 1fr) minmax(0, 1fr)',
  alignItems: 'stretch',
} as const;

const compareCellStyle = {
  minWidth: 0,
  padding: 12,
  display: 'flex',
} as const;

const compareFieldStyle = {
  border: '1px solid #f0f0f0',
  borderRadius: 6,
  padding: '8px 10px',
  minHeight: 48,
  height: '100%',
  width: '100%',
  boxSizing: 'border-box',
  overflowWrap: 'anywhere',
  background: '#fff',
} as const;

const skuCompareCardStyle = {
  border: '1px solid #f0f0f0',
  borderRadius: 6,
  background: '#fff',
  overflow: 'hidden',
} as const;

const skuCompareHeaderStyle = {
  display: 'grid',
  gridTemplateColumns: 'auto minmax(0, 1fr) auto',
  gap: 12,
  alignItems: 'center',
  padding: 12,
  background: '#fafafa',
  borderBottom: '1px solid #f0f0f0',
} as const;

const salesStatusColor: Record<string, string> = {
  DRAFT: 'default',
  READY: 'warning',
  ON_SALE: 'success',
  OFF_SALE: 'processing',
  DISABLED: 'error',
};

function parsePayload<T>(snapshot?: API.ProductReview.Snapshot) {
  if (!snapshot?.payloadJson) {
    return undefined;
  }
  try {
    return JSON.parse(snapshot.payloadJson) as T;
  } catch {
    return undefined;
  }
}

function getItemKey(item?: API.ProductReview.Item, snapshot?: API.ProductReview.Snapshot, index = 0) {
  return String(item?.itemId ?? snapshot?.itemId ?? snapshot?.snapshotId ?? index);
}

function findReviewItem(review: API.ProductReview.Review, itemId?: number) {
  return (review.items || []).find((item) => item.itemId === itemId);
}

function getSnapshotPayloads<T>(
  review: API.ProductReview.Review,
  role: string,
  payloadType: string,
): SnapshotPayload<T>[] {
  return (review.snapshots || [])
    .filter((snapshot) => snapshot.snapshotRole === role && snapshot.payloadType === payloadType)
    .map((snapshot, index) => ({
      key: getItemKey(findReviewItem(review, snapshot.itemId), snapshot, index),
      item: findReviewItem(review, snapshot.itemId),
      snapshot,
      payload: parsePayload<T>(snapshot),
    }))
    .filter((item) => !!item.payload);
}

function getProductSnapshot(
  review: API.ProductReview.Review,
  role: string,
): API.ProductDistribution.Spu | undefined {
  return getSnapshotPayloads<API.ProductDistribution.Spu>(review, role, 'SPU')[0]?.payload;
}

function getAfterProduct(review: API.ProductReview.Review) {
  return getProductSnapshot(review, 'AFTER');
}

function getBeforeProduct(review: API.ProductReview.Review) {
  return getProductSnapshot(review, 'BEFORE');
}

function getReviewCategoryId(review: API.ProductReview.Review) {
  return getAfterProduct(review)?.categoryId || getBeforeProduct(review)?.categoryId || review.categoryId;
}

function getProductWarehouseKind(product?: API.ProductDistribution.Spu, review?: API.ProductReview.Review) {
  return product?.warehouseKind || product?.warehouseKindSummary || review?.warehouseSummary;
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

function renderSalesStatus(value?: string) {
  return <Tag color={value ? salesStatusColor[value] || 'default' : 'default'}>{formatSalesStatusLabel(value)}</Tag>;
}

function formatSkuWarehouseValue(sku?: API.ProductDistribution.Sku) {
  return sku?.sourceWarehouseNames || formatWarehouseKindLabel(sku?.warehouseKindSummary);
}

function shouldShowDeliveryWarehouse(product?: API.ProductDistribution.Spu, review?: API.ProductReview.Review) {
  return normalizeWarehouseKind(getProductWarehouseKind(product, review)) !== 'official';
}

function getSkuPairs(review: API.ProductReview.Review): SkuPair[] {
  const pairMap = new Map<string, SkuPair>();

  (review.snapshots || [])
    .filter((snapshot) => snapshot.payloadType === 'SKU')
    .forEach((snapshot, index) => {
      const item = findReviewItem(review, snapshot.itemId);
      const key = getItemKey(item, snapshot, index);
      const pair = pairMap.get(key) || { key, item };
      const payload = parsePayload<API.ProductDistribution.Sku>(snapshot);
      if (snapshot.snapshotRole === 'BEFORE') {
        pair.before = payload;
      }
      if (snapshot.snapshotRole === 'AFTER') {
        pair.after = payload;
      }
      pairMap.set(key, pair);
    });

  return Array.from(pairMap.values());
}

function getAfterSkus(review: API.ProductReview.Review) {
  const productSkus = getAfterProduct(review)?.skus || [];
  if (productSkus.length) {
    return productSkus;
  }
  return getSkuPairs(review)
    .map((pair) => pair.after)
    .filter((sku): sku is API.ProductDistribution.Sku => !!sku);
}

function valueText(value?: ReactNode) {
  if (value == null || value === '') {
    return '--';
  }
  return value;
}

function formatMoney(value?: number, currency?: string) {
  if (value == null) {
    return '--';
  }
  return `${value}${currency ? ` ${currency}` : ''}`;
}

function formatRange(min?: number, max?: number, currency?: string) {
  if (min == null && max == null) {
    return '--';
  }
  if (min != null && max != null && min !== max) {
    return `${formatMoney(min, currency)} - ${formatMoney(max, currency)}`;
  }
  return formatMoney(min ?? max, currency);
}

function formatDimension(sku?: API.ProductDistribution.Sku) {
  if (!sku) {
    return '--';
  }
  const legacy = [sku.lengthValue, sku.widthValue, sku.heightValue].filter(Boolean).join(' x ');
  const measured = [sku.measureLengthCm, sku.measureWidthCm, sku.measureHeightCm]
    .filter((item) => item != null)
    .join(' x ');
  const size = measured || legacy;
  const weight = sku.measureWeightKg != null ? `${sku.measureWeightKg}kg` : sku.weight;
  if (size && weight) {
    return `${size} / ${weight}`;
  }
  return size || weight || '--';
}

function formatSkuSpecs(sku?: API.ProductDistribution.Sku) {
  if (!sku) {
    return '--';
  }
  return [
    sku.color ? `颜色：${sku.color}` : '',
    sku.size ? `尺码：${sku.size}` : '',
    sku.material ? `材质：${sku.material}` : '',
    sku.style ? `款式：${sku.style}` : '',
    sku.model ? `型号：${sku.model}` : '',
    sku.capacity ? `容量：${sku.capacity}` : '',
  ].filter(Boolean).join(' / ') || '--';
}

function formatSkuIdentity(sku?: API.ProductDistribution.Sku, item?: API.ProductReview.Item) {
  return sku?.sellerSkuCode || sku?.systemSkuCode || item?.sellerSkuCode || item?.systemSkuCode || '--';
}

function formatSkuIdentityTitle(pair: SkuPair) {
  const sku = pair.after || pair.before;
  const sellerSku = sku?.sellerSkuCode || pair.item?.sellerSkuCode;
  const systemSku = sku?.systemSkuCode || pair.item?.systemSkuCode;
  const values = [sellerSku, systemSku].filter(Boolean);
  return values.length ? values.join(' / ') : '--';
}

function formatMeasurementValue(value?: unknown, defaultUnit = '') {
  if (value == null || value === '') {
    return '--';
  }
  if (typeof value === 'number') {
    return `${trimTrailingZeros(value.toFixed(2))}${defaultUnit ? ` ${defaultUnit}` : ''}`;
  }
  if (typeof value === 'string') {
    const normalized = normalizeMeasurementString(value, defaultUnit);
    const match = normalized.match(/^(-?\d+(?:\.\d+)?)\s*(.*)$/);
    if (match) {
      return `${trimTrailingZeros(Number(match[1]).toFixed(2))}${match[2] ? ` ${match[2]}` : ''}`;
    }
    return value || '--';
  }
  return String(value);
}

function getSkuMeasurementRaw(
  sku: API.ProductDistribution.Sku | undefined,
  measuredKey: keyof API.ProductDistribution.Sku,
  legacyKey: keyof API.ProductDistribution.Sku,
  defaultUnit: string,
) {
  const measuredValue = sku?.[measuredKey];
  if (measuredValue != null && measuredValue !== '') {
    return normalizeMeasurementString(String(measuredValue), defaultUnit);
  }
  const legacyValue = sku?.[legacyKey];
  return legacyValue == null || legacyValue === '' ? '' : normalizeMeasurementString(String(legacyValue), defaultUnit);
}

function renderSkuMeasurementValue(
  sku: API.ProductDistribution.Sku | undefined,
  measuredKey: keyof API.ProductDistribution.Sku,
  legacyKey: keyof API.ProductDistribution.Sku,
  defaultUnit: string,
) {
  const measuredValue = sku?.[measuredKey];
  if (measuredValue != null && measuredValue !== '') {
    return renderSkuValue(formatMeasurementValue(measuredValue, defaultUnit));
  }
  return renderSkuValue(formatMeasurementValue(sku?.[legacyKey], defaultUnit));
}

function renderImage(url?: string, size = 72) {
  return url ? (
    <Image
      width={size}
      height={size}
      src={resolveResourceUrl(url)}
      style={{ objectFit: 'cover', borderRadius: 4 }}
    />
  ) : (
    <Typography.Text type="secondary">未上传</Typography.Text>
  );
}

function trimTrailingZeros(value: string) {
  return value.replace(/(\.\d*?[1-9])0+$/, '$1').replace(/\.0+$/, '');
}

function normalizeMeasurementString(value: string, defaultUnit = '') {
  const normalized = value.trim().replace(/\s+/g, ' ');
  const match = normalized.match(/^(-?\d+(?:\.\d+)?)\s*([a-zA-Z%°\u4e00-\u9fa5]+)?$/);
  if (!match) {
    return normalized;
  }
  const unit = (match[2] || defaultUnit).trim().toLowerCase();
  return `${Number(match[1])}${unit ? ` ${unit}` : ''}`;
}

function normalizeCompareValue(value: unknown): unknown {
  if (value == null || value === '') {
    return '';
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? Number(value.toFixed(6)) : value;
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) {
      return '';
    }
    return normalizeMeasurementString(trimmed);
  }
  if (Array.isArray(value)) {
    return value.map(normalizeCompareValue);
  }
  if (typeof value === 'object') {
    return Object.keys(value as Record<string, unknown>)
      .sort()
      .reduce<Record<string, unknown>>((result, key) => {
        result[key] = normalizeCompareValue((value as Record<string, unknown>)[key]);
        return result;
      }, {});
  }
  return value;
}

function normalizeText(value: unknown) {
  const normalized = normalizeCompareValue(value);
  return typeof normalized === 'string' ? normalized : JSON.stringify(normalized);
}

function isDifferent(before: unknown, after: unknown) {
  return normalizeText(before) !== normalizeText(after);
}

function renderSection(title: string, children: ReactNode, extra?: ReactNode) {
  return (
    <section style={sectionStyle}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center' }}>
        <Typography.Title level={5} style={{ margin: 0 }}>{title}</Typography.Title>
        {extra}
      </div>
      {children}
    </section>
  );
}

function renderMetric(label: string, value: ReactNode, color?: string) {
  return (
    <div style={{ padding: 12, border: '1px solid #f0f0f0', borderRadius: 6, background: '#fff' }}>
      <Typography.Text type="secondary">{label}</Typography.Text>
      <div style={{ marginTop: 6, fontSize: 18, fontWeight: 600, color }}>{valueText(value)}</div>
    </div>
  );
}

function compareToneStyle(tone: CompareTone) {
  if (tone === 'before') {
    return {
      ...compareFieldStyle,
      borderColor: '#ffccc7',
      background: '#fff1f0',
    };
  }
  if (tone === 'after') {
    return {
      ...compareFieldStyle,
      borderColor: '#b7eb8f',
      background: '#f6ffed',
    };
  }
  if (tone === 'empty') {
    return {
      ...compareFieldStyle,
      borderStyle: 'dashed',
      background: '#fafafa',
    };
  }
  return compareFieldStyle;
}

function renderCompareFieldBlock(label: string, value: ReactNode, tone: CompareTone = 'same') {
  return (
    <div style={compareToneStyle(tone)}>
      <div style={{ marginBottom: 4 }}>
        <Typography.Text type="secondary">{label}</Typography.Text>
      </div>
      <div>{valueText(value)}</div>
    </div>
  );
}

function renderCompareEmpty(label = '无') {
  return renderCompareFieldBlock(label, <Typography.Text type="secondary">--</Typography.Text>, 'empty');
}

function buildCompareField(
  key: string,
  label: string,
  beforeRaw: unknown,
  afterRaw: unknown,
  beforeNode?: ReactNode,
  afterNode?: ReactNode,
): CompareField {
  const changed = isDifferent(beforeRaw, afterRaw);
  return {
    key,
    before: renderCompareFieldBlock(label, beforeNode ?? valueText(beforeRaw as ReactNode), changed ? 'before' : 'same'),
    after: renderCompareFieldBlock(label, afterNode ?? valueText(afterRaw as ReactNode), changed ? 'after' : 'same'),
  };
}

function renderAlignedCompareGrid(fields: CompareField[], rowKeyPrefix = 'compare') {
  return (
    <div style={compareTableStyle} data-review-compare-grid>
      <div style={compareHeaderRowStyle}>
        <div style={{ ...compareHeaderCellStyle, borderRight: '1px solid #f0f0f0' }}>修改前</div>
        <div style={compareHeaderCellStyle}>修改后</div>
      </div>
      {fields.map((field, index) => (
        <div
          key={`${rowKeyPrefix}-${field.key}`}
          data-review-compare-row={field.key}
          style={{
            ...compareRowStyle,
            borderBottom: index === fields.length - 1 ? undefined : '1px solid #f5f5f5',
          }}
        >
          <div
            data-review-compare-cell="before"
            style={{ ...compareCellStyle, borderRight: '1px solid #f0f0f0' }}
          >
            {field.before}
          </div>
          <div data-review-compare-cell="after" style={compareCellStyle}>{field.after}</div>
        </div>
      ))}
    </div>
  );
}

function renderComparePanels(title: string, fields: CompareField[], extra?: ReactNode) {
  return renderSection(title, renderAlignedCompareGrid(fields, title), extra);
}

function renderCompareContentBlock(content: ReactNode, tone: CompareTone = 'same') {
  return <div style={compareToneStyle(tone)}>{content}</div>;
}

function renderProductBasicInfo(product?: API.ProductDistribution.Spu, review?: API.ProductReview.Review) {
  return (
    <Descriptions size="small" bordered column={2}>
      <Descriptions.Item label="商品标题" span={2}>
        {valueText(product?.productName || review?.productNameAfter)}
      </Descriptions.Item>
      <Descriptions.Item label="卖家">{valueText(product?.sellerName || review?.sellerName)}</Descriptions.Item>
      <Descriptions.Item label="系统SPU">{valueText(product?.systemSpuCode || review?.systemSpuCode)}</Descriptions.Item>
      <Descriptions.Item label="类目">{valueText(product?.categoryName || review?.categoryName)}</Descriptions.Item>
      <Descriptions.Item label="商品状态">{renderSalesStatus(product?.spuStatus)}</Descriptions.Item>
      <Descriptions.Item label="英文标题" span={2}>{valueText(product?.productNameEn)}</Descriptions.Item>
    </Descriptions>
  );
}

function buildProductBasicCompareFields(
  before?: API.ProductDistribution.Spu,
  after?: API.ProductDistribution.Spu,
  review?: API.ProductReview.Review,
): CompareField[] {
  return [
    buildCompareField('productName', '商品标题', before?.productName, after?.productName,
      before?.productName, after?.productName || review?.productNameAfter),
    buildCompareField('sellerName', '卖家', before?.sellerName, after?.sellerName,
      before?.sellerName || review?.sellerName, after?.sellerName || review?.sellerName),
    buildCompareField('systemSpuCode', '系统SPU', before?.systemSpuCode, after?.systemSpuCode,
      before?.systemSpuCode || review?.systemSpuCode, after?.systemSpuCode || review?.systemSpuCode),
    buildCompareField('categoryName', '类目', before?.categoryName, after?.categoryName,
      before?.categoryName || review?.categoryName, after?.categoryName || review?.categoryName),
    buildCompareField('spuStatus', '商品状态', before?.spuStatus, after?.spuStatus,
      renderSalesStatus(before?.spuStatus), renderSalesStatus(after?.spuStatus)),
    buildCompareField('warehouseKind', '仓库类型', normalizeWarehouseKind(getProductWarehouseKind(before)), normalizeWarehouseKind(getProductWarehouseKind(after)),
      formatWarehouseKindLabel(getProductWarehouseKind(before)), formatWarehouseKindLabel(getProductWarehouseKind(after))),
    buildCompareField('productNameEn', '英文标题', before?.productNameEn, after?.productNameEn),
  ];
}

function renderProductBasicCompare(
  before?: API.ProductDistribution.Spu,
  after?: API.ProductDistribution.Spu,
  review?: API.ProductReview.Review,
) {
  return renderComparePanels('商品基础信息', buildProductBasicCompareFields(before, after, review));
}

function renderImageGalleryCompare(before?: API.ProductDistribution.Spu, after?: API.ProductDistribution.Spu, review?: API.ProductReview.Review) {
  const beforeRaw = JSON.stringify({
    mainImageUrl: before?.mainImageUrl,
    images: before?.images || [],
  });
  const afterRaw = JSON.stringify({
    mainImageUrl: after?.mainImageUrl,
    images: after?.images || [],
  });
  const changed = isDifferent(beforeRaw, afterRaw);
  return renderComparePanels('商品图片', [
    {
      key: 'images',
      before: renderCompareContentBlock(renderImageGallery(before, {
        ...review,
        mainImageUrlAfter: before?.mainImageUrl || review?.mainImageUrlBefore,
      } as API.ProductReview.Review), changed ? 'before' : 'same'),
      after: renderCompareContentBlock(renderImageGallery(after, review), changed ? 'after' : 'same'),
    },
  ]);
}

function attributeCompareKey(item: API.ProductDistribution.AttributeValue) {
  return String(item.attributeId ?? item.attributeCode ?? item.attributeName ?? item.valueId ?? JSON.stringify(item));
}

function attributeCompareLabel(
  before?: API.ProductDistribution.AttributeValue,
  after?: API.ProductDistribution.AttributeValue,
  displayMaps?: AttributeDisplayMaps,
) {
  const item = after || before;
  return item && displayMaps ? resolveAttributeLabel(item, displayMaps.attributeLabelMap) : item?.attributeName || '--';
}

function buildAttributeCompareFields(
  before?: API.ProductDistribution.Spu,
  after?: API.ProductDistribution.Spu,
  displayMaps?: AttributeDisplayMaps,
): CompareField[] {
  const beforeMap = new Map((before?.attributeValues || []).map((item) => [attributeCompareKey(item), item]));
  const afterMap = new Map((after?.attributeValues || []).map((item) => [attributeCompareKey(item), item]));
  const keys = Array.from(new Set([...beforeMap.keys(), ...afterMap.keys()]));
  return keys.map((key) => {
    const beforeItem = beforeMap.get(key);
    const afterItem = afterMap.get(key);
    const beforeText = beforeItem && displayMaps ? formatAttributeValue(beforeItem, displayMaps.optionLabelMap) : undefined;
    const afterText = afterItem && displayMaps ? formatAttributeValue(afterItem, displayMaps.optionLabelMap) : undefined;
    return buildCompareField(
      key,
      attributeCompareLabel(beforeItem, afterItem, displayMaps),
      beforeText,
      afterText,
      beforeText,
      afterText,
    );
  });
}

function renderAttributeCompare(
  before: API.ProductDistribution.Spu | undefined,
  after: API.ProductDistribution.Spu | undefined,
  displayMaps: AttributeDisplayMaps,
) {
  const fields = buildAttributeCompareFields(before, after, displayMaps);
  if (!fields.length) {
    return renderSection('商品属性', <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="未填写类目属性" />);
  }
  return renderComparePanels('商品属性', fields);
}

function renderDetailContentCompare(before?: API.ProductDistribution.Spu, after?: API.ProductDistribution.Spu) {
  const changed = isDifferent(before?.detailContent, after?.detailContent);
  return renderComparePanels('商品详情图文', [
    {
      key: 'detailContent',
      before: renderCompareContentBlock(<DetailContentPreview value={before?.detailContent} />, changed ? 'before' : 'same'),
      after: renderCompareContentBlock(<DetailContentPreview value={after?.detailContent} />, changed ? 'after' : 'same'),
    },
  ]);
}

function productBasicCompareRaw(product?: API.ProductDistribution.Spu) {
  return JSON.stringify({
    sellerSpuCode: product?.sellerSpuCode,
    sellerId: product?.sellerId,
    sellerName: product?.sellerName,
    categoryId: product?.categoryId,
    categoryName: product?.categoryName,
    productName: product?.productName,
    productNameEn: product?.productNameEn,
    spuStatus: product?.spuStatus,
    warehouseKind: normalizeWarehouseKind(getProductWarehouseKind(product)),
  });
}

function productImagesCompareRaw(product?: API.ProductDistribution.Spu) {
  return JSON.stringify({
    mainImageUrl: product?.mainImageUrl,
    images: product?.images || [],
  });
}

function productAttributesCompareRaw(product?: API.ProductDistribution.Spu) {
  return JSON.stringify(product?.attributeValues || []);
}

function productWarehouseCompareRaw(product?: API.ProductDistribution.Spu) {
  return JSON.stringify({
    warehouseKind: normalizeWarehouseKind(getProductWarehouseKind(product)),
    warehouseIds: product?.warehouseIds,
    warehouses: product?.warehouses || [],
  });
}

function hasProductInfoChanged(before?: API.ProductDistribution.Spu, after?: API.ProductDistribution.Spu) {
  return isDifferent(productBasicCompareRaw(before), productBasicCompareRaw(after))
    || isDifferent(productImagesCompareRaw(before), productImagesCompareRaw(after))
    || isDifferent(productAttributesCompareRaw(before), productAttributesCompareRaw(after))
    || isDifferent(productWarehouseCompareRaw(before), productWarehouseCompareRaw(after))
    || isDifferent(before?.detailContent, after?.detailContent);
}

function renderWarehouseCompare(before?: API.ProductDistribution.Spu, after?: API.ProductDistribution.Spu) {
  const changed = isDifferent(productWarehouseCompareRaw(before), productWarehouseCompareRaw(after));
  return renderComparePanels('发货仓库', [
    {
      key: 'warehouses',
      before: renderCompareContentBlock(renderWarehouseTable(before), changed ? 'before' : 'same'),
      after: renderCompareContentBlock(renderWarehouseTable(after), changed ? 'after' : 'same'),
    },
  ]);
}

function renderImageGallery(product?: API.ProductDistribution.Spu, review?: API.ProductReview.Review) {
  const gallery = (product?.images || [])
    .filter((item) => item.imageRole === 'GALLERY' && item.imageUrl)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  const mainImage = product?.mainImageUrl || review?.mainImageUrlAfter;

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
      <div>
        <div style={{ marginBottom: 6 }}><Tag color="blue">主图</Tag></div>
        {renderImage(mainImage, 96)}
      </div>
      {gallery.map((item, index) => (
        <div key={item.imageId || item.imageUrl || index}>
          <div style={{ marginBottom: 6 }}><Tag>图库 {index + 1}</Tag></div>
          {renderImage(item.imageUrl, 96)}
        </div>
      ))}
      {!mainImage && !gallery.length ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有商品图片" /> : null}
    </div>
  );
}

function renderWarehouseTable(product?: API.ProductDistribution.Spu) {
  const columns: ColumnsType<API.ProductDistribution.ProductWarehouse> = [
    { title: '仓库类型', dataIndex: 'warehouseKind', width: 120, render: (value) => formatWarehouseKindLabel(String(value || '')) },
    { title: '仓库编码', dataIndex: 'warehouseCode', width: 160, render: (value) => value || '--' },
    { title: '仓库名称', dataIndex: 'warehouseName', render: (value) => value || '--' },
    { title: '结算币种', dataIndex: 'settlementCurrency', width: 120, render: (value) => value || '--' },
  ];

  return (
    <Table
      rowKey={(row) => String(row.id ?? row.warehouseId ?? row.warehouseCode ?? row.warehouseName ?? JSON.stringify(row))}
      size="small"
      columns={columns}
      dataSource={product?.warehouses || []}
      pagination={false}
      locale={{ emptyText: '未绑定发货仓库' }}
    />
  );
}

function renderAttributeTable(product: API.ProductDistribution.Spu | undefined, displayMaps: AttributeDisplayMaps) {
  const columns: ColumnsType<API.ProductDistribution.AttributeValue> = [
    {
      title: '属性',
      dataIndex: 'attributeName',
      width: 220,
      render: (_, row) => resolveAttributeLabel(row, displayMaps.attributeLabelMap),
    },
    {
      title: '值',
      dataIndex: 'valueText',
      render: (_, row) => formatAttributeValue(row, displayMaps.optionLabelMap),
    },
  ];

  return (
    <Table
      rowKey={(row) => String(row.valueId ?? row.attributeId ?? row.attributeCode ?? row.attributeName ?? JSON.stringify(row))}
      size="small"
      columns={columns}
      dataSource={product?.attributeValues || []}
      pagination={false}
      locale={{ emptyText: '未填写类目属性' }}
    />
  );
}

function renderSkuTable(skus: API.ProductDistribution.Sku[], mode: 'full' | 'added' = 'full') {
  const columns: ColumnsType<API.ProductDistribution.Sku> = [
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      width: 90,
      render: (value) => renderImage(String(value || ''), 56),
    },
    {
      title: mode === 'added' ? '新增SKU' : 'SKU',
      dataIndex: 'sellerSkuCode',
      width: 180,
      render: (_, row) => (
        <div>
          <div>{formatSkuIdentity(row)}</div>
          <Typography.Text type="secondary">{row.systemSkuCode || '--'}</Typography.Text>
        </div>
      ),
    },
    { title: '规格', dataIndex: 'color', render: (_, row) => formatSkuSpecs(row) },
    {
      title: '销售价',
      dataIndex: 'salePrice',
      width: 130,
      render: (_, row) => formatMoney(row.salePrice, row.currencyCode),
    },
    {
      title: '供货价',
      dataIndex: 'supplyPrice',
      width: 130,
      render: (_, row) => formatMoney(row.supplyPrice, row.currencyCode),
    },
    { title: '尺寸重量', dataIndex: 'measureLengthCm', width: 180, render: (_, row) => formatDimension(row) },
    { title: '仓库', dataIndex: 'sourceWarehouseNames', width: 180, render: (_, row) => formatSkuWarehouseValue(row) },
    { title: '状态', dataIndex: 'skuStatus', width: 100, render: (value) => renderSalesStatus(String(value || '')) },
  ];

  return (
    <Table
      rowKey={(row) => String(row.skuId ?? row.sellerSkuCode ?? row.systemSkuCode ?? row.sourceDimensionGroupKey ?? JSON.stringify(row))}
      size="small"
      columns={columns}
      dataSource={skus}
      pagination={false}
      scroll={{ x: 1120 }}
      locale={{ emptyText: '没有SKU数据' }}
    />
  );
}

function NewProductReviewView({
  review,
  displayMaps,
}: {
  review: API.ProductReview.Review;
  displayMaps: AttributeDisplayMaps;
}) {
  const product = getAfterProduct(review);
  const skus = getAfterSkus(review);
  const showDeliveryWarehouse = shouldShowDeliveryWarehouse(product, review);

  return (
    <div style={stackStyle}>
      <div style={inlineGridStyle}>
        {renderMetric('SKU数量', skus.length || review.skuCount || '--')}
        {renderMetric('供货价区间', formatRange(review.priceAfterMin, review.priceAfterMax, review.currencySummary))}
        {renderMetric('仓库类型', formatWarehouseKindLabel(getProductWarehouseKind(product, review)))}
        {showDeliveryWarehouse
          ? renderMetric('发货仓库', product?.warehouses?.length ? `${product.warehouses.length} 个` : '--')
          : null}
      </div>
      {renderSection('商品基础信息', renderProductBasicInfo(product, review))}
      {renderSection('商品图片', renderImageGallery(product, review))}
      {renderSection('SKU 和价格', renderSkuTable(skus))}
      {showDeliveryWarehouse ? renderSection('发货仓库', renderWarehouseTable(product)) : null}
      {renderSection('商品属性', renderAttributeTable(product, displayMaps))}
      {renderSection('商品详情图文', <DetailContentPreview value={product?.detailContent} />)}
    </div>
  );
}

function AddSkuReviewView({ review }: { review: API.ProductReview.Review }) {
  const product = getAfterProduct(review);
  const addedPairs = getSkuPairs(review).filter((pair) => !pair.before && pair.after);

  return (
    <div style={stackStyle}>
      {renderSection('商品上下文', renderProductBasicInfo(product, review))}
      {renderSection('新增 SKU 左右对比', (
        <div style={stackStyle}>
          {(addedPairs.length ? addedPairs : getAfterSkus(review).map((sku, index) => ({
            key: String(sku.skuId ?? sku.sellerSkuCode ?? index),
            after: sku,
          }))).map((pair) => renderAddedSkuCompare(pair))}
        </div>
      ))}
      <Collapse
        items={[
          {
            key: 'product',
            label: '查看完整商品上下文',
            children: (
              <div style={stackStyle}>
                {renderImageGallery(product, review)}
                {renderWarehouseTable(product)}
              </div>
            ),
          },
        ]}
      />
    </div>
  );
}

function renderChangedTag(changed: boolean) {
  return <Tag color={changed ? 'orange' : 'default'}>{changed ? '已变更' : '无变化'}</Tag>;
}

function buildProductChangeRows(
  before?: API.ProductDistribution.Spu,
  after?: API.ProductDistribution.Spu,
): ChangeRow[] {
  const rows = [
    { key: 'productName', label: '商品标题', before: before?.productName, after: after?.productName },
    { key: 'productNameEn', label: '英文标题', before: before?.productNameEn, after: after?.productNameEn },
    { key: 'categoryName', label: '类目', before: before?.categoryName, after: after?.categoryName },
    { key: 'mainImageUrl', label: '主图', before: renderImage(before?.mainImageUrl), after: renderImage(after?.mainImageUrl) },
    { key: 'detailContent', label: '详情图文', before: before?.detailContent ? '已填写' : '未填写', after: after?.detailContent ? '已填写' : '未填写' },
    { key: 'attributeValues', label: '类目属性', before: `${before?.attributeValues?.length || 0} 项`, after: `${after?.attributeValues?.length || 0} 项` },
    { key: 'images', label: '图库', before: `${before?.images?.length || 0} 张`, after: `${after?.images?.length || 0} 张` },
  ];

  return rows.map((row) => ({
    ...row,
    changed: row.key === 'mainImageUrl'
      ? isDifferent(before?.mainImageUrl, after?.mainImageUrl)
      : isDifferent(row.before, row.after),
  }));
}

function renderChangeTable(rows: ChangeRow[]) {
  const changedRows = rows.filter((row) => row.changed);
  const dataSource = changedRows.length ? changedRows : rows;
  const columns: ColumnsType<ChangeRow> = [
    { title: '字段', dataIndex: 'label', width: 160 },
    { title: '修改前', dataIndex: 'before', render: (value) => valueText(value) },
    { title: '修改后', dataIndex: 'after', render: (value) => valueText(value) },
    { title: '变化', dataIndex: 'changed', width: 100, render: (_, row) => renderChangedTag(row.changed) },
  ];

  return (
    <Table
      rowKey="key"
      size="small"
      columns={columns}
      dataSource={dataSource}
      pagination={false}
      scroll={{ x: 900 }}
    />
  );
}

function ProductInfoChangeReviewView({
  review,
  displayMaps,
}: {
  review: API.ProductReview.Review;
  displayMaps: AttributeDisplayMaps;
}) {
  const before = getBeforeProduct(review);
  const after = getAfterProduct(review);

  return (
    <div style={stackStyle}>
      {renderProductBasicCompare(before, after, review)}
      {renderImageGalleryCompare(before, after, review)}
      {renderAttributeCompare(before, after, displayMaps)}
      {renderDetailContentCompare(before, after)}
    </div>
  );
}

function renderSkuValue(value?: ReactNode) {
  return <Typography.Text>{valueText(value)}</Typography.Text>;
}

function renderSkuWarehouseValue(sku?: API.ProductDistribution.Sku) {
  return renderSkuValue(formatSkuWarehouseValue(sku));
}

function buildSkuFieldChanges(before?: API.ProductDistribution.Sku, after?: API.ProductDistribution.Sku): SkuFieldChange[] {
  const currency = after?.currencyCode || before?.currencyCode;
  const fields: Omit<SkuFieldChange, 'changed'>[] = [
    {
      key: 'skuImageUrl',
      label: 'SKU图',
      beforeRaw: before?.skuImageUrl,
      afterRaw: after?.skuImageUrl,
      before: renderImage(before?.skuImageUrl, 56),
      after: renderImage(after?.skuImageUrl, 56),
    },
    {
      key: 'sellerSkuCode',
      label: '客户SKU',
      beforeRaw: before?.sellerSkuCode,
      afterRaw: after?.sellerSkuCode,
      before: renderSkuValue(before?.sellerSkuCode),
      after: renderSkuValue(after?.sellerSkuCode),
    },
    {
      key: 'systemSkuCode',
      label: '系统SKU',
      beforeRaw: before?.systemSkuCode,
      afterRaw: after?.systemSkuCode,
      before: renderSkuValue(before?.systemSkuCode),
      after: renderSkuValue(after?.systemSkuCode),
    },
    { key: 'color', label: '颜色', beforeRaw: before?.color, afterRaw: after?.color, before: renderSkuValue(before?.color), after: renderSkuValue(after?.color) },
    { key: 'size', label: '尺码', beforeRaw: before?.size, afterRaw: after?.size, before: renderSkuValue(before?.size), after: renderSkuValue(after?.size) },
    { key: 'material', label: '材质', beforeRaw: before?.material, afterRaw: after?.material, before: renderSkuValue(before?.material), after: renderSkuValue(after?.material) },
    { key: 'style', label: '款式', beforeRaw: before?.style, afterRaw: after?.style, before: renderSkuValue(before?.style), after: renderSkuValue(after?.style) },
    { key: 'model', label: '型号', beforeRaw: before?.model, afterRaw: after?.model, before: renderSkuValue(before?.model), after: renderSkuValue(after?.model) },
    {
      key: 'packageQuantity',
      label: '包装数量',
      beforeRaw: before?.packageQuantity,
      afterRaw: after?.packageQuantity,
      before: renderSkuValue(before?.packageQuantity),
      after: renderSkuValue(after?.packageQuantity),
    },
    { key: 'capacity', label: '容量', beforeRaw: before?.capacity, afterRaw: after?.capacity, before: renderSkuValue(before?.capacity), after: renderSkuValue(after?.capacity) },
    {
      key: 'currencyCode',
      label: '币种',
      beforeRaw: before?.currencyCode,
      afterRaw: after?.currencyCode,
      before: renderSkuValue(before?.currencyCode),
      after: renderSkuValue(after?.currencyCode),
    },
    {
      key: 'sourceDimensionGroupKey',
      label: '来源维度组',
      beforeRaw: before?.sourceDimensionGroupKey,
      afterRaw: after?.sourceDimensionGroupKey,
      before: renderSkuValue(before?.sourceDimensionGroupKey),
      after: renderSkuValue(after?.sourceDimensionGroupKey),
    },
    {
      key: 'sourceSkuGroupKey',
      label: '来源SKU组',
      beforeRaw: before?.sourceSkuGroupKey,
      afterRaw: after?.sourceSkuGroupKey,
      before: renderSkuValue(before?.sourceSkuGroupKey),
      after: renderSkuValue(after?.sourceSkuGroupKey),
    },
    {
      key: 'masterSku',
      label: 'Master SKU',
      beforeRaw: before?.masterSku,
      afterRaw: after?.masterSku,
      before: renderSkuValue(before?.masterSku),
      after: renderSkuValue(after?.masterSku),
    },
    {
      key: 'length',
      label: '长',
      beforeRaw: getSkuMeasurementRaw(before, 'measureLengthCm', 'lengthValue', 'cm'),
      afterRaw: getSkuMeasurementRaw(after, 'measureLengthCm', 'lengthValue', 'cm'),
      before: renderSkuMeasurementValue(before, 'measureLengthCm', 'lengthValue', 'cm'),
      after: renderSkuMeasurementValue(after, 'measureLengthCm', 'lengthValue', 'cm'),
    },
    {
      key: 'width',
      label: '宽',
      beforeRaw: getSkuMeasurementRaw(before, 'measureWidthCm', 'widthValue', 'cm'),
      afterRaw: getSkuMeasurementRaw(after, 'measureWidthCm', 'widthValue', 'cm'),
      before: renderSkuMeasurementValue(before, 'measureWidthCm', 'widthValue', 'cm'),
      after: renderSkuMeasurementValue(after, 'measureWidthCm', 'widthValue', 'cm'),
    },
    {
      key: 'height',
      label: '高',
      beforeRaw: getSkuMeasurementRaw(before, 'measureHeightCm', 'heightValue', 'cm'),
      afterRaw: getSkuMeasurementRaw(after, 'measureHeightCm', 'heightValue', 'cm'),
      before: renderSkuMeasurementValue(before, 'measureHeightCm', 'heightValue', 'cm'),
      after: renderSkuMeasurementValue(after, 'measureHeightCm', 'heightValue', 'cm'),
    },
    {
      key: 'weight',
      label: '重量',
      beforeRaw: getSkuMeasurementRaw(before, 'measureWeightKg', 'weight', 'kg'),
      afterRaw: getSkuMeasurementRaw(after, 'measureWeightKg', 'weight', 'kg'),
      before: renderSkuMeasurementValue(before, 'measureWeightKg', 'weight', 'kg'),
      after: renderSkuMeasurementValue(after, 'measureWeightKg', 'weight', 'kg'),
    },
    {
      key: 'supplyPrice',
      label: '供货价',
      beforeRaw: before?.supplyPrice,
      afterRaw: after?.supplyPrice,
      before: renderSkuValue(formatMoney(before?.supplyPrice, currency)),
      after: renderSkuValue(formatMoney(after?.supplyPrice, currency)),
    },
    {
      key: 'warehouse',
      label: '仓库',
      beforeRaw: before?.sourceWarehouseNames || normalizeWarehouseKind(before?.warehouseKindSummary),
      afterRaw: after?.sourceWarehouseNames || normalizeWarehouseKind(after?.warehouseKindSummary),
      before: renderSkuWarehouseValue(before),
      after: renderSkuWarehouseValue(after),
    },
    {
      key: 'skuStatus',
      label: '状态',
      beforeRaw: before?.skuStatus,
      afterRaw: after?.skuStatus,
      before: renderSalesStatus(before?.skuStatus),
      after: renderSalesStatus(after?.skuStatus),
    },
  ];

  return fields.map((field) => ({
    ...field,
    changed: isDifferent(field.beforeRaw, field.afterRaw),
  }));
}

function getSkuInfoChangedFields(pair: SkuPair) {
  return buildSkuFieldChanges(pair.before, pair.after)
    .filter((field) => field.key !== 'supplyPrice')
    .filter((field) => field.changed);
}

function hasSupplyPriceChanged(pair: SkuPair) {
  return !!pair.before && !!pair.after && isDifferent(pair.before.supplyPrice, pair.after.supplyPrice);
}

function toSkuCompareField(field: SkuFieldChange, pair: SkuPair): CompareField {
  const beforeTone: CompareTone = !pair.before ? 'empty' : field.changed ? 'before' : 'same';
  const afterTone: CompareTone = !pair.after ? 'empty' : field.changed || !pair.before ? 'after' : 'same';
  return {
    key: field.key,
    before: !pair.before
      ? renderCompareEmpty(field.label)
      : renderCompareFieldBlock(field.label, field.before, beforeTone),
    after: !pair.after
      ? renderCompareEmpty(field.label)
      : renderCompareFieldBlock(field.label, field.after, afterTone),
  };
}

function renderSkuCompareHeader(pair: SkuPair, extra?: ReactNode) {
  const sku = pair.after || pair.before;
  return (
    <div style={skuCompareHeaderStyle}>
      <div>{renderSkuImageSummary(pair)}</div>
      <div style={{ minWidth: 0 }}>
        <Typography.Text strong>{formatSkuIdentityTitle(pair)}</Typography.Text>
        <div style={{ marginTop: 4 }}>
          <Typography.Text type="secondary">
            客户SKU：{sku?.sellerSkuCode || pair.item?.sellerSkuCode || '--'} / 系统SKU：{sku?.systemSkuCode || pair.item?.systemSkuCode || '--'}
          </Typography.Text>
        </div>
        <div style={{ marginTop: 8 }}>{renderSkuSpecTags(pair)}</div>
      </div>
      <div style={{ maxWidth: 260 }}>{extra}</div>
    </div>
  );
}

function renderSkuComparePair(pair: SkuPair, fields: CompareField[], title?: string, extra?: ReactNode) {
  return (
    <section key={pair.key} style={skuCompareCardStyle}>
      {renderSkuCompareHeader(pair, extra)}
      <div style={{ padding: 12 }}>
        {renderAlignedCompareGrid(fields, title || pair.key)}
      </div>
    </section>
  );
}

function renderSkuDetailCompare(pair: SkuPair, mode: 'all' | 'changed' = 'all') {
  const fieldChanges = buildSkuFieldChanges(pair.before, pair.after)
    .filter((field) => field.key !== 'supplyPrice');
  const visibleFields = mode === 'all' ? fieldChanges : fieldChanges.filter((field) => field.changed);
  const fields = (visibleFields.length ? visibleFields : fieldChanges).map((field) => toSkuCompareField(field, pair));
  return renderSkuComparePair(
    pair,
    fields,
    formatSkuIdentity(pair.after || pair.before, pair.item),
    renderSkuChangeTags(pair),
  );
}

function renderAddedSkuCompare(pair: SkuPair) {
  const fields = buildSkuFieldChanges(pair.before, pair.after).map((field) => toSkuCompareField(field, pair));
  return renderSkuComparePair(
    pair,
    fields,
    formatSkuIdentity(pair.after || pair.before, pair.item),
    <Tag color="green">新增SKU</Tag>,
  );
}

function renderSkuSupplyPriceCompare(pair: SkuPair) {
  const currency = pair.after?.currencyCode || pair.before?.currencyCode;
  const fields: CompareField[] = [
    buildCompareField(
      'supplyPrice',
      '供货价',
      pair.before?.supplyPrice,
      pair.after?.supplyPrice,
      formatMoney(pair.before?.supplyPrice, currency),
      formatMoney(pair.after?.supplyPrice, currency),
    ),
  ];
  return renderSkuComparePair(
    pair,
    fields,
    formatSkuIdentity(pair.after || pair.before, pair.item),
    renderSupplyPriceChangeTags(pair.before, pair.after),
  );
}

function renderSkuChangeTags(pair: SkuPair) {
  const changedFields = getSkuInfoChangedFields(pair);
  if (!changedFields.length) {
    return <Typography.Text type="secondary">未识别到字段变化</Typography.Text>;
  }
  return (
    <div style={compactTagWrapStyle}>
      {changedFields.map((field) => (
        <Tag key={field.key} color="blue">{field.label}</Tag>
      ))}
    </div>
  );
}

function renderSkuSpecTags(pair: SkuPair) {
  const after = pair.after || pair.before;
  const changedKeys = new Set(getSkuInfoChangedFields(pair).map((field) => field.key));
  const specs = [
    ['color', '颜色', after?.color],
    ['size', '尺码', after?.size],
    ['material', '材质', after?.material],
    ['style', '款式', after?.style],
    ['model', '型号', after?.model],
    ['capacity', '容量', after?.capacity],
  ] as const;
  const visibleSpecs = specs.filter(([, , value]) => !!value);
  if (!visibleSpecs.length) {
    return <Typography.Text type="secondary">--</Typography.Text>;
  }
  return (
    <div style={compactTagWrapStyle}>
      {visibleSpecs.map(([key, label, value]) => (
        <Tag key={key} color={changedKeys.has(key) ? 'orange' : undefined}>
          {label}：{value}
        </Tag>
      ))}
    </div>
  );
}

function renderSkuImageSummary(pair: SkuPair) {
  const imageChanged = isDifferent(pair.before?.skuImageUrl, pair.after?.skuImageUrl);
  if (!imageChanged) {
    return renderImage(pair.after?.skuImageUrl || pair.before?.skuImageUrl, 56);
  }
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      {renderImage(pair.before?.skuImageUrl, 48)}
      <Typography.Text type="secondary">-&gt;</Typography.Text>
      {renderImage(pair.after?.skuImageUrl, 48)}
    </div>
  );
}

function renderSkuFieldChangeDetail(pair: SkuPair) {
  const changedFields = getSkuInfoChangedFields(pair);
  const columns: ColumnsType<SkuFieldChange> = [
    { title: '字段', dataIndex: 'label', width: 120, render: (value) => <Tag>{value}</Tag> },
    { title: '修改前', dataIndex: 'before', render: (_, row) => row.before },
    { title: '修改后', dataIndex: 'after', render: (_, row) => <div style={{ color: '#1677ff' }}>{row.after}</div> },
  ];

  return (
    <Table
      rowKey="key"
      size="small"
      columns={columns}
      dataSource={changedFields}
      pagination={false}
      locale={{ emptyText: '未识别到字段变化' }}
    />
  );
}

function renderSkuInfoChangeTable(pairs: SkuPair[]) {
  const columns: ColumnsType<SkuPair> = [
    {
      title: 'SKU',
      dataIndex: 'key',
      width: 190,
      render: (_, row) => (
        <div>
          <Typography.Text strong>{formatSkuIdentity(row.after || row.before, row.item)}</Typography.Text>
          <div>
            <Typography.Text type="secondary">{row.after?.systemSkuCode || row.before?.systemSkuCode || '--'}</Typography.Text>
          </div>
        </div>
      ),
    },
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      width: 130,
      render: (_, row) => renderSkuImageSummary(row),
    },
    {
      title: '规格摘要',
      dataIndex: 'spec',
      render: (_, row) => renderSkuSpecTags(row),
    },
    {
      title: '变化项',
      dataIndex: 'diff',
      width: 240,
      render: (_, row) => renderSkuChangeTags(row),
    },
  ];

  return (
    <Table
      rowKey="key"
      size="small"
      columns={columns}
      dataSource={pairs}
      pagination={false}
      expandable={{ expandedRowRender: renderSkuFieldChangeDetail }}
      scroll={{ x: 920 }}
      locale={{ emptyText: '没有 SKU 资料变化' }}
    />
  );
}

function SkuInfoChangeReviewView({ review }: { review: API.ProductReview.Review }) {
  const beforeProduct = getBeforeProduct(review);
  const afterProduct = getAfterProduct(review);
  const pairs = getSkuPairs(review).filter((pair) => !!pair.after);
  const changedPairs = pairs.filter((pair) => getSkuInfoChangedFields(pair).length > 0);
  const visiblePairs = changedPairs.length ? changedPairs : pairs;

  return (
    <div style={stackStyle}>
      {beforeProduct || afterProduct ? renderProductBasicCompare(beforeProduct, afterProduct, review) : null}
      {renderSection('SKU 资料左右对比', (
        <div style={stackStyle}>
          {visiblePairs.map((pair) => renderSkuDetailCompare(pair))}
        </div>
      ), <Tag color="blue">{visiblePairs.length} 个SKU</Tag>)}
    </div>
  );
}

function renderPriceDelta(before?: number, after?: number) {
  if (before == null || after == null) {
    return '--';
  }
  const delta = after - before;
  const percent = before === 0 ? undefined : (delta / before) * 100;
  const color = delta > 0 ? '#cf1322' : delta < 0 ? '#3f8600' : undefined;
  return (
    <span style={{ color }}>
      {delta > 0 ? '+' : ''}{delta.toFixed(2)}
      {percent == null ? '' : ` / ${percent > 0 ? '+' : ''}${percent.toFixed(1)}%`}
    </span>
  );
}

function renderSupplyPriceChangeTags(before?: API.ProductDistribution.Sku, after?: API.ProductDistribution.Sku) {
  if (after?.supplyPrice == null) {
    return <Tag color="error">缺供货价</Tag>;
  }
  if (before?.supplyPrice == null) {
    return <Tag color="green">新增供货价</Tag>;
  }
  const delta = after.supplyPrice - before.supplyPrice;
  if (delta === 0) {
    return <Tag>供货价未变</Tag>;
  }
  const percent = before.supplyPrice === 0 ? undefined : Math.abs(delta / before.supplyPrice) * 100;
  const directionTag = (
    <Tag color={delta > 0 ? 'red' : 'green'}>
      {delta > 0 ? '供货价上涨' : '供货价下降'} {renderPriceDelta(before.supplyPrice, after.supplyPrice)}
    </Tag>
  );
  if (percent != null && percent >= 30) {
    return (
      <div style={compactTagWrapStyle}>
        {directionTag}
        <Tag color="warning">涨跌幅较大</Tag>
      </div>
    );
  }
  return directionTag;
}

function PriceChangeReviewView({
  review,
  pairs: inputPairs,
}: {
  review: API.ProductReview.Review;
  pairs?: SkuPair[];
}) {
  const pairs = inputPairs || getSkuPairs(review).filter(hasSupplyPriceChanged);

  return (
    <div style={stackStyle}>
      {renderSection('SKU 供货价左右对比', (
        <div style={stackStyle}>
          {pairs.map((pair) => renderSkuSupplyPriceCompare(pair))}
        </div>
      ))}
    </div>
  );
}

function getEditChangeScopes(review: API.ProductReview.Review): EditChangeScope[] {
  const beforeProduct = getBeforeProduct(review);
  const afterProduct = getAfterProduct(review);
  const pairs = getSkuPairs(review);
  const scopes: EditChangeScope[] = [];
  if (beforeProduct && afterProduct && hasProductInfoChanged(beforeProduct, afterProduct)) {
    scopes.push('PRODUCT_INFO');
  }
  if (pairs.some((pair) => !pair.before && !!pair.after)) {
    scopes.push('ADD_SKU');
  }
  if (pairs.some((pair) => !!pair.before && !!pair.after && getSkuInfoChangedFields(pair).length > 0)) {
    scopes.push('SKU_INFO');
  }
  if (pairs.some(hasSupplyPriceChanged)) {
    scopes.push('SUPPLY_PRICE');
  }
  return scopes;
}

function renderChangeScopeTags(scopes: EditChangeScope[]) {
  const labelMap: Record<EditChangeScope, string> = {
    PRODUCT_INFO: '商品资料',
    ADD_SKU: '新增SKU',
    SKU_INFO: 'SKU资料',
    SUPPLY_PRICE: '供货价',
  };
  if (!scopes.length) {
    return <Typography.Text type="secondary">未识别到字段变化</Typography.Text>;
  }
  return (
    <div style={compactTagWrapStyle}>
      {scopes.map((scope) => <Tag key={scope} color={scope === 'SUPPLY_PRICE' ? 'red' : 'blue'}>{labelMap[scope]}</Tag>)}
    </div>
  );
}

function EditChangeOverviewView({
  review,
  displayMaps,
}: {
  review: API.ProductReview.Review;
  displayMaps: AttributeDisplayMaps;
}) {
  const beforeProduct = getBeforeProduct(review);
  const afterProduct = getAfterProduct(review);
  const pairs = getSkuPairs(review);
  const scopes = getEditChangeScopes(review);
  const addedPairs = pairs.filter((pair) => !pair.before && !!pair.after);
  const skuInfoPairs = pairs.filter((pair) => !!pair.before && !!pair.after && getSkuInfoChangedFields(pair).length > 0);
  const supplyPricePairs = pairs.filter(hasSupplyPriceChanged);
  const productBasicChanged = isDifferent(productBasicCompareRaw(beforeProduct), productBasicCompareRaw(afterProduct));
  const productImagesChanged = isDifferent(productImagesCompareRaw(beforeProduct), productImagesCompareRaw(afterProduct));
  const productAttributesChanged = isDifferent(productAttributesCompareRaw(beforeProduct), productAttributesCompareRaw(afterProduct));
  const productWarehouseChanged = isDifferent(productWarehouseCompareRaw(beforeProduct), productWarehouseCompareRaw(afterProduct));
  const productDetailChanged = isDifferent(beforeProduct?.detailContent, afterProduct?.detailContent);
  const shouldFallbackToProduct = review.reviewType === 'EDIT_PRODUCT_INFO' && beforeProduct && afterProduct && !scopes.length;
  const shouldFallbackToSku = review.reviewType === 'EDIT_SKU_INFO' && !skuInfoPairs.length;
  const fallbackSkuPairs = shouldFallbackToSku ? pairs.filter((pair) => !!pair.after) : [];
  const visibleScopes = scopes.length ? scopes : review.reviewType === 'EDIT_MIXED' ? ['PRODUCT_INFO', 'SKU_INFO', 'SUPPLY_PRICE'] as EditChangeScope[] : [];

  return (
    <div style={stackStyle}>
      {renderSection('变更范围', renderChangeScopeTags(visibleScopes.length ? visibleScopes : scopes))}
      {(productBasicChanged || shouldFallbackToProduct) ? renderProductBasicCompare(beforeProduct, afterProduct, review) : null}
      {productImagesChanged || shouldFallbackToProduct ? renderImageGalleryCompare(beforeProduct, afterProduct, review) : null}
      {productWarehouseChanged || shouldFallbackToProduct ? renderWarehouseCompare(beforeProduct, afterProduct) : null}
      {productAttributesChanged || shouldFallbackToProduct ? renderAttributeCompare(beforeProduct, afterProduct, displayMaps) : null}
      {productDetailChanged || shouldFallbackToProduct ? renderDetailContentCompare(beforeProduct, afterProduct) : null}
      {addedPairs.length ? renderSection('新增 SKU 左右对比', (
        <div style={stackStyle}>
          {addedPairs.map((pair) => renderAddedSkuCompare(pair))}
        </div>
      )) : null}
      {skuInfoPairs.length || fallbackSkuPairs.length ? renderSection('SKU 资料左右对比', (
        <div style={stackStyle}>
          {(skuInfoPairs.length ? skuInfoPairs : fallbackSkuPairs).map((pair) => renderSkuDetailCompare(pair))}
        </div>
      ), <Tag color="blue">{(skuInfoPairs.length ? skuInfoPairs : fallbackSkuPairs).length} 个SKU</Tag>) : null}
      {supplyPricePairs.length ? <PriceChangeReviewView review={review} pairs={supplyPricePairs} /> : null}
      {!scopes.length && !shouldFallbackToProduct && !fallbackSkuPairs.length ? <GenericReviewView review={review} /> : null}
    </div>
  );
}

function GenericReviewView({ review }: { review: API.ProductReview.Review }) {
  const product = getAfterProduct(review);
  const skus = getAfterSkus(review);

  return (
    <div style={stackStyle}>
      {renderSection('商品基础信息', renderProductBasicInfo(product, review))}
      {renderSection('SKU', renderSkuTable(skus))}
    </div>
  );
}

export default function ProductReviewBusinessPreview({ review }: ProductReviewBusinessPreviewProps) {
  const [categorySchema, setCategorySchema] = useState<API.Product.CategoryAttribute[]>([]);
  const categoryId = getReviewCategoryId(review);
  const attributeLabelMap = useMemo(() => buildAttributeLabelMap(categorySchema), [categorySchema]);
  const optionLabelMap = useMemo(() => buildOptionLabelMap(categorySchema), [categorySchema]);
  const displayMaps = useMemo(
    () => ({ attributeLabelMap, optionLabelMap }),
    [attributeLabelMap, optionLabelMap],
  );

  useEffect(() => {
    if (!categoryId) {
      setCategorySchema([]);
      return undefined;
    }
    let cancelled = false;
    getCategorySchema(categoryId, { skipErrorHandler: true })
      .then((resp) => {
        if (!cancelled) {
          setCategorySchema(resp.data || []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCategorySchema([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [categoryId]);

  return (
    <div style={stackStyle}>
      <div>
        <Typography.Title level={4} style={{ marginBottom: 4 }}>
          {review.productNameAfter || review.productNameBefore || '商品审核'}
        </Typography.Title>
        <Typography.Text type="secondary">
          {review.systemSpuCode || '--'} / {review.sellerName || '--'} / {review.categoryName || '--'}
        </Typography.Text>
      </div>
      <Divider style={{ margin: 0 }} />
      {review.reviewType === 'NEW_PRODUCT' ? <NewProductReviewView review={review} displayMaps={displayMaps} /> : null}
      {review.reviewType === 'ADD_SKU' ? <AddSkuReviewView review={review} /> : null}
      {['EDIT_PRODUCT_INFO', 'EDIT_SKU_INFO', 'EDIT_PRICE', 'EDIT_MIXED'].includes(String(review.reviewType || '')) ? (
        <EditChangeOverviewView review={review} displayMaps={displayMaps} />
      ) : null}
      {!review.reviewType || !['NEW_PRODUCT', 'ADD_SKU', 'EDIT_PRODUCT_INFO', 'EDIT_SKU_INFO', 'EDIT_PRICE', 'EDIT_MIXED'].includes(review.reviewType)
        ? <GenericReviewView review={review} />
        : null}
    </div>
  );
}
