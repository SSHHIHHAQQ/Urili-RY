import { request } from '@umijs/max';
import { downLoadXlsx } from '@/utils/downloadfile';
// 查询字典数据列表
export async function getDictDataList(params, options) {
    return request('/api/system/dict/data/list', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        params,
        ...(options || {}),
    });
}
// 查询字典数据详细
export function getDictData(dictCode, options) {
    return request(`/api/system/dict/data/${dictCode}`, {
        method: 'GET',
        ...(options || {}),
    });
}
// 新增字典数据
export async function addDictData(params, options) {
    return request('/api/system/dict/data', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params,
        ...(options || {}),
    });
}
// 修改字典数据
export async function updateDictData(params, options) {
    return request('/api/system/dict/data', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params,
        ...(options || {}),
    });
}
// 删除字典数据
export async function removeDictData(ids, options) {
    return request(`/api/system/dict/data/${ids}`, {
        method: 'DELETE',
        ...(options || {}),
    });
}
// 导出字典数据
export function exportDictData(params, options) {
    return downLoadXlsx(`/api/system/dict/data/export`, { params }, `dict_data_${new Date().getTime()}.xlsx`);
}
