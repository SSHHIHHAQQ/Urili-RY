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

export async function updateAdminBuyerAccount(buyerId: number, data: API.Partner.BuyerAccount) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/accounts`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function lockAdminBuyerAccount(buyerId: number, buyerAccountId: number, lockReason: string) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/lock`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data: { lockReason },
  });
}

export async function unlockAdminBuyerAccount(buyerId: number, buyerAccountId: number) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/unlock`, {
    method: 'PUT',
  });
}

export async function getAdminBuyerAccountRoles(buyerId: number, buyerAccountId: number) {
  return request<API.Partner.PortalAccountRoleResult>(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/roles`, {
    method: 'GET',
  });
}

export async function assignAdminBuyerAccountRoles(buyerId: number, buyerAccountId: number, roleIds: number[]) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/roles`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data: { roleIds },
  });
}

export async function getAdminBuyerMenuTree() {
  return request<API.Partner.PortalMenuTreeResult>('/api/buyer/admin/menus/treeselect', {
    method: 'GET',
  });
}

export async function getAdminBuyerMenus(params?: Record<string, any>) {
  return request<API.Partner.PortalMenuListResult>('/api/buyer/admin/menus/list', {
    method: 'GET',
    params,
  });
}

export async function getAdminBuyerMenu(menuId: number) {
  return request<API.Partner.PortalMenuInfoResult>(`/api/buyer/admin/menus/${menuId}`, {
    method: 'GET',
  });
}

export async function addAdminBuyerMenu(data: API.Partner.PortalMenu) {
  return request<API.Result>('/api/buyer/admin/menus', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminBuyerMenu(data: API.Partner.PortalMenu) {
  return request<API.Result>('/api/buyer/admin/menus', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function removeAdminBuyerMenu(menuId: number) {
  return request<API.Result>(`/api/buyer/admin/menus/${menuId}`, {
    method: 'DELETE',
  });
}

export async function getAdminBuyerRoleMenuTree(buyerId: number, roleId: number) {
  return request<API.Partner.PortalRoleMenuTreeResult>(`/api/buyer/admin/menus/roleMenuTreeselect/${buyerId}/${roleId}`, {
    method: 'GET',
  });
}

export async function getAdminBuyerRoles(buyerId: number, params?: Record<string, any>) {
  return request<API.Partner.PortalRolePageResult>(`/api/buyer/admin/buyers/${buyerId}/roles/list`, {
    method: 'GET',
    params,
  });
}

export async function getAdminBuyerRole(buyerId: number, roleId: number) {
  return request<API.Partner.PortalRoleInfoResult>(`/api/buyer/admin/buyers/${buyerId}/roles/${roleId}`, {
    method: 'GET',
  });
}

export async function addAdminBuyerRole(buyerId: number, data: API.Partner.PortalRole) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/roles`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminBuyerRole(buyerId: number, data: API.Partner.PortalRole) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/roles`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function changeAdminBuyerRoleStatus(buyerId: number, data: Pick<API.Partner.PortalRole, 'roleId' | 'status'>) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/roles/changeStatus`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function removeAdminBuyerRoles(buyerId: number, roleIds: number[]) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/roles/${roleIds.join(',')}`, {
    method: 'DELETE',
  });
}

export async function getAdminBuyerDepts(buyerId: number) {
  return request<API.Partner.PortalDeptListResult>(`/api/buyer/admin/buyers/${buyerId}/depts/list`, {
    method: 'GET',
  });
}

export async function getAdminBuyerDept(buyerId: number, deptId: number) {
  return request<{ code: number; msg?: string; data: API.Partner.PortalDept }>(`/api/buyer/admin/buyers/${buyerId}/depts/${deptId}`, {
    method: 'GET',
  });
}

export async function getAdminBuyerDeptTree(buyerId: number) {
  return request<API.Partner.PortalDeptTreeResult>(`/api/buyer/admin/buyers/${buyerId}/depts/treeselect`, {
    method: 'GET',
  });
}

export async function addAdminBuyerDept(buyerId: number, data: API.Partner.PortalDept) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/depts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminBuyerDept(buyerId: number, data: API.Partner.PortalDept) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/depts`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function removeAdminBuyerDept(buyerId: number, deptId: number) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/depts/${deptId}`, {
    method: 'DELETE',
  });
}

export async function resetAdminBuyerAccountPassword(data: Pick<API.Partner.BuyerAccount, 'buyerAccountId' | 'password'>) {
  return request<API.Result>('/api/buyer/admin/buyers/accounts/resetPwd', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function resetAdminBuyerAccountDefaultPassword(data: Pick<API.Partner.BuyerAccount, 'buyerAccountId'>) {
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

export async function forceLogoutAdminBuyerSessions(buyerId: number) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/sessions`, {
    method: 'DELETE',
  });
}

export async function getAdminBuyerSessions(buyerId: number, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalSessionProfile>>(
    `/api/buyer/admin/buyers/${buyerId}/sessions/list`,
    {
      method: 'GET',
      params,
    },
  );
}

export async function forceLogoutAdminBuyerAccountSessions(buyerId: number, buyerAccountId: number) {
  return request<API.Result>(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/sessions`, {
    method: 'DELETE',
  });
}

export async function getAdminBuyerAccountSessions(buyerId: number, buyerAccountId: number, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalSessionProfile>>(
    `/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/sessions/list`,
    {
      method: 'GET',
      params,
    },
  );
}

export async function createAdminBuyerDirectLogin(buyerId: number, reason: string) {
  return request<API.Partner.DirectLoginApiResult>(`/api/buyer/admin/buyers/${buyerId}/directLogin`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data: { reason },
  });
}

export async function createAdminBuyerAccountDirectLogin(buyerId: number, buyerAccountId: number, reason: string) {
  return request<API.Partner.DirectLoginApiResult>(
    `/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/directLogin`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
      },
      data: { reason },
    },
  );
}

export async function getAdminBuyerLoginLogs(params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalLoginLog>>('/api/buyer/admin/buyers/loginLogs/list', {
    method: 'GET',
    params,
  });
}

export async function getAdminBuyerOperLogs(params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalOperLog>>('/api/buyer/admin/buyers/operLogs/list', {
    method: 'GET',
    params,
  });
}

export async function getAdminBuyerDirectLoginTickets(params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalDirectLoginTicket>>('/api/buyer/admin/buyers/directLoginTickets/list', {
    method: 'GET',
    params,
  });
}
