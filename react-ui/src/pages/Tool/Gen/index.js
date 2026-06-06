import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { DownloadOutlined, DownOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Drawer, Modal, Card, Dropdown, Layout } from 'antd';
import { useState, useRef } from 'react';
import { history, FormattedMessage, useAccess } from '@umijs/max';
import PreviewForm from './components/PreviewCode';
import { message } from '@/utils/feedback';
import { batchGenCode, genCode, previewCode, getGenCodeList, removeData, syncDbInfo, } from './service';
import { FooterToolbar, ProDescriptions, ProTable, } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
const { Content } = Layout;
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
        await removeData({
            ids: selectedRows.map((row) => row.tableId),
        });
        hide();
        message.success('删除成功，即将刷新');
        return true;
    }
    catch (_error) {
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
        const params = [selectedRow.tableId];
        await removeData({
            ids: params,
        });
        hide();
        message.success('删除成功，即将刷新');
        return true;
    }
    catch (_error) {
        hide();
        message.error('删除失败，请重试');
        return false;
    }
};
const GenCodeView = () => {
    const formTableRef = { current: undefined };
    const [showDetail, setShowDetail] = useState(false);
    const [showPreview, setShowPreview] = useState(false);
    const [preivewData, setPreivewData] = useState(false);
    const actionRef = useRef(null);
    const [currentRow, setCurrentRow] = useState();
    const [selectedRows, setSelectedRows] = useState([]);
    const access = useAccess();
    const columns = [
        {
            title: '编号',
            dataIndex: 'tableId',
            render: (dom, entity) => {
                return (_jsx("a", { onClick: () => {
                        setCurrentRow(entity);
                        setShowDetail(true);
                    }, children: dom }));
            },
        },
        {
            title: '表名',
            dataIndex: 'tableName',
            valueType: 'textarea',
        },
        {
            title: '表描述',
            dataIndex: 'tableComment',
            hideInForm: true,
            search: false,
        },
        {
            title: '实体',
            dataIndex: 'className',
            valueType: 'textarea',
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            valueType: 'textarea',
            search: false,
        },
        {
            title: '更新时间',
            dataIndex: 'updateTime',
            valueType: 'textarea',
            search: false,
        },
        {
            title: '操作',
            dataIndex: 'option',
            width: '220px',
            valueType: 'option',
            render: (_, record) => {
                const moreItems = [];
                if (access.hasPerms('tool:gen:del')) {
                    moreItems.push({ key: 'delete', label: '删除', danger: true });
                }
                if (access.hasPerms('tool:gen:edit')) {
                    moreItems.push({ key: 'sync', label: '同步' }, { key: 'gencode', label: '生成代码' });
                }
                return [
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('tool:gen:edit'), onClick: () => {
                            previewCode(record.tableId).then((res) => {
                                if (res.code === 200) {
                                    setPreivewData(res.data);
                                    setShowPreview(true);
                                }
                                else {
                                    message.error('获取数据失败');
                                }
                            });
                        }, children: "\u9884\u89C8" }, "preview"),
                    _jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms('tool:gen:edit'), onClick: () => {
                            history.push(`/tool/gen/edit?id=${record.tableId}`);
                        }, children: "\u7F16\u8F91" }, "config"),
                    moreItems.length > 0 ? (_jsx(Dropdown, { trigger: ['click'], menu: {
                            items: moreItems,
                            onClick: ({ key }) => {
                                if (key === 'delete') {
                                    Modal.confirm({
                                        title: '删除任务',
                                        content: '确定删除该任务吗？',
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
                                }
                                else if (key === 'sync') {
                                    syncDbInfo(record.tableName).then((res) => {
                                        if (res.code === 200) {
                                            message.success('同步成功');
                                        }
                                        else {
                                            message.error('同步失败');
                                        }
                                    });
                                }
                                else if (key === 'gencode') {
                                    if (record.genType === '1') {
                                        genCode(record.tableName).then((res) => {
                                            if (res.code === 200) {
                                                message.success(`成功生成到自定义路径：${record.genPath}`);
                                            }
                                            else {
                                                message.error(res.msg);
                                            }
                                        });
                                    }
                                    else {
                                        batchGenCode(record.tableName);
                                    }
                                }
                            },
                        }, children: _jsxs("a", { onClick: (event) => event.preventDefault(), children: ["\u66F4\u591A ", _jsx(DownOutlined, { style: { fontSize: 10 } })] }) }, "more")) : null,
                ];
            },
        },
    ];
    return (_jsx(Content, { children: _jsxs(Card, { variant: "borderless", children: [_jsx(ProTable, { headerTitle: "\u4EE3\u7801\u751F\u6210\u4FE1\u606F", actionRef: actionRef, formRef: formTableRef, rowKey: "tableId", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('tool:gen:edit'), onClick: () => {
                                if (selectedRows.length === 0) {
                                    message.error('请选择要生成的数据');
                                    return;
                                }
                                const tableNames = selectedRows.map((row) => row.tableName);
                                if (selectedRows[0].genType === '1') {
                                    genCode(tableNames.join(',')).then((res) => {
                                        if (res.code === 200) {
                                            message.success(`成功生成到自定义路径：${selectedRows[0].genPath}`);
                                        }
                                        else {
                                            message.error(res.msg);
                                        }
                                    });
                                }
                                else {
                                    batchGenCode(tableNames.join(','));
                                }
                            }, children: [_jsx(DownloadOutlined, {}), " ", _jsx(FormattedMessage, { id: "gen.gencode", defaultMessage: "\u751F\u6210" })] }, "gen"),
                        _jsxs(Button, { type: "primary", hidden: !access.hasPerms('tool:gen:add'), onClick: () => {
                                history.push('/tool/gen/import');
                            }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "gen.import", defaultMessage: "\u5BFC\u5165" })] }, "import"),
                    ], request: (params) => getGenCodeList({ ...params }).then((res) => {
                        const rows = res?.rows ?? [];
                        return {
                            data: rows,
                            total: res?.total ?? rows.length,
                            success: true,
                        };
                    }), columns: columns, rowSelection: {
                        onChange: (_, selectedRows) => {
                            setSelectedRows(selectedRows);
                        },
                    } }), selectedRows?.length > 0 && (_jsx(FooterToolbar, { extra: _jsxs("div", { children: [_jsx(FormattedMessage, { id: "pages.searchTable.chosen", defaultMessage: "\u5DF2\u9009\u62E9" }), ' ', _jsx("a", { style: { fontWeight: 600 }, children: selectedRows.length }), ' ', _jsx(FormattedMessage, { id: "pages.searchTable.item", defaultMessage: "\u9879" })] }), children: _jsx(Button, { hidden: !access.hasPerms('tool:gen:remove'), onClick: async () => {
                            Modal.confirm({
                                title: '删除任务',
                                content: '确定删除该任务吗？',
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
                        }, children: _jsx(FormattedMessage, { id: "pages.searchTable.batchDeletion", defaultMessage: "\u6279\u91CF\u5220\u9664" }) }, "delete") })), _jsx(PreviewForm, { open: showPreview, data: preivewData, onHide: () => {
                        setShowPreview(false);
                    } }), _jsx(Drawer, { size: 600, open: showDetail, onClose: () => {
                        setCurrentRow(undefined);
                        setShowDetail(false);
                    }, closable: false, children: currentRow?.tableName && (_jsx(ProDescriptions, { column: 2, title: currentRow?.tableName, request: async () => ({
                            data: currentRow || {},
                        }), params: {
                            id: currentRow?.tableName,
                        }, columns: columns })) })] }) }));
};
export default GenCodeView;
