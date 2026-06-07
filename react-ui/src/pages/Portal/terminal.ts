import { clearTerminalSessionToken, setTerminalSessionToken } from '@/access';
import {
  buyerPortalSessionService,
  sellerPortalSessionService,
} from '@/services/portal/session';
import {
  getPortalLoginPath,
  getPortalTerminalFromPath,
  type PortalTerminal,
} from '@/utils/portalPaths';

export type { PortalTerminal };

export type PortalService = typeof sellerPortalSessionService;

export const PORTAL_META: Record<PortalTerminal, { label: string; homePath: string; loginPath: string }> = {
  seller: {
    label: '卖家端',
    homePath: '/seller/portal',
    loginPath: getPortalLoginPath('seller'),
  },
  buyer: {
    label: '买家端',
    homePath: '/buyer/portal',
    loginPath: getPortalLoginPath('buyer'),
  },
};

export const PORTAL_SERVICE: Record<PortalTerminal, PortalService> = {
  seller: sellerPortalSessionService,
  buyer: buyerPortalSessionService,
};

export function getPortalTerminal(pathname: string): PortalTerminal | undefined {
  return getPortalTerminalFromPath(pathname);
}

export function persistPortalLogin(
  result: API.Partner.PortalLoginResultData | undefined,
  expectedTerminal: PortalTerminal,
) {
  if (!result?.token || result.terminal !== expectedTerminal) {
    clearPortalLogin(expectedTerminal);
    return false;
  }
  const terminal = expectedTerminal;
  const expireTime = Date.now() + (result.expireMinutes || 30) * 60 * 1000;
  setTerminalSessionToken(terminal, result.token, undefined, expireTime);
  return true;
}

export function clearPortalLogin(terminal: PortalTerminal) {
  clearTerminalSessionToken(terminal);
}
