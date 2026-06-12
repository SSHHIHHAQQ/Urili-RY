import { getTerminalAccessToken } from '@/access';
import { request } from '@umijs/max';

type PortalTerminal = API.Partner.PortalTerminal;
type PortalSessionPageParams = {
  pageNum?: number;
  pageSize?: number;
};

const TERMINAL_PATH: Record<PortalTerminal, string> = {
  seller: 'seller',
  buyer: 'buyer',
};
const PORTAL_SCOPE_PARAM_KEYS = new Set([
  'sellerId',
  'buyerId',
  'subjectId',
  'accountId',
  'sellerAccountId',
  'buyerAccountId',
  'terminal',
]);

function buildPortalUrl(terminal: PortalTerminal, path: string) {
  return `/api/${TERMINAL_PATH[terminal]}${path}`;
}

function buildPortalAuthHeaders(terminal: PortalTerminal): Record<string, string> | undefined {
  const token = getTerminalAccessToken(terminal);
  return token ? { Authorization: `Bearer ${token}` } : undefined;
}

function sanitizePortalQueryParams(params?: Record<string, any>) {
  if (!params) {
    return undefined;
  }
  return Object.fromEntries(
    Object.entries(params).filter(([key]) => !PORTAL_SCOPE_PARAM_KEYS.has(key)),
  );
}

function sanitizePortalPayload(data?: Record<string, any>) {
  if (!data) {
    return undefined;
  }
  return Object.fromEntries(
    Object.entries(data).filter(
      ([key, value]) => !PORTAL_SCOPE_PARAM_KEYS.has(key) && value !== undefined,
    ),
  );
}

function normalizePortalIdArray(values: unknown, fieldName: string) {
  if (values == null) {
    return [];
  }
  if (!Array.isArray(values)) {
    throw new Error(`${fieldName} 必须是数组`);
  }
  const ids: number[] = [];
  const seen = new Set<number>();
  for (const value of values) {
    const id = typeof value === 'number' ? value : Number(String(value).trim());
    if (!Number.isSafeInteger(id) || id <= 0) {
      throw new Error(`${fieldName} 必须使用有效正整数ID`);
    }
    if (seen.has(id)) {
      throw new Error(`${fieldName} 不能包含重复ID`);
    }
    seen.add(id);
    ids.push(id);
  }
  return ids;
}

function normalizePortalIdentifier(value: unknown, fieldName: string) {
  const id = typeof value === 'number' ? value : Number(String(value).trim());
  if (!Number.isSafeInteger(id) || id <= 0) {
    throw new Error(`${fieldName} must be a positive integer id`);
  }
  return id;
}

function sanitizePortalRolePayload(data?: Record<string, any>) {
  const payload = sanitizePortalPayload(data);
  if (!payload) {
    return undefined;
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'menuIds')) {
    return {
      ...payload,
      menuIds: normalizePortalIdArray(payload.menuIds, 'menuIds'),
    };
  }
  return payload;
}

function sanitizePortalSessionPageParams(params?: PortalSessionPageParams) {
  if (!params) {
    return undefined;
  }
  const result: PortalSessionPageParams = {};
  if (params.pageNum != null) {
    result.pageNum = params.pageNum;
  }
  if (params.pageSize != null) {
    result.pageSize = params.pageSize;
  }
  return Object.keys(result).length > 0 ? result : undefined;
}

export async function portalLogin(terminal: PortalTerminal, data: API.Partner.PortalLoginParams) {
  return request<API.Partner.PortalLoginApiResult>(buildPortalUrl(terminal, '/login'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      isToken: false,
    },
    data,
  });
}

export async function portalDirectLogin(terminal: PortalTerminal, directLoginToken: string) {
  return request<API.Partner.PortalLoginApiResult>(buildPortalUrl(terminal, '/direct-login'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      isToken: false,
    },
    data: { directLoginToken },
  });
}

export async function portalLogout(terminal: PortalTerminal) {
  return request<API.Result>(buildPortalUrl(terminal, '/logout'), {
    method: 'POST',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalInfo(terminal: PortalTerminal) {
  return request<API.Partner.PortalPermissionInfoResult>(buildPortalUrl(terminal, '/getInfo'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalRouters(terminal: PortalTerminal) {
  return request<API.GetRoutersResult>(buildPortalUrl(terminal, '/getRouters'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalSubjectProfile(terminal: PortalTerminal) {
  return request<API.Partner.PortalSubjectProfileResult>(buildPortalUrl(terminal, '/profile'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalAccountProfile(terminal: PortalTerminal) {
  return request<API.Partner.PortalAccountProfileResult>(buildPortalUrl(terminal, '/account/profile'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function updatePortalPassword(terminal: PortalTerminal, data: API.Partner.PortalPasswordChangeParams) {
  return request<API.Result>(buildPortalUrl(terminal, '/account/password'), {
    method: 'PUT',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    data: sanitizePortalPayload(data),
  });
}

export async function getPortalAccounts(terminal: PortalTerminal) {
  return request<API.Partner.PortalAccountListResult>(buildPortalUrl(terminal, '/accounts'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function createPortalAccount(
  terminal: PortalTerminal,
  data: API.Partner.PortalAccountPayload,
) {
  return request<API.Result>(buildPortalUrl(terminal, '/accounts'), {
    method: 'POST',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    data: sanitizePortalPayload(data),
  });
}

export async function updatePortalAccount(
  terminal: PortalTerminal,
  accountIdentifier: number,
  data: API.Partner.PortalAccountPayload,
) {
  const accountId = normalizePortalIdentifier(accountIdentifier, 'accountIdentifier');
  return request<API.Result>(buildPortalUrl(terminal, `/accounts/${accountId}`), {
    method: 'PUT',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    data: sanitizePortalPayload(data),
  });
}

export async function getPortalAccountRoles(terminal: PortalTerminal, accountIdentifier: number) {
  const accountId = normalizePortalIdentifier(accountIdentifier, 'accountIdentifier');
  return request<API.Partner.PortalAccountRoleResult>(
    buildPortalUrl(terminal, `/accounts/${accountId}/roles`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    },
  );
}

export async function assignPortalAccountRoles(
  terminal: PortalTerminal,
  accountIdentifier: number,
  roleIds: number[],
) {
  const accountId = normalizePortalIdentifier(accountIdentifier, 'accountIdentifier');
  return request<API.Result>(buildPortalUrl(terminal, `/accounts/${accountId}/roles`), {
    method: 'PUT',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    data: { roleIds: normalizePortalIdArray(roleIds, 'roleIds') },
  });
}

export async function getPortalDepts(terminal: PortalTerminal) {
  return request<API.Partner.PortalDeptProfileListResult>(buildPortalUrl(terminal, '/depts'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalDept(terminal: PortalTerminal, deptIdentifier: number) {
  const deptId = normalizePortalIdentifier(deptIdentifier, 'deptIdentifier');
  return request<API.Partner.PortalDeptInfoResult>(buildPortalUrl(terminal, `/depts/${deptId}`), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalDeptTree(terminal: PortalTerminal) {
  return request<API.Partner.PortalDeptTreeResult>(buildPortalUrl(terminal, '/depts/treeselect'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function createPortalDept(terminal: PortalTerminal, data: API.Partner.PortalDept) {
  return request<API.Result>(buildPortalUrl(terminal, '/depts'), {
    method: 'POST',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    data: sanitizePortalPayload(data),
  });
}

export async function updatePortalDept(
  terminal: PortalTerminal,
  deptIdentifier: number,
  data: API.Partner.PortalDept,
) {
  const deptId = normalizePortalIdentifier(deptIdentifier, 'deptIdentifier');
  return request<API.Result>(buildPortalUrl(terminal, `/depts/${deptId}`), {
    method: 'PUT',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    data: sanitizePortalPayload(data),
  });
}

export async function deletePortalDept(terminal: PortalTerminal, deptIdentifier: number) {
  const deptId = normalizePortalIdentifier(deptIdentifier, 'deptIdentifier');
  return request<API.Result>(buildPortalUrl(terminal, `/depts/${deptId}`), {
    method: 'DELETE',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalRoles(terminal: PortalTerminal) {
  return request<API.Partner.PortalRoleProfileListResult>(buildPortalUrl(terminal, '/roles'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalRole(terminal: PortalTerminal, roleIdentifier: number) {
  const roleId = normalizePortalIdentifier(roleIdentifier, 'roleIdentifier');
  return request<API.Partner.PortalRoleInfoResult>(buildPortalUrl(terminal, `/roles/${roleId}`), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalRoleMenus(terminal: PortalTerminal, roleIdentifier?: number) {
  const path =
    roleIdentifier == null
      ? '/roles/menus'
      : `/roles/${normalizePortalIdentifier(roleIdentifier, 'roleIdentifier')}/menus`;
  return request<API.Partner.PortalRoleMenuTreeResult>(buildPortalUrl(terminal, path), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function createPortalRole(terminal: PortalTerminal, data: API.Partner.PortalRole) {
  return request<API.Result>(buildPortalUrl(terminal, '/roles'), {
    method: 'POST',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    data: sanitizePortalRolePayload(data),
  });
}

export async function updatePortalRole(
  terminal: PortalTerminal,
  roleIdentifier: number,
  data: API.Partner.PortalRole,
) {
  const roleId = normalizePortalIdentifier(roleIdentifier, 'roleIdentifier');
  return request<API.Result>(buildPortalUrl(terminal, `/roles/${roleId}`), {
    method: 'PUT',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    data: sanitizePortalRolePayload(data),
  });
}

export async function deletePortalRole(terminal: PortalTerminal, roleIdentifier: number) {
  const roleId = normalizePortalIdentifier(roleIdentifier, 'roleIdentifier');
  return request<API.Result>(buildPortalUrl(terminal, `/roles/${roleId}`), {
    method: 'DELETE',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalLoginLogs(terminal: PortalTerminal, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalOwnLoginLogProfile>>(
    buildPortalUrl(terminal, '/account/login-logs'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
      params: sanitizePortalQueryParams(params),
    },
  );
}

export async function getPortalOperLogs(terminal: PortalTerminal, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalOwnOperLogProfile>>(
    buildPortalUrl(terminal, '/account/oper-logs'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
      params: sanitizePortalQueryParams(params),
    },
  );
}

export async function getPortalSessions(terminal: PortalTerminal, params?: PortalSessionPageParams) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalOwnSessionProfile>>(
    buildPortalUrl(terminal, '/account/sessions'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
      params: sanitizePortalSessionPageParams(params),
    },
  );
}

export async function getSellerPortalProductCategories() {
  return request<API.Partner.PortalProductCategoryListResult>(
    buildPortalUrl('seller', '/product/categories'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
    },
  );
}

export async function getSellerPortalProductSchema(categoryId: number) {
  return request<API.Partner.PortalProductSchemaResult>(
    buildPortalUrl('seller', `/product/categories/${categoryId}/schema`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
    },
  );
}

export async function getSellerPortalDistributionProducts(params?: Record<string, any>) {
  return request<API.Partner.SellerPortalProductPageResult>(
    buildPortalUrl('seller', '/product/distribution-products/list'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
      params: sanitizePortalQueryParams(params),
    },
  );
}

export async function getSellerPortalDistributionProduct(spuId: number) {
  return request<API.Partner.SellerPortalProductInfoResult>(
    buildPortalUrl('seller', `/product/distribution-products/${spuId}`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
    },
  );
}

export async function getSellerPortalDistributionProductSkus(spuId: number) {
  return request<API.Partner.SellerPortalProductSkuListResult>(
    buildPortalUrl('seller', `/product/distribution-products/${spuId}/skus`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
    },
  );
}

export async function getBuyerPortalProductCategories() {
  return request<API.Partner.PortalProductCategoryListResult>(
    buildPortalUrl('buyer', '/product/categories'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    },
  );
}

export async function getBuyerPortalProductSchema(categoryId: number) {
  return request<API.Partner.PortalProductSchemaResult>(
    buildPortalUrl('buyer', `/product/categories/${categoryId}/schema`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    },
  );
}

export async function getBuyerPortalDistributionProducts(params?: Record<string, any>) {
  return request<API.Partner.BuyerPortalProductPageResult>(
    buildPortalUrl('buyer', '/product/distribution-products/list'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
      params: sanitizePortalQueryParams(params),
    },
  );
}

export async function getBuyerPortalDistributionProduct(spuId: number) {
  return request<API.Partner.BuyerPortalProductInfoResult>(
    buildPortalUrl('buyer', `/product/distribution-products/${spuId}`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    },
  );
}

export async function getBuyerPortalDistributionProductSkus(spuId: number) {
  return request<API.Partner.BuyerPortalProductSkuListResult>(
    buildPortalUrl('buyer', `/product/distribution-products/${spuId}/skus`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    },
  );
}

export async function getBuyerPortalProductCenterProducts(params?: Record<string, any>) {
  return request<API.Partner.BuyerPortalProductPageResult>(
    buildPortalUrl('buyer', '/product/center/list'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
      params: sanitizePortalQueryParams(params),
    },
  );
}

export async function getBuyerPortalProductCenterProduct(spuId: number) {
  return request<API.Partner.BuyerPortalProductInfoResult>(
    buildPortalUrl('buyer', `/product/center/${spuId}`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    },
  );
}

export async function getBuyerPortalProductCenterProductSkus(spuId: number) {
  return request<API.Partner.BuyerPortalProductSkuListResult>(
    buildPortalUrl('buyer', `/product/center/${spuId}/skus`),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    },
  );
}

export const sellerPortalSessionService = {
  login: (data: API.Partner.PortalLoginParams) => portalLogin('seller', data),
  directLogin: (directLoginToken: string) => portalDirectLogin('seller', directLoginToken),
  logout: () => portalLogout('seller'),
  getInfo: () => getPortalInfo('seller'),
  getRouters: () => getPortalRouters('seller'),
  getSubjectProfile: () => getPortalSubjectProfile('seller'),
  getAccountProfile: () => getPortalAccountProfile('seller'),
  updatePassword: (data: API.Partner.PortalPasswordChangeParams) => updatePortalPassword('seller', data),
  getAccounts: () => getPortalAccounts('seller'),
  createAccount: (data: API.Partner.PortalAccountPayload) => createPortalAccount('seller', data),
  updateAccount: (accountIdentifier: number, data: API.Partner.PortalAccountPayload) =>
    updatePortalAccount('seller', accountIdentifier, data),
  getAccountRoles: (accountIdentifier: number) => getPortalAccountRoles('seller', accountIdentifier),
  assignAccountRoles: (accountIdentifier: number, roleIds: number[]) =>
    assignPortalAccountRoles('seller', accountIdentifier, roleIds),
  getDepts: () => getPortalDepts('seller'),
  getDept: (deptIdentifier: number) => getPortalDept('seller', deptIdentifier),
  getDeptTree: () => getPortalDeptTree('seller'),
  createDept: (data: API.Partner.PortalDept) => createPortalDept('seller', data),
  updateDept: (deptIdentifier: number, data: API.Partner.PortalDept) =>
    updatePortalDept('seller', deptIdentifier, data),
  deleteDept: (deptIdentifier: number) => deletePortalDept('seller', deptIdentifier),
  getRoles: () => getPortalRoles('seller'),
  getRole: (roleIdentifier: number) => getPortalRole('seller', roleIdentifier),
  getRoleMenus: (roleIdentifier?: number) => getPortalRoleMenus('seller', roleIdentifier),
  createRole: (data: API.Partner.PortalRole) => createPortalRole('seller', data),
  updateRole: (roleIdentifier: number, data: API.Partner.PortalRole) =>
    updatePortalRole('seller', roleIdentifier, data),
  deleteRole: (roleIdentifier: number) => deletePortalRole('seller', roleIdentifier),
  getLoginLogs: (params?: Record<string, any>) => getPortalLoginLogs('seller', params),
  getOperLogs: (params?: Record<string, any>) => getPortalOperLogs('seller', params),
  getSessions: (params?: PortalSessionPageParams) => getPortalSessions('seller', params),
};

export const buyerPortalSessionService = {
  login: (data: API.Partner.PortalLoginParams) => portalLogin('buyer', data),
  directLogin: (directLoginToken: string) => portalDirectLogin('buyer', directLoginToken),
  logout: () => portalLogout('buyer'),
  getInfo: () => getPortalInfo('buyer'),
  getRouters: () => getPortalRouters('buyer'),
  getSubjectProfile: () => getPortalSubjectProfile('buyer'),
  getAccountProfile: () => getPortalAccountProfile('buyer'),
  updatePassword: (data: API.Partner.PortalPasswordChangeParams) => updatePortalPassword('buyer', data),
  getAccounts: () => getPortalAccounts('buyer'),
  createAccount: (data: API.Partner.PortalAccountPayload) => createPortalAccount('buyer', data),
  updateAccount: (accountIdentifier: number, data: API.Partner.PortalAccountPayload) =>
    updatePortalAccount('buyer', accountIdentifier, data),
  getAccountRoles: (accountIdentifier: number) => getPortalAccountRoles('buyer', accountIdentifier),
  assignAccountRoles: (accountIdentifier: number, roleIds: number[]) =>
    assignPortalAccountRoles('buyer', accountIdentifier, roleIds),
  getDepts: () => getPortalDepts('buyer'),
  getDept: (deptIdentifier: number) => getPortalDept('buyer', deptIdentifier),
  getDeptTree: () => getPortalDeptTree('buyer'),
  createDept: (data: API.Partner.PortalDept) => createPortalDept('buyer', data),
  updateDept: (deptIdentifier: number, data: API.Partner.PortalDept) =>
    updatePortalDept('buyer', deptIdentifier, data),
  deleteDept: (deptIdentifier: number) => deletePortalDept('buyer', deptIdentifier),
  getRoles: () => getPortalRoles('buyer'),
  getRole: (roleIdentifier: number) => getPortalRole('buyer', roleIdentifier),
  getRoleMenus: (roleIdentifier?: number) => getPortalRoleMenus('buyer', roleIdentifier),
  createRole: (data: API.Partner.PortalRole) => createPortalRole('buyer', data),
  updateRole: (roleIdentifier: number, data: API.Partner.PortalRole) =>
    updatePortalRole('buyer', roleIdentifier, data),
  deleteRole: (roleIdentifier: number) => deletePortalRole('buyer', roleIdentifier),
  getLoginLogs: (params?: Record<string, any>) => getPortalLoginLogs('buyer', params),
  getOperLogs: (params?: Record<string, any>) => getPortalOperLogs('buyer', params),
  getSessions: (params?: PortalSessionPageParams) => getPortalSessions('buyer', params),
};
