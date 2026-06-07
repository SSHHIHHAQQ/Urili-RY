# 2026-06-07 三端 P0/P1 免密消费确认记录

## 背景

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按三端独立账号权限改造推进。

当前模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 情况

- 本轮按目标要求启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 本切片采纳免密 ack 子 Agent 结论：管理端此前只等目标窗口 `READY`，未等待 portal 端 `/direct-login` 真正消费成功。
- 其他子 Agent 返回的 P1 保留为后续切片：
  - 账号行缺少账号级审计入口。
  - `20260604_three_terminal_isolation_migration.sql` legacy blocker 仍需前置为真正 preflight，并固定非事务化说明合同。
  - `seller_buyer_management_seed.sql` 仍混合 fresh bootstrap 和运行库增量补写职责。
  - portal `accounts/depts/roles` 与商城商品接口实现已细化，仍可补更精确 controller 合同。
  - `/api/seller/admin/**`、`/api/buyer/admin/**` 的 401 分流还可补前端回归测试。

## 已完成

- `react-ui/src/utils/portalDirectLoginMessage.ts` 和 `.js`：
  - 新增 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE`。
  - `openPortalDirectLoginWindow(...)` 从打开窗口即返回成功，改为返回 Promise。
  - READY 只负责确认目标窗口可接收 token。
  - 只有目标 portal 回传 RESULT success 后，管理端才认为免密登录已确认。
  - RESULT error、popup 关闭、READY 超时和消费确认超时都会失败。
- `react-ui/src/pages/Portal/DirectLogin/index.tsx` 和 `.js`：
  - portal 端收到 token 后调用 `/direct-login`。
  - 只有接口成功且 `persistPortalLogin(...)` 成功后，才向 opener 回传 RESULT success。
  - 消费失败、terminal mismatch 或持久化失败只回传 RESULT error 或展示失败状态。
- `PartnerManagementPage` 主体级免密入口和 `PartnerAccountModal` 账号级免密入口：
  - 均改为 `await openPortalDirectLoginWindow(...)`。
  - 管理端成功提示从“链接已生成”收敛为“端内免密登录已确认”。
- `react-ui/tests/portal-direct-login-message.test.ts`：
  - 断言 READY 后只发送 token，不 resolve 成功。
  - 断言 RESULT success 才 resolve。
  - 断言 RESULT error 和消费确认超时会 reject。
- `react-ui/scripts/check-portal-token-isolation.mjs`：
  - 静态守卫 RESULT 消息、消费确认回传和管理端 await 调用。
- 已更新：
  - `AGENTS.md`
  - `docs/architecture/reuse-ledger.md`

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runInBand tests/portal-direct-login-message.test.ts`：通过，`1` 个 suite / `4` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过：
  - 前端 `5` 个 suite / `23` 个测试通过。
  - 后端 `ruoyi-system` `132`、`ruoyi-framework` `15`、`product` `1`、`seller` `91`、`buyer` `92` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 `11` 个变更文件，`470` 个节点。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留 P1

- 账号行缺少账号级审计入口；后端管理端审计接口已有按 `subjectId + accountId` 过滤能力。
- `20260604_three_terminal_isolation_migration.sql` 仍需把 legacy blocker 前移为真正 preflight，并用合同固定非事务化说明。
- `seller_buyer_management_seed.sql` 仍同时承担 fresh bootstrap 和全局增量修补职责，后续应拆分或增加 profile/freshness guard。
- portal `accounts/depts/roles`、商城商品读接口和 admin dept/role controller 可补更精确方法级权限合同。
- `/api/seller/admin/**`、`/api/buyer/admin/**` 的 401 分流还可补前端回归测试，防止被误判为 portal 请求。
