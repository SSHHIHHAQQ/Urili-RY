# 2026-06-08 三端 P0/P1：Portal 首页、SQL 精确目标与 Authority Guard 记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮继续执行快速推进模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 历史记录（已过期口径）：当时优先尝试 `gpt-5.3-codex-spark`，部分任务触发额度限制后降级到 `gpt-5.4`；现行规则为默认使用 `gpt-5.4`，除非用户在当前任务重新明确要求。
- 实际采纳结果：
  - 后端 seller/buyer 管理端扫描：未发现新增 P0/P1，并生成 `docs/reviews/2026-06-08-seller-buyer-admin-backend-p0p1-scan.md`。
  - SQL guard 扫描：采纳 2 个 P1。
  - portal 前端扫描：采纳 1 个 P1。
  - verify/contract 扫描：采纳 2 个 P1 覆盖缺口。
  - React 管理端扫描：未发现新增 P0/P1。
- 所有本轮子 Agent 均已关闭；未完成的 portal 后端扫描线程已关闭，未采纳未返回结论。

## 已修复

### Portal 首页异常处理

- `react-ui/src/pages/Portal/Home/index.tsx`
  - `loadData` 不再把普通加载异常当成登录失效。
  - 不再执行 `clearPortalLogin(currentTerminal)`。
  - 不再覆盖全局 401 处理已经写入的 `redirect`。
  - 页面层只提示“门户数据加载失败，请稍后重试”，401 跳转和 token 清理由 request 层统一处理。

### `/getRouters -> authority` 合同

- `react-ui/src/services/session.ts`
  - `convertCompatRouters` 将后端 `perms` 显式转换为 `authority: [perms]`。
  - 缺少 `perms` 时转换为 `authority: []`，继续由 `RemoteMenuRouteGuard` fail-closed。
- 新增 `react-ui/tests/getrouters-authority-contract.test.ts`。
- 新增 `react-ui/tests/portal-home-error-handling.test.ts`。
- `react-ui/tests/three-terminal.manifest.json` 已登记新增测试。
- `react-ui/scripts/verify-three-terminal.mjs` 扩展 critical 前端测试发现关键字。

### SQL Guard

- `RuoYi-Vue/sql/20260605_seller_account_lock_control.sql`
  - 新增 `assert_partner_seller_parent_menu_ready()`。
  - 写入 `2322 seller:admin:account:lock` 前先断言 `2010 -> 2011` 父级菜单签名正确。
- `RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql`
  - 新增 `assert_partner_buyer_parent_menu_ready()`。
  - 写入 `2323 buyer:admin:account:lock` 前先断言 `2010 -> 2012` 父级菜单签名正确。
- `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - 新增 `@admin_partner_role_menu_grant_role_ids`。
  - 新增 expected role count 和 exact role signature。
  - 授权 DML 只作用于预览确认过的 admin `role_id` 集合，避免远端库多个 `role_key='admin'` 时扩散授权。

### 后端合同测试

- `SqlExecutionGuardContractTest`
  - 固定账号锁定 seed 的父级菜单 owner/signature preflight。
  - 固定 partner role-menu grant 的精确 admin role target guard。
- `TerminalRoleMenuMapperIsolationContractTest`
  - 补齐 `select*MenuById`、`hasChildByMenuId`、`checkMenuExistRole` 的当前端表隔离合同。
  - 防止运行时菜单读路径回退到 `sys_menu` 或对端菜单表。

## 验证

- 历史记录（已过期命令口径）：当时执行 `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/getrouters-authority-contract.test.ts tests/portal-home-error-handling.test.ts tests/portal-unauthorized-redirect.test.ts tests/terminal-session-token.test.ts --runInBand`；当前定向 Jest 请使用 `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/getrouters-authority-contract.test.ts tests/portal-home-error-handling.test.ts tests/portal-unauthorized-redirect.test.ts tests/terminal-session-token.test.ts --runInBand`。
  - 通过：4 个 suite / 14 个测试。
  - Jest 仍提示既有 open handle，测试结果本身通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalRoleMenuMapperIsolationContractTest" test`
  - 通过：45 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端：4 个 guard 通过，React typecheck 通过，10 个 Jest suite / 38 个测试通过。
  - 后端：reactor `test-compile` 通过，三端合同测试通过。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留

- `requestErrorConfig.ts` 对非 401 的 `ErrorShowType.REDIRECT` 清 token 行为仍可后续独立收口。
- `getRoutersInfo()` 非 200 时返回空菜单的策略仍可后续独立收口。
- `Portal/Home` 当前只做错误提示，不做页面级重试按钮细调；按本轮 P2 不阻塞。
