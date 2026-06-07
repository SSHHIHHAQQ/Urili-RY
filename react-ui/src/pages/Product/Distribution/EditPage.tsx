import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { PageContainer, ProTable, type ProColumns } from '@ant-design/pro-components';
import { history, useAccess, useParams } from '@umijs/max';
import { Affix, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Radio, Select, Space, TreeSelect, Typography } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { getCategoryList, getCategorySchema } from '@/services/product/product';
import {
  addDistributionProduct,
  getDistributionProduct,
  updateDistributionProduct,
} from '@/services/product/distributionProduct';
import { getSourceProductList } from '@/services/integration/sourceProduct';
import { getAdminSellerList } from '@/services/seller/seller';
import {
  getOfficialWarehouseList,
  getThirdPartyWarehouseList,
} from '@/services/warehouse/warehouse';
import { message } from '@/utils/feedback';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import { buildCategoryTree } from '../categoryTree';
import { yesNoOptions } from '../constants';
import DetailContentBuilder from './components/DetailContentBuilder';
import ProductImageSection from './components/ProductImageSection';
import SkuMatrixEditor from './components/SkuMatrixEditor';
import {
  parseDetailContent,
  serializeDetailContent,
  type DetailContentBlock,
} from './detailContent';
import styles from './style.module.css';

type ProductEditValues = API.ProductDistribution.Spu & {
  attributeValueMap?: Record<string, any>;
};

const ATTRIBUTE_DATE_FORMAT = 'YYYY-MM-DD';

type WarehouseOption = {
  label: string;
  value: number;
  currencyCode: string;
  currencyLabel: string;
  warehouseKind: string;
  warehouseKindLabel: string;
};

const warehouseKindLabels: Record<string, string> = {
  official: '官方仓',
  third_party: '三方仓',
};

function parseAttributeJsonArray(value?: string) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function valueFromAttribute(item: API.ProductDistribution.AttributeValue) {
  if (item.attributeType === 'MULTI_SELECT') {
    return parseAttributeJsonArray(item.valueJson);
  }
  if (item.attributeType === 'DATE' && item.valueDate) {
    const value = dayjs(item.valueDate);
    return value.isValid() ? value : undefined;
  }
  return item.valueText ?? item.valueCode ?? item.valueNumber ?? item.valueDate ?? item.valueJson;
}

function toPublishCategoryTreeData(categories: API.Product.Category[]): any[] {
  return categories.map((item) => {
    const children = item.children?.length ? toPublishCategoryTreeData(item.children) : undefined;
    return {
      title: item.categoryName,
      value: item.categoryId,
      disabled: !!children?.length || item.publishEnabled !== 'Y',
      ...(children ? { children } : {}),
    };
  });
}

function stripSkuRows(rows: (API.ProductDistribution.Sku & { rowKey?: string })[]) {
  return rows.map(({ rowKey: _rowKey, ...row }) => row);
}

function formatNumber(value?: number, digits = 2) {
  return typeof value === 'number' && Number.isFinite(value) ? value.toFixed(digits) : undefined;
}

function hasWmsMeasurement(item: API.Integration.SourceProductItem) {
  return [item.wmsLength, item.wmsWidth, item.wmsHeight, item.wmsWeight]
    .every((value) => typeof value === 'number' && Number.isFinite(value));
}

function pickSourceMeasurement(item: API.Integration.SourceProductItem) {
  const useWms = hasWmsMeasurement(item);
  return {
    length: useWms ? item.wmsLength : item.length,
    width: useWms ? item.wmsWidth : item.width,
    height: useWms ? item.wmsHeight : item.height,
    weight: useWms ? item.wmsWeight : item.weight,
    source: useWms ? 'WMS' : 'PRODUCT',
  };
}

function formatSourceDimension(item: API.Integration.SourceProductItem) {
  const measurement = pickSourceMeasurement(item);
  const length = formatNumber(measurement.length);
  const width = formatNumber(measurement.width);
  const height = formatNumber(measurement.height);
  const weight = formatNumber(measurement.weight);
  if (!length || !width || !height || !weight) return '-';
  return `${length} x ${width} x ${height} cm  ${weight} kg`;
}

function toMeasurementText(value: number | undefined, unit: string) {
  const text = formatNumber(value);
  return text ? `${text} ${unit}` : undefined;
}

function hasSkuContent(row?: API.ProductDistribution.Sku & { rowKey?: string }) {
  if (!row) return false;
  return !!(
    row.skuId
    || row.sourceDimensionGroupKey
    || row.sellerSkuCode
    || row.color
    || row.size
    || row.supplyPrice !== undefined
  );
}

function toSourceSkuRow(
  item: API.Integration.SourceProductItem,
  sortOrder: number,
): API.ProductDistribution.Sku & { rowKey?: string } {
  const measurement = pickSourceMeasurement(item);
  return {
    rowKey: `source-${item.sourceDimensionGroupKey || item.sourceSkuGroupKey}-${Date.now()}`,
    skuStatus: 'DRAFT',
    sortOrder,
    sourceScope: 'OFFICIAL_MASTER',
    sourceSkuGroupKey: item.sourceSkuGroupKey,
    sourceDimensionGroupKey: item.sourceDimensionGroupKey,
    masterSku: item.masterSku,
    masterProductNameSnapshot: item.masterProductName,
    sourcePayloadHash: item.sourcePayloadHash,
    measureLengthCm: measurement.length,
    measureWidthCm: measurement.width,
    measureHeightCm: measurement.height,
    measureWeightKg: measurement.weight,
    measureSource: measurement.source,
    sourceWarehouseNames: item.sourceWarehouseNames,
    sourceWarehouseCount: item.warehouseCount,
    lengthValue: toMeasurementText(measurement.length, 'cm'),
    widthValue: toMeasurementText(measurement.width, 'cm'),
    heightValue: toMeasurementText(measurement.height, 'cm'),
    weight: toMeasurementText(measurement.weight, 'kg'),
    currencyCode: item.currencyCode,
    skuImageUrl: item.imageUrl,
  };
}

function toWarehouseOption(warehouse: API.Warehouse.Warehouse): WarehouseOption | undefined {
  const value = Number(warehouse.warehouseId);
  if (!Number.isFinite(value)) return undefined;
  const currencyCode = warehouse.settlementCurrency || '';
  const warehouseKind = warehouse.warehouseKind || '';
  const warehouseKindLabel = warehouseKindLabels[warehouseKind] || warehouseKind || '-';
  const warehouseText = warehouse.warehouseName || warehouse.warehouseCode || String(value);
  return {
    label: `${warehouseText}（${warehouse.warehouseCode || '-'} / ${warehouseKindLabel} / ${currencyCode || '-'}）`,
    value,
    currencyCode,
    currencyLabel: currencyCode,
    warehouseKind,
    warehouseKindLabel,
  };
}

function toBoundWarehouseOption(warehouse: API.ProductDistribution.ProductWarehouse): WarehouseOption | undefined {
  const value = Number(warehouse.warehouseId);
  if (!Number.isFinite(value)) return undefined;
  const currencyCode = warehouse.settlementCurrency || '';
  const warehouseKind = warehouse.warehouseKind || '';
  const warehouseKindLabel = warehouseKindLabels[warehouseKind] || warehouseKind || '-';
  const warehouseText = warehouse.warehouseName || warehouse.warehouseCode || String(value);
  return {
    label: `${warehouseText}（${warehouse.warehouseCode || '-'} / ${warehouseKindLabel} / ${currencyCode || '-'}）`,
    value,
    currencyCode,
    currencyLabel: currencyCode,
    warehouseKind,
    warehouseKindLabel,
  };
}

function mergeWarehouseOptions(options: WarehouseOption[], boundWarehouses?: API.ProductDistribution.ProductWarehouse[]) {
  const map = new Map<number, WarehouseOption>();
  options.forEach((item) => {
    map.set(item.value, item);
  });
  (boundWarehouses || []).forEach((warehouse) => {
    const option = toBoundWarehouseOption(warehouse);
    if (option && !map.has(option.value)) {
      map.set(option.value, option);
    }
  });
  return Array.from(map.values());
}

export default function ProductDistributionEditPage() {
  const access = useAccess();
  const params = useParams<{ spuId?: string }>();
  const spuId = params.spuId ? Number(params.spuId) : undefined;
  const focusSkuId = useMemo(() => {
    const value = new URLSearchParams(history.location.search).get('skuId');
    const numberValue = value ? Number(value) : undefined;
    return Number.isFinite(numberValue) ? numberValue : undefined;
  }, []);
  const isEdit = !!spuId;
  const [form] = Form.useForm<ProductEditValues>();
  const mainImageUrl = Form.useWatch('mainImageUrl', form);
  const selectedSellerId = Form.useWatch('sellerId', form);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [product, setProduct] = useState<API.ProductDistribution.Spu>();
  const [categories, setCategories] = useState<API.Product.Category[]>([]);
  const [schema, setSchema] = useState<API.Product.CategoryAttribute[]>([]);
  const [sellerOptions, setSellerOptions] = useState<{ label: string; value: number }[]>([]);
  const [warehouseOptions, setWarehouseOptions] = useState<WarehouseOption[]>([]);
  const [galleryUrls, setGalleryUrls] = useState<string[]>([]);
  const [detailBlocks, setDetailBlocks] = useState<DetailContentBlock[]>([]);
  const [selectedWarehouseKind, setSelectedWarehouseKind] = useState<string>();
  const [selectedWarehouseIds, setSelectedWarehouseIds] = useState<number[]>([]);
  const [sourceSelectorOpen, setSourceSelectorOpen] = useState(false);
  const [skuRows, setSkuRows] = useState<(API.ProductDistribution.Sku & { rowKey?: string })[]>([
    { rowKey: 'sku-new-0', skuStatus: 'DRAFT', sortOrder: 0 },
  ]);
  const canQuerySourceProducts = access.hasPerms('product:list:list');
  const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list');
  const canQueryThirdPartyWarehouses = access.hasPerms('warehouse:thirdParty:list');

  const categoryTreeData = useMemo(
    () => toPublishCategoryTreeData(buildCategoryTree(categories)),
    [categories],
  );

  const availableWarehouseOptions = useMemo(
    () => selectedWarehouseKind
      ? warehouseOptions.filter((item) => item.warehouseKind === selectedWarehouseKind)
      : [],
    [selectedWarehouseKind, warehouseOptions],
  );

  const persistedWarehouseKind = product?.warehouses?.find((item) => item.warehouseKind)?.warehouseKind;
  const canChangeWarehouseKind = !isEdit || product?.spuStatus === 'DRAFT' || !persistedWarehouseKind;

  useEffect(() => {
    Promise.all([
      getCategoryList({ status: '0' }),
      getAdminSellerList({ pageNum: 1, pageSize: 100, status: '0' }),
    ]).then(([categoryResp, sellerResp]) => {
      setCategories(categoryResp.data || []);
      setSellerOptions(
        (sellerResp.rows || []).flatMap((seller) =>
          seller.sellerId == null
            ? []
            : [{
                label: `${seller.sellerName || seller.sellerShortName || seller.sellerNo}（${seller.sellerNo || '-'}）`,
                value: seller.sellerId,
              }],
        ),
      );
    });

  }, []);

  useEffect(() => {
    const officialWarehouseRequest = canQueryOfficialWarehouses
      ? getOfficialWarehouseList({ pageNum: 1, pageSize: 500, status: '0' })
      : Promise.resolve({ code: 200, msg: 'ok', total: 0, rows: [] } satisfies API.Warehouse.WarehousePageResult);
    const thirdPartyWarehouseRequest = selectedSellerId && canQueryThirdPartyWarehouses
      ? getThirdPartyWarehouseList({ pageNum: 1, pageSize: 500, status: '0', sellerId: selectedSellerId })
      : Promise.resolve({ code: 200, msg: 'ok', total: 0, rows: [] } satisfies API.Warehouse.WarehousePageResult);

    Promise.all([
      officialWarehouseRequest,
      thirdPartyWarehouseRequest,
    ]).then(([officialWarehouseResp, thirdPartyWarehouseResp]) => {
      const options = [
        ...(officialWarehouseResp.code === 200 ? officialWarehouseResp.rows || [] : []),
        ...(thirdPartyWarehouseResp.code === 200 ? thirdPartyWarehouseResp.rows || [] : []),
      ].map(toWarehouseOption).filter((item): item is WarehouseOption => !!item);
      const boundWarehouses = selectedSellerId === product?.sellerId ? product?.warehouses : undefined;
      setWarehouseOptions(mergeWarehouseOptions(options, boundWarehouses));
    }).catch(() => {
      const boundWarehouses = selectedSellerId === product?.sellerId ? product?.warehouses : undefined;
      setWarehouseOptions(mergeWarehouseOptions([], boundWarehouses));
    });
  }, [canQueryOfficialWarehouses, canQueryThirdPartyWarehouses, product?.warehouses, selectedSellerId]);

  useEffect(() => {
    if (!spuId) {
      form.setFieldsValue({ spuStatus: 'DRAFT' });
      return;
    }
    setLoading(true);
    getDistributionProduct(spuId)
      .then((resp) => {
        const current = resp.data;
        setProduct(current);
        const attributeValueMap: Record<string, any> = {};
        (current.attributeValues || []).forEach((item) => {
          if (item.attributeId) {
            attributeValueMap[String(item.attributeId)] = valueFromAttribute(item);
          }
        });
        form.setFieldsValue({ ...current, attributeValueMap });
        setDetailBlocks(parseDetailContent(current.detailContent));
        setSkuRows((current.skus || []).map((sku) => ({ ...sku, rowKey: String(sku.skuId) })));
        setSelectedWarehouseKind(current.warehouses?.[0]?.warehouseKind);
        setSelectedWarehouseIds(current.warehouseIds || (current.warehouses || [])
          .map((item) => item.warehouseId)
          .filter((warehouseId): warehouseId is number => warehouseId != null));
        setWarehouseOptions((options) => mergeWarehouseOptions(options, current.warehouses));
        setGalleryUrls(
          (current.images || [])
            .filter((item): item is API.ProductDistribution.ProductImage & { imageUrl: string } =>
              item.imageRole === 'GALLERY' && !!item.imageUrl)
            .map((item) => item.imageUrl),
        );
        if (current.categoryId) {
          loadSchema(current.categoryId);
        }
      })
      .finally(() => setLoading(false));
  }, [form, spuId]);

  const loadSchema = async (categoryId: number) => {
    const resp = await getCategorySchema(categoryId);
    setSchema(resp.data || []);
  };

  const handleCategoryChange = (categoryId: number) => {
    form.setFieldValue('attributeValueMap', {});
    if (categoryId) {
      loadSchema(categoryId);
    } else {
      setSchema([]);
    }
  };

  const handleSellerChange = () => {
    setSelectedWarehouseIds([]);
  };

  const handleWarehouseKindChange = (kind: string) => {
    if (!canChangeWarehouseKind && kind !== selectedWarehouseKind) {
      message.warning('仅草稿商品允许修改仓库类型');
      return;
    }
    setSelectedWarehouseKind(kind);
    setSelectedWarehouseIds([]);
  };

  const selectedWarehouses = useMemo(
    () => selectedWarehouseIds
      .map((warehouseId) => warehouseOptions.find((item) => item.value === warehouseId))
      .filter(Boolean) as WarehouseOption[],
    [selectedWarehouseIds, warehouseOptions],
  );

  const derivedCurrencyCode = selectedWarehouses[0]?.currencyCode;
  const derivedCurrencyLabel = selectedWarehouses[0]?.currencyLabel;
  const isOfficialWarehouse = selectedWarehouseKind === 'official';

  const handleWarehouseChange = (nextIds: number[]) => {
    const nextWarehouses = nextIds
      .map((warehouseId) => availableWarehouseOptions.find((item) => item.value === warehouseId))
      .filter(Boolean) as WarehouseOption[];
    if (nextIds.length !== nextWarehouses.length) {
      message.warning('请选择当前仓库类型下的发货仓库');
      return;
    }
    if (nextWarehouses.some((item) => !item.currencyCode)) {
      message.warning('所选发货仓库未维护币种');
      return;
    }
    if (nextWarehouses.some((item) => !item.warehouseKind)) {
      message.warning('所选发货仓库未维护仓库类型');
      return;
    }
    const currencyCodes = new Set(nextWarehouses.map((item) => item.currencyCode));
    if (currencyCodes.size > 1) {
      message.warning('发货仓库必须选择相同币种');
      return;
    }
    const warehouseKinds = new Set(nextWarehouses.map((item) => item.warehouseKind));
    if (warehouseKinds.size > 1) {
      message.warning('官方仓和三方仓不能混在一起选择');
      return;
    }
    setSelectedWarehouseIds(nextIds);
  };

  const openSourceSelector = () => {
    if (!canQuerySourceProducts) {
      message.warning('缺少来源 SKU 查询权限');
      return;
    }
    setSourceSelectorOpen(true);
  };

  const handleSelectSourceSku = (item: API.Integration.SourceProductItem) => {
    if (!canQuerySourceProducts) {
      message.warning('缺少来源 SKU 查询权限');
      setSourceSelectorOpen(false);
      return;
    }
    const sourceSkuGroupKey = item.sourceSkuGroupKey;
    const sourceDimensionGroupKey = item.sourceDimensionGroupKey;
    if (!sourceSkuGroupKey || !sourceDimensionGroupKey) {
      message.error('来源 SKU 缺少稳定绑定键');
      return;
    }
    if (skuRows.some((row) => row.sourceSkuGroupKey === sourceSkuGroupKey)) {
      message.warning('该来源 SKU 已在当前商品中选择');
      return;
    }
    const baseRows = skuRows.length === 1 && !hasSkuContent(skuRows[0]) ? [] : skuRows;
    setSkuRows([...baseRows, toSourceSkuRow(item, baseRows.length)]);
    setSourceSelectorOpen(false);
  };

  const sourceColumns: ProColumns<API.Integration.SourceProductItem>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '来源SKU',
      dataIndex: 'masterSku',
      width: 160,
      ellipsis: true,
    },
    {
      title: '来源商品',
      dataIndex: 'masterProductName',
      width: 260,
      ellipsis: true,
    },
    {
      title: '尺寸重量',
      dataIndex: 'sourceDimensionGroupKey',
      width: 220,
      search: false,
      renderText: (_, record) => formatSourceDimension(record),
    },
    {
      title: '来源仓',
      dataIndex: 'sourceWarehouseNames',
      width: 220,
      ellipsis: true,
      search: false,
    },
    {
      title: '仓库数',
      dataIndex: 'warehouseCount',
      width: 80,
      search: false,
    },
    {
      title: '状态',
      dataIndex: 'pairingStatus',
      width: 90,
      search: false,
      renderText: (value) => (value === 'UNASSIGNED' ? '未配对' : value === 'PAIRED' ? '已配对' : value || '-'),
    },
    {
      title: '操作',
      width: 80,
      search: false,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => handleSelectSourceSku(record)}>
          选择
        </Button>
      ),
    },
  ];

  const buildAttributeValues = (values: ProductEditValues): API.ProductDistribution.AttributeValue[] =>
    schema
      .map((item) => {
        const value = values.attributeValueMap?.[String(item.attributeId)];
        if (
          value === undefined
          || value === null
          || value === ''
          || (Array.isArray(value) && value.length === 0)
        ) return undefined;
        const base = {
          attributeId: item.attributeId,
          attributeCode: item.attributeCode,
          attributeName: item.attributeName,
          attributeType: item.attributeType,
        };
        if (item.attributeType === 'NUMBER') return { ...base, valueNumber: Number(value) };
        if (item.attributeType === 'SINGLE_SELECT' || item.attributeType === 'BOOLEAN') return { ...base, valueCode: String(value) };
        if (item.attributeType === 'MULTI_SELECT') return { ...base, valueJson: JSON.stringify(value) };
        if (item.attributeType === 'DATE') {
          return { ...base, valueDate: dayjs.isDayjs(value) ? value.format(ATTRIBUTE_DATE_FORMAT) : String(value) };
        }
        return { ...base, valueText: String(value) };
      })
      .filter(Boolean) as API.ProductDistribution.AttributeValue[];

  const renderAttributeField = (item: API.Product.CategoryAttribute) => {
    const itemKey = item.attributeId;
    const name = ['attributeValueMap', String(item.attributeId)];
    const common = {
      name,
      label: item.attributeName,
      rules: item.requiredFlag === 'Y' ? [{ required: true, message: `请输入${item.attributeName}` }] : undefined,
    };
    if (item.attributeType === 'NUMBER') {
      return (
        <Form.Item key={itemKey} {...common}>
          <InputNumber
            suffix={item.unit || undefined}
            precision={item.valuePrecision}
            placeholder={item.placeholder || `请输入${item.attributeName || ''}`}
            style={{ width: '100%' }}
          />
        </Form.Item>
      );
    }
    if (item.attributeType === 'BOOLEAN') {
      return (
        <Form.Item key={itemKey} {...common}>
          <Select
            allowClear
            options={yesNoOptions}
            placeholder={item.placeholder || '请选择是或否'}
          />
        </Form.Item>
      );
    }
    if (item.attributeType === 'SINGLE_SELECT') {
      return (
        <Form.Item key={itemKey} {...common}>
          <Select
            {...SEARCHABLE_SELECT_PROPS}
            allowClear
            placeholder={item.placeholder || `请选择${item.attributeName || ''}`}
            options={(item.options || []).map((option) => ({ label: option.optionLabel, value: option.optionCode }))}
          />
        </Form.Item>
      );
    }
    if (item.attributeType === 'MULTI_SELECT') {
      return (
        <Form.Item key={itemKey} {...common}>
          <Select
            {...SEARCHABLE_SELECT_PROPS}
            mode="multiple"
            placeholder={item.placeholder || `请选择${item.attributeName || ''}`}
            options={(item.options || []).map((option) => ({ label: option.optionLabel, value: option.optionCode }))}
          />
        </Form.Item>
      );
    }
    if (item.attributeType === 'DATE') {
      return (
        <Form.Item key={itemKey} {...common}>
          <DatePicker
            format={ATTRIBUTE_DATE_FORMAT}
            placeholder={item.placeholder || `请选择${item.attributeName || ''}`}
            style={{ width: '100%' }}
          />
        </Form.Item>
      );
    }
    return (
      <Form.Item key={itemKey} {...common}>
        <Input placeholder={item.placeholder || `请输入${item.attributeName || ''}`} />
      </Form.Item>
    );
  };

  const submit = async (targetStatus?: string) => {
    const values = await form.validateFields();
    if (!skuRows.length) {
      message.error('至少需要维护一个 SKU');
      return;
    }
    if (!selectedWarehouseKind) {
      message.error('请选择仓库类型');
      return;
    }
    if (!isOfficialWarehouse && !selectedWarehouseIds.length) {
      message.error('请选择发货仓库');
      return;
    }
    const nextSpuStatus = isEdit
      ? product?.spuStatus || values.spuStatus || 'DRAFT'
      : targetStatus || values.spuStatus || 'DRAFT';
    const cleanSkus = stripSkuRows(skuRows).map((sku) => ({
      ...sku,
      currencyCode: isOfficialWarehouse ? sku.currencyCode : derivedCurrencyCode || sku.currencyCode,
      skuStatus: targetStatus === 'READY' && (!sku.skuStatus || sku.skuStatus === 'DRAFT')
        ? 'READY'
        : sku.skuStatus || 'DRAFT',
    }));
    if (isOfficialWarehouse && cleanSkus.some((sku) => !sku.sourceDimensionGroupKey)) {
      message.error('官方仓 SKU 必须从来源商品库选择');
      return;
    }
    const invalidPriceSku = cleanSkus.find((sku) => sku.supplyPrice === undefined);
    if (invalidPriceSku) {
      message.error('请补齐 SKU 的供货价');
      return;
    }
    const missingCurrencySku = !isOfficialWarehouse && cleanSkus.find((sku) => !sku.currencyCode);
    if (missingCurrencySku) {
      message.error('请选择发货仓库以确定 SKU 币种');
      return;
    }
    setSaving(true);
    const payload: API.ProductDistribution.Spu = {
      ...values,
      detailContent: serializeDetailContent(detailBlocks),
      spuStatus: nextSpuStatus,
      warehouseKind: selectedWarehouseKind,
      warehouseIds: isOfficialWarehouse ? [] : selectedWarehouseIds,
      skus: cleanSkus,
      attributeValues: buildAttributeValues(values),
      images: [
        ...galleryUrls.filter(Boolean).map((url, index) => ({
          imageUrl: url,
          imageRole: 'GALLERY',
          sortOrder: index + 1,
        })),
      ],
    };
    const resp = await (isEdit && spuId
      ? updateDistributionProduct(spuId, payload)
      : addDistributionProduct(payload)).finally(() => setSaving(false));
    if (resp.code === 200) {
      message.success(isEdit ? '商品已更新' : '商品已新增');
      history.push('/product/distribution');
      return;
    }
    message.error(resp.msg || '保存失败');
  };

  return (
    <PageContainer title={false}>
      <div className={styles.editPage}>
        <div className={styles.editHeader}>
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => history.push('/product/distribution')}>返回</Button>
            <div>
              <div className={styles.editTitle}>{isEdit ? '编辑商城商品' : '新增商城商品'}</div>
              <div className={styles.editSubtitle}>维护 SPU 主信息、商品图片、类目属性、详情图文和 SKU 矩阵。</div>
            </div>
          </Space>
        </div>

        {isEdit ? (
          <div className={styles.readonlySummary}>
            <span>系统 SPU：{product?.systemSpuCode || '-'}</span>
            <span>来源：{product?.sourceType || '-'}</span>
            <span>SKU 数：{product?.skuCount ?? skuRows.length}</span>
          </div>
        ) : null}

        <Form form={form} layout="vertical" className={styles.editForm} disabled={loading}>
          <section className={styles.formSection}>
            <div className={styles.sectionTitle}>基础信息</div>
            <div className={styles.formGrid}>
              <Form.Item name="productName" label="商品中文标题" rules={[{ required: true, message: '请输入商品中文标题' }]}>
                <Input placeholder="例如：轻量透气棒球帽" />
              </Form.Item>
              <Form.Item name="productNameEn" label="商品英文标题" rules={[{ required: true, message: '请输入商品英文标题' }]}>
                <Input placeholder="例如：Lightweight Breathable Baseball Cap" />
              </Form.Item>
              <Form.Item name="sellerSpuCode" label="客户SPU">
                <Input placeholder="卖家自己的 SPU 编码" />
              </Form.Item>
              <Form.Item name="sellerId" label="绑定卖家" rules={[{ required: true, message: '请选择卖家' }]}>
                <Select {...SEARCHABLE_SELECT_PROPS} options={sellerOptions} placeholder="请选择卖家" onChange={handleSellerChange} />
              </Form.Item>
              <Form.Item name="categoryId" label="商品分类" rules={[{ required: true, message: '请选择末级商品分类' }]}>
                <TreeSelect
                  {...SEARCHABLE_TREE_SELECT_PROPS}
                  treeData={categoryTreeData}
                  treeDefaultExpandAll
                  placeholder="请选择末级可发布分类"
                  onChange={handleCategoryChange}
                />
              </Form.Item>
              <Form.Item label="仓库类型" required>
                <Radio.Group
                  value={selectedWarehouseKind}
                  disabled={!canChangeWarehouseKind}
                  onChange={(event) => handleWarehouseKindChange(event.target.value)}
                >
                  <Radio.Button value="official">官方仓</Radio.Button>
                  <Radio.Button value="third_party">三方仓</Radio.Button>
                </Radio.Group>
              </Form.Item>
              {isOfficialWarehouse ? (
                <Form.Item label="发货仓库">
                  <Input value="由来源 SKU 的官方履约仓自动派生" disabled />
                </Form.Item>
              ) : (
                <Form.Item label="发货仓库" required>
                  <Select
                    {...SEARCHABLE_SELECT_PROPS}
                    mode="multiple"
                    value={selectedWarehouseIds}
                    options={availableWarehouseOptions}
                    placeholder={
                      selectedWarehouseKind
                        ? selectedWarehouseKind === 'third_party' && !selectedSellerId
                          ? '请先选择卖家'
                          : '选择同币种的发货仓库'
                        : '请先选择仓库类型'
                    }
                    disabled={!selectedWarehouseKind || (selectedWarehouseKind === 'third_party' && !selectedSellerId)}
                    onChange={handleWarehouseChange}
                  />
                </Form.Item>
              )}
              <Form.Item label="币种">
                <Input value={isOfficialWarehouse ? '由官方履约仓派生' : derivedCurrencyLabel || derivedCurrencyCode || '-'} disabled />
              </Form.Item>
            </div>
            <Form.Item name="sellingPoint" label="商品卖点">
              <Input.TextArea rows={2} placeholder="用于列表或详情摘要展示" />
            </Form.Item>
          </section>

          <section className={styles.formSection}>
            <ProductImageSection
              mainImageUrl={mainImageUrl}
              galleryUrls={galleryUrls}
              onMainImageChange={(value) => form.setFieldValue('mainImageUrl', value)}
              onGalleryChange={setGalleryUrls}
            />
            <Form.Item name="mainImageUrl" hidden rules={[{ required: true, message: '请上传 SPU 主图' }]}>
              <Input />
            </Form.Item>
          </section>

          {schema.length > 0 ? (
            <section className={styles.formSection}>
              <div className={styles.sectionTitle}>类目属性</div>
              <div className={styles.formGrid}>{schema.map(renderAttributeField)}</div>
            </section>
          ) : null}

          <section className={styles.formSection}>
            <div className={styles.sectionTitle}>详情图文</div>
            <DetailContentBuilder value={detailBlocks} onChange={setDetailBlocks} />
          </section>

          <section className={styles.formSection}>
            {isOfficialWarehouse ? (
              <div className={styles.sourceSkuToolbar}>
                <Space>
                  <Button type="primary" disabled={!canQuerySourceProducts} onClick={openSourceSelector}>
                    选择来源 SKU
                  </Button>
                  <Typography.Text type="secondary">
                    官方仓商品的尺寸重量和发货仓库由来源 SKU 派生。
                  </Typography.Text>
                </Space>
              </div>
            ) : null}
            <SkuMatrixEditor
              value={skuRows}
              focusSkuId={focusSkuId}
              currencyCode={isOfficialWarehouse ? undefined : derivedCurrencyCode}
              currencyLabel={isOfficialWarehouse ? '由官方履约仓派生' : derivedCurrencyLabel}
              sourceMode={isOfficialWarehouse}
              onChange={setSkuRows}
            />
          </section>
        </Form>

        <Affix offsetBottom={0}>
          <Card size="small" className={styles.editActionCard}>
            <Space>
              <Button onClick={() => history.push('/product/distribution')}>取消</Button>
              {isEdit ? (
                <Button type="primary" loading={saving} icon={<SaveOutlined />} onClick={() => submit()}>
                  保存
                </Button>
              ) : (
                <>
                  <Button loading={saving} icon={<SaveOutlined />} onClick={() => submit('DRAFT')}>保存草稿</Button>
                  <Button type="primary" loading={saving} onClick={() => submit('READY')}>保存为待上架</Button>
                </>
              )}
            </Space>
          </Card>
        </Affix>

        <Modal
          title="选择来源 SKU"
          open={sourceSelectorOpen}
          width={1120}
          footer={null}
          destroyOnClose
          onCancel={() => setSourceSelectorOpen(false)}
        >
          <ProTable<API.Integration.SourceProductItem>
            rowKey={(record) => record.sourceDimensionGroupKey || record.sourceSkuGroupKey || record.masterSku}
            columns={sourceColumns}
            size="small"
            search={{ labelWidth: 70, span: 8 }}
            options={false}
            pagination={{ pageSize: 10 }}
            request={async (params) => {
              if (!canQuerySourceProducts) {
                return {
                  data: [],
                  total: 0,
                  success: true,
                };
              }
              const resp = await getSourceProductList({
                ...params,
                pageNum: params.current,
                pageSize: params.pageSize,
                repositoryScope: 'OFFICIAL_MASTER',
                status: 'ACTIVE',
                pairingStatus: 'UNASSIGNED',
              });
              return {
                data: resp.rows || [],
                total: resp.total || 0,
                success: resp.code === 200,
              };
            }}
          />
        </Modal>
      </div>
    </PageContainer>
  );
}
