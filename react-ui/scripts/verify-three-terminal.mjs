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
const frontendGuardScripts = manifest.frontendGuardScripts;
const backendReportModules = readBackendReactorModules();
const frontendJestResultPath = path.join(uiRoot, 'node_modules', '.cache', 'three-terminal-jest-results.json');
const frontendJestCommand = path.join(
  uiRoot,
  'node_modules',
  '.bin',
  process.platform === 'win32' ? 'jest.cmd' : 'jest',
);
const frontendUmiTestSetupScript = path.join(uiRoot, 'scripts', 'prepare-umi-test.mjs');
const frontendUmiTestExportsPath = path.join(uiRoot, 'src', '.umi-test', 'exports.ts');
const cliArgs = process.argv.slice(2);
const frontendJestPassThroughArgs = readFrontendJestPassThroughArgs(cliArgs);
const frontendDiscoveryIgnoredDirs = new Set([
  'node_modules',
  'dist',
  'coverage',
  '.cache',
  '.umi',
  '.umi-production',
  '.umi-test',
  '.umi-undefined',
  'test-results',
]);
const criticalBackendTestClassPattern = /(?:Terminal|ThreeTerminal|Portal|DirectLogin|Partner|SqlExecutionGuard|Admin.*(?:Permission|Route)|Permission.*Account|SysMenuServiceImpl|LogAspectSensitiveFieldFilter|TokenServiceTerminalIsolation|Finance|Currency)/;
const criticalBackendTestPathPattern = /^(?:ruoyi-system[\\/]src[\\/]test[\\/]java[\\/]com[\\/]ruoyi[\\/]system[\\/]architecture[\\/]|seller[\\/]src[\\/]test[\\/]java[\\/]|buyer[\\/]src[\\/]test[\\/]java[\\/]|product[\\/]src[\\/]test[\\/]java[\\/]|integration[\\/]src[\\/]test[\\/]java[\\/]|inventory[\\/]src[\\/]test[\\/]java[\\/]|warehouse[\\/]src[\\/]test[\\/]java[\\/]|finance[\\/]src[\\/]test[\\/]java[\\/])/;
const criticalBackendExplicitTestClasses = new Set(manifest.criticalBackendExplicitTestClasses);
const frontendDiscoveryRoots = [uiRoot];
const criticalFrontendTestPathPattern = /(?:terminal|portal|partner|remote-menu|getrouters|authority|auth-sidecar|direct-login|unauthorized|redirect|three-terminal|namespace|menu-id|menu-range|account-scope|permission-contract|system-user-service|product-distribution-permission|product-center|source-product|upstream-system-permission|inventory-overview|inventory-adjustment-review|source-warehouse|warehouse|finance|currency)/i;
const criticalFrontendExplicitTestPaths = new Set(
  manifest.criticalFrontendExplicitTestPaths.map(normalizeFrontendTestPath),
);

function readFrontendJestPassThroughArgs(args) {
  const allowedJestArgs = new Set(['--coverage', '-u', '--updateSnapshot']);
  const passThroughArgs = [];
  const unsupported = [];
  for (const arg of args) {
    if (arg === '--check-manifest') {
      continue;
    }
    if (allowedJestArgs.has(arg)) {
      passThroughArgs.push(arg);
    } else {
      unsupported.push(arg);
    }
  }
  if (unsupported.length > 0) {
    throw new Error(
      `unsupported verify-three-terminal arguments: ${unsupported.join(', ')}. `
        + 'Use the three-terminal gate; only --coverage, -u, and --updateSnapshot can be forwarded to Jest.',
    );
  }
  return passThroughArgs;
}

function readThreeTerminalManifest() {
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  if (manifest.version !== 1) {
    throw new Error('three-terminal manifest version must be 1');
  }
  const backendTestClasses = readStringArray(manifest, 'backendTestClasses');
  const criticalBackendExplicitTestClasses = readStringArray(manifest, 'criticalBackendExplicitTestClasses');
  const frontendTestPaths = readStringArray(manifest, 'frontendTestPaths');
  const criticalFrontendExplicitTestPaths = readStringArray(manifest, 'criticalFrontendExplicitTestPaths');
  const frontendGuardScriptEntries = readFrontendGuardScriptEntries(manifest);
  const frontendGuardScripts = frontendGuardScriptEntries.map((entry) => entry.name);
  assertUnique('backendTestClasses', backendTestClasses);
  assertUnique('criticalBackendExplicitTestClasses', criticalBackendExplicitTestClasses);
  assertUnique('frontendTestPaths', frontendTestPaths);
  assertUnique('criticalFrontendExplicitTestPaths', criticalFrontendExplicitTestPaths);
  assertUnique('frontendGuardScripts', frontendGuardScripts);
  assertFrontendGuardScriptsMatch(frontendGuardScriptEntries);

  const backendConfigured = new Set(backendTestClasses);
  const explicitMissing = criticalBackendExplicitTestClasses.filter((testClass) => !backendConfigured.has(testClass));
  if (explicitMissing.length > 0) {
    throw new Error(
      `critical backend explicit test classes are missing from backendTestClasses: ${explicitMissing.join(', ')}`,
    );
  }

  const frontendConfigured = new Set(frontendTestPaths.map(normalizeFrontendTestPath));
  const frontendExplicitMissing = criticalFrontendExplicitTestPaths
    .filter((testPath) => !frontendConfigured.has(normalizeFrontendTestPath(testPath)));
  if (frontendExplicitMissing.length > 0) {
    throw new Error(
      `critical frontend test files are not included in three-terminal manifest: ${frontendExplicitMissing.join(', ')}`,
    );
  }

  return {
    backendTestClasses,
    criticalBackendExplicitTestClasses,
    frontendTestPaths,
    criticalFrontendExplicitTestPaths,
    frontendGuardScripts,
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

function readFrontendGuardScriptEntries(source) {
  const key = 'frontendGuardScripts';
  const value = source[key];
  if (!Array.isArray(value) || value.length === 0) {
    throw new Error(`three-terminal manifest field must be a non-empty array: ${key}`);
  }
  const invalid = value.filter((item) => {
    return item === null
      || typeof item !== 'object'
      || typeof item.name !== 'string'
      || item.name.trim() === ''
      || typeof item.expectedCommand !== 'string'
      || item.expectedCommand.trim() === '';
  });
  if (invalid.length > 0) {
    throw new Error(`three-terminal manifest field contains invalid guard script entries: ${key}`);
  }
  return value.map((item) => ({
    name: item.name.trim(),
    expectedCommand: item.expectedCommand.trim(),
  }));
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

function assertFrontendGuardScriptsMatch(guardScripts) {
  const packageJson = JSON.parse(fs.readFileSync(path.join(uiRoot, 'package.json'), 'utf8'));
  const scripts = packageJson.scripts ?? {};
  const missing = guardScripts
    .map((script) => script.name)
    .filter((scriptName) => typeof scripts[scriptName] !== 'string');
  if (missing.length > 0) {
    throw new Error(`frontend guard scripts are missing from package.json: ${missing.join(', ')}`);
  }

  const mismatched = guardScripts.filter((script) => scripts[script.name] !== script.expectedCommand);
  if (mismatched.length > 0) {
    throw new Error(
      `frontend guard script commands differ from three-terminal manifest:\n${mismatched
        .map((script) => `${script.name}: expected "${script.expectedCommand}", actual "${scripts[script.name]}"`)
        .join('\n')}`,
    );
  }

  const configured = new Set(guardScripts.map((script) => script.name));
  const unlisted = Object.keys(scripts)
    .filter((scriptName) => scriptName.startsWith('guard:') && !configured.has(scriptName));
  if (unlisted.length > 0) {
    throw new Error(
      `frontend guard scripts are not included in three-terminal manifest: ${unlisted.sort().join(', ')}`,
    );
  }

  assertFrontendGuardScriptTargetsExist(guardScripts);
}

function assertPublicTestScriptsUseThreeTerminalVerifier() {
  const packageJson = JSON.parse(fs.readFileSync(path.join(uiRoot, 'package.json'), 'utf8'));
  const scripts = packageJson.scripts ?? {};
  const requiredScripts = {
    test: 'node scripts/verify-three-terminal.mjs',
    'test:coverage': 'node scripts/verify-three-terminal.mjs --coverage',
    'test:update': 'node scripts/verify-three-terminal.mjs -u',
    'test:unit': 'node scripts/verify-three-terminal.mjs',
    jest: 'node scripts/verify-three-terminal.mjs',
  };
  const missing = Object.keys(requiredScripts).filter((scriptName) => typeof scripts[scriptName] !== 'string');
  if (missing.length > 0) {
    throw new Error(`public test scripts are missing from package.json: ${missing.join(', ')}`);
  }
  if (scripts['verify:three-terminal'] !== 'node scripts/verify-three-terminal.mjs') {
    throw new Error('verify:three-terminal must point to node scripts/verify-three-terminal.mjs');
  }

  const mismatched = Object.entries(requiredScripts)
    .filter(([scriptName, expectedCommand]) => scripts[scriptName] !== expectedCommand)
    .map(([scriptName, expectedCommand]) => `${scriptName}: expected "${expectedCommand}", actual "${scripts[scriptName]}"`);
  if (mismatched.length > 0) {
    throw new Error(
      `public test scripts must call verify-three-terminal directly so CLI args are preserved:\n${mismatched.join('\n')}`,
    );
  }
}

function assertFrontendGuardScriptTargetsExist(guardScripts) {
  const missing = [];
  for (const script of guardScripts) {
    const target = parseNodeScriptTarget(script.expectedCommand);
    if (!target) {
      missing.push(`${script.name}: command must be "node scripts/*.mjs"`);
      continue;
    }
    if (!fs.existsSync(path.join(uiRoot, target))) {
      missing.push(`${script.name}: ${target}`);
    }
  }
  if (missing.length > 0) {
    throw new Error(`frontend guard script targets are missing or invalid:\n${missing.join('\n')}`);
  }
}

function parseNodeScriptTarget(command) {
  const parts = command.trim().split(/\s+/);
  if (parts.length !== 2 || parts[0] !== 'node') {
    return null;
  }
  const scriptPath = normalizeFrontendTestPath(parts[1]);
  if (!scriptPath.startsWith('scripts/') || !scriptPath.endsWith('.mjs')) {
    return null;
  }
  return scriptPath;
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
  return backendReportModules
    .map((moduleName) => path.join(backendRoot, moduleName, 'src', 'test', 'java'))
    .filter((root) => fs.existsSync(root));
}

function isCriticalBackendTestClass(testClass, files = []) {
  return criticalBackendTestClassPattern.test(testClass)
    || criticalBackendExplicitTestClasses.has(testClass)
    || files.some((file) => criticalBackendTestPathPattern.test(file.replaceAll(path.sep, '/')));
}

function collectBackendTestSources() {
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

  return testSources;
}

function getBackendTestModules() {
  const testSources = collectBackendTestSources();
  const missing = [];
  const selectedModules = new Set();

  for (const testClass of backendTestClasses) {
    const files = testSources.get(testClass);
    if (!files) {
      missing.push(testClass);
      continue;
    }
    for (const file of files) {
      const moduleName = file.split(/[\\/]/)[0];
      selectedModules.add(moduleName);
    }
  }

  if (missing.length > 0) {
    throw new Error(`backend test classes are missing: ${missing.join(', ')}`);
  }

  const modules = backendReportModules.filter((moduleName) => selectedModules.has(moduleName));
  if (modules.length === 0) {
    throw new Error('backend manifest did not select any test modules');
  }
  return modules;
}

function assertBackendTestSourcesExist() {
  const testSources = collectBackendTestSources();

  const missing = backendTestClasses.filter((testClass) => !testSources.has(testClass));
  if (missing.length > 0) {
    throw new Error(`backend test classes are missing: ${missing.join(', ')}`);
  }

  const configuredTests = new Set(backendTestClasses);
  const unlisted = [...testSources.entries()].filter(([testClass, files]) => {
    return isCriticalBackendTestClass(testClass, files) && !configuredTests.has(testClass);
  }).map(([testClass]) => testClass);
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
  assertGeneratedFrontendTestMirrors(testFiles);
  const sourceTestFiles = testFiles.filter((file) => !isGeneratedFrontendTestMirror(file));
  const unlisted = sourceTestFiles.filter((file) => {
    const normalized = normalizeFrontendTestPath(file);
    return (
      criticalFrontendExplicitTestPaths.has(normalized)
      || criticalFrontendTestPathPattern.test(normalized)
    ) && !configuredTests.has(file);
  });

  if (unlisted.length > 0) {
    throw new Error(
      `critical frontend test files are not included in three-terminal manifest: ${unlisted.sort().join(', ')}`,
    );
  }
}

function isGeneratedFrontendTestMirror(file) {
  return resolveGeneratedFrontendTestMirrorSource(file) !== null;
}

function resolveGeneratedFrontendTestMirrorSource(file) {
  if (!file.endsWith('.js')) {
    return null;
  }
  const sourceBase = file.slice(0, -'.js'.length);
  for (const extension of ['.ts', '.tsx']) {
    const sourceFile = `${sourceBase}${extension}`;
    if (fs.existsSync(path.join(uiRoot, sourceFile))) {
      return sourceFile;
    }
  }
  return null;
}

function assertGeneratedFrontendTestMirrors(testFiles) {
  const violations = [];
  for (const file of testFiles) {
    const sourceFile = resolveGeneratedFrontendTestMirrorSource(file);
    if (!sourceFile) {
      continue;
    }
    const expected = `export * from './${path.basename(sourceFile)}';\n`;
    const actual = fs.readFileSync(path.join(uiRoot, file), 'utf8').replace(/\r\n/g, '\n');
    if (actual !== expected) {
      violations.push(`${file} must be a pure re-export of ${sourceFile}`);
    }
  }
  if (violations.length > 0) {
    throw new Error(`generated frontend test mirrors must be pure re-exports:\n${violations.sort().join('\n')}`);
  }
}

function prepareFrontendJestResultFile() {
  assertFrontendTestSourcesIncluded();
  if (!fs.existsSync(frontendJestCommand)) {
    throw new Error(`frontend Jest binary is missing: ${path.relative(uiRoot, frontendJestCommand)}`);
  }
  assertFrontendUmiTestExportsReady();
  fs.mkdirSync(path.dirname(frontendJestResultPath), { recursive: true });
  fs.rmSync(frontendJestResultPath, { force: true });
}

function assertFrontendUmiTestExportsReady() {
  if (!fs.existsSync(frontendUmiTestExportsPath)) {
    throw new Error(
      `frontend Jest Umi exports are missing: ${path.relative(uiRoot, frontendUmiTestExportsPath)}. `
        + 'Run max setup before executing three-terminal Jest tests.',
    );
  }
}

function assertFrontendJestResults() {
  if (!fs.existsSync(frontendJestResultPath)) {
    throw new Error(`frontend Jest did not produce result JSON: ${path.relative(uiRoot, frontendJestResultPath)}`);
  }
  const report = JSON.parse(fs.readFileSync(frontendJestResultPath, 'utf8'));
  const violations = [];
  const expected = new Set(frontendTestPaths.map(normalizeFrontendTestPath));
  const results = Array.isArray(report.testResults) ? report.testResults : [];
  const actual = new Map();

  for (const result of results) {
    if (!result || typeof result.name !== 'string') {
      continue;
    }
    actual.set(normalizeFrontendTestPath(path.relative(uiRoot, result.name)), result);
  }

  for (const testPath of expected) {
    const result = actual.get(testPath);
    if (!result) {
      violations.push(`${testPath} did not appear in frontend Jest results`);
      continue;
    }
    const assertions = Array.isArray(result.assertionResults) ? result.assertionResults : [];
    const passingAssertions = assertions.filter((assertion) => assertion.status === 'passed').length;
    const pendingAssertions = assertions.filter((assertion) => assertion.status === 'pending').length;
    const todoAssertions = assertions.filter((assertion) => assertion.status === 'todo').length;

    if (result.status !== 'passed') {
      violations.push(`${testPath} reported status ${result.status ?? 'unknown'}`);
    }
    if (passingAssertions <= 0) {
      violations.push(`${testPath} did not execute any passing tests`);
    }
    if (pendingAssertions > 0) {
      violations.push(`${testPath} skipped ${pendingAssertions} tests`);
    }
    if (todoAssertions > 0) {
      violations.push(`${testPath} has ${todoAssertions} todo tests`);
    }
  }

  if (Number(report.numTotalTests) <= 0 || Number(report.numPassedTests) <= 0) {
    violations.push('frontend Jest reported no executed passing tests');
  }
  if (Number(report.numPendingTests) > 0) {
    violations.push(`frontend Jest reported ${report.numPendingTests} skipped tests`);
  }
  if (Number(report.numTodoTests) > 0) {
    violations.push(`frontend Jest reported ${report.numTodoTests} todo tests`);
  }

  if (violations.length > 0) {
    throw new Error(`frontend Jest results must execute all listed tests without skip/todo:\n${violations.join('\n')}`);
  }
}

function normalizeFrontendTestPath(file) {
  return path.normalize(file).replaceAll(path.sep, '/');
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
  assertPublicTestScriptsUseThreeTerminalVerifier();
  assertFrontendTestSourcesIncluded();
  assertBackendTestSourcesExist();
  console.log('three-terminal manifest check passed.');
}

if (process.argv.includes('--check-manifest')) {
  runManifestCheck();
  process.exit(0);
}

assertPublicTestScriptsUseThreeTerminalVerifier();

const steps = [
  ...frontendGuardScripts.map((scriptName) => ({
    label: scriptName.replace(/^guard:/, '').replaceAll('-', ' ') + ' guard',
    cwd: uiRoot,
    command: 'npm',
    args: ['run', scriptName],
  })),
  {
    label: 'umi test setup',
    cwd: uiRoot,
    command: process.execPath,
    args: [frontendUmiTestSetupScript],
    after: assertFrontendUmiTestExportsReady,
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
    before: prepareFrontendJestResultFile,
    after: assertFrontendJestResults,
    command: frontendJestCommand,
    args: [
      '--config',
      'jest.config.ts',
      '--runTestsByPath',
      ...frontendTestPaths,
      ...frontendJestPassThroughArgs,
      '--runInBand',
      '--json',
      '--outputFile',
      normalizeFrontendTestPath(frontendJestResultPath),
    ],
  },
  {
    label: 'backend reactor test-compile',
    cwd: backendRoot,
    command: 'mvn',
    args: ['-pl', backendReportModules.join(','), '-am', '-DskipTests', 'test-compile'],
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
      getBackendTestModules().join(','),
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
