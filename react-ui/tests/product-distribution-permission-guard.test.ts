import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function expectPureReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export { default } from '${target}';`);
}

function expectPureNamedReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export * from '${target}';`);
}

function expectRouteGuard(source: string, routePath: string, permissions: string[], authorityMode?: string) {
  const blockPattern = new RegExp(
    `path:\\s*['"]${routePath.replaceAll('/', '\\/')}['"][\\s\\S]*?component:\\s*['"]\\.\\/Product\\/Distribution\\/EditPage['"]`,
  );
  const block = source.match(blockPattern)?.[0] || '';
  expect(block).not.toBe('');
  for (const permission of permissions) {
    expect(block).toContain(`'${permission}'`);
  }
  if (authorityMode) {
    expect(block).toContain(`authorityMode: '${authorityMode}'`);
  }
  expect(block).toContain("wrappers: ['@/wrappers/RemoteMenuRouteGuard']");
}

function expectStaticPatchRouteGuard(source: string, routePath: string, permissions: string[], authorityMode?: string) {
  const blockPattern = new RegExp(
    `path:\\s*['"]${routePath.replaceAll('/', '\\/')}['"][\\s\\S]*?pagePath:\\s*['"]Product\\/Distribution\\/EditPage\\.tsx['"][\\s\\S]*?name:`,
  );
  const block = source.match(blockPattern)?.[0] || '';
  expect(block).not.toBe('');
  for (const permission of permissions) {
    expect(block).toContain(`'${permission}'`);
  }
  if (authorityMode) {
    expect(block).toContain(`authorityMode: '${authorityMode}'`);
  }
}

describe('product distribution permission guard', () => {
  it('keeps product page js mirrors as pure re-exports', () => {
    expectPureReExport('src/pages/Product/Category/index.js', './index.tsx');
    expectPureReExport(
      'src/pages/Product/Attribute/components/AttributeLibrary.js',
      './AttributeLibrary.tsx',
    );
    expectPureReExport('src/pages/Product/Distribution/index.js', './index.tsx');
    expectPureReExport('src/pages/Product/Distribution/EditPage.js', './EditPage.tsx');
    expectPureReExport('src/pages/Product/Review/index.js', './index.tsx');
    expectPureNamedReExport('src/services/product/product.js', './product.ts');
    expectPureNamedReExport('src/services/product/distributionProduct.js', './distributionProduct.ts');
    expectPureNamedReExport('src/services/product/productReview.js', './productReview.ts');
  });

  it('guards direct create and edit routes with product distribution permissions', () => {
    const source = readSource('config/routes.ts');
    const sessionSource = readSource('src/services/session.ts');
    expect(readSource('config/routes.js').trim()).toBe("export { default } from './routes.ts';");
    expectRouteGuard(source, '/product/distribution/create', [
      'product:distribution:add',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ], 'all');
    expectRouteGuard(source, '/product/distribution/edit/:spuId', [
      'product:distribution:query',
      'product:distribution:edit',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ], 'all');
    expectStaticPatchRouteGuard(sessionSource, '/product/distribution/create', [
      'product:distribution:add',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ], 'all');
    expectStaticPatchRouteGuard(sessionSource, '/product/distribution/edit/:spuId', [
      'product:distribution:query',
      'product:distribution:edit',
      'seller:admin:list',
      'product:category:list',
      'product:categoryAttribute:preview',
      'warehouse:official:list',
      'warehouse:thirdParty:list',
    ], 'all');
  });

  it('keeps edit entry and edit page actions aligned with the route guard', () => {
    const distributionPageTsx = readSource('src/pages/Product/Distribution/index.tsx');
    const editPageTsx = readSource('src/pages/Product/Distribution/EditPage.tsx');

    expect(distributionPageTsx).toContain('const canMaintainDistributionProductDependencies =');
    expect(distributionPageTsx).toContain("const canPreviewCategorySchema = access.hasPerms('product:categoryAttribute:preview')");
    expect(distributionPageTsx).toContain("const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list')");
    expect(distributionPageTsx).toContain("const canQueryThirdPartyWarehouses = access.hasPerms('warehouse:thirdParty:list')");
    expect(distributionPageTsx).toMatch(/const canMaintainDistributionProductDependencies =[\s\S]*?canQueryOfficialWarehouses[\s\S]*?canQueryThirdPartyWarehouses/);
    expect(distributionPageTsx).toContain('const canCreateDistributionProduct =');
    expect(distributionPageTsx).toContain("access.hasPerms('product:distribution:add') && canMaintainDistributionProductDependencies");
    expect(distributionPageTsx).toMatch(/const canEditDistributionProduct =[\s\S]*?access\.hasPerms\('product:distribution:edit'\)[\s\S]*?canMaintainDistributionProductDependencies/);
    expect(distributionPageTsx).toMatch(/key="edit"[\s\S]*?hidden=\{!canEditDistributionProduct\}/);
    expect(distributionPageTsx).toMatch(/const openEdit = async[\s\S]*?if \(!canEditDistributionProduct\)/);
    expect(distributionPageTsx).toMatch(/const openSkuEdit = \([\s\S]*?if \(!canEditDistributionProduct\)/);
    expect(distributionPageTsx).toMatch(/key="add"[\s\S]*?hidden=\{!canCreateDistributionProduct\}/);
    expect(distributionPageTsx).not.toContain("hidden={!access.hasPerms('product:distribution:edit')}");

    expect(editPageTsx).toContain("const canCreateDistributionProduct = access.hasPerms('product:distribution:add')");
    expect(editPageTsx).toContain('const canEditDistributionProduct = isEdit');
    expect(editPageTsx).toContain("? (canQueryDistributionProduct && access.hasPerms('product:distribution:edit'))");
    expect(editPageTsx).toContain(': canCreateDistributionProduct');
    expect(editPageTsx).toMatch(/const submit = async[\s\S]*?if \(!canEditDistributionProduct\)/);
    expect(editPageTsx).toMatch(/保存[\s\S]*?disabled=\{!canEditDistributionProduct \|\| !canMaintainDistributionProductDependencies\}/);
    expect(editPageTsx).toMatch(/if \(isEdit\) \{[\s\S]*?history\.push\('\/product\/distribution'\)/);
  });

  it('keeps admin list category as leaf name and buyer preview as full category path', () => {
    const distributionPageTsx = readSource('src/pages/Product/Distribution/index.tsx');
    const editPageTsx = readSource('src/pages/Product/Distribution/EditPage.tsx');

    expect(distributionPageTsx).toContain('const renderListCategoryName =');
    expect(distributionPageTsx).toContain('record.categoryName?.trim()');
    expect(distributionPageTsx).toMatch(/title: '类目'[\s\S]*?render: \(_, record\) => renderListCategoryName\(record\)/);
    expect(distributionPageTsx).not.toContain('findCategoryDisplayPath');

    expect(editPageTsx).toContain('const categoryPath = findCategoryDisplayPath');
    expect(editPageTsx).toContain('categoryPath,');
  });

  it('maps product attribute library pagination to RuoYi page parameters', () => {
    const attributeLibraryTsx = readSource('src/pages/Product/Attribute/components/AttributeLibrary.tsx');

    expect(attributeLibraryTsx).toContain('const { current, pageSize, ...rest } = params;');
    expect(attributeLibraryTsx).toMatch(/getAttributeList\(\{[\s\S]*?\.\.\.rest[\s\S]*?pageNum:\s*current[\s\S]*?pageSize[\s\S]*?\}\)/);
    expect(attributeLibraryTsx).not.toContain('getAttributeList(params)');
  });

  it('maps product review pagination to RuoYi page parameters', () => {
    const reviewPageTsx = readSource('src/pages/Product/Review/index.tsx');

    expect(reviewPageTsx).toContain('request={async ({ current, pageSize, ...params }) => {');
    expect(reviewPageTsx).toMatch(/getProductReviewList\(\{[\s\S]*?\.\.\.queryParams[\s\S]*?pageNum:\s*current[\s\S]*?pageSize[\s\S]*?\}\)/);
    expect(reviewPageTsx).not.toContain('getProductReviewList(params)');
  });

  it('keeps product review page and submit-review action permission guarded', () => {
    const distributionPageTsx = readSource('src/pages/Product/Distribution/index.tsx');
    const distributionServiceTs = readSource('src/services/product/distributionProduct.ts');
    const editPageTsx = readSource('src/pages/Product/Distribution/EditPage.tsx');
    const productDetailDrawerTsx = readSource('src/pages/Product/Distribution/components/ProductDetailDrawer.tsx');
    const reviewPageTsx = readSource('src/pages/Product/Review/index.tsx');
    const reviewBusinessPreviewTsx = readSource('src/pages/Product/Review/components/ProductReviewBusinessPreview.tsx');
    const attributeDisplayTs = readSource('src/pages/Product/utils/attributeDisplay.ts');
    const reviewServiceTs = readSource('src/services/product/productReview.ts');

    expect(distributionServiceTs).toContain('submitDistributionProductReview');
    expect(distributionServiceTs).toContain('`${baseUrl}/${spuId}/submit-review`');
    expect(distributionPageTsx).toContain("DRAFT: { targetStatus: 'SUBMIT_REVIEW', label: '提交审核'");
    expect(distributionPageTsx).toContain('submitDistributionProductReview(spuId)');
    expect(distributionPageTsx).toContain('const canSubmitDistributionReview = canEditDistributionProduct');
    expect(distributionPageTsx).not.toContain("DRAFT: { targetStatus: 'READY'");

    expect(reviewServiceTs).toContain("const baseUrl = '/api/product/admin/reviews';");
    expect(reviewPageTsx).toContain("const canListProductReview = access.hasPerms('review:productDistribution:list')");
    expect(reviewPageTsx).toContain("const canQueryProductReview = access.hasPerms('review:productDistribution:query')");
    expect(reviewPageTsx).toContain("const canApproveProductReview = access.hasPerms('review:productDistribution:approve')");
    expect(reviewPageTsx).toContain("const canRejectProductReview = access.hasPerms('review:productDistribution:reject')");
    expect(reviewPageTsx).toContain("const canViewProductReviewLog = access.hasPerms('review:productDistribution:log')");
    expect(reviewPageTsx).toContain("const canPreviewCategorySchema = access.hasPerms('product:categoryAttribute:preview')");
    expect(reviewPageTsx).toContain('getProductReviewLogs');
    expect(reviewPageTsx).toContain("const DEFAULT_PRODUCT_REVIEW_LIST_STATUS = 'PENDING';");
    expect(reviewPageTsx).toContain('initialValues: { reviewStatus: DEFAULT_PRODUCT_REVIEW_LIST_STATUS }');
    expect(reviewPageTsx).toContain('beforeSearchSubmit={(params) => ({');
    expect(reviewPageTsx).toContain('reviewStatus: params.reviewStatus || DEFAULT_PRODUCT_REVIEW_LIST_STATUS');
    expect(reviewPageTsx).toContain('(queryParams as Record<string, unknown>).reviewStatus || DEFAULT_PRODUCT_REVIEW_LIST_STATUS');
    expect(reviewPageTsx).toMatch(/if \(canViewProductReviewLog\)[\s\S]*?getProductReviewLogs\(record\.reviewId\)/);
    expect(reviewPageTsx).toMatch(/if \(!canListProductReview\)[\s\S]*?return \{ data: \[\], total: 0, success: false \}/);
    expect(reviewPageTsx).toContain("hidden={!canQueryProductReview}");
    expect(reviewPageTsx).toContain("hidden={!pending || !canApproveProductReview}");
    expect(reviewPageTsx).toContain("hidden={!pending || !canRejectProductReview}");
    const actionGuardBlock = reviewPageTsx.match(/const canHandleReviewAction[\s\S]*?const openAction/)?.[0] || '';
    expect(actionGuardBlock).toContain("record.reviewStatus !== 'PENDING'");
    expect(actionGuardBlock).toContain("kind === 'APPROVE' && !canApproveProductReview");
    expect(actionGuardBlock).toContain("kind === 'REJECT' && !canRejectProductReview");
    const openActionBlock = reviewPageTsx.match(/const openAction[\s\S]*?const closeAction/)?.[0] || '';
    expect(openActionBlock).toContain('!canHandleReviewAction(kind, record)');
    const submitActionBlock = reviewPageTsx.match(/const submitAction[\s\S]*?const columns/)?.[0] || '';
    expect(submitActionBlock).toContain('!canHandleReviewAction(actionState.kind, actionState.review)');
    const detailFooterBlock = reviewPageTsx.match(/footer=\{currentReview\?\.reviewStatus === 'PENDING'[\s\S]*?\) : null\}/)?.[0] || '';
    expect(detailFooterBlock).toContain("currentReview?.reviewStatus === 'PENDING'");
    expect(detailFooterBlock).toContain('hidden={!canRejectProductReview}');
    expect(detailFooterBlock).toContain('hidden={!canApproveProductReview}');
    expect(reviewPageTsx).toContain('通过审核');
    expect(detailFooterBlock).toMatch(/openAction\('APPROVE', currentReview\)/);
    expect(detailFooterBlock).toMatch(/openAction\('REJECT', currentReview\)/);
    expect(reviewPageTsx).toContain('setDetailOpen(false);');
    expect(reviewPageTsx).toContain('setCurrentReview(undefined);');
    expect(reviewPageTsx).not.toContain('继续编辑');
    expect(reviewPageTsx).not.toContain('canContinueRejectedReview');
    expect(reviewPageTsx).not.toContain('?reviewId=');
    expect(editPageTsx).not.toContain('getProductReview');
    expect(editPageTsx).not.toContain('parseReviewProductSnapshot');
    expect(editPageTsx).not.toContain("get('reviewId')");
    expect(reviewPageTsx).not.toContain('<Dropdown');
    expect(reviewPageTsx).not.toContain('更多 <DownOutlined />');
    expect(reviewPageTsx).toContain('ProductReviewBusinessPreview');
    expect(reviewPageTsx).toContain('canPreviewCategorySchema={canPreviewCategorySchema}');
    expect(reviewBusinessPreviewTsx).toContain('export default function ProductReviewBusinessPreview');
    expect(reviewPageTsx).not.toContain('function renderReviewFocus');
    expect(reviewPageTsx).not.toContain('function renderFocusLine');
    expect(reviewPageTsx).toContain('function normalizeWarehouseKind');
    expect(reviewPageTsx).toContain('function formatWarehouseKindLabel');
    expect(reviewPageTsx).toContain('function formatSalesStatusLabel');
    expect(reviewPageTsx).toContain('function formatControlStatusLabel');
    expect(reviewPageTsx).toContain('function formatSnapshotRoleLabel');
    expect(reviewPageTsx).toContain('function formatPayloadTypeLabel');
    expect(reviewPageTsx).toContain('function formatReviewOrSalesStatusLabel');
    expect(reviewPageTsx).toContain('function localizeSnapshotPayload');
    expect(reviewPageTsx).toContain('function isOfficialWarehouseKind');
    expect(reviewPageTsx).toContain("return 'third_party'");
    expect(reviewPageTsx).toContain("title: '仓库类型'");
    expect(reviewPageTsx).toContain("dataIndex: 'warehouseSummary'");
    const reviewListColumnsBlock = reviewPageTsx.match(/function buildReviewListColumns[\s\S]*?const ProductReviewPage/)?.[0] || '';
    expect(reviewPageTsx).toContain('const columns = buildReviewListColumns(reviewTypeTab');
    expect(reviewListColumnsBlock).not.toContain("title: '提交端'");
    expect(reviewListColumnsBlock).not.toContain("dataIndex: 'submitTerminal'");
    expect(reviewListColumnsBlock).toContain("title: '系统SPU'");
    expect(reviewListColumnsBlock).toContain("dataIndex: 'systemSpuCode'");
    expect(reviewListColumnsBlock).toContain('审核单号 / 系统SPU / 标题 / 卖家');
    expect(reviewListColumnsBlock.indexOf("title: '系统SPU'")).toBeGreaterThan(
      reviewListColumnsBlock.indexOf("title: '商品图'"),
    );
    expect(reviewListColumnsBlock.indexOf("title: '系统SPU'")).toBeLessThan(
      reviewListColumnsBlock.indexOf("title: '审核类型'"),
    );
    expect(reviewListColumnsBlock).not.toContain("title: '审核重点'");
    expect(reviewListColumnsBlock).not.toContain("title: '变化摘要'");
    expect(reviewListColumnsBlock.indexOf("title: '审核状态'")).toBeGreaterThan(
      reviewListColumnsBlock.indexOf("title: '仓库类型'"),
    );
    expect(reviewListColumnsBlock.indexOf("title: '审核状态'")).toBeLessThan(
      reviewListColumnsBlock.indexOf("title: '提交人'"),
    );
    expect(reviewListColumnsBlock).toContain("reviewTypeTab === 'NEW_PRODUCT'");
    expect(reviewListColumnsBlock).toContain("title: 'SKU数量'");
    expect(reviewListColumnsBlock).toContain("title: '供货价区间'");
    expect(reviewListColumnsBlock).toContain("title: '发货仓库'");
    expect(reviewListColumnsBlock).toContain("reviewTypeTab === 'ADD_SKU'");
    expect(reviewListColumnsBlock).toContain("title: reviewTypeTab === 'ADD_SKU' ? '新增SKU' : '变更SKU'");
    expect(reviewListColumnsBlock).toContain("title: '规格'");
    expect(reviewListColumnsBlock).toContain('renderSummaryTextLines(item.afterSpecSummary || item.beforeSpecSummary)');
    expect(reviewPageTsx).toContain("whiteSpace: 'normal'");
    expect(reviewPageTsx).toContain("overflowWrap: 'anywhere'");
    expect(reviewPageTsx).not.toContain("whiteSpace: 'nowrap'");
    expect(reviewListColumnsBlock).toContain("title: '变更字段'");
    expect(reviewListColumnsBlock).not.toContain("title: '关键变更'");
    expect(reviewPageTsx).not.toContain('renderKeyChange');
    expect(reviewListColumnsBlock).toContain("reviewTypeTab === 'EDIT_PRICE'");
    expect(reviewListColumnsBlock).toContain("title: '原供货价'");
    expect(reviewListColumnsBlock).toContain("title: '新供货价'");
    expect(reviewListColumnsBlock).toContain("title: '币种'");
    expect(reviewListColumnsBlock).toContain("changedFieldNames?.includes('供货价')");
    expect(reviewListColumnsBlock).toContain("reviewTypeTab === 'EDIT_MIXED'");
    expect(reviewListColumnsBlock).toContain("title: '变更模块'");
    expect(reviewListColumnsBlock).toContain("title: '影响SKU'");
    expect(reviewListColumnsBlock).toContain("title: '供货价变化'");
    expect(reviewPageTsx).not.toContain("title: '风险'");
    expect(reviewPageTsx).not.toContain("dataIndex: 'riskLevel'");
    expect(reviewPageTsx).not.toContain('riskLevelValueEnum');
    expect(reviewPageTsx).not.toContain('product_review_risk_level');
    expect(reviewPageTsx).not.toContain('风险等级');
    expect(reviewPageTsx).toContain('function safeParseJson');
    expect(reviewPageTsx).toContain('function renderSnapshotCompareTable');
    expect(reviewPageTsx).toContain("SNAPSHOT_ROLE_TEXT: Record<string, string>");
    expect(reviewPageTsx).toContain("PAYLOAD_TYPE_TEXT: Record<string, string>");
    expect(reviewPageTsx).toContain("ATTRIBUTE_TYPE_TEXT: Record<string, string>");
    expect(reviewPageTsx).toContain("IMAGE_ROLE_TEXT: Record<string, string>");
    expect(reviewPageTsx).toContain("LOCK_STATUS_TEXT: Record<string, string>");
    expect(reviewPageTsx).toContain("BINDING_STATUS_TEXT: Record<string, string>");
    expect(reviewPageTsx).toContain('inventoryStatusText[value]');
    expect(reviewPageTsx).toContain("case 'ownerType':");
    expect(reviewPageTsx).toContain("case 'valueCode':");
    expect(reviewPageTsx).toContain("item?.text || (value ? '未知' : '--')");
    expect(reviewPageTsx).toContain("render: (value) => formatSnapshotRoleLabel(String(value || ''))");
    expect(reviewPageTsx).toContain("render: (value) => formatPayloadTypeLabel(String(value || ''))");
    expect(reviewPageTsx).toContain("render: (value) => formatReviewOrSalesStatusLabel(String(value || ''))");
    expect(reviewPageTsx).toContain('function renderReviewTypeTabLabel');
    expect(reviewPageTsx).toContain('function normalizeReviewTypeValueEnum');
    expect(reviewPageTsx).toContain("text: '供货价变更'");
    expect(reviewPageTsx).toContain("EDIT_MIXED: { text: '综合变更'");
    expect(reviewPageTsx).toContain('reviewTypePendingCounts');
    expect(reviewPageTsx).toContain("const pending = record.reviewStatus === 'PENDING';");
    expect(reviewPageTsx).toContain('refreshReviewTypePendingCounts();');
    expect(reviewPageTsx).toContain('items={reviewTypeTabs}');
    expect(reviewPageTsx).toContain("style={{ flex: 'none' }}");
    expect(reviewPageTsx).toContain('params={{ reviewTypeTab }}');
    expect(reviewPageTsx).not.toContain('headerTitle={');
    expect(reviewPageTsx).toContain("case 'NEW_PRODUCT'");
    expect(reviewPageTsx).toContain("case 'ADD_SKU'");
    expect(reviewPageTsx).toContain("case 'EDIT_PRODUCT_INFO'");
    expect(reviewPageTsx).toContain("case 'EDIT_SKU_INFO'");
    expect(reviewPageTsx).toContain("case 'EDIT_PRICE'");
    expect(reviewPageTsx).toContain("case 'EDIT_MIXED'");
    expect(reviewPageTsx).toContain('priceBeforeMin');
    expect(reviewPageTsx).toContain('priceAfterMin');
    expect(reviewPageTsx).toContain("label: '变更预览'");
    expect(reviewPageTsx).toContain("label: '审核基础信息'");
    expect(reviewPageTsx).toContain("label: '审计快照'");
    expect(reviewPageTsx).toContain('function renderReviewBasicInfo');
    expect(reviewPageTsx).toMatch(/key: 'focus'[\s\S]*?label: '变更预览'[\s\S]*?key: 'basic'[\s\S]*?label: '审核基础信息'/);
    expect(reviewBusinessPreviewTsx).toContain('type AuditField');
    expect(reviewBusinessPreviewTsx).toContain('function AuditTemplateReviewView');
    expect(reviewBusinessPreviewTsx).toContain('function buildAuditBasicFields');
    expect(reviewBusinessPreviewTsx).toContain('function renderAuditImagePairs');
    expect(reviewBusinessPreviewTsx).toContain('function buildAuditAttributeFields');
    expect(reviewBusinessPreviewTsx).toContain('function buildAuditSkuFields');
    expect(reviewBusinessPreviewTsx).toContain('function renderAuditSkuCard');
    expect(reviewBusinessPreviewTsx).toContain('function renderAuditDetailContent');
    expect(reviewBusinessPreviewTsx).toContain('function renderAuditLineList');
    expect(reviewBusinessPreviewTsx).toContain('function renderAuditSkuSpecs');
    expect(reviewBusinessPreviewTsx).toContain('function renderAuditSkuDimension');
    expect(reviewBusinessPreviewTsx).toContain('function formatAuditChangeBadge');
    expect(reviewBusinessPreviewTsx).toContain('<AuditTemplateReviewView review={review} displayMaps={displayMaps} />');
    expect(reviewBusinessPreviewTsx).toContain("title: '基础信息'");
    expect(reviewBusinessPreviewTsx).toContain("title: '商品图片'");
    expect(reviewBusinessPreviewTsx).toContain("title: '类目属性'");
    expect(reviewBusinessPreviewTsx).toContain("title: 'SKU 信息'");
    expect(reviewBusinessPreviewTsx).toContain("title: '详情图文'");
    expect(reviewBusinessPreviewTsx.indexOf("title: 'SKU 信息'")).toBeGreaterThan(
      reviewBusinessPreviewTsx.indexOf("title: '类目属性'"),
    );
    expect(reviewBusinessPreviewTsx.indexOf("title: '详情图文'")).toBeGreaterThan(
      reviewBusinessPreviewTsx.indexOf("title: 'SKU 信息'"),
    );
    expect(reviewBusinessPreviewTsx).toContain('const defaultActiveKey = sections.filter((section) => section.changed)');
    expect(reviewBusinessPreviewTsx).toContain('defaultActiveKey={defaultActiveKey}');
    expect(reviewBusinessPreviewTsx).toContain("extra: <Tag color={section.changed ? 'green' : 'default'}>{section.badge}</Tag>");
    expect(reviewBusinessPreviewTsx).toContain("return count > 0 ? `${count} ${unit}${suffix}` : '无变化'");
    expect(reviewBusinessPreviewTsx).not.toContain("badge: `${countChangedFields");
    expect(reviewBusinessPreviewTsx).not.toContain("badge: imageChanged ? '有变更' : '无变化'");
    expect(reviewBusinessPreviewTsx).toContain("buildAuditField('supplyPrice', '供货价'");
    expect(reviewBusinessPreviewTsx).toContain("field.key === 'supplyPrice'");
    expect(reviewBusinessPreviewTsx).toContain('renderSupplyPriceChangeTags(pair.before, pair.after)');
    expect(reviewBusinessPreviewTsx).toContain('renderAuditImage(sku?.skuImageUrl, 44)');
    expect(reviewBusinessPreviewTsx).toContain('renderAuditImage(pair.beforeUrl, 72)');
    const auditPairKeyBlock = reviewBusinessPreviewTsx.match(/function skuAuditPairKey[\s\S]*?function skuSpecAuditRaw/)?.[0] || '';
    expect(auditPairKeyBlock).toContain('sku?.skuId');
    expect(auditPairKeyBlock).toContain('sku?.sourceSkuGroupKey');
    expect(auditPairKeyBlock).toContain('sku?.sourceDimensionGroupKey');
    expect(auditPairKeyBlock).toContain('sku?.systemSkuCode');
    expect(auditPairKeyBlock).not.toContain('sku?.sellerSkuCode');
    expect(auditPairKeyBlock).not.toContain('?? index');
    expect(auditPairKeyBlock).toContain('before-unmatched');
    expect(auditPairKeyBlock).toContain('after-unmatched');
    const auditSkuFieldsBlock = reviewBusinessPreviewTsx.match(/function buildAuditSkuFields[\s\S]*?function renderAuditSkuChangeTags/)?.[0] || '';
    expect(auditSkuFieldsBlock).toContain("buildAuditField('skuImageUrl', 'SKU图'");
    expect(auditSkuFieldsBlock).toContain("buildAuditField('sellerSkuCode', '客户SKU'");
    expect(auditSkuFieldsBlock).toContain("buildAuditField('spec', '规格'");
    expect(auditSkuFieldsBlock).toContain('renderAuditSkuSpecs(pair.before)');
    expect(auditSkuFieldsBlock).toContain('renderAuditSkuSpecs(pair.after)');
    expect(auditSkuFieldsBlock).toContain("buildAuditField('supplyPrice', '供货价'");
    expect(auditSkuFieldsBlock).toContain("buildAuditField('dimension', '尺寸重量'");
    expect(auditSkuFieldsBlock).toContain('renderAuditSkuDimension(pair.before)');
    expect(auditSkuFieldsBlock).toContain('renderAuditSkuDimension(pair.after)');
    expect(auditSkuFieldsBlock).toContain("buildAuditField('warehouse', '发货仓库'");
    expect(auditSkuFieldsBlock).not.toContain('formatSkuSpecs(pair.before)');
    expect(auditSkuFieldsBlock).not.toContain('formatDimension(pair.before)');
    expect(auditSkuFieldsBlock).not.toContain('sourceDimensionGroupKey');
    expect(auditSkuFieldsBlock).not.toContain('sourceSkuGroupKey');
    expect(auditSkuFieldsBlock).not.toContain('masterSku');
    expect(reviewBusinessPreviewTsx).not.toContain('function PriceChangeReviewView');
    expect(reviewBusinessPreviewTsx).not.toContain("renderSection('SKU 供货价左右对比'");
    expect(reviewBusinessPreviewTsx).not.toContain("field.key !== 'supplyPrice'");
    expect(reviewBusinessPreviewTsx).toContain("['颜色', sku?.color]");
    expect(reviewBusinessPreviewTsx).toContain("['尺码', sku?.size]");
    expect(reviewBusinessPreviewTsx).toContain("['尺寸', formatAuditSkuSize(sku)]");
    expect(reviewBusinessPreviewTsx).toContain("['重量', formatAuditSkuWeight(sku)]");
    expect(reviewBusinessPreviewTsx).toContain("measured.join(' × ')");
    expect(reviewBusinessPreviewTsx).toContain('厘米');
    expect(reviewBusinessPreviewTsx).toContain('千克');
    expect(reviewBusinessPreviewTsx).not.toContain("系统SKU：{sku?.systemSkuCode || pair.item?.systemSkuCode || '--'} /");
    expect(reviewBusinessPreviewTsx).toContain('function renderSalesStatus');
    expect(reviewBusinessPreviewTsx).toContain('function normalizeCompareValue');
    expect(reviewBusinessPreviewTsx).toContain('function normalizeMeasurementString');
    expect(reviewBusinessPreviewTsx).toContain('function formatSkuWarehouseValue');
    expect(reviewBusinessPreviewTsx).not.toContain("`${formatSkuSpecs(row.before)} -> ${formatSkuSpecs(row.after)}`");
    expect(reviewBusinessPreviewTsx).toContain('function EditChangeOverviewView');
    expect(reviewBusinessPreviewTsx).toContain('function getEditChangeScopes');
    expect(reviewBusinessPreviewTsx).toContain('function renderChangeScopeTags');
    expect(reviewBusinessPreviewTsx).toContain("'EDIT_MIXED'");
    expect(reviewBusinessPreviewTsx).toContain("'SUPPLY_PRICE'");
    expect(reviewBusinessPreviewTsx).toContain('function getProductWarehouseKind');
    expect(reviewBusinessPreviewTsx).toContain('function normalizeWarehouseKind');
    expect(reviewBusinessPreviewTsx).toContain('function shouldShowDeliveryWarehouse');
    expect(reviewBusinessPreviewTsx).toContain("normalizeWarehouseKind(getProductWarehouseKind(product, review)) !== 'official'");
    expect(reviewBusinessPreviewTsx).toContain("return 'third_party'");
    expect(reviewBusinessPreviewTsx).toContain("renderMetric('仓库类型'");
    expect(reviewBusinessPreviewTsx).toContain("renderMetric('供货价区间'");
    expect(reviewBusinessPreviewTsx).not.toContain("renderMetric('销售价区间'");
    expect(reviewBusinessPreviewTsx).toContain("showDeliveryWarehouse ? renderSection('发货仓库'");
    expect(reviewBusinessPreviewTsx).toContain('canPreviewCategorySchema?: boolean');
    expect(reviewBusinessPreviewTsx).toContain('canPreviewCategorySchema = false');
    expect(reviewBusinessPreviewTsx).toContain('if (!categoryId || !canPreviewCategorySchema)');
    expect(reviewBusinessPreviewTsx).toContain('getCategorySchema(categoryId, { skipErrorHandler: true })');
    expect(reviewBusinessPreviewTsx).toContain('formatAttributeValue(row, displayMaps.optionLabelMap)');
    expect(reviewBusinessPreviewTsx).toContain('resolveAttributeLabel(row, displayMaps.attributeLabelMap)');
    expect(reviewBusinessPreviewTsx).toContain("<Descriptions.Item label=\"商品状态\">{renderSalesStatus(product?.spuStatus)}</Descriptions.Item>");
    expect(reviewBusinessPreviewTsx).toContain("render: (value) => renderSalesStatus(String(value || ''))");
    expect(reviewBusinessPreviewTsx).toContain('before: renderSalesStatus(before?.skuStatus)');
    expect(reviewBusinessPreviewTsx).toContain('after: renderSalesStatus(after?.skuStatus)');
    expect(reviewBusinessPreviewTsx).not.toContain('function renderAttributeValue');
    expect(productDetailDrawerTsx).toContain('@/pages/Product/utils/attributeDisplay');
    expect(productDetailDrawerTsx).not.toContain('function formatAttributeValue');
    expect(attributeDisplayTs).toContain('option.optionLabel');
    expect(attributeDisplayTs).toContain("if (item.valueCode === 'Y') return '是'");
    expect(attributeDisplayTs).toContain("if (item.valueCode === 'N') return '否'");
    expect(attributeDisplayTs).toContain('const valueCodeLabel = resolveOptionLabel(item, item.valueCode, optionLabelMap)');
    expect(attributeDisplayTs).toContain('parseJsonArrayValues(item.valueJson)');
    expect(reviewBusinessPreviewTsx).toContain('DetailContentPreview');
    expect(reviewBusinessPreviewTsx).not.toContain('新增商品审核需要看完整商品');
    expect(reviewBusinessPreviewTsx).not.toContain('审核检查清单');
    expect(reviewBusinessPreviewTsx).not.toContain('buildNewProductChecks');
    expect(reviewBusinessPreviewTsx).not.toContain('本次只审核新增 SKU');
    expect(reviewBusinessPreviewTsx).not.toContain('旧商品资料只作为上下文');
    expect(reviewBusinessPreviewTsx).not.toContain('<Alert');
    expect(reviewBusinessPreviewTsx).not.toContain('默认只看变更字段');
    expect(reviewBusinessPreviewTsx).not.toContain('这里只展示发生变化的 SKU');
    expect(reviewBusinessPreviewTsx).not.toContain('价格变更审核');
    expect(reviewBusinessPreviewTsx).not.toContain('暂未识别到专用审核视图');
    expect(reviewBusinessPreviewTsx).not.toContain('卖点');
    expect(reviewBusinessPreviewTsx).not.toContain('sellingPoint');
    expect(reviewBusinessPreviewTsx).toContain("borderColor: '#ffccc7'");
    expect(reviewBusinessPreviewTsx).toContain("borderColor: '#b7eb8f'");
    expect(reviewBusinessPreviewTsx).not.toContain("renderMetric('影响 SKU'");
    expect(reviewBusinessPreviewTsx).not.toContain("renderMetric('涨价 SKU'");
    expect(reviewBusinessPreviewTsx).not.toContain("renderMetric('降价 SKU'");
    expect(reviewBusinessPreviewTsx).not.toContain("renderMetric('原供货价区间'");
    expect(reviewBusinessPreviewTsx).not.toContain("renderMetric('新供货价区间'");
    expect(reviewBusinessPreviewTsx).toContain("'供货价'");
    expect(reviewBusinessPreviewTsx).toContain('renderSupplyPriceChangeTags');
    expect(reviewBusinessPreviewTsx).toContain('供货价上涨');
    expect(reviewBusinessPreviewTsx).toContain('供货价下降');
    expect(reviewBusinessPreviewTsx).toContain('供货价未变');
    expect(reviewBusinessPreviewTsx).not.toContain('供货价正常');
    expect(reviewBusinessPreviewTsx).not.toContain("buildCompareField('spec', '规格'");
    expect(reviewBusinessPreviewTsx).not.toContain("当前销售价");
    expect(reviewBusinessPreviewTsx).not.toContain("高于销售价");
  });

  it('keeps sales status changes reasoned before calling the admin API', () => {
    const distributionPageTsx = readSource('src/pages/Product/Distribution/index.tsx');
    const distributionServiceTs = readSource('src/services/product/distributionProduct.ts');

    expect(distributionPageTsx).toContain("'已调整售价'");
    expect(distributionPageTsx).not.toContain('已提交调价审核');
    expect(distributionPageTsx).toContain("let statusReason = '';");
    expect(distributionPageTsx).toContain('placeholder="请输入状态调整原因"');
    expect(distributionPageTsx).toContain("message.warning('请输入状态调整原因')");
    expect(distributionPageTsx).toMatch(/const normalizedReason = statusReason\.trim\(\);[\s\S]*?if \(!normalizedReason\)/);
    expect(distributionPageTsx).toMatch(/batchUpdateDistributionStatus\([\s\S]*?targetStatus,[\s\S]*?normalizedReason,[\s\S]*?shouldAskSkuSync/);
    expect(distributionServiceTs).toMatch(/status:\s*string,[\s\S]*?reason\?:\s*string,[\s\S]*?syncSkuStatus\?:\s*boolean/);
    expect(distributionServiceTs).toMatch(/data:\s*\{[\s\S]*?ownerType,[\s\S]*?status,[\s\S]*?reason,/);
    expect(distributionServiceTs).toMatch(/updateDistributionProductStatus\([\s\S]*?reason\?: string/);
    expect(distributionServiceTs).toMatch(/updateDistributionSkuStatus\([\s\S]*?reason\?: string/);
  });

  it('guards dependent product, seller, category and warehouse APIs on distribution pages', () => {
    const distributionPageTsx = readSource('src/pages/Product/Distribution/index.tsx');
    const editPageTsx = readSource('src/pages/Product/Distribution/EditPage.tsx');

    expect(editPageTsx).toContain("import { history, useAccess, useParams } from '@umijs/max'");
    expect(distributionPageTsx).toContain("const canQueryAdminSellers = access.hasPerms('seller:admin:list')");
    expect(distributionPageTsx).toContain("const canQueryProductCategories = access.hasPerms('product:category:list')");
    expect(distributionPageTsx).toMatch(/if \(canQueryAdminSellers\)[\s\S]*?getAdminSellerList/);
    expect(distributionPageTsx).toMatch(/if \(canQueryProductCategories\)[\s\S]*?getCategoryList/);

    expect(editPageTsx).toContain("const canQueryDistributionProduct = access.hasPerms('product:distribution:query')");
    expect(editPageTsx).toContain("const canQueryAdminSellers = access.hasPerms('seller:admin:list')");
    expect(editPageTsx).toContain("const canQueryProductCategories = access.hasPerms('product:category:list')");
    expect(editPageTsx).toContain("const canPreviewCategorySchema = access.hasPerms('product:categoryAttribute:preview')");
    expect(editPageTsx).toContain('const canMaintainDistributionProductDependencies =');
    expect(editPageTsx).toContain('const [schemaLoadedCategoryId, setSchemaLoadedCategoryId] = useState<number>()');
    expect(editPageTsx).toContain("const canQuerySourceProducts = access.hasPerms('integration:upstream:query')");
    expect(editPageTsx).not.toContain("access.hasPerms('product:list:list')");
    expect(editPageTsx).toContain("const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list')");
    expect(editPageTsx).toContain("const canQueryThirdPartyWarehouses = access.hasPerms('warehouse:thirdParty:list')");
    expect(editPageTsx).toMatch(/const canMaintainDistributionProductDependencies =[\s\S]*?canQueryOfficialWarehouses[\s\S]*?canQueryThirdPartyWarehouses/);
    expect(editPageTsx).toMatch(/const categoryRequest = canQueryProductCategories[\s\S]*?\? getCategoryList/);
    expect(editPageTsx).toMatch(/const sellerRequest = canQueryAdminSellers[\s\S]*?\? getAdminSellerList/);
    expect(editPageTsx).toMatch(/const officialWarehouseRequest = canQueryOfficialWarehouses[\s\S]*?\? getOfficialWarehouseList/);
    expect(editPageTsx).toMatch(/const thirdPartyWarehouseRequest = selectedSellerId && canQueryThirdPartyWarehouses[\s\S]*?\? getThirdPartyWarehouseList/);
    expect(editPageTsx).toMatch(/if \(!canQueryDistributionProduct\)[\s\S]*?history\.replace\('\/product\/distribution'\)[\s\S]*?getDistributionProduct/);
    expect(editPageTsx).toMatch(/const loadSchema = async \(categoryId: number\) => \{[\s\S]*?if \(!canPreviewCategorySchema\)/);
    expect(editPageTsx).toMatch(/const submit = async[\s\S]*?if \(!canMaintainDistributionProductDependencies\)/);
    expect(editPageTsx).toMatch(/schemaLoadedCategoryId !== categoryId[\s\S]*?类目属性未加载/);
    expect(editPageTsx).toMatch(/const openSourceSelector = \(\) => \{[\s\S]*?if \(!canQuerySourceProducts\)/);
    expect(editPageTsx).toMatch(/request=\{async \(params\) => \{[\s\S]*?if \(!canQuerySourceProducts\)[\s\S]*?getSourceProductList/);
    expect(editPageTsx).toContain('disabled={!canQuerySourceProducts}');
  });

  it('does not mix buyer admin permissions into the admin product page', () => {
    const sources = [
      readSource('src/pages/Product/Distribution/index.tsx'),
      readSource('src/pages/Product/Distribution/EditPage.tsx'),
    ];

    for (const source of sources) {
      expect(source).not.toMatch(/buyer:admin:/);
    }
  });
});
