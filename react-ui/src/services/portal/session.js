import { getTerminalAccessToken } from '@/access';
import { request } from '@umijs/max';
const TERMINAL_PATH = {
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
function buildPortalUrl(terminal, path) {
    return `/api/${TERMINAL_PATH[terminal]}${path}`;
}
function buildPortalAuthHeaders(terminal) {
    const token = getTerminalAccessToken(terminal);
    return token ? { Authorization: `Bearer ${token}` } : undefined;
}
function sanitizePortalQueryParams(params) {
    if (!params) {
        return undefined;
    }
    return Object.fromEntries(Object.entries(params).filter(([key]) => !PORTAL_SCOPE_PARAM_KEYS.has(key)));
}
export async function portalLogin(terminal, data) {
    return request(buildPortalUrl(terminal, '/login'), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
            isToken: false,
        },
        data,
    });
}
export async function portalDirectLogin(terminal, directLoginToken) {
    return request(buildPortalUrl(terminal, '/direct-login'), {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
            isToken: false,
        },
        data: { directLoginToken },
    });
}
export async function portalLogout(terminal) {
    return request(buildPortalUrl(terminal, '/logout'), {
        method: 'POST',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    });
}
export async function getPortalInfo(terminal) {
    return request(buildPortalUrl(terminal, '/getInfo'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    });
}
export async function getPortalRouters(terminal) {
    return request(buildPortalUrl(terminal, '/getRouters'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    });
}
export async function getPortalSubjectProfile(terminal) {
    return request(buildPortalUrl(terminal, '/profile'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    });
}
export async function getPortalAccountProfile(terminal) {
    return request(buildPortalUrl(terminal, '/account/profile'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    });
}
export async function updatePortalPassword(terminal, data) {
    return request(buildPortalUrl(terminal, '/account/password'), {
        method: 'PUT',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
        data,
    });
}
export async function getPortalAccounts(terminal) {
    return request(buildPortalUrl(terminal, '/accounts'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    });
}
export async function getPortalDepts(terminal) {
    return request(buildPortalUrl(terminal, '/depts'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    });
}
export async function getPortalRoles(terminal) {
    return request(buildPortalUrl(terminal, '/roles'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
    });
}
export async function getPortalLoginLogs(terminal, params) {
    return request(buildPortalUrl(terminal, '/account/login-logs'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
        params: sanitizePortalQueryParams(params),
    });
}
export async function getPortalOperLogs(terminal, params) {
    return request(buildPortalUrl(terminal, '/account/oper-logs'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
        params: sanitizePortalQueryParams(params),
    });
}
export async function getPortalSessions(terminal, params) {
    return request(buildPortalUrl(terminal, '/account/sessions'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders(terminal), isToken: false },
        params: sanitizePortalQueryParams(params),
    });
}
export async function getSellerPortalProductCategories() {
    return request(buildPortalUrl('seller', '/product/categories'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
    });
}
export async function getSellerPortalProductSchema(categoryId) {
    return request(buildPortalUrl('seller', `/product/categories/${categoryId}/schema`), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
    });
}
export async function getSellerPortalDistributionProducts(params) {
    return request(buildPortalUrl('seller', '/product/distribution-products/list'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
        params: sanitizePortalQueryParams(params),
    });
}
export async function getSellerPortalDistributionProduct(spuId) {
    return request(buildPortalUrl('seller', `/product/distribution-products/${spuId}`), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
    });
}
export async function getSellerPortalDistributionProductSkus(spuId) {
    return request(buildPortalUrl('seller', `/product/distribution-products/${spuId}/skus`), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('seller'), isToken: false },
    });
}
export async function getBuyerPortalProductCategories() {
    return request(buildPortalUrl('buyer', '/product/categories'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    });
}
export async function getBuyerPortalProductSchema(categoryId) {
    return request(buildPortalUrl('buyer', `/product/categories/${categoryId}/schema`), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    });
}
export async function getBuyerPortalDistributionProducts(params) {
    return request(buildPortalUrl('buyer', '/product/distribution-products/list'), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
        params: sanitizePortalQueryParams(params),
    });
}
export async function getBuyerPortalDistributionProduct(spuId) {
    return request(buildPortalUrl('buyer', `/product/distribution-products/${spuId}`), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    });
}
export async function getBuyerPortalDistributionProductSkus(spuId) {
    return request(buildPortalUrl('buyer', `/product/distribution-products/${spuId}/skus`), {
        method: 'GET',
        headers: { ...buildPortalAuthHeaders('buyer'), isToken: false },
    });
}
export const sellerPortalSessionService = {
    login: (data) => portalLogin('seller', data),
    directLogin: (directLoginToken) => portalDirectLogin('seller', directLoginToken),
    logout: () => portalLogout('seller'),
    getInfo: () => getPortalInfo('seller'),
    getRouters: () => getPortalRouters('seller'),
    getSubjectProfile: () => getPortalSubjectProfile('seller'),
    getAccountProfile: () => getPortalAccountProfile('seller'),
    updatePassword: (data) => updatePortalPassword('seller', data),
    getAccounts: () => getPortalAccounts('seller'),
    getDepts: () => getPortalDepts('seller'),
    getRoles: () => getPortalRoles('seller'),
    getLoginLogs: (params) => getPortalLoginLogs('seller', params),
    getOperLogs: (params) => getPortalOperLogs('seller', params),
    getSessions: (params) => getPortalSessions('seller', params),
};
export const buyerPortalSessionService = {
    login: (data) => portalLogin('buyer', data),
    directLogin: (directLoginToken) => portalDirectLogin('buyer', directLoginToken),
    logout: () => portalLogout('buyer'),
    getInfo: () => getPortalInfo('buyer'),
    getRouters: () => getPortalRouters('buyer'),
    getSubjectProfile: () => getPortalSubjectProfile('buyer'),
    getAccountProfile: () => getPortalAccountProfile('buyer'),
    updatePassword: (data) => updatePortalPassword('buyer', data),
    getAccounts: () => getPortalAccounts('buyer'),
    getDepts: () => getPortalDepts('buyer'),
    getRoles: () => getPortalRoles('buyer'),
    getLoginLogs: (params) => getPortalLoginLogs('buyer', params),
    getOperLogs: (params) => getPortalOperLogs('buyer', params),
    getSessions: (params) => getPortalSessions('buyer', params),
};
