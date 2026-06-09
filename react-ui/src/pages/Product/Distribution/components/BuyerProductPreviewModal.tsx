import { SafetyCertificateOutlined } from '@ant-design/icons';
import { Button, Empty, Image, InputNumber, Modal, Space, Table, Tabs, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { resolveResourceUrl, skuSpecFields } from '../constants';
import type { DetailContentBlock } from '../detailContent';
import styles from '../style.module.css';

export type BuyerPreviewWarehouse = {
  key: string;
  name: string;
  code?: string;
  kind?: string;
  kindLabel?: string;
  currencyCode?: string;
  stockText?: string;
  deliveryText: string;
};

export type BuyerPreviewAttribute = {
  label: string;
  value?: string;
};

export type BuyerPreviewSku = {
  skuId?: number;
  spuId?: number;
  rowKey?: string;
  sourceDimensionGroupKey?: string;
  systemSkuCode?: string;
  color?: string;
  size?: string;
  material?: string;
  style?: string;
  model?: string;
  packageQuantity?: string;
  capacity?: string;
  lengthValue?: string;
  widthValue?: string;
  heightValue?: string;
  weight?: string;
  skuImageUrl?: string;
  salePrice?: number;
  currencyCode?: string;
  previewPrice: string;
  previewStock: string;
};

export type BuyerProductPreviewData = {
  productName?: string;
  productNameEn?: string;
  categoryName?: string;
  categoryPath?: string;
  mainImageUrl?: string;
  sellingPoint?: string;
  galleryUrls: string[];
  warehouseKind?: string;
  warehouses: BuyerPreviewWarehouse[];
  skus: BuyerPreviewSku[];
  attributes: BuyerPreviewAttribute[];
  detailBlocks: DetailContentBlock[];
};

type BuyerProductPreviewModalProps = {
  open: boolean;
  data?: BuyerProductPreviewData;
  mode?: 'preview' | 'real';
  footer?: ReactNode;
  onClose: () => void;
};

const specFields = skuSpecFields.map((item) => String(item.value));

type BuyerSpecGroup = {
  field: string;
  label: string;
  values: string[];
};

function displayText(value?: string) {
  return value?.trim() || '--';
}

function imageUrl(value?: string) {
  return value ? resolveResourceUrl(value) : '';
}

function buildImageList(data?: BuyerProductPreviewData, selectedSku?: BuyerPreviewSku) {
  const images = [
    selectedSku?.skuImageUrl,
    data?.mainImageUrl,
    ...(data?.galleryUrls || []),
  ].filter(Boolean) as string[];
  return Array.from(new Set(images));
}

function getSkuSpecValue(sku: BuyerPreviewSku, field: string) {
  const value = sku[field as keyof BuyerPreviewSku];
  return typeof value === 'string' ? value.trim() : '';
}

function getSpecValue(sku: BuyerPreviewSku, field: string) {
  return getSkuSpecValue(sku, field);
}

function getSkuKey(sku: BuyerPreviewSku) {
  return String(sku.skuId || sku.rowKey || sku.sourceDimensionGroupKey);
}

function isSkuMatchedBySpecs(sku: BuyerPreviewSku, specs: Record<string, string>) {
  return Object.entries(specs).every(([field, value]) => !value || getSpecValue(sku, field) === value);
}

function findFirstSkuBySpecs(skus: BuyerPreviewSku[], specs: Record<string, string>) {
  return skus.find((sku) => isSkuMatchedBySpecs(sku, specs));
}

function isColorSpecGroup(group: BuyerSpecGroup) {
  return group.field === 'color' || group.label === '颜色';
}

function buildSpecGroups(skus: BuyerPreviewSku[]): BuyerSpecGroup[] {
  return specFields.flatMap((field) => {
    const values = Array.from(new Set(skus.map((sku) => getSkuSpecValue(sku, field)).filter(Boolean)));
    if (!values.length) return [];
    const label = skuSpecFields.find((item) => item.value === field)?.label || String(field);
    return [{ field: String(field), label, values }];
  });
}

function isSpecOptionAvailable(
  skus: BuyerPreviewSku[],
  groups: BuyerSpecGroup[],
  selectedSpecs: Record<string, string>,
  groupIndex: number,
  value: string,
) {
  const group = groups[groupIndex];
  if (!group) return false;
  return skus.some((sku) => {
    if (getSpecValue(sku, group.field) !== value) return false;
    return groups.slice(0, groupIndex).every((previousGroup) => {
      const selectedValue = selectedSpecs[previousGroup.field];
      return !selectedValue || getSpecValue(sku, previousGroup.field) === selectedValue;
    });
  });
}

function getAvailableValuesForGroup(
  skus: BuyerPreviewSku[],
  specs: Record<string, string>,
  group: BuyerSpecGroup,
) {
  return group.values.filter((value) => findFirstSkuBySpecs(skus, { ...specs, [group.field]: value }));
}

function buildNextSelectedSpecs(
  skus: BuyerPreviewSku[],
  groups: BuyerSpecGroup[],
  selectedSpecs: Record<string, string>,
  field: string,
  value: string,
) {
  const groupIndex = groups.findIndex((group) => group.field === field);
  if (groupIndex < 0) return selectedSpecs;

  const nextSpecs: Record<string, string> = {};
  groups.slice(0, groupIndex).forEach((group) => {
    const previousValue = selectedSpecs[group.field];
    if (previousValue) {
      nextSpecs[group.field] = previousValue;
    }
  });
  nextSpecs[field] = value;

  groups.slice(groupIndex + 1).forEach((group) => {
    const availableValues = getAvailableValuesForGroup(skus, nextSpecs, group);
    if (availableValues.length === 1) {
      nextSpecs[group.field] = availableValues[0];
    }
  });

  return nextSpecs;
}

function findSkuForSpecOption(
  skus: BuyerPreviewSku[],
  groups: BuyerSpecGroup[],
  selectedSpecs: Record<string, string>,
  groupIndex: number,
  value: string,
) {
  const group = groups[groupIndex];
  if (!group) return undefined;
  const specs: Record<string, string> = { [group.field]: value };
  groups.slice(0, groupIndex).forEach((previousGroup) => {
    const selectedValue = selectedSpecs[previousGroup.field];
    if (selectedValue) {
      specs[previousGroup.field] = selectedValue;
    }
  });
  return findFirstSkuBySpecs(skus, specs) || skus.find((sku) => getSpecValue(sku, group.field) === value);
}

function buildDimensionText(sku?: BuyerPreviewSku) {
  if (!sku) return '';
  const dimension = [sku.lengthValue, sku.widthValue, sku.heightValue].filter(Boolean).join(' x ');
  return [dimension, sku.weight].filter(Boolean).join(' / ');
}

function OfficialWarehouseBadge() {
  return (
    <div className={styles.buyerOfficialHero}>
      <div className={styles.buyerOfficialIcon}>
        <SafetyCertificateOutlined />
      </div>
      <div className={styles.buyerOfficialBody}>
        <div className={styles.buyerOfficialTitle}>平台官方仓</div>
        <div className={styles.buyerOfficialSubtitle}>平台验仓 · 库存同步 · 优先履约 · 售后保障</div>
        <div className={styles.buyerOfficialTags}>
          <Tag color="blue">官方认证</Tag>
          <Tag color="cyan">现货履约</Tag>
          <Tag color="green">稳定发货</Tag>
        </div>
      </div>
    </div>
  );
}

function DetailBlocks({ blocks }: { blocks: DetailContentBlock[] }) {
  if (!blocks.length) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无商品详情" />;
  }
  return (
    <div className={styles.buyerDetailFlow}>
      {blocks.map((block) => {
        if (block.type === 'TEXT') {
          return (
            <section className={styles.buyerDetailTextSection} key={block.id}>
              {block.title ? <h3>{block.title}</h3> : null}
              <p>{block.text || '--'}</p>
            </section>
          );
        }
        if (block.type === 'IMAGE') {
          return block.imageUrl ? (
            <div className={styles.buyerDetailImageBlock} key={block.id}>
              <img src={imageUrl(block.imageUrl)} alt={block.title || '商品详情图'} />
            </div>
          ) : null;
        }
        if (block.type === 'IMAGE_TEXT') {
          return (
            <section className={styles.buyerImageTextSection} key={block.id}>
              {block.imageUrl ? <img src={imageUrl(block.imageUrl)} alt={block.title || '商品详情图'} /> : <div className={styles.buyerImagePlaceholder}>图片</div>}
              <div>
                {block.title ? <h3>{block.title}</h3> : null}
                <p>{block.text || '--'}</p>
              </div>
            </section>
          );
        }
        return (
          <div className={styles.buyerDetailParamTable} key={block.id}>
            {(block.rows || []).map((row) => (
              <div className={styles.buyerParamRow} key={row.id}>
                <span>{row.name || '--'}</span>
                <span>{row.value || '--'}</span>
              </div>
            ))}
          </div>
        );
      })}
    </div>
  );
}

const warehouseColumns: ColumnsType<BuyerPreviewWarehouse> = [
  {
    title: '仓库类型',
    dataIndex: 'kind',
    width: 110,
    render: (_, record) =>
      record.kind === 'official'
        ? <Tag color="processing">平台官方仓</Tag>
        : <Tag>{record.kindLabel || '三方仓'}</Tag>,
  },
  {
    title: '仓库名称',
    dataIndex: 'name',
    width: 180,
    render: (value) => value || '--',
  },
  {
    title: '仓库编码',
    dataIndex: 'code',
    width: 120,
    render: (value) => value || '--',
  },
  {
    title: '币种',
    dataIndex: 'currencyCode',
    width: 90,
    render: (value) => value || '--',
  },
  {
    title: '库存',
    dataIndex: 'stockText',
    width: 130,
    render: (value) => value || '--',
  },
  {
    title: '发货说明',
    dataIndex: 'deliveryText',
    render: (value) => value || '--',
  },
];

export default function BuyerProductPreviewModal({
  open,
  data,
  mode = 'preview',
  footer,
  onClose,
}: BuyerProductPreviewModalProps) {
  const [activeSkuKey, setActiveSkuKey] = useState<string>();
  const [selectedSpecs, setSelectedSpecs] = useState<Record<string, string>>({});
  const [activeImage, setActiveImage] = useState<string>();
  const [selectedWarehouseKey, setSelectedWarehouseKey] = useState<string>();
  const [quantity, setQuantity] = useState<number>(1);
  const isPreviewMode = mode === 'preview';
  const isOfficial = data?.warehouseKind === 'official';
  const skus = data?.skus || [];
  const specGroups = useMemo(() => buildSpecGroups(skus), [skus]);
  const selectedSku = useMemo(
    () => skus.find((sku) => getSkuKey(sku) === activeSkuKey) || findFirstSkuBySpecs(skus, selectedSpecs) || skus[0],
    [activeSkuKey, selectedSpecs, skus],
  );
  const images = useMemo(() => buildImageList(data, selectedSku), [data, selectedSku]);
  const selectedWarehouse = data?.warehouses.find((item) => item.key === selectedWarehouseKey) || data?.warehouses[0];
  const parameterRows = [
    ...(data?.attributes || []),
    ...specGroups.map((group) => ({ label: group.label, value: group.values.join(' / ') })),
    { label: '尺寸重量', value: buildDimensionText(selectedSku) || (isPreviewMode ? '42.00 x 42.00 x 17.00 cm / 920 g' : '--') },
    { label: '发货方式', value: selectedWarehouse?.deliveryText || (isPreviewMode ? '仓库现货发货 / 运费下单时计算' : '发货仓库待确认') },
  ].filter((item) => item.value);

  useEffect(() => {
    if (!open || !data) return;
    const firstSku = data.skus[0];
    setActiveSkuKey(firstSku ? getSkuKey(firstSku) : undefined);
    const initialSpecs: Record<string, string> = {};
    specFields.forEach((field) => {
      const value = firstSku ? getSkuSpecValue(firstSku, field) : '';
      if (value) initialSpecs[String(field)] = value;
    });
    setSelectedSpecs(initialSpecs);
    setActiveImage(firstSku?.skuImageUrl || data.mainImageUrl || data.galleryUrls[0]);
    setSelectedWarehouseKey(data.warehouses[0]?.key);
    setQuantity(1);
  }, [open, data]);

  useEffect(() => {
    if (selectedSku?.skuImageUrl) {
      setActiveImage(selectedSku.skuImageUrl);
    }
  }, [selectedSku?.skuImageUrl]);

  const selectSpec = (field: string, value: string) => {
    const nextSpecs = buildNextSelectedSpecs(skus, specGroups, selectedSpecs, field, value);
    const matchedSku = findFirstSkuBySpecs(skus, nextSpecs);
    setSelectedSpecs(nextSpecs);
    if (matchedSku) {
      setActiveSkuKey(getSkuKey(matchedSku));
    }
  };

  const modalFooter = footer !== undefined ? footer : (
    isPreviewMode ? (
      <Space>
        <Button type="primary" size="large">填写下单信息</Button>
        <Button size="large">加入采购单</Button>
        <Button size="large" disabled>提交订单</Button>
      </Space>
    ) : null
  );

  return (
    <Modal
      title={(
        <Space>
          <span>{isPreviewMode ? '买家商品详情预览' : '商品详情'}</span>
          {isPreviewMode ? <Tag color="processing">预览模式</Tag> : null}
        </Space>
      )}
      open={open}
      width={1180}
      style={{ top: 24 }}
      footer={modalFooter}
      destroyOnClose
      onCancel={onClose}
    >
      {isPreviewMode ? <div className={styles.buyerPreviewNotice}>数据仅用于预览，未发布到买家端。</div> : null}
      <div className={styles.buyerPreviewShell}>
        <div className={styles.buyerPreviewTop}>
          <div className={styles.buyerGallery}>
            <div className={styles.buyerThumbs}>
              {images.map((url) => (
                <button
                  className={`${styles.buyerThumb} ${activeImage === url ? styles.buyerThumbActive : ''}`}
                  key={url}
                  type="button"
                  onClick={() => setActiveImage(url)}
                >
                  <img src={imageUrl(url)} alt="商品缩略图" />
                </button>
              ))}
            </div>
            <div className={styles.buyerMainImage}>
              {activeImage ? <Image src={imageUrl(activeImage)} /> : <div className={styles.buyerImagePlaceholder}>暂无图片</div>}
            </div>
          </div>

          <div className={styles.buyerInfoPanel}>
            <div className={styles.buyerCategoryLine}>{displayText(data?.categoryPath || data?.categoryName)}</div>
            <Typography.Title level={4} className={styles.buyerPreviewTitle}>
              {displayText(data?.productName)}
            </Typography.Title>
            <Typography.Text type="secondary">{displayText(data?.productNameEn)}</Typography.Text>
            {data?.sellingPoint ? <Typography.Paragraph>{data.sellingPoint}</Typography.Paragraph> : null}

            {isOfficial ? <OfficialWarehouseBadge /> : null}

            <div className={styles.buyerPricePanel}>
              <span className={styles.buyerPriceLabel}>商品价格</span>
              <span className={styles.buyerPrice}>{selectedSku?.previewPrice || (isPreviewMode ? '¥199.00' : '--')}</span>
              {isPreviewMode ? <Tag color="orange">样式预览价</Tag> : null}
            </div>

            <div className={styles.buyerPurchaseGrid}>
              {specGroups.map((group, groupIndex) => (
                <div className={styles.buyerOptionRow} key={group.field}>
                  <div className={styles.buyerOptionLabel}>{group.label}</div>
                  <Space wrap>
                    {group.values.map((value) => {
                      const disabled = !isSpecOptionAvailable(skus, specGroups, selectedSpecs, groupIndex, value);
                      const shouldShowImage = isColorSpecGroup(group);
                      const optionSku = shouldShowImage
                        ? findSkuForSpecOption(skus, specGroups, selectedSpecs, groupIndex, value)
                        : undefined;
                      const optionImageUrl = imageUrl(optionSku?.skuImageUrl);
                      const button = (
                        <Button
                          key={value}
                          aria-label={shouldShowImage ? `${group.label}：${value}` : undefined}
                          className={shouldShowImage ? styles.buyerColorOptionButton : undefined}
                          disabled={disabled}
                          title={shouldShowImage ? value : undefined}
                          type={selectedSpecs[group.field] === value ? 'primary' : 'default'}
                          onClick={() => selectSpec(group.field, value)}
                        >
                          {shouldShowImage ? (
                            optionImageUrl ? (
                              <img className={styles.buyerColorOptionImage} src={optionImageUrl} alt={value} />
                            ) : (
                              <span className={styles.buyerColorOptionPlaceholder}>无图</span>
                            )
                          ) : value}
                        </Button>
                      );
                      return (
                        shouldShowImage
                          ? <Tooltip key={value} title={optionImageUrl ? value : `${value}：未上传 SKU 图`}>{button}</Tooltip>
                          : button
                      );
                    })}
                  </Space>
                </div>
              ))}

              <div className={styles.buyerOptionRow}>
                <div className={styles.buyerOptionLabel}>发货仓库</div>
                <Space wrap>
                  {(data?.warehouses || []).map((warehouse) => (
                    <Button
                      key={warehouse.key}
                      className={warehouse.kind === 'official' ? styles.buyerOfficialWarehouseButton : undefined}
                      type={selectedWarehouse?.key === warehouse.key ? 'primary' : 'default'}
                      onClick={() => setSelectedWarehouseKey(warehouse.key)}
                    >
                      {warehouse.name}
                    </Button>
                  ))}
                </Space>
              </div>

              <div className={styles.buyerOptionRow}>
                <div className={styles.buyerOptionLabel}>库存</div>
                <Space>
                  <Typography.Text strong>
                    {selectedSku?.previewStock || selectedWarehouse?.stockText || (isPreviewMode ? '现货 128 件' : '按SKU库存展示')}
                  </Typography.Text>
                  <Tag color="success">
                    {selectedWarehouse?.deliveryText || (isPreviewMode ? '预计 2-5 个工作日发货' : '发货仓库待确认')}
                  </Tag>
                </Space>
              </div>

              <div className={styles.buyerOptionRow}>
                <div className={styles.buyerOptionLabel}>数量</div>
                <InputNumber min={1} value={quantity} onChange={(value) => setQuantity(value || 1)} />
              </div>
            </div>
          </div>
        </div>

        <Tabs
          className={styles.buyerPreviewTabs}
          items={[
            {
              key: 'detail',
              label: '商品详情',
              children: <DetailBlocks blocks={data?.detailBlocks || []} />,
            },
            {
              key: 'params',
              label: '商品参数',
              children: (
                <div className={styles.buyerParamTable}>
                  {parameterRows.map((row) => (
                    <div className={styles.buyerParamRow} key={row.label}>
                      <span>{row.label}</span>
                      <span>{row.value}</span>
                    </div>
                  ))}
                </div>
              ),
            },
            {
              key: 'warehouses',
              label: '发货仓库',
              children: (
                <div className={styles.buyerWarehouseTab}>
                  {isOfficial ? <OfficialWarehouseBadge /> : null}
                  <Table<BuyerPreviewWarehouse>
                    rowKey="key"
                    size="small"
                    pagination={false}
                    columns={warehouseColumns}
                    dataSource={data?.warehouses || []}
                    locale={{ emptyText: '暂无发货仓库' }}
                  />
                </div>
              ),
            },
          ]}
        />
      </div>
    </Modal>
  );
}
