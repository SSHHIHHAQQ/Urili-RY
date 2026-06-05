import { getTerminalAccessToken } from '@/access';
import { request } from '@umijs/max';

type PortalTerminal = API.Partner.PortalTerminal;

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
    data,
  });
}

export async function getPortalAccounts(terminal: PortalTerminal) {
  return request<API.Partner.PortalAccountListResult>(buildPortalUrl(terminal, '/accounts'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalDepts(terminal: PortalTerminal) {
  return request<API.Partner.PortalDeptProfileListResult>(buildPortalUrl(terminal, '/depts'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalRoles(terminal: PortalTerminal) {
  return request<API.Partner.PortalRoleProfileListResult>(buildPortalUrl(terminal, '/roles'), {
    method: 'GET',
    headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
  });
}

export async function getPortalLoginLogs(terminal: PortalTerminal, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalLoginLog>>(
    buildPortalUrl(terminal, '/account/login-logs'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
      params: sanitizePortalQueryParams(params),
    },
  );
}

export async function getPortalOperLogs(terminal: PortalTerminal, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalOperLog>>(
    buildPortalUrl(terminal, '/account/oper-logs'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
      params: sanitizePortalQueryParams(params),
    },
  );
}

export async function getPortalSessions(terminal: PortalTerminal, params?: Record<string, any>) {
  return request<API.Partner.PortalAuditPageResult<API.Partner.PortalSessionProfile>>(
    buildPortalUrl(terminal, '/account/sessions'),
    {
      method: 'GET',
      headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
      params: sanitizePortalQueryParams(params),
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
  getDepts: () => getPortalDepts('seller'),
  getRoles: () => getPortalRoles('seller'),
  getLoginLogs: (params?: Record<string, any>) => getPortalLoginLogs('seller', params),
  getOperLogs: (params?: Record<string, any>) => getPortalOperLogs('seller', params),
  getSessions: (params?: Record<string, any>) => getPortalSessions('seller', params),
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
  getDepts: () => getPortalDepts('buyer'),
  getRoles: () => getPortalRoles('buyer'),
  getLoginLogs: (params?: Record<string, any>) => getPortalLoginLogs('buyer', params),
  getOperLogs: (params?: Record<string, any>) => getPortalOperLogs('buyer', params),
  getSessions: (params?: Record<string, any>) => getPortalSessions('buyer', params),
};
