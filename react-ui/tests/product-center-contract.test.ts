import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function expectPureDefaultReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export { default } from '${target}';`);
}

function expectPureNamedReExport(relativePath: string, target: string) {
  expect(readSource(relativePath).trim()).toBe(`export * from '${target}';`);
}

describe('product center buyer-facing contract', () => {
  it('keeps js mirrors aligned for product center pages and shared components', () => {
    expectPureDefaultReExport('src/pages/Product/ProductCenter/index.js', './index.tsx');
    expectPureDefaultReExport('src/pages/Buyer/ProductCenter/index.js', './index.tsx');
    expectPureDefaultReExport('src/pages/Portal/Home/BuyerProductCenter.js', './BuyerProductCenter.tsx');
    expectPureDefaultReExport('src/components/ProductCenter/ProductCenterPage.js', './ProductCenterPage.tsx');
    expectPureDefaultReExport(
      'src/components/ProductCenter/ProductCenterDetailModal.js',
      './ProductCenterDetailModal.tsx',
    );
    expectPureDefaultReExport(
      'src/pages/Product/Distribution/components/BuyerProductPreviewModal.js',
      './BuyerProductPreviewModal.tsx',
    );
    expectPureNamedReExport('src/services/product/productCenter.js', './productCenter.ts');
  });

  it('uses admin product center service routes and admin permissions only at the admin entry', () => {
    const serviceTs = readSource('src/services/product/productCenter.ts');
    const adminPage = readSource('src/pages/Product/ProductCenter/index.tsx');
    const sharedPage = readSource('src/components/ProductCenter/ProductCenterPage.tsx');

    expect(serviceTs).toContain("const baseUrl = '/api/product/admin/product-center';");
    expect(serviceTs).toContain('getProductCenterList');
    expect(serviceTs).toContain('getProductCenterProduct');
    expect(serviceTs).toContain('getProductCenterSkus');

    expect(adminPage).toContain("access.hasPerms('product:center:list')");
    expect(adminPage).toContain("access.hasPerms('product:center:query')");
    expect(adminPage).toContain('fetchList={getProductCenterList}');
    expect(adminPage).toContain('fetchProduct={getProductCenterProduct}');
    expect(sharedPage).not.toContain("access.hasPerms('product:center");
    expect(sharedPage).toContain('fetchList: (params?: Record<string, any>)');
    expect(sharedPage).toContain('fetchProduct: (spuId: number)');
  });

  it('reuses the shared product center page for the buyer portal entry', () => {
    const buyerPage = readSource('src/pages/Portal/Home/BuyerProductCenter.tsx');
    const buyerRouteEntry = readSource('src/pages/Buyer/ProductCenter/index.tsx');
    const portalService = readSource('src/services/portal/session.ts');

    expect(buyerRouteEntry.trim()).toBe("export { default } from '@/pages/Portal/Home';");
    expect(buyerPage).toContain("import ProductCenterPage from '@/components/ProductCenter/ProductCenterPage';");
    expect(buyerPage).toContain('getBuyerPortalProductCenterProducts');
    expect(buyerPage).toContain('getBuyerPortalProductCenterProduct');
    expect(buyerPage).toContain('getBuyerPortalProductCenterProductSkus');
    expect(buyerPage).toContain("hasPermission(permissions, 'buyer:product:center:list')");
    expect(buyerPage).toContain("hasPermission(permissions, 'buyer:product:center:query')");
    expect(buyerPage).toContain('visibleSystemSkuCodes');
    expect(buyerPage).toContain('fetchList={fetchBuyerProductCenterList}');
    expect(buyerPage).toContain('fetchProduct={fetchBuyerProductCenterProduct}');
    expect(buyerPage).toContain('storageKey="buyer-product-center"');
    expect(portalService).toContain("buildPortalUrl('buyer', '/product/center/list')");
    expect(portalService).toContain("buildPortalUrl('buyer', `/product/center/${spuId}`)");
    expect(portalService).toContain("buildPortalUrl('buyer', `/product/center/${spuId}/skus`)");

    for (const forbidden of [
      'buyerId',
      'sellerId',
      'sellerName',
      'sellerSkuCode',
      'supplyPrice',
      'controlStatus',
      'getBuyerPortalDistributionProducts',
    ]) {
      expect(buyerPage).not.toContain(forbidden);
    }
  });

  it('does not expose seller, customer sku, supply price, or admin control fields', () => {
    const sources = [
      readSource('src/types/product/product-center.d.ts'),
      readSource('src/components/ProductCenter/ProductCenterPage.tsx'),
      readSource('src/components/ProductCenter/ProductCenterDetailModal.tsx'),
    ].join('\n');

    for (const forbidden of [
      'sellerId',
      'sellerName',
      'sellerSpuCode',
      'sellerSkuCode',
      'supplyPrice',
      'controlReason',
      'controlStatus',
    ]) {
      expect(sources).not.toContain(forbidden);
    }

    expect(sources).toContain('systemSpuCode');
    expect(sources).toContain('systemSkuCode');
    expect(sources).toContain('salePrice');
  });

  it('reuses buyer preview modal in real data mode without preview-only affordances', () => {
    const detailModal = readSource('src/components/ProductCenter/ProductCenterDetailModal.tsx');
    const previewModal = readSource('src/pages/Product/Distribution/components/BuyerProductPreviewModal.tsx');

    expect(detailModal).toContain('BuyerProductPreviewModal');
    expect(detailModal).toContain('mode="real"');
    expect(detailModal).toContain('footer={null}');
    expect(detailModal).not.toContain('warehouse.stockText');
    expect(detailModal).not.toContain("stockText: warehouse.stockText || '库存待同步'");
    expect(readSource('src/types/product/product-center.d.ts')).not.toContain('stockText?: string');
    expect(previewModal).toContain("mode?: 'preview' | 'real'");
    expect(previewModal).toContain('const isPreviewMode = mode ===');
    expect(previewModal).toContain("isPreviewMode ? '买家商品详情预览' : '商品详情'");
    expect(previewModal).toContain("isPreviewMode ? <Tag color=\"orange\">样式预览价</Tag> : null");
  });
});
