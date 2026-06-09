# Portal Auth / Direct-Login / Session / Log 审计（切片 2）

日期：2026-06-09
范围：`seller` / `buyer` portal auth、direct-login、session、login/oper log 链路
口径：只读 P0/P1 审计，不改代码；仅关注编译、guard、接口、权限、串端、service/字段缺失

## 结论

本次切片 **未发现 P0/P1 问题**。

已核对的高风险点均有代码约束和测试证据覆盖：

- seller / buyer token 存储与清理按 terminal 隔离
- Redis direct-login payload key 使用 `portal_direct_login:{terminal}:{token_hash}`
- 免密票据消费强校验 terminal / ticket / target subject / target account 一致性
- direct-login 成功/失败、session 强退、密码重置强退均写入结构化审计字段
- seller / buyer self log DTO 未暴露 admin 审计字段
- session 强退审计使用当前执行管理端账号覆盖 `actingAdmin*`

## P0/P1 Findings

无。

## 证据

### 1. Redis key 与 terminal 绑定

- [`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`](</E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:38>)
  定义缓存前缀 `portal_direct_login:`。
- [`PortalDirectLoginSupport.java:98`](</E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:98>)
  创建票据时按 `cacheKey(portalType, tokenHash)` 写 Redis。
- [`PortalDirectLoginSupport.java:187`](</E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:187>)
  票据 terminal 不匹配直接 fail-closed。
- [`PortalDirectLoginSupport.java:215`](</E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:215>)
  payload terminal 不匹配直接 fail-closed。
- [`PortalDirectLoginSupport.java:231`](</E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:231>)
  target subject / subjectNo / account / username 任一不匹配即拒绝。

### 2. seller / buyer direct-login 消费链路未串端

- [`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:684`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:684>)
  seller 端消费 direct-login token。
- [`SellerServiceImpl.java:714`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:714>)
  seller 端明确调用 `consumeToken("seller", ...)`。
- [`SellerServiceImpl.java:688`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:688>)
  账号查询使用 `selectSellerAccountByIdAndSellerId`，不是裸 accountId。
- [`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:684`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:684>)
  buyer 端对称实现。
- [`BuyerServiceImpl.java:714`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:714>)
  buyer 端明确调用 `consumeToken("buyer", ...)`。
- [`BuyerServiceImpl.java:688`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:688>)
  账号查询使用 `selectBuyerAccountByIdAndBuyerId`。

### 3. self log DTO 未泄露 admin 审计字段

- 后端 self log 查询强制带当前 session 的 `subjectId + accountId`：
  - [`SellerServiceImpl.java:535`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:535>)
  - [`SellerServiceImpl.java:550`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:550>)
  - [`BuyerServiceImpl.java:535`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:535>)
  - [`BuyerServiceImpl.java:550`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:550>)
- 后端 self log 映射只输出有限字段：
  - [`SellerServiceImpl.java:384`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:384>)
  - [`SellerServiceImpl.java:420`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:420>)
  - [`BuyerServiceImpl.java:384`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:384>)
  - [`BuyerServiceImpl.java:420`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:420>)
- DTO 定义未暴露 `actingAdminId` / `actingAdminName` / `directLoginTicketId` / `directLoginReason`：
  - [`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnLoginLogProfile.java:14`](</E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnLoginLogProfile.java:14>)
  - [`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnOperLogProfile.java:14`](</E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnOperLogProfile.java:14>)
  - [`react-ui/src/types/seller-buyer/party.d.ts:344`](</E:/Urili-Ruoyi/react-ui/src/types/seller-buyer/party.d.ts:344>)
  - [`react-ui/src/types/seller-buyer/party.d.ts:355`](</E:/Urili-Ruoyi/react-ui/src/types/seller-buyer/party.d.ts:355>)

### 4. 结构化审计字段已落库

- seller login / oper / session：
  - [`RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:431`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:431>)
  - [`SellerMapper.xml:489`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:489>)
  - [`SellerMapper.xml:536`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:536>)
- buyer login / oper / session：
  - [`RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:372`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:372>)
  - [`BuyerMapper.xml:430`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:430>)
  - [`BuyerMapper.xml:477`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:477>)

这些 SQL 都包含：

- `direct_login`
- `direct_login_ticket_id`
- `acting_admin_id`
- `acting_admin_name`
- `direct_login_reason`

### 5. session 强退审计记录当前执行 admin，而不是沿用原票据签发人

- seller：
  - [`SellerServiceImpl.java:966`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:966>)
  - [`SellerServiceImpl.java:989`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:989>)
  - [`SellerServiceImpl.java:1004`](</E:/Urili-Ruoyi/RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:1004>)
- buyer：
  - [`BuyerServiceImpl.java:966`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:966>)
  - [`BuyerServiceImpl.java:989`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:989>)
  - [`BuyerServiceImpl.java:1004`](</E:/Urili-Ruoyi/RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:1004>)

`applyCurrentAdminAudit(...)` 会把 `actingAdminId` / `actingAdminName` 覆盖为当前管理端执行人。

### 6. 前端 token 隔离与 direct-login 失败不清 token

- [`react-ui/src/pages/Portal/terminal.ts:38`](</E:/Urili-Ruoyi/react-ui/src/pages/Portal/terminal.ts:38>)
  `persistPortalLogin(...)` 要求 `result.terminal === expectedTerminal`。
- [`terminal.ts:42`](</E:/Urili-Ruoyi/react-ui/src/pages/Portal/terminal.ts:42>)
  terminal 不匹配直接返回 `false`，不清任何 token。
- [`react-ui/src/requestErrorConfig.ts:33`](</E:/Urili-Ruoyi/react-ui/src/requestErrorConfig.ts:33>)
  portal 401 先按 API URL 识别 terminal。
- [`requestErrorConfig.ts:36`](</E:/Urili-Ruoyi/react-ui/src/requestErrorConfig.ts:36>)
  direct-login API 401 直接返回，不清 token。
- [`requestErrorConfig.ts:39`](</E:/Urili-Ruoyi/react-ui/src/requestErrorConfig.ts:39>)
  非 direct-login portal 401 只清对应 terminal token。
- [`react-ui/src/pages/Portal/DirectLogin/index.tsx:77`](</E:/Urili-Ruoyi/react-ui/src/pages/Portal/DirectLogin/index.tsx:77>)
  直登页消费 token 后仍再次依赖 `persistPortalLogin(...)` 做 terminal 一致性校验。
- [`DirectLogin/index.tsx:100`](</E:/Urili-Ruoyi/react-ui/src/pages/Portal/DirectLogin/index.tsx:100>)
  `postMessage` 还要求 `event.source === window.opener` 且 `event.origin === openerOrigin`。

## 执行证据

### 已通过

1. 前端 portal token guard

```powershell
node .\scripts\check-portal-token-isolation.mjs
```

结果：`Portal token isolation guard passed.`

2. seller / buyer service 单测

```powershell
mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- `SellerServiceImplTest`：55 passed
- `BuyerServiceImplTest`：55 passed
- Reactor build：`BUILD SUCCESS`

3. 前端定向 Jest

```powershell
npx jest --config jest.config.ts tests/terminal-session-token.test.ts tests/portal-unauthorized-redirect.test.ts --runInBand
```

结果：

- `Test Suites: 2 passed, 2 total`
- `Tests: 24 passed, 24 total`

### 非阻塞备注

- `npm test -- --runInBand ...` 失败是因为仓库把 `test` 脚本封装到 `verify-three-terminal.mjs`，该脚本只允许 `--coverage`、`-u`、`--updateSnapshot` 透传，不是业务缺陷，也不构成 P0/P1。
- 直接调用 Jest 时出现 `Jest did not exit one second after the test run has completed.` 提示；本轮相关用例已全部通过，这个提示更像测试进程尾部异步句柄未完全回收，不属于本次 portal auth/direct-login/session/log 链路的 P0/P1。
