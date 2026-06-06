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
const componentJsFile = path.join(
  root,
  'src',
  'pages',
  'Portal',
  'Home',
  'SellerOwnDistributionProductList.js',
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
const componentJsSource = readRequired(componentJsFile);
const homeSource = readRequired(homeFile);
const homeJsSource = readRequired(homeJsFile);
const portalServiceSource = readRequired(portalServiceFile);
const portalServiceJsSource = readRequired(portalServiceJsFile);

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
      `${relativePath} must import seller product APIs from portal session service`,
    );
  }
  if (requireTypes) {
    if (!source.includes('API.Partner.SellerPortalProduct')) {
      violations.push(`${relativePath} must use API.Partner.SellerPortalProduct`);
    }
    if (!source.includes('API.Partner.SellerPortalProductSku')) {
      violations.push(
        `${relativePath} must use API.Partner.SellerPortalProductSku`,
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
    /\/api\/(?:product\/admin|seller)\/distribution-products/,
    'must not hardcode product API paths',
  );
  assertNoPattern(
    source,
    relativePath,
    /\bAPI\.ProductDistribution\b/,
    'must not reuse admin product distribution types',
  );
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
      "import SellerOwnDistributionProductList from './SellerOwnDistributionProductList';",
    )
  ) {
    violations.push(
      `${relativePath} must import SellerOwnDistributionProductList`,
    );
  }
  const componentIndex = source.indexOf(
    '<SellerOwnDistributionProductList',
  );
  if (componentIndex < 0) {
    violations.push(
      `${relativePath} must render SellerOwnDistributionProductList`,
    );
  } else {
    const sellerBranchIndex = source.lastIndexOf(
      "terminal === 'seller'",
      componentIndex,
    );
    const buyerBranchIndex = source.lastIndexOf(
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
    source,
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

checkPortalServiceSource(portalServiceSource, toRelative(portalServiceFile));
checkPortalServiceSource(portalServiceJsSource, toRelative(portalServiceJsFile));

if (violations.length > 0) {
  console.error('Seller portal product template guard failed:');
  for (const violation of violations) {
    console.error(`- ${violation}`);
  }
  process.exit(1);
}

console.log('Seller portal product template guard passed.');
