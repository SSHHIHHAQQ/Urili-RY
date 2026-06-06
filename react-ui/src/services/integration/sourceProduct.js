import { request } from '@umijs/max';
const baseUrl = '/api/integration/admin/source-products';
export async function getSourceProductList(params) {
    return request(`${baseUrl}/list`, {
        method: 'GET',
        params,
    });
}
