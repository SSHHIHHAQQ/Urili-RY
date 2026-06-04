import React from 'react';
import { useAccess } from '@umijs/max';
import { Modal, Space, Tabs, Tag, Typography } from 'antd';
import { ProTable, type ProColumns } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import type { PartnerModuleConfig } from './PartnerManagementPage';

type PartnerRecord = Record<string, any>;
type AuditRecord = Record<string, any>;

type PartnerAuditModalProps = {
  config: PartnerModuleConfig;
  open: boolean;
  partner?: PartnerRecord;
  onOpenChange: (open: boolean) => void;
};

const compactCellTextStyle: React.CSSProperties = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  lineHeight: 1.35,
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

function renderCompactText(value: unknown) {
  const text = value == null || value === '' ? '-' : String(value);
  return <Typography.Text style={compactCellTextStyle} title={text}>{text}</Typography.Text>;
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

function buildAuditParams(
  params: Record<string, any>,
  current: number | undefined,
  pageSize: number | undefined,
  partnerId: number | undefined,
  subjectField: 'subjectId' | 'targetSubjectId',
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
  onOpenChange,
}) => {
  const access = useAccess();
  const permPrefix = `${config.moduleKey}:admin`;
  const partnerId = getValue(partner, config.idField);
  const partnerTitle = partnerId
    ? `${getValue(partner, config.nameField) || getValue(partner, config.noField) || partnerId}`
    : `全部${config.label}`;

  const loginLogColumns: ProColumns<AuditRecord>[] = [
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

  const operLogColumns: ProColumns<AuditRecord>[] = [
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
      render: (_, record) => renderCompactText(record.operName),
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueType: 'select',
      valueEnum: operStatusValueEnum,
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

  const ticketColumns: ProColumns<AuditRecord>[] = [
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

  const renderTable = (
    tableKey: string,
    columns: ProColumns<AuditRecord>[],
    request: (params?: Record<string, any>) => Promise<API.Partner.PortalAuditPageResult<any>>,
    subjectField: 'subjectId' | 'targetSubjectId',
  ) => (
    <ProTable<AuditRecord>
      rowKey={(record) => String(record.infoId || record.operId || record.ticketId)}
      columns={columns}
      search={getPersistedProTableSearch({ labelWidth: 88 }, `${config.moduleKey}:audit:${tableKey}`)}
      tableLayout="fixed"
      pagination={{ pageSize: 10 }}
      toolBarRender={false}
      request={(params) => {
        const { current, pageSize, ...rest } = params;
        return request(buildAuditParams(rest, current, pageSize, partnerId, subjectField))
          .then((res) => ({
            data: res.rows || [],
            total: res.total || 0,
            success: res.code === 200,
          }));
      }}
    />
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
          ),
        }
      : null,
  ].filter(Boolean);

  return (
    <Modal
      width={1120}
      title={`${config.label}审计 - ${partnerTitle}`}
      open={open}
      destroyOnHidden
      footer={null}
      onCancel={() => onOpenChange(false)}
    >
      {partnerId ? (
        <Space size={8} style={{ marginBottom: 12 }}>
          <Tag>{getValue(partner, config.noField) || '-'}</Tag>
          <Typography.Text>{getValue(partner, config.nameField) || '-'}</Typography.Text>
        </Space>
      ) : null}
      {tabItems.length > 0 ? <Tabs items={tabItems as any[]} /> : <Typography.Text type="secondary">暂无审计权限</Typography.Text>}
    </Modal>
  );
};

export default PartnerAuditModal;
