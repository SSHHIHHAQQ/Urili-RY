# 2026-06-09 三端会话免密审计字段远端库修复记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按三端独立账号权限模型推进 P0/P1 修复。

本轮只处理一个 P1：当前远端运行库 `seller_session` / `buyer_session` 缺少免密代入会话审计字段，而代码侧 Mapper 已经在会话写入和查询中使用这些字段。

## 数据源确认

- 工作区：`E:\Urili-Ruoyi`
- 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`、`application-druid.yml` 和本机 `.env.local`
- MySQL：从 `.env.local` 的 `RUOYI_DB_URL` 读取，目标库为远端 `gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`
- Redis：从 `.env.local` 的 `RUOYI_REDIS_*` 读取，目标为远端 `114.132.156.75:6379`，本轮未读写 Redis
- 本轮不使用本地 Docker MySQL / Redis
- 本记录不输出数据库密码、Redis 密码、token secret 或任何明文密钥

## 执行前只读核验

只读 JDBC 核验结果：

- 三端核心表存在：`21/21`
- `seller_account.password`：`varchar(100) not null`，无空白密码行
- `buyer_account.password`：`varchar(100) not null`，无空白密码行
- `seller_menu` / `buyer_menu` ID 区间、perms、component、父子关系和 role-menu 关联未发现异常
- `seller_login_log` / `buyer_login_log` 已包含免密审计字段
- `seller_oper_log` / `buyer_oper_log` 已包含免密审计字段
- `portal_direct_login_ticket` 关键审计字段存在
- `seller_session` 缺少：`direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`
- `buyer_session` 缺少：`direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`

## 修复范围

仅对以下两张表追加缺失字段：

- `seller_session`
- `buyer_session`

字段契约：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `direct_login` | `tinyint(1) not null` | `0` | 是否免密代入会话 |
| `direct_login_ticket_id` | `bigint(20)` | `null` | 免密代入票据 ID |
| `acting_admin_id` | `bigint(20)` | `null` | 执行代入的管理端账号 ID |
| `acting_admin_name` | `varchar(64)` | `''` | 执行代入的管理端账号名 |
| `direct_login_reason` | `varchar(255)` | `''` | 免密代入原因 |

## 执行方式

- 使用本机 Maven 缓存的 `mysql-connector-j` JDBC 驱动和 `jshell`
- 每个字段先查 `information_schema.columns`，仅在字段不存在时执行 `ALTER TABLE ... ADD COLUMN`
- 不删除表、不删除行、不更新账号、密码、菜单、角色或业务数据
- DDL 会影响远端运行库表结构，但不改写已有会话行内容；新增字段按默认值或 `null` 补齐

## P2 记录

- 远端 `sys_config` 中 `portal.seller.web.url` / `portal.buyer.web.url` 当前仍是本地验证占位地址 `127.0.0.1:8001`。当前阶段仍在 `react-ui` 验证三端入口，先记录为 P2，不阻塞本轮 P0/P1。

## 执行结果

执行目标：

- 远端 MySQL：`gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`
- Redis：未读写

执行过程：

- 第一次受控 DDL 已补齐 `seller_session` 五个字段。
- 第一次脚本在校验阶段将 `character_maximum_length` 作为 `Integer` 读取，触发 JShell 类型转换异常；该异常发生在 `seller_session` 字段已新增之后、`buyer_session` 字段新增之前。
- 第二次使用修正后的校验脚本继续执行，`seller_session` 五个字段识别为已存在，`buyer_session` 五个字段新增成功。

DDL 输出摘要：

```text
DDL|seller_session.direct_login|EXISTS
DDL|seller_session.direct_login_ticket_id|EXISTS
DDL|seller_session.acting_admin_id|EXISTS
DDL|seller_session.acting_admin_name|EXISTS
DDL|seller_session.direct_login_reason|EXISTS
DDL|buyer_session.direct_login|ADDED
DDL|buyer_session.direct_login_ticket_id|ADDED
DDL|buyer_session.acting_admin_id|ADDED
DDL|buyer_session.acting_admin_name|ADDED
DDL|buyer_session.direct_login_reason|ADDED
RESULT|session_direct_login_audit_columns|OK
```

执行后只读复核：

```text
CHECK|database|fenxiao
CHECK|seller_session_missing_direct_login_audit_columns|-
CHECK|buyer_session_missing_direct_login_audit_columns|-
COL|seller_session|direct_login|tinyint||NO|0
COL|seller_session|direct_login_ticket_id|bigint||YES|null
COL|seller_session|acting_admin_id|bigint||YES|null
COL|seller_session|acting_admin_name|varchar|64|YES|
COL|seller_session|direct_login_reason|varchar|255|YES|
COL|buyer_session|direct_login|tinyint||NO|0
COL|buyer_session|direct_login_ticket_id|bigint||YES|null
COL|buyer_session|acting_admin_id|bigint||YES|null
COL|buyer_session|acting_admin_name|varchar|64|YES|
COL|buyer_session|direct_login_reason|varchar|255|YES|
```

结论：

- `seller_session` / `buyer_session` 免密代入会话审计字段已经补齐。
- 本轮只执行 DDL 补列，不执行 DML，不删除或更新任何已有业务数据。
- 当前代码侧 session insert/select 所需字段与远端运行库结构已对齐。

## 收尾验证

- `git diff --check`
  - 通过；仅提示当前工作区已有 LF/CRLF 换行转换 warning。
- 行尾空白检查
  - 通过；本记录、快速推进记录、目标追踪记录均无行尾空白命中。
- `codegraph sync .`
  - 通过，输出 `Already up to date`。
- 浏览器、截图、DOM、UI 细调验收
  - 按当前快速推进模式跳过。
