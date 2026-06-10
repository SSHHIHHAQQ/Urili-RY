import { request } from '@umijs/max';

const baseUrl = '/api/logistics/admin/carriers';

export async function getCarrierList(params?: Record<string, any>) {
  return request<any>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function getCarrier(carrierAccountId: number) {
  return request<any>(`${baseUrl}/${carrierAccountId}`, {
    method: 'GET',
  });
}

export async function addCarrier(data: Record<string, any>) {
  return request<API.Result>(baseUrl, {
    method: 'POST',
    data,
  });
}

export async function updateCarrier(carrierAccountId: number, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${carrierAccountId}`, {
    method: 'PUT',
    data,
  });
}

export async function updateCarrierStatus(carrierAccountId: number, status: string) {
  return request<API.Result>(`${baseUrl}/${carrierAccountId}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function saveAgg56Credentials(carrierAccountId: number, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${carrierAccountId}/agg56-credentials`, {
    method: 'PUT',
    data,
  });
}

export async function authorizeCarrier(carrierAccountId: number) {
  return request<API.Result>(`${baseUrl}/${carrierAccountId}/authorize`, {
    method: 'POST',
  });
}

export async function syncCarrierChannels(carrierAccountId: number) {
  return request<API.Result>(`${baseUrl}/${carrierAccountId}/channels/sync`, {
    method: 'POST',
  });
}

export async function getCarrierChannels(carrierAccountId: number, params?: Record<string, any>) {
  return request<any>(`${baseUrl}/${carrierAccountId}/channels/list`, {
    method: 'GET',
    params,
  });
}

export async function getSystemChannelList(params?: Record<string, any>) {
  return request<any>(`${baseUrl}/system-channels/list`, {
    method: 'GET',
    params,
  });
}

export async function addSystemChannel(data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/system-channels`, {
    method: 'POST',
    data,
  });
}

export async function updateSystemChannel(
  systemChannelCode: string,
  status: string | undefined,
  data: Record<string, any>,
) {
  return request<API.Result>(`${baseUrl}/system-channels/${systemChannelCode}`, {
    method: 'PUT',
    params: { status },
    data,
  });
}

export async function getChannelMappings(carrierAccountId: number) {
  return request<any>(`${baseUrl}/${carrierAccountId}/channel-mappings/list`, {
    method: 'GET',
  });
}

export async function addChannelMapping(carrierAccountId: number, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${carrierAccountId}/channel-mappings`, {
    method: 'POST',
    data,
  });
}

export async function deleteChannelMapping(carrierAccountId: number, mappingId: number) {
  return request<API.Result>(`${baseUrl}/${carrierAccountId}/channel-mappings/${mappingId}`, {
    method: 'DELETE',
  });
}

export async function getRequestLogs(carrierAccountId: number, params?: Record<string, any>) {
  return request<any>(`${baseUrl}/${carrierAccountId}/request-logs/list`, {
    method: 'GET',
    params,
  });
}
