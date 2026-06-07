# Integration Fresh Bootstrap 必跑 SQL 清单

本清单固定 `integration` 模块在 fresh bootstrap 场景下的后置 SQL 顺序。前置条件仍是若依官方 bootstrap、`top_menu_seed.sql` 已完成，并且执行前已按当前激活数据源确认目标 MySQL/Redis。

## Fresh Bootstrap 必跑顺序

1. `RuoYi-Vue/sql/upstream_system_management_seed.sql`
   - 创建上游系统基础表、字典、菜单和按钮权限。
   - 依赖 `sys_menu 2030` 已由 top menu seed 提供。
2. `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql`
   - 创建 `upstream_system_sku_inventory_snapshot` 和 `upstream_system_inventory_sync_state`。
   - 来源仓库库存读模型依赖这一步。
3. `RuoYi-Vue/sql/20260606_upstream_sync_staging_diff.sql`
   - 补齐仓库、物流渠道、SKU payload/hash 列。
   - 创建 `upstream_system_sync_state`、`upstream_system_sync_batch` 和仓库/渠道/SKU staging 表。
   - 来源商品读模型依赖其中的 `wms_payload_hash`。
4. `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
   - 创建 `source_product_group`、`source_product_dimension_group`、`source_product_warehouse_detail`。
   - 从 `upstream_system_sku_candidate`、`upstream_system_connection`、`upstream_system_sku_pairing` 构建 `OFFICIAL_MASTER` 范围读模型。
5. `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql`
   - 创建 `source_warehouse_stock_group`、`source_warehouse_stock_detail`、`source_warehouse_stock_filter_metric`。
   - 从 `upstream_system_sku_inventory_snapshot` 构建 `OFFICIAL_MASTER` 范围读模型。

## 不吸收到基础 Seed 的原因

- `20260607_source_product_read_model.sql` 和 `20260607_source_warehouse_stock_read_model.sql` 不只是建表，还会按 `repository_scope = 'OFFICIAL_MASTER'` 清理并回填读模型数据；不应混入基础 seed 扩大执行影响面。
- `20260606_upstream_sync_staging_diff.sql` 同时承担老库补列、staging/state/batch 建表和 Quartz 任务拆分前的 job seed；保留为明确后置脚本更利于已有库重放。
- 基础 seed 只负责 `integration` 核心事实源、菜单和字典；读模型和同步 staging 作为 bootstrap 后必跑链路由本清单和 `SqlExecutionGuardContractTest` 固定。

## 已有库兼容补丁

- `RuoYi-Vue/sql/20260604_source_product_library_sku_candidate_fields.sql`：老库 SKU 候选字段补齐脚本；当前基础 seed 已包含这些字段，fresh bootstrap 不要求作为必跑步骤。
- `RuoYi-Vue/sql/20260607_upstream_pairing_role_binding.sql`：老库履约/报价配对用途补齐脚本；当前基础 seed 已包含 `pairing_role` 字段、索引和字典，fresh bootstrap 不要求作为必跑步骤。
- `RuoYi-Vue/sql/20260607_upstream_task_component_split.sql`：调度入口收口脚本；当前 Java 仍保留 `upstreamSystemTask` 兼容入口，fresh schema/read-model 链路不依赖它。

## 执行边界

- 远程库执行前必须按 AGENTS 数据源确认规则生成执行记录，并显式设置每个脚本要求的确认 token。
- 不能只运行 `upstream_system_management_seed.sql` 就认为 `integration` 管理端可用；Mapper 已硬依赖 staging/state/batch、库存快照和两个 read model。
- 如果未来决定把某个后置 schema 吸收到基础 seed，必须同时更新本清单、`upstream_system_management_seed.sql` 指向注释和 `SqlExecutionGuardContractTest`。
