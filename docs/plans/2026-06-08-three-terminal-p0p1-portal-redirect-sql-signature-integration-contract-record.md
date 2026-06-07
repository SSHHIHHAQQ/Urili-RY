# 2026-06-08 三端 P0/P1：Portal Redirect、SQL 目标签名与 Integration 合同记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮继续按快速推进口径执行：只处理 P0/P1，也就是编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 按用户最新规则先尝试 `gpt-5.3-codex-spark` 子 Agent。
- 平台返回 GPT-5.3 Codex Spark 用量限制，失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 `gpt-5.4` 只读子 Agent，共收敛 8 份结果。
- 采纳的 P1：
  - portal path 识别过宽，`/seller/accounts`、`/buyer/roles` 这类管理端路径可能被登录页 redirect 当成 portal 路径。
  - 两个 admin partner cleanup SQL 只确认数量，没有确认精确目标集合签名。
  - `seller_menu` / `buyer_menu` 依赖 `perms` 绑定角色，但 seed/migration 没有全局唯一索引和端前缀污染 guard。
  - integration admin route/service 合同没有进入 critical manifest。
- 暂不在本切片落地：
  - 将所有管理端业务菜单路径迁移到显式 `/admin` 前端命名空间。该项会牵动历史 `sys_menu.path`、seed、静态路由和跳转路径，当前无 seller/buyer 串端证据，先记录为后续架构清理项。

## 已完成

- Portal path 收窄：
  - `getPortalTerminalFromPath(...)` 只识别 `/seller/login`、`/seller/direct-login`、`/seller/portal` 及 buyer 对应路径。
  - 新增 `isPortalTerminalPath(...)`，登录页 redirect 只允许当前端白名单路径。
  - 登录页拒绝回跳当前端 login/direct-login，避免登录循环。
- Portal guard/test：
  - `check-portal-token-isolation.mjs` 固定白名单 redirect 规则。
  - `portal-session-request.test.ts` 增加 `/seller/accounts`、`/seller`、`/buyer/admin/menus` 非 portal 路径合同。
- SQL fail-closed：
  - `20260606_admin_partner_non_admin_button_grant_cleanup.sql` 新增 `@admin_partner_button_cleanup_expected_signature`，执行前按 `role_id/menu_id/perms` 重算 SHA-256。
  - `20260607_admin_partner_owner_reset_permission_cleanup.sql` 新增 role-menu 和 menu 两个 SHA-256 目标签名。
  - `SqlExecutionGuardContractTest` 固定两个 cleanup 必须同时校验数量和签名。
- 端内菜单 perms guard：
  - `seller_buyer_management_seed.sql` 和 `20260604_three_terminal_isolation_migration.sql` fresh DDL 新增 `uk_seller_menu_perms` / `uk_buyer_menu_perms`。
  - migration 对已有表重建并断言唯一索引。
  - seed/migration 在角色菜单授权前 fail-closed 检查端前缀、禁止 `*`、禁止 `seller:admin:` / `buyer:admin:`、页面菜单 component 必填、perms 不得重复。
- Integration 合同：
  - 新增 `IntegrationAdminRouteContractTest`，固定 integration 三个 admin controller 和 React service 只能使用 `/integration/admin/**` / `/api/integration/admin/**`。
  - `IntegrationAdminRouteContractTest` 已加入 `react-ui/tests/three-terminal.manifest.json` 的 backend 与 critical 列表。
- 规则沉淀：
  - `AGENTS.md` 新增 portal 登录 redirect 白名单规则。
  - `docs/architecture/reuse-ledger.md` 和目标追踪同步本轮结论。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-session-request.test.ts tests/terminal-session-token.test.ts --runInBand`：通过，2 个 suite / 9 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,IntegrationAdminRouteContractTest" test`：通过，43 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalSqlIsolationContractTest,IntegrationAdminRouteContractTest,SqlExecutionGuardContractTest" test`：通过，55 个测试。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；只有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留

- 管理端裸业务前端路径统一迁移到显式 admin namespace 仍是后续架构清理项，不作为当前 P0/P1 阻塞。
- 运行态 403/200 对照、按钮显隐和浏览器菜单缓存验证仍不是当前快速推进阻塞项。
- 商品配对投影下沉、库存事实源与聚合口径仍按后续设计继续推进。
