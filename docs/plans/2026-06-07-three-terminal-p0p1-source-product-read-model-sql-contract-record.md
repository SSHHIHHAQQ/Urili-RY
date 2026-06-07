# 2026-06-07 三端 P0/P1 来源商品读模型 SQL Contract 收口记录

## 背景

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 当前模式：只修 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 本轮不做浏览器运行态验收、不做截图、不做 DOM 检测、不做 UI 细调。
- 本轮不执行远程 MySQL DDL/DML，不读取或写入 Redis。

## 子 Agent 使用

- `gpt-5.3-codex-spark` 当前因额度限制不可用，本轮按用户规则降级使用 `gpt-5.4`。
- 本切片启动 2 个 `gpt-5.4` 只读子 Agent，分别检查：
  - `20260607_source_product_read_model.sql` replay-safe 风险。
  - 相关 Markdown 残留记录。
- 2 个子 Agent 均已完成并关闭。

## 问题

- P0：`source_product_warehouse_detail` 原唯一键为 `(repository_scope, connection_code, master_sku)`，但明细行实际包含 `source_dimension_group_key` 和尺寸字段。同一连接同一来源 SKU 存在多尺寸候选行时，后写入行会覆盖前一行。
- P1：`20260607_source_product_read_model.sql` 原先主要依赖通用高影响 SQL guard，缺少显式专项 SQL contract。
- P1：脚本缺少源表/源列前置校验，确认后可能先删除读模型，再因上游 schema 漂移回填失败。
- P1：`source_product_group.search_text` / `source_product_dimension_group.search_text` 存在无序 `group_concat` 摘要抖动风险。

## 已完成

- `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
  - 新增 `assert_table_exists(...)`。
  - 新增 `assert_column_exists(...)`。
  - 在创建、删除和回填读模型前，校验 `upstream_system_sku_candidate` / `upstream_system_connection` / `upstream_system_sku_pairing` 及关键列存在。
  - 追加临时 staging 刷新：先写 `tmp_source_product_group` / `tmp_source_product_dimension_group` / `tmp_source_product_warehouse_detail`，staging 全部成功后再在事务内替换 `OFFICIAL_MASTER` 正式范围。
  - `search_text` 的 `group_concat` 摘要增加 `order by`。
  - `source_product_warehouse_detail` 唯一键改为 `(repository_scope, connection_code, master_sku, source_dimension_group_key)`。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 新增 `sourceProductReadModelMustStayReplaySafeAndScoped()`。
  - 合同锁住源 schema 前置校验、三张读模型表、临时 staging 构建、事务内 `OFFICIAL_MASTER` 定向刷新、禁止 destructive shortcut、包含 dimension key 的明细唯一键和有序搜索摘要。
- 文档同步：
  - `docs/architecture/reuse-ledger.md`
  - `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
  - `docs/plans/2026-06-07-source-product-library-read-model-implementation-record.md`

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 31 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn clean -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 31 个测试通过，确认没有吃旧 `target` 测试产物。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue\sql\20260607_source_product_read_model.sql RuoYi-Vue\sql\20260607_source_warehouse_stock_read_model.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-07-source-product-library-read-model-implementation-record.md docs\plans\2026-06-07-three-terminal-p0p1-source-product-read-model-sql-contract-record.md docs\plans\2026-06-07-source-warehouse-stock-read-model-implementation-record.md docs\plans\2026-06-07-three-terminal-p0p1-source-warehouse-stock-read-model-sql-contract-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，`Synced 1 changed files`，`Modified: 1 - 62 nodes in 948ms`。

## 未验证

- 未执行远程 SQL。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态验收、截图或 DOM 检测。

## 残留

- 历史残留已收口：`source_product_read_model.sql` 已改为临时 staging 表构建成功后，再在事务内替换 `OFFICIAL_MASTER` 正式范围。
- 尚未采用独立 swap-table / rename cutover；当前选择是低侵入的 temporary staging + final transaction copy。
- 历史残留已收口：`integration` fresh bootstrap schema 策略已由 `docs/plans/2026-06-07-three-terminal-p0p1-integration-bootstrap-chain-record.md` 固定为 bootstrap 后必跑 SQL 清单。
