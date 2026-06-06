# 2026-06-06 三端 P0/P1：认证审计链与 Guard 覆盖补强记录

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本轮不做浏览器运行态验收、不做截图、不做 DOM 检测、不做 UI 细调。

## 子 Agent 使用情况

- 按用户要求使用 `gpt-5.4` 并行启动 6 个只读子 Agent，覆盖 seller 后端、buyer 后端、framework/system、React portal、SQL/seed、验证脚本/合同测试。
- 6 个子 Agent 均已关闭。
- 采纳并修复的 P1：
  - buyer 认证入口缺端类型审计边界；主代理按双端同构同步修复 seller/buyer。
  - 免密代入成功后 acting admin 未进入 portal session 和后续端内操作日志审计链。
  - React portal / partner / 商品 guard 对响应身份字段、敏感字段和 `.js` 副本覆盖不足。
  - legacy `sys_user` 回填 helper 在已完成三端隔离的新库上可能空跑。
  - 部分高影响 SQL helper 未纳入统一 SQL guard 合同。
  - `TerminalRouteOwnershipTest` 和 `PortalAnonymousEndpointContractTest` 的 controller 发现/handler 判断过窄。

## 新增问题

- P1：`PortalLoginResultData` 只禁止 `subjectId/accountId`，未禁止 `sellerId/buyerId/sellerAccountId/buyerAccountId` 回流。
- P1：`check-partner-management-template.mjs` 未覆盖 `seller.d.ts` / `buyer.d.ts` 的读模型敏感字段。
- P1：`check-seller-portal-product-template.mjs` / `check-buyer-portal-product-template.mjs` 未覆盖并存 `.js` 副本。
- P1：`SellerPortalAuthController` / `BuyerPortalAuthController` 登录和免密登录入口没有 `@PortalLog` 端类型审计边界。
- P1：免密代入 ticket 的 `actingAdminId` / `actingAdminName` / `reason` 未进入 `PortalLoginSession`，后续端内操作日志难以区分普通登录和管理端代入。
- P1：legacy `sys_user` backfill helper 在没有 legacy `user_id` 列或没有 legacy 绑定行时不 fail-fast。
- P1：`20260606_admin_partner_role_menu_grant.sql`、`20260606_admin_partner_non_admin_button_grant_cleanup.sql`、`20260606_legacy_disable_sys_seller_buyer_roles.sql` 未纳入统一 `SqlExecutionGuardContractTest`。
- P1：`TerminalRouteOwnershipTest` 依赖文件名包含 `Portal`，`PortalAnonymousEndpointContractTest` 依赖固定行窗。

## 已修复问题

- `check-portal-token-isolation.mjs`：
  - `PortalLoginResultData` 改为白名单响应契约。
  - 禁止 `sellerId`、`buyerId`、`sellerAccountId`、`buyerAccountId` 等端内身份字段回流到 portal 登录响应。
- `check-partner-management-template.mjs`：
  - 纳入 `Seller`、`Buyer`、`SellerAccount`、`BuyerAccount` 和对应读结果类型。
  - 禁止读模型暴露 `password`、`token`、`refreshToken`、`directLoginToken`、`loginUrl`、`tokenHash`、`authorization`、`accessToken`。
- seller/buyer 商品 guard：
  - 同时检查 `.tsx/.ts` 和 `.js` 副本。
  - `Portal/Home/index.js` 显式 re-export `index.tsx` 时视为跟随 TSX，不要求重复渲染。
- `PortalLog` / `PortalLogAspect`：
  - 增加 `allowAnonymous`，默认不放宽缺 session 行为。
  - seller/buyer 登录和免密登录入口显式 `allowAnonymous = true`、`isSaveResponseData = false`。
  - 免密代入 session 后续端内操作日志会把 `directLoginAudit{ticketId, actingAdminId, actingAdminName, reason}` 追加到 `oper_param`。
- `PortalDirectLoginToken` / `PortalLoginSession` / `PortalTokenSupport`：
  - 免密 payload 和 Redis session 新增 ticket、acting admin、reason 字段传递。
  - 普通登录路径仍走原入口，不设置 direct-login 审计字段。
- seller/buyer service：
  - 免密登录调用 `createLogin(..., PortalDirectLoginToken)`。
  - 登录成功日志 `msg` 带 `ticketId` 和 `actingAdminName`。
- SQL helper 和合同：
  - legacy `sys_user` backfill 新增 `assert_legacy_user_id_binding_exists()`，要求旧库存在 legacy `user_id` 列且至少一行绑定。
  - 三个高影响 admin/legacy helper 纳入 `SqlExecutionGuardContractTest`。
  - `SqlExecutionGuardContractTest` 固定确认调用必须出现在执行区第一个高影响 DDL/DML 前。
- 后端合同测试：
  - `PortalAnonymousEndpointContractTest` 改为 handler 级解析，并纳入 auth controller 专用契约。
  - `TerminalRouteOwnershipTest` 不再只按 `*Portal*Controller.java` 文件名发现 portal controller。
  - `PortalDirectLoginAuthContractTest` 固定 acting admin 进入 session 和 oper log 审计链。
  - `PortalTokenSupportTest` 固定 direct-login audit 写入 Redis session。

## 残留问题

- P2：免密 acting admin 当前长期留痕通过登录日志 `msg` 和后续端内操作日志 `oper_param` 完成；`seller_session` / `buyer_session` 表本身未扩展 acting admin 字段。若后续要在会话表长期保留代入来源，需要单独提交 DDL 方案并确认。
- P2：前端 partner-management / portal-product 仍主要依赖静态 guard，未新增 Jest 组件语义测试。本轮按快速模式先补 P1 guard 覆盖，不扩大到 UI 组件运行态。
- P2：`seller_menu` / `buyer_menu` 是端级菜单池还是 subject-scoped 菜单池仍需后续设计澄清。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=SqlExecutionGuardContractTest,PortalAnonymousEndpointContractTest,TerminalRouteOwnershipTest,PortalDirectLoginAuthContractTest,PortalTokenSupportTest,PortalLogAspectContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；ruoyi-system `21`、ruoyi-framework `1`、seller `45`、buyer `45` 测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`；后端三端合同中 ruoyi-system `85`、ruoyi-framework `15`、seller `64`、buyer `65` 测试通过；portal Jest `3` 个 suite、`7` 个测试通过。

## 未验证原因

- 未启动浏览器、未截图、未做 DOM 检测或 UI 细调，因为用户已明确排除。
- 未重启后端服务，本轮以代码、guard、typecheck、Jest 和 Maven 合同测试为准。
- 未连接 Redis。
- 未执行远程 MySQL DDL/DML，本轮只修改 SQL 文件和契约测试。

## 权限检查结果

- seller/buyer 认证入口仅增加 `@PortalLog(... allowAnonymous = true)`，没有给登录接口套 `@PortalPreAuthorize`，避免登录被已登录 session 依赖拦死。
- 管理端权限仍归若依 `sys_*`，端内权限仍归 `seller_*` / `buyer_*`。
- 免密代入后，portal session 和端内操作日志 now 能携带 acting admin 审计来源。

## 字典/选项复用检查结果

- 本轮未新增字典或业务选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，补充匿名认证入口审计、免密 acting admin 链路、React guard 覆盖和 SQL guard 规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出显示 `Synced 22 changed files`，本机 `.codegraph/` 索引已更新。
- `.codegraph/` 仍按本机索引目录处理，不作为业务代码、报告或迁移产物提交。

## 大文件合理性判断结果

- `PortalAnonymousEndpointContractTest.java` 重写为 handler 级解析，职责仍单一：固定 portal 匿名/认证入口边界。
- seller/buyer service 测试文件已较大，但本轮只补同构断言和测试桩重载，不做拆分，避免在 P0/P1 快速模式引入结构性改动。

## 重复代码检查结果

- seller/buyer service、测试和商品 guard 按既定同构模板机械同步，只替换 terminal、命名和字段。
- `.ts/.tsx` 与 `.js` 并存是当前前端工作区既有现状，本轮没有新增业务分叉，只把 guard 覆盖同步到两套文件。
