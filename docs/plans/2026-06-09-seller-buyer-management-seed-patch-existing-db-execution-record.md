# seller/buyer 管理综合 seed PATCH_EXISTING 远程库执行回溯记录

日期：2026-06-09
补记日期：2026-06-09

## 记录性质

本文件是回溯补齐的数据库执行记录，用于补强 `seller_buyer_management_seed.sql` 在 `PATCH_EXISTING` 画像下的远程执行证据链。

- 本补记不代表再次执行 SQL。
- 本补记未执行 DDL/DML。
- 本补记未读取或写入 Redis。
- 本补记依据既有 P0/P1 快速推进记录、后续只读核验记录和当前 SQL/合同测试证据整理。

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

当时远程运行库已有 `seller_menu` / `buyer_menu` 端内菜单数据，且菜单 ID 已在隔离区间，但缺少非空 `perms` 唯一性约束：

- `perms_unique_key` 生成列。
- `uk_seller_menu_perms` / `uk_buyer_menu_perms` 唯一索引。

该缺口会破坏端内菜单权限 slot 和 role-menu grant 的数据库完整性前提，因此在快速推进模式下采纳为 P1，并通过 `seller_buyer_management_seed.sql` 的 `PATCH_EXISTING` profile 收敛远程运行库。

## 数据源确认

当时执行摘要记录的目标为远程 MySQL，连接来源为本机 `.env.local` 中的 `RUOYI_DB_*`，地址已脱敏，未输出数据库密码、Redis 密码或 token secret。

后续只读核验记录确认当前三端隔离核验目标为远程 MySQL，连接来源为本机 `.env.local`，地址已脱敏。本补记保留历史执行与后续只读核验属于不同时间点的事实，避免混淆历史执行和当前配置。

Redis 配置当时仍指向远端 Redis，但该 seed 执行未连接、读取或写入 Redis。

## 执行脚本

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`

## 执行变量

历史快速推进记录显示执行时显式设置：

```sql
set @confirm_seller_buyer_management_seed = 'APPLY_SELLER_BUYER_MANAGEMENT_SEED';
set @seller_buyer_management_seed_profile = 'PATCH_EXISTING';
```

## 历史执行结果

既有快速推进记录显示：

- 执行结果：`executed_statements=123`。
- 影响范围：远端 MySQL DDL/DML 收敛 terminal 管理 seed、端内菜单权限约束及相关幂等数据。
- 未触碰 Redis。
- 当轮未执行 `20260604_three_terminal_isolation_migration.sql`，因为当时事实显示账号主体列、菜单区间和自增游标已满足本次 P1 收敛条件。

## 执行前预检摘要

既有记录显示，执行前预检未发现：

- 重复非空 `perms`。
- 非法端前缀。
- 菜单 ID 越界。
- role-menu 孤儿关系。

因此可以用已有 fail-closed seed 做运行库收敛。

## 执行后只读核验摘要

既有记录显示，执行后远端库满足：

- `seller_menu` / `buyer_menu` 均已存在 `perms_unique_key` 生成列。
- `uk_seller_menu_perms` / `uk_buyer_menu_perms` 均已存在，且为 `perms_unique_key` 上的唯一索引。
- `seller_invalid_perms=0`。
- `buyer_invalid_perms=0`。
- `seller_duplicate_perms=0`。
- `buyer_duplicate_perms=0`。
- `seller_menu`：min `100008`，max `100018`，count `10`，out_of_range `0`，auto_increment `100019`。
- `buyer_menu`：min `200003`，max `200013`，count `10`，out_of_range `0`，auto_increment `200014`。
- `seller_account.password` / `buyer_account.password` 均为 `varchar(100) not null`，默认值为 `null`。
- 未出现 `seller_account.user_id` / `buyer_account.user_id` legacy 列。
- `seller_role_menu_orphans_or_cross_range=0`。
- `buyer_role_menu_orphans_or_cross_range=0`。

后续远程运行库 schema 只读复核进一步确认：

- 端内菜单 `C/F` 权限非空、前缀正确、无管理端命名空间、无跨端前缀、无通配权限。
- 页面菜单 `component` 非空。
- 端内菜单父子关系、role-menu、account-role 均无孤儿。
- account-role 未发现跨主体角色绑定。

## 本地验证证据

既有快速推进记录显示，执行后已运行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：

- `SqlExecutionGuardContractTest` 77 个测试通过。
- `TerminalSqlIsolationContractTest` 12 个测试通过。
- 共 89 个测试通过。

同时运行：

```powershell
cd E:\Urili-Ruoyi\react-ui
node scripts\verify-three-terminal.mjs --check-manifest
```

结果：通过，`three-terminal manifest check passed`。

## 边界说明

- 本补记没有重新执行 `seller_buyer_management_seed.sql`。
- 本补记没有执行 `CREATE` / `ALTER` / `DROP` / `INSERT` / `UPDATE` / `DELETE`。
- 本补记没有读取或写入 Redis。
- 本补记没有启动或重启后端。
- 本补记没有做浏览器、截图、DOM 或 UI 细调验收。

## 结论

现有证据链可以支撑结论：`seller_buyer_management_seed.sql` 已在 `PATCH_EXISTING` 画像下对远程运行库完成端内菜单权限唯一性和相关幂等数据收敛；后续只读核验和合同测试未发现该 seed 在三端隔离控制面上的 P0/P1 缺口。
