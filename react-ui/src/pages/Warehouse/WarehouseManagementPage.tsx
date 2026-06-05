import { PlusOutlined, SyncOutlined } from '@ant-design/icons';
import {
  type ActionType,
  PageContainer,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Modal, Space, Tag, Typography } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getDictSelectOption } from '@/services/system/dict';
import {
  addOfficialWarehouse,
  addThirdPartyWarehouse,
  getOfficialWarehouseList,
  getThirdPartyWarehouseList,
  getWarehouseCurrencyOptions,
  getWarehouseSellerOptions,
  syncOfficialWarehouse,
  updateOfficialWarehouse,
  updateOfficialWarehouseStatus,
  updateThirdPartyWarehouse,
  updateThirdPartyWarehouseStatus,
} from '@/services/warehouse/warehouse';
import { message } from '@/utils/feedback';
import {
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  statusColor,
  statusText,
  WAREHOUSE_STATUS_OPTIONS,
  WAREHOUSE_STATUS_VALUE_ENUM,
} from './constants';
import OfficialSyncModal from './components/OfficialSyncModal';
import WarehouseFormModal from './components/WarehouseFormModal';

type WarehouseKind = 'official' | 'third_party';

interface WarehouseManagementPageProps {
  kind: WarehouseKind;
}

type WarehouseServiceSet = {
  list: typeof getOfficialWarehouseList;
  add: typeof addOfficialWarehouse;
  update: typeof updateOfficialWarehouse;
  updateStatus: typeof updateOfficialWarehouseStatus;
};

const serviceMap: Record<WarehouseKind, WarehouseServiceSet> = {
  official: {
    list: getOfficialWarehouseList,
    add: addOfficialWarehouse,
    update: updateOfficialWarehouse,
    updateStatus: updateOfficialWarehouseStatus,
  },
  third_party: {
    list: getThirdPartyWarehouseList,
    add: addThirdPartyWarehouse,
    update: updateThirdPartyWarehouse,
    updateStatus: updateThirdPartyWarehouseStatus,
  },
};

const permissionMap = {
  official: {
    add: 'warehouse:official:add',
    edit: 'warehouse:official:edit',
    status: 'warehouse:official:status',
    sync: 'warehouse:official:sync',
  },
  third_party: {
    add: 'warehouse:thirdParty:add',
    edit: 'warehouse:thirdParty:edit',
    status: 'warehouse:thirdParty:status',
  },
};

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function displayText(value?: string | number) {
  return value === undefined || value === null || value === '' ? '-' : value;
}

function cleanParams(params: Record<string, any>) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );
}

export default function WarehouseManagementPage({ kind }: WarehouseManagementPageProps) {
  const access = useAccess();
  const actionRef = useRef<ActionType>(null);
  const [countryOptions, setCountryOptions] = useState<any[]>([]);
  const [currencyOptions, setCurrencyOptions] = useState<any[]>([]);
  const [sellerOptions, setSellerOptions] = useState<any[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [syncOpen, setSyncOpen] = useState(false);
  const [currentWarehouse, setCurrentWarehouse] = useState<API.Warehouse.Warehouse>();

  const isOfficial = kind === 'official';
  const services = serviceMap[kind];
  const permissions = permissionMap[kind];
  const searchFieldCount = isOfficial ? 7 : 8;

  useEffect(() => {
    getDictSelectOption('country_region').then(setCountryOptions);
    getWarehouseCurrencyOptions().then((resp) => {
      if (resp.code === 200) {
        setCurrencyOptions(resp.data || []);
      }
    });
    if (!isOfficial) {
      getWarehouseSellerOptions().then((resp) => {
        if (resp.code === 200) {
          setSellerOptions(resp.data || []);
        }
      });
    }
  }, [isOfficial]);

  const openCreate = () => {
    setCurrentWarehouse(undefined);
    setModalOpen(true);
  };

  const openEdit = (record: API.Warehouse.Warehouse) => {
    setCurrentWarehouse(record);
    setModalOpen(true);
  };

  const saveWarehouse = async (values: API.Warehouse.Warehouse) => {
    const resp = values.warehouseId
      ? await services.update(values)
      : await services.add(values);
    if (resultOk(resp, values.warehouseId ? '仓库已更新' : '仓库已新增')) {
      setModalOpen(false);
      actionRef.current?.reload();
      return true;
    }
    return false;
  };

  const toggleStatus = (record: API.Warehouse.Warehouse) => {
    const nextStatus = record.status === '0' ? '1' : '0';
    Modal.confirm({
      title: `${nextStatus === '0' ? '启用' : '停用'}仓库`,
      content: `确认${nextStatus === '0' ? '启用' : '停用'} ${record.warehouseCode}？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await services.updateStatus({
            warehouseId: record.warehouseId!,
            status: nextStatus,
          }),
          nextStatus === '0' ? '仓库已启用' : '仓库已停用',
        );
        if (ok) {
          actionRef.current?.reload();
        }
      },
    });
  };

  const submitSync = async (values: API.Warehouse.OfficialSyncRequest) => {
    const ok = resultOk(await syncOfficialWarehouse(values), '官方仓库已同步并配对');
    if (ok) {
      setSyncOpen(false);
      actionRef.current?.reload();
      return true;
    }
    return false;
  };

  const countryValueEnum = useMemo(
    () =>
      Object.fromEntries(
        countryOptions.map((item) => [item.value, { text: item.label }]),
      ),
    [countryOptions],
  );
  const currencyValueEnum = useMemo(
    () =>
      Object.fromEntries(
        currencyOptions.map((item) => [item.value, { text: item.label }]),
      ),
    [currencyOptions],
  );

  const columns: ProColumns<API.Warehouse.Warehouse>[] = [
    {
      title: '仓库编码',
      dataIndex: 'warehouseCode',
      width: 150,
      copyable: true,
      ellipsis: true,
    },
    {
      title: '仓库名称',
      dataIndex: 'warehouseName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '国家/地区',
      dataIndex: 'countryCode',
      width: 130,
      valueType: 'select',
      valueEnum: countryValueEnum,
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: countryOptions,
      },
    },
    {
      title: '州/省',
      dataIndex: 'stateProvince',
      width: 140,
      ellipsis: true,
    },
    {
      title: '城市',
      dataIndex: 'city',
      width: 130,
      ellipsis: true,
    },
    {
      title: '地址',
      dataIndex: 'addressLine1',
      width: 260,
      search: false,
      ellipsis: true,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text ellipsis={{ tooltip: record.addressLine1 }}>
            {displayText(record.addressLine1)}
          </Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.addressLine2 }}>
            {displayText(record.addressLine2)}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '结算币种',
      dataIndex: 'settlementCurrency',
      width: 120,
      valueType: 'select',
      valueEnum: currencyValueEnum,
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: currencyOptions,
      },
    },
    {
      title: '联系人',
      dataIndex: 'contactName',
      width: 130,
      search: false,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      valueType: 'select',
      valueEnum: WAREHOUSE_STATUS_VALUE_ENUM,
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: WAREHOUSE_STATUS_OPTIONS,
      },
      render: (_, record) => <Tag color={statusColor(record.status)}>{statusText(record.status)}</Tag>,
    },
  ];

  if (isOfficial) {
    columns.push({
      title: '上游配对',
      dataIndex: 'upstreamWarehouseName',
      width: 220,
      search: false,
      render: (_, record) =>
        record.warehousePairingId ? (
          <Space direction="vertical" size={0}>
            <Typography.Text>
              {displayText(record.masterWarehouseName || record.connectionCode)}
            </Typography.Text>
            <Typography.Text type="secondary">
              {displayText(record.upstreamWarehouseName)} / {displayText(record.upstreamWarehouseCode)}
            </Typography.Text>
          </Space>
        ) : (
          <Tag>未配对</Tag>
        ),
    });
  } else {
    columns.splice(2, 0, {
      title: '归属卖家',
      dataIndex: 'sellerKeyword',
      width: 220,
      ellipsis: true,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{displayText(record.sellerCode)}</Typography.Text>
          <Typography.Text type="secondary">{displayText(record.sellerShortName || record.sellerName)}</Typography.Text>
        </Space>
      ),
    });
  }

  columns.push({
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 170,
    search: false,
    renderText: (_, record) => record.updateTime || record.createTime || '-',
  });

  columns.push({
    title: '操作',
    valueType: 'option',
    width: 130,
    fixed: 'right',
    render: (_, record) => [
      access.hasPerms(permissions.edit) ? (
        <Button key="edit" type="link" size="small" onClick={() => openEdit(record)}>
          编辑
        </Button>
      ) : null,
      access.hasPerms(permissions.status) ? (
        <Button key="status" type="link" size="small" onClick={() => toggleStatus(record)}>
          {record.status === '0' ? '停用' : '启用'}
        </Button>
      ) : null,
    ],
  });

  return (
    <PageContainer title={false}>
      <ProTable<API.Warehouse.Warehouse>
        actionRef={actionRef}
        rowKey="warehouseId"
        columns={columns}
        options={false}
        search={getPersistedProTableSearch(
          { labelWidth: 96, fieldCount: searchFieldCount },
          `warehouse-${kind}`,
        )}
        pagination={getProTablePagination(20)}
        scroll={getProTableScroll(isOfficial ? 1540 : 1640)}
        request={async (params) => {
          const resp = await services.list(cleanParams(params));
          return {
            data: resp.rows || [],
            success: resp.code === 200,
            total: resp.total,
          };
        }}
        toolBarRender={() => [
          isOfficial && access.hasPerms(permissionMap.official.sync) ? (
            <Button key="sync" icon={<SyncOutlined />} onClick={() => setSyncOpen(true)}>
              同步仓库
            </Button>
          ) : null,
          access.hasPerms(permissions.add) ? (
            <Button key="add" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              {isOfficial ? '新增官方仓库' : '新增第三方仓库'}
            </Button>
          ) : null,
        ]}
      />
      <WarehouseFormModal
        open={modalOpen}
        title={currentWarehouse ? '编辑仓库' : isOfficial ? '新增官方仓库' : '新增第三方仓库'}
        current={currentWarehouse}
        countryOptions={countryOptions}
        currencyOptions={currencyOptions}
        sellerOptions={sellerOptions}
        showSeller={!isOfficial}
        onOpenChange={setModalOpen}
        onSubmit={saveWarehouse}
      />
      {isOfficial ? (
        <OfficialSyncModal
          open={syncOpen}
          countryOptions={countryOptions}
          currencyOptions={currencyOptions}
          onOpenChange={setSyncOpen}
          onSubmit={submitSync}
        />
      ) : null}
    </PageContainer>
  );
}
