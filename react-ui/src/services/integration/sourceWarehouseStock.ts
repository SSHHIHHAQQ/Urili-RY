import { request } from '@umijs/max';

const baseUrl = '/api/integration/admin/source-warehouse-stocks';

export async function getSourceWarehouseStockList(params?: Record<string, any>) {
  return request<API.Integration.SourceWarehouseStockPageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function getSourceWarehouseStockGroupList(params?: Record<string, any>) {
  return request<API.Integration.SourceWarehouseStockGroupPageResult>(`${baseUrl}/groups/list`, {
    method: 'GET',
    params,
  });
}

export async function getSourceWarehouseStockGroupDetail(params?: Record<string, any>) {
  return request<API.Result & { data: API.Integration.SourceWarehouseStockItem[] }>(`${baseUrl}/groups/detail`, {
    method: 'GET',
    params,
  });
}

export async function getSourceWarehouseStockMasterWarehouseOptions(params?: Record<string, any>) {
  return request<API.Result & { data: API.Integration.SourceWarehouseStockOption[] }>(
    `${baseUrl}/options/master-warehouses`,
    {
      method: 'GET',
      params,
    },
  );
}

export async function getSourceWarehouseStockSourceWarehouseOptions(params?: Record<string, any>) {
  return request<API.Result & { data: API.Integration.SourceWarehouseStockOption[] }>(
    `${baseUrl}/options/source-warehouses`,
    {
      method: 'GET',
      params,
    },
  );
}
