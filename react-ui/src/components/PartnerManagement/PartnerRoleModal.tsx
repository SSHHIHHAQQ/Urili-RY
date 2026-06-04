import React, { useEffect, useState } from 'react';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Table,
  Tag,
  Tree,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { DataNode } from 'antd/es/tree';
import { PlusOutlined } from '@ant-design/icons';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import type { PartnerModuleConfig } from './PartnerManagementPage';

type PartnerRecord = Record<string, any>;
type RoleRecord = API.Partner.PortalRole & Record<string, any>;

type PartnerRoleModalProps = {
  config: PartnerModuleConfig;
  open: boolean;
  partner?: PartnerRecord;
  onOpenChange: (open: boolean) => void;
};

type RoleFormValues = {
  roleName?: string;
  roleKey?: string;
  roleSort?: number;
  status?: string;
  remark?: string;
};

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
];

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

function renderCompactText(value: unknown) {
  const text = value == null || value === '' ? '-' : String(value);
  return <Typography.Text style={compactCellTextStyle} title={text}>{text}</Typography.Text>;
}

function normalizeKeys(keys: React.Key[]) {
  return keys
    .map((key) => Number(key))
    .filter((key) => Number.isFinite(key));
}

function toTreeData(nodes: API.Partner.PortalTreeNode[]): DataNode[] {
  return nodes.map((node) => ({
    key: node.id,
    title: node.label,
    children: node.children ? toTreeData(node.children) : undefined,
  }));
}

function mapRoleToForm(role?: RoleRecord): RoleFormValues {
  return {
    roleName: role?.roleName,
    roleKey: role?.roleKey,
    roleSort: role?.roleSort ?? 0,
    status: role?.status || '0',
    remark: role?.remark,
  };
}

function buildRolePayload(
  currentRole: RoleRecord | undefined,
  values: RoleFormValues,
  checkedMenuIds: number[],
) {
  return {
    roleId: currentRole?.roleId,
    roleName: values.roleName,
    roleKey: values.roleKey,
    roleSort: values.roleSort ?? 0,
    status: values.status || '0',
    remark: values.remark,
    menuIds: checkedMenuIds,
  };
}

const PartnerRoleModal: React.FC<PartnerRoleModalProps> = ({
  config,
  open,
  partner,
  onOpenChange,
}) => {
  const { message, modal } = App.useApp();
  const access = useAccess();
  const [form] = Form.useForm<RoleFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [currentRole, setCurrentRole] = useState<RoleRecord>();
  const [roles, setRoles] = useState<RoleRecord[]>([]);
  const [menuTree, setMenuTree] = useState<API.Partner.PortalTreeNode[]>([]);
  const [checkedMenuIds, setCheckedMenuIds] = useState<number[]>([]);

  const partnerId = Number(getValue(partner, config.idField) || 0);
  const partnerName = getValue(partner, config.nameField) || getValue(partner, config.codeField) || '';
  const permPrefix = `${config.moduleKey}:admin`;
  const currentRoleId = currentRole?.roleId;

  const loadRoles = async () => {
    if (!partnerId) {
      setRoles([]);
      return;
    }
    setLoading(true);
    try {
      const resp = await config.services.listRoles(partnerId);
      if (resp.code === 200) {
        setRoles((resp.rows || []) as RoleRecord[]);
        return;
      }
      message.error(resp.msg || '角色列表加载失败');
    } catch {
      message.error('角色列表加载失败，请重试');
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
  }, [open, partnerId]);

  useEffect(() => {
    if (formOpen) {
      form.resetFields();
      form.setFieldsValue(mapRoleToForm(currentRole));
    }
  }, [formOpen, currentRole?.roleId]);

  const openRoleForm = async (role?: RoleRecord) => {
    setCurrentRole(role);
    setCheckedMenuIds([]);
    setMenuTree([]);
    setFormOpen(true);
    try {
      if (role?.roleId) {
        const [roleResp, menuResp] = await Promise.all([
          config.services.getRole(partnerId, role.roleId),
          config.services.getRoleMenuTree(partnerId, role.roleId),
        ]);
        if (roleResp.code === 200) {
          setCurrentRole(roleResp.data as RoleRecord);
          form.setFieldsValue(mapRoleToForm(roleResp.data as RoleRecord));
        }
        if (menuResp.code === 200) {
          setMenuTree(menuResp.menus || []);
          setCheckedMenuIds((menuResp.checkedKeys || []).map(Number));
        }
        return;
      }
      const menuResp = await config.services.getMenuTree();
      if (menuResp.code === 200) {
        setMenuTree(menuResp.data || []);
      }
    } catch {
      message.error('角色菜单加载失败，请重试');
    }
  };

  const closeRoleForm = () => {
    setFormOpen(false);
    setCurrentRole(undefined);
    setMenuTree([]);
    setCheckedMenuIds([]);
  };

  const handleSubmit = async () => {
    if (!partnerId) {
      return;
    }
    const values = await form.validateFields();
    const payload = buildRolePayload(currentRole, values, checkedMenuIds);
    setSaving(true);
    try {
      const resp = currentRoleId
        ? await config.services.updateRole(partnerId, payload)
        : await config.services.addRole(partnerId, payload);
      if (resp.code === 200) {
        message.success(currentRoleId ? '角色已更新' : '角色已新增');
        closeRoleForm();
        await loadRoles();
        return;
      }
      message.error(resp.msg || '角色保存失败');
    } catch {
      message.error('角色保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (role: RoleRecord) => {
    if (!partnerId || !role.roleId) {
      return;
    }
    const nextStatus = role.status === '0' ? '1' : '0';
    const resp = await config.services.changeRoleStatus(partnerId, {
      roleId: role.roleId,
      status: nextStatus,
    });
    if (resp.code === 200) {
      message.success('角色状态已更新');
      await loadRoles();
      return;
    }
    message.error(resp.msg || '角色状态更新失败');
  };

  const handleRemove = (role: RoleRecord) => {
    if (!partnerId || !role.roleId) {
      return;
    }
    modal.confirm({
      title: `确认删除角色 ${role.roleName || role.roleId} 吗？`,
      content: '已有账号绑定时，后端会按端内权限规则处理。',
      onOk: async () => {
        const resp = await config.services.removeRoles(partnerId, [role.roleId as number]);
        if (resp.code === 200) {
          message.success('角色已删除');
          await loadRoles();
          return;
        }
        message.error(resp.msg || '角色删除失败');
      },
    });
  };

  const columns: ColumnsType<RoleRecord> = [
    {
      title: '角色名称',
      dataIndex: 'roleName',
      width: 160,
      render: renderCompactText,
    },
    {
      title: '权限字符',
      dataIndex: 'roleKey',
      width: 160,
      render: renderCompactText,
    },
    {
      title: '排序',
      dataIndex: 'roleSort',
      width: 72,
      render: renderCompactText,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 96,
      render: (value, record) => (
        <Tag
          color={value === '0' ? 'success' : 'default'}
          style={{ cursor: access.hasPerms(`${permPrefix}:role:edit`) ? 'pointer' : 'default' }}
          onClick={() => {
            if (access.hasPerms(`${permPrefix}:role:edit`)) {
              void handleStatusChange(record);
            }
          }}
        >
          {value === '0' ? '正常' : '停用'}
        </Tag>
      ),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 180,
      render: renderCompactText,
    },
    {
      title: '操作',
      dataIndex: 'option',
      width: 120,
      render: (_, record) => (
        <Flex gap={4}>
          <Button
            type="link"
            size="small"
            hidden={!access.hasPerms(`${permPrefix}:role:edit`)}
            onClick={() => void openRoleForm(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            hidden={!access.hasPerms(`${permPrefix}:role:remove`)}
            onClick={() => handleRemove(record)}
          >
            删除
          </Button>
        </Flex>
      ),
    },
  ];

  return (
    <>
      <Modal
        width={900}
        title={`${config.label}角色 - ${partnerName || '-'}`}
        open={open}
        destroyOnHidden
        footer={null}
        onCancel={() => onOpenChange(false)}
      >
        <Table<RoleRecord>
          rowKey={(record) => String(record.roleId)}
          loading={loading}
          columns={columns}
          dataSource={roles}
          size="small"
          pagination={false}
          tableLayout="fixed"
          title={() => (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              hidden={!access.hasPerms(`${permPrefix}:role:add`)}
              onClick={() => void openRoleForm()}
            >
              新增角色
            </Button>
          )}
        />
      </Modal>

      <Modal
        width={680}
        title={currentRoleId ? '编辑角色' : '新增角色'}
        open={formOpen}
        destroyOnHidden
        confirmLoading={saving}
        onOk={handleSubmit}
        onCancel={closeRoleForm}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="角色名称" name="roleName" rules={[{ required: true, message: '请输入角色名称' }]}>
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="权限字符" name="roleKey" rules={[{ required: true, message: '请输入权限字符' }]}>
            <Input placeholder="例如 admin / staff" />
          </Form.Item>
          <Form.Item label="排序" name="roleSort" rules={[{ required: true, message: '请输入排序' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="状态" name="status" rules={[{ required: true, message: '请选择状态' }]}>
            <Select {...SEARCHABLE_SELECT_PROPS} options={statusOptions} />
          </Form.Item>
          <Form.Item label="菜单权限">
            <div style={{ maxHeight: 260, overflow: 'auto', border: '1px solid #f0f0f0', padding: 8 }}>
              {menuTree.length > 0 ? (
                <Tree
                  checkable
                  checkedKeys={checkedMenuIds}
                  treeData={toTreeData(menuTree)}
                  defaultExpandAll
                  onCheck={(checked) => {
                    const keys = Array.isArray(checked) ? checked : checked.checked;
                    setCheckedMenuIds(normalizeKeys(keys));
                  }}
                />
              ) : (
                <Typography.Text type="secondary">暂无可分配菜单</Typography.Text>
              )}
            </div>
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="请输入" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default PartnerRoleModal;
