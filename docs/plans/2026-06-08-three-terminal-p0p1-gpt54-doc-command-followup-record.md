# 2026-06-08 三端隔离 P0/P1 gpt-5.4 文档命令口径追补记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮继续按快速推进模式处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 按用户最新要求，本轮 6 个子 Agent 全部直接使用 `gpt-5.4`，未再使用 GPT-5.3 Codex。
- 6 个子 Agent 覆盖 seller/buyer 后端隔离、portal auth/direct-login/session/log、SQL guard、React route/access/request/session、共享业务域、文档/验证口径。
- 6 个子 Agent 均已完成并关闭。
- 代码级结论：seller/buyer 后端隔离、portal 链路、SQL guard、React guard、共享业务域均未确认新的 P0/P1。
- 采纳的 P1：部分近期 Markdown 仍把已失效的 `npm run test:unit -- --runTestsByPath ... --runInBand` 或不带 `-am` 的 product Maven 窄范围命令当成当前可复核命令；少量记录仍把 GPT-5.3 Codex 写成当前默认规则或缺少子 Agent 关闭状态 / CodeGraph 追补说明。

## 已完成

- 将多份 2026-06-08 P0/P1 记录中的旧 `npm run test:unit -- --runTestsByPath ... --runInBand` 标为“历史记录（已过期命令口径）”，并补当前复核方式：`npm run verify:three-terminal` 或显式调用 `.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath ... --runInBand`。
- 将 product 模块不带 `-am` 的 Maven 定向命令标为历史过期命令口径，并补当前 reactor 模板：`mvn -pl product -am ... "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- 将 GPT-5.3 Codex 被描述为 AGENTS 当前规则的旧表述改为“历史记录（已过期口径）”，并明确当前默认使用 `gpt-5.4`。
- 对缺少子 Agent 关闭状态和 CodeGraph 字段的历史记录补追补说明，避免后续按不完整报告模板复制。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 5 个 guard、React typecheck、21 个 Jest suite / 156 个测试、后端 reactor `test-compile` 和后端三端合同测试均通过。Jest 仍输出既有 open handle 提示，但命令退出码为 0。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅输出既有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；`Already up to date`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留

- P2：`PortalDirectLoginSupport` 仍保留旧 `portal_direct_login:{token_hash}` 删除兼容分支；当前读取只走端隔离 key，不构成本轮 P0/P1。
- P2：warehouse `/options/sellers` 后续如拆更细权限，可能需要独立选项权限；当前未证实为现网 P1。
- P2：finance 等部分共享域的 admin 权限合同覆盖不如 integration 完整，后续可补自动化合同。
