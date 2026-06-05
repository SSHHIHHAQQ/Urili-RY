# 端内 Portal 守卫自动发现执行记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理端内 portal 静态守卫问题。

只读审计确认：当前 seller/buyer 已有 portal handler 模板守卫和综合 seed 守卫，但此前 `TerminalRouteOwnershipTest` 只硬编码当前几个 portal controller 文件；如果后续新增 `SellerPortalOrderController`、`SellerPortalProductController` 等真实业务 controller，存在忘记加入测试清单的风险。`TerminalSeedPermissionContractTest` 也主要依赖当前手写权限清单，不能自动发现源码中新增的 `@PortalPreAuthorize(hasPermi=...)`。

## 本轮范围

- 修改 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`。
- 修改 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java`。
- 更新目标追踪和复用台账。
- 不修改业务代码。
- 不修改前端。
- 不执行远端 MySQL / Redis DDL 或 DML。

## 已完成

- 使用 6 个只读子 agent 并行审计管理端卖家 UI、卖家后端控制面、卖家 portal 范围、权限 seed、三端前端耦合和测试缺口；子 agent 均已关闭。
- `TerminalRouteOwnershipTest` 改为自动发现 seller/buyer 模块 controller 目录下的受保护 `*Portal*Controller.java`。
- `SellerPortalAuthController` / `BuyerPortalAuthController` 继续作为登录和免密消费认证入口例外，不纳入受保护 handler 模板检查。
- 受保护 portal handler 继续统一校验：
  - 方法级 `@Anonymous`
  - `@PortalPreAuthorize(terminal = "...")`
  - `@PortalLog(terminal = "...")`
  - `PortalSessionContext.requireSession("...")`
- `TerminalSeedPermissionContractTest` 新增源码扫描：自动提取 seller/buyer 源码中的 `@PortalPreAuthorize(hasPermi = "...")`，要求这些端内权限存在于 `seller_buyer_management_seed.sql`。
- 保留综合 seed 当前最小权限和默认角色、账号角色、角色菜单绑定结构检查。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过；Git 仅提示 LF/CRLF 转换警告。
- 新增/未跟踪测试和记录文件空白检查：无尾随空白输出。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 5 changed files`。

## 当前判断

- 后续新增 seller/buyer 受保护 portal controller 时，只要文件名符合 `*Portal*Controller.java` 且不属于 `*PortalAuthController.java`，会自动进入模板守卫。
- 后续新增端内 `@PortalPreAuthorize(hasPermi=...)` 权限时，如果只改 controller 不更新综合 seed，`TerminalSeedPermissionContractTest` 会失败。
- 本轮只增强自动化守卫，没有触碰远端运行库；运行库是否应用最新 seed 仍需在需要执行 SQL 时单独确认数据源和执行记录。

## 后续候选

- 卖家管理端账号控制硬化：拆出更细的 `seller:admin:account:*` 权限，并决定重置密码是否强制踢出当前账号会话。
- portal seller 模板去命名化：把 `PortalProductSchemaPreview` 从 seller 文件移到中性组件，并把商品 schema API 从 `portal/session.ts` 中拆出。
- 端内当前账号日志/会话专门契约：固定账号日志、操作日志和当前会话接口必须从 `PortalSessionContext` 派生范围，并验证 `PortalSessionProfile.tokenId` 不序列化输出。
