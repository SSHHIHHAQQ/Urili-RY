import { SafetyCertificateOutlined } from '@ant-design/icons';
import { Button, Empty, Image, InputNumber, Modal, Space, Tabs, Tag, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { resolveResourceUrl, skuSpecFields } from '../constants';
import type { DetailContentBlock } from '../detailContent';
import styles from '../style.module.css';

export type BuyerPreviewWarehouse = {
  key: string;
  name: string;
  kind?: string;
  stockText: string;
  deliveryText: string;
};

export type BuyerPreviewAttribute = {
  label: string;
  value?: string;
};

export type BuyerPreviewSku = API.ProductDistribution.Sku & {
  rowKey?: string;
  previewPrice: string;
  previewStock: string;
};

export type BuyerProductPreviewData = {
  productName?: string;
  productNameEn?: string;
  sellingPoint?: string;
  categoryName?: string;
  mainImageUrl?: string;
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
  onClose: () => void;
};

const specFields = skuSpecFields.map((item) => item.value);

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

function getSkuSpecValue(sku: BuyerPreviewSku, field: keyof API.ProductDistribution.Sku) {
  const value = sku[field];
  return typeof value === 'string' ? value.trim() : '';
}

function isSkuMatched(
  sku: BuyerPreviewSku,
  selectedSpecs: Record<string, string>,
  patch?: { field: string; value: string },
) {
  return specFields.every((field) => {
    const nextValue = patch?.field === field ? patch.value : selectedSpecs[String(field)];
    return !nextValue || getSkuSpecValue(sku, field) === nextValue;
  });
}

function buildSpecGroups(skus: BuyerPreviewSku[]) {
  return specFields.flatMap((field) => {
    const values = Array.from(new Set(skus.map((sku) => getSkuSpecValue(sku, field)).filter(Boolean)));
    if (!values.length) return [];
    const label = skuSpecFields.find((item) => item.value === field)?.label || String(field);
    return [{ field: String(field), label, values }];
  });
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
    <div className={styles.buyerDetailBlocks}>
      {blocks.map((block) => {
        if (block.type === 'TEXT') {
          return (
            <section className={styles.buyerDetailTextBlock} key={block.id}>
              {block.title ? <h3>{block.title}</h3> : null}
              <p>{block.text || '--'}</p>
            </section>
          );
        }
        if (block.type === 'IMAGE') {
          return block.imageUrl ? (
            <Image key={block.id} width="100%" src={imageUrl(block.imageUrl)} />
          ) : null;
        }
        if (block.type === 'IMAGE_TEXT') {
          return (
            <section className={styles.buyerImageTextBlock} key={block.id}>
              {block.imageUrl ? <Image src={imageUrl(block.imageUrl)} /> : <div className={styles.buyerImagePlaceholder}>图片</div>}
              <div>
                {block.title ? <h3>{block.title}</h3> : null}
                <p>{block.text || '--'}</p>
              </div>
            </section>
          );
        }
        return (
          <div className={styles.buyerParamTable} key={block.id}>
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

export default function BuyerProductPreviewModal({ open, data, onClose }: BuyerProductPreviewModalProps) {
  const [activeSkuKey, setActiveSkuKey] = useState<string>();
  const [selectedSpecs, setSelectedSpecs] = useState<Record<string, string>>({});
  const [activeImage, setActiveImage] = useState<string>();
  const [selectedWarehouseKey, setSelectedWarehouseKey] = useState<string>();
  const [quantity, setQuantity] = useState<number>(1);
  const isOfficial = data?.warehouseKind === 'official';
  const skus = data?.skus || [];
  const selectedSku = useMemo(
    () => skus.find((sku) => String(sku.skuId || sku.rowKey || sku.sourceDimensionGroupKey) === activeSkuKey) || skus[0],
    [activeSkuKey, skus],
  );
  const images = useMemo(() => buildImageList(data, selectedSku), [data, selectedSku]);
  const specGroups = useMemo(() => buildSpecGroups(skus), [skus]);
  const selectedWarehouse = data?.warehouses.find((item) => item.key === selectedWarehouseKey) || data?.warehouses[0];
  const parameterRows = [
    ...(data?.attributes || []),
    ...specGroups.map((group) => ({ label: group.label, value: group.values.join(' / ') })),
    { label: '尺寸重量', value: buildDimensionText(selectedSku) || '42.00 x 42.00 x 17.00 cm / 920 g' },
    { label: '发货方式', value: selectedWarehouse?.deliveryText || '仓库现货发货 / 运费下单时计算' },
  ].filter((item) => item.value);

  useEffect(() => {
    if (!open || !data) return;
    const firstSku = data.skus[0];
    setActiveSkuKey(firstSku ? String(firstSku.skuId || firstSku.rowKey || firstSku.sourceDimensionGroupKey) : undefined);
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
    const matchedSku = skus.find((sku) => isSkuMatched(sku, selectedSpecs, { field, value }));
    setSelectedSpecs({ ...selectedSpecs, [field]: value });
    if (matchedSku) {
      setActiveSkuKey(String(matchedSku.skuId || matchedSku.rowKey || matchedSku.sourceDimensionGroupKey));
    }
  };

  return (
    <Modal
      title={(
        <Space>
          <span>买家商品详情预览</span>
          <Tag color="processing">预览模式</Tag>
        </Space>
      )}
      open={open}
      width={1180}
      style={{ top: 24 }}
      footer={(
        <Space>
          <Button type="primary" size="large">填写下单信息</Button>
          <Button size="large">加入采购单</Button>
          <Button size="large" disabled>提交订单</Button>
        </Space>
      )}
      destroyOnClose
      onCancel={onClose}
    >
      <div className={styles.buyerPreviewNotice}>数据仅用于预览，未发布到买家端。</div>
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
            <div className={styles.buyerCategoryLine}>{displayText(data?.categoryName)}</div>
            <Typography.Title level={4} className={styles.buyerPreviewTitle}>
              {displayText(data?.productName)}
            </Typography.Title>
            <Typography.Text type="secondary">{displayText(data?.productNameEn)}</Typography.Text>
            {data?.sellingPoint ? <div className={styles.buyerSellingPoint}>{data.sellingPoint}</div> : null}

            {isOfficial ? <OfficialWarehouseBadge /> : null}

            <div className={styles.buyerPricePanel}>
              <span className={styles.buyerPriceLabel}>商品价格</span>
              <span className={styles.buyerPrice}>{selectedSku?.previewPrice || '¥199.00'}</span>
              <Tag color="orange">样式预览价</Tag>
            </div>

            <div className={styles.buyerPurchaseGrid}>
              {specGroups.map((group) => (
                <div className={styles.buyerOptionRow} key={group.field}>
                  <div className={styles.buyerOptionLabel}>{group.label}</div>
                  <Space wrap>
                    {group.values.map((value) => (
                      <Button
                        key={value}
                        type={selectedSpecs[group.field] === value ? 'primary' : 'default'}
                        onClick={() => selectSpec(group.field, value)}
                      >
                        {value}
                      </Button>
                    ))}
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
                  <Typography.Text strong>{selectedSku?.previewStock || selectedWarehouse?.stockText || '现货 128 件'}</Typography.Text>
                  <Tag color="success">{selectedWarehouse?.deliveryText || '预计 2-5 个工作日发货'}</Tag>
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
          ]}
        />
      </div>
    </Modal>
  );
}
