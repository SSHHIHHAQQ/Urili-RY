import type { ReactNode } from 'react';

export function parseJsonArrayValues(value?: string) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.filter(Boolean).map(String) : [];
  } catch {
    return [value];
  }
}

function optionLookupKey(prefix: 'id' | 'code', attributeKey: string | number | undefined, optionCode: string) {
  return attributeKey === undefined || attributeKey === null || attributeKey === ''
    ? ''
    : `${prefix}:${attributeKey}:${optionCode}`;
}

function attributeLookupKey(prefix: 'id' | 'code', attributeKey: string | number | undefined) {
  return attributeKey === undefined || attributeKey === null || attributeKey === ''
    ? ''
    : `${prefix}:${attributeKey}`;
}

export function buildAttributeLabelMap(categorySchema: API.Product.CategoryAttribute[]) {
  const map = new Map<string, string>();
  categorySchema.forEach((item) => {
    if (!item.attributeName) return;
    const idKey = attributeLookupKey('id', item.attributeId);
    const codeKey = attributeLookupKey('code', item.attributeCode);
    if (idKey) map.set(idKey, item.attributeName);
    if (codeKey) map.set(codeKey, item.attributeName);
  });
  return map;
}

export function buildOptionLabelMap(categorySchema: API.Product.CategoryAttribute[]) {
  const map = new Map<string, string>();
  categorySchema.forEach((item) => {
    (item.options || []).forEach((option) => {
      if (!option.optionCode || !option.optionLabel) return;
      const optionCode = String(option.optionCode);
      const idKey = optionLookupKey('id', item.attributeId, optionCode);
      const codeKey = optionLookupKey('code', item.attributeCode, optionCode);
      if (idKey) map.set(idKey, option.optionLabel);
      if (codeKey) map.set(codeKey, option.optionLabel);
    });
  });
  return map;
}

export function resolveOptionLabel(
  item: API.ProductDistribution.AttributeValue,
  optionCode: string | undefined,
  optionLabelMap: Map<string, string>,
) {
  if (!optionCode) return '';
  return optionLabelMap.get(optionLookupKey('id', item.attributeId, optionCode))
    || optionLabelMap.get(optionLookupKey('code', item.attributeCode, optionCode))
    || '';
}

export function resolveAttributeLabel(
  item: API.ProductDistribution.AttributeValue,
  attributeLabelMap: Map<string, string>,
) {
  return attributeLabelMap.get(attributeLookupKey('id', item.attributeId))
    || attributeLabelMap.get(attributeLookupKey('code', item.attributeCode))
    || item.attributeName
    || item.attributeCode
    || '属性';
}

export function formatAttributeValue(
  item: API.ProductDistribution.AttributeValue,
  optionLabelMap: Map<string, string>,
): ReactNode {
  const valueCodeLabel = resolveOptionLabel(item, item.valueCode, optionLabelMap);
  if (item.attributeType === 'BOOLEAN') {
    if (item.valueCode === 'Y') return '是';
    if (item.valueCode === 'N') return '否';
  }
  if (item.attributeType === 'SINGLE_SELECT') {
    return valueCodeLabel
      || item.valueText
      || item.valueCode
      || '--';
  }
  if (item.attributeType === 'MULTI_SELECT') {
    const values = parseJsonArrayValues(item.valueJson);
    return values.length
      ? values.map((value) => resolveOptionLabel(item, value, optionLabelMap) || value).join(' / ')
      : '--';
  }
  return item.valueText
    || valueCodeLabel
    || item.valueCode
    || (item.valueNumber !== undefined && item.valueNumber !== null ? String(item.valueNumber) : '')
    || item.valueDate
    || item.valueJson
    || '--';
}
