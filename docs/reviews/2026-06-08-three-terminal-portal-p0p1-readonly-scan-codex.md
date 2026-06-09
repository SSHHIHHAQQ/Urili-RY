# 2026-06-08 三端 portal auth/direct-login/session/log P0/P1 只读扫描

## 范围

- 后端：
  - `RuoYi-Vue/ruoyi-system` portal 支撑
  - `RuoYi-Vue/seller` / `RuoYi-Vue/buyer` portal controllers / services
- 前端：
  - `react-ui/src/pages/Portal`
  - `react-ui/src/requestErrorConfig.ts`
  - `react-ui/src/app.tsx`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/utils/portalPaths.ts`
  - `react-ui/src/utils/portalRequest.ts`
  - `react-ui/src/utils/portalDirectLoginMessage.ts`
- 测试 / guard：
  - `react-ui/tests/portal-unauthorized-redirect.test.ts`
  - `react-ui/tests/terminal-session-token.test.ts`
  - `react-ui/tests/portal-session-request.test.ts`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalSelfServiceSurfaceContractTest.java`

## 结论

- 本轮未发现新的确定 **P0/P1**。
- 你点名的 5 个检查项，当前 worktree 内代码和合同测试都已对齐：
  1. direct-login Redis key 已 terminal scoped
  2. terminal mismatch 不写入外端票据上下文
  3. 登录 / 直登失败前不清 token
  4. 401 按 seller / buyer portal 隔离并 reject
  5. 自助日志 DTO 不暴露内部审计字段

## P0 / P1

### 未发现新的确定 P0 / P1

#### 1. direct-login Redis key 已按 terminal 隔离

- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:98`
  - 写入 Redis 使用 `cacheKey(portalType, tokenHash)`
- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:244`
  - 读取 payload 只读 `cacheKey(portalType, tokenHash)`
- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:352-353`
  - 旧 key 只在删除历史残留时一起清理，不参与读取
- 合同 / 单测：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java:75`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java:70`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java:174`

#### 2. terminal mismatch 不写外端票据上下文

- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:189`
  - ticket terminal mismatch 直接抛 `免密登录票据端类型不匹配`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:733-749`
  - seller failure auditor 只在 `token.getPortalType() == "seller"` 时写 direct-login 结构化失败日志
  - `免密登录票据端类型不匹配` 已纳入“无账号上下文失败”分支
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:733-749`
  - buyer 端同样处理
- 单测：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java:321`
    - mismatch 时不消费票据、不走 failure auditor

#### 3. 登录 / 直登失败前不清 token

- `react-ui/src/pages/Portal/terminal.ts:42-47`
  - `persistPortalLogin(...)` 仅在 `result.token` 存在且 `result.terminal === expectedTerminal` 时写入当前端 token
  - mismatch 直接 `return false`
- `react-ui/src/pages/Portal/Login/index.tsx:71-72`
  - 只有 `persistPortalLogin(...)` 成功后才跳转
- `react-ui/tests/terminal-session-token.test.ts:75`
  - 跨端登录结果不会清已有 token
- `react-ui/tests/terminal-session-token.test.ts:92`
  - 登录页 / 直登页源码合同固定“成功前不得 clearPortalLogin”

#### 4. 401 已按 seller / buyer portal 隔离并 reject

- `react-ui/src/requestErrorConfig.ts:36-37`
  - portal 401 只清对应 terminal token，并跳对应 portal login
- `react-ui/src/requestErrorConfig.ts:89-91`
  - BizError 401 处理后继续 `throw error`
- `react-ui/src/requestErrorConfig.ts:119-121`
  - HTTP 401 处理后继续 `throw error`
- `react-ui/src/app.tsx:76-77`
  - 运行时同样按 terminal 分流
- `react-ui/src/app.tsx:354`
  - body-level 401 处理后 `return Promise.reject(response)`
- 测试：
  - `react-ui/tests/portal-unauthorized-redirect.test.ts:89`
  - `react-ui/tests/portal-unauthorized-redirect.test.ts:127`

#### 5. 自助日志 / 会话 DTO 未泄露内部审计字段

- DTO 面：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnLoginLogProfile.java:10`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnOperLogProfile.java:10`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnSessionProfile.java:10`
  - 这 3 个 DTO 本身不包含 `subjectId/accountId/tokenId/directLoginTicketId/actingAdmin*/directLoginReason/operParam/jsonResult`
- service 投影面：
  - seller:
    - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:330-365`
    - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:383-449`
  - buyer:
    - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:330-365`
    - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:383-449`
- 序列化测试：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java:23`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java:42`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java:51-59`

#### 6. portal 查询参数已过滤 caller-controlled scope

- `react-ui/src/services/portal/session.ts:10-34`
  - `PORTAL_SCOPE_PARAM_KEYS` + `sanitizePortalQueryParams(...)`
- 已覆盖日志 / 会话 / seller / buyer portal 商品列表：
  - `react-ui/src/services/portal/session.ts:124-152`
  - `react-ui/src/services/portal/session.ts:177-183`
  - `react-ui/src/services/portal/session.ts:228-234`
- 测试：
  - `react-ui/tests/portal-session-request.test.ts:254`
  - `react-ui/tests/portal-session-request.test.ts:279`

## P2

### 1. direct-login 页面在“未收到 token”超时分支没有主动回传 RESULT error

- 文件：
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx:113-115`
  - `react-ui/src/utils/portalDirectLoginMessage.ts:172`
- 原因：
  - popup 页本地 5 秒超时只更新自身错误态，没有 `postConsumeResult('error', ...)` 回传给 opener。
  - 管理端目前仍会依赖 bridge 层 15 秒 `DIRECT_LOGIN_READY_TIMEOUT / DIRECT_LOGIN_CONSUME_TIMEOUT` 收敛，所以不是当前 P0/P1，但会拉长失败反馈时间。
- 最小修复：
  - 在 `DirectLogin/index.tsx` 的 timeout 分支补一条 `postConsumeResult('error', undefined, 'Direct login token was not received')`
  - 同步补一个前端单测，固定 opener 能更快收到失败结果

### 2. 401 分流逻辑在 `app.tsx` 与 `requestErrorConfig.ts` 存在重复实现

- 文件：
  - `react-ui/src/requestErrorConfig.ts:30-40`
  - `react-ui/src/app.tsx:70-80`
- 原因：
  - 当前行为一致，也有测试守住；但双份分流逻辑后续容易出现一处升级、一处回退。
- 最小修复：
  - 抽一个共享 helper，例如 `handleUnauthorizedPortalOrAdmin(requestUrl)`
  - `app.tsx` 和 `requestErrorConfig.ts` 都复用同一实现

## 备注

- 本轮严格按要求做只读扫描，没有改业务代码，也没有做浏览器 / UI 验证。
- 当前 worktree 存在大量用户未提交改动；本报告只基于静态代码和现有测试合同给出结论。
