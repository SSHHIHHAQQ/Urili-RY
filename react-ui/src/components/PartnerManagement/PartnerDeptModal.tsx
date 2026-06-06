import React, { useEffect, useState } from 'react';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Flex,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Table,
  Tag,
  TreeSelect,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined } from '@ant-design/icons';
import { SEARCHABLE_SELECT_PROPS, SEARCHABLE_TREE_SELECT_PROPS } from '@/utils/selectSearch';
import type { PartnerModuleConfig } from './PartnerManagementPage';

type PartnerRecord = Record<string, any>;
type DeptRecord = API.Partner.PortalDept & Record<string, any>;

type PartnerDeptModalProps = {
  config: PartnerModuleConfig;
  open: boolean;
  partner?: PartnerRecord;
  onOpenChange: (open: boolean) => void;
};

type DeptFormValues = {
  parentId?: number;
  deptName?: string;
  orderNum?: number;
  leader?: string;
  phone?: string;
  email?: string;
  status?: string;
};

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
];

const compactCellTextStyle: React.CSSProperties = {
  display: 'block',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  lineHeight: 1.35,
};

function getValue(record: PartnerRecord | undefined, field: string) {
  return record ? record[field] : undefined;
}

function mapDeptToForm(dept?: DeptRecord): DeptFormValues {
  return {
    parentId: dept?.parentId && dept.parentId > 0 ? dept.parentId : 0,
    deptName: dept?.deptName,
    orderNum: dept?.orderNum ?? 0,
    leader: dept?.leader,
    phone: dept?.phone,
    email: dept?.email,
    status: dept?.status || '0',
  };
}

function buildDeptPayload(
  currentDept: DeptRecord | undefined,
  values: DeptFormValues,
) {
  return {
    deptId: currentDept?.deptId,
    parentId: values.parentId || 0,
    deptName: values.deptName,
    orderNum: values.orderNum ?? 0,
    leader: values.leader,
    phone: values.phone,
    email: values.email,
    status: values.status || '0',
  };
}

function renderCompactText(value: unknown) {
  const text = value == null || value === '' ? '-' : String(value);
  return <Typography.Text style={compactCellTextStyle} title={text}>{text}</Typography.Text>;
}

const PartnerDeptModal: React.FC<PartnerDeptModalProps> = ({
  config,
  open,
  partner,
  onOpenChange,
}) => {
  const { message, modal } = App.useApp();
  const access = useAccess();
  const [form] = Form.useForm<DeptFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [currentDept, setCurrentDept] = useState<DeptRecord>();
  const [depts, setDepts] = useState<DeptRecord[]>([]);
  const [deptTree, setDeptTree] = useState<API.Partner.PortalTreeNode[]>([]);

  const partnerId = Number(getValue(partner, config.idField) || 0);
  const partnerName = getValue(partner, config.nameField) || getValue(partner, config.codeField) || '';
  const permPrefix = `${config.moduleKey}:admin`;
  const currentDeptId = currentDept?.deptId;
  const canQueryDeptTree = access.hasPerms(`${permPrefix}:dept:query`);
  const canAddDept = access.hasPerms(`${permPrefix}:dept:add`) && canQueryDeptTree;
  const canEditDept = access.hasPerms(`${permPrefix}:dept:edit`) && canQueryDeptTree;

  const parentTreeData = [
    {
      id: 0,
      label: '顶级部门',
      children: deptTree,
    },
  ];

  const loadDepts = async () => {
    if (!partnerId) {
      setDepts([]);
      setDeptTree([]);
      return;
    }
    setLoading(true);
    try {
      const [listResp, treeResp] = await Promise.all([
        config.services.listDepts(partnerId),
        canQueryDeptTree ? config.services.getDeptTree(partnerId) : Promise.resolve(undefined),
      ]);
      if (listResp.code === 200) {
        setDepts((listResp.data || []) as DeptRecord[]);
      } else {
        message.error(listResp.msg || '部门列表加载失败');
      }
      if (treeResp?.code === 200) {
        setDeptTree(treeResp.data || []);
      } else {
        setDeptTree([]);
      }
    } catch {
      message.error('部门列表加载失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      void loadDepts();
      return;
    }
    setDepts([]);
    setDeptTree([]);
  }, [open, partnerId]);

  const openDeptForm = (dept?: DeptRecord) => {
    setCurrentDept(dept);
    setFormOpen(true);
  };

  useEffect(() => {
    if (formOpen) {
      form.resetFields();
      form.setFieldsValue(mapDeptToForm(currentDept));
    }
  }, [formOpen, currentDept?.deptId]);

  const closeDeptForm = () => {
    setFormOpen(false);
    setCurrentDept(undefined);
  };

  const handleSubmit = async () => {
    if (!partnerId) {
      return;
    }
    const values = await form.validateFields();
    const payload = buildDeptPayload(currentDept, values);
    setSaving(true);
    try {
      const resp = currentDeptId
        ? await config.services.updateDept(partnerId, payload)
        : await config.services.addDept(partnerId, payload);
      if (resp.code === 200) {
        message.success(currentDeptId ? '部门已更新' : '部门已新增');
        closeDeptForm();
        await loadDepts();
        return;
      }
      message.error(resp.msg || '部门保存失败');
    } catch {
      message.error('部门保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = (dept: DeptRecord) => {
    if (!partnerId || !dept.deptId) {
      return;
    }
    modal.confirm({
      title: `确认删除部门 ${dept.deptName || dept.deptId} 吗？`,
      content: '存在子部门或账号占用时，后端会拒绝删除。',
      onOk: async () => {
        const resp = await config.services.removeDept(partnerId, dept.deptId as number);
        if (resp.code === 200) {
          message.success('部门已删除');
          await loadDepts();
          return;
        }
        message.error(resp.msg || '部门删除失败');
      },
    });
  };

  const columns: ColumnsType<DeptRecord> = [
    {
      title: '部门名称',
      dataIndex: 'deptName',
      width: 180,
      render: renderCompactText,
    },
    {
      title: '上级部门',
      dataIndex: 'parentName',
      width: 150,
      render: renderCompactText,
    },
    {
      title: '排序',
      dataIndex: 'orderNum',
      width: 72,
      render: renderCompactText,
    },
    {
      title: '负责人',
      dataIndex: 'leader',
      width: 120,
      render: renderCompactText,
    },
    {
      title: '联系方式',
      dataIndex: 'phone',
      width: 150,
      render: (_, record) => (
        <Flex vertical gap={0}>
          {renderCompactText(record.phone)}
          <Typography.Text type="secondary" style={compactCellTextStyle} title={record.email || '-'}>
            {record.email || '-'}
          </Typography.Text>
        </Flex>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (value) => <Tag color={value === '0' ? 'success' : 'default'}>{value === '0' ? '正常' : '停用'}</Tag>,
    },
    {
      title: '操作',
      dataIndex: 'option',
      width: 120,
      render: (_, record) => (
        <Flex gap={4}>
          <Button
            type="link"
            size="small"
            hidden={!canEditDept}
            onClick={() => openDeptForm(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            hidden={!access.hasPerms(`${permPrefix}:dept:remove`)}
            onClick={() => handleRemove(record)}
          >
            删除
          </Button>
        </Flex>
      ),
    },
  ];

  return (
    <>
      <Modal
        width={900}
        title={`${config.label}部门 - ${partnerName || '-'}`}
        open={open}
        destroyOnHidden
        footer={null}
        onCancel={() => onOpenChange(false)}
      >
        <Table<DeptRecord>
          rowKey={(record) => String(record.deptId)}
          loading={loading}
          columns={columns}
          dataSource={depts}
          size="small"
          pagination={false}
          tableLayout="fixed"
          title={() => (
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              hidden={!canAddDept}
              onClick={() => openDeptForm()}
            >
              新增部门
            </Button>
          )}
        />
      </Modal>

      <Modal
        width={640}
        title={currentDeptId ? '编辑部门' : '新增部门'}
        open={formOpen}
        destroyOnHidden
        confirmLoading={saving}
        onOk={handleSubmit}
        onCancel={closeDeptForm}
      >
        <Form form={form} layout="vertical">
          <Form.Item label="上级部门" name="parentId">
            <TreeSelect
              {...SEARCHABLE_TREE_SELECT_PROPS}
              treeDefaultExpandAll
              placeholder="请选择"
              treeData={parentTreeData}
              fieldNames={{ label: 'label', value: 'id', children: 'children' }}
            />
          </Form.Item>
          <Form.Item label="部门名称" name="deptName" rules={[{ required: true, message: '请输入部门名称' }]}>
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="排序" name="orderNum" rules={[{ required: true, message: '请输入排序' }]}>
            <InputNumber min={0} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="负责人" name="leader">
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="联系电话" name="phone">
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="邮箱" name="email">
            <Input placeholder="请输入" />
          </Form.Item>
          <Form.Item label="状态" name="status" rules={[{ required: true, message: '请选择状态' }]}>
            <Select {...SEARCHABLE_SELECT_PROPS} options={statusOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default PartnerDeptModal;
