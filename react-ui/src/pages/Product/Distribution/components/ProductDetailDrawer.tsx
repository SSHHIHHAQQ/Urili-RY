import { Descriptions, Drawer, Image, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  buildSkuDimensionText,
  buildSkuSpecText,
  getControlStatusText,
  getSalesStatusText,
  resolveResourceUrl,
} from '../constants';
import DetailContentPreview from './DetailContentPreview';
import styles from '../style.module.css';

type ProductDetailDrawerProps = {
  open: boolean;
  product?: API.ProductDistribution.Spu;
  onClose: () => void;
};

function statusText(status?: string) {
  return getSalesStatusText(status);
}

function controlStatusText(status?: string) {
  return getControlStatusText(status || 'NORMAL');
}

function controlStatusTag(record: API.ProductDistribution.Sku) {
  if (record.spuControlStatus === 'DISABLED') {
    return <Tag color="error">SPU停用</Tag>;
  }
  const status = record.controlStatus || 'NORMAL';
  return <Tag color={status === 'DISABLED' ? 'error' : 'success'}>{controlStatusText(status)}</Tag>;
}

function amountText(value?: number | null) {
  return value === undefined || value === null ? '--' : String(value);
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

function parseJsonArrayText(value?: string) {
  if (!value) return '';
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.filter(Boolean).join(' / ') : '';
  } catch {
    return value;
  }
}

function formatAttributeValue(item: API.ProductDistribution.AttributeValue) {
  if (item.attributeType === 'BOOLEAN') {
    if (item.valueCode === 'Y') return '是';
    if (item.valueCode === 'N') return '否';
  }
  if (item.attributeType === 'MULTI_SELECT') {
    return parseJsonArrayText(item.valueJson) || '--';
  }
  return item.valueText
    || item.valueCode
    || (item.valueNumber !== undefined && item.valueNumber !== null ? String(item.valueNumber) : '')
    || item.valueDate
    || item.valueJson
    || '--';
}

export default function ProductDetailDrawer({
  open,
  product,
  onClose,
}: ProductDetailDrawerProps) {
  const skuColumns: ColumnsType<API.ProductDistribution.Sku> = [
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      width: 72,
      render: (url) =>
        url ? <Image width={44} height={44} src={resolveResourceUrl(url)} style={{ objectFit: 'cover' }} /> : '--',
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
    { title: '币种', dataIndex: 'currencyCode', width: 90 },
    {
      title: '销售状态',
      dataIndex: 'skuStatus',
      width: 100,
      render: (value) => <Tag>{statusText(value)}</Tag>,
    },
    {
      title: '管控',
      dataIndex: 'controlStatus',
      width: 100,
      render: (_, record) => controlStatusTag(record),
    },
  ];

  return (
    <Drawer
      title="商城商品详情"
      open={open}
      onClose={onClose}
      size="large"
      destroyOnClose
    >
      {product ? (
        <div className={styles.detailDrawerBody}>
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="系统SPU">{product.systemSpuCode || '--'}</Descriptions.Item>
            <Descriptions.Item label="客户SPU">{product.sellerSpuCode || '--'}</Descriptions.Item>
            <Descriptions.Item label="中文标题">{product.productName || '--'}</Descriptions.Item>
            <Descriptions.Item label="英文标题">{product.productNameEn || '--'}</Descriptions.Item>
            <Descriptions.Item label="卖家">{product.sellerName || '--'}</Descriptions.Item>
            <Descriptions.Item label="商品分类">{product.categoryName || '--'}</Descriptions.Item>
            <Descriptions.Item label="销售状态">{statusText(product.spuStatus)}</Descriptions.Item>
            <Descriptions.Item label="管控状态">
              <Tag color={product.controlStatus === 'DISABLED' ? 'error' : 'success'}>
                {controlStatusText(product.controlStatus)}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="来源">{product.sourceType || '--'}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{product.updateTime || '--'}</Descriptions.Item>
            <Descriptions.Item label="卖点" span={2}>{product.sellingPoint || '--'}</Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>{product.remark || '--'}</Descriptions.Item>
            <Descriptions.Item label="SPU主图" span={2}>
              {product.mainImageUrl ? (
                <Image width={96} src={resolveResourceUrl(product.mainImageUrl)} />
              ) : '--'}
            </Descriptions.Item>
          </Descriptions>

          <div>
            <div className={styles.sectionTitle}>详情图文</div>
            <DetailContentPreview value={product.detailContent} />
          </div>

          <Table
            rowKey="skuId"
            size="small"
            pagination={false}
            columns={skuColumns}
            dataSource={product.skus || []}
            scroll={{ x: 1540 }}
          />

          <Descriptions bordered size="small" column={1} title="类目属性">
            {(product.attributeValues || []).map((item) => (
              <Descriptions.Item key={item.valueId || item.attributeId} label={item.attributeName || item.attributeCode}>
                {formatAttributeValue(item)}
              </Descriptions.Item>
            ))}
          </Descriptions>
        </div>
      ) : null}
    </Drawer>
  );
}
