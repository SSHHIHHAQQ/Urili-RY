import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormRadio, ProFormTextArea, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
import { Tree } from 'antd';
const RoleForm = (props) => {
    const [form] = Form.useForm();
    const { menuTree, menuCheckedKeys } = props;
    const [menuIds, setMenuIds] = useState([]);
    const { statusOptions } = props;
    useEffect(() => {
        form.resetFields();
        form.setFieldsValue({
            roleId: props.values.roleId,
            roleName: props.values.roleName,
            roleKey: props.values.roleKey,
            roleSort: props.values.roleSort,
            dataScope: props.values.dataScope,
            menuCheckStrictly: props.values.menuCheckStrictly,
            deptCheckStrictly: props.values.deptCheckStrictly,
            status: props.values.status,
            delFlag: props.values.delFlag,
            createBy: props.values.createBy,
            createTime: props.values.createTime,
            updateBy: props.values.updateBy,
            updateTime: props.values.updateTime,
            remark: props.values.remark,
        });
    }, [form, props]);
    const intl = useIntl();
    const handleOk = () => {
        form.submit();
    };
    const handleCancel = () => {
        props.onCancel();
    };
    const handleFinish = async (values) => {
        props.onSubmit({ ...values, menuIds });
    };
    return (_jsx(Modal, { width: 640, title: intl.formatMessage({
            id: 'system.role.title',
            defaultMessage: '编辑角色信息',
        }), forceRender: true, open: props.open, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: _jsxs(ProForm, { form: form, grid: true, layout: "horizontal", submitter: false, onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "roleId", label: intl.formatMessage({
                        id: 'system.role.role_id',
                        defaultMessage: '角色编号',
                    }), placeholder: "\u8BF7\u8F93\u5165\u89D2\u8272\u7F16\u53F7", disabled: true, hidden: true, rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u89D2\u8272\u7F16\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u89D2\u8272\u7F16\u53F7\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "roleName", label: intl.formatMessage({
                        id: 'system.role.role_name',
                        defaultMessage: '角色名称',
                    }), placeholder: "\u8BF7\u8F93\u5165\u89D2\u8272\u540D\u79F0", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u89D2\u8272\u540D\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u89D2\u8272\u540D\u79F0\uFF01" }),
                        },
                    ] }), _jsx(ProFormText, { name: "roleKey", label: intl.formatMessage({
                        id: 'system.role.role_key',
                        defaultMessage: '权限字符串',
                    }), placeholder: "\u8BF7\u8F93\u5165\u89D2\u8272\u6743\u9650\u5B57\u7B26\u4E32", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u89D2\u8272\u6743\u9650\u5B57\u7B26\u4E32\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u89D2\u8272\u6743\u9650\u5B57\u7B26\u4E32\uFF01" }),
                        },
                    ] }), _jsx(ProFormDigit, { name: "roleSort", label: intl.formatMessage({
                        id: 'system.role.role_sort',
                        defaultMessage: '显示顺序',
                    }), placeholder: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F\uFF01" }),
                        },
                    ], fieldProps: {
                        defaultValue: 1
                    } }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                        id: 'system.role.status',
                        defaultMessage: '角色状态',
                    }), placeholder: "\u8BF7\u8F93\u5165\u89D2\u8272\u72B6\u6001", rules: [
                        {
                            required: true,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u89D2\u8272\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u89D2\u8272\u72B6\u6001\uFF01" }),
                        },
                    ], fieldProps: {
                        defaultValue: "0"
                    } }), _jsx(ProForm.Item, { name: "menuIds", label: intl.formatMessage({
                        id: 'system.role.auth',
                        defaultMessage: '菜单权限',
                    }), children: _jsx(Tree, { checkable: true, multiple: true, checkStrictly: true, defaultExpandAll: false, treeData: menuTree, defaultCheckedKeys: menuCheckedKeys, onCheck: (checkedKeys) => {
                            return setMenuIds(checkedKeys.checked);
                        } }) }), _jsx(ProFormTextArea, { name: "remark", label: intl.formatMessage({
                        id: 'system.role.remark',
                        defaultMessage: '备注',
                    }), placeholder: "\u8BF7\u8F93\u5165\u5907\u6CE8", rules: [
                        {
                            required: false,
                            message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u5907\u6CE8\uFF01" }),
                        },
                    ] })] }) }));
};
export default RoleForm;
