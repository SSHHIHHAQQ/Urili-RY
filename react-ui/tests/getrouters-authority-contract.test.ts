import { convertCompatRouters, getRoutersInfo, RemoteMenuRouteGuard } from '@/services/session';
import { request, useAccess } from '@umijs/max';
import { render, screen } from '@testing-library/react';
import React from 'react';

jest.mock('@umijs/max', () => ({
  request: jest.fn(),
  useAccess: jest.fn(),
}));

const mockedUseAccess = useAccess as jest.Mock;
const mockedRequest = request as unknown as jest.Mock;

describe('getRouters authority contract', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('maps backend perms into route authority arrays and keeps missing perms fail-closed', () => {
    const routes = convertCompatRouters([
      {
        path: '/seller/accounts',
        component: 'Portal/Home/index',
        perms: 'seller:account:list',
        hidden: false,
        meta: { title: '账号', icon: '' },
      },
      {
        path: '/seller/missing-perms',
        component: 'Portal/Home/index',
        hidden: false,
        meta: { title: '缺权限', icon: '' },
      },
    ] as any);

    expect(routes[0].authority).toEqual(['seller:account:list']);
    expect(routes[1].authority).toEqual([]);

    mockedUseAccess.mockReturnValue({
      hasPerms: () => true,
    });

    render(
      React.createElement(
        RemoteMenuRouteGuard,
        { authority: routes[1].authority },
        React.createElement('span', null, 'missing permission route'),
      ),
    );

    expect(screen.queryByText('missing permission route')).toBeNull();
    expect(screen.getAllByText('403').length).toBeGreaterThan(0);
  });

  it('rejects non-success getRouters responses instead of caching an empty menu', async () => {
    mockedRequest.mockResolvedValue({
      code: 500,
      msg: 'menu load failed',
      data: [],
    });

    await expect(getRoutersInfo()).rejects.toThrow('menu load failed');
  });
});
