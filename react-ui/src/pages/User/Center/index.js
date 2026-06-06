import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ClusterOutlined, MailOutlined, TeamOutlined, UserOutlined, MobileOutlined, ManOutlined, } from '@ant-design/icons';
import { Card, Col, Divider, Row } from 'antd';
import { useEffect, useState } from 'react';
import styles from './Center.module.css';
import BaseInfo from './components/BaseInfo';
import ResetPassword from './components/ResetPassword';
import AvatarCropper from './components/AvatarCropper';
import { getUserInfo } from '@/services/session';
import { PageLoading } from '@ant-design/pro-components';
const operationTabList = [
    {
        key: 'base',
        tab: _jsx("span", { children: "\u57FA\u672C\u8D44\u6599" }),
    },
    {
        key: 'password',
        tab: _jsx("span", { children: "\u91CD\u7F6E\u5BC6\u7801" }),
    },
];
const DEFAULT_AVATAR = 'https://gw.alipayobjects.com/zos/rmsportal/BiazfanxmamNRoxxVxka.png';
const sexMap = {
    '0': '男',
    '1': '女',
    '2': '未知',
};
function InfoItem({ icon, label, value, }) {
    return (_jsxs("div", { className: styles.infoItem, children: [_jsxs("div", { className: styles.infoLabel, children: [icon, label] }), _jsx("div", { className: styles.infoValue, children: value || '-' })] }));
}
const Center = () => {
    const [tabKey, setTabKey] = useState('base');
    const [cropperModalOpen, setCropperModalOpen] = useState(false);
    const [userInfo, setUserInfo] = useState();
    const [loading, setLoading] = useState(true);
    useEffect(() => {
        getUserInfo()
            .then((data) => {
            if (!data?.user) {
                setUserInfo(undefined);
                return;
            }
            setUserInfo({
                ...data,
                user: {
                    ...data.user,
                    avatar: data.user.avatar || DEFAULT_AVATAR,
                },
            });
        })
            .finally(() => {
            setLoading(false);
        });
    }, []);
    if (loading) {
        return _jsx(PageLoading, {});
    }
    const currentUser = userInfo?.user;
    const renderUserInfo = ({ userName, phonenumber, email, sex, dept, }) => (_jsxs("div", { className: styles.infoList, children: [_jsx(InfoItem, { icon: _jsx(UserOutlined, {}), label: "\u7528\u6237\u540D", value: userName }), _jsx(InfoItem, { icon: _jsx(ManOutlined, {}), label: "\u6027\u522B", value: sexMap[`${sex}`] || '-' }), _jsx(InfoItem, { icon: _jsx(MobileOutlined, {}), label: "\u7535\u8BDD", value: phonenumber }), _jsx(InfoItem, { icon: _jsx(MailOutlined, {}), label: "\u90AE\u7BB1", value: email }), _jsx(InfoItem, { icon: _jsx(ClusterOutlined, {}), label: "\u90E8\u95E8", value: dept?.deptName })] }));
    const renderChildrenByTabKey = (tabValue) => {
        if (tabValue === 'base') {
            return _jsx(BaseInfo, { values: currentUser });
        }
        if (tabValue === 'password') {
            return _jsx(ResetPassword, {});
        }
        return null;
    };
    if (!currentUser) {
        return _jsx(PageLoading, {});
    }
    return (_jsxs("div", { children: [_jsxs(Row, { gutter: [16, 24], children: [_jsx(Col, { lg: 8, md: 24, children: _jsx(Card, { title: "\u4E2A\u4EBA\u4FE1\u606F", variant: "borderless", loading: loading, children: !loading && (_jsxs("div", { style: { textAlign: 'center' }, children: [_jsx("div", { className: styles.avatarHolder, onClick: () => setCropperModalOpen(true), children: _jsx("img", { alt: "", src: currentUser.avatar }) }), renderUserInfo(currentUser), _jsx(Divider, { dashed: true }), _jsxs("div", { className: styles.team, children: [_jsx("div", { className: styles.teamTitle, children: "\u89D2\u8272" }), _jsx(Row, { gutter: 36, children: currentUser.roles &&
                                                    currentUser.roles.map((item) => (_jsxs(Col, { lg: 24, xl: 12, children: [_jsx(TeamOutlined, { style: {
                                                                    marginRight: 8,
                                                                } }), item.roleName] }, item.roleId))) })] })] })) }) }), _jsx(Col, { lg: 16, md: 24, children: _jsx(Card, { variant: "borderless", tabList: operationTabList, activeTabKey: tabKey, onTabChange: (_tabKey) => {
                                setTabKey(_tabKey);
                            }, children: renderChildrenByTabKey(tabKey) }) })] }), _jsx(AvatarCropper, { onFinished: () => {
                    setCropperModalOpen(false);
                }, open: cropperModalOpen, data: currentUser.avatar })] }));
};
export default Center;
