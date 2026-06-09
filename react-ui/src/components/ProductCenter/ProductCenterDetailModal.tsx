import BuyerProductPreviewModal, {
  type BuyerPreviewAttribute,
  type BuyerPreviewSku,
  type BuyerPreviewWarehouse,
  type BuyerProductPreviewData,
} from '@/pages/Product/Distribution/components/BuyerProductPreviewModal';
import { inventoryStatusText } from '@/pages/Product/Distribution/constants';
import { parseDetailContent } from '@/pages/Product/Distribution/detailContent';
import { useMemo } from 'react';

type ProductCenterDetailModalProps = {
  open: boolean;
  product?: API.ProductCenter.Product;
  onClose: () => void;
};

function formatSkuPrice(sku: API.ProductCenter.Sku, currencySummary?: string) {
  if (sku.salePrice === undefined || sku.salePrice === null) {
    return '--';
  }
  return [sku.salePrice, sku.currencyCode || currencySummary].filter(Boolean).join(' ');
}

function formatSkuStock(sku: API.ProductCenter.Sku) {
  if (sku.availableStock !== undefined && sku.availableStock !== null) {
    return `可用库存 ${sku.availableStock} 件`;
  }
  if (sku.inventoryStatus) {
    return inventoryStatusText[sku.inventoryStatus] || sku.inventoryStatus;
  }
  return '库存待同步';
}

function toPreviewSku(sku: API.ProductCenter.Sku, currencySummary?: string): BuyerPreviewSku {
  return {
    skuId: sku.skuId,
    spuId: sku.spuId,
    rowKey: sku.skuId ? String(sku.skuId) : sku.systemSkuCode,
    systemSkuCode: sku.systemSkuCode,
    color: sku.color,
    size: sku.size,
    material: sku.material,
    style: sku.style,
    model: sku.model,
    packageQuantity: sku.packageQuantity,
    capacity: sku.capacity,
    lengthValue: sku.lengthValue,
    widthValue: sku.widthValue,
    heightValue: sku.heightValue,
    weight: sku.weight,
    skuImageUrl: sku.skuImageUrl,
    salePrice: sku.salePrice,
    currencyCode: sku.currencyCode,
    previewPrice: formatSkuPrice(sku, currencySummary),
    previewStock: formatSkuStock(sku),
  };
}

function toPreviewWarehouse(
  warehouse: API.ProductCenter.Warehouse,
  index: number,
): BuyerPreviewWarehouse {
  const key = warehouse.id || warehouse.warehouseId || warehouse.warehouseCode || index;
  return {
    key: String(key),
    name: warehouse.warehouseName || warehouse.warehouseCode || '未命名仓库',
    code: warehouse.warehouseCode,
    kind: warehouse.warehouseKind,
    kindLabel: warehouse.warehouseKindLabel,
    currencyCode: warehouse.settlementCurrency,
    deliveryText: warehouse.deliveryText || '发货仓库待确认',
  };
}

function toPreviewAttributes(attributes?: API.ProductCenter.Attribute[]): BuyerPreviewAttribute[] {
  return (attributes || []).flatMap((attribute) => {
    const label = attribute.label?.trim();
    return label ? [{ label, value: attribute.value }] : [];
  });
}

function toPreviewData(product?: API.ProductCenter.Product): BuyerProductPreviewData | undefined {
  if (!product) {
    return undefined;
  }
  return {
    productName: product.productName,
    productNameEn: product.productNameEn,
    categoryName: product.categoryName,
    categoryPath: product.categoryName,
    sellingPoint: product.sellingPoint,
    mainImageUrl: product.mainImageUrl,
    galleryUrls: product.galleryUrls || [],
    warehouseKind: product.warehouseKindSummary,
    warehouses: (product.warehouses || []).map(toPreviewWarehouse),
    skus: (product.skus || []).map((sku) => toPreviewSku(sku, product.currencySummary)),
    attributes: toPreviewAttributes(product.attributes),
    detailBlocks: parseDetailContent(product.detailContent),
  };
}

export default function ProductCenterDetailModal({
  open,
  product,
  onClose,
}: ProductCenterDetailModalProps) {
  const data = useMemo(() => toPreviewData(product), [product]);

  return (
    <BuyerProductPreviewModal
      open={open}
      data={data}
      mode="real"
      footer={null}
      onClose={onClose}
    />
  );
}
