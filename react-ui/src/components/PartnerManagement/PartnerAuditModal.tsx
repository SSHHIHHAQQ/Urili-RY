import { ProTable, type ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Descriptions, Modal, Space, Tabs, Tag, Typography } from 'antd';
import React from 'react';
import { getPersistedProTableSearch, getProTablePagination } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import type { PartnerModuleConfig } from './PartnerManagementPage';

type PartnerRecord = Record<string, any>;
type AccountRecord = API.Partner.PortalAccountBase & Record<string, any>;
type LoginAuditRecord = API.Partner.PortalLoginLog;
type OperAuditRecord = API.Partner.PortalOperLog;
type TicketAuditRecord = API.Partner.PortalDirectLoginTicket;
type AuditRecord = LoginAuditRecord | OperAuditRecord | TicketAuditRecord;

type PartnerAuditModalProps = {
  config: PartnerModuleConfig;
  open: boolean;
  partner?: PartnerRecord;
  account?: AccountRecord;
  onOpenChange: (open: boolean) => void;
};

const compactCellTextStyle: React.CSSProperties = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  lineHeight: 1.35,
};

const auditModalWidth = 'min(1480px, calc(100vw - 64px))';

const auditTabsStyle: React.CSSProperties = {
  minWidth: 0,
};

const auditTableWrapperStyle: React.CSSProperties = {
  minWidth: 0,
  overflow: 'hidden',
};

const detailTextStyle: React.CSSProperties = {
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

function getValue(record: PartnerRecord | undefined, field: string) {
  return record ? record[field] : undefined;
}

function getAccountId(config: PartnerModuleConfig, account?: AccountRecord) {
  return account ? account[config.accountIdField] || account.accountId : undefined;
}

function renderCompactText(value: unknown) {
  const text = value == null || value === '' ? '-' : String(value);
  return <Typography.Text style={compactCellTextStyle} title={text}>{text}</Typography.Text>;
}

function renderDetailText(value: unknown) {
  const text = value == null || value === '' ? '-' : String(value);
  return <Typography.Text style={detailTextStyle}>{text}</Typography.Text>;
}

function formatDateTimeText(value: unknown) {
  if (!value) {
    return '-';
  }
  const text = String(value).trim();
  if (!text || text.toLowerCase() === 'invalid date') {
    return '-';
  }
  return text.replace('T', ' ').replace(/\.\d{3}Z?$/, '');
}

function renderDateTime(value: unknown) {
  return renderCompactText(formatDateTimeText(value));
}

function getTableScrollX<T extends AuditRecord>(columns: ProColumns<T>[]) {
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

function getAuditRowKey(record: AuditRecord) {
  if ('infoId' in record && record.infoId) {
    return `login-${record.infoId}`;
  }
  if ('operId' in record && record.operId) {
    return `oper-${record.operId}`;
  }
  if ('ticketId' in record && record.ticketId) {
    return `ticket-${record.ticketId}`;
  }
  return JSON.stringify(record);
}

export function buildAuditParams(
  params: Record<string, any>,
  current: number | undefined,
  pageSize: number | undefined,
  partnerId: number | undefined,
  accountId: number | undefined,
  subjectField: 'subjectId' | 'targetSubjectId',
  accountField: 'accountId' | 'targetAccountId',
) {
  const { timeRange, ...rest } = params;
  const range = Array.isArray(timeRange) ? timeRange : [];
  const next: Record<string, any> = {
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

const PartnerAuditModal: React.FC<PartnerAuditModalProps> = ({
  config,
  open,
  partner,
  account,
  onOpenChange,
}) => {
  const access = useAccess();
  const permPrefix = `${config.moduleKey}:admin`;
  const partnerId = getValue(partner, config.idField);
  const accountId = getAccountId(config, account);
  const partnerTitle = partnerId
    ? `${getValue(partner, config.nameField) || getValue(partner, config.noField) || partnerId}`
    : `全部${config.label}`;
  const accountTitle = accountId ? `${account?.userName || account?.nickName || accountId}` : undefined;
  const auditScopeKey = accountId ? `account:${accountId}` : partnerId ? `subject:${partnerId}` : 'all';

  const renderLoginDetail = (record: LoginAuditRecord) => (
    <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }}>
      <Descriptions.Item label="登录地点">{renderDetailText(record.loginLocation)}</Descriptions.Item>
      <Descriptions.Item label="浏览器">{renderDetailText(record.browser)}</Descriptions.Item>
      <Descriptions.Item label="操作系统">{renderDetailText(record.os)}</Descriptions.Item>
      <Descriptions.Item label="登录提示" span={3}>{renderDetailText(record.msg)}</Descriptions.Item>
    </Descriptions>
  );

  const renderOperDetail = (record: OperAuditRecord) => (
    <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }}>
      <Descriptions.Item label="请求地址">{renderDetailText(record.operUrl)}</Descriptions.Item>
      <Descriptions.Item label="操作IP">{renderDetailText(record.operIp)}</Descriptions.Item>
      <Descriptions.Item label="操作地点">{renderDetailText(record.operLocation)}</Descriptions.Item>
      <Descriptions.Item label="方法名" span={3}>{renderDetailText(record.method)}</Descriptions.Item>
      <Descriptions.Item label="异常信息" span={3}>{renderDetailText(record.errorMsg)}</Descriptions.Item>
    </Descriptions>
  );

  const renderTicketDetail = (record: TicketAuditRecord) => (
    <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }}>
      <Descriptions.Item label="目标端">{renderDetailText(record.terminal)}</Descriptions.Item>
      <Descriptions.Item label="签发人ID">{renderDetailText(record.actingAdminId)}</Descriptions.Item>
      <Descriptions.Item label="使用IP">{renderDetailText(record.usedIp)}</Descriptions.Item>
      <Descriptions.Item label="创建人">{renderDetailText(record.createBy)}</Descriptions.Item>
      <Descriptions.Item label="更新人">{renderDetailText(record.updateBy)}</Descriptions.Item>
      <Descriptions.Item label="更新时间">{renderDetailText(formatDateTimeText(record.updateTime))}</Descriptions.Item>
      <Descriptions.Item label="代入原因" span={3}>{renderDetailText(record.reason)}</Descriptions.Item>
      <Descriptions.Item label="备注" span={3}>{renderDetailText(record.remark)}</Descriptions.Item>
    </Descriptions>
  );

  const loginLogColumns: ProColumns<LoginAuditRecord>[] = [
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

  const operLogColumns: ProColumns<OperAuditRecord>[] = [
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

  const ticketColumns: ProColumns<TicketAuditRecord>[] = [
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

  const renderTable = <T extends AuditRecord,>(
    tableKey: string,
    columns: ProColumns<T>[],
    request: (params?: Record<string, any>) => Promise<API.Partner.PortalAuditPageResult<T>>,
    subjectField: 'subjectId' | 'targetSubjectId',
    accountField: 'accountId' | 'targetAccountId',
    expandedRowRender: (record: T) => React.ReactNode,
  ) => (
    <div style={auditTableWrapperStyle}>
      <ProTable<T>
        rowKey={(record) => getAuditRowKey(record)}
        columns={columns}
        search={getPersistedProTableSearch({ labelWidth: 88 }, `${config.moduleKey}:audit:${tableKey}:${auditScopeKey}`)}
        tableLayout="fixed"
        scroll={{ x: getTableScrollX(columns) }}
        pagination={getProTablePagination(10)}
        toolBarRender={false}
        expandable={{
          expandedRowRender,
        }}
        request={(params) => {
          const { current, pageSize, ...rest } = params;
          return request(buildAuditParams(rest, current, pageSize, partnerId, accountId, subjectField, accountField))
            .then((res) => ({
              data: res.rows || [],
              total: res.total || 0,
              success: res.code === 200,
            }));
        }}
      />
    </div>
  );

  const tabItems = [
    access.hasPerms(`${permPrefix}:loginLog:list`)
      ? {
          key: 'login',
          label: '登录日志',
          children: renderTable(
            'login',
            loginLogColumns,
            config.services.listLoginLogs,
            'subjectId',
            'accountId',
            renderLoginDetail,
          ),
        }
      : null,
    access.hasPerms(`${permPrefix}:operLog:list`)
      ? {
          key: 'oper',
          label: '操作日志',
          children: renderTable(
            'oper',
            operLogColumns,
            config.services.listOperLogs,
            'subjectId',
            'accountId',
            renderOperDetail,
          ),
        }
      : null,
    access.hasPerms(`${permPrefix}:ticket:list`)
      ? {
          key: 'ticket',
          label: '免密票据',
          children: renderTable(
            'ticket',
            ticketColumns,
            config.services.listDirectLoginTickets,
            'targetSubjectId',
            'targetAccountId',
            renderTicketDetail,
          ),
        }
      : null,
  ].filter(Boolean);

  return (
    <Modal
      width={auditModalWidth}
      title={`${config.label}审计 - ${partnerTitle}${accountTitle ? ` / ${accountTitle}` : ''}`}
      open={open}
      destroyOnHidden
      footer={null}
      onCancel={() => onOpenChange(false)}
    >
      {partnerId ? (
        <Space size={8} style={{ marginBottom: 12 }}>
          <Tag>{getValue(partner, config.noField) || '-'}</Tag>
          <Typography.Text>{getValue(partner, config.nameField) || '-'}</Typography.Text>
          {accountTitle ? <Tag>{accountTitle}</Tag> : null}
        </Space>
      ) : null}
      {tabItems.length > 0 ? <Tabs items={tabItems as any[]} style={auditTabsStyle} /> : <Typography.Text type="secondary">暂无审计权限</Typography.Text>}
    </Modal>
  );
};

export default PartnerAuditModal;
