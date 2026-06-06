import { request } from '@umijs/max';
const baseUrl = '/api/warehouse';
function withRuoYiPage(params) {
    if (!params) {
        return params;
    }
    const { current, ...rest } = params;
    return {
        ...rest,
        pageNum: params.pageNum || current,
    };
}
export async function getOfficialWarehouseList(params) {
    return request(`${baseUrl}/official/list`, {
        method: 'GET',
        params: withRuoYiPage(params),
    });
}
export async function getOfficialWarehouse(warehouseId) {
    return request(`${baseUrl}/official/${warehouseId}`, {
        method: 'GET',
    });
}
export async function addOfficialWarehouse(data) {
    return request(`${baseUrl}/official`, {
        method: 'POST',
        data,
    });
}
export async function updateOfficialWarehouse(data) {
    return request(`${baseUrl}/official`, {
        method: 'PUT',
        data,
    });
}
export async function updateOfficialWarehouseStatus(data) {
    return request(`${baseUrl}/official/status`, {
        method: 'PUT',
        data,
    });
}
export async function getThirdPartyWarehouseList(params) {
    return request(`${baseUrl}/third-party/list`, {
        method: 'GET',
        params: withRuoYiPage(params),
    });
}
export async function getThirdPartyWarehouse(warehouseId) {
    return request(`${baseUrl}/third-party/${warehouseId}`, {
        method: 'GET',
    });
}
export async function addThirdPartyWarehouse(data) {
    return request(`${baseUrl}/third-party`, {
        method: 'POST',
        data,
    });
}
export async function updateThirdPartyWarehouse(data) {
    return request(`${baseUrl}/third-party`, {
        method: 'PUT',
        data,
    });
}
export async function updateThirdPartyWarehouseStatus(data) {
    return request(`${baseUrl}/third-party/status`, {
        method: 'PUT',
        data,
    });
}
export async function getWarehouseCurrencyOptions() {
    return request(`${baseUrl}/options/currencies`, {
        method: 'GET',
    });
}
export async function getWarehouseSellerOptions(keyword) {
    return request(`${baseUrl}/options/sellers`, {
        method: 'GET',
        params: { keyword },
    });
}
export async function getUsStateOptions(keyword) {
    return request(`${baseUrl}/options/us-states`, {
        method: 'GET',
        params: { keyword },
    });
}
export async function getUsCityOptions(params) {
    return request(`${baseUrl}/options/us-cities`, {
        method: 'GET',
        params,
    });
}
export async function getOfficialSyncConnections(keyword) {
    return request(`${baseUrl}/official/sync-connections`, {
        method: 'GET',
        params: { keyword },
    });
}
export async function getOfficialSyncCandidates(params) {
    return request(`${baseUrl}/official/sync-candidates`, {
        method: 'GET',
        params,
    });
}
export async function syncOfficialWarehouse(data) {
    return request(`${baseUrl}/official/sync`, {
        method: 'POST',
        data,
    });
}
