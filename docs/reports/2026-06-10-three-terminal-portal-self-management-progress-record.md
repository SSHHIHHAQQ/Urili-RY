# 2026-06-10 三端 Portal 最小权限框架推进记录

本记录对应目标：以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向，在 `react-ui` 作为当前三端前端终局载体的前提下，推进“卖家端 / 买家端最小可登录权限框架”。本轮进入快速推进模式，只处理 P0/P1，不扩展商品、订单、库存、物流、财务、履约、外部系统等业务菜单和业务页面。

## 本轮范围

- 管理端继续保留若依 `sys_*` 后台体系。
- 卖家端、买家端继续使用独立的账号、角色、部门、菜单模板、日志和会话控制面。
- Portal 自助管理只开放最小闭环：账号管理、角色管理、部门管理、登录日志、操作日志、在线会话。
- 端内角色只能从平台预置的 `seller_menu` / `buyer_menu` 权限模板中分配权限。
- 本轮不做浏览器截图、DOM/UI 细调、视觉优化或完整业务菜单铺设。

## 已完成

- 后端补齐 seller/buyer portal 自助管理接口：
  - 账号新增、编辑、账号角色查询和分配。
  - 部门详情、树、创建、编辑、删除。
  - 角色详情、菜单模板、创建、编辑、删除。
- 后端 portal 自助接口均从 `PortalSessionContext` 推导当前端和当前主体，未信任前端传入 `sellerId` / `buyerId`。
- 后端新增 `PortalActorSupport`，用于 portal 自助写操作记录当前 portal 登录人；管理端强制控制审计仍保留当前管理端账号。
- 角色菜单保存前增加最小自助权限模板过滤，避免 portal 自助角色分配 product 等冻结业务权限。
- 角色菜单编辑查询已按 self-management 菜单 ID 过滤 `checkedKeys`，避免已有角色的冻结业务菜单 ID 被 portal 自助入口带出。
- 角色菜单提交遇到不存在、跨端或非 self-management 菜单 ID 时明确 fail-closed。
- 前端 `react-ui` 增加 `PortalSelfManagement`，在 portal 首页接入账号、部门、角色和审计信息的最小自助管理闭环。
- 前端 portal service 增加自助管理接口，并在提交前剥离 caller-controlled scope 字段。
- SQL/seed 增补 seller/buyer 最小自助管理权限点，并新增 `20260610_portal_self_management_permission_seed.sql`：
  - 包含确认 token。
  - 包含 menu ID 区间、权限前缀、空权限、通配权限、admin 命名空间和 role-menu 完成态校验。
  - 本轮未执行该 SQL。
- 合同测试补强：
  - portal 自助接口必须从 token subject 调用 service。
  - 角色模板必须限制为 self-management 权限集合。
  - 角色 `checkedKeys` 必须按 self-management 权限集合过滤。
  - `PortalSelfManagement` 必须通过 `PORTAL_SERVICE` 访问端内接口，不能直连 `/api/seller` / `/api/buyer`，并固定账号、角色、部门、日志和会话相邻闭环入口。
  - 新 seed 和新增权限点纳入 SQL/seed 合同。

## 数据源与 SQL

- 已读取当前激活配置：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 当前 profile 为 `druid`。
- MySQL 连接由 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 注入。
- Redis 连接由 `RUOYI_REDIS_HOST` / `RUOYI_REDIS_PORT` / `RUOYI_REDIS_DATABASE` / `RUOYI_REDIS_PASSWORD` 注入。
- 本轮未读取 `.env.local` 明文，未执行远端 MySQL DDL/DML，未读写 Redis。
- 新增 seed 需要用户确认目标环境后再执行。

## 验证结果

- 通过：`cd react-ui; node scripts/check-portal-token-isolation.mjs`
  - `Portal token isolation guard passed.`
- 通过：`cd react-ui; npm run tsc`
- 通过：`cd react-ui; npx jest --config jest.config.js tests/portal-session-request.test.ts tests/portal-home-error-handling.test.ts --runInBand`
  - 2 suites / 45 tests passed。
- 通过：`cd react-ui; npx jest --config jest.config.js tests/portal-self-management-contract.test.ts --runInBand`
  - 1 suite / 4 tests passed。
- 通过：`cd react-ui; node scripts/verify-three-terminal.mjs --check-manifest`
  - `three-terminal manifest check passed.`
- 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest,SqlExecutionGuardContractTest,TerminalSeedPermissionContractTest,TerminalRouteOwnershipTest,TerminalSqlIsolationContractTest,PortalLoginSessionConsistencyContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 105 tests passed，reactor BUILD SUCCESS。
- 通过：`cd react-ui; npm run verify:three-terminal`
  - `three-terminal verification passed.`
  - 前端 29 suites / 244 tests passed。
  - 后端 reactor test-compile 15 个模块 BUILD SUCCESS。
  - 后端三端合同 12 个模块 BUILD SUCCESS。
- 通过：`cd E:\Urili-Ruoyi; git diff --check -- <本轮三端 portal 相关文件>`
  - 无 whitespace error，仅有 CRLF 工作区提示。
- 通过：`cd E:\Urili-Ruoyi; codegraph sync .`
  - 退出码 0。
  - 本轮代码同步摘要：`Synced 5 changed files`；追加 live 脚本同步摘要：`Synced 1 changed files`。

## 追加复核：自助写接口主体边界

- 复核时间：2026-06-11。
- 复核范围：seller/buyer portal 自助账号、角色、部门写接口，以及登录日志、操作日志、在线会话等相邻只读接口。
- 前端证据：
  - `react-ui/tests/portal-session-request.test.ts` 对账号创建/编辑、部门创建/编辑、角色创建/编辑构造了带 `sellerId` / `buyerId` / `subjectId` / `accountId` 的 caller-controlled payload。
  - 同一测试用例断言最终 `request` 只发送清洗后的 `data` / `params`，不把上述主体或账号作用域字段透传给后端。
  - audit 和 portal 商品相邻接口也保留了 scope stripping 断言，用于防止后续服务方法误把前端主体字段当作可信来源。
- 后端证据：
  - `SellerPortalController` / `BuyerPortalController` 的自助写接口统一调用 `PortalSessionContext.requireSession("seller")` / `PortalSessionContext.requireSession("buyer")`，并使用 `session.getSubjectId()` 调用 service。
  - 账号角色分配路径变量使用 `targetAccountId`，避免把主体 ID 与账号 ID 混淆。
  - `SellerPortalPermissionServiceImplTest` / `BuyerPortalPermissionServiceImplTest` 已覆盖跨主体账号拒绝、跨主体角色拒绝、禁用角色拒绝、OWNER 账号不得丢失 OWNER 角色、非 OWNER 账号不得获得 OWNER 角色。
  - portal 角色菜单查询和保存已限制在 self-management 菜单集合，提交不存在、跨端或非 self-management `menuId` 时 fail-closed。
- 结论：本次只读复核未发现新的 P0/P1 缺口；剩余风险仍集中在新增 seed 未落库和 live portal 闭环未验证。

## 追加修复：新主体 OWNER 默认自助权限

- 修复时间：2026-06-11。
- 发现问题：
  - 新增 `20260610_portal_self_management_permission_seed.sql` 可以给已有 active OWNER 角色补齐本轮自助管理权限。
  - 但 `SellerServiceImpl` / `BuyerServiceImpl` 的 `DEFAULT_OWNER_PERMS` 仍只包含 `portal:home`、账号/角色/部门列表、日志和会话查看权限。
  - 这会导致管理端后续新建卖家/买家主体时，自动创建的 OWNER 主账号缺少账号新增/编辑、账号角色查询/分配、部门详情/新增/编辑/删除、角色详情/新增/编辑/删除权限，无法满足本轮 portal 最小自助闭环。
- 已修复：
  - `SellerServiceImpl.DEFAULT_OWNER_PERMS` 补齐 seller self-management 全量权限集合。
  - `BuyerServiceImpl.DEFAULT_OWNER_PERMS` 补齐 buyer self-management 全量权限集合。
  - `PortalSelfServiceSurfaceContractTest` 增加 owner 默认 self-management 权限合同，防止后续 seed 和新主体默认授权再次漂移。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：`PortalSelfServiceSurfaceContractTest` 1 test passed，reactor BUILD SUCCESS。
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest,PortalSelfServiceSurfaceContractTest,SqlExecutionGuardContractTest,TerminalSeedPermissionContractTest,TerminalRouteOwnershipTest,TerminalSqlIsolationContractTest,PortalLoginSessionConsistencyContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：ruoyi-system 105 tests passed，seller `SellerServiceImplTest` 63 tests passed，buyer `BuyerServiceImplTest` 64 tests passed，reactor BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 29 suites / 244 tests passed；后端 reactor 和三端合同均 BUILD SUCCESS。

## 追加加固：自助权限集合精确合同

- 加固时间：2026-06-11。
- 加固原因：
  - 上一轮合同已要求 `SellerServiceImpl` / `BuyerServiceImpl` 包含本轮 self-management 权限字符串。
  - 但“包含字符串”不能阻止后续把商品、订单、库存、物流、财务、履约、外部系统等冻结业务权限额外塞进 OWNER 默认授权或 portal 自助角色模板。
- 已加固：
  - `PortalSelfServiceSurfaceContractTest` 现在会从源码中抽取 `PORTAL_SELF_MANAGEMENT_PERMS` 和 `DEFAULT_OWNER_PERMS` 的字符串字面量。
  - seller/buyer 两端均要求该集合与本轮 19 个 self-management 权限精确一致：`portal:home`、账号管理、账号角色、登录日志、操作日志、在线会话、部门管理、角色管理。
  - 复核 `seller_buyer_management_seed.sql`：历史商品只读权限定义仍存在于综合 seed 的终端菜单模板中，但当前 OWNER 授权块不授予这些商品权限；本轮新增 `20260610_portal_self_management_permission_seed.sql` 后续进一步收敛为只保留 self-management 授权。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：`PortalSelfServiceSurfaceContractTest` 1 test passed，reactor BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 29 suites / 244 tests passed；后端 reactor 和三端合同均 BUILD SUCCESS。

## 追加加固：管理端直登 OWNER 与消费回执合同

- 加固时间：2026-06-11。
- 复核结论：
  - 管理端主体级直登 `createSellerDirectLogin` / `createBuyerDirectLogin` 当前默认查询对应主体的 OWNER 主账号。
  - 管理端账号级直登 `createSellerAccountDirectLogin` / `createBuyerAccountDirectLogin` 当前使用 `subjectId + accountId` 的端内作用域查询，未使用裸账号 ID。
  - 前端主体直登和账号直登入口当前都会 `await openPortalDirectLoginWindow(...)`，只有目标 portal 回传匹配的 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 成功结果后才提示“免密登录已确认”。
- 已加固：
  - `AdminDirectLoginPermissionContractTest` 增加后端合同，固定主体级直登必须使用 OWNER，账号级直登必须使用端内 scoped account 查询，避免后续退回裸账号或任意账号直登。
  - `AdminDirectLoginPermissionContractTest` 增加前端合同，固定 `PartnerManagementPage` 和 `PartnerAccountModal` 的成功提示必须发生在 `await openPortalDirectLoginWindow(resp.data, config.moduleKey)` 和 `if (bridgeResult)` 之后。
  - 现有 `portal-direct-login-message.test.ts` 继续覆盖消息桥：收到 `READY` 只发送一次 token，不 resolve；必须等待同 source、同 origin、同 terminal、同 ticketId 的消费成功回执才 resolve。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=AdminDirectLoginPermissionContractTest,SellerServiceImplTest,BuyerServiceImplTest,PortalDirectLoginAuthContractTest,PortalDirectLoginTicketSqlContractTest,PortalLoginSessionConsistencyContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：`AdminDirectLoginPermissionContractTest` 1 test passed，`PortalDirectLoginAuthContractTest` 5 tests passed，`PortalDirectLoginTicketSqlContractTest` 3 tests passed，`PortalLoginSessionConsistencyContractTest` 2 tests passed，seller `SellerServiceImplTest` 63 tests passed，buyer `BuyerServiceImplTest` 64 tests passed，reactor BUILD SUCCESS。
  - 通过：`cd react-ui; npx jest --config jest.config.js tests/portal-direct-login-message.test.ts --runInBand`
  - 结果：1 suite / 5 tests passed。

## 追加加固：冻结业务权限运行态收敛

- 加固时间：2026-06-11。
- 发现问题：
  - 综合 seed 和历史拆分 seed 仍可能在远端库中留下 `seller:product:*` / `buyer:product:*` 等冻结业务授权。
  - 仅靠 portal 自助角色编辑过滤 self-management 菜单，不能阻止已有 role-menu 授权继续通过 `getInfo` / `getRouters` 或 `@PortalPreAuthorize` 暴露业务权限。
- 已加固：
  - `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 增加 19 个 self-management 权限精确集合。
  - `selectPortalPermissionInfo` 仍会 fail-closed 拒绝串端、admin 命名空间和通配权限，但对合法的冻结业务权限只做运行态剥离，不返回给 portal。
  - `selectPortalMenuTree` 会先校验端内菜单 ID、component 和 perms，再只把 self-management 菜单交给 `buildRouters`。
  - `20260610_portal_self_management_permission_seed.sql` 现在在事务内仅清理 active OWNER 角色上的非 self-management 授权，并以 `v_owner_role_count * 19` 和“owner role menu contains non self-management permission grants”完成断言固定最终状态。
  - `PortalSelfServiceSurfaceContractTest` 和 `SqlExecutionGuardContractTest` 已纳入上述运行态过滤与 seed cleanup 合同。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest,SqlExecutionGuardContractTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：ruoyi-system 82 tests passed；seller 权限服务 16 tests passed；buyer 权限服务 16 tests passed；reactor BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 29 suites / 244 tests passed；后端 reactor 和三端合同均 BUILD SUCCESS。

## 追加加固：`PortalPreAuthorize` 权限入口与密码修改 scope 清洗

- 加固时间：2026-06-11。
- 复核结论：
  - 历史 product portal 控制器仍保留 `seller:product:*` / `buyer:product:*` 注解；本轮不删除历史业务代码，但必须保证最小权限框架下不可被默认 OWNER 或历史 role-menu 授权放行。
  - `@PortalPreAuthorize` 当前通过 `PortalPermissionChecker -> selectPermissions(session)` 做权限判断，因此 `selectPermissions` 必须复用 `selectPortalPermissionInfo(session).getPermissions()` 的 self-management 运行态剥离结果。
  - `updatePortalPassword` 原先直接透传 payload；后端虽从 token 推导主体，但前端 service 层也应和账号、角色、部门写接口一致剥离 caller-controlled scope 字段。
- 已加固：
  - `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest` 新增 `selectPermissionsStripsFrozenBusinessPermissionsForPortalPreAuthorize`，固定 `selectPermissions` 会剥离 `product` / `order` 等冻结业务权限，只保留本轮 self-management 权限。
  - `PortalSelfServiceSurfaceContractTest` 固定 `selectPermissions(session)` 必须返回 `selectPortalPermissionInfo(session).getPermissions()`，防止未来绕开运行态剥离链路。
  - `react-ui/src/services/portal/session.ts` 的 `updatePortalPassword` 改为使用 `sanitizePortalPayload(data)`。
  - `portal-session-request.test.ts` 在密码修改请求中注入伪造 `buyerId` / `subjectId`，断言实际请求只发送密码字段。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest,SellerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：`PortalSelfServiceSurfaceContractTest` 1 test passed；seller 权限访问 9 tests passed；buyer 权限访问 9 tests passed；reactor BUILD SUCCESS。
  - 通过：`cd react-ui; npx jest --config jest.config.js tests/portal-session-request.test.ts --runInBand`
  - 结果：1 suite / 41 tests passed。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 29 suites / 244 tests passed；后端 reactor 和三端合同均 BUILD SUCCESS。

## 追加准备：live 只读验证脚本

- 准备时间：2026-06-11。
- 准备原因：
  - SQL 执行和 live 验证仍需用户确认目标环境后才能进行。
  - 为避免后续临时拼命令或漏检 `getInfo` / `getRouters` / 自助列表接口 / 跨端 token，先把只读 live 验证固定成脚本入口。
- 已完成：
  - 新增 `react-ui/scripts/verify-portal-self-management-live.mjs`。
  - 新增 npm 入口：`npm run verify:portal-self-management-live`。
  - 脚本默认不读取 `.env.local`，不执行 SQL，不做新增账号、角色、部门等写操作。
  - 脚本在提供 `SELLER_PORTAL_USERNAME` / `SELLER_PORTAL_PASSWORD` / `BUYER_PORTAL_USERNAME` / `BUYER_PORTAL_PASSWORD` 后，会验证：
    - seller/buyer 账号密码登录。
    - `/getInfo` 权限集合精确等于 19 个 self-management 权限。
    - `/getRouters` 和自助列表接口不暴露 product/order/inventory/logistics/finance/fulfillment/integration 等冻结业务权限。
    - seller token 不能访问 buyer `/getInfo`，buyer token 不能访问 seller `/getInfo`。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 已更新为先跑只读脚本，再人工验证写操作闭环。
- 已验证：
  - 通过：`cd react-ui; node scripts/verify-portal-self-management-live.mjs --help`
  - 结果：只打印用法，不访问网络，不要求真实账号。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 29 suites / 244 tests passed；后端 reactor 和三端合同均 BUILD SUCCESS。

## 追加加固：live 只读验证脚本静态合同

- 加固时间：2026-06-11。
- 加固原因：
  - `verify:portal-self-management-live` 后续会在 SQL 落库后用于真实 seller/buyer portal 只读验证，必须防止脚本漂移为读取 `.env.local`、读取运行数据源变量、执行 SQL 或发起 portal 写操作。
  - live 验证入口本身也需要纳入 `verify:three-terminal`，避免只存在手工脚本而没有门禁覆盖。
- 已加固：
  - 新增 `react-ui/tests/portal-self-management-live-contract.test.ts`。
  - `react-ui/tests/three-terminal.manifest.json` 已把该测试加入 `frontendTestPaths` 和 `criticalFrontendExplicitTestPaths`。
  - 合同固定以下边界：
    - npm 入口必须是 `node scripts/verify-portal-self-management-live.mjs`。
    - 脚本只依赖 `PORTAL_LIVE_BASE_URL`、`SELLER_PORTAL_USERNAME`、`SELLER_PORTAL_PASSWORD`、`BUYER_PORTAL_USERNAME`、`BUYER_PORTAL_PASSWORD`。
    - 脚本不得读取本机 secret 文件，不得依赖 `dotenv`，不得读取 `RUOYI_DB_*` / `RUOYI_REDIS_*`。
    - 除 seller/buyer 登录 POST 外，后续 live 检查只允许 GET，不允许 PUT / DELETE / PATCH，也不允许调用账号、角色、部门写接口。
    - seller/buyer `/getInfo` 权限集合必须精确等于本轮 19 个 self-management 权限。
    - `/getInfo`、`/getRouters` 和自助只读列表必须检查 product/order/inventory/logistics/finance/fulfillment/integration 等冻结业务权限不外露。
    - seller token 必须被 buyer `/getInfo` 拒绝，buyer token 必须被 seller `/getInfo` 拒绝。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.js tests/portal-self-management-live-contract.test.ts --runInBand`
  - 结果：1 suite / 5 tests passed。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 30 suites / 249 tests passed；后端 reactor 和三端合同均 BUILD SUCCESS。

## 追加准备：live 写闭环受控验证脚本

- 准备时间：2026-06-11。
- 准备原因：
  - 当前完成标准中“OWNER 可在 portal 内创建子账号、设置角色、维护部门、查看日志和在线会话”仍缺 live 证据。
  - 该验证会产生远端写操作，不能混入只读脚本，也不能在未确认目标环境时执行。
- 已完成：
  - 新增 `react-ui/scripts/verify-portal-self-management-live-write.mjs`。
  - 新增 npm 入口：`npm run verify:portal-self-management-live-write`。
  - 新增 `react-ui/tests/portal-self-management-live-write-contract.test.ts`，并纳入 `three-terminal.manifest.json` 的 `frontendTestPaths` 和 `criticalFrontendExplicitTestPaths`。
  - 写脚本默认 fail-closed，必须显式提供 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY` 才会执行。
  - 脚本不读取 `.env.local`，不读取 `RUOYI_DB_*` / `RUOYI_REDIS_*`，不执行 SQL，不访问商品、订单、库存、物流、财务、履约、外部系统业务 API。
  - 脚本只验证 seller/buyer portal 自助权限框架接口：账号、部门、角色、角色菜单模板、登录日志、操作日志、在线会话。
  - 脚本会清理可删除的测试角色和测试部门；由于当前 portal 自助面没有账号删除能力，写脚本会留下一个停用的 `verify_` STAFF 测试账号作为 live 写闭环证据。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 已更新为：seed 后先跑只读脚本，再用显式确认变量跑写脚本，最后人工验证管理端直登消费和 401 运行态场景。
- 已验证：
  - 通过：`cd react-ui; node scripts/verify-portal-self-management-live-write.mjs --help`
  - 结果：只打印用法，不访问网络，不要求真实账号，不执行写操作。
  - 通过：`cd react-ui; npx jest --config jest.config.js tests/portal-self-management-live-write-contract.test.ts --runInBand`
  - 结果：1 suite / 5 tests passed。
  - 通过：`cd react-ui; npx jest --config jest.config.js tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts --runInBand`
  - 结果：2 suites / 10 tests passed。
  - 通过：`cd react-ui; npm run tsc`
  - 结果：TypeScript 编译检查通过。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 254 tests passed；后端 reactor 和三端合同均 BUILD SUCCESS。

## 追加修复：self-management seed 首页授权补齐口径

- 修复时间：2026-06-11。
- 修复原因：
  - `20260610_portal_self_management_permission_seed.sql` 的最终断言要求 active OWNER 角色具备 19 个 self-management 权限，其中包含 `seller:portal:home` / `buyer:portal:home`。
  - 但原 OWNER 授权补齐 SQL 统一使用根级 `F` 菜单签名过滤，无法补回 portal 首页 `C` 菜单授权；如果远端库缺少首页 role-menu 绑定，seed 会在最终断言处失败。
- 已修复：
  - seller/buyer OWNER 授权补齐拆成两段：portal 首页按 `C` 页面菜单签名匹配；账号、部门、角色、日志、会话等自助权限继续按根级 `F` 权限模板匹配。
  - 新增 `SqlExecutionGuardContractTest.portalSelfManagementSeedMustGrantPortalHomeAsPageMenu`，固定首页 `C` 菜单授权不得混入根级 `F` 权限授权块。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,PortalSelfServiceSurfaceContractTest,TerminalSeedPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：85 tests passed，BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 254 tests passed；后端 reactor test-compile 和三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL。
  - 未改业务菜单、商品、订单、库存、物流、财务、履约或外部系统页面。

## 追加加固：portal 角色菜单 ID 全量 fail-closed

- 加固时间：2026-06-11。
- 加固原因：
  - 目标要求端内角色绑定菜单时，后端必须在写 `seller_role_menu` / `buyer_role_menu` 前全量校验提交的 `menuIds` 均存在于当前端菜单模板。
  - 原 service 路径会通过 `sanitizeIds(role.getMenuIds())` 静默丢弃 `null`、`0` 和重复 ID；虽然跨端/不存在菜单会失败，但无效 ID 不应被吞掉。
- 已加固：
  - `SellerPortalController` / `BuyerPortalController` 的自助角色入口新增 `menuIds` 非空值、正数和重复校验，明显非法输入在请求层 fail-closed。
  - `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 在写 role-menu 前新增 `normalizeRoleMenuIds(...)`，以原始提交数组校验 `null`、非正数和重复 ID，再执行当前端菜单存在性、ID 区间、权限前缀、component 等校验。
  - `PortalPermissionSupport.normalizeRole(...)` 不再静默清洗 `menuIds`，避免服务层丢失原始提交证据。
  - `PortalSelfServiceSurfaceContractTest` 固定 controller 和 service 均具备无效/重复菜单 ID fail-closed 片段。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：seller 19 tests passed，buyer 19 tests passed，system contract 1 test passed，BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 254 tests passed；后端 reactor test-compile 和三端合同均 BUILD SUCCESS。
- 静态扫描：
  - 未发现 portal controller/service 中 `sellerId` / `buyerId` 作为 `@RequestParam` 或 `@PathVariable` 的残留。
  - 未发现 seller/buyer portal 自助面引入商品、订单、库存、物流、财务、履约或外部系统业务权限串。

## 追加加固：前端角色与菜单 ID 写请求 fail-closed

- 加固时间：2026-06-11。
- 加固原因：
  - 后端已在 portal 角色菜单保存前拒绝 `null`、非正数和重复 `menuIds`。
  - 前端 `portal/session` service 原本只剥离 caller-controlled scope 字段，`roleIds` / `menuIds` 仍可能把无效或重复 ID 发到后端后再失败。
  - 本轮要求端内角色权限模板 fail-closed，前端 service 层也应在请求发出前收紧同一语义。
- 已加固：
  - `react-ui/src/services/portal/session.ts` 新增 `normalizePortalIdArray(...)`，统一要求 ID 数组只包含正整数，允许空数组，禁止重复。
  - `assignPortalAccountRoles(...)` 发送账号角色授权前会规范 `roleIds`。
  - `createPortalRole(...)` / `updatePortalRole(...)` 发送角色保存请求前会规范 `menuIds`，并继续剥离 `sellerId` / `buyerId` / `subjectId` / `accountId` 等 caller-controlled scope 字段。
  - 数字字符串会规范成数字；0、负数、空值、非数字和重复 ID 会在请求发出前失败。
- 已验证：
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 259 tests passed；后端 reactor test-compile 和三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL。
  - 未做浏览器、截图、DOM 或 UI 细调验证。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 追加加固：后端账号角色 ID 写绑定 fail-closed

- 加固时间：2026-06-11。
- 加固原因：
  - seller/buyer 端账号角色绑定路径仍使用 `PortalPermissionSupport.sanitizeIds(roleIds)`，会静默丢弃 `null`、`0` 和重复角色 ID。
  - 该行为会让非法提交被“修正后继续执行”，不符合本轮端内权限绑定 fail-closed 要求。
- 已加固：
  - `SellerPortalPermissionServiceImpl.assignAccountRoles(...)` 改为使用 `normalizeAccountRoleIds(...)`。
  - `BuyerPortalPermissionServiceImpl.assignAccountRoles(...)` 改为使用 `normalizeAccountRoleIds(...)`。
  - 空数组仍允许用于清空账号角色；但提交数组中出现 `null`、非正数或重复角色 ID 时，会在 `count*RolesByIds`、删除旧绑定和批量插入前失败。
  - `PortalSelfServiceSurfaceContractTest` 固定账号角色绑定必须使用 `normalizeAccountRoleIds(roleIds)`，并固定正整数和重复 ID 校验片段。
  - seller/buyer 权限服务测试已从“静默清洗非法 ID”改为“非法/重复 ID 不得触发旧绑定删除或新绑定插入”。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：`PortalSelfServiceSurfaceContractTest` 1 test passed；seller 权限服务 21 tests passed；buyer 权限服务 21 tests passed；reactor BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 259 tests passed；后端 reactor test-compile 和三端合同均 BUILD SUCCESS；seller 115 tests passed；buyer 117 tests passed。
- 影响边界：
  - 未执行 SQL。
  - 未做浏览器、截图、DOM 或 UI 细调验证。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 追加加固：后端角色删除 ID 写路径 fail-closed

- 加固时间：2026-06-11。
- 加固原因：
  - `deleteRoleByIds(...)` 仍使用 `PortalPermissionSupport.sanitizeIds(roleIds)`，会静默丢弃 `null`、`0` 和重复角色 ID。
  - 角色删除属于端内权限管理写路径，非法 ID 不应被清洗后继续执行，否则会弱化 fail-closed 语义。
- 已加固：
  - `SellerPortalPermissionServiceImpl.deleteRoleByIds(...)` 改为使用 `normalizeRoleIds(...)`。
  - `BuyerPortalPermissionServiceImpl.deleteRoleByIds(...)` 改为使用 `normalizeRoleIds(...)`。
  - 原账号角色绑定也复用同一个 `normalizeRoleIds(...)`，避免账号授权和角色删除两条写路径规则漂移。
  - `PortalSelfServiceSurfaceContractTest` 固定 `roleIds` 写路径不得继续调用 `PortalPermissionSupport.sanitizeIds(roleIds)`。
  - seller/buyer 权限服务测试新增角色删除非法 ID、重复 ID 在角色查询、账号绑定检查、role-menu 删除和角色删除前失败的覆盖。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：`PortalSelfServiceSurfaceContractTest` 1 test passed；seller 权限服务 23 tests passed；buyer 权限服务 23 tests passed；reactor BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 259 tests passed；后端 reactor test-compile 和三端合同均 BUILD SUCCESS；seller 117 tests passed；buyer 119 tests passed。
- 影响边界：
  - 未执行 SQL。
  - 未做浏览器、截图、DOM 或 UI 细调验证。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 追加加固：前端 portal 路径 ID fail-closed

- 加固时间：2026-06-11。
- 加固原因：
  - 前端 `portal/session` service 已对 `roleIds` / `menuIds` 数组做写请求 fail-closed，但账号、部门、角色的 path ID 仍直接拼进 URL。
  - `getPortalRoleMenus(0)` 这类调用会因为旧逻辑的 truthy 判断退回 `/roles/menus` 模板路径，语义不够 fail-closed。
- 已加固：
  - `react-ui/src/services/portal/session.ts` 新增 `normalizePortalIdentifier(...)`，统一要求账号、部门、角色 path ID 必须是正整数。
  - `updatePortalAccount`、`getPortalAccountRoles`、`assignPortalAccountRoles`、`getPortalDept`、`updatePortalDept`、`deletePortalDept`、`getPortalRole`、`getPortalRoleMenus`、`updatePortalRole`、`deletePortalRole` 均在构造 URL 前先规范 path ID。
  - `getPortalRoleMenus` 仅在 `roleIdentifier == null` 时访问模板路径；`0`、负数、小数、`NaN` 或非数字字符串均在请求发出前失败。
  - `portal-session-request.test.ts` 增加数字字符串规范、无效单 ID 阻断和“0 不回退模板路径”合同。
- 已验证：
  - 误跑：`cd react-ui; npm test -- portal-session-request.test.ts` 被 `verify-three-terminal` 拒绝，原因是本仓库三端总门禁不支持传单个测试文件名；该命令未执行测试。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 270 tests passed；后端 reactor test-compile 和三端合同均 BUILD SUCCESS；seller 117 tests passed；buyer 119 tests passed。
- 影响边界：
  - 未执行 SQL。
  - 未做浏览器、截图、DOM 或 UI 细调验证。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 追加加固：portal home 业务入口冻结合同

- 加固时间：2026-06-11。
- 加固原因：
  - `react-ui/src/pages/Portal/Home/` 目录中仍保留历史商品相关组件文件；虽然当前 `PortalHome` 未引入它们，但缺少合同固定“本轮 portal 入口只开放最小自助权限闭环”。
  - 本轮完成口径明确冻结商品、订单、库存、物流、财务、履约、外部系统等业务菜单和业务页面，后续不能通过 portal home 入口误挂回历史业务组件。
- 已加固：
  - `portal-self-management-contract.test.ts` 新增 portal home 冻结业务入口合同。
  - 合同固定 `Portal/Home/index.tsx` 与 `PortalSelfManagement.tsx` 不得引入或引用历史商品组件、商品路径、业务权限片段。
  - 合同固定 `/seller/portal` 与 `/buyer/portal` 当前入口仍为 `./Portal/Home`，并不得新增 `/seller/portal/product`、`/buyer/portal/product`、`/seller/portal/order`、`/buyer/portal/order` 等业务子路由。
- 已验证：
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 271 tests passed；后端 reactor test-compile 和三端合同均 BUILD SUCCESS；seller 117 tests passed；buyer 119 tests passed。
- 影响边界：
  - 未删除历史商品 portal 组件文件，只固定本轮当前入口不可达。
  - 未执行 SQL。
  - 未做浏览器、截图、DOM 或 UI 细调验证。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 追加修复：self-management seed 清理范围收窄到 OWNER

- 修复时间：2026-06-11。
- 修复原因：
  - `20260610_portal_self_management_permission_seed.sql` 的目标是给当前 active seller/buyer OWNER 角色补齐最小 self-management 权限，并冻结 OWNER 默认业务菜单。
  - 原清理段会删除 seller/buyer 所有角色上的非 self-management 授权，作用域超过 OWNER，属于高影响 DML 范围过宽。
- 已修复：
  - seller/buyer 两段 `delete rm from *_role_menu` 均新增 `join *_role r`，并限定 `r.del_flag = '0'`、`r.status = '0'`、`r.role_key = 'owner'`。
  - seed 完成断言同步收窄为只检查 active OWNER 角色是否仍残留非 self-management 授权。
  - `SqlExecutionGuardContractTest` 固定上述 OWNER 范围，防止后续回退为全角色清理。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：83 tests passed；BUILD SUCCESS。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-contract.test.ts tests/portal-session-request.test.ts --runInBand`
  - 结果：2 suites / 62 tests passed。
  - 通过：`cd react-ui; npm run verify:three-terminal`
  - 结果：`three-terminal verification passed.`；前端 31 suites / 271 tests passed；后端 reactor test-compile 和三端合同均 BUILD SUCCESS；seller 117 tests passed；buyer 119 tests passed。
- 影响边界：
  - 未执行 SQL。
  - 未清理非 OWNER 角色授权。
  - 未做浏览器、截图、DOM 或 UI 细调验证。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 追加加固：端内通配权限运行态 fail-closed

- 加固时间：2026-06-11。
- 加固原因：
  - 本轮规则要求端内菜单和权限禁止 `*` 通配。
  - `PortalPermissionSupport` 已在菜单写入和菜单树读取时拒绝任意 `*`，但 `selectPortalPermissionInfo` 直接读取账号权限聚合字符串时仅拒绝 `*:*:*`，对 `seller:*` / `buyer:*` 这类污染权限不够严格。
- 已加固：
  - `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 的运行态权限字符串校验改为包含 `*` 即 fail-closed。
  - seller/buyer 权限访问测试新增 `seller:*`、`seller:role:*`、`buyer:*`、`buyer:role:*` 污染权限用例。
  - `PortalSelfServiceSurfaceContractTest` 固定权限服务必须包含 `permission.contains("*")` 检查。
  - `20260610_portal_self_management_permission_seed.sql` 的端内菜单预检从 `perms = '*'` 收紧为 `perms like '%*%'`。
  - SQL runbook 明确当前 seed 校验任意 `*` 通配权限。
- 已验证：
  - 2026-06-11 02:10 重跑 `npm run verify:three-terminal` 通过；前端 31 个 Jest suite / 271 个测试通过，后端 reactor `test-compile` 与三端合同测试通过。
- 影响边界：
  - 未执行 SQL。
  - 未清理历史旧 seed 文件，只加固本轮 self-management 执行入口。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 未验证

- 未执行新增 SQL，因此远端库是否已具备本轮新增最小自助权限点仍未验证。
- 未做浏览器、截图、DOM 或 UI 细调验证。
- 未做 live portal 账号密码登录、管理端免密直登消费、真实 `/getInfo` / `/getRouters`、OWNER 写闭环、401 跳转和会话隔离验证。
- SQL 执行与 live 验证固定入口已补充：`docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md`。

## 完成标准矩阵

| 完成标准 | 当前判断 | 已有证据 | 仍需证据 |
| --- | --- | --- | --- |
| 管理端可维护卖家 / 买家主体，并默认直登对应 OWNER | 代码级已覆盖，live 未验证 | 管理端 seller/buyer service、direct-login bridge、direct-login 后端合同和 `npm run verify:three-terminal` 通过 | 远端 seed 执行后，用管理端真实账号对 seller/buyer 主体各发起一次 OWNER 直登，并确认 portal 消费成功回传 |
| 卖家 / 买家可账号密码登录各自 portal | 代码级已覆盖，live 未验证 | `portal-session-request.test.ts`、`terminal-session-token.test.ts`、后端 portal 登录和 token/session 合同均进入总门禁 | 执行 seed 后用远端 OWNER 账号分别登录 `/seller/login`、`/buyer/login` |
| OWNER 可在 portal 内创建子账号、设置角色、维护部门、查看登录日志、操作日志和在线会话 | 代码级已覆盖，数据授权未落库 | `PortalSelfManagement`、portal self-management service、`PortalSelfServiceSurfaceContractTest`、`portal-self-management-contract.test.ts` 和总门禁通过 | 执行 self-management seed 后，用 OWNER 做一次新增子账号、分配角色、部门维护、日志和会话查看的 live 验证 |
| `getInfo`、`getRouters`、401 跳转、token/session、权限校验、日志审计按 seller / buyer 隔离 | 代码级和静态/单元门禁已覆盖，live 未验证 | `check-portal-token-isolation.mjs`、`portal-unauthorized-redirect.test.ts`、`portal-session-request.test.ts`、`PortalLoginSessionConsistencyContractTest`、`TokenServiceTerminalIsolationTest`、`PortalLogAspectContractTest` 纳入总门禁 | 在真实 token 下分别调用 seller/buyer `/getInfo`、`/getRouters`，并触发一次 401、日志和会话场景 |
| 端内角色只能从平台预置 `seller_menu` / `buyer_menu` 权限模板分配权限 | 代码级已覆盖，远端 seed 未执行 | controller 已过滤 self-management 菜单树和 `checkedKeys`，提交不存在/跨端/非 self-management menuId fail-closed；SQL guard 覆盖菜单 ID 区间、权限前缀、空权限、通配权限和 admin 命名空间 | 远端执行 seed 后抽查 `seller_menu` / `buyer_menu` 和 OWNER role-menu 授权结果 |
| 商品、订单、库存、物流、财务、履约、外部系统等业务菜单冻结 | 代码级已收敛，live 未验证 | `portal-self-management-contract.test.ts` 固定自助页只调用账号、角色、部门、日志接口，并固定 portal home 当前入口不挂接历史商品/业务组件或业务子路由；角色模板过滤 self-management 权限；`SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 运行态剥离非 self-management 权限；新 seed 仅清理 active OWNER 角色上的非 self-management role-menu 授权 | 执行 seed 后抽查 OWNER role-menu，并用真实 OWNER token 确认 portal 默认菜单只出现本轮最小权限框架入口 |
| 最小必要测试通过 | 已完成 | `npm run verify:three-terminal` 通过，前端 32 suites / 277 tests，后端 reactor test-compile 和三端合同均 BUILD SUCCESS | 远端 SQL/live 验证后如有代码变更，需重新跑总门禁 |

## 收口验证

- 2026-06-11 02:10 已重跑 `npm run verify:three-terminal`：portal token guard、partner management guard、seller/buyer portal product guard、product upstream mirrors guard、React typecheck、前端 Jest 31 suites / 271 tests、后端 reactor `test-compile` 和三端合同测试全部通过。
- 2026-06-11 受控 SQL runner 合同纳入总门禁后已重跑 `npm run verify:three-terminal`：前端 Jest 32 suites / 276 tests、后端 reactor `test-compile` 和三端合同测试全部通过。
- 2026-06-11 管理端 direct-login 成功提示合同加固后已重跑 `npm run verify:three-terminal`：前端 Jest 32 suites / 277 tests、后端 reactor `test-compile` 和三端合同测试全部通过。
- 已对本轮已跟踪改动执行 `git diff --check -- ...`，结果通过；仅输出 LF/CRLF 工作区换行提示。
- 已对本轮新增未跟踪 SQL、Markdown 和前端合同测试文件执行行尾空白检查，结果无输出。
- 已在仓库根目录执行 `codegraph sync .`，结果通过：`Synced 9 changed files`，`Added: 2, Modified: 7 - 520 nodes`；`.codegraph/` 仍按本机索引目录处理，不作为本轮业务产物。

## 续跑预检：SQL/live 执行前环境记录

- 预检时间：2026-06-11 02:14。
- 预检性质：只读检查；未执行 SQL，未读取或输出 `.env.local` 明文值，未写 MySQL 或 Redis。
- 已确认：
  - 后端激活配置为 `spring.profiles.active: druid`。
  - MySQL 实际连接来自 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 注入，不能默认本地 Docker 或 localhost。
  - Redis 实际连接来自 `RUOYI_REDIS_*` 注入，live 验证会使用当前后端运行配置。
  - `.env.local` 存在，且 DB / Redis / token secret / `URILI_SECRET_ENCRYPTION_KEY` 运行变量存在；仅记录存在性和是否像本机地址，不记录明文值。
  - 当前 `docker ps` 未发现 URILI MySQL / Redis 容器正在运行；存在的 `open-design`、`urili-postgres`、`lianghua-*` 容器不应作为本轮目标环境。
  - `mysql` CLI 当前不在 PATH；后续如执行 SQL，需要使用 JDBC/脚本方式或先明确可用 MySQL 客户端，并保证同一连接内先设置确认变量再执行 seed。
- 已写入 runbook：`docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 的“执行前只读预检记录”。

## 续跑静态审计：portal 主体来源与 live 脚本入口

- seller/buyer portal controller 自助接口均通过 `PortalSessionContext.requireSession("seller" / "buyer")` 获取当前端 session，并以 `session.getSubjectId()` 下传账号、角色、部门、日志和会话 service；未发现 controller 直接信任前端传入的 `sellerId` / `buyerId` / `subjectId` 作为自助接口主体。
- `react-ui/src/services/portal/session.ts` 已对 portal 请求剥离 `sellerId` / `buyerId` / `subjectId` / `accountId` / `sellerAccountId` / `buyerAccountId` / `terminal` 等 caller-controlled scope 字段。
- `verify:portal-self-management-live` 和 `verify:portal-self-management-live-write` 均已在 `react-ui/package.json` 暴露，并由 `react-ui/tests/three-terminal.manifest.json` 的 frontend/critical frontend 路径覆盖。
- live 只读脚本不读取 `.env.local`、不读取 `RUOYI_DB_*` / `RUOYI_REDIS_*`、不执行 SQL、不做 portal 写操作；写脚本需要显式 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY`，且只触达账号、角色、部门、日志和会话自助接口。
- 2026-06-11 02:16 复查记录文件行尾空白无输出；重跑 `codegraph sync .` 通过：`Synced 5 changed files`，`Modified: 5 - 225 nodes`。

## 续跑只读预检：目标库权限模板现状

- 预检时间：2026-06-11 02:20。
- 预检性质：只读 `SELECT`；未执行 DML、DDL 或 seed；未写 MySQL 或 Redis；未在记录中输出连接串、账号或密码明文。
- 预检工具：JShell + 本机 Maven 缓存 MySQL JDBC 驱动。
- 目标库只读结果：
  - 当前库：`fenxiao`。
  - seller portal 首页 `C` 菜单已存在 1 个；buyer portal 首页 `C` 菜单已存在 1 个。
  - seller/buyer active OWNER 角色各 35 个。
  - seller/buyer self-management 权限模板当前均为 7 / 19 个。
  - seller/buyer OWNER self-management 授权当前均为 245 条，即 `35 * 7`。
  - seller/buyer OWNER 非 self-management 授权当前均为 0 条。
  - seller/buyer 端无效菜单权限当前均为 0 条。
- 缺口判断：
  - 不需要先执行 `20260610_terminal_portal_home_menu_seed.sql`。
  - 仍需要执行 `20260610_portal_self_management_permission_seed.sql`，用于补齐每端缺失的 12 个账号/部门/角色自助按钮和查询权限，并把 active OWNER 授权从 `35 * 7` 补齐到 `35 * 19`。
  - OWNER-only 非 self-management 清理预计为 no-op，但仍保留 seed 内写前/写后断言。
- 详细缺失权限清单已写入 `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md`。

## 续跑加固：受控 SQL runner

- 加固时间：2026-06-11。
- 加固原因：
  - 当前目标库仍缺少每端 12 个 self-management 账号、部门、角色权限模板，后续需要执行 `20260610_portal_self_management_permission_seed.sql`。
  - `mysql` CLI 当前不在 PATH；手工拼接 JShell/JDBC 命令容易泄露环境变量或遗漏同连接确认变量。
  - seed 属于远端 MySQL DML，必须保留“默认只读、显式确认、同连接设置确认变量、执行后只读复核”的固定入口。
- 已完成：
  - 新增 `scripts/portal-self-management-sql-runner.mjs`。
  - 默认行为为只读预检：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 写入模式必须显式提供 `PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED` 并传入 `--apply`；未设置确认变量时会在连接数据库前 fail-closed。
  - runner 读取 `.env.local` 中当前 `RUOYI_DB_*` 目标，但不输出连接串、账号或密码明文。
  - runner 通过本机 Maven 缓存 MySQL JDBC 驱动执行 Java 单文件；`--precheck` 使用 read-only 连接，只输出当前库名、菜单模板和 OWNER 授权计数。
  - `--apply` 会在同一 JDBC 连接内先设置 `@confirm_portal_self_management_permission_seed`，再解析并执行 seed；已支持 MySQL `delimiter` 块。
  - apply 执行异常时显式 `connection.rollback()`，避免只依赖连接关闭回滚。
  - 新增 `react-ui/tests/portal-self-management-sql-runner-contract.test.ts`，并纳入 `react-ui/tests/three-terminal.manifest.json` 的 frontend/critical frontend 门禁。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 已更新为优先使用该 runner 做预检和受控执行。
- 已验证：
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功，目标库为 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19，OWNER 授权仍为 `35 * 7`，未执行 DML。
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --apply` 在未设置确认变量时 fail-closed。
  - 结果：退出前提示必须设置 `PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED`，未连接数据库。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：1 suite / 5 tests passed。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 32 suites / 276 tests passed；后端 reactor `test-compile` 与三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL seed。
  - 未做 live portal 写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：live-write 确认门槛前置合同

- 加固时间：2026-06-12 01:58 +08:00。
- 当前已执行事项：
  - 已按用户确认对目标库 `fenxiao` 执行本阶段 self-management 权限 seed。
  - seed 影响范围限定在 `seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`。
  - 执行后 postcheck 确认 seller/buyer self-management 权限模板均为 `19 / 19`，root button 模板均为 `18 / 18`，active OWNER 授权均为 `665 = 35 * 19`，非 self-management 授权均为 `0`。
- 本次加固：
  - `portal-self-management-live-write-contract.test.ts` 固定 `verify-portal-self-management-live-write.mjs` 的 `main()` 必须先执行 `requireLiveEnv()`，再进入 `verifyTerminalWrites(terminal)`。
  - 确认门槛前不得出现 `await`、`requestJson(...)`、`login(...)`、`authorizedRequest(...)` 或 `cleanupTerminalWrites(...)`，避免缺少确认变量时仍发起真实登录、写入或清理动作。
  - 写验证确认变量继续固定为 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY`。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-write-contract.test.ts --runInBand`，1 suite / 7 tests passed。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 通过：`cd react-ui; npm run verify:three-terminal`，前端 33 suites / 300 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - 通过：`git diff --check -- react-ui\tests\portal-self-management-live-write-contract.test.ts react-ui\scripts\verify-portal-self-management-live-write.mjs`，仅 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`，结果为 `Synced 1 changed files`，`Modified: 1 - 5 nodes`。
- 尚未验证：
  - 真实 seller/buyer OWNER 账号密码登录。
  - 管理端 direct-login 票据签发、目标 portal 一次性消费和成功回传。
  - 真实 `/getInfo` / `/getRouters`、401 跳转、日志、会话隔离。
  - OWNER 在真实 portal 内创建子账号、分配角色、维护部门并查看日志/会话的 live 写闭环。
- 阻塞条件：live 登录、direct-login 和写闭环会写登录日志、操作日志、direct-login 票据、portal session，并会留下停用 STAFF 测试账号；继续前需要用户明确确认 live 写入范围和 seller/buyer OWNER 凭据。

## 续跑加固：管理端直登成功提示等待合同

- 加固时间：2026-06-11。
- 加固原因：
  - 本轮规则要求管理端免密代入打开 seller/buyer portal 后，成功提示必须等待目标 portal 完成 `/direct-login` 消费并通过 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 回传成功。
  - 现有消息桥测试已覆盖 `READY` 只发送 token、不 resolve；但 partner 管理页还需要固定主体直登和账号直登两个入口都不能只按后端生成票据成功就提示完成。
- 已加固：
  - `react-ui/tests/partner-management-contract.test.ts` 新增合同：`PartnerManagementPage` 和 `PartnerAccountModal` 都必须 `await openPortalDirectLoginWindow(resp.data, config.moduleKey)`。
  - 合同固定 `message.success(...免密登录已确认...)` 必须位于 bridge 结果之后，并禁止退回“免密登录链接已生成”这类仅票据生成成功文案。
  - `react-ui/tests/portal-direct-login-message.test.ts` 继续覆盖同 source、同 origin、同 terminal、同 ticketId 的消费成功回执才 resolve。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/partner-management-contract.test.ts --runInBand`。
  - 结果：1 suite / 8 tests passed。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 32 suites / 277 tests passed；后端 reactor `test-compile` 与三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未改运行代码，仅补前端合同。
  - 未执行 SQL seed。
  - 未做 live portal 写验证。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：portal 自助主体来源负向合同

- 加固时间：2026-06-11。
- 加固原因：
  - 本轮规则要求所有 seller/buyer portal 自助接口必须从当前 token/session 推导 `sellerId` / `buyerId`，禁止相信前端传入主体 ID。
  - 现有合同已固定主要自助接口使用 `session.getSubjectId()` 下传 service；本次补充 controller 层负向合同，防止后续新增接口重新引入 query/path 主体参数。
- 已加固：
  - `PortalSelfServiceSurfaceContractTest` 新增禁止项：portal controller 不得出现 `@RequestParam("subjectId")`、`@PathVariable("subjectId")`、`@RequestParam("accountId")`、`@RequestParam("*AccountId")`，并继续禁止 `@RequestParam("*Id")` / `@PathVariable("*Id")` 作为端主体来源。
  - 该合同和已有正向断言共同固定：账号、角色、部门、日志、会话自助接口均以 `PortalSessionContext.requireSession(...)` 和 `session.getSubjectId()` 为主体边界。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 结果：1 test passed；BUILD SUCCESS。
  - 通过：`cd RuoYi-Vue; mvn -pl finance -am "-DskipTests" test-compile`。
  - 结果：旁路 finance 当前态编译通过；此前一次总门禁被 finance 未跟踪脏改落盘时序打断，当前已恢复。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 32 suites / 277 tests passed；后端 reactor `test-compile` 与三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未改运行代码，仅补后端合同测试。
  - 未执行 SQL seed。
  - 未做 live portal 写验证。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：SQL runner 执行后强断言

- 加固时间：2026-06-11。
- 加固原因：
  - `20260610_portal_self_management_permission_seed.sql` 本身已有 SQL 层完成断言，但受控 runner 在 apply 后原先只打印 postcheck 计数。
  - 为避免远端 seed 执行异常、部分执行或目标库状态漂移时被误判为完成，runner 需要在 apply 后把最终状态也作为硬门禁。
- 已加固：
  - `scripts/portal-self-management-sql-runner.mjs` 的 Java runner 在 `postcheck` 阶段汇总计数并强断言：
    - seller/buyer portal 首页菜单各 1 个。
    - seller/buyer self-management 权限模板各 19 个。
    - seller/buyer active OWNER 授权必须等于 `ownerRoleCount * 19`。
    - seller/buyer OWNER 非 self-management 授权必须为 0。
    - seller/buyer 无效端内菜单权限必须为 0。
  - 若 postcheck 缺少计数或计数不匹配，runner 直接失败，不再只输出结果。
  - `react-ui/tests/portal-self-management-sql-runner-contract.test.ts` 增加合同，固定 apply 后强断言边界。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：1 suite / 6 tests passed。
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库仍为 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；未执行 DML。
- 影响边界：
  - 未执行 SQL seed。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：live 验证入口路径与直登链路

- 加固时间：2026-06-11。
- 加固原因：
  - 后端当前 `server.servlet.context-path` 为 `/`，`/api` 只由 `react-ui/config/proxy.ts` 在 React dev server 层重写。
  - 原 live 脚本默认 `PORTAL_LIVE_BASE_URL=http://127.0.0.1:8080`，但请求路径带 `/api/...`，后续直连后端验证会跑偏。
  - 完成标准还要求管理端可默认直登 seller/buyer OWNER；此前已有代码级合同，但缺少受控 live 直登验证入口。
- 已加固：
  - `react-ui/scripts/verify-portal-self-management-live.mjs` 和 `react-ui/scripts/verify-portal-self-management-live-write.mjs` 默认直连后端真实路径，不再默认拼 `/api`。
  - 两个脚本新增可选 `PORTAL_LIVE_API_PREFIX=/api`，仅在目标为 React dev proxy 时启用。
  - `verify-portal-self-management-live.mjs` 增加匿名 `/getInfo` 拒绝检查，避免只验证跨端 token 而漏掉无 token 鉴权入口。
  - `verify-portal-self-management-live.mjs` 增加 `/getRouters` 路由硬断言：
    - seller/buyer portal 首页必须分别使用 `seller:portal:home` / `buyer:portal:home`。
    - 首页 path 必须是 `/{terminal}/portal`。
    - 首页 component 必须是 `Seller/Portal/index` / `Buyer/Portal/index`。
    - 返回路由不得包含空权限外的通配权限、跨端权限、`*:admin:*` 权限或非 self-management 权限。
  - 新增 `react-ui/scripts/verify-portal-direct-login-live.mjs`：
    - 需要外部显式提供 `ADMIN_AUTH_TOKEN`、`SELLER_DIRECT_LOGIN_SUBJECT_ID`、`BUYER_DIRECT_LOGIN_SUBJECT_ID`。
    - 需要显式 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`。
    - 每端只生成并消费一次管理端到 OWNER 的 direct-login ticket，随后复用同一 ticket 必须失败，再用 portal token 调用 `/getInfo` 校验 19 个 self-management 权限，并 best-effort logout。
    - 不读取 `.env.local`，不执行 SQL，不触达商品、订单、库存、物流、财务、履约或外部系统业务 API。
  - `react-ui/package.json` 新增 `verify:portal-direct-login-live`。
  - `react-ui/tests/portal-direct-login-live-contract.test.ts` 新增合同，并纳入 `react-ui/tests/three-terminal.manifest.json` 的 frontend/critical frontend 门禁。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 23 tests passed。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live.mjs --help`。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live-write.mjs --help`。
  - 通过：`cd react-ui; node scripts\verify-portal-direct-login-live.mjs --help`。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 286 tests passed；后端 reactor `test-compile` 与三端合同均 BUILD SUCCESS。
  - 通过：`git diff --check -- <本轮三端 portal 相关文件>`。
  - 结果：无 whitespace error；仅 `react-ui/package.json` 和 `react-ui/tests/three-terminal.manifest.json` 的 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`。
  - 结果：`Synced 6 changed files`；`Added: 2, Modified: 4 - 87 nodes in 557ms`。
  - 记录补写后复跑：`codegraph sync .`。
  - 结果：`Already up to date`。
- 影响边界：
  - 未执行 SQL seed。
  - 未做 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 最终收口复验记录

- 复验时间：2026-06-11 03:11 +08:00。
- 已复验：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 23 tests passed。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 286 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
  - 通过：`git diff --check -- <本轮三端 live 校验与进度记录文件>`。
  - 结果：无 whitespace error；仅 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`。
  - 结果：`Already up to date`。
- 影响边界：
  - 本次复验没有执行 SQL seed。
  - 本次复验没有执行 live portal 登录、直登或写验证。
  - 本次复验没有写 MySQL、Redis 或业务数据。

## 续跑加固：direct-login live 跨端与路由门禁

- 加固时间：2026-06-11。
- 加固原因：
  - 本阶段要求管理端 direct-login 只能被目标端消费，不能发生 seller/buyer 票据串端。
  - 直登消费后的 portal token 不应只校验 `/getInfo`，还应校验 `/getRouters` 只返回当前端 self-management 路由。
- 已加固：
  - `react-ui/scripts/verify-portal-direct-login-live.mjs` 在创建 direct-login ticket 后，会先用另一端 `/direct-login` 尝试消费并要求失败；随后再由目标端消费。
  - 管理端 direct-login ticket 响应必须保持短时，且不得泄漏 `accountId`、`username`、`tokenHash`；`loginUrl` 也不得带 `directLoginToken` 或 `token` 查询参数。
  - 同一 ticket 仍必须不可复用。
  - 直登消费后的 token 会同时校验 `/getInfo` 和 `/getRouters`：
    - 权限集合必须精确等于 19 个 self-management 权限。
    - portal 首页 path/component 必须端内隔离。
    - 路由不得包含通配权限、跨端权限、`*:admin:*` 权限或非 self-management 权限。
    - `getInfo` / `getRouters` 不得暴露 product/order/inventory/logistics/finance/fulfillment/integration 等冻结业务面。
  - 直登消费后的 token 还会尝试访问另一端 `/getInfo` 和 `/getRouters`，任何 200 成功都视为 token/session 串端失败。
  - `react-ui/tests/portal-direct-login-live-contract.test.ts` 同步固定上述行为，并纳入既有三端 manifest。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 同步更新 direct-login live 验证范围。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts --runInBand`。
  - 结果：1 suite / 8 tests passed。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 26 tests passed。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`cd react-ui; node scripts\verify-portal-direct-login-live.mjs --help`。
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 289 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
  - 通过：`git diff --check -- <本轮 direct-login live 校验、合同、runbook、进度记录文件>`。
  - 结果：无 whitespace error；仅 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`。
  - 结果：`Synced 2 changed files`；`Modified: 2 - 36 nodes in 645ms`。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live direct-login 写验证。
  - 未写 MySQL、Redis 或业务数据。

## 续跑加固：密码登录 live 跨端路由门禁

- 加固时间：2026-06-11。
- 加固原因：
  - 密码登录 live 校验原先已覆盖跨端 `/getInfo` 拒绝，但还需要把 `/getRouters` 纳入同一串端门禁，避免 portal token 在另一端拿到路由树。
  - 本阶段完成口径要求 seller/buyer 的 `getInfo`、`getRouters`、token/session 和权限校验均按端隔离。
- 已加固：
  - `react-ui/scripts/verify-portal-self-management-live.mjs` 的跨端 token 拒绝检查已同时覆盖另一端 `/getInfo` 和 `/getRouters`。
  - `react-ui/tests/portal-self-management-live-contract.test.ts` 同步固定密码登录 live 脚本的跨端 `/getInfo` / `/getRouters` 拒绝合同。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 同步更新密码登录 live 验证范围。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts --runInBand`。
  - 结果：1 suite / 7 tests passed。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 26 tests passed。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live.mjs --help`。
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 289 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
  - 通过：`git diff --check -- <本轮密码登录 live 校验、合同、runbook、进度记录文件>`。
  - 结果：无 whitespace error；仅 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`。
  - 结果：`Synced 2 changed files`；`Modified: 2 - 30 nodes in 583ms`。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：密码登录 live 匿名路由与冻结 path 门禁

- 加固时间：2026-06-11。
- 加固原因：
  - 密码登录 live verifier 已检查冻结业务权限串，但如果后续错误返回带空权限或异常权限的冻结业务 path，仅靠 `seller:product:` / `buyer:product:` 字符串不足以发现。
  - 匿名入口此前只检查 `/getInfo` 拒绝；本轮把匿名 `/getRouters` 也纳入同一门禁，避免未登录状态下拿到 portal 路由树。
- 已加固：
  - `react-ui/scripts/verify-portal-self-management-live.mjs` 的冻结业务面检查从权限串扩大到权限串 + 路由 path，覆盖 product/order/inventory/logistics/finance/fulfillment/integration。
  - 匿名 portal 请求拒绝检查从 `/getInfo` 扩大为 `/getInfo` 和 `/getRouters`。
  - `react-ui/tests/portal-self-management-live-contract.test.ts` 同步固定上述脚本行为。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 同步更新密码登录 live 验证范围：匿名 `getInfo` / `getRouters` 拒绝，冻结业务权限和冻结业务 path 不可见。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts --runInBand`。
  - 结果：1 suite / 7 tests passed。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 26 tests passed。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live.mjs --help`。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 289 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 最终当前状态：seed 已完成，live 脚本确认门槛已运行验证

- 收口时间：2026-06-12 02:05 +08:00。
- 当前口径：
  - 本文件中较早段落保留的 `未执行 SQL seed`、`7 / 19`、`245 = 35 * 7`、`权限模板未落库`、`下一步执行 seed` 等内容均为历史快照。
  - 当前状态以 `2026-06-12 01:36` seed 执行记录、`2026-06-12 02:02` 只读 precheck 和本节为准。
- 当前数据状态：
  - 目标库：`fenxiao`。
  - seller/buyer self-management 权限模板：均为 `19 / 19`。
  - seller/buyer root button 权限模板：均为 `18 / 18`。
  - seller/buyer OWNER self-management 授权：均为 `665 = 35 * 19`。
  - seller/buyer OWNER 非 self-management 授权：均为 0。
  - 无无效权限、菜单 ID 区间、页面 component 或重复 perms 问题。
- live 脚本缺确认变量 fail-closed 验证：
  - `verify-portal-self-management-live.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY`。
  - `verify-portal-self-management-live-write.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY`。
  - `verify-portal-direct-login-live.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`。
  - 上述验证未提供账号、token、主体 ID 或确认变量，未执行真实 portal 登录、direct-login、写闭环或清理动作。
- 当前仍未完成：
  - seller/buyer OWNER 账号密码真实登录。
  - 管理端 direct-login 票据签发、目标 portal 一次性消费和成功回传。
  - 真实 token 下的 `/getInfo` / `/getRouters`、401 跳转、token/session、日志审计隔离。
  - OWNER 在真实 portal 内创建子账号、设置角色、维护部门、查看登录日志、操作日志和在线会话。
- 当前下一步：
  - 需要用户明确确认 live 写入范围和 seller/buyer OWNER 凭据后，才可运行 `verify:portal-self-management-live`、`verify:portal-direct-login-live`、`verify:portal-self-management-live-write`。
  - live 验证会写登录日志、操作日志、direct-login 票据、portal session，并会留下停用 STAFF 测试账号；未确认前继续保持 fail-closed。

## runbook 与 live 脚本参数同步加固

- 加固时间：2026-06-12 02:09 +08:00。
- 发现问题：
  - `verify-portal-self-management-live.mjs` 已要求 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY`，但 runbook 的只读 live 示例缺少该确认变量。
  - `verify-portal-direct-login-live.mjs` 已支持 `ADMIN_AUTH_TOKEN` 或 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 两种管理端认证方式，并支持不传主体 ID 时自动发现 active OWNER 主体；runbook 仍写成必须显式传入当前管理端 token 和目标主体 ID。
- 已修正：
  - runbook 的只读 live 示例补入 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM`，并说明缺少确认变量或账号密码时脚本 fail-closed。
  - runbook 的 direct-login live 说明改为管理端认证上下文二选一，目标主体 ID 可选；未提供时自动发现 active OWNER 主体。
  - `portal-self-management-live-contract.test.ts` 新增 runbook 对齐合同，固定只读 live 文档必须包含确认变量。
  - `portal-direct-login-live-contract.test.ts` 新增 runbook 对齐合同，固定 direct-login 文档必须包含 token 登录、账号密码登录、主体 ID 可选和自动发现说明。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts tests/portal-direct-login-live-contract.test.ts --runInBand` 通过，2 suites / 26 tests passed。
  - `cd react-ui; node scripts\verify-portal-self-management-live.mjs --help` 通过，help 输出包含 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY`。
  - `cd react-ui; node scripts\verify-portal-direct-login-live.mjs --help` 通过，help 输出包含 `ADMIN_AUTH_TOKEN`、`ADMIN_USERNAME` / `ADMIN_PASSWORD` 和可选主体 ID。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `git diff --check -- <本轮 runbook 同步相关文件>` 通过，仅 LF/CRLF 工作区提示。
- 影响边界：
  - 本次只修文档与静态合同，不执行 live 登录、direct-login 或写闭环。
  - 未写 MySQL、Redis 或业务数据。

## 静态复核：主体与账号作用域隔离

- 复核时间：2026-06-12 02:12 +08:00。
- 复核范围：
  - 前端 `react-ui/src/pages/Portal/Home/PortalSelfManagement.tsx` 和 `react-ui/src/services/portal/session.ts` 的 portal 自助请求。
  - 后端 seller/buyer portal controller、service、mapper 和 SQL 映射中的账号查询链路。
- 静态扫描结论：
  - 未发现生产代码保留裸 `select*AccountById(accountId)` mapper/service 调用；命中的生产入口均为 `selectSellerAccountById(Long sellerId, Long sellerAccountId)` / `selectBuyerAccountById(Long buyerId, Long buyerAccountId)` 或 `select*AccountByIdAnd*Id(...)` scoped 查询。
  - portal 自助页面不直接引用 `/api/seller` / `/api/buyer`，不直接传 `sellerId` / `buyerId` / `subjectId`；通过 `PORTAL_SERVICE[terminal]` 调用端内 service。
  - portal service 对账号、密码、角色、部门等写 payload 清洗 caller-controlled scope 字段，避免把前端传入主体 ID 当权限范围。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过；`TerminalAccountIsolationTest` 4 tests passed，reactor BUILD SUCCESS。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-session-request.test.ts tests/portal-self-management-contract.test.ts --runInBand` 通过，2 suites / 62 tests passed。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
- 影响边界：
  - 本次只做静态扫描和合同复核，不执行 live 登录、direct-login 或写闭环。
  - 未写 MySQL、Redis 或业务数据。

## 当前状态收口：seed 已落库，live 验证待确认

- 收口时间：2026-06-12 02:02 +08:00。
- 记录口径：
  - 本文件中早于 `2026-06-12 01:36` 的 `7 / 19`、`245 = 35 * 7`、`未执行 SQL seed`、`权限模板未落库` 等结论均为历史预检快照。
  - 当前权威状态以 `2026-06-12 01:36` seed 执行结果和本节只读 precheck 为准。
- 当前数据源确认：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 当前 `spring.profiles.active` 为 `druid`。
  - `application-druid.yml` 中 MySQL 连接仍来自 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 注入。
  - Redis 仍来自 `RUOYI_REDIS_*` 注入；本节只读 precheck 不读写 Redis。
  - `.env.local` 中上述 MySQL / Redis 运行变量存在，记录中不输出明文值。
- 当前只读复核：
  - 命令：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 性质：只读 `SELECT`；未执行 SQL seed；未写 MySQL、Redis 或业务数据。
  - 目标库：`fenxiao`。
  - `seller_menu`：23 行，ID 范围 `100008-100031`。
  - `buyer_menu`：23 行，ID 范围 `200003-200026`。
  - active seller/buyer OWNER 角色：各 35 个。
  - seller/buyer self-management 权限模板：均为 `19 / 19`。
  - seller/buyer root button 权限模板：均为 `18 / 18`。
  - seller/buyer OWNER self-management 授权：均为 `665 = 35 * 19`。
  - seller/buyer OWNER 非 self-management 授权：均为 0。
  - seller/buyer 无效权限、菜单 ID 区间违规、页面 component 违规、重复 perms：均为 0。
- 当前完成标准状态：
  - 权限 seed 和 OWNER 最小自助授权：已落库并经只读 precheck 验证。
  - 代码级门禁：`verify:three-terminal` 最新已通过，前端 33 suites / 300 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - 仍未完成：真实 seller/buyer OWNER 账号密码登录、管理端 direct-login 票据签发与目标 portal 一次性消费、真实 `/getInfo` / `/getRouters`、401 跳转、日志/会话隔离、OWNER 创建子账号/角色/部门写闭环。
- 本轮收尾检查：
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `git diff --check -- docs\plans\2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md docs\reports\2026-06-10-three-terminal-portal-self-management-progress-record.md` 通过，仅 LF/CRLF 工作区提示。
  - `codegraph sync .` 通过，结果为 `Already up to date`。
- 当前下一步：
  - 继续前需要用户明确确认 live 写入范围和 seller/buyer OWNER 凭据。
  - live 登录会写登录日志和 portal session；direct-login 会写短时票据、审计和 session；self-management 写闭环会创建/清理测试角色、测试部门并留下停用 STAFF 测试账号。
  - 未获确认前，不执行 `verify:portal-self-management-live`、`verify:portal-direct-login-live` 或 `verify:portal-self-management-live-write`。

## 续跑执行前确认：self-management 权限 seed

- 记录时间：2026-06-12 01:33 +08:00。
- 用户已明确确认：允许对当前目标库 `fenxiao` 执行本阶段 self-management 权限 seed，影响范围仅限 `seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`。
- 已复核当前激活配置：后端 profile 为 `druid`；MySQL 连接由 `.env.local` 注入的 `RUOYI_DB_*` 提供，记录中不输出连接串、账号或密码；Redis 由 `RUOYI_REDIS_*` 提供，但本次 SQL seed 不读写 Redis。
- 已执行只读预检：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
- 预检结果：目标库 `fenxiao`；seller/buyer self-management 权限模板均为 `7 / 19`；seller/buyer OWNER self-management 授权均为 `245 = 35 * 7`；无非 self-management OWNER 授权残留；无无效权限、ID 区间、component 或重复 perms 问题。
- 下一步：执行受控命令 `PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`，随后以 runner postcheck、`npm run verify:three-terminal` 和 Markdown 记录收口。

## 续跑执行结果：self-management 权限 seed 已落库

- 记录时间：2026-06-12 01:36 +08:00。
- 已执行受控命令：`PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`。
- SQL 执行结果：退出码 0；未出现 `45000`；runner 完成 commit 后输出 `portal self-management SQL seed applied with explicit confirmation.`。
- 执行后 postcheck：
  - 目标库仍为 `fenxiao`。
  - `seller_menu` 23 行，ID 范围 `100008-100031`；`buyer_menu` 23 行，ID 范围 `200003-200026`。
  - seller/buyer self-management 权限模板均为 `19 / 19`。
  - seller/buyer root button 权限模板均为 `18 / 18`。
  - seller/buyer OWNER self-management 授权均为 `665 = 35 * 19`。
  - seller/buyer OWNER 非 self-management 授权均为 0。
  - 无无效权限、ID 区间违规、页面 component 违规或重复 perms。
- 执行后又复跑只读 `--precheck`，结果与 postcheck 一致。
- 已复跑 `cd react-ui; npm run verify:three-terminal`：通过；前端 33 suites / 296 tests passed；后端 reactor `test-compile` 与三端合同测试均 BUILD SUCCESS。
- live 验证未执行：当前 shell 未注入 seller/buyer portal 账号密码、`ADMIN_AUTH_TOKEN`、直登主体 ID、`PORTAL_DIRECT_LOGIN_LIVE_CONFIRM` 和 `PORTAL_LIVE_WRITE_CONFIRM`；同时本轮用户确认只覆盖权限 seed，不覆盖 live 写验证新增/禁用账号、角色、部门等远端 DML。
- 当前完成度判断：
  - 已完成：远端 `fenxiao` 的 seller/buyer self-management 最小权限模板和 OWNER 授权补齐；代码级 guard、合同测试和三端总门禁通过。
  - 未验证：真实 seller/buyer 账号密码登录、管理端 OWNER 直登一次性消费、真实 `/getInfo` / `/getRouters`、401 跳转、日志/会话隔离、OWNER 创建子账号/角色/部门写闭环。
  - P2 遗留：浏览器截图、DOM/UI 细调、完整业务菜单和冻结业务页仍不进入本轮完成口径。

## 续跑收尾检查

- 检查时间：2026-06-12 01:37 +08:00。
- `git diff --check -- <本轮三端相关文件>`：通过；仅输出 LF/CRLF 工作区换行提示，无 whitespace error。
- `codegraph sync .`：通过；输出 `Already up to date`。
- 工作区说明：仍存在 finance、`application.yml`、六小时审查日志等非本轮 self-management seed 收口文件改动；本轮未回退、未覆盖这些改动。

## 续跑 live 验证准备：后端启动与候选主体发现

- 记录时间：2026-06-12 01:42 +08:00。
- 已按项目约定读取激活配置和启动脚本：后端 profile 为 `druid`；MySQL/Redis/token secret 均由 `.env.local` 注入，记录中未输出任何连接串、账号、密码、token 或密钥明文。
- 后端启动：执行 `.\start-backend-local.ps1 -Restart`，随后 `http://127.0.0.1:8080` 返回 HTTP 200，8080 监听进程存在。
- 管理端登录探测：`/captchaImage` 返回 `captchaEnabled=false`；默认管理端账号 `admin/admin123` 登录成功并获得临时 token。token 仅用于当前 live 探测，未写入记录、未输出。
- 管理端候选主体发现：通过管理端接口只读列出 seller/buyer 主体和 OWNER 账号候选；seller active OWNER 候选 35 个，buyer active OWNER 候选 35 个。
- 可用于后续 direct-login live 的候选：
  - seller：`sellerId=41`，主体编号 `SAF100031`，OWNER 用户名 `s_mingtaistorage`。
  - buyer：`buyerId=36`，主体编号 `BAF100034`，OWNER 用户名 `b_parksideretail`。
- 尚未执行：
  - 未执行 `verify-portal-direct-login-live.mjs`，因为该脚本会创建并消费 direct-login 票据、写 direct-login 审计和 portal session，需要 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY` 级别确认。
  - 未执行 seller/buyer portal 账号密码登录 live，因为当前环境没有注入 `SELLER_PORTAL_USERNAME`、`SELLER_PORTAL_PASSWORD`、`BUYER_PORTAL_USERNAME`、`BUYER_PORTAL_PASSWORD`；如使用发现的 OWNER 用户名和候选默认密码，也会写登录日志和 session，需要确认。
  - 未执行 `verify-portal-self-management-live-write.mjs`，因为该脚本会创建/维护测试角色、部门和禁用 STAFF 测试账号，需要 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY` 级别确认。
- 下一步确认口径：如继续完成 live 闭环，需要用户明确允许在当前 `fenxiao` / `127.0.0.1:8080` 后端上执行 live direct-login、portal 登录和受控写验证，并接受写入登录日志、操作日志、direct-login 票据、portal session，以及写验证留下禁用 STAFF 测试账号作为证据。

## 续跑加固：direct-login live verifier 自动认证与主体发现

- 记录时间：2026-06-12 01:46 +08:00。
- 加固范围：`react-ui/scripts/verify-portal-direct-login-live.mjs` 与 `react-ui/tests/portal-direct-login-live-contract.test.ts`。
- 已完成：
  - direct-login live 仍必须显式设置 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`，未确认时不会执行写票据/写 session 验证。
  - 支持 `ADMIN_AUTH_TOKEN` 或 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 两种管理端认证来源；脚本不会读取 `.env.local`，不会输出管理端 token。
  - 支持在未提供 `SELLER_DIRECT_LOGIN_SUBJECT_ID` / `BUYER_DIRECT_LOGIN_SUBJECT_ID` 时，通过管理端只读列表和账号列表自动发现 active OWNER 主体。
  - 自动发现只访问管理端 seller/buyer 主体与账号列表，不扩展业务接口，不访问商品、订单、库存、物流、财务、履约或外部系统。
- 已验证：
  - `node --check react-ui/scripts/verify-portal-direct-login-live.mjs` 通过。
  - `node scripts/verify-portal-direct-login-live.mjs --help` 通过。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts --runInBand` 通过，1 suite / 11 tests。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 297 tests passed；后端 reactor `test-compile` 与三端合同测试均 BUILD SUCCESS。
- 未验证：未执行真实 direct-login live，因为仍缺少用户对 direct-login 票据、审计日志和 portal session 写入的确认。

## 续跑加固：direct-login live 确认门槛负向合同

- 记录时间：2026-06-12 01:50 +08:00。
- 加固范围：`react-ui/tests/portal-direct-login-live-contract.test.ts`。
- 加固内容：
  - 新增合同固定 `verify-portal-direct-login-live.mjs` 的 `main()` 必须先执行 `requireLiveEnv()`，再进入任何 `verifyTerminal(...)`。
  - 新增合同固定 `requireLiveEnv()` 必须检查 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`。
  - 新增合同固定确认门槛之前不得出现 `await`，避免缺少确认变量时触发管理端登录、主体发现或 direct-login 写入。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts --runInBand` 通过，1 suite / 12 tests。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `node --check react-ui/scripts/verify-portal-direct-login-live.mjs` 通过。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 298 tests passed；后端 reactor `test-compile` 与三端合同测试均 BUILD SUCCESS。
- 仍未执行：真实 direct-login live、portal 账号密码登录 live 和 self-management 写闭环，原因仍是缺少用户对相关远端写日志、票据、session 和测试数据的确认。

## 续跑加固：portal 账号密码 live 确认门槛

- 记录时间：2026-06-12 01:54 +08:00。
- 加固范围：`react-ui/scripts/verify-portal-self-management-live.mjs` 与 `react-ui/tests/portal-self-management-live-contract.test.ts`。
- 加固原因：账号密码 live 验证会调用 seller/buyer `/login`，即使后续只读，也会写登录日志和 portal session；因此必须和 direct-login live、write live 一样有显式确认门槛。
- 已完成：
  - 新增 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY`。
  - `requireLiveEnv()` 会在检查 seller/buyer 账号密码前检查该确认变量。
  - help 文案明确：登录后不做 portal 写操作，但真实登录会创建登录日志和 session。
  - 合同测试固定 `main()` 必须先执行 `requireLiveEnv()`，再进入任何 `verifyTerminal(...)`；确认门槛前不得出现 `await`。
- 已验证：
  - `node --check react-ui/scripts/verify-portal-self-management-live.mjs` 通过。
  - `cd react-ui; node scripts\verify-portal-self-management-live.mjs --help` 通过。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts --runInBand` 通过，1 suite / 12 tests。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 299 tests passed；后端 reactor `test-compile` 与三端合同测试均 BUILD SUCCESS。
- 未验证：未执行真实 seller/buyer 账号密码登录，原因仍是缺少用户对登录日志和 portal session 写入的确认，以及 seller/buyer OWNER 凭据确认。

## 续跑审计：本阶段完成标准逐项证据矩阵

- 审计时间：2026-06-11 04:55 +08:00。
- 审计结论：当前代码侧 P0/P1 门禁通过，但本阶段目标不能标记完成；远端 `fenxiao` 仍未落库本轮新增 self-management 权限模板，且 seller/buyer portal live 登录、直登和写闭环尚未验证。
- 当前只读数据证据：
  - 通过：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：目标库 `fenxiao`；seller/buyer portal 首页 `C` 菜单各 1 个；seller/buyer self-management 权限模板各 7 / 19 个；seller/buyer root button 权限模板各 6 / 18 个；active OWNER 角色各 35 个；OWNER self-management 授权各 245 条，即 `35 * 7`；OWNER 非 self-management 授权均为 0；无效权限、菜单 ID 区间违规、页面 component 违规、重复 perms 均为 0。
  - 影响边界：只读 `SELECT`，runner 输出 `portal self-management SQL precheck completed without writes.`；未执行 SQL seed，未写 MySQL 或 Redis。
- 当前代码门禁证据：
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 296 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。

| 完成标准 | 当前状态 | 当前证据 | 仍缺证据 / 下一步 |
| --- | --- | --- | --- |
| 管理端继续保留若依 `sys_*`，只负责平台管理员、主体管理、主体状态、免密代入、强制控制和审计 | 代码级已覆盖，live 未验证 | 三端门禁包含 seller/buyer admin permission、direct-login、partner management 和审计相关合同；`verify:three-terminal` 通过 | 需要真实管理端账号在 live 环境对 seller/buyer 主体各发起一次 OWNER 直登并确认审计记录 |
| seller/buyer 账号、密码、角色、部门、菜单模板、权限、登录日志、操作日志、会话独立于 `sys_*` | 代码级已覆盖，数据模板未补齐 | portal 控制器和 service 走 `seller_*` / `buyer_*`；subject/account 从 `PortalSessionContext` 推导；precheck 显示端内表和 ID 区间无违规 | 执行 self-management seed 后确认 `seller_menu` / `buyer_menu` 各 19 个模板，OWNER 授权为 `35 * 19` |
| 管理端“卖家管理 / 买家管理”默认免密进入对应 OWNER 主账号 | 代码级已覆盖，live 未验证 | direct-login message bridge、后端 direct-login 支持、一次性/跨端/字段白名单合同均进入 `verify:three-terminal` | 需要用真实管理端 token 运行 direct-login live 验证，确认目标 portal 消费后回传成功 |
| seller/buyer 主账号可通过账号密码直接登录各自 portal | 代码级已覆盖，live 未验证 | portal 登录、token/session、401 和请求净化测试进入 `verify:three-terminal` | 需要真实 seller/buyer OWNER 账号密码运行只读 live 验证 |
| OWNER 可在 portal 内创建子账号、设置角色、维护部门、查看登录日志、操作日志和在线会话 | 代码级已覆盖，数据模板未补齐，live 未验证 | `PortalSelfManagement`、portal service、账号/角色/部门/日志/会话合同和受控写 verifier 合同进入 `verify:three-terminal` | 需要先执行 seed，再运行受控写 verifier；当前远端模板只有 7 / 19，写闭环会缺权限 |
| seller/buyer 不开放菜单定义能力，角色只能从预置模板分配权限 | 代码级已覆盖，数据模板未补齐 | controller 和 service 对 role-menu 使用 `PortalPermissionSupport.assertReadableTerminalMenu(...)`；SQL seed 与 runner postcheck 固定 ID 区间、component、权限前缀、通配/admin 禁止 | 执行 seed 后用 live `/roles/menus` 验证每端 19 个模板且 ID 区间正确 |
| 商品、订单、库存、物流、财务、履约、外部系统等业务菜单冻结 | 代码级已覆盖，live 未验证 | portal self-management 合同、live verifier 合同和三端门禁固定冻结业务权限/路由不可见；precheck OWNER 非 self-management 授权为 0 | 执行 seed 后用真实 OWNER token 验证 `getRouters` 不出现冻结业务路由 |
| `getInfo`、`getRouters`、401 跳转、token/session、权限校验、日志审计按 seller/buyer 隔离 | 代码级已覆盖，live 未验证 | `check-portal-token-isolation`、`portal-unauthorized-redirect`、`portal-session-request`、live verifier 合同和后端合同均通过 | 需要真实 token 下运行只读 live 验证和 direct-login live 验证 |
| 最小必要测试通过 | 已完成 | `verify:three-terminal` 通过，前端 33 suites / 296 tests passed，后端 reactor `test-compile` 和三端合同 BUILD SUCCESS | SQL apply 后需再复跑同一门禁 |
| Markdown 记录明确已完成、未验证、P2 遗留和下一步 | 持续更新中 | 本文件和 `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 已追加当前 precheck、未完成项和下一步 | SQL apply / live 验证后继续追加执行记录 |

当前硬阻塞不是代码编译或静态门禁，而是远端 DML seed 需要用户明确确认目标环境后才能执行。未确认前不得执行 `PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`。

## 阻塞后刷新：2026-06-12 只读预检

- 刷新时间：2026-06-12 01:16 +08:00。
- 刷新原因：
  - 目标已因缺少远端 DML seed 执行确认而标记 blocked；新一轮继续前先确认目标库是否已被外部补齐。
  - 本次只允许只读刷新，不执行 SQL apply，不做 live 写验证。
- 当前只读结果：
  - 通过：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：目标库仍为 `fenxiao`。
  - seller/buyer portal 首页 `C` 菜单仍各 1 个。
  - seller/buyer self-management 权限模板仍各 7 / 19 个。
  - seller/buyer root button 权限模板仍各 6 / 18 个。
  - active seller/buyer OWNER 角色仍各 35 个。
  - seller/buyer OWNER self-management 授权仍各 245 条，即 `35 * 7`。
  - seller/buyer OWNER 非 self-management 授权仍均为 0。
  - seller/buyer 无效权限、菜单 ID 区间违规、页面 component 违规、重复 perms 仍均为 0。
- 当前结论：
  - 外部状态没有补齐本阶段权限模板。
  - 仍不能标记完成；仍需要用户确认后执行 `20260610_portal_self_management_permission_seed.sql`，再做 postcheck、`verify:three-terminal` 和 live 验证。
  - 本次未写 MySQL、Redis 或业务数据，未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## blocked 后恢复审计：2026-06-12 01:18 只读刷新

- 刷新时间：2026-06-12 01:18 +08:00。
- 刷新性质：fresh blocked audit；只读确认当前外部状态，不执行 SQL apply，不做 live 写验证。
- 当前只读结果：
  - 通过：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：目标库仍为 `fenxiao`；seller/buyer self-management 权限模板仍各 7 / 19 个；seller/buyer root button 权限模板仍各 6 / 18 个；active OWNER 角色仍各 35 个；OWNER self-management 授权仍各 245 条，即 `35 * 7`；OWNER 非 self-management 授权仍为 0；无效权限、菜单 ID 区间违规、页面 component 违规、重复 perms 均为 0。
  - runner 输出：`portal self-management SQL precheck completed without writes.`
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
- 当前结论：
  - 目标库仍未补齐本阶段权限模板，不能进入 live 最小闭环验收。
  - 仍需用户明确确认后执行 `PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`。
  - 未确认前，本轮继续保持只读，不写 MySQL、Redis 或业务数据。

## 续跑复核：SQL seed 当前目标库只读预检

- 复核时间：2026-06-11 04:51 +08:00。
- 复核原因：
  - 本阶段最终闭环仍需要执行 self-management 权限 seed 后再做 live 验证；执行前必须确认当前激活数据源和目标库状态。
  - 需要用当前工作区和当前外部状态重新确认缺口是否仍是“权限模板未落库”，而不是代码侧新增 P0/P1 缺口。
- 已复核：
  - 读取 `application.yml` / `application-druid.yml`，当前激活 profile 为 `druid`；MySQL 连接由 `RUOYI_DB_*` 注入，Redis 由 `RUOYI_REDIS_*` 注入，不能默认 localhost。
  - 只脱敏检查 `.env.local` 中存在 `RUOYI_DB_URL`、`RUOYI_REDIS_HOST`、`RUOYI_REDIS_DATABASE`，未输出连接串、账号、密码或 Redis 密码。
  - `docker ps` 无法连接 Docker API，未发现本地 Docker MySQL/Redis 正在运行并可被误读。
  - 运行 `node .\scripts\portal-self-management-sql-runner.mjs --precheck`，仅执行只读 `SELECT`，runner 输出 `portal self-management SQL precheck completed without writes.`。
- 预检结果：
  - 目标库：`fenxiao`。
  - seller/buyer portal 首页 `C` 菜单：各 1 个。
  - seller/buyer self-management 权限模板：各 7 / 19 个。
  - seller/buyer root button 权限模板：各 6 / 18 个。
  - active seller/buyer OWNER 角色：各 35 个。
  - seller/buyer OWNER self-management 授权：各 245 条，即 `35 * 7`。
  - seller/buyer OWNER 非 self-management 授权：均为 0。
  - seller/buyer 无效权限、菜单 ID 区间违规、页面 component 违规、重复 perms：均为 0。
- 当前结论：
  - 目标库不需要先执行 portal 首页前置 seed。
  - 目标库仍缺少本轮新增的每端 12 个账号/部门/角色自助按钮和查询权限模板。
  - SQL seed 仍未执行；live portal 登录、直登、写验证仍未执行。
  - 下一步仍需用户确认目标环境后执行 `20260610_portal_self_management_permission_seed.sql`，再做 seller/buyer portal live 最小闭环验证。

## 继续修复：portal 账号角色接口后端多权限强约束

- 修复时间：2026-06-11 04:46 +08:00。
- 修复原因：
  - `PortalSelfManagement.tsx` 前端已要求分配账号角色入口同时具备端内角色列表、账号角色查询和账号角色编辑权限。
  - 后端 portal `/accounts/{targetAccountId}/roles` 会读取端内角色列表；如果后端只校验 `*:account:role:query` 或 `*:account:role:edit`，会形成“前端严、后端松”的权限缺口。
  - 本阶段 P1 要求权限校验不能只依赖前端隐藏按钮，后端实际调用链路也必须覆盖会读取或修改的权限域。
- 已修复：
  - `SellerPortalController` / `BuyerPortalController` 的账号角色查询接口改为同时要求 `*:account:role:query` 和 `*:role:list`。
  - `SellerPortalController` / `BuyerPortalController` 的账号角色分配接口改为同时要求 `*:account:role:edit`、`*:account:role:query` 和 `*:role:list`。
  - `PortalSelfServiceSurfaceContractTest` 固定上述后端多权限合同，防止回退为单权限校验。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 结果：`PortalSelfServiceSurfaceContractTest` 1 test passed；seller/buyer 模块随 reactor 编译通过；BUILD SUCCESS。
  - 首次误在仓库根目录执行 `npm run verify:three-terminal`，因根目录无 `package.json` 返回 `ENOENT`；随后改到 `react-ui` 目录执行。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 296 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
  - 通过：`git diff --check -- <本阶段三端 portal 目标相关文件>`。
  - 结果：无 whitespace error；仅 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`。
  - 结果：`Synced 3 changed files`，`Modified: 3 - 284 nodes in 1.3s`。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：SQL seed 与 runner 模板签名强断言

- 加固时间：2026-06-11。
- 加固原因：
  - `20260610_portal_self_management_permission_seed.sql` 已能补齐每端缺失的 12 个 self-management 权限，并对 OWNER 授权数量做断言；但本轮执行前还需要把已有 7 个模板的签名也纳入同一 seed 的 fail-closed 边界。
  - 受控 runner 原 postcheck 主要校验数量和 OWNER 授权，若目标库存在签名漂移，可能要到 live `/roles/menus` 阶段才暴露；执行后检查应提前判死。
- 已加固：
  - `20260610_portal_self_management_permission_seed.sql` 增加 portal 首页 `C` 菜单签名断言，以及 18 个 root `F` self-management 权限模板签名断言。
  - 同一 seed 的 slot guard 扩展到 portal 首页、账号列表、登录日志、操作日志、在线会话、部门列表、角色列表等既有模板，避免历史脏模板被本轮授权继续沿用。
  - `scripts/portal-self-management-sql-runner.mjs` 的 postcheck 增加 root `F` 模板精确数量、端内菜单 ID 区间、页面 component 根路径、重复 perms 等硬断言。
  - `SqlExecutionGuardContractTest` 与 `portal-self-management-sql-runner-contract.test.ts` 同步固定上述合同。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：1 suite / 6 tests passed。
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 结果：`SqlExecutionGuardContractTest` 82 tests passed；BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 296 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
  - 通过：`git diff --check -- <本轮三端 portal 与 SQL runner 相关文件>`。
  - 结果：无 whitespace error；仅 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`。
  - 结果：`Synced 3 changed files`，`Modified: 3 - 176 nodes in 1.5s`。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：portal 自管前端入口权限依赖收口

- 加固时间：2026-06-11。
- 加固原因：
  - 账号“分配角色”入口会依赖已加载的端内角色下拉选项，页面只有具备 `role:list` 时才加载角色列表。
  - “新增角色”入口会先调用 `/roles/menus` 加载平台预置权限模板，后端要求 `role:query`。
  - 为避免前端入口可见但后端查询接口 403，入口权限需要覆盖实际会调用的查询和编辑接口权限。
- 已加固：
  - `PortalSelfManagement.tsx` 新增 `canAssignAccountRoles = canViewRoles && canQueryAccountRole && canEditAccountRole`。
  - 账号行内“角色”入口改为使用 `canAssignAccountRoles`。
  - `PortalSelfManagement.tsx` 新增 `canCreateRole = canAddRole && canQueryRole`。
  - 角色管理“新增角色”入口改为使用 `canCreateRole`。
  - `portal-self-management-contract.test.ts` 固定上述两个派生权限条件，防止后续只按写权限展示入口。
- 已验证：
  - 首次并行窄跑 `npx jest --config jest.config.ts tests/portal-self-management-contract.test.ts --runInBand` 与 `terminal-session-token` / `portal-unauthorized-redirect` 时，portal 自管合同进程遇到 Umi `.umi-test` 并发清理 `ENOTEMPTY`，属于并行生成目录冲突。
  - 串行重跑 `npx jest --config jest.config.ts tests/portal-self-management-contract.test.ts --runInBand` 通过：1 suite / 5 tests passed。
  - 并行的 `terminal-session-token.test.ts` 与 `portal-unauthorized-redirect.test.ts` 已通过：2 suites / 24 tests passed。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 296 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
  - 通过：`git diff --check -- <本轮 portal 自管入口权限、菜单模板加固与记录文件>`。
  - 结果：无 whitespace error；仅 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`。
  - 结果：`Synced 2 changed files`，`Modified: 2 - 31 nodes in 1.0s`。
  - 记录补写后复跑 `git diff --check -- <本轮相关文件>` 仍无 whitespace error；复跑 `codegraph sync .` 显示 `Already up to date`。
- 影响边界：
  - 未新增接口、SQL、seed 或业务菜单。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务页面。

## 续跑加固：只读 live 自助审计 DTO 脱敏门禁

- 加固时间：2026-06-11。
- 加固原因：
  - 写验证脚本已经检查自助登录日志、操作日志和在线会话 DTO 不泄露内部审计字段，但只读 live 脚本此前只验证接口可读和冻结业务面不可见。
  - 本阶段要求端内自助日志接口不得返回 `subjectId`、`accountId`、`directLoginTicketId`、`actingAdmin*`、`directLoginReason`、`operParam`、`jsonResult`、`tokenId` 等内部审计模型字段；该规则应在只读 live 验证中也能提前发现漂移。
- 已加固：
  - `react-ui/scripts/verify-portal-self-management-live.mjs` 新增 `FORBIDDEN_SELF_AUDIT_FIELDS` 和 `assertSelfAuditDto(...)`。
  - 只读 live 验证访问 `/account/login-logs`、`/account/oper-logs`、`/account/sessions` 时，会检查 rows/data 中不得出现内部审计字段。
  - `react-ui/tests/portal-self-management-live-contract.test.ts` 新增合同，固定三个自助审计端点和禁止字段清单。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 同步更新只读 live 验证范围。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts --runInBand`。
  - 结果：1 suite / 8 tests passed。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 27 tests passed。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 290 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：live 登录与直登响应白名单门禁

- 加固时间：2026-06-11。
- 加固原因：
  - 后端 `PortalLoginResult` / `PortalDirectLoginResult` 已通过 `@JsonIgnore` 隐藏内部 ID，前端类型也已有白名单；但 live verifier 原先只检查 token、terminal 等必要字段，没有在真实响应层拒绝额外字段。
  - 本阶段要求 token/session 和审计边界端内隔离，登录响应与 direct-login 响应不应把 `subjectId`、`accountId`、`tokenHash`、内部审计字段或其他未知字段交给前端运行态。
- 已加固：
  - `react-ui/scripts/verify-portal-self-management-live.mjs` 新增 portal 登录响应白名单，只允许 `token`、`terminal`、`subjectNo`、`username`、`nickName`、`expireMinutes`、`expireTime`。
  - `react-ui/scripts/verify-portal-direct-login-live.mjs` 新增管理端 direct-login ticket 响应白名单，只允许 `token`、`ticketId`、`loginUrl`、`expireMinutes`、`expireTime`。
  - direct-login 消费后的 portal 登录响应同步复用 portal 登录白名单。
  - `react-ui/tests/portal-self-management-live-contract.test.ts` 和 `react-ui/tests/portal-direct-login-live-contract.test.ts` 同步固定白名单合同。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 同步更新 live 验证范围。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts tests/portal-direct-login-live-contract.test.ts --runInBand`。
  - 结果：2 suites / 18 tests passed。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 29 tests passed。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 292 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：live getInfo 响应白名单门禁

- 加固时间：2026-06-11。
- 加固原因：
  - 登录响应与 direct-login 响应已纳入白名单，但 `/getInfo` 真实响应仍需要运行态字段白名单，避免后续把 `subjectId`、`accountId`、`terminal` 或其他内部身份字段带回 portal 前端。
  - 本阶段完成口径要求 `getInfo`、权限校验和 token/session 均按 seller / buyer 隔离；`getInfo` 不仅要权限集合正确，还应只暴露端内前端需要的公开身份与权限字段。
- 已加固：
  - `react-ui/scripts/verify-portal-self-management-live.mjs` 新增 `ALLOWED_PORTAL_GET_INFO_FIELDS`，只允许 `subjectNo`、`userName`、`nickName`、`roles`、`permissions`。
  - 账号密码登录 live verifier 在 `/getInfo` 成功后立即执行 `assertPortalGetInfoContract(terminal, info)`。
  - `react-ui/scripts/verify-portal-direct-login-live.mjs` 同步给 direct-login 消费后的 `/getInfo` 增加相同字段白名单。
  - `react-ui/tests/portal-self-management-live-contract.test.ts` 和 `react-ui/tests/portal-direct-login-live-contract.test.ts` 同步固定 getInfo 白名单合同。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 同步更新只读 live 与 direct-login live 验证范围。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts tests/portal-direct-login-live-contract.test.ts --runInBand`。
  - 结果：2 suites / 20 tests passed。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live.mjs --help`。
  - 通过：`cd react-ui; node scripts\verify-portal-direct-login-live.mjs --help`。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 31 tests passed。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 294 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑加固：只读 live 角色菜单模板门禁

- 加固时间：2026-06-11。
- 加固原因：
  - 本阶段要求端内角色只能从平台预置的 `seller_menu` / `buyer_menu` 权限模板中分配权限，并且菜单 ID 空间必须 seller/buyer 分离。
  - 写验证脚本已在受控写闭环中检查 role menu 模板 ID 区间，但只读 live 脚本此前只访问 `/roles/menus`，没有校验模板是否完整或是否出现跨端 ID 污染。
- 已加固：
  - `react-ui/scripts/verify-portal-self-management-live.mjs` 新增 `assertTerminalRoleMenuTemplate(...)`。
  - 只读 live 脚本访问 `/roles/menus` 后会校验：
    - 模板非空。
    - 菜单 ID 不重复。
    - seller 菜单 ID 必须位于 `100000-199999`，buyer 菜单 ID 必须位于 `200000-299999`。
    - 模板数量必须等于 19 个 self-management 权限点，提前发现 seed 未补齐或模板缺口。
  - `react-ui/tests/portal-self-management-live-contract.test.ts` 同步固定该只读门禁。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 同步更新只读 live 验证范围。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts --runInBand`。
  - 结果：1 suite / 11 tests passed。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live.mjs --help`。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 32 tests passed。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 295 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 续跑修复：portal 账号管理 accountId 可见与写验证入口收紧

- 修复时间：2026-06-11。
- 修复原因：
  - `PortalAccountProfile.accountId` 此前被 `@JsonIgnore` 隐藏，但 portal 自助账号管理页面和受控写验证都需要端内 `accountId` 作为编辑账号、加载账号角色、分配角色和停用测试账号的操作句柄。
  - `/getInfo` 的 `subjectId/accountId/terminal` 仍必须隐藏；账号列表可以暴露当前端内账号的 `accountId`，但不得暴露 `subjectId` 或 `terminal`。
  - write verifier 此前还用 `info.subjectId` 对比新建账号，和 `/getInfo` 响应白名单边界冲突。
- 已修复：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalAccountProfile.java` 取消 `accountId` 的 `@JsonIgnore`，保留 `terminal` 和 `subjectId` 隐藏。
  - `PortalHomeProfileSerializationTest` 更新合同：账号 profile 必须序列化 `accountId`，但不得序列化 `terminal` / `subjectId`。
  - `react-ui/scripts/verify-portal-self-management-live-write.mjs` 新增：
    - 登录响应字段白名单。
    - `/getInfo` 响应字段白名单。
    - 角色菜单模板非空、无重复、端内 ID 区间正确且数量等于 19 个 self-management 权限点。
    - 新建账号记录必须返回可用 `accountId`，且不得返回 `subjectId` / `terminal`。
  - write verifier 移除对 `info.subjectId` 的依赖，后续真实写验证不再要求 `/getInfo` 暴露内部主体 ID。
  - `react-ui/tests/portal-self-management-live-write-contract.test.ts` 固定上述入口合同。
  - `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 同步更新写验证通过标准。
- 已验证：
  - 首次尝试：`cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalHomeProfileSerializationTest test` 失败。
  - 失败原因：未带 reactor 依赖，`ruoyi-system` 单独编译找不到 `ruoyi-common` 中的 `ImageResourceUtils`；未发现本次代码逻辑失败。
  - 修正后通过：`cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=PortalHomeProfileSerializationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 结果：`PortalHomeProfileSerializationTest` 5 tests passed；BUILD SUCCESS。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-write-contract.test.ts --runInBand`。
  - 结果：1 suite / 6 tests passed。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live-write.mjs --help`。
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 33 tests passed。
  - 通过：`cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`。
  - 结果：`three-terminal manifest check passed.`
  - 通过：`node scripts\portal-self-management-sql-runner.mjs --precheck`。
  - 结果：只读预检成功；目标库 `fenxiao`；seller/buyer self-management 权限模板仍为 7 / 19；OWNER 非 self-management 授权为 0；未执行 DML。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 296 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 子 Agent 记录

- 本轮尝试启动 2 个只读复核子 Agent，均按规则使用 `gpt-5.3-codex-spark`：
  - 后端只读复核。
  - 前端只读复核。
- 两个子 Agent 均因 `gpt-5.3-codex-spark` 用量限制失败，已关闭。
- 本轮未将只读复核回退到实现模型，未让子 Agent 接管旁路业务。

## P2 遗留

- `react-ui/src/pages/Portal/Home/PortalSelfManagement.tsx` 当前超过 500 行。当前职责仍集中在 portal 最小自助管理闭环，快速推进模式下先不拆；后续如继续增加字段或交互，应拆到 `components` / `hooks`。
- Portal 自助管理界面没有做视觉优化和密集交互细调。
- 需要在执行 seed 后做一次 live seller/buyer portal 登录、直登消费、`getInfo`、`getRouters`、401 跳转和会话隔离验证。
- 本轮冻结的商品、订单、库存、物流、财务、履约、外部系统等业务菜单仍不计入完成口径。

## 下一步

1. 用户确认目标环境后，按 `docs/plans/2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md` 执行 `20260610_portal_self_management_permission_seed.sql` 或等价受控迁移，并记录执行环境、命令类型和影响范围。
2. 对 seller/buyer portal 做一次 live 最小闭环验证：账号密码登录、OWNER 创建子账号、角色授权、部门维护、日志和在线会话查看。
3. 若 live 验证发现权限模板缺口，只修本阶段 self-management 权限框架，不扩展业务菜单。

## 续跑加固：portal 角色菜单模板 fail-closed 校验

- 加固时间：2026-06-11。
- 加固原因：
  - 本阶段要求 seller/buyer 端内角色只能从平台预置的 `seller_menu` / `buyer_menu` 最小权限模板分配权限。
  - 已有 service 写入链路会校验端内菜单 ID 区间、权限前缀、component 和通配/admin 命名空间，但 portal `/roles/menus` 模板读取和 controller 分配预检仍主要按权限集合过滤。
  - 为避免脏模板被展示或参与角色分配，需要在 controller 层也执行本端菜单可读性校验。
- 已加固：
  - `SellerPortalController` / `BuyerPortalController` 在角色菜单分配预检中调用 `PortalPermissionSupport.assertReadableTerminalMenu(...)`，再检查是否属于本轮 self-management 权限集合。
  - `/roles/menus` 模板读取时先按 self-management 权限集合命中，再对命中的菜单执行本端 ID 区间、菜单类型、component、权限前缀、通配符和 admin 命名空间校验。
  - `PortalSelfServiceSurfaceContractTest` 同步固定该 controller 层 fail-closed 合同。
- 已验证：
  - 通过：`cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
  - 结果：`PortalSelfServiceSurfaceContractTest` 1 test passed；seller/buyer 模块随 reactor 编译通过；BUILD SUCCESS。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 296 tests passed；后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
  - 通过：`git diff --check -- <本轮 portal 菜单模板加固与记录文件>`。
  - 结果：无 whitespace error；仅 LF/CRLF 工作区提示。
  - 通过：`codegraph sync .`。
  - 结果：`Synced 3 changed files`，`Modified: 3 - 284 nodes in 1.2s`。
- 影响边界：
  - 未执行 SQL seed。
  - 未执行 live portal 登录、直登或写验证。
  - 未写 MySQL、Redis 或业务数据。
  - 未扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 文件末尾当前状态：seed 已完成，live 验证待确认

- 收口时间：2026-06-12 02:05 +08:00。
- 本文件中较早段落保留的 `未执行 SQL seed`、`7 / 19`、`245 = 35 * 7`、`权限模板未落库`、`下一步执行 seed` 等内容均为历史快照。
- 当前权威状态：
  - 目标库 `fenxiao` 已执行本阶段 self-management 权限 seed。
  - seller/buyer self-management 权限模板均为 `19 / 19`。
  - seller/buyer root button 权限模板均为 `18 / 18`。
  - seller/buyer OWNER self-management 授权均为 `665 = 35 * 19`。
  - seller/buyer OWNER 非 self-management 授权均为 0。
  - 无无效权限、菜单 ID 区间、页面 component 或重复 perms 问题。
- live 脚本缺确认变量 fail-closed 验证：
  - `verify-portal-self-management-live.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY`。
  - `verify-portal-self-management-live-write.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY`。
  - `verify-portal-direct-login-live.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`。
  - 上述验证未提供账号、token、主体 ID 或确认变量，未执行真实 portal 登录、direct-login、写闭环或清理动作。
- 当前仍未完成：
  - seller/buyer OWNER 账号密码真实登录。
  - 管理端 direct-login 票据签发、目标 portal 一次性消费和成功回传。
  - 真实 token 下的 `/getInfo` / `/getRouters`、401 跳转、token/session、日志审计隔离。
  - OWNER 在真实 portal 内创建子账号、设置角色、维护部门、查看登录日志、操作日志和在线会话。
- 当前下一步：
  - 需要用户明确确认 live 写入范围和 seller/buyer OWNER 凭据后，才可运行 `verify:portal-self-management-live`、`verify:portal-direct-login-live`、`verify:portal-self-management-live-write`。
  - live 验证会写登录日志、操作日志、direct-login 票据、portal session，并会留下停用 STAFF 测试账号；未确认前继续保持 fail-closed。
## 2026-06-12 02:18 portal 自助审计 DTO 序列化门禁复核

- 加固原因：
  - 端内自助登录日志、操作日志和在线会话接口必须只返回 portal 可见 DTO，不能把管理审计字段、主体作用域字段或 token/session 内部字段序列化给 seller/buyer portal。
  - `PortalSelfAuditSerializationTest` 之前只覆盖登录日志和操作日志；在线会话虽然在 `PortalHomeProfileSerializationTest` 有基础覆盖，但自助审计合同未把 `PortalOwnSessionProfile` 纳入同一边界。
- 已加固：
  - 重建 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSelfAuditSerializationTest.java` 的稳定 ASCII 断言，移除历史乱码文案。
  - 新增 `ownSessionMustNotSerializeInternalAuditScope`，固定 `PortalOwnSessionProfile` 不序列化 `terminal`、`subjectId`、`accountId`、`tokenId`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`、`directLogin`。
  - 登录日志和操作日志断言改为按 JSON 字段名精确检查，避免普通文本命中造成误判。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=PortalSelfAuditSerializationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests passed。
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfAuditSerializationTest,PortalSelfServiceSurfaceContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，130 tests passed，BUILD SUCCESS。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- 影响边界：
  - 本次只更新测试合同，不执行 live portal 登录、direct-login 或 self-management 写闭环。
  - 未写 MySQL、Redis 或业务数据。
  - 不扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 2026-06-12 02:23 portal 权限拒绝日志 direct-login 审计门禁

- 加固原因：
  - `PortalLogAspect` 已固定普通 portal 操作日志的 direct-login 结构化审计字段，但 `PortalPreAuthorizeAspect` 的权限拒绝路径同样会写 `PortalOperLog`。
  - 本阶段要求权限校验、日志审计均按 seller/buyer 端隔离；如果 403 拒绝日志丢失 `directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`，管理端免密代入失败审计会不完整。
- 已加固：
  - `PortalLogAspectContractTest` 新增 `portalPreAuthorizeAspectMustKeepDirectLoginAuditOnAuthorizationFailures`。
  - 合同固定 `PortalPreAuthorizeAspect` 在权限拒绝时先调用 `recordAuthorizationFailure(...)`，从当前 terminal token/session 解析会话，并调用 `applyDirectLoginAudit(operLog, session)`。
  - 合同固定拒绝日志仍写入 `directLogin`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason` 结构化字段，并保留 `directLoginAudit{ticketId=...}` 兼容前缀。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-framework -am "-Dtest=PortalLogAspectContractTest,PortalPreAuthorizeAspectTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，5 tests passed，BUILD SUCCESS。
- 影响边界：
  - 本次只更新测试合同，不执行 live portal 登录、direct-login 或 self-management 写闭环。
  - 未写 MySQL、Redis 或业务数据。
  - 不扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 2026-06-12 02:29 portal redirect 白名单与完整门禁复核

- 加固原因：
  - portal 登录页 redirect 必须只接受当前端相对路径白名单：`/{terminal}/login`、`/{terminal}/direct-login` 和 `/{terminal}/portal/**`。
  - `/seller`、`/buyer`、`/seller/*`、`/buyer/*` 管理端路径以及绝对 URL、协议相对 URL 不得被识别为 portal route，避免 401 跳转或登录后 redirect 串端。
- 已加固：
  - `react-ui/tests/portal-session-request.test.ts` 补充绝对 URL 与协议相对 URL 负向断言。
  - 当前 `portalPaths` helper 继续只按相对端内路径判定 portal route，未改变运行时代码。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-session-request.test.ts --runInBand`：通过，1 suite / 57 tests passed。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过，`three-terminal manifest check passed.`。
  - `cd react-ui; npm run verify:three-terminal`：通过，前端 33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- 当前状态：
  - `fenxiao` self-management SQL seed 已执行并通过 postcheck，seller/buyer self-management 模板均为 `19 / 19`，root button 模板均为 `18 / 18`，OWNER self-management 授权均为 `665 = 35 * 19`。
  - 仍未执行真实 portal 账号密码登录、direct-login 或 self-management 写闭环；后续真实 live 仍需显式确认写入登录日志、操作日志、direct-login 票据、portal session 和测试账号/角色/部门数据。
- 影响边界：
  - 本次只更新前端静态合同和记录，不写 MySQL、Redis 或业务数据。
  - 不执行浏览器截图、DOM/UI 细调，也不扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 2026-06-12 02:31 当前收口状态

- 只读复核：
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，`portal self-management SQL precheck completed without writes.`。
  - 当前库仍为 `fenxiao`；`seller_menu` 23 行，ID `100008-100031`；`buyer_menu` 23 行，ID `200003-200026`。
  - active seller/buyer OWNER 角色各 35 个；seller/buyer self-management 模板均为 `19 / 19`；root button 模板均为 `18 / 18`。
  - seller/buyer OWNER self-management 授权均为 `665 = 35 * 19`；OWNER 非 self-management 授权均为 0。
  - seller/buyer 无效权限、菜单 ID 区间违规、页面 component 违规、重复 perms 均为 0。
- 收尾检查：
  - `git diff --check -- <本轮 redirect 合同、审计合同和记录文件>`：通过，仅 LF/CRLF 工作区提示。
  - `codegraph sync .`：首次同步通过，`Synced 1 changed files`，`Modified: 1 - 19 nodes`；记录补写后最终复跑为 `Already up to date`。
- 当前未完成项：
  - 真实 seller/buyer portal 账号密码登录、管理端 direct-login 票据签发与目标 portal 一次性消费、真实 `/getInfo` / `/getRouters`、401 跳转、日志/会话隔离和 OWNER 自助写闭环仍未执行。
  - 这些 live 验证会写登录日志、操作日志、direct-login 票据、portal session 和测试账号/角色/部门数据；未获进一步确认前继续 fail-closed。

## 2026-06-12 02:39 管理端角色菜单树 self-management 收口

- 加固原因：
  - 本阶段 seller/buyer 端内角色只能从平台预置的最小 self-management 模板分配权限。
  - 管理端 `roleMenuTreeselect` 此前虽然写入链路会校验端内菜单 ID、权限前缀、component、通配符和 admin 命名空间，但读取给角色分配 UI 的菜单树仍是 `buildMenuTreeSelect(new PortalMenu())` 全量端菜单。
  - 若历史库保留冻结业务菜单模板，管理端角色分配入口会展示非本阶段权限模板，形成误导并可能把错误菜单提交到后端失败路径。
- 已加固：
  - `ISellerPortalPermissionService` / `IBuyerPortalPermissionService` 新增 `buildSelfManagementMenuTreeSelect()` 和 `selectSelfManagementMenuIdsByRoleId(...)`。
  - `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 复用本端 `PORTAL_SELF_MANAGEMENT_PERMS`，并对候选菜单执行 `PortalPermissionSupport.assertReadableTerminalMenu(...)` 后再进入菜单树。
  - `AdminSellerMenuController` / `AdminBuyerMenuController` 的 `roleMenuTreeselect` 改为只返回 self-management 菜单树和 self-management checkedKeys。
  - `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` 固定上述合同，禁止回退为全量端菜单树。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，9 tests passed，BUILD SUCCESS。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认 `fenxiao` 仍为 seller/buyer self-management `19 / 19`、OWNER grants `665 = 35 * 19`，无跨端、无效权限、ID 区间、component 或重复 perms 问题。
  - `cd react-ui; npm run verify:three-terminal`：通过，前端 33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `git diff --check -- <本轮管理端角色菜单树收口文件>`：通过，仅 LF/CRLF 工作区提示。
  - `codegraph sync .`：通过，`Synced 8 changed files`，`Modified: 8 - 457 nodes`。
- 影响边界：
  - 本次只修管理端角色菜单树的权限模板读取边界，不执行 live portal 登录、direct-login 或 self-management 写闭环。
  - 未写 MySQL、Redis 或业务数据；不扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 2026-06-12 02:46 管理端角色写入 self-management fail-closed 收口

- 加固原因：
  - 管理端角色菜单树已经收敛为 self-management 模板，但 `insertRole` / `updateRole` 写入侧此前只校验端内菜单 ID、component、权限前缀、通配符和 admin 命名空间。
  - 如果历史库保留合法端内业务按钮权限，例如 `seller:product:list` / `buyer:product:list`，直接调用角色保存接口仍可能把冻结业务权限写入端内角色。
- 已加固：
  - `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 在角色菜单写入预检中新增 `assertRoleMenuSelfManagement(menu)`。
  - 写入侧先确认 menuId 属于当前端并通过 `PortalPermissionSupport` 校验，再强制 `menu.getPerms()` 必须命中本端 `PORTAL_SELF_MANAGEMENT_PERMS`；否则在写 `seller_role_menu` / `buyer_role_menu` 前抛 `ServiceException`。
  - seller/buyer 权限 Service 单测各新增 insert/update 非 self-management 合法端菜单拒绝用例，确认不会执行角色保存、清空绑定或批量写 role-menu。
  - `PortalSelfServiceSurfaceContractTest` 固定写入侧必须调用 `assertRoleMenuSelfManagement(menu)`，禁止后续只保留读取侧过滤。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,PortalSelfServiceSurfaceContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`ruoyi-system` 9 tests、`seller` 25 tests、`buyer` 25 tests，BUILD SUCCESS。
  - `cd react-ui; npm run verify:three-terminal`：通过，前端 33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认当前库 `fenxiao`，seller/buyer self-management 模板均为 `19 / 19`，root button 模板均为 `18 / 18`，OWNER self-management 授权均为 `665 = 35 * 19`，OWNER 非 self-management 授权均为 0，无无效权限、ID 区间、component 或重复 perms 问题。
  - `git diff --check -- <本轮角色写入 fail-closed 文件>`：通过，仅 LF/CRLF 工作区提示。
- 影响边界：
  - 本次只修管理端角色写入权限模板边界，不执行 live portal 账号密码登录、direct-login 或 self-management 写闭环。
  - 本次 SQL 只做 `--precheck` 只读查询；未写 MySQL、Redis 或业务数据。
  - 不扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。

## 2026-06-12 02:52 live-write 角色菜单负向门禁补强

- 加固原因：
  - 后端 seller/buyer 角色写入侧已补 `assertRoleMenuSelfManagement(menu)`，但 live-write verifier 此前只验证 `/roles/menus` 模板数量、ID 区间和正常角色写入。
  - 为了让后续真实 live 写闭环能证明“角色保存接口本身拒绝跨端 menuId”，需要在 verifier 中加入负向写入验证。
- 已加固：
  - `react-ui/scripts/verify-portal-self-management-live-write.mjs` 新增 `crossTerminalMenuIdFor(...)`、`assertRoleCreateRejectsInvalidMenuIds(...)`、`assertRoleUpdateRejectsInvalidMenuIds(...)`。
  - live-write 在创建正式测试角色前，会尝试用 self-management menuIds 追加跨端 menuId 创建角色；若接口成功或失败后仍落库，直接报错。
  - live-write 在正式测试角色创建后，会尝试用跨端 menuId 更新角色；若接口成功或失败后污染 checkedKeys，直接报错。
  - `react-ui/tests/portal-self-management-live-write-contract.test.ts` 固定上述负向门禁，防止后续 verifier 只验证正向写入。
- 已验证：
  - `cd react-ui; node --check scripts\verify-portal-self-management-live-write.mjs`：通过。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-write-contract.test.ts --runInBand`：通过，1 suite / 7 tests passed。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; node scripts\verify-portal-self-management-live-write.mjs --help`：通过。
  - `git diff --check -- react-ui/scripts/verify-portal-self-management-live-write.mjs react-ui/tests/portal-self-management-live-write-contract.test.ts`：通过，仅 LF/CRLF 工作区提示。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- 影响边界：
  - 本次只更新 live-write verifier 和静态合同，不执行真实 portal 登录或写闭环。
  - 未写 MySQL、Redis 或业务数据；不扩展商品、订单、库存、物流、财务、履约或外部系统业务菜单。
