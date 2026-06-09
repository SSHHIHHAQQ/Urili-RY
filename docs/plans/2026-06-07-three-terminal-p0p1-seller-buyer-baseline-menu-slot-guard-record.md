# 2026-06-07 三端独立 P0/P1 综合 Seed 端内菜单 Slot Guard 记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本切片只收口 `seller_buyer_management_seed.sql` 综合初始化 seed 对 `seller_menu` / `buyer_menu` 的 fail-closed 签名校验，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent

- 历史记录（已过期口径）：先按最新规则尝试启动 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回额度限制，失败 Agent 已关闭。
- 随后回退启动 6 个 `gpt-5.4` 只读子 Agent，覆盖 SQL seed、seller 后端、buyer 后端、React 端隔离、验证入口、日志/会话/免密链路；6 个子 Agent 均已关闭。
- 已采纳 SQL 子 Agent 的 P1：`seller_buyer_management_seed.sql` 基础端内菜单 seed 缺 fail-closed signature guard，且默认 Owner 授权 join 只按 `perms` 绑定。
- 其余只读结论：seller 后端、buyer 后端、React 端隔离、日志/会话/免密链路未发现新的 P0/P1。验证入口子 Agent 指出 `verify-three-terminal.mjs` 仍有验证范围漂移和测试发现过宽问题，本切片记录为后续独立 P1，不混入 SQL guard 修复。

## 已完成

- `seller_buyer_management_seed.sql` 新增 `assert_seller_menu_permission_slot(...)` / `assert_buyer_menu_permission_slot(...)`。
- 综合 seed 写入 10 个 seller 端内权限、10 个 buyer 端内权限前，逐一断言同 `perms` 已有菜单必须保持 `parent_id=0`、`menu_type=F`、空 `path/component/route_name`。
- 综合 seed 给默认 `owner` 角色写 `seller_role_menu` / `buyer_role_menu` 时，菜单 join 增加 `parent_id/menu_type/path/component/route_name` 签名条件，避免历史脏菜单被 Owner 角色绑定。
- `SqlExecutionGuardContractTest.terminalPermissionSeedsMustGuardMenuSlotsBeforeRoleBinding()` 将 `seller_buyer_management_seed.sql` 纳入同一合同测试。
- `docs/architecture/reuse-ledger.md` 已登记综合 seed 和独立增量 seed 共同复用的端内菜单 slot guard 规则，并补齐当前最小端内权限清单。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 35 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`：通过，前端 6 个 Jest suite / 30 个测试通过，后端三端合同链路通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`、`Modified: 1 - 69 nodes`。

## 未执行事项

- 未执行数据库迁移或远程 DDL/DML。
- 未做浏览器运行态验收、截图或 DOM 检测。
- `verify-three-terminal.mjs` 的验证范围漂移和测试发现过宽问题仍是后续 P1；下一切片应单独处理，不与 SQL guard 混在一起。
