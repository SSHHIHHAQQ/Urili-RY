import { getTerminalAccessToken } from '@/access';
import {
  assignPortalAccountRoles,
  createPortalAccount,
  createPortalDept,
  createPortalRole,
  deletePortalDept,
  deletePortalRole,
  getBuyerPortalDistributionProduct,
  getBuyerPortalDistributionProducts,
  getBuyerPortalDistributionProductSkus,
  getBuyerPortalProductCategories,
  getBuyerPortalProductSchema,
  getPortalAccountProfile,
  getPortalAccountRoles,
  getPortalAccounts,
  getPortalDept,
  getPortalDeptTree,
  getPortalDepts,
  getPortalInfo,
  getPortalLoginLogs,
  getPortalOperLogs,
  getPortalRole,
  getPortalRoleMenus,
  getPortalRoles,
  getPortalRouters,
  getPortalSessions,
  getPortalSubjectProfile,
  getSellerPortalDistributionProduct,
  getSellerPortalDistributionProducts,
  getSellerPortalDistributionProductSkus,
  getSellerPortalProductCategories,
  getSellerPortalProductSchema,
  portalLogout,
  updatePortalAccount,
  updatePortalDept,
  updatePortalPassword,
  updatePortalRole,
} from '@/services/portal/session';
import { getPortalTerminalFromPath, isPortalRoute, isPortalTerminalPath } from '@/utils/portalPaths';
import { getPortalTerminalFromApiUrl } from '@/utils/portalRequest';
import { request } from '@umijs/max';

jest.mock('@umijs/max', () => ({
  request: jest.fn(),
}));

jest.mock('@/access', () => ({
  getTerminalAccessToken: jest.fn(),
}));

const mockedRequest = request as jest.Mock;
const mockedGetTerminalAccessToken = getTerminalAccessToken as jest.Mock;

const portalPasswordFieldNames = {
  old: 'oldPassword',
  next: 'newPassword',
} as const;

const portalPasswordChangeData = {
  [portalPasswordFieldNames.old]: 'old-pass',
  [portalPasswordFieldNames.next]: 'new-pass',
} as API.Partner.PortalPasswordChangeParams & Record<string, string>;

type PortalRequestCase = {
  name: string;
  terminal: 'seller' | 'buyer';
  url: string;
  method: string;
  call: () => Promise<unknown>;
  params?: Record<string, unknown>;
  data?: Record<string, unknown>;
};

const portalAuthenticatedRequestCases: PortalRequestCase[] = [
  {
    name: 'seller logout',
    terminal: 'seller',
    url: '/api/seller/logout',
    method: 'POST',
    call: () => portalLogout('seller'),
  },
  {
    name: 'buyer getInfo',
    terminal: 'buyer',
    url: '/api/buyer/getInfo',
    method: 'GET',
    call: () => getPortalInfo('buyer'),
  },
  {
    name: 'seller getRouters',
    terminal: 'seller',
    url: '/api/seller/getRouters',
    method: 'GET',
    call: () => getPortalRouters('seller'),
  },
  {
    name: 'buyer subject profile',
    terminal: 'buyer',
    url: '/api/buyer/profile',
    method: 'GET',
    call: () => getPortalSubjectProfile('buyer'),
  },
  {
    name: 'seller account profile',
    terminal: 'seller',
    url: '/api/seller/account/profile',
    method: 'GET',
    call: () => getPortalAccountProfile('seller'),
  },
  {
    name: 'buyer password update',
    terminal: 'buyer',
    url: '/api/buyer/account/password',
    method: 'PUT',
    data: portalPasswordChangeData,
    call: () =>
      updatePortalPassword('buyer', {
        ...portalPasswordChangeData,
        buyerId: 12,
        subjectId: 13,
      } as any),
  },
  {
    name: 'seller accounts',
    terminal: 'seller',
    url: '/api/seller/accounts',
    method: 'GET',
    call: () => getPortalAccounts('seller'),
  },
  {
    name: 'seller account create',
    terminal: 'seller',
    url: '/api/seller/accounts',
    method: 'POST',
    data: { userName: 'sub-seller', nickName: 'Sub Seller', password: 'P12345', accountRole: 'STAFF' },
    call: () =>
      createPortalAccount('seller', {
        userName: 'sub-seller',
        nickName: 'Sub Seller',
        password: 'P12345',
        accountRole: 'STAFF',
        sellerId: 11,
      } as any),
  },
  {
    name: 'seller account update',
    terminal: 'seller',
    url: '/api/seller/accounts/7',
    method: 'PUT',
    data: { nickName: 'Edited Seller', status: '0' },
    call: () =>
      updatePortalAccount('seller', 7, {
        accountId: 9,
        sellerId: 11,
        nickName: 'Edited Seller',
        status: '0',
      } as any),
  },
  {
    name: 'buyer account roles',
    terminal: 'buyer',
    url: '/api/buyer/accounts/8/roles',
    method: 'GET',
    call: () => getPortalAccountRoles('buyer', 8),
  },
  {
    name: 'buyer account role assign',
    terminal: 'buyer',
    url: '/api/buyer/accounts/8/roles',
    method: 'PUT',
    data: { roleIds: [21, 22] },
    call: () => assignPortalAccountRoles('buyer', 8, [21, 22]),
  },
  {
    name: 'buyer departments',
    terminal: 'buyer',
    url: '/api/buyer/depts',
    method: 'GET',
    call: () => getPortalDepts('buyer'),
  },
  {
    name: 'seller department detail',
    terminal: 'seller',
    url: '/api/seller/depts/4',
    method: 'GET',
    call: () => getPortalDept('seller', 4),
  },
  {
    name: 'seller department tree',
    terminal: 'seller',
    url: '/api/seller/depts/treeselect',
    method: 'GET',
    call: () => getPortalDeptTree('seller'),
  },
  {
    name: 'seller department create',
    terminal: 'seller',
    url: '/api/seller/depts',
    method: 'POST',
    data: { deptName: 'Ops', status: '0' },
    call: () => createPortalDept('seller', { deptName: 'Ops', status: '0', subjectId: 11 } as any),
  },
  {
    name: 'buyer department update',
    terminal: 'buyer',
    url: '/api/buyer/depts/5',
    method: 'PUT',
    data: { deptName: 'Finance', status: '0' },
    call: () => updatePortalDept('buyer', 5, { deptName: 'Finance', status: '0', buyerId: 12 } as any),
  },
  {
    name: 'buyer department delete',
    terminal: 'buyer',
    url: '/api/buyer/depts/5',
    method: 'DELETE',
    call: () => deletePortalDept('buyer', 5),
  },
  {
    name: 'seller roles',
    terminal: 'seller',
    url: '/api/seller/roles',
    method: 'GET',
    call: () => getPortalRoles('seller'),
  },
  {
    name: 'seller role detail',
    terminal: 'seller',
    url: '/api/seller/roles/3',
    method: 'GET',
    call: () => getPortalRole('seller', 3),
  },
  {
    name: 'seller role menus',
    terminal: 'seller',
    url: '/api/seller/roles/3/menus',
    method: 'GET',
    call: () => getPortalRoleMenus('seller', 3),
  },
  {
    name: 'buyer role menu template',
    terminal: 'buyer',
    url: '/api/buyer/roles/menus',
    method: 'GET',
    call: () => getPortalRoleMenus('buyer'),
  },
  {
    name: 'seller role create',
    terminal: 'seller',
    url: '/api/seller/roles',
    method: 'POST',
    data: { roleName: 'Ops', roleKey: 'ops', menuIds: [100001] },
    call: () => createPortalRole('seller', { roleName: 'Ops', roleKey: 'ops', menuIds: [100001], sellerId: 11 } as any),
  },
  {
    name: 'buyer role update',
    terminal: 'buyer',
    url: '/api/buyer/roles/6',
    method: 'PUT',
    data: { roleName: 'Audit', roleKey: 'audit', menuIds: [200001] },
    call: () =>
      updatePortalRole('buyer', 6, {
        roleName: 'Audit',
        roleKey: 'audit',
        menuIds: [200001],
        buyerId: 12,
      } as any),
  },
  {
    name: 'buyer role delete',
    terminal: 'buyer',
    url: '/api/buyer/roles/6',
    method: 'DELETE',
    call: () => deletePortalRole('buyer', 6),
  },
  {
    name: 'seller login logs',
    terminal: 'seller',
    url: '/api/seller/account/login-logs',
    method: 'GET',
    params: { pageNum: 1 },
    call: () => getPortalLoginLogs('seller', { pageNum: 1, sellerId: 11 }),
  },
  {
    name: 'buyer operation logs',
    terminal: 'buyer',
    url: '/api/buyer/account/oper-logs',
    method: 'GET',
    params: { pageNum: 2 },
    call: () => getPortalOperLogs('buyer', { pageNum: 2, buyerId: 12 }),
  },
  {
    name: 'seller sessions',
    terminal: 'seller',
    url: '/api/seller/account/sessions',
    method: 'GET',
    params: { pageNum: 1, pageSize: 5 },
    call: () =>
      getPortalSessions('seller', {
        pageNum: 1,
        pageSize: 5,
        accountId: 99,
        ipaddr: '127.0.0.1',
      } as any),
  },
  {
    name: 'seller product categories',
    terminal: 'seller',
    url: '/api/seller/product/categories',
    method: 'GET',
    call: () => getSellerPortalProductCategories(),
  },
  {
    name: 'seller product schema',
    terminal: 'seller',
    url: '/api/seller/product/categories/101/schema',
    method: 'GET',
    call: () => getSellerPortalProductSchema(101),
  },
  {
    name: 'seller product list',
    terminal: 'seller',
    url: '/api/seller/product/distribution-products/list',
    method: 'GET',
    params: { spuName: 'chair' },
    call: () => getSellerPortalDistributionProducts({ sellerId: 11, spuName: 'chair' }),
  },
  {
    name: 'seller product detail',
    terminal: 'seller',
    url: '/api/seller/product/distribution-products/1001',
    method: 'GET',
    call: () => getSellerPortalDistributionProduct(1001),
  },
  {
    name: 'seller product skus',
    terminal: 'seller',
    url: '/api/seller/product/distribution-products/1001/skus',
    method: 'GET',
    call: () => getSellerPortalDistributionProductSkus(1001),
  },
  {
    name: 'buyer product categories',
    terminal: 'buyer',
    url: '/api/buyer/product/categories',
    method: 'GET',
    call: () => getBuyerPortalProductCategories(),
  },
  {
    name: 'buyer product schema',
    terminal: 'buyer',
    url: '/api/buyer/product/categories/201/schema',
    method: 'GET',
    call: () => getBuyerPortalProductSchema(201),
  },
  {
    name: 'buyer product list',
    terminal: 'buyer',
    url: '/api/buyer/product/distribution-products/list',
    method: 'GET',
    params: { spuName: 'desk' },
    call: () => getBuyerPortalDistributionProducts({ buyerId: 12, spuName: 'desk' }),
  },
  {
    name: 'buyer product detail',
    terminal: 'buyer',
    url: '/api/buyer/product/distribution-products/2001',
    method: 'GET',
    call: () => getBuyerPortalDistributionProduct(2001),
  },
  {
    name: 'buyer product skus',
    terminal: 'buyer',
    url: '/api/buyer/product/distribution-products/2001/skus',
    method: 'GET',
    call: () => getBuyerPortalDistributionProductSkus(2001),
  },
];

describe('portal request isolation', () => {
  beforeEach(() => {
    mockedRequest.mockResolvedValue({} as never);
    mockedGetTerminalAccessToken.mockImplementation((terminal) => `${terminal}-token`);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('classifies only non-admin seller and buyer API paths as portal requests', () => {
    expect(getPortalTerminalFromApiUrl('/api/seller/account/login-logs')).toBe('seller');
    expect(getPortalTerminalFromApiUrl('https://example.test/api/buyer/product/list?pageNum=1')).toBe(
      'buyer',
    );
    expect(getPortalTerminalFromApiUrl('/api/seller/admin/list')).toBeUndefined();
    expect(getPortalTerminalFromApiUrl('/api/buyer/admin/list')).toBeUndefined();
    expect(getPortalTerminalFromApiUrl('/seller')).toBeUndefined();
    expect(getPortalTerminalFromApiUrl('/buyer')).toBeUndefined();
    expect(getPortalTerminalFromApiUrl('/api/system/user/list')).toBeUndefined();
  });

  it('classifies only login, direct-login, and portal page paths as portal routes', () => {
    expect(getPortalTerminalFromPath('/seller/portal/orders?status=pending#row')).toBe('seller');
    expect(getPortalTerminalFromPath('/buyer/login?redirect=/buyer/portal/account')).toBe('buyer');
    expect(isPortalTerminalPath('/buyer/portal/account/logs', 'buyer')).toBe(true);
    expect(isPortalTerminalPath('/seller/login/next', 'seller')).toBe(false);
    expect(isPortalTerminalPath('/seller/direct-login/next', 'seller')).toBe(false);
    expect(isPortalRoute('/seller/login/next')).toBe(false);
    expect(isPortalRoute('/seller/direct-login/next')).toBe(false);
    expect(isPortalTerminalPath('https://evil.test/seller/portal/account', 'seller')).toBe(false);
    expect(isPortalTerminalPath('//evil.test/seller/portal/account', 'seller')).toBe(false);
    expect(getPortalTerminalFromPath('https://evil.test/buyer/portal/account')).toBeUndefined();
    expect(isPortalRoute('//evil.test/buyer/portal/account')).toBe(false);
    expect(isPortalTerminalPath('/buyer/admin/menus', 'buyer')).toBe(false);
    expect(getPortalTerminalFromPath('/seller/accounts')).toBeUndefined();
    expect(getPortalTerminalFromPath('/seller')).toBeUndefined();
    expect(isPortalRoute('/buyer')).toBe(false);
  });

  it('uses the selected terminal token and strips caller-controlled scope params', async () => {
    await getPortalLoginLogs('seller', {
      pageNum: 1,
      pageSize: 10,
      userName: 'seller-user',
      sellerId: 11,
      buyerId: 12,
      subjectId: 13,
      accountId: 14,
      sellerAccountId: 15,
      buyerAccountId: 16,
      terminal: 'buyer',
    });

    expect(mockedRequest).toHaveBeenCalledWith('/api/seller/account/login-logs', {
      method: 'GET',
      headers: { Authorization: 'Bearer seller-token', isToken: false },
      params: {
        pageNum: 1,
        pageSize: 10,
        userName: 'seller-user',
      },
    });
  });

  it('applies scope stripping consistently to portal audit and product list requests', async () => {
    await getPortalOperLogs('buyer', { pageNum: 2, buyerId: 12, operName: 'edit' });
    await getPortalSessions('buyer', {
      pageNum: 3,
      accountId: 99,
      ipaddr: '127.0.0.1',
    } as any);
    await getSellerPortalDistributionProducts({ sellerId: 11, spuName: 'sku' });
    await getBuyerPortalDistributionProducts({ buyerId: 12, spuName: 'sku' });

    expect(mockedRequest).toHaveBeenNthCalledWith(1, '/api/buyer/account/oper-logs', {
      method: 'GET',
      headers: { Authorization: 'Bearer buyer-token', isToken: false },
      params: { pageNum: 2, operName: 'edit' },
    });
    expect(mockedRequest).toHaveBeenNthCalledWith(2, '/api/buyer/account/sessions', {
      method: 'GET',
      headers: { Authorization: 'Bearer buyer-token', isToken: false },
      params: { pageNum: 3 },
    });
    expect(mockedRequest).toHaveBeenNthCalledWith(
      3,
      '/api/seller/product/distribution-products/list',
      {
        method: 'GET',
        headers: { Authorization: 'Bearer seller-token', isToken: false },
        params: { spuName: 'sku' },
      },
    );
    expect(mockedRequest).toHaveBeenNthCalledWith(
      4,
      '/api/buyer/product/distribution-products/list',
      {
        method: 'GET',
        headers: { Authorization: 'Bearer buyer-token', isToken: false },
        params: { spuName: 'sku' },
      },
    );
  });

  it('normalizes portal role and menu id arrays before write requests', async () => {
    await assignPortalAccountRoles('seller', 8, ['21', 22] as any);
    await createPortalRole('seller', {
      roleName: 'Ops',
      roleKey: 'ops',
      menuIds: ['100001', 100002],
    } as any);
    await updatePortalRole('buyer', 6, {
      roleName: 'Audit',
      roleKey: 'audit',
      menuIds: ['200001'],
    } as any);

    expect(mockedRequest).toHaveBeenNthCalledWith(1, '/api/seller/accounts/8/roles', {
      method: 'PUT',
      headers: { Authorization: 'Bearer seller-token', isToken: false },
      data: { roleIds: [21, 22] },
    });
    expect(mockedRequest).toHaveBeenNthCalledWith(2, '/api/seller/roles', {
      method: 'POST',
      headers: { Authorization: 'Bearer seller-token', isToken: false },
      data: { roleName: 'Ops', roleKey: 'ops', menuIds: [100001, 100002] },
    });
    expect(mockedRequest).toHaveBeenNthCalledWith(3, '/api/buyer/roles/6', {
      method: 'PUT',
      headers: { Authorization: 'Bearer buyer-token', isToken: false },
      data: { roleName: 'Audit', roleKey: 'audit', menuIds: [200001] },
    });
  });

  it('normalizes portal path identifiers before request URLs are built', async () => {
    await updatePortalAccount('seller', '7' as any, { nickName: 'Edited Seller' } as any);
    await getPortalAccountRoles('buyer', '8' as any);
    await getPortalDept('seller', '4' as any);
    await updatePortalDept('buyer', '5' as any, { deptName: 'Finance' } as any);
    await deletePortalDept('buyer', '5' as any);
    await getPortalRole('seller', '3' as any);
    await getPortalRoleMenus('seller', '3' as any);
    await updatePortalRole('buyer', '6' as any, {
      roleName: 'Audit',
      roleKey: 'audit',
      menuIds: [200001],
    } as any);
    await deletePortalRole('buyer', '6' as any);

    expect(mockedRequest).toHaveBeenNthCalledWith(1, '/api/seller/accounts/7', expect.any(Object));
    expect(mockedRequest).toHaveBeenNthCalledWith(2, '/api/buyer/accounts/8/roles', expect.any(Object));
    expect(mockedRequest).toHaveBeenNthCalledWith(3, '/api/seller/depts/4', expect.any(Object));
    expect(mockedRequest).toHaveBeenNthCalledWith(4, '/api/buyer/depts/5', expect.any(Object));
    expect(mockedRequest).toHaveBeenNthCalledWith(5, '/api/buyer/depts/5', expect.any(Object));
    expect(mockedRequest).toHaveBeenNthCalledWith(6, '/api/seller/roles/3', expect.any(Object));
    expect(mockedRequest).toHaveBeenNthCalledWith(7, '/api/seller/roles/3/menus', expect.any(Object));
    expect(mockedRequest).toHaveBeenNthCalledWith(8, '/api/buyer/roles/6', expect.any(Object));
    expect(mockedRequest).toHaveBeenNthCalledWith(9, '/api/buyer/roles/6', expect.any(Object));
  });

  it.each([
    {
      name: 'invalid roleIds',
      call: () => assignPortalAccountRoles('seller', 8, [21, 0] as any),
      message: 'roleIds 必须使用有效正整数ID',
    },
    {
      name: 'duplicate roleIds',
      call: () => assignPortalAccountRoles('buyer', 8, [21, 21] as any),
      message: 'roleIds 不能包含重复ID',
    },
    {
      name: 'invalid menuIds',
      call: () =>
        createPortalRole('seller', {
          roleName: 'Ops',
          roleKey: 'ops',
          menuIds: [100001, null],
        } as any),
      message: 'menuIds 必须使用有效正整数ID',
    },
    {
      name: 'duplicate menuIds',
      call: () =>
        updatePortalRole('buyer', 6, {
          roleName: 'Audit',
          roleKey: 'audit',
          menuIds: [200001, 200001],
        } as any),
      message: 'menuIds 不能包含重复ID',
    },
  ])('rejects $name before the portal write request is sent', async ({ call, message }) => {
    await expect(call()).rejects.toThrow(message);
    expect(mockedRequest).not.toHaveBeenCalled();
  });

  it.each([
    {
      name: 'invalid account detail id',
      call: () => getPortalAccountRoles('seller', 0 as any),
      message: 'accountIdentifier must be a positive integer id',
    },
    {
      name: 'invalid account update id',
      call: () => updatePortalAccount('buyer', -1 as any, { nickName: 'bad' } as any),
      message: 'accountIdentifier must be a positive integer id',
    },
    {
      name: 'invalid account role assign id',
      call: () => assignPortalAccountRoles('buyer', 1.5 as any, [21]),
      message: 'accountIdentifier must be a positive integer id',
    },
    {
      name: 'invalid dept detail id',
      call: () => getPortalDept('seller', 'abc' as any),
      message: 'deptIdentifier must be a positive integer id',
    },
    {
      name: 'invalid dept update id',
      call: () => updatePortalDept('buyer', 0 as any, { deptName: 'bad' } as any),
      message: 'deptIdentifier must be a positive integer id',
    },
    {
      name: 'invalid dept delete id',
      call: () => deletePortalDept('buyer', -2 as any),
      message: 'deptIdentifier must be a positive integer id',
    },
    {
      name: 'invalid role detail id',
      call: () => getPortalRole('seller', 0 as any),
      message: 'roleIdentifier must be a positive integer id',
    },
    {
      name: 'invalid role menu id does not fall back to template',
      call: () => getPortalRoleMenus('buyer', 0 as any),
      message: 'roleIdentifier must be a positive integer id',
    },
    {
      name: 'invalid role update id',
      call: () => updatePortalRole('buyer', Number.NaN as any, { roleName: 'bad' } as any),
      message: 'roleIdentifier must be a positive integer id',
    },
    {
      name: 'invalid role delete id',
      call: () => deletePortalRole('seller', 1.25 as any),
      message: 'roleIdentifier must be a positive integer id',
    },
  ])('rejects $name before the portal request is sent', async ({ call, message }) => {
    await expect(call()).rejects.toThrow(message);
    expect(mockedRequest).not.toHaveBeenCalled();
  });

  it.each(portalAuthenticatedRequestCases)(
    'keeps $name on terminal token headers without admin token fallback',
    async (requestCase) => {
      await requestCase.call();

      const expectedOptions: Record<string, unknown> = {
        method: requestCase.method,
        headers: {
          Authorization: `Bearer ${requestCase.terminal}-token`,
          isToken: false,
        },
      };
      if (requestCase.params) {
        expectedOptions.params = requestCase.params;
      }
      if (requestCase.data) {
        expectedOptions.data = requestCase.data;
      }

      expect(mockedRequest).toHaveBeenLastCalledWith(requestCase.url, expectedOptions);
    },
  );
});
