const PORTAL_API_PREFIXES = [
    {
        terminal: 'seller',
        prefix: '/api/seller',
        adminPrefix: '/api/seller/admin',
    },
    { terminal: 'buyer', prefix: '/api/buyer', adminPrefix: '/api/buyer/admin' },
];
function getRequestPathname(url) {
    if (!url) {
        return '';
    }
    try {
        return new URL(url, 'http://localhost').pathname;
    }
    catch {
        return url.split('?')[0] || '';
    }
}
export function getPortalTerminalFromApiUrl(url) {
    const pathname = getRequestPathname(url);
    for (const item of PORTAL_API_PREFIXES) {
        if (pathname === item.adminPrefix ||
            pathname.startsWith(`${item.adminPrefix}/`)) {
            continue;
        }
        if (pathname === item.prefix || pathname.startsWith(`${item.prefix}/`)) {
            return item.terminal;
        }
    }
    return undefined;
}
