import { request } from '@umijs/max';
import { downLoadXlsx } from '@/utils/downloadfile';
// 查询岗位信息列表
export async function getPostList(params) {
    return request('/api/system/post/list', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        params
    });
}
// 查询岗位信息详细
export function getPost(postId) {
    return request(`/api/system/post/${postId}`, {
        method: 'GET'
    });
}
// 新增岗位信息
export async function addPost(params) {
    return request('/api/system/post', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params
    });
}
// 修改岗位信息
export async function updatePost(params) {
    return request('/api/system/post', {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json;charset=UTF-8',
        },
        data: params
    });
}
// 删除岗位信息
export async function removePost(ids) {
    return request(`/api/system/post/${ids}`, {
        method: 'DELETE'
    });
}
// 导出岗位信息
export function exportPost(params) {
    return downLoadXlsx(`/api/system/post/export`, { params }, `post_${new Date().getTime()}.xlsx`);
}
