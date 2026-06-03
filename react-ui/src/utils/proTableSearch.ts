import type { ProTableProps } from '@ant-design/pro-components';

type ProTableSearchConfig = Exclude<ProTableProps<Record<string, unknown>, Record<string, unknown>>['search'], false | undefined>;

const SEARCH_COLLAPSED_STORAGE_PREFIX = 'proTableSearch:collapsed:';

function getSearchStorageKey(storageKey?: string) {
  if (storageKey) {
    return `${SEARCH_COLLAPSED_STORAGE_PREFIX}${storageKey}`;
  }
  if (typeof window === 'undefined') {
    return `${SEARCH_COLLAPSED_STORAGE_PREFIX}default`;
  }
  return `${SEARCH_COLLAPSED_STORAGE_PREFIX}${window.location.pathname}`;
}

function getPersistedCollapsed(storageKey: string) {
  if (typeof window === 'undefined') {
    return false;
  }
  const value = window.localStorage.getItem(storageKey);
  return value === null ? false : value === 'true';
}

export function getPersistedProTableSearch(
  config: ProTableSearchConfig = {},
  storageKey?: string,
): ProTableSearchConfig {
  const key = getSearchStorageKey(storageKey);
  const onCollapse = config.onCollapse;

  return {
    ...config,
    defaultCollapsed: getPersistedCollapsed(key),
    onCollapse: (collapsed) => {
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(key, String(collapsed));
      }
      onCollapse?.(collapsed);
    },
  };
}
