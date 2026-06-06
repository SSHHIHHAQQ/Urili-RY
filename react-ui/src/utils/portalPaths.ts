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

export function getPortalTerminalFromPath(pathname: string): PortalTerminal | undefined {
  if (pathname.startsWith('/seller/')) {
    return 'seller';
  }
  if (pathname.startsWith('/buyer/')) {
    return 'buyer';
  }
  return undefined;
}

export function getPortalLoginPath(terminal: PortalTerminal) {
  return `/${terminal}/login`;
}

export function isPortalRoute(pathname?: string) {
  return PORTAL_ROUTE_PREFIXES.some((prefix) => pathname === prefix || pathname?.startsWith(`${prefix}/`));
}
