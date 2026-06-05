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

export async function lockAdminSellerAccount(sellerId: number, sellerAccountId: number, lockReason: string) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/lock`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data: { lockReason },
  });
}

export async function unlockAdminSellerAccount(sellerId: number, sellerAccountId: number) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/unlock`, {
    method: 'PUT',
  });
}

export async function getAdminSellerDepts(sellerId: number) {
  return request<API.Partner.PortalDeptListResult>(`/api/seller/admin/sellers/${sellerId}/depts/list`, {
    method: 'GET',
  });
}

export async function getAdminSellerDept(sellerId: number, deptId: number) {
  return request<{ code: number; msg?: string; data: API.Partner.PortalDept }>(`/api/seller/admin/sellers/${sellerId}/depts/${deptId}`, {
    method: 'GET',
  });
}

export async function getAdminSellerDeptTree(sellerId: number) {
  return request<API.Partner.PortalDeptTreeResult>(`/api/seller/admin/sellers/${sellerId}/depts/treeselect`, {
    method: 'GET',
  });
}

export async function addAdminSellerDept(sellerId: number, data: API.Partner.PortalDept) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/depts`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminSellerDept(sellerId: number, data: API.Partner.PortalDept) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/depts`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function removeAdminSellerDept(sellerId: number, deptId: number) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/depts/${deptId}`, {
    method: 'DELETE',
  });
}

export async function getAdminSellerAccountRoles(sellerId: number, sellerAccountId: number) {
  return request<API.Partner.PortalAccountRoleResult>(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/roles`, {
    method: 'GET',
  });
}

export async function assignAdminSellerAccountRoles(sellerId: number, sellerAccountId: number, roleIds: number[]) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/roles`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data: { roleIds },
  });
}

export async function getAdminSellerMenuTree() {
  return request<API.Partner.PortalMenuTreeResult>('/api/seller/admin/menus/treeselect', {
    method: 'GET',
  });
}

export async function getAdminSellerMenus(params?: Record<string, any>) {
  return request<API.Partner.PortalMenuListResult>('/api/seller/admin/menus/list', {
    method: 'GET',
    params,
  });
}

export async function getAdminSellerMenu(menuId: number) {
  return request<API.Partner.PortalMenuInfoResult>(`/api/seller/admin/menus/${menuId}`, {
    method: 'GET',
  });
}

export async function addAdminSellerMenu(data: API.Partner.PortalMenu) {
  return request<API.Result>('/api/seller/admin/menus', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminSellerMenu(data: API.Partner.PortalMenu) {
  return request<API.Result>('/api/seller/admin/menus', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function removeAdminSellerMenu(menuId: number) {
  return request<API.Result>(`/api/seller/admin/menus/${menuId}`, {
    method: 'DELETE',
  });
}

export async function getAdminSellerRoleMenuTree(sellerId: number, roleId: number) {
  return request<API.Partner.PortalRoleMenuTreeResult>(`/api/seller/admin/menus/roleMenuTreeselect/${sellerId}/${roleId}`, {
    method: 'GET',
  });
}

export async function getAdminSellerRoles(sellerId: number, params?: Record<string, any>) {
  return request<API.Partner.PortalRolePageResult>(`/api/seller/admin/sellers/${sellerId}/roles/list`, {
    method: 'GET',
    params,
  });
}

export async function getAdminSellerRole(sellerId: number, roleId: number) {
  return request<API.Partner.PortalRoleInfoResult>(`/api/seller/admin/sellers/${sellerId}/roles/${roleId}`, {
    method: 'GET',
  });
}

export async function addAdminSellerRole(sellerId: number, data: API.Partner.PortalRole) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/roles`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function updateAdminSellerRole(sellerId: number, data: API.Partner.PortalRole) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/roles`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function changeAdminSellerRoleStatus(sellerId: number, data: Pick<API.Partner.PortalRole, 'roleId' | 'status'>) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/roles/changeStatus`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data,
  });
}

export async function removeAdminSellerRoles(sellerId: number, roleIds: number[]) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/roles/${roleIds.join(',')}`, {
    method: 'DELETE',
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

export async function getAdminSellerSessions(sellerId: number, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalSessionProfile>>(
    `/api/seller/admin/sellers/${sellerId}/sessions/list`,
    {
      method: 'GET',
      params,
    },
  );
}

export async function forceLogoutAdminSellerAccountSessions(sellerId: number, sellerAccountId: number) {
  return request<API.Result>(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/sessions`, {
    method: 'DELETE',
  });
}

export async function getAdminSellerAccountSessions(sellerId: number, sellerAccountId: number, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalSessionProfile>>(
    `/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/sessions/list`,
    {
      method: 'GET',
      params,
    },
  );
}

export async function createAdminSellerDirectLogin(sellerId: number, reason: string) {
  return request<API.Partner.DirectLoginApiResult>(`/api/seller/admin/sellers/${sellerId}/directLogin`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
    },
    data: { reason },
  });
}

export async function createAdminSellerAccountDirectLogin(sellerId: number, sellerAccountId: number, reason: string) {
  return request<API.Partner.DirectLoginApiResult>(
    `/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/directLogin`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
      },
      data: { reason },
    },
  );
}

export async function getAdminSellerLoginLogs(params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalLoginLog>>('/api/seller/admin/sellers/loginLogs/list', {
    method: 'GET',
    params,
  });
}

export async function getAdminSellerOperLogs(params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalOperLog>>('/api/seller/admin/sellers/operLogs/list', {
    method: 'GET',
    params,
  });
}

export async function getAdminSellerDirectLoginTickets(params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalDirectLoginTicket>>('/api/seller/admin/sellers/directLoginTickets/list', {
    method: 'GET',
    params,
  });
}
