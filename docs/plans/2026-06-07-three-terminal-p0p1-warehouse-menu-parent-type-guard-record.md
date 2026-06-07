# 2026-06-07 三端 P0/P1 仓库菜单 Parent/Type Guard 收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本切片聚焦 `warehouse_management_seed.sql` 的管理端 `sys_menu` slot guard，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 问题

`warehouse_management_seed.sql` 已有 `tmp_warehouse_management_sys_menu_guard` 和 `assert_warehouse_management_sys_menu_guard()`，但同 ID slot 校验只覆盖：

- `path`
- `component`
- `route_name`
- `perms`

它没有覆盖 `parent_id` 和 `menu_type`。如果历史库中同一个 `menu_id` 使用了相同技术签名但挂错父级，或菜单类型从 `C/F` 漂移，脚本仍可能进入 `on duplicate key update` 并静默改写。

## 已完成

- `RuoYi-Vue/sql/warehouse_management_seed.sql`
  - `tmp_warehouse_management_sys_menu_guard` 增加 `parent_id` 和 `menu_type`。
  - `assert_warehouse_management_sys_menu_guard()` 的同 ID slot 校验增加：
    - `m.parent_id <> seed.parent_id`
    - `coalesce(m.menu_type, '') <> coalesce(seed.menu_type, '')`
  - guard 清单补齐 `2021/2022` 页面菜单和 `202101-202105/202201-202204` 按钮菜单的父级和类型。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `warehouseManagementMenuSeedMustGuardSysMenuSlotsBeforeUpsert()` 增加 parent/type 断言。
  - 同步更新仓库菜单 guard 清单的完整签名。
- `docs/architecture/reuse-ledger.md`
  - 补充仓库菜单 seed guard 必须覆盖 `parent_id/menu_type` 的复用规则。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
  - 追加本检查点。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`34` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue\sql\warehouse_management_seed.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-07-three-terminal-p0p1-warehouse-menu-parent-type-guard-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码和合同同步时为 `Synced 6 changed files`，记录回写后复跑为 `Synced 1 changed files`。

## 未执行事项

- 未执行数据库 DDL/DML；本轮只做 SQL seed 静态 guard 和合同测试。
- 未做浏览器运行态验收、截图或 DOM 检测，符合本轮快速推进边界。
