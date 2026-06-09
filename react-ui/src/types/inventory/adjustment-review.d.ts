declare namespace API.InventoryAdjustmentReview {
  export interface Review {
    reviewId?: number;
    reviewNo?: string;
    reviewStatus?: string;
    policyId?: number;
    policySnapshotJson?: string;
    stockId?: number;
    stockKey?: string;
    spuId?: number;
    skuId?: number;
    sellerId?: number;
    systemSkuCode?: string;
    productName?: string;
    skuName?: string;
    warehouseKind?: string;
    warehouseRefType?: string;
    warehouseName?: string;
    adjustField?: string;
    adjustDirection?: string;
    requestBeforePlatformTotalQty?: number;
    requestedAdjustQty?: number;
    requestExpectedAfterPlatformTotalQty?: number;
    platformReservedQtySnapshot?: number;
    sales7dQty?: number;
    sales7dDailyAvg?: number;
    sales30dQty?: number;
    sales30dDailyAvg?: number;
    thresholdDailyAvg?: number;
    thresholdReserveDays?: number;
    protectedRetainedQty?: number;
    minRetainedQty?: number;
    immediateReturnableQty?: number;
    triggerReason?: string;
    submitTerminal?: string;
    submitUserId?: number;
    submitUserName?: string;
    submitReason?: string;
    submitTime?: string;
    plannedEffectiveTime?: string;
    effectiveTime?: string;
    effectiveOperatorId?: number;
    effectiveOperatorName?: string;
    effectiveBeforePlatformTotalQty?: number;
    actualEffectQty?: number;
    unfulfilledQty?: number;
    effectiveAfterPlatformTotalQty?: number;
    reviewReason?: string;
    version?: number;
    keyword?: string;
    submitTimeRange?: string[];
    submitTimeStart?: string;
    submitTimeEnd?: string;
    plannedEffectiveTimeRange?: string[];
    plannedEffectiveTimeStart?: string;
    plannedEffectiveTimeEnd?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface OperationLog {
    logId?: number;
    reviewId?: number;
    reviewNo?: string;
    operationType?: string;
    beforeStatus?: string;
    afterStatus?: string;
    operationReason?: string;
    operatorId?: number;
    operatorName?: string;
    operateTime?: string;
    changeSummary?: string;
    createTime?: string;
    remark?: string;
  }

  export interface Policy {
    policyId?: number;
    policyName?: string;
    policyStatus?: string;
    reviewMode?: string;
    directionScope?: string;
    fieldScope?: string;
    salesWindowDays?: string;
    salesAggregateMode?: string;
    reserveDays?: number;
    cooldownHours?: number;
    minReturnQtyToReview?: number;
    minReturnRatioToReview?: number;
    autoEffectEnabled?: string;
    manualEffectAllowed?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface PolicyBinding {
    bindingId?: number;
    policyId?: number;
    policyName?: string;
    bindingType?: string;
    bindingIdValue?: number;
    priority?: number;
    status?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
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

  export interface PolicyPageResult {
    code: number;
    msg: string;
    total: number;
    rows: Policy[];
  }

  export interface PolicyBindingPageResult {
    code: number;
    msg: string;
    total: number;
    rows: PolicyBinding[];
  }
}
