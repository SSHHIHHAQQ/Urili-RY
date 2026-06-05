import { ModalForm, type ProFormInstance } from '@ant-design/pro-components';
import { useEffect, useRef } from 'react';
import WarehouseFields from './WarehouseFields';

interface WarehouseFormModalProps {
  open: boolean;
  title: string;
  current?: API.Warehouse.Warehouse;
  countryOptions: any[];
  currencyOptions: any[];
  sellerOptions?: any[];
  showSeller?: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (values: API.Warehouse.Warehouse) => Promise<boolean>;
}

export default function WarehouseFormModal({
  open,
  title,
  current,
  countryOptions,
  currencyOptions,
  sellerOptions,
  showSeller,
  onOpenChange,
  onSubmit,
}: WarehouseFormModalProps) {
  const formRef = useRef<ProFormInstance<API.Warehouse.Warehouse> | undefined>(undefined);

  useEffect(() => {
    if (!open) {
      return;
    }
    formRef.current?.resetFields();
    formRef.current?.setFieldsValue(current || { countryCode: 'US', status: '0' });
  }, [current, open]);

  return (
    <ModalForm<API.Warehouse.Warehouse>
      formRef={formRef}
      title={title}
      open={open}
      width={760}
      modalProps={{ destroyOnClose: true }}
      onOpenChange={onOpenChange}
      onFinish={async (values) =>
        onSubmit({
          ...current,
          ...values,
          status: current?.status || '0',
        })
      }
      grid
      colProps={{ span: 12 }}
    >
      <WarehouseFields
        countryOptions={countryOptions}
        currencyOptions={currencyOptions}
        sellerOptions={sellerOptions}
        showSeller={showSeller}
        codeDisabled={Boolean(current?.warehouseId)}
      />
    </ModalForm>
  );
}
