import { DownOutlined, SettingOutlined } from '@ant-design/icons';
import {
  PageContainer,
  type ActionType,
  type ProColumns,
  ProTable,
} from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  Button,
  DatePicker,
  Descriptions,
  Drawer,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useRef, useState } from 'react';
import {
  changeInventoryAdjustmentReviewEffectiveTime,
  effectNowInventoryAdjustmentReview,
  getInventoryAdjustmentReview,
  getInventoryAdjustmentReviewList,
  getInventoryAdjustmentReviewLogs,
  getInventoryAdjustmentReviewPolicyBindingList,
  getInventoryAdjustmentReviewPolicyList,
  rejectInventoryAdjustmentReview,
  saveInventoryAdjustmentReviewPolicy,
  saveInventoryAdjustmentReviewPolicyBinding,
} from '@/services/inventory/adjustmentReview';
import {
  getPersistedProTableSearch,
  getProTablePagination,
  getProTableScroll,
} from '@/utils/proTableSearch';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

type ActionKind = 'EFFECT' | 'REJECT' | 'TIME';
type ReviewActionState = {
  open: boolean;
  kind: ActionKind;
  review?: API.InventoryAdjustmentReview.Review;
};

type PolicyFormState = {
  open: boolean;
  record?: API.InventoryAdjustmentReview.Policy;
};

type BindingFormState = {
  open: boolean;
  record?: API.InventoryAdjustmentReview.PolicyBinding;
};

const TABLE_SCROLL_X = 2060;

const reviewStatusValueEnum = {
  WAITING: { text: '等待生效', status: 'warning' },
  EFFECTIVE: { text: '已生效', status: 'success' },
  REJECTED: { text: '已驳回', status: 'error' },
};

const adjustDirectionValueEnum = {
  DECREASE: { text: '退回库存', status: 'warning' },
  INCREASE: { text: '增加库存', status: 'processing' },
};

const reviewModeValueEnum = {
  DISABLED: { text: '不审核' },
  CONDITIONAL: { text: '按门槛审核' },
  ALWAYS: { text: '强制审核' },
};

const directionScopeValueEnum = {
  DECREASE: { text: '仅退回' },
  INCREASE: { text: '仅增加' },
  BOTH: { text: '增减都审' },
};

const enabledValueEnum = {
  ENABLED: { text: '启用', status: 'success' },
  DISABLED: { text: '停用', status: 'default' },
};

const bindingTypeValueEnum = {
  GLOBAL: { text: '全局' },
  SELLER: { text: '卖家' },
};

const operationTypeText: Record<string, string> = {
  SUBMIT: '提交',
  EFFECT_NOW: '生效',
  CHANGE_EFFECTIVE_TIME: '调整时间',
  REJECT: '驳回',
};

function qty(value?: number | null) {
  return Number(value || 0);
}

function formatQuantity(value?: number | null) {
  if (value === undefined || value === null) {
    return '--';
  }
  return new Intl.NumberFormat('zh-CN').format(Number(value));
}

function formatDecimal(value?: number | null) {
  if (value === undefined || value === null) {
    return '--';
  }
  return Number(value).toFixed(2);
}

function formatDateTime(value?: string | null) {
  return value || '--';
}

function renderStatus(value?: string) {
  const item = reviewStatusValueEnum[value as keyof typeof reviewStatusValueEnum];
  return <Tag color={item?.status === 'success' ? 'green' : item?.status === 'error' ? 'red' : 'gold'}>{item?.text || value || '--'}</Tag>;
}

function renderDirection(value?: string) {
  const item = adjustDirectionValueEnum[value as keyof typeof adjustDirectionValueEnum];
  return <Tag color={value === 'DECREASE' ? 'orange' : 'blue'}>{item?.text || value || '--'}</Tag>;
}

function rangeParams(params: Record<string, any>) {
  const { submitTimeRange, plannedEffectiveTimeRange, ...rest } = params;
  return {
    ...rest,
    submitTimeStart: submitTimeRange?.[0],
    submitTimeEnd: submitTimeRange?.[1],
    plannedEffectiveTimeStart: plannedEffectiveTimeRange?.[0],
    plannedEffectiveTimeEnd: plannedEffectiveTimeRange?.[1],
  };
}

function resultOk(resp: { code: number; msg?: string }, successText: string) {
  if (resp.code === 200) {
    message.success(successText);
    return true;
  }
  message.error(resp.msg || '操作失败');
  return false;
}

function validateSalesWindowText(value?: string) {
  const days = String(value || '')
    .replace(/\[/g, '')
    .replace(/\]/g, '')
    .replace(/"/g, '')
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isInteger(item) && item > 0);
  return days.length > 0;
}

export default function InventoryAdjustmentReviewPage() {
  const access = useAccess();
  const canList = access.hasPerms('review:inventoryAdjustment:list');
  const canQuery = access.hasPerms('review:inventoryAdjustment:query');
  const canEffect = access.hasPerms('review:inventoryAdjustment:effect');
  const canEdit = access.hasPerms('review:inventoryAdjustment:edit');
  const canReject = access.hasPerms('review:inventoryAdjustment:reject');
  const canLog = access.hasPerms('review:inventoryAdjustment:log');
  const canConfig = access.hasPerms('review:inventoryAdjustment:config');
  const actionRef = useRef<ActionType>(null);
  const policyActionRef = useRef<ActionType>(null);
  const bindingActionRef = useRef<ActionType>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [currentReview, setCurrentReview] = useState<API.InventoryAdjustmentReview.Review>();
  const [logs, setLogs] = useState<API.InventoryAdjustmentReview.OperationLog[]>([]);
  const [configOpen, setConfigOpen] = useState(false);
  const [actionState, setActionState] = useState<ReviewActionState>({ open: false, kind: 'EFFECT' });
  const [actionForm] = Form.useForm();
  const [policyForm] = Form.useForm();
  const [bindingForm] = Form.useForm();
  const [policyState, setPolicyState] = useState<PolicyFormState>({ open: false });
  const [bindingState, setBindingState] = useState<BindingFormState>({ open: false });

  const reload = () => actionRef.current?.reload();

  const openDetail = async (record: API.InventoryAdjustmentReview.Review) => {
    if (!canQuery) {
      message.warning('缺少库存调整审核查询权限');
      return;
    }
    const resp = await getInventoryAdjustmentReview(record.reviewId!);
    if (resp.code === 200) {
      setCurrentReview(resp.data);
      setDetailOpen(true);
      if (canLog) {
        const logResp = await getInventoryAdjustmentReviewLogs(record.reviewId!);
        setLogs(logResp.code === 200 ? logResp.data || [] : []);
      } else {
        setLogs([]);
      }
    } else {
      message.error(resp.msg || '读取审核单失败');
    }
  };

  const openAction = (kind: ActionKind, review: API.InventoryAdjustmentReview.Review) => {
    setActionState({ open: true, kind, review });
    actionForm.resetFields();
  };

  const submitAction = async () => {
    const review = actionState.review;
    if (!review?.reviewId) {
      return;
    }
    const values = await actionForm.validateFields();
    const reason = values.reason?.trim();
    let resp: API.InventoryAdjustmentReview.InfoResult;
    if (actionState.kind === 'EFFECT') {
      resp = await effectNowInventoryAdjustmentReview(review.reviewId, reason);
    } else if (actionState.kind === 'REJECT') {
      resp = await rejectInventoryAdjustmentReview(review.reviewId, reason);
    } else {
      resp = await changeInventoryAdjustmentReviewEffectiveTime(review.reviewId, {
        reason,
        plannedEffectiveTime: values.plannedEffectiveTime?.format('YYYY-MM-DD HH:mm:ss'),
      });
    }
    if (resultOk(resp, actionState.kind === 'TIME' ? '计划生效时间已调整' : '审核单已处理')) {
      setActionState({ open: false, kind: 'EFFECT' });
      actionForm.resetFields();
      reload();
    }
  };

  const openPolicyForm = (record?: API.InventoryAdjustmentReview.Policy) => {
    setPolicyState({ open: true, record });
    policyForm.setFieldsValue({
      policyStatus: 'ENABLED',
      reviewMode: 'CONDITIONAL',
      directionScope: 'DECREASE',
      fieldScope: 'PLATFORM_TOTAL',
      salesWindowDays: '[7,30]',
      salesAggregateMode: 'MAX_DAILY_AVG',
      reserveDays: 7,
      cooldownHours: 168,
      autoEffectEnabled: 'Y',
      manualEffectAllowed: 'Y',
      ...record,
    });
  };

  const submitPolicy = async () => {
    const values = await policyForm.validateFields();
    const resp = await saveInventoryAdjustmentReviewPolicy({
      ...policyState.record,
      ...values,
    });
    if (resultOk(resp, '策略已保存')) {
      setPolicyState({ open: false });
      policyForm.resetFields();
      policyActionRef.current?.reload();
    }
  };

  const openBindingForm = (record?: API.InventoryAdjustmentReview.PolicyBinding) => {
    setBindingState({ open: true, record });
    bindingForm.setFieldsValue({
      bindingType: 'SELLER',
      priority: 100,
      status: 'ENABLED',
      ...record,
    });
  };

  const submitBinding = async () => {
    const values = await bindingForm.validateFields();
    const resp = await saveInventoryAdjustmentReviewPolicyBinding({
      ...bindingState.record,
      ...values,
    });
    if (resultOk(resp, '策略绑定已保存')) {
      setBindingState({ open: false });
      bindingForm.resetFields();
      bindingActionRef.current?.reload();
    }
  };

  const columns: ProColumns<API.InventoryAdjustmentReview.Review>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '审核单号 / SKU / 商品 / 仓库' },
    },
    {
      title: '状态',
      dataIndex: 'reviewStatus',
      valueType: 'select',
      valueEnum: reviewStatusValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 110,
      render: (_, record) => renderStatus(record.reviewStatus),
    },
    {
      title: '方向',
      dataIndex: 'adjustDirection',
      valueType: 'select',
      valueEnum: adjustDirectionValueEnum,
      fieldProps: SEARCHABLE_SELECT_PROPS,
      width: 110,
      render: (_, record) => renderDirection(record.adjustDirection),
    },
    {
      title: '提交时间',
      dataIndex: 'submitTimeRange',
      valueType: 'dateTimeRange',
      hideInTable: true,
    },
    {
      title: '计划生效',
      dataIndex: 'plannedEffectiveTimeRange',
      valueType: 'dateTimeRange',
      hideInTable: true,
    },
    {
      title: '审核单',
      dataIndex: 'reviewNo',
      width: 190,
      fixed: 'left',
      search: false,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{record.reviewNo || '--'}</Typography.Text>
          <Typography.Text type="secondary">{formatDateTime(record.submitTime)}</Typography.Text>
        </Space>
      ),
    },
    {
      title: 'SKU / 商品',
      dataIndex: 'systemSkuCode',
      width: 260,
      search: false,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{record.systemSkuCode || '--'}</Typography.Text>
          <Typography.Text type="secondary" ellipsis={{ tooltip: record.productName || record.skuName }}>
            {record.productName || record.skuName || '--'}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '卖家',
      dataIndex: 'sellerId',
      width: 100,
      search: false,
      render: (value) => value || '--',
    },
    {
      title: '仓库',
      dataIndex: 'warehouseName',
      width: 160,
      search: false,
      ellipsis: true,
    },
    {
      title: '申请退回/调整',
      dataIndex: 'requestedAdjustQty',
      width: 130,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '申请前库存',
      dataIndex: 'requestBeforePlatformTotalQty',
      width: 120,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '申请后库存',
      dataIndex: 'requestExpectedAfterPlatformTotalQty',
      width: 120,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '可立即退回',
      dataIndex: 'immediateReturnableQty',
      width: 120,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '保护保留',
      dataIndex: 'protectedRetainedQty',
      width: 120,
      align: 'right',
      search: false,
      render: (value) => formatQuantity(value as number),
    },
    {
      title: '阈值日销',
      dataIndex: 'thresholdDailyAvg',
      width: 110,
      align: 'right',
      search: false,
      render: (value) => formatDecimal(value as number),
    },
    {
      title: '计划生效时间',
      dataIndex: 'plannedEffectiveTime',
      width: 170,
      search: false,
      render: (value) => formatDateTime(value as string),
    },
    {
      title: '实际生效',
      dataIndex: 'actualEffectQty',
      width: 120,
      align: 'right',
      search: false,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{record.actualEffectQty == null ? '--' : formatQuantity(record.actualEffectQty)}</Typography.Text>
          {qty(record.unfulfilledQty) > 0 ? (
            <Typography.Text type="danger">未满足 {formatQuantity(record.unfulfilledQty)}</Typography.Text>
          ) : null}
        </Space>
      ),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 170,
      fixed: 'right',
      render: (_, record) => {
        const moreItems = [
          canEdit && record.reviewStatus === 'WAITING'
            ? { key: 'time', label: '调整时间', onClick: () => openAction('TIME', record) }
            : null,
          canReject && record.reviewStatus === 'WAITING'
            ? { key: 'reject', label: '驳回', onClick: () => openAction('REJECT', record) }
            : null,
          canLog
            ? { key: 'log', label: '操作日志', onClick: () => openDetail(record) }
            : null,
        ].filter(Boolean) as any[];
        return (
          <Space size={8}>
            {canQuery ? (
              <Button type="link" size="small" onClick={() => openDetail(record)}>
                详情
              </Button>
            ) : null}
            {canEffect && record.reviewStatus === 'WAITING' ? (
              <Button type="link" size="small" onClick={() => openAction('EFFECT', record)}>
                立即生效
              </Button>
            ) : null}
            {moreItems.length ? (
              <Dropdown menu={{ items: moreItems }}>
                <Button type="link" size="small">
                  更多 <DownOutlined />
                </Button>
              </Dropdown>
            ) : null}
          </Space>
        );
      },
    },
  ];

  const policyColumns: ProColumns<API.InventoryAdjustmentReview.Policy>[] = useMemo(() => [
    {
      title: '策略名称',
      dataIndex: 'policyName',
      width: 180,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'policyStatus',
      valueEnum: enabledValueEnum,
      width: 90,
    },
    {
      title: '模式',
      dataIndex: 'reviewMode',
      valueEnum: reviewModeValueEnum,
      width: 110,
    },
    {
      title: '方向',
      dataIndex: 'directionScope',
      valueEnum: directionScopeValueEnum,
      width: 100,
    },
    {
      title: '窗口',
      dataIndex: 'salesWindowDays',
      width: 90,
    },
    {
      title: '保留天数',
      dataIndex: 'reserveDays',
      width: 90,
      align: 'right',
    },
    {
      title: '冷却小时',
      dataIndex: 'cooldownHours',
      width: 90,
      align: 'right',
    },
    {
      title: '操作',
      valueType: 'option',
      width: 80,
      render: (_, record) => (
        canConfig ? (
          <Button type="link" size="small" onClick={() => openPolicyForm(record)}>
            编辑
          </Button>
        ) : null
      ),
    },
  ], [canConfig]);

  const bindingColumns: ProColumns<API.InventoryAdjustmentReview.PolicyBinding>[] = useMemo(() => [
    {
      title: '策略',
      dataIndex: 'policyName',
      width: 180,
      ellipsis: true,
    },
    {
      title: '策略ID',
      dataIndex: 'policyId',
      width: 90,
      align: 'right',
    },
    {
      title: '绑定类型',
      dataIndex: 'bindingType',
      valueEnum: bindingTypeValueEnum,
      width: 100,
    },
    {
      title: '绑定对象',
      dataIndex: 'bindingIdValue',
      width: 100,
      align: 'right',
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 90,
      align: 'right',
    },
    {
      title: '状态',
      dataIndex: 'status',
      valueEnum: enabledValueEnum,
      width: 90,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 80,
      render: (_, record) => (
        canConfig ? (
          <Button type="link" size="small" onClick={() => openBindingForm(record)}>
            编辑
          </Button>
        ) : null
      ),
    },
  ], [canConfig]);

  return (
    <PageContainer title={false}>
      <ProTable<API.InventoryAdjustmentReview.Review>
        className="urili-fill-table"
        actionRef={actionRef}
        rowKey="reviewId"
        columns={columns}
        tableLayout="fixed"
        scroll={getProTableScroll(TABLE_SCROLL_X)}
        search={getPersistedProTableSearch({ labelWidth: 90, fieldCount: 5 }, 'inventory-adjustment-review')}
        pagination={getProTablePagination(20)}
        request={async ({ current, pageSize, ...params }) => {
          if (!canList) {
            return { data: [], total: 0, success: false };
          }
          const resp = await getInventoryAdjustmentReviewList({
            ...rangeParams(params),
            pageNum: current,
            pageSize,
          });
          return {
            data: resp.rows || [],
            total: resp.total || 0,
            success: resp.code === 200,
          };
        }}
        toolBarRender={() => (
          canConfig ? [
            <Button key="config" icon={<SettingOutlined />} onClick={() => setConfigOpen(true)}>
              审核配置
            </Button>,
          ] : []
        )}
      />

      <Drawer
        title="库存调整审核详情"
        open={detailOpen}
        width={1080}
        onClose={() => setDetailOpen(false)}
      >
        {currentReview ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Descriptions size="small" bordered column={2}>
              <Descriptions.Item label="审核单号">{currentReview.reviewNo || '--'}</Descriptions.Item>
              <Descriptions.Item label="状态">{renderStatus(currentReview.reviewStatus)}</Descriptions.Item>
              <Descriptions.Item label="方向">{renderDirection(currentReview.adjustDirection)}</Descriptions.Item>
              <Descriptions.Item label="卖家ID">{currentReview.sellerId || '--'}</Descriptions.Item>
              <Descriptions.Item label="SKU">{currentReview.systemSkuCode || '--'}</Descriptions.Item>
              <Descriptions.Item label="仓库">{currentReview.warehouseName || '--'}</Descriptions.Item>
              <Descriptions.Item label="申请退回/调整数量">{formatQuantity(currentReview.requestedAdjustQty)}</Descriptions.Item>
              <Descriptions.Item label="申请时库存">
                {formatQuantity(currentReview.requestBeforePlatformTotalQty)} -&gt; {formatQuantity(currentReview.requestExpectedAfterPlatformTotalQty)}
              </Descriptions.Item>
              <Descriptions.Item label="可立即退回">{formatQuantity(currentReview.immediateReturnableQty)}</Descriptions.Item>
              <Descriptions.Item label="保护保留库存">{formatQuantity(currentReview.protectedRetainedQty)}</Descriptions.Item>
              <Descriptions.Item label="近7日日销">{formatDecimal(currentReview.sales7dDailyAvg)}</Descriptions.Item>
              <Descriptions.Item label="近30日日销">{formatDecimal(currentReview.sales30dDailyAvg)}</Descriptions.Item>
              <Descriptions.Item label="计划生效时间">{formatDateTime(currentReview.plannedEffectiveTime)}</Descriptions.Item>
              <Descriptions.Item label="实际生效时间">{formatDateTime(currentReview.effectiveTime)}</Descriptions.Item>
              <Descriptions.Item label="实际生效数量">{formatQuantity(currentReview.actualEffectQty)}</Descriptions.Item>
              <Descriptions.Item label="无法满足数量">{formatQuantity(currentReview.unfulfilledQty)}</Descriptions.Item>
              <Descriptions.Item label="触发原因" span={2}>{currentReview.triggerReason || '--'}</Descriptions.Item>
              <Descriptions.Item label="提交原因" span={2}>{currentReview.submitReason || '--'}</Descriptions.Item>
              <Descriptions.Item label="处理原因" span={2}>{currentReview.reviewReason || '--'}</Descriptions.Item>
            </Descriptions>
            {canLog ? (
              <Table<API.InventoryAdjustmentReview.OperationLog>
                rowKey="logId"
                size="small"
                pagination={false}
                columns={[
                  {
                    title: '操作',
                    dataIndex: 'operationType',
                    width: 120,
                    render: (value) => operationTypeText[String(value)] || String(value || '--'),
                  },
                  {
                    title: '状态',
                    dataIndex: 'afterStatus',
                    width: 120,
                    render: (_, record) => `${record.beforeStatus || '--'} -> ${record.afterStatus || '--'}`,
                  },
                  {
                    title: '操作人',
                    dataIndex: 'operatorName',
                    width: 120,
                  },
                  {
                    title: '时间',
                    dataIndex: 'operateTime',
                    width: 170,
                  },
                  {
                    title: '摘要',
                    dataIndex: 'changeSummary',
                  },
                  {
                    title: '原因',
                    dataIndex: 'operationReason',
                  },
                ] as ColumnsType<API.InventoryAdjustmentReview.OperationLog>}
                dataSource={logs}
              />
            ) : null}
          </Space>
        ) : null}
      </Drawer>

      <Modal
        title={actionState.kind === 'EFFECT' ? '立即生效' : actionState.kind === 'REJECT' ? '驳回审核单' : '调整计划生效时间'}
        open={actionState.open}
        onOk={submitAction}
        onCancel={() => setActionState({ open: false, kind: 'EFFECT' })}
        destroyOnClose
      >
        <Form form={actionForm} layout="vertical">
          {actionState.kind === 'TIME' ? (
            <Form.Item
              label="计划生效时间"
              name="plannedEffectiveTime"
              rules={[{ required: true, message: '请选择计划生效时间' }]}
            >
              <DatePicker showTime style={{ width: '100%' }} />
            </Form.Item>
          ) : null}
          <Form.Item label="处理原因" name="reason">
            <Input.TextArea rows={3} maxLength={500} placeholder="选填" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="库存调整审核配置"
        open={configOpen}
        width={980}
        onClose={() => setConfigOpen(false)}
      >
        <Tabs
          items={[
            {
              key: 'policies',
              label: '策略组',
              children: (
                <ProTable<API.InventoryAdjustmentReview.Policy>
                  actionRef={policyActionRef}
                  rowKey="policyId"
                  size="small"
                  columns={policyColumns}
                  search={false}
                  pagination={getProTablePagination(10)}
                  scroll={getProTableScroll(900, { y: 360 })}
                  request={async ({ current, pageSize, ...params }) => {
                    if (!canConfig) {
                      return { data: [], total: 0, success: false };
                    }
                    const resp = await getInventoryAdjustmentReviewPolicyList({
                      ...params,
                      pageNum: current,
                      pageSize,
                    });
                    return {
                      data: resp.rows || [],
                      total: resp.total || 0,
                      success: resp.code === 200,
                    };
                  }}
                  toolBarRender={() => [
                    <Button key="add" type="primary" onClick={() => openPolicyForm()}>
                      新增策略
                    </Button>,
                  ]}
                />
              ),
            },
            {
              key: 'bindings',
              label: '卖家绑定',
              children: (
                <ProTable<API.InventoryAdjustmentReview.PolicyBinding>
                  actionRef={bindingActionRef}
                  rowKey="bindingId"
                  size="small"
                  columns={bindingColumns}
                  search={false}
                  pagination={getProTablePagination(10)}
                  scroll={getProTableScroll(820, { y: 360 })}
                  request={async ({ current, pageSize, ...params }) => {
                    if (!canConfig) {
                      return { data: [], total: 0, success: false };
                    }
                    const resp = await getInventoryAdjustmentReviewPolicyBindingList({
                      ...params,
                      pageNum: current,
                      pageSize,
                    });
                    return {
                      data: resp.rows || [],
                      total: resp.total || 0,
                      success: resp.code === 200,
                    };
                  }}
                  toolBarRender={() => [
                    <Button key="add" type="primary" onClick={() => openBindingForm()}>
                      新增绑定
                    </Button>,
                  ]}
                />
              ),
            },
          ]}
        />
      </Drawer>

      <Modal
        title={policyState.record ? '编辑策略' : '新增策略'}
        open={policyState.open}
        onOk={submitPolicy}
        onCancel={() => setPolicyState({ open: false })}
        destroyOnClose
        width={720}
      >
        <Form form={policyForm} layout="vertical">
          <Form.Item name="fieldScope" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="salesAggregateMode" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="策略名称" name="policyName" rules={[{ required: true, message: '请输入策略名称' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Space size={16} style={{ width: '100%' }} align="start">
            <Form.Item label="状态" name="policyStatus" rules={[{ required: true }]} style={{ width: 150 }}>
              <Select options={[
                { label: '启用', value: 'ENABLED' },
                { label: '停用', value: 'DISABLED' },
              ]} />
            </Form.Item>
            <Form.Item label="审核模式" name="reviewMode" rules={[{ required: true }]} style={{ width: 170 }}>
              <Select options={[
                { label: '不审核', value: 'DISABLED' },
                { label: '按门槛审核', value: 'CONDITIONAL' },
                { label: '强制审核', value: 'ALWAYS' },
              ]} />
            </Form.Item>
            <Form.Item label="方向范围" name="directionScope" rules={[{ required: true }]} style={{ width: 150 }}>
              <Select options={[
                { label: '仅退回', value: 'DECREASE' },
                { label: '仅增加', value: 'INCREASE' },
                { label: '增减都审', value: 'BOTH' },
              ]} />
            </Form.Item>
          </Space>
          <Space size={16} style={{ width: '100%' }} align="start">
            <Form.Item
              label="销量窗口"
              name="salesWindowDays"
              rules={[
                { required: true, message: '请输入销量窗口' },
                {
                  validator: (_, value) => (
                    validateSalesWindowText(value)
                      ? Promise.resolve()
                      : Promise.reject(new Error('销量窗口必须至少包含一个正整数天数'))
                  ),
                },
              ]}
              style={{ width: 170 }}
            >
              <Input placeholder="[7,30]" />
            </Form.Item>
            <Form.Item label="保留天数" name="reserveDays" rules={[{ required: true }]} style={{ width: 150 }}>
              <InputNumber min={0} precision={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="冷却小时" name="cooldownHours" rules={[{ required: true }]} style={{ width: 150 }}>
              <InputNumber min={0} precision={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space size={16} style={{ width: '100%' }} align="start">
            <Form.Item label="最低退回数量" name="minReturnQtyToReview" style={{ width: 170 }}>
              <InputNumber min={0} precision={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="最低退回比例" name="minReturnRatioToReview" style={{ width: 170 }}>
              <InputNumber min={0} max={1} step={0.01} precision={4} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="到期自动生效" name="autoEffectEnabled" style={{ width: 150 }}>
              <Select options={[
                { label: '是', value: 'Y' },
                { label: '否', value: 'N' },
              ]} />
            </Form.Item>
            <Form.Item label="允许人工生效" name="manualEffectAllowed" style={{ width: 150 }}>
              <Select options={[
                { label: '是', value: 'Y' },
                { label: '否', value: 'N' },
              ]} />
            </Form.Item>
          </Space>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={bindingState.record ? '编辑绑定' : '新增绑定'}
        open={bindingState.open}
        onOk={submitBinding}
        onCancel={() => setBindingState({ open: false })}
        destroyOnClose
      >
        <Form form={bindingForm} layout="vertical">
          <Form.Item label="策略ID" name="policyId" rules={[{ required: true, message: '请输入策略ID' }]}>
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="绑定类型" name="bindingType" rules={[{ required: true }]}>
            <Select options={[
              { label: '全局', value: 'GLOBAL' },
              { label: '卖家', value: 'SELLER' },
            ]} />
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(prev, next) => prev.bindingType !== next.bindingType}>
            {({ getFieldValue }) => {
              const bindingType = getFieldValue('bindingType');
              return (
                <Form.Item
                  label="绑定对象ID"
                  name="bindingIdValue"
                  rules={bindingType === 'SELLER'
                    ? [{ required: true, message: '请输入卖家ID' }]
                    : []}
                >
                  <InputNumber
                    min={bindingType === 'SELLER' ? 1 : 0}
                    precision={0}
                    disabled={bindingType === 'GLOBAL'}
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              );
            }}
          </Form.Item>
          <Form.Item label="优先级" name="priority">
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select options={[
              { label: '启用', value: 'ENABLED' },
              { label: '停用', value: 'DISABLED' },
            ]} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
}
