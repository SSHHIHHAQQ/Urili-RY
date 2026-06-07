import { request } from '@umijs/max';
export async function getAdminSellerList(params) {
    return request('/api/seller/admin/sellers/list', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        params,
    });
}
export async function getAdminSeller(sellerId) {
    return request(`/api/seller/admin/sellers/${sellerId}`, {
        method: 'GET',
    });
}
export async function addAdminSeller(data) {
    return request('/api/seller/admin/sellers', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminSeller(data) {
    return request('/api/seller/admin/sellers', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function changeAdminSellerStatus(data) {
    return request('/api/seller/admin/sellers/changeStatus', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function getAdminSellerAccounts(sellerId) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts`, {
        method: 'GET',
    });
}
export async function addAdminSellerAccount(sellerId, data) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminSellerAccount(sellerId, data) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function lockAdminSellerAccount(sellerId, sellerAccountId, lockReason) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/lock`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { lockReason },
    });
}
export async function unlockAdminSellerAccount(sellerId, sellerAccountId) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/unlock`, {
        method: 'PUT',
    });
}
export async function getAdminSellerDepts(sellerId) {
    return request(`/api/seller/admin/sellers/${sellerId}/depts/list`, {
        method: 'GET',
    });
}
export async function getAdminSellerDept(sellerId, deptId) {
    return request(`/api/seller/admin/sellers/${sellerId}/depts/${deptId}`, {
        method: 'GET',
    });
}
export async function getAdminSellerDeptTree(sellerId) {
    return request(`/api/seller/admin/sellers/${sellerId}/depts/treeselect`, {
        method: 'GET',
    });
}
export async function addAdminSellerDept(sellerId, data) {
    return request(`/api/seller/admin/sellers/${sellerId}/depts`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminSellerDept(sellerId, data) {
    return request(`/api/seller/admin/sellers/${sellerId}/depts`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function removeAdminSellerDept(sellerId, deptId) {
    return request(`/api/seller/admin/sellers/${sellerId}/depts/${deptId}`, {
        method: 'DELETE',
    });
}
export async function getAdminSellerAccountRoles(sellerId, sellerAccountId) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/roles`, {
        method: 'GET',
    });
}
export async function assignAdminSellerAccountRoles(sellerId, sellerAccountId, roleIds) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/roles`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { roleIds },
    });
}
export async function getAdminSellerMenuTree() {
    return request('/api/seller/admin/menus/treeselect', {
        method: 'GET',
    });
}
export async function getAdminSellerMenus(params) {
    return request('/api/seller/admin/menus/list', {
        method: 'GET',
        params,
    });
}
export async function getAdminSellerMenu(menuId) {
    return request(`/api/seller/admin/menus/${menuId}`, {
        method: 'GET',
    });
}
export async function addAdminSellerMenu(data) {
    return request('/api/seller/admin/menus', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminSellerMenu(data) {
    return request('/api/seller/admin/menus', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function removeAdminSellerMenu(menuId) {
    return request(`/api/seller/admin/menus/${menuId}`, {
        method: 'DELETE',
    });
}
export async function getAdminSellerRoleMenuTree(sellerId, roleId) {
    return request(`/api/seller/admin/menus/roleMenuTreeselect/${sellerId}/${roleId}`, {
        method: 'GET',
    });
}
export async function getAdminSellerRoles(sellerId, params) {
    return request(`/api/seller/admin/sellers/${sellerId}/roles/list`, {
        method: 'GET',
        params,
    });
}
export async function getAdminSellerRole(sellerId, roleId) {
    return request(`/api/seller/admin/sellers/${sellerId}/roles/${roleId}`, {
        method: 'GET',
    });
}
export async function addAdminSellerRole(sellerId, data) {
    return request(`/api/seller/admin/sellers/${sellerId}/roles`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminSellerRole(sellerId, data) {
    return request(`/api/seller/admin/sellers/${sellerId}/roles`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function changeAdminSellerRoleStatus(sellerId, data) {
    return request(`/api/seller/admin/sellers/${sellerId}/roles/changeStatus`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function removeAdminSellerRoles(sellerId, roleIds) {
    return request(`/api/seller/admin/sellers/${sellerId}/roles/${roleIds.join(',')}`, {
        method: 'DELETE',
    });
}
export async function resetAdminSellerAccountPassword(sellerId, sellerAccountId, password) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/resetPwd`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { password },
    });
}
export async function forceLogoutAdminSellerSessions(sellerId) {
    return request(`/api/seller/admin/sellers/${sellerId}/sessions`, {
        method: 'DELETE',
    });
}
export async function getAdminSellerSessions(sellerId, params) {
    return request(`/api/seller/admin/sellers/${sellerId}/sessions/list`, {
        method: 'GET',
        params,
    });
}
export async function forceLogoutAdminSellerAccountSessions(sellerId, sellerAccountId) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/sessions`, {
        method: 'DELETE',
    });
}
export async function getAdminSellerAccountSessions(sellerId, sellerAccountId, params) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/sessions/list`, {
        method: 'GET',
        params,
    });
}
export async function createAdminSellerDirectLogin(sellerId, reason) {
    return request(`/api/seller/admin/sellers/${sellerId}/directLogin`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { reason },
    });
}
export async function createAdminSellerAccountDirectLogin(sellerId, sellerAccountId, reason) {
    return request(`/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/directLogin`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { reason },
    });
}
export async function getAdminSellerLoginLogs(params) {
    return request('/api/seller/admin/sellers/loginLogs/list', {
        method: 'GET',
        params,
    });
}
export async function getAdminSellerOperLogs(params) {
    return request('/api/seller/admin/sellers/operLogs/list', {
        method: 'GET',
        params,
    });
}
export async function getAdminSellerDirectLoginTickets(params) {
    return request('/api/seller/admin/sellers/directLoginTickets/list', {
        method: 'GET',
        params,
    });
}
