# 2026-06-08 三端 P0/P1：管理端启动假登出与端内菜单 ID 重排 Guard 记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮继续执行快速推进模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

> 历史记录（已过期口径）：以下 GPT-5.3 优先尝试记录只描述当时执行情况；现行规则已按用户最新要求改为子 Agent 默认使用 `gpt-5.4`，不再优先使用 GPT-5.3 Codex。

- 按当时用户规则先尝试 `gpt-5.3-codex-spark` 子 Agent。
- 平台返回额度限制：需等到 `2026-06-14 15:12` 后再试。
- 已关闭失败的 GPT-5.3 子 Agent，并按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent。
- 6 个 `gpt-5.4` 子 Agent 均已完成并关闭。

## 采纳的 P1

- 管理端 `/api/getInfo` 非 401 失败时，`getInitialState` 会保留 token，但 `layout.onPageChange` 仅因 `currentUser` 为空就跳到 `/user/login`，形成假登出。
- `20260607_terminal_menu_id_range_isolation.sql` 对端内菜单低位 ID 做主键重排前，没有锁定预览确认的精确目标集合；同时把 `AUTO_INCREMENT` reset DDL 混在主键重排脚本中，MySQL `ALTER TABLE` 隐式提交会破坏脚本想表达的事务边界。

## 已修复

### 管理端启动假登出

- `react-ui/src/app.tsx` 与 `react-ui/src/app.js`：
  - `layout.onPageChange` 改为只有在 `currentUser` 缺失且 admin token 也缺失时才跳登录。
  - 非 401 的用户信息加载失败不再通过页面切换制造假登出。
- `react-ui/tests/portal-unauthorized-redirect.test.ts`：
  - 新增 layout page change 合同：admin token 仍在时不得跳登录。
  - 保留无 admin token 时仍跳登录的合同。

### 端内菜单 ID 重排 Guard

- `RuoYi-Vue/sql/20260607_terminal_menu_id_range_isolation.sql`：
  - 增加四类 expected target 预览锁定变量：`seller_menu`、`seller_role_menu`、`buyer_menu`、`buyer_role_menu`。
  - 增加 `assert_terminal_menu_id_range_expected_targets()`，执行前按 count + SHA-256 signature 校验低位 ID 迁移目标。
  - 移除同脚本内 `AUTO_INCREMENT` reset DDL，使该脚本只负责端内菜单 ID、parent_id 和 role-menu 引用重排。
- 新增 `RuoYi-Vue/sql/20260608_terminal_menu_auto_increment_reset.sql`：
  - 单独确认 `@confirm_terminal_menu_auto_increment_reset = APPLY_TERMINAL_MENU_AUTO_INCREMENT_RESET`。
  - 在最终 ID 范围、parent 和 role-menu 孤儿校验通过后，单独 reset `seller_menu` / `buyer_menu` 的 `AUTO_INCREMENT`。
- `SqlExecutionGuardContractTest` 与 `TerminalSqlIsolationContractTest`：
  - 固定主键重排脚本必须有 expected count/signature。
  - 固定重排脚本不得混入 `AUTO_INCREMENT` reset DDL。
  - 固定 auto_increment reset 必须是独立确认步骤。

## 验证

- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-unauthorized-redirect.test.ts --runInBand`
  - 通过：1 个 suite / 13 个测试。
  - 当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 Jest 二进制。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" test`
  - 通过：58 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端：4 个 guard 通过，React typecheck 通过，10 个 Jest suite / 45 个测试通过。
  - 后端：reactor `test-compile` 通过，三端合同测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过；代码和合同同步时输出 `Synced 5 changed files`，`Modified: 5 - 206 nodes in 832ms`。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
