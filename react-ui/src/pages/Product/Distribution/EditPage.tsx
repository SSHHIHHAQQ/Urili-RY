import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { history, useParams } from '@umijs/max';
import { Affix, Button, Card, DatePicker, Form, Input, InputNumber, Select, Space, TreeSelect } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { getCategoryList, getCategorySchema } from '@/services/product/product';
import {
  addDistributionProduct,
  getDistributionProduct,
  updateDistributionProduct,
} from '@/services/product/distributionProduct';
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
  value: string;
  currencyCode: string;
  currencyLabel: string;
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

function toWarehouseOption(warehouse: API.Warehouse.Warehouse): WarehouseOption {
  const value = String(warehouse.warehouseCode || warehouse.warehouseId || '');
  const currencyCode = warehouse.settlementCurrency || '';
  const warehouseText = warehouse.warehouseName || warehouse.warehouseCode || value;
  return {
    label: `${warehouseText}（${warehouse.warehouseCode || '-'} / ${currencyCode || '-'}）`,
    value,
    currencyCode,
    currencyLabel: currencyCode,
  };
}

export default function ProductDistributionEditPage() {
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
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [product, setProduct] = useState<API.ProductDistribution.Spu>();
  const [categories, setCategories] = useState<API.Product.Category[]>([]);
  const [schema, setSchema] = useState<API.Product.CategoryAttribute[]>([]);
  const [sellerOptions, setSellerOptions] = useState<{ label: string; value: number }[]>([]);
  const [warehouseOptions, setWarehouseOptions] = useState<WarehouseOption[]>([]);
  const [galleryUrls, setGalleryUrls] = useState<string[]>([]);
  const [detailBlocks, setDetailBlocks] = useState<DetailContentBlock[]>([]);
  const [selectedWarehouseCodes, setSelectedWarehouseCodes] = useState<string[]>([]);
  const [skuRows, setSkuRows] = useState<(API.ProductDistribution.Sku & { rowKey?: string })[]>([
    { rowKey: 'sku-new-0', skuStatus: 'DRAFT', sortOrder: 0 },
  ]);

  const categoryTreeData = useMemo(
    () => toPublishCategoryTreeData(buildCategoryTree(categories)),
    [categories],
  );

  useEffect(() => {
    Promise.all([
      getCategoryList({ status: '0' }),
      getAdminSellerList({ pageNum: 1, pageSize: 100, status: '0' }),
    ]).then(([categoryResp, sellerResp]) => {
      setCategories(categoryResp.data || []);
      setSellerOptions(
        (sellerResp.rows || []).map((seller) => ({
          label: `${seller.sellerName || seller.sellerShortName || seller.sellerNo}（${seller.sellerNo || '-'}）`,
          value: seller.sellerId!,
        })),
      );
    });

    Promise.all([
      getOfficialWarehouseList({ pageNum: 1, pageSize: 500, status: '0' }),
      getThirdPartyWarehouseList({ pageNum: 1, pageSize: 500, status: '0' }),
    ]).then(([officialWarehouseResp, thirdPartyWarehouseResp]) => {
      setWarehouseOptions([
        ...(officialWarehouseResp.code === 200 ? officialWarehouseResp.rows || [] : []),
        ...(thirdPartyWarehouseResp.code === 200 ? thirdPartyWarehouseResp.rows || [] : []),
      ].map(toWarehouseOption).filter((item) => item.value));
    }).catch(() => setWarehouseOptions([]));
  }, []);

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
        setGalleryUrls(
          (current.images || [])
            .filter((item) => item.imageRole === 'GALLERY' && item.imageUrl)
            .map((item) => item.imageUrl!),
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

  const selectedWarehouses = useMemo(
    () => selectedWarehouseCodes
      .map((code) => warehouseOptions.find((item) => item.value === code))
      .filter(Boolean) as WarehouseOption[],
    [selectedWarehouseCodes, warehouseOptions],
  );

  const derivedCurrencyCode = selectedWarehouses[0]?.currencyCode;
  const derivedCurrencyLabel = selectedWarehouses[0]?.currencyLabel;

  const handleWarehouseChange = (nextCodes: string[]) => {
    const nextWarehouses = nextCodes
      .map((code) => warehouseOptions.find((item) => item.value === code))
      .filter(Boolean) as WarehouseOption[];
    if (nextWarehouses.some((item) => !item.currencyCode)) {
      message.warning('所选发货仓库未维护币种');
      return;
    }
    const currencyCodes = new Set(nextWarehouses.map((item) => item.currencyCode));
    if (currencyCodes.size > 1) {
      message.warning('发货仓库必须选择相同币种');
      return;
    }
    setSelectedWarehouseCodes(nextCodes);
  };

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
    const nextSpuStatus = isEdit
      ? product?.spuStatus || values.spuStatus || 'DRAFT'
      : targetStatus || values.spuStatus || 'DRAFT';
    const cleanSkus = stripSkuRows(skuRows).map((sku) => ({
      ...sku,
      currencyCode: derivedCurrencyCode || sku.currencyCode,
      skuStatus: targetStatus === 'READY' && (!sku.skuStatus || sku.skuStatus === 'DRAFT')
        ? 'READY'
        : sku.skuStatus || 'DRAFT',
    }));
    const invalidPriceSku = cleanSkus.find((sku) => sku.supplyPrice === undefined);
    if (invalidPriceSku) {
      message.error('请补齐 SKU 的供货价');
      return;
    }
    const missingCurrencySku = cleanSkus.find((sku) => !sku.currencyCode);
    if (missingCurrencySku) {
      message.error('请选择发货仓库以确定 SKU 币种');
      return;
    }
    setSaving(true);
    const payload: API.ProductDistribution.Spu = {
      ...values,
      detailContent: serializeDetailContent(detailBlocks),
      spuStatus: nextSpuStatus,
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
                <Select {...SEARCHABLE_SELECT_PROPS} options={sellerOptions} placeholder="请选择卖家" />
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
              <Form.Item label="发货仓库（预留）">
                <Select
                  {...SEARCHABLE_SELECT_PROPS}
                  mode="multiple"
                  value={selectedWarehouseCodes}
                  options={warehouseOptions}
                  placeholder="选择币种相同的发货仓库"
                  onChange={handleWarehouseChange}
                />
              </Form.Item>
              <Form.Item label="币种">
                <Input value={derivedCurrencyLabel || derivedCurrencyCode || '-'} disabled />
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
            <SkuMatrixEditor
              value={skuRows}
              focusSkuId={focusSkuId}
              currencyCode={derivedCurrencyCode}
              currencyLabel={derivedCurrencyLabel}
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
      </div>
    </PageContainer>
  );
}
