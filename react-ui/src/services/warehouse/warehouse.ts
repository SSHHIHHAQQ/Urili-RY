import { request } from '@umijs/max';

const baseUrl = '/api/warehouse';

function withRuoYiPage(params?: Record<string, any>) {
  if (!params) {
    return params;
  }
  const { current, ...rest } = params;
  return {
    ...rest,
    pageNum: params.pageNum || current,
  };
}

export async function getOfficialWarehouseList(params?: API.Warehouse.WarehouseListParams) {
  return request<API.Warehouse.WarehousePageResult>(`${baseUrl}/official/list`, {
    method: 'GET',
    params: withRuoYiPage(params),
  });
}

export async function getOfficialWarehouse(warehouseId: number) {
  return request<API.Warehouse.WarehouseInfoResult>(`${baseUrl}/official/${warehouseId}`, {
    method: 'GET',
  });
}

export async function addOfficialWarehouse(data: API.Warehouse.Warehouse) {
  return request<API.Result>(`${baseUrl}/official`, {
    method: 'POST',
    data,
  });
}

export async function updateOfficialWarehouse(data: API.Warehouse.Warehouse) {
  return request<API.Result>(`${baseUrl}/official`, {
    method: 'PUT',
    data,
  });
}

export async function updateOfficialWarehouseStatus(data: API.Warehouse.WarehouseStatusRequest) {
  return request<API.Result>(`${baseUrl}/official/status`, {
    method: 'PUT',
    data,
  });
}

export async function getThirdPartyWarehouseList(params?: API.Warehouse.WarehouseListParams) {
  return request<API.Warehouse.WarehousePageResult>(`${baseUrl}/third-party/list`, {
    method: 'GET',
    params: withRuoYiPage(params),
  });
}

export async function getThirdPartyWarehouse(warehouseId: number) {
  return request<API.Warehouse.WarehouseInfoResult>(`${baseUrl}/third-party/${warehouseId}`, {
    method: 'GET',
  });
}

export async function addThirdPartyWarehouse(data: API.Warehouse.Warehouse) {
  return request<API.Result>(`${baseUrl}/third-party`, {
    method: 'POST',
    data,
  });
}

export async function updateThirdPartyWarehouse(data: API.Warehouse.Warehouse) {
  return request<API.Result>(`${baseUrl}/third-party`, {
    method: 'PUT',
    data,
  });
}

export async function updateThirdPartyWarehouseStatus(data: API.Warehouse.WarehouseStatusRequest) {
  return request<API.Result>(`${baseUrl}/third-party/status`, {
    method: 'PUT',
    data,
  });
}

export async function getWarehouseCurrencyOptions() {
  return request<API.Result & { data: API.Warehouse.CurrencyOption[] }>(`${baseUrl}/options/currencies`, {
    method: 'GET',
  });
}

export async function getWarehouseSellerOptions(keyword?: string) {
  return request<API.Result & { data: API.Warehouse.Option[] }>(`${baseUrl}/options/sellers`, {
    method: 'GET',
    params: { keyword },
  });
}

export async function getUsStateOptions(keyword?: string) {
  return request<API.Result & { data: API.Warehouse.UsState[] }>(`${baseUrl}/options/us-states`, {
    method: 'GET',
    params: { keyword },
  });
}

export async function getUsCityOptions(params?: { stateName?: string; keyword?: string }) {
  return request<API.Result & { data: API.Warehouse.UsCity[] }>(`${baseUrl}/options/us-cities`, {
    method: 'GET',
    params,
  });
}

export async function getOfficialSyncConnections(keyword?: string) {
  return request<API.Result & { data: API.Warehouse.SyncConnection[] }>(`${baseUrl}/official/sync-connections`, {
    method: 'GET',
    params: { keyword },
  });
}

export async function getOfficialSyncCandidates(params: { connectionCode: string; keyword?: string }) {
  return request<API.Result & { data: API.Warehouse.SyncCandidate[] }>(`${baseUrl}/official/sync-candidates`, {
    method: 'GET',
    params,
  });
}

export async function syncOfficialWarehouse(data: API.Warehouse.OfficialSyncRequest) {
  return request<API.Result>(`${baseUrl}/official/sync`, {
    method: 'POST',
    data,
  });
}
