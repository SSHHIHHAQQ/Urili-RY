export type WarehouseRow = API.Integration.WarehouseSyncItem & Partial<API.Integration.WarehousePairing>;

export type LogisticsRow = API.Integration.LogisticsChannelSyncItem & {
  warehouseCodes: string;
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
