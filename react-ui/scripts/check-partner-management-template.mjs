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
    }
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
    assertIncludes(
      directLoginBlock,
      relativePath,
      'token: string',
      'DirectLoginResult must expose direct-login token for postMessage delivery',
    );
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
    'config.services.updateDept(partnerId, payload)',
    'config.services.addDept(partnerId, payload)',
    'config.services.removeDept(partnerId, dept.deptId',
    'access.hasPerms(`${permPrefix}:dept:add`)',
    'access.hasPerms(`${permPrefix}:dept:edit`)',
    'access.hasPerms(`${permPrefix}:dept:remove`)',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep department modal terminal-scoped behavior for ${expected}`,
    );
  }
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
    'access.hasPerms(`${permPrefix}:role:add`)',
    'access.hasPerms(`${permPrefix}:role:edit`)',
    'access.hasPerms(`${permPrefix}:role:remove`)',
  ]) {
    assertIncludes(
      source,
      relativePath,
      expected,
      `must keep role modal terminal-scoped behavior for ${expected}`,
    );
  }
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
    'normalized.startsWith(`${moduleKey}:`)',
    'forbiddenPathRoots',
    'forbiddenComponentRoots',
    'normalized.startsWith(`${moduleKey}:admin:`)',
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

for (const module of modules) {
  checkPage(module);
  checkService(module);
}
checkPartnerTypes();
checkPartnerReadTypes();
checkSharedTemplate();

if (violations.length > 0) {
  console.error('Partner management template guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Partner management template guard passed.');
