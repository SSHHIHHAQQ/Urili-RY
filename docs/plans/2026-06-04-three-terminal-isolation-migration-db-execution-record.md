# 三端隔离主迁移 SQL 远程库执行回溯记录

日期：2026-06-04
补记日期：2026-06-09

## 记录性质

本文件是回溯补齐的数据库执行记录，用于补强三端隔离主迁移的证据链。

- 本补记不代表 2026-06-09 重新执行了 SQL。
- 本补记未执行 DDL/DML。
- 本补记未读取或写入 Redis。
- 本补记依据既有目标追踪、后续只读核验记录和当前 SQL/合同测试证据整理。

## 背景

本记录服务于 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

三端隔离目标为：

- 管理端继续保留若依 `sys_*` 后台体系。
- 卖家端、买家端账号、密码、角色、菜单、部门、登录日志、操作日志和会话独立。
- 卖家端/买家端运行态不得继续复用 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 作为端内控制面。

## 数据源确认

历史目标追踪记录显示，本次执行使用本机 `.env.local` 中的 `RUOYI_DB_*` 连接远程 MySQL；记录未输出数据库密码、Redis 密码或 token secret。

后续只读核验记录确认当前三端隔离核验目标为远程 MySQL，连接来源为本机 `.env.local`，目标地址已脱敏。本补记不记录任何连接地址或凭证。

## 执行脚本

- `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`

## 历史执行结果

既有目标追踪已记录：

- 远程库已执行 `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`。
- 执行来源为本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行结果为迁移脚本 82 条语句成功。
- 当时表结构校验显示 14 张端内核心表存在。
- 当时账号字段校验显示 `seller_account` / `buyer_account` 旧 `user_id` 列数量为 0。
- 当时数据量辅助校验显示 `seller_account = 3`、`buyer_account = 1`。

## 迁移范围

该主迁移脚本覆盖三端独立第一批核心结构，包括：

- `seller` / `buyer` 主体表。
- `seller_account` / `buyer_account` 端账号表。
- `seller_role` / `buyer_role` 端内角色表。
- `seller_menu` / `buyer_menu` 端内菜单表。
- `seller_dept` / `buyer_dept` 端内部门表。
- `seller_account_role` / `buyer_account_role` 端账号角色关系表。
- `seller_role_menu` / `buyer_role_menu` 端角色菜单关系表。
- `seller_login_log` / `buyer_login_log` 端登录日志表。
- `seller_oper_log` / `buyer_oper_log` 端操作日志表。
- `seller_session` / `buyer_session` 端会话表。
- `portal_direct_login_ticket` 免密代入票据表。

## 后续只读核验证据

后续远程库只读核验记录显示：

- 三端独立核心表存在：`21/21`，缺失列表为空。
- `seller_account.user_id` 不存在。
- `buyer_account.user_id` 不存在。
- `seller_account` / `buyer_account` 关键字段完整，包括端账号 ID、主体 ID、`user_name`、`password`、`account_role`、`status`、`lock_status`。
- `seller_login_log` / `buyer_login_log`、`seller_oper_log` / `buyer_oper_log`、`seller_session` / `buyer_session` 关键字段完整。
- `seller_account.uk_seller_account_username`、`buyer_account.uk_buyer_account_username`、`seller_account.uk_seller_account_owner`、`buyer_account.uk_buyer_account_owner` 等关键索引存在。
- 后续 P0/P1 只读复核进一步确认：
  - `seller_account.password` / `buyer_account.password` 均为 `varchar(100) not null` 且无默认值。
  - 两端账号空密码行数均为 0。
  - `seller_menu` / `buyer_menu` 菜单 ID 区间和 `auto_increment` floor 正确。
  - 端内菜单 `C/F` 权限非空、前缀正确、无 `*:admin:*`、无跨端前缀、无 `*` 通配。
  - 端内菜单父子关系、role-menu、account-role 均无孤儿。

## 合同测试证据

当前仓库已有以下合同测试固定迁移边界：

- `TerminalAccountIsolationTest`
  - 固定 seller/buyer 模块不得引用 `sys_user`、`sys_role`、`sys_menu`、`sys_dept` 作为端内控制面。
- `TerminalSqlIsolationContractTest`
  - 固定主三端隔离迁移脚本不得通过 `LIKE sys_logininfor` / `LIKE sys_oper_log` 派生端内日志表。
  - 固定主三端隔离迁移脚本不得从 `sys_user` 回填 seller/buyer 端账号。
- `SqlExecutionGuardContractTest`
  - 固定高影响 SQL 的确认门禁、dynamic DDL helper、完成态断言和相关 fail-closed 约束。

## 边界说明

- 本补记没有重新执行 `20260604_three_terminal_isolation_migration.sql`。
- 本补记没有执行 `CREATE` / `ALTER` / `DROP` / `INSERT` / `UPDATE` / `DELETE`。
- 本补记没有读取或写入 Redis。
- 本补记没有启动或重启后端。
- 本补记没有做浏览器、截图、DOM 或 UI 细调验收。

## 结论

现有证据链可以支撑结论：`20260604_three_terminal_isolation_migration.sql` 已按三端隔离方向写入远程运行库；后续只读核验和合同测试均未发现该主迁移在账号、角色、菜单、部门、日志、会话独立性上的 P0/P1 缺口。
