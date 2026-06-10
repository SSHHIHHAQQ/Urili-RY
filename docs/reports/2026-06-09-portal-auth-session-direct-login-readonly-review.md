# 2026-06-09 portal auth/session/direct-login/401/日志自助 DTO 只读复核

## 范围

- 后端：`RuoYi-Vue/ruoyi-system` portal/support 相关代码与测试；补充核对 `seller` / `buyer` portal 自助控制器与 service 实现。
- 前端：`react-ui/src/pages/Portal`、`react-ui/src/utils/portal*`、`react-ui/src/requestErrorConfig.ts`、`react-ui/src/app.tsx`
- 测试：`react-ui/tests/terminal-session-token.test.ts`、`portal-session-request.test.ts`、`portal-direct-login-message.test.ts`、`portal-unauthorized-redirect.test.ts`

## 已验证边界

- 仅做静态代码与测试复核。
- 未运行浏览器、数据库、Redis、后端、前端。
- 结论基于当前工作区源码，不等于现场运行态验收。

## P0

未发现。

## P1

未发现。

## 关键证据

1. token / Redis key 没有串端
   - 前端本地 token key 已按端隔离：`react-ui/src/access.ts:30-49`
   - portal 登录只在响应 terminal 与页面 terminal 一致时才落 token：`react-ui/src/pages/Portal/terminal.ts:38-48`
   - portal 请求只取当前端 token，不回退 admin token：`react-ui/src/services/portal/session.ts:24-27`
   - 后端 JWT claim 与 Redis session key 同时带 terminal：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:61-80`, `130-153`, `263-265`
   - direct-login payload Redis key 使用 `portal_direct_login:{terminal}:{token_hash}`，且仅读 scoped key、不读 legacy key：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:98`, `243-245`, `351-364`
   - 对应测试已覆盖：`react-ui/tests/terminal-session-token.test.ts:25-53`, `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java:100-104`, `173-196`

2. direct-login 跨端误消费当前是 fail-closed
   - 前端 popup bridge 同时校验 `origin + source + terminal + ticketId`：`react-ui/src/utils/portalDirectLoginMessage.ts:71-96`, `146-163`
   - direct-login 页面只接受当前 terminal 的 token message：`react-ui/src/pages/Portal/DirectLogin/index.tsx:33-45`, `98-109`
   - 后端消费时同时校验票据 terminal、payload terminal、ticketId、subject/account 目标一致性：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:179-240`
   - wrong-terminal 场景不会消费真实 scoped payload，也不会把外端 payload 交给失败审计：`RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java:321-355`
   - seller/buyer 失败审计对外端 token 退化为无主体上下文失败记录，不会拿外端票据反查当前端账号：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:743-751`, `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:743-751`

3. 401 清 token 目前按端隔离，direct-login 401 不会误清已有 portal token
   - 统一错误处理按请求 URL 判定 terminal；portal 非 admin API 只清对应端 token：`react-ui/src/requestErrorConfig.ts:33-45`
   - app 侧请求/启动阶段的 401 处理逻辑一致：`react-ui/src/app.tsx:73-92`, `353-358`
   - direct-login API 401 直接 return，不清 portal token：`react-ui/src/requestErrorConfig.ts:35-41`, `react-ui/src/app.tsx:75-81`
   - 对应测试已覆盖 seller/buyer portal 401、admin 前缀 401、direct-login 401：`react-ui/tests/portal-unauthorized-redirect.test.ts:89-179`, `181-280`

4. portal 自助日志 / 会话 DTO 当前没有泄露内部审计字段
   - 自助 DTO 本身不含 `subjectId/accountId/tokenId/directLoginTicketId/actingAdmin*` 等内部字段：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnLoginLogProfile.java:14-29`, `PortalOwnOperLogProfile.java:14-35`, `PortalOwnSessionProfile.java:14-30`
   - seller/buyer service 自助映射只拷贝白名单字段：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:384-460`, `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:384-460`
   - JSON 序列化测试明确断言不返回内部审计字段：`RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java:12-60`
   - 架构测试明确禁止 controller/service 把内部字段拷到 portal 自助 DTO：`RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalSelfServiceSurfaceContractTest.java:86-215`

5. 前端当前没有把 sellerId / buyerId 当成 portal 自助数据范围信任源
   - service 层统一剥离 `sellerId/buyerId/subjectId/accountId/...`：`react-ui/src/services/portal/session.ts:10-35`
   - seller/buyer 自助日志、会话、商品列表请求都走该剥离：`react-ui/src/services/portal/session.ts:124-155`, `177-235`
   - 前端测试已覆盖 scope param stripping：`react-ui/tests/portal-session-request.test.ts:260-319`
   - 后端最终仍以 session 回填主体/账号范围：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:163-193`, `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:535-562`; `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:167-193`, `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:535-562`

## P2

1. portal 自助 controller 仍然直接绑定内部查询模型，当前靠 service 白名单重建兜底，后续有漂移风险
   - 证据：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:167-182`, `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:167-182`
   - 当前为什么不是 P0/P1：
     - 前端已剥离身份参数；
     - service 已重建白名单 query，并强制回填 `session.getSubjectId()/getAccountId()`：`SellerServiceImpl.java:535-562`, `BuyerServiceImpl.java:535-562`
   - 最小修复建议：
     - 后续把 `PortalLoginLog` / `PortalOperLog` 绑定参数替换成专用 query DTO，只保留 `userName/ipaddr/status/title/operName/status + beginTime/endTime` 这类白名单字段；
     - 保留现有架构测试，再新增“controller 不再直接绑定 PortalLoginLog/PortalOperLog”静态契约。

2. portal 401 处理逻辑在 `requestErrorConfig.ts` 与 `app.tsx` 重复维护，存在未来一边修了另一边漏修的漂移风险
   - 证据：`react-ui/src/requestErrorConfig.ts:10-45`, `react-ui/src/app.tsx:24-93`
   - 当前为什么不是 P0/P1：
     - 两处逻辑当前一致；
     - 相关测试已经覆盖 request error handler、response interceptor、startup/getInitialState 分支：`react-ui/tests/portal-unauthorized-redirect.test.ts:61-439`
   - 最小修复建议：
     - 抽一个共享 helper，例如 `src/utils/portalUnauthorized.ts`；
     - `requestErrorConfig.ts` 与 `app.tsx` 只保留薄封装调用，避免 direct-login 401 特判、portal/admin 分流、redirect 拼接未来发生分叉。

## 结论

- 当前范围内未看到你点名的 P0/P1 问题。
- 现状更接近“portal auth/session/direct-login/401/自助 DTO 合同已经被实现和测试同时钉住”。
- 剩余主要风险是 P2 级的绑定面和重复逻辑漂移，不是当前的串端或越权缺口。
