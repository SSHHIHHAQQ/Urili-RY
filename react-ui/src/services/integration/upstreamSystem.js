import { request } from '@umijs/max';

const baseUrl = '/api/integration/admin/upstream-systems';

export async function getUpstreamConnectionList(params) {
  return request(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function addUpstreamConnection(data) {
  return request(baseUrl, {
    method: 'POST',
    data,
  });
}

export async function updateUpstreamConnection(connectionCode, data) {
  return request(`${baseUrl}/${connectionCode}`, {
    method: 'PUT',
    data,
  });
}

export async function updateUpstreamCredentials(connectionCode, data) {
  return request(`${baseUrl}/${connectionCode}/credentials`, {
    method: 'PUT',
    data,
  });
}

export async function updateUpstreamStatus(connectionCode, status) {
  return request(`${baseUrl}/${connectionCode}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function updateUpstreamConnectionOrder(connectionCodes) {
  return request(`${baseUrl}/order`, {
    method: 'PUT',
    data: { connectionCodes },
  });
}

export async function authorizeUpstreamConnection(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/authorize`, {
    method: 'POST',
  });
}

export async function syncUpstreamConnection(connectionCode, data) {
  return request(`${baseUrl}/${connectionCode}/sync`, {
    method: 'POST',
    data,
  });
}

export async function syncUpstreamSku(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/skus/sync`, {
    method: 'POST',
  });
}

export async function syncUpstreamSkuDimensions(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/sku-dimensions/sync`, {
    method: 'POST',
  });
}

export async function syncUpstreamSkuDimensionsSelected(connectionCode, skuList) {
  return request(`${baseUrl}/${connectionCode}/sku-dimensions/sync-selected`, {
    method: 'POST',
    data: { skuList },
  });
}

export async function syncUpstreamInventory(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/inventory/sync`, {
    method: 'POST',
  });
}

export async function getUpstreamInventoryList(connectionCode, params) {
  return request(`${baseUrl}/${connectionCode}/inventory/list`, {
    method: 'GET',
    params,
  });
}

export async function getInventorySyncState(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/inventory-sync-state`, {
    method: 'GET',
  });
}

export async function getUpstreamSyncStates(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/sync-states`, {
    method: 'GET',
  });
}

export async function getWarehouseSyncList(connectionCode, params) {
  return request(`${baseUrl}/${connectionCode}/warehouses`, {
    method: 'GET',
    params,
  });
}

export async function getWarehousePairings(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/warehouse-pairings`, {
    method: 'GET',
  });
}

export async function addWarehousePairing(connectionCode, data) {
  return request(`${baseUrl}/${connectionCode}/warehouse-pairings`, {
    method: 'POST',
    data,
  });
}

export async function deleteWarehousePairing(connectionCode, warehousePairingId) {
  return request(`${baseUrl}/${connectionCode}/warehouse-pairings/${warehousePairingId}`, {
    method: 'DELETE',
  });
}

export async function getLogisticsChannelSyncList(connectionCode, params) {
  return request(`${baseUrl}/${connectionCode}/logistics-channels`, {
    method: 'GET',
    params,
  });
}

export async function getLogisticsChannelPairings(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/logistics-channel-pairings`, {
    method: 'GET',
  });
}

export async function addLogisticsChannelPairing(connectionCode, data) {
  return request(`${baseUrl}/${connectionCode}/logistics-channel-pairings`, {
    method: 'POST',
    data,
  });
}

export async function deleteLogisticsChannelPairing(connectionCode, logisticsChannelPairingId) {
  return request(`${baseUrl}/${connectionCode}/logistics-channel-pairings/${logisticsChannelPairingId}`, {
    method: 'DELETE',
  });
}

export async function getSkuSyncList(connectionCode, params) {
  return request(`${baseUrl}/${connectionCode}/skus/list`, {
    method: 'GET',
    params,
  });
}

export async function getSkuSyncState(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/sku-sync-state`, {
    method: 'GET',
  });
}

export async function getSkuPairings(connectionCode) {
  return request(`${baseUrl}/${connectionCode}/sku-pairings`, {
    method: 'GET',
  });
}

export async function addSkuPairing(connectionCode, data) {
  return request(`${baseUrl}/${connectionCode}/sku-pairings`, {
    method: 'POST',
    data,
  });
}

export async function deleteSkuPairing(connectionCode, skuPairingId) {
  return request(`${baseUrl}/${connectionCode}/sku-pairings/${skuPairingId}`, {
    method: 'DELETE',
  });
}

export async function getRequestLogList(connectionCode, params) {
  return request(`${baseUrl}/${connectionCode}/request-logs/list`, {
    method: 'GET',
    params,
  });
}
