// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';
/** 此处后端没有提供注释 GET /api/notices */
export async function getNotices(options) {
    return request('/api/notices', {
        method: 'GET',
        ...(options || {}),
    });
}
