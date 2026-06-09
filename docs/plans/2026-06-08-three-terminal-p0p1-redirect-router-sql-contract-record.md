# 2026-06-08 三端 P0/P1：非 401 登录态、远程菜单失败与 SQL 合同记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮继续执行快速推进模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 历史记录（已过期口径）：按当时用户要求优先启动 6 个 `gpt-5.3-codex-spark` 子 Agent；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度限制：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at Jun 14th, 2026 3:12 PM.`
- 已关闭失败的 GPT-5.3 子 Agent，并按规则回退启动 6 个 `gpt-5.4` 只读扫描。
- 采纳的 P1：
  - `requestErrorConfig` 非 401 `REDIRECT` 会清 token / 跳登录。
  - `app.tsx` 启动、路由切换和首次菜单加载把非 401 异常当成登录失效。
  - `getRoutersInfo()` 非 200 失败策略必须显式失败，避免写入空远程菜单。
  - 账号锁定 SQL seed 的父菜单 preflight 应早于首个 DDL/DML。
  - terminal menu ID range 迁移缺 `parent_id` 孤儿检查。
  - `/getRouters` 的 `perms -> authority` 后端合同缺失。
  - 裸 `accountId` mapper 防守需要从点名方法升级为泛化扫描。
- 未采纳项：React seller/buyer 管理端只读扫描未发现 P0/P1，记录在 `docs/reviews/2026-06-08-react-ui-seller-buyer-p0p1-scan.md`。
- 所有本轮子 Agent 均已关闭。

## 已修复

### 非 401 不再清登录态

- `react-ui/src/requestErrorConfig.ts`
- `react-ui/src/requestErrorConfig.js`
  - `ErrorShowType.REDIRECT` 在非 401 时只展示错误信息，不再执行 `handleUnauthorized(requestUrl)`。
  - 401 仍由 `isUnauthorizedCode(errorCode)` 分支统一清理对应端 token 并跳登录。

- `react-ui/src/app.tsx`
- `react-ui/src/app.js`
  - 新增 `isUnauthorizedError()` / `handleUnauthorizedError()`。
  - `getInitialState()`、`onRouteChange()`、`render()` 中普通 500、网络错误、菜单失败不再清 admin token 或跳登录。
  - 明确 401 仍走 `handleUnauthorizedResponse()`。

### 远程菜单失败显式化

- `react-ui/src/services/session.ts`
  - `getRoutersInfo()` 非 200 不再 `return []`。
  - 非 200 时抛出带 `code/info/response.data` 的错误，方便上层只在明确 401 时清登录态。
  - `convertCompatRouters()` 保持 `perms -> authority: [perms]`，缺失权限时 fail-closed。

### 前端 guard 与测试

- `react-ui/scripts/check-portal-token-isolation.mjs`
  - 固定非 401 `REDIRECT` 不得清 token。
  - 固定 `getRoutersInfo()` 非成功响应必须抛错。
  - 固定 `app.tsx/app.js` 启动和菜单加载链路必须区分 401，不得在泛型 `catch(error)` 里直接清 admin session。
- `react-ui/tests/getrouters-authority-contract.test.ts`
  - 增加 `getRoutersInfo()` 非 200 reject 合同。
- `react-ui/tests/portal-unauthorized-redirect.test.ts`
  - 增加非 401 `REDIRECT` 不清 token、不跳登录合同。
  - 增加 `getInitialState()`、`onRouteChange()`、`render()` 非 401 异常保留 admin token 合同。

### SQL Guard

- `RuoYi-Vue/sql/20260605_seller_account_lock_control.sql`
- `RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql`
  - 将 `assert_partner_*_parent_menu_ready()`、`assert_sys_menu_slot(...)`、`assert_sys_menu_signature_available(...)` 前移到第一个 `add_column_if_missing(...)` 之前。
  - 避免远端缺父菜单或菜单签名冲突时，账号锁定列、索引、字典先半落库。

- `RuoYi-Vue/sql/20260607_terminal_menu_id_range_isolation.sql`
  - 新增 `assert_no_terminal_menu_parent_orphans()`。
  - 迁移前、ID 平移后、auto_increment reset 后都校验 `seller_menu.parent_id` / `buyer_menu.parent_id` 必须能在同表命中。

### 后端合同测试

- `SqlExecutionGuardContractTest`
  - 固定账号锁定 seed 的父菜单和 sys_menu guard 必须早于首个高影响 DDL/DML。
  - 固定 terminal menu ID range 迁移必须包含 parent orphan 检查。
- `TerminalSqlIsolationContractTest`
  - 同步要求最终 commit 前包含 parent orphan 检查。
- `RouterVoPermissionContractTest`
  - 新增 `/getRouters` 后端合同，固定 `RouterVo.perms`、`getPerms/setPerms` 和 `SysMenuServiceImpl.buildMenus()` 的 `menu.getPerms()` 透传。
- `TerminalAccountIsolationTest`
  - 升级裸 `accountId` 防守为生产 Java/XML 泛化扫描：
    - 禁止 controller/service/mapper 暴露账号方法的单参 `Long accountId` 签名。
    - 禁止账号事实表或账号角色表 SQL 只按 `*_account_id` 过滤而没有对应 `seller_id/buyer_id` 约束。
- `react-ui/tests/three-terminal.manifest.json`
  - 新增 `RouterVoPermissionContractTest`。

### 项目规则

- 历史记录（已过期口径）：本记录生成时曾把 `AGENTS.md` 子 Agent 规则写成 GPT-5.3 Codex（工具模型 `gpt-5.3-codex-spark`）优先。
- 当前现行规则：默认使用 `gpt-5.4`，除非用户在当前任务重新明确要求；Markdown 检查点必须记录实际模型、数量、回退原因和结论处理。

## 验证

- 历史记录（已过期命令口径）：当时执行 `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/getrouters-authority-contract.test.ts tests/portal-unauthorized-redirect.test.ts --runInBand`，通过 2 个 suite / 13 个测试；当前定向 Jest 请使用 `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/getrouters-authority-contract.test.ts tests/portal-unauthorized-redirect.test.ts --runInBand`。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalAccountIsolationTest,RouterVoPermissionContractTest" test`
  - 通过：49 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端：4 个 guard 通过，React typecheck 通过，10 个 Jest suite / 43 个测试通过。
  - 后端：reactor `test-compile` 通过，三端合同测试通过。
  - Jest 仍提示既有 open handle，命令退出码为 0。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留

- `Portal/Home` 当前只做错误提示，不做页面级重试按钮细调；按本轮 P2 不阻塞。
- 工作区仍存在库存、商品、demo 图片等既有改动，本轮未读取或修改其业务逻辑。
