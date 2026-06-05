# 来源商品库来源 SKU 字段落库执行记录

日期：2026-06-04

## 目标

为「来源商品库」补齐领星产品资料字段存储能力。前端展示字段可后续取舍，但领星 `/product/pagelist` 返回的产品基础资料需要先落库，避免只保存 `masterSku` 和产品名。

## 当前表结构

当前运行库 `upstream_system_sku_candidate` 只有以下字段：

- `connection_code`
- `master_sku`
- `master_product_name`
- `status`
- `search_text`
- `sync_batch_id`
- `first_seen_time`
- `last_seen_time`
- `update_time`

## 设计逻辑

- 扩展现有 `upstream_system_sku_candidate`，不另建重复来源商品表。
- 原因：现有主键 `(connection_code, master_sku)` 已经等于首版「来源系统 + 主仓 + SKU」粒度，`upstream_system_sku_pairing` 也依赖这个键。
- 该表仍是外部系统来源快照，不是商城商品事实表。
- 复杂数组字段先保存 JSON，不拆子表。
- 来源完整行保存 `source_payload_json` 和 `source_payload_hash`，便于后续字段追溯。

## 数据源确认

- 激活配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`，`spring.profiles.active=druid`
- MySQL 连接来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`
- 目标环境：远端 MySQL
- 目标库：`fenxiao`
- 执行命令类型：DDL + 定点 DML 回填
- 影响范围：新增来源商品快照字段和查询索引；回填最近一次成功领星 SKU 同步日志中的来源字段；不删除现有数据，不修改配对表和商城商品数据

## 字段来源

字段来自领星 `/product/pagelist` 文档和运行库 `SKU_SYNC` 脱敏响应：

- `sku`
- `productName`
- `productAliasName`
- `approveStatus`
- `productDescription`
- `imageUrl`
- `mainCode`
- `otherCode`
- `fnsku`
- `countryOfOriginName`
- `currencyCode`
- `customhouseCode`
- `dangerousCargo`
- `declareNameCn`
- `declareNameEn`
- `declarePrice`
- `height`
- `heightBs`
- `length`
- `lengthBs`
- `weight`
- `weightBs`
- `width`
- `widthBs`
- `wmsHeight`
- `wmsHeightBs`
- `wmsLength`
- `wmsLengthBs`
- `wmsWeight`
- `wmsWeightBs`
- `wmsWidth`
- `wmsWidthBs`
- `type`
- `cat1Name`
- `cat2Name`
- `cat3Name`
- `platformSkuInfoList`
- `brazilTaxInfoList`

## 仓库内 SQL

- 初始化脚本：`RuoYi-Vue/sql/upstream_system_management_seed.sql`
- 迁移脚本：`RuoYi-Vue/sql/20260604_source_product_library_sku_candidate_fields.sql`

## 执行结果

- 已执行 `RuoYi-Vue/sql/20260604_source_product_library_sku_candidate_fields.sql`。
- 执行语句数量：`5`
  - `ALTER TABLE upstream_system_sku_candidate ...`
  - `CREATE INDEX idx_upstream_sku_candidate_main_code ...`
  - `CREATE INDEX idx_upstream_sku_candidate_fnsku ...`
  - `CREATE INDEX idx_upstream_sku_candidate_approve ...`
  - `CREATE INDEX idx_upstream_sku_candidate_category ...`
- 现有数据处理：未删除；新增字段先默认空值或 `null`。
- 回填处理：已使用最近一次成功 `SKU_SYNC` 同步窗口的脱敏响应日志，按 `(connection_code, sku)` 回填来源快照字段、`source_payload_json`、`source_payload_hash` 和增强后的 `search_text`。
- 回填结果：解析 `5401` 条，更新 `5401` 条。
- 回填边界：未修改 `status`、`sync_batch_id`、`first_seen_time`、`last_seen_time`、配对表和商城商品数据。

## 验证结果

- 远端表字段数：`47`
- 已确认关键新增字段存在：
  - `product_alias_name`
  - `approve_status`
  - `product_type`
  - `image_url`
  - `main_code`
  - `other_code`
  - `fnsku`
  - `declare_price`
  - `wms_width_bs`
  - `platform_sku_info_json`
  - `brazil_tax_info_json`
  - `source_payload_json`
  - `source_payload_hash`
- 已确认索引存在：
  - `idx_upstream_sku_candidate_status`
  - `idx_upstream_sku_candidate_search`
  - `idx_upstream_sku_candidate_main_code`
  - `idx_upstream_sku_candidate_fnsku`
  - `idx_upstream_sku_candidate_approve`
  - `idx_upstream_sku_candidate_category`
- 回填后来源 SKU 统计：
  - `LX-CA012` 总行数：`5401`
  - `source_payload_hash` 非空：`5401`
  - `main_code` 非空：`5401`
  - `approve_status` 非空：`5401`
  - `product_description` 非空：`201`
  - `brazil_tax_info_json` 非空数组：`5401`
- `platform_sku_info_json` 非空数组：`0`
- `image_url` 非空：`0`
- `source_payload_json` 敏感字段抽查：`appKey` / `appSecret` 命中行数 `0`

## 验证命令

- `mvn -pl integration -am -DskipTests package`
  - 结果：通过，`BUILD SUCCESS`
  - 说明：本次只验证 integration 相关后端编译，测试被 `-DskipTests` 跳过。
- `codegraph sync .`
  - 结果：通过，`Already up to date`

## 残留说明

- 前端「来源商品库」列表首版展示字段还未在本记录中实现；本次先补来源字段存储、同步映射和远端数据回填。
- `platformSkuInfoList` / `brazilTaxInfoList` 首版按 JSON 快照保存，暂不拆子表。
- 来源商品和商城商品的匹配逻辑仍按已确认边界，留在「上游系统管理」内处理。
