import { jsx as _jsx } from "react/jsx-runtime";
import { ProTable } from '@ant-design/pro-components';
import { Modal, Table, Tag, Typography } from 'antd';
import { getProductConfigChangeLogList } from '@/services/product/product';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
const bizTypeText = {
    CATEGORY: '商品分类',
    ATTRIBUTE: '商品属性',
    ATTRIBUTE_OPTION: '属性选项',
    CATEGORY_ATTRIBUTE_RULE: '类目属性规则',
};
const actionText = {
    CREATE: '新增',
    UPDATE: '修改',
    ENABLE: '启用',
    DISABLE: '停用',
    DELETE: '删除',
};
const actionColor = {
    CREATE: 'green',
    UPDATE: 'blue',
    ENABLE: 'green',
    DISABLE: 'orange',
    DELETE: 'red',
};
const sourceText = {
    PAGE: '页面',
    IMPORT: '导入',
};
function toValueEnum(source) {
    return Object.entries(source).reduce((valueEnum, [value, text]) => {
        valueEnum[value] = { text };
        return valueEnum;
    }, {});
}
function parseDiff(diffJson) {
    if (!diffJson) {
        return [];
    }
    try {
        const parsed = JSON.parse(diffJson);
        return Array.isArray(parsed) ? parsed : [];
    }
    catch {
        return [];
    }
}
function textValue(value) {
    if (value === undefined || value === null || value === '') {
        return '-';
    }
    return String(value);
}
function buildBizTypeValueEnum(bizType, bizTypes) {
    const allowedTypes = bizTypes?.length ? bizTypes : bizType ? [bizType] : [];
    if (allowedTypes.length === 0) {
        return toValueEnum(bizTypeText);
    }
    return allowedTypes.reduce((valueEnum, item) => {
        valueEnum[item] = { text: bizTypeText[item] || item };
        return valueEnum;
    }, {});
}
export default function ProductConfigChangeLogDrawer({ open, onOpenChange, bizType, bizTypes, bizId, title, }) {
    const scopedBizTypes = bizTypes?.filter(Boolean);
    const bizTypeValueEnum = buildBizTypeValueEnum(bizType, scopedBizTypes);
    const actionValueEnum = toValueEnum(actionText);
    const sourceValueEnum = toValueEnum(sourceText);
    const columns = [
        {
            title: '关键词',
            dataIndex: 'keyword',
            hideInTable: true,
            fieldProps: {
                placeholder: '对象名称/编码/摘要',
            },
        },
        {
            title: '操作时间',
            dataIndex: 'changeTimeRange',
            valueType: 'dateTimeRange',
            hideInTable: true,
            search: {
                transform: (value) => ({
                    beginTime: value?.[0],
                    endTime: value?.[1],
                }),
            },
        },
        {
            title: '变更时间',
            dataIndex: 'changeTime',
            width: 170,
            search: false,
        },
        {
            title: '对象类型',
            dataIndex: 'bizType',
            valueType: 'select',
            valueEnum: bizTypeValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 120,
            search: !bizType || Boolean(scopedBizTypes?.length && scopedBizTypes.length > 1),
            renderText: (value) => bizTypeText[value] || value || '-',
        },
        {
            title: '对象名称',
            dataIndex: 'bizName',
            width: 160,
            search: false,
            ellipsis: true,
            renderText: (value) => value || '-',
        },
        {
            title: '对象编码',
            dataIndex: 'bizCode',
            width: 170,
            search: false,
            ellipsis: true,
            renderText: (value) => value || '-',
        },
        {
            title: '操作类型',
            dataIndex: 'actionType',
            valueType: 'select',
            valueEnum: actionValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 100,
            render: (_, record) => (_jsx(Tag, { color: actionColor[record.actionType || ''] || 'default', children: actionText[record.actionType || ''] || record.actionType || '-' })),
        },
        {
            title: '来源',
            dataIndex: 'actionSource',
            valueType: 'select',
            valueEnum: sourceValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 90,
            renderText: (value) => sourceText[value] || value || '-',
        },
        {
            title: '操作人',
            dataIndex: 'operatorName',
            width: 110,
            renderText: (value) => value || '-',
        },
        {
            title: '变更摘要',
            dataIndex: 'changeSummary',
            search: false,
            ellipsis: true,
        },
    ];
    return (_jsx(Modal, { title: `操作日志${title ? `：${title}` : ''}`, open: open, width: 1180, footer: null, centered: true, destroyOnHidden: true, styles: {
            body: {
                maxHeight: '72vh',
                overflow: 'auto',
                paddingTop: 8,
            },
        }, onCancel: () => onOpenChange(false), children: _jsx(ProTable, { rowKey: "logId", columns: columns, size: "small", options: false, pagination: { pageSize: 10 }, search: {
                labelWidth: 80,
                span: 8,
                defaultCollapsed: false,
            }, scroll: { x: 1180 }, request: async (params) => {
                const requestParams = {
                    ...params,
                    pageNum: params.current,
                    pageSize: params.pageSize,
                };
                delete requestParams.current;
                if (bizType && !requestParams.bizType) {
                    requestParams.bizType = bizType;
                }
                if (scopedBizTypes?.length) {
                    requestParams.bizTypes = scopedBizTypes.join(',');
                }
                if (bizId) {
                    requestParams.bizId = bizId;
                }
                const resp = await getProductConfigChangeLogList(requestParams);
                return {
                    data: resp.rows || [],
                    total: resp.total || 0,
                    success: resp.code === 200,
                };
            }, expandable: {
                expandedRowRender: (record) => {
                    const diff = parseDiff(record.diffJson);
                    if (diff.length === 0) {
                        return _jsx(Typography.Text, { type: "secondary", children: "\u65E0\u5B57\u6BB5\u5DEE\u5F02" });
                    }
                    return (_jsx(Table, { size: "small", rowKey: (item) => item.field || `${item.fieldLabel}-${item.beforeValue}-${item.afterValue}`, pagination: false, dataSource: diff, columns: [
                            {
                                title: '字段',
                                dataIndex: 'fieldLabel',
                                width: 180,
                                render: (value) => value || '-',
                            },
                            {
                                title: '修改前',
                                dataIndex: 'beforeValue',
                                render: (value) => textValue(value),
                            },
                            {
                                title: '修改后',
                                dataIndex: 'afterValue',
                                render: (value) => textValue(value),
                            },
                        ] }));
                },
            } }) }));
}
