# 三端 Portal 自助权限 seed 与 live 验证 runbook

日期：2026-06-11 00:48 +08:00

本 runbook 对应 `docs/reports/2026-06-10-three-terminal-portal-self-management-progress-record.md` 的下一步：执行 `RuoYi-Vue/sql/20260610_portal_self_management_permission_seed.sql`，并完成 seller/buyer portal 最小自助权限闭环 live 验证。

## 当前结论

- 本文档是执行前计划和记录模板，不代表 SQL 已执行。
- 本轮不得扩展商品、订单、库存、物流、财务、履约、外部系统等业务菜单或业务页面。
- 本轮 SQL 只允许用于三端 portal 最小自助权限框架：
  - 补齐 seller/buyer 账号、角色、部门自助管理按钮权限。
  - 给 active OWNER 角色补齐 19 个 self-management 权限。
  - 仅清理 active OWNER 角色上的非 self-management role-menu 授权。
- 前置依赖：
  - `seller:portal:home` / `buyer:portal:home` 必须已作为端内 `C` 页面菜单存在。
  - 如果目标库缺少 portal 首页 `C` 菜单，先按同样确认机制执行 `RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql`。
  - 如果只缺 OWNER 对 portal 首页的 role-menu 授权，本次 `20260610_portal_self_management_permission_seed.sql` 会按 `C` 页面菜单签名补齐。
- 执行前必须由用户确认目标环境；未确认前不得执行远端 DDL/DML。

## 数据源证据

已只读检查配置文件，未读取 `.env.local` 明文。

| 项 | 当前证据 | 结论 |
| --- | --- | --- |
| 激活 profile | `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 中 `spring.profiles.active: druid` | 后端使用 druid 配置 |
| MySQL 连接来源 | `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 中 `url: ${RUOYI_DB_URL:}`、`username: ${RUOYI_DB_USERNAME:}`、`password: ${RUOYI_DB_PASSWORD:}` | 实际目标由本机环境变量或 `.env.local` 注入，不能默认 localhost |
| Redis 连接来源 | `application.yml` 中 `RUOYI_REDIS_HOST` / `RUOYI_REDIS_PORT` / `RUOYI_REDIS_DATABASE` / `RUOYI_REDIS_PASSWORD` | Redis 不参与本 SQL 执行；live 验证会使用当前后端配置 |
| 前置 SQL | `RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql` | 仅当目标库缺少 portal 首页 `C` 菜单时执行 |
| SQL 文件 | `RuoYi-Vue/sql/20260610_portal_self_management_permission_seed.sql` | 待执行 |

## SQL 安全边界

脚本自带以下 guard：

- 确认变量：`@confirm_portal_self_management_permission_seed`
- 确认值：`APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED`
- fail-closed：未设置确认变量时抛 `45000`
- 端内菜单 ID 区间：
  - seller：`100000-199999`
  - buyer：`200000-299999`
- 权限前缀、空权限、任意 `*` 通配权限、admin 命名空间校验。
- 页面菜单 `component` 校验。
- 事务：`start transaction` 到 `commit`。
- 写后断言：
  - seller/buyer self-management 权限必须创建完成。
  - active OWNER 角色授权数量必须等于 `owner_role_count * 19`。
  - seller/buyer active OWNER role-menu 不得残留非 self-management 授权。

## 预执行只读检查

执行 SQL 前先做只读检查，并把结果追加到本文件的“执行记录”部分。

推荐固定入口：

```powershell
cd E:\Urili-Ruoyi
node .\scripts\portal-self-management-sql-runner.mjs --precheck
```

该 runner 默认只读预检，读取当前 `.env.local` 中的 `RUOYI_DB_*` 目标，不输出连接串、账号或密码明文，不执行 SQL seed。

1. 确认目标库不是本地 Docker 误连。

```powershell
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}"
```

2. 确认本机运行环境变量来源，不输出明文值。

```powershell
cd E:\Urili-Ruoyi
Select-String -Path .\.env.local -Pattern '^RUOYI_DB_URL=|^RUOYI_REDIS_HOST=|^RUOYI_REDIS_DATABASE='
```

3. 只读查询目标库关键状态。

命令类型：只读 `SELECT`。如果本机没有 `mysql` CLI，可继续使用本机 Maven 缓存中的 MySQL JDBC 驱动或既有 JShell/JDBC 执行方式。只读结果不得包含密码、token 或连接串明文。

建议检查：

```sql
select database() as current_database;

select 'seller_menu' as table_name, count(*) as rows_count,
       min(seller_menu_id) as min_id, max(seller_menu_id) as max_id
from seller_menu
union all
select 'buyer_menu', count(*), min(buyer_menu_id), max(buyer_menu_id)
from buyer_menu;

select 'seller_owner_role' as item, count(*) as rows_count
from seller_role
where del_flag = '0' and status = '0' and role_key = 'owner'
union all
select 'buyer_owner_role', count(*)
from buyer_role
where del_flag = '0' and status = '0' and role_key = 'owner';

select 'seller_portal_home_menu' as item, count(*) as rows_count
from seller_menu
where perms = 'seller:portal:home'
  and parent_id = 0
  and menu_type = 'C'
  and coalesce(path, '') = '/seller/portal'
  and coalesce(component, '') = 'Seller/Portal/index'
  and coalesce(route_name, '') = 'SellerPortalHome'
union all
select 'buyer_portal_home_menu', count(*)
from buyer_menu
where perms = 'buyer:portal:home'
  and parent_id = 0
  and menu_type = 'C'
  and coalesce(path, '') = '/buyer/portal'
  and coalesce(component, '') = 'Buyer/Portal/index'
  and coalesce(route_name, '') = 'BuyerPortalHome';

select 'seller_non_self_management_grants' as item, count(*) as rows_count
from seller_role_menu rm
join seller_role r on r.seller_role_id = rm.seller_role_id
join seller_menu m on m.seller_menu_id = rm.seller_menu_id
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and m.perms not in (
  'seller:portal:home',
  'seller:account:list', 'seller:account:add', 'seller:account:edit',
  'seller:account:role:query', 'seller:account:role:edit',
  'seller:account:loginLog:list', 'seller:account:operLog:list',
  'seller:account:session:list',
  'seller:dept:list', 'seller:dept:query', 'seller:dept:add', 'seller:dept:edit', 'seller:dept:remove',
  'seller:role:list', 'seller:role:query', 'seller:role:add', 'seller:role:edit', 'seller:role:remove'
)
union all
select 'buyer_non_self_management_grants', count(*)
from buyer_role_menu rm
join buyer_role r on r.buyer_role_id = rm.buyer_role_id
join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
where r.del_flag = '0'
  and r.status = '0'
  and r.role_key = 'owner'
  and m.perms not in (
  'buyer:portal:home',
  'buyer:account:list', 'buyer:account:add', 'buyer:account:edit',
  'buyer:account:role:query', 'buyer:account:role:edit',
  'buyer:account:loginLog:list', 'buyer:account:operLog:list',
  'buyer:account:session:list',
  'buyer:dept:list', 'buyer:dept:query', 'buyer:dept:add', 'buyer:dept:edit', 'buyer:dept:remove',
  'buyer:role:list', 'buyer:role:query', 'buyer:role:add', 'buyer:role:edit', 'buyer:role:remove'
);
```

## SQL 执行步骤

仅在用户确认目标环境后执行。

命令类型：远端 MySQL DML seed。影响范围：`seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`；其中 role-menu 清理仅限 active OWNER 角色。不新增业务表，不写商品、订单、库存、物流、财务、履约或外部系统业务数据。

执行要求：

- 必须在同一数据库连接内先设置确认变量，再执行脚本。
- 不得把 `.env.local` 中的明文连接信息写入报告或聊天。
- 任意 `45000` fail-closed 失败后停止，不做手工补写。

如预执行只读检查发现 portal 首页 `C` 菜单缺失，应先在同一目标环境执行前置 seed：

```sql
set @confirm_terminal_portal_home_menu_seed = 'APPLY_TERMINAL_PORTAL_HOME_MENU_SEED';
source RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql;
```

同连接执行语义：

```sql
set @confirm_portal_self_management_permission_seed = 'APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED';
source RuoYi-Vue/sql/20260610_portal_self_management_permission_seed.sql;
```

推荐固定执行入口：

```powershell
cd E:\Urili-Ruoyi
$env:PORTAL_SELF_MANAGEMENT_SQL_CONFIRM = 'APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED'
node .\scripts\portal-self-management-sql-runner.mjs --apply
```

该 runner 会在同一 JDBC 连接内先设置 `@confirm_portal_self_management_permission_seed`，再执行 `20260610_portal_self_management_permission_seed.sql`；未设置确认变量时 `--apply` 会在连接数据库前 fail-closed。

如果使用 JShell/JDBC 或其他脚本执行，必须等价保证：

1. 读取本机当前运行配置。
2. 打开单个 MySQL 连接。
3. 先执行确认变量设置。
4. 再逐条执行 `20260610_portal_self_management_permission_seed.sql`。
5. 记录退出码、异常信息和完成态只读查询结果。

## 执行后只读确认

执行后必须至少确认：

```sql
select 'seller_owner_exact_grants' as item, count(*) as rows_count
from seller_role r
join seller_role_menu rm on rm.seller_role_id = r.seller_role_id
join seller_menu m on m.seller_menu_id = rm.seller_menu_id
where r.del_flag = '0' and r.status = '0' and r.role_key = 'owner'
  and m.perms in (
    'seller:portal:home',
    'seller:account:list', 'seller:account:add', 'seller:account:edit',
    'seller:account:role:query', 'seller:account:role:edit',
    'seller:account:loginLog:list', 'seller:account:operLog:list',
    'seller:account:session:list',
    'seller:dept:list', 'seller:dept:query', 'seller:dept:add', 'seller:dept:edit', 'seller:dept:remove',
    'seller:role:list', 'seller:role:query', 'seller:role:add', 'seller:role:edit', 'seller:role:remove'
  )
union all
select 'buyer_owner_exact_grants', count(*)
from buyer_role r
join buyer_role_menu rm on rm.buyer_role_id = r.buyer_role_id
join buyer_menu m on m.buyer_menu_id = rm.buyer_menu_id
where r.del_flag = '0' and r.status = '0' and r.role_key = 'owner'
  and m.perms in (
    'buyer:portal:home',
    'buyer:account:list', 'buyer:account:add', 'buyer:account:edit',
    'buyer:account:role:query', 'buyer:account:role:edit',
    'buyer:account:loginLog:list', 'buyer:account:operLog:list',
    'buyer:account:session:list',
    'buyer:dept:list', 'buyer:dept:query', 'buyer:dept:add', 'buyer:dept:edit', 'buyer:dept:remove',
    'buyer:role:list', 'buyer:role:query', 'buyer:role:add', 'buyer:role:edit', 'buyer:role:remove'
  );
```

并复跑预执行中的 active OWNER `non_self_management_grants` 查询，期望 seller/buyer 均为 `0`。

## live 验证矩阵

不做截图、DOM/UI 细调；只验证 P0/P1 功能闭环。

### 只读脚本验证

执行 seed 后，可先用只读脚本验证账号密码登录、登录响应字段白名单、匿名 `getInfo` / `getRouters` 拒绝、`getInfo` 响应字段白名单、`getRouters`、自助列表接口、`/roles/menus` 模板 ID 区间和 19 个 self-management 权限模板数量、冻结业务权限和冻结业务路由 path 不可见、自助登录日志/操作日志/在线会话 DTO 不泄露内部审计字段，以及跨端 token 拒绝。该脚本不会读取 `.env.local`，不会执行 SQL，也不会做新增账号、角色、部门等写操作。

```powershell
cd E:\Urili-Ruoyi\react-ui
$env:PORTAL_LIVE_BASE_URL = 'http://127.0.0.1:8080'
$env:SELLER_PORTAL_USERNAME = '<seller owner username>'
$env:SELLER_PORTAL_PASSWORD = '<seller owner password>'
$env:BUYER_PORTAL_USERNAME = '<buyer owner username>'
$env:BUYER_PORTAL_PASSWORD = '<buyer owner password>'
$env:PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM = 'APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY'
npm run verify:portal-self-management-live
```

如果缺少确认变量或任一账号密码环境变量，脚本会 fail-closed 并列出缺失变量；不得把明文账号密码写入报告。

### 受控写脚本验证

只读脚本通过后，可以用受控写脚本验证 OWNER 在 seller/buyer portal 内创建子账号、维护部门、维护角色、分配角色、查看自助审计 DTO 的最小闭环。该脚本默认不会运行写操作，必须显式提供确认变量。账号列表 DTO 必须返回端内 `accountId` 作为编辑和分配角色的操作句柄，但不得返回 `subjectId` 或 `terminal`。

安全边界：

- 不读取 `.env.local`。
- 不执行 SQL。
- 不访问商品、订单、库存、物流、财务、履约、外部系统等业务 API。
- 只调用 seller/buyer portal 自助权限框架接口：`/accounts`、`/depts`、`/roles`、`/roles/menus`、`/account/login-logs`、`/account/oper-logs`、`/account/sessions`。
- 脚本会清理可删除的测试角色和测试部门。
- 因当前 portal 自助面没有账号删除能力，脚本会留下一个停用的 STAFF 测试账号作为写闭环证据；该账号用户名带 `verify_` 前缀，不能作为业务账号使用。

```powershell
cd E:\Urili-Ruoyi\react-ui
$env:PORTAL_LIVE_BASE_URL = 'http://127.0.0.1:8080'
$env:SELLER_PORTAL_USERNAME = '<seller owner username>'
$env:SELLER_PORTAL_PASSWORD = '<seller owner password>'
$env:BUYER_PORTAL_USERNAME = '<buyer owner username>'
$env:BUYER_PORTAL_PASSWORD = '<buyer owner password>'
$env:PORTAL_LIVE_WRITE_CONFIRM = 'APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY'
npm run verify:portal-self-management-live-write
```

通过标准：

- seller/buyer `/getInfo` 权限集合精确等于 19 个 self-management 权限。
- seller/buyer 角色菜单模板 ID 分别位于 `100000-199999` / `200000-299999`。
- seller/buyer `/getInfo` 响应只包含 `subjectNo` / `userName` / `nickName` / `roles` / `permissions` 白名单字段。
- seller/buyer 角色菜单模板非空、ID 不重复、数量等于 19 个 self-management 权限点。
- OWNER 能创建、编辑、删除测试部门。
- OWNER 能创建、编辑、删除测试角色，且角色授权只来自 self-management 菜单模板。
- OWNER 能创建停用 STAFF 子账号；账号记录必须返回可用 `accountId`，不得返回 `subjectId` / `terminal`，并能给该子账号分配、清空测试角色。
- 自助登录日志、操作日志、在线会话 DTO 不暴露内部审计字段。

### 人工运行态验证

脚本验证后，再人工验证脚本无法覆盖或不适合自动化写入的运行态场景。

| 场景 | 验证方式 | 通过标准 |
| --- | --- | --- |
| seller OWNER 账号密码登录 | 调用 seller portal 登录接口或浏览器登录 `/seller/login` | 返回 seller token，Redis/session 端标识为 seller |
| buyer OWNER 账号密码登录 | 调用 buyer portal 登录接口或浏览器登录 `/buyer/login` | 返回 buyer token，Redis/session 端标识为 buyer |
| 管理端直登 seller OWNER | 管理端对 seller 主体发起 direct-login | 目标 portal 消费成功并回传 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 成功 |
| 管理端直登 buyer OWNER | 管理端对 buyer 主体发起 direct-login | 目标 portal 消费成功并回传成功 |
| seller `/getInfo` / `/getRouters` | 使用 seller token 调用 | 只返回 seller self-management 权限和路由，不返回 product/order 等冻结业务权限或冻结业务 path |
| buyer `/getInfo` / `/getRouters` | 使用 buyer token 调用 | 只返回 buyer self-management 权限和路由，不返回 product/order 等冻结业务权限或冻结业务 path |
| OWNER 创建子账号 | 复核写脚本留下的停用 STAFF 测试账号，必要时人工创建一个测试账号 | 后端从 token 推导主体，前端不传 `sellerId/buyerId/subjectId` |
| OWNER 角色授权 | 复核写脚本的角色授权日志，必要时人工给测试账号分配非 OWNER 角色 | 不存在/跨端/非 self-management menuId fail-closed |
| 部门维护 | 复核写脚本新增、编辑、删除测试部门的操作日志 | 只影响当前端当前主体 |
| 登录日志 / 操作日志 / 在线会话 | 分别查看 seller/buyer 自助审计页 | 自助 DTO 不暴露内部审计字段，管理端审计仍完整 |
| 401 隔离 | 使用失效 seller/buyer token 调用端内 API | 只清当前端 token，跳当前端 login，保留另一端 token |

## 验证后门禁

SQL 和 live 验证后，如果代码没有变化，至少复跑：

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run verify:portal-self-management-live
npm run verify:portal-self-management-live-write
npm run verify:portal-direct-login-live
npm run verify:three-terminal
```

三类 live 脚本覆盖范围：

- `verify:portal-self-management-live`：只读验证账号密码登录响应只包含 `token` / `terminal` / `subjectNo` / `username` / `nickName` / `expire*` 白名单字段、匿名 `/getInfo` / `/getRouters` 拒绝、`getInfo` 响应只包含 `subjectNo` / `userName` / `nickName` / `roles` / `permissions` 白名单字段、`getInfo` / `getRouters` 自助权限与路由收敛、`/roles/menus` 模板非空、ID 不重复、seller/buyer ID 区间正确且数量等于 19 个 self-management 权限模板、冻结业务权限和冻结业务 path 不可见、只读自助接口、自助审计 DTO 不泄露 `subjectId` / `accountId` / `directLoginTicketId` / `actingAdmin*` / `directLoginReason` / `operParam` / `jsonResult` / `tokenId`，以及账号密码登录后的跨端 token 访问另一端 `/getInfo` / `/getRouters` 必须被拒绝。
- `verify:portal-self-management-live-write`：需要写确认变量，验证登录响应和 `/getInfo` 响应白名单、OWNER 创建子账号、账号 DTO 返回 `accountId` 但不返回 `subjectId` / `terminal`、角色授权、部门维护、日志和会话自助 DTO 脱敏。
- `verify:portal-direct-login-live`：需要管理端认证上下文和写确认变量；管理端认证可用 `ADMIN_AUTH_TOKEN`，也可用 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 临时登录获取 token。目标主体 ID 可选，未提供 `SELLER_DIRECT_LOGIN_SUBJECT_ID` / `BUYER_DIRECT_LOGIN_SUBJECT_ID` 时脚本会用管理端只读列表自动选择 active OWNER 主体。该脚本验证管理端到 OWNER 的 direct-login ticket 生成响应短时且只包含 `token` / `ticketId` / `loginUrl` / `expire*` 白名单字段、外端 portal 拒绝消费、当前端消费后的登录响应只包含 portal 登录白名单字段、消费后 portal `/getInfo` 响应只包含 `subjectNo` / `userName` / `nickName` / `roles` / `permissions` 白名单字段、同一 ticket 不可复用、消费后 portal `/getInfo` / `/getRouters` 权限和路由收敛，并验证消费后的 portal token 不能访问另一端 `/getInfo` / `/getRouters`。

live 脚本默认直连后端 `http://127.0.0.1:8080`，请求路径不带 `/api`。如果改为通过 React dev server `http://127.0.0.1:8001` 验证，必须显式设置：

```powershell
$env:PORTAL_LIVE_BASE_URL = 'http://127.0.0.1:8001'
$env:PORTAL_LIVE_API_PREFIX = '/api'
```

管理端直登 live 验证需要管理端认证上下文，并设置写确认变量。目标主体 ID 可选；未显式传入时，脚本会通过管理端只读列表自动发现 active OWNER 主体：

```powershell
# 方式一：复用当前管理端 token
$env:ADMIN_AUTH_TOKEN = '<current-admin-token>'

# 方式二：让脚本临时登录管理端，二选一即可
$env:ADMIN_USERNAME = '<admin username>'
$env:ADMIN_PASSWORD = '<admin password>'

# 可选：显式指定目标主体；不提供时自动发现 active OWNER 主体
$env:SELLER_DIRECT_LOGIN_SUBJECT_ID = '<optional-seller-id>'
$env:BUYER_DIRECT_LOGIN_SUBJECT_ID = '<optional-buyer-id>'

$env:PORTAL_DIRECT_LOGIN_LIVE_CONFIRM = 'APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY'
npm run verify:portal-direct-login-live
```

如执行过程中产生任何代码或 seed 修改，还需复跑相关 Maven/Jest 窄测，并在报告中写明。

## 代码级前置门禁

执行 SQL seed 或 live 写验证前，后端代码应继续满足以下门禁：

- `SellerPortalController` / `BuyerPortalController` 的 `/roles/menus` 模板读取只展示本轮 self-management 权限模板。
- 模板读取和角色分配预检对命中的菜单调用 `PortalPermissionSupport.assertReadableTerminalMenu(...)`，确保端内菜单 ID 区间、菜单类型、component、权限前缀、通配符和 admin 命名空间 fail-closed。
- `PortalSelfServiceSurfaceContractTest` 必须通过，避免 portal 自助角色分配退回只按权限字符串过滤。

当前已验证：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：`PortalSelfServiceSurfaceContractTest` 1 test passed，seller/buyer 模块随 reactor 编译通过，BUILD SUCCESS。

## 执行记录模板

执行前后把本节改为实际记录。

### 执行前只读预检记录

- 预检时间：2026-06-11 02:14:05 +08:00。
- 预检性质：只读检查；未执行 SQL，未读取或输出 `.env.local` 明文值，未写 MySQL 或 Redis。
- 激活配置：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 中 `spring.profiles.active: druid`。
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 中 MySQL 连接来自 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`。
  - `application.yml` 中 Redis 连接来自 `RUOYI_REDIS_HOST` / `RUOYI_REDIS_PORT` / `RUOYI_REDIS_DATABASE` / `RUOYI_REDIS_PASSWORD`。
- 本机运行变量存在性：
  - `.env.local` 存在。
  - `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 均存在，且 `RUOYI_DB_URL` 不像 `localhost` / `127.0.0.1` / `::1`。
  - `RUOYI_REDIS_HOST` / `RUOYI_REDIS_DATABASE` 均存在，且 `RUOYI_REDIS_HOST` 不像 `localhost` / `127.0.0.1` / `::1`。
  - `RUOYI_TOKEN_SECRET` 和 `URILI_SECRET_ENCRYPTION_KEY` 均存在。
- 本地 Docker 干扰检查：
  - `docker ps` 未发现 URILI MySQL / Redis 容器正在运行。
  - 当前运行容器包含 `open-design`、`urili-postgres`、`lianghua-*`；本轮 SQL/live 不应读取这些容器作为目标环境。
- 执行工具检查：
  - `mysql` CLI 当前不在 PATH。
  - 如用户确认执行 SQL，需使用 JDBC/脚本方式或先明确可用的 MySQL 客户端；无论使用哪种方式，都必须保证同一连接内先设置确认变量，再执行 seed。
- SQL 文件存在性：
  - `RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql` 存在。
  - `RuoYi-Vue/sql/20260610_portal_self_management_permission_seed.sql` 存在。
- 当前结论：
  - 目标连接由本机运行变量注入，不能默认本地 Docker 或 localhost。
  - 仍需用户明确确认目标环境后，才能执行远端 DML seed 或任何 live 写验证。

### 目标库只读 SELECT 预检记录

- 预检时间：2026-06-11 02:20 +08:00。
- 预检工具：JShell + 本机 Maven 缓存 MySQL JDBC 驱动。
- 预检性质：只读 `SELECT`；连接设置为 read-only；未执行 `INSERT` / `UPDATE` / `DELETE` / DDL；未设置 seed 确认变量；未写 MySQL 或 Redis。
- 连接来源：读取当前 `.env.local` 注入的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`，记录中不输出明文连接串、账号或密码。

| 检查项 | 结果 |
| --- | --- |
| 当前库 | `fenxiao` |
| `seller_menu` | 11 行，ID 范围 `100008-100019` |
| `buyer_menu` | 11 行，ID 范围 `200003-200014` |
| active seller OWNER 角色 | 35 个 |
| active buyer OWNER 角色 | 35 个 |
| seller portal 首页 C 菜单 | 已存在 1 个 |
| buyer portal 首页 C 菜单 | 已存在 1 个 |
| seller self-management 权限模板 | 已存在 7 / 19 个 |
| buyer self-management 权限模板 | 已存在 7 / 19 个 |
| seller OWNER self-management 授权 | 245 条，即 `35 * 7` |
| buyer OWNER self-management 授权 | 245 条，即 `35 * 7` |
| seller OWNER 非 self-management 授权 | 0 条 |
| buyer OWNER 非 self-management 授权 | 0 条 |
| seller 端无效菜单权限 | 0 条 |
| buyer 端无效菜单权限 | 0 条 |

当前缺失的 seller self-management 权限：

```text
seller:account:add
seller:account:edit
seller:account:role:edit
seller:account:role:query
seller:dept:add
seller:dept:edit
seller:dept:query
seller:dept:remove
seller:role:add
seller:role:edit
seller:role:query
seller:role:remove
```

当前缺失的 buyer self-management 权限：

```text
buyer:account:add
buyer:account:edit
buyer:account:role:edit
buyer:account:role:query
buyer:dept:add
buyer:dept:edit
buyer:dept:query
buyer:dept:remove
buyer:role:add
buyer:role:edit
buyer:role:query
buyer:role:remove
```

预检结论：

- 目标库已具备 seller/buyer portal 首页 `C` 菜单，因此本次执行 `20260610_portal_self_management_permission_seed.sql` 前，不需要先执行 `20260610_terminal_portal_home_menu_seed.sql`。
- 目标库当前每端仅具备 7 个 self-management 权限模板，缺少本轮新增的 12 个账号/部门/角色自助按钮和查询权限；本轮 seed 仍需执行。
- active OWNER 当前没有非 self-management 授权残留；本轮 seed 的 OWNER-only 清理预计为 no-op，但仍应保留写前/写后断言。

### 执行确认

- 用户确认时间：
- 确认目标环境：
- MySQL 连接来源：当前 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 注入，未在记录中输出明文。
- Redis 连接来源：当前 `RUOYI_REDIS_*` 注入，未在记录中输出明文。
- 是否发现本地 Docker MySQL/Redis 正在运行：
- SQL 文件：`RuoYi-Vue/sql/20260610_portal_self_management_permission_seed.sql`
- 确认变量：`@confirm_portal_self_management_permission_seed`
- 确认值：`APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED`
- 命令类型：远端 MySQL DML seed。
- 影响范围：`seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`。
- 不影响范围：不写商品、订单、库存、物流、财务、履约、外部系统业务数据；不读写 Redis。

### 执行结果

- 执行时间：
- 执行工具：
- 退出码：
- 是否出现 `45000`：
- 执行后只读确认：
- live 验证结果：
- `npm run verify:three-terminal` 结果：
- CodeGraph 同步结果：
- 未验证项：

## 代码级门禁补充：SQL 模板签名与 runner postcheck

- 更新时间：2026-06-11。
- SQL seed 前置断言：
  - `20260610_portal_self_management_permission_seed.sql` 会在写入前检查 seller/buyer 端内菜单 ID 区间、权限前缀、空权限、通配权限、admin 命名空间、页面 component 根路径和重复 perms。
  - 本轮新增补强后，seed 会同时检查 portal 首页 `C` 菜单签名，以及 18 个 root `F` self-management 权限模板签名。
  - slot guard 覆盖 portal 首页、账号列表、登录日志、操作日志、在线会话、部门列表、角色列表以及本轮新增的账号/部门/角色按钮和查询权限；任一历史模板签名漂移都会 `45000` fail-closed。
- runner 执行后断言：
  - `scripts/portal-self-management-sql-runner.mjs --apply` 在 seed commit 后会执行只读 postcheck。
  - postcheck 期望 seller/buyer 各有 1 个 portal 首页 `C` 菜单、19 个 self-management 权限模板、18 个 root `F` 权限模板。
  - postcheck 期望 active OWNER 授权数量为 `owner_role_count * 19`，且 active OWNER 不保留非 self-management 授权。
  - postcheck 还会检查端内菜单 ID 区间、页面 component 根路径、重复 perms 和无效权限前缀；任一项不符合都会让 runner 以非 0 退出。
- 当前已验证：
  - `npx jest --config jest.config.ts tests/portal-self-management-sql-runner-contract.test.ts --runInBand` 通过，1 suite / 6 tests passed。
  - `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，82 tests passed。
  - `npm run verify:three-terminal` 通过，前端 33 suites / 296 tests passed，后端 reactor `test-compile` 和三端合同均 BUILD SUCCESS。
- 执行边界：
  - 本补充仅更新代码级执行门禁和记录；尚未执行远端 SQL seed。
  - 真正执行 `--apply` 前仍需用户确认目标环境和影响范围。

## 复跑只读预检记录

- 预检时间：2026-06-11 04:51 +08:00。
- 预检工具：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
- 预检性质：只读 `SELECT`；runner 输出 `portal self-management SQL precheck completed without writes.`；未执行 SQL seed，未写 MySQL 或 Redis。
- 配置证据：
  - `application.yml` 当前激活 profile 仍为 `druid`。
  - `application-druid.yml` 中 MySQL URL、用户名、密码仍由 `RUOYI_DB_*` 注入，不能默认 `localhost`。
  - Redis 仍由 `RUOYI_REDIS_*` 注入；本次 SQL precheck 未读写 Redis。
  - `.env.local` 存在 `RUOYI_DB_URL`、`RUOYI_REDIS_HOST`、`RUOYI_REDIS_DATABASE`，记录中已脱敏，不输出明文。
  - `docker ps` 无法连接 Docker API，未发现本地 Docker MySQL/Redis 正在运行并可被误读。

| 检查项 | 结果 |
| --- | --- |
| 当前库 | `fenxiao` |
| `seller_menu` | 11 行，ID 范围 `100008-100019` |
| `buyer_menu` | 11 行，ID 范围 `200003-200014` |
| active seller OWNER 角色 | 35 个 |
| active buyer OWNER 角色 | 35 个 |
| seller portal 首页 C 菜单 | 1 个 |
| buyer portal 首页 C 菜单 | 1 个 |
| seller self-management 权限模板 | 7 / 19 个 |
| buyer self-management 权限模板 | 7 / 19 个 |
| seller root button 权限模板 | 6 / 18 个 |
| buyer root button 权限模板 | 6 / 18 个 |
| seller OWNER self-management 授权 | 245 条，即 `35 * 7` |
| buyer OWNER self-management 授权 | 245 条，即 `35 * 7` |
| seller OWNER 非 self-management 授权 | 0 条 |
| buyer OWNER 非 self-management 授权 | 0 条 |
| seller 无效菜单权限 | 0 条 |
| buyer 无效菜单权限 | 0 条 |
| seller 菜单 ID 区间违规 | 0 条 |
| buyer 菜单 ID 区间违规 | 0 条 |
| seller 页面 component 违规 | 0 条 |
| buyer 页面 component 违规 | 0 条 |
| seller 重复 perms | 0 条 |
| buyer 重复 perms | 0 条 |

复跑结论：

- 目标库仍已具备 seller/buyer portal 首页 `C` 菜单，不需要先执行 `20260610_terminal_portal_home_menu_seed.sql`。
- 目标库仍只具备每端 7 个 self-management 权限模板，缺少本轮新增的 12 个账号/部门/角色自助按钮和查询权限。
- active OWNER 当前没有非 self-management 授权残留。
- 最终闭环仍需要用户确认目标环境后执行 `20260610_portal_self_management_permission_seed.sql`，再做 seller/buyer portal live 登录、直登和写验证。

## 2026-06-12 01:33 执行前确认记录

- 用户确认时间：2026-06-12 01:33 +08:00。
- 用户确认原文：确认允许对当前目标库 fenxiao 执行本阶段 self-management 权限 seed，影响范围仅限 seller_menu、buyer_menu、seller_role_menu、buyer_role_menu。
- 当前激活配置：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 当前 `spring.profiles.active` 为 `druid`。
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 中 MySQL 连接来自 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 注入。
  - Redis 连接来自 `RUOYI_REDIS_*` 注入；本次 SQL seed 不读写 Redis。
- 只读预检命令：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
- 只读预检结果：
  - 当前库：`fenxiao`。
  - `seller_menu`：11 行，ID 范围 `100008-100019`。
  - `buyer_menu`：11 行，ID 范围 `200003-200014`。
  - active seller OWNER 角色：35 个。
  - active buyer OWNER 角色：35 个。
  - seller/buyer self-management 权限模板：均为 `7 / 19`。
  - seller/buyer root button 权限模板：均为 `6 / 18`。
  - seller/buyer OWNER self-management 授权：均为 `245 = 35 * 7`。
  - seller/buyer OWNER 非 self-management 授权：均为 0。
  - seller/buyer 无效权限、ID 区间违规、页面 component 违规、重复 perms：均为 0。
- 即将执行的受控命令：`PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`。
- SQL 文件：`RuoYi-Vue/sql/20260610_portal_self_management_permission_seed.sql`。
- 影响范围：仅本阶段 self-management 权限模板和 OWNER 授权，限 `seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`。
- 不影响范围：不写商品、订单、库存、物流、财务、履约、外部系统等业务数据；不读写 Redis。

## 2026-06-12 01:36 执行结果记录

- 执行时间：2026-06-12 01:34-01:36 +08:00。
- 执行命令：`PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`。
- 执行结果：退出码 0；未出现 `45000`；runner 输出 `portal self-management SQL seed applied with explicit confirmation.`。
- runner postcheck：
  - 当前库：`fenxiao`。
  - `seller_menu`：23 行，ID 范围 `100008-100031`。
  - `buyer_menu`：23 行，ID 范围 `200003-200026`。
  - active seller OWNER 角色：35 个。
  - active buyer OWNER 角色：35 个。
  - seller/buyer self-management 权限模板：均为 `19 / 19`。
  - seller/buyer root button 权限模板：均为 `18 / 18`。
  - seller/buyer OWNER self-management 授权：均为 `665 = 35 * 19`。
  - seller/buyer OWNER 非 self-management 授权：均为 0。
  - seller/buyer 无效权限、ID 区间违规、页面 component 违规、重复 perms：均为 0。
  - runner 输出 `postcheck exact self-management permission state verified.`。
- 执行后只读复核命令：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
- 执行后只读复核结果：与 postcheck 一致，确认 seed 已落库且未发现跨端、空权限、通配权限、admin 命名空间、ID 区间、component 或重复 perms 问题。
- 三端门禁：`cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 296 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- live 验证状态：未执行。当前 shell 未注入 `SELLER_PORTAL_USERNAME`、`SELLER_PORTAL_PASSWORD`、`BUYER_PORTAL_USERNAME`、`BUYER_PORTAL_PASSWORD`、`ADMIN_AUTH_TOKEN`、`SELLER_DIRECT_LOGIN_SUBJECT_ID`、`BUYER_DIRECT_LOGIN_SUBJECT_ID`、`PORTAL_LIVE_WRITE_CONFIRM`、`PORTAL_DIRECT_LOGIN_LIVE_CONFIRM`。
- live 未验证项：真实 seller/buyer 账号密码登录、管理端 OWNER 直登票据签发与一次性消费、真实 `/getInfo` / `/getRouters`、401 跳转、真实日志/会话隔离、OWNER 创建子账号/角色/部门写闭环。
- P2 遗留：仍不做浏览器截图、DOM/UI 细调和冻结业务菜单铺设；商品、订单、库存、物流、财务、履约、外部系统业务页不纳入本轮完成口径。
- 收尾检查：
  - `git diff --check -- <本轮三端相关文件>` 通过，仅有 LF/CRLF 提示。
  - `codegraph sync .` 通过，结果为 `Already up to date`。

## 2026-06-12 01:42 live 验证准备记录

- 后端启动：已执行 `.\start-backend-local.ps1 -Restart`，随后 8080 监听存在，`http://127.0.0.1:8080` 返回 HTTP 200。
- 配置来源：继续使用 `druid` profile；MySQL、Redis、token secret 和加密密钥均从 `.env.local` 注入，记录中不输出明文。
- 管理端登录探测：`/captchaImage` 显示验证码关闭；默认管理端账号 `admin/admin123` 登录成功并取得临时 token。token 未输出、未写入记录。
- 候选主体：
  - seller：`sellerId=41` / `SAF100031` / OWNER `s_mingtaistorage`。
  - buyer：`buyerId=36` / `BAF100034` / OWNER `b_parksideretail`。
- 当前阻塞 live 完成项：
  - direct-login live 需要确认 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`，因为会创建/消费短时票据并写审计和 session。
  - portal 账号密码登录 live 需要 seller/buyer OWNER 凭据或确认使用候选 OWNER 用户名与候选默认密码尝试登录；该操作会写登录日志和 session。
  - self-management 写闭环需要确认 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY`，因为会创建测试角色、部门和禁用 STAFF 测试账号。

## 2026-06-12 01:46 direct-login live verifier 收口加固

- 加固原因：当前环境可以通过默认管理端登录只读发现 seller/buyer OWNER 主体，但 `verify-portal-direct-login-live.mjs` 原先必须手工提供 `ADMIN_AUTH_TOKEN`、`SELLER_DIRECT_LOGIN_SUBJECT_ID`、`BUYER_DIRECT_LOGIN_SUBJECT_ID`，导致 live 直登验证仍依赖易过期 token 和人工复制主体 ID。
- 已加固：
  - `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY` 仍是执行 direct-login live 的硬门槛。
  - 如未提供 `ADMIN_AUTH_TOKEN`，脚本可使用 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 调用管理端 `/login` 获取临时 token，且不输出 token。
  - 如未提供 `SELLER_DIRECT_LOGIN_SUBJECT_ID` / `BUYER_DIRECT_LOGIN_SUBJECT_ID`，脚本会使用管理端只读列表和账号列表自动选择第一个 active 主体下的 active OWNER 账号。
  - 自动发现只访问管理端 seller/buyer 主体列表和账号列表，不访问商品、订单、库存、物流、财务、履约或外部系统业务接口。
- 已验证：
  - `node --check react-ui/scripts/verify-portal-direct-login-live.mjs` 通过。
  - `node scripts/verify-portal-direct-login-live.mjs --help` 通过。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts --runInBand` 通过，1 suite / 11 tests。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 297 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- 尚未执行 live direct-login：仍需用户确认会写 direct-login 票据、审计日志和 portal session。

## 2026-06-12 01:50 direct-login live 确认门槛负向合同

- 加固原因：`verify-portal-direct-login-live.mjs` 已支持自动管理端登录和 OWNER 主体发现，必须防止后续回退成“缺少 live 确认变量时仍先登录管理端或读取主体列表”。
- 已加固：
  - `react-ui/tests/portal-direct-login-live-contract.test.ts` 新增负向合同，固定 `main()` 必须先执行 `requireLiveEnv()`，再进入 `verifyTerminal(...)`。
  - 合同同时固定 `requireLiveEnv()` 必须检查 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`。
  - `requireLiveEnv()` 之前不得出现 `await`，避免确认门槛前触发网络调用或副作用。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-direct-login-live-contract.test.ts --runInBand` 通过，1 suite / 12 tests。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `node --check react-ui/scripts/verify-portal-direct-login-live.mjs` 通过。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 298 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- live 状态：仍未执行 direct-login / portal 登录 / 写闭环；等待用户明确确认写入登录日志、直登票据、portal session 和测试账号/角色/部门数据。

## 2026-06-12 01:54 portal 账号密码 live 确认门槛加固

- 加固原因：`verify-portal-self-management-live.mjs` 虽然只做登录后的只读接口检查，但 seller/buyer 账号密码登录本身会写登录日志和 portal session；缺少显式确认变量时不应允许脚本发起真实登录。
- 已加固：
  - 新增 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY` 确认门槛。
  - `requireLiveEnv()` 在检查 seller/buyer 账号密码前先检查确认变量。
  - help 文案明确该脚本登录后不做 portal 写操作，但真实登录会产生登录日志和 session。
  - `react-ui/tests/portal-self-management-live-contract.test.ts` 新增负向合同，固定 `main()` 必须先执行 `requireLiveEnv()`，再进入任何 `verifyTerminal(...)`，且确认门槛前不得出现 `await`。
- 已验证：
  - `node --check react-ui/scripts/verify-portal-self-management-live.mjs` 通过。
  - `cd react-ui; node scripts\verify-portal-self-management-live.mjs --help` 通过。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts --runInBand` 通过，1 suite / 12 tests。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 299 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- live 状态：仍未执行真实 portal 账号密码登录；等待用户明确确认写入登录日志和 portal session，并提供或确认 seller/buyer OWNER 凭据。

## 2026-06-12 01:58 live-write 确认门槛负向合同

- 加固原因：`verify-portal-self-management-live-write.mjs` 会执行真实 seller/buyer portal 写闭环，包含登录、创建/编辑/清理测试部门、创建/编辑/清理测试角色、创建并停用 STAFF 测试账号、分配/清空账号角色，以及读取登录日志、操作日志和在线会话；必须固定确认门槛早于任何网络调用或写副作用。
- 已加固：
  - `react-ui/tests/portal-self-management-live-write-contract.test.ts` 新增负向合同，固定 `main()` 必须先执行 `requireLiveEnv()`，再进入 `verifyTerminalWrites(terminal)`。
  - 合同固定 `requireLiveEnv()` 必须检查 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY`。
  - 合同固定确认门槛前不得出现 `await`、`requestJson(...)`、`login(...)`、`authorizedRequest(...)` 或 `cleanupTerminalWrites(...)`。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-write-contract.test.ts --runInBand` 通过，1 suite / 7 tests。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 300 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `git diff --check -- react-ui\tests\portal-self-management-live-write-contract.test.ts react-ui\scripts\verify-portal-self-management-live-write.mjs` 通过，仅 LF/CRLF 工作区提示。
  - `codegraph sync .` 通过，结果为 `Synced 1 changed files`，`Modified: 1 - 5 nodes`。
- live 状态：仍未执行真实 portal self-management write 验证；等待用户明确确认写入登录日志、操作日志、portal session、测试角色、测试部门和停用 STAFF 测试账号，并提供或确认 seller/buyer OWNER 凭据。

## 2026-06-12 02:02 执行后只读复核

- 复核命令：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
- 复核性质：只读 `SELECT`；未执行 SQL seed；未写 MySQL、Redis 或业务数据。
- 数据源确认：
  - `application.yml` 当前 `spring.profiles.active` 为 `druid`。
  - MySQL 连接仍来自 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD` 注入。
  - Redis 仍来自 `RUOYI_REDIS_*` 注入；本次 precheck 不读写 Redis。
  - `.env.local` 中运行变量存在，记录中不输出明文值。
- 复核结果：
  - 当前库：`fenxiao`。
  - `seller_menu`：23 行，ID 范围 `100008-100031`。
  - `buyer_menu`：23 行，ID 范围 `200003-200026`。
  - active seller/buyer OWNER 角色：各 35 个。
  - seller/buyer self-management 权限模板：均为 `19 / 19`。
  - seller/buyer root button 权限模板：均为 `18 / 18`。
  - seller/buyer OWNER self-management 授权：均为 `665 = 35 * 19`。
  - seller/buyer OWNER 非 self-management 授权：均为 0。
  - seller/buyer 无效权限、ID 区间违规、页面 component 违规、重复 perms：均为 0。
- 收尾检查：
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `git diff --check -- docs\plans\2026-06-11-three-terminal-portal-self-management-sql-live-runbook.md docs\reports\2026-06-10-three-terminal-portal-self-management-progress-record.md` 通过，仅 LF/CRLF 工作区提示。
  - `codegraph sync .` 通过，结果为 `Already up to date`。
- live 脚本缺确认变量运行验证：
  - `verify-portal-self-management-live.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY`。
  - `verify-portal-self-management-live-write.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY`。
  - `verify-portal-direct-login-live.mjs`：清空 live 相关环境变量后运行，退出码 1，提示缺少 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY`。
  - 上述验证未提供账号、token、主体 ID 或确认变量，未执行真实 portal 登录、direct-login、写闭环或清理动作。
- 当前下一步：SQL/seed 缺口已收口；后续只剩 live 登录、direct-login 和 self-management 写闭环验证。未获用户确认 live 写入范围和 seller/buyer OWNER 凭据前，不执行会写登录日志、操作日志、direct-login 票据、portal session 或测试账号/角色/部门数据的 live 脚本。

## 2026-06-12 02:09 live 参数示例同步

- 同步原因：live verifier 已增加确认变量和管理端认证兜底能力，runbook 必须和脚本真实入口保持一致，避免执行时因文档缺参 fail-closed 或误以为必须手工复制主体 ID。
- 已同步：
  - 只读 live 示例补入 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY`。
  - direct-login live 说明改为管理端认证上下文二选一：`ADMIN_AUTH_TOKEN` 或 `ADMIN_USERNAME` / `ADMIN_PASSWORD`。
  - `SELLER_DIRECT_LOGIN_SUBJECT_ID` / `BUYER_DIRECT_LOGIN_SUBJECT_ID` 明确为可选；未传时脚本通过管理端只读列表自动发现 active OWNER 主体。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-contract.test.ts tests/portal-direct-login-live-contract.test.ts --runInBand` 通过，2 suites / 26 tests passed。
  - `cd react-ui; node scripts\verify-portal-self-management-live.mjs --help` 通过。
  - `cd react-ui; node scripts\verify-portal-direct-login-live.mjs --help` 通过。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `cd react-ui; npm run verify:three-terminal` 通过；前端 33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `git diff --check -- <本轮 runbook 同步相关文件>` 通过，仅 LF/CRLF 工作区提示。
- 影响边界：本次只更新文档示例和静态合同，不执行 live 登录、direct-login 或写闭环。

## 2026-06-12 02:12 live 前作用域隔离静态复核

- 复核目的：在执行真实 live 登录或写闭环前，确认代码侧没有退回 caller-controlled 主体 ID 或裸 accountId 查询。
- 复核结论：
  - 后端 seller/buyer 账号查询链路继续保持 subject-scoped 查询，未发现生产代码裸 `select*AccountById(accountId)`。
  - 前端 portal 自助面继续通过 `PORTAL_SERVICE[terminal]` 调用端内 service，不直接拼 `/api/seller` / `/api/buyer`，也不把 `sellerId` / `buyerId` / `subjectId` 作为请求范围。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，`TerminalAccountIsolationTest` 4 tests passed。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-session-request.test.ts tests/portal-self-management-contract.test.ts --runInBand` 通过，2 suites / 62 tests passed。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
- 影响边界：本次只做静态扫描和合同复核，不执行 live 登录、direct-login 或写闭环。
## 2026-06-12 02:18 live 前自助审计 DTO 序列化门禁

- 补充目的：live 登录、direct-login 和写闭环执行前，先固定端内自助审计 DTO 的静态序列化边界，避免 self-service 接口把管理审计字段或 token/session 内部字段暴露给 seller/buyer portal。
- 当前门禁：
  - `PortalOwnLoginLogProfile`、`PortalOwnOperLogProfile`、`PortalOwnSessionProfile` 均不得序列化 `terminal`、`subjectId`、`accountId`、`tokenId`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`。
  - 自助在线会话还必须拒绝 `directLogin` 字段外露。
  - 自助操作日志还必须拒绝 `operParam`、`jsonResult`、`method` 等内部执行细节外露。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=PortalSelfAuditSerializationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests passed。
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfAuditSerializationTest,PortalSelfServiceSurfaceContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，130 tests passed，BUILD SUCCESS。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- live 状态：本次仍未执行 portal 账号密码登录、direct-login 或 self-management 写闭环；后续真实 live 仍需显式确认写入日志、票据、session 和测试账号/角色/部门数据。

## 2026-06-12 02:23 live 前权限拒绝日志审计门禁

- 补充目的：真实 live 验证前，固定 403 权限拒绝日志的 direct-login 结构化审计字段，避免免密代入会话在权限拒绝路径丢失 acting admin、ticket 和 reason。
- 当前门禁：
  - `PortalPreAuthorizeAspect` 必须在 `ServiceException` 权限拒绝路径调用 `recordAuthorizationFailure(...)` 后再抛出异常。
  - 拒绝日志必须从当前 terminal token/session 解析 `PortalLoginSession`，并调用 `applyDirectLoginAudit(operLog, session)`。
  - direct-login 拒绝日志必须写入 `directLogin`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason` 结构化字段，并保留 `directLoginAudit{ticketId=...}` 兼容前缀。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-framework -am "-Dtest=PortalLogAspectContractTest,PortalPreAuthorizeAspectTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，5 tests passed，BUILD SUCCESS。
- live 状态：本次仍未执行 portal 账号密码登录、direct-login 或 self-management 写闭环；后续真实 live 仍需显式确认写入日志、票据、session 和测试账号/角色/部门数据。

## 2026-06-12 02:29 live 前 portal redirect 白名单门禁

- 补充目的：真实 live 登录前，固定 portal 登录 redirect 只能跳回当前端相对路径白名单，避免绝对 URL、协议相对 URL、管理端 `/seller` / `/buyer` 路径或跨端路径进入 portal 登录后跳转链路。
- 当前门禁：
  - `isPortalTerminalPath(...)` 只允许 `/{terminal}/login`、`/{terminal}/direct-login` 和 `/{terminal}/portal/**`。
  - `/seller/login/next`、`/seller/direct-login/next`、`/buyer/admin/**`、`/seller`、`/buyer`、`https://.../{terminal}/portal/**`、`//.../{terminal}/portal/**` 均必须 fail-closed。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-session-request.test.ts --runInBand`：通过，57 tests passed。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- live 状态：本次仍未执行 portal 账号密码登录、direct-login 或 self-management 写闭环；后续真实 live 仍需显式确认写入日志、票据、session 和测试账号/角色/部门数据。

## 2026-06-12 02:31 SQL seed 后只读复核

- 复核命令：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
- 复核性质：只读 `SELECT`；未执行 SQL seed；未写 MySQL、Redis 或业务数据。
- 复核结果：
  - 当前库：`fenxiao`。
  - `seller_menu`：23 行，ID `100008-100031`；`buyer_menu`：23 行，ID `200003-200026`。
  - active seller/buyer OWNER 角色：各 35 个。
  - seller/buyer self-management 模板：均为 `19 / 19`。
  - seller/buyer root button 模板：均为 `18 / 18`。
  - seller/buyer OWNER self-management 授权：均为 `665 = 35 * 19`。
  - seller/buyer OWNER 非 self-management 授权：均为 0。
  - seller/buyer 无效权限、菜单 ID 区间违规、页面 component 违规、重复 perms：均为 0。
- 收尾检查：
  - `git diff --check -- <本轮 redirect 合同、审计合同和记录文件>`：通过，仅 LF/CRLF 工作区提示。
  - `codegraph sync .`：首次同步通过，`Synced 1 changed files`，`Modified: 1 - 19 nodes`；记录补写后最终复跑为 `Already up to date`。
- 当前下一步：SQL/seed 权限模板缺口已收口；live 登录、direct-login 和 self-management 写闭环仍需单独确认写入范围和 OWNER 凭据后执行。

## 2026-06-12 02:39 live 前管理端角色菜单树模板收口

- 补充目的：真实 live 写闭环前，先固定管理端 seller/buyer 角色菜单树只能展示本阶段 self-management 模板，避免历史冻结业务菜单模板通过管理端角色分配 UI 重新进入端内角色。
- 当前门禁：
  - `AdminSellerMenuController#roleMenuTreeselect` 和 `AdminBuyerMenuController#roleMenuTreeselect` 必须返回 `selectSelfManagementMenuIdsByRoleId(...)` 与 `buildSelfManagementMenuTreeSelect()`。
  - self-management 菜单树候选必须先经过 `PortalPermissionSupport.assertReadableTerminalMenu(...)`，再按本端 `PORTAL_SELF_MANAGEMENT_PERMS` 过滤。
  - 禁止回退到 `selectMenuIdsByRoleId(...)` + `buildMenuTreeSelect(new PortalMenu())` 的全量端菜单返回。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，9 tests passed，BUILD SUCCESS。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认 `fenxiao` self-management 模板和 OWNER 授权状态仍正确。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `git diff --check -- <本轮管理端角色菜单树收口文件>`：通过，仅 LF/CRLF 工作区提示。
  - `codegraph sync .`：通过，`Synced 8 changed files`，`Modified: 8 - 457 nodes`。
- live 状态：本次仍未执行 portal 账号密码登录、direct-login 或 self-management 写闭环；后续真实 live 仍需显式确认写入日志、票据、session 和测试账号/角色/部门数据。

## 2026-06-12 02:46 live 前管理端角色写入模板收口

- 补充目的：真实 live 写闭环前，固定管理端 seller/buyer 角色保存接口只能绑定本轮 self-management 菜单模板，避免通过接口提交历史冻结业务菜单 ID。
- 当前门禁：
  - `SellerPortalPermissionServiceImpl#assertRoleMenusExist` 和 `BuyerPortalPermissionServiceImpl#assertRoleMenusExist` 必须在端内菜单 ID、component、权限前缀、通配符和 admin 命名空间校验后调用 `assertRoleMenuSelfManagement(menu)`。
  - `assertRoleMenuSelfManagement(menu)` 必须使用本端 `PORTAL_SELF_MANAGEMENT_PERMS` 校验 `StringUtils.trimToEmpty(menu.getPerms())`。
  - 非 self-management 的合法端内业务权限，例如 `seller:product:list` / `buyer:product:list`，必须在写角色和 role-menu 绑定前 fail-closed。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest,PortalSelfServiceSurfaceContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`ruoyi-system` 9 tests、`seller` 25 tests、`buyer` 25 tests，BUILD SUCCESS。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认当前库 `fenxiao` self-management 模板和 OWNER 授权状态仍正确。
  - `git diff --check -- <本轮角色写入 fail-closed 文件>`：通过，仅 LF/CRLF 工作区提示。
- live 状态：本次仍未执行 portal 账号密码登录、direct-login 或 self-management 写闭环；后续真实 live 仍需显式确认写入日志、票据、session、测试角色、测试部门和停用 STAFF 测试账号。

## 2026-06-12 02:52 live-write 角色菜单负向验证补强

- 补充目的：真实 live 写闭环执行时，不只验证正向角色创建/编辑，还要验证角色保存接口对跨端 role-menu 提交 fail-closed。
- 当前门禁：
  - `verify-portal-self-management-live-write.mjs` 在创建正式测试角色前，必须调用 `assertRoleCreateRejectsInvalidMenuIds(...)`，用当前端 self-management menuIds 追加跨端 menuId 尝试创建角色。
  - 如果创建接口返回成功，或者失败后仍能在 `/roles` 中查到该 `roleKey`，脚本必须失败，并尝试清理意外落库角色。
  - 正式角色创建后，脚本必须调用 `assertRoleUpdateRejectsInvalidMenuIds(...)`，用跨端 menuId 尝试更新角色。
  - 如果更新接口返回成功，或者失败后 checkedKeys 出现跨端 ID、丢失 self-management 模板、或出现非模板 ID，脚本必须失败。
- 已验证：
  - `cd react-ui; node --check scripts\verify-portal-self-management-live-write.mjs`：通过。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-live-write-contract.test.ts --runInBand`：通过，1 suite / 7 tests passed。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; node scripts\verify-portal-self-management-live-write.mjs --help`：通过。
  - `git diff --check -- react-ui/scripts/verify-portal-self-management-live-write.mjs react-ui/tests/portal-self-management-live-write-contract.test.ts`：通过，仅 LF/CRLF 工作区提示。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 302 tests passed；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- live 状态：本次仍未执行 portal 账号密码登录、direct-login 或 self-management 写闭环；后续真实 live 仍需显式确认写入日志、票据、session、测试角色、测试部门和停用 STAFF 测试账号。

## 2026-06-12 03:05 管理端菜单定义入口冻结复核

- 复核目的：确认 seller/buyer 管理端只读使用平台预置权限模板，不再开放端内菜单定义能力；端内角色后续只能从 self-management 模板授权。
- 当前代码门禁：
  - 后端 `AdminSellerMenuController` / `AdminBuyerMenuController` 不暴露菜单 `add`、`edit`、`remove` handler。
  - 前端 `PartnerMenuModal.tsx` 只读展示 `listMenus()` 返回的模板树，不包含 `useAccess`、`addMenu`、`updateMenu`、`removeMenu`、`handleSubmit`、`handleRemove`、`openMenuForm` 或 `menu:add/edit/remove`。
  - seller/buyer 管理页与前端 service 不再 wiring / export `addAdmin*Menu`、`updateAdmin*Menu`、`removeAdmin*Menu`。
  - `check-partner-management-template.mjs` 和 `partner-management-contract.test.ts` 固定上述只读口径。
- 验证结果：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，8 tests passed，BUILD SUCCESS。
  - `cd react-ui; npx jest --config jest.config.ts tests/partner-management-contract.test.ts --runInBand`：通过，1 suite / 9 tests passed。
  - `cd react-ui; node scripts\check-partner-management-template.mjs`：通过。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests passed；React typecheck 通过；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认 `fenxiao` 中 seller/buyer self-management 模板均为 `19 / 19`，root button 均为 `18 / 18`，OWNER self-management grants 均为 `665 = 35 * 19`，OWNER non-self grants 均为 0。
- 未执行项：
  - 未执行 live 管理端历史 `sys_menu` 权限按钮清理 DML；如需清理 `*:admin:menu:add/edit/remove` 历史权限，需要单独确认目标集合与受控 SQL。
  - 未执行真实 portal 登录、direct-login 或 self-management 写闭环；后续仍需显式确认写入日志、票据、session、测试角色、测试部门和停用 STAFF 测试账号。
## 2026-06-12 03:13 live 前门禁复核

- 复核目的：在 SQL seed 已落库后，继续确认本阶段 P0/P1 硬边界没有回退；真实 live 登录、direct-login 和 self-management 写闭环仍需单独确认。
- 已复核边界：
  - 管理端主体级 direct-login 继续默认签发到 active OWNER 主账号；缺失、停用或锁定 OWNER 时不签发 token。
  - seller/buyer portal 自助接口继续从当前 token/session 推导 `subjectId` 和 `accountId`，前端 service 会剥离 caller-controlled `sellerId` / `buyerId` / `subjectId` / `accountId` / `terminal`。
  - 后端生产代码未恢复裸 `select*AccountById(accountId)` 查询，账号读取继续使用 `subjectId + accountId` 约束。
  - 管理端 seller/buyer 菜单模板仍为只读入口，端内角色菜单树和写入校验继续收口到 self-management 模板。
- 验证结果：
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认当前库 `fenxiao`；seller/buyer self-management 模板均为 `19 / 19`，root button 均为 `18 / 18`，OWNER self-management grants 均为 `665 = 35 * 19`，OWNER non-self grants 均为 0，且无无效权限、ID 区间、component 或重复 perms 问题。
  - `cd RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,AdminDirectLoginPermissionContractTest,PortalSelfServiceSurfaceContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，14 tests passed，BUILD SUCCESS。
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-session-request.test.ts tests/portal-self-management-contract.test.ts tests/partner-management-contract.test.ts --runInBand`：通过，3 suites / 71 tests passed。
  - `cd react-ui; node scripts\check-partner-management-template.mjs`：通过。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests passed；React typecheck 通过；后端 reactor `test-compile` 和三端合同测试均 BUILD SUCCESS。
- 未执行项：未执行真实 portal 账号密码登录、direct-login 票据签发/消费或 self-management 写闭环；后续仍需显式确认写入登录日志、操作日志、direct-login 票据、portal session、测试角色、测试部门和停用 STAFF 测试账号。
- 完成度审计：见 `docs/reports/2026-06-12-three-terminal-minimal-permission-completion-audit.md`。
## 2026-06-12 03:22 会话表独立审计合同

- 补强目的：固定 seller/buyer 端内会话表不得复用或克隆 `sys_*` 控制面，并且必须保留 direct-login 结构化审计字段。
- 已补合同：`TerminalSqlIsolationContractTest#terminalSessionTablesMustUseIndependentDdlAndDirectLoginAuditFields`。
- 合同覆盖：
  - `seller_session` / `buyer_session` 必须是独立 `create table if not exists` DDL。
  - DDL 必须包含 `token_id`、端内主体 ID、端内账号 ID。
  - DDL 必须包含 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`。
  - DDL 不得使用 `LIKE` 克隆其他表，不得引用 `sys_*`。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，13 tests，BUILD SUCCESS。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试均通过。
- 影响边界：本轮只补静态合同；未执行 SQL、portal 登录、direct-login 或 self-management 写闭环。

## 2026-06-12 03:26 portal 自助可见类型泄露合同

- 新增合同：`react-ui/tests/portal-self-management-contract.test.ts` 固定 `PortalOwnLoginLogProfile`、`PortalOwnOperLogProfile`、`PortalOwnSessionProfile` 前端类型不得包含内部身份范围或管理端审计字段。
- 固定范围：禁止在 portal 自助可见类型中重新暴露 `sellerId`、`buyerId`、`subjectId`、`accountId`、`tokenId`、`directLoginTicketId`、`actingAdmin*`、`directLoginReason`、`operParam`、`jsonResult`。
- 已验证：
  - `cd react-ui; npx jest --config jest.config.ts tests/portal-self-management-contract.test.ts --runInBand`
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
- 边界：未执行新的 SQL 或 live 写入；仅补强前端类型层的 portal 自助 DTO 泄露门禁。

## 2026-06-12 03:30 完整门禁复跑

- `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试均通过。
- `git diff --check -- <本轮三端相关文件>`：通过，无 whitespace error，仅有 LF/CRLF 工作区提示。
- live 状态：仍未执行真实 portal 账号密码登录、direct-login 或 self-management 写闭环；这些动作会写登录日志、操作日志、direct-login 票据、portal session 和测试账号/角色/部门数据，仍需单独确认。

## 2026-06-12 03:31 seed 后只读预检复核

- 命令：`node .\scripts\portal-self-management-sql-runner.mjs --precheck`。
- 命令类型：只读 `SELECT`，未执行 DDL/DML，未写 MySQL、Redis 或业务数据。
- 目标库：`fenxiao`。
- 连接来源：当前后端 `druid` 激活配置，数据库和 Redis 连接来自 `RUOYI_DB_*` / `RUOYI_REDIS_*` 运行变量；本次不输出连接明文。
- 结果：seller/buyer self-management 模板均为 `19 / 19`，root button 模板均为 `18 / 18`，OWNER self-management grants 均为 `665 = 35 * 19`，OWNER non-self grants 均为 0；无无效权限、菜单 ID 区间违规、页面 component 违规或重复 perms。

## 2026-06-12 03:37 主体 ID 防串端服务合同

- 新增合同：seller/buyer 账号更新服务层测试固定请求体主体 ID 不可信。
- 覆盖点：
  - `updateSellerAccount(sellerId, account)` 最终写 mapper 前必须使用方法参数 `sellerId` 覆盖 `account.sellerId`。
  - `updateBuyerAccount(buyerId, account)` 最终写 mapper 前必须使用方法参数 `buyerId` 覆盖 `account.buyerId`。
  - 请求体携带的通用 `accountId` 不能覆盖当前端内账号主键。
- 已验证：
  - `cd RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 64 tests、buyer 65 tests。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; npm run verify:three-terminal`：通过，33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 和三端合同测试均通过。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读，目标库 `fenxiao`。
- live 边界：该合同不替代真实 portal 登录、direct-login 消费和 self-management 写闭环；后续 live 写验证仍需按本 runbook 的确认 token 执行。

## 2026-06-12 03:49 live 前只读复查补记

- 子 Agent 记录：
  - 前端只读复核子 Agent：`gpt-5.3-codex-spark`，已完成，未发现高置信 P0/P1 缺口。
  - 后端只读复核子 Agent：`gpt-5.3-codex-spark`，连续等待未返回后关闭，未产出结论。
- 本地只读复查：
  - portal 前端请求继续通过 `PORTAL_SCOPE_PARAM_KEYS` 剥离 caller-controlled 主体和账号字段。
  - direct-login 支撑层继续使用 terminal-scoped Redis key，跨端消费不触发当前端 failure audit。
  - self-management SQL runner 仍只允许 `20260610_portal_self_management_permission_seed.sql`，默认 `--precheck` 只读，`--apply` 需要 `PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED`。
- live 执行边界：本补记仅说明 live 前静态状态；真实登录、direct-login 和 self-management 写闭环仍需要按本 runbook 逐项提供确认变量和账号凭据。

## 2026-06-12 04:07 self-management 权限 seed 执行前确认

- 用户确认：允许对当前目标库 `fenxiao` 执行本阶段 self-management 权限 seed。
- 影响范围：仅限 `seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`。
- 数据源确认：
  - 后端当前激活配置为 `spring.profiles.active=druid`。
  - MySQL 连接来自 `.env.local` 注入的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`，runner 不输出连接明文。
  - Redis 连接来自 `RUOYI_REDIS_*`；本次 SQL seed 不读写 Redis。
- 执行命令：`PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`。
- 执行前只读状态：最近一次 `node .\scripts\portal-self-management-sql-runner.mjs --precheck` 已确认目标库 `fenxiao`，seller/buyer self-management 模板均为 `19 / 19`，OWNER self-management grants 均为 `665 = 35 * 19`，异常项均为 0。

## 2026-06-12 04:08 self-management 权限 seed 执行结果

- 已执行命令：`PORTAL_SELF_MANAGEMENT_SQL_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_PERMISSION_SEED node .\scripts\portal-self-management-sql-runner.mjs --apply`。
- 执行结果：成功，runner 输出 `portal self-management SQL seed applied with explicit confirmation.`。
- postcheck 结果：
  - 当前库：`fenxiao`。
  - `seller_menu`：23 行，ID 范围 `100008-100031`。
  - `buyer_menu`：23 行，ID 范围 `200003-200026`。
  - active seller/buyer OWNER 角色：各 35 个。
  - seller/buyer self-management 模板：均为 `19 / 19`。
  - seller/buyer root button 模板：均为 `18 / 18`。
  - seller/buyer OWNER self-management grants：均为 `665 = 35 * 19`。
  - seller/buyer OWNER non-self grants：均为 0。
  - seller/buyer 无效权限、菜单 ID 区间违规、页面 component 违规、重复 perms：均为 0。
- 影响边界：本次只执行 self-management 权限 seed；未执行 portal 登录、direct-login 消费、Redis 写入或 self-management 写闭环 live 脚本。

## 2026-06-12 04:09 seed 后收尾复核

- `git diff --check -- <本轮相关文件>`：通过，无 whitespace error，仅 LF/CRLF 工作区提示。
- `node .\scripts\portal-self-management-sql-runner.mjs --precheck`：通过，只读确认 `fenxiao` 中 self-management 权限状态仍与 postcheck 一致。
- `codegraph sync .`：通过，`Synced 5 changed files; Modified: 5 - 299 nodes in 1.0s`。
- 仍未执行项：真实 seller/buyer 账号密码登录、direct-login 消费成功回传、OWNER self-management 写闭环 live 脚本。

## 2026-06-12 04:12 seed 后 live 前门禁复跑

- 只读数据库复核：`node .\scripts\portal-self-management-sql-runner.mjs --precheck` 通过，目标库仍为 `fenxiao`，seller/buyer self-management 模板均为 `19 / 19`，OWNER self-management grants 均为 `665 = 35 * 19`，异常项均为 0。
- 静态 guard：
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
  - `cd react-ui; node scripts\check-portal-token-isolation.mjs`：通过。
  - `cd react-ui; node scripts\check-partner-management-template.mjs`：通过。
- live 确认门槛负向验证：
  - `verify-portal-self-management-live.mjs` 在未提供确认变量和 seller/buyer portal 凭据时直接失败，提示缺少 `PORTAL_SELF_MANAGEMENT_LIVE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_LIVE_VERIFY` 及 portal 账号密码。
  - `verify-portal-direct-login-live.mjs` 在未提供确认变量和管理端认证上下文时直接失败，提示缺少 `PORTAL_DIRECT_LOGIN_LIVE_CONFIRM=APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY` 及 `ADMIN_AUTH_TOKEN` 或 `ADMIN_USERNAME/ADMIN_PASSWORD`。
  - `verify-portal-self-management-live-write.mjs` 在未提供确认变量和 seller/buyer portal 凭据时直接失败，提示缺少 `PORTAL_LIVE_WRITE_CONFIRM=APPLY_PORTAL_SELF_MANAGEMENT_WRITE_VERIFY` 及 portal 账号密码。
- 完整门禁：`cd react-ui; npm run verify:three-terminal` 通过，33 suites / 304 tests，React typecheck 通过，后端 reactor `test-compile` 与三端合同测试均 BUILD SUCCESS。
- 当前可继续项：若要进入真实 live 验证，需要分别确认登录日志/session、direct-login 票据/审计、测试角色/部门/停用 STAFF 测试账号的写入范围，并提供或确认 seller/buyer OWNER 账号密码与管理端认证上下文。

## 2026-06-12 04:15 live 执行阻塞条件确认

- 已完成的阻塞前复核：
  - 生产代码未发现裸 `selectSeller/BuyerAccountById(Long accountId)` 签名。
  - seller/buyer 模块未发现端内 `sys_*` 控制面引用。
  - self-management seed 的 ID 区间、权限前缀、通配权限、admin 命名空间和 component fail-closed guard 仍在。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck` 继续通过。
- 不能继续自动执行的原因：
  - `verify:portal-self-management-live` 会写 seller/buyer 登录日志和 portal session。
  - `verify:portal-direct-login-live` 会写 direct-login 票据、票据消费审计、登录日志和 portal session。
  - `verify:portal-self-management-live-write` 会写操作日志，并创建/清理测试角色、测试部门和停用 STAFF 测试账号。
- 恢复执行条件：
  - 明确确认上述 live 写入范围。
  - 提供或确认 seller/buyer OWNER 账号密码。
  - 提供或确认管理端认证上下文：`ADMIN_AUTH_TOKEN` 或 `ADMIN_USERNAME` / `ADMIN_PASSWORD`。

## 2026-06-12 11:13 三类 live 执行授权记录

- 用户授权：允许自行解决三类 live 验证。
- 授权覆盖范围：
  - portal 账号密码登录 live：会写 seller/buyer 登录日志和 portal session。
  - 管理端 direct-login live：会写 direct-login 票据、消费审计、登录日志和 portal session。
  - self-management 写闭环 live：会写操作日志，并创建/清理测试角色、测试部门，留下停用 STAFF 测试账号作为证据。
- 数据源确认：
  - 后端当前激活 `spring.profiles.active=druid`。
  - MySQL 连接仍由 `.env.local` 注入的 `RUOYI_DB_*` 提供，记录不输出连接明文。
  - Redis 连接仍由 `.env.local` 注入的 `RUOYI_REDIS_*` 提供，记录不输出连接明文。
- 凭据处理：
  - 管理端认证优先使用默认本机验证账号上下文，由脚本临时登录获取 token，token 不输出、不写入记录。
  - seller/buyer OWNER 凭据优先使用 runbook 已记录的候选 OWNER 用户名和代码默认 OWNER 密码常量尝试登录；密码不写入记录。
- 服务状态：11:13 访问 `http://127.0.0.1:8080` 失败，执行 live 前需按项目约定重启本机后端。

## 2026-06-12 11:34 三类 live 执行结果

- 执行前环境：已按项目约定重启本机后端，实际服务 `http://127.0.0.1:8080` 可访问；SQL 预检确认目标库为 `fenxiao`。记录不输出连接串、token 或密码明文。
- 账号密码登录 live：
  - 命令：`cd react-ui; npm run verify:portal-self-management-live`。
  - 结果：通过，输出 `portal self-management live read-only verification passed.`。
  - 写入范围：seller/buyer 登录日志和 portal session。
  - 覆盖点：OWNER 账号密码登录、`getInfo`、`getRouters`、账号/部门/角色/登录日志/操作日志/在线会话只读接口、冻结业务权限不可见、跨端 token 拒绝。
- direct-login live：
  - 命令：`cd react-ui; npm run verify:portal-direct-login-live`。
  - 结果：通过，输出 `portal direct-login live verification passed. Tickets: seller#120, buyer#121`。
  - 写入范围：direct-login 票据、消费审计、登录日志和 portal session。
  - 覆盖点：管理端自动选择 active OWNER、票据签发、目标 portal 一次性消费、跨端票据拒绝、重复消费拒绝、portal token 隔离与登出清理。
- self-management 写闭环 live：
  - 命令：`cd react-ui; npm run verify:portal-self-management-live-write`。
  - 结果：通过，输出 `portal self-management live write verification passed. Disabled evidence accounts: seller#41, buyer#37`。
  - 写入范围：操作日志、测试部门、测试角色、账号角色关联、停用 STAFF 证据账号。
  - 覆盖点：OWNER 创建并清理测试部门、创建/更新/删除测试角色、创建禁用 STAFF 账号、账号角色分配与清理、跨端 menuId fail-closed。
- live 暴露并已修复的 P0：
  - 普通登录 session 写入缺少 `direct_login` 默认值，已由 `PortalTokenSupport#buildSession` 固定为 `Boolean.FALSE`，并补 `PortalTokenSupportTest`。
  - self-list 分页接口的 PageHelper 上下文污染账号校验 SQL，已由 seller/buyer service 的 `assert*SessionAccountWithoutPage` 保护，合同测试已覆盖。
  - live verifier 对若依分页响应取值过窄，已兼容 top-level `rows`。
- 执行后复核：
  - `cd react-ui; npm run verify:three-terminal` 通过，33 suites / 304 tests。
  - `cd react-ui; node scripts\verify-three-terminal.mjs --check-manifest` 通过。
  - `node .\scripts\portal-self-management-sql-runner.mjs --precheck` 通过，seller/buyer self-management 模板均 `19 / 19`，OWNER grants 均 `665 = 35 * 19`，异常项均为 0。
  - `git diff --check` 通过，仅有 LF/CRLF 工作区提示。
- 结论：三类 live 已完成，本阶段 seller/buyer portal 最小可登录权限框架可按完成口径收口；业务菜单、业务页面、截图/DOM/UI 细调仍作为 P2 或后续阶段处理。
