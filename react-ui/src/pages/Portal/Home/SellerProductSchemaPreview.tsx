import {
  Card,
  Empty,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  attributeGroupOptions,
  attributeTypeOptions,
  optionSourceOptions,
  ruleModeOptions,
  yesNoOptions,
} from '@/pages/Product/constants';
import {
  getSellerPortalProductCategories,
  getSellerPortalProductSchema,
} from '@/services/portal/session';
import { message } from '@/utils/feedback';
import { SEARCHABLE_SELECT_PROPS } from '@/utils/selectSearch';

type PortalProductSchemaPreviewProps = {
  categoryPlaceholder?: string;
  getCategories: () => Promise<API.Partner.PortalProductCategoryListResult>;
  getSchema: (categoryId: number) => Promise<API.Partner.PortalProductSchemaResult>;
  title: string;
};

function displayText(value?: string | number | null) {
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function optionLabel(options: { label: string; value: string }[], value?: string) {
  return options.find((item) => item.value === value)?.label || displayText(value);
}

function flagTag(value?: string) {
  const label = optionLabel(yesNoOptions, value);
  return <Tag color={value === 'Y' ? 'success' : undefined}>{label}</Tag>;
}

function optionTags(options?: API.Partner.PortalProductAttributeOption[]) {
  const enabledOptions = options || [];
  if (enabledOptions.length === 0) {
    return '-';
  }
  const visibleOptions = enabledOptions.slice(0, 4);
  const hiddenCount = enabledOptions.length - visibleOptions.length;
  return (
    <Space wrap size={[4, 4]}>
      {visibleOptions.map((option) => (
        <Tag key={option.optionCode || option.optionLabel}>
          {option.optionLabel || option.optionCode}
        </Tag>
      ))}
      {hiddenCount > 0 ? <Tag>+{hiddenCount}</Tag> : null}
    </Space>
  );
}

export const PortalProductSchemaPreview: React.FC<PortalProductSchemaPreviewProps> = ({
  categoryPlaceholder = '商品分类',
  getCategories,
  getSchema,
  title,
}) => {
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [schemaLoading, setSchemaLoading] = useState(false);
  const [categories, setCategories] = useState<API.Partner.PortalProductCategory[]>([]);
  const [schemaRows, setSchemaRows] = useState<API.Partner.PortalProductCategorySchemaItem[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState<number>();
  const schemaRequestSeq = useRef(0);

  const loadCategories = useCallback(async () => {
    setCategoryLoading(true);
    try {
      const response = await getCategories();
      if (response.code !== 200) {
        message.error(response.msg || '商品分类加载失败');
        setCategories([]);
        setSelectedCategoryId(undefined);
        return;
      }
      const rows = response.data || [];
      setCategories(rows);
      setSelectedCategoryId((current) => {
        if (current && rows.some((item) => item.categoryId === current)) {
          return current;
        }
        return rows[0]?.categoryId;
      });
    } catch (error) {
      console.log(error);
      message.error('商品分类加载失败');
      setCategories([]);
      setSelectedCategoryId(undefined);
    } finally {
      setCategoryLoading(false);
    }
  }, [getCategories]);

  const loadSchema = useCallback(async (categoryId: number) => {
    const requestSeq = schemaRequestSeq.current + 1;
    schemaRequestSeq.current = requestSeq;
    setSchemaLoading(true);
    try {
      const response = await getSchema(categoryId);
      if (schemaRequestSeq.current !== requestSeq) {
        return;
      }
      if (response.code !== 200) {
        message.error(response.msg || '商品 Schema 加载失败');
        setSchemaRows([]);
        return;
      }
      setSchemaRows(response.data || []);
    } catch (error) {
      console.log(error);
      if (schemaRequestSeq.current === requestSeq) {
        message.error('商品 Schema 加载失败');
        setSchemaRows([]);
      }
    } finally {
      if (schemaRequestSeq.current === requestSeq) {
        setSchemaLoading(false);
      }
    }
  }, [getSchema]);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  useEffect(() => {
    if (!selectedCategoryId) {
      setSchemaRows([]);
      return;
    }
    loadSchema(selectedCategoryId);
  }, [loadSchema, selectedCategoryId]);

  const categoryOptions = useMemo(
    () =>
      categories
        .filter((item) => item.categoryId)
        .map((item) => ({
          label: `${displayText(item.categoryName)} / ${displayText(item.categoryCode)}`,
          value: item.categoryId as number,
          searchText: `${item.categoryName || ''} ${item.categoryCode || ''}`,
        })),
    [categories],
  );

  const selectedCategory = useMemo(
    () => categories.find((item) => item.categoryId === selectedCategoryId),
    [categories, selectedCategoryId],
  );

  const columns: ColumnsType<API.Partner.PortalProductCategorySchemaItem> = [
    {
      title: '属性',
      dataIndex: 'attributeName',
      key: 'attributeName',
      width: 180,
      render: (value, record) => (
        <Space orientation="vertical" size={0}>
          <span>{displayText(value)}</span>
          <span style={{ color: '#8c8c8c', fontSize: 12 }}>{displayText(record.attributeCode)}</span>
        </Space>
      ),
    },
    {
      title: '类型',
      dataIndex: 'attributeType',
      key: 'attributeType',
      width: 120,
      render: (value) => optionLabel(attributeTypeOptions, value),
    },
    {
      title: '必填',
      dataIndex: 'requiredFlag',
      key: 'requiredFlag',
      width: 88,
      render: flagTag,
    },
    {
      title: '可编辑',
      dataIndex: 'editableFlag',
      key: 'editableFlag',
      width: 88,
      render: flagTag,
    },
    {
      title: '可筛选',
      dataIndex: 'filterableFlag',
      key: 'filterableFlag',
      width: 88,
      render: flagTag,
    },
    {
      title: '分组',
      dataIndex: 'groupCode',
      key: 'groupCode',
      width: 120,
      render: (value) => optionLabel(attributeGroupOptions, value),
    },
    {
      title: '规则',
      dataIndex: 'ruleMode',
      key: 'ruleMode',
      width: 120,
      render: (value) => optionLabel(ruleModeOptions, value),
    },
    {
      title: '选项来源',
      dataIndex: 'optionSource',
      key: 'optionSource',
      width: 140,
      render: (value) => optionLabel(optionSourceOptions, value),
    },
    {
      title: '选项',
      dataIndex: 'options',
      key: 'options',
      width: 220,
      render: optionTags,
    },
    {
      title: '来源类目',
      dataIndex: 'sourceCategoryName',
      key: 'sourceCategoryName',
      width: 140,
      ellipsis: true,
      render: displayText,
    },
    {
      title: '提示',
      dataIndex: 'placeholder',
      key: 'placeholder',
      width: 160,
      ellipsis: true,
      render: (value, record) => {
        const text = value || record.helpText || record.validationRule;
        return text ? (
          <Tooltip title={text}>
            <span>{text}</span>
          </Tooltip>
        ) : (
          '-'
        );
      },
    },
  ];

  return (
    <Card title={title} variant="borderless">
      <Space orientation="vertical" size={12} style={{ width: '100%' }}>
        <Space wrap>
          <Select
            {...SEARCHABLE_SELECT_PROPS}
            loading={categoryLoading}
            value={selectedCategoryId}
            options={categoryOptions}
            placeholder={categoryPlaceholder}
            style={{ minWidth: 280 }}
            onChange={setSelectedCategoryId}
          />
          <Tag>Schema v{displayText(selectedCategory?.schemaVersion)}</Tag>
        </Space>
        <Table<API.Partner.PortalProductCategorySchemaItem>
          size="small"
          rowKey={(record) =>
            `${record.categoryId || selectedCategoryId || 0}-${record.attributeId || record.attributeCode || record.attributeName || record.sortOrder || 'schema'}`
          }
          loading={schemaLoading}
          columns={columns}
          dataSource={schemaRows}
          pagination={false}
          scroll={{ x: 1450 }}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
        />
      </Space>
    </Card>
  );
};

const SellerProductSchemaPreview: React.FC = () => (
  <PortalProductSchemaPreview
    title="商品发布准备"
    getCategories={getSellerPortalProductCategories}
    getSchema={getSellerPortalProductSchema}
  />
);

export default SellerProductSchemaPreview;
