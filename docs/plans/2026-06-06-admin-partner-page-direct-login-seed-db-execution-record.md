# 管理端卖家/买家页面与免密权限 seed 远程库执行记录

日期：2026-06-06

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向。

上一检查点新增了 `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql`，用于补齐独立增量 seed 场景下的管理端卖家/买家页面基础权限和免密登录权限。本轮按已确认的远程数据库 DDL/DML 执行边界，将该 seed 写入当前远程运行库。

## 数据源确认

- 后端配置基线：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 激活 `druid`。
- MySQL 配置来源：当前后端运行变量链路读取本机 `.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- 数据源类型：远程 MySQL，非 `localhost` / `127.0.0.1`。
- Redis：本轮不读取、不写入 Redis。
- 本机 `mysql` CLI 不在 PATH 中；本轮使用 Maven 本地缓存的 `mysql-connector-j` JDBC 驱动和 `jshell` 执行。
- 本记录不保存数据库连接串、用户名、密码、Redis 密码或 token secret。

## 执行范围

执行 SQL：

- `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql`

影响表：

- `sys_menu`

写入或更新的目标菜单：

| menu_id | perms | 说明 |
| --- | --- | --- |
| 2011 | `seller:admin:list` | 管理端卖家管理页面 |
| 2012 | `buyer:admin:list` | 管理端买家管理页面 |
| 2205 | `seller:admin:directLogin` | 管理端卖家免密登录按钮权限 |
| 2215 | `buyer:admin:directLogin` | 管理端买家免密登录按钮权限 |

SQL 为幂等 `ON DUPLICATE KEY UPDATE`，重复执行不会产生重复菜单。

## 执行结果

- 执行前目标 `menu_id in (2011, 2012, 2205, 2215)` 计数：`4`。
- 本轮显式执行 `set names utf8mb4` 后重放 seed。
- 执行后目标权限行数：`4`。
- `sys_menu` 中目标权限计数：`4`。

最终远程库校验：

| menu_id | parent_id | path | component | menu_type | perms |
| --- | --- | --- | --- | --- | --- |
| 2011 | 2010 | `seller` | `Seller/index` | `C` | `seller:admin:list` |
| 2012 | 2010 | `buyer` | `Buyer/index` | `C` | `buyer:admin:list` |
| 2205 | 2011 | `#` |  | `F` | `seller:admin:directLogin` |
| 2215 | 2012 | `#` |  | `F` | `buyer:admin:directLogin` |

中文字段字节校验：

- `2011.menu_name` HEX：`E58D96E5AEB6E7AEA1E79086`
- `2012.menu_name` HEX：`E4B9B0E5AEB6E7AEA1E79086`
- `2205.menu_name` HEX：`E58D96E5AEB6E5858DE5AF86E799BBE5BD95`
- `2215.menu_name` HEX：`E4B9B0E5AEB6E5858DE5AF86E799BBE5BD95`

上述 HEX 对应 UTF-8 中文菜单名，JShell/PowerShell 控制台显示层乱码不影响数据库真实字节。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dtest=AdminDirectLoginPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

验证结果：

- `AdminDirectLoginPermissionContractTest` 通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- Maven 构建结果：`BUILD SUCCESS`。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 边界说明

- 本轮没有执行 DDL。
- 本轮没有写入 `sys_role_menu`；当前 seed 只补 `sys_menu` 页面和按钮权限点。
- 本轮没有启动浏览器，没有做截图/DOM/UI 细调。
- 本轮没有重启后端；菜单缓存如需立即刷新，后续可按运行环境需要重启或清理相关缓存。
