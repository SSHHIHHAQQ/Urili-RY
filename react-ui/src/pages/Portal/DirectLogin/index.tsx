import { Button, Result, Spin } from 'antd';
import { history, useLocation } from '@umijs/max';
import React, { useEffect, useMemo, useState } from 'react';
import {
  clearPortalLogin,
  getPortalTerminal,
  persistPortalLogin,
  PORTAL_META,
  PORTAL_SERVICE,
} from '../terminal';

type DirectLoginState = {
  status: 'loading' | 'success' | 'error';
  message?: string;
};

const pageStyle: React.CSSProperties = {
  minHeight: '100vh',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: 24,
  background: '#f5f7fb',
};

const DirectLoginPage: React.FC = () => {
  const location = useLocation();
  const terminal = useMemo(() => getPortalTerminal(location.pathname), [location.pathname]);
  const [state, setState] = useState<DirectLoginState>({ status: 'loading' });

  useEffect(() => {
    let mounted = true;
    const run = async () => {
      if (!terminal) {
        setState({ status: 'error', message: '端类型无效' });
        return;
      }

      const directLoginToken = new URLSearchParams(location.search).get('directLoginToken');
      if (!directLoginToken) {
        clearPortalLogin(terminal);
        setState({ status: 'error', message: '免密登录 token 不能为空' });
        return;
      }

      try {
        const response = await PORTAL_SERVICE[terminal].directLogin(directLoginToken);
        if (response.code !== 200 || !persistPortalLogin(response.data, terminal)) {
          setState({ status: 'error', message: response.msg || '免密登录端类型不匹配' });
          return;
        }
        if (mounted) {
          setState({ status: 'success' });
          history.replace(PORTAL_META[terminal].homePath);
        }
      } catch (error) {
        console.log(error);
        clearPortalLogin(terminal);
        if (mounted) {
          setState({ status: 'error', message: '免密登录失败' });
        }
      }
    };

    run();
    return () => {
      mounted = false;
    };
  }, [location.search, terminal]);

  if (state.status === 'loading') {
    return (
      <div style={pageStyle}>
        <Spin description={terminal ? `正在进入${PORTAL_META[terminal].label}` : '正在进入'} />
      </div>
    );
  }

  if (state.status === 'success') {
    return (
      <div style={pageStyle}>
        <Result status="success" title="登录成功" />
      </div>
    );
  }

  return (
    <div style={pageStyle}>
      <Result
        status="error"
        title="免密登录失败"
        subTitle={state.message}
        extra={[
          <Button key="login" type="primary" onClick={() => history.replace('/user/login')}>
            返回管理端登录
          </Button>,
        ]}
      />
    </div>
  );
};

export default DirectLoginPage;
