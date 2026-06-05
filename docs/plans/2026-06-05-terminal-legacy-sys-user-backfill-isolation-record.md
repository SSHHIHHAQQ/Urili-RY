# legacy sys_user 账号回填隔离记录

## 背景

本记录服务于 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的三端独立改造目标。当前主方向要求 seller/buyer 端账号只使用 `seller_account` / `buyer_account`，不再复用若依 `sys_user` 作为端内账号体系。

前序审计已确认运行时 Java 和 Mapper 不再依赖 `sys_user`，但 `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` 仍保留一段历史兼容过程：

- `migrate_seller_account_from_sys_user`
- `migrate_buyer_account_from_sys_user`
- `left join sys_user`

这段逻辑只适用于旧混用账号库迁出，不适合继续留在当前主三端隔离基线中。

本轮不执行远程数据库 DDL/DML，不连接远程 MySQL / Redis。

## 已处理

- 从 `20260604_three_terminal_isolation_migration.sql` 移除 seller/buyer 账号从 `sys_user` 回填的过程和调用。
- 新增 `20260604_three_terminal_legacy_sys_user_account_backfill.sql`，明确标注为 legacy helper。
- legacy helper 只适用于历史库仍存在 `seller_account.user_id` / `buyer_account.user_id` 指向 `sys_user` 的场景，并且必须在主三端隔离迁移脚本之前按需执行。
- `TerminalSqlIsolationContractTest` 增加当前主迁移脚本不得包含 `migrate_*_account_from_sys_user` 或 `join sys_user` 的断言。
- 更新目标追踪和复用台账。

## 验证命令

```powershell
cd E:\Urili-Ruoyi
rg -n "migrate_(seller|buyer)_account_from_sys_user|join\s+sys_user|left\s+join\s+sys_user|sys_user" RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql
```

结果：命中只存在于 `20260604_three_terminal_legacy_sys_user_account_backfill.sql`，主迁移脚本无命中。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -Dtest=TerminalSqlIsolationContractTest test
```

结果：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest" test
```

结果：通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。

## 当前判断

- 当前主三端隔离迁移脚本不再承载端账号从若依 `sys_user` 迁出的逻辑。
- legacy helper 的存在是为了老库迁出，不代表当前架构允许 seller/buyer 账号继续复用 `sys_user`。
- 主迁移脚本仍会删除旧 `seller_account.user_id` / `buyer_account.user_id` 列；如果历史库需要从旧 `sys_user` 回填账号字段，必须先执行 legacy helper，再执行主迁移脚本。
