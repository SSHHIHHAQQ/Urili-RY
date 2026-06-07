import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Form, Input, Typography, App } from 'antd';
import { history, useLocation } from '@umijs/max';
import React, { useMemo, useState } from 'react';
import { isPortalTerminalPath } from '@/utils/portalPaths';
import {
  getPortalTerminal,
  persistPortalLogin,
  PORTAL_META,
  PORTAL_SERVICE,
  type PortalTerminal,
} from '../terminal';

type LoginFormValues = {
  username?: string;
  password?: string;
};

const pageStyle: React.CSSProperties = {
  minHeight: '100vh',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: 24,
  background: '#f5f7fb',
};

const panelStyle: React.CSSProperties = {
  width: 360,
  maxWidth: '100%',
  padding: 28,
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: 8,
  boxShadow: '0 12px 32px rgba(15, 23, 42, 0.08)',
};

function resolveRedirect(search: string, terminal: PortalTerminal) {
  const redirect = new URLSearchParams(search).get('redirect');
  const redirectPath = (redirect || '').split(/[?#]/, 1)[0];
  if (
    !redirect
    || !isPortalTerminalPath(redirect, terminal)
    || redirectPath === PORTAL_META[terminal].loginPath
    || redirectPath === `/${terminal}/direct-login`
  ) {
    return PORTAL_META[terminal].homePath;
  }
  return redirect;
}

const PortalLoginPage: React.FC = () => {
  const { message } = App.useApp();
  const location = useLocation();
  const [form] = Form.useForm<LoginFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const terminal = useMemo(() => getPortalTerminal(location.pathname), [location.pathname]);

  const handleSubmit = async () => {
    if (!terminal) {
      history.replace('/seller/login');
      return;
    }
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      const response = await PORTAL_SERVICE[terminal].login({
        username: values.username?.trim(),
        password: values.password,
      });
      if (response.code === 200 && persistPortalLogin(response.data, terminal)) {
        history.replace(resolveRedirect(location.search, terminal));
        return;
      }
      message.error(response.msg || 'Login failed');
    } catch (error) {
      console.log(error);
      message.error('Login failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (!terminal) {
    return null;
  }

  return (
    <div style={pageStyle}>
      <div style={panelStyle}>
        <Typography.Title level={3} style={{ marginTop: 0, marginBottom: 24 }}>
          {PORTAL_META[terminal].label}
        </Typography.Title>
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="username" label="登录账号" rules={[{ required: true, message: '请输入登录账号' }]}>
            <Input prefix={<UserOutlined />} autoComplete="username" placeholder="请输入" />
          </Form.Item>
          <Form.Item name="password" label="登录密码" rules={[{ required: true, message: '请输入登录密码' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" placeholder="请输入" />
          </Form.Item>
          <Button type="primary" block htmlType="submit" loading={submitting}>
            登录
          </Button>
        </Form>
      </div>
    </div>
  );
};

export default PortalLoginPage;
