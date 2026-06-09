import { request } from '@umijs/max';

const baseUrl = '/api/inventory/admin/adjustment-reviews';

export async function getInventoryAdjustmentReviewList(params?: Record<string, any>) {
  return request<API.InventoryAdjustmentReview.PageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function getInventoryAdjustmentReview(reviewId: number) {
  return request<API.InventoryAdjustmentReview.InfoResult>(`${baseUrl}/${reviewId}`, {
    method: 'GET',
  });
}

export async function getInventoryAdjustmentReviewLogs(reviewId: number) {
  return request<API.InventoryAdjustmentReview.LogListResult>(`${baseUrl}/${reviewId}/logs`, {
    method: 'GET',
  });
}

export async function effectNowInventoryAdjustmentReview(reviewId: number, reason?: string) {
  return request<API.InventoryAdjustmentReview.InfoResult>(`${baseUrl}/${reviewId}/effect-now`, {
    method: 'POST',
    data: { reason },
  });
}

export async function changeInventoryAdjustmentReviewEffectiveTime(
  reviewId: number,
  data: { plannedEffectiveTime?: string; reason?: string },
) {
  return request<API.InventoryAdjustmentReview.InfoResult>(`${baseUrl}/${reviewId}/effective-time`, {
    method: 'POST',
    data,
  });
}

export async function rejectInventoryAdjustmentReview(reviewId: number, reason?: string) {
  return request<API.InventoryAdjustmentReview.InfoResult>(`${baseUrl}/${reviewId}/reject`, {
    method: 'POST',
    data: { reason },
  });
}

export async function getInventoryAdjustmentReviewPolicyList(params?: Record<string, any>) {
  return request<API.InventoryAdjustmentReview.PolicyPageResult>(`${baseUrl}/policies/list`, {
    method: 'GET',
    params,
  });
}

export async function saveInventoryAdjustmentReviewPolicy(data: API.InventoryAdjustmentReview.Policy) {
  const policyId = data.policyId;
  return request<API.Result>(policyId ? `${baseUrl}/policies/${policyId}` : `${baseUrl}/policies`, {
    method: policyId ? 'PUT' : 'POST',
    data,
  });
}

export async function getInventoryAdjustmentReviewPolicyBindingList(params?: Record<string, any>) {
  return request<API.InventoryAdjustmentReview.PolicyBindingPageResult>(`${baseUrl}/policy-bindings/list`, {
    method: 'GET',
    params,
  });
}

export async function saveInventoryAdjustmentReviewPolicyBinding(
  data: API.InventoryAdjustmentReview.PolicyBinding,
) {
  const bindingId = data.bindingId;
  return request<API.Result>(
    bindingId ? `${baseUrl}/policy-bindings/${bindingId}` : `${baseUrl}/policy-bindings`,
    {
      method: bindingId ? 'PUT' : 'POST',
      data,
    },
  );
}
