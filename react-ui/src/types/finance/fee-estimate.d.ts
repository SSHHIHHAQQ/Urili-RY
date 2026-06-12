declare namespace API.Finance {
  export type FeeEstimateInputMode = 'SKU' | 'MANUAL';
  export type FeeEstimateSelectionMode = 'MANUAL' | 'AUTO_BEST';
  export type FeeEstimateView = 'OPERATIONS' | 'BUYER_SIMULATION';

  export interface FeeEstimateQuoteSchemeOption {
    schemeId?: number;
    value: number;
    label: string;
    schemeCode?: string;
    schemeName?: string;
    schemeType?: string;
    feeSourceMode?: string;
    currencyCode?: string;
    channels?: QuoteSchemeOption[];
  }

  export interface FeeEstimateOptions {
    quoteSchemes?: FeeEstimateQuoteSchemeOption[];
    buyers?: QuoteSchemeOption[];
    warehouses?: QuoteSchemeOption[];
    channels?: QuoteSchemeOption[];
    customerChannels?: QuoteSchemeOption[];
    selectedScheme?: FeeEstimateQuoteSchemeOption;
  }

  export interface FeeEstimateSkuSnapshot {
    skuId: number;
    systemSkuCode?: string;
    sellerSkuCode?: string;
    productName?: string;
    productNameEn?: string;
    masterSku?: string;
    measureLengthCm?: number;
    measureWidthCm?: number;
    measureHeightCm?: number;
    measureWeightKg?: number;
    measureSource?: string;
    sourceWarehouseNames?: string;
    sourceWarehouseCodes?: string[];
    sourceWarehouseCount?: number;
    availableStock?: number;
    quantity?: number;
    label?: string;
    searchText?: string;
  }

  export interface FeeEstimatePackageLine {
    skuId?: number;
    quantity?: number;
    lengthCm?: number;
    widthCm?: number;
    heightCm?: number;
    weightKg?: number;
  }

  export interface FeeEstimateRequest {
    estimateView?: FeeEstimateView;
    selectionMode?: FeeEstimateSelectionMode;
    buyerId?: number;
    packageInputMode: FeeEstimateInputMode;
    originWarehouseCode?: string;
    warehouseCodes?: string[];
    customerChannelCode?: string;
    destinationCountryCode: string;
    destinationState?: string;
    destinationCity?: string;
    destinationPostalCode: string;
    destinationAddress1?: string;
    destinationAddress2?: string;
    quoteSchemeId?: number;
    channelCodes?: string[];
    packageLines: FeeEstimatePackageLine[];
  }

  export interface FeeEstimatePackageSummary {
    edge1Cm?: number;
    edge2Cm?: number;
    edge3Cm?: number;
    actualWeightKg?: number;
    volumeWeightKg?: number;
    chargeableWeightKg?: number;
    packageCount?: number;
    sizeExpression?: string;
    mergeRule?: string;
  }

  export interface FeeEstimateChannelResult {
    schemeChannelId?: number;
    selectionMode?: FeeEstimateSelectionMode;
    warehouseCode?: string;
    warehouseName?: string;
    systemChannelCode?: string;
    systemChannelName?: string;
    fulfillmentMode?: string;
    channelCode?: string;
    channelName?: string;
    schemeType?: string;
    feeSourceMode?: string;
    currencyCode?: string;
    success?: boolean;
    recommended?: boolean;
    totalAmount?: number;
    basicFreightAmount?: number;
    surchargeAmount?: number;
    operationFeeAmount?: number;
    packageMaterialFeeAmount?: number;
    actualWeightKg?: number;
    volumeWeightKg?: number;
    chargeableWeightKg?: number;
    packageCount?: number;
    errorCode?: string;
    errorMessage?: string;
    traceId?: string;
  }

  export interface FeeEstimateRouteCandidate {
    schemeId?: number;
    schemeChannelId?: number;
    schemeName?: string;
    feeSourceMode?: string;
    currencyCode?: string;
    warehouseCode?: string;
    warehouseName?: string;
    warehouseKind?: string;
    countryCode?: string;
    customerChannelCode?: string;
    customerChannelName?: string;
    labelUploadRequired?: string;
    buyerScopeMode?: string;
    systemChannelCode?: string;
    systemChannelName?: string;
    fulfillmentMode?: string;
    carrierConnectionCode?: string;
    carrierExternalChannelCode?: string;
    executable?: boolean;
    failureCode?: string;
    failureMessage?: string;
  }

  export interface FeeEstimateResponse {
    requestNo?: string;
    estimateView?: FeeEstimateView;
    selectionMode?: FeeEstimateSelectionMode;
    buyerId?: number;
    buyerCode?: string;
    buyerName?: string;
    buyerLevel?: string;
    originWarehouseCode?: string;
    quoteSchemeId?: number;
    quoteSchemeName?: string;
    packageInputMode?: FeeEstimateInputMode;
    packageSummary?: FeeEstimatePackageSummary;
    skuSnapshots?: FeeEstimateSkuSnapshot[];
    resolveSummary?: {
      warehouseCandidateCount?: number;
      quoteSchemeCandidateCount?: number;
      customerChannelCandidateCount?: number;
      routeCandidateCount?: number;
      executableRouteCount?: number;
      failedCandidateCount?: number;
      resolveCostMs?: number;
    };
    routeCandidates?: FeeEstimateRouteCandidate[];
    results?: FeeEstimateChannelResult[];
  }

  export interface FeeEstimateOptionsResult {
    code: number;
    msg: string;
    data: FeeEstimateOptions;
  }

  export interface FeeEstimateSkuPageResult {
    code: number;
    msg: string;
    total: number;
    rows: FeeEstimateSkuSnapshot[];
  }

  export interface FeeEstimateCalculateResult {
    code: number;
    msg: string;
    data: FeeEstimateResponse;
  }
}
