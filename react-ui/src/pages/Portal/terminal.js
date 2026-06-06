import { clearTerminalSessionToken, setTerminalSessionToken } from '@/access';
import { buyerPortalSessionService, sellerPortalSessionService, } from '@/services/portal/session';
export const PORTAL_META = {
    seller: {
        label: '卖家端',
        homePath: '/seller/portal',
    },
    buyer: {
        label: '买家端',
        homePath: '/buyer/portal',
    },
};
export const PORTAL_SERVICE = {
    seller: sellerPortalSessionService,
    buyer: buyerPortalSessionService,
};
export function getPortalTerminal(pathname) {
    if (pathname.startsWith('/seller/')) {
        return 'seller';
    }
    if (pathname.startsWith('/buyer/')) {
        return 'buyer';
    }
    return undefined;
}
export function persistPortalLogin(result, expectedTerminal) {
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
export function clearPortalLogin(terminal) {
    clearTerminalSessionToken(terminal);
}
