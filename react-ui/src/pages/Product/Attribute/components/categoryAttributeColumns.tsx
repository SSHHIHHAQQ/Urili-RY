import { type ProColumns } from '@ant-design/pro-components';
import { Button } from 'antd';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  ruleModeValueEnum,
  statusValueEnum,
  yesNoValueEnum,
} from '../../constants';
import { sourceScopeValueEnum } from './categoryAttributeFilterUtils';

type AccessLike = {
  hasPerms: (permission: string) => boolean;
};

type BuildCategoryAttributeColumnsOptions = {
  access: AccessLike;
  attributeTypeValueEnum: Record<string, { text: string }>;
  attributeGroupValueEnum: Record<string, { text: string }>;
  sourceCategoryOptions: { label: string; value: number }[];
  onEditRule: (record: API.Product.CategoryAttribute) => void;
  onRemoveRule: (record: API.Product.CategoryAttribute) => void;
};

export function buildCategoryAttributeColumns({
  access,
  attributeTypeValueEnum,
  attributeGroupValueEnum,
  sourceCategoryOptions,
  onEditRule,
  onRemoveRule,
}: BuildCategoryAttributeColumnsOptions) {
  const baseColumns: ProColumns<API.Product.CategoryAttribute>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '属性名称',
      dataIndex: 'attributeName',
      search: false,
      width: 150,
    },
    {
      title: '属性编码',
      dataIndex: 'attributeCode',
      search: false,
      width: 150,
    },
    {
      title: '类型',
      dataIndex: 'attributeType',
      valueType: 'select',
      valueEnum: attributeTypeValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 110,
    },
    {
      title: '规则',
      dataIndex: 'ruleMode',
      valueEnum: ruleModeValueEnum,
      search: false,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 110,
    },
    {
      title: '必填',
      dataIndex: 'requiredFlag',
      valueType: 'select',
      valueEnum: yesNoValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 80,
    },
    {
      title: '展示',
      dataIndex: 'visibleFlag',
      valueEnum: yesNoValueEnum,
      search: false,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 80,
    },
    {
      title: '可编辑',
      dataIndex: 'editableFlag',
      valueEnum: yesNoValueEnum,
      search: false,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 90,
    },
    {
      title: '可筛选',
      dataIndex: 'filterableFlag',
      valueType: 'select',
      valueEnum: yesNoValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 90,
    },
    {
      title: '分组',
      dataIndex: 'groupCode',
      valueType: 'select',
      valueEnum: attributeGroupValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 110,
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      search: false,
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: statusValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 90,
    },
  ];

  const directColumns: ProColumns<API.Product.CategoryAttribute>[] = [
    ...baseColumns,
    {
      title: '操作',
      valueType: 'option',
      width: 130,
      render: (_, record) => [
        <Button
          key="edit"
          type="link"
          size="small"
          hidden={!access.hasPerms('product:categoryAttribute:edit')}
          onClick={() => onEditRule(record)}
        >
          编辑
        </Button>,
        <Button
          key="delete"
          type="link"
          size="small"
          danger
          hidden={!access.hasPerms('product:categoryAttribute:edit')}
          onClick={() => onRemoveRule(record)}
        >
          移除
        </Button>,
      ],
    },
  ];

  const schemaColumns: ProColumns<API.Product.CategoryAttribute>[] = [
    {
      title: '来源范围',
      dataIndex: 'sourceScope',
      valueType: 'select',
      valueEnum: sourceScopeValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      hideInTable: true,
    },
    {
      title: '来源类目',
      dataIndex: 'sourceCategoryId',
      valueType: 'select',
      fieldProps: {
        ...SEARCHABLE_SELECT_PROPS,
        options: sourceCategoryOptions,
      },
      hideInTable: true,
    },
    {
      title: '来源类目',
      dataIndex: 'sourceCategoryName',
      search: false,
      width: 150,
    },
    ...baseColumns,
  ];

  return { directColumns, schemaColumns };
}
