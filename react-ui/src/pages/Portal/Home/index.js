import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Card, Descriptions, Empty, Form, Grid, Input, Modal, Space, Spin, Table, Tag, Typography, } from 'antd';
import { LockOutlined, LogoutOutlined, ReloadOutlined } from '@ant-design/icons';
import { history, useLocation } from '@umijs/max';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getTerminalAccessToken } from '@/access';
import { message } from '@/utils/feedback';
import { clearPortalLogin, getPortalTerminal, PORTAL_META, PORTAL_SERVICE, } from '../terminal';
import BuyerDistributionProductList from './BuyerDistributionProductList';
import BuyerProductSchemaPreview from './BuyerProductSchemaPreview';
import SellerOwnDistributionProductList from './SellerOwnDistributionProductList';
import SellerProductSchemaPreview from './SellerProductSchemaPreview';
const pageStyle = {
    minHeight: '100vh',
    padding: 24,
    background: '#f5f7fb',
};
const headerStyle = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: 16,
    marginBottom: 16,
};
const gridStyle = {
    display: 'grid',
    gap: 16,
};
const fullGridStyle = {
    gridColumn: '1 / -1',
};
function displayText(value) {
    return value === undefined || value === null || value === '' ? '-' : String(value);
}
function renderTags(values) {
    if (!values || values.length === 0) {
        return _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE });
    }
    return (_jsx(Space, { wrap: true, size: [6, 6], children: values.map((value) => (_jsx(Tag, { children: value }, value))) }));
}
function renderRoleList(roles) {
    if (!roles || roles.length === 0) {
        return _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE });
    }
    return (_jsx(Space, { wrap: true, size: [6, 6], children: roles.map((role) => (_jsx(Tag, { children: role.roleName || role.roleKey || role.roleId }, role.roleId))) }));
}
function renderDeptList(depts) {
    if (!depts || depts.length === 0) {
        return _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE });
    }
    return (_jsx(Space, { wrap: true, size: [6, 6], children: depts.map((dept) => (_jsx(Tag, { children: dept.deptName || dept.deptId }, dept.deptId))) }));
}
function renderSessionStatus(record) {
    if (record.current) {
        return _jsx(Tag, { color: "processing", children: "\u5F53\u524D" });
    }
    if (record.logoutTime || record.status === '1') {
        return _jsx(Tag, { children: "\u5DF2\u9000\u51FA" });
    }
    if (record.status === '0') {
        return _jsx(Tag, { color: "success", children: "\u6709\u6548" });
    }
    return _jsx(Tag, { children: displayText(record.status) });
}
const PortalHomePage = () => {
    const location = useLocation();
    const screens = Grid.useBreakpoint();
    const terminal = useMemo(() => getPortalTerminal(location.pathname), [location.pathname]);
    const [loading, setLoading] = useState(true);
    const [sessionLoading, setSessionLoading] = useState(false);
    const [data, setData] = useState({});
    const [sessionRows, setSessionRows] = useState([]);
    const [passwordOpen, setPasswordOpen] = useState(false);
    const [passwordSubmitting, setPasswordSubmitting] = useState(false);
    const [passwordForm] = Form.useForm();
    const sessionRequestSeq = useRef(0);
    const loadData = useCallback(async (currentTerminal) => {
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
        }
        catch (error) {
            console.log(error);
            clearPortalLogin(currentTerminal);
            message.error('登录状态已失效');
            history.replace('/user/login');
        }
        finally {
            setLoading(false);
        }
    }, []);
    const loadSessions = useCallback(async (currentTerminal) => {
        const requestSeq = sessionRequestSeq.current + 1;
        sessionRequestSeq.current = requestSeq;
        setSessionLoading(true);
        try {
            const response = await PORTAL_SERVICE[currentTerminal].getSessions({ pageNum: 1, pageSize: 5 });
            if (sessionRequestSeq.current === requestSeq) {
                setSessionRows((response.rows || [])
                    .slice(0, 5)
                    .map((row, index) => ({
                    ...row,
                    uiRowKey: [
                        currentTerminal,
                        row.userName || '',
                        row.loginTime || '',
                        row.expireTime || '',
                        row.logoutTime || '',
                        row.status || '',
                        row.current ? 'current' : 'history',
                        index,
                    ].join('-'),
                })));
            }
        }
        catch (error) {
            console.log(error);
            if (sessionRequestSeq.current === requestSeq) {
                setSessionRows([]);
            }
        }
        finally {
            if (sessionRequestSeq.current === requestSeq) {
                setSessionLoading(false);
            }
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
        loadSessions(terminal);
    }, [loadData, loadSessions, terminal]);
    const handleLogout = async () => {
        if (!terminal) {
            return;
        }
        try {
            await PORTAL_SERVICE[terminal].logout();
        }
        catch (error) {
            console.log(error);
        }
        finally {
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
            }
            else {
                message.error(response.msg || '密码更新失败');
            }
        }
        finally {
            setPasswordSubmitting(false);
        }
    };
    if (!terminal) {
        return null;
    }
    const meta = PORTAL_META[terminal];
    const permissions = data.info?.permissions || [];
    const gridColumns = screens.lg ? 3 : screens.sm ? 2 : 1;
    const contentGridStyle = {
        ...gridStyle,
        gridTemplateColumns: `repeat(${gridColumns}, minmax(240px, 1fr))`,
    };
    const wideGridStyle = {
        gridColumn: gridColumns > 1 ? 'span 2' : '1 / -1',
    };
    const descriptionColumns = screens.md ? 2 : 1;
    const sessionColumns = [
        {
            title: '状态',
            key: 'status',
            width: 96,
            render: (_, record) => renderSessionStatus(record),
        },
        {
            title: '登录账号',
            dataIndex: 'userName',
            key: 'userName',
            ellipsis: true,
            render: displayText,
        },
        {
            title: '登录 IP',
            dataIndex: 'loginIp',
            key: 'loginIp',
            width: 140,
            responsive: ['sm'],
            render: displayText,
        },
        {
            title: '登录时间',
            dataIndex: 'loginTime',
            key: 'loginTime',
            width: 180,
            render: displayText,
        },
        {
            title: '过期时间',
            dataIndex: 'expireTime',
            key: 'expireTime',
            width: 180,
            responsive: ['md'],
            render: displayText,
        },
        {
            title: '退出时间',
            dataIndex: 'logoutTime',
            key: 'logoutTime',
            width: 180,
            responsive: ['md'],
            render: displayText,
        },
    ];
    return (_jsxs("div", { style: pageStyle, children: [_jsxs("div", { style: headerStyle, children: [_jsxs(Space, { orientation: "vertical", size: 0, children: [_jsx(Typography.Title, { level: 3, style: { margin: 0 }, children: meta.label }), _jsxs(Typography.Text, { type: "secondary", children: [displayText(data.info?.subjectNo), " / ", displayText(data.account?.userName)] })] }), _jsxs(Space, { wrap: true, children: [_jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: () => {
                                    loadData(terminal);
                                    loadSessions(terminal);
                                }, children: "\u5237\u65B0" }), _jsx(Button, { icon: _jsx(LockOutlined, {}), onClick: () => setPasswordOpen(true), children: "\u4FEE\u6539\u5BC6\u7801" }), _jsx(Button, { danger: true, icon: _jsx(LogoutOutlined, {}), onClick: handleLogout, children: "\u9000\u51FA" })] })] }), _jsx(Spin, { spinning: loading, children: _jsxs("div", { style: contentGridStyle, children: [_jsx(Card, { title: "\u4E3B\u4F53\u8D44\u6599", variant: "borderless", style: wideGridStyle, children: _jsxs(Descriptions, { column: descriptionColumns, size: "small", children: [_jsx(Descriptions.Item, { label: "\u4E3B\u4F53\u7F16\u53F7", children: displayText(data.subject?.subjectNo) }), _jsx(Descriptions.Item, { label: "\u4E3B\u4F53\u4EE3\u7801", children: displayText(data.subject?.subjectCode) }), _jsx(Descriptions.Item, { label: "\u4E3B\u4F53\u540D\u79F0", children: displayText(data.subject?.subjectName) }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: displayText(data.subject?.status) }), _jsx(Descriptions.Item, { label: "\u8054\u7CFB\u4EBA", children: displayText(data.subject?.contactName) }), _jsx(Descriptions.Item, { label: "\u90AE\u7BB1", children: displayText(data.subject?.contactEmail) })] }) }), _jsx(Card, { title: "\u5F53\u524D\u8D26\u53F7", variant: "borderless", children: _jsxs(Descriptions, { column: 1, size: "small", children: [_jsx(Descriptions.Item, { label: "\u8D26\u53F7", children: displayText(data.account?.userName) }), _jsx(Descriptions.Item, { label: "\u6635\u79F0", children: displayText(data.account?.nickName) }), _jsx(Descriptions.Item, { label: "\u90E8\u95E8", children: displayText(data.account?.deptName) }), _jsx(Descriptions.Item, { label: "\u72B6\u6001", children: displayText(data.account?.status) })] }) }), _jsx(Card, { title: "\u7AEF\u5185\u89D2\u8272", variant: "borderless", children: renderRoleList(data.roles) }), _jsx(Card, { title: "\u7AEF\u5185\u90E8\u95E8", variant: "borderless", children: renderDeptList(data.depts) }), _jsx(Card, { title: "\u7AEF\u5185\u8D26\u53F7", variant: "borderless", children: _jsx(Typography.Text, { children: data.accounts?.length || 0 }) }), terminal === 'seller' ? (_jsx("div", { style: fullGridStyle, children: _jsx(SellerProductSchemaPreview, {}) })) : terminal === 'buyer' ? (_jsx("div", { style: fullGridStyle, children: _jsx(BuyerProductSchemaPreview, {}) })) : null, terminal === 'seller' ? (_jsx("div", { style: fullGridStyle, children: _jsx(SellerOwnDistributionProductList, {}) })) : terminal === 'buyer' ? (_jsx("div", { style: fullGridStyle, children: _jsx(BuyerDistributionProductList, {}) })) : null, _jsx(Card, { title: "\u5F53\u524D\u8D26\u53F7\u4F1A\u8BDD", variant: "borderless", style: fullGridStyle, children: _jsx(Table, { size: "small", rowKey: "uiRowKey", loading: sessionLoading, pagination: false, columns: sessionColumns, dataSource: sessionRows, scroll: { x: 780 }, locale: { emptyText: _jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE }) } }) }), _jsx(Card, { title: "\u6743\u9650\u6807\u8BC6", variant: "borderless", style: fullGridStyle, children: renderTags(permissions) })] }) }), _jsx(Modal, { title: "\u4FEE\u6539\u5BC6\u7801", open: passwordOpen, onCancel: () => {
                    setPasswordOpen(false);
                    passwordForm.resetFields();
                }, onOk: handlePasswordSubmit, confirmLoading: passwordSubmitting, destroyOnHidden: true, children: _jsxs(Form, { form: passwordForm, layout: "vertical", preserve: false, children: [_jsx(Form.Item, { name: "oldPassword", label: "\u65E7\u5BC6\u7801", rules: [{ required: true, message: '请输入旧密码' }], children: _jsx(Input.Password, { autoComplete: "current-password" }) }), _jsx(Form.Item, { name: "newPassword", label: "\u65B0\u5BC6\u7801", rules: [{ required: true, message: '请输入新密码' }], children: _jsx(Input.Password, { autoComplete: "new-password" }) }), _jsx(Form.Item, { name: "confirmPassword", label: "\u786E\u8BA4\u5BC6\u7801", dependencies: ['newPassword'], rules: [
                                { required: true, message: '请再次输入新密码' },
                                ({ getFieldValue }) => ({
                                    validator(_, value) {
                                        if (!value || getFieldValue('newPassword') === value) {
                                            return Promise.resolve();
                                        }
                                        return Promise.reject(new Error('两次输入的新密码不一致'));
                                    },
                                }),
                            ], children: _jsx(Input.Password, { autoComplete: "new-password" }) })] }) })] }));
};
export default PortalHomePage;
