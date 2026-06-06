import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const componentFile = path.join(
  root,
  'src',
  'pages',
  'Portal',
  'Home',
  'BuyerDistributionProductList.tsx',
);
const componentJsFile = path.join(
  root,
  'src',
  'pages',
  'Portal',
  'Home',
  'BuyerDistributionProductList.js',
);
const homeFile = path.join(root, 'src', 'pages', 'Portal', 'Home', 'index.tsx');
const homeJsFile = path.join(root, 'src', 'pages', 'Portal', 'Home', 'index.js');
const portalServiceFile = path.join(
  root,
  'src',
  'services',
  'portal',
  'session.ts',
);
const portalServiceJsFile = path.join(
  root,
  'src',
  'services',
  'portal',
  'session.js',
);
const typeFile = path.join(root, 'src', 'types', 'seller-buyer', 'party.d.ts');

const requiredServiceFunctions = [
  'getBuyerPortalDistributionProducts',
  'getBuyerPortalDistributionProduct',
  'getBuyerPortalDistributionProductSkus',
];

const forbiddenBuyerFieldPatterns = [
  /\bsellerId\b/,
  /\bsellerNo\b/,
  /\bsellerName\b/,
  /\bsellerSpuCode\b/,
  /\bsellerSkuCode\b/,
  /\bsystemSpuCode\b/,
  /\bsystemSkuCode\b/,
  /\bsupplyPrice\w*\b/,
  /\bcreateBy\b/,
  /\bupdateBy\b/,
  /\bremark\b/,
  /\btokenId\b/,
  /\bredisKey\b/,
];

const violations = [];

function readRequired(file) {
  if (!fs.existsSync(file)) {
    violations.push(`${toRelative(file)} is missing`);
    return '';
  }
  return fs.readFileSync(file, 'utf8');
}

function toRelative(file) {
  return path.relative(root, file).replaceAll(path.sep, '/');
}

function extractFunction(source, name) {
  const marker = `export async function ${name}`;
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

function assertNoPattern(source, relativePath, pattern, message) {
  if (pattern.test(source)) {
    violations.push(`${relativePath} ${message}`);
  }
}

function assertNoBuyerForbiddenFields(source, relativePath) {
  for (const pattern of forbiddenBuyerFieldPatterns) {
    assertNoPattern(
      source,
      relativePath,
      pattern,
      `must not reference buyer-hidden field ${pattern}`,
    );
  }
}

const componentSource = readRequired(componentFile);
const componentJsSource = readRequired(componentJsFile);
const homeSource = readRequired(homeFile);
const homeJsSource = readRequired(homeJsFile);
const portalServiceSource = readRequired(portalServiceFile);
const portalServiceJsSource = readRequired(portalServiceJsFile);
const typeSource = readRequired(typeFile);

function checkComponentSource(source, relativePath, requireTypes) {
  if (!source) {
    return;
  }
  for (const fnName of requiredServiceFunctions) {
    if (!source.includes(fnName)) {
      violations.push(`${relativePath} must use ${fnName}`);
    }
  }
  if (!source.includes("from '@/services/portal/session'")) {
    violations.push(
      `${relativePath} must import buyer product APIs from portal session service`,
    );
  }
  if (requireTypes) {
    if (!source.includes('API.Partner.BuyerPortalProduct')) {
      violations.push(`${relativePath} must use API.Partner.BuyerPortalProduct`);
    }
    if (!source.includes('API.Partner.BuyerPortalProductSku')) {
      violations.push(
        `${relativePath} must use API.Partner.BuyerPortalProductSku`,
      );
    }
  }
  for (const requiredToken of [
    'ProTable',
    'getPersistedProTableSearch',
    'getProTablePagination',
    'getProTableScroll',
  ]) {
    if (!source.includes(requiredToken)) {
      violations.push(`${relativePath} must use standard ProTable template token ${requiredToken}`);
    }
  }
  if (requireTypes && !source.includes('ProColumns')) {
    violations.push(`${relativePath} must use standard ProTable template token ProColumns`);
  }
  if (!source.includes('pageNum: currentPage')) {
    violations.push(`${relativePath} must map ProTable current to RuoYi pageNum`);
  }
  if (!source.includes('pageSize: currentPageSize')) {
    violations.push(`${relativePath} must map ProTable pageSize to RuoYi pageSize`);
  }
  assertNoPattern(
    source,
    relativePath,
    /\bspuStatus\s*:\s*params\.spuStatus\b/,
    'must not expose buyer product status as a query filter; buyer visibility is fixed to ON_SALE by backend',
  );
  assertNoPattern(
    source,
    relativePath,
    /\brequest\s*(?:<[^;]+?>)?\s*\(/,
    'must not call request(...) directly',
  );
  assertNoPattern(
    source,
    relativePath,
    /from\s+['"]@\/services\/product\//,
    'must not import admin product services',
  );
  assertNoPattern(
    source,
    relativePath,
    /\/api\/(?:product\/admin|seller|buyer)\/distribution-products/,
    'must not hardcode product API paths',
  );
  assertNoPattern(
    source,
    relativePath,
    /\bAPI\.ProductDistribution\b/,
    'must not reuse admin product distribution types',
  );
  assertNoPattern(
    source,
    relativePath,
    /\bAPI\.Partner\.SellerPortalProduct\b/,
    'must not reuse seller product DTO',
  );
  assertNoPattern(
    source,
    relativePath,
    /\bAPI\.Partner\.SellerPortalProductSku\b/,
    'must not reuse seller SKU DTO',
  );
  assertNoBuyerForbiddenFields(source, relativePath);
}

checkComponentSource(componentSource, toRelative(componentFile), true);
checkComponentSource(componentJsSource, toRelative(componentJsFile), false);

function checkHomeSource(source, relativePath) {
  if (!source) {
    return;
  }
  if (source.includes("export { default } from './index.tsx';")) {
    return;
  }
  if (
    !source.includes(
      "import BuyerDistributionProductList from './BuyerDistributionProductList';",
    )
  ) {
    violations.push(`${relativePath} must import BuyerDistributionProductList`);
  }
  const componentIndex = source.indexOf('<BuyerDistributionProductList');
  if (componentIndex < 0) {
    violations.push(`${relativePath} must render BuyerDistributionProductList`);
  } else {
    const buyerBranchIndex = source.lastIndexOf(
      "terminal === 'buyer'",
      componentIndex,
    );
    const sellerBranchIndex = source.lastIndexOf(
      "terminal === 'seller'",
      componentIndex,
    );
    if (buyerBranchIndex < 0 || sellerBranchIndex > buyerBranchIndex) {
      violations.push(
        `${relativePath} must render BuyerDistributionProductList only in buyer branch`,
      );
    }
  }

  const sellerComponentIndex = source.indexOf(
    '<SellerOwnDistributionProductList',
  );
  if (sellerComponentIndex >= 0) {
    const sellerBranchIndex = source.lastIndexOf(
      "terminal === 'seller'",
      sellerComponentIndex,
    );
    const buyerBranchIndex = source.lastIndexOf(
      "terminal === 'buyer'",
      sellerComponentIndex,
    );
    if (sellerBranchIndex < 0 || buyerBranchIndex > sellerBranchIndex) {
      violations.push(
        `${relativePath} must keep SellerOwnDistributionProductList only in seller branch`,
      );
    }
  }
}

checkHomeSource(homeSource, toRelative(homeFile));
checkHomeSource(homeJsSource, toRelative(homeJsFile));

function checkPortalServiceSource(source, relativePath) {
  if (!source) {
    return;
  }
  for (const fnName of requiredServiceFunctions) {
    const fnSource = extractFunction(source, fnName);
    if (!fnSource) {
      violations.push(`${relativePath} must export ${fnName}`);
      continue;
    }
    if (!fnSource.includes("buildPortalUrl('buyer'")) {
      violations.push(`${relativePath} ${fnName} must use buyer portal URL`);
    }
    if (!fnSource.includes("buildPortalAuthHeaders('buyer')")) {
      violations.push(`${relativePath} ${fnName} must use buyer auth headers`);
    }
    if (!/\bisToken\s*:\s*false\b/.test(fnSource)) {
      violations.push(`${relativePath} ${fnName} must set isToken:false`);
    }
    if (fnSource.includes("buildPortalUrl('seller'")) {
      violations.push(
        `${relativePath} ${fnName} must not use seller portal URL`,
      );
    }
  }

  const listSource = extractFunction(
    source,
    'getBuyerPortalDistributionProducts',
  );
  if (listSource) {
    if (!listSource.includes("'/product/distribution-products/list'")) {
      violations.push(
        `${relativePath} getBuyerPortalDistributionProducts must use buyer distribution product list endpoint`,
      );
    }
    if (!listSource.includes('params: sanitizePortalQueryParams(params)')) {
      violations.push(
        `${relativePath} getBuyerPortalDistributionProducts must sanitize query params`,
      );
    }
    if (/\n\s*params\s*,/.test(listSource)) {
      violations.push(
        `${relativePath} getBuyerPortalDistributionProducts must not pass raw params`,
      );
    }
  }
}

checkPortalServiceSource(portalServiceSource, toRelative(portalServiceFile));
checkPortalServiceSource(portalServiceJsSource, toRelative(portalServiceJsFile));

if (typeSource) {
  const relativePath = toRelative(typeFile);
  for (const typeName of [
    'BuyerPortalProductSku',
    'BuyerPortalProduct',
    'BuyerPortalProductPageResult',
    'BuyerPortalProductInfoResult',
    'BuyerPortalProductSkuListResult',
  ]) {
    if (!typeSource.includes(`interface ${typeName}`)) {
      violations.push(`${relativePath} must define ${typeName}`);
    }
  }
  const buyerTypeStart = typeSource.indexOf('interface BuyerPortalProductSku');
  const directLoginStart = typeSource.indexOf('interface PortalDirectLoginTicket');
  const buyerTypeBlock =
    buyerTypeStart >= 0 && directLoginStart > buyerTypeStart
      ? typeSource.slice(buyerTypeStart, directLoginStart)
      : '';
  if (!buyerTypeBlock) {
    violations.push(`${relativePath} must keep buyer product DTOs before PortalDirectLoginTicket`);
  } else {
    assertNoBuyerForbiddenFields(buyerTypeBlock, relativePath);
  }
}

if (violations.length > 0) {
  console.error('Buyer portal product template guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Buyer portal product template guard passed.');
