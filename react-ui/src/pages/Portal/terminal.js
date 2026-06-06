import { clearTerminalSessionToken, setTerminalSessionToken } from '@/access';
import { buyerPortalSessionService, sellerPortalSessionService } from '@/services/portal/session';
import { getPortalLoginPath, getPortalTerminalFromPath } from '@/utils/portalPaths';

export const PORTAL_META = {
    seller: {
        label: 'Seller portal',
        homePath: '/seller/portal',
        loginPath: getPortalLoginPath('seller'),
    },
    buyer: {
        label: 'Buyer portal',
        homePath: '/buyer/portal',
        loginPath: getPortalLoginPath('buyer'),
    },
};

export const PORTAL_SERVICE = {
    seller: sellerPortalSessionService,
    buyer: buyerPortalSessionService,
};

export function getPortalTerminal(pathname) {
    return getPortalTerminalFromPath(pathname);
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
    setTerminalSessionToken(terminal, result.token, undefined, expireTime);
    return true;
}

export function clearPortalLogin(terminal) {
    clearTerminalSessionToken(terminal);
}
