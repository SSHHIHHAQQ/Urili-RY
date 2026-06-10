import { request } from '@umijs/max';

const baseUrl = '/api/logistics/admin/system-channels';

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

export async function getSystemChannelList(params?: Record<string, any>) {
  return request<any>(`${baseUrl}/list`, {
    method: 'GET',
    params: withRuoYiPage(params),
  });
}

export async function getSystemChannel(systemChannelCode: string) {
  return request<any>(`${baseUrl}/${systemChannelCode}`, {
    method: 'GET',
  });
}

export async function addSystemChannel(data: Record<string, any>) {
  return request<API.Result>(baseUrl, {
    method: 'POST',
    data,
  });
}

export async function updateSystemChannel(systemChannelCode: string, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${systemChannelCode}`, {
    method: 'PUT',
    data,
  });
}

export async function updateSystemChannelStatus(systemChannelCode: string, status: string) {
  return request<API.Result>(`${baseUrl}/${systemChannelCode}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function getCarrierMappings(systemChannelCode: string) {
  return request<any>(`${baseUrl}/${systemChannelCode}/carrier-mappings/list`, {
    method: 'GET',
  });
}

export async function addCarrierMapping(systemChannelCode: string, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${systemChannelCode}/carrier-mappings`, {
    method: 'POST',
    data,
  });
}

export async function deleteCarrierMapping(systemChannelCode: string, mappingId: number) {
  return request<API.Result>(`${baseUrl}/${systemChannelCode}/carrier-mappings/${mappingId}`, {
    method: 'DELETE',
  });
}

export async function getWarehouseBindings(systemChannelCode: string) {
  return request<any>(`${baseUrl}/${systemChannelCode}/warehouses/list`, {
    method: 'GET',
  });
}

export async function addWarehouseBinding(systemChannelCode: string, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${systemChannelCode}/warehouses`, {
    method: 'POST',
    data,
  });
}

export async function updateWarehouseBinding(systemChannelCode: string, bindingId: number, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${systemChannelCode}/warehouses/${bindingId}`, {
    method: 'PUT',
    data,
  });
}

export async function deleteWarehouseBinding(systemChannelCode: string, bindingId: number) {
  return request<API.Result>(`${baseUrl}/${systemChannelCode}/warehouses/${bindingId}`, {
    method: 'DELETE',
  });
}

export async function getOrderSetting(systemChannelCode: string) {
  return request<any>(`${baseUrl}/${systemChannelCode}/order-setting`, {
    method: 'GET',
  });
}

export async function saveOrderSetting(systemChannelCode: string, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${systemChannelCode}/order-setting`, {
    method: 'PUT',
    data,
  });
}

export async function getCarrierAccountOptions(keyword?: string) {
  return request<any>(`${baseUrl}/options/carrier-accounts`, {
    method: 'GET',
    params: { keyword },
  });
}

export async function getCarrierChannelOptions(carrierAccountId: number, keyword?: string) {
  return request<any>(`${baseUrl}/options/carrier-channels`, {
    method: 'GET',
    params: { carrierAccountId, keyword },
  });
}

export async function getWarehouseOptions(keyword?: string) {
  return request<any>(`${baseUrl}/options/warehouses`, {
    method: 'GET',
    params: { keyword },
  });
}
