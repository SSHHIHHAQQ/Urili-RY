import { request } from '@umijs/max';

const baseUrl = '/api/integration/admin/source-products';

export async function getSourceProductList(params?: Record<string, any>) {
  return request<API.Integration.SourceProductPageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}
