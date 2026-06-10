# 端内 Portal 首页菜单 seed 远端库执行记录

日期：2026-06-10

## 用户确认

- 当前任务处于三端独立账号权限改造的快速推进模式。
- 用户已确认以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向推进实现。
- 本次执行只适用于 `RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql`，不外推到其他 SQL 重放。

## 数据源确认

- 激活配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
- 激活 profile：`druid`
- MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 连接变量来源：本机 `.env.local` 的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`
- 目标环境：远端 MySQL
- 目标库：`fenxiao`
- 敏感信息处理：不记录 JDBC URL 全文、账号、密码、Redis 密码或 token secret
- Redis：本次 SQL 不读取、不写入 Redis

## 执行脚本

- 首页菜单 seed：`RuoYi-Vue/sql/20260610_terminal_portal_home_menu_seed.sql`
- 追加 P1 收敛：`RuoYi-Vue/sql/20260608_terminal_menu_auto_increment_reset.sql`

## 执行范围

- 执行命令类型：
  - `20260610_terminal_portal_home_menu_seed.sql`：DML-only terminal menu seed
  - `20260608_terminal_menu_auto_increment_reset.sql`：DDL-only auto_increment reset
- 影响表：
  - `seller_menu`
  - `buyer_menu`
  - `seller_role_menu`
  - `buyer_role_menu`
- 影响语义：
  - 新增或确认卖家端首页 `C` 菜单：`seller:portal:home`
  - 新增或确认买家端首页 `C` 菜单：`buyer:portal:home`
  - 给当前启用的 seller/buyer 端内 `owner` 角色补齐首页菜单授权
- 不包含：
  - 首页菜单 seed 不做 DDL
  - auto_increment reset 只调整 `seller_menu` / `buyer_menu` 自增游标，不新增字段、索引或业务表
  - 不修改 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`
  - 不修改业务事实数据、订单、库存、财务流水
  - 不写 Redis
  - 不触发外部系统

## SQL 确认 token

- 首页菜单 seed 执行前必须设置：

```sql
set @confirm_terminal_portal_home_menu_seed = 'APPLY_TERMINAL_PORTAL_HOME_MENU_SEED';
```

- 脚本入口 `assert_terminal_portal_home_menu_seed_confirmed()` 会在 token 缺失或错误时 `45000` fail-closed。
- 脚本还会在端内菜单 ID 越界、端前缀错误、跨端权限、管理端权限命名空间、通配权限、页面组件为空、页面组件不在本端根路径、权限重复、菜单 slot 签名冲突、owner 授权不完整时 `45000` fail-closed。

- auto_increment reset 执行前设置：

```sql
set @confirm_terminal_menu_auto_increment_reset = 'APPLY_TERMINAL_MENU_AUTO_INCREMENT_RESET';
set @terminal_menu_auto_increment_seller_expected_count = '11';
set @terminal_menu_auto_increment_seller_expected_signature = 'a252e155976be048e08a0b61d646c9a2a7f398d6ec06e74ce95b58e3b45aaee4';
set @terminal_menu_auto_increment_buyer_expected_count = '11';
set @terminal_menu_auto_increment_buyer_expected_signature = 'e111b4d2ca89fb03fdbc6b40393c1e3b562a48663a21eb374128b1abab911b8f';
```

- auto_increment reset 会在确认 token、全表 count/signature、ID 区间、父子关系、role-menu 关系不满足预期时 `45000` fail-closed。

## 执行前预览

- 预览时间：2026-06-10
- 执行通道：JShell + JDBC，只读查询；未输出数据库密钥
- 当前目标集合：
  - `seller_home_menu_count = 0`
  - `buyer_home_menu_count = 0`
  - `seller_home_owner_grant_count = 0`
  - `buyer_home_owner_grant_count = 0`
  - `seller_home_existing_signature = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`
  - `buyer_home_existing_signature = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`
- 当前启用 owner 角色：
  - `seller_owner_role_count = 3`
  - `buyer_owner_role_count = 1`
- 执行后预期：
  - 新增或确认 `seller:portal:home` 页面菜单 1 条
  - 新增或确认 `buyer:portal:home` 页面菜单 1 条
  - seller owner 首页授权数等于 3
  - buyer owner 首页授权数等于 1

## 执行通道

- 本机未使用 `mysql` CLI。
- 本次采用本机 Maven 缓存中的 `mysql-connector-j`，通过 JShell/JDBC 执行迁移脚本。
- SQL 文件包含 `delimiter` 和存储过程，执行器按 delimiter 拆分后逐句执行。
- 执行通道未输出 `.env.local` 明文内容。

## 执行结果

- 首页菜单 seed：执行成功。
  - 执行语句数：`27`
  - 新增/确认 `seller:portal:home`
  - 新增/确认 `buyer:portal:home`
  - 给当前启用 owner 角色补齐首页菜单授权
- auto_increment reset：最终执行成功。
  - 第一次执行失败：远端 MySQL 不接受 `set session group_concat_max_len = greatest(...)`，失败发生在任何 DDL 前。
  - 修正：将 `group_concat_max_len` 赋值改为固定值 `1048576`。
  - 第二次执行失败：`information_schema.tables.AUTO_INCREMENT` 后置断言读取到旧值，`SHOW CREATE TABLE` 已显示 DDL 生效；属于元数据口径误报。
  - 修正：保留 `information_schema` 的空值和区间上界保护，不再用其短暂低于 `SHOW CREATE TABLE` 的值判定失败。
  - 最终执行语句数：`51`

## 执行后核验

- 核验时间：2026-06-10
- 核验通道：JShell + JDBC，只读查询；未输出数据库密钥
- 首页菜单：
  - `seller_home_menu_count = 1`
  - `buyer_home_menu_count = 1`
  - `seller_home_owner_grant_count = 3`
  - `buyer_home_owner_grant_count = 1`
- 当前启用 owner 角色：
  - `seller_owner_role_count = 3`
  - `buyer_owner_role_count = 1`
- 端内菜单完整性：
  - `seller_menu_out_of_range = 0`
  - `buyer_menu_out_of_range = 0`
  - `seller_menu_invalid_perms = 0`
  - `buyer_menu_invalid_perms = 0`
  - `seller_page_blank_or_cross_component = 0`
  - `buyer_page_blank_or_cross_component = 0`
  - `seller_role_menu_orphans = 0`
  - `buyer_role_menu_orphans = 0`
- 新增菜单签名：
  - `seller_menu_id = 100019`，`menu_name = 卖家端首页`，`parent_id = 0`，`order_num = 1`，`path = /seller/portal`，`component = Seller/Portal/index`，`route_name = SellerPortalHome`，`menu_type = C`，`perms = seller:portal:home`
  - `buyer_menu_id = 200014`，`menu_name = 买家端首页`，`parent_id = 0`，`order_num = 1`，`path = /buyer/portal`，`component = Buyer/Portal/index`，`route_name = BuyerPortalHome`，`menu_type = C`，`perms = buyer:portal:home`
- 自增游标：
  - `seller_menu max = 100019`，`SHOW CREATE TABLE AUTO_INCREMENT = 100020`
  - `buyer_menu max = 200014`，`SHOW CREATE TABLE AUTO_INCREMENT = 200015`

## 回滚方式

- 如果需要回滚，必须先确认 seller/buyer portal 不依赖首页远程菜单。
- 回滚时只处理本次新增的 terminal menu 和 role-menu 关系：
  - 删除 `seller_role_menu` 中指向 `seller:portal:home` 菜单的授权。
  - 删除 `buyer_role_menu` 中指向 `buyer:portal:home` 菜单的授权。
  - 删除 `seller_menu.perms = 'seller:portal:home'` 且签名匹配的页面菜单。
  - 删除 `buyer_menu.perms = 'buyer:portal:home'` 且签名匹配的页面菜单。
- 不得粗暴清理其他 seller/buyer 端内菜单、角色、账号或权限。
- auto_increment reset 不建议单独回滚；如果删除本次新增菜单，后续仍应让 `seller_menu` / `buyer_menu` 自增游标保持在各自区间内且高于现存最大 ID。

## 边界说明

- 本次不启动或重启后端。
- 本次不做浏览器、截图、DOM 或 UI 细调验收。
- 本次不读取或写入 Redis。
