# 2026-06-07 三端独立 P0/P1 售后菜单 Parent/Type Guard 记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本切片只收口管理端 `sys_menu` 售后菜单 seed 的 slot guard，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 问题

`RuoYi-Vue/sql/20260605_order_after_sale_menu_seed.sql` 的 `tmp_order_after_sale_sys_menu_guard` 原本只记录 `menu_id/path/component/route_name/perms`。如果历史库里 `2412` 已存在但挂错父级，或菜单类型不是页面菜单 `C`，旧 guard 仍可能把它当成同 ID 同签名菜单并继续 upsert，违反当前 `sys_menu` seed 必须覆盖 `parent_id/menu_type` 的规则。

## 已完成

- `tmp_order_after_sale_sys_menu_guard` 增加 `parent_id` 和 `menu_type`。
- `assert_order_after_sale_sys_menu_guard()` 的同 ID slot 校验增加 `parent_id` 和 `menu_type` 匹配。
- guard seed 签名改为完整菜单签名：`2412 / 2070 / C / after-sale / Common/PlannedPage/index / AfterSaleManagement / order:afterSale:list`。
- `SqlExecutionGuardContractTest.orderAfterSaleMenuSeedMustOwnAndGuardAfterSaleMenu()` 同步锁定 parent/type guard 和完整签名。
- `docs/architecture/reuse-ledger.md` 登记售后菜单 seed 的 parent/type guard 规则。

## 子 Agent 执行情况

- 本轮按用户最新要求优先使用 `gpt-5.3-codex-spark`。
- 实际启动 4 个只读 explorer，分别检查商品分类属性、商城商品列表、来源仓库库存、币种配置四个剩余旧菜单 seed。
- 4 个 explorer 均已完成并关闭；本轮只采纳其“仍存在同类缺口”的残留判断，不让子 Agent 修改文件。
- 未启动 6 个子 Agent 的原因：当前只剩 4 个互不重叠的只读 seed 检查问题，继续拆分会重复。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 34 个测试通过。
- `git diff --check -- RuoYi-Vue\sql\20260605_order_after_sale_menu_seed.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-07-three-terminal-p0p1-order-after-sale-menu-parent-type-guard-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `codegraph sync .`：通过，首次同步输出 `Synced 6 changed files`、`Modified: 6 - 406 nodes`；验证记录回写后复跑输出 `Synced 1 changed files`、`Modified: 1 - 51 nodes`。

## 残留 P1

- `20260604_product_category_attribute_seed.sql` 仍需按同一模式补 `parent_id/menu_type` guard。
- `20260605_mall_product_distribution_seed.sql` 仍需按同一模式补 `parent_id/menu_type` guard。
- `20260606_source_warehouse_stock_menu_rename.sql` 仍需补 `parent_id/menu_type` guard，并保留旧占位组件到正式组件的兼容签名。
- `currency_configuration_seed.sql` 仍需按同一模式补 `parent_id/menu_type` guard。
- 本切片未执行数据库迁移；如后续需要回放 SQL，必须先按激活数据源确认目标环境并设置确认 token。
