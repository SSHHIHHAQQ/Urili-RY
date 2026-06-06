import { request } from '@umijs/max';

const baseUrl = '/api/integration/admin/source-products';

export async function getSourceProductList(params?: Record<string, any>) {
  return request<API.Integration.SourceProductPageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function getSourceProductGroupDetail(params?: Record<string, any>) {
  return request<API.Result & { data: API.Integration.SourceProductGroupDetail }>(`${baseUrl}/group-detail`, {
    method: 'GET',
    params,
  });
}
