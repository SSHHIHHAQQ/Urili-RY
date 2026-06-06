import { request } from '@umijs/max';
// 查询菜单权限列表
export async function getMenuList(params, options) {
    return request('/api/system/menu/list', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        params,
        ...(options || {}),
    });
}
// 查询菜单权限详细
export function getMenu(menuId, options) {
    return request(`/api/system/menu/${menuId}`, {
        method: 'GET',
        ...(options || {})
    });
}
// 新增菜单权限
export async function addMenu(params, options) {
    return request('/api/system/menu', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params,
        ...(options || {})
    });
}
// 修改菜单权限
export async function updateMenu(params, options) {
    return request('/api/system/menu', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params,
        ...(options || {})
    });
}
// 级联启停菜单权限
export async function cascadeMenuStatus(menuIds, status, options) {
    return request('/api/system/menu/cascadeStatus', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: {
            menuIds,
            status,
        },
        ...(options || {})
    });
}
// 级联显隐菜单权限
export async function cascadeMenuVisible(menuIds, visible, options) {
    return request('/api/system/menu/cascadeVisible', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: {
            menuIds,
            visible,
        },
        ...(options || {})
    });
}
// 删除菜单权限
export async function removeMenu(ids, options) {
    return request(`/api/system/menu/${ids}`, {
        method: 'DELETE',
        ...(options || {})
    });
}
// 查询菜单权限详细
export function getMenuTree() {
    return request('/api/system/menu/treeselect', {
        method: 'GET',
    });
}
