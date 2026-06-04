import { request } from '@umijs/max';

export async function getAdminBuyerList(params?: API.Partner.BuyerListParams) {
  return request<API.Partner.BuyerPageResult>('/api/buyer/admin/buyers/list', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    params,
  });
}

export async function getAdminBuyer(buyerId: number) {
  return request<API.Partner.BuyerInfoResult>(`/api/buyer/admin/buyers/${buyerId}`, {
    method: 'GET',
  });
}

export async function addAdminBuyer(data: API.Partner.Buyer) {
  return request<API.Result>('/api/buyer/admin/buyers', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminBuyer(data: API.Partner.Buyer) {
  return request<API.Result>('/api/buyer/admin/buyers', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function changeAdminBuyerStatus(data: Pick<API.Partner.Buyer, 'buyerId' | 'status'>) {
  return request<API.Result>('/api/buyer/admin/buyers/changeStatus', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function getAdminBuyerAccounts(buyerId: number) {
  return request<API.Partner.BuyerAccountListResult>(`/api/buyer/admin/buyers/${buyerId}/accounts`, {
    method: 'GET',
  });
}

export async function addAdminBuyerAccount(buyerId: number, data: API.Partner.BuyerAccount) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/accounts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function resetAdminBuyerAccountPassword(data: Pick<API.Partner.BuyerAccount, 'userId' | 'password'>) {
  return request<API.Result>('/api/buyer/admin/buyers/accounts/resetPwd', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function resetAdminBuyerAccountDefaultPassword(data: Pick<API.Partner.BuyerAccount, 'userId'>) {
  return request<API.Result>('/api/buyer/admin/buyers/accounts/resetDefaultPwd', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function resetAdminBuyerOwnerPassword(buyerId: number) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/resetOwnerPwd`, {
    method: 'PUT',
  });
}

export async function createAdminBuyerDirectLogin(buyerId: number) {
  return request<API.Partner.DirectLoginApiResult>(`/api/buyer/admin/buyers/${buyerId}/directLogin`, {
    method: 'POST',
  });
}
