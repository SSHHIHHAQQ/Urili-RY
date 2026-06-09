# 2026-06-07 三端隔离 P0/P1 主迁移 Preflight 收口记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

执行模式：快速推进模式，只修 P0/P1（编译、guard、接口、权限、串端、service/字段缺失）；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮目标

- 将 `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` 中 legacy `user_id` blocker 前移为真正 preflight。
- 在首条业务 DDL/DML 前完成只读 preflight，降低 MySQL DDL 非事务化导致的半执行风险。
- 用 `SqlExecutionGuardContractTest` 固定 preflight 顺序和非事务/半执行说明，避免后续回退。

## 子 Agent 使用记录

- 本轮延续目标要求使用并关闭 6 个只读子 Agent。
- 6 个子 Agent 均已返回并关闭。
- 历史记录（已过期口径）：这些子 Agent 是在用户补充模型偏好前启动，使用的是 `gpt-5.4`；工具侧可选模型包含 `gpt-5.3-codex-spark` 和 `gpt-5.4`，当时记录的后续新开子 Agent 优先 `gpt-5.3-codex-spark` 已不再适用。现行规则为默认使用 `gpt-5.4`。

## 新增问题

- 主迁移脚本原先只有确认 token，legacy `seller_account.user_id` / `buyer_account.user_id` blocker 位于账号补列、回填、删索引等动作之后，不是真正 preflight。
- 脚本混合 DDL 和 DML，但顶部缺少清晰的非事务化、可能半执行和重放策略说明。
- 主迁移脚本已创建 `assert_no_legacy_account_user_bindings`，但尾部清理此前未覆盖该 helper。

## 已修复问题

- `20260604_three_terminal_isolation_migration.sql` 顶部补充非事务化说明：MySQL DDL 会隐式提交，失败后可能半执行，需修复失败原因并从头重放同一幂等脚本。
- 新增 `assert_three_terminal_isolation_preflight()`，并在 `create table if not exists seller_account` 前立即调用。
- preflight 只做只读检查，不修数据：
  - `assert_database_selected()`：要求当前会话已选中目标 database。
  - `assert_existing_column_if_table_present(...)`：如果 `seller_account` / `buyer_account` 已存在，则先确认账号表关键列存在。
  - `assert_no_legacy_account_user_bindings(...)`：如果旧 `user_id` 仍有非空绑定，阻断主迁移，要求先执行 legacy `sys_user` 回填 helper。
- 删除后续账号改造阶段的延迟 legacy blocker 调用，避免“先改后拦”。
- 尾部补齐 `assert_three_terminal_isolation_preflight`、`assert_existing_column_if_table_present`、`assert_database_selected`、`assert_no_legacy_account_user_bindings` 的清理。
- `SqlExecutionGuardContractTest` 新增合同，固定：
  - preflight 调用必须位于确认 token 之后、首条业务 DDL/DML 之前。
  - preflight 必须早于账号 `UPDATE` 和 `DROP user_id`。
  - 脚本必须保留非事务化/半执行说明。
  - 脚本不得伪装为 `START TRANSACTION` / `ROLLBACK` 可保护的事务迁移。
- `docs/architecture/reuse-ledger.md` 增补 SQL preflight 和非事务混合迁移复用规则。

## 残留问题

- `seller_buyer_management_seed.sql` 仍同时承担 fresh bootstrap 和全局增量修补职责，后续应作为独立 P1 拆分或增加 profile/freshness guard。
- integration fresh bootstrap schema 缺口仍需单独收口。
- 本轮未执行远程数据库 SQL，因此只完成静态合同和脚本层收口，未验证真实库执行结果。

## 权限检查结果

- 本轮只改 SQL 静态脚本、SQL guard 测试和 Markdown 记录，不新增后端接口、不新增菜单、不新增按钮权限。
- 三端账号权限模型未改变。

## 字典/选项复用检查结果

- 本轮不新增字典、不新增前端选项、不修改 `sys_dict`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 SQL DDL 可重放 Helper 与条件 Modify Guard 条目，登记主迁移 preflight 与非事务混合迁移说明规则。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已是集中式 SQL 合同测试文件，本轮只新增一个针对主迁移 preflight 的合同方法，保持在既有职责内。
- `20260604_three_terminal_isolation_migration.sql` 本身是三端隔离主迁移脚本，本轮只前置 preflight 和补说明，不拆分主脚本。

## 重复代码检查结果

- 没有新增前端页面、service 或 Java 业务逻辑重复。
- SQL helper 是脚本内局部 helper，避免引入跨文件运行依赖；复用规则已写入台账供后续同类脚本参考。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest test

cd E:\Urili-Ruoyi
git diff --check
codegraph sync .
```

验证结果：
- `mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" test`：通过；`SqlExecutionGuardContractTest` 32 个测试通过，`TerminalSqlIsolationContractTest` 11 个测试通过。
- `git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `codegraph sync .`：通过；代码变更同步 2 个变更文件、77 个节点；记录回写后复跑同步 1 个变更文件、80 个节点。

## 未验证原因

- 本轮按用户要求不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 本轮不执行远程 MySQL DDL/DML，不读取或写入 Redis。
- 本轮未启动或重启后端。

## CodeGraph 更新结果

- `codegraph sync .` 已通过；代码变更同步 2 个变更文件、77 个节点；记录回写后复跑同步 1 个变更文件、80 个节点。
