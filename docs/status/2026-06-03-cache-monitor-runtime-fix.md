# Cache Monitor Runtime Fix

Date: 2026-06-03

Workspace: `E:\Urili-Ruoyi`

## Symptoms

- Clicking `系统监控 -> 缓存监控` showed the Umi error boundary.
- The page displayed `Something went wrong`.
- Browser console reported:

```text
Invalid hook call
Cannot read properties of null (reading 'useContext')
```

## Cause

The cache monitor page rendered `Gauge` and `Pie` from `@ant-design/plots`.
In the current React 19 + Umi dev runtime, that chart chunk triggered an invalid React hook call inside the plots package before the cache API request completed.

Because the exception happened during React render, Umi caught it at the route error boundary and left the app in an error-looking state.

## Fixes

- Replaced `@ant-design/plots` usage on the cache monitor page with Ant Design native `Progress` and `Table`.
- Added loading and error states around the cache monitor API call.
- Reworked the cache list page text and actions to remove garbled UI strings.
- URL-encoded cache name and key path parameters in cache list service calls.
- Fixed backend cache remark strings returned by `GET /monitor/cache/getNames`.
- Removed deprecated Ant Design `List` usage from the account center so browser console no longer reports that warning as an error.

Changed files:

- `react-ui/src/pages/Monitor/Cache/index.tsx`
- `react-ui/src/pages/Monitor/Cache/List.tsx`
- `react-ui/src/services/monitor/cachelist.ts`
- `react-ui/src/pages/User/Center/index.tsx`
- `react-ui/src/pages/User/Center/Center.module.css`
- `RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/monitor/CacheController.java`

## Verification

Commands:

```powershell
npm run tsc
mvn -DskipTests install
Invoke-WebRequest -Uri 'http://127.0.0.1:8080/captchaImage' -UseBasicParsing
```

Browser checks with Playwright:

- `http://127.0.0.1:8001/monitor/cache`
  - `GET /api/monitor/cache` returned 200.
  - Console errors: 0.
- `http://127.0.0.1:8001/monitor/cacheList`
  - `GET /api/monitor/cache/getNames` returned 200.
  - Console errors: 0.
- Clicked `login_tokens:` cache name.
  - `GET /api/monitor/cache/getKeys/login_tokens%3A` returned 200.
  - Console errors: 0.
- `http://127.0.0.1:8001/account/center`
  - Console errors: 0.

## Runtime

- Backend restarted from the rebuilt jar and is listening on `8080`.
- Frontend dev server remains on `8001`.
