# 三端 Portal 登录 / Direct-Login / Token / Session P0-P1 只读扫描

- 日期：2026-06-08
- 范围：`E:\Urili-Ruoyi\react-ui`、`E:\Urili-Ruoyi\RuoYi-Vue`
- 方式：只读扫描，不改业务代码；本文件仅记录审查结论
- 目标：专查 Portal 登录、direct-login、token/session、Redis key、401 处理、前端端隔离路径中的串端、旧 key 依赖、成功提示误判、清错 token 等 P0/P1

## 结论

### P0

- **未发现明确 P0 缺口。**

### P1

- **未发现明确 P1 缺口。**

当前主链路已经具备以下隔离与 fail-closed 约束：

1. Portal token、本地存储 key、Redis key 按 terminal 分槽。
2. direct-login 只接受 POST body，不接受 URL query token。
3. direct-login 成功提示依赖 popup 端回传 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 成功消息，不会在只收到 READY 或仅创建链接时提前报成功。
4. Portal 401 只清当前 terminal token；`/api/seller/admin/**`、`/api/buyer/admin/**` 仍走 admin 登录流。
5. Portal 路由白名单仅覆盖 `/{terminal}/login`、`/{terminal}/direct-login`、`/{terminal}/portal/**`，不会把 `/seller`、`/buyer` 管理端路径误判为 portal。
6. direct-login Redis payload 只读取新 key `portal_direct_login:{terminal}:{token_hash}`，没有读取旧无 terminal key。

## P0/P1 证据

### 1. 前端 token / redirect 已按 terminal 隔离

- 文件：
  - `E:\Urili-Ruoyi\react-ui\src\access.ts`
  - `E:\Urili-Ruoyi\react-ui\src\requestErrorConfig.ts`
  - `E:\Urili-Ruoyi\react-ui\src\app.tsx`
  - `E:\Urili-Ruoyi\react-ui\src\utils\portalRequest.ts`
  - `E:\Urili-Ruoyi\react-ui\src\utils\portalPaths.ts`
- 证据：
  - `react-ui/src/access.ts:30-49`
    - admin / seller / buyer 使用不同 storage key：`access_token`、`seller_access_token`、`buyer_access_token` 等。
  - `react-ui/src/access.ts:87-93`
    - `clearTerminalSessionToken(terminal)` 只删除当前 terminal 的 token / refresh / expireTime / user。
  - `react-ui/src/requestErrorConfig.ts:33-42`
    - `handleUnauthorized` 先按 `getPortalTerminalFromApiUrl(requestUrl)` 判断是否 portal 请求；portal 只清当前端 token，否则才清 admin token。
  - `react-ui/src/app.tsx:73-90`
    - `handleUnauthorizedResponse` 同样按 API URL 推导 terminal，portal/admin 分流清理。
  - `react-ui/src/utils/portalRequest.ts:33-43`
    - `/api/seller/admin/**`、`/api/buyer/admin/**` 被显式排除，不会被判成 portal API。
  - `react-ui/src/utils/portalPaths.ts:23-29,31-46`
    - Portal 页面路径只承认 `/{terminal}/login`、`/{terminal}/direct-login`、`/{terminal}/portal` 三类前缀；`/seller`、`/buyer` 不算 portal。
- 结论：
  - 本轮未发现 portal/admin 401 串端清 token，也未发现 `/seller`、`/buyer` 被误当成 portal 路径的 P0/P1。
- 建议修复：
  - **当前无需 P0/P1 修复。**

### 2. direct-login 成功提示没有“只收到 READY 就报成功”的误判

- 文件：
  - `E:\Urili-Ruoyi\react-ui\src\utils\portalDirectLoginMessage.ts`
  - `E:\Urili-Ruoyi\react-ui\src\pages\Portal\DirectLogin\index.tsx`
  - `E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx`
  - `E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerAccountModal.tsx`
- 证据：
  - `react-ui/src/utils/portalDirectLoginMessage.ts:150-163`
    - popup 端先发 READY，主窗口只在收到 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 且 `status === 'success'` 时才 `resolveBridge(...)`。
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx:77-88`
    - popup 页只有在 `response.code === 200` 且 `persistPortalLogin(response.data, terminal)` 成功后，才 `postConsumeResult('success', ...)` 并跳转首页。
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:617-623`
    - 管理端“免密登录已确认”成功提示建立在 `await openPortalDirectLoginWindow(...)` 返回成功之上。
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:564-569`
    - 账号级 direct-login 也是同样的 success 判定。
- 结论：
  - 本轮未发现“只发出 token”或“只收到 READY”就提前提示成功的 P0/P1。
- 建议修复：
  - **当前无需 P0/P1 修复。**

### 3. direct-login Redis payload 已切到 terminal-scoped key，未读旧 key

- 文件：
  - `E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java`
  - `E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\PortalDirectLoginAuthContractTest.java`
- 证据：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:98`
    - payload 写入 `cacheKey(portalType, tokenHash)`。
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:242-245`
    - payload 读取只走 `redisCache.getCacheObject(cacheKey(portalType, tokenHash))`。
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:350-363`
    - 旧 key `portal_direct_login:{token_hash}` 只在删除逻辑里作为清理对象存在，没有读取依赖。
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java:75-94`
    - 契约测试显式禁止读取 `legacyCacheKey(tokenHash)`。
- 结论：
  - 本轮未发现 runtime 仍依赖旧无 terminal Redis key 的 P0/P1。
- 建议修复：
  - **当前无需 P0/P1 修复。**

### 4. 后端 portal session / JWT / Redis token 均按 terminal 绑定

- 文件：
  - `E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalTokenSupport.java`
  - `E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java`
  - `E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java`
  - `E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalSessionContext.java`
- 证据：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:66-75`
    - tokenId 带 terminal 前缀，JWT claims 同时写入 `portal_login_key` 与 `portal_terminal`。
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:130-158`
    - 取 session 时同时校验 `expectedTerminal == claims.portal_terminal` 且 Redis session 自身 terminal 一致。
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:214-227`
    - 删除 token 时按传入 terminal 拼 Redis key。
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:674-706,929-938`
    - seller direct-login / forceLogout 全部使用 `"seller"` 固定 terminal。
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:674-706,929-938`
    - buyer direct-login / forceLogout 全部使用 `"buyer"` 固定 terminal。
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalSessionContext.java:29-47`
    - controller 层 `requireSession(expectedTerminal)` 仍有第二层 terminal 校验。
- 结论：
  - 本轮未发现 seller/buyer portal session 取错端、删错端或强退删错 Redis token 的 P0/P1。
- 建议修复：
  - **当前无需 P0/P1 修复。**

### 5. direct-login API 入口未退回 URL query token 模式

- 文件：
  - `E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\SellerPortalAuthController.java`
  - `E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\controller\BuyerPortalAuthController.java`
  - `E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\PortalDirectLoginAuthContractTest.java`
- 证据：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java:33-44`
    - `@PostMapping("/direct-login")` 且从 `@RequestBody Map<String, String>` 读取 `directLoginToken`。
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalAuthController.java:33-44`
    - buyer 同构实现。
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java:17-42`
    - 契约测试显式禁止 `@GetMapping("/direct-login")` 和 `@RequestParam("directLoginToken")`。
- 结论：
  - 本轮未发现 direct-login token 回流到 URL query/hash 的 P0/P1。
- 建议修复：
  - **当前无需 P0/P1 修复。**

## P2 单列

### P2-1 `PortalDirectLoginSupport` 运行时代码仍保留 legacy key 清理分支

- 文件：
  - `E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java`
- 证据：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:350-363`
    - `deletePayloadCacheKeys(...)` 同时删除新 key 和旧 key：
      - `portal_direct_login:{terminal}:{token_hash}`
      - `portal_direct_login:{token_hash}`
- 影响判断：
  - 这不是当前 P0/P1，因为代码没有读取旧 key。
  - 但 runtime 仍然显式知道 legacy key 命名，后续如果有人回滚实现、手抄老逻辑或做“兼容读取”，容易把旧 key 重新带回主链路。
- 建议修复：
  - 等远端 Redis 历史残留完成清点后，把 legacy key 清理从主运行链路下沉到一次性迁移/巡检脚本，避免业务代码继续持有旧 key 命名知识。
  - 至少保留当前契约测试，继续禁止 `getCacheObject(legacyCacheKey(...))`。

### P2-2 direct-login popup 超时错误目前只在管理端落通用失败提示

- 文件：
  - `E:\Urili-Ruoyi\react-ui\src\utils\portalDirectLoginMessage.ts`
  - `E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx`
  - `E:\Urili-Ruoyi\react-ui\src\components\PartnerManagement\PartnerAccountModal.tsx`
- 证据：
  - `react-ui/src/utils/portalDirectLoginMessage.ts:170-173`
    - popup 15 秒未 READY / 未消费完成时，分别 reject `DIRECT_LOGIN_READY_TIMEOUT`、`DIRECT_LOGIN_CONSUME_TIMEOUT`。
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:625-629`
    - 上层 catch 统一提示“免密登录链接生成失败，请重试”。
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:571-576`
    - 账号级入口同样只给通用失败提示。
- 影响判断：
  - 这不是串端或清错 token 问题，因此不归 P0/P1。
  - 但排查 popup 被浏览器拦截、portal 页未回传成功消息、跨域 opener 丢失时，现场信号偏弱。
- 建议修复：
  - 后续可把 `DIRECT_LOGIN_READY_TIMEOUT`、`DIRECT_LOGIN_CONSUME_TIMEOUT`、`DIRECT_LOGIN_POPUP_CLOSED` 映射成更可诊断的提示文案，便于一线排障。

## 建议

1. 当前不建议再按“发现 P0/P1 缺口”开修复单，这条链路的主隔离面已经基本补齐。
2. 后续若继续做回归，优先回归以下契约：
   - `react-ui/tests/portal-unauthorized-redirect.test.ts`
   - `react-ui/tests/portal-session-request.test.ts`
   - `react-ui/tests/terminal-session-token.test.ts`
   - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java`
3. 若你要继续压缩 residual risk，下一轮最值得看的不是 Portal 主链路，而是围绕 direct-login 的管理端审计、popup 失败诊断和 legacy key 清理退出策略。
