export type RemoteMenuScope = 'admin' | 'seller' | 'buyer';

export const REMOTE_MENU_SCOPES: RemoteMenuScope[] = ['admin', 'seller', 'buyer'];

export function getRemoteMenuStorageKey(scope: RemoteMenuScope = 'admin') {
  return `admin_remote_menu:${scope}`;
}
