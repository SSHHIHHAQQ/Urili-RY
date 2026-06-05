import { Descriptions, Drawer, Image, Space, Tag, Typography } from 'antd';
import {
  approveStatusText,
  dangerousCargoText,
  dimensionText,
  displayNumber,
  displayPrice,
  displayText,
  joinText,
  jsonArrayCount,
  productTypeText,
  weightText,
} from './constants';
import styles from './style.module.css';

type SourceProductDetailDrawerProps = {
  open: boolean;
  record?: API.Integration.SourceProductItem;
  onClose: () => void;
};

function sourceTag(record?: API.Integration.SourceProductItem) {
  if (!record) {
    return '-';
  }
  return (
    <Space size={4} wrap>
      <Tag color="blue">{record.systemKindLabel || record.systemKind || '-'}</Tag>
      <Typography.Text>{record.masterWarehouseName || '-'}</Typography.Text>
      <Typography.Text type="secondary">{record.connectionCode}</Typography.Text>
    </Space>
  );
}

export default function SourceProductDetailDrawer({
  open,
  record,
  onClose,
}: SourceProductDetailDrawerProps) {
  return (
    <Drawer
      size={760}
      title={record?.masterSku || '来源商品详情'}
      open={open}
      onClose={onClose}
      destroyOnHidden
    >
      <div className={styles.detailLayout}>
        <div className={styles.detailImageBox}>
          {record?.imageUrl ? (
            <Image src={record.imageUrl} className={styles.detailImage} />
          ) : (
            <div className={styles.emptyImage}>无图</div>
          )}
        </div>
        <Descriptions
          size="small"
          column={1}
          className={styles.detailSummary}
        >
          <Descriptions.Item label="来源">{sourceTag(record)}</Descriptions.Item>
          <Descriptions.Item label="来源 SKU">
            <Typography.Text copyable>{displayText(record?.masterSku)}</Typography.Text>
          </Descriptions.Item>
          <Descriptions.Item label="商品名称">
            {displayText(record?.masterProductName)}
          </Descriptions.Item>
          <Descriptions.Item label="别名">
            {displayText(record?.productAliasName)}
          </Descriptions.Item>
          <Descriptions.Item label="描述">
            <Typography.Paragraph className={styles.descriptionText}>
              {displayText(record?.productDescription)}
            </Typography.Paragraph>
          </Descriptions.Item>
        </Descriptions>
      </div>

      <Descriptions title="基础信息" size="small" column={2}>
        <Descriptions.Item label="产品类型">
          {record?.productType === undefined || record?.productType === null
            ? '-'
            : productTypeText[record.productType] || record.productType}
        </Descriptions.Item>
        <Descriptions.Item label="审核状态">
          {record?.approveStatus
            ? approveStatusText[record.approveStatus] || record.approveStatus
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="同步状态">
          {displayText(record?.status)}
        </Descriptions.Item>
        <Descriptions.Item label="配对状态">
          {record?.pairingStatus === 'PAIRED' ? '已配对' : '未配对'}
        </Descriptions.Item>
        <Descriptions.Item label="首次发现">
          {displayText(record?.firstSeenTime)}
        </Descriptions.Item>
        <Descriptions.Item label="同步时间">
          {displayText(record?.lastSeenTime)}
        </Descriptions.Item>
      </Descriptions>

      <Descriptions title="识别码与分类" size="small" column={2}>
        <Descriptions.Item label="主识别码">
          {displayText(record?.mainCode)}
        </Descriptions.Item>
        <Descriptions.Item label="FNSKU">
          {displayText(record?.fnsku)}
        </Descriptions.Item>
        <Descriptions.Item label="其他条码" span={2}>
          {displayText(record?.otherCode)}
        </Descriptions.Item>
        <Descriptions.Item label="分类" span={2}>
          {joinText([record?.cat1Name, record?.cat2Name, record?.cat3Name])}
        </Descriptions.Item>
      </Descriptions>

      <Descriptions title="尺寸重量" size="small" column={2}>
        <Descriptions.Item label="产品尺寸">
          {dimensionText(record)}
        </Descriptions.Item>
        <Descriptions.Item label="产品重量">
          {weightText(record)}
        </Descriptions.Item>
        <Descriptions.Item label="英制尺寸">
          {record?.lengthBs || record?.widthBs || record?.heightBs
            ? `${displayNumber(record.lengthBs)} x ${displayNumber(record.widthBs)} x ${displayNumber(record.heightBs)} in`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="英制重量">
          {displayNumber(record?.weightBs, ' lb')}
        </Descriptions.Item>
        <Descriptions.Item label="WMS尺寸">
          {record?.wmsLength || record?.wmsWidth || record?.wmsHeight
            ? `${displayNumber(record.wmsLength)} x ${displayNumber(record.wmsWidth)} x ${displayNumber(record.wmsHeight)} cm`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="WMS重量">
          {displayNumber(record?.wmsWeight, ' kg')}
        </Descriptions.Item>
      </Descriptions>

      <Descriptions title="申报信息" size="small" column={2}>
        <Descriptions.Item label="申报中文名">
          {displayText(record?.declareNameCn)}
        </Descriptions.Item>
        <Descriptions.Item label="申报英文名">
          {displayText(record?.declareNameEn)}
        </Descriptions.Item>
        <Descriptions.Item label="海关编码">
          {displayText(record?.customhouseCode)}
        </Descriptions.Item>
        <Descriptions.Item label="申报价">
          {displayPrice(record?.declarePrice, record?.currencyCode)}
        </Descriptions.Item>
        <Descriptions.Item label="原产国家/地区">
          {displayText(record?.countryOfOriginName)}
        </Descriptions.Item>
        <Descriptions.Item label="危险品">
          {record?.dangerousCargo === undefined || record?.dangerousCargo === null
            ? '-'
            : dangerousCargoText[record.dangerousCargo] || record.dangerousCargo}
        </Descriptions.Item>
      </Descriptions>

      <Descriptions title="商城配对" size="small" column={2}>
        <Descriptions.Item label="客户">
          {displayText(record?.customerName)}
        </Descriptions.Item>
        <Descriptions.Item label="系统 SKU">
          {displayText(record?.systemSku)}
        </Descriptions.Item>
        <Descriptions.Item label="系统商品名" span={2}>
          {displayText(record?.systemSkuName)}
        </Descriptions.Item>
      </Descriptions>

      <Descriptions title="来源快照" size="small" column={2}>
        <Descriptions.Item label="平台 SKU 信息">
          {jsonArrayCount(record?.platformSkuInfoJson)} 条
        </Descriptions.Item>
        <Descriptions.Item label="巴西税务信息">
          {jsonArrayCount(record?.brazilTaxInfoJson)} 条
        </Descriptions.Item>
        <Descriptions.Item label="快照 Hash" span={2}>
          <Typography.Text copyable className={styles.hashText}>
            {displayText(record?.sourcePayloadHash)}
          </Typography.Text>
        </Descriptions.Item>
      </Descriptions>
    </Drawer>
  );
}
