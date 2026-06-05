# 端内 Portal 后台上下文回退守卫执行记录

日期：2026-06-05

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按当前执行节奏推进：先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西。

本轮只处理一类问题：seller/buyer 端受 `@PortalPreAuthorize` 保护的端内接口，身份来源只能是端 token 校验后写入的 `PortalSessionContext`，不得在缺失端内上下文时回退到若依后台 `SecurityUtils` / `LoginUser` / `SysUser` / `@ss` 权限上下文。

不处理的内容：

- 不新增业务接口。
- 不改 seller/buyer service 业务逻辑。
- 不复制 buyer 前端。
- 不执行远程数据库 DDL/DML。
- 不连接远程 MySQL / Redis。
- 不启动 `seller-ui` / `buyer-ui` 物理拆分。

## 已完成

- 更新 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`。
- 新增 seller 模板守卫：`sellerPortalHandlersMustNotUseAdminLoginContext`。
- 按同构规则复制 buyer 守卫：`buyerPortalHandlersMustNotUseAdminLoginContext`。
- 新增对受保护 portal handler 方法体的检查，禁止出现：
  - `SecurityUtils.getLoginUser(...)`
  - `SecurityUtils.getUserId(...)`
  - `SecurityUtils.getUsername(...)`
  - 裸调用 `getLoginUser(...)` / `getUserId(...)` / `getUsername(...)`
  - `LoginUser`
  - `SysUser`
- 继续保留原有守卫：
  - `product` 模块不得暴露 `/seller...` 或 `/buyer...` 端入口。
  - 受保护 portal handler 必须方法级声明 `@Anonymous`。
  - 受保护 portal handler 必须声明 `@PortalPreAuthorize(terminal = "...")`。
  - 受保护 portal handler 必须声明 `@PortalLog(terminal = "...")`。
  - 受保护 portal handler 必须调用 `PortalSessionContext.requireSession(...)`。
  - 受保护 portal handler 方法签名不得接收客户端身份范围参数。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内 Controller 鉴权模板和 `PortalSessionContext` 复用规则。

## 子 agent 审计结论

本轮使用 6 个子 agent 并行做只读审计，并已全部关闭：

- seller controller 审计：当前受保护 seller portal handler 均调用 `PortalSessionContext.requireSession("seller")`，未发现后台 `SecurityUtils` / `LoginUser` / `SysUser` 身份回退。
- buyer controller 审计：当前受保护 buyer portal handler 均调用 `PortalSessionContext.requireSession("buyer")`，未发现后台 `SecurityUtils` / `LoginUser` / `SysUser` 身份回退。
- 架构测试审计：该守卫适合放在 `TerminalRouteOwnershipTest`，只扫受保护 portal handler；不应扩大到全模块扫描，避免误伤管理端控制面合法使用后台上下文写审计字段。
- service 审计：`SellerServiceImpl` / `BuyerServiceImpl` 中 `SecurityUtils.getUsername()` 当前用于管理端控制面审计字段，`encryptPassword` / `matchesPassword` 只是 BCrypt 工具能力，当前可接受。
- service 风险提示：`SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 和 `SellerPortalDeptServiceImpl` / `BuyerPortalDeptServiceImpl` 的写方法当前由管理端 controller 调用；如果未来开放端内自管理，操作人必须改为从 `PortalLoginSession` 获取，不能复用后台 `SecurityUtils.getUsername()`。
- UI 模板审计：管理端卖家模板仍按 `sellerConfig + PartnerManagementPage` 作为标准模板推进，后续 buyer 只替换配置和 service；本轮未改 UI。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest" test`：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- UI 子 agent 只读审计中运行 `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过，`Partner management template guard passed.`。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-terminal-portal-admin-context-guard-record.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 当前判断

- 当前受保护 seller/buyer portal handler 已由测试同时守住三层身份边界：必须从 `PortalSessionContext` 派生身份、不能接收客户端身份范围参数、不能回退若依后台登录上下文。
- 管理端 `/seller/admin/**`、`/buyer/admin/**` 继续使用若依 `@PreAuthorize("@ss.hasPermi(...)")` 和后台登录上下文；本轮守卫不限制管理端控制面。
- 该守卫只覆盖受保护 portal controller handler，不替代具体业务 service 的数据范围单元测试或真实接口烟测。
- 本轮未执行 SQL，未连接远程 MySQL / Redis，未修改远程数据。
