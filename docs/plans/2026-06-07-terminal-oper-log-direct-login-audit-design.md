# 端内操作日志免密代入结构化审计字段方案

日期：2026-06-07

## 背景

三端隔离方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准。管理端保留若依 `sys_*`，卖家端和买家端独立维护账号、权限、日志和会话。

当前端内登录日志和会话已经结构化保存免密代入审计字段，但 `seller_oper_log` / `buyer_oper_log` 仍主要依赖 `oper_param` 中的 `directLoginAudit{...}` 文本前缀，无法稳定筛选和结构化追溯。

## 目标

- `seller_oper_log` / `buyer_oper_log` 与端内登录日志保持同一组免密代入审计字段。
- `@PortalLog` 和 `@PortalPreAuthorize` 生成的端内操作日志都能记录结构化审计字段。
- 继续保留 `oper_param` 文本前缀作为兼容信息，但不再作为唯一审计来源。
- 不执行远程数据库 DDL；本方案只提供 guarded SQL 文件和代码契约。

## 字段设计

| 表 | 字段 | 类型 | 默认值 | 含义 |
| --- | --- | --- | --- | --- |
| `seller_oper_log` / `buyer_oper_log` | `direct_login` | `tinyint(1)` | `0` | 是否免密代入会话产生 |
| `seller_oper_log` / `buyer_oper_log` | `direct_login_ticket_id` | `bigint(20)` | `null` | 免密代入票据 ID |
| `seller_oper_log` / `buyer_oper_log` | `acting_admin_id` | `bigint(20)` | `null` | 免密票据签发的管理端账号 ID |
| `seller_oper_log` / `buyer_oper_log` | `acting_admin_name` | `varchar(64)` | `''` | 免密票据签发的管理端账号名 |
| `seller_oper_log` / `buyer_oper_log` | `direct_login_reason` | `varchar(255)` | `''` | 免密代入原因 |

`acting_admin_*` 表示原免密票据签发人，不表示端内当前账号，也不表示后续强退、重置密码等管理端动作的执行人。

## 落地方式

- fresh baseline：更新 `20260604_three_terminal_isolation_migration.sql` 和 `seller_buyer_management_seed.sql`。
- 运行库补丁：新增 `20260607_terminal_oper_log_direct_login_audit.sql`，使用确认变量 `@confirm_terminal_oper_log_direct_login_audit` 和 token `APPLY_TERMINAL_OPER_LOG_DIRECT_LOGIN_AUDIT`。
- 代码：`PortalOperLog` 新增字段，Aspect 写结构化字段，Mapper 写入并查询字段。
- 契约：补 `TerminalSqlIsolationContractTest`、`SqlExecutionGuardContractTest`、`PortalDirectLoginAuthContractTest`、`PortalLogAspectContractTest`。

## 边界

- 本方案不新增索引；当前查询仍以主体、账号、时间为主。
- 本方案不做管理端 UI 列展示和筛选细调，前端展示可作为 P2 后续处理。
- 本方案不连接远程 MySQL / Redis，不执行 DDL/DML。
