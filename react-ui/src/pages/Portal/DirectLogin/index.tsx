import { Button, Result, Spin } from 'antd';
import { history, useLocation } from '@umijs/max';
import React, { useEffect, useMemo, useState } from 'react';
import {
  PORTAL_DIRECT_LOGIN_READY_MESSAGE,
  PORTAL_DIRECT_LOGIN_RESULT_MESSAGE,
  PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE,
  resolvePortalDirectLoginOpenerOrigin,
  type PortalDirectLoginResultMessage,
  type PortalDirectLoginTokenMessage,
} from '@/utils/portalDirectLoginMessage';
import {
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

function isDirectLoginTokenMessage(
  data: unknown,
  terminal: API.Partner.PortalTerminal,
): data is PortalDirectLoginTokenMessage {
  return Boolean(
    data
      && typeof data === 'object'
      && (data as PortalDirectLoginTokenMessage).type === PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE
      && (data as PortalDirectLoginTokenMessage).terminal === terminal
      && typeof (data as PortalDirectLoginTokenMessage).token === 'string'
      && (data as PortalDirectLoginTokenMessage).token,
  );
}

const DirectLoginPage: React.FC = () => {
  const location = useLocation();
  const terminal = useMemo(() => getPortalTerminal(location.pathname), [location.pathname]);
  const [state, setState] = useState<DirectLoginState>({ status: 'loading' });

  useEffect(() => {
    let mounted = true;
    let consumed = false;

    if (!terminal) {
      setState({ status: 'error', message: 'Invalid terminal' });
      return () => {
        mounted = false;
      };
    }

    const openerOrigin = resolvePortalDirectLoginOpenerOrigin(location.search);
    const postConsumeResult = (status: PortalDirectLoginResultMessage['status'], ticketId?: number, message?: string) => {
      const payload: PortalDirectLoginResultMessage = {
        type: PORTAL_DIRECT_LOGIN_RESULT_MESSAGE,
        terminal,
        status,
        ticketId,
        message,
      };
      window.opener?.postMessage(payload, openerOrigin);
    };

    const consumeToken = async (message: PortalDirectLoginTokenMessage) => {
      try {
        const response = await PORTAL_SERVICE[terminal].directLogin(message.token);
        if (response.code !== 200 || !persistPortalLogin(response.data, terminal)) {
          const errorMessage = response.msg || 'Direct login terminal mismatch';
          postConsumeResult('error', message.ticketId, errorMessage);
          setState({ status: 'error', message: errorMessage });
          return;
        }
        postConsumeResult('success', message.ticketId);
        if (mounted) {
          setState({ status: 'success' });
          history.replace(PORTAL_META[terminal].homePath);
        }
      } catch (error) {
        console.log(error);
        postConsumeResult('error', message.ticketId, 'Direct login failed');
        if (mounted) {
          setState({ status: 'error', message: 'Direct login failed' });
        }
      }
    };

    const handleTokenMessage = (event: MessageEvent) => {
      if (
        event.source !== window.opener
        || event.origin !== openerOrigin
        || !isDirectLoginTokenMessage(event.data, terminal)
        || consumed
      ) {
        return;
      }
      consumed = true;
      void consumeToken(event.data);
    };

    window.addEventListener('message', handleTokenMessage);
    window.opener?.postMessage({ type: PORTAL_DIRECT_LOGIN_READY_MESSAGE, terminal }, openerOrigin);
    const timeoutTimer = window.setTimeout(() => {
      if (!consumed && mounted) {
        setState({ status: 'error', message: 'Direct login token was not received' });
      }
    }, 5000);

    return () => {
      mounted = false;
      window.removeEventListener('message', handleTokenMessage);
      window.clearTimeout(timeoutTimer);
    };
  }, [location.search, terminal]);

  if (state.status === 'loading') {
    return (
      <div style={pageStyle}>
        <Spin description="Entering portal" />
      </div>
    );
  }

  if (state.status === 'success') {
    return (
      <div style={pageStyle}>
        <Result status="success" title="Login succeeded" />
      </div>
    );
  }

  return (
    <div style={pageStyle}>
      <Result
        status="error"
        title="Direct login failed"
        subTitle={state.message}
        extra={[
          <Button key="login" type="primary" onClick={() => history.replace(terminal ? PORTAL_META[terminal].loginPath : '/seller/login')}>
            Back to portal login
          </Button>,
        ]}
      />
    </div>
  );
};

export default DirectLoginPage;
