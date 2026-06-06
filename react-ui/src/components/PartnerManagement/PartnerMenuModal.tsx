import React, { useEffect, useMemo, useState } from 'react';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Table,
  Tag,
  TreeSelect,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import type { PartnerModuleConfig } from './PartnerManagementPage';

type MenuRecord = API.Partner.PortalMenu & Record<string, any>;

type PartnerMenuModalProps = {
  config: PartnerModuleConfig;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

type MenuFormValues = {
  menuName?: string;
  parentId?: number;
  orderNum?: number;
  path?: string;
  component?: string;
  query?: string;
  routeName?: string;
  isFrame?: string;
  isCache?: string;
  menuType?: string;
  visible?: string;
  status?: string;
  perms?: string;
  icon?: string;
  remark?: string;
};

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

const compactCellTextStyle: React.CSSProperties = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  lineHeight: 1.35,
};

function renderCompactText(value: unknown) {
  const text = value == null || value === '' ? '-' : String(value);
  return <Typography.Text style={compactCellTextStyle} title={text}>{text}</Typography.Text>;
}

function renderMenuType(value: string | undefined) {
  const match = menuTypeOptions.find((item) => item.value === value);
  return <Tag color={value === 'F' ? 'default' : value === 'M' ? 'processing' : 'success'}>{match?.label || '-'}</Tag>;
}

function renderStatus(value: string | undefined) {
  return <Tag color={value === '0' ? 'success' : 'default'}>{value === '0' ? '正常' : '停用'}</Tag>;
}

function renderVisible(value: string | undefined) {
  return <Tag color={value === '0' ? 'processing' : 'default'}>{value === '0' ? '显示' : '隐藏'}</Tag>;
}

function buildMenuTree(menus: MenuRecord[]): MenuRecord[] {
  const nodeMap = new Map<number, MenuRecord>();
  const roots: MenuRecord[] = [];

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

  const sortTree = (nodes: MenuRecord[]) => {
    nodes.sort((a, b) => Number(a.orderNum || 0) - Number(b.orderNum || 0));
    nodes.forEach((node) => {
      if (node.children?.length) {
        sortTree(node.children as MenuRecord[]);
      }
    });
  };

  sortTree(roots);
  return roots;
}

function toTreeSelectData(nodes: MenuRecord[], currentMenuId?: number): any[] {
  return nodes.map((node) => ({
    key: node.menuId,
    value: node.menuId,
    title: node.menuName || String(node.menuId),
    disabled: currentMenuId != null && node.menuId === currentMenuId,
    children: node.children?.length ? toTreeSelectData(node.children as MenuRecord[], currentMenuId) : undefined,
  }));
}

function mapMenuToForm(menu?: MenuRecord): MenuFormValues {
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

function buildMenuPayload(currentMenu: MenuRecord | undefined, values: MenuFormValues): API.Partner.PortalMenu {
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

function getOppositeModuleKey(moduleKey: PartnerModuleConfig['moduleKey']) {
  return moduleKey === 'seller' ? 'buyer' : 'seller';
}

const forbiddenPathRoots = new Set(['admin', 'common', 'shared', 'system', 'account', 'monitor', 'tool']);
const forbiddenComponentRoots = new Set(['Admin', 'Common', 'Shared', 'System', 'User', 'Monitor', 'Tool']);

function getFirstSegment(value: string) {
  return value.trim().replace(/^\/+/, '').split('/')[0] || '';
}

function validateMenuPathForTerminal(moduleKey: PartnerModuleConfig['moduleKey'], value?: string) {
  const normalized = (value || '').trim().replace(/^\/+/, '').toLowerCase();
  const opposite = getOppositeModuleKey(moduleKey);
  const root = getFirstSegment(normalized);
  if (!normalized || normalized === '#') {
    return Promise.resolve();
  }
  if (root === opposite) {
    return Promise.reject(new Error('menu path cannot point to the opposite terminal'));
  }
  if (forbiddenPathRoots.has(root)) {
    return Promise.reject(new Error('menu path cannot point to admin or shared control roots'));
  }
  if (normalized === opposite || normalized.startsWith(`${opposite}/`)) {
    return Promise.reject(new Error(`路由不能指向${opposite}端`));
  }
  return Promise.resolve();
}

function validateMenuComponentForTerminal(moduleKey: PartnerModuleConfig['moduleKey'], value?: string) {
  const normalized = (value || '').trim();
  const oppositeRoot = moduleKey === 'seller' ? 'Buyer' : 'Seller';
  const root = getFirstSegment(normalized);
  if (!normalized) {
    return Promise.resolve();
  }
  if (root === oppositeRoot) {
    return Promise.reject(new Error('menu component cannot point to the opposite terminal'));
  }
  if (forbiddenComponentRoots.has(root)) {
    return Promise.reject(new Error('menu component cannot point to admin or shared control roots'));
  }
  if (normalized === oppositeRoot || normalized.startsWith(`${oppositeRoot}/`)) {
    return Promise.reject(new Error(`组件不能指向${oppositeRoot}端页面`));
  }
  return Promise.resolve();
}

function validateMenuPermsForTerminal(moduleKey: PartnerModuleConfig['moduleKey'], value?: string) {
  const normalized = (value || '').trim();
  if (normalized && !normalized.startsWith(`${moduleKey}:`)) {
    return Promise.reject(new Error(`权限标识必须以 ${moduleKey}: 开头`));
  }
  if (normalized === `${moduleKey}:admin` || normalized.startsWith(`${moduleKey}:admin:`)) {
    return Promise.reject(new Error('terminal menu cannot use admin permission namespace'));
  }
  return Promise.resolve();
}

const PartnerMenuModal: React.FC<PartnerMenuModalProps> = ({ config, open, onOpenChange }) => {
  const { message, modal } = App.useApp();
  const access = useAccess();
  const [form] = Form.useForm<MenuFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [currentMenu, setCurrentMenu] = useState<MenuRecord>();
  const [menus, setMenus] = useState<MenuRecord[]>([]);
  const [menuType, setMenuType] = useState('C');

  const permPrefix = `${config.moduleKey}:admin`;
  const currentMenuId = currentMenu?.menuId;
  const canEditMenu = access.hasPerms(`${permPrefix}:menu:edit`) && access.hasPerms(`${permPrefix}:menu:query`);
  const menuTree = useMemo(() => buildMenuTree(menus), [menus]);
  const parentTreeData = useMemo(
    () => [
      {
        key: 0,
        value: 0,
        title: '主类目',
        children: toTreeSelectData(menuTree, currentMenuId),
      },
    ],
    [menuTree, currentMenuId],
  );

  const loadMenus = async () => {
    setLoading(true);
    try {
      const resp = await config.services.listMenus();
      if (resp.code === 200) {
        setMenus((resp.data || []) as MenuRecord[]);
        return;
      }
      message.error(resp.msg || '菜单列表加载失败');
    } catch {
      message.error('菜单列表加载失败，请重试');
    } finally {
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

  const openMenuForm = async (menu?: MenuRecord) => {
    setCurrentMenu(menu);
    setFormOpen(true);
    if (!menu?.menuId) {
      return;
    }
    try {
      const resp = await config.services.getMenu(menu.menuId);
      if (resp.code === 200) {
        setCurrentMenu(resp.data as MenuRecord);
        return;
      }
      message.error(resp.msg || '菜单详情加载失败');
    } catch {
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
    } catch {
      message.error('菜单保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = (menuRecord: MenuRecord) => {
    if (!menuRecord.menuId) {
      return;
    }
    modal.confirm({
      title: `确认删除菜单 ${menuRecord.menuName || menuRecord.menuId} 吗？`,
      content: '已有下级菜单或角色绑定时，后端会按端内权限规则处理。',
      onOk: async () => {
        const resp = await config.services.removeMenu(menuRecord.menuId as number);
        if (resp.code === 200) {
          message.success('菜单已删除');
          await loadMenus();
          return;
        }
        message.error(resp.msg || '菜单删除失败');
      },
    });
  };

  const columns: ColumnsType<MenuRecord> = [
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
      render: (_, record) => (
        <Flex vertical gap={0}>
          {renderCompactText(record.path)}
          <Typography.Text style={compactCellTextStyle} type="secondary" title={record.component || '-'}>
            {record.component || '-'}
          </Typography.Text>
        </Flex>
      ),
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
      render: (_, record) => (
        <Flex gap={4}>
          <Button
            type="link"
            size="small"
            hidden={!canEditMenu}
            onClick={() => void openMenuForm(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            hidden={!access.hasPerms(`${permPrefix}:menu:remove`)}
            onClick={() => handleRemove(record)}
          >
            删除
          </Button>
        </Flex>
      ),
    },
  ];

  return (
    <>
      <Modal
        width={1080}
        title={`${config.label}端菜单配置`}
        open={open}
        destroyOnHidden
        footer={null}
        onCancel={() => onOpenChange(false)}
      >
        <Table<MenuRecord>
          rowKey={(record) => String(record.menuId)}
          loading={loading}
          columns={columns}
          dataSource={menuTree}
          size="small"
          pagination={false}
          tableLayout="fixed"
          expandable={{ defaultExpandAllRows: true }}
          title={() => (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              hidden={!access.hasPerms(`${permPrefix}:menu:add`)}
              onClick={() => void openMenuForm()}
            >
              新增菜单
            </Button>
          )}
        />
      </Modal>

      <Modal
        width={720}
        title={currentMenuId ? '编辑菜单' : '新增菜单'}
        open={formOpen}
        destroyOnHidden
        confirmLoading={saving}
        onOk={handleSubmit}
        onCancel={closeMenuForm}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="上级菜单" name="parentId" rules={[{ required: true, message: '请选择上级菜单' }]}>
            <TreeSelect
              {...SEARCHABLE_TREE_SELECT_PROPS}
              treeData={parentTreeData}
              treeDefaultExpandAll
              placeholder="请选择"
            />
          </Form.Item>
          <Form.Item label="菜单类型" name="menuType" rules={[{ required: true, message: '请选择菜单类型' }]}>
            <Radio.Group
              options={menuTypeOptions}
              onChange={(event) => setMenuType(event.target.value)}
            />
          </Form.Item>
          <Form.Item label="菜单名称" name="menuName" rules={[{ required: true, message: '请输入菜单名称' }]}>
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="显示顺序" name="orderNum" rules={[{ required: true, message: '请输入显示顺序' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="图标" name="icon" hidden={menuType === 'F'}>
            <Input placeholder="请输入图标标识" />
          </Form.Item>
          <Form.Item
            label="是否外链"
            name="isFrame"
            hidden={menuType === 'F'}
            rules={[{ required: menuType !== 'F', message: '请选择是否外链' }]}
          >
            <Radio.Group options={frameOptions} />
          </Form.Item>
          <Form.Item
            label="路由地址"
            name="path"
            hidden={menuType === 'F'}
            rules={[
              { required: menuType !== 'F', message: '请输入路由地址' },
              { validator: (_, value) => validateMenuPathForTerminal(config.moduleKey, value) },
            ]}
          >
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item
            label="组件路径"
            name="component"
            hidden={menuType !== 'C'}
            rules={[{ validator: (_, value) => validateMenuComponentForTerminal(config.moduleKey, value) }]}
          >
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="路由参数" name="query" hidden={menuType !== 'C'}>
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="路由名称" name="routeName" hidden={menuType !== 'C'}>
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item
            label="权限标识"
            name="perms"
            hidden={menuType === 'M'}
            rules={[{ validator: (_, value) => validateMenuPermsForTerminal(config.moduleKey, value) }]}
          >
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="是否缓存" name="isCache" hidden={menuType !== 'C'}>
            <Radio.Group options={cacheOptions} />
          </Form.Item>
          <Form.Item label="显示状态" name="visible" hidden={menuType === 'F'}>
            <Select {...SEARCHABLE_SELECT_PROPS} options={visibleOptions} />
          </Form.Item>
          <Form.Item label="菜单状态" name="status" rules={[{ required: true, message: '请选择菜单状态' }]}>
            <Select {...SEARCHABLE_SELECT_PROPS} options={statusOptions} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} placeholder="请输入" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default PartnerMenuModal;
