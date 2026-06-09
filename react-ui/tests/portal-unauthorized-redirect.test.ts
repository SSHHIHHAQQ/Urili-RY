import { clearSessionToken, clearTerminalSessionToken, getAccessToken, getTokenExpireTime } from '@/access';
import { getInitialState, layout, onRouteChange, render as appRender, request } from '@/app';
import { errorConfig } from '@/requestErrorConfig';
import { getRoutersInfo, getUserInfo } from '@/services/session';
import { message } from '@/utils/feedback';
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
  getAccessToken: jest.fn(() => 'admin-token'),
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
const mockedGetAccessToken = getAccessToken as jest.Mock;
const mockedGetTokenExpireTime = getTokenExpireTime as jest.Mock;
const mockedMessage = message as unknown as { error: jest.Mock };
const mockedGetRoutersInfo = getRoutersInfo as jest.Mock;
const mockedGetUserInfo = getUserInfo as jest.Mock;

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
    mockedMessage.error.mockClear();
    mockedGetRoutersInfo.mockReset();
    mockedGetUserInfo.mockReset();
    mockedGetAccessToken.mockReturnValue('admin-token');
    mockedGetTokenExpireTime.mockReset();
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

  it('rejects direct-login BizError 401 without clearing existing portal tokens', () => {
    const error = new Error('直登票据已失效') as any;
    error.name = 'BizError';
    error.config = { url: '/api/seller/direct-login' };
    error.info = {
      errorCode: 401,
      errorMessage: '直登票据已失效',
    };

    expectErrorHandlerToThrow(error);

    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
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

  it('rejects direct-login 401 response bodies without clearing existing portal tokens', async () => {
    const response = {
      config: { url: '/api/buyer/direct-login' },
      data: { code: 401, msg: '直登票据已失效' },
    };
    mockedHistory.location = {
      pathname: '/buyer/direct-login',
      search: '?openerOrigin=http%3A%2F%2F127.0.0.1%3A8001',
      hash: '',
    };

    await expect(request.responseInterceptors[0](response)).rejects.toBe(response);

    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
  });

  it.each(['seller', 'buyer'])(
    'rejects %s direct-login HTTP 401 without clearing existing portal tokens',
    (terminal) => {
      mockedHistory.location = {
        pathname: `/${terminal}/direct-login`,
        search: '?openerOrigin=http%3A%2F%2F127.0.0.1%3A8001',
        hash: '',
      };
      const error = {
          response: {
            status: 401,
            statusText: 'Unauthorized',
            headers: {},
            data: {},
            config: { url: `/api/${terminal}/direct-login` },
          },
        } as any;

      expectErrorHandlerToThrow(error);

      expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
      expect(mockedClearSessionToken).not.toHaveBeenCalled();
      expect(mockedHistory.replace).not.toHaveBeenCalled();
    },
  );

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

  it('does not clear tokens or redirect for non-401 BizError REDIRECT responses', () => {
    const error = new Error('business redirect') as any;
    error.name = 'BizError';
    error.config = { url: '/api/seller/account/profile' };
    error.info = {
      errorCode: 500,
      errorMessage: 'business redirect',
      showType: 9,
    };

    expect(() => errorConfig.errorConfig?.errorHandler?.(error, {})).not.toThrow();

    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
    expect(mockedMessage.error).toHaveBeenCalledWith('business redirect');
  });

  it('keeps admin tokens when getInitialState user info fails with a non-401 error', async () => {
    mockedHistory.location = {
      pathname: '/system/user',
      search: '',
      hash: '',
    };
    mockedGetUserInfo.mockRejectedValue({
      response: {
        status: 500,
        data: { code: 500, msg: 'server error' },
        config: { url: '/api/getInfo' },
      },
    });

    const state = await getInitialState();

    expect(state.currentUser).toBeUndefined();
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
  });

  it('keeps admin tokens when route menu loading fails with a non-401 error', async () => {
    mockedHistory.location = {
      pathname: '/system/user',
      search: '',
      hash: '',
    };
    mockedGetRoutersInfo.mockRejectedValue({
      response: {
        status: 500,
        data: { code: 500, msg: 'menu failed' },
        config: { url: '/api/getRouters' },
      },
    });

    await onRouteChange({
      clientRoutes: [],
      location: mockedHistory.location,
    });

    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
  });

  it('keeps admin tokens when initial render menu loading fails with a non-401 error', async () => {
    mockedHistory.location = {
      pathname: '/system/user',
      search: '',
      hash: '',
    };
    mockedGetRoutersInfo.mockRejectedValue({
      response: {
        status: 500,
        data: { code: 500, msg: 'menu failed' },
        config: { url: '/api/getRouters' },
      },
    });
    const oldRender = jest.fn();

    appRender(oldRender);
    await new Promise<void>((resolve) => setTimeout(resolve, 0));

    expect(oldRender).toHaveBeenCalled();
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
  });

  it('does not redirect from layout page changes when admin token remains after a non-401 startup failure', () => {
    mockedHistory.location = {
      pathname: '/system/user',
      search: '',
      hash: '',
    };
    mockedGetAccessToken.mockReturnValue('admin-token');

    const runtimeLayout = layout({
      initialState: { currentUser: undefined },
      setInitialState: jest.fn(),
    } as any);
    runtimeLayout.onPageChange?.();

    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
  });

  it('does not clear admin tokens before a request when local expire time is missing or expired', () => {
    const interceptor = request.requestInterceptors[0];

    mockedGetTokenExpireTime.mockReturnValueOnce(undefined);
    const missingExpireOptions = { headers: {} as Record<string, string> };
    const missingExpireResult = interceptor('/api/getInfo', missingExpireOptions);

    expect(missingExpireResult.options.headers.Authorization).toBeUndefined();
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();

    mockedGetTokenExpireTime.mockReturnValueOnce(String(Date.now() - 1000));
    const expiredOptions = { headers: {} as Record<string, string> };
    const expiredResult = interceptor('/api/getRouters', expiredOptions);

    expect(expiredResult.options.headers.Authorization).toBeUndefined();
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
  });

  it('attaches admin Authorization only when local expire time is still valid', () => {
    mockedGetTokenExpireTime.mockReturnValue(String(Date.now() + 1000 * 60));

    const result = request.requestInterceptors[0]('/api/getInfo', {
      headers: {} as Record<string, string>,
    });

    expect(result.options.headers.Authorization).toBe('Bearer admin-token');
    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).not.toHaveBeenCalled();
  });

  it('redirects from layout page changes when both admin user and admin token are missing', () => {
    mockedHistory.location = {
      pathname: '/system/user',
      search: '',
      hash: '',
    };
    mockedGetAccessToken.mockReturnValue('');

    const runtimeLayout = layout({
      initialState: { currentUser: undefined },
      setInitialState: jest.fn(),
    } as any);
    runtimeLayout.onPageChange?.();

    expect(mockedClearSessionToken).not.toHaveBeenCalled();
    expect(mockedClearTerminalSessionToken).not.toHaveBeenCalled();
    expect(mockedHistory.replace).toHaveBeenCalledWith('/user/login?redirect=%2Fsystem%2Fuser');
  });
});
