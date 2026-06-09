# 2026-06-09 三端隔离切片2只读审计（portal auth / direct-login / session / log）

## 范围

- 仓库：`E:\Urili-Ruoyi`
- 模式：只读审计，不改业务文件
- 审计口径：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 与 `AGENTS.md`
- 本轮只看三端隔离快速模式 P0/P1：
  - 编译
  - guard
  - 接口
  - 权限
  - 串端
  - service / 字段缺失
- 切片范围：
  - `RuoYi-Vue/ruoyi-system`
  - `RuoYi-Vue/seller`
  - `RuoYi-Vue/buyer`
  - `react-ui` portal auth / direct-login / session / log / 401

## 结论

- **P0/P1：本轮未发现新增阻塞问题。**
- 代码、contract、unit test、guard 与计划口径一致，当前切片满足：
  - 免密 Redis payload 只使用 `portal_direct_login:{terminal}:{token_hash}` 作为读写主链路，旧 key 只做残留清理。
  - 免密票据 30 分钟、一次性、terminal/subject/account 校验齐全。
  - 跨端失败不会把外端票据的 `actingAdmin*` / `ticketId` 写入当前端自助日志。
  - portal 登录页 / direct-login 失败不会预清当前端既有 token。
  - 自助日志与自助会话 DTO 未暴露内部审计字段。
  - 401 语义按 admin / seller / buyer 隔离，direct-login 401 不清既有 portal token。

## P0/P1 证据

### 1. Redis key 主链路符合 `{terminal}:{token_hash}`

- `PortalDirectLoginSupport` 写入和读取都走 `cacheKey(portalType, tokenHash)`：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:98`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:244`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:356`
- 旧 key `portal_direct_login:{token_hash}` 没有参与认证读取，只在删除时一并清理历史残留：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:350-363`

### 2. 票据 30 分钟、一次性、terminal/subject/account 校验齐全

- 30 分钟有效期常量与写 Redis TTL：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:32`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:62`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:98`
- 一次性消费：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:191-199`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:282-297`
  - `RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/PortalDirectLoginTicketMapper.xml:89-110`
- terminal 校验：
  - ticket terminal：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:187-190`
  - payload terminal：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:215-219`
- subject / account / username / subjectNo 一致性校验：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:226-238`
- seller / buyer 端消费后再按当前端主体+账号重查，防止串端或脏上下文直接放行：
  - seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:684-741`
  - buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:684-741`

### 3. 跨端失败不会写外端 acting admin / ticket

- seller 端失败审计先判断 `token.getPortalType()`；不是 `seller` 时退回普通失败日志，不走 direct-login 审计字段：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:743-752`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:1109-1125`
- buyer 端同样逻辑：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:743-752`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:1109-1125`
- 覆盖到单测：
  - seller：`RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java:998-1008`
  - buyer：`RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java:998-1008`

### 4. 自助日志 / 自助会话 DTO 不暴露内部审计字段

- DTO 自身未定义 `subjectId`、`accountId`、`tokenId`、`directLoginTicketId`、`actingAdmin*`、`directLoginReason`：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnLoginLogProfile.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnOperLogProfile.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnSessionProfile.java`
- seller / buyer service 的自助映射仅拷贝白名单字段：
  - seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:384-460`
  - buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:384-460`
- mapper 查询虽读取完整内部字段，但 controller 返回的是自助 DTO，不是内部模型：
  - seller：`RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:443-575`
  - buyer：`RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:384-516`
- 有序列化与架构合同测试兜底：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java:12-60`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalSelfServiceSurfaceContractTest.java:86-214`

### 5. 401 语义符合 admin / portal 隔离；direct-login 401 不清既有 token

- 后端 portal 会话缺失统一抛 `401 UNAUTHORIZED`：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalSessionContext.java:39-47`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:165-173`
- 前端 request 层：
  - portal 401 只清命中的 terminal token，并带 `redirect` 跳对应 portal login：
    - `react-ui/src/requestErrorConfig.ts:33-45`
  - direct-login 401 直接返回，不清 token：
    - `react-ui/src/requestErrorConfig.ts:35-38`
  - admin 前缀 `/api/seller/admin/**`、`/api/buyer/admin/**` 不会误走 portal 401：
    - `react-ui/src/utils/portalRequest.ts:33-44`
- `app.tsx` 启动期 / response body 401 也复用同样隔离语义：
  - `react-ui/src/app.tsx:73-93`
- portal login / direct-login 页面不会在成功前预清 token：
  - login：`react-ui/src/pages/Portal/Login/index.tsx:67-79`
  - direct-login：`react-ui/src/pages/Portal/DirectLogin/index.tsx:75-95`
  - persist 仅在 terminal 匹配且 token 存在时写入：
    - `react-ui/src/pages/Portal/terminal.ts:38-49`
- 前端测试与 guard 已覆盖：
  - `react-ui/tests/portal-unauthorized-redirect.test.ts`
  - `react-ui/tests/terminal-session-token.test.ts`
  - `react-ui/scripts/check-portal-token-isolation.mjs`

### 6. portal 自助日志 / 会话接口权限和会话来源正确

- seller portal controller：
  - 登录日志：`seller:account:loginLog:list`
  - 操作日志：`seller:account:operLog:list`
  - 会话：`seller:account:session:list`
  - 文件：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:163-194`
- buyer portal controller 同步口径：
  - 文件：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:163-194`

## P2

### P2-1 验证命令层有一个工具噪音，不是当前切片业务缺口

- 直接跑：

```powershell
npx jest tests/portal-unauthorized-redirect.test.ts tests/terminal-session-token.test.ts --runInBand
```

  会先被仓库同时存在的 `jest.config.js` / `jest.config.ts` 阻断；显式指定 `--config jest.config.ts` 后，又可能碰到 `.umi-test` 准备目录的 `ENOTEMPTY`。
- 但仓库标准入口 `node scripts/verify-three-terminal.mjs` 能正确先做 `umi test setup`，随后完整跑过 portal token guard、前端 portal 相关单测、react typecheck 和 backend contracts。
- 这说明 **验证入口应固定走仓库脚本，不应把裸 `npx jest` 当成稳定审计命令**。该项属工具链注意事项，不构成本轮 P0/P1 代码阻塞。

## 执行命令

```powershell
Get-Content docs/plans/2026-06-04-three-terminal-isolation-control-plan.md -Encoding UTF8
rg -n "portal_direct_login|directLogin|actingAdmin|ticketId|401|PortalOwn" RuoYi-Vue/ruoyi-system RuoYi-Vue/seller RuoYi-Vue/buyer
rg -n "401|clear.*token|direct-login|PORTAL_DIRECT_LOGIN_RESULT_MESSAGE" react-ui/src react-ui/tests react-ui/scripts
mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfAuditSerializationTest,PortalSelfServiceSurfaceContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
npm run guard:portal-token
node scripts/verify-three-terminal.mjs
```

## 验证结果

- `mvn -pl ruoyi-system,seller,buyer -am ... test`：通过
- `npm run guard:portal-token`：通过
- `node scripts/verify-three-terminal.mjs`：通过
  - 前端：23 suites / 180 tests 通过
  - 后端：three-terminal verification passed

## 最终判断

- **本切片当前可判定为 P0/P1 通过。**
- 后续若继续做 slice 2 相关整改，优先保持：
  - 只通过 `PortalOwn*Profile` 对外暴露自助日志/会话
  - 只在 request 层处理 portal 401 与 token 清理
  - 免密 Redis payload 继续禁止读取旧 `portal_direct_login:{token_hash}`
  - seller / buyer 镜像改动继续同步 contract / unit test / guard
