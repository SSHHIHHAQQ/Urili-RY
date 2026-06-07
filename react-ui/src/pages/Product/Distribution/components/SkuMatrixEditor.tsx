import { PlusOutlined } from '@ant-design/icons';
import { Button, Checkbox, Input, InputNumber, Radio, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import { skuSpecFields } from '../constants';
import ImageUploadField from './ImageUploadField';
import styles from '../style.module.css';

type SkuRow = API.ProductDistribution.Sku & {
  rowKey?: string;
};

type SkuMatrixEditorProps = {
  value: SkuRow[];
  focusSkuId?: number;
  currencyCode?: string;
  currencyLabel?: string;
  sourceMode?: boolean;
  onChange: (value: SkuRow[]) => void;
};

type MeasurementKind = 'dimension' | 'weight';
type MeasurementUnitSystem = 'METRIC' | 'IMPERIAL';
type MeasurementField = 'lengthValue' | 'widthValue' | 'heightValue' | 'weight';

type MeasurementInputProps = {
  value?: number;
  unit: string;
  placeholder?: string;
  style?: CSSProperties;
  disabled?: boolean;
  onChange: (value: string | number | null) => void;
};

const compactMeasurementUnitOptions = [
  { label: '公制单位', value: 'METRIC' },
  { label: '英制单位', value: 'IMPERIAL' },
];

const measurementUnits: Record<MeasurementUnitSystem, { dimension: 'cm' | 'in'; weight: 'kg' | 'lb' }> = {
  METRIC: { dimension: 'cm', weight: 'kg' },
  IMPERIAL: { dimension: 'in', weight: 'lb' },
};

const dimensionFields: MeasurementField[] = ['lengthValue', 'widthValue', 'heightValue'];
const measurementFields: MeasurementField[] = [...dimensionFields, 'weight'];
const maxSelectedSpecCount = 2;

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
  return selected.length
    ? selected.slice(0, maxSelectedSpecCount)
    : ['color', 'size'] as (keyof API.ProductDistribution.Sku)[];
}

function MeasurementInput({
  value,
  unit,
  placeholder,
  style,
  disabled,
  onChange,
}: MeasurementInputProps) {
  return (
    <InputNumber
      min={0}
      controls={false}
      value={value}
      placeholder={placeholder}
      suffix={<span className={styles.measurementUnitSuffix}>{unit}</span>}
      className={styles.measurementInput}
      style={style}
      disabled={disabled}
      onChange={onChange}
    />
  );
}

function normalizeUnit(unit?: string) {
  const value = unit?.trim().toLowerCase();
  if (!value) return undefined;
  if (['cm', '厘米'].includes(value)) return 'cm';
  if (['in', 'inch', 'inches', '英寸'].includes(value)) return 'in';
  if (['kg', 'kgs', 'kilogram', 'kilograms', '公斤', '千克'].includes(value)) return 'kg';
  if (['g', 'gram', 'grams', '克'].includes(value)) return 'g';
  if (['lb', 'lbs', 'pound', 'pounds', '磅'].includes(value)) return 'lb';
  return value;
}

function parseMeasurementValue(value?: string) {
  const raw = value?.trim();
  if (!raw) return undefined;
  const match = raw.match(/^(-?\d+(?:\.\d+)?)\s*(.*)?$/);
  if (!match) return undefined;
  const numberValue = Number(match[1]);
  if (!Number.isFinite(numberValue)) return undefined;
  return {
    value: numberValue,
    unit: normalizeUnit(match[2]),
  };
}

function convertMeasurementValue(value: number, fromUnit: string | undefined, toUnit: string, kind: MeasurementKind) {
  if (!fromUnit || fromUnit === toUnit) return value;
  if (kind === 'dimension') {
    const cmValue = fromUnit === 'in' ? value * 2.54 : value;
    return toUnit === 'in' ? cmValue / 2.54 : cmValue;
  }
  let kgValue = value;
  if (fromUnit === 'g') {
    kgValue = value / 1000;
  } else if (fromUnit === 'lb') {
    kgValue = value / 2.2046226218;
  }
  return toUnit === 'lb' ? kgValue * 2.2046226218 : kgValue;
}

function stripTrailingZero(value: string) {
  return value.replace(/(\.\d*?[1-9])0+$/, '$1').replace(/\.0+$/, '');
}

function formatMeasurementNumber(value: number) {
  return stripTrailingZero(value.toFixed(4));
}

function toOptionalNumber(value: string | number | null | undefined) {
  if (value === undefined || value === null || value === '') return undefined;
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

function readMeasurementInputValue(value: string | undefined, unit: string, kind: MeasurementKind) {
  const parsed = parseMeasurementValue(value);
  if (!parsed) return undefined;
  return Number(formatMeasurementNumber(convertMeasurementValue(parsed.value, parsed.unit, unit, kind)));
}

function formatMeasurementInputValue(value: string | number | null | undefined, unit: string) {
  const numberValue = toOptionalNumber(value);
  return numberValue === undefined ? undefined : `${formatMeasurementNumber(numberValue)} ${unit}`;
}

function inferMeasurementUnitSystem(rows: SkuRow[]): MeasurementUnitSystem {
  const values = rows.flatMap((row) => [
    row.lengthValue,
    row.widthValue,
    row.heightValue,
    row.weight,
  ]);
  return values.some((value) => {
    const unit = parseMeasurementValue(value)?.unit;
    return unit === 'in' || unit === 'lb';
  }) ? 'IMPERIAL' : 'METRIC';
}

function normalizeMeasurementField(
  value: string | undefined,
  unitSystem: MeasurementUnitSystem,
  kind: MeasurementKind,
) {
  const unit = kind === 'dimension' ? measurementUnits[unitSystem].dimension : measurementUnits[unitSystem].weight;
  const numberValue = readMeasurementInputValue(value, unit, kind);
  return numberValue === undefined ? undefined : formatMeasurementInputValue(numberValue, unit);
}

function normalizeSkuMeasurements(row: SkuRow, unitSystem: MeasurementUnitSystem): SkuRow {
  return {
    ...row,
    lengthValue: normalizeMeasurementField(row.lengthValue, unitSystem, 'dimension'),
    widthValue: normalizeMeasurementField(row.widthValue, unitSystem, 'dimension'),
    heightValue: normalizeMeasurementField(row.heightValue, unitSystem, 'dimension'),
    weight: normalizeMeasurementField(row.weight, unitSystem, 'weight'),
  };
}

function hasMeasurementChange(current: SkuRow[], next: SkuRow[]) {
  return current.some((row, index) => {
    const nextRow = next[index];
    if (!nextRow) return true;
    return measurementFields.some((field) => row[field] !== nextRow[field]);
  });
}

export default function SkuMatrixEditor({
  value,
  focusSkuId,
  currencyCode,
  currencyLabel,
  sourceMode = false,
  onChange,
}: SkuMatrixEditorProps) {
  const [selectedSpecs, setSelectedSpecs] = useState<(keyof API.ProductDistribution.Sku)[]>(['color', 'size']);
  const [specValues, setSpecValues] = useState<Record<string, string[]>>({});
  const [draftValues, setDraftValues] = useState<Record<string, string>>({});
  const [bulkLengthValue, setBulkLengthValue] = useState<number>();
  const [bulkWidthValue, setBulkWidthValue] = useState<number>();
  const [bulkHeightValue, setBulkHeightValue] = useState<number>();
  const [bulkWeight, setBulkWeight] = useState<number>();
  const [unitSystem, setUnitSystem] = useState<MeasurementUnitSystem>('METRIC');
  const [bulkSupplyPrice, setBulkSupplyPrice] = useState<number>();
  const hydratedRowsKeyRef = useRef<string | undefined>(undefined);
  const currentUnits = measurementUnits[unitSystem];

  useEffect(() => {
    const rowsKey = value.map((row) => row.skuId || row.rowKey).join('|');
    if (rowsKey === hydratedRowsKeyRef.current) return;
    hydratedRowsKeyRef.current = rowsKey;
    const nextUnitSystem = inferMeasurementUnitSystem(value);
    setUnitSystem(nextUnitSystem);
    const selected = inferSelectedSpecs(value);
    const values: Record<string, string[]> = {};
    selected.forEach((field) => {
      values[field] = unique(value.map((row) => row[field] as string));
    });
    setSelectedSpecs(selected);
    setSpecValues(values);
    const normalizedRows = value.map((row) => normalizeSkuMeasurements(row, nextUnitSystem));
    if (hasMeasurementChange(value, normalizedRows)) {
      onChange(normalizedRows);
    }
  }, [value]);

  const rows = useMemo(
    () => value.map((row) => ({ ...row, rowKey: row.rowKey || String(row.skuId || makeRowKey()) })),
    [value],
  );

  useEffect(() => {
    if (!focusSkuId) return undefined;
    const timer = window.setTimeout(() => {
      document.querySelector(`.${styles.focusSkuRow}`)?.scrollIntoView({ block: 'center' });
    }, 120);
    return () => window.clearTimeout(timer);
  }, [focusSkuId, rows.length]);

  const specCheckboxOptions = useMemo(
    () => skuSpecFields.map((field) => ({
      ...field,
      disabled: selectedSpecs.length >= maxSelectedSpecCount && !selectedSpecs.includes(field.value),
    })),
    [selectedSpecs],
  );

  const updateRow = (rowKey: string, patch: Partial<SkuRow>) => {
    onChange(rows.map((row) => (row.rowKey === rowKey ? { ...row, ...patch } : row)));
  };

  const changeSelectedSpecs = (checked: (keyof API.ProductDistribution.Sku)[]) => {
    setSelectedSpecs(checked.slice(0, maxSelectedSpecCount));
  };

  const changeUnitSystem = (nextUnitSystem: MeasurementUnitSystem) => {
    if (nextUnitSystem === unitSystem) return;
    setUnitSystem(nextUnitSystem);
    onChange(rows.map((row) => normalizeSkuMeasurements(row, nextUnitSystem)));
  };

  const updateMeasurementRow = (
    row: SkuRow,
    field: MeasurementField,
    value: string | number | null,
    kind: MeasurementKind,
  ) => {
    const unit = kind === 'dimension' ? currentUnits.dimension : currentUnits.weight;
    updateRow(row.rowKey!, { [field]: formatMeasurementInputValue(value, unit) } as Partial<SkuRow>);
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
      lengthValue: bulkLengthValue === undefined ? row.lengthValue : formatMeasurementInputValue(bulkLengthValue, currentUnits.dimension),
      widthValue: bulkWidthValue === undefined ? row.widthValue : formatMeasurementInputValue(bulkWidthValue, currentUnits.dimension),
      heightValue: bulkHeightValue === undefined ? row.heightValue : formatMeasurementInputValue(bulkHeightValue, currentUnits.dimension),
      weight: bulkWeight === undefined ? row.weight : formatMeasurementInputValue(bulkWeight, currentUnits.weight),
      supplyPrice: bulkSupplyPrice ?? row.supplyPrice,
    })));
  };

  const columns: ColumnsType<SkuRow> = [
    ...(sourceMode ? [
      {
        title: '来源SKU',
        dataIndex: 'masterSku',
        width: 150,
        fixed: 'left' as const,
        render: (_: unknown, row: SkuRow) => row.masterSku || '-',
      },
      {
        title: '来源商品',
        dataIndex: 'masterProductNameSnapshot',
        width: 220,
        render: (_: unknown, row: SkuRow) => row.masterProductNameSnapshot || '-',
      },
      {
        title: '来源仓',
        dataIndex: 'sourceWarehouseNames',
        width: 180,
        render: (_: unknown, row: SkuRow) => row.sourceWarehouseNames || '-',
      },
    ] : []),
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
      title: `长度 (${currentUnits.dimension})`,
      dataIndex: 'lengthValue',
      width: 136,
      render: (_, row) => (
        <MeasurementInput
          value={readMeasurementInputValue(row.lengthValue, currentUnits.dimension, 'dimension')}
          unit={currentUnits.dimension}
          placeholder="如 30"
          disabled={sourceMode}
          onChange={(nextValue) => updateMeasurementRow(row, 'lengthValue', nextValue, 'dimension')}
        />
      ),
    },
    {
      title: `宽度 (${currentUnits.dimension})`,
      dataIndex: 'widthValue',
      width: 136,
      render: (_, row) => (
        <MeasurementInput
          value={readMeasurementInputValue(row.widthValue, currentUnits.dimension, 'dimension')}
          unit={currentUnits.dimension}
          placeholder="如 20"
          disabled={sourceMode}
          onChange={(nextValue) => updateMeasurementRow(row, 'widthValue', nextValue, 'dimension')}
        />
      ),
    },
    {
      title: `高度 (${currentUnits.dimension})`,
      dataIndex: 'heightValue',
      width: 136,
      render: (_, row) => (
        <MeasurementInput
          value={readMeasurementInputValue(row.heightValue, currentUnits.dimension, 'dimension')}
          unit={currentUnits.dimension}
          placeholder="如 8"
          disabled={sourceMode}
          onChange={(nextValue) => updateMeasurementRow(row, 'heightValue', nextValue, 'dimension')}
        />
      ),
    },
    {
      title: `重量 (${currentUnits.weight})`,
      dataIndex: 'weight',
      width: 136,
      render: (_, row) => (
        <MeasurementInput
          value={readMeasurementInputValue(row.weight, currentUnits.weight, 'weight')}
          unit={currentUnits.weight}
          placeholder="如 0.35"
          disabled={sourceMode}
          onChange={(nextValue) => updateMeasurementRow(row, 'weight', nextValue, 'weight')}
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
      title: '币种',
      dataIndex: 'currencyCode',
      width: 120,
      render: () => currencyLabel || currencyCode || '-',
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
          options={specCheckboxOptions}
          onChange={(checked) => changeSelectedSpecs(checked as (keyof API.ProductDistribution.Sku)[])}
        />
        {!sourceMode ? (
          <>
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
          </>
        ) : null}
      </div>

      <div className={styles.bulkPanel}>
        <Space size={8} wrap>
          {!sourceMode ? (
            <>
              <Typography.Text strong className={styles.bulkPanelLabel}>
                单位制
              </Typography.Text>
              <Radio.Group
                optionType="button"
                buttonStyle="solid"
                options={compactMeasurementUnitOptions}
                value={unitSystem}
                onChange={(event) => changeUnitSystem(event.target.value)}
              />
              <Typography.Text strong className={styles.bulkPanelLabel}>
                批量填充
              </Typography.Text>
              <MeasurementInput
                placeholder="长"
                style={{ width: 124 }}
                value={bulkLengthValue}
                unit={currentUnits.dimension}
                onChange={(nextValue) => setBulkLengthValue(toOptionalNumber(nextValue))}
              />
              <MeasurementInput
                placeholder="宽"
                style={{ width: 124 }}
                value={bulkWidthValue}
                unit={currentUnits.dimension}
                onChange={(nextValue) => setBulkWidthValue(toOptionalNumber(nextValue))}
              />
              <MeasurementInput
                placeholder="高"
                style={{ width: 124 }}
                value={bulkHeightValue}
                unit={currentUnits.dimension}
                onChange={(nextValue) => setBulkHeightValue(toOptionalNumber(nextValue))}
              />
              <MeasurementInput
                placeholder="重量"
                style={{ width: 132 }}
                value={bulkWeight}
                unit={currentUnits.weight}
                onChange={(nextValue) => setBulkWeight(toOptionalNumber(nextValue))}
              />
            </>
          ) : (
            <Typography.Text strong className={styles.bulkPanelLabel}>
              批量填充
            </Typography.Text>
          )}
          <InputNumber
            min={0}
            precision={4}
            placeholder="供货价"
            style={{ width: 140 }}
            value={bulkSupplyPrice}
            onChange={(value) => setBulkSupplyPrice(value ?? undefined)}
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
        rowClassName={(row) => (focusSkuId && row.skuId === focusSkuId ? styles.focusSkuRow : '')}
        pagination={false}
        scroll={{ x: 1400 }}
        size="small"
      />
    </div>
  );
}
