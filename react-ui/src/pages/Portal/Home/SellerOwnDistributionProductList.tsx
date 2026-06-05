import { ReloadOutlined } from '@ant-design/icons';
import {
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import {
  Button,
  Descriptions,
  Empty,
  Image,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useRef, useState } from 'react';
import {
  buildSkuDimensionText,
  buildSkuSpecText,
  formatPriceRange,
  getSalesStatusText,
  resolveResourceUrl,
} from '@/pages/Product/Distribution/constants';
import {
  getSellerPortalDistributionProduct,
  getSellerPortalDistributionProductSkus,
  getSellerPortalDistributionProducts,
} from '@/services/portal/session';
import { message } from '@/utils/feedback';
import {
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

type SellerProductRow = API.Partner.SellerPortalProduct & {
  uiRowKey: string;
};

function displayText(value?: string | number | null) {
  return value === undefined || value === null || value === ''
    ? '-'
    : String(value);
}

function statusTag(status?: string) {
  const label = getSalesStatusText(status);
  const color =
    status === 'ON_SALE'
      ? 'success'
      : status === 'DISABLED'
        ? 'error'
        : undefined;
  return <Tag color={color}>{label}</Tag>;
}

const PRODUCT_STATUS_VALUE_ENUM = {
  DRAFT: { text: getSalesStatusText('DRAFT') },
  READY: { text: getSalesStatusText('READY') },
  ON_SALE: { text: getSalesStatusText('ON_SALE') },
  OFF_SALE: { text: getSalesStatusText('OFF_SALE') },
  DISABLED: { text: getSalesStatusText('DISABLED') },
};

const PRODUCT_STATUS_OPTIONS = Object.entries(PRODUCT_STATUS_VALUE_ENUM).map(
  ([value, item]) => ({ value, label: item.text }),
);

const SellerOwnDistributionProductList: React.FC = () => {
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [current, setCurrent] = useState<API.Partner.SellerPortalProduct>();
  const [skuRows, setSkuRows] = useState<API.Partner.SellerPortalProductSku[]>(
    [],
  );
  const actionRef = useRef<ActionType | undefined>(undefined);

  const openDetail = async (record: API.Partner.SellerPortalProduct) => {
    if (!record.spuId) {
      return;
    }
    setDetailOpen(true);
    setDetailLoading(true);
    setCurrent(record);
    setSkuRows([]);
    try {
      const [detailResponse, skuResponse] = await Promise.all([
        getSellerPortalDistributionProduct(record.spuId),
        getSellerPortalDistributionProductSkus(record.spuId),
      ]);
      if (detailResponse.code !== 200) {
        message.error(detailResponse.msg || '商品详情加载失败');
        return;
      }
      if (skuResponse.code !== 200) {
        message.error(skuResponse.msg || '商品 SKU 加载失败');
        return;
      }
      setCurrent(detailResponse.data);
      setSkuRows(skuResponse.data || []);
    } catch (error) {
      console.log(error);
      message.error('商品详情加载失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const columns: ProColumns<SellerProductRow>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '商品',
      dataIndex: 'productName',
      key: 'productName',
      width: 320,
      search: false,
      render: (_, record) => (
        <Space size={10}>
          {record.mainImageUrl ? (
            <Image
              width={48}
              height={48}
              src={resolveResourceUrl(record.mainImageUrl)}
              style={{ objectFit: 'cover' }}
            />
          ) : (
            <div style={{ width: 48, height: 48, background: '#f0f0f0' }} />
          )}
          <Space orientation="vertical" size={0}>
            <Typography.Text ellipsis style={{ maxWidth: 220 }}>
              {displayText(record.productName)}
            </Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {displayText(record.productNameEn)}
            </Typography.Text>
          </Space>
        </Space>
      ),
    },
    {
      title: '客户SPU',
      dataIndex: 'sellerSpuCode',
      key: 'sellerSpuCode',
      width: 140,
      render: (_, record) => displayText(record.sellerSpuCode),
    },
    {
      title: '客户SKU',
      dataIndex: 'sellerSkuCode',
      hideInTable: true,
    },
    {
      title: '类目',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 160,
      search: false,
      render: (_, record) => displayText(record.categoryName),
    },
    {
      title: '价格',
      key: 'price',
      width: 160,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <span>
            {formatPriceRange(record.salePriceMin, record.salePriceMax)}
          </span>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {displayText(record.currencySummary)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: 'SKU',
      dataIndex: 'skuCount',
      key: 'skuCount',
      width: 80,
      search: false,
      render: (_, record) => displayText(record.skuCount),
    },
    {
      title: '状态',
      dataIndex: 'spuStatus',
      key: 'spuStatus',
      width: 96,
      valueType: 'select',
      valueEnum: PRODUCT_STATUS_VALUE_ENUM,
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: PRODUCT_STATUS_OPTIONS,
      },
      render: (_, record) => statusTag(record.spuStatus),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 88,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => openDetail(record)}>
          详情
        </Button>
      ),
    },
  ];

  const skuColumns: ColumnsType<API.Partner.SellerPortalProductSku> = [
    {
      title: '客户SKU',
      dataIndex: 'sellerSkuCode',
      key: 'sellerSkuCode',
      width: 150,
      render: displayText,
    },
    {
      title: 'SKU规格',
      key: 'spec',
      width: 220,
      render: (_, record) => buildSkuSpecText(record, skuRows) || '-',
    },
    {
      title: '尺寸重量',
      key: 'dimension',
      width: 220,
      render: (_, record) => buildSkuDimensionText(record) || '-',
    },
    {
      title: '销售价',
      dataIndex: 'salePrice',
      key: 'salePrice',
      width: 100,
      render: displayText,
    },
    {
      title: '币种',
      dataIndex: 'currencyCode',
      key: 'currencyCode',
      width: 80,
      render: displayText,
    },
    {
      title: '状态',
      dataIndex: 'skuStatus',
      key: 'skuStatus',
      width: 96,
      render: statusTag,
    },
  ];

  return (
    <>
      <ProTable<SellerProductRow>
        actionRef={actionRef}
        rowKey="uiRowKey"
        headerTitle="我的商城商品"
        columns={columns}
        search={getPersistedProTableSearch(
          { labelWidth: 96, defaultFormItemsNumber: 4 },
          'seller-portal-distribution-product',
        )}
        pagination={getProTablePagination({
          defaultPageSize: 5,
          pageSizeOptions: [5, 10, 20],
        })}
        scroll={getProTableScroll(1040)}
        request={async ({ current: currentPage, pageSize: currentPageSize, ...params }) => {
          const response = await getSellerPortalDistributionProducts({
            keyword: params.keyword,
            sellerSpuCode: params.sellerSpuCode,
            sellerSkuCode: params.sellerSkuCode,
            spuStatus: params.spuStatus,
            pageNum: currentPage,
            pageSize: currentPageSize,
          });
          if (response.code !== 200) {
            message.error(response.msg || '商品加载失败');
            return { data: [], total: 0, success: false };
          }
          return {
            data: (response.rows || []).map((row, index) => ({
              ...row,
              uiRowKey: `${row.spuId || 'product'}-${row.sellerSpuCode || ''}-${index}`,
            })),
            total: response.total || 0,
            success: true,
          };
        }}
        toolBarRender={() => [
          <Button
            key="refresh"
            icon={<ReloadOutlined />}
            onClick={() => actionRef.current?.reload()}
          >
            刷新
          </Button>,
        ]}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
      />

      <Modal
        title="商品详情"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={920}
        destroyOnHidden
      >
        <Space orientation="vertical" size={14} style={{ width: '100%' }}>
          <Descriptions column={2} size="small" bordered>
            <Descriptions.Item label="客户SPU">
              {displayText(current?.sellerSpuCode)}
            </Descriptions.Item>
            <Descriptions.Item label="商品状态">
              {statusTag(current?.spuStatus)}
            </Descriptions.Item>
            <Descriptions.Item label="商品名称">
              {displayText(current?.productName)}
            </Descriptions.Item>
            <Descriptions.Item label="英文名称">
              {displayText(current?.productNameEn)}
            </Descriptions.Item>
            <Descriptions.Item label="类目">
              {displayText(current?.categoryName)}
            </Descriptions.Item>
            <Descriptions.Item label="价格">
              {formatPriceRange(current?.salePriceMin, current?.salePriceMax)}
            </Descriptions.Item>
            <Descriptions.Item label="卖点" span={2}>
              {displayText(current?.sellingPoint)}
            </Descriptions.Item>
          </Descriptions>
          <Table<API.Partner.SellerPortalProductSku>
            size="small"
            rowKey={(record) =>
              `${record.spuId || current?.spuId || 0}-${record.skuId || record.sellerSkuCode || 'sku'}`
            }
            loading={detailLoading}
            columns={skuColumns}
            dataSource={skuRows}
            pagination={false}
            scroll={{ x: 860 }}
            locale={{
              emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />,
            }}
          />
        </Space>
      </Modal>
    </>
  );
};

export default SellerOwnDistributionProductList;
