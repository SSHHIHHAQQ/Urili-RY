declare namespace API.ProductReview {
  export interface Item {
    itemId?: number;
    reviewId?: number;
    itemType?: string;
    changeType?: string;
    spuId?: number;
    skuId?: number;
    systemSkuCode?: string;
    sellerSkuCode?: string;
    itemStatus?: string;
    beforeHash?: string;
    afterHash?: string;
    diffSummary?: string;
    riskSummary?: string;
    sortOrder?: number;
    createTime?: string;
  }

  export interface Snapshot {
    snapshotId?: number;
    reviewId?: number;
    itemId?: number;
    snapshotRole?: string;
    payloadType?: string;
    payloadJson?: string;
    payloadHash?: string;
    createTime?: string;
  }

  export interface OperationLog {
    logId?: number;
    reviewId?: number;
    spuId?: number;
    operationType?: string;
    beforeStatus?: string;
    afterStatus?: string;
    operatorTerminal?: string;
    operatorId?: number;
    operatorName?: string;
    operationTime?: string;
    reason?: string;
    remark?: string;
  }

  export interface Review {
    reviewId?: number;
    reviewNo?: string;
    reviewType?: string;
    reviewStatus?: string;
    spuId?: number;
    systemSpuCode?: string;
    sellerId?: number;
    sellerName?: string;
    categoryId?: number;
    categoryName?: string;
    productNameBefore?: string;
    productNameAfter?: string;
    mainImageUrlBefore?: string;
    mainImageUrlAfter?: string;
    submitTerminal?: string;
    submitSubjectId?: number;
    submitAccountId?: number;
    submitUserName?: string;
    submitTime?: string;
    reviewerId?: number;
    reviewerName?: string;
    reviewTime?: string;
    reviewReason?: string;
    riskLevel?: string;
    riskSummary?: string;
    itemCount?: number;
    skuCount?: number;
    priceBeforeMin?: number;
    priceBeforeMax?: number;
    priceAfterMin?: number;
    priceAfterMax?: number;
    currencySummary?: string;
    warehouseSummary?: string;
    diffSummary?: string;
    activePendingKey?: string;
    keyword?: string;
    reviewTypes?: string;
    submitTimeRange?: string[];
    beginTime?: string;
    endTime?: string;
    items?: Item[];
    snapshots?: Snapshot[];
    logs?: OperationLog[];
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
    rows: Review[];
  }

  export interface InfoResult {
    code: number;
    msg: string;
    data: Review;
  }

  export interface LogListResult {
    code: number;
    msg: string;
    data: OperationLog[];
  }
}
