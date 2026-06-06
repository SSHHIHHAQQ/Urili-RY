import { getTerminalAccessToken } from '@/access';
import {
  getBuyerPortalDistributionProducts,
  getPortalLoginLogs,
  getPortalOperLogs,
  getPortalSessions,
  getSellerPortalDistributionProducts,
} from '@/services/portal/session';
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
    expect(getPortalTerminalFromApiUrl('/api/system/user/list')).toBeUndefined();
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
    await getPortalSessions('buyer', { pageNum: 3, accountId: 99, ipaddr: '127.0.0.1' });
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
      params: { pageNum: 3, ipaddr: '127.0.0.1' },
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
});
