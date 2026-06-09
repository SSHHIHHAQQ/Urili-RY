import type { SessionTerminal } from '@/access';

export type PortalTerminal = Exclude<SessionTerminal, 'admin'>;

export const PORTAL_ROUTE_PREFIXES = [
  '/seller/login',
  '/buyer/login',
  '/seller/direct-login',
  '/buyer/direct-login',
  '/seller/portal',
  '/buyer/portal',
];

function normalizePathname(pathname?: string) {
  return (pathname || '').split(/[?#]/, 1)[0];
}

function matchesPathPrefix(pathname: string | undefined, prefix: string) {
  const normalizedPathname = normalizePathname(pathname);
  return normalizedPathname === prefix || normalizedPathname.startsWith(`${prefix}/`);
}

export function isPortalTerminalPath(pathname: string | undefined, terminal: PortalTerminal) {
  const normalizedPathname = normalizePathname(pathname);
  return normalizedPathname === `/${terminal}/login`
    || normalizedPathname === `/${terminal}/direct-login`
    || matchesPathPrefix(normalizedPathname, `/${terminal}/portal`);
}

export function getPortalTerminalFromPath(pathname: string): PortalTerminal | undefined {
  if (isPortalTerminalPath(pathname, 'seller')) {
    return 'seller';
  }
  if (isPortalTerminalPath(pathname, 'buyer')) {
    return 'buyer';
  }
  return undefined;
}

export function getPortalLoginPath(terminal: PortalTerminal) {
  return `/${terminal}/login`;
}

export function isPortalRoute(pathname?: string) {
  return isPortalTerminalPath(pathname, 'seller') || isPortalTerminalPath(pathname, 'buyer');
}
