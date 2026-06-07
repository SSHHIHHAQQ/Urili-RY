# 2026-06-07 三端 P0/P1 账号身份列 MODIFY 可重放 Guard 记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦 `20260604_three_terminal_isolation_migration.sql` 中 seller/buyer 账号身份列定义收敛的可重放风险；不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P1：`20260604_three_terminal_isolation_migration.sql` 原先对 `seller_account` / `buyer_account` 使用裸 `ALTER TABLE ... MODIFY user_name/nick_name/password`。该脚本在历史库、半迁移库或 future schema 漂移环境中重放时，会无条件触发表结构变更，也缺少缺列时的清晰 fail-closed 信息。

## 已修复问题

- `20260604_three_terminal_isolation_migration.sql` 新增 `modify_terminal_account_identity_columns_if_needed(...)`。
- helper 先通过 `information_schema.columns` 确认 `user_name` / `nick_name` / `password` 三列存在；缺列时主动 `signal sqlstate '45000'`，错误信息为 `terminal account identity columns are required before account identity modify`。
- helper 比对 `data_type`、`character_maximum_length`、`is_nullable` 和 `column_default`；只有当前定义不满足目标定义时才执行动态 `MODIFY`。
- seller/buyer 两处账号身份列收敛已从裸 `ALTER TABLE ... MODIFY` 改为 helper 调用，并保持在 legacy `user_id` 删除之后、username 唯一索引创建之前。
- `SqlExecutionGuardContractTest` 新增 `threeTerminalIsolationMigrationMustUseReplaySafeAccountModifyHelper()`，锁定 helper、缺列 fail-closed、定义比对、helper 调用顺序和禁止裸 `MODIFY` 回归。

## 残留问题

- P1：`sys_menu 2010` 仍存在 top owner + 兼容 seed 双写同签名，后续可单独收口 owner 兼容边界。
- P1：旧 `portal_direct_login:{token_hash}` Redis key 仍保留 30 分钟兼容读取/清理策略；完全移除 legacy fallback 后续可单独硬化。
- 本轮没有执行远程 SQL，真实数据库是否存在半迁移列漂移未在本切片验证。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" test`：通过，`SqlExecutionGuardContractTest` 28 个测试、`TerminalSqlIsolationContractTest` 10 个测试，合计 38 个测试通过。
- `cd E:\Urili-Ruoyi; rg -n "alter table seller_account modify|alter table buyer_account modify|modify_terminal_account_identity_columns_if_needed|terminal account identity columns" RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`：通过，旧裸 SQL 只存在于测试禁止字符串，迁移脚本本体只保留 helper 和调用。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮是 SQL 脚本与静态合同测试修复，不需要对远程库执行迁移。
- 未启动后端：本轮不涉及运行态接口或权限行为变更。

## 权限检查结果

- 本轮未改管理端、卖家端、买家端权限判断逻辑。
- 本轮只加强三端隔离迁移脚本的账号身份列 DDL 可重放性，不改变 `sys_*`、`seller_*`、`buyer_*` 权限语义。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。
- 不涉及国家/地区、状态、客户类型等选项字段。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，把 `modify_terminal_account_identity_columns_if_needed(...)` 纳入 SQL DDL 可重放 helper 与条件 modify guard 规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Synced 1 changed files`，`Modified: 1 - 59 nodes in 1.2s`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行 guard 合同；本轮只追加同类静态合同，不在快速推进切片中拆分。
- `20260604_three_terminal_isolation_migration.sql` 是既有大型三端隔离迁移脚本，本轮只替换账号身份列定义收敛段，不拆分整份迁移脚本，避免影响已确认的执行顺序。

## 重复代码检查结果

- 新 helper 只在 `20260604_three_terminal_isolation_migration.sql` 内局部使用，未复制 Java Service 或 React 业务逻辑。
- SQL 动态 DDL helper 与既有脚本内局部 helper 模式一致，符合当前 MySQL 脚本没有 include 机制的写法。

## 子 Agent 使用记录

- 本检查点未新增子 Agent。
- 复用当前已完成的 2 个 `gpt-5.4` 只读子 Agent 结论：裸账号身份列 `MODIFY` 是 P1 可重放风险，应使用脚本内条件 helper，并在 `SqlExecutionGuardContractTest` 中锁住。
- 后续如需新增子 Agent，按用户最新要求优先使用 `gpt-5.3-codex-spark`，不可用再回退 `gpt-5.4`。

## 一句话总结

本轮把三端隔离主迁移脚本中 seller/buyer 账号身份列的裸 `MODIFY` 收敛为条件执行 helper，并用静态合同锁住；未执行远程 SQL，下一批继续处理残留 P1 guard。
