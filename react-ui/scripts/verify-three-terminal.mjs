import { spawnSync } from 'node:child_process';
import path from 'node:path';

const uiRoot = process.cwd();
const repoRoot = path.resolve(uiRoot, '..');
const backendRoot = path.join(repoRoot, 'RuoYi-Vue');

const backendTests = [
  'AdminDirectLoginPermissionContractTest',
  'AdminAccountPermissionUiContractTest',
  'SellerAdminPermissionContractTest',
  'BuyerAdminPermissionContractTest',
  'TerminalRouteOwnershipTest',
  'TerminalAccountIsolationTest',
  'TerminalSeedPermissionContractTest',
  'TerminalSqlIsolationContractTest',
  'PortalLoginResultTest',
  'PortalDirectLoginResultTest',
  'PortalDirectLoginTicketTest',
  'PortalHomeProfileSerializationTest',
  'PortalPreAuthorizeAspectTest',
  'LogAspectSensitiveFieldFilterTest',
  'PortalPermissionCheckerTest',
  'TokenServiceTerminalIsolationTest',
  'SellerServiceImplTest',
  'BuyerServiceImplTest',
].join(',');

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
    label: 'backend three-terminal contracts',
    cwd: backendRoot,
    command: 'mvn',
    args: [
      '-pl',
      'ruoyi-system,ruoyi-framework,seller,buyer',
      '-am',
      `-Dtest=${backendTests}`,
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
}

console.log('\nthree-terminal verification passed.');
