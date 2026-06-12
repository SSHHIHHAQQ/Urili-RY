import {
  ApartmentOutlined,
  AppstoreOutlined,
  AuditOutlined,
  DashboardOutlined,
  HistoryOutlined,
  LockOutlined,
  LogoutOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { PageContainer, ProLayout } from '@ant-design/pro-components';
import { history, useLocation } from '@umijs/max';
import {
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Grid,
  Input,
  Modal,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getTerminalAccessToken } from '@/access';
import { SelectLang } from '@/components';
import HeaderDropdown from '@/components/HeaderDropdown';
import { message } from '@/utils/feedback';
import { matchPermission } from '@/utils/permission';
import defaultSettings from '../../../../config/defaultSettings';
import {
  clearPortalLogin,
  getPortalTerminal,
  PORTAL_META,
  PORTAL_SERVICE,
  type PortalTerminal,
} from '../terminal';
import BuyerProductCenter from './BuyerProductCenter';
import PortalSelfManagement, { type PortalSelfManagementView } from './PortalSelfManagement';

type PortalHomeData = {
  info?: API.Partner.PortalPermissionInfo;
  subject?: API.Partner.PortalSubjectProfile;
  account?: API.Partner.PortalAccountProfile;
  accounts?: API.Partner.PortalAccountBase[];
  depts?: API.Partner.PortalDeptProfile[];
  roles?: API.Partner.PortalRoleProfile[];
};

type PortalSessionRow = API.Partner.PortalOwnSessionProfile & {
  uiRowKey: string;
};

type PasswordFormValues = API.Partner.PortalPasswordChangeParams;

type PortalViewKey =
  | 'workbench'
  | 'accounts'
  | 'roles'
  | 'depts'
  | 'sessions'
  | 'loginLogs'
  | 'operLogs'
  | 'productCenter';

type PortalMenuItem = {
  key: PortalViewKey;
  label: string;
  permission?: string;
  icon?: React.ReactNode;
  terminal?: PortalTerminal;
};

type PortalLayoutRoute = {
  path: string;
  name?: string;
  icon?: React.ReactNode;
  locale?: boolean;
  routes?: PortalLayoutRoute[];
};

const PORTAL_VIEW_LABELS: Record<PortalViewKey, string> = {
  workbench: '工作台',
  accounts: '用户管理',
  roles: '角色管理',
  depts: '部门管理',
  sessions: '在线会话',
  loginLogs: '登录日志',
  operLogs: '操作日志',
  productCenter: '商品中心',
};

const PORTAL_VIEW_PATH_SEGMENTS: Record<PortalViewKey, string> = {
  workbench: 'workbench',
  accounts: 'accounts',
  roles: 'roles',
  depts: 'depts',
  sessions: 'sessions',
  loginLogs: 'loginLogs',
  operLogs: 'operLogs',
  productCenter: 'product-center',
};

const ORGANIZATION_MENU_ITEMS: PortalMenuItem[] = [
  { key: 'accounts', label: PORTAL_VIEW_LABELS.accounts, permission: 'account:list', icon: <TeamOutlined /> },
  { key: 'roles', label: PORTAL_VIEW_LABELS.roles, permission: 'role:list', icon: <SafetyCertificateOutlined /> },
  { key: 'depts', label: PORTAL_VIEW_LABELS.depts, permission: 'dept:list', icon: <ApartmentOutlined /> },
  { key: 'sessions', label: PORTAL_VIEW_LABELS.sessions, permission: 'account:session:list', icon: <HistoryOutlined /> },
];

const AUDIT_MENU_ITEMS: PortalMenuItem[] = [
  { key: 'loginLogs', label: PORTAL_VIEW_LABELS.loginLogs, permission: 'account:loginLog:list' },
  { key: 'operLogs', label: PORTAL_VIEW_LABELS.operLogs, permission: 'account:operLog:list' },
];

const BUSINESS_MENU_ITEMS: PortalMenuItem[] = [
  {
    key: 'productCenter',
    label: PORTAL_VIEW_LABELS.productCenter,
    permission: 'product:center:list',
    icon: <AppstoreOutlined />,
    terminal: 'buyer',
  },
];

const SELF_MANAGEMENT_VIEWS: readonly PortalSelfManagementView[] = [
  'accounts',
  'depts',
  'roles',
  'loginLogs',
  'operLogs',
];

const gridStyle: React.CSSProperties = {
  display: 'grid',
  gap: 16,
};

const fullGridStyle: React.CSSProperties = {
  gridColumn: '1 / -1',
};

function hasPortalPermission(permissions: string[] | undefined, permission: string) {
  return matchPermission(permissions, permission);
}

function portalPermission(terminal: PortalTerminal, permission: string) {
  return `${terminal}:${permission}`;
}

function getPortalViewPath(terminal: PortalTerminal, view: PortalViewKey) {
  return `/${terminal}/portal/${PORTAL_VIEW_PATH_SEGMENTS[view]}`;
}

function normalizePathname(pathname: string) {
  return pathname.split(/[?#]/, 1)[0];
}

function getPortalView(pathname: string, terminal?: PortalTerminal): PortalViewKey {
  if (!terminal) {
    return 'workbench';
  }
  const normalizedPathname = normalizePathname(pathname);
  const basePath = `/${terminal}/portal`;
  const suffix = normalizedPathname === basePath ? '' : normalizedPathname.slice(basePath.length + 1);
  const segment = suffix.split('/', 1)[0];
  const entry = Object.entries(PORTAL_VIEW_PATH_SEGMENTS).find(([, value]) => value === segment);
  const view = entry?.[0] as PortalViewKey | undefined;
  return view && Object.prototype.hasOwnProperty.call(PORTAL_VIEW_LABELS, view) ? view : 'workbench';
}

function shouldNormalizePortalPath(pathname: string, terminal: PortalTerminal, view: PortalViewKey) {
  return normalizePathname(pathname) !== getPortalViewPath(terminal, view);
}

function isSelfManagementView(view: PortalViewKey): view is PortalSelfManagementView {
  return (SELF_MANAGEMENT_VIEWS as readonly string[]).includes(view);
}

function displayText(value?: string | number | null) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function assertPortalSuccess<T extends { code?: number | string; msg?: string }>(
  response: T,
  fallbackMessage: string,
) {
  if (Number(response?.code) !== 200) {
    throw new Error(response?.msg || fallbackMessage);
  }
  return response;
}

function renderTags(values?: string[]) {
  if (!values || values.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }
  return (
    <Space wrap size={[6, 6]}>
      {values.map((value) => (
        <Tag key={value}>{value}</Tag>
      ))}
    </Space>
  );
}

function renderRoleList(roles?: API.Partner.PortalRoleProfile[]) {
  if (!roles || roles.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }
  return (
    <Space wrap size={[6, 6]}>
      {roles.map((role) => (
        <Tag key={role.roleId}>{role.roleName || role.roleKey || role.roleId}</Tag>
      ))}
    </Space>
  );
}

function renderDeptList(depts?: API.Partner.PortalDeptProfile[]) {
  if (!depts || depts.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }
  return (
    <Space wrap size={[6, 6]}>
      {depts.map((dept) => (
        <Tag key={dept.deptId}>{dept.deptName || dept.deptId}</Tag>
      ))}
    </Space>
  );
}

function renderSessionStatus(record: API.Partner.PortalOwnSessionProfile) {
  if (record.current) {
    return <Tag color="processing">当前</Tag>;
  }
  if (record.logoutTime || record.status === '1') {
    return <Tag>已退出</Tag>;
  }
  if (record.status === '0') {
    return <Tag color="success">有效</Tag>;
  }
  return <Tag>{displayText(record.status)}</Tag>;
}

function buildRouteChildren(
  terminal: PortalTerminal,
  permissions: string[],
  items: PortalMenuItem[],
): PortalLayoutRoute[] {
  return items
    .filter((item) => !item.terminal || item.terminal === terminal)
    .filter((item) => !item.permission || hasPortalPermission(permissions, portalPermission(terminal, item.permission)))
    .map((item) => ({
      path: getPortalViewPath(terminal, item.key),
      name: item.label,
      icon: item.icon,
      locale: false,
    }));
}

function canAccessView(terminal: PortalTerminal, permissions: string[], view: PortalViewKey) {
  const item = [...ORGANIZATION_MENU_ITEMS, ...AUDIT_MENU_ITEMS, ...BUSINESS_MENU_ITEMS].find(
    (menuItem) => menuItem.key === view,
  );
  if (item?.terminal && item.terminal !== terminal) {
    return false;
  }
  return !item?.permission || hasPortalPermission(permissions, portalPermission(terminal, item.permission));
}

function buildPortalRoute(terminal: PortalTerminal, permissions: string[]): PortalLayoutRoute {
  const organizationChildren = buildRouteChildren(terminal, permissions, ORGANIZATION_MENU_ITEMS);
  const auditChildren = buildRouteChildren(terminal, permissions, AUDIT_MENU_ITEMS);
  const businessChildren = buildRouteChildren(terminal, permissions, BUSINESS_MENU_ITEMS);
  return {
    path: `/${terminal}/portal`,
    routes: [
      {
        path: getPortalViewPath(terminal, 'workbench'),
        name: PORTAL_VIEW_LABELS.workbench,
        icon: <DashboardOutlined />,
        locale: false,
      },
      ...businessChildren,
      ...(organizationChildren.length > 0
        ? [
            {
              path: `/${terminal}/portal/organization`,
              name: '组织权限',
              icon: <TeamOutlined />,
              locale: false,
              routes: organizationChildren,
            },
          ]
        : []),
      ...(auditChildren.length > 0
        ? [
            {
              path: `/${terminal}/portal/audit`,
              name: '日志审计',
              icon: <AuditOutlined />,
              locale: false,
              routes: auditChildren,
            },
          ]
        : []),
    ],
  };
}

const PortalHomePage: React.FC = () => {
  const location = useLocation();
  const screens = Grid.useBreakpoint();
  const terminal = useMemo(() => getPortalTerminal(location.pathname), [location.pathname]);
  const activeView = useMemo(() => getPortalView(location.pathname, terminal), [location.pathname, terminal]);
  const [collapsed, setCollapsed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [sessionLoading, setSessionLoading] = useState(false);
  const [data, setData] = useState<PortalHomeData>({});
  const [sessionRows, setSessionRows] = useState<PortalSessionRow[]>([]);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [passwordForm] = Form.useForm<PasswordFormValues>();
  const sessionRequestSeq = useRef(0);

  const clearSessions = useCallback(() => {
    sessionRequestSeq.current += 1;
    setSessionRows([]);
    setSessionLoading(false);
  }, []);

  const loadData = useCallback(async (currentTerminal: PortalTerminal) => {
    setLoading(true);
    try {
      const service = PORTAL_SERVICE[currentTerminal];
      const [infoResponse, subjectResponse, accountResponse] = await Promise.all([
        service.getInfo(),
        service.getSubjectProfile(),
        service.getAccountProfile(),
      ]);
      const infoRes = assertPortalSuccess(infoResponse, 'Portal info loading failed');
      const subjectRes = assertPortalSuccess(subjectResponse, 'Portal subject loading failed');
      const accountRes = assertPortalSuccess(accountResponse, 'Portal account loading failed');
      const permissions = infoRes.data?.permissions || [];
      const [accountsRes, deptsRes, rolesRes] = await Promise.all([
        hasPortalPermission(permissions, portalPermission(currentTerminal, 'account:list'))
          ? service.getAccounts().then((response) => assertPortalSuccess(response, 'Portal accounts loading failed'))
          : Promise.resolve({ code: 200, data: [] }),
        hasPortalPermission(permissions, portalPermission(currentTerminal, 'dept:list'))
          ? service.getDepts().then((response) => assertPortalSuccess(response, 'Portal depts loading failed'))
          : Promise.resolve({ code: 200, data: [] }),
        hasPortalPermission(permissions, portalPermission(currentTerminal, 'role:list'))
          ? service.getRoles().then((response) => assertPortalSuccess(response, 'Portal roles loading failed'))
          : Promise.resolve({ code: 200, data: [] }),
      ]);
      setData({
        info: infoRes.data,
        subject: subjectRes.data,
        account: accountRes.data,
        accounts: accountsRes.data || [],
        depts: deptsRes.data || [],
        roles: rolesRes.data || [],
      });
    } catch (error) {
      console.log(error);
      message.error('门户数据加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadSessions = useCallback(async (currentTerminal: PortalTerminal, pageSize: number) => {
    const requestSeq = sessionRequestSeq.current + 1;
    sessionRequestSeq.current = requestSeq;
    setSessionLoading(true);
    try {
      const response = assertPortalSuccess(
        await PORTAL_SERVICE[currentTerminal].getSessions({ pageNum: 1, pageSize }),
        'Portal sessions loading failed',
      );
      if (sessionRequestSeq.current === requestSeq) {
        setSessionRows(
          (response.rows || [])
            .slice(0, pageSize)
            .map((row, index) => ({
              ...row,
              uiRowKey: [
                currentTerminal,
                row.userName || '',
                row.loginTime || '',
                row.expireTime || '',
                row.logoutTime || '',
                row.status || '',
                row.current ? 'current' : 'history',
                index,
              ].join('-'),
            })),
        );
      }
    } catch (error) {
      console.log(error);
    } finally {
      if (sessionRequestSeq.current === requestSeq) {
        setSessionLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    if (!terminal) {
      history.replace('/seller/login');
      return;
    }
    if (!getTerminalAccessToken(terminal)) {
      history.replace(PORTAL_META[terminal].loginPath);
      return;
    }
    if (shouldNormalizePortalPath(location.pathname, terminal, activeView)) {
      history.replace(getPortalViewPath(terminal, activeView));
      return;
    }
    loadData(terminal);
  }, [activeView, loadData, location.pathname, terminal]);

  useEffect(() => {
    if (!terminal || !getTerminalAccessToken(terminal)) {
      return;
    }
    const permissions = data.info?.permissions;
    if (!permissions) {
      return;
    }
    const sessionViewActive = activeView === 'workbench' || activeView === 'sessions';
    if (
      sessionViewActive
      && hasPortalPermission(permissions, portalPermission(terminal, 'account:session:list'))
    ) {
      loadSessions(terminal, activeView === 'sessions' ? 20 : 5);
      return;
    }
    clearSessions();
  }, [activeView, clearSessions, data.info?.permissions, loadSessions, terminal]);

  const handleLogout = async () => {
    if (!terminal) {
      return;
    }
    try {
      await PORTAL_SERVICE[terminal].logout();
    } catch (error) {
      console.log(error);
    } finally {
      clearPortalLogin(terminal);
      history.replace(PORTAL_META[terminal].loginPath);
    }
  };

  const handlePasswordSubmit = async () => {
    if (!terminal) {
      return;
    }
    const values = await passwordForm.validateFields();
    setPasswordSubmitting(true);
    try {
      const response = await PORTAL_SERVICE[terminal].updatePassword(values);
      if (response.code === 200) {
        message.success('密码已更新');
        setPasswordOpen(false);
        passwordForm.resetFields();
      } else {
        message.error(response.msg || '密码更新失败');
      }
    } finally {
      setPasswordSubmitting(false);
    }
  };

  if (!terminal) {
    return null;
  }

  const meta = PORTAL_META[terminal];
  const permissions = data.info?.permissions || [];
  const canViewAccounts = hasPortalPermission(permissions, portalPermission(terminal, 'account:list'));
  const canViewDepts = hasPortalPermission(permissions, portalPermission(terminal, 'dept:list'));
  const canViewRoles = hasPortalPermission(permissions, portalPermission(terminal, 'role:list'));
  const canViewSessions = hasPortalPermission(permissions, portalPermission(terminal, 'account:session:list'));
  const canViewActivePage = canAccessView(terminal, permissions, activeView);
  const portalRoute = buildPortalRoute(terminal, permissions);
  const portalPathname = getPortalViewPath(terminal, activeView);
  const gridColumns = screens.lg ? 3 : screens.sm ? 2 : 1;
  const contentGridStyle: React.CSSProperties = {
    ...gridStyle,
    gridTemplateColumns: `repeat(${gridColumns}, minmax(240px, 1fr))`,
  };
  const wideGridStyle: React.CSSProperties = {
    gridColumn: gridColumns > 1 ? 'span 2' : '1 / -1',
  };
  const descriptionColumns = screens.md ? 2 : 1;
  const accountName = data.account?.nickName || data.account?.userName || '当前账号';

  const sessionColumns: ColumnsType<PortalSessionRow> = [
    {
      title: '状态',
      key: 'status',
      width: 96,
      render: (_, record) => renderSessionStatus(record),
    },
    {
      title: '登录账号',
      dataIndex: 'userName',
      key: 'userName',
      ellipsis: true,
      render: displayText,
    },
    {
      title: '登录 IP',
      dataIndex: 'loginIp',
      key: 'loginIp',
      width: 140,
      responsive: ['sm'],
      render: displayText,
    },
    {
      title: '登录时间',
      dataIndex: 'loginTime',
      key: 'loginTime',
      width: 180,
      render: displayText,
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 180,
      responsive: ['md'],
      render: displayText,
    },
    {
      title: '退出时间',
      dataIndex: 'logoutTime',
      key: 'logoutTime',
      width: 180,
      responsive: ['md'],
      render: displayText,
    },
  ];

  const handleRefresh = () => {
    clearSessions();
    loadData(terminal);
  };

  const renderWorkbench = () => (
    <div style={contentGridStyle}>
      <Card title="主体资料" variant="borderless" style={wideGridStyle}>
        <Descriptions column={descriptionColumns} size="small">
          <Descriptions.Item label="主体编号">{displayText(data.subject?.subjectNo)}</Descriptions.Item>
          <Descriptions.Item label="主体代码">{displayText(data.subject?.subjectCode)}</Descriptions.Item>
          <Descriptions.Item label="主体名称">{displayText(data.subject?.subjectName)}</Descriptions.Item>
          <Descriptions.Item label="状态">{displayText(data.subject?.status)}</Descriptions.Item>
          <Descriptions.Item label="联系人">{displayText(data.subject?.contactName)}</Descriptions.Item>
          <Descriptions.Item label="邮箱">{displayText(data.subject?.contactEmail)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="个人中心" variant="borderless">
        <Descriptions column={1} size="small">
          <Descriptions.Item label="账号">{displayText(data.account?.userName)}</Descriptions.Item>
          <Descriptions.Item label="姓名">{displayText(data.account?.nickName)}</Descriptions.Item>
          <Descriptions.Item label="部门">{displayText(data.account?.deptName)}</Descriptions.Item>
          <Descriptions.Item label="状态">{displayText(data.account?.status)}</Descriptions.Item>
        </Descriptions>
      </Card>

      {canViewRoles ? (
        <Card title="角色" variant="borderless">
          {renderRoleList(data.roles)}
        </Card>
      ) : null}

      {canViewDepts ? (
        <Card title="部门" variant="borderless">
          {renderDeptList(data.depts)}
        </Card>
      ) : null}

      {canViewAccounts ? (
        <Card title="用户数量" variant="borderless">
          <Typography.Title level={2} style={{ margin: 0 }}>
            {data.accounts?.length || 0}
          </Typography.Title>
          <Typography.Text type="secondary">当前端内可管理账号</Typography.Text>
        </Card>
      ) : null}

      {canViewSessions ? (
        <Card
          title="在线会话"
          variant="borderless"
          style={fullGridStyle}
          extra={<Button type="link" onClick={() => history.push(getPortalViewPath(terminal, 'sessions'))}>查看全部</Button>}
        >
          <Table<PortalSessionRow>
            size="small"
            rowKey="uiRowKey"
            loading={sessionLoading}
            pagination={false}
            columns={sessionColumns}
            dataSource={sessionRows}
            scroll={{ x: 780 }}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          />
        </Card>
      ) : null}

      <Card title="权限标识" variant="borderless" style={fullGridStyle}>
        {renderTags(permissions)}
      </Card>
    </div>
  );

  const renderSessions = () => (
    <Card
      title="在线会话"
      variant="borderless"
      extra={<Typography.Text type="secondary">只读列表，不提供强制下线操作</Typography.Text>}
    >
      <Table<PortalSessionRow>
        rowKey="uiRowKey"
        loading={sessionLoading}
        pagination={false}
        columns={sessionColumns}
        dataSource={sessionRows}
        scroll={{ x: 780 }}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
      />
    </Card>
  );

  const renderNoAccess = () => (
    <Card variant="borderless">
      <Empty description="当前账号没有该页面权限" />
    </Card>
  );

  const renderActiveContent = () => {
    if (activeView === 'workbench') {
      return renderWorkbench();
    }
    if (!canViewActivePage) {
      return renderNoAccess();
    }
    if (activeView === 'sessions') {
      return renderSessions();
    }
    if (activeView === 'productCenter' && terminal === 'buyer') {
      return <BuyerProductCenter permissions={permissions} />;
    }
    if (isSelfManagementView(activeView)) {
      return (
        <div style={gridStyle}>
          <PortalSelfManagement
            terminal={terminal}
            permissions={permissions}
            activeView={activeView}
            accounts={data.accounts || []}
            depts={data.depts || []}
            roles={data.roles || []}
            onChanged={handleRefresh}
          />
        </div>
      );
    }
    return renderWorkbench();
  };

  return (
    <ProLayout
      {...defaultSettings}
      title={meta.label}
      route={portalRoute}
      location={{ pathname: portalPathname }}
      collapsed={collapsed}
      onCollapse={setCollapsed}
      menu={{ locale: false }}
      breadcrumbRender={false}
      footerRender={false}
      pageTitleRender={false}
      menuItemRender={(item, defaultDom) => {
        if (!item.path) {
          return defaultDom;
        }
        return (
          <a
            onClick={(event) => {
              event.preventDefault();
              history.push(item.path || portalPathname);
            }}
          >
            {defaultDom}
          </a>
        );
      }}
      actionsRender={() => [
        <Button key="refresh" type="text" icon={<ReloadOutlined />} onClick={handleRefresh}>
          刷新
        </Button>,
        <SelectLang key="SelectLang" />,
      ]}
      avatarProps={{
        icon: <UserOutlined />,
        title: accountName,
        render: (_, dom) => (
          <HeaderDropdown
            menu={{
              selectedKeys: [],
              items: [
                { key: 'profile', icon: <UserOutlined />, label: '个人中心' },
                { key: 'password', icon: <LockOutlined />, label: '修改密码' },
                { type: 'divider' },
                { key: 'logout', danger: true, icon: <LogoutOutlined />, label: '退出登录' },
              ],
              onClick: ({ key }) => {
                if (key === 'profile') {
                  history.push(getPortalViewPath(terminal, 'workbench'));
                }
                if (key === 'password') {
                  setPasswordOpen(true);
                }
                if (key === 'logout') {
                  handleLogout();
                }
              },
            }}
          >
            {dom}
          </HeaderDropdown>
        ),
      }}
    >
      <PageContainer title={false}>
        <Spin spinning={loading}>{renderActiveContent()}</Spin>
      </PageContainer>

      <Modal
        title="修改密码"
        open={passwordOpen}
        onCancel={() => {
          setPasswordOpen(false);
          passwordForm.resetFields();
        }}
        onOk={handlePasswordSubmit}
        confirmLoading={passwordSubmitting}
        destroyOnHidden
      >
        <Form form={passwordForm} layout="vertical" preserve={false}>
          <Form.Item name="oldPassword" label="旧密码" rules={[{ required: true, message: '请输入旧密码' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item name="newPassword" label="新密码" rules={[{ required: true, message: '请输入新密码' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的新密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </ProLayout>
  );
};

export default PortalHomePage;
