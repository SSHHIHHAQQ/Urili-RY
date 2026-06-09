# 只读 P0/P1 审计报告（切片 5：React portal / request / proxy / access / JS mirror）

- 审计日期：2026-06-09
- 审计模式：只读，不修改业务代码
- 审计范围：
  - `react-ui/src/app.tsx`
  - `react-ui/src/requestErrorConfig.ts`
  - `react-ui/src/access.ts`
  - `react-ui/src/utils/portalPaths.ts`
  - `react-ui/src/utils/portalRequest.ts`
  - `react-ui/src/pages/Portal/**`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/services/session.ts`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
  - `react-ui/config/proxy.ts`
  - `react-ui/scripts/check-portal-token-isolation.mjs`
  - 相关 `.js` mirror、manifest 与 Jest 合同测试

## P0 / P1 结论

本次切片内未发现新增的 P0 / P1 问题。

## P0 / P1 证据

### 1. `/api/seller/**` 与 `/api/buyer/**` 的 401 只清当前端 token，并保留当前 portal 路由为 `redirect`

- `react-ui/src/utils/portalRequest.ts:29-45` 通过 `getPortalTerminalFromApiUrl(...)` 识别 portal API，同时显式跳过 `/api/seller/admin/**`、`/api/buyer/admin/**`。
- `react-ui/src/app.tsx:73-85` 在 `handleUnauthorizedResponse(...)` 中，对 portal 401 仅执行 `clearTerminalSessionToken(portalTerminal)`，随后 `redirectToPortalLogin(portalTerminal)`。
- `react-ui/src/requestErrorConfig.ts:33-45` 在 request 错误处理层复用相同逻辑；portal 命中时不会清管理端 token。
- `react-ui/src/app.tsx:33-40`、`react-ui/src/requestErrorConfig.ts:19-26` 都使用 `history.replace(\`${loginPath}?redirect=${encodeURIComponent(redirect)}\`)`，保留当前路径、查询串和 hash。
- 运行证据：
  - `tests/portal-unauthorized-redirect.test.ts` 通过，覆盖 seller/buyer portal 401、direct-login 401、admin 前缀例外、body-level 401 reject。
  - 关键断言见 `react-ui/tests/portal-unauthorized-redirect.test.ts:89-160`、`207-279`。

### 2. `/api/*/admin` 仍走管理端，不会误判成 portal

- `react-ui/src/utils/portalRequest.ts:33-42` 先排除 `adminPrefix`，再识别 portal prefix。
- `react-ui/tests/portal-unauthorized-redirect.test.ts:233-279` 显式固定：
  - `/api/seller/admin/menus/list`
  - `/api/buyer/admin/menus/list`
  - `/api/seller/admin/sellers/list`
  的 401 都继续走管理端登录跳转，而不是 seller/buyer portal 登录。

### 3. portal 登录 `redirect` 白名单没有把 `/seller/*`、`/buyer/*` 管理路由当成 portal

- `react-ui/src/utils/portalPaths.ts:23-28` 只允许三类 portal 路径：
  - `/{terminal}/login`
  - `/{terminal}/direct-login`
  - `/{terminal}/portal/**`
- `react-ui/src/pages/Portal/Login/index.tsx:38-49` 的 `resolveRedirect(...)` 要求：
  - `redirect` 必须满足 `isPortalTerminalPath(redirect, terminal)`
  - 不能回到当前 terminal 的登录页
  - 不能回到 `/{terminal}/direct-login`
- 因此 `/seller`、`/seller/*`、`/buyer`、`/buyer/*` 管理路由不会被当作 portal 登录后的合法回跳目标。

### 4. 响应体 `code = 401` 会在完成 portal/admin 分流后继续 reject / throw

- `react-ui/src/requestErrorConfig.ts:88-95`：BizError 的 `errorCode = 401` 会 `handleUnauthorized(requestUrl)` 后 `throw error`。
- `react-ui/src/requestErrorConfig.ts:119-125`：HTTP 401 会 `handleUnauthorized(requestUrl)` 后 `throw error`。
- `react-ui/src/app.tsx:351-356`：response interceptor 命中 body-level `code = 401` 后，执行 `handleUnauthorizedResponse(response?.config?.url)`，随后 `return Promise.reject(response)`。
- `react-ui/tests/portal-unauthorized-redirect.test.ts:143-160`、`262-279` 固定了 body-level 401 在 portal/admin 两侧都必须 reject，而不是继续被业务页当成功响应处理。

### 5. 远程菜单空 `authority` 仍然 fail-closed

- `react-ui/src/services/session.ts:95-135`
  - `normalizeAuthority(...)` 会把空权限归一化成 `[]`
  - `allowed` 要求 `permissions.length > 0`
  - 空 authority 最终返回 `403 Forbidden`
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx:57-87,111-114`
  - portal 公共路由被排除，不套管理端静态 authority fallback
  - `/seller`、`/buyer` 等管理端静态路由继续绑定显式 authority fallback
- `react-ui/tests/remote-menu-route-guard.test.ts:150-165` 明确断言“remote menu route has no authority”时渲染 `403`。

### 6. 相关 `.js` mirror 已纳入 guard，不是只守 TS 源文件

- `react-ui/scripts/check-portal-token-isolation.mjs:23-55` 显式枚举并检查：
  - `src/app.js`
  - `src/access.js`
  - `src/requestErrorConfig.js`
  - `config/proxy.js`
  - `src/utils/portalPaths.js`
  - `src/utils/portalRequest.js`
  - `src/services/portal/session.js`
  - `src/services/session.js`
  - `src/wrappers/RemoteMenuRouteGuard.js`
  - `src/pages/Portal/Login/index.js`
  - `src/pages/Portal/Home/index.js`
  - `src/pages/Portal/DirectLogin/index.js`
- 同一脚本在 `264-271`、`359-366`、`400-407`、`496-549`、`717-724`、`901-926`、`1026-1067` 对这些 mirror 固定“纯 re-export / bridge export”合同。
- `react-ui/package.json:16-32` 中：
  - `guard:portal-token = node scripts/check-portal-token-isolation.mjs`
  - `test/test:unit/jest/verify:three-terminal` 都走 `scripts/verify-three-terminal.mjs`
- `node scripts/verify-three-terminal.mjs --check-manifest` 已通过，说明 guard 脚本仍在 manifest 管控面里。

## P2

本次切片内未发现需要单列的新增 P2。

## 执行命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run guard:portal-token
node scripts/verify-three-terminal.mjs --check-manifest
node .\node_modules\jest\bin\jest.js --config jest.config.ts --runInBand tests/portal-unauthorized-redirect.test.ts
node .\node_modules\jest\bin\jest.js --config jest.config.ts --runInBand tests/remote-menu-route-guard.test.ts
```

## 命令结果

- `npm run guard:portal-token`：通过，输出 `Portal token isolation guard passed.`
- `node scripts/verify-three-terminal.mjs --check-manifest`：通过，输出 `three-terminal manifest check passed.`
- `tests/portal-unauthorized-redirect.test.ts`：`19 passed`
- `tests/remote-menu-route-guard.test.ts`：`13 passed`

## 未覆盖项

- 本轮未做浏览器级 seller/buyer portal 401 实流回归；当前结论基于源码、guard 和 Jest 合同测试。
- 本轮为只读审计，未运行 `codegraph sync .`；原因是没有代码改动。

## 结论

当前 `react-ui` 在本切片关注的 portal/request/proxy/access/JS mirror 面上，`/api/seller/**` 与 `/api/buyer/**` 的 401 分流、`/api/*/admin` 管理端例外、portal 登录 redirect 白名单、body-level 401 reject，以及远程菜单空 authority fail-closed 都仍然成立；相关 `.js` mirror 也已经被 guard 和 manifest 固定，没有发现新增的三端隔离 P0/P1 漏口。
