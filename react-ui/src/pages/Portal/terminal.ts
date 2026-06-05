import { clearTerminalSessionToken, setTerminalSessionToken } from '@/access';
import {
  buyerPortalSessionService,
  sellerPortalSessionService,
} from '@/services/portal/session';

export type PortalTerminal = API.Partner.PortalTerminal;

export type PortalService = typeof sellerPortalSessionService;

export const PORTAL_META: Record<PortalTerminal, { label: string; homePath: string }> = {
  seller: {
    label: '卖家端',
    homePath: '/seller/portal',
  },
  buyer: {
    label: '买家端',
    homePath: '/buyer/portal',
  },
};

export const PORTAL_SERVICE: Record<PortalTerminal, PortalService> = {
  seller: sellerPortalSessionService,
  buyer: buyerPortalSessionService,
};

export function getPortalTerminal(pathname: string): PortalTerminal | undefined {
  if (pathname.startsWith('/seller/')) {
    return 'seller';
  }
  if (pathname.startsWith('/buyer/')) {
    return 'buyer';
  }
  return undefined;
}

export function persistPortalLogin(
  result: API.Partner.PortalLoginResultData | undefined,
  expectedTerminal: PortalTerminal,
) {
  if (!result?.token || result.terminal !== expectedTerminal) {
    clearPortalLogin(expectedTerminal);
    if (result?.terminal) {
      clearPortalLogin(result.terminal);
    }
    return false;
  }
  const terminal = expectedTerminal;
  const expireTime = Date.now() + (result.expireMinutes || 30) * 60 * 1000;
  setTerminalSessionToken(terminal, result.token, result.token, expireTime);
  return true;
}

export function clearPortalLogin(terminal: PortalTerminal) {
  clearTerminalSessionToken(terminal);
}
