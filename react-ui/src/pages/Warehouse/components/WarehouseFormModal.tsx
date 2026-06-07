import { ModalForm } from '@ant-design/pro-components';
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
  const initialValues = current || { countryCode: 'US', status: '0' };

  return (
    <ModalForm<API.Warehouse.Warehouse>
      key={current?.warehouseId || 'warehouse-create'}
      title={title}
      open={open}
      width={760}
      initialValues={initialValues}
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
