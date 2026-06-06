import { request } from '@umijs/max';

const baseUrl = '/api/integration/admin/source-warehouse-stocks';

export async function getSourceWarehouseStockList(params?: Record<string, any>) {
  return request<API.Integration.SourceWarehouseStockPageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}
