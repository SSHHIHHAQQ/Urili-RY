# 端内登录日志免密审计字段远程库执行记录

记录时间：2026-06-07 01:08 +08:00

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 执行范围

本轮只回放一个已确认的三端 P0/P1 增量 SQL：

- `RuoYi-Vue/sql/20260607_terminal_login_log_direct_login_audit.sql`

目的：让当前远程运行库的 `seller_login_log` / `buyer_login_log` 与代码、mapper、fresh seed 和 contract test 对齐，避免免密登录成功或失败写登录日志时因缺少 `direct_login_*` / `acting_admin_*` 字段导致运行态失败。

## 数据源确认

- 后端激活配置：当前若依后端使用 `druid` profile。
- MySQL 连接来源：本机 `.env.local` 中的 `RUOYI_DB_*` 运行变量。
- 目标环境：当前激活的远端 MySQL 运行库，不是本地 Docker MySQL。
- 敏感信息处理：本记录不输出 JDBC URL、数据库账号、数据库密码、Redis 地址、Redis 密码或 token secret。

## 执行方式

- 本机没有使用 `mysql` CLI。
- 使用 Maven 本地缓存中的 `mysql-connector-j` JDBC 驱动。
- 通过 JShell 临时脚本读取 `.env.local`，设置 SQL guard 变量：
  - `@confirm_terminal_login_log_direct_login_audit = 'APPLY_TERMINAL_LOGIN_LOG_DIRECT_LOGIN_AUDIT'`
- 按 SQL 文件内的 delimiter 逐条执行脚本。

## 执行前检查

执行前缺少字段：

- `seller_login_log.direct_login`
- `seller_login_log.direct_login_ticket_id`
- `seller_login_log.acting_admin_id`
- `seller_login_log.acting_admin_name`
- `seller_login_log.direct_login_reason`
- `buyer_login_log.direct_login`
- `buyer_login_log.direct_login_ticket_id`
- `buyer_login_log.acting_admin_id`
- `buyer_login_log.acting_admin_name`
- `buyer_login_log.direct_login_reason`

## 执行结果

- `scriptExecuted=true`
- `executedStatements=38`
- 执行后缺少字段：`[]`

说明：JShell 输出开头出现一次 BOM 导致的 import 警告，但后续脚本继续执行，且最终 JDBC 检查结果确认 `afterMissing=[]`。

## 数据影响

- DDL：对 `seller_login_log` / `buyer_login_log` 各新增 5 个缺失字段。
- DML：无业务数据写入。
- Redis：未读取、未写入。
- 外部系统：未调用。

## 验证结果

- 远程库字段检查：通过，`afterMissing=[]`。
- `git diff --check`：通过，仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `codegraph sync .`：通过，结果为 `Synced 2 changed files`，`Modified: 2 - 247 nodes in 558ms`。
- 记录回填后复跑 `codegraph sync .`：通过，结果为 `Synced 1 changed files`，`Modified: 1 - 79 nodes in 437ms`。
- 代码级验证沿用本轮前置结果：
  - 目标 Maven 测试通过。
  - `npm run guard:partner-management` 通过。
  - `npm run tsc -- --pretty false` 通过。
  - `npm run verify:three-terminal` 最终通过。

## 未验证原因

- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收，符合当前快速推进模式边界。
