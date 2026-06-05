# 管理端买家账号权限远程库执行记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。在卖家账号域权限模板和远程库执行完成后，本切片只按同一模板复制买家账号域权限。

本轮只执行买家账号域权限 DML，不新增表，不修改账号、角色、部门、日志或会话数据。

## 数据源确认

- 后端激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 中 `spring.profiles.active=druid`。
- MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 使用 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- 本机运行变量来源：`.env.local` 中存在 `RUOYI_DB_*`、`RUOYI_REDIS_*` 和 `RUOYI_TOKEN_SECRET` 键。
- 本记录不输出连接串、账号、密码、Redis 地址或 token secret。

## 执行前核对

- 远程库 `menu_id between 2316 and 2325` 与 `buyer:admin:account:%` 查询结果为 `rows=0`。
- 因此本轮使用 `2316-2321` 作为买家账号域权限连续 ID 段，紧跟卖家 `2310-2315`。

## 执行脚本

- `RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql`

脚本内容只包含 `sys_menu` 的 6 条 upsert：

- `buyer:admin:account:list`
- `buyer:admin:account:add`
- `buyer:admin:account:edit`
- `buyer:admin:account:resetPwd`
- `buyer:admin:account:role:query`
- `buyer:admin:account:role:edit`

## 执行结果

- 已通过 JDBC 读取本机 `.env.local` 中的 `RUOYI_DB_*` 运行变量执行增量 SQL。
- 执行成功，影响行数：`affected=6`。
- 本轮未执行 DDL，未读取或写入 Redis，未输出连接串、账号、密码、Redis 地址或 token secret。

## 执行后核验

远程库查询结果：

```text
2316|2012|125|买家账号列表|buyer:admin:account:list|0
2317|2012|130|买家账号新增|buyer:admin:account:add|0
2318|2012|135|买家账号修改|buyer:admin:account:edit|0
2319|2012|140|买家账号重置密码|buyer:admin:account:resetPwd|0
2320|2012|145|买家账号角色查询|buyer:admin:account:role:query|0
2321|2012|150|买家账号角色分配|buyer:admin:account:role:edit|0
permissionRows=6
```

核验结论：6 个买家账号域权限均已写入当前远程运行库，父菜单为买家管理菜单 `2012`，状态为正常 `0`。

## 回滚方式

如果需要回滚本轮权限 seed，可删除以下 `sys_menu` 权限行：

```sql
delete from sys_menu
where perms in (
  'buyer:admin:account:list',
  'buyer:admin:account:add',
  'buyer:admin:account:edit',
  'buyer:admin:account:resetPwd',
  'buyer:admin:account:role:query',
  'buyer:admin:account:role:edit'
);
```

回滚前应确认没有角色依赖这些权限；如已有 `sys_role_menu` 绑定，应先删除对应绑定或使用外键约束允许的顺序执行。
