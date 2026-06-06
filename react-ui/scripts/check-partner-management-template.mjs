import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();

const files = {
  sellerPage: path.join(root, 'src', 'pages', 'Seller', 'index.tsx'),
  buyerPage: path.join(root, 'src', 'pages', 'Buyer', 'index.tsx'),
  sellerService: path.join(root, 'src', 'services', 'seller', 'seller.ts'),
  sellerServiceJs: path.join(root, 'src', 'services', 'seller', 'seller.js'),
  buyerService: path.join(root, 'src', 'services', 'buyer', 'buyer.ts'),
  buyerServiceJs: path.join(root, 'src', 'services', 'buyer', 'buyer.js'),
  pageTemplate: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerManagementPage.tsx',
  ),
  pageTemplateJs: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerManagementPage.js',
  ),
  accountModal: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerAccountModal.tsx',
  ),
  accountModalJs: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerAccountModal.js',
  ),
  sessionModal: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerSessionModal.tsx',
  ),
  sessionModalJs: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerSessionModal.js',
  ),
  auditModal: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerAuditModal.tsx',
  ),
  partnerTypes: path.join(root, 'src', 'types', 'seller-buyer', 'party.d.ts'),
};

const modules = [
  {
    key: 'seller',
    label: '卖家',
    pageKey: 'sellerPage',
    serviceKey: 'sellerService',
    serviceJsKey: 'sellerServiceJs',
    serviceImport: "@/services/seller/seller",
    idField: 'sellerId',
    noField: 'sellerNo',
    codeField: 'sellerCode',
    nameField: 'sellerName',
    accountIdField: 'sellerAccountId',
    searchStorageKey: 'admin-seller-management',
    forbiddenWords: ['BuyerPage', 'buyerConfig', 'AdminBuyer', 'buyer:admin'],
    forbiddenServicePath: /\/api\/buyer\/admin|\/api\/system\//,
    requiredServices: [
      'getAdminSellerList',
      'getAdminSeller',
      'addAdminSeller',
      'updateAdminSeller',
      'changeAdminSellerStatus',
      'getAdminSellerAccounts',
      'addAdminSellerAccount',
      'updateAdminSellerAccount',
      'lockAdminSellerAccount',
      'unlockAdminSellerAccount',
      'getAdminSellerDepts',
      'getAdminSellerDeptTree',
      'addAdminSellerDept',
      'updateAdminSellerDept',
      'removeAdminSellerDept',
      'getAdminSellerMenuTree',
      'getAdminSellerMenus',
      'getAdminSellerMenu',
      'addAdminSellerMenu',
      'updateAdminSellerMenu',
      'removeAdminSellerMenu',
      'getAdminSellerRoleMenuTree',
      'getAdminSellerRoles',
      'getAdminSellerRole',
      'addAdminSellerRole',
      'updateAdminSellerRole',
      'changeAdminSellerRoleStatus',
      'removeAdminSellerRoles',
      'resetAdminSellerAccountDefaultPassword',
      'forceLogoutAdminSellerSessions',
      'getAdminSellerSessions',
      'forceLogoutAdminSellerAccountSessions',
      'getAdminSellerAccountSessions',
      'getAdminSellerAccountRoles',
      'assignAdminSellerAccountRoles',
      'resetAdminSellerOwnerPassword',
      'createAdminSellerDirectLogin',
      'createAdminSellerAccountDirectLogin',
      'getAdminSellerLoginLogs',
      'getAdminSellerOperLogs',
      'getAdminSellerDirectLoginTickets',
    ],
    requiredServiceUrls: [
      '/api/seller/admin/sellers/list',
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/accounts`,
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/accounts/${templatePlaceholder('sellerAccountId')}/lock`,
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/accounts/${templatePlaceholder('sellerAccountId')}/unlock`,
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/accounts/${templatePlaceholder('sellerAccountId')}/roles`,
      '/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/resetPwd',
      '/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/resetDefaultPwd',
      '/api/seller/admin/menus/list',
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/roles/list`,
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/depts/list`,
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/sessions/list`,
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/accounts/${templatePlaceholder('sellerAccountId')}/sessions/list`,
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/directLogin`,
      `/api/seller/admin/sellers/${templatePlaceholder('sellerId')}/accounts/${templatePlaceholder('sellerAccountId')}/directLogin`,
      '/api/seller/admin/sellers/loginLogs/list',
      '/api/seller/admin/sellers/operLogs/list',
      '/api/seller/admin/sellers/directLoginTickets/list',
    ],
  },
  {
    key: 'buyer',
    label: '买家',
    pageKey: 'buyerPage',
    serviceKey: 'buyerService',
    serviceJsKey: 'buyerServiceJs',
    serviceImport: "@/services/buyer/buyer",
    idField: 'buyerId',
    noField: 'buyerNo',
    codeField: 'buyerCode',
    nameField: 'buyerName',
    accountIdField: 'buyerAccountId',
    searchStorageKey: 'admin-buyer-management',
    forbiddenWords: ['SellerPage', 'sellerConfig', 'AdminSeller', 'seller:admin'],
    forbiddenServicePath: /\/api\/seller\/admin|\/api\/system\//,
    requiredServices: [
      'getAdminBuyerList',
      'getAdminBuyer',
      'addAdminBuyer',
      'updateAdminBuyer',
      'changeAdminBuyerStatus',
      'getAdminBuyerAccounts',
      'addAdminBuyerAccount',
      'updateAdminBuyerAccount',
      'lockAdminBuyerAccount',
      'unlockAdminBuyerAccount',
      'getAdminBuyerDepts',
      'getAdminBuyerDeptTree',
      'addAdminBuyerDept',
      'updateAdminBuyerDept',
      'removeAdminBuyerDept',
      'getAdminBuyerMenuTree',
      'getAdminBuyerMenus',
      'getAdminBuyerMenu',
      'addAdminBuyerMenu',
      'updateAdminBuyerMenu',
      'removeAdminBuyerMenu',
      'getAdminBuyerRoleMenuTree',
      'getAdminBuyerRoles',
      'getAdminBuyerRole',
      'addAdminBuyerRole',
      'updateAdminBuyerRole',
      'changeAdminBuyerRoleStatus',
      'removeAdminBuyerRoles',
      'resetAdminBuyerAccountDefaultPassword',
      'forceLogoutAdminBuyerSessions',
      'getAdminBuyerSessions',
      'forceLogoutAdminBuyerAccountSessions',
      'getAdminBuyerAccountSessions',
      'getAdminBuyerAccountRoles',
      'assignAdminBuyerAccountRoles',
      'resetAdminBuyerOwnerPassword',
      'createAdminBuyerDirectLogin',
      'createAdminBuyerAccountDirectLogin',
      'getAdminBuyerLoginLogs',
      'getAdminBuyerOperLogs',
      'getAdminBuyerDirectLoginTickets',
    ],
    requiredServiceUrls: [
      '/api/buyer/admin/buyers/list',
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/accounts`,
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/accounts/${templatePlaceholder('buyerAccountId')}/lock`,
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/accounts/${templatePlaceholder('buyerAccountId')}/unlock`,
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/accounts/${templatePlaceholder('buyerAccountId')}/roles`,
      '/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/resetPwd',
      '/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/resetDefaultPwd',
      '/api/buyer/admin/menus/list',
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/roles/list`,
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/depts/list`,
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/sessions/list`,
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/accounts/${templatePlaceholder('buyerAccountId')}/sessions/list`,
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/directLogin`,
      `/api/buyer/admin/buyers/${templatePlaceholder('buyerId')}/accounts/${templatePlaceholder('buyerAccountId')}/directLogin`,
      '/api/buyer/admin/buyers/loginLogs/list',
      '/api/buyer/admin/buyers/operLogs/list',
      '/api/buyer/admin/buyers/directLoginTickets/list',
    ],
  },
];

const violations = [];

function templatePlaceholder(name) {
  return `$${`{${name}}`}`;
}

function toRelative(file) {
  return path.relative(root, file).replaceAll(path.sep, '/');
}

function capitalize(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function readRequired(file) {
  if (!fs.existsSync(file)) {
    violations.push(`${toRelative(file)} is missing`);
    return '';
  }
  return fs.readFileSync(file, 'utf8');
}

function readOptional(file) {
  return fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';
}

function assertIncludes(source, relativePath, expected, message) {
  if (!source.includes(expected)) {
    violations.push(`${relativePath} ${message}`);
  }
}

function assertNotIncludes(source, relativePath, forbidden, message) {
  if (source.includes(forbidden)) {
    violations.push(`${relativePath} ${message}`);
  }
}

function assertNoPattern(source, relativePath, pattern, message) {
  if (pattern.test(source)) {
    violations.push(`${relativePath} ${message}`);
  }
}

function extractConfigBlock(source, moduleKey) {
  const marker = `const ${moduleKey}Config: PartnerModuleConfig = {`;
  const start = source.indexOf(marker);
  if (start < 0) {
    return '';
  }
  const openBrace = source.indexOf('{', start);
  if (openBrace < 0) {
    return '';
  }
  let depth = 0;
  for (let index = openBrace; index < source.length; index += 1) {
    const char = source[index];
    if (char === '{') {
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0) {
        return source.slice(start, index + 1);
      }
    }
  }
  return '';
}

function extractInterfaceBlock(source, interfaceName) {
  const marker = `export interface ${interfaceName}`;
  const start = source.indexOf(marker);
  if (start < 0) {
    return '';
  }
  const openBrace = source.indexOf('{', start);
  if (openBrace < 0) {
    return '';
  }
  let depth = 0;
  for (let index = openBrace; index < source.length; index += 1) {
    const char = source[index];
    if (char === '{') {
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0) {
        return source.slice(start, index + 1);
      }
    }
  }
  return '';
}

function checkPage(module) {
  const file = files[module.pageKey];
  const source = readRequired(file);
  const relativePath = toRelative(file);
  if (!source) {
    return;
  }

  assertIncludes(
    source,
    relativePath,
    "import PartnerManagementPage, { type PartnerModuleConfig } from '@/components/PartnerManagement/PartnerManagementPage';",
    'must use the shared PartnerManagementPage template',
  );
  assertIncludes(
    source,
    relativePath,
    `from '${module.serviceImport}'`,
    `must import only ${module.key} admin services`,
  );
  assertNoPattern(
    source,
    relativePath,
    /\brequest\s*(?:<[^;]+?>)?\s*\(/,
    'must not call request(...) directly',
  );
  const forbiddenServiceImport =
    module.key === 'seller'
      ? /from\s+['"]@\/services\/(?:buyer|system)\//
      : /from\s+['"]@\/services\/(?:seller|system)\//;
  assertNoPattern(
    source,
    relativePath,
    forbiddenServiceImport,
    'must not import cross-terminal or system services',
  );
  for (const forbidden of module.forbiddenWords) {
    assertNotIncludes(
      source,
      relativePath,
      forbidden,
      `must not reference cross-terminal token ${forbidden}`,
    );
  }

  const configBlock = extractConfigBlock(source, module.key);
  if (!configBlock) {
    violations.push(`${relativePath} must define ${module.key}Config`);
    return;
  }
  for (const [fieldName, value] of [
    ['moduleKey', module.key],
    ['label', module.label],
    ['idField', module.idField],
    ['noField', module.noField],
    ['codeField', module.codeField],
    ['nameField', module.nameField],
    ['ownerIdField', module.idField],
    ['accountIdField', module.accountIdField],
    ['listTemplate', 'standard'],
    ['searchStorageKey', module.searchStorageKey],
  ]) {
    assertIncludes(
      configBlock,
      relativePath,
      `${fieldName}: '${value}'`,
      `must configure ${fieldName}: '${value}'`,
    );
  }

  for (const permission of [
    'list',
    'add',
    'edit',
    'resetPwd',
    'roleQuery',
    'roleEdit',
  ]) {
    const permissionValue = `${module.key}:admin:account:${permission
      .replace('roleQuery', 'role:query')
      .replace('roleEdit', 'role:edit')}`;
    assertIncludes(
      configBlock,
      relativePath,
      `${permission}: '${permissionValue}'`,
      `must configure account permission ${permissionValue}`,
    );
  }
  assertIncludes(
    configBlock,
    relativePath,
    `lock: '${module.key}:admin:account:lock'`,
    `must configure ${module.key} account lock permission ${module.key}:admin:account:lock`,
  );
  assertIncludes(
    configBlock,
    relativePath,
    `lockAccount: lockAdmin${capitalize(module.key)}Account`,
    `must wire ${module.key} lockAccount service`,
  );
  assertIncludes(
    configBlock,
    relativePath,
    `unlockAccount: unlockAdmin${capitalize(module.key)}Account`,
    `must wire ${module.key} unlockAccount service`,
  );

  for (const serviceName of module.requiredServices) {
    assertIncludes(
      source,
      relativePath,
      serviceName,
      `must wire ${serviceName} into the ${module.key} management template`,
    );
  }
}

function checkServiceSource(module, source, relativePath) {
  if (!source) {
    return;
  }

  for (const serviceName of module.requiredServices) {
    assertIncludes(
      source,
      relativePath,
      `function ${serviceName}`,
      `must export ${serviceName}`,
    );
  }
  for (const url of module.requiredServiceUrls) {
    assertIncludes(source, relativePath, url, `must call ${url}`);
  }
  assertNoPattern(
    source,
    relativePath,
    module.forbiddenServicePath,
    'must not call cross-terminal or system admin APIs',
  );
  assertNoPattern(
    source,
    relativePath,
    /\buser_id\b|\bsys_user\b|\bsys_role\b|\bsys_menu\b|\bsys_dept\b/i,
    'must not reference sys_* terminal account fields',
  );
}

function checkService(module) {
  const file = files[module.serviceKey];
  const source = readRequired(file);
  checkServiceSource(module, source, toRelative(file));

  const jsFile = files[module.serviceJsKey];
  const jsSource = readOptional(jsFile);
  if (jsSource) {
    checkServiceSource(module, jsSource, toRelative(jsFile));
  }
}

function checkPartnerTypes() {
  const source = readRequired(files.partnerTypes);
  const relativePath = toRelative(files.partnerTypes);
  if (!source) {
    return;
  }

  const accountBaseBlock = extractInterfaceBlock(source, 'PortalAccountBase');
  if (!accountBaseBlock) {
    violations.push(`${relativePath} must define PortalAccountBase`);
  } else {
    assertNoPattern(
      accountBaseBlock,
      relativePath,
      /\bpassword\s*\?/,
      'PortalAccountBase must not include password in account response types',
    );
  }

  const accountPayloadBlock = extractInterfaceBlock(source, 'PortalAccountPayload');
  if (!accountPayloadBlock) {
    violations.push(`${relativePath} must define PortalAccountPayload for account write requests`);
  } else {
    assertIncludes(
      accountPayloadBlock,
      relativePath,
      'password?: string',
      'PortalAccountPayload must keep password only for account write requests',
    );
  }

  const directLoginBlock = extractInterfaceBlock(source, 'DirectLoginResult');
  if (!directLoginBlock) {
    violations.push(`${relativePath} must define DirectLoginResult`);
  } else {
    assertNoPattern(
      directLoginBlock,
      relativePath,
      /\btoken\s*[:?]/,
      'DirectLoginResult must not expose direct-login token',
    );
  }
}

function checkAccountModalFailSoft(source, relativePath) {
  if (!source) {
    return;
  }

  assertIncludes(
    source,
    relativePath,
    'const loadDeptTree = async () =>',
    'must load dept tree in an isolated fail-soft path',
  );
  assertIncludes(
    source,
    relativePath,
    'const accountResp = await config.services.getAccounts(partnerId)',
    'must load account list independently from dept tree',
  );
  assertIncludes(
    source,
    relativePath,
    'void loadDeptTree();',
    'must trigger dept tree load without blocking account list load',
  );
  assertNoPattern(
    source,
    relativePath,
    /Promise\.all\(\s*\[\s*config\.services\.getAccounts\(partnerId\),\s*config\.services\.getDeptTree\(partnerId\)/s,
    'must not couple account list and dept tree requests in one Promise.all',
  );
}

function checkAccountModalSource(source, relativePath) {
  if (!source) {
    return;
  }
  for (const expected of [
    'config.services.getAccounts',
    'config.services.getDeptTree',
    'config.services.addAccount',
    'config.services.updateAccount',
    'config.services.resetAccountDefaultPassword',
    'config.services.lockAccount',
    'config.services.unlockAccount',
    'config.services.forceLogoutAccount',
    'config.services.directLoginAccount',
    'PartnerAccountRoleModal',
    'PartnerSessionModal',
    'accountPermissions',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep account modal support for ${expected}`,
    );
  }
  assertNoPattern(
    source,
    relativePath,
    /\/api\/(?:seller|buyer|system)\//,
    'must not hardcode API paths inside account modal',
  );
  checkAccountModalFailSoft(source, relativePath);
}

function checkPageTemplateSource(source, relativePath, requireTypeExport) {
  if (!source) {
    return;
  }
  const expectedItems = [
    'accountPermissions',
    'getPersistedProTableSearch',
    'getProTableScroll',
    'PartnerAccountModal',
    'PartnerDeptModal',
    'PartnerRoleModal',
    'PartnerMenuModal',
    'PartnerSessionModal',
    'PartnerAuditModal',
    'listTemplate ===',
    'searchStorageKey',
    'config.services.directLogin',
    'config.services.forceLogoutSubject',
  ];
  if (requireTypeExport) {
    expectedItems.unshift('export type PartnerModuleConfig');
  }

  for (const expected of expectedItems) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep shared template support for ${expected}`,
    );
  }
  assertNoPattern(
    source,
    relativePath,
    /\/api\/(?:seller|buyer|system)\//,
    'must not hardcode API paths inside the shared page template',
  );
}

function checkSessionModalSource(source, relativePath) {
  if (!source) {
    return;
  }
  for (const expected of [
    'config.services.listAccountSessions?.(partnerId, accountId, { pageNum, pageSize })',
    'config.services.listSubjectSessions?.(partnerId, { pageNum, pageSize })',
    'record.terminal || config.moduleKey',
    'record.subjectId || 0',
    'record.accountId || 0',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep scoped session modal behavior for ${expected}`,
    );
  }
  assertNoPattern(
    source,
    relativePath,
    /\/api\/(?:seller|buyer|system)\//,
    'must not hardcode API paths inside session modal',
  );
}

function checkSharedTemplate() {
  const pageTemplateSource = readRequired(files.pageTemplate);
  const pageTemplateJsSource = readOptional(files.pageTemplateJs);
  const accountModalSource = readRequired(files.accountModal);
  const accountModalJsSource = readOptional(files.accountModalJs);
  const sessionModalSource = readRequired(files.sessionModal);
  const sessionModalJsSource = readOptional(files.sessionModalJs);
  const auditModalSource = readRequired(files.auditModal);

  checkPageTemplateSource(pageTemplateSource, toRelative(files.pageTemplate), true);
  checkPageTemplateSource(pageTemplateJsSource, toRelative(files.pageTemplateJs), false);

  checkAccountModalSource(accountModalSource, toRelative(files.accountModal));
  checkAccountModalSource(accountModalJsSource, toRelative(files.accountModalJs));

  checkSessionModalSource(sessionModalSource, toRelative(files.sessionModal));
  checkSessionModalSource(sessionModalJsSource, toRelative(files.sessionModalJs));

  if (auditModalSource) {
    const relativePath = toRelative(files.auditModal);
    for (const expected of [
      'config.services.listLoginLogs',
      'config.services.listOperLogs',
      'config.services.listDirectLoginTickets',
      'getPersistedProTableSearch',
      "label: '登录日志'",
      "label: '操作日志'",
      "label: '免密票据'",
    ]) {
      assertIncludes(
        auditModalSource,
        relativePath,
        expected,
        `must keep audit modal support for ${expected}`,
      );
    }
    assertNoPattern(
      auditModalSource,
      relativePath,
      /\btokenHash\b|\bdirectLoginToken\b|\bloginUrl\b/,
      'must not render direct-login sensitive token fields',
    );
  }
}

for (const module of modules) {
  checkPage(module);
  checkService(module);
}
checkPartnerTypes();
checkSharedTemplate();

if (violations.length > 0) {
  console.error('Partner management template guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Partner management template guard passed.');
