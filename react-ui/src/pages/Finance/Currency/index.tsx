import { DownOutlined, PlusOutlined } from '@ant-design/icons';
import {
  type ActionType,
  DrawerForm,
  ModalForm,
  PageContainer,
  type ProColumns,
  ProFormDependency,
  ProFormDigit,
  type ProFormInstance,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Button, Dropdown, Modal, Tabs } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { getDictSelectOption } from '@/services/system/dict';
import { message } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  addCurrency,
  deleteCurrency,
  getCurrencyList,
  getRateHistoryList,
  updateCurrency,
  updateCurrencyStatus,
} from '@/services/finance/currency';
import {
  adjustmentModeOptions,
  roundingModeOptions,
  statusValueEnum,
  yesNoValueEnum,
} from './constants';
import SyncSettingsPanel from './components/SyncSettingsPanel';

const defaultCurrencyValues: Partial<API.Finance.Currency> = {
  amountPrecision: 2,
  baseCurrencyCode: 'CNY',
  isDefault: 'N',
  ratePrecision: 8,
  roundingMode: 'HALF_UP',
  adjustmentMode: 'NONE',
  status: '0',
};

const baseCurrencyOptions = [{ label: '人民币 (CNY)', value: 'CNY' }];

const adjustedModeValues = ['PERCENT_UP', 'PERCENT_DOWN', 'FIXED_DELTA'];

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function normalizeRatePrecision(precision?: number) {
  if (precision === undefined || precision === null) {
    return 8;
  }
  return Math.min(Math.max(Number(precision), 0), 10);
}

function formatRate(value?: number | string, precision?: number) {
  if (value === undefined || value === null) {
    return '-';
  }
  return Number(value).toFixed(normalizeRatePrecision(precision));
}

export default function FinanceCurrencyPage() {
  const access = useAccess();
  const actionRef = useRef<ActionType>(null);
  const historyActionRef = useRef<ActionType>(null);
  const currencyFormRef =
    useRef<ProFormInstance<API.Finance.Currency> | undefined>(undefined);
  const [currencyOptions, setCurrencyOptions] = useState<any[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [currentCurrency, setCurrentCurrency] =
    useState<API.Finance.Currency>();
  const [historyCurrency, setHistoryCurrency] =
    useState<API.Finance.Currency>();

  useEffect(() => {
    getDictSelectOption('currency_code').then(setCurrencyOptions);
  }, []);

  const openCreateModal = () => {
    setCurrentCurrency(undefined);
    setModalOpen(true);
  };

  const openEditModal = (record: API.Finance.Currency) => {
    setCurrentCurrency(record);
    setModalOpen(true);
  };

  const saveCurrency = async (values: API.Finance.Currency) => {
    const payload = { ...values, baseCurrencyCode: 'CNY' };
    const resp = currentCurrency
      ? await updateCurrency(currentCurrency.currencyCode, payload)
      : await addCurrency(payload);
    if (resultOk(resp, currentCurrency ? '币种已更新' : '币种已新增')) {
      actionRef.current?.reload();
      return true;
    }
    return false;
  };

  const toggleStatus = async (record: API.Finance.Currency) => {
    const nextStatus = record.status === '0' ? '1' : '0';
    const ok = resultOk(
      await updateCurrencyStatus(record.currencyCode, nextStatus),
      nextStatus === '0' ? '币种已启用' : '币种已停用',
    );
    if (ok) actionRef.current?.reload();
  };

  const removeCurrency = (record: API.Finance.Currency) => {
    Modal.confirm({
      title: '删除币种',
      content: `确认删除 ${record.currencyCode}？已有汇率历史的币种会被后端拒绝删除。`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await deleteCurrency(record.currencyCode),
          '币种已删除',
        );
        if (ok) actionRef.current?.reload();
      },
    });
  };

  const setDefaultCurrency = async (record: API.Finance.Currency) => {
    const ok = resultOk(
      await updateCurrency(record.currencyCode, { ...record, isDefault: 'Y' }),
      '默认币种已更新',
    );
    if (ok) actionRef.current?.reload();
  };

  useEffect(() => {
    if (!modalOpen) {
      return;
    }
    currencyFormRef.current?.resetFields();
    currencyFormRef.current?.setFieldsValue(currentCurrency || defaultCurrencyValues);
  }, [currentCurrency, modalOpen]);

  const currencyColumns: ProColumns<API.Finance.Currency>[] = [
    {
      title: '币种代码',
      dataIndex: 'currencyCode',
      width: 110,
    },
    {
      title: '币种名称',
      dataIndex: 'currencyName',
      width: 180,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 100,
    },
    {
      title: '默认',
      dataIndex: 'isDefault',
      valueType: 'select',
      valueEnum: yesNoValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 90,
    },
    {
      title: '基准币种',
      dataIndex: 'baseCurrencyCode',
      width: 110,
      search: false,
    },
    {
      title: '官方汇率',
      dataIndex: 'officialRate',
      width: 130,
      search: false,
      render: (_, record) => formatRate(record.officialRate, record.ratePrecision),
    },
    {
      title: '生效汇率',
      dataIndex: 'effectiveRate',
      width: 130,
      search: false,
      render: (_, record) => formatRate(record.effectiveRate, record.ratePrecision),
    },
    {
      title: '调整方式',
      dataIndex: 'adjustmentMode',
      width: 110,
      search: false,
      renderText: (value) =>
        adjustmentModeOptions.find((item) => item.value === value)?.label ||
        value,
    },
    {
      title: '生效汇率时间',
      dataIndex: 'effectiveRateTime',
      width: 170,
      search: false,
    },
    {
      title: '生效汇率时间',
      dataIndex: 'effectiveRateTimeRange',
      colSize: 2,
      valueType: 'dateRange',
      hideInTable: true,
      search: {
        transform: (value) => ({
          effectiveBeginTime: value?.[0],
          effectiveEndTime: value?.[1],
        }),
      },
    },
    {
      title: '操作',
      valueType: 'option',
      width: 190,
      render: (_, record) => [
        <Button
          key="edit"
          type="link"
          size="small"
          hidden={!access.hasPerms('finance:currency:edit')}
          onClick={() => openEditModal(record)}
        >
          编辑
        </Button>,
        <Button
          key="status"
          type="link"
          size="small"
          hidden={!access.hasPerms('finance:currency:edit')}
          onClick={() => toggleStatus(record)}
        >
          {record.status === '0' ? '停用' : '启用'}
        </Button>,
        <Dropdown
          key="more"
          menu={{
            items: [
              {
                key: 'history',
                label: '汇率历史',
              },
              {
                key: 'default',
                label: '设为默认',
                disabled:
                  record.isDefault === 'Y' ||
                  !access.hasPerms('finance:currency:edit'),
              },
              {
                key: 'delete',
                danger: true,
                label: '删除',
                disabled: !access.hasPerms('finance:currency:remove'),
              },
            ],
            onClick: ({ key }) => {
              if (key === 'history') setHistoryCurrency(record);
              if (key === 'default') setDefaultCurrency(record);
              if (key === 'delete') removeCurrency(record);
            },
          }}
          trigger={['click']}
        >
          <Button type="link" size="small">
            更多 <DownOutlined style={{ fontSize: 10 }} />
          </Button>
        </Dropdown>,
      ],
    },
  ];

  const historyColumns: ProColumns<API.Finance.RateHistory>[] = [
    { title: '时间', dataIndex: 'createTime', width: 170 },
    { title: '来源', dataIndex: 'sourceType', width: 100 },
    { title: '基准币种', dataIndex: 'baseCurrencyCode', width: 110 },
    {
      title: '官方汇率',
      dataIndex: 'officialRate',
      width: 130,
      render: (_, record) => formatRate(record.officialRate, historyCurrency?.ratePrecision),
    },
    {
      title: '生效汇率',
      dataIndex: 'effectiveRate',
      width: 130,
      render: (_, record) => formatRate(record.effectiveRate, historyCurrency?.ratePrecision),
    },
    { title: '调整方式', dataIndex: 'adjustmentMode', width: 120 },
    { title: '原因', dataIndex: 'changeReason', ellipsis: true },
  ];

  const currencyTable = (
    <ProTable<API.Finance.Currency>
      actionRef={actionRef}
      rowKey="currencyCode"
      columns={currencyColumns}
      scroll={getProTableScroll(1500)}
      search={getPersistedProTableSearch({ labelWidth: 110 }, 'finance-currency')}
      request={async (params) => {
        const resp = await getCurrencyList(params);
        return {
          data: resp.rows || [],
          success: resp.code === 200,
          total: resp.total || 0,
        };
      }}
      toolBarRender={() => [
        <Button
          key="add"
          type="primary"
          icon={<PlusOutlined />}
          hidden={!access.hasPerms('finance:currency:add')}
          onClick={openCreateModal}
        >
          新增币种
        </Button>,
      ]}
    />
  );

  return (
    <PageContainer>
      <Tabs
        items={[
          { key: 'currency', label: '币种列表', children: currencyTable },
          {
            key: 'sync',
            label: '同步设置',
            children: (
              <SyncSettingsPanel
                access={access}
                onSynced={() => actionRef.current?.reload()}
              />
            ),
          },
        ]}
      />

      <ModalForm<API.Finance.Currency>
        key={currentCurrency?.currencyCode || 'new-currency'}
        formRef={currencyFormRef}
        title={currentCurrency ? '编辑币种' : '新增币种'}
        open={modalOpen}
        modalProps={{ destroyOnHidden: true, onCancel: () => setModalOpen(false) }}
        initialValues={currentCurrency || defaultCurrencyValues}
        onOpenChange={setModalOpen}
        onFinish={saveCurrency}
      >
        <ProFormSelect
          name="currencyCode"
          label="币种代码"
          options={currencyOptions}
          disabled={!!currentCurrency}
          rules={[{ required: true }]}
          fieldProps={{
            ...SEARCHABLE_SELECT_PROPS,
            onChange: (_, option: any) => {
              if (!currentCurrency && option?.label) {
                currencyFormRef.current?.setFieldValue('currencyName', option.label);
              }
            },
          }}
        />
        <ProFormText
          name="currencyName"
          label="币种名称"
          rules={[{ required: true }]}
        />
        <ProFormText name="currencySymbol" label="币种符号" />
        <ProFormSelect
          name="baseCurrencyCode"
          label="基准币种"
          options={baseCurrencyOptions}
          readonly
          rules={[{ required: true }]}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormDigit name="officialRate" label="官方汇率" min={0} />
        <ProFormSelect
          name="adjustmentMode"
          label="调整方式"
          options={adjustmentModeOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormDependency name={['adjustmentMode']}>
          {({ adjustmentMode }) => {
            const mode = adjustmentMode || 'NONE';
            const manualMode = mode === 'MANUAL';
            const adjustedMode = adjustedModeValues.includes(mode);
            return (
              <>
                <ProFormDigit
                  name="effectiveRate"
                  label="生效汇率"
                  min={0}
                  disabled={!manualMode}
                  tooltip="人工维护模式下填写；其他调整方式由后端按官方汇率和调整值计算"
                />
                <ProFormDigit
                  name="adjustmentValue"
                  label="调整值"
                  disabled={!adjustedMode}
                  tooltip="百分比模式下 1 表示 1%，固定加减值按汇率值填写"
                />
              </>
            );
          }}
        </ProFormDependency>
        <ProFormDigit name="ratePrecision" label="汇率精度" min={0} max={10} />
        <ProFormDigit name="amountPrecision" label="金额精度" min={0} max={6} />
        <ProFormSelect
          name="roundingMode"
          label="舍入方式"
          options={roundingModeOptions}
          fieldProps={SEARCHABLE_SELECT_PROPS}
        />
        <ProFormSelect name="isDefault" label="默认币种" valueEnum={yesNoValueEnum} fieldProps={SEARCHABLE_SELECT_PROPS} />
        <ProFormSelect name="status" label="状态" valueEnum={statusValueEnum} fieldProps={SEARCHABLE_SELECT_PROPS} />
        <ProFormTextArea name="remark" label="备注" />
      </ModalForm>

      <DrawerForm
        title={`${historyCurrency?.currencyCode || ''} 汇率历史`}
        open={!!historyCurrency}
        drawerProps={{
          destroyOnHidden: true,
          onClose: () => setHistoryCurrency(undefined),
        }}
        submitter={false}
      >
        {historyCurrency ? (
          <ProTable<API.Finance.RateHistory>
            actionRef={historyActionRef}
            rowKey="rateHistoryId"
            columns={historyColumns}
            search={false}
            scroll={getProTableScroll(1000)}
            request={async (params) => {
              const resp = await getRateHistoryList(
                historyCurrency.currencyCode,
                params,
              );
              return {
                data: resp.rows || [],
                success: resp.code === 200,
                total: resp.total || 0,
              };
            }}
          />
        ) : null}
      </DrawerForm>
    </PageContainer>
  );
}
