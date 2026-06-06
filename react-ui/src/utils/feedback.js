import { Fragment as _Fragment, jsx as _jsx } from "react/jsx-runtime";
import { App, Modal as staticModal, message as staticMessage, notification as staticNotification } from 'antd';
import { useEffect } from 'react';
let appApi;
function bindApi(getApi) {
    return new Proxy({}, {
        get(_, property) {
            const api = getApi();
            const value = api[property];
            return typeof value === 'function' ? value.bind(api) : value;
        },
    });
}
export const message = bindApi(() => appApi?.message ?? staticMessage);
export const notification = bindApi(() => appApi?.notification ?? staticNotification);
export const modal = bindApi(() => appApi?.modal ?? staticModal);
export function AntdFeedbackProvider({ children }) {
    const api = App.useApp();
    useEffect(() => {
        appApi = api;
        return () => {
            if (appApi === api) {
                appApi = undefined;
            }
        };
    }, [api]);
    return _jsx(_Fragment, { children: children });
}
