import { isPortalRoute } from './portalPaths';

const DEFAULT_ADMIN_REDIRECT = '/';
const ADMIN_REDIRECT_BASE = 'http://admin.local';

export function resolveAdminRedirect(rawRedirect?: string | null) {
  const redirect = typeof rawRedirect === 'string' ? rawRedirect.trim() : '';
  if (
    !redirect
    || !redirect.startsWith('/')
    || redirect.startsWith('//')
    || redirect.includes('\\')
  ) {
    return DEFAULT_ADMIN_REDIRECT;
  }

  try {
    const url = new URL(redirect, ADMIN_REDIRECT_BASE);
    if (url.origin !== ADMIN_REDIRECT_BASE) {
      return DEFAULT_ADMIN_REDIRECT;
    }

    const resolved = `${url.pathname}${url.search}${url.hash}`;
    if (isPortalRoute(resolved) || resolved === '/user/login') {
      return DEFAULT_ADMIN_REDIRECT;
    }
    return resolved || DEFAULT_ADMIN_REDIRECT;
  } catch {
    return DEFAULT_ADMIN_REDIRECT;
  }
}

export function resolveAdminRedirectFromSearch(search?: string) {
  return resolveAdminRedirect(new URLSearchParams(search || '').get('redirect'));
}
