import { InboxOutlined } from '@ant-design/icons';
import { Alert, Button, Checkbox, Modal, Space, Table, Tag, Upload } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { message } from '@/utils/feedback';

type ProductImportModalProps = {
  open: boolean;
  title: string;
  onOpenChange: (open: boolean) => void;
  onDownloadTemplate: () => Promise<unknown>;
  onPreview: (
    file: File,
    updateSupport: boolean,
  ) => Promise<API.Product.ImportResultResponse>;
  onImport: (
    file: File,
    updateSupport: boolean,
  ) => Promise<API.Product.ImportResultResponse>;
  onSuccess?: () => void;
};

const actionText: Record<string, string> = {
  CREATE: '新增',
  UPDATE: '更新',
  SKIP: '跳过',
  ERROR: '错误',
};

function isImportResult(value: unknown): value is API.Product.ImportResult {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const result = value as API.Product.ImportResult;
  return (
    typeof result.totalCount === 'number' ||
    typeof result.errorCount === 'number' ||
    Array.isArray(result.messages)
  );
}

function getErrorImportResult(error: any) {
  const candidates = [
    error?.info?.data,
    error?.info?.data?.data,
    error?.data,
    error?.data?.data,
    error?.response?.data?.data,
    error?.response?.data,
  ];
  return candidates.find(isImportResult) as API.Product.ImportResult | undefined;
}

function getErrorMessage(error: any, fallback: string) {
  return (
    error?.info?.errorMessage ||
    error?.response?.data?.msg ||
    error?.data?.msg ||
    error?.message ||
    fallback
  );
}

export default function ProductImportModal({
  open,
  title,
  onOpenChange,
  onDownloadTemplate,
  onPreview,
  onImport,
  onSuccess,
}: ProductImportModalProps) {
  const [file, setFile] = useState<File>();
  const [updateSupport, setUpdateSupport] = useState(true);
  const [previewResult, setPreviewResult] = useState<API.Product.ImportResult>();
  const [previewing, setPreviewing] = useState(false);
  const [importing, setImporting] = useState(false);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!open) {
      setFile(undefined);
      setPreviewResult(undefined);
      setUpdateSupport(true);
    }
  }, [open]);

  const canImport = useMemo(
    () =>
      !!file &&
      !!previewResult &&
      (previewResult.totalCount || 0) > 0 &&
      (previewResult.errorCount || 0) === 0,
    [file, previewResult],
  );

  const handleDownloadTemplate = async () => {
    setDownloading(true);
    try {
      await onDownloadTemplate();
    } finally {
      setDownloading(false);
    }
  };

  const handlePreview = async () => {
    if (!file) {
      message.warning('请选择导入文件');
      return;
    }
    setPreviewing(true);
    setPreviewResult(undefined);
    try {
      const resp = await onPreview(file, updateSupport);
      if (!resp.data) {
        message.warning(resp.msg || '导入校验没有返回结果');
        return;
      }
      setPreviewResult(resp.data);
      if ((resp.data.errorCount || 0) > 0) {
        message.warning(resp.msg || '导入校验未通过');
      } else {
        message.success(resp.msg || '导入校验通过');
      }
    } catch (error) {
      const result = getErrorImportResult(error);
      if (result) {
        setPreviewResult(result);
      }
      message.warning(getErrorMessage(error, '导入校验失败'));
    } finally {
      setPreviewing(false);
    }
  };

  const handleImport = async () => {
    if (!file) {
      message.warning('请选择导入文件');
      return;
    }
    if (!canImport) {
      message.warning('请先完成校验并修正错误');
      return;
    }
    setImporting(true);
    try {
      const resp = await onImport(file, updateSupport);
      if (!resp.data) {
        message.warning(resp.msg || '导入没有返回结果');
        return;
      }
      setPreviewResult(resp.data);
      if ((resp.data.errorCount || 0) > 0) {
        message.warning(resp.msg || '导入校验未通过');
        return;
      }
      message.success(resp.msg || '导入完成');
      onSuccess?.();
      onOpenChange(false);
    } catch (error) {
      const result = getErrorImportResult(error);
      if (result) {
        setPreviewResult(result);
      }
      message.warning(getErrorMessage(error, '导入失败'));
    } finally {
      setImporting(false);
    }
  };

  const summaryText = previewResult
    ? `共 ${previewResult.totalCount || 0} 行，新增 ${
        previewResult.createCount || 0
      } 行，更新 ${previewResult.updateCount || 0} 行，错误 ${
        previewResult.errorCount || 0
      } 行`
    : '请先下载模板，填写后上传 Excel 文件并校验。';
  const alertType = previewResult
    ? (previewResult.errorCount || 0) > 0
      ? 'warning'
      : 'success'
    : 'info';

  return (
    <Modal
      title={title}
      open={open}
      onCancel={() => onOpenChange(false)}
      width={780}
      destroyOnHidden
      footer={[
        <Button key="template" loading={downloading} onClick={handleDownloadTemplate}>
          下载模板
        </Button>,
        <Button key="preview" loading={previewing} onClick={handlePreview}>
          校验
        </Button>,
        <Button
          key="import"
          type="primary"
          disabled={!canImport}
          loading={importing}
          onClick={handleImport}
        >
          确认导入
        </Button>,
      ]}
    >
      <Space orientation="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          type={alertType}
          showIcon
          title={summaryText}
        />
        <Upload.Dragger
          accept=".xls,.xlsx"
          maxCount={1}
          fileList={
            file
              ? [
                  {
                    uid: 'product-import-file',
                    name: file.name,
                    status: 'done',
                  },
                ]
              : []
          }
          beforeUpload={(selectedFile) => {
            if (!/\.(xls|xlsx)$/i.test(selectedFile.name)) {
              message.warning('只能上传 Excel 文件');
              return Upload.LIST_IGNORE;
            }
            setFile(selectedFile);
            setPreviewResult(undefined);
            return false;
          }}
          onRemove={() => {
            setFile(undefined);
            setPreviewResult(undefined);
          }}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">选择或拖入 Excel 文件</p>
        </Upload.Dragger>
        <Checkbox
          checked={updateSupport}
          onChange={(event) => {
            setUpdateSupport(event.target.checked);
            setPreviewResult(undefined);
          }}
        >
          已存在时更新
        </Checkbox>
        {previewResult?.messages?.length ? (
          <Table<API.Product.ImportMessage>
            size="small"
            rowKey={(record) =>
              `${record.rowNum || 0}-${record.action || ''}-${
                record.status || ''
              }-${record.message || ''}`
            }
            dataSource={previewResult.messages}
            pagination={previewResult.messages.length > 6 ? { pageSize: 6 } : false}
            columns={[
              {
                title: '行号',
                dataIndex: 'rowNum',
                width: 80,
              },
              {
                title: '动作',
                dataIndex: 'action',
                width: 90,
                render: (value: string) => actionText[value] || value,
              },
              {
                title: '状态',
                dataIndex: 'status',
                width: 90,
                render: (value: string) => (
                  <Tag color={value === 'ERROR' ? 'red' : value === 'WARN' ? 'orange' : 'green'}>
                    {value === 'ERROR' ? '错误' : value === 'WARN' ? '提醒' : '通过'}
                  </Tag>
                ),
              },
              {
                title: '说明',
                dataIndex: 'message',
              },
            ]}
          />
        ) : null}
      </Space>
    </Modal>
  );
}
