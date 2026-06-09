import { Empty, Modal, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { getCategorySchema } from '@/services/product/product';
import {
  buildAttributeLabelMap,
  buildOptionLabelMap,
  formatAttributeValue,
  resolveAttributeLabel,
} from '@/pages/Product/utils/attributeDisplay';
import {
  buildSkuDimensionText,
  buildSkuSpecText,
  getControlStatusText,
  getSalesStatusText,
  inventoryStatusColor,
  inventoryStatusText,
  resolveResourceUrl,
  sourceTypeValueEnum,
  warehouseKindText,
} from '../constants';
import DetailContentPreview from './DetailContentPreview';
import styles from '../style.module.css';

type ProductDetailDrawerProps = {
  open: boolean;
  product?: API.ProductDistribution.Spu;
  categoryPath?: string;
  canPreviewCategorySchema?: boolean;
  onClose: () => void;
};

type ReadonlyFieldProps = {
  label: string;
  value?: ReactNode;
  span?: 1 | 2 | 3;
  multiline?: boolean;
};

const GALLERY_SLOT_KEYS = [
  'gallery-slot-1',
  'gallery-slot-2',
  'gallery-slot-3',
  'gallery-slot-4',
  'gallery-slot-5',
  'gallery-slot-6',
  'gallery-slot-7',
];

function displayText(value?: ReactNode) {
  if (value === undefined || value === null || value === '') {
    return '--';
  }
  return value;
}

function statusText(status?: string) {
  return getSalesStatusText(status);
}

function controlStatusText(status?: string) {
  return getControlStatusText(status || 'NORMAL');
}

function sourceTypeText(sourceType?: string) {
  if (!sourceType) return '--';
  const option = sourceTypeValueEnum[sourceType];
  if (option && typeof option === 'object' && 'text' in option) {
    return String(option.text);
  }
  return sourceType;
}

function salesStatusTag(status?: string) {
  const color = status === 'ON_SALE' ? 'success' : status === 'READY' ? 'warning' : status === 'OFF_SALE' ? 'processing' : 'default';
  return <Tag color={color}>{statusText(status)}</Tag>;
}

function controlStatusTag(status?: string) {
  const normalizedStatus = status || 'NORMAL';
  return (
    <Tag color={normalizedStatus === 'DISABLED' ? 'error' : 'success'}>
      {controlStatusText(normalizedStatus)}
    </Tag>
  );
}

function skuControlStatusTag(record: API.ProductDistribution.Sku) {
  if (record.spuControlStatus === 'DISABLED') {
    return <Tag color="error">SPU停用</Tag>;
  }
  return controlStatusTag(record.controlStatus);
}

function amountText(value?: number | null) {
  return value === undefined || value === null ? '--' : String(value);
}

function warehouseKindLabel(kind?: string) {
  return kind ? warehouseKindText[kind] || kind : '--';
}

function renderSourceSku(record: API.ProductDistribution.Sku) {
  if (!record.masterSku) return '--';
  return (
    <div>
      <Space size={4}>
        <span>{record.masterSku}</span>
        {record.lockStatus === 'LOCKED' ? <Tag color="blue">已锁定</Tag> : null}
      </Space>
      <div className={styles.mutedText}>{record.masterProductNameSnapshot || '--'}</div>
    </div>
  );
}

function renderInventoryStatus(record: API.ProductDistribution.Sku) {
  if (!record.inventoryStatus) return '--';
  return (
    <Tag color={inventoryStatusColor[record.inventoryStatus] || 'default'}>
      {inventoryStatusText[record.inventoryStatus] || record.inventoryStatus}
    </Tag>
  );
}

function ReadonlyField({ label, value, span = 1, multiline }: ReadonlyFieldProps) {
  return (
    <div className={styles.readonlyField} data-span={span}>
      <div className={styles.readonlyFieldLabel}>{label}</div>
      <div className={`${styles.readonlyFieldValue} ${multiline ? styles.readonlyFieldValueMultiline : ''}`}>
        {displayText(value)}
      </div>
    </div>
  );
}

function ReadonlyImageSlot({
  label,
  required,
  url,
  reserveLabelSpace,
}: {
  label?: string;
  required?: boolean;
  url?: string;
  reserveLabelSpace?: boolean;
}) {
  return (
    <div className={`${styles.imageSlotWrap} ${styles.readonlyImageSlotWrap}`}>
      {label ? (
        <div className={styles.imageSlotLabel}>
          {label}
          {required ? <span>*</span> : null}
        </div>
      ) : reserveLabelSpace ? (
        <div className={styles.imageSlotLabelPlaceholder} />
      ) : null}
      <div className={`${styles.imageSlot} ${url ? styles.imageSlotFilled : ''} ${styles.readonlyImageSlot}`}>
        {url ? <img src={resolveResourceUrl(url)} alt={label || '商品图片'} /> : <span>暂无图片</span>}
      </div>
    </div>
  );
}

function getGalleryUrls(product?: API.ProductDistribution.Spu) {
  const images = product?.images || [];
  return images
    .filter((image): image is API.ProductDistribution.ProductImage & { imageUrl: string } =>
      image.imageRole === 'GALLERY' && !!image.imageUrl)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map((image) => image.imageUrl)
    .slice(0, 7);
}

export default function ProductDetailDrawer({
  open,
  product,
  categoryPath,
  canPreviewCategorySchema,
  onClose,
}: ProductDetailDrawerProps) {
  const [categorySchema, setCategorySchema] = useState<API.Product.CategoryAttribute[]>([]);
  const galleryUrls = getGalleryUrls(product);
  const normalizedGallerySlots = GALLERY_SLOT_KEYS.map((slotKey, index) => ({
    slotKey,
    url: galleryUrls[index] || '',
  }));
  const attributeLabelMap = useMemo(() => {
    return buildAttributeLabelMap(categorySchema);
  }, [categorySchema]);
  const optionLabelMap = useMemo(() => {
    return buildOptionLabelMap(categorySchema);
  }, [categorySchema]);

  useEffect(() => {
    if (!open || !product?.categoryId || !canPreviewCategorySchema) {
      setCategorySchema([]);
      return undefined;
    }
    let cancelled = false;
    getCategorySchema(product.categoryId)
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
  }, [canPreviewCategorySchema, open, product?.categoryId]);

  const skuColumns: ColumnsType<API.ProductDistribution.Sku> = [
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      width: 72,
      render: (url) =>
        url ? <img className={styles.readonlySkuImage} src={resolveResourceUrl(String(url))} alt="SKU图" /> : '--',
    },
    { title: '系统SKU', dataIndex: 'systemSkuCode', width: 160 },
    { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 160 },
    {
      title: '来源SKU',
      width: 220,
      render: (_, record) => renderSourceSku(record),
    },
    {
      title: 'SKU规格',
      width: 220,
      render: (_, record) => buildSkuSpecText(record, product?.skus || []) || '--',
    },
    {
      title: '尺寸重量',
      width: 220,
      render: (_, record) => buildSkuDimensionText(record) || '--',
    },
    { title: '供货价', dataIndex: 'supplyPrice', width: 100, render: (value) => amountText(value as number) },
    { title: '销售价', dataIndex: 'salePrice', width: 100, render: (value) => amountText(value as number) },
    { title: '币种', dataIndex: 'currencyCode', width: 90, render: (value) => displayText(value as string) },
    {
      title: '可售库存',
      dataIndex: 'availableStock',
      width: 100,
      render: (value) => displayText(value as number | undefined),
    },
    {
      title: '仓库数',
      dataIndex: 'warehouseCount',
      width: 90,
      render: (value) => displayText(value as number | undefined),
    },
    {
      title: '库存状态',
      dataIndex: 'inventoryStatus',
      width: 120,
      render: (_, record) => renderInventoryStatus(record),
    },
    {
      title: '销售状态',
      dataIndex: 'skuStatus',
      width: 100,
      render: (value) => salesStatusTag(value as string),
    },
    {
      title: '管控',
      dataIndex: 'controlStatus',
      width: 100,
      render: (_, record) => skuControlStatusTag(record),
    },
  ];

  const warehouseColumns: ColumnsType<API.ProductDistribution.ProductWarehouse> = [
    {
      title: '仓库类型',
      dataIndex: 'warehouseKind',
      width: 120,
      render: (value) => <Tag color={value === 'official' ? 'blue' : 'default'}>{warehouseKindLabel(value as string)}</Tag>,
    },
    { title: '仓库编码', dataIndex: 'warehouseCode', width: 160, render: (value) => displayText(value as string) },
    { title: '仓库名称', dataIndex: 'warehouseName', width: 220, render: (value) => displayText(value as string) },
    { title: '币种', dataIndex: 'settlementCurrency', width: 100, render: (value) => displayText(value as string) },
  ];

  return (
    <Modal
      title="商城商品详情"
      open={open}
      width={1180}
      style={{ top: 24 }}
      footer={null}
      destroyOnClose
      className={styles.detailModal}
      onCancel={onClose}
    >
      {product ? (
        <div className={styles.detailModalBody}>
          <div className={styles.readonlySummary}>
            <span>系统 SPU：{product.systemSpuCode || '-'}</span>
            <span>来源：{sourceTypeText(product.sourceType)}</span>
            <span>SKU 数：{product.skuCount ?? product.skus?.length ?? '-'}</span>
            <span>销售状态：{salesStatusTag(product.spuStatus)}</span>
            <span>管控：{controlStatusTag(product.controlStatus)}</span>
          </div>

          <section className={styles.formSection}>
            <div className={styles.sectionTitle}>基础信息</div>
            <div className={styles.readonlyGrid}>
              <ReadonlyField label="商品中文标题" value={product.productName} />
              <ReadonlyField label="商品英文标题" value={product.productNameEn} />
              <ReadonlyField label="客户SPU" value={product.sellerSpuCode} />
              <ReadonlyField label="绑定卖家" value={product.sellerName} />
              <ReadonlyField label="商品分类" value={categoryPath || product.categoryName} />
              <ReadonlyField label="仓库类型" value={warehouseKindLabel(product.warehouseKind || product.warehouseKindSummary)} />
              <ReadonlyField label="系统SPU" value={product.systemSpuCode} />
              <ReadonlyField label="创建时间" value={product.createTime} />
              <ReadonlyField label="更新时间" value={product.updateTime} />
            </div>
          </section>

          <section className={styles.formSection}>
            <div className={styles.imageSection}>
              <div className={styles.imageSectionHeader}>
                <div>
                  <div className={styles.sectionTitle}>商品图片</div>
                  <div className={styles.sectionHint}>只读展示 SPU 主图和轮播图，图片维护请进入编辑页。</div>
                </div>
              </div>
              <div className={styles.imageGrid}>
                <ReadonlyImageSlot label="主图" required url={product.mainImageUrl} />
                {normalizedGallerySlots.map((slot, index) => (
                  <ReadonlyImageSlot
                    key={slot.slotKey}
                    label={index === 0 ? '尺寸图' : undefined}
                    reserveLabelSpace
                    url={slot.url}
                  />
                ))}
              </div>
            </div>
          </section>

          <section className={styles.formSection}>
            <div className={styles.sectionTitle}>类目属性</div>
            {(product.attributeValues || []).length ? (
              <div className={styles.readonlyGrid}>
                {(product.attributeValues || []).map((item) => (
                  <ReadonlyField
                    key={item.valueId || item.attributeId}
                    label={resolveAttributeLabel(item, attributeLabelMap)}
                    value={formatAttributeValue(item, optionLabelMap)}
                  />
                ))}
              </div>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无类目属性" />
            )}
          </section>

          <section className={styles.formSection}>
            <div className={styles.sectionTitle}>详情图文</div>
            <DetailContentPreview value={product.detailContent} />
          </section>

          <section className={styles.formSection}>
            <div className={styles.sectionTitle}>SKU 信息</div>
            <Table
              rowKey={(record) => String(record.skuId || record.systemSkuCode || record.sellerSkuCode)}
              size="small"
              pagination={false}
              columns={skuColumns}
              dataSource={product.skus || []}
              scroll={{ x: 1760 }}
            />
          </section>

          <section className={styles.formSection}>
            <div className={styles.sectionTitle}>发货仓库</div>
            <Table
              rowKey={(record) => String(record.id || record.warehouseId || record.warehouseCode)}
              size="small"
              pagination={false}
              columns={warehouseColumns}
              dataSource={product.warehouses || []}
              locale={{ emptyText: '暂无发货仓库' }}
            />
          </section>
        </div>
      ) : (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无商品详情" />
      )}
    </Modal>
  );
}
