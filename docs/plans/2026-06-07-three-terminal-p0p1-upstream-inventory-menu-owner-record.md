# 2026-06-07 三端 P0/P1 上游库存菜单 Owner 收敛记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只修 P0/P1。本轮聚焦 `20260606_upstream_inventory_dimension_sync.sql` 重复 owning 上游系统管理按钮 `2307/2308/2309` 的问题。

## 新增问题

- P1：`upstream_system_management_seed.sql` 已经通过 `tmp_upstream_system_management_sys_menu_guard` / `assert_upstream_system_management_sys_menu_guard()` 持有 `2031` 和 `2300-2309`。
- P1：`20260606_upstream_inventory_dimension_sync.sql` 仍直接 `insert into sys_menu ... on duplicate key update` 写入 `2307/2308/2309`，形成双 owner。
- P1：该脚本后续 `sys_role_menu` 授权依赖 `target_menu.perms`；如果 upstream seed 未先提供菜单，授权会静默少做，而 schema/job 仍可能继续执行。

## 已修复问题

- `20260606_upstream_inventory_dimension_sync.sql` 删除 `2307/2308/2309` 的 `sys_menu` upsert，不再 owning 上游系统管理按钮。
- 新增 `assert_upstream_inventory_menu_owner_ready()`，在执行 schema/job/授权前确认：
  - `2307 / integration:upstream:dimensionSync`
  - `2308 / integration:upstream:inventoryQuery`
  - `2309 / integration:upstream:inventorySync`
- 如果上述菜单未由 `upstream_system_management_seed.sql` 准备好，脚本 fail-closed，不再进入部分成功状态。
- `SqlExecutionGuardContractTest` 新增 `upstreamInventoryDimensionSyncMustNotOwnUpstreamManagementMenuButtons`，固定库存维度同步脚本不能回退到 `insert into sys_menu` 或 2307/2308/2309 菜单行 owner。
- `docs/architecture/reuse-ledger.md` 已更新：`2031/2300-2309` 只由 upstream seed 持有，库存维度同步脚本只做 schema、job 和授权继承。

## 残留问题

- 本轮不执行远程 SQL，不校验远程 `sys_menu` / `sys_role_menu` 实际数据。
- 本轮不改库存同步业务语义，不处理 `upstream_system_sku_inventory_snapshot` schema、job 调度或读模型。
- 2026-06-06 的部分历史记录曾描述该脚本为 cleanup/disable；本轮已追加追记修正，历史文本保留其当时背景。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`26` 个测试通过。

## 未验证原因

- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只改 SQL 文件、静态合同和 Markdown 记录。
- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。

## 权限检查结果

- 管理端按钮权限仍是若依 `sys_menu` / `sys_role_menu` 控制面。
- `2307/2308/2309` 的 `sys_menu` owner 固定为 `upstream_system_management_seed.sql`。
- `20260606_upstream_inventory_dimension_sync.sql` 只从 `integration:upstream:sync` / `integration:upstream:query` 继承 `sys_role_menu` 授权到已有按钮，不再创建或改写按钮菜单。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或前端枚举。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记上游系统管理按钮单 owner、库存维度同步脚本依赖 owner seed 的规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Synced 1 changed files`，`Modified: 1 - 57 nodes`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行和菜单 owner guard 合同；本轮只追加同类静态合同，不拆分。
- `20260606_upstream_inventory_dimension_sync.sql` 是既有库存迁移脚本，本轮只删除重复菜单 owner 并增加前置断言，不拆分 schema/job 逻辑。

## 重复代码检查结果

- 未新增 Java 业务逻辑或 React 业务逻辑重复。
- SQL 前置断言只在当前脚本内使用，符合当前 MySQL 独立脚本局部 procedure 模式。

## 子 Agent 使用记录

- 本轮使用 6 个 `gpt-5.4` 只读子 Agent。
- 子 Agent 结论：`upstream_system_management_seed.sql` 已完整 owning `2031/2300-2309` 并带 guard；本轮最小修复是让 `20260606_upstream_inventory_dimension_sync.sql` 停止写 `sys_menu`，同时保留 `sys_role_menu` 授权继承并增加菜单存在性断言。

## 一句话总结

本轮把 `2307/2308/2309` 上游系统管理按钮从库存维度同步脚本中迁回单一 owner：菜单只由 `upstream_system_management_seed.sql` 持有，库存脚本只断言菜单存在后做授权继承、schema 和 job 迁移。
