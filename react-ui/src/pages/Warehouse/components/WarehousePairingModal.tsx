import {
  ModalForm,
  ProFormDependency,
  type ProFormInstance,
  ProFormSelect,
} from '@ant-design/pro-components';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  getOfficialPairingCandidates,
  getOfficialPairingConnections,
  getOfficialSyncCandidates,
  getOfficialSyncConnections,
} from '@/services/warehouse/warehouse';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

interface WarehousePairingModalProps {
  open: boolean;
  current?: API.Warehouse.Warehouse;
  onOpenChange: (open: boolean) => void;
  onSubmit: (values: API.Warehouse.OfficialPairingRequest) => Promise<boolean>;
}

const PAIRING_ROLE_OPTIONS = [
  {
    label: '履约仓',
    value: 'FULFILLMENT',
    searchText: '履约 上游仓 应付 FULFILLMENT',
  },
  {
    label: '报价仓',
    value: 'QUOTE',
    searchText: '报价 自营仓 应收 QUOTE',
  },
];
const NO_PAIRING_VALUE = '__NO_PAIRING__';

function defaultPairingRole(current?: API.Warehouse.Warehouse): API.Warehouse.PairingRole {
  if (!current?.warehousePairingId) {
    return 'FULFILLMENT';
  }
  if (!current?.quoteWarehousePairingId) {
    return 'QUOTE';
  }
  return 'FULFILLMENT';
}

function connectionLabel(item: API.Warehouse.SyncConnection) {
  return item.masterWarehouseName ? `${item.connectionCode} - ${item.masterWarehouseName}` : item.connectionCode;
}

function candidateLabel(item: API.Warehouse.SyncCandidate) {
  const base = `${item.warehouseCode} - ${item.warehouseName}`;
  return item.paired ? `${base}（已配对：${item.systemWarehouseCode || '-'}）` : base;
}

function currentPairing(
  current: API.Warehouse.Warehouse | undefined,
  pairingRole: API.Warehouse.PairingRole,
) {
  if (!current) {
    return undefined;
  }
  if (pairingRole === 'QUOTE') {
    return current.quoteWarehousePairingId
      ? {
          connectionCode: current.quoteConnectionCode,
          upstreamWarehouseCode: current.quoteUpstreamWarehouseCode,
        }
      : undefined;
  }
  return current.warehousePairingId
    ? {
        connectionCode: current.connectionCode,
        upstreamWarehouseCode: current.upstreamWarehouseCode,
      }
    : undefined;
}

function normalizeSettlementType(value?: string) {
  if (!value) {
    return undefined;
  }
  if (value === 'UPSTREAM_PAYABLE' || value.toLowerCase() === 'upstream-payable') {
    return 'upstream-payable';
  }
  if (value === 'PLATFORM_ADVANCE' || value.toLowerCase() === 'self-operated-receivable') {
    return 'self-operated-receivable';
  }
  return value;
}

function filterConnections(
  items: API.Warehouse.SyncConnection[],
  pairingRole: API.Warehouse.PairingRole,
) {
  const expectedSettlementType =
    pairingRole === 'QUOTE' ? 'self-operated-receivable' : 'upstream-payable';
  return items.filter((item) => {
    const settlementType = normalizeSettlementType(item.settlementType);
    return !settlementType || settlementType === expectedSettlementType;
  });
}

async function requestConnections(pairingRole: API.Warehouse.PairingRole, keyword?: string) {
  if (pairingRole === 'FULFILLMENT') {
    return getOfficialSyncConnections(keyword);
  }
  return getOfficialPairingConnections({ pairingRole, keyword });
}

async function requestCandidates(
  pairingRole: API.Warehouse.PairingRole,
  connectionCode: string,
  keyword?: string,
) {
  if (pairingRole === 'FULFILLMENT') {
    return getOfficialSyncCandidates({ connectionCode, keyword });
  }
  return getOfficialPairingCandidates({ pairingRole, connectionCode, keyword });
}

export default function WarehousePairingModal({
  open,
  current,
  onOpenChange,
  onSubmit,
}: WarehousePairingModalProps) {
  const formRef = useRef<ProFormInstance<API.Warehouse.OfficialPairingRequest> | undefined>(undefined);
  const [connections, setConnections] = useState<API.Warehouse.SyncConnection[]>([]);
  const [candidates, setCandidates] = useState<API.Warehouse.SyncCandidate[]>([]);
  const [activePairingRole, setActivePairingRole] = useState<API.Warehouse.PairingRole>('FULFILLMENT');

  const initialValues = useMemo(
    () => ({ pairingRole: defaultPairingRole(current) }),
    [current],
  );

  useEffect(() => {
    if (!open) {
      return;
    }
    formRef.current?.resetFields();
    setCandidates([]);
    const pairingRole = defaultPairingRole(current);
    const existingPairing = currentPairing(current, pairingRole);
    setActivePairingRole(pairingRole);
    formRef.current?.setFieldsValue({
      pairingRole,
      connectionCode: existingPairing?.connectionCode,
      upstreamWarehouseCode: existingPairing?.upstreamWarehouseCode,
    });
    requestConnections(pairingRole).then((resp) => {
      if (resp.code === 200) {
        setConnections(filterConnections(resp.data || [], pairingRole));
      }
    });
    if (existingPairing?.connectionCode) {
      loadCandidates(pairingRole, existingPairing.connectionCode);
    }
  }, [current, open]);

  const connectionOptions = useMemo(
    () =>
      connections.map((item) => ({
        label: connectionLabel(item),
        value: item.connectionCode,
        searchText: connectionLabel(item),
      })),
    [connections],
  );

  const candidateOptions = useMemo(
    () => {
      const options = candidates.map((item) => ({
        label: candidateLabel(item),
        value: item.warehouseCode,
        disabled: item.paired,
        searchText: candidateLabel(item),
      }));
      if (currentPairing(current, activePairingRole)) {
        return [
          {
            label: '无配对',
            value: NO_PAIRING_VALUE,
            searchText: '无配对 取消 解除',
          },
          ...options,
        ];
      }
      return options;
    },
    [activePairingRole, candidates, current],
  );

  const loadConnections = async (pairingRole?: API.Warehouse.PairingRole, keyword?: string) => {
    if (!pairingRole) {
      setConnections([]);
      return;
    }
    const resp = await requestConnections(pairingRole, keyword);
    if (resp.code === 200) {
      setConnections(filterConnections(resp.data || [], pairingRole));
    }
  };

  const loadCandidates = async (
    pairingRole?: API.Warehouse.PairingRole,
    connectionCode?: string,
    keyword?: string,
  ) => {
    if (!pairingRole || !connectionCode) {
      setCandidates([]);
      return;
    }
    const resp = await requestCandidates(pairingRole, connectionCode, keyword);
    if (resp.code === 200) {
      setCandidates(resp.data || []);
    }
  };

  const handleFinish = async (values: API.Warehouse.OfficialPairingRequest) => {
    if (values.upstreamWarehouseCode === NO_PAIRING_VALUE) {
      return onSubmit({
        pairingRole: values.pairingRole,
        unpair: true,
      });
    }
    return onSubmit(values);
  };

  return (
    <ModalForm<API.Warehouse.OfficialPairingRequest>
      key={current?.warehouseId || 'warehouse-pairing'}
      formRef={formRef}
      title="配对仓库"
      open={open}
      width={620}
      initialValues={initialValues}
      modalProps={{ destroyOnClose: true }}
      onOpenChange={onOpenChange}
      onFinish={handleFinish}
      grid
      colProps={{ span: 24 }}
    >
      <ProFormSelect
        name="pairingRole"
        label="配对仓库类型"
        rules={[{ required: true, message: '请选择配对仓库类型' }]}
        fieldProps={{
          ...SEARCHABLE_SELECT_PROPS,
          options: PAIRING_ROLE_OPTIONS,
          onChange: (value) => {
            const pairingRole = value as API.Warehouse.PairingRole;
            const existingPairing = currentPairing(current, pairingRole);
            setActivePairingRole(pairingRole);
            formRef.current?.setFieldsValue({
              connectionCode: existingPairing?.connectionCode,
              upstreamWarehouseCode: existingPairing?.upstreamWarehouseCode,
            });
            setCandidates([]);
            loadConnections(pairingRole);
            if (existingPairing?.connectionCode) {
              loadCandidates(pairingRole, existingPairing.connectionCode);
            }
          },
        }}
      />
      <ProFormDependency name={['pairingRole']}>
        {({ pairingRole }) => (
          <ProFormSelect
            name="connectionCode"
            label="主仓接入"
            rules={[{ required: true, message: '请选择主仓接入' }]}
            fieldProps={{
              ...SEARCHABLE_SELECT_PROPS,
              options: connectionOptions,
              onSearch: (keyword) => loadConnections(pairingRole, keyword),
              onChange: (value) => {
                formRef.current?.setFieldsValue({ upstreamWarehouseCode: undefined });
                loadCandidates(pairingRole, value as string);
              },
            }}
          />
        )}
      </ProFormDependency>
      <ProFormDependency name={['pairingRole', 'connectionCode']}>
        {({ pairingRole, connectionCode }) => (
          <ProFormSelect
            name="upstreamWarehouseCode"
            label="主仓仓库"
            rules={[{ required: true, message: '请选择主仓仓库' }]}
            fieldProps={{
              ...SEARCHABLE_SELECT_PROPS,
              options: candidateOptions,
              onSearch: (keyword) => loadCandidates(pairingRole, connectionCode, keyword),
            }}
          />
        )}
      </ProFormDependency>
    </ModalForm>
  );
}
