# 2026-06-07 三端 P0/P1 仓库菜单 Seed Guard 收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只修 P0/P1。本轮聚焦 `warehouse_management_seed.sql` 直接 upsert 管理端仓库菜单但缺少 slot/signature guard 的问题。

## 新增问题

- P1：`warehouse_management_seed.sql` 写入 `2021/2022/202101-202105/202201-202204` 到 `sys_menu`，但原先只做 confirm token，没有在 upsert 前确认菜单 ID slot 和签名未被其它菜单占用。
- P1：仓库子菜单依赖父菜单 `2020`，但原脚本未在写子菜单前 fail-closed 确认 `top_menu_seed.sql` 已提供仓库顶级菜单。
- 文档残留：目标追踪和部分专项记录仍把 `20260606_admin_partner_role_menu_grant.sql` wildcard 授权列为待修 P1；当前 SQL 和合同已显示它已白名单化。

## 已修复问题

- `warehouse_management_seed.sql` 新增 `assert_warehouse_management_sys_menu_guard()`。
- 新增 `tmp_warehouse_management_sys_menu_guard`，覆盖：
  - `2021 / warehouse:official:list`
  - `2022 / warehouse:thirdParty:list`
  - `202101-202105 / warehouse:official:*`
  - `202201-202204 / warehouse:thirdParty:*`
- 写仓库菜单前先确认父菜单 `2020` 已存在且签名为 `warehouse / WarehouseManagement`。
- `SqlExecutionGuardContractTest` 新增 `warehouseManagementMenuSeedMustGuardSysMenuSlotsBeforeUpsert`，固定 guard、临时表、父菜单检查和 upsert 前调用顺序。
- `docs/architecture/reuse-ledger.md` 已登记仓库菜单 seed guard 复用规则。
- 目标追踪和相关专项记录中 `admin_partner_role_menu_grant` wildcard 待修口径已改成已收口。

## 残留问题

- 本轮不处理其它旧 `sys_menu` seed 的 slot/signature guard。
- 本轮不处理旧索引 helper 的定义漂移 P2。
- 本轮不执行远程 SQL，不校验远程 `sys_menu` 实际数据。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`27` 个测试通过。

## 未验证原因

- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只改 SQL 文件、静态合同和 Markdown 记录。
- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。

## 权限检查结果

- 本轮只收口管理端仓库菜单 seed 的写入安全，不改变权限点语义。
- 仓库管理仍使用若依 `sys_menu` / `sys_role_menu` 管理端控制面。
- `20260606_admin_partner_role_menu_grant.sql` 当前已是显式 `menu_id` + `perms` 白名单，不再作为待修 wildcard P1。

## 字典/选项复用检查结果

- 本轮未新增字典或前端选项。
- `warehouse_kind`、`country_region`、`us_state` / `us_city` 的既有复用口径不变。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 `warehouse_management_seed.sql` 的父菜单 ready 检查和 `sys_menu` slot/signature guard 规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Synced 1 changed files`，`Modified: 1 - 58 nodes`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行和菜单 owner guard 合同；本轮只追加同类静态合同，不拆分。
- `warehouse_management_seed.sql` 是既有 seed，本轮只补 guard 和静态合同，不拆分仓库表/字典/menu seed。

## 重复代码检查结果

- 仓库菜单 guard 使用现有批量菜单 seed 的临时表 + guard procedure 模式。
- 未新增 Java 业务逻辑或 React 业务逻辑重复。

## 子 Agent 使用记录

- 历史记录（已过期口径）：本轮按当时规则先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent，均因平台额度限制失败并关闭；现行规则为默认使用 `gpt-5.4`。
- 降级使用 6 个 `gpt-5.4` 只读子 Agent，覆盖授权 SQL、仓库 seed、旧 DDL、测试入口、文档更新和 dirty worktree 风险。
- 子 Agent 结论：`warehouse_management_seed.sql` 缺少菜单 slot/signature guard；`admin_partner_role_menu_grant` 当前已白名单化；旧裸 DDL 当前不是 P1。

## 一句话总结

本轮把仓库管理菜单 seed 从“确认 token 后直接 upsert”收口为“确认父菜单 ready + 菜单 ID/signature guard 后再 upsert”，并用静态合同防止回退。
