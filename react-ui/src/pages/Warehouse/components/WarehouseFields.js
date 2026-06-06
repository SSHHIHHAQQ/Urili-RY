import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ProForm, ProFormField, ProFormSelect, ProFormText, ProFormTextArea, } from '@ant-design/pro-components';
import { AutoComplete, Form, Input, Typography } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getUsCityOptions, getUsStateOptions } from '@/services/warehouse/warehouse';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
function toStateOptions(items) {
    return items.map((item) => ({
        label: item.stateName,
        value: item.stateName,
        searchText: `${item.stateName} ${item.stateCode}`,
    }));
}
function toCityOptions(items) {
    const seenCityNames = new Set();
    return items.reduce((options, item) => {
        const cityName = item.cityName || '';
        const cityKey = cityName.toLowerCase();
        if (!cityName || seenCityNames.has(cityKey)) {
            return options;
        }
        seenCityNames.add(cityKey);
        options.push({
            label: cityName,
            value: cityName,
            searchText: `${cityName} ${item.placeName || ''}`,
        });
        return options;
    }, []);
}
function filterLocationOption(inputValue, option) {
    const keyword = inputValue.trim().toLowerCase();
    if (!keyword) {
        return true;
    }
    return [option?.label, option?.value, option?.searchText]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(keyword);
}
function sectionTitle(title) {
    return (_jsx(Typography.Text, { strong: true, style: { borderLeft: '3px solid #1677ff', paddingLeft: 8 }, children: title }));
}
function StateProvinceControl({ countryCode, options, onStateChange, onStateSelect, value, onChange, onBlur, disabled, id, className, style, }) {
    const [open, setOpen] = useState(false);
    const controlStyle = { width: '100%', ...style };
    if (countryCode !== 'US') {
        return (_jsx(Input, { id: id, value: value, onChange: onChange, onBlur: onBlur, disabled: disabled, className: className, maxLength: 100, placeholder: "\u8BF7\u8F93\u5165", style: controlStyle }));
    }
    return (_jsx(AutoComplete, { id: id, value: value, options: options, open: open && options.length > 0, filterOption: filterLocationOption, disabled: disabled, className: className, placeholder: "\u8BF7\u8F93\u5165", onBlur: onBlur, onClick: () => setOpen(true), onChange: (nextValue) => {
            setOpen(true);
            onChange?.(nextValue);
            onStateChange();
        }, onFocus: () => setOpen(true), onOpenChange: setOpen, onSelect: (nextValue) => onStateSelect(nextValue), style: controlStyle }));
}
function CityControl({ countryCode, options, stateProvince, onLoadCities, value, onChange, onBlur, disabled, id, className, style, }) {
    const [open, setOpen] = useState(false);
    const controlStyle = { width: '100%', ...style };
    if (countryCode !== 'US') {
        return (_jsx(Input, { id: id, value: value, onChange: onChange, onBlur: onBlur, disabled: disabled, className: className, maxLength: 100, placeholder: "\u8BF7\u8F93\u5165", style: controlStyle }));
    }
    return (_jsx(AutoComplete, { id: id, value: value, options: options, open: open && options.length > 0, filterOption: filterLocationOption, disabled: disabled, className: className, placeholder: "\u8BF7\u8F93\u5165", onBlur: onBlur, onClick: () => {
            setOpen(true);
            onLoadCities(stateProvince, value);
        }, onChange: (nextValue) => {
            setOpen(true);
            onChange?.(nextValue);
        }, onFocus: () => {
            setOpen(true);
            onLoadCities(stateProvince, value);
        }, onOpenChange: setOpen, onSearch: (keyword) => {
            setOpen(true);
            onLoadCities(stateProvince, keyword);
        }, style: controlStyle }));
}
export default function WarehouseFields({ countryOptions, currencyOptions, sellerOptions = [], showSeller, codeDisabled, }) {
    const [stateOptions, setStateOptions] = useState([]);
    const [cityOptions, setCityOptions] = useState([]);
    const cityRequestRef = useRef(0);
    const form = Form.useFormInstance();
    const countryCode = Form.useWatch('countryCode');
    const stateProvince = Form.useWatch('stateProvince');
    useEffect(() => {
        getUsStateOptions().then((resp) => {
            if (resp.code === 200) {
                setStateOptions(toStateOptions(resp.data || []));
            }
        });
    }, []);
    const searchableCountryProps = useMemo(() => ({ ...SEARCHABLE_SELECT_PROPS, options: countryOptions }), [countryOptions]);
    const searchableCurrencyProps = useMemo(() => ({ ...SEARCHABLE_SELECT_PROPS, options: currencyOptions }), [currencyOptions]);
    const searchableSellerProps = useMemo(() => ({ ...SEARCHABLE_SELECT_PROPS, options: sellerOptions }), [sellerOptions]);
    const loadCities = useCallback(async (stateName, keyword) => {
        const requestId = cityRequestRef.current + 1;
        cityRequestRef.current = requestId;
        if (!stateName?.trim()) {
            setCityOptions([]);
            return;
        }
        const resp = await getUsCityOptions({ stateName, keyword });
        if (requestId === cityRequestRef.current && resp.code === 200) {
            setCityOptions(toCityOptions(resp.data || []));
        }
    }, []);
    const clearCityForStateChange = useCallback(() => {
        form.setFieldValue('city', undefined);
        setCityOptions([]);
    }, [form]);
    const loadCitiesForStateSelect = useCallback((stateName) => {
        form.setFieldValue('city', undefined);
        loadCities(stateName);
    }, [form, loadCities]);
    return (_jsxs(_Fragment, { children: [_jsxs(ProForm.Group, { title: sectionTitle('基础信息'), colProps: { span: 24 }, children: [_jsx(ProFormText, { name: "warehouseCode", label: "\u4ED3\u5E93\u7F16\u7801", disabled: codeDisabled, rules: [{ required: true, message: '请输入仓库编码' }], fieldProps: { maxLength: 64 } }), _jsx(ProFormText, { name: "warehouseName", label: "\u4ED3\u5E93\u540D\u79F0", rules: [{ required: true, message: '请输入仓库名称' }], fieldProps: { maxLength: 200 } }), _jsx(ProFormSelect, { name: "settlementCurrency", label: "\u7ED3\u7B97\u5E01\u79CD", rules: [{ required: true, message: '请选择结算币种' }], fieldProps: searchableCurrencyProps }), showSeller ? (_jsx(ProFormSelect, { name: "sellerId", label: "\u5F52\u5C5E\u5356\u5BB6", rules: [{ required: true, message: '请选择归属卖家' }], fieldProps: searchableSellerProps })) : null] }), _jsxs(ProForm.Group, { title: sectionTitle('地址信息'), colProps: { span: 24 }, children: [_jsx(ProFormText, { name: "contactName", label: "\u8054\u7CFB\u4EBA", rules: [{ required: true, message: '请输入联系人' }], fieldProps: { maxLength: 100 } }), _jsx(ProFormText, { name: "contactPhone", label: "\u8054\u7CFB\u7535\u8BDD", fieldProps: { maxLength: 64 } }), _jsx(ProFormText, { name: "contactEmail", label: "\u8054\u7CFB\u90AE\u7BB1", rules: [
                            { required: true, message: '请输入联系邮箱' },
                            { type: 'email', message: '邮箱格式不正确' },
                        ], fieldProps: { maxLength: 128 } }), _jsx(ProFormText, { name: "companyName", label: "\u516C\u53F8\u540D\u79F0", fieldProps: { maxLength: 200 } }), _jsx(ProFormSelect, { name: "countryCode", label: "\u56FD\u5BB6/\u5730\u533A", rules: [{ required: true, message: '请选择国家/地区' }], fieldProps: searchableCountryProps }), _jsx(ProFormField, { name: "stateProvince", label: "\u5DDE/\u7701", valueType: "text", rules: [{ required: true, message: '请输入州/省' }], colProps: { span: 12 }, children: _jsx(StateProvinceControl, { countryCode: countryCode, options: stateOptions, onStateChange: clearCityForStateChange, onStateSelect: loadCitiesForStateSelect }) }), _jsx(ProFormField, { name: "city", label: "\u57CE\u5E02", valueType: "text", rules: [{ required: true, message: '请输入城市' }], colProps: { span: 12 }, children: _jsx(CityControl, { countryCode: countryCode, options: cityOptions, stateProvince: stateProvince, onLoadCities: loadCities }) }), _jsx(ProFormText, { name: "postalCode", label: "\u90AE\u7F16", rules: [{ required: true, message: '请输入邮编' }], fieldProps: { maxLength: 32 } }), _jsx(ProFormText, { name: "addressLine1", label: "\u5730\u57401", rules: [{ required: true, message: '请输入地址1' }], fieldProps: { maxLength: 255 } }), _jsx(ProFormText, { name: "addressLine2", label: "\u5730\u57402", fieldProps: { maxLength: 255 } }), _jsx(ProFormTextArea, { name: "remark", label: "\u5907\u6CE8", fieldProps: { maxLength: 500, rows: 3 } })] })] }));
}
