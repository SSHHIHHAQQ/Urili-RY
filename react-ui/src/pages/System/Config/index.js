import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess } from '@umijs/max';
import { Button, Modal } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, ExclamationCircleOutlined, ReloadOutlined, DownloadOutlined } from '@ant-design/icons';
import { getConfigList, removeConfig, addConfig, updateConfig, exportConfig, refreshConfigCache } from '@/services/system/config';
import UpdateForm from './edit';
import { getDictValueEnum } from '@/services/system/dict';
import DictTag from '@/components/DictTag';
/**
 * 添加节点
 *
 * @param fields
 */
const handleAdd = async (fields) => {
    const hide = message.loading('正在添加');
    try {
        const resp = await addConfig({ ...fields });
        hide();
        if (resp.code === 200) {
            message.success('添加成功');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
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
    const hide = message.loading('正在更新');
    try {
        const resp = await updateConfig(fields);
        hide();
        if (resp.code === 200) {
            message.success('更新成功');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
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
        const resp = await removeConfig(selectedRows.map((row) => row.configId).join(','));
        hide();
        if (resp.code === 200) {
            message.success('删除成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
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
        const params = [selectedRow.configId];
        const resp = await removeConfig(params.join(','));
        hide();
        if (resp.code === 200) {
            message.success('删除成功，即将刷新');
        }
        else {
            message.error(resp.msg);
        }
        return true;
    }
    catch (error) {
        hide();
        message.error('删除失败，请重试');
        return false;
    }
};
/**
 * 导出数据
 *
 *
 */
const handleExport = async () => {
    const hide = message.loading('正在导出');
    try {
        await exportConfig();
        hide();
        message.success('导出成功');
        return true;
    }
    catch (error) {
        hide();
        message.error('导出失败，请重试');
        return false;
    }
};
const handleRefreshCache = async () => {
    const hide = message.loading('正在刷新');
    try {
        await refreshConfigCache();
        hide();
        message.success('刷新成功');
        return true;
    }
    catch (error) {
        hide();
        message.error('刷新失败，请重试');
        return false;
    }
};
const ConfigTableList = () => {
    const formTableRef = { current: undefined };
    const [modalVisible, setModalVisible] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const [configTypeOptions, setConfigTypeOptions] = useState([]);
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    useEffect(() => {
        getDictValueEnum('sys_yes_no').then((data) => {
            setConfigTypeOptions(data);
        });
    }, []);
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "system.config.config_id", defaultMessage: "\u53C2\u6570\u4E3B\u952E" }),
            dataIndex: 'configId',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.config.config_name", defaultMessage: "\u53C2\u6570\u540D\u79F0" }),
            dataIndex: 'configName',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.config.config_key", defaultMessage: "\u53C2\u6570\u952E\u540D" }),
            dataIndex: 'configKey',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.config.config_value", defaultMessage: "\u53C2\u6570\u952E\u503C" }),
            dataIndex: 'configValue',
            valueType: 'textarea',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.config.config_type", defaultMessage: "\u7CFB\u7EDF\u5185\u7F6E" }),
            dataIndex: 'configType',
            valueType: 'select',
            valueEnum: configTypeOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: configTypeOptions, value: record.configType }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "system.config.remark", defaultMessage: "\u5907\u6CE8" }),
            dataIndex: 'remark',
            valueType: 'textarea',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "pages.searchTable.titleOption", defaultMessage: "\u64CD\u4F5C" }),
            dataIndex: 'option',
            width: '120px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('system:config:edit'), onClick: () => {
                        setModalVisible(true);
                        setCurrentRow(record);
                    }, children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('system:config:remove'), onClick: async () => {
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
                    }), actionRef: actionRef, formRef: formTableRef, rowKey: "configId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:config:add'), onClick: async () => {
                                setCurrentRow(undefined);
                                setModalVisible(true);
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "pages.searchTable.new", defaultMessage: "\u65B0\u5EFA" })] }, "add"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:config:remove'), onClick: async () => {
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
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:config:export'), onClick: async () => {
                                handleExport();
                            }, children: [_jsx(DownloadOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.export", defaultMessage: "\u5BFC\u51FA" })] }, "export"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: !access.hasPerms('system:config:remove'), onClick: async () => {
                                handleRefreshCache();
                            }, children: [_jsx(ReloadOutlined, {}), _jsx(FormattedMessage, { id: "system.config.refreshCache", defaultMessage: "\u5237\u65B0\u7F13\u5B58" })] }, "refresh"),
                    ], request: (params) => getConfigList({ ...params }).then((res) => {
                        const result = {
                            data: res.rows,
                            total: res.total,
                            success: true,
                        };
                        return result;
                    }), columns: columns, rowSelection: {
                        onChange: (_, selectedRows) => {
                            setSelectedRows(selectedRows);
                        },
                    } }, "configList") }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { danger: true, hidden: !access.hasPerms('system:config:del'), onClick: async () => {
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
                    }, children: _jsx(FormattedMessage, { id: "pages.searchTable.batchDeletion", defaultMessage: "\u6279\u91CF\u5220\u9664" }) }, "remove") })), _jsx(UpdateForm, { onSubmit: async (values) => {
                    let success = false;
                    if (values.configId) {
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
                }, open: modalVisible, values: currentRow || {}, configTypeOptions: configTypeOptions })] }));
};
export default ConfigTableList;
