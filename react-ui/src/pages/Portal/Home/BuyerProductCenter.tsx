import ProductCenterPage from '@/components/ProductCenter/ProductCenterPage';
import {
  getBuyerPortalProductCenterProduct,
  getBuyerPortalProductCenterProductSkus,
  getBuyerPortalProductCenterProducts,
} from '@/services/portal/session';
import { matchPermission } from '@/utils/permission';

type BuyerProductCenterProps = {
  permissions: string[];
};

function hasPermission(permissions: string[], permission: string) {
  return matchPermission(permissions, permission);
}

function mapSku(sku: Partial<API.Partner.BuyerPortalProductSku>): API.ProductCenter.Sku {
  return {
    skuId: sku.skuId,
    spuId: sku.spuId,
    systemSkuCode: sku.systemSkuCode,
    color: sku.color,
    size: sku.size,
    lengthValue: sku.lengthValue,
    widthValue: sku.widthValue,
    heightValue: sku.heightValue,
    weight: sku.weight,
    material: sku.material,
    style: sku.style,
    model: sku.model,
    packageQuantity: sku.packageQuantity,
    capacity: sku.capacity,
    skuImageUrl: sku.skuImageUrl,
    salePrice: sku.salePrice,
    currencyCode: sku.currencyCode,
    warehouseCount: sku.warehouseCount,
    skuStatus: sku.skuStatus,
    sortOrder: sku.sortOrder,
  };
}

function mapProduct(product: Partial<API.Partner.BuyerPortalProduct>): API.ProductCenter.Product {
  const skus = (product.skus || []).map(mapSku);
  return {
    spuId: product.spuId,
    systemSpuCode: product.systemSpuCode,
    categoryId: product.categoryId,
    categoryCode: product.categoryCode,
    categoryName: product.categoryName,
    productName: product.productName,
    productNameEn: product.productNameEn,
    sellingPoint: product.sellingPoint,
    mainImageUrl: product.mainImageUrl,
    detailContent: product.detailContent,
    spuStatus: product.spuStatus,
    skuCount: product.skuCount,
    visibleSystemSkuCodes: skus.flatMap((sku) => (sku.systemSkuCode ? [sku.systemSkuCode] : [])),
    salePriceMin: product.salePriceMin,
    salePriceMax: product.salePriceMax,
    currencySummary: product.currencySummary,
    warehouseCount: product.warehouseCount,
    skus,
  };
}

async function fetchBuyerProductCenterList(params?: Record<string, any>): Promise<API.ProductCenter.PageResult> {
  const response = await getBuyerPortalProductCenterProducts(params);
  return {
    code: response.code,
    msg: response.msg,
    total: response.total,
    rows: (response.rows || []).map(mapProduct),
  };
}

async function fetchBuyerProductCenterProduct(spuId: number): Promise<API.ProductCenter.InfoResult> {
  const [productResponse, skuResponse] = await Promise.all([
    getBuyerPortalProductCenterProduct(spuId),
    getBuyerPortalProductCenterProductSkus(spuId),
  ]);
  if (productResponse.code !== 200) {
    return {
      code: productResponse.code,
      msg: productResponse.msg,
      data: mapProduct(productResponse.data || {}),
    };
  }
  if (skuResponse.code !== 200) {
    return {
      code: skuResponse.code,
      msg: skuResponse.msg,
      data: mapProduct(productResponse.data || {}),
    };
  }
  return {
    code: productResponse.code,
    msg: productResponse.msg,
    data: mapProduct({
      ...(productResponse.data || {}),
      skus: skuResponse.data || productResponse.data?.skus || [],
    }),
  };
}

export default function BuyerProductCenter({ permissions }: BuyerProductCenterProps) {
  return (
    <ProductCenterPage
      canList={hasPermission(permissions, 'buyer:product:center:list')}
      canQuery={hasPermission(permissions, 'buyer:product:center:query')}
      fetchList={fetchBuyerProductCenterList}
      fetchProduct={fetchBuyerProductCenterProduct}
      storageKey="buyer-product-center"
    />
  );
}
