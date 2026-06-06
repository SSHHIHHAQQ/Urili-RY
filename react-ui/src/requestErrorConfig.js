import { getIntl, history } from '@umijs/max';
import { clearSessionToken, clearTerminalSessionToken } from '@/access';
import { PageEnum } from '@/enums/pagesEnums';
import { message, notification } from '@/utils/feedback';
import { getPortalTerminalFromApiUrl } from '@/utils/portalRequest';
const PORTAL_ROUTE_PREFIXES = ['/seller/direct-login', '/buyer/direct-login', '/seller/portal', '/buyer/portal'];
function isPortalRoute(pathname) {
    return PORTAL_ROUTE_PREFIXES.some((prefix) => pathname === prefix || pathname?.startsWith(`${prefix}/`));
}
function redirectToLogin() {
    const { pathname, search, hash } = history.location;
    if (pathname === PageEnum.LOGIN || isPortalRoute(pathname)) {
        return;
    }
    const redirect = `${pathname}${search || ''}${hash || ''}`;
    history.replace(`${PageEnum.LOGIN}?redirect=${encodeURIComponent(redirect)}`);
}
function isUnauthorizedCode(code) {
    return Number(code) === 401;
}
function handleUnauthorized(requestUrl) {
    const portalTerminal = getPortalTerminalFromApiUrl(requestUrl);
    if (portalTerminal) {
        clearTerminalSessionToken(portalTerminal);
        return;
    }
    clearSessionToken();
    redirectToLogin();
}
// 错误处理方案： 错误类型
var ErrorShowType;
(function (ErrorShowType) {
    ErrorShowType[ErrorShowType["SILENT"] = 0] = "SILENT";
    ErrorShowType[ErrorShowType["WARN_MESSAGE"] = 1] = "WARN_MESSAGE";
    ErrorShowType[ErrorShowType["ERROR_MESSAGE"] = 2] = "ERROR_MESSAGE";
    ErrorShowType[ErrorShowType["NOTIFICATION"] = 3] = "NOTIFICATION";
    ErrorShowType[ErrorShowType["REDIRECT"] = 9] = "REDIRECT";
})(ErrorShowType || (ErrorShowType = {}));
/**
 * @name 错误处理
 * pro 自带的错误处理， 可以在这里做自己的改动
 * @doc https://umijs.org/docs/max/request#配置
 */
export const errorConfig = {
    // 错误处理： umi@3 的错误处理方案。
    errorConfig: {
        // 错误抛出
        errorThrower: (res) => {
            const { success, data, errorCode, errorMessage, showType } = res;
            if (!success) {
                const error = new Error(errorMessage);
                error.name = 'BizError';
                error.info = { errorCode, errorMessage, showType, data };
                throw error; // 抛出自制的错误
            }
        },
        // 错误接收及处理
        errorHandler: (error, opts) => {
            if (opts?.skipErrorHandler)
                throw error;
            const requestUrl = error?.config?.url || error?.response?.config?.url || opts?.url;
            // 我们的 errorThrower 抛出的错误。
            if (error.name === 'BizError') {
                const errorInfo = error.info;
                if (errorInfo) {
                    const { errorMessage, errorCode } = errorInfo;
                    if (isUnauthorizedCode(errorCode)) {
                        handleUnauthorized(requestUrl);
                        return;
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
            }
            else if (error.response) {
                // Axios 的错误
                // 请求成功发出且服务器也响应了状态码，但状态代码超出了 2xx 的范围
                if (isUnauthorizedCode(error.response.status)) {
                    handleUnauthorized(requestUrl);
                    return;
                }
                message.error(`Response status:${error.response.status}`);
            }
            else if (typeof navigator !== 'undefined' && !navigator.onLine) {
                message.error(getIntl().formatMessage({
                    id: 'app.request.offline',
                    defaultMessage: 'Network unavailable. Please check your connection and try again.',
                }));
            }
            else if (error.request) {
                message.error('None response! Please retry.');
            }
            else {
                message.error('Request error, please retry.');
            }
        },
    },
    // 请求拦截器
    requestInterceptors: [
        (config) => {
            // 拦截请求配置，进行个性化处理。
            const url = config?.url?.concat('?token=123');
            return { ...config, url };
        },
    ],
    // 响应拦截器
    responseInterceptors: [],
};
