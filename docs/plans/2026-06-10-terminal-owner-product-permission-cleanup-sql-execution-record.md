# 端内 Owner 商品权限默认授权清理执行记录

时间：2026-06-10 13:46，本机 `Asia/Shanghai`。

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

## 背景

上一检查点已经把代码和未来 seed 收窄为：

- `seller_menu` / `buyer_menu` 保留商品相关 hidden F 权限定义。
- `seller_role_menu` / `buyer_role_menu` 不再默认把 `*:product:*` 权限授给端内 `owner` 角色。
- portal 首页不再挂载商品样板。

本次只读核对发现远程当前库仍保留历史默认授权：

| 项 | 清理前 |
| --- | ---: |
| seller owner product grants | 12 |
| buyer owner product grants | 4 |
| seller product permission menus | 4 |
| buyer product permission menus | 4 |

其中 product permission menu 是权限定义，应保留；owner product grants 是历史默认授权，应清理。

## 数据源确认

- 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`、`application-druid.yml` 与本机 `.env.local`。
- 后端激活数据源：`spring.datasource.type = druid`，JDBC URL 从 `RUOYI_DB_URL` 读取。
- 本次目标：`.env.local` 中 `RUOYI_DB_URL` 指向非本地地址，按远程 MySQL 处理。
- Redis：本次未读取、未写入。
- 敏感信息：未在记录或对话中输出远程地址、账号、密码、Redis 密码、token secret 或 `URILI_SECRET_ENCRYPTION_KEY`。

## 执行脚本

脚本：`RuoYi-Vue/sql/20260610_terminal_owner_product_permission_cleanup.sql`

脚本保护：

- 需要确认 token：`CLEAN_TERMINAL_OWNER_PRODUCT_PERMISSION_GRANTS`。
- 需要执行前传入预览确认的 seller/buyer 精确目标数量。
- 需要执行前传入预览确认的 seller/buyer 精确目标签名。
- 执行前和事务内再次校验目标集合。
- 只删除 `seller_role_menu` / `buyer_role_menu` 中 active owner 角色绑定的 `*:product:%` 授权。
- 完成态校验 owner product grants 为 0。
- 完成态校验 seller/buyer product permission menus 各保留 4 条。

## 预览签名

| 端 | target_count | target_signature |
| --- | ---: | --- |
| seller | 12 | `37479291dbbcd0f6d868904f7186be8fea4ca218b100f1da4a1f977a7a5216e0` |
| buyer | 4 | `939d70c67a2fe9831c2bd5af75663185cd20296e7c0e3cfd7995058a0f3a3413` |

签名字段：

- seller：`seller_id + seller_role_id + seller_menu_id + perms`
- buyer：`buyer_id + buyer_role_id + buyer_menu_id + perms`

## 执行方式

本机没有 `mysql` CLI，使用本机 Maven 依赖中的 MySQL JDBC 驱动和 `jshell` 执行。

命令类型：

- 只读预览 SQL：`SELECT count + SHA2(GROUP_CONCAT(...))`
- 远程 DML：执行 guarded cleanup SQL，删除历史 owner product grants。
- 验证 SQL：`SELECT count(*)` 校验清理后状态。

## 执行结果

脚本执行结果：

| 项 | 结果 |
| --- | ---: |
| executed_statements | 29 |
| seller_owner_product_grants | 0 |
| buyer_owner_product_grants | 0 |
| seller_product_menus | 4 |
| buyer_product_menus | 4 |

结论：

- 已清理远程库中 seller/buyer owner 历史默认 product 权限授权。
- 未删除 seller/buyer product 权限菜单定义。
- 未执行 Redis 操作。
- 未启动或重启后端。

## 回滚方式

本次清理是已确认的权限收窄，不提供自动回滚脚本。

如后续确需恢复，需要单独提交并确认恢复 DML：

- 重新预览当前 active owner role 与 product permission menu 的精确集合。
- 使用 count/signature fail-closed 后再写回 `seller_role_menu` / `buyer_role_menu`。
- 恢复操作必须重新生成执行记录。

## 验证

- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest#highImpactSqlScriptsMustRequireExplicitConfirmToken+datedHighImpactSqlScriptsMustBeAutoDiscoveredAndGuarded+terminalOwnerProductPermissionCleanupMustLockExactRoleMenuTargetsAndKeepMenus" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests passed。
- 远程验证 SQL：seller/buyer owner product grants 均为 0，seller/buyer product permission menus 均为 4。
