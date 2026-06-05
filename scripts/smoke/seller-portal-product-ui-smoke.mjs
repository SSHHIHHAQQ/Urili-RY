import { createRequire } from 'node:module';
import os from 'node:os';
import path from 'node:path';

const DEFAULT_PLAYWRIGHT_REQUIRE_FROM = path.join(
  os.homedir(),
  'AppData',
  'Roaming',
  'npm',
  'node_modules',
  '@playwright',
  'cli',
  'package.json',
);

function parseArgs(argv) {
  const args = {
    baseUrl: 'http://127.0.0.1:8080',
    frontendUrl: 'http://127.0.0.1:8001',
    adminUsername: 'admin',
    adminPassword: process.env.URILI_ADMIN_PASSWORD || '',
    sellerId: 5,
    reason: 'codex seller portal product ui smoke',
    browserChannel: 'chrome',
    executablePath: '',
    headed: false,
    timeoutMs: 30000,
    screenshotPath: '',
    playwrightRequireFrom:
      process.env.PLAYWRIGHT_REQUIRE_FROM || DEFAULT_PLAYWRIGHT_REQUIRE_FROM,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    const next = () => {
      index += 1;
      if (index >= argv.length) {
        throw new Error(`Missing value for ${arg}`);
      }
      return argv[index];
    };

    switch (arg) {
      case '--base-url':
        args.baseUrl = next();
        break;
      case '--frontend-url':
        args.frontendUrl = next();
        break;
      case '--admin-username':
        args.adminUsername = next();
        break;
      case '--admin-password':
        args.adminPassword = next();
        break;
      case '--seller-id':
        args.sellerId = Number(next());
        break;
      case '--reason':
        args.reason = next();
        break;
      case '--browser-channel':
        args.browserChannel = next();
        break;
      case '--executable-path':
        args.executablePath = next();
        break;
      case '--headed':
        args.headed = true;
        break;
      case '--timeout-ms':
        args.timeoutMs = Number(next());
        break;
      case '--screenshot-path':
        args.screenshotPath = next();
        break;
      case '--playwright-require-from':
        args.playwrightRequireFrom = next();
        break;
      default:
        throw new Error(`Unknown argument: ${arg}`);
    }
  }

  if (!Number.isFinite(args.sellerId) || args.sellerId <= 0) {
    throw new Error('--seller-id must be a positive number');
  }
  if (!Number.isFinite(args.timeoutMs) || args.timeoutMs <= 0) {
    throw new Error('--timeout-ms must be a positive number');
  }
  if (!args.adminPassword) {
    throw new Error('--admin-password or URILI_ADMIN_PASSWORD is required');
  }

  args.baseUrl = args.baseUrl.replace(/\/+$/, '');
  args.frontendUrl = args.frontendUrl.replace(/\/+$/, '');
  return args;
}

function redact(value) {
  return String(value)
    .replace(/directLoginToken=[^&\s"']+/g, 'directLoginToken=[redacted]')
    .replace(/Bearer\s+[A-Za-z0-9._-]+/g, 'Bearer [redacted]')
    .replace(/"token"\s*:\s*"[^"]+"/g, '"token":"[redacted]"')
    .replace(/"loginUrl"\s*:\s*"[^"]+"/g, '"loginUrl":"[redacted]"');
}

function assertTrue(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

async function requestJson(method, url, { token, body, timeoutMs } = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs || 30000);
  try {
    const response = await fetch(url, {
      method,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(body ? { 'Content-Type': 'application/json;charset=UTF-8' } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
    const text = await response.text();
    let json;
    try {
      json = text ? JSON.parse(text) : {};
    } catch {
      throw new Error(
        `${method} ${url} did not return JSON, status=${response.status}`,
      );
    }
    if (!response.ok) {
      throw new Error(
        `${method} ${url} failed, status=${response.status}, body=${redact(text)}`,
      );
    }
    return json;
  } finally {
    clearTimeout(timer);
  }
}

async function loginAdmin(args) {
  const captcha = await requestJson('GET', `${args.baseUrl}/captchaImage`, {
    timeoutMs: args.timeoutMs,
  });
  assertTrue(
    captcha.captchaEnabled !== true && captcha.captchaEnabled !== 'true',
    'captcha is enabled; this smoke does not change captcha settings',
  );

  const response = await requestJson('POST', `${args.baseUrl}/login`, {
    body: {
      username: args.adminUsername,
      password: args.adminPassword,
      code: '',
      uuid: captcha.uuid || '',
    },
    timeoutMs: args.timeoutMs,
  });
  assertTrue(
    Number(response.code) === 200,
    `admin login failed, code=${response.code}, msg=${response.msg}`,
  );
  assertTrue(response.token, 'admin login did not return token');
  console.log('[ok] admin login');
  return response.token;
}

async function createDirectLogin(args, adminToken) {
  const response = await requestJson(
    'POST',
    `${args.baseUrl}/seller/admin/sellers/${args.sellerId}/directLogin`,
    {
      token: adminToken,
      body: { reason: args.reason },
      timeoutMs: args.timeoutMs,
    },
  );
  assertTrue(
    Number(response.code) === 200,
    `seller direct-login ticket failed, code=${response.code}, msg=${response.msg}`,
  );
  const token =
    response.data?.token ||
    (response.data?.loginUrl
      ? new URL(response.data.loginUrl).searchParams.get('directLoginToken')
      : '');
  assertTrue(token, 'seller direct-login ticket did not return a usable token');
  console.log('[ok] seller direct-login ticket created');
  return `${args.frontendUrl}/seller/direct-login?directLoginToken=${encodeURIComponent(token)}`;
}

function loadPlaywright(args) {
  try {
    const requireFromPlaywright = createRequire(args.playwrightRequireFrom);
    return requireFromPlaywright('playwright');
  } catch (error) {
    throw new Error(
      `Unable to load playwright from ${args.playwrightRequireFrom}. Run the PowerShell wrapper so it can prepare a runtime. ${error.message}`,
    );
  }
}

async function waitForText(page, text, timeoutMs) {
  await page
    .getByText(text, { exact: false })
    .first()
    .waitFor({ timeout: timeoutMs });
}

async function runBrowserSmoke(args, directLoginUrl) {
  const { chromium } = loadPlaywright(args);
  const launchOptions = {
    headless: !args.headed,
  };
  if (args.executablePath) {
    launchOptions.executablePath = args.executablePath;
  } else if (args.browserChannel) {
    launchOptions.channel = args.browserChannel;
  }

  const browser = await chromium.launch(launchOptions);
  const consoleIssues = [];
  let context;
  try {
    context = await browser.newContext({
      viewport: { width: 1366, height: 900 },
      baseURL: args.frontendUrl,
    });
    const page = await context.newPage();
    page.on('console', (message) => {
      if (['error', 'warning', 'warn'].includes(message.type())) {
        consoleIssues.push(`${message.type()}: ${redact(message.text())}`);
      }
    });
    page.on('pageerror', (error) => {
      consoleIssues.push(`pageerror: ${redact(error.message)}`);
    });

    await page.goto(directLoginUrl, {
      waitUntil: 'domcontentloaded',
      timeout: args.timeoutMs,
    });
    await page.waitForURL(/\/seller\/portal(?:$|\?)/, {
      timeout: args.timeoutMs,
    });
    await page
      .waitForLoadState('networkidle', { timeout: args.timeoutMs })
      .catch(() => {});
    console.log('[ok] seller portal loaded');

    const storage = await page.evaluate(() => ({
      adminToken: window.localStorage.getItem('access_token'),
      sellerToken: window.localStorage.getItem('seller_access_token'),
      buyerToken: window.localStorage.getItem('buyer_access_token'),
    }));
    assertTrue(
      storage.sellerToken,
      'seller token was not persisted in seller_access_token',
    );
    assertTrue(
      !storage.adminToken,
      'seller portal wrote or reused admin access_token in a fresh browser context',
    );
    assertTrue(
      !storage.buyerToken,
      'seller portal wrote buyer_access_token in a fresh browser context',
    );
    console.log('[ok] seller token storage isolated');

    await waitForText(page, '卖家端', args.timeoutMs);
    await waitForText(page, '商品发布准备', args.timeoutMs);
    await waitForText(page, '我的商城商品', args.timeoutMs);
    await waitForText(page, '客户SPU', args.timeoutMs);
    console.log('[ok] seller product card rendered');

    const detailButton = page.getByRole('button', { name: '详情' }).first();
    await detailButton.waitFor({ timeout: args.timeoutMs });
    await detailButton.click();
    await waitForText(page, '商品详情', args.timeoutMs);
    await waitForText(page, '客户SKU', args.timeoutMs);
    await waitForText(page, 'SKU规格', args.timeoutMs);
    await waitForText(page, '商品状态', args.timeoutMs);

    const visibleText = await page
      .locator('body')
      .innerText({ timeout: args.timeoutMs });
    for (const forbidden of [
      'sellerId',
      'systemSpuCode',
      'systemSkuCode',
      'tokenId',
      'Authorization',
    ]) {
      assertTrue(
        !visibleText.includes(forbidden),
        `seller portal UI leaked visible field ${forbidden}`,
      );
    }
    console.log('[ok] seller product detail modal rendered');

    if (args.screenshotPath) {
      await page.screenshot({ path: args.screenshotPath, fullPage: true });
      console.log(`[ok] screenshot saved: ${args.screenshotPath}`);
    }

    assertTrue(
      consoleIssues.length === 0,
      `browser console issues found:\n${consoleIssues.join('\n')}`,
    );

    await page.keyboard.press('Escape');
    await page.locator('.ant-modal-wrap').waitFor({
      state: 'hidden',
      timeout: args.timeoutMs,
    });

    await page.getByRole('button', { name: /退出/ }).click();
    await page.waitForURL(/\/user\/login(?:$|\?)/, { timeout: args.timeoutMs });
    console.log('[ok] seller portal logout cleanup');
  } finally {
    if (context) {
      await context.close();
    }
    await browser.close();
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const adminToken = await loginAdmin(args);
  const directLoginUrl = await createDirectLogin(args, adminToken);
  await runBrowserSmoke(args, directLoginUrl);
  console.log('[pass] seller portal product UI smoke completed.');
}

main().catch((error) => {
  console.error(redact(error.stack || error.message || error));
  process.exit(1);
});
