import { request } from '@umijs/max';
// 查询部门列表
export async function getDeptList(params) {
    return request('/api/system/dept/list', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        params
    });
}
// 查询部门列表（排除节点）
export function getDeptListExcludeChild(deptId) {
    return request(`/api/system/dept/list/exclude/${deptId}`, {
        method: 'get',
    });
}
// 查询部门详细
export function getDept(deptId) {
    return request(`/api/system/dept/${deptId}`, {
        method: 'GET'
    });
}
// 新增部门
export async function addDept(params) {
    return request('/api/system/dept', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params
    });
}
// 修改部门
export async function updateDept(params) {
    return request('/api/system/dept', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params
    });
}
// 删除部门
export async function removeDept(ids) {
    return request(`/api/system/dept/${ids}`, {
        method: 'DELETE'
    });
}
