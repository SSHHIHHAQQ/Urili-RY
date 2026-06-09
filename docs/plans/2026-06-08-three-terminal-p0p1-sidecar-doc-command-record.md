# 2026-06-08 三端 P0/P1 Sidecar Mirror 与旧命令口径收口记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 本轮按用户最新要求直接使用 6 个 `gpt-5.4` 只读子 Agent。
- 覆盖范围：seller/buyer 后端隔离、portal auth/direct-login/session/log、SQL guard、React route/service mirror、共享业务域、verifier/manifest/docs。
- 6 个子 Agent 均已完成并关闭。

## 已处理 P1

- React sidecar 合同补齐：
  - `src/pages/Portal/terminal.js`
  - `src/pages/Portal/Home/index.js`
  - `src/pages/Portal/Login/index.js`
  - `src/pages/Portal/DirectLogin/index.js`
  - `src/services/seller/seller.js`
  - `src/services/buyer/buyer.js`
- 文档口径收口：
  - `docs/plans/2026-06-08-three-terminal-p0p1-direct-login-sql-target-guard-record.md` 中 GPT-5.3 优先尝试标为过期历史。
  - `docs/plans/2026-06-07-three-terminal-p0p1-subagent-contract-cleanup-record.md` 中 GPT-5.3 优先尝试标为过期历史。
  - `docs/reviews/2026-06-07-react-ui-three-terminal-service-url-readonly-scan.md` 中旧 `npm run test:unit -- --runTestsByPath ... --runInBand` 命令标为过期，并补当前显式 Jest 命令。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/admin-auth-sidecar-contract.test.ts --runInBand`：通过，1 个 Jest suite / 28 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端 guard 通过。
  - TypeScript typecheck 通过。
  - Jest `21/21` suites、`156` tests 通过。
  - 后端 reactor test-compile 通过。
  - 后端三端合同测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅输出工作区 LF/CRLF 换行风格 warning，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：首次同步通过，输出 `Synced 1 changed files`；本记录回填后会再次同步，最终结果以最终回复为准。

## 未执行

- 未执行远端 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## P2 留存

- SQL guard 未来可增强递归发现与动态 DML 识别。
- direct-login terminal mismatch 可补更直接的 session/log 未写入测试。
- portal 自助接口可补 API 级敏感字段序列化测试。
- reuse-ledger 个别手工命令口径可后续更新为“以 verifier 动态派生模块为准”。
