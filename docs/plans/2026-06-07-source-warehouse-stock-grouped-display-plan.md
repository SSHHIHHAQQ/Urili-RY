# 来源仓库库存按来源 SKU 组合并展示永久读模型方案

日期：2026-06-07

状态：方案整理，未执行代码修改、未执行 DDL、未执行 DML。

## 目标

将「库存管理 / 来源仓库库存」从当前库存快照明细平铺列表，调整为按“同一个来源 SKU + 同一个商品名”合并展示。

期望展示结构类似商品商城列表中的 SPU/SKU 关系：

- 父级行：同一个来源 SKU 组的库存汇总。
- 子级明细：该来源 SKU 组下不同来源主仓、来源仓库、库存属性、批次、库位、系统仓库、商城 SKU、客户等库存快照明细。

本方案直接采用永久读模型表，不把动态 `group by` 作为第一实现。原因是来源仓库库存会长期按 SKU、商品名、库存口径、库存属性、仓库、批次、库位、配对状态和同步状态组合查询；如果每次列表请求都从 `upstream_system_sku_inventory_snapshot` 实时聚合，数据量上来后会反复触发大范围扫描、临时表、排序和分页 count。

本方案只处理来源仓库库存只读展示和只读查询契约，不处理平台真实库存、库存总览、库存流水、订单占用库存、财务库存或上游系统库存同步落库。

## 项目规则对齐

已阅读 `AGENTS.md`，本方案按其中的数据表设计确认规则整理。

当前边界：

- 后端业务改动只进入 `RuoYi-Vue/`。
- 当前管理端前端业务改动只进入 `react-ui/`。
- 新增库存读模型表前，必须先提交 Markdown 方案并确认。
- 未确认前不新增 `CREATE TABLE`、Entity、Mapper、Service、Controller、菜单权限或前端页面实现。
- 涉及远端数据库 DDL/DML 时，后续必须单独写执行记录，写明目标环境、连接来源、命令类型和影响范围。

本文件是设计方案，不是已落地实现。

## 已参考文件

- `docs/plans/2026-06-06-source-sku-group-contract-implementation-plan.md`
- `docs/plans/2026-06-07-source-product-library-permanent-read-model-design.md`
- `docs/plans/2026-06-07-source-warehouse-stock-list-display-adjust-record.md`
- `docs/plans/2026-06-07-upstream-inventory-empty-list-fix-record.md`
- `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
- `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql`
- `docs/architecture/reuse-ledger.md`

## 当前事实

### 当前明细事实源

当前来源仓库库存快照事实源：

```text
upstream_system_sku_inventory_snapshot
```

该表由上游库存同步链路维护，当前菜单只消费已落库的库存快照。

现有自然唯一键：

```text
connection_code
+ upstream_warehouse_code
+ master_sku
+ inventory_scope
+ inventory_attribute
+ batch_no
+ location_code
```

这说明当前快照明细天然会按来源连接、来源仓库、SKU、库存口径、库存属性、批次和库位拆成多行。它适合作为上游库存明细事实源，不适合作为管理端列表每次请求实时聚合的直接查询源。

### 当前菜单和接口

当前独立菜单：

```text
库存管理 / 来源仓库库存
```

当前接口：

```text
GET /integration/admin/source-warehouse-stocks/list
```

当前权限：

```text
inventory:sourceWarehouse:list
```

当前页面：

```text
react-ui/src/pages/Inventory/SourceWarehouseStock/index.tsx
```

已完成的展示调整：

- 菜单名为「来源仓库库存」。
- 字段「来源系统」已调整为「来源主仓」。
- 页面只展示主仓名，例如 `CA012`，不展示 `领星WMS` 或 `LX-CA012`。
- 库存口径已改成 Tabs，顺序为：综合库存、产品库存、退货库存、箱库存。
- 库存属性 `0` / `1` 已展示为 `正品` / `次品`。

## 业务边界

「来源仓库库存」表达：

```text
从来源主仓同步并落库后的来源库存快照，在库存管理下做只读观察。
```

它不表达：

- 平台真实可售库存。
- 库存总览。
- 订单锁定库存。
- 履约扣减库存。
- 库存流水。
- 财务库存。
- 对某个上游系统的实时库存直连。

上游系统未来可能不止一个，因此本菜单不能把表、接口和页面写死为领星库存。读模型字段可以保存当前来源系统类型和主仓名，但查询契约只认“来源主仓库存快照”，不直接认“领星库存”。

## 来源商品库方案的借鉴和边界

### 借鉴

来源商品库已经证明，长期列表不能一直从原始快照现场聚合。来源仓库库存应借鉴：

- 稳定分组 key。
- 父级摘要表。
- 结构化明细表。
- 同步或配对变更后维护读模型。
- 列表分页直接读读模型表，分页 total 直接 `count(*)`。
- 详情展开按稳定 key 查询结构化明细。

### 不照搬

不直接复用：

- `source_product_group`
- `source_product_dimension_group`
- `source_product_warehouse_detail`

原因：

来源商品库处理的是 SKU 基础资料、尺寸、申报、分类和来源仓明细。来源仓库库存处理的是库存数量事实，数量会按库存口径、库存属性、批次、库位和来源仓变化。两类事实不同，不能共用同一套读模型表。

## 总体设计结论

新增来源仓库库存读模型三张表：

```text
source_warehouse_stock_group
source_warehouse_stock_detail
source_warehouse_stock_filter_metric
```

职责：

| 表 | 粒度 | 用途 |
| --- | --- | --- |
| `source_warehouse_stock_group` | 一个库存口径下，一个来源 SKU + 商品名一行 | 父级列表直接读取，承载汇总库存和摘要字段 |
| `source_warehouse_stock_detail` | 一个上游库存快照明细一行 | 展开明细直接读取，避免再 join 快照和连接表 |
| `source_warehouse_stock_filter_metric` | 一个父级组在某个筛选维度上的指标一行 | 大数据下支撑常用筛选、属性筛选和后续指标校验 |

读模型是派生表，可以覆盖更新。事实源仍然是：

```text
upstream_system_sku_inventory_snapshot
```

读模型不保存上游原始 JSON，只保存页面和查询需要的结构化字段。原始 JSON 继续留在快照事实源。

## 分组规则

### 稳定基础组

父级业务分组：

```text
repositoryScope + inventoryScope + trim(masterSku) + trim(masterProductName)
```

其中：

- `repositoryScope` 第一版固定为 `OFFICIAL_MASTER`，用于未来兼容其他来源主仓范围。
- `inventoryScope` 必须进入 key，避免综合库存、产品库存、退货库存、箱库存混算。
- `masterSku` 和 `masterProductName` 只做 trim。
- 不做大小写归一。
- 不做相似商品名匹配。
- 不做人工别名合并。

建议 key 格式：

```text
SOURCE_STOCK:{sha256("repositoryScope|inventoryScope|len(masterSku):masterSku|len(masterProductName):masterProductName")}
```

### 不参与父级分组的字段

以下字段不参与父级分组：

- 来源主仓
- 来源仓库
- 库存属性
- 批次
- 库位
- 系统仓库
- 商城 SKU
- 客户
- 仓库配对状态
- SKU 配对状态
- 同步状态

它们进入摘要、明细或筛选指标表。

### 库存属性

库存属性不拆父级列表行。

父级行展示库存属性摘要：

- 只有 `0`：正品
- 只有 `1`：次品
- 同时存在 `0` 和 `1`：正品/次品
- 其他未知 code：按统一兜底规则展示原始 code

为了避免用户按库存属性筛选时再实时聚合，`source_warehouse_stock_filter_metric` 会预先保存 `filter_type = INVENTORY_ATTRIBUTE` 的指标行。

### 多主仓合并

同一个来源 SKU + 商品名如果同时存在于 `CA012`、`NY013` 等多个来源主仓，应合并到同一个父级行。

父级行展示来源主仓摘要和主仓数量，展开后展示每个来源主仓、来源仓库、库存属性、批次和库位的明细。

## 表设计

### 1. `source_warehouse_stock_group`

业务目的：

- 作为「来源仓库库存」父级列表的永久读模型。
- 避免列表接口每次从明细快照实时 `group by master_sku, master_product_name`。
- 保存父级行所需的库存汇总、仓库摘要、配对摘要、同步摘要和搜索文本。

业务逻辑：

- 一行表示一个库存口径下的一个来源 SKU 库存组。
- 可以覆盖更新。
- 不作为平台真实库存事实源。
- 不记录库存流水。
- 不保存上游原始 JSON。

建议字段：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `source_stock_group_key` | varchar(128) | 是 | 无 | 主键，稳定来源库存组 key |
| `repository_scope` | varchar(32) | 是 | `OFFICIAL_MASTER` | 来源范围，第一版官方主仓 |
| `inventory_scope` | varchar(32) | 是 | 无 | 库存口径：COMPREHENSIVE / PRODUCT / RETURN / BOX |
| `master_sku` | varchar(128) | 是 | 无 | 来源 SKU |
| `master_product_name` | varchar(255) | 是 | `''` | 来源商品名 |
| `inventory_attribute_codes` | varchar(200) | 是 | `''` | 组内库存属性 code 摘要，例如 `0,1` |
| `inventory_attribute_labels` | varchar(500) | 是 | `''` | 组内库存属性展示摘要，例如 `正品/次品` |
| `inventory_attribute_count` | int | 是 | `0` | 库存属性去重数量 |
| `source_connection_codes` | varchar(1000) | 是 | `''` | 来源连接编号摘要 |
| `master_warehouse_names` | varchar(1000) | 是 | `''` | 来源主仓名摘要，例如 `CA012 / NY013` |
| `master_warehouse_count` | int | 是 | `0` | 来源主仓去重数量 |
| `upstream_warehouse_codes` | varchar(1000) | 是 | `''` | 来源仓库 code 摘要 |
| `upstream_warehouse_names` | varchar(1000) | 是 | `''` | 来源仓库名称摘要 |
| `upstream_warehouse_count` | int | 是 | `0` | 来源仓库去重数量 |
| `detail_row_count` | int | 是 | `0` | 组内快照明细行数 |
| `active_detail_count` | int | 是 | `0` | ACTIVE 明细数 |
| `missing_detail_count` | int | 是 | `0` | MISSING 明细数 |
| `total_quantity` | bigint | 是 | `0` | 组内总库存汇总 |
| `available_quantity` | bigint | 是 | `0` | 组内可用库存汇总 |
| `locked_quantity` | bigint | 是 | `0` | 组内锁定库存汇总 |
| `in_transit_quantity` | bigint | 是 | `0` | 组内在途库存汇总 |
| `boxed_quantity` | bigint | 否 | null | 组内箱内库存汇总 |
| `unboxed_quantity` | bigint | 否 | null | 组内散件库存汇总 |
| `system_warehouse_codes` | varchar(1000) | 是 | `''` | 系统仓库 code 摘要 |
| `system_warehouse_names` | varchar(1000) | 是 | `''` | 系统仓库名称摘要 |
| `system_skus` | varchar(1000) | 是 | `''` | 商城 SKU 摘要 |
| `system_sku_names` | varchar(1000) | 是 | `''` | 商城 SKU 名称摘要 |
| `customer_names` | varchar(1000) | 是 | `''` | 客户名称摘要 |
| `warehouse_pairing_status` | varchar(32) | 是 | `UNASSIGNED` | 仓库配对汇总状态 |
| `sku_pairing_status` | varchar(32) | 是 | `UNASSIGNED` | SKU 配对汇总状态 |
| `status` | varchar(16) | 是 | `ACTIVE` | 同步汇总状态；混合时为 `MIXED` |
| `latest_sync_batch_id` | varchar(64) | 是 | `''` | 组内最近同步批次 |
| `first_seen_time` | datetime | 是 | 无 | 组内最早首次发现时间 |
| `last_seen_time` | datetime | 是 | 无 | 组内最近发现时间 |
| `latest_update_time` | datetime | 是 | 无 | 组内最近更新时间 |
| `search_text` | text | 是 | 无 | 搜索文本，包含 SKU、商品名、仓库、系统 SKU、客户等 |
| `rebuild_time` | datetime | 是 | 无 | 读模型最近构建时间 |

建议约束和索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| `pk_source_warehouse_stock_group` | `source_stock_group_key` | 父级 rowKey 和详情入口 |
| `uk_source_warehouse_stock_group_natural` | `repository_scope, inventory_scope, master_sku, master_product_name` | 防止同一组重复 |
| `idx_source_warehouse_stock_group_list` | `repository_scope, inventory_scope, latest_update_time, source_stock_group_key` | 列表排序分页 |
| `idx_source_warehouse_stock_group_sku` | `repository_scope, inventory_scope, master_sku` | SKU 查询 |
| `idx_source_warehouse_stock_group_status` | `repository_scope, inventory_scope, status` | 同步状态筛选 |
| `idx_source_warehouse_stock_group_wh_pairing` | `repository_scope, inventory_scope, warehouse_pairing_status` | 仓库配对筛选 |
| `idx_source_warehouse_stock_group_sku_pairing` | `repository_scope, inventory_scope, sku_pairing_status` | SKU 配对筛选 |

是否进入 `sys_dict`：

- `repository_scope` 后续可进入字典或前端集中选项。
- `inventory_scope`、`status`、配对状态复用 integration 现有选项。
- `MIXED` 如正式作为筛选项，应补集中选项；第一版可只展示不筛选。

### 2. `source_warehouse_stock_detail`

业务目的：

- 作为父级展开明细的永久读模型。
- 避免展开时再 join `upstream_system_sku_inventory_snapshot` 和 `upstream_system_connection`。
- 保存页面展示所需的结构化字段。

业务逻辑：

- 一行对应一个 `upstream_system_sku_inventory_snapshot.inventory_snapshot_id`。
- 可以覆盖更新。
- 不保存 `source_payload_json`。
- 不作为平台真实库存或库存流水。

建议字段：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `inventory_snapshot_id` | bigint | 是 | 无 | 主键，对应库存快照明细 ID |
| `source_stock_group_key` | varchar(128) | 是 | 无 | 所属来源库存组 key |
| `repository_scope` | varchar(32) | 是 | `OFFICIAL_MASTER` | 来源范围 |
| `connection_code` | varchar(64) | 是 | 无 | 来源连接编号 |
| `system_kind` | varchar(64) | 是 | `''` | 来源系统类型 |
| `master_warehouse_name` | varchar(128) | 是 | `''` | 来源主仓名 |
| `upstream_warehouse_code` | varchar(100) | 是 | 无 | 来源仓库 code |
| `upstream_warehouse_name` | varchar(200) | 是 | `''` | 来源仓库名称 |
| `master_sku` | varchar(128) | 是 | 无 | 来源 SKU |
| `master_product_name` | varchar(255) | 是 | `''` | 来源商品名 |
| `inventory_scope` | varchar(32) | 是 | 无 | 库存口径 |
| `inventory_attribute` | varchar(64) | 是 | `''` | 库存属性 code |
| `inventory_attribute_label` | varchar(100) | 是 | `''` | 库存属性展示名 |
| `batch_no` | varchar(128) | 是 | `''` | 批次号 |
| `location_code` | varchar(128) | 是 | `''` | 库位代码 |
| `total_quantity` | bigint | 是 | `0` | 总库存 |
| `available_quantity` | bigint | 是 | `0` | 可用库存 |
| `locked_quantity` | bigint | 是 | `0` | 锁定库存 |
| `in_transit_quantity` | bigint | 是 | `0` | 在途库存 |
| `boxed_quantity` | bigint | 否 | null | 箱内库存 |
| `unboxed_quantity` | bigint | 否 | null | 散件库存 |
| `system_warehouse_code` | varchar(64) | 是 | `''` | 系统仓库 code |
| `system_warehouse_name` | varchar(200) | 是 | `''` | 系统仓库名称 |
| `system_sku` | varchar(128) | 是 | `''` | 商城 SKU |
| `system_sku_name` | varchar(255) | 是 | `''` | 商城 SKU 名称 |
| `customer_name` | varchar(200) | 是 | `''` | 客户名称 |
| `warehouse_pairing_status` | varchar(32) | 是 | `UNASSIGNED` | 仓库配对状态 |
| `sku_pairing_status` | varchar(32) | 是 | `UNASSIGNED` | SKU 配对状态 |
| `status` | varchar(16) | 是 | `ACTIVE` | 同步状态 |
| `sync_batch_id` | varchar(64) | 是 | `''` | 同步批次 |
| `source_payload_hash` | varchar(64) | 是 | `''` | 来源库存行 hash |
| `first_seen_time` | datetime | 是 | 无 | 首次发现时间 |
| `last_seen_time` | datetime | 是 | 无 | 最近发现时间 |
| `update_time` | datetime | 是 | 无 | 快照更新时间 |
| `rebuild_time` | datetime | 是 | 无 | 读模型构建时间 |

建议约束和索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| `pk_source_warehouse_stock_detail` | `inventory_snapshot_id` | 一条快照对应一条明细读模型 |
| `idx_source_warehouse_stock_detail_group` | `source_stock_group_key, update_time` | 展开父级明细 |
| `idx_source_warehouse_stock_detail_connection` | `connection_code, status` | 按连接重建和排查 |
| `idx_source_warehouse_stock_detail_master_wh` | `repository_scope, inventory_scope, master_warehouse_name` | 来源主仓筛选 |
| `idx_source_warehouse_stock_detail_upstream_wh` | `repository_scope, inventory_scope, upstream_warehouse_code` | 来源仓库筛选 |
| `idx_source_warehouse_stock_detail_sku` | `repository_scope, inventory_scope, master_sku` | SKU 查询 |
| `idx_source_warehouse_stock_detail_attribute` | `repository_scope, inventory_scope, inventory_attribute` | 库存属性筛选 |
| `idx_source_warehouse_stock_detail_system_sku` | `repository_scope, inventory_scope, system_sku` | 商城 SKU 查询 |

### 3. `source_warehouse_stock_filter_metric`

业务目的：

- 为大数据量下的常用筛选提供预计算指标和倒排索引。
- 避免页面按库存属性、来源主仓、来源仓库、配对状态等筛选时回到明细快照实时聚合。
- 为后续核对“筛选后组数、数量、明细数”提供可校验的派生数据。

业务逻辑：

- 一行表示某个父级库存组在一个筛选维度上的预计算指标。
- 可以覆盖更新。
- 不承载业务事实，只承担读模型筛选和指标。

第一版建议维护的 `filter_type`：

| filter_type | filter_value 来源 |
| --- | --- |
| `INVENTORY_ATTRIBUTE` | `inventory_attribute` |
| `MASTER_WAREHOUSE` | `master_warehouse_name` |
| `UPSTREAM_WAREHOUSE` | `upstream_warehouse_code` |
| `SYSTEM_WAREHOUSE` | `system_warehouse_code` |
| `SYSTEM_SKU` | `system_sku` |
| `CUSTOMER` | `customer_name` |
| `STATUS` | `status` |
| `WAREHOUSE_PAIRING_STATUS` | `warehouse_pairing_status` |
| `SKU_PAIRING_STATUS` | `sku_pairing_status` |

建议字段：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `metric_key` | varchar(160) | 是 | 无 | 主键，`groupKey + filterType + filterValue` hash |
| `source_stock_group_key` | varchar(128) | 是 | 无 | 所属来源库存组 key |
| `repository_scope` | varchar(32) | 是 | `OFFICIAL_MASTER` | 来源范围 |
| `inventory_scope` | varchar(32) | 是 | 无 | 库存口径 |
| `filter_type` | varchar(64) | 是 | 无 | 筛选维度类型 |
| `filter_value` | varchar(255) | 是 | `''` | 筛选维度值 |
| `filter_label` | varchar(255) | 是 | `''` | 筛选维度展示值 |
| `detail_row_count` | int | 是 | `0` | 该维度命中的明细行数 |
| `total_quantity` | bigint | 是 | `0` | 该维度命中的总库存 |
| `available_quantity` | bigint | 是 | `0` | 该维度命中的可用库存 |
| `locked_quantity` | bigint | 是 | `0` | 该维度命中的锁定库存 |
| `in_transit_quantity` | bigint | 是 | `0` | 该维度命中的在途库存 |
| `boxed_quantity` | bigint | 否 | null | 该维度命中的箱内库存 |
| `unboxed_quantity` | bigint | 否 | null | 该维度命中的散件库存 |
| `latest_update_time` | datetime | 是 | 无 | 该维度命中的最近更新时间 |
| `rebuild_time` | datetime | 是 | 无 | 读模型构建时间 |

建议约束和索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| `pk_source_warehouse_stock_filter_metric` | `metric_key` | 指标唯一 |
| `uk_source_warehouse_stock_filter_metric_natural` | `source_stock_group_key, filter_type, filter_value` | 防重复 |
| `idx_source_warehouse_stock_filter_lookup` | `repository_scope, inventory_scope, filter_type, filter_value, source_stock_group_key` | 筛选时定位父级组 |
| `idx_source_warehouse_stock_filter_group` | `source_stock_group_key, filter_type` | 查询一个组的筛选指标 |

说明：

- 单个筛选维度，例如只筛 `库存属性 = 正品`，可以直接读取该指标行的数量。
- 多个筛选维度组合时，第一版以“定位父级组 + 展开明细过滤”为主；如果后续要求任意多维组合下父级数量也必须完全重算，需要再确认是否增加多维组合指标表。当前不建议提前做组合爆炸表。

## 读模型维护逻辑

### 触发点

读模型必须在以下事件后维护：

1. 上游 SKU 库存同步完成。
2. 上游库存快照标记 `MISSING` 完成。
3. 来源连接的 `master_warehouse_name` 或 `system_kind` 变化。
4. 仓库配对结果影响 `system_warehouse_code` / `system_warehouse_name` 时。
5. SKU 配对结果影响 `system_sku` / `system_sku_name` / `customer_name` 时。
6. 管理员手动触发读模型全量重建时。

### 推荐维护方式

第一版采用“受影响连接 + 受影响来源 SKU 组重建”。

库存同步某个连接：

```text
sync connection
-> upsert upstream_system_sku_inventory_snapshot
-> mark missing
-> 找出本次连接内受影响的 master_sku + master_product_name + inventory_scope
-> 扩展成 source_stock_group_key
-> 删除这些 key 的读模型 group/detail/filter_metric
-> 从 upstream_system_sku_inventory_snapshot 重建这些 key 的读模型
```

来源连接主仓名变化：

```text
update upstream_system_connection.master_warehouse_name
-> 找出该 connection_code 下所有 source_stock_group_key
-> 重建这些 key
```

配对变化：

```text
配对关系变化或快照内系统仓库/SKU字段变化
-> 找出受影响 connection_code + master_sku
-> 重建对应 source_stock_group_key
```

全量重建：

```text
truncate/delete read model for repository_scope = OFFICIAL_MASTER
-> 从 upstream_system_sku_inventory_snapshot 全量构建 detail
-> 聚合构建 group
-> 聚合构建 filter_metric
```

### 一致性原则

- `upstream_system_sku_inventory_snapshot` 是事实源。
- `source_warehouse_stock_*` 是读模型，可以覆盖更新。
- 外部请求日志、同步日志、库存流水如果未来存在，仍然只追加。
- 读模型重建失败不能污染快照事实源，应保留旧读模型并记录错误。
- 如果一次同步中部分读模型重建失败，页面应继续读取上一版可用读模型，并在同步状态或执行记录中暴露失败原因。

## 接口调整

### 父级分组列表

建议接口：

```text
GET /integration/admin/source-warehouse-stocks/groups/list
```

读取：

```text
source_warehouse_stock_group
```

分页：

```sql
select count(1)
from source_warehouse_stock_group
where repository_scope = ?
  and inventory_scope = ?
```

列表：

```sql
select ...
from source_warehouse_stock_group
where repository_scope = ?
  and inventory_scope = ?
order by latest_update_time desc, source_stock_group_key asc
limit ?, ?
```

筛选策略：

- `inventoryScope`：直接查 group 表。
- `keyword`：查 group 表 `search_text`，后续如数据量继续放大，可再引入搜索 token 表。
- `status`：优先查 group 表汇总状态。
- `warehousePairingStatus` / `skuPairingStatus`：优先查 group 表汇总状态。
- `inventoryAttribute`：通过 `source_warehouse_stock_filter_metric` 定位 group，并可使用指标数量。
- `masterWarehouseKeyword` / `warehouseKeyword`：第一版通过 group 摘要字段和 filter metric 定位 group。

### 子级展开明细

建议接口：

```text
GET /integration/admin/source-warehouse-stocks/groups/detail
```

读取：

```text
source_warehouse_stock_detail
```

查询参数：

| 参数 | 说明 |
| --- | --- |
| `sourceStockGroupKey` | 必填，父级来源库存组 key |
| `inventoryAttribute` | 可选，展开时只看某个库存属性 |
| `masterWarehouseName` | 可选，展开时只看某个来源主仓 |
| `upstreamWarehouseCode` | 可选，展开时只看某个来源仓库 |

第一版建议展开明细不分页；如果单组明细超过阈值，再补子级分页。

### 保留旧明细接口

建议暂时保留：

```text
GET /integration/admin/source-warehouse-stocks/list
```

用途：

- 回退。
- 排查明细。
- 上游系统工作台内连接级库存清单继续使用现有明细口径。

## 前端实现计划

页面继续使用：

```text
react-ui/src/pages/Inventory/SourceWarehouseStock/index.tsx
```

### 父级 ProTable

- `rowKey` 使用 `sourceStockGroupKey`。
- `request` 调用 `/groups/list`。
- 分页按 `source_warehouse_stock_group` 行数分页。
- Tabs 保持当前顺序：
  - 综合库存
  - 产品库存
  - 退货库存
  - 箱库存

父级列建议顺序：

1. 来源 SKU / 商品名称
2. 来源主仓
3. 来源仓库数
4. 库存属性
5. 总库存
6. 可用库存
7. 锁定库存
8. 在途库存
9. 箱内库存
10. 系统仓库
11. 商城 SKU
12. 客户
13. 仓库配对
14. SKU 配对
15. 同步状态
16. 更新时间

父级行不展示批次和库位，因为批次、库位是明细维度。

### 子级明细表

展开后调用 `/groups/detail`。

子级列建议顺序：

1. 来源主仓
2. 来源仓库
3. 库存属性
4. 总库存
5. 可用库存
6. 锁定库存
7. 在途库存
8. 箱内库存
9. 批次
10. 库位
11. 系统仓库
12. 商城 SKU
13. 客户
14. 仓库配对
15. SKU 配对
16. 同步状态
17. 同步时间

### 筛选区

继续复用：

```text
getPersistedProTableSearch(...)
getProTablePagination(...)
getProTableScroll(...)
getProTableColumnsState(...)
```

不新增自定义筛选容器。

### 选项和中文展示

继续集中复用：

```text
react-ui/src/services/integration/constants.ts
```

库存属性 `0 = 正品`、`1 = 次品` 应抽到 integration 共享选项中，不继续散落在页面内部。

## 权限、菜单和审计

### 权限

父级列表和子级明细都是同一页面的只读能力，建议复用：

```text
inventory:sourceWarehouse:list
```

本方案不新增：

- `inventory:sourceWarehouse:sync`
- `inventory:sourceWarehouse:export`
- `inventory:sourceWarehouse:rebuild`
- `inventory:sourceWarehouse:delete`

如果后续新增导出、手动重建、同步、批量处理，必须单独补按钮权限、后端权限和操作日志。

### 菜单

菜单仍为：

```text
库存管理 / 来源仓库库存
```

不新增新菜单。

### 审计

本阶段接口是只读查询，按当前若依查询接口惯例处理。

如果新增以下能力，必须加 `@Log`：

- 手动重建读模型。
- 导出。
- 同步。
- 批量处理。

## 字典和 code/label

### 库存口径

继续复用 integration 库存口径选项：

- `COMPREHENSIVE`：综合库存
- `PRODUCT`：产品库存
- `RETURN`：退货库存
- `BOX`：箱库存

### 库存属性

当前规则：

| code | label |
| --- | --- |
| `0` | 正品 |
| `1` | 次品 |

如果后续上游系统引入更多库存属性 code，优先扩展统一选项或字典，不在页面内硬编码多套映射。

### 汇总状态

读模型会派生：

- `MIXED`
- `PARTIAL`

第一版可以作为前端集中选项展示；如果要进入筛选条件或后台配置，应再确认是否写入 `sys_dict`。

## 初始化和迁移方案

### SQL 文件

建议新增：

```text
RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql
```

SQL 必须带显式确认 guard，类似：

```sql
set @confirm_source_warehouse_stock_read_model := coalesce(@confirm_source_warehouse_stock_read_model, '');
```

未设置确认 token 时必须 `signal sqlstate '45000'`。

### 初始回填

回填顺序：

1. 从 `upstream_system_sku_inventory_snapshot` 和 `upstream_system_connection` 构建 `source_warehouse_stock_detail`。
2. 从 `source_warehouse_stock_detail` 聚合构建 `source_warehouse_stock_group`。
3. 从 `source_warehouse_stock_detail` 聚合构建 `source_warehouse_stock_filter_metric`。
4. 抽样校验 group 数量、detail 数量和库存求和。

### 可回放性

读模型表是派生表，回填脚本可以按 `repository_scope` 删除后重建。

回填失败时：

- 不修改快照事实源。
- 删除或保留失败批次读模型由执行记录说明。
- 可以重新执行回填。

## 回滚方案

如果读模型上线后出现问题：

1. 前端临时切回旧 `/source-warehouse-stocks/list` 明细列表。
2. 后端保留旧明细接口。
3. 停止读模型重建任务或调用。
4. 读模型表是派生表，可清空或废弃，不影响 `upstream_system_sku_inventory_snapshot`。
5. 如已新增 SQL 文件但未执行远端 DDL，则无需数据库回滚。
6. 如已执行 DDL，回滚前必须确认是否有其他接口读取新表；确认后可 drop 三张读模型表。

## 验证计划

### DDL 验证

执行前：

- 检查 SQL guard。
- 检查表名、字段名、索引名是否符合项目命名规则。
- 检查是否没有写入 `sys_menu`、`sys_role_menu` 或真实业务库存表。

### 后端编译

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
```

### 前端检查

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/pages/Inventory/SourceWarehouseStock/index.tsx src/services/integration/sourceWarehouseStock.ts
npm run tsc -- --pretty false
```

### 数据校验

至少验证：

1. `source_warehouse_stock_detail` 行数等于当前有效快照行数。
2. 某个 `source_stock_group_key` 的 group 数量等于 detail 求和。
3. `total_quantity`、`available_quantity`、`locked_quantity`、`in_transit_quantity`、`boxed_quantity` 求和一致。
4. `inventory_attribute = 0` 的 filter metric 数量等于 detail 中正品明细求和。
5. 跨 `CA012` / `NY013` 的同 SKU + 商品名合并为一个 group。
6. 不同库存口径不会混入同一个 group。

建议抽样：

- 已知存在的 SKU：`2105115-silver-L`
- 再选一个跨 `CA012` / `NY013` 的来源 SKU。

### API 验证

验证：

- `/groups/list` 返回父级组，`total` 是 group 表行数。
- `/groups/detail` 返回该组明细。
- 父级汇总等于子级明细求和。
- 库存属性筛选不再扫描原始快照实时聚合。
- 页面不展示 `领星WMS` 或 `LX-CA012`。

### 浏览器验证

页面：

```text
http://127.0.0.1:8001/inventory/source-warehouse-stock
```

验证点：

1. 页面可从菜单进入和直达刷新。
2. Tabs 顺序保持「综合库存 / 产品库存 / 退货库存 / 箱库存」。
3. 父级列表不再按库存明细平铺。
4. 同一 SKU + 商品名合并为一个父级行。
5. 展开后能看到来源主仓、来源仓库、库存属性、批次、库位明细。
6. 库存属性展示中文。
7. 查询、重置、分页、展开不互相影响。

### CodeGraph

实现代码后执行：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

本方案文档阶段没有执行代码修改，因此本阶段不运行 CodeGraph。后续实现记录中必须写明 CodeGraph 执行结果。

## 风险和处理

| 风险 | 处理 |
| --- | --- |
| 读模型与快照短暂不一致 | 同步完成后重建受影响 group；重建失败保留上一版读模型并记录错误 |
| 多维筛选组合要求精确重算父级数量 | 第一版支持常用单维指标；多维组合如果必须精确，需要再确认组合指标表，不提前制造组合爆炸 |
| `search_text like` 在超大数据量下变慢 | 第一版先用 group 表摘要；如数据继续放大，再补搜索 token 表或专用全文搜索方案 |
| 同一 SKU 商品名轻微差异无法合并 | 第一版不模糊合并，保持确定性；后续通过上游数据治理处理 |
| 读模型字段过宽 | 父级只保留摘要，完整明细进入 detail 表，原始 JSON 不复制 |
| 误把来源库存当平台库存 | 文档、接口名、权限和页面文案都限定为来源库存快照，不写平台库存表 |

## 实施顺序

1. 确认本方案和三张读模型表。
2. 新增 guarded DDL SQL 文件，但不自动执行远端。
3. 新增读模型 DTO、Mapper、Service 构建能力。
4. 在库存同步完成后接入受影响 group 重建。
5. 增加全量重建能力，供初始回填和异常修复使用。
6. 新增 `/groups/list` 和 `/groups/detail` 只读接口。
7. 前端切换为父级 ProTable + 展开明细。
8. 运行 DDL 回填验证、后端编译、前端 lint/tsc、API 验证、浏览器验证。
9. 执行 `codegraph sync .`。
10. 生成 Markdown 实施记录，写清楚改动、验证、权限、字典、复用、CodeGraph 和残留问题。

## 待确认问题

当前推荐默认规则如下：

1. 父级按 `repositoryScope + inventoryScope + masterSku + masterProductName` 合并。
2. 不同来源主仓的同一 SKU + 商品名合并到同一个父级行。
3. 库存属性不拆父级行，只做父级摘要和 filter metric。
4. 父级数量默认展示整个 group 的数量。
5. 单个筛选维度可以使用 `source_warehouse_stock_filter_metric` 的数量。
6. 多个明细维度组合筛选时，第一版先保证命中 group 和展开明细准确；如果父级数量也必须按所有筛选组合精确重算，再单独确认组合指标设计。

## 当前结论

来源仓库库存应直接做永久读模型，不再把动态聚合作为第一阶段。

推荐落地形态：

```text
upstream_system_sku_inventory_snapshot  事实源
        |
        v
source_warehouse_stock_detail           展开明细读模型
        |
        +--> source_warehouse_stock_group          父级列表读模型
        |
        +--> source_warehouse_stock_filter_metric  常用筛选指标读模型
```

这样能满足“同一个 SKU 和商品名合并展示”的页面效果，也能提前处理数据量大、聚合复杂和分页 count 不稳定的问题，同时不把来源库存误写成平台真实库存或库存流水。
