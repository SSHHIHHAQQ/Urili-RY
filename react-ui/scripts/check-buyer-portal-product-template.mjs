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
const homeFile = path.join(root, 'src', 'pages', 'Portal', 'Home', 'index.tsx');
const portalServiceFile = path.join(
  root,
  'src',
  'services',
  'portal',
  'session.ts',
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
const homeSource = readRequired(homeFile);
const portalServiceSource = readRequired(portalServiceFile);
const typeSource = readRequired(typeFile);

if (componentSource) {
  const relativePath = toRelative(componentFile);
  for (const fnName of requiredServiceFunctions) {
    if (!componentSource.includes(fnName)) {
      violations.push(`${relativePath} must use ${fnName}`);
    }
  }
  if (!componentSource.includes("from '@/services/portal/session'")) {
    violations.push(
      `${relativePath} must import buyer product APIs from portal session service`,
    );
  }
  if (!componentSource.includes('API.Partner.BuyerPortalProduct')) {
    violations.push(`${relativePath} must use API.Partner.BuyerPortalProduct`);
  }
  if (!componentSource.includes('API.Partner.BuyerPortalProductSku')) {
    violations.push(
      `${relativePath} must use API.Partner.BuyerPortalProductSku`,
    );
  }
  assertNoPattern(
    componentSource,
    relativePath,
    /\brequest\s*(?:<[^;]+?>)?\s*\(/,
    'must not call request(...) directly',
  );
  assertNoPattern(
    componentSource,
    relativePath,
    /from\s+['"]@\/services\/product\//,
    'must not import admin product services',
  );
  assertNoPattern(
    componentSource,
    relativePath,
    /\/api\/(?:product\/admin|seller|buyer)\/distribution-products/,
    'must not hardcode product API paths',
  );
  assertNoPattern(
    componentSource,
    relativePath,
    /\bAPI\.ProductDistribution\b/,
    'must not reuse admin product distribution types',
  );
  assertNoPattern(
    componentSource,
    relativePath,
    /\bAPI\.Partner\.SellerPortalProduct\b/,
    'must not reuse seller product DTO',
  );
  assertNoPattern(
    componentSource,
    relativePath,
    /\bAPI\.Partner\.SellerPortalProductSku\b/,
    'must not reuse seller SKU DTO',
  );
  assertNoBuyerForbiddenFields(componentSource, relativePath);
}

if (homeSource) {
  const relativePath = toRelative(homeFile);
  if (
    !homeSource.includes(
      "import BuyerDistributionProductList from './BuyerDistributionProductList';",
    )
  ) {
    violations.push(`${relativePath} must import BuyerDistributionProductList`);
  }
  const componentIndex = homeSource.indexOf('<BuyerDistributionProductList');
  if (componentIndex < 0) {
    violations.push(`${relativePath} must render BuyerDistributionProductList`);
  } else {
    const buyerBranchIndex = homeSource.lastIndexOf(
      "terminal === 'buyer'",
      componentIndex,
    );
    const sellerBranchIndex = homeSource.lastIndexOf(
      "terminal === 'seller'",
      componentIndex,
    );
    if (buyerBranchIndex < 0 || sellerBranchIndex > buyerBranchIndex) {
      violations.push(
        `${relativePath} must render BuyerDistributionProductList only in buyer branch`,
      );
    }
  }

  const sellerComponentIndex = homeSource.indexOf(
    '<SellerOwnDistributionProductList',
  );
  if (sellerComponentIndex >= 0) {
    const sellerBranchIndex = homeSource.lastIndexOf(
      "terminal === 'seller'",
      sellerComponentIndex,
    );
    const buyerBranchIndex = homeSource.lastIndexOf(
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

if (portalServiceSource) {
  const relativePath = toRelative(portalServiceFile);
  for (const fnName of requiredServiceFunctions) {
    const fnSource = extractFunction(portalServiceSource, fnName);
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
    portalServiceSource,
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
