import { DeleteOutlined, EyeOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Image, Upload } from 'antd';
import type { UploadProps } from 'antd';
import { useState } from 'react';
import { uploadCommonFile } from '@/services/common/file';
import { message } from '@/utils/feedback';
import { resolveResourceUrl } from '../constants';
import styles from '../style.module.css';

type ImageUploadFieldProps = {
  value?: string;
  onChange?: (value?: string) => void;
  label?: string;
  required?: boolean;
  size?: 'small' | 'normal';
  reserveLabelSpace?: boolean;
};

export default function ImageUploadField({
  value,
  onChange,
  label,
  required = false,
  size = 'normal',
  reserveLabelSpace = false,
}: ImageUploadFieldProps) {
  const [previewOpen, setPreviewOpen] = useState(false);
  const imageUrl = resolveResourceUrl(value);
  const uploadProps: UploadProps = {
    accept: 'image/*',
    showUploadList: false,
    customRequest: async ({ file, onError, onSuccess }) => {
      try {
        const resp = await uploadCommonFile(file as File);
        if (resp.code !== 200) {
          throw new Error(resp.msg || '上传失败');
        }
        const url = resp.fileName || resp.url || resp.newFileName;
        if (!url) {
          throw new Error('上传响应缺少资源路径');
        }
        onChange?.(url);
        onSuccess?.(resp);
      } catch (error) {
        message.error(error instanceof Error ? error.message : '上传失败');
        onError?.(error as Error);
      }
    },
  };

  return (
    <div className={`${styles.imageSlotWrap} ${size === 'small' ? styles.imageSlotSmall : ''}`}>
      {label ? (
        <div className={styles.imageSlotLabel}>
          {label}
          {required ? <span>*</span> : null}
        </div>
      ) : reserveLabelSpace ? <div className={styles.imageSlotLabelPlaceholder} /> : null}
      <Upload {...uploadProps}>
        <div className={`${styles.imageSlot} ${value ? styles.imageSlotFilled : ''}`}>
          {value ? (
            <>
              <img src={imageUrl} alt={label || '商品图片'} />
              <div className={styles.imageSlotMask}>
                <Button
                  size="small"
                  type="text"
                  icon={<EyeOutlined />}
                  onClick={(event) => {
                    event.stopPropagation();
                    setPreviewOpen(true);
                  }}
                />
                <Button size="small" type="text" icon={<UploadOutlined />} />
                <Button
                  size="small"
                  type="text"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={(event) => {
                    event.stopPropagation();
                    onChange?.(undefined);
                  }}
                />
              </div>
            </>
          ) : (
            <PlusOutlined className={styles.imageSlotPlus} />
          )}
        </div>
      </Upload>
      {value ? (
        <Image
          src={imageUrl}
          style={{ display: 'none' }}
          preview={{ open: previewOpen, onOpenChange: setPreviewOpen }}
        />
      ) : null}
    </div>
  );
}
