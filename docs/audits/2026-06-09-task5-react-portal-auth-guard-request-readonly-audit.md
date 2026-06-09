# Task 5 React Portal Auth/Guard/Request 只读审计

日期：2026-06-09
范围：`react-ui` portal auth / guard / request
模式：只读 P0/P1 审计，不改代码

## 结论

本次针对以下 6 条主线做了只读审计：

1. `/api/seller/**` 与 `/api/buyer/**` 的 401 分流
2. portal redirect 白名单
3. direct-login 消费成功消息闭环
4. token 成功持久化前不清旧 token
5. 跨端响应 fail-closed
6. 空 `authority` 拒绝访问

**未发现属于本切片的 P0/P1 缺陷。**

## 证据

### 1. `/api/seller/**` 与 `/api/buyer/**` 401 分流已按端隔离

- [react-ui/src/utils/portalRequest.ts](E:/Urili-Ruoyi/react-ui/src/utils/portalRequest.ts:29)
  - `getPortalTerminalFromApiUrl(...)` 只把非 `admin` 前缀的 `/api/seller/**`、`/api/buyer/**` 识别为 portal 请求；`/api/seller/admin/**`、`/api/buyer/admin/**` 明确排除。
- [react-ui/src/requestErrorConfig.ts](E:/Urili-Ruoyi/react-ui/src/requestErrorConfig.ts:33)
  - `handleUnauthorized(...)` 对 portal 401 只清当前 terminal token，并跳到对应 `/{terminal}/login`；非 portal 请求继续走 admin 登录流。
- [react-ui/src/app.tsx](E:/Urili-Ruoyi/react-ui/src/app.tsx:73)
  - app runtime 的 `handleUnauthorizedResponse(...)` 与 request errorConfig 逻辑一致，没有把 seller/buyer 401 误伤到 admin token。
- [react-ui/tests/portal-unauthorized-redirect.test.ts](E:/Urili-Ruoyi/react-ui/tests/portal-unauthorized-redirect.test.ts:79)
  - 覆盖 seller/buyer portal 401、BizError 401、admin 前缀 401、response body 401、direct-login 401 等分支。

建议：维持现状；后续新增 portal 接口时继续复用 `getPortalTerminalFromApiUrl(...)` 的分流逻辑，不要在页面内自行判断 terminal。

### 2. portal redirect 白名单已 fail-closed

- [react-ui/src/utils/portalPaths.ts](E:/Urili-Ruoyi/react-ui/src/utils/portalPaths.ts:23)
  - `isPortalTerminalPath(...)` 只接受 `/{terminal}/login`、`/{terminal}/direct-login`、`/{terminal}/portal/**`。
- [react-ui/src/pages/Portal/Login/index.tsx](E:/Urili-Ruoyi/react-ui/src/pages/Portal/Login/index.tsx:38)
  - `resolveRedirect(...)` 对非当前 terminal 白名单路径、回跳登录页、回跳直登页全部回退到 portal home。
- [react-ui/tests/portal-session-request.test.ts](E:/Urili-Ruoyi/react-ui/tests/portal-session-request.test.ts:246)
  - 明确验证 `/seller/login/next`、`/seller/direct-login/next`、`/seller`、`/seller/accounts` 等都不算 portal 合法 redirect。

建议：维持现状；后续若新增 portal 公开页，必须先同步扩充 `isPortalTerminalPath(...)`，否则默认拒绝是正确行为。

### 3. direct-login 成功消息闭环已满足“成功持久化后再确认”

- [react-ui/src/utils/portalDirectLoginMessage.ts](E:/Urili-Ruoyi/react-ui/src/utils/portalDirectLoginMessage.ts:98)
  - opener 侧只在收到目标窗口 `READY` 后发送一次 token，并且只在收到同 terminal、同 origin、同 ticketId 的 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 成功消息后 resolve。
- [react-ui/src/pages/Portal/DirectLogin/index.tsx](E:/Urili-Ruoyi/react-ui/src/pages/Portal/DirectLogin/index.tsx:77)
  - 消费页先调用 `directLogin(...)`，再要求 `persistPortalLogin(...)` 成功；只有成功持久化后才 `postConsumeResult('success', ...)`。
- [react-ui/tests/portal-direct-login-message.test.ts](E:/Urili-Ruoyi/react-ui/tests/portal-direct-login-message.test.ts:27)
  - 覆盖 READY 握手、跨 origin/跨 terminal 忽略、ticketId 不匹配忽略、成功结果才 resolve、失败结果 reject、超时 reject。

建议：维持现状；继续把 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 作为唯一成功确认信号，不要退回到“发出 token 即提示成功”。

### 4. token 成功持久化前未清旧 token

- [react-ui/src/pages/Portal/terminal.ts](E:/Urili-Ruoyi/react-ui/src/pages/Portal/terminal.ts:38)
  - `persistPortalLogin(...)` 仅在 `result.token` 存在且 `result.terminal === expectedTerminal` 时才写入当前 terminal token。
- [react-ui/src/pages/Portal/Login/index.tsx](E:/Urili-Ruoyi/react-ui/src/pages/Portal/Login/index.tsx:71)
  - portal 登录页只有在 `persistPortalLogin(...)` 返回 `true` 后才跳转，没有预清动作。
- [react-ui/src/pages/Portal/DirectLogin/index.tsx](E:/Urili-Ruoyi/react-ui/src/pages/Portal/DirectLogin/index.tsx:78)
  - direct-login 消费失败时只返回错误，不会先清当前 terminal 旧 token。
- [react-ui/tests/terminal-session-token.test.ts](E:/Urili-Ruoyi/react-ui/tests/terminal-session-token.test.ts:92)
  - 直接断言 portal login / direct-login 页源码不包含预清 token 的 `clearPortalLogin` 调用。
- [react-ui/src/requestErrorConfig.ts](E:/Urili-Ruoyi/react-ui/src/requestErrorConfig.ts:36)
  - direct-login 接口如果 401，也显式跳过 `clearTerminalSessionToken(...)`，避免因无效票据误清已有会话。

建议：维持现状；任何后续“登录前先清缓存”的优化都不应进入 portal 登录和 direct-login 成功链路。

### 5. 跨端响应已 fail-closed

- [react-ui/src/pages/Portal/DirectLogin/index.tsx](E:/Urili-Ruoyi/react-ui/src/pages/Portal/DirectLogin/index.tsx:33)
  - `isDirectLoginTokenMessage(...)` 要求消息 terminal 与当前页面 terminal 一致。
- [react-ui/src/pages/Portal/DirectLogin/index.tsx](E:/Urili-Ruoyi/react-ui/src/pages/Portal/DirectLogin/index.tsx:99)
  - 事件处理还要求 `event.source === window.opener`、`event.origin === openerOrigin`、且只能消费一次。
- [react-ui/src/pages/Portal/terminal.ts](E:/Urili-Ruoyi/react-ui/src/pages/Portal/terminal.ts:42)
  - 即使后端响应到了错误 terminal，`persistPortalLogin(...)` 也直接返回 `false`，不会写错端 token。
- [react-ui/tests/terminal-session-token.test.ts](E:/Urili-Ruoyi/react-ui/tests/terminal-session-token.test.ts:67)
  - 覆盖“seller 响应落到 buyer 页面”时直接拒绝且不清任一端 token。

建议：维持现状；后续若 direct-login payload 增字段，terminal/source/origin/ticketId 这几层 guard 不能删减。

### 6. 空 `authority` 已拒绝访问

- [react-ui/src/services/session.ts](E:/Urili-Ruoyi/react-ui/src/services/session.ts:95)
  - `normalizeAuthority(...)` 把空值归一成空数组。
- [react-ui/src/services/session.ts](E:/Urili-Ruoyi/react-ui/src/services/session.ts:112)
  - `RemoteMenuRouteGuard(...)` 只有 `permissions.length > 0` 且权限校验通过才允许渲染；空数组直接 403。
- [react-ui/src/services/session.ts](E:/Urili-Ruoyi/react-ui/src/services/session.ts:339)
  - 远程菜单 authority 来自后端 `perms`；缺失时会变成空数组，前端默认拒绝。
- [react-ui/src/wrappers/RemoteMenuRouteGuard.tsx](E:/Urili-Ruoyi/react-ui/src/wrappers/RemoteMenuRouteGuard.tsx:57)
  - portal 登录、direct-login、portal home 被明确列为公共路径，不会误套 seller/buyer admin fallback authority。
- [react-ui/tests/remote-menu-route-guard.test.ts](E:/Urili-Ruoyi/react-ui/tests/remote-menu-route-guard.test.ts:150)
  - 明确覆盖“远程菜单 route 没有 authority 时渲染 403”。

建议：维持现状；所有新增远程菜单 contract 继续要求后端返回 `perms`，不要把空 authority 解释成允许访问。

### 7. portal request 已使用端隔离 token，并剥离前端可注入 scope 参数

- [react-ui/src/services/portal/session.ts](E:/Urili-Ruoyi/react-ui/src/services/portal/session.ts:24)
  - `buildPortalAuthHeaders(...)` 只取当前 terminal token，不会 fallback 到 admin token。
- [react-ui/src/services/portal/session.ts](E:/Urili-Ruoyi/react-ui/src/services/portal/session.ts:29)
  - `sanitizePortalQueryParams(...)` 会剥掉 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId`、`terminal`。
- [react-ui/src/services/portal/session.ts](E:/Urili-Ruoyi/react-ui/src/services/portal/session.ts:39)
  - login / direct-login 请求统一 `isToken: false`，不会误带 admin 默认 Authorization。
- [react-ui/tests/portal-session-request.test.ts](E:/Urili-Ruoyi/react-ui/tests/portal-session-request.test.ts:211)
  - 覆盖 scope 参数剥离，以及 seller/buyer 全套 portal request 只带本端 token。

建议：维持现状；新增 portal service 时必须继续走 `buildPortalAuthHeaders(...) + sanitizePortalQueryParams(...)`。

## 验证记录

### 静态审计

- 已阅读：
  - `react-ui/src/requestErrorConfig.ts`
  - `react-ui/src/app.tsx`
  - `react-ui/src/utils/portalRequest.ts`
  - `react-ui/src/utils/portalPaths.ts`
  - `react-ui/src/utils/portalDirectLoginMessage.ts`
  - `react-ui/src/pages/Portal/terminal.ts`
  - `react-ui/src/pages/Portal/Login/index.tsx`
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/services/session.ts`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
  - `react-ui/config/routes.ts`

### guard 验证

- 命令：`node scripts/check-portal-token-isolation.mjs`
- 结果：通过

### 定向 Jest 验证

- 尝试命令 1：`npm exec jest -- tests/portal-unauthorized-redirect.test.ts tests/portal-session-request.test.ts tests/portal-direct-login-message.test.ts tests/terminal-session-token.test.ts tests/remote-menu-route-guard.test.ts --runInBand`
  - 结果：失败
  - 原因：Jest 隐式解析发现 `jest.config.js` 与 `jest.config.ts` 两份配置，需要显式指定 `--config`
- 尝试命令 2：`npm exec jest -- --config jest.config.ts tests/portal-unauthorized-redirect.test.ts tests/portal-session-request.test.ts tests/portal-direct-login-message.test.ts tests/terminal-session-token.test.ts tests/remote-menu-route-guard.test.ts --runInBand`
  - 结果：仍失败
  - 现象：多条用例报 `Could not locate module @umijs/max mapped as: E:/Urili-Ruoyi/react-ui/src/.umi-test/exports`
  - 说明：这是当前定向 Jest 入口的验证环境问题，不是本切片 portal auth/guard/request 业务逻辑漏洞；本次因此以静态审计 + 现有 guard 脚本为主证据

## 审计结论摘要

本切片要求关注的 6 条 P0/P1 主线，当前实现均表现为 fail-closed，没有看到 seller/buyer 401 串到 admin、portal redirect 白名单放开、direct-login 提前报成功、成功前清旧 token、跨端 token 落错端、或空 authority 放行的问题。

当前剩余风险不在业务实现，而在**定向 Jest 入口的可复验性**。若后续要把这类 portal 审计做成稳定回归门，建议单独收敛 Jest 入口问题，但这不影响本次对业务逻辑的 P0/P1 结论。
