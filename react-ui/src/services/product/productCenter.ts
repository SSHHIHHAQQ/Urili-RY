import { request } from '@umijs/max';

const baseUrl = '/api/product/admin/product-center';

export async function getProductCenterList(params?: Record<string, any>) {
  return request<API.ProductCenter.PageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function getProductCenterProduct(spuId: number) {
  return request<API.ProductCenter.InfoResult>(`${baseUrl}/${spuId}`, {
    method: 'GET',
  });
}

export async function getProductCenterSkus(spuId: number) {
  return request<API.ProductCenter.SkuListResult>(`${baseUrl}/${spuId}/skus`, {
    method: 'GET',
  });
}
