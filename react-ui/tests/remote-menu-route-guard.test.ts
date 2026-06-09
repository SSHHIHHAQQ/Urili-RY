import { RemoteMenuRouteGuard, getRemoteMenuStorageKey, setRemoteMenu } from '@/services/session';
import RemoteMenuRouteGuardWrapper, {
  getStaticRouteAuthority,
  getStaticRouteAuthorityMode,
} from '@/wrappers/RemoteMenuRouteGuard';
import { useAccess, useLocation } from '@umijs/max';
import { render, screen } from '@testing-library/react';
import * as fs from 'node:fs';
import * as path from 'node:path';
import React from 'react';

jest.mock('@umijs/max', () => ({
  request: jest.fn(),
  useAccess: jest.fn(),
  useLocation: jest.fn(),
}));

const mockedUseAccess = useAccess as jest.Mock;
const mockedUseLocation = useLocation as jest.Mock;
const routeConfigFile = path.join(process.cwd(), 'config', 'routes.ts');
const routeConfigJsFile = path.join(process.cwd(), 'config', 'routes.js');

function escapeRegex(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function getRouteBlock(source: string, routePath: string) {
  const pattern = new RegExp(`\\{[\\s\\S]*?path:\\s*['"]${escapeRegex(routePath)}['"][\\s\\S]*?\\n\\s*\\}`, 'm');
  const match = source.match(pattern);
  return match?.[0] ?? '';
}

function expectRouteBlock(
  source: string,
  routePath: string,
  expectations: {
    component: string;
    authority?: string;
    authorities?: string[];
    authorityMode?: 'any' | 'all';
    wrapper?: string;
    layoutFalse?: boolean;
  },
) {
  const block = getRouteBlock(source, routePath);
  expect(block).not.toBe('');
  expect(block).toMatch(new RegExp(`component:\\s*['"]${escapeRegex(expectations.component)}['"]`));
  if (expectations.authority) {
    expect(block).toMatch(new RegExp(`authority:\\s*\\[\\s*['"]${escapeRegex(expectations.authority)}['"]\\s*\\]`));
  }
  if (expectations.authorities) {
    expect(block).toMatch(/authority:\s*\[/);
    expectations.authorities.forEach((authority) => {
      expect(block).toMatch(new RegExp(`['"]${escapeRegex(authority)}['"]`));
    });
  }
  if (expectations.authorityMode) {
    expect(block).toMatch(new RegExp(`authorityMode:\\s*['"]${expectations.authorityMode}['"]`));
  }
  if (expectations.wrapper) {
    expect(block).toMatch(new RegExp(`wrappers:\\s*\\[\\s*['"]${escapeRegex(expectations.wrapper)}['"]\\s*\\]`));
  }
  if (expectations.layoutFalse) {
    expect(block).toMatch(/layout:\s*false/);
  }
}

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

  it('requires every permission when authority mode is all', () => {
    mockedUseAccess.mockReturnValue({
      hasPerms: (permission: string) => permission === 'product:distribution:query',
    });

    render(
      React.createElement(
        RemoteMenuRouteGuard,
        { authority: ['product:distribution:query', 'product:distribution:edit'], authorityMode: 'all' },
        React.createElement('span', null, 'edit product content'),
      ),
    );

    expect(screen.queryByText('edit product content')).toBeNull();
    expect(screen.getAllByText('403').length).toBeGreaterThan(0);
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
    expect(getStaticRouteAuthority('/product/distribution/create')).toEqual([
      'product:distribution:add',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ]);
    expect(getStaticRouteAuthorityMode('/product/distribution/create')).toBe('all');
    expect(getStaticRouteAuthority('/product/distribution/edit/1')).toEqual([
      'product:distribution:query',
      'product:distribution:edit',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ]);
    expect(getStaticRouteAuthorityMode('/product/distribution/edit/1')).toBe('all');
    expect(getStaticRouteAuthority('/system/dict-data/index/1')).toEqual(['system:dict:list']);
    expect(getStaticRouteAuthority('/system/role-auth/user/1')).toEqual([
      'system:role:list',
      'system:role:edit',
    ]);
    expect(getStaticRouteAuthorityMode('/system/role-auth/user/1')).toBe('all');
    expect(getStaticRouteAuthority('/monitor/job-log/index/1')).toEqual([
      'monitor:job:list',
      'monitor:job:query',
    ]);
    expect(getStaticRouteAuthorityMode('/monitor/job-log/index/1')).toBe('all');
    expect(getStaticRouteAuthority('/tool/gen/import')).toEqual(['tool:gen:list', 'tool:gen:import']);
    expect(getStaticRouteAuthorityMode('/tool/gen/import')).toBe('all');
    expect(getStaticRouteAuthority('/tool/gen/edit')).toEqual(['tool:gen:query', 'tool:gen:edit']);
    expect(getStaticRouteAuthorityMode('/tool/gen/edit')).toBe('all');
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

  it('keeps three-terminal static routes bound to their terminal components and guards', () => {
    const source = fs.readFileSync(routeConfigFile, 'utf8');
    expectRouteBlock(source, '/seller', {
      authority: 'seller:admin:list',
      component: './Seller',
      wrapper: '@/wrappers/RemoteMenuRouteGuard',
    });
    expectRouteBlock(source, '/buyer', {
      authority: 'buyer:admin:list',
      component: './Buyer',
      wrapper: '@/wrappers/RemoteMenuRouteGuard',
    });
    expectRouteBlock(source, '/seller/login', {
      component: './Portal/Login',
      layoutFalse: true,
    });
    expectRouteBlock(source, '/buyer/login', {
      component: './Portal/Login',
      layoutFalse: true,
    });
    expectRouteBlock(source, '/seller/direct-login', {
      component: './Portal/DirectLogin',
      layoutFalse: true,
    });
    expectRouteBlock(source, '/buyer/direct-login', {
      component: './Portal/DirectLogin',
      layoutFalse: true,
    });
    expectRouteBlock(source, '/seller/portal', {
      component: './Portal/Home',
      layoutFalse: true,
    });
    expectRouteBlock(source, '/buyer/portal', {
      component: './Portal/Home',
      layoutFalse: true,
    });
  });

  it('keeps static backend detail and edit routes guarded by matching backend permissions', () => {
    const source = fs.readFileSync(routeConfigFile, 'utf8');
    expectRouteBlock(source, '/system/dict-data/index/:id', {
      authorities: ['system:dict:list'],
      component: './System/DictData',
      wrapper: '@/wrappers/RemoteMenuRouteGuard',
    });
    expectRouteBlock(source, '/system/role-auth/user/:id', {
      authorities: ['system:role:list', 'system:role:edit'],
      authorityMode: 'all',
      component: './System/Role/authUser',
      wrapper: '@/wrappers/RemoteMenuRouteGuard',
    });
    expectRouteBlock(source, '/monitor/job-log/index/:id', {
      authorities: ['monitor:job:list', 'monitor:job:query'],
      authorityMode: 'all',
      component: './Monitor/JobLog',
      wrapper: '@/wrappers/RemoteMenuRouteGuard',
    });
    expectRouteBlock(source, '/tool/gen/import', {
      authorities: ['tool:gen:list', 'tool:gen:import'],
      authorityMode: 'all',
      component: './Tool/Gen/import',
      wrapper: '@/wrappers/RemoteMenuRouteGuard',
    });
    expectRouteBlock(source, '/tool/gen/edit', {
      authorities: ['tool:gen:query', 'tool:gen:edit'],
      authorityMode: 'all',
      component: './Tool/Gen/edit',
      wrapper: '@/wrappers/RemoteMenuRouteGuard',
    });
  });

  it('keeps the JavaScript route mirror as a pure re-export', () => {
    expect(fs.readFileSync(routeConfigJsFile, 'utf8').trim()).toBe("export { default } from './routes.ts';");
  });
});
