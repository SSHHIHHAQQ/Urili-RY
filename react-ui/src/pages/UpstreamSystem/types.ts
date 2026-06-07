export type WarehouseRow = API.Integration.WarehouseSyncItem &
  Partial<API.Integration.WarehousePairing>;

export type LogisticsWarehouseContext = API.Integration.LogisticsChannelSyncItem & {
  systemWarehouseCode?: string;
  systemWarehouseName?: string;
  warehousePairingId?: number;
  pairingRole?: string;
};

export type LogisticsRow = API.Integration.LogisticsChannelSyncItem & {
  warehouseCodes: string;
  warehouseItems: LogisticsWarehouseContext[];
  pairings: API.Integration.LogisticsChannelPairing[];
};

export type PairingModalState =
  | { open: false }
  | {
      open: true;
      type: 'warehouse';
      row: WarehouseRow;
    }
  | {
      open: true;
      type: 'logistics';
      row: LogisticsRow;
    }
  | {
      open: true;
      type: 'sku';
      row: API.Integration.SkuSyncItem;
    };
