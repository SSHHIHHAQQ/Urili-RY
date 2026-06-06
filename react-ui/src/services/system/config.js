import { request } from '@umijs/max';
import { downLoadXlsx } from '@/utils/downloadfile';
// 查询参数配置列表
export async function getConfigList(params) {
    return request('/api/system/config/list', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        params
    });
}
// 查询参数配置详细
export function getConfig(configId) {
    return request(`/api/system/config/${configId}`, {
        method: 'GET'
    });
}
// 新增参数配置
export async function addConfig(params) {
    return request('/api/system/config', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params
    });
}
// 修改参数配置
export async function updateConfig(params) {
    return request('/api/system/config', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params
    });
}
// 删除参数配置
export async function removeConfig(ids) {
    return request(`/api/system/config/${ids}`, {
        method: 'DELETE'
    });
}
// 导出参数配置
export function exportConfig(params) {
    return downLoadXlsx(`/api/system/config/export`, { params }, `config_${new Date().getTime()}.xlsx`);
}
// 刷新参数缓存
export function refreshConfigCache() {
    return request('/api/system/config/refreshCache', {
        method: 'delete'
    });
}
