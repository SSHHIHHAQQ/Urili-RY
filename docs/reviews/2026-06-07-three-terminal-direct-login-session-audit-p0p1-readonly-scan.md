# 2026-06-07 三端独立 direct-login / session / audit 只读扫描（P0/P1 快速模式）

## 结论

- 本轮按 `P0/P1` 快速模式，**未发现需要立即修复的 direct-login / session / audit 行为回归缺口**。
- 已重点核查以下方向：
  - 跨端票据消费
  - 旧 Redis key 兼容残留
  - `actingAdmin` 归属
  - 强退 / 重置密码后的强退审计
  - 端内自助日志泄漏

## P0 / P1 列表

### 无 P0

- 未发现会导致跨端会话串用、免密票据跨端落库、旧 Redis key 继续生效、端内自助日志直接泄漏后台审计字段的 P0 问题。

### 无 P1

- 未发现会导致 direct-login / session / audit 链路行为与当前三端独立约束明显偏离的 P1 回归。

## 关键核查点与证据

### 1. 跨端票据消费：当前实现为拒绝并避免外端审计落入当前端

- `PortalDirectLoginSupport` 在票据层先校验 terminal，不匹配直接拒绝：
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:182)
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:187)
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:189)
- seller/buyer 端 direct-login 失败日志写入时，对外端 token 不回填本端账号 ID：
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:1107)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:1110)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:1107)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:1110)

### 2. 旧 Redis key：当前实现只写新 key，消费失败时同时清理新旧 key

- 新 key 形状为 `portal_direct_login:{terminal}:{token_hash}`：
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:98)
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:356)
- 删除逻辑同时覆盖旧 key `portal_direct_login:{token_hash}`：
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:350)
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:353)
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:361)

### 3. `actingAdmin` 归属：当前实现区分“票据签发人”与“当前执行后台动作的人”

- direct-login 成功会把票据中的 `actingAdmin*` 写入 session / login log：
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:175)
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:188)
  - [RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:201)
- 后台强退 / 后台重置密码强退时，会用当前后台账号覆盖 `actingAdmin*`：
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:982)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:994)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:982)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:994)
- 端内自助改密强退显式关闭后台审计覆盖，不会把 portal 用户误记成后台管理员：
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:805)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:807)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:805)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:807)

### 4. 强退 / 重置密码强退：当前实现会落审计并删 Redis 会话

- 先查在线会话，再批量 `logout_time`，再补登录日志，再删 token：
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:926)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:929)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:937)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:926)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:929)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:937)

### 5. 端内自助日志泄漏：当前实现 DTO 收敛正确

- seller/buyer 端自助日志接口都映射为 `PortalOwn*Profile`，未直接返回内部审计模型：
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:330)
  - [RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:383)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:330)
  - [RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:383)
- `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile` / `PortalOwnSessionProfile` 序列化测试明确禁止输出 `subjectId/accountId/directLoginTicketId/actingAdmin*`：
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java:13)
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java:51)
  - [RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalHomeProfileSerializationTest.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalHomeProfileSerializationTest.java:92)

## 已有测试覆盖

### direct-login / Redis key / terminal 约束

- `PortalDirectLoginSupportTest`
  - 新 key 写入且不暴露旧 key：`createTokenShouldPersistHashedTicketAndHashKeyedRedisPayload`
  - 旧 key 不再可消费且会被清理：`consumeTokenShouldRejectLegacyRedisPayloadAndDeleteBothKeyShapes`
  - terminal 不匹配直接拒绝：`consumeTokenShouldRejectTerminalMismatchWithoutConsumingTicket`
  - 文件位置：[RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java:69)

### seller / buyer direct-login 跨端与审计

- `SellerServiceImplTest`
  - `directLoginSellerDoesNotWriteForeignTicketAuditIntoSellerLog`
  - `directLoginSellerUsesCurrentSellerAndAccountState`
  - `selectSellerOwnLoginLogListUsesSessionScopeAndIgnoresClientScope`
  - `selectSellerOwnOperLogListUsesSessionScopeAndIgnoresClientScope`
  - 文件位置：[RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java](E:/Urili-Ruoyi/RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java:798)
- `BuyerServiceImplTest`
  - `directLoginBuyerDoesNotWriteForeignTicketAuditIntoBuyerLog`
  - `directLoginBuyerUsesCurrentBuyerAndAccountState`
  - `selectBuyerOwnLoginLogListUsesSessionScopeAndIgnoresClientScope`
  - `selectBuyerOwnOperLogListUsesSessionScopeAndIgnoresClientScope`
  - 文件位置：[RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java](E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java:798)

### contract / serialization

- `PortalSelfServiceSurfaceContractTest`
  - 限制 seller/buyer 自助服务面只能返回 `PortalOwn*Profile`
- `PortalDirectLoginAuthContractTest`
  - 校验 direct-login 审计字段贯通存在
- `PortalLoginSessionConsistencyContractTest`
  - 校验 session mapper 映射 direct-login / actingAdmin 字段

## 最小修复建议

当前 `P0/P1` 档位下，**不建议直接改代码**。如果要继续降残余风险，建议只做以下最小补强：

1. 给强退 / 后台重置密码强退各补一条显式测试：
   - 断言 direct-login 会话被强退后，`directLoginTicketId` 仍保留；
   - 同时 `actingAdminId/actingAdminName` 被当前执行后台动作的人覆盖。
2. 给 seller/buyer portal 自助改密各补一条测试：
   - 断言 `PASSWORD_RESET_FORCE_LOGOUT` 会写日志；
   - 且不会写入后台 `actingAdmin*`。
3. 如果后续进入非快速模式，再加一条浏览器级联调：
   - admin 发 seller 票据到 buyer `/direct-login` 页面；
   - 断言页面失败、buyer 端不落外端审计、seller 票据状态符合预期。

## 本轮未执行

- 未修改任何业务代码。
- 未执行数据库 / Redis 实连验证。
- 未实际跑测试，只做了代码与测试覆盖的只读核查。
