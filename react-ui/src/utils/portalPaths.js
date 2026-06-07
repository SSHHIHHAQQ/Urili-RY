export const PORTAL_ROUTE_PREFIXES = [
  '/seller/login',
  '/buyer/login',
  '/seller/direct-login',
  '/buyer/direct-login',
  '/seller/portal',
  '/buyer/portal',
];

function normalizePathname(pathname) {
  return (pathname || '').split(/[?#]/, 1)[0];
}

function matchesPathPrefix(pathname, prefix) {
  const normalizedPathname = normalizePathname(pathname);
  return normalizedPathname === prefix || normalizedPathname.startsWith(`${prefix}/`);
}

export function isPortalTerminalPath(pathname, terminal) {
  return [
    `/${terminal}/login`,
    `/${terminal}/direct-login`,
    `/${terminal}/portal`,
  ].some((prefix) => matchesPathPrefix(pathname, prefix));
}

export function getPortalTerminalFromPath(pathname) {
  if (isPortalTerminalPath(pathname, 'seller')) {
    return 'seller';
  }
  if (isPortalTerminalPath(pathname, 'buyer')) {
    return 'buyer';
  }
  return undefined;
}

export function getPortalLoginPath(terminal) {
  return `/${terminal}/login`;
}

export function isPortalRoute(pathname) {
  return PORTAL_ROUTE_PREFIXES.some((prefix) => matchesPathPrefix(pathname, prefix));
}
