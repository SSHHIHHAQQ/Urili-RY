import { checkRole, matchPermission } from './utils/permission';
import { getRemoteMenuStorageKey, REMOTE_MENU_SCOPES } from './utils/remoteMenuStorage';
/**
 * @see https://umijs.org/zh-CN/plugins/plugin-access
 * */
export default function access(initialState: { currentUser?: API.CurrentUser } | undefined) {
  const { currentUser } = initialState ?? {};
  const hasPerms = (perm: string) => {
    return matchPermission(initialState?.currentUser?.permissions, perm);
  };
  const roleFiler = (route: { authority: string[] }) => {
    return checkRole(initialState?.currentUser?.roles, route.authority);
  };
  return {
    canAdmin: currentUser && currentUser.access === 'admin',
    hasPerms,
    roleFiler,
  };
}

export type SessionTerminal = 'admin' | 'seller' | 'buyer';

type SessionTokenKeys = {
  accessToken: string;
  refreshToken: string;
  expireTime: string;
  user: string;
};

const SESSION_TOKEN_KEYS: Record<SessionTerminal, SessionTokenKeys> = {
  admin: {
    accessToken: 'access_token',
    refreshToken: 'refresh_token',
    expireTime: 'expireTime',
    user: 'user',
  },
  seller: {
    accessToken: 'seller_access_token',
    refreshToken: 'seller_refresh_token',
    expireTime: 'seller_expireTime',
    user: 'seller_user',
  },
  buyer: {
    accessToken: 'buyer_access_token',
    refreshToken: 'buyer_refresh_token',
    expireTime: 'buyer_expireTime',
    user: 'buyer_user',
  },
};

export function getTerminalSessionTokenKeys(terminal: SessionTerminal) {
  return SESSION_TOKEN_KEYS[terminal];
}

export function setTerminalSessionToken(
  terminal: SessionTerminal,
  access_token: string | undefined,
  refresh_token: string | undefined,
  expireTime: number,
): void {
  const keys = getTerminalSessionTokenKeys(terminal);
  if (access_token) {
    localStorage.setItem(keys.accessToken, access_token);
  } else {
    localStorage.removeItem(keys.accessToken);
  }
  if (refresh_token) {
    localStorage.setItem(keys.refreshToken, refresh_token);
  } else {
    localStorage.removeItem(keys.refreshToken);
  }
  localStorage.setItem(keys.expireTime, `${expireTime}`);
}

export function getTerminalAccessToken(terminal: SessionTerminal) {
  return localStorage.getItem(getTerminalSessionTokenKeys(terminal).accessToken);
}

export function getTerminalRefreshToken(terminal: SessionTerminal) {
  return localStorage.getItem(getTerminalSessionTokenKeys(terminal).refreshToken);
}

export function getTerminalTokenExpireTime(terminal: SessionTerminal) {
  return localStorage.getItem(getTerminalSessionTokenKeys(terminal).expireTime);
}

export function clearTerminalSessionToken(terminal: SessionTerminal) {
  const keys = getTerminalSessionTokenKeys(terminal);
  sessionStorage.removeItem(keys.user);
  localStorage.removeItem(keys.accessToken);
  localStorage.removeItem(keys.refreshToken);
  localStorage.removeItem(keys.expireTime);
}

export function setSessionToken(
  access_token: string | undefined,
  refresh_token: string | undefined,
  expireTime: number,
): void {
  setTerminalSessionToken('admin', access_token, refresh_token, expireTime);
}

export function getAccessToken() {
  return getTerminalAccessToken('admin');
}

export function getRefreshToken() {
  return getTerminalRefreshToken('admin');
}

export function getTokenExpireTime() {
  return getTerminalTokenExpireTime('admin');
}

export function clearSessionToken() {
  clearTerminalSessionToken('admin');
  if (typeof sessionStorage !== 'undefined') {
    REMOTE_MENU_SCOPES.forEach((scope) => sessionStorage.removeItem(getRemoteMenuStorageKey(scope)));
  }
}
