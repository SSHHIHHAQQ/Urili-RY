declare namespace API.ProductDistribution {
  export interface AttributeValue {
    valueId?: number;
    ownerType?: string;
    ownerId?: number;
    spuId?: number;
    categoryId?: number;
    categorySchemaVersion?: number;
    attributeId?: number;
    attributeCode?: string;
    attributeName?: string;
    attributeType?: string;
    valueCode?: string;
    valueText?: string;
    valueNumber?: number;
    valueDate?: string;
    valueJson?: string;
  }

  export interface ProductImage {
    imageId?: number;
    ownerType?: string;
    ownerId?: number;
    spuId?: number;
    skuId?: number;
    imageUrl?: string;
    imageRole?: string;
    sortOrder?: number;
  }

  export interface Sku {
    skuId?: number;
    spuId?: number;
    sellerId?: number;
    systemSkuCode?: string;
    sellerSkuCode?: string;
    color?: string;
    size?: string;
    weight?: string;
    material?: string;
    style?: string;
    model?: string;
    packageQuantity?: string;
    capacity?: string;
    skuImageUrl?: string;
    supplyPrice?: number;
    salePrice?: number;
    currencyCode?: string;
    skuStatus?: string;
    sortOrder?: number;
    warehouseCount?: number;
    remark?: string;
  }

  export interface Spu {
    spuId?: number;
    systemSpuCode?: string;
    sellerSpuCode?: string;
    sellerId?: number;
    sellerNo?: string;
    sellerName?: string;
    categoryId?: number;
    categoryCode?: string;
    categoryName?: string;
    productName?: string;
    productNameEn?: string;
    sellingPoint?: string;
    mainImageUrl?: string;
    detailContent?: string;
    spuStatus?: string;
    sourceType?: string;
    sourceRefType?: string;
    sourceRefId?: string;
    keyword?: string;
    systemSkuCode?: string;
    sellerSkuCode?: string;
    skuCount?: number;
    supplyPriceMin?: number;
    supplyPriceMax?: number;
    salePriceMin?: number;
    salePriceMax?: number;
    currencySummary?: string;
    warehouseCount?: number;
    outboundWarehouseCodes?: string[];
    skus?: Sku[];
    attributeValues?: AttributeValue[];
    images?: ProductImage[];
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface PageResult {
    code: number;
    msg: string;
    total: number;
    rows: Spu[];
  }

  export interface InfoResult {
    code: number;
    msg: string;
    data: Spu;
  }

  export interface SkuListResult {
    code: number;
    msg: string;
    data: Sku[];
  }
}
