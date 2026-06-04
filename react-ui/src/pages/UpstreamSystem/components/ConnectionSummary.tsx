import {
  CheckCircleOutlined,
  EditOutlined,
  KeyOutlined,
  SafetyCertificateOutlined,
  StopOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { Button, Descriptions, Popconfirm, Space, Typography } from 'antd';
import { settlementOptions, systemKindText } from '../constants';
import { statusTag } from '../helpers';

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
  settlementOptions.find((item) => item.value === value)?.label || value || '-';

export default function ConnectionSummary({
  access,
  connection,
  onAuthorize,
  onCredential,
  onEdit,
  onSync,
  onToggleStatus,
}: ConnectionSummaryProps) {
  return (
    <div
      style={{
        background: '#fff',
        border: '1px solid #f0f0f0',
        borderRadius: 6,
        padding: 16,
      }}
    >
      <Space
        align="start"
        style={{
          width: '100%',
          justifyContent: 'space-between',
          marginBottom: 12,
        }}
      >
        <Space direction="vertical" size={2}>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {connection.masterWarehouseName}
          </Typography.Title>
          <Typography.Text type="secondary">
            {connection.connectionCode}
          </Typography.Text>
        </Space>
        <Space wrap>
          <Button
            icon={<EditOutlined />}
            hidden={!access.hasPerms('integration:upstream:edit')}
            onClick={onEdit}
          >
            编辑
          </Button>
          <Button
            icon={<SyncOutlined />}
            hidden={!access.hasPerms('integration:upstream:sync')}
            onClick={onSync}
          >
            同步
          </Button>
          <Button
            icon={<SafetyCertificateOutlined />}
            hidden={!access.hasPerms('integration:upstream:sync')}
            onClick={onAuthorize}
          >
            校验授权
          </Button>
          <Button
            icon={<KeyOutlined />}
            hidden={!access.hasPerms('integration:upstream:credential')}
            onClick={onCredential}
          >
            重新授权
          </Button>
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
        </Space>
      </Space>
      <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3, xl: 4 }}>
        <Descriptions.Item label="主仓类型">
          {systemKindText[connection.systemKind || ''] ||
            connection.systemKind ||
            '-'}
        </Descriptions.Item>
        <Descriptions.Item label="接入状态">
          {statusTag(connection.status)}
        </Descriptions.Item>
        <Descriptions.Item label="授权状态">
          {statusTag(
            connection.credentialStatus ||
              (connection.appKeyMask ? 'CONFIGURED' : undefined),
          )}
        </Descriptions.Item>
        <Descriptions.Item label="结算类型">
          {settlementText(connection.settlementType)}
        </Descriptions.Item>
        <Descriptions.Item label="最近授权">
          {connection.lastAuthorizedTime || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="最近同步">
          {connection.lastSyncTime || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="请求日志数">
          {connection.requestLogCount ?? 0}
        </Descriptions.Item>
        <Descriptions.Item label="Key">
          {connection.appKeyMask || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="备注" span={2}>
          {connection.remark || '-'}
        </Descriptions.Item>
      </Descriptions>
    </div>
  );
}
