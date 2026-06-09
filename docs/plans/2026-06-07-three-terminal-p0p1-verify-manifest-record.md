# 2026-06-07 三端 P0/P1 验证入口 Manifest 化记录

> 当前口径追补（2026-06-09）：本文件中的 GPT-5.3 Codex 优先尝试仅代表 2026-06-07 当时历史执行事实，不再作为后续规则。当前 `AGENTS.md` 已收紧为子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex。当前验证入口以 `react-ui/scripts/verify-three-terminal.mjs` 的 manifest 驱动、动态 reactor 模块发现和仓库级前端测试发现为准。

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理当前 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本轮只收口 `verify-three-terminal` 验证入口清单治理，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 历史执行事实：按 2026-06-07 当时规则优先尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回用量限制，失败 Agent 已关闭。该模型规则已在 2026-06-09 过期。
- 回退使用 6 个 `gpt-5.4` 只读子 Agent，均已关闭。
- 采纳结论：
  - `verify-three-terminal` 清单 manifest 化是当前最适合直接代码收口的 P1。
  - manifest 只能承载声明式清单数据，源码发现、fail-closed、surefire report 校验、执行顺序继续保留在 `.mjs` 执行器。
  - 除验证入口治理外，剩余库存事实源、读模型归属、跨模块事实归属等问题需要设计确认，不适合直接开发。

## 已完成

- 新增 `react-ui/tests/three-terminal.manifest.json`：
  - `backendTestClasses` 承载后端三端关键合同测试清单。
  - `criticalBackendExplicitTestClasses` 承载无法完全靠 critical pattern 自动归类的显式关键测试。
  - `frontendTestPaths` 承载前端三端关键 Jest 清单。
- `react-ui/scripts/verify-three-terminal.mjs` 改为读取 manifest：
  - 校验 manifest `version = 1`。
  - 校验三组清单非空、无重复。
  - 校验 `criticalBackendExplicitTestClasses` 必须包含在 `backendTestClasses` 内。
  - 保留后端源码自动发现、前端测试自动发现、重复类名检查和 surefire XML 检查。
  - 新增 `--check-manifest` 轻量入口，用于只验证 manifest 与源码发现是否一致。
- 错误提示从“未加入 `verify-three-terminal.mjs`”调整为“未加入 three-terminal manifest”。
- 已更新 `docs/architecture/reuse-ledger.md`，把现行规则切到 manifest 口径。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过，输出 `three-terminal manifest check passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端：7 个 Jest suite / 33 个测试通过。
  - 后端：`ruoyi-system` 147、`ruoyi-framework` 15、`integration` 4、`product` 5、`seller` 93、`buyer` 94 个测试通过。
  - surefire report 检查通过，未发现 listed 测试空跑或 skipped。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，本轮触碰文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码变更后输出 `Synced 1 changed files`、`Modified: 1 - 36 nodes`，补写记录后再次运行输出 `Already up to date`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮只是验证入口清单治理，不改变三端业务口径、权限模型或数据库结构。

## 后续规则

- 后续新增三端关键后端合同测试，必须登记到 `react-ui/tests/three-terminal.manifest.json` 的 `backendTestClasses`；如果测试类名不匹配当前 critical pattern，还必须加入 `criticalBackendExplicitTestClasses`。
- 后续新增三端关键前端 Jest，必须登记到 `react-ui/tests/three-terminal.manifest.json` 的 `frontendTestPaths`。
- 更新清单后优先运行 `node scripts\verify-three-terminal.mjs --check-manifest` 做轻量一致性检查；涉及验证入口执行链或关键 P0/P1 改动时，再运行完整 `npm run verify:three-terminal`。

## 残留项

- `ProductDistributionMapper` 跨模块表访问仍需按来源快照、SKU pairing 投影和事实归属方案继续收口。
- 商品库存聚合字段仍需库存事实源、SPU 汇总/去重规则、仓库口径和状态枚举设计确认后再接通。
