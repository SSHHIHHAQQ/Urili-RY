# 2026-06-07 三端 P0/P1 子 Agent 复核收口记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮执行快速推进模式：只处理 P0/P1 编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 或 UI 细调。

## 子 Agent 情况

- 历史记录（已过期口径）：按当时用户要求优先尝试 `gpt-5.3-codex-spark`，实际命中额度限制。现行规则已改为默认使用 `gpt-5.4`，不要再把 GPT-5.3 Codex 作为首选。
- 已降级使用并关闭 6 个 `gpt-5.4` 只读扫描子 Agent。
- 采纳的 P1 结论：前端 service 缺 `/resetPwd` 调用、三端验证漏跑 `SysMenuServiceImplTest`、账号角色查询权限偏宽、登录日志缺 direct-login 筛选、匿名登录审计可能归属旧 token、seed 覆盖端地址、前端审计类型缺字段。

## 已完成

- `react-ui` seller/buyer service 补齐账号自定义密码重置接口：
  - `/api/seller/admin/sellers/${sellerId}/accounts/${sellerAccountId}/resetPwd`
  - `/api/buyer/admin/buyers/${buyerId}/accounts/${buyerAccountId}/resetPwd`
- `verify-three-terminal.mjs` 纳入 `SysMenuServiceImplTest`，防止远程菜单 `perms` payload 回归漏测。
- seller/buyer 账号角色查询接口增加双权限门槛：
  - `*:admin:account:role:query`
  - `*:admin:role:query`
- seller/buyer 登录日志查询补齐 direct-login 结构化筛选：
  - `directLogin`
  - `directLoginTicketId`
  - `actingAdminId`
  - `actingAdminName`
  - `directLoginReason`
- `PortalLogAspect` 对 `allowAnonymous=true` 的登录/免密登录接口不再先读取请求头旧 token；成功时只从本次登录结果解析新 session，失败时按 anonymous 记录。
- `seller_buyer_management_seed.sql` 对 `portal.seller.web.url` / `portal.buyer.web.url` 改为缺失插入，不再覆盖已有环境配置。
- 管理端强制踢出、账号锁定/停用导致的强退、密码重置导致的强退，登录日志 `actingAdmin*` 改为当前执行控制动作的后台管理员；direct-login 会话仍保留 `directLogin` / `directLoginTicketId` 标记会话来源。
- `PartnerAuditModal` 拆分登录日志、操作日志、免密票据类型，去掉审计表统一 `Record<string, any>`。
- `PortalOperLog` 前端类型补齐 direct-login 审计字段。
- seller/buyer 列表 params 类型补齐若依区间查询键。
- `AGENTS.md` 和复用台账同步新增端地址 seed 不覆盖、后台控制动作 actor 规则。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system,ruoyi-framework -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalDirectLoginAuthContractTest,PortalLogAspectContractTest,SqlExecutionGuardContractTest,SysMenuServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 5 个 suite / 21 个测试通过，后端三端合同与 seller/buyer 模块测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；结果为 `Synced 4 changed files`，`Modified: 4 - 284 nodes in 665ms`。

## 边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器、截图、DOM 或 UI 细调验收。

## 残留 P1

- 免密登录管理端提示目前仍以弹窗消息链路成功为准，未等待 portal 端真正消费 token 后 ack。
- 历史记录（已过期口径）：当时管理端账号“重置密码”主要入口仍是默认密码重置，自定义临时密码弹窗需要另起前端交互切片。当前实现已由后续检查点覆盖：管理端账号“重置密码”已接入人工临时密码 `resetPwd`，默认密码重置入口已移除。
- 账号行缺少账号级审计入口；现有审计弹窗仍以主体级入口为主。
- `20260604_three_terminal_isolation_migration.sql` 仍需按脚本单独设计半执行保护、preflight 拆分或不可事务化原因说明。
- `seller_buyer_management_seed.sql` 仍同时承担 fresh bootstrap 和增量修补职责，后续应拆分或增加 profile/freshness guard。
