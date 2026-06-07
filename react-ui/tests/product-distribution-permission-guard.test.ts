import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function expectRouteGuard(source: string, routePath: string, permission: string) {
  const pattern = new RegExp(
    `path:\\s*['"]${routePath.replaceAll('/', '\\/')}['"][\\s\\S]*?authority:\\s*\\[\\s*['"]${permission}['"]\\s*\\][\\s\\S]*?wrappers:\\s*\\[\\s*['"]@\\/wrappers\\/RemoteMenuRouteGuard['"]\\s*\\][\\s\\S]*?component:\\s*['"]\\.\\/Product\\/Distribution\\/EditPage['"]`,
  );

  expect(source).toMatch(pattern);
}

describe('product distribution permission guard', () => {
  it('guards direct create and edit routes with product distribution permissions', () => {
    for (const source of [
      readSource('config/routes.ts'),
      readSource('config/routes.js'),
    ]) {
      expectRouteGuard(source, '/product/distribution/create', 'product:distribution:add');
      expectRouteGuard(source, '/product/distribution/edit/:spuId', 'product:distribution:edit');
    }
  });

  it('guards dependent product and warehouse list APIs on the edit page', () => {
    const editPageTsx = readSource('src/pages/Product/Distribution/EditPage.tsx');
    const editPageJs = readSource('src/pages/Product/Distribution/EditPage.js');

    expect(editPageTsx).toContain("import { history, useAccess, useParams } from '@umijs/max'");
    expect(editPageTsx).toContain("const canQuerySourceProducts = access.hasPerms('product:list:list')");
    expect(editPageTsx).toContain("const canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list')");
    expect(editPageTsx).toContain("const canQueryThirdPartyWarehouses = access.hasPerms('warehouse:thirdParty:list')");
    expect(editPageTsx).toMatch(/const officialWarehouseRequest = canQueryOfficialWarehouses[\s\S]*?\? getOfficialWarehouseList/);
    expect(editPageTsx).toMatch(/const thirdPartyWarehouseRequest = selectedSellerId && canQueryThirdPartyWarehouses[\s\S]*?\? getThirdPartyWarehouseList/);
    expect(editPageTsx).toMatch(/const openSourceSelector = \(\) => \{[\s\S]*?if \(!canQuerySourceProducts\)/);
    expect(editPageTsx).toMatch(/request=\{async \(params\) => \{[\s\S]*?if \(!canQuerySourceProducts\)[\s\S]*?getSourceProductList/);
    expect(editPageTsx).toContain('disabled={!canQuerySourceProducts}');

    expect(editPageJs).toMatch(/from ["']@umijs\/max["']/);
    expect(editPageJs).toMatch(/const canQuerySourceProducts = access\.hasPerms\(["']product:list:list["']\)/);
    expect(editPageJs).toMatch(/const canQueryOfficialWarehouses = access\.hasPerms\(["']warehouse:official:list["']\)/);
    expect(editPageJs).toMatch(/const canQueryThirdPartyWarehouses = access\.hasPerms\(["']warehouse:thirdParty:list["']\)/);
    expect(editPageJs).toMatch(/const officialWarehouseRequest = canQueryOfficialWarehouses[\s\S]*?\? getOfficialWarehouseList/);
    expect(editPageJs).toMatch(/const thirdPartyWarehouseRequest = selectedSellerId && canQueryThirdPartyWarehouses[\s\S]*?\? getThirdPartyWarehouseList/);
    expect(editPageJs).toMatch(/const openSourceSelector = \(\) => \{[\s\S]*?if \(!canQuerySourceProducts\)/);
    expect(editPageJs).toMatch(/request: async \(params\d*\) => \{[\s\S]*?if \(!canQuerySourceProducts\)[\s\S]*?getSourceProductList/);
    expect(editPageJs).toContain('disabled: !canQuerySourceProducts');
  });

  it('does not mix seller or buyer admin permissions into the admin product page', () => {
    const sources = [
      readSource('src/pages/Product/Distribution/EditPage.tsx'),
      readSource('src/pages/Product/Distribution/EditPage.js'),
    ];

    for (const source of sources) {
      expect(source).not.toMatch(/seller:admin:|buyer:admin:/);
    }
  });
});
