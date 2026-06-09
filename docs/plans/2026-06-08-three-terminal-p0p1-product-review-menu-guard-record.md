# 2026-06-08 三端隔离 P0/P1 快速推进记录：商品审核菜单 Seed Guard

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮仍按快速推进模式执行，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 结论

- 本轮未新建子 Agent；收口的是前序 6 个 `gpt-5.4` 只读子 Agent。
- 历史记录（已过期口径）：后续如需新建子 Agent，按最新规则优先使用 GPT-5.3 Codex；不可用时再回退 `gpt-5.4`。
- seller/buyer 后端隔离、Portal 鉴权和 direct-login、React guard、product/inventory/integration、验证闸门切片均未发现新的可坐实 P0/P1。
- SQL guard 切片发现并采纳 2 个 P1，均位于 `RuoYi-Vue/sql/20260608_product_review.sql`。

## 采纳的 P1

1. `2451` 商品审核页面菜单缺少必须存在的 fail-closed 断言。
   - 风险：`2451` 缺失时，`update sys_menu where menu_id = 2451` 影响 0 行，但按钮菜单仍可能以 `parent_id = 2451` 插入，形成悬空父子关系。

2. `2491-2494` 商品审核按钮菜单缺少固定 `menu_id` 和 `perms` 的 slot/signature guard。
   - 风险：历史库中 ID 或 perms 被占用时，脚本会因 `where not exists` 静默跳过，留下缺按钮、错 owner 或半完成状态。

## 已完成

- 扩展 `tmp_product_review_sys_menu_guard`，从只登记 `2451` 扩展为登记 `2451 + 2491-2494`。
- `assert_product_review_sys_menu_guard` 增加 `2451` 必须存在且签名符合预期的断言。
- `assert_product_review_sys_menu_guard` 增加固定 `menu_id` slot guard 和 `perms` 跨 ID 占用 guard。
- `SqlExecutionGuardContractTest` 新增 `productReviewMenuSeedMustFailClosedForParentAndButtonSlots`，固定父菜单存在性、按钮 slot/perms guard 和 guard 顺序。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，64 个测试。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 同步 4 个变更文件。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
