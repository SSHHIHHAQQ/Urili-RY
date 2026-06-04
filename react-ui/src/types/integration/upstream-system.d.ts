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
    syncBatchId: string;
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
    status?: string;
    createTime?: string;
    remark?: string;
  }

  export interface WarehousePairingRequest {
    upstreamWarehouseCode: string;
    systemWarehouseCode: string;
    systemWarehouseName: string;
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
    upstreamChannelCode: string;
    upstreamChannelName: string;
    systemChannelCode: string;
    systemChannelName: string;
    status?: string;
    createTime?: string;
    remark?: string;
  }

  export interface LogisticsChannelPairingRequest {
    upstreamChannelCode: string;
    systemChannelCode: string;
    systemChannelName: string;
    remark?: string;
  }

  export interface SkuSyncItem {
    connectionCode: string;
    masterSku: string;
    masterProductName: string;
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
}
