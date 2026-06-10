declare namespace API.Finance {
  export interface QuoteScheme {
    schemeId?: number;
    schemeCode?: string;
    schemeName: string;
    schemeType?: string;
    feeSourceMode?: string;
    currencyCode?: string;
    scopeType?: string;
    warehouseScopeMode?: string;
    effectiveTime?: string;
    expireTime?: string;
    effectivePriority?: number;
    status?: string;
    keyword?: string;
    scopeCount?: number;
    warehouseCount?: number;
    channelCount?: number;
    scopeSummary?: string;
    warehouseSummary?: string;
    channelSummary?: string;
    buyerLevelCodes?: string[];
    buyerIds?: number[];
    warehouseCodes?: string[];
    scopes?: QuoteSchemeScope[];
    warehouses?: QuoteSchemeWarehouse[];
    channels?: QuoteSchemeChannel[];
    valueFeeRules?: QuoteSchemeValueFeeRule[];
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface QuoteSchemeScope {
    scopeId?: number;
    schemeId?: number;
    scopeType?: string;
    scopeKey?: string;
    buyerLevelCode?: string;
    buyerLevelNameSnapshot?: string;
    buyerId?: number;
    buyerCodeSnapshot?: string;
    buyerNameSnapshot?: string;
    buyerShortNameSnapshot?: string;
  }

  export interface QuoteSchemeWarehouse {
    schemeWarehouseId?: number;
    schemeId?: number;
    warehouseCode?: string;
    warehouseNameSnapshot?: string;
    warehouseKindSnapshot?: string;
  }

  export interface QuoteSchemeChannel {
    schemeChannelId?: number;
    schemeId?: number;
    customerChannelCode?: string;
    customerChannelNameSnapshot?: string;
    operationFeeCode?: string;
    operationFeeNameSnapshot?: string;
    freightFeeCode?: string;
    freightFeeNameSnapshot?: string;
    status?: string;
    displayOrder?: number;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface QuoteSchemeValueFeeRule {
    valueFeeRuleId?: number;
    schemeId?: number;
    logisticsChannelCode?: string;
    logisticsChannelNameSnapshot?: string;
    triggerCode?: string;
    calculationMethod?: string;
    adjustmentDirection?: string;
    adjustmentValue?: number;
    status?: string;
    displayOrder?: number;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface QuoteSchemeOption {
    id?: number;
    label: string;
    value: string | number;
    code?: string;
    name?: string;
    shortName?: string;
    kind?: string;
    searchText?: string;
  }

  export interface QuoteSchemePageResult {
    code: number;
    msg: string;
    total: number;
    rows: QuoteScheme[];
  }

  export interface QuoteSchemeResult {
    code: number;
    msg: string;
    data: QuoteScheme;
  }

  export interface QuoteSchemeChannelListResult {
    code: number;
    msg: string;
    data: QuoteSchemeChannel[];
  }

  export interface QuoteSchemeWarehouseListResult {
    code: number;
    msg: string;
    data: QuoteSchemeWarehouse[];
  }

  export interface QuoteSchemeValueFeeListResult {
    code: number;
    msg: string;
    data: QuoteSchemeValueFeeRule[];
  }

  export interface QuoteSchemeOptionResult {
    code: number;
    msg: string;
    data: QuoteSchemeOption[];
  }
}
