# 2026-06-04 三端审计权限 SQL 执行记录

## 目标

- 为管理端卖家/买家管理新增只读审计入口权限。
- 本次只写入 `sys_menu` 按钮权限，不新增业务表，不修改账号、角色、业务数据。

## 执行环境确认

- 工作区：`E:\Urili-Ruoyi`
- 配置来源：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
  - `.env.local`
- 目标 MySQL：远端 `fenxiao` 库。
- 目标 Redis：远端 Redis。
- 本次不使用本地 Docker MySQL/Redis。

## SQL 文件

- `RuoYi-Vue/sql/20260604_portal_audit_admin_menu_seed.sql`

## 写入内容

| menu_id | 权限标识 | 说明 |
| --- | --- | --- |
| 2250 | `seller:admin:loginLog:list` | 卖家登录日志只读列表 |
| 2251 | `seller:admin:operLog:list` | 卖家操作日志只读列表 |
| 2252 | `seller:admin:ticket:list` | 卖家免密票据只读列表 |
| 2253 | `buyer:admin:loginLog:list` | 买家登录日志只读列表 |
| 2254 | `buyer:admin:operLog:list` | 买家操作日志只读列表 |
| 2255 | `buyer:admin:ticket:list` | 买家免密票据只读列表 |

## 影响范围

- 影响远端数据库 `sys_menu`。
- 使用 `insert ... on duplicate key update`，重复执行会更新同 ID 的按钮权限定义。
- 不影响 `seller_*`、`buyer_*`、`portal_direct_login_ticket` 业务表数据。

## 回滚方式

如需回滚，可删除上述 6 条 `sys_menu` 记录：

```sql
delete from sys_menu where menu_id in (2250, 2251, 2252, 2253, 2254, 2255);
```

如角色已经关联这些按钮权限，回滚时同步删除 `sys_role_menu` 中对应 `menu_id` 的关联。

## 执行结果

- 执行时间：2026-06-04
- 执行方式：通过 MySQL JDBC 驱动读取 `.env.local` 中的远端连接变量执行 SQL。
- 执行语句数：2
- 验证结果：远端 `sys_menu` 已存在 6 条权限记录：
  - `2250 seller:admin:loginLog:list`
  - `2251 seller:admin:operLog:list`
  - `2252 seller:admin:ticket:list`
  - `2253 buyer:admin:loginLog:list`
  - `2254 buyer:admin:operLog:list`
  - `2255 buyer:admin:ticket:list`
