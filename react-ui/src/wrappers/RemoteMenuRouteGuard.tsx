import { RemoteMenuRouteGuard } from '@/services/session';
import { useLocation } from '@umijs/max';
import React from 'react';

type RouteAuthorityMode = 'any' | 'all';

type StaticRouteRequirement = {
  authority: string[];
  authorityMode?: RouteAuthorityMode;
};

const STATIC_ROUTE_REQUIREMENTS: Record<string, StaticRouteRequirement> = {
  '/seller': { authority: ['seller:admin:list'] },
  '/buyer': { authority: ['buyer:admin:list'] },
  '/product/distribution/create': {
    authority: [
      'product:distribution:add',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ],
    authorityMode: 'all',
  },
  '/product/distribution/edit': {
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
  },
  '/system/dict-data/index': { authority: ['system:dict:list'] },
  '/system/role-auth/user': {
    authority: ['system:role:list', 'system:role:edit'],
    authorityMode: 'all',
  },
  '/monitor/job-log/index': {
    authority: ['monitor:job:list', 'monitor:job:query'],
    authorityMode: 'all',
  },
  '/tool/gen/import': {
    authority: ['tool:gen:list', 'tool:gen:import'],
    authorityMode: 'all',
  },
  '/tool/gen/edit': {
    authority: ['tool:gen:query', 'tool:gen:edit'],
    authorityMode: 'all',
  },
};

const PUBLIC_PORTAL_ROUTE_PATHS = new Set([
  '/seller/login',
  '/buyer/login',
  '/seller/direct-login',
  '/buyer/direct-login',
  '/seller/portal',
  '/buyer/portal',
]);

function normalizePathname(pathname: string) {
  const pathnameOnly = pathname.split(/[?#]/)[0];
  if (pathnameOnly.length > 1 && pathnameOnly.endsWith('/')) {
    return pathnameOnly.slice(0, -1);
  }
  return pathnameOnly || '/';
}

function getStaticRouteRequirement(pathname: string): StaticRouteRequirement | undefined {
  const normalizedPathname = normalizePathname(pathname);
  const isPublicPortalPath = [...PUBLIC_PORTAL_ROUTE_PATHS].some(
    (path) => normalizedPathname === path || normalizedPathname.startsWith(`${path}/`),
  );
  if (isPublicPortalPath) {
    return undefined;
  }

  const routePrefix = Object.keys(STATIC_ROUTE_REQUIREMENTS).find(
    (path) => normalizedPathname === path || normalizedPathname.startsWith(`${path}/`),
  );

  return routePrefix ? STATIC_ROUTE_REQUIREMENTS[routePrefix] : undefined;
}

export function getStaticRouteAuthority(pathname: string): string[] | undefined {
  return getStaticRouteRequirement(pathname)?.authority;
}

export function getStaticRouteAuthorityMode(pathname: string): RouteAuthorityMode {
  return getStaticRouteRequirement(pathname)?.authorityMode ?? 'any';
}

type RemoteMenuRouteGuardWrapperProps = {
  children?: React.ReactNode;
  route?: {
    authority?: unknown;
    authorityMode?: unknown;
  };
};

export default function RemoteMenuRouteGuardWrapper({
  children,
  route,
}: RemoteMenuRouteGuardWrapperProps) {
  const location = useLocation();
  const authority = route?.authority ?? getStaticRouteAuthority(location.pathname) ?? [];
  const authorityMode = route?.authorityMode === 'all' ? 'all' : getStaticRouteAuthorityMode(location.pathname);

  return <RemoteMenuRouteGuard authority={authority} authorityMode={authorityMode}>{children}</RemoteMenuRouteGuard>;
}
