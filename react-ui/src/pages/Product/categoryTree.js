export function buildCategoryTree(list) {
    const map = new Map();
    const roots = [];
    list.forEach((item) => {
        if (item.categoryId) {
            const { children: _children, ...category } = item;
            map.set(item.categoryId, { ...category });
        }
    });
    map.forEach((item) => {
        const parent = item.parentId ? map.get(item.parentId) : undefined;
        if (item.parentId && item.parentId !== 0 && parent) {
            parent.children = [...(parent.children || []), item];
            return;
        }
        roots.push(item);
    });
    return roots;
}
export function toCategoryTreeSelectData(categories) {
    return categories.map((item) => {
        const children = item.children?.length
            ? toCategoryTreeSelectData(item.children)
            : undefined;
        return {
            title: item.categoryName,
            value: item.categoryId,
            ...(children ? { children } : {}),
        };
    });
}
export function toCategoryTreeData(categories) {
    return categories.map((item) => {
        const children = item.children?.length
            ? toCategoryTreeData(item.children)
            : undefined;
        return {
            title: item.categoryName,
            key: item.categoryId,
            ...(children ? { children } : {}),
        };
    });
}
export function getCategoryDisplayPath(category) {
    return category.fullPath || category.categoryName || category.categoryCode || '-';
}
export function toCategoryOption(category) {
    return {
        label: `${getCategoryDisplayPath(category)}${category.categoryCode ? ` (${category.categoryCode})` : ''}`,
        value: category.categoryId || 0,
    };
}
export function normalizeLazyCategoryRows(categories) {
    return categories.map((item) => {
        const hasChildren = Number(item.childrenCount || 0) > 0;
        return {
            ...item,
            children: item.children?.length
                ? normalizeLazyCategoryRows(item.children)
                : hasChildren
                    ? []
                    : undefined,
        };
    });
}
export function mergeCategoryChildren(categories, parentId, children) {
    if (parentId === 0) {
        return normalizeLazyCategoryRows(children);
    }
    return categories.map((item) => {
        if (item.categoryId === parentId) {
            return {
                ...item,
                children: normalizeLazyCategoryRows(children),
            };
        }
        if (item.children?.length) {
            return {
                ...item,
                children: mergeCategoryChildren(item.children, parentId, children),
            };
        }
        return item;
    });
}
export function toLazyCategoryTreeData(categories, useFullPath = false, disableExpand = false) {
    return categories.map((item) => {
        const children = !disableExpand && item.children?.length
            ? toLazyCategoryTreeData(item.children, useFullPath, disableExpand)
            : undefined;
        const hasChildren = Number(item.childrenCount || 0) > 0;
        return {
            title: useFullPath
                ? getCategoryDisplayPath(item)
                : item.categoryName || item.categoryCode || '-',
            key: item.categoryId,
            isLeaf: disableExpand || !hasChildren,
            ...(children ? { children } : {}),
        };
    });
}
