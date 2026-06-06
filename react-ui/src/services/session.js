import { createIcon } from '@/utils/IconUtil';
import { request } from '@umijs/max';
import React, { lazy } from 'react';
const REMOTE_MENU_STORAGE_KEY = 'admin_remote_menu';
function readStoredRemoteMenu() {
    if (typeof window === 'undefined') {
        return null;
    }
    const rawMenu = window.sessionStorage.getItem(REMOTE_MENU_STORAGE_KEY);
    if (!rawMenu) {
        return null;
    }
    try {
        return JSON.parse(rawMenu);
    }
    catch {
        window.sessionStorage.removeItem(REMOTE_MENU_STORAGE_KEY);
        return null;
    }
}
let remoteMenu = readStoredRemoteMenu();
const PLANNED_PAGE_COMPONENT = 'Common/PlannedPage/index.tsx';
export function getRemoteMenu() {
    return remoteMenu;
}
export function setRemoteMenu(data) {
    remoteMenu = data;
    if (typeof window === 'undefined') {
        return;
    }
    if (data === null || data === undefined) {
        window.sessionStorage.removeItem(REMOTE_MENU_STORAGE_KEY);
        return;
    }
    window.sessionStorage.setItem(REMOTE_MENU_STORAGE_KEY, JSON.stringify(data));
}
function toPageComponentPath(component) {
    if (!component) {
        return PLANNED_PAGE_COMPONENT;
    }
    const names = component.split('/');
    let path = '';
    names.forEach(name => {
        if (path.length > 0) {
            path += '/';
        }
        if (name !== 'index') {
            path += name.at(0)?.toUpperCase() + name.substr(1);
        }
        else {
            path += name;
        }
    });
    if (!path.endsWith('.tsx')) {
        path += '.tsx';
    }
    return path;
}
function isMissingMenuPage(error, pagePath) {
    if (!(error instanceof Error)) {
        return false;
    }
    return error.message.includes('Cannot find module') && error.message.includes(pagePath);
}
function loadMenuPage(pagePath) {
    return import('@/pages/' + pagePath).catch((error) => {
        if (isMissingMenuPage(error, pagePath)) {
            console.warn(`菜单页面未实现，已显示规划中占位页: ${pagePath}`);
            return import('@/pages/' + PLANNED_PAGE_COMPONENT);
        }
        throw error;
    });
}
function patchRouteItems(route, menu, parentPath) {
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
        }
        else {
            const pagePath = toPageComponentPath(menuItem.component);
            if (route.routes === undefined) {
                route.routes = [];
            }
            if (route.children === undefined) {
                route.children = [];
            }
            const newRoute = {
                element: React.createElement(lazy(() => loadMenuPage(pagePath))),
                path: parentPath + menuItem.path,
                name: menuItem.name,
                icon: menuItem.icon,
                hideChildrenInMenu: menuItem.hideChildrenInMenu,
                hideInMenu: menuItem.hideInMenu,
                authority: menuItem.authority,
            };
            const routeIndex = route.routes.findIndex((routeItem) => routeItem.path === newRoute.path);
            if (routeIndex >= 0) {
                Object.assign(route.routes[routeIndex], newRoute);
            }
            else {
                route.routes.push(newRoute);
            }
            const childIndex = route.children.findIndex((routeItem) => routeItem.path === newRoute.path);
            if (childIndex >= 0) {
                Object.assign(route.children[childIndex], newRoute);
            }
            else {
                route.children.push(newRoute);
            }
        }
    }
}
export function patchRouteWithRemoteMenus(routes) {
    if (remoteMenu === null) {
        return;
    }
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
}
/** 获取当前的用户 GET /api/getUserInfo */
export async function getUserInfo(options) {
    return request('/api/getInfo', {
        method: 'GET',
        ...(options || {}),
    });
}
// 刷新方法
export async function refreshToken() {
    return request('/api/auth/refresh', {
        method: 'post'
    });
}
export async function getRouters() {
    return request('/api/getRouters');
}
export function convertCompatRouters(childrens) {
    return childrens.map((item) => {
        return {
            path: item.path,
            icon: createIcon(item.meta.icon),
            //  icon: item.meta.icon,
            name: item.meta.title,
            routes: item.children ? convertCompatRouters(item.children) : undefined,
            hideChildrenInMenu: item.hidden,
            hideInMenu: item.hidden,
            component: item.component,
            authority: item.perms,
        };
    });
}
export async function getRoutersInfo() {
    return getRouters().then((res) => {
        if (res.code === 200) {
            return convertCompatRouters(res.data);
        }
        else {
            return [];
        }
    });
}
export function getMatchMenuItem(path, menuData) {
    if (!menuData)
        return [];
    let items = [];
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
                        const subItem = getMatchMenuItem(subpath, item.routes);
                        items = items.concat(subItem);
                    }
                    else {
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
