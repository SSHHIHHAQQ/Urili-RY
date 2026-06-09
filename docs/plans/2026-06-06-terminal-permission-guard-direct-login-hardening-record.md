# 2026-06-06 三端账号权限 P0/P1 收口记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮范围：继续按三端独立方向推进，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 已完成

- 远程库日志查询索引补强：
  - 新增 `RuoYi-Vue/sql/20260606_terminal_log_scope_indexes.sql`。
  - 已对当前远程 MySQL 执行 8 个 seller/buyer login/oper log 的 subject/account 时间索引补齐。
  - 执行后校验结果：`required=8`、`present=8`、`missing=0`。
- Mapper 主体 guard 收口：
  - `seller_account_role` / `buyer_account_role` 删除和批量插入不再只按 `accountId`，SQL 层同时绑定 `sellerId` / `buyerId`。
  - `seller_role_menu` / `buyer_role_menu` 删除和批量插入不再只按 `roleId`，SQL 层先校验角色主体归属。
  - `select*AccountPermissions` / `select*AccountMenuList` 增加账号主体 join，避免只校验 role 侧主体。
  - `checkDeptExistAccount` 增加 `sellerId` / `buyerId` 参数，部门删除占用检查限定在当前主体内。
- 自行修改密码后的会话处理：
  - seller/buyer 端用户自己改密码成功后，复用后台重置密码的强退逻辑，强制该账号现有 portal session 失效。
- 管理端会话审计字段：
  - `PortalSessionProfile.tokenId` 允许序列化给管理端会话列表使用。
  - seller/buyer 端内自助会话仍通过 `PortalOwnSessionProfile` 返回，不暴露 `tokenId`、`terminal`、`subjectId`、`accountId`。
  - `PartnerSessionModal` 的 `rowKey` 优先使用 `tokenId`，保留旧拼接 fallback。
- 免密登录 URL hardening：
  - `PortalDirectLoginSupport` 生成 `loginUrl` 改为 `#directLoginToken=...`，避免 token 进入前端服务器 query 日志。
  - `/seller/direct-login`、`/buyer/direct-login` 页面优先从 hash 读取 token，并继续兼容旧 query 链接。
  - `SellerServiceImpl` / `BuyerServiceImpl` 不再内置 `http://127.0.0.1:8001/...` fallback，必须通过 `portal.seller.web.url` / `portal.buyer.web.url` 配置实际端地址。
- standalone seed 修复：
  - `20260606_admin_partner_page_direct_login_seed.sql` 补入父菜单 `2010`，避免只回放该 seed 时 `2011/2012` 变成孤儿菜单。
  - `AdminDirectLoginPermissionContractTest` 增加 `PartnerManagement` 检查，防止父菜单再次漏掉。

## 子 Agent 使用

- 历史记录（已过期口径）：本轮按要求优先考虑 GPT-5.3 Codex；当前可用工具列表里实际可用模型名为 `gpt-5.3-codex-spark`，此前尝试受平台可用性限制后降级到 `gpt-5.4`。
- 已采纳只读审计结果中的 P1：Mapper SQL guard、改密会话失效、管理端会话 `tokenId`、免密 URL 配置/泄露风险、standalone seed 父菜单风险。
- 已返回的 Agent 在清理时显示不可再关闭；最后一个未完成初始化的 Agent 已关闭。

## 残留 P1

- `20260604_portal_direct_login_ticket.sql` 仍只有 `CREATE TABLE IF NOT EXISTS`，对“表已存在但列/索引不完整”的历史环境缺少自愈迁移。
- OWNER 唯一性脚本仍主要按列名/索引名存在判断，未校验 `generation_expression` 是否等于预期表达式。
- 当前验证库里的 `portal.seller.web.url` / `portal.buyer.web.url` 可能仍指向当前单一 `react-ui` 验证地址；真正拆出 `seller-ui` / `buyer-ui` 前必须改成对应端地址。

## 验证结果

- `mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,SellerPortalPermissionServiceImplTest,BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 42 tests，buyer 42 tests。
- `mvn -pl ruoyi-system "-Dtest=TerminalSqlIsolationContractTest,PortalSessionProfileTest,PortalDirectLoginResultTest,PortalDirectLoginSupportTest" test`：通过，13 tests。
- `mvn -pl ruoyi-system "-Dtest=AdminDirectLoginPermissionContractTest,TerminalSqlIsolationContractTest" test`：通过，5 tests。
- `npm run guard:partner-management`：通过。
- `npm run guard:portal-token`：通过。
- `npm exec tsc -- --noEmit --pretty false`：通过。
- `npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`
- `git diff --check`：通过；只有 LF/CRLF 工作区换行提示，无空白错误。
- `codegraph sync .`：通过，输出 `Synced 34 changed files`，`Modified: 34 - 1,377 nodes`。

## 边界说明

- 本轮未启动浏览器，未做截图/DOM/UI 细节验收。
- 本轮执行过远程 MySQL DDL，仅限 8 个日志查询索引；未读取或写入 Redis。
- 本轮未提交 `.env.local`、数据库连接串、数据库账号密码、Redis 信息或 token secret。
