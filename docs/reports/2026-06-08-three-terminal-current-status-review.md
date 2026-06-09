# 三端独立账号权限改造现状梳理

日期：2026-06-08

范围：

- 方向文件：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 目标追踪：`docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
- 后端：`RuoYi-Vue/`
- 前端：`react-ui/`
- SQL：`RuoYi-Vue/sql/`

本次动作：只读梳理代码、文档、SQL 和验证记录；未执行 DDL/DML，未改业务代码，未重启服务。

## 一、总体结论

当前三端独立方向已经不是方案阶段，已经进入“账号权限基础能力已落地，继续做 P0/P1 收口和真实业务接口范围接入”的阶段。

按 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的阶段拆分看：

| 阶段 | 当前判断 | 说明 |
| --- | --- | --- |
| 阶段 0：冻结旧方向 | 已完成 | 新方向已明确：管理端继续若依 `sys_*`，卖家/买家端不再复用 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 做端内账号权限。 |
| 阶段 1：账号表改造方案 | 已完成 | `seller_account` / `buyer_account`、端内角色、菜单、部门、日志、会话、免密票据表方案已写入 SQL 和目标追踪。 |
| 阶段 2：数据库迁移 | 历史记录显示已执行 | 目标追踪记录显示远程库已执行三端隔离迁移，并完成核心表、旧 `user_id` 移除、关键字段和索引核验。本轮因本机无 `mysql` / `mariadb` / `mysqlsh` 客户端，未重新 live 查询远程库。 |
| 阶段 3：后端认证和权限改造 | 主链路已完成，真实业务接口继续推进 | seller/buyer 登录、token、session、免密消费、getInfo/getRouters、端内权限校验器、端内日志和会话已经落地；后续真实业务接口仍需逐个接入端 token 主体范围。 |
| 阶段 4：管理端控制能力 | 第一版已完成 | 管理端已能维护卖家/买家主体、账号、部门、角色、菜单、账号角色、会话、强退、登录日志、操作日志、免密票据。 |
| 阶段 5：前端三端物理拆分 | 未开始 | 当前仍用 `react-ui/` 同时承载管理端验证和 seller/buyer portal 验证入口，尚未拆出 `admin-ui` / `seller-ui` / `buyer-ui`。 |

当前实际计划不是继续大改身份模型，而是沿着已确认模型做两类事：

1. 继续压 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
2. 继续把真实卖家端/买家端业务接口接入 `@PortalPreAuthorize`、`@PortalLog` 和端 token 推导范围，避免前端传 `sellerId` / `buyerId` 决定数据边界。

## 二、核心设计

### 1. 三端账号权限边界

管理端保留若依后台体系：

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_dept`
- `sys_user_role`
- `sys_role_menu`
- `sys_oper_log`
- `sys_logininfor`

卖家端独立：

- `seller_account`
- `seller_role`
- `seller_menu`
- `seller_dept`
- `seller_account_role`
- `seller_role_menu`
- `seller_login_log`
- `seller_oper_log`
- `seller_session`

买家端独立：

- `buyer_account`
- `buyer_role`
- `buyer_menu`
- `buyer_dept`
- `buyer_account_role`
- `buyer_role_menu`
- `buyer_login_log`
- `buyer_oper_log`
- `buyer_session`

免密代入票据是平台共享审计表：

- `portal_direct_login_ticket`

这张表不代表端内账号体系复用，只记录管理端代入 seller/buyer 时的票据、目标主体、目标账号、acting admin、原因、过期、使用状态和 token hash。

### 2. 后端模块设计

`RuoYi-Vue/pom.xml` 已包含独立业务模块：

- `seller`
- `buyer`
- `integration`
- `warehouse`
- `product`
- `inventory`

卖家端和买家端有各自的 controller、service、mapper：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/...`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/...`

公共支撑放在若依系统/框架层：

- `PortalTokenSupport`：seller/buyer portal token、JWT claim、Redis key。
- `PortalDirectLoginSupport`：免密 token 生成、hash、票据、消费。
- `PortalPermissionChecker`：端内统一权限校验。
- `PortalPreAuthorizeAspect`：`@PortalPreAuthorize` 切面。
- `PortalLogAspect` / `PortalOperLogServiceImpl`：端内操作日志。
- `PortalPermissionSupport`：端内菜单、权限前缀、菜单 ID 段、可读菜单校验。

### 3. 路由设计

管理端控制接口仍是管理端身份访问，使用若依后台权限：

- `/seller/admin/sellers/**`
- `/buyer/admin/buyers/**`
- `/seller/admin/menus/**`
- `/buyer/admin/menus/**`

卖家端 portal 接口：

- `/seller/login`
- `/seller/direct-login`
- `/seller/getInfo`
- `/seller/getRouters`
- `/seller/account/password`
- `/seller/account/login-logs`
- `/seller/account/oper-logs`
- `/seller/account/sessions`
- `/seller/product/**`

买家端 portal 接口：

- `/buyer/login`
- `/buyer/direct-login`
- `/buyer/getInfo`
- `/buyer/getRouters`
- `/buyer/account/password`
- `/buyer/account/login-logs`
- `/buyer/account/oper-logs`
- `/buyer/account/sessions`
- `/buyer/product/**`

端内接口使用 `@PortalPreAuthorize(terminal = "seller" | "buyer", hasPermi = "...")`，不是若依 `@PreAuthorize("@ss.hasPermi(...)")`。

## 三、若依基础架构复用情况

有复用，但边界很明确。

### 管理端继续复用若依

管理端仍复用：

- 若依登录、token、后台角色、菜单、权限。
- `@PreAuthorize("@ss.hasPermi('...')")`。
- `@Log` 和 `sys_oper_log`。
- `BaseController`、`AjaxResult`、`TableDataInfo`。
- `sys_menu` 做管理端菜单和按钮权限 seed。
- `sys_config` 做 portal 地址等平台配置。
- `RedisCache`、`SecurityUtils` 等若依公共基础设施。

这意味着管理端还是若依后台，不重写一套后台 RBAC。

### seller/buyer 端不复用若依 `sys_*` 做端内账号权限

卖家/买家端不再把端账号塞进：

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_dept`
- `sys_user_role`
- `sys_role_menu`

代码层已有静态合同守住这一点：

- `TerminalAccountIsolationTest`
- `TerminalSqlIsolationContractTest`
- `TerminalRoleMenuMapperIsolationContractTest`

当前端账号读取也已从裸 `accountId` 改成主体和账号组合：

- seller：`selectSellerAccountByIdAndSellerId(sellerId, sellerAccountId)`
- buyer：`selectBuyerAccountByIdAndBuyerId(buyerId, buyerAccountId)`

### 前端复用 Ant Design Pro / RuoYi React 基础

当前 `react-ui/` 仍作为管理端验证入口，复用：

- Umi / Ant Design Pro 路由和布局。
- ProTable。
- 远程菜单 `/getRouters`。
- 前端 `access` 权限判断。
- `PartnerManagement` 同构模板。

卖家/买家管理页面共用 `PartnerManagement` 模板，只替换 terminal、文案、权限前缀、字段配置和 service。

## 四、已经做了什么

### 1. 数据库和 SQL

已形成主迁移脚本：

- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`

该脚本包含：

- `seller_account` / `buyer_account`
- `seller_dept` / `buyer_dept`
- `seller_role` / `buyer_role`
- `seller_menu` / `buyer_menu`
- `seller_account_role` / `buyer_account_role`
- `seller_role_menu` / `buyer_role_menu`
- `seller_login_log` / `buyer_login_log`
- `seller_oper_log` / `buyer_oper_log`
- `seller_session` / `buyer_session`

已形成免密票据脚本：

- `RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`

已形成菜单 ID 段和自增保护：

- seller 菜单 ID：`100000-199999`
- buyer 菜单 ID：`200000-299999`
- `20260607_terminal_menu_id_range_isolation.sql`
- `20260608_terminal_menu_auto_increment_reset.sql`

SQL 风格已经从“直接执行”收敛为：

- 确认 token。
- 精确 count。
- SHA-256 signature。
- `signal sqlstate '45000'` fail-closed。
- dynamic DDL helper 纳入 `SqlExecutionGuardContractTest`。

### 2. 后端管理端控制能力

seller 管理端已有：

- 卖家主体列表、详情、新增、编辑、状态。
- 卖家账号列表、新增、编辑、锁定、解锁、重置密码。
- 卖家账号角色查询和分配。
- 卖家部门维护。
- 卖家角色维护。
- 卖家菜单维护。
- 卖家会话列表、账号会话列表、主体强退、账号强退。
- 卖家免密代入、账号级免密代入。
- 卖家登录日志、操作日志、免密票据审计列表。

buyer 管理端已有同构能力：

- 买家主体、账号、部门、角色、菜单、会话、强退、免密、审计列表。

这些管理端接口用若依 `@PreAuthorize` 和 `@Log`，仍由管理端 `sys_*` 控制谁能操作。

### 3. 后端端内登录、权限、菜单

seller/buyer 已有端内登录：

- `SellerPortalAuthController`
- `BuyerPortalAuthController`

登录结果中有：

- `terminal`
- `subjectId`
- `subjectNo`
- `accountId`
- `token`
- `expireMinutes`

token 支撑：

- JWT claim 写入 `portal_terminal` 和 `portal_login_key`。
- Redis key 使用 `portal_login_tokens:{terminal}:{tokenId}`。
- 取 session 时同时校验 expected terminal、JWT terminal 和 Redis session terminal。

端内权限：

- seller 从 `seller_account_role`、`seller_role_menu`、`seller_menu` 读取角色、权限和菜单。
- buyer 从 `buyer_account_role`、`buyer_role_menu`、`buyer_menu` 读取角色、权限和菜单。
- 菜单读出后会复核菜单 ID 段、component、权限前缀，避免脏菜单下发给 portal。

### 4. 免密代入

已完成：

- 管理端生成一次性 token。
- token 明文只返回给调用方和 Redis payload。
- DB 中只存 SHA-256 `token_hash`。
- Redis payload key 使用 `portal_direct_login:{terminal}:{token_hash}`。
- 旧无 terminal key 只作为清理对象，不作为读取依赖。
- 票据消费时校验 terminal、ticket、目标主体、目标账号、过期、状态。
- 管理端成功提示等待 portal popup 消费成功后回传 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE`。

### 5. 端内日志和会话

已完成：

- `seller_login_log` / `buyer_login_log`
- `seller_oper_log` / `buyer_oper_log`
- `seller_session` / `buyer_session`
- direct-login 审计字段：
  - `direct_login`
  - `direct_login_ticket_id`
  - `acting_admin_id`
  - `acting_admin_name`
  - `direct_login_reason`

端内自助日志接口返回 DTO，避免暴露内部审计字段：

- `PortalOwnLoginLogProfile`
- `PortalOwnOperLogProfile`
- `PortalOwnSessionProfile`

### 6. 前端管理端

当前管理端 seller/buyer 页面使用同构模板：

- `react-ui/src/components/PartnerManagement/PartnerManagementPage.js`
- `PartnerAccountModal`
- `PartnerAccountRoleModal`
- `PartnerDeptModal`
- `PartnerRoleModal`
- `PartnerSessionModal`
- `PartnerAuditModal`

seller service：

- `react-ui/src/services/seller/seller.ts`

buyer service：

- `react-ui/src/services/buyer/buyer.ts`

这些 service 覆盖账号、部门、角色、菜单、会话、免密、日志、票据。

### 7. 前端 portal 验证入口

当前 `react-ui/` 里已有：

- `/seller/login`
- `/buyer/login`
- `/seller/direct-login`
- `/buyer/direct-login`
- `/seller/portal`
- `/buyer/portal`

前端 token key 已分端：

- admin：`access_token`
- seller：`seller_access_token`
- buyer：`buyer_access_token`

portal 请求：

- `/api/seller/admin/**` 和 `/api/buyer/admin/**` 不当作 portal 请求。
- `/api/seller/**` 和 `/api/buyer/**` 的非 admin 请求按当前端清 token 并跳对应登录页。
- portal query 会过滤 `sellerId`、`buyerId`、`subjectId`、`accountId`、`terminal` 等客户端身份范围字段。

## 五、还没做什么

### 1. 前端三端物理拆分未开始

当前还没有：

- `admin-ui/`
- `seller-ui/`
- `buyer-ui/`

seller/buyer 只是 `react-ui/` 内的验证入口和工作台模板。真正物理拆分要等账号、端入口、菜单域、权限模型、管理端控制权继续稳定后再做。

### 2. 端内真实业务接口还要继续接入范围控制

基础登录、权限、菜单、日志、会话已经有模板，但后续真实业务接口仍要逐个做到：

- 从 `PortalSessionContext` / token 推导 `sellerId` / `buyerId`。
- 不相信前端传入的 `sellerId` / `buyerId`。
- 接入 `@PortalPreAuthorize`。
- 接入 `@PortalLog`。
- Mapper SQL 层下推主体范围，而不是 Java 层事后过滤。

已有商品分类、商品 Schema、商城商品只读接口属于第一批模板，不代表所有业务域都完成。

### 3. 浏览器运行态不是当前 P0/P1 阻塞项

近期目标追踪明确：当前快速推进只处理 P0/P1，不做浏览器运行态、截图、DOM 检测或 UI 细调。本轮也没有启动浏览器验证。

### 4. 本轮未重新 live 查询远程数据库

本轮已读取激活配置：

- 后端激活 profile：`druid`
- MySQL URL 来自 `RUOYI_DB_URL`
- Redis 来自 `RUOYI_REDIS_*`

但本机没有 `mysql` / `mariadb` / `mysqlsh` 客户端，本轮没有重新查询远程库。关于远程库已执行和核验的结论来自现有目标追踪记录，而不是本轮实时复核。

### 5. 当前 P2 残留

目标追踪和只读审计中保留的非阻塞项主要是：

- 历史 `.js` mirror 仍有部分不是纯 re-export，但当前 P0/P1 gate 相关项已收口。
- `app.tsx` 与 `requestErrorConfig.ts` 的 401 分流有重复逻辑，行为一致，后续可抽 helper。
- direct-login popup 超时提示仍偏通用，可后续优化排障文案。
- legacy direct-login Redis key 名称仍在清理逻辑里出现，当前不读取旧 key，后续可下沉到一次性清理脚本。
- 管理端审计查询的 `accountId + subjectId` fail-closed 主要靠运行时服务层守卫，后续可继续补更强架构合同。

## 六、当前验证状态

现有最新记录显示：

- `npm run verify:three-terminal` 曾多轮通过。
- 前端 guard、React typecheck、Jest、后端合同测试已纳入 `react-ui/scripts/verify-three-terminal.mjs`。
- `react-ui/tests/three-terminal.manifest.json` 登记了 seller/buyer、portal、direct-login、权限、SQL guard、product/inventory/integration/warehouse/finance 等关键测试。
- `SqlExecutionGuardContractTest` 已覆盖日期前缀 SQL 自动发现、高影响 SQL 确认 token、dynamic DDL、菜单 seed owner/slot/signature、三端隔离迁移、免密票据、端内菜单 ID 段等。

本轮没有重新跑完整验证命令；本轮只是现状梳理。

## 七、下一步建议

建议下一步不要再重复讨论“是否用 `sys_user` 承载卖家/买家账号”，这个方向已经冻结并被代码/SQL/合同测试约束。

更实际的推进顺序：

1. 选一个真实 seller portal 业务接口，按现有模板接入 `@PortalPreAuthorize`、`@PortalLog` 和 token 主体范围。
2. 用同构方式复制到 buyer，只替换 terminal、路径、权限点、DTO 可见字段和 service。
3. 每个接口补 service 层单测和合同测试，登记到 `three-terminal.manifest.json`。
4. 管理端 UI 继续复用 `PartnerManagement` 和已确认 ProTable 模板，不重新设计同构页面。
5. 等 seller/buyer 真实业务入口稳定后，再启动 `seller-ui` / `buyer-ui` 物理拆分方案。
