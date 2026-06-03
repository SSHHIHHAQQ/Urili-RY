import { App, Modal as staticModal, message as staticMessage, notification as staticNotification } from 'antd';
import type { ReactNode } from 'react';
import { useEffect } from 'react';
import type { useAppProps } from 'antd/es/app/context';

type MessageApi = useAppProps['message'];
type NotificationApi = useAppProps['notification'];
type ModalApi = useAppProps['modal'];

let appApi: useAppProps | undefined;

function bindApi<T extends object>(getApi: () => T): T {
  return new Proxy({} as T, {
    get(_, property) {
      const api = getApi() as Record<PropertyKey, unknown>;
      const value = api[property];
      return typeof value === 'function' ? value.bind(api) : value;
    },
  });
}

export const message = bindApi<MessageApi>(() => appApi?.message ?? staticMessage);
export const notification = bindApi<NotificationApi>(() => appApi?.notification ?? staticNotification);
export const modal = bindApi<ModalApi>(() => appApi?.modal ?? (staticModal as unknown as ModalApi));

export function AntdFeedbackProvider({ children }: { children: ReactNode }) {
  const api = App.useApp();

  useEffect(() => {
    appApi = api;
    return () => {
      if (appApi === api) {
        appApi = undefined;
      }
    };
  }, [api]);

  return <>{children}</>;
}
