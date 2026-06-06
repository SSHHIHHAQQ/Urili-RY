# 来源商品库性能永久方案：来源 SKU 组读模型

## 背景

当前来源商品库列表直接从 `upstream_system_sku_candidate` 动态聚合：

- `upstream_system_sku_candidate`：上游 SKU 快照明细。
- `upstream_system_connection`：来源仓库和系统类型。
- `upstream_system_sku_pairing`：系统 SKU 配对关系。

列表每次请求都要现场计算：

- `sourceSkuGroupKey`
- `sourceDimensionGroupKey`
- 仓库列表、仓库数量、来源行数量
- 客户尺寸和 WMS 尺寸拆分
- 配对状态
- `group_concat`
- `group by`
- `order by max(update_time)`
- 分页 total

这在几千行时还能接受，但数据到 50000 行后，请求路径会反复做全量聚合和临时排序。即使把 PageHelper 自动 count 改掉，主查询仍然不是长期方案。

## 结论

不要继续把来源商品库做成实时动态聚合查询。

应新增“来源 SKU 组读模型”表，由上游同步链路和配对变更链路维护。来源商品库列表、详情抽屉、后续商品列表绑定来源 SKU，都消费这套读模型。

核心目标：

1. 列表接口不再执行大聚合 SQL。
2. 列表分页 total 变成普通 `count(*)`。
3. 列表排序走读模型索引。
4. 商品列表侧绑定稳定 `sourceSkuGroupKey`，不重新拼聚合规则。
5. 来源仓库明细可通过 `sourceSkuGroupKey` 快速查询。

## 不采用的方案

### 1. 继续优化当前大 SQL

例如两段式查询、先查当前页 key 再聚合详情。

不采用为最终方案，因为它仍然每次请求都依赖 `upstream_system_sku_candidate` 做动态分组，数据继续增长后仍会受到全表扫描、表达式计算、临时表和排序影响。

### 2. 前端缓存或接口缓存

不采用为最终方案，因为来源商品库后续要作为商品创建、快捷创建、商品列表绑定的事实入口。缓存只能缓解重复点击，不能解决可查询性、分页 total、外部按 SKU 查询仓库能力的问题。

## 表设计

### 1. `source_product_group`

来源 SKU 组主表。一个真实来源 SKU 组一行。

聚合粒度：

```text
repository_scope + master_sku + master_product_name
```

用途：

- 商品列表侧保存和查询的主入口。
- 外部只按来源 SKU 查询时，先命中该表。
- 保存一个来源 SKU 组对应的仓库数量、仓库名、来源连接编号。

建议字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| source_sku_group_key | varchar(96) | 是 | 主键，稳定来源 SKU 组 key |
| repository_scope | varchar(32) | 是 | `OFFICIAL_MASTER` / `THIRD_PARTY_MASTER` |
| master_sku | varchar(128) | 是 | 来源 SKU |
| master_product_name | varchar(255) | 是 | 来源商品名 |
| system_kind | varchar(64) | 是 | 来源系统类型，当前官方主仓为 `lingxing-wms` |
| source_connection_codes | varchar(1000) | 是 | 逗号拼接的来源连接编号 |
| source_warehouse_names | varchar(1000) | 是 | 展示用仓库名 |
| warehouse_count | int | 是 | 来源仓库数量 |
| source_row_count | int | 是 | 来源明细行数量 |
| pairing_status | varchar(32) | 是 | `UNASSIGNED` / `PAIRED` / `PARTIAL` |
| status | varchar(16) | 是 | `ACTIVE` / `MISSING` / `MIXED` |
| latest_update_time | datetime | 是 | 组内最新更新时间 |
| search_text | text | 是 | 搜索文本 |
| rebuild_time | datetime | 是 | 读模型最近构建时间 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_source_product_group | source_sku_group_key | 商品侧稳定引用 |
| idx_source_product_group_sku | repository_scope, master_sku | 外部按 SKU 查询来源组 |
| idx_source_product_group_list | repository_scope, latest_update_time, source_sku_group_key | 列表排序分页 |
| idx_source_product_group_status | repository_scope, status | 状态筛选 |
| idx_source_product_group_pairing | repository_scope, pairing_status | 配对状态筛选 |

### 2. `source_product_dimension_group`

来源商品库列表表。一个“来源 SKU 组 + 一组客户尺寸 + 一组 WMS 尺寸”一行。

聚合粒度：

```text
source_sku_group_key
+ product_length/product_width/product_height/product_weight
+ wms_length/wms_width/wms_height/wms_weight
```

用途：

- 来源商品库列表直接查询该表。
- 客户尺寸和仓库尺寸不同时，在这里拆成多行。
- 页面不再现场做尺寸拆分和仓库聚合。

建议字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| source_dimension_group_key | varchar(128) | 是 | 主键，稳定尺寸组 key |
| source_sku_group_key | varchar(96) | 是 | 所属来源 SKU 组 |
| repository_scope | varchar(32) | 是 | 仓库范围 |
| master_sku | varchar(128) | 是 | 来源 SKU |
| master_product_name | varchar(255) | 是 | 来源商品名 |
| product_alias_name | varchar(255) | 否 | 来源别名 |
| approve_status | varchar(32) | 否 | 审核状态 |
| product_type | int | 否 | 来源产品类型 |
| image_url | varchar(1000) | 否 | 图片 |
| main_code | varchar(128) | 否 | 主条码 |
| other_code | varchar(1000) | 否 | 其他条码 |
| fnsku | varchar(1000) | 否 | FNSKU |
| cat1_name/cat2_name/cat3_name | varchar(100) | 否 | 分类 |
| dangerous_cargo | int | 否 | 危险品 code |
| declare_name_cn | varchar(255) | 否 | 申报中文名 |
| declare_name_en | varchar(255) | 否 | 申报英文名 |
| declare_price | decimal(18,4) | 否 | 申报价 |
| currency_code | varchar(16) | 否 | 币种 code |
| product_length/product_width/product_height/product_weight | decimal(18,4) | 否 | 客户尺寸重量 |
| wms_length/wms_width/wms_height/wms_weight | decimal(18,4) | 否 | WMS 尺寸重量 |
| source_connection_codes | varchar(1000) | 是 | 该尺寸组覆盖的连接编号 |
| source_warehouse_names | varchar(1000) | 是 | 该尺寸组覆盖的仓库名 |
| warehouse_count | int | 是 | 该尺寸组覆盖的仓库数量 |
| source_row_count | int | 是 | 该尺寸组来源行数量 |
| pairing_status | varchar(32) | 是 | 配对状态 |
| system_sku | varchar(1000) | 否 | 配对系统 SKU 摘要 |
| system_sku_name | varchar(1000) | 否 | 配对系统商品名摘要 |
| customer_name | varchar(1000) | 否 | 客户名摘要 |
| status | varchar(16) | 是 | 同步状态 |
| first_seen_time | datetime | 是 | 首次发现时间 |
| last_seen_time | datetime | 是 | 最近发现时间 |
| update_time | datetime | 是 | 最新更新时间 |
| search_text | text | 是 | 搜索文本 |
| rebuild_time | datetime | 是 | 读模型最近构建时间 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| pk_source_product_dimension_group | source_dimension_group_key | 列表 rowKey |
| idx_source_product_dimension_group_list | repository_scope, update_time, source_dimension_group_key | 列表排序分页 |
| idx_source_product_dimension_group_sku_group | source_sku_group_key | 详情抽屉按组查尺寸 |
| idx_source_product_dimension_group_master_sku | repository_scope, master_sku | SKU 查询 |
| idx_source_product_dimension_group_status | repository_scope, status | 状态筛选 |
| idx_source_product_dimension_group_pairing | repository_scope, pairing_status | 配对状态筛选 |
| idx_source_product_dimension_group_approve | repository_scope, approve_status | 审核状态筛选 |

### 3. `source_product_warehouse_detail`

来源 SKU 组仓库明细表。一个来源仓库明细一行。

用途：

- 来源商品库详情抽屉展示官方仓明细。
- 商品列表侧或快捷创建商品时，按 `sourceSkuGroupKey` 查询这个来源 SKU 有哪些仓库可用。
- 后续外部接口按 SKU 查询仓库能力时直接读该表。

建议字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | bigint | 是 | 主键 |
| source_sku_group_key | varchar(96) | 是 | 来源 SKU 组 |
| source_dimension_group_key | varchar(128) | 是 | 所属尺寸组 |
| repository_scope | varchar(32) | 是 | 仓库范围 |
| connection_code | varchar(64) | 是 | 来源连接编号 |
| master_warehouse_name | varchar(128) | 是 | 来源仓库名 |
| system_kind | varchar(64) | 是 | 来源系统类型 |
| master_sku | varchar(128) | 是 | 来源 SKU |
| master_product_name | varchar(255) | 是 | 来源商品名 |
| product_length/product_width/product_height/product_weight | decimal(18,4) | 否 | 客户尺寸重量 |
| wms_length/wms_width/wms_height/wms_weight | decimal(18,4) | 否 | WMS 尺寸重量 |
| status | varchar(16) | 是 | 同步状态 |
| pairing_status | varchar(32) | 是 | 配对状态 |
| sku_pairing_id | bigint | 否 | 配对 ID |
| system_sku | varchar(128) | 否 | 系统 SKU |
| system_sku_name | varchar(255) | 否 | 系统商品名 |
| customer_name | varchar(200) | 否 | 客户名 |
| source_payload_hash | varchar(64) | 否 | 产品快照 hash |
| wms_payload_hash | varchar(64) | 否 | WMS 尺寸快照 hash |
| first_seen_time | datetime | 是 | 首次发现时间 |
| last_seen_time | datetime | 是 | 最近发现时间 |
| update_time | datetime | 是 | 更新时间 |
| rebuild_time | datetime | 是 | 读模型最近构建时间 |

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| uk_source_product_warehouse_row | repository_scope, connection_code, master_sku | 防重复 |
| idx_source_product_warehouse_sku_group | source_sku_group_key | 按来源 SKU 组查仓库 |
| idx_source_product_warehouse_dimension_group | source_dimension_group_key | 按尺寸组查仓库 |
| idx_source_product_warehouse_master_sku | repository_scope, master_sku | 外部按 SKU 查仓库 |
| idx_source_product_warehouse_connection | connection_code | 来源连接变更重建 |

## key 规则

### `sourceSkuGroupKey`

稳定表达“同一个真实来源 SKU 组”。

```text
repository_scope + master_sku + master_product_name
```

建议由 Java 统一生成，不再在列表 SQL 里现场 `sha2(...)`：

```text
OFFICIAL_MASTER:{sha256("OFFICIAL_MASTER|len(masterSku):masterSku|len(masterProductName):masterProductName")}
```

### `sourceDimensionGroupKey`

稳定表达“同一个来源 SKU 组下的一组尺寸”。

```text
sourceSkuGroupKey
+ product_length/product_width/product_height/product_weight
+ wms_length/wms_width/wms_height/wms_weight
```

建议格式：

```text
{sourceSkuGroupKey}:{sha256(dimensionSignature)}
```

## 同步维护逻辑

### 触发点

读模型必须在这些事件后更新：

1. SKU 基础资料同步完成。
2. WMS 尺寸重量同步完成。
3. 上游全量快照标记 `MISSING` 完成。
4. SKU 配对新增、修改、删除完成。
5. 来源连接的仓库名、系统类型发生变更。
6. 管理员手动触发读模型重建。

### 推荐维护方式

第一阶段采用“受影响连接重建”，不是请求时临时聚合：

```text
同步某个 connection_code
-> upsert upstream_system_sku_candidate
-> 标记 missing
-> rebuild source_product_* read model for this connection_code
```

配对变更时采用“受影响 SKU 组重建”：

```text
变更 upstream_system_sku_pairing(connection_code, master_sku)
-> 找到对应 sourceSkuGroupKey
-> 重建该 sourceSkuGroupKey 的 group / dimension_group / warehouse_detail
```

后续如果数据超过 50 万，再把“受影响连接重建”细化成“受影响 sourceSkuGroupKey 批量重建”。

### 一致性原则

- `upstream_system_sku_candidate` 仍然是上游快照明细源。
- `source_product_*` 是读模型，可以覆盖更新。
- 外部请求日志、同步日志、审计日志仍然只追加。
- 读模型重建失败不能污染候选明细，应记录错误并保留上一版可读数据。

## 接口调整

### 来源商品库列表

现有接口保持：

```text
GET /integration/admin/source-products/list
```

内部改为：

```sql
select ...
from source_product_dimension_group
where repository_scope = ?
order by update_time desc, source_dimension_group_key asc
limit ?, ?
```

total：

```sql
select count(1)
from source_product_dimension_group
where repository_scope = ?
```

列表接口不再 join `upstream_system_sku_candidate`，不再做 `group by`。

### 来源 SKU 组详情

现有接口保持：

```text
GET /integration/admin/source-products/group-detail
```

内部改为：

- `source_product_group` 查组摘要。
- `source_product_dimension_group` 查尺寸拆分。
- `source_product_warehouse_detail` 查仓库明细。

### 商品列表侧消费

商品列表或快捷创建商品不应该再绑定单条：

```text
connection_code + master_sku
```

应该保存：

```text
source_ref_type = SOURCE_SKU_GROUP
source_ref_id = sourceSkuGroupKey
```

查询来源仓库能力时：

```sql
select *
from source_product_warehouse_detail
where source_sku_group_key = ?
order by master_warehouse_name asc, connection_code asc
```

## 前端影响

来源商品库页面字段和交互可以基本不变。

需要确认的变化：

1. 列表返回仍然是尺寸组行，即 `SourceProductItem`。
2. `rowKey` 继续使用 `sourceDimensionGroupKey`。
3. 详情抽屉继续按 `sourceSkuGroupKey` 查询。
4. 仓库尺寸为空时仍展示 `-`。
5. 不再显示“一致 / 不一致”状态文案。

## 权限和审计

列表和详情继续复用当前权限：

```text
product:list:list
```

如果新增以下能力，需要单独补权限和审计：

- 手动重建来源商品读模型。
- 快捷创建商品。
- 批量从来源 SKU 组创建商品。
- 导出来源商品库。

建议新增权限点：

| 权限 | 用途 |
| --- | --- |
| product:source:rebuild | 手动重建来源商品读模型 |
| product:source:createProduct | 从来源 SKU 快捷创建商品 |
| product:source:export | 导出来源商品库 |

## 字典和 code/label

- `repository_scope` 建议进入字典或前端集中枚举：`OFFICIAL_MASTER` / `THIRD_PARTY_MASTER`。
- `pairing_status` 继续复用现有 integration 配对状态选项。
- `status` 继续复用同步状态选项。
- `system_kind` 继续复用上游系统类型选项。
- 金额字段 `declare_price` 继续使用 `decimal(18,4)`，Java 使用 `BigDecimal`。

## 迁移计划

### 第一步：DDL 方案确认

确认三张表：

- `source_product_group`
- `source_product_dimension_group`
- `source_product_warehouse_detail`

确认索引、字段长度、key 规则。

未确认前不执行 DDL。

### 第二步：后端读模型构建器

新增 Service 内部构建能力：

```text
rebuildByConnectionCode(connectionCode)
rebuildBySourceSkuGroupKey(sourceSkuGroupKey)
rebuildAllOfficialMaster()
```

构建逻辑集中在 integration 模块，不放到 Controller。

### 第三步：同步链路接入

在 SKU 基础资料同步、WMS 尺寸同步、配对变更后调用重建方法。

### 第四步：接口切换

把 `/list` 和 `/group-detail` 从动态聚合 SQL 切到读模型表。

### 第五步：历史数据回填

执行一次全量回填：

```text
upstream_system_sku_candidate
-> source_product_group
-> source_product_dimension_group
-> source_product_warehouse_detail
```

### 第六步：验证

必须验证：

1. 来源商品库第一页加载时间。
2. 50000 行候选明细下分页稳定性。
3. `KATGJ-SS-B885` 聚合为 1 个来源 SKU 组。
4. `KATGJ-SS-B885` 仓库明细包含 `CA012` / `NY013`。
5. 客户尺寸和 WMS 尺寸相同时只显示一行。
6. 多个仓库 WMS 尺寸不同则拆成多行。
7. `sourceSkuGroupKey` 在 WMS 尺寸补采后不变化。
8. 配对新增、删除后读模型配对状态更新。

## 回滚方案

1. 保留当前动态聚合 Mapper 作为短期回滚入口。
2. 接口切换时使用 Service 层开关，必要时临时切回旧查询。
3. 新表为读模型，不作为唯一事实源；回滚时可清空或停用，不影响 `upstream_system_sku_candidate`。
4. 如果回填失败，只删除读模型数据并重新构建，不回滚候选明细。

## 风险和处理

| 风险 | 处理 |
| --- | --- |
| 读模型与候选明细短暂不一致 | 同步完成后统一重建；重建失败保留上一版，并记录错误 |
| 配对状态滞后 | 配对写接口后同步重建对应来源 SKU 组 |
| 仓库名变更后摘要不更新 | 来源连接保存后重建该 connection_code 关联读模型 |
| 字段过宽 | 列表表只保留展示摘要；原始 JSON 仍留在候选明细，不复制到读模型 |
| 第三方主仓未实现 | 表结构预留 `repository_scope`，但同步构建先只写 `OFFICIAL_MASTER` |

## 推荐实施顺序

1. 先确认本方案和表结构。
2. 新增 DDL SQL 文件，但不自动执行远端。
3. 实现读模型构建 Service 和 Mapper。
4. 加一个只读校验接口或测试方法，对比旧动态聚合和新读模型行数。
5. 回填远端数据。
6. 切换来源商品库列表读取新表。
7. 商品列表侧后续只消费 `sourceSkuGroupKey` 和仓库明细表。

## 当前结论

50000 行不是问题本身，问题是“每次打开列表都实时重算聚合视图”。

彻底解决方式是把来源商品库变成正式读模型：

```text
同步时聚合一次，查询时直接读取。
```

这样来源商品库、商品列表绑定、快捷创建商品和外部按 SKU 查仓库能力都复用同一套聚合结果，不再各自临时拼 SQL。
