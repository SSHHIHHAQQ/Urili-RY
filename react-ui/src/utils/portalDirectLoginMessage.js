export const PORTAL_DIRECT_LOGIN_READY_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_READY';
export const PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_TOKEN';
export const PORTAL_DIRECT_LOGIN_RESULT_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_RESULT';
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
function isResultMessage(data, terminal, ticketId) {
    if (!(data
        && typeof data === 'object'
        && data.type === PORTAL_DIRECT_LOGIN_RESULT_MESSAGE
        && data.terminal === terminal
        && (data.status === 'success' || data.status === 'error'))) {
        return false;
    }
    return ticketId == null || data.ticketId === ticketId;
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
    let tokenPosted = false;
    let resolveBridge = () => undefined;
    let rejectBridge = () => undefined;
    const cleanup = () => {
        if (cleaned) {
            return;
        }
        cleaned = true;
        window.removeEventListener('message', handleBridgeMessage);
        if (timeoutTimer !== undefined) {
            window.clearTimeout(timeoutTimer);
        }
    };
    const postToken = () => {
        if (popup.closed) {
            cleanup();
            rejectBridge(new Error('DIRECT_LOGIN_POPUP_CLOSED'));
            return;
        }
        tokenPosted = true;
        popup.postMessage(payload, targetOrigin);
    };
    const handleBridgeMessage = (event) => {
        if (event.source !== popup || event.origin !== targetOrigin) {
            return;
        }
        if (isReadyMessage(event.data, terminal) && !tokenPosted) {
            postToken();
            return;
        }
        if (!tokenPosted || !isResultMessage(event.data, terminal, result.ticketId)) {
            return;
        }
        cleanup();
        if (event.data.status === 'success') {
            resolveBridge({ status: 'success', ticketId: event.data.ticketId });
            return;
        }
        rejectBridge(new Error(event.data.message || 'DIRECT_LOGIN_CONSUME_FAILED'));
    };
    const bridge = new Promise((resolve, reject) => {
        resolveBridge = resolve;
        rejectBridge = reject;
    });
    timeoutTimer = window.setTimeout(() => {
        cleanup();
        rejectBridge(new Error(tokenPosted ? 'DIRECT_LOGIN_CONSUME_TIMEOUT' : 'DIRECT_LOGIN_READY_TIMEOUT'));
    }, 15000);
    window.addEventListener('message', handleBridgeMessage);
    return bridge;
}
