import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  MenuOutlined,
  PlusOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import { Button, Empty, Input, Space, Spin, Tooltip, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { systemKindText } from '../constants';
import { statusTag } from '../helpers';

type ConnectionSidebarProps = {
  access: { hasPerms: (permission: string) => boolean };
  connections: API.Integration.UpstreamConnection[];
  loading?: boolean;
  selectedCode?: string;
  onCreate: () => void;
  onSaveOrder: (connectionCodes: string[]) => Promise<boolean>;
  onSelect: (connection: API.Integration.UpstreamConnection) => void;
};

export default function ConnectionSidebar({
  access,
  connections,
  loading,
  selectedCode,
  onCreate,
  onSaveOrder,
  onSelect,
}: ConnectionSidebarProps) {
  const [keyword, setKeyword] = useState('');
  const [ordering, setOrdering] = useState(false);
  const [draftConnections, setDraftConnections] = useState<
    API.Integration.UpstreamConnection[]
  >([]);

  useEffect(() => {
    if (!ordering) {
      setDraftConnections(connections);
    }
  }, [connections, ordering]);

  const visibleConnections = useMemo(() => {
    const rows = ordering ? draftConnections : connections;
    const normalizedKeyword = keyword.trim().toLowerCase();
    if (!normalizedKeyword || ordering) {
      return rows;
    }
    return rows.filter((item) =>
      [
        item.masterWarehouseName,
        item.connectionCode,
        systemKindText[item.systemKind || ''] || item.systemKind,
      ]
        .filter(Boolean)
        .some((value) =>
          String(value).toLowerCase().includes(normalizedKeyword),
        ),
    );
  }, [connections, draftConnections, keyword, ordering]);

  const groups = useMemo(() => {
    const result = new Map<string, API.Integration.UpstreamConnection[]>();
    visibleConnections.forEach((item) => {
      const groupName =
        systemKindText[item.systemKind || ''] || item.systemKind || '其他系统';
      result.set(groupName, [...(result.get(groupName) || []), item]);
    });
    return Array.from(result.entries());
  }, [visibleConnections]);

  const moveConnection = (connectionCode: string, offset: number) => {
    const currentIndex = draftConnections.findIndex(
      (item) => item.connectionCode === connectionCode,
    );
    const nextIndex = currentIndex + offset;
    if (
      currentIndex < 0 ||
      nextIndex < 0 ||
      nextIndex >= draftConnections.length
    ) {
      return;
    }
    const nextRows = [...draftConnections];
    const [current] = nextRows.splice(currentIndex, 1);
    nextRows.splice(nextIndex, 0, current);
    setDraftConnections(nextRows);
  };

  const renderConnection = (connection: API.Integration.UpstreamConnection) => {
    const active = selectedCode === connection.connectionCode;
    const draftIndex = draftConnections.findIndex(
      (item) => item.connectionCode === connection.connectionCode,
    );
    return (
      <div
        key={connection.connectionCode}
        onClick={() => onSelect(connection)}
        style={{
          borderRadius: 6,
          cursor: 'pointer',
          marginBottom: 4,
          padding: '8px 10px',
          background: active ? '#e6f4ff' : 'transparent',
          border: active ? '1px solid #91caff' : '1px solid transparent',
        }}
      >
        <Space
          align="start"
          style={{ width: '100%', justifyContent: 'space-between' }}
        >
          <Space direction="vertical" size={2} style={{ minWidth: 0 }}>
            <Typography.Text strong ellipsis style={{ maxWidth: 170 }}>
              {connection.masterWarehouseName}
            </Typography.Text>
            <Typography.Text
              type="secondary"
              ellipsis
              style={{ maxWidth: 170, fontSize: 12 }}
            >
              {connection.connectionCode}
            </Typography.Text>
            {statusTag(connection.status)}
          </Space>
          {ordering ? (
            <Space size={0}>
              <Tooltip title="上移">
                <Button
                  type="text"
                  size="small"
                  icon={<ArrowUpOutlined />}
                  disabled={draftIndex <= 0}
                  onClick={(event) => {
                    event.stopPropagation();
                    moveConnection(connection.connectionCode, -1);
                  }}
                />
              </Tooltip>
              <Tooltip title="下移">
                <Button
                  type="text"
                  size="small"
                  icon={<ArrowDownOutlined />}
                  disabled={
                    draftIndex < 0 || draftIndex >= draftConnections.length - 1
                  }
                  onClick={(event) => {
                    event.stopPropagation();
                    moveConnection(connection.connectionCode, 1);
                  }}
                />
              </Tooltip>
            </Space>
          ) : null}
        </Space>
      </div>
    );
  };

  return (
    <div
      style={{
        background: '#fff',
        border: '1px solid #f0f0f0',
        borderRadius: 6,
        padding: 12,
      }}
    >
      <Space direction="vertical" size={10} style={{ width: '100%' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
          <Typography.Text strong>主仓接入</Typography.Text>
          <Space size={4}>
            <Tooltip title={ordering ? '保存排序' : '调整排序'}>
              <Button
                icon={ordering ? <SaveOutlined /> : <MenuOutlined />}
                size="small"
                hidden={!access.hasPerms('integration:upstream:edit')}
                onClick={async () => {
                  if (!ordering) {
                    setOrdering(true);
                    setDraftConnections(connections);
                    return;
                  }
                  const ok = await onSaveOrder(
                    draftConnections.map((item) => item.connectionCode),
                  );
                  if (ok) {
                    setOrdering(false);
                  }
                }}
              />
            </Tooltip>
            <Tooltip title="新增主仓">
              <Button
                type="primary"
                icon={<PlusOutlined />}
                size="small"
                hidden={!access.hasPerms('integration:upstream:add')}
                onClick={onCreate}
              />
            </Tooltip>
          </Space>
        </Space>
        {ordering ? (
          <Button
            block
            size="small"
            onClick={() => {
              setOrdering(false);
              setDraftConnections(connections);
            }}
          >
            退出排序
          </Button>
        ) : (
          <Input.Search
            allowClear
            placeholder="搜索主仓名称/编号"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
        )}
        <Spin spinning={loading}>
          {groups.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="暂无主仓"
            />
          ) : (
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              {groups.map(([groupName, rows]) => (
                <div key={groupName}>
                  <Typography.Text
                    type="secondary"
                    style={{ display: 'block', fontSize: 12, margin: '6px 0' }}
                  >
                    {groupName}
                  </Typography.Text>
                  {rows.map(renderConnection)}
                </div>
              ))}
            </Space>
          )}
        </Spin>
      </Space>
    </div>
  );
}
