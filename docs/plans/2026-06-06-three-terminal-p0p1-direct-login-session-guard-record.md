# 2026-06-06 三端隔离 P0/P1：免密会话审计与 JS Sidecar Guard 收口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 约束

- 用户已更新后续子 Agent 模型偏好：优先 GPT-5.3 Codex；不可用时退到 `gpt-5.4`。
- 本轮采纳上一批 6 个只读子 Agent 的 P1 结论后由主 Agent 收口；未新增子 Agent。
- 需要关闭的既有子 Agent 已在本轮后续收尾中关闭。

## 已完成

- `PortalDirectLoginResult` 对 `accountId`、`username` 增加 JSON 序列化隐藏契约，管理端直登响应只保留 `token`、`ticketId`、`loginUrl`、`expireMinutes`、`expireTime`。
- `PortalPreAuthorizeAspect` 在端内权限拒绝日志里补入 `directLoginAudit{...}` 前缀，免密代入后即使触发权限拒绝，也能看到 acting admin、ticket 和原因。
- `PortalSessionProfile`、seller/buyer session mapper 和 SQL 脚本补齐免密会话审计字段：`direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
- 管理端 `PartnerSessionModal` 在原状态列内展示免密代入标识和 acting admin，不新增横向列宽。
- `DirectLoginResult` 前端类型移除目标账号字段；`check-portal-token-isolation.mjs` 对 direct-login 响应字段做白名单检查。
- `persistPortalLogin(result, expectedTerminal)` 增加端类型不一致单测，确认 seller token 不会写入 buyer key，也不会写入管理端 token。
- `check-portal-token-isolation.mjs`、`check-partner-management-template.mjs`、Java UI 契约均扩展到 `.js` sidecar，避免只检查 `.tsx`。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/terminal-session-token.test.ts --runInBand`：通过，`4` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=PortalDirectLoginAuthContractTest,SqlExecutionGuardContractTest,PortalDirectLoginResultTest,AdminAccountPermissionUiContractTest,AdminDirectLoginPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`10` 个目标契约测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 guard、tsc、Jest 和后端三端契约均通过，最终输出 `three-terminal verification passed.`。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；返回 `Synced 6 changed files`，`Modified: 6 - 184 nodes`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML；只更新代码、SQL 文件和静态契约。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 和 `application-druid.yml` 是当前工作区已有本地配置改动，本轮未修改。

## 后续 P2 记录

- direct-login 返回 `loginUrl` 仍是当前已确认链路；后续如要改成后端跳转或一次性兑换页，需要单独设计。
- `.tsx` / `.js` 双文件并存仍会增加维护成本；当前先用 guard 收口，后续可评估是否统一构建产物来源。
