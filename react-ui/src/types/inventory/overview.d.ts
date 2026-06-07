declare namespace API.InventoryOverview {
  export interface OverviewItem {
    stockKey?: string;
    spuId?: number;
    skuId?: number;
    sellerId?: number;
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
    latestSourceSnapshotTime?: string;
    latestStockUpdateTime?: string;
    keyword?: string;
  }

  export interface WarehouseStock {
    stockId?: number;
    stockKey?: string;
    spuId?: number;
    skuId?: number;
    sellerId?: number;
    systemSkuCode?: string;
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
    version?: number;
    calcTime?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface Ledger {
    ledgerId?: number;
    stockId?: number;
    stockKey?: string;
    spuId?: number;
    skuId?: number;
    sellerId?: number;
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

  export interface AdjustPreviewResult {
    code: number;
    msg: string;
    data: AdjustPreview;
  }

  export interface LedgerPageResult {
    code: number;
    msg: string;
    total: number;
    rows: Ledger[];
  }
}
