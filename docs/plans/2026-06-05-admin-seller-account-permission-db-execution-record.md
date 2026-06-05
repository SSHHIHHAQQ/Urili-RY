# 管理端卖家账号权限远程库执行记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。上一切片已把卖家管理端账号维护接口切换为 `seller:admin:account:*` 权限，并更新综合 seed；本切片只让当前远程运行库补齐这些管理端 `sys_menu` 权限。

本轮只执行卖家账号域权限 DML，不复制买家，不新增表，不修改账号、角色、部门、日志或会话数据。

## 数据源确认

- 后端激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 中 `spring.profiles.active=druid`。
- MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 使用 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- 本机运行变量来源：`.env.local` 中存在 `RUOYI_DB_*`、`RUOYI_REDIS_*` 和 `RUOYI_TOKEN_SECRET` 键。
- 本记录不输出连接串、账号、密码、Redis 地址或 token secret。

## 执行前核对

- 运行库中 6 个目标权限当前不存在：`matchedRows=0`。
- 运行库中 `menu_id=2260/2261` 已被客户渠道菜单占用：
  - `2260|客户渠道查询|channel:customer:query`
  - `2261|客户渠道新增|channel:customer:add`
- 因此本轮不使用早期 seed 中的 `2256-2261` 连续段，改用已确认空闲的 `2310-2315` 连续段。
- 远程库 `menu_id between 2310 and 2330` 查询结果为 `rows=0`。

## 执行脚本

- `RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql`

脚本内容只包含 `sys_menu` 的 6 条 upsert：

- `seller:admin:account:list`
- `seller:admin:account:add`
- `seller:admin:account:edit`
- `seller:admin:account:resetPwd`
- `seller:admin:account:role:query`
- `seller:admin:account:role:edit`

## 执行结果

- 执行方式：使用本机 `.env.local` 中的 `RUOYI_DB_*` 变量，通过 MySQL JDBC 执行 `RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql`。
- 执行结果：成功，JDBC 返回 `affected=6`。
- 未执行 DDL。
- 未执行 Redis 操作。
- 未输出数据库连接串、账号、密码或 token secret。

## 执行后核验

目标权限核验：

```text
2310|2011|125|卖家账号列表|seller:admin:account:list|0
2311|2011|130|卖家账号新增|seller:admin:account:add|0
2312|2011|135|卖家账号修改|seller:admin:account:edit|0
2313|2011|140|卖家账号重置密码|seller:admin:account:resetPwd|0
2314|2011|145|卖家账号角色查询|seller:admin:account:role:query|0
2315|2011|150|卖家账号角色分配|seller:admin:account:role:edit|0
permissionRows=6
```

旧菜单避让核验：

```text
reserved=2260|客户渠道查询|channel:customer:query
reserved=2261|客户渠道新增|channel:customer:add
```

结论：

- 6 个卖家账号域管理端权限已写入当前远程运行库。
- 早期冲突 ID `2260/2261` 未被覆盖。

## 回滚方式

如果需要回滚本轮权限 seed，可删除以下 `sys_menu` 权限行：

```sql
delete from sys_menu
where perms in (
  'seller:admin:account:list',
  'seller:admin:account:add',
  'seller:admin:account:edit',
  'seller:admin:account:resetPwd',
  'seller:admin:account:role:query',
  'seller:admin:account:role:edit'
);
```

回滚前应确认没有角色依赖这些权限；如已有 `sys_role_menu` 绑定，应先删除对应绑定或使用外键约束允许的顺序执行。

## 验证计划

- 已查询 `sys_menu` 中 6 个权限的 `menu_id`、`parent_id`、`perms` 和 `status`。
- 已查询 `menu_id=2260/2261` 仍保持客户渠道菜单，确认未被覆盖。
- `mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest" test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- SQL ID 检查：综合 seed 和增量 SQL 中卖家账号权限均使用 `2310-2315`，未发现旧的 `2256-2261` 卖家账号权限。
- `git diff --check`：通过，仅有 LF/CRLF 工作区换行提示。
- 新增 SQL 和执行记录尾随空白检查：通过。
- `codegraph sync .`：通过，输出 `Synced 1 changed files`。
