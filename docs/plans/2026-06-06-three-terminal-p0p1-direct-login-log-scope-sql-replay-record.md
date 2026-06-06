# 2026-06-06 三端 P0/P1：免密 token、日志范围、SQL 回放与门禁收口记录

## 参考方向

- 参考方案：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 执行口径：快速推进模式，只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 本轮边界：不做浏览器运行态验收，不做截图、DOM 检测或 UI 细调。

## 本轮修复

- 修复免密直登 P0：`PortalDirectLoginSupport` 不再把一次性 token 拼进 `loginUrl`；管理端响应体单独返回 token，前端用干净 URL 打开 portal direct-login 页面后通过 `postMessage` 握手投递 token。
- 修复前端直登页：`/Portal/DirectLogin` 不再从 `location.search` / `location.hash` / `URLSearchParams` 读取 `directLoginToken`，只接收 opener message 后再 POST 端内 `/direct-login` 接口。
- 修复 portal token P1：portal 登录没有 refresh token 时，不再把 access token 同时写入 refresh token 存储槽。
- 修复 seller/buyer 管理端日志查询 P1：管理端传 `accountId` 时，service 层校验账号存在并推导/校验 `subjectId`；构造不一致的 `subjectId/accountId` 会被拒绝。
- 修复 seller/buyer mapper guard P1：`reset*AccountPassword` 和 `update*AccountLoginInfo` 的 SQL 更新条件增加 `seller_id` / `buyer_id`。
- 修复强制下线审计 P1：实际踢出 seller/buyer 在线 session 后，会补写端内 login log；密码重置强制下线记录为 `PASSWORD_RESET_FORCE_LOGOUT`，普通强制下线记录为 `FORCE_LOGOUT`。
- 修复前端同构模板 P1：账号权限 fallback 改为 `*:admin:account:*` 命名空间；seller/buyer 配置传入 `searchFieldCount: 8`，共享模板传给 `getPersistedProTableSearch`。
- 修复验证脚本 P0：`verify-three-terminal.mjs` 清理 surefire 历史报告时补齐 `backendReportModules`，避免门禁脚本自身运行失败。
- 修复 SQL P1：OWNER generated column 表达式校验兼容 MySQL `_utf8mb3` 与反斜杠表达式；direct-login ticket SQL 增加关键列定义收敛；log scope index 脚本增加列存在断言；standalone seed 增加 `sys_menu` 固定槽位冲突断言。

## 远程库执行

- 数据源确认：通过当前激活配置和本机 `.env.local` 注入运行变量确认目标为当前远程 MySQL；未在记录中输出 JDBC URL、账号、密码、Redis 信息或 token secret。
- 执行性质：本轮对远程 MySQL 执行已确认的幂等 DDL/DML 脚本回放；未读写 Redis。
- 回放脚本：
  - `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
  - `RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`
  - `RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql`
  - `RuoYi-Vue/sql/20260606_terminal_log_scope_indexes.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql`
- 回放结果：五个脚本均 `executed`。
- 回放后结构检查：`missingTables=[]`、`missingColumns=[]`、`missingIndexes=[]`、`duplicateOwnerGroups=0`。

## 验证结果

- `node --check react-ui/scripts/verify-three-terminal.mjs`：通过。
- `node --check react-ui/scripts/check-portal-token-isolation.mjs`：通过。
- `node --check react-ui/scripts/check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginAuthContractTest,PortalDirectLoginSupportTest,PortalDirectLoginResultTest,TerminalSqlIsolationContractTest,PortalDirectLoginTicketSqlContractTest,StandalonePartnerSeedMenuContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；只有 LF/CRLF 工作区换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 34 changed files`，`Added: 2, Modified: 32 - 1,477 nodes`。

## 残留说明

- 未做浏览器、截图、DOM 或 UI 细调验收，符合本轮用户边界。
- 买家端商品可见域仍按当前“公共在售目录”测试口径通过；如果后续业务确认买家只能看分配给自己的商品，需要另起 buyer-product 可见关系设计，不应在本轮直接改成临时过滤。
- 三前端物理拆分仍是路线项，本轮仅在当前 `react-ui` 管理端和 portal 验证入口内收口 P0/P1。

## 临时工具清理

- 本轮用于远程 SQL 回放的临时 `tools/tmp/DbSqlRunner.java` 和编译产物已删除。
