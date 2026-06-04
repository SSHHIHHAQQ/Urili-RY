# 免密代入审计票据远程库执行记录

日期：2026-06-04

## 执行目标

为管理端免密代入卖家端、买家端补齐可审计票据表：

- 新增 `portal_direct_login_ticket`
- 只保存 `token_hash`，不保存免密 token 明文
- 记录 acting admin、目标端、目标主体、目标端账号、过期时间、使用时间、使用 IP 和状态

## 连接来源

- 配置来源：本机 `.env.local` 的 `RUOYI_DB_*`
- 目标环境：远程 MySQL
- 敏感信息处理：不在本文档记录 JDBC URL、用户名、密码、token secret 或 Redis 密码

## 执行脚本

- `RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`

## 执行前状态

- 用户已确认本目标下允许执行远程数据库 DDL/DML。
- 本次脚本为 `create table if not exists`，不会修改既有业务表数据。

## 执行结果

- 执行时间：2026-06-04
- 执行方式：使用本机 Maven 依赖中的 MySQL JDBC 驱动读取 `.env.local` 的 `RUOYI_DB_*` 连接远程 MySQL。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`
- 执行语句数：2
- 执行结果：成功。

## 验证结果

- `portal_direct_login_ticket` 表存在：是。
- 字段数量：19。
- 索引数量：5，包括主键、`token_hash` 唯一索引、目标对象索引、管理员时间索引、状态过期时间索引。
- 明文 token 存储检查：表结构只包含 `token_hash`，不包含 token 明文字段。
