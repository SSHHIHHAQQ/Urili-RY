import { jsx as _jsx } from "react/jsx-runtime";
import { Button } from 'antd';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { ruleModeValueEnum, statusValueEnum, yesNoValueEnum, } from '../../constants';
export function buildCategoryAttributeColumns({ access, attributeTypeValueEnum, attributeGroupValueEnum, onEditRule, onRemoveRule, }) {
    const baseColumns = [
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
    const directColumns = [
        ...baseColumns,
        {
            title: '操作',
            valueType: 'option',
            width: 150,
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('product:categoryAttribute:edit'), onClick: () => onEditRule(record), children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('product:categoryAttribute:edit'), onClick: () => onRemoveRule(record), children: "\u79FB\u9664" }, "delete"),
            ],
        },
    ];
    const schemaColumns = [
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
