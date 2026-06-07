import type { RequestOptions } from '@@/plugin-request/request';
import type { RequestConfig } from '@umijs/max';
import { getIntl, history } from '@umijs/max';
import { clearSessionToken, clearTerminalSessionToken } from '@/access';
import { PageEnum } from '@/enums/pagesEnums';
import { message, notification } from '@/utils/feedback';
import { getPortalLoginPath, isPortalRoute } from '@/utils/portalPaths';
import { getPortalTerminalFromApiUrl } from '@/utils/portalRequest';

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

function isUnauthorizedCode(code: unknown) {
  return Number(code) === 401;
}

function handleUnauthorized(requestUrl?: string) {
  const portalTerminal = getPortalTerminalFromApiUrl(requestUrl);
  if (portalTerminal) {
    clearTerminalSessionToken(portalTerminal);
    redirectToPortalLogin(portalTerminal);
    return;
  }
  clearSessionToken();
  redirectToLogin();
}

// 错误处理方案： 错误类型
enum ErrorShowType {
  SILENT = 0,
  WARN_MESSAGE = 1,
  ERROR_MESSAGE = 2,
  NOTIFICATION = 3,
  REDIRECT = 9,
}
// 与后端约定的响应数据格式
interface ResponseStructure {
  success: boolean;
  data: unknown;
  errorCode?: number;
  errorMessage?: string;
  showType?: ErrorShowType;
}

/**
 * @name 错误处理
 * pro 自带的错误处理， 可以在这里做自己的改动
 * @doc https://umijs.org/docs/max/request#配置
 */
export const errorConfig: RequestConfig = {
  // 错误处理： umi@3 的错误处理方案。
  errorConfig: {
    // 错误抛出
    errorThrower: (res) => {
      const { success, data, errorCode, errorMessage, showType } =
        res as unknown as ResponseStructure;
      if (!success) {
        const error: any = new Error(errorMessage);
        error.name = 'BizError';
        error.info = { errorCode, errorMessage, showType, data };
        throw error; // 抛出自制的错误
      }
    },
    // 错误接收及处理
    errorHandler: (error: any, opts: any) => {
      if (opts?.skipErrorHandler) throw error;
      const requestUrl = error?.config?.url || error?.response?.config?.url || opts?.url;
      // 我们的 errorThrower 抛出的错误。
      if (error.name === 'BizError') {
        const errorInfo: ResponseStructure | undefined = error.info;
        if (errorInfo) {
          const { errorMessage, errorCode } = errorInfo;
          if (isUnauthorizedCode(errorCode)) {
            handleUnauthorized(requestUrl);
            throw error;
          }
          switch (errorInfo.showType) {
            case ErrorShowType.SILENT:
              // do nothing
              break;
            case ErrorShowType.WARN_MESSAGE:
              message.warning(errorMessage);
              break;
            case ErrorShowType.ERROR_MESSAGE:
              message.error(errorMessage);
              break;
            case ErrorShowType.NOTIFICATION:
              notification.open({
                title: errorCode,
                description: errorMessage,
              });
              break;
            case ErrorShowType.REDIRECT:
              handleUnauthorized(requestUrl);
              break;
            default:
              message.error(errorMessage);
          }
        }
      } else if (error.response) {
        // Axios 的错误
        // 请求成功发出且服务器也响应了状态码，但状态代码超出了 2xx 的范围
        if (isUnauthorizedCode(error.response.status)) {
          handleUnauthorized(requestUrl);
          throw error;
        }
        message.error(`Response status:${error.response.status}`);
      } else if (typeof navigator !== 'undefined' && !navigator.onLine) {
        message.error(
          getIntl().formatMessage({
            id: 'app.request.offline',
            defaultMessage:
              'Network unavailable. Please check your connection and try again.',
          }),
        );
      } else if (error.request) {
        message.error('None response! Please retry.');
      } else {
        message.error('Request error, please retry.');
      }
    },
  },

  // 请求拦截器
  requestInterceptors: [
    (config: RequestOptions) => {
      return config;
    },
  ],

  // 响应拦截器
  responseInterceptors: [],
};
