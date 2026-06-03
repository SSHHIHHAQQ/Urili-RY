import { request } from '@umijs/max';

const encodePath = (value: string) => encodeURIComponent(value);

// 查询缓存名称列表
export function listCacheName() {
  return request<API.Monitor.CacheNamesResponse>('/api/monitor/cache/getNames', {
    method: 'get',
  });
}

// 查询缓存键名列表
export function listCacheKey(cacheName: string) {
  return request<API.Monitor.CacheKeysResponse>(
    `/api/monitor/cache/getKeys/${encodePath(cacheName)}`,
    {
      method: 'get',
    },
  );
}

// 查询缓存内容
export function getCacheValue(cacheName: string, cacheKey: string) {
  return request<API.Monitor.CacheValueResponse>(
    `/api/monitor/cache/getValue/${encodePath(cacheName)}/${encodePath(cacheKey)}`,
    {
      method: 'get',
    },
  );
}

// 清理指定名称缓存
export function clearCacheName(cacheName: string) {
  return request<API.Result>(`/api/monitor/cache/clearCacheName/${encodePath(cacheName)}`, {
    method: 'delete',
  });
}

// 清理指定键名缓存
export function clearCacheKey(cacheKey: string) {
  return request<API.Result>(`/api/monitor/cache/clearCacheKey/${encodePath(cacheKey)}`, {
    method: 'delete',
  });
}

// 清理全部缓存
export function clearCacheAll() {
  return request<API.Result>('/api/monitor/cache/clearCacheAll', {
    method: 'delete',
  });
}
