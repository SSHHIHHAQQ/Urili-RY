declare namespace API.Finance {
  export interface Currency {
    currencyId?: number;
    currencyCode: string;
    currencyName: string;
    currencySymbol?: string;
    baseCurrencyCode?: string;
    officialRate?: number;
    effectiveRate?: number;
    ratePrecision?: number;
    amountPrecision?: number;
    roundingMode?: string;
    adjustmentMode?: string;
    adjustmentValue?: number;
    officialRateTime?: string;
    effectiveRateTime?: string;
    isDefault?: string;
    status?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface CurrencyPageResult {
    code: number;
    msg: string;
    total: number;
    rows: Currency[];
  }

  export interface CurrencyOption {
    label: string;
    value: string;
    symbol?: string;
    amountPrecision?: number;
  }

  export interface CurrencyOptionResult {
    code: number;
    msg: string;
    data: CurrencyOption[];
  }

  export interface RateHistory {
    rateHistoryId: number;
    currencyCode: string;
    baseCurrencyCode: string;
    officialRate?: number;
    effectiveRate?: number;
    adjustmentMode?: string;
    adjustmentValue?: number;
    sourceType?: string;
    sourceConfigId?: number;
    officialRateTime?: string;
    effectiveRateTime?: string;
    changeReason?: string;
    createBy?: string;
    createTime?: string;
  }

  export interface RateHistoryPageResult {
    code: number;
    msg: string;
    total: number;
    rows: RateHistory[];
  }

  export interface SyncConfig {
    syncConfigId?: number;
    providerCode?: string;
    providerName?: string;
    showApiApplicationName?: string;
    showApiApplicationId?: string;
    baseCurrencyCode?: string;
    apiBaseUrl?: string;
    authType?: string;
    credential?: string;
    credentialMasked?: string;
    requestTimeoutMs?: number;
    retryCount?: number;
    scheduleType?: string;
    cronExpression?: string;
    rateAnchorTime?: string;
    syncEnabled?: string;
    lastSyncTime?: string;
    lastSyncStatus?: string;
    status?: string;
    remark?: string;
  }

  export interface SyncConfigResult {
    code: number;
    msg: string;
    data: SyncConfig;
  }

  export interface SyncResult {
    traceId: string;
    currencyCount: number;
    updatedCount: number;
    status: string;
  }

  export interface SyncResultResponse {
    code: number;
    msg: string;
    data: SyncResult;
  }

  export interface SyncLog {
    syncLogId: number;
    traceId: string;
    syncConfigId: number;
    providerCode: string;
    requestUrl?: string;
    requestTime?: string;
    responseTime?: string;
    costMs?: number;
    status?: string;
    errorCode?: string;
    errorMessage?: string;
    currencyCount?: number;
    updatedCount?: number;
    responseSummary?: string;
    createTime?: string;
  }

  export interface SyncLogPageResult {
    code: number;
    msg: string;
    total: number;
    rows: SyncLog[];
  }
}
