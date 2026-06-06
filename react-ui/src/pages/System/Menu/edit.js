import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { ProForm, ProFormDigit, ProFormText, ProFormRadio, ProFormTreeSelect, ProFormSelect, } from '@ant-design/pro-components';
import { Form, Modal } from 'antd';
import { useIntl, FormattedMessage } from '@umijs/max';
import { createIcon } from '@/utils/IconUtil';
import IconSelector from '@/components/IconSelector';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
const MenuForm = (props) => {
    const [form] = Form.useForm();
    const [menuTypeId, setMenuTypeId] = useState('M');
    const [menuIconName, setMenuIconName] = useState();
    const [iconSelectorOpen, setIconSelectorOpen] = useState(false);
    const { menuTree, visibleOptions, statusOptions } = props;
    useEffect(() => {
        form.resetFields();
        setMenuIconName(props.values.icon);
        form.setFieldsValue({
            menuId: props.values.menuId,
            menuName: props.values.menuName,
            parentId: props.values.parentId,
            orderNum: props.values.orderNum,
            path: props.values.path,
            component: props.values.component,
            query: props.values.query,
            isFrame: props.values.isFrame,
            isCache: props.values.isCache,
            menuType: props.values.menuType,
            visible: props.values.visible,
            status: props.values.status,
            perms: props.values.perms,
            icon: props.values.icon,
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
        props.onSubmit(values);
    };
    return (_jsxs(Modal, { width: 640, title: intl.formatMessage({
            id: 'system.menu.title',
            defaultMessage: '编辑菜单权限',
        }), open: props.open, forceRender: true, destroyOnHidden: true, onOk: handleOk, onCancel: handleCancel, children: [_jsxs(ProForm, { form: form, grid: true, submitter: false, layout: "horizontal", onFinish: handleFinish, children: [_jsx(ProFormDigit, { name: "menuId", label: intl.formatMessage({
                            id: 'system.menu.menu_id',
                            defaultMessage: '菜单编号',
                        }), placeholder: "\u8BF7\u8F93\u5165\u83DC\u5355\u7F16\u53F7", disabled: true, hidden: true, rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u83DC\u5355\u7F16\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u83DC\u5355\u7F16\u53F7\uFF01" }),
                            },
                        ] }), _jsx(ProFormTreeSelect, { name: "parentId", label: intl.formatMessage({
                            id: 'system.menu.parent_id',
                            defaultMessage: '上级菜单',
                        }), params: { menuTree }, request: async () => {
                            return menuTree;
                        }, placeholder: "\u8BF7\u8F93\u5165\u7236\u83DC\u5355\u7F16\u53F7", rules: [
                            {
                                required: true,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7236\u83DC\u5355\u7F16\u53F7\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7236\u83DC\u5355\u7F16\u53F7\uFF01" }),
                            },
                        ], fieldProps: {
                            ...SEARCHABLE_TREE_SELECT_PROPS,
                            defaultValue: 0
                        } }), _jsx(ProFormRadio.Group, { name: "menuType", valueEnum: {
                            M: '目录',
                            C: '菜单',
                            F: '按钮',
                        }, label: intl.formatMessage({
                            id: 'system.menu.menu_type',
                            defaultMessage: '菜单类型',
                        }), placeholder: "\u8BF7\u8F93\u5165\u83DC\u5355\u7C7B\u578B", rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u83DC\u5355\u7C7B\u578B\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u83DC\u5355\u7C7B\u578B\uFF01" }),
                            },
                        ], fieldProps: {
                            defaultValue: 'M',
                            onChange: (e) => {
                                setMenuTypeId(e.target.value);
                            },
                        } }), _jsx(ProFormSelect, { name: "icon", label: intl.formatMessage({
                            id: 'system.menu.icon',
                            defaultMessage: '菜单图标',
                        }), valueEnum: {}, hidden: menuTypeId === 'F', addonBefore: createIcon(menuIconName), fieldProps: {
                            ...SEARCHABLE_SELECT_PROPS,
                            onClick: () => {
                                setIconSelectorOpen(true);
                            },
                        }, placeholder: "\u8BF7\u8F93\u5165\u83DC\u5355\u56FE\u6807", rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u83DC\u5355\u56FE\u6807\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u83DC\u5355\u56FE\u6807\uFF01" }),
                            },
                        ] }), _jsx(ProFormText, { name: "menuName", label: intl.formatMessage({
                            id: 'system.menu.menu_name',
                            defaultMessage: '菜单名称',
                        }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u83DC\u5355\u540D\u79F0", rules: [
                            {
                                required: true,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u83DC\u5355\u540D\u79F0\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u83DC\u5355\u540D\u79F0\uFF01" }),
                            },
                        ] }), _jsx(ProFormDigit, { name: "orderNum", label: intl.formatMessage({
                            id: 'system.menu.order_num',
                            defaultMessage: '显示顺序',
                        }), width: "lg", colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F", rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u663E\u793A\u987A\u5E8F\uFF01" }),
                            },
                        ], fieldProps: {
                            defaultValue: 1
                        } }), _jsx(ProFormRadio.Group, { name: "isFrame", valueEnum: {
                            0: '是',
                            1: '否',
                        }, initialValue: "1", label: intl.formatMessage({
                            id: 'system.menu.is_frame',
                            defaultMessage: '是否为外链',
                        }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u662F\u5426\u4E3A\u5916\u94FE", hidden: menuTypeId === 'F', rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u662F\u5426\u4E3A\u5916\u94FE\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u662F\u5426\u4E3A\u5916\u94FE\uFF01" }),
                            },
                        ], fieldProps: {
                            defaultValue: '1'
                        } }), _jsx(ProFormText, { name: "path", label: intl.formatMessage({
                            id: 'system.menu.path',
                            defaultMessage: '路由地址',
                        }), width: "lg", colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u8DEF\u7531\u5730\u5740", hidden: menuTypeId === 'F', rules: [
                            {
                                required: menuTypeId !== 'F',
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u8DEF\u7531\u5730\u5740\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u8DEF\u7531\u5730\u5740\uFF01" }),
                            },
                        ] }), _jsx(ProFormText, { name: "component", label: intl.formatMessage({
                            id: 'system.menu.component',
                            defaultMessage: '组件路径',
                        }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u7EC4\u4EF6\u8DEF\u5F84", hidden: menuTypeId !== 'C', rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u7EC4\u4EF6\u8DEF\u5F84\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u7EC4\u4EF6\u8DEF\u5F84\uFF01" }),
                            },
                        ] }), _jsx(ProFormText, { name: "query", label: intl.formatMessage({
                            id: 'system.menu.query',
                            defaultMessage: '路由参数',
                        }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u8DEF\u7531\u53C2\u6570", hidden: menuTypeId !== 'C', rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u8DEF\u7531\u53C2\u6570\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u8DEF\u7531\u53C2\u6570\uFF01" }),
                            },
                        ] }), _jsx(ProFormText, { name: "perms", label: intl.formatMessage({
                            id: 'system.menu.perms',
                            defaultMessage: '权限标识',
                        }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u6743\u9650\u6807\u8BC6", hidden: menuTypeId === 'M', rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u6743\u9650\u6807\u8BC6\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u6743\u9650\u6807\u8BC6\uFF01" }),
                            },
                        ] }), _jsx(ProFormRadio.Group, { name: "isCache", valueEnum: {
                            0: '缓存',
                            1: '不缓存',
                        }, label: intl.formatMessage({
                            id: 'system.menu.is_cache',
                            defaultMessage: '是否缓存',
                        }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u662F\u5426\u7F13\u5B58", hidden: menuTypeId !== 'C', rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u662F\u5426\u7F13\u5B58\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u662F\u5426\u7F13\u5B58\uFF01" }),
                            },
                        ], fieldProps: {
                            defaultValue: 0
                        } }), _jsx(ProFormRadio.Group, { name: "visible", valueEnum: visibleOptions, label: intl.formatMessage({
                            id: 'system.menu.visible',
                            defaultMessage: '显示状态',
                        }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u663E\u793A\u72B6\u6001", hidden: menuTypeId === 'F', rules: [
                            {
                                required: false,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u663E\u793A\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u663E\u793A\u72B6\u6001\uFF01" }),
                            },
                        ], fieldProps: {
                            defaultValue: '0'
                        } }), _jsx(ProFormRadio.Group, { valueEnum: statusOptions, name: "status", label: intl.formatMessage({
                            id: 'system.menu.status',
                            defaultMessage: '菜单状态',
                        }), colProps: { md: 12, xl: 12 }, placeholder: "\u8BF7\u8F93\u5165\u83DC\u5355\u72B6\u6001", hidden: menuTypeId === 'F', rules: [
                            {
                                required: true,
                                message: _jsx(FormattedMessage, { id: "\u8BF7\u8F93\u5165\u83DC\u5355\u72B6\u6001\uFF01", defaultMessage: "\u8BF7\u8F93\u5165\u83DC\u5355\u72B6\u6001\uFF01" }),
                            },
                        ], fieldProps: {
                            defaultValue: '0'
                        } })] }), _jsx(Modal, { width: 800, open: iconSelectorOpen, onCancel: () => {
                    setIconSelectorOpen(false);
                }, footer: null, children: _jsx(IconSelector, { onSelect: (name) => {
                        form.setFieldsValue({ icon: name });
                        setMenuIconName(name);
                        setIconSelectorOpen(false);
                    } }) })] }));
};
export default MenuForm;
