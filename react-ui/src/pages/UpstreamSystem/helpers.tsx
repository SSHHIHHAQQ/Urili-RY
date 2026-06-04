import { message } from '@/utils/feedback';
import { Tag } from 'antd';
import { connectionStatusText, pairingStatusText, syncItemStatusText } from './constants';

export const statusTag = (value?: string) => {
  const color = value === 'ENABLED' || value === 'ACTIVE' || value === 'PAIRED' ? 'green' : 'default';
  return <Tag color={color}>{connectionStatusText[value || ''] || syncItemStatusText[value || ''] || pairingStatusText[value || ''] || value || '-'}</Tag>;
};

export const resultOk = (resp: API.Result | undefined, successText: string) => {
  if (resp?.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp?.msg || '操作失败');
  return false;
};
