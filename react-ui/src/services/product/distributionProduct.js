import { request } from '@umijs/max';
const baseUrl = '/api/product/admin/distribution-products';
export async function getDistributionProductList(params) {
    return request(`${baseUrl}/list`, {
        method: 'GET',
        params,
    });
}
export async function getDistributionSkuList(params) {
    return request(`${baseUrl}/skus/list`, {
        method: 'GET',
        params,
    });
}
export async function getDistributionProduct(spuId) {
    return request(`${baseUrl}/${spuId}`, {
        method: 'GET',
    });
}
export async function addDistributionProduct(data) {
    return request(baseUrl, {
        method: 'POST',
        data,
    });
}
export async function updateDistributionProduct(spuId, data) {
    return request(`${baseUrl}/${spuId}`, {
        method: 'PUT',
        data,
    });
}
export async function updateDistributionProductStatus(spuId, status) {
    return request(`${baseUrl}/${spuId}/status`, {
        method: 'PUT',
        data: { status },
    });
}
export async function updateDistributionSkuStatus(spuId, skuId, status) {
    return request(`${baseUrl}/${spuId}/skus/${skuId}/status`, {
        method: 'PUT',
        data: { status },
    });
}
export async function getDistributionProductSkus(spuId) {
    return request(`${baseUrl}/${spuId}/skus`, { method: 'GET' });
}
export async function batchUpdateDistributionStatus(ownerType, ids, status, syncSkuStatus) {
    return request(`${baseUrl}/status/batch`, {
        method: 'PUT',
        data: {
            ownerType,
            status,
            ...(ownerType === 'SPU' && syncSkuStatus !== undefined ? { syncSkuStatus } : {}),
            ...(ownerType === 'SPU' ? { spuIds: ids } : { skuIds: ids }),
        },
    });
}
export async function batchUpdateDistributionControlStatus(ownerType, ids, controlStatus, reason) {
    return request(`${baseUrl}/control-status/batch`, {
        method: 'PUT',
        data: {
            ownerType,
            controlStatus,
            reason,
            ...(ownerType === 'SPU' ? { spuIds: ids } : { skuIds: ids }),
        },
    });
}
export async function batchUpdateDistributionSkuSalePrices(items, reason) {
    return request(`${baseUrl}/skus/sale-prices`, {
        method: 'PUT',
        data: { items, reason },
    });
}
export async function getDistributionOperationLogList(params) {
    return request(`${baseUrl}/operation-logs/list`, {
        method: 'GET',
        params,
    });
}
