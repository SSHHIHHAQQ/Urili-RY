import type { Key } from 'react';

export const categoryStatusOptions = [
  { label: '正常类目', value: '0' },
  { label: '停用类目', value: '1' },
  { label: '全部状态', value: 'ALL' },
];

export const sourceScopeValueEnum = {
  CURRENT: { text: '本类目' },
  INHERITED: { text: '继承上级' },
};

function normalizeSearchText(value?: string | number) {
  return String(value ?? '').trim().toLowerCase();
}

function isLeafCategory(category: API.Product.Category) {
  return !category.children?.length;
}

export function collectCategoryKeys(categories: API.Product.Category[]): Key[] {
  return categories
    .flatMap((item) => [
      item.categoryId as Key,
      ...collectCategoryKeys(item.children || []),
    ])
    .filter((key) => key !== undefined);
}

export function findCategoryInTree(
  categories: API.Product.Category[],
  categoryId?: number,
): boolean {
  if (!categoryId) {
    return false;
  }
  return categories.some(
    (item) =>
      item.categoryId === categoryId ||
      findCategoryInTree(item.children || [], categoryId),
  );
}

export function filterCategoryTree(
  categories: API.Product.Category[],
  filters: {
    keyword: string;
    status: string;
    level: string;
    leafOnly: boolean;
  },
  parentPath: string[] = [],
): API.Product.Category[] {
  const keyword = normalizeSearchText(filters.keyword);
  return categories
    .map((category) => {
      const path = [...parentPath, category.categoryName || ''];
      const children = filterCategoryTree(category.children || [], filters, path);
      const searchText = normalizeSearchText(
        [category.categoryName, category.categoryCode, path.join('/')].join(' '),
      );
      const statusMatched =
        filters.status === 'ALL' || category.status === filters.status;
      const levelMatched =
        filters.level === 'ALL' ||
        String(category.categoryLevel || '') === filters.level;
      const leafMatched = !filters.leafOnly || isLeafCategory(category);
      const keywordMatched = !keyword || searchText.includes(keyword);
      const selfMatched =
        statusMatched && levelMatched && leafMatched && keywordMatched;
      if (!selfMatched && children.length === 0) {
        return undefined;
      }
      return {
        ...category,
        children,
      };
    })
    .filter(Boolean) as API.Product.Category[];
}

export function collectCategoryOptions(
  categories: API.Product.Category[],
  parentPath: string[] = [],
): { label: string; value: number }[] {
  return categories.flatMap((category) => {
    const path = [...parentPath, category.categoryName || ''];
    const current = category.categoryId
      ? [{ label: path.join(' / '), value: category.categoryId }]
      : [];
    return [...current, ...collectCategoryOptions(category.children || [], path)];
  });
}

export function filterCategoryAttributeRows(
  rows: API.Product.CategoryAttribute[],
  params: Record<string, any>,
  selectedCategoryId?: number,
) {
  const keyword = normalizeSearchText(params.keyword);
  return rows.filter((row) => {
    const keywordMatched =
      !keyword ||
      normalizeSearchText(`${row.attributeName || ''} ${row.attributeCode || ''}`).includes(keyword);
    const sourceScopeMatched =
      !params.sourceScope ||
      (params.sourceScope === 'CURRENT' && row.categoryId === selectedCategoryId) ||
      (params.sourceScope === 'INHERITED' && row.categoryId !== selectedCategoryId);
    const sourceCategoryMatched =
      !params.sourceCategoryId || row.categoryId === Number(params.sourceCategoryId);
    const attributeTypeMatched =
      !params.attributeType || row.attributeType === params.attributeType;
    const groupMatched = !params.groupCode || row.groupCode === params.groupCode;
    const requiredMatched =
      !params.requiredFlag || row.requiredFlag === params.requiredFlag;
    const filterableMatched =
      !params.filterableFlag || row.filterableFlag === params.filterableFlag;
    const statusMatched = !params.status || row.status === params.status;

    return (
      keywordMatched &&
      sourceScopeMatched &&
      sourceCategoryMatched &&
      attributeTypeMatched &&
      groupMatched &&
      requiredMatched &&
      filterableMatched &&
      statusMatched
    );
  });
}
