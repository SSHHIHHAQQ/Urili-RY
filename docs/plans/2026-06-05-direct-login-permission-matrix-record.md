# 2026-06-05 directLogin 权限矩阵补强记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“每个切片只改一类东西”的节奏推进。

本切片只补若依运行时 `PermissionService` 权限矩阵，确认管理端免密代入 `*:admin:directLogin`、免密票据 `*:admin:ticket:list` 与账号 reset/lock、强制踢出权限互不串用。本轮不改接口、不改前端、不新增权限点、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `PermissionServiceAccountPermissionTest` 增加 directLogin / ticket 运行时权限断言。
- 主体权限和端内角色权限不能误授权账号域权限、强制踢出、免密代入或免密票据。
- 账号域 reset/lock 和强制踢出权限不能误授权 `*:admin:directLogin` 或 `*:admin:ticket:list`。
- `*:admin:directLogin` / `*:admin:ticket:list` 不能误授权账号 reset/lock 或强制踢出。
- 精确 seller directLogin/ticket 权限不能串到 buyer，超管通配仍可访问 seller/buyer 两端权限。
- 更新 `docs/architecture/reuse-ledger.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`。

## 新增问题

- 无。

## 残留问题

- 本切片没有新增真实低权限 HTTP / 浏览器 smoke；seller/buyer directLogin 低权限真实验收已有前序记录，本轮只固化运行时 `@ss.hasPermi(...)` 矩阵。
- 当前 `AdminDirectLoginPermissionContractTest` 仍负责 controller、seed、前端显隐的静态契约；本切片不重复改该测试。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest test
mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest clean test

cd E:\Urili-Ruoyi
git diff --check -- RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-direct-login-permission-matrix-record.md
```

## 验证结果

- 非 clean 定向测试：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- clean 定向测试：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `git diff --check`：通过，仅有 Git 行尾转换 warning，无空白错误。
- 残留关键词扫描：无常见代码占位词、冲突标记或未完成占位标记命中。

## 未验证原因

- 本切片不启动后端，因为只补权限服务单元测试。
- 本切片不连接远程 MySQL / Redis，因为未新增或修改 DDL / DML，也未做运行时 smoke。
- 本切片不做浏览器验证，因为前端没有变化。

## 权限检查结果

- seller 免密代入仍为 `seller:admin:directLogin`，免密票据仍为 `seller:admin:ticket:list`。
- buyer 免密代入仍为 `buyer:admin:directLogin`，免密票据仍为 `buyer:admin:ticket:list`。
- 账号 reset/lock、强制踢出、免密代入和免密票据四类敏感权限互相独立。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 登记：`PermissionServiceAccountPermissionTest` 已覆盖 directLogin/ticket 运行时矩阵，防止账号 reset/lock/forceLogout 与 directLogin/ticket 权限互相误授权。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`，退出码 `0`。

## 大文件合理性判断结果

- `PermissionServiceAccountPermissionTest.java`：`253` 行，职责集中在若依 `PermissionService` 的 seller/buyer 账号域、强制踢出、免密代入权限矩阵，未触发 300 行阈值。
- 本切片没有新增或修改其他大文件中的代码。

## 重复代码检查结果

- seller/buyer 权限矩阵在同一测试类中按同构权限点成组断言，避免拆到多个测试类后遗漏跨端负向断言。
