const SEARCH_COLLAPSED_STORAGE_PREFIX = 'proTableSearch:collapsed:';
const DEFAULT_PRO_TABLE_PAGE_SIZE = 20;
const DEFAULT_PRO_TABLE_PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const RESPONSIVE_VERTICAL_SEARCH_SPAN = {
    xs: 24,
    sm: 12,
    md: 12,
    lg: 6,
    xl: 4,
    xxl: 3,
};
const SEARCH_SPAN_BY_COLUMN_COUNT = {
    1: 24,
    2: 12,
    3: 8,
    4: 6,
    6: 4,
    8: 3,
};
const ACTION_SAFE_SEARCH_COLUMN_CANDIDATES = {
    xs: [1],
    sm: [2],
    md: [2, 3],
    lg: [4, 3, 2],
    xl: [6, 4, 3],
    xxl: [8, 6, 4],
};
const DEFAULT_PRO_TABLE_SEARCH_LAYOUT = {
    defaultFormItemsNumber: 5,
    labelWidth: 'auto',
    layout: 'vertical',
    searchGutter: [24, 16],
    span: RESPONSIVE_VERTICAL_SEARCH_SPAN,
};
function getSearchStorageKey(storageKey) {
    if (storageKey) {
        return `${SEARCH_COLLAPSED_STORAGE_PREFIX}${storageKey}`;
    }
    if (typeof window === 'undefined') {
        return `${SEARCH_COLLAPSED_STORAGE_PREFIX}default`;
    }
    return `${SEARCH_COLLAPSED_STORAGE_PREFIX}${window.location.pathname}`;
}
function getPersistedCollapsed(storageKey, fallback = false) {
    if (typeof window === 'undefined') {
        return fallback;
    }
    const value = window.localStorage.getItem(storageKey);
    return value === null ? fallback : value === 'true';
}
function pickActionSafeColumnCount(fieldCount, candidates) {
    const normalizedFieldCount = Number.isFinite(fieldCount) && fieldCount > 0
        ? Math.floor(fieldCount)
        : 0;
    if (normalizedFieldCount === 0) {
        return candidates[0];
    }
    return (candidates.find((columnCount) => normalizedFieldCount % columnCount !== 0) ??
        candidates[0]);
}
export function getActionSafeProTableSearchSpan(fieldCount) {
    return Object.fromEntries(Object.entries(ACTION_SAFE_SEARCH_COLUMN_CANDIDATES).map(([breakpoint, candidates]) => {
        const columnCount = pickActionSafeColumnCount(fieldCount, candidates);
        return [breakpoint, SEARCH_SPAN_BY_COLUMN_COUNT[columnCount]];
    }));
}
export function getPersistedProTableSearch(config = {}, storageKey) {
    const { fieldCount, ...proTableConfig } = config;
    const key = getSearchStorageKey(storageKey);
    const onCollapse = proTableConfig.onCollapse;
    const layout = proTableConfig.layout ?? DEFAULT_PRO_TABLE_SEARCH_LAYOUT.layout;
    return {
        ...proTableConfig,
        defaultFormItemsNumber: proTableConfig.defaultFormItemsNumber ??
            DEFAULT_PRO_TABLE_SEARCH_LAYOUT.defaultFormItemsNumber,
        labelWidth: layout === 'horizontal'
            ? proTableConfig.labelWidth
            : DEFAULT_PRO_TABLE_SEARCH_LAYOUT.labelWidth,
        layout,
        searchGutter: proTableConfig.searchGutter ?? DEFAULT_PRO_TABLE_SEARCH_LAYOUT.searchGutter,
        span: proTableConfig.span ??
            (fieldCount
                ? getActionSafeProTableSearchSpan(fieldCount)
                : DEFAULT_PRO_TABLE_SEARCH_LAYOUT.span),
        defaultCollapsed: getPersistedCollapsed(key, proTableConfig.defaultCollapsed ?? false),
        onCollapse: (collapsed) => {
            if (typeof window !== 'undefined') {
                window.localStorage.setItem(key, String(collapsed));
            }
            onCollapse?.(collapsed);
        },
    };
}
export function getProTablePagination(config = {}) {
    const normalizedConfig = typeof config === 'number' ? { defaultPageSize: config } : config;
    const { pageSize, ...restConfig } = normalizedConfig;
    return {
        ...restConfig,
        defaultPageSize: restConfig.defaultPageSize ?? pageSize ?? DEFAULT_PRO_TABLE_PAGE_SIZE,
        showSizeChanger: restConfig.showSizeChanger ?? true,
        pageSizeOptions: restConfig.pageSizeOptions ?? DEFAULT_PRO_TABLE_PAGE_SIZE_OPTIONS,
    };
}
export function getProTableScroll(x, config = {}) {
    return {
        ...config,
        x,
        y: config.y ?? '100%',
    };
}
export function getProTableColumnsState(persistenceKey, config = {}) {
    return {
        persistenceType: 'localStorage',
        ...config,
        persistenceKey,
    };
}
