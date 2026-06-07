import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { ArrowLeftOutlined, SaveOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { history, useAccess, useParams } from '@umijs/max';
import { Affix, Button, Card, DatePicker, Form, Input, InputNumber, Radio, Select, Space, TreeSelect } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { getCategoryList, getCategorySchema } from '@/services/product/product';
import { addDistributionProduct, getDistributionProduct, updateDistributionProduct, } from '@/services/product/distributionProduct';
import { getAdminSellerList } from '@/services/seller/seller';
import { getOfficialWarehouseList, getThirdPartyWarehouseList, } from '@/services/warehouse/warehouse';
import { message } from '@/utils/feedback';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import { buildCategoryTree } from '../categoryTree';
import { yesNoOptions } from '../constants';
import DetailContentBuilder from './components/DetailContentBuilder';
import ProductImageSection from './components/ProductImageSection';
import SkuMatrixEditor from './components/SkuMatrixEditor';
import { parseDetailContent, serializeDetailContent, } from './detailContent';
import styles from './style.module.css';
const ATTRIBUTE_DATE_FORMAT = 'YYYY-MM-DD';
const warehouseKindLabels = {
    official: '官方仓',
    third_party: '三方仓',
};
function parseAttributeJsonArray(value) {
    if (!value)
        return [];
    try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed : [];
    }
    catch {
        return [];
    }
}
function valueFromAttribute(item) {
    if (item.attributeType === 'MULTI_SELECT') {
        return parseAttributeJsonArray(item.valueJson);
    }
    if (item.attributeType === 'DATE' && item.valueDate) {
        const value = dayjs(item.valueDate);
        return value.isValid() ? value : undefined;
    }
    return item.valueText ?? item.valueCode ?? item.valueNumber ?? item.valueDate ?? item.valueJson;
}
function toPublishCategoryTreeData(categories) {
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
function stripSkuRows(rows) {
    return rows.map(({ rowKey: _rowKey, ...row }) => row);
}
function toWarehouseOption(warehouse) {
    const value = Number(warehouse.warehouseId);
    if (!Number.isFinite(value))
        return undefined;
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
function toBoundWarehouseOption(warehouse) {
    const value = Number(warehouse.warehouseId);
    if (!Number.isFinite(value))
        return undefined;
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
function mergeWarehouseOptions(options, boundWarehouses) {
    const map = new Map();
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
    const params = useParams();
    const spuId = params.spuId ? Number(params.spuId) : undefined;
    const focusSkuId = useMemo(() => {
        const value = new URLSearchParams(history.location.search).get('skuId');
        const numberValue = value ? Number(value) : undefined;
        return Number.isFinite(numberValue) ? numberValue : undefined;
    }, []);
    const isEdit = !!spuId;
    const [form] = Form.useForm();
    const mainImageUrl = Form.useWatch('mainImageUrl', form);
    const selectedSellerId = Form.useWatch('sellerId', form);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [product, setProduct] = useState();
    const [categories, setCategories] = useState([]);
    const [schema, setSchema] = useState([]);
    const [sellerOptions, setSellerOptions] = useState([]);
    const [warehouseOptions, setWarehouseOptions] = useState([]);
    const [galleryUrls, setGalleryUrls] = useState([]);
    const [detailBlocks, setDetailBlocks] = useState([]);
    const [selectedWarehouseKind, setSelectedWarehouseKind] = useState();
    const [selectedWarehouseIds, setSelectedWarehouseIds] = useState([]);
    const [skuRows, setSkuRows] = useState([
        { rowKey: 'sku-new-0', skuStatus: 'DRAFT', sortOrder: 0 },
    ]);
    const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list');
    const canQueryThirdPartyWarehouses = access.hasPerms('warehouse:thirdParty:list');
    const categoryTreeData = useMemo(() => toPublishCategoryTreeData(buildCategoryTree(categories)), [categories]);
    const availableWarehouseOptions = useMemo(() => selectedWarehouseKind
        ? warehouseOptions.filter((item) => item.warehouseKind === selectedWarehouseKind)
        : [], [selectedWarehouseKind, warehouseOptions]);
    useEffect(() => {
        Promise.all([
            getCategoryList({ status: '0' }),
            getAdminSellerList({ pageNum: 1, pageSize: 100, status: '0' }),
        ]).then(([categoryResp, sellerResp]) => {
            setCategories(categoryResp.data || []);
            setSellerOptions((sellerResp.rows || []).flatMap((seller) => seller.sellerId == null
                ? []
                : [{
                        label: `${seller.sellerName || seller.sellerShortName || seller.sellerNo}（${seller.sellerNo || '-'}）`,
                        value: seller.sellerId,
                    }]));
        });
    }, []);
    useEffect(() => {
        const officialWarehouseRequest = canQueryOfficialWarehouses
            ? getOfficialWarehouseList({ pageNum: 1, pageSize: 500, status: '0' })
            : Promise.resolve({ code: 200, msg: 'ok', total: 0, rows: [] });
        const thirdPartyWarehouseRequest = selectedSellerId && canQueryThirdPartyWarehouses
            ? getThirdPartyWarehouseList({ pageNum: 1, pageSize: 500, status: '0', sellerId: selectedSellerId })
            : Promise.resolve({ code: 200, msg: 'ok', total: 0, rows: [] });
        Promise.all([
            officialWarehouseRequest,
            thirdPartyWarehouseRequest,
        ]).then(([officialWarehouseResp, thirdPartyWarehouseResp]) => {
            const options = [
                ...(officialWarehouseResp.code === 200 ? officialWarehouseResp.rows || [] : []),
                ...(thirdPartyWarehouseResp.code === 200 ? thirdPartyWarehouseResp.rows || [] : []),
            ].map(toWarehouseOption).filter((item) => !!item);
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
            const attributeValueMap = {};
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
                .filter((warehouseId) => warehouseId != null));
            setWarehouseOptions((options) => mergeWarehouseOptions(options, current.warehouses));
            setGalleryUrls((current.images || [])
                .filter((item) => item.imageRole === 'GALLERY' && !!item.imageUrl)
                .map((item) => item.imageUrl));
            if (current.categoryId) {
                loadSchema(current.categoryId);
            }
        })
            .finally(() => setLoading(false));
    }, [form, spuId]);
    const loadSchema = async (categoryId) => {
        const resp = await getCategorySchema(categoryId);
        setSchema(resp.data || []);
    };
    const handleCategoryChange = (categoryId) => {
        form.setFieldValue('attributeValueMap', {});
        if (categoryId) {
            loadSchema(categoryId);
        }
        else {
            setSchema([]);
        }
    };
    const handleSellerChange = () => {
        setSelectedWarehouseIds([]);
    };
    const handleWarehouseKindChange = (kind) => {
        setSelectedWarehouseKind(kind);
        setSelectedWarehouseIds([]);
    };
    const selectedWarehouses = useMemo(() => selectedWarehouseIds
        .map((warehouseId) => warehouseOptions.find((item) => item.value === warehouseId))
        .filter(Boolean), [selectedWarehouseIds, warehouseOptions]);
    const derivedCurrencyCode = selectedWarehouses[0]?.currencyCode;
    const derivedCurrencyLabel = selectedWarehouses[0]?.currencyLabel;
    const handleWarehouseChange = (nextIds) => {
        const nextWarehouses = nextIds
            .map((warehouseId) => availableWarehouseOptions.find((item) => item.value === warehouseId))
            .filter(Boolean);
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
    const buildAttributeValues = (values) => schema
        .map((item) => {
        const value = values.attributeValueMap?.[String(item.attributeId)];
        if (value === undefined
            || value === null
            || value === ''
            || (Array.isArray(value) && value.length === 0))
            return undefined;
        const base = {
            attributeId: item.attributeId,
            attributeCode: item.attributeCode,
            attributeName: item.attributeName,
            attributeType: item.attributeType,
        };
        if (item.attributeType === 'NUMBER')
            return { ...base, valueNumber: Number(value) };
        if (item.attributeType === 'SINGLE_SELECT' || item.attributeType === 'BOOLEAN')
            return { ...base, valueCode: String(value) };
        if (item.attributeType === 'MULTI_SELECT')
            return { ...base, valueJson: JSON.stringify(value) };
        if (item.attributeType === 'DATE') {
            return { ...base, valueDate: dayjs.isDayjs(value) ? value.format(ATTRIBUTE_DATE_FORMAT) : String(value) };
        }
        return { ...base, valueText: String(value) };
    })
        .filter(Boolean);
    const renderAttributeField = (item) => {
        const itemKey = item.attributeId;
        const name = ['attributeValueMap', String(item.attributeId)];
        const common = {
            name,
            label: item.attributeName,
            rules: item.requiredFlag === 'Y' ? [{ required: true, message: `请输入${item.attributeName}` }] : undefined,
        };
        if (item.attributeType === 'NUMBER') {
            return (_jsx(Form.Item, { ...common, children: _jsx(InputNumber, { suffix: item.unit || undefined, precision: item.valuePrecision, placeholder: item.placeholder || `请输入${item.attributeName || ''}`, style: { width: '100%' } }) }, itemKey));
        }
        if (item.attributeType === 'BOOLEAN') {
            return (_jsx(Form.Item, { ...common, children: _jsx(Select, { allowClear: true, options: yesNoOptions, placeholder: item.placeholder || '请选择是或否' }) }, itemKey));
        }
        if (item.attributeType === 'SINGLE_SELECT') {
            return (_jsx(Form.Item, { ...common, children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, allowClear: true, placeholder: item.placeholder || `请选择${item.attributeName || ''}`, options: (item.options || []).map((option) => ({ label: option.optionLabel, value: option.optionCode })) }) }, itemKey));
        }
        if (item.attributeType === 'MULTI_SELECT') {
            return (_jsx(Form.Item, { ...common, children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, mode: "multiple", placeholder: item.placeholder || `请选择${item.attributeName || ''}`, options: (item.options || []).map((option) => ({ label: option.optionLabel, value: option.optionCode })) }) }, itemKey));
        }
        if (item.attributeType === 'DATE') {
            return (_jsx(Form.Item, { ...common, children: _jsx(DatePicker, { format: ATTRIBUTE_DATE_FORMAT, placeholder: item.placeholder || `请选择${item.attributeName || ''}`, style: { width: '100%' } }) }, itemKey));
        }
        return (_jsx(Form.Item, { ...common, children: _jsx(Input, { placeholder: item.placeholder || `请输入${item.attributeName || ''}` }) }, itemKey));
    };
    const submit = async (targetStatus) => {
        const values = await form.validateFields();
        if (!skuRows.length) {
            message.error('至少需要维护一个 SKU');
            return;
        }
        if (!selectedWarehouseKind) {
            message.error('请选择仓库类型');
            return;
        }
        if (!selectedWarehouseIds.length) {
            message.error('请选择发货仓库');
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
        const payload = {
            ...values,
            detailContent: serializeDetailContent(detailBlocks),
            spuStatus: nextSpuStatus,
            warehouseIds: selectedWarehouseIds,
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
    return (_jsx(PageContainer, { title: false, children: _jsxs("div", { className: styles.editPage, children: [_jsx("div", { className: styles.editHeader, children: _jsxs(Space, { children: [_jsx(Button, { icon: _jsx(ArrowLeftOutlined, {}), onClick: () => history.push('/product/distribution'), children: "\u8FD4\u56DE" }), _jsxs("div", { children: [_jsx("div", { className: styles.editTitle, children: isEdit ? '编辑商城商品' : '新增商城商品' }), _jsx("div", { className: styles.editSubtitle, children: "\u7EF4\u62A4 SPU \u4E3B\u4FE1\u606F\u3001\u5546\u54C1\u56FE\u7247\u3001\u7C7B\u76EE\u5C5E\u6027\u3001\u8BE6\u60C5\u56FE\u6587\u548C SKU \u77E9\u9635\u3002" })] })] }) }), isEdit ? (_jsxs("div", { className: styles.readonlySummary, children: [_jsxs("span", { children: ["\u7CFB\u7EDF SPU\uFF1A", product?.systemSpuCode || '-'] }), _jsxs("span", { children: ["\u6765\u6E90\uFF1A", product?.sourceType || '-'] }), _jsxs("span", { children: ["SKU \u6570\uFF1A", product?.skuCount ?? skuRows.length] })] })) : null, _jsxs(Form, { form: form, layout: "vertical", className: styles.editForm, disabled: loading, children: [_jsxs("section", { className: styles.formSection, children: [_jsx("div", { className: styles.sectionTitle, children: "\u57FA\u7840\u4FE1\u606F" }), _jsxs("div", { className: styles.formGrid, children: [_jsx(Form.Item, { name: "productName", label: "\u5546\u54C1\u4E2D\u6587\u6807\u9898", rules: [{ required: true, message: '请输入商品中文标题' }], children: _jsx(Input, { placeholder: "\u4F8B\u5982\uFF1A\u8F7B\u91CF\u900F\u6C14\u68D2\u7403\u5E3D" }) }), _jsx(Form.Item, { name: "productNameEn", label: "\u5546\u54C1\u82F1\u6587\u6807\u9898", rules: [{ required: true, message: '请输入商品英文标题' }], children: _jsx(Input, { placeholder: "\u4F8B\u5982\uFF1ALightweight Breathable Baseball Cap" }) }), _jsx(Form.Item, { name: "sellerSpuCode", label: "\u5BA2\u6237SPU", children: _jsx(Input, { placeholder: "\u5356\u5BB6\u81EA\u5DF1\u7684 SPU \u7F16\u7801" }) }), _jsx(Form.Item, { name: "sellerId", label: "\u7ED1\u5B9A\u5356\u5BB6", rules: [{ required: true, message: '请选择卖家' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: sellerOptions, placeholder: "\u8BF7\u9009\u62E9\u5356\u5BB6", onChange: handleSellerChange }) }), _jsx(Form.Item, { name: "categoryId", label: "\u5546\u54C1\u5206\u7C7B", rules: [{ required: true, message: '请选择末级商品分类' }], children: _jsx(TreeSelect, { ...SEARCHABLE_TREE_SELECT_PROPS, treeData: categoryTreeData, treeDefaultExpandAll: true, placeholder: "\u8BF7\u9009\u62E9\u672B\u7EA7\u53EF\u53D1\u5E03\u5206\u7C7B", onChange: handleCategoryChange }) }), _jsx(Form.Item, { label: "\u4ED3\u5E93\u7C7B\u578B", required: true, children: _jsxs(Radio.Group, { value: selectedWarehouseKind, onChange: (event) => handleWarehouseKindChange(event.target.value), children: [_jsx(Radio.Button, { value: "official", children: "\u5B98\u65B9\u4ED3" }), _jsx(Radio.Button, { value: "third_party", children: "\u4E09\u65B9\u4ED3" })] }) }), _jsx(Form.Item, { label: "\u53D1\u8D27\u4ED3\u5E93", required: true, children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, mode: "multiple", value: selectedWarehouseIds, options: availableWarehouseOptions, placeholder: selectedWarehouseKind
                                                    ? selectedWarehouseKind === 'third_party' && !selectedSellerId
                                                        ? '请先选择卖家'
                                                        : '选择同币种的发货仓库'
                                                    : '请先选择仓库类型', disabled: !selectedWarehouseKind || (selectedWarehouseKind === 'third_party' && !selectedSellerId), onChange: handleWarehouseChange }) }), _jsx(Form.Item, { label: "\u5E01\u79CD", children: _jsx(Input, { value: derivedCurrencyLabel || derivedCurrencyCode || '-', disabled: true }) })] }), _jsx(Form.Item, { name: "sellingPoint", label: "\u5546\u54C1\u5356\u70B9", children: _jsx(Input.TextArea, { rows: 2, placeholder: "\u7528\u4E8E\u5217\u8868\u6216\u8BE6\u60C5\u6458\u8981\u5C55\u793A" }) })] }), _jsxs("section", { className: styles.formSection, children: [_jsx(ProductImageSection, { mainImageUrl: mainImageUrl, galleryUrls: galleryUrls, onMainImageChange: (value) => form.setFieldValue('mainImageUrl', value), onGalleryChange: setGalleryUrls }), _jsx(Form.Item, { name: "mainImageUrl", hidden: true, rules: [{ required: true, message: '请上传 SPU 主图' }], children: _jsx(Input, {}) })] }), schema.length > 0 ? (_jsxs("section", { className: styles.formSection, children: [_jsx("div", { className: styles.sectionTitle, children: "\u7C7B\u76EE\u5C5E\u6027" }), _jsx("div", { className: styles.formGrid, children: schema.map(renderAttributeField) })] })) : null, _jsxs("section", { className: styles.formSection, children: [_jsx("div", { className: styles.sectionTitle, children: "\u8BE6\u60C5\u56FE\u6587" }), _jsx(DetailContentBuilder, { value: detailBlocks, onChange: setDetailBlocks })] }), _jsx("section", { className: styles.formSection, children: _jsx(SkuMatrixEditor, { value: skuRows, focusSkuId: focusSkuId, currencyCode: derivedCurrencyCode, currencyLabel: derivedCurrencyLabel, onChange: setSkuRows }) })] }), _jsx(Affix, { offsetBottom: 0, children: _jsx(Card, { size: "small", className: styles.editActionCard, children: _jsxs(Space, { children: [_jsx(Button, { onClick: () => history.push('/product/distribution'), children: "\u53D6\u6D88" }), isEdit ? (_jsx(Button, { type: "primary", loading: saving, icon: _jsx(SaveOutlined, {}), onClick: () => submit(), children: "\u4FDD\u5B58" })) : (_jsxs(_Fragment, { children: [_jsx(Button, { loading: saving, icon: _jsx(SaveOutlined, {}), onClick: () => submit('DRAFT'), children: "\u4FDD\u5B58\u8349\u7A3F" }), _jsx(Button, { type: "primary", loading: saving, onClick: () => submit('READY'), children: "\u4FDD\u5B58\u4E3A\u5F85\u4E0A\u67B6" })] }))] }) }) })] }) }));
}
