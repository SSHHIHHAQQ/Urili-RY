import { request } from '@umijs/max';

const baseUrl = '/api/product/admin';

export async function getCategoryList(params?: Record<string, any>) {
  return request<API.Product.InfoResult<API.Product.Category[]>>(
    `${baseUrl}/categories/list`,
    { method: 'GET', params },
  );
}

export async function getCategory(categoryId: number) {
  return request<API.Product.InfoResult<API.Product.Category>>(
    `${baseUrl}/categories/${categoryId}`,
    { method: 'GET' },
  );
}

export async function addCategory(data: API.Product.Category) {
  return request<API.Result>(`${baseUrl}/categories`, {
    method: 'POST',
    data,
  });
}

export async function updateCategory(
  categoryId: number,
  data: API.Product.Category,
) {
  return request<API.Result>(`${baseUrl}/categories/${categoryId}`, {
    method: 'PUT',
    data,
  });
}

export async function deleteCategory(categoryId: number) {
  return request<API.Result>(`${baseUrl}/categories/${categoryId}`, {
    method: 'DELETE',
  });
}

export async function getAttributeList(params?: Record<string, any>) {
  return request<API.Product.PageResult<API.Product.Attribute>>(
    `${baseUrl}/attributes/list`,
    { method: 'GET', params },
  );
}

export async function getEnabledAttributeList() {
  return request<API.Product.InfoResult<API.Product.Attribute[]>>(
    `${baseUrl}/attributes/options`,
    { method: 'GET' },
  );
}

export async function getAttribute(attributeId: number) {
  return request<API.Product.InfoResult<API.Product.Attribute>>(
    `${baseUrl}/attributes/${attributeId}`,
    { method: 'GET' },
  );
}

export async function addAttribute(data: API.Product.Attribute) {
  return request<API.Result>(`${baseUrl}/attributes`, {
    method: 'POST',
    data,
  });
}

export async function updateAttribute(
  attributeId: number,
  data: API.Product.Attribute,
) {
  return request<API.Result>(`${baseUrl}/attributes/${attributeId}`, {
    method: 'PUT',
    data,
  });
}

export async function deleteAttribute(attributeId: number) {
  return request<API.Result>(`${baseUrl}/attributes/${attributeId}`, {
    method: 'DELETE',
  });
}

export async function getAttributeOptionList(attributeId: number) {
  return request<API.Product.InfoResult<API.Product.AttributeOption[]>>(
    `${baseUrl}/attributes/${attributeId}/options`,
    { method: 'GET' },
  );
}

export async function addAttributeOption(
  attributeId: number,
  data: API.Product.AttributeOption,
) {
  return request<API.Result>(`${baseUrl}/attributes/${attributeId}/options`, {
    method: 'POST',
    data,
  });
}

export async function updateAttributeOption(
  attributeId: number,
  optionId: number,
  data: API.Product.AttributeOption,
) {
  return request<API.Result>(
    `${baseUrl}/attributes/${attributeId}/options/${optionId}`,
    { method: 'PUT', data },
  );
}

export async function deleteAttributeOption(
  attributeId: number,
  optionId: number,
) {
  return request<API.Result>(
    `${baseUrl}/attributes/${attributeId}/options/${optionId}`,
    { method: 'DELETE' },
  );
}

export async function getCategoryAttributeList(categoryId: number) {
  return request<API.Product.InfoResult<API.Product.CategoryAttribute[]>>(
    `${baseUrl}/category-attributes/list/${categoryId}`,
    { method: 'GET' },
  );
}

export async function getCategorySchema(categoryId: number) {
  return request<API.Product.InfoResult<API.Product.CategoryAttribute[]>>(
    `${baseUrl}/category-attributes/schema/${categoryId}`,
    { method: 'GET' },
  );
}

export async function saveCategoryAttribute(
  data: API.Product.CategoryAttribute,
) {
  return request<API.Result>(`${baseUrl}/category-attributes`, {
    method: 'POST',
    data,
  });
}

export async function deleteCategoryAttribute(categoryAttributeId: number) {
  return request<API.Result>(
    `${baseUrl}/category-attributes/${categoryAttributeId}`,
    { method: 'DELETE' },
  );
}
