import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess } from '@umijs/max';
import { Button, Modal } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch, getProTableScroll } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, ExclamationCircleOutlined, PoweroffOutlined, StopOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { getMenuList, removeMenu, addMenu, updateMenu, cascadeMenuStatus, cascadeMenuVisible } from '@/services/system/menu';
import UpdateForm from './edit';
import { getDictValueEnum } from '@/services/system/dict';
import { buildTreeData } from '@/utils/tree';
import DictTag from '@/components/DictTag';
/**
 * 添加节点
 *
 * @param fields
 */
const handleAdd = async (fields) => {
    const hide = message.loading('正在添加');
    try {
        await addMenu({ ...fields });
        hide();
        message.success('添加成功');
        return true;
    }
    catch {
        hide();
        message.error('添加失败请重试！');
        return false;
    }
};
/**
 * 更新节点
 *
 * @param fields
 */
const handleUpdate = async (fields) => {
    const hide = message.loading('正在配置');
    try {
        await updateMenu(fields);
        hide();
        message.success('配置成功');
        return true;
    }
    catch {
        hide();
        message.error('配置失败请重试！');
        return false;
    }
};
/**
 * 删除节点
 *
 * @param selectedRows
 */
const handleRemove = async (selectedRows) => {
    const hide = message.loading('正在删除');
    if (!selectedRows)
        return true;
    try {
        await removeMenu(selectedRows.map((row) => row.menuId).join(','));
        hide();
        message.success('删除成功，即将刷新');
        return true;
    }
    catch {
        hide();
        message.error('删除失败，请重试');
        return false;
    }
};
const handleRemoveOne = async (selectedRow) => {
    const hide = message.loading('正在删除');
    if (!selectedRow)
        return true;
    try {
        const params = [selectedRow.menuId];
        await removeMenu(params.join(','));
        hide();
        message.success('删除成功，即将刷新');
        return true;
    }
    catch {
        hide();
        message.error('删除失败，请重试');
        return false;
    }
};
const handleCascadeStatus = async (selectedRows, status) => {
    const actionText = status === '0' ? '启用' : '停用';
    if (!selectedRows?.length)
        return true;
    const hide = message.loading(`正在级联${actionText}`);
    try {
        await cascadeMenuStatus(selectedRows.map((row) => row.menuId).join(','), status);
        hide();
        message.success(`级联${actionText}成功，即将刷新`);
        return true;
    }
    catch {
        hide();
        message.error(`级联${actionText}失败，请重试`);
        return false;
    }
};
const handleCascadeVisible = async (selectedRows, visible) => {
    const actionText = visible === '0' ? '显示' : '隐藏';
    if (!selectedRows?.length)
        return true;
    const hide = message.loading(`正在级联${actionText}`);
    try {
        await cascadeMenuVisible(selectedRows.map((row) => row.menuId).join(','), visible);
        hide();
        message.success(`级联${actionText}成功，即将刷新`);
        return true;
    }
    catch {
        hide();
        message.error(`级联${actionText}失败，请重试`);
        return false;
    }
};
const MenuTableList = () => {
    const [modalVisible, setModalVisible] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const [menuTree, setMenuTree] = useState([]);
    const [visibleOptions, setVisibleOptions] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    useEffect(() => {
        getDictValueEnum('sys_show_hide').then((data) => {
            setVisibleOptions(data);
        });
        getDictValueEnum('sys_normal_disable').then((data) => {
            setStatusOptions(data);
        });
    }, []);
    const showCascadeStatusConfirm = (status) => {
        const actionText = status === '0' ? '启用' : '停用';
        Modal.confirm({
            title: `确认级联${actionText}所选菜单吗？`,
            icon: _jsx(ExclamationCircleOutlined, {}),
            content: `将同时${actionText}选中菜单及其所有下级菜单、按钮权限，请谨慎操作。`,
            okText: '确认',
            cancelText: '取消',
            onOk: async () => {
                const success = await handleCascadeStatus(selectedRows, status);
                if (success) {
                    setSelectedRows([]);
                    actionRef.current?.reloadAndRest?.();
                }
            },
        });
    };
    const showCascadeVisibleConfirm = (visible) => {
        const actionText = visible === '0' ? '显示' : '隐藏';
        Modal.confirm({
            title: `确认级联${actionText}所选菜单吗？`,
            icon: _jsx(ExclamationCircleOutlined, {}),
            content: `将同时${actionText}选中菜单及其所有下级菜单、按钮权限，请谨慎操作。`,
            okText: '确认',
            cancelText: '取消',
            onOk: async () => {
                const success = await handleCascadeVisible(selectedRows, visible);
                if (success) {
                    setSelectedRows([]);
                    actionRef.current?.reloadAndRest?.();
                }
            },
        });
    };
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "system.menu.menu_name", defaultMessage: "\u83DC\u5355\u540D\u79F0" }),
            dataIndex: 'menuName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.menu.icon", defaultMessage: "\u83DC\u5355\u56FE\u6807" }),
            dataIndex: 'icon',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.menu.order_num", defaultMessage: "\u663E\u793A\u987A\u5E8F" }),
            dataIndex: 'orderNum',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.menu.component", defaultMessage: "\u7EC4\u4EF6\u8DEF\u5F84" }),
            dataIndex: 'component',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.menu.perms", defaultMessage: "\u6743\u9650\u6807\u8BC6" }),
            dataIndex: 'perms',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.menu.visible", defaultMessage: "\u663E\u793A\u72B6\u6001" }),
            dataIndex: 'visible',
            valueType: 'select',
            valueEnum: visibleOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: visibleOptions, value: record.visible }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "system.menu.status", defaultMessage: "\u83DC\u5355\u72B6\u6001" }),
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: statusOptions, value: record.status }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "pages.searchTable.titleOption", defaultMessage: "\u64CD\u4F5C" }),
            dataIndex: 'option',
            width: '220px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('system:menu:edit'), onClick: () => {
                        setModalVisible(true);
                        setCurrentRow(record);
                    }, children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('system:menu:remove'), onClick: async () => {
                        Modal.confirm({
                            title: '删除',
                            content: '确定删除该项吗？',
                            okText: '确认',
                            cancelText: '取消',
                            onOk: async () => {
                                const success = await handleRemoveOne(record);
                                if (success) {
                                    if (actionRef.current) {
                                        actionRef.current.reload();
                                    }
                                }
                            },
                        });
                    }, children: "\u5220\u9664" }, "batchRemove"),
            ],
        },
    ];
    return (_jsxs(PageContainer, { children: [_jsx("div", { style: { width: '100%', float: 'right' }, children: _jsx(ProTable, { headerTitle: intl.formatMessage({
                        id: 'pages.searchTable.title',
                        defaultMessage: '信息',
                    }), actionRef: actionRef, rowKey: "menuId", search: getPersistedProTableSearch({ labelWidth: 120 }), scroll: getProTableScroll(1300), tableAlertRender: false, tableAlertOptionRender: false, toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:menu:add'), onClick: async () => {
                                setCurrentRow(undefined);
                                setModalVisible(true);
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "pages.searchTable.new", defaultMessage: "\u65B0\u5EFA" })] }, "add"),
                        _jsxs(Button, { hidden: selectedRows?.length === 0 || !access.hasPerms('system:menu:edit'), onClick: () => showCascadeStatusConfirm('0'), children: [_jsx(PoweroffOutlined, {}), "\u7EA7\u8054\u542F\u7528"] }, "cascadeEnable"),
                        _jsxs(Button, { danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:menu:edit'), onClick: () => showCascadeStatusConfirm('1'), children: [_jsx(StopOutlined, {}), "\u7EA7\u8054\u505C\u7528"] }, "cascadeDisable"),
                        _jsxs(Button, { hidden: selectedRows?.length === 0 || !access.hasPerms('system:menu:edit'), onClick: () => showCascadeVisibleConfirm('0'), children: [_jsx(EyeOutlined, {}), "\u7EA7\u8054\u663E\u793A"] }, "cascadeShow"),
                        _jsxs(Button, { danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:menu:edit'), onClick: () => showCascadeVisibleConfirm('1'), children: [_jsx(EyeInvisibleOutlined, {}), "\u7EA7\u8054\u9690\u85CF"] }, "cascadeHide"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:menu:remove'), onClick: async () => {
                                Modal.confirm({
                                    title: '是否确认删除所选数据项?',
                                    icon: _jsx(ExclamationCircleOutlined, {}),
                                    content: '请谨慎操作',
                                    async onOk() {
                                        const success = await handleRemove(selectedRows);
                                        if (success) {
                                            setSelectedRows([]);
                                            actionRef.current?.reloadAndRest?.();
                                        }
                                    },
                                    onCancel() { },
                                });
                            }, children: [_jsx(DeleteOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.delete", defaultMessage: "\u5220\u9664" })] }, "remove"),
                    ], request: (params) => getMenuList({ ...params }).then((res) => {
                        const rootMenu = { id: 0, label: '主类目', children: [], value: 0 };
                        const memuData = buildTreeData(res.data, 'menuId', 'menuName', '', '', '');
                        rootMenu.children = memuData;
                        const treeData = [];
                        treeData.push(rootMenu);
                        setMenuTree(treeData);
                        return {
                            data: memuData,
                            total: res.data.length,
                            success: true,
                        };
                    }), columns: columns, rowSelection: {
                        onChange: (_, selectedRows) => {
                            setSelectedRows(selectedRows);
                        },
                    } }, "menuList") }), selectedRows?.length > 0 && (_jsxs(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: [_jsx(Button, { hidden: !access.hasPerms('system:menu:edit'), onClick: () => showCascadeStatusConfirm('0'), children: "\u7EA7\u8054\u542F\u7528" }, "cascadeEnable"), _jsx(Button, { danger: true, hidden: !access.hasPerms('system:menu:edit'), onClick: () => showCascadeStatusConfirm('1'), children: "\u7EA7\u8054\u505C\u7528" }, "cascadeDisable"), _jsx(Button, { hidden: !access.hasPerms('system:menu:edit'), onClick: () => showCascadeVisibleConfirm('0'), children: "\u7EA7\u8054\u663E\u793A" }, "cascadeShow"), _jsx(Button, { danger: true, hidden: !access.hasPerms('system:menu:edit'), onClick: () => showCascadeVisibleConfirm('1'), children: "\u7EA7\u8054\u9690\u85CF" }, "cascadeHide"), _jsx(Button, { danger: true, hidden: !access.hasPerms('system:menu:del'), onClick: async () => {
                            Modal.confirm({
                                title: '删除',
                                content: '确定删除该项吗？',
                                okText: '确认',
                                cancelText: '取消',
                                onOk: async () => {
                                    const success = await handleRemove(selectedRows);
                                    if (success) {
                                        setSelectedRows([]);
                                        actionRef.current?.reloadAndRest?.();
                                    }
                                },
                            });
                        }, children: _jsx(FormattedMessage, { id: "pages.searchTable.batchDeletion", defaultMessage: "\u6279\u91CF\u5220\u9664" }) }, "remove")] })), _jsx(UpdateForm, { onSubmit: async (values) => {
                    let success = false;
                    if (values.menuId) {
                        success = await handleUpdate({ ...values });
                    }
                    else {
                        success = await handleAdd({ ...values });
                    }
                    if (success) {
                        setModalVisible(false);
                        setCurrentRow(undefined);
                        if (actionRef.current) {
                            actionRef.current.reload();
                        }
                    }
                }, onCancel: () => {
                    setModalVisible(false);
                    setCurrentRow(undefined);
                }, open: modalVisible, values: currentRow || {}, visibleOptions: visibleOptions, statusOptions: statusOptions, menuTree: menuTree })] }));
};
export default MenuTableList;
