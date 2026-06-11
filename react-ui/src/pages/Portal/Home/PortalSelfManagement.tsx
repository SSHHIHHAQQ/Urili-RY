import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  TreeSelect,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { message } from '@/utils/feedback';
import { matchPermission } from '@/utils/permission';
import { PORTAL_SERVICE, type PortalTerminal } from '../terminal';

type Props = {
  terminal: PortalTerminal;
  permissions: string[];
  accounts: API.Partner.PortalAccountBase[];
  depts: API.Partner.PortalDeptProfile[];
  roles: API.Partner.PortalRoleProfile[];
  onChanged: () => void;
};

type PortalTreeDataNode = {
  title: string;
  value: number;
  children?: PortalTreeDataNode[];
};

const fullSpanStyle: React.CSSProperties = {
  gridColumn: '1 / -1',
};

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
];

const accountRoleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '员工', value: 'STAFF' },
];

function hasPermission(permissions: string[], terminal: PortalTerminal, permission: string) {
  return matchPermission(permissions, `${terminal}:${permission}`);
}

function assertPortalSuccess<T extends { code?: number | string; msg?: string }>(
  response: T,
  fallbackMessage: string,
) {
  if (Number(response?.code) !== 200) {
    throw new Error(response?.msg || fallbackMessage);
  }
  return response;
}

function displayText(value?: string | number | null) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function mapTreeData(nodes?: API.Partner.PortalTreeNode[]): PortalTreeDataNode[] {
  return (nodes || []).map((node) => ({
    title: node.label,
    value: node.id,
    children: mapTreeData(node.children),
  }));
}

const PortalSelfManagement: React.FC<Props> = ({
  terminal,
  permissions,
  accounts,
  depts,
  roles,
  onChanged,
}) => {
  const service = PORTAL_SERVICE[terminal];
  const [submitting, setSubmitting] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<API.Partner.PortalAccountBase>();
  const [accountRoleOpen, setAccountRoleOpen] = useState(false);
  const [roleAssignAccount, setRoleAssignAccount] = useState<API.Partner.PortalAccountBase>();
  const [checkedRoleIds, setCheckedRoleIds] = useState<number[]>([]);
  const [deptOpen, setDeptOpen] = useState(false);
  const [editingDept, setEditingDept] = useState<API.Partner.PortalDeptProfile>();
  const [roleOpen, setRoleOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<API.Partner.PortalRoleProfile>();
  const [roleMenuTree, setRoleMenuTree] = useState<PortalTreeDataNode[]>([]);
  const [loginLogs, setLoginLogs] = useState<API.Partner.PortalOwnLoginLogProfile[]>([]);
  const [operLogs, setOperLogs] = useState<API.Partner.PortalOwnOperLogProfile[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [accountForm] = Form.useForm<API.Partner.PortalAccountPayload>();
  const [deptForm] = Form.useForm<API.Partner.PortalDept>();
  const [roleForm] = Form.useForm<API.Partner.PortalRole>();

  const canViewAccounts = hasPermission(permissions, terminal, 'account:list');
  const canViewDepts = hasPermission(permissions, terminal, 'dept:list');
  const canViewRoles = hasPermission(permissions, terminal, 'role:list');
  const canAddAccount = hasPermission(permissions, terminal, 'account:add');
  const canEditAccount = hasPermission(permissions, terminal, 'account:edit');
  const canQueryAccountRole = hasPermission(permissions, terminal, 'account:role:query');
  const canEditAccountRole = hasPermission(permissions, terminal, 'account:role:edit');
  const canAddDept = hasPermission(permissions, terminal, 'dept:add');
  const canEditDept = hasPermission(permissions, terminal, 'dept:edit');
  const canRemoveDept = hasPermission(permissions, terminal, 'dept:remove');
  const canQueryRole = hasPermission(permissions, terminal, 'role:query');
  const canAddRole = hasPermission(permissions, terminal, 'role:add');
  const canEditRole = hasPermission(permissions, terminal, 'role:edit');
  const canRemoveRole = hasPermission(permissions, terminal, 'role:remove');
  const canViewLoginLogs = hasPermission(permissions, terminal, 'account:loginLog:list');
  const canViewOperLogs = hasPermission(permissions, terminal, 'account:operLog:list');
  const canAssignAccountRoles = canViewRoles && canQueryAccountRole && canEditAccountRole;
  const canCreateRole = canAddRole && canQueryRole;

  const deptOptions = useMemo(
    () => depts.map((dept) => ({ label: dept.deptName || String(dept.deptId), value: dept.deptId })),
    [depts],
  );
  const roleOptions = useMemo(
    () => roles.map((role) => ({ label: role.roleName || String(role.roleId), value: role.roleId })),
    [roles],
  );

  const loadAudit = useCallback(async () => {
    if (!canViewLoginLogs && !canViewOperLogs) {
      setLoginLogs([]);
      setOperLogs([]);
      return;
    }
    setAuditLoading(true);
    try {
      const [loginRes, operRes] = await Promise.all([
        canViewLoginLogs
          ? service.getLoginLogs({ pageNum: 1, pageSize: 5 }).then((response) =>
              assertPortalSuccess(response, '登录日志加载失败'),
            )
          : Promise.resolve({ rows: [] }),
        canViewOperLogs
          ? service.getOperLogs({ pageNum: 1, pageSize: 5 }).then((response) =>
              assertPortalSuccess(response, '操作日志加载失败'),
            )
          : Promise.resolve({ rows: [] }),
      ]);
      setLoginLogs(loginRes.rows || []);
      setOperLogs(operRes.rows || []);
    } catch (error) {
      console.log(error);
      message.error('审计日志加载失败');
    } finally {
      setAuditLoading(false);
    }
  }, [canViewLoginLogs, canViewOperLogs, service]);

  useEffect(() => {
    loadAudit();
  }, [loadAudit]);

  const refreshAfterMutation = async (successText: string) => {
    message.success(successText);
    onChanged();
  };

  const openAccountCreate = () => {
    setEditingAccount(undefined);
    accountForm.resetFields();
    accountForm.setFieldsValue({ accountRole: 'STAFF', status: '0' });
    setAccountOpen(true);
  };

  const openAccountEdit = (record: API.Partner.PortalAccountBase) => {
    setEditingAccount(record);
    accountForm.setFieldsValue(record);
    setAccountOpen(true);
  };

  const submitAccount = async () => {
    const values = await accountForm.validateFields();
    setSubmitting(true);
    try {
      if (editingAccount?.accountId) {
        await assertPortalSuccess(
          await service.updateAccount(editingAccount.accountId, values),
          '账号修改失败',
        );
        await refreshAfterMutation('账号已更新');
      } else {
        await assertPortalSuccess(await service.createAccount(values), '账号新增失败');
        await refreshAfterMutation('账号已新增');
      }
      setAccountOpen(false);
    } finally {
      setSubmitting(false);
    }
  };

  const openAccountRoleAssign = async (record: API.Partner.PortalAccountBase) => {
    if (!record.accountId) {
      return;
    }
    setRoleAssignAccount(record);
    setSubmitting(true);
    try {
      const response = assertPortalSuccess(
        await service.getAccountRoles(record.accountId),
        '账号角色加载失败',
      );
      setCheckedRoleIds(response.checkedKeys || []);
      setAccountRoleOpen(true);
    } finally {
      setSubmitting(false);
    }
  };

  const submitAccountRoles = async () => {
    if (!roleAssignAccount?.accountId) {
      return;
    }
    setSubmitting(true);
    try {
      await assertPortalSuccess(
        await service.assignAccountRoles(roleAssignAccount.accountId, checkedRoleIds),
        '账号角色分配失败',
      );
      setAccountRoleOpen(false);
      await refreshAfterMutation('账号角色已更新');
    } finally {
      setSubmitting(false);
    }
  };

  const openDeptCreate = () => {
    setEditingDept(undefined);
    deptForm.resetFields();
    deptForm.setFieldsValue({ status: '0', orderNum: 0 });
    setDeptOpen(true);
  };

  const openDeptEdit = (record: API.Partner.PortalDeptProfile) => {
    setEditingDept(record);
    deptForm.setFieldsValue(record);
    setDeptOpen(true);
  };

  const submitDept = async () => {
    const values = await deptForm.validateFields();
    setSubmitting(true);
    try {
      if (editingDept?.deptId) {
        await assertPortalSuccess(await service.updateDept(editingDept.deptId, values), '部门修改失败');
        await refreshAfterMutation('部门已更新');
      } else {
        await assertPortalSuccess(await service.createDept(values), '部门新增失败');
        await refreshAfterMutation('部门已新增');
      }
      setDeptOpen(false);
    } finally {
      setSubmitting(false);
    }
  };

  const removeDept = async (record: API.Partner.PortalDeptProfile) => {
    if (!record.deptId) {
      return;
    }
    await assertPortalSuccess(await service.deleteDept(record.deptId), '部门删除失败');
    await refreshAfterMutation('部门已删除');
  };

  const openRoleCreate = async () => {
    setEditingRole(undefined);
    roleForm.resetFields();
    setSubmitting(true);
    try {
      const response = assertPortalSuccess(await service.getRoleMenus(), '角色权限模板加载失败');
      setRoleMenuTree(mapTreeData(response.menus));
      roleForm.setFieldsValue({ status: '0', roleSort: 0, menuIds: [] });
      setRoleOpen(true);
    } finally {
      setSubmitting(false);
    }
  };

  const openRoleEdit = async (record: API.Partner.PortalRoleProfile) => {
    if (!record.roleId) {
      return;
    }
    setEditingRole(record);
    roleForm.resetFields();
    setSubmitting(true);
    try {
      const [roleResponse, menuResponse] = await Promise.all([
        service.getRole(record.roleId).then((response) => assertPortalSuccess(response, '角色详情加载失败')),
        service
          .getRoleMenus(record.roleId)
          .then((response) => assertPortalSuccess(response, '角色权限模板加载失败')),
      ]);
      setRoleMenuTree(mapTreeData(menuResponse.menus));
      roleForm.setFieldsValue({
        ...roleResponse.data,
        menuIds: menuResponse.checkedKeys || [],
      });
      setRoleOpen(true);
    } finally {
      setSubmitting(false);
    }
  };

  const submitRole = async () => {
    const values = await roleForm.validateFields();
    values.menuIds = values.menuIds || [];
    setSubmitting(true);
    try {
      if (editingRole?.roleId) {
        await assertPortalSuccess(await service.updateRole(editingRole.roleId, values), '角色修改失败');
        await refreshAfterMutation('角色已更新');
      } else {
        await assertPortalSuccess(await service.createRole(values), '角色新增失败');
        await refreshAfterMutation('角色已新增');
      }
      setRoleOpen(false);
    } finally {
      setSubmitting(false);
    }
  };

  const removeRole = async (record: API.Partner.PortalRoleProfile) => {
    if (!record.roleId) {
      return;
    }
    await assertPortalSuccess(await service.deleteRole(record.roleId), '角色删除失败');
    await refreshAfterMutation('角色已删除');
  };

  const accountColumns: ColumnsType<API.Partner.PortalAccountBase> = [
    { title: '账号', dataIndex: 'userName', key: 'userName', render: displayText },
    { title: '姓名', dataIndex: 'nickName', key: 'nickName', render: displayText },
    { title: '角色类型', dataIndex: 'accountRole', key: 'accountRole', width: 120, render: displayText },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90, render: displayText },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          {canEditAccount ? <Button type="link" size="small" onClick={() => openAccountEdit(record)}>编辑</Button> : null}
          {canAssignAccountRoles ? (
            <Button type="link" size="small" onClick={() => openAccountRoleAssign(record)}>角色</Button>
          ) : null}
        </Space>
      ),
    },
  ];

  const deptColumns: ColumnsType<API.Partner.PortalDeptProfile> = [
    { title: '部门', dataIndex: 'deptName', key: 'deptName', render: displayText },
    { title: '上级部门', dataIndex: 'parentName', key: 'parentName', render: displayText },
    { title: '排序', dataIndex: 'orderNum', key: 'orderNum', width: 90, render: displayText },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90, render: displayText },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          {canEditDept ? <Button type="link" size="small" onClick={() => openDeptEdit(record)}>编辑</Button> : null}
          {canRemoveDept ? (
            <Popconfirm title="确认删除该部门？" onConfirm={() => removeDept(record)}>
              <Button type="link" danger size="small">删除</Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  const roleColumns: ColumnsType<API.Partner.PortalRoleProfile> = [
    { title: '角色', dataIndex: 'roleName', key: 'roleName', render: displayText },
    { title: '权限字符', dataIndex: 'roleKey', key: 'roleKey', render: displayText },
    { title: '排序', dataIndex: 'roleSort', key: 'roleSort', width: 90, render: displayText },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90, render: displayText },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          {canQueryRole && canEditRole ? <Button type="link" size="small" onClick={() => openRoleEdit(record)}>编辑</Button> : null}
          {canRemoveRole ? (
            <Popconfirm title="确认删除该角色？" onConfirm={() => removeRole(record)}>
              <Button type="link" danger size="small">删除</Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ];

  return (
    <>
      {canViewAccounts ? (
        <Card
          title="账号管理"
          variant="borderless"
          style={fullSpanStyle}
          extra={canAddAccount ? <Button type="primary" onClick={openAccountCreate}>新增账号</Button> : null}
        >
          <Table
            rowKey={(record) => String(record.accountId || record.userName)}
            size="small"
            pagination={false}
            columns={accountColumns}
            dataSource={accounts}
            scroll={{ x: 760 }}
          />
        </Card>
      ) : null}

      {canViewDepts ? (
        <Card
          title="部门管理"
          variant="borderless"
          style={fullSpanStyle}
          extra={canAddDept ? <Button type="primary" onClick={openDeptCreate}>新增部门</Button> : null}
        >
          <Table
            rowKey={(record) => String(record.deptId || record.deptName)}
            size="small"
            pagination={false}
            columns={deptColumns}
            dataSource={depts}
            scroll={{ x: 680 }}
          />
        </Card>
      ) : null}

      {canViewRoles ? (
        <Card
          title="角色管理"
          variant="borderless"
          style={fullSpanStyle}
          extra={canCreateRole ? <Button type="primary" onClick={openRoleCreate}>新增角色</Button> : null}
        >
          <Table
            rowKey={(record) => String(record.roleId || record.roleKey)}
            size="small"
            pagination={false}
            columns={roleColumns}
            dataSource={roles}
            scroll={{ x: 720 }}
          />
        </Card>
      ) : null}

      {canViewLoginLogs ? (
        <Card title="登录日志" variant="borderless" style={fullSpanStyle}>
          <Table
            rowKey={(record, index) => `${record.userName || ''}-${record.loginTime || ''}-${index}`}
            size="small"
            loading={auditLoading}
            pagination={false}
            dataSource={loginLogs}
            columns={[
              { title: '账号', dataIndex: 'userName', key: 'userName', render: displayText },
              { title: 'IP', dataIndex: 'ipaddr', key: 'ipaddr', render: displayText },
              { title: '状态', dataIndex: 'status', key: 'status', render: displayText },
              { title: '时间', dataIndex: 'loginTime', key: 'loginTime', render: displayText },
            ]}
          />
        </Card>
      ) : null}

      {canViewOperLogs ? (
        <Card title="操作日志" variant="borderless" style={fullSpanStyle}>
          <Table
            rowKey={(record, index) => `${record.title || ''}-${record.operTime || ''}-${index}`}
            size="small"
            loading={auditLoading}
            pagination={false}
            dataSource={operLogs}
            columns={[
              { title: '模块', dataIndex: 'title', key: 'title', render: displayText },
              { title: '操作人', dataIndex: 'operName', key: 'operName', render: displayText },
              { title: '状态', dataIndex: 'status', key: 'status', render: displayText },
              { title: '时间', dataIndex: 'operTime', key: 'operTime', render: displayText },
            ]}
          />
        </Card>
      ) : null}

      <Modal
        title={editingAccount ? '编辑账号' : '新增账号'}
        open={accountOpen}
        onCancel={() => setAccountOpen(false)}
        onOk={submitAccount}
        confirmLoading={submitting}
        destroyOnHidden
      >
        <Form form={accountForm} layout="vertical" preserve={false}>
          <Form.Item name="userName" label="登录账号" rules={[{ required: !editingAccount, message: '请输入登录账号' }]}>
            <Input disabled={!!editingAccount} autoComplete="off" />
          </Form.Item>
          <Form.Item name="nickName" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input />
          </Form.Item>
          {!editingAccount ? (
            <Form.Item name="password" label="初始密码" rules={[{ required: true, message: '请输入初始密码' }]}>
              <Input.Password autoComplete="new-password" />
            </Form.Item>
          ) : null}
          <Form.Item name="deptId" label="部门">
            <Select allowClear showSearch optionFilterProp="label" options={deptOptions} />
          </Form.Item>
          <Form.Item name="accountRole" label="账号角色" rules={[{ required: true, message: '请选择账号角色' }]}>
            <Select options={accountRoleOptions} disabled={!!editingAccount} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={statusOptions} />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
          <Form.Item name="phonenumber" label="手机号">
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="分配角色"
        open={accountRoleOpen}
        onCancel={() => setAccountRoleOpen(false)}
        onOk={submitAccountRoles}
        confirmLoading={submitting}
        destroyOnHidden
      >
        <Select
          mode="multiple"
          style={{ width: '100%' }}
          value={checkedRoleIds}
          options={roleOptions}
          onChange={setCheckedRoleIds}
          optionFilterProp="label"
          showSearch
        />
      </Modal>

      <Modal
        title={editingDept ? '编辑部门' : '新增部门'}
        open={deptOpen}
        onCancel={() => setDeptOpen(false)}
        onOk={submitDept}
        confirmLoading={submitting}
        destroyOnHidden
      >
        <Form form={deptForm} layout="vertical" preserve={false}>
          <Form.Item name="deptName" label="部门名称" rules={[{ required: true, message: '请输入部门名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="parentId" label="上级部门">
            <Select allowClear showSearch optionFilterProp="label" options={deptOptions} />
          </Form.Item>
          <Form.Item name="orderNum" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="leader" label="负责人">
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="电话">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={statusOptions} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
        open={roleOpen}
        onCancel={() => setRoleOpen(false)}
        onOk={submitRole}
        confirmLoading={submitting}
        destroyOnHidden
      >
        <Form form={roleForm} layout="vertical" preserve={false}>
          <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="roleKey" label="权限字符" rules={[{ required: true, message: '请输入权限字符' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="roleSort" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={statusOptions} />
          </Form.Item>
          <Form.Item name="menuIds" label="权限模板">
            <TreeSelect
              treeCheckable
              showCheckedStrategy={TreeSelect.SHOW_PARENT}
              treeData={roleMenuTree}
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default PortalSelfManagement;
