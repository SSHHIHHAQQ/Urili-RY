import { DownOutlined, PlusOutlined } from '@ant-design/icons';
import { PageContainer, type ActionType, type ProColumns, ProTable } from '@ant-design/pro-components';
import { history, useAccess } from '@umijs/max';
import { Button, Dropdown, Image, Modal, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getAdminSellerList } from '@/services/seller/seller';
import { getCategoryList } from '@/services/product/product';
import {
  getDistributionProduct,
  getDistributionProductList,
  updateDistributionProductStatus,
  updateDistributionSkuStatus,
} from '@/services/product/distributionProduct';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import { buildCategoryTree, toCategoryTreeSelectData } from '../categoryTree';
import {
  buildSkuSpecText,
  formatPriceRange,
  getSalesStatusText,
  resolveResourceUrl,
  salesStatusOptions,
  salesStatusValueEnum,
  sourceTypeValueEnum,
} from './constants';
import ProductDetailDrawer from './components/ProductDetailDrawer';
import styles from './style.module.css';

export default function ProductDistributionPage() {
  const access = useAccess();
  const actionRef = useRef<ActionType>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [current, setCurrent] = useState<API.ProductDistribution.Spu>();
  const [sellerOptions, setSellerOptions] = useState<{ label: string; value: number }[]>([]);
  const [categories, setCategories] = useState<API.Product.Category[]>([]);

  const categoryTreeData = useMemo(
    () => toCategoryTreeSelectData(buildCategoryTree(categories)),
    [categories],
  );

  useEffect(() => {
    getAdminSellerList({ pageNum: 1, pageSize: 100, status: '0' }).then((resp) => {
      setSellerOptions(
        (resp.rows || []).map((seller) => ({
          label: `${seller.sellerName || seller.sellerShortName || seller.sellerNo}（${seller.sellerNo || '-'}）`,
          value: seller.sellerId as number,
        })),
      );
    });
    getCategoryList({ status: '0' }).then((resp) => setCategories(resp.data || []));
  }, []);

  const reload = () => actionRef.current?.reload();

  const openDetail = async (record: API.ProductDistribution.Spu) => {
    if (record.spuId == null) {
      return;
    }
    const resp = await getDistributionProduct(record.spuId);
    setCurrent(resp.data);
    setDetailOpen(true);
  };

  const openEdit = async (record: API.ProductDistribution.Spu) => {
    history.push(`/product/distribution/edit/${record.spuId}`);
  };

  const changeSpuStatus = (record: API.ProductDistribution.Spu, status: string) => {
    if (record.spuId == null) {
      return;
    }
    const spuId = record.spuId;
    Modal.confirm({
      title: '切换商品状态',
      content: `确认将 ${record.productName} 切换为 ${getSalesStatusText(status)}？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const resp = await updateDistributionProductStatus(spuId, status);
        if (resp.code === 200) {
          message.success('状态已更新');
          reload();
        } else {
          message.error(resp.msg || '状态更新失败');
        }
      },
    });
  };

  const changeSkuStatus = (spuId: number, record: API.ProductDistribution.Sku, status: string) => {
    if (record.skuId == null) {
      return;
    }
    const skuId = record.skuId;
    Modal.confirm({
      title: '切换SKU状态',
      content: `确认将 ${record.systemSkuCode || record.sellerSkuCode} 切换为 ${getSalesStatusText(status)}？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const resp = await updateDistributionSkuStatus(spuId, skuId, status);
        if (resp.code === 200) {
          message.success('SKU状态已更新');
          reload();
        } else {
          message.error(resp.msg || 'SKU状态更新失败');
        }
      },
    });
  };

  const skuColumns = (spuId: number): ColumnsType<API.ProductDistribution.Sku> => [
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      width: 72,
      render: (url) =>
        url ? <Image width={40} height={40} src={resolveResourceUrl(url)} style={{ objectFit: 'cover' }} /> : '--',
    },
    { title: '系统SKU', dataIndex: 'systemSkuCode', width: 150 },
    { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 140 },
    {
      title: 'SKU规格',
      width: 220,
      render: (_, record) => buildSkuSpecText(record) || '--',
    },
    { title: '供货价', dataIndex: 'supplyPrice', width: 90 },
    { title: '销售价', dataIndex: 'salePrice', width: 90 },
    { title: '币种', dataIndex: 'currencyCode', width: 80 },
    { title: '可售库存', width: 90, render: () => '--' },
    { title: '仓库数', width: 80, render: () => '--' },
    {
      title: '状态',
      dataIndex: 'skuStatus',
      width: 90,
      render: (value) => <Tag>{getSalesStatusText(String(value))}</Tag>,
    },
    {
      title: '操作',
      width: 110,
      render: (_, record) => (
        <Dropdown
          menu={{
            items: salesStatusOptions.map((item) => ({ key: item.value, label: item.label })),
            onClick: ({ key }) => changeSkuStatus(spuId, record, key),
          }}
        >
          <Button type="link" size="small">
            状态 <DownOutlined />
          </Button>
        </Dropdown>
      ),
    },
  ];

  const columns: ProColumns<API.ProductDistribution.Spu>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '商品图',
      dataIndex: 'mainImageUrl',
      search: false,
      width: 72,
      render: (_, record) =>
        record.mainImageUrl ? (
          <Image width={44} height={44} src={resolveResourceUrl(record.mainImageUrl)} style={{ objectFit: 'cover' }} />
        ) : '--',
    },
    { title: '系统SPU', dataIndex: 'systemSpuCode', width: 160 },
    { title: '客户SPU', dataIndex: 'sellerSpuCode', width: 150 },
    {
      title: '商品标题',
      dataIndex: 'productName',
      width: 260,
      render: (_, record) => (
        <div>
          <div>{record.productName || '--'}</div>
          <div className={styles.mutedText}>{record.productNameEn || '--'}</div>
        </div>
      ),
    },
    {
      title: '卖家',
      dataIndex: 'sellerId',
      valueType: 'select',
      fieldProps: { ...SEARCHABLE_SELECT_PROPS, options: sellerOptions },
      render: (_, record) => record.sellerName || '--',
      width: 180,
    },
    {
      title: '类目',
      dataIndex: 'categoryId',
      valueType: 'treeSelect',
      fieldProps: { ...SEARCHABLE_TREE_SELECT_PROPS, treeData: categoryTreeData, treeDefaultExpandAll: true },
      render: (_, record) => record.categoryName || '--',
      width: 160,
    },
    { title: 'SKU数', dataIndex: 'skuCount', search: false, width: 80 },
    {
      title: '供货价区间',
      search: false,
      width: 130,
      render: (_, record) => formatPriceRange(record.supplyPriceMin, record.supplyPriceMax),
    },
    {
      title: '销售价区间',
      search: false,
      width: 130,
      render: (_, record) => formatPriceRange(record.salePriceMin, record.salePriceMax),
    },
    { title: '币种', dataIndex: 'currencySummary', search: false, width: 90 },
    { title: '总可售库存', search: false, width: 100, render: () => '--' },
    { title: '仓库数', search: false, width: 80, render: () => '--' },
    {
      title: '状态',
      dataIndex: 'spuStatus',
      valueType: 'select',
      valueEnum: salesStatusValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 100,
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      valueEnum: sourceTypeValueEnum,
      search: false,
      width: 140,
    },
    { title: '更新时间', dataIndex: 'updateTime', search: false, width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 180,
      fixed: 'right',
      render: (_, record) => [
        <Button key="view" type="link" size="small" onClick={() => openDetail(record)}>
          查看
        </Button>,
        <Button
          key="edit"
          type="link"
          size="small"
          hidden={!access.hasPerms('product:distribution:edit')}
          onClick={() => openEdit(record)}
        >
          编辑
        </Button>,
        <Dropdown
          key="more"
          menu={{
            items: salesStatusOptions.map((item) => ({ key: item.value, label: item.label })),
            onClick: ({ key }) => changeSpuStatus(record, key),
          }}
        >
          <Button type="link" size="small" hidden={!access.hasPerms('product:distribution:status')}>
            更多 <DownOutlined />
          </Button>
        </Dropdown>,
      ],
    },
  ];

  return (
    <PageContainer title={false}>
      <ProTable<API.ProductDistribution.Spu>
        actionRef={actionRef}
        rowKey="spuId"
        columns={columns}
        scroll={getProTableScroll(1780)}
        search={getPersistedProTableSearch({ labelWidth: 90 }, 'product-distribution')}
        expandable={{
          expandedRowRender: (record) => (
            <Table
              rowKey="skuId"
              size="small"
              columns={skuColumns(record.spuId ?? 0)}
              dataSource={record.skus || []}
              pagination={false}
              scroll={{ x: 1200 }}
            />
          ),
        }}
        request={async ({ current, pageSize, ...params }) => {
          const resp = await getDistributionProductList({
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
        toolBarRender={() => [
          <Button
            key="add"
            type="primary"
            icon={<PlusOutlined />}
            hidden={!access.hasPerms('product:distribution:add')}
            onClick={() => history.push('/product/distribution/create')}
          >
            新增商品
          </Button>,
        ]}
      />

      <ProductDetailDrawer
        open={detailOpen}
        product={current}
        onClose={() => setDetailOpen(false)}
      />
    </PageContainer>
  );
}
