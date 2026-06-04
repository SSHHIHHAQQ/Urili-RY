import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  MenuOutlined,
  PlusOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import { Button, Empty, Input, Spin, Tooltip, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { systemKindText } from '../constants';
import { statusTag } from '../helpers';
import styles from '../style.module.css';

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
        className={`${styles.connectionItem} ${
          active ? styles.connectionItemActive : ''
        }`}
        onClick={() => onSelect(connection)}
      >
        <div className={styles.connectionItemMain}>
          <div className={styles.connectionItemTitle}>
            <Typography.Text strong ellipsis>
              {connection.masterWarehouseName}
            </Typography.Text>
          </div>
          <div className={styles.connectionItemCode}>
            <Typography.Text type="secondary" ellipsis>
              {connection.connectionCode}
            </Typography.Text>
          </div>
          {statusTag(connection.status)}
        </div>
        {ordering ? (
          <div className={styles.connectionOrderActions}>
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
          </div>
        ) : null}
      </div>
    );
  };

  return (
    <div className={styles.sidebar}>
      <div className={styles.sidebarHeader}>
        <div className={styles.sidebarTitleLine}>
          <Typography.Text strong>主仓接入</Typography.Text>
          <div className={styles.sidebarActions}>
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
          </div>
        </div>
      </div>
      <div className={styles.sidebarSearch}>
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
      </div>
      <div className={styles.sidebarBody}>
        <Spin spinning={loading}>
          {groups.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="暂无主仓"
            />
          ) : (
            <div>
              {groups.map(([groupName, rows]) => (
                <div className={styles.connectionGroup} key={groupName}>
                  <Typography.Text
                    type="secondary"
                    className={styles.connectionGroupLabel}
                  >
                    {groupName}
                  </Typography.Text>
                  {rows.map(renderConnection)}
                </div>
              ))}
            </div>
          )}
        </Spin>
      </div>
      <div className={styles.sidebarFooter}>
        <Typography.Text type="secondary">
          共 {visibleConnections.length} 个主仓
        </Typography.Text>
        {ordering ? (
          <Typography.Text type="secondary">排序中</Typography.Text>
        ) : null}
      </div>
    </div>
  );
}
