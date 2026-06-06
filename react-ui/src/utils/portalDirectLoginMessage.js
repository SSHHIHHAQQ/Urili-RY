export const PORTAL_DIRECT_LOGIN_READY_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_READY';
export const PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_TOKEN';
function resolveTargetOrigin(loginUrl) {
    try {
        return new URL(loginUrl, window.location.href).origin;
    }
    catch {
        return window.location.origin;
    }
}
function isReadyMessage(data, terminal) {
    return Boolean(data
        && typeof data === 'object'
        && data.type === PORTAL_DIRECT_LOGIN_READY_MESSAGE
        && data.terminal === terminal);
}
export function openPortalDirectLoginWindow(result, terminal) {
    if (!result?.loginUrl || !result.token) {
        return false;
    }
    const popup = window.open(result.loginUrl, '_blank');
    if (!popup) {
        return false;
    }
    const targetOrigin = resolveTargetOrigin(result.loginUrl);
    const payload = {
        type: PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE,
        terminal,
        token: result.token,
        ticketId: result.ticketId,
    };
    let cleaned = false;
    let timeoutTimer;
    const cleanup = () => {
        if (cleaned) {
            return;
        }
        cleaned = true;
        window.removeEventListener('message', handleReadyMessage);
        if (timeoutTimer !== undefined) {
            window.clearTimeout(timeoutTimer);
        }
    };
    const postToken = () => {
        if (popup.closed) {
            cleanup();
            return;
        }
        popup.postMessage(payload, targetOrigin);
    };
    const handleReadyMessage = (event) => {
        if (event.source !== popup || event.origin !== targetOrigin || !isReadyMessage(event.data, terminal)) {
            return;
        }
        postToken();
        cleanup();
    };
    timeoutTimer = window.setTimeout(cleanup, 5000);
    window.addEventListener('message', handleReadyMessage);
    return true;
}
