import {
  DeleteOutlined,
  ExclamationCircleFilled,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { PageContainer, type ProColumns, ProTable } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  Button,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import { useEffect, useMemo, useState } from 'react';
import {
  calculateFeeEstimate,
  getFeeEstimateOptions,
  getFeeEstimateSkus,
} from '@/services/finance/feeEstimate';
import { message } from '@/utils/feedback';
import { getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import styles from './style.module.css';

type FormValues = {
  selectionMode?: API.Finance.FeeEstimateSelectionMode;
  buyerId?: number;
  originWarehouseCode?: string;
  warehouseCodes?: string[];
  customerChannelCode?: string;
  destinationCountryCode?: string;
  destinationState?: string;
  destinationCity?: string;
  destinationPostalCode?: string;
  destinationAddress1?: string;
  destinationAddress2?: string;
  quoteSchemeId?: number;
  channelCodes?: string[];
  manualLengthCm?: number;
  manualWidthCm?: number;
  manualHeightCm?: number;
  manualWeightKg?: number;
};

const VIEW_OPERATIONS: API.Finance.FeeEstimateView = 'OPERATIONS';
const VIEW_BUYER_SIMULATION: API.Finance.FeeEstimateView = 'BUYER_SIMULATION';

const inputModeOptions = [
  { label: '选择 SKU', value: 'SKU' },
  { label: '手工尺寸', value: 'MANUAL' },
];

const selectionModeOptions = [
  { label: '手动指定', value: 'MANUAL' },
  { label: '自动最优', value: 'AUTO_BEST' },
];

const viewTabs = [
  { key: VIEW_OPERATIONS, label: '运营试算' },
  { key: VIEW_BUYER_SIMULATION, label: '买家模拟' },
];

const countryOptions = [
  { label: 'United States of America (USA)', value: 'US', searchText: 'US USA United States' },
];

function resultOk(resp: API.Result, successText?: string) {
  if (resp.code === 200) {
    if (successText) {
      message.success(successText);
    }
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function formatNumber(value?: number, suffix = '') {
  if (value === undefined || value === null) {
    return '--';
  }
  const text = Number(value).toFixed(3).replace(/\.?0+$/, '');
  return `${text}${suffix}`;
}

function formatAmount(value?: number, currencyCode?: string) {
  if (value === undefined || value === null) {
    return '--';
  }
  return `${currencyCode || ''} ${Number(value).toFixed(2)}`.trim();
}

function skuSizeText(record: API.Finance.FeeEstimateSkuSnapshot) {
  if (
    record.measureLengthCm === undefined
    || record.measureWidthCm === undefined
    || record.measureHeightCm === undefined
  ) {
    return '--';
  }
  return `${formatNumber(record.measureLengthCm)} * ${formatNumber(record.measureWidthCm)} * ${formatNumber(record.measureHeightCm)} cm`;
}

function skuWeightText(record: API.Finance.FeeEstimateSkuSnapshot) {
  return formatNumber(record.measureWeightKg, ' kg');
}

function routeLabel(record: API.Finance.FeeEstimateRouteCandidate) {
  return record.failureMessage || record.failureCode || '候选不可执行';
}

function skuSelectionKey(record?: Pick<API.Finance.FeeEstimateSkuSnapshot, 'skuId'>) {
  return record?.skuId ? `sku-${record.skuId}` : '';
}

function getSkuSourceWarehouseCodes(record?: API.Finance.FeeEstimateSkuSnapshot) {
  return Array.from(new Set((record?.sourceWarehouseCodes || [])
    .map((code) => String(code || '').trim())
    .filter(Boolean)));
}

function getCommonSourceWarehouseCodes(records: API.Finance.FeeEstimateSkuSnapshot[]) {
  if (!records.length) {
    return [];
  }
  const [firstRecord, ...restRecords] = records;
  let commonCodes = getSkuSourceWarehouseCodes(firstRecord);
  restRecords.forEach((record) => {
    const codes = new Set(getSkuSourceWarehouseCodes(record));
    commonCodes = commonCodes.filter((code) => codes.has(code));
  });
  return commonCodes;
}

function canMergeSkuBySourceWarehouse(
  currentRows: API.Finance.FeeEstimateSkuSnapshot[],
  candidate: API.Finance.FeeEstimateSkuSnapshot,
) {
  const candidateCodes = getSkuSourceWarehouseCodes(candidate);
  if (candidateCodes.length === 0) {
    return false;
  }
  const nextRows = currentRows
    .filter((row) => row.skuId !== candidate.skuId)
    .concat(candidate);
  return getCommonSourceWarehouseCodes(nextRows).length > 0;
}

export default function FinanceFeeEstimatePage() {
  const access = useAccess();
  const canQuery = access.hasPerms('finance:feeEstimate:query');
  const canCalculate = access.hasPerms('finance:feeEstimate:calculate');
  const [form] = Form.useForm<FormValues>();
  const [estimateView, setEstimateView] =
    useState<API.Finance.FeeEstimateView>(VIEW_OPERATIONS);
  const [selectionMode, setSelectionMode] =
    useState<API.Finance.FeeEstimateSelectionMode>('MANUAL');
  const [packageInputMode, setPackageInputMode] =
    useState<API.Finance.FeeEstimateInputMode>('SKU');
  const [options, setOptions] = useState<API.Finance.FeeEstimateOptions>({});
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [skuRows, setSkuRows] = useState<API.Finance.FeeEstimateSkuSnapshot[]>([]);
  const [skuOptions, setSkuOptions] = useState<API.Finance.FeeEstimateSkuSnapshot[]>([]);
  const [skuSelectorOpen, setSkuSelectorOpen] = useState(false);
  const [selectedSkuMap, setSelectedSkuMap] =
    useState<Record<string, API.Finance.FeeEstimateSkuSnapshot>>({});
  const [skuLoading, setSkuLoading] = useState(false);
  const [calculateLoading, setCalculateLoading] = useState(false);
  const [estimateResult, setEstimateResult] = useState<API.Finance.FeeEstimateResponse>();

  const isBuyerSimulation = estimateView === VIEW_BUYER_SIMULATION;
  const effectivePackageInputMode = isBuyerSimulation ? 'SKU' : packageInputMode;

  const quoteSchemeOptions = useMemo(
    () => options.quoteSchemes || [],
    [options.quoteSchemes],
  );
  const buyerOptions = useMemo(
    () => options.buyers || [],
    [options.buyers],
  );
  const warehouseOptions = useMemo(
    () => options.warehouses || [],
    [options.warehouses],
  );
  const channelOptions = useMemo(
    () => options.channels || [],
    [options.channels],
  );
  const customerChannelOptions = useMemo(
    () => options.customerChannels || options.channels || [],
    [options.customerChannels, options.channels],
  );
  const selectedSkuItems = useMemo(() => Object.values(selectedSkuMap), [selectedSkuMap]);
  const selectedSkuKeys = useMemo(() => Object.keys(selectedSkuMap), [selectedSkuMap]);
  const selectedCommonWarehouseCodes = useMemo(
    () => getCommonSourceWarehouseCodes(selectedSkuItems),
    [selectedSkuItems],
  );
  const buyerCommonWarehouseCodes = useMemo(
    () => getCommonSourceWarehouseCodes(skuRows),
    [skuRows],
  );

  const loadOptions = async (schemeId?: number) => {
    if (!canQuery) {
      return;
    }
    setOptionsLoading(true);
    try {
      const resp = await getFeeEstimateOptions(schemeId);
      if (resultOk(resp as API.Result)) {
        const data = resp.data || {};
        setOptions(data);
        if (!schemeId && data.selectedScheme?.schemeId) {
          form.setFieldValue('quoteSchemeId', data.selectedScheme.schemeId);
        }
      }
    } finally {
      setOptionsLoading(false);
    }
  };

  const loadSkuOptions = async (skuCode?: string) => {
    if (!canQuery) {
      return;
    }
    setSkuLoading(true);
    try {
      const resp = await getFeeEstimateSkus({
        skuCode,
        pageNum: 1,
        pageSize: 20,
      });
      if (resultOk(resp as API.Result)) {
        setSkuOptions(resp.rows || []);
      }
    } finally {
      setSkuLoading(false);
    }
  };

  useEffect(() => {
    loadOptions();
    loadSkuOptions();
  }, [canQuery]);

  const addSkuRow = (skuId: number) => {
    const selected = skuOptions.find((item) => item.skuId === skuId);
    if (!selected) {
      return;
    }
    if (skuRows.some((item) => item.skuId === skuId)) {
      message.warning('SKU 已在试算列表中');
      return;
    }
    setSkuRows([...skuRows, { ...selected, quantity: 1 }]);
    setEstimateResult(undefined);
  };

  const updateSkuQuantity = (skuId: number, quantity?: number | null) => {
    setSkuRows((rows) => rows.map((item) => (
      item.skuId === skuId ? { ...item, quantity: Number(quantity || 1) } : item
    )));
    setEstimateResult(undefined);
  };

  const removeSkuRow = (skuId: number) => {
    setSkuRows((rows) => rows.filter((item) => item.skuId !== skuId));
    setEstimateResult(undefined);
  };

  const openSkuSelector = () => {
    if (!canQuery) {
      message.warning('缺少 SKU 查询权限');
      return;
    }
    const nextSelectedMap: Record<string, API.Finance.FeeEstimateSkuSnapshot> = {};
    skuRows.forEach((row) => {
      const key = skuSelectionKey(row);
      if (key) {
        nextSelectedMap[key] = row;
      }
    });
    setSelectedSkuMap(nextSelectedMap);
    setSkuSelectorOpen(true);
  };

  const updateSelectedSku = (item: API.Finance.FeeEstimateSkuSnapshot, selected: boolean) => {
    const key = skuSelectionKey(item);
    if (!key) {
      if (selected) {
        message.warning('SKU 缺少稳定标识，不能选择');
      }
      return;
    }
    setSelectedSkuMap((current) => {
      const next = { ...current };
      if (selected) {
        if (!canMergeSkuBySourceWarehouse(Object.values(current), item)) {
          message.warning('所选 SKU 没有共同来源仓，不能放在同一个包裹试算');
          return current;
        }
        const currentRow = skuRows.find((row) => row.skuId === item.skuId);
        next[key] = {
          ...item,
          quantity: current[key]?.quantity || currentRow?.quantity || item.quantity || 1,
        };
      } else {
        delete next[key];
      }
      return next;
    });
  };

  const removeSelectedSku = (key: string) => {
    setSelectedSkuMap((current) => {
      const next = { ...current };
      delete next[key];
      return next;
    });
  };

  const clearSelectedSkus = () => {
    setSelectedSkuMap({});
  };

  const applySelectedSkus = () => {
    if (!selectedSkuItems.length) {
      message.warning('请选择商品 SKU');
      return;
    }
    if (!getCommonSourceWarehouseCodes(selectedSkuItems).length) {
      message.error('所选 SKU 必须至少有一个共同来源仓');
      return;
    }
    const quantityBySkuId = new Map(skuRows.map((row) => [row.skuId, row.quantity || 1]));
    setSkuRows(selectedSkuItems.map((item) => ({
      ...item,
      quantity: quantityBySkuId.get(item.skuId) || item.quantity || 1,
    })));
    setEstimateResult(undefined);
    setSkuSelectorOpen(false);
  };

  const handleViewChange = (nextView: string) => {
    const view = nextView as API.Finance.FeeEstimateView;
    setEstimateView(view);
    setEstimateResult(undefined);
    setSelectionMode('MANUAL');
    form.setFieldValue('selectionMode', 'MANUAL');
    if (view === VIEW_BUYER_SIMULATION) {
      setPackageInputMode('SKU');
      form.setFieldsValue({
        quoteSchemeId: undefined,
        channelCodes: [],
        warehouseCodes: [],
      });
    }
  };

  const handleSelectionModeChange = (value: API.Finance.FeeEstimateSelectionMode) => {
    setSelectionMode(value);
    setEstimateResult(undefined);
    if (value === 'AUTO_BEST') {
      form.setFieldsValue({
        customerChannelCode: undefined,
        originWarehouseCode: undefined,
      });
    } else {
      form.setFieldValue('warehouseCodes', []);
    }
  };

  const buildPackageLines = (values: FormValues): API.Finance.FeeEstimatePackageLine[] => {
    if (effectivePackageInputMode === 'MANUAL') {
      return [{
        quantity: 1,
        lengthCm: values.manualLengthCm,
        widthCm: values.manualWidthCm,
        heightCm: values.manualHeightCm,
        weightKg: values.manualWeightKg,
      }];
    }
    return skuRows.map((item) => ({
      skuId: item.skuId,
      quantity: item.quantity || 1,
    }));
  };

  const handleCalculate = async (values: FormValues) => {
    if (!canCalculate) {
      message.error('缺少费用试算权限');
      return;
    }
    if (effectivePackageInputMode === 'SKU' && skuRows.length === 0) {
      message.error('请选择 SKU');
      return;
    }
    if (isBuyerSimulation && effectivePackageInputMode === 'SKU' && skuRows.length > 0 && !buyerCommonWarehouseCodes.length) {
      message.error('所选 SKU 必须至少有一个共同来源仓');
      return;
    }
    setCalculateLoading(true);
    try {
      const isBuyerAuto = isBuyerSimulation && selectionMode === 'AUTO_BEST';
      const isBuyerManual = isBuyerSimulation && selectionMode === 'MANUAL';
      const resp = await calculateFeeEstimate({
        estimateView,
        selectionMode,
        buyerId: isBuyerSimulation ? values.buyerId : undefined,
        packageInputMode: effectivePackageInputMode as API.Finance.FeeEstimateInputMode,
        originWarehouseCode: isBuyerAuto ? undefined : values.originWarehouseCode,
        warehouseCodes: isBuyerAuto ? values.warehouseCodes || [] : undefined,
        customerChannelCode: isBuyerManual ? values.customerChannelCode : undefined,
        destinationCountryCode: values.destinationCountryCode!,
        destinationState: values.destinationState,
        destinationCity: values.destinationCity,
        destinationPostalCode: values.destinationPostalCode!,
        destinationAddress1: values.destinationAddress1,
        destinationAddress2: values.destinationAddress2,
        quoteSchemeId: isBuyerSimulation ? undefined : values.quoteSchemeId,
        channelCodes: isBuyerSimulation
          ? (isBuyerManual && values.customerChannelCode ? [values.customerChannelCode] : [])
          : values.channelCodes || [],
        packageLines: buildPackageLines(values),
      });
      if (resultOk(resp as API.Result)) {
        setEstimateResult(resp.data);
      }
    } finally {
      setCalculateLoading(false);
    }
  };

  const skuColumns: ProColumns<API.Finance.FeeEstimateSkuSnapshot>[] = [
    {
      title: 'SKU',
      dataIndex: 'systemSkuCode',
      width: 180,
      render: (_, record) => record.systemSkuCode || record.sellerSkuCode || record.masterSku || record.skuId,
    },
    {
      title: '商品名称',
      dataIndex: 'productName',
      ellipsis: true,
      render: (_, record) => record.productName || record.productNameEn || '--',
    },
    {
      title: '数量',
      dataIndex: 'quantity',
      width: 140,
      render: (_, record) => (
        <InputNumber
          min={1}
          precision={0}
          value={record.quantity || 1}
          onChange={(value) => updateSkuQuantity(record.skuId, value)}
        />
      ),
    },
    {
      title: '尺寸',
      dataIndex: 'measureLengthCm',
      width: 190,
      render: (_, record) => skuSizeText(record),
    },
    {
      title: '重量',
      dataIndex: 'measureWeightKg',
      width: 120,
      render: (_, record) => skuWeightText(record),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 90,
      render: (_, record) => [
        <Button
          key="remove"
          type="link"
          size="small"
          danger
          icon={<DeleteOutlined />}
          onClick={() => removeSkuRow(record.skuId)}
        />,
      ],
    },
  ];

  const skuSelectorColumns: ProColumns<API.Finance.FeeEstimateSkuSnapshot>[] = [
    {
      title: '来源仓',
      dataIndex: 'sourceWarehouseCode',
      valueType: 'select',
      hideInTable: true,
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        allowClear: true,
        options: warehouseOptions,
        placeholder: '筛选来源仓',
      },
    },
    {
      title: 'SKU',
      dataIndex: 'skuCode',
      hideInTable: true,
      fieldProps: {
        placeholder: '系统 SKU / 客户 SKU / 来源 SKU',
      },
    },
    {
      title: '商品名称',
      dataIndex: 'productName',
      hideInTable: true,
      fieldProps: {
        placeholder: '商品中文名 / 英文名',
      },
    },
    {
      title: 'SKU',
      dataIndex: 'systemSkuCode',
      width: 180,
      search: false,
      render: (_, record) => (
        <Space size={4} direction="vertical">
          <span>{record.systemSkuCode || record.sellerSkuCode || record.masterSku || record.skuId}</span>
          {record.sellerSkuCode && record.sellerSkuCode !== record.systemSkuCode ? (
            <Typography.Text type="secondary">{record.sellerSkuCode}</Typography.Text>
          ) : null}
        </Space>
      ),
    },
    {
      title: '商品名称',
      dataIndex: 'productName',
      width: 260,
      ellipsis: true,
      search: false,
      render: (_, record) => record.productName || record.productNameEn || '--',
    },
    {
      title: '来源 SKU',
      dataIndex: 'masterSku',
      width: 150,
      ellipsis: true,
      search: false,
      renderText: (value) => value || '--',
    },
    {
      title: '可用库存',
      dataIndex: 'availableStock',
      width: 110,
      search: false,
      renderText: (value) => value ?? '--',
    },
    {
      title: '尺寸重量',
      dataIndex: 'measureLengthCm',
      width: 230,
      search: false,
      render: (_, record) => (
        <Space size={4} direction="vertical">
          <span>{skuSizeText(record)}</span>
          <Typography.Text type="secondary">{skuWeightText(record)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '来源仓',
      dataIndex: 'sourceWarehouseNames',
      width: 220,
      ellipsis: true,
      search: false,
      render: (_, record) => (
        <Space size={4} direction="vertical">
          <span>{record.sourceWarehouseNames || '--'}</span>
          {getSkuSourceWarehouseCodes(record).length ? (
            <Typography.Text type="secondary">
              {getSkuSourceWarehouseCodes(record).join(', ')}
            </Typography.Text>
          ) : null}
        </Space>
      ),
    },
    {
      title: '仓库数',
      dataIndex: 'sourceWarehouseCount',
      width: 90,
      search: false,
      renderText: (value) => value ?? '--',
    },
  ];

  const routeColumns: ProColumns<API.Finance.FeeEstimateRouteCandidate>[] = [
    {
      title: '仓库',
      dataIndex: 'warehouseName',
      width: 180,
      render: (_, record) => record.warehouseName || record.warehouseCode || '--',
    },
    {
      title: '报价方案',
      dataIndex: 'schemeName',
      width: 180,
      ellipsis: true,
      renderText: (value) => value || '--',
    },
    {
      title: '客户渠道',
      dataIndex: 'customerChannelName',
      width: 200,
      render: (_, record) => (
        <Space size={4}>
          <span>{record.customerChannelName || record.customerChannelCode || '--'}</span>
          {record.customerChannelCode ? <Tag>{record.customerChannelCode}</Tag> : null}
        </Space>
      ),
    },
    {
      title: '系统渠道',
      dataIndex: 'systemChannelName',
      width: 200,
      render: (_, record) => record.systemChannelName || record.systemChannelCode || '--',
    },
    {
      title: '上游能力',
      dataIndex: 'carrierExternalChannelCode',
      width: 180,
      render: (_, record) => record.carrierExternalChannelCode || record.carrierConnectionCode || record.fulfillmentMode || '--',
    },
    {
      title: '状态',
      dataIndex: 'executable',
      width: 180,
      render: (_, record) => (
        record.executable === true ? (
          <Tag color="success">可执行</Tag>
        ) : (
          <Tooltip title={routeLabel(record)}>
            <Tag color="warning">不可执行</Tag>
          </Tooltip>
        )
      ),
    },
  ];

  const resultColumns: ProColumns<API.Finance.FeeEstimateChannelResult>[] = [
    ...(isBuyerSimulation ? [{
      title: '推荐',
      dataIndex: 'recommended',
      width: 80,
      fixed: 'left' as const,
      render: (_: unknown, record: API.Finance.FeeEstimateChannelResult) => (
        record.recommended ? <Tag color="processing">推荐</Tag> : '--'
      ),
    }] : []),
    {
      title: isBuyerSimulation ? '客户渠道' : '物流渠道',
      dataIndex: 'channelName',
      width: 240,
      fixed: isBuyerSimulation ? undefined : 'left',
      render: (_, record) => (
        <Space size={6}>
          {!record.success ? (
            <Tooltip title={record.errorMessage}>
              <ExclamationCircleFilled className={styles.warningIcon} />
            </Tooltip>
          ) : null}
          <span>{record.channelName || record.channelCode || '--'}</span>
          {record.channelCode ? <Tag>{record.channelCode}</Tag> : null}
        </Space>
      ),
    },
    {
      title: '仓库',
      dataIndex: 'warehouseName',
      width: 160,
      render: (_, record) => record.warehouseName || record.warehouseCode || '--',
    },
    {
      title: isBuyerSimulation ? '买家费用' : '总费用',
      dataIndex: 'totalAmount',
      width: 120,
      render: (_, record) => formatAmount(record.totalAmount, record.currencyCode),
    },
    {
      title: '基础运费',
      dataIndex: 'basicFreightAmount',
      width: 120,
      render: (_, record) => formatAmount(record.basicFreightAmount, record.currencyCode),
    },
    {
      title: '附加费',
      dataIndex: 'surchargeAmount',
      width: 120,
      render: (_, record) => formatAmount(record.surchargeAmount, record.currencyCode),
    },
    {
      title: '操作费',
      dataIndex: 'operationFeeAmount',
      width: 120,
      render: (_, record) => formatAmount(record.operationFeeAmount, record.currencyCode),
    },
    {
      title: '包材费',
      dataIndex: 'packageMaterialFeeAmount',
      width: 120,
      render: (_, record) => formatAmount(record.packageMaterialFeeAmount, record.currencyCode),
    },
    {
      title: '系统渠道',
      dataIndex: 'systemChannelName',
      width: 180,
      render: (_, record) => record.systemChannelName || record.systemChannelCode || '--',
    },
    {
      title: '系统成本',
      dataIndex: 'systemCostAmount',
      width: 120,
      renderText: () => '--',
    },
    {
      title: '差额',
      dataIndex: 'grossMarginAmount',
      width: 100,
      renderText: () => '--',
    },
    {
      title: '币种',
      dataIndex: 'currencyCode',
      width: 90,
      renderText: (value) => value || '--',
    },
    {
      title: '包裹尺寸',
      dataIndex: 'sizeExpression',
      width: 160,
      render: () => estimateResult?.packageSummary?.sizeExpression || '--',
    },
    {
      title: '实重',
      dataIndex: 'actualWeightKg',
      width: 110,
      renderText: (value) => formatNumber(value, ' kg'),
    },
    {
      title: '体积重',
      dataIndex: 'volumeWeightKg',
      width: 110,
      renderText: (value) => formatNumber(value, ' kg'),
    },
    {
      title: '计费重',
      dataIndex: 'chargeableWeightKg',
      width: 110,
      renderText: (value) => formatNumber(value, ' kg'),
    },
    {
      title: '状态',
      dataIndex: 'success',
      width: 180,
      render: (_, record) => (
        record.success ? <Tag color="success">成功</Tag> : (
          <Tooltip title={record.errorMessage || record.errorCode}>
            <Tag color="warning">{record.errorCode || '未试算'}</Tag>
          </Tooltip>
        )
      ),
    },
  ];

  const renderSkuSelector = () => (
    <div className={styles.modeRow}>
      <Select
        {...SEARCHABLE_SELECT_PROPS}
        className={styles.skuPicker}
        loading={skuLoading}
        value={undefined}
        options={skuOptions.map((item) => ({
          label: item.label || item.systemSkuCode || item.skuId,
          value: item.skuId,
          searchText: item.searchText,
        }))}
        placeholder="输入 SKU 搜索"
        onSearch={loadSkuOptions}
        onDropdownVisibleChange={(open) => {
          if (open && skuOptions.length === 0) {
            loadSkuOptions();
          }
        }}
        onChange={(value) => addSkuRow(Number(value))}
      />
      <Button
        icon={<ReloadOutlined />}
        onClick={() => loadSkuOptions()}
      />
    </div>
  );

  const renderBuyerSkuList = () => (
    <div className={styles.selectedSkuList}>
      {skuRows.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无商品" />
      ) : skuRows.map((item) => (
        <div className={styles.selectedSkuItem} key={item.skuId}>
          <div className={styles.selectedSkuMain}>
            <strong>{item.systemSkuCode || item.sellerSkuCode || item.skuId}</strong>
            <span>{item.productName || item.productNameEn || '--'}</span>
            <small>{skuSizeText(item)} / {skuWeightText(item)}</small>
            <small>
              来源仓: {item.sourceWarehouseNames || getSkuSourceWarehouseCodes(item).join(', ') || '--'}
            </small>
          </div>
          <InputNumber
            min={1}
            precision={0}
            value={item.quantity || 1}
            onChange={(value) => updateSkuQuantity(item.skuId, value)}
          />
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            onClick={() => removeSkuRow(item.skuId)}
          />
        </div>
      ))}
      {skuRows.length ? (
        <div className={styles.skuCommonWarehouseLine}>
          <Typography.Text type={buyerCommonWarehouseCodes.length ? 'secondary' : 'danger'}>
            共同来源仓：{buyerCommonWarehouseCodes.length ? buyerCommonWarehouseCodes.join(', ') : '无'}
          </Typography.Text>
        </div>
      ) : null}
    </div>
  );

  const renderBuyerSkuPicker = () => (
    <>
      <Button
        block
        icon={<SearchOutlined />}
        onClick={openSkuSelector}
      >
        选择商品
      </Button>
      {renderBuyerSkuList()}
    </>
  );

  const resolveSummary = estimateResult?.resolveSummary;
  const resolveCards = [
    { label: '商品仓库候选', value: resolveSummary?.warehouseCandidateCount },
    { label: '报价方案', value: resolveSummary?.quoteSchemeCandidateCount },
    { label: '客户渠道', value: resolveSummary?.customerChannelCandidateCount },
    { label: '路由候选', value: resolveSummary?.routeCandidateCount },
    { label: '可执行', value: resolveSummary?.executableRouteCount },
    { label: '失败候选', value: resolveSummary?.failedCandidateCount },
  ];

  return (
    <PageContainer title={false}>
      <Form<FormValues>
        form={form}
        layout="vertical"
        className={styles.page}
        initialValues={{
          selectionMode: 'MANUAL',
          destinationCountryCode: 'US',
        }}
        onFinish={handleCalculate}
      >
        <aside className={styles.conditionPanel}>
          <div className={styles.conditionBody}>
            <Tabs
              className={styles.viewTabs}
              activeKey={estimateView}
              items={viewTabs}
              onChange={handleViewChange}
            />

            {isBuyerSimulation ? (
              <>
                <Form.Item
                  name="buyerId"
                  label="买家"
                  rules={[{ required: true, message: '请选择买家' }]}
                >
                  <Select
                    {...SEARCHABLE_SELECT_PROPS}
                    loading={optionsLoading}
                    options={buyerOptions}
                    placeholder="请选择买家"
                  />
                </Form.Item>
                <Form.Item label="商品/SKU" required>
                  {renderBuyerSkuPicker()}
                </Form.Item>
                <Form.Item name="selectionMode" label="仓库/渠道选择方式">
                  <Radio.Group
                    className={styles.inputModeGroup}
                    optionType="button"
                    buttonStyle="solid"
                    options={selectionModeOptions}
                    value={selectionMode}
                    onChange={(event) => handleSelectionModeChange(event.target.value)}
                  />
                </Form.Item>
                {selectionMode === 'MANUAL' ? (
                  <>
                    <Form.Item
                      name="originWarehouseCode"
                      label="选择仓库"
                      rules={[{ required: true, message: '请选择仓库' }]}
                    >
                      <Select
                        {...SEARCHABLE_SELECT_PROPS}
                        loading={optionsLoading}
                        options={warehouseOptions}
                        placeholder="请选择仓库"
                      />
                    </Form.Item>
                    <Form.Item
                      name="customerChannelCode"
                      label="选择客户渠道"
                      rules={[{ required: true, message: '请选择客户渠道' }]}
                    >
                      <Select
                        {...SEARCHABLE_SELECT_PROPS}
                        loading={optionsLoading}
                        options={customerChannelOptions}
                        placeholder="请选择客户渠道"
                      />
                    </Form.Item>
                  </>
                ) : (
                  <Form.Item name="warehouseCodes" label="限制仓库">
                    <Select
                      {...SEARCHABLE_SELECT_PROPS}
                      mode="multiple"
                      allowClear
                      loading={optionsLoading}
                      options={warehouseOptions}
                      placeholder="不选则全部可用仓库参与计算"
                    />
                  </Form.Item>
                )}
              </>
            ) : (
              <>
                <Form.Item
                  name="originWarehouseCode"
                  label="发货仓"
                  rules={[{ required: true, message: '请选择发货仓' }]}
                >
                  <Select
                    {...SEARCHABLE_SELECT_PROPS}
                    loading={optionsLoading}
                    options={warehouseOptions}
                    placeholder="请选择发货仓"
                  />
                </Form.Item>
                <Form.Item
                  name="quoteSchemeId"
                  label="报价方案"
                  rules={[{ required: true, message: '请选择报价方案' }]}
                >
                  <Select
                    {...SEARCHABLE_SELECT_PROPS}
                    loading={optionsLoading}
                    options={quoteSchemeOptions}
                    placeholder="请选择报价方案"
                    onChange={(schemeId) => {
                      form.setFieldValue('channelCodes', []);
                      loadOptions(Number(schemeId));
                    }}
                  />
                </Form.Item>
                <Form.Item name="channelCodes" label="物流渠道">
                  <Select
                    {...SEARCHABLE_SELECT_PROPS}
                    mode="multiple"
                    allowClear
                    options={channelOptions}
                    placeholder="不选则展示全部渠道"
                  />
                </Form.Item>
                <Form.Item label="包裹方式" required>
                  <Radio.Group
                    className={styles.inputModeGroup}
                    optionType="button"
                    buttonStyle="solid"
                    options={inputModeOptions}
                    value={packageInputMode}
                    onChange={(event) => {
                      setPackageInputMode(event.target.value);
                      setEstimateResult(undefined);
                    }}
                  />
                </Form.Item>
                {packageInputMode === 'MANUAL' ? (
                  <div className={styles.manualFields}>
                    <Form.Item
                      name="manualLengthCm"
                      label="长度 cm"
                      rules={[{ required: true, message: '请输入长度' }]}
                    >
                      <InputNumber min={0.001} precision={3} />
                    </Form.Item>
                    <Form.Item
                      name="manualWidthCm"
                      label="宽度 cm"
                      rules={[{ required: true, message: '请输入宽度' }]}
                    >
                      <InputNumber min={0.001} precision={3} />
                    </Form.Item>
                    <Form.Item
                      name="manualHeightCm"
                      label="高度 cm"
                      rules={[{ required: true, message: '请输入高度' }]}
                    >
                      <InputNumber min={0.001} precision={3} />
                    </Form.Item>
                    <Form.Item
                      name="manualWeightKg"
                      label="重量 kg"
                      rules={[{ required: true, message: '请输入重量' }]}
                    >
                      <InputNumber min={0.001} precision={3} />
                    </Form.Item>
                  </div>
                ) : null}
              </>
            )}

            <Form.Item
              name="destinationCountryCode"
              label="到货国家/地区"
              rules={[{ required: true, message: '请输入到货国家/地区' }]}
            >
              <Select
                {...SEARCHABLE_SELECT_PROPS}
                options={countryOptions}
                placeholder="请选择到货国家/地区"
              />
            </Form.Item>
            <Form.Item name="destinationState" label="到货省州">
              <Input placeholder="例如 CA" />
            </Form.Item>
            <Form.Item name="destinationCity" label="到货城市">
              <Input placeholder="例如 Los Angeles" />
            </Form.Item>
            <Form.Item
              name="destinationPostalCode"
              label="到货邮编"
              rules={[{ required: true, message: '请输入到货邮编' }]}
            >
              <Input placeholder="例如 91144" />
            </Form.Item>
            <Form.Item name="destinationAddress1" label="收件地址1">
              <Input placeholder="街道地址" />
            </Form.Item>
            <Form.Item name="destinationAddress2" label="收件地址2">
              <Input placeholder="公寓、楼层、门牌等" />
            </Form.Item>
          </div>
          <div className={styles.conditionFooter}>
            <Button
              type="primary"
              block
              htmlType="submit"
              loading={calculateLoading}
              disabled={!canCalculate}
            >
              试算
            </Button>
          </div>
        </aside>

        <main className={
          isBuyerSimulation
            ? `${styles.workspace} ${styles.buyerWorkspace}`
            : effectivePackageInputMode === 'MANUAL'
              ? `${styles.workspace} ${styles.manualWorkspace}`
              : styles.workspace
        }
        >
          {isBuyerSimulation ? (
            <section className={styles.resolvePanel}>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>候选解析</h2>
                <span className={styles.muted}>
                  耗时: {resolveSummary?.resolveCostMs ?? '--'} ms
                </span>
              </div>
              <div className={styles.panelBody}>
                <div className={styles.resolveGrid}>
                  {resolveCards.map((item) => (
                    <div className={styles.resolveCard} key={item.label}>
                      <span>{item.label}</span>
                      <strong>{item.value ?? '--'}</strong>
                    </div>
                  ))}
                </div>
                <ProTable<API.Finance.FeeEstimateRouteCandidate>
                  rowKey={(record, index) => `${record.warehouseCode || ''}-${record.customerChannelCode || ''}-${record.systemChannelCode || ''}-${index}`}
                  columns={routeColumns}
                  dataSource={estimateResult?.routeCandidates || []}
                  search={false}
                  options={false}
                  pagination={false}
                  scroll={getProTableScroll(900, { y: 170 })}
                  locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                />
              </div>
            </section>
          ) : effectivePackageInputMode === 'SKU' ? (
            <section className={styles.packagePanel}>
              <div className={styles.panelHeader}>
                <h2 className={styles.panelTitle}>包裹信息</h2>
              </div>
              <div className={styles.panelBody}>
                {renderSkuSelector()}
                <ProTable<API.Finance.FeeEstimateSkuSnapshot>
                  rowKey="skuId"
                  columns={skuColumns}
                  dataSource={skuRows}
                  search={false}
                  options={false}
                  pagination={false}
                  scroll={getProTableScroll(900, { y: 180 })}
                  locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
                />
              </div>
            </section>
          ) : null}

          <section className={styles.resultPanel}>
            <div className={styles.panelHeader}>
              <h2 className={styles.panelTitle}>试算结果</h2>
              <span className={styles.muted}>
                RequestID: {estimateResult?.requestNo || '--'}
              </span>
            </div>
            <div className={styles.panelBody}>
              {estimateResult?.packageSummary ? (
                <div className={styles.summaryBar}>
                  <span className={styles.summaryItem}>
                    包裹尺寸
                    <strong className={styles.summaryValue}>
                      {estimateResult.packageSummary.sizeExpression || '--'}
                    </strong>
                  </span>
                  <span className={styles.summaryItem}>
                    实重
                    <strong className={styles.summaryValue}>
                      {formatNumber(estimateResult.packageSummary.actualWeightKg, ' kg')}
                    </strong>
                  </span>
                  <span className={styles.summaryItem}>
                    体积重
                    <strong className={styles.summaryValue}>
                      {formatNumber(estimateResult.packageSummary.volumeWeightKg, ' kg')}
                    </strong>
                  </span>
                  <span className={styles.summaryItem}>
                    计费重
                    <strong className={styles.summaryValue}>
                      {formatNumber(estimateResult.packageSummary.chargeableWeightKg, ' kg')}
                    </strong>
                  </span>
                </div>
              ) : null}
              <ProTable<API.Finance.FeeEstimateChannelResult>
                rowKey={(record) => record.traceId || `${record.warehouseCode || ''}-${record.systemChannelCode || ''}-${record.schemeChannelId || record.channelCode}`}
                columns={resultColumns}
                dataSource={estimateResult?.results || []}
                search={false}
                options={false}
                pagination={false}
                loading={calculateLoading}
                scroll={getProTableScroll(1800)}
                locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
              />
            </div>
          </section>
        </main>
      </Form>
      <Modal
        title="选择商品 SKU"
        open={skuSelectorOpen}
        width={1120}
        okText={`确认选择（${selectedSkuItems.length}）`}
        cancelText="取消"
        okButtonProps={{ disabled: !selectedSkuItems.length || !selectedCommonWarehouseCodes.length }}
        destroyOnClose
        onOk={applySelectedSkus}
        onCancel={() => setSkuSelectorOpen(false)}
      >
        <div className={styles.skuSelectionBoard}>
          <div className={styles.skuSelectionHeader}>
            <Typography.Text strong>已选择 SKU（{selectedSkuItems.length}）</Typography.Text>
            <Button type="link" size="small" disabled={!selectedSkuItems.length} onClick={clearSelectedSkus}>
              清空
            </Button>
          </div>
          {selectedSkuItems.length ? (
            <Space wrap size={[8, 8]}>
              {selectedSkuItems.map((item) => {
                const key = skuSelectionKey(item);
                return (
                  <Tag
                    key={key}
                    closable
                    className={styles.skuSelectionTag}
                    onClose={() => removeSelectedSku(key)}
                  >
                    {item.systemSkuCode || item.sellerSkuCode || item.masterSku || item.skuId} / {item.productName || item.productNameEn || '-'}
                  </Tag>
                );
              })}
            </Space>
          ) : (
            <Typography.Text type="secondary">
              商品数据量较大，请通过来源仓、SKU 或商品名称筛选后跨页勾选；确认后会回填到左侧商品清单。
            </Typography.Text>
          )}
          {selectedSkuItems.length ? (
            <div className={styles.skuCommonWarehouseLine}>
              <Typography.Text type={selectedCommonWarehouseCodes.length ? 'secondary' : 'danger'}>
                共同来源仓：{selectedCommonWarehouseCodes.length ? selectedCommonWarehouseCodes.join(', ') : '无'}
              </Typography.Text>
            </div>
          ) : null}
        </div>
        <ProTable<API.Finance.FeeEstimateSkuSnapshot>
          rowKey={(record) => skuSelectionKey(record)}
          columns={skuSelectorColumns}
          size="small"
          search={{ labelWidth: 70, span: 8, defaultCollapsed: false }}
          options={false}
          pagination={{ pageSize: 10 }}
          tableAlertRender={false}
          tableAlertOptionRender={false}
          rowSelection={{
            preserveSelectedRowKeys: true,
            selectedRowKeys: selectedSkuKeys,
            onSelect: (record, selected) => updateSelectedSku(record, selected),
            onSelectAll: (selected, _selectedRows, changeRows) => {
              changeRows.forEach((record) => updateSelectedSku(record, selected));
            },
            getCheckboxProps: (record) => {
              const selected = Boolean(selectedSkuMap[skuSelectionKey(record)]);
              return {
                disabled: !selected && !canMergeSkuBySourceWarehouse(selectedSkuItems, record),
              };
            },
          }}
          request={async (params) => {
            if (!canQuery) {
              return {
                data: [],
                total: 0,
                success: true,
              };
            }
            const resp = await getFeeEstimateSkus({
              sourceWarehouseCode: params.sourceWarehouseCode,
              skuCode: params.skuCode,
              productName: params.productName,
              pageNum: params.current,
              pageSize: params.pageSize,
            });
            return {
              data: resp.rows || [],
              total: resp.total || 0,
              success: resp.code === 200,
            };
          }}
          scroll={getProTableScroll(1280, { y: 330 })}
        />
      </Modal>
    </PageContainer>
  );
}
