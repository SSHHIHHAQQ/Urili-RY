import React, { useEffect, useMemo, useState } from 'react';
import { App, Checkbox, Empty, Flex, Modal, Spin, Typography } from 'antd';
import type { PartnerModuleConfig } from './PartnerManagementPage';

type AccountRecord = API.Partner.PortalAccountBase & Record<string, any>;

type PartnerAccountRoleModalProps = {
  config: PartnerModuleConfig;
  partnerId: number;
  account?: AccountRecord;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

function getAccountId(config: PartnerModuleConfig, account?: AccountRecord) {
  return account ? account[config.accountIdField] || account.accountId : undefined;
}

function normalizeRoleIds(roleIds?: Array<string | number | boolean>) {
  return (roleIds || [])
    .map((roleId) => Number(roleId))
    .filter((roleId) => Number.isFinite(roleId));
}

const PartnerAccountRoleModal: React.FC<PartnerAccountRoleModalProps> = ({
  config,
  partnerId,
  account,
  open,
  onOpenChange,
}) => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [roles, setRoles] = useState<API.Partner.PortalRole[]>([]);
  const [selectedRoleIds, setSelectedRoleIds] = useState<number[]>([]);

  const accountId = Number(getAccountId(config, account) || 0);
  const accountTitle = account?.userName || account?.nickName || accountId || '-';

  const roleItems = useMemo(
    () => roles.filter((role) => role.roleId != null),
    [roles],
  );

  const loadRoles = async () => {
    if (!partnerId || !accountId) {
      setRoles([]);
      setSelectedRoleIds([]);
      return;
    }
    setLoading(true);
    try {
      const resp = await config.services.getAccountRoles(partnerId, accountId);
      if (resp.code === 200) {
        setRoles(resp.roles || []);
        setSelectedRoleIds(normalizeRoleIds(resp.checkedKeys));
        return;
      }
      message.error(resp.msg || '角色加载失败');
    } catch {
      message.error('角色加载失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      void loadRoles();
      return;
    }
    setRoles([]);
    setSelectedRoleIds([]);
  }, [open, partnerId, accountId]);

  const handleSubmit = async () => {
    if (!partnerId || !accountId) {
      return;
    }
    setSaving(true);
    try {
      const resp = await config.services.assignAccountRoles(partnerId, accountId, selectedRoleIds);
      if (resp.code === 200) {
        message.success('角色已更新');
        onOpenChange(false);
        return;
      }
      message.error(resp.msg || '角色保存失败');
    } catch {
      message.error('角色保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      width={560}
      title={`分配角色 - ${accountTitle}`}
      open={open}
      destroyOnHidden
      confirmLoading={saving}
      onOk={handleSubmit}
      onCancel={() => onOpenChange(false)}
    >
      <Spin spinning={loading}>
        {roleItems.length > 0 ? (
          <Checkbox.Group
            value={selectedRoleIds}
            onChange={(values) => setSelectedRoleIds(normalizeRoleIds(values))}
            style={{ width: '100%' }}
          >
            <Flex vertical gap={10} style={{ width: '100%' }}>
              {roleItems.map((role) => {
                const roleName = role.roleName || role.roleKey || String(role.roleId);
                return (
                  <Checkbox
                    key={role.roleId}
                    value={role.roleId}
                    disabled={role.status === '1'}
                  >
                    <Flex vertical gap={0}>
                      <Typography.Text>{roleName}</Typography.Text>
                      <Typography.Text type="secondary">{role.roleKey || '-'}</Typography.Text>
                    </Flex>
                  </Checkbox>
                );
              })}
            </Flex>
          </Checkbox.Group>
        ) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可分配角色" />
        )}
      </Spin>
    </Modal>
  );
};

export default PartnerAccountRoleModal;
