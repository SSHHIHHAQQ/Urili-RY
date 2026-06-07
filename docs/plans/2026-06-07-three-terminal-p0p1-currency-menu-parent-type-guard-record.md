# 2026-06-07 三端独立 P0/P1 币种菜单 Parent/Type Guard 记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本切片只收口管理端 `sys_menu` 币种配置菜单 seed 的 slot guard，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 问题

`RuoYi-Vue/sql/currency_configuration_seed.sql` 的 `tmp_currency_configuration_sys_menu_guard` 原本只记录 `menu_id/path/component/route_name/perms`。如果历史库里 `2442` 或 `2460-2466` 已存在但挂错父级，或菜单类型错误，旧 guard 仍可能把它当成同 ID 同签名菜单并继续 upsert，违反当前 `sys_menu` seed 必须覆盖 `parent_id/menu_type` 的规则。

## 已完成

- `tmp_currency_configuration_sys_menu_guard` 增加 `parent_id` 和 `menu_type`。
- `assert_currency_configuration_sys_menu_guard()` 的同 ID slot 校验增加 `parent_id` 和 `menu_type` 匹配。
- `2442` 保留正式页签名和历史占位签名两条允许路径，但两条都锁定为 `parent_id=2050`、`menu_type=C`。
- `2460-2466` 按钮菜单 guard 签名补齐为 `parent_id=2442`、`menu_type=F`。
- `SqlExecutionGuardContractTest.currencyMenuSeedMustHaveSingleFinanceOwnerAndGuardSysMenuSlots()` 同步锁定 parent/type guard 和关键完整签名。
- `docs/architecture/reuse-ledger.md` 登记币种菜单 seed 的 parent/type guard 规则。

## 子 Agent 执行情况

- 本切片复用同日上一切片的只读 explorer 结论；未新增子 Agent。
- 该 explorer 使用 `gpt-5.3-codex-spark`，只读检查后已关闭，未改文件、未执行测试。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 34 个测试通过。
- `git diff --check -- RuoYi-Vue\sql\currency_configuration_seed.sql RuoYi-Vue\sql\20260605_mall_product_distribution_seed.sql RuoYi-Vue\sql\20260605_order_after_sale_menu_seed.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-07-three-terminal-p0p1-currency-menu-parent-type-guard-record.md docs\plans\2026-06-07-three-terminal-p0p1-mall-product-distribution-menu-parent-type-guard-record.md docs\plans\2026-06-07-three-terminal-p0p1-order-after-sale-menu-parent-type-guard-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `codegraph sync .`：通过，输出 `Synced 1 changed files`、`Modified: 1 - 66 nodes`。

## 残留 P1

- `20260604_product_category_attribute_seed.sql` 仍需按同一模式补 `parent_id/menu_type` guard。
- `20260606_source_warehouse_stock_menu_rename.sql` 仍需补 `parent_id/menu_type` guard，并保留旧占位组件到正式组件的兼容签名。
- 本切片未执行数据库迁移；如后续需要回放 SQL，必须先按激活数据源确认目标环境并设置确认 token。
