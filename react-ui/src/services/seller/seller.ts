import { request } from '@umijs/max';

export async function getAdminSellerList(params?: API.Partner.SellerListParams) {
  return request<API.Partner.SellerPageResult>('/api/seller/admin/sellers/list', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    params,
  });
}

export async function getAdminSeller(sellerId: number) {
  return request<API.Partner.SellerInfoResult>(`/api/seller/admin/sellers/${sellerId}`, {
    method: 'GET',
  });
}

export async function addAdminSeller(data: API.Partner.Seller) {
  return request<API.Result>('/api/seller/admin/sellers', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminSeller(data: API.Partner.Seller) {
  return request<API.Result>('/api/seller/admin/sellers', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function changeAdminSellerStatus(data: Pick<API.Partner.Seller, 'sellerId' | 'status'>) {
  return request<API.Result>('/api/seller/admin/sellers/changeStatus', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function getAdminSellerAccounts(sellerId: number) {
  return request<API.Partner.SellerAccountListResult>(`/api/seller/admin/sellers/${sellerId}/accounts`, {
    method: 'GET',
  });
}

export async function addAdminSellerAccount(sellerId: number, data: API.Partner.SellerAccount) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/accounts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminSellerAccount(sellerId: number, data: API.Partner.SellerAccount) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/accounts`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function getAdminSellerDepts(sellerId: number) {
  return request<API.Partner.PortalDeptListResult>(`/api/seller/admin/sellers/${sellerId}/depts/list`, {
    method: 'GET',
  });
}

export async function getAdminSellerDeptTree(sellerId: number) {
  return request<API.Partner.PortalDeptTreeResult>(`/api/seller/admin/sellers/${sellerId}/depts/treeselect`, {
    method: 'GET',
  });
}

export async function resetAdminSellerAccountPassword(data: Pick<API.Partner.SellerAccount, 'sellerAccountId' | 'password'>) {
  return request<API.Result>('/api/seller/admin/sellers/accounts/resetPwd', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function resetAdminSellerAccountDefaultPassword(data: Pick<API.Partner.SellerAccount, 'sellerAccountId'>) {
  return request<API.Result>('/api/seller/admin/sellers/accounts/resetDefaultPwd', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function resetAdminSellerOwnerPassword(sellerId: number) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/resetOwnerPwd`, {
    method: 'PUT',
  });
}

export async function forceLogoutAdminSellerSessions(sellerId: number) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/sessions`, {
    method: 'DELETE',
  });
}

export async function forceLogoutAdminSellerAccountSessions(sellerId: number, sellerAccountId: number) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/sessions`, {
    method: 'DELETE',
  });
}

export async function createAdminSellerDirectLogin(sellerId: number) {
  return request<API.Partner.DirectLoginApiResult>(`/api/seller/admin/sellers/${sellerId}/directLogin`, {
    method: 'POST',
  });
}
