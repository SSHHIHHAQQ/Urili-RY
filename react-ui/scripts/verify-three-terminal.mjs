import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const uiRoot = process.cwd();
const repoRoot = path.resolve(uiRoot, '..');
const backendRoot = path.join(repoRoot, 'RuoYi-Vue');

const backendTestClasses = [
  'AdminDirectLoginPermissionContractTest',
  'PortalDirectLoginAuthContractTest',
  'PortalAnonymousEndpointContractTest',
  'PortalLogAspectContractTest',
  'AdminAccountPermissionUiContractTest',
  'SellerAdminPermissionContractTest',
  'BuyerAdminPermissionContractTest',
  'TerminalRouteOwnershipTest',
  'TerminalAccountIsolationTest',
  'TerminalSeedPermissionContractTest',
  'TerminalSqlIsolationContractTest',
  'PortalDirectLoginTicketSqlContractTest',
  'StandalonePartnerSeedMenuContractTest',
  'SqlExecutionGuardContractTest',
  'PortalLoginResultTest',
  'PortalDirectLoginResultTest',
  'PortalDirectLoginTicketTest',
  'PortalHomeProfileSerializationTest',
  'PortalSessionProfileTest',
  'PortalAccountTest',
  'PortalTokenSupportTest',
  'PortalSessionContextTest',
  'PortalDirectLoginSupportTest',
  'PartnerSupportTest',
  'PortalPermissionCheckerTest',
  'PortalPermissionSupportTest',
  'PortalPreAuthorizeAspectTest',
  'PortalOperLogServiceImplTest',
  'LogAspectSensitiveFieldFilterTest',
  'TokenServiceTerminalIsolationTest',
  'PermissionServiceAccountPermissionTest',
  'SellerServiceImplTest',
  'SellerPortalPermissionServiceImplTest',
  'SellerPortalPermissionServiceImplMenuTreeTest',
  'SellerPortalPermissionServiceImplPortalAccessTest',
  'SellerPortalProductServiceImplTest',
  'BuyerServiceImplTest',
  'BuyerPortalPermissionServiceImplTest',
  'BuyerPortalPermissionServiceImplMenuTreeTest',
  'BuyerPortalPermissionServiceImplPortalAccessTest',
  'BuyerPortalProductServiceImplTest',
];
const backendTests = backendTestClasses.join(',');
const backendReportModules = [
  'ruoyi-system',
  'ruoyi-framework',
  'seller',
  'buyer',
];

function walkFiles(dir, files = []) {
  if (!fs.existsSync(dir)) {
    return files;
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walkFiles(target, files);
    } else {
      files.push(target);
    }
  }
  return files;
}

function assertBackendTestSourcesExist() {
  const sourceRoots = [
    path.join(backendRoot, 'ruoyi-system', 'src', 'test', 'java'),
    path.join(backendRoot, 'ruoyi-framework', 'src', 'test', 'java'),
    path.join(backendRoot, 'seller', 'src', 'test', 'java'),
    path.join(backendRoot, 'buyer', 'src', 'test', 'java'),
  ];
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
  const unlisted = [...testSources.keys()].filter((testClass) => !configuredTests.has(testClass));
  if (unlisted.length > 0) {
    throw new Error(
      `backend test classes are not included in verify-three-terminal.mjs: ${unlisted.sort().join(', ')}`,
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

function clearBackendTestReports() {
  for (const moduleName of backendReportModules) {
    const reportDir = path.join(backendRoot, moduleName, 'target', 'surefire-reports');
    fs.rmSync(reportDir, { recursive: true, force: true });
  }
}

function assertBackendTestReportsExist() {
  const reportDirs = [
    path.join(backendRoot, 'ruoyi-system', 'target', 'surefire-reports'),
    path.join(backendRoot, 'ruoyi-framework', 'target', 'surefire-reports'),
    path.join(backendRoot, 'seller', 'target', 'surefire-reports'),
    path.join(backendRoot, 'buyer', 'target', 'surefire-reports'),
  ];
  const reportFiles = reportDirs.flatMap((dir) => walkFiles(dir));
  const missing = backendTestClasses.filter((testClass) => !reportFiles.some((file) => {
    const baseName = path.basename(file);
    return baseName === `${testClass}.txt`
      || baseName.endsWith(`.${testClass}.txt`)
      || baseName === `TEST-${testClass}.xml`
      || baseName.endsWith(`.${testClass}.xml`);
  }));
  if (missing.length > 0) {
    throw new Error(`backend tests did not produce surefire reports: ${missing.join(', ')}`);
  }
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
    command: 'npm',
    args: [
      'run',
      'test:unit',
      '--',
      '--runTestsByPath',
      'tests/terminal-session-token.test.ts',
      'tests/portal-session-request.test.ts',
      'tests/portal-direct-login-message.test.ts',
      '--runInBand',
    ],
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
      'ruoyi-system,ruoyi-framework,seller,buyer',
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
