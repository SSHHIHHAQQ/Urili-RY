import { EyeOutlined } from '@ant-design/icons';
import {
  type ActionType,
  PageContainer,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Space, Tag, Typography } from 'antd';
import { useRef, useState } from 'react';
import {
  pairingStatusText,
  skuPairingStatusSearchOptions,
  skuSyncItemStatusSearchOptions,
  syncItemStatusText,
  systemKindOptions,
  systemKindText,
} from '@/services/integration/constants';
import { getSourceProductList } from '@/services/integration/sourceProduct';
import {
  getProTableColumnsState,
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  approveStatusOptions,
  approveStatusText,
  dimensionText,
  displayText,
  weightText,
  wmsDimensionText,
  wmsWeightText,
} from './constants';
import SourceProductDetailDrawer from './SourceProductDetailDrawer';
import styles from './style.module.css';

function cleanParams(params: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

function statusTag(value?: string) {
  const text = syncItemStatusText[value || ''] || value || '-';
  const color = value === 'ACTIVE' ? 'green' : value === 'MISSING' ? 'orange' : 'default';
  return <Tag color={color}>{text}</Tag>;
}

function pairingTag(value?: string) {
  const text = pairingStatusText[value || ''] || value || '未配对';
  return <Tag color={value === 'PAIRED' ? 'blue' : 'default'}>{text}</Tag>;
}

function approveTag(value?: string) {
  if (!value) {
    return '-';
  }
  const color = value === '2' ? 'green' : value === '3' || value === '4' ? 'red' : 'gold';
  return <Tag color={color}>{approveStatusText[value] || value}</Tag>;
}

function sourceLabel(record: API.Integration.SourceProductItem) {
  return record.systemKindLabel || systemKindText[record.systemKind || ''] || record.systemKind || '-';
}

function renderProduct(record: API.Integration.SourceProductItem) {
  return (
    <div className={styles.productCell}>
      <Space className={styles.productTextStack} orientation="vertical" size={0}>
        <Typography.Text
          className={styles.productText}
          strong
          ellipsis={{ tooltip: record.masterProductName }}
        >
          {record.masterProductName}
        </Typography.Text>
        <Typography.Text
          className={`${styles.productText} ${styles.subText}`}
          ellipsis={{ tooltip: record.productAliasName || record.productDescription }}
        >
          {record.productAliasName || record.productDescription || '-'}
        </Typography.Text>
      </Space>
    </div>
  );
}

export default function SourceProductLibraryPage() {
  const actionRef = useRef<ActionType>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<API.Integration.SourceProductItem>();

  const openDetail = (record: API.Integration.SourceProductItem) => {
    setCurrentRecord(record);
    setDetailOpen(true);
  };

  const columns: ProColumns<API.Integration.SourceProductItem>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '来源系统',
      dataIndex: 'systemKind',
      hideInTable: true,
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: systemKindOptions,
      },
    },
    {
      title: '仓库名称',
      dataIndex: 'masterWarehouseName',
      hideInTable: true,
    },
    {
      title: '来源仓库',
      key: 'sourceWarehouse',
      dataIndex: 'sourceWarehouse',
      width: 170,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Tag color="blue">{sourceLabel(record)}</Tag>
          <Typography.Text>{displayText(record.masterWarehouseName)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '来源 SKU',
      dataIndex: 'masterSku',
      width: 180,
      copyable: true,
      ellipsis: true,
    },
    {
      title: '商品名称',
      dataIndex: 'productName',
      hideInTable: true,
    },
    {
      title: '来源商品',
      key: 'masterProductName',
      dataIndex: 'masterProductName',
      width: 280,
      search: false,
      render: (_, record) => renderProduct(record),
    },
    {
      title: '识别码',
      dataIndex: 'identifyCodeKeyword',
      hideInTable: true,
    },
    {
      title: '条码 / FNSKU',
      key: 'mainCode',
      dataIndex: 'mainCode',
      width: 230,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text copyable={!!record.mainCode}>{displayText(record.mainCode)}</Typography.Text>
          <Typography.Text className={styles.subText}>FNSKU {displayText(record.fnsku)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '审核状态',
      dataIndex: 'approveStatus',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: approveStatusOptions,
      },
      width: 110,
      render: (_, record) => approveTag(record.approveStatus),
    },
    {
      title: '客户尺寸',
      key: 'customerDimension',
      dataIndex: 'length',
      width: 190,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>{dimensionText(record)}</Typography.Text>
          <Typography.Text className={styles.subText}>{weightText(record)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '仓库尺寸',
      key: 'warehouseDimension',
      dataIndex: 'wmsLength',
      width: 190,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>{wmsDimensionText(record)}</Typography.Text>
          <Typography.Text className={styles.subText}>{wmsWeightText(record)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '同步状态',
      dataIndex: 'status',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: skuSyncItemStatusSearchOptions,
      },
      width: 110,
      render: (_, record) => statusTag(record.status),
    },
    {
      title: '配对状态',
      dataIndex: 'pairingStatus',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: skuPairingStatusSearchOptions,
      },
      width: 110,
      render: (_, record) => pairingTag(record.pairingStatus),
    },
    {
      title: '匹配客户',
      dataIndex: 'customerName',
      width: 160,
      search: false,
      ellipsis: true,
      renderText: (value) => displayText(value),
    },
    {
      title: '商城商品',
      key: 'mallProduct',
      dataIndex: 'systemSku',
      width: 220,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text copyable={!!record.systemSku}>{displayText(record.systemSku)}</Typography.Text>
          <Typography.Text className={styles.subText} ellipsis={{ tooltip: record.systemSkuName }}>
            {displayText(record.systemSkuName)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '同步时间',
      key: 'syncTime',
      dataIndex: 'lastSeenTime',
      width: 190,
      search: false,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>{displayText(record.lastSeenTime)}</Typography.Text>
          <Typography.Text className={styles.subText}>更新 {displayText(record.updateTime)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'option',
      valueType: 'option',
      width: 90,
      fixed: 'right',
      render: (_, record) => [
        <Button
          key="detail"
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => openDetail(record)}
        >
          查看
        </Button>,
      ],
    },
  ];

  return (
    <PageContainer title={false}>
      <ProTable<API.Integration.SourceProductItem>
        actionRef={actionRef}
        className="urili-fill-table"
        rowKey={(record) => `${record.connectionCode}:${record.masterSku}`}
        columns={columns}
        columnsState={getProTableColumnsState('source-product-library-columns')}
        search={getPersistedProTableSearch({ labelWidth: 96 }, 'source-product-library')}
        request={async (params) => {
          const { current, pageSize, ...filters } = params;
          const resp = await getSourceProductList(
            cleanParams({
              pageNum: current,
              pageSize,
              ...filters,
            }),
          );
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
        pagination={getProTablePagination(20)}
        options={{ density: true, reload: true, setting: true }}
        scroll={getProTableScroll(2050)}
        toolBarRender={() => []}
      />

      <SourceProductDetailDrawer
        open={detailOpen}
        record={currentRecord}
        onClose={() => setDetailOpen(false)}
      />
    </PageContainer>
  );
}
