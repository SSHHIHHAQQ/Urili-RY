import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { PlusOutlined } from '@ant-design/icons';
import { Button, Checkbox, Input, InputNumber, Radio, Space, Table, Tag, Typography } from 'antd';
import { useEffect, useMemo, useRef, useState } from 'react';
import { skuSpecFields } from '../constants';
import ImageUploadField from './ImageUploadField';
import styles from '../style.module.css';
const compactMeasurementUnitOptions = [
    { label: '公制单位', value: 'METRIC' },
    { label: '英制单位', value: 'IMPERIAL' },
];
const measurementUnits = {
    METRIC: { dimension: 'cm', weight: 'kg' },
    IMPERIAL: { dimension: 'in', weight: 'lb' },
};
const dimensionFields = ['lengthValue', 'widthValue', 'heightValue'];
const measurementFields = [...dimensionFields, 'weight'];
const maxSelectedSpecCount = 2;
function normalizeSpecValue(value) {
    return (value || '').trim();
}
function unique(values) {
    return Array.from(new Set(values.map(normalizeSpecValue).filter(Boolean)));
}
function makeRowKey() {
    return `sku-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}
function cartesian(entries) {
    if (!entries.length) {
        return [{}];
    }
    return entries.reduce((acc, entry) => acc.flatMap((item) => entry.values.map((value) => ({ ...item, [entry.field]: value }))), [{}]);
}
function specSignature(row, fields) {
    return fields.map((field) => normalizeSpecValue(row[field])).join('||');
}
function inferSelectedSpecs(rows) {
    const selected = skuSpecFields
        .filter((field) => rows.some((row) => normalizeSpecValue(row[field.value])))
        .map((field) => field.value);
    return selected.length
        ? selected.slice(0, maxSelectedSpecCount)
        : ['color', 'size'];
}
function MeasurementInput({ value, unit, placeholder, style, onChange, }) {
    return (_jsx(InputNumber, { min: 0, controls: false, value: value, placeholder: placeholder, suffix: _jsx("span", { className: styles.measurementUnitSuffix, children: unit }), className: styles.measurementInput, style: style, onChange: onChange }));
}
function normalizeUnit(unit) {
    const value = unit?.trim().toLowerCase();
    if (!value)
        return undefined;
    if (['cm', '厘米'].includes(value))
        return 'cm';
    if (['in', 'inch', 'inches', '英寸'].includes(value))
        return 'in';
    if (['kg', 'kgs', 'kilogram', 'kilograms', '公斤', '千克'].includes(value))
        return 'kg';
    if (['g', 'gram', 'grams', '克'].includes(value))
        return 'g';
    if (['lb', 'lbs', 'pound', 'pounds', '磅'].includes(value))
        return 'lb';
    return value;
}
function parseMeasurementValue(value) {
    const raw = value?.trim();
    if (!raw)
        return undefined;
    const match = raw.match(/^(-?\d+(?:\.\d+)?)\s*(.*)?$/);
    if (!match)
        return undefined;
    const numberValue = Number(match[1]);
    if (!Number.isFinite(numberValue))
        return undefined;
    return {
        value: numberValue,
        unit: normalizeUnit(match[2]),
    };
}
function convertMeasurementValue(value, fromUnit, toUnit, kind) {
    if (!fromUnit || fromUnit === toUnit)
        return value;
    if (kind === 'dimension') {
        const cmValue = fromUnit === 'in' ? value * 2.54 : value;
        return toUnit === 'in' ? cmValue / 2.54 : cmValue;
    }
    let kgValue = value;
    if (fromUnit === 'g') {
        kgValue = value / 1000;
    }
    else if (fromUnit === 'lb') {
        kgValue = value / 2.2046226218;
    }
    return toUnit === 'lb' ? kgValue * 2.2046226218 : kgValue;
}
function stripTrailingZero(value) {
    return value.replace(/(\.\d*?[1-9])0+$/, '$1').replace(/\.0+$/, '');
}
function formatMeasurementNumber(value) {
    return stripTrailingZero(value.toFixed(4));
}
function toOptionalNumber(value) {
    if (value === undefined || value === null || value === '')
        return undefined;
    const numberValue = Number(value);
    return Number.isFinite(numberValue) ? numberValue : undefined;
}
function readMeasurementInputValue(value, unit, kind) {
    const parsed = parseMeasurementValue(value);
    if (!parsed)
        return undefined;
    return Number(formatMeasurementNumber(convertMeasurementValue(parsed.value, parsed.unit, unit, kind)));
}
function formatMeasurementInputValue(value, unit) {
    const numberValue = toOptionalNumber(value);
    return numberValue === undefined ? undefined : `${formatMeasurementNumber(numberValue)} ${unit}`;
}
function inferMeasurementUnitSystem(rows) {
    const values = rows.flatMap((row) => [
        row.lengthValue,
        row.widthValue,
        row.heightValue,
        row.weight,
    ]);
    return values.some((value) => {
        const unit = parseMeasurementValue(value)?.unit;
        return unit === 'in' || unit === 'lb';
    }) ? 'IMPERIAL' : 'METRIC';
}
function normalizeMeasurementField(value, unitSystem, kind) {
    const unit = kind === 'dimension' ? measurementUnits[unitSystem].dimension : measurementUnits[unitSystem].weight;
    const numberValue = readMeasurementInputValue(value, unit, kind);
    return numberValue === undefined ? undefined : formatMeasurementInputValue(numberValue, unit);
}
function normalizeSkuMeasurements(row, unitSystem) {
    return {
        ...row,
        lengthValue: normalizeMeasurementField(row.lengthValue, unitSystem, 'dimension'),
        widthValue: normalizeMeasurementField(row.widthValue, unitSystem, 'dimension'),
        heightValue: normalizeMeasurementField(row.heightValue, unitSystem, 'dimension'),
        weight: normalizeMeasurementField(row.weight, unitSystem, 'weight'),
    };
}
function hasMeasurementChange(current, next) {
    return current.some((row, index) => {
        const nextRow = next[index];
        if (!nextRow)
            return true;
        return measurementFields.some((field) => row[field] !== nextRow[field]);
    });
}
export default function SkuMatrixEditor({ value, focusSkuId, currencyCode, currencyLabel, onChange, }) {
    const [selectedSpecs, setSelectedSpecs] = useState(['color', 'size']);
    const [specValues, setSpecValues] = useState({});
    const [draftValues, setDraftValues] = useState({});
    const [bulkLengthValue, setBulkLengthValue] = useState();
    const [bulkWidthValue, setBulkWidthValue] = useState();
    const [bulkHeightValue, setBulkHeightValue] = useState();
    const [bulkWeight, setBulkWeight] = useState();
    const [unitSystem, setUnitSystem] = useState('METRIC');
    const [bulkSupplyPrice, setBulkSupplyPrice] = useState();
    const hydratedRowsKeyRef = useRef(undefined);
    const currentUnits = measurementUnits[unitSystem];
    useEffect(() => {
        const rowsKey = value.map((row) => row.skuId || row.rowKey).join('|');
        if (rowsKey === hydratedRowsKeyRef.current)
            return;
        hydratedRowsKeyRef.current = rowsKey;
        const nextUnitSystem = inferMeasurementUnitSystem(value);
        setUnitSystem(nextUnitSystem);
        const selected = inferSelectedSpecs(value);
        const values = {};
        selected.forEach((field) => {
            values[field] = unique(value.map((row) => row[field]));
        });
        setSelectedSpecs(selected);
        setSpecValues(values);
        const normalizedRows = value.map((row) => normalizeSkuMeasurements(row, nextUnitSystem));
        if (hasMeasurementChange(value, normalizedRows)) {
            onChange(normalizedRows);
        }
    }, [value]);
    const rows = useMemo(() => value.map((row) => ({ ...row, rowKey: row.rowKey || String(row.skuId || makeRowKey()) })), [value]);
    useEffect(() => {
        if (!focusSkuId)
            return undefined;
        const timer = window.setTimeout(() => {
            document.querySelector(`.${styles.focusSkuRow}`)?.scrollIntoView({ block: 'center' });
        }, 120);
        return () => window.clearTimeout(timer);
    }, [focusSkuId, rows.length]);
    const specCheckboxOptions = useMemo(() => skuSpecFields.map((field) => ({
        ...field,
        disabled: selectedSpecs.length >= maxSelectedSpecCount && !selectedSpecs.includes(field.value),
    })), [selectedSpecs]);
    const updateRow = (rowKey, patch) => {
        onChange(rows.map((row) => (row.rowKey === rowKey ? { ...row, ...patch } : row)));
    };
    const changeSelectedSpecs = (checked) => {
        setSelectedSpecs(checked.slice(0, maxSelectedSpecCount));
    };
    const changeUnitSystem = (nextUnitSystem) => {
        if (nextUnitSystem === unitSystem)
            return;
        setUnitSystem(nextUnitSystem);
        onChange(rows.map((row) => normalizeSkuMeasurements(row, nextUnitSystem)));
    };
    const updateMeasurementRow = (row, field, value, kind) => {
        const unit = kind === 'dimension' ? currentUnits.dimension : currentUnits.weight;
        updateRow(row.rowKey, { [field]: formatMeasurementInputValue(value, unit) });
    };
    const addSpecValue = (field) => {
        const draft = normalizeSpecValue(draftValues[field]);
        if (!draft)
            return;
        const current = specValues[field] || [];
        if (current.includes(draft)) {
            setDraftValues({ ...draftValues, [field]: '' });
            return;
        }
        setSpecValues({ ...specValues, [field]: [...current, draft] });
        setDraftValues({ ...draftValues, [field]: '' });
    };
    const removeSpecValue = (field, item) => {
        setSpecValues({
            ...specValues,
            [field]: (specValues[field] || []).filter((valueItem) => valueItem !== item),
        });
    };
    const generateSkus = () => {
        const entries = selectedSpecs
            .map((field) => ({ field, values: specValues[field] || [] }))
            .filter((entry) => entry.values.length > 0);
        const currentMap = new Map(rows.map((row) => [specSignature(row, selectedSpecs), row]));
        const nextRows = cartesian(entries).map((combo, index) => {
            const existed = currentMap.get(selectedSpecs.map((field) => combo[field] || '').join('||'));
            return {
                ...(existed || {}),
                ...combo,
                rowKey: existed?.rowKey || makeRowKey(),
                skuStatus: existed?.skuStatus || 'DRAFT',
                sortOrder: existed?.sortOrder ?? index,
            };
        });
        onChange(nextRows.length ? nextRows : [{ rowKey: makeRowKey(), skuStatus: 'DRAFT', sortOrder: 0 }]);
    };
    const addManualSku = () => {
        onChange([
            ...rows,
            {
                rowKey: makeRowKey(),
                skuStatus: 'DRAFT',
                sortOrder: rows.length,
            },
        ]);
    };
    const applyBulk = () => {
        onChange(rows.map((row) => ({
            ...row,
            lengthValue: bulkLengthValue === undefined ? row.lengthValue : formatMeasurementInputValue(bulkLengthValue, currentUnits.dimension),
            widthValue: bulkWidthValue === undefined ? row.widthValue : formatMeasurementInputValue(bulkWidthValue, currentUnits.dimension),
            heightValue: bulkHeightValue === undefined ? row.heightValue : formatMeasurementInputValue(bulkHeightValue, currentUnits.dimension),
            weight: bulkWeight === undefined ? row.weight : formatMeasurementInputValue(bulkWeight, currentUnits.weight),
            supplyPrice: bulkSupplyPrice ?? row.supplyPrice,
        })));
    };
    const columns = [
        ...selectedSpecs.map((field) => ({
            title: skuSpecFields.find((item) => item.value === field)?.label || String(field),
            dataIndex: field,
            width: 92,
            fixed: 'left',
            render: (_, row) => (_jsx(Input, { value: row[field], onChange: (event) => updateRow(row.rowKey, { [field]: event.target.value }) })),
        })),
        {
            title: '客户SKU',
            dataIndex: 'sellerSkuCode',
            width: 150,
            render: (_, row) => (_jsx(Input, { value: row.sellerSkuCode, onChange: (event) => updateRow(row.rowKey, { sellerSkuCode: event.target.value }) })),
        },
        {
            title: 'SKU图',
            dataIndex: 'skuImageUrl',
            width: 104,
            render: (_, row) => (_jsx(ImageUploadField, { size: "small", value: row.skuImageUrl, onChange: (skuImageUrl) => updateRow(row.rowKey, { skuImageUrl }) })),
        },
        {
            title: `长度 (${currentUnits.dimension})`,
            dataIndex: 'lengthValue',
            width: 136,
            render: (_, row) => (_jsx(MeasurementInput, { value: readMeasurementInputValue(row.lengthValue, currentUnits.dimension, 'dimension'), unit: currentUnits.dimension, placeholder: "\u5982 30", onChange: (nextValue) => updateMeasurementRow(row, 'lengthValue', nextValue, 'dimension') })),
        },
        {
            title: `宽度 (${currentUnits.dimension})`,
            dataIndex: 'widthValue',
            width: 136,
            render: (_, row) => (_jsx(MeasurementInput, { value: readMeasurementInputValue(row.widthValue, currentUnits.dimension, 'dimension'), unit: currentUnits.dimension, placeholder: "\u5982 20", onChange: (nextValue) => updateMeasurementRow(row, 'widthValue', nextValue, 'dimension') })),
        },
        {
            title: `高度 (${currentUnits.dimension})`,
            dataIndex: 'heightValue',
            width: 136,
            render: (_, row) => (_jsx(MeasurementInput, { value: readMeasurementInputValue(row.heightValue, currentUnits.dimension, 'dimension'), unit: currentUnits.dimension, placeholder: "\u5982 8", onChange: (nextValue) => updateMeasurementRow(row, 'heightValue', nextValue, 'dimension') })),
        },
        {
            title: `重量 (${currentUnits.weight})`,
            dataIndex: 'weight',
            width: 136,
            render: (_, row) => (_jsx(MeasurementInput, { value: readMeasurementInputValue(row.weight, currentUnits.weight, 'weight'), unit: currentUnits.weight, placeholder: "\u5982 0.35", onChange: (nextValue) => updateMeasurementRow(row, 'weight', nextValue, 'weight') })),
        },
        {
            title: '供货价',
            dataIndex: 'supplyPrice',
            width: 120,
            render: (_, row) => (_jsx(InputNumber, { min: 0, precision: 4, value: row.supplyPrice, style: { width: '100%' }, onChange: (supplyPrice) => updateRow(row.rowKey, { supplyPrice: supplyPrice ?? undefined }) })),
        },
        {
            title: '币种',
            dataIndex: 'currencyCode',
            width: 120,
            render: () => currencyLabel || currencyCode || '-',
        },
        {
            title: '排序',
            dataIndex: 'sortOrder',
            width: 90,
            render: (_, row) => (_jsx(InputNumber, { min: 0, value: row.sortOrder, style: { width: '100%' }, onChange: (sortOrder) => updateRow(row.rowKey, { sortOrder: sortOrder ?? undefined }) })),
        },
        {
            title: '操作',
            width: 72,
            fixed: 'right',
            render: (_, row) => (_jsx(Button, { danger: true, type: "link", size: "small", disabled: rows.length <= 1, onClick: () => onChange(rows.filter((item) => item.rowKey !== row.rowKey)), children: "\u5220\u9664" })),
        },
    ];
    return (_jsxs("div", { className: styles.skuEditor, children: [_jsx("div", { className: styles.sectionTitle, children: "SKU \u4FE1\u606F" }), _jsxs("div", { className: styles.specPanel, children: [_jsx("div", { className: styles.specPanelTitle, children: "\u89C4\u683C\u5C5E\u6027" }), _jsx(Checkbox.Group, { value: selectedSpecs, options: specCheckboxOptions, onChange: (checked) => changeSelectedSpecs(checked) }), _jsx("div", { className: styles.specValueGrid, children: selectedSpecs.map((field) => (_jsxs("div", { className: styles.specValueItem, children: [_jsx("div", { className: styles.specValueTitle, children: skuSpecFields.find((item) => item.value === field)?.label }), _jsxs(Space, { wrap: true, children: [(specValues[field] || []).map((item) => (_jsx(Tag, { closable: true, onClose: () => removeSpecValue(field, item), children: item }, item))), _jsx(Input, { className: styles.specInput, value: draftValues[field], placeholder: "\u8F93\u5165\u89C4\u683C\u503C", onChange: (event) => setDraftValues({ ...draftValues, [field]: event.target.value }), onPressEnter: () => addSpecValue(field) }), _jsx(Button, { size: "small", type: "primary", onClick: () => addSpecValue(field), children: "\u6DFB\u52A0" })] })] }, field))) }), _jsxs(Space, { children: [_jsx(Button, { type: "primary", onClick: generateSkus, children: "\u751F\u6210 SKU" }), _jsx(Button, { onClick: addManualSku, icon: _jsx(PlusOutlined, {}), children: "\u65B0\u589E SKU" })] })] }), _jsx("div", { className: styles.bulkPanel, children: _jsxs(Space, { size: 8, wrap: true, children: [_jsx(Typography.Text, { strong: true, className: styles.bulkPanelLabel, children: "\u5355\u4F4D\u5236" }), _jsx(Radio.Group, { optionType: "button", buttonStyle: "solid", options: compactMeasurementUnitOptions, value: unitSystem, onChange: (event) => changeUnitSystem(event.target.value) }), _jsx(Typography.Text, { strong: true, className: styles.bulkPanelLabel, children: "\u6279\u91CF\u586B\u5145" }), _jsx(MeasurementInput, { placeholder: "\u957F", style: { width: 124 }, value: bulkLengthValue, unit: currentUnits.dimension, onChange: (nextValue) => setBulkLengthValue(toOptionalNumber(nextValue)) }), _jsx(MeasurementInput, { placeholder: "\u5BBD", style: { width: 124 }, value: bulkWidthValue, unit: currentUnits.dimension, onChange: (nextValue) => setBulkWidthValue(toOptionalNumber(nextValue)) }), _jsx(MeasurementInput, { placeholder: "\u9AD8", style: { width: 124 }, value: bulkHeightValue, unit: currentUnits.dimension, onChange: (nextValue) => setBulkHeightValue(toOptionalNumber(nextValue)) }), _jsx(MeasurementInput, { placeholder: "\u91CD\u91CF", style: { width: 132 }, value: bulkWeight, unit: currentUnits.weight, onChange: (nextValue) => setBulkWeight(toOptionalNumber(nextValue)) }), _jsx(InputNumber, { min: 0, precision: 4, placeholder: "\u4F9B\u8D27\u4EF7", style: { width: 140 }, value: bulkSupplyPrice, onChange: (value) => setBulkSupplyPrice(value ?? undefined) }), _jsx(Button, { type: "primary", onClick: applyBulk, children: "\u5E94\u7528\u5230\u5168\u90E8 SKU" })] }) }), _jsx(Table, { rowKey: "rowKey", columns: columns, dataSource: rows, rowClassName: (row) => (focusSkuId && row.skuId === focusSkuId ? styles.focusSkuRow : ''), pagination: false, scroll: { x: 1400 }, size: "small" })] }));
}
