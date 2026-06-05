import type { ProTableProps } from '@ant-design/pro-components';

type ProTableSearchConfig = Exclude<
  ProTableProps<Record<string, unknown>, Record<string, unknown>>['search'],
  false | undefined
>;
type ProTablePaginationConfig = Exclude<
  ProTableProps<Record<string, unknown>, Record<string, unknown>>['pagination'],
  false | undefined
>;
type ProTableScrollConfig = Exclude<
  ProTableProps<Record<string, unknown>, Record<string, unknown>>['scroll'],
  false | undefined
>;
type ProTableColumnsStateConfig = Exclude<
  ProTableProps<Record<string, unknown>, Record<string, unknown>>['columnsState'],
  false | undefined
>;

const SEARCH_COLLAPSED_STORAGE_PREFIX = 'proTableSearch:collapsed:';
const DEFAULT_PRO_TABLE_PAGE_SIZE = 20;
const DEFAULT_PRO_TABLE_PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

const RESPONSIVE_VERTICAL_SEARCH_SPAN: NonNullable<
  ProTableSearchConfig['span']
> = {
  xs: 24,
  sm: 12,
  md: 12,
  lg: 6,
  xl: 4,
  xxl: 3,
};
const DEFAULT_PRO_TABLE_SEARCH_LAYOUT: Pick<
  ProTableSearchConfig,
  'defaultFormItemsNumber' | 'labelWidth' | 'layout' | 'searchGutter' | 'span'
> = {
  defaultFormItemsNumber: 5,
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

function getPersistedCollapsed(storageKey: string, fallback = false) {
  if (typeof window === 'undefined') {
    return fallback;
  }
  const value = window.localStorage.getItem(storageKey);
  return value === null ? fallback : value === 'true';
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
    defaultFormItemsNumber:
      config.defaultFormItemsNumber ??
      DEFAULT_PRO_TABLE_SEARCH_LAYOUT.defaultFormItemsNumber,
    labelWidth:
      layout === 'horizontal'
        ? config.labelWidth
        : DEFAULT_PRO_TABLE_SEARCH_LAYOUT.labelWidth,
    layout,
    searchGutter:
      config.searchGutter ?? DEFAULT_PRO_TABLE_SEARCH_LAYOUT.searchGutter,
    span: config.span ?? DEFAULT_PRO_TABLE_SEARCH_LAYOUT.span,
    defaultCollapsed: getPersistedCollapsed(
      key,
      config.defaultCollapsed ?? false,
    ),
    onCollapse: (collapsed) => {
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(key, String(collapsed));
      }
      onCollapse?.(collapsed);
    },
  };
}

export function getProTablePagination(
  config: ProTablePaginationConfig | number = {},
): ProTablePaginationConfig {
  const normalizedConfig =
    typeof config === 'number' ? { defaultPageSize: config } : config;
  const { pageSize, ...restConfig } =
    normalizedConfig as ProTablePaginationConfig & {
      pageSize?: number;
    };

  return {
    ...restConfig,
    defaultPageSize:
      restConfig.defaultPageSize ?? pageSize ?? DEFAULT_PRO_TABLE_PAGE_SIZE,
    showSizeChanger: restConfig.showSizeChanger ?? true,
    pageSizeOptions:
      restConfig.pageSizeOptions ?? DEFAULT_PRO_TABLE_PAGE_SIZE_OPTIONS,
  };
}

export function getProTableScroll(
  x: ProTableScrollConfig['x'],
  config: Omit<ProTableScrollConfig, 'x'> = {},
): ProTableScrollConfig {
  return {
    ...config,
    x,
    y: config.y ?? '100%',
  };
}

export function getProTableColumnsState(
  persistenceKey: string,
  config: ProTableColumnsStateConfig = {},
): ProTableColumnsStateConfig {
  return {
    persistenceType: 'localStorage',
    ...config,
    persistenceKey,
  };
}
