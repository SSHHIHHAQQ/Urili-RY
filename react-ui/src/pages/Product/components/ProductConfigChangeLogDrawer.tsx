import { ProTable, type ProColumns } from '@ant-design/pro-components';
import { Modal, Table, Tag, Typography } from 'antd';
import { getProductConfigChangeLogList } from '@/services/product/product';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

type ProductConfigChangeLogDrawerProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  bizType?: string;
  bizTypes?: string[];
  bizId?: number;
  title?: string;
};

const bizTypeText: Record<string, string> = {
  CATEGORY: '商品分类',
  ATTRIBUTE: '商品属性',
  ATTRIBUTE_OPTION: '属性选项',
  CATEGORY_ATTRIBUTE_RULE: '类目属性规则',
};

const actionText: Record<string, string> = {
  CREATE: '新增',
  UPDATE: '修改',
  ENABLE: '启用',
  DISABLE: '停用',
  DELETE: '删除',
};

const actionColor: Record<string, string> = {
  CREATE: 'green',
  UPDATE: 'blue',
  ENABLE: 'green',
  DISABLE: 'orange',
  DELETE: 'red',
};

const sourceText: Record<string, string> = {
  PAGE: '页面',
  IMPORT: '导入',
};

function toValueEnum(source: Record<string, string>) {
  return Object.entries(source).reduce<Record<string, { text: string }>>(
    (valueEnum, [value, text]) => {
      valueEnum[value] = { text };
      return valueEnum;
    },
    {},
  );
}

function parseDiff(diffJson?: string): API.Product.ConfigChangeDiffItem[] {
  if (!diffJson) {
    return [];
  }
  try {
    const parsed = JSON.parse(diffJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function textValue(value?: string | number | boolean) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function buildBizTypeValueEnum(bizType?: string, bizTypes?: string[]) {
  const allowedTypes = bizTypes?.length ? bizTypes : bizType ? [bizType] : [];
  if (allowedTypes.length === 0) {
    return toValueEnum(bizTypeText);
  }
  return allowedTypes.reduce<Record<string, { text: string }>>((valueEnum, item) => {
    valueEnum[item] = { text: bizTypeText[item] || item };
    return valueEnum;
  }, {});
}

export default function ProductConfigChangeLogDrawer({
  open,
  onOpenChange,
  bizType,
  bizTypes,
  bizId,
  title,
}: ProductConfigChangeLogDrawerProps) {
  const scopedBizTypes = bizTypes?.filter(Boolean);
  const bizTypeValueEnum = buildBizTypeValueEnum(bizType, scopedBizTypes);
  const actionValueEnum = toValueEnum(actionText);
  const sourceValueEnum = toValueEnum(sourceText);

  const columns: ProColumns<API.Product.ConfigChangeLog>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: {
        placeholder: '对象名称/编码/摘要',
      },
    },
    {
      title: '操作时间',
      dataIndex: 'changeTimeRange',
      valueType: 'dateTimeRange',
      hideInTable: true,
      search: {
        transform: (value) => ({
          beginTime: value?.[0],
          endTime: value?.[1],
        }),
      },
    },
    {
      title: '变更时间',
      dataIndex: 'changeTime',
      width: 170,
      search: false,
    },
    {
      title: '对象类型',
      dataIndex: 'bizType',
      valueType: 'select',
      valueEnum: bizTypeValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 120,
      search: !bizType || Boolean(scopedBizTypes?.length && scopedBizTypes.length > 1),
      renderText: (value) => bizTypeText[value] || value || '-',
    },
    {
      title: '对象名称',
      dataIndex: 'bizName',
      width: 160,
      search: false,
      ellipsis: true,
      renderText: (value) => value || '-',
    },
    {
      title: '对象编码',
      dataIndex: 'bizCode',
      width: 170,
      search: false,
      ellipsis: true,
      renderText: (value) => value || '-',
    },
    {
      title: '操作类型',
      dataIndex: 'actionType',
      valueType: 'select',
      valueEnum: actionValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 100,
      render: (_, record) => (
        <Tag color={actionColor[record.actionType || ''] || 'default'}>
          {actionText[record.actionType || ''] || record.actionType || '-'}
        </Tag>
      ),
    },
    {
      title: '来源',
      dataIndex: 'actionSource',
      valueType: 'select',
      valueEnum: sourceValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 90,
      renderText: (value) => sourceText[value] || value || '-',
    },
    {
      title: '操作人',
      dataIndex: 'operatorName',
      width: 110,
      renderText: (value) => value || '-',
    },
    {
      title: '变更摘要',
      dataIndex: 'changeSummary',
      search: false,
      ellipsis: true,
    },
  ];

  return (
    <Modal
      title={`操作日志${title ? `：${title}` : ''}`}
      open={open}
      width={1180}
      footer={null}
      centered
      destroyOnHidden
      styles={{
        body: {
          maxHeight: '72vh',
          overflow: 'auto',
          paddingTop: 8,
        },
      }}
      onCancel={() => onOpenChange(false)}
    >
      <ProTable<API.Product.ConfigChangeLog>
        rowKey="logId"
        columns={columns}
        size="small"
        options={false}
        pagination={{ pageSize: 10 }}
        search={{
          labelWidth: 80,
          span: 8,
          defaultCollapsed: false,
        }}
        scroll={{ x: 1180 }}
        request={async (params) => {
          const requestParams: Record<string, any> = {
            ...params,
            pageNum: params.current,
            pageSize: params.pageSize,
          };
          delete requestParams.current;
          if (bizType && !requestParams.bizType) {
            requestParams.bizType = bizType;
          }
          if (scopedBizTypes?.length) {
            requestParams.bizTypes = scopedBizTypes.join(',');
          }
          if (bizId) {
            requestParams.bizId = bizId;
          }
          const resp = await getProductConfigChangeLogList(requestParams);
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
        expandable={{
          expandedRowRender: (record) => {
            const diff = parseDiff(record.diffJson);
            if (diff.length === 0) {
              return <Typography.Text type="secondary">无字段差异</Typography.Text>;
            }
            return (
              <Table<API.Product.ConfigChangeDiffItem>
                size="small"
                rowKey={(item) =>
                  item.field || `${item.fieldLabel}-${item.beforeValue}-${item.afterValue}`
                }
                pagination={false}
                dataSource={diff}
                columns={[
                  {
                    title: '字段',
                    dataIndex: 'fieldLabel',
                    width: 180,
                    render: (value) => value || '-',
                  },
                  {
                    title: '修改前',
                    dataIndex: 'beforeValue',
                    render: (value) => textValue(value),
                  },
                  {
                    title: '修改后',
                    dataIndex: 'afterValue',
                    render: (value) => textValue(value),
                  },
                ]}
              />
            );
          },
        }}
      />
    </Modal>
  );
}
