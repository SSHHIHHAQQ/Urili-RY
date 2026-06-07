export const REMOTE_MENU_SCOPES = ['admin', 'seller', 'buyer'];
export function getRemoteMenuStorageKey(scope = 'admin') {
    return `admin_remote_menu:${scope}`;
}
