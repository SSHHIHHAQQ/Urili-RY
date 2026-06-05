import { PlusOutlined } from '@ant-design/icons';
import { Button, Checkbox, Input, InputNumber, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { salesStatusOptions, skuSpecFields } from '../constants';
import ImageUploadField from './ImageUploadField';
import styles from '../style.module.css';

type SkuRow = API.ProductDistribution.Sku & {
  rowKey?: string;
};

type SkuMatrixEditorProps = {
  value: SkuRow[];
  currencyCode?: string;
  currencyLabel?: string;
  onChange: (value: SkuRow[]) => void;
};

function normalizeSpecValue(value?: string) {
  return (value || '').trim();
}

function unique(values: (string | undefined)[]) {
  return Array.from(new Set(values.map(normalizeSpecValue).filter(Boolean)));
}

function makeRowKey() {
  return `sku-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function cartesian(entries: { field: keyof API.ProductDistribution.Sku; values: string[] }[]) {
  if (!entries.length) {
    return [{} as Record<string, string>];
  }
  return entries.reduce<Record<string, string>[]>(
    (acc, entry) =>
      acc.flatMap((item) => entry.values.map((value) => ({ ...item, [entry.field]: value }))),
    [{}],
  );
}

function specSignature(row: API.ProductDistribution.Sku, fields: (keyof API.ProductDistribution.Sku)[]) {
  return fields.map((field) => normalizeSpecValue(row[field] as string)).join('||');
}

function inferSelectedSpecs(rows: SkuRow[]) {
  const selected = skuSpecFields
    .filter((field) => rows.some((row) => normalizeSpecValue(row[field.value] as string)))
    .map((field) => field.value);
  return selected.length ? selected : ['color', 'size'] as (keyof API.ProductDistribution.Sku)[];
}

export default function SkuMatrixEditor({
  value,
  currencyCode,
  currencyLabel,
  onChange,
}: SkuMatrixEditorProps) {
  const [selectedSpecs, setSelectedSpecs] = useState<(keyof API.ProductDistribution.Sku)[]>(['color', 'size']);
  const [specValues, setSpecValues] = useState<Record<string, string[]>>({});
  const [draftValues, setDraftValues] = useState<Record<string, string>>({});
  const [bulkLengthValue, setBulkLengthValue] = useState<string>();
  const [bulkWidthValue, setBulkWidthValue] = useState<string>();
  const [bulkHeightValue, setBulkHeightValue] = useState<string>();
  const [bulkWeight, setBulkWeight] = useState<string>();
  const [bulkSupplyPrice, setBulkSupplyPrice] = useState<number>();
  const [bulkSalePrice, setBulkSalePrice] = useState<number>();

  useEffect(() => {
    const selected = inferSelectedSpecs(value);
    const values: Record<string, string[]> = {};
    selected.forEach((field) => {
      values[field] = unique(value.map((row) => row[field] as string));
    });
    setSelectedSpecs(selected);
    setSpecValues(values);
  }, []);

  const rows = useMemo(
    () => value.map((row) => ({ ...row, rowKey: row.rowKey || String(row.skuId || makeRowKey()) })),
    [value],
  );

  const updateRow = (rowKey: string, patch: Partial<SkuRow>) => {
    onChange(rows.map((row) => (row.rowKey === rowKey ? { ...row, ...patch } : row)));
  };

  const addSpecValue = (field: keyof API.ProductDistribution.Sku) => {
    const draft = normalizeSpecValue(draftValues[field]);
    if (!draft) return;
    const current = specValues[field] || [];
    if (current.includes(draft)) {
      setDraftValues({ ...draftValues, [field]: '' });
      return;
    }
    setSpecValues({ ...specValues, [field]: [...current, draft] });
    setDraftValues({ ...draftValues, [field]: '' });
  };

  const removeSpecValue = (field: keyof API.ProductDistribution.Sku, item: string) => {
    setSpecValues({
      ...specValues,
      [field]: (specValues[field] || []).filter((valueItem) => valueItem !== item),
    });
  };

  const generateSkus = () => {
    const entries = selectedSpecs
      .map((field) => ({ field, values: specValues[field] || [] }))
      .filter((entry) => entry.values.length > 0);
    const currentMap = new Map(rows.map((row) => [specSignature(row, selectedSpecs), row]));
    const nextRows = cartesian(entries).map((combo, index) => {
      const existed = currentMap.get(selectedSpecs.map((field) => combo[field] || '').join('||'));
      return {
        ...(existed || {}),
        ...combo,
        rowKey: existed?.rowKey || makeRowKey(),
        skuStatus: existed?.skuStatus || 'DRAFT',
        sortOrder: existed?.sortOrder ?? index,
      } as SkuRow;
    });
    onChange(nextRows.length ? nextRows : [{ rowKey: makeRowKey(), skuStatus: 'DRAFT', sortOrder: 0 }]);
  };

  const addManualSku = () => {
    onChange([
      ...rows,
      {
        rowKey: makeRowKey(),
        skuStatus: 'DRAFT',
        sortOrder: rows.length,
      },
    ]);
  };

  const applyBulk = () => {
    onChange(rows.map((row) => ({
      ...row,
      lengthValue: bulkLengthValue || row.lengthValue,
      widthValue: bulkWidthValue || row.widthValue,
      heightValue: bulkHeightValue || row.heightValue,
      weight: bulkWeight || row.weight,
      supplyPrice: bulkSupplyPrice ?? row.supplyPrice,
      salePrice: bulkSalePrice ?? row.salePrice,
    })));
  };

  const columns: ColumnsType<SkuRow> = [
    ...selectedSpecs.map((field) => ({
      title: skuSpecFields.find((item) => item.value === field)?.label || String(field),
      dataIndex: field as string,
      width: 92,
      fixed: 'left' as const,
      render: (_: unknown, row: SkuRow) => (
        <Input
          value={row[field] as string}
          onChange={(event) => updateRow(row.rowKey!, { [field]: event.target.value })}
        />
      ),
    })),
    {
      title: '客户SKU',
      dataIndex: 'sellerSkuCode',
      width: 150,
      render: (_, row) => (
        <Input value={row.sellerSkuCode} onChange={(event) => updateRow(row.rowKey!, { sellerSkuCode: event.target.value })} />
      ),
    },
    {
      title: 'SKU图',
      dataIndex: 'skuImageUrl',
      width: 104,
      render: (_, row) => (
        <ImageUploadField
          size="small"
          value={row.skuImageUrl}
          onChange={(skuImageUrl) => updateRow(row.rowKey!, { skuImageUrl })}
        />
      ),
    },
    {
      title: '长度',
      dataIndex: 'lengthValue',
      width: 110,
      render: (_, row) => (
        <Input
          value={row.lengthValue}
          placeholder="如 30cm"
          onChange={(event) => updateRow(row.rowKey!, { lengthValue: event.target.value })}
        />
      ),
    },
    {
      title: '宽度',
      dataIndex: 'widthValue',
      width: 110,
      render: (_, row) => (
        <Input
          value={row.widthValue}
          placeholder="如 20cm"
          onChange={(event) => updateRow(row.rowKey!, { widthValue: event.target.value })}
        />
      ),
    },
    {
      title: '高度',
      dataIndex: 'heightValue',
      width: 110,
      render: (_, row) => (
        <Input
          value={row.heightValue}
          placeholder="如 8cm"
          onChange={(event) => updateRow(row.rowKey!, { heightValue: event.target.value })}
        />
      ),
    },
    {
      title: '重量',
      dataIndex: 'weight',
      width: 110,
      render: (_, row) => (
        <Input
          value={row.weight}
          placeholder="如 350g"
          onChange={(event) => updateRow(row.rowKey!, { weight: event.target.value })}
        />
      ),
    },
    {
      title: '供货价',
      dataIndex: 'supplyPrice',
      width: 120,
      render: (_, row) => (
        <InputNumber
          min={0}
          precision={4}
          value={row.supplyPrice}
          style={{ width: '100%' }}
          onChange={(supplyPrice) => updateRow(row.rowKey!, { supplyPrice: supplyPrice ?? undefined })}
        />
      ),
    },
    {
      title: '销售价',
      dataIndex: 'salePrice',
      width: 120,
      render: (_, row) => (
        <div className={styles.priceCell}>
          <InputNumber
            min={0}
            precision={4}
            value={row.salePrice}
            style={{ width: '100%' }}
            onChange={(salePrice) => updateRow(row.rowKey!, { salePrice: salePrice ?? undefined })}
          />
          {row.salePrice !== undefined && row.supplyPrice !== undefined && row.salePrice < row.supplyPrice ? (
            <Typography.Text type="warning" className={styles.priceRiskText}>低于供货价</Typography.Text>
          ) : null}
        </div>
      ),
    },
    {
      title: '币种',
      dataIndex: 'currencyCode',
      width: 120,
      render: () => currencyLabel || currencyCode || '-',
    },
    {
      title: '状态',
      dataIndex: 'skuStatus',
      width: 110,
      render: (_, row) => (
        <Select
          value={row.skuStatus || 'DRAFT'}
          options={salesStatusOptions}
          style={{ width: '100%' }}
          onChange={(skuStatus) => updateRow(row.rowKey!, { skuStatus })}
        />
      ),
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 90,
      render: (_, row) => (
        <InputNumber
          min={0}
          value={row.sortOrder}
          style={{ width: '100%' }}
          onChange={(sortOrder) => updateRow(row.rowKey!, { sortOrder: sortOrder ?? undefined })}
        />
      ),
    },
    {
      title: '操作',
      width: 72,
      fixed: 'right',
      render: (_, row) => (
        <Button
          danger
          type="link"
          size="small"
          disabled={rows.length <= 1}
          onClick={() => onChange(rows.filter((item) => item.rowKey !== row.rowKey))}
        >
          删除
        </Button>
      ),
    },
  ];

  return (
    <div className={styles.skuEditor}>
      <div className={styles.sectionTitle}>SKU 信息</div>
      <div className={styles.specPanel}>
        <div className={styles.specPanelTitle}>规格属性</div>
        <Checkbox.Group
          value={selectedSpecs as string[]}
          options={skuSpecFields}
          onChange={(checked) => setSelectedSpecs(checked as (keyof API.ProductDistribution.Sku)[])}
        />
        <div className={styles.specValueGrid}>
          {selectedSpecs.map((field) => (
            <div className={styles.specValueItem} key={field}>
              <div className={styles.specValueTitle}>
                {skuSpecFields.find((item) => item.value === field)?.label}
              </div>
              <Space wrap>
                {(specValues[field] || []).map((item) => (
                  <Tag closable key={item} onClose={() => removeSpecValue(field, item)}>
                    {item}
                  </Tag>
                ))}
                <Input
                  className={styles.specInput}
                  value={draftValues[field]}
                  placeholder="输入规格值"
                  onChange={(event) => setDraftValues({ ...draftValues, [field]: event.target.value })}
                  onPressEnter={() => addSpecValue(field)}
                />
                <Button size="small" type="primary" onClick={() => addSpecValue(field)}>
                  添加
                </Button>
              </Space>
            </div>
          ))}
        </div>
        <Space>
          <Button type="primary" onClick={generateSkus}>生成 SKU</Button>
          <Button onClick={addManualSku} icon={<PlusOutlined />}>新增 SKU</Button>
        </Space>
      </div>

      <div className={styles.bulkPanel}>
        <Space size={8} wrap>
          <Typography.Text strong className={styles.bulkPanelLabel}>
            批量填充
          </Typography.Text>
          <Space.Compact>
            <Input
              allowClear
              placeholder="长"
              style={{ width: 96 }}
              value={bulkLengthValue}
              onChange={(event) => setBulkLengthValue(event.target.value || undefined)}
            />
            <Input
              allowClear
              placeholder="宽"
              style={{ width: 96 }}
              value={bulkWidthValue}
              onChange={(event) => setBulkWidthValue(event.target.value || undefined)}
            />
            <Input
              allowClear
              placeholder="高"
              style={{ width: 96 }}
              value={bulkHeightValue}
              onChange={(event) => setBulkHeightValue(event.target.value || undefined)}
            />
          </Space.Compact>
          <Input
            allowClear
            placeholder="重量"
            style={{ width: 112 }}
            value={bulkWeight}
            onChange={(event) => setBulkWeight(event.target.value || undefined)}
          />
          <InputNumber
            min={0}
            precision={4}
            placeholder="供货价"
            style={{ width: 140 }}
            value={bulkSupplyPrice}
            onChange={(value) => setBulkSupplyPrice(value ?? undefined)}
          />
          <InputNumber
            min={0}
            precision={4}
            placeholder="销售价"
            style={{ width: 140 }}
            value={bulkSalePrice}
            onChange={(value) => setBulkSalePrice(value ?? undefined)}
          />
          <Button type="primary" onClick={applyBulk}>
            应用到全部 SKU
          </Button>
        </Space>
      </div>

      <Table
        rowKey="rowKey"
        columns={columns}
        dataSource={rows}
        pagination={false}
        scroll={{ x: 1480 }}
        size="small"
      />
    </div>
  );
}
