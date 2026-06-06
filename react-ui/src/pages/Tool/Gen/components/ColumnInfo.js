import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Fragment, useEffect, useState } from 'react';
import { Button, Checkbox, Col, Row, Tag } from 'antd';
import { history } from '@umijs/max';
import styles from '../style.module.css';
import { EditableProTable } from '@ant-design/pro-components';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
const booleanEnum = [
    {
        label: 'true',
        value: '1',
    },
    {
        label: 'false',
        value: '0',
    },
];
const ColumnInfo = (props) => {
    const formRef = { current: undefined };
    const [dataSource, setDataSource] = useState();
    const [editableKeys, setEditableRowKeys] = useState([]);
    const { data, dictData, onStepSubmit } = props;
    const columns = [
        {
            title: '编号',
            dataIndex: 'columnId',
            editable: false,
            width: 80,
        },
        {
            title: '字段名',
            dataIndex: 'columnName',
            editable: false,
        },
        {
            title: '字段描述',
            dataIndex: 'columnComment',
            hideInForm: true,
            search: false,
            width: 200,
        },
        {
            title: '字段类型',
            dataIndex: 'columnType',
            editable: false,
        },
        {
            title: 'Java类型',
            dataIndex: 'javaType',
            valueType: 'select',
            fieldProps: SEARCHABLE_SELECT_PROPS,
            valueEnum: {
                Long: {
                    text: 'Long',
                },
                String: {
                    text: 'String',
                },
                Integer: {
                    text: 'Integer',
                },
                Double: {
                    text: 'Double',
                },
                BigDecimal: {
                    text: 'BigDecimal',
                },
                Date: {
                    text: 'Date',
                },
            },
        },
        {
            title: 'Java属性',
            dataIndex: 'javaField',
        },
        {
            title: '插入',
            dataIndex: 'isInsert',
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: booleanEnum,
            },
            render: (_, record) => {
                return _jsx(Checkbox, { checked: record.isInsert === '1' });
            },
        },
        {
            title: '编辑',
            dataIndex: 'isEdit',
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: booleanEnum,
            },
            render: (_, record) => {
                return _jsx(Checkbox, { checked: record.isEdit === '1' });
            },
        },
        {
            title: '列表',
            dataIndex: 'isList',
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: booleanEnum,
            },
            render: (_, record) => {
                return _jsx(Checkbox, { checked: record.isList === '1' });
            },
        },
        {
            title: '查询',
            dataIndex: 'isQuery',
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: booleanEnum,
            },
            render: (_, record) => {
                return _jsx(Checkbox, { checked: record.isQuery === '1' });
            },
        },
        {
            title: '查询方式',
            dataIndex: 'queryType',
            valueType: 'select',
            fieldProps: SEARCHABLE_SELECT_PROPS,
            valueEnum: {
                EQ: {
                    text: '=',
                },
                NE: {
                    text: '!=',
                },
                GT: {
                    text: '>',
                },
                GTE: {
                    text: '>=',
                },
                LT: {
                    text: '<',
                },
                LTE: {
                    text: '<=',
                },
                LIKE: {
                    text: 'LIKE',
                },
                BETWEEN: {
                    text: 'BETWEEN',
                },
            },
        },
        {
            title: '必填',
            dataIndex: 'isRequired',
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: booleanEnum,
            },
            render: (_, record) => {
                return _jsx(Checkbox, { checked: record.isRequired === '1' });
            },
        },
        {
            title: '显示类型',
            dataIndex: 'htmlType',
            search: false,
            valueType: 'select',
            fieldProps: SEARCHABLE_SELECT_PROPS,
            valueEnum: {
                input: {
                    text: '文本框',
                },
                textarea: {
                    text: '文本域',
                },
                select: {
                    text: '下拉框',
                },
                radio: {
                    text: '单选框',
                },
                checkbox: {
                    text: '复选框',
                },
                datetime: {
                    text: '日期控件',
                },
                imageUpload: {
                    text: '图片上传',
                },
                fileUpload: {
                    text: '文件上传',
                },
                editor: {
                    text: '富文本控件',
                },
            },
        },
        {
            title: '字典类型',
            dataIndex: 'dictType',
            search: false,
            valueType: 'select',
            fieldProps: {
                ...SEARCHABLE_SELECT_PROPS,
                options: dictData,
            },
            render: (text) => {
                return _jsx(Tag, { color: "#108ee9", children: text });
            },
        },
    ];
    useEffect(() => {
        setDataSource(data);
        if (data) {
            setEditableRowKeys(data.map((item) => item.columnId));
        }
    }, [data]);
    const onSubmit = (direction) => {
        if (onStepSubmit) {
            onStepSubmit('column', dataSource, direction);
        }
    };
    const onDataChange = (value) => {
        setDataSource({ ...value });
    };
    return (_jsxs(Fragment, { children: [_jsx(Row, { children: _jsx(Col, { span: 24, children: _jsx(EditableProTable, { formRef: formRef, rowKey: "columnId", search: false, columns: columns, value: dataSource, editable: {
                            type: 'multiple',
                            editableKeys,
                            onChange: setEditableRowKeys,
                            actionRender: (row, config, defaultDoms) => {
                                return [defaultDoms.delete];
                            },
                            onValuesChange: (record, recordList) => {
                                setDataSource(recordList);
                            },
                        }, onChange: onDataChange, recordCreatorProps: false }) }) }), _jsxs(Row, { justify: "center", children: [_jsx(Col, { span: 4, children: _jsx(Button, { type: "primary", onClick: () => {
                                history.back();
                            }, children: "\u8FD4\u56DE" }) }), _jsx(Col, { span: 4, children: _jsx(Button, { type: "primary", className: styles.step_buttons, onClick: () => {
                                onSubmit('prev');
                            }, children: "\u4E0A\u4E00\u6B65" }) }), _jsx(Col, { span: 4, children: _jsx(Button, { type: "primary", onClick: () => {
                                onSubmit('next');
                            }, children: "\u4E0B\u4E00\u6B65" }) })] })] }));
};
export default ColumnInfo;
