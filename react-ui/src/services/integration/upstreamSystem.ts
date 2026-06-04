import { request } from '@umijs/max';

const baseUrl = '/api/integration/admin/upstream-systems';

export async function getUpstreamConnectionList(params?: Record<string, any>) {
  return request<API.Integration.ConnectionPageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function addUpstreamConnection(data: API.Integration.ConnectionSaveRequest) {
  return request<API.Result>(baseUrl, {
    method: 'POST',
    data,
  });
}

export async function updateUpstreamConnection(connectionCode: string, data: API.Integration.ConnectionInfoRequest) {
  return request<API.Result>(`${baseUrl}/${connectionCode}`, {
    method: 'PUT',
    data,
  });
}

export async function updateUpstreamCredentials(connectionCode: string, data: API.Integration.CredentialRequest) {
  return request<API.Result>(`${baseUrl}/${connectionCode}/credentials`, {
    method: 'PUT',
    data,
  });
}

export async function updateUpstreamStatus(connectionCode: string, status: string) {
  return request<API.Result>(`${baseUrl}/${connectionCode}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function authorizeUpstreamConnection(connectionCode: string) {
  return request<API.Result>(`${baseUrl}/${connectionCode}/authorize`, {
    method: 'POST',
  });
}

export async function syncUpstreamConnection(connectionCode: string) {
  return request<API.Result & { data: API.Integration.SyncResult }>(`${baseUrl}/${connectionCode}/sync`, {
    method: 'POST',
  });
}

export async function getWarehouseSyncList(connectionCode: string, params?: Record<string, any>) {
  return request<API.Result & { data: API.Integration.WarehouseSyncItem[] }>(`${baseUrl}/${connectionCode}/warehouses`, {
    method: 'GET',
    params,
  });
}

export async function getWarehousePairings(connectionCode: string) {
  return request<API.Result & { data: API.Integration.WarehousePairing[] }>(`${baseUrl}/${connectionCode}/warehouse-pairings`, {
    method: 'GET',
  });
}

export async function addWarehousePairing(connectionCode: string, data: API.Integration.WarehousePairingRequest) {
  return request<API.Result>(`${baseUrl}/${connectionCode}/warehouse-pairings`, {
    method: 'POST',
    data,
  });
}

export async function deleteWarehousePairing(warehousePairingId: number) {
  return request<API.Result>(`${baseUrl}/warehouse-pairings/${warehousePairingId}`, {
    method: 'DELETE',
  });
}

export async function getLogisticsChannelSyncList(connectionCode: string, params?: Record<string, any>) {
  return request<API.Result & { data: API.Integration.LogisticsChannelSyncItem[] }>(
    `${baseUrl}/${connectionCode}/logistics-channels`,
    {
      method: 'GET',
      params,
    },
  );
}

export async function getLogisticsChannelPairings(connectionCode: string) {
  return request<API.Result & { data: API.Integration.LogisticsChannelPairing[] }>(
    `${baseUrl}/${connectionCode}/logistics-channel-pairings`,
    {
      method: 'GET',
    },
  );
}

export async function addLogisticsChannelPairing(connectionCode: string, data: API.Integration.LogisticsChannelPairingRequest) {
  return request<API.Result>(`${baseUrl}/${connectionCode}/logistics-channel-pairings`, {
    method: 'POST',
    data,
  });
}

export async function deleteLogisticsChannelPairing(logisticsChannelPairingId: number) {
  return request<API.Result>(`${baseUrl}/logistics-channel-pairings/${logisticsChannelPairingId}`, {
    method: 'DELETE',
  });
}

export async function getSkuSyncList(connectionCode: string, params?: Record<string, any>) {
  return request<API.Integration.SkuPageResult>(`${baseUrl}/${connectionCode}/skus/list`, {
    method: 'GET',
    params,
  });
}

export async function getSkuPairings(connectionCode: string) {
  return request<API.Result & { data: API.Integration.SkuPairing[] }>(`${baseUrl}/${connectionCode}/sku-pairings`, {
    method: 'GET',
  });
}

export async function addSkuPairing(connectionCode: string, data: API.Integration.SkuPairingRequest) {
  return request<API.Result>(`${baseUrl}/${connectionCode}/sku-pairings`, {
    method: 'POST',
    data,
  });
}

export async function deleteSkuPairing(skuPairingId: number) {
  return request<API.Result>(`${baseUrl}/sku-pairings/${skuPairingId}`, {
    method: 'DELETE',
  });
}

export async function getRequestLogList(connectionCode: string, params?: Record<string, any>) {
  return request<API.Integration.RequestLogPageResult>(`${baseUrl}/${connectionCode}/request-logs/list`, {
    method: 'GET',
    params,
  });
}
