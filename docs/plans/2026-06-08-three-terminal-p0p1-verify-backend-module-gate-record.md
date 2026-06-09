# 2026-06-08 三端 P0/P1 快速推进：后端验证模块选择闸门收口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

> 当前口径追补（2026-06-09）：本文件中的 GPT-5.3 Codex 相关描述仅代表历史执行事实，当前规则已收紧为子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。本文件验证结果里的“4 个前端 guard”也只是当时快照；当前 `react-ui/tests/three-terminal.manifest.json` 已登记 5 个 frontend guard，`npm run verify:three-terminal` 以当前 manifest 为准。

当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 情况

- 历史记录（已过期口径）：按当时旧规则曾优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`；平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试，6 个失败 Agent 已关闭。现行规则已改为默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。
- 随后按当时 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent。
- 5 个 fallback 切片已返回并关闭：seller/buyer 后端、Portal 鉴权、SQL guard、React 运行入口均未发现确定 P0/P1；验证闸门切片发现并被采纳 1 个 P1。
- product/inventory/integration 共享域切片两次等待未返回；本轮已有确定 P1 和完整验证结果，已关闭该 Agent，未采纳其结果。

## 采纳的 P1

`verify-three-terminal.mjs` 原先按“存在 `src/test/java`”选择后端测试模块，再统一传入 `-Dtest=${backendTests}` 与 `-Dsurefire.failIfNoSpecifiedTests=false`。这会让某些被选中的业务模块没有任何 manifest 测试命中时仍被静默吞掉，削弱三端 P0/P1 验证闸门。

本轮改为：后端合同测试模块由 manifest 中 `backendTestClasses` 反查所属模块，再组 `-pl`；依赖模块仍可通过 `-am` 进入 reactor，但不再作为盲选测试模块证明三端合同。

## 已完成

- `react-ui/scripts/verify-three-terminal.mjs` 新增 `collectBackendTestSources()`，按测试类名收集真实 Java 测试文件。
- `getBackendTestModules()` 改为遍历 manifest 的 `backendTestClasses`，反查测试类所属模块并按 reactor 顺序组装模块清单。
- `assertBackendTestSourcesExist()` 复用同一测试源索引，避免验证逻辑和执行逻辑漂移。
- 新增 `react-ui/tests/verify-three-terminal-backend-gate.test.ts`，用静态契约固定“manifest 测试类反查模块”的验证入口。
- `react-ui/tests/three-terminal.manifest.json` 纳入新增前端契约测试。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests\verify-three-terminal-backend-gate.test.ts --runInBand`：通过，1 个 suite / 1 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；当时快照为 4 个前端 guard、React typecheck、13 个 Jest suite / 67 个测试、后端 reactor `test-compile` 和后端三端合同测试全部通过。当前 manifest 已扩展为 5 个 frontend guard。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；本轮新增 1 个记录文件、修改 1 个目标追踪文件已同步。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- P2：`-am` 依赖模块仍可能输出 `No tests to run`，本轮接受，因为闸门证明范围是 manifest 选中的三端合同模块，不是 Maven 依赖模块。
- P2：product/inventory/integration 共享域子 Agent 未返回，已关闭；本轮没有采纳该切片结果。
