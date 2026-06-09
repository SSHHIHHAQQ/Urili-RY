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
    const reviewPageTsx = readSource('src/pages/Product/Review/index.tsx');
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
    expect(reviewPageTsx).toContain('getProductReviewLogs');
    expect(reviewPageTsx).toMatch(/if \(canViewProductReviewLog\)[\s\S]*?getProductReviewLogs\(record\.reviewId\)/);
    expect(reviewPageTsx).toMatch(/if \(!canListProductReview\)[\s\S]*?return \{ data: \[\], total: 0, success: false \}/);
    expect(reviewPageTsx).toContain("hidden={!canQueryProductReview}");
    expect(reviewPageTsx).toContain("hidden={!pending || !canApproveProductReview}");
    expect(reviewPageTsx).toContain("hidden={!pending || !canRejectProductReview}");
    expect(reviewPageTsx).not.toContain('继续编辑');
    expect(reviewPageTsx).not.toContain('canContinueRejectedReview');
    expect(reviewPageTsx).not.toContain('?reviewId=');
    expect(editPageTsx).not.toContain('getProductReview');
    expect(editPageTsx).not.toContain('parseReviewProductSnapshot');
    expect(editPageTsx).not.toContain("get('reviewId')");
    expect(reviewPageTsx).not.toContain('<Dropdown');
    expect(reviewPageTsx).not.toContain('更多 <DownOutlined />');
    expect(reviewPageTsx).toContain('function renderReviewFocus');
    expect(reviewPageTsx).toContain('function renderTypeDetailPanel');
    expect(reviewPageTsx).toContain('function safeParseJson');
    expect(reviewPageTsx).toContain('function renderSnapshotCompareTable');
    expect(reviewPageTsx).toContain('function renderReviewTypeTabLabel');
    expect(reviewPageTsx).toContain('reviewTypePendingCounts');
    expect(reviewPageTsx).toContain("reviewStatus: 'PENDING'");
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
    expect(reviewPageTsx).toContain('priceBeforeMin');
    expect(reviewPageTsx).toContain('priceAfterMin');
    expect(reviewPageTsx).toContain("label: '审核重点'");
  });

  it('keeps sales status changes reasoned before calling the admin API', () => {
    const distributionPageTsx = readSource('src/pages/Product/Distribution/index.tsx');
    const distributionServiceTs = readSource('src/services/product/distributionProduct.ts');

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
