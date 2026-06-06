export const PORTAL_DIRECT_LOGIN_READY_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_READY';
export const PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_TOKEN';

export type PortalDirectLoginReadyMessage = {
  type: typeof PORTAL_DIRECT_LOGIN_READY_MESSAGE;
  terminal: API.Partner.PortalTerminal;
};

export type PortalDirectLoginTokenMessage = {
  type: typeof PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE;
  terminal: API.Partner.PortalTerminal;
  token: string;
  ticketId?: number;
};

function resolveTargetOrigin(loginUrl: string) {
  try {
    return new URL(loginUrl, window.location.href).origin;
  } catch {
    return window.location.origin;
  }
}

function isReadyMessage(data: unknown, terminal: API.Partner.PortalTerminal) {
  return Boolean(
    data
      && typeof data === 'object'
      && (data as PortalDirectLoginReadyMessage).type === PORTAL_DIRECT_LOGIN_READY_MESSAGE
      && (data as PortalDirectLoginReadyMessage).terminal === terminal,
  );
}

export function openPortalDirectLoginWindow(
  result: API.Partner.DirectLoginResult | undefined,
  terminal: API.Partner.PortalTerminal,
) {
  if (!result?.loginUrl || !result.token) {
    return false;
  }

  const popup = window.open(result.loginUrl, '_blank');
  if (!popup) {
    return false;
  }

  const targetOrigin = resolveTargetOrigin(result.loginUrl);
  const payload: PortalDirectLoginTokenMessage = {
    type: PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE,
    terminal,
    token: result.token,
    ticketId: result.ticketId,
  };
  let cleaned = false;
  let timeoutTimer: number | undefined;

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

  const handleReadyMessage = (event: MessageEvent) => {
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
