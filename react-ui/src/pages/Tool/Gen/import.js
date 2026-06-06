import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Card, Layout } from 'antd';
import { useState } from 'react';
import { history, FormattedMessage } from '@umijs/max';
import { importTables, queryTableList } from './service';
import { ProTable } from '@ant-design/pro-components';
import { getPersistedProTableSearch } from '@/utils/proTableSearch';
import { PlusOutlined, RollbackOutlined } from '@ant-design/icons';
import { message } from '@/utils/feedback';
const { Content } = Layout;
const handleImport = async (tables) => {
    const hide = message.loading('正在配置');
    try {
        await importTables(tables);
        hide();
        message.success('配置成功');
        return true;
    }
    catch (error) {
        hide();
        message.error('配置失败请重试！');
        return false;
    }
};
const ImportTableList = () => {
    const [selectTables, setSelectTables] = useState([]);
    const columns = [
        {
            title: '表名称',
            dataIndex: 'tableName',
        },
        {
            title: '表描述',
            dataIndex: 'tableComment',
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            valueType: 'textarea',
            search: false,
        },
    ];
    return (_jsx(Content, { children: _jsx(Card, { variant: "borderless", children: _jsx(ProTable, { headerTitle: "\u4EE3\u7801\u751F\u6210\u4FE1\u606F", rowKey: "tableName", search: getPersistedProTableSearch({ labelWidth: 120 }), toolBarRender: () => [
                    _jsxs(Button, { type: "primary", onClick: async () => {
                            if (selectTables.length < 1) {
                                message.error('请选择要导入的表！');
                                return;
                            }
                            const success = await handleImport(selectTables.join(','));
                            if (success) {
                                history.back();
                            }
                        }, children: [_jsx(PlusOutlined, {}), " ", _jsx(FormattedMessage, { id: "gen.submit", defaultMessage: "\u63D0\u4EA4" })] }, "primary"),
                    _jsxs(Button, { type: "primary", onClick: () => {
                            history.back();
                        }, children: [_jsx(RollbackOutlined, {}), " ", _jsx(FormattedMessage, { id: "gen.goback", defaultMessage: "\u8FD4\u56DE" })] }, "goback"),
                ], request: (params) => queryTableList({ ...params }).then((res) => {
                    return {
                        data: res.rows,
                        total: res.total,
                        success: true,
                    };
                }), columns: columns, rowSelection: {
                    onChange: (_, selectedRows) => {
                        if (selectedRows && selectedRows.length > 0) {
                            const tables = selectedRows.map((row) => {
                                return row.tableName;
                            });
                            setSelectTables(tables);
                        }
                    },
                } }) }) }));
};
export default ImportTableList;
