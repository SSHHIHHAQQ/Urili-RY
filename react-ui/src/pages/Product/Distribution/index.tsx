import { DownOutlined, HistoryOutlined, PlusOutlined } from '@ant-design/icons';
import { PageContainer, type ActionType, type ProColumns, ProTable } from '@ant-design/pro-components';
import { history, useAccess } from '@umijs/max';
import {
  Alert,
  Button,
  Dropdown,
  Image,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { MenuProps } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { getAdminSellerList } from '@/services/seller/seller';
import { getCategoryList } from '@/services/product/product';
import {
  batchUpdateDistributionControlStatus,
  batchUpdateDistributionSkuSalePrices,
  batchUpdateDistributionStatus,
  getDistributionProduct,
  getDistributionProductList,
  getDistributionSkuList,
} from '@/services/product/distributionProduct';
import { message, modal } from '@/utils/feedback';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import { buildCategoryTree, toCategoryTreeSelectData } from '../categoryTree';
import {
  buildSkuDimensionText,
  buildSkuSpecText,
  formatPriceRange,
  getControlStatusText,
  getSalesStatusText,
  resolveResourceUrl,
  salesStatusTabOptions,
  type SalesStatusTabValue,
  salesStatusValueEnum,
  sourceTypeValueEnum,
} from './constants';
import ProductDetailDrawer from './components/ProductDetailDrawer';
import ProductDistributionOperationLogDrawer from './components/ProductDistributionOperationLogDrawer';
import styles from './style.module.css';

type ViewMode = 'SPU' | 'SKU';
type ControlModalState = {
  open: boolean;
  ownerType: ViewMode;
  ids: number[];
  targetStatus: 'NORMAL' | 'DISABLED';
};
type PriceMode = 'SUPPLY_MARKUP' | 'CURRENT_ADJUST' | 'FIXED';
type PriceAdjustDirection = 'UP' | 'DOWN';
type PriceNumberType = 'PERCENT' | 'AMOUNT';
type TailRule = 'NONE' | 'TAIL_99' | 'TAIL_90' | 'TAIL_09' | 'INTEGER';
type PricePreviewRow = API.ProductDistribution.Sku & {
  nextSalePrice?: number;
  priceError?: string;
};

const viewModeOptions = [
  { label: 'SPU视图', value: 'SPU' },
  { label: 'SKU视图', value: 'SKU' },
];

const salesStatusColor: Record<string, string> = {
  DRAFT: 'default',
  READY: 'warning',
  ON_SALE: 'success',
  OFF_SALE: 'processing',
};

const statusFlowMap: Record<string, { targetStatus: string; label: string; batchLabel: string }> = {
  DRAFT: { targetStatus: 'READY', label: '提交待上架', batchLabel: '提交待上架' },
  READY: { targetStatus: 'ON_SALE', label: '上架', batchLabel: '批量上架' },
  ON_SALE: { targetStatus: 'OFF_SALE', label: '下架', batchLabel: '批量下架' },
  OFF_SALE: { targetStatus: 'ON_SALE', label: '上架', batchLabel: '批量上架' },
};

const priceModeOptions = [
  { label: '按供货价加价', value: 'SUPPLY_MARKUP' },
  { label: '按当前售价调整', value: 'CURRENT_ADJUST' },
  { label: '统一设置售价', value: 'FIXED' },
];

const tailRuleOptions = [
  { label: '不处理尾数', value: 'NONE' },
  { label: '尾数 .99', value: 'TAIL_99' },
  { label: '尾数 .90', value: 'TAIL_90' },
  { label: '尾数 .09', value: 'TAIL_09' },
  { label: '取整数', value: 'INTEGER' },
];
const TABLE_SELECTION_COLUMN_WIDTH = 48;
const SPU_TABLE_SCROLL_X = 2480;
const SKU_TABLE_SCROLL_X = 2900;
const SKU_DETAIL_TABLE_SCROLL_X = 1680;

function resultOk(resp: API.Result, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function compactIds<T extends { spuId?: number; skuId?: number }>(rows: T[], ownerType: ViewMode) {
  const field = ownerType === 'SPU' ? 'spuId' : 'skuId';
  return rows
    .map((row) => row[field])
    .filter((value): value is number => typeof value === 'number' && Number.isFinite(value));
}

function renderSalesStatusTag(status?: string) {
  return <Tag color={salesStatusColor[status || '']}>{getSalesStatusText(status)}</Tag>;
}

function renderSpuControlStatusTag(record: Pick<API.ProductDistribution.Spu, 'controlStatus'>) {
  const status = record.controlStatus || 'NORMAL';
  return <Tag color={status === 'DISABLED' ? 'error' : 'success'}>{getControlStatusText(status)}</Tag>;
}

function renderSkuControlStatusTag(record: API.ProductDistribution.Sku) {
  if (record.spuControlStatus === 'DISABLED') {
    return <Tag color="error">SPU停用</Tag>;
  }
  return <Tag color={record.controlStatus === 'DISABLED' ? 'error' : 'success'}>
    {getControlStatusText(record.controlStatus || 'NORMAL')}
  </Tag>;
}

function formatAmount(value?: number | null) {
  if (value === undefined || value === null) return '--';
  return String(value);
}

function applyTailRule(value: number, rule: TailRule) {
  if (!Number.isFinite(value) || value < 0) return undefined;
  if (rule === 'INTEGER') return Number(Math.round(value).toFixed(2));
  if (rule === 'TAIL_99') return Number((Math.floor(value) + 0.99).toFixed(2));
  if (rule === 'TAIL_90') return Number((Math.floor(value) + 0.9).toFixed(2));
  if (rule === 'TAIL_09') return Number((Math.floor(value) + 0.09).toFixed(2));
  return Number(value.toFixed(2));
}

export default function ProductDistributionPage() {
  const access = useAccess();
  const actionRef = useRef<ActionType>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [current, setCurrent] = useState<API.ProductDistribution.Spu>();
  const [sellerOptions, setSellerOptions] = useState<{ label: string; value: number }[]>([]);
  const [categories, setCategories] = useState<API.Product.Category[]>([]);
  const [statusTab, setStatusTab] = useState<SalesStatusTabValue>('READY');
  const [viewMode, setViewMode] = useState<ViewMode>('SPU');
  const [selectedSpuRows, setSelectedSpuRows] = useState<API.ProductDistribution.Spu[]>([]);
  const [selectedSkuRows, setSelectedSkuRows] = useState<API.ProductDistribution.Sku[]>([]);
  const [controlReason, setControlReason] = useState('');
  const [controlModal, setControlModal] = useState<ControlModalState>({
    open: false,
    ownerType: 'SPU',
    ids: [],
    targetStatus: 'DISABLED',
  });
  const [priceRows, setPriceRows] = useState<API.ProductDistribution.Sku[]>([]);
  const [priceModalOpen, setPriceModalOpen] = useState(false);
  const [priceMode, setPriceMode] = useState<PriceMode>('SUPPLY_MARKUP');
  const [priceAdjustDirection, setPriceAdjustDirection] = useState<PriceAdjustDirection>('UP');
  const [priceNumberType, setPriceNumberType] = useState<PriceNumberType>('PERCENT');
  const [priceAmount, setPriceAmount] = useState<number>();
  const [tailRule, setTailRule] = useState<TailRule>('NONE');
  const [priceReason, setPriceReason] = useState('');
  const [operationLogOpen, setOperationLogOpen] = useState(false);

  const categoryTreeData = useMemo(
    () => toCategoryTreeSelectData(buildCategoryTree(categories)),
    [categories],
  );

  const selectedRows = viewMode === 'SPU' ? selectedSpuRows : selectedSkuRows;
  const selectedIds = compactIds(selectedRows, viewMode);
  const currentFlow = statusFlowMap[statusTab];

  const priceCurrencyText = useMemo(() => {
    const currencies = Array.from(new Set(priceRows.map((row) => row.currencyCode).filter(Boolean)));
    if (currencies.length === 0) return '金额';
    return currencies.length === 1 ? currencies[0] : '多币种';
  }, [priceRows]);

  useEffect(() => {
    getAdminSellerList({ pageNum: 1, pageSize: 100, status: '0' }).then((resp) => {
      setSellerOptions(
        (resp.rows || []).map((seller) => ({
          label: `${seller.sellerName || seller.sellerShortName || seller.sellerNo}（${seller.sellerNo || '-'}）`,
          value: seller.sellerId as number,
        })),
      );
    });
    getCategoryList({ status: '0' }).then((resp) => setCategories(resp.data || []));
  }, []);

  useEffect(() => {
    setSelectedSpuRows([]);
    setSelectedSkuRows([]);
  }, [statusTab, viewMode]);

  const reload = () => actionRef.current?.reload();

  const statusTabItems = useMemo(
    () => salesStatusTabOptions.map((item) => ({ key: item.value, label: item.label })),
    [],
  );

  const renderInventoryNumber = (value?: number | null) => (value === undefined || value === null ? '--' : value);

  const renderInventoryStatus = (record: { availableStock?: number | null; warehouseCount?: number | null; inventoryStatus?: string | null }) => {
    if (record.inventoryStatus) {
      return <Tag>{record.inventoryStatus}</Tag>;
    }
    if (record.availableStock == null && record.warehouseCount == null) {
      return '--';
    }
    return record.availableStock && record.availableStock > 0
      ? <Tag color="success">有库存</Tag>
      : <Tag>无库存</Tag>;
  };

  const openDetail = async (record: API.ProductDistribution.Spu) => {
    if (record.spuId == null) {
      return;
    }
    const resp = await getDistributionProduct(record.spuId);
    setCurrent(resp.data);
    setDetailOpen(true);
  };

  const openEdit = async (record: API.ProductDistribution.Spu) => {
    history.push(`/product/distribution/edit/${record.spuId}`);
  };

  const openSkuEdit = (record: API.ProductDistribution.Sku) => {
    if (record.spuId == null) {
      return;
    }
    history.push(`/product/distribution/edit/${record.spuId}?skuId=${record.skuId || ''}`);
  };

  const executeSalesStatus = (ownerType: ViewMode, ids: number[], targetStatus: string, label: string) => {
    if (ids.length === 0) {
      message.warning('请先选择商品');
      return;
    }
    const shouldAskSkuSync = ownerType === 'SPU' && ['ON_SALE', 'OFF_SALE'].includes(targetStatus);
    let syncSkuStatus = true;
    const skuSyncActionText = targetStatus === 'OFF_SALE' ? '下架' : '上架';
    modal.confirm({
      title: label,
      content: shouldAskSkuSync ? (
        <Space orientation="vertical" size={12}>
          <div>{`确认对 ${ids.length} 个 SPU 执行“${label}”？`}</div>
          <Alert
            type="info"
            showIcon
            title="SKU同步处理"
            description={`可以同时${skuSyncActionText}符合当前状态流转条件的 SKU，也可以只调整 SPU 状态。`}
          />
          <Radio.Group
            defaultValue="WITH_SKU"
            onChange={(event) => {
              syncSkuStatus = event.target.value === 'WITH_SKU';
            }}
          >
            <Space orientation="vertical">
              <Radio value="WITH_SKU">{`同时${skuSyncActionText}可处理 SKU`}</Radio>
              <Radio value="SPU_ONLY">仅调整 SPU</Radio>
            </Space>
          </Radio.Group>
        </Space>
      ) : `确认对 ${ids.length} 个${ownerType}执行“${label}”？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const ok = resultOk(
          await batchUpdateDistributionStatus(
            ownerType,
            ids,
            targetStatus,
            shouldAskSkuSync ? syncSkuStatus : undefined,
          ),
          '状态已更新',
        );
        if (ok) reload();
      },
    });
  };

  const openControlModal = (
    ownerType: ViewMode,
    ids: number[],
    targetStatus: 'NORMAL' | 'DISABLED',
  ) => {
    if (ids.length === 0) {
      message.warning('请先选择商品');
      return;
    }
    setControlReason('');
    setControlModal({ open: true, ownerType, ids, targetStatus });
  };

  const submitControlStatus = async () => {
    if (controlModal.targetStatus === 'DISABLED' && !controlReason.trim()) {
      message.warning('请输入停用原因');
      return;
    }
    const ok = resultOk(
      await batchUpdateDistributionControlStatus(
        controlModal.ownerType,
        controlModal.ids,
        controlModal.targetStatus,
        controlReason.trim(),
      ),
      controlModal.targetStatus === 'DISABLED' ? '商品已停用' : '商品已恢复',
    );
    if (ok) {
      setControlModal({ ...controlModal, open: false });
      reload();
    }
  };

  const buildStatusQuery = (activeStatusTab?: SalesStatusTabValue) => {
    if (!activeStatusTab || activeStatusTab === 'ALL') return {};
    if (activeStatusTab === 'DISABLED') return { controlStatus: 'DISABLED' };
    return { spuStatus: activeStatusTab, controlStatus: 'NORMAL' };
  };

  const buildSkuStatusQuery = (activeStatusTab?: SalesStatusTabValue) => {
    if (!activeStatusTab || activeStatusTab === 'ALL') return {};
    if (activeStatusTab === 'DISABLED') return { controlStatus: 'DISABLED' };
    return { skuStatus: activeStatusTab, controlStatus: 'NORMAL' };
  };

  const calculatePreviewPrice = (record: API.ProductDistribution.Sku) => {
    if (priceAmount === undefined || priceAmount === null) return undefined;
    if (priceMode === 'FIXED') return applyTailRule(priceAmount, tailRule);

    const baseValue = priceMode === 'SUPPLY_MARKUP' ? record.supplyPrice : record.salePrice;
    if (baseValue === undefined || baseValue === null) return undefined;
    const base = Number(baseValue);
    if (!Number.isFinite(base)) return undefined;

    const sign = priceMode === 'CURRENT_ADJUST' && priceAdjustDirection === 'DOWN' ? -1 : 1;
    const next = priceNumberType === 'PERCENT'
      ? base * (1 + sign * priceAmount / 100)
      : base + sign * priceAmount;
    return applyTailRule(next, tailRule);
  };

  const pricePreviewRows = useMemo<PricePreviewRow[]>(
    () => priceRows.map((row) => {
      const nextSalePrice = calculatePreviewPrice(row);
      let priceError: string | undefined;
      if (nextSalePrice === undefined) {
        priceError = priceMode === 'CURRENT_ADJUST' && (row.salePrice === undefined || row.salePrice === null)
          ? '当前售价为空'
          : '待输入';
      } else if (nextSalePrice < 0) {
        priceError = '价格不能小于0';
      }
      return { ...row, nextSalePrice, priceError };
    }),
    [priceRows, priceMode, priceAdjustDirection, priceNumberType, priceAmount, tailRule],
  );

  const openPriceModal = (rows: API.ProductDistribution.Sku[]) => {
    if (rows.length === 0) {
      message.warning('请先选择 SKU');
      return;
    }
    setPriceRows(rows);
    setPriceReason('');
    setPriceModalOpen(true);
  };

  const submitSalePrice = async () => {
    if (priceAmount === undefined || priceAmount === null) {
      message.warning('请输入调价值');
      return;
    }
    const invalid = pricePreviewRows.find((row) => row.nextSalePrice === undefined || row.nextSalePrice < 0);
    if (invalid) {
      message.warning('存在无法计算的新售价，请检查调价方式和选中的 SKU');
      return;
    }
    const belowSupply = pricePreviewRows.some((row) =>
      row.nextSalePrice !== undefined
      && row.supplyPrice !== undefined
      && row.supplyPrice !== null
      && row.nextSalePrice < Number(row.supplyPrice));
    const items = pricePreviewRows
      .filter((row) => row.skuId && row.nextSalePrice !== undefined)
      .map((row) => ({ skuId: row.skuId!, salePrice: row.nextSalePrice! }));
    const run = async () => {
      const ok = resultOk(
        await batchUpdateDistributionSkuSalePrices(items, priceReason.trim()),
        '销售价已更新',
      );
      if (ok) {
        setPriceModalOpen(false);
        setPriceRows([]);
        reload();
      }
    };
    if (belowSupply) {
      modal.confirm({
        title: '存在低于供货价的售价',
        content: '低于供货价可能导致亏损或审核风险，确认继续保存本次调价？',
        okText: '确认保存',
        cancelText: '返回修改',
        okButtonProps: { danger: true },
        onOk: run,
      });
      return;
    }
    await run();
  };

  const skuColumns = (
    siblingRows: API.ProductDistribution.Sku[] = [],
  ): ColumnsType<API.ProductDistribution.Sku> => [
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      width: 72,
      render: (url) =>
        url ? <Image width={40} height={40} src={resolveResourceUrl(url)} style={{ objectFit: 'cover' }} /> : '--',
    },
    { title: '系统SKU', dataIndex: 'systemSkuCode', width: 150 },
    { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 140 },
    {
      title: 'SKU规格',
      width: 220,
      render: (_, record) => buildSkuSpecText(record, siblingRows) || '--',
    },
    {
      title: '尺寸重量',
      width: 220,
      render: (_, record) => buildSkuDimensionText(record) || '--',
    },
    { title: '供货价', dataIndex: 'supplyPrice', width: 90, render: (value) => formatAmount(value as number) },
    { title: '销售价', dataIndex: 'salePrice', width: 90, render: (value) => formatAmount(value as number) },
    { title: '币种', dataIndex: 'currencyCode', width: 80 },
    {
      title: '可售库存',
      dataIndex: 'availableStock',
      width: 90,
      render: (_, record) => renderInventoryNumber(record.availableStock),
    },
    {
      title: '仓库数',
      dataIndex: 'warehouseCount',
      width: 80,
      render: (_, record) => renderInventoryNumber(record.warehouseCount),
    },
    {
      title: '库存状态',
      width: 90,
      render: (_, record) => renderInventoryStatus(record),
    },
    {
      title: '销售状态',
      dataIndex: 'skuStatus',
      width: 100,
      render: (value) => renderSalesStatusTag(String(value || '')),
    },
    {
      title: '管控',
      dataIndex: 'controlStatus',
      width: 90,
      render: (_, record) => renderSkuControlStatusTag(record),
    },
    {
      title: '操作',
      width: 130,
      render: (_, record) => {
        const flow = statusFlowMap[record.skuStatus || ''];
        const parentDisabled = record.spuControlStatus === 'DISABLED';
        const skuDisabled = record.controlStatus === 'DISABLED';
        const items: MenuProps['items'] = [
          ...(access.hasPerms('product:distribution:price')
            ? [{ key: 'price', label: '调价' }]
            : []),
          ...(flow && !parentDisabled && !skuDisabled
            ? [{ key: `status:${flow.targetStatus}`, label: flow.label }]
            : []),
          ...(!parentDisabled && !skuDisabled
            ? [{ key: 'disable', label: '停用', danger: true }]
            : []),
          ...(skuDisabled
            ? [{ key: 'recover', label: '恢复' }]
            : []),
          ...(parentDisabled && !skuDisabled
            ? [{ key: 'parent-disabled', label: 'SPU已停用', disabled: true }]
            : []),
        ];
        return (
          <Dropdown
            menu={{
              items,
              onClick: ({ key }) => {
                if (key === 'disable') {
                  openControlModal('SKU', compactIds([record], 'SKU'), 'DISABLED');
                } else if (key === 'recover') {
                  openControlModal('SKU', compactIds([record], 'SKU'), 'NORMAL');
                } else if (String(key).startsWith('status:')) {
                  executeSalesStatus('SKU', compactIds([record], 'SKU'), String(key).slice(7), flow?.label || '调整状态');
                }
              },
            }}
          >
            <Button type="link" size="small" hidden={!access.hasPerms('product:distribution:status')}>
              状态 <DownOutlined />
            </Button>
          </Dropdown>
        );
      },
    },
  ];

  const columns: ProColumns<API.ProductDistribution.Spu>[] = [
    Table.SELECTION_COLUMN as ProColumns<API.ProductDistribution.Spu>,
    Table.EXPAND_COLUMN as ProColumns<API.ProductDistribution.Spu>,
    {
      title: '商品图',
      dataIndex: 'mainImageUrl',
      search: false,
      width: 72,
      render: (_, record) =>
        record.mainImageUrl ? (
          <Image width={44} height={44} src={resolveResourceUrl(record.mainImageUrl)} style={{ objectFit: 'cover' }} />
        ) : '--',
    },
    { title: '系统SPU', dataIndex: 'systemSpuCode', width: 160 },
    { title: '客户SPU', dataIndex: 'sellerSpuCode', width: 150 },
    {
      title: '商品标题',
      dataIndex: 'productName',
      width: 260,
      render: (_, record) => (
        <div>
          <div>{record.productName || '--'}</div>
          <div className={styles.mutedText}>{record.productNameEn || '--'}</div>
        </div>
      ),
    },
    {
      title: '卖家',
      dataIndex: 'sellerId',
      valueType: 'select',
      fieldProps: { ...SEARCHABLE_SELECT_PROPS, options: sellerOptions },
      render: (_, record) => record.sellerName || '--',
      width: 180,
    },
    {
      title: '类目',
      dataIndex: 'categoryId',
      valueType: 'treeSelect',
      fieldProps: { ...SEARCHABLE_TREE_SELECT_PROPS, treeData: categoryTreeData, treeDefaultExpandAll: true },
      render: (_, record) => record.categoryName || '--',
      width: 160,
    },
    { title: 'SKU数', dataIndex: 'skuCount', search: false, width: 80 },
    {
      title: '供货价区间',
      search: false,
      width: 130,
      render: (_, record) => formatPriceRange(record.supplyPriceMin, record.supplyPriceMax),
    },
    {
      title: '销售价区间',
      search: false,
      width: 130,
      render: (_, record) => formatPriceRange(record.salePriceMin, record.salePriceMax),
    },
    { title: '币种', dataIndex: 'currencySummary', search: false, width: 90 },
    {
      title: '总可售库存',
      dataIndex: 'availableStock',
      search: false,
      width: 100,
      render: (_, record) => renderInventoryNumber(record.availableStock),
    },
    {
      title: '仓库数',
      dataIndex: 'warehouseCount',
      search: false,
      width: 80,
      render: (_, record) => renderInventoryNumber(record.warehouseCount),
    },
    {
      title: '库存状态',
      search: false,
      width: 90,
      render: (_, record) => renderInventoryStatus(record),
    },
    {
      title: '销售状态',
      dataIndex: 'spuStatus',
      search: false,
      valueEnum: salesStatusValueEnum,
      width: 100,
      render: (_, record) => renderSalesStatusTag(record.spuStatus),
    },
    {
      title: '管控',
      dataIndex: 'controlStatus',
      search: false,
      width: 90,
      render: (_, record) => renderSpuControlStatusTag(record),
    },
    {
      title: '来源',
      dataIndex: 'sourceType',
      valueEnum: sourceTypeValueEnum,
      search: false,
      width: 140,
    },
    { title: '更新时间', dataIndex: 'updateTime', search: false, width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 190,
      fixed: 'right',
      render: (_, record) => {
        const flow = statusFlowMap[record.spuStatus || ''];
        const disabled = record.controlStatus === 'DISABLED';
        const items: MenuProps['items'] = [
          ...(flow && !disabled ? [{ key: `status:${flow.targetStatus}`, label: flow.label }] : []),
          ...(!disabled ? [{ key: 'disable', label: '停用', danger: true }] : []),
          ...(disabled ? [{ key: 'recover', label: '恢复' }] : []),
        ];
        return [
          <Button key="view" type="link" size="small" onClick={() => openDetail(record)}>
            查看
          </Button>,
          <Button
            key="edit"
            type="link"
            size="small"
            hidden={!access.hasPerms('product:distribution:edit')}
            onClick={() => openEdit(record)}
          >
            编辑
          </Button>,
          <Dropdown
            key="more"
            menu={{
              items,
              onClick: ({ key }) => {
                if (key === 'disable') {
                  openControlModal('SPU', compactIds([record], 'SPU'), 'DISABLED');
                } else if (key === 'recover') {
                  openControlModal('SPU', compactIds([record], 'SPU'), 'NORMAL');
                } else if (String(key).startsWith('status:')) {
                  executeSalesStatus('SPU', compactIds([record], 'SPU'), String(key).slice(7), flow?.label || '调整状态');
                }
              },
            }}
          >
            <Button type="link" size="small" hidden={!access.hasPerms('product:distribution:status')}>
              更多 <DownOutlined />
            </Button>
          </Dropdown>,
        ];
      },
    },
  ];

  const skuListColumns: ProColumns<API.ProductDistribution.Sku>[] = [
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      search: false,
      width: 72,
      render: (_, record) =>
        record.skuImageUrl ? (
          <Image width={44} height={44} src={resolveResourceUrl(record.skuImageUrl)} style={{ objectFit: 'cover' }} />
        ) : '--',
    },
    { title: '系统SKU', dataIndex: 'systemSkuCode', width: 160 },
    { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 150 },
    {
      title: 'SKU规格',
      search: false,
      width: 220,
      render: (_, record) => buildSkuSpecText(record) || '--',
    },
    {
      title: '商品标题',
      dataIndex: 'productName',
      width: 260,
      render: (_, record) => (
        <div>
          <div>{record.productName || '--'}</div>
          <div className={styles.mutedText}>{record.productNameEn || '--'}</div>
        </div>
      ),
    },
    { title: '系统SPU', dataIndex: 'systemSpuCode', width: 160 },
    { title: '客户SPU', dataIndex: 'sellerSpuCode', width: 150 },
    {
      title: '卖家',
      dataIndex: 'sellerId',
      valueType: 'select',
      fieldProps: { ...SEARCHABLE_SELECT_PROPS, options: sellerOptions },
      render: (_, record) => record.sellerName || '--',
      width: 180,
    },
    {
      title: '类目',
      dataIndex: 'categoryId',
      valueType: 'treeSelect',
      fieldProps: { ...SEARCHABLE_TREE_SELECT_PROPS, treeData: categoryTreeData, treeDefaultExpandAll: true },
      render: (_, record) => record.categoryName || '--',
      width: 160,
    },
    {
      title: '尺寸重量',
      search: false,
      width: 220,
      render: (_, record) => buildSkuDimensionText(record) || '--',
    },
    { title: '供货价', dataIndex: 'supplyPrice', search: false, width: 90, render: (value) => formatAmount(value as number) },
    { title: '销售价', dataIndex: 'salePrice', search: false, width: 90, render: (value) => formatAmount(value as number) },
    { title: '币种', dataIndex: 'currencyCode', search: false, width: 80 },
    {
      title: '可售库存',
      dataIndex: 'availableStock',
      search: false,
      width: 90,
      render: (_, record) => renderInventoryNumber(record.availableStock),
    },
    {
      title: '仓库数',
      dataIndex: 'warehouseCount',
      search: false,
      width: 80,
      render: (_, record) => renderInventoryNumber(record.warehouseCount),
    },
    {
      title: '库存状态',
      search: false,
      width: 90,
      render: (_, record) => renderInventoryStatus(record),
    },
    {
      title: '销售状态',
      dataIndex: 'skuStatus',
      search: false,
      valueEnum: salesStatusValueEnum,
      width: 100,
      render: (_, record) => renderSalesStatusTag(record.skuStatus),
    },
    {
      title: '管控',
      dataIndex: 'controlStatus',
      search: false,
      width: 90,
      render: (_, record) => renderSkuControlStatusTag(record),
    },
    { title: '更新时间', dataIndex: 'updateTime', search: false, width: 170 },
    {
      title: '操作',
      valueType: 'option',
      width: 210,
      fixed: 'right',
      render: (_, record) => {
        const flow = statusFlowMap[record.skuStatus || ''];
        const parentDisabled = record.spuControlStatus === 'DISABLED';
        const skuDisabled = record.controlStatus === 'DISABLED';
        const items: MenuProps['items'] = [
          ...(access.hasPerms('product:distribution:price')
            ? [{ key: 'price', label: '调价' }]
            : []),
          ...(flow && !parentDisabled && !skuDisabled
            ? [{ key: `status:${flow.targetStatus}`, label: flow.label }]
            : []),
          ...(!parentDisabled && !skuDisabled
            ? [{ key: 'disable', label: '停用', danger: true }]
            : []),
          ...(skuDisabled ? [{ key: 'recover', label: '恢复' }] : []),
          ...(parentDisabled && !skuDisabled
            ? [{ key: 'parent-disabled', label: 'SPU已停用', disabled: true }]
            : []),
        ];
        return [
          <Button key="view" type="link" size="small" onClick={() => openDetail({ spuId: record.spuId })}>
            查看SPU
          </Button>,
          <Button
            key="edit"
            type="link"
            size="small"
            hidden={!access.hasPerms('product:distribution:edit')}
            onClick={() => openSkuEdit(record)}
          >
            编辑商品
          </Button>,
          <Dropdown
            key="status"
            menu={{
              items,
              onClick: ({ key }) => {
                if (key === 'price') {
                  openPriceModal([record]);
                } else if (key === 'disable') {
                  openControlModal('SKU', compactIds([record], 'SKU'), 'DISABLED');
                } else if (key === 'recover') {
                  openControlModal('SKU', compactIds([record], 'SKU'), 'NORMAL');
                } else if (String(key).startsWith('status:')) {
                  executeSalesStatus('SKU', compactIds([record], 'SKU'), String(key).slice(7), flow?.label || '调整状态');
                }
              },
            }}
          >
            <Button type="link" size="small" hidden={!access.hasPerms('product:distribution:status')}>
              更多 <DownOutlined />
            </Button>
          </Dropdown>,
        ];
      },
    },
  ];

  const viewModeSwitch = (
    <Radio.Group
      key="view-mode"
      buttonStyle="solid"
      value={viewMode}
      onChange={(event) => setViewMode(event.target.value as ViewMode)}
    >
      {viewModeOptions.map((option) => (
        <Radio.Button key={option.value} value={option.value}>
          {option.label}
        </Radio.Button>
      ))}
    </Radio.Group>
  );

  const headerTitle = (
    <Tabs
      activeKey={statusTab}
      items={statusTabItems}
      tabBarStyle={{ marginBottom: 0 }}
      onChange={(key) => setStatusTab(key as SalesStatusTabValue)}
    />
  );

  const renderBatchActions = () => {
    const actions: ReactNode[] = [viewModeSwitch];
    if (statusTab === 'DISABLED') {
      actions.push(
        <Button
          key="recover"
          disabled={!selectedIds.length}
          hidden={!access.hasPerms('product:distribution:status')}
          onClick={() => openControlModal(viewMode, selectedIds, 'NORMAL')}
        >
          恢复
        </Button>,
      );
    } else if (currentFlow) {
      actions.push(
        <Button
          key="flow"
          disabled={!selectedIds.length}
          hidden={!access.hasPerms('product:distribution:status')}
          onClick={() => executeSalesStatus(viewMode, selectedIds, currentFlow.targetStatus, currentFlow.batchLabel)}
        >
          {currentFlow.batchLabel}
        </Button>,
      );
      actions.push(
        <Button
          key="disable"
          danger
          disabled={!selectedIds.length}
          hidden={!access.hasPerms('product:distribution:status')}
          onClick={() => openControlModal(viewMode, selectedIds, 'DISABLED')}
        >
          批量停用
        </Button>,
      );
    }
    if (viewMode === 'SKU') {
      actions.push(
        <Button
          key="price"
          disabled={!selectedSkuRows.length}
          hidden={!access.hasPerms('product:distribution:price')}
          onClick={() => openPriceModal(selectedSkuRows)}
        >
          调整售价
        </Button>,
      );
    }
    actions.push(
      <Button
        key="log"
        icon={<HistoryOutlined />}
        hidden={!access.hasPerms('product:distribution:log')}
        onClick={() => setOperationLogOpen(true)}
      >
        操作日志
      </Button>,
    );
    actions.push(
      <Button
        key="add"
        type="primary"
        icon={<PlusOutlined />}
        hidden={!access.hasPerms('product:distribution:add')}
        onClick={() => history.push('/product/distribution/create')}
      >
        新增商品
      </Button>,
    );
    return actions;
  };

  return (
    <PageContainer title={false}>
      {viewMode === 'SPU' ? (
        <ProTable<API.ProductDistribution.Spu>
          key="spu-view"
          actionRef={actionRef}
          rowKey="spuId"
          columns={columns}
          scroll={getProTableScroll(SPU_TABLE_SCROLL_X)}
          tableLayout="fixed"
          search={getPersistedProTableSearch({ labelWidth: 90 }, 'product-distribution-spu')}
          params={{ statusTab }}
          headerTitle={headerTitle}
          rowSelection={{
            columnWidth: TABLE_SELECTION_COLUMN_WIDTH,
            selectedRowKeys: selectedSpuRows.map((row) => row.spuId).filter(Boolean) as number[],
            onChange: (_, rows) => setSelectedSpuRows(rows),
          }}
          expandable={{
            expandedRowRender: (record) => (
              <Table
                rowKey="skuId"
                size="small"
                columns={skuColumns(record.skus || [])}
                dataSource={record.skus || []}
                pagination={false}
                scroll={{ x: SKU_DETAIL_TABLE_SCROLL_X }}
                tableLayout="fixed"
              />
            ),
          }}
          request={async ({ current, pageSize, ...params }) => {
            const { statusTab: activeStatusTab, ...queryParams } = params;
            delete (queryParams as Record<string, unknown>).spuStatus;
            delete (queryParams as Record<string, unknown>).controlStatus;
            const resp = await getDistributionProductList({
              ...queryParams,
              ...buildStatusQuery(activeStatusTab as SalesStatusTabValue),
              pageNum: current,
              pageSize,
            });
            return {
              data: resp.rows || [],
              total: resp.total || 0,
              success: resp.code === 200,
            };
          }}
          toolBarRender={renderBatchActions}
        />
      ) : (
        <ProTable<API.ProductDistribution.Sku>
          key="sku-view"
          actionRef={actionRef}
          rowKey="skuId"
          columns={skuListColumns}
          scroll={getProTableScroll(SKU_TABLE_SCROLL_X)}
          tableLayout="fixed"
          search={getPersistedProTableSearch({ labelWidth: 90 }, 'product-distribution-sku')}
          params={{ statusTab }}
          headerTitle={headerTitle}
          rowSelection={{
            columnWidth: TABLE_SELECTION_COLUMN_WIDTH,
            selectedRowKeys: selectedSkuRows.map((row) => row.skuId).filter(Boolean) as number[],
            onChange: (_, rows) => setSelectedSkuRows(rows),
          }}
          request={async ({ current, pageSize, ...params }) => {
            const { statusTab: activeStatusTab, ...queryParams } = params;
            delete (queryParams as Record<string, unknown>).skuStatus;
            delete (queryParams as Record<string, unknown>).controlStatus;
            const resp = await getDistributionSkuList({
              ...queryParams,
              ...buildSkuStatusQuery(activeStatusTab as SalesStatusTabValue),
              pageNum: current,
              pageSize,
            });
            return {
              data: resp.rows || [],
              total: resp.total || 0,
              success: resp.code === 200,
            };
          }}
          toolBarRender={renderBatchActions}
        />
      )}

      <ProductDetailDrawer
        open={detailOpen}
        product={current}
        onClose={() => setDetailOpen(false)}
      />
      <ProductDistributionOperationLogDrawer
        open={operationLogOpen}
        onOpenChange={setOperationLogOpen}
      />

      <Modal
        title={controlModal.targetStatus === 'DISABLED' ? '停用商品' : '恢复商品'}
        open={controlModal.open}
        okText={controlModal.targetStatus === 'DISABLED' ? '确认停用' : '确认恢复'}
        cancelText="取消"
        okButtonProps={{ danger: controlModal.targetStatus === 'DISABLED' }}
        onOk={submitControlStatus}
        onCancel={() => setControlModal({ ...controlModal, open: false })}
      >
        {controlModal.targetStatus === 'DISABLED' ? (
          <Input.TextArea
            rows={4}
            value={controlReason}
            maxLength={500}
            showCount
            placeholder="请输入停用原因"
            onChange={(event) => setControlReason(event.target.value)}
          />
        ) : (
          <Alert
            type="info"
            showIcon
            title={`确认恢复 ${controlModal.ids.length} 个${controlModal.ownerType}？恢复后仍保持原销售状态。`}
          />
        )}
      </Modal>

      <Modal
        title="调整售价"
        open={priceModalOpen}
        width={1040}
        okText="保存调价"
        cancelText="取消"
        onOk={submitSalePrice}
        onCancel={() => setPriceModalOpen(false)}
      >
        <Space orientation="vertical" size={14} style={{ width: '100%' }}>
          <Radio.Group
            buttonStyle="solid"
            value={priceMode}
            options={priceModeOptions}
            onChange={(event) => setPriceMode(event.target.value)}
          />
          <Space wrap>
            {priceMode === 'CURRENT_ADJUST' ? (
              <Select
                value={priceAdjustDirection}
                style={{ width: 96 }}
                options={[
                  { label: '上调', value: 'UP' },
                  { label: '下调', value: 'DOWN' },
                ]}
                onChange={(value) => setPriceAdjustDirection(value as PriceAdjustDirection)}
              />
            ) : null}
            {priceMode !== 'FIXED' ? (
              <Select
                value={priceNumberType}
                style={{ width: 110 }}
                options={[
                  { label: '百分比', value: 'PERCENT' },
                  { label: '金额', value: 'AMOUNT' },
                ]}
                onChange={(value) => setPriceNumberType(value as PriceNumberType)}
              />
            ) : null}
            <InputNumber
              min={0}
              precision={4}
              value={priceAmount}
              style={{ width: 180 }}
              suffix={priceMode === 'FIXED' || priceNumberType === 'AMOUNT' ? priceCurrencyText : '%'}
              placeholder={priceMode === 'FIXED' ? '输入统一售价' : '输入调价值'}
              onChange={(value) => setPriceAmount(value === null ? undefined : Number(value))}
            />
            <Select
              value={tailRule}
              style={{ width: 140 }}
              options={tailRuleOptions}
              onChange={(value) => setTailRule(value as TailRule)}
            />
            <Input
              value={priceReason}
              style={{ width: 260 }}
              placeholder="调价原因（可选）"
              onChange={(event) => setPriceReason(event.target.value)}
            />
          </Space>
          {priceCurrencyText === '多币种' ? (
            <Alert type="warning" showIcon title="当前选择包含多币种 SKU，请确认调价规则对所有币种都适用。" />
          ) : null}
          <Table<PricePreviewRow>
            rowKey="skuId"
            size="small"
            pagination={false}
            dataSource={pricePreviewRows}
            scroll={{ x: 980, y: 320 }}
            columns={[
              { title: '系统SKU', dataIndex: 'systemSkuCode', width: 160 },
              { title: '客户SKU', dataIndex: 'sellerSkuCode', width: 140 },
              { title: '供货价', dataIndex: 'supplyPrice', width: 100, render: (value) => formatAmount(value as number) },
              { title: '原销售价', dataIndex: 'salePrice', width: 100, render: (value) => formatAmount(value as number) },
              {
                title: '新销售价',
                dataIndex: 'nextSalePrice',
                width: 120,
                render: (_, record) => record.priceError ? (
                  <Typography.Text type="secondary">{record.priceError}</Typography.Text>
                ) : (
                  <Typography.Text
                    type={
                      record.nextSalePrice !== undefined
                      && record.supplyPrice !== undefined
                      && record.nextSalePrice < Number(record.supplyPrice)
                        ? 'warning'
                        : undefined
                    }
                  >
                    {formatAmount(record.nextSalePrice)}
                  </Typography.Text>
                ),
              },
              { title: '币种', dataIndex: 'currencyCode', width: 90 },
              {
                title: '规格',
                width: 220,
                render: (_, record) => buildSkuSpecText(record) || '--',
              },
            ]}
          />
        </Space>
      </Modal>
    </PageContainer>
  );
}
