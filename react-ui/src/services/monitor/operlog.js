import { request } from '@umijs/max';
import { downLoadXlsx } from '@/utils/downloadfile';
// 查询操作日志记录列表
export async function getOperlogList(params) {
    return request('/api/monitor/operlog/list', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        params
    });
}
// 查询操作日志记录详细
export function getOperlog(operId) {
    return request(`/api/monitor/operlog/${operId}`, {
        method: 'GET'
    });
}
// 新增操作日志记录
export async function addOperlog(params) {
    return request('/api/monitor/operlog', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params
    });
}
// 修改操作日志记录
export async function updateOperlog(params) {
    return request('/api/monitor/operlog', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params
    });
}
// 删除操作日志记录
export async function removeOperlog(ids) {
    return request(`/api/monitor/operlog/${ids}`, {
        method: 'DELETE'
    });
}
export async function cleanAllOperlog() {
    return request(`/api/monitor/operlog/clean`, {
        method: 'DELETE'
    });
}
// 导出操作日志记录
export function exportOperlog(params) {
    return downLoadXlsx(`/api/monitor/operlog/export`, { params }, `operlog_${new Date().getTime()}.xlsx`);
}
