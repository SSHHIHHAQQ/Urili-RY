import { LinkOutlined } from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import { SettingDrawer } from '@ant-design/pro-components';
import type { RunTimeLayoutConfig } from '@umijs/max';
import { history, Link, useModel } from '@umijs/max';
import { App as AntdApp, ConfigProvider } from 'antd';
import React from 'react';
import type { ReactNode } from 'react';
import { AvatarDropdown, AvatarName, Question, SelectLang } from '@/components';
import defaultSettings from '../config/defaultSettings';
import { clearSessionToken, clearTerminalSessionToken, getAccessToken, getTokenExpireTime } from './access';
import { PageEnum } from './enums/pagesEnums';
import { errorConfig } from './requestErrorConfig';
import { getRemoteMenu, getRoutersInfo, getUserInfo, patchRouteWithRemoteMenus, setRemoteMenu } from './services/session';
import { AntdFeedbackProvider } from './utils/feedback';
import { selectInitialStateModel, type InitialStateData } from './utils/initialStateModel';
import { getPortalLoginPath, isPortalRoute } from './utils/portalPaths';
import { getPortalTerminalFromApiUrl, isPortalDirectLoginApiUrl } from './utils/portalRequest';
import './global.css';


const isDev = process.env.NODE_ENV === 'development';

function redirectToLogin(includePortal = false) {
  const { pathname, search, hash } = history.location;
  if (pathname === PageEnum.LOGIN || (!includePortal && isPortalRoute(pathname))) {
    return;
  }
  const redirect = `${pathname}${search || ''}${hash || ''}`;
  history.replace(`${PageEnum.LOGIN}?redirect=${encodeURIComponent(redirect)}`);
}

function redirectToPortalLogin(portalTerminal: NonNullable<ReturnType<typeof getPortalTerminalFromApiUrl>>) {
  const { pathname, search, hash } = history.location;
  const loginPath = getPortalLoginPath(portalTerminal);
  if (pathname === loginPath) {
    return;
  }
  const redirect = `${pathname}${search || ''}${hash || ''}`;
  history.replace(`${loginPath}?redirect=${encodeURIComponent(redirect)}`);
}

function clearAdminSession() {
  clearSessionToken();
  setRemoteMenu(null);
}

function getResponseCode(data: unknown) {
  if (!data || typeof data !== 'object') {
    return undefined;
  }
  const responseData = data as { code?: number | string; errorCode?: number | string };
  return responseData.code ?? responseData.errorCode;
}

function isUnauthorizedCode(code: unknown) {
  return Number(code) === 401;
}

function getErrorRequestUrl(error: any) {
  return error?.config?.url || error?.response?.config?.url;
}

function isUnauthorizedError(error: any) {
  return (
    isUnauthorizedCode(error?.response?.status) ||
    isUnauthorizedCode(getResponseCode(error?.response?.data)) ||
    isUnauthorizedCode(error?.info?.errorCode) ||
    isUnauthorizedCode(error?.code)
  );
}

function handleUnauthorizedResponse(requestUrl?: string) {
  const portalTerminal = getPortalTerminalFromApiUrl(requestUrl);
  if (portalTerminal) {
    if (isPortalDirectLoginApiUrl(requestUrl)) {
      return;
    }
    clearTerminalSessionToken(portalTerminal);
    redirectToPortalLogin(portalTerminal);
    return;
  }
  clearAdminSession();
  redirectToLogin();
}

function handleUnauthorizedError(error: any) {
  if (!isUnauthorizedError(error)) {
    return false;
  }
  handleUnauthorizedResponse(getErrorRequestUrl(error));
  return true;
}

/**
 * @see  https://umijs.org/zh-CN/plugins/plugin-initial-state
 * */
export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  currentUser?: API.CurrentUser;
  loading?: boolean;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
}> {
  const fetchUserInfo = async () => {
    try {
      const response = await getUserInfo({
        skipErrorHandler: true,
      });
      if (!response?.user) {
        if (isUnauthorizedCode(getResponseCode(response))) {
          handleUnauthorizedResponse('/api/getInfo');
        }
        return undefined;
      }
      const avatar =
        response.user.avatar || 'https://gw.alipayobjects.com/zos/rmsportal/BiazfanxmamNRoxxVxka.png';
      return {
        ...response.user,
        avatar,
        permissions: response.permissions,
        roles: response.roles,
      } as API.CurrentUser;
    } catch (error) {
      console.log(error);
      handleUnauthorizedError(error);
    }
    return undefined;
  };
  // 如果不是登录页面，执行
  const { location } = history;
  if (location.pathname !== PageEnum.LOGIN && !isPortalRoute(location.pathname)) {
    const currentUser = await fetchUserInfo();
    return {
      fetchUserInfo,
      currentUser,
      settings: defaultSettings as Partial<LayoutSettings>,
    };
  }
  return {
    fetchUserInfo,
    settings: defaultSettings as Partial<LayoutSettings>,
  };
}

// ProLayout 支持的api https://procomponents.ant.design/components/layout
type LayoutRuntimeContext = {
  initialState?: InitialStateData;
};

function RuntimeSettingDrawer({ settings }: { settings?: Partial<LayoutSettings> }) {
  const { initialState, setInitialState } = useModel('@@initialState', selectInitialStateModel);
  const [fallbackSettings, setFallbackSettings] = React.useState(settings);
  const drawerSettings = initialState?.settings ?? fallbackSettings ?? settings;

  return (
    <SettingDrawer
      disableUrlParams
      enableDarkTheme
      settings={drawerSettings}
      onSettingChange={(nextSettings) => {
        if (typeof setInitialState !== 'function') {
          setFallbackSettings(nextSettings);
          return;
        }
        setInitialState((preInitialState) => ({
          ...(preInitialState ?? {}),
          settings: nextSettings,
        }));
      }}
    />
  );
}

export const layout: RunTimeLayoutConfig = (runtimeContext) => {
  const { initialState } = runtimeContext as unknown as LayoutRuntimeContext;
  return {
    actionsRender: () => [<Question key="doc" />, <SelectLang key="SelectLang" />],
    avatarProps: {
      src: initialState?.currentUser?.avatar || undefined,
      title: <AvatarName />,
      render: (_, avatarChildren) => {
        return <AvatarDropdown menu={true}>{avatarChildren}</AvatarDropdown>;
      },
    },
    waterMarkProps: {
      // content: initialState?.currentUser?.nickName,
    },
    menu: {
      locale: false,
      // 每当 initialState?.currentUser?.userid 发生修改时重新执行 request
      params: {
        userId: initialState?.currentUser?.userId,
      },
      request: async () => {
        if (!initialState?.currentUser?.userId) {
          return [];
        }
        return getRemoteMenu();
      },
    },
    footerRender: false,
    onPageChange: () => {
      const { location } = history;
      const adminToken = getAccessToken();
      // 如果没有登录，重定向到 login
      if (
        !initialState?.currentUser
        && (!adminToken || adminToken.length === 0)
        && location.pathname !== PageEnum.LOGIN
        && !isPortalRoute(location.pathname)
      ) {
        redirectToLogin();
      }
    },
    layoutBgImgList: [
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/D2LWSqNny4sAAAAAAAAAAAAAFl94AQBr',
        left: 85,
        bottom: 100,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/C2TWRpJpiC0AAAAAAAAAAAAAFl94AQBr',
        bottom: -68,
        right: -45,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/F6vSTbj8KpYAAAAAAAAAAAAAFl94AQBr',
        bottom: 0,
        left: 0,
        width: '331px',
      },
    ],
    links: isDev
      ? [
        <Link key="openapi" to="/umi/plugin/openapi" target="_blank">
          <LinkOutlined />
          <span>OpenAPI 文档</span>
        </Link>,
      ]
      : [],
    menuHeaderRender: undefined,
    // 自定义 403 页面
    // unAccessible: <div>unAccessible</div>,
    // 增加一个 loading 的状态
    childrenRender: (children) => {
      // if (initialState?.loading) return <PageLoading />;
      return (
        <>
          {children}
          <RuntimeSettingDrawer settings={initialState?.settings} />
        </>
      );
    },
    ...initialState?.settings,
  };
};

export function rootContainer(container: ReactNode) {
  return (
    <ConfigProvider select={{ showSearch: true }} variant="outlined">
      <AntdApp>
        <AntdFeedbackProvider>{container}</AntdFeedbackProvider>
      </AntdApp>
    </ConfigProvider>
  );
}

export async function onRouteChange({ location }: { clientRoutes: any; location: any }) {
  if (isPortalRoute(location.pathname)) {
    return;
  }
  if (location.pathname !== PageEnum.LOGIN && !getAccessToken()) {
    redirectToLogin();
    return;
  }
  const menus = getRemoteMenu();
  if (menus !== null || location.pathname === PageEnum.LOGIN) {
    return;
  }

  try {
    const routers = await getRoutersInfo();
    setRemoteMenu(routers);
  } catch (error) {
    console.log(error);
    handleUnauthorizedError(error);
  }
}

// export function patchRoutes({ routes, routeComponents }) {
//   console.log('patchRoutes', routes, routeComponents);
// }


export async function patchClientRoutes({ routes }: { routes: any }) {
  // console.log('patchClientRoutes', routes);
  patchRouteWithRemoteMenus(routes);
}

export function render(oldRender: () => void) {
  // console.log('render get routers', oldRender)
  if (isPortalRoute(history.location.pathname)) {
    oldRender();
    return;
  }
  const token = getAccessToken();
  if(!token || token?.length === 0) {
    clearAdminSession();
    redirectToLogin();
    oldRender();
    return;
  }
  getRoutersInfo().then(res => {
    setRemoteMenu(res);
  }).catch(error => {
    console.log(error);
    handleUnauthorizedError(error);
  }).finally(() => {
    oldRender();
  });
}

/**
 * @name request 配置，可以配置错误处理
 * 它基于 axios 和 ahooks 的 useRequest 提供了一套统一的网络请求和错误处理方案。
 * @doc https://umijs.org/docs/max/request#配置
 */
export const request: any = {
  ...errorConfig,
  requestInterceptors: [
    (url: any, options: { headers: Record<string, any> }) => {
      const headers = options.headers ?? {};
      options.headers = headers;
      console.log('request ====>:', url);
      const authHeader = headers.Authorization;
      const isToken = headers.isToken;
      delete headers.isToken;
      if (!authHeader && isToken !== false) {
        const expireTime = getTokenExpireTime();
        const left = expireTime ? Number(expireTime) - Date.now() : -1;
        if (left >= 0) {
          const accessToken = getAccessToken();
          if (accessToken) {
            headers.Authorization = `Bearer ${accessToken}`;
          }
        }
      }
      return { url, options };
    },
  ],
  responseInterceptors: [
    (response: any) => {
      if (isUnauthorizedCode(getResponseCode(response?.data))) {
        handleUnauthorizedResponse(response?.config?.url);
        return Promise.reject(response);
      }
      return response;
    },
    // (response) =>
    // {
    //   // // 不再需要异步处理读取返回体内容，可直接在data中读出，部分字段可在 config 中找到
    //   // const { data = {} as any, config } = response;
    //   // // do something
    //   // console.log('data: ', data)
    //   // console.log('config: ', config)
    //   return response
    // },
  ],
};
