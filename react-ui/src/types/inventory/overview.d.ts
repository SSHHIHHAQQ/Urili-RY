declare namespace API.InventoryOverview {
  export interface OverviewItem {
    stockKey?: string;
    spuId?: number;
    skuId?: number;
    sellerId?: number;
    sellerNo?: string;
    sellerName?: string;
    systemSpuCode?: string;
    systemSkuCode?: string;
    productName?: string;
    mainImageUrl?: string;
    skuName?: string;
    skuImageUrl?: string;
    skuCount?: number;
    warehouseKindSummary?: string;
    warehouseCount?: number;
    platformTotalQty?: number;
    platformAvailableQty?: number;
    platformReservedQty?: number;
    platformInTransitQty?: number;
    sourceTotalQty?: number;
    sourceAvailableQty?: number;
    sourceInTransitQty?: number;
    inventoryStatus?: string;
    syncModeSummary?: string;
    syncPolicyScopeSummary?: string;
    latestSourceSnapshotTime?: string;
    latestStockUpdateTime?: string;
    keyword?: string;
    pairingStatus?: string;
    warehouseKey?: string;
  }

  export interface WarehouseStock {
    stockId?: number;
    stockKey?: string;
    spuId?: number;
    skuId?: number;
    sellerId?: number;
    sellerNo?: string;
    sellerName?: string;
    systemSkuCode?: string;
    productName?: string;
    skuName?: string;
    skuImageUrl?: string;
    warehouseKind?: string;
    warehouseRefType?: string;
    warehouseId?: number;
    warehouseCode?: string;
    warehouseName?: string;
    sourceScope?: string;
    sourceMasterWarehouseName?: string;
    sourceTotalQty?: number;
    sourceAvailableQty?: number;
    sourceInTransitQty?: number;
    sourceSnapshotTime?: string;
    platformTotalQty?: number;
    platformReservedQty?: number;
    platformInTransitQty?: number;
    pendingAvailableInboundQty?: number;
    pendingSourceDeductionQty?: number;
    platformAvailableQty?: number;
    effectiveStatus?: string;
    syncMode?: string;
    syncPolicyId?: number;
    syncPolicyScope?: string;
    syncPolicyKey?: string;
    syncStatus?: string;
    lastAutoSyncTime?: string;
    version?: number;
    calcTime?: string;
    updateTime?: string;
    remark?: string;
    keyword?: string;
    pairingStatus?: string;
    warehouseKey?: string;
  }

  export interface SkuWarehouseGroup {
    sku?: OverviewItem;
    warehouses?: WarehouseStock[];
  }

  export interface WarehouseOption {
    label?: string;
    value?: string;
    warehouseKind?: string;
    warehouseRefType?: string;
    warehouseId?: number;
    warehouseCode?: string;
    warehouseName?: string;
    searchText?: string;
  }

  export interface SellerOption {
    label?: string;
    value?: number;
    sellerId?: number;
    sellerNo?: string;
    sellerName?: string;
    searchText?: string;
  }

  export interface Ledger {
    ledgerId?: number;
    stockId?: number;
    stockKey?: string;
    spuId?: number;
    skuId?: number;
    sellerId?: number;
    syncPolicyId?: number;
    syncPolicyScope?: string;
    syncPolicyKey?: string;
    warehouseKind?: string;
    warehouseRefType?: string;
    warehouseName?: string;
    operationType?: string;
    operationSource?: string;
    bizType?: string;
    bizNo?: string;
    deltaQty?: number;
    beforePlatformTotalQty?: number;
    afterPlatformTotalQty?: number;
    beforeAvailableQty?: number;
    afterAvailableQty?: number;
    beforeReservedQty?: number;
    afterReservedQty?: number;
    beforeInTransitQty?: number;
    afterInTransitQty?: number;
    riskConfirmed?: string;
    riskMessage?: string;
    reason?: string;
    operatorId?: number;
    operatorName?: string;
    operateTime?: string;
  }

  export interface AdjustPreview {
    allowed?: boolean;
    confirmationRequired?: boolean;
    message?: string;
    beforeValue?: number;
    afterValue?: number;
    beforeAvailableQty?: number;
    afterAvailableQty?: number;
    reservedQty?: number;
    reviewRequired?: boolean;
    reviewId?: number;
    reviewNo?: string;
    requestedAdjustQty?: number;
    protectedRetainedQty?: number;
    minRetainedQty?: number;
    immediateReturnableQty?: number;
    plannedEffectiveTime?: string;
  }

  export interface BatchAdjustItem {
    stockId?: number;
    targetPlatformTotalQty?: number;
    targetPlatformInTransitQty?: number;
  }

  export interface BatchAdjustRowPreview {
    stockId?: number;
    systemSkuCode?: string;
    warehouseName?: string;
    allowed?: boolean;
    message?: string;
    beforePlatformTotalQty?: number;
    afterPlatformTotalQty?: number;
    platformTotalDeltaQty?: number;
    beforePlatformInTransitQty?: number;
    afterPlatformInTransitQty?: number;
    platformInTransitDeltaQty?: number;
    beforeAvailableQty?: number;
    afterAvailableQty?: number;
    reviewRequired?: boolean;
    requestedAdjustQty?: number;
    immediateReturnableQty?: number;
  }

  export interface BatchAdjustPreview {
    allowed?: boolean;
    confirmationRequired?: boolean;
    message?: string;
    changedRowCount?: number;
    beforePlatformTotalQty?: number;
    afterPlatformTotalQty?: number;
    beforePlatformInTransitQty?: number;
    afterPlatformInTransitQty?: number;
    beforeAvailableQty?: number;
    afterAvailableQty?: number;
    reviewRequiredCount?: number;
    rows?: BatchAdjustRowPreview[];
  }

  export type SyncPolicyScope = 'SELLER' | 'WAREHOUSE' | 'SPU' | 'SKU' | 'SKU_WAREHOUSE';
  export type SyncMode = 'MANUAL' | 'AUTO_SOURCE_AVAILABLE';

  export interface SyncPolicyRequest {
    scopeType?: SyncPolicyScope;
    syncMode?: SyncMode;
    sellerId?: number;
    warehouseKey?: string;
    warehouseKeys?: string[];
    warehouseName?: string;
    spuId?: number;
    skuId?: number;
    stockId?: number;
    confirmed?: boolean;
    remark?: string;
  }

  export interface SyncPolicyPreviewRow {
    stockId?: number;
    spuId?: number;
    skuId?: number;
    sellerId?: number;
    sellerName?: string;
    systemSkuCode?: string;
    productName?: string;
    skuName?: string;
    warehouseName?: string;
    warehouseKind?: string;
    warehouseRefType?: string;
    eligible?: boolean;
    changed?: boolean;
    message?: string;
    beforeSyncMode?: string;
    afterSyncMode?: string;
    beforePolicyScope?: string;
    afterPolicyScope?: string;
    sourceAvailableQty?: number;
    pendingSourceDeductionQty?: number;
    beforePlatformTotalQty?: number;
    afterPlatformTotalQty?: number;
    platformTotalDeltaQty?: number;
    beforeAvailableQty?: number;
    afterAvailableQty?: number;
    reservedQty?: number;
    syncStatus?: string;
  }

  export interface SyncPolicyPreview {
    allowed?: boolean;
    confirmationRequired?: boolean;
    message?: string;
    scopeType?: string;
    syncMode?: string;
    affectedRowCount?: number;
    eligibleRowCount?: number;
    skippedRowCount?: number;
    changedRowCount?: number;
    beforePlatformTotalQty?: number;
    afterPlatformTotalQty?: number;
    beforeAvailableQty?: number;
    afterAvailableQty?: number;
    rows?: SyncPolicyPreviewRow[];
  }

  export interface PageResult {
    code: number;
    msg: string;
    total: number;
    rows: OverviewItem[];
  }

  export interface WarehouseResult {
    code: number;
    msg: string;
    data: WarehouseStock[];
  }

  export interface WarehousePageResult {
    code: number;
    msg: string;
    total: number;
    rows: WarehouseStock[];
  }

  export interface WarehouseOptionResult {
    code: number;
    msg: string;
    data: WarehouseOption[];
  }

  export interface SellerOptionResult {
    code: number;
    msg: string;
    data: SellerOption[];
  }

  export interface SkuWarehouseGroupResult {
    code: number;
    msg: string;
    data: SkuWarehouseGroup[];
  }

  export interface AdjustPreviewResult {
    code: number;
    msg: string;
    data: AdjustPreview;
  }

  export interface BatchAdjustPreviewResult {
    code: number;
    msg: string;
    data: BatchAdjustPreview;
  }

  export interface SyncPolicyPreviewResult {
    code: number;
    msg: string;
    data: SyncPolicyPreview;
  }

  export interface LedgerPageResult {
    code: number;
    msg: string;
    total: number;
    rows: Ledger[];
  }
}
