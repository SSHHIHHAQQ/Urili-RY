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

执行 seed 后，可先用只读脚本验证账号密码登录、`getInfo`、`getRouters`、自助列表接口、冻结业务权限不可见和跨端 token 拒绝。该脚本不会读取 `.env.local`，不会执行 SQL，也不会做新增账号、角色、部门等写操作。

```powershell
cd E:\Urili-Ruoyi\react-ui
$env:PORTAL_LIVE_BASE_URL = 'http://127.0.0.1:8080'
$env:SELLER_PORTAL_USERNAME = '<seller owner username>'
$env:SELLER_PORTAL_PASSWORD = '<seller owner password>'
$env:BUYER_PORTAL_USERNAME = '<buyer owner username>'
$env:BUYER_PORTAL_PASSWORD = '<buyer owner password>'
npm run verify:portal-self-management-live
```

如果缺少任一账号密码环境变量，脚本会 fail-closed 并列出缺失变量；不得把明文账号密码写入报告。

### 受控写脚本验证

只读脚本通过后，可以用受控写脚本验证 OWNER 在 seller/buyer portal 内创建子账号、维护部门、维护角色、分配角色、查看自助审计 DTO 的最小闭环。该脚本默认不会运行写操作，必须显式提供确认变量。

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
- OWNER 能创建、编辑、删除测试部门。
- OWNER 能创建、编辑、删除测试角色，且角色授权只来自 self-management 菜单模板。
- OWNER 能创建停用 STAFF 子账号，并给该子账号分配、清空测试角色。
- 自助登录日志、操作日志、在线会话 DTO 不暴露内部审计字段。

### 人工运行态验证

脚本验证后，再人工验证脚本无法覆盖或不适合自动化写入的运行态场景。

| 场景 | 验证方式 | 通过标准 |
| --- | --- | --- |
| seller OWNER 账号密码登录 | 调用 seller portal 登录接口或浏览器登录 `/seller/login` | 返回 seller token，Redis/session 端标识为 seller |
| buyer OWNER 账号密码登录 | 调用 buyer portal 登录接口或浏览器登录 `/buyer/login` | 返回 buyer token，Redis/session 端标识为 buyer |
| 管理端直登 seller OWNER | 管理端对 seller 主体发起 direct-login | 目标 portal 消费成功并回传 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 成功 |
| 管理端直登 buyer OWNER | 管理端对 buyer 主体发起 direct-login | 目标 portal 消费成功并回传成功 |
| seller `/getInfo` / `/getRouters` | 使用 seller token 调用 | 只返回 seller self-management 权限和路由，不返回 product/order 等冻结业务权限 |
| buyer `/getInfo` / `/getRouters` | 使用 buyer token 调用 | 只返回 buyer self-management 权限和路由 |
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

- `verify:portal-self-management-live`：只读验证账号密码登录、匿名 `/getInfo` 拒绝、`getInfo` / `getRouters` 自助权限收敛、只读自助接口和跨端 token 拒绝。
- `verify:portal-self-management-live-write`：需要写确认变量，验证 OWNER 创建子账号、角色授权、部门维护、日志和会话自助 DTO 脱敏。
- `verify:portal-direct-login-live`：需要管理端 token、目标主体 ID 和写确认变量，验证管理端到 OWNER 的 direct-login ticket 生成、消费、同一 ticket 不可复用、消费后 portal `/getInfo` 权限收敛。

live 脚本默认直连后端 `http://127.0.0.1:8080`，请求路径不带 `/api`。如果改为通过 React dev server `http://127.0.0.1:8001` 验证，必须显式设置：

```powershell
$env:PORTAL_LIVE_BASE_URL = 'http://127.0.0.1:8001'
$env:PORTAL_LIVE_API_PREFIX = '/api'
```

管理端直登 live 验证还需要显式传入当前已登录管理端 token 和目标主体 ID，并设置写确认变量：

```powershell
$env:ADMIN_AUTH_TOKEN = '<current-admin-token>'
$env:SELLER_DIRECT_LOGIN_SUBJECT_ID = '<seller-id>'
$env:BUYER_DIRECT_LOGIN_SUBJECT_ID = '<buyer-id>'
$env:PORTAL_DIRECT_LOGIN_LIVE_CONFIRM = 'APPLY_PORTAL_DIRECT_LOGIN_LIVE_VERIFY'
npm run verify:portal-direct-login-live
```

如执行过程中产生任何代码或 seed 修改，还需复跑相关 Maven/Jest 窄测，并在报告中写明。

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
