import { request } from '@umijs/max';

const baseUrl = '/api/finance/admin';

export async function getCurrencyList(params?: Record<string, any>) {
  return request<API.Finance.CurrencyPageResult>(`${baseUrl}/currencies/list`, {
    method: 'GET',
    params,
  });
}

export async function getCurrency(currencyCode: string) {
  return request<API.Result & { data: API.Finance.Currency }>(
    `${baseUrl}/currencies/${currencyCode}`,
    { method: 'GET' },
  );
}

export async function addCurrency(data: API.Finance.Currency) {
  return request<API.Result>(`${baseUrl}/currencies`, {
    method: 'POST',
    data,
  });
}

export async function updateCurrency(
  currencyCode: string,
  data: API.Finance.Currency,
) {
  return request<API.Result>(`${baseUrl}/currencies/${currencyCode}`, {
    method: 'PUT',
    data,
  });
}

export async function updateCurrencyStatus(currencyCode: string, status: string) {
  return request<API.Result>(`${baseUrl}/currencies/${currencyCode}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function deleteCurrency(currencyCode: string) {
  return request<API.Result>(`${baseUrl}/currencies/${currencyCode}`, {
    method: 'DELETE',
  });
}

export async function getCurrencyOptions() {
  return request<API.Finance.CurrencyOptionResult>(
    `${baseUrl}/currencies/options`,
    { method: 'GET' },
  );
}

export async function getRateHistoryList(
  currencyCode: string,
  params?: Record<string, any>,
) {
  return request<API.Finance.RateHistoryPageResult>(
    `${baseUrl}/currencies/${currencyCode}/rate-history/list`,
    { method: 'GET', params },
  );
}

export async function getSyncConfig() {
  return request<API.Finance.SyncConfigResult>(
    `${baseUrl}/currency-sync-config`,
    { method: 'GET' },
  );
}

export async function saveSyncConfig(data: API.Finance.SyncConfig) {
  return request<API.Result>(`${baseUrl}/currency-sync-config`, {
    method: 'PUT',
    data,
  });
}

export async function testSyncConfig(data: API.Finance.SyncConfig) {
  return request<API.Finance.SyncResultResponse>(
    `${baseUrl}/currency-sync-config/test`,
    { method: 'POST', data },
  );
}

export async function syncRates() {
  return request<API.Finance.SyncResultResponse>(
    `${baseUrl}/currency-sync-config/sync`,
    { method: 'POST' },
  );
}

export async function getSyncLogList(params?: Record<string, any>) {
  return request<API.Finance.SyncLogPageResult>(
    `${baseUrl}/currency-sync-config/logs/list`,
    { method: 'GET', params },
  );
}
