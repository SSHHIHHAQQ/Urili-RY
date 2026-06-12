declare namespace API.Integration {
  export interface UpstreamConnection {
    connectionCode: string;
    systemKind?: string;
    masterWarehouseName: string;
    settlementType: string;
    appKeyMask?: string;
    appSecretMask?: string;
    status?: string;
    credentialStatus?: string;
    enabledCapabilities?: string;
    displayOrder?: number;
    lastAuthorizedTime?: string;
    lastSyncTime?: string;
    requestLogCount?: number;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface ConnectionPageResult {
    code: number;
    msg: string;
    total: number;
    rows: UpstreamConnection[];
  }

  export interface ConnectionInfoResult {
    code: number;
    msg: string;
    data: UpstreamConnection;
  }

  export interface ConnectionSaveRequest {
    connectionCode?: string;
    systemKind: string;
    masterWarehouseName: string;
    settlementType: string;
    appKey: string;
    appSecret: string;
    remark?: string;
  }

  export interface ConnectionInfoRequest {
    masterWarehouseName: string;
    settlementType: string;
    remark?: string;
  }

  export interface CredentialRequest {
    appKey: string;
    appSecret: string;
  }

  export interface SyncResult {
    warehouseCount: number;
    logisticsChannelCount: number;
    skuCount: number;
    skuDimensionCount: number;
    warehouseStockCount: number;
    syncBatchId: string;
    requestNo?: string;
    requestId?: number;
    taskCount?: number;
    items?: SyncItemResult[];
  }

  export interface SyncItemResult {
    syncType?: string;
    taskId?: number;
    syncBatchId?: string;
    status?: string;
    count?: number;
    pulledCount?: number;
    insertedCount?: number;
    changedCount?: number;
    unchangedCount?: number;
    disabledCount?: number;
    errorMessage?: string;
  }

  export interface SyncRequest {
    syncTypes: string[];
  }

  export interface SkuDimensionSelectedSyncRequest {
    skuList: string[];
  }

  export interface WarehouseSyncItem {
    connectionCode: string;
    warehouseCode: string;
    warehouseName: string;
    countryCode?: string;
    status?: string;
    syncBatchId?: string;
    firstSeenTime?: string;
    lastSeenTime?: string;
    updateTime?: string;
  }

  export interface WarehousePairing {
    warehousePairingId: number;
    connectionCode: string;
    upstreamWarehouseCode: string;
    upstreamWarehouseName: string;
    systemWarehouseCode: string;
    systemWarehouseName: string;
    pairingRole?: 'FULFILLMENT' | 'QUOTE' | string;
    status?: string;
    createTime?: string;
    remark?: string;
  }

  export interface WarehousePairingRequest {
    upstreamWarehouseCode: string;
    systemWarehouseCode: string;
    systemWarehouseName: string;
    pairingRole?: 'FULFILLMENT' | 'QUOTE' | string;
    remark?: string;
  }

  export interface LogisticsChannelSyncItem {
    connectionCode: string;
    warehouseCode: string;
    channelCode: string;
    channelName: string;
    status?: string;
    syncBatchId?: string;
    firstSeenTime?: string;
    lastSeenTime?: string;
    updateTime?: string;
  }

  export interface LogisticsChannelPairing {
    logisticsChannelPairingId: number;
    connectionCode: string;
    systemWarehouseCode?: string;
    upstreamWarehouseCode?: string;
    upstreamChannelCode: string;
    upstreamChannelName: string;
    systemChannelCode: string;
    systemChannelName: string;
    pairingRole?: 'FULFILLMENT' | 'QUOTE' | string;
    status?: string;
    createTime?: string;
    remark?: string;
  }

  export interface LogisticsChannelPairingRequest {
    upstreamChannelCode: string;
    systemChannelCode: string;
    systemChannelName: string;
    pairingRole?: 'FULFILLMENT' | 'QUOTE' | string;
    remark?: string;
  }

  export interface SkuSyncItem {
    connectionCode: string;
    masterSku: string;
    masterProductName: string;
    length?: number;
    width?: number;
    height?: number;
    weight?: number;
    wmsLength?: number;
    wmsWidth?: number;
    wmsHeight?: number;
    wmsWeight?: number;
    status?: string;
    pairingStatus?: string;
    skuPairingId?: number;
    systemSku?: string;
    systemSkuName?: string;
    customerName?: string;
    syncBatchId?: string;
    lastSeenTime?: string;
    updateTime?: string;
  }

  export interface SkuSyncState {
    connectionCode: string;
    status?: string;
    syncBatchId?: string;
    lastStartedTime?: string;
    lastFinishedTime?: string;
    lastSuccessTime?: string;
    lastErrorMessage?: string;
    nextSyncTime?: string;
    updateTime?: string;
  }

  export interface InventorySyncState {
    connectionCode: string;
    status?: string;
    syncBatchId?: string;
    lastStartedTime?: string;
    lastFinishedTime?: string;
    lastSuccessTime?: string;
    nextSyncTime?: string;
    totalCount?: number;
    activeCount?: number;
    missingCount?: number;
    lastErrorMessage?: string;
    updateTime?: string;
  }

  export interface SyncState {
    connectionCode: string;
    syncType: string;
    status?: string;
    syncBatchId?: string;
    lastStartedTime?: string;
    lastFinishedTime?: string;
    lastSuccessTime?: string;
    nextSyncTime?: string;
    totalCount?: number;
    successCount?: number;
    failedCount?: number;
    lastErrorCode?: string;
    lastErrorMessage?: string;
    lastMode?: string;
    rateLimitMs?: number;
    updateTime?: string;
  }

  export interface SkuPageResult {
    code: number;
    msg: string;
    total: number;
    rows: SkuSyncItem[];
  }

  export interface SkuPairing {
    skuPairingId: number;
    connectionCode: string;
    masterSku: string;
    systemSku: string;
    systemSkuName: string;
    customerName?: string;
    createTime?: string;
    remark?: string;
  }

  export interface SkuPairingRequest {
    masterSku: string;
    systemSku: string;
    systemSkuName: string;
    customerName?: string;
    remark?: string;
  }

  export interface SourceWarehouseStockItem {
    inventorySnapshotId: number;
    connectionCode: string;
    systemKind?: string;
    systemKindLabel?: string;
    masterWarehouseName?: string;
    upstreamWarehouseCode: string;
    upstreamWarehouseName?: string;
    masterSku: string;
    masterProductName?: string;
    inventoryScope?: string;
    inventoryAttribute?: string;
    batchNo?: string;
    locationCode?: string;
    totalQuantity?: number;
    availableQuantity?: number;
    lockedQuantity?: number;
    inTransitQuantity?: number;
    boxedQuantity?: number;
    unboxedQuantity?: number;
    systemWarehouseCode?: string;
    systemWarehouseName?: string;
    systemSku?: string;
    systemSkuName?: string;
    customerName?: string;
    warehousePairingStatus?: string;
    skuPairingStatus?: string;
    status?: string;
    syncBatchId?: string;
    firstSeenTime?: string;
    lastSeenTime?: string;
    updateTime?: string;
  }

  export interface SourceWarehouseStockGroupItem {
    sourceStockGroupKey: string;
    repositoryScope?: string;
    inventoryScope?: string;
    masterSku: string;
    masterProductName?: string;
    inventoryAttributeCodes?: string;
    inventoryAttributeLabels?: string;
    inventoryAttributeCount?: number;
    sourceConnectionCodes?: string;
    masterWarehouseNames?: string;
    masterWarehouseCount?: number;
    upstreamWarehouseCodes?: string;
    upstreamWarehouseNames?: string;
    upstreamWarehouseCount?: number;
    detailRowCount?: number;
    activeDetailCount?: number;
    missingDetailCount?: number;
    totalQuantity?: number;
    availableQuantity?: number;
    lockedQuantity?: number;
    inTransitQuantity?: number;
    boxedQuantity?: number;
    unboxedQuantity?: number;
    systemWarehouseCodes?: string;
    systemWarehouseNames?: string;
    systemSkus?: string;
    systemSkuNames?: string;
    customerNames?: string;
    warehousePairingStatus?: string;
    skuPairingStatus?: string;
    status?: string;
    latestSyncBatchId?: string;
    firstSeenTime?: string;
    lastSeenTime?: string;
    latestUpdateTime?: string;
    rebuildTime?: string;
  }

  export interface SourceWarehouseStockOption {
    label: string;
    value: string;
    code?: string;
    name?: string;
    searchText?: string;
  }

  export interface SourceWarehouseStockPageResult {
    code: number;
    msg: string;
    total: number;
    rows: SourceWarehouseStockItem[];
  }

  export interface SourceWarehouseStockGroupPageResult {
    code: number;
    msg: string;
    total: number;
    rows: SourceWarehouseStockGroupItem[];
  }

  export interface RequestLog {
    requestLogId: number;
    connectionCode: string;
    traceId: string;
    operation: string;
    endpoint: string;
    requestTime?: string;
    responseTime?: string;
    durationMs?: number;
    requestPayloadRedacted?: string;
    responsePayloadRedacted?: string;
    externalErrorCode?: string;
    externalErrorMessage?: string;
    status?: string;
    createTime?: string;
  }

  export interface RequestLogPageResult {
    code: number;
    msg: string;
    total: number;
    rows: RequestLog[];
  }

  export interface SyncRequestRecord {
    requestId: number;
    requestNo: string;
    connectionCode: string;
    triggerSource?: string;
    mode?: string;
    requestedSyncTypes?: string;
    status?: string;
    submittedBy?: string;
    submittedTime?: string;
    startedTime?: string;
    finishedTime?: string;
    taskCount?: number;
    successCount?: number;
    failedCount?: number;
    timeoutCount?: number;
    skippedCount?: number;
    cancelledCount?: number;
    lastErrorMessage?: string;
    createTime?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface SyncTask {
    taskId: number;
    requestNo: string;
    syncBatchId: string;
    connectionCode: string;
    syncType?: string;
    mode?: string;
    triggerSource?: string;
    status?: string;
    priority?: number;
    leaseOwner?: string;
    leaseUntil?: string;
    attemptCount?: number;
    maxAttempts?: number;
    nextAttemptTime?: string;
    deadlineAt?: string;
    startedTime?: string;
    finishedTime?: string;
    traceId?: string;
    sysJobInvokeTarget?: string;
    pulledCount?: number;
    insertedCount?: number;
    changedCount?: number;
    unchangedCount?: number;
    disabledCount?: number;
    failedCount?: number;
    errorCode?: string;
    errorMessage?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface SyncTaskPageResult {
    code: number;
    msg: string;
    total: number;
    rows: SyncTask[];
  }

  export interface SyncRequestPageResult {
    code: number;
    msg: string;
    total: number;
    rows: SyncRequestRecord[];
  }
}
