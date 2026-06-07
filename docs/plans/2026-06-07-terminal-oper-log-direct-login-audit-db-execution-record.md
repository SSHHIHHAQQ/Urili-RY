# 端内操作日志免密代入结构化审计远程库执行记录

日期：2026-06-07

## 执行目的

按 `docs/plans/2026-06-07-terminal-oper-log-direct-login-audit-design.md` 和 `RuoYi-Vue/sql/20260607_terminal_oper_log_direct_login_audit.sql` 的字段方案，补齐远程运行库 `seller_oper_log` / `buyer_oper_log` 的 direct-login 结构化审计字段。

## 目标环境

- 连接来源：`E:\Urili-Ruoyi\.env.local` 中的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`。
- 激活配置依据：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 当前 `spring.profiles.active=druid`，`application-druid.yml` 使用 `RUOYI_DB_*` 环境变量。
- 目标库：远程 MySQL，JDBC URL 指向 `gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`。
- 本记录不输出数据库密码、Redis 密码或 token secret。

## 执行前检查

本机未找到 `mysql` / `mariadb` 命令，改用本地 Java + MySQL Connector/J 读取 `.env.local` 环境变量执行 JDBC 检查。

执行前只读检查结果：

```text
database=fenxiao
seller_oper_log:beforeMissing=[direct_login, direct_login_ticket_id, acting_admin_id, acting_admin_name, direct_login_reason]
buyer_oper_log:beforeMissing=[direct_login, direct_login_ticket_id, acting_admin_id, acting_admin_name, direct_login_reason]
```

## 执行动作

执行类型：远程 MySQL DDL。

实际补列：

- `seller_oper_log.direct_login tinyint(1) not null default 0`
- `seller_oper_log.direct_login_ticket_id bigint(20) default null`
- `seller_oper_log.acting_admin_id bigint(20) default null`
- `seller_oper_log.acting_admin_name varchar(64) default ''`
- `seller_oper_log.direct_login_reason varchar(255) default ''`
- `buyer_oper_log.direct_login tinyint(1) not null default 0`
- `buyer_oper_log.direct_login_ticket_id bigint(20) default null`
- `buyer_oper_log.acting_admin_id bigint(20) default null`
- `buyer_oper_log.acting_admin_name varchar(64) default ''`
- `buyer_oper_log.direct_login_reason varchar(255) default ''`

执行输出：

```text
database=fenxiao
seller_oper_log:beforeMissing=[direct_login, direct_login_ticket_id, acting_admin_id, acting_admin_name, direct_login_reason]
seller_oper_log:added=direct_login
seller_oper_log:added=direct_login_ticket_id
seller_oper_log:added=acting_admin_id
seller_oper_log:added=acting_admin_name
seller_oper_log:added=direct_login_reason
seller_oper_log:afterMissing=[]
buyer_oper_log:beforeMissing=[direct_login, direct_login_ticket_id, acting_admin_id, acting_admin_name, direct_login_reason]
buyer_oper_log:added=direct_login
buyer_oper_log:added=direct_login_ticket_id
buyer_oper_log:added=acting_admin_id
buyer_oper_log:added=acting_admin_name
buyer_oper_log:added=direct_login_reason
buyer_oper_log:afterMissing=[]
```

## 执行后复核

执行后只读复核结果：

```text
database=fenxiao
seller_oper_log:beforeMissing=[]
buyer_oper_log:beforeMissing=[]
```

结论：远程运行库 `seller_oper_log` / `buyer_oper_log` 已补齐 direct-login 结构化审计字段。

## 边界

- 本次未读取或写入 Redis。
- 本次未启动或重启后端。
- 本次未执行浏览器、截图、DOM 或 UI 细调验收。
- 本次未改动业务数据行，只执行缺失列 DDL。
- 临时 JDBC 工具源码和 class 已清理，不作为业务代码保留。
