import { jsx as _jsx } from "react/jsx-runtime";
import { Tag } from 'antd';
import { message } from '@/utils/feedback';
import { connectionStatusText, credentialStatusText, pairingStatusText, skuSyncStateText, syncItemStatusText, } from './constants';
export const statusTag = (value) => {
    const color = value === 'ENABLED' ||
        value === 'ACTIVE' ||
        value === 'PAIRED' ||
        value === 'FRESH' ||
        value === 'CONFIGURED'
        ? 'green'
        : value === 'SYNCING'
            ? 'blue'
            : value === 'FAILED' || value === 'INVALID'
                ? 'red'
                : value === 'MISSING'
                    ? 'orange'
                    : 'default';
    return (_jsx(Tag, { color: color, children: connectionStatusText[value || ''] ||
            credentialStatusText[value || ''] ||
            syncItemStatusText[value || ''] ||
            pairingStatusText[value || ''] ||
            skuSyncStateText[value || ''] ||
            value ||
            '-' }));
};
export const resultOk = (resp, successText) => {
    if (resp?.code === 200) {
        message.success(successText);
        return true;
    }
    message.error(resp?.msg || '操作失败');
    return false;
};
