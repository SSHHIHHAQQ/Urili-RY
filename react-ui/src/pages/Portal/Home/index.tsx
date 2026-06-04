import {
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  Modal,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd';
import { LockOutlined, LogoutOutlined, ReloadOutlined } from '@ant-design/icons';
import { history, useLocation } from '@umijs/max';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { getTerminalAccessToken } from '@/access';
import { message } from '@/utils/feedback';
import {
  clearPortalLogin,
  getPortalTerminal,
  PORTAL_META,
  PORTAL_SERVICE,
  type PortalTerminal,
} from '../terminal';

type PortalHomeData = {
  info?: API.Partner.PortalPermissionInfo;
  subject?: API.Partner.PortalSubjectProfile;
  account?: API.Partner.PortalAccountProfile;
  accounts?: API.Partner.PortalAccountProfile[];
  depts?: API.Partner.PortalDeptProfile[];
  roles?: API.Partner.PortalRoleProfile[];
};

type PasswordFormValues = API.Partner.PortalPasswordChangeParams;

const pageStyle: React.CSSProperties = {
  minHeight: '100vh',
  padding: 24,
  background: '#f5f7fb',
};

const headerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 16,
  marginBottom: 16,
};

const gridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
  gap: 16,
};

const wideGridStyle: React.CSSProperties = {
  gridColumn: 'span 2',
};

const fullGridStyle: React.CSSProperties = {
  gridColumn: '1 / -1',
};

function displayText(value?: string | number | null) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function renderTags(values?: string[]) {
  if (!values || values.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }
  return (
    <Space wrap size={[6, 6]}>
      {values.map((value) => (
        <Tag key={value}>{value}</Tag>
      ))}
    </Space>
  );
}

function renderRoleList(roles?: API.Partner.PortalRoleProfile[]) {
  if (!roles || roles.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }
  return (
    <Space wrap size={[6, 6]}>
      {roles.map((role) => (
        <Tag key={role.roleId}>{role.roleName || role.roleKey || role.roleId}</Tag>
      ))}
    </Space>
  );
}

function renderDeptList(depts?: API.Partner.PortalDeptProfile[]) {
  if (!depts || depts.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }
  return (
    <Space wrap size={[6, 6]}>
      {depts.map((dept) => (
        <Tag key={dept.deptId}>{dept.deptName || dept.deptId}</Tag>
      ))}
    </Space>
  );
}

const PortalHomePage: React.FC = () => {
  const location = useLocation();
  const terminal = useMemo(() => getPortalTerminal(location.pathname), [location.pathname]);
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<PortalHomeData>({});
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [passwordForm] = Form.useForm<PasswordFormValues>();

  const loadData = useCallback(async (currentTerminal: PortalTerminal) => {
    setLoading(true);
    try {
      const service = PORTAL_SERVICE[currentTerminal];
      const [infoRes, subjectRes, accountRes, accountsRes, deptsRes, rolesRes] = await Promise.all([
        service.getInfo(),
        service.getSubjectProfile(),
        service.getAccountProfile(),
        service.getAccounts(),
        service.getDepts(),
        service.getRoles(),
      ]);
      setData({
        info: infoRes.data,
        subject: subjectRes.data,
        account: accountRes.data,
        accounts: accountsRes.data || [],
        depts: deptsRes.data || [],
        roles: rolesRes.data || [],
      });
    } catch (error) {
      console.log(error);
      clearPortalLogin(currentTerminal);
      message.error('登录状态已失效');
      history.replace('/user/login');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!terminal) {
      history.replace('/user/login');
      return;
    }
    if (!getTerminalAccessToken(terminal)) {
      history.replace('/user/login');
      return;
    }
    loadData(terminal);
  }, [loadData, terminal]);

  const handleLogout = async () => {
    if (!terminal) {
      return;
    }
    try {
      await PORTAL_SERVICE[terminal].logout();
    } catch (error) {
      console.log(error);
    } finally {
      clearPortalLogin(terminal);
      history.replace('/user/login');
    }
  };

  const handlePasswordSubmit = async () => {
    if (!terminal) {
      return;
    }
    const values = await passwordForm.validateFields();
    setPasswordSubmitting(true);
    try {
      const response = await PORTAL_SERVICE[terminal].updatePassword(values);
      if (response.code === 200) {
        message.success('密码已更新');
        setPasswordOpen(false);
        passwordForm.resetFields();
      } else {
        message.error(response.msg || '密码更新失败');
      }
    } finally {
      setPasswordSubmitting(false);
    }
  };

  if (!terminal) {
    return null;
  }

  const meta = PORTAL_META[terminal];
  const permissions = data.info?.permissions || [];

  return (
    <div style={pageStyle}>
      <div style={headerStyle}>
        <Space direction="vertical" size={0}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {meta.label}
          </Typography.Title>
          <Typography.Text type="secondary">
            {displayText(data.info?.subjectNo)} / {displayText(data.account?.userName)}
          </Typography.Text>
        </Space>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => loadData(terminal)}>
            刷新
          </Button>
          <Button icon={<LockOutlined />} onClick={() => setPasswordOpen(true)}>
            修改密码
          </Button>
          <Button danger icon={<LogoutOutlined />} onClick={handleLogout}>
            退出
          </Button>
        </Space>
      </div>

      <Spin spinning={loading}>
        <div style={gridStyle}>
          <Card title="主体资料" bordered={false} style={wideGridStyle}>
            <Descriptions column={2} size="small">
              <Descriptions.Item label="主体编号">{displayText(data.subject?.subjectNo)}</Descriptions.Item>
              <Descriptions.Item label="主体代码">{displayText(data.subject?.subjectCode)}</Descriptions.Item>
              <Descriptions.Item label="主体名称">{displayText(data.subject?.subjectName)}</Descriptions.Item>
              <Descriptions.Item label="状态">{displayText(data.subject?.status)}</Descriptions.Item>
              <Descriptions.Item label="联系人">{displayText(data.subject?.contactName)}</Descriptions.Item>
              <Descriptions.Item label="邮箱">{displayText(data.subject?.contactEmail)}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="当前账号" bordered={false}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="账号">{displayText(data.account?.userName)}</Descriptions.Item>
              <Descriptions.Item label="昵称">{displayText(data.account?.nickName)}</Descriptions.Item>
              <Descriptions.Item label="部门">{displayText(data.account?.deptName)}</Descriptions.Item>
              <Descriptions.Item label="状态">{displayText(data.account?.status)}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="端内角色" bordered={false}>
            {renderRoleList(data.roles)}
          </Card>

          <Card title="端内部门" bordered={false}>
            {renderDeptList(data.depts)}
          </Card>

          <Card title="端内账号" bordered={false}>
            <Typography.Text>{data.accounts?.length || 0}</Typography.Text>
          </Card>

          <Card title="权限标识" bordered={false} style={fullGridStyle}>
            {renderTags(permissions)}
          </Card>
        </div>
      </Spin>

      <Modal
        title="修改密码"
        open={passwordOpen}
        onCancel={() => {
          setPasswordOpen(false);
          passwordForm.resetFields();
        }}
        onOk={handlePasswordSubmit}
        confirmLoading={passwordSubmitting}
        destroyOnHidden
      >
        <Form form={passwordForm} layout="vertical" preserve={false}>
          <Form.Item name="oldPassword" label="旧密码" rules={[{ required: true, message: '请输入旧密码' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item name="newPassword" label="新密码" rules={[{ required: true, message: '请输入新密码' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的新密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PortalHomePage;
