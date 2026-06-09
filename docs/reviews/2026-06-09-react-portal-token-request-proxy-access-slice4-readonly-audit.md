# 2026-06-09 React Portal Token/Request/Proxy/Access/Direct-Login/401 只读审计（切片 4）

## 审计范围

- 目标文档：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 目标代码：`react-ui` 中 seller/buyer portal 的 token、request、proxy、access、direct-login、401 处理
- 审计级别：仅查 P0/P1
- 约束：只读，不修改业务代码

## 结论

本切片未发现需要立即修复的 P0/P1 缺口。

当前实现已经满足本轮关注的 5 个检查点：

1. seller / buyer portal token 隔离存在，且写入/清理按 terminal 分流
2. portal 401 只清当前端 token，admin 401 仍走 admin 登录流
3. direct-login 成功提示等待目标端消费确认，不是拿到链接就提示成功
4. 空 `authority` 继续 fail-closed 拒绝访问
5. `proxy/access` 以及相关 JS 镜像已纳入 guard 与测试覆盖

## P0/P1 证据

### 1. Portal token 按 terminal 隔离

- `react-ui/src/access.ts:30-49` 定义三套独立 key：`access_token` / `seller_access_token` / `buyer_access_token`
- `react-ui/src/access.ts:55-93` 的 `setTerminalSessionToken` / `clearTerminalSessionToken` 只读写选中 terminal 的 key
- `react-ui/src/pages/Portal/terminal.ts:38-49` 的 `persistPortalLogin(...)` 只有 `result.terminal === expectedTerminal` 才持久化 token
- `react-ui/tests/terminal-session-token.test.ts:25-53` 验证三端 key 不重叠，且 buyer 清理不会误删 seller
- `react-ui/tests/terminal-session-token.test.ts:55-90` 验证跨端 login/direct-login 响应不会写错 terminal，也不会预清已有 token

### 2. Portal request 使用当前端 token，且剥离前端伪造 scope

- `react-ui/src/utils/portalRequest.ts:29-45` 只把 `/api/seller/**`、`/api/buyer/**` 且非 `/api/*/admin/**` 识别为 portal API
- `react-ui/tests/portal-session-request.test.ts:234-244` 验证 admin 前缀不会被误判成 portal 请求
- `react-ui/tests/portal-session-request.test.ts:260-318` 验证 seller/buyer 请求只带对应 terminal token，并剥离 `sellerId` / `buyerId` / `subjectId` / `accountId` 等前端传入 scope 字段
- `react-ui/tests/portal-session-request.test.ts:321-342` 覆盖 portal 账号、日志、会话、商品等请求都不回退到 admin token

### 3. Portal 401 只清当前端 token，direct-login 401 不误清现有会话

- `react-ui/src/app.tsx:73-85` 的 `handleUnauthorizedResponse(...)`：
  - portal API：`clearTerminalSessionToken(portalTerminal)` + `redirectToPortalLogin(portalTerminal)`
  - admin API：`clearAdminSession()` + `redirectToLogin()`
- `react-ui/src/app.tsx:75-81` 对 `/api/*/direct-login` 特判，401 时直接返回，不清 portal 既有 token
- `react-ui/src/app.tsx:353-358` 响应体 `code/errorCode = 401` 后会 `Promise.reject(response)`，不会把 401 当成功结果继续流向页面
- `react-ui/tests/portal-unauthorized-redirect.test.ts:89-125` 验证 seller portal 401 只清 seller token，不清 admin token，并保留 redirect
- `react-ui/tests/portal-unauthorized-redirect.test.ts:127-205` 验证 direct-login 的 HTTP/BizError 401 不清任何既有 portal token
- `react-ui/tests/portal-unauthorized-redirect.test.ts:207-280` 验证 `/api/seller/admin/**`、`/api/buyer/admin/**` 的 401 仍回 admin 登录流

### 4. Direct-login 成功消息等待目标端消费确认

- `react-ui/src/utils/portalDirectLoginMessage.ts:98-175`
  - 先打开 popup
  - 等目标页发 `PORTAL_DIRECT_LOGIN_READY_MESSAGE`
  - 再 post 一次性 token
  - 只有收到同 terminal、同 ticket 的 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE(status=success)` 才 resolve
- `react-ui/src/pages/Portal/DirectLogin/index.tsx:63-112` 目标 portal 页收到 token 后调用 `PORTAL_SERVICE[terminal].directLogin(...)`，成功/失败都通过 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 回传 opener
- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:617-623` 与 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:564-569` 都是 `await openPortalDirectLoginWindow(...)` 后才 `message.success(...)`
- `react-ui/tests/portal-direct-login-message.test.ts:30-113` 明确验证“READY 之后才发 token，且只有 matching consume result 后 bridge 才 resolve”
- `react-ui/scripts/check-portal-token-isolation.mjs:609-614` 还把“等待 consume ack 才提示成功”固化成 guard

### 5. 空 authority 拒绝，proxy/access 镜像被 guard 覆盖

- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx:57-64` 把 portal login/direct-login/portal 页面列为公开 portal 路由；seller/buyer 管理页不在公开名单内
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx:106-114` 对受保护静态路由使用 `route?.authority ?? getStaticRouteAuthority(...) ?? []`
- 当 authority 缺失时最终传入空数组，guard 继续 fail-closed，而不是自动放行
- `react-ui/tests/remote-menu-route-guard.test.ts` 已覆盖：
  - 缺少权限返回 403
  - `authorityMode = all` 必须全满足
  - `authority: []` 返回 403
  - seller/buyer 静态 fallback authority 不串端
- `react-ui/tests/three-terminal.manifest.json:117-160` 已把
  - `tests/terminal-session-token.test.ts`
  - `tests/portal-session-request.test.ts`
  - `tests/portal-direct-login-message.test.ts`
  - `tests/remote-menu-route-guard.test.ts`
  - `tests/portal-unauthorized-redirect.test.ts`
  - `tests/admin-auth-sidecar-contract.test.ts`
  - `guard:portal-token -> node scripts/check-portal-token-isolation.mjs`
  纳入 manifest
- `react-ui/tests/admin-auth-sidecar-contract.test.ts:23-27,78-81,178-182` 固化 `src/access.js`、`config/proxy.js` 等 JS 镜像必须纯 re-export 到 TS 源文件
- `react-ui/scripts/check-portal-token-isolation.mjs:870-890` 固化“portal 401 只清当前端 token 并跳对应 portal 登录”
- `react-ui/scripts/check-portal-token-isolation.mjs:1005-1033` 固化 `config/proxy.ts` 必须保留 `API_PROXY_TARGET`、`/api/` 代理、`changeOrigin`、`^/api` 重写
- `react-ui/scripts/check-portal-token-isolation.mjs:1036-1066` 固化 `src/access.ts` 的 terminal token key 与 `src/access.js` 镜像桥

## 已执行的最小验证

在 `E:\Urili-Ruoyi\react-ui` 执行：

```powershell
node scripts/check-portal-token-isolation.mjs
node scripts/prepare-umi-test.mjs
npx jest --config jest.config.ts --runTestsByPath `
  tests/terminal-session-token.test.ts `
  tests/portal-session-request.test.ts `
  tests/portal-direct-login-message.test.ts `
  tests/remote-menu-route-guard.test.ts `
  tests/portal-unauthorized-redirect.test.ts `
  tests/admin-auth-sidecar-contract.test.ts `
  --runInBand
```

结果：

- `Portal token isolation guard passed.`
- `6` 个 test suite 全过，`101` 个测试全过，`0` skipped / `0` todo

## 最小修复建议（仅在后续回归时使用）

本轮无 P0/P1 待修。

若后续此切片回归，最小修法应保持 fail-closed，不放松 guard：

1. token 串端：只修 `src/access.ts` 与 `src/pages/Portal/terminal.ts` 的 terminal key / terminal match，不引入共享 portal token key
2. 401 串清理：只修 `src/app.tsx` 的 `getPortalTerminalFromApiUrl(...)` 分流与 `/direct-login` 特判，不把 portal/admin 401 合并处理
3. direct-login 误报成功：只修 `src/utils/portalDirectLoginMessage.ts` 的 READY -> TOKEN -> RESULT 握手，不回退成“拿到 loginUrl 就成功”
4. authority 空放行：只补 route authority / `/getRouters.perms` / static fallback 映射，不修改成 permissive guard
5. JS 镜像漂移：保持 `src/access.js`、`config/proxy.js` 等 sidecar 纯 re-export，并继续通过 manifest + guard 固化

## 一句话总结

切片 4 当前 P0/P1 状态为通过：portal token、401、direct-login、authority、proxy/access guard 已形成实现 + 单测 + manifest/guard 三层闭环。
