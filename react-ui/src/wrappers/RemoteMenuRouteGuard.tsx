import { RemoteMenuRouteGuard } from '@/services/session';
import { useLocation } from '@umijs/max';
import React from 'react';

const STATIC_ROUTE_AUTHORITIES: Record<string, string[]> = {
  '/seller': ['seller:admin:list'],
  '/buyer': ['buyer:admin:list'],
  '/product/distribution/create': ['product:distribution:add'],
  '/product/distribution/edit': ['product:distribution:edit'],
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

export function getStaticRouteAuthority(pathname: string): string[] | undefined {
  const normalizedPathname = normalizePathname(pathname);
  const isPublicPortalPath = [...PUBLIC_PORTAL_ROUTE_PATHS].some(
    (path) => normalizedPathname === path || normalizedPathname.startsWith(`${path}/`),
  );
  if (isPublicPortalPath) {
    return undefined;
  }

  const routePrefix = Object.keys(STATIC_ROUTE_AUTHORITIES).find(
    (path) => normalizedPathname === path || normalizedPathname.startsWith(`${path}/`),
  );

  return routePrefix ? STATIC_ROUTE_AUTHORITIES[routePrefix] : undefined;
}

type RemoteMenuRouteGuardWrapperProps = {
  children?: React.ReactNode;
  route?: {
    authority?: unknown;
  };
};

export default function RemoteMenuRouteGuardWrapper({
  children,
  route,
}: RemoteMenuRouteGuardWrapperProps) {
  const location = useLocation();
  const authority = route?.authority ?? getStaticRouteAuthority(location.pathname) ?? [];

  return <RemoteMenuRouteGuard authority={authority}>{children}</RemoteMenuRouteGuard>;
}
