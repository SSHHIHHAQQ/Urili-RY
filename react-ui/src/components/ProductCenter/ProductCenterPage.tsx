import { EyeOutlined } from '@ant-design/icons';
import { type ActionType, type ProColumns, ProTable } from '@ant-design/pro-components';
import { Button, Empty, Image, Space, Tag, Typography } from 'antd';
import { useMemo, useRef, useState } from 'react';
import {
  formatPriceRange,
  inventoryStatusColor,
  inventoryStatusText,
  resolveResourceUrl,
  warehouseKindText,
} from '@/pages/Product/Distribution/constants';
import { message } from '@/utils/feedback';
import {
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import ProductCenterDetailModal from './ProductCenterDetailModal';
import styles from './style.module.css';

type ProductCenterPageProps = {
  canList?: boolean;
  canQuery?: boolean;
  storageKey?: string;
  fetchList: (params?: Record<string, any>) => Promise<API.ProductCenter.PageResult>;
  fetchProduct: (spuId: number) => Promise<API.ProductCenter.InfoResult>;
};

function displayText(value?: string) {
  return value?.trim() || '--';
}

function renderWarehouseKindTag(kind?: string) {
  if (!kind) return <span className={styles.emptyDash}>--</span>;
  if (kind === 'MIXED') {
    return <Tag color="cyan">{warehouseKindText[kind] || '混合'}</Tag>;
  }
  return (
    <Tag color={kind === 'official' ? 'blue' : 'purple'}>
      {warehouseKindText[kind] || kind}
    </Tag>
  );
}

function renderInventoryStatus(record: API.ProductCenter.Product) {
  const status = record.inventoryStatus;
  if (status) {
    return (
      <Tag color={inventoryStatusColor[status] || 'default'}>
        {inventoryStatusText[status] || status}
      </Tag>
    );
  }
  if (record.availableStock === undefined || record.availableStock === null) {
    return <Tag>库存待同步</Tag>;
  }
  return record.availableStock > 0 ? <Tag color="success">有货</Tag> : <Tag>缺货</Tag>;
}

function renderSkuTags(record: API.ProductCenter.Product) {
  const codes = record.visibleSystemSkuCodes || [];
  if (!codes.length) {
    return <Tag>系统SKU --</Tag>;
  }
  const visibleCodes = codes.slice(0, 2);
  return (
    <>
      {visibleCodes.map((code) => (
        <Tag key={code}>SKU {code}</Tag>
      ))}
      {codes.length > visibleCodes.length ? <Tag>+{codes.length - visibleCodes.length}</Tag> : null}
    </>
  );
}

function renderProductNameCell(record: API.ProductCenter.Product) {
  return (
    <div className={styles.productNameCell}>
      <div className={styles.productImage}>
        {record.mainImageUrl ? (
          <Image
            width={64}
            height={64}
            src={resolveResourceUrl(record.mainImageUrl)}
            style={{ objectFit: 'cover' }}
          />
        ) : (
          <div className={styles.imagePlaceholder}>暂无图片</div>
        )}
      </div>
      <div className={styles.productText}>
        <div className={styles.productTitle}>{displayText(record.productName)}</div>
        <div className={styles.productSubTitle}>{displayText(record.productNameEn)}</div>
        <div className={styles.metaLine}>
          <Tag color="processing">SPU {displayText(record.systemSpuCode)}</Tag>
          {renderSkuTags(record)}
        </div>
      </div>
    </div>
  );
}

function renderPrice(record: API.ProductCenter.Product) {
  const price = formatPriceRange(record.salePriceMin, record.salePriceMax);
  if (price === '--') {
    return '--';
  }
  return [price, record.currencySummary].filter(Boolean).join(' ');
}

export default function ProductCenterPage({
  canList = true,
  canQuery = false,
  storageKey = 'product-center',
  fetchList,
  fetchProduct,
}: ProductCenterPageProps) {
  const actionRef = useRef<ActionType>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [currentProduct, setCurrentProduct] = useState<API.ProductCenter.Product>();

  const openDetail = async (record: API.ProductCenter.Product) => {
    if (!canQuery || !record.spuId) {
      return;
    }
    const resp = await fetchProduct(record.spuId);
    if (resp.code !== 200 || !resp.data) {
      message.error(resp.msg || '商品详情加载失败');
      return;
    }
    setCurrentProduct(resp.data);
    setDetailOpen(true);
  };

  const columns = useMemo<ProColumns<API.ProductCenter.Product>[]>(
    () => [
      {
        title: '关键词',
        dataIndex: 'keyword',
        hideInTable: true,
      },
      {
        title: '中文名称',
        dataIndex: 'productName',
        hideInTable: true,
      },
      {
        title: '英文名称',
        dataIndex: 'productNameEn',
        hideInTable: true,
      },
      {
        title: '系统SPU',
        dataIndex: 'systemSpuCode',
        hideInTable: true,
      },
      {
        title: '系统SKU',
        dataIndex: 'systemSkuCode',
        hideInTable: true,
      },
      {
        title: '商品基础信息',
        search: false,
        width: 460,
        render: (_, record) => renderProductNameCell(record),
      },
      {
        title: '销售与库存',
        search: false,
        width: 220,
        render: (_, record) => (
          <div className={styles.stack}>
            <Typography.Text className={styles.priceText}>{renderPrice(record)}</Typography.Text>
            <Space size={4} wrap>
              {renderInventoryStatus(record)}
              <span className={styles.mutedText}>
                可售 {record.availableStock ?? '--'} 件 / {record.warehouseCount ?? 0} 仓
              </span>
            </Space>
          </div>
        ),
      },
      {
        title: '类目与卖点',
        search: false,
        width: 300,
        render: (_, record) => (
          <div className={styles.stack}>
            <Typography.Text>{displayText(record.categoryName)}</Typography.Text>
            {record.sellingPoint ? (
              <div className={styles.sellingPoint}>{record.sellingPoint}</div>
            ) : (
              <span className={styles.emptyDash}>暂无卖点</span>
            )}
          </div>
        ),
      },
      {
        title: '发货信息',
        search: false,
        width: 220,
        render: (_, record) => (
          <div className={styles.stack}>
            <Space size={4} wrap>
              {renderWarehouseKindTag(record.warehouseKindSummary)}
              <span className={styles.mutedText}>{record.warehouseCount ?? 0} 个发货仓</span>
            </Space>
            <span className={styles.mutedText}>
              库存同步：{record.stockUpdateTime || '--'}
            </span>
          </div>
        ),
      },
      {
        title: '操作',
        valueType: 'option',
        width: 100,
        fixed: 'right',
        render: (_, record) => [
          <Button
            key="detail"
            type="link"
            size="small"
            icon={<EyeOutlined />}
            hidden={!canQuery}
            onClick={() => openDetail(record)}
          >
            详情
          </Button>,
        ],
      },
    ],
    [canQuery],
  );

  return (
    <>
      <ProTable<API.ProductCenter.Product>
        actionRef={actionRef}
        rowKey="spuId"
        columns={columns}
        scroll={getProTableScroll(1300)}
        tableLayout="fixed"
        pagination={getProTablePagination()}
        search={getPersistedProTableSearch({ fieldCount: 5, defaultFormItemsNumber: 5 }, storageKey)}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无已上架商品" /> }}
        request={async ({ current, pageSize, ...params }) => {
          if (!canList) {
            return { data: [], total: 0, success: false };
          }
          const resp = await fetchList({
            ...params,
            pageNum: current,
            pageSize,
          });
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
      />
      <ProductCenterDetailModal
        open={detailOpen}
        product={currentProduct}
        onClose={() => {
          setDetailOpen(false);
          setCurrentProduct(undefined);
        }}
      />
    </>
  );
}
