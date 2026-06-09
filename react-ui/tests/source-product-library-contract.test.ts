import fs from 'node:fs';
import path from 'node:path';

const uiRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(uiRoot, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function readRepoSource(relativePath: string) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8');
}

function expectPureSource(relativePath: string, expected: string) {
  expect(readSource(relativePath).trim()).toBe(expected);
}

describe('source product library contract', () => {
  it('keeps source product library JS mirrors as pure TS/TSX re-exports', () => {
    expectPureSource('src/services/integration/sourceProduct.js', "export * from './sourceProduct.ts';");
    expectPureSource('src/pages/Product/SourceProductLibrary/index.js', "export { default } from './index.tsx';");
    expectPureSource(
      'src/pages/Product/SourceProductLibrary/SourceProductDetailDrawer.js',
      "export { default } from './SourceProductDetailDrawer.tsx';",
    );
    expectPureSource('src/pages/Product/SourceProductLibrary/constants.js', "export * from './constants.ts';");
  });

  it('keeps source product library on integration admin query permission', () => {
    const service = readSource('src/services/integration/sourceProduct.ts');
    const page = readSource('src/pages/Product/SourceProductLibrary/index.tsx');
    const businessSeed = readRepoSource('RuoYi-Vue/sql/business_menu_seed.sql');
    const componentSeed = readRepoSource('RuoYi-Vue/sql/20260605_source_product_library_menu_component.sql');

    expect(service).toContain("const baseUrl = '/api/integration/admin/source-products';");
    expect(page).toContain("const access = useAccess();");
    expect(page).toContain("const canQuerySourceProducts = access.hasPerms('integration:upstream:query');");
    expect(page).not.toContain("access.hasPerms('product:list:list')");
    expect(page).toMatch(/const openDetail = \(record:[\s\S]*?if \(!canQuerySourceProducts\)/);
    expect(page).toMatch(/request=\{async \(params\) => \{[\s\S]*?if \(!canQuerySourceProducts\)/);
    expect(businessSeed).toContain(
      "(2400, 2060, 'C', 'list', 'Product/SourceProductLibrary/index', 'SourceProductLibrary', 'integration:upstream:query')",
    );
    expect(componentSeed).toContain("perms = 'integration:upstream:query'");
  });

  it('maps source product library pagination to RuoYi page parameters', () => {
    const page = readSource('src/pages/Product/SourceProductLibrary/index.tsx');

    expect(page).toContain('const { current, pageSize, repositoryScope: scope, ...filters } = params;');
    expect(page).toMatch(/getSourceProductList\([\s\S]*?cleanParams\(\{[\s\S]*?pageNum:\s*current[\s\S]*?pageSize[\s\S]*?\.\.\.filters[\s\S]*?\}\)/);
    expect(page).not.toContain('getSourceProductList(params)');
  });

  it('does not expose unsupported source product repository scopes', () => {
    const page = readSource('src/pages/Product/SourceProductLibrary/index.tsx');
    const backendService = readRepoSource(
      'RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java',
    );

    expect(page).toContain("{ key: 'OFFICIAL_MASTER', label: '官方主仓' }");
    expect(page).not.toContain('THIRD_PARTY_MASTER');
    expect(backendService).toContain(
      '!SourceProductReadModelService.REPOSITORY_SCOPE_OFFICIAL_MASTER.equals(normalized.getRepositoryScope())',
    );
    expect(backendService).toContain('Unsupported source product repository scope');
  });

  it('keeps source product library tests in the three-terminal gate', () => {
    const manifest = JSON.parse(readSource('tests/three-terminal.manifest.json'));
    const verifier = readSource('scripts/verify-three-terminal.mjs');

    expect(manifest.frontendTestPaths).toContain('tests/source-product-library-contract.test.ts');
    expect(verifier).toContain('source-product');
    expect(verifier).toContain('product-center');
  });
});
