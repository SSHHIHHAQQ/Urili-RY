import { request } from '@umijs/max';
export async function getAdminBuyerList(params) {
    return request('/api/buyer/admin/buyers/list', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        params,
    });
}
export async function getAdminBuyer(buyerId) {
    return request(`/api/buyer/admin/buyers/${buyerId}`, {
        method: 'GET',
    });
}
export async function addAdminBuyer(data) {
    return request('/api/buyer/admin/buyers', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminBuyer(data) {
    return request('/api/buyer/admin/buyers', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function changeAdminBuyerStatus(data) {
    return request('/api/buyer/admin/buyers/changeStatus', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function getAdminBuyerAccounts(buyerId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts`, {
        method: 'GET',
    });
}
export async function addAdminBuyerAccount(buyerId, data) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminBuyerAccount(buyerId, data) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function lockAdminBuyerAccount(buyerId, buyerAccountId, lockReason) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/lock`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { lockReason },
    });
}
export async function unlockAdminBuyerAccount(buyerId, buyerAccountId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/unlock`, {
        method: 'PUT',
    });
}
export async function getAdminBuyerAccountRoles(buyerId, buyerAccountId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/roles`, {
        method: 'GET',
    });
}
export async function assignAdminBuyerAccountRoles(buyerId, buyerAccountId, roleIds) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/roles`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { roleIds },
    });
}
export async function getAdminBuyerMenuTree() {
    return request('/api/buyer/admin/menus/treeselect', {
        method: 'GET',
    });
}
export async function getAdminBuyerMenus(params) {
    return request('/api/buyer/admin/menus/list', {
        method: 'GET',
        params,
    });
}
export async function getAdminBuyerMenu(menuId) {
    return request(`/api/buyer/admin/menus/${menuId}`, {
        method: 'GET',
    });
}
export async function addAdminBuyerMenu(data) {
    return request('/api/buyer/admin/menus', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminBuyerMenu(data) {
    return request('/api/buyer/admin/menus', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function removeAdminBuyerMenu(menuId) {
    return request(`/api/buyer/admin/menus/${menuId}`, {
        method: 'DELETE',
    });
}
export async function getAdminBuyerRoleMenuTree(buyerId, roleId) {
    return request(`/api/buyer/admin/menus/roleMenuTreeselect/${buyerId}/${roleId}`, {
        method: 'GET',
    });
}
export async function getAdminBuyerRoles(buyerId, params) {
    return request(`/api/buyer/admin/buyers/${buyerId}/roles/list`, {
        method: 'GET',
        params,
    });
}
export async function getAdminBuyerRole(buyerId, roleId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/roles/${roleId}`, {
        method: 'GET',
    });
}
export async function addAdminBuyerRole(buyerId, data) {
    return request(`/api/buyer/admin/buyers/${buyerId}/roles`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminBuyerRole(buyerId, data) {
    return request(`/api/buyer/admin/buyers/${buyerId}/roles`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function changeAdminBuyerRoleStatus(buyerId, data) {
    return request(`/api/buyer/admin/buyers/${buyerId}/roles/changeStatus`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function removeAdminBuyerRoles(buyerId, roleIds) {
    return request(`/api/buyer/admin/buyers/${buyerId}/roles/${roleIds.join(',')}`, {
        method: 'DELETE',
    });
}
export async function getAdminBuyerDepts(buyerId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/depts/list`, {
        method: 'GET',
    });
}
export async function getAdminBuyerDept(buyerId, deptId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/depts/${deptId}`, {
        method: 'GET',
    });
}
export async function getAdminBuyerDeptTree(buyerId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/depts/treeselect`, {
        method: 'GET',
    });
}
export async function addAdminBuyerDept(buyerId, data) {
    return request(`/api/buyer/admin/buyers/${buyerId}/depts`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function updateAdminBuyerDept(buyerId, data) {
    return request(`/api/buyer/admin/buyers/${buyerId}/depts`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data,
    });
}
export async function removeAdminBuyerDept(buyerId, deptId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/depts/${deptId}`, {
        method: 'DELETE',
    });
}
export async function resetAdminBuyerAccountPassword(buyerId, buyerAccountId, password) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/resetPwd`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { password },
    });
}
export async function forceLogoutAdminBuyerSessions(buyerId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/sessions`, {
        method: 'DELETE',
    });
}
export async function getAdminBuyerSessions(buyerId, params) {
    return request(`/api/buyer/admin/buyers/${buyerId}/sessions/list`, {
        method: 'GET',
        params,
    });
}
export async function forceLogoutAdminBuyerAccountSessions(buyerId, buyerAccountId) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/sessions`, {
        method: 'DELETE',
    });
}
export async function getAdminBuyerAccountSessions(buyerId, buyerAccountId, params) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/sessions/list`, {
        method: 'GET',
        params,
    });
}
export async function createAdminBuyerDirectLogin(buyerId, reason) {
    return request(`/api/buyer/admin/buyers/${buyerId}/directLogin`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { reason },
    });
}
export async function createAdminBuyerAccountDirectLogin(buyerId, buyerAccountId, reason) {
    return request(`/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/directLogin`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: { reason },
    });
}
export async function getAdminBuyerLoginLogs(params) {
    return request('/api/buyer/admin/buyers/loginLogs/list', {
        method: 'GET',
        params,
    });
}
export async function getAdminBuyerOperLogs(params) {
    return request('/api/buyer/admin/buyers/operLogs/list', {
        method: 'GET',
        params,
    });
}
export async function getAdminBuyerDirectLoginTickets(params) {
    return request('/api/buyer/admin/buyers/directLoginTickets/list', {
        method: 'GET',
        params,
    });
}
