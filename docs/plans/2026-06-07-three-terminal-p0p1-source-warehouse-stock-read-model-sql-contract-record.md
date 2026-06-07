# 2026-06-07 三端 P0/P1 来源仓库库存读模型 SQL Contract 收口记录

## 背景

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 当前模式：只修 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 本轮不做浏览器运行态验收、不做截图、不做 DOM 检测、不做 UI 细调。
- 本轮不执行远程 MySQL DDL/DML，不读取或写入 Redis。

## 子 Agent 使用

- `gpt-5.3-codex-spark` 当前因额度限制不可用，本轮按用户规则降级使用 `gpt-5.4`。
- 本切片启动 3 个 `gpt-5.4` 只读子 Agent，分别检查：
  - `20260607_source_warehouse_stock_read_model.sql` replay-safe 风险。
  - `SqlExecutionGuardContractTest` 既有合同风格和落点。
  - 相关 Markdown 残留记录。
- 3 个子 Agent 均已完成并关闭。

## 问题

- P0：`20260607_source_warehouse_stock_read_model.sql` 原先只有 confirm token，缺少源表/源列前置校验。确认后会先创建/清理读模型，再在回填时因 `upstream_system_sku_inventory_snapshot` 或 `upstream_system_connection` schema 漂移失败，存在先删读模型再失败的风险。
- P1：`source_warehouse_stock_group.search_text` 中多段 `group_concat(distinct ...)` 未指定 `order by`，同批源数据重放后摘要字符串顺序可能抖动。
- P1：该脚本此前主要依赖高影响 SQL 自动发现，缺少显式专项 SQL contract。

## 已完成

- `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql`
  - 新增 `assert_table_exists(...)`。
  - 新增 `assert_column_exists(...)`。
  - 在创建、删除和回填读模型前，校验 `upstream_system_sku_inventory_snapshot` / `upstream_system_connection` 及关键列存在。
  - 追加临时 staging 刷新：先写 `tmp_source_warehouse_stock_detail` / `tmp_source_warehouse_stock_group` / `tmp_source_warehouse_stock_filter_metric`，staging 全部成功后再在事务内替换 `OFFICIAL_MASTER` 正式范围。
  - `search_text` 的 `group_concat` 摘要增加 `order by`。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 将 `20260607_source_warehouse_stock_read_model.sql` 纳入显式高影响 SQL guard 清单。
  - 新增 `sourceWarehouseStockReadModelMustStayReplaySafeAndScoped()`。
  - 合同锁住源 schema 前置校验、三张读模型表、临时 staging 构建、事务内 `OFFICIAL_MASTER` 定向刷新、detail -> group -> filter_metric 回填顺序、禁止 destructive shortcut 和有序搜索摘要。
- 文档同步：
  - `docs/architecture/reuse-ledger.md`
  - `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
  - `docs/plans/2026-06-07-source-warehouse-stock-read-model-implementation-record.md`
  - `docs/plans/2026-06-07-three-terminal-p0p1-verify-list-drift-integration-coverage-record.md`

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 30 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn clean -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 31 个测试通过，确认没有吃旧 `target` 测试产物。
- `cd E:\Urili-Ruoyi; rg -n "source_warehouse_stock_read_model\.sql.*仍缺|仍缺.*source_warehouse_stock_read_model|仍缺显式专项 SQL contract|下一切片应补 source warehouse stock read model" docs\plans docs\architecture\reuse-ledger.md`：无命中。
- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue\sql\20260607_source_warehouse_stock_read_model.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-07-source-warehouse-stock-read-model-implementation-record.md docs\plans\2026-06-07-three-terminal-p0p1-verify-list-drift-integration-coverage-record.md docs\plans\2026-06-07-three-terminal-p0p1-source-warehouse-stock-read-model-sql-contract-record.md docs\plans\2026-06-07-three-terminal-p0p1-portal-js-sidecar-guard-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，`Synced 1 changed files`，`Modified: 1 - 61 nodes in 1.5s`。

## 未验证

- 未执行远程 SQL。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态验收、截图或 DOM 检测。

## 残留

- 历史残留已收口：`integration` fresh bootstrap schema 策略已由 `docs/plans/2026-06-07-three-terminal-p0p1-integration-bootstrap-chain-record.md` 固定为 bootstrap 后必跑 SQL 清单。
- 历史残留已收口：`source_product_read_model.sql` 已补对称专项 replay-safe contract。
- 历史残留已收口：`source_warehouse_stock_read_model.sql` 已改为临时 staging 表构建成功后，再在事务内替换 `OFFICIAL_MASTER` 正式范围。
- 尚未采用独立 swap-table / rename cutover；当前选择是低侵入的 temporary staging + final transaction copy。
