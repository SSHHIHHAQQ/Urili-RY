# 端内账号列表权限 SQL 执行记录

日期：2026-06-04

## 目标

为端内账号只读列表接口补齐权限数据：

- `GET /seller/accounts` 需要 `seller:account:list`
- `GET /buyer/accounts` 需要 `buyer:account:list`

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 为参考方向。

## 数据源确认

- 连接来源：本机 `.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- 配置基线：`application.yml` 激活 `druid`，`application-druid.yml` 使用 `RUOYI_DB_URL` 环境变量。
- 本记录不输出数据库地址、账号或密码。
- 当前任务已确认允许对远程数据库执行 DDL/DML。

## 执行脚本

- `RuoYi-Vue/sql/20260604_portal_account_list_permission_seed.sql`

脚本行为：

- 幂等创建现有卖家主体的默认端内 Owner 角色。
- 幂等创建现有买家主体的默认端内 Owner 角色。
- 幂等绑定现有 `account_role = OWNER` 的卖家账号到对应卖家端 Owner 角色。
- 幂等绑定现有 `account_role = OWNER` 的买家账号到对应买家端 Owner 角色。
- 幂等插入 `seller_menu.perms = seller:account:list`
- 幂等插入 `buyer_menu.perms = buyer:account:list`
- 将该权限授予当前已有、启用且未删除的 `seller_role`
- 将该权限授予当前已有、启用且未删除的 `buyer_role`

## 执行结果

- 第一次执行：失败，原因是脚本按角色/部门表习惯使用了 `seller.del_flag` / `buyer.del_flag`，但当前主体表没有 `del_flag` 字段。
  - 结果：未提交变更。
  - 失败后校验：`seller_menu_account_list=0`，`buyer_menu_account_list=0`，`seller_active_roles=0`，`buyer_active_roles=0`。
- 修正方式：主体过滤改为 `seller.status = '0'` / `buyer.status = '0'`。
- 第二次执行：成功。

执行后计数：

| 项目 | 结果 |
| --- | ---: |
| `seller_menu` 中 `seller:account:list` | 1 |
| `buyer_menu` 中 `buyer:account:list` | 1 |
| 启用 `seller_role` | 3 |
| 启用 `buyer_role` | 1 |
| OWNER 卖家账号绑定 owner 角色 | 3 |
| OWNER 买家账号绑定 owner 角色 | 1 |
| `seller:account:list` 角色菜单绑定 | 3 |
| `buyer:account:list` 角色菜单绑定 | 1 |

## 验证结果

- `mvn -DskipTests install`：通过，最新 `ruoyi-admin.jar` 已重新打包。
- `.\start-backend-local.ps1 -Restart`：通过，后端已用 `.env.local` 运行变量重启。
- 管理端登录：`code=200`。
- 卖家端真实烟测：
  - 测试主体：`sellerId=9`。
  - 免密票据：`ticketId=34`。
  - `GET /seller/accounts` 返回 `code=200`。
  - 返回账号数量：1。
  - 返回对象不包含 `password` / `createBy`。
  - 卖家端 token 访问 `GET /buyer/accounts` 返回 `code=401`。
  - 烟测后已调用管理端账号级强制踢出接口清理会话。
- 买家端真实烟测：
  - 测试主体：`buyerId=2`。
  - 免密票据：`ticketId=35`。
  - `GET /buyer/accounts` 返回 `code=200`。
  - 返回账号数量：1。
  - 返回对象不包含 `password` / `createBy`。
  - 买家端 token 访问 `GET /seller/accounts` 返回 `code=401`。
  - 烟测后已调用管理端账号级强制踢出接口清理会话。
- 端内操作日志：
  - `seller_oper_log` 可查到 `operUrl=/seller/accounts`。
  - `buyer_oper_log` 可查到 `operUrl=/buyer/accounts`。
