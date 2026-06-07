# 2026-06-07 三端 P0/P1 Integration Fresh Bootstrap 必跑链路收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本切片收口 `integration` fresh bootstrap schema 策略，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 背景

此前残留问题是：`integration` 模块的 Mapper 已硬依赖 staging/state/batch、来源商品读模型、来源仓库库存读模型等 schema，但 fresh 环境如果只跑 `upstream_system_management_seed.sql`，会缺少这些表或列。需要明确是吸收到基础 seed，还是固定为 bootstrap 后必跑 SQL 链路。

## 结论

本轮选择固定为 bootstrap 后必跑 SQL 链路，不把两个 read model 脚本吸收到基础 seed。

原因：

- `20260607_source_product_read_model.sql` 和 `20260607_source_warehouse_stock_read_model.sql` 不只是建表，还会按 `repository_scope = 'OFFICIAL_MASTER'` 清理并回填读模型数据。
- `20260606_upstream_sync_staging_diff.sql` 同时承担老库补列、staging/state/batch 建表和 Quartz job 调整职责，保留为后置脚本更利于已有库重放。
- 基础 seed 继续只负责 `integration` 核心事实源、字典、菜单和权限；读模型与同步 staging 由文档清单和静态合同固定。

## 已落地

- 新增 `docs/architecture/integration-bootstrap-required-sql.md`，固定 fresh bootstrap 必跑顺序：
  1. `RuoYi-Vue/sql/upstream_system_management_seed.sql`
  2. `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql`
  3. `RuoYi-Vue/sql/20260606_upstream_sync_staging_diff.sql`
  4. `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
  5. `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql`
- `RuoYi-Vue/sql/upstream_system_management_seed.sql` 增加指向该清单的注释。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java` 新增 `integrationFreshBootstrapMustDeclareMandatoryPostSeedSqlChain()`：
  - 断言清单包含并按顺序声明 5 个必跑脚本。
  - 复用 `assertGuard(...)` 锁定每个脚本的确认 token 和确认调用位置。
  - 断言基础 seed 不直接包含 staging/state/batch、库存快照、来源商品读模型、来源仓库库存读模型和 read model 清理回填语句。
  - 断言后置脚本仍分别持有库存快照、staging/state/batch、来源商品读模型和来源仓库库存读模型 schema。
- `docs/architecture/reuse-ledger.md` 已同步该复用/执行边界。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 已追加本检查点。

## Fresh 与已有库边界

- Fresh bootstrap 必跑：上游系统基础 seed、库存快照、staging/state/batch、来源商品 read model、来源仓库库存 read model。
- 已有库兼容补丁：
  - `20260604_source_product_library_sku_candidate_fields.sql`：基础 seed 已包含来源商品候选字段，fresh 不要求必跑。
  - `20260607_upstream_pairing_role_binding.sql`：基础 seed 已包含 `pairing_role` 字段、索引和字典，fresh 不要求必跑。
  - `20260607_upstream_task_component_split.sql`：调度入口收口脚本；当前 Java 仍保留 `upstreamSystemTask` 兼容入口，fresh schema/read-model 链路不依赖它。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`34` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue\sql\upstream_system_management_seed.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java docs\architecture\integration-bootstrap-required-sql.md docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-07-three-terminal-p0p1-integration-bootstrap-chain-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，`Synced 1 changed files`。

## 子 Agent

- 按用户最新要求优先尝试 `gpt-5.3-codex-spark`。
- `019ea0ed-0ac9-7382-8217-d0a0cde544d0`：只读 explorer，因上下文窗口失败关闭；未采纳输出。
- `019ea0ed-7a23-7e61-bfbc-308676ce2e2d`：只读 explorer 完成，建议在 `SqlExecutionGuardContractTest` 中复用 `assertGuard(...)` 固定后置链路；已采纳。
- `019ea0ed-48ea-70d0-bc11-6c6b4abc2956`：只读 explorer 未返回有效结果，已关闭；主线未等待其结果。

## 未执行事项

- 未执行数据库 DDL/DML；如需对远程库执行，必须按当前激活数据源确认、逐脚本设置确认 token，并生成执行记录。
- 未做浏览器运行态验收、截图或 DOM 检测，符合本轮快速推进边界。
