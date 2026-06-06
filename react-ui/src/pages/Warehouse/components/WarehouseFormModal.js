import { jsx as _jsx } from "react/jsx-runtime";
import { ModalForm } from '@ant-design/pro-components';
import { useEffect, useRef } from 'react';
import WarehouseFields from './WarehouseFields';
export default function WarehouseFormModal({ open, title, current, countryOptions, currencyOptions, sellerOptions, showSeller, onOpenChange, onSubmit, }) {
    const formRef = useRef(undefined);
    useEffect(() => {
        if (!open) {
            return;
        }
        formRef.current?.resetFields();
        formRef.current?.setFieldsValue(current || { countryCode: 'US', status: '0' });
    }, [current, open]);
    return (_jsx(ModalForm, { formRef: formRef, title: title, open: open, width: 760, modalProps: { destroyOnClose: true }, onOpenChange: onOpenChange, onFinish: async (values) => onSubmit({
            ...current,
            ...values,
            status: current?.status || '0',
        }), grid: true, colProps: { span: 12 }, children: _jsx(WarehouseFields, { countryOptions: countryOptions, currencyOptions: currencyOptions, sellerOptions: sellerOptions, showSeller: showSeller, codeDisabled: Boolean(current?.warehouseId) }) }));
}
