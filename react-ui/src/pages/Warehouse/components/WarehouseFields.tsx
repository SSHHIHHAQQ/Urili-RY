import {
  ProFormAutoComplete,
  ProFormDependency,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
} from '@ant-design/pro-components';
import { useEffect, useMemo, useState } from 'react';
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
  return items.map((item) => ({
    label: item.placeType ? `${item.cityName} (${item.placeType})` : item.cityName,
    value: item.cityName,
    searchText: `${item.cityName} ${item.placeName || ''} ${item.placeType || ''}`,
  }));
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

  const loadCities = async (stateName?: string, keyword?: string) => {
    const resp = await getUsCityOptions({ stateName, keyword });
    if (resp.code === 200) {
      setCityOptions(toCityOptions(resp.data || []));
    }
  };

  return (
    <>
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
      {showSeller ? (
        <ProFormSelect
          name="sellerId"
          label="归属卖家"
          rules={[{ required: true, message: '请选择归属卖家' }]}
          fieldProps={searchableSellerProps}
        />
      ) : null}
      <ProFormSelect
        name="settlementCurrency"
        label="结算币种"
        rules={[{ required: true, message: '请选择结算币种' }]}
        fieldProps={searchableCurrencyProps}
      />
      <ProFormSelect
        name="countryCode"
        label="国家/地区"
        rules={[{ required: true, message: '请选择国家/地区' }]}
        fieldProps={searchableCountryProps}
      />
      <ProFormDependency name={['countryCode', 'stateProvince']}>
        {({ countryCode, stateProvince }) =>
          countryCode === 'US' ? (
            <>
              <ProFormAutoComplete
                name="stateProvince"
                label="州/省"
                rules={[{ required: true, message: '请输入州/省' }]}
                fieldProps={{
                  options: stateOptions,
                  filterOption: true,
                  onChange: () => setCityOptions([]),
                }}
              />
              <ProFormAutoComplete
                name="city"
                label="城市"
                rules={[{ required: true, message: '请输入城市' }]}
                fieldProps={{
                  options: cityOptions,
                  filterOption: true,
                  onFocus: () => loadCities(stateProvince),
                  onSearch: (keyword) => loadCities(stateProvince, keyword),
                }}
              />
            </>
          ) : (
            <>
              <ProFormText
                name="stateProvince"
                label="州/省"
                rules={[{ required: true, message: '请输入州/省' }]}
                fieldProps={{ maxLength: 100 }}
              />
              <ProFormText
                name="city"
                label="城市"
                rules={[{ required: true, message: '请输入城市' }]}
                fieldProps={{ maxLength: 100 }}
              />
            </>
          )
        }
      </ProFormDependency>
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
      <ProFormTextArea name="remark" label="备注" fieldProps={{ maxLength: 500, rows: 3 }} />
    </>
  );
}
