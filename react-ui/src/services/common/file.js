import { request } from '@umijs/max';
export async function uploadCommonFile(file) {
    const formData = new FormData();
    formData.append('file', file);
    return request('/api/common/upload', {
        method: 'POST',
        data: formData,
        requestType: 'form',
    });
}
