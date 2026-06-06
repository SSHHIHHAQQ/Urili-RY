import { request } from '@umijs/max';
const baseUrl = '/api/finance/admin';
export async function getCurrencyList(params) {
    return request(`${baseUrl}/currencies/list`, {
        method: 'GET',
        params,
    });
}
export async function getCurrency(currencyCode) {
    return request(`${baseUrl}/currencies/${currencyCode}`, { method: 'GET' });
}
export async function addCurrency(data) {
    return request(`${baseUrl}/currencies`, {
        method: 'POST',
        data,
    });
}
export async function updateCurrency(currencyCode, data) {
    return request(`${baseUrl}/currencies/${currencyCode}`, {
        method: 'PUT',
        data,
    });
}
export async function updateCurrencyStatus(currencyCode, status) {
    return request(`${baseUrl}/currencies/${currencyCode}/status`, {
        method: 'PUT',
        data: { status },
    });
}
export async function deleteCurrency(currencyCode) {
    return request(`${baseUrl}/currencies/${currencyCode}`, {
        method: 'DELETE',
    });
}
export async function getCurrencyOptions() {
    return request(`${baseUrl}/currencies/options`, { method: 'GET' });
}
export async function getRateHistoryList(currencyCode, params) {
    return request(`${baseUrl}/currencies/${currencyCode}/rate-history/list`, { method: 'GET', params });
}
export async function getSyncConfig() {
    return request(`${baseUrl}/currency-sync-config`, { method: 'GET' });
}
export async function saveSyncConfig(data) {
    return request(`${baseUrl}/currency-sync-config`, {
        method: 'PUT',
        data,
    });
}
export async function testSyncConfig(data) {
    return request(`${baseUrl}/currency-sync-config/test`, { method: 'POST', data });
}
export async function syncRates() {
    return request(`${baseUrl}/currency-sync-config/sync`, { method: 'POST' });
}
export async function getSyncLogList(params) {
    return request(`${baseUrl}/currency-sync-config/logs/list`, { method: 'GET', params });
}
