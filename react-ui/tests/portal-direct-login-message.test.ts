import {
  openPortalDirectLoginWindow,
  PORTAL_DIRECT_LOGIN_READY_MESSAGE,
  PORTAL_DIRECT_LOGIN_RESULT_MESSAGE,
  PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE,
} from '@/utils/portalDirectLoginMessage';

function dispatchMessage(source: any, origin: string, data: unknown) {
  const event = new MessageEvent('message', { data, origin });
  Object.defineProperty(event, 'source', {
    configurable: true,
    value: source,
  });
  window.dispatchEvent(event);
}

describe('portal direct-login message bridge', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.restoreAllMocks();
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('posts the one-time token after READY and resolves only after a matching consume result', async () => {
    const popup = {
      closed: false,
      postMessage: jest.fn(),
    };
    jest.spyOn(window, 'open').mockReturnValue(popup as never);

    const bridge = openPortalDirectLoginWindow(
      {
        loginUrl: 'https://seller.example.test/direct-login',
        token: 'direct-token',
        ticketId: 7,
      } as API.Partner.DirectLoginResult,
      'seller',
    );

    expect(bridge).not.toBe(false);
    jest.advanceTimersByTime(1000);
    expect(popup.postMessage).not.toHaveBeenCalled();

    dispatchMessage(popup, 'https://evil.example.test', {
      type: PORTAL_DIRECT_LOGIN_READY_MESSAGE,
      terminal: 'seller',
    });
    dispatchMessage({ postMessage: jest.fn() }, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_READY_MESSAGE,
      terminal: 'seller',
    });
    dispatchMessage(popup, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_READY_MESSAGE,
      terminal: 'buyer',
    });

    expect(popup.postMessage).not.toHaveBeenCalled();

    dispatchMessage(popup, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_READY_MESSAGE,
      terminal: 'seller',
    });

    expect(popup.postMessage).toHaveBeenCalledTimes(1);
    expect(popup.postMessage).toHaveBeenCalledWith(
      {
        type: PORTAL_DIRECT_LOGIN_TOKEN_MESSAGE,
        terminal: 'seller',
        token: 'direct-token',
        ticketId: 7,
      },
      'https://seller.example.test',
    );

    let resolved = false;
    void (bridge as Promise<unknown>).then(() => {
      resolved = true;
    });
    await Promise.resolve();
    expect(resolved).toBe(false);

    dispatchMessage(popup, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_RESULT_MESSAGE,
      terminal: 'buyer',
      status: 'success',
      ticketId: 7,
    });
    dispatchMessage(popup, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_RESULT_MESSAGE,
      terminal: 'seller',
      status: 'success',
      ticketId: 8,
    });
    await Promise.resolve();
    expect(resolved).toBe(false);

    dispatchMessage(popup, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_RESULT_MESSAGE,
      terminal: 'seller',
      status: 'success',
      ticketId: 7,
    });

    await expect(bridge as Promise<unknown>).resolves.toEqual({ status: 'success', ticketId: 7 });
  });

  it('does not open a bridge without a token or popup window', () => {
    const openSpy = jest.spyOn(window, 'open').mockReturnValue(null);

    expect(
      openPortalDirectLoginWindow(
        { loginUrl: 'https://seller.example.test/direct-login' } as API.Partner.DirectLoginResult,
        'seller',
      ),
    ).toBe(false);

    expect(
      openPortalDirectLoginWindow(
        {
          loginUrl: 'https://seller.example.test/direct-login',
          token: 'direct-token',
        } as API.Partner.DirectLoginResult,
        'seller',
      ),
    ).toBe(false);
    expect(openSpy).toHaveBeenCalledTimes(1);
  });

  it('rejects when the target popup reports direct-login consume failure', async () => {
    const popup = {
      closed: false,
      postMessage: jest.fn(),
    };
    jest.spyOn(window, 'open').mockReturnValue(popup as never);

    const bridge = openPortalDirectLoginWindow(
      {
        loginUrl: 'https://seller.example.test/direct-login',
        token: 'direct-token',
        ticketId: 7,
      } as API.Partner.DirectLoginResult,
      'seller',
    );

    dispatchMessage(popup, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_READY_MESSAGE,
      terminal: 'seller',
    });
    const assertion = expect(bridge as Promise<unknown>).rejects.toThrow('ticket expired');
    dispatchMessage(popup, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_RESULT_MESSAGE,
      terminal: 'seller',
      status: 'error',
      ticketId: 7,
      message: 'ticket expired',
    });

    await assertion;
  });

  it('rejects when the target popup never confirms token consumption', async () => {
    const popup = {
      closed: false,
      postMessage: jest.fn(),
    };
    jest.spyOn(window, 'open').mockReturnValue(popup as never);

    const bridge = openPortalDirectLoginWindow(
      {
        loginUrl: 'https://seller.example.test/direct-login',
        token: 'direct-token',
        ticketId: 7,
      } as API.Partner.DirectLoginResult,
      'seller',
    );

    dispatchMessage(popup, 'https://seller.example.test', {
      type: PORTAL_DIRECT_LOGIN_READY_MESSAGE,
      terminal: 'seller',
    });
    const assertion = expect(bridge as Promise<unknown>).rejects.toThrow('DIRECT_LOGIN_CONSUME_TIMEOUT');
    jest.advanceTimersByTime(15000);

    await assertion;
  });
});
