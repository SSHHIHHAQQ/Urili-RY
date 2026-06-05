import {
  ProForm,
  ProFormField,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
} from '@ant-design/pro-components';
import { AutoComplete, Form, Input, Typography } from 'antd';
import { type CSSProperties, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getUsCityOptions, getUsStateOptions } from '@/services/warehouse/warehouse';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

interface WarehouseFieldsProps {
  countryOptions: any[];
  currencyOptions: any[];
  sellerOptions?: any[];
  showSeller?: boolean;
  codeDisabled?: boolean;
}

function toStateOptions(items: API.Warehouse.UsState[]) {
  return items.map((item) => ({
    label: item.stateName,
    value: item.stateName,
    searchText: `${item.stateName} ${item.stateCode}`,
  }));
}

function toCityOptions(items: API.Warehouse.UsCity[]) {
  const seenCityNames = new Set<string>();
  return items.reduce<any[]>((options, item) => {
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

function filterLocationOption(inputValue: string, option?: any) {
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

function sectionTitle(title: string) {
  return (
    <Typography.Text
      strong
      style={{ borderLeft: '3px solid #1677ff', paddingLeft: 8 }}
    >
      {title}
    </Typography.Text>
  );
}

interface LocationControlProps {
  value?: string;
  onChange?: (value: any) => void;
  onBlur?: (...args: any[]) => void;
  disabled?: boolean;
  id?: string;
  className?: string;
  style?: CSSProperties;
}

interface StateProvinceControlProps extends LocationControlProps {
  countryCode?: string;
  options: any[];
  onStateChange: () => void;
  onStateSelect: (stateName: string) => void;
}

function StateProvinceControl({
  countryCode,
  options,
  onStateChange,
  onStateSelect,
  value,
  onChange,
  onBlur,
  disabled,
  id,
  className,
  style,
}: StateProvinceControlProps) {
  const [open, setOpen] = useState(false);
  const controlStyle = { width: '100%', ...style };

  if (countryCode !== 'US') {
    return (
      <Input
        id={id}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        disabled={disabled}
        className={className}
        maxLength={100}
        placeholder="请输入"
        style={controlStyle}
      />
    );
  }

  return (
    <AutoComplete
      id={id}
      value={value}
      options={options}
      open={open && options.length > 0}
      filterOption={filterLocationOption}
      disabled={disabled}
      className={className}
      placeholder="请输入"
      onBlur={onBlur}
      onClick={() => setOpen(true)}
      onChange={(nextValue) => {
        setOpen(true);
        onChange?.(nextValue);
        onStateChange();
      }}
      onFocus={() => setOpen(true)}
      onOpenChange={setOpen}
      onSelect={(nextValue) => onStateSelect(nextValue)}
      style={controlStyle}
    />
  );
}

interface CityControlProps extends LocationControlProps {
  countryCode?: string;
  options: any[];
  stateProvince?: string;
  onLoadCities: (stateName?: string, keyword?: string) => void;
}

function CityControl({
  countryCode,
  options,
  stateProvince,
  onLoadCities,
  value,
  onChange,
  onBlur,
  disabled,
  id,
  className,
  style,
}: CityControlProps) {
  const [open, setOpen] = useState(false);
  const controlStyle = { width: '100%', ...style };

  if (countryCode !== 'US') {
    return (
      <Input
        id={id}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        disabled={disabled}
        className={className}
        maxLength={100}
        placeholder="请输入"
        style={controlStyle}
      />
    );
  }

  return (
    <AutoComplete
      id={id}
      value={value}
      options={options}
      open={open && options.length > 0}
      filterOption={filterLocationOption}
      disabled={disabled}
      className={className}
      placeholder="请输入"
      onBlur={onBlur}
      onClick={() => {
        setOpen(true);
        onLoadCities(stateProvince, value);
      }}
      onChange={(nextValue) => {
        setOpen(true);
        onChange?.(nextValue);
      }}
      onFocus={() => {
        setOpen(true);
        onLoadCities(stateProvince, value);
      }}
      onOpenChange={setOpen}
      onSearch={(keyword: string) => {
        setOpen(true);
        onLoadCities(stateProvince, keyword);
      }}
      style={controlStyle}
    />
  );
}

export default function WarehouseFields({
  countryOptions,
  currencyOptions,
  sellerOptions = [],
  showSeller,
  codeDisabled,
}: WarehouseFieldsProps) {
  const [stateOptions, setStateOptions] = useState<any[]>([]);
  const [cityOptions, setCityOptions] = useState<any[]>([]);
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

  const searchableCountryProps = useMemo(
    () => ({ ...SEARCHABLE_SELECT_PROPS, options: countryOptions }),
    [countryOptions],
  );
  const searchableCurrencyProps = useMemo(
    () => ({ ...SEARCHABLE_SELECT_PROPS, options: currencyOptions }),
    [currencyOptions],
  );
  const searchableSellerProps = useMemo(
    () => ({ ...SEARCHABLE_SELECT_PROPS, options: sellerOptions }),
    [sellerOptions],
  );

  const loadCities = useCallback(async (stateName?: string, keyword?: string) => {
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

  const loadCitiesForStateSelect = useCallback(
    (stateName: string) => {
      form.setFieldValue('city', undefined);
      loadCities(stateName);
    },
    [form, loadCities],
  );

  return (
    <>
      <ProForm.Group title={sectionTitle('基础信息')} colProps={{ span: 24 }}>
        <ProFormText
          name="warehouseCode"
          label="仓库编码"
          disabled={codeDisabled}
          rules={[{ required: true, message: '请输入仓库编码' }]}
          fieldProps={{ maxLength: 64 }}
        />
        <ProFormText
          name="warehouseName"
          label="仓库名称"
          rules={[{ required: true, message: '请输入仓库名称' }]}
          fieldProps={{ maxLength: 200 }}
        />
        <ProFormSelect
          name="settlementCurrency"
          label="结算币种"
          rules={[{ required: true, message: '请选择结算币种' }]}
          fieldProps={searchableCurrencyProps}
        />
        {showSeller ? (
          <ProFormSelect
            name="sellerId"
            label="归属卖家"
            rules={[{ required: true, message: '请选择归属卖家' }]}
            fieldProps={searchableSellerProps}
          />
        ) : null}
      </ProForm.Group>

      <ProForm.Group title={sectionTitle('地址信息')} colProps={{ span: 24 }}>
        <ProFormText
          name="contactName"
          label="联系人"
          rules={[{ required: true, message: '请输入联系人' }]}
          fieldProps={{ maxLength: 100 }}
        />
        <ProFormText name="contactPhone" label="联系电话" fieldProps={{ maxLength: 64 }} />
        <ProFormText
          name="contactEmail"
          label="联系邮箱"
          rules={[
            { required: true, message: '请输入联系邮箱' },
            { type: 'email', message: '邮箱格式不正确' },
          ]}
          fieldProps={{ maxLength: 128 }}
        />
        <ProFormText name="companyName" label="公司名称" fieldProps={{ maxLength: 200 }} />
        <ProFormSelect
          name="countryCode"
          label="国家/地区"
          rules={[{ required: true, message: '请选择国家/地区' }]}
          fieldProps={searchableCountryProps}
        />
        <ProFormField
          name="stateProvince"
          label="州/省"
          valueType="text"
          rules={[{ required: true, message: '请输入州/省' }]}
          colProps={{ span: 12 }}
        >
          <StateProvinceControl
            countryCode={countryCode}
            options={stateOptions}
            onStateChange={clearCityForStateChange}
            onStateSelect={loadCitiesForStateSelect}
          />
        </ProFormField>
        <ProFormField
          name="city"
          label="城市"
          valueType="text"
          rules={[{ required: true, message: '请输入城市' }]}
          colProps={{ span: 12 }}
        >
          <CityControl
            countryCode={countryCode}
            options={cityOptions}
            stateProvince={stateProvince}
            onLoadCities={loadCities}
          />
        </ProFormField>
        <ProFormText
          name="postalCode"
          label="邮编"
          rules={[{ required: true, message: '请输入邮编' }]}
          fieldProps={{ maxLength: 32 }}
        />
        <ProFormText
          name="addressLine1"
          label="地址1"
          rules={[{ required: true, message: '请输入地址1' }]}
          fieldProps={{ maxLength: 255 }}
        />
        <ProFormText name="addressLine2" label="地址2" fieldProps={{ maxLength: 255 }} />
        <ProFormTextArea name="remark" label="备注" fieldProps={{ maxLength: 500, rows: 3 }} />
      </ProForm.Group>
    </>
  );
}
