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

function normalizeCategoryId(value?: number | string | null) {
  const categoryId = Number(value);
  return Number.isFinite(categoryId) && categoryId > 0 ? categoryId : undefined;
}

function flattenCategories(categories: API.Product.Category[], result: API.Product.Category[] = []) {
  categories.forEach((item) => {
    result.push(item);
    if (item.children?.length) {
      flattenCategories(item.children, result);
    }
  });
  return result;
}

export function findCategoryDisplayPath(
  categories: API.Product.Category[],
  categoryId?: number | string | null,
  fallback?: string,
) {
  const targetId = normalizeCategoryId(categoryId);
  const fallbackText = fallback?.trim();
  if (!targetId) return fallbackText;

  const categoryMap = new Map<number, API.Product.Category>();
  flattenCategories(categories).forEach((item) => {
    const itemId = normalizeCategoryId(item.categoryId);
    if (itemId) {
      categoryMap.set(itemId, item);
    }
  });

  const target = categoryMap.get(targetId);
  if (!target) return fallbackText;
  if (target.fullPath?.trim()) return target.fullPath.trim();

  const ancestorIds = (target.ancestors || '')
    .split(',')
    .map((item) => normalizeCategoryId(item.trim()))
    .filter((item): item is number => !!item && item !== targetId);
  const ancestorNames = ancestorIds
    .map((item) => categoryMap.get(item)?.categoryName?.trim())
    .filter((item): item is string => !!item);
  if (ancestorNames.length) {
    const names = [...ancestorNames, target.categoryName?.trim()].filter((item): item is string => !!item);
    return names.length ? names.join(' / ') : fallbackText;
  }

  const names: string[] = [];
  const visited = new Set<number>();
  let current: API.Product.Category | undefined = target;
  while (current) {
    const currentId = normalizeCategoryId(current.categoryId);
    if (currentId && visited.has(currentId)) break;
    if (currentId) visited.add(currentId);
    const name = current.categoryName?.trim();
    if (name) names.unshift(name);
    const parentId = normalizeCategoryId(current.parentId);
    if (!parentId || !currentId || parentId === currentId) break;
    current = categoryMap.get(parentId);
  }

  return names.length ? names.join(' / ') : fallbackText;
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
