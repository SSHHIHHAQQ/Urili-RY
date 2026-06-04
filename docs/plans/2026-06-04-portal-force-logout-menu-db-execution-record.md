# 端内强制踢出权限远程库执行记录

日期：2026-06-04

## 执行目标

为管理端卖家/买家强制踢出能力补齐按钮权限：

- `seller:admin:forceLogout`
- `buyer:admin:forceLogout`

## 执行环境

- 工作区：`E:\Urili-Ruoyi`
- 数据源来源：本机 `.env.local` 中的 `RUOYI_DB_*`
- 目标库类型：以后端激活配置指向的远程 MySQL 为准
- 凭证处理：未在记录、命令输出或聊天中明文输出数据库账号、密码或连接地址

## 执行脚本

- `RuoYi-Vue/sql/20260604_portal_force_logout_menu_seed.sql`

## 执行方式

使用本机 Maven 缓存中的 MySQL JDBC 驱动执行 SQL。

## 执行结果

- `executedStatements=1`
- `permissionCount=2`

## 影响范围

- 只 upsert `sys_menu` 中两个按钮权限点。
- 不修改卖家、买家主体数据。
- 不修改端账号、端角色、端菜单、端部门、端登录日志或端会话数据。

## 回滚方式

如需回滚，可从 `sys_menu` 删除以下权限点：

```sql
delete from sys_menu
where perms in ('seller:admin:forceLogout', 'buyer:admin:forceLogout');
```

执行回滚前仍需重新确认目标数据源。
