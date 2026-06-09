# 2026-06-08 三端 P0/P1：直登 Opener Origin 与 SQL 精确目标 Guard 记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮继续执行快速推进模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 历史记录（已过期口径）：按当时用户规则先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；现行规则已改为默认使用 `gpt-5.4`，不要再把 GPT-5.3 Codex 作为首选。
- 平台返回额度限制：需等到 `2026-06-14 15:12` 后再试。
- 已关闭失败的 GPT-5.3 子 Agent，并按当时 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent。
- 6 个 `gpt-5.4` 子 Agent 均已完成并关闭。

## 采纳的 P1

- direct-login 弹窗页只靠 `document.referrer` 推断 opener origin；referrer 被裁掉时，READY/RESULT 可能发到错误 origin，导致免密代入桥接超时。
- `20260607_upstream_task_component_split.sql` 对 `sys_job` 批量改写只有确认 token，没有精确 target count/signature。
- `20260607_upstream_pairing_role_binding.sql` 对上游仓库/渠道配对回填只有确认 token，没有精确 target count/signature。
- `20260608_inventory_overview_sku_baseline_refresh.sql` 对 `inventory_status/NO_SOURCE` 字典更新没有精确 target count/signature。
- `20260608_terminal_menu_auto_increment_reset.sql` 只保证现有 ID 在段内，没有保证下一次 `AUTO_INCREMENT` 不越过 seller/buyer 保留段上界。

## 已完成

- `portalDirectLoginMessage.ts/js`：
  - `openPortalDirectLoginWindow(...)` 打开目标 portal direct-login URL 时追加 `openerOrigin`。
  - 新增 `resolvePortalDirectLoginOpenerOrigin(...)`，弹窗页优先使用显式 opener origin，`document.referrer` 只做兜底。
- `Portal/DirectLogin/index.tsx/js`：
  - READY、TOKEN 校验和 RESULT 回传都使用显式 opener origin。
- `portal-direct-login-message.test.ts`：
  - 固定 direct-login URL 会携带 `openerOrigin`。
  - 固定 referrer 缺失时仍能解析显式 opener origin。
- `check-portal-token-isolation.mjs`：
  - 同步新的 direct-login opener origin 契约，仍禁止 URL token 和 wildcard postMessage。
- `20260607_upstream_task_component_split.sql`：
  - 增加 `@upstream_task_component_split_expected_count/signature` 和 `assert_upstream_task_component_split_targets()`。
- `20260607_upstream_pairing_role_binding.sql`：
  - 增加仓库 role、物流渠道 role、物流渠道仓库编码三组 expected count/signature。
  - 在三段回填 DML 前执行精确目标校验。
- `20260608_inventory_overview_sku_baseline_refresh.sql`：
  - 增加 `inventory_status/NO_SOURCE` 字典行 expected count/signature，并要求该字典行正好存在一行。
- `20260608_terminal_menu_auto_increment_reset.sql`：
  - `reset_terminal_menu_auto_increment(...)` 增加 `p_ceiling_exclusive` 上界。
  - seller 下一自增值必须小于 `200000`，buyer 下一自增值必须小于 `300000`。
- `SqlExecutionGuardContractTest` 和 `TerminalSqlIsolationContractTest`：
  - 固定上述 SQL target guard 和 auto_increment 上界合同。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" test`
  - 通过：61 个测试。
- 历史记录（已过期命令口径）：当时执行 `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-direct-login-message.test.ts --runInBand`；当前定向 Jest 请使用 `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/portal-direct-login-message.test.ts --runInBand`
  - 通过：1 个 suite / 5 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/check-portal-token-isolation.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端：4 个 guard 通过，React typecheck 通过，10 个 Jest suite / 46 个测试通过。
  - 后端：reactor `test-compile` 通过，三端合同测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过；CodeGraph 返回 `Already up to date`。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
