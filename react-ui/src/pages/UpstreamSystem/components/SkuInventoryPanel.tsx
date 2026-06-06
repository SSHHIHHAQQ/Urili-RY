import { SearchOutlined, SyncOutlined } from '@ant-design/icons';
import {
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { Button, Input, Select, Space, Typography } from 'antd';
import { type MutableRefObject, useEffect, useState } from 'react';
import {
  getInventorySyncState,
  getUpstreamInventoryList,
  syncUpstreamInventory,
} from '@/services/integration/upstreamSystem';
import { message } from '@/utils/feedback';
import {
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  inventoryScopeOptions,
  skuPairingStatusOptions,
  skuSyncItemStatusOptions,
} from '../constants';
import { statusTag } from '../helpers';
import styles from '../style.module.css';

type SkuInventoryPanelProps = {
  access: { hasPerms: (permission: string) => boolean };
  actionRef: MutableRefObject<ActionType | null>;
  selectedCode: string;
};

const displayText = (value?: string | number | null) => {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
};

const displayQuantity = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '-';
  }
  return new Intl.NumberFormat('zh-CN').format(Number(value));
};

export default function SkuInventoryPanel({
  access,
  actionRef,
  selectedCode,
}: SkuInventoryPanelProps) {
  const [syncing, setSyncing] = useState(false);
  const [syncState, setSyncState] = useState<API.Integration.InventorySyncState>();
  const [searchInput, setSearchInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [warehouseInput, setWarehouseInput] = useState('');
  const [warehouseKeyword, setWarehouseKeyword] = useState('');
  const [syncStatus, setSyncStatus] = useState('');
  const [warehousePairingStatus, setWarehousePairingStatus] = useState('');
  const [skuPairingStatus, setSkuPairingStatus] = useState('');
  const [inventoryScope, setInventoryScope] = useState('');

  const loadSyncState = async () => {
    if (!selectedCode) {
      setSyncState(undefined);
      return;
    }
    const resp = await getInventorySyncState(selectedCode);
    if (resp.code === 200) {
      setSyncState(resp.data);
    }
  };

  useEffect(() => {
    loadSyncState();
  }, [selectedCode]);

  const triggerInventorySync = async () => {
    setSyncing(true);
    const hide = message.loading('正在同步SKU库存');
    const resp = await syncUpstreamInventory(selectedCode);
    hide();
    setSyncing(false);
    if (resp.code === 200) {
      message.success(`SKU库存同步完成：${resp.data?.warehouseStockCount || 0}`);
      await loadSyncState();
      actionRef.current?.reload();
    } else {
      message.error(resp.msg);
      await loadSyncState();
    }
  };

  const columns: ProColumns<API.Integration.SourceWarehouseStockItem>[] = [
    {
      title: '领星仓库',
      key: 'sourceWarehouse',
      dataIndex: 'upstreamWarehouseCode',
      width: 210,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text copyable={!!record.upstreamWarehouseCode}>
            {displayText(record.upstreamWarehouseCode)}
          </Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.upstreamWarehouseName }}>
            {displayText(record.upstreamWarehouseName)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '领星SKU',
      key: 'sourceSku',
      dataIndex: 'masterSku',
      width: 230,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text copyable={!!record.masterSku}>{displayText(record.masterSku)}</Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.masterProductName }}>
            {displayText(record.masterProductName)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '库存数量',
      key: 'quantity',
      dataIndex: 'totalQuantity',
      width: 220,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>总库存 {displayQuantity(record.totalQuantity)}</Typography.Text>
          <Typography.Text type="secondary">
            可用 {displayQuantity(record.availableQuantity)} / 锁定 {displayQuantity(record.lockedQuantity)}
          </Typography.Text>
          <Typography.Text type="secondary">
            在途 {displayQuantity(record.inTransitQuantity)} / 箱内 {displayQuantity(record.boxedQuantity)}
          </Typography.Text>
        </Space>
      ),
    },
    { title: '库存口径', dataIndex: 'inventoryScope', width: 120, renderText: (value) => displayText(value) },
    { title: '库存属性', dataIndex: 'inventoryAttribute', width: 120, renderText: (value) => displayText(value) },
    {
      title: '批次 / 库位',
      key: 'batchLocation',
      dataIndex: 'batchNo',
      width: 170,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>批次 {displayText(record.batchNo)}</Typography.Text>
          <Typography.Text type="secondary">库位 {displayText(record.locationCode)}</Typography.Text>
        </Space>
      ),
    },
    { title: '仓库配对', dataIndex: 'warehousePairingStatus', width: 110, render: (_, record) => statusTag(record.warehousePairingStatus) },
    { title: 'SKU配对', dataIndex: 'skuPairingStatus', width: 110, render: (_, record) => statusTag(record.skuPairingStatus) },
    {
      title: '系统仓库',
      key: 'systemWarehouse',
      dataIndex: 'systemWarehouseCode',
      width: 180,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text>{displayText(record.systemWarehouseCode)}</Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.systemWarehouseName }}>
            {displayText(record.systemWarehouseName)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '系统SKU',
      key: 'systemSku',
      dataIndex: 'systemSku',
      width: 210,
      render: (_, record) => (
        <Space orientation="vertical" size={0}>
          <Typography.Text copyable={!!record.systemSku}>{displayText(record.systemSku)}</Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.systemSkuName }}>
            {displayText(record.systemSkuName)}
          </Typography.Text>
          <Typography.Text type="secondary">{displayText(record.customerName)}</Typography.Text>
        </Space>
      ),
    },
    { title: '同步状态', dataIndex: 'status', width: 100, render: (_, record) => statusTag(record.status) },
    { title: '最近同步', dataIndex: 'lastSeenTime', width: 170 },
  ];

  return (
    <div className={styles.skuPanel}>
      <div className={styles.skuToolbar}>
        <div className={styles.skuState}>
          <Typography.Text type="secondary">同步状态</Typography.Text>
          {statusTag(syncState?.status || 'NEVER')}
          <Typography.Text>上次成功 {syncState?.lastSuccessTime || '-'}</Typography.Text>
          <Typography.Text type="secondary">下次同步 {syncState?.nextSyncTime || '-'}</Typography.Text>
          <Typography.Text type="secondary">库存条数 {syncState?.activeCount ?? 0}</Typography.Text>
          {syncState?.lastErrorMessage ? (
            <Typography.Text type="danger">错误：{syncState.lastErrorMessage}</Typography.Text>
          ) : null}
        </div>
        <Button
          type="primary"
          icon={<SyncOutlined />}
          loading={syncing}
          hidden={!access.hasPerms('integration:upstream:inventorySync')}
          onClick={triggerInventorySync}
        >
          同步库存
        </Button>
      </div>
      <div className={styles.skuFilters}>
        <Input.Search
          allowClear
          enterButton={<SearchOutlined />}
          placeholder="搜索SKU/商品"
          style={{ width: 240 }}
          value={searchInput}
          onChange={(event) => {
            setSearchInput(event.target.value);
            if (!event.target.value) setKeyword('');
          }}
          onSearch={(value) => setKeyword(value.trim())}
        />
        <Input.Search
          allowClear
          enterButton={<SearchOutlined />}
          placeholder="搜索仓库"
          style={{ width: 220 }}
          value={warehouseInput}
          onChange={(event) => {
            setWarehouseInput(event.target.value);
            if (!event.target.value) setWarehouseKeyword('');
          }}
          onSearch={(value) => setWarehouseKeyword(value.trim())}
        />
        <Select {...SEARCHABLE_SELECT_PROPS} style={{ width: 150 }} options={inventoryScopeOptions} value={inventoryScope} onChange={setInventoryScope} />
        <Select {...SEARCHABLE_SELECT_PROPS} style={{ width: 150 }} options={skuPairingStatusOptions} value={warehousePairingStatus} onChange={setWarehousePairingStatus} />
        <Select {...SEARCHABLE_SELECT_PROPS} style={{ width: 150 }} options={skuPairingStatusOptions} value={skuPairingStatus} onChange={setSkuPairingStatus} />
        <Select {...SEARCHABLE_SELECT_PROPS} style={{ width: 150 }} options={skuSyncItemStatusOptions} value={syncStatus} onChange={setSyncStatus} />
      </div>
      <ProTable<API.Integration.SourceWarehouseStockItem>
        actionRef={actionRef}
        className={`${styles.fillTable} upstream-fill-table`}
        rowKey={(record) =>
          record.inventorySnapshotId ||
          [
            selectedCode,
            record.upstreamWarehouseCode,
            record.masterSku,
            record.inventoryScope,
            record.inventoryAttribute,
            record.batchNo,
            record.locationCode,
          ].join(':')
        }
        columns={columns}
        params={{ selectedCode, keyword, warehouseKeyword, inventoryScope, warehousePairingStatus, skuPairingStatus, syncStatus }}
        request={async (params) => {
          const resp = await getUpstreamInventoryList(selectedCode, {
            pageNum: params.current,
            pageSize: params.pageSize,
            keyword,
            warehouseKeyword,
            inventoryScope: inventoryScope || undefined,
            warehousePairingStatus: warehousePairingStatus || undefined,
            skuPairingStatus: skuPairingStatus || undefined,
            status: syncStatus || undefined,
          });
          return { data: resp.rows || [], total: resp.total || 0, success: resp.code === 200 };
        }}
        pagination={getProTablePagination(10)}
        search={false}
        options={false}
        scroll={getProTableScroll(1850)}
        toolBarRender={false}
      />
    </div>
  );
}
