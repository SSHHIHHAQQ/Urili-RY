import {
  CheckCircleOutlined,
  DownOutlined,
  EditOutlined,
  KeyOutlined,
  SafetyCertificateOutlined,
  StopOutlined,
  SyncOutlined,
  UpOutlined,
} from '@ant-design/icons';
import { Button, Popconfirm, Tooltip, Typography } from 'antd';
import { type ReactNode, useEffect, useState } from 'react';
import { settlementTypeText, systemKindText } from '../constants';
import { statusTag } from '../helpers';
import styles from '../style.module.css';

type ConnectionSummaryProps = {
  access: { hasPerms: (permission: string) => boolean };
  connection: API.Integration.UpstreamConnection;
  onAuthorize: () => void;
  onCredential: () => void;
  onEdit: () => void;
  onSync: () => void;
  onToggleStatus: () => void;
};

const settlementText = (value?: string) =>
  settlementTypeText[value || ''] || value || '-';

const manualSyncEntryPermissions = [
  'integration:upstream:sync',
  'integration:upstream:dimensionSync',
  'integration:upstream:inventorySync',
];

type SummaryItemProps = {
  label: string;
  value: ReactNode;
  wide?: boolean;
};

function SummaryItem({ label, value, wide }: SummaryItemProps) {
  return (
    <div
      className={`${styles.summaryItem} ${wide ? styles.summaryItemWide : ''}`}
    >
      <span className={styles.summaryLabel}>{label}</span>
      <Typography.Text ellipsis className={styles.summaryValue}>
        <span className={styles.summaryValueText}>{value ?? '-'}</span>
      </Typography.Text>
    </div>
  );
}

export default function ConnectionSummary({
  access,
  connection,
  onAuthorize,
  onCredential,
  onEdit,
  onSync,
  onToggleStatus,
}: ConnectionSummaryProps) {
  const [detailsOpen, setDetailsOpen] = useState(false);
  const systemKind =
    systemKindText[connection.systemKind || ''] || connection.systemKind || '-';
  const canOpenSync = manualSyncEntryPermissions.some((permission) =>
    access.hasPerms(permission),
  );

  useEffect(() => {
    setDetailsOpen(false);
  }, [connection.connectionCode]);

  return (
    <div className={styles.summaryPanel}>
      <div className={styles.summaryHeader}>
        <div className={styles.summaryTitleBlock}>
          <div className={styles.summaryTitleLine}>
            <Typography.Title level={4} className={styles.summaryTitle}>
              {connection.masterWarehouseName}
            </Typography.Title>
            {statusTag(connection.status)}
          </div>
          <Typography.Text type="secondary" className={styles.summarySubtitle}>
            {connection.connectionCode} · {systemKind}
          </Typography.Text>
          <Button
            className={styles.summaryDetailToggle}
            icon={detailsOpen ? <UpOutlined /> : <DownOutlined />}
            size="small"
            type="link"
            onClick={() => setDetailsOpen((open) => !open)}
          >
            {detailsOpen ? '收起详情' : '展开详情'}
          </Button>
        </div>
        <div className={styles.summaryActions}>
          <Tooltip title="编辑主仓资料">
            <Button
              icon={<EditOutlined />}
              hidden={!access.hasPerms('integration:upstream:edit')}
              onClick={onEdit}
            >
              编辑
            </Button>
          </Tooltip>
          <Tooltip title="选择同步内容">
            <Button
              icon={<SyncOutlined />}
              hidden={!canOpenSync}
              onClick={onSync}
            >
              同步
            </Button>
          </Tooltip>
          <Tooltip title="校验当前授权是否可用">
            <Button
              icon={<SafetyCertificateOutlined />}
              hidden={!access.hasPerms('integration:upstream:sync')}
              onClick={onAuthorize}
            >
              校验授权
            </Button>
          </Tooltip>
          <Tooltip title="更新 Key 和 Secret">
            <Button
              icon={<KeyOutlined />}
              hidden={!access.hasPerms('integration:upstream:credential')}
              onClick={onCredential}
            >
              重新授权
            </Button>
          </Tooltip>
          <Popconfirm
            title={
              connection.status === 'ENABLED'
                ? '确认停用该主仓接入？'
                : '确认启用该主仓接入？'
            }
            onConfirm={onToggleStatus}
          >
            <Button
              danger={connection.status === 'ENABLED'}
              icon={
                connection.status === 'ENABLED' ? (
                  <StopOutlined />
                ) : (
                  <CheckCircleOutlined />
                )
              }
              hidden={!access.hasPerms('integration:upstream:edit')}
            >
              {connection.status === 'ENABLED' ? '停用' : '启用'}
            </Button>
          </Popconfirm>
        </div>
      </div>
      {detailsOpen ? (
        <div className={styles.summaryGrid}>
          <SummaryItem
            label="授权状态"
            value={statusTag(
              connection.credentialStatus ||
                (connection.appKeyMask ? 'CONFIGURED' : undefined),
            )}
          />
          <SummaryItem
            label="最近同步"
            value={connection.lastSyncTime || '-'}
          />
          <SummaryItem
            label="最近授权"
            value={connection.lastAuthorizedTime || '-'}
          />
          <SummaryItem
            label="请求日志数"
            value={connection.requestLogCount ?? 0}
          />
          <SummaryItem
            label="结算类型"
            value={settlementText(connection.settlementType)}
          />
          <SummaryItem label="Key" value={connection.appKeyMask || '-'} />
          <SummaryItem label="备注" value={connection.remark || '-'} wide />
        </div>
      ) : null}
    </div>
  );
}
