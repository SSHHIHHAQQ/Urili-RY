import {
  ModalForm,
  ProFormDependency,
  type ProFormInstance,
  ProFormSelect,
} from '@ant-design/pro-components';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  getOfficialSyncCandidates,
  getOfficialSyncConnections,
} from '@/services/warehouse/warehouse';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import WarehouseFields from './WarehouseFields';

interface OfficialSyncModalProps {
  open: boolean;
  countryOptions: any[];
  currencyOptions: any[];
  onOpenChange: (open: boolean) => void;
  onSubmit: (values: API.Warehouse.OfficialSyncRequest) => Promise<boolean>;
}

function connectionLabel(item: API.Warehouse.SyncConnection) {
  return `${item.connectionCode} - ${item.masterWarehouseName}`;
}

function candidateLabel(item: API.Warehouse.SyncCandidate) {
  const base = `${item.warehouseCode} - ${item.warehouseName}`;
  return item.paired ? `${base}（已配对：${item.systemWarehouseCode || '-'}）` : base;
}

export default function OfficialSyncModal({
  open,
  countryOptions,
  currencyOptions,
  onOpenChange,
  onSubmit,
}: OfficialSyncModalProps) {
  const formRef = useRef<ProFormInstance<API.Warehouse.OfficialSyncRequest> | undefined>(undefined);
  const [connections, setConnections] = useState<API.Warehouse.SyncConnection[]>([]);
  const [candidates, setCandidates] = useState<API.Warehouse.SyncCandidate[]>([]);

  useEffect(() => {
    if (!open) {
      return;
    }
    formRef.current?.resetFields();
    formRef.current?.setFieldsValue({ countryCode: 'US', status: '0' });
    getOfficialSyncConnections().then((resp) => {
      if (resp.code === 200) {
        setConnections(resp.data || []);
      }
    });
    setCandidates([]);
  }, [open]);

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
    () =>
      candidates.map((item) => ({
        label: candidateLabel(item),
        value: item.warehouseCode,
        disabled: item.paired,
        searchText: candidateLabel(item),
      })),
    [candidates],
  );

  const loadCandidates = async (connectionCode?: string, keyword?: string) => {
    if (!connectionCode) {
      setCandidates([]);
      return;
    }
    const resp = await getOfficialSyncCandidates({ connectionCode, keyword });
    if (resp.code === 200) {
      setCandidates(resp.data || []);
    }
  };

  const selectCandidate = (warehouseCode?: string) => {
    const candidate = candidates.find((item) => item.warehouseCode === warehouseCode);
    if (!candidate) {
      return;
    }
    formRef.current?.setFieldsValue({
      upstreamWarehouseCode: candidate.warehouseCode,
      warehouseCode: candidate.warehouseCode,
      warehouseName: candidate.warehouseName,
      countryCode: candidate.countryCode || 'US',
    });
  };

  return (
    <ModalForm<API.Warehouse.OfficialSyncRequest>
      formRef={formRef}
      title="同步官方仓库"
      open={open}
      width={820}
      modalProps={{ destroyOnClose: true }}
      onOpenChange={onOpenChange}
      onFinish={onSubmit}
      grid
      colProps={{ span: 12 }}
    >
      <ProFormSelect
        name="connectionCode"
        label="主仓接入"
        rules={[{ required: true, message: '请选择主仓接入' }]}
        fieldProps={{
          ...SEARCHABLE_SELECT_PROPS,
          options: connectionOptions,
          onChange: (value) => {
            formRef.current?.setFieldsValue({ upstreamWarehouseCode: undefined });
            loadCandidates(value as string);
          },
        }}
      />
      <ProFormDependency name={['connectionCode']}>
        {({ connectionCode }) => (
          <ProFormSelect
            name="upstreamWarehouseCode"
            label="上游仓库"
            rules={[{ required: true, message: '请选择上游仓库' }]}
            fieldProps={{
              ...SEARCHABLE_SELECT_PROPS,
              options: candidateOptions,
              onSearch: (keyword) => loadCandidates(connectionCode, keyword),
              onChange: (value) => selectCandidate(value as string),
            }}
          />
        )}
      </ProFormDependency>
      <WarehouseFields
        countryOptions={countryOptions}
        currencyOptions={currencyOptions}
      />
    </ModalForm>
  );
}
