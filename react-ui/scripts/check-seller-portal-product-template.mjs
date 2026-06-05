import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const componentFile = path.join(
  root,
  'src',
  'pages',
  'Portal',
  'Home',
  'SellerOwnDistributionProductList.tsx',
);
const homeFile = path.join(root, 'src', 'pages', 'Portal', 'Home', 'index.tsx');
const portalServiceFile = path.join(
  root,
  'src',
  'services',
  'portal',
  'session.ts',
);

const requiredServiceFunctions = [
  'getSellerPortalDistributionProducts',
  'getSellerPortalDistributionProduct',
  'getSellerPortalDistributionProductSkus',
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

const componentSource = readRequired(componentFile);
const homeSource = readRequired(homeFile);
const portalServiceSource = readRequired(portalServiceFile);

if (componentSource) {
  const relativePath = toRelative(componentFile);
  for (const fnName of requiredServiceFunctions) {
    if (!componentSource.includes(fnName)) {
      violations.push(`${relativePath} must use ${fnName}`);
    }
  }
  if (!componentSource.includes("from '@/services/portal/session'")) {
    violations.push(
      `${relativePath} must import seller product APIs from portal session service`,
    );
  }
  if (!componentSource.includes('API.Partner.SellerPortalProduct')) {
    violations.push(`${relativePath} must use API.Partner.SellerPortalProduct`);
  }
  if (!componentSource.includes('API.Partner.SellerPortalProductSku')) {
    violations.push(
      `${relativePath} must use API.Partner.SellerPortalProductSku`,
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
    /\/api\/(?:product\/admin|seller)\/distribution-products/,
    'must not hardcode product API paths',
  );
  assertNoPattern(
    componentSource,
    relativePath,
    /\bAPI\.ProductDistribution\b/,
    'must not reuse admin product distribution types',
  );
}

if (homeSource) {
  const relativePath = toRelative(homeFile);
  if (
    !homeSource.includes(
      "import SellerOwnDistributionProductList from './SellerOwnDistributionProductList';",
    )
  ) {
    violations.push(
      `${relativePath} must import SellerOwnDistributionProductList`,
    );
  }
  const componentIndex = homeSource.indexOf(
    '<SellerOwnDistributionProductList',
  );
  if (componentIndex < 0) {
    violations.push(
      `${relativePath} must render SellerOwnDistributionProductList`,
    );
  } else {
    const sellerBranchIndex = homeSource.lastIndexOf(
      "terminal === 'seller'",
      componentIndex,
    );
    const buyerBranchIndex = homeSource.lastIndexOf(
      "terminal === 'buyer'",
      componentIndex,
    );
    if (sellerBranchIndex < 0 || buyerBranchIndex > sellerBranchIndex) {
      violations.push(
        `${relativePath} must render SellerOwnDistributionProductList only in seller branch`,
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
    if (!fnSource.includes("buildPortalUrl('seller'")) {
      violations.push(`${relativePath} ${fnName} must use seller portal URL`);
    }
    if (!/\bisToken\s*:\s*false\b/.test(fnSource)) {
      violations.push(`${relativePath} ${fnName} must set isToken:false`);
    }
    if (fnSource.includes("buildPortalUrl('buyer'")) {
      violations.push(
        `${relativePath} ${fnName} must not use buyer portal URL`,
      );
    }
  }

  const listSource = extractFunction(
    portalServiceSource,
    'getSellerPortalDistributionProducts',
  );
  if (listSource) {
    if (!listSource.includes("'/product/distribution-products/list'")) {
      violations.push(
        `${relativePath} getSellerPortalDistributionProducts must use seller distribution product list endpoint`,
      );
    }
    if (!listSource.includes('params: sanitizePortalQueryParams(params)')) {
      violations.push(
        `${relativePath} getSellerPortalDistributionProducts must sanitize query params`,
      );
    }
    if (/\n\s*params\s*,/.test(listSource)) {
      violations.push(
        `${relativePath} getSellerPortalDistributionProducts must not pass raw params`,
      );
    }
  }
}

if (violations.length > 0) {
  console.error('Seller portal product template guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Seller portal product template guard passed.');
