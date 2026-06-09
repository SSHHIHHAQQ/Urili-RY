# react-ui portal/token/request/401/direct-login 只读审计

- 审计时间：2026-06-09
- 审计范围：`react-ui` 中 seller/buyer portal 登录、direct-login 消费、token 持久化、request 拦截、401 分流、redirect 白名单、proxy guard、JS/TS 镜像
- 审计方式：只读代码审计 + 现有 guard/测试执行；未改源码，未跑浏览器

## 结论

本次范围内，**未坐实**以下 P0/P1：

1. 跨端 token 清理
2. 旧 `portal_direct_login:{token_hash}` Redis key 前端依赖
3. portal 客户端向 seller/buyer 端接口透传 `sellerId` / `buyerId` / `accountId`
4. proxy rewrite 未被 guard 固定
5. JS/TS 镜像分叉导致 guard 失效

## 可坐实证据

### 1. token 持久化与清理按端隔离，未发现跨端清理

- `react-ui/src/access.ts` 为 `admin` / `seller` / `buyer` 定义了独立 token key：
  - `seller_access_token` / `seller_refresh_token` / `seller_expireTime`
  - `buyer_access_token` / `buyer_refresh_token` / `buyer_expireTime`
  - 证据：`react-ui/src/access.ts:30-49`
- `clearTerminalSessionToken(terminal)` 只按传入 terminal 删除本端 key，不会联动删除另一端：
  - 证据：`react-ui/src/access.ts:87-93`
- `persistPortalLogin(...)` 只有在 `result.terminal === expectedTerminal` 时才落 token；终端不匹配直接返回 `false`，未见任何“先清旧 token 再重登”的逻辑：
  - 证据：`react-ui/src/pages/Portal/terminal.ts:38-49`
- portal 登录页和 direct-login 消费页均未调用 `clearPortalLogin` 预清理旧 token：
  - 证据：`react-ui/src/pages/Portal/Login/index.tsx:67-75`
  - 证据：`react-ui/src/pages/Portal/DirectLogin/index.tsx:75-95`
- `Portal/Home` 中 `clearPortalLogin(terminal)` 只出现在显式退出 `handleLogout` 的 `finally`，不在 401/直登失败链路里：
  - 证据：`react-ui/src/pages/Portal/Home/index.tsx:251-262`

### 2. direct-login 消费链路未发现旧 Redis key 前端依赖

- `react-ui/src/pages/Portal/DirectLogin/index.tsx` 通过 `postMessage` 收一次性 token，再调用 `/api/{terminal}/direct-login`，没有读取 URL token、localStorage token 或任何 Redis key 名：
  - 证据：`react-ui/src/pages/Portal/DirectLogin/index.tsx:63-109`
- `react-ui/src/utils/portalDirectLoginMessage.ts` 负责 opener/popup 的消息桥，只处理 `openerOrigin`、READY、TOKEN、RESULT 三类消息：
  - 证据：`react-ui/src/utils/portalDirectLoginMessage.ts:1-24`
  - 证据：`react-ui/src/utils/portalDirectLoginMessage.ts:98-150`
- 对 `react-ui/src` / `react-ui/config` / `react-ui/tests` 执行 `rg -n "portal_direct_login"`，**无匹配结果**。说明前端实现层没有显式依赖旧 `portal_direct_login:*` key。

### 3. portal 客户端未向端内接口透传 sellerId/buyerId/accountId

- `react-ui/src/services/portal/session.ts` 定义 `PORTAL_SCOPE_PARAM_KEYS`，明确剔除：
  - `sellerId`
  - `buyerId`
  - `subjectId`
  - `accountId`
  - `sellerAccountId`
  - `buyerAccountId`
  - `terminal`
  - 证据：`react-ui/src/services/portal/session.ts:10-18`
- `sanitizePortalQueryParams(params)` 在 portal audit / session / product list 请求前过滤上述字段：
  - 证据：`react-ui/src/services/portal/session.ts:29-35`
- portal 所有请求都通过 `buildPortalAuthHeaders(terminal)` 从本端 token 注入 `Authorization`，没有回退到 admin token：
  - 证据：`react-ui/src/services/portal/session.ts:24-27`
- portal 登录和 direct-login 请求体只发送：
  - 登录：`username` / `password`
  - 直登：`directLoginToken`
  - 证据：`react-ui/src/services/portal/session.ts:38-57`

### 4. 401 分流与 redirect 白名单保持 fail-closed

- API URL 分类会排除 `/api/seller/admin/**` 和 `/api/buyer/admin/**`，仅将非 admin seller/buyer API 识别为 portal 请求：
  - 证据：`react-ui/src/utils/portalRequest.ts:5-16`
  - 证据：`react-ui/src/utils/portalRequest.ts:29-50`
- portal 路由白名单只接受 `/{terminal}/login`、`/{terminal}/direct-login`、`/{terminal}/portal/**`：
  - 证据：`react-ui/src/utils/portalPaths.ts:5-27`
- portal 登录页 `resolveRedirect(...)` 会拒绝：
  - 非当前 terminal 的 portal 路径
  - login 自身
  - direct-login 页面
  - 不满足时回退到 `PORTAL_META[terminal].homePath`
  - 证据：`react-ui/src/pages/Portal/Login/index.tsx:38-49`
- request 错误处理对 portal 401 的处理是：
  - 先按 URL 判断 terminal
  - direct-login API 401 不清 token
  - 其它 portal 401 只清命中的 terminal token，并跳本端 login
  - 非 portal 走 admin 登录流程
  - 证据：`react-ui/src/requestErrorConfig.ts:33-45`
  - 证据：`react-ui/src/app.tsx:73-92`
  - 证据：`react-ui/src/app.tsx:353-359`

### 5. direct-login opener/popup 消息桥约束仍在

- popup 只接受来自 `window.opener` 且 `event.origin === openerOrigin` 的 TOKEN 消息：
  - 证据：`react-ui/src/pages/Portal/DirectLogin/index.tsx:98-108`
- admin 侧桥接代码只在收到目标页 READY 后才发送 token，并且要求 RESULT 的 terminal、ticketId 都匹配才 resolve：
  - 证据：`react-ui/src/utils/portalDirectLoginMessage.ts:136-150`
- 管理端调用方等待 portal 端消费确认后才提示成功，不是拿到 ticket 就报成功：
  - 证据：`react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:559-569`

### 6. proxy rewrite 与 JS/TS 镜像已被 guard 固定

- `config/proxy.ts` 仍保留：
  - `API_PROXY_TARGET` 可覆盖
  - `dev['/api/'].target = apiProxyTarget`
  - `changeOrigin: true`
  - `pathRewrite: { '^/api': '' }`
  - 证据：`react-ui/config/proxy.ts:12-25`
- `config/proxy.js` 只是纯转发到 `proxy.ts`，未见第二份分叉逻辑：
  - 证据：`react-ui/config/proxy.js:1`
- 本次抽查的 portal 相关 JS 镜像均为纯 re-export：
  - `src/pages/Portal/DirectLogin/index.js`
  - `src/pages/Portal/Login/index.js`
  - `src/pages/Portal/Home/index.js`
  - `src/pages/Portal/terminal.js`
  - `src/services/portal/session.js`
  - `src/utils/portalRequest.js`
  - `src/utils/portalPaths.js`
  - `src/utils/portalDirectLoginMessage.js`
  - `src/requestErrorConfig.js`
  - `src/app.js`
  - `config/proxy.js`
  - `config/routes.js`

## 自动化验证结果

### guard

- 执行：`node scripts/check-portal-token-isolation.mjs`
- 结果：`Portal token isolation guard passed.`

该 guard 当前覆盖了本次关心的几类风险：

- portal 登录 / direct-login 不得预清 token
- direct-login 必须走 postMessage，不得从 URL 读 token
- portal/admin 401 必须按 terminal 分流
- `proxy.ts`/`proxy.js` 与其它 JS/TS 镜像必须保持 guard 约束

### 测试

- 执行：`npx jest --config jest.config.js tests/terminal-session-token.test.ts tests/portal-session-request.test.ts tests/portal-unauthorized-redirect.test.ts tests/portal-direct-login-message.test.ts --runInBand`
- 结果：`4 passed, 55 passed`

关键覆盖点：

- `tests/terminal-session-token.test.ts`
  - 验证 seller/buyer/admin token key 独立
  - 验证终端不匹配时不落错端 token、不清现有 token
  - 证据：`react-ui/tests/terminal-session-token.test.ts:25-101`
- `tests/portal-session-request.test.ts`
  - 验证 portal 请求只使用对应 terminal token
  - 验证调用方传入的 `sellerId` / `buyerId` / `accountId` / `terminal` 会被剔除
  - 证据：`react-ui/tests/portal-session-request.test.ts:234-319`
- `tests/portal-unauthorized-redirect.test.ts`
  - 验证 portal 401 只清命中的 terminal token
  - 验证 direct-login 401 不清现有 portal token
  - 验证 `/api/seller/admin/**`、`/api/buyer/admin/**` 仍回 admin 登录流
  - 证据：`react-ui/tests/portal-unauthorized-redirect.test.ts:89-179`
  - 证据：`react-ui/tests/portal-unauthorized-redirect.test.ts:233-260`
- `tests/portal-direct-login-message.test.ts`
  - 验证只有 READY 后才发送 token
  - 验证错误 origin / 错误 source / 错误 terminal / 错误 ticketId 都不会误确认
  - 证据：`react-ui/tests/portal-direct-login-message.test.ts:30-113`

## 覆盖边界

本次只读审计**已覆盖**：

- `react-ui/src/pages/Portal/**`
- `react-ui/src/services/portal/session.ts`
- `react-ui/src/utils/portalPaths.ts`
- `react-ui/src/utils/portalRequest.ts`
- `react-ui/src/utils/portalDirectLoginMessage.ts`
- `react-ui/src/requestErrorConfig.ts`
- `react-ui/src/app.tsx`
- `react-ui/config/proxy.ts`
- portal 相关 JS 镜像文件
- 对应 guard 与 Jest 测试

本次**未覆盖**：

- 后端 direct-login Redis key 读写实现
- seller/buyer 后端从 token 推导主体/账号的服务端约束
- 浏览器真实运行态、跨窗口行为、网络层实际响应

因此，本结论仅说明：**在 `react-ui` 当前源码、镜像和 guard/测试层面，没有坐实你列出的 P0/P1。**
