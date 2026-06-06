# 2026-06-06 三端 P0/P1：postMessage、SQL 索引自检与远程回放记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 子 Agent 情况

- 按用户最新要求优先尝试 `gpt-5.3-codex-spark`；因平台可用性限制，实际降级使用 `gpt-5.4` 做只读切片审计。
- 6 个 `gpt-5.4` 子 agent 已完成并关闭，覆盖：auth/direct-login、service/mapper、SQL、前端管理模板、portal token/frontend、product portal 范围。
- 已采纳 P1：`postMessage` origin 校验缺失、portal 401 未退出 portal 路由、SQL 同名错索引不自愈、菜单 seed 不防同语义不同 ID、legacy sys_role 清理脚本缺少 guard。
- 未采纳为本轮阻塞：`seller_menu/buyer_menu` 是否主体私有化。当前按已确认方向保留为端级菜单控制面，由管理端配置；如果后续要主体级菜单，需要另起表设计方案。

## 已完成

- 管理端向 portal 直登页投递 token 时校验 `event.origin === targetOrigin`。
- Portal direct-login 页面接收 token 时校验 opener origin，并用明确 origin 发送 ready 消息，不再使用 `'*'`。
- Portal 请求 401 后清除对应端 token，并跳出 portal 路由到登录页。
- `check-portal-token-isolation.mjs` 增加 postMessage origin、ready targetOrigin、portal 401 redirect 的静态 guard。
- seller/buyer service 测试桩去掉 `#directLoginToken=` URL 模式，并新增 contract 防止测试桩把 token URL 模式带回来。
- `20260604_portal_direct_login_ticket.sql` 修复中断遗留的破损 procedure，并增加：
  - `recreate_index_if_mismatch`
  - `assert_index_definition`
  - `assert_no_invalid_direct_login_ticket_rows`
- `20260604_three_terminal_isolation_migration.sql`、`20260605_terminal_owner_account_unique_constraint.sql`、`20260606_terminal_log_scope_indexes.sql` 改为对 OWNER 唯一索引和 terminal log 范围索引校验列序与唯一性，同名但定义错误时重建。
- `20260606_admin_partner_page_direct_login_seed.sql` 增加 `assert_sys_menu_signature_available`，防止同路径/组件/路由/权限语义被不同 `menu_id` 重复占用。
- `20260606_legacy_disable_sys_seller_buyer_roles.sql` 增加端内表存在断言、活跃 `sys_user_role` 绑定断言，并让 remark 更新保持幂等。
- 更新后端 architecture contract，锁住以上 SQL 与 direct-login URL/body 约束。

## 远程库执行

- 数据源确认：本轮通过当前激活配置和本机 `.env.local` 注入运行变量确认目标为当前远程 MySQL；记录中不输出 JDBC URL、账号、密码、Redis 信息或 token secret。
- 执行性质：对远程 MySQL 执行已确认的幂等 DDL/DML 脚本回放；未读写 Redis。
- 回放脚本：
  - `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
  - `RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`
  - `RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql`
  - `RuoYi-Vue/sql/20260606_terminal_log_scope_indexes.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql`
- 未执行：`RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql`。该脚本是可选历史清理脚本，不属于默认迁移；本轮只增强 guard。
- 回放结果：5 个脚本均执行成功。
- 回放后核验：`exactIndexes=14`，`menuRows=5`，`invalidTickets=0`。

## 验证结果

- `node --check react-ui/scripts/check-portal-token-isolation.mjs`：通过。
- `node --check react-ui/scripts/check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，输出 `Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过，输出 `Partner management template guard passed.`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginAuthContractTest,PortalDirectLoginTicketSqlContractTest,TerminalSqlIsolationContractTest,StandalonePartnerSeedMenuContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`BUILD SUCCESS`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；只有 LF/CRLF 工作区换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 边界说明

- 本轮没有启动浏览器，没有做截图、DOM 或 UI 细调验收。
- 本轮未读写 Redis。
- 文件里的中文经 Node UTF-8 检查正常；PowerShell 控制台出现的乱码是显示层编码问题，不是 SQL 文件真实内容。
- buyer 商品可见域仍按当前“公共在售目录”口径保留；如果后续业务确认要按 buyer 授权可见，需要另起 buyer 商品可见关系设计。
