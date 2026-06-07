import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();

const files = {
  sellerPage: path.join(root, 'src', 'pages', 'Seller', 'index.tsx'),
  sellerPageJs: path.join(root, 'src', 'pages', 'Seller', 'index.js'),
  buyerPage: path.join(root, 'src', 'pages', 'Buyer', 'index.tsx'),
  buyerPageJs: path.join(root, 'src', 'pages', 'Buyer', 'index.js'),
  sellerService: path.join(root, 'src', 'services', 'seller', 'seller.ts'),
  sellerServiceJs: path.join(root, 'src', 'services', 'seller', 'seller.js'),
  buyerService: path.join(root, 'src', 'services', 'buyer', 'buyer.ts'),
  buyerServiceJs: path.join(root, 'src', 'services', 'buyer', 'buyer.js'),
  routes: path.join(root, 'config', 'routes.ts'),
  routesJs: path.join(root, 'config', 'routes.js'),
  sessionService: path.join(root, 'src', 'services', 'session.ts'),
  sessionServiceJs: path.join(root, 'src', 'services', 'session.js'),
  routeGuardWrapper: path.join(root, 'src', 'wrappers', 'RemoteMenuRouteGuard.tsx'),
  routeGuardWrapperJs: path.join(root, 'src', 'wrappers', 'RemoteMenuRouteGuard.js'),
  remoteMenuStorage: path.join(root, 'src', 'utils', 'remoteMenuStorage.ts'),
  remoteMenuStorageJs: path.join(root, 'src', 'utils', 'remoteMenuStorage.js'),
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
  accountRoleModal: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerAccountRoleModal.tsx',
  ),
  accountRoleModalJs: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerAccountRoleModal.js',
  ),
  deptModal: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerDeptModal.tsx',
  ),
  deptModalJs: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerDeptModal.js',
  ),
  roleModal: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerRoleModal.tsx',
  ),
  roleModalJs: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerRoleModal.js',
  ),
  menuModal: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerMenuModal.tsx',
  ),
  menuModalJs: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerMenuModal.js',
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
  auditModalJs: path.join(
    root,
    'src',
    'components',
    'PartnerManagement',
    'PartnerAuditModal.js',
  ),
  partnerTypes: path.join(root, 'src', 'types', 'seller-buyer', 'party.d.ts'),
  sellerTypes: path.join(root, 'src', 'types', 'seller-buyer', 'seller.d.ts'),
  buyerTypes: path.join(root, 'src', 'types', 'seller-buyer', 'buyer.d.ts'),
};

const modules = [
  {
    key: 'seller',
    label: '卖家',
    pageKey: 'sellerPage',
    pageJsKey: 'sellerPageJs',
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
      'resetAdminSellerAccountPassword',
      'forceLogoutAdminSellerSessions',
      'getAdminSellerSessions',
      'forceLogoutAdminSellerAccountSessions',
      'getAdminSellerAccountSessions',
      'getAdminSellerAccountRoles',
      'assignAdminSellerAccountRoles',
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
    pageJsKey: 'buyerPageJs',
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
      'resetAdminBuyerAccountPassword',
      'forceLogoutAdminBuyerSessions',
      'getAdminBuyerSessions',
      'forceLogoutAdminBuyerAccountSessions',
      'getAdminBuyerAccountSessions',
      'getAdminBuyerAccountRoles',
      'assignAdminBuyerAccountRoles',
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

function partnerPath(module) {
  return module.key === 'seller' ? 'sellers' : 'buyers';
}

function buildExpectedServiceCalls(module) {
  const cap = capitalize(module.key);
  const subjectPath = partnerPath(module);
  const adminBase = `/api/${module.key}/admin`;
  const subjectBase = `${adminBase}/${subjectPath}`;
  const subjectId = templatePlaceholder(module.idField);
  const accountId = templatePlaceholder(module.accountIdField);
  const menuId = templatePlaceholder('menuId');
  const roleId = templatePlaceholder('roleId');
  const roleIds = templatePlaceholder("roleIds.join(',')");
  const deptId = templatePlaceholder('deptId');

  return {
    [`getAdmin${cap}List`]: `${subjectBase}/list`,
    [`getAdmin${cap}`]: `${subjectBase}/${subjectId}`,
    [`addAdmin${cap}`]: subjectBase,
    [`updateAdmin${cap}`]: subjectBase,
    [`changeAdmin${cap}Status`]: `${subjectBase}/changeStatus`,
    [`getAdmin${cap}Accounts`]: `${subjectBase}/${subjectId}/accounts`,
    [`addAdmin${cap}Account`]: `${subjectBase}/${subjectId}/accounts`,
    [`updateAdmin${cap}Account`]: `${subjectBase}/${subjectId}/accounts`,
    [`lockAdmin${cap}Account`]: `${subjectBase}/${subjectId}/accounts/${accountId}/lock`,
    [`unlockAdmin${cap}Account`]: `${subjectBase}/${subjectId}/accounts/${accountId}/unlock`,
    [`getAdmin${cap}AccountRoles`]: `${subjectBase}/${subjectId}/accounts/${accountId}/roles`,
    [`assignAdmin${cap}AccountRoles`]: `${subjectBase}/${subjectId}/accounts/${accountId}/roles`,
    [`getAdmin${cap}MenuTree`]: `${adminBase}/menus/treeselect`,
    [`getAdmin${cap}Menus`]: `${adminBase}/menus/list`,
    [`getAdmin${cap}Menu`]: `${adminBase}/menus/${menuId}`,
    [`addAdmin${cap}Menu`]: `${adminBase}/menus`,
    [`updateAdmin${cap}Menu`]: `${adminBase}/menus`,
    [`removeAdmin${cap}Menu`]: `${adminBase}/menus/${menuId}`,
    [`getAdmin${cap}RoleMenuTree`]: `${adminBase}/menus/roleMenuTreeselect/${subjectId}/${roleId}`,
    [`getAdmin${cap}Roles`]: `${subjectBase}/${subjectId}/roles/list`,
    [`getAdmin${cap}Role`]: `${subjectBase}/${subjectId}/roles/${roleId}`,
    [`addAdmin${cap}Role`]: `${subjectBase}/${subjectId}/roles`,
    [`updateAdmin${cap}Role`]: `${subjectBase}/${subjectId}/roles`,
    [`changeAdmin${cap}RoleStatus`]: `${subjectBase}/${subjectId}/roles/changeStatus`,
    [`removeAdmin${cap}Roles`]: `${subjectBase}/${subjectId}/roles/${roleIds}`,
    [`getAdmin${cap}Depts`]: `${subjectBase}/${subjectId}/depts/list`,
    [`getAdmin${cap}Dept`]: `${subjectBase}/${subjectId}/depts/${deptId}`,
    [`getAdmin${cap}DeptTree`]: `${subjectBase}/${subjectId}/depts/treeselect`,
    [`addAdmin${cap}Dept`]: `${subjectBase}/${subjectId}/depts`,
    [`updateAdmin${cap}Dept`]: `${subjectBase}/${subjectId}/depts`,
    [`removeAdmin${cap}Dept`]: `${subjectBase}/${subjectId}/depts/${deptId}`,
    [`resetAdmin${cap}AccountPassword`]: `${subjectBase}/${subjectId}/accounts/${accountId}/resetPwd`,
    [`forceLogoutAdmin${cap}Sessions`]: `${subjectBase}/${subjectId}/sessions`,
    [`getAdmin${cap}Sessions`]: `${subjectBase}/${subjectId}/sessions/list`,
    [`forceLogoutAdmin${cap}AccountSessions`]: `${subjectBase}/${subjectId}/accounts/${accountId}/sessions`,
    [`getAdmin${cap}AccountSessions`]: `${subjectBase}/${subjectId}/accounts/${accountId}/sessions/list`,
    [`createAdmin${cap}DirectLogin`]: `${subjectBase}/${subjectId}/directLogin`,
    [`createAdmin${cap}AccountDirectLogin`]: `${subjectBase}/${subjectId}/accounts/${accountId}/directLogin`,
    [`getAdmin${cap}LoginLogs`]: `${subjectBase}/loginLogs/list`,
    [`getAdmin${cap}OperLogs`]: `${subjectBase}/operLogs/list`,
    [`getAdmin${cap}DirectLoginTickets`]: `${subjectBase}/directLoginTickets/list`,
  };
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

function assertIncludesAny(source, relativePath, expectedValues, message) {
  if (!expectedValues.some((expected) => source.includes(expected))) {
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
  const fallbackMarker = `const ${moduleKey}Config = {`;
  let start = source.indexOf(marker);
  if (start < 0) {
    start = source.indexOf(fallbackMarker);
  }
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

function extractRouteBlock(source, routePath) {
  const marker = `path: '${routePath}',`;
  const pathIndex = source.indexOf(marker);
  if (pathIndex < 0) {
    return '';
  }
  const openBrace = source.lastIndexOf('{', pathIndex);
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
        return source.slice(openBrace, index + 1);
      }
    }
  }
  return '';
}

function extractExportedAsyncFunctionBlock(source, functionName) {
  const marker = `export async function ${functionName}(`;
  const start = source.indexOf(marker);
  if (start < 0) {
    return '';
  }
  const nextStart = source.indexOf('\nexport async function ', start + marker.length);
  return source.slice(start, nextStart < 0 ? source.length : nextStart);
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
  assertNotIncludes(
    source,
    relativePath,
    `resetAdmin${capitalize(module.key)}OwnerPassword`,
    'must not import subject-level owner password reset service',
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
  assertNotIncludes(
    configBlock,
    relativePath,
    'resetOwnerPassword',
    'must not wire subject-level owner password reset',
  );

  for (const serviceName of module.requiredServices) {
    assertIncludes(
      source,
      relativePath,
      serviceName,
      `must wire ${serviceName} into the ${module.key} management template`,
    );
  }

  const jsFile = files[module.pageJsKey];
  const jsSource = readOptional(jsFile);
  if (jsSource) {
    const jsRelativePath = toRelative(jsFile);
    assertIncludes(
      jsSource,
      jsRelativePath,
      "import PartnerManagementPage from '@/components/PartnerManagement/PartnerManagementPage';",
      'must use the shared PartnerManagementPage template',
    );
    assertIncludes(
      jsSource,
      jsRelativePath,
      `from '${module.serviceImport}'`,
      `must import only ${module.key} admin services`,
    );
    assertNotIncludes(
      jsSource,
      jsRelativePath,
      `resetAdmin${capitalize(module.key)}OwnerPassword`,
      'must not import subject-level owner password reset service',
    );
    assertNoPattern(
      jsSource,
      jsRelativePath,
      /\brequest\s*(?:<[^;]+?>)?\s*\(/,
      'must not call request(...) directly',
    );
    assertNoPattern(
      jsSource,
      jsRelativePath,
      forbiddenServiceImport,
      'must not import cross-terminal or system services',
    );
    for (const forbidden of module.forbiddenWords) {
      assertNotIncludes(
        jsSource,
        jsRelativePath,
        forbidden,
        `must not reference cross-terminal token ${forbidden}`,
      );
    }
    const jsConfigBlock = extractConfigBlock(jsSource, module.key);
    if (!jsConfigBlock) {
      violations.push(`${jsRelativePath} must define ${module.key}Config`);
    } else {
      for (const [fieldName, value] of [
        ['moduleKey', module.key],
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
          jsConfigBlock,
          jsRelativePath,
          `${fieldName}: '${value}'`,
          `must configure ${fieldName}: '${value}'`,
        );
      }
      assertNotIncludes(
        jsConfigBlock,
        jsRelativePath,
        'resetOwnerPassword',
        'must not wire subject-level owner password reset',
      );
    }
  }
}

function checkServiceSource(module, source, relativePath) {
  if (!source) {
    return;
  }

  const expectedServiceCalls = buildExpectedServiceCalls(module);
  for (const serviceName of module.requiredServices) {
    assertIncludes(
      source,
      relativePath,
      `function ${serviceName}`,
      `must export ${serviceName}`,
    );
    const expectedUrl = expectedServiceCalls[serviceName];
    if (!expectedUrl) {
      violations.push(`${relativePath} must define a function-level URL contract for ${serviceName}`);
      continue;
    }
    const functionBlock = extractExportedAsyncFunctionBlock(source, serviceName);
    if (functionBlock) {
      assertIncludes(
        functionBlock,
        relativePath,
        expectedUrl,
        `must call ${expectedUrl} inside ${serviceName}`,
      );
    }
  }
  for (const url of module.requiredServiceUrls) {
    assertIncludes(source, relativePath, url, `must call ${url}`);
  }
  assertNotIncludes(
    source,
    relativePath,
    `resetAdmin${capitalize(module.key)}OwnerPassword`,
    'must not export subject-level owner password reset service',
  );
  assertNotIncludes(
    source,
    relativePath,
    'resetOwnerPwd',
    'must not call subject-level owner password reset API',
  );
  assertNotIncludes(
    source,
    relativePath,
    `resetAdmin${capitalize(module.key)}AccountDefaultPassword`,
    'must not export account default password reset service',
  );
  assertNotIncludes(
    source,
    relativePath,
    'resetDefaultPwd',
    'must not call account default password reset API',
  );
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
    assertIncludes(
      directLoginBlock,
      relativePath,
      'token: string',
      'DirectLoginResult must expose direct-login token for postMessage delivery',
    );
  }

  const sessionProfileBlock = extractInterfaceBlock(source, 'PortalSessionProfile');
  if (!sessionProfileBlock) {
    violations.push(`${relativePath} must define PortalSessionProfile`);
  } else {
    for (const expected of [
      'directLogin?: boolean',
      'directLoginTicketId?: number',
      'actingAdminId?: number',
      'actingAdminName?: string',
      'directLoginReason?: string',
    ]) {
      assertIncludes(
        sessionProfileBlock,
        relativePath,
        expected,
        `PortalSessionProfile must expose admin session audit field ${expected}`,
      );
    }
  }

  const loginLogBlock = extractInterfaceBlock(source, 'PortalLoginLog');
  if (!loginLogBlock) {
    violations.push(`${relativePath} must define PortalLoginLog`);
  } else {
    for (const expected of [
      'directLogin?: boolean',
      'directLoginTicketId?: number',
      'actingAdminId?: number',
      'actingAdminName?: string',
      'directLoginReason?: string',
    ]) {
      assertIncludes(
        loginLogBlock,
        relativePath,
        expected,
        `PortalLoginLog must expose direct-login audit field ${expected}`,
      );
    }
  }
}

function checkPartnerReadTypes() {
  const sensitiveResponseFields = [
    'password',
    'token',
    'refreshToken',
    'directLoginToken',
    'loginUrl',
    'tokenHash',
    'authorization',
    'accessToken',
  ];
  const readTypeGroups = [
    {
      file: files.sellerTypes,
      interfaces: [
        'Seller',
        'SellerPageResult',
        'SellerInfoResult',
        'SellerAccount',
        'SellerAccountListResult',
      ],
    },
    {
      file: files.buyerTypes,
      interfaces: [
        'Buyer',
        'BuyerPageResult',
        'BuyerInfoResult',
        'BuyerAccount',
        'BuyerAccountListResult',
      ],
    },
  ];

  for (const group of readTypeGroups) {
    const source = readRequired(group.file);
    const relativePath = toRelative(group.file);
    if (!source) {
      continue;
    }
    for (const interfaceName of group.interfaces) {
      const block = extractInterfaceBlock(source, interfaceName);
      if (!block) {
        violations.push(`${relativePath} must define ${interfaceName}`);
        continue;
      }
      for (const fieldName of sensitiveResponseFields) {
        const fieldPattern = new RegExp(`\\b${fieldName}\\s*\\??\\s*:`, 'i');
        if (fieldPattern.test(block)) {
          violations.push(
            `${relativePath} ${interfaceName} must not expose sensitive response field ${fieldName}`,
          );
        }
      }
    }
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
    'const canQueryDept = access.hasPerms(`${permPrefix}:dept:query`)',
    'must derive dept tree query permission before loading account dept tree',
  );
  assertIncludesAny(source, relativePath, ['disabled={!canQueryDept}', 'disabled: !canQueryDept'],
    'must disable account department field when dept:query is missing');
  assertIncludesAny(source, relativePath, ["placeholder={canQueryDept ? '请选择' : '无部门查询权限'}", "placeholder: canQueryDept ? '\\u8BF7\\u9009\\u62E9' : '\\u65E0\\u90E8\\u95E8\\u67E5\\u8BE2\\u6743\\u9650'"],
    'must explain account department field when dept:query is missing');
  assertIncludes(
    source,
    relativePath,
    'if (!partnerId || !canQueryDept)',
    'must not request account dept tree without dept query permission',
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
    'config.services.resetAccountPassword',
    'resetPasswordForm.validateFields()',
    'Input.Password',
    'config.services.lockAccount',
    'config.services.unlockAccount',
    'config.services.forceLogoutAccount',
    'config.services.directLoginAccount',
    'PartnerAccountRoleModal',
    'PartnerSessionModal',
    'PartnerAuditModal',
    'accountPermissions',
    'access.hasPerms(`${permPrefix}:session:list`)',
    'const canQueryRole = access.hasPerms(`${permPrefix}:role:query`)',
    'const canAssignAccountRoles = canQueryRole',
    'const canViewAccountAudit = access.hasPerms(`${permPrefix}:loginLog:list`)',
    '|| access.hasPerms(`${permPrefix}:operLog:list`)',
    '|| access.hasPerms(`${permPrefix}:ticket:list`)',
    'setAuditAccount(record)',
    'setAuditModalOpen(true)',
    'normalizePassword',
    'passwordRules',
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
  assertIncludesAny(source, relativePath, ["key: 'audit', label: '审计'", "key: 'audit', label: '\\u5BA1\\u8BA1'"],
    'must expose account row audit action from the More menu');
  assertIncludesAny(source, relativePath, ['account={auditAccount}', 'account: auditAccount'],
    'must pass the selected account into PartnerAuditModal');
  assertNotIncludes(
    source,
    relativePath,
    'DEFAULT_ACCOUNT_PASSWORD',
    'must not keep a default account password fallback',
  );
  assertNotIncludes(
    source,
    relativePath,
    'U12346',
    'must not hardcode the old default account password',
  );
  assertNoPattern(
    source,
    relativePath,
    /values\.password\s*\|\|/,
    'must not fall back when account password is blank',
  );
  checkAccountModalFailSoft(source, relativePath);
}

function checkAccountRoleModalSource(source, relativePath) {
  if (!source) {
    return;
  }
  for (const expected of [
    'config.services.getAccountRoles(partnerId, accountId)',
    'config.services.assignAccountRoles(partnerId, accountId, selectedRoleIds)',
    'normalizeRoleIds',
    'account[config.accountIdField] || account.accountId',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep account role modal scoped behavior for ${expected}`,
    );
  }
  assertNoPattern(
    source,
    relativePath,
    /\/api\/(?:seller|buyer|system)\//,
    'must not hardcode API paths inside account role modal',
  );
}

function checkDeptModalSource(source, relativePath) {
  if (!source) {
    return;
  }
  for (const expected of [
    'const permPrefix = `${config.moduleKey}:admin`',
    'config.services.listDepts(partnerId)',
    'config.services.getDeptTree(partnerId)',
    'const loadDeptTree = async () =>',
    'const listResp = await config.services.listDepts(partnerId)',
    'void loadDeptTree();',
    'config.services.updateDept(partnerId, payload)',
    'config.services.addDept(partnerId, payload)',
    'config.services.removeDept(partnerId, dept.deptId',
    'const canQueryDeptTree = access.hasPerms(`${permPrefix}:dept:query`)',
    'const canAddDept = access.hasPerms(`${permPrefix}:dept:add`) && canQueryDeptTree',
    'const canEditDept = access.hasPerms(`${permPrefix}:dept:edit`) && canQueryDeptTree',
    'access.hasPerms(`${permPrefix}:dept:remove`)',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep department modal terminal-scoped behavior for ${expected}`,
    );
  }
  assertIncludesAny(source, relativePath, ['hidden={!canAddDept}', 'hidden: !canAddDept'],
    'must gate department add by canAddDept');
  assertIncludesAny(source, relativePath, ['hidden={!canEditDept}', 'hidden: !canEditDept'],
    'must gate department edit by canEditDept');
  assertNoPattern(
    source,
    relativePath,
    /Promise\.all\(\s*\[\s*config\.services\.listDepts\(partnerId\),\s*(?:canQueryDeptTree\s*\?\s*)?config\.services\.getDeptTree\(partnerId\)/s,
    'must not couple department list and tree requests in one Promise.all',
  );
  assertNoPattern(
    source,
    relativePath,
    /\/api\/(?:seller|buyer|system)\//,
    'must not hardcode API paths inside department modal',
  );
}

function checkRoleModalSource(source, relativePath) {
  if (!source) {
    return;
  }
  for (const expected of [
    'const permPrefix = `${config.moduleKey}:admin`',
    'config.services.listRoles(partnerId)',
    'config.services.getRole(partnerId, role.roleId)',
    'config.services.getRoleMenuTree(partnerId, role.roleId)',
    'config.services.getMenuTree()',
    'config.services.updateRole(partnerId, payload)',
    'config.services.addRole(partnerId, payload)',
    'config.services.changeRoleStatus(partnerId',
    'config.services.removeRoles(partnerId, [role.roleId',
    'const canQueryRole = access.hasPerms(`${permPrefix}:role:query`)',
    'const canQueryMenu = access.hasPerms(`${permPrefix}:menu:query`)',
    'const canAddRole = access.hasPerms(`${permPrefix}:role:add`) && canQueryMenu',
    'const canChangeRoleStatus = access.hasPerms(`${permPrefix}:role:edit`)',
    'const canEditRoleForm = canChangeRoleStatus && canQueryRole && canQueryMenu',
    'access.hasPerms(`${permPrefix}:role:remove`)',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep role modal terminal-scoped behavior for ${expected}`,
    );
  }
  assertIncludesAny(source, relativePath, ['hidden={!canAddRole}', 'hidden: !canAddRole'],
    'must gate role add by canAddRole');
  assertIncludesAny(source, relativePath, ['hidden={!canEditRoleForm}', 'hidden: !canEditRoleForm'],
    'must gate role edit form by canEditRoleForm');
  assertNoPattern(
    source,
    relativePath,
    /\/api\/(?:seller|buyer|system)\//,
    'must not hardcode API paths inside role modal',
  );
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
    'searchFieldCount',
    'openPortalDirectLoginWindow',
    'config.services.directLogin',
    'config.services.forceLogoutSubject',
    'access.hasPerms(`${permPrefix}:session:list`)',
    'const canEditPartner = access.hasPerms(`${permPrefix}:edit`) && access.hasPerms(`${permPrefix}:query`)',
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
  assertIncludes(
    source,
    relativePath,
    'fieldCount: config.searchFieldCount',
    'must pass configured fieldCount into getPersistedProTableSearch',
  );
  assertIncludesAny(source, relativePath, ['hidden={!canEditPartner}', 'hidden: !canEditPartner'],
    'must gate partner edit by canEditPartner');
  for (const expected of [
    'list: `${permPrefix}:account:list`',
    'add: `${permPrefix}:account:add`',
    'edit: `${permPrefix}:account:edit`',
    'resetPwd: `${permPrefix}:account:resetPwd`',
    'roleQuery: `${permPrefix}:account:role:query`',
    'roleEdit: `${permPrefix}:account:role:edit`',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep account permission fallback ${expected}`,
    );
  }
}

function checkSessionModalSource(source, relativePath) {
  if (!source) {
    return;
  }
  for (const expected of [
    'config.services.listAccountSessions?.(partnerId, accountId, { pageNum, pageSize })',
    'config.services.listSubjectSessions?.(partnerId, { pageNum, pageSize })',
    'record.tokenId ||',
    'record.terminal || config.moduleKey',
    'record.subjectId || 0',
    'record.accountId || 0',
    'renderDirectLoginAudit',
    'record.directLogin',
    'record.actingAdminName',
    'record.directLoginReason',
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

function checkMenuModalSource(source, relativePath) {
  if (!source) {
    return;
  }
  if (source.includes("from './PartnerMenuModal.tsx'")) {
    return;
  }
  for (const expected of [
    'validateMenuPathForTerminal',
    'validateMenuComponentForTerminal',
    'validateMenuPermsForTerminal',
    'function normalizeMenuTarget',
    "replace(/^(?:\\.\\/|\\/)+/, '')",
    "const normalizedLower = normalized.toLowerCase()",
    'menu component is required for page menus',
    'menu component must use the current terminal root',
    'const permissions = normalized.split',
    'menu permission is required for page and button menus',
    'terminal menu cannot use wildcard permissions',
    'permission.startsWith(`${moduleKey}:`)',
    'forbiddenPathRoots',
    'forbiddenComponentRoots',
    "forbiddenComponentRoots = new Set(['admin', 'common', 'shared', 'system', 'user', 'monitor', 'tool'])",
    'permission.startsWith(`${moduleKey}:admin:`)',
    'const canEditMenu = access.hasPerms(`${permPrefix}:menu:edit`) && access.hasPerms(`${permPrefix}:menu:query`)',
    'hidden={!canEditMenu}',
    'permissions.some((permission) => permission === `${moduleKey}:admin` || permission.startsWith(`${moduleKey}:admin:`))',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep terminal fail-closed menu validation for ${expected}`,
    );
  }
}

function checkAuditModalSource(source, relativePath, requireTabLabels) {
  if (!source) {
    return;
  }
  for (const expected of [
    'config.services.listLoginLogs',
    'config.services.listOperLogs',
    'config.services.listDirectLoginTickets',
    'getPersistedProTableSearch',
    'next[accountField] = accountId',
    'request(buildAuditParams(rest, current, pageSize, partnerId, accountId, subjectField, accountField))',
    "dataIndex: 'accountId'",
    'render: (_, record) => renderCompactText(record.accountId)',
    "dataIndex: 'targetAccountId'",
    'render: (_, record) => renderCompactText(record.targetAccountId)',
    "dataIndex: 'actingAdminName'",
    'render: (_, record) => renderCompactText(record.actingAdminName)',
    'renderDetailText(record.actingAdminId)',
    'renderDetailText(record.reason)',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep audit modal support for ${expected}`,
    );
  }
  if (requireTabLabels) {
    for (const expected of [
      "label: '登录日志'",
      "label: '操作日志'",
      "label: '免密票据'",
    ]) {
      assertIncludes(
        source,
        relativePath,
        expected,
        `must keep audit modal support for ${expected}`,
      );
    }
  }
  assertNoPattern(
    source,
    relativePath,
    /\btokenHash\b|\bdirectLoginToken\b|\bloginUrl\b/,
    'must not render direct-login sensitive token fields',
  );
  assertIncludesAny(source, relativePath, ['暂无审计权限', '\\u6682\\u65E0\\u5BA1\\u8BA1\\u6743\\u9650'],
    'must keep deny-by-default no audit permission text');
  assertNoPattern(
    source,
    relativePath,
    /renderDetailText\(record\.(?:operParam|jsonResult)\)/,
    'must not render raw operation request/response payloads in audit modal',
  );
}

function checkSharedTemplate() {
  const pageTemplateSource = readRequired(files.pageTemplate);
  const pageTemplateJsSource = readOptional(files.pageTemplateJs);
  const accountModalSource = readRequired(files.accountModal);
  const accountModalJsSource = readOptional(files.accountModalJs);
  const accountRoleModalSource = readRequired(files.accountRoleModal);
  const accountRoleModalJsSource = readOptional(files.accountRoleModalJs);
  const deptModalSource = readRequired(files.deptModal);
  const deptModalJsSource = readOptional(files.deptModalJs);
  const roleModalSource = readRequired(files.roleModal);
  const roleModalJsSource = readOptional(files.roleModalJs);
  const sessionModalSource = readRequired(files.sessionModal);
  const sessionModalJsSource = readOptional(files.sessionModalJs);
  const auditModalSource = readRequired(files.auditModal);
  const auditModalJsSource = readOptional(files.auditModalJs);
  const menuModalSource = readRequired(files.menuModal);
  const menuModalJsSource = readOptional(files.menuModalJs);

  checkPageTemplateSource(pageTemplateSource, toRelative(files.pageTemplate), true);
  checkPageTemplateSource(pageTemplateJsSource, toRelative(files.pageTemplateJs), false);

  checkAccountModalSource(accountModalSource, toRelative(files.accountModal));
  checkAccountModalSource(accountModalJsSource, toRelative(files.accountModalJs));
  checkAccountRoleModalSource(accountRoleModalSource, toRelative(files.accountRoleModal));
  checkAccountRoleModalSource(accountRoleModalJsSource, toRelative(files.accountRoleModalJs));
  checkDeptModalSource(deptModalSource, toRelative(files.deptModal));
  checkDeptModalSource(deptModalJsSource, toRelative(files.deptModalJs));
  checkRoleModalSource(roleModalSource, toRelative(files.roleModal));
  checkRoleModalSource(roleModalJsSource, toRelative(files.roleModalJs));

  checkSessionModalSource(sessionModalSource, toRelative(files.sessionModal));
  checkSessionModalSource(sessionModalJsSource, toRelative(files.sessionModalJs));
  checkMenuModalSource(menuModalSource, toRelative(files.menuModal));
  checkMenuModalSource(menuModalJsSource, toRelative(files.menuModalJs));
  checkAuditModalSource(auditModalSource, toRelative(files.auditModal), true);
  checkAuditModalSource(auditModalJsSource, toRelative(files.auditModalJs), false);

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

function checkStaticPartnerRoutes() {
  const routeSources = [
    { file: files.routes, source: readRequired(files.routes) },
    { file: files.routesJs, source: readRequired(files.routesJs) },
  ];
  const routeGuardWrapperSource = readRequired(files.routeGuardWrapper);
  const routeGuardWrapperJsSource = readRequired(files.routeGuardWrapperJs);
  const sessionServiceSource = readRequired(files.sessionService);
  const sessionServiceJsSource = readRequired(files.sessionServiceJs);
  const remoteMenuStorageSource = readRequired(files.remoteMenuStorage);
  const remoteMenuStorageJsSource = readRequired(files.remoteMenuStorageJs);

  for (const { file, source } of routeSources) {
    const relativePath = toRelative(file);
    if (!source) {
      continue;
    }
    for (const module of modules) {
      const routePath = `/${module.key}`;
      const routeBlock = extractRouteBlock(source, routePath);
      if (!routeBlock) {
        violations.push(`${relativePath} must keep static ${routePath} fallback route`);
        continue;
      }
      assertIncludes(
        routeBlock,
        relativePath,
        `authority: ['${module.key}:admin:list']`,
        `must guard static ${routePath} route by ${module.key}:admin:list`,
      );
      assertIncludes(
        routeBlock,
        relativePath,
        "wrappers: ['@/wrappers/RemoteMenuRouteGuard']",
        `must wrap static ${routePath} route with RemoteMenuRouteGuard`,
      );
      assertIncludes(
        routeBlock,
        relativePath,
        `component: './${capitalize(module.key)}'`,
        `must route static ${routePath} to ${capitalize(module.key)} page`,
      );
    }
  }

  if (sessionServiceSource) {
    assertIncludes(
      sessionServiceSource,
      toRelative(files.sessionService),
      'export function RemoteMenuRouteGuard',
      'must export RemoteMenuRouteGuard for static fallback wrappers',
    );
    assertIncludes(
      sessionServiceSource,
      toRelative(files.sessionService),
      'getRemoteMenuStorageKey',
      'must use scoped remote menu cache keys',
    );
    assertNotIncludes(
      sessionServiceSource,
      toRelative(files.sessionService),
      "const REMOTE_MENU_STORAGE_KEY = 'admin_remote_menu'",
      'must not use a global remote menu cache key',
    );
    assertIncludes(
      sessionServiceSource,
      toRelative(files.sessionService),
      'permissions.length > 0 && permissions.some',
      'must fail closed when a remote menu route has no authority',
    );
  }
  if (sessionServiceJsSource) {
    assertIncludes(
      sessionServiceJsSource,
      toRelative(files.sessionServiceJs),
      "export * from './session.ts';",
      'must keep the JavaScript session mirror exporting RemoteMenuRouteGuard',
    );
  }

  for (const { file, source } of [
    { file: files.remoteMenuStorage, source: remoteMenuStorageSource },
    { file: files.remoteMenuStorageJs, source: remoteMenuStorageJsSource },
  ]) {
    const relativePath = toRelative(file);
    if (!source) {
      continue;
    }
    for (const expected of [
      "['admin', 'seller', 'buyer']",
      'getRemoteMenuStorageKey',
      'admin_remote_menu:${scope}',
    ]) {
      assertIncludes(
        source,
        relativePath,
        expected,
        `must keep scoped remote menu storage support for ${expected}`,
      );
    }
  }

  for (const { file, source } of [
    { file: files.routeGuardWrapper, source: routeGuardWrapperSource },
    { file: files.routeGuardWrapperJs, source: routeGuardWrapperJsSource },
  ]) {
    const relativePath = toRelative(file);
    if (!source) {
      continue;
    }
    for (const expected of [
      'RemoteMenuRouteGuard',
      'useLocation',
      'STATIC_ROUTE_AUTHORITIES',
      'PUBLIC_PORTAL_ROUTE_PATHS',
      'getStaticRouteAuthority',
      "route?.authority",
      'normalizePathname',
      'startsWith',
      "'/seller': ['seller:admin:list']",
      "'/buyer': ['buyer:admin:list']",
      "'/seller/direct-login'",
      "'/buyer/direct-login'",
      "'/seller/portal'",
      "'/buyer/portal'",
    ]) {
      assertIncludes(
        source,
        relativePath,
        expected,
        `must keep static partner route guard support for ${expected}`,
      );
    }
  }
}

for (const module of modules) {
  checkPage(module);
  checkService(module);
}
checkPartnerTypes();
checkPartnerReadTypes();
checkSharedTemplate();
checkStaticPartnerRoutes();

if (violations.length > 0) {
  console.error('Partner management template guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Partner management template guard passed.');
