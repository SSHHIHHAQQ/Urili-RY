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

export async function getInventoryOverviewWarehouses(skuId: number) {
  return request<API.InventoryOverview.WarehouseResult>(`${baseUrl}/sku/${skuId}/warehouses`, {
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

export async function getInventoryOverviewLedgerList(params?: Record<string, any>) {
  return request<API.InventoryOverview.LedgerPageResult>(`${baseUrl}/ledger/list`, {
    method: 'GET',
    params,
  });
}
