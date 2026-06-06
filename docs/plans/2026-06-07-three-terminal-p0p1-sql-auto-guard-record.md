# 2026-06-07 三端 P0/P1 快速推进：高影响 SQL 自动 Guard 记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未修改 source-product / integration Java 和前端旁支脏改。
- 本轮未新增子 Agent：问题边界已由上一轮只读 Agent 指明，主 Agent 本地实现和验证更直接。

## 新增问题

- P1：`SqlExecutionGuardContractTest` 此前主要依赖手工 `assertGuard(...)` 点名，无法自动发现 20260606/20260607 新增高影响 SQL。
- P1：`20260606_product_spu_warehouse_binding.sql`、`20260607_source_product_read_model.sql`、`20260607_upstream_task_component_split.sql` 含 `create table` 或 `update sys_job`，但缺少 fail-closed 确认 guard。

## 已修复问题

- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 新增 `recentHighImpactSqlScriptsMustBeAutoDiscoveredAndGuarded()`，自动扫描 `20260606*.sql` / `20260607*.sql`。
  - 高影响 SQL 命中 `insert/update/delete/alter table/create table` 时，要求 `set @confirm_*`、`signal sqlstate '45000'`、`call assert_*_confirmed();`，并复用确认调用必须早于首条 DDL/DML 的检查。
  - 将 `20260606_product_spu_warehouse_binding.sql`、`20260606_upstream_sync_staging_diff.sql`、`20260607_source_product_read_model.sql`、`20260607_upstream_task_component_split.sql` 纳入显式 `assertGuard(...)`。
- `RuoYi-Vue/sql/20260606_product_spu_warehouse_binding.sql`
  - 新增 `@confirm_product_spu_warehouse_binding` 和 `assert_product_spu_warehouse_binding_confirmed()`。
- `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
  - 新增 `@confirm_source_product_read_model` 和 `assert_source_product_read_model_confirmed()`。
- `RuoYi-Vue/sql/20260607_upstream_task_component_split.sql`
  - 新增 `@confirm_upstream_task_component_split` 和 `assert_upstream_task_component_split_confirmed()`。
- `docs/architecture/reuse-ledger.md`
  - 补充高影响 SQL 自动发现模板。

## 残留问题

- P1：管理端卖家/买家页缺静态路由兜底，直达 `/seller`、`/buyer` 或刷新存在 404 风险。
- P1：`portal_direct_login:<tokenHash>` Redis key 仍未在 key 层编码 `seller/buyer` 端类型。
- P1：部门树跨主体写入/删除、角色菜单 `checkedKeys` 主体隔离、owner 角色禁停用/禁删除仍需补运行时隔离契约测试。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过，`7` 个测试通过。
- 高影响 SQL guard 扫描结果：20260606/20260607 命中的高影响脚本均具备 `@confirm_`、`signal sqlstate '45000'` 和 `call assert_*_confirmed();`。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确本阶段无需浏览器运行态验收。
- 未执行远程 SQL：本轮只补脚本文本 guard 和契约测试，没有执行 DDL/DML 的必要。
- 未启动后端：本轮修改点是 SQL 文本 guard 和架构测试。

## 权限检查结果

- 本轮未新增后端接口或权限标识。
- SQL guard 覆盖的是高影响脚本执行前确认，不改变管理端或端内权限模型。

## 字典/选项复用检查结果

- 本轮未新增业务字典、选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，记录高影响 SQL 自动发现模板。

## CodeGraph 更新结果

- `codegraph sync .` 已执行，首次结果为 `Synced 1 changed files`，`Modified: 1 - 34 nodes in 691ms`；回填记录后最终复跑结果为 `Already up to date`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest` 是既有架构契约测试，本轮新增自动扫描 helper，职责仍集中在 SQL 执行 guard。
- 三个 SQL 脚本只新增确认块，不改变业务 SQL 主体。

## 重复代码检查结果

- SQL 确认块按现有 `@confirm_*` / `assert_*_confirmed()` 模板复制。
- 自动发现逻辑集中在 `SqlExecutionGuardContractTest`，后续新增 20260606/20260607 高影响 SQL 不需要再靠人工发现。
