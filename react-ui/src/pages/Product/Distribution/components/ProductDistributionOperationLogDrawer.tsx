import { ProTable, type ProColumns } from '@ant-design/pro-components';
import { Modal, Table, Tag, Typography } from 'antd';
import { getDistributionOperationLogList } from '@/services/product/distributionProduct';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';
import {
  getControlStatusText,
  getSalesStatusText,
  productOperationTypeColor,
  productOperationTypeText,
} from '../constants';

type ProductDistributionOperationLogDrawerProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

type DiffItem = {
  field?: string;
  before?: string | number;
  after?: string | number;
  beforeValue?: string | number;
  afterValue?: string | number;
};

const ownerTypeText: Record<string, string> = {
  SPU: 'SPU',
  SKU: 'SKU',
};

const operationTypeValueEnum = Object.entries(productOperationTypeText).reduce<
  Record<string, { text: string }>
>((valueEnum, [value, text]) => {
  valueEnum[value] = { text };
  return valueEnum;
}, {});

const ownerTypeValueEnum = Object.entries(ownerTypeText).reduce<Record<string, { text: string }>>(
  (valueEnum, [value, text]) => {
    valueEnum[value] = { text };
    return valueEnum;
  },
  {},
);

function parseDiff(diffJson?: string): DiffItem[] {
  if (!diffJson) return [];
  try {
    const parsed = JSON.parse(diffJson);
    return Array.isArray(parsed) ? parsed : [parsed];
  } catch {
    return [];
  }
}

function renderDiffValue(field?: string, value?: string | number) {
  if (value === undefined || value === null || value === '') return '-';
  if (field === 'spuStatus' || field === 'skuStatus') return getSalesStatusText(String(value));
  if (field === 'controlStatus') return getControlStatusText(String(value));
  return String(value);
}

function fieldLabel(field?: string) {
  const labels: Record<string, string> = {
    spuStatus: 'SPU销售状态',
    skuStatus: 'SKU销售状态',
    controlStatus: '管控状态',
    salePrice: '销售价',
  };
  return (field && labels[field]) || field || '-';
}

export default function ProductDistributionOperationLogDrawer({
  open,
  onOpenChange,
}: ProductDistributionOperationLogDrawerProps) {
  const columns: ProColumns<API.ProductDistribution.OperationLog>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: {
        placeholder: '系统编码/卖家/摘要',
      },
    },
    {
      title: '操作时间',
      dataIndex: 'operationTimeRange',
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
      title: '操作时间',
      dataIndex: 'operationTime',
      search: false,
      width: 170,
    },
    {
      title: '对象',
      dataIndex: 'ownerType',
      valueType: 'select',
      valueEnum: ownerTypeValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 90,
      renderText: (value) => ownerTypeText[value] || value || '-',
    },
    {
      title: '系统SPU',
      dataIndex: 'systemSpuCode',
      search: false,
      width: 160,
      ellipsis: true,
    },
    {
      title: '系统SKU',
      dataIndex: 'systemSkuCode',
      search: false,
      width: 160,
      ellipsis: true,
      renderText: (value) => value || '-',
    },
    {
      title: '操作类型',
      dataIndex: 'operationType',
      valueType: 'select',
      valueEnum: operationTypeValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 130,
      render: (_, record) => (
        <Tag color={productOperationTypeColor[record.operationType || ''] || 'default'}>
          {productOperationTypeText[record.operationType || ''] || record.operationType || '-'}
        </Tag>
      ),
    },
    {
      title: '卖家',
      dataIndex: 'sellerName',
      search: false,
      width: 150,
      ellipsis: true,
      renderText: (value) => value || '-',
    },
    {
      title: '操作人',
      dataIndex: 'operatorName',
      width: 110,
      renderText: (value) => value || '-',
    },
    {
      title: '摘要',
      dataIndex: 'changeSummary',
      search: false,
      width: 240,
      ellipsis: true,
      renderText: (value) => value || '-',
    },
    {
      title: '原因',
      dataIndex: 'reason',
      search: false,
      width: 180,
      ellipsis: true,
      renderText: (value) => value || '-',
    },
  ];

  return (
    <Modal
      title="操作日志"
      open={open}
      width={1260}
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
      <ProTable<API.ProductDistribution.OperationLog>
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
        scroll={{ x: 1260 }}
        request={async (params) => {
          const requestParams: Record<string, any> = {
            ...params,
            pageNum: params.current,
            pageSize: params.pageSize,
          };
          delete requestParams.current;
          const resp = await getDistributionOperationLogList(requestParams);
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
              <Table<DiffItem>
                size="small"
                rowKey={(item) => item.field || `${item.before}-${item.after}`}
                pagination={false}
                dataSource={diff}
                columns={[
                  {
                    title: '字段',
                    dataIndex: 'field',
                    width: 180,
                    render: (value) => fieldLabel(value),
                  },
                  {
                    title: '修改前',
                    width: 260,
                    render: (_, item) =>
                      renderDiffValue(item.field, item.before ?? item.beforeValue),
                  },
                  {
                    title: '修改后',
                    width: 260,
                    render: (_, item) =>
                      renderDiffValue(item.field, item.after ?? item.afterValue),
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
