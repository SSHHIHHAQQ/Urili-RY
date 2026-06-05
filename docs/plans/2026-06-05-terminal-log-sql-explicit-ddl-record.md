# 端内日志 SQL 独立 DDL 守卫记录

## 背景

本记录服务于 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的三端独立改造目标。当前方向要求 seller/buyer 端账号、权限、菜单、部门、登录日志、操作日志和会话体系独立于若依后台 `sys_*` 控制面。

审计发现：`RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` 中端内日志表仍使用：

- `create table if not exists seller_login_log like sys_logininfor`
- `create table if not exists buyer_login_log like sys_logininfor`
- `create table if not exists seller_oper_log like sys_oper_log`
- `create table if not exists buyer_oper_log like sys_oper_log`

这属于旧迁移脚本的历史痕迹，不是当前 Java 运行时混用，但会让三端日志表基线继续依赖若依后台日志表模板。

本轮不执行远程数据库 DDL/DML，不连接远程 MySQL / Redis。

## 已处理

- 将 `20260604_three_terminal_isolation_migration.sql` 中四张端内日志表改为显式独立 DDL。
- 保留对已有运行库表的兼容补齐：
  - `add_column_if_missing(...)` 继续补 `seller_id` / `buyer_id` 与端账号 ID。
  - `add_index_if_missing(...)` 补端内主体/账号与时间字段索引。
- 新增 `TerminalSqlIsolationContractTest`，扫描三端隔离迁移脚本和综合 seed，禁止 seller/buyer 端内日志表再次通过 `LIKE sys_*` 生成。
- 更新 `docs/architecture/reuse-ledger.md`，登记端内日志 SQL 独立 DDL 规则。
- 更新 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`，增加本检查点。

## 验证命令

```powershell
cd E:\Urili-Ruoyi
rg -n "create table if not exists (seller|buyer)_(login|oper)_log like sys_|like sys_logininfor|like sys_oper_log" RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql RuoYi-Vue/sql/seller_buyer_management_seed.sql
```

结果：无命中。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -Dtest=TerminalSqlIsolationContractTest test
```

结果：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest" test
```

结果：通过，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。

## 当前判断

- seller/buyer 端内登录日志和操作日志的 SQL 基线已经从若依后台日志表模板派生改为显式独立建表。
- 当前运行时 Java 代码本来已经写入 `seller_login_log` / `buyer_login_log` / `seller_oper_log` / `buyer_oper_log`，本轮只收敛 SQL 基线和守卫。
- 迁移脚本中 `sys_user` 历史字段回填仍属于旧混用账号迁出兼容块，本轮未删除；如需彻底拆出 legacy migration，应另开切片评估历史库兼容和回滚方式。
