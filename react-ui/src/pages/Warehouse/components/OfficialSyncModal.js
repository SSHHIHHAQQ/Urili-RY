import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ModalForm, ProFormDependency, ProFormSelect, } from '@ant-design/pro-components';
import { useEffect, useMemo, useRef, useState } from 'react';
import { getOfficialSyncCandidates, getOfficialSyncConnections, } from '@/services/warehouse/warehouse';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import WarehouseFields from './WarehouseFields';
function connectionLabel(item) {
    return `${item.connectionCode} - ${item.masterWarehouseName}`;
}
function candidateLabel(item) {
    const base = `${item.warehouseCode} - ${item.warehouseName}`;
    return item.paired ? `${base}（已配对：${item.systemWarehouseCode || '-'}）` : base;
}
export default function OfficialSyncModal({ open, countryOptions, currencyOptions, onOpenChange, onSubmit, }) {
    const formRef = useRef(undefined);
    const [connections, setConnections] = useState([]);
    const [candidates, setCandidates] = useState([]);
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
    const connectionOptions = useMemo(() => connections.map((item) => ({
        label: connectionLabel(item),
        value: item.connectionCode,
        searchText: connectionLabel(item),
    })), [connections]);
    const candidateOptions = useMemo(() => candidates.map((item) => ({
        label: candidateLabel(item),
        value: item.warehouseCode,
        disabled: item.paired,
        searchText: candidateLabel(item),
    })), [candidates]);
    const loadCandidates = async (connectionCode, keyword) => {
        if (!connectionCode) {
            setCandidates([]);
            return;
        }
        const resp = await getOfficialSyncCandidates({ connectionCode, keyword });
        if (resp.code === 200) {
            setCandidates(resp.data || []);
        }
    };
    const selectCandidate = (warehouseCode) => {
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
    return (_jsxs(ModalForm, { formRef: formRef, title: "\u540C\u6B65\u5B98\u65B9\u4ED3\u5E93", open: open, width: 820, modalProps: { destroyOnClose: true }, onOpenChange: onOpenChange, onFinish: onSubmit, grid: true, colProps: { span: 12 }, children: [_jsx(ProFormSelect, { name: "connectionCode", label: "\u4E3B\u4ED3\u63A5\u5165", rules: [{ required: true, message: '请选择主仓接入' }], fieldProps: {
                    ...SEARCHABLE_SELECT_PROPS,
                    options: connectionOptions,
                    onChange: (value) => {
                        formRef.current?.setFieldsValue({ upstreamWarehouseCode: undefined });
                        loadCandidates(value);
                    },
                } }), _jsx(ProFormDependency, { name: ['connectionCode'], children: ({ connectionCode }) => (_jsx(ProFormSelect, { name: "upstreamWarehouseCode", label: "\u4E0A\u6E38\u4ED3\u5E93", rules: [{ required: true, message: '请选择上游仓库' }], fieldProps: {
                        ...SEARCHABLE_SELECT_PROPS,
                        options: candidateOptions,
                        onSearch: (keyword) => loadCandidates(connectionCode, keyword),
                        onChange: (value) => selectCandidate(value),
                    } })) }), _jsx(WarehouseFields, { countryOptions: countryOptions, currencyOptions: currencyOptions })] }));
}
