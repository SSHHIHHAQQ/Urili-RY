# 2026-06-07 三端独立 P0/P1 来源仓库库存菜单 Parent/Type Guard 记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本切片只收口管理端 `sys_menu` 来源仓库库存菜单 seed 的 slot guard，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 问题

`RuoYi-Vue/sql/20260606_source_warehouse_stock_menu_rename.sql` 的 `tmp_source_warehouse_stock_sys_menu_guard` 原本只记录 `menu_id/path/component/route_name/perms`。该脚本允许 `2421` 从旧占位组件升级到正式来源仓库库存页面，但旧 guard 没有锁定 `parent_id/menu_type`，无法阻止同 ID 菜单挂错父级或类型错误后被静默更新。

## 已完成

- `tmp_source_warehouse_stock_sys_menu_guard` 增加 `parent_id` 和 `menu_type`。
- `assert_source_warehouse_stock_sys_menu_guard()` 的同 ID slot 校验增加 `parent_id` 和 `menu_type` 匹配。
- 保留两条 `2421` 允许签名：正式组件 `Inventory/SourceWarehouseStock/index` 和历史占位组件 `Common/PlannedPage/index`。
- 两条允许签名都锁定为 `parent_id=2080`、`menu_type=C`。
- `SqlExecutionGuardContractTest.sourceWarehouseStockMenuSeedMustOwnAndGuardSourceWarehouseStockMenu()` 同步锁定 parent/type guard 和两条兼容签名。
- `docs/architecture/reuse-ledger.md` 登记来源仓库库存菜单 seed 的 parent/type guard 规则。

## 子 Agent 执行情况

- 本切片复用同日上一切片的只读 explorer 结论；未新增子 Agent。
- 该 explorer 使用 `gpt-5.3-codex-spark`，只读检查后已关闭，未改文件、未执行测试。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 34 个测试通过。
- `git diff --check -- RuoYi-Vue\sql\20260606_source_warehouse_stock_menu_rename.sql RuoYi-Vue\sql\currency_configuration_seed.sql RuoYi-Vue\sql\20260605_mall_product_distribution_seed.sql RuoYi-Vue\sql\20260605_order_after_sale_menu_seed.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-07-three-terminal-p0p1-source-warehouse-stock-menu-parent-type-guard-record.md docs\plans\2026-06-07-three-terminal-p0p1-currency-menu-parent-type-guard-record.md docs\plans\2026-06-07-three-terminal-p0p1-mall-product-distribution-menu-parent-type-guard-record.md docs\plans\2026-06-07-three-terminal-p0p1-order-after-sale-menu-parent-type-guard-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `codegraph sync .`：通过，输出 `Synced 1 changed files`、`Modified: 1 - 66 nodes`。

## 残留 P1

- `20260604_product_category_attribute_seed.sql` 仍需按同一模式补 `parent_id/menu_type` guard。
- 本切片未执行数据库迁移；如后续需要回放 SQL，必须先按激活数据源确认目标环境并设置确认 token。
