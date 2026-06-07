export const PORTAL_ROUTE_PREFIXES = [
  '/seller/login',
  '/buyer/login',
  '/seller/direct-login',
  '/buyer/direct-login',
  '/seller/portal',
  '/buyer/portal',
];

export function getPortalTerminalFromPath(pathname) {
  if (pathname.startsWith('/seller/')) {
    return 'seller';
  }
  if (pathname.startsWith('/buyer/')) {
    return 'buyer';
  }
  return undefined;
}

export function getPortalLoginPath(terminal) {
  return `/${terminal}/login`;
}

export function isPortalRoute(pathname) {
  return PORTAL_ROUTE_PREFIXES.some((prefix) => pathname === prefix || pathname?.startsWith(`${prefix}/`));
}
