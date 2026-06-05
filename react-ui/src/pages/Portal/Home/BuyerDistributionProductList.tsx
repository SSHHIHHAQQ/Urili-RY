import { ReloadOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
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
import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  buildSkuDimensionText,
  buildSkuSpecText,
  formatPriceRange,
  getSalesStatusText,
  resolveResourceUrl,
} from '@/pages/Product/Distribution/constants';
import {
  getBuyerPortalDistributionProduct,
  getBuyerPortalDistributionProductSkus,
  getBuyerPortalDistributionProducts,
} from '@/services/portal/session';
import { message } from '@/utils/feedback';

type BuyerProductRow = API.Partner.BuyerPortalProduct & {
  uiRowKey: string;
};

type BuyerSkuRow = API.Partner.BuyerPortalProductSku & {
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

const BuyerDistributionProductList: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [rows, setRows] = useState<BuyerProductRow[]>([]);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [detailOpen, setDetailOpen] = useState(false);
  const [current, setCurrent] = useState<API.Partner.BuyerPortalProduct>();
  const [skuRows, setSkuRows] = useState<BuyerSkuRow[]>([]);
  const listRequestSeq = useRef(0);

  const loadProducts = useCallback(
    async (currentPage: number, currentPageSize: number) => {
      const requestSeq = listRequestSeq.current + 1;
      listRequestSeq.current = requestSeq;
      setLoading(true);
      try {
        const response = await getBuyerPortalDistributionProducts({
          pageNum: currentPage,
          pageSize: currentPageSize,
        });
        if (listRequestSeq.current !== requestSeq) {
          return;
        }
        if (response.code !== 200) {
          message.error(response.msg || '商品加载失败');
          setRows([]);
          setTotal(0);
          return;
        }
        setPageNum(currentPage);
        setPageSize(currentPageSize);
        setTotal(response.total || 0);
        setRows(
          (response.rows || []).map((row, index) => ({
            ...row,
            uiRowKey: `${row.spuId || 'product'}-${index}`,
          })),
        );
      } catch (error) {
        console.log(error);
        if (listRequestSeq.current === requestSeq) {
          message.error('商品加载失败');
          setRows([]);
          setTotal(0);
        }
      } finally {
        if (listRequestSeq.current === requestSeq) {
          setLoading(false);
        }
      }
    },
    [],
  );

  useEffect(() => {
    loadProducts(1, 5);
  }, [loadProducts]);

  const openDetail = async (record: API.Partner.BuyerPortalProduct) => {
    if (!record.spuId) {
      return;
    }
    setDetailOpen(true);
    setDetailLoading(true);
    setCurrent(record);
    setSkuRows([]);
    try {
      const [detailResponse, skuResponse] = await Promise.all([
        getBuyerPortalDistributionProduct(record.spuId),
        getBuyerPortalDistributionProductSkus(record.spuId),
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
      setSkuRows(
        (skuResponse.data || []).map((row, index) => ({
          ...row,
          uiRowKey: `${row.spuId || record.spuId || 0}-${row.skuId || 'sku'}-${index}`,
        })),
      );
    } catch (error) {
      console.log(error);
      message.error('商品详情加载失败');
    } finally {
      setDetailLoading(false);
    }
  };

  const columns: ColumnsType<BuyerProductRow> = [
    {
      title: '商品',
      dataIndex: 'productName',
      key: 'productName',
      width: 360,
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
            <Typography.Text ellipsis style={{ maxWidth: 250 }}>
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
      title: '类目',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 180,
      render: displayText,
    },
    {
      title: '销售价',
      key: 'price',
      width: 160,
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
      render: displayText,
    },
    {
      title: '状态',
      dataIndex: 'spuStatus',
      key: 'spuStatus',
      width: 96,
      render: statusTag,
    },
    {
      title: '操作',
      key: 'action',
      width: 88,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => openDetail(record)}>
          详情
        </Button>
      ),
    },
  ];

  const skuColumns: ColumnsType<BuyerSkuRow> = [
    {
      title: 'SKU规格',
      key: 'spec',
      width: 240,
      render: (_, record) => buildSkuSpecText(record, skuRows) || '-',
    },
    {
      title: '尺寸重量',
      key: 'dimension',
      width: 240,
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
      <Card
        title="商城商品"
        variant="borderless"
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={() => loadProducts(pageNum, pageSize)}
          >
            刷新
          </Button>
        }
      >
        <Table<BuyerProductRow>
          size="small"
          rowKey="uiRowKey"
          loading={loading}
          columns={columns}
          dataSource={rows}
          scroll={{ x: 980 }}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            pageSizeOptions: [5, 10, 20],
            onChange: loadProducts,
          }}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
        />
      </Card>

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
            <Descriptions.Item label="商品状态">
              {statusTag(current?.spuStatus)}
            </Descriptions.Item>
            <Descriptions.Item label="类目">
              {displayText(current?.categoryName)}
            </Descriptions.Item>
            <Descriptions.Item label="商品名称">
              {displayText(current?.productName)}
            </Descriptions.Item>
            <Descriptions.Item label="英文名称">
              {displayText(current?.productNameEn)}
            </Descriptions.Item>
            <Descriptions.Item label="价格">
              {formatPriceRange(current?.salePriceMin, current?.salePriceMax)}
            </Descriptions.Item>
            <Descriptions.Item label="币种">
              {displayText(current?.currencySummary)}
            </Descriptions.Item>
            <Descriptions.Item label="卖点" span={2}>
              {displayText(current?.sellingPoint)}
            </Descriptions.Item>
          </Descriptions>
          <Table<BuyerSkuRow>
            size="small"
            rowKey="uiRowKey"
            loading={detailLoading}
            columns={skuColumns}
            dataSource={skuRows}
            pagination={false}
            scroll={{ x: 760 }}
            locale={{
              emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />,
            }}
          />
        </Space>
      </Modal>
    </>
  );
};

export default BuyerDistributionProductList;
