import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ArrowDownOutlined, ArrowUpOutlined, MenuOutlined, PlusOutlined, SaveOutlined, } from '@ant-design/icons';
import { Button, Empty, Input, Spin, Tooltip, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { systemKindText } from '../constants';
import { statusTag } from '../helpers';
import styles from '../style.module.css';
export default function ConnectionSidebar({ access, connections, loading, selectedCode, onCreate, onSaveOrder, onSelect, }) {
    const [keyword, setKeyword] = useState('');
    const [ordering, setOrdering] = useState(false);
    const [draftConnections, setDraftConnections] = useState([]);
    useEffect(() => {
        if (!ordering) {
            setDraftConnections(connections);
        }
    }, [connections, ordering]);
    const visibleConnections = useMemo(() => {
        const rows = ordering ? draftConnections : connections;
        const normalizedKeyword = keyword.trim().toLowerCase();
        if (!normalizedKeyword || ordering) {
            return rows;
        }
        return rows.filter((item) => [
            item.masterWarehouseName,
            item.connectionCode,
            systemKindText[item.systemKind || ''] || item.systemKind,
        ]
            .filter(Boolean)
            .some((value) => String(value).toLowerCase().includes(normalizedKeyword)));
    }, [connections, draftConnections, keyword, ordering]);
    const groups = useMemo(() => {
        const result = new Map();
        visibleConnections.forEach((item) => {
            const groupName = systemKindText[item.systemKind || ''] || item.systemKind || '其他系统';
            result.set(groupName, [...(result.get(groupName) || []), item]);
        });
        return Array.from(result.entries());
    }, [visibleConnections]);
    const moveConnection = (connectionCode, offset) => {
        const currentIndex = draftConnections.findIndex((item) => item.connectionCode === connectionCode);
        const nextIndex = currentIndex + offset;
        if (currentIndex < 0 ||
            nextIndex < 0 ||
            nextIndex >= draftConnections.length) {
            return;
        }
        const nextRows = [...draftConnections];
        const [current] = nextRows.splice(currentIndex, 1);
        nextRows.splice(nextIndex, 0, current);
        setDraftConnections(nextRows);
    };
    const renderConnection = (connection) => {
        const active = selectedCode === connection.connectionCode;
        const draftIndex = draftConnections.findIndex((item) => item.connectionCode === connection.connectionCode);
        return (_jsxs("div", { className: `${styles.connectionItem} ${active ? styles.connectionItemActive : ''}`, onClick: () => onSelect(connection), children: [_jsxs("div", { className: styles.connectionItemMain, children: [_jsx("div", { className: styles.connectionItemTitle, children: _jsx(Typography.Text, { strong: true, ellipsis: true, children: connection.masterWarehouseName }) }), _jsx("div", { className: styles.connectionItemCode, children: _jsx(Typography.Text, { type: "secondary", ellipsis: true, children: connection.connectionCode }) }), statusTag(connection.status)] }), ordering ? (_jsxs("div", { className: styles.connectionOrderActions, children: [_jsx(Tooltip, { title: "\u4E0A\u79FB", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(ArrowUpOutlined, {}), disabled: draftIndex <= 0, onClick: (event) => {
                                    event.stopPropagation();
                                    moveConnection(connection.connectionCode, -1);
                                } }) }), _jsx(Tooltip, { title: "\u4E0B\u79FB", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(ArrowDownOutlined, {}), disabled: draftIndex < 0 || draftIndex >= draftConnections.length - 1, onClick: (event) => {
                                    event.stopPropagation();
                                    moveConnection(connection.connectionCode, 1);
                                } }) })] })) : null] }, connection.connectionCode));
    };
    return (_jsxs("div", { className: styles.sidebar, children: [_jsx("div", { className: styles.sidebarHeader, children: _jsxs("div", { className: styles.sidebarTitleLine, children: [_jsx(Typography.Text, { strong: true, children: "\u4E3B\u4ED3\u63A5\u5165" }), _jsxs("div", { className: styles.sidebarActions, children: [_jsx(Tooltip, { title: ordering ? '保存排序' : '调整排序', children: _jsx(Button, { icon: ordering ? _jsx(SaveOutlined, {}) : _jsx(MenuOutlined, {}), size: "small", hidden: !access.hasPerms('integration:upstream:edit'), onClick: async () => {
                                            if (!ordering) {
                                                setOrdering(true);
                                                setDraftConnections(connections);
                                                return;
                                            }
                                            const ok = await onSaveOrder(draftConnections.map((item) => item.connectionCode));
                                            if (ok) {
                                                setOrdering(false);
                                            }
                                        } }) }), _jsx(Tooltip, { title: "\u65B0\u589E\u4E3B\u4ED3", children: _jsx(Button, { type: "primary", icon: _jsx(PlusOutlined, {}), size: "small", hidden: !access.hasPerms('integration:upstream:add'), onClick: onCreate }) })] })] }) }), _jsx("div", { className: styles.sidebarSearch, children: ordering ? (_jsx(Button, { block: true, size: "small", onClick: () => {
                        setOrdering(false);
                        setDraftConnections(connections);
                    }, children: "\u9000\u51FA\u6392\u5E8F" })) : (_jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u4E3B\u4ED3\u540D\u79F0/\u7F16\u53F7", value: keyword, onChange: (event) => setKeyword(event.target.value) })) }), _jsx("div", { className: styles.sidebarBody, children: _jsx(Spin, { spinning: loading, children: groups.length === 0 ? (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u4E3B\u4ED3" })) : (_jsx("div", { children: groups.map(([groupName, rows]) => (_jsxs("div", { className: styles.connectionGroup, children: [_jsx(Typography.Text, { type: "secondary", className: styles.connectionGroupLabel, children: groupName }), rows.map(renderConnection)] }, groupName))) })) }) }), _jsxs("div", { className: styles.sidebarFooter, children: [_jsxs(Typography.Text, { type: "secondary", children: ["\u5171 ", visibleConnections.length, " \u4E2A\u4E3B\u4ED3"] }), ordering ? (_jsx(Typography.Text, { type: "secondary", children: "\u6392\u5E8F\u4E2D" })) : null] })] }));
}
