import {
  Alert,
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
import type { ReactNode } from 'react';
import DetailContentPreview from '../../Distribution/components/DetailContentPreview';
import { resolveResourceUrl } from '../../Distribution/constants';

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

type CheckItem = {
  label: string;
  ok: boolean;
  detail: string;
};

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

const imageCompareStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
  gap: 12,
} as const;

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

function normalizeText(value: unknown) {
  if (value == null || value === '') {
    return '';
  }
  return typeof value === 'string' ? value : JSON.stringify(value);
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

function renderCheckList(items: CheckItem[]) {
  return (
    <div style={inlineGridStyle}>
      {items.map((item) => (
        <div key={item.label} style={{ padding: 12, border: '1px solid #f0f0f0', borderRadius: 6 }}>
          <Tag color={item.ok ? 'success' : 'error'}>{item.ok ? '已完整' : '需关注'}</Tag>
          <Typography.Text strong>{item.label}</Typography.Text>
          <div style={{ marginTop: 6 }}>
            <Typography.Text type="secondary">{item.detail}</Typography.Text>
          </div>
        </div>
      ))}
    </div>
  );
}

function renderProductBasicInfo(product?: API.ProductDistribution.Spu, review?: API.ProductReview.Review) {
  return (
    <Descriptions size="small" bordered column={3}>
      <Descriptions.Item label="商品标题" span={2}>
        {valueText(product?.productName || review?.productNameAfter)}
      </Descriptions.Item>
      <Descriptions.Item label="系统SPU">{valueText(product?.systemSpuCode || review?.systemSpuCode)}</Descriptions.Item>
      <Descriptions.Item label="卖家">{valueText(product?.sellerName || review?.sellerName)}</Descriptions.Item>
      <Descriptions.Item label="类目">{valueText(product?.categoryName || review?.categoryName)}</Descriptions.Item>
      <Descriptions.Item label="商品状态">{valueText(product?.spuStatus)}</Descriptions.Item>
      <Descriptions.Item label="英文标题" span={2}>{valueText(product?.productNameEn)}</Descriptions.Item>
      <Descriptions.Item label="卖点" span={3}>{valueText(product?.sellingPoint)}</Descriptions.Item>
    </Descriptions>
  );
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
    { title: '仓库类型', dataIndex: 'warehouseKind', width: 120, render: (value) => value || '--' },
    { title: '仓库编码', dataIndex: 'warehouseCode', width: 160, render: (value) => value || '--' },
    { title: '仓库名称', dataIndex: 'warehouseName', render: (value) => value || '--' },
    { title: '结算币种', dataIndex: 'settlementCurrency', width: 120, render: (value) => value || '--' },
  ];

  return (
    <Table
      rowKey={(row, index) => String(row.id ?? row.warehouseId ?? index)}
      size="small"
      columns={columns}
      dataSource={product?.warehouses || []}
      pagination={false}
      locale={{ emptyText: '未绑定发货仓库' }}
    />
  );
}

function renderAttributeValue(item: API.ProductDistribution.AttributeValue) {
  if (item.valueText) return item.valueText;
  if (item.valueCode) return item.valueCode;
  if (item.valueNumber != null) return item.valueNumber;
  if (item.valueDate) return item.valueDate;
  if (item.valueJson) return item.valueJson;
  return '--';
}

function renderAttributeTable(product?: API.ProductDistribution.Spu) {
  const columns: ColumnsType<API.ProductDistribution.AttributeValue> = [
    { title: '属性', dataIndex: 'attributeName', width: 220, render: (value, row) => value || row.attributeCode || '--' },
    { title: '值', dataIndex: 'valueText', render: (_, row) => renderAttributeValue(row) },
  ];

  return (
    <Table
      rowKey={(row, index) => String(row.valueId ?? row.attributeId ?? index)}
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
    { title: '仓库', dataIndex: 'sourceWarehouseNames', width: 180, render: (value, row) => value || row.warehouseKindSummary || '--' },
    { title: '状态', dataIndex: 'skuStatus', width: 100, render: (value) => value || '--' },
  ];

  return (
    <Table
      rowKey={(row, index) => String(row.skuId ?? row.sellerSkuCode ?? row.systemSkuCode ?? index)}
      size="small"
      columns={columns}
      dataSource={skus}
      pagination={false}
      scroll={{ x: 1120 }}
      locale={{ emptyText: '没有SKU数据' }}
    />
  );
}

function buildNewProductChecks(product?: API.ProductDistribution.Spu, skus: API.ProductDistribution.Sku[] = []) {
  const hasPrice = skus.every((sku) => sku.salePrice != null);
  const hasSupplyPrice = skus.every((sku) => sku.supplyPrice != null);
  return [
    {
      label: '基础资料',
      ok: !!product?.productName && !!product?.categoryName,
      detail: product?.productName && product?.categoryName ? '标题和类目已填写' : '标题或类目缺失',
    },
    {
      label: '商品图片',
      ok: !!product?.mainImageUrl,
      detail: product?.mainImageUrl ? '主图已上传' : '主图缺失',
    },
    {
      label: 'SKU',
      ok: skus.length > 0,
      detail: skus.length ? `共 ${skus.length} 个 SKU` : '没有 SKU',
    },
    {
      label: '价格',
      ok: !!skus.length && hasPrice && hasSupplyPrice,
      detail: hasPrice && hasSupplyPrice ? '销售价和供货价完整' : '存在价格缺失',
    },
    {
      label: '发货仓库',
      ok: !!product?.warehouses?.length,
      detail: product?.warehouses?.length ? `已绑定 ${product.warehouses.length} 个仓库` : '未绑定仓库',
    },
    {
      label: '商品详情',
      ok: !!product?.detailContent,
      detail: product?.detailContent ? '详情图文已填写' : '详情图文为空',
    },
  ];
}

function NewProductReviewView({ review }: { review: API.ProductReview.Review }) {
  const product = getAfterProduct(review);
  const skus = getAfterSkus(review);

  return (
    <div style={stackStyle}>
      <Alert
        type="info"
        showIcon
        message="新增商品审核需要看完整商品"
        description="这个视图按商城商品详情的顺序展示基础资料、图片、SKU、价格、仓库和详情图文，所有内容都是本次新增。"
      />
      <div style={inlineGridStyle}>
        {renderMetric('SKU数量', skus.length || review.skuCount || '--')}
        {renderMetric('销售价区间', formatRange(review.priceAfterMin, review.priceAfterMax, review.currencySummary))}
        {renderMetric('发货仓库', product?.warehouses?.length ? `${product.warehouses.length} 个` : review.warehouseSummary || '--')}
      </div>
      {renderSection('审核检查清单', renderCheckList(buildNewProductChecks(product, skus)))}
      {renderSection('商品基础信息', renderProductBasicInfo(product, review))}
      {renderSection('商品图片', renderImageGallery(product, review))}
      {renderSection('SKU 和价格', renderSkuTable(skus))}
      {renderSection('发货仓库', renderWarehouseTable(product))}
      {renderSection('商品属性', renderAttributeTable(product))}
      {renderSection('商品详情图文', <DetailContentPreview value={product?.detailContent} />)}
    </div>
  );
}

function AddSkuReviewView({ review }: { review: API.ProductReview.Review }) {
  const product = getAfterProduct(review);
  const addedSkus = getSkuPairs(review)
    .filter((pair) => !pair.before && pair.after)
    .map((pair) => pair.after)
    .filter((sku): sku is API.ProductDistribution.Sku => !!sku);

  return (
    <div style={stackStyle}>
      <Alert
        type="info"
        showIcon
        message="本次只审核新增 SKU"
        description="旧商品资料只作为上下文，审核重点是下面这些新增 SKU 的图片、规格、价格、尺寸重量和仓库。"
      />
      {renderSection('商品上下文', renderProductBasicInfo(product, review))}
      {renderSection('新增 SKU', renderSkuTable(addedSkus.length ? addedSkus : getAfterSkus(review), 'added'))}
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
    { key: 'sellingPoint', label: '卖点', before: before?.sellingPoint, after: after?.sellingPoint },
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

function ProductInfoChangeReviewView({ review }: { review: API.ProductReview.Review }) {
  const before = getBeforeProduct(review);
  const after = getAfterProduct(review);
  const rows = buildProductChangeRows(before, after);
  const changedCount = rows.filter((row) => row.changed).length;

  return (
    <div style={stackStyle}>
      <Alert
        type="warning"
        showIcon
        message={`商品资料变更：${changedCount || 0} 项变化`}
        description="默认只看变更字段；详情图文和完整属性保留在下方，方便审核员按商城商品详情理解。"
      />
      {renderSection('商品资料变化', renderChangeTable(rows))}
      {renderSection('主图对比', (
        <div style={imageCompareStyle}>
          <div>
            <Typography.Text type="secondary">修改前</Typography.Text>
            <div style={{ marginTop: 8 }}>{renderImage(before?.mainImageUrl, 120)}</div>
          </div>
          <div>
            <Typography.Text type="secondary">修改后</Typography.Text>
            <div style={{ marginTop: 8 }}>{renderImage(after?.mainImageUrl, 120)}</div>
          </div>
        </div>
      ))}
      {renderSection('修改后的商品详情图文', <DetailContentPreview value={after?.detailContent} />)}
      <Collapse
        items={[
          {
            key: 'before-detail',
            label: '查看修改前商品详情图文',
            children: <DetailContentPreview value={before?.detailContent} />,
          },
          {
            key: 'attributes',
            label: '查看修改后类目属性',
            children: renderAttributeTable(after),
          },
        ]}
      />
    </div>
  );
}

function buildSkuChangeSummary(before?: API.ProductDistribution.Sku, after?: API.ProductDistribution.Sku) {
  const fields = [
    ['SKU图', before?.skuImageUrl, after?.skuImageUrl],
    ['客户SKU', before?.sellerSkuCode, after?.sellerSkuCode],
    ['颜色', before?.color, after?.color],
    ['尺码', before?.size, after?.size],
    ['材质', before?.material, after?.material],
    ['款式', before?.style, after?.style],
    ['型号', before?.model, after?.model],
    ['容量', before?.capacity, after?.capacity],
    ['尺寸重量', formatDimension(before), formatDimension(after)],
    ['仓库', before?.sourceWarehouseNames || before?.warehouseKindSummary, after?.sourceWarehouseNames || after?.warehouseKindSummary],
  ] as const;
  return fields
    .filter(([, beforeValue, afterValue]) => isDifferent(beforeValue, afterValue))
    .map(([label]) => label)
    .join('、') || '未识别到字段变化';
}

function renderSkuInfoChangeTable(pairs: SkuPair[]) {
  const columns: ColumnsType<SkuPair> = [
    {
      title: 'SKU',
      dataIndex: 'key',
      width: 180,
      render: (_, row) => formatSkuIdentity(row.after || row.before, row.item),
    },
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      width: 150,
      render: (_, row) => (
        <div style={imageCompareStyle}>
          {renderImage(row.before?.skuImageUrl, 48)}
          {renderImage(row.after?.skuImageUrl, 48)}
        </div>
      ),
    },
    { title: '规格', dataIndex: 'spec', render: (_, row) => `${formatSkuSpecs(row.before)} -> ${formatSkuSpecs(row.after)}` },
    { title: '变化字段', dataIndex: 'diff', render: (_, row) => buildSkuChangeSummary(row.before, row.after) },
  ];

  return (
    <Table
      rowKey="key"
      size="small"
      columns={columns}
      dataSource={pairs}
      pagination={false}
      scroll={{ x: 980 }}
      locale={{ emptyText: '没有 SKU 资料变化' }}
    />
  );
}

function SkuInfoChangeReviewView({ review }: { review: API.ProductReview.Review }) {
  const pairs = getSkuPairs(review).filter((pair) => !!pair.after);

  return (
    <div style={stackStyle}>
      <Alert
        type="warning"
        showIcon
        message={`SKU资料变更：影响 ${pairs.length || review.skuCount || 0} 个 SKU`}
        description="这里只展示发生变化的 SKU，重点看图片、规格、编码、尺寸重量、仓库等非价格资料。"
      />
      {renderSection('SKU 资料变化', renderSkuInfoChangeTable(pairs))}
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

function renderPriceRisk(before?: API.ProductDistribution.Sku, after?: API.ProductDistribution.Sku) {
  if (after?.salePrice == null) {
    return <Tag color="error">缺销售价</Tag>;
  }
  if (after.supplyPrice != null && after.salePrice < after.supplyPrice) {
    return <Tag color="error">低于供货价</Tag>;
  }
  if (before?.salePrice != null && before.salePrice !== 0) {
    const percent = Math.abs((after.salePrice - before.salePrice) / before.salePrice) * 100;
    if (percent >= 30) {
      return <Tag color="warning">涨跌幅较大</Tag>;
    }
  }
  return <Tag color="success">价格正常</Tag>;
}

function renderPriceChangeTable(pairs: SkuPair[]) {
  const columns: ColumnsType<SkuPair> = [
    {
      title: 'SKU',
      dataIndex: 'sku',
      width: 180,
      render: (_, row) => formatSkuIdentity(row.after || row.before, row.item),
    },
    { title: '规格', dataIndex: 'spec', render: (_, row) => formatSkuSpecs(row.after || row.before) },
    {
      title: '原销售价',
      dataIndex: 'beforePrice',
      width: 130,
      render: (_, row) => formatMoney(row.before?.salePrice, row.before?.currencyCode || row.after?.currencyCode),
    },
    {
      title: '新销售价',
      dataIndex: 'afterPrice',
      width: 130,
      render: (_, row) => formatMoney(row.after?.salePrice, row.after?.currencyCode || row.before?.currencyCode),
    },
    { title: '变化', dataIndex: 'delta', width: 160, render: (_, row) => renderPriceDelta(row.before?.salePrice, row.after?.salePrice) },
    {
      title: '供货价',
      dataIndex: 'supplyPrice',
      width: 130,
      render: (_, row) => formatMoney(row.after?.supplyPrice ?? row.before?.supplyPrice, row.after?.currencyCode || row.before?.currencyCode),
    },
    { title: '风险', dataIndex: 'risk', width: 140, render: (_, row) => renderPriceRisk(row.before, row.after) },
  ];

  return (
    <Table
      rowKey="key"
      size="small"
      columns={columns}
      dataSource={pairs}
      pagination={false}
      scroll={{ x: 1080 }}
      locale={{ emptyText: '没有价格变化' }}
    />
  );
}

function PriceChangeReviewView({ review }: { review: API.ProductReview.Review }) {
  const pairs = getSkuPairs(review).filter((pair) => !!pair.before || !!pair.after);
  const increased = pairs.filter((pair) =>
    pair.before?.salePrice != null && pair.after?.salePrice != null && pair.after.salePrice > pair.before.salePrice).length;
  const decreased = pairs.filter((pair) =>
    pair.before?.salePrice != null && pair.after?.salePrice != null && pair.after.salePrice < pair.before.salePrice).length;

  return (
    <div style={stackStyle}>
      <Alert
        type="warning"
        showIcon
        message="价格变更审核"
        description="这个视图只看价格变化，默认展示原销售价、新销售价、差额、涨跌幅和低于供货价等风险。"
      />
      <div style={inlineGridStyle}>
        {renderMetric('影响 SKU', pairs.length || review.skuCount || '--')}
        {renderMetric('涨价 SKU', increased, increased ? '#cf1322' : undefined)}
        {renderMetric('降价 SKU', decreased, decreased ? '#3f8600' : undefined)}
        {renderMetric('原价格区间', formatRange(review.priceBeforeMin, review.priceBeforeMax, review.currencySummary))}
        {renderMetric('新价格区间', formatRange(review.priceAfterMin, review.priceAfterMax, review.currencySummary))}
      </div>
      {renderSection('SKU 价格对比', renderPriceChangeTable(pairs))}
    </div>
  );
}

function GenericReviewView({ review }: { review: API.ProductReview.Review }) {
  const product = getAfterProduct(review);
  const skus = getAfterSkus(review);

  return (
    <div style={stackStyle}>
      <Alert type="info" showIcon message="审核预览" description="暂未识别到专用审核视图，按商品和 SKU 信息展示。" />
      {renderSection('商品基础信息', renderProductBasicInfo(product, review))}
      {renderSection('SKU', renderSkuTable(skus))}
    </div>
  );
}

export default function ProductReviewBusinessPreview({ review }: ProductReviewBusinessPreviewProps) {
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
      {review.reviewType === 'NEW_PRODUCT' ? <NewProductReviewView review={review} /> : null}
      {review.reviewType === 'ADD_SKU' ? <AddSkuReviewView review={review} /> : null}
      {review.reviewType === 'EDIT_PRODUCT_INFO' ? <ProductInfoChangeReviewView review={review} /> : null}
      {review.reviewType === 'EDIT_SKU_INFO' ? <SkuInfoChangeReviewView review={review} /> : null}
      {review.reviewType === 'EDIT_PRICE' ? <PriceChangeReviewView review={review} /> : null}
      {!review.reviewType || !['NEW_PRODUCT', 'ADD_SKU', 'EDIT_PRODUCT_INFO', 'EDIT_SKU_INFO', 'EDIT_PRICE'].includes(review.reviewType)
        ? <GenericReviewView review={review} />
        : null}
    </div>
  );
}
