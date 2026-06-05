# 端内 Portal 客户端身份参数守卫执行记录

日期：2026-06-05

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，聚焦三端独立里“卖家端/买家端接口不能相信前端传入的 `sellerId` / `buyerId`，必须从端 token 身份推导”的规则。

本轮只处理一类问题：增强端内受保护 portal handler 的架构测试，禁止 handler 签名接收客户端传入的端身份范围参数。

不处理的内容：

- 不新增业务接口。
- 不执行远程数据库 DDL/DML。
- 不改前端 token 持久化。
- 不改当前日志列表 service 的查询 DTO 收敛。
- 不开始 `seller-ui` / `buyer-ui` 物理拆分。

## 已完成

- 更新 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`。
- 新增 seller 模板守卫：`sellerPortalHandlersMustNotAcceptClientIdentityScope`。
- 按同构规则复制 buyer 守卫：`buyerPortalHandlersMustNotAcceptClientIdentityScope`。
- 新增对受保护 portal handler 方法声明的检查：方法签名中不得出现以下客户端身份范围参数：
  - `sellerId`
  - `buyerId`
  - `subjectId`
  - `accountId`
  - `terminal`
- 继续允许非身份业务参数，例如商品 Schema 的 `categoryId`。
- 继续保留原有守卫：
  - `product` 模块不得暴露 `/seller...` 或 `/buyer...` 端入口。
  - 受保护 portal handler 必须方法级声明 `@Anonymous`。
  - 受保护 portal handler 必须声明 `@PortalPreAuthorize(terminal = "...")`。
  - 受保护 portal handler 必须声明 `@PortalLog(terminal = "...")`。
  - 受保护 portal handler 必须调用 `PortalSessionContext.requireSession(...)`。

## 只读审计结论

本轮使用子 agent 做了只读并行审计，并已关闭子 agent：

- 后端审计未发现当前受保护 portal controller 直接信任前端传入 `sellerId` / `buyerId` / `accountId` / `subjectId` / `terminal` 作为数据范围边界。
- 前端审计未发现 seller/buyer portal 端接口复用管理端 `access_token`，也未发现 portal 页面通过传 `sellerId` / `buyerId` / `accountId` 决定端内数据范围。
- 目标追踪审计建议继续优先推进“端内真实业务接口范围控制模板”，前端三端物理拆分仍不建议马上大拆。

后续独立切片建议：

- 卖家端当前账号登录日志/操作日志列表现在由 controller 覆盖查询 DTO 的 `subjectId` / `accountId`，当前不构成绕过，但模板上更稳的做法是把范围收敛到 service：新增 `selectSellerOwnLoginLogList(PortalLoginSession, PortalLoginLog)` / `selectSellerOwnOperLogList(...)`，再复制 buyer。
- 前端可以单独加固 `persistPortalLogin(expectedTerminal, result)`，把端类型校验下沉到持久化封装内部。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\TerminalRouteOwnershipTest.java docs\plans\2026-06-05-terminal-portal-client-identity-guard-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，本轮代码变更后的首次同步输出 `Synced 1 changed files`；记录回填后最终复跑输出 `Already up to date`。

## 当前判断

- 当前受保护 seller/buyer portal handler 已由测试同时守住“必须从 `PortalSessionContext` 派生身份”和“不能在 handler 签名接收客户端身份范围参数”。
- 本切片是端内接口范围控制的架构守卫，不替代具体业务 service 的数据范围单元测试或真实接口烟测。
- 下一刀更适合按卖家模板先收敛当前账号日志列表 service 的 session-scoped 查询，再复制买家。
