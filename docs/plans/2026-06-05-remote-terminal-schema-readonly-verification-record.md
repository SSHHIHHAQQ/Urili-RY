# 远程库三端独立结构只读核验记录

## 背景

本记录服务于 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的三端独立改造目标。目标要求管理端保留若依 `sys_*` 后台体系，seller/buyer 端账号、密码、角色、菜单、部门、日志和会话独立。

本轮只做远程 MySQL 当前结构核验，不执行 DDL/DML，不修改数据，不读取账号密码明文或业务内容。

## 数据源确认

- 激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 当前 `active: druid`。
- JDBC 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`。
- 目标库：远程 MySQL，库名 `fenxiao`。
- Redis 配置：`application.yml` 指向远程 Redis。
- 凭证来源：本机 `.env.local` 的 `RUOYI_DB_*`，未在命令输出或记录中写入明文。
- 本机 `mysql` CLI 不存在，本轮使用 Maven 本地缓存的 `mysql-connector-j` 和 `jshell` 进行 JDBC 只读查询。

## 查询范围

只读查询范围：

- `information_schema.tables`
- `information_schema.columns`
- `information_schema.statistics`
- 核心表的 `count(*)`

未读取：

- 账号密码字段内容
- token / Redis key
- 业务详情数据
- `.env.local` 明文

## 核验结果

- 三端独立核心表存在：`21/21`，缺失列表为空。
- `seller_account.user_id`：不存在，计数 `0`。
- `buyer_account.user_id`：不存在，计数 `0`。
- `seller_account` / `buyer_account` 关键字段完整：端账号 ID、主体 ID、`user_name`、`password`、`account_role`、`status`、`lock_status` 均存在。
- `seller_login_log` / `buyer_login_log` 关键字段完整：日志 ID、端主体 ID、端账号 ID、账号名、登录时间均存在。
- `seller_oper_log` / `buyer_oper_log` 关键字段完整：日志 ID、端主体 ID、端账号 ID、标题、操作人、操作时间均存在。
- `seller_session` / `buyer_session` 关键字段完整：`token_id`、端主体 ID、端账号 ID、账号名、过期时间、状态均存在。
- 关键索引存在：
  - `seller_account.uk_seller_account_username`
  - `buyer_account.uk_buyer_account_username`
  - `seller_account.uk_seller_account_owner`
  - `buyer_account.uk_buyer_account_owner`
  - `seller_session.idx_seller_session_account`
  - `buyer_session.idx_buyer_session_account`

## 输出摘要

```text
target=fenxiao
coreTablesPresent=21/21 missing=[]
forbiddenColumn seller_account.user_id count=0
forbiddenColumn buyer_account.user_id count=0
requiredColumns seller_account missing=[]
requiredColumns buyer_account missing=[]
requiredColumns seller_login_log missing=[]
requiredColumns buyer_login_log missing=[]
requiredColumns seller_oper_log missing=[]
requiredColumns buyer_oper_log missing=[]
requiredColumns seller_session missing=[]
requiredColumns buyer_session missing=[]
index seller_account.uk_seller_account_username count=1
index buyer_account.uk_buyer_account_username count=1
index seller_account.uk_seller_account_owner count=1
index buyer_account.uk_buyer_account_owner count=1
index seller_session.idx_seller_session_account count=1
index buyer_session.idx_buyer_session_account count=1
```

核心表行数只作为结构核验辅助，不作为业务验收结论：

```text
seller_account=3
buyer_account=1
seller_role=10
buyer_role=3
seller_menu=7
buyer_menu=7
seller_session=96
buyer_session=61
portal_direct_login_ticket=116
```

## 当前判断

- 当前远程 MySQL 的三端账号、权限、日志、会话核心结构与三端独立方向一致。
- 本轮只证明结构存在、旧 `user_id` 列已移除、关键字段和索引存在；不替代接口权限测试、前端显隐 guard、登录 smoke 或业务流程验收。
- 本轮未执行 DDL/DML，未修改远程数据。
