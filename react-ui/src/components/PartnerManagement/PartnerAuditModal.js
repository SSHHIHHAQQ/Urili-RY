import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ProTable } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Descriptions, Modal, Space, Tabs, Tag, Typography } from 'antd';
import { getPersistedProTableSearch, getProTablePagination } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
const compactCellTextStyle = {
    display: 'block',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    lineHeight: 1.35,
};
const auditModalWidth = 'min(1480px, calc(100vw - 64px))';
const auditTabsStyle = {
    minWidth: 0,
};
const auditTableWrapperStyle = {
    minWidth: 0,
    overflow: 'hidden',
};
const detailTextStyle = {
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-word',
};
const loginStatusValueEnum = {
    '0': { text: '成功', status: 'Success' },
    '1': { text: '失败', status: 'Error' },
};
const operStatusValueEnum = {
    0: { text: '正常', status: 'Success' },
    1: { text: '异常', status: 'Error' },
};
const ticketStatusValueEnum = {
    ISSUED: { text: '已签发', status: 'Processing' },
    USED: { text: '已使用', status: 'Success' },
    EXPIRED: { text: '已过期', status: 'Default' },
};
function getValue(record, field) {
    return record ? record[field] : undefined;
}
function getAccountId(config, account) {
    return account ? account[config.accountIdField] || account.accountId : undefined;
}
function renderCompactText(value) {
    const text = value == null || value === '' ? '-' : String(value);
    return _jsx(Typography.Text, { style: compactCellTextStyle, title: text, children: text });
}
function renderDetailText(value) {
    const text = value == null || value === '' ? '-' : String(value);
    return _jsx(Typography.Text, { style: detailTextStyle, children: text });
}
function formatDateTimeText(value) {
    if (!value) {
        return '-';
    }
    const text = String(value).trim();
    if (!text || text.toLowerCase() === 'invalid date') {
        return '-';
    }
    return text.replace('T', ' ').replace(/\.\d{3}Z?$/, '');
}
function renderDateTime(value) {
    return renderCompactText(formatDateTimeText(value));
}
function getTableScrollX(columns) {
    return columns.reduce((total, column) => {
        if (column.hideInTable) {
            return total;
        }
        if (typeof column.width === 'number') {
            return total + column.width;
        }
        if (typeof column.width === 'string') {
            const parsedWidth = Number.parseInt(column.width, 10);
            return Number.isFinite(parsedWidth) ? total + parsedWidth : total + 180;
        }
        return total + 180;
    }, 0);
}
export function buildAuditParams(params, current, pageSize, partnerId, accountId, subjectField, accountField) {
    const { timeRange, ...rest } = params;
    const range = Array.isArray(timeRange) ? timeRange : [];
    const next = {
        ...rest,
        pageNum: current,
        pageSize,
    };
    if (partnerId) {
        next[subjectField] = partnerId;
        if (accountId) {
            next[accountField] = accountId;
        }
    }
    if (range[0]) {
        next['params[beginTime]'] = range[0];
    }
    if (range[1]) {
        next['params[endTime]'] = range[1];
    }
    return next;
}
const PartnerAuditModal = ({ config, open, partner, account, onOpenChange, }) => {
    const access = useAccess();
    const permPrefix = `${config.moduleKey}:admin`;
    const partnerId = getValue(partner, config.idField);
    const accountId = getAccountId(config, account);
    const partnerTitle = partnerId
        ? `${getValue(partner, config.nameField) || getValue(partner, config.noField) || partnerId}`
        : `全部${config.label}`;
    const accountTitle = accountId ? `${account?.userName || account?.nickName || accountId}` : undefined;
    const auditScopeKey = accountId ? `account:${accountId}` : partnerId ? `subject:${partnerId}` : 'all';
    const renderLoginDetail = (record) => (_jsxs(Descriptions, { size: "small", column: { xs: 1, sm: 2, md: 3 }, children: [_jsx(Descriptions.Item, { label: "\u767B\u5F55\u5730\u70B9", children: renderDetailText(record.loginLocation) }), _jsx(Descriptions.Item, { label: "\u6D4F\u89C8\u5668", children: renderDetailText(record.browser) }), _jsx(Descriptions.Item, { label: "\u64CD\u4F5C\u7CFB\u7EDF", children: renderDetailText(record.os) }), _jsx(Descriptions.Item, { label: "\u767B\u5F55\u63D0\u793A", span: 3, children: renderDetailText(record.msg) })] }));
    const renderOperDetail = (record) => (_jsxs(Descriptions, { size: "small", column: { xs: 1, sm: 2, md: 3 }, children: [_jsx(Descriptions.Item, { label: "\u8BF7\u6C42\u5730\u5740", children: renderDetailText(record.operUrl) }), _jsx(Descriptions.Item, { label: "\u64CD\u4F5CIP", children: renderDetailText(record.operIp) }), _jsx(Descriptions.Item, { label: "\u64CD\u4F5C\u5730\u70B9", children: renderDetailText(record.operLocation) }), _jsx(Descriptions.Item, { label: "\u65B9\u6CD5\u540D", span: 3, children: renderDetailText(record.method) }), _jsx(Descriptions.Item, { label: "\u5F02\u5E38\u4FE1\u606F", span: 3, children: renderDetailText(record.errorMsg) })] }));
    const renderTicketDetail = (record) => (_jsxs(Descriptions, { size: "small", column: { xs: 1, sm: 2, md: 3 }, children: [_jsx(Descriptions.Item, { label: "\u76EE\u6807\u7AEF", children: renderDetailText(record.terminal) }), _jsx(Descriptions.Item, { label: "\u7B7E\u53D1\u4EBAID", children: renderDetailText(record.actingAdminId) }), _jsx(Descriptions.Item, { label: "\u4F7F\u7528IP", children: renderDetailText(record.usedIp) }), _jsx(Descriptions.Item, { label: "\u521B\u5EFA\u4EBA", children: renderDetailText(record.createBy) }), _jsx(Descriptions.Item, { label: "\u66F4\u65B0\u4EBA", children: renderDetailText(record.updateBy) }), _jsx(Descriptions.Item, { label: "\u66F4\u65B0\u65F6\u95F4", children: renderDetailText(formatDateTimeText(record.updateTime)) }), _jsx(Descriptions.Item, { label: "\u4EE3\u5165\u539F\u56E0", span: 3, children: renderDetailText(record.reason) }), _jsx(Descriptions.Item, { label: "\u5907\u6CE8", span: 3, children: renderDetailText(record.remark) })] }));
    const loginLogColumns = [
        {
            title: `${config.label}ID`,
            dataIndex: 'subjectId',
            width: 90,
            search: partnerId ? false : undefined,
            render: (_, record) => renderCompactText(record.subjectId),
        },
        {
            title: '账号ID',
            dataIndex: 'accountId',
            width: 90,
            search: false,
            render: (_, record) => renderCompactText(record.accountId),
        },
        {
            title: '登录账号',
            dataIndex: 'userName',
            width: 140,
            search: accountId ? false : undefined,
            render: (_, record) => renderCompactText(record.userName),
        },
        {
            title: '登录IP',
            dataIndex: 'ipaddr',
            width: 130,
            render: (_, record) => renderCompactText(record.ipaddr),
        },
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: loginStatusValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 90,
        },
        {
            title: '登录时间',
            dataIndex: 'loginTime',
            search: false,
            width: 160,
            render: (_, record) => renderDateTime(record.loginTime),
        },
        {
            title: '登录时间',
            dataIndex: 'timeRange',
            colSize: 2,
            valueType: 'dateRange',
            hideInTable: true,
        },
        {
            title: '提示',
            dataIndex: 'msg',
            search: false,
            ellipsis: true,
            render: (_, record) => renderCompactText(record.msg),
        },
    ];
    const operLogColumns = [
        {
            title: `${config.label}ID`,
            dataIndex: 'subjectId',
            width: 90,
            search: partnerId ? false : undefined,
            render: (_, record) => renderCompactText(record.subjectId),
        },
        {
            title: '账号ID',
            dataIndex: 'accountId',
            width: 90,
            search: false,
            render: (_, record) => renderCompactText(record.accountId),
        },
        {
            title: '模块',
            dataIndex: 'title',
            width: 150,
            render: (_, record) => renderCompactText(record.title),
        },
        {
            title: '操作人',
            dataIndex: 'operName',
            width: 130,
            search: accountId ? false : undefined,
            render: (_, record) => renderCompactText(record.operName),
        },
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: operStatusValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 90,
        },
        {
            title: '请求方式',
            dataIndex: 'requestMethod',
            width: 100,
            search: false,
            render: (_, record) => renderCompactText(record.requestMethod),
        },
        {
            title: '操作时间',
            dataIndex: 'operTime',
            search: false,
            width: 160,
            render: (_, record) => renderDateTime(record.operTime),
        },
        {
            title: '操作时间',
            dataIndex: 'timeRange',
            colSize: 2,
            valueType: 'dateRange',
            hideInTable: true,
        },
        {
            title: '耗时',
            dataIndex: 'costTime',
            width: 90,
            search: false,
            render: (_, record) => (record.costTime == null ? '-' : `${record.costTime} ms`),
        },
    ];
    const ticketColumns = [
        {
            title: `${config.label}ID`,
            dataIndex: 'targetSubjectId',
            width: 90,
            search: partnerId ? false : undefined,
            render: (_, record) => renderCompactText(record.targetSubjectId),
        },
        {
            title: '内部编号',
            dataIndex: 'targetSubjectNo',
            width: 120,
            search: accountId ? false : undefined,
            render: (_, record) => renderCompactText(record.targetSubjectNo),
        },
        {
            title: '账号ID',
            dataIndex: 'targetAccountId',
            width: 90,
            search: false,
            render: (_, record) => renderCompactText(record.targetAccountId),
        },
        {
            title: '登录账号',
            dataIndex: 'targetUserName',
            width: 140,
            search: accountId ? false : undefined,
            render: (_, record) => renderCompactText(record.targetUserName),
        },
        {
            title: '签发人',
            dataIndex: 'actingAdminName',
            width: 120,
            render: (_, record) => renderCompactText(record.actingAdminName),
        },
        {
            title: '状态',
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: ticketStatusValueEnum,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            width: 100,
        },
        {
            title: '签发时间',
            dataIndex: 'createTime',
            search: false,
            width: 160,
            render: (_, record) => renderDateTime(record.createTime),
        },
        {
            title: '签发时间',
            dataIndex: 'timeRange',
            colSize: 2,
            valueType: 'dateRange',
            hideInTable: true,
        },
        {
            title: '有效期至',
            dataIndex: 'expireTime',
            search: false,
            width: 160,
            render: (_, record) => renderDateTime(record.expireTime),
        },
        {
            title: '使用时间',
            dataIndex: 'usedTime',
            search: false,
            width: 160,
            render: (_, record) => renderDateTime(record.usedTime),
        },
    ];
    const renderTable = (tableKey, columns, request, subjectField, accountField, expandedRowRender) => (_jsx("div", { style: auditTableWrapperStyle, children: _jsx(ProTable, { rowKey: (record) => String(record.infoId || record.operId || record.ticketId), columns: columns, search: getPersistedProTableSearch({ labelWidth: 88 }, `${config.moduleKey}:audit:${tableKey}:${auditScopeKey}`), tableLayout: "fixed", scroll: { x: getTableScrollX(columns) }, pagination: getProTablePagination(10), toolBarRender: false, expandable: {
                expandedRowRender,
            }, request: (params) => {
                const { current, pageSize, ...rest } = params;
                return request(buildAuditParams(rest, current, pageSize, partnerId, accountId, subjectField, accountField))
                    .then((res) => ({
                    data: res.rows || [],
                    total: res.total || 0,
                    success: res.code === 200,
                }));
            } }) }));
    const tabItems = [
        access.hasPerms(`${permPrefix}:loginLog:list`)
            ? {
                key: 'login',
                label: '登录日志',
                children: renderTable('login', loginLogColumns, config.services.listLoginLogs, 'subjectId', 'accountId', renderLoginDetail),
            }
            : null,
        access.hasPerms(`${permPrefix}:operLog:list`)
            ? {
                key: 'oper',
                label: '操作日志',
                children: renderTable('oper', operLogColumns, config.services.listOperLogs, 'subjectId', 'accountId', renderOperDetail),
            }
            : null,
        access.hasPerms(`${permPrefix}:ticket:list`)
            ? {
                key: 'ticket',
                label: '免密票据',
                children: renderTable('ticket', ticketColumns, config.services.listDirectLoginTickets, 'targetSubjectId', 'targetAccountId', renderTicketDetail),
            }
            : null,
    ].filter(Boolean);
    return (_jsxs(Modal, { width: auditModalWidth, title: `${config.label}审计 - ${partnerTitle}${accountTitle ? ` / ${accountTitle}` : ''}`, open: open, destroyOnHidden: true, footer: null, onCancel: () => onOpenChange(false), children: [partnerId ? (_jsxs(Space, { size: 8, style: { marginBottom: 12 }, children: [_jsx(Tag, { children: getValue(partner, config.noField) || '-' }), _jsx(Typography.Text, { children: getValue(partner, config.nameField) || '-' }), accountTitle ? _jsx(Tag, { children: accountTitle }) : null] })) : null, tabItems.length > 0 ? _jsx(Tabs, { items: tabItems, style: auditTabsStyle }) : _jsx(Typography.Text, { type: "secondary", children: "\u6682\u65E0\u5BA1\u8BA1\u6743\u9650" })] }));
};
export default PartnerAuditModal;
