import { RemoteMenuRouteGuard, getRemoteMenuStorageKey, setRemoteMenu } from '@/services/session';
import RemoteMenuRouteGuardWrapper, {
  getStaticRouteAuthority,
} from '@/wrappers/RemoteMenuRouteGuard';
import { useAccess, useLocation } from '@umijs/max';
import { render, screen } from '@testing-library/react';
import React from 'react';

jest.mock('@umijs/max', () => ({
  request: jest.fn(),
  useAccess: jest.fn(),
  useLocation: jest.fn(),
}));

const mockedUseAccess = useAccess as jest.Mock;
const mockedUseLocation = useLocation as jest.Mock;

describe('remote menu route guard', () => {
  afterEach(() => {
    jest.restoreAllMocks();
    jest.clearAllMocks();
  });

  it('uses distinct remote menu cache keys by terminal scope', () => {
    const keys = ['admin', 'seller', 'buyer'].map((scope) =>
      getRemoteMenuStorageKey(scope as 'admin' | 'seller' | 'buyer'),
    );

    expect(new Set(keys).size).toBe(keys.length);
    expect(getRemoteMenuStorageKey('seller')).toBe('admin_remote_menu:seller');
    expect(getRemoteMenuStorageKey('seller')).not.toBe(getRemoteMenuStorageKey('buyer'));
  });

  it('persists remote menus under the selected scoped key', () => {
    const setItemSpy = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => undefined);
    const removeItemSpy = jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => undefined);

    setRemoteMenu([{ path: '/seller' }], 'seller');
    setRemoteMenu(null, 'seller');
    setRemoteMenu(null, 'admin');

    expect(setItemSpy).toHaveBeenCalledWith(
      'admin_remote_menu:seller',
      expect.stringContaining('/seller'),
    );
    expect(removeItemSpy).toHaveBeenCalledWith('admin_remote_menu:seller');
    expect(removeItemSpy).toHaveBeenCalledWith('admin_remote_menu:admin');
  });

  it('renders children when one required permission matches', () => {
    mockedUseAccess.mockReturnValue({
      hasPerms: (permission: string) => permission === 'seller:admin:list',
    });

    render(
      React.createElement(
        RemoteMenuRouteGuard,
        { authority: ['seller:admin:list', 'buyer:admin:list'] },
        React.createElement('span', null, 'seller management content'),
      ),
    );

    expect(screen.getByText('seller management content')).toBeTruthy();
  });

  it('renders 403 when required permissions are missing', () => {
    mockedUseAccess.mockReturnValue({
      hasPerms: () => false,
    });

    render(
      React.createElement(
        RemoteMenuRouteGuard,
        { authority: ['buyer:admin:list'] },
        React.createElement('span', null, 'buyer management content'),
      ),
    );

    expect(screen.queryByText('buyer management content')).toBeNull();
    expect(screen.getAllByText('403').length).toBeGreaterThan(0);
  });

  it('renders 403 when the remote menu route has no authority', () => {
    mockedUseAccess.mockReturnValue({
      hasPerms: () => true,
    });

    render(
      React.createElement(
        RemoteMenuRouteGuard,
        { authority: [] },
        React.createElement('span', null, 'unguarded remote menu content'),
      ),
    );

    expect(screen.queryByText('unguarded remote menu content')).toBeNull();
    expect(screen.getAllByText('403').length).toBeGreaterThan(0);
  });

  it('maps static seller and buyer fallback paths to terminal admin permissions', () => {
    expect(getStaticRouteAuthority('/seller')).toEqual(['seller:admin:list']);
    expect(getStaticRouteAuthority('/seller/')).toEqual(['seller:admin:list']);
    expect(getStaticRouteAuthority('/seller/missing')).toEqual(['seller:admin:list']);
    expect(getStaticRouteAuthority('/buyer')).toEqual(['buyer:admin:list']);
    expect(getStaticRouteAuthority('/buyer/')).toEqual(['buyer:admin:list']);
    expect(getStaticRouteAuthority('/buyer/missing')).toEqual(['buyer:admin:list']);
    expect(getStaticRouteAuthority('/seller/direct-login')).toBeUndefined();
    expect(getStaticRouteAuthority('/seller/portal/orders/1')).toBeUndefined();
    expect(getStaticRouteAuthority('/buyer/login')).toBeUndefined();
    expect(getStaticRouteAuthority('/buyer/portal/account/profile')).toBeUndefined();
    expect(getStaticRouteAuthority('/account/center')).toBeUndefined();
  });

  it('uses seller fallback authority for guarded static seller paths', () => {
    mockedUseLocation.mockReturnValue({ pathname: '/seller/' });
    mockedUseAccess.mockReturnValue({
      hasPerms: (permission: string) => permission === 'seller:admin:list',
    });

    render(
      React.createElement(
        RemoteMenuRouteGuardWrapper,
        { route: {} },
        React.createElement('span', null, 'seller fallback content'),
      ),
    );

    expect(screen.getByText('seller fallback content')).toBeTruthy();
  });

  it('does not allow buyer permissions to pass seller fallback routes', () => {
    mockedUseLocation.mockReturnValue({ pathname: '/seller' });
    mockedUseAccess.mockReturnValue({
      hasPerms: (permission: string) => permission === 'buyer:admin:list',
    });

    render(
      React.createElement(
        RemoteMenuRouteGuardWrapper,
        { route: {} },
        React.createElement('span', null, 'seller fallback content'),
      ),
    );

    expect(screen.queryByText('seller fallback content')).toBeNull();
    expect(screen.getAllByText('403').length).toBeGreaterThan(0);
  });

  it('prefers explicit route authority over static fallback authority', () => {
    mockedUseLocation.mockReturnValue({ pathname: '/seller' });
    mockedUseAccess.mockReturnValue({
      hasPerms: (permission: string) => permission === 'buyer:admin:list',
    });

    render(
      React.createElement(
        RemoteMenuRouteGuardWrapper,
        { route: { authority: ['buyer:admin:list'] } },
        React.createElement('span', null, 'explicit authority content'),
      ),
    );

    expect(screen.getByText('explicit authority content')).toBeTruthy();
  });
});
