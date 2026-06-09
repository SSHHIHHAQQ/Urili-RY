import fs from 'fs';
import path from 'path';
import { spawnSync } from 'child_process';

const uiRoot = path.resolve(__dirname, '..');

function readSource(relativePath: string) {
  return fs.readFileSync(path.join(uiRoot, relativePath), 'utf8');
}

function runManifestCheck() {
  return spawnSync(process.execPath, ['scripts/verify-three-terminal.mjs', '--check-manifest'], {
    cwd: uiRoot,
    encoding: 'utf8',
  });
}

function withMutatedJsonFile(relativePath: string, mutate: (value: any) => void, run: () => void) {
  const target = path.join(uiRoot, relativePath);
  const original = fs.readFileSync(target, 'utf8');

  try {
    const value = JSON.parse(original);
    mutate(value);
    fs.writeFileSync(target, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
    run();
  } finally {
    fs.writeFileSync(target, original, 'utf8');
  }
}

describe('verify-three-terminal backend gate', () => {
  it('selects backend contract modules from manifest test classes, not every module with test sources', () => {
    const source = readSource('scripts/verify-three-terminal.mjs');

    expect(source).toContain('function collectBackendTestSources()');
    expect(source).toContain('function getBackendTestModules()');
    expect(source).toMatch(/for \(const testClass of backendTestClasses\)/);
    expect(source).toMatch(/const files = testSources\.get\(testClass\)/);
    expect(source).toMatch(/selectedModules\.add\(moduleName\)/);
    expect(source).toMatch(/backendReportModules\.filter\(\(moduleName\) => selectedModules\.has\(moduleName\)\)/);
    expect(source).not.toMatch(
      /function getBackendTestModules\(\) \{[\s\S]*?src['"], ['"]test['"], ['"]java['"][\s\S]*?\}/,
    );
  });

  it('treats integration backend tests as critical manifest-owned tests', () => {
    const source = readSource('scripts/verify-three-terminal.mjs');

    expect(source).toContain('integration[\\\\/]src[\\\\/]test[\\\\/]java');
    expect(source).toContain('product[\\\\/]src[\\\\/]test[\\\\/]java');
    expect(source).toContain('warehouse[\\\\/]src[\\\\/]test[\\\\/]java');
    expect(source).toContain('finance[\\\\/]src[\\\\/]test[\\\\/]java');
    expect(source).toMatch(/Finance\|Currency/);
    expect(source).toContain('criticalBackendTestPathPattern');
    expect(source).toContain('critical backend test classes are not included in three-terminal manifest');
    expect(source).toMatch(/source-warehouse\|warehouse\|finance/);
    expect(source).toMatch(/system-user-service/);
    expect(source).toMatch(/inventory-adjustment-review/);
  });

  it('keeps backend reactor modules and maven command wiring dynamic', () => {
    const source = readSource('scripts/verify-three-terminal.mjs');

    expect(source).toContain('const backendReportModules = readBackendReactorModules();');
    expect(source).toContain('function readBackendReactorModules()');
    expect(source).toContain("args: ['-pl', backendReportModules.join(','), '-am', '-DskipTests', 'test-compile']");
    expect(source).toContain("'-pl',\n      getBackendTestModules().join(','),\n      '-am'");
    expect(source).not.toMatch(/const backendReportModules = \[[\s\S]*?\];/);
  });

  it('prepares Umi test exports before running frontend Jest', () => {
    const source = readSource('scripts/verify-three-terminal.mjs');
    const setupSource = readSource('scripts/prepare-umi-test.mjs');

    expect(source).toContain("const frontendUmiTestSetupScript = path.join(uiRoot, 'scripts', 'prepare-umi-test.mjs');");
    expect(source).toContain("const frontendUmiTestExportsPath = path.join(uiRoot, 'src', '.umi-test', 'exports.ts');");
    expect(source).toContain('function assertFrontendUmiTestExportsReady()');
    expect(source).toContain("label: 'umi test setup'");
    expect(source).toContain('command: process.execPath');
    expect(source).toContain('args: [frontendUmiTestSetupScript]');
    expect(source).toMatch(/label: 'umi test setup'[\s\S]*?after: assertFrontendUmiTestExportsReady[\s\S]*?label: 'react typecheck'/);
    expect(source).toMatch(/assertFrontendUmiTestExportsReady\(\);[\s\S]*fs\.mkdirSync\(path\.dirname\(frontendJestResultPath\)/);
    expect(setupSource).toContain("process.env.NODE_ENV = 'test';");
    expect(setupSource).toContain("await import('@umijs/max/test.js')");
    expect(setupSource).toContain('await configUmiAlias({');
    expect(setupSource).toContain("target: 'browser'");
    expect(setupSource).toContain("path.join(uiRoot, 'src', '.umi-test', 'exports.ts')");
  });

  it('ignores local generated frontend directories during manifest discovery', () => {
    const source = readSource('scripts/verify-three-terminal.mjs');
    const generatedDirs = [
      '.umi-test',
      '.umi-undefined',
      'test-results',
    ];

    generatedDirs.forEach((dirName) => {
      expect(source).toContain(`'${dirName}'`);
    });

    const tempFiles = [
      path.join(uiRoot, 'src', '.umi-test', 'portal-generated.test.ts'),
      path.join(uiRoot, 'src', '.umi-undefined', 'partner-generated.test.ts'),
      path.join(uiRoot, 'test-results', 'three-terminal-generated.test.ts'),
    ];

    try {
      tempFiles.forEach((file) => {
        fs.mkdirSync(path.dirname(file), { recursive: true });
        fs.writeFileSync(file, "test('generated local artifact', () => expect(true).toBe(true));\n", 'utf8');
      });

      const result = runManifestCheck();

      expect(result.status).toBe(0);
    } finally {
      tempFiles.forEach((file) => fs.rmSync(file, { force: true }));
    }
  });

  it('runs the domain JS mirror guard from lint and covers shared admin domains', () => {
    const packageJson = JSON.parse(readSource('package.json'));
    const mirrorGuard = readSource('scripts/check-product-upstream-js-mirrors.mjs');

    expect(packageJson.scripts.lint).toContain('npm run guard:product-upstream-mirrors');
    expect(mirrorGuard).toContain('src/pages/Inventory');
    expect(mirrorGuard).toContain('src/pages/Warehouse');
    expect(mirrorGuard).toContain('src/pages/Finance');
    expect(mirrorGuard).toContain('src/components/ProductCenter');
    expect(mirrorGuard).toContain('src/services/inventory');
    expect(mirrorGuard).toContain('src/services/warehouse');
    expect(mirrorGuard).toContain('src/services/finance');
  });

  it('rejects drifted generated frontend test mirrors', () => {
    const tempDir = path.join(uiRoot, 'tests', '__mirror_guard_tmp__');
    fs.mkdirSync(tempDir, { recursive: true });

    try {
      fs.writeFileSync(
        path.join(tempDir, 'mirror-drift.test.ts'),
        "test('placeholder', () => expect(true).toBe(true));\n",
        'utf8',
      );
      fs.writeFileSync(
        path.join(tempDir, 'mirror-drift.test.js'),
        "test('drifted mirror', () => expect(true).toBe(true));\n",
        'utf8',
      );

      const result = runManifestCheck();

      expect(result.status).not.toBe(0);
      expect(`${result.stdout}${result.stderr}`).toContain(
        'generated frontend test mirrors must be pure re-exports',
      );
    } finally {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  it('rejects public test scripts that bypass the three-terminal verifier', () => {
    withMutatedJsonFile('package.json', (packageJson) => {
      packageJson.scripts.test = 'jest --config jest.config.ts';
    }, () => {
      const result = runManifestCheck();

      expect(result.status).not.toBe(0);
      expect(`${result.stdout}${result.stderr}`).toContain(
        'public test scripts must call verify-three-terminal directly',
      );
    });
  });

  it('keeps admin account, seller/buyer admin permission, and partner seed contracts critical', () => {
    const manifest = JSON.parse(readSource('tests/three-terminal.manifest.json'));
    const critical = new Set(manifest.criticalBackendExplicitTestClasses);

    [
      'AdminAccountPermissionUiContractTest',
      'SellerAdminPermissionContractTest',
      'BuyerAdminPermissionContractTest',
      'StandalonePartnerSeedMenuContractTest',
    ].forEach((testClass) => {
      expect(critical.has(testClass)).toBe(true);
    });
  });

  it.each([
    'IntegrationAdminPermissionContractTest',
    'AdminAccountPermissionUiContractTest',
    'SellerAdminPermissionContractTest',
    'BuyerAdminPermissionContractTest',
    'StandalonePartnerSeedMenuContractTest',
  ])('rejects critical backend test removed from the three-terminal manifest: %s', (criticalTestClass) => {
    withMutatedJsonFile('tests/three-terminal.manifest.json', (manifest) => {
      manifest.backendTestClasses = manifest.backendTestClasses.filter(
        (testClass: string) => testClass !== criticalTestClass,
      );
    }, () => {
      const result = runManifestCheck();

      expect(result.status).not.toBe(0);
      expect(`${result.stdout}${result.stderr}`).toMatch(
        /critical backend (?:test classes are not included in three-terminal manifest|explicit test classes are missing from backendTestClasses)/,
      );
      expect(`${result.stdout}${result.stderr}`).toContain(criticalTestClass);
    });
  });

  it.each([
    'tests/upstream-system-permission-guard.test.ts',
    'tests/system-user-service-contract.test.ts',
    'tests/inventory-adjustment-review-contract.test.ts',
  ])('rejects critical frontend test removed from the three-terminal manifest: %s', (criticalTestPath) => {
    withMutatedJsonFile('tests/three-terminal.manifest.json', (manifest) => {
      manifest.frontendTestPaths = manifest.frontendTestPaths.filter(
        (testPath: string) => testPath !== criticalTestPath,
      );
    }, () => {
      const result = runManifestCheck();

      expect(result.status).not.toBe(0);
      expect(`${result.stdout}${result.stderr}`).toContain(
        'critical frontend test files are not included in three-terminal manifest',
      );
      expect(`${result.stdout}${result.stderr}`.replaceAll('\\', '/')).toContain(
        criticalTestPath,
      );
    });
  });

  it('rejects guard scripts that are missing from the three-terminal manifest', () => {
    withMutatedJsonFile('package.json', (packageJson) => {
      packageJson.scripts['guard:unlisted-three-terminal-drift'] = 'node scripts/check-portal-token-isolation.mjs';
    }, () => {
      const result = runManifestCheck();

      expect(result.status).not.toBe(0);
      expect(`${result.stdout}${result.stderr}`).toContain(
        'frontend guard scripts are not included in three-terminal manifest',
      );
    });
  });
});
