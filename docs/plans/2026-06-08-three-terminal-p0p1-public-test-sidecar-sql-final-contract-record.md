# 2026-06-08 三端隔离 P0/P1：测试入口、运行 Sidecar 与 SQL 终态契约记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 结论处理

- 本轮收口前序 6 个子 Agent 的只读扫描结果。
- 历史记录（已过期口径）：当时规则曾要求后续新建子 Agent 时优先使用 GPT-5.3 Codex；不可用时再回退 `gpt-5.4`。当前现行规则已改为默认使用 `gpt-5.4`，除非用户在当前任务中重新明确要求，否则不要再把 GPT-5.3 Codex 作为首选。
- 本轮未再新建子 Agent。
- 追补说明：前序 6 个只读子 Agent 均已关闭，结论由主 Agent 复核后只采纳可验证的 P0/P1。

采纳的 P0/P1：

- P0：`npm run test -- --check-manifest`、`npm run test:unit -- --check-manifest`、`npm run jest -- --check-manifest` 原先会被 `npm run verify:three-terminal` 中间层吞掉参数，可能误跑全量 verifier 或误报。
- P1：运行态 `.js` sidecar 覆盖不完整，`app`、`access`、`requestErrorConfig`、portal session/request、remote menu、proxy/routes 等入口缺少统一契约。
- P1：`20260604_three_terminal_isolation_migration.sql` 缺少最终完成态断言，迁移中途状态可能留下账号、菜单、role-menu、审计字段不完整但脚本继续结束的风险。
- P1：`20260604_portal_direct_login_ticket.sql` 未断言 `status` 与 `used_time` 的一致性。

## 已完成

- `react-ui/package.json` 将 `test`、`test:unit`、`jest` 等公开测试入口改为直接调用 `node scripts/verify-three-terminal.mjs`，保留 CLI 参数透传。
- `react-ui/scripts/verify-three-terminal.mjs` 将公开测试入口契约改为精确命令匹配，防止再次通过 `npm run` 中间层吞参。
- `react-ui/tests/admin-auth-sidecar-contract.test.ts` 纳入运行态关键 `.js` sidecar：`app`、`access`、`requestErrorConfig`、session、portal utilities、remote menu、proxy/routes。
- `react-ui/src/services/seller/seller.js`、`react-ui/src/services/buyer/buyer.js` 改为纯 re-export 对应 `.ts` service。
- `react-ui/scripts/check-partner-management-template.mjs` 与 `AdminAccountPermissionUiContractTest` 固定卖家/买家 service JS 镜像必须纯 re-export。
- `20260604_three_terminal_isolation_migration.sql` 增加 `assert_three_terminal_isolation_migration_completed()`，尾部断言账号关键列、旧 `user_id` 移除、seller/buyer 菜单 ID 区间、role-menu 孤儿引用，以及登录/操作/会话审计字段。
- `20260604_portal_direct_login_ticket.sql` 增加 `status in ('ISSUED','USED','EXPIRED')` 与 `used_time` 一致性断言。
- `SqlExecutionGuardContractTest` 增加 SQL 终态契约和免密票据状态契约。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\admin-auth-sidecar-contract.test.ts tests\verify-three-terminal-backend-gate.test.ts --runInBand`：通过，2 个 suites / 26 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,AdminAccountPermissionUiContractTest" test`：通过，72 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run test -- --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run jest -- --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal -- --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮未跑完整 `npm run verify:three-terminal`，只跑了针对当前 P0/P1 的最小必要测试和 manifest gate。
- 追补说明：原记录未写明 CodeGraph 结果；后续报告必须记录 `codegraph sync .` 结果或无法执行原因。本轮文档口径修复会在新的检查点中统一记录 CodeGraph 执行结果。

## 当前残留项

- P2：direct-login 页 token timeout 失败回传可以更快，目前管理端仍会通过自身 bridge 超时收敛。
- P2：`app.tsx` 与 `requestErrorConfig.ts` 的 401 分流逻辑仍有重复，当前行为一致，后续可抽共享 helper。
- P2：管理端审计查询的 accountId + subjectId fail-closed 主要靠运行时服务层守卫，后续可增加更强的架构合同。
- P2：`verify-three-terminal` 仍允许纯 re-export 的 `.test.js` twin 存在，后续可以统一清理生成副本。
