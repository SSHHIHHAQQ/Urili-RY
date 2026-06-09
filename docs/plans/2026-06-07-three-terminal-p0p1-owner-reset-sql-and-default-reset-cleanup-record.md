# 2026-06-07 三端 P0/P1 主体级重置权限与默认重置接口收口记录

## 背景

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

子 Agent 按最新规则先尝试 `gpt-5.3-codex-spark`，平台返回额度限制后已关闭失败 Agent，并回退 6 个 `gpt-5.4` 只读 Agent。回退 Agent 覆盖 SQL owner reset、integration 验证缺口、React guard、后端 runtime、端内菜单 fail-closed、Markdown/AGENTS 口径。（历史记录，已过期口径）

## 问题

- P1：`seller_buyer_management_seed.sql` 与管理端授权脚本仍会写入或授权废弃主体级权限 `seller:admin:resetPwd` / `buyer:admin:resetPwd`，虽然生产 Controller 和 React 已不再使用。
- P1：后端和前端配置仍保留账号级 `resetDefaultPwd` 默认密码重置通道，与当前“管理端重置密码必须人工输入 5-20 位临时密码并调用 `resetPwd`”口径冲突。

## 已完成

- 从 `seller_buyer_management_seed.sql` 移除废弃主体级按钮 `2204/2214` 及 `seller:admin:resetPwd` / `buyer:admin:resetPwd`。
- 从 `20260606_admin_partner_role_menu_grant.sql` 和 `20260606_admin_partner_non_admin_button_grant_cleanup.sql` 的按钮白名单中移除 `2204/2214` 与主体级 reset 权限。
- 新增 `20260607_admin_partner_owner_reset_permission_cleanup.sql`，用于后续按确认流程清理已运行库中的旧 `sys_menu` 与 `sys_role_menu` 残留。脚本要求 confirm token、预览后的预期 role-menu 删除数量和 menu 删除数量，且只删除签名完全匹配的旧按钮。
- 移除 `AdminSellerController` / `AdminBuyerController` 的 `/{sellerId|buyerId}/accounts/{accountId}/resetDefaultPwd` 路由。
- 移除 `ISellerService` / `IBuyerService` 与 `SellerServiceImpl` / `BuyerServiceImpl` 中的默认密码重置方法。
- 移除 React seller/buyer page 配置和 service 中的 `resetAdmin*AccountDefaultPassword` / `resetDefaultPwd`。
- 更新 `check-partner-management-template.mjs`、`AdminAccountPermissionUiContractTest`、`SellerAdminPermissionContractTest`、`BuyerAdminPermissionContractTest`、`TerminalSeedPermissionContractTest`、`AdminDirectLoginPermissionContractTest` 和 `SqlExecutionGuardContractTest`，禁止上述入口和 seed 回归。
- 更新 `AGENTS.md` 和 `docs/architecture/reuse-ledger.md`：当前实现不保留 `resetDefaultPwd`，创建账号默认密码 `U12346` 规则不等同于重置已有账号为默认密码。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,ruoyi-framework -am "-Dtest=SqlExecutionGuardContractTest,TerminalSeedPermissionContractTest,AdminDirectLoginPermissionContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest,PermissionServiceAccountPermissionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，54 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=AdminAccountPermissionUiContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，109 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 guard、TypeScript、6 个 Jest suite / 30 个测试、后端 reactor test-compile 和后端三端合同链路均通过。Jest 仍输出 open handles 提示，但命令退出码为 0。

## 未执行

- 未执行远程 MySQL DDL/DML；新增清理 SQL 只是落盘和合同验证，真正执行前仍需按数据源确认流程预览并设置预期数量。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图或 DOM 验收。

## 残留

- `integration` 模块仍只有 reactor compile 覆盖，没有模块内 surefire report 覆盖；本轮只记录为后续 P1，不阻塞当前权限与接口收口。
- 历史 Markdown 中仍保留旧阶段事实，例如“默认密码重置”曾经接入；以后引用状态时以本记录和目标追踪最新检查点为准。
