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
