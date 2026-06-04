import type { ProTableProps } from '@ant-design/pro-components';

type ProTableSearchConfig = Exclude<ProTableProps<Record<string, unknown>, Record<string, unknown>>['search'], false | undefined>;

const SEARCH_COLLAPSED_STORAGE_PREFIX = 'proTableSearch:collapsed:';
const SIX_FIELD_SEARCH_SPAN: NonNullable<ProTableSearchConfig['span']> = {
  xs: 24,
  sm: 12,
  md: 8,
  lg: 4,
  xl: 4,
  xxl: 4,
};
const DEFAULT_PRO_TABLE_SEARCH_LAYOUT: Pick<ProTableSearchConfig, 'defaultFormItemsNumber' | 'span'> = {
  defaultFormItemsNumber: 6,
  span: SIX_FIELD_SEARCH_SPAN,
};

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
    ...DEFAULT_PRO_TABLE_SEARCH_LAYOUT,
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
