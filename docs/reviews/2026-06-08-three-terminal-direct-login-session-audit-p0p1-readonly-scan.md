# 2026-06-08 三端独立登录/免密代入/session/audit P0/P1 只读扫描

## 扫描结论

本轮按用户指定范围只读检查 `login`、`direct-login`、`session`、`login_log`、`oper_log`、端内自助日志 DTO 链路，**未发现可确定的 P0/P1 问题**。

下面只保留能直接支撑该结论的代码证据，便于继续推进三端独立收尾。

## 已确认无问题的重点项

### 1. 免密跨端审计污染

- `PortalDirectLoginSupport.consumeToken(...)` 先校验票据 `terminal`，端不匹配直接抛错，不消费票据，也不会回填异端 payload：
  - [PortalDirectLoginSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java:182)
  - [PortalDirectLoginSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java:187)
- seller/buyer 端在失败审计时都额外做了 terminal guard。若 token 的 `portalType` 不是当前端，只记匿名失败，不把外端 `ticketId / actingAdmin / reason / target account` 写入当前端日志：
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:733)
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:735)
  - [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:733)
  - [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:735)
- 对应单测已明确覆盖“foreign ticket 不写入当前端审计”：
  - [SellerServiceImplTest.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\test\java\com\ruoyi\seller\service\impl\SellerServiceImplTest.java:966)
  - [PortalDirectLoginSupportTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\service\support\PortalDirectLoginSupportTest.java:325)

### 2. Redis key 是否仍依赖旧 `portal_direct_login:{token_hash}`

- 新写入只使用带 terminal 的新 key：`portal_direct_login:{terminal}:{token_hash}`：
  - [PortalDirectLoginSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java:98)
  - [PortalDirectLoginSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java:356)
- 读取只查新 key，不回退读取旧 key：
  - [PortalDirectLoginSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java:242)
  - [PortalDirectLoginSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java:244)
- 旧 key 仅作为清理对象保留，不能作为成功消费来源：
  - [PortalDirectLoginSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java:350)
  - [PortalDirectLoginSupport.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\service\support\PortalDirectLoginSupport.java:361)
- 单测已验证“只剩 legacy key 时消费失败并删除两种 key”：
  - [PortalDirectLoginSupportTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\service\support\PortalDirectLoginSupportTest.java:168)

### 3. actingAdmin 记录是否误用票据签发人

- 管理端强退/锁定/后台重置密码后的强退审计，会在写 `seller_login_log` / `buyer_login_log` 前覆盖为**当前执行控制动作的管理端账号**，不会沿用历史 direct-login session 的原签发人：
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:956)
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:994)
  - [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:956)
  - [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:994)
- 单测已明确断言 direct-login 会话被强退/密码重置后，登录日志中的 acting admin 是“当前 admin”，不是旧 issuer：
  - [SellerServiceImplTest.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\test\java\com\ruoyi\seller\service\impl\SellerServiceImplTest.java:464)
  - [SellerServiceImplTest.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\test\java\com\ruoyi\seller\service\impl\SellerServiceImplTest.java:502)
  - [BuyerServiceImplTest.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\test\java\com\ruoyi\buyer\service\impl\BuyerServiceImplTest.java:464)
  - [BuyerServiceImplTest.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\test\java\com\ruoyi\buyer\service\impl\BuyerServiceImplTest.java:502)

### 4. 端内自助日志 / session DTO 是否泄露敏感字段

- seller/buyer 自助接口返回的不是内部审计模型，而是 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile` / `PortalOwnSessionProfile`：
  - [SellerPortalController.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\SellerPortalController.java:163)
  - [SellerPortalController.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\SellerPortalController.java:174)
  - [SellerPortalController.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\SellerPortalController.java:185)
- service 层是新建 profile 并逐字段拷贝，未把 `subjectId / accountId / directLoginTicketId / actingAdmin / directLoginReason / operParam / jsonResult / tokenId` 带出去：
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:383)
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:419)
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:449)
  - [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:383)
- DTO 定义本身也未声明这些敏感字段：
  - [PortalOwnLoginLogProfile.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\domain\PortalOwnLoginLogProfile.java:10)
  - [PortalOwnOperLogProfile.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\domain\PortalOwnOperLogProfile.java:10)
  - [PortalOwnSessionProfile.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\main\java\com\ruoyi\system\domain\PortalOwnSessionProfile.java:10)
  - [party.d.ts](E:\Urili-Ruoyi\react-ui\src\types\seller-buyer\party.d.ts:339)
  - [party.d.ts](E:\Urili-Ruoyi\react-ui\src\types\seller-buyer\party.d.ts:350)
  - [party.d.ts](E:\Urili-Ruoyi\react-ui\src\types\seller-buyer\party.d.ts:388)
- 序列化测试已固定“自助 DTO 不得包含 subjectId/accountId/directLoginTicketId/actingAdmin/operParam/jsonResult/tokenId”：
  - [PortalSelfAuditSerializationTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\domain\PortalSelfAuditSerializationTest.java:11)
  - [PortalHomeProfileSerializationTest.java](E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\domain\PortalHomeProfileSerializationTest.java:78)

### 5. 端内自助接口是否允许前端注入 subject/account scope

- 前端 portal session/log API 会剥离调用方传入的 `sellerId / buyerId / subjectId / accountId / sellerAccountId / buyerAccountId / terminal`，避免 caller-controlled scope：
  - [session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:9)
  - [session.ts](E:\Urili-Ruoyi\react-ui\src\services\portal\session.ts:26)
- 后端自助查询又会强制改写成当前 session 的 `subjectId + accountId`：
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:525)
  - [SellerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java:540)
  - [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:525)
  - [BuyerServiceImpl.java](E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java:540)
- 前端单测已覆盖 scope stripping：
  - [portal-session-request.test.ts](E:\Urili-Ruoyi\react-ui\tests\portal-session-request.test.ts:38)

## 本轮未列入问题清单的原因

- `portal_direct_login:{token_hash}` 旧 key 仍在代码中出现，但仅用于**删除历史残留 key**，没有读取回退路径，因此不构成本次要求里的 P0/P1。
- 管理端 `PortalSessionProfile / PortalLoginLog / PortalOperLog` 仍然保留敏感审计字段，这属于管理端审计面本应可见的数据，不属于“端内自助接口泄露”。

## 最小后续动作

1. 继续按当前实现推进，不建议在这条链路上再做无证据重构。
2. 若要继续压缩回归风险，优先跑现有测试：
   - `PortalDirectLoginSupportTest`
   - `SellerServiceImplTest`
   - `BuyerServiceImplTest`
   - `PortalSelfAuditSerializationTest`
   - `PortalHomeProfileSerializationTest`
   - `portal-session-request.test.ts`
