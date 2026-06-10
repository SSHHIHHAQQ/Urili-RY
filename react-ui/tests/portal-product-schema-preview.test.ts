import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(__dirname, '..');

function read(relativePath: string) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function expectSource(relativePath: string, expected: string) {
  expect(read(relativePath).trim()).toBe(expected);
}

describe('portal product schema preview terminal binding', () => {
  it('keeps seller schema preview bound to seller portal schema services', () => {
    const source = read('src/pages/Portal/Home/SellerProductSchemaPreview.tsx');

    expect(source).toContain('getSellerPortalProductCategories');
    expect(source).toContain('getSellerPortalProductSchema');
    expect(source).not.toMatch(/getBuyerPortalProduct(?:Categories|Schema)/);
    expect(source).not.toMatch(/\brequest\s*(?:<[^;]+?>)?\s*\(/);
    expect(source).not.toMatch(/from\s+['"]@\/services\/product\//);
    expectSource(
      'src/pages/Portal/Home/SellerProductSchemaPreview.js',
      "export { default, PortalProductSchemaPreview } from './SellerProductSchemaPreview.tsx';",
    );
  });

  it('keeps buyer schema preview bound to buyer portal schema services', () => {
    const source = read('src/pages/Portal/Home/BuyerProductSchemaPreview.tsx');

    expect(source).toContain('getBuyerPortalProductCategories');
    expect(source).toContain('getBuyerPortalProductSchema');
    expect(source).not.toMatch(/getSellerPortalProduct(?:Categories|Schema)/);
    expect(source).not.toMatch(/\brequest\s*(?:<[^;]+?>)?\s*\(/);
    expect(source).not.toMatch(/from\s+['"]@\/services\/product\//);
    expectSource(
      'src/pages/Portal/Home/BuyerProductSchemaPreview.js',
      "export { default } from './BuyerProductSchemaPreview.tsx';",
    );
  });

  it('keeps portal product list js mirrors as pure re-exports', () => {
    expectSource(
      'src/pages/Portal/Home/SellerOwnDistributionProductList.js',
      "export { default } from './SellerOwnDistributionProductList.tsx';",
    );
    expectSource(
      'src/pages/Portal/Home/BuyerDistributionProductList.js',
      "export { default } from './BuyerDistributionProductList.tsx';",
    );
  });

  it('keeps portal home scoped to the minimal account/dept/role frame', () => {
    const homeSource = read('src/pages/Portal/Home/index.tsx');

    for (const forbiddenToken of [
      "import SellerProductSchemaPreview from './SellerProductSchemaPreview';",
      "import BuyerProductSchemaPreview from './BuyerProductSchemaPreview';",
      "import SellerOwnDistributionProductList from './SellerOwnDistributionProductList';",
      "import BuyerDistributionProductList from './BuyerDistributionProductList';",
      '<SellerProductSchemaPreview',
      '<BuyerProductSchemaPreview',
      '<SellerOwnDistributionProductList',
      '<BuyerDistributionProductList',
      'canViewProductSchema',
      'canViewDistributionProducts',
      'canQueryDistributionProducts',
      "portalPermission(terminal, 'product:category:list')",
      "portalPermission(terminal, 'product:schema:query')",
      "portalPermission(terminal, 'product:distribution:list')",
      "portalPermission(terminal, 'product:distribution:query')",
    ]) {
      expect(homeSource).not.toContain(forbiddenToken);
    }

    expect(homeSource).toContain("portalPermission(terminal, 'account:list')");
    expect(homeSource).toContain("portalPermission(terminal, 'dept:list')");
    expect(homeSource).toContain("portalPermission(terminal, 'role:list')");
  });

  it('keeps portal product detail actions fail-closed in detached reuse widgets', () => {
    const sellerSource = read('src/pages/Portal/Home/SellerOwnDistributionProductList.tsx');
    const buyerSource = read('src/pages/Portal/Home/BuyerDistributionProductList.tsx');

    for (const source of [sellerSource, buyerSource]) {
      expect(source).toContain('canQuery?: boolean');
      expect(source).toContain('canQuery = false');
      expect(source).toContain('if (!canQuery || !record.spuId)');
      expect(source).toContain('...(canQuery');
    }
  });

  it('keeps portal product business fields visible in seller and buyer templates', () => {
    const sellerSource = read('src/pages/Portal/Home/SellerOwnDistributionProductList.tsx');
    const buyerSource = read('src/pages/Portal/Home/BuyerDistributionProductList.tsx');
    const typeSource = read('src/types/seller-buyer/party.d.ts');
    const buyerTypeStart = typeSource.indexOf('interface BuyerPortalProductSku');
    const directLoginStart = typeSource.indexOf('interface PortalDirectLoginTicket');
    const buyerTypeBlock = typeSource.slice(buyerTypeStart, directLoginStart);

    expect(sellerSource).toContain('supplyPriceMin');
    expect(sellerSource).toContain('supplyPriceMax');
    expect(sellerSource).toContain('warehouseCount');
    expect(buyerSource).toContain('warehouseCount');
    expect(buyerTypeBlock).toContain('warehouseCount?: number');
    expect(buyerTypeBlock).not.toMatch(/\bsellerId\b/);
    expect(buyerTypeBlock).not.toMatch(/\bsellerSpuCode\b/);
    expect(buyerTypeBlock).not.toMatch(/\bsupplyPrice\w*\b/);
  });

});
