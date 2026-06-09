# 三端隔离 P0/P1 快速推进：前端 Jest 结果 fail-closed 守门记录

日期：2026-06-08

## 参考方向

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：

- 编译
- guard
- 接口
- 权限
- 串端
- service/字段缺失

本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：按最新规则，优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`。
- 平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试；6 个失败的 5.3 子 Agent 已关闭。
- 按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent；6 个已全部完成并关闭。
- 主 Agent 只采纳确定 P1，P2 记录但不阻塞。

## 采纳的 P1

`react-ui/scripts/verify-three-terminal.mjs` 之前只把关键前端测试文件传给 Jest，但没有在 Jest 真实执行结果上 fail-closed：

- 如果 Jest JSON 结果缺失，无法被脚本显式识别。
- 如果 manifest 中的测试文件未实际出现在 Jest 结果中，脚本无法识别。
- 如果测试被 skip/todo/pending，存在被误认为通过的风险。
- 如果输出文件沿用旧结果，存在 stale report 风险。

该问题属于三端隔离公共验证入口缺口，按 P1 处理。

## 已完成

- 新增 `node_modules/.cache/three-terminal-jest-results.json` 作为 `verify-three-terminal` 的前端 Jest 结果文件。
- 每次运行前先删除旧 JSON，避免 stale report。
- 前端 Jest 运行后校验：
  - manifest 中每个关键前端测试文件必须出现在 Jest 结果中。
  - 每个关键测试文件必须 `status = passed`。
  - 每个关键测试文件至少有一个实际通过的断言。
  - 不允许 pending / todo / skipped 测试。
  - 顶层 Jest 统计必须有实际执行并通过的测试。
- `portal session unit tests` 步骤增加 `--json --outputFile`，并在 `after` 阶段执行结果校验。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 4 个前端 guard 通过。
  - React typecheck 通过。
  - Jest 12 个 suite / 65 个测试通过，并生成 `node_modules\.cache\three-terminal-jest-results.json`。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## P2 残留

- 卖家/买家后台锁定、重置密码 request DTO 仍偏宽，当前未发现 P0/P1 串端。
- React portal denied-path 弹窗链路仍可补更细的单元测试。
- JS sidecar 体积偏大，当前以守门脚本约束镜像漂移，不做结构性拆分。
- portal 401 helper 有重复，可后续抽公共实现。
- direct-login bridge 失败提示仍偏通用，可后续改善文案。
- 非日期前缀 mutating SQL 仍依赖显式 allowlist 维护。
- `permission.js` 镜像可后续纳入更完整的 public verify 覆盖。
