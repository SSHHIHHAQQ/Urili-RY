import { isValidElement } from 'react';
import type { ReactNode } from 'react';
import type { SelectProps, TreeSelectProps } from 'antd';

type SelectSearchOption = Record<string, unknown>;

function normalizeSearchText(value: unknown): string {
  if (value === undefined || value === null) {
    return '';
  }
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  if (Array.isArray(value)) {
    return value.map(normalizeSearchText).filter(Boolean).join(' ');
  }
  if (isValidElement<{ children?: ReactNode }>(value)) {
    return normalizeSearchText(value.props.children);
  }
  if (typeof value === 'object') {
    const record = value as SelectSearchOption;
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

export function filterSelectOption(input: string, option?: SelectSearchOption) {
  const keyword = input.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  return normalizeSearchText(option).toLowerCase().includes(keyword);
}

export function filterTreeSelectNode(input: string, node?: SelectSearchOption) {
  return filterSelectOption(input, node);
}

export const SEARCHABLE_SELECT_PROPS: SelectProps = {
  showSearch: true,
  optionFilterProp: 'label',
  filterOption: filterSelectOption as SelectProps['filterOption'],
};

export const SEARCHABLE_TREE_SELECT_PROPS: TreeSelectProps = {
  showSearch: true,
  treeNodeFilterProp: 'label',
  filterTreeNode: filterTreeSelectNode as TreeSelectProps['filterTreeNode'],
};
