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
  - 新增 `react-ui/scripts/verify-portal-direct-login-live.mjs`：
    - 需要外部显式提供 `ADMIN_AUTH_TOKEN`、`SELLER_DIRECT_LOGIN_SUBJECT_ID`、`BUYER_DIRECT_LOGIN_SUBJECT_ID`。
    - 需要显式 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`。
    - 每端只生成并消费一次管理端到 OWNER 的 direct-login ticket，随后用 portal token 调用 `/getInfo` 校验 19 个 self-management 权限，并 best-effort logout。
    - 不读取 `.env.local`，不执行 SQL，不触达商品、订单、库存、物流、财务、履约或外部系统业务 API。
  - `react-ui/package.json` 新增 `verify:portal-direct-login-live`。
  - `react-ui/tests/portal-direct-login-live-contract.test.ts` 新增合同，并纳入 `react-ui/tests/three-terminal.manifest.json` 的 frontend/critical frontend 门禁。
- 已验证：
  - 通过：`cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts tests/portal-self-management-live-contract.test.ts tests/portal-self-management-live-write-contract.test.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand`。
  - 结果：4 suites / 21 tests passed。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live.mjs --help`。
  - 通过：`cd react-ui; node scripts\verify-portal-self-management-live-write.mjs --help`。
  - 通过：`cd react-ui; node scripts\verify-portal-direct-login-live.mjs --help`。
  - 通过：`cd react-ui; npm run verify:three-terminal`。
  - 结果：`three-terminal verification passed.`；前端 33 suites / 284 tests passed；后端 reactor `test-compile` 与三端合同均 BUILD SUCCESS。
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
