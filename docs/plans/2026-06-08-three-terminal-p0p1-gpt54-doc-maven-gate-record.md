# 2026-06-08 三端 P0/P1 gpt-5.4 扫描、文档口径与 Maven Gate 收口记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 本轮按用户最新要求直接启动 6 个 `gpt-5.4` 只读子 Agent。
- 覆盖范围：seller/buyer 后端隔离、portal auth/direct-login/session/log、SQL guard、React route/service mirror、共享业务域、verifier/manifest/docs。
- 6 个子 Agent 均已完成并关闭。

## 已确认结果

- 未确认新的代码级 P0。
- seller/buyer 后端隔离、portal auth、SQL guard、React route/service mirror 4 个分面未确认新的 P0/P1。
- 共享业务域分面确认 P1：不带 `-am` 的窄范围 Maven 命令会因为 reactor 内部依赖或旧本地 artifact 导致误爆，不能作为当前门禁。
- verifier/docs 分面确认 P1：部分近期 Markdown 仍把 GPT-5.3 或旧 `npm run test:unit -- --runTestsByPath ... --runInBand` 当成现行口径。

## 已修复

- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
  - 将被点名的 Integration Fresh Bootstrap 检查点中 GPT-5.3 优先表述标为历史过期口径。
- `docs/plans/2026-06-08-three-terminal-p0p1-redirect-router-sql-contract-record.md`
  - 将“AGENTS.md 子 Agent 规则改为 GPT-5.3 优先”改为历史过期口径，并补当前默认 `gpt-5.4`。
- `docs/plans/2026-06-08-three-terminal-p0p1-permission-sql-js-mirror-record.md`
  - 将 GPT-5.3 优先表述标为历史过期口径。
  - 将旧 `npm run test:unit -- --runTestsByPath ... --runInBand` 命令标为历史过期命令口径，并补当前显式 Jest 命令。
- `docs/plans/2026-06-08-three-terminal-p0p1-portal-home-sql-authority-guard-record.md`
  - 将 GPT-5.3 优先表述标为历史过期口径。
  - 将旧 `npm run test:unit -- --runTestsByPath ... --runInBand` 命令标为历史过期命令口径，并补当前显式 Jest 命令。
- `docs/plans/2026-06-08-three-terminal-p0p1-source-product-library-sql-seed-guard-record.md`
  - 将 GPT-5.3 优先表述标为历史过期口径。
- `docs/architecture/reuse-ledger.md`
  - 补充 Maven 窄范围验证模板：`product`、`integration`、`seller`、`buyer` 等有 reactor 内部依赖的模块，定向测试必须使用 `-am`。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端 guard 通过。
  - TypeScript typecheck 通过。
  - Jest `21/21` suites、`156` tests 通过。
  - 后端 reactor test-compile 通过。
  - 后端三端合同测试通过。
- 本记录落地后的 `git diff --check` 和 `codegraph sync .` 结果以后续记录或最终回复为准。

## 未执行

- 未执行远端 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## P2 留存

- SQL guard 未来可增强动态 DML 识别、confirm 变量和 assert 过程绑定、portal URL 非空/形态检查。
- 端内用户名全局唯一可补数据库约束和契约测试。
- React `401` 分流逻辑可抽成共享 helper，减少 `app.tsx` 与 `requestErrorConfig.ts` 的漂移风险。
- 普通登录/direct-login 不预清 token 可补 runtime 级测试，而不只靠源码文本守卫。
