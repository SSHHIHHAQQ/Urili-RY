# 2026-06-08 三端隔离 P0/P1 gpt-5.4 文档合同收口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；P2 仅记录，不阻塞。

## 子 Agent 执行情况

- 本轮直接使用 6 个 `gpt-5.4` 子 Agent，不再把 GPT-5.3 Codex 作为首选。
- 6 个切片分别覆盖：seller/buyer 后端隔离、React seller/buyer 管理端模板、portal 登录与直登、SQL seed/guard、verify manifest/gate、Markdown 口径一致性。
- 6 个子 Agent 均已完成并关闭。

## 采纳并修复的 P1

1. `AGENTS.md` 和 `docs/architecture/reuse-ledger.md` 仍有旧口径，表述为跨端或无效 portal 响应可以清理当前页面端 token。
   - 当前实现和 guard 已固定为：普通登录、免密失败、超时、未收到 token、跨端响应或无效响应都只返回失败，不清理任何已有端内 token。
   - 已同步 `AGENTS.md` 和复用台账，避免后续按旧口径回退实现。

2. 3 份旧 review 仍把已修 P1 写成现存缺口，且没有显式过期标记。
   - `docs/reviews/2026-06-08-react-ui-three-terminal-p0p1-readonly-scan.md`
   - `docs/reviews/2026-06-08-react-admin-page-service-route-permission-scan.md`
   - `docs/reviews/2026-06-08-three-terminal-isolation-audit-slice3-readonly.md`
   - 已在文件顶部追加“历史记录（已过期口径）”说明，明确这些 P1 已被后续检查点和当前代码覆盖。

## 未采纳为 P0/P1 的记录项

- seller/buyer 管理端模板中 buyer 的充值占位和余额文案差异属于显式配置差异，不是串端漂移。
- seller/buyer service 函数声明顺序不完全一致，只增加人工 diff 噪音，不影响 URL/权限语义。
- `PortalDirectLoginSupport` 仍保留 legacy key 删除逻辑，但当前无读取依赖，属于迁移残留清理。
- `verify-three-terminal` 的 `guard:` 前缀外脚本不自动纳入 manifest，当前不构成现状 P0/P1。
- SQL 审计未做 live 运行库只读核验，属于运行态残余风险，不是当前代码级 P0/P1。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`：通过，`Portal token isolation guard passed.`
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过，`three-terminal manifest check passed.`
- `cd E:\Urili-Ruoyi\react-ui; cmd /d /s /c ".\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/terminal-session-token.test.ts tests/portal-direct-login-message.test.ts tests/portal-unauthorized-redirect.test.ts --runInBand"`：通过，3 个 suite / 25 个测试。
- 旧 portal token 口径未标过期搜索：通过，未发现未标过期的 stale 命中。
- 3 份旧 review 顶部过期标记检查：通过。

## CodeGraph 追补

- 2026-06-09 后续记录层复核发现：本文件记录过 `AGENTS.md` 和 `docs/architecture/reuse-ledger.md` 口径同步，但当时未在本文件中写入 CodeGraph 结果。
- 追补口径：本轮已在仓库根目录执行 `codegraph sync .`，同步结果以 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 和 `docs/plans/2026-06-09-three-terminal-p0p1-gpt54-fast-pass-record.md` 最新检查点为准。

## 未执行

- 未执行远端 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。
