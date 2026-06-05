declare namespace API.Integration {
  export interface SourceProductItem {
    connectionCode: string;
    systemKind?: string;
    systemKindLabel?: string;
    masterWarehouseName?: string;
    masterSku: string;
    masterProductName: string;
    productAliasName?: string;
    approveStatus?: string;
    productType?: number;
    productDescription?: string;
    imageUrl?: string;
    mainCode?: string;
    otherCode?: string;
    fnsku?: string;
    countryOfOriginName?: string;
    currencyCode?: string;
    customhouseCode?: string;
    dangerousCargo?: number;
    declareNameCn?: string;
    declareNameEn?: string;
    declarePrice?: number;
    height?: number;
    heightBs?: number;
    length?: number;
    lengthBs?: number;
    weight?: number;
    weightBs?: number;
    width?: number;
    widthBs?: number;
    wmsHeight?: number;
    wmsHeightBs?: number;
    wmsLength?: number;
    wmsLengthBs?: number;
    wmsWeight?: number;
    wmsWeightBs?: number;
    wmsWidth?: number;
    wmsWidthBs?: number;
    cat1Name?: string;
    cat2Name?: string;
    cat3Name?: string;
    platformSkuInfoJson?: string;
    brazilTaxInfoJson?: string;
    sourcePayloadHash?: string;
    status?: string;
    searchText?: string;
    pairingStatus?: string;
    skuPairingId?: number;
    systemSku?: string;
    systemSkuName?: string;
    customerName?: string;
    syncBatchId?: string;
    firstSeenTime?: string;
    lastSeenTime?: string;
    updateTime?: string;
  }

  export interface SourceProductPageResult {
    code: number;
    msg: string;
    total: number;
    rows: SourceProductItem[];
  }
}
