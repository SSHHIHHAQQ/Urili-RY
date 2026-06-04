import type { ProTableProps } from '@ant-design/pro-components';

type ProTableSearchConfig = Exclude<ProTableProps<Record<string, unknown>, Record<string, unknown>>['search'], false | undefined>;

const SEARCH_COLLAPSED_STORAGE_PREFIX = 'proTableSearch:collapsed:';

const RESPONSIVE_VERTICAL_SEARCH_SPAN: NonNullable<ProTableSearchConfig['span']> = {
  xs: 24,
  sm: 12,
  md: 12,
  lg: 8,
  xl: 6,
  xxl: 4,
};
const DEFAULT_PRO_TABLE_SEARCH_LAYOUT: Pick<
  ProTableSearchConfig,
  'defaultFormItemsNumber' | 'labelWidth' | 'layout' | 'searchGutter' | 'span'
> = {
  defaultFormItemsNumber: 6,
  labelWidth: 'auto',
  layout: 'vertical',
  searchGutter: [24, 16],
  span: RESPONSIVE_VERTICAL_SEARCH_SPAN,
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
  const layout = config.layout ?? DEFAULT_PRO_TABLE_SEARCH_LAYOUT.layout;

  return {
    ...config,
    defaultFormItemsNumber: config.defaultFormItemsNumber ?? DEFAULT_PRO_TABLE_SEARCH_LAYOUT.defaultFormItemsNumber,
    labelWidth:
      layout === 'horizontal'
        ? config.labelWidth
        : DEFAULT_PRO_TABLE_SEARCH_LAYOUT.labelWidth,
    layout,
    searchGutter: config.searchGutter ?? DEFAULT_PRO_TABLE_SEARCH_LAYOUT.searchGutter,
    span: config.span ?? DEFAULT_PRO_TABLE_SEARCH_LAYOUT.span,
    defaultCollapsed: getPersistedCollapsed(key),
    onCollapse: (collapsed) => {
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(key, String(collapsed));
      }
      onCollapse?.(collapsed);
    },
  };
}
