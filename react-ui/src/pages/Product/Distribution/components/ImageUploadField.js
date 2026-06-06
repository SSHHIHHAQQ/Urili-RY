import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { DeleteOutlined, EyeOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Image, Upload } from 'antd';
import { useState } from 'react';
import { uploadCommonFile } from '@/services/common/file';
import { message } from '@/utils/feedback';
import { resolveResourceUrl } from '../constants';
import styles from '../style.module.css';
export default function ImageUploadField({ value, onChange, label, required = false, size = 'normal', reserveLabelSpace = false, }) {
    const [previewOpen, setPreviewOpen] = useState(false);
    const imageUrl = resolveResourceUrl(value);
    const uploadProps = {
        accept: 'image/*',
        showUploadList: false,
        customRequest: async ({ file, onError, onSuccess }) => {
            try {
                const resp = await uploadCommonFile(file);
                if (resp.code !== 200) {
                    throw new Error(resp.msg || '上传失败');
                }
                const url = resp.url || resp.fileName || resp.newFileName;
                if (!url) {
                    throw new Error('上传响应缺少资源路径');
                }
                onChange?.(url);
                onSuccess?.(resp);
            }
            catch (error) {
                message.error(error instanceof Error ? error.message : '上传失败');
                onError?.(error);
            }
        },
    };
    return (_jsxs("div", { className: `${styles.imageSlotWrap} ${size === 'small' ? styles.imageSlotSmall : ''}`, children: [label ? (_jsxs("div", { className: styles.imageSlotLabel, children: [label, required ? _jsx("span", { children: "*" }) : null] })) : reserveLabelSpace ? _jsx("div", { className: styles.imageSlotLabelPlaceholder }) : null, _jsx(Upload, { ...uploadProps, children: _jsx("div", { className: `${styles.imageSlot} ${value ? styles.imageSlotFilled : ''}`, children: value ? (_jsxs(_Fragment, { children: [_jsx("img", { src: imageUrl, alt: label || '商品图片' }), _jsxs("div", { className: styles.imageSlotMask, children: [_jsx(Button, { size: "small", type: "text", icon: _jsx(EyeOutlined, {}), onClick: (event) => {
                                            event.stopPropagation();
                                            setPreviewOpen(true);
                                        } }), _jsx(Button, { size: "small", type: "text", icon: _jsx(UploadOutlined, {}) }), _jsx(Button, { size: "small", type: "text", danger: true, icon: _jsx(DeleteOutlined, {}), onClick: (event) => {
                                            event.stopPropagation();
                                            onChange?.(undefined);
                                        } })] })] })) : (_jsx(PlusOutlined, { className: styles.imageSlotPlus })) }) }), value ? (_jsx(Image, { src: imageUrl, style: { display: 'none' }, preview: { open: previewOpen, onOpenChange: setPreviewOpen } })) : null] }));
}
