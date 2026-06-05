# 管理端账号域权限运行时负向验证记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家/买家账号域权限菜单和远程库补齐之后，专门验证低权限管理账号不能通过账号维护权限。

本轮只新增测试和记录，不新增表，不执行远程数据库 DDL/DML，不修改 Redis，不改变接口路径、菜单 seed 或业务服务实现。

## 已完成

- 新增框架层运行时测试：`RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java`。
- `PermissionServiceAccountPermissionTest` 覆盖若依实际 `@ss.hasPermi(...)` Bean：
  - 只有 `seller:admin:query/add/edit/resetPwd/role:*` 时，不能通过 `seller:admin:account:*`。
  - 只有 `buyer:admin:query/add/edit/resetPwd/role:*` 时，不能通过 `buyer:admin:account:*`。
  - 精确 `seller:admin:account:*` 只允许卖家账号域动作，不允许买家账号域动作。
  - 超级权限 `*:*:*` 仍可通过卖家/买家账号域权限。
- 为 `ruoyi-framework` 增加 `junit` 的 test scope 依赖，仅用于该模块单元测试。
- 新增前端显隐契约测试：`RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminAccountPermissionUiContractTest.java`。
- `AdminAccountPermissionUiContractTest` 固定：
  - `Seller/index.tsx` 必须配置 `seller:admin:account:*`。
  - `Buyer/index.tsx` 必须配置 `buyer:admin:account:*`。
  - `PartnerManagementPage` 的主体行“账号”入口必须受 `accountPermissions.list` 控制。
  - `PartnerAccountModal` 的新增、编辑、重置密码和账号角色分配必须受 `accountPermissions` 控制。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminAccountPermissionUiContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src\components\PartnerManagement\PartnerManagementPage.tsx src\components\PartnerManagement\PartnerAccountModal.tsx src\pages\Seller\index.tsx src\pages\Buyer\index.tsx`：通过。

## 权限检查结果

- 后端运行时权限判断已证明：主体查询、新增、编辑、主账号重置、端内角色查询和端内角色编辑权限，不能替代账号域权限。
- 前端显隐契约已证明：卖家/买家页面必须显式配置账号域权限，公共账号入口和账号操作按钮必须读取 `PartnerModuleConfig.accountPermissions`。
- 本轮没有改变免密代入、强制踢出、主体主账号重置密码、端内菜单或端内角色权限模型。

## 字典/选项复用检查结果

- 本轮未新增字典、业务选项或 mock 数据。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记账号域权限运行时负向测试和 UI 显隐契约守卫。

## CodeGraph 更新结果

- `codegraph sync .`：通过，首次输出 `Synced 3 changed files`；记录回填后最终复跑输出 `Already up to date`。

## 大文件合理性判断结果

- 新增测试文件职责单一，未触发 300 行检查阈值。
- `PartnerManagementPage.tsx` 和 `PartnerAccountModal.tsx` 本轮未继续扩大。

## 残留问题

- 本轮没有启动浏览器用真实低权限账号做人工点击验收。
- 本轮没有新建低权限管理端账号或修改远程库角色绑定；真实账号 403 验收可作为后续独立切片执行。
