import { createIcon } from '@/utils/IconUtil';
import { getRemoteMenuStorageKey, type RemoteMenuScope } from '@/utils/remoteMenuStorage';
import { MenuDataItem } from '@ant-design/pro-components';
import { request, useAccess } from '@umijs/max';
import { Result } from 'antd';
import React, { lazy } from 'react';


export { getRemoteMenuStorageKey } from '@/utils/remoteMenuStorage';

function readStoredRemoteMenu(scope: RemoteMenuScope = 'admin') {
  if (typeof window === 'undefined') {
    return null;
  }
  const storageKey = getRemoteMenuStorageKey(scope);
  const rawMenu = window.sessionStorage.getItem(storageKey);
  if (!rawMenu) {
    return null;
  }
  try {
    return JSON.parse(rawMenu);
  } catch {
    window.sessionStorage.removeItem(storageKey);
    return null;
  }
}

let remoteMenuScope: RemoteMenuScope = 'admin';
let remoteMenu: any = readStoredRemoteMenu(remoteMenuScope);

const PLANNED_PAGE_COMPONENT = 'Common/PlannedPage/index.tsx';

export function getRemoteMenu(scope: RemoteMenuScope = remoteMenuScope) {
  if (scope !== remoteMenuScope) {
    remoteMenuScope = scope;
    remoteMenu = readStoredRemoteMenu(scope);
  }
  return remoteMenu;
}

export function setRemoteMenu(data: any, scope: RemoteMenuScope = remoteMenuScope) {
  remoteMenuScope = scope;
  remoteMenu = data;
  if (typeof window === 'undefined') {
    return;
  }
  const storageKey = getRemoteMenuStorageKey(scope);
  if (data === null || data === undefined) {
    window.sessionStorage.removeItem(storageKey);
    return;
  }
  window.sessionStorage.setItem(storageKey, JSON.stringify(data));
}

function toPageComponentPath(component?: string) {
  if (!component) {
    return PLANNED_PAGE_COMPONENT;
  }

  const names: string[] = component.split('/');
  let path = '';
  names.forEach(name => {
    if (path.length > 0) {
      path += '/';
    }
    if (name !== 'index') {
      path += name.at(0)?.toUpperCase() + name.substr(1);
    } else {
      path += name;
    }
  });
  if (!path.endsWith('.tsx')) {
    path += '.tsx';
  }
  return path;
}

function isMissingMenuPage(error: unknown, pagePath: string) {
  if (!(error instanceof Error)) {
    return false;
  }
  return error.message.includes('Cannot find module') && error.message.includes(pagePath);
}

function loadMenuPage(pagePath: string) {
  return import('@/pages/' + pagePath).catch((error) => {
    if (isMissingMenuPage(error, pagePath)) {
      console.warn(`菜单页面未实现，已显示规划中占位页: ${pagePath}`);
      return import('@/pages/' + PLANNED_PAGE_COMPONENT);
    }
    throw error;
  });
}

function normalizeAuthority(authority: unknown): string[] {
  if (!authority) {
    return [];
  }
  if (Array.isArray(authority)) {
    return authority.filter((item): item is string => typeof item === 'string' && item.length > 0);
  }
  return typeof authority === 'string' && authority.length > 0 ? [authority] : [];
}

function toRouteAuthority(perms?: string) {
  const permission = typeof perms === 'string' ? perms.trim() : '';
  return permission ? [permission] : [];
}

type RouteAuthorityMode = 'any' | 'all';

export function RemoteMenuRouteGuard({
  authority,
  authorityMode = 'any',
  children,
}: {
  authority?: unknown;
  authorityMode?: RouteAuthorityMode;
  children?: React.ReactNode;
}) {
  const permissions = normalizeAuthority(authority);
  const access = useAccess();
  const allowed = permissions.length > 0 && (authorityMode === 'all'
    ? permissions.every((permission) => access.hasPerms(permission))
    : permissions.some((permission) => access.hasPerms(permission)));

  if (!allowed) {
    return React.createElement(Result, {
      status: '403',
      title: '403',
      subTitle: 'Forbidden',
    });
  }

  return React.createElement(React.Fragment, null, children);
}

function createGuardedMenuElement(pagePath: string, authority: unknown, authorityMode?: RouteAuthorityMode) {
  return React.createElement(
    RemoteMenuRouteGuard,
    { authority, authorityMode },
    React.createElement(lazy(() => loadMenuPage(pagePath))),
  );
}

const STATIC_GUARDED_LAYOUT_ROUTES: Array<{
  path: string;
  pagePath: string;
  authority: string[];
  authorityMode?: RouteAuthorityMode;
  name: string;
}> = [
  {
    path: '/product/distribution/create',
    pagePath: 'Product/Distribution/EditPage.tsx',
    authority: [
      'product:distribution:add',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ],
    authorityMode: 'all',
    name: '新增商城商品',
  },
  {
    path: '/product/distribution/edit/:spuId',
    pagePath: 'Product/Distribution/EditPage.tsx',
    authority: [
      'product:distribution:query',
      'product:distribution:edit',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ],
    authorityMode: 'all',
    name: '编辑商城商品',
  },
];

function upsertLayoutRoute(route: any, routeItem: any) {
  if (route.routes === undefined) {
    route.routes = [];
  }
  if (route.children === undefined) {
    route.children = [];
  }

  const newRoute = {
    element: createGuardedMenuElement(routeItem.pagePath, routeItem.authority, routeItem.authorityMode),
    path: routeItem.path,
    name: routeItem.name,
    hideChildrenInMenu: true,
    hideInMenu: true,
    authority: routeItem.authority,
    authorityMode: routeItem.authorityMode,
  };
  const routeIndex = route.routes.findIndex((item: any) => item.path === newRoute.path);
  if (routeIndex >= 0) {
    Object.assign(route.routes[routeIndex], newRoute);
  } else {
    route.routes.push(newRoute);
  }
  const childIndex = route.children.findIndex((item: any) => item.path === newRoute.path);
  if (childIndex >= 0) {
    Object.assign(route.children[childIndex], newRoute);
  } else {
    route.children.push(newRoute);
  }
}

function patchStaticGuardedLayoutRoutes(route: any) {
  STATIC_GUARDED_LAYOUT_ROUTES.forEach((routeItem) => upsertLayoutRoute(route, routeItem));
}

function patchRouteItems(route: any, menu: any, parentPath: string) {
  for (const menuItem of menu) {
    if (menuItem.component === 'Layout' || menuItem.component === 'ParentView') {
      if (menuItem.routes) {
        let hasItem = false;
        let newItem = null;
        if (route.routes === undefined) {
          route.routes = [];
        }
        if (route.children === undefined) {
          route.children = [];
        }
        for (const routeChild of route.routes) {
          if (routeChild.path === menuItem.path) {
            hasItem = true;
            newItem = routeChild;
          }
        }
        if (!hasItem) {
          newItem = {
            path: menuItem.path,
            routes: [],
            children: [],
          };
          route.routes.push(newItem);
        }
        if (newItem.routes === undefined) {
          newItem.routes = [];
        }
        if (newItem.children === undefined) {
          newItem.children = [];
        }
        Object.assign(newItem, {
          name: menuItem.name,
          icon: menuItem.icon,
          hideChildrenInMenu: menuItem.hideChildrenInMenu,
          hideInMenu: menuItem.hideInMenu,
          authority: menuItem.authority,
        });
        patchRouteItems(newItem, menuItem.routes, parentPath + menuItem.path + '/');
      }
    } else {
      const pagePath = toPageComponentPath(menuItem.component);
      if (route.routes === undefined) {
        route.routes = [];
      }
      if (route.children === undefined) {
        route.children = [];
      }
      const newRoute = {
        element: createGuardedMenuElement(pagePath, menuItem.authority),
        path: parentPath + menuItem.path,
        name: menuItem.name,
        icon: menuItem.icon,
        hideChildrenInMenu: menuItem.hideChildrenInMenu,
        hideInMenu: menuItem.hideInMenu,
        authority: menuItem.authority,
      };
      const routeIndex = route.routes.findIndex((routeItem: any) => routeItem.path === newRoute.path);
      if (routeIndex >= 0) {
        Object.assign(route.routes[routeIndex], newRoute);
      } else {
        route.routes.push(newRoute);
      }
      const childIndex = route.children.findIndex((routeItem: any) => routeItem.path === newRoute.path);
      if (childIndex >= 0) {
        Object.assign(route.children[childIndex], newRoute);
      } else {
        route.children.push(newRoute);
      }
    }
  }
}

export function patchRouteWithRemoteMenus(routes: any) {
  if (remoteMenu === null) { return; }
  let proLayout = null;
  for (const routeItem of routes) {
    if (routeItem.id === 'ant-design-pro-layout') {
      proLayout = routeItem;
      break;
    }
  }
  if (!proLayout) {
    return;
  }
  patchRouteItems(proLayout, remoteMenu, '');
  patchStaticGuardedLayoutRoutes(proLayout);
}

/** 获取当前的用户 GET /api/getUserInfo */
export async function getUserInfo(options?: Record<string, any>) {
  return request<API.UserInfoResult>('/api/getInfo', {
    method: 'GET',
    ...(options || {}),
  });
}

// 刷新方法
export async function refreshToken() {
  return request('/api/auth/refresh', {
    method: 'post'
  })
}

export async function getRouters(): Promise<any> {
  return request('/api/getRouters');
}

export function convertCompatRouters(childrens: API.RoutersMenuItem[]): any[] {
  return childrens.map((item: API.RoutersMenuItem) => {
    return {
      path: item.path,
      icon: createIcon(item.meta.icon),
      //  icon: item.meta.icon,
      name: item.meta.title,
      routes: item.children ? convertCompatRouters(item.children) : undefined,
      hideChildrenInMenu: item.hidden,
      hideInMenu: item.hidden,
      component: item.component,
      authority: toRouteAuthority(item.perms),
    };
  });
}

export async function getRoutersInfo(): Promise<MenuDataItem[]> {
  return getRouters().then((res) => {
    if (Number(res?.code) === 200) {
      return convertCompatRouters(res.data || []);
    }
    const error = new Error(res?.msg || res?.message || 'Failed to load remote menus') as Error & {
      code?: unknown;
      info?: { errorCode?: unknown; errorMessage?: unknown };
      response?: { data?: unknown };
    };
    error.code = res?.code;
    error.info = { errorCode: res?.code, errorMessage: res?.msg || res?.message };
    error.response = { data: res };
    throw error;
  });
}

export function getMatchMenuItem(
  path: string,
  menuData: MenuDataItem[] | undefined,
): MenuDataItem[] {
  if (!menuData) return [];
  let items: MenuDataItem[] = [];
  menuData.forEach((item) => {
    if (item.path) {
      if (item.path === path) {
        items.push(item);
        return;
      }
      if (path.length >= item.path?.length) {
        const exp = `${item.path}/*`;
        if (path.match(exp)) {
          if (item.routes) {
            const subpath = path.substr(item.path.length + 1);
            const subItem: MenuDataItem[] = getMatchMenuItem(subpath, item.routes);
            items = items.concat(subItem);
          } else {
            const paths = path.split('/');
            if (paths.length >= 2 && paths[0] === item.path && paths[1] === 'index') {
              items.push(item);
            }
          }
        }
      }
    }
  });
  return items;
}
