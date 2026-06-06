import React, { useCallback, useEffect, useRef, useState } from 'react';
import { App, Modal, Table, Tag, Typography } from 'antd';
import type { TablePaginationConfig } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { PartnerModuleConfig } from './PartnerManagementPage';

type PartnerRecord = Record<string, any>;
type AccountRecord = API.Partner.PortalAccountBase & Record<string, any>;

type PartnerSessionModalProps = {
  config: PartnerModuleConfig;
  open: boolean;
  partner?: PartnerRecord;
  account?: AccountRecord;
  onOpenChange: (open: boolean) => void;
};

const DEFAULT_PAGE_SIZE = 10;

const compactCellTextStyle: React.CSSProperties = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  lineHeight: 1.35,
};

function getValue(record: PartnerRecord | undefined, field: string) {
  return record ? record[field] : undefined;
}

function getAccountId(config: PartnerModuleConfig, account?: AccountRecord) {
  return account ? account[config.accountIdField] || account.accountId : undefined;
}

function displayText(value: unknown) {
  if (value === undefined || value === null) {
    return '-';
  }
  const text = String(value).trim();
  if (!text || text.toLowerCase() === 'invalid date') {
    return '-';
  }
  return text;
}

function renderCompactText(value: unknown) {
  const text = displayText(value);
  return <Typography.Text style={compactCellTextStyle} title={text}>{text}</Typography.Text>;
}

function isDirectLoginSession(record: API.Partner.PortalSessionProfile) {
  return record.directLogin === true || String(record.directLogin) === '1';
}

function renderDirectLoginAudit(record: API.Partner.PortalSessionProfile) {
  if (!isDirectLoginSession(record)) {
    return null;
  }
  const actor = displayText(record.actingAdminName || record.actingAdminId);
  const reason = displayText(record.directLoginReason);
  const title = reason === '-' ? `免密代入：${actor}` : `免密代入：${actor} / ${reason}`;
  return (
    <Typography.Text type="secondary" style={compactCellTextStyle} title={title}>
      免密：{actor}
    </Typography.Text>
  );
}

function renderSessionStatus(record: API.Partner.PortalSessionProfile) {
  let statusTag: React.ReactNode;
  if (record.current) {
    statusTag = <Tag color="processing">当前</Tag>;
  } else if (record.logoutTime || record.status === '1') {
    statusTag = <Tag>已退出</Tag>;
  } else if (record.status === '2') {
    statusTag = <Tag color="default">已过期</Tag>;
  } else if (record.status === '0') {
    statusTag = <Tag color="success">有效</Tag>;
  } else {
    statusTag = <Tag>{displayText(record.status)}</Tag>;
  }
  return (
    <>
      {statusTag}
      {renderDirectLoginAudit(record)}
    </>
  );
}

const sessionColumns: ColumnsType<API.Partner.PortalSessionProfile> = [
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

const PartnerSessionModal: React.FC<PartnerSessionModalProps> = ({
  config,
  open,
  partner,
  account,
  onOpenChange,
}) => {
  const { message } = App.useApp();
  const requestSeq = useRef(0);
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<API.Partner.PortalSessionProfile[]>([]);
  const [pagination, setPagination] = useState<TablePaginationConfig>({
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
    } catch {
      if (requestSeq.current === currentSeq) {
        setRows([]);
        setPagination((current) => ({ ...current, current: pageNum, pageSize, total: 0 }));
        message.error('会话列表加载失败，请重试');
      }
    } finally {
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

  return (
    <Modal
      width={960}
      title={title}
      open={open}
      destroyOnHidden
      footer={null}
      onCancel={() => onOpenChange(false)}
    >
      <Table<API.Partner.PortalSessionProfile>
        rowKey={(record) => record.tokenId || [
          record.terminal || config.moduleKey,
          record.subjectId || 0,
          record.accountId || 0,
          record.loginTime || '',
          record.expireTime || '',
          record.logoutTime || '',
          record.status || '',
        ].join('-')}
        loading={loading}
        columns={sessionColumns}
        dataSource={rows}
        size="small"
        tableLayout="fixed"
        pagination={pagination}
        locale={{ emptyText: '暂无会话' }}
        onChange={(nextPagination) => {
          void loadSessions(
            Number(nextPagination.current) || 1,
            Number(nextPagination.pageSize) || DEFAULT_PAGE_SIZE,
          );
        }}
      />
    </Modal>
  );
};

export default PartnerSessionModal;
