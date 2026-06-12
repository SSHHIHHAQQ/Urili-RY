# 三端最小账号权限框架完成度审计

- 审计时间：2026-06-12 03:25 +08:00
- 审计范围：管理端、卖家端、买家端三套逻辑入口和权限域；仅覆盖卖家端 / 买家端最小可登录权限框架。
- 当前口径：只处理 P0/P1。商品、订单、库存、物流、财务、履约、外部系统等业务菜单和业务页面仍冻结，不纳入本轮完成口径。

## 结论

当前代码级和 seed 级 P0/P1 门禁已收口；`fenxiao` 权限模板 seed 已落库并通过只读复核。完整目标尚不能标记完成，因为真实 seller/buyer 账号密码登录、管理端 direct-login 票据签发与目标 portal 一次性消费、以及 OWNER 自助写闭环尚未执行 live 验证。

## 完成标准对照

| 完成标准 | 当前状态 | 证据 |
| --- | --- | --- |
| 管理端保留若依 `sys_*`，只负责平台管理和控制动作 | 代码级已验证 | 管理端仍使用 `AdminSeller*` / `AdminBuyer*` 管理接口；三端合同测试通过。 |
| seller/buyer 账号、角色、部门、菜单、日志、会话独立于 `sys_*` | 代码级已验证 | `TerminalAccountIsolationTest`、`PortalSelfServiceSurfaceContractTest` 通过；生产账号查询使用 `subjectId + accountId` 约束。 |
| 管理端默认 direct-login 到对应主体 OWNER | 代码级已验证，live 未验证 | `AdminDirectLoginPermissionContractTest`、`SellerServiceImplTest`、`BuyerServiceImplTest` 覆盖 OWNER 选择和状态拒绝；尚未运行 direct-login live。 |
| seller/buyer 主账号可账号密码登录各自 portal | 脚本与合同已就绪，live 未验证 | `verify-portal-self-management-live.mjs` 已覆盖登录、`getInfo`、`getRouters`、跨端 token 拒绝；缺确认变量时 fail-closed。 |
| OWNER 可在 portal 内创建子账号、设置角色、维护部门、查看日志和会话 | 代码级已验证，live 写闭环未验证 | seller/buyer portal service 单测和 `verify-three-terminal` 通过；`verify-portal-self-management-live-write.mjs` 已覆盖写闭环，但未获确认执行。 |
| seller/buyer 不开放菜单定义能力 | 已验证 | `AdminSellerMenuController` / `AdminBuyerMenuController` 无菜单写 handler；`PartnerMenuModal` 只读；`partner-management-contract.test.ts` 和 `check-partner-management-template.mjs` 通过。 |
| 端内角色只能从 `seller_menu` / `buyer_menu` self-management 模板分配权限 | 已验证 | role-menu 读写收口到 self-management；live-write 负向 verifier 覆盖跨端 menuId 拒绝；`fenxiao` precheck 为 `19 / 19`。 |
| 商品、订单、库存、物流、财务、履约、外部系统业务菜单冻结 | 代码级已验证 | `verify-three-terminal` 中 portal token guard、portal live 合同和业务 guard 通过；本轮未扩展业务菜单。 |
| `getInfo`、`getRouters`、401、token/session、权限校验、日志审计按端隔离 | 静态/单测已验证，真实运行态未全量验证 | `portal-session-request.test.ts`、`portal-unauthorized-redirect.test.ts`、`remote-menu-route-guard.test.ts`、后端权限日志合同均已纳入总门禁；live 未执行。 |
| 最小必要测试通过 | 已验证 | `npm run verify:three-terminal` 通过：33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试通过。 |
| Markdown 记录明确已完成、未验证、P2 遗留和下一步 | 已更新 | 本审计文件、SQL/live runbook 和阶段进展记录均已更新。 |

## 数据源与 seed 证据

- 当前只读预检命令：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`
- 当前目标库：`fenxiao`
- `seller_menu`：23 行，ID `100008-100031`
- `buyer_menu`：23 行，ID `200003-200026`
- active seller/buyer OWNER 角色：各 35 个
- seller/buyer self-management 模板：均为 `19 / 19`
- seller/buyer root button 模板：均为 `18 / 18`
- seller/buyer OWNER self-management grants：均为 `665 = 35 * 19`
- seller/buyer OWNER non-self grants：均为 0
- 无效权限、ID 区间违规、页面 component 违规、重复 perms：均为 0

## 本轮验证

- `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读，无写入。
- `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,AdminDirectLoginPermissionContractTest,PortalSelfServiceSurfaceContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，14 tests，BUILD SUCCESS。
- `cd react-ui; npx jest --config jest.config.ts tests/portal-session-request.test.ts tests/portal-self-management-contract.test.ts tests/partner-management-contract.test.ts --runInBand`：通过，3 suites / 71 tests。
- `cd react-ui; node scripts\check-partner-management-template.mjs`：通过。
- `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试均通过。

## live fail-closed 证据

以下命令均清空相关环境变量后运行，退出码按预期为 1，并在 shell 包装中转为成功断言；未执行真实登录、direct-login、写闭环或清理动作。

- `node scripts\verify-portal-self-management-live.mjs`：提示缺少 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY` 和 seller/buyer portal 账号密码。
- `node scripts\verify-portal-self-management-live-write.mjs`：提示缺少 seller/buyer portal 账号密码和 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY`。
- `node scripts\verify-portal-direct-login-live.mjs`：提示缺少 `ADMIN_AUTH_TOKEN or ADMIN_USERNAME/ADMIN_PASSWORD` 和 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`。

## 未验证项

- 真实 seller/buyer OWNER 账号密码登录。
- 管理端 direct-login 票据签发、目标 portal 一次性消费、消费成功消息回传。
- 真实 `/getInfo`、`/getRouters`、401 跳转、日志和会话隔离。
- OWNER 在真实 portal 内创建子账号、设置角色、维护部门、查看登录日志、操作日志和在线会话。

## P2 遗留

- 不做浏览器截图、DOM/UI 细调和视觉优化。
- 不铺设商品、订单、库存、物流、财务、履约、外部系统等业务菜单。
- `PortalSelfManagement.tsx` 仍是较大页面文件；本阶段职责集中且快速推进模式下暂不拆分。

## 下一步

1. 在用户明确确认 live 写入范围并提供或确认 seller/buyer OWNER 凭据后，运行 `verify:portal-self-management-live`。
2. 在用户明确确认 direct-login 会写票据、审计日志和 portal session 后，运行 `verify:portal-direct-login-live`。
3. 在用户明确确认写闭环会创建测试角色、测试部门和停用 STAFF 测试账号后，运行 `verify:portal-self-management-live-write`。

## 2026-06-12 03:22 补充审计

- 补强项：新增 `TerminalSqlIsolationContractTest#terminalSessionTablesMustUseIndependentDdlAndDirectLoginAuditFields`。
- 覆盖目的：固定 `seller_session` / `buyer_session` 必须使用独立端内会话表 DDL，不得通过 `LIKE` 克隆或引用 `sys_*`；会话表必须包含 `token_id`、端内主体 ID、端内账号 ID，以及 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason` 结构化审计字段。
- 对应目标：卖家端、买家端会话必须独立，管理端直登必须可审计。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，13 tests，BUILD SUCCESS。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试均通过。
- 影响边界：本轮只补合同测试，不执行 SQL、portal 登录、direct-login 或 self-management 写闭环。

## 2026-06-12 03:26 补充审计

- 补强项：在 `react-ui/tests/portal-self-management-contract.test.ts` 增加 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile` / `PortalOwnSessionProfile` 前端类型泄露合同。
- 覆盖目的：固定 portal 自助日志和在线会话的前端可见类型不得包含 `sellerId`、`buyerId`、`subjectId`、`accountId`、`tokenId`、`directLoginTicketId`、`actingAdmin*`、`directLoginReason`、`operParam`、`jsonResult` 等内部身份范围或审计字段。
- 对应目标：卖家端、买家端自助登录日志、操作日志、在线会话只暴露端内可见 DTO，避免前端类型层把管理端审计字段重新引入 portal。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-contract.test.ts --runInBand`：通过，1 suite / 6 tests。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- 影响边界：本轮只补前端静态合同，不执行 SQL、portal 登录、direct-login 或 self-management 写闭环。

## 2026-06-12 03:30 完整门禁复跑

- `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试均通过。
- `git diff --check -- <本轮三端相关文件>`：通过，无 whitespace error，仅有 LF/CRLF 工作区提示。
- 备注：Jest 结束时仍输出既有 open handle 提示，但命令退出码为 0，未阻塞本轮最小门禁。

## 2026-06-12 03:31 SQL seed 后只读复核

- 命令：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
- 命令类型：只读 `SELECT` 预检，未执行 SQL seed，未写 MySQL、Redis 或业务数据。
- 目标库：`fenxiao`。
- 连接来源：继续以当前后端 `druid` 激活配置和 `RUOYI_DB_*` / `RUOYI_REDIS_*` 运行变量为准；记录中不输出连接明文。
- 复核结果：`seller_menu` 23 行，ID `100008-100031`；`buyer_menu` 23 行，ID `200003-200026`；seller/buyer self-management 模板均为 `19 / 19`；root button 模板均为 `18 / 18`；OWNER self-management grants 均为 `665 = 35 * 19`；OWNER non-self grants 均为 0；无无效权限、ID 区间违规、页面 component 违规或重复 perms。

## 2026-06-12 03:37 服务层主体 ID 防串端合同

- 补强项：在 `SellerServiceImplTest` / `BuyerServiceImplTest` 增加账号更新服务层合同。
- 覆盖目的：固定 `updateSellerAccount(sellerId, account)` / `updateBuyerAccount(buyerId, account)` 在写 mapper 前必须使用调用参数里的主体 ID 覆盖请求体中的 `sellerId` / `buyerId` 和通用 `accountId`，避免 portal 或管理端请求体伪造主体 ID 影响端内账号写入。
- 对应目标：seller/buyer portal 接口必须从当前 token/session 派生主体；管理端主体路径同样必须以路径主体为准，不信任请求体主体 ID。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 64 tests、buyer 65 tests，BUILD SUCCESS。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认目标库 `fenxiao`；seller/buyer self-management 模板均为 `19 / 19`，OWNER self-management grants 均为 `665 = 35 * 19`，异常项均为 0。
- 影响边界：本轮未改生产代码，未执行新的 SQL seed、portal 登录、direct-login 或 self-management 写闭环。

## 2026-06-12 03:49 子 Agent 只读复核与本地静态复查

- 子 Agent 使用记录：
  - 启动 2 个只读复核子 Agent，均按审查类任务指定 `gpt-5.3-codex-spark`。
  - 前端复核子 Agent 已完成，范围为 `react-ui/src`、`react-ui/tests`、`react-ui/scripts`；结论为未发现高置信 P0/P1 缺口，剩余项仍是真实 live 登录、direct-login 和 self-management 写闭环未验证。
  - 后端复核子 Agent 连续等待未返回，已关闭，状态为 `running` 后 shutdown；未产出结论，未纳入验收证据。
- 本地复查结论：
  - `react-ui/src/services/portal/session.ts` 已对 portal 请求剥离 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId`、`terminal`，并在写入前规范化 roleIds/menuIds 和路径 ID。
  - `PortalDirectLoginSupport` 仍使用 `portal_direct_login:{terminal}:{token_hash}`，跨端消费不会触发当前端 failureAuditor，也不会删除真实端 payload；legacy key 只作为清理目标。
  - SQL/seed guard 已覆盖 menu ID 区间、auto_increment 区间、权限前缀、component、role-menu orphan/out-of-range 和 OWNER self-management grants；本轮未执行 SQL。
- 本轮未新增生产代码；未执行真实 portal 登录、direct-login 或写闭环。

## 2026-06-12 03:57 portal 账号编辑入口主体 ID 收紧

- 补强项：`SellerPortalController#editAccount` 和 `BuyerPortalController#editAccount` 在使用路径 `targetAccountId` 覆盖端内账号主键后，显式清空请求体中的 `sellerId` / `buyerId`。
- 覆盖目的：portal 账号编辑入口不把前端请求体携带的主体 ID 继续传入服务层；最终主体仍由当前 `PortalLoginSession` 的 `subjectId` 派生，账号 ID 由路径参数派生。
- 合同固定：`PortalSelfServiceSurfaceContractTest` 现在要求 `editAccount` 方法体内必须执行 `account.setSellerId(null)` / `account.setBuyerId(null)`，避免只靠文件其他位置出现同名清理逻辑。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，1 test，BUILD SUCCESS。
  - `cd RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 64 tests、buyer 65 tests，BUILD SUCCESS。
  - 首次 `ruoyi-system` 窄测命令未给 PowerShell 中的 `-Dsurefire.failIfNoSpecifiedTests=false` 加引号，Maven 将其解析为 lifecycle phase 导致命令失败；已用引号形式重跑通过，判定为命令参数问题，不是代码失败。
- 影响边界：本次只收紧 portal 账号编辑入口和静态合同；未执行 SQL、Redis 写入、真实 portal 登录、direct-login 或 self-management 写闭环；未扩展商品、订单、库存、物流、财务、履约或外部系统业务。

## 2026-06-12 04:04 完整门禁收口与旁路 P0 修复

- 门禁阻塞 1：`react-ui` TypeScript 编译先后发现 `UpstreamSystem/components/SyncTabs.tsx` 引用未转发的 `syncTaskStatusColor` / `syncTaskStatusText`，以及 `useRef<ActionType>()` 缺初始值。
- 最小修复 1：`react-ui/src/pages/UpstreamSystem/constants.ts` 仅 re-export 已存在的同步任务状态常量；`SyncTabs.tsx` 的任务表 ref 改为 `useRef<ActionType | null>(null)`。该修复只解除编译 P0，不扩展外部系统业务流程。
- 门禁阻塞 2：`StandalonePartnerSeedMenuContractTest` 发现上游同步任务权限 seed 使用 `sys_menu.menu_id = 2310-2312`，与卖家账号管理按钮冲突。
- 最小修复 2：`upstream_system_management_seed.sql` 与 `20260612_upstream_sync_task_lifecycle.sql` 将上游同步任务权限 ID 统一迁到空闲的 `2324-2326`，仍挂在 `2031` 上游系统管理菜单下，避免污染 seller/buyer 管理端权限签名。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=StandalonePartnerSeedMenuContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，2 tests。
  - `cd react-ui; npm run verify:three-terminal`：通过，前端 33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认目标库 `fenxiao`，seller/buyer self-management 模板均为 `19 / 19`，OWNER self-management grants 均为 `665 = 35 * 19`，异常项均为 0。
  - `git diff --check -- <本轮相关文件>`：通过，无 whitespace error，仅 LF/CRLF 工作区提示。
  - `codegraph sync .`：通过，`Synced 20 changed files; Added: 1, Modified: 19 - 1,226 nodes in 1.8s`。
- 影响边界：本轮没有执行新的 SQL seed、未写 MySQL/Redis、未执行真实 portal 登录、direct-login 或 self-management 写闭环；旁路 UpstreamSystem 和 upstream SQL 仅做门禁级 P0/P1 修复，不纳入三端权限框架完成口径。

## 2026-06-12 04:07 self-management 权限 seed 执行前确认

- 用户确认：允许对当前目标库 `fenxiao` 执行本阶段 self-management 权限 seed。
- 表范围：仅限 `seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`。
- 执行方式：使用 guarded runner，带 `PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED` 和 `--apply`。
- 边界说明：本次不执行真实 portal 账号密码登录、direct-login 消费回传或 self-management 写闭环，不扩展冻结业务菜单和业务页面。

## 2026-06-12 04:08 self-management 权限 seed 执行完成

- 执行命令：`PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`。
- 执行结果：成功，postcheck 精确状态已验证。
- 目标库：`fenxiao`。
- postcheck 摘要：
  - `seller_menu` 23 行，ID 范围 `100008-100031`。
  - `buyer_menu` 23 行，ID 范围 `200003-200026`。
  - active seller/buyer OWNER 角色各 35 个。
  - seller/buyer self-management 模板均为 `19 / 19`，root button 模板均为 `18 / 18`。
  - seller/buyer OWNER self-management grants 均为 `665 = 35 * 19`，OWNER non-self grants 均为 0。
  - 无效权限、菜单 ID 区间违规、页面 component 违规和重复 perms 均为 0。
- 未验证项：真实账号密码登录、direct-login 消费成功回传、OWNER 在 portal 内的自主管理写闭环仍未执行。

## 2026-06-12 04:09 seed 后收尾复核

- `git diff --check -- <本轮相关文件>`：通过，无 whitespace error，仅 LF/CRLF 工作区提示。
- `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认 seed 后状态仍正确。
- `codegraph sync .`：通过，`Synced 5 changed files; Modified: 5 - 299 nodes in 1.0s`。
- 结论：SQL/seed 权限模板缺口已完成落库和复核；总目标仍不能标记完成，因为真实登录、direct-login 回传和 OWNER 自主管理写闭环尚未 live 验证。

## 2026-06-12 04:12 seed 后 live 前完成度复核

- 已完成且已验证：
  - self-management 权限模板 seed 已落 `fenxiao`，表范围限定在 `seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`。
  - seed 后只读 precheck 通过：seller/buyer self-management 模板 `19 / 19`，OWNER grants `665 = 35 * 19`，异常项为 0。
  - 静态 guard 与完整 `verify-three-terminal` 通过：33 suites / 304 tests，后端 reactor `test-compile` 与三端合同测试通过。
  - live verifier 的确认变量负向门槛有效，缺少确认变量和凭据时不进入真实登录或写入链路。
- 仍未验证：
  - seller/buyer OWNER 账号密码真实登录各自 portal。
  - 管理端 direct-login 真实签发、目标 portal 消费、成功回传后再提示成功。
  - OWNER 在 portal 内创建子账号、设置角色、维护部门、查看登录日志/操作日志/在线会话的写闭环。
- 下一步所需确认：真实 live 将写登录日志、操作日志、portal session、direct-login 票据/审计、测试角色、测试部门和停用 STAFF 测试账号；执行前需要对应确认变量和 OWNER/admin 凭据。

## 2026-06-12 04:15 阻塞前静态证据复核

- 生产代码账号查询复核：
  - `rg -n "select[A-Za-z]*(Seller|Buyer)?AccountById\s*\(|select[A-Za-z]*AccountById\s*\(" RuoYi-Vue\seller\src\main RuoYi-Vue\buyer\src\main RuoYi-Vue\ruoyi-system\src\main RuoYi-Vue\ruoyi-framework\src\main` 仅发现 `selectSellerAccountById(sellerId, sellerAccountId)` / `selectBuyerAccountById(buyerId, buyerAccountId)` 两参调用。
  - `rg -n "select(Seller|Buyer)AccountById\s*\(\s*Long\s+(seller|buyer)?AccountId\s*\)" ...`：未发现裸 `selectSeller/BuyerAccountById(Long accountId)` 签名。
- `sys_*` 控制面复核：
  - `rg -n "sys_user|sys_role|sys_menu|sys_dept|sys_user_role|sys_role_menu" RuoYi-Vue\seller\src\main RuoYi-Vue\buyer\src\main RuoYi-Vue\seller\src\test RuoYi-Vue\buyer\src\test`：无匹配，seller/buyer 模块未恢复端内 `sys_*` 控制面引用。
- seed fail-closed 复核：
  - `20260610_portal_self_management_permission_seed.sql` 仍包含 seller/buyer menu ID 区间检查、通配权限拒绝、`seller:admin:` / `buyer:admin:` 命名空间拒绝、页面 component 根路径检查，以及 `seller_role_menu` / `buyer_role_menu` 授权写入。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认 `fenxiao` seed 后状态仍正确。
- 阻塞条件：
  - 剩余未验证项均需要真实写入：portal 登录日志、portal session、direct-login 票据与审计、操作日志、测试角色、测试部门和停用 STAFF 测试账号。
  - 当前缺少执行这些 live 验证所需的明确写入确认和 seller/buyer OWNER、admin 认证上下文，因此不能继续推进到完成态，也不能标记目标完成。

## 2026-06-12 11:13 三类 live 授权恢复

- 用户已授权自行解决三类 live 验证。
- 本次可继续执行：
  - seller/buyer OWNER 账号密码登录 live。
  - 管理端到 seller/buyer OWNER 的 direct-login 票据签发、消费和隔离验证。
  - seller/buyer OWNER 自主管理写闭环验证。
- 允许写入：登录日志、操作日志、portal session、direct-login 票据/审计、测试角色、测试部门和停用 STAFF 测试账号。
- 执行前状态：8080 当前未连通，需先启动本机后端；凭据、token、连接串均不写入记录。

## 2026-06-12 11:34 三类 live 完成记录

- 数据源确认：后端激活 profile 为 `druid`，MySQL/Redis 继续来自 `.env.local` 注入的 `RUOYI_DB_*` / `RUOYI_REDIS_*`，SQL 预检确认当前库为 `fenxiao`；记录不输出连接串、token 或密码明文。
- live 前修复 1：普通 seller/buyer 账号密码登录写入 `seller_session` / `buyer_session` 时，`direct_login` 不能为 null。已在 `PortalTokenSupport#buildSession` 默认写入 `Boolean.FALSE`，并用 `PortalTokenSupportTest` 固定普通登录不会携带 direct-login 审计字段。
- live 前修复 2：portal 自助日志/会话分页接口中，PageHelper 不能污染 session/account 校验 SQL。已在 seller/buyer service 中新增 `assert*SessionAccountWithoutPage`，校验期间临时 `clearPage` 并在 `finally` 恢复分页上下文；`PortalSelfServiceSurfaceContractTest` 已固定该合同。
- live 脚本修复：`verify-portal-self-management-live.mjs` 兼容若依分页响应，缺少 `data` 时按完整 body 校验 `rows`，避免只读 live 在分页响应上误判。
- 账号密码登录 live：`cd react-ui; npm run verify:portal-self-management-live` 通过。覆盖 seller/buyer OWNER 登录、`getInfo`、`getRouters`、自助账号/部门/角色/登录日志/操作日志/在线会话读接口、冻结业务权限不可见、跨端 token 拒绝。
- direct-login live：`cd react-ui; npm run verify:portal-direct-login-live` 通过。脚本自动选择 seller 主体 41 与 buyer 主体 36 的 active OWNER，完成管理端票据签发、目标 portal 一次性消费、跨端票据拒绝、重复消费拒绝、portal token 隔离与登出清理。
- self-management 写入 live：`cd react-ui; npm run verify:portal-self-management-live-write` 通过。完成 seller/buyer OWNER 创建并清理测试部门、创建/更新/删除测试角色、创建禁用 STAFF 证据账号、账号角色分配与清理、跨端 menuId 写入拒绝；保留禁用证据账号 `seller#41`、`buyer#37`。
- 最终门禁：`cd react-ui; npm run verify:three-terminal` 通过，33 suites / 304 tests；React typecheck、portal token guard、partner management guard、后端 reactor `test-compile` 与三端合同测试均通过。
- 收尾复核：`node .\scripts\portal-self-management-sql-runner.mjs --precheck` 通过，`seller_menu` 23 行、`buyer_menu` 23 行，seller/buyer self-management 模板均 `19 / 19`，root button 均 `18 / 18`，OWNER self-management grants 均 `665 = 35 * 19`，OWNER non-self grants 为 0，异常项均为 0。
- 静态检查：`node scripts\verify-three-terminal.mjs --check-manifest` 通过；`git diff --check` 通过，仅有 LF/CRLF 工作区提示。
- 当前完成结论：本阶段“卖家端 / 买家端最小可登录权限框架”已达到完成口径；管理端保留 `sys_*` 控制面，seller/buyer 端内账号、角色、部门、菜单模板、日志、会话按端隔离；业务菜单和业务页面仍冻结。
- P2 遗留：未做浏览器截图、DOM/UI 细调、视觉优化；未铺设商品、订单、库存、物流、财务、履约、外部系统等业务 portal 菜单和业务页面。
