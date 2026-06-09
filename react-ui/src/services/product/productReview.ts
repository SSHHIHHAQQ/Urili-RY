import { request } from '@umijs/max';

const baseUrl = '/api/product/admin/reviews';

export async function getProductReviewList(params?: Record<string, any>) {
  return request<API.ProductReview.PageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function getProductReview(reviewId: number) {
  return request<API.ProductReview.InfoResult>(`${baseUrl}/${reviewId}`, {
    method: 'GET',
  });
}

export async function approveProductReview(reviewId: number, reason?: string) {
  return request<API.Result>(`${baseUrl}/${reviewId}/approve`, {
    method: 'POST',
    data: { reason },
  });
}

export async function rejectProductReview(reviewId: number, reason: string) {
  return request<API.Result>(`${baseUrl}/${reviewId}/reject`, {
    method: 'POST',
    data: { reason },
  });
}

export async function getProductReviewLogs(reviewId: number) {
  return request<API.ProductReview.LogListResult>(`${baseUrl}/${reviewId}/logs`, {
    method: 'GET',
  });
}
