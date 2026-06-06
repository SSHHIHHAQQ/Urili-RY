# 2026-06-06 三端 P0/P1：Portal 拒绝审计、Legacy Guard 与管理端授权记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮输入

- 用户要求子 Agent 优先使用 `gpt-5.3-codex-spark`，不可用时使用 `gpt-5.4`。
- 本轮共接收 6 个有效只读审计结果：
  - SQL/seed 审计：legacy sys_role 清理缺 guard、legacy sys_user 回填缺 guard、管理端权限菜单存在授权缺口。
  - portal 权限/日志审计：`PortalPreAuthorizeAspect` 拒绝访问时不会写入 `seller_oper_log` / `buyer_oper_log`。
  - 前端管理模板审计：未发现当前口径下的 P0/P1 串端、路径、权限或 service 缺失。
  - 验证覆盖审计：真实 HTTP/浏览器链路测试不足，按用户要求记录为 P2，不阻塞。
  - auth 放行治理审计：`SecurityConfig` hardcode permitAll 与 `@Anonymous` 扫描未完全收敛，当前有契约测试约束，记录为 P2。
  - 管理控制面审计：菜单模板按端级全局菜单设计保留；ticket 审计全量查询继续由后台权限控制，未在本轮改成按 acting admin 默认收敛。

## 已落地修改

- `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalPreAuthorizeAspect.java`
  - 捕获端内鉴权失败分支，写入对应 terminal 的 `PortalOperLog` 失败记录。
  - 日志包含请求 URI、HTTP method、目标方法、失败状态、错误信息；能解析到端内 session 时带 `subjectId`、`accountId`、`operName`。
  - 审计写入失败不吞掉原始 401/403 业务异常。
- `RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/aspectj/PortalPreAuthorizeAspectTest.java`
  - 同步 `PortalPreAuthorizeAspect` 构造器变更。
- `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql`
  - 默认拒绝执行，必须显式设置 `@confirm_legacy_sys_user_backfill = 'BACKFILL_TERMINAL_ACCOUNTS_FROM_SYS_USER'`。
  - 回填前输出 seller/buyer legacy 命中行数和缺失 `sys_user` 行数。
- `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql`
  - 默认拒绝执行，必须显式设置 `@confirm_legacy_sys_role_cleanup = 'DISABLE_SYS_ROLE_SELLER_BUYER'`。
  - 更新前输出将影响的 `sys_role` 明细。
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - 全量 seed 增加管理端 `sys_role_menu` 授权补齐。
  - 只补 `admin` 角色，以及已经拥有 `seller:admin:list` / `buyer:admin:list` 页面权限的角色对应子权限，不默认授予普通角色。
- `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - 新增独立幂等补丁脚本，便于远程库只回放最小授权修复。

## 远程数据库执行记录

- 配置确认：`application.yml` 当前激活 profile 为 `druid`；`.env.local` 存在。
- 执行来源：本机 `.env.local` 的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- 执行方式：JShell + Maven 缓存 `mysql-connector-j`，只执行 `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`。
- 目标：当前激活配置对应的远程 MySQL。
- 本记录不输出 JDBC URL、数据库账号、数据库密码、Redis 信息或 token secret。
- 执行结果：
  - `executedStatements=3`
  - `updateCounts=[0, 67, 58]`
  - `missingAdminGrants=0`
  - `missingInheritedChildGrants=0`

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework -am '-Dtest=com.ruoyi.framework.aspectj.PortalPreAuthorizeAspectTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。

## 未采纳为本轮 P0/P1 的项

- `seller_menu` / `buyer_menu` 是否按主体隔离：当前设计为端级全局菜单模板，`seller_role` / `buyer_role` 按主体归属；`roleMenuTreeselect/{subjectId}/{roleId}` 中 subject 用于已选菜单，不用于菜单模板本身。若后续要主体级私有菜单，需要另起表设计。
- ticket 审计列表按 acting admin 默认收敛：当前保留为后台权限控制的全量审计查询；后续如果需要运营角色最小权限，可新增独立权限码和默认过滤策略。
- 登录/直登放行从 `SecurityConfig` hardcode 收敛到 `@Anonymous`：当前已有契约测试锁定该模式，后续作为治理项处理。
- 真实 HTTP、MockMvc、浏览器/DOM 覆盖不足：按用户明确要求，本轮不做浏览器运行态验收；记录为 P2。

## 边界说明

- 本轮未执行 legacy helper，只增强其 guard。
- 本轮未读写 Redis。
- 本轮未启动浏览器、未做截图/DOM/UI 细调。
- 工作区存在大量前序改动，本轮只处理上述 P0/P1 范围，未回滚或清理其他文件。

## 追加检查点：管理端授权脚本收窄与 Portal 边界守卫

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 结论

- 按用户要求先尝试 `gpt-5.3-codex-spark`；当前不可用后降级使用 `gpt-5.4`。
- seller portal、buyer portal、前端 portal/request 只读审计未发现新的 P0/P1。
- SQL/seed 只读审计发现 P1：`sys_role_menu` 子按钮补权会把任意已有卖家/买家页面权限的非 admin 角色扩成完整按钮权限。
- product/warehouse 边界只读审计提出架构 P1 候选：`warehouse` 当前直接 join `seller`，`product` 写链路通过 seller 侧实现获取卖家快照。该项不是安全小修，本轮先记录为后续架构收敛候选，不在本检查点直接重构。

### 已落地修改

- `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - 增加 `@confirm_admin_partner_role_menu_grant = 'GRANT_ADMIN_PARTNER_MENUS'` 强确认。
  - 增加 admin 角色存在与 `2010/2011/2012` 管理端菜单签名断言。
  - 子按钮补权收窄到 `role_key = 'admin'`，不再继承给任何已有页面权限的非 admin 角色。
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - 综合 seed 中同一段子按钮补权同步收窄到 `role_key = 'admin'`。
- `RuoYi-Vue/sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql`
  - 新增可选清理脚本；只清理显式列出的非 admin `role_key`。
  - 必须设置 `@confirm_admin_partner_non_admin_button_cleanup = 'CLEANUP_NON_ADMIN_PARTNER_BUTTON_GRANTS'` 和 `@admin_partner_button_cleanup_role_keys`。
  - 脚本先输出预览，再删除所选非 admin 角色在卖家/买家管理页面下的按钮权限，保留页面权限。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`
  - 增加 admin-only 子按钮授权契约。
  - 增加独立授权脚本与可选清理脚本的 guard 契约。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalAnonymousEndpointContractTest.java`
  - 端内 `@Anonymous` endpoint 必须同时声明 `@PortalPreAuthorize`、`@PortalLog`。
  - 端内受保护 endpoint 必须使用 `PortalSessionContext.requireSession(...)`。
  - 端内受保护 endpoint 方法签名不得接收 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId` 作为客户端身份范围参数。

### 远端只读核验

- 数据源确认：按当前激活配置和 `.env.local` 使用远程 MySQL；本记录不输出 JDBC URL、账号、密码、Redis 信息或 token secret。
- 查询目的：确认此前授权补丁是否已经给非 admin 角色继承按钮权限。
- 查询结果：
  - `role_id=102`，`role_key=codex_seller_audit_only`，`inherited_child_grants=32`
  - `role_id=103`，`role_key=codex_buyer_audit_only`，`inherited_child_grants=32`
  - `nonAdminPartnerButtonGrantRows=2`
- 本检查点未执行远端清理 DML；清理脚本已准备，等待明确确认后再执行。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=AdminDirectLoginPermissionContractTest,PortalAnonymousEndpointContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。

### 未执行项

- 未执行 `20260606_admin_partner_non_admin_button_grant_cleanup.sql`，因为它会删除远端 `sys_role_menu` 权限记录，按本工程规则需要明确确认。
- 未重构 `warehouse` / `product` 与 `seller` 的主体快照依赖；该项需要先定“卖家快照/主体目录服务”方案，不能在 P0/P1 快速修复里硬切。
- 未读写 Redis。
- 未启动浏览器，未做截图、DOM 或 UI 细调验收。

## 追加检查点：端内登录入口、SQL Guard 与编译收口

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 结论

- 按用户要求优先使用 `gpt-5.3-codex-spark`；当前平台不可用后降级使用 `gpt-5.4`。
- SQL/seed 审计发现 P1：全量 seed 缺少显式确认变量和管理端 `sys_menu` 槽位/签名 guard；legacy helper 的确认变量与影响范围还可收紧。
- 验证脚本审计发现 P1：`verify:three-terminal` 缺少 `PartnerSupportTest`。
- Controller 审计发现 P1：seller/buyer 管理端敏感读取接口虽有权限，但缺少 `@Log` 审计。
- Mapper 审计发现 P1：端内角色菜单读取和角色占用检查主要依赖 service 前置校验，SQL 层缺少主体范围参数。
- 后端模块审计发现 P0：`product` 对 seller lookup bean 为硬依赖，独立装配时可能启动失败。
- 前端审计发现 P1：portal 登录入口未按 seller/buyer 独立，portal 401 或 direct-login 失败可能回到管理端登录页；管理端菜单编辑缺少端隔离校验。

### 已落地修改

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - 新增 `@confirm_seller_buyer_management_seed = 'APPLY_SELLER_BUYER_MANAGEMENT_SEED'` 强确认。
  - 新增 `assert_seller_buyer_sys_menu_seed_guard()` 和管理端菜单槽位签名临时表，seed 前校验菜单 ID、path、component、routeName、perms。
- `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql`
  - 新增 `@confirm_legacy_sys_user_backfill_profile = 'LEGACY_SYS_USER_ACCOUNT_MIXED_DB'`。
  - 回填预览增加空账号字段统计，动态 update 仅处理已关联到端内账号的行。
- `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql`
  - 新增 `@confirm_legacy_sys_role_cleanup_profile = 'LEGACY_SYS_ROLE_SELLER_BUYER_CONFIRMED'`。
  - 新增 `@legacy_sys_role_cleanup_role_keys = 'seller,buyer'`，预览和更新都用 `find_in_set` 限定影响角色。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
  - 会话、账号会话、登录日志、操作日志、免密 ticket 列表补 `@Log(..., isSaveResponseData = false)`。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java`
  - 同步 seller 侧敏感读取审计。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerPortalPermissionMapper.java`
  - `selectSellerMenuIdsByRoleId`、`countSellerAccountRoleByRoleId` 增加 `sellerId` 参数。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerPortalPermissionMapper.java`
  - 同步 buyer 侧主体参数。
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml`
  - 角色菜单读取 join `seller_role` 并校验 `seller_id`、`del_flag`。
  - 角色占用检查 join `seller_account` 和 `seller_role`，SQL 层同时限定账号主体和角色主体。
- `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml`
  - 同步 buyer 侧 SQL guard。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java`
  - seller lookup bean 改为 `ObjectProvider<ProductSellerLookupService>`。
  - 缺少 seller lookup bean 时在保存商品需要卖家快照处 fail closed，不让模块装配硬失败。
- `react-ui/src/utils/portalPaths.ts`
  - 新增 portal 路径工具，统一判断 seller/buyer portal 路由和同端登录地址。
- `react-ui/config/routes.ts`
  - 新增 `/seller/login`、`/buyer/login`。
- `react-ui/src/pages/Portal/Login/index.tsx`
  - 新增 seller/buyer 端内登录页，登录成功后写入同端 portal token。
- `react-ui/src/app.tsx`、`react-ui/src/requestErrorConfig.ts`
  - portal 401 统一清端 token 并回到同端登录页，不再回管理端 `/user/login`。
- `react-ui/src/pages/Portal/Home/index.tsx`、`react-ui/src/pages/Portal/DirectLogin/index.tsx`
  - 无 token、退出、direct-login 失败都回到同端登录页。
- `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx`
  - 菜单 path、component、perms 增加端隔离校验，阻止跨 seller/buyer 端配置。
- `react-ui/scripts/check-portal-token-isolation.mjs`
  - 增加 portal login path 与 same-terminal redirect guard。
- `react-ui/scripts/check-partner-management-template.mjs`
  - 增加菜单端隔离校验 guard。
- `react-ui/scripts/verify-three-terminal.mjs`
  - 补入 `PartnerSupportTest`。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminUpstreamSystemController.java`
  - 回收未确认的 `inventory/sync`、`inventory/list` 引用，只保留已实现的 WMS 尺寸重量同步。
- `react-ui/src/services/integration/upstreamSystem.ts`
  - 删除未确认的库存同步/库存列表 service 函数，避免暴露假入口并修复 tsc 缺类型问题。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework,ruoyi-system,seller,buyer -am "-Dtest=PortalLogAspectContractTest,PortalPreAuthorizeAspectTest,AdminDirectLoginPermissionContractTest,PortalAnonymousEndpointContractTest,PortalDirectLoginAuthContractTest,PortalDirectLoginTicketSqlContractTest,StandalonePartnerSeedMenuContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,TerminalAccountIsolationTest,TerminalSeedPermissionContractTest,SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,PartnerSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。

### 未执行项与残留风险

- 未执行远端 DDL/DML，未读写 Redis。
- 未执行 `20260606_admin_partner_non_admin_button_grant_cleanup.sql`，因为它会删除远端 `sys_role_menu` 权限记录，需要明确确认。
- `seller` / `buyer` 对完整 `product` artifact 的依赖仍偏重；本轮只把 `product` 对 seller lookup 的硬装配依赖改为 fail closed，后续建议抽 contract/facade 模块。
- 来源仓库库存仍按当前记录保持占位，不新增上游库存快照表、不新增库存同步接口、不新增库存列表 service。
- 未启动浏览器，未做截图、DOM 或 UI 细调验收。
