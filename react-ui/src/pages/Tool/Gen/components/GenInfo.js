import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Col, Divider, Form, Row, TreeSelect } from 'antd';
import { Fragment, useEffect, useState } from 'react';
import { history } from '@umijs/max';
import styles from '../style.module.css';
import { ProForm, ProFormRadio, ProFormSelect, ProFormText } from '@ant-design/pro-components';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
const GenInfo = (props) => {
    const [form] = Form.useForm();
    const [pathType, setPathType] = useState('0');
    const [tlpType, setTlpType] = useState('curd');
    const [subTablesColumnOptions, setSubTablesColumnOptions] = useState();
    const { menuData, tableInfo, onStepSubmit } = props;
    const tablesOptions = tableInfo?.map((item) => {
        return {
            value: item.tableName,
            label: `${item.tableName}：${item.tableComment}`,
        };
    });
    if (tableInfo) {
        for (let index = 0; index < tableInfo?.length; index += 1) {
            const tbl = tableInfo[index];
            if (tbl.tableName === props.values.subTableName) {
                const opts = [];
                tbl.columns.forEach((item) => {
                    opts.push({
                        value: item.columnName,
                        label: `${item.columnName}: ${item.columnComment}`,
                    });
                });
                break;
            }
        }
    }
    const treeColumns = props.values.columns.map((item) => {
        return {
            value: item.columnName,
            label: `${item.columnName}: ${item.columnComment}`,
        };
    });
    const onSubmit = async (direction) => {
        const values = await form.validateFields();
        onStepSubmit('gen', values, direction);
    };
    useEffect(() => {
        setPathType(props.values.genType);
        setTlpType(props.values.tplCategory);
    }, [props.values.genType, props.values.tplCategory]);
    return (_jsxs(Fragment, { children: [_jsx(Row, { children: _jsx(Col, { span: 24, children: _jsxs(ProForm, { form: form, onFinish: async () => {
                            const values = await form.validateFields();
                            onStepSubmit('gen', values);
                        }, initialValues: {
                            curd: props.values.curd,
                            tree: props.values.tree,
                            sub: props.values.sub,
                            tplCategory: props.values.tplCategory,
                            packageName: props.values.packageName,
                            moduleName: props.values.moduleName,
                            businessName: props.values.businessName,
                            functionName: props.values.functionName,
                            parentMenuId: props.values.parentMenuId,
                            genType: props.values.genType,
                            genPath: props.values.genPath,
                            treeCode: props.values.treeCode,
                            treeParentCode: props.values.treeParentCode,
                            treeName: props.values.treeName,
                            subTableName: props.values.subTableName,
                            subTableFkName: props.values.subTableFkName,
                        }, submitter: {
                            resetButtonProps: {
                                style: { display: 'none' },
                            },
                            submitButtonProps: {
                                style: { display: 'none' },
                            },
                        }, children: [_jsxs(Row, { gutter: [16, 16], children: [_jsx(Col, { span: 12, order: 1, children: _jsx(ProFormSelect, { fieldProps: {
                                                ...SEARCHABLE_SELECT_PROPS,
                                                onChange: (val) => {
                                                    setTlpType(val);
                                                },
                                            }, valueEnum: {
                                                crud: '单表（增删改查）',
                                                tree: '树表（增删改查）',
                                                sub: '主子表（增删改查）',
                                            }, name: "tplCategory", label: "\u751F\u6210\u6A21\u677F", rules: [
                                                {
                                                    required: true,
                                                    message: '选择类型',
                                                },
                                            ] }) }), _jsx(Col, { span: 12, order: 2, children: _jsx(ProFormText, { name: "packageName", label: "\u751F\u6210\u5305\u8DEF\u5F84" }) })] }), _jsxs(Row, { gutter: [16, 16], children: [_jsx(Col, { span: 12, order: 1, children: _jsx(ProFormText, { name: "moduleName", label: "\u751F\u6210\u6A21\u5757\u540D" }) }), _jsx(Col, { span: 12, order: 2, children: _jsx(ProFormText, { name: "businessName", label: "\u751F\u6210\u4E1A\u52A1\u540D" }) })] }), _jsxs(Row, { gutter: [16, 16], children: [_jsx(Col, { span: 12, order: 1, children: _jsx(ProFormText, { name: "functionName", label: "\u751F\u6210\u529F\u80FD\u540D" }) }), _jsx(Col, { span: 12, order: 2, children: _jsx(ProForm.Item, { labelCol: { span: 20 }, name: "parentMenuId", label: "\u7236\u83DC\u5355", children: _jsx(TreeSelect, { ...SEARCHABLE_TREE_SELECT_PROPS, style: { width: '74%' }, defaultValue: props.values.parentMenuId, treeData: menuData, placeholder: "\u8BF7\u9009\u62E9\u7236\u83DC\u5355" }) }) })] }), _jsx(Row, { gutter: [16, 16], children: _jsx(Col, { span: 24, children: _jsx(ProFormRadio.Group, { valueEnum: {
                                            '0': 'zip压缩包',
                                            '1': '自定义路径',
                                        }, name: "genType", label: "\u751F\u6210\u4EE3\u7801\u65B9\u5F0F", rules: [
                                            {
                                                required: true,
                                                message: '选择类型',
                                            },
                                        ], fieldProps: {
                                            onChange: (e) => {
                                                setPathType(e.target.value);
                                            },
                                        } }) }) }), _jsx(Row, { gutter: [16, 16], children: _jsx(Col, { span: 24, order: 1, children: _jsx(ProFormText, { hidden: pathType === '0', width: "md", name: "genPath", label: "\u81EA\u5B9A\u4E49\u8DEF\u5F84" }) }) }), _jsxs("div", { hidden: tlpType !== 'tree', children: [_jsx(Divider, { children: "\u5176\u4ED6\u4FE1\u606F" }), _jsxs(Row, { gutter: [16, 16], children: [_jsx(Col, { span: 12, order: 1, children: _jsx(ProFormSelect, { name: "treeCode", label: "\u6811\u7F16\u7801\u5B57\u6BB5", options: treeColumns, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [
                                                        {
                                                            required: tlpType === 'tree',
                                                            message: '树编码字段',
                                                        },
                                                    ] }) }), _jsx(Col, { span: 12, order: 2, children: _jsx(ProFormSelect, { name: "treeParentCode", label: "\u6811\u7236\u7F16\u7801\u5B57\u6BB5", options: treeColumns, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [
                                                        {
                                                            required: tlpType === 'tree',
                                                            message: '树父编码字段',
                                                        },
                                                    ] }) })] }), _jsx(Row, { gutter: [16, 16], children: _jsx(Col, { span: 12, order: 1, children: _jsx(ProFormSelect, { name: "treeName", label: "\u6811\u540D\u79F0\u5B57\u6BB5", options: treeColumns, fieldProps: SEARCHABLE_SELECT_PROPS, rules: [
                                                    {
                                                        required: tlpType === 'tree',
                                                        message: '树名称字段',
                                                    },
                                                ] }) }) })] }), _jsxs("div", { hidden: tlpType !== 'sub', children: [_jsx(Divider, { children: "\u5173\u8054\u4FE1\u606F" }), _jsxs(Row, { gutter: [16, 16], children: [_jsx(Col, { span: 12, order: 1, children: _jsx(ProFormSelect, { name: "subTableName", label: "\u5173\u8054\u5B50\u8868\u7684\u8868\u540D", options: tablesOptions, rules: [
                                                        {
                                                            required: tlpType === 'sub',
                                                            message: '关联子表的表名',
                                                        },
                                                    ], fieldProps: {
                                                        ...SEARCHABLE_SELECT_PROPS,
                                                        onChange: (val) => {
                                                            form.setFieldsValue({
                                                                subTableFkName: '',
                                                            });
                                                            if (tableInfo) {
                                                                for (let index = 0; index < tableInfo?.length; index += 1) {
                                                                    const tbl = tableInfo[index];
                                                                    if (tbl.tableName === val) {
                                                                        const opts = [];
                                                                        tbl.columns.forEach((item) => {
                                                                            opts.push({
                                                                                value: item.columnName,
                                                                                label: `${item.columnName}：${item.columnComment}`,
                                                                            });
                                                                        });
                                                                        setSubTablesColumnOptions(opts);
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    } }) }), _jsx(Col, { span: 12, order: 2, children: _jsx(ProFormSelect, { name: "subTableFkName", options: subTablesColumnOptions, label: "\u5B50\u8868\u5173\u8054\u7684\u5916\u952E\u540D", fieldProps: SEARCHABLE_SELECT_PROPS, rules: [
                                                        {
                                                            required: tlpType === 'sub',
                                                            message: '子表关联的外键名',
                                                        },
                                                    ] }) })] })] })] }) }) }), _jsxs(Row, { justify: "center", children: [_jsx(Col, { span: 4, children: _jsx(Button, { type: "primary", onClick: () => {
                                history.back();
                            }, children: "\u8FD4\u56DE" }) }), _jsx(Col, { span: 4, children: _jsx(Button, { type: "primary", className: styles.step_buttons, onClick: () => {
                                onSubmit('prev');
                            }, children: "\u4E0A\u4E00\u6B65" }) }), _jsx(Col, { span: 4, children: _jsx(Button, { type: "primary", onClick: () => {
                                onSubmit('next');
                            }, children: "\u63D0\u4EA4" }) })] })] }));
};
export default GenInfo;
