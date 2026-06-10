import { request } from '@umijs/max';

const baseUrl = '/api/logistics/admin/customer-channels';

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

export async function getCustomerChannelList(params?: Record<string, any>) {
  return request<any>(`${baseUrl}/list`, {
    method: 'GET',
    params: withRuoYiPage(params),
  });
}

export async function getCustomerChannel(customerChannelCode: string) {
  return request<any>(`${baseUrl}/${customerChannelCode}`, {
    method: 'GET',
  });
}

export async function addCustomerChannel(data: Record<string, any>) {
  return request<API.Result>(baseUrl, {
    method: 'POST',
    data,
  });
}

export async function updateCustomerChannel(customerChannelCode: string, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${customerChannelCode}`, {
    method: 'PUT',
    data,
  });
}

export async function updateCustomerChannelStatus(customerChannelCode: string, status: string) {
  return request<API.Result>(`${baseUrl}/${customerChannelCode}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function getSystemMappings(customerChannelCode: string) {
  return request<any>(`${baseUrl}/${customerChannelCode}/system-mappings/list`, {
    method: 'GET',
  });
}

export async function addSystemMapping(customerChannelCode: string, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${customerChannelCode}/system-mappings`, {
    method: 'POST',
    data,
  });
}

export async function updateSystemMapping(customerChannelCode: string, mappingId: number, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${customerChannelCode}/system-mappings/${mappingId}`, {
    method: 'PUT',
    data,
  });
}

export async function deleteSystemMapping(customerChannelCode: string, mappingId: number) {
  return request<API.Result>(`${baseUrl}/${customerChannelCode}/system-mappings/${mappingId}`, {
    method: 'DELETE',
  });
}

export async function getBuyerScope(customerChannelCode: string) {
  return request<any>(`${baseUrl}/${customerChannelCode}/buyer-scope`, {
    method: 'GET',
  });
}

export async function saveBuyerScope(customerChannelCode: string, data: Record<string, any>) {
  return request<API.Result>(`${baseUrl}/${customerChannelCode}/buyer-scope`, {
    method: 'PUT',
    data,
  });
}

export async function getSystemChannelOptions(keyword?: string) {
  return request<any>(`${baseUrl}/options/system-channels`, {
    method: 'GET',
    params: { keyword },
  });
}

export async function getBuyerOptions(keyword?: string) {
  return request<any>(`${baseUrl}/options/buyers`, {
    method: 'GET',
    params: { keyword },
  });
}
