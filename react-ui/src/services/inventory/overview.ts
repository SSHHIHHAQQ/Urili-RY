import { request } from '@umijs/max';

const baseUrl = '/api/inventory/admin/overview';

export async function getInventoryOverviewSpuList(params?: Record<string, any>) {
  return request<API.InventoryOverview.PageResult>(`${baseUrl}/spu/list`, {
    method: 'GET',
    params,
  });
}

export async function getInventoryOverviewSkuList(params?: Record<string, any>) {
  return request<API.InventoryOverview.PageResult>(`${baseUrl}/sku/list`, {
    method: 'GET',
    params,
  });
}

export async function getInventoryOverviewWarehouseList(params?: Record<string, any>) {
  return request<API.InventoryOverview.WarehousePageResult>(`${baseUrl}/warehouse/list`, {
    method: 'GET',
    params,
  });
}

export async function getInventoryOverviewWarehouseOptions() {
  return request<API.InventoryOverview.WarehouseOptionResult>(`${baseUrl}/warehouse/options`, {
    method: 'GET',
  });
}

export async function getInventoryOverviewOfficialWarehouseOptions() {
  return request<API.InventoryOverview.WarehouseOptionResult>(`${baseUrl}/official-warehouse/options`, {
    method: 'GET',
  });
}

export async function getInventoryOverviewSellerOptions() {
  return request<API.InventoryOverview.SellerOptionResult>(`${baseUrl}/seller/options`, {
    method: 'GET',
  });
}

export async function getInventoryOverviewWarehouses(skuId: number) {
  return request<API.InventoryOverview.WarehouseResult>(`${baseUrl}/sku/${skuId}/warehouses`, {
    method: 'GET',
  });
}

export async function getInventoryOverviewSpuSkuWarehouses(spuId: number) {
  return request<API.InventoryOverview.SkuWarehouseGroupResult>(`${baseUrl}/spu/${spuId}/sku-warehouses`, {
    method: 'GET',
  });
}

export async function previewInventoryOverviewAdjust(data: {
  stockId?: number;
  adjustField: 'PLATFORM_TOTAL' | 'PLATFORM_IN_TRANSIT';
  targetQty: number;
  reason?: string;
}) {
  return request<API.InventoryOverview.AdjustPreviewResult>(`${baseUrl}/adjust/preview`, {
    method: 'POST',
    data,
  });
}

export async function confirmInventoryOverviewAdjust(data: {
  stockId?: number;
  adjustField: 'PLATFORM_TOTAL' | 'PLATFORM_IN_TRANSIT';
  targetQty: number;
  confirmed?: boolean;
  reason?: string;
}) {
  return request<API.InventoryOverview.AdjustPreviewResult>(`${baseUrl}/adjust/confirm`, {
    method: 'POST',
    data,
  });
}

export async function previewInventoryOverviewBatchAdjust(data: {
  items: API.InventoryOverview.BatchAdjustItem[];
  reason?: string;
}) {
  return request<API.InventoryOverview.BatchAdjustPreviewResult>(`${baseUrl}/adjust/batch-preview`, {
    method: 'POST',
    data,
  });
}

export async function confirmInventoryOverviewBatchAdjust(data: {
  items: API.InventoryOverview.BatchAdjustItem[];
  confirmed?: boolean;
  reason?: string;
}) {
  return request<API.InventoryOverview.BatchAdjustPreviewResult>(`${baseUrl}/adjust/batch-confirm`, {
    method: 'POST',
    data,
  });
}

export async function previewInventoryOverviewSyncPolicy(data: API.InventoryOverview.SyncPolicyRequest) {
  return request<API.InventoryOverview.SyncPolicyPreviewResult>(`${baseUrl}/sync-policy/preview`, {
    method: 'POST',
    data,
  });
}

export async function confirmInventoryOverviewSyncPolicy(data: API.InventoryOverview.SyncPolicyRequest) {
  return request<API.InventoryOverview.SyncPolicyPreviewResult>(`${baseUrl}/sync-policy/confirm`, {
    method: 'POST',
    data,
  });
}

export async function getInventoryOverviewLedgerList(params?: Record<string, any>) {
  return request<API.InventoryOverview.LedgerPageResult>(`${baseUrl}/ledger/list`, {
    method: 'GET',
    params,
  });
}
