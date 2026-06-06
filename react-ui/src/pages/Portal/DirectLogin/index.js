import { jsx as _jsx } from "react/jsx-runtime";
import { Button, Result, Spin } from 'antd';
import { history, useLocation } from '@umijs/max';
import { useEffect, useMemo, useState } from 'react';
import { clearPortalLogin, getPortalTerminal, persistPortalLogin, PORTAL_META, PORTAL_SERVICE, } from '../terminal';
const pageStyle = {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    background: '#f5f7fb',
};
const DirectLoginPage = () => {
    const location = useLocation();
    const terminal = useMemo(() => getPortalTerminal(location.pathname), [location.pathname]);
    const [state, setState] = useState({ status: 'loading' });
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
            }
            catch (error) {
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
        return (_jsx("div", { style: pageStyle, children: _jsx(Spin, { description: terminal ? `正在进入${PORTAL_META[terminal].label}` : '正在进入' }) }));
    }
    if (state.status === 'success') {
        return (_jsx("div", { style: pageStyle, children: _jsx(Result, { status: "success", title: "\u767B\u5F55\u6210\u529F" }) }));
    }
    return (_jsx("div", { style: pageStyle, children: _jsx(Result, { status: "error", title: "\u514D\u5BC6\u767B\u5F55\u5931\u8D25", subTitle: state.message, extra: [
                _jsx(Button, { type: "primary", onClick: () => history.replace('/user/login'), children: "\u8FD4\u56DE\u7BA1\u7406\u7AEF\u767B\u5F55" }, "login"),
            ] }) }));
};
export default DirectLoginPage;
