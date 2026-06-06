import { isValidElement } from 'react';
function normalizeSearchText(value) {
    if (value === undefined || value === null) {
        return '';
    }
    if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
        return String(value);
    }
    if (Array.isArray(value)) {
        return value.map(normalizeSearchText).filter(Boolean).join(' ');
    }
    if (isValidElement(value)) {
        return normalizeSearchText(value.props.children);
    }
    if (typeof value === 'object') {
        const record = value;
        return [
            record.label,
            record.value,
            record.title,
            record.children,
            record.text,
            record.name,
            record.code,
            record.searchText,
        ]
            .map(normalizeSearchText)
            .filter(Boolean)
            .join(' ');
    }
    return '';
}
export function filterSelectOption(input, option) {
    const keyword = input.trim().toLowerCase();
    if (!keyword) {
        return true;
    }
    return normalizeSearchText(option).toLowerCase().includes(keyword);
}
export function filterTreeSelectNode(input, node) {
    const keyword = input.trim().toLowerCase();
    if (!keyword) {
        return true;
    }
    return normalizeSearchText(node?.title)
        .toLowerCase()
        .includes(keyword);
}
export const SEARCHABLE_SELECT_PROPS = {
    showSearch: true,
    optionFilterProp: 'label',
    filterOption: filterSelectOption,
};
export const SEARCHABLE_TREE_SELECT_PROPS = {
    showSearch: true,
    treeNodeFilterProp: 'title',
    filterTreeNode: filterTreeSelectNode,
};
