import routes from '../config/routes';

type RouteItem = {
  path?: string;
  routes?: RouteItem[];
};

function flattenRoutes(items: RouteItem[]): RouteItem[] {
  return items.flatMap((item) => [item, ...flattenRoutes(item.routes ?? [])]);
}

describe('static route authority contract', () => {
  it('keeps the wildcard 404 route last so later static routes remain reachable', () => {
    const topLevelPaths = routes.map((route) => route.path);
    const wildcardIndex = topLevelPaths.indexOf('*');

    expect(wildcardIndex).toBeGreaterThanOrEqual(0);
    expect(wildcardIndex).toBe(routes.length - 1);
  });

  it('keeps management direct routes behind explicit route guard authority', () => {
    const allRoutes = flattenRoutes(routes as RouteItem[]);
    const guardedPaths = [
      '/seller',
      '/buyer',
      '/product/distribution/create',
      '/product/distribution/edit/:spuId',
      '/system/dict-data/index/:id',
      '/system/role-auth/user/:id',
      '/monitor/job-log/index/:id',
      '/tool/gen/import',
      '/tool/gen/edit',
    ];

    for (const path of guardedPaths) {
      const route = allRoutes.find((item) => item.path === path) as any;
      expect(route).toBeDefined();
      expect(route.authority).toEqual(expect.any(Array));
      expect(route.authority.length).toBeGreaterThan(0);
      expect(route.wrappers).toContain('@/wrappers/RemoteMenuRouteGuard');
    }
  });
});
