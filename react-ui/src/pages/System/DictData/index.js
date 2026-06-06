import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { message } from '@/utils/feedback';
import { useState, useRef, useEffect } from 'react';
import { useIntl, FormattedMessage, useAccess, history, useParams } from '@umijs/max';
import { Button, Modal } from 'antd';
import { FooterToolbar, PageContainer, ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import { PlusOutlined, DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { getDictDataList, removeDictData, addDictData, updateDictData, exportDictData } from '@/services/system/dictdata';
import UpdateForm from './edit';
import { getDictValueEnum, getDictType, getDictTypeOptionSelect } from '@/services/system/dict';
import DictTag from '@/components/DictTag';
/**
 * 添加节点
 *
 * @param fields
 */
const handleAdd = async (fields) => {
    const hide = message.loading('正在添加');
    try {
        const resp = await addDictData({ ...fields });
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
        const resp = await updateDictData(fields);
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
        const resp = await removeDictData(selectedRows.map((row) => row.dictCode).join(','));
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
        const params = [selectedRow.dictCode];
        const resp = await removeDictData(params.join(','));
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
        await exportDictData();
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
const DictDataTableList = () => {
    const formTableRef = { current: undefined };
    const [dictId, setDictId] = useState('');
    const [dictType, setDictType] = useState('');
    const [modalVisible, setModalVisible] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const [dictTypeOptions, setDictTypeOptions] = useState([]);
    const [statusOptions, setStatusOptions] = useState([]);
    const access = useAccess();
    /** 国际化配置 */
    const intl = useIntl();
    const params = useParams();
    if (params.id === undefined) {
        history.push('/system/dict');
    }
    const id = params.id || '0';
    useEffect(() => {
        if (dictId !== id) {
            setDictId(id);
            getDictTypeOptionSelect().then((res) => {
                if (res.code === 200) {
                    const opts = {};
                    res.data.forEach((item) => {
                        opts[item.dictType] = item.dictName;
                    });
                    setDictTypeOptions(opts);
                }
            });
            getDictValueEnum('sys_normal_disable').then((data) => {
                setStatusOptions(data);
            });
            getDictType(id).then((res) => {
                if (res.code === 200) {
                    setDictType(res.data.dictType);
                    formTableRef.current?.setFieldsValue({
                        dictType: res.data.dictType,
                    });
                    actionRef.current?.reloadAndRest?.();
                }
                else {
                    message.error(res.msg);
                }
            });
        }
    }, [dictId, dictType, params]);
    const columns = [
        {
            title: _jsx(FormattedMessage, { id: "system.dict.data.dict_code", defaultMessage: "\u5B57\u5178\u7F16\u7801" }),
            dataIndex: 'dictCode',
            valueType: 'text',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.dict.data.dict_label", defaultMessage: "\u5B57\u5178\u6807\u7B7E" }),
            dataIndex: 'dictLabel',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.dict.data.dict_type", defaultMessage: "\u5B57\u5178\u7C7B\u578B" }),
            dataIndex: 'dictType',
            valueType: 'select',
            hideInTable: true,
            valueEnum: dictTypeOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            search: {
                transform: (value) => {
                    setDictType(value);
                    return value;
                },
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "system.dict.data.dict_value", defaultMessage: "\u5B57\u5178\u952E\u503C" }),
            dataIndex: 'dictValue',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.dict.data.dict_sort", defaultMessage: "\u5B57\u5178\u6392\u5E8F" }),
            dataIndex: 'dictSort',
            valueType: 'text',
        },
        {
            title: _jsx(FormattedMessage, { id: "system.dict.data.status", defaultMessage: "\u72B6\u6001" }),
            dataIndex: 'status',
            valueType: 'select',
            valueEnum: statusOptions,
            fieldProps: SEARCHABLE_SELECT_PROPS,
            render: (_, record) => {
                return (_jsx(DictTag, { enums: statusOptions, value: record.status }));
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "system.dict.data.remark", defaultMessage: "\u5907\u6CE8" }),
            dataIndex: 'remark',
            valueType: 'textarea',
            search: false,
        },
        {
            title: _jsx(FormattedMessage, { id: "system.dict.data.create_time", defaultMessage: "\u521B\u5EFA\u65F6\u95F4" }),
            dataIndex: 'createTime',
            colSize: 2,
            valueType: 'dateRange',
            render: (_, record) => {
                return (_jsxs("span", { children: [record.createTime.toString(), " "] }));
            },
            search: {
                transform: (value) => {
                    return {
                        'params[beginTime]': value[0],
                        'params[endTime]': value[1],
                    };
                },
            },
        },
        {
            title: _jsx(FormattedMessage, { id: "pages.searchTable.titleOption", defaultMessage: "\u64CD\u4F5C" }),
            dataIndex: 'option',
            width: '120px',
            valueType: 'option',
            render: (_, record) => [
                _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('system:data:edit'), onClick: () => {
                        setModalVisible(true);
                        setCurrentRow(record);
                    }, children: "\u7F16\u8F91" }, "edit"),
                _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms('system:data:remove'), onClick: async () => {
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
                    }), actionRef: actionRef, formRef: formTableRef, rowKey: "dictCode", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:data:add'), onClick: async () => {
                                setCurrentRow({ dictType: dictType, isDefault: 'N', status: '0' });
                                setModalVisible(true);
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "pages.searchTable.new", defaultMessage: "\u65B0\u5EFA" })] }, "add"),
                        _jsxs(Button, { type: "primary", danger: true, hidden: selectedRows?.length === 0 || !access.hasPerms('system:data:remove'), onClick: async () => {
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
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('system:data:export'), onClick: async () => {
                                handleExport();
                            }, children: [_jsx(PlusOutlined, {}), _jsx(FormattedMessage, { id: "pages.searchTable.export", defaultMessage: "\u5BFC\u51FA" })] }, "export"),
                    ], request: (params) => {
                        const { current, pageSize, ...rest } = params;
                        return getDictDataList({ ...rest, dictType, pageNum: current, pageSize }).then((res) => {
                            const result = {
                                data: res.rows,
                                total: res.total,
                                success: true,
                            };
                            return result;
                        });
                    }, columns: columns, rowSelection: {
                        onChange: (_, selectedRows) => {
                            setSelectedRows(selectedRows);
                        },
                    } }, "dataList") }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { danger: true, hidden: !access.hasPerms('system:data:del'), onClick: async () => {
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
                    if (values.dictCode) {
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
                }, open: modalVisible, values: currentRow || {}, statusOptions: statusOptions })] }));
};
export default DictDataTableList;
