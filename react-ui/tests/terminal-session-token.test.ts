import {
  clearTerminalSessionToken,
  getTerminalSessionTokenKeys,
  setTerminalSessionToken,
  type SessionTerminal,
} from '../src/access';
import { persistPortalLogin } from '../src/pages/Portal/terminal';
import fs from 'fs';
import path from 'path';

describe('terminal session token isolation', () => {
  const terminals: SessionTerminal[] = ['admin', 'seller', 'buyer'];
  let setItemSpy: jest.SpyInstance;
  let removeItemSpy: jest.SpyInstance;

  beforeEach(() => {
    setItemSpy = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => undefined);
    removeItemSpy = jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('uses distinct storage keys for admin, seller, and buyer terminals', () => {
    const tokenKeys = terminals.flatMap((terminal) =>
      Object.values(getTerminalSessionTokenKeys(terminal)),
    );

    expect(new Set(tokenKeys).size).toBe(tokenKeys.length);
    expect(getTerminalSessionTokenKeys('seller').accessToken).not.toBe(
      getTerminalSessionTokenKeys('buyer').accessToken,
    );
    expect(getTerminalSessionTokenKeys('seller').accessToken).not.toBe(
      getTerminalSessionTokenKeys('admin').accessToken,
    );
  });

  it('writes and clears only the selected terminal token keys', () => {
    setTerminalSessionToken('buyer', 'buyer-token', 'buyer-refresh', 1780742400000);

    expect(setItemSpy).toHaveBeenCalledWith('buyer_access_token', 'buyer-token');
    expect(setItemSpy).toHaveBeenCalledWith('buyer_refresh_token', 'buyer-refresh');
    expect(setItemSpy).toHaveBeenCalledWith('buyer_expireTime', '1780742400000');
    expect(setItemSpy).not.toHaveBeenCalledWith('seller_access_token', expect.anything());

    clearTerminalSessionToken('buyer');

    expect(removeItemSpy).toHaveBeenCalledWith('buyer_access_token');
    expect(removeItemSpy).toHaveBeenCalledWith('buyer_refresh_token');
    expect(removeItemSpy).toHaveBeenCalledWith('buyer_expireTime');
    expect(removeItemSpy).not.toHaveBeenCalledWith('seller_access_token');
  });

  it('persists a portal login only when the response terminal matches the page terminal', () => {
    jest.spyOn(Date, 'now').mockReturnValue(1780742400000);

    expect(
      persistPortalLogin(
        {
          token: 'seller-token',
          terminal: 'seller',
          username: 'seller-owner',
          expireMinutes: 30,
        },
        'seller',
      ),
    ).toBe(true);

    expect(setItemSpy).toHaveBeenCalledWith('seller_access_token', 'seller-token');
    expect(setItemSpy).toHaveBeenCalledWith('seller_expireTime', '1780744200000');
    expect(setItemSpy).not.toHaveBeenCalledWith('buyer_access_token', expect.anything());
  });

  it('rejects a portal login response from another terminal without clearing existing tokens', () => {
    expect(
      persistPortalLogin(
        {
          token: 'seller-token',
          terminal: 'seller',
          username: 'seller-owner',
        },
        'buyer',
      ),
    ).toBe(false);

    expect(setItemSpy).not.toHaveBeenCalledWith('buyer_access_token', 'seller-token');
    expect(removeItemSpy).not.toHaveBeenCalledWith('buyer_access_token');
    expect(removeItemSpy).not.toHaveBeenCalledWith('seller_access_token');
  });

  it('does not pre-clear existing portal tokens before login or direct-login succeeds', () => {
    const portalPageSources = [
      '../src/pages/Portal/Login/index.tsx',
      '../src/pages/Portal/DirectLogin/index.tsx',
      '../src/pages/Portal/DirectLogin/index.js',
    ].map((relativePath) => fs.readFileSync(path.resolve(__dirname, relativePath), 'utf8'));

    for (const source of portalPageSources) {
      expect(source).not.toContain('clearPortalLogin');
    }
  });
});
