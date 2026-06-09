export const PORTAL_DIRECT_LOGIN_READY_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_READY';
export const PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_TOKEN';
export const PORTAL_DIRECT_LOGIN_RESULT_MESSAGE = 'URILI_PORTAL_DIRECT_LOGIN_RESULT';
export const PORTAL_DIRECT_LOGIN_OPENER_ORIGIN_PARAM = 'openerOrigin';

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

export type PortalDirectLoginResultMessage = {
  type: typeof PORTAL_DIRECT_LOGIN_RESULT_MESSAGE;
  terminal: API.Partner.PortalTerminal;
  status: 'success' | 'error';
  ticketId?: number;
  message?: string;
};

export type PortalDirectLoginBridgeResult = {
  status: 'success';
  ticketId?: number;
};

function resolveTargetOrigin(loginUrl: string) {
  try {
    return new URL(loginUrl, window.location.href).origin;
  } catch {
    return window.location.origin;
  }
}

function normalizeHttpOrigin(value: string | null | undefined) {
  if (!value) {
    return null;
  }
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.origin : null;
  } catch {
    return null;
  }
}

export function buildPortalDirectLoginWindowUrl(loginUrl: string) {
  try {
    const url = new URL(loginUrl, window.location.href);
    url.searchParams.set(PORTAL_DIRECT_LOGIN_OPENER_ORIGIN_PARAM, window.location.origin);
    return url.toString();
  } catch {
    return loginUrl;
  }
}

export function resolvePortalDirectLoginOpenerOrigin(search = window.location.search) {
  const explicitOrigin = normalizeHttpOrigin(
    new URLSearchParams(search).get(PORTAL_DIRECT_LOGIN_OPENER_ORIGIN_PARAM),
  );
  if (explicitOrigin) {
    return explicitOrigin;
  }
  return normalizeHttpOrigin(document.referrer) || window.location.origin;
}

function isReadyMessage(data: unknown, terminal: API.Partner.PortalTerminal) {
  return Boolean(
    data
      && typeof data === 'object'
      && (data as PortalDirectLoginReadyMessage).type === PORTAL_DIRECT_LOGIN_READY_MESSAGE
      && (data as PortalDirectLoginReadyMessage).terminal === terminal,
  );
}

function isResultMessage(
  data: unknown,
  terminal: API.Partner.PortalTerminal,
  ticketId?: number,
): data is PortalDirectLoginResultMessage {
  if (!(
    data
      && typeof data === 'object'
      && (data as PortalDirectLoginResultMessage).type === PORTAL_DIRECT_LOGIN_RESULT_MESSAGE
      && (data as PortalDirectLoginResultMessage).terminal === terminal
      && ((data as PortalDirectLoginResultMessage).status === 'success'
        || (data as PortalDirectLoginResultMessage).status === 'error')
  )) {
    return false;
  }
  return ticketId == null || (data as PortalDirectLoginResultMessage).ticketId === ticketId;
}

export function openPortalDirectLoginWindow(
  result: API.Partner.DirectLoginResult | undefined,
  terminal: API.Partner.PortalTerminal,
): Promise<PortalDirectLoginBridgeResult> | false {
  if (!result?.loginUrl || !result.token) {
    return false;
  }

  const loginUrl = buildPortalDirectLoginWindowUrl(result.loginUrl);
  const popup = window.open(loginUrl, '_blank');
  if (!popup) {
    return false;
  }

  const targetOrigin = resolveTargetOrigin(loginUrl);
  const payload: PortalDirectLoginTokenMessage = {
    type: PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE,
    terminal,
    token: result.token,
    ticketId: result.ticketId,
  };
  let cleaned = false;
  let timeoutTimer: number | undefined;
  let tokenPosted = false;
  let resolveBridge: (value: PortalDirectLoginBridgeResult) => void = () => undefined;
  let rejectBridge: (reason?: Error) => void = () => undefined;

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

  const handleBridgeMessage = (event: MessageEvent) => {
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

  const bridge = new Promise<PortalDirectLoginBridgeResult>((resolve, reject) => {
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
