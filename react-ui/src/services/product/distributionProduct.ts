import { request } from '@umijs/max';

const baseUrl = '/api/product/admin/distribution-products';

export async function getDistributionProductList(params?: Record<string, any>) {
  return request<API.ProductDistribution.PageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function getDistributionProduct(spuId: number) {
  return request<API.ProductDistribution.InfoResult>(`${baseUrl}/${spuId}`, {
    method: 'GET',
  });
}

export async function addDistributionProduct(data: API.ProductDistribution.Spu) {
  return request<API.Result>(baseUrl, {
    method: 'POST',
    data,
  });
}

export async function updateDistributionProduct(
  spuId: number,
  data: API.ProductDistribution.Spu,
) {
  return request<API.Result>(`${baseUrl}/${spuId}`, {
    method: 'PUT',
    data,
  });
}

export async function updateDistributionProductStatus(
  spuId: number,
  status: string,
) {
  return request<API.Result>(`${baseUrl}/${spuId}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function updateDistributionSkuStatus(
  spuId: number,
  skuId: number,
  status: string,
) {
  return request<API.Result>(`${baseUrl}/${spuId}/skus/${skuId}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function getDistributionProductSkus(spuId: number) {
  return request<API.ProductDistribution.SkuListResult>(
    `${baseUrl}/${spuId}/skus`,
    { method: 'GET' },
  );
}
