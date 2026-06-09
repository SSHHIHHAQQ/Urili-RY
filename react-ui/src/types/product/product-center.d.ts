declare namespace API.ProductCenter {
  export interface Attribute {
    label?: string;
    value?: string;
  }

  export interface Warehouse {
    id?: number;
    warehouseId?: number;
    warehouseCode?: string;
    warehouseName?: string;
    warehouseKind?: 'official' | 'third_party' | 'MIXED' | string;
    warehouseKindLabel?: string;
    settlementCurrency?: string;
    deliveryText?: string;
  }

  export interface Sku {
    skuId?: number;
    spuId?: number;
    systemSkuCode?: string;
    color?: string;
    size?: string;
    lengthValue?: string;
    widthValue?: string;
    heightValue?: string;
    weight?: string;
    material?: string;
    style?: string;
    model?: string;
    packageQuantity?: string;
    capacity?: string;
    skuImageUrl?: string;
    salePrice?: number;
    currencyCode?: string;
    availableStock?: number;
    warehouseCount?: number;
    inventoryStatus?: string;
    stockUpdateTime?: string;
    skuStatus?: string;
    sortOrder?: number;
  }

  export interface Product {
    spuId?: number;
    systemSpuCode?: string;
    categoryId?: number;
    categoryCode?: string;
    categoryName?: string;
    productName?: string;
    productNameEn?: string;
    sellingPoint?: string;
    mainImageUrl?: string;
    detailContent?: string;
    spuStatus?: string;
    skuCount?: number;
    visibleSystemSkuCodes?: string[];
    salePriceMin?: number;
    salePriceMax?: number;
    currencySummary?: string;
    warehouseKindSummary?: string;
    availableStock?: number;
    warehouseCount?: number;
    inventoryStatus?: string;
    stockUpdateTime?: string;
    galleryUrls?: string[];
    skus?: Sku[];
    warehouses?: Warehouse[];
    attributes?: Attribute[];
  }

  export interface PageResult {
    code: number;
    msg: string;
    total: number;
    rows: Product[];
  }

  export interface InfoResult {
    code: number;
    msg: string;
    data: Product;
  }

  export interface SkuListResult {
    code: number;
    msg: string;
    data: Sku[];
  }
}
