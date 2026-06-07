import { jsx as _jsx } from "react/jsx-runtime";
import { Button, Result, Spin } from 'antd';
import { history, useLocation } from '@umijs/max';
import { useEffect, useMemo, useState } from 'react';
import { PORTAL_DIRECT_LOGIN_READY_MESSAGE, PORTAL_DIRECT_LOGIN_RESULT_MESSAGE, PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE, } from '@/utils/portalDirectLoginMessage';
import { clearPortalLogin, getPortalTerminal, persistPortalLogin, PORTAL_META, PORTAL_SERVICE, } from '../terminal';
const pageStyle = {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    background: '#f5f7fb',
};
function isDirectLoginTokenMessage(data, terminal) {
    return Boolean(data
        && typeof data === 'object'
        && data.type === PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE
        && data.terminal === terminal
        && typeof data.token === 'string'
        && data.token);
}
function resolveOpenerOrigin() {
    try {
        return document.referrer ? new URL(document.referrer).origin : window.location.origin;
    }
    catch {
        return window.location.origin;
    }
}
const DirectLoginPage = () => {
    const location = useLocation();
    const terminal = useMemo(() => getPortalTerminal(location.pathname), [location.pathname]);
    const [state, setState] = useState({ status: 'loading' });
    useEffect(() => {
        let mounted = true;
        let consumed = false;
        if (!terminal) {
            setState({ status: 'error', message: 'Invalid terminal' });
            return () => {
                mounted = false;
            };
        }
        clearPortalLogin(terminal);
        const openerOrigin = resolveOpenerOrigin();
        const postConsumeResult = (status, ticketId, message) => {
            const payload = {
                type: PORTAL_DIRECT_LOGIN_RESULT_MESSAGE,
                terminal,
                status,
                ticketId,
                message,
            };
            window.opener?.postMessage(payload, openerOrigin);
        };
        const consumeToken = async (message) => {
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
            }
            catch (error) {
                console.log(error);
                clearPortalLogin(terminal);
                postConsumeResult('error', message.ticketId, 'Direct login failed');
                if (mounted) {
                    setState({ status: 'error', message: 'Direct login failed' });
                }
            }
        };
        const handleTokenMessage = (event) => {
            if (event.source !== window.opener
                || event.origin !== openerOrigin
                || !isDirectLoginTokenMessage(event.data, terminal)
                || consumed) {
                return;
            }
            consumed = true;
            void consumeToken(event.data);
        };
        window.addEventListener('message', handleTokenMessage);
        window.opener?.postMessage({ type: PORTAL_DIRECT_LOGIN_READY_MESSAGE, terminal }, openerOrigin);
        const timeoutTimer = window.setTimeout(() => {
            if (!consumed && mounted) {
                clearPortalLogin(terminal);
                setState({ status: 'error', message: 'Direct login token was not received' });
            }
        }, 5000);
        return () => {
            mounted = false;
            window.removeEventListener('message', handleTokenMessage);
            window.clearTimeout(timeoutTimer);
        };
    }, [terminal]);
    if (state.status === 'loading') {
        return (_jsx("div", { style: pageStyle, children: _jsx(Spin, { description: "Entering portal" }) }));
    }
    if (state.status === 'success') {
        return (_jsx("div", { style: pageStyle, children: _jsx(Result, { status: "success", title: "Login succeeded" }) }));
    }
    return (_jsx("div", { style: pageStyle, children: _jsx(Result, { status: "error", title: "Direct login failed", subTitle: state.message, extra: [
                _jsx(Button, { type: "primary", onClick: () => history.replace(terminal ? PORTAL_META[terminal].loginPath : '/seller/login'), children: "Back to portal login" }, "login"),
            ] }) }));
};
export default DirectLoginPage;
