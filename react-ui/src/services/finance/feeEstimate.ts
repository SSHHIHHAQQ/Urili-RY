import { request } from '@umijs/max';

const baseUrl = '/api/finance/admin/fee-estimate';

export async function getFeeEstimateOptions(schemeId?: number) {
  return request<API.Finance.FeeEstimateOptionsResult>(`${baseUrl}/options`, {
    method: 'GET',
    params: { schemeId },
  });
}

export async function getFeeEstimateSkus(params?: Record<string, any>) {
  return request<API.Finance.FeeEstimateSkuPageResult>(`${baseUrl}/skus/list`, {
    method: 'GET',
    params,
  });
}

export async function calculateFeeEstimate(data: API.Finance.FeeEstimateRequest) {
  return request<API.Finance.FeeEstimateCalculateResult>(`${baseUrl}/calculate`, {
    method: 'POST',
    data,
  });
}
