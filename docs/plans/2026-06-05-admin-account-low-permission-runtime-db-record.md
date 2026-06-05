# 管理端账号域低权限真实账号验收执行记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在账号域权限运行时负向单元测试之后，使用真实管理端低权限账号验证：

- 有主体列表/查询权限时，可以进入卖家/买家管理列表。
- 没有 `seller:admin:account:*` / `buyer:admin:account:*` 时，后端账号域接口应拒绝。
- 前端不应显示主体行“账号”入口。

本轮需要向当前远程运行库写入一个可回滚的临时测试角色和测试账号。不新增表，不修改业务主体、卖家/买家端账号、端内角色、端内菜单、日志或会话数据。

## 数据源确认

- 后端激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 中 `spring.profiles.active=druid`。
- MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 使用 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- Redis 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 使用 `RUOYI_REDIS_*`。
- 本机运行变量来源：`.env.local` 中存在 `RUOYI_DB_*`、`RUOYI_REDIS_*` 和 `RUOYI_TOKEN_SECRET` 键。
- 本记录不输出连接串、账号、密码、Redis 地址或 token secret。

## 执行前核对

- 当前 `sys.account.captchaEnabled=false`，低权限账号可以走普通 `/login` 接口验收，不需要临时修改验证码开关。
- 运行库中不存在以下测试角色：`codex_account_negative_seller`、`codex_account_negative_buyer`、`codex_account_negative_both`。
- 运行库中不存在以下测试账号：`codex_seller_limited`、`codex_buyer_limited`、`codex_account_limited`。
- 目标菜单权限已存在：
  - `seller:admin:query`
  - `seller:admin:account:list`
  - `buyer:admin:query`
  - `buyer:admin:account:list`

## 计划写入数据

- 新增测试角色：`codex_account_negative_both`。
- 新增测试账号：原计划为 `codex_account_limited`；实际执行改为 `codex_limited`，因为若依用户名长度上限为 20。
- 测试账号密码使用管理端登录密码规则，具体明文不写入记录。
- 角色只绑定以下菜单权限：
  - 顶级主体管理菜单 `2010`
  - 卖家管理菜单 `2011`
  - 买家管理菜单 `2012`
  - 卖家查询 `seller:admin:query`
  - 买家查询 `buyer:admin:query`
- 角色不绑定任何 `seller:admin:account:*` 或 `buyer:admin:account:*` 权限。

## 执行结果

- 已向当前远程运行库写入临时测试角色 `codex_account_negative_both`。
- 已向当前远程运行库写入临时测试账号 `codex_limited`；测试密码仅用于本轮验收，明文不写入记录。
- 已绑定 5 条角色菜单关系：`2010`、`2011`、`2012`、`seller:admin:query`、`buyer:admin:query`。
- 使用 `codex_limited` 登录后，`/getInfo` 返回的有效权限只有：
  - `seller:admin:list`
  - `seller:admin:query`
  - `buyer:admin:list`
  - `buyer:admin:query`
- 旧 jar 问题：首次用低权限账号调用账号域接口时，账号域接口没有返回 403，而是进入业务 service 后返回“卖家/买家不存在”。这说明 8080 当时运行的是旧 jar，未加载本轮新的 `@PreAuthorize` 注解。
- 已停止旧 Java 进程，执行 `mvn -DskipTests install` 重新打包后，通过 `.\start-backend-local.ps1 -Restart` 重启后端。
- 重启后验证 `/captchaImage` 正常，验证码仍为关闭状态；本轮未修改验证码开关。

接口验收结果：

- `GET /seller/admin/sellers/list?pageNum=1&pageSize=1`：`code=200`，主体列表可访问。
- `GET /seller/admin/sellers/1/accounts`：`code=403`，账号域列表被拒绝。
- `GET /buyer/admin/buyers/list?pageNum=1&pageSize=1`：`code=200`，主体列表可访问。
- `GET /buyer/admin/buyers/1/accounts`：`code=403`，账号域列表被拒绝。
- `GET /system/user/list?pageNum=1&pageSize=1`：`code=403`，作为管理端系统用户权限对照，同样被拒绝。

浏览器验收结果：

- 使用 Playwright CLI 以 `codex_limited` 登录管理端并访问 `/partner/seller`。
- 卖家管理表格操作列文本为 `操作|||`；数据行没有显示“账号”入口。
- 使用同一账号访问 `/partner/buyer`。
- 买家管理表格操作列文本为 `操作|`；数据行没有显示“账号”入口。
- 浏览器会话已关闭。

清理结果：

- 已通过管理端在线用户接口强制踢出 `codex_limited` 的在线 token，返回 `forcedLogoutTokens=4`。
- 已按回滚顺序删除临时 `sys_user_role`、`sys_role_menu`、`sys_user`、`sys_role` 数据。
- 清理结果：`userRole=1`、`roleMenu=5`、`user=1`、`role=1`。
- 清理后核验：`codex_limited` 用户剩余 `0`，`codex_account_negative_both` 角色剩余 `0`。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests install`：通过，重新生成 `ruoyi-admin.jar`。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1 -Restart`：通过，8080 由新 jar 重新监听。
- 使用低权限 token 调用卖家/买家主体列表和账号列表接口：列表 `code=200`，账号域接口 `code=403`。
- 使用 Playwright CLI 登录低权限账号并访问 `/partner/seller`、`/partner/buyer`：数据行均未显示“账号”入口。
- `cd E:\Urili-Ruoyi; git diff --check -- docs\plans\2026-06-05-admin-account-low-permission-runtime-db-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 回滚方式

本轮已完成回滚。若后续复用该模板再次创建临时账号和角色，可按以下顺序回滚：

```sql
delete from sys_user_role where user_id in (
  select user_id from sys_user where user_name = 'codex_limited'
);

delete from sys_role_menu where role_id in (
  select role_id from sys_role where role_key = 'codex_account_negative_both'
);

delete from sys_user where user_name = 'codex_limited';

delete from sys_role where role_key = 'codex_account_negative_both';
```

## 残留风险

- 本轮临时管理端测试账号和测试角色已删除，未留下测试账号或测试角色。
- 本轮发现的旧 jar 问题已通过重新打包和重启解决；后续如果改了后端注解但未重启新 jar，真实接口验收仍可能误判为旧行为。
- 本轮只验证账号域列表入口和后端账号域列表接口；账号新增、编辑、重置密码、角色分配的真实低权限接口拒绝已有 `PermissionServiceAccountPermissionTest` 和前端契约测试覆盖，后续如需更严格可再做逐接口真实账号验收。
