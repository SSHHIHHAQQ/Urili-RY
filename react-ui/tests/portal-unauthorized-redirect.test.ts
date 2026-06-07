import { clearSessionToken, clearTerminalSessionToken } from '@/access';
import { request } from '@/app';
import { errorConfig } from '@/requestErrorConfig';
import { history } from '@umijs/max';

jest.mock('@umijs/max', () => ({
  getIntl: () => ({
    formatMessage: ({ defaultMessage }: { defaultMessage: string }) => defaultMessage,
  }),
  history: {
    location: {
      pathname: '/',
      search: '',
      hash: '',
    },
    replace: jest.fn(),
  },
}));

jest.mock('@/access', () => ({
  clearSessionToken: jest.fn(),
  clearTerminalSessionToken: jest.fn(),
  getAccessToken: jest.fn(),
  getTokenExpireTime: jest.fn(),
}));

jest.mock('@/services/session', () => ({
  getRemoteMenu: jest.fn(() => null),
  getRoutersInfo: jest.fn(),
  getUserInfo: jest.fn(),
  patchRouteWithRemoteMenus: jest.fn(),
  setRemoteMenu: jest.fn(),
}));

jest.mock('@/utils/feedback', () => ({
  message: {
    error: jest.fn(),
    warning: jest.fn(),
  },
  notification: {
    open: jest.fn(),
  },
  AntdFeedbackProvider: ({ children }: { children: unknown }) => children,
}));

const mockedHistory = history as unknown as {
  location: { pathname: string; search: string; hash: string };
  replace: jest.Mock;
};

const mockedClearSessionToken = clearSessionToken as jest.Mock;
const mockedClearTerminalSessionToken = clearTerminalSessionToken as jest.Mock;

function expectErrorHandlerToThrow(error: any, opts: Record<string, unknown> = {}) {
  let thrown: unknown;
  try {
    errorConfig.errorConfig?.errorHandler?.(error, opts);
  } catch (caught) {
    thrown = caught;
  }

  expect(thrown).toBe(error);
}

describe('portal unauthorized redirect', () => {
  beforeEach(() => {
    mockedHistory.location = {
      pathname: '/seller/portal/orders',
      search: '?status=pending',
      hash: '#row-7',
    };
    mockedHistory.replace.mockClear();
    mockedClearSessionToken.mockClear();
    mockedClearTerminalSessionToken.mockClear();
  });

  it('keeps portal 401 scoped to the matched terminal and preserves redirect', () => {
    const error = {
        response: {
          status: 401,
          statusText: 'Unauthorized',
          headers: {},
          data: {},
          config: { url: '/api/seller/account/sessions' },
        },
      } as any;

    expectErrorHandlerToThrow(error);

    expect(mockedClearTerminalSessionToken).toHaveBeenCalledWith('seller');
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).toHaveBeenCalledWith(
      '/seller/login?redirect=%2Fseller%2Fportal%2Forders%3Fstatus%3Dpending%23row-7',
    );
  });

  it('rejects portal BizError 401 after redirecting to the matched terminal login', () => {
    const error = new Error('登录状态已过期') as any;
    error.name = 'BizError';
    error.config = { url: '/api/seller/account/profile' };
    error.info = {
      errorCode: 401,
      errorMessage: '登录状态已过期',
    };

    expectErrorHandlerToThrow(error);

    expect(mockedClearTerminalSessionToken).toHaveBeenCalledWith('seller');
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).toHaveBeenCalledWith(
      '/seller/login?redirect=%2Fseller%2Fportal%2Forders%3Fstatus%3Dpending%23row-7',
    );
  });

  it('rejects 401 response bodies after redirecting to the matched portal login', async () => {
    const response = {
      config: { url: '/api/buyer/account/login-logs' },
      data: { code: 401, msg: '登录状态已过期' },
    };
    mockedHistory.location = {
      pathname: '/buyer/portal/account/logs',
      search: '',
      hash: '',
    };

    await expect(request.responseInterceptors[0](response)).rejects.toBe(response);

    expect(mockedClearTerminalSessionToken).toHaveBeenCalledWith('buyer');
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).toHaveBeenCalledWith(
      '/buyer/login?redirect=%2Fbuyer%2Fportal%2Faccount%2Flogs',
    );
  });

  it('keeps admin 401 on the admin login flow', () => {
    mockedHistory.location = {
      pathname: '/system/user',
      search: '?page=1',
      hash: '',
    };

    const error = {
        response: {
          status: 401,
          statusText: 'Unauthorized',
          headers: {},
          data: {},
          config: { url: '/api/system/user/list' },
        },
      } as any;

    expectErrorHandlerToThrow(error);

    expect(mockedClearSessionToken).toHaveBeenCalled();
    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).toHaveBeenCalledWith(
      '/user/login?redirect=%2Fsystem%2Fuser%3Fpage%3D1',
    );
  });

  it.each([
    ['/api/seller/admin/menus/list', '/seller'],
    ['/api/buyer/admin/menus/list', '/buyer'],
  ])('keeps %s 401 on the admin login flow', (url, pathname) => {
    mockedHistory.location = {
      pathname,
      search: '?tab=menu',
      hash: '',
    };

    const error = {
        response: {
          status: 401,
          statusText: 'Unauthorized',
          headers: {},
          data: {},
          config: { url },
        },
      } as any;

    expectErrorHandlerToThrow(error);

    expect(mockedClearSessionToken).toHaveBeenCalled();
    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).toHaveBeenCalledWith(
      `/user/login?redirect=${encodeURIComponent(`${pathname}?tab=menu`)}`,
    );
  });

  it('rejects admin-prefixed portal 401 response bodies on the admin login flow', async () => {
    const response = {
      config: { url: '/api/seller/admin/sellers/list' },
      data: { code: 401, msg: '登录状态已过期' },
    };
    mockedHistory.location = {
      pathname: '/seller',
      search: '?page=1',
      hash: '',
    };

    await expect(request.responseInterceptors[0](response)).rejects.toBe(response);

    expect(mockedClearSessionToken).toHaveBeenCalled();
    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).toHaveBeenCalledWith(
      '/user/login?redirect=%2Fseller%3Fpage%3D1',
    );
  });
});
