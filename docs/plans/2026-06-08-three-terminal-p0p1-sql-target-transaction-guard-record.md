# 2026-06-08 三端隔离 P0/P1 快速推进：SQL 目标签名与事务 Guard 记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

## 本轮边界

- 只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 不执行远程 MySQL DDL/DML，不读取或写入 Redis。
- 历史记录（已过期口径）：本轮未再启动新的子 Agent；沿用本切片前置阶段已完成并关闭的 6 个只读 `gpt-5.4` 子 Agent 扫描结论。当前追补（2026-06-09）：后续子 Agent 默认且只能使用 `gpt-5.4`，不得再使用 GPT-5.3 Codex；旧的 GPT-5.3 优先表述已经作废。

## 采纳的 P1

1. `20260605_seller_account_lock_control.sql` / `20260605_buyer_account_lock_control.sql`
   - 问题：脚本会规范化已有账号的 `lock_status` / `lock_reason`，但只要求确认 token，没有锁定精确目标行集合。
   - 修复：增加 expected count/signature 变量、目标行 `group_concat + sha2` 签名、执行前断言和合同测试。

2. `20260608_inventory_overview_sku_baseline_refresh.sql`
   - 问题：脚本会批量 upsert `inventory_sku_warehouse_stock`，但只锁了 `NO_SOURCE` 字典行，未锁 5 类库存写入来源目标集合。
   - 修复：为 `OFFICIAL_MASTER`、`SOURCE_UNBOUND`、`UNMATCHED_OFFICIAL`、`THIRD_PARTY`、`NO_WAREHOUSE` 增加 expected count/signature 和执行前断言。
   - 问题：原事务只包住读模型删除/重建，字典更新和库存 upsert 不在同一事务内。
   - 修复：把 `start transaction` 前移到第一条 DML 之前，使字典更新、库存 upsert、读模型重建共用同一事务。

## 未采纳的候选项

- 子 Agent 提到 owner account bootstrap 仍使用默认密码 `U12346`。
- 判断：当前需求历史明确“创建登录密码默认 U12346”；AGENTS 中禁止的是“重置密码入口静默恢复默认密码”。新建主账号默认密码与重置密码语义不同，因此本轮不改。

## 已修改

- `RuoYi-Vue/sql/20260605_seller_account_lock_control.sql`
- `RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql`
- `RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`
  - 通过：53 个测试，0 failures，0 errors。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/sql/20260605_seller_account_lock_control.sql RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 通过；仅出现 Git 的 LF/CRLF 工作区换行提示。

## 当前残留

- 未执行远程 SQL，后续如要实际回放这些迁移，仍必须先按脚本要求在目标库预览每个 expected count/signature，并在执行记录里写清目标环境和确认变量。
- 未运行浏览器、截图、DOM 检测，符合本轮快速推进边界。
