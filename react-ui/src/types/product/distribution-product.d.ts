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

  export interface ProductWarehouse {
    id?: number;
    spuId?: number;
    warehouseId?: number;
    warehouseCode?: string;
    warehouseName?: string;
    warehouseKind?: 'official' | 'third_party' | string;
    settlementCurrency?: string;
    sellerId?: number;
  }

  export interface Sku {
    skuId?: number;
    spuId?: number;
    sellerId?: number;
    systemSpuCode?: string;
    sellerSpuCode?: string;
    sellerName?: string;
    categoryId?: number;
    categoryName?: string;
    productName?: string;
    productNameEn?: string;
    spuStatus?: string;
    keyword?: string;
    systemSkuCode?: string;
    sellerSkuCode?: string;
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
    supplyPrice?: number;
    salePrice?: number;
    currencyCode?: string;
    warehouseKindSummary?: string;
    skuStatus?: string;
    controlStatus?: string;
    spuControlStatus?: string;
    controlReason?: string;
    controlBy?: string;
    controlTime?: string;
    recoverBy?: string;
    recoverTime?: string;
    sortOrder?: number;
    availableStock?: number;
    warehouseCount?: number;
    inventoryStatus?: string;
    stockUpdateTime?: string;
    latestReviewId?: number;
    latestReviewNo?: string;
    latestReviewStatus?: string;
    latestReviewFeedback?: string;
    latestReviewTime?: string;
    sourceBindingId?: number;
    sourceScope?: string;
    sourceSkuGroupKey?: string;
    sourceDimensionGroupKey?: string;
    masterSku?: string;
    masterProductNameSnapshot?: string;
    sourcePayloadHash?: string;
    wmsPayloadHash?: string;
    measureLengthCm?: number;
    measureWidthCm?: number;
    measureHeightCm?: number;
    measureWeightKg?: number;
    measureSource?: string;
    sourceWarehouseNames?: string;
    sourceWarehouseCount?: number;
    bindingStatus?: string;
    lockStatus?: string;
    lockedTime?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
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
    controlStatus?: string;
    controlReason?: string;
    controlBy?: string;
    controlTime?: string;
    recoverBy?: string;
    recoverTime?: string;
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
    warehouseKindSummary?: string;
    availableStock?: number;
    warehouseCount?: number;
    inventoryStatus?: string;
    stockUpdateTime?: string;
    latestReviewId?: number;
    latestReviewNo?: string;
    latestReviewStatus?: string;
    latestReviewFeedback?: string;
    latestReviewTime?: string;
    warehouseKind?: 'official' | 'third_party' | string;
    outboundWarehouseCodes?: string[];
    warehouseIds?: number[];
    warehouses?: ProductWarehouse[];
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

  export interface SkuPageResult {
    code: number;
    msg: string;
    total: number;
    rows: Sku[];
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

  export interface OperationLog {
    logId?: number;
    batchNo?: string;
    operationType?: string;
    ownerType?: string;
    spuId?: number;
    skuId?: number;
    systemSpuCode?: string;
    systemSkuCode?: string;
    sellerId?: number;
    sellerName?: string;
    beforeSalesStatus?: string;
    afterSalesStatus?: string;
    beforeControlStatus?: string;
    afterControlStatus?: string;
    beforeSalePrice?: number;
    afterSalePrice?: number;
    currencyCode?: string;
    reason?: string;
    changeSummary?: string;
    diffJson?: string;
    operatorName?: string;
    operationTime?: string;
    operationSource?: string;
    keyword?: string;
    operationTypes?: string;
    operationTimeRange?: string[];
    beginTime?: string;
    endTime?: string;
    remark?: string;
  }

  export interface OperationLogPageResult {
    code: number;
    msg: string;
    total: number;
    rows: OperationLog[];
  }
}
