# 三端端入口归属测试记录

## 背景

当前开发方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准。管理端保留若依 `sys_*` 控制面；卖家端、买家端的端内入口必须分别落在 `seller`、`buyer` 模块，`product` 只作为共享商品领域基础能力，不作为第四个终端入口。

本轮用户明确要求先做一套标准卖家模板，验收通过后再复制买家，并且每个切片只改一类东西。因此本切片只固化“端入口归属”和“端内受保护接口鉴权模板”边界，不新增页面、不新增接口、不执行 SQL。

## 本次改动

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`。
- 测试扫描 `RuoYi-Vue/product/src/main/java` 下的 Java 源码。
- 若 `product` 模块出现 `@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping` 直接暴露 `/seller...` 或 `/buyer...` 路径，测试失败。
- 在同一测试中先增加 seller 受保护 portal controller 鉴权模板检查，覆盖 `SellerPortalController` 和 `SellerPortalProductSchemaController`。
- seller 模板通过后，按同一 helper 复制 buyer 检查，只替换 terminal 和 controller 路径，覆盖 `BuyerPortalController` 和 `BuyerPortalProductSchemaController`。
- 受保护端内 handler 必须方法级声明 `@Anonymous`、`@PortalPreAuthorize(terminal = "...")`、`@PortalLog(terminal = "...")`，且方法体必须从 `PortalSessionContext.requireSession(...)` 派生当前端身份。

## 设计判断

- 卖家端商品发布、买家端商品浏览这类端内入口应由 `seller` / `buyer` facade controller 承载。
- `product` 模块可以提供共享 Service、领域对象、Mapper 和管理端 `/product/admin/**` 配置入口。
- 本测试只限制 `product` 模块不得暴露 seller/buyer 端入口，不限制管理端 `/product/admin/**`。
- 本测试不替代真实接口权限验证，但会防止后续新增端内 handler 漏掉端 token 鉴权、日志或当前会话派生。

## 验证结果

- seller 模板验证：`cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- buyer 复制后验证：`cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java docs/plans/2026-06-05-terminal-route-ownership-test-record.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 提示。
- `codegraph sync .`：通过；代码变更同步时输出 `Synced 1 changed files`，记录补充后最终复跑输出 `Already up to date`。

## 风险与剩余事项

- 当前还没有做新的卖家 UI 模板，本轮只做端内入口和鉴权模板边界守卫。
- 后续端内真实业务接口还需要继续接入具体业务权限点和数据范围校验；本测试只能证明 handler 模板不漏，不证明业务规则完整。
- 若后续新增 seller/buyer portal controller，需要把该 controller 纳入本测试的 controller 路径清单。
