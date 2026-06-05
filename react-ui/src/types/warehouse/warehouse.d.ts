declare namespace API.Warehouse {
  export interface Warehouse {
    warehouseId?: number;
    warehouseCode?: string;
    warehouseName?: string;
    warehouseKind?: 'official' | 'third_party';
    countryCode?: string;
    stateProvince?: string;
    city?: string;
    postalCode?: string;
    addressLine1?: string;
    addressLine2?: string;
    contactName?: string;
    contactPhone?: string;
    contactEmail?: string;
    companyName?: string;
    settlementCurrency?: string;
    status?: string;
    sellerId?: number;
    sellerNo?: string;
    sellerCode?: string;
    sellerName?: string;
    sellerShortName?: string;
    sellerKeyword?: string;
    warehousePairingId?: number;
    connectionCode?: string;
    upstreamWarehouseCode?: string;
    upstreamWarehouseName?: string;
    pairingStatus?: string;
    createBy?: string;
    createTime?: string;
    updateBy?: string;
    updateTime?: string;
    remark?: string;
  }

  export interface WarehouseListParams extends Warehouse {
    pageNum?: number;
    pageSize?: number;
    current?: number;
  }

  export interface WarehousePageResult {
    code: number;
    msg: string;
    total: number;
    rows: Warehouse[];
  }

  export interface WarehouseInfoResult {
    code: number;
    msg: string;
    data: Warehouse;
  }

  export interface WarehouseStatusRequest {
    warehouseId: number;
    status: string;
  }

  export interface Option {
    label: string;
    value: string | number;
    code?: string;
    name?: string;
    searchText?: string;
    disabled?: boolean;
  }

  export interface CurrencyOption {
    label: string;
    value: string;
    symbol?: string;
    amountPrecision?: number;
  }

  export interface UsState {
    stateId?: number;
    stateCode: string;
    stateName: string;
    status?: string;
  }

  export interface UsCity {
    cityId?: number;
    placeGeoid?: string;
    stateCode: string;
    stateName: string;
    cityName: string;
    placeName?: string;
    placeType?: string;
    status?: string;
  }

  export interface SyncConnection {
    connectionCode: string;
    masterWarehouseName: string;
    systemKind?: string;
  }

  export interface SyncCandidate {
    connectionCode: string;
    masterWarehouseName?: string;
    warehouseCode: string;
    warehouseName: string;
    countryCode?: string;
    status?: string;
    paired?: boolean;
    systemWarehouseCode?: string;
    systemWarehouseName?: string;
  }

  export interface OfficialSyncRequest extends Warehouse {
    connectionCode: string;
    upstreamWarehouseCode: string;
  }
}
