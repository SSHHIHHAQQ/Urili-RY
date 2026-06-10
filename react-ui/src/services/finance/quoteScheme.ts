import { request } from '@umijs/max';

const baseUrl = '/api/finance/admin/quote-schemes';

export async function getQuoteSchemeList(params?: Record<string, any>) {
  return request<API.Finance.QuoteSchemePageResult>(`${baseUrl}/list`, {
    method: 'GET',
    params,
  });
}

export async function getQuoteScheme(schemeId: number) {
  return request<API.Finance.QuoteSchemeResult>(`${baseUrl}/${schemeId}`, {
    method: 'GET',
  });
}

export async function addQuoteScheme(data: API.Finance.QuoteScheme) {
  return request<API.Finance.QuoteSchemeResult>(baseUrl, {
    method: 'POST',
    data,
  });
}

export async function updateQuoteScheme(
  schemeId: number,
  data: API.Finance.QuoteScheme,
) {
  return request<API.Result>(`${baseUrl}/${schemeId}`, {
    method: 'PUT',
    data,
  });
}

export async function updateQuoteSchemeStatus(schemeId: number, status: string) {
  return request<API.Result>(`${baseUrl}/${schemeId}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function getQuoteSchemeWarehouses(schemeId: number) {
  return request<API.Finance.QuoteSchemeWarehouseListResult>(
    `${baseUrl}/${schemeId}/warehouses`,
    { method: 'GET' },
  );
}

export async function saveQuoteSchemeWarehouses(
  schemeId: number,
  warehouseCodes: string[],
) {
  return request<API.Result>(`${baseUrl}/${schemeId}/warehouses`, {
    method: 'PUT',
    data: { warehouseCodes },
  });
}

export async function getQuoteSchemeChannels(schemeId: number) {
  return request<API.Finance.QuoteSchemeChannelListResult>(
    `${baseUrl}/${schemeId}/channels/list`,
    { method: 'GET' },
  );
}

export async function addQuoteSchemeChannel(
  schemeId: number,
  data: API.Finance.QuoteSchemeChannel,
) {
  return request<API.Result>(`${baseUrl}/${schemeId}/channels`, {
    method: 'POST',
    data,
  });
}

export async function updateQuoteSchemeChannel(
  schemeId: number,
  schemeChannelId: number,
  data: API.Finance.QuoteSchemeChannel,
) {
  return request<API.Result>(
    `${baseUrl}/${schemeId}/channels/${schemeChannelId}`,
    { method: 'PUT', data },
  );
}

export async function deleteQuoteSchemeChannel(
  schemeId: number,
  schemeChannelId: number,
) {
  return request<API.Result>(
    `${baseUrl}/${schemeId}/channels/${schemeChannelId}`,
    { method: 'DELETE' },
  );
}

export async function getQuoteSchemeValueFees(schemeId: number) {
  return request<API.Finance.QuoteSchemeValueFeeListResult>(
    `${baseUrl}/${schemeId}/value-fees/list`,
    { method: 'GET' },
  );
}

export async function addQuoteSchemeValueFee(
  schemeId: number,
  data: API.Finance.QuoteSchemeValueFeeRule,
) {
  return request<API.Result>(`${baseUrl}/${schemeId}/value-fees`, {
    method: 'POST',
    data,
  });
}

export async function updateQuoteSchemeValueFee(
  schemeId: number,
  valueFeeRuleId: number,
  data: API.Finance.QuoteSchemeValueFeeRule,
) {
  return request<API.Result>(
    `${baseUrl}/${schemeId}/value-fees/${valueFeeRuleId}`,
    { method: 'PUT', data },
  );
}

export async function deleteQuoteSchemeValueFee(
  schemeId: number,
  valueFeeRuleId: number,
) {
  return request<API.Result>(
    `${baseUrl}/${schemeId}/value-fees/${valueFeeRuleId}`,
    { method: 'DELETE' },
  );
}

export async function getQuoteSchemeBuyerOptions(keyword?: string) {
  return request<API.Finance.QuoteSchemeOptionResult>(
    `${baseUrl}/options/buyers`,
    { method: 'GET', params: { keyword } },
  );
}

export async function getQuoteSchemeWarehouseOptions(keyword?: string) {
  return request<API.Finance.QuoteSchemeOptionResult>(
    `${baseUrl}/options/warehouses`,
    { method: 'GET', params: { keyword } },
  );
}

export async function getQuoteSchemeCustomerChannelOptions(keyword?: string) {
  return request<API.Finance.QuoteSchemeOptionResult>(
    `${baseUrl}/options/customer-channels`,
    { method: 'GET', params: { keyword } },
  );
}

export async function getQuoteSchemeSystemChannelOptions(keyword?: string) {
  return request<API.Finance.QuoteSchemeOptionResult>(
    `${baseUrl}/options/system-channels`,
    { method: 'GET', params: { keyword } },
  );
}

export async function getQuoteSchemeFeePlaceholderOptions(feeType?: string) {
  return request<API.Finance.QuoteSchemeOptionResult>(
    `${baseUrl}/options/fee-placeholders`,
    { method: 'GET', params: { feeType } },
  );
}
