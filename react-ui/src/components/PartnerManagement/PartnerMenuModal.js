import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useEffect, useMemo, useState } from 'react';
import { useAccess } from '@umijs/max';
import { App, Button, Flex, Form, Input, InputNumber, Modal, Radio, Select, Table, Tag, TreeSelect, Typography, } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
const menuTypeOptions = [
    { label: '目录', value: 'M' },
    { label: '菜单', value: 'C' },
    { label: '按钮', value: 'F' },
];
const visibleOptions = [
    { label: '显示', value: '0' },
    { label: '隐藏', value: '1' },
];
const statusOptions = [
    { label: '正常', value: '0' },
    { label: '停用', value: '1' },
];
const frameOptions = [
    { label: '是', value: '0' },
    { label: '否', value: '1' },
];
const cacheOptions = [
    { label: '缓存', value: '0' },
    { label: '不缓存', value: '1' },
];
const compactCellTextStyle = {
    display: 'block',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    lineHeight: 1.35,
};
function renderCompactText(value) {
    const text = value == null || value === '' ? '-' : String(value);
    return _jsx(Typography.Text, { style: compactCellTextStyle, title: text, children: text });
}
function renderMenuType(value) {
    const match = menuTypeOptions.find((item) => item.value === value);
    return _jsx(Tag, { color: value === 'F' ? 'default' : value === 'M' ? 'processing' : 'success', children: match?.label || '-' });
}
function renderStatus(value) {
    return _jsx(Tag, { color: value === '0' ? 'success' : 'default', children: value === '0' ? '正常' : '停用' });
}
function renderVisible(value) {
    return _jsx(Tag, { color: value === '0' ? 'processing' : 'default', children: value === '0' ? '显示' : '隐藏' });
}
function buildMenuTree(menus) {
    const nodeMap = new Map();
    const roots = [];
    menus.forEach((menu) => {
        if (menu.menuId == null) {
            return;
        }
        nodeMap.set(menu.menuId, { ...menu, children: [] });
    });
    nodeMap.forEach((node) => {
        const parentId = Number(node.parentId || 0);
        const parent = nodeMap.get(parentId);
        if (parent && parent.menuId !== node.menuId) {
            parent.children = [...(parent.children || []), node];
            return;
        }
        roots.push(node);
    });
    const sortTree = (nodes) => {
        nodes.sort((a, b) => Number(a.orderNum || 0) - Number(b.orderNum || 0));
        nodes.forEach((node) => {
            if (node.children?.length) {
                sortTree(node.children);
            }
        });
    };
    sortTree(roots);
    return roots;
}
function toTreeSelectData(nodes, currentMenuId) {
    return nodes.map((node) => ({
        key: node.menuId,
        value: node.menuId,
        title: node.menuName || String(node.menuId),
        disabled: currentMenuId != null && node.menuId === currentMenuId,
        children: node.children?.length ? toTreeSelectData(node.children, currentMenuId) : undefined,
    }));
}
function mapMenuToForm(menu) {
    return {
        menuName: menu?.menuName,
        parentId: Number(menu?.parentId ?? 0),
        orderNum: Number(menu?.orderNum ?? 0),
        path: menu?.path,
        component: menu?.component,
        query: menu?.query,
        routeName: menu?.routeName,
        isFrame: menu?.isFrame || '1',
        isCache: menu?.isCache || '0',
        menuType: menu?.menuType || 'C',
        visible: menu?.visible || '0',
        status: menu?.status || '0',
        perms: menu?.perms,
        icon: menu?.icon,
        remark: menu?.remark,
    };
}
function buildMenuPayload(currentMenu, values) {
    return {
        menuId: currentMenu?.menuId,
        menuName: values.menuName,
        parentId: Number(values.parentId ?? 0),
        orderNum: Number(values.orderNum ?? 0),
        path: values.path,
        component: values.component,
        query: values.query,
        routeName: values.routeName,
        isFrame: values.isFrame || '1',
        isCache: values.isCache || '0',
        menuType: values.menuType || 'C',
        visible: values.visible || '0',
        status: values.status || '0',
        perms: values.perms,
        icon: values.icon,
        remark: values.remark,
    };
}
const PartnerMenuModal = ({ config, open, onOpenChange }) => {
    const { message, modal } = App.useApp();
    const access = useAccess();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [formOpen, setFormOpen] = useState(false);
    const [currentMenu, setCurrentMenu] = useState();
    const [menus, setMenus] = useState([]);
    const [menuType, setMenuType] = useState('C');
    const permPrefix = `${config.moduleKey}:admin`;
    const currentMenuId = currentMenu?.menuId;
    const menuTree = useMemo(() => buildMenuTree(menus), [menus]);
    const parentTreeData = useMemo(() => [
        {
            key: 0,
            value: 0,
            title: '主类目',
            children: toTreeSelectData(menuTree, currentMenuId),
        },
    ], [menuTree, currentMenuId]);
    const loadMenus = async () => {
        setLoading(true);
        try {
            const resp = await config.services.listMenus();
            if (resp.code === 200) {
                setMenus((resp.data || []));
                return;
            }
            message.error(resp.msg || '菜单列表加载失败');
        }
        catch {
            message.error('菜单列表加载失败，请重试');
        }
        finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        if (open) {
            void loadMenus();
            return;
        }
        setMenus([]);
    }, [open]);
    useEffect(() => {
        if (formOpen) {
            const values = mapMenuToForm(currentMenu);
            setMenuType(values.menuType || 'C');
            form.resetFields();
            form.setFieldsValue(values);
        }
    }, [formOpen, currentMenu?.menuId]);
    const openMenuForm = async (menu) => {
        setCurrentMenu(menu);
        setFormOpen(true);
        if (!menu?.menuId) {
            return;
        }
        try {
            const resp = await config.services.getMenu(menu.menuId);
            if (resp.code === 200) {
                setCurrentMenu(resp.data);
                return;
            }
            message.error(resp.msg || '菜单详情加载失败');
        }
        catch {
            message.error('菜单详情加载失败，请重试');
        }
    };
    const closeMenuForm = () => {
        setFormOpen(false);
        setCurrentMenu(undefined);
    };
    const handleSubmit = async () => {
        const values = await form.validateFields();
        const payload = buildMenuPayload(currentMenu, values);
        setSaving(true);
        try {
            const resp = currentMenuId
                ? await config.services.updateMenu(payload)
                : await config.services.addMenu(payload);
            if (resp.code === 200) {
                message.success(currentMenuId ? '菜单已更新' : '菜单已新增');
                closeMenuForm();
                await loadMenus();
                return;
            }
            message.error(resp.msg || '菜单保存失败');
        }
        catch {
            message.error('菜单保存失败，请重试');
        }
        finally {
            setSaving(false);
        }
    };
    const handleRemove = (menuRecord) => {
        if (!menuRecord.menuId) {
            return;
        }
        modal.confirm({
            title: `确认删除菜单 ${menuRecord.menuName || menuRecord.menuId} 吗？`,
            content: '已有下级菜单或角色绑定时，后端会按端内权限规则处理。',
            onOk: async () => {
                const resp = await config.services.removeMenu(menuRecord.menuId);
                if (resp.code === 200) {
                    message.success('菜单已删除');
                    await loadMenus();
                    return;
                }
                message.error(resp.msg || '菜单删除失败');
            },
        });
    };
    const columns = [
        {
            title: '菜单名称',
            dataIndex: 'menuName',
            width: 180,
            render: renderCompactText,
        },
        {
            title: '类型',
            dataIndex: 'menuType',
            width: 80,
            render: renderMenuType,
        },
        {
            title: '排序',
            dataIndex: 'orderNum',
            width: 72,
            render: renderCompactText,
        },
        {
            title: '路由/组件',
            dataIndex: 'path',
            width: 210,
            render: (_, record) => (_jsxs(Flex, { vertical: true, gap: 0, children: [renderCompactText(record.path), _jsx(Typography.Text, { style: compactCellTextStyle, type: "secondary", title: record.component || '-', children: record.component || '-' })] })),
        },
        {
            title: '权限标识',
            dataIndex: 'perms',
            width: 180,
            render: renderCompactText,
        },
        {
            title: '显示',
            dataIndex: 'visible',
            width: 80,
            render: renderVisible,
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 80,
            render: renderStatus,
        },
        {
            title: '操作',
            dataIndex: 'option',
            width: 120,
            render: (_, record) => (_jsxs(Flex, { gap: 4, children: [_jsx(Button, { type: "link", size: "small", hidden: !access.hasPerms(`${permPrefix}:menu:edit`), onClick: () => void openMenuForm(record), children: "\u7F16\u8F91" }), _jsx(Button, { type: "link", size: "small", danger: true, hidden: !access.hasPerms(`${permPrefix}:menu:remove`), onClick: () => handleRemove(record), children: "\u5220\u9664" })] })),
        },
    ];
    return (_jsxs(_Fragment, { children: [_jsx(Modal, { width: 1080, title: `${config.label}端菜单配置`, open: open, destroyOnHidden: true, footer: null, onCancel: () => onOpenChange(false), children: _jsx(Table, { rowKey: (record) => String(record.menuId), loading: loading, columns: columns, dataSource: menuTree, size: "small", pagination: false, tableLayout: "fixed", expandable: { defaultExpandAllRows: true }, title: () => (_jsx(Button, { type: "primary", size: "small", icon: _jsx(PlusOutlined, {}), hidden: !access.hasPerms(`${permPrefix}:menu:add`), onClick: () => void openMenuForm(), children: "\u65B0\u589E\u83DC\u5355" })) }) }), _jsx(Modal, { width: 720, title: currentMenuId ? '编辑菜单' : '新增菜单', open: formOpen, destroyOnHidden: true, confirmLoading: saving, onOk: handleSubmit, onCancel: closeMenuForm, children: _jsxs(Form, { form: form, layout: "vertical", children: [_jsx(Form.Item, { label: "\u4E0A\u7EA7\u83DC\u5355", name: "parentId", rules: [{ required: true, message: '请选择上级菜单' }], children: _jsx(TreeSelect, { ...SEARCHABLE_TREE_SELECT_PROPS, treeData: parentTreeData, treeDefaultExpandAll: true, placeholder: "\u8BF7\u9009\u62E9" }) }), _jsx(Form.Item, { label: "\u83DC\u5355\u7C7B\u578B", name: "menuType", rules: [{ required: true, message: '请选择菜单类型' }], children: _jsx(Radio.Group, { options: menuTypeOptions, onChange: (event) => setMenuType(event.target.value) }) }), _jsx(Form.Item, { label: "\u83DC\u5355\u540D\u79F0", name: "menuName", rules: [{ required: true, message: '请输入菜单名称' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u663E\u793A\u987A\u5E8F", name: "orderNum", rules: [{ required: true, message: '请输入显示顺序' }], children: _jsx(InputNumber, { min: 0, precision: 0, style: { width: '100%' } }) }), _jsx(Form.Item, { label: "\u56FE\u6807", name: "icon", hidden: menuType === 'F', children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165\u56FE\u6807\u6807\u8BC6" }) }), _jsx(Form.Item, { label: "\u662F\u5426\u5916\u94FE", name: "isFrame", hidden: menuType === 'F', rules: [{ required: menuType !== 'F', message: '请选择是否外链' }], children: _jsx(Radio.Group, { options: frameOptions }) }), _jsx(Form.Item, { label: "\u8DEF\u7531\u5730\u5740", name: "path", hidden: menuType === 'F', rules: [{ required: menuType !== 'F', message: '请输入路由地址' }], children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u7EC4\u4EF6\u8DEF\u5F84", name: "component", hidden: menuType !== 'C', children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u8DEF\u7531\u53C2\u6570", name: "query", hidden: menuType !== 'C', children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u8DEF\u7531\u540D\u79F0", name: "routeName", hidden: menuType !== 'C', children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u6743\u9650\u6807\u8BC6", name: "perms", hidden: menuType === 'M', children: _jsx(Input, { placeholder: "\u8BF7\u8F93\u5165" }) }), _jsx(Form.Item, { label: "\u662F\u5426\u7F13\u5B58", name: "isCache", hidden: menuType !== 'C', children: _jsx(Radio.Group, { options: cacheOptions }) }), _jsx(Form.Item, { label: "\u663E\u793A\u72B6\u6001", name: "visible", hidden: menuType === 'F', children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: visibleOptions }) }), _jsx(Form.Item, { label: "\u83DC\u5355\u72B6\u6001", name: "status", rules: [{ required: true, message: '请选择菜单状态' }], children: _jsx(Select, { ...SEARCHABLE_SELECT_PROPS, options: statusOptions }) }), _jsx(Form.Item, { label: "\u5907\u6CE8", name: "remark", children: _jsx(Input.TextArea, { rows: 3, placeholder: "\u8BF7\u8F93\u5165" }) })] }) })] }));
};
export default PartnerMenuModal;
