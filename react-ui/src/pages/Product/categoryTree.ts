export function buildCategoryTree(list: API.Product.Category[]) {
  const map = new Map<number, API.Product.Category>();
  const roots: API.Product.Category[] = [];
  list.forEach((item) => {
    if (item.categoryId) {
      map.set(item.categoryId, { ...item, children: [] });
    }
  });
  map.forEach((item) => {
    if (item.parentId && item.parentId !== 0 && map.has(item.parentId)) {
      map.get(item.parentId)?.children?.push(item);
      return;
    }
    roots.push(item);
  });
  return roots;
}

export function toCategoryTreeSelectData(categories: API.Product.Category[]): any[] {
  return categories.map((item) => ({
    title: item.categoryName,
    value: item.categoryId,
    children: toCategoryTreeSelectData(item.children || []),
  }));
}

export function toCategoryTreeData(categories: API.Product.Category[]): any[] {
  return categories.map((item) => ({
    title: item.categoryName,
    key: item.categoryId,
    children: toCategoryTreeData(item.children || []),
  }));
}
