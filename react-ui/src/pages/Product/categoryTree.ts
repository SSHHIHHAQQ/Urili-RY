export function buildCategoryTree(list: API.Product.Category[]) {
  const map = new Map<number, API.Product.Category>();
  const roots: API.Product.Category[] = [];
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

export function toCategoryTreeSelectData(categories: API.Product.Category[]): any[] {
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

export function toCategoryTreeData(categories: API.Product.Category[]): any[] {
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

export function getCategoryDisplayPath(category: API.Product.Category) {
  return category.fullPath || category.categoryName || category.categoryCode || '-';
}

export function toCategoryOption(category: API.Product.Category): { label: string; value: number } {
  return {
    label: `${getCategoryDisplayPath(category)}${
      category.categoryCode ? ` (${category.categoryCode})` : ''
    }`,
    value: category.categoryId || 0,
  };
}

export function normalizeLazyCategoryRows(
  categories: API.Product.Category[],
): API.Product.Category[] {
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

export function mergeCategoryChildren(
  categories: API.Product.Category[],
  parentId: number,
  children: API.Product.Category[],
): API.Product.Category[] {
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

export function toLazyCategoryTreeData(
  categories: API.Product.Category[],
  useFullPath = false,
  disableExpand = false,
): any[] {
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
