import React, { useEffect, useMemo, useState } from 'react';
import { App, Flex, Modal, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { PartnerModuleConfig } from './PartnerManagementPage';

type MenuRecord = API.Partner.PortalMenu & Record<string, any>;

type PartnerMenuModalProps = {
  config: PartnerModuleConfig;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

const menuTypeOptions = [
  { label: '目录', value: 'M' },
  { label: '菜单', value: 'C' },
  { label: '按钮', value: 'F' },
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
  return (
    <Typography.Text style={compactCellTextStyle} title={text}>
      {text}
    </Typography.Text>
  );
}

function renderMenuType(value: string | undefined) {
  const match = menuTypeOptions.find((item) => item.value === value);
  return (
    <Tag color={value === 'F' ? 'default' : value === 'M' ? 'processing' : 'success'}>
      {match?.label || '-'}
    </Tag>
  );
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
        return;
      }
      delete node.children;
    });
  };

  sortTree(roots);
  return roots;
}

const PartnerMenuModal: React.FC<PartnerMenuModalProps> = ({ config, open, onOpenChange }) => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [menus, setMenus] = useState<MenuRecord[]>([]);

  const menuTree = useMemo(() => buildMenuTree(menus), [menus]);

  const loadMenus = async () => {
    setLoading(true);
    try {
      const resp = await config.services.listMenus();
      if (resp.code === 200) {
        setMenus((resp.data || []) as MenuRecord[]);
        return;
      }
      message.error(resp.msg || '菜单模板加载失败');
    } catch {
      message.error('菜单模板加载失败，请重试');
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
  ];

  return (
    <Modal
      width={1080}
      title={`${config.label}端权限模板`}
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
      />
    </Modal>
  );
};

export default PartnerMenuModal;
