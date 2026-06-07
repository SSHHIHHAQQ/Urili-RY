import { Fragment, jsx, jsxs } from "react/jsx-runtime";
import { ArrowLeftOutlined, EyeOutlined, SaveOutlined } from "@ant-design/icons";
import { PageContainer, ProTable } from "@ant-design/pro-components";
import { history, useAccess, useParams } from "@umijs/max";
import { Affix, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Radio, Select, Space, Tag, TreeSelect, Typography } from "antd";
import dayjs from "dayjs";
import { useEffect, useMemo, useState } from "react";
import { getCategoryList, getCategorySchema } from "@/services/product/product";
import {
  addDistributionProduct,
  getDistributionProduct,
  updateDistributionProduct
} from "@/services/product/distributionProduct";
import { getSourceProductList } from "@/services/integration/sourceProduct";
import { getAdminSellerList } from "@/services/seller/seller";
import {
  getOfficialWarehouseList,
  getThirdPartyWarehouseList
} from "@/services/warehouse/warehouse";
import { message } from "@/utils/feedback";
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from "@/utils/selectSearch";
import { buildCategoryTree } from "../categoryTree";
import { yesNoOptions } from "../constants";
import DetailContentBuilder from "./components/DetailContentBuilder";
import ProductImageSection from "./components/ProductImageSection";
import SkuMatrixEditor from "./components/SkuMatrixEditor";
import BuyerProductPreviewModal from "./components/BuyerProductPreviewModal";
import {
  parseDetailContent,
  serializeDetailContent
} from "./detailContent";
import styles from "./style.module.css";
const ATTRIBUTE_DATE_FORMAT = "YYYY-MM-DD";
const warehouseKindLabels = {
  official: "\u5B98\u65B9\u4ED3",
  third_party: "\u4E09\u65B9\u4ED3"
};
const previewPriceSamples = {
  CNY: ["\xA5199.00", "\xA5229.00", "\xA5259.00", "\xA5299.00"],
  USD: ["$29.90", "$34.90", "$39.90", "$46.90"]
};
function parseAttributeJsonArray(value) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
function valueFromAttribute(item) {
  if (item.attributeType === "MULTI_SELECT") {
    return parseAttributeJsonArray(item.valueJson);
  }
  if (item.attributeType === "DATE" && item.valueDate) {
    const value = dayjs(item.valueDate);
    return value.isValid() ? value : void 0;
  }
  return item.valueText ?? item.valueCode ?? item.valueNumber ?? item.valueDate ?? item.valueJson;
}
function toPublishCategoryTreeData(categories) {
  return categories.map((item) => {
    const children = item.children?.length ? toPublishCategoryTreeData(item.children) : void 0;
    return {
      title: item.categoryName,
      value: item.categoryId,
      disabled: !!children?.length || item.publishEnabled !== "Y",
      ...children ? { children } : {}
    };
  });
}
function stripSkuRows(rows) {
  return rows.map(({ rowKey: _rowKey, ...row }) => row);
}
function formatNumber(value, digits = 2) {
  return typeof value === "number" && Number.isFinite(value) ? value.toFixed(digits) : void 0;
}
function hasWmsMeasurement(item) {
  return [item.wmsLength, item.wmsWidth, item.wmsHeight, item.wmsWeight].every((value) => typeof value === "number" && Number.isFinite(value));
}
function pickSourceMeasurement(item) {
  const useWms = hasWmsMeasurement(item);
  return {
    length: useWms ? item.wmsLength : item.length,
    width: useWms ? item.wmsWidth : item.width,
    height: useWms ? item.wmsHeight : item.height,
    weight: useWms ? item.wmsWeight : item.weight,
    source: useWms ? "WMS" : "PRODUCT"
  };
}
function formatSourceDimension(item) {
  const measurement = pickSourceMeasurement(item);
  const length = formatNumber(measurement.length);
  const width = formatNumber(measurement.width);
  const height = formatNumber(measurement.height);
  const weight = formatNumber(measurement.weight);
  if (!length || !width || !height || !weight) return "-";
  return `${length} x ${width} x ${height} cm  ${weight} kg`;
}
function toMeasurementText(value, unit) {
  const text = formatNumber(value);
  return text ? `${text} ${unit}` : void 0;
}
function createEmptySkuRow(sortOrder = 0) {
  return {
    rowKey: `sku-new-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    skuStatus: "DRAFT",
    sortOrder
  };
}
function toSourceSkuRow(item, sortOrder) {
  const measurement = pickSourceMeasurement(item);
  return {
    rowKey: `source-${item.sourceDimensionGroupKey || item.sourceSkuGroupKey}-${Date.now()}`,
    skuStatus: "DRAFT",
    sortOrder,
    sourceScope: "OFFICIAL_MASTER",
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
    lengthValue: toMeasurementText(measurement.length, "cm"),
    widthValue: toMeasurementText(measurement.width, "cm"),
    heightValue: toMeasurementText(measurement.height, "cm"),
    weight: toMeasurementText(measurement.weight, "kg"),
    currencyCode: item.currencyCode,
    skuImageUrl: item.imageUrl
  };
}
function getSourceSkuKey(item) {
  return item?.sourceDimensionGroupKey || item?.sourceSkuGroupKey || item?.masterSku || "";
}
function getSkuSourceKey(row) {
  return row?.sourceDimensionGroupKey || row?.sourceSkuGroupKey || row?.masterSku || "";
}
function toSourceItemFromSkuRow(row) {
  if (!row.sourceDimensionGroupKey && !row.sourceSkuGroupKey) return void 0;
  return {
    connectionCode: "",
    sourceSkuGroupKey: row.sourceSkuGroupKey,
    sourceDimensionGroupKey: row.sourceDimensionGroupKey,
    masterSku: row.masterSku || "",
    masterProductName: row.masterProductNameSnapshot || row.productName || "",
    sourceWarehouseNames: row.sourceWarehouseNames,
    warehouseCount: row.sourceWarehouseCount,
    currencyCode: row.currencyCode,
    imageUrl: row.skuImageUrl,
    length: row.measureLengthCm,
    width: row.measureWidthCm,
    height: row.measureHeightCm,
    weight: row.measureWeightKg,
    sourcePayloadHash: row.sourcePayloadHash
  };
}
function mergeSourceSkuRow(current, sourceRow, sortOrder) {
  return {
    ...current,
    ...sourceRow,
    rowKey: current.rowKey || sourceRow.rowKey,
    skuId: current.skuId,
    sellerSkuCode: current.sellerSkuCode,
    color: current.color,
    size: current.size,
    material: current.material,
    style: current.style,
    model: current.model,
    packageQuantity: current.packageQuantity,
    capacity: current.capacity,
    skuImageUrl: current.skuImageUrl || sourceRow.skuImageUrl,
    supplyPrice: current.supplyPrice,
    salePrice: current.salePrice,
    skuStatus: current.skuStatus || sourceRow.skuStatus,
    sortOrder: current.sortOrder ?? sortOrder
  };
}
function toWarehouseOption(warehouse) {
  const value = Number(warehouse.warehouseId);
  if (!Number.isFinite(value)) return void 0;
  const currencyCode = warehouse.settlementCurrency || "";
  const warehouseKind = warehouse.warehouseKind || "";
  const warehouseKindLabel = warehouseKindLabels[warehouseKind] || warehouseKind || "-";
  const warehouseText = warehouse.warehouseName || warehouse.warehouseCode || String(value);
  return {
    label: `${warehouseText}\uFF08${warehouse.warehouseCode || "-"} / ${warehouseKindLabel} / ${currencyCode || "-"}\uFF09`,
    value,
    currencyCode,
    currencyLabel: currencyCode,
    warehouseKind,
    warehouseKindLabel
  };
}
function toBoundWarehouseOption(warehouse) {
  const value = Number(warehouse.warehouseId);
  if (!Number.isFinite(value)) return void 0;
  const currencyCode = warehouse.settlementCurrency || "";
  const warehouseKind = warehouse.warehouseKind || "";
  const warehouseKindLabel = warehouseKindLabels[warehouseKind] || warehouseKind || "-";
  const warehouseText = warehouse.warehouseName || warehouse.warehouseCode || String(value);
  return {
    label: `${warehouseText}\uFF08${warehouse.warehouseCode || "-"} / ${warehouseKindLabel} / ${currencyCode || "-"}\uFF09`,
    value,
    currencyCode,
    currencyLabel: currencyCode,
    warehouseKind,
    warehouseKindLabel
  };
}
function mergeWarehouseOptions(options, boundWarehouses) {
  const map = /* @__PURE__ */ new Map();
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
function findCategoryName(categories, categoryId) {
  if (!categoryId) return void 0;
  for (const item of categories) {
    if (item.categoryId === categoryId) return item.categoryName;
    const childName = findCategoryName(item.children || [], categoryId);
    if (childName) return childName;
  }
  return void 0;
}
function formatPreviewAttributeValue(item, value) {
  if (value === void 0 || value === null || value === "" || Array.isArray(value) && value.length === 0) {
    return void 0;
  }
  if (item.attributeType === "BOOLEAN") {
    const option = yesNoOptions.find((entry) => entry.value === value);
    return option?.label || String(value);
  }
  if (item.attributeType === "SINGLE_SELECT") {
    return item.options?.find((option) => option.optionCode === value)?.optionLabel || String(value);
  }
  if (item.attributeType === "MULTI_SELECT") {
    return (Array.isArray(value) ? value : [value]).map((entry) => item.options?.find((option) => option.optionCode === entry)?.optionLabel || String(entry)).join(" / ");
  }
  if (item.attributeType === "DATE") {
    return dayjs.isDayjs(value) ? value.format(ATTRIBUTE_DATE_FORMAT) : String(value);
  }
  return item.attributeType === "NUMBER" && item.unit ? `${value} ${item.unit}` : String(value);
}
function buildPreviewAttributes(values, schema) {
  return schema.flatMap((item) => {
    const label = item.attributeName || item.attributeCode || "";
    const value = formatPreviewAttributeValue(item, values.attributeValueMap?.[String(item.attributeId)]);
    return label && value ? [{ label, value }] : [];
  });
}
function buildPreviewPrice(currencyCode, index) {
  const normalizedCurrency = currencyCode?.toUpperCase() || "CNY";
  const samples = previewPriceSamples[normalizedCurrency] || previewPriceSamples.CNY;
  return samples[index % samples.length];
}
function buildPreviewSkus(rows, currencyCode) {
  const sourceRows = rows.length ? rows : [{
    rowKey: "preview-sku",
    color: "\u9ED1\u8272",
    size: "M",
    lengthValue: "42.00 cm",
    widthValue: "42.00 cm",
    heightValue: "17.00 cm",
    weight: "920 g"
  }];
  return sourceRows.map((row, index) => {
    const rowCurrency = row.currencyCode || currencyCode || "CNY";
    return {
      ...row,
      currencyCode: rowCurrency,
      previewPrice: buildPreviewPrice(rowCurrency, index),
      previewStock: `\u73B0\u8D27 ${128 + index * 37} \u4EF6`
    };
  });
}
function buildPreviewWarehouses(warehouseKind, rows, selectedWarehouses) {
  if (warehouseKind === "official") {
    const officialNames = Array.from(new Set(rows.flatMap((row) => (row.sourceWarehouseNames || "").split(/[,\uFF0C/]/).map((item) => item.trim()).filter(Boolean))));
    const names = officialNames.length ? officialNames : ["\u5E73\u53F0\u5B98\u65B9\u4ED3"];
    return names.map((name, index) => ({
      key: `official-${name}-${index}`,
      name,
      kind: "official",
      stockText: `\u5B98\u65B9\u73B0\u8D27 ${186 + index * 42} \u4EF6`,
      deliveryText: "\u5E73\u53F0\u5B98\u65B9\u4ED3\u73B0\u8D27\u53D1\u8D27 / \u8FD0\u8D39\u4E0B\u5355\u65F6\u8BA1\u7B97"
    }));
  }
  const warehouses = selectedWarehouses.length ? selectedWarehouses : [{
    label: "\u9ED8\u8BA4\u53D1\u8D27\u4ED3",
    value: 0,
    currencyCode: "CNY",
    currencyLabel: "CNY",
    warehouseKind: warehouseKind || "third_party",
    warehouseKindLabel: "\u4E09\u65B9\u4ED3"
  }];
  return warehouses.map((warehouse, index) => ({
    key: String(warehouse.value || index),
    name: String(warehouse.label || `\u53D1\u8D27\u4ED3 ${index + 1}`).split("/")[0].trim(),
    kind: warehouse.warehouseKind,
    stockText: `\u73B0\u8D27 ${96 + index * 28} \u4EF6`,
    deliveryText: "\u4ED3\u5E93\u73B0\u8D27\u53D1\u8D27 / \u8FD0\u8D39\u4E0B\u5355\u65F6\u8BA1\u7B97"
  }));
}
function ProductDistributionEditPage() {
  const access = useAccess();
  const params = useParams();
  const spuId = params.spuId ? Number(params.spuId) : void 0;
  const focusSkuId = useMemo(() => {
    const value = new URLSearchParams(history.location.search).get("skuId");
    const numberValue = value ? Number(value) : void 0;
    return Number.isFinite(numberValue) ? numberValue : void 0;
  }, []);
  const isEdit = !!spuId;
  const [form] = Form.useForm();
  const mainImageUrl = Form.useWatch("mainImageUrl", form);
  const selectedSellerId = Form.useWatch("sellerId", form);
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
  const [sourceSelectorOpen, setSourceSelectorOpen] = useState(false);
  const [selectedSourceSkuMap, setSelectedSourceSkuMap] = useState({});
  const [buyerPreviewOpen, setBuyerPreviewOpen] = useState(false);
  const [buyerPreviewData, setBuyerPreviewData] = useState();
  const [skuRows, setSkuRows] = useState([
    createEmptySkuRow()
  ]);
  const canQuerySourceProducts = access.hasPerms("product:list:list");
  const canQueryOfficialWarehouses = access.hasPerms("warehouse:official:list");
  const canQueryThirdPartyWarehouses = access.hasPerms("warehouse:thirdParty:list");
  const categoryTreeData = useMemo(
    () => toPublishCategoryTreeData(buildCategoryTree(categories)),
    [categories]
  );
  const availableWarehouseOptions = useMemo(
    () => selectedWarehouseKind ? warehouseOptions.filter((item) => item.warehouseKind === selectedWarehouseKind) : [],
    [selectedWarehouseKind, warehouseOptions]
  );
  const persistedWarehouseKind = product?.warehouses?.find((item) => item.warehouseKind)?.warehouseKind;
  const canChangeWarehouseKind = !isEdit || product?.spuStatus === "DRAFT" || !persistedWarehouseKind;
  useEffect(() => {
    Promise.all([
      getCategoryList({ status: "0" }),
      getAdminSellerList({ pageNum: 1, pageSize: 100, status: "0" })
    ]).then(([categoryResp, sellerResp]) => {
      setCategories(categoryResp.data || []);
      setSellerOptions(
        (sellerResp.rows || []).flatMap(
          (seller) => seller.sellerId == null ? [] : [{
            label: `${seller.sellerName || seller.sellerShortName || seller.sellerNo}\uFF08${seller.sellerNo || "-"}\uFF09`,
            value: seller.sellerId
          }]
        )
      );
    });
  }, []);
  useEffect(() => {
    const officialWarehouseRequest = canQueryOfficialWarehouses ? getOfficialWarehouseList({ pageNum: 1, pageSize: 500, status: "0" }) : Promise.resolve({ code: 200, msg: "ok", total: 0, rows: [] });
    const thirdPartyWarehouseRequest = selectedSellerId && canQueryThirdPartyWarehouses ? getThirdPartyWarehouseList({ pageNum: 1, pageSize: 500, status: "0", sellerId: selectedSellerId }) : Promise.resolve({ code: 200, msg: "ok", total: 0, rows: [] });
    Promise.all([
      officialWarehouseRequest,
      thirdPartyWarehouseRequest
    ]).then(([officialWarehouseResp, thirdPartyWarehouseResp]) => {
      const options = [
        ...officialWarehouseResp.code === 200 ? officialWarehouseResp.rows || [] : [],
        ...thirdPartyWarehouseResp.code === 200 ? thirdPartyWarehouseResp.rows || [] : []
      ].map(toWarehouseOption).filter((item) => !!item);
      const boundWarehouses = selectedSellerId === product?.sellerId ? product?.warehouses : void 0;
      setWarehouseOptions(mergeWarehouseOptions(options, boundWarehouses));
    }).catch(() => {
      const boundWarehouses = selectedSellerId === product?.sellerId ? product?.warehouses : void 0;
      setWarehouseOptions(mergeWarehouseOptions([], boundWarehouses));
    });
  }, [canQueryOfficialWarehouses, canQueryThirdPartyWarehouses, product?.warehouses, selectedSellerId]);
  useEffect(() => {
    if (!spuId) {
      form.setFieldsValue({ spuStatus: "DRAFT" });
      return;
    }
    setLoading(true);
    getDistributionProduct(spuId).then((resp) => {
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
      setSelectedWarehouseIds(current.warehouseIds || (current.warehouses || []).map((item) => item.warehouseId).filter((warehouseId) => warehouseId != null));
      setWarehouseOptions((options) => mergeWarehouseOptions(options, current.warehouses));
      setGalleryUrls(
        (current.images || []).filter((item) => item.imageRole === "GALLERY" && !!item.imageUrl).map((item) => item.imageUrl)
      );
      if (current.categoryId) {
        loadSchema(current.categoryId);
      }
    }).finally(() => setLoading(false));
  }, [form, spuId]);
  const loadSchema = async (categoryId) => {
    const resp = await getCategorySchema(categoryId);
    setSchema(resp.data || []);
  };
  const handleCategoryChange = (categoryId) => {
    form.setFieldValue("attributeValueMap", {});
    if (categoryId) {
      loadSchema(categoryId);
    } else {
      setSchema([]);
    }
  };
  const handleSellerChange = () => {
    setSelectedWarehouseIds([]);
  };
  const handleWarehouseKindChange = (kind) => {
    if (!canChangeWarehouseKind && kind !== selectedWarehouseKind) {
      message.warning("\u4EC5\u8349\u7A3F\u5546\u54C1\u5141\u8BB8\u4FEE\u6539\u4ED3\u5E93\u7C7B\u578B");
      return;
    }
    setSelectedWarehouseKind(kind);
    setSelectedWarehouseIds([]);
    setSkuRows((currentRows) => {
      if (kind === "official") {
        return currentRows.filter((row) => !!row.sourceDimensionGroupKey || !!row.sourceSkuGroupKey);
      }
      if (selectedWarehouseKind === "official") {
        return [createEmptySkuRow()];
      }
      return currentRows.length ? currentRows : [createEmptySkuRow()];
    });
  };
  const selectedWarehouses = useMemo(
    () => selectedWarehouseIds.map((warehouseId) => warehouseOptions.find((item) => item.value === warehouseId)).filter(Boolean),
    [selectedWarehouseIds, warehouseOptions]
  );
  const derivedCurrencyCode = selectedWarehouses[0]?.currencyCode;
  const derivedCurrencyLabel = selectedWarehouses[0]?.currencyLabel;
  const isOfficialWarehouse = selectedWarehouseKind === "official";
  const selectedSourceSkuItems = useMemo(() => Object.values(selectedSourceSkuMap), [selectedSourceSkuMap]);
  const selectedSourceSkuKeys = useMemo(() => Object.keys(selectedSourceSkuMap), [selectedSourceSkuMap]);
  const handleWarehouseChange = (nextIds) => {
    const nextWarehouses = nextIds.map((warehouseId) => availableWarehouseOptions.find((item) => item.value === warehouseId)).filter(Boolean);
    if (nextIds.length !== nextWarehouses.length) {
      message.warning("\u8BF7\u9009\u62E9\u5F53\u524D\u4ED3\u5E93\u7C7B\u578B\u4E0B\u7684\u53D1\u8D27\u4ED3\u5E93");
      return;
    }
    if (nextWarehouses.some((item) => !item.currencyCode)) {
      message.warning("\u6240\u9009\u53D1\u8D27\u4ED3\u5E93\u672A\u7EF4\u62A4\u5E01\u79CD");
      return;
    }
    if (nextWarehouses.some((item) => !item.warehouseKind)) {
      message.warning("\u6240\u9009\u53D1\u8D27\u4ED3\u5E93\u672A\u7EF4\u62A4\u4ED3\u5E93\u7C7B\u578B");
      return;
    }
    const currencyCodes = new Set(nextWarehouses.map((item) => item.currencyCode));
    if (currencyCodes.size > 1) {
      message.warning("\u53D1\u8D27\u4ED3\u5E93\u5FC5\u987B\u9009\u62E9\u76F8\u540C\u5E01\u79CD");
      return;
    }
    const warehouseKinds = new Set(nextWarehouses.map((item) => item.warehouseKind));
    if (warehouseKinds.size > 1) {
      message.warning("\u5B98\u65B9\u4ED3\u548C\u4E09\u65B9\u4ED3\u4E0D\u80FD\u6DF7\u5728\u4E00\u8D77\u9009\u62E9");
      return;
    }
    setSelectedWarehouseIds(nextIds);
  };
  const openSourceSelector = () => {
    if (!canQuerySourceProducts) {
      message.warning("\u7F3A\u5C11\u6765\u6E90 SKU \u67E5\u8BE2\u6743\u9650");
      return;
    }
    const nextSelectedMap = {};
    skuRows.forEach((row) => {
      const sourceItem = toSourceItemFromSkuRow(row);
      const sourceKey = getSourceSkuKey(sourceItem);
      if (sourceItem && sourceKey) {
        nextSelectedMap[sourceKey] = sourceItem;
      }
    });
    setSelectedSourceSkuMap(nextSelectedMap);
    setSourceSelectorOpen(true);
  };
  const updateSelectedSourceSku = (item, selected) => {
    const sourceKey = getSourceSkuKey(item);
    if (!sourceKey || !item.sourceSkuGroupKey || !item.sourceDimensionGroupKey) {
      if (selected) {
        message.warning("\u6765\u6E90 SKU \u7F3A\u5C11\u7A33\u5B9A\u7ED1\u5B9A\u952E\uFF0C\u4E0D\u80FD\u9009\u62E9");
      }
      return;
    }
    setSelectedSourceSkuMap((current) => {
      const next = { ...current };
      if (selected) {
        next[sourceKey] = item;
      } else {
        delete next[sourceKey];
      }
      return next;
    });
  };
  const removeSelectedSourceSku = (sourceKey) => {
    setSelectedSourceSkuMap((current) => {
      const next = { ...current };
      delete next[sourceKey];
      return next;
    });
  };
  const clearSelectedSourceSkus = () => {
    setSelectedSourceSkuMap({});
  };
  const applySelectedSourceSkus = () => {
    if (!selectedSourceSkuItems.length) {
      message.warning("\u8BF7\u9009\u62E9\u6765\u6E90 SKU");
      return;
    }
    const currentSkuMap = /* @__PURE__ */ new Map();
    skuRows.forEach((row) => {
      const sourceKey = getSkuSourceKey(row);
      if (sourceKey) {
        currentSkuMap.set(sourceKey, row);
      }
    });
    const nextRows = selectedSourceSkuItems.map((item, index) => {
      const sourceKey = getSourceSkuKey(item);
      const sourceRow = toSourceSkuRow(item, index);
      const currentRow = currentSkuMap.get(sourceKey);
      return currentRow ? mergeSourceSkuRow(currentRow, sourceRow, index) : sourceRow;
    });
    setSkuRows(nextRows);
    setSourceSelectorOpen(false);
  };
  const sourceColumns = [
    {
      title: "\u5173\u952E\u8BCD",
      dataIndex: "keyword",
      hideInTable: true
    },
    {
      title: "\u6765\u6E90SKU",
      dataIndex: "masterSku",
      width: 160,
      ellipsis: true
    },
    {
      title: "\u6765\u6E90\u5546\u54C1",
      dataIndex: "masterProductName",
      width: 260,
      ellipsis: true
    },
    {
      title: "\u5C3A\u5BF8\u91CD\u91CF",
      dataIndex: "sourceDimensionGroupKey",
      width: 220,
      search: false,
      renderText: (_, record) => formatSourceDimension(record)
    },
    {
      title: "\u6765\u6E90\u4ED3",
      dataIndex: "sourceWarehouseNames",
      width: 220,
      ellipsis: true,
      search: false
    },
    {
      title: "\u4ED3\u5E93\u6570",
      dataIndex: "warehouseCount",
      width: 80,
      search: false
    },
    {
      title: "\u72B6\u6001",
      dataIndex: "pairingStatus",
      width: 90,
      search: false,
      renderText: (value) => value === "UNASSIGNED" ? "\u672A\u914D\u5BF9" : value === "PAIRED" ? "\u5DF2\u914D\u5BF9" : value || "-"
    }
  ];
  const buildAttributeValues = (values) => schema.map((item) => {
    const value = values.attributeValueMap?.[String(item.attributeId)];
    if (value === void 0 || value === null || value === "" || Array.isArray(value) && value.length === 0) return void 0;
    const base = {
      attributeId: item.attributeId,
      attributeCode: item.attributeCode,
      attributeName: item.attributeName,
      attributeType: item.attributeType
    };
    if (item.attributeType === "NUMBER") return { ...base, valueNumber: Number(value) };
    if (item.attributeType === "SINGLE_SELECT" || item.attributeType === "BOOLEAN") return { ...base, valueCode: String(value) };
    if (item.attributeType === "MULTI_SELECT") return { ...base, valueJson: JSON.stringify(value) };
    if (item.attributeType === "DATE") {
      return { ...base, valueDate: dayjs.isDayjs(value) ? value.format(ATTRIBUTE_DATE_FORMAT) : String(value) };
    }
    return { ...base, valueText: String(value) };
  }).filter(Boolean);
  const renderAttributeField = (item) => {
    const itemKey = item.attributeId;
    const name = ["attributeValueMap", String(item.attributeId)];
    const common = {
      name,
      label: item.attributeName,
      rules: item.requiredFlag === "Y" ? [{ required: true, message: `\u8BF7\u8F93\u5165${item.attributeName}` }] : void 0
    };
    if (item.attributeType === "NUMBER") {
      return /* @__PURE__ */ jsx(Form.Item, { ...common, children: /* @__PURE__ */ jsx(
        InputNumber,
        {
          suffix: item.unit || void 0,
          precision: item.valuePrecision,
          placeholder: item.placeholder || `\u8BF7\u8F93\u5165${item.attributeName || ""}`,
          style: { width: "100%" }
        }
      ) }, itemKey);
    }
    if (item.attributeType === "BOOLEAN") {
      return /* @__PURE__ */ jsx(Form.Item, { ...common, children: /* @__PURE__ */ jsx(
        Select,
        {
          allowClear: true,
          options: yesNoOptions,
          placeholder: item.placeholder || "\u8BF7\u9009\u62E9\u662F\u6216\u5426"
        }
      ) }, itemKey);
    }
    if (item.attributeType === "SINGLE_SELECT") {
      return /* @__PURE__ */ jsx(Form.Item, { ...common, children: /* @__PURE__ */ jsx(
        Select,
        {
          ...SEARCHABLE_SELECT_PROPS,
          allowClear: true,
          placeholder: item.placeholder || `\u8BF7\u9009\u62E9${item.attributeName || ""}`,
          options: (item.options || []).map((option) => ({ label: option.optionLabel, value: option.optionCode }))
        }
      ) }, itemKey);
    }
    if (item.attributeType === "MULTI_SELECT") {
      return /* @__PURE__ */ jsx(Form.Item, { ...common, children: /* @__PURE__ */ jsx(
        Select,
        {
          ...SEARCHABLE_SELECT_PROPS,
          mode: "multiple",
          placeholder: item.placeholder || `\u8BF7\u9009\u62E9${item.attributeName || ""}`,
          options: (item.options || []).map((option) => ({ label: option.optionLabel, value: option.optionCode }))
        }
      ) }, itemKey);
    }
    if (item.attributeType === "DATE") {
      return /* @__PURE__ */ jsx(Form.Item, { ...common, children: /* @__PURE__ */ jsx(
        DatePicker,
        {
          format: ATTRIBUTE_DATE_FORMAT,
          placeholder: item.placeholder || `\u8BF7\u9009\u62E9${item.attributeName || ""}`,
          style: { width: "100%" }
        }
      ) }, itemKey);
    }
    return /* @__PURE__ */ jsx(Form.Item, { ...common, children: /* @__PURE__ */ jsx(Input, { placeholder: item.placeholder || `\u8BF7\u8F93\u5165${item.attributeName || ""}` }) }, itemKey);
  };
  const submit = async (targetStatus) => {
    const values = await form.validateFields();
    if (!skuRows.length) {
      message.error("\u81F3\u5C11\u9700\u8981\u7EF4\u62A4\u4E00\u4E2A SKU");
      return;
    }
    if (!selectedWarehouseKind) {
      message.error("\u8BF7\u9009\u62E9\u4ED3\u5E93\u7C7B\u578B");
      return;
    }
    if (!isOfficialWarehouse && !selectedWarehouseIds.length) {
      message.error("\u8BF7\u9009\u62E9\u53D1\u8D27\u4ED3\u5E93");
      return;
    }
    const nextSpuStatus = isEdit ? product?.spuStatus || values.spuStatus || "DRAFT" : targetStatus || values.spuStatus || "DRAFT";
    const cleanSkus = stripSkuRows(skuRows).map((sku) => ({
      ...sku,
      currencyCode: isOfficialWarehouse ? sku.currencyCode : derivedCurrencyCode || sku.currencyCode,
      skuStatus: targetStatus === "READY" && (!sku.skuStatus || sku.skuStatus === "DRAFT") ? "READY" : sku.skuStatus || "DRAFT"
    }));
    if (isOfficialWarehouse && cleanSkus.some((sku) => !sku.sourceDimensionGroupKey)) {
      message.error("\u5B98\u65B9\u4ED3 SKU \u5FC5\u987B\u4ECE\u6765\u6E90\u5546\u54C1\u5E93\u9009\u62E9");
      return;
    }
    const invalidPriceSku = cleanSkus.find((sku) => sku.supplyPrice === void 0);
    if (invalidPriceSku) {
      message.error("\u8BF7\u8865\u9F50 SKU \u7684\u4F9B\u8D27\u4EF7");
      return;
    }
    const missingCurrencySku = !isOfficialWarehouse && cleanSkus.find((sku) => !sku.currencyCode);
    if (missingCurrencySku) {
      message.error("\u8BF7\u9009\u62E9\u53D1\u8D27\u4ED3\u5E93\u4EE5\u786E\u5B9A SKU \u5E01\u79CD");
      return;
    }
    setSaving(true);
    const payload = {
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
          imageRole: "GALLERY",
          sortOrder: index + 1
        }))
      ]
    };
    const resp = await (isEdit && spuId ? updateDistributionProduct(spuId, payload) : addDistributionProduct(payload)).finally(() => setSaving(false));
    if (resp.code === 200) {
      message.success(isEdit ? "\u5546\u54C1\u5DF2\u66F4\u65B0" : "\u5546\u54C1\u5DF2\u65B0\u589E");
      history.push("/product/distribution");
      return;
    }
    message.error(resp.msg || "\u4FDD\u5B58\u5931\u8D25");
  };
  const openBuyerPreview = () => {
    const values = form.getFieldsValue(true);
    const currencyCode = isOfficialWarehouse ? skuRows.find((row) => row.currencyCode)?.currencyCode : derivedCurrencyCode;
    setBuyerPreviewData({
      productName: values.productName || product?.productName || "\u5E73\u53F0\u7CBE\u9009\u73B0\u8D27\u5546\u54C1",
      productNameEn: values.productNameEn || product?.productNameEn || "Platform Ready Stock Product",
      sellingPoint: values.sellingPoint || product?.sellingPoint || "\u9002\u5408\u5206\u9500\u73B0\u8D27\u5C65\u7EA6\uFF0C\u652F\u6301\u591A\u89C4\u683C\u5FEB\u901F\u4E0B\u5355\u3002",
      categoryName: findCategoryName(categories, values.categoryId) || product?.categoryName,
      mainImageUrl: values.mainImageUrl || mainImageUrl,
      galleryUrls: galleryUrls.filter(Boolean),
      warehouseKind: selectedWarehouseKind,
      warehouses: buildPreviewWarehouses(selectedWarehouseKind, skuRows, selectedWarehouses),
      skus: buildPreviewSkus(skuRows, currencyCode),
      attributes: buildPreviewAttributes(values, schema),
      detailBlocks
    });
    setBuyerPreviewOpen(true);
  };
  return /* @__PURE__ */ jsx(PageContainer, { title: false, children: /* @__PURE__ */ jsxs("div", { className: styles.editPage, children: [
    /* @__PURE__ */ jsx("div", { className: styles.editHeader, children: /* @__PURE__ */ jsxs(Space, { children: [
      /* @__PURE__ */ jsx(Button, { icon: /* @__PURE__ */ jsx(ArrowLeftOutlined, {}), onClick: () => history.push("/product/distribution"), children: "\u8FD4\u56DE" }),
      /* @__PURE__ */ jsxs("div", { children: [
        /* @__PURE__ */ jsx("div", { className: styles.editTitle, children: isEdit ? "\u7F16\u8F91\u5546\u57CE\u5546\u54C1" : "\u65B0\u589E\u5546\u57CE\u5546\u54C1" }),
        /* @__PURE__ */ jsx("div", { className: styles.editSubtitle, children: "\u7EF4\u62A4 SPU \u4E3B\u4FE1\u606F\u3001\u5546\u54C1\u56FE\u7247\u3001\u7C7B\u76EE\u5C5E\u6027\u3001\u8BE6\u60C5\u56FE\u6587\u548C SKU \u77E9\u9635\u3002" })
      ] })
    ] }) }),
    isEdit ? /* @__PURE__ */ jsxs("div", { className: styles.readonlySummary, children: [
      /* @__PURE__ */ jsxs("span", { children: [
        "\u7CFB\u7EDF SPU\uFF1A",
        product?.systemSpuCode || "-"
      ] }),
      /* @__PURE__ */ jsxs("span", { children: [
        "\u6765\u6E90\uFF1A",
        product?.sourceType || "-"
      ] }),
      /* @__PURE__ */ jsxs("span", { children: [
        "SKU \u6570\uFF1A",
        product?.skuCount ?? skuRows.length
      ] })
    ] }) : null,
    /* @__PURE__ */ jsxs(Form, { form, layout: "vertical", className: styles.editForm, disabled: loading, children: [
      /* @__PURE__ */ jsxs("section", { className: styles.formSection, children: [
        /* @__PURE__ */ jsx("div", { className: styles.sectionTitle, children: "\u57FA\u7840\u4FE1\u606F" }),
        /* @__PURE__ */ jsxs("div", { className: styles.formGrid, children: [
          /* @__PURE__ */ jsx(Form.Item, { name: "productName", label: "\u5546\u54C1\u4E2D\u6587\u6807\u9898", rules: [{ required: true, message: "\u8BF7\u8F93\u5165\u5546\u54C1\u4E2D\u6587\u6807\u9898" }], children: /* @__PURE__ */ jsx(Input, { placeholder: "\u4F8B\u5982\uFF1A\u8F7B\u91CF\u900F\u6C14\u68D2\u7403\u5E3D" }) }),
          /* @__PURE__ */ jsx(Form.Item, { name: "productNameEn", label: "\u5546\u54C1\u82F1\u6587\u6807\u9898", rules: [{ required: true, message: "\u8BF7\u8F93\u5165\u5546\u54C1\u82F1\u6587\u6807\u9898" }], children: /* @__PURE__ */ jsx(Input, { placeholder: "\u4F8B\u5982\uFF1ALightweight Breathable Baseball Cap" }) }),
          /* @__PURE__ */ jsx(Form.Item, { name: "sellerSpuCode", label: "\u5BA2\u6237SPU", children: /* @__PURE__ */ jsx(Input, { placeholder: "\u5356\u5BB6\u81EA\u5DF1\u7684 SPU \u7F16\u7801" }) }),
          /* @__PURE__ */ jsx(Form.Item, { name: "sellerId", label: "\u7ED1\u5B9A\u5356\u5BB6", rules: [{ required: true, message: "\u8BF7\u9009\u62E9\u5356\u5BB6" }], children: /* @__PURE__ */ jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: sellerOptions, placeholder: "\u8BF7\u9009\u62E9\u5356\u5BB6", onChange: handleSellerChange }) }),
          /* @__PURE__ */ jsx(Form.Item, { name: "categoryId", label: "\u5546\u54C1\u5206\u7C7B", rules: [{ required: true, message: "\u8BF7\u9009\u62E9\u672B\u7EA7\u5546\u54C1\u5206\u7C7B" }], children: /* @__PURE__ */ jsx(
            TreeSelect,
            {
              ...SEARCHABLE_TREE_SELECT_PROPS,
              treeData: categoryTreeData,
              treeDefaultExpandAll: true,
              placeholder: "\u8BF7\u9009\u62E9\u672B\u7EA7\u53EF\u53D1\u5E03\u5206\u7C7B",
              onChange: handleCategoryChange
            }
          ) }),
          /* @__PURE__ */ jsx(Form.Item, { label: "\u4ED3\u5E93\u7C7B\u578B", required: true, children: /* @__PURE__ */ jsxs(
            Radio.Group,
            {
              value: selectedWarehouseKind,
              disabled: !canChangeWarehouseKind,
              onChange: (event) => handleWarehouseKindChange(event.target.value),
              children: [
                /* @__PURE__ */ jsx(Radio.Button, { value: "official", children: "\u5B98\u65B9\u4ED3" }),
                /* @__PURE__ */ jsx(Radio.Button, { value: "third_party", children: "\u4E09\u65B9\u4ED3" })
              ]
            }
          ) }),
          isOfficialWarehouse ? /* @__PURE__ */ jsx(Form.Item, { label: "\u53D1\u8D27\u4ED3\u5E93", children: /* @__PURE__ */ jsx(Input, { value: "\u7531\u6765\u6E90 SKU \u7684\u5B98\u65B9\u5C65\u7EA6\u4ED3\u81EA\u52A8\u6D3E\u751F", disabled: true }) }) : /* @__PURE__ */ jsx(Form.Item, { label: "\u53D1\u8D27\u4ED3\u5E93", required: true, children: /* @__PURE__ */ jsx(
            Select,
            {
              ...SEARCHABLE_SELECT_PROPS,
              mode: "multiple",
              value: selectedWarehouseIds,
              options: availableWarehouseOptions,
              placeholder: selectedWarehouseKind ? selectedWarehouseKind === "third_party" && !selectedSellerId ? "\u8BF7\u5148\u9009\u62E9\u5356\u5BB6" : "\u9009\u62E9\u540C\u5E01\u79CD\u7684\u53D1\u8D27\u4ED3\u5E93" : "\u8BF7\u5148\u9009\u62E9\u4ED3\u5E93\u7C7B\u578B",
              disabled: !selectedWarehouseKind || selectedWarehouseKind === "third_party" && !selectedSellerId,
              onChange: handleWarehouseChange
            }
          ) }),
          /* @__PURE__ */ jsx(Form.Item, { label: "\u5E01\u79CD", children: /* @__PURE__ */ jsx(Input, { value: isOfficialWarehouse ? "\u7531\u5B98\u65B9\u5C65\u7EA6\u4ED3\u6D3E\u751F" : derivedCurrencyLabel || derivedCurrencyCode || "-", disabled: true }) })
        ] }),
        /* @__PURE__ */ jsx(Form.Item, { name: "sellingPoint", label: "\u5546\u54C1\u5356\u70B9", children: /* @__PURE__ */ jsx(Input.TextArea, { rows: 2, placeholder: "\u7528\u4E8E\u5217\u8868\u6216\u8BE6\u60C5\u6458\u8981\u5C55\u793A" }) })
      ] }),
      /* @__PURE__ */ jsxs("section", { className: styles.formSection, children: [
        /* @__PURE__ */ jsx(
          ProductImageSection,
          {
            mainImageUrl,
            galleryUrls,
            onMainImageChange: (value) => form.setFieldValue("mainImageUrl", value),
            onGalleryChange: setGalleryUrls
          }
        ),
        /* @__PURE__ */ jsx(Form.Item, { name: "mainImageUrl", hidden: true, rules: [{ required: true, message: "\u8BF7\u4E0A\u4F20 SPU \u4E3B\u56FE" }], children: /* @__PURE__ */ jsx(Input, {}) })
      ] }),
      schema.length > 0 ? /* @__PURE__ */ jsxs("section", { className: styles.formSection, children: [
        /* @__PURE__ */ jsx("div", { className: styles.sectionTitle, children: "\u7C7B\u76EE\u5C5E\u6027" }),
        /* @__PURE__ */ jsx("div", { className: styles.formGrid, children: schema.map(renderAttributeField) })
      ] }) : null,
      /* @__PURE__ */ jsxs("section", { className: styles.formSection, children: [
        /* @__PURE__ */ jsx("div", { className: styles.sectionTitle, children: "\u8BE6\u60C5\u56FE\u6587" }),
        /* @__PURE__ */ jsx(DetailContentBuilder, { value: detailBlocks, onChange: setDetailBlocks })
      ] }),
      /* @__PURE__ */ jsxs("section", { className: styles.formSection, children: [
        isOfficialWarehouse ? /* @__PURE__ */ jsx("div", { className: styles.sourceSkuToolbar, children: /* @__PURE__ */ jsxs(Space, { children: [
          /* @__PURE__ */ jsx(Button, { type: "primary", disabled: !canQuerySourceProducts, onClick: openSourceSelector, children: "\u9009\u62E9\u6765\u6E90 SKU" }),
          /* @__PURE__ */ jsx(Typography.Text, { type: "secondary", children: "\u5B98\u65B9\u4ED3\u5546\u54C1\u7684\u5C3A\u5BF8\u91CD\u91CF\u548C\u53D1\u8D27\u4ED3\u5E93\u7531\u6765\u6E90 SKU \u6D3E\u751F\u3002" })
        ] }) }) : null,
        /* @__PURE__ */ jsx(
          SkuMatrixEditor,
          {
            value: skuRows,
            focusSkuId,
            currencyCode: isOfficialWarehouse ? void 0 : derivedCurrencyCode,
            currencyLabel: isOfficialWarehouse ? "\u7531\u5B98\u65B9\u5C65\u7EA6\u4ED3\u6D3E\u751F" : derivedCurrencyLabel,
            sourceMode: isOfficialWarehouse,
            onChange: setSkuRows
          }
        )
      ] })
    ] }),
    /* @__PURE__ */ jsx(Affix, { offsetBottom: 0, children: /* @__PURE__ */ jsx(Card, { size: "small", className: styles.editActionCard, children: /* @__PURE__ */ jsxs(Space, { children: [
      /* @__PURE__ */ jsx(Button, { onClick: () => history.push("/product/distribution"), children: "\u53D6\u6D88" }),
      /* @__PURE__ */ jsx(Button, { icon: /* @__PURE__ */ jsx(EyeOutlined, {}), onClick: openBuyerPreview, children: "\u9884\u89C8\u4E70\u5BB6\u89C6\u56FE" }),
      isEdit ? /* @__PURE__ */ jsx(Button, { type: "primary", loading: saving, icon: /* @__PURE__ */ jsx(SaveOutlined, {}), onClick: () => submit(), children: "\u4FDD\u5B58" }) : /* @__PURE__ */ jsxs(Fragment, { children: [
        /* @__PURE__ */ jsx(Button, { loading: saving, icon: /* @__PURE__ */ jsx(SaveOutlined, {}), onClick: () => submit("DRAFT"), children: "\u4FDD\u5B58\u8349\u7A3F" }),
        /* @__PURE__ */ jsx(Button, { type: "primary", loading: saving, onClick: () => submit("READY"), children: "\u4FDD\u5B58\u4E3A\u5F85\u4E0A\u67B6" })
      ] })
    ] }) }) }),
    /* @__PURE__ */ jsxs(
      Modal,
      {
        title: "\u9009\u62E9\u6765\u6E90 SKU",
        open: sourceSelectorOpen,
        width: 1120,
        okText: `\u786E\u8BA4\u9009\u62E9\uFF08${selectedSourceSkuItems.length}\uFF09`,
        cancelText: "\u53D6\u6D88",
        okButtonProps: { disabled: !selectedSourceSkuItems.length },
        destroyOnClose: true,
        onOk: applySelectedSourceSkus,
        onCancel: () => setSourceSelectorOpen(false),
        children: [
          /* @__PURE__ */ jsxs("div", { className: styles.sourceSkuSelectionBoard, children: [
            /* @__PURE__ */ jsxs("div", { className: styles.sourceSkuSelectionHeader, children: [
              /* @__PURE__ */ jsxs(Typography.Text, { strong: true, children: [
                "\u5DF2\u9009\u62E9 SKU\uFF08",
                selectedSourceSkuItems.length,
                "\uFF09"
              ] }),
              /* @__PURE__ */ jsx(Button, { type: "link", size: "small", disabled: !selectedSourceSkuItems.length, onClick: clearSelectedSourceSkus, children: "\u6E05\u7A7A" })
            ] }),
            selectedSourceSkuItems.length ? /* @__PURE__ */ jsx(Space, { wrap: true, size: [8, 8], children: selectedSourceSkuItems.map((item) => {
              const sourceKey = getSourceSkuKey(item);
              return /* @__PURE__ */ jsxs(
                Tag,
                {
                  closable: true,
                  className: styles.sourceSkuSelectionTag,
                  onClose: () => removeSelectedSourceSku(sourceKey),
                  children: [
                    item.masterSku || "-",
                    " / ",
                    item.masterProductName || "-"
                  ]
                },
                sourceKey
              );
            }) }) : /* @__PURE__ */ jsx(Typography.Text, { type: "secondary", children: "\u8DE8\u9875\u52FE\u9009\u4F1A\u4FDD\u7559\u5728\u8FD9\u91CC\uFF0C\u786E\u8BA4\u540E\u5199\u5165 SKU \u5217\u8868\u3002" })
          ] }),
          /* @__PURE__ */ jsx(
            ProTable,
            {
              rowKey: (record) => getSourceSkuKey(record),
              columns: sourceColumns,
              size: "small",
              search: { labelWidth: 70, span: 8 },
              options: false,
              pagination: { pageSize: 10 },
              tableAlertRender: false,
              tableAlertOptionRender: false,
              rowSelection: {
                preserveSelectedRowKeys: true,
                selectedRowKeys: selectedSourceSkuKeys,
                onSelect: (record, selected) => updateSelectedSourceSku(record, selected),
                onSelectAll: (selected, _selectedRows, changeRows) => {
                  changeRows.forEach((record) => updateSelectedSourceSku(record, selected));
                },
                getCheckboxProps: (record) => ({
                  disabled: !record.sourceSkuGroupKey || !record.sourceDimensionGroupKey
                })
              },
              request: async (params2) => {
                if (!canQuerySourceProducts) {
                  return {
                    data: [],
                    total: 0,
                    success: true
                  };
                }
                const resp = await getSourceProductList({
                  ...params2,
                  pageNum: params2.current,
                  pageSize: params2.pageSize,
                  repositoryScope: "OFFICIAL_MASTER",
                  status: "ACTIVE",
                  pairingStatus: "UNASSIGNED"
                });
                return {
                  data: resp.rows || [],
                  total: resp.total || 0,
                  success: resp.code === 200
                };
              }
            }
          )
        ]
      }
    ),
    /* @__PURE__ */ jsx(
      BuyerProductPreviewModal,
      {
        open: buyerPreviewOpen,
        data: buyerPreviewData,
        onClose: () => setBuyerPreviewOpen(false)
      }
    )
  ] }) });
}
export {
  ProductDistributionEditPage as default
};
