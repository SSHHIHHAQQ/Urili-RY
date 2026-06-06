import { jsx as _jsx } from "react/jsx-runtime";
import { useCallback, useEffect, useRef, useState } from 'react';
import { App, Modal, Table, Tag, Typography } from 'antd';
const DEFAULT_PAGE_SIZE = 10;
const compactCellTextStyle = {
    display: 'block',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    lineHeight: 1.35,
};
function getValue(record, field) {
    return record ? record[field] : undefined;
}
function getAccountId(config, account) {
    return account ? account[config.accountIdField] || account.accountId : undefined;
}
function displayText(value) {
    if (value === undefined || value === null) {
        return '-';
    }
    const text = String(value).trim();
    if (!text || text.toLowerCase() === 'invalid date') {
        return '-';
    }
    return text;
}
function renderCompactText(value) {
    const text = displayText(value);
    return _jsx(Typography.Text, { style: compactCellTextStyle, title: text, children: text });
}
function renderSessionStatus(record) {
    if (record.current) {
        return _jsx(Tag, { color: "processing", children: "\u5F53\u524D" });
    }
    if (record.logoutTime || record.status === '1') {
        return _jsx(Tag, { children: "\u5DF2\u9000\u51FA" });
    }
    if (record.status === '0') {
        return _jsx(Tag, { color: "success", children: "\u6709\u6548" });
    }
    return _jsx(Tag, { children: displayText(record.status) });
}
const sessionColumns = [
    {
        title: '状态',
        key: 'status',
        width: 96,
        render: (_, record) => renderSessionStatus(record),
    },
    {
        title: '登录账号',
        dataIndex: 'userName',
        key: 'userName',
        ellipsis: true,
        render: renderCompactText,
    },
    {
        title: '登录 IP',
        dataIndex: 'loginIp',
        key: 'loginIp',
        width: 140,
        responsive: ['sm'],
        render: renderCompactText,
    },
    {
        title: '登录时间',
        dataIndex: 'loginTime',
        key: 'loginTime',
        width: 180,
        render: renderCompactText,
    },
    {
        title: '过期时间',
        dataIndex: 'expireTime',
        key: 'expireTime',
        width: 180,
        responsive: ['md'],
        render: renderCompactText,
    },
    {
        title: '退出时间',
        dataIndex: 'logoutTime',
        key: 'logoutTime',
        width: 180,
        responsive: ['md'],
        render: renderCompactText,
    },
];
const PartnerSessionModal = ({ config, open, partner, account, onOpenChange, }) => {
    const { message } = App.useApp();
    const requestSeq = useRef(0);
    const [loading, setLoading] = useState(false);
    const [rows, setRows] = useState([]);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: DEFAULT_PAGE_SIZE,
        total: 0,
        showSizeChanger: true,
    });
    const partnerId = Number(getValue(partner, config.idField) || 0);
    const accountId = Number(getAccountId(config, account) || 0);
    const partnerTitle = getValue(partner, config.nameField)
        || getValue(partner, config.codeField)
        || getValue(partner, config.noField)
        || '-';
    const accountTitle = account?.userName || account?.nickName || accountId || '-';
    const title = account
        ? `${config.label}账号会话 - ${accountTitle}`
        : `${config.label}会话 - ${partnerTitle}`;
    const loadSessions = useCallback(async (pageNum = 1, pageSize = DEFAULT_PAGE_SIZE) => {
        if (!partnerId) {
            setRows([]);
            setPagination((current) => ({ ...current, current: pageNum, pageSize, total: 0 }));
            return;
        }
        const currentSeq = requestSeq.current + 1;
        requestSeq.current = currentSeq;
        setLoading(true);
        try {
            const response = accountId
                ? await config.services.listAccountSessions?.(partnerId, accountId, { pageNum, pageSize })
                : await config.services.listSubjectSessions?.(partnerId, { pageNum, pageSize });
            if (!response) {
                setRows([]);
                setPagination((current) => ({ ...current, current: pageNum, pageSize, total: 0 }));
                return;
            }
            if (requestSeq.current === currentSeq) {
                setRows(response.rows || []);
                setPagination((current) => ({
                    ...current,
                    current: pageNum,
                    pageSize,
                    total: response.total || 0,
                }));
            }
        }
        catch {
            if (requestSeq.current === currentSeq) {
                setRows([]);
                setPagination((current) => ({ ...current, current: pageNum, pageSize, total: 0 }));
                message.error('会话列表加载失败，请重试');
            }
        }
        finally {
            if (requestSeq.current === currentSeq) {
                setLoading(false);
            }
        }
    }, [accountId, config.services, message, partnerId]);
    useEffect(() => {
        if (open) {
            void loadSessions(1, DEFAULT_PAGE_SIZE);
            return;
        }
        setRows([]);
        setPagination({
            current: 1,
            pageSize: DEFAULT_PAGE_SIZE,
            total: 0,
            showSizeChanger: true,
        });
    }, [loadSessions, open]);
    return (_jsx(Modal, { width: 960, title: title, open: open, destroyOnHidden: true, footer: null, onCancel: () => onOpenChange(false), children: _jsx(Table, { rowKey: (record) => [
                record.terminal || config.moduleKey,
                record.subjectId || 0,
                record.accountId || 0,
                record.loginTime || '',
                record.expireTime || '',
                record.logoutTime || '',
                record.status || '',
            ].join('-'), loading: loading, columns: sessionColumns, dataSource: rows, size: "small", tableLayout: "fixed", pagination: pagination, locale: { emptyText: '暂无会话' }, onChange: (nextPagination) => {
                void loadSessions(Number(nextPagination.current) || 1, Number(nextPagination.pageSize) || DEFAULT_PAGE_SIZE);
            } }) }));
};
export default PartnerSessionModal;
