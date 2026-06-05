import { request } from '@umijs/max';
import { downLoadXlsx } from '@/utils/downloadfile';

const baseUrl = '/api/product/admin';

function buildImportFormData(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
}

export async function getCategoryList(params?: Record<string, any>) {
  return request<API.Product.InfoResult<API.Product.Category[]>>(
    `${baseUrl}/categories/list`,
    { method: 'GET', params },
  );
}

export async function getCategoryChildren(params?: Record<string, any>) {
  return request<API.Product.InfoResult<API.Product.Category[]>>(
    `${baseUrl}/categories/children`,
    { method: 'GET', params },
  );
}

export async function searchCategories(params?: Record<string, any>) {
  return request<API.Product.PageResult<API.Product.Category>>(
    `${baseUrl}/categories/search`,
    { method: 'GET', params },
  );
}

export async function getCategoryOptions(params?: Record<string, any>) {
  return request<API.Product.InfoResult<API.Product.Category[]>>(
    `${baseUrl}/categories/options`,
    { method: 'GET', params },
  );
}

export async function getCategoryPath(categoryId: number) {
  return request<API.Product.InfoResult<API.Product.Category[]>>(
    `${baseUrl}/categories/path/${categoryId}`,
    { method: 'GET' },
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

export function downloadCategoryImportTemplate() {
  return downLoadXlsx(
    `${baseUrl}/categories/importTemplate`,
    {},
    `product_category_import_template_${new Date().getTime()}.xlsx`,
  );
}

export async function previewCategoryImport(file: File, updateSupport: boolean) {
  return request<API.Product.ImportResultResponse>(
    `${baseUrl}/categories/importPreview`,
    {
      method: 'POST',
      params: { updateSupport },
      data: buildImportFormData(file),
      skipErrorHandler: true,
    },
  );
}

export async function importCategoryData(file: File, updateSupport: boolean) {
  return request<API.Product.ImportResultResponse>(
    `${baseUrl}/categories/importData`,
    {
      method: 'POST',
      params: { updateSupport },
      data: buildImportFormData(file),
      skipErrorHandler: true,
    },
  );
}

export async function getAttributeList(params?: Record<string, any>) {
  return request<API.Product.PageResult<API.Product.Attribute>>(
    `${baseUrl}/attributes/list`,
    { method: 'GET', params },
  );
}

export async function getEnabledAttributeList(params?: Record<string, any>) {
  return request<API.Product.InfoResult<API.Product.Attribute[]>>(
    `${baseUrl}/attributes/options`,
    { method: 'GET', params },
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

export async function updateAttributeStatus(attributeId: number, status: string) {
  return request<API.Result>(`${baseUrl}/attributes/${attributeId}/status`, {
    method: 'PUT',
    data: { status },
  });
}

export async function deleteAttribute(attributeId: number) {
  return request<API.Result>(`${baseUrl}/attributes/${attributeId}`, {
    method: 'DELETE',
  });
}

export function downloadAttributeImportTemplate() {
  return downLoadXlsx(
    `${baseUrl}/attributes/importTemplate`,
    {},
    `product_attribute_import_template_${new Date().getTime()}.xlsx`,
  );
}

export async function previewAttributeImport(file: File, updateSupport: boolean) {
  return request<API.Product.ImportResultResponse>(
    `${baseUrl}/attributes/importPreview`,
    {
      method: 'POST',
      params: { updateSupport },
      data: buildImportFormData(file),
      skipErrorHandler: true,
    },
  );
}

export async function importAttributeData(file: File, updateSupport: boolean) {
  return request<API.Product.ImportResultResponse>(
    `${baseUrl}/attributes/importData`,
    {
      method: 'POST',
      params: { updateSupport },
      data: buildImportFormData(file),
      skipErrorHandler: true,
    },
  );
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

export function downloadAttributeOptionImportTemplate() {
  return downLoadXlsx(
    `${baseUrl}/attributes/options/importTemplate`,
    {},
    `product_attribute_option_import_template_${new Date().getTime()}.xlsx`,
  );
}

export async function previewAttributeOptionImport(
  file: File,
  updateSupport: boolean,
) {
  return request<API.Product.ImportResultResponse>(
    `${baseUrl}/attributes/options/importPreview`,
    {
      method: 'POST',
      params: { updateSupport },
      data: buildImportFormData(file),
      skipErrorHandler: true,
    },
  );
}

export async function importAttributeOptionData(
  file: File,
  updateSupport: boolean,
) {
  return request<API.Product.ImportResultResponse>(
    `${baseUrl}/attributes/options/importData`,
    {
      method: 'POST',
      params: { updateSupport },
      data: buildImportFormData(file),
      skipErrorHandler: true,
    },
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

export async function getProductConfigChangeLogList(
  params?: Record<string, any>,
) {
  return request<API.Product.PageResult<API.Product.ConfigChangeLog>>(
    `${baseUrl}/change-logs/list`,
    { method: 'GET', params },
  );
}
