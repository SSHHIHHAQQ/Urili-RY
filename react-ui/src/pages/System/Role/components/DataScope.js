import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { Checkbox, Col, Form, Modal, Row, Tree } from 'antd';
import { FormattedMessage, useIntl } from '@umijs/max';
import { ProForm, ProFormDigit, ProFormSelect, ProFormText } from '@ant-design/pro-components';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
const DataScopeForm = (props) => {
    const [form] = Form.useForm();
    const { deptTree, deptCheckedKeys } = props;
    const [dataScopeType, setDataScopeType] = useState('1');
    const [deptIds, setDeptIds] = useState([]);
    const [deptTreeExpandKey, setDeptTreeExpandKey] = useState([]);
    const [checkStrictly, setCheckStrictly] = useState(true);
    useEffect(() => {
        setDeptIds(deptCheckedKeys);
        form.resetFields();
        form.setFieldsValue({
            roleId: props.values.roleId,
            roleName: props.values.roleName,
            roleKey: props.values.roleKey,
            dataScope: props.values.dataScope,
        });
        setDataScopeType(props.values.dataScope);
    }, [props.values]);
    const intl = useIntl();
    const handleOk = () => {
        form.submit();
    };
    const handleCancel = () => {
        props.onCancel();
    };
    const handleFinish = async (values) => {
        props.onSubmit({ ...values, deptIds });
    };
    const getAllDeptNode = (node) => {
        let keys = [];
        node.forEach(value => {
            keys.push(value.key);
            if (value.children) {
                keys = keys.concat(getAllDeptNode(value.children));
            }
        });
        return keys;
    };
    const deptAllNodes = getAllDeptNode(deptTree);
    const onDeptOptionChange = (checkedValues) => {
        if (checkedValues.includes('deptExpand')) {
            setDeptTreeExpandKey(deptAllNodes);
        }
        else {
            setDeptTreeExpandKey([]);
        }
        if (checkedValues.includes('deptNodeAll')) {
            setDeptIds(deptAllNodes);
        }
        else {
            setDeptIds([]);
        }
        if (checkedValues.includes('deptCheckStrictly')) {
            setCheckStrictly(false);
        }
        else {
            setCheckStrictly(true);
        }
    };
    return (_jsx(Modal, { width: 640, title: intl.formatMessage({
            id: 'system.user.auth.role',
            defaultMessage: '分配角色',
        }), open: props.open, destroyOnHidden: true, forceRender: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, layout: "horizontal", onFinish: handleFinish, initialValues: {
                login_password: '',
                confirm_password: '',
            }, children: [_jsx(ProFormDigit, { name: "roleId", label: intl.formatMessage({
                        id: 'system.role.role_id',
                        defaultMessage: '角色编号',
                    }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u89D2\u8272\u7F16\u53F7", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u89D2\u8272\u7F16\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u89D2\u8272\u7F16\u53F7\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "roleName", label: intl.formatMessage({
                        id: 'system.role.role_name',
                        defaultMessage: '角色名称',
                    }), disabled: true, placeholder: "\u8BF7\u8F93\u5165\u89D2\u8272\u540D\u79F0", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u89D2\u8272\u540D\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u89D2\u8272\u540D\u79F0\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "roleKey", label: intl.formatMessage({
                        id: 'system.role.role_key',
                        defaultMessage: '权限字符串',
                    }), disabled: true, placeholder: "\u8BF7\u8F93\u5165\u89D2\u8272\u6743\u9650\u5B57\u7B26\u4E32", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u89D2\u8272\u6743\u9650\u5B57\u7B26\u4E32\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u89D2\u8272\u6743\u9650\u5B57\u7B26\u4E32\uFF01" }),
                        },
                    ] }), _jsx(ProFormSelect, { name: "dataScope", label: '\u6743\u9650\u8303\u56F4', initialValue: '1', placeholder: "\u8BF7\u8F93\u5165\u7528\u6237\u6027\u522B", valueEnum: {
                        "1": "全部数据权限",
                        "2": "自定数据权限",
                        "3": "本部门数据权限",
                        "4": "本部门及以下数据权限",
                        "5": "仅本人数据权限"
                    }, rules: [
                        {
                            required: true,
                        },
                    ], fieldProps: {
                        ...SEARCHABLE_SELECT_PROPS,
                        onChange: (value) => {
                            setDataScopeType(value);
                        },
                    } }), _jsx(ProForm.Item, { name: "deptIds", label: intl.formatMessage({
                        id: 'system.role.auth',
                        defaultMessage: '菜单权限',
                    }), required: dataScopeType === '1', hidden: dataScopeType !== '1', children: _jsxs(Row, { gutter: [16, 16], children: [_jsx(Col, { md: 24, children: _jsx(Checkbox.Group, { options: [
                                        { label: '展开/折叠', value: 'deptExpand' },
                                        { label: '全选/全不选', value: 'deptNodeAll' },
                                        // { label: '父子联动', value: 'deptCheckStrictly' },
                                    ], onChange: onDeptOptionChange }) }), _jsx(Col, { md: 24, children: _jsx(Tree, { checkable: true, checkStrictly: checkStrictly, expandedKeys: deptTreeExpandKey, treeData: deptTree, checkedKeys: deptIds, defaultCheckedKeys: deptCheckedKeys, onCheck: (checkedKeys, checkInfo) => {
                                        console.log(checkedKeys, checkInfo);
                                        if (checkStrictly) {
                                            return setDeptIds(checkedKeys.checked);
                                        }
                                        else {
                                            return setDeptIds({ checked: checkedKeys, halfChecked: checkInfo.halfCheckedKeys });
                                        }
                                    }, onExpand: (expandedKeys) => {
                                        setDeptTreeExpandKey(deptTreeExpandKey.concat(expandedKeys));
                                    } }) })] }) })] }) }));
};
export default DataScopeForm;
