# 三端独立改造目标追踪

## 2026-06-06 免密 Redis 明文 Token 收敛检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：管理端免密代入的一次性明文 token 仍可返回给管理端用于短时直登链接，但 Redis 侧不再用明文 token 做 key，也不在 payload 中保存明文 token。本轮不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- `PortalDirectLoginSupport` 继续生成明文一次性 token 并返回给管理端 `PortalDirectLoginResult`，保持 direct-login 链接契约不变。
- Redis key 从 `portal_direct_login:{token}` 改为 `portal_direct_login:{token_hash}`。
- `PortalDirectLoginToken` 删除 `token` 字段，Redis payload 不再保存明文 token。
- `consumeToken(...)` 先对请求 token 做 hash，再用 hash 查询审计票据和 Redis payload。
- `PortalDirectLoginSupportTest` 更新为固定 DB `token_hash`、Redis hash key、payload 无 token 字段、30 分钟 TTL、一次性消费、跨端拒绝、过期删除和 validator 失败不消费票据。
- seller/buyer service 测试假件同步删除 `PortalDirectLoginToken#setToken(...)`。
- 更新 `docs/architecture/reuse-ledger.md`，登记免密 Redis 明文 token 收敛规则。
- 新增执行记录：`docs/plans/2026-06-06-direct-login-redis-token-hash-hardening-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，support `Tests run: 6`、seller `Tests run: 28`、buyer `Tests run: 28`，均无失败。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Already up to date`。

当前判断：

- 免密代入的明文 token 仍只作为一次性链接参数存在；后端 Redis 存储面不再保留明文 token key 或 payload 字段。
- 本轮没有新增表或字段，没有改远程数据，也没有改变 seller/buyer 端 direct-login 入口参数。
- `PortalDirectLoginSupportTest` 当前 510 行，触发 500 行判断阈值；该文件职责仍集中在免密票据生成、消费、Redis payload、状态机和原因校验，暂不拆分，后续若继续增加免密场景再拆成 create/consume 两类测试。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 为既有大文件，本轮只删除测试假件中的旧 token 字段设置，不扩大职责。
- 本轮未执行 SQL，未连接远程 MySQL / Redis，未修改远程数据。

## 2026-06-06 端内操作日志写入路由测试检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：端内 `@PortalLog` 生成的操作日志必须通过 `PortalOperLogServiceImpl` 按 terminal 写入 seller/buyer 各自端内操作日志表，不能回落到若依管理端 `sys_oper_log`。本轮不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- 新增 `PortalOperLogServiceImplTest`，固定 seller terminal 只调用 `insertSellerOperLog(...)`，不调用 buyer 写入。
- 同一测试按 buyer 模板固定 buyer terminal 只调用 `insertBuyerOperLog(...)`，不调用 seller 写入。
- 新增未知 terminal 负向用例：非 seller/buyer terminal 必须抛出 `ServiceException`，且不写任何端内操作日志。
- 新增 service 依赖负向守卫：`PortalOperLogServiceImpl` 不得持有 `ISysOperLogService` 或 `SysOperLog` 相关依赖。
- 更新 `docs/architecture/reuse-ledger.md`，登记 `PortalOperLogService` 端内写入路由复用规则。
- 新增执行记录：`docs/plans/2026-06-06-portal-oper-log-service-routing-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=PortalOperLogServiceImplTest,TerminalRouteOwnershipTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 同步 `1 changed files`。

当前判断：

- 端内操作日志第一批写入链路现在不只依赖 `@PortalLog` 静态模板，也有 service 层 terminal 路由测试守住 seller/buyer 写入目标。
- 本轮只补测试证据和记录，没有改变现有运行时行为。
- `PortalOperLogServiceImplTest` 当前 151 行，未触发 300 行判断阈值。
- 本轮未执行 SQL，未连接远程 MySQL / Redis，未修改远程数据。

## 2026-06-06 端内商品 Service Session 守卫补强检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：补强 seller/buyer 端商城商品 service 的 session fail-closed 自动化守卫。本轮不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- `SellerPortalProductServiceImplTest` 新增列表入口负向用例：非 seller session 必须先返回“登录状态已失效”，不得调用共享 product service 查询商品。
- `SellerPortalProductServiceImplTest` 新增详情和 SKU 入口负向用例：非 seller session 必须先拒绝，且 `selectProductById(...)` / `selectSkuList(...)` 不得被调用。
- `BuyerPortalProductServiceImplTest` 在已有列表负向用例基础上，新增详情和 SKU 入口负向用例：非 buyer session 必须先拒绝，且 `selectOnSaleProductById(...)` / `selectOnSaleSkuList(...)` 不得被调用。
- seller/buyer 测试 fake 同步当前 `IProductDistributionService.batchUpdateSpuStatus(List<Long>, String, boolean)` 签名，避免接口变更后测试编译漂移。
- 新增执行记录：`docs/plans/2026-06-06-portal-product-service-session-guard-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内商品 service 的 session fail-closed 复用规则。

验证结果：

- 首次 seller 定向测试失败，原因是测试 fake 仍实现旧的 `batchUpdateSpuStatus(List<Long>, String)` 签名；已只修测试 fake，不改生产代码。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi; git diff --check -- <本轮触碰测试文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Already up to date`。

当前判断：

- 当前 seller/buyer 端商城商品 service 三个入口均有 service 层负向测试守住：端类型不匹配时先拒绝，不进入共享 product service。
- 本轮只补测试证据，没有改变现有运行时行为。
- seller/buyer 商品 service 测试文件当前分别为 395 行和 425 行，触发 300/400 行判断阈值；职责仍集中在端内商品 service 范围控制、可见性和 DTO 脱敏，暂不拆分。
- 本轮未执行 SQL，未连接远程 MySQL / Redis，未修改远程数据。

## 2026-06-06 端内权限菜单树测试补强检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：补强 seller/buyer 端 `selectPortalMenuTree(...)` 的 service 层自动化守卫。本轮不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- 新增 `SellerPortalPermissionServiceImplMenuTreeTest`，从 PortalAccess 测试中迁入 seller 离线菜单树拒绝用例。
- 按 seller 标准模板新增在线菜单树正向用例：必须先校验 DB session 在线，再用当前 `sellerId/accountId` 查询 `selectSellerAccountMenuList(...)`，并返回 `PortalPermissionSupport.buildMenuTree(...)` 形成的父子树。
- 新增 `BuyerPortalPermissionServiceImplMenuTreeTest`，按 seller 菜单树模板同构复制，只替换 terminal、领域对象、mapper、service、token 和文案。
- `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest` 不再混入菜单树 fake，只保留权限信息和 `selectPermissions(...)` 访问测试。
- 新增执行记录：`docs/plans/2026-06-06-terminal-permission-service-menu-tree-test-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记菜单树测试分文件规则和 `selectPortalMenuTree(...)` 的端内 mapper 读取契约。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- 尾随空白检查通过：`rg -n "[ \t]+$" <本轮触碰测试文件>` 未发现匹配。
- `cd E:\Urili-Ruoyi; git diff --check -- <本轮触碰 tracked 测试文件>`：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- 菜单树读取已从“只覆盖离线拒绝”补强为“离线拒绝 + 在线返回父子树 + mapper 入参固定”。
- 权限 service 测试当前拆成三类职责：角色绑定测试各 321 行、portal access 测试各 456 行、menu tree 测试各 338 行。
- 本轮未执行 SQL，未连接远程 MySQL / Redis，未修改远程数据。

## 2026-06-06 端内权限 Service 测试拆分检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：把 seller/buyer 端 `PortalPermissionServiceImpl` 测试从 500 行以上的大文件拆成“角色绑定写操作”和“端内访问/权限读取”两个测试类。本轮不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- 新增 `SellerPortalPermissionServiceImplPortalAccessTest`，承载 seller 端 `selectPortalPermissionInfo(...)`、`selectPermissions(...)`、`selectPortalMenuTree(...)` 的会话守卫、在线 session 和权限信息返回测试。
- `SellerPortalPermissionServiceImplTest` 收缩为 4 个 `assignAccountRoles...` 角色绑定写操作测试。
- 新增 `BuyerPortalPermissionServiceImplPortalAccessTest`，按 seller 模板同构复制，只替换 terminal、领域对象、mapper、service、token 和权限 code。
- `BuyerPortalPermissionServiceImplTest` 收缩为 4 个 `assignAccountRoles...` 角色绑定写操作测试。
- 新增执行记录：`docs/plans/2026-06-06-terminal-permission-service-test-split-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记权限 service 测试结构规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi; git diff --check -- <本轮触碰 tracked 文件>`：通过，仅有 LF/CRLF 工作区换行提示；新增未跟踪文件另用行尾空白检查覆盖。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；测试拆分后输出过 `Synced 2 changed files`，复用台账修正后最近一次输出 `Synced 1 changed files`。

当前判断：

- 权限 service 测试已从单个 529 行文件拆成角色绑定测试 288 行、portal access 测试 423 行。
- seller/buyer 原有 10 个测试行为均保留，只改变测试文件归属。
- 本轮未执行 SQL，未连接远程 MySQL / Redis，未修改远程数据。

## 2026-06-05 端内权限 Service 会话 fail-closed 守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：seller/buyer 端 `@PortalPreAuthorize` 背后的权限 service 必须在 session 形态异常或 DB session 缺失/失效时 fail-closed，并且在线 session 的权限信息必须从各自端内权限 mapper 返回。本轮不新增接口、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- `SellerPortalPermissionServiceImpl.assertActiveSellerSession(...)` 增加 session 形态守卫：`session` 非空、terminal 必须为 `seller`、`subjectId` / `accountId` 必须存在、`tokenId` 必须非空白；不满足时返回 `401` 登录失效。
- `SellerPortalPermissionServiceImplTest` 按卖家标准模板补充畸形 session、空白 token、DB session 离线、DB session 入参和账号锁定的权限 service 级守卫；畸形 session 用“查了就失败”的 fake 固定 BeforeLookup 契约，并覆盖 `selectPortalPermissionInfo(...)`、`selectPermissions(...)`、`selectPortalMenuTree(...)` 三个 public 入口。
- `SellerPortalPermissionServiceImplTest` 按卖家标准模板补充 `selectPortalPermissionInfo(...)` 正向返回测试：在线 session 返回 terminal、主体、账号、用户名、昵称、角色和权限；角色和权限读取入参必须是当前 seller/account，权限字符串支持逗号拆分。
- `BuyerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImplTest` 按卖家模板同构复制，只替换 terminal、领域对象、mapper、service 和文案。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内权限 service session fail-closed 复用规则。
- 新增执行记录：`docs/plans/2026-06-05-terminal-permission-service-session-shape-guard-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi; git diff --check -- <本轮触碰文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；本检查点先后输出 `Synced 5 changed files`、`Synced 2 changed files`、`Synced 3 changed files`。

当前判断：

- seller/buyer 端权限 service 现在同时守住三层边界：session 形态异常先拒绝、主体/账号状态异常拒绝、DB session 不在线拒绝。
- `selectPortalPermissionInfo(...)`、`selectPermissions(...)`、`selectPortalMenuTree(...)` 三个权限 service public 入口均已由测试固定复用同一 fail-closed 守卫。
- `selectPortalPermissionInfo(...)` 正向返回已经按卖家标准模板验收，并同构复制到买家；后续端内 `getInfo` 相关改动应沿用这套 fake mapper 与入参断言。
- `PortalTokenSupport.requireSession(...)` 的 Redis/JWT 校验仍是入口守卫，权限 service 的 DB session 校验是运行时兜底；两者重复但不冲突。
- 本轮未执行 SQL，未连接远程 MySQL / Redis，未修改远程数据。

## 2026-06-05 端内 Portal 后台上下文回退守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：seller/buyer 端受 `@PortalPreAuthorize` 保护的端内接口，不能在缺失端内上下文时回退到若依后台 `SecurityUtils` / `LoginUser` / `SysUser` / `@ss` 权限上下文。本轮不新增接口、不改 service 业务逻辑、不复制 buyer 前端、不执行远程数据库 DDL/DML、不连接远程 MySQL / Redis。

已完成：

- `TerminalRouteOwnershipTest` 新增 seller/buyer 受保护 portal handler 后台上下文回退守卫。
- 新增断言：受保护 portal handler 方法体不得使用 `SecurityUtils.getLoginUser/getUserId/getUsername`、裸调用 `getLoginUser/getUserId/getUsername`、`LoginUser` 或 `SysUser` 推导端内身份。
- 继续保留原有守卫：端内 handler 必须方法级 `@Anonymous`、`@PortalPreAuthorize`、`@PortalLog`、`PortalSessionContext.requireSession(...)`，且方法签名不得接收客户端身份范围参数。
- 6 个子 agent 已完成只读审计并关闭；seller/buyer controller 未发现后台登录上下文回退，service 审计确认当前后台 `SecurityUtils.getUsername()` 用于管理端控制面审计字段，暂不属于本切片违规。
- 新增执行记录：`docs/plans/2026-06-05-terminal-portal-admin-context-guard-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内 Controller 鉴权模板和 `PortalSessionContext` 复用规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest" test`：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- UI 子 agent 只读审计中运行 `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过，`Partner management template guard passed.`。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-terminal-portal-admin-context-guard-record.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- 当前受保护 seller/buyer portal handler 已由测试同时守住三层身份边界：必须从 `PortalSessionContext` 派生身份、不能接收客户端身份范围参数、不能回退若依后台登录上下文。
- 管理端 `/seller/admin/**`、`/buyer/admin/**` 继续使用若依后台 `@PreAuthorize("@ss.hasPermi(...)")` 和后台登录上下文；本轮守卫不限制管理端控制面。
- 本轮未执行 SQL，未连接远程 MySQL / Redis，未修改远程数据。

## 2026-06-05 远程库三端独立结构只读核验检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，只做远程 MySQL 当前结构的只读核验。本轮不执行 DDL/DML，不修改数据，不读取账号密码明文或业务内容。

数据源确认：

- 激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 当前 `active: druid`。
- JDBC 来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`，目标库为远程 MySQL `fenxiao`。
- Redis 配置：`application.yml` 指向远程 Redis。
- 连接凭证来源：本机 `.env.local` 的 `RUOYI_DB_*`，未在命令输出或记录中写入明文。
- 本机 `mysql` CLI 不存在，本轮使用 Maven 本地缓存的 `mysql-connector-j` 和 `jshell` 进行 JDBC 只读查询。

已核验：

- 三端独立核心表存在：`seller`、`buyer`、`seller_account`、`buyer_account`、`seller_role`、`buyer_role`、`seller_menu`、`buyer_menu`、`seller_dept`、`buyer_dept`、`seller_account_role`、`buyer_account_role`、`seller_role_menu`、`buyer_role_menu`、`seller_login_log`、`buyer_login_log`、`seller_oper_log`、`buyer_oper_log`、`seller_session`、`buyer_session`、`portal_direct_login_ticket`，共 `21/21`。
- `seller_account.user_id` 不存在，`buyer_account.user_id` 不存在。
- `seller_account` / `buyer_account` 的端账号关键字段存在：端账号 ID、主体 ID、`user_name`、`password`、`account_role`、`status`、`lock_status`。
- `seller_login_log` / `buyer_login_log`、`seller_oper_log` / `buyer_oper_log` 的端主体 ID、端账号 ID 和时间字段存在。
- `seller_session` / `buyer_session` 的端主体 ID、端账号 ID、账号名、过期时间和状态字段存在。
- 关键索引存在：端账号用户名唯一索引、OWNER 唯一索引、端会话账号索引。

只读查询结果摘要：

- `coreTablesPresent=21/21 missing=[]`
- `forbiddenColumn seller_account.user_id count=0`
- `forbiddenColumn buyer_account.user_id count=0`
- `requiredColumns seller_account missing=[]`
- `requiredColumns buyer_account missing=[]`
- `requiredColumns seller_login_log missing=[]`
- `requiredColumns buyer_login_log missing=[]`
- `requiredColumns seller_oper_log missing=[]`
- `requiredColumns buyer_oper_log missing=[]`
- `requiredColumns seller_session missing=[]`
- `requiredColumns buyer_session missing=[]`
- `index seller_account.uk_seller_account_username count=1`
- `index buyer_account.uk_buyer_account_username count=1`
- `index seller_account.uk_seller_account_owner count=1`
- `index buyer_account.uk_buyer_account_owner count=1`
- `index seller_session.idx_seller_session_account count=1`
- `index buyer_session.idx_buyer_session_account count=1`

新增执行记录：

- `docs/plans/2026-06-05-remote-terminal-schema-readonly-verification-record.md`

当前判断：

- 当前远程 MySQL 的三端账号、权限、日志、会话核心结构与三端独立方向一致。
- 本轮只证明当前结构存在与关键字段/索引满足要求，不证明所有端内业务权限、页面按钮和运行时接口已经全部覆盖；这些仍以对应接口测试、guard 和 smoke 记录为准。
- 本轮未执行 DDL/DML，未修改远程数据。

## 2026-06-05 legacy sys_user 账号回填隔离检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家/买家端账号不再复用若依 `sys_user`”的目标推进。本轮只处理一类问题：旧三端隔离迁移 SQL 中端账号从 `sys_user` 回填的兼容块不再保留在当前主三端隔离基线中。本轮不执行远程数据库 DDL/DML、不连接远程 MySQL / Redis。

已完成：

- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` 移除 `migrate_seller_account_from_sys_user` / `migrate_buyer_account_from_sys_user` 过程和 `left join sys_user` 回填逻辑。
- 新增 `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql`，明确作为历史混用账号库的 legacy helper；仅当旧库仍存在 `seller_account.user_id` / `buyer_account.user_id` 指向 `sys_user` 时，在主迁移脚本之前按需执行。
- `TerminalSqlIsolationContractTest` 增加断言：当前主三端隔离迁移脚本不得再包含 legacy `sys_user` 账号回填过程或 `join sys_user`。
- 新增执行记录：`docs/plans/2026-06-05-terminal-legacy-sys-user-backfill-isolation-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记当前主三端隔离 SQL 与 legacy 回填脚本边界。

验证结果：

- `cd E:\Urili-Ruoyi; rg -n "migrate_(seller|buyer)_account_from_sys_user|join\s+sys_user|left\s+join\s+sys_user|sys_user" RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql`：命中只存在于 legacy helper 文件，主迁移脚本无命中。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalSqlIsolationContractTest test`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest" test`：通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。

当前判断：

- 当前主三端隔离迁移脚本不再从若依 `sys_user` 回填 seller/buyer 端账号，账号独立基线更清楚。
- legacy helper 只作为历史混用账号库迁出工具保留，不属于新环境或当前主三端隔离基线。
- 本轮未执行 SQL；远程 MySQL / Redis 未连接。

## 2026-06-05 端内日志 SQL 独立 DDL 守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“三端账号、权限、日志、会话独立”的目标推进。本轮只处理一类问题：旧三端隔离迁移 SQL 中 seller/buyer 登录日志、操作日志表不再通过 `LIKE sys_logininfor` / `LIKE sys_oper_log` 继承若依后台日志表结构。本轮不复制 buyer 前端、不改运行时 Java 业务逻辑、不执行远程数据库 DDL/DML、不连接远程 MySQL / Redis。

已完成：

- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` 中 `seller_login_log`、`buyer_login_log`、`seller_oper_log`、`buyer_oper_log` 改为显式独立 DDL。
- 对已有运行库表保持兼容：仍通过 `add_column_if_missing(...)` 补端内主体/账号字段，并通过 `add_index_if_missing(...)` 补端内主体/账号时间索引。
- 新增 `TerminalSqlIsolationContractTest`，扫描三端隔离迁移脚本和综合 seed，防止 seller/buyer 端内日志表再次使用 `LIKE sys_*` 模板生成。
- 新增执行记录：`docs/plans/2026-06-05-terminal-log-sql-explicit-ddl-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内日志 SQL 必须显式建表，不从若依 `sys_logininfor` / `sys_oper_log` 派生。

验证结果：

- `cd E:\Urili-Ruoyi; rg -n "create table if not exists (seller|buyer)_(login|oper)_log like sys_|like sys_logininfor|like sys_oper_log" RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql RuoYi-Vue/sql/seller_buyer_management_seed.sql`：无命中。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalSqlIsolationContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest" test`：通过，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。

当前判断：

- seller/buyer 端内登录日志和操作日志的 SQL 基线不再依赖若依后台日志表模板，三端日志独立性更清楚。
- 迁移脚本中的 `sys_user` 历史字段回填已在后续 `legacy sys_user 账号回填隔离检查点` 中拆出为独立 legacy helper；本检查点只负责端内日志 DDL 独立。
- 本轮未执行 SQL；远程 MySQL / Redis 未连接。

## 2026-06-05 卖家端商品 ProTable 标准模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按最新确认节奏执行：先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西。本轮只固定 seller portal “我的商城商品”主列表的标准 ProTable 模板，并解除阻塞该验证链路的 product 模块编译问题；不复制 buyer 前端、不执行数据库 DDL/DML、不连接远程 MySQL / Redis。

已完成：

- 6 个只读子 agent 并行审查 seller 后端、buyer 后端、seller 前端、buyer 前端、测试守卫和文档台账，结论用于收敛本切片边界。
- `ProductDistributionServiceImpl` 补齐已有商城商品状态/控制/价格操作日志代码缺失的批次号、状态校验和日志记录辅助方法，解除 `product` 模块编译阻塞。
- seller portal “我的商城商品”主列表从手写 `Card + Table + useEffect + pageNum/pageSize` 列表整理为标准 `ProTable`。
- seller 商品主列表统一复用 `getPersistedProTableSearch(...)`、`getProTablePagination(...)` 和 `getProTableScroll(...)`，并在 ProTable request 中固定 `current -> pageNum`、`pageSize -> pageSize` 映射。
- seller 商品查询参数继续通过端内 service 清理客户端身份范围字段，不让 `sellerId`、`subjectId`、`accountId` 或 `terminal` 决定端内数据范围。
- `react-ui/scripts/check-seller-portal-product-template.mjs` 增加 ProTable、统一筛选、统一分页、统一滚动和若依分页映射的静态契约断言。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller 商品主列表 ProTable 模板和 buyer 后续复制边界。
- 新增执行记录：
  - `docs/plans/2026-06-05-product-distribution-operation-log-compile-unblock-record.md`
  - `docs/plans/2026-06-05-seller-portal-product-protable-template-record.md`

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-DskipTests" compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest,PortalTokenSupportTest,PortalSessionProfileTest,PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest,SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,PermissionServiceAccountPermissionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`ruoyi-system Tests run: 24`、`ruoyi-framework Tests run: 7`、`seller Tests run: 39`、`buyer Tests run: 41`，均无失败。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过，确认本切片未破坏 buyer 既有模板。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Portal/Home/SellerOwnDistributionProductList.tsx scripts/check-seller-portal-product-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run lint`：未通过，失败点为既有 Biome lint 问题，集中在 `DictTag`、`RightContent`、`utils/tree`、`IconSelector`、`Monitor` 等非本切片修改文件；本切片修改文件的定向 lint 已通过。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-portal-product-ui-smoke.ps1 -AdminPassword 'admin123' -ScreenshotPath 'output/playwright/seller-portal-product-protable-smoke.png' -TimeoutMs 60000`：通过，真实进入 seller portal，验证 seller token 隔离、“我的商城商品”列表、详情弹窗、字段脱敏和退出清理；截图保存到 `output/playwright/seller-portal-product-protable-smoke.png`。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 同步 `9 changed files`。

当前判断：

- seller portal 商品主列表已经形成当前可验收的标准 ProTable 模板，且已通过真实浏览器 smoke；本检查点当时未复制 buyer 前端 ProTable 差量。
- 后续已通过 `docs/plans/2026-06-05-buyer-portal-product-protable-copy-record.md` 按 buyer 浏览口径完成 ProTable 差量复制；顶部当前状态以“双端 ProTable 标准模板已完成”为准。
- product 模块本轮只完成已有操作日志链路的编译阻塞修复，没有执行 SQL；远程 MySQL / Redis 未连接。
- 子 agent 提醒的后续项已纳入边界：buyer schema 组件可后续中性化复用；商品日志 SQL 中历史 `sys_user` / `sys_log` 类命名已由后续 SQL 隔离检查点继续收敛。

## 2026-06-05 免密票据目标账号失效守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只补 direct-login 票据指向的端账号生命周期守卫测试，不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- `SellerServiceImplTest` 先作为标准模板新增 2 个用例：票据指向账号已不存在时拒绝登录、票据指向账号已被改绑到其他卖家时拒绝登录。
- seller 模板验证通过后，`BuyerServiceImplTest` 按同一结构复制 2 个用例，只替换 terminal、编号、实体和 service。
- 两端新增用例均断言失败时不创建端登录 session、不创建端 token，并写入失败登录日志；账号不存在或归属变化时失败日志保留目标主体 ID，账号 ID 为空。
- 新增执行记录：`docs/plans/2026-06-05-direct-login-target-account-validation-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记免密票据消费前还必须校验目标账号存在且仍属于票据主体。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test`：通过，support `Tests run: 6`，seller `Tests run: 28`，buyer `Tests run: 28`，均无失败。

当前判断：

- 管理端签发的免密票据如果目标端账号后续被删除、迁移或错绑，不会生成 seller/buyer 端登录会话。
- 该切片没有改变票据状态机；真正“不消耗票据”的行为仍由前序 `PortalDirectLoginSupportTest` 的 validator 失败测试守住，本轮补齐 seller/buyer service 对目标账号缺失和归属变化的业务输入。
- 本轮未执行 SQL；远程 MySQL / Redis 未连接。

## 2026-06-05 directLogin 权限矩阵补强检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“每个切片只改一类东西”的节奏推进。本轮只补若依运行时 `PermissionService` 权限矩阵，不改接口、不改 UI、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- `PermissionServiceAccountPermissionTest` 增加 `seller:admin:directLogin` / `seller:admin:ticket:list` 与 buyer 对应权限的运行时矩阵断言。
- 证明只有主体权限或端内角色权限时，不能访问账号域权限、强制踢出、免密代入和免密票据。
- 证明只有账号 reset/lock/forceLogout 权限时，不能访问 directLogin 或 ticket 列表。
- 证明只有 directLogin/ticket 权限时，不能访问账号 reset/lock 或 forceLogout，也不能串到 buyer 端。
- 超管通配权限仍能访问 seller/buyer 账号、强踢、免密代入和免密票据权限。
- 新增执行记录：`docs/plans/2026-06-05-direct-login-permission-matrix-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 directLogin/ticket 已进入 `PermissionServiceAccountPermissionTest` 运行时权限矩阵。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest test`：通过，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest clean test`：通过，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，确认 clean 后重新编译。

当前判断：

- 管理端免密代入和免密票据现在同时有 controller/UI/seed 静态契约，以及若依运行时 `@ss.hasPermi(...)` 权限矩阵兜底。
- 本轮没有做真实低权限 HTTP / 浏览器 smoke；seller/buyer 的 directLogin 低权限真实验收已有前序记录，本轮只固化运行时权限服务矩阵。
- 本轮未执行 SQL；远程 MySQL / Redis 未连接。

## 2026-06-05 免密票据消费前状态校验检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：管理端已生成的 seller/buyer 免密票据，在目标主体或端账号后续被停用/锁定后，不能被消费为 `USED`。本轮不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- `PortalDirectLoginSupport` 增加 `consumeToken(portalType, token, validator)` 重载；validator 在票据标记 `USED` 和 Redis payload 删除之前执行。
- `PortalDirectLoginSupportTest` 增加 validator 失败测试，固定失败时不 mark used、不 mark expired、不删除 Redis payload。
- `SellerServiceImpl` 的 `directLoginSeller(...)` 接入消费前 validator，按 token 中的 subject/account 重新读取当前 seller 与 seller_account；主体停用、账号停用、账号锁定或目标账号不存在时，在消费票据前拒绝。
- `SellerServiceImplTest` 先作为标准模板补齐免密票据消费时当前状态复验：正常可登录、主体停用拒绝、账号停用拒绝、账号锁定拒绝。
- `BuyerServiceImpl` / `BuyerServiceImplTest` 按 seller 模板同构复制，只替换 buyer 字段、service 和 terminal。
- 新增执行记录：`docs/plans/2026-06-05-direct-login-pre-consume-validation-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记免密票据消费前状态校验复用规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，support `Tests run: 6`，seller `Tests run: 26`，buyer `Tests run: 26`，均无失败。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test`：通过，确认 clean 后重新编译。

当前判断：

- 免密票据现在不会因为目标主体/端账号已失效而被“先消费再拒绝登录”；无效状态会在 mark `USED` 前被 seller/buyer 当前状态校验拦截。
- 本轮未做 HTTP smoke，因为该切片主要固定 service/support 行为；真实端入口仍复用既有 seller/buyer direct-login controller。
- 本轮未执行 SQL；远程 MySQL / Redis 未连接。

## 2026-06-05 密码重置强制踢出端会话检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只处理一类问题：管理端重置 seller/buyer 端账号密码后，旧端会话不能继续使用。本轮不改前端、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- `SellerServiceImpl` 将自定义重置密码、恢复默认密码、重置主体主账号密码三个入口纳入事务；密码更新成功后强制踢出目标 seller 端账号会话，并删除 seller terminal Redis token。
- `SellerServiceImplTest` 先作为标准模板补齐三类入口的 service 单测，覆盖密码写入 BCrypt 密文、只踢目标端账号会话、Redis token 删除 terminal 为 `seller`。
- `BuyerServiceImpl` 按 seller 模板同构复制，只替换 buyer 字段、service 和 terminal。
- `BuyerServiceImplTest` 按 seller 模板补齐三类入口的 service 单测，覆盖 terminal 为 `buyer`。
- 新增执行记录：`docs/plans/2026-06-05-password-reset-force-logout-sessions-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记管理端重置端账号密码必须同步清理旧端会话的复用规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" clean test`：通过，seller / buyer 各 `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`，并确认 clean 后重新编译。

当前判断：

- 管理端重置端账号密码现在不再只改密码字段，还会立即失效该端账号旧会话，符合管理端对 seller/buyer 端账号的控制权要求。
- 本轮未做 HTTP smoke，因为该切片只补 service 行为；真实接口入口仍复用既有管理端 controller 和权限契约。
- 本轮未执行 SQL；远程 MySQL / Redis 未连接。

## 2026-06-05 会话与强制踢出权限契约补强检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“每个切片只改一类东西”的节奏推进。本轮只补管理端会话列表 / 强制踢出的权限契约测试，不改接口、不改 UI、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- `SellerAdminPermissionContractTest` 增加卖家管理端 `sessions`、`accountSessions`、`forceLogoutSeller`、`forceLogoutSellerAccount` 必须使用 `seller:admin:forceLogout` 的断言。
- `BuyerAdminPermissionContractTest` 按卖家模板补齐买家管理端 `sessions`、`accountSessions`、`forceLogoutBuyer`、`forceLogoutBuyerAccount` 必须使用 `buyer:admin:forceLogout` 的断言。
- `AdminAccountPermissionUiContractTest` 增加 seller/buyer 页面必须注入主体会话、账号会话、主体强踢、账号强踢 service 的断言。
- `AdminAccountPermissionUiContractTest` 增加共享主体列表和账号弹窗必须通过 `access.hasPerms(\`${permPrefix}:forceLogout\`)` 控制“会话 / 强制踢出”入口的断言。
- `PermissionServiceAccountPermissionTest` 增加 `*:admin:forceLogout` 的权限矩阵：主体权限、角色权限和账号权限不能误授权强踢；seller 精确强踢权限不能串到 buyer；超管通配仍可访问双端强踢权限。
- 新增执行记录：`docs/plans/2026-06-05-session-force-logout-permission-contract-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记会话列表与强制踢出权限契约守卫。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest" test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。

当前判断：

- 管理端 seller/buyer 会话列表和强制踢出已经有后端 controller 权限、前端显隐和 PermissionService 权限矩阵三层契约兜底。
- 旧检查点中“管理端前端 session 列表 UI 尚未接入 / buyer UI 仍未复制”的表述已是历史状态；当前以 `PartnerSessionModal`、Seller/Buyer 页面配置和本检查点为准。
- 本轮未做运行时低权限验收；下一类更适合单独验证低权限账号看不到“会话 / 强制踢出”且后端接口返回拒绝，或补 seller/buyer service 级会话列表/强踢直接单测。

## 2026-06-05 账号锁定权限契约补强检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按当前确认节奏执行：先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西。本轮只补账号锁定/解锁权限的契约测试，不改业务实现、不执行 SQL、不做页面布局调整。

已完成：

- `SellerAdminPermissionContractTest` 增加 `lockAccount` / `unlockAccount` 的 `seller:admin:account:lock` 权限断言。
- `BuyerAdminPermissionContractTest` 按卖家模板同构补齐 `buyer:admin:account:lock` 权限断言。
- `AdminAccountPermissionUiContractTest` 增加 seller/buyer 页面配置必须声明 `accountPermissions.lock`，并要求共享账号弹窗通过 `access.hasPerms(accountPermissions.lock...)` 控制锁定/解锁入口。
- `PermissionServiceAccountPermissionTest` 增加 `*:admin:account:lock` 的权限矩阵：主体/角色权限不能误授权账号锁定权限，精确 seller 账号权限不能串到 buyer，超管通配仍可访问双端账号锁定权限。
- 新增执行记录：`docs/plans/2026-06-05-account-lock-permission-contract-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记账号锁定权限必须被后端接口、权限服务和前端 UI 契约共同保护。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest" test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。

当前判断：

- seller/buyer 账号锁定/解锁的后端接口权限、权限服务矩阵和前端显隐契约已经被固定为同一套标准模板。
- 后续再改账号弹窗、页面配置或 controller 权限时，测试会阻止把锁定/解锁误挂到主体权限、角色权限或漏掉前端按钮权限。
- 本轮未连接远程 MySQL / Redis，未启动浏览器；因为切片只补源码级契约测试，运行时低权限验收已由前序 seller/buyer 锁定记录覆盖。

## 2026-06-05 Portal 401 端 token 隔离检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“每个切片只改一类东西”的节奏推进。本轮只处理一类前端隔离问题：seller/buyer portal 请求返回 401 时，不得清理管理端 `access_token`、`admin_remote_menu` 或跳转管理端登录页。本轮不改后端、不执行 SQL、不改管理端业务页面、不启动三端物理前端拆分。

已完成：

- 新增 `react-ui/src/utils/portalRequest.ts`，通过请求 URL 判断是否为 seller/buyer portal API。
- `/api/seller/admin/**` 和 `/api/buyer/admin/**` 明确排除在 portal API 之外，继续按管理端后台接口处理。
- `react-ui/src/app.tsx` 的响应拦截器在业务 `code=401` 时，先判断 portal terminal；若是 portal 请求，只清对应 `seller` / `buyer` 端 token。
- `react-ui/src/requestErrorConfig.ts` 的 BizError / HTTP status 401 处理同样按 URL 区分 portal 请求和管理端请求。
- `react-ui/scripts/check-portal-token-isolation.mjs` 增加静态守卫：必须存在 portal URL 判定工具，必须排除管理端 seller/buyer admin API，`app.tsx` 和 `requestErrorConfig.ts` 必须用 `clearTerminalSessionToken(portalTerminal)` 处理 portal 401。
- 更新 `docs/architecture/reuse-ledger.md`，登记 portal 401 与 admin session 隔离规则。
- 新增执行记录：`docs/plans/2026-06-05-portal-401-terminal-token-isolation-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/app.tsx src/requestErrorConfig.ts src/utils/portalRequest.ts scripts/check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check src/utils/portalRequest.ts scripts/check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。

当前判断：

- portal 端 401 现在只影响对应端 token，不再误伤管理端登录态。
- 管理端卖家/买家后台接口仍通过 `/api/seller/admin/**`、`/api/buyer/admin/**` 保持原有 admin 401 处理。
- 本轮未运行浏览器 smoke；当前切片是请求错误处理和静态守卫增强，后续如果做 portal 页面回归，可复跑 seller/buyer portal smoke。

## 2026-06-05 买家账号锁定解锁模板复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板先定型、买家只复制替换；每个切片只改一类东西”的节奏推进。本轮只复制买家账号锁定/解锁能力，不做充值、余额或三端物理前端拆分。

已完成：

- 新增 buyer 账号锁定字段 `lock_status` / `lock_reason`，并同步 `seller_buyer_management_seed.sql`、`20260604_three_terminal_isolation_migration.sql` 和新增增量 SQL `20260605_buyer_account_lock_control.sql`。
- 新增管理端权限点 `buyer:admin:account:lock`，同步买家账号权限 seed 和综合 seed。
- `BuyerAccount`、`BuyerMapper` / `BuyerMapper.xml`、`IBuyerService`、`AdminBuyerController`、`BuyerServiceImpl` 已按 seller 模板复制锁定/解锁链路。
- buyer 登录、免密登录生成、端内改密、端内权限校验均拒绝锁定账号。
- `Buyer/index.tsx` 和 `buyer.ts` 已注入 `lockAccount` / `unlockAccount`，共享账号弹窗无需重写。
- 模板守卫 `check-partner-management-template.mjs` 已升级为 seller/buyer 双端都检查锁定 service、URL、权限和配置绑定。
- 新增执行记录：`docs/plans/2026-06-05-buyer-account-lock-control-copy-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`。

验证结果：

- 远程 SQL 首次执行 `14` 条语句，幂等复跑 `14` 条语句。
- 远程 SQL 执行后：`buyer_account` 锁定字段数 `2`，`idx_buyer_account_buyer_lock` distinct 索引数 `1`，`buyer:admin:account:lock` 权限数 `1`，`buyer_account_lock_status` 字典类型数 `1`，字典数据数 `2`。
- 管理员 HTTP smoke：空锁定原因返回业务 `code=500` 且状态不变；有效锁定后 `lockStatus=1`，账号级免密被拒；解锁后 `lockStatus=0` 且 `lockReason=''`。
- 低权限接口验收：临时角色拥有 `buyer:admin:account:list` 但没有 `buyer:admin:account:lock`；买家列表和账号列表 `code=200`，锁定/解锁接口均 `code=403`，账号锁定字段不变。
- 低权限浏览器验收：买家账号弹窗可打开，锁定列数量 `1`，锁定/解锁操作数量 `0`，更多按钮数量 `0`；管理端 token 存在，`seller_access_token=false`，`buyer_access_token=false`。
- 临时账号 `codex_b_lock_ltd` 和临时角色 `codex_buyer_lock_negative` 均已清理，剩余 `0`。
- 截图：`react-ui/output/playwright/buyer-lock-lowperm-negative.png`，文件大小 `51957` bytes。
- `mvn -pl buyer -am test`：通过，`ruoyi-system` 44 个测试、`finance` 9 个测试、`buyer` 33 个测试均通过。
- 前端 `node --check`、`npm run guard:partner-management`、定向 `biome lint`、`npm run tsc -- --pretty false`、`npm run guard:portal-token` 均通过。
- `mvn -DskipTests install` 首次因旧 8080 Java 进程锁住 jar 在 `ruoyi-admin` repackage 失败；停止旧进程后 `mvn -DskipTests install -rf :ruoyi-admin` 通过，并已重启 8080。

当前判断：

- 买家账号锁定/解锁已按 seller 标准模板复制完成，并通过后端、前端、远程 SQL、低权限接口和低权限浏览器验收。
- 本轮证明“能看账号但不能锁定/解锁”的权限边界成立，不是通过隐藏整个账号入口规避验证。

## 2026-06-05 卖家账号锁定低权限负向验收检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家账号锁定/解锁模板之后，只验证一类权限边界：管理端低权限账号可以查看卖家账号列表，但没有 `seller:admin:account:lock` 时不能锁定或解锁卖家账号。本轮不复制 buyer，不改业务代码，不改表结构。

已完成：

- 通过若依管理端 API 创建临时角色 `codex_lock_negative` 和临时账号 `codex_lock_limited`。
- 临时角色只绑定 `seller:admin:list`、`seller:admin:query`、`seller:admin:account:list` 及其父级菜单；未绑定 `seller:admin:account:lock`。
- 使用低权限 token 验证卖家列表和卖家账号列表接口可访问。
- 使用低权限 token 验证卖家账号锁定/解锁接口均被后端拒绝。
- 使用管理员 token 复查目标账号 `lock_status` / `lock_reason` 未被低权限请求改变。
- 使用 Playwright CLI 通过真实登录页进入卖家管理，打开账号弹窗，验证能看到“锁定”状态列，但没有“锁定账号” / “解锁账号”或“更多”账号操作入口。
- 验收完成后退出临时账号会话，并删除临时账号和临时角色。
- 新增执行记录：`docs/plans/2026-06-05-seller-account-lock-low-permission-negative-record.md`。

验证结果：

- `/getInfo` 权限返回：`seller:admin:list,seller:admin:account:list,seller:admin:query`。
- `GET /seller/admin/sellers/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `GET /seller/admin/sellers/{sellerId}/accounts`：业务 `code=200`。
- `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/lock`：业务 `code=403`。
- `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/unlock`：业务 `code=403`。
- 状态核验：目标账号请求前 `lock_status=0`、`lock_reason=''`；拒绝请求后状态不变。
- Playwright CLI：卖家列表“账号”入口数量 `3`，账号弹窗 `modalHasLockColumn=true`，`modalHasLockAction=false`，`modalMoreButtonCount=0`。
- token 核验：管理端 token 存在，`seller_access_token=false`，`buyer_access_token=false`。
- 清理核验：`codex_lock_limited` 用户剩余 `0`，`codex_lock_negative` 角色剩余 `0`。
- 截图：`react-ui/output/playwright/seller-lock-lowperm-negative.png`，文件大小 `55735` bytes。

当前判断：

- 卖家账号锁定/解锁模板现在有后端权限拒绝、状态不变和真实浏览器按钮隐藏三层证据。
- 该验收角色拥有账号列表权限但没有锁定权限，证明的是“能看账号但不能锁定/解锁”，不是靠隐藏整个账号入口规避。
- buyer 锁定/解锁仍等待 seller 模板验收后按同构配置复制。

## 2026-06-05 卖家账号锁定解锁模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。本轮只做 seller 账号 `lock_status` / `lock_reason` 锁定解锁模板，不复制 buyer，不调整三端物理拆分。

已完成：

- 新增 seller 账号锁定字段 `lock_status` / `lock_reason`，并同步 `seller_buyer_management_seed.sql`、`20260604_three_terminal_isolation_migration.sql` 和新增增量 SQL `20260605_seller_account_lock_control.sql`。
- 新增 seller 管理端权限点 `seller:admin:account:lock`，同步卖家账号权限 seed 和综合 seed。
- `SellerAccount` 增加 seller 专属锁定字段；未把字段放到共享 `PortalAccount`，避免 buyer 被本切片隐式扩展。
- `SellerMapper` / `SellerMapper.xml` 新增锁定字段映射和 `updateSellerAccountLockStatus(...)` 专用更新。
- `AdminSellerController` 新增锁定/解锁接口，均受 `seller:admin:account:lock` 保护。
- `SellerServiceImpl` 新增 `lockSellerAccount(...)` / `unlockSellerAccount(...)`；锁定强踢该账号 seller 会话，解锁不恢复旧会话。
- seller 登录、免密登录生成、免密登录消费、端内改密、端内权限校验均拒绝锁定账号。
- 管理端账号弹窗通过 `lockAccount` / `unlockAccount` 可选 service 展示 seller 锁定列和“更多”操作；buyer 未注入，不展示锁定列或锁定操作。
- 新增执行记录：`docs/plans/2026-06-05-seller-account-lock-control-template-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`。

验证结果：

- 远程 SQL 预检：`seller_account` 锁定字段数为 `0`，`buyer_account` 锁定字段数为 `0`，seller 锁定权限和字典均不存在。
- 远程 SQL 执行：首次执行 `14` 条语句，幂等复跑 `14` 条语句。
- 远程 SQL 执行后：`seller_account` 锁定字段数为 `2`，`buyer_account` 锁定字段数仍为 `0`，`idx_seller_account_seller_lock` 索引数为 `1`，seller 锁定权限数为 `1`，seller 锁定字典数据数为 `2`。
- `mvn -pl ruoyi-system -am "-Dtest=PartnerSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl seller -am test`：通过，`ruoyi-system` 44 个测试、`finance` 9 个测试、`seller` 31 个测试均通过。
- `node --check scripts/check-partner-management-template.mjs`、`npm run guard:partner-management`、定向 `biome lint`、`npm run tsc -- --pretty false`、`npm run guard:portal-token` 均通过。
- `mvn -DskipTests install`：除 `ruoyi-admin` repackage 因旧后端进程锁 jar 失败外，其余模块和 admin 编译通过；停止旧 8080 Java 后端后，`mvn -DskipTests install -rf :ruoyi-admin` 通过。
- 后端已通过 `start-backend-local.ps1 -Restart` 重启；`/captchaImage` HTTP `200`，业务 `code=200`，验证码仍为关闭状态。
- HTTP smoke：空锁定原因返回业务 `code=500` 且状态不变；选择无在线会话的未锁定卖家账号成功锁定后 `lock_status=1`、解锁后 `lock_status=0` 且 `lock_reason=''`。

当前判断：

- seller 账号锁定/解锁已形成可复制模板。
- buyer 复制仍等待 seller 模板验收后单独推进。
- 低权限管理端真实账号对 `seller:admin:account:lock` 的前端隐藏、后端拒绝和状态不变验收已在后续独立检查点补齐。

## 2026-06-05 端内 OWNER 主账号数据库唯一约束检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在 Service 级 OWNER 主账号唯一性兜底之后，补齐远程 MySQL 数据库层约束。本轮只处理数据库 OWNER 唯一约束，不改 Java 生产逻辑，不改前端 UI，不处理 `lock_status` / 解锁账号。

已完成：

- 新增增量 SQL：`RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql`。
- `seller_account` 新增生成列 `owner_unique_seller_id`，当 `account_role = 'OWNER'` 时生成 `seller_id`，否则为 `NULL`。
- `seller_account` 新增唯一索引 `uk_seller_account_owner(owner_unique_seller_id)`。
- `buyer_account` 新增生成列 `owner_unique_buyer_id`，当 `account_role = 'OWNER'` 时生成 `buyer_id`，否则为 `NULL`。
- `buyer_account` 新增唯一索引 `uk_buyer_account_owner(owner_unique_buyer_id)`。
- 更新初始化 SQL：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 更新三端迁移 SQL：`RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`。
- 新增执行记录：`docs/plans/2026-06-05-terminal-owner-account-db-constraint-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记数据库层 OWNER 唯一约束。

验证结果：

- 远程 MySQL 预检：版本 `8.0.30-cynos-3.1.16.003`，seller/buyer 重复 OWNER 主体组数均为 `0`，目标列和目标索引执行前均不存在。
- 远程 DDL 执行：通过，临时 JDBC SQL runner 执行 `16` 条语句。
- 远程 DDL 执行后校验：seller/buyer 重复 OWNER 主体组数均为 `0`；`owner_unique_seller_id`、`owner_unique_buyer_id`、`uk_seller_account_owner`、`uk_buyer_account_owner` 存在数均为 `1`。
- 幂等复跑：通过，同一 SQL 复跑后列/索引存在数仍为 `1`，重复 OWNER 主体组数仍为 `0`。

当前判断：

- 端内 OWNER 主账号唯一性现在同时有 Service 级兜底和数据库唯一约束。
- 本轮没有执行 Redis 操作，也没有改前端 UI。
- `lock_status` / `lock_reason` / 解锁账号仍属于后续独立 DDL/后端/前端切片。

## 2026-06-05 端账号角色白名单 Service 校验检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在 OWNER 主账号唯一性 Service 硬化之后，只处理一类规则：端账号 `accountRole` 合法值校验。本轮不改前端 UI，不执行数据库 DDL/DML，不处理 `lock_status` / 解锁账号。

已完成：

- `PartnerSupport` 新增 `ACCOUNT_ROLE_ADMIN` 常量。
- `PartnerSupport` 新增 `normalizeAccountRole(...)`，统一允许 `OWNER` / `ADMIN` / `STAFF`，非法值抛出 `账号角色不正确`。
- `SellerServiceImpl.normalizeSellerAccount(...)` 改为复用 `PartnerSupport.normalizeAccountRole(...)`。
- `BuyerServiceImpl.normalizeBuyerAccount(...)` 按 seller 模板复制同一规则。
- `PartnerSupportTest` 新增角色默认值、大写化和非法值拒绝测试。
- `SellerServiceImplTest` 新增非法 `accountRole` 拒绝测试。
- `BuyerServiceImplTest` 按 seller 模板新增非法 `accountRole` 拒绝测试。
- 新增执行记录：`docs/plans/2026-06-05-terminal-account-role-whitelist-service-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记端账号角色白名单公共 helper。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=PartnerSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`PartnerSupportTest` 为 `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`SellerServiceImplTest` 为 `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`BuyerServiceImplTest` 为 `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am test`：通过，`ruoyi-system` 为 `Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`seller` 为 `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am test`：通过，`ruoyi-system` 为 `Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`buyer` 为 `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。

当前判断：

- 端账号角色合法值现在有统一公共 helper，seller/buyer 不再各自散写或只做大写化。
- `ADMIN` 作为第一阶段角色之一已有常量承载；后续如果字典 code 调整，需同步 `PartnerSupport.normalizeAccountRole(...)` 和端账号角色字典。
- 数据库层 OWNER 唯一约束已在后续“端内 OWNER 主账号数据库唯一约束检查点”补齐。
- `lock_status` / 解锁账号仍属于后续独立切片。

## 2026-06-05 端内 OWNER 主账号唯一性 Service 硬化检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“seller 先形成标准样板，验收后复制 buyer；每个切片只改一类东西”的节奏推进。本轮只做端内 OWNER 主账号唯一性 Service 兜底和测试守卫，不改前端 UI，不执行数据库 DDL/DML，不处理 `lock_status` / 解锁账号。

已完成：

- `SellerServiceImpl.insertSellerAccount(...)` 新增手工新增第二个 `OWNER` 的 Service 拦截。
- `BuyerServiceImpl.insertBuyerAccount(...)` 按 seller 模板复制同一拦截。
- `SellerServiceImpl.updateSellerAccount(...)` 编辑账号时不再采纳 payload 的 `accountRole`，始终保留当前账号角色。
- `BuyerServiceImpl.updateBuyerAccount(...)` 按 seller 模板复制同一规则。
- `SellerServiceImplTest` 新增 2 个测试：新增第二个 OWNER 拒绝、编辑主账号时保留当前 OWNER 角色。
- `BuyerServiceImplTest` 按 seller 模板新增 2 个测试：新增第二个 OWNER 拒绝、编辑主账号时保留当前 OWNER 角色。
- 新增执行记录：`docs/plans/2026-06-05-terminal-owner-account-uniqueness-service-hardening-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 OWNER 主账号唯一性 Service 守卫。
- 本轮启动 6 个只读 explorer 子 agent 辅助盘点，均已关闭。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`SellerServiceImplTest` 为 `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`BuyerServiceImplTest` 为 `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am test`：通过，`ruoyi-system` 为 `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`seller` 为 `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。本次并行跑 Maven 时出现一次 Surefire 临时目录 warning，但构建结果为 `BUILD SUCCESS`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am test`：通过，`ruoyi-system` 为 `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`buyer` 为 `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`。

当前判断：

- 端内手工新增第二个 OWNER 已有 Service 级兜底；前端禁选不再是唯一保护。
- 账号编辑路径现在明确为“账号角色不可通过账号编辑接口变更”；角色绑定仍走端内角色分配能力。
- 数据库层唯一约束已在后续“端内 OWNER 主账号数据库唯一约束检查点”补齐。
- `accountRole` 白名单校验已在后续“端账号角色白名单 Service 校验检查点”补齐。
- `lock_status` / `lock_reason` / 解锁账号仍属于后续独立 DDL/后端/前端切片。

## 2026-06-05 买家账号生命周期 Service 测试复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家账号生命周期 Service 测试守卫之后，只把已验收的 seller 测试模板复制到 buyer。本轮不改业务实现、不改前端、不执行数据库 DDL/DML。

已完成：

- `BuyerServiceImplTest` 从 5 个测试扩展到 12 个测试。
- 新增 buyer 账号新增测试，覆盖密码加密、默认 `STAFF`、部门必须属于当前买家。
- 新增 buyer 账号新增负向测试，覆盖其他买家部门拒绝保存。
- 新增 buyer 默认密码重置测试，固定重置的是 `buyer_account` 端内账号且密码为 BCrypt 密文。
- 新增 buyer 账号停用测试，固定停用账号后只强踢该账号会话，并通过 `buyer` 端 token key 删除。
- 新增 buyer 登录成功测试，覆盖最后登录信息更新、`buyer_session` 写入和 `buyer_login_log` 成功记录。
- 新增 buyer 停用账号登录失败测试，覆盖不签发 token 且写入登录失败日志。
- 新增 buyer 当前账号会话列表测试，覆盖查询范围来自 session，当前 token 标记为 `current=true`。
- 新增执行记录：`docs/plans/2026-06-05-buyer-account-lifecycle-service-test-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller/buyer 账号生命周期守卫已按同一模板对齐。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`BuyerServiceImplTest` 为 `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am test`：通过，`ruoyi-system` 为 `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`finance` 为 `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`buyer` 为 `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。

当前判断：

- buyer 账号生命周期核心 service 行为已按 seller 标准模板复制并通过完整 buyer 模块测试。
- 本轮只复制测试模板，没有修改生产逻辑；如果后续要补 `lock_status` / `lock_reason` / 解锁账号，应作为独立 DDL/后端/前端方案推进。
- 主账号唯一性是否要加强为数据库/后端硬约束仍需单独处理。

## 2026-06-05 卖家账号生命周期 Service 测试守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在管理端共享模板守卫之后，只补卖家端账号生命周期自动化守卫。本轮不改业务实现、不复制买家、不执行数据库 DDL/DML。

已完成：

- `SellerServiceImplTest` 从 5 个测试扩展到 12 个测试。
- 新增 seller 账号新增测试，覆盖密码加密、默认 `STAFF`、部门必须属于当前卖家。
- 新增 seller 账号新增负向测试，覆盖其他卖家部门拒绝保存。
- 新增 seller 默认密码重置测试，固定重置的是 `seller_account` 端内账号且密码为 BCrypt 密文。
- 新增 seller 账号停用测试，固定停用账号后只强踢该账号会话，并通过 `seller` 端 token key 删除。
- 新增 seller 登录成功测试，覆盖最后登录信息更新、`seller_session` 写入和 `seller_login_log` 成功记录。
- 新增 seller 停用账号登录失败测试，覆盖不签发 token 且写入登录失败日志。
- 新增 seller 当前账号会话列表测试，覆盖查询范围来自 session，当前 token 标记为 `current=true`。
- 新增执行记录：`docs/plans/2026-06-05-seller-account-lifecycle-service-test-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller 账号生命周期守卫。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`SellerServiceImplTest` 为 `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am test`：通过，`ruoyi-system` 为 `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`，`seller` 为 `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\seller\src\test\java\com\ruoyi\seller\service\impl\SellerServiceImplTest.java`：通过，仅有 LF/CRLF 工作区换行提示。
- 冲突标记检查：通过。

当前判断：

- seller 账号生命周期核心 service 行为已有自动化守卫，后续可按同一模式复制 buyer。
- `lock_status` / `lock_reason` / 解锁账号仍属于后续独立设计，不应混入本轮测试守卫。
- 主账号唯一性是否要加强为数据库/后端硬约束仍需单独处理。

## 2026-06-05 管理端卖家/买家共享模板守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按“卖家先形成标准样板，买家只替换配置和 service；每个切片只改一类东西”的节奏推进。本轮只做管理端共享模板静态守卫，不改 UI、不改后端、不执行数据库 DDL/DML。

已完成：

- 新增 `react-ui/scripts/check-partner-management-template.mjs`，固定 Seller / Buyer 管理页面必须只通过共享 `PartnerManagementPage` 配置接入。
- 新增 `guard:partner-management` 并接入 `npm run lint`。
- 守卫覆盖 seller/buyer 页面配置、账号域权限、账号/部门/角色/菜单/会话/日志/免密 service 映射、service 路径端隔离和公共组件不得硬编码端 API。
- 更新 `docs/architecture/reuse-ledger.md`，登记管理端共享模板守卫规则。
- 新增执行记录：`docs/plans/2026-06-05-admin-partner-management-template-guard-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint scripts/check-partner-management-template.mjs package.json`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts/check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `git diff --check -- react-ui\scripts\check-partner-management-template.mjs react-ui\package.json`：通过，仅有 LF/CRLF 工作区换行提示。

当前判断：

- 管理端 seller/buyer 同构 UI 模板已具备可复跑静态契约守卫，后续不会只靠人工记忆保证“卖家模板验收后复制买家”。
- 本轮没有跑完整 `npm run lint`；全量 `biome:lint` 的历史无关文件问题仍不在本切片处理。
- 下一片建议只补 seller 账号生命周期 service 测试，覆盖新增、默认密码、停用登录拒绝、最后登录、部门归属和强制踢出等账号核心行为；不与 `lock_status` DDL/UI 混合。

## 2026-06-05 买家端商城商品前端工作台复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`、`docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md`、`docs/plans/2026-06-05-buyer-distribution-product-read-template-record.md` 和 `docs/plans/2026-06-05-buyer-product-permission-dml-smoke-record.md` 为开发方向。本轮只做 buyer 前端工作台复制和验收，不做后端，不执行 DDL/DML，不重复补远程权限。

已完成：

- 新增 `BuyerDistributionProductList`，按 seller 商品卡片模板复制 buyer 商城商品卡片。
- buyer portal 首页已在 `terminal === 'buyer'` 分支渲染 buyer 商品卡片，位置与 seller 商品卡片同构。
- `react-ui/src/services/portal/session.ts` 新增 buyer 商品列表、详情和 SKU 三个 portal service。
- `react-ui/src/types/seller-buyer/party.d.ts` 新增 buyer 商品 DTO 和结果类型，不包含 seller 内部字段、系统编码、供货价或后台审计字段。
- 新增 buyer 商品模板契约守卫 `react-ui/scripts/check-buyer-portal-product-template.mjs`，并接入 `guard:buyer-portal-product` 和 `npm run lint`。
- `guard:portal-token` 已纳入 `getBuyerPortalDistributionProducts`，固定 buyer 商品列表请求必须净化身份范围参数。
- 新增 buyer 商品前端浏览器 smoke：
  - `scripts/smoke/buyer-portal-product-ui-smoke.ps1`
  - `scripts/smoke/buyer-portal-product-ui-smoke.mjs`
- 新增执行记录：`docs/plans/2026-06-05-buyer-portal-product-ui-template-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check ..\scripts\smoke\buyer-portal-product-ui-smoke.mjs`：通过。
- PowerShell 解析 `scripts/smoke/buyer-portal-product-ui-smoke.ps1`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint <本切片前端相关文件>`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\buyer-portal-product-ui-smoke.ps1 -BuyerId 2 -TimeoutMs 45000`：通过，覆盖管理端生成 buyer 免密票据、buyer direct-login、buyer portal、token storage 隔离、商品卡片、详情弹窗和退出清理。
- Browser 插件内置浏览器轻量检查：通过，`/buyer/portal` 可见“买家端”“商品浏览准备”“商城商品”和“详情”。
- `cd E:\Urili-Ruoyi\react-ui; npm run lint`：三个 guard 通过，但全量 `biome:lint` 命中既有无关文件问题，未作为本切片通过项。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 本切片相关文件尾随空白和冲突标记检查：通过。
- 敏感明文检查：未发现真实连接串、Bearer 明文、免密 token 明文或 JSON token/loginUrl 明文；命中项仅为文档中的“不得输出/省略号示例/环境变量名”说明。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，先输出 `Synced 23 changed files`、`Added: 17, Modified: 6 - 663 nodes in 1.5s`；记录回填后复跑输出 `Synced 2 changed files`、`Added: 2 - 34 nodes in 516ms`。

当前判断：

- buyer 商品浏览已完成后端只读模板、远程权限 DML、HTTP smoke、前端工作台复制、前端契约守卫和浏览器 smoke。
- buyer 前端首版只读浏览平台 `ON_SALE` 商品，不承载购物车、下单、库存承诺或客户专属价格。
- 后续如果 seller/buyer 商品卡片继续演进，建议再抽公共端内只读商品列表组件；当前按“先 seller 标准模板，再复制 buyer”的已确认节奏保留两份组件更直接。

## 2026-06-05 买家端商城商品权限 DML 与 HTTP smoke 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`、`docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md` 和 `docs/plans/2026-06-05-buyer-distribution-product-read-template-record.md` 为开发方向。本轮只做 buyer 权限 DML 与真实 HTTP smoke，不做 buyer 前端，不执行 DDL，不修改 `sys_menu` / `sys_role`。

已完成：

- 确认当前激活环境为远程 MySQL / 远程 Redis，不使用本地 Docker MySQL / Redis。
- 向远程 `buyer_menu` 幂等补入：
  - `buyer:product:distribution:list`
  - `buyer:product:distribution:query`
- 向远程 `buyer_role_menu` 幂等补入 active buyer role 对上述两个权限的授权。
- 新增并加固 buyer 商品 HTTP smoke 脚本：`scripts/smoke/buyer-distribution-product-read-template-smoke.ps1`。
- 停止旧 8080 后端进程，重建 `ruoyi-admin.jar` 并启动新 jar。
- 通过 buyer 商品真实 HTTP smoke，覆盖无 token 拒绝、buyer 登录、`getInfo` 权限集合、列表、伪造范围参数不生效、详情、SKU、固定不存在商品拒绝和 logout 后旧 token 失效。
- 更新执行记录：`docs/plans/2026-06-05-buyer-product-permission-dml-smoke-record.md`。
- 回填 buyer 后端模板记录：`docs/plans/2026-06-05-buyer-distribution-product-read-template-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 buyer 权限 DML 与 HTTP smoke 模板。

验证结果：

- 远程 DML 影响：`buyer_menu` 新增 2 行，`buyer_role_menu` 新增 2 行；执行后目标权限计数均已核对。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1`：已启动新后端 jar。
- `GET http://127.0.0.1:8080/captchaImage`：返回 `code=200`，`captchaEnabled=False`；本轮未修改验证码开关。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\buyer-distribution-product-read-template-smoke.ps1 -BuyerUsername '<管理端列表返回的测试买家账号>' -PageSize 10`：通过。
- `scripts/smoke/buyer-distribution-product-read-template-smoke.ps1` PowerShell 解析检查：通过。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 本切片相关文件尾随空白和冲突标记检查：通过。
- 敏感明文检查：未发现数据库连接串、明文密码、Bearer token 或 JWT；命中项均为“不得输出/不得展示”的说明文字。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- buyer 后端只读模板与远程权限已经具备真实 HTTP 验收证据。
- buyer 前端工作台复制和浏览器 smoke 尚未做。
- 下一切片建议只做 buyer 前端工作台复制：按 seller portal 商品卡片模板替换 terminal、service、路由、DTO 和断言文本，不重新设计页面结构。

## 2026-06-05 买家端商城商品浏览只读后端模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 和 `docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md` 为开发方向。本轮只做 buyer 后端只读模板，不做前端，不执行远程数据库 DDL/DML，不做 HTTP smoke。

已完成：

- 新增 `BuyerPortalProductDistributionController`，提供 buyer 端商城商品列表、详情和 SKU 只读入口。
- 新增 `IBuyerPortalProductService` / `BuyerPortalProductServiceImpl`，buyer 端身份只用于鉴权和 session 校验，不作为商品归属。
- 新增 `BuyerPortalProduct` / `BuyerPortalProductSku`，不复用管理端 `ProductSpu` / `ProductSku`，不复用 seller DTO。
- 扩展 product 共享只读查询：上架商品列表、上架商品详情、上架 SKU 列表，列表聚合只基于 `ON_SALE` SKU。
- 更新 `seller_buyer_management_seed.sql`，补齐 `buyer:product:distribution:list` 和 `buyer:product:distribution:query` 到 `buyer_menu` 与 active `buyer_role_menu` seed。
- 新增 `BuyerPortalProductServiceImplTest`，覆盖 buyer session 校验、客户端身份字段忽略、分页元数据保留、上架可见性、DTO 脱敏和 SKU 读取前可见性校验。
- 新增执行记录：`docs/plans/2026-06-05-buyer-distribution-product-read-template-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 buyer 商品浏览只读后端模板。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am test`：通过，buyer 模块 `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`，依赖模块同轮通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,PortalTokenSupportTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 本切片相关文件尾随空白和冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 11 changed files`、`Added: 6, Modified: 5 - 456 nodes in 1.1s`。

当前判断：

- buyer 后端只读模板已经完成。
- 远程运行库尚未补 `buyer_menu` / `buyer_role_menu` 权限，因此本轮不做 HTTP smoke。
- 下一切片建议只做 buyer 权限 DML 与真实 HTTP smoke，不做前端。

## 2026-06-05 买家商品浏览复制前边界检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按当前已确认节奏执行：先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西。本轮只做 buyer 商品浏览复制前边界记录，不新增 buyer 接口，不改前端，不执行数据库 DDL/DML。

已完成：

- 确认 seller portal “我的商城商品”模板已经完成后端、前端、HTTP smoke、浏览器 smoke 和契约守卫验收。
- 梳理 buyer 当前只有商品分类和商品 schema 只读入口，尚未落地 buyer 商品列表、详情、SKU 浏览入口。
- 明确 buyer 商品浏览不能机械复制 seller 商品拥有关系：buyer 的 `subjectId` 只代表登录主体，不代表商品归属。
- 明确 buyer 首版推荐口径：只读浏览平台 `ON_SALE` SPU 和 `ON_SALE` SKU，且列表价格聚合基于 `ON_SALE` SKU；只展示销售价和币种，不展示供货价、seller 内部编码、系统 SPU/SKU、后台审计字段或 token 字段。
- 明确 buyer 商品路径和权限命名暂定沿用 seller 模板的 `distribution-products` / `buyer:product:distribution:*`；如果后续决定改为 `browse-products` / `buyer:product:browse:*`，必须在 buyer 后端切片开始前一次选定，不混用两套命名。
- 明确旧 `sys_*` 混用、`PortalAccountSupport` / `PortalAccountMapper` 回退方向禁止恢复。
- 新增边界方案：`docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md`。

当前判断：

- seller 模板可以作为结构模板复制，但 buyer 必须替换业务谓词、DTO、权限和验收断言。
- buyer 后续建议按四个切片推进：后端只读模板、远程权限 DML 与 HTTP smoke、前端工作台复制、浏览器 smoke 与模板验收。
- portal 请求 401 可能清理 admin session 的隔离瑕疵作为后续独立 token/session 加固项处理，不混入 buyer 商品后端切片。
- 本轮没有执行数据库写入；涉及 `buyer_menu` / `buyer_role_menu` 的远程 DML 需要在后续独立切片中记录目标数据源、执行类型和影响范围。

验证结果：

- `git diff --check -- <本切片相关 Markdown>`：通过，仅有 LF/CRLF 工作区换行提示。
- 本切片相关 Markdown 尾随空白和冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 2026-06-05 卖家端商品模板验收检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按当前节奏执行：先做一套标准卖家模板，验收通过后再复制买家；每个切片只改一类东西。本轮只做 seller portal “我的商城商品”模板验收，不复制 buyer，不改后端接口，不执行数据库 DDL/DML。

已完成：

- 复跑 seller 商品只读 service 单测。
- 复跑端路由归属和 seller/buyer seed 权限契约测试。
- 复跑 seller 模块完整测试。
- 复跑 seller 商品前端模板契约守卫。
- 复跑 portal token / query 参数隔离守卫。
- 复跑前端 TypeScript 检查。
- 复跑后端真实 HTTP smoke。
- 复跑前端真实浏览器 smoke。
- 新增验收记录：`docs/plans/2026-06-05-seller-portal-product-template-acceptance-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller 商品模板验收基线。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller test`：通过，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-own-distribution-product-read-template-smoke.ps1 -SellerUsername '594165649@qq.com' -OtherSellerUsername '1234'`：通过，覆盖 seller 登录、列表、伪造客户端范围参数、详情、SKU、字段脱敏、跨 seller 拒绝和 logout 清理。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-portal-product-ui-smoke.ps1 -SellerId 5`：通过，覆盖 seller 免密代入、token storage 隔离、商品卡片、详情弹窗和退出清理。

当前判断：

- seller portal “我的商城商品”模板已完成后端契约、前端契约、真实 HTTP 链路和真实浏览器链路验收。
- buyer 仍未复制；复制前必须先确认 buyer 商品浏览业务口径，不得机械套用 seller 商品拥有关系。

## 2026-06-05 卖家端商品前端浏览器烟测脚本检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按当前节奏执行：先做一套标准卖家模板，验收通过后再复制买家；每个切片只改一类东西。本轮只补 seller portal 商品前端可复跑浏览器验收脚本，不复制 buyer，不改后端，不执行数据库 DDL/DML。

已完成：

- 新增 `scripts/smoke/seller-portal-product-ui-smoke.ps1`。
- 新增 `scripts/smoke/seller-portal-product-ui-smoke.mjs`。
- 脚本默认通过管理端 `admin / admin123` 创建 seller 免密票据，进入 `/seller/portal` 验证“我的商城商品”卡片和详情弹窗。
- 脚本验证 fresh browser context 下只写入 `seller_access_token`，不写入 `access_token` 或 `buyer_access_token`。
- 脚本验证页面可见文本不包含 `sellerId`、`systemSpuCode`、`systemSkuCode`、`tokenId`、`Authorization`。
- 脚本不会输出 `admin token`、`seller token`、`directLoginToken`、免密 URL、Redis key 或 `.env.local` 内容。
- 新增执行记录：`docs/plans/2026-06-05-seller-portal-product-ui-smoke-script-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller 商品前端浏览器烟测脚本。

验证结果：

- `GET http://127.0.0.1:8080/captchaImage`：返回 `code=200`，`captchaEnabled=False`。
- `GET http://127.0.0.1:8001`：返回 HTTP 200。
- 本机 Chrome 和 Edge 通道均可 headless launch。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check ..\scripts\smoke\seller-portal-product-ui-smoke.mjs`：通过。
- `cd E:\Urili-Ruoyi; node --check scripts/smoke/seller-portal-product-ui-smoke.mjs`：通过。
- PowerShell 解析检查：通过。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-portal-product-ui-smoke.ps1 -SellerId 5`：通过，覆盖 seller 免密代入、token storage 隔离、商品卡片、详情弹窗和退出清理。

当前判断：

- seller portal “我的商城商品”已有可复跑浏览器验收脚本，可以作为 seller 模板验收门槛。
- buyer 仍未复制；复制前必须先确认 buyer 商品浏览口径，并按 seller 已验收模板替换 terminal、路由、service 和断言文本。

## 2026-06-05 卖家端商品前端模板契约守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按当前节奏执行：先做一套标准卖家模板，验收通过后再复制买家；每个切片只改一类东西。本轮只加固 seller portal 商品前端模板契约，不复制 buyer，不改后端，不执行数据库 DDL/DML。

已完成：

- 新增 `react-ui/scripts/check-seller-portal-product-template.mjs`。
- 新增 `react-ui` 脚本 `guard:seller-portal-product`，并接入 `npm run lint`。
- 守卫固定 `SellerOwnDistributionProductList.tsx` 必须通过 `@/services/portal/session` 调用 seller 商品 service。
- 守卫固定 seller 商品组件使用 `API.Partner.SellerPortalProduct` / `API.Partner.SellerPortalProductSku`，不得复用管理端 `API.ProductDistribution`。
- 守卫固定 portal 首页只在 `terminal === 'seller'` 分支渲染 seller 商品卡片。
- 守卫固定 seller 商品列表 service 必须使用 `sanitizePortalQueryParams(params)`，并继续显式 `isToken:false`。
- 新增执行记录：`docs/plans/2026-06-05-seller-portal-product-template-contract-guard-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller 商品前端模板契约守卫。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check scripts/check-seller-portal-product-template.mjs`：通过。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 本切片相关文件尾随空白和冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，新增脚本索引输出 `Synced 1 changed files`、`Added: 1 - 24 nodes`；记录回填后再次同步输出 `Synced 2 changed files`、`Modified: 2 - 100 nodes`。

当前判断：

- seller portal “我的商城商品”前端模板已有独立契约守卫，可以作为后续 seller 模板验收的一部分。
- buyer 仍未复制；复制 buyer 前必须先完成 seller 验收，并确认 buyer 商品浏览口径不是 seller 商品拥有关系的机械替换。

日期：2026-06-04

## 参考方向

本目标追踪以以下方案为当前唯一参考方向：

- `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

该方案明确替代此前“卖家/买家账号继续复用若依 `sys_user`”的旧方向。

后续如果旧文档、旧代码或旧 SQL 仍然体现以下思路，均应视为待迁移或待清理项：

- 卖家端账号写入 `sys_user`
- 买家端账号写入 `sys_user`
- 卖家端角色写入 `sys_role`
- 买家端角色写入 `sys_role`
- 卖家端菜单写入 `sys_menu`
- 买家端菜单写入 `sys_menu`
- 卖家端部门写入 `sys_dept`
- 买家端部门写入 `sys_dept`
- 卖家端登录/操作日志只写若依系统日志
- 买家端登录/操作日志只写若依系统日志

## 总目标

形成三端独立的账号权限控制面：

| 端 | 目标 |
| --- | --- |
| 管理端 | 保留若依 `sys_*` 后台能力，作为平台控制面 |
| 卖家端 | 独立账号、密码、角色、菜单、权限、部门、日志、会话 |
| 买家端 | 独立账号、密码、角色、菜单、权限、部门、日志、会话 |

管理端仍保留对卖家和买家的控制权，但控制权来自：

- 平台管理接口
- 主体状态
- 账号状态
- 菜单/角色配置
- 免密代入
- 强制踢出
- 审计日志

不再来自账号体系混用。

## 当前状态

说明：本节是当前权威状态。下方历史检查点按发生时间保留，若早期“未完成”事项与本节或最新检查点冲突，以本节和最新检查点为准。

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| 三端隔离方案 | 已完成 | 已写入 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` |
| AGENTS 规则更新 | 已完成 | 已将新方向写入 `AGENTS.md` |
| 账号表字段设计 | 第一批已落地 | `seller_account` / `buyer_account` 已改为端内账号字段；端内角色、菜单、部门、日志、会话表已进入 SQL |
| 数据库 DDL/DML | 已执行 | 远程库已执行 `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` |
| 远程库三端结构核验 | 已完成 | 只读核验确认远程 MySQL 三端核心表 `21/21` 存在，`seller_account.user_id` / `buyer_account.user_id` 均不存在，端账号、端日志和端会话关键字段及索引存在 |
| 后端管理端账号改造 | 第一批已完成 | 卖家/买家账号创建、列表、重置密码、主账号重置、免密登录不再依赖 `sys_user` |
| 后端端内认证改造 | 第一批已完成 | 已新增卖家端/买家端登录入口、独立 token、免密 token 消费、登录日志、会话写入；端内菜单/角色权限读取已接入，端内操作日志第一批写入链路已接入 |
| 后端端内权限基础改造 | 第四批已完成 | 管理端已可维护端内菜单、角色、账号角色绑定、部门和端账号部门绑定；卖家端/买家端登录后已可读取端内角色、权限和菜单；端内接口级权限注解和统一校验器已落地 |
| 管理端前端字段改造 | 第八批已完成 | 卖家/买家管理已接入公共端账号弹窗、端内部门弹窗、端内角色弹窗和端内菜单配置弹窗；支持端账号列表、新增、编辑、部门树绑定、重置默认密码、强制踢出、账号角色绑定、端内部门维护、端内角色维护和端内菜单维护 |
| 管理端卖家/买家账号权限粒度 | 已完成并已补远程库 | 卖家账号域已先形成标准模板；买家已按同构规则只替换 terminal、controller、权限前缀、seed 和 service 配置完成复制。当前远程运行库已补齐 `seller:admin:account:*` 与 `buyer:admin:account:*` 各 6 个 `sys_menu` 权限 |
| 管理端账号域低权限负向守卫 | 已完成并已实测 | 已新增 `PermissionServiceAccountPermissionTest` 和 `AdminAccountPermissionUiContractTest`，覆盖运行时低权限负向判断和前端账号按钮显隐契约；真实低权限账号 `codex_limited` 已完成接口 403 和浏览器按钮隐藏验收，临时账号和角色已清理 |
| 同构 UI 模板化推进 | 已确认 | 后续已确定模式按“卖家先做标准样板，买家只替换配置和 service”推进，不再逐页重新设计 |
| 管理端审计 UI 与查询 | 已完成 | 卖家/买家管理已按同构模板接入审计弹窗，可查询登录日志、操作日志和免密票据；端内操作日志第一批写入链路已覆盖 `getInfo` / `getRouters` |
| 免密代入审计原因 | 已完成并已实测 | 管理端生成卖家/买家端免密代入票据时必须填写代入原因，并写入 `portal_direct_login_ticket.reason`；真实接口烟测已确认未填原因会被拒绝、原因可在审计列表读回、票据消费后变为 `USED` |
| 前端直登入口与端内工作台 | 第一版已完成，token 持久化已加固 | 当前 `react-ui/` 已落地 `/seller/direct-login`、`/buyer/direct-login`、`/seller/portal`、`/buyer/portal`；seller/buyer 工作台已分别接入商品 Schema 前端消费卡片和商城商品只读卡片；`persistPortalLogin` 已校验 expected terminal，portal token 静态守卫已接入 `npm run lint`；该工作台是验证型入口，后续物理拆分可迁移模板 |
| 前端 portal 请求身份范围参数守卫 | 已完成，已覆盖 seller/buyer 商品列表 | 已在 `portal/session.ts` 清洗日志、会话、seller 商品列表和 buyer 商品列表 query 参数，并扩展 `guard:portal-token`，防止 `src/pages/Portal/**` 和 `src/services/portal/**` 直接请求、硬编码端 API 或把 `sellerId` / `buyerId` / `subjectId` / `accountId` / `terminal` 等客户端身份范围字段作为请求参数发送 |
| 端内当前账号日志接口 | 已完成，双端已加固 | 已落地 seller/buyer 当前账号登录日志、操作日志只读接口；seller/buyer 均已从“Controller 覆盖 DTO”升级为“Service 内按 `PortalLoginSession` 强制收敛范围”，买家端已按卖家模板只替换 terminal、service、controller、mapper、测试名和文案完成同构复制 |
| 端内当前账号会话接口 | 已完成 | 已落地 seller/buyer 当前账号会话只读接口；查询范围由 `PortalSessionContext` 推导，只返回当前端账号自己的会话，不返回 `tokenId`、JWT、Redis key 或密码字段 |
| 端内会话响应脱敏契约守卫 | 已完成 | 已新增 `PortalSessionProfileTest`，固定 `PortalSessionProfile.tokenId` 不得序列化输出，防止端内和管理端会话列表响应泄漏 tokenId |
| 端内商品分类与 Schema 只读接口 | 已完成 | 已落地 seller/buyer 端可发布商品分类列表和商品 schema 只读接口；seller/buyer 商品分类/schema 端入口均已从 product controller 收口到各自 terminal facade，product 只保留共享 schema service |
| 端内商品 Schema 前端消费模板 | 已完成 | `react-ui` 的 `/seller/portal` 已接入商品发布准备卡片，`/buyer/portal` 已按卖家模板接入商品浏览准备卡片；均通过端 service 真实消费对应端商品分类和 Schema 接口，不复用管理端 token |
| 端内商城商品只读后端模板 | seller/buyer 双端已完成 | seller 端“我的商城商品”按当前 seller 范围读取自有商城商品；buyer 端“商城商品”按平台 `ON_SALE` 可见性只读浏览，不把 buyerId 当商品归属。两端均使用端内 DTO，不直接返回 product 管理端实体；seller/buyer 远程权限 DML 与 HTTP smoke 均已完成 |
| 端内商城商品前端工作台模板 | 双端 ProTable 标准模板已完成 | `react-ui` 的 `/seller/portal` 已将“我的商城商品”主列表升级为标准 ProTable；`/buyer/portal` 已按 buyer 浏览口径复制 ProTable 差量，只保留关键词搜索、分页、刷新和详情，不展示或筛选 seller/system/供货价字段；双端均继续通过 portal service 使用端 token 和 `isToken:false`，并过滤客户端身份范围字段 |
| 免密登录响应日志脱敏 | 已完成 | 管理端 seller/buyer directLogin 的 `@Log` 已关闭响应体记录；真实验证 `sys_oper_log` 未写入 `token` / `loginUrl` / `directLoginToken` |
| Portal Controller 匿名放行硬化 | 已完成 | `SellerPortalController` / `BuyerPortalController` 已移除类级 `@Anonymous`，12 个 seller 映射和 12 个 buyer 映射均改为方法级 `@Anonymous` + `@PortalPreAuthorize` |
| 端内 Controller 鉴权模板守卫 | 已完成 | `TerminalRouteOwnershipTest` 已覆盖 product 不承载 seller/buyer 端入口，并覆盖 seller/buyer 受保护 portal handler 必须方法级 `@Anonymous` + `@PortalPreAuthorize` + `@PortalLog` + `PortalSessionContext.requireSession(...)` |
| 端账号权限 sys_* 回退守卫 | 已完成 | `TerminalAccountIsolationTest` 已覆盖 seller/buyer 模块不得引用 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`SysUser`、`SysRole`、`SysMenu`、`SysDept`、旧 `PortalAccountSupport` / `PortalAccountMapper` 或旧 `*.user_id` |
| 三端隔离 SQL 基线守卫 | 已完成 | `TerminalSqlIsolationContractTest` 已覆盖主三端隔离迁移脚本不得通过 `LIKE sys_logininfor/sys_oper_log` 派生端内日志表，也不得从 `sys_user` 回填 seller/buyer 端账号；历史混用账号库回填已拆到 legacy helper |
| 端 token 隔离守卫 | 已完成 | `PortalTokenSupportTest` 已覆盖 `portal_login_tokens:{terminal}:{tokenId}`、JWT terminal claim、Redis session terminal 校验和按端删除 token key |
| 卖家端 DB 会话权威鉴权模板 | 已完成 | seller 端 `@PortalPreAuthorize` 鉴权时已回查 `seller_session` 的 `status/logout_time/expire_time`；实测只改 DB session 失效且保留 Redis token 时，旧 seller token 调 `/seller/getInfo` 返回 `401` |
| 买家端 DB 会话权威鉴权模板 | 已完成 | buyer 端已按卖家模板复制，`@PortalPreAuthorize` 鉴权时回查 `buyer_session` 的 `status/logout_time/expire_time`；实测只改 DB session 失效且保留 Redis token 时，旧 buyer token 调 `/buyer/getInfo` 返回 `401` |
| 端内权限 Service 会话 fail-closed 守卫 | 已完成 | `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest` 已覆盖权限信息和 `selectPermissions(...)` 入口的畸形 session、空白 token、DB session 和账号锁定拒绝；`SellerPortalPermissionServiceImplMenuTreeTest` / `BuyerPortalPermissionServiceImplMenuTreeTest` 已单独覆盖 `selectPortalMenuTree(...)`，防止只凭 Redis token 或账号状态放行 |
| 管理端会话列表后端模板 | 已完成 | seller/buyer 管理端均已新增主体级和账号级 session 只读列表接口，复用 `PortalSessionProfile` 脱敏响应，真实接口烟测通过 |
| 管理端会话列表 UI 模板 | 已完成 | `react-ui/` 已接入 seller/buyer 主体级和账号级会话只读弹窗；buyer 已按 seller 模板复制，只替换 service 和配置 |
| 前端三端物理拆分 | 未开始 | 当前仍在 `react-ui/` 中验证 seller/buyer 直登页和工作台模板，尚未复制 `seller-ui` / `buyer-ui` |
| 旧实现迁移 | 第二批已完成 | 旧 `PortalAccountSupport` / `PortalAccountMapper` 已移除；主迁移脚本已删除账号表旧 `user_id` 列，且不再内置 `sys_user` 回填；历史混用账号库回填仅保留在明确标记的 legacy helper 中 |

## 2026-06-04 实施检查点

本次实施以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，已从方案阶段进入代码和远程库落地。

已完成：

- 后端卖家/买家账号 Mapper 与 Service 改造：账号字段来自 `seller_account` / `buyer_account`，不再 join `sys_user`。
- 删除旧的 `PortalAccountSupport` 和 `PortalAccountMapper`，避免后续继续把端账号写回 `sys_user`。
- 免密登录返回和 Redis payload 改为 `accountId`，有效期保持 30 分钟。
- 前端账号重置默认密码改为发送端账号 ID。
- 初始化脚本 `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 已更新为三端独立表结构。
- 新增并执行远程库迁移脚本 `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`。

远程库执行与校验：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行结果：迁移脚本 82 条语句成功。
- 表结构校验：14 张端内核心表存在。
- 账号字段校验：`seller_account` / `buyer_account` 旧 `user_id` 列数量为 0。
- 数据量校验：`seller_account = 3`，`buyer_account = 1`。

接口验证：

- `mvn -DskipTests install`：通过。
- `npm run tsc`：通过。
- 后端启动：`start-backend-local.ps1 -Restart` 后 8080 正常监听。
- `/captchaImage`：200。
- 管理端登录：成功。
- `/seller/admin/sellers/list`：200，返回 3 条。
- `/buyer/admin/buyers/list`：200，返回 1 条。
- 卖家账号列表：返回 1 条。
- 买家账号列表：返回 1 条。
- 卖家免密登录：200，`expireMinutes = 30`，返回 `accountId`。
- 买家免密登录：200，`expireMinutes = 30`，返回 `accountId`。

未完成：

- `seller_role` / `buyer_role`、`seller_menu` / `buyer_menu`、`seller_dept` / `buyer_dept` 的管理端后端配置接口已完成；管理端配置页面和账号部门绑定仍未做。
- 免密登录目前仍是 Redis 一次性 token，尚未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出已在后续管理端强制踢出检查点完成；端内操作日志写入链路已接入 `@PortalLog`，当前覆盖卖家/买家端 `getInfo` 和 `getRouters`，后续真实业务接口继续复用。

## 阶段目标

### 阶段 0：冻结旧方向

目标：停止继续扩展卖家/买家复用 `sys_user` 的实现。

任务：

- [x] 写明新方向参考方案。
- [x] 更新 `AGENTS.md`。
- [x] 审计当前文档，标记旧方向文档为过期或待迁移。
- [x] 审计当前代码，列出并迁移第一批依赖 `sys_user` 的卖家/买家端账号逻辑。

完成标准：

- 新任务不再基于 `seller_account.user_id` / `buyer_account.user_id` 扩展功能。
- 后续代码实现引用三端隔离方案。

### 阶段 1：表结构设计确认

目标：确认三端独立账号权限基础表。

任务：

- [x] 输出并落地 `seller_account` 新字段方案。
- [x] 输出并落地 `buyer_account` 新字段方案。
- [x] 输出并落地 `seller_role` / `seller_menu` / `seller_dept` / `seller_account_role` / `seller_role_menu` 表方案。
- [x] 输出并落地 `buyer_role` / `buyer_menu` / `buyer_dept` / `buyer_account_role` / `buyer_role_menu` 表方案。
- [x] 输出并落地 `seller_login_log` / `seller_oper_log` 表方案。
- [x] 输出并落地 `buyer_login_log` / `buyer_oper_log` 表方案。
- [x] 输出并落地管理端免密代入票据表方案。
- [x] 输出并执行旧数据迁移方案。

完成标准：

- 用户确认 Markdown 表结构设计。
- 明确哪些表新增、哪些字段废弃、哪些旧逻辑迁移。
- 明确远程数据库执行计划。

### 阶段 2：数据库迁移

目标：让数据库具备三端独立账号权限基础。

前置条件：

- 表结构设计已确认。
- 已读取当前激活 MySQL/Redis 配置。
- 已生成远程数据库执行记录。

任务：

- [x] 只读确认远程库当前激活配置。
- [x] 只读确认远程库当前卖家/买家账号数据量。
- [x] 执行已确认 DDL。
- [x] 执行已确认迁移 DML。
- [x] 校验迁移后数量、账号字段和旧列移除状态。

完成标准：

- 卖家端账号可独立存在于 `seller_account`。
- 买家端账号可独立存在于 `buyer_account`。
- 新增卖家/买家端账号不再写入 `sys_user`。

### 阶段 3：后端认证和权限改造

目标：管理端、卖家端、买家端认证分开。

任务：

- [x] 保留管理端若依登录。
- [x] 新增卖家端登录服务。
- [x] 新增买家端登录服务。
- [x] token/session 增加 `terminal`、`accountId`、`subjectId`。
- [x] 改造管理端卖家账号密码重置。
- [x] 改造管理端买家账号密码重置。
- [x] 改造卖家端最后登录记录。
- [x] 改造买家端最后登录记录。
- [x] 改造免密代入生成和消费。
- [x] 增加端内权限读取、端类型校验和菜单数据范围校验。
- [x] 增加端内接口级权限注解、切面和统一校验器。
- [x] 卖家端 `@PortalPreAuthorize` 鉴权接入 `seller_session` 在线状态兜底校验。
- [x] 买家端按卖家模板复制 `buyer_session` 在线状态兜底校验。
- [ ] 端内业务接口逐步接入数据范围校验。

完成标准：

- 管理端账号不能登录卖家端/买家端。
- 卖家端账号不能登录管理端/买家端。
- 买家端账号不能登录管理端/卖家端。
- 停用主体后，该主体下账号不可登录。
- 停用账号后，该账号不可登录。

### 阶段 4：管理端控制能力

目标：管理端对卖家/买家保持平台控制权。

任务：

- [x] 管理端可管理卖家主体状态。
- [x] 管理端可管理买家主体状态。
- [x] 管理端可管理卖家端账号。
- [x] 管理端可管理买家端账号。
- [x] 管理端可通过后端接口配置卖家端菜单和角色。
- [x] 管理端可通过后端接口配置买家端菜单和角色。
- [x] 管理端可通过后端接口绑定卖家端账号与端内角色。
- [x] 管理端可通过后端接口绑定买家端账号与端内角色。
- [x] 管理端可查看卖家端登录/操作日志。
- [x] 管理端可查看买家端登录/操作日志。
- [x] 管理端可强制踢出卖家/买家主体或账号的在线会话。
- [x] 管理端可查看卖家端主体或账号的 session 列表。
- [x] 管理端可查看买家端主体或账号的 session 列表。

完成标准：

- 管理端不混用账号体系也能停用、重置、代入、踢出、审计卖家/买家端账号。

### 阶段 5：前端三端物理拆分

目标：在账号权限模型稳定后拆分前端。

任务：

- [ ] 确认最终目录命名。
- [ ] 拆出管理端前端。
- [ ] 拆出卖家端前端。
- [ ] 拆出买家端前端。
- [ ] 三端使用不同登录入口。
- [ ] 三端使用不同 token storage key。
- [ ] 三端使用不同菜单接口。

完成标准：

- 三个前端独立运行、独立构建、独立登录。
- 卖家端和买家端不携带管理端菜单和权限逻辑。

## 当前残留点

| 残留点 | 说明 | 处理方式 |
| --- | --- | --- |
| 端内权限业务鉴权未全面接入 | `getInfo` / `getRouters` 已读取端内角色、权限和菜单；后续真实业务接口仍需逐步使用端 token 推导主体范围 | 后续业务接口开发时逐接口接入 |
| 管理端同构 UI 模板已形成 | 卖家侧先做标准样板，买家侧只替换端类型、文案、路由、权限、字段配置和 service；账号、部门、角色、菜单、审计和会话列表弹窗已按此方式完成 seller/buyer 接入 | 后续已确定模式的管理端 UI 直接套模板推进，不再逐页重新设计 |
| 端内真实业务接口范围控制仍需逐步接入 | 管理端审计弹窗已可查看登录日志、操作日志和 ticket；`seller_oper_log` / `buyer_oper_log` 第一批写入链路已接入端内 `getInfo` / `getRouters` | 后续真实端内业务接口必须从 token 推导主体范围，并继续使用 `@PortalLog` 写入端内操作日志 |
| 商品 Schema terminal facade 已收口 | seller/buyer 商品分类和 Schema 入口均已迁到各自 terminal facade，product 只保留共享只读 schema service | 后续同构端内商品接口继续按此模板推进，只替换 terminal、路径、权限点、日志 title 和模块依赖 |
| 前端三端物理拆分仍未开始 | 当前仍以 `react-ui/` 作为管理端验证入口；真正 `admin-ui` / `seller-ui` / `buyer-ui` 物理拆分尚未落地 | 等账号、端入口、菜单域、权限模型和管理端控制权继续稳定后再拆目录 |

## 下一步

下一步不再重复设计已确认的管理端同构 UI。后续同构页面按“卖家一套做好，再复制成买家，只替换配置和 service”的方式提速推进；后端同构鉴权也按本轮卖家到买家的模板复制方式推进。

建议顺序：

- 继续把后续真实卖家端/买家端业务接口接入端 token 主体推导，不信任前端传入的 `sellerId` / `buyerId`。
- 真实端内接口继续接入 `@PortalPreAuthorize` 和 `@PortalLog`，让权限校验、数据范围和操作日志形成默认模板。
- 后续端内商品发布、浏览、筛选和详情页面优先复用已对齐的 seller/buyer 商品分类与 Schema 前端模板，只替换 terminal、文案和 service。
- 新增管理端同构 UI 时，优先复用当前 `PartnerManagement` 模板和 service 配置注入方式。
- 前端三端物理拆分暂不提前做，等端入口、菜单域、权限模型和管理端控制权继续稳定后再拆目录。

## 2026-06-04 目标追踪状态清理检查点

本检查点用于清理目标追踪里的陈旧状态，不做代码改动。

已完成：

- `AGENTS.md` 已记录同构管理端 UI 的模板化推进规则：卖家侧先形成样板，买家侧替换配置和 service。
- 本文件顶部“当前状态”补齐“管理端审计 UI 与查询”已完成。
- 本文件顶部“当前残留点”移除“日志与审计页面仍未接入”的陈旧表述。
- 历史检查点保留原始时间线；若早期“未完成”与顶部当前状态冲突，以顶部当前状态和最新检查点为准。

验证结果：

- 本轮仅文档清理。
- `git diff --check -- docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md AGENTS.md`：通过，仅有 LF/CRLF 提示。

## 2026-06-04 端内权限校验器自动化验证检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补强后续真实端内业务接口会复用的 `PortalPermissionChecker` 自动化验证。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalPermissionCheckerTest.java`。
- 覆盖无显式权限要求时只校验端会话即可通过。
- 覆盖 `requiredPermissions` 与 `anyPermissions` 同时存在时的组合校验。
- 覆盖缺少 required 权限时返回 `403` 和“没有操作权限”。
- 覆盖缺少 any 权限时返回 `403` 和“没有操作权限”。
- 覆盖端类型未注册权限服务时拒绝访问，避免卖家/买家端权限服务串用。

验证结果：

- 首次执行 `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test` 失败，原因是 JUnit `assertEquals` 在 `int` 与 `Integer` 间存在重载歧义。
- 修正断言为 `Integer.valueOf(HttpStatus.FORBIDDEN)` 后重跑通过。
- 最终结果：`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。

本轮未执行事项：

- 本轮没有执行 DDL/DML。
- 本轮没有连接远程 MySQL/Redis。
- 本轮没有重启后端。
- 本轮没有改动前端。

## 2026-06-04 免密代入原因必填检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补齐管理端免密代入必须可审计的“代入原因”链路。

已完成：

- 新增 `PortalDirectLoginRequest`，承载管理端生成免密代入票据时提交的 `reason`。
- `PortalDirectLoginSupport.createToken(...)` 新增 `reason` 参数，并统一校验：
  - 代入原因不能为空。
  - 代入原因不能超过 `portal_direct_login_ticket.reason` 字段长度 255 字符。
  - 生成票据时写入 `portal_direct_login_ticket.reason`。
- 卖家管理端接口 `POST /seller/admin/sellers/{sellerId}/directLogin` 改为接收 request body，并把 `reason` 传入公共支撑。
- 买家管理端接口 `POST /buyer/admin/buyers/{buyerId}/directLogin` 按同一模板改造。
- 卖家/买家 service 的 `create*DirectLogin` 签名同步补充 `reason`。
- 前端 `PartnerManagementPage` 点击“登录卖家端/买家端”时先弹出“代入原因”输入框，校验通过后才生成并打开免密链接。
- 卖家/买家前端 service 同步改为 `POST` JSON body：`{ reason }`。
- `docs/architecture/reuse-ledger.md` 已更新：免密代入必须通过公共支撑写入 `reason`，不能另开临时备注字段或绕过公共支撑。

验证结果：

- `mvn -DskipTests compile`：通过。
- `npm run tsc`：通过。
- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check`：通过，仅有 LF/CRLF 提示。

大文件合理性判断：

- `PartnerManagementPage.tsx` 是既有公共主体管理大文件。本轮只在已有免密登录操作中加入原因弹窗和 service 参数，没有继续扩展审计表格、账号表格或端内配置表单；暂不为了这一处交互拆新组件。
- `PortalDirectLoginSupport.java` 仍只负责免密 token 生成、票据审计和消费校验；本轮增加 `reason` 校验属于同一职责。

本轮未执行事项：

- 本轮没有新增或修改表结构，未执行 DDL。
- 本轮没有执行远程 DML。
- 本轮没有重启后端。
- 本轮没有生成真实免密代入票据；如需验证远程库 `reason` 落库，需要重启后端后做一次真实免密代入烟测。

## 2026-06-04 端登录实现检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成三端独立认证的第一批后端落地。

已完成：

- 新增 `PortalTokenSupport`，卖家端/买家端 token 使用 `portal_login_tokens:` Redis 前缀和端内 claim，不复用管理端 `login_tokens:`。
- 新增 `PortalLoginResult`、`PortalLoginSession`、`PortalLoginIssue`、`PortalLoginLog`，统一承载端登录返回、会话和日志。
- 新增 `/seller/login`、`/buyer/login`，账号密码只读取 `seller_account` / `buyer_account`。
- 新增 `/seller/direct-login`、`/buyer/direct-login`，消费管理端生成的免密 token；token 30 分钟有效，消费后立即删除。
- 登录成功后更新 `seller_account.last_login_time` / `buyer_account.last_login_time`，写入 `seller_login_log` / `buyer_login_log` 和 `seller_session` / `buyer_session`。
- Spring Security 仅匿名放行 `/seller/login`、`/buyer/login`、`/seller/direct-login`、`/buyer/direct-login`，管理端接口保持认证要求。

验证结果：

- `mvn -DskipTests install`：通过。
- 后端通过 `.\start-backend-local.ps1 -Restart` 重启，8080 正常监听。
- `/captchaImage`：200，验证码开关仍为关闭状态。
- 管理端 `admin / admin123` 登录成功。
- `/seller/login`：返回 `code=200`，`terminal=seller`，`expireMinutes=30`。
- `/buyer/login`：返回 `code=200`，`terminal=buyer`，`expireMinutes=30`。
- `/seller/direct-login`：第一次消费返回 `code=200`，第二次复用返回失败。
- 卖家端 token 访问管理端 `/getInfo`：业务返回 `code=401`。
- 买家端 token 访问管理端 `/getInfo`：业务返回 `code=401`。
- 远程库近 10 分钟新增：`seller_login_log=9`、`buyer_login_log=3`、`seller_session=9`、`buyer_session=3`。
- 远程库账号最后登录：`seller_account` 中已有最后登录账号 1 个，`buyer_account` 中已有最后登录账号 1 个。

未完成：

- `seller_dept` / `buyer_dept` 管理端后端配置已在后续检查点完成；管理端前端页面和账号部门绑定仍未做。
- 免密代入仍未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出已在后续管理端强制踢出检查点完成。

## 2026-06-04 端内菜单角色管理接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成管理端控制卖家端/买家端菜单和角色的第一批后端接口。

已完成：

- 新增 `PortalMenu`、`PortalRole`、`PortalTreeSelect`，统一卖家端/买家端菜单和角色领域对象。
- 新增 `PortalPermissionSupport`，统一端内菜单默认值、角色校验、ID 去重和树结构构建。
- 新增卖家端管理接口：
  - `/seller/admin/menus/**`
  - `/seller/admin/sellers/{sellerId}/roles/**`
- 新增买家端管理接口：
  - `/buyer/admin/menus/**`
  - `/buyer/admin/buyers/{buyerId}/roles/**`
- 新增 `SellerPortalPermissionMapper` / `BuyerPortalPermissionMapper`，数据只读写 `seller_menu` / `buyer_menu`、`seller_role` / `buyer_role`、`seller_role_menu` / `buyer_role_menu`。
- 更新 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，补齐管理端按钮权限。
- 新增并执行 `RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql`，只 upsert 管理端 `sys_menu` 中用于控制端内菜单/角色的 20 个按钮权限。

远程库执行与校验：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql`。
- 执行结果：2 条语句成功。
- 权限校验：`sys_menu` 中 20 个 `seller:admin:menu:*`、`seller:admin:role:*`、`buyer:admin:menu:*`、`buyer:admin:role:*` 权限存在。

接口验证：

- 首次 `mvn -DskipTests install`：Java 编译通过，最终 repackage 因旧后端进程占用 jar 失败。
- 停止 8080 旧后端进程后重新执行 `mvn -DskipTests install`：通过。
- 后端通过 `.\start-backend-local.ps1 -Restart` 启动，8080 正常监听。
- `/captchaImage`：200。
- 管理端 `admin / admin123` 登录成功。
- 卖家端闭环验证通过：
  - 新增临时 `seller_menu`
  - 新增临时 `seller_role`
  - `seller_role_menu` 绑定菜单
  - `roleMenuTreeselect` 返回绑定菜单
  - 删除临时角色和菜单
- 买家端闭环验证通过：
  - 新增临时 `buyer_menu`
  - 新增临时 `buyer_role`
  - `buyer_role_menu` 绑定菜单
  - `roleMenuTreeselect` 返回绑定菜单
  - 删除临时角色和菜单
- 无 token 访问 `/seller/admin/menus/list`：返回业务 `code=401`。
- 无 token 访问 `/buyer/admin/menus/list`：返回业务 `code=401`。

未完成：

- 管理端前端页面尚未接入这些菜单/角色配置接口。

## 2026-06-04 端账号角色绑定与端内权限读取检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成端账号绑定端内角色，以及卖家端/买家端登录后读取端内角色、权限和菜单的第一批后端闭环。

已完成：

- 新增 `PortalAccountRoleAssign` 和 `PortalPermissionInfo`，分别承载端账号角色绑定请求和端内权限返回对象。
- `PortalTokenSupport` 增加端 token 解析能力，可按 `seller` / `buyer` 校验 terminal 并读取 `portal_login_tokens:` Redis 会话。
- 卖家管理端新增账号角色接口：
  - `GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/roles`
  - `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/roles`
- 买家管理端新增账号角色接口：
  - `GET /buyer/admin/buyers/{buyerId}/accounts/{accountId}/roles`
  - `PUT /buyer/admin/buyers/{buyerId}/accounts/{accountId}/roles`
- 卖家端新增端内会话接口：
  - `GET /seller/getInfo`
  - `GET /seller/getRouters`
- 买家端新增端内会话接口：
  - `GET /buyer/getInfo`
  - `GET /buyer/getRouters`
- `SellerPortalPermissionMapper` / `BuyerPortalPermissionMapper` 增加端账号角色、权限 code 和菜单树查询，数据只读取 `seller_*` / `buyer_*` 表。
- 修复端内权限读取 SQL：去掉 `select distinct role_key/perms` 查询中按未选出字段排序的问题，避免 MySQL `DISTINCT + ORDER BY` 报错。

接口验证：

- `mvn -DskipTests install`：通过。
- 后端通过 `.\start-backend-local.ps1 -Restart` 重启，8080 正常监听。
- `/captchaImage`：200。
- 管理端 `admin / admin123` 登录成功。
- 卖家端闭环验证通过：
  - 选择卖家主体 `sellerId=9`、端账号 `accountId=8`。
  - 新增临时 `seller_menu` 和 `seller_role`。
  - 通过管理端接口把临时角色绑定到端账号。
  - 管理端免密代入后，`/seller/getInfo` 返回临时 `roleKey` 和 `perms`。
  - `/seller/getRouters` 返回临时菜单。
  - 无 token 访问 `/seller/getInfo` 返回业务 `code=401`。
  - 验证完成后恢复端账号原角色并删除临时角色和菜单。
- 买家端闭环验证通过：
  - 选择买家主体 `buyerId=2`、端账号 `accountId=2`。
  - 新增临时 `buyer_menu` 和 `buyer_role`。
  - 通过管理端接口把临时角色绑定到端账号。
  - 管理端免密代入后，`/buyer/getInfo` 返回临时 `roleKey` 和 `perms`。
  - `/buyer/getRouters` 返回临时菜单。
  - 无 token 访问 `/buyer/getInfo` 返回业务 `code=401`。
  - 验证完成后恢复端账号原角色并删除临时角色和菜单。

未完成：

- 管理端前端页面尚未接入端内菜单、角色和账号角色绑定接口。
- `seller_dept` / `buyer_dept` 管理端后端配置已完成；管理端前端页面和账号部门绑定仍未做。
- 端内业务接口还未逐步接入 token 推导主体范围。
- 免密代入仍未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出已在后续管理端强制踢出检查点完成。

## 2026-06-04 端内部门管理接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成卖家端/买家端独立部门的第一批管理端后端接口。

已完成：

- 新增 `PortalDept`，统一承载卖家端/买家端部门字段。
- 新增 `PortalDeptSupport`，统一部门默认值、状态校验、父级校验、祖级路径和树结构构建。
- 卖家端新增管理端部门接口：
  - `GET /seller/admin/sellers/{sellerId}/depts/list`
  - `GET /seller/admin/sellers/{sellerId}/depts/{deptId}`
  - `GET /seller/admin/sellers/{sellerId}/depts/treeselect`
  - `POST /seller/admin/sellers/{sellerId}/depts`
  - `PUT /seller/admin/sellers/{sellerId}/depts`
  - `DELETE /seller/admin/sellers/{sellerId}/depts/{deptId}`
- 买家端新增管理端部门接口：
  - `GET /buyer/admin/buyers/{buyerId}/depts/list`
  - `GET /buyer/admin/buyers/{buyerId}/depts/{deptId}`
  - `GET /buyer/admin/buyers/{buyerId}/depts/treeselect`
  - `POST /buyer/admin/buyers/{buyerId}/depts`
  - `PUT /buyer/admin/buyers/{buyerId}/depts`
  - `DELETE /buyer/admin/buyers/{buyerId}/depts/{deptId}`
- 部门数据分别读写 `seller_dept` / `buyer_dept`，不复用 `sys_dept`。
- 删除部门前会检查同端子部门和端账号 `dept_id` 占用，避免账号挂到已删除部门。
- 更新 `RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql` 和 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，补齐 10 个管理端部门按钮权限。

远程库执行与校验：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql`。
- 执行方式：使用本机 Maven 依赖中的 MySQL JDBC 驱动执行 SQL。
- 执行结果：1 条 upsert 语句成功。
- 权限校验：`sys_menu` 中 10 个 `seller:admin:dept:*`、`buyer:admin:dept:*` 权限存在。

接口验证：

- `mvn -DskipTests install`：通过。
- 后端通过 `.\start-backend-local.ps1 -Restart` 重启，8080 正常监听。
- `/captchaImage`：200。
- 管理端 `admin / admin123` 登录成功。
- 卖家端部门闭环验证通过：
  - 选择卖家主体 `sellerId=9`。
  - 新增临时 `seller_dept`。
  - 查询列表、详情和树选择均返回临时部门。
  - 修改临时部门名称和排序后可查询到更新结果。
  - 删除临时部门后列表不可见。
  - 无 token 访问部门列表返回业务 `code=401`。
- 买家端部门闭环验证通过：
  - 选择买家主体 `buyerId=2`。
  - 新增临时 `buyer_dept`。
  - 查询列表、详情和树选择均返回临时部门。
  - 修改临时部门名称和排序后可查询到更新结果。
  - 删除临时部门后列表不可见。
  - 无 token 访问部门列表返回业务 `code=401`。

未完成：

- 管理端前端页面已在后续 UI 检查点接入端账号、账号角色绑定、端内部门、端内角色和端内菜单配置；登录/操作日志与 ticket 审计页面仍未接入。
- 端账号 `dept_id` 字段已接入后端新增/编辑账号流程和前端 service/type 契约；管理端页面弹窗仍未接入。
- 免密代入仍未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出已在后续管理端强制踢出检查点完成。

## 2026-06-04 端账号部门绑定检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成卖家端/买家端员工账号归属端内部门的第一批后端闭环和前端契约。

已完成：

- `PortalAccount` 增加 `deptId` / `deptName`，作为卖家端和买家端账号共用的端内部门字段。
- 卖家端账号列表、详情、登录查询和主账号查询已映射 `seller_account.dept_id`，并通过 `seller_dept` 返回 `deptName`。
- 买家端账号列表、详情、登录查询和主账号查询已映射 `buyer_account.dept_id`，并通过 `buyer_dept` 返回 `deptName`。
- 新增卖家端账号时可写入 `dept_id`，并校验部门必须属于同一个 `seller_id`。
- 新增买家端账号时可写入 `dept_id`，并校验部门必须属于同一个 `buyer_id`。
- 新增管理端编辑端账号接口：
  - `PUT /seller/admin/sellers/{sellerId}/accounts`
  - `PUT /buyer/admin/buyers/{buyerId}/accounts`
- 编辑端账号时保留原登录账号，允许更新昵称、邮箱、手机号、状态、备注和 `dept_id`。
- 普通新增端账号如果未传 `accountRole`，默认从 `OWNER` 调整为 `STAFF`；主体创建主账号时仍显式使用 `OWNER`。
- 前端补齐账号部门契约：
  - `PortalAccountBase.deptId`
  - `PortalAccountBase.deptName`
  - `PortalDept`
  - `PortalTreeNode`
  - `getAdminSellerDepts`
  - `getAdminSellerDeptTree`
  - `updateAdminSellerAccount`
  - `getAdminBuyerDepts`
  - `getAdminBuyerDeptTree`
  - `updateAdminBuyerAccount`

验证结果：

- `mvn -DskipTests install`：通过；首次因旧 8080 进程占用 `ruoyi-admin.jar` 导致 repackage 失败，停止旧进程后重新执行通过。
- `.\start-backend-local.ps1 -Restart`：已重启后端。
- `GET /captchaImage`：HTTP 200。
- `npm run tsc`：通过。
- 卖家端账号部门绑定闭环通过：
  - 使用管理端 `admin / admin123` 登录。
  - 选择卖家主体 `sellerId=9` 和端账号。
  - 创建临时 `seller_dept`。
  - 调用 `PUT /seller/admin/sellers/{sellerId}/accounts` 绑定临时部门。
  - 再查账号列表，返回的 `deptId` 和 `deptName` 与临时部门一致。
  - 恢复原 `deptId`，删除临时部门。
- 买家端账号部门绑定闭环通过：
  - 使用管理端 `admin / admin123` 登录。
  - 选择买家主体 `buyerId=2` 和端账号。
  - 创建临时 `buyer_dept`。
  - 调用 `PUT /buyer/admin/buyers/{buyerId}/accounts` 绑定临时部门。
  - 再查账号列表，返回的 `deptId` 和 `deptName` 与临时部门一致。
  - 恢复原 `deptId`，删除临时部门。
- 临时数据清理校验：`sellerCodexDeptCount=0; buyerCodexDeptCount=0`。

大文件合理性判断：

- `SellerServiceImpl.java` 和 `BuyerServiceImpl.java` 当前均约 403 行，已触发 400 行判断阈值。本轮新增逻辑只是在既有账号新增/更新链路中补 `dept_id` 校验和写入，职责仍属于当前服务的主体账号管理范围；此时拆分会把既有主体资料、登录、账号逻辑一起牵动，改动面超过本轮目标。
- `SellerMapper.xml` 和 `BuyerMapper.xml` 当前均约 337 行，已触发 300 行判断阈值。本轮只补账号查询字段、部门 join 和 `dept_id` 写入，仍在同一个 Mapper 表范围内。
- 后续继续接入端内操作日志或更多端内业务接口时，应优先考虑拆分账号服务、登录服务和主体资料服务，避免继续扩大 `SellerServiceImpl` / `BuyerServiceImpl`。

未完成：

- 管理端页面尚未提供端账号新增/编辑弹窗和部门树选择控件。
- 管理端页面已在后续 UI 检查点接入端账号、账号角色绑定、端内部门、端内角色和端内菜单配置；登录/操作日志与 ticket 审计页面仍未接入。
- 强制踢出已在后续管理端强制踢出检查点完成。

## 2026-06-04 免密代入审计票据检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成管理端免密代入卖家端、买家端的审计票据落地。

已完成：

- 新增 `portal_direct_login_ticket` 表，用于记录管理端免密代入票据。
- 新增 `PortalDirectLoginTicket`、`PortalDirectLoginTicketMapper` 和 `PortalDirectLoginTicketMapper.xml`。
- `PortalDirectLoginSupport` 从 Redis-only 改为 DB ticket + Redis payload：
  - 生成免密 token 时写入 `portal_direct_login_ticket`。
  - DB 只保存 `token_hash`，不保存 token 明文。
  - 票据记录 `terminal`、目标主体、目标账号、acting admin、过期时间、使用时间、使用 IP 和状态。
  - 消费前校验 ticket 仍为 `ISSUED` 且未过期。
  - 消费成功后原子更新 ticket 为 `USED`，再删除 Redis token。
  - 同一 token 第二次消费会失败。
- `PortalDirectLoginResult` 和前端 `DirectLoginResult` 类型补充 `ticketId`，方便管理端后续审计展示或跳转。
- 新增远程库执行记录：`docs/plans/2026-06-04-portal-direct-login-ticket-db-execution-record.md`。
- 更新初始化脚本 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，后续初始化也会包含该票据表。

远程库执行与验证：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`。
- 执行结果：成功。
- 表结构校验：`portal_direct_login_ticket` 存在，字段数 19，索引数 5。
- 明文 token 检查：表结构不包含明文 token 字段。

接口闭环验证：

- `mvn -DskipTests install`：通过；首次因旧 8080 进程占用 jar 导致 repackage 失败，停止旧进程后重新执行通过。
- `npm run tsc`：通过。
- `.\start-backend-local.ps1 -Restart`：已启动后端。
- `GET /captchaImage`：HTTP 200。
- 管理端 `admin / admin123` 登录：成功。
- 卖家免密代入：
  - `POST /seller/admin/sellers/{sellerId}/directLogin` 返回 `code=200`，返回 `ticketId`。
  - `GET /seller/direct-login?directLoginToken=...` 第一次返回 `code=200`。
  - 同一 token 第二次消费返回 `code=500`。
  - 远程库对应 ticket：`status=USED`，`used_time` 已写入，`used_ip` 已写入，`token_hash` 长度 64，hash 匹配，未保存 token 明文。
- 买家免密代入：
  - `POST /buyer/admin/buyers/{buyerId}/directLogin` 返回 `code=200`，返回 `ticketId`。
  - `GET /buyer/direct-login?directLoginToken=...` 第一次返回 `code=200`。
  - 同一 token 第二次消费返回 `code=500`。
  - 远程库对应 ticket：`status=USED`，`used_time` 已写入，`used_ip` 已写入，`token_hash` 长度 64，hash 匹配，未保存 token 明文。

大文件合理性判断：

- 本轮没有继续扩大 `SellerServiceImpl` / `BuyerServiceImpl` 主体服务；免密审计能力集中在 `PortalDirectLoginSupport` 与独立 ticket mapper。
- `PortalDirectLoginSupport` 当前约 210 行，职责仍单一：端免密 token 生成、票据审计、消费校验。
- 新增 ticket mapper/xml 只负责 `portal_direct_login_ticket`，没有混入卖家/买家业务查询。

未完成：

- 管理端页面尚未提供 ticket 审计列表入口。
- 强制踢出已在后续管理端强制踢出检查点完成。

## 2026-06-04 管理端账号 UI 接入检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板做好，买家替换配置和 service”的方式接入管理端端账号维护 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`。
- 卖家管理、买家管理主体行新增“账号”入口。
- 卖家/买家共用同一个账号弹窗，按 `PartnerModuleConfig` 注入：
  - 主体 ID 字段
  - 端账号 ID 字段
  - 账号列表 service
  - 新增账号 service
  - 编辑账号 service
  - 部门树 service
  - 重置默认密码 service
- 账号弹窗支持：
  - 查看端账号列表
  - 新增端账号，默认初始密码 `U12346`
  - 编辑端账号
  - 绑定端内部门树
  - 维护账号角色字段
  - 维护账号状态、手机、邮箱、备注
  - 重置端账号默认密码
- 新增账号默认角色为 `STAFF`；已有 `OWNER` 可展示，但新增时前端不主动创建第二个负责人账号。
- 主体列表去掉强制横向 `scroll.x`，继续使用紧凑单元格和 `tableLayout="fixed"`，避免页面主动生成横向滚动条。

验证结果：

- `npm run tsc`：通过。
- `git diff --check -- react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx react-ui/src/pages/Seller/index.tsx react-ui/src/pages/Buyer/index.tsx`：通过，仅出现 Git CRLF 提示。
- 浏览器验收：管理端登录成功；卖家管理可打开账号弹窗和新增账号表单；买家管理可打开账号弹窗。
- 浏览器 console error：0。
- 截图证据：`logs/screenshots/2026-06-04-buyer-account-modal.png`。

未完成：

- 管理端端内部门、菜单、角色独立配置页面仍未接入；账号角色绑定已在账号弹窗中接入。
- 管理端 ticket 审计列表入口尚未接入。
- 强制踢出已在后续管理端强制踢出检查点完成。

## 2026-06-04 管理端强制踢出检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成管理端对卖家端、买家端在线会话的强制踢出能力。

已完成：

- `PortalTokenSupport` 新增端内 token 批量删除方法，删除范围限定在 `portal_login_tokens:{terminal}:{tokenId}`。
- 卖家端新增主体级和账号级强制踢出：
  - `DELETE /seller/admin/sellers/{sellerId}/sessions`
  - `DELETE /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions`
- 买家端新增主体级和账号级强制踢出：
  - `DELETE /buyer/admin/buyers/{buyerId}/sessions`
  - `DELETE /buyer/admin/buyers/{buyerId}/accounts/{accountId}/sessions`
- 强制踢出会：
  - 从 `seller_session` / `buyer_session` 查询在线 token。
  - 更新 session `status = '1'`。
  - 写入 `logout_time`。
  - 删除 Redis 中对应端内 token。
- 主体或端账号被停用时，Service 会同步调用强制踢出逻辑。
- 强制踢出接口改为幂等返回：即使当前没有在线会话，也返回 `code=200`，`data=0`。
- 前端卖家/买家主体行“更多”菜单新增“强制踢出”。
- 前端卖家/买家账号弹窗账号行新增“强制踢出”。
- 新增远程库执行记录：`docs/plans/2026-06-04-portal-force-logout-menu-db-execution-record.md`。

远程库执行与验证：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_force_logout_menu_seed.sql`。
- 执行结果：`executedStatements=1`。
- 权限校验：`seller:admin:forceLogout` / `buyer:admin:forceLogout` 共 2 个权限点存在。

接口闭环验证：

- `mvn -DskipTests install`：通过；首次因旧 8080 进程占用 jar 导致 repackage 失败，停止旧进程后重新执行通过。
- `npm run tsc`：通过。
- `.\start-backend-local.ps1 -Restart`：已启动后端。
- 管理端 `admin / admin123` 登录：成功。
- 卖家账号级强退：
  - 强退前 `/seller/getInfo` 返回 `code=200`。
  - `DELETE /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions` 返回 `code=200`，`data=1`。
  - 强退后同 token 调 `/seller/getInfo` 返回 `code=401`。
  - 重复强退返回 `code=200`，`data=0`。
  - `seller_session` 对应 token：`status=1`，`logout_time` 已写入。
- 买家账号级强退：
  - 强退前 `/buyer/getInfo` 返回 `code=200`。
  - `DELETE /buyer/admin/buyers/{buyerId}/accounts/{accountId}/sessions` 返回 `code=200`，`data=1`。
  - 强退后同 token 调 `/buyer/getInfo` 返回 `code=401`。
  - 重复强退返回 `code=200`，`data=0`。
  - `buyer_session` 对应 token：`status=1`，`logout_time` 已写入。
- 卖家主体级强退：
  - 强退前 `/seller/getInfo` 返回 `code=200`。
  - `DELETE /seller/admin/sellers/{sellerId}/sessions` 返回 `code=200`，`data=1`。
  - 强退后同 token 调 `/seller/getInfo` 返回 `code=401`。
  - 重复强退返回 `code=200`，`data=0`。
- 买家主体级强退：
  - 强退前 `/buyer/getInfo` 返回 `code=200`。
  - `DELETE /buyer/admin/buyers/{buyerId}/sessions` 返回 `code=200`，`data=1`。
  - 强退后同 token 调 `/buyer/getInfo` 返回 `code=401`。
  - 重复强退返回 `code=200`，`data=0`。

浏览器验证：

- 卖家管理主体行“更多”菜单已展示“强制踢出”。
- 卖家账号弹窗账号行已展示“强制踢出”。
- 浏览器 console error：0。
- 截图证据：`logs/screenshots/2026-06-04-seller-force-logout-account-modal.png`。

大文件合理性判断：

- `SellerServiceImpl.java` / `BuyerServiceImpl.java` 继续超过 400 行。本轮新增的是账号/主体控制流里的会话作废逻辑，和当前服务已有账号状态、登录、免密代入职责相关；为避免一次性拆动主体、账号、登录三类历史逻辑，本轮保持在原服务内。
- 后续接入登录/操作日志页面或更多端内业务接口时，应优先拆分登录会话控制服务，避免继续扩大主体服务类。

未完成：

- 管理端端内部门、菜单、角色独立配置页面仍未接入；账号角色绑定已在账号弹窗中接入。
- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端账号角色绑定 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按已确认的“卖家一套做好，再复制成买家，只替换配置和 service”方式接入管理端端账号角色绑定 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerAccountRoleModal.tsx`，单独承载端账号角色分配弹窗。
- `PartnerManagementPage` 的公共 service 契约补充：
  - `getAccountRoles`
  - `assignAccountRoles`
- 卖家管理接入：
  - `getAdminSellerAccountRoles`
  - `assignAdminSellerAccountRoles`
- 买家管理接入：
  - `getAdminBuyerAccountRoles`
  - `assignAdminBuyerAccountRoles`
- `PartnerAccountModal` 账号行新增“分配角色”入口，按 `seller:admin:role:edit` / `buyer:admin:role:edit` 权限展示。
- 账号行操作调整为：高频“编辑”“分配角色”直接展示，低频“重置密码”“强制踢出”收进“更多”，继续遵守复用台账中的表格操作规则。
- 主体管理公共组件中的 Ant Design `Space direction` 旧写法已替换为 `Flex vertical`，避免浏览器验证时出现弃用告警。

验证结果：

- `npm run tsc`：通过。
- Playwright 浏览器验证：
  - 管理端登录成功。
  - 卖家管理从实际菜单进入 `/partner/seller`。
  - 卖家账号弹窗可打开，账号行展示“分配角色”，角色弹窗可打开。
  - 买家管理进入 `/partner/buyer`。
  - 买家账号弹窗可打开，账号行展示“分配角色”，角色弹窗可打开。
  - 卖家和买家的浏览器 console 均为 `0 errors / 0 warnings`。
- 截图证据：
  - `logs/screenshots/2026-06-04-seller-account-role-modal.png`
  - `logs/screenshots/2026-06-04-buyer-account-role-modal.png`

大文件合理性判断：

- `PartnerAccountRoleModal.tsx` 独立拆出，避免继续把角色分配逻辑堆进账号弹窗。
- `PartnerAccountModal.tsx` 当前约 507 行，触发 500 行判断阈值。本轮只新增角色弹窗入口和操作列收敛，真实角色分配表单已拆到独立组件；为保持本轮模板化提速，不在本批拆账号表格和账号表单。
- `PartnerManagementPage.tsx` 属于既有公共主体管理大文件，本轮只补 service 契约并替换旧 UI API。后续如果继续扩展日志或审计页面，不应再扩大该文件，应新建独立公共配置组件或按菜单拆页。

未完成：

- 管理端端内菜单配置已在后续 UI 检查点完成；日志和审计页面仍未接入。
- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端端内菜单 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按已确认的“卖家一套做好，买家只替换配置和 service”的模板化方式接入管理端卖家端/买家端菜单维护 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx`，单独承载端维度菜单列表、新增/编辑表单和删除入口。
- `react-ui/src/types/seller-buyer/party.d.ts` 补齐端内菜单列表和菜单详情结果类型，并补充 `PortalMenu.remark` 字段。
- 卖家 service 补齐端内菜单维护接口：
  - `getAdminSellerMenus`
  - `getAdminSellerMenu`
  - `addAdminSellerMenu`
  - `updateAdminSellerMenu`
  - `removeAdminSellerMenu`
- 买家 service 按同一模板补齐端内菜单维护接口：
  - `getAdminBuyerMenus`
  - `getAdminBuyerMenu`
  - `addAdminBuyerMenu`
  - `updateAdminBuyerMenu`
  - `removeAdminBuyerMenu`
- `PartnerManagementPage` 公共 service 契约补充：
  - `listMenus`
  - `getMenu`
  - `addMenu`
  - `updateMenu`
  - `removeMenu`
- 卖家/买家管理页工具栏新增“菜单配置”，通过 `seller:admin:menu:list` / `buyer:admin:menu:list` 权限展示。
- 菜单配置弹窗支持：
  - 查看端内菜单树。
  - 新增菜单。
  - 编辑菜单。
  - 删除菜单。
  - 维护上级菜单、菜单类型、菜单名称、显示顺序、图标、外链、路由地址、组件路径、路由参数、路由名称、权限标识、是否缓存、显示状态、菜单状态和备注。
- 本轮没有提交新增/编辑/删除表单，没有写入远程业务数据；浏览器验证只打开弹窗和新增表单。

验证结果：

- `npm run tsc`：通过。
- 浏览器验证：
  - 卖家管理 `/partner/seller` 工具栏展示“菜单配置”。
  - 卖家端菜单配置弹窗可打开。
  - 卖家新增菜单表单可打开，包含上级菜单、菜单类型、菜单名称和权限标识等字段。
  - 卖家浏览器 console 为 `0 errors / 0 warnings`。
  - 买家管理 `/partner/buyer` 工具栏展示“菜单配置”。
  - 买家端菜单配置弹窗可打开。
  - 买家新增菜单表单可打开，包含上级菜单、菜单类型、菜单名称和权限标识等字段。
  - 买家浏览器 console 为 `0 errors / 0 warnings`。
- 截图证据：
  - `logs/screenshots/2026-06-04-seller-menu-modal.png`
  - `logs/screenshots/2026-06-04-buyer-menu-modal.png`

大文件合理性判断：

- `PartnerMenuModal.tsx` 当前约 499 行，触发 400 行判断阈值并接近 500 行阈值；它只承载端内菜单表格、菜单树和菜单表单，职责仍然单一，暂不拆分。
- `PartnerManagementPage.tsx` 当前约 1122 行，属于既有公共主体管理大文件。本轮只增加“菜单配置”入口、状态和 service 契约，具体菜单表格和表单已拆到 `PartnerMenuModal.tsx`。
- 后续日志和审计页面不应继续堆进 `PartnerManagementPage.tsx`，应继续新建独立公共组件或独立页面。

未完成：

- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端端内部门 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家一套做好，买家只替换配置和 service”的模板化方式接入管理端卖家端/买家端部门维护 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerDeptModal.tsx`，单独承载端内部门列表和新增/编辑表单。
- 卖家 service 补齐端内部门维护接口：
  - `getAdminSellerDept`
  - `addAdminSellerDept`
  - `updateAdminSellerDept`
  - `removeAdminSellerDept`
- 买家 service 补齐端内部门维护接口：
  - `getAdminBuyerDept`
  - `addAdminBuyerDept`
  - `updateAdminBuyerDept`
  - `removeAdminBuyerDept`
- `PartnerManagementPage` 公共 service 契约补充：
  - `listDepts`
  - `addDept`
  - `updateDept`
  - `removeDept`
- 卖家/买家主体行“更多”菜单新增“部门”，通过 `seller:admin:dept:list` / `buyer:admin:dept:list` 权限展示。
- 部门弹窗支持：
  - 查看端内部门列表。
  - 新增部门。
  - 编辑部门。
  - 删除部门。
  - 维护上级部门、部门名称、排序、负责人、电话、邮箱和状态。
- 表格操作继续保持最多两个直接文字操作；部门入口放入主体行“更多”，避免主体列表操作列变宽。

验证结果：

- `npm run tsc`：通过。
- Playwright 浏览器验证：
  - 买家管理 `/partner/buyer` 主体行“更多”展示“部门”。
  - 买家部门弹窗可打开。
  - 买家新增部门表单可打开。
  - 修复新增部门表单 `useForm` 挂载警告后，买家浏览器 console 为 `0 errors / 0 warnings`。
  - 卖家管理 `/partner/seller` 主体行“更多”展示“部门”。
  - 卖家部门弹窗可打开。
  - 卖家浏览器 console 为 `0 errors / 0 warnings`。
- 截图证据：
  - `logs/screenshots/2026-06-04-buyer-dept-modal.png`
  - `logs/screenshots/2026-06-04-seller-dept-modal.png`

大文件合理性判断：

- `PartnerDeptModal.tsx` 独立拆出，避免继续扩大 `PartnerManagementPage.tsx`。
- `PartnerManagementPage.tsx` 本轮只增加部门弹窗入口、状态和 service 契约，未承载部门表格或表单细节。
- 后续日志和审计页面应继续新建独立公共组件或独立页面，不应继续堆进 `PartnerManagementPage.tsx`。

未完成：

- 管理端端内菜单配置和端内角色维护已在后续 UI 检查点完成。
- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端端内角色 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按已确认的“卖家一套做好，买家只替换配置和 service”的模板化方式接入管理端卖家端/买家端角色维护 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerRoleModal.tsx`，单独承载某个卖家或买家主体下的端内角色列表、新增/编辑表单、状态切换和删除入口。
- `react-ui/src/types/seller-buyer/party.d.ts` 补齐端内角色、端内菜单、角色分页、角色详情、菜单树和角色菜单树类型。
- 卖家 service 补齐端内菜单和角色维护接口：
  - `getAdminSellerMenuTree`
  - `getAdminSellerRoleMenuTree`
  - `getAdminSellerRoles`
  - `getAdminSellerRole`
  - `addAdminSellerRole`
  - `updateAdminSellerRole`
  - `changeAdminSellerRoleStatus`
  - `removeAdminSellerRoles`
- 买家 service 按同一模板补齐端内菜单和角色维护接口：
  - `getAdminBuyerMenuTree`
  - `getAdminBuyerRoleMenuTree`
  - `getAdminBuyerRoles`
  - `getAdminBuyerRole`
  - `addAdminBuyerRole`
  - `updateAdminBuyerRole`
  - `changeAdminBuyerRoleStatus`
  - `removeAdminBuyerRoles`
- `PartnerManagementPage` 公共 service 契约补充：
  - `getMenuTree`
  - `getRoleMenuTree`
  - `listRoles`
  - `getRole`
  - `addRole`
  - `updateRole`
  - `changeRoleStatus`
  - `removeRoles`
- 卖家/买家主体行“更多”菜单新增“角色”，通过 `seller:admin:role:list` / `buyer:admin:role:list` 权限展示。
- 角色弹窗支持：
  - 查看当前主体下的端内角色列表。
  - 新增角色。
  - 编辑角色。
  - 删除角色。
  - 维护角色名称、权限字符、显示顺序、状态、备注和菜单权限树。
- 本轮没有提交新增/编辑/删除表单，没有写入远程业务数据；浏览器验证只打开弹窗和新增表单。

验证结果：

- `npm run tsc`：通过。
- 浏览器验证：
  - 卖家管理 `/partner/seller` 主体行“更多”展示“角色”。
  - 卖家角色弹窗可打开。
  - 卖家新增角色表单可打开，包含角色名称、权限字符、状态和菜单权限。
  - 卖家浏览器 console 为 `0 errors / 0 warnings`。
  - 买家管理 `/partner/buyer` 主体行“更多”展示“角色”。
  - 买家角色弹窗可打开。
  - 买家新增角色表单可打开，包含角色名称、权限字符、状态和菜单权限。
  - 买家浏览器 console 为 `0 errors / 0 warnings`。
- 截图证据：
  - `logs/screenshots/2026-06-04-seller-role-modal.png`
  - `logs/screenshots/2026-06-04-buyer-role-modal.png`

大文件合理性判断：

- `PartnerRoleModal.tsx` 当前约 409 行，触发 400 行判断阈值；它只承载端内角色表格、角色表单和菜单树勾选，职责仍然单一，暂不拆分。
- `PartnerManagementPage.tsx` 当前约 1100 行，属于既有公共主体管理大文件。本轮只增加角色弹窗入口、状态和 service 契约，具体角色表格和表单已拆到 `PartnerRoleModal.tsx`。
- 后续日志和审计页面不应继续堆进 `PartnerManagementPage.tsx`，应继续新建独立公共组件或独立页面。

未完成：

- 管理端端内菜单配置已在后续 UI 检查点完成；日志和审计页面仍未接入。
- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端 UI 第八批收口检查点

当前管理端卖家/买家页面已按模板化方式完成第一批端内控制 UI：

- 端账号维护：已接入 `PartnerAccountModal`。
- 端账号角色绑定：已接入 `PartnerAccountRoleModal`。
- 端内部门维护：已接入 `PartnerDeptModal`。
- 端内角色维护：已接入 `PartnerRoleModal`。
- 端内菜单维护：已接入 `PartnerMenuModal`。

当前剩余前端控制项：

- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

验证汇总：

- `npm run tsc`：通过。
- `git diff --check`：通过，仅出现 Git CRLF 提示。
- 浏览器验证已覆盖卖家/买家端账号角色、端内部门、端内角色、端内菜单弹窗；本轮验证未提交新增/编辑/删除表单。

## 2026-06-04 端内权限校验基础设施检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成卖家端、买家端接口级权限校验的基础设施。

已完成：

- 新增 `@PortalPreAuthorize`，用于声明端类型、全部权限要求和任一权限要求。
- 新增 `PortalPreAuthorizeAspect`，在端内接口执行前统一进入权限校验。
- 新增 `PortalPermissionChecker`，统一解析端 token、校验端类型、读取端内权限集合，并支持若依超级权限 `*:*:*`。
- 新增 `IPortalPermissionCheckService`，让卖家端、买家端权限服务以同一契约接入统一校验器。
- `SellerPortalPermissionServiceImpl`、`BuyerPortalPermissionServiceImpl` 已实现该契约。
- `PortalPermissionSupport` 已补齐权限匹配方法。
- 新增 `PortalPermissionSupportTest` 覆盖端内权限匹配规则。
- `/seller/getInfo`、`/seller/getRouters`、`/buyer/getInfo`、`/buyer/getRouters` 已接入 `@PortalPreAuthorize`。
- 登录失效通过端内权限守卫返回 `code=401`，权限不足返回 `code=403`。
- `AGENTS.md` 已补充：已确认的同构管理端 UI 模式按模板化推进，卖家侧做好后买家侧只替换端类型、文案、路由、权限标识、字段配置和 service。

验证结果：

- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过。
- `mvn -DskipTests compile`：通过。
- `mvn -DskipTests install`：通过。
- `.\start-backend-local.ps1 -Restart`：后端已重启。
- 管理端登录成功。
- 卖家端免密登录后 `/seller/getInfo` 返回 `code=200`。
- 买家端免密登录后 `/buyer/getInfo` 返回 `code=200`。
- 无 token 访问 `/seller/getInfo` 返回 `code=401`。
- 卖家端 token 访问 `/seller/getRouters` 返回 `code=200`。
- 买家端 token 访问 `/buyer/getRouters` 返回 `code=200`。
- 卖家端 token 访问 `/buyer/getInfo` 返回 `code=401`。

未完成：

- 卖家端、买家端会话接口已接入 `@PortalPreAuthorize`；真实端内业务接口尚未批量接入。
- 后续真实端内业务接口仍必须从端 token 推导 `sellerId` / `buyerId`，不能相信前端传入主体 ID。
- 本轮未改管理端前端代码，未运行 `npm run tsc`。

## 2026-06-04 旧方向文档审计检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成阶段 0 中“审计当前文档，标记旧方向文档为过期或待迁移”的收口。

已标记过期的历史方案：

- `docs/architecture/2026-06-03-three-portal-buyer-seller-plan.md`
- `docs/architecture/2026-06-03-seller-buyer-split-module-design.md`
- `docs/architecture/2026-06-03-admin-portal-start-plan.md`
- `docs/architecture/2026-06-03-admin-customer-field-alignment.md`
- `docs/architecture/2026-06-03-remote-seller-buyer-table-migration-plan.md`
- `docs/architecture/2026-06-03-multi-user-marketplace-best-architecture.md`
- `docs/plans/2026-06-04-seller-buyer-operations-implementation-plan.md`
- `docs/plans/2026-06-04-seller-buyer-full-remediation-plan.md`

说明：

- 上述文档作为历史记录保留，正文不删除。
- 其中“卖家/买家账号继续复用若依 `sys_user` / `sys_role` / `sys_menu`”的设计已废弃。
- 后续三端独立账号权限改造只以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 和本目标追踪为准。

代码同步修复：

- 修复 `AdminSellerController` 中卖家账号编辑操作日志标题乱码。
- 修复 `AdminBuyerController` 中买家账号编辑操作日志标题乱码。

验证结果：

- `rg -n -F "鍗"`：卖家、买家管理相关代码无匹配。
- `rg -n -F "涔"`：卖家、买家管理相关代码无匹配。
- `rg -n -F "璐"`：卖家、买家管理相关代码无匹配。

## 2026-06-04 端内会话上下文检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，推进“端内业务接口从端 token 推导主体范围，不相信前端传入 `sellerId` / `buyerId`”的基础能力。

已完成：

- 新增 `PortalSessionContext`，用 ThreadLocal 保存当前请求内已校验通过的 `PortalLoginSession`。
- `PortalPreAuthorizeAspect` 在权限校验通过后写入 `PortalSessionContext`，并在请求结束时恢复或清理。
- `PortalLogAspect` 优先从 `PortalSessionContext` 读取会话，只有缺少上下文时才回退到 token 解析。
- `/seller/getInfo`、`/seller/getRouters` 已改为从 `PortalSessionContext.requireSession("seller")` 获取端内会话。
- `/buyer/getInfo`、`/buyer/getRouters` 已改为从 `PortalSessionContext.requireSession("buyer")` 获取端内会话。
- 新增 `PortalSessionContextTest`，覆盖同端读取、跨端拒绝和清理行为。

验证结果：

- `mvn -DskipTests compile`：通过。
- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过，`PortalPermissionSupportTest` 4 条、`PortalSessionContextTest` 3 条。
- `mvn -DskipTests install`：通过。
- `.\start-backend-local.ps1 -Restart`：后端已重启，8080 正常监听。
- 管理端登录：`code=200`。
- 卖家端免密登录后 `/seller/getInfo`、`/seller/getRouters` 均返回 `code=200`。
- 买家端免密登录后 `/buyer/getInfo`、`/buyer/getRouters` 均返回 `code=200`。
- 卖家端 token 访问 `/buyer/getInfo` 返回 `code=401`。

未完成：

- 当前只是给真实业务接口准备统一会话入口；后续真实 seller/buyer 业务接口仍需逐个使用 `PortalSessionContext.requireSession(...)` 派生主体范围。

## 2026-06-04 免密代入原因真实烟测检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，验证管理端免密代入原因必填链路在最新 jar 和远程数据源上真实可用。

执行环境：

- 后端启动方式：`.\start-backend-local.ps1 -Restart`。
- 运行服务：`http://127.0.0.1:8080`。
- 数据源确认：启动前已读取当前激活配置；`.env.local` 存在，当前激活配置指向远程 MySQL/Redis；未输出主机、端口、库名、凭证、密码或 token secret。
- 本轮未执行 DDL。
- 本轮执行了真实远程 DML：生成并消费卖家、买家各 1 张免密代入票据，端内登录会话随后通过管理端强制踢出接口清理。

验证结果：

- `mvn -DskipTests install`：通过，已重新打包最新 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1 -Restart`：通过，8080 正常监听，HTTP 探活返回 200。
- 管理端登录：`code=200`，验证码开关保持关闭。
- 卖家管理端 `POST /seller/admin/sellers/{sellerId}/directLogin` 未传 `reason`：返回 `code=500`，业务消息为“免密登录原因不能为空”。
- 买家管理端 `POST /buyer/admin/buyers/{buyerId}/directLogin` 未传 `reason`：返回 `code=500`，业务消息为“免密登录原因不能为空”。
- 卖家正向烟测：`sellerId=9`，生成票据 `ticketId=28`，审计列表可读回相同 `reason`，消费后状态为 `USED` 且 `usedTime` 已写入。
- 买家正向烟测：`buyerId=2`，生成票据 `ticketId=29`，审计列表可读回相同 `reason`，消费后状态为 `USED` 且 `usedTime` 已写入。
- 卖家、买家端内免密登录消费接口均返回 `code=200`。
- 卖家、买家端账号会话清理接口均返回 `code=200`。

当前判断：

- 管理端免密代入已经具备“原因必填、短时一次性、可审计、可消费、可清理会话”的第一批闭环。
- 后续管理端 UI 接入不需要重新设计原因链路，只需要复用当前公共弹窗和 service 契约。
- 已确定的 seller/buyer 同构管理端 UI 继续按模板化方式推进：卖家侧先做标准样板并验证，买家侧只替换端类型、文案、路由、权限标识、字段配置和 service。

## 2026-06-04 端内主体资料接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，推进“真实端内业务接口从端 token 推导主体范围，不相信前端传入 `sellerId` / `buyerId`”。

已完成：

- 新增 `PortalSubjectProfile`，作为卖家端/买家端主体资料只读 DTO。
- 新增卖家端接口 `GET /seller/profile`：
  - 使用 `@PortalPreAuthorize(terminal = "seller")`。
  - 使用 `@PortalLog(terminal = "seller", title = "卖家端主体资料", ...)` 写入卖家端操作日志。
  - 从 `PortalSessionContext.requireSession("seller")` 读取当前端会话，再使用 `session.subjectId` 查询主体资料。
  - 不接收前端传入 `sellerId`。
- 新增买家端接口 `GET /buyer/profile`：
  - 使用 `@PortalPreAuthorize(terminal = "buyer")`。
  - 使用 `@PortalLog(terminal = "buyer", title = "买家端主体资料", ...)` 写入买家端操作日志。
  - 从 `PortalSessionContext.requireSession("buyer")` 读取当前端会话，再使用 `session.subjectId` 查询主体资料。
  - 不接收前端传入 `buyerId`。
- 端内 profile 返回 `PortalSubjectProfile`，不直接返回管理端 `Seller` / `Buyer` 全对象，避免暴露 `createBy`、`updateBy`、`remark` 等后台字段。
- `docs/architecture/reuse-ledger.md` 已登记 `PortalSubjectProfile` 复用规则。

验证结果：

- `npm run tsc`：通过，确认管理端动态菜单缺页兜底改动无 TypeScript 错误。
- `mvn -DskipTests compile`：通过。
- `mvn -DskipTests install`：通过，已重新打包最新 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1 -Restart`：通过，后端使用 `.env.local` 重启成功。
- 本轮没有执行 DDL。
- 本轮执行了真实远程 DML：生成并消费卖家、买家各 1 张免密代入票据，用于获取端 token 后调用 profile 接口；烟测后已通过管理端强制踢出接口清理对应端账号会话。
- 卖家端 profile 烟测：
  - `sellerId=9`，`ticketId=30`。
  - `GET /seller/profile` 返回 `code=200`。
  - 返回 `terminal=seller`，`subjectId=9`。
  - 返回对象不包含 `createBy` 和 `remark`。
- 买家端 profile 烟测：
  - `buyerId=2`，`ticketId=31`。
  - `GET /buyer/profile` 返回 `code=200`。
  - 返回 `terminal=buyer`，`subjectId=2`。
  - 返回对象不包含 `createBy` 和 `remark`。
- 跨端访问验证：
  - 卖家端 token 访问 `GET /buyer/profile` 返回 `code=401`。
  - 买家端 token 访问 `GET /seller/profile` 返回 `code=401`。
- 端内操作日志验证：
  - 卖家操作日志列表存在 `operUrl=/seller/profile`、`subjectId=9` 的记录。
  - 买家操作日志列表存在 `operUrl=/buyer/profile`、`subjectId=2` 的记录。

当前判断：

- 真实端内业务接口范围控制已有第一条只读业务接口样板。
- 后续 seller/buyer 端真实业务接口继续套用该模式：端 token 推导主体 ID，接口不收前端主体 ID，进入 Service 前先过 `@PortalPreAuthorize`，操作通过 `@PortalLog` 写入端内日志。

## 2026-06-04 端内当前账号资料接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，推进卖家端/买家端账号体系独立后的“当前账号”只读接口。

已完成：

- 新增 `PortalAccountProfile`，作为卖家端/买家端当前账号资料只读 DTO。
- `ISellerService` / `SellerServiceImpl` 新增 `selectSellerAccountById(Long sellerId, Long sellerAccountId)`：
  - 先校验卖家主体存在。
  - 再按账号 ID 查询 `seller_account`。
  - 账号不存在或不属于当前卖家主体时，抛出“卖家账号不存在”。
- `IBuyerService` / `BuyerServiceImpl` 新增 `selectBuyerAccountById(Long buyerId, Long buyerAccountId)`：
  - 先校验买家主体存在。
  - 再按账号 ID 查询 `buyer_account`。
  - 账号不存在或不属于当前买家主体时，抛出“买家账号不存在”。
- 新增卖家端接口 `GET /seller/account/profile`：
  - 使用 `@PortalPreAuthorize(terminal = "seller")`。
  - 使用 `@PortalLog(terminal = "seller", title = "卖家端账号资料", ...)` 写入卖家端操作日志。
  - 从 `PortalSessionContext.requireSession("seller")` 读取当前端会话，再使用 `session.subjectId` 和 `session.accountId` 查询账号资料。
  - 不接收前端传入 `sellerId` 或 `accountId`。
- 新增买家端接口 `GET /buyer/account/profile`：
  - 使用 `@PortalPreAuthorize(terminal = "buyer")`。
  - 使用 `@PortalLog(terminal = "buyer", title = "买家端账号资料", ...)` 写入买家端操作日志。
  - 从 `PortalSessionContext.requireSession("buyer")` 读取当前端会话，再使用 `session.subjectId` 和 `session.accountId` 查询账号资料。
  - 不接收前端传入 `buyerId` 或 `accountId`。
- 端内账号资料返回 `PortalAccountProfile`，不直接返回 `SellerAccount` / `BuyerAccount` 全对象，避免暴露 `password`、`createBy`、`updateBy`、`remark` 等字段。
- `docs/architecture/reuse-ledger.md` 已登记 `PortalAccountProfile` 复用规则。

验证结果：

- 数据源确认：tracked YAML 继续使用环境变量占位，`.env.local` 存在；本轮未输出凭证。
- `mvn -DskipTests compile`：通过。
- `mvn -DskipTests install`：通过，已重新打包最新 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1 -Restart`：通过，后端使用 `.env.local` 重启成功。
- 本轮没有执行 DDL。
- 本轮执行了真实远程 DML：生成并消费卖家、买家各 1 张免密代入票据，用于获取端 token 后调用当前账号资料接口；烟测后已通过管理端强制踢出接口清理对应端账号会话。
- 卖家端当前账号资料烟测：
  - `sellerId=9`，`accountId=8`，`ticketId=32`。
  - `GET /seller/account/profile` 返回 `code=200`。
  - 返回 `terminal=seller`，`subjectId=9`，`accountId=8`。
  - 返回对象不包含 `password` 和 `createBy`。
- 买家端当前账号资料烟测：
  - `buyerId=2`，`accountId=2`，`ticketId=33`。
  - `GET /buyer/account/profile` 返回 `code=200`。
  - 返回 `terminal=buyer`，`subjectId=2`，`accountId=2`。
  - 返回对象不包含 `password` 和 `createBy`。
- 跨端访问验证：
  - 卖家端 token 访问 `GET /buyer/account/profile` 返回 `code=401`。
  - 买家端 token 访问 `GET /seller/account/profile` 返回 `code=401`。
- 端内操作日志验证：
  - 卖家操作日志列表存在 `operUrl=/seller/account/profile`、`subjectId=9`、`accountId=8` 的记录。
  - 买家操作日志列表存在 `operUrl=/buyer/account/profile`、`subjectId=2`、`accountId=2` 的记录。

当前判断：

- 端内主体资料和当前账号资料都已形成只读接口样板。
- 后续真实端内业务接口继续套用该模式：从端 token 推导主体和账号，不接收前端主体 ID 或账号 ID 作为权限边界，返回 DTO 时避免直接暴露管理端对象或密码密文字段。

## 2026-06-04 三端前端 session 模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，落实“已确认模式模板化复用”的执行方式，为后续卖家端、买家端物理拆分准备前端 session 基础层。

已完成：

- `react-ui/src/access.ts` 新增三端 token key map：
  - 管理端继续使用原有 `access_token` / `refresh_token` / `expireTime`，避免影响当前 admin 登录。
  - 卖家端预留 `seller_access_token` / `seller_refresh_token` / `seller_expireTime`。
  - 买家端预留 `buyer_access_token` / `buyer_refresh_token` / `buyer_expireTime`。
- `react-ui/src/access.ts` 新增端感知方法：
  - `setTerminalSessionToken`
  - `getTerminalAccessToken`
  - `getTerminalRefreshToken`
  - `getTerminalTokenExpireTime`
  - `clearTerminalSessionToken`
- 保留管理端现有 `setSessionToken` / `getAccessToken` / `getRefreshToken` / `getTokenExpireTime` / `clearSessionToken` 作为 admin 包装方法。
- 新增 `react-ui/src/services/portal/session.ts`：
  - 公共封装 `seller` / `buyer` 的 `/login`、`/direct-login`、`/getInfo`、`/getRouters`、`/profile`、`/account/profile`。
  - 导出 `sellerPortalSessionService` 和 `buyerPortalSessionService`，后续端内页面按模板使用，不在页面中重复拼路径。
- `react-ui/src/types/seller-buyer/party.d.ts` 补齐端内登录、权限信息、主体资料、当前账号资料的前端类型。
- `docs/architecture/reuse-ledger.md` 已登记“三端前端 session 基础层”复用规则。

验证结果：

- `npm run tsc`：通过。
- 本轮没有启动后端。
- 本轮没有执行 SQL、DDL 或远程 DML。

当前判断：

- 当前 `react-ui/` 仍作为管理端验证入口，尚未复制 `seller-ui` / `buyer-ui`。
- 后续卖家端、买家端物理拆分时，登录存储和端内 session API 可直接按该模板复用，只替换 terminal、入口配置和页面 service。

## 2026-06-04 端内账号只读列表接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，推进“真实端内业务接口从端 token 推导主体范围，不相信前端传入 `sellerId` / `buyerId`”。

已完成：

- 新增卖家端接口 `GET /seller/accounts`：
  - 使用 `@PortalPreAuthorize(terminal = "seller", hasPermi = "seller:account:list")`。
  - 使用 `@PortalLog(terminal = "seller", title = "卖家端账号列表", ...)` 写入卖家端操作日志。
  - 从 `PortalSessionContext.requireSession("seller")` 读取当前端会话，再使用 `session.subjectId` 查询当前卖家主体下账号。
  - 不接收前端传入 `sellerId`。
  - 返回 `PortalAccountProfile` 列表，不返回 `SellerAccount` 原始对象，避免暴露 `password`、`createBy`、`updateBy`、`remark` 等字段。
- 新增买家端接口 `GET /buyer/accounts`：
  - 使用 `@PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:account:list")`。
  - 使用 `@PortalLog(terminal = "buyer", title = "买家端账号列表", ...)` 写入买家端操作日志。
  - 从 `PortalSessionContext.requireSession("buyer")` 读取当前端会话，再使用 `session.subjectId` 查询当前买家主体下账号。
  - 不接收前端传入 `buyerId`。
  - 返回 `PortalAccountProfile` 列表，不返回 `BuyerAccount` 原始对象。
- `react-ui/src/services/portal/session.ts` 新增 `getPortalAccounts`，并挂到 `sellerPortalSessionService.getAccounts` / `buyerPortalSessionService.getAccounts`。
- `react-ui/src/types/seller-buyer/party.d.ts` 新增 `PortalAccountListResult`。
- `docs/architecture/reuse-ledger.md` 已补充端内账号只读列表的复用规则。

验证结果：

- `mvn -DskipTests compile`：通过。
- `npm run tsc`：通过。
- 本轮没有启动后端。
- 本轮没有执行 SQL、DDL 或远程 DML。

当前判断：

- 端内账号列表已经形成第一条“非 profile 类”的端内只读业务接口样板。
- 端内账号列表优先保证权限边界：如果端内角色未配置 `seller:account:list` / `buyer:account:list`，运行时应返回无权限，而不是绕过端内权限放行。

## 2026-06-04 端内账号列表权限 seed 与真实烟测检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补齐 `/seller/accounts` 和 `/buyer/accounts` 所需端内权限数据，并完成真实接口烟测。

已完成：

- 新增 SQL：`RuoYi-Vue/sql/20260604_portal_account_list_permission_seed.sql`。
- 新增执行记录：`docs/plans/2026-06-04-portal-account-list-permission-sql-execution-record.md`。
- 脚本幂等补齐：
  - 现有正常卖家主体的默认端内 Owner 角色。
  - 现有正常买家主体的默认端内 Owner 角色。
  - 现有 `account_role = OWNER` 的端账号和默认 Owner 角色绑定。
  - `seller_menu.perms = seller:account:list`。
  - `buyer_menu.perms = buyer:account:list`。
  - 启用端内角色和账号列表权限菜单绑定。

远程库执行：

- 连接来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 第一次执行失败：脚本误用了主体表不存在的 `del_flag` 字段，未提交变更。
- 修正后第二次执行成功。
- 执行后计数：
  - `seller:account:list` 菜单：1。
  - `buyer:account:list` 菜单：1。
  - 启用 `seller_role`：3。
  - 启用 `buyer_role`：1。
  - OWNER 卖家账号绑定 owner 角色：3。
  - OWNER 买家账号绑定 owner 角色：1。
  - `seller:account:list` 角色菜单绑定：3。
  - `buyer:account:list` 角色菜单绑定：1。

验证结果：

- `mvn -DskipTests install`：通过。
- `.\start-backend-local.ps1 -Restart`：通过。
- 管理端登录：`code=200`。
- 卖家端：
  - `sellerId=9`，免密票据 `ticketId=34`。
  - `GET /seller/accounts` 返回 `code=200`。
  - 返回账号数量 1。
  - 返回对象不包含 `password` 和 `createBy`。
  - 卖家端 token 访问 `GET /buyer/accounts` 返回 `code=401`。
  - `seller_oper_log` 可查到 `operUrl=/seller/accounts`。
  - 烟测后已调用管理端账号级强制踢出接口清理会话。
- 买家端：
  - `buyerId=2`，免密票据 `ticketId=35`。
  - `GET /buyer/accounts` 返回 `code=200`。
  - 返回账号数量 1。
  - 返回对象不包含 `password` 和 `createBy`。
  - 买家端 token 访问 `GET /seller/accounts` 返回 `code=401`。
  - `buyer_oper_log` 可查到 `operUrl=/buyer/accounts`。
  - 烟测后已调用管理端账号级强制踢出接口清理会话。

当前判断：

- 端内账号只读列表已形成完整闭环：接口权限、端 token 主体范围、DTO 脱敏、端内权限 seed、真实 200、跨端 401、端内操作日志均已验证。
- 后续端内业务接口继续按这个模板推进：先从 token 推导主体范围，再用 `@PortalPreAuthorize` 校验端内权限，响应使用端内 DTO，不直接暴露后台对象。

## 2026-06-04 端内部门/角色只读列表接口与权限 seed 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按已确认的端内业务接口模板推进：接口从端 token 推导主体范围，使用端内权限校验，响应使用脱敏 DTO，不相信前端传入的 `sellerId` / `buyerId`。

已完成：

- 新增 `PortalDeptProfile`，承载端内部门只读列表响应。
- 新增 `PortalRoleProfile`，承载端内角色只读列表响应。
- 新增卖家端接口：
  - `GET /seller/depts`
  - `GET /seller/roles`
- 新增买家端接口：
  - `GET /buyer/depts`
  - `GET /buyer/roles`
- 四个接口均使用 `PortalSessionContext.requireSession(...)` 读取当前端会话。
- 四个接口均使用 `@PortalPreAuthorize` 校验端内权限：
  - `seller:dept:list`
  - `seller:role:list`
  - `buyer:dept:list`
  - `buyer:role:list`
- 四个接口均使用 `@PortalLog` 写入对应端内操作日志。
- `react-ui/src/services/portal/session.ts` 新增 `getPortalDepts` / `getPortalRoles`，并挂到 `sellerPortalSessionService` / `buyerPortalSessionService`。
- `react-ui/src/types/seller-buyer/party.d.ts` 新增 `PortalDeptProfile` / `PortalRoleProfile` 及列表结果类型。
- 新增 SQL：`RuoYi-Vue/sql/20260604_portal_dept_role_list_permission_seed.sql`。
- 新增执行记录：`docs/plans/2026-06-04-portal-dept-role-list-permission-sql-execution-record.md`。

远程库执行：

- 连接来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未在记录中输出凭证。
- 第一次 jshell 输入被 BOM 干扰，SQL 未执行且未提交。
- 第二次执行成功。
- 执行后计数：
  - `seller:dept:list` 菜单：1。
  - `seller:role:list` 菜单：1。
  - `buyer:dept:list` 菜单：1。
  - `buyer:role:list` 菜单：1。
  - 卖家启用角色绑定部门/角色只读权限：6。
  - 买家启用角色绑定部门/角色只读权限：2。

验证结果：

- `mvn -DskipTests compile`：通过。
- `npm run tsc`：通过。
- `mvn -DskipTests install`：Java 编译通过，首次 repackage 因运行中的 8080 后端锁定 `ruoyi-admin.jar` 失败。
- 停止 8080 后执行 `mvn -DskipTests install -rf :ruoyi-admin`：通过。
- `.\start-backend-local.ps1 -Restart`：通过，`http://127.0.0.1:8080` 返回 200。
- 管理端登录：`code=200`。
- 卖家端：
  - `sellerId=9`，免密票据 `ticketId=36`，端账号 `accountId=8`。
  - `GET /seller/depts` 返回 `code=200`，数量 0。
  - `GET /seller/roles` 返回 `code=200`，数量 1。
  - 返回字段未发现 `password`、`createBy`、`updateBy`、`delFlag`、`remark`。
  - 卖家端 token 访问 `GET /buyer/depts` 和 `GET /buyer/roles` 均返回 `code=401`。
  - `seller_oper_log` 可查到 `/seller/depts` 和 `/seller/roles`。
  - 烟测后已调用管理端账号级强制踢出接口清理会话，返回清理 1 条。
- 买家端：
  - `buyerId=2`，免密票据 `ticketId=37`，端账号 `accountId=2`。
  - `GET /buyer/depts` 返回 `code=200`，数量 0。
  - `GET /buyer/roles` 返回 `code=200`，数量 1。
  - 返回字段未发现 `password`、`createBy`、`updateBy`、`delFlag`、`remark`。
  - 买家端 token 访问 `GET /seller/depts` 和 `GET /seller/roles` 均返回 `code=401`。
  - `buyer_oper_log` 可查到 `/buyer/depts` 和 `/buyer/roles`。
  - 烟测后已调用管理端账号级强制踢出接口清理会话，返回清理 1 条。
- 最终验证：
  - `npm run tsc`：通过。
  - `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
  - `git diff --check -- <本轮触碰文件>`：通过，仅有 Git LF/CRLF 换行提示。

当前判断：

- 端内部门/角色只读列表已补齐权限、DTO、前端 service/type 契约和真实接口烟测。
- 当前远程库下卖家/买家主体暂无端内部门数据，因此部门列表数量为 0 是可接受状态；角色列表已返回当前端内启用角色。
- 后续端内业务接口继续套用同一模板：先 seller 做成标准样板，再按配置复制到 buyer，只替换 terminal、权限标识、service 和文案。

## 2026-06-04 端内主动退出登录接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补齐卖家端、买家端账号体系独立后的“当前端账号主动退出”能力。该能力和管理端强制踢出不同：管理端强制踢出可以按主体或账号范围清理会话；端内主动退出只能清理当前 token 对应的一条会话。

已完成：

- `PortalTokenSupport` 新增 `deleteLoginToken(PortalLoginSession session)`，只删除当前会话对应的 `portal_login_tokens:{terminal}:{tokenId}` Redis key。
- 卖家端新增 `POST /seller/logout`：
  - 使用 `@PortalPreAuthorize(terminal = "seller")`。
  - 使用 `@PortalLog(terminal = "seller", title = "卖家端退出登录", ...)` 写入 `seller_oper_log`。
  - 从 `PortalSessionContext.requireSession("seller")` 读取当前会话。
  - 更新 `seller_session` 当前 token 行为退出状态，并写入 `seller_login_log`。
- 买家端新增 `POST /buyer/logout`，按同一模板写入 `buyer_session`、`buyer_login_log` 和 `buyer_oper_log`。
- `react-ui/src/services/portal/session.ts` 新增 `portalLogout`，并挂到 `sellerPortalSessionService.logout` / `buyerPortalSessionService.logout`。
- `PortalPreAuthorizeAspect` 保持高优先级执行，让 `PortalLogAspect` 在退出时仍能从 `PortalSessionContext` 读取当前会话。
- 修复 `PortalPreAuthorizeAspect` 注解参数绑定不稳定问题：不再依赖 `argNames` 绑定 `PortalPreAuthorize` 参数，改为从 `MethodSignature` 读取方法注解。
- `docs/architecture/reuse-ledger.md` 已补充端内主动退出和前端 portal session 的复用规则。

修复过程记录：

- 第一次真实烟测发现退出登录日志使用 `Constants.LOGOUT` 写入端内登录日志时，远程 MySQL `status` 字段长度不匹配，导致 `Data too long for column 'status'`。已改为 `Constants.SUCCESS` 表示退出成功，退出语义写入 `msg`。
- 为保证退出时 `@PortalLog` 能拿到会话，曾尝试通过注解参数绑定调整切面顺序；最新修复已改为手动读取注解，避免 Spring AOP 参数绑定差异导致端内接口 500。

验证结果：

- 数据源确认：tracked YAML 当前通过 `RUOYI_*` 环境变量读取 MySQL/Redis 配置；本轮没有输出 `.env.local` 中的凭证、密码或 token secret。
- `mvn -DskipTests compile`：通过。
- 停止 8080 当前后端进程后执行 `mvn -DskipTests install`：通过，已重新打包 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1 -Restart`：通过，`http://127.0.0.1:8080` 探活返回 200。
- 完整真实接口烟测：
  - 管理端登录：`code=200`。
  - 卖家端免密登录：`sellerId=9`，`accountId=8`，登录成功。
  - 买家端免密登录：`buyerId=2`，`accountId=2`，登录成功。
  - 退出前 `/seller/getInfo` 和 `/buyer/getInfo` 均返回 `code=200`。
  - 卖家 token 调用 `/buyer/logout` 返回 `code=401`。
  - 买家 token 调用 `/seller/logout` 返回 `code=401`。
  - `POST /seller/logout` 返回 `code=200`，`data=1`。
  - `POST /buyer/logout` 返回 `code=200`，`data=1`。
  - 退出后旧卖家 token 调用 `/seller/getInfo` 返回 `code=401`。
  - 退出后旧买家 token 调用 `/buyer/getInfo` 返回 `code=401`。
  - `seller_login_log` 新增退出相关记录，烟测读取到 `infoId=39`。
  - `buyer_login_log` 新增退出相关记录，烟测读取到 `infoId=27`。
  - `seller_oper_log` 新增 `operUrl=/seller/logout`，烟测读取到 `operId=18`。
  - `buyer_oper_log` 新增 `operUrl=/buyer/logout`，烟测读取到 `operId=18`。

当前判断：

- 端内主动退出已形成闭环：端 token 校验、跨端拒绝、当前会话退出、Redis token 删除、旧 token 失效、登录日志和操作日志均已验证。
- 后续卖家端/买家端真实前端退出按钮直接调用 `sellerPortalSessionService.logout` / `buyerPortalSessionService.logout`，成功后清理对应端本地 token；不要在页面内重新拼路径或自行传会话 ID。
- 已确认的 seller/buyer 同构部分继续模板化推进：卖家侧先形成标准样板并完成验证，买家侧只替换端类型、文案、路由、权限标识、字段配置和 service，不再逐页重新设计。

## 2026-06-04 端内当前账号修改密码接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补齐卖家端、买家端账号体系独立后的“当前端账号自行修改密码”能力。该能力必须只修改当前端账号自己的密码：卖家端写 `seller_account.password`，买家端写 `buyer_account.password`，不复用也不改写管理端 `sys_user`。

已完成：

- 新增 `PortalPasswordChangeRequest`，作为卖家端/买家端统一的修改密码请求对象。
- `PartnerSupport` 新增 `normalizePasswordChange(...)`：
  - 统一校验旧密码、新密码、确认密码。
  - 新密码长度沿用若依 `UserConstants.PASSWORD_MIN_LENGTH` / `PASSWORD_MAX_LENGTH`。
  - 校验新密码和确认密码一致。
- 卖家端新增 `PUT /seller/account/password`：
  - 使用 `@PortalPreAuthorize(terminal = "seller")` 校验当前端 token。
  - 使用 `PortalSessionContext.requireSession("seller")` 推导当前 `sellerId` 和 `sellerAccountId`。
  - 只允许修改当前 token 对应的 `seller_account`。
  - 使用 `@PortalLog(..., isSaveResponseData = false)` 写入 `seller_oper_log`。
- 买家端新增 `PUT /buyer/account/password`，按同一模板只允许修改当前 token 对应的 `buyer_account`，并写入 `buyer_oper_log`。
- `SellerMapper.xml` / `BuyerMapper.xml` 的账号按 ID 查询补充读取 `a.password`，供内部旧密码校验使用。
- `react-ui/src/services/portal/session.ts` 新增 `updatePortalPassword(...)`，并挂到 `sellerPortalSessionService.updatePassword` / `buyerPortalSessionService.updatePassword`。
- `react-ui/src/types/seller-buyer/party.d.ts` 新增 `PortalPasswordChangeParams`。
- `docs/architecture/reuse-ledger.md` 已补充端内当前账号修改密码的复用规则。

修复过程记录：

- 第一次真实烟测发现旧密码校验一直失败，根因是 `selectSellerAccountById` / `selectBuyerAccountById` 原查询没有读取 `password` 字段。
- 已修复为按 ID 查询时读取密码密文，仅供 Service 内部校验；管理端列表、端内资料接口和端内账号列表仍通过 DTO 输出，不暴露 `password` 字段。

验证结果：

- 数据源确认：tracked YAML 当前通过 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 和 `RUOYI_REDIS_*` 环境变量读取 MySQL/Redis 配置；本轮没有输出 `.env.local` 中的凭证、密码或 token secret。
- `mvn -DskipTests compile`：通过。
- `npm run tsc`：通过。
- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过，新增 `PartnerSupportTest` 覆盖密码修改字段校验。
- 停止并重启后端：`.\start-backend-local.ps1 -Restart` 通过，`http://127.0.0.1:8080` 探活返回 200。
- 完整真实接口烟测：
  - 管理端登录：`code=200`。
  - 卖家端：`sellerId=9`，`accountId=8`。
  - 买家端：`buyerId=2`，`accountId=2`。
  - 烟测前管理端重置卖家/买家端账号默认密码为 `U12346`，并强制清理旧会话。
  - 卖家端、买家端使用默认密码登录成功。
  - 旧密码错误时，卖家端和买家端修改密码均被拒绝。
  - 卖家 token 调用 `/buyer/account/password` 返回 401，买家 token 调用 `/seller/account/password` 返回 401。
  - 卖家端、买家端使用正确旧密码修改为临时密码均返回 `code=200`。
  - 修改后默认密码登录失败，临时密码登录成功。
  - 使用临时密码登录后，已把卖家端和买家端账号密码恢复为默认密码 `U12346`。
  - 恢复后默认密码登录成功。
  - `seller_oper_log` 可查到 `/seller/account/password`，烟测读取到 `operId=24`。
  - `buyer_oper_log` 可查到 `/buyer/account/password`，烟测读取到 `operId=23`。
  - 烟测后已调用管理端账号级强制踢出接口清理本次会话。

当前判断：

- 端内当前账号修改密码已形成闭环：端 token 校验、当前账号范围、旧密码校验、新密码写入、跨端拒绝、旧密码失效、新密码生效、恢复默认密码和端内操作日志均已验证。
- 该能力不需要 DDL；本轮真实 DML 只发生在被选中卖家/买家端账号的密码重置、修改和恢复上，最终已恢复默认密码。
- 后续卖家端/买家端个人中心或安全设置页面直接调用 `sellerPortalSessionService.updatePassword` / `buyerPortalSessionService.updatePassword`，页面不要传 `sellerId`、`buyerId`、`accountId`，也不要记录旧密码、新密码或确认密码。

## 2026-06-04 三端前端直登入口与端内工作台检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“已确认模式模板化复制”的方式，先在当前 `react-ui/` 管理端验证入口中补齐卖家端、买家端直登录地页和端内工作台页。当前阶段仍不复制 `seller-ui` / `buyer-ui`，只准备后续物理拆分可复用的入口代码。

已完成：

- 新增 `react-ui/src/pages/Portal/terminal.ts`：
  - 集中维护 `seller` / `buyer` 的端名称、首页路径和 service 映射。
  - 统一封装免密登录成功后的端 token 持久化和清理。
- 新增 `react-ui/src/pages/Portal/DirectLogin/index.tsx`：
  - 同一个页面同时承载 `/seller/direct-login` 和 `/buyer/direct-login`。
  - 从 URL 读取 `directLoginToken` 后调用对应端的 `directLogin`。
  - 成功后只写入对应端本地 token key，并跳转到对应端工作台。
  - 失败时只清理对应端 token，不影响管理端 `access_token`。
- 新增 `react-ui/src/pages/Portal/Home/index.tsx`：
  - 同一个页面同时承载 `/seller/portal` 和 `/buyer/portal`。
  - 从对应端 token 调用 `getInfo`、主体资料、当前账号资料、端内账号、端内部门和端内角色接口。
  - 提供端内主动退出和修改当前账号密码入口。
  - 页面不传 `sellerId`、`buyerId`、`accountId` 作为权限边界。
- `react-ui/config/routes.ts` 新增静态路由：
  - `/seller/direct-login`
  - `/buyer/direct-login`
  - `/seller/portal`
  - `/buyer/portal`
- `react-ui/src/app.tsx` 新增 portal 路由白名单：
  - `/seller/direct-login`、`/buyer/direct-login`、`/seller/portal`、`/buyer/portal` 不触发管理端 `getUserInfo()`、动态菜单加载或管理端登录态重定向。
  - 请求拦截器会删除内部 `isToken` 标记，避免该标记被发送到后端。
- `react-ui/src/services/portal/session.ts` 的 portal 请求均显式设置 `isToken: false`：
  - 登录和免密登录不自动注入管理端 token。
  - 已登录后的端内请求只使用对应端 `seller_*` / `buyer_*` token，不回退使用管理端 `access_token`。
- 直登页校验后端返回的 `terminal` 必须与 URL 端类型一致，不一致时清理相关端 token 并失败。
- 端内工作台基础布局改为响应式：大屏 3 列、中屏 2 列、小屏 1 列，头部操作按钮允许换行。
- 卖家端与买家端没有分别重写页面；只通过 `terminal` 配置、文案、路径和 service 映射区分。

验证结果：

- `npm run tsc`：通过。
- `mvn -DskipTests compile`：通过。
- 停止 8080 当前后端进程后执行 `mvn -DskipTests package`：通过，已重新打包 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1`：通过，8080 正常监听，`http://127.0.0.1:8080/` 探活返回 200。
- Playwright 真实页面验证：
  - 管理端生成卖家端免密票据后，打开 `/seller/direct-login?directLoginToken=...`，最终进入 `/seller/portal`。
  - 管理端生成买家端免密票据后，打开 `/buyer/direct-login?directLoginToken=...`，最终进入 `/buyer/portal`。
  - 浏览器 localStorage 中卖家端、买家端 token 分别写入对应 key。
  - 页面能展示当前端主体、当前账号、端内角色、端内部门、端内账号和权限标识。
  - 页面端内退出后调用 `/seller/logout` / `/buyer/logout` 返回 `code=200`。
- 无管理端登录态直登验证：
  - 使用全新浏览器上下文直接打开卖家端直登 URL，最终进入 `/seller/portal`，未被重定向到 `/user/login`。
  - 使用全新浏览器上下文直接打开买家端直登 URL，最终进入 `/buyer/portal`，未被重定向到 `/user/login`。
  - 验证后已通过管理端强制清理测试主体本轮端会话。
- 截图留存：
  - `output/playwright/seller-portal-loaded.png`
  - `output/playwright/buyer-portal-loaded.png`
  - `output/playwright/seller-portal-no-admin.png`
  - `output/playwright/buyer-portal-no-admin.png`
- 本轮未执行 DDL。
- 本轮真实 DML 仅包含：生成并消费卖家端、买家端免密票据，写入对应端登录日志、操作日志和会话记录；验证结束后已调用端内退出清理本轮端 token。

当前判断：

- 端内直登入口和工作台已经形成第一版前端模板：卖家端先成样板，买家端只替换 terminal 配置、路径、文案和 service。
- portal 路由和 portal 请求已经与管理端初始化、管理端 token 注入隔离；后续不要在 portal service 中移除 `isToken: false`。
- 后续三端物理拆分时，可直接迁移 `Portal/terminal.ts`、`Portal/DirectLogin`、`Portal/Home` 的模式到卖家端、买家端独立前端；管理端仍保留若依 `sys_*` 登录和菜单体系。
- 该工作台目前是验证型入口，不是最终业务门户；后续正式卖家端/买家端页面应继续复用端 token、端内 service 和 `PortalSessionContext` 后端边界。

## 2026-06-04 端内当前账号日志只读接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补齐卖家端、买家端当前账号查看自己登录日志和操作日志的只读能力。该能力只服务端内当前账号，不提供管理端筛全量日志的能力。

已完成：

- 卖家端新增：
  - `GET /seller/account/login-logs`
  - `GET /seller/account/oper-logs`
- 买家端新增：
  - `GET /buyer/account/login-logs`
  - `GET /buyer/account/oper-logs`
- 四个接口均使用 `@PortalPreAuthorize` 校验端 token。
- 四个接口均从 `PortalSessionContext.requireSession(...)` 推导当前 `subjectId` 和 `accountId`。
- 即使前端传入 `subjectId` 或 `accountId` 查询参数，后端也会覆盖为当前 token 的主体和账号范围。
- 四个接口复用既有 seller/buyer 日志 mapper 和 service 列表方法，不新增日志表、不改 SQL。
- 四个接口的分页最大 `pageSize` 限制为 100，避免端内当前账号日志页请求超大分页。
- `react-ui/src/services/portal/session.ts` 新增：
  - `getPortalLoginLogs`
  - `getPortalOperLogs`
  - `sellerPortalSessionService.getLoginLogs`
  - `sellerPortalSessionService.getOperLogs`
  - `buyerPortalSessionService.getLoginLogs`
  - `buyerPortalSessionService.getOperLogs`

验证结果：

- `npm run tsc`：通过。
- `mvn -DskipTests compile`：通过。
- 停止 8080 当前后端进程后执行 `mvn -DskipTests package`：通过，已重新打包 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1`：通过，8080 正常监听。
- 真实接口烟测：
  - 管理端登录：`code=200`。
  - 卖家端免密登录：返回 `terminal=seller`。
  - 买家端免密登录：返回 `terminal=buyer`。
  - `GET /seller/account/login-logs` 返回 `code=200`，当前数据返回 61 行。
  - `GET /seller/account/oper-logs` 返回 `code=200`，当前数据返回 46 行。
  - `GET /buyer/account/login-logs` 返回 `code=200`，当前数据返回 44 行。
  - `GET /buyer/account/oper-logs` 返回 `code=200`，当前数据返回 45 行。
  - 四个日志接口使用 `pageSize=999` 请求时，代码上限为 100；当前远程库对应账号日志量低于上限，因此返回量未超过 100。
  - 卖家 token 访问 `/buyer/account/login-logs` 返回 body `code=401`。
  - 买家 token 访问 `/seller/account/oper-logs` 返回 body `code=401`。
  - 卖家登录日志返回行，`subjectId/accountId` 越界数量 0。
  - 卖家操作日志返回行，`subjectId/accountId` 越界数量 0。
  - 买家登录日志返回行，`subjectId/accountId` 越界数量 0。
  - 买家操作日志返回行，`subjectId/accountId` 越界数量 0。
- 本轮未执行 DDL。
- 本轮真实 DML 仅包含：为烟测生成并消费卖家端、买家端免密票据，写入对应端登录日志、操作日志和会话记录；烟测结束后已调用端内退出清理本轮端 token。

当前判断：

- 当前账号日志只读接口已形成模板：接口从端 token 推导主体和账号，查询参数不能扩大范围，跨端 token 在业务 body 中返回 `code=401`。
- 查看操作日志本身会通过 `@PortalLog` 追加一条端内操作日志，这是保留审计行为；前端日志页后续不要做高频自动轮询。
- 后续卖家端/买家端个人中心或安全中心日志页直接调用 `sellerPortalSessionService.getLoginLogs` / `getOperLogs` 和 `buyerPortalSessionService.getLoginLogs` / `getOperLogs`。
- 管理端日志审计仍继续使用 `/seller/admin/sellers/loginLogs/list`、`/seller/admin/sellers/operLogs/list`、`/buyer/admin/buyers/loginLogs/list`、`/buyer/admin/buyers/operLogs/list`，不要和端内当前账号日志接口混用。

## 2026-06-04 端内当前账号会话只读接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补齐卖家端、买家端当前账号查看自己登录会话的只读能力。该能力只服务当前端账号，不提供管理端筛全量会话，也不提供端内踢出其他会话能力。

已完成：

- 新增 `PortalSessionProfile`，统一承载端内当前账号会话响应；`tokenId` 仅供后端内部判断 `current`，通过 `@JsonIgnore` 不输出给前端。
- 卖家端新增 `GET /seller/account/sessions`。
- 买家端新增 `GET /buyer/account/sessions`。
- 两个接口均使用 `@PortalPreAuthorize` 校验端 token。
- 两个接口均从 `PortalSessionContext.requireSession(...)` 推导当前 `subjectId` 和 `accountId`，不接收前端传入的主体 ID 或账号 ID 扩大查询范围。
- 两个接口复用 `seller_session` / `buyer_session`，不新增会话表、不执行 DDL。
- 响应只返回 `terminal`、`subjectId`、`accountId`、`userName`、`loginIp`、`loginTime`、`expireTime`、`logoutTime`、`status`、`current`，不返回 `tokenId`、JWT、Redis key 或密码字段。
- 分页最大 `pageSize` 限制为 100。
- `react-ui/src/services/portal/session.ts` 新增：
  - `getPortalSessions`
  - `sellerPortalSessionService.getSessions`
  - `buyerPortalSessionService.getSessions`
- `react-ui/src/pages/Portal/Home/index.tsx` 的验证型工作台新增“当前账号会话”只读卡片，独立加载最近 5 条会话；会话接口失败不会清理端 token 或跳回管理端登录页。

验证结果：

- `mvn -DskipTests compile`：通过。
- 停止 8080 旧后端进程后执行 `mvn -DskipTests package`：通过，已重新打包 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1`：通过，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`，验证码开关保持关闭。
- 真实接口烟测：
  - 管理端登录：`code=200`。
  - 卖家端免密登录：返回 `terminal=seller`。
  - 买家端免密登录：返回 `terminal=buyer`。
  - `GET /seller/account/sessions?pageNum=1&pageSize=999` 返回 `code=200`，当前数据返回 53 行。
  - `GET /buyer/account/sessions?pageNum=1&pageSize=999` 返回 `code=200`，当前数据返回 38 行。
  - 卖家、买家会话接口返回行数均未超过 100。
  - 卖家、买家会话接口均有且只有 1 条 `current=true`。
  - 卖家、买家会话接口响应均未输出 `tokenId`。
  - 卖家 token 访问 `/buyer/account/sessions` 返回 body `code=401`。
  - 买家 token 访问 `/seller/account/sessions` 返回 body `code=401`。
  - 伪造 `sellerId` / `buyerId` / `sellerAccountId` / `buyerAccountId` / `accountId` / `subjectId` 参数访问会话接口时仍返回 `code=200`，但返回行越界数量为 0。
  - 验证后已调用端内退出清理本轮端 token。
- 本轮未执行 DDL。
- 本轮真实 DML 仅包含：为烟测生成并消费卖家端、买家端免密票据，写入对应端登录日志、操作日志和会话记录；烟测结束后已调用端内退出清理本轮端 token。

当前判断：

- 当前账号会话只读接口继续沿用端内接口模板：接口从端 token 推导主体和账号，查询参数不能扩大范围，响应 DTO 必须脱敏。
- 查看会话本身会通过 `@PortalLog` 追加一条端内操作日志，这是保留审计行为；接口设置 `isSaveResponseData=false`，不把会话列表写入操作日志响应体。
- 管理端强制踢出仍继续使用 `/seller/admin/sellers/*/sessions` 和 `/buyer/admin/buyers/*/sessions`，不要和端内当前账号会话接口混用。
- 后续端内安全中心或个人中心会话页直接调用 `sellerPortalSessionService.getSessions` / `buyerPortalSessionService.getSessions`，页面不要传主体 ID、账号 ID、`tokenId` 或 Redis key。

## 2026-06-04 卖家端商品 Schema 只读标准模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按最新节奏收敛：先做一套标准卖家模板，验收通过后再复制买家；每个切片只改一类东西，减少返工。

已完成：

- 在 `product` 模块新增卖家端只读入口 `GET /seller/product/categories/{categoryId}/schema`。
- 接口使用 `@PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:schema:query")`。
- 接口使用 `@PortalLog(terminal = "seller", title = "Seller product schema", isSaveResponseData = false)` 写入卖家端操作日志，但不记录响应体。
- 接口从 `PortalSessionContext.requireSession("seller")` 确认当前请求是卖家端会话。
- 接口复用 `IProductConfigService.previewCategorySchema(categoryId)`，不在 seller/buyer 模块或前端重复实现类目继承合并逻辑。
- 新增端内 DTO：`PortalProductCategorySchemaItem`、`PortalProductAttributeOption`，不直接暴露管理端 domain 的 `createBy`、`updateBy`、`remark` 等审计字段。
- 端内接口只允许启用且可发布的类目，并只返回启用的属性规则和启用的属性选项。
- 新增卖家端权限 seed：`RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql`。
- 新增 SQL 执行记录：`docs/plans/2026-06-04-seller-product-schema-permission-sql-execution-record.md`。
- 本轮已按最新要求删除刚开始误加的 buyer endpoint 和前端 portal product service，当前切片只保留卖家端后端模板。

验证结果：

- 数据源确认：tracked YAML 通过 `RUOYI_*` 环境变量读取远程 MySQL/Redis 配置；本轮不输出 `.env.local` 凭证。
- 卖家权限 DML：执行成功。
- `seller_menu` 中 `seller:product:schema:query` 数量：`1`。
- `seller_role_menu` 中该权限绑定数量：`3`。
- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1`：通过，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端生成卖家端免密票据：`code=200`。
- 卖家端消费免密票据：`code=200`，`terminal=seller`。
- seller token 调 `GET /seller/product/categories/446/schema`：HTTP 200，业务 `code=200`。
- 当前远程库选中可发布类目的 schema 字段数为 `0`，因此本轮验证的是接口模板、权限和脱敏边界，不代表商品类目属性配置已有内容。
- 响应敏感 key 检查：未发现 `password`、`token`、`tokenId`、`createBy`、`updateBy`、`remark`、Redis key。
- admin token 调卖家端 schema：业务 `code=401`。
- 无 token 调卖家端 schema：业务 `code=401`。
- 伪造 `sellerId`、`buyerId`、`accountId`、`terminal` 查询参数不能扩大范围。
- 验证后已调用 `POST /seller/logout` 清理本轮 seller portal token。
- 本轮未新增 buyer 端接口、buyer 权限、buyer 前端 service 或买家端验证。

当前判断：

- 卖家端商品 Schema 只读接口已经形成可复制的标准模板。
- buyer 端后续复制时，不重新设计；只替换 terminal、路径、权限点、日志 title、seed 表名和验证主体。
- 商品 Schema 内容为空是当前远程库业务配置状态问题，后续需要通过商品类目属性绑定数据补齐，不应在本切片里临时插入业务配置数据。

## 2026-06-04 买家端商品 Schema 只读复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按最新节奏执行：卖家端模板验收后复制买家端；本切片只改端内商品 Schema 只读入口、权限 seed 和执行记录，不扩展前端、不新增业务配置数据。

已完成：

- 在 `product` 模块新增买家端只读入口 `GET /buyer/product/categories/{categoryId}/schema`。
- 接口使用 `@PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:schema:query")`。
- 接口使用 `@PortalLog(terminal = "buyer", title = "Buyer product schema", isSaveResponseData = false)` 写入买家端操作日志，但不记录响应体。
- 接口从 `PortalSessionContext.requireSession("buyer")` 确认当前请求是买家端会话。
- 接口继续复用 `IProductConfigService.previewCategorySchema(categoryId)`，不在 buyer 模块或前端重复实现类目继承合并逻辑。
- seller/buyer 两个端内 schema 方法均采用方法级 `@Anonymous`，不使用类级匿名。
- 端内 DTO 继续复用 `PortalProductCategorySchemaItem`、`PortalProductAttributeOption`，不直接暴露管理端 domain 的审计字段。
- 端内接口只允许启用且可发布的类目，并只返回启用且可见的属性规则和启用的属性选项。
- 新增买家端权限 seed：`RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql`。
- 新增 SQL 执行记录：`docs/plans/2026-06-04-buyer-product-schema-permission-sql-execution-record.md`。

验证结果：

- 数据源确认：tracked YAML 通过 `RUOYI_*` 环境变量读取远程 MySQL/Redis 配置；本轮不输出 `.env.local` 凭证。
- 买家权限 DML：执行成功。
- `buyer_menu` 中 `buyer:product:schema:query` 数量：`1`。
- `buyer_role_menu` 中该权限绑定数量：`1`。
- `buyer_role` 当前 active 角色数量：`1`。
- `buyer_account_role` 中 owner 账号绑定数量：`1`。
- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1`：通过，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 当前远程库选中启用且可发布类目，管理端 schema 字段数为 `8`。
- 管理端生成买家端免密票据：`code=200`。
- 买家端消费免密票据：`code=200`，`terminal=buyer`。
- buyer token 调买家端 schema：HTTP 200，业务 `code=200`。
- 买家端 schema 响应字段数为 `8`，必填字段数为 `5`，选项数量为 `40`。
- 响应敏感 key 检查：未发现 `password`、`token`、`tokenId`、`createBy`、`updateBy`、`remark`、Redis key。
- admin token 调买家端 schema：业务 `code=401`。
- 无 token 调买家端 schema：业务 `code=401`。
- seller token 调买家端 schema：业务 `code=401`。
- 伪造 `sellerId`、`buyerId`、`accountId`、`subjectId`、`terminal` 查询参数不能扩大范围，响应字段数变化为 `0`。
- 卖家端回归验证：seller token 调卖家端 schema 返回业务 `code=200`，schema 字段数为 `8`，敏感 key 命中数量为 `0`。
- 验证后已调用 `POST /buyer/logout` 和 `POST /seller/logout` 清理本轮 portal token。

当前判断：

- 买家端商品 Schema 只读入口已按卖家模板复制完成，只替换 terminal、路径、权限点、日志 title、seed 表名和验证主体。
- seller/buyer 商品 Schema 只读接口已形成同构模板；后续端内商品相关只读入口继续优先按这一套模板推进。
- 权限 seed 当前会把该只读权限授予 active 端内角色；后续如果引入更细的角色授权策略，应把 seed 从“所有 active 角色”调整为明确角色清单。
- 远程库存在已软删除的 owner 角色历史数据；本轮 DML 已执行成功，但后续 seed 继续写 owner 角色时应先检查唯一约束和软删除数据，避免历史脏数据造成冲突。

## 2026-06-04 端内商品分类只读接口与免密日志脱敏检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板先做、验收后复制买家”的方式补齐商品 Schema 的前置分类列表入口，并修复管理端免密登录响应日志可能记录明文 token 的风险。

已完成：

- 在 `product` 模块新增端内商品分类 DTO：`PortalProductCategory`。
- 卖家端新增 `GET /seller/product/categories`。
- 买家端新增 `GET /buyer/product/categories`。
- 两个分类接口均使用方法级 `@Anonymous`。
- 卖家端分类接口使用 `@PortalPreAuthorize(terminal = "seller", hasPermi = "seller:product:category:list")`。
- 买家端分类接口使用 `@PortalPreAuthorize(terminal = "buyer", hasPermi = "buyer:product:category:list")`。
- 两个分类接口均使用 `@PortalLog(..., isSaveResponseData = false)` 写入端内操作日志，但不记录响应体。
- 两个分类接口均从 `PortalSessionContext.requireSession(...)` 确认当前端会话。
- 商品分类查询复用 `IProductConfigService.selectCategoryList(...)`，后端强制 `status=0` 和 `publishEnabled=Y`。
- 新增权限 seed：`RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`。
- 新增 SQL 执行记录：`docs/plans/2026-06-04-portal-product-category-permission-sql-execution-record.md`。
- 管理端卖家/买家 directLogin 的 `@Log` 均增加 `isSaveResponseData=false`，避免把一次性明文 token 和 loginUrl 写入 `sys_oper_log.json_result`。

远程库执行结果：

- 第一次临时 Java SQL 执行器因 BOM 编码导致 `javac` 失败，未连接数据库、未执行 SQL。
- 使用无 BOM 临时执行器重新执行 `20260604_portal_product_category_permission_seed.sql` 成功。
- 执行语句数：`4`。
- `seller_menu` 中 `seller:product:category:list` 数量：`1`。
- `seller_role_menu` 中该权限绑定数量：`3`。
- `buyer_menu` 中 `buyer:product:category:list` 数量：`1`。
- `buyer_role_menu` 中该权限绑定数量：`1`。
- 当前启用且可发布商品分类数量：`160`。

验证结果：

- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过。
- 中途曾因一个新 Java 进程占用 `ruoyi-admin.jar` 导致一次 package repackage 重命名失败；停止占用进程后重包通过。
- `.\start-backend-local.ps1`：返回成功，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端生成卖家端免密票据：`code=200`。
- 管理端生成买家端免密票据：`code=200`。
- 卖家端消费免密票据：`code=200`，`terminal=seller`。
- 买家端消费免密票据：`code=200`，`terminal=buyer`。
- seller token 调 `GET /seller/product/categories`：业务 `code=200`，返回 `160` 个分类。
- buyer token 调 `GET /buyer/product/categories`：业务 `code=200`，返回 `160` 个分类。
- seller/buyer 分类响应中 `publishEnabled != Y` 的数量均为 `0`。
- seller/buyer 分类响应敏感 key 命中数均为 `0`，未发现 `password`、`token`、`tokenId`、`createBy`、`updateBy`、`remark`、`delFlag`。
- admin token 调 seller/buyer 分类接口：业务 `code=401`。
- 无 token 调 seller/buyer 分类接口：业务 `code=401`。
- seller token 调 buyer 分类接口：业务 `code=401`。
- buyer token 调 seller 分类接口：业务 `code=401`。
- 伪造 `sellerId`、`buyerId`、`accountId`、`subjectId`、`terminal` 查询参数不能改变返回范围，返回数量差异为 `0`。
- 本轮 directLogin 后，`sys_oper_log` 中卖家免密登录日志行数为 `1`，买家免密登录日志行数为 `1`。
- 本轮 directLogin 操作日志中命中 `token`、`loginUrl`、`directLoginToken` 的行数为 `0`。
- 验证后已调用 `POST /seller/logout` 和 `POST /buyer/logout` 清理本轮 portal token。

当前判断：

- 端内商品分类只读接口补齐了商品 Schema 读取前的合法 `categoryId` 来源。
- seller/buyer 端商品只读接口继续保持同构模板：端 token 鉴权、端内权限点、端内 DTO、后端强制过滤、跨端拒绝和脱敏响应。
- directLogin 响应日志脱敏已修复本轮发现的高风险点；历史 `sys_oper_log` 是否曾记录过旧 token，需要单独安全审计，不在本切片里做数据清理。
- `SellerPortalController` / `BuyerPortalController` 类级 `@Anonymous` 硬化问题已在后续检查点处理，当前状态以最新检查点为准。

## 2026-06-04 Portal Controller 匿名放行硬化检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，修复 seller/buyer 主 portal controller 类级 `@Anonymous` 带来的后续扩展风险：如果新增端内方法漏挂 `@PortalPreAuthorize`，类级匿名会先让请求通过若依外层登录过滤。

已完成：

- `SellerPortalController` 移除类级 `@Anonymous`。
- `BuyerPortalController` 移除类级 `@Anonymous`。
- `SellerPortalController` 的 12 个映射方法均补充方法级 `@Anonymous`，并保留原 `@PortalPreAuthorize`。
- `BuyerPortalController` 的 12 个映射方法均补充方法级 `@Anonymous`，并保留原 `@PortalPreAuthorize`。
- 本轮不改业务逻辑、不改路径、不改权限点、不执行 DDL/DML。

静态检查：

- `SellerPortalController`：`classAnonymous=false`，`mappings=12`，`missingAnonymous=0`，`missingPortalPreAuthorize=0`。
- `BuyerPortalController`：`classAnonymous=false`，`mappings=12`，`missingAnonymous=0`，`missingPortalPreAuthorize=0`。

验证结果：

- 数据源确认：后端激活 `druid`，MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮不输出 `.env.local` 凭证。
- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1`：返回成功，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端生成卖家端免密票据：`code=200`。
- 管理端生成买家端免密票据：`code=200`。
- 卖家端消费免密票据：`code=200`，`terminal=seller`。
- 买家端消费免密票据：`code=200`，`terminal=buyer`。
- seller token 调 `/seller/getInfo`、`/seller/getRouters`、`/seller/profile`、`/seller/accounts`、`/seller/account/sessions`：均可访问，其中主体资料返回 `terminal=seller`，账号列表返回 `1` 条，会话列表返回 `59` 条。
- buyer token 调 `/buyer/getInfo`、`/buyer/getRouters`、`/buyer/profile`、`/buyer/accounts`、`/buyer/account/sessions`：均可访问，其中主体资料返回 `terminal=buyer`，账号列表返回 `1` 条，会话列表返回 `42` 条。
- 无 token 调 `/seller/getInfo`：业务 `code=401`。
- 无 token 调 `/buyer/getInfo`：业务 `code=401`。
- buyer token 调 `/seller/getInfo`：业务 `code=401`。
- seller token 调 `/buyer/getInfo`：业务 `code=401`。
- 验证后已调用 `POST /seller/logout` 和 `POST /buyer/logout` 清理本轮 portal token。

当前判断：

- seller/buyer 主 portal controller 已从“类级匿名 + 方法级权限”收敛为“方法级匿名 + 方法级权限”。
- 该模式保留 portal token 可进入若依外层过滤的必要放行，同时避免新增方法因类级匿名被无意公开。
- 后续新增 seller/buyer portal 端受保护接口时，必须方法级同时声明 `@Anonymous` 和 `@PortalPreAuthorize`；只声明其中一个都不算符合模板。

## 2026-06-04 卖家端 DB 会话权威鉴权模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板先做，验收通过后复制买家”的节奏，只处理一类问题：seller 端 portal 鉴权不能只依赖 Redis session，还要以 `seller_session` 的在线状态作为兜底权威。

已完成：

- `SellerMapper` 新增 `countOnlineSellerSession(...)`。
- `SellerMapper.xml` 新增 `countOnlineSellerSession` 查询，按 `seller_id`、`seller_account_id`、`token_id`、`status='0'`、`logout_time is null`、`expire_time >= sysdate()` 判断当前 session 是否仍在线。
- `SellerPortalPermissionServiceImpl.assertActiveSellerSession(...)` 在校验卖家主体状态、卖家账号状态后，增加 `seller_session` 在线状态校验。
- 本轮不改 buyer 代码，不新增 DDL，不新增权限点，不改前端。
- 新增执行记录：`docs/plans/2026-06-04-seller-db-session-authority-execution-record.md`。

验证结果：

- 数据源确认：后端激活 `druid`，MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮不输出 `.env.local` 凭证。
- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1 -Restart`：后端启动成功，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端卖家列表：`code=200`。
- 管理端生成卖家端免密票据：`code=200`。
- 卖家端消费免密票据：`code=200`，`terminal=seller`。
- seller token 初次调用 `/seller/getInfo`：`code=200`。
- 受控远程 DML：仅将本轮新建测试 session 对应 `seller_session` 行更新为 `status='1'`、`logout_time=sysdate()`，影响行数 `1`；Redis token 保持不变。
- DB session 失效后，旧 seller token 再调 `/seller/getInfo`：业务 `code=401`，消息为“登录状态已失效”。
- 受控远程 DML：将同一测试 session 临时恢复为 `status='0'`、`logout_time=null`，影响行数 `1`，随后调用 `/seller/logout` 正常清理 Redis 和 DB session。
- `/seller/logout`：`code=200`。
- logout 后旧 seller token 再调 `/seller/getInfo`：业务 `code=401`。
- 最后通过管理端 `DELETE /seller/admin/sellers/{sellerId}/sessions` 清理本轮早期脚本失败遗留的同卖家测试 session，返回 `code=200`，影响行数 `1`。

当前判断：

- 卖家端已经形成 DB 会话权威鉴权模板：Redis token 仍是入口缓存，但只要 `seller_session` 已失效，受 `@PortalPreAuthorize(terminal = "seller", ...)` 保护的 seller 接口会拒绝旧 token。
- 当前实现没有新增表字段，`seller_session` 现有 `status/logout_time/expire_time` 字段足够承载该规则。
- buyer 端已在后续检查点按同一模板复制，且只替换 mapper、表名、字段名和 terminal。
- 管理端强制踢出执行链已能让 token 失效，但管理端 session 列表查询仍不完整，需后续补齐。

## 2026-06-04 买家端 DB 会话权威鉴权复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板验收通过后复制买家”的节奏，只处理一类问题：buyer 端 portal 鉴权不能只依赖 Redis session，还要以 `buyer_session` 的在线状态作为兜底权威。

已完成：

- `BuyerMapper` 新增 `countOnlineBuyerSession(...)`。
- `BuyerMapper.xml` 新增 `countOnlineBuyerSession` 查询，按 `buyer_id`、`buyer_account_id`、`token_id`、`status='0'`、`logout_time is null`、`expire_time >= sysdate()` 判断当前 session 是否仍在线。
- `BuyerPortalPermissionServiceImpl.assertActiveBuyerSession(...)` 在校验买家主体状态、买家账号状态后，增加 `buyer_session` 在线状态校验。
- 本轮不改 seller 代码，不新增 DDL，不新增权限点，不改前端。
- 新增执行记录：`docs/plans/2026-06-04-buyer-db-session-authority-execution-record.md`。

验证结果：

- 数据源确认：后端激活 `druid`，MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮不输出 `.env.local` 凭证。
- `mvn -DskipTests compile`：通过。
- `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1 -Restart`：后端启动成功，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端买家列表：`code=200`。
- 管理端生成买家端免密票据：`code=200`。
- 买家端消费免密票据：`code=200`，`terminal=buyer`。
- buyer token 初次调用 `/buyer/getInfo`：`code=200`。
- 受控远程 DML：仅将本轮新建测试 session 对应 `buyer_session` 行更新为 `status='1'`、`logout_time=sysdate()`，影响行数 `1`；Redis token 保持不变。
- DB session 失效后，旧 buyer token 再调 `/buyer/getInfo`：业务 `code=401`，消息为“登录状态已失效”。
- 受控远程 DML：将同一测试 session 临时恢复为 `status='0'`、`logout_time=null`，影响行数 `1`，随后调用 `/buyer/logout` 正常清理 Redis 和 DB session。
- `/buyer/logout`：`code=200`。
- logout 后旧 buyer token 再调 `/buyer/getInfo`：业务 `code=401`。
- 本轮早期 JShell 失败可能遗留的最近本地测试 session 清理检查：影响行数 `0`，未发现遗留在线测试 session。

当前判断：

- 买家端已经按卖家模板完成 DB 会话权威鉴权复制：Redis token 仍是入口缓存，但只要 `buyer_session` 已失效，受 `@PortalPreAuthorize(terminal = "buyer", ...)` 保护的 buyer 接口会拒绝旧 token。
- 当前实现没有新增表字段，`buyer_session` 现有 `status/logout_time/expire_time` 字段足够承载该规则。
- seller/buyer 两端 DB 会话权威鉴权模板已对齐；后续真实端内业务接口接入 `@PortalPreAuthorize` 时会自然复用该兜底校验。
- 管理端强制踢出执行链已能让 token 失效，但管理端 session 列表查询仍不完整，需后续补齐。

## 2026-06-04 管理端卖家会话列表后端模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家先做标准模板，验收通过后复制买家”的节奏，只处理一类问题：管理端在强制踢出前后需要能只读查看卖家端主体或端账号的 session 列表。

已完成：

- `AdminSellerController` 新增 `GET /seller/admin/sellers/{sellerId}/sessions/list`，查询某个卖家主体下的 session 列表。
- `AdminSellerController` 新增 `GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions/list`，查询某个卖家端账号的 session 列表。
- 两个接口复用现有管理端权限 `seller:admin:forceLogout`，不新增菜单权限 DML；能强制踢出的管理员才允许查看会话 IP、登录时间等信息。
- `ISellerService` / `SellerServiceImpl` 新增对应只读查询方法。
- `SellerMapper.xml` 的 `selectSellerSessionProfileList` 支持 `sellerAccountId` 可选，主体级查询不传账号 ID，账号级查询传账号 ID。
- 响应复用 `PortalSessionProfile`，`tokenId` 继续通过 `@JsonIgnore` 不输出给前端。
- 本轮不改 buyer 代码，不新增 DDL/DML，不改前端。
- 新增执行记录：`docs/plans/2026-06-04-admin-seller-session-list-execution-record.md`。

验证结果：

- 数据源确认：后端激活 `druid`，MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮不输出 `.env.local` 凭证。
- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1 -Restart`：后端启动成功，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端卖家列表：`code=200`。
- 管理端生成卖家端免密票据：`code=200`。
- 卖家端消费免密票据：`code=200`。
- `GET /seller/admin/sellers/{sellerId}/sessions/list?pageNum=1&pageSize=10`：`code=200`。
- `GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions/list?pageNum=1&pageSize=10`：`code=200`。
- 主体级和账号级会话列表响应均未输出 `tokenId`。
- 烟测后调用 `/seller/logout` 清理本轮 seller token；随后管理端主体级强制清理返回 `data=0`，说明没有额外在线测试会话遗留。

当前判断：

- 管理端卖家 session 列表后端模板已落地。
- 该模板保持只读，不承担强制踢出动作；强制踢出仍继续使用现有 DELETE 接口。
- 因为 RuoYi `startPage()` 只作用于 Service 内第一条查询，列表 Service 方法不做前置校验查询，避免分页被校验 SQL 消耗；数据范围由 SQL 中的 `seller_id` / `seller_account_id` 限定。
- buyer 端已在后续检查点按同一模板复制，且只替换 buyer controller、service、mapper、表名、字段名和路径。
- 管理端前端 session 列表 UI 尚未接入。

## 2026-06-04 管理端买家会话列表后端复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板验收通过后复制买家”的节奏，只处理一类问题：管理端在强制踢出前后需要能只读查看买家端主体或端账号的 session 列表。

已完成：

- `AdminBuyerController` 新增 `GET /buyer/admin/buyers/{buyerId}/sessions/list`，查询某个买家主体下的 session 列表。
- `AdminBuyerController` 新增 `GET /buyer/admin/buyers/{buyerId}/accounts/{accountId}/sessions/list`，查询某个买家端账号的 session 列表。
- 两个接口复用现有管理端权限 `buyer:admin:forceLogout`，不新增菜单权限 DML；能强制踢出的管理员才允许查看会话 IP、登录时间等信息。
- `IBuyerService` / `BuyerServiceImpl` 新增对应只读查询方法。
- `BuyerMapper.xml` 的 `selectBuyerSessionProfileList` 支持 `buyerAccountId` 可选，主体级查询不传账号 ID，账号级查询传账号 ID。
- 响应复用 `PortalSessionProfile`，`tokenId` 继续通过 `@JsonIgnore` 不输出给前端。
- 本轮不改 seller 代码，不新增 DDL/DML，不改前端。
- 新增执行记录：`docs/plans/2026-06-04-admin-buyer-session-list-execution-record.md`。

验证结果：

- 数据源确认：后端激活 `druid`，MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮不输出 `.env.local` 凭证。
- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1 -Restart`：后端启动成功，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端买家列表：`code=200`。
- 管理端生成买家端免密票据：`code=200`。
- 买家端消费免密票据：`code=200`。
- `GET /buyer/admin/buyers/{buyerId}/sessions/list?pageNum=1&pageSize=10`：`code=200`。
- `GET /buyer/admin/buyers/{buyerId}/accounts/{accountId}/sessions/list?pageNum=1&pageSize=10`：`code=200`。
- 主体级和账号级会话列表响应均未输出 `tokenId`。
- 烟测后调用 `/buyer/logout` 清理本轮 buyer token；随后管理端主体级强制清理返回 `data=0`，说明没有额外在线测试会话遗留。

当前判断：

- 管理端买家 session 列表后端模板已按卖家模板复制完成。
- 该模板保持只读，不承担强制踢出动作；强制踢出仍继续使用现有 DELETE 接口。
- 因为 RuoYi `startPage()` 只作用于 Service 内第一条查询，列表 Service 方法不做前置校验查询，避免分页被校验 SQL 消耗；数据范围由 SQL 中的 `buyer_id` / `buyer_account_id` 限定。
- seller/buyer 两端管理端 session 列表后端模板已对齐。
- 管理端前端 session 列表 UI 尚未接入。

## 2026-06-04 管理端卖家会话列表 UI 模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：管理端卖家列表和卖家账号列表需要能只读查看端内 session 列表。

已完成：

- 新增 `react-ui/src/components/PartnerManagement/PartnerSessionModal.tsx`，作为管理端主体/账号会话只读弹窗。
- `PartnerManagementPage` 在卖家主体行“更多”中接入“会话”入口；仅当当前端配置了 `listSubjectSessions` 且管理员具备 `seller:admin:forceLogout` 时展示。
- `PartnerAccountModal` 在卖家账号行“更多”中接入“会话”入口；仅当当前端配置了 `listAccountSessions` 且管理员具备 `seller:admin:forceLogout` 时展示。
- `react-ui/src/services/seller/seller.ts` 新增 seller 主体级和账号级 session list service。
- `react-ui/src/pages/Seller/index.tsx` 只接入 seller service；本轮不改 buyer UI、不新增 DDL/DML、不新增权限点。
- 浏览器验收截图：`output/playwright/admin-seller-session-modal.png`。

验证结果：

- 数据源确认：后端激活 `druid`，`.env.local` 中 MySQL 和 Redis 均为远端/非本机；本轮未输出连接明文。
- `npm run tsc`：通过。
- `npx biome lint src/components/PartnerManagement/PartnerSessionModal.tsx src/components/PartnerManagement/PartnerManagementPage.tsx src/components/PartnerManagement/PartnerAccountModal.tsx src/services/seller/seller.ts src/pages/Seller/index.tsx`：通过。
- `git diff --check -- react-ui/src/components/PartnerManagement/PartnerSessionModal.tsx react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx react-ui/src/services/seller/seller.ts react-ui/src/pages/Seller/index.tsx`：通过，仅有 LF/CRLF 提示。
- 全仓 `npm run biome:lint`：未通过，失败点主要为既有文件；本轮新增的非空断言问题已修复，定向 lint 已通过。
- 管理端接口冒烟：`GET /seller/admin/sellers/{sellerId}/sessions/list` 返回 `code=200`，响应不含 `tokenId` / `token`。
- 管理端接口冒烟：`GET /seller/admin/sellers/{sellerId}/accounts/{sellerAccountId}/sessions/list` 返回 `code=200`，响应不含 `tokenId` / `token`。
- Playwright 浏览器验收：`/partner/seller` 卖家列表加载正常；主体“更多 / 会话”弹窗请求 200；账号弹窗内“更多 / 会话”弹窗请求 200；弹窗展示状态、登录账号、登录 IP、登录时间、过期时间、退出时间；未展示 `tokenId`、JWT、Redis key；console error 数量为 0。

当前判断：

- 卖家管理端会话列表 UI 已形成可复制模板。
- 该 UI 只读展示会话，不承担强制踢出动作；强制踢出仍使用现有 DELETE 接口。
- buyer UI 仍未复制；后续只替换 `buyer` service、字段配置、文案和权限前缀，不改 `PartnerSessionModal` 通用逻辑。

## 2026-06-04 管理端买家会话列表 UI 复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：把已验收的 seller 管理端会话列表 UI 模板复制到 buyer。

已完成：

- `react-ui/src/services/buyer/buyer.ts` 新增 `getAdminBuyerSessions(...)`。
- `react-ui/src/services/buyer/buyer.ts` 新增 `getAdminBuyerAccountSessions(...)`。
- `react-ui/src/pages/Buyer/index.tsx` 接入 `listSubjectSessions: getAdminBuyerSessions`。
- `react-ui/src/pages/Buyer/index.tsx` 接入 `listAccountSessions: getAdminBuyerAccountSessions`。
- 本轮不改 `PartnerSessionModal` 通用逻辑，不改 seller UI，不新增 DDL/DML，不新增权限点。
- 浏览器验收截图：`output/playwright/admin-buyer-session-modal.png`。

验证结果：

- 数据源确认：后端激活 `druid`，`.env.local` 中 MySQL 和 Redis 均为远端/非本机；本轮未输出连接明文。
- `npm run tsc`：通过。
- `npx biome lint src/components/PartnerManagement/PartnerSessionModal.tsx src/components/PartnerManagement/PartnerManagementPage.tsx src/components/PartnerManagement/PartnerAccountModal.tsx src/services/buyer/buyer.ts src/pages/Buyer/index.tsx`：通过。
- 管理端接口冒烟：`GET /buyer/admin/buyers/{buyerId}/sessions/list` 返回 `code=200`，响应不含 `tokenId` / `token`。
- 管理端接口冒烟：`GET /buyer/admin/buyers/{buyerId}/accounts/{buyerAccountId}/sessions/list` 返回 `code=200`，响应不含 `tokenId` / `token`。
- Playwright 浏览器验收：`/partner/buyer` 买家列表加载正常；主体“更多 / 会话”弹窗请求 200；账号弹窗内“更多 / 会话”弹窗请求 200；弹窗展示状态、登录账号、登录 IP、登录时间、过期时间、退出时间；未展示 `tokenId`、JWT、Redis key；console error 数量为 0。

当前判断：

- 管理端 seller/buyer 会话列表 UI 已对齐，均复用 `PartnerSessionModal`。
- buyer UI 复制只替换了 buyer service 和 Buyer 页面配置，保留 `buyerAccountId`、`buyer_level`、`showRechargePlaceholder` 等 buyer 专属配置。
- 会话列表 UI 仍为只读展示；强制踢出继续使用现有 DELETE 接口。

## 2026-06-04 卖家端商品 Schema 前端消费模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：在当前 `react-ui/` 卖家端工作台真实消费 seller 商品分类与 Schema 只读接口。

已完成：

- 新增 `react-ui/src/pages/Portal/Home/SellerProductSchemaPreview.tsx`，作为卖家端商品 Schema 前端消费模板。
- `react-ui/src/services/portal/session.ts` 新增 seller 商品分类与 Schema 只读 service，继续使用 seller portal token，并显式 `isToken: false`。
- `react-ui/src/types/seller-buyer/party.d.ts` 新增端内商品分类、属性选项和 Schema 响应类型。
- `react-ui/src/pages/Portal/Home/index.tsx` 仅在 `terminal === 'seller'` 时挂载商品发布准备卡片；buyer 不展示。
- 当前账号会话表按前端请求意图只展示 5 条，并修复 Ant Design v6 弃用告警和 row key 冲突。
- 本轮不改后端、不新增 DDL/DML、不改权限 seed、不复制 buyer。

验证结果：

- 数据源确认：后端激活 `druid`，`.env.local` 中 MySQL 和 Redis 均为远端/非本机；本轮未输出连接明文。
- `npm run tsc`：通过。
- `npx biome lint src/pages/Portal/Home/index.tsx src/pages/Portal/Home/SellerProductSchemaPreview.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts`：通过。
- `git diff --check -- react-ui/src/types/seller-buyer/party.d.ts react-ui/src/services/portal/session.ts react-ui/src/pages/Portal/Home/index.tsx react-ui/src/pages/Portal/Home/SellerProductSchemaPreview.tsx`：通过，仅有 LF/CRLF 提示。
- Playwright 浏览器验收：`/seller/portal` 商品发布准备卡片可见；`GET /api/seller/product/categories` 返回 200；`GET /api/seller/product/categories/446/schema` 返回 200；Schema 表格展示 8 行；console error 数量为 0；页面无横向溢出；浏览器未注入管理端 `access_token`。
- Playwright 浏览器验收：`/buyer/portal` 不展示商品发布准备卡片；console error 数量为 0。
- 截图：`output/playwright/seller-portal-product-schema-ui.png`、`output/playwright/buyer-portal-without-product-schema-ui.png`。

当前判断：

- 卖家端商品 Schema 前端消费模板已形成。
- 买家端复制应作为后续单独切片，只替换 terminal、路径、权限点、文案和 service。
- 当前 `ProductPortalSchemaController` 位于 `product` 模块但暴露 seller/buyer 路径，鉴权合规；是否迁移为 seller/buyer facade controller 后续单独评估，不在本切片顺手处理。

## 2026-06-05 买家端商品 Schema 前端消费复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：把已验收的 seller 商品 Schema 前端消费模板复制到 buyer 工作台。

已完成：

- 新增 `react-ui/src/pages/Portal/Home/BuyerProductSchemaPreview.tsx`，作为买家端商品 Schema 前端消费入口。
- `react-ui/src/pages/Portal/Home/SellerProductSchemaPreview.tsx` 保留 seller 包装入口，并抽出 `PortalProductSchemaPreview` 作为 seller/buyer 共用预览模板，避免复制整份表格逻辑。
- `react-ui/src/services/portal/session.ts` 新增 buyer 商品分类与 Schema 只读 service，继续使用 buyer portal token，并显式 `isToken: false`。
- `react-ui/src/pages/Portal/Home/index.tsx` 在 `terminal === 'buyer'` 时挂载买家端商品浏览准备卡片；seller 入口继续使用卖家端商品发布准备卡片。
- 本轮只复制 buyer 前端消费模板，不改后端、不新增 DDL/DML、不改权限 seed。

验证结果：

- 数据源确认：后端激活 `druid`，MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- `npm run tsc`：通过。
- `npx biome lint src/pages/Portal/Home/index.tsx src/pages/Portal/Home/SellerProductSchemaPreview.tsx src/pages/Portal/Home/BuyerProductSchemaPreview.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts`：通过。
- Playwright 浏览器验收：`/buyer/portal` 商品浏览准备卡片可见；`GET /api/buyer/product/categories` 返回 200；`GET /api/buyer/product/categories/446/schema` 返回 200；Schema 表格展示 8 行；console error 数量为 0；page error 数量为 0；页面无横向溢出；浏览器未注入管理端 `access_token`。
- Playwright 回归：`/seller/portal` 商品发布准备卡片仍可见；`GET /api/seller/product/categories` 返回 200；`GET /api/seller/product/categories/446/schema` 返回 200；Schema 表格展示 8 行；console error 数量为 0；page error 数量为 0。
- 本轮 Playwright 成功验收生成并消费了 seller/buyer 免密票据，验收结束已调用 `/seller/logout` 和 `/buyer/logout` 清理本轮新 token；第一次浏览器脚本因中文字符串管道转码失败中断，已消费的端 token 未在聊天中输出，未执行管理端强制踢出以避免误伤真实在线会话。
- 截图：`output/playwright/buyer-portal-product-schema-ui.png`、`output/playwright/seller-portal-product-schema-ui-regression.png`。

当前判断：

- 买家端商品 Schema 前端消费已按卖家模板复制完成，只替换 terminal、路径、权限点、文案和 service。
- seller/buyer 端商品 Schema 前端消费模板已对齐，后续正式商品发布、浏览、筛选和详情页面优先复用这套端 token、端 service 和 product schema 只读模板。
- 当前 `ProductPortalSchemaController` 位于 `product` 模块但暴露 seller/buyer 路径，鉴权合规；是否迁移为 seller/buyer facade controller 后续单独评估，不在本切片顺手处理。

## 2026-06-05 卖家商品 Schema facade 迁移检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板先做、验收后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：把 seller 商品分类和 Schema 端入口从 product controller 迁入 seller 模块 facade，避免 product 共享域继续承载 seller 端入口。

已完成：

- 新增 `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductPortalSchemaService.java`，作为 seller/buyer portal 只读商品分类与 Schema 的窄接口。
- 新增 `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductPortalSchemaServiceImpl.java`，集中承载分类过滤、schema 计算和端内 DTO 脱敏映射。
- `RuoYi-Vue/seller/pom.xml` 新增对 `product` 模块的依赖，依赖方向为 `seller -> product -> ruoyi-system`，不形成循环。
- 新增 `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductSchemaController.java`，承载 `GET /seller/product/categories` 和 `GET /seller/product/categories/{categoryId}/schema`。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/ProductPortalSchemaController.java` 移除 seller 映射，只保留 buyer 映射；buyer facade 迁移后续另起切片。
- seller 路径、权限点和日志 title 保持不变，不新增 DDL/DML，不改权限 seed，不改前端。

验证结果：

- 数据源确认：后端激活 `druid`，MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests compile`：通过。
- 执行 `mvn -DskipTests package`：通过，`ruoyi-admin.jar` 已重新打包。
- 执行 `.\start-backend-local.ps1 -Restart`：新后端启动成功，8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- seller token 调 `GET /seller/product/categories`：`code=200`，返回 160 个分类。
- seller token 调 `GET /seller/product/categories/{categoryId}/schema`：`code=200`，返回 8 个 schema 字段。
- buyer token 调 `GET /buyer/product/categories`：`code=200`，返回 160 个分类，证明 buyer 当前入口未被误伤。
- buyer token 调 `GET /buyer/product/categories/{categoryId}/schema`：`code=200`，返回 8 个 schema 字段。
- 无 token 调 seller 分类接口：`code=401`。
- admin token 调 seller 分类接口：`code=401`。
- buyer token 调 seller 分类接口：`code=401`。
- seller token 调 buyer 分类接口：`code=401`。
- 验证结束后已调用 `/seller/logout` 和 `/buyer/logout` 清理本轮新 token。

当前判断：

- seller 商品分类和 Schema 端入口已符合 terminal ownership：seller 路径由 seller 模块 facade 承载，product 模块只提供共享只读 schema 服务。
- seller facade 方法级保留 `@Anonymous`、`@PortalPreAuthorize(terminal = "seller", ...)`、`@PortalLog(terminal = "seller", ...)`，不使用类级匿名，不使用管理端 `@PreAuthorize`。
- buyer 端入口已在后续切片按 seller 模板迁入 `BuyerPortalProductSchemaController`；本检查点中的 buyer 残留判断已由 2026-06-05 买家商品 Schema facade 迁移检查点关闭。
- seller/buyer 同构的是权限、DTO、脱敏、product schema 服务复用和前端消费方式；controller 归属也已完成 terminal 模块收口。

## 2026-06-05 买家商品 Schema facade 迁移检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家 facade 验证之后，只处理一类问题：把 buyer 商品分类和 Schema 端入口从 product controller 迁入 buyer 模块 facade。业务规则、前端字段、权限 seed 和路由路径保持不变。

已完成：

- `RuoYi-Vue/buyer/pom.xml` 新增对 `product` 模块的依赖，依赖方向为 `buyer -> product -> ruoyi-system`，不形成循环。
- 新增 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductSchemaController.java`，承载 `GET /buyer/product/categories` 和 `GET /buyer/product/categories/{categoryId}/schema`。
- 删除 `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/ProductPortalSchemaController.java`；product 模块不再暴露 seller/buyer 端入口，只保留 `IProductPortalSchemaService` 共享只读服务。
- buyer 路径、权限点和日志 title 保持不变，不新增 DDL/DML，不改权限 seed，不改前端。

验证结果：

- 执行 `mvn -DskipTests compile`：通过。
- 执行 `mvn -DskipTests package`：通过，`ruoyi-admin.jar` 已重新打包。
- 执行 `.\start-backend-local.ps1 -Restart`：新版后端启动成功，8080 正常监听。
- `/captchaImage`：`code=200`。
- seller token 调 `GET /seller/product/categories`：`code=200`，返回 160 个分类。
- seller token 调 `GET /seller/product/categories/{categoryId}/schema`：`code=200`，返回 8 个 schema 字段，敏感字段检查为 false。
- buyer token 调 `GET /buyer/product/categories`：`code=200`，返回 160 个分类。
- buyer token 调 `GET /buyer/product/categories/{categoryId}/schema`：`code=200`，返回 8 个 schema 字段，敏感字段检查为 false。
- buyer token 携带伪造 `buyerId`、`sellerId`、`accountId`、`terminal` 参数调用 buyer 分类接口：`code=200`，返回 160 个分类，结果未因参数扩大范围。
- 无 token、admin token、seller token 调 buyer 分类接口：均为 `code=401`。
- seller token 调 buyer schema 接口：`code=401`。
- buyer token 调 seller 分类接口和 seller schema 接口：均为 `code=401`。
- 验证结束后已调用 `/buyer/logout`、`/seller/logout` 清理本轮新 token，登出后旧 token 调 `/buyer/getInfo`、`/seller/getInfo` 均返回 `code=401`。

当前判断：

- seller/buyer 商品分类和 Schema 端入口均已符合 terminal ownership：端路径由各自 terminal 模块 facade 承载，product 只提供共享只读 schema 服务。
- buyer facade 方法级保留 `@Anonymous`、`@PortalPreAuthorize(terminal = "buyer", ...)`、`@PortalLog(terminal = "buyer", ...)`，不使用类级匿名，不使用管理端 `@PreAuthorize`。
- buyer facade 只委托 `IProductPortalSchemaService`，没有复制商品分类过滤、schema 继承合并或 DTO 映射逻辑。

## 2026-06-05 terminal ownership 文档一致性检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，不改代码、不执行 SQL，只清理 seller/buyer/product 端入口归属相关的陈旧文档，避免后续误按旧状态开发。

已完成：

- 静态扫描 `RuoYi-Vue` 下 `@RequestMapping` / `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` 的 seller/buyer 绝对路径；当前未发现 seller/buyer 端路径继续落在 product 模块。
- 更新 `docs/architecture/reuse-ledger.md`，把“buyer 商品分类与 schema 入口仍由 product 模块承载、待迁移”的旧表述改为 buyer 已迁入 `buyer` 模块 facade。
- 更新 `docs/plans/2026-06-05-mall-product-distribution-implementation-record.md`，把管理端商城商品接口路径修正为当前代码实际使用的 `/product/admin/distribution-products`。

验证结果：

- 本轮不涉及后端代码、前端代码或数据库变更。
- 陈旧表述复查：`当前 buyer 商品分类与 schema 入口仍由 product 模块承载`、`待后续单独迁移` 已不再命中当前台账。
- `git diff --check -- docs/architecture/reuse-ledger.md docs/plans/2026-06-05-mall-product-distribution-implementation-record.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 提示。
- `codegraph sync .`：通过，输出 `Already up to date`。

## 2026-06-05 端入口归属测试守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：防止 `product` 共享模块重新暴露 seller/buyer 端入口。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`。
- 测试扫描 `RuoYi-Vue/product/src/main/java` 下的 Java 源码。
- 若 `product` 模块出现 `@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping`、`@PatchMapping` 直接暴露 `/seller...` 或 `/buyer...` 路径，测试失败。
- 本轮不新增接口、不改 UI、不执行 SQL、不复制 buyer。
- 新增记录：`docs/plans/2026-06-05-terminal-route-ownership-test-record.md`。

验证结果：

- 初始端入口归属测试：`cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- 当前该测试类已扩展到 3 个测试；最新验证见后续“端内 Controller 鉴权模板测试守卫检查点”。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java docs/plans/2026-06-05-terminal-route-ownership-test-record.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 提示。
- `codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- 当前 `product` 模块没有 seller/buyer 端入口回退。
- seller/buyer 商品端入口继续由各自 terminal facade controller 承载，`product` 只保留共享商品领域服务和管理端 `/product/admin/**` 能力。
- 本测试不替代后续端内真实业务鉴权；真实业务接口仍要继续接入 `@PortalPreAuthorize` 和从当前 token 推导主体范围。

## 2026-06-05 端内 Controller 鉴权模板测试守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：把 seller/buyer 受保护 portal handler 的鉴权模板固化成自动化测试。

已完成：

- 扩展 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`。
- 先增加 seller 模板检查，覆盖 `SellerPortalController` 和 `SellerPortalProductSchemaController`。
- seller 模板验证通过后，按同一 helper 复制 buyer 检查，只替换 terminal 和 controller 路径，覆盖 `BuyerPortalController` 和 `BuyerPortalProductSchemaController`。
- 检查内容包括：
  - 受保护 portal controller 不允许类级 `@Anonymous`。
  - 受保护 portal controller 不允许使用管理端 `@PreAuthorize`。
  - 每个 handler 必须方法级声明 `@Anonymous`。
  - 每个 handler 必须声明 `@PortalPreAuthorize(terminal = "...")`。
  - 每个 handler 必须声明 `@PortalLog(terminal = "...")`。
  - 每个 handler 必须从 `PortalSessionContext.requireSession(...)` 派生当前端身份。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内 Controller 鉴权模板守卫。
- 更新执行记录：`docs/plans/2026-06-05-terminal-route-ownership-test-record.md`。

验证结果：

- seller 模板验证：`cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- buyer 复制后验证：`cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java docs/plans/2026-06-05-terminal-route-ownership-test-record.md docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 提示。
- `codegraph sync .`：通过；代码变更同步时输出 `Synced 1 changed files`，记录补充后最终复跑输出 `Already up to date`。

当前判断：

- seller/buyer 当前受保护 portal handler 均符合端内鉴权模板。
- 该测试不覆盖登录和免密消费入口；这些属于认证入口例外，仍由登录接口自身验证。
- 该测试只证明端入口模板不漏；具体业务权限点、数据范围、字段脱敏和审计内容仍需在后续真实业务接口中继续验证。

## 2026-06-05 端账号权限 sys_* 回退测试守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按三端账号权限独立的核心要求，只处理一类问题：防止 seller/buyer 模块重新依赖管理端 `sys_*` 账号权限控制面。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java`。
- 测试扫描 `seller`、`buyer` 模块的 `src/main/java` 和 `src/main/resources`。
- 若端模块源码或 mapper XML 中出现以下回退引用，测试失败：
  - `sys_user`
  - `sys_role`
  - `sys_menu`
  - `sys_dept`
  - `sys_user_role`
  - `sys_role_menu`
  - `SysUser`
  - `SysRole`
  - `SysMenu`
  - `SysDept`
  - `PortalAccountSupport`
  - `PortalAccountMapper`
  - `seller_account.user_id`
  - `buyer_account.user_id`
- 本测试不扫描 `ruoyi-system`、`ruoyi-admin` 或 `RuoYi-Vue/sql`，因为管理端后台和若依初始化脚本合法使用 `sys_*`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 `TerminalAccountIsolationTest`。
- 新增记录：`docs/plans/2026-06-05-terminal-account-isolation-test-record.md`。

验证结果：

- `cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalAccountIsolationTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java docs/plans/2026-06-05-terminal-account-isolation-test-record.md docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 提示。
- `codegraph sync .`：通过；代码变更同步时输出 `Synced 1 changed files`，记录补充后最终复跑输出 `Already up to date`。

当前判断：

- 当前 seller/buyer 模块没有明显回退到管理端 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 控制面。
- 当前 seller/buyer 模块没有恢复旧 `PortalAccountSupport` / `PortalAccountMapper`。
- 本测试只证明端账号权限控制面没有明显回退；真实业务接口的数据范围、权限点、脱敏和接口烟测仍需在对应业务切片中继续验证。

## 2026-06-05 管理端卖家标准列表模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：管理端卖家管理列表模板。

已完成：

- `PartnerModuleConfig` 新增 `listTemplate` 和 `searchStorageKey` 配置。
- `PartnerManagementPage` 增加标准列表模板开关，不直接影响未启用的买家入口。
- `react-ui/src/pages/Seller/index.tsx` 启用 `listTemplate: 'standard'`。
- 标准列表把编号/代码、名称/简称、登录账号/等级压缩为上下两行展示。
- 创建时间和最后登录时间继续在“时间”列上下展示，空值显示 `-`。
- 本轮不改后端、不改菜单、不改权限 seed、不执行 SQL、不复制 buyer。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Seller/index.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi; git diff --check -- react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx react-ui/src/pages/Seller/index.tsx`：通过，仅有 LF/CRLF 工作区换行提示。
- Playwright / Chrome 验收 `/partner/seller`：通过；1366x768 下 `bodyOverflowX=false`、`tableOverflowX=false`；卖家列表接口 HTTP 200、业务 `code=200`、返回 3 行；console error 和 page error 均为 0；截图 `output/playwright/admin-seller-standard-template-1366.png`。
- 筛选区状态保存验证：点击“收起”后，浏览器写入 `proTableSearch:collapsed:admin-seller-management=true`。

当前判断：

- 卖家管理标准列表模板已通过代码层和浏览器验收。
- 买家管理暂不启用该模板；待卖家页面浏览器验收通过后，再按同一配置复制买家，只替换端类型、字段配置、权限标识、文案和 service。
- `PartnerManagementPage.tsx` 是既有大文件，本轮没有继续拆分；后续如果继续扩展列表或账号弹窗，应优先拆组件。

## 2026-06-05 管理端买家标准列表模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在卖家标准列表模板验收通过后，按同一模板复制到买家管理，只替换端类型、字段配置、权限标识、文案和 service。

已完成：

- `react-ui/src/pages/Buyer/index.tsx` 启用 `listTemplate: 'standard'`。
- `react-ui/src/pages/Buyer/index.tsx` 设置 `searchStorageKey: 'admin-buyer-management'`。
- 买家继续保留充值占位列，不接入充值功能、不新增弹窗。
- `PartnerManagementPage` 标准模板操作列改为不换行操作组，避免“更多”在买家多一列时竖排。
- `proTableSearch.ts` 将 `xl` 断点调整为 6 列展示，保证常见 1366 桌面宽度下筛选区默认展开且不与表格重叠。
- 本轮不改后端、不改菜单、不改权限 seed、不执行 SQL。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/utils/proTableSearch.ts src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Seller/index.tsx src/pages/Buyer/index.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- Playwright / Chrome 验收 `/partner/buyer`：通过；1366x900 下 `bodyOverflowX=false`、`tableOverflowX=false`；搜索区默认展开，包含创建时间和最后登录时间；搜索区与表格不重叠；买家列表接口 HTTP 200、业务 `code=200`、返回 1 行；console error 和 page error 均为 0；截图 `output/playwright/admin-buyer-standard-template-expanded.png`。
- Playwright / Chrome 回归 `/partner/seller`：通过；1366x900 下 `bodyOverflowX=false`、`tableOverflowX=false`；搜索区默认展开且与表格不重叠；卖家列表接口 HTTP 200、业务 `code=200`、返回 3 行；console error 和 page error 均为 0；截图 `output/playwright/admin-seller-standard-template-expanded.png`。

当前判断：

- 管理端卖家/买家标准列表模板已对齐。
- 买家复制没有新增后端能力，只启用同一前端模板配置。
- 后续如继续扩展账号、部门、角色、菜单或审计弹窗，应另起切片，不混在列表模板里。

## 2026-06-05 PortalTokenSupport 端 token 隔离测试守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“每个切片只改一类东西”的节奏，只补一类测试守卫：固定 seller/buyer 端 token 的 terminal claim 与 Redis key 隔离边界。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalTokenSupportTest.java`。
- 测试 `createLogin(...)` 写入 `portal_login_tokens:seller:seller_...`，不会写入管理端 `login_tokens:`。
- 测试 `getSession(...)` 必须同时匹配 JWT 中的 `portal_terminal` 和 Redis session 中的 terminal。
- 测试 seller token 不能作为 buyer session 使用，`requireSession("buyer")` 返回未授权异常。
- 测试 `deleteLoginTokens(...)` 和 `deleteLoginToken(...)` 均按 terminal 拼接删除 key。
- 新增执行记录：`docs/plans/2026-06-05-portal-token-support-isolation-test-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 `PortalTokenSupportTest` 和当前短期配置边界。

验证结果：

- `cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalTokenSupportTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalTokenSupportTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-portal-token-support-isolation-test-record.md`：通过，仅有 LF/CRLF 提示。
- `codegraph sync .`：通过；代码变更同步时输出 `Synced 1 changed files`，记录补充后最终复跑输出 `Already up to date`。

当前判断：

- 当前 `PortalTokenSupport` 的端 token、端 Redis key 和端 session terminal 校验已有自动化守卫。
- 本测试不改变当前短期配置：seller/buyer 仍共享 `token.secret`、`token.header` 和 `token.expireTime`，隔离依赖 `portal_terminal` claim 与 `portal_login_tokens:{terminal}:{tokenId}`。
- 验证码、登录失败限流、独立 token 配置或独立 Spring Security filter chain 属于后续更大切片，不混入本轮测试守卫。

## 2026-06-05 管理端动态菜单登录兜底检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：管理端动态菜单页面未登录直达时的登录兜底。

已完成：

- `react-ui/src/app.tsx` 新增统一 `redirectToLogin()`，登录跳转保留当前目标地址。
- `fetchUserInfo()`、`layout.onPageChange()`、`onRouteChange()` 和 `render()` 统一使用该登录兜底。
- 未登录直达非 portal 动态菜单页面时，先跳登录并保留 `redirect`，不再落入静态 404。
- `react-ui/src/services/session.ts` 增加动态路由 patch 的 `proLayout` 空保护。
- 新增执行记录：`docs/plans/2026-06-05-admin-dynamic-menu-login-redirect-record.md`。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/app.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/services/session.ts`：未处理；该路径被当前 Biome 配置忽略，改由 `npm run tsc` 覆盖类型检查。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- Playwright / Chrome 验收未登录直达 `/partner/seller`：通过，最终地址为 `/user/login?redirect=%2Fpartner%2Fseller`，不是 404。
- Playwright / Chrome 验收管理端登录后直达 `/partner/seller`：通过，页面标题为 `卖家管理 - Ant Design Pro`，不是 404，`bodyOverflowX=false`，console error 为 0。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，代码变更同步时输出 `Synced 2 changed files`；记录补充后最终复跑结果见本轮回复。

当前判断：

- 管理端动态菜单未登录直达兜底已恢复到登录页 redirect 模式。
- portal 路由仍排除在管理端动态菜单加载和管理端登录态重定向之外。
- 本切片不改卖家/买家列表模板、不改账号权限模型、不执行 SQL；后续继续按卖家标准模板优先、验收后复制买家的节奏推进。

## 2026-06-05 管理端卖家账号弹窗模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：管理端卖家账号维护弹窗模板的浏览器验收细节。

已完成：

- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx` 收紧账号弹窗表格列宽。
- 账号弹窗宽度从 `1000` 调整为 `1040`，保证 1366 桌面视口下账号表格完整显示。
- 账号新增/编辑表单 Modal 增加 `forceRender`，避免隐藏表单 `useForm` 未挂载警告。
- 新增执行记录：`docs/plans/2026-06-05-admin-seller-account-modal-template-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/components/PartnerManagement/PartnerAccountModal.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- Playwright / Chrome 验收 `/partner/seller`：通过；卖家账号弹窗可打开；账号行“更多”展示“重置密码 / 会话 / 强制踢出”；新增账号弹窗展示登录账号、初始密码、部门、账号角色和状态；`bodyOverflowX=false`、`modalOverflowX=false`、`tableOverflowX=false`；console error 数量为 0。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，代码变更同步时输出 `Synced 1 changed files`；记录补充后最终复跑结果见本轮回复。

当前判断：

- 管理端卖家账号维护弹窗已形成可验收模板。
- 本切片不改后端、不执行 SQL、不复制买家；买家后续只按卖家模板替换端类型、文案、权限标识、字段配置和 service。
- 子 agent 只读检查发现的账号级免密代入、账号级日志入口、权限组合细化、索引补齐和服务层测试守卫均属于后续切片，不混入本轮 UI 验收修复。

## 2026-06-05 管理端卖家账号级免密模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类能力：管理端在卖家账号弹窗内，对指定卖家端账号生成 30 分钟有效的免密登录链接。

已完成：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java` 新增卖家账号级免密入口。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerService.java` 和 `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java` 新增账号级免密 service。
- 后端校验账号必须属于当前卖家，卖家停用时拒绝生成免密链接。
- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx` 的 service 配置增加可选 `directLoginAccount`。
- `react-ui/src/services/seller/seller.ts` 和 `react-ui/src/pages/Seller/index.tsx` 接入卖家账号级免密 service。
- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx` 在账号行“更多”里新增“登录卖家端”，填写原因后生成并打开免密链接。
- 新增执行记录：`docs/plans/2026-06-05-admin-seller-account-direct-login-template-record.md`。
- 本轮不复制买家、不执行 SQL、不改菜单 seed。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests package`：通过，并已用新 jar 重启后端。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/components/PartnerManagement/PartnerAccountModal.tsx src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Seller/index.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- Playwright / 系统 Chrome 验收 `/partner/seller`：通过；账号弹窗 `bodyOverflowX=false`、`modalOverflowX=false`、`tableOverflowX=false`；账号级免密接口 HTTP `200`、业务 `code=200`，返回目标 `accountId=8`、有效期 `30` 分钟、路径 `/seller/direct-login` 且包含 `directLoginToken`；console error / warning 和 page error 均为 `0`。

当前判断：

- 卖家账号级免密模板已通过代码层和浏览器验收。
- 买家侧暂不复制；下一步复制买家时只替换 terminal、文案、路径、权限标识和 buyer service。
- `PartnerAccountModal.tsx` 当前约 `580` 行，后续继续扩账号管理时优先拆账号表格、账号表单或操作区。

## 2026-06-05 管理端买家账号级免密模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，在卖家账号级免密模板验收通过后，只复制同一类能力到买家账号弹窗。

已完成：

- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java` 新增买家账号级免密入口。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java` 和 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java` 新增账号级免密 service。
- 后端校验账号必须属于当前买家，买家停用时拒绝生成免密链接。
- `react-ui/src/services/buyer/buyer.ts` 和 `react-ui/src/pages/Buyer/index.tsx` 接入买家账号级免密 service。
- 前端复用卖家已验收的 `PartnerAccountModal`，未新增公共组件分支。
- 新增执行记录：`docs/plans/2026-06-05-admin-buyer-account-direct-login-template-record.md`。
- 本轮不执行 SQL、不改菜单 seed、不改卖家已验收模板。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests package`：通过，并已用新 jar 重启后端。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Buyer/index.tsx src/services/buyer/buyer.ts`：通过；当前 Biome 配置只检查了 `src/pages/Buyer/index.tsx`，`src/services/buyer/buyer.ts` 被忽略。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过，覆盖 buyer service 类型检查。
- Playwright / 系统 Chrome 验收 `/partner/buyer`：通过；账号弹窗 `bodyOverflowX=false`、`modalOverflowX=false`、`tableOverflowX=false`；账号级免密接口 HTTP `200`、业务 `code=200`，返回目标 `accountId=2`、有效期 `30` 分钟、路径 `/buyer/direct-login` 且包含 `directLoginToken`；console error / warning 和 page error 均为 `0`。

当前判断：

- 管理端卖家/买家账号弹窗现在都具备账号级免密代入能力。
- 买家复制没有新增表、没有执行 SQL、没有改变公共组件交互。
- `BuyerServiceImpl.java` 当前约 `528` 行，后续继续扩买家账号、免密、会话或权限测试时，应考虑拆出 buyer account/direct-login service。

## 2026-06-05 端账号级免密服务测试守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“每个切片只改一类东西”的节奏，只补一类自动化守卫：管理端生成卖家/买家账号级免密票据时，必须绑定当前主体下的端内账号，不能跨主体生成票据。

已完成：

- 新增 `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`。
- 新增 `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`。
- `RuoYi-Vue/seller/pom.xml` 和 `RuoYi-Vue/buyer/pom.xml` 新增 JUnit test 依赖；POM 中 `product` 依赖为既有工作区改动，本轮没有回退。
- 卖家测试覆盖账号级免密成功入参、跨卖家账号拒绝、卖家停用拒绝。
- 买家测试覆盖账号级免密成功入参、跨买家账号拒绝、买家停用拒绝。
- 新增执行记录：`docs/plans/2026-06-05-terminal-account-direct-login-service-test-record.md`。
- 本轮不改接口行为、不改前端、不执行 SQL、不改远程数据库。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -Dtest=SellerServiceImplTest test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -Dtest=BuyerServiceImplTest test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am test`：通过；`ruoyi-system` 24 个测试、`finance` 9 个测试、`seller` 3 个测试、`buyer` 3 个测试均通过。

当前判断：

- 账号级免密代入已有 service 层测试守卫。
- 当前守卫只证明 service 不会对跨主体端内账号生成免密票据；接口鉴权、菜单权限和浏览器烟测仍按各切片单独验证。
- 新增测试未启动 Spring 容器、未连接数据库，适合作为后续重构 seller/buyer account/direct-login service 的快速回归。

## 2026-06-05 端账号角色绑定服务测试守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做卖家标准模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只补账号角色绑定 service 层自动化守卫。

已完成：

- 新增 `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplTest.java`。
- 新增 `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplTest.java`。
- 卖家测试覆盖正常绑定、清空绑定、跨卖家账号拒绝、跨卖家角色拒绝。
- 买家测试按卖家模板同构复制，覆盖正常绑定、清空绑定、跨买家账号拒绝、跨买家角色拒绝。
- 新增执行记录：`docs/plans/2026-06-05-terminal-account-role-binding-service-test-record.md`。
- 本轮不改接口行为、不改前端、不执行 SQL、不改远程数据库。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -Dtest=SellerPortalPermissionServiceImplTest test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -Dtest=BuyerPortalPermissionServiceImplTest test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am test`：通过；`ruoyi-system` 24 个测试、`finance` 9 个测试、`seller` 7 个测试、`buyer` 7 个测试均通过。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplTest.java docs/plans/2026-06-05-terminal-account-role-binding-service-test-record.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/architecture/reuse-ledger.md`：通过，仅有 LF/CRLF 提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 11 changed files`。

当前判断：

- 账号角色绑定已有 service 层数据范围守卫：账号必须属于当前主体，角色也必须属于当前主体。
- `PortalPermissionSupport.sanitizeIds(...)` 的过滤、去重规则已被正常绑定用例覆盖。
- 新增测试未启动 Spring 容器、未连接数据库，适合作为后续拆分或重构 seller/buyer portal permission service 的快速回归。
- 本轮没有做浏览器 UI 验收；账号角色弹窗 UI 已在前序切片验收，本轮只补服务层守卫。
## 2026-06-05 管理端卖家权限契约守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：卖家管理端控制面的权限契约自动化守卫。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java`。
- 自动扫描 `seller` 模块下所有 `AdminSeller*Controller.java`。
- 校验卖家管理端 controller 必须使用 `/seller/admin` 路由前缀。
- 校验卖家管理端 handler 必须使用 `seller:admin:*` 权限前缀。
- 校验 controller 中声明的卖家管理端权限必须存在于 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 校验变更类卖家管理端操作必须声明 `@Log`。
- 校验卖家管理端 controller 不得误用端内 `@Anonymous`、`@PortalPreAuthorize` 或 `@PortalLog`。
- 更新 `docs/architecture/reuse-ledger.md`，登记“卖家管理端权限契约守卫”。
- 新增执行记录：`docs/plans/2026-06-05-admin-seller-permission-contract-test-record.md`。
- 本轮已收回并关闭 6 个只读子 agent；子 agent 结果仅作为切片选择依据，未直接写入代码。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SellerAdminPermissionContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-admin-seller-permission-contract-test-record.md`：通过；仅有 Markdown LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码变更同步时输出 `Synced 2 changed files`，文档回填后最终复跑输出 `Already up to date`。
- 本切片未启动后端，未连接远程 MySQL / Redis，未执行 SQL，未改 UI，未复制买家。

当前判断：

- 卖家管理端权限模板已经有自动化守卫，后续新增 `AdminSeller*Controller` 时能及时发现权限前缀、seed 或 `@Log` 漏配。
- 买家复制暂不做；验收卖家守卫后，再按同一规则替换 terminal、controller 路径、权限前缀和 seed 权限集合。
- 免密票据 `tokenHash` 脱敏、审计详情 UI、低权限账号负向验证、AOP 执行级测试属于后续独立切片，不混入本轮。
## 2026-06-05 管理端买家权限契约守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，在卖家管理端权限契约守卫验收通过后，只复制同构守卫到买家管理端。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java`。
- 自动扫描 `buyer` 模块下所有 `AdminBuyer*Controller.java`。
- 校验买家管理端 controller 必须使用 `/buyer/admin` 路由前缀。
- 校验买家管理端 handler 必须使用 `buyer:admin:*` 权限前缀。
- 校验 controller 中声明的买家管理端权限必须存在于 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 校验变更类买家管理端操作必须声明 `@Log`。
- 校验买家管理端 controller 不得误用端内 `@Anonymous`、`@PortalPreAuthorize` 或 `@PortalLog`。
- 更新 `docs/architecture/reuse-ledger.md`，将“卖家管理端权限契约守卫”扩展为“卖家/买家管理端权限契约守卫”。
- 新增执行记录：`docs/plans/2026-06-05-admin-buyer-permission-contract-test-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=BuyerAdminPermissionContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" test`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`。
- 未加引号的组合测试命令曾被 PowerShell 解析失败；已用引号重跑通过，非代码问题。
- 本切片未启动后端，未连接远程 MySQL / Redis，未执行 SQL，未改 UI。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-admin-buyer-permission-contract-test-record.md`：通过；仅有 Markdown LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码变更同步时输出 `Synced 1 changed files`，文档回填后最终复跑输出 `Already up to date`。

当前判断：

- 卖家/买家管理端权限模板现在均有自动化守卫。
- 本轮只复制买家权限契约守卫，没有把买家复制扩大成 UI、接口或 SQL 改动。
- 免密票据 `tokenHash` 脱敏、审计详情 UI、低权限账号负向验证、AOP 执行级测试仍属于后续独立切片。
## 2026-06-05 免密票据 tokenHash 脱敏检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，只处理一类问题：管理端免密票据审计响应不暴露 `tokenHash`。本轮不改 UI、不改接口路径、不改 SQL、不连接远程 MySQL / Redis。

已完成：

- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginTicket.java` 为 `tokenHash` 字段增加 `@JsonIgnore`。
- 保留 `getTokenHash()` / `setTokenHash(...)`，Mapper、Support 和一次性消费链路仍可在后端内部使用 hash。
- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalDirectLoginTicketTest.java`。
- 测试覆盖 Jackson 序列化时保留普通审计字段，但不输出 `tokenHash` 字段名或 hash 值。
- 更新 `docs/architecture/reuse-ledger.md` 的 `PortalDirectLoginTicketMapper` 条目，登记 tokenHash 内部使用和响应脱敏边界。
- 新增执行记录：`docs/plans/2026-06-05-portal-direct-login-ticket-token-hash-redaction-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalDirectLoginTicketTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。
- 本切片未启动后端，未连接远程 MySQL / Redis，未执行 SQL，未改 UI。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginTicket.java RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalDirectLoginTicketTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-portal-direct-login-ticket-token-hash-redaction-record.md`：通过；仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码变更同步时输出 `Synced 4 changed files`，文档回填后最终复跑输出 `Already up to date`。

当前判断：

- 管理端 ticket 列表继续读取 `portal_direct_login_ticket` 审计字段，但 JSON 响应不再包含 `tokenHash`。
- 免密票据 hash 仍用于 `selectPortalDirectLoginTicketByTokenHash(...)` 和一次性消费校验，不影响 30 分钟一次性 token 链路。
- 审计详情 UI 字段补齐、低权限账号负向验证、`PortalDirectLoginSupport` 生命周期测试仍属于后续独立切片。

## 2026-06-05 免密登录 Support 生命周期测试检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板先做、验收后复制买家；每个切片只改一类东西”的节奏，只补一类自动化守卫：`PortalDirectLoginSupport` 生成、消费、拒绝、过期和原因校验的生命周期测试。本轮不改生产逻辑、不改 UI、不改接口路径、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java`。
- 使用 fake Redis、fake ticket mapper、fake config service、测试 SecurityContext 和 fake RequestContext 验证真实 `PortalDirectLoginSupport`。
- 覆盖生成免密 token：ticket 只保存 SHA-256 hash、写入 acting admin / target terminal / subject / account / reason / 30 分钟过期时间，Redis `portal_direct_login:{token}` 写入 payload 且 TTL 为 30 分钟，返回 URL 包含端地址和 `directLoginToken`。
- 覆盖消费免密 token：标记 `USED`、记录 used IP、`updateBy=system`、删除 Redis payload，二次消费被拒绝。
- 覆盖 seller token 被 buyer 端消费时拒绝，且不标记 used、不标记 expired、不删除 Redis payload。
- 覆盖过期 ticket 被消费时标记 `EXPIRED` 并删除 Redis payload。
- 覆盖空代入原因在落库和写 Redis 前被拒绝。
- 更新 `docs/architecture/reuse-ledger.md`，登记 `PortalDirectLoginSupportTest` 的守卫范围。
- 新增执行记录：`docs/plans/2026-06-05-portal-direct-login-support-lifecycle-test-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalDirectLoginSupportTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`。
- 首次定向测试因测试环境缺少 RequestContext，在 `IpUtils.getIpAddr()` 处暴露空请求上下文；已在测试中补 fake `HttpServletRequest`，生产逻辑未改。
- 本切片未启动后端，未连接远程 MySQL / Redis，未执行 SQL，未改 UI。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-portal-direct-login-support-lifecycle-test-record.md`：通过，仅有 Markdown LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- `PortalDirectLoginSupport` 现在有真实 support 层生命周期测试守卫，后续改免密票据、Redis payload、一次性消费、跨端拒绝或原因校验时能快速回归。
- 管理端审计详情 UI 字段补齐、低权限账号负向验证和端内直登浏览器消费流程仍属于后续独立切片。

## 2026-06-05 管理端卖家审计详情模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：管理端卖家审计弹窗详情字段补齐。本轮不改后端接口、不改权限点、不执行 SQL、不改买家业务 service。

已完成：

- 修改 `react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx`。
- 登录日志 tab 新增展开行详情：登录地点、浏览器、操作系统、登录提示。
- 操作日志 tab 新增展开行详情：请求地址、操作 IP、操作地点、方法名、异常信息。
- 免密票据 tab 新增展开行详情：目标端、签发人 ID、使用 IP、创建人、更新人、更新时间、代入原因、备注。
- 主表列不继续加宽，详情字段通过 `Descriptions` 展开行展示。
- 更新 `docs/architecture/reuse-ledger.md` 的 `PartnerAuditModal` 复用规则。
- 新增执行记录：`docs/plans/2026-06-05-admin-seller-audit-detail-template-record.md`。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/components/PartnerManagement/PartnerAuditModal.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- 数据源确认：后端激活 `druid`；MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- Playwright / Chrome 验收 `/partner/seller`：登录后打开卖家审计弹窗，通过工具栏“审计”进入。
- “免密票据”展开行已显示 `目标端`、`签发人ID`、`使用IP`、`创建人`、`更新人`、`更新时间`、`代入原因`、`备注`。
- “操作日志”展开行已显示 `请求地址`、`操作IP`、`操作地点`、`方法名`、`异常信息`。
- Playwright console 检查：`Errors: 0`。
- `cd E:\Urili-Ruoyi; git diff --check -- react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-admin-seller-audit-detail-template-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- 管理端卖家审计详情模板已补齐主要可追溯字段，管理端控制权审计可见性更完整。
- 买家侧未单独打开验证；由于当前为 seller/buyer 共用 `PartnerAuditModal`，后续买家只需按同构入口做浏览器验收，不需要重新设计。
- `PartnerAuditModal.tsx` 当前约 435 行，已超过 400 行自检阈值；本轮暂不拆分，因为职责仍集中在只读审计弹窗，后续继续增加导出、详情抽屉、更多 tab 或写操作时再拆。
- 低权限账号负向验证和端内直登浏览器消费流程仍属于后续独立切片。

## 2026-06-05 卖家端免密直登浏览器消费检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：卖家端免密直登浏览器消费链路验收。本轮不复制买家、不改 SQL、不改后端接口、不改权限点。

已完成：

- 修改 `react-ui/src/pages/Portal/DirectLogin/index.tsx`，将直登页加载态 `Spin` 的 `tip` 改为 `description`。
- 使用浏览器生成并消费卖家端账号级免密票据，目标为 `sellerId=9`、`sellerNo=SAF030002`、`accountId=8`、`userName=1234`。
- 验证 `/seller/direct-login` 消费后落到 `/seller/portal`。
- 验证消费后只写入 seller 端 token，不写入 buyer 端 token，且不覆盖管理端 token。
- 验证 seller token 可访问 seller `getInfo` / `getRouters`，但调用 buyer `getInfo` 被业务拒绝。
- 验证票据 `ticketId=106` 消费后状态为 `USED`，`usedTime` 已写入，代入原因匹配。
- 验证完成后调用 `/api/seller/logout`，清理 seller 端浏览器会话。
- 新增执行记录：`docs/plans/2026-06-05-seller-direct-login-browser-consumption-record.md`。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Portal/DirectLogin/index.tsx src/components/PartnerManagement/PartnerAuditModal.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- Playwright / Chrome 生成卖家直登票据：HTTP `200`、业务 `code=200`，有效期 `30` 分钟，直登路径 `/seller/direct-login`。
- Playwright / Chrome 消费卖家直登票据：最终路径 `/seller/portal`。
- 端 token 隔离验证：`sellerTokenPresent=true`、`buyerTokenPresent=false`。
- seller 端接口验证：seller `getInfo` HTTP `200`、业务 `code=200`；seller `getRouters` HTTP `200`、业务 `code=200`。
- 跨端拒绝验证：使用 seller token 调 buyer `getInfo`，HTTP `200`、业务 `code=401`。
- Playwright console 检查：`Errors: 0`、`Warnings: 0`。
- 会话清理验证：`/api/seller/logout` HTTP `200`、业务 `code=200`；清理后 seller token 和 seller session 缓存均不存在。
- `cd E:\Urili-Ruoyi; git diff --check -- react-ui/src/pages/Portal/DirectLogin/index.tsx docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-seller-direct-login-browser-consumption-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`。

当前判断：

- 卖家端免密直登浏览器消费链路已作为标准模板验收通过：管理端生成一次性票据，卖家端消费并建立 seller 独立 token，跨端调用被拒绝，票据转为 `USED`。
- 买家端暂不复制；后续复制买家时只替换 terminal、路径、权限标识、service 和文案，并按同一浏览器验收清单跑一遍。
- 本轮通过运行时接口产生了正常票据、会话和日志数据；未执行人工 SQL。

## 2026-06-05 买家端免密直登浏览器消费检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，在卖家直登消费验收通过后，只按同一清单验收买家端免密直登浏览器消费链路。本轮不改 SQL、不改后端接口、不改权限点、不重新设计直登页。

已完成：

- 使用浏览器生成并消费买家端账号级免密票据，目标为 `buyerId=2`、`buyerNo=BAF030001`、`accountId=2`。
- 验证 `/buyer/direct-login` 消费后落到 `/buyer/portal`。
- 验证消费后只写入 buyer 端 token，不写入 seller 端 token，且不覆盖管理端 token。
- 验证 buyer token 可访问 buyer `getInfo` / `getRouters`，但调用 seller `getInfo` 被业务拒绝。
- 验证票据 `ticketId=107` 消费后状态为 `USED`，`usedTime` 已写入，代入原因匹配。
- 验证完成后调用 `/api/buyer/logout`，清理 buyer 端浏览器会话。
- 更新 `docs/architecture/reuse-ledger.md` 的直登入口与端内工作台模板规则。
- 新增执行记录：`docs/plans/2026-06-05-buyer-direct-login-browser-consumption-record.md`。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- Playwright / Chrome 生成买家直登票据：HTTP `200`、业务 `code=200`，有效期 `30` 分钟，直登路径 `/buyer/direct-login`。
- Playwright / Chrome 消费买家直登票据：最终路径 `/buyer/portal`。
- 端 token 隔离验证：`buyerTokenPresent=true`、`sellerTokenPresent=false`。
- buyer 端接口验证：buyer `getInfo` HTTP `200`、业务 `code=200`；buyer `getRouters` HTTP `200`、业务 `code=200`。
- 跨端拒绝验证：使用 buyer token 调 seller `getInfo`，HTTP `200`、业务 `code=401`。
- Playwright console 检查：`Errors: 0`、`Warnings: 0`。
- 会话清理验证：`/api/buyer/logout` HTTP `200`、业务 `code=200`；清理后 buyer token 和 buyer session 缓存均不存在。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Portal/DirectLogin/index.tsx src/pages/Buyer/index.tsx src/services/buyer/buyer.ts`：通过，Biome 输出 `Checked 2 files`；`src/services/buyer/buyer.ts` 按当前 Biome 配置被忽略，service 类型由 `tsc` 覆盖。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi; git diff --check -- docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-buyer-direct-login-browser-consumption-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- 买家端免密直登浏览器消费链路已按卖家模板验收通过：管理端生成一次性票据，买家端消费并建立 buyer 独立 token，跨端调用被拒绝，票据转为 `USED`。
- seller/buyer 两端直登消费模板现在都有真实浏览器验收记录。
- 本轮通过运行时接口产生了正常票据、会话和日志数据；未执行人工 SQL。

## 2026-06-05 管理端买家审计详情模板验收检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，在卖家审计详情模板已完成后，只做买家入口浏览器验收。本轮不改代码、不改后端接口、不改权限点、不执行 SQL。

已完成：

- 打开 `/partner/buyer`，通过买家管理工具栏“审计”进入 `买家审计 - 全部买家` 弹窗。
- 验收“登录日志”tab 展开详情字段：`登录地点`、`浏览器`、`操作系统`、`登录提示`。
- 验收“操作日志”tab 展开详情字段：`请求地址`、`操作IP`、`操作地点`、`方法名`、`异常信息`。
- 验收“免密票据”tab 展开详情字段：`目标端`、`签发人ID`、`使用IP`、`创建人`、`更新人`、`更新时间`、`代入原因`、`备注`。
- 使用稳定脚本依次切换三个 tab 并展开首行，字段检查结果全部为 `true`。
- 更新 `docs/architecture/reuse-ledger.md` 的 `PartnerAuditModal` 规则。
- 新增执行记录：`docs/plans/2026-06-05-admin-buyer-audit-detail-template-record.md`。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL/Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- 本轮未执行人工 SQL、DDL 或 DML；只读取远程运行库中的买家审计数据。
- Playwright / Chrome 验收 `/partner/buyer`：买家审计弹窗正常打开，三个 tab 详情字段可见。
- Playwright console 检查：`Errors: 0`、`Warnings: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/components/PartnerManagement/PartnerAuditModal.tsx src/pages/Buyer/index.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi; git diff --check -- docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-admin-buyer-audit-detail-template-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- 管理端买家审计详情已按卖家模板完成入口级浏览器验收。
- `PartnerAuditModal` 保持 seller/buyer 共用，没有复制第二套买家审计弹窗。
- 低权限账号下 direct-login / ticket 审计入口隐藏和后端拒绝仍需单独验证。

## 2026-06-05 管理端免密代入权限契约守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只补管理端免密代入和免密票据审计的权限契约守卫。本轮不改业务逻辑、不改 UI 布局、不执行 SQL、不连接远程 MySQL / Redis。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`。
- 锁定 seller/buyer 主体级免密代入接口必须使用 `seller:admin:directLogin` / `buyer:admin:directLogin`。
- 锁定 seller/buyer 账号级免密代入接口必须使用 `seller:admin:directLogin` / `buyer:admin:directLogin`。
- 锁定 seller/buyer 免密票据列表接口必须使用 `seller:admin:ticket:list` / `buyer:admin:ticket:list`。
- 校验免密代入接口必须有管理端 `@Log`，避免敏感代入动作没有后台操作日志。
- 校验 `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 必须包含上述四个按钮权限。
- 校验共享前端模板必须按 `directLogin` 和 `ticket:list` 权限显隐主体行入口、账号行入口、全局审计入口和免密票据 tab。
- 新增执行记录：`docs/plans/2026-06-05-admin-direct-login-permission-contract-record.md`。
- 更新 `docs/architecture/reuse-ledger.md` 的管理端权限契约守卫规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminDirectLoginPermissionContractTest test`：首次失败，原因是测试从 `RuoYi-Vue/ruoyi-system` 运行时未正确定位仓库根目录；已修正测试自身路径定位。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminDirectLoginPermissionContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`。

当前判断：

- 管理端免密代入和免密票据审计现在有专门静态契约守卫，后续不能把 `directLogin` 合并到 `query` / `edit`，也不能让缺少 `ticket:list` 的账号看到免密票据 tab。
- 该守卫覆盖 seller 标准模板和 buyer 同构复制结果；后续同类改动先以 seller 模板验证，再复制 buyer。
- 真实低权限管理端账号的运行时负向验收仍未执行，后续需要单独用低权限角色验证前端按钮隐藏和后端接口拒绝。

## 2026-06-05 管理端卖家低权限免密代入负向验收检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只做卖家侧低权限运行时负向验收。本轮不改业务代码、不改表结构，不手工执行 SQL；仅通过若依管理端 API 创建或更新测试角色和测试账号。

已完成：

- 创建或更新测试角色：`roleId=102`，`roleKey=codex_seller_audit_only`。
- 创建或更新测试用户：`userId=111`，`userName=codex_seller_lowperm`。
- 测试角色只授予 `seller:admin:list`、`seller:admin:query`、`seller:admin:loginLog:list`、`seller:admin:operLog:list`。
- 测试角色不授予 `seller:admin:directLogin` 和 `seller:admin:ticket:list`。
- 使用低权限测试用户验证卖家列表、登录日志和操作日志接口可访问。
- 使用低权限测试用户验证卖家免密代入接口和免密票据列表接口被后端拒绝。
- 使用 Playwright CLI 验证低权限账号前端可进入卖家管理，行内“更多”没有免密代入入口，审计弹窗没有免密票据 tab。
- 新增执行记录：`docs/plans/2026-06-05-admin-seller-low-permission-direct-login-negative-record.md`。
- 更新 `docs/architecture/reuse-ledger.md` 的低权限负向验收复用规则。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL / Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- `/getInfo` 返回权限数量 `4`，包含卖家列表、卖家查询、卖家登录日志和卖家操作日志权限，不包含 `seller:admin:directLogin` 和 `seller:admin:ticket:list`。
- `GET /seller/admin/sellers/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `GET /seller/admin/sellers/loginLogs/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `GET /seller/admin/sellers/operLogs/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `POST /seller/admin/sellers/{sellerId}/directLogin`：HTTP `200`，业务 `code=403`。
- `GET /seller/admin/sellers/directLoginTickets/list?pageNum=1&pageSize=1`：HTTP `200`，业务 `code=403`。
- Playwright CLI 验证 `/partner/seller`：表格行数 `3`；首行“更多”菜单只有 `审计`，没有 `登录卖家端` / `directLogin`。
- Playwright CLI 验证审计弹窗：只展示 `登录日志`、`操作日志`，没有 `免密票据` tab。
- Playwright CLI 验证 token：管理端 token 存在，`seller_access_token=false`，`buyer_access_token=false`。
- 截图已生成：`output/playwright/seller-lowperm-negative.png`，大小 `92058` bytes。

当前判断：

- 卖家低权限管理端负向验收模板已跑通：普通列表和普通审计可用，免密代入和免密票据审计在后端被拒绝，在前端不可见。
- Playwright console 有 2 条无关错误：`CategoryAttributeTemplate.css` 缺失，来源为商品属性组件热更新，不是本次卖家权限页面或接口触发。
- 低权限账号首次登录后动态菜单需要刷新后才稳定显示左侧菜单；刷新后可展开主体管理并进入卖家管理。本轮没有改该前端路由时序问题，后续应单独处理。
- 本轮只做卖家模板；买家低权限负向验收尚未复制。

## 2026-06-05 管理端买家低权限免密代入负向验收检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在卖家低权限负向验收通过后，只按同一模板复制买家侧验收。本轮不改业务代码、不改表结构，不手工执行 SQL；仅通过若依管理端 API 创建或更新测试角色和测试账号。

已完成：

- 创建或更新测试角色：`roleId=103`，`roleKey=codex_buyer_audit_only`。
- 创建或更新测试用户：`userId=112`，`userName=codex_buyer_lowperm`。
- 测试角色只授予 `buyer:admin:list`、`buyer:admin:query`、`buyer:admin:loginLog:list`、`buyer:admin:operLog:list`。
- 测试角色不授予 `buyer:admin:directLogin` 和 `buyer:admin:ticket:list`。
- 使用低权限测试用户验证买家列表、登录日志和操作日志接口可访问。
- 使用低权限测试用户验证买家免密代入接口和免密票据列表接口被后端拒绝。
- 使用 Playwright CLI 验证低权限账号前端可进入买家管理，行内“更多”没有免密代入入口，审计弹窗没有免密票据 tab。
- 新增执行记录：`docs/plans/2026-06-05-admin-buyer-low-permission-direct-login-negative-record.md`。
- 更新 `docs/architecture/reuse-ledger.md` 的两端低权限负向验收复用规则。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL / Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- `/getInfo` 返回权限数量 `4`，包含买家列表、买家查询、买家登录日志和买家操作日志权限，不包含 `buyer:admin:directLogin` 和 `buyer:admin:ticket:list`。
- `GET /buyer/admin/buyers/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `GET /buyer/admin/buyers/loginLogs/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `GET /buyer/admin/buyers/operLogs/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `POST /buyer/admin/buyers/{buyerId}/directLogin`：HTTP `200`，业务 `code=403`。
- `GET /buyer/admin/buyers/directLoginTickets/list?pageNum=1&pageSize=1`：HTTP `200`，业务 `code=403`。
- Playwright CLI 验证 `/partner/buyer`：表格行数 `1`；首行“更多”菜单只有 `审计`，没有 `登录买家端` / `directLogin`。
- Playwright CLI 验证审计弹窗：只展示 `登录日志`、`操作日志`，没有 `免密票据` tab。
- Playwright CLI 验证 token：管理端 token 存在，`seller_access_token=false`，`buyer_access_token=false`。
- Playwright console 检查：`Errors: 0`，`Warnings: 0`。
- 截图已生成：`output/playwright/buyer-lowperm-negative.png`，大小 `110909` bytes。

当前判断：

- 买家低权限管理端负向验收已按卖家模板复制通过：普通列表和普通审计可用，免密代入和免密票据审计在后端被拒绝，在前端不可见。
- seller/buyer 两端低权限免密代入负向验收现在均有运行时证据。
- 角色名使用 ASCII：`Codex Buyer Audit Only`；原因是当前运行库中文测试角色名显示存在转码冲突，继续使用中文名会与卖家测试角色名冲突。

## 2026-06-05 管理端首次登录动态菜单缓存检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理管理端首次登录后左侧动态菜单和动态路由需要刷新才稳定显示的问题。本轮不改业务字段、不改端内权限模型、不复制买家业务模板、不执行 SQL。

已完成：

- `react-ui/src/services/session.ts` 新增 `admin_remote_menu` 缓存读取和写入。
- `setRemoteMenu(...)` 同时维护运行时内存和 `sessionStorage.admin_remote_menu`。
- `react-ui/src/access.ts` 清理 admin token 时同步清理 `admin_remote_menu`。
- `react-ui/src/app.tsx` 新增 `clearAdminSession()`，统一处理 token 过期、无 token、`getInfo` 失败、`getRouters` 失败和登录重定向时的 admin session 清理。
- `patchRouteItems(...)` 改为按 path 更新已有 `routes` / `children`，避免动态菜单 patch 重复追加路由。
- 新增执行记录：`docs/plans/2026-06-05-admin-dynamic-menu-first-login-cache-record.md`。
- 更新 `docs/architecture/reuse-ledger.md` 的管理端远程菜单缓存和幂等 patch 复用规则。

验证结果：

- 数据源确认：后端激活 `druid`；MySQL / Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- 本轮未执行 DDL、DML 或人工 SQL。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/app.tsx src/access.ts src/services/session.ts`：通过，当前 Biome 配置实际检查 2 个文件。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：未通过，阻断点为无关的 `src/pages/Product/Attribute/components/CategoryAttributeTemplate.tsx` 类型错误，本轮未跨切片修复。
- Playwright CLI 低权限买家首次登录 `/partner/buyer`：通过；首次跳转后页面标题为 `买家管理 - Ant Design Pro`，表格行数 `1`，`admin_remote_menu` 存在，只有管理端 token 存在。
- Playwright CLI 低权限买家控制台：`Errors: 0`、`Warnings: 0`。
- Playwright CLI admin 首次登录 `/partner/seller`：通过；首次跳转后页面标题为 `卖家管理 - Ant Design Pro`，表格行数 `3`，左侧菜单和面包屑可见，`admin_remote_menu` 已写入。
- Playwright CLI admin 控制台：`Errors: 0`、`Warnings: 0`。
- 截图证据：`output/playwright/buyer-first-login-menu.png`、`output/playwright/admin-first-login-menu.png`。

当前判断：

- 管理端首次登录后动态菜单和动态路由现在不再依赖刷新恢复。
- 管理端菜单缓存只跟 admin token 同生命周期，admin token 清理时同步清理 `admin_remote_menu`。
- portal 路由仍排除在管理端 `getUserInfo()`、动态菜单加载和管理端登录态重定向之外。
- 商品属性页面的 TypeScript 错误属于后续独立切片，不混入本轮动态菜单修复。

## 2026-06-05 管理端免密票据端类型过滤契约检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，补强管理端免密票据审计列表的端类型过滤契约。本轮不执行 SQL、不改接口业务逻辑、不改页面交互。

已完成：

- 使用 6 个只读子 agent 并行审计后端能力、管理端 UI 对齐、portal 安全边界、测试守卫、SQL/seed 和前端类型门禁；子 agent 均已关闭。
- 更新 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`。
- 测试新增 seller/buyer service 层 terminal 强制过滤检查：`selectSellerDirectLoginTicketList(...)` 必须设置 `query.setTerminal("seller")`，`selectBuyerDirectLoginTicketList(...)` 必须设置 `query.setTerminal("buyer")`。
- 测试新增共享 mapper XML terminal 条件检查：`PortalDirectLoginTicketMapper.xml` 的列表查询必须包含 `and terminal = #{terminal}`。
- 修复 `react-ui/src/pages/Product/categoryTree.ts` 的 `toCategoryOption(...)` 返回类型，让前端 `npm run tsc` 恢复通过。
- 新增执行记录：`docs/plans/2026-06-05-admin-direct-login-ticket-terminal-contract-record.md`。
- 更新 `docs/architecture/reuse-ledger.md` 的 `PortalDirectLoginTicketMapper` 复用规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminDirectLoginPermissionContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Product/categoryTree.ts`：通过。

当前判断：

- seller/buyer 管理端免密票据审计列表现在有静态契约守卫：权限点独立、前端 tab 权限显隐、service 层 terminal 强制过滤和 mapper terminal 条件均被覆盖。
- 只读审计确认当前 `react-ui` 管理端 PartnerManagement 模板 seller/buyer 已对齐，未发现 seller 已有但 buyer 未复制。
- 只读审计未发现受保护 seller/buyer portal 接口信任前端 `sellerId` / `buyerId` / `accountId` 的证据。
- SQL/seed 只读审计发现端内权限 seed 尚未并入综合初始化 seed；这属于后续 SQL 初始化一致性切片，本轮不混入。

## 2026-06-05 端内权限综合 Seed 对齐检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理 SQL 初始化一致性问题。本轮不执行远端 SQL、不改 UI、不改接口业务逻辑。

已完成：

- 更新 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，补入 active 卖家、买家的默认 `Owner` 端内角色初始化。
- 补入卖家端当前最小端内门户权限：`seller:account:list`、`seller:dept:list`、`seller:role:list`、`seller:product:category:list`、`seller:product:schema:query`。
- 补入买家端当前最小端内门户权限：`buyer:account:list`、`buyer:dept:list`、`buyer:role:list`、`buyer:product:category:list`、`buyer:product:schema:query`。
- 补入 `OWNER` 账号到默认 `owner` 角色的绑定，以及 active 端内角色到上述端内权限菜单的绑定。
- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java`，守住综合 seed 的端内门户权限和绑定结构。
- 新增执行记录：`docs/plans/2026-06-05-terminal-comprehensive-seed-permission-alignment-record.md`。
- 更新 `docs/architecture/reuse-ledger.md` 的综合 seed 复用规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalSeedPermissionContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\sql\seller_buyer_management_seed.sql docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md react-ui\src\pages\Product\categoryTree.ts`：通过；Git 仅提示 LF/CRLF 转换警告。
- 新增未跟踪文件空白检查：无尾随空白输出。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，结果为 `Already up to date`。
- UTF-8 读取 `seller_buyer_management_seed.sql`：新增中文备注显示正常；PowerShell 默认编码下的乱码为控制台显示问题。

当前判断：

- 综合 seed 现在覆盖端内门户当前最小权限和默认角色绑定；新环境只执行综合 seed 时，不再遗漏这些端内权限。
- 历史增量 SQL 继续保留，作为已运行环境按批次补丁执行的依据。
- 本轮未触碰远端数据库；运行库是否应用这些权限，后续需要执行 SQL 时再单独确认数据源、执行记录和回滚方式。
- `seller_buyer_management_seed.sql` 已超过 500 行，但职责仍是卖家/买家主体和端内基础控制面综合初始化；本轮只在同一初始化边界内补权限和绑定，不单独拆分。

## 2026-06-05 端内 Portal 守卫自动发现检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只增强端内 portal 静态守卫。本轮不改业务代码、不改前端、不执行远端 SQL。

已完成：

- 使用 6 个只读子 agent 并行审计管理端卖家 UI、卖家后端控制面、卖家 portal 范围、权限 seed、三端前端耦合和测试缺口；子 agent 均已关闭。
- `TerminalRouteOwnershipTest` 改为自动发现 seller/buyer 模块 controller 目录下的受保护 `*Portal*Controller.java`。
- `SellerPortalAuthController` / `BuyerPortalAuthController` 继续作为登录和免密消费认证入口例外，不纳入受保护 handler 模板检查。
- 受保护 portal handler 继续统一校验方法级 `@Anonymous`、`@PortalPreAuthorize`、`@PortalLog` 和 `PortalSessionContext.requireSession(...)`。
- `TerminalSeedPermissionContractTest` 新增源码扫描，自动提取 seller/buyer 源码中的 `@PortalPreAuthorize(hasPermi = "...")`，并要求这些端内权限存在于 `seller_buyer_management_seed.sql`。
- 新增执行记录：`docs/plans/2026-06-05-terminal-portal-guard-auto-discovery-record.md`。
- 更新 `docs/architecture/reuse-ledger.md` 的端内 Controller 和综合 seed 守卫复用规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过；Git 仅提示 LF/CRLF 转换警告。
- 新增/未跟踪测试和记录文件空白检查：无尾随空白输出。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 5 changed files`。

当前判断：

- 后续新增 seller/buyer 受保护 portal controller 时，只要文件名符合 `*Portal*Controller.java` 且不属于 `*PortalAuthController.java`，会自动进入模板守卫。
- 后续新增端内 `@PortalPreAuthorize(hasPermi=...)` 权限时，如果只改 controller 不更新综合 seed，`TerminalSeedPermissionContractTest` 会失败。
- 本轮未触碰远端运行库；运行库是否应用最新 seed 仍需在需要执行 SQL 时单独确认数据源和执行记录。

## 2026-06-05 管理端卖家账号权限粒度硬化检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类问题：卖家管理端端账号维护权限过粗。本轮不复制买家、不新增表、不执行远程 SQL、不改变免密代入和强制踢出权限模型。

已完成：

- 使用 3 个只读子 agent 并行核对后端权限、前端共享组件、seed/test 契约；子 agent 均已关闭。
- `AdminSellerController` 中卖家端账号维护接口切换为独立账号域权限：
  - 账号列表：`seller:admin:account:list`
  - 账号新增：`seller:admin:account:add`
  - 账号编辑：`seller:admin:account:edit`
  - 账号密码重置：`seller:admin:account:resetPwd`
  - 账号角色查询：`seller:admin:account:role:query`
  - 账号角色分配：`seller:admin:account:role:edit`
- 卖家主体详情保留 `seller:admin:query`；主体主账号重置仍保留 `seller:admin:resetPwd`。
- 主体级/账号级免密代入继续使用 `seller:admin:directLogin`；主体级/账号级会话和强制踢出继续使用 `seller:admin:forceLogout`。
- `seller_buyer_management_seed.sql` 补入 6 个卖家管理端账号域 `sys_menu` 权限。
- `PartnerModuleConfig` 增加可选 `accountPermissions` 配置；本卖家切片先启用 `seller:admin:account:*`，买家已在后续复制检查点启用 `buyer:admin:account:*`。
- `SellerAdminPermissionContractTest` 增加账号域权限契约，固定主体详情与账号列表不能再错位。
- 新增执行记录：`docs/plans/2026-06-05-admin-seller-account-permission-hardening-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记卖家账号域权限和 `PartnerModuleConfig.accountPermissions` 复用规则。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest" test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am -DskipTests compile`：通过，seller 及依赖模块编译成功。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src\components\PartnerManagement\PartnerManagementPage.tsx src\components\PartnerManagement\PartnerAccountModal.tsx src\pages\Seller\index.tsx`：通过。
- `git diff --check -- RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerController.java RuoYi-Vue\sql\seller_buyer_management_seed.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SellerAdminPermissionContractTest.java react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx react-ui\src\components\PartnerManagement\PartnerAccountModal.tsx react-ui\src\pages\Seller\index.tsx docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 工作区换行提示。
- 新增记录文件尾随空白检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 10 changed files`。

补充说明：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -DskipTests compile` 未通过，原因是当前工作区已有 seller 依赖的 `ruoyi-system` / `product` 新增类，需要 `-am` 一起构建依赖模块；带 `-am` 后已通过。
- 远程运行库执行已在后续独立切片完成，记录见 `docs/plans/2026-06-05-admin-seller-account-permission-db-execution-record.md`。

当前判断：

- 卖家管理端账号维护已经形成一套更细的权限样板，不再借用主体新增、主体编辑、主体查询或端内角色维护权限。
- 买家账号域权限尚未复制；下一步应等卖家模板验收后，只替换为 `buyer:admin:account:*`、buyer controller、buyer seed 和 buyer 前端配置。
- 低权限账号运行时负向验证尚未做；当前完成的是源码契约、编译和前端类型层验证。

## 2026-06-05 管理端卖家账号权限远程库执行检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家账号权限粒度硬化之后，只处理一类问题：让当前远程运行库补齐卖家管理端账号域 `sys_menu` 权限。本轮不复制买家、不新增表、不改账号/角色/部门/日志/会话数据。

已完成：

- 读取激活配置：后端 `spring.profiles.active=druid`，MySQL 由 `application-druid.yml` 中的 `RUOYI_DB_*` 变量提供。
- 确认 `.env.local` 中存在 `RUOYI_DB_*`、`RUOYI_REDIS_*` 和 `RUOYI_TOKEN_SECRET` 键；未在记录或回复中输出连接串、账号、密码、Redis 地址或 token secret。
- 执行前确认运行库中 6 个目标权限不存在：`matchedRows=0`。
- 执行前发现 `menu_id=2260/2261` 已被客户渠道菜单占用，因此把综合 seed 和增量 SQL 的新增权限 ID 调整为 `2310-2315`。
- 确认运行库 `menu_id between 2310 and 2330` 查询结果为 `rows=0`。
- 新增增量 SQL：`RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql`。
- 使用本机 `.env.local` 的 `RUOYI_DB_*` 通过 MySQL JDBC 执行该 SQL，返回 `affected=6`。
- 新增执行记录：`docs/plans/2026-06-05-admin-seller-account-permission-db-execution-record.md`。

执行后核验：

- 6 个权限已写入运行库：
  - `2310|2011|125|卖家账号列表|seller:admin:account:list|0`
  - `2311|2011|130|卖家账号新增|seller:admin:account:add|0`
  - `2312|2011|135|卖家账号修改|seller:admin:account:edit|0`
  - `2313|2011|140|卖家账号重置密码|seller:admin:account:resetPwd|0`
  - `2314|2011|145|卖家账号角色查询|seller:admin:account:role:query|0`
  - `2315|2011|150|卖家账号角色分配|seller:admin:account:role:edit|0`
- 旧冲突 ID 未被覆盖：
  - `2260|客户渠道查询|channel:customer:query`
  - `2261|客户渠道新增|channel:customer:add`

当前判断：

- 当前远程运行库已经具备卖家管理端账号域权限菜单。
- 综合 seed 已同步改为 `2310-2315`，避免新环境或后续执行覆盖客户渠道菜单。
- 本轮只补卖家；买家账号域权限仍等待卖家验收后复制。

## 2026-06-05 管理端买家账号权限粒度复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家账号权限粒度模板和远程库执行之后，只处理一类问题：把已验收的卖家账号域权限模板复制到买家管理端。本轮不新增表，不重设 UI，不改变免密代入、强制踢出、主体主账号重置密码或端内权限模型。

已完成：

- `AdminBuyerController` 中买家端账号维护接口切换为独立账号域权限：
  - 账号列表：`buyer:admin:account:list`
  - 账号新增：`buyer:admin:account:add`
  - 账号编辑：`buyer:admin:account:edit`
  - 账号密码重置：`buyer:admin:account:resetPwd`
  - 账号角色查询：`buyer:admin:account:role:query`
  - 账号角色分配：`buyer:admin:account:role:edit`
- 买家主体详情保留 `buyer:admin:query`；主体主账号重置仍保留 `buyer:admin:resetPwd`。
- 主体级/账号级免密代入继续使用 `buyer:admin:directLogin`；主体级/账号级会话和强制踢出继续使用 `buyer:admin:forceLogout`。
- `Buyer/index.tsx` 配置 `PartnerModuleConfig.accountPermissions`，启用 `buyer:admin:account:*`。
- `seller_buyer_management_seed.sql` 补入 6 个买家管理端账号域 `sys_menu` 权限，ID 使用 `2316-2321`。
- 新增增量 SQL：`RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql`。
- 使用本机 `.env.local` 的 `RUOYI_DB_*` 通过 MySQL JDBC 执行该 SQL，返回 `affected=6`。
- 新增执行记录：
  - `docs/plans/2026-06-05-admin-buyer-account-permission-hardening-record.md`
  - `docs/plans/2026-06-05-admin-buyer-account-permission-db-execution-record.md`
- 更新 `docs/architecture/reuse-ledger.md`，登记卖家/买家账号域权限均已按同一标准模板落地。

执行后核验：

- 6 个买家账号域权限已写入运行库：
  - `2316|2012|125|买家账号列表|buyer:admin:account:list|0`
  - `2317|2012|130|买家账号新增|buyer:admin:account:add|0`
  - `2318|2012|135|买家账号修改|buyer:admin:account:edit|0`
  - `2319|2012|140|买家账号重置密码|buyer:admin:account:resetPwd|0`
  - `2320|2012|145|买家账号角色查询|buyer:admin:account:role:query|0`
  - `2321|2012|150|买家账号角色分配|buyer:admin:account:role:edit|0`
- 权限行总数核验：`permissionRows=6`。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest" test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest" test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src\pages\Buyer\index.tsx`：通过。
- `git diff --check -- ...` 本轮相关文件：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- 卖家账号域权限模板已经完成复制，买家不再复用主体查询、新增、编辑或端内角色维护权限来控制账号维护。
- 管理端账号域权限的源码契约、前端权限配置、综合 seed、远程运行库菜单层均已对齐。
- 低权限账号运行时负向验证和前端按钮显隐契约已在后续检查点补充；真实低权限账号浏览器点击和接口 403 仍可作为后续验收切片补充。

## 2026-06-05 管理端账号域权限运行时负向验证检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家/买家账号域权限菜单补齐之后，只处理一类问题：验证低权限管理端账号不能用主体权限或端内角色权限绕过账号域权限。本轮不执行远程库 DDL/DML，不新增菜单，不改变接口路径或业务服务实现。

已完成：

- 新增 `RuoYi-Vue/ruoyi-framework/src/test/java/com/ruoyi/framework/web/service/PermissionServiceAccountPermissionTest.java`。
- 为 `ruoyi-framework` 增加 `junit` test scope 依赖，仅用于框架模块单元测试。
- `PermissionServiceAccountPermissionTest` 直接覆盖若依 `@ss.hasPermi(...)` 使用的 `PermissionService`：
  - 只有 `seller:admin:query/add/edit/resetPwd/role:*` 时，不能通过 `seller:admin:account:*`。
  - 只有 `buyer:admin:query/add/edit/resetPwd/role:*` 时，不能通过 `buyer:admin:account:*`。
  - 精确 `seller:admin:account:*` 只允许卖家账号域动作，不允许买家账号域动作。
  - 超级权限 `*:*:*` 仍可通过卖家/买家账号域权限。
- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminAccountPermissionUiContractTest.java`。
- `AdminAccountPermissionUiContractTest` 固定前端显隐契约：
  - `Seller/index.tsx` 必须配置 `seller:admin:account:*`。
  - `Buyer/index.tsx` 必须配置 `buyer:admin:account:*`。
  - `PartnerManagementPage` 的主体行“账号”入口必须受 `accountPermissions.list` 控制。
  - `PartnerAccountModal` 的新增、编辑、重置密码和账号角色分配必须受 `accountPermissions` 控制。
- 新增执行记录：`docs/plans/2026-06-05-admin-account-permission-negative-runtime-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记运行时负向和 UI 显隐契约守卫。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminAccountPermissionUiContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework test`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src\components\PartnerManagement\PartnerManagementPage.tsx src\components\PartnerManagement\PartnerAccountModal.tsx src\pages\Seller\index.tsx src\pages\Buyer\index.tsx`：通过。
- `git diff --check -- ...` 本轮相关文件：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，首次输出 `Synced 3 changed files`；记录回填后最终复跑输出 `Already up to date`。

当前判断：

- 账号域权限已经不只是静态注解正确，运行时低权限判断也有测试守住。
- 前端账号入口和账号操作按钮的显隐契约已有测试守住。
- 真实低权限管理端账号浏览器点击和接口 403 验收已在后续检查点完成；后续不再把该项列为账号域权限的未验证项。

## 2026-06-05 管理端账号域真实低权限账号验收检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在账号域权限运行时负向单元测试之后，只处理一类问题：用真实管理端低权限账号验证账号域接口和前端入口不能被主体查询权限绕过。本轮不新增表，不修改业务主体、端内账号、端内角色、端内菜单、日志或会话数据；临时管理端测试账号和角色在验收后已清理。

已完成：

- 读取激活配置：后端 `spring.profiles.active=druid`；MySQL 和 Redis 均来自本机 `.env.local` 提供的 `RUOYI_*` 运行变量，记录中未输出连接串、账号、密码、Redis 地址或 token secret。
- 执行前确认验证码开关为关闭状态，本轮未改验证码配置。
- 创建临时测试角色 `codex_account_negative_both`，只绑定主体管理、卖家管理、买家管理、卖家查询和买家查询 5 个菜单/权限。
- 创建临时测试账号 `codex_limited`；原计划用户名 `codex_account_limited` 超过若依 20 字符上限，实际改为 `codex_limited`。
- 使用 `codex_limited` 登录后，`/getInfo` 只返回 `seller:admin:list`、`seller:admin:query`、`buyer:admin:list`、`buyer:admin:query`，不包含任何 `*:admin:account:*`。
- 首次验收发现 8080 当时运行的是旧 jar：账号域接口没有在鉴权层返回 403，而是进入业务 service 后返回“卖家/买家不存在”。
- 已执行 `mvn -DskipTests install` 重新打包，并通过 `.\start-backend-local.ps1 -Restart` 重启后端，新 jar 生效后重新验收。
- 使用低权限 token 验证接口：
  - `GET /seller/admin/sellers/list?pageNum=1&pageSize=1`：`code=200`。
  - `GET /seller/admin/sellers/1/accounts`：`code=403`。
  - `GET /buyer/admin/buyers/list?pageNum=1&pageSize=1`：`code=200`。
  - `GET /buyer/admin/buyers/1/accounts`：`code=403`。
  - `GET /system/user/list?pageNum=1&pageSize=1`：`code=403`，作为系统用户权限对照。
- 使用 Playwright CLI 以 `codex_limited` 登录并访问 `/partner/seller`、`/partner/buyer`：
  - 卖家管理数据行未显示“账号”入口，操作列文本为 `操作|||`。
  - 买家管理数据行未显示“账号”入口，操作列文本为 `操作|`。
- 已强制踢出 `codex_limited` 在线 token，返回 `forcedLogoutTokens=4`。
- 已删除临时 `sys_user_role`、`sys_role_menu`、`sys_user`、`sys_role` 数据；清理后测试用户剩余 `0`，测试角色剩余 `0`。
- 新增执行记录：`docs/plans/2026-06-05-admin-account-low-permission-runtime-db-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记账号域真实低权限验收模板。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests install`：通过。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1 -Restart`：通过，8080 使用新 jar 重新监听。
- 低权限真实账号接口验收：主体列表 `code=200`，卖家/买家账号域列表 `code=403`。
- Playwright CLI 浏览器验收：卖家/买家管理数据行均不显示“账号”入口。
- 临时账号、临时角色、在线 token 清理：通过。
- `git diff --check -- docs\plans\2026-06-05-admin-account-low-permission-runtime-db-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- 管理端账号域权限已经完成静态契约、运行时单元测试、真实低权限接口 403 和浏览器按钮隐藏四层验证。
- 本轮发现并修正了旧 jar 造成的运行时误判；后续后端注解或权限改动验收前，必须确认 8080 使用的是新打包 jar。
- 本轮是账号域权限切片，未扩大到免密代入、审计 tab、端内权限或三端前端物理拆分。

## 2026-06-05 端内 Portal 客户端身份参数守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，只处理一类问题：受保护 seller/buyer portal handler 不能在方法签名中接收前端传入的端身份范围参数。本轮不新增业务接口、不执行远程数据库 DDL/DML、不改前端 token 持久化、不开始三端前端物理拆分。

已完成：

- 更新 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`。
- 在原有“受保护 portal handler 必须方法级 `@Anonymous` + `@PortalPreAuthorize` + `@PortalLog` + `PortalSessionContext.requireSession(...)`”守卫基础上，新增客户端身份参数守卫。
- seller 模板新增 `sellerPortalHandlersMustNotAcceptClientIdentityScope`。
- buyer 按同构规则新增 `buyerPortalHandlersMustNotAcceptClientIdentityScope`。
- 新守卫禁止受保护 portal handler 方法声明出现：
  - `sellerId`
  - `buyerId`
  - `subjectId`
  - `accountId`
  - `terminal`
- 继续允许非身份业务参数，例如商品 Schema 的 `categoryId`。
- 新增执行记录：`docs/plans/2026-06-05-terminal-portal-client-identity-guard-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内 handler 身份参数守卫和后续 session-scoped service 规则。

只读并行审计结论：

- 后端子 agent 未发现当前受保护 portal controller 直接信任前端传入 `sellerId` / `buyerId` / `accountId` / `subjectId` / `terminal` 作为数据范围边界。
- 前端子 agent 未发现 seller/buyer portal 端接口复用管理端 `access_token`，也未发现 portal 页面通过传 `sellerId` / `buyerId` / `accountId` 决定端内数据范围。
- 目标追踪子 agent 建议继续优先推进“端内真实业务接口范围控制模板”，前端物理三端拆分仍不建议马上大拆。
- 子 agent 均已关闭。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 39, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\TerminalRouteOwnershipTest.java docs\plans\2026-06-05-terminal-portal-client-identity-guard-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，本轮代码变更后的首次同步输出 `Synced 1 changed files`；记录回填后最终复跑输出 `Already up to date`。

当前判断：

- 端内受保护 handler 现在已有两层范围守卫：必须从 `PortalSessionContext` 派生当前会话，并且不能在 handler 签名中接收客户端身份范围参数。
- 本轮只守 handler 层模板，不替代具体业务 service 的数据范围单元测试或真实接口烟测。
- 后端只读审计指出当前账号登录日志/操作日志列表仍依赖 controller 覆盖查询 DTO 的 `subjectId` / `accountId`；当前不是可利用问题，但下一刀应先按卖家模板把日志列表收敛为 session-scoped service，再复制买家。
- 前端只读审计建议的 `persistPortalLogin(expectedTerminal, result)` 加固属于后续单独前端边界切片，本轮未混入。

## 2026-06-05 卖家端当前账号日志 session-scoped Service 模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，只处理一类问题：卖家端当前账号登录日志/操作日志列表的数据范围必须在 Service 内由 `PortalLoginSession` 强制收敛。本轮只做卖家模板，不复制买家；不改 SQL、不改前端、不改接口路径、不改权限模型、不执行远程数据库 DDL/DML。

已完成：

- 更新 `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerService.java`，新增：
  - `selectSellerOwnLoginLogList(PortalLoginSession, PortalLoginLog)`
  - `selectSellerOwnOperLogList(PortalLoginSession, PortalOperLog)`
- 更新 `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`：
  - Service 内校验当前卖家端账号仍属于当前卖家主体。
  - Service 内创建干净查询对象，并强制写入 session 中的 `subjectId` / `accountId`。
  - 登录日志只保留 `userName`、`ipaddr`、`status`、`params.beginTime`、`params.endTime` 筛选。
  - 操作日志只保留 `title`、`operName`、`status`、`params.beginTime`、`params.endTime` 筛选。
- 更新 `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`：
  - Controller 只负责 `PortalSessionContext.requireSession("seller")`、分页和返回表格。
  - Controller 不再直接覆盖日志查询 DTO 的 `subjectId` / `accountId`。
- 更新 `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`：
  - 验证前端伪造 `subjectId` / `accountId` 会被 session 覆盖。
  - 验证安全筛选字段保留。
  - 验证额外 `params` 不会被带入 mapper 查询对象。
- 新增执行记录：`docs/plans/2026-06-05-seller-own-log-session-scoped-service-template-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内当前账号日志 session-scoped Service 模板。

子 agent 并行审计结论：

- 后端链路审计确认当前实现可用，但 service 原本只是透传 mapper；本轮已把范围收敛下沉到 seller service。
- 测试风格审计确认应在 `SellerServiceImplTest` 中沿用 JUnit4 + JDK Proxy mapper 方式测试。
- buyer 对比审计确认 buyer 与 seller 当前同构，卖家验收通过后可只替换 terminal、service、controller、mapper、测试名和文案复制。
- 筛选字段审计确认本轮只保留当前 mapper 明确支持的内容筛选，不新增 `operUrl`、`operIp`、`businessType` 等筛选。
- 子 agent 均已关闭。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -Dtest=SellerServiceImplTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller test`：通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\SellerPortalController.java RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\ISellerService.java RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java RuoYi-Vue\seller\src\test\java\com\ruoyi\seller\service\impl\SellerServiceImplTest.java docs\plans\2026-06-05-seller-own-log-session-scoped-service-template-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，本轮代码变更后的首次同步输出 `Synced 4 changed files`；记录回填后最终复跑输出 `Already up to date`。

当前判断：

- 卖家端当前账号日志列表已经形成标准模板：Controller 负责入口和分页，Service 负责 session-scoped 数据范围。
- 管理端全量审计接口仍继续走 `selectSellerLoginLogList` / `selectSellerOperLogList`，没有被端内 own-log 模板影响。
- 买家端本轮未复制；下一刀应在卖家验收通过后按同构规则复制买家，不重新设计。

## 2026-06-05 端内前端登录持久化 terminal 守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，只处理一类问题：前端端内登录 token 持久化必须校验期望 terminal，并且 portal 请求不能回退使用管理端 token。本轮不复制买家后端日志模板，不改后端接口、SQL、菜单或权限，不改管理端登录 token 存储，不开始 `seller-ui` / `buyer-ui` 物理拆分。

已完成：

- 更新 `react-ui/src/pages/Portal/terminal.ts`：
  - `persistPortalLogin(result, expectedTerminal)` 必须接收期望端类型。
  - 如果响应缺少 token 或响应 `terminal` 与当前端不一致，清理相关端内 token 并返回 `false`。
  - 只有校验通过时才写入当前端 `seller_*` 或 `buyer_*` token key。
- 更新 `react-ui/src/pages/Portal/DirectLogin/index.tsx`：
  - 免密登录成功后统一调用 `persistPortalLogin(response.data, terminal)`。
  - 页面不再直接承担端 token 写入判断。
- 新增 `react-ui/scripts/check-portal-token-isolation.mjs`：
  - 禁止 `src/pages/Portal/**` 和 `src/services/portal/**` 直接使用管理端 `getAccessToken`、`setSessionToken`、`clearSessionToken`。
  - 禁止 portal 目录出现裸 `access_token` / `portal_login_token`。
  - 校验 `src/services/portal/session.ts` 每个 portal request 都显式 `isToken:false`。
- 更新 `react-ui/package.json`：
  - 新增 `guard:portal-token`。
  - 将 `guard:portal-token` 接入 `npm run lint` 前置步骤。
- 新增执行记录：`docs/plans/2026-06-05-portal-token-persist-terminal-guard-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内前端 token 持久化和静态守卫规则。

子 agent 并行审计结论：

- 前端登录持久化审计确认 `persistPortalLogin` 原本只读取响应 `terminal` 写 token，本轮已改为由调用方传入期望 terminal 并强制校验。
- 前端 token 隔离审计确认当前 portal 代码没有直接调用管理端 `getAccessToken` / `setSessionToken` / `clearSessionToken`。
- 前端 token 隔离审计确认当前 portal 请求通过 `getTerminalAccessToken(terminal)` 构造端内 Authorization，并且请求配置都带 `isToken:false`。
- 2 个子 agent 均已关闭。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint scripts\check-portal-token-isolation.mjs src\pages\Portal\terminal.ts src\pages\Portal\DirectLogin\index.tsx package.json`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run lint`：未通过；新增 `guard:portal-token` 已通过，失败发生在既有 Biome 全仓问题，例如 `src/components/DictTag/index.tsx`、`src/components/IconSelector/style.module.css`、`src/components/IconSelector/themeIcons.tsx` 和 `src/pages/Monitor/Druid/index.tsx`，不属于本切片引入。
- `git diff --check -- react-ui\src\pages\Portal\terminal.ts react-ui\src\pages\Portal\DirectLogin\index.tsx react-ui\scripts\check-portal-token-isolation.mjs react-ui\package.json docs\architecture\reuse-ledger.md docs\plans\2026-06-05-portal-token-persist-terminal-guard-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，本轮代码变更后的首次同步输出 `Synced 3 changed files`；记录回填后最终复跑输出 `Already up to date`。

当前判断：

- 端内前端 token 持久化已具备 terminal 校验防线。
- 新增端内 portal 前端请求时，如果忘记 `isToken:false` 或误用管理端 token API，会被 `guard:portal-token` 捕获。
- 本轮没有越过“先做卖家模板、验收通过后再复制买家”的边界；买家后端日志模板仍等待卖家模板验收通过后再复制。

## 2026-06-05 买家端当前账号日志 session-scoped Service 复制检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家端当前账号日志 session-scoped Service 模板验收之后，只处理一类问题：把已验证的卖家端当前账号登录日志/操作日志 Service 范围收敛模板按同构规则复制到买家端。本轮不改 SQL、不改前端、不改接口路径、不改权限模型、不执行远程数据库 DDL/DML、不重新设计筛选字段或端内日志页面。

已完成：

- 更新 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java`，新增：
  - `selectBuyerOwnLoginLogList(PortalLoginSession, PortalLoginLog)`
  - `selectBuyerOwnOperLogList(PortalLoginSession, PortalOperLog)`
- 更新 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`：
  - 新增 `assertBuyerSessionAccount(PortalLoginSession)`，校验当前买家端账号仍属于当前买家主体。
  - 新增 `buildBuyerOwnLoginLogQuery(...)` 和 `buildBuyerOwnOperLogQuery(...)`，创建干净查询对象并强制写入 session 中的 `subjectId` / `accountId`。
  - 新增 `copyTimeRangeParams(...)`，只复制 `beginTime` / `endTime`。
  - `selectBuyerOwnSessionList(...)` 复用 `assertBuyerSessionAccount(...)`。
- 更新 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`：
  - Controller 只负责 `PortalSessionContext.requireSession("buyer")`、分页和返回表格。
  - Controller 不再直接覆盖日志查询 DTO 的 `subjectId` / `accountId`。
- 更新 `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`：
  - 验证前端伪造 `subjectId` / `accountId` 会被 session 覆盖。
  - 验证安全筛选字段保留。
  - 验证额外 `params` 不会被带入 mapper 查询对象。
- 新增执行记录：`docs/plans/2026-06-05-buyer-own-log-session-scoped-service-template-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记买家端 session-scoped Service 模板。

子 agent 并行审计结论：

- 6 个只读子 agent 已完成并关闭。
- buyer/seller 两端 Mapper 支持的日志筛选字段一致：
  - 登录日志：`userName`、`ipaddr`、`status`、`params.beginTime`、`params.endTime`。
  - 操作日志：`title`、`operName`、`status`、`params.beginTime`、`params.endTime`。
- buyer 可复用 `selectBuyerAccountById(...)` 做主体和账号归属校验；本轮已抽出无副作用的 `assertBuyerSessionAccount(...)`。
- 文档口径采用追加检查点，不回改早期历史检查点；顶部当前状态和复用台账改为双端已完成。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -Dtest=BuyerServiceImplTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer test`：通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\controller\BuyerPortalController.java RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\IBuyerService.java RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java RuoYi-Vue\buyer\src\test\java\com\ruoyi\buyer\service\impl\BuyerServiceImplTest.java docs\plans\2026-06-05-buyer-own-log-session-scoped-service-template-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，本轮代码变更后的首次同步输出 `Synced 4 changed files`；记录回填后最终复跑输出 `Already up to date`。

当前判断：

- 买家端当前账号日志列表已经按卖家标准模板完成同构复制：Controller 负责入口和分页，Service 负责 session-scoped 数据范围。
- seller/buyer 双端当前账号日志接口现在均不再依赖 Controller 覆盖 DTO 作为数据范围安全边界。
- 管理端全量审计接口仍继续走 `selectBuyerLoginLogList` / `selectBuyerOperLogList`，没有被端内 own-log 模板影响。

## 2026-06-05 前端 portal 请求身份范围参数守卫检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，只处理一类问题：前端 portal 页面和 service 不得把客户端身份范围字段作为请求参数发送给 seller/buyer 端接口。本轮不新增 SQL，不执行远程数据库 DDL/DML，不改变后端权限模型，不启动 `seller-ui` / `buyer-ui` 物理拆分，也不替代后端 `PortalSessionContext` 和 session-scoped Service 的数据范围收敛。

已完成：

- 核对 `react-ui/src/services/portal/session.ts`：当前日志和会话查询已经统一通过 `sanitizePortalQueryParams(params)` 清洗参数，过滤 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId` 和 `terminal`。
- 扩展 `react-ui/scripts/check-portal-token-isolation.mjs`：禁止 `src/pages/Portal/**` 和 `src/services/portal/**` 中出现 `sellerId:`、`buyerId:`、`subjectId:`、`accountId:`、`sellerAccountId:`、`buyerAccountId:` 这类身份范围对象键。
- 保留已有守卫：portal 页面不得直接调用 `request(...)`，不得硬编码 `/api/seller` / `/api/buyer`，portal 请求必须显式 `isToken:false`，日志和会话查询必须使用 `sanitizePortalQueryParams(params)`。
- 更新 `docs/architecture/reuse-ledger.md`，登记前端 portal 请求身份范围参数守卫规则。
- 新增执行记录：`docs/plans/2026-06-05-portal-request-scope-param-guard-record.md`。

子 agent 并行审计结论：

- 6 个只读子 agent 已完成审计；关闭调用时工具侧已无可关闭句柄。
- 当前 `Portal/Home` 中 `row.accountId` 只作为表格行 key 使用，不属于请求身份范围参数。
- 当前 portal service 的请求出口集中在 `react-ui/src/services/portal/session.ts`；本轮不重新设计页面和 service，只加强静态守卫。
- `categoryId` 属于商品 schema 业务路径参数，允许保留。
- 验证建议采用 `npm run guard:portal-token`、定向 `biome lint` 和 `npm run tsc`。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint scripts\check-portal-token-isolation.mjs src\services\portal\session.ts src\pages\Portal\Home\index.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `git diff --check -- react-ui\scripts\check-portal-token-isolation.mjs react-ui\src\services\portal\session.ts docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-05-portal-request-scope-param-guard-record.md`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，首次同步输出 `Synced 11 changed files`；记录回填后最终复跑输出 `Already up to date`。

当前判断：

- 前端 portal 目录现在有静态守卫防止把客户端身份范围对象键带进请求构造。
- 该守卫只能减少前端误传和回归风险，真实数据范围仍必须由端 token、后端 `PortalSessionContext` 和 seller/buyer Service 内的 session-scoped 查询决定。
- 本轮没有扩大到三端前端物理拆分；后续管理端 UI 接入仍按“卖家模板验收通过后复制买家，只替换配置和 service”的方式推进。

## 2026-06-05 端内会话响应 tokenId 脱敏测试检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，只处理一类问题：固定 `PortalSessionProfile.tokenId` 不得序列化输出给 seller/buyer 端和管理端会话列表响应。本轮不新增接口，不改 SQL，不执行远程数据库 DDL/DML，不改变 seller/buyer 会话查询逻辑，不复制买家，也不启动三端前端物理拆分。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSessionProfileTest.java`。
- 测试覆盖 `terminal`、`subjectId`、`accountId`、`current` 等会话展示字段可正常序列化。
- 测试覆盖 `tokenId` 字段名和内部 tokenId 值均不会出现在 JSON 中。
- 更新 `docs/architecture/reuse-ledger.md`，登记 `PortalSessionProfileTest` 作为会话响应脱敏契约守卫。
- 新增执行记录：`docs/plans/2026-06-05-portal-session-profile-token-redaction-test-record.md`。

子 agent 并行审计结论：

- 文档审计建议下一类工作应进入“端内真实业务接口范围控制模板”，不是立即做三端前端物理拆分。
- seller 后端审计未发现当前 seller portal Controller 直接接收前端传入 `sellerId` 作为端内数据范围。
- seller 后端审计指出当前商品 Schema 是全局只读配置，暂未造成跨卖家数据泄露；下一类更适合做 seller 端商品 SPU/SKU 列表、详情或状态等真实业务接口模板。
- buyer 同构审计确认当前 seller/buyer portal Controller 没有结构性不同构问题；seller 下一刀可以做模板，buyer 后续只替换 terminal、命名、路径、权限、service、mapper、测试和文案。
- buyer 同构审计提醒：`ProductSellerLookupServiceImpl` 是 seller 专属商品归属快照能力，不能机械复制成 buyer；买家浏览商品的谓词应单独设计。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalSessionProfileTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,PortalSessionProfileTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\domain\PortalSessionProfileTest.java docs\plans\2026-06-05-portal-session-profile-token-redaction-test-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，首次同步输出 `Synced 3 changed files`；记录回填后最终复跑输出 `Already up to date`。

当前判断：

- 端内和管理端会话列表继续可以复用 `PortalSessionProfile`，但 `tokenId` 只能作为后端内部字段。
- 该守卫降低后续会话 UI、会话列表接口或 DTO 调整时误把 `tokenId` 暴露给前端的回归风险。
- 下一切片更适合进入 seller 端“我的商城商品”只读查询后端模板，卖家验收通过后再评估买家浏览模板；不要机械按 `buyerId` 复制商品拥有关系。

## 2026-06-05 卖家端我的商城商品只读后端模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并按用户最新节奏执行：先做一套标准卖家模板，验收通过后再复制买家；每个切片只改一类东西。本轮只处理 seller 端我的商城商品只读后端模板，不做前端页面，不复制 buyer，不执行数据库 DDL，不执行 buyer 权限 DML。

已完成：

- 新增 `SellerPortalProductDistributionController`，提供 seller 端商品列表、详情和 SKU 只读入口：
  - `GET /seller/product/distribution-products/list`
  - `GET /seller/product/distribution-products/{spuId}`
  - `GET /seller/product/distribution-products/{spuId}/skus`
- 新增 `ISellerPortalProductService` 和 `SellerPortalProductServiceImpl`：
  - 列表查询强制从 `PortalLoginSession.subjectId` 写入 seller 范围。
  - 前端传入的 `sellerId`、`systemSpuCode`、`systemSkuCode`、`sourceType` 不作为 seller 端查询范围或过滤条件。
  - DTO 转换保留 PageHelper 分页元数据，避免列表 total 退化为当前页条数。
  - 详情和 SKU 查询先校验商品归属，不属于当前 seller 的商品统一按“商城商品不存在”处理。
- 新增 seller 端响应 DTO：
  - `SellerPortalProduct`
  - `SellerPortalProductSku`
  - 不直接返回 `ProductSpu` / `ProductSku`，避免把 `sellerId`、系统 SPU/SKU 和 `BaseEntity` 审计字段作为端内 API 标准。
- 更新 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`：
  - 新增 `seller:product:distribution:list`
  - 新增 `seller:product:distribution:query`
  - active seller role seed 增加上述两个只读权限。
- 远程运行库已执行 seller 权限 DML：
  - 连接来源：本机 `.env.local` 的 `RUOYI_DB_*`。
  - 目标环境：远程 MySQL，数据库 `fenxiao`。
  - 执行类型：DML，仅写入 `seller_menu` 和 `seller_role_menu`。
  - 执行结果：`seller_menu` 中两个权限从 0 条变为 2 条；新增菜单 2 条；新增 active seller role 授权 6 条；最终相关 role-menu 授权 6 条。
- 新增 `SellerPortalProductServiceImplTest`，覆盖 seller 范围收敛、分页 total 保留、非本卖家商品拒绝、DTO 不暴露管理端范围字段、SKU 查询先校验归属。
- 新增 `scripts/smoke/seller-own-distribution-product-read-template-smoke.ps1`，把 seller 登录、商品列表、伪造客户端范围参数、详情、SKU、字段脱敏、跨卖家详情/SKU 拒绝和 logout 清理固化为可复跑 HTTP 烟测。
- 更新 `docs/architecture/reuse-ledger.md`，登记“卖家端我的商城商品只读后端模板”。
- 新增执行记录：`docs/plans/2026-06-05-seller-own-distribution-product-read-template-record.md`。

边界说明：

- 本轮没有修改 `product` 模块现有 admin 分销商品接口、mapper 或业务规则。
- 本轮没有复制 buyer；买家商品浏览的可见性规则不等同于 seller 商品拥有关系，后续必须单独确认。
- 本轮已写入远程 MySQL 的 seller 端权限 DML；没有执行 DDL，没有写入 Redis，没有执行 buyer 相关 DML。
- 本轮没有新增管理端前端页面，也没有启动 `seller-ui` / `buyer-ui` 物理拆分。

验证结果：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller test`：通过，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,PortalTokenSupportTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-own-distribution-product-read-template-smoke.ps1 -SellerUsername '594165649@qq.com' -OtherSellerUsername '1234'`：通过，覆盖 seller 登录、列表、伪造客户端范围参数、详情、SKU、字段脱敏、跨卖家详情/SKU 拒绝和 logout 清理。
- 远程 MySQL seller 权限 DML：通过，`menuBefore=0`、`insertedMenus=2`、`insertedRoleMenus=6`、`menuAfter=2`、`roleMenuAfter=6`。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

运行验收：

- 当前运行数据源已按 `application.yml` / `application-druid.yml` 和 `.env.local` 确认为远程 MySQL / 远程 Redis；记录中不输出凭据。
- 首次 `mvn -DskipTests install` 在 `ruoyi-admin` repackage 阶段失败，原因是旧 8080 Java 进程锁住 `ruoyi-admin.jar`，报 `Unable to rename ... ruoyi-admin.jar.original`。
- 停止旧 8080 Java 进程后执行 `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests install -rf :ruoyi-admin`：通过，`ruoyi-admin.jar` 已重打包。
- `seller-3.9.2.jar` 已确认包含 `SellerPortalProductDistributionController.class` 和 `SellerPortalProductServiceImpl.class`。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1`：已启动新后端，8080 Java 进程存在，`/captchaImage` 返回 200。
- sellerId=5 / accountId=5 真实 seller 登录成功，调用：
  - `GET /seller/product/distribution-products/list?pageNum=1&pageSize=10`：`code=200`，`total=4`，`rows=4`。
  - `GET /seller/product/distribution-products/{sampleSpuId}`：`code=200`。
  - `GET /seller/product/distribution-products/{sampleSpuId}/skus`：`code=200`，`skuRows=2`。
  - 列表、详情和 SKU 响应未出现 `sellerId`、`systemSpuCode`、`systemSkuCode` 字段。
- sellerId=9 / accountId=8 真实 seller 登录成功后访问 sellerId=5 的 sample SPU：接口返回业务 `code=500`，消息为“商城商品不存在”，跨卖家访问被拒绝。
- 脚本化烟测走真实 HTTP 业务链路，会产生 seller 端登录/退出日志和会话记录；脚本结束时已调用 `/seller/logout` 清理本次 token，且不输出 token、JWT、Redis key、`.env.local` 或数据库连接明文。
- 脚本化烟测已补强断言：伪造 `sellerId`、`subjectId`、`accountId`、`terminal`、`systemSpuCode`、`sourceType` 不改变当前 seller 列表范围；跨卖家详情和 SKU 均返回业务 `code=500`，语义为“商城商品不存在”。
- 运行验收记录补充后执行 `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 5 changed files`。
- 脚本化烟测补强和文档补记后再次执行 `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- seller 端“我的商城商品”只读后端模板已经具备端入口、权限点、范围收敛、响应 DTO 和契约测试。
- 当前 seller 模板适合作为后续 seller 端同类业务接口的基线：Controller 只负责端入口、鉴权、分页和返回；Service 负责基于 `PortalLoginSession` 做数据范围和响应字段收敛。
- 真实后端运行验收已通过，seller 模板可以进入用户验收；但 buyer 仍未复制。
- buyer 端后续不应机械复制 seller 商品拥有关系。若要做买家商品浏览，需要先确认商品可见性、上架状态、价格口径和库存可见边界，再按已验收模板替换 terminal、路径、权限点、service、DTO 和测试。

## 2026-06-05 卖家端我的商城商品前端工作台模板检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，并遵守“先做一套标准卖家模板，验收通过后再复制买家；每个切片只改一类东西”的节奏。本轮只处理 seller portal 前端工作台接入，不复制 buyer，不改后端接口，不执行数据库 DDL/DML，不改权限 seed。

已完成：

- 更新 `react-ui/src/types/seller-buyer/party.d.ts`，新增 seller 端商品只读 DTO 类型。
- 更新 `react-ui/src/services/portal/session.ts`，新增 seller 端商品列表、详情和 SKU service。
- 新增 `react-ui/src/pages/Portal/Home/SellerOwnDistributionProductList.tsx`：
  - 展示“我的商城商品”只读列表。
  - 支持分页、刷新、详情弹窗和 SKU 表。
  - 不展示 `sellerId`、系统 SPU/SKU、token 或后台审计字段。
- 更新 `react-ui/src/pages/Portal/Home/index.tsx`，仅当 `terminal === 'seller'` 时展示“我的商城商品”卡片。
- 新增执行记录：`docs/plans/2026-06-05-seller-portal-distribution-product-ui-template-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller 前端工作台模板。

边界说明：

- 本轮没有复制 buyer；买家商品浏览仍需单独确认可见性、上架状态、价格口径和库存边界。
- 本轮没有新增或修改后端接口，没有执行远程数据库 DDL/DML，没有变更权限 seed。
- 浏览器验收生成了 seller 免密登录票据、seller 登录/退出日志和会话记录；本记录不输出任何 token、directLoginToken 或登录 URL。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，`Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check src/pages/Portal/Home/SellerOwnDistributionProductList.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check src/pages/Portal/Home/SellerOwnDistributionProductList.tsx src/pages/Portal/Home/index.tsx src/services/portal/session.ts src/types/seller-buyer/party.d.ts`：未作为通过项；`Portal/Home/index.tsx` 存在大量既有格式化差异，已避免整文件格式化造成无关 diff。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白和冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 6 changed files`，`Added: 1, Modified: 5 - 210 nodes`。

浏览器验收：

- 8080 后端和 8001 前端均已监听。
- 通过管理端免密票据进入 `/seller/portal`。
- DOM 验证出现“卖家端”“商品发布准备”“我的商城商品”“客户SPU”和商品列。
- 点击商品“详情”后，弹窗出现“商品详情”“客户SKU”“SKU规格”和“商品状态”。
- 截图检查未发现新增卡片或详情弹窗明显遮挡、错位。
- 浏览器控制台错误检查：无 error / warning / warn。
- 验收结束后通过 UI 点击“退出”，页面回到 `/user/login`。

当前判断：

- seller 端“我的商城商品”前端工作台模板已经和后端只读模板贯通，可以进入 seller 模板验收。
- buyer 仍未复制；后续复制前必须先确认买家商品浏览口径。

## 2026-06-05 卖家端商品列表前端查询范围守卫补强检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，只处理一类问题：seller portal 商品列表前端 service 必须纳入 portal query 参数清洗静态守卫。本轮不复制 buyer，不改后端接口，不执行数据库 DDL/DML，不启动三端前端物理拆分。

已完成：

- 更新 `react-ui/scripts/check-portal-token-isolation.mjs`：
  - `portalQueryFunctions` 新增 `getSellerPortalDistributionProducts`。
  - 新增 `PORTAL_SCOPE_PARAM_KEYS` 必须包含 `terminal` 的静态检查。
- 新增执行记录：`docs/plans/2026-06-05-seller-product-portal-query-scope-guard-record.md`。
- 更新 `docs/architecture/reuse-ledger.md`，登记 seller 商品列表查询参数清洗守卫规则。

边界说明：

- 首次尝试把 `terminal:` 加入全局禁止对象键正则后，守卫误伤合法端配置和 TypeScript 函数参数；已改为检查 sanitizer 清单必须包含 `terminal`。
- 真实数据范围仍以后端 `PortalLoginSession.subjectId` 为准；前端守卫只防止误传和回归。

验证结果：

- `cd E:\Urili-Ruoyi\react-ui; npx biome check --write scripts/check-portal-token-isolation.mjs`：通过并格式化脚本。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，`Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check scripts/check-portal-token-isolation.mjs`：通过。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白和冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

当前判断：

- seller portal 商品列表 service 已被静态守卫纳入查询参数清洗契约。
- buyer 仍未复制；后续复制前必须先完成 seller 模板验收并确认买家商品浏览口径。
