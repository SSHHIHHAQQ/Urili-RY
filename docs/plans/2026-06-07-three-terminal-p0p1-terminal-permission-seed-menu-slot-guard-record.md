# 2026-06-07 三端独立 P0/P1 端内权限 Seed 菜单 Slot Guard 记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本切片只收口 seller/buyer 端内权限增量 seed 写 `seller_menu` / `buyer_menu` 时的 fail-closed guard，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 问题

多个端内权限增量 seed 原先只通过 `where not exists (...) where perms = ...` 判断菜单是否已存在，并在授权时只按 `m.perms` join 到 `seller_menu` / `buyer_menu`。如果历史库里已有同 `perms` 但父级、类型、路径、组件或 route 错误的菜单，脚本会静默复用并把该错误菜单绑定给端内角色。

## 已完成

- `20260604_portal_account_list_permission_seed.sql` 增加 seller/buyer 菜单签名断言，并在 role-menu 授权 join 中加入 `parent_id/menu_type/path/component/route_name` 条件。
- `20260604_portal_dept_role_list_permission_seed.sql` 按同一模板收口 `dept:list` / `role:list` 权限。
- `20260604_portal_product_category_permission_seed.sql` 按同一模板收口商品分类只读权限。
- `20260604_seller_product_schema_permission_seed.sql` 先处理卖家商品 schema 权限。
- `20260604_buyer_product_schema_permission_seed.sql` 按卖家模板机械复制买家商品 schema 权限。
- `20260607_portal_self_audit_permission_seed.sql` 收口端内本人登录日志、操作日志和会话列表三组自助审计权限。
- `SqlExecutionGuardContractTest` 新增 `terminalPermissionSeedsMustGuardMenuSlotsBeforeRoleBinding()`，锁住独立增量权限 seed 必须先断言菜单签名，并且授权 join 也必须带签名条件。
- `docs/architecture/reuse-ledger.md` 已登记端内权限增量 seed 的 fail-closed 菜单 slot guard 模板。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 35 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`：通过，前端 6 个 Jest suite / 30 个测试通过，后端三端合同链路通过。
- `git diff --check`：通过，仅有 LF/CRLF 归一化提示。
- `codegraph sync .`：通过，输出 `Synced 1 changed files`、`Modified: 1 - 69 nodes`。

## 未执行事项

- 未执行数据库迁移或远程 DDL/DML。
- 未做浏览器运行态验收、截图或 DOM 检测。
- 本切片只处理已列出的端内权限增量 seed；后续如果新增端内权限 seed，必须复用同一 guard 模板并纳入合同测试。
