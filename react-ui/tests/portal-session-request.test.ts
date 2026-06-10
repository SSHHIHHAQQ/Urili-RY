import { getTerminalAccessToken } from '@/access';
import {
  getBuyerPortalDistributionProduct,
  getBuyerPortalDistributionProducts,
  getBuyerPortalDistributionProductSkus,
  getBuyerPortalProductCategories,
  getBuyerPortalProductSchema,
  getPortalAccountProfile,
  getPortalAccounts,
  getPortalDepts,
  getPortalInfo,
  getPortalLoginLogs,
  getPortalOperLogs,
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
  updatePortalPassword,
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
    call: () => updatePortalPassword('buyer', portalPasswordChangeData),
  },
  {
    name: 'seller accounts',
    terminal: 'seller',
    url: '/api/seller/accounts',
    method: 'GET',
    call: () => getPortalAccounts('seller'),
  },
  {
    name: 'buyer departments',
    terminal: 'buyer',
    url: '/api/buyer/depts',
    method: 'GET',
    call: () => getPortalDepts('buyer'),
  },
  {
    name: 'seller roles',
    terminal: 'seller',
    url: '/api/seller/roles',
    method: 'GET',
    call: () => getPortalRoles('seller'),
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
