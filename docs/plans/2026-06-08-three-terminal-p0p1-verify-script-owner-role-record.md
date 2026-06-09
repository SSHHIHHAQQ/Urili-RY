# 2026-06-08 三端 P0/P1 快速推进：验证入口与 Owner 角色 Guard

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

> 当前口径追补（2026-06-09）：本文件中的 GPT-5.3 Codex 相关描述仅代表历史执行事实，当前规则已收紧为子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。本文件验证结果里的“4 个前端 guard”也只是当时快照；当前 `react-ui/tests/three-terminal.manifest.json` 已登记 5 个 frontend guard，`npm run verify:three-terminal` 以当前 manifest 为准。

当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 情况

- 本轮收口的是已启动的 6 个 `gpt-5.4` 只读子 Agent，均已关闭。
- 历史记录（已过期口径）：当时旧规则曾要求后续新增子 Agent 优先使用 GPT-5.3 Codex（工具模型 `gpt-5.3-codex-spark`），不可用时再回退 `gpt-5.4`；现行规则已改为默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。
- 采纳的 P1：
  - `react-ui/package.json` 公开 `test:unit` 仍可直接跑 raw Jest，绕过 `verify:three-terminal` 的 manifest、guard、typecheck 和后端合同闭环。
  - `seller_buyer_management_seed.sql` 在 `PATCH_EXISTING` 场景下可能遇到 active seller/buyer 下已有失活或删除态 `owner` 角色，seed 不新建可用 owner，后续 owner 账号/角色菜单授权静默缺失。
- 未发现新的确定 P0。

## 已完成

- `react-ui/scripts/verify-three-terminal.mjs`
  - 公开测试脚本自检新增 `assertPublicTestScriptsUseThreeTerminalVerifier()`。
  - 内部三端关键 Jest 不再通过 `npm run test:unit` 间接调用，而是直接调用本地 `node_modules/.bin/jest`。
  - 只允许 `--coverage`、`-u`、`--updateSnapshot` 转发到 Jest；其他参数 fail-closed，避免重新出现按路径缩小关键测试集的绕过口。
- `react-ui/package.json`
  - `test`、`test:coverage`、`test:update`、`test:unit`、`jest` 统一走 `verify:three-terminal`。
  - 不再暴露 raw Jest 的公开 npm script。
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - 新增 `assert_terminal_owner_role_slots_ready()`。
  - active seller/buyer 下已有 `role_key='owner'` 但 `status <> '0'` 或 `del_flag <> '0'` 时直接 `45000`。
  - owner 账号绑定角色时同步要求 `r.status = '0'` 和 `r.del_flag = '0'`。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 新增 `sellerBuyerManagementSeedMustFailClosedOnInactiveTerminalOwnerRoles()`，固定 Owner 角色 guard、调用顺序和账号角色绑定条件。
- `docs/architecture/reuse-ledger.md`
  - 更新三端验证入口规则：公开测试入口不得绕过 `verify:three-terminal`，raw Jest 不再作为公开脚本提供。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过，55 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 当时快照为 4 个前端 guard 通过；当前 manifest 已扩展为 5 个 frontend guard。
  - React typecheck 通过。
  - 12 个 Jest suite / 66 个测试通过，并生成 `node_modules\.cache\three-terminal-jest-results.json`。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## P2 记录

- `JwtAuthenticationTokenFilter` 仍可能在 portal 匿名路由解析后台 admin JWT 并写入 `SecurityContext`；当前 portal 权限链路实际走 `PortalTokenSupport` / `PortalPreAuthorize`，本轮不升 P1。
- seller/buyer 端内 `select*MenuById` / `delete*MenuById` 仍以物理端表内裸 `menuId` 操作；当前端内菜单是端级共享模板，后续如果改成主体私有菜单，需要第一批收口。
- React 侧仍有部分手工 JS mirror 和缺少 sidecar 的页面/service，当前未发现已成立串端或权限绕过，后续按 P2 清理。
- `SourceProductLibrary` 前端仍有 `THIRD_PARTY_MASTER` 占位 tab，但后端读模型默认只维护 `OFFICIAL_MASTER`；当前视为能力未闭合，不作为 P0/P1 阻塞。
- `AdminSourceProductController` 仍使用已被现有 seed/合同固化的 `product:list:list` 权限命名；后续如果治理 integration 权限命名，需要单独迁移菜单、路由 authority 和合同测试。
