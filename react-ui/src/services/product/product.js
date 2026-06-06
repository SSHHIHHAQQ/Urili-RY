import { request } from '@umijs/max';
import { downLoadXlsx } from '@/utils/downloadfile';
const baseUrl = '/api/product/admin';
function buildImportFormData(file) {
    const formData = new FormData();
    formData.append('file', file);
    return formData;
}
export async function getCategoryList(params) {
    return request(`${baseUrl}/categories/list`, { method: 'GET', params });
}
export async function getCategoryChildren(params) {
    return request(`${baseUrl}/categories/children`, { method: 'GET', params });
}
export async function searchCategories(params) {
    return request(`${baseUrl}/categories/search`, { method: 'GET', params });
}
export async function getCategoryOptions(params) {
    return request(`${baseUrl}/categories/options`, { method: 'GET', params });
}
export async function getCategoryPath(categoryId) {
    return request(`${baseUrl}/categories/path/${categoryId}`, { method: 'GET' });
}
export async function getCategory(categoryId) {
    return request(`${baseUrl}/categories/${categoryId}`, { method: 'GET' });
}
export async function addCategory(data) {
    return request(`${baseUrl}/categories`, {
        method: 'POST',
        data,
    });
}
export async function updateCategory(categoryId, data) {
    return request(`${baseUrl}/categories/${categoryId}`, {
        method: 'PUT',
        data,
    });
}
export async function deleteCategory(categoryId) {
    return request(`${baseUrl}/categories/${categoryId}`, {
        method: 'DELETE',
    });
}
export function downloadCategoryImportTemplate() {
    return downLoadXlsx(`${baseUrl}/categories/importTemplate`, {}, `product_category_import_template_${new Date().getTime()}.xlsx`);
}
export async function previewCategoryImport(file, updateSupport) {
    return request(`${baseUrl}/categories/importPreview`, {
        method: 'POST',
        params: { updateSupport },
        data: buildImportFormData(file),
        skipErrorHandler: true,
    });
}
export async function importCategoryData(file, updateSupport) {
    return request(`${baseUrl}/categories/importData`, {
        method: 'POST',
        params: { updateSupport },
        data: buildImportFormData(file),
        skipErrorHandler: true,
    });
}
export async function getAttributeList(params) {
    return request(`${baseUrl}/attributes/list`, { method: 'GET', params });
}
export async function getEnabledAttributeList(params) {
    return request(`${baseUrl}/attributes/options`, { method: 'GET', params });
}
export async function getAttribute(attributeId) {
    return request(`${baseUrl}/attributes/${attributeId}`, { method: 'GET' });
}
export async function addAttribute(data) {
    return request(`${baseUrl}/attributes`, {
        method: 'POST',
        data,
    });
}
export async function updateAttribute(attributeId, data) {
    return request(`${baseUrl}/attributes/${attributeId}`, {
        method: 'PUT',
        data,
    });
}
export async function updateAttributeStatus(attributeId, status) {
    return request(`${baseUrl}/attributes/${attributeId}/status`, {
        method: 'PUT',
        data: { status },
    });
}
export async function deleteAttribute(attributeId) {
    return request(`${baseUrl}/attributes/${attributeId}`, {
        method: 'DELETE',
    });
}
export function downloadAttributeImportTemplate() {
    return downLoadXlsx(`${baseUrl}/attributes/importTemplate`, {}, `product_attribute_import_template_${new Date().getTime()}.xlsx`);
}
export async function previewAttributeImport(file, updateSupport) {
    return request(`${baseUrl}/attributes/importPreview`, {
        method: 'POST',
        params: { updateSupport },
        data: buildImportFormData(file),
        skipErrorHandler: true,
    });
}
export async function importAttributeData(file, updateSupport) {
    return request(`${baseUrl}/attributes/importData`, {
        method: 'POST',
        params: { updateSupport },
        data: buildImportFormData(file),
        skipErrorHandler: true,
    });
}
export async function getAttributeOptionList(attributeId) {
    return request(`${baseUrl}/attributes/${attributeId}/options`, { method: 'GET' });
}
export async function addAttributeOption(attributeId, data) {
    return request(`${baseUrl}/attributes/${attributeId}/options`, {
        method: 'POST',
        data,
    });
}
export async function updateAttributeOption(attributeId, optionId, data) {
    return request(`${baseUrl}/attributes/${attributeId}/options/${optionId}`, { method: 'PUT', data });
}
export async function deleteAttributeOption(attributeId, optionId) {
    return request(`${baseUrl}/attributes/${attributeId}/options/${optionId}`, { method: 'DELETE' });
}
export function downloadAttributeOptionImportTemplate() {
    return downLoadXlsx(`${baseUrl}/attributes/options/importTemplate`, {}, `product_attribute_option_import_template_${new Date().getTime()}.xlsx`);
}
export async function previewAttributeOptionImport(file, updateSupport) {
    return request(`${baseUrl}/attributes/options/importPreview`, {
        method: 'POST',
        params: { updateSupport },
        data: buildImportFormData(file),
        skipErrorHandler: true,
    });
}
export async function importAttributeOptionData(file, updateSupport) {
    return request(`${baseUrl}/attributes/options/importData`, {
        method: 'POST',
        params: { updateSupport },
        data: buildImportFormData(file),
        skipErrorHandler: true,
    });
}
export async function getCategoryAttributeList(categoryId) {
    return request(`${baseUrl}/category-attributes/list/${categoryId}`, { method: 'GET' });
}
export async function getCategorySchema(categoryId) {
    return request(`${baseUrl}/category-attributes/schema/${categoryId}`, { method: 'GET' });
}
export async function saveCategoryAttribute(data) {
    return request(`${baseUrl}/category-attributes`, {
        method: 'POST',
        data,
    });
}
export async function deleteCategoryAttribute(categoryAttributeId) {
    return request(`${baseUrl}/category-attributes/${categoryAttributeId}`, { method: 'DELETE' });
}
export async function getProductConfigChangeLogList(params) {
    return request(`${baseUrl}/change-logs/list`, { method: 'GET', params });
}
