import { jsx as _jsx } from "react/jsx-runtime";
import { ProTable } from '@ant-design/pro-components';
import { Modal, Table, Tag, Typography } from 'antd';
import { getDistributionOperationLogList } from '@/services/product/distributionProduct';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { getControlStatusText, getSalesStatusText, productOperationTypeColor, productOperationTypeText, } from '../constants';
const ownerTypeText = {
    SPU: 'SPU',
    SKU: 'SKU',
};
const operationTypeValueEnum = Object.entries(productOperationTypeText).reduce((valueEnum, [value, text]) => {
    valueEnum[value] = { text };
    return valueEnum;
}, {});
const ownerTypeValueEnum = Object.entries(ownerTypeText).reduce((valueEnum, [value, text]) => {
    valueEnum[value] = { text };
    return valueEnum;
}, {});
function parseDiff(diffJson) {
    if (!diffJson)
        return [];
    try {
        const parsed = JSON.parse(diffJson);
        return Array.isArray(parsed) ? parsed : [parsed];
    }
    catch {
        return [];
    }
}
function renderDiffValue(field, value) {
    if (value === undefined || value === null || value === '')
        return '-';
    if (field === 'spuStatus' || field === 'skuStatus')
        return getSalesStatusText(String(value));
    if (field === 'controlStatus')
        return getControlStatusText(String(value));
    return String(value);
}
function fieldLabel(field) {
    const labels = {
        spuStatus: 'SPU销售状态',
        skuStatus: 'SKU销售状态',
        controlStatus: '管控状态',
        salePrice: '销售价',
    };
    return (field && labels[field]) || field || '-';
}
export default function ProductDistributionOperationLogDrawer({ open, onOpenChange, }) {
    const columns = [
        {
            title: '关键词',
            dataIndex: 'keyword',
            hideInTable: true,
            fieldProps: {
                placeholder: '系统编码/卖家/摘要',
            },
        },
        {
            title: '操作时间',
            dataIndex: 'operationTimeRange',
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
            title: '操作时间',
            dataIndex: 'operationTime',
            search: false,
            width: 170,
        },
        {
            title: '对象',
            dataIndex: 'ownerType',
            valueType: 'select',
            valueEnum: ownerTypeValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 90,
            renderText: (value) => ownerTypeText[value] || value || '-',
        },
        {
            title: '系统SPU',
            dataIndex: 'systemSpuCode',
            search: false,
            width: 160,
            ellipsis: true,
        },
        {
            title: '系统SKU',
            dataIndex: 'systemSkuCode',
            search: false,
            width: 160,
            ellipsis: true,
            renderText: (value) => value || '-',
        },
        {
            title: '操作类型',
            dataIndex: 'operationType',
            valueType: 'select',
            valueEnum: operationTypeValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 130,
            render: (_, record) => (_jsx(Tag, { color: productOperationTypeColor[record.operationType || ''] || 'default', children: productOperationTypeText[record.operationType || ''] || record.operationType || '-' })),
        },
        {
            title: '卖家',
            dataIndex: 'sellerName',
            search: false,
            width: 150,
            ellipsis: true,
            renderText: (value) => value || '-',
        },
        {
            title: '操作人',
            dataIndex: 'operatorName',
            width: 110,
            renderText: (value) => value || '-',
        },
        {
            title: '摘要',
            dataIndex: 'changeSummary',
            search: false,
            width: 240,
            ellipsis: true,
            renderText: (value) => value || '-',
        },
        {
            title: '原因',
            dataIndex: 'reason',
            search: false,
            width: 180,
            ellipsis: true,
            renderText: (value) => value || '-',
        },
    ];
    return (_jsx(Modal, { title: "\u64CD\u4F5C\u65E5\u5FD7", open: open, width: 1260, footer: null, centered: true, destroyOnHidden: true, styles: {
            body: {
                maxHeight: '72vh',
                overflow: 'auto',
                paddingTop: 8,
            },
        }, onCancel: () => onOpenChange(false), children: _jsx(ProTable, { rowKey: "logId", columns: columns, size: "small", options: false, pagination: { pageSize: 10 }, search: {
                labelWidth: 80,
                span: 8,
                defaultCollapsed: false,
            }, scroll: { x: 1260 }, request: async (params) => {
                const requestParams = {
                    ...params,
                    pageNum: params.current,
                    pageSize: params.pageSize,
                };
                delete requestParams.current;
                const resp = await getDistributionOperationLogList(requestParams);
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
                    return (_jsx(Table, { size: "small", rowKey: (item) => item.field || `${item.before}-${item.after}`, pagination: false, dataSource: diff, columns: [
                            {
                                title: '字段',
                                dataIndex: 'field',
                                width: 180,
                                render: (value) => fieldLabel(value),
                            },
                            {
                                title: '修改前',
                                width: 260,
                                render: (_, item) => renderDiffValue(item.field, item.before ?? item.beforeValue),
                            },
                            {
                                title: '修改后',
                                width: 260,
                                render: (_, item) => renderDiffValue(item.field, item.after ?? item.afterValue),
                            },
                        ] }));
                },
            } }) }));
}
