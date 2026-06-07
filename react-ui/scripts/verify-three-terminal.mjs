import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const uiRoot = process.cwd();
const repoRoot = path.resolve(uiRoot, '..');
const backendRoot = path.join(repoRoot, 'RuoYi-Vue');
const manifestPath = path.join(uiRoot, 'tests', 'three-terminal.manifest.json');

const manifest = readThreeTerminalManifest();
const backendTestClasses = manifest.backendTestClasses;
const backendTests = backendTestClasses.join(',');
const frontendTestPaths = manifest.frontendTestPaths;
const backendReportModules = readBackendReactorModules();
const frontendDiscoveryIgnoredDirs = new Set([
  'node_modules',
  'dist',
  'coverage',
  '.cache',
  '.umi',
  '.umi-production',
]);
const criticalBackendTestClassPattern = /(?:Terminal|ThreeTerminal|Portal|DirectLogin|Partner|SqlExecutionGuard|Admin.*Permission|Permission.*Account|SysMenuServiceImpl|LogAspectSensitiveFieldFilter|TokenServiceTerminalIsolation)/;
const criticalBackendExplicitTestClasses = new Set(manifest.criticalBackendExplicitTestClasses);
const frontendDiscoveryRoots = [
  path.join(uiRoot, 'tests'),
  path.join(uiRoot, 'src'),
];
const criticalFrontendTestPathPattern = /(?:terminal|portal|partner|remote-menu|direct-login|unauthorized|redirect|three-terminal|product-distribution-permission)/i;

function readThreeTerminalManifest() {
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  if (manifest.version !== 1) {
    throw new Error('three-terminal manifest version must be 1');
  }
  const backendTestClasses = readStringArray(manifest, 'backendTestClasses');
  const criticalBackendExplicitTestClasses = readStringArray(manifest, 'criticalBackendExplicitTestClasses');
  const frontendTestPaths = readStringArray(manifest, 'frontendTestPaths');
  assertUnique('backendTestClasses', backendTestClasses);
  assertUnique('criticalBackendExplicitTestClasses', criticalBackendExplicitTestClasses);
  assertUnique('frontendTestPaths', frontendTestPaths);

  const backendConfigured = new Set(backendTestClasses);
  const explicitMissing = criticalBackendExplicitTestClasses.filter((testClass) => !backendConfigured.has(testClass));
  if (explicitMissing.length > 0) {
    throw new Error(
      `critical backend explicit test classes are missing from backendTestClasses: ${explicitMissing.join(', ')}`,
    );
  }

  return {
    backendTestClasses,
    criticalBackendExplicitTestClasses,
    frontendTestPaths,
  };
}

function readStringArray(source, key) {
  const value = source[key];
  if (!Array.isArray(value) || value.length === 0) {
    throw new Error(`three-terminal manifest field must be a non-empty array: ${key}`);
  }
  const invalid = value.filter((item) => typeof item !== 'string' || item.trim() === '');
  if (invalid.length > 0) {
    throw new Error(`three-terminal manifest field contains non-string or blank values: ${key}`);
  }
  return value.map((item) => item.trim());
}

function assertUnique(key, values) {
  const seen = new Set();
  const duplicates = values.filter((value) => {
    if (seen.has(value)) {
      return true;
    }
    seen.add(value);
    return false;
  });
  if (duplicates.length > 0) {
    throw new Error(`three-terminal manifest field contains duplicates: ${key}: ${duplicates.join(', ')}`);
  }
}

function readBackendReactorModules() {
  const pomPath = path.join(backendRoot, 'pom.xml');
  const pom = fs.readFileSync(pomPath, 'utf8');
  const modules = [...pom.matchAll(/<module>([^<]+)<\/module>/g)].map((match) => match[1].trim());
  if (modules.length === 0) {
    throw new Error('RuoYi backend reactor modules were not found in pom.xml');
  }
  return modules;
}

function walkFiles(dir, files = [], ignoredDirs = new Set()) {
  if (!fs.existsSync(dir)) {
    return files;
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!ignoredDirs.has(entry.name)) {
        walkFiles(target, files, ignoredDirs);
      }
    } else {
      files.push(target);
    }
  }
  return files;
}

function getBackendTestSourceRoots() {
  return fs
    .readdirSync(backendRoot, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && backendReportModules.includes(entry.name))
    .map((entry) => path.join(backendRoot, entry.name, 'src', 'test', 'java'))
    .filter((root) => fs.existsSync(root));
}

function isCriticalBackendTestClass(testClass) {
  return criticalBackendTestClassPattern.test(testClass)
    || criticalBackendExplicitTestClasses.has(testClass);
}

function assertBackendTestSourcesExist() {
  const sourceRoots = getBackendTestSourceRoots();
  const testSourceFiles = sourceRoots
    .flatMap((root) => walkFiles(root))
    .filter((file) => file.endsWith('Test.java'));
  const testSources = new Map();

  for (const file of testSourceFiles) {
    const testClass = path.basename(file, '.java');
    const existing = testSources.get(testClass) ?? [];
    existing.push(path.relative(backendRoot, file));
    testSources.set(testClass, existing);
  }

  const missing = backendTestClasses.filter((testClass) => !testSources.has(testClass));
  if (missing.length > 0) {
    throw new Error(`backend test classes are missing: ${missing.join(', ')}`);
  }

  const configuredTests = new Set(backendTestClasses);
  const unlisted = [...testSources.keys()].filter((testClass) => {
    return isCriticalBackendTestClass(testClass) && !configuredTests.has(testClass);
  });
  if (unlisted.length > 0) {
    throw new Error(
      `critical backend test classes are not included in three-terminal manifest: ${unlisted.sort().join(', ')}`,
    );
  }

  const duplicates = [...testSources.entries()].filter(([, files]) => files.length > 1);
  if (duplicates.length > 0) {
    throw new Error(
      `backend test class names are duplicated: ${duplicates
        .map(([testClass, files]) => `${testClass} (${files.join(', ')})`)
        .join('; ')}`,
    );
  }
}

function assertFrontendTestSourcesIncluded() {
  const configuredTests = new Set(frontendTestPaths.map((file) => path.normalize(file)));
  const missing = [...configuredTests].filter((file) => !fs.existsSync(path.join(uiRoot, file)));
  if (missing.length > 0) {
    throw new Error(`configured frontend test files are missing: ${missing.sort().join(', ')}`);
  }

  const testFiles = frontendDiscoveryRoots.flatMap((root) => walkFiles(root, [], frontendDiscoveryIgnoredDirs))
    .filter((file) => /\.(test|spec)\.[cm]?[jt]sx?$/.test(path.basename(file)))
    .map((file) => path.normalize(path.relative(uiRoot, file)));
  const unlisted = testFiles.filter((file) => {
    return criticalFrontendTestPathPattern.test(file.replaceAll(path.sep, '/'))
      && !configuredTests.has(file);
  });

  if (unlisted.length > 0) {
    throw new Error(
      `critical frontend test files are not included in three-terminal manifest: ${unlisted.sort().join(', ')}`,
    );
  }
}

function clearBackendTestReports() {
  for (const moduleName of backendReportModules) {
    const reportDir = path.join(backendRoot, moduleName, 'target', 'surefire-reports');
    fs.rmSync(reportDir, { recursive: true, force: true });
  }
}

function assertBackendTestReportsExist() {
  const reportDirs = backendReportModules.map((moduleName) => {
    return path.join(backendRoot, moduleName, 'target', 'surefire-reports');
  });
  const reportFiles = reportDirs.flatMap((dir) => walkFiles(dir));
  const missing = [];
  const invalid = [];
  for (const testClass of backendTestClasses) {
    const xmlReports = reportFiles.filter((file) => {
      const baseName = path.basename(file);
      return baseName === `TEST-${testClass}.xml`
        || baseName.endsWith(`.${testClass}.xml`);
    });
    if (xmlReports.length === 0) {
      missing.push(testClass);
      continue;
    }
    for (const report of xmlReports) {
      const stats = readSurefireSuiteStats(report);
      if (stats.tests <= 0) {
        invalid.push(`${testClass} produced an empty surefire report: ${path.relative(backendRoot, report)}`);
      }
      if (stats.skipped > 0) {
        invalid.push(`${testClass} skipped ${stats.skipped} tests: ${path.relative(backendRoot, report)}`);
      }
    }
  }
  if (missing.length > 0) {
    throw new Error(`backend tests did not produce surefire reports: ${missing.join(', ')}`);
  }
  if (invalid.length > 0) {
    throw new Error(`backend surefire reports must execute all listed tests:\n${invalid.join('\n')}`);
  }
}

function readSurefireSuiteStats(reportFile) {
  const xml = fs.readFileSync(reportFile, 'utf8');
  const suiteStart = xml.match(/<testsuite\b[^>]*>/);
  if (!suiteStart) {
    throw new Error(`backend surefire report is missing <testsuite>: ${path.relative(backendRoot, reportFile)}`);
  }
  const attrs = suiteStart[0];
  return {
    tests: readXmlNumberAttribute(attrs, 'tests', reportFile),
    skipped: readXmlNumberAttribute(attrs, 'skipped', reportFile),
  };
}

function readXmlNumberAttribute(source, name, reportFile) {
  const match = source.match(new RegExp(`\\b${name}="(\\d+)"`));
  if (!match) {
    throw new Error(
      `backend surefire report is missing ${name} attribute: ${path.relative(backendRoot, reportFile)}`,
    );
  }
  return Number.parseInt(match[1], 10);
}

function runManifestCheck() {
  assertFrontendTestSourcesIncluded();
  assertBackendTestSourcesExist();
  console.log('three-terminal manifest check passed.');
}

if (process.argv.includes('--check-manifest')) {
  runManifestCheck();
  process.exit(0);
}

const steps = [
  {
    label: 'portal token guard',
    cwd: uiRoot,
    command: 'npm',
    args: ['run', 'guard:portal-token'],
  },
  {
    label: 'partner management guard',
    cwd: uiRoot,
    command: 'npm',
    args: ['run', 'guard:partner-management'],
  },
  {
    label: 'seller portal product guard',
    cwd: uiRoot,
    command: 'npm',
    args: ['run', 'guard:seller-portal-product'],
  },
  {
    label: 'buyer portal product guard',
    cwd: uiRoot,
    command: 'npm',
    args: ['run', 'guard:buyer-portal-product'],
  },
  {
    label: 'react typecheck',
    cwd: uiRoot,
    command: 'npm',
    args: ['run', 'tsc', '--', '--pretty', 'false'],
  },
  {
    label: 'portal session unit tests',
    cwd: uiRoot,
    before: assertFrontendTestSourcesIncluded,
    command: 'npm',
    args: [
      'run',
      'test:unit',
      '--',
      '--runTestsByPath',
      ...frontendTestPaths,
      '--runInBand',
    ],
  },
  {
    label: 'backend reactor test-compile',
    cwd: backendRoot,
    command: 'mvn',
    args: ['-pl', 'ruoyi-admin', '-am', '-DskipTests', 'test-compile'],
  },
  {
    label: 'backend three-terminal contracts',
    cwd: backendRoot,
    before: () => {
      assertBackendTestSourcesExist();
      clearBackendTestReports();
    },
    after: assertBackendTestReportsExist,
    command: 'mvn',
    args: [
      '-pl',
      'ruoyi-system,ruoyi-framework,integration,product,seller,buyer',
      '-am',
      `-Dtest=${backendTests}`,
      // Kept for reactor dependency modules; report checks above/below prevent silent class skips.
      '-Dsurefire.failIfNoSpecifiedTests=false',
      'test',
    ],
  },
];

function buildCommand(step) {
  if (process.platform !== 'win32') {
    return {
      command: step.command,
      args: step.args,
    };
  }

  return {
    command: 'cmd.exe',
    args: ['/d', '/s', '/c', [step.command, ...step.args].join(' ')],
  };
}

for (const step of steps) {
  console.log(`\n== ${step.label} ==`);
  if (step.before) {
    step.before();
  }
  const command = buildCommand(step);
  const result = spawnSync(command.command, command.args, {
    cwd: step.cwd,
    stdio: 'inherit',
  });
  if (result.error) {
    console.error(result.error.message);
    process.exit(1);
  }
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
  if (step.after) {
    step.after();
  }
}

console.log('\nthree-terminal verification passed.');
