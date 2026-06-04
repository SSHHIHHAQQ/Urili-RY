export const statusValueEnum = {
  0: { text: '正常', status: 'Success' },
  1: { text: '停用', status: 'Default' },
};

export const yesNoValueEnum = {
  Y: { text: '是', status: 'Success' },
  N: { text: '否', status: 'Default' },
};

export const yesNoOptions = [
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' },
];

export const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
];

export const attributeTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '数字', value: 'NUMBER' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: '单选', value: 'SINGLE_SELECT' },
  { label: '多选', value: 'MULTI_SELECT' },
  { label: '日期', value: 'DATE' },
];

export const optionSourceOptions = [
  { label: '无选项', value: 'NONE' },
  { label: '属性自定义选项', value: 'ATTRIBUTE_OPTION' },
  { label: '若依字典', value: 'SYS_DICT' },
];

export const selectAttributeOptionSourceOptions = [
  { label: '属性自定义选项', value: 'ATTRIBUTE_OPTION' },
  { label: '若依字典', value: 'SYS_DICT' },
];

export const optionAttributeTypes = ['SINGLE_SELECT', 'MULTI_SELECT'];

export function isOptionAttributeType(attributeType?: string) {
  return optionAttributeTypes.includes(attributeType || '');
}

export function isNumberAttributeType(attributeType?: string) {
  return attributeType === 'NUMBER';
}

export const ruleModeOptions = [
  { label: '新增本类目属性', value: 'ADD' },
  { label: '调整继承属性', value: 'OVERRIDE' },
  { label: '停用继承属性', value: 'DISABLE' },
];

export const ruleModeValueEnum = {
  ADD: { text: '新增' },
  OVERRIDE: { text: '调整继承' },
  DISABLE: { text: '停用继承' },
};

export const attributeGroupOptions = [
  { label: '基础信息', value: 'BASIC' },
  { label: '功能参数', value: 'FUNCTION' },
  { label: '合规信息', value: 'COMPLIANCE' },
  { label: '物流信息', value: 'LOGISTICS' },
];

export function optionArrayToValueEnum(
  options: { label: string; value: string }[],
) {
  return options.reduce<Record<string, { text: string }>>((valueEnum, item) => {
    valueEnum[item.value] = { text: item.label };
    return valueEnum;
  }, {});
}
